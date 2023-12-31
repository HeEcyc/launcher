package com.iosapp.ioslauncher

import android.app.Application
import android.service.notification.StatusBarNotification

class LauncherApplication : Application() {

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