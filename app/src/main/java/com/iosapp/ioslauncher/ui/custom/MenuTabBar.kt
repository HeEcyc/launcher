package com.iosapp.ioslauncher.ui.custom

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.databinding.ObservableBoolean
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.iosapp.ioslauncher.R
import com.iosapp.ioslauncher.base.createAdapter
import com.iosapp.ioslauncher.databinding.ItemMenuTabBinding
import com.iosapp.ioslauncher.ui.app.MenuAdapter
import kotlin.math.min

class MenuTabBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : ConstraintLayout(context, attrs, defStyle) {

    val onItemClick = MutableLiveData<MenuTab>()

    private val recyclerView = RecyclerView(context, attrs)
    private val underlineView = UnderlineView(context, attrs, defStyle)

    private val adapter = createAdapter<MenuTab, ItemMenuTabBinding>(R.layout.item_menu_tab) {
        onItemClick = {

            this@MenuTabBar.onItemClick.postValue(it)
        }
        onBind = { tab, binding, adapter ->
            if (tab.isSelected.get()) {
//                underlineView.layoutParams.width = binding.title.width
                underlineView.layoutParams = LayoutParams(binding.title.width, LayoutParams.MATCH_CONSTRAINT).apply {
                    bottomToBottom = LayoutParams.PARENT_ID
                    startToStart = LayoutParams.PARENT_ID
                    marginStart = binding.title.xDirectionIndependent.toInt()
                    matchConstraintPercentHeight = 0.1f
                }
            }
        }
    }

    init {
        underlineView.id = View.generateViewId()
        recyclerView.id = View.generateViewId()
        addView(underlineView, LayoutParams(0, LayoutParams.MATCH_CONSTRAINT).apply {
            bottomToBottom = LayoutParams.PARENT_ID
            startToStart = LayoutParams.PARENT_ID
            matchConstraintPercentHeight = 0.1f
        })
        addView(recyclerView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        recyclerView.layoutManager = LinearLayoutManager(context).apply { orientation = LinearLayoutManager.HORIZONTAL }
        recyclerView.adapter = adapter
    }

    fun reloadItems(categories: List<MenuAdapter.MenuCategory>) {
        adapter.reloadData(categories.mapIndexed { index, menuCategory ->
            MenuTab(menuCategory.categoryTitle, index == 0)
        })
        if (categories.isNotEmpty())
            recyclerView.scrollToPosition(0)
    }

    fun bindMenuAdapter(menuAdapter: MenuAdapter) {
        reloadItems(menuAdapter.getData())
        menuAdapter.onDataSetChanged.observe(findViewTreeLifecycleOwner()!!) {
            reloadItems(menuAdapter.getData())
        }
    }

    class MenuTab(val name: String, isSelected: Boolean = false) {
        val isSelected = ObservableBoolean(isSelected)
    }

    private class UnderlineView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyle: Int = 0
    ) : View(context, attrs, defStyle) {

        private val radius get() = min(height, width) / 2f
        private val centerY get() = height / 2f

        private val paint = Paint().apply {
            color = Color.parseColor("#4996E3")
        }

        override fun onDraw(canvas: Canvas) {
            paint.strokeWidth = radius * 2f
            canvas.drawLine(radius, centerY, width - radius, centerY, paint)
            canvas.drawCircle(radius, centerY, radius, paint)
            canvas.drawCircle(width - radius, centerY, radius, paint)
        }

    }

    private val View.xDirectionIndependent: Float
        get() = if (layoutDirection == View.LAYOUT_DIRECTION_LTR) x else this@MenuTabBar.width - x

}