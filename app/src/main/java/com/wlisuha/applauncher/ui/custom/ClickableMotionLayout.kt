package com.wlisuha.applauncher.ui.custom

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.constraintlayout.motion.widget.MotionLayout
import kotlinx.coroutines.*

class ClickableMotionLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : MotionLayout(context, attrs, defStyle), CoroutineScope by MainScope() {
    private var canCallLongCLick = true
    private var longClick: OnLongClickListener? = null
    private var longClickTask: Job? = null

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_MOVE) {
            val eventDuration = event.eventTime - event.downTime
            if (eventDuration > 500 && canCallLongCLick) {
                longClickTask?.cancel()
                callLongClick()
            }
            return super.onInterceptTouchEvent(event)
        } else if (event.action == MotionEvent.ACTION_DOWN) {
            canCallLongCLick = true
            longClickTask = launch(Dispatchers.IO) {
                delay(500)
                callLongClick()
            }
        }
        return false
    }

    private fun callLongClick() {
        canCallLongCLick = false
        longClick?.onLongClick(this)
    }

    override fun setOnLongClickListener(longClick: OnLongClickListener?) {
        this.longClick = longClick
    }

}