package com.example.service

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class BackgroundMonitorState(
    val isEnabled: Boolean = true,
    val isServiceRunning: Boolean = false,
    val isRefreshingNow: Boolean = false,
    val intervalSeconds: Int = 15,
    val targetRouterId: Long = 0L,
    val showCpuInNotif: Boolean = true,
    val showUsersInNotif: Boolean = true,
    val showPingAndUptime: Boolean = true,
    val hasRouters: Boolean = false,
    val routerName: String = "بانتظار إضافة راوتر",
    val boardInfo: String = "MikroTik RouterOS",
    val isOnline: Boolean = false,
    val cpuLoad: Int = 0,
    val freeMemory: String = "-- MB",
    val totalMemory: String = "-- MB",
    val uptime: String = "--",
    val pingMs: Long = 0L,
    val activeUsers: Int = 0,
    val onlineRouters: Int = 0,
    val totalRouters: Int = 0,
    val onlineDevices: Int = 0,
    val totalDevices: Int = 0,
    val lastUpdatedTime: String = "--:--:--",
    val lastError: String? = null
)

object RouterMonitorManager {
    private const val PREFS_NAME = "talal_pro_prefs"
    private const val KEY_ENABLED = "bg_monitor_enabled"
    private const val KEY_INTERVAL = "bg_monitor_interval_sec"
    private const val KEY_TARGET_ROUTER_ID = "bg_monitor_router_id"
    private const val KEY_SHOW_CPU = "bg_monitor_show_cpu"
    private const val KEY_SHOW_USERS = "bg_monitor_show_users"
    private const val KEY_SHOW_PING_UPTIME = "bg_monitor_show_ping_uptime"

    private val _state = MutableStateFlow(BackgroundMonitorState())
    val state: StateFlow<BackgroundMonitorState> = _state.asStateFlow()

    @Volatile
    private var initialized = false

    fun ensureInitialized(context: Context) {
        if (initialized) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _state.update {
            it.copy(
                isEnabled = prefs.getBoolean(KEY_ENABLED, true),
                intervalSeconds = prefs.getInt(KEY_INTERVAL, 15).coerceIn(5, 300),
                targetRouterId = prefs.getLong(KEY_TARGET_ROUTER_ID, 0L),
                showCpuInNotif = prefs.getBoolean(KEY_SHOW_CPU, true),
                showUsersInNotif = prefs.getBoolean(KEY_SHOW_USERS, true),
                showPingAndUptime = prefs.getBoolean(KEY_SHOW_PING_UPTIME, true)
            )
        }
        initialized = true
    }

    fun isEnabled(context: Context): Boolean {
        ensureInitialized(context)
        return _state.value.isEnabled
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun startService(context: Context) {
        ensureInitialized(context)
        setEnabledPref(context, true)
        val intent = Intent(context, RouterMonitorForegroundService::class.java).apply {
            action = RouterMonitorForegroundService.ACTION_START
        }
        ContextCompat.startForegroundService(context, intent)
    }

    fun stopService(context: Context, disableAutoStart: Boolean = true) {
        ensureInitialized(context)
        if (disableAutoStart) {
            setEnabledPref(context, false)
        }
        val intent = Intent(context, RouterMonitorForegroundService::class.java).apply {
            action = RouterMonitorForegroundService.ACTION_STOP
        }
        try {
            context.startService(intent)
        } catch (_: Exception) {
            try {
                context.stopService(Intent(context, RouterMonitorForegroundService::class.java))
            } catch (_: Exception) {}
        }
        _state.update { it.copy(isServiceRunning = false, isRefreshingNow = false) }
    }

    fun refreshNow(context: Context) {
        ensureInitialized(context)
        if (!_state.value.isServiceRunning) {
            startService(context)
            return
        }
        val intent = Intent(context, RouterMonitorForegroundService::class.java).apply {
            action = RouterMonitorForegroundService.ACTION_REFRESH_NOW
        }
        try {
            ContextCompat.startForegroundService(context, intent)
        } catch (_: Exception) {}
    }

    fun updateSettings(
        context: Context,
        intervalSeconds: Int = _state.value.intervalSeconds,
        targetRouterId: Long = _state.value.targetRouterId,
        showCpuInNotif: Boolean = _state.value.showCpuInNotif,
        showUsersInNotif: Boolean = _state.value.showUsersInNotif,
        showPingAndUptime: Boolean = _state.value.showPingAndUptime
    ) {
        ensureInitialized(context)
        val safeInterval = intervalSeconds.coerceIn(5, 300)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putInt(KEY_INTERVAL, safeInterval)
            .putLong(KEY_TARGET_ROUTER_ID, targetRouterId)
            .putBoolean(KEY_SHOW_CPU, showCpuInNotif)
            .putBoolean(KEY_SHOW_USERS, showUsersInNotif)
            .putBoolean(KEY_SHOW_PING_UPTIME, showPingAndUptime)
            .apply()

        _state.update {
            it.copy(
                intervalSeconds = safeInterval,
                targetRouterId = targetRouterId,
                showCpuInNotif = showCpuInNotif,
                showUsersInNotif = showUsersInNotif,
                showPingAndUptime = showPingAndUptime
            )
        }

        if (_state.value.isServiceRunning) {
            val intent = Intent(context, RouterMonitorForegroundService::class.java).apply {
                action = RouterMonitorForegroundService.ACTION_UPDATE_SETTINGS
            }
            try {
                ContextCompat.startForegroundService(context, intent)
            } catch (_: Exception) {}
        }
    }

    fun setEnabledPref(context: Context, enabled: Boolean) {
        ensureInitialized(context)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_ENABLED, enabled)
            .apply()
        _state.update { it.copy(isEnabled = enabled) }
    }

    internal fun markRunning(running: Boolean) {
        _state.update { it.copy(isServiceRunning = running) }
    }

    internal fun markRefreshing(refreshing: Boolean) {
        _state.update { it.copy(isRefreshingNow = refreshing) }
    }

    internal fun updateTelemetry(transform: (BackgroundMonitorState) -> BackgroundMonitorState) {
        _state.update(transform)
    }
}
