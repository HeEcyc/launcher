package com.wlisuha.applauncher.ui.custom

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import androidx.constraintlayout.motion.widget.MotionLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope

class ClickableMotionLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : MotionLayout(context, attrs, defStyle), CoroutineScope by MainScope() {
    private var canCallLongCLick = true
    private var longClick: OnLongClickListener? = null

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        Log.d("12345", "event")

        if (event.action == MotionEvent.ACTION_MOVE) {
            val eventDuration = event.eventTime - event.downTime
            if (eventDuration > 500 && canCallLongCLick) {
                canCallLongCLick = false
                longClick?.onLongClick(this)
            }
            return super.onInterceptTouchEvent(event)
        } else if (event.action == MotionEvent.ACTION_DOWN) {
            canCallLongCLick = true
        }
        return false
    }

    override fun setOnLongClickListener(longClick: OnLongClickListener?) {
        this.longClick = longClick
    }

}