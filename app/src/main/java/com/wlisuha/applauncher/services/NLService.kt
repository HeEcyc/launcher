package com.wlisuha.applauncher.services

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class NLService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
//        getNotificationsList().onEach {
//        }.let {
//            //   Log.d("12345", it.size.toString())
//        }
        if (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0)
            return

        getNotificationsList()
        // LauncherApplication.instance.notificationListener?.onAddedNotification(sbn)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
//        getNotificationsList().let {
//            //      Log.d("12345", it.size.toString())
//        }
        //LauncherApplication.instance.notificationListener?.onRemovedNotification(sbn)
    }


    private fun getNotificationsList() = activeNotifications
        .filter { it.notification.flags and Notification.FLAG_GROUP_SUMMARY == 0 }
        .groupBy { it.groupKey }
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