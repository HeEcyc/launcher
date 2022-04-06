package com.iosapp.ioslauncher.ui.custom

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.viewpager.widget.ViewPager

class NonSwipeableViewPager @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : ViewPager(context, attrs) {
    var canSwipe = true

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return if (canSwipe) {
            super.onTouchEvent(event)
        } else false
    }

    override fun onInterceptTouchEvent(event: MotionEvent?): Boolean {
        return if (canSwipe) {
            super.onInterceptTouchEvent(event)
        } else false
    }


    interface StateProvider {
        fun isPresentOnHomeScreen(): Boolean

        fun onAppSelected()
    }
}