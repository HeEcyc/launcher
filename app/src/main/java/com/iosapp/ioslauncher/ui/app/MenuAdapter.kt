package com.iosapp.ioslauncher.ui.app

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.PagerAdapter
import com.iosapp.ioslauncher.BR
import com.iosapp.ioslauncher.R
import com.iosapp.ioslauncher.base.*
import com.iosapp.ioslauncher.data.DesktopCell
import com.iosapp.ioslauncher.data.DragInfo
import com.iosapp.ioslauncher.data.InstalledApp
import com.iosapp.ioslauncher.databinding.LauncherItemApplicationMenuBinding
import com.iosapp.ioslauncher.utils.PAGE_INDEX_JUST_MENU

class MenuAdapter(
    private val categories: MutableList<MenuCategory>,
    private val viewModel: AppViewModel
) : PagerAdapter() {

    val onDataSetChanged = MutableLiveData<Unit>()

    private val recyclers = mutableListOf<RecyclerView>()
    private val recyclersAdapters = mutableListOf<BaseAdapter<InstalledApp, *>>()

    fun getData(): List<MenuCategory> = categories

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
            container.addView(this)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createAdapter(position: Int) = createAdapter<InstalledApp, LauncherItemApplicationMenuBinding>(R.layout.launcher_item_application_menu) {
        initItems = categories[position].apps
        onItemClick = { viewModel.launchApp(it.packageName) }
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
                viewModel.onMenuItemLongClick.postValue(Unit)
                startDragAndDrop(item, binding, adapter, 0, false)
                false
            }
        }
    }.apply { recyclersAdapters.add(position, this) }

    @SuppressLint("NewApi")
    private fun startDragAndDrop(
        item: InstalledApp,
        binding: LauncherItemApplicationMenuBinding,
        adapter: BaseAdapter<InstalledApp, *>,
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
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view === `object` as? View
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as? View ?: return)
    }

    class MenuCategory(
        val category: Int,
        val categoryTitle: String,
        val apps: MutableList<InstalledApp>
    )

}