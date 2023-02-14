package com.iosapp.ioslauncher.ui.app

import android.annotation.SuppressLint
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.MotionEvent
import android.view.View
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.RecyclerView
import com.iosapp.ioslauncher.BR
import com.iosapp.ioslauncher.LauncherApplication
import com.iosapp.ioslauncher.R
import com.iosapp.ioslauncher.base.BaseAdapter
import com.iosapp.ioslauncher.base.BaseViewModel
import com.iosapp.ioslauncher.base.createAdapter
import com.iosapp.ioslauncher.data.*
import com.iosapp.ioslauncher.data.db.DataBase
import com.iosapp.ioslauncher.databinding.BottomItemApplicationBinding
import com.iosapp.ioslauncher.ui.custom.NonSwipeableViewPager
import com.iosapp.ioslauncher.utils.APP_COLUMN_COUNT
import com.iosapp.ioslauncher.utils.PAGE_INDEX_BOTTOM
import com.iosapp.ioslauncher.utils.PAGE_INDEX_JUST_MENU
import kotlinx.coroutines.*
import java.text.Collator
import kotlin.math.max
import com.iosapp.ioslauncher.databinding.LauncherItemApplicationMenuBinding as LauncherItemApplicationBinding

class AppViewModel : BaseViewModel(), SharedPreferences.OnSharedPreferenceChangeListener {

    val showTopFields = MutableLiveData<InstalledApp>()

    val onMenuItemLongClick = MutableLiveData<Unit>()
    val disableMotionLayoutLongClick = MutableLiveData<Unit>()

    var disableSelection = false

    private val browserPackage by lazy {
        Intent("android.intent.action.VIEW", Uri.parse("http://"))
            .let { packageManager.resolveActivity(it, 0)?.activityInfo?.packageName }
    }
    val isSelectionEnabled = object : ObservableBoolean(false) {
        override fun set(value: Boolean) {
            if (!disableSelection) {
                if (value && !get()) {
                    vibrate()
                }
                super.set(value)
            }
            notifyChange()
        }
    }
    val intentFilter = IntentFilter().apply {
        addAction(Intent.ACTION_PACKAGE_REMOVED)
        addAction(Intent.ACTION_PACKAGE_ADDED)
        addDataScheme("package")
    }
    val launcherBG = ObservableField(Prefs.bgRes)
    var stateProvider: NonSwipeableViewPager.StateProvider? = null

    private var saveDBJob: Job? = null
    var movePageJob: Job? = null

    private val packageManager get() = LauncherApplication.instance.packageManager
    private val skipPackagesList = arrayOf(
        "com.android.traceur",
        LauncherApplication.instance.packageName
    )

    private val defaultBottomAppList = arrayOf(
        "com.google.android.gm",
        "com.android.contacts",
        "com.google.android.youtube"
    )

    @SuppressLint("ClickableViewAccessibility")
    val menuAdapter = createAdapter<InstalledApp, LauncherItemApplicationBinding>(R.layout.launcher_item_application_menu) {
        onItemClick = { launchApp(it.packageName) }
        onBind = { item, binding, adapter ->
            binding.isShortcut = false
            binding.root.setOnTouchListener { _, _ ->
                disableMotionLayoutLongClick.postValue(Unit)
                false
            }
            binding.setVariable(BR.viewModel, this@AppViewModel)
            binding.label.setTextColor(Color.BLACK)
            binding.notifyPropertyChanged(BR.viewModel)
            binding.root.setOnLongClickListener {
                onMenuItemLongClick.postValue(Unit)
                startDragAndDrop(item, binding, adapter, 0, false)
                false
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    val bottomAppListAdapter =
        createAdapter<InstalledApp, BottomItemApplicationBinding>(R.layout.bottom_item_application) {
            onItemClick = { launchApp(it.packageName) }
            onBind = { item, binding, adapter ->
                binding.root.setOnTouchListener { _, motionEvent ->
                    if (stateProvider?.isPresentOnHomeScreen() == false)
                        return@setOnTouchListener false
                    else when (motionEvent.action) {
                        MotionEvent.ACTION_UP -> disableSelection = false
                        MotionEvent.ACTION_DOWN -> disableSelection = true
                    }
                    false
                }
                binding.setVariable(BR.viewModel, this@AppViewModel)
                binding.notifyPropertyChanged(BR.viewModel)
                binding.root.setOnLongClickListener {
                    if (stateProvider?.isPresentOnHomeScreen() == true) {
                        stateProvider?.onAppSelected()
                        viewModelScope.launch(Dispatchers.Main) {
                            binding.appIcon.imageTintList = ColorStateList.valueOf(Color.parseColor("#40000000"))
                            delay(500)
                            binding.appIcon.imageTintList = null
                        }
                        createDragAndDropView(item, binding, adapter)
                    }
                    false
                }
            }
        }.also {
            it.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {

                private var previousItems: List<InstalledApp>? = null

                private fun saveChanges() = runBlocking(Dispatchers.IO) {
                    val newItems = listOf(*it.getData().toTypedArray())
                    val updatedIndices = max(newItems.size, previousItems?.size ?: 0)
                    if (updatedIndices > 0)
                        for (i in 0 until updatedIndices) {
                            val item = newItems.getOrNull(i)
                            if (item?.packageName == previousItems?.getOrNull(i)?.packageName) continue
                            if (item !== null)
                                DataBase.dao.addItem(AppScreenLocation(item.packageName, -1, i))
                            else
                                DataBase.dao.deleteShortcutByPosition(-1, i)
                        }
                    previousItems = newItems
                }

                override fun onChanged() {
                    saveChanges()
                }

                override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
                    saveChanges()
                }

                override fun onItemRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) {
                    saveChanges()
                }

                override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                    saveChanges()
                }

                override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
                    saveChanges()
                }

                override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
                    saveChanges()
                }

            })
        }

    init {
        Prefs.sharedPreference.registerOnSharedPreferenceChangeListener(this)
        viewModelScope.launch(Dispatchers.IO) {
            val apps = readAllAppPositions()
            launch(Dispatchers.Main) {
                menuAdapter.reloadData(apps.distinctBy { it.packageName }.map { createModel(it.packageName) })
            }
        }
    }

    fun showTopFields(app: InstalledApp) = showTopFields.postValue(app)

    @SuppressLint("NewApi")
    private fun startDragAndDrop(
        item: InstalledApp,
        binding: LauncherItemApplicationBinding,
        adapter: BaseAdapter<InstalledApp, *>,
        position: Int,
        removeFromOriginalPlace: Boolean = true,
    ) {
//        viewModel.isSelectionEnabled.set(true)
//        canCreatePage = adapter.getData().size > 1
        binding.appIcon.startDragAndDrop(
            null,
            View.DragShadowBuilder(binding.appIcon),
            DragInfo(DesktopCell(adapter.getData().indexOf(item), PAGE_INDEX_JUST_MENU, item), adapter.getData().indexOf(item), position, item, removeFromOriginalPlace),
            0
        )
        showTopFields(item)
    }

    fun onEditClick() = isSelectionEnabled.set(!isSelectionEnabled.get())

    @SuppressLint("NewApi")
    private fun createDragAndDropView(
        item: InstalledApp,
        binding: BottomItemApplicationBinding,
        adapter: BaseAdapter<InstalledApp, *>
    ) {
        disableSelection = false
//        isSelectionEnabled.set(true)
        binding.root.startDragAndDrop(
            null,
            View.DragShadowBuilder(binding.root),
            DragInfo(
                DesktopCell(adapter.getData().indexOf(item), PAGE_INDEX_BOTTOM, item),
                adapter.getData().indexOf(item),
                -1,
                item,
                bottomAdapter = adapter
            ),
            0
        )
        showTopFields(item)
    }

    fun isAppSystem(packageName: String) =
        packageManager.getApplicationInfo(packageName, 0).flags and ApplicationInfo.FLAG_SYSTEM == ApplicationInfo.FLAG_SYSTEM

    fun vibrate() {
        with(LauncherApplication.instance.getSystemService(Vibrator::class.java)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
            else vibrate(100)
        }
    }

    fun getBottomAppsItemCount() = bottomAppListAdapter.itemCount - 1

    fun insertItemToBottomBar(dragInfo: DragInfo, position: Int?) {
        position ?: return
        if (canAddItem(dragInfo, position)) {
            if (dragInfo.removeFromOriginalPlace) dragInfo.removeItem()
            addItemToPosition(position, dragInfo.draggedItem)
            dragInfo.currentPage = -1
            dragInfo.cell = DesktopCell(position, PAGE_INDEX_BOTTOM, dragInfo.draggedItem)
        }
    }

    private fun canAddItem(dragInfo: DragInfo, position: Int): Boolean {
        bottomAppListAdapter.getData()
            .takeIf { it.size == APP_COLUMN_COUNT }
            ?.let { it ->
                if (!it.any { it.packageName == dragInfo.draggedItem.packageName }) return false
            }
        bottomAppListAdapter.getData()
            .getOrNull(position)
            ?.let { if (it.packageName == dragInfo.draggedItem.packageName) return false }

        return true
    }

    private fun addItemToPosition(position: Int, dragInfo: InstalledApp) {
        if (bottomAppListAdapter.itemCount == 0) {
            bottomAppListAdapter.addItem(dragInfo)
            addItem(dragInfo, 0, -1)
            return
        }
        bottomAppListAdapter.addItemSafe(position, dragInfo)
        var removeItemPosition = -1

        bottomAppListAdapter.getData().forEachIndexed { index, installedApp ->
            if (installedApp.packageName == dragInfo.packageName && index != position) {
                removeItemPosition = index
            }
        }
        if (removeItemPosition != -1) bottomAppListAdapter.removeItem(removeItemPosition)


        saveDBJob?.cancel()
        saveDBJob = viewModelScope.launch(Dispatchers.IO) {
            delay(300)
            saveBottomAppsList()
        }
    }

    private fun saveBottomAppsList() {
        bottomAppListAdapter
            .getData()
            .forEachIndexed { index, installedApp -> addItem(installedApp, index, -1) }
    }

    private fun availableApp(applicationInfo: ApplicationInfo): Boolean {
        return packageManager.getLaunchIntentForPackage(applicationInfo.packageName) != null
                && !skipPackagesList.contains(applicationInfo.packageName) &&
                applicationInfo.enabled
    }

    fun availableApp(packageName: String): Boolean {
        return packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            .let(::availableApp)
    }

    fun removeItem(packageName: String) {
        Intent(Intent.ACTION_DELETE, Uri.fromParts("package", packageName, null))
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .let(LauncherApplication.instance::startActivity)
    }

    fun deleteShortcut(app: InstalledApp) {
        val index = bottomAppListAdapter.getData().indexOf(app)
        bottomAppListAdapter.removeItem(index)
        bottomAppListAdapter.notifyItemRemoved(index)
    }

    fun launchApp(packageName: String) {
        if (stateProvider?.isPresentOnHomeScreen() == false) return
        packageManager.getLaunchIntentForPackage(packageName)
            ?.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ?.let(LauncherApplication.instance::startActivity)
    }

    suspend fun getFormattedAppPositions(itemsPerPage: Int): MutableList<List<DesktopCell>> {
        val positions = readAllAppPositions().filterNot { it.page in listOf(PAGE_INDEX_JUST_MENU, PAGE_INDEX_BOTTOM) }
        val pages = positions.groupBy { it.page }
        return MutableList((pages.keys.maxOrNull() ?: 0) + 1) { page ->
            List(itemsPerPage) { index -> DesktopCell(
                index, page, pages[page]?.find { it.position == index }?.let { createModel(it.packageName) }
            ) }
        }
    }

    suspend fun readAllAppPositions(): List<AppScreenLocation> {
        return if (DataBase.dao.getRowCount() == 0) {
            val apps = getInstalledApps()
            saveAppsToDB(apps)
            saveBottomAppList()
            apps.toDefaultAppPositions()
        } else {
            readAllAppPositionsFromDB()
        }.also {
            listOf(*it.toTypedArray())
                .filter { it.page == PAGE_INDEX_BOTTOM }
                .sortedBy { it.position }
                .map { createModel(it.packageName) }
                .let { withContext(Dispatchers.Main) { bottomAppListAdapter.reloadData(it) } }
        }
    }

    private fun getInstalledApps(): List<InstalledApp> {
        val collator = Collator
            .getInstance(getCurrentLocale())
        return packageManager
            .getInstalledApplications(PackageManager.GET_META_DATA)
            .asSequence()
            .filter(::availableApp)
            .map(::createModel)
            .sortedWith(compareBy(collator) { it.name })
            .toList()
    }

    private fun saveAppsToDB(apps: List<InstalledApp>) {
        viewModelScope.launch(Dispatchers.IO) {
            var currentBottomAppIndex = 0
            apps.forEachIndexed { index, installedApp ->
                val isDefaultBottomApp = isDefaultBottomApp(installedApp.packageName)
                addItem(
                    installedApp,
                    if (isDefaultBottomApp) currentBottomAppIndex++ else index,
                    if (isDefaultBottomApp) -1 else PAGE_INDEX_JUST_MENU
                )
            }
        }
    }

    private fun List<InstalledApp>.toDefaultAppPositions() = mapIndexed { index, installedApp ->
        AppScreenLocation(
            installedApp.packageName,
            if (isDefaultBottomApp(installedApp.packageName)) -1 else PAGE_INDEX_JUST_MENU,
            index
        )
    }

    private fun readAllAppPositionsFromDB(): List<AppScreenLocation> {
        val appList = DataBase.dao
            .getAppsPositions()
            .filter(::appExist)
            .toMutableList()

        val collator = Collator
            .getInstance(getCurrentLocale())

        return appList.sortedWith(compareBy(collator) { createModel(it.packageName).name })
    }

    suspend fun readAllPackage(itemCountOnPage: Int): MutableList<List<InstalledApp>> {
        return if (DataBase.dao.getRowCount() == 0) {
            val appList = getInstalledAppList(itemCountOnPage)
            saveApplicationToDB(appList)
            saveBottomAppList()
            appList.toMutableList()
        } else readAppFromDB().toMutableList()
    }

    private fun saveBottomAppList() {
        bottomAppListAdapter.getData().forEachIndexed { index, installedApp ->
            AppScreenLocation(installedApp.packageName, -1, index)
                .let(DataBase.dao::addItem)
        }
    }

    private fun saveApplicationToDB(appList: List<List<InstalledApp>>) {
        viewModelScope.launch(Dispatchers.IO) {
            appList.forEachIndexed { pageIndex, list ->
                list.forEachIndexed { index, installedApp ->
                    addItem(
                        installedApp,
                        index,
                        if (isDefaultBottomApp(installedApp.packageName)) -1 else pageIndex
                    )
                }
            }
        }
    }

    private suspend fun readAppFromDB(): List<List<InstalledApp>> {
        val appList = DataBase.dao
            .getAppsPositions()
            .filter(::appExist)
            .toMutableList()

        val bottomApps = appList.filter { it.page == -1 }
//        appList.removeAll(bottomApps)

        bottomApps.sortedBy { it.position }
            .map { createModel(it.packageName) }
            .let {
                withContext(Dispatchers.Main) { bottomAppListAdapter.reloadData(it) }
            }

        val collator = Collator
            .getInstance(getCurrentLocale())

        return appList
            .map { createModel(it.packageName) }
            .sortedWith(compareBy(collator) { it.name })
            .chunked(16)
    }

    private fun appExist(appScreenLocation: AppScreenLocation): Boolean = try {
        packageManager.getPackageInfo(appScreenLocation.packageName, 0)
        true
    } catch (e: Exception) {
        DataBase.dao.delete(appScreenLocation)
        false
    }

    private fun getInstalledAppList(itemCountOnPage: Int): List<List<InstalledApp>> {
        val collator = Collator
            .getInstance(getCurrentLocale())
        return packageManager
            .getInstalledApplications(PackageManager.GET_META_DATA)
            .asSequence()
            .filter(::availableApp)
//            .filter(::notBottomBarAppList)
            .map(::createModel)
            .sortedWith(compareBy(collator) { it.name })
            .chunked(itemCountOnPage)
            .toList()
    }

    private fun notBottomBarAppList(applicationInfo: ApplicationInfo): Boolean {
        if (isDefaultBottomApp(applicationInfo.packageName)) {
            viewModelScope.launch(Dispatchers.Main) {
                bottomAppListAdapter.addItem(createModel(applicationInfo.packageName))
            }
            return false
        }
        return true
    }

    private fun isDefaultBottomApp(packageName: String): Boolean {
        return if (defaultBottomAppList.contains(packageName)) true
        else browserPackage == packageName
    }

    private fun getCurrentLocale() =
        with(LauncherApplication.instance.resources.configuration) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) locales[0]
            else locale
        }

    fun createModel(packageName: String): InstalledApp {
        return packageManager.getApplicationInfo(packageName, 0)
            .let(::createModel)
    }

    private fun createModel(rf: ApplicationInfo): InstalledApp {
        val isNotSystemApp =
            rf.flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0
        return InstalledApp(
            rf.loadLabel(packageManager).toString(),
            runBlocking(Dispatchers.IO) { rf.loadIcon(packageManager) },
            rf.packageName,
            isNotSystemApp,
            this
        )
    }

    fun saveNewPositionItem(item: InstalledApp?, newPosition: Int, page: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            if (item === null)
                DataBase.dao.deleteShortcutByPosition(page, newPosition)
            else
                AppScreenLocation(item.packageName, page, newPosition)
                    .let(DataBase.dao::addItem)
        }
    }

    fun addItem(item: InstalledApp, newPosition: Int, page: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            AppScreenLocation(item.packageName, page, newPosition)
                .let(DataBase.dao::addItem)
        }
    }

    fun onAppInstalled(installedApp: InstalledApp) {
        if (menuAdapter.getData().contains(installedApp)) return
        val collator = Collator.getInstance(getCurrentLocale())
        val newIndex = menuAdapter.getData()
            .toMutableList()
            .apply { add(installedApp); sortWith(compareBy(collator) { it.name }) }
            .indexOf(installedApp)
        menuAdapter.addItem(newIndex, installedApp)
        menuAdapter.notifyDataSetChanged()
        viewModelScope.launch(Dispatchers.IO) {
            AppScreenLocation(installedApp.packageName, 0, newIndex).let(DataBase.dao::addItem)
        }
    }

    fun deletePackage(packageName: String) {
        viewModelScope.launch(Dispatchers.IO) { DataBase.dao.deletePackage(packageName) }
        val index = menuAdapter.getData().indexOfFirst { it.packageName == packageName }
        if (index == -1) return
        menuAdapter.removeItem(index)
//        menuAdapter.notifyItemRemoved(index)
        menuAdapter.notifyDataSetChanged()
        val bottomIndex = bottomAppListAdapter.getData().indexOfFirst { it.packageName == packageName }
        if (bottomIndex == -1) return
        bottomAppListAdapter.removeItem(bottomIndex)
        bottomAppListAdapter.notifyItemRemoved(bottomIndex)
    }

    override fun onSharedPreferenceChanged(
        sharedPreferences: SharedPreferences,
        key: String
    ) {
        when (key) {
            Prefs.bgResKey -> launcherBG.set(sharedPreferences.getInt(key, R.mipmap.img_10))
        }
    }

    override fun onCleared() {
        super.onCleared()
        Prefs.sharedPreference.unregisterOnSharedPreferenceChangeListener(this)
    }


}