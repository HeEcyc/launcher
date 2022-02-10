package com.wlisuha.applauncher.ui.custom

import android.content.Context
import android.service.notification.StatusBarNotification
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
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
        ResourcesCompat.getFont(context, R.font.sf_pro_display)?.let {
            binding.mainClock.typeface = it
            binding.textClock.typeface = it
        }
    }

    override fun onAddedNotification(statusBarNotification: StatusBarNotification) {
        for (i in 0 until binding.notificationsList.childCount) {
            val currentView = binding.notificationsList
                .getChildAt(0) as? NotificationStackView ?: return
            if (currentView.getNotificationsGroup() == statusBarNotification.groupKey) {
                currentView.addNotification(statusBarNotification)
                return
            }
        }
        addNotificationView(statusBarNotification)
        binding.scrollView.requestLayout()
    }

    override fun onRemovedNotification(statusBarNotification: StatusBarNotification) {
        val viewsToRemove = mutableListOf<View>()
        for (i in 0 until binding.notificationsList.childCount) {
            binding.notificationsList.getChildAt(i)?.let {
                it as NotificationStackView
                if (it.getNotificationsGroup() == statusBarNotification.groupKey) {
                    if (it.removeNotification(statusBarNotification)) viewsToRemove.add(it)
                }
            }

        }
        viewsToRemove.forEach { binding.notificationsList.removeView(it) }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        LauncherApplication.instance.notificationListener = null
    }

    private fun addNotificationView(statusBarNotification: StatusBarNotification) {
        NotificationStackView(context).let {
            it.addNotification(statusBarNotification)
            binding.notificationsList.addView(it)
        }
    }


}