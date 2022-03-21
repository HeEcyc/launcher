package com.applauncher.applauncher

import android.app.Application
import android.service.notification.StatusBarNotification

class LauncherApplication : Application() {
    //requestDisallowInterceptTouchEvent(boolean)

    var notificationListener: NotificationListener? = null

    companion object {
        lateinit var instance: LauncherApplication
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    interface NotificationListener {
        fun onNotificationsChanges(statusBarNotification: List<StatusBarNotification>)
    }
}