package com.wlisuha.applauncher.utils

import android.service.notification.StatusBarNotification
import android.util.Log
import android.view.View
import androidx.appcompat.widget.AppCompatImageView
import androidx.databinding.BindingAdapter
import android.content.Context.CONTEXT_IGNORE_SECURITY
import android.content.res.ColorStateList
import android.graphics.Bitmap


@BindingAdapter("onEnableSelected")
fun AppCompatImageView.isVisibleRemoving(isVisibleRemoving: Boolean) {

    clearAnimation()

    with(animate()) {

        alpha(if (isVisibleRemoving) 1f else 0f)

        withStartAction { if (isVisibleRemoving) visibility = View.VISIBLE }
        withEndAction { if (!isVisibleRemoving) visibility = View.GONE }

    }.duration = ENABLED_ANIMATION_DURATION
}

@BindingAdapter("notificationIcon")
fun AppCompatImageView.notificationIcon(packageName: String) {
    context.packageManager.getApplicationIcon(packageName)
        .let (::setImageDrawable)
}