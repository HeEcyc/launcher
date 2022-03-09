package com.applauncher.applauncher.ui.custom

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import androidx.constraintlayout.motion.widget.MotionLayout
import kotlinx.coroutines.*

class ClickableMotionLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : MotionLayout(context, attrs, defStyle), CoroutineScope by MainScope() {
    var canCallLongCLick = true
    var active = true

    private var longClick: OnLongClickListener? = null
    var longClickTask: Job? = null
    private var touchRect = Rect()

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_MOVE -> {
                if (!touchRect.contains(event.x.toInt(), event.y.toInt())) {
                    Log.d("12345", "cancel")
                    longClickTask?.cancel()
                    canCallLongCLick = false
                }
                return super.onInterceptTouchEvent(event)
            }
            MotionEvent.ACTION_DOWN -> {
                canCallLongCLick = true

                val top = event.y.toInt() - 15
                val bottom = event.y.toInt() + 15
                val left = event.x.toInt() - 15
                val right = event.x.toInt() + 15
                touchRect.set(top, left, bottom, right)

                longClickTask = launch(Dispatchers.IO) {
                    delay(2000)
                    callLongClick()
                }
            }
            MotionEvent.ACTION_UP -> {
                Log.d("12345", "up")
                if (10 < event.eventTime - event.downTime) callLongClick()
                canCallLongCLick = false


                longClickTask?.cancel()
            }
        }
        return false
    }

    private fun callLongClick() {
        if (!canCallLongCLick || !active) return
        canCallLongCLick = false
        longClick?.onLongClick(this)
    }

    override fun setOnLongClickListener(longClick: OnLongClickListener?) {
        this.longClick = longClick
    }
}