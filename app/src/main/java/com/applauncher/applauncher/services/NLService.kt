package com.applauncher.applauncher.services

import android.app.Notification
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.applauncher.applauncher.LauncherApplication

class NLService : NotificationListenerService() {
    private val receiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent) {
            when (intent.action) {
                "cancel" -> cancelAllNotifications()
                "cancel_current" -> cancelNotification(intent.getStringExtra("key"))
            }
        }
    }

    private fun receiveNotifications() {
        registerReceiver(receiver, IntentFilter("cancel").apply {
            addAction("cancel_current")
        })
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        LauncherApplication.instance.notificationListener
            ?.onNotificationsChanges(getNotificationsList())
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        LauncherApplication.instance.notificationListener
            ?.onNotificationsChanges(getNotificationsList())
    }

    private fun getNotificationsList() = try {
        activeNotifications
            .filter {
                it.notification.flags and Notification.FLAG_GROUP_SUMMARY == 0
                        && it.notification.extras.containsKey("android.text")
            }
            .distinctBy { it.groupKey }
            .sortedByDescending { it.postTime }
    } catch (e: Exception) {
        listOf()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d("12345", "conncetcet")

        receiveNotifications()
        LauncherApplication.instance.notificationListener
            ?.onNotificationsChanges(getNotificationsList())
    }
}