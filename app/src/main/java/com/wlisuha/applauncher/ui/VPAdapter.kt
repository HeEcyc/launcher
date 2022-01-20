package com.wlisuha.applauncher.ui

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.PagerAdapter
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
    private val listApplicationPackages: List<InstalledApp>,
    private val visibleApplicationsOnScreen: Int,
    private val onItemClicked: (InstalledApp) -> Unit,
) : PagerAdapter() {

    private val swapHelper = SwapHelper(Handler(Looper.getMainLooper()))
    private var recyclers = mutableMapOf<Int, RecyclerView>()

    private var pagesCount = listApplicationPackages.size / visibleApplicationsOnScreen

    init {
        if (listApplicationPackages.size % visibleApplicationsOnScreen != 0)
            pagesCount++
    }

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
            initItems = getItems(position)
            onItemClick = onItemClicked::invoke
            onBind = { item, binding, adapter ->
                binding.root.setOnLongClickListener {
                    startDragAndDrop(item, binding, adapter)
                    false
                }
            }
        }

    @SuppressLint("NewApi")
    private fun startDragAndDrop(
        item: InstalledApp,
        binding: LauncherItemApplicationBinding,
        adapter: BaseAdapter<InstalledApp, *>
    ) {
        binding.root.startDragAndDrop(
            null,
            View.DragShadowBuilder(binding.root),
            DragInfo(adapter, adapter.getData().indexOf(item), item),
            0
        )
    }

    private fun getItems(position: Int): List<InstalledApp> {
        val itemsStartRange = position * visibleApplicationsOnScreen
        val itemsEndRange = itemsStartRange + visibleApplicationsOnScreen
        return listApplicationPackages.subList(
            itemsStartRange,
            if (itemsEndRange > listApplicationPackages.size) listApplicationPackages.size
            else itemsEndRange
        )
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as View)
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view === `object` as View
    }

    fun swapItem(dragInfo: DragInfo, newPosition: Int) {
        swapHelper.requestToSwap(dragInfo.getCurrentItemPosition(), newPosition) {
            moveItem(dragInfo, newPosition)
        }
    }

    private fun moveItem(dragInfo: DragInfo, newPosition: Int) {
        val currentItemPosition = dragInfo.getCurrentItemPosition()
        if (currentItemPosition == -1) {
            swapHelper.removeRequestToSwap()
            return
        }

        val adapter = dragInfo.adapter
        val oldList = adapter.getData().toList()

        dragInfo.draggedItemPos = newPosition

        Collections.swap(adapter.getData(), currentItemPosition, newPosition)
        DiffUtil.calculateDiff(AppListDiffUtils(oldList, adapter.getData()))
            .dispatchUpdatesTo(adapter)

    }

    private fun isSwapInSameAdapter(item: InstalledApp, adapter: BaseAdapter<*, *>) =
        adapter.getData()
            .map { it as InstalledApp }
            .any { it.packageName == item.packageName }


    fun getCurrentAppListView(page: Int) = recyclers[page]

    fun removeRequestToSwap() {
        swapHelper.removeRequestToSwap()
    }
}