package com.wlisuha.applauncher.data

import com.wlisuha.applauncher.base.BaseAdapter

class DragInfo(
    val adapter: BaseAdapter<InstalledApp, *>,
    var draggedItemPos: Int,
    val draggedItem: InstalledApp,
    val fromBottomPanel: Boolean = false
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
}