package com.raqeem.app.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.raqeem.app.R

object RaqeemNotificationManager {
    const val BUDGET_CHANNEL_ID = "budget_warnings"
    const val SUBSCRIPTION_CHANNEL_ID = "subscription_reminders"
    const val SUMMARY_CHANNEL_ID = "weekly_summary"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(BUDGET_CHANNEL_ID, "Budget warnings", NotificationManager.IMPORTANCE_DEFAULT),
        )
        manager.createNotificationChannel(
            NotificationChannel(SUBSCRIPTION_CHANNEL_ID, "Subscription reminders", NotificationManager.IMPORTANCE_DEFAULT),
        )
        manager.createNotificationChannel(
            NotificationChannel(SUMMARY_CHANNEL_ID, "Weekly summary", NotificationManager.IMPORTANCE_DEFAULT),
        )
    }

    fun notify(context: Context, channelId: String, notificationId: Int, title: String, body: String) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }
}
