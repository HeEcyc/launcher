package com.iosapp.ioslauncher.ui.app

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
import com.iosapp.ioslauncher.BR
import com.iosapp.ioslauncher.LauncherApplication
import com.iosapp.ioslauncher.R
import com.iosapp.ioslauncher.base.*
import com.iosapp.ioslauncher.data.DesktopCell
import com.iosapp.ioslauncher.data.DragInfo
import com.iosapp.ioslauncher.data.InstalledApp
import com.iosapp.ioslauncher.databinding.LauncherItemApplicationMenuBinding
import com.iosapp.ioslauncher.ui.custom.ItemDecorationWithEnds
import com.iosapp.ioslauncher.utils.PAGE_INDEX_JUST_MENU

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
        return RecyclerView(container.context).apply {
            overScrollMode = View.OVER_SCROLL_NEVER
            adapter = recyclersAdapters.getOrElse(position) { createAdapter(position) }
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            layoutManager = GridLayoutManager(context, 4)
            recyclers.getOrNull(position)?.let { recyclers.set(position, this) } ?: recyclers.add(position, this)
            addOnScrollListener(getOnScrollListener(this))
            val navBarHeight = if (hasNavigationBar()) getNavBarSize() * 2 else findParentWithId(R.id.motionView, container)?.findViewById<View>(R.id.fakeNavBar)?.height ?: 0
            addItemDecoration(ItemDecorationWithEnds(
                bottomLast = navBarHeight,
                lastPredicate = { position, count ->
                    val countLast = count.rem(4).takeIf { it > 0 } ?: 4
                    position >= count - countLast
                }
            ))
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

    private fun hasNavigationBar(): Boolean {
        val id: Int = LauncherApplication.instance.resources.getIdentifier("config_showNavigationBar", "bool", "android")
        return id > 0 && LauncherApplication.instance.resources.getBoolean(id)
    }

    private fun getNavBarSize() =
        with(LauncherApplication.instance.resources.getIdentifier("navigation_bar_height", "dimen", "android")) {
            if (this != 0) LauncherApplication.instance.resources.getDimensionPixelSize(this)
            else 0
        }

    private fun findParentWithId(id: Int, view: View?): View? {
        if (view === null) return null
        return if (view.id == id)
            view
        else
            findParentWithId(id, view.parent as? View)
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
    private fun createAdapter(position: Int) = createAdapter<InstalledApp?, LauncherItemApplicationMenuBinding>(R.layout.launcher_item_application_menu) {
        initItems = categories[position].apps
        onItemClick = { if (it !== null) viewModel.launchApp(it.packageName) }
        onBind = { item, binding, adapter ->
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
                    viewModel.onMenuItemLongClick.postValue(Unit)
                    startDragAndDrop(item, binding, adapter, 0, false)
                }
                false
            }
        }
    }.apply { recyclersAdapters.add(position, this) }

    @SuppressLint("NewApi")
    private fun startDragAndDrop(
        item: InstalledApp,
        binding: LauncherItemApplicationMenuBinding,
        adapter: BaseAdapter<InstalledApp?, *>,
        position: Int,
        removeFromOriginalPlace: Boolean = true,
    ) {
//        viewModel.isSelectionEnabled.set(true)
//        canCreatePage = adapter.getData().size > 1
        binding.appIcon.startDragAndDrop(
            null,
            View.DragShadowBuilder(binding.appIcon),
            DragInfo(DesktopCell(adapter.getData().indexOf(item), PAGE_INDEX_JUST_MENU, item), adapter.getData().indexOf(item), position, item, removeFromOriginalPlace),
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