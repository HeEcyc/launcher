package com.iosapp.ioslauncher.ui.custom

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.animation.doOnEnd
import androidx.core.view.children
import androidx.core.view.marginStart
import androidx.databinding.ObservableBoolean
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
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

    private val tabHorizontalSpace get() = recyclerView.height / 4

    private val adapter = createAdapter<MenuTab, ItemMenuTabBinding>(R.layout.item_menu_tab) {
        onItemClick = ::onTabClick
        onBind = { tab, binding, adapter ->
            if (tab.isSelected.get()) {
                binding.root.post {
                    underlineView.layoutParams = provideUnderlineLayoutParams(
                        binding.title.width,
                        binding.root.xDirectionIndependent.toInt() + tabHorizontalSpace
                    )
                }
            }
        }
    }

    init {
        underlineView.id = View.generateViewId()
        recyclerView.id = View.generateViewId()
        addView(underlineView, provideUnderlineLayoutParams(0))
        addView(recyclerView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        recyclerView.layoutManager = LinearLayoutManager(context).apply { orientation = LinearLayoutManager.HORIZONTAL }
        recyclerView.adapter = adapter
        recyclerView.overScrollMode = OVER_SCROLL_NEVER
        post {
            val margin = (height / 4 * 1.5f).toInt()
            val isLTR = layoutDirection == LAYOUT_DIRECTION_LTR
            recyclerView.addItemDecoration(ItemDecorationWithEnds(
                leftFirst = margin,
                rightLast = margin,
                firstPredicate = { p -> isLTR && p == 0 },
                lastPredicate = { p, c -> isLTR && p == c - 1 }
            ))
            recyclerView.addItemDecoration(ItemDecorationWithEnds(
                leftLast = margin,
                rightFirst = margin,
                firstPredicate = { p -> !isLTR && p == 0 },
                lastPredicate = { p, c -> !isLTR && p == c - 1 }
            ))
        }
        recyclerView.addOnScrollListener(object : OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) = animateUnderlineScroll()
        })
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

    private fun onTabClick(tab: MenuTab) {
        val movingForward = adapter.getData().indexOfFirst { it.isSelected.get() } < adapter.getData().indexOf(tab)
        adapter.getData().forEach { it.isSelected.set(it === tab) }
        animateUnderlineClick(movingForward)
        onItemClick.postValue(tab)
    }

    private var underlineAnimator: ValueAnimator? = null
    private fun animateUnderlineClick(movingForward: Boolean) {
        underlineAnimator?.cancel()
        val selectedTabIndex = adapter.getData().indexOfFirst { it.isSelected.get() }
        val viewsWithTabIndices = recyclerView.children.map { it to recyclerView.getChildAdapterPosition(it) }
        val selectedTabView = viewsWithTabIndices.firstOrNull { it.second == selectedTabIndex }?.first
        if (selectedTabView === null) return
        val title = selectedTabView.findViewById<View>(R.id.title)
        val initialWidth = underlineView.width
        val initialMarginStart = underlineView.marginStart
        val newWidthStep1: Int
        val newMarginStartStep1: Int
        val newWidthStep2 = title.width
        val newMarginStartStep2 = selectedTabView.xDirectionIndependent.toInt() + tabHorizontalSpace
        if (movingForward) {
            newWidthStep1 = selectedTabView.xDirectionIndependent.toInt() + tabHorizontalSpace - initialMarginStart
            newMarginStartStep1 = initialMarginStart
        } else {
            newWidthStep1 = initialWidth + initialMarginStart - selectedTabView.xDirectionIndependent.toInt() - tabHorizontalSpace
            newMarginStartStep1 = selectedTabView.xDirectionIndependent.toInt() + tabHorizontalSpace
        }
        val widthStep1Delta = newWidthStep1 - initialWidth
        val marginStartStep1Delta = newMarginStartStep1 - initialMarginStart
        val widthStep2Delta = newWidthStep2 - newWidthStep1
        val marginStartStep2Delta = newMarginStartStep2 - newMarginStartStep1
        underlineAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            interpolator = LinearInterpolator()
            duration = 25
            addUpdateListener {
                val progressStep1 = it.animatedValue as Float
                underlineView.layoutParams = provideUnderlineLayoutParams(
                    initialWidth + (widthStep1Delta * progressStep1).toInt(),
                    initialMarginStart + (marginStartStep1Delta * progressStep1).toInt()
                )
            }
            doOnEnd {
                underlineAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
                    interpolator = LinearInterpolator()
                    duration = 25
                    addUpdateListener {
                        val progressStep2 = it.animatedValue as Float
                        underlineView.layoutParams = provideUnderlineLayoutParams(
                            newWidthStep1 + (widthStep2Delta * progressStep2).toInt(),
                            newMarginStartStep1 + (marginStartStep2Delta * progressStep2).toInt()
                        )
                    }
                    doOnEnd { underlineAnimator = null }
                    start()
                }
            }
            start()
        }
    }

    private fun animateUnderlineScroll() {
        val selectedTabIndex = adapter.getData().indexOfFirst { it.isSelected.get() }
        val viewsWithTabIndices = recyclerView.children.map { it to recyclerView.getChildAdapterPosition(it) }
        val selectedTabView = viewsWithTabIndices.firstOrNull { it.second == selectedTabIndex }?.first
        if (selectedTabIndex !in viewsWithTabIndices.map { it.second }) {
            underlineView.layoutParams = provideUnderlineLayoutParams(0)
        } else if(selectedTabView !== null) {
            val title = selectedTabView.findViewById<View>(R.id.title)
            underlineView.layoutParams = provideUnderlineLayoutParams(
                title.width,
                selectedTabView.xDirectionIndependent.toInt() + tabHorizontalSpace
            )
        }
    }

    private fun provideUnderlineLayoutParams(
        width: Int,
        marginStart: Int = underlineView.marginStart
    ): LayoutParams {
        val defaultParams =
            underlineView.layoutParams as? LayoutParams ?: LayoutParams(width, LayoutParams.MATCH_CONSTRAINT).apply {
                bottomToBottom = LayoutParams.PARENT_ID
                startToStart = LayoutParams.PARENT_ID
                matchConstraintPercentHeight = 0.05f
            }
        return LayoutParams(defaultParams).apply {
            this.width = width
            this.marginStart = marginStart
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