package com.wlisuha.applauncher.services

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.wlisuha.applauncher.LauncherApplication

class NLService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {

        Log.d("12345", sbn.groupKey)
        Log.d("12345", sbn.key)

        if (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0)
            return

        LauncherApplication.instance.notificationListener?.onAddedNotification(sbn)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        Log.d("12345", "remove")
        Log.d("12345", sbn.notification.extras.getString("android.text") ?: "Asd")
        LauncherApplication.instance.notificationListener?.onRemovedNotification(sbn)
    }
}