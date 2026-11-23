package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.local.*
import com.example.data.mikrotik.MikroTikApiClient
import com.example.data.mikrotik.MikroTikResponse
import com.example.data.mikrotik.PingResult
import com.example.data.mikrotik.PingUtility
import com.example.data.model.*
import com.example.data.telegram.AdvancedTelegramManager
import com.example.data.telegram.TelegramManager
import com.example.notifications.NotificationHelper
import com.example.sound.SoundEffectManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

class NetworkRepository(
    private val routerDao: RouterDao,
    private val networkDeviceDao: NetworkDeviceDao,
    private val alertDao: AlertDao,
    private val networkLogDao: NetworkLogDao,
    private val userManagerDao: UserManagerDao,
    private val hotspotDao: HotspotDao,
    private val telegramConfigDao: TelegramConfigDao,
    private val cardTemplateDao: CardTemplateDao,
    private val advancedTelegramBotDao: AdvancedTelegramBotDao,
    val customTelegramButtonDao: CustomTelegramButtonDao,
    private val cardBatchDao: CardBatchDao,
    private val soundEffectManager: SoundEffectManager,
    private val appContext: Context
) {
    private val tag = "NetworkRepository"
    val telegramManager = TelegramManager()
    val advancedTelegramManager = AdvancedTelegramManager()

    private val _currentUserId = MutableStateFlow("")
    val currentUserId: StateFlow<String> = _currentUserId.asStateFlow()

    private val _isMonitoring = MutableStateFlow(true)
    val isMonitoring: StateFlow<Boolean> = _isMonitoring.asStateFlow()

    private val _soundEnabled = MutableStateFlow(true)
    val soundEnabled: StateFlow<Boolean> = _soundEnabled.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private var backgroundJob: Job? = null
    private val repoScope = CoroutineScope(Dispatchers.IO)

    init {
        startBackgroundMonitoring()
    }

    fun setCurrentUserId(userId: String) {
        _currentUserId.value = userId
    }

    // Flows isolated per current user
    fun getRoutersForUser(userId: String): Flow<List<RouterEntity>> = routerDao.getRoutersForUser(userId)
    fun getDevicesForUser(userId: String): Flow<List<NetworkDeviceEntity>> = networkDeviceDao.getDevicesForUser(userId)
    fun getAlertsForUser(userId: String): Flow<List<AlertEntity>> = alertDao.getAlertsForUser(userId)
    fun getUnreadAlertsCount(userId: String): Flow<Int> = alertDao.getUnreadAlertsCount(userId)
    fun getRecentLogs(userId: String): Flow<List<NetworkLogEntity>> = networkLogDao.getRecentLogs(userId)

    fun getUserManagerUsers(routerId: Long, userId: String): Flow<List<UserManagerUserEntity>> =
        userManagerDao.getUsersForRouter(routerId, userId)

    fun getUserManagerProfiles(routerId: Long, userId: String): Flow<List<UserManagerProfileEntity>> =
        userManagerDao.getProfilesForRouter(routerId, userId)

    fun getHotspotUsers(routerId: Long, userId: String): Flow<List<HotspotUserEntity>> =
        hotspotDao.getHotspotUsersForRouter(routerId, userId)

    fun getHotspotActiveSessions(routerId: Long, userId: String): Flow<List<HotspotActiveSessionEntity>> =
        hotspotDao.getActiveSessionsForRouter(routerId, userId)

    fun getHotspotProfiles(routerId: Long, userId: String): Flow<List<HotspotProfileEntity>> =
        hotspotDao.getHotspotProfilesForRouter(routerId, userId)

    fun getTelegramConfig(userId: String): Flow<TelegramConfigEntity?> =
        telegramConfigDao.getTelegramConfigForUser(userId)

    fun getAllCardTemplates(userId: String): Flow<List<CardTemplateEntity>> =
        cardTemplateDao.getAllTemplates(userId)

    fun getCardTemplates(systemType: String, userId: String): Flow<List<CardTemplateEntity>> =
        cardTemplateDao.getTemplatesForSystem(userId, systemType)

    fun getCardTemplateForProfile(systemType: String, profileName: String, userId: String): Flow<CardTemplateEntity?> =
        cardTemplateDao.getTemplateForProfile(userId, systemType, profileName)

    suspend fun saveCardTemplate(template: CardTemplateEntity): Long =
        cardTemplateDao.saveTemplate(template.copy(updatedAt = System.currentTimeMillis()))

    suspend fun deleteCardTemplate(template: CardTemplateEntity) =
        cardTemplateDao.deleteTemplate(template)

    suspend fun deleteCardTemplateById(templateId: Long) =
        cardTemplateDao.deleteTemplateById(templateId)

    private val prefs = appContext.getSharedPreferences("talal_pro_prefs", Context.MODE_PRIVATE)

    fun saveUserSession(email: String, isActivated: Boolean) {
        prefs.edit()
            .putString("saved_email", email.trim().lowercase())
            .putBoolean("is_activated", isActivated)
            .putLong("last_login_time", System.currentTimeMillis())
            .apply()
    }

    fun getSavedUserEmail(): String? {
        return prefs.getString("saved_email", null)
    }

    fun isSavedUserActivated(): Boolean {
        return prefs.getBoolean("is_activated", false)
    }

    fun clearUserSession() {
        prefs.edit()
            .remove("saved_email")
            .remove("is_activated")
            .remove("last_login_time")
            .apply()
    }

    fun toggleSound(enabled: Boolean? = null) {
        val newState = enabled ?: !_soundEnabled.value
        _soundEnabled.value = newState
        if (newState) {
            soundEffectManager.playSuccessBeep()
        }
    }

    fun playSoundTest() {
        soundEffectManager.testSound()
    }

    private fun startBackgroundMonitoring() {
        backgroundJob?.cancel()
        backgroundJob = repoScope.launch {
            while (isActive) {
                if (_isMonitoring.value) {
                    checkAllRoutersAndDevices()
                }
                delay(60000) // 60s
            }
        }
    }

    suspend fun checkAllRoutersAndDevices() {
        _isRefreshing.value = true
        try {
            val uid = _currentUserId.value
            val routers = routerDao.getRoutersListForUser(uid)
            for (router in routers) {
                checkSingleRouter(router)
            }

            val devices = networkDeviceDao.getDevicesListForUser(uid)
            for (device in devices) {
                checkSingleDevice(device)
            }
        } catch (e: Exception) {
            Log.e(tag, "Error in checkAllRoutersAndDevices", e)
        } finally {
            _isRefreshing.value = false
        }
    }

    suspend fun checkSingleRouter(router: RouterEntity): RouterEntity {
        val client = MikroTikApiClient(
            host = router.host,
            port = router.port,
            username = router.username,
            password = router.password
        )
        val info = client.getSystemInfo()

        val wasOffline = !router.isOnline
        val isNowOnline = info.isOnline

        val updated = router.copy(
            isOnline = isNowOnline,
            lastChecked = System.currentTimeMillis(),
            pingMs = info.pingMs,
            cpuLoad = info.cpuLoad,
            freeMemory = info.freeMemory,
            totalMemory = info.totalMemory,
            uptime = info.uptime,
            version = info.version,
            boardName = info.boardName,
            activeHotspotUsers = info.activeHotspotUsers,
            activePppoeUsers = info.activePppoeUsers,
            cloudDdnsName = if (info.cloudDdnsName.isNotBlank()) info.cloudDdnsName else router.cloudDdnsName,
            cloudStatus = info.cloudStatus,
            lastError = info.errorMessage
        )
        routerDao.updateRouter(updated)

        // Alerts & Sound effects
        if (wasOffline && isNowOnline) {
            if (_soundEnabled.value) soundEffectManager.playSuccessBeep()
            addAlert(
                userId = router.userId,
                title = "عودة اتصال الراوتر",
                message = "تمت استعادة الاتصال بنجاح مع الراوتر [${router.name}].",
                severity = AlertSeverity.INFO,
                routerId = router.id
            )
            dispatchTelegramAlert(router.userId, "🟢 <b>عودة اتصال راوتر:</b> ${router.name} أصبح متصلاً الآن بنجاح.")
        } else if (!wasOffline && !isNowOnline && router.lastChecked > 0) {
            if (_soundEnabled.value) soundEffectManager.playCriticalAlertBeep()
            addAlert(
                userId = router.userId,
                title = "انقطاع اتصال الراوتر ⚠️",
                message = "تعذر الاتصال بالراوتر [${router.name}]. السبب: ${info.errorMessage ?: "انقطاع الاتصال"}",
                severity = AlertSeverity.ERROR,
                routerId = router.id
            )
            dispatchTelegramAlert(router.userId, "🔴 <b>تحذير هام:</b> انقطع الاتصال بالراوتر ${router.name}!\nالسبب: ${info.errorMessage ?: "عدم استجابة"}")
        }

        return updated
    }

    suspend fun checkSingleDevice(device: NetworkDeviceEntity): NetworkDeviceEntity {
        val ping = PingUtility.pingHost(device.ip, timeoutMs = 3000)
        val newStatus = when {
            ping.isReachable -> DeviceStatus.ONLINE
            else -> DeviceStatus.OFFLINE
        }

        val wasOffline = device.status == DeviceStatus.OFFLINE
        val isNowOffline = newStatus == DeviceStatus.OFFLINE

        val updated = device.copy(
            status = newStatus,
            pingMs = ping.latencyMs,
            lastSeen = if (ping.isReachable) System.currentTimeMillis() else device.lastSeen,
            hasIssue = isNowOffline,
            issueReason = if (isNowOffline) "الجهاز لا يستجيب لطلبات ICMP Ping" else ""
        )
        networkDeviceDao.updateDevice(updated)

        if (wasOffline && !isNowOffline) {
            if (_soundEnabled.value) soundEffectManager.playBeep()
            addAlert(
                userId = device.userId,
                title = "عودة جهاز للعمل",
                message = "الجهاز/البرج [${device.name}] يعمل الآن (${device.ip})",
                severity = AlertSeverity.INFO,
                deviceId = device.id
            )
        } else if (!wasOffline && isNowOffline) {
            if (_soundEnabled.value) soundEffectManager.playCriticalAlertBeep()
            addAlert(
                userId = device.userId,
                title = "توقف جهاز أو برج ⚠️",
                message = "الجهاز [${device.name}] ذو العنوان (${device.ip}) توقف عن العمل!",
                severity = AlertSeverity.ERROR,
                deviceId = device.id
            )
        }

        return updated
    }

    // ================= SPECIAL TOOLS (ONE-CLICK IP CLOUD DDNS) =================

    suspend fun enableRouterCloudDdns(router: RouterEntity): MikroTikResponse {
        val client = MikroTikApiClient(
            host = router.host,
            port = router.port,
            username = router.username,
            password = router.password
        )
        val res = client.enableCloudDdns()
        if (res.isSuccess) {
            checkSingleRouter(router)
            addLog(
                userId = router.userId,
                action = "تفعيل IP Cloud DDNS",
                details = "تم تفعيل خدمة الكلاود بنجاح في الراوتر [${router.name}]"
            )
        }
        return res
    }

    suspend fun rebootRouter(router: RouterEntity): MikroTikResponse {
        val client = MikroTikApiClient(
            host = router.host,
            port = router.port,
            username = router.username,
            password = router.password
        )
        val res = client.rebootRouter()
        addLog(
            userId = router.userId,
            action = "إعادة تشغيل الراوتر",
            details = "تم إرسال أمر إعادة التشغيل للراوتر [${router.name}]"
        )
        return res
    }

    suspend fun backupRouter(router: RouterEntity, name: String): MikroTikResponse {
        val client = MikroTikApiClient(
            host = router.host,
            port = router.port,
            username = router.username,
            password = router.password
        )
        val res = client.createBackup(name)
        if (res.isSuccess) {
            addLog(
                userId = router.userId,
                action = "نسخ احتياطي",
                details = "تم إنشاء نسخة احتياطية بنجاح باسم [$name] في الراوتر [${router.name}]"
            )
        }
        return res
    }

    // ================= VLAN MANAGEMENT =================

    suspend fun getRouterVlans(router: RouterEntity): List<MikrotikVlan> {
        val client = MikroTikApiClient(
            host = router.host,
            port = router.port,
            username = router.username,
            password = router.password
        )
        return client.getVlans()
    }

    suspend fun getRouterLogs(router: RouterEntity, limit: Int = 250): Pair<List<MikrotikLogEntry>, String?> {
        val client = MikroTikApiClient(
            host = router.host,
            port = router.port,
            username = router.username,
            password = router.password
        )
        return client.getRouterLogs(limit)
    }

    suspend fun getRouterInterfacesList(router: RouterEntity): List<String> {
        val client = MikroTikApiClient(
            host = router.host,
            port = router.port,
            username = router.username,
            password = router.password
        )
        return client.getInterfacesList()
    }

    suspend fun addRouterVlan(
        router: RouterEntity,
        name: String,
        vlanId: Int,
        interfaceName: String,
        mtu: Int = 1500,
        arp: String = "enabled",
        comment: String = ""
    ): MikroTikResponse {
        val client = MikroTikApiClient(
            host = router.host,
            port = router.port,
            username = router.username,
            password = router.password
        )
        val res = client.addVlan(name, vlanId, interfaceName, mtu, arp, comment)
        if (res.isSuccess) {
            addLog(
                userId = router.userId,
                action = "إضافة فيلان (VLAN)",
                details = "تم إنشاء VLAN [$name] معرف ($vlanId) على المنفذ [$interfaceName] في [${router.name}]"
            )
        }
        return res
    }

    suspend fun updateRouterVlan(
        router: RouterEntity,
        idOrName: String,
        name: String,
        vlanId: Int,
        interfaceName: String,
        mtu: Int = 1500,
        arp: String = "enabled",
        comment: String = ""
    ): MikroTikResponse {
        val client = MikroTikApiClient(
            host = router.host,
            port = router.port,
            username = router.username,
            password = router.password
        )
        val res = client.updateVlan(idOrName, name, vlanId, interfaceName, mtu, arp, comment)
        if (res.isSuccess) {
            addLog(
                userId = router.userId,
                action = "تعديل فيلان (VLAN)",
                details = "تم تعديل VLAN [$name] معرف ($vlanId) في راوتر [${router.name}]"
            )
        }
        return res
    }

    suspend fun toggleRouterVlan(router: RouterEntity, idOrName: String, enable: Boolean): MikroTikResponse {
        val client = MikroTikApiClient(
            host = router.host,
            port = router.port,
            username = router.username,
            password = router.password
        )
        val res = client.toggleVlan(idOrName, enable)
        if (res.isSuccess) {
            addLog(
                userId = router.userId,
                action = if (enable) "تفعيل فيلان" else "إيقاف فيلان",
                details = "تم ${if (enable) "تفعيل" else "تعطيل"} الفيلان [$idOrName] في راوتر [${router.name}]"
            )
        }
        return res
    }

    suspend fun removeRouterVlan(router: RouterEntity, idOrName: String): MikroTikResponse {
        val client = MikroTikApiClient(
            host = router.host,
            port = router.port,
            username = router.username,
            password = router.password
        )
        val res = client.removeVlan(idOrName)
        if (res.isSuccess) {
            addLog(
                userId = router.userId,
                action = "حذف فيلان (VLAN)",
                details = "تم حذف الفيلان [$idOrName] من راوتر [${router.name}]"
            )
        }
        return res
    }

    suspend fun assignIpToRouterVlan(router: RouterEntity, vlanName: String, ipWithCidr: String): MikroTikResponse {
        val client = MikroTikApiClient(
            host = router.host,
            port = router.port,
            username = router.username,
            password = router.password
        )
        val res = client.assignIpToVlan(vlanName, ipWithCidr)
        if (res.isSuccess) {
            addLog(
                userId = router.userId,
                action = "تعيين IP لفيلان",
                details = "تم تعيين العنوان [$ipWithCidr] للفيلان [$vlanName] في راوتر [${router.name}]"
            )
        }
        return res
    }

    // ================= USER MANAGER SYNC & OPS =================

    suspend fun syncUserManager(router: RouterEntity): Boolean {
        return try {
            val client = MikroTikApiClient(router.host, router.port, router.username, router.password)
            val connCheck = client.testConnection()
            if (!connCheck.isSuccess) return false

            val usersRaw = client.getUserManagerUsers()
            val profilesRaw = client.getUserManagerProfiles()

            userManagerDao.clearUsersForRouter(router.id, router.userId)
            userManagerDao.clearProfilesForRouter(router.id, router.userId)

            val users = usersRaw.map { map ->
                UserManagerUserEntity(
                    userId = router.userId,
                    routerId = router.id,
                    mikrotikId = map[".id"] ?: "",
                    username = map["name"] ?: map["username"] ?: "user",
                    password = map["password"] ?: "••••",
                    profile = map["actual-profile"] ?: map["group"] ?: map["profile"] ?: "default",
                    uptime = map["uptime"] ?: "0s",
                    downloadBytes = map["download-used"] ?: map["bytes-out"] ?: "0 MB",
                    uploadBytes = map["upload-used"] ?: map["bytes-in"] ?: "0 MB",
                    timeLeft = map["time-left"] ?: "",
                    comment = map["comment"] ?: "",
                    isDisabled = map["disabled"] == "true" || map["disabled"] == "yes"
                )
            }
            userManagerDao.insertUsers(users)

            val profiles = profilesRaw.mapNotNull { map ->
                val pName = map["name"]?.trim() ?: ""
                if (pName.isBlank()) null else UserManagerProfileEntity(
                    userId = router.userId,
                    routerId = router.id,
                    name = pName,
                    nameForUsers = map["name-for-users"] ?: pName,
                    price = map["price"] ?: "0",
                    validity = map["validity"] ?: "30d",
                    rateLimitRx = map["rate-limit-rx"] ?: "2M",
                    rateLimitTx = map["rate-limit-tx"] ?: "4M",
                    sharedUsers = map["shared-users"]?.toIntOrNull() ?: 1
                )
            }
            userManagerDao.insertProfiles(profiles)
            true
        } catch (e: Exception) {
            Log.e(tag, "Failed to sync user manager", e)
            false
        }
    }

    suspend fun addUserManagerCard(
        router: RouterEntity,
        username: String,
        passwordVal: String,
        profile: String,
        comment: String
    ): MikroTikResponse {
        val client = MikroTikApiClient(router.host, router.port, router.username, router.password)
        val res = client.addUserManagerUser(username, passwordVal, profile, comment)
        if (res.isSuccess) {
            userManagerDao.insertUser(
                UserManagerUserEntity(
                    userId = router.userId,
                    routerId = router.id,
                    username = username,
                    password = passwordVal,
                    profile = profile,
                    comment = comment
                )
            )
            addLog(
                userId = router.userId,
                action = "إضافة كرت يوزرمانجر",
                details = "تمت إضافة الكرت [$username] في الراوتر [${router.name}]"
            )
        }
        return res
    }

    suspend fun deleteUserManagerCard(router: RouterEntity, userEntity: UserManagerUserEntity): MikroTikResponse {
        val client = MikroTikApiClient(router.host, router.port, router.username, router.password)
        val res = client.removeUserManagerUser(if (userEntity.mikrotikId.isNotBlank()) userEntity.mikrotikId else userEntity.username)
        userManagerDao.deleteUser(userEntity)
        return res
    }

    suspend fun editUserManagerCard(
        router: RouterEntity,
        userEntity: UserManagerUserEntity,
        newUsername: String,
        newPassword: String,
        newProfile: String,
        comment: String
    ): MikroTikResponse {
        val client = MikroTikApiClient(router.host, router.port, router.username, router.password)
        val targetId = if (userEntity.mikrotikId.isNotBlank()) userEntity.mikrotikId else userEntity.username
        val res = client.editUserManagerUser(targetId, newUsername, newPassword, newProfile, comment)
        if (res.isSuccess) {
            val updated = userEntity.copy(
                username = newUsername,
                password = newPassword,
                profile = newProfile,
                comment = comment
            )
            userManagerDao.updateUser(updated)
            addLog(router.userId, "تعديل كرت يوزرمانجر", "تم تعديل الكرت [$newUsername] في راوتر [${router.name}]")
        }
        return res
    }

    suspend fun toggleUserManagerCard(
        router: RouterEntity,
        userEntity: UserManagerUserEntity,
        enable: Boolean
    ): MikroTikResponse {
        val client = MikroTikApiClient(router.host, router.port, router.username, router.password)
        val targetId = if (userEntity.mikrotikId.isNotBlank()) userEntity.mikrotikId else userEntity.username
        val res = client.toggleUserManagerUser(targetId, enable)
        if (res.isSuccess) {
            val updated = userEntity.copy(isDisabled = !enable)
            userManagerDao.updateUser(updated)
            addLog(router.userId, if (enable) "تفعيل كرت" else "تعطيل كرت", "تم ${if (enable) "تفعيل" else "تعطيل"} كرت اليوزرمانجر [${userEntity.username}]")
        }
        return res
    }

    suspend fun changeUserManagerProfile(
        router: RouterEntity,
        userEntity: UserManagerUserEntity,
        newProfile: String
    ): MikroTikResponse {
        val client = MikroTikApiClient(router.host, router.port, router.username, router.password)
        val targetId = if (userEntity.mikrotikId.isNotBlank()) userEntity.mikrotikId else userEntity.username
        val res = client.changeUserManagerProfile(targetId, newProfile)
        if (res.isSuccess) {
            val updated = userEntity.copy(profile = newProfile)
            userManagerDao.updateUser(updated)
            addLog(router.userId, "تغيير فئة الكرت", "تم تغيير باقة الكرت [${userEntity.username}] إلى [$newProfile]")
        }
        return res
    }

    suspend fun renewUserManagerCard(
        router: RouterEntity,
        userEntity: UserManagerUserEntity,
        newProfile: String
    ): MikroTikResponse {
        val client = MikroTikApiClient(router.host, router.port, router.username, router.password)
        val targetId = if (userEntity.mikrotikId.isNotBlank()) userEntity.mikrotikId else userEntity.username
        val res = client.renewUserManagerUser(targetId, userEntity.username, newProfile)
        if (res.isSuccess) {
            val updated = userEntity.copy(
                profile = newProfile,
                isDisabled = false,
                downloadBytes = "0 MB",
                uploadBytes = "0 MB",
                uptime = "0s"
            )
            userManagerDao.updateUser(updated)
            addLog(router.userId, "تجديد كرت يوزرمانجر", "تم تجديد الكرت [${userEntity.username}] بنجاح بباقة [$newProfile]")
        }
        return res
    }

    // ================= HOTSPOT SYNC & OPS =================

    suspend fun syncHotspot(router: RouterEntity): Boolean {
        return try {
            val client = MikroTikApiClient(router.host, router.port, router.username, router.password)
            val connCheck = client.testConnection()
            if (!connCheck.isSuccess) return false

            val usersRaw = client.getHotspotUsers()
            val activeRaw = client.getHotspotActiveSessions()
            val profilesRaw = client.getHotspotProfiles()

            hotspotDao.clearHotspotUsersForRouter(router.id, router.userId)
            hotspotDao.clearActiveSessions(router.id, router.userId)
            hotspotDao.clearProfilesForRouter(router.id, router.userId)

            val activeUsersSet = activeRaw.mapNotNull { it["user"] }.toSet()

            val users = usersRaw.map { map ->
                val uName = map["name"] ?: ""
                HotspotUserEntity(
                    userId = router.userId,
                    routerId = router.id,
                    mikrotikId = map[".id"] ?: "",
                    name = uName,
                    password = map["password"] ?: "••••",
                    profile = map["profile"] ?: "default",
                    uptime = map["uptime"] ?: "0s",
                    bytesIn = map["bytes-in"] ?: "0",
                    bytesOut = map["bytes-out"] ?: "0",
                    limitUptime = map["limit-uptime"] ?: "",
                    limitBytesTotal = map["limit-bytes-total"] ?: "",
                    macAddress = map["mac-address"] ?: "",
                    comment = map["comment"] ?: "",
                    isDisabled = map["disabled"] == "true" || map["disabled"] == "yes",
                    isOnline = activeUsersSet.contains(uName)
                )
            }
            hotspotDao.insertHotspotUsers(users)

            val sessions = activeRaw.map { map ->
                HotspotActiveSessionEntity(
                    userId = router.userId,
                    routerId = router.id,
                    mikrotikId = map[".id"] ?: "",
                    username = map["user"] ?: "",
                    ipAddress = map["address"] ?: "",
                    macAddress = map["mac-address"] ?: "",
                    uptime = map["uptime"] ?: "0s",
                    bytesIn = map["bytes-in"] ?: "0",
                    bytesOut = map["bytes-out"] ?: "0",
                    loginBy = map["login-by"] ?: "http"
                )
            }
            hotspotDao.insertActiveSessions(sessions)

            val profiles = profilesRaw.mapNotNull { map ->
                val pName = map["name"]?.trim() ?: ""
                if (pName.isBlank()) null else HotspotProfileEntity(
                    userId = router.userId,
                    routerId = router.id,
                    name = pName,
                    rateLimit = map["rate-limit"] ?: "2M/4M",
                    sharedUsers = map["shared-users"]?.toIntOrNull() ?: 1,
                    sessionTimeout = map["session-timeout"] ?: "0s",
                    keepaliveTimeout = map["keepalive-timeout"] ?: "2m",
                    statusAutorefresh = map["status-autorefresh"] ?: "1m"
                )
            }
            hotspotDao.insertHotspotProfiles(profiles)
            true
        } catch (e: Exception) {
            Log.e(tag, "Failed to sync hotspot", e)
            false
        }
    }

    suspend fun addHotspotUser(
        router: RouterEntity,
        name: String,
        passwordVal: String,
        profile: String,
        comment: String,
        limitUptime: String,
        limitBytes: String
    ): MikroTikResponse {
        val client = MikroTikApiClient(router.host, router.port, router.username, router.password)
        val res = client.addHotspotUser(name, passwordVal, profile, comment, limitUptime, limitBytes)
        if (res.isSuccess) {
            hotspotDao.insertHotspotUser(
                HotspotUserEntity(
                    userId = router.userId,
                    routerId = router.id,
                    name = name,
                    password = passwordVal,
                    profile = profile,
                    comment = comment,
                    limitUptime = limitUptime,
                    limitBytesTotal = limitBytes
                )
            )
            addLog(
                userId = router.userId,
                action = "إضافة مستخدم هوتسبوت",
                details = "تمت إضافة المستخدم [$name] للراوتر [${router.name}]"
            )
        }
        return res
    }

    suspend fun deleteHotspotUser(router: RouterEntity, userEntity: HotspotUserEntity): MikroTikResponse {
        val client = MikroTikApiClient(router.host, router.port, router.username, router.password)
        val res = client.removeHotspotUser(if (userEntity.mikrotikId.isNotBlank()) userEntity.mikrotikId else userEntity.name)
        hotspotDao.deleteHotspotUser(userEntity)
        return res
    }

    suspend fun disconnectHotspotSession(router: RouterEntity, session: HotspotActiveSessionEntity): MikroTikResponse {
        val client = MikroTikApiClient(router.host, router.port, router.username, router.password)
        val res = client.removeHotspotActiveSession(if (session.mikrotikId.isNotBlank()) session.mikrotikId else session.username)
        syncHotspot(router)
        return res
    }

    suspend fun editHotspotUser(
        router: RouterEntity,
        userEntity: HotspotUserEntity,
        newName: String,
        newPassword: String,
        newProfile: String,
        comment: String,
        limitUptime: String,
        limitBytes: String
    ): MikroTikResponse {
        val client = MikroTikApiClient(router.host, router.port, router.username, router.password)
        val targetId = if (userEntity.mikrotikId.isNotBlank()) userEntity.mikrotikId else userEntity.name
        val res = client.editHotspotUser(targetId, newName, newPassword, newProfile, comment, limitUptime, limitBytes)
        if (res.isSuccess) {
            val updated = userEntity.copy(
                name = newName,
                password = newPassword,
                profile = newProfile,
                comment = comment,
                limitUptime = limitUptime,
                limitBytesTotal = limitBytes
            )
            hotspotDao.updateHotspotUser(updated)
            addLog(router.userId, "تعديل مستخدم هوتسبوت", "تم تعديل المستخدم [$newName] في راوتر [${router.name}]")
        }
        return res
    }

    suspend fun toggleHotspotUser(
        router: RouterEntity,
        userEntity: HotspotUserEntity,
        enable: Boolean
    ): MikroTikResponse {
        val client = MikroTikApiClient(router.host, router.port, router.username, router.password)
        val targetId = if (userEntity.mikrotikId.isNotBlank()) userEntity.mikrotikId else userEntity.name
        val res = client.toggleHotspotUser(targetId, enable)
        if (res.isSuccess) {
            val updated = userEntity.copy(isDisabled = !enable)
            hotspotDao.updateHotspotUser(updated)
            addLog(router.userId, if (enable) "تفعيل مستخدم" else "تعطيل مستخدم", "تم ${if (enable) "تفعيل" else "تعطيل"} مستخدم الهوتسبوت [${userEntity.name}]")
        }
        return res
    }

    suspend fun changeHotspotProfile(
        router: RouterEntity,
        userEntity: HotspotUserEntity,
        newProfile: String
    ): MikroTikResponse {
        val client = MikroTikApiClient(router.host, router.port, router.username, router.password)
        val targetId = if (userEntity.mikrotikId.isNotBlank()) userEntity.mikrotikId else userEntity.name
        val res = client.changeHotspotProfile(targetId, newProfile)
        if (res.isSuccess) {
            val updated = userEntity.copy(profile = newProfile)
            hotspotDao.updateHotspotUser(updated)
            addLog(router.userId, "تغيير فئة الهوتسبوت", "تم تغيير باقة المستخدم [${userEntity.name}] إلى [$newProfile]")
        }
        return res
    }

    suspend fun renewHotspotUser(
        router: RouterEntity,
        userEntity: HotspotUserEntity,
        newProfile: String,
        limitUptime: String = "",
        limitBytes: String = ""
    ): MikroTikResponse {
        val client = MikroTikApiClient(router.host, router.port, router.username, router.password)
        val targetId = if (userEntity.mikrotikId.isNotBlank()) userEntity.mikrotikId else userEntity.name
        val res = client.renewHotspotUser(targetId, newProfile, limitUptime, limitBytes)
        if (res.isSuccess) {
            val updated = userEntity.copy(
                profile = newProfile,
                isDisabled = false,
                bytesIn = "0",
                bytesOut = "0",
                uptime = "0s",
                limitUptime = limitUptime,
                limitBytesTotal = limitBytes
            )
            hotspotDao.updateHotspotUser(updated)
            addLog(router.userId, "تجديد كرت هوتسبوت", "تم تجديد الكرت [${userEntity.name}] بباقة [$newProfile]")
        }
        return res
    }

    suspend fun kickActiveUser(router: RouterEntity, activeId: String): MikroTikResponse {
        val client = MikroTikApiClient(router.host, router.port, router.username, router.password)
        val res = client.removeHotspotActiveSession(activeId)
        syncHotspot(router)
        return res
    }

    suspend fun blockDevice(router: RouterEntity, macAddress: String, ipAddress: String, comment: String): MikroTikResponse {
        val client = MikroTikApiClient(router.host, router.port, router.username, router.password)
        val res = client.blockHotspotUserMac(macAddress, ipAddress, comment)
        syncHotspot(router)
        addLog(router.userId, "حظر جهاز", "تم حظر الماك [$macAddress] للجهاز [$comment] في راوتر [${router.name}]")
        return res
    }

    suspend fun loadDetailedActiveDevices(router: RouterEntity): List<ActiveDeviceEntity> {
        val client = MikroTikApiClient(router.host, router.port, router.username, router.password)
        val raw = client.getDetailedConnectedDevices()
        return raw.map { map ->
            val bytesInNum = map["bytes-in"]?.toLongOrNull() ?: 0L
            val bytesOutNum = map["bytes-out"]?.toLongOrNull() ?: 0L
            val totalMb = (bytesInNum + bytesOutNum) / (1024 * 1024)
            val inMb = bytesInNum / (1024 * 1024)
            val outMb = bytesOutNum / (1024 * 1024)

            val limitBytes = map["limit_bytes_total"]?.toLongOrNull()
            val remainingQuota = if (limitBytes != null && limitBytes > 0) {
                val remMb = (limitBytes - (bytesInNum + bytesOutNum)) / (1024 * 1024)
                if (remMb > 1024) "${remMb / 1024} GB متبقية" else "$remMb MB متبقية"
            } else {
                "رصيد مفتوح"
            }

            val limitUptime = map["limit_uptime"] ?: ""
            val uptime = map["uptime"] ?: "0s"
            val remainingTime = if (limitUptime.isNotBlank()) "محدد ($limitUptime)" else "وقت غير محدد"

            ActiveDeviceEntity(
                id = map[".id"] ?: "",
                username = map["user"] ?: "مجهول",
                type = map["connection_type"] ?: "HOTSPOT",
                ipAddress = map["address"] ?: "",
                macAddress = map["mac-address"] ?: "",
                hostName = map["host_name"] ?: "",
                deviceType = map["device_type"] ?: "هاتف ذكي",
                uptime = uptime,
                startTime = map["login-by"] ?: "",
                bytesIn = "$inMb MB",
                bytesOut = "$outMb MB",
                totalBytes = "$totalMb MB",
                remainingQuota = remainingQuota,
                remainingTime = remainingTime,
                profile = map["profile"] ?: "default"
            )
        }
    }

    // ================= TELEGRAM DISPATCH =================

    suspend fun saveTelegramConfig(config: TelegramConfigEntity): Long {
        return telegramConfigDao.saveTelegramConfig(config)
    }

    private suspend fun dispatchTelegramAlert(userId: String, message: String) {
        val config = telegramConfigDao.getTelegramConfigSync(userId)
        if (config != null && config.isEnabled && config.botToken.isNotBlank() && config.chatId.isNotBlank()) {
            telegramManager.sendMessage(config.botToken, config.chatId, message)
        }
    }

    suspend fun installTelegramScriptToRouter(router: RouterEntity, token: String, chatId: String): MikroTikResponse {
        val client = MikroTikApiClient(router.host, router.port, router.username, router.password)
        val res = client.pushTelegramScript(token, chatId)
        if (res.isSuccess) {
            addLog(
                userId = router.userId,
                action = "تثبيت سكربت التليجرام",
                details = "تم تثبيت سكربت التنبيهات في الراوتر [${router.name}]"
            )
        }
        return res
    }

    suspend fun testRouterConnection(host: String, port: Int, username: String, pass: String): MikroTikResponse {
        val client = MikroTikApiClient(host, port, username, pass)
        return client.testConnection()
    }

    suspend fun importNeighbors(router: RouterEntity): Int {
        return try {
            val client = MikroTikApiClient(router.host, router.port, router.username, router.password)
            val neighbors = client.getNeighbors()
            val newDevices = neighbors.mapNotNull { n ->
                val ip = n["address"] ?: n["ipv4-address"] ?: return@mapNotNull null
                val name = n["identity"] ?: n["interface"] ?: "جهاز مكتشف"
                NetworkDeviceEntity(
                    userId = router.userId,
                    routerId = router.id,
                    name = name,
                    ip = ip,
                    type = DeviceType.MODEM,
                    status = DeviceStatus.ONLINE,
                    notes = "مستورد من راوتر ${router.name}"
                )
            }
            if (newDevices.isNotEmpty()) {
                networkDeviceDao.insertDevices(newDevices)
            }
            newDevices.size
        } catch (e: Exception) {
            Log.e(tag, "Failed to import neighbors", e)
            0
        }
    }

    suspend fun syncNetwatch(router: RouterEntity): Int {
        return try {
            val client = MikroTikApiClient(router.host, router.port, router.username, router.password)
            val netwatchList = client.getNetwatchList()
            val devices = netwatchList.mapNotNull { nw ->
                val host = nw["host"] ?: return@mapNotNull null
                val comment = nw["comment"] ?: "Netwatch Host"
                val status = if (nw["status"] == "up") DeviceStatus.ONLINE else DeviceStatus.OFFLINE
                NetworkDeviceEntity(
                    userId = router.userId,
                    routerId = router.id,
                    name = comment,
                    ip = host,
                    type = DeviceType.MAIN_TOWER,
                    status = status,
                    isNetwatchSynced = true,
                    notes = "مستورد من Netwatch"
                )
            }
            if (devices.isNotEmpty()) {
                networkDeviceDao.insertDevices(devices)
            }
            devices.size
        } catch (e: Exception) {
            Log.e(tag, "Failed to sync Netwatch", e)
            0
        }
    }

    suspend fun makeArpStatic(router: RouterEntity, device: NetworkDeviceEntity): MikroTikResponse {
        val client = MikroTikApiClient(router.host, router.port, router.username, router.password)
        return client.executeCommand(listOf("/ip/arp/add", "=address=${device.ip}", "=interface=bridge", "=comment=Static-TalalPro"))
    }

    // ================= CRUD BASIC METHODS =================

    suspend fun insertRouter(router: RouterEntity): Long = routerDao.insertRouter(router)
    suspend fun updateRouter(router: RouterEntity) = routerDao.updateRouter(router)
    suspend fun deleteRouter(router: RouterEntity) = routerDao.deleteRouter(router)

    suspend fun insertDevice(device: NetworkDeviceEntity): Long = networkDeviceDao.insertDevice(device)
    suspend fun insertDevices(devices: List<NetworkDeviceEntity>) = networkDeviceDao.insertDevices(devices)
    suspend fun updateDevice(device: NetworkDeviceEntity) = networkDeviceDao.updateDevice(device)
    suspend fun deleteDevice(device: NetworkDeviceEntity) = networkDeviceDao.deleteDevice(device)

    // ================= PORTS & NETWORK DISTRIBUTION =================

    suspend fun getRouterPorts(router: RouterEntity): List<NetworkPortEntity> {
        return try {
            val client = MikroTikApiClient(router.host, router.port, router.username, router.password)
            client.getRouterPorts()
        } catch (e: Exception) {
            Log.e(tag, "Failed to get router ports", e)
            emptyList()
        }
    }

    suspend fun setPortStatus(router: RouterEntity, portIdentifier: String, enable: Boolean): MikroTikResponse {
        val client = MikroTikApiClient(router.host, router.port, router.username, router.password)
        return client.setPortStatus(portIdentifier, enable)
    }

    suspend fun setPortCommentAndName(router: RouterEntity, portIdentifier: String, name: String, comment: String): MikroTikResponse {
        val client = MikroTikApiClient(router.host, router.port, router.username, router.password)
        return client.setPortCommentAndName(portIdentifier, name, comment)
    }

    suspend fun resetPortCounters(router: RouterEntity, portName: String): MikroTikResponse {
        val client = MikroTikApiClient(router.host, router.port, router.username, router.password)
        return client.resetPortCounters(portName)
    }

    suspend fun setPortSpeedLimit(router: RouterEntity, interfaceName: String, maxLimit: String): MikroTikResponse {
        val client = MikroTikApiClient(router.host, router.port, router.username, router.password)
        return client.setPortSpeedLimit(interfaceName, maxLimit)
    }

    suspend fun testPortCable(router: RouterEntity, interfaceName: String): String {
        val client = MikroTikApiClient(router.host, router.port, router.username, router.password)
        return client.testPortCable(interfaceName)
    }

    suspend fun getDistributionUnits(router: RouterEntity, localDevices: List<NetworkDeviceEntity>): List<DistributionDeviceItem> {
        return try {
            val client = MikroTikApiClient(router.host, router.port, router.username, router.password)
            client.getDistributionUnits(localDevices)
        } catch (e: Exception) {
            Log.e(tag, "Failed to get distribution units", e)
            emptyList()
        }
    }

    suspend fun addAlert(
        userId: String,
        title: String,
        message: String,
        severity: AlertSeverity = AlertSeverity.INFO,
        routerId: Long? = null,
        deviceId: Long? = null
    ): Long {
        return alertDao.insertAlert(
            AlertEntity(
                userId = userId,
                title = title,
                message = message,
                severity = severity,
                routerId = routerId,
                deviceId = deviceId
            )
        )
    }

    suspend fun markAllAlertsRead(userId: String) = alertDao.markAllAsRead(userId)
    suspend fun clearAllAlerts(userId: String) = alertDao.clearAllAlerts(userId)

    suspend fun addLog(userId: String, action: String, details: String, isSuccess: Boolean = true): Long {
        return networkLogDao.insertLog(
            NetworkLogEntity(
                userId = userId,
                action = action,
                details = details,
                isSuccess = isSuccess
            )
        )
    }

    suspend fun clearLogs(userId: String) = networkLogDao.clearLogs(userId)

    // ================= ADVANCED TELEGRAM BOT & CUSTOM BUTTONS =================

    fun getAdvancedTelegramConfig(userId: String): Flow<AdvancedTelegramBotEntity?> =
        advancedTelegramBotDao.getAdvancedConfigForUser(userId)

    suspend fun getAdvancedTelegramConfigSync(userId: String): AdvancedTelegramBotEntity? =
        advancedTelegramBotDao.getAdvancedConfigSync(userId)

    suspend fun saveAdvancedTelegramConfig(config: AdvancedTelegramBotEntity) {
        advancedTelegramBotDao.saveAdvancedConfig(config)
    }

    suspend fun deleteAdvancedTelegramBot(userId: String, router: RouterEntity?) {
        val cur = advancedTelegramBotDao.getAdvancedConfigSync(userId)
        if (cur != null && cur.botToken.isNotBlank()) {
            advancedTelegramManager.deleteWebhookAndReset(cur.botToken)
        }
        if (router != null) {
            val client = MikroTikApiClient(router.host, router.port, router.username, router.password)
            client.removeAdvancedTelegramCloudScript()
        }
        advancedTelegramBotDao.deleteByUserId(userId)
        customTelegramButtonDao.deleteAllForUser(userId)
        addLog(
            userId = userId,
            action = "حذف بوت تليجرام المتقدم",
            details = "تم حذف البوت المتقدم وجميع أزراره والسكربتات المرتبطة من المايكروتيك وتليجرام بنجاح"
        )
    }

    fun getAllCustomButtons(userId: String): Flow<List<TelegramCustomButtonEntity>> =
        customTelegramButtonDao.getAllButtons(userId)

    suspend fun saveCustomButton(button: TelegramCustomButtonEntity) {
        customTelegramButtonDao.saveButton(button)
    }

    suspend fun deleteCustomButton(button: TelegramCustomButtonEntity) {
        customTelegramButtonDao.deleteButton(button)
    }

    suspend fun seedDefaultAdvancedButtonsIfEmpty(userId: String) {
        val existing = customTelegramButtonDao.getAllButtonsSync(userId)
        if (existing.isEmpty()) {
            val defaults = listOf(
                TelegramCustomButtonEntity("btn_menu", userId, "📋 القائمة الرئيسية", "القائمة", "عرض لوحة الأوامر الرئيسية", "TEXT", "", true, true, 1),
                TelegramCustomButtonEntity("btn_status", userId, "🟢 الحالة المباشرة", "الحالة", "فحص المعالج والذاكرة والحرارة", "ROUTER_COMMAND", "", true, true, 2),
                TelegramCustomButtonEntity("btn_cards", userId, "🎫 الكروت", "الكروت", "فتح قائمة إدارة وتوليد والبحث عن الكروت", "SUB_MENU_CARDS", "", true, true, 3),
                TelegramCustomButtonEntity("btn_active", userId, "👥 المتصلين الآن", "المتصلين", "كشف المتصلين بالهوتسبوت وPPPoE", "ROUTER_COMMAND", "", true, true, 4),
                TelegramCustomButtonEntity("btn_suspect", userId, "⚠️ فحص الاشتباه", "فحص الاشتباه", "كشف الكروت المستخدمة في جهازين", "SUSPICIOUS_CHECK", "", true, true, 5),
                TelegramCustomButtonEntity("btn_profiles", userId, "📦 باقات الشبكة", "الباقات", "استعراض باقات وفئات الراوتر", "ROUTER_COMMAND", "", true, true, 6),
                TelegramCustomButtonEntity("btn_dist", userId, "📡 قطع التوزيع", "قطع التوزيع", "مراقبة قطع التوزيع وعدد المتصلين", "ROUTER_COMMAND", "", true, true, 7),
                TelegramCustomButtonEntity("btn_ports", userId, "🔌 منافذ الشبكة", "منافذ الشبكة", "حالة المنافذ المتصلة وغير المتصلة", "ROUTER_COMMAND", "", true, true, 8),
                TelegramCustomButtonEntity("btn_port_traffic", userId, "📊 استهلاك المنافذ", "استهلاك المنافذ", "استهلاك الإنترنت لكل منفذ", "ROUTER_COMMAND", "", true, true, 9),
                TelegramCustomButtonEntity("btn_perf", userId, "📈 ملخص الاداء", "ملخص الاداء", "ضغط المعالج واستقرار البينج", "ROUTER_COMMAND", "", true, true, 10),
                TelegramCustomButtonEntity("btn_backup", userId, "💾 نسخة احتياطية", "نسخة احتياطية", "حفظ وتصدير ملفات الباك اب", "ROUTER_COMMAND", "", true, true, 11),
                TelegramCustomButtonEntity("btn_optimize", userId, "⚡ تحسين الشبكة", "تحسين الشبكة", "تفريغ الكاش وتسريع التصفح", "ROUTER_COMMAND", "", true, true, 12)
            )
            customTelegramButtonDao.insertButtons(defaults)
        }
    }

    suspend fun detectCardSharingSuspects(router: RouterEntity): List<SuspiciousCardSharingAlert> {
        val client = MikroTikApiClient(router.host, router.port, router.username, router.password)
        return client.getCardSharingSuspects(router.name)
    }

    suspend fun batchCreateCards(
        router: RouterEntity,
        system: String,
        profile: String,
        count: Int,
        validityDays: Int?
    ): List<Pair<String, String>> {
        val client = MikroTikApiClient(router.host, router.port, router.username, router.password)
        return client.batchCreateCards(system, profile, count, validityDays)
    }

    suspend fun searchCard(router: RouterEntity, cardQuery: String): Map<String, Any?>? {
        val client = MikroTikApiClient(router.host, router.port, router.username, router.password)
        return client.searchCard(cardQuery)
    }

    suspend fun disableCard(router: RouterEntity, cardQuery: String): MikroTikResponse {
        val client = MikroTikApiClient(router.host, router.port, router.username, router.password)
        return client.disableCard(cardQuery)
    }

    suspend fun enableCard(router: RouterEntity, cardQuery: String): MikroTikResponse {
        val client = MikroTikApiClient(router.host, router.port, router.username, router.password)
        return client.enableCard(cardQuery)
    }

    suspend fun deleteCard(router: RouterEntity, cardQuery: String): MikroTikResponse {
        val client = MikroTikApiClient(router.host, router.port, router.username, router.password)
        return client.deleteCard(cardQuery)
    }

    suspend fun installAdvancedCloudScript(
        router: RouterEntity,
        token: String,
        chatId: String,
        detectSharing: Boolean = true
    ): MikroTikResponse {
        val client = MikroTikApiClient(router.host, router.port, router.username, router.password)
        val res = client.pushAdvancedTelegramCloudScript(token, chatId, detectSharing)
        if (res.isSuccess) {
            addLog(
                userId = router.userId,
                action = "تثبيت سكربت البوت المتقدم التلقائي",
                details = "تم تثبيت وجدولة سكربت البوت السحابي المستقل في راوتر [${router.name}]"
            )
        }
        return res
    }

    suspend fun removeAdvancedCloudScript(router: RouterEntity): MikroTikResponse {
        val client = MikroTikApiClient(router.host, router.port, router.username, router.password)
        return client.removeAdvancedTelegramCloudScript()
    }

    fun generateAdvancedRouterScriptText(
        token: String,
        chatId: String,
        routerName: String,
        detectSharing: Boolean = true
    ): String {
        val d = "$"
        val suspectBlock = if (detectSharing) {
            """
            :local foundCards ""
            :foreach act in=[/ip hotspot active find] do={
                :local u [/ip hotspot active get ${d}act user]
                :local m [/ip hotspot active get ${d}act mac-address]
                :local count 0
                :local macs ""
                :foreach other in=[/ip hotspot active find user=${d}u] do={
                    :local om [/ip hotspot active get ${d}other mac-address]
                    :set count (${d}count + 1)
                    :set macs (${d}macs . "• " . ${d}om . "%0A")
                }
                :if (${d}count > 1) do={
                    :local alertMsg "⚠️ *اشتباه مشاركة كارت*%0A%0Aالكارت: \`${d}u\`%0Aظهر على ${d}count أجهزة:%0A${d}macs%0Aتقدر توقفه من التطبيق أو من البوت عبر الأمر:%0A/disable_card_${d}u"
                    /tool fetch url="https://api.telegram.org/bot$token/sendMessage?chat_id=$chatId&text=${d}alertMsg&parse_mode=Markdown" keep-result=no
                }
            }
            """.trimIndent()
        } else ""

        return """
            # ========================================================
            # TALAL PRO - Advanced Cloud Autonomous Router Script
            # For Router: $routerName
            # ========================================================
            :local bot "$token"
            :local chat "$chatId"
            :local rName [/system identity get name]
            :local rCpu [/system resource get cpu-load]
            :local rMem [/system resource get free-memory]
            :local rUp [/system resource get uptime]
            
            $suspectBlock
            
            :if (${d}rCpu > 85) do={
                :local highCpuMsg "⚠️ *تحذير ارتفاع حمل المعالج!*%0A📡 الراوتر: ${d}rName%0A⚡ CPU: ${d}rCpu%%0A⏱ مدة التشغيل: ${d}rUp"
                /tool fetch url="https://api.telegram.org/bot${d}bot/sendMessage?chat_id=${d}chat&text=${d}highCpuMsg&parse_mode=Markdown" keep-result=no
            }
        """.trimIndent()
    }

    // ================= CARD BATCHES MANAGEMENT =================

    fun getCardBatches(userId: String): Flow<List<CardBatchEntity>> =
        cardBatchDao.getBatchesForUser(userId)

    fun getBatchItems(batchId: Long): Flow<List<CardBatchItemEntity>> =
        cardBatchDao.getItemsForBatch(batchId)

    suspend fun getBatchItemsSync(batchId: Long): List<CardBatchItemEntity> =
        cardBatchDao.getItemsForBatchSync(batchId)

    suspend fun saveCardBatchWithItems(
        batch: CardBatchEntity,
        items: List<CardBatchItemEntity>
    ): Long {
        val batchId = cardBatchDao.insertBatch(batch)
        val mapped = items.map { it.copy(batchId = batchId) }
        cardBatchDao.insertBatchItems(mapped)
        return batchId
    }

    suspend fun updateCardBatch(batch: CardBatchEntity) {
        cardBatchDao.updateBatch(batch)
    }

    suspend fun updateBatchItemsDisabled(batchId: Long, disabled: Boolean) {
        cardBatchDao.updateBatchItemsDisabled(batchId, disabled)
    }

    suspend fun updateBatchItemsProfile(batchId: Long, newProfile: String) {
        cardBatchDao.updateBatchItemsProfile(batchId, newProfile)
    }

    suspend fun updateBatchItemDisabled(itemId: Long, disabled: Boolean) {
        cardBatchDao.updateBatchItemDisabled(itemId, disabled)
    }

    suspend fun deleteBatchItem(itemId: Long) {
        cardBatchDao.deleteBatchItem(itemId)
    }

    suspend fun deleteCardBatch(batchId: Long) {
        cardBatchDao.deleteItemsByBatchId(batchId)
        cardBatchDao.deleteBatchById(batchId)
    }

    suspend fun addBatchCardsToRouter(
        router: RouterEntity,
        serverType: String,
        cards: List<Pair<String, String>>,
        profile: String,
        comment: String
    ): Pair<Int, String?> {
        val client = MikroTikApiClient(router.host, router.port, router.username, router.password)
        val result = client.addBatchCardsOnRouter(serverType, cards, profile, comment)
        if (result.first > 0) {
            addLog(
                userId = router.userId,
                action = "إنشاء دفعة كروت",
                details = "تم إنشاء ${result.first} كرت ($serverType - $profile) في راوتر [${router.name}]",
                isSuccess = true
            )
        } else {
            addLog(
                userId = router.userId,
                action = "فشل رفع دفعة كروت",
                details = "تعذر رفع الدفعة إلى [${router.name}]: ${result.second ?: "غير متصل"}",
                isSuccess = false
            )
        }
        return result
    }

    suspend fun toggleBatchOnRouter(
        router: RouterEntity,
        serverType: String,
        usernames: List<String>,
        enable: Boolean
    ): MikroTikResponse {
        val client = MikroTikApiClient(router.host, router.port, router.username, router.password)
        val res = client.toggleBatchCardsOnRouter(serverType, usernames, enable)
        if (res.isSuccess) {
            addLog(
                userId = router.userId,
                action = if (enable) "تفعيل دفعة كروت" else "إيقاف دفعة كروت",
                details = "تم ${if (enable) "تفعيل" else "تعطيل"} ${usernames.size} كرت في راوتر [${router.name}]"
            )
        }
        return res
    }

    suspend fun changeBatchProfileOnRouter(
        router: RouterEntity,
        serverType: String,
        usernames: List<String>,
        newProfile: String
    ): MikroTikResponse {
        val client = MikroTikApiClient(router.host, router.port, router.username, router.password)
        val res = client.changeBatchProfileOnRouter(serverType, usernames, newProfile)
        if (res.isSuccess) {
            addLog(
                userId = router.userId,
                action = "تعديل باقة دفعة كروت",
                details = "تم تغيير باقة ${usernames.size} كرت إلى [$newProfile] في [${router.name}]"
            )
        }
        return res
    }

    suspend fun deleteBatchOnRouter(
        router: RouterEntity,
        serverType: String,
        usernames: List<String>
    ): MikroTikResponse {
        val client = MikroTikApiClient(router.host, router.port, router.username, router.password)
        val res = client.deleteBatchCardsOnRouter(serverType, usernames)
        if (res.isSuccess) {
            addLog(
                userId = router.userId,
                action = "حذف دفعة كروت من الراوتر",
                details = "تم حذف ${usernames.size} كرت نهائياً من راوتر [${router.name}]"
            )
        }
        return res
    }
}

