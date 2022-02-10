package com.wlisuha.applauncher.services

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.wlisuha.applauncher.LauncherApplication

class NLService : NotificationListenerService() {
    companion object {
        private var previousNotification: StatusBarNotification? = null
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {

//        Log.d("12345", sbn.groupKey)
//        Log.d("12345", sbn.key)
//
//        if (previousNotification == null) {
//            previousNotification = sbn
//            Log.d("12345","is null")
//        } else {
//            Log.d("12345", (previousNotification == sbn).toString())
//        }
//
//
        if (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0)
            return
        Log.d("12345", sbn.key)
        Log.d("12345", "enter")
        LauncherApplication.instance.notificationListener?.onAddedNotification(sbn)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        Log.d("12345", sbn.key)
        LauncherApplication.instance.notificationListener?.onRemovedNotification(sbn)
    }
}