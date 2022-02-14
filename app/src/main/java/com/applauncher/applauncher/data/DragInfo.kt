package com.applauncher.applauncher.data

import com.applauncher.applauncher.base.BaseAdapter

class DragInfo(
    var adapter: BaseAdapter<InstalledApp, *>,
    var draggedItemPos: Int,
    var currentPage: Int = -1,
    val draggedItem: InstalledApp
) {

    fun removeItem() {
        adapter.removeItem(draggedItem)
    }

    fun getCurrentItemPosition() = adapter.getData()
        .indexOf(draggedItem)

    fun updateItemPosition() {
        draggedItemPos = adapter.getData().indexOf(draggedItem)
    }
}