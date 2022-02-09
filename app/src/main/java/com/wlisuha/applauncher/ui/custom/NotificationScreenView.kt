package com.wlisuha.applauncher.ui.custom

import android.content.Context
import android.service.notification.StatusBarNotification
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.databinding.DataBindingUtil
import com.wlisuha.applauncher.LauncherApplication
import com.wlisuha.applauncher.R
import com.wlisuha.applauncher.databinding.NotificationViewBinding

class NotificationScreenView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : ConstraintLayout(context, attrs, defStyle), LauncherApplication.NotificationListener {

    val binding: NotificationViewBinding = DataBindingUtil.inflate(
        LayoutInflater.from(context),
        R.layout.notification_view,
        this,
        true
    )

    init {
        LauncherApplication.instance.notificationListener = this
    }

    override fun onAddedNotification(statusBarNotification: StatusBarNotification) {
        Log.d("12345","enter")
        binding.notificationStack.addNotification(statusBarNotification)
    }

    override fun onRemovedNotification(statusBarNotification: StatusBarNotification) {
        Log.d("12345","remove")
        binding.notificationStack.removeNotification(statusBarNotification)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        LauncherApplication.instance.notificationListener = null
    }
}