package com.wlisuha.applauncher.ui

import android.annotation.SuppressLint
import android.app.WallpaperManager
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.ColorInt
import androidx.databinding.ObservableField
import com.wlisuha.applauncher.LauncherApplication
import com.wlisuha.applauncher.R
import com.wlisuha.applauncher.base.BaseViewModel
import com.wlisuha.applauncher.base.createAdapter
import com.wlisuha.applauncher.data.DragInfo
import com.wlisuha.applauncher.data.InstalledApp
import com.wlisuha.applauncher.databinding.BottomItemApplicationBinding

class AppViewModel : BaseViewModel() {
    val labelColor = ObservableField(Color.BLACK)

    private val packageManager get() = LauncherApplication.instance.packageManager
    private val skipPackagesList = arrayOf(
        "com.android.traceur",
        LauncherApplication.instance.packageName
    )

    private val wallpaperManager = WallpaperManager.getInstance(LauncherApplication.instance)

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setLabelColor()
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


    val fastAppListAdapter =
        createAdapter<InstalledApp, BottomItemApplicationBinding>(R.layout.bottom_item_application) {

        }

    fun deleteItemFromAppsBarList(dragInfo: DragInfo) {
        dragInfo.enableRestore()
        deleteItemFromFastAppList(dragInfo.draggedItem)
    }

    private fun deleteItemFromFastAppList(draggedItem: InstalledApp) {
        fastAppListAdapter.removeItem(draggedItem)
    }

    fun isFirstItem(dragInfo: DragInfo): Boolean {
        return fastAppListAdapter.getData()
            .isEmpty() || fastAppListAdapter.getData().size == 1 && fastAppListAdapter.getData()
            .any { it.packageName == dragInfo.draggedItem.packageName }
    }

    fun getBottomAppsItemCount() = fastAppListAdapter.itemCount

    fun insertFirstItemToBottomBar(dragInfo: DragInfo) {
        if (fastAppListAdapter.getData().size > 0) return
        fastAppListAdapter.addItem(dragInfo.draggedItem)
        dragInfo.disableRestore()
    }

    fun insertItemToBottomBar(dragInfo: DragInfo, sideIndexes: Array<Int?>) {
        sideIndexes.filterNotNull()
            .map { fastAppListAdapter.getData()[it] }
            .forEach { if (it.packageName == dragInfo.draggedItem.packageName) return }

        if (fastAppListAdapter.getData().size == 4 &&
            !fastAppListAdapter.getData().any { it.packageName == dragInfo.draggedItem.packageName }
        ) return

        if (sideIndexes[0] == null && sideIndexes[1] != null) {
            addItemToPosition(getBottomAppsItemCount(), dragInfo.draggedItem)
        } else if (sideIndexes[1] == null && sideIndexes[0] != null)
            addItemToPosition(0, dragInfo.draggedItem)
        else addItemToPosition(sideIndexes[0]!!, dragInfo.draggedItem)

        dragInfo.disableRestore()
    }

    private fun addItemToPosition(position: Int, dragInfo: InstalledApp) {
        fastAppListAdapter.addItem(position, dragInfo)
        var removeItemPosition = -1
        fastAppListAdapter.getData().forEachIndexed { index, installedApp ->
            if (installedApp.packageName == dragInfo.packageName && index != position) {
                removeItemPosition = index
            }
        }
        fastAppListAdapter.removeItem(removeItemPosition)
    }

    @SuppressLint("NewApi")
    private fun getPrimaryColor() = wallpaperManager
        .getWallpaperColors(1)
        ?.primaryColor
        ?.toArgb()

    private fun getContrastColor(@ColorInt color: Int): Int {
        val a =
            1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255
        return if (a < 0.5) Color.BLACK else Color.WHITE
    }

    fun getApplicationList() = packageManager
        .getInstalledApplications(PackageManager.GET_META_DATA)
        .filter(::availableApp)
        .map(::createModel)

    private fun availableApp(applicationInfo: ApplicationInfo): Boolean {
        return packageManager.getLaunchIntentForPackage(applicationInfo.packageName) != null
                && !skipPackagesList.contains(applicationInfo.packageName)
    }

    private fun createModel(rf: ApplicationInfo): InstalledApp {
        return InstalledApp(
            rf.loadLabel(packageManager).toString(),
            rf.loadIcon(packageManager),
            rf.packageName,
            labelColor
        )
    }

}