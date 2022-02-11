package com.wlisuha.applauncher.services

import android.app.Notification
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.wlisuha.applauncher.LauncherApplication

class NLService : NotificationListenerService() {
    private val receiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent) {
            when (intent.action) {
                "cancel" -> cancelAllNotifications()
                "cancel_current" -> cancelNotification(intent.getStringExtra("key"))
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        registerReceiver(receiver, IntentFilter("cancel").apply {
            addAction("cancel_current")
        })

        Handler(Looper.getMainLooper()).postDelayed({
            LauncherApplication.instance.notificationListener
                ?.onNotificationsChanges(getNotificationsList())
        }, 1000)
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
}