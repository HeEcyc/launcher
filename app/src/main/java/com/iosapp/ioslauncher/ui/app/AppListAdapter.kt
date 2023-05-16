package com.iosapp.ioslauncher.ui.app

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import androidx.databinding.OnRebindCallback
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.PagerAdapter
import com.iosapp.ioslauncher.BR
import com.iosapp.ioslauncher.R
import com.iosapp.ioslauncher.base.BaseAdapter
import com.iosapp.ioslauncher.base.createAdapter
import com.iosapp.ioslauncher.data.DesktopCell
import com.iosapp.ioslauncher.data.DragInfo
import com.iosapp.ioslauncher.data.InstalledApp
import com.iosapp.ioslauncher.databinding.LauncherItemApplicationBinding
import com.iosapp.ioslauncher.databinding.LauncherItemApplicationMenuBinding
import com.iosapp.ioslauncher.ui.custom.ClickableMotionLayout
import com.iosapp.ioslauncher.ui.custom.NonSwipeableViewPager
import com.iosapp.ioslauncher.utils.PAGE_INDEX_BOTTOM
import com.iosapp.ioslauncher.utils.SwapHelper
import com.iosapp.ioslauncher.utils.diff.utils.AppListDiffUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AppListAdapter(
    private val listAppPages: MutableList<MutableList<DesktopCell>>,
    private val visibleApplicationsOnScreen: Int,
    private val viewModel: AppViewModel,
    private val stateProvider: NonSwipeableViewPager.StateProvider,
    private val motionLayout: ClickableMotionLayout
) : PagerAdapter() {

    init {
        listAppPages.forEach { page -> page.forEach { cell -> cell } }
    }

    private val swapHelper = SwapHelper(Handler(Looper.getMainLooper()))
    private var recyclers = mutableListOf<RecyclerView>()
    private var recyclersAdapters = mutableListOf<BaseAdapter<DesktopCell, *>>()
    val canCreatePage get() = listAppPages.size <= 5

    override fun getCount() = listAppPages.size.takeIf { it > 0 } ?: 1

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        return RecyclerView(container.context).apply {
            overScrollMode = View.OVER_SCROLL_NEVER
            adapter = recyclersAdapters.getOrElse(position) { createAdapter(position) }
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            layoutManager = GridLayoutManager(context, 4)
            recyclers.getOrNull(position)?.let { recyclers.set(position, this) } ?: recyclers.add(position, this)
            container.addView(this)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createAdapter(position: Int) =
        createAdapter<DesktopCell, LauncherItemApplicationBinding>(R.layout.launcher_item_application) {
//            initItems = listAppPages.getOrNull(position) ?: listOf()
            initItems = listAppPages[position]
            onPreBind = { cell, cellBinding, adapter ->
                cellBinding.addOnRebindCallback(object : OnRebindCallback<LauncherItemApplicationBinding>() {
                    override fun onBound(binding: LauncherItemApplicationBinding) {
                        val app = cell.app.get()
                        if (app === null) {
                            cellBinding.frame.children.forEach { try { cellBinding.frame.removeView(it) } catch (e: Exception) {} }
                        } else {
                            val appBinding = LauncherItemApplicationMenuBinding.inflate(LayoutInflater.from(cellBinding.root.context))
                            appBinding.item = app
                            appBinding.viewModel = app.viewModel
                            appBinding.isShortcut = true
                            appBinding.cell = cell
                            appBinding.root.setOnClickListener {
                                cell.app.get()?.packageName?.let(viewModel::launchApp)
                            }
                            appBinding.root.setOnTouchListener { _, _ ->
                                motionLayout.canCallLongCLick = false
                                false
                            }
                            appBinding.setVariable(BR.viewModel, app.viewModel)
                            appBinding.label.setTextColor(Color.WHITE)
                            appBinding.notifyPropertyChanged(BR.viewModel)
                            appBinding.root.setOnLongClickListener {
                                if (!stateProvider.isPresentOnHomeScreen()) return@setOnLongClickListener false
                                stateProvider.onAppSelected()
                                viewModel.viewModelScope.launch(Dispatchers.Main) {
                                    appBinding.appIcon.imageTintList = ColorStateList.valueOf(Color.parseColor("#40000000"))
                                    delay(500)
                                    appBinding.appIcon.imageTintList = null
                                }
                                startDragAndDrop(app, appBinding, cell, position)
                                false
                            }
                            cellBinding.frame.removeAllViews()
                            cellBinding.frame.addView(appBinding.root, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
                        }
                    }
                })
            }
            onBind = { item, cellBinding, _ ->
                cellBinding.item = item
            }
            onPostBind = { item, _, _ ->
                viewModel.observe(item.app) { _, _ ->
                    if (item.app.get() !== null) {
                        for(page in listAppPages.indices.reversed()) {
                            if (pageIsEmpty(page)) removePage(page)
                            else break
                        }
                    }

                    saveItemPositionsOnPage(listOf(item), item.page)
                }
            }
        }.apply { recyclersAdapters.add(position, this) }

    @SuppressLint("NewApi")
    private fun startDragAndDrop(
        item: InstalledApp,
        binding: LauncherItemApplicationMenuBinding,
        cell: DesktopCell,
        position: Int
    ) {
//        viewModel.isSelectionEnabled.set(true)
//        canCreatePage = adapter.getData().size > 1
        binding.appIcon.startDragAndDrop(
            null,
            View.DragShadowBuilder(binding.appIcon),
            DragInfo(cell, cell.position, position, item),
            0
        )
        viewModel.showTopFields(item)
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as View)
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view === `object` as View
    }

    fun swapItem(dragInfo: DragInfo, newPosition: Int, currentPage: Int) {
        if (/*isSwapInSameAdapter(dragInfo.draggedItem, currentPage)*/ currentPage == dragInfo.currentPage)
            swapItemInSamePage(dragInfo, newPosition, currentPage)
        else swapItemBetweenPages(dragInfo, newPosition, currentPage)
    }

    private fun swapItemBetweenPages(dragInfo: DragInfo, newItemPosition: Int, currentPage: Int) {
        val currentAdapter = getCurrentAppListAdapter(currentPage)
        val swappedItemCell = currentAdapter.getData()[newItemPosition]
        val swappedItem = swappedItemCell.app.get()
        val hasSwappedItem = swappedItem !== null

        swapHelper.requestToSwapBetweenPages(
            newItemPosition,
            dragInfo.draggedItemPos,
            dragInfo.currentPage,
            currentPage
        ) {

//            currentAdapter.addItem(newItemPosition, dragInfo.draggedItem)
            currentAdapter.getData()[newItemPosition].app.set(dragInfo.draggedItem)

//            if (dragInfo.replaceIfCollide) {
                if (hasSwappedItem) {
                    if (dragInfo.currentPage != PAGE_INDEX_BOTTOM)
                        dragInfo.cell?.app?.set(swappedItem)
                    else {
                        viewModel.bottomAppListAdapter.getData()[dragInfo.draggedItemPos] = swappedItem!!
                        viewModel.bottomAppListAdapter.notifyItemChanged(dragInfo.draggedItemPos)
                    }
                }

//                dragInfo.removeItem()
//                if (hasSwappedItem) currentAdapter.removeItem(swappedItem!!)
//            }

            dragInfo.cell = currentAdapter.getData()[newItemPosition]
//            dragInfo.currentPage = currentPage
            dragInfo.updateItemPosition()
        }
    }

    @Suppress("NAME_SHADOWING")
    private fun swapItemInSamePage(dragInfo: DragInfo, newPosition: Int, currentPage: Int) {
//        val newPosition = min(newPosition, getCurrentAppListAdapter(currentPage).itemCount)
        val oldItemPosition = dragInfo.getCurrentItemPosition()!!
        swapHelper.requestToSwapInSomePage(oldItemPosition, newPosition) {

            moveItem(dragInfo, newPosition)

            val swappedItem = getCurrentAppListAdapter(currentPage).getData()[newPosition]

            viewModel.saveNewPositionItem(dragInfo.draggedItem, newPosition, currentPage)
            viewModel.saveNewPositionItem(swappedItem.app.get(), oldItemPosition, currentPage)
        }
    }

    private fun moveItem(dragInfo: DragInfo, newPosition: Int) {
        val currentItemPosition = dragInfo.getCurrentItemPosition() ?: -1
        if (currentItemPosition == -1) {
            swapHelper.clearRequest()
            return
        }

        val currentCell = listAppPages[dragInfo.currentPage][currentItemPosition]
        val tmp = currentCell.app.get()
        val newCell = listAppPages[dragInfo.currentPage][newPosition]

//        val adapter = dragInfo.cell
//        val oldList = adapter.getData().toList()
//
        dragInfo.cell = newCell
//        dragInfo.draggedItemPos = newPosition
        dragInfo.updateItemPosition()
//
//        Collections.swap(adapter.getData(), currentItemPosition, newPosition)
//        updateItems(oldList, adapter)

        currentCell.app.set(newCell.app.get())
        currentCell.app.notifyChange()
        newCell.app.set(tmp)
        newCell.app.notifyChange()
        updateItems(recyclersAdapters[dragInfo.currentPage], currentItemPosition, newPosition)
    }

    private fun updateItems(adapter: BaseAdapter<DesktopCell, *>, vararg indices: Int) {
        indices.forEach { adapter.notifyItemChanged(it) }
    }

    private fun updateItems(oldList: List<InstalledApp>, adapter: BaseAdapter<InstalledApp, *>) {
        DiffUtil.calculateDiff(AppListDiffUtils(oldList, adapter.getData()))
            .dispatchUpdatesTo(adapter)
    }

    private fun isSwapInSameAdapter(item: InstalledApp, currentPage: Int) =
        getCurrentAppListAdapter(currentPage)
            .getData().any { it.app.get()?.packageName == item.packageName }

    fun getCurrentAppListView(page: Int) = recyclers[page]

    private fun getCurrentAppListAdapter(page: Int) = recyclersAdapters[page]

    fun clearRequests() {
        swapHelper.clearRequest()
    }

    fun insertToLastPosition(dragInfo: DragInfo, currentPage: Int, withDelay: Boolean) {
        with(getCurrentAppListAdapter(currentPage)) {
            if (getData().last().app.get() !== null /*itemCount == visibleApplicationsOnScreen*/) return
            else getData().lastOrNull()
                ?.app?.get()
                ?.packageName
                ?.let { if (it == dragInfo.draggedItem.packageName) return }
        }

        if (!withDelay) insertItemToLastPosition(dragInfo, currentPage)
        else swapHelper.requestInsertToLastPosition(currentPage) {
            insertItemToLastPosition(dragInfo, currentPage)
        }
    }

    private fun pageIsEmpty(oldPage: Int) = when {
        oldPage == -1 -> false
        oldPage >= recyclersAdapters.size -> false
        else -> getCurrentAppListAdapter(oldPage).getData().none { it.app.get() !== null }
    }

    private fun removePage(oldPage: Int) {
        listAppPages.removeAt(oldPage)
        notifyDataSetChanged()
    }

    fun createPage(): Boolean {
        if (!canCreatePage) return false

//        listAppPages.add(listOf())
        MutableList(visibleApplicationsOnScreen) {
            DesktopCell(it, listAppPages.size)
        }.let(listAppPages::add)
//        recyclersAdapters.add(createAdapter(listAppPages.size - 1))
        notifyDataSetChanged()
//        canCreatePage = false
        return true
    }

    fun onNewApp(installedApp: InstalledApp) {
        val lastAdapter = recyclersAdapters.last()
//        if (lastAdapter.getData().size == visibleApplicationsOnScreen) {
//            canCreatePage = true
//            createPage()
//            onNewApp(installedApp)
//        } else {
//            lastAdapter.addItem(installedApp)
        viewModel.addItem(
            installedApp,
            lastAdapter.itemCount - 1,
            recyclersAdapters.size - 1
        )
//        }
    }

    fun onRemovedApp(packageName: String, onSwap: (Int) -> Unit) {
//        var adapterWithCurrentPackageKey = -1
//        recyclersAdapters.forEachIndexed { index, it ->
//            if (it.getData().any { it.app.get()?.packageName == packageName }) {
//                adapterWithCurrentPackageKey = index
//                return@forEachIndexed
//            }
//        }
//
        viewModel.deletePackage(packageName)
//
//        if (adapterWithCurrentPackageKey == -1) return
//        val currentAdapter = recyclersAdapters[adapterWithCurrentPackageKey]
//
//        currentAdapter.remove { it.app.get()?.packageName == packageName }
//        if (currentAdapter.getData().none { it.app.get() !== null }) {
//            if (adapterWithCurrentPackageKey == 0) onSwap(1)
//            else onSwap(-1)
//            Handler(Looper.getMainLooper()).postDelayed(200) {
//                try {
//                    removePage(adapterWithCurrentPackageKey)
//                } catch (e: Exception) {
//
//                }
//            }
//        }
        listAppPages.forEach { it.forEach { cell ->
            if (cell.app.get()?.packageName == packageName) {
                cell.app.set(null)
            }
        } }
        for(page in listAppPages.indices.reversed()) {
            if (pageIsEmpty(page)) removePage(page)
            else break
        }
    }

    private fun insertItemToLastPosition(dragInfo: DragInfo, currentPage: Int) {
        val oldPage = dragInfo.currentPage
//        val adapter = getCurrentAppListAdapter(currentPage)

        if (dragInfo.removeFromOriginalPlace) dragInfo.removeItem()

//        dragInfo.cell = adapter
        dragInfo.currentPage = currentPage
//
//        adapter.addItem(dragInfo.draggedItem)

        val cells = recyclersAdapters[currentPage].getData()
        cells.dropLastWhile {
            val size = cells.size
            (size > 1 || cells[size - 2].app.get() === null) && it.app.get() === null
        }

        cells.last { it.app.get() === null }.also { dragInfo.cell = it }.app.set(dragInfo.draggedItem)

        viewModel.saveNewPositionItem(dragInfo.draggedItem, dragInfo.draggedItemPos, currentPage)
        dragInfo.updateItemPosition()
        if (oldPage > currentPage && pageIsEmpty(oldPage)) removePage(oldPage)
    }

    fun insertItemToPosition(currentPage: Int, position: Int, dragInfo: DragInfo) {
        val currentAdapter = getCurrentAppListAdapter(currentPage)
        val oldPosition = dragInfo.draggedItemPos
        swapHelper.requestInsertToPosition(currentPage, position) {
            if (currentAdapter.getData().filter { it.app.get() !== null }.size == visibleApplicationsOnScreen)
                moveItems(currentPage + 1, currentAdapter.removeLastItem()?.app?.get())
            dragInfo.removeItem()
            currentAdapter.getData()[position].app.set(dragInfo.draggedItem)
            dragInfo.cell = currentAdapter.getData()[position]
            dragInfo.currentPage = currentPage
            dragInfo.updateItemPosition()
//            dragInfo.cell?.app?.set(dragInfo.draggedItem)
            updateItems(recyclersAdapters[currentPage], oldPosition, position)
            saveItemPositionsOnPage(currentAdapter.getData(), currentPage)
        }
    }

    private fun moveItems(newAdapterPage: Int, lastItem: InstalledApp?) {
        lastItem ?: return
        if (recyclersAdapters.getOrNull(newAdapterPage) == null) createPage()
        val newAdapter = getCurrentAppListAdapter(newAdapterPage)
//        newAdapter.addItem(0, lastItem)
        newAdapter.getData()[0].app.set(lastItem)
        saveItemPositionsOnPage(newAdapter.getData(), newAdapterPage)
//        if (newAdapter.itemCount > visibleApplicationsOnScreen) {
//            moveItems(newAdapterPage + 1, newAdapter.removeLastItem()?.app?.get())
//        }
    }

    fun insertAndMoveOtherForward(dragInfo: DragInfo, targetPage: Int, targetPosition: Int) {
        val itemsToMove = recyclersAdapters
            .flatMap { it.getData() }
            .dropWhile { it.page != targetPage || it.position != targetPosition }
            .takeWhile { it.app.get() !== null }

        val needToCreateNewPage = with(itemsToMove) {
            last().page == listAppPages.size - 1 && last().position == visibleApplicationsOnScreen - 1
        }
        swapHelper.requestInsertToPosition(targetPage, targetPosition) {
            if (needToCreateNewPage) createPage()
            moveBy1(itemsToMove)
            val targetCell = recyclersAdapters[targetPage].getData()[targetPosition]
            targetCell.app.set(dragInfo.draggedItem)
            dragInfo.cell = targetCell
            dragInfo.updateItemPosition()
        }
    }

    private fun moveBy1(items: List<DesktopCell>) {
        if (items.isEmpty()) return
        val currentCell = items.last()
        val newPage = currentCell.page.takeIf { currentCell.position < visibleApplicationsOnScreen - 1 } ?: (currentCell.page + 1)
        val newPosition = 0.takeIf { currentCell.position == visibleApplicationsOnScreen - 1 } ?: (currentCell.position + 1)
        val newCell = recyclersAdapters[newPage].getData()[newPosition]
        val app = currentCell.app.get()
//        swapHelper.requestInsertToPosition(newPage, newPosition) {
        currentCell.app.set(null)
        newCell.app.set(app)
            moveBy1(items.dropLast(1))
//        }
    }

    private fun saveItemPositionsOnPage(items: List<DesktopCell>, newAdapterPage: Int) {
        items.forEach { cell ->
            viewModel.saveNewPositionItem(cell.app.get(), cell.position, newAdapterPage)
        }
    }

    fun checkForRemovePage(currentPage: Int, onSwap: (Int) -> Unit) {
        if (currentPage == 0) return
        if (currentPage == count - 1 && pageIsEmpty(currentPage)) {
            removePage(currentPage)
            if (currentPage == 0) onSwap(1)
            else onSwap(-1)
        }
    }

    override fun getItemPosition(`object`: Any): Int {
        return POSITION_NONE
    }

    fun getAdapterIndex(targetAdapter: BaseAdapter<DesktopCell, LauncherItemApplicationBinding>): Int {
        return recyclersAdapters.indexOfFirst { it === targetAdapter }
    }

    fun removeShortcut(dragInfo: DragInfo) {
        dragInfo.removeItem()
        dragInfo.currentPage.takeIf { it >= 0 }?.let {
            updateItems(recyclersAdapters[it], dragInfo.draggedItemPos)
            saveItemPositionsOnPage(listOfNotNull(dragInfo.cell), it)
            if (pageIsEmpty(it) && it == recyclersAdapters.lastIndex) removePage(it)
        }
    }

}