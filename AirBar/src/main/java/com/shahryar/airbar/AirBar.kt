package com.shahryar.airbar

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import java.util.*

class AirBar(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private val mAttrs = context.obtainStyledAttributes(attrs, R.styleable.AirBar)
    private val mPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var mLeft = 0F
    private var mTop = 200F
    private var mRight = mLeft
    private var mBottom = 0F
    private var isVirgin = true
    private val mProgressRect = RectF()
    private var mListener: OnProgressChangedListener? = null
    private var initPercent = -1.0

    var max: Double = mAttrs.getInt(R.styleable.AirBar_max, 100).toDouble()
    var min: Double = mAttrs.getInt(R.styleable.AirBar_min, 0).toDouble()

    var progressBarFillColor: Int = mAttrs.getColor(
        R.styleable.AirBar_progressBarFillColor,
        resources.getColor(R.color.defaultLevel)
    )
        set(value) {
            field = value
            progressBarColor0 = value
            progressBarColor1 = value
            invalidate()
        }

    var backgroundCornerRadius: Float =
        mAttrs.getFloat(R.styleable.AirBar_backgroundCornerRadius, 50F)
        set(value) {
            field = value
            invalidate()
        }

    var backgroundFillColor: Int = mAttrs.getColor(
        R.styleable.AirBar_backgroundFillColor,
        resources.getColor(R.color.defaultBackground)
    )
        set(value) {
            field = value
            invalidate()
        }

    var icon: Drawable? = mAttrs.getDrawable(R.styleable.AirBar_icon)
        set(value) {
            field = value
            invalidate()
        }

    var progressBarColor0: Int =
        mAttrs.getColor(R.styleable.AirBar_progressBarColor0, progressBarFillColor)
        set(value) {
            field = value
            invalidate()
        }

    var progressBarColor1: Int =
        mAttrs.getColor(R.styleable.AirBar_progressBarColor1, progressBarFillColor)
        set(value) {
            field = value
            invalidate()
        }

    fun setOnProgressChangedListener(listener: OnProgressChangedListener) {
        mListener = listener
    }

    override fun setBackgroundColor(color: Int) {
        super.setBackgroundColor(color)
        invalidate()
    }

    /**
     * Draw background
     */
    override fun draw(canvas: Canvas?) {
        setBackgroundColor(backgroundFillColor)
        //Set rounded corner frame
        canvas?.clipPath(
            getRoundedRect(
                0F,
                0F,
                mRight,
                mBottom,
                backgroundCornerRadius,
                backgroundCornerRadius,
                true,
                true,
                true,
                true
            )
        )
        super.draw(canvas)
    }

    /**
     * Draw icon
     */
    override fun onDrawForeground(canvas: Canvas?) {
        val bitmap = icon?.toBitmap()
        if (bitmap != null && canvas != null) {
            val centerX: Float =
                canvas.width.toDouble().div(2.00).toFloat() - bitmap.width.toDouble().div(2.00)
                    .toFloat()
            canvas.drawBitmap(
                bitmap,
                centerX,
                mBottom - (bitmap.height.toDouble() * 1.5).toFloat(),
                mPaint
            )
        }
    }

    @SuppressLint("DrawAllocation")
    @RequiresApi(Build.VERSION_CODES.M)
    override fun onDraw(canvas: Canvas?) {

        mPaint.color = progressBarFillColor
        mPaint.style = Paint.Style.FILL
        mPaint.shader =
            LinearGradient(
                0F,
                0F,
                0F,
                height.toFloat(),
                progressBarColor0,
                progressBarColor1,
                Shader.TileMode.MIRROR
            )

        //First init of level rect
        if (isVirgin) {
            mLeft = 0F
            mTop = if (initPercent != -1.0) getTopPosition(initPercent)
            else 200F
            mRight = mLeft + width
            mBottom = height.toFloat()
            mProgressRect.top = mTop
            mProgressRect.left = mLeft
            mProgressRect.bottom = mBottom
            mProgressRect.right = mRight
            isVirgin = false
        }

        canvas?.drawRect(mProgressRect, mPaint)
    }


    fun setProgress(percentage: Double) {
        mProgressRect.top = getTopPosition(percentage)
        if (isVirgin) initPercent = percentage
        invalidate()
    }

    private fun getTopPosition(percentage: Double) = height - height * percentage.toFloat()

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event!!.action == MotionEvent.ACTION_MOVE) {
            if (mListener?.canChange(this) == false) return true
            isVirgin = false
            val percent = 1 - event.y / mBottom.toDouble()

            if (mListener?.canMoveByFinger(this) == true) {
                when {
                    event.y in 0.0..mBottom.toDouble() -> mProgressRect.top = event.y
                    event.y > 100 -> mProgressRect.top = mBottom
                    event.y < 0 -> mProgressRect.top = 0F
                }
            }

            mListener?.onProgressChanged(this, getProgress(), percent)
            invalidate()
            return true
        } else if (event.action == MotionEvent.ACTION_UP) {
            if (mListener?.canChange(this) == false)
                mListener?.actionWhenCantChange(this)
            else mListener
                ?.afterProgressChanged(this, getProgress(), getPercentage())
        }
        return true
    }

    /**
     * Calculate percentage
     */
    private fun getPercentage(): Double {
        return 1 - mProgressRect.top.toDouble() / mBottom.toDouble()
    }

    /**
     * Calculate progress
     */
    private fun getProgress(): Double {
        return mProgressRect.top.toDouble() / mBottom.toDouble()
    }

    /**
     * @author Moh Mah at https://stackoverflow.com/a/35668889/10315711
     */
    private fun getRoundedRect(
        left: Float, top: Float, right: Float, bottom: Float, rx: Float, ry: Float,
        tl: Boolean, tr: Boolean, br: Boolean, bl: Boolean
    ): Path {
        var rx = rx
        var ry = ry
        val path = Path()
        if (rx < 0) rx = 0f
        if (ry < 0) ry = 0f
        val width = right - left
        val height = bottom - top
        if (rx > width / 2) rx = width / 2
        if (ry > height / 2) ry = height / 2
        val widthMinusCorners = width - 2 * rx
        val heightMinusCorners = height - 2 * ry
        path.moveTo(right, top + ry)
        if (tr) path.rQuadTo(0f, -ry, -rx, -ry) //top-right corner
        else {
            path.rLineTo(0f, -ry)
            path.rLineTo(-rx, 0f)
        }
        path.rLineTo(-widthMinusCorners, 0f)
        if (tl) path.rQuadTo(-rx, 0f, -rx, ry) //top-left corner
        else {
            path.rLineTo(-rx, 0f)
            path.rLineTo(0f, ry)
        }
        path.rLineTo(0f, heightMinusCorners)
        if (bl) path.rQuadTo(0f, ry, rx, ry) //bottom-left corner
        else {
            path.rLineTo(0f, ry)
            path.rLineTo(rx, 0f)
        }
        path.rLineTo(widthMinusCorners, 0f)
        if (br) path.rQuadTo(rx, 0f, rx, -ry) //bottom-right corner
        else {
            path.rLineTo(rx, 0f)
            path.rLineTo(0f, -ry)
        }
        path.rLineTo(0f, -heightMinusCorners)

        path.close() //Given close, last lineto can be removed.
        return path
    }

    interface OnProgressChangedListener {
        fun onProgressChanged(airBar: AirBar, progress: Double, percentage: Double)
        fun afterProgressChanged(airBar: AirBar, progress: Double, percentage: Double)

        fun canChange(airBar: AirBar): Boolean

        fun actionWhenCantChange(airBar: AirBar)

        fun canMoveByFinger(airBar: AirBar): Boolean
    }
}