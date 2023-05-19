package com.accent.launcher.ui.custom

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import androidx.databinding.BindingAdapter
import com.accent.launcher.R

class TrueAutoSizeTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AppCompatTextView(context, attrs, defStyleAttr) {

    var fontToHeightRatio = 0.5f
        set(value) {
            field = value
            requestLayout()
        }

    companion object {
        @JvmStatic
        @BindingAdapter("fontToHeightRatio")
        fun setFontToHeightRatio(tastv: TrueAutoSizeTextView, fthr: Float) {
            tastv.fontToHeightRatio = fthr
        }
    }

    init {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.TrueAutoSizeTextView,
            0, 0
        ).apply {
            try {
                fontToHeightRatio = getFloat(R.styleable.TrueAutoSizeTextView_font_to_height_ratio, 0.5f)
            } finally {
                recycle()
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        textSize =
            MeasureSpec
                .getSize(heightMeasureSpec) / resources.displayMetrics.density * fontToHeightRatio
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

}
