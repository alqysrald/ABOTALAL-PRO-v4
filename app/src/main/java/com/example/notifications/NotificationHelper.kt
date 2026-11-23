package com.example.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.AlertSeverity

object NotificationHelper {

    private const val CHANNEL_ALERTS_ID = "talal_pro_alerts_channel"
    private const val CHANNEL_MESSAGES_ID = "talal_pro_messages_channel"
    const val CHANNEL_LIVE_MONITOR_ID = "talal_pro_live_monitor_channel"
    const val CHANNEL_USER_LOGIN_ID = "talal_pro_user_login_channel"

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

            val liveMonitorChannel = NotificationChannel(
                CHANNEL_LIVE_MONITOR_ID,
                "حالة الاتصال والمعالج بالخلفية - TALAL PRO",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "إشعار مستمر لعرض حالة اتصال الراوتر وحمل المعالج والأجهزة بالخلفية"
                enableVibration(false)
                setSound(null, null)
                setShowBadge(false)
            }

            val userLoginChannel = NotificationChannel(
                CHANNEL_USER_LOGIN_ID,
                "دخول مستخدمي هوتسبوت ويوزرمانجر - TALAL PRO",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "إشعارات فورية عند اتصال مستخدم جديد تعرض رقم الكرت ونوع الفئة ونوع الجهاز"
                enableVibration(true)
            }

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(alertsChannel)
            manager?.createNotificationChannel(messagesChannel)
            manager?.createNotificationChannel(liveMonitorChannel)
            manager?.createNotificationChannel(userLoginChannel)
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

    fun showNewCardLoginNotification(
        context: Context,
        cardCode: String,
        profileName: String,
        systemLabel: String,
        deviceType: String,
        routerName: String,
        ipAddress: String,
        macAddress: String,
        notificationId: Int = (50000 + (System.currentTimeMillis() % 40000)).toInt()
    ) {
        try {
            createNotificationChannels(context)

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            )

            val compactViews = RemoteViews(context.packageName, R.layout.notification_user_login_compact).apply {
                setTextViewText(R.id.notif_login_compact_title, "دخول كرت جديد ($systemLabel)")
                setTextViewText(R.id.notif_login_compact_system, systemLabel)
                setTextViewText(
                    R.id.notif_login_compact_summary,
                    "الكرت: $cardCode • الفئة: $profileName • الجهاز: $deviceType"
                )
            }

            val expandedViews = RemoteViews(context.packageName, R.layout.notification_user_login).apply {
                setTextViewText(R.id.notif_login_header, "دخول مستخدم جديد ($systemLabel)")
                setTextViewText(R.id.notif_login_router_info, "الراوتر: $routerName")
                setTextViewText(R.id.notif_login_system_badge, systemLabel)
                setTextViewText(R.id.notif_login_card_code, cardCode)
                setTextViewText(R.id.notif_login_profile, profileName)
                setTextViewText(R.id.notif_login_device, deviceType)
                val footerText = buildString {
                    if (ipAddress.isNotBlank()) append("IP: $ipAddress")
                    if (macAddress.isNotBlank()) {
                        if (isNotEmpty()) append(" • ")
                        append("MAC: $macAddress")
                    }
                    if (isEmpty()) append("متصل عبر $routerName")
                }
                setTextViewText(R.id.notif_login_footer, footerText)
            }

            val title = "🎫 دخول كرت جديد ($systemLabel): $cardCode"
            val summary = "رقم الكرت: $cardCode | الفئة: $profileName | الجهاز: $deviceType"

            val builder = NotificationCompat.Builder(context, CHANNEL_USER_LOGIN_ID)
                .setSmallIcon(R.drawable.ic_notif_router)
                .setContentTitle(title)
                .setContentText(summary)
                .setCustomContentView(compactViews)
                .setCustomBigContentView(expandedViews)
                .setStyle(NotificationCompat.DecoratedCustomViewStyle())
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)

            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (_: SecurityException) {
        } catch (_: Exception) {}
    }
}
