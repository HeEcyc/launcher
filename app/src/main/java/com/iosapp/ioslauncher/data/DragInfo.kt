package com.iosapp.ioslauncher.data

import com.iosapp.ioslauncher.base.BaseAdapter

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