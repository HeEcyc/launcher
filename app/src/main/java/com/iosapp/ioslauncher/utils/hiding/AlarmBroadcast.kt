package com.iosapp.ioslauncher.utils.hiding

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import com.iosapp.ioslauncher.R
import com.iosapp.ioslauncher.ui.app.AppActivity
import com.iosapp.ioslauncher.utils.EXTRAS_HIDING
import java.util.concurrent.TimeUnit

class AlarmBroadcast : BroadcastReceiver() {

    companion object {

        fun startAlarm(context: Context) {
            doStartAlarm(context)
        }

        private fun doStartAlarm(context: Context) {
            doDoStartAlarm(context)
        }

        private fun doDoStartAlarm(context: Context) {
            val am = context.getSystemService(AlarmManager::class.java)

            am.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(5),
                getPendingInten(context)
            )
        }

        private fun getPendingInten(context: Context): PendingIntent {
            return doGetPendingInten(context)
        }

        private fun doGetPendingInten(context: Context): PendingIntent {
            return doDoGetPendingInten(context)
        }

        @SuppressLint("UnspecifiedImmutableFlag")
        private fun doDoGetPendingInten(context: Context): PendingIntent {
            val intent = Intent(context, AlarmBroadcast::class.java)
            intent.action = "notify"
            return PendingIntent.getBroadcast(
                context,
                10,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        doOnReceive(intent, context)
    }

    private fun doOnReceive(intent: Intent, context: Context) {
        doDoOnReceive(intent, context)
    }

    private fun doDoOnReceive(intent: Intent, context: Context) {
        if (intent.action == "notify" && !Settings.canDrawOverlays(context))
            showNotification(context)
        startAlarm(context)
    }

    private fun showNotification(context: Context) {
        doShowNotification(context)
    }

    private fun doShowNotification(context: Context) {
        doDoShowNotification(context)
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    private fun doDoShowNotification(context: Context) {
        val channel = context.getString(R.string.app_name)

        val nm = context.getSystemService(NotificationManager::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (nm.getNotificationChannel(channel) == null)
                NotificationChannel(channel, channel, NotificationManager.IMPORTANCE_HIGH)
                    .let(nm::createNotificationChannel)
        }

        val pi = PendingIntent
            .getActivity(
                context,
                0,
                Intent(context, AppActivity::class.java).putExtra(EXTRAS_HIDING, EXTRAS_HIDING),
                PendingIntent.FLAG_CANCEL_CURRENT
            )

        NotificationCompat.Builder(context, channel)
            .setContentText("Don't forget to add necessary permission to use extra\nfunctions of application")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setAutoCancel(true)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentIntent(pi)
            .build()
            .let { nm.notify(0, it) }
    }


}