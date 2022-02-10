package com.wlisuha.applauncher.services

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.wlisuha.applauncher.LauncherApplication

class NLService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        LauncherApplication.instance.notificationListener
            ?.onNotificationsChanges(getNotificationsList())
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        LauncherApplication.instance.notificationListener
            ?.onNotificationsChanges(getNotificationsList())
    }


    private fun getNotificationsList() = activeNotifications
        .filter { it.notification.flags and Notification.FLAG_GROUP_SUMMARY == 0 }
        .onEach {
//            Log.d(
//                "12345", it.packageName + ":" + it.groupKey + ":" +
//                        (it.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0).toString()
//            )

//            Log.d("12345", it.notification.extras.getString("android.text") ?: "fuck")
//            Log.d("12345", it.packageName)
        }


    class NotificationsGroup {
        private val notificationsList = mutableListOf<StatusBarNotification>()
    }
}