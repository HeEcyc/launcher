package com.iosapp.ioslauncher.ui.custom

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class ArrowView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : ConstraintLayout(context, attrs, defStyle) {

    var state
        get() = impl.state
        set(value) { impl.state = value }

    private val impl: ArrowViewImpl = ArrowViewImpl(context, attrs, defStyle)

    init {
        addView(
            impl,
            LayoutParams(LayoutParams.MATCH_CONSTRAINT, LayoutParams.MATCH_CONSTRAINT).apply {
                topToTop = LayoutParams.PARENT_ID
                bottomToBottom = LayoutParams.PARENT_ID
                startToStart = LayoutParams.PARENT_ID
                endToEnd = LayoutParams.PARENT_ID
                dimensionRatio = "1:1"
            }
        )
    }

    private class ArrowViewImpl @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyle: Int = 0
    ) : View(context, attrs, defStyle) {

        private val thickness get() = width / 10f
        private val radius get() = thickness / 2f
        private val length get() = width / 2f - radius

        private val centerX get() = width / 2f
        private val centerY get() = width / 2f

        private val paintCircle = Paint().apply { color = Color.WHITE }
        private val paintLine by lazy {
            Paint().apply {
                color = Color.WHITE
                strokeWidth = thickness
            }
        }

        companion object {
            private const val PIf = PI.toFloat()
        }

        /**
         * 0 - wings horizontally
         * 1 - wings up
         * -1 - wings down
        */
        var state: Float = 0f
            set(value) {
                field =
                    if (value > 1f) 1f
                    else if (value < -1) -1f
                    else value
                invalidate()
            }

        override fun onDraw(canvas: Canvas) {
            canvas.drawCircle(centerX, centerY, radius, paintCircle)
            val angleOffset = (PIf / 4 * state)
            val angleLeft = PIf + angleOffset
            val xLeft = centerX + length * cos(angleLeft)
            val yLeft = centerY + length * sin(angleLeft)
            canvas.drawLine(centerX, centerY, xLeft, yLeft, paintLine)
            canvas.drawCircle(xLeft, yLeft, radius, paintCircle)
            val angleRight =
                if (state > 0)
                    -angleOffset
                else if (state < 0)
                    2 * PIf - angleOffset
                else
                    0f
            val xRight = centerX + length * cos(angleRight)
            val yRight = centerY + length * sin(angleRight)
            canvas.drawLine(centerX, centerY, xRight, yRight, paintLine)
            canvas.drawCircle(xRight, yRight, radius, paintCircle)
        }

    }

}