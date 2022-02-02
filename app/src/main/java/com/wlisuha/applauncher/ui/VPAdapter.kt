package com.wlisuha.applauncher.ui

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.core.os.postDelayed
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.PagerAdapter
import com.wlisuha.applauncher.BR
import com.wlisuha.applauncher.R
import com.wlisuha.applauncher.base.BaseAdapter
import com.wlisuha.applauncher.base.createAdapter
import com.wlisuha.applauncher.data.DragInfo
import com.wlisuha.applauncher.data.InstalledApp
import com.wlisuha.applauncher.databinding.LauncherItemApplicationBinding
import com.wlisuha.applauncher.utils.AppListDiffUtils
import com.wlisuha.applauncher.utils.SwapHelper
import java.util.*


class VPAdapter(
    private val listAppPages: MutableList<List<InstalledApp>>,
    private val visibleApplicationsOnScreen: Int,
    private val viewModel: AppViewModel,
) : PagerAdapter() {

    private val swapHelper = SwapHelper(Handler(Looper.getMainLooper()))
    private var recyclers = mutableListOf<RecyclerView>()
    private var recyclersAdapters = mutableListOf<BaseAdapter<InstalledApp, *>>()
    private var canCreatePage = false

    override fun getCount() = listAppPages.size

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        return RecyclerView(container.context).apply {
            overScrollMode = View.OVER_SCROLL_NEVER
            adapter = createAdapter(position)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            layoutManager = GridLayoutManager(context, 4)
            recyclers.add(position, this)
            container.addView(this)
        }
    }

    private fun createAdapter(position: Int) =
        createAdapter<InstalledApp, LauncherItemApplicationBinding>(R.layout.launcher_item_application) {
            initItems = listAppPages.getOrNull(position) ?: listOf()
            onItemClick = { viewModel.launchApp(it.packageName) }
            onBind = { item, binding, adapter ->

                binding.setVariable(BR.viewModel, viewModel)
                binding.notifyPropertyChanged(BR.viewModel)

                binding.root.setOnLongClickListener {
                    viewModel.isSelectionEnabled.set(true)
                    startDragAndDrop(item, binding, adapter, position)
                    false
                }
            }
        }.apply { recyclersAdapters.add(position, this) }

    @SuppressLint("NewApi")
    private fun startDragAndDrop(
        item: InstalledApp,
        binding: LauncherItemApplicationBinding,
        adapter: BaseAdapter<InstalledApp, *>,
        position: Int
    ) {
        canCreatePage = adapter.getData().size > 1
        binding.root.startDragAndDrop(
            null,
            View.DragShadowBuilder(binding.root),
            DragInfo(adapter, adapter.getData().indexOf(item), position, item),
            0
        )
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as View)
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view === `object` as View
    }

    fun swapItem(dragInfo: DragInfo, newPosition: Int, currentPage: Int) {
        if (isSwapInSameAdapter(dragInfo.draggedItem, currentPage))
            swapItemInSamePage(dragInfo, newPosition, currentPage)
        else swapItemBetweenPages(dragInfo, newPosition, currentPage)
    }

    private fun swapItemBetweenPages(dragInfo: DragInfo, newItemPosition: Int, currentPage: Int) {
        val currentAdapter = getCurrentAppListAdapter(currentPage)
        val swappedItem = currentAdapter.getData()[newItemPosition]

        swapHelper.requestToSwapBetweenPages(
            newItemPosition,
            dragInfo.draggedItemPos,
            dragInfo.currentPage,
            currentPage
        ) {

            currentAdapter.addItem(newItemPosition, dragInfo.draggedItem)
            dragInfo.adapter.addItem(dragInfo.draggedItemPos, swappedItem)

            dragInfo.removeItem()
            currentAdapter.removeItem(swappedItem)

            dragInfo.adapter = currentAdapter
            dragInfo.currentPage = currentPage

            dragInfo.updateItemPosition()
        }
    }

    private fun swapItemInSamePage(dragInfo: DragInfo, newPosition: Int, currentPage: Int) {
        val oldItemPosition = dragInfo.getCurrentItemPosition()
        swapHelper.requestToSwapInSomePage(oldItemPosition, newPosition) {

            moveItem(dragInfo, newPosition)

            val swappedItem = getCurrentAppListAdapter(currentPage).getData()[newPosition]

            viewModel.saveNewPositionItem(dragInfo.draggedItem, newPosition, currentPage)
            viewModel.saveNewPositionItem(swappedItem, oldItemPosition, currentPage)
        }
    }

    private fun moveItem(dragInfo: DragInfo, newPosition: Int) {
        val currentItemPosition = dragInfo.getCurrentItemPosition()
        if (currentItemPosition == -1) {
            swapHelper.clearRequest()
            return
        }

        val adapter = dragInfo.adapter
        val oldList = adapter.getData().toList()

        dragInfo.draggedItemPos = newPosition

        Collections.swap(adapter.getData(), currentItemPosition, newPosition)
        updateItems(oldList, adapter)

    }

    private fun updateItems(oldList: List<InstalledApp>, adapter: BaseAdapter<InstalledApp, *>) {
        DiffUtil.calculateDiff(AppListDiffUtils(oldList, adapter.getData()))
            .dispatchUpdatesTo(adapter)
    }

    private fun isSwapInSameAdapter(item: InstalledApp, currentPage: Int) =
        getCurrentAppListAdapter(currentPage)
            .getData().any { it.packageName == item.packageName }

    fun getCurrentAppListView(page: Int) = recyclers[page]

    private fun getCurrentAppListAdapter(page: Int) = recyclersAdapters[page]!!

    fun clearRequests() {
        swapHelper.clearRequest()
    }

    fun insertToLastPosition(dragInfo: DragInfo, currentPage: Int, withDelay: Boolean) {
        with(getCurrentAppListAdapter(currentPage)) {
            if (itemCount == visibleApplicationsOnScreen) return
            else getData().lastOrNull()
                ?.packageName
                ?.let { if (it == dragInfo.draggedItem.packageName) return }
        }

        if (!withDelay) insertItemToLastPosition(dragInfo, currentPage)
        else swapHelper.requestInsertToLastPosition(currentPage) {
            insertItemToLastPosition(dragInfo, currentPage)
        }
    }

    private fun pageIsEmpty(oldPage: Int) =
        if (oldPage == -1) false
        else getCurrentAppListAdapter(oldPage).getData().isEmpty()

    private fun removePage(oldPage: Int) {
        listAppPages.removeAt(oldPage)
        recyclersAdapters.removeAt(oldPage)
        recyclers.removeAt(oldPage)
        notifyDataSetChanged()
    }

    fun createPage(): Boolean {
        if (!canCreatePage) return false
        listAppPages.add(listOf())
        notifyDataSetChanged()
        canCreatePage = false
        return true
    }

    fun onNewApp(installedApp: InstalledApp) {
        val lastAdapter = recyclersAdapters.last()
        if (lastAdapter.getData().size == visibleApplicationsOnScreen) {
            canCreatePage = true
            createPage()
            onNewApp(installedApp)
        } else {
            lastAdapter.addItem(installedApp)
            viewModel.addItem(
                installedApp,
                lastAdapter.itemCount - 1,
                recyclersAdapters.size - 1
            )
        }
    }

    fun onRemovedApp(packageName: String, onSwap: (Int) -> Unit) {
        var adapterWithCurrentPackageKey = -1
        recyclersAdapters.forEachIndexed { index, it ->
            if (it.getData().any { it.packageName == packageName }) {
                adapterWithCurrentPackageKey = index
                return@forEachIndexed
            }
        }
        val currentAdapter = recyclersAdapters[adapterWithCurrentPackageKey] ?: return

        currentAdapter.remove { it.packageName == packageName }
        viewModel.deletePackage(packageName)
        if (currentAdapter.getData().isEmpty()) {
            if (adapterWithCurrentPackageKey == 0) onSwap(1)
            else onSwap(-1)
            Handler(Looper.getMainLooper()).postDelayed(200) {
                removePage(adapterWithCurrentPackageKey)
            }
        }
    }

    private fun insertItemToLastPosition(dragInfo: DragInfo, currentPage: Int) {
        val oldPage = dragInfo.currentPage
        val adapter = getCurrentAppListAdapter(currentPage)
        val oldList = adapter.getData().toList()
        dragInfo.removeItem()
        dragInfo.adapter = adapter
        dragInfo.currentPage = currentPage
        viewModel.saveNewPositionItem(dragInfo.draggedItem, dragInfo.draggedItemPos, currentPage)
        dragInfo.updateItemPosition()
        adapter.getData().add(dragInfo.draggedItem)
        updateItems(oldList, adapter)
        if (pageIsEmpty(oldPage)) removePage(oldPage)
    }

    fun insertItemToPosition(currentPage: Int, position: Int, dragInfo: DragInfo) {
        val currentAdapter = getCurrentAppListAdapter(currentPage)
        swapHelper.requestInsertToPosition(currentPage, position) {
            if (currentAdapter.getData().size == visibleApplicationsOnScreen)
                moveItems(currentPage + 1, currentAdapter.removeLastItem())
            dragInfo.removeItem()
            currentAdapter.addItem(position, dragInfo.draggedItem)
            dragInfo.adapter = currentAdapter
            dragInfo.currentPage = currentPage
            dragInfo.updateItemPosition()
            Log.d("12345", currentAdapter.getData().last().packageName)
            Log.d("12345", dragInfo.draggedItem.packageName)
            viewModel.saveNewPositionItem(dragInfo.draggedItem, position, currentPage)
        }
    }

    private fun moveItems(newAdapterPage: Int, lastItem: InstalledApp?) {
        lastItem ?: return
        if (recyclersAdapters.getOrNull(newAdapterPage) == null) createPage()
        val newAdapter = getCurrentAppListAdapter(newAdapterPage)
        newAdapter.addItem(0, lastItem)
        if (newAdapter.itemCount > visibleApplicationsOnScreen) {
            moveItems(newAdapterPage + 1, newAdapter.removeLastItem())
        }
    }

}