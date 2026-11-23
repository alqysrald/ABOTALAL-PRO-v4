package com.example.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import com.example.MainActivity
import com.example.R
import com.example.TalalProApplication
import com.example.data.model.DeviceStatus
import com.example.data.model.RouterEntity
import com.example.notifications.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RouterMonitorForegroundService : Service() {

    private val tag = "RouterMonitorService"
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var monitorLoopJob: Job? = null
    private var manualRefreshJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannels(this)
        RouterMonitorManager.ensureInitialized(this)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        RouterMonitorManager.ensureInitialized(this)
        when (intent?.action) {
            ACTION_STOP -> {
                val fromNotification = intent.getBooleanExtra(EXTRA_FROM_NOTIFICATION, false)
                if (fromNotification) {
                    RouterMonitorManager.setEnabledPref(this, false)
                }
                monitorLoopJob?.cancel()
                manualRefreshJob?.cancel()
                RouterMonitorManager.markRunning(false)
                RouterMonitorManager.markRefreshing(false)
                ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }

            ACTION_REFRESH_NOW -> {
                ensureForegroundStarted()
                manualRefreshJob?.cancel()
                manualRefreshJob = serviceScope.launch {
                    performPollCycle(refreshAllRouters = true)
                }
                if (monitorLoopJob?.isActive != true) {
                    startMonitorLoop(immediatePoll = false)
                }
                return START_STICKY
            }

            ACTION_UPDATE_SETTINGS -> {
                ensureForegroundStarted()
                publishCurrentNotification(RouterMonitorManager.state.value)
                startMonitorLoop(immediatePoll = true)
                return START_STICKY
            }

            else -> {
                ensureForegroundStarted()
                RouterMonitorManager.markRunning(true)
                if (monitorLoopJob?.isActive != true) {
                    startMonitorLoop(immediatePoll = true)
                } else {
                    publishCurrentNotification(RouterMonitorManager.state.value)
                }
                return START_STICKY
            }
        }
    }

    private fun ensureForegroundStarted() {
        val notification = buildLuxuryNotification(RouterMonitorManager.state.value)
        val serviceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, serviceType)
        RouterMonitorManager.markRunning(true)
    }

    private fun startMonitorLoop(immediatePoll: Boolean) {
        monitorLoopJob?.cancel()
        monitorLoopJob = serviceScope.launch {
            var isFirst = immediatePoll
            while (isActive) {
                if (isFirst) {
                    performPollCycle(refreshAllRouters = false)
                    isFirst = false
                }
                val intervalMs = (RouterMonitorManager.state.value.intervalSeconds.coerceIn(5, 300)) * 1000L
                delay(intervalMs)
                performPollCycle(refreshAllRouters = false)
            }
        }
    }

    private suspend fun performPollCycle(refreshAllRouters: Boolean) {
        RouterMonitorManager.markRefreshing(true)
        try {
            val app = application as TalalProApplication
            val repo = app.repository
            val routerDao = app.database.routerDao()
            val deviceDao = app.database.networkDeviceDao()

            val uid = repo.currentUserId.value.ifBlank { repo.getSavedUserEmail() ?: "" }
            val savedRouters: List<RouterEntity> = if (uid.isNotBlank()) {
                routerDao.getRoutersListForUser(uid).ifEmpty { routerDao.getAllRoutersList() }
            } else {
                routerDao.getAllRoutersList()
            }

            val savedDevices = if (uid.isNotBlank()) {
                deviceDao.getDevicesListForUser(uid).ifEmpty { deviceDao.getAllDevicesList() }
            } else {
                deviceDao.getAllDevicesList()
            }

            val timeFormatted = SimpleDateFormat("HH:mm:ss", Locale.ENGLISH).format(Date())

            if (savedRouters.isEmpty()) {
                val onlineDev = savedDevices.count { it.status == DeviceStatus.ONLINE }
                RouterMonitorManager.updateTelemetry { prev ->
                    prev.copy(
                        isRefreshingNow = false,
                        hasRouters = false,
                        routerName = "بانتظار إضافة راوتر MikroTik",
                        boardInfo = "أضف راوتر لبدء المراقبة اللحظية",
                        isOnline = false,
                        cpuLoad = 0,
                        freeMemory = "-- MB",
                        totalMemory = "-- MB",
                        uptime = "--",
                        pingMs = 0L,
                        activeUsers = 0,
                        onlineRouters = 0,
                        totalRouters = 0,
                        onlineDevices = onlineDev,
                        totalDevices = savedDevices.size,
                        lastUpdatedTime = timeFormatted,
                        lastError = null
                    )
                }
                publishCurrentNotification(RouterMonitorManager.state.value)
                return
            }

            val configuredId = RouterMonitorManager.state.value.targetRouterId
            val primaryRouter = savedRouters.firstOrNull { it.id == configuredId }
                ?: savedRouters.firstOrNull { it.isOnline }
                ?: savedRouters.first()

            val checkedPrimary = try {
                repo.checkSingleRouter(primaryRouter)
            } catch (e: Exception) {
                Log.e(tag, "Primary router poll error", e)
                primaryRouter.copy(isOnline = false, lastError = e.message)
            }

            val updatedFleet = mutableListOf(checkedPrimary)
            if (refreshAllRouters) {
                for (r in savedRouters) {
                    if (r.id != primaryRouter.id) {
                        val updated = try {
                            repo.checkSingleRouter(r)
                        } catch (_: Exception) {
                            r
                        }
                        updatedFleet.add(updated)
                    }
                }
            } else {
                savedRouters.filterTo(updatedFleet) { it.id != primaryRouter.id }
            }

            val onlineRouters = updatedFleet.count { it.isOnline }
            val onlineDevices = savedDevices.count { it.status == DeviceStatus.ONLINE }
            val activeUsers = checkedPrimary.activeHotspotUsers + checkedPrimary.activePppoeUsers

            val boardSummary = buildString {
                append(checkedPrimary.boardName.ifBlank { "MikroTik" })
                if (checkedPrimary.version.isNotBlank()) {
                    append(" • ")
                    append(checkedPrimary.version)
                }
            }

            RouterMonitorManager.updateTelemetry { prev ->
                prev.copy(
                    isRefreshingNow = false,
                    hasRouters = true,
                    routerName = checkedPrimary.name,
                    boardInfo = boardSummary,
                    isOnline = checkedPrimary.isOnline,
                    cpuLoad = checkedPrimary.cpuLoad.coerceIn(0, 100),
                    freeMemory = checkedPrimary.freeMemory.ifBlank { "-- MB" },
                    totalMemory = checkedPrimary.totalMemory.ifBlank { "-- MB" },
                    uptime = checkedPrimary.uptime.ifBlank { "--" },
                    pingMs = checkedPrimary.pingMs.coerceAtLeast(0L),
                    activeUsers = activeUsers,
                    onlineRouters = onlineRouters,
                    totalRouters = updatedFleet.size,
                    onlineDevices = onlineDevices,
                    totalDevices = savedDevices.size,
                    lastUpdatedTime = timeFormatted,
                    lastError = checkedPrimary.lastError
                )
            }

            publishCurrentNotification(RouterMonitorManager.state.value)
        } catch (e: Exception) {
            Log.e(tag, "Error in performPollCycle", e)
            RouterMonitorManager.markRefreshing(false)
        }
    }

    private fun publishCurrentNotification(state: BackgroundMonitorState) {
        if (!RouterMonitorManager.hasNotificationPermission(this)) return
        try {
            NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, buildLuxuryNotification(state))
        } catch (se: SecurityException) {
            Log.w(tag, "POST_NOTIFICATIONS denied: ${se.message}")
        } catch (e: Exception) {
            Log.e(tag, "Failed to update notification", e)
        }
    }

    private fun buildLuxuryNotification(state: BackgroundMonitorState): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val flagsImmutable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val contentPendingIntent = PendingIntent.getActivity(this, 100, openAppIntent, flagsImmutable)

        val refreshIntent = Intent(this, RouterMonitorForegroundService::class.java).apply {
            action = ACTION_REFRESH_NOW
        }
        val refreshPendingIntent = PendingIntent.getService(this, 101, refreshIntent, flagsImmutable)

        val stopIntent = Intent(this, RouterMonitorForegroundService::class.java).apply {
            action = ACTION_STOP
            putExtra(EXTRA_FROM_NOTIFICATION, true)
        }
        val stopPendingIntent = PendingIntent.getService(this, 102, stopIntent, flagsImmutable)

        val statusBadgeText = when {
            !state.hasRouters -> "بانتظار راوتر"
            state.isOnline -> "متصل ●"
            else -> "غير متصل ●"
        }
        val statusBgRes = if (state.isOnline) {
            R.drawable.bg_notif_status_online
        } else {
            R.drawable.bg_notif_status_offline
        }
        val statusTextColor = if (state.isOnline) Color.parseColor("#6EE7B7") else Color.parseColor("#FCA5A5")

        val cpuPercent = state.cpuLoad.coerceIn(0, 100)
        val cpuTextColor = when {
            !state.isOnline -> Color.parseColor("#94A3B8")
            cpuPercent >= 80 -> Color.parseColor("#F87171")
            cpuPercent >= 50 -> Color.parseColor("#FBBF24")
            else -> Color.parseColor("#34D399")
        }

        // Collapsed RemoteViews
        val collapsedViews = RemoteViews(packageName, R.layout.notification_router_monitor).apply {
            setTextViewText(R.id.notif_router_name, state.routerName)
            setTextViewText(R.id.notif_status_badge, statusBadgeText)
            setInt(R.id.notif_status_badge, "setBackgroundResource", statusBgRes)
            setTextColor(R.id.notif_status_badge, statusTextColor)

            if (state.showCpuInNotif) {
                setViewVisibility(R.id.notif_cpu_text, View.VISIBLE)
                setViewVisibility(R.id.notif_cpu_progress, View.VISIBLE)
                setTextViewText(R.id.notif_cpu_text, "CPU: $cpuPercent%")
                setTextColor(R.id.notif_cpu_text, cpuTextColor)
                setProgressBar(R.id.notif_cpu_progress, 100, if (state.isOnline) cpuPercent else 0, false)
            } else {
                setViewVisibility(R.id.notif_cpu_text, View.GONE)
                setViewVisibility(R.id.notif_cpu_progress, View.GONE)
            }

            val quickMetrics = buildString {
                if (state.showUsersInNotif) {
                    append("متصلين: ${state.activeUsers}")
                } else {
                    append("الراوترات: ${state.onlineRouters}/${state.totalRouters}")
                }
                if (state.showPingAndUptime && state.isOnline) {
                    append(" • ${state.pingMs}ms")
                }
            }
            setTextViewText(R.id.notif_quick_metrics, quickMetrics)
        }

        // Expanded RemoteViews
        val expandedViews = RemoteViews(packageName, R.layout.notification_router_monitor_expanded).apply {
            setTextViewText(R.id.notif_exp_router_name, state.routerName)
            setTextViewText(R.id.notif_exp_board_version, state.boardInfo)

            setTextViewText(R.id.notif_exp_status_badge, statusBadgeText)
            setInt(R.id.notif_exp_status_badge, "setBackgroundResource", statusBgRes)
            setTextColor(R.id.notif_exp_status_badge, statusTextColor)

            if (state.showPingAndUptime) {
                setViewVisibility(R.id.notif_exp_ping, View.VISIBLE)
                setViewVisibility(R.id.notif_exp_uptime_tile, View.VISIBLE)
                setTextViewText(R.id.notif_exp_ping, if (state.isOnline) "${state.pingMs} ms" else "Offline")
                setTextViewText(R.id.notif_exp_uptime_value, state.uptime)
            } else {
                setViewVisibility(R.id.notif_exp_ping, View.GONE)
                setViewVisibility(R.id.notif_exp_uptime_tile, View.GONE)
            }

            if (state.showCpuInNotif) {
                setViewVisibility(R.id.notif_exp_cpu_card, View.VISIBLE)
                setTextViewText(R.id.notif_exp_cpu_value, if (state.isOnline) "$cpuPercent%" else "--%")
                setTextColor(R.id.notif_exp_cpu_value, cpuTextColor)
                setProgressBar(R.id.notif_exp_cpu_progress, 100, if (state.isOnline) cpuPercent else 0, false)
            } else {
                setViewVisibility(R.id.notif_exp_cpu_card, View.GONE)
            }

            setTextViewText(R.id.notif_exp_ram_value, state.freeMemory)

            if (state.showUsersInNotif) {
                setViewVisibility(R.id.notif_exp_users_tile, View.VISIBLE)
                setTextViewText(R.id.notif_exp_users_value, "${state.activeUsers} نشط")
            } else {
                setViewVisibility(R.id.notif_exp_users_tile, View.GONE)
            }

            setTextViewText(
                R.id.notif_exp_fleet_summary,
                "الراوترات: ${state.onlineRouters}/${state.totalRouters} • الأجهزة: ${state.onlineDevices}/${state.totalDevices}"
            )
            setTextViewText(R.id.notif_exp_last_update, "تحديث: ${state.lastUpdatedTime}")
        }

        val fallbackTitle = "${state.routerName} • $statusBadgeText"
        val fallbackSummary = "CPU: $cpuPercent% | الرام: ${state.freeMemory} | المتصلين: ${state.activeUsers}"

        return NotificationCompat.Builder(this, NotificationHelper.CHANNEL_LIVE_MONITOR_ID)
            .setSmallIcon(R.drawable.ic_notif_router)
            .setContentTitle(fallbackTitle)
            .setContentText(fallbackSummary)
            .setCustomContentView(collapsedViews)
            .setCustomBigContentView(expandedViews)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(contentPendingIntent)
            .addAction(android.R.drawable.ic_popup_sync, "تحديث الآن", refreshPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "إيقاف الخلفية", stopPendingIntent)
            .build()
    }

    override fun onDestroy() {
        monitorLoopJob?.cancel()
        manualRefreshJob?.cancel()
        serviceScope.cancel()
        RouterMonitorManager.markRunning(false)
        RouterMonitorManager.markRefreshing(false)
        super.onDestroy()
    }

    companion object {
        const val NOTIFICATION_ID = 41001
        const val ACTION_START = "com.talalpro.action.START_MONITOR"
        const val ACTION_STOP = "com.talalpro.action.STOP_MONITOR"
        const val ACTION_REFRESH_NOW = "com.talalpro.action.REFRESH_MONITOR"
        const val ACTION_UPDATE_SETTINGS = "com.talalpro.action.UPDATE_MONITOR_SETTINGS"
        private const val EXTRA_FROM_NOTIFICATION = "extra_from_notification"
    }
}
