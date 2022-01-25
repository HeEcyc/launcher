package com.wlisuha.applauncher.ui

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewGroup
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
    private var recyclers = mutableMapOf<Int, RecyclerView>()
    private var canCreatePage = false

//    init {
//        Handler(Looper.getMainLooper()).postDelayed({
//            pagesCount++
//            notifyDataSetChanged()
//        }, 2000)
//    }

    private var pagesCount = listAppPages.size

    override fun getCount() = pagesCount

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        return RecyclerView(container.context).apply {
            overScrollMode = View.OVER_SCROLL_NEVER
            adapter = createAdapter(position)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            layoutManager = GridLayoutManager(context, 4)
            recyclers[position] = this
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
        }

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
        DiffUtil.calculateDiff(AppListDiffUtils(oldList, adapter.getData()))
            .dispatchUpdatesTo(adapter)

    }

    private fun isSwapInSameAdapter(item: InstalledApp, currentPage: Int) =
        getCurrentAppListAdapter(currentPage)
            .getData().any { it.packageName == item.packageName }

    fun getCurrentAppListView(page: Int) = recyclers[page]

    private fun getCurrentAppListAdapter(page: Int) =
        recyclers[page]?.adapter as BaseAdapter<InstalledApp, *>

    fun removeRequestToSwap() {
        swapHelper.clearRequest()
    }

    fun insertToLastPosition(dragInfo: DragInfo, currentPage: Int) {
        val oldPage = dragInfo.currentPage
        with(getCurrentAppListAdapter(currentPage)) {
            if (itemCount == visibleApplicationsOnScreen) return
            else if (getData().any { it.packageName == dragInfo.draggedItem.packageName }) return
            dragInfo.removeItem()
            addItem(dragInfo.draggedItem)
            dragInfo.adapter = this
            dragInfo.currentPage = currentPage
            dragInfo.updateItemPosition()

            viewModel.saveNewPositionItem(
                dragInfo.draggedItem,
                dragInfo.draggedItemPos,
                currentPage
            )
            if (pageIsEmpty(oldPage)) removePage(oldPage)
        }
    }

    private fun pageIsEmpty(oldPage: Int) =
        if (oldPage == -1) false
        else getCurrentAppListAdapter(oldPage).getData().isEmpty()

    private fun removePage(oldPage: Int) {
        listAppPages.removeAt(oldPage)
        pagesCount--
        notifyDataSetChanged()
    }

    fun createPage(currentPage: Int): Boolean {

        val currentAdapter = getCurrentAppListAdapter(currentPage)

        if (getCurrentAppListAdapter(currentPage).getData().size < 1 && canCreatePage) return false
        pagesCount++
        listAppPages.add(listOf())
        notifyDataSetChanged()
        canCreatePage = false
        return true
    }
}