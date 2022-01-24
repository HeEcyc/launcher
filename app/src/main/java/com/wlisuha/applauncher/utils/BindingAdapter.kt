package com.wlisuha.applauncher.utils

import android.view.View
import androidx.appcompat.widget.AppCompatImageView
import androidx.databinding.BindingAdapter

@BindingAdapter("onEnableSelected")
fun AppCompatImageView.isVisibleRemoving(isVisibleRemoving: Boolean) {

    clearAnimation()

    with(animate()) {

        alpha(if (isVisibleRemoving) 1f else 0f)

        withStartAction { if (isVisibleRemoving) visibility = View.VISIBLE }
        withEndAction { if (!isVisibleRemoving) visibility = View.GONE }

    }.duration = ENABLED_ANIMATION_DURATION
}