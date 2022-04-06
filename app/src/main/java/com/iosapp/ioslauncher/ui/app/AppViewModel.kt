package com.iosapp.ioslauncher.ui.app

import android.annotation.SuppressLint
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.MotionEvent
import android.view.View
import androidx.databinding.ObservableField
import androidx.lifecycle.viewModelScope
import com.iosapp.ioslauncher.BR
import com.iosapp.ioslauncher.LauncherApplication
import com.iosapp.ioslauncher.R
import com.iosapp.ioslauncher.base.BaseAdapter
import com.iosapp.ioslauncher.base.BaseViewModel
import com.iosapp.ioslauncher.base.createAdapter
import com.iosapp.ioslauncher.data.AppScreenLocation
import com.iosapp.ioslauncher.data.DragInfo
import com.iosapp.ioslauncher.data.InstalledApp
import com.iosapp.ioslauncher.data.Prefs
import com.iosapp.ioslauncher.data.db.DataBase
import com.iosapp.ioslauncher.databinding.BottomItemApplicationBinding
import com.iosapp.ioslauncher.ui.custom.NonSwipeableViewPager
import com.iosapp.ioslauncher.utils.APP_COLUMN_COUNT
import kotlinx.coroutines.*
import java.text.Collator


class AppViewModel : BaseViewModel(), SharedPreferences.OnSharedPreferenceChangeListener {
    var disableSelection = false
    val labelColor = ObservableField(Color.WHITE)

    private val browserPackage by lazy {
        Intent("android.intent.action.VIEW", Uri.parse("http://"))
            .let { packageManager.resolveActivity(it, 0)?.activityInfo?.packageName }
    }
    val isSelectionEnabled = object : ObservableField<Boolean>(false) {
        override fun set(value: Boolean?) {
            if (!disableSelection) {
                if (value == true && get() == false) vibrate()
                super.set(value)
            }
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

    init {
        Prefs.sharedPreference.registerOnSharedPreferenceChangeListener(this)
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
                        createDragAndDropView(item, binding, adapter)
                    }
                    false
                }
            }
        }

    @SuppressLint("NewApi")
    private fun createDragAndDropView(
        item: InstalledApp,
        binding: BottomItemApplicationBinding,
        adapter: BaseAdapter<InstalledApp, *>
    ) {
        disableSelection = false
        isSelectionEnabled.set(true)
        binding.root.startDragAndDrop(
            null,
            View.DragShadowBuilder(binding.root),
            DragInfo(adapter, adapter.getData().indexOf(item), -1, item),
            0
        )
    }

    fun vibrate() {
        with(LauncherApplication.instance.getSystemService(Vibrator::class.java)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                vibrate(VibrationEffect.createOneShot(5, VibrationEffect.DEFAULT_AMPLITUDE))
            else vibrate(5)
        }
    }

    fun getBottomAppsItemCount() = bottomAppListAdapter.itemCount - 1

    fun insertItemToBottomBar(dragInfo: DragInfo, position: Int?) {
        position ?: return
        if (canAddItem(dragInfo, position)) {
            dragInfo.removeItem()
            addItemToPosition(position, dragInfo.draggedItem)
            dragInfo.currentPage = -1
            dragInfo.adapter = bottomAppListAdapter
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

    fun launchApp(packageName: String) {
        if (stateProvider?.isPresentOnHomeScreen() == false) return
        packageManager.getLaunchIntentForPackage(packageName)
            ?.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ?.let(LauncherApplication.instance::startActivity)
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
                    addItem(installedApp, index, pageIndex)
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
        appList.removeAll(bottomApps)

        bottomApps.sortedBy { it.position }
            .map { createModel(it.packageName) }
            .let {
                withContext(Dispatchers.Main) { bottomAppListAdapter.reloadData(it) }
            }

        return appList.sortedBy { it.page }
            .groupBy { it.page }
            .map {
                it.value.sortedBy { appScreenLocation -> appScreenLocation.position }
                    .map { appScreenLocation -> createModel(appScreenLocation.packageName) }
            }
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
            .filter(::notBottomBarAppList)
            .map(::createModel)
            .sortedWith(compareBy(collator) { it.name })
            .chunked(itemCountOnPage)
            .toList()
    }

    private fun notBottomBarAppList(applicationInfo: ApplicationInfo): Boolean {
        if (isDefaultBottomApp(applicationInfo)) {
            viewModelScope.launch(Dispatchers.Main) {
                bottomAppListAdapter.addItem(createModel(applicationInfo.packageName))
            }
            return false
        }
        return true
    }

    private fun isDefaultBottomApp(applicationInfo: ApplicationInfo): Boolean {
        return if (defaultBottomAppList.contains(applicationInfo.packageName)) true
        else browserPackage == applicationInfo.packageName
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
            rf.loadIcon(packageManager),
            rf.packageName,
            isNotSystemApp
        )
    }

    fun saveNewPositionItem(item: InstalledApp, newPosition: Int, page: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            AppScreenLocation(item.packageName, page, newPosition)
                .let(DataBase.dao::updateItem)
        }
    }

    fun addItem(item: InstalledApp, newPosition: Int, page: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            AppScreenLocation(item.packageName, page, newPosition)
                .let(DataBase.dao::addItem)
        }
    }

    fun deletePackage(packageName: String) {
        viewModelScope.launch(Dispatchers.IO) { DataBase.dao.deletePackage(packageName) }
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