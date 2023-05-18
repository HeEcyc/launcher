package com.iosapp.ioslauncher.utils

import android.graphics.Color
import android.graphics.Outline
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.databinding.BindingAdapter
import com.iosapp.ioslauncher.R
import com.iosapp.ioslauncher.data.InstalledApp

@BindingAdapter("onEnableSelected")
fun AppCompatImageView.isVisibleRemoving(isVisibleRemoving: Boolean) {

    if (isVisibleRemoving && visibility == View.VISIBLE) return
    else if (!isVisibleRemoving && visibility == View.GONE) return

    with(animate()) {

        alpha(if (isVisibleRemoving) 1f else 0f)

        withStartAction { if (isVisibleRemoving) visibility = View.VISIBLE }
        withEndAction { if (!isVisibleRemoving) visibility = View.GONE }

    }.duration = ENABLED_ANIMATION_DURATION
}

@BindingAdapter("radius")
fun AppCompatImageView.radius(radius: Float) {
    object : ViewOutlineProvider() {
        override fun getOutline(view: View, outline: Outline) {
            outline.setRoundRect(0, 0, view.width, view.height, radius)
        }
    }.let(::setOutlineProvider)
    clipToOutline = true
}

@BindingAdapter("isSelected")
fun AppCompatImageView.setDrawable(isSelected: Boolean) {
    val strokeStrokeColor = if (isSelected) Color.parseColor("#007AFF")
    else Color.parseColor("#C7C7CC")
    GradientDrawable().apply {
        cornerRadius = 18f
        setStroke(6, strokeStrokeColor)
    }.let(::setImageDrawable)
}

@BindingAdapter("app")
fun FrameLayout.setApp(app: InstalledApp?) {}

@BindingAdapter("isEnabled")
fun MotionLayout.setSwipeEnabled(b: Boolean) {
    enableTransition(R.id.mainTransition, b)
}