package com.applauncher.applauncher.ui.custom

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
    private var longClickTask: Job? = null
    private var touchRect = Rect()

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_MOVE -> {
                val eventDuration = event.eventTime - event.downTime
                if (eventDuration > 1000 && canCallLongCLick) {
                    longClickTask?.cancel()
                    callLongClick()
                }
                if (!touchRect.contains(event.x.toInt(), event.y.toInt())) {
                    longClickTask?.cancel()
                    canCallLongCLick = false
                }
                return super.onInterceptTouchEvent(event)
            }
            MotionEvent.ACTION_DOWN -> {
                touchRect.set(
                    (event.x - 20).toInt(), (event.y - 20).toInt(),
                    (event.x + 20).toInt(), (event.y + 20).toInt()
                )
                canCallLongCLick = true
                longClickTask = launch(Dispatchers.IO) {
                    delay(1000)
                    if (canCallLongCLick) callLongClick()
                }
            }
            MotionEvent.ACTION_UP -> longClickTask?.cancel()
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