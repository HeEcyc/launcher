package com.iosapp.ioslauncher.data

import android.view.View
import com.iosapp.ioslauncher.base.BaseAdapter
import com.iosapp.ioslauncher.utils.PAGE_INDEX_JUST_MENU

class DragInfo(
    var cell: DesktopCell?,
    var draggedItemPos: Int,
    var currentPage: Int = -1,
    val draggedItem: InstalledApp,
    val removeFromOriginalPlace: Boolean = true,
    private val bottomAdapter: BaseAdapter<InstalledApp, *>? = null,
    val originView: View? = null
) {

    var canStillRequestAppInfo: Boolean = true
    var hasCheckedIfOverOriginal: Boolean = false

    private val dragBeginTimeMillis = System.currentTimeMillis()

    val timeMillisSinceDragBegin get() = System.currentTimeMillis() - dragBeginTimeMillis

    fun removeItem() {
        cell?.app?.set(null)
        bottomAdapter?.removeItem(draggedItem)
    }

    fun getCurrentItemPosition() = cell?.position

    fun updateItemPosition() {
        draggedItemPos = cell?.position ?: -1
        currentPage = cell?.page ?: PAGE_INDEX_JUST_MENU
    }

}