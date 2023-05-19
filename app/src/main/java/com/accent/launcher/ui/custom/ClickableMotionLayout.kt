package com.accent.launcher.ui.custom

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.constraintlayout.motion.widget.MotionLayout
import kotlinx.coroutines.*

class ClickableMotionLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : MotionLayout(context, attrs, defStyle), CoroutineScope by MainScope() {
    var canCallLongCLick = true

    private var longClick: OnLongClickListener? = null
    var longClickTask: Job? = null
    private var touchRect = Rect()

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_MOVE -> {
                if (!touchRect.contains(event.x.toInt(), event.y.toInt())) {
                    longClickTask?.cancel()
                }
                return super.onInterceptTouchEvent(event)
            }
            MotionEvent.ACTION_DOWN -> {
                canCallLongCLick = true

                val top = event.y.toInt() - 150
                val bottom = event.y.toInt() + 150
                val left = event.x.toInt() - 150
                val right = event.x.toInt() + 150
                touchRect.set(top, left, bottom, right)

                longClickTask = launch(Dispatchers.IO) {
                    delay(2000)
                    callLongClick()
                }
            }
            MotionEvent.ACTION_UP -> {
                if (10 < event.eventTime - event.downTime) callLongClick()
                longClickTask?.cancel()
            }
        }
        return false
    }

    private fun callLongClick() {
        if (!canCallLongCLick) return
        canCallLongCLick = false
        longClick?.onLongClick(this)
    }

    override fun setOnLongClickListener(longClick: OnLongClickListener?) {
        this.longClick = longClick
    }
}