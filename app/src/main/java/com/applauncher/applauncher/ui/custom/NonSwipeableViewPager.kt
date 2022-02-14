package com.applauncher.applauncher.ui.custom

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.viewpager.widget.ViewPager

class NonSwipeableViewPager @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : ViewPager(context, attrs) {
    var stateProvider: StateProvider? = null

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return if (stateProvider?.isPresentOnHomeScreen() == true) {
            super.onTouchEvent(event)
        } else false
    }

    override fun onInterceptTouchEvent(event: MotionEvent?): Boolean {
        return if (stateProvider?.isPresentOnHomeScreen() == true) {
            super.onInterceptTouchEvent(event)
        } else false
    }


    interface StateProvider {
        fun isPresentOnHomeScreen(): Boolean

        fun onAppSelected()
    }
}