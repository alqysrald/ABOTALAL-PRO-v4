package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ConnectionType {
    CLOUD,
    PUBLIC_IP,
    SSTP
}

enum class DeviceType {
    MAIN_TOWER, // برج رئيسي
    ROUTER,     // راوتر
    AP,         // نقطة بث AP
    MODEM       // مودم
}

enum class DeviceStatus {
    ONLINE,      // متصل
    OFFLINE,     // غير متصل / طافية
    MAINTENANCE  // صيانة / تحذير
}

enum class AlertSeverity {
    INFO,
    WARNING,
    ERROR
}

@Entity(tableName = "routers")
data class RouterEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String = "", // Multi-tenant user isolation
    val name: String,
    val connectionType: ConnectionType = ConnectionType.CLOUD,
    val host: String, // Cloud DNS or Public IP or VPN Host
    val port: Int = 8728,
    val username: String = "admin",
    val password: String = "",
    val location: String = "15.369,44.191",
    val localIpFallback: String = "10.0.0.1",
    val localPort: Int = 8728,
    val isOnline: Boolean = false,
    val lastChecked: Long = 0L,
    val pingMs: Long = 0L,
    val cpuLoad: Int = 0,
    val freeMemory: String = "0 MB",
    val totalMemory: String = "0 MB",
    val uptime: String = "0s",
    val version: String = "RouterOS v7.x",
    val boardName: String = "MikroTik",
    val activeHotspotUsers: Int = 0,
    val activePppoeUsers: Int = 0,
    val lastError: String? = null,
    val cloudDdnsName: String = "",
    val cloudStatus: String = "UNKNOWN"
)

@Entity(tableName = "network_devices")
data class NetworkDeviceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String = "", // Multi-tenant user isolation
    val name: String,
    val ip: String,
    val manager: String = "",
    val location: String = "15.369,44.191",
    val notes: String = "",
    val routerId: Long? = null,
    val parentDeviceId: Long? = null,
    val parentDeviceName: String = "",
    val type: DeviceType = DeviceType.MAIN_TOWER,
    val status: DeviceStatus = DeviceStatus.ONLINE,
    val pingMs: Long = 0L,
    val lastSeen: Long = System.currentTimeMillis(),
    val isMonitored: Boolean = true,
    val hasIssue: Boolean = false,
    val issueReason: String = "",
    val isNetwatchSynced: Boolean = false,
    val apiUsername: String = "admin",
    val apiPassword: String = ""
)

@Entity(tableName = "alerts")
data class AlertEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String = "",
    val title: String,
    val message: String,
    val severity: AlertSeverity = AlertSeverity.INFO,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val routerId: Long? = null,
    val deviceId: Long? = null
)

@Entity(tableName = "network_logs")
data class NetworkLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String = "",
    val action: String,
    val details: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isSuccess: Boolean = true
)

// ================= USER MANAGER MODELS =================

@Entity(tableName = "user_manager_users")
data class UserManagerUserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String = "",
    val routerId: Long = 0,
    val mikrotikId: String = "",
    val username: String = "",
    val password: String = "",
    val profile: String = "default",
    val uptime: String = "0s",
    val downloadBytes: String = "0 MB",
    val uploadBytes: String = "0 MB",
    val timeLeft: String = "",
    val comment: String = "",
    val isDisabled: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_manager_profiles")
data class UserManagerProfileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String = "",
    val routerId: Long = 0,
    val name: String = "",
    val nameForUsers: String = "",
    val price: String = "0",
    val validity: String = "30d",
    val rateLimitRx: String = "2M",
    val rateLimitTx: String = "4M",
    val sharedUsers: Int = 1
)

// ================= HOTSPOT MODELS =================

@Entity(tableName = "hotspot_users")
data class HotspotUserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String = "",
    val routerId: Long = 0,
    val mikrotikId: String = "",
    val name: String = "",
    val password: String = "",
    val profile: String = "default",
    val uptime: String = "0s",
    val bytesIn: String = "0 MB",
    val bytesOut: String = "0 MB",
    val limitUptime: String = "",
    val limitBytesTotal: String = "",
    val macAddress: String = "",
    val comment: String = "",
    val isDisabled: Boolean = false,
    val isOnline: Boolean = false
)

@Entity(tableName = "hotspot_profiles")
data class HotspotProfileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String = "",
    val routerId: Long = 0,
    val name: String = "",
    val rateLimit: String = "2M/4M",
    val sharedUsers: Int = 1,
    val sessionTimeout: String = "0s",
    val keepaliveTimeout: String = "2m",
    val statusAutorefresh: String = "1m"
)

@Entity(tableName = "hotspot_active_sessions")
data class HotspotActiveSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String = "",
    val routerId: Long = 0,
    val mikrotikId: String = "",
    val username: String = "",
    val ipAddress: String = "",
    val macAddress: String = "",
    val uptime: String = "0s",
    val bytesIn: String = "0 MB",
    val bytesOut: String = "0 MB",
    val loginBy: String = "cookie"
)

data class ActiveDeviceEntity(
    val id: String = "",
    val username: String = "",
    val type: String = "HOTSPOT", // "HOTSPOT" or "USER_MANAGER"
    val ipAddress: String = "",
    val macAddress: String = "",
    val hostName: String = "",
    val deviceType: String = "هاتف ذكي",
    val uptime: String = "0s",
    val startTime: String = "",
    val bytesIn: String = "0 MB",
    val bytesOut: String = "0 MB",
    val totalBytes: String = "0 MB",
    val remainingQuota: String = "مفتوح",
    val remainingTime: String = "غير محدد",
    val profile: String = "default"
)

// ================= PORTS & NETWORK DISTRIBUTION MODELS =================

data class NetworkPortEntity(
    val id: String = "",
    val name: String = "",
    val defaultName: String = "",
    val type: String = "ether",
    val isRunning: Boolean = false,
    val isDisabled: Boolean = false,
    val macAddress: String = "",
    val mtu: String = "1500",
    val speed: String = "غير متصل",
    val comment: String = "",
    val rxBytes: Long = 0L,
    val txBytes: Long = 0L,
    val rxRateBps: Long = 0L,
    val txRateBps: Long = 0L,
    val rxDrops: Long = 0L,
    val txDrops: Long = 0L,
    val rxErrors: Long = 0L,
    val txErrors: Long = 0L,
    val bandwidthLimit: String = "مفتوح"
)

data class DistributionDeviceItem(
    val id: String = "",
    val name: String = "",
    val ip: String = "",
    val mac: String = "",
    val interfaceName: String = "",
    val deviceModel: String = "",
    val type: String = "نقطة توزيع AP",
    val status: DeviceStatus = DeviceStatus.ONLINE,
    val pingMs: Long = 0L,
    val connectedUsersCount: Int = 0,
    val rxBytes: Long = 0L,
    val txBytes: Long = 0L,
    val currentRateDownload: String = "0 Kbps",
    val currentRateUpload: String = "0 Kbps",
    val uptime: String = "0s",
    val signalDbm: String = "-62 dBm",
    val lastSeen: Long = System.currentTimeMillis()
)


// ================= TELEGRAM CONFIG =================

@Entity(tableName = "telegram_config")
data class TelegramConfigEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String = "",
    val botToken: String = "",
    val chatId: String = "",
    val channelId: String = "",
    val isEnabled: Boolean = false,
    val alertOnRouterOffline: Boolean = true,
    val alertOnCpuHigh: Boolean = true,
    val alertOnHotspotLogin: Boolean = false,
    val alertOnBackupDone: Boolean = true,
    val customBotName: String = "TALAL PRO Bot",
    val lastTestTimestamp: Long = 0L,
    val lastTestStatus: String = "Not Tested"
)

// ================= ADVANCED INDEPENDENT TELEGRAM BOT =================

@Entity(tableName = "advanced_telegram_bot")
data class AdvancedTelegramBotEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String = "",
    val botToken: String = "",
    val chatId: String = "",
    val botUsername: String = "",
    val botName: String = "TALAL PRO Advanced Bot",
    val isEnabled: Boolean = true,
    val isCloudActive: Boolean = false,
    val detectCardSharing: Boolean = true,
    val enableCardsMenu: Boolean = true,
    val enableVoucherCreation: Boolean = true,
    val enablePortControls: Boolean = true,
    val enableAutoProtection: Boolean = true,
    val installedScriptRouterId: Long = 0L,
    val lastCloudHeartbeat: Long = 0L,
    val lastDetectionAlert: String = ""
)

@Entity(tableName = "custom_telegram_buttons")
data class TelegramCustomButtonEntity(
    @PrimaryKey
    val id: String,
    val userId: String = "",
    val title: String,
    val command: String,
    val description: String = "",
    val actionType: String = "TEXT", // "TEXT", "ROUTER_COMMAND", "SUB_MENU_CARDS", "SUSPICIOUS_CHECK"
    val payload: String = "",
    val isEnabled: Boolean = true,
    val isBuiltIn: Boolean = false,
    val sortOrder: Int = 0
)

data class SuspiciousCardSharingAlert(
    val cardUsername: String,
    val macAddresses: List<String>,
    val routerName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "ACTIVE_ALERT",
    val detectedSessions: Int = 2
)

data class VoucherCreationSessionState(
    var step: String = "IDLE", // IDLE, SYSTEM_PICKED, PROFILE_PICKED, COUNT_PICKED, VALIDITY_PICKED
    var system: String = "HOTSPOT", // "HOTSPOT" or "USER_MANAGER"
    var profile: String = "",
    var count: Int = 10,
    var days: Int? = null
)

// ================= CARD TEMPLATES DESIGNER (VOUCHER PRINT TEMPLATE) =================

@Entity(tableName = "card_templates")
data class CardTemplateEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String = "",
    val systemType: String = "USER_MANAGER", // "USER_MANAGER" or "HOTSPOT" or "BOTH"
    val profileName: String = "default",
    val templateName: String = "Default_Design",
    val categoryChip: String = "فئة 100 ريال",
    val linkedUmProfile: String = "",
    val linkedHotspotProfile: String = "",

    // Page Data & Print Settings
    val columnsPerPage: Int = 3,
    val rowsPerPage: Int = 17,
    val cardsCount: Int = 51,
    val usernameDigits: Int = 10,
    val scaleToA4: Boolean = true,
    val marginHorizontalMm: Float = 0.0f,
    val marginVerticalMm: Float = 0.0f,
    val showPageNote: Boolean = false,
    val pageNote: String = "",

    // Background
    val enableBackground: Boolean = false,
    val bgImageUri: String = "",

    // Card Borders
    val enableBorder: Boolean = true,
    val borderColorHex: String = "#000000",
    val borderSizeMm: Float = 0.3f,

    // Card Elements & Font
    val showUsername: Boolean = true,
    val enableCustomTitle: Boolean = false,
    val customTitleText: String = "",
    val fontColorHex: String = "#111827",
    val fontSize: Float = 10.0f,
    val isFontBold: Boolean = true,

    // Page Number & Manual Username Coordinates
    val showPageNumber: Boolean = true,
    val userXPercent: Float = 50f,
    val userYPercent: Float = 50f,

    // Password Settings
    val showPassword: Boolean = false,
    val passwordText: String = "{password}",
    val passwordFontSize: Float = 9.0f,
    val passwordXPercent: Float = 50f,
    val passwordYPercent: Float = 66f,

    // Payment Number Settings
    val showPayment: Boolean = false,
    val paymentText: String = "{payment}",
    val paymentFontSize: Float = 9.0f,
    val paymentXPercent: Float = 25f,
    val paymentYPercent: Float = 84f,

    // Serial Number Settings
    val showSerial: Boolean = false,
    val serialText: String = "{serial}",
    val serialFontSize: Float = 9.0f,
    val serialXPercent: Float = 75f,
    val serialYPercent: Float = 84f,

    // Package Price Settings
    val showPrice: Boolean = false,
    val priceTagText: String = "{price}",
    val priceFontSize: Float = 9.0f,
    val priceXPercent: Float = 78f,
    val priceYPercent: Float = 20f,

    // Point of Sale (POS) Settings
    val showPos: Boolean = false,
    val posText: String = "{pos}",
    val posFontSize: Float = 9.0f,
    val posXPercent: Float = 22f,
    val posYPercent: Float = 20f,

    // Barcode Settings
    val enableBarcode: Boolean = false,
    val barcodeXPercent: Float = 50f,
    val barcodeYPercent: Float = 78f,

    // Logo Settings
    val enableLogo: Boolean = false,
    val logoImageUri: String = "",
    val logoWidthMm: Float = 10.0f,
    val logoHeightMm: Float = 10.0f,
    val logoXPercent: Float = 14f,
    val logoYPercent: Float = 24f,

    val updatedAt: Long = System.currentTimeMillis()
)

data class GeneratedCardPrintItem(
    val username: String,
    val password: String,
    val profileName: String = "default",
    val price: String = "",
    val serial: String = "",
    val paymentNumber: String = "",
    val posName: String = "TALAL PRO"
)

// ================= MIKROTIK VLAN MANAGEMENT =================

data class MikrotikVlan(
    val id: String = "",
    val name: String = "",
    val vlanId: Int = 1,
    val interfaceName: String = "",
    val mtu: Int = 1500,
    val arp: String = "enabled",
    val comment: String = "",
    val disabled: Boolean = false,
    val running: Boolean = false,
    val macAddress: String = "",
    val rxByte: Long = 0L,
    val txByte: Long = 0L,
    val rxPacket: Long = 0L,
    val txPacket: Long = 0L
)
