package com.accent.launcher.ui.custom

import android.content.Context
import android.util.AttributeSet
import androidx.cardview.widget.CardView
import androidx.databinding.BindingAdapter
import com.accent.launcher.R
import com.accent.launcher.ui.custom.ScalableCardView.ScaleType.*

class ScalableCardView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : CardView(context, attrs, defStyleAttr) {

    var scaleBy = BY_WIDTH
        set(value) {
            field = value
            requestLayout()
        }
    // percent
    var scalableCornerRadius = 0f
        set(value) {
            field = value
            requestLayout()
        }

    companion object {
        @JvmStatic
        @BindingAdapter("scalableCornerRadius")
        fun setScalableCornerRadius(scv: ScalableCardView, scr: Float) {
            scv.scalableCornerRadius = scr
        }
        @JvmStatic
        @BindingAdapter("scaleType")
        fun setScaleType(scv: ScalableCardView, st: ScaleType) {
            scv.scaleBy = st
        }
    }

    init {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.ScalableCardView,
            0, 0
        ).apply {
            try {
                scaleBy = ScaleType.fromAttr(getInteger(R.styleable.ScalableCardView_scale_by, 0))
                scalableCornerRadius = getFloat(R.styleable.ScalableCardView_scalable_corner_radius, 0f)
            } finally {
                recycle()
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        radius = MeasureSpec.getSize(
            when (scaleBy) {
                BY_WIDTH -> widthMeasureSpec
                BY_HEIGHT -> heightMeasureSpec
            }
        ) * scalableCornerRadius
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    enum class ScaleType {
        BY_WIDTH, BY_HEIGHT;

        companion object {
            fun fromAttr(value: Int) = when (value) {
                0 -> BY_WIDTH
                1 -> BY_HEIGHT
                else -> BY_WIDTH
            }
        }
    }

}
