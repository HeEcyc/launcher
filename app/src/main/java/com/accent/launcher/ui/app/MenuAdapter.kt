package com.accent.launcher.ui.app

import android.annotation.SuppressLint
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.PagerAdapter
import com.accent.launcher.BR
import com.accent.launcher.R
import com.accent.launcher.base.*
import com.accent.launcher.data.DesktopCell
import com.accent.launcher.data.DragInfo
import com.accent.launcher.data.InstalledApp
import com.accent.launcher.databinding.LauncherItemApplicationMenuBinding
import com.accent.launcher.ui.custom.LauncherView
import com.accent.launcher.utils.PAGE_INDEX_JUST_MENU

class MenuAdapter(
    private val categories: MutableList<MenuCategory>,
    private val viewModel: AppViewModel
) : PagerAdapter() {

    val onDataSetChanged = MutableLiveData<Unit>()

    val mainCategory get() = recyclersAdapters.first().getData()
    val recentApps get() = mainCategory.take(8)

    private val recyclers = mutableListOf<RecyclerView>()
    private val recyclersAdapters = mutableListOf<BaseAdapter<InstalledApp?, *>>()

    fun getData(): MutableList<MenuCategory> = categories

    override fun getCount(): Int = categories.size

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val launcherView = getParentLauncherView(container)
        return RecyclerView(container.context).apply {
            overScrollMode = View.OVER_SCROLL_NEVER
            adapter = recyclersAdapters.getOrElse(position) { createAdapter(position, launcherView) }
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            layoutManager = GridLayoutManager(context, 4)
            recyclers.getOrNull(position)?.let { recyclers.set(position, this) } ?: recyclers.add(position, this)
            addOnScrollListener(getOnScrollListener(this))
            if (position == 0) {
                addItemDecoration(object : RecyclerView.ItemDecoration() {
                    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
                        val firstChildOfSecondRow = parent.children.firstOrNull { parent.getChildAdapterPosition(it) == 4 }
                        if (parent.childCount > 8 && firstChildOfSecondRow !== null)
                            c.drawLine(
                                parent.left.toFloat(),
                                firstChildOfSecondRow.y + firstChildOfSecondRow.height,
                                parent.right.toFloat(),
                                firstChildOfSecondRow.y + firstChildOfSecondRow.height + 1,
                                Paint().apply { color = Color.parseColor("#D4D4D4") }
                            )
                    }
                })
            }
            setOnDragListener { _, _ -> true }
            container.addView(this)
        }
    }

    private fun getOnScrollListener(targetRV: RecyclerView) = object : RecyclerView.OnScrollListener() {
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            if (targetRV.scrollState == RecyclerView.SCROLL_STATE_IDLE)
                targetRV.isNestedScrollingEnabled =
                    (targetRV.layoutManager?.let { it as GridLayoutManager }
                        ?.findFirstVisibleItemPosition() ?: 0) == 0
        }
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            targetRV.isNestedScrollingEnabled = false
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createAdapter(position: Int, launcherView: LauncherView?) = createAdapter<InstalledApp?, LauncherItemApplicationMenuBinding>(R.layout.launcher_item_application_menu) {
        initItems = categories[position].apps
        onItemClick = { if (it !== null) viewModel.launchApp(it.packageName) }
        onBind = { item, binding, adapter ->
            if (launcherView !== null)
                binding.root.setOnDragListener(launcherView)
            binding.isShortcut = false
            binding.root.setOnTouchListener { _, _ ->
                viewModel.disableMotionLayoutLongClick.postValue(Unit)
                false
            }
            binding.setVariable(BR.viewModel, viewModel)
            binding.label.setTextColor(Color.BLACK)
            binding.notifyPropertyChanged(BR.viewModel)
            binding.root.setOnLongClickListener {
                if (item !== null) {
//                    viewModel.onMenuItemLongClick.postValue(Unit)
                    startDragAndDrop(item, binding, adapter, 0, false, binding.root)
                }
                false
            }
        }
    }.apply { recyclersAdapters.add(position, this) }

    private fun getParentLauncherView(view: View?): LauncherView? {
        return if (view === null)
            null
        else if (view.parent is LauncherView)
            view.parent as LauncherView
        else
            getParentLauncherView(view.parent as? View)
    }

    @SuppressLint("NewApi")
    private fun startDragAndDrop(
        item: InstalledApp,
        binding: LauncherItemApplicationMenuBinding,
        adapter: BaseAdapter<InstalledApp?, *>,
        position: Int,
        removeFromOriginalPlace: Boolean = true,
        originView: View
    ) {
//        viewModel.isSelectionEnabled.set(true)
//        canCreatePage = adapter.getData().size > 1
        binding.appIcon.startDragAndDrop(
            null,
            View.DragShadowBuilder(binding.appIcon),
            DragInfo(DesktopCell(adapter.getData().indexOf(item), PAGE_INDEX_JUST_MENU, item), adapter.getData().indexOf(item), position, item, removeFromOriginalPlace, originView = originView),
            0
        )
        viewModel.showTopFields(item)
    }

    fun reloadData(newData: List<MenuCategory>) {
        categories.clear()
        categories.addAll(newData)
        notifyDataSetChanged()
    }

    override fun notifyDataSetChanged() {
        onDataSetChanged.postValue(Unit)
        super.notifyDataSetChanged()
        recyclersAdapters.onEach { it.notifyDataSetChanged() }
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view === `object` as? View
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as? View ?: return)
    }

    class MenuCategory(
        val categoryTitle: String,
        val apps: MutableList<InstalledApp?>
    )

}