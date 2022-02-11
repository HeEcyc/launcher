package com.wlisuha.applauncher.utils

import android.service.notification.StatusBarNotification
import android.util.Log
import android.view.View
import androidx.appcompat.widget.AppCompatImageView
import androidx.databinding.BindingAdapter
import android.content.Context.CONTEXT_IGNORE_SECURITY
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Outline
import android.text.SpannableString
import android.view.ViewOutlineProvider
import androidx.appcompat.widget.AppCompatTextView


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
            object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, 10f)
                }
            }.let(this@notificationImage::setOutlineProvider)
            this@notificationImage.clipToOutline = true
            this@notificationImage.setImageDrawable(this)
        }
    }
String
}

@BindingAdapter("notificationAppOwner")
fun AppCompatTextView.notificationAppOwner(statusBarNotification: StatusBarNotification) {
    val appInfo = context.packageManager.getApplicationInfo(statusBarNotification.packageName, 0)
    text = context.packageManager.getApplicationLabel(appInfo)
}