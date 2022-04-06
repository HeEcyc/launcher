package com.iosapp.ioslauncher.utils

import android.graphics.Color
import android.service.notification.StatusBarNotification
import android.view.View
import androidx.appcompat.widget.AppCompatImageView
import androidx.databinding.BindingAdapter
import android.graphics.Outline
import android.graphics.drawable.GradientDrawable
import android.view.ViewOutlineProvider
import androidx.appcompat.widget.AppCompatTextView


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

@BindingAdapter("notificationIcon")
fun AppCompatImageView.notificationIcon(packageName: String) {
    context.packageManager.getApplicationIcon(packageName)
        .let(::setImageDrawable)
}

@BindingAdapter("notificationTitle")
fun AppCompatTextView.notificationTitle(statusBarNotification: StatusBarNotification) {
    text = statusBarNotification.notification.extras.getString("android.title")
}

@BindingAdapter("notificationText")
fun AppCompatTextView.notificationText(statusBarNotification: StatusBarNotification) {
    text = statusBarNotification.notification.extras.get("android.text").toString()
}

@BindingAdapter("notificationImage")
fun AppCompatImageView.notificationImage(statusBarNotification: StatusBarNotification) {
    with(statusBarNotification.notification.getLargeIcon()?.loadDrawable(context)) {
        if (this == null) visibility = View.GONE
        else {
            this@notificationImage.radius(10f)
            this@notificationImage.setImageDrawable(this)
        }
    }
}

@BindingAdapter("notificationAppOwner")
fun AppCompatTextView.notificationAppOwner(statusBarNotification: StatusBarNotification) {
    val appInfo = context.packageManager.getApplicationInfo(statusBarNotification.packageName, 0)
    text = context.packageManager.getApplicationLabel(appInfo)
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

@BindingAdapter("notificationTextColor")
fun AppCompatTextView.setNotificationTextColor(notificationTextColor: Int?) {
    notificationTextColor ?: return
    setTextColor(notificationTextColor)
}