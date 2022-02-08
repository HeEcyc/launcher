package com.wlisuha.applauncher.ui

import android.annotation.SuppressLint
import android.app.WallpaperManager
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.annotation.ColorInt
import androidx.databinding.ObservableField
import androidx.lifecycle.viewModelScope
import com.wlisuha.applauncher.BR
import com.wlisuha.applauncher.LauncherApplication
import com.wlisuha.applauncher.R
import com.wlisuha.applauncher.base.BaseAdapter
import com.wlisuha.applauncher.base.BaseViewModel
import com.wlisuha.applauncher.base.createAdapter
import com.wlisuha.applauncher.data.AppScreenLocation
import com.wlisuha.applauncher.data.DragInfo
import com.wlisuha.applauncher.data.InstalledApp
import com.wlisuha.applauncher.data.db.DataBase
import com.wlisuha.applauncher.databinding.BottomItemApplicationBinding
import com.wlisuha.applauncher.utils.APP_COLUMN_COUNT
import kotlinx.coroutines.*
import java.text.Collator

class AppViewModel : BaseViewModel() {
    val labelColor = ObservableField(Color.WHITE)
    val isSelectionEnabled = ObservableField(false)
    val intentFilter = IntentFilter().apply {
        addAction(Intent.ACTION_PACKAGE_REMOVED)
        addAction(Intent.ACTION_PACKAGE_ADDED)
        addDataScheme("package")
    }

    private var saveDBJob: Job? = null
    var movePageJob: Job? = null

    private val packageManager get() = LauncherApplication.instance.packageManager
    private val skipPackagesList = arrayOf(
        "com.android.traceur",
        LauncherApplication.instance.packageName
    )

    private val wallpaperManager = WallpaperManager.getInstance(LauncherApplication.instance)

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            // setLabelColor()
            wallpaperManager.addOnColorsChangedListener(
                { _, _ -> setLabelColor() }, Handler(Looper.getMainLooper())
            )
        }
    }

    private fun setLabelColor() {
        getPrimaryColor()
            ?.let(::getContrastColor)
            ?.let(labelColor::set)
    }

    val bottomAppListAdapter =
        createAdapter<InstalledApp, BottomItemApplicationBinding>(R.layout.bottom_item_application) {
            onItemClick = { launchApp(it.packageName) }
            onBind = { item, binding, adapter ->

                binding.setVariable(BR.viewModel, this@AppViewModel)
                binding.notifyPropertyChanged(BR.viewModel)
                binding.root.setOnLongClickListener {
                    isSelectionEnabled.set(true)
                    createDragAndDropView(item, binding, adapter)
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
        binding.root.startDragAndDrop(
            null,
            View.DragShadowBuilder(binding.root),
            DragInfo(adapter, adapter.getData().indexOf(item), -1, item),
            0
        )
    }

    fun getBottomAppsItemCount() = bottomAppListAdapter.itemCount

    fun insertItemToBottomBar(dragInfo: DragInfo, position: Int?) {
        position ?: return
        dragInfo.removeItem()
        if (canAddItem(dragInfo, position)) {
            addItemToPosition(position, dragInfo.draggedItem)
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
        bottomAppListAdapter.addItem(position, dragInfo)
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

    @SuppressLint("NewApi")
    private fun getPrimaryColor() = wallpaperManager
        .getWallpaperColors(1)
        ?.primaryColor
        ?.toArgb()

    private fun getContrastColor(@ColorInt color: Int): Int {
        val a =
            1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(
                color
            )) / 255
        return if (a < 0.5) Color.BLACK else Color.WHITE
    }

    private fun availableApp(applicationInfo: ApplicationInfo): Boolean {
        return packageManager.getLaunchIntentForPackage(applicationInfo.packageName) != null
                && !skipPackagesList.contains(applicationInfo.packageName) &&
                applicationInfo.enabled
    }

    fun removeItem(packageName: String) {
        Intent(Intent.ACTION_DELETE, Uri.fromParts("package", packageName, null))
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .let(LauncherApplication.instance::startActivity)
    }

    fun launchApp(packageName: String) {
        packageManager.getLaunchIntentForPackage(packageName)
            ?.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ?.let(LauncherApplication.instance::startActivity)
    }

    suspend fun readAllPackage(itemCountOnPage: Int): MutableList<List<InstalledApp>> {
        return if (DataBase.dao.getRowCount() == 0) {
            val appList = getInstalledAppList(itemCountOnPage)
            saveApplicationToDB(appList)
            appList.toMutableList()
        } else readAppFromDB().toMutableList()
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
            .filter(::availableApp)
            .map(::createModel)
            .sortedWith(compareBy(collator) { it.name })
            .chunked(itemCountOnPage)
    }

    private fun getCurrentLocale() = with(LauncherApplication.instance.resources.configuration) {
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
}