package com.wlisuha.applauncher.services

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class NLService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
//
//        sbn.notification.contentIntent?.let {
//
//        }
//        sbn.notification.actions?.forEach {
//            it.title.toString()
//            it.getIcon()
//            if (it.title.toString() == "Answer") it.actionIntent.send()
//            it.actionIntent
//            Log.d("12345", it.title.toString())
//        }
    }
}