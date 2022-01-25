package com.wlisuha.applauncher.data

import com.wlisuha.applauncher.base.BaseAdapter

class DragInfo(
    var adapter: BaseAdapter<InstalledApp, *>,
    var draggedItemPos: Int,
    var currentPage: Int = -1,
    val draggedItem: InstalledApp
) {

    private var needRestore = false

    fun restoreItem() {
        if (!needRestore) return
        adapter.addItem(draggedItemPos, draggedItem)
        needRestore = false
    }

    fun disableRestore() {
        needRestore = false
    }

    fun enableRestore() {
        needRestore = true
    }

    fun removeItem() {
        adapter.removeItem(draggedItem)
    }

    fun getCurrentItemPosition() = adapter.getData()
        .indexOf(draggedItem)

    fun updateItemPosition() {
        draggedItemPos = adapter.getData().indexOf(draggedItem)
    }
}