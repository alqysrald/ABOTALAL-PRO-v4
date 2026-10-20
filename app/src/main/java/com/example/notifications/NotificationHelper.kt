package com.example.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.data.model.AlertSeverity

object NotificationHelper {

    private const val CHANNEL_ALERTS_ID = "talal_pro_alerts_channel"
    private const val CHANNEL_MESSAGES_ID = "talal_pro_messages_channel"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val alertsChannel = NotificationChannel(
                CHANNEL_ALERTS_ID,
                "تنبيهات وأعطال الشبكة - TALAL PRO",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "إشعارات انقطاع الراوترات وتوقف الأجهزة والأبراج اللحظية"
                enableVibration(true)
            }

            val messagesChannel = NotificationChannel(
                CHANNEL_MESSAGES_ID,
                "إشعارات الإدارة والاشتراكات - TALAL PRO",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "رسائل الإدارة وتجديد التراخيص وإشعارات التفعيل"
                enableVibration(true)
            }

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(alertsChannel)
            manager?.createNotificationChannel(messagesChannel)
        }
    }

    fun showSystemNotification(
        context: Context,
        title: String,
        message: String,
        severity: AlertSeverity = AlertSeverity.INFO,
        notificationId: Int = (System.currentTimeMillis() % 100000).toInt()
    ) {
        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            )

            val builder = NotificationCompat.Builder(context, CHANNEL_ALERTS_ID)
                .setSmallIcon(android.R.drawable.stat_notify_more)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)

            val manager = NotificationManagerCompat.from(context)
            manager.notify(notificationId, builder.build())
        } catch (_: Exception) {}
    }

    fun showAdminDirectMessageNotification(
        context: Context,
        message: String,
        notificationId: Int = 9999
    ) {
        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            )

            val builder = NotificationCompat.Builder(context, CHANNEL_MESSAGES_ID)
                .setSmallIcon(android.R.drawable.stat_notify_chat)
                .setContentTitle("📢 إشعار هام من إدارة TALAL PRO")
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)

            val manager = NotificationManagerCompat.from(context)
            manager.notify(notificationId, builder.build())
        } catch (_: Exception) {}
    }
}
