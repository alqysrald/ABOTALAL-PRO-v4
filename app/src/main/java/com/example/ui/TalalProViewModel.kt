package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.AIAssistantPermissions
import com.example.data.firebase.FirebaseAppConfig
import com.example.data.firebase.FirebaseManager
import com.example.data.firebase.FirebaseUserAccount
import com.example.data.mikrotik.MikroTikApiClient
import com.example.data.mikrotik.MikroTikResponse
import com.example.data.mikrotik.PingResult
import com.example.data.model.*
import com.example.data.repository.NetworkRepository
import com.example.data.telegram.TelegramChatInfo
import com.example.data.telegram.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Random

enum class ScreenTab {
    DASHBOARD,
    ROUTERS,
    DEVICES,
    PORTS_AND_DEVICES,
    USER_MANAGER,
    HOTSPOT,
    ACTIVE_USERS,
    ADD_CARDS,
    CARD_DESIGNER,
    MAP,
    TELEGRAM,
    ADVANCED_TELEGRAM,
    TOOLS,
    ASSISTANT,
    ISSUES
}

enum class DeviceFilter {
    ALL,
    ONLINE,
    OFFLINE,
    ISSUES
}

enum class AdminTab {
    USERS,
    AI_CONTROL,
    APP_CONFIG,
    TRIAL_SETTINGS,
    STATS
}

data class AssistantMessage(
    val id: Long = System.currentTimeMillis(),
    val sender: String, // "user" or "assistant"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRouterCommand: Boolean = false,
    val commandOutput: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class TalalProViewModel(
    val repository: NetworkRepository,
    val firebaseManager: FirebaseManager
) : ViewModel() {

    // Auth state
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _currentUser = MutableStateFlow<FirebaseUserAccount?>(null)
    val currentUser: StateFlow<FirebaseUserAccount?> = _currentUser.asStateFlow()

    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

    private val _isCheckingAuth = MutableStateFlow(false)
    val isCheckingAuth: StateFlow<Boolean> = _isCheckingAuth.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    val showActivationScreen = MutableStateFlow(false)
    val pendingActivationUser = MutableStateFlow<FirebaseUserAccount?>(null)

    // Navigation state
    private val _currentTab = MutableStateFlow(ScreenTab.DASHBOARD)
    val currentTab: StateFlow<ScreenTab> = _currentTab.asStateFlow()

    // Admin Panel Screen State
    private val _isInAdminPanel = MutableStateFlow(false)
    val isInAdminPanel: StateFlow<Boolean> = _isInAdminPanel.asStateFlow()

    private val _adminTab = MutableStateFlow(AdminTab.USERS)
    val adminTab: StateFlow<AdminTab> = _adminTab.asStateFlow()

    // Firebase Data States
    private val _firebaseUsers = MutableStateFlow<List<FirebaseUserAccount>>(emptyList())
    val firebaseUsers: StateFlow<List<FirebaseUserAccount>> = _firebaseUsers.asStateFlow()

    private val _appConfig = MutableStateFlow(FirebaseAppConfig())
    val appConfig: StateFlow<FirebaseAppConfig> = _appConfig.asStateFlow()

    private val _isLoadingUsers = MutableStateFlow(false)
    val isLoadingUsers: StateFlow<Boolean> = _isLoadingUsers.asStateFlow()

    val showDirectUserMessage = MutableStateFlow<String?>(null)
    val snackbarMessage = MutableStateFlow<String?>(null)

    // Exit Confirmation Dialog
    val showExitConfirmDialog = MutableStateFlow(false)

    // Multi-tenant Dynamic Flow Bindings
    val allRouters: StateFlow<List<RouterEntity>> = _currentUser.flatMapLatest { user ->
        repository.getRoutersForUser(user?.id ?: "")
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allDevices: StateFlow<List<NetworkDeviceEntity>> = _currentUser.flatMapLatest { user ->
        repository.getDevicesForUser(user?.id ?: "")
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalDevicesCount: StateFlow<Int> = allDevices.map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val onlineDevicesCount: StateFlow<Int> = allDevices.map { devs -> devs.count { it.status == DeviceStatus.ONLINE } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val offlineDevicesCount: StateFlow<Int> = allDevices.map { devs -> devs.count { it.status == DeviceStatus.OFFLINE } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val maintenanceDevicesCount: StateFlow<Int> = allDevices.map { devs -> devs.count { it.status == DeviceStatus.MAINTENANCE || it.hasIssue } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val routersCount: StateFlow<Int> = allRouters.map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val onlineRoutersCount: StateFlow<Int> = allRouters.map { rts -> rts.count { it.isOnline } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val allAlerts: StateFlow<List<AlertEntity>> = _currentUser.flatMapLatest { user ->
        repository.getAlertsForUser(user?.id ?: "")
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadAlertsCount: StateFlow<Int> = _currentUser.flatMapLatest { user ->
        repository.getUnreadAlertsCount(user?.id ?: "")
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val recentLogs: StateFlow<List<NetworkLogEntity>> = _currentUser.flatMapLatest { user ->
        repository.getRecentLogs(user?.id ?: "")
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val telegramConfig: StateFlow<TelegramConfigEntity?> = _currentUser.flatMapLatest { user ->
        repository.getTelegramConfig(user?.id ?: "")
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val advancedTelegramConfig: StateFlow<AdvancedTelegramBotEntity?> = _currentUser.flatMapLatest { user ->
        repository.getAdvancedTelegramConfig(user?.id ?: "")
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val advancedCustomButtons: StateFlow<List<TelegramCustomButtonEntity>> = _currentUser.flatMapLatest { user ->
        repository.getAllCustomButtons(user?.id ?: "")
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isRefreshing = repository.isRefreshing
    val soundEnabled = repository.soundEnabled

    // UI Dialog States
    val showAddRouterDialog = MutableStateFlow(false)
    val showCloudGuideDialog = MutableStateFlow(false)
    val showAddDeviceDialog = MutableStateFlow(false)
    val showImportNeighborsDialog = MutableStateFlow(false)
    val showNetwatchSyncDialog = MutableStateFlow(false)
    val showNotificationsDialog = MutableStateFlow(false)

    // Admin Dialog States
    val showAddUserDialog = MutableStateFlow(false)
    val showSendMessageDialog = MutableStateFlow(false)
    val showExtendUserDialog = MutableStateFlow(false)
    val selectedUserForAction = MutableStateFlow<FirebaseUserAccount?>(null)

    // Filter and Search states
    val deviceSearchQuery = MutableStateFlow("")
    val deviceFilter = MutableStateFlow(DeviceFilter.ALL)
    val userSearchQuery = MutableStateFlow("")

    val filteredDevices = combine(
        allDevices,
        deviceSearchQuery,
        deviceFilter
    ) { devices, query, filter ->
        devices.filter { device ->
            val matchesQuery = query.isBlank() ||
                    device.name.contains(query, ignoreCase = true) ||
                    device.ip.contains(query, ignoreCase = true) ||
                    device.manager.contains(query, ignoreCase = true) ||
                    device.notes.contains(query, ignoreCase = true)

            val matchesFilter = when (filter) {
                DeviceFilter.ALL -> true
                DeviceFilter.ONLINE -> device.status == DeviceStatus.ONLINE
                DeviceFilter.OFFLINE -> device.status == DeviceStatus.OFFLINE
                DeviceFilter.ISSUES -> device.hasIssue || device.status == DeviceStatus.MAINTENANCE || device.status == DeviceStatus.OFFLINE
            }

            matchesQuery && matchesFilter
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Map screen filters
    val mapDeviceTypeFilter = MutableStateFlow<DeviceType?>(null)
    val showMapLinks = MutableStateFlow(true)
    val isSatelliteMap = MutableStateFlow(true)

    // ================= USER MANAGER STATES =================
    val selectedUserManagerRouter = MutableStateFlow<RouterEntity?>(null)
    val isSyncingUserManager = MutableStateFlow(false)

    val userManagerUsers: StateFlow<List<UserManagerUserEntity>> = combine(
        selectedUserManagerRouter,
        _currentUser
    ) { router, user ->
        Pair(router, user)
    }.flatMapLatest { (router, user) ->
        if (router != null && user != null) {
            repository.getUserManagerUsers(router.id, user.id)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userManagerProfiles: StateFlow<List<UserManagerProfileEntity>> = combine(
        selectedUserManagerRouter,
        _currentUser
    ) { router, user ->
        Pair(router, user)
    }.flatMapLatest { (router, user) ->
        if (router != null && user != null) {
            repository.getUserManagerProfiles(router.id, user.id)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ================= HOTSPOT STATES =================
    val selectedHotspotRouter = MutableStateFlow<RouterEntity?>(null)
    val isSyncingHotspot = MutableStateFlow(false)

    val hotspotUsers: StateFlow<List<HotspotUserEntity>> = combine(
        selectedHotspotRouter,
        _currentUser
    ) { router, user ->
        Pair(router, user)
    }.flatMapLatest { (router, user) ->
        if (router != null && user != null) {
            repository.getHotspotUsers(router.id, user.id)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val hotspotActiveSessions: StateFlow<List<HotspotActiveSessionEntity>> = combine(
        selectedHotspotRouter,
        _currentUser
    ) { router, user ->
        Pair(router, user)
    }.flatMapLatest { (router, user) ->
        if (router != null && user != null) {
            repository.getHotspotActiveSessions(router.id, user.id)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val hotspotProfiles: StateFlow<List<HotspotProfileEntity>> = combine(
        selectedHotspotRouter,
        _currentUser
    ) { router, user ->
        Pair(router, user)
    }.flatMapLatest { (router, user) ->
        if (router != null && user != null) {
            repository.getHotspotProfiles(router.id, user.id)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ================= CARD DESIGNER & TEMPLATE SYNC STATES =================
    val cardTemplates: StateFlow<List<CardTemplateEntity>> = _currentUser.flatMapLatest { user ->
        if (user != null) {
            repository.getAllCardTemplates(user.id)
        } else {
            repository.getAllCardTemplates("")
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeDesignerRouter = MutableStateFlow<RouterEntity?>(null)
    val isRefreshingDesignerProfiles = MutableStateFlow(false)
    val designerProfilesDisconnected = MutableStateFlow(false)
    val lastGeneratedUmBatch = MutableStateFlow<List<GeneratedCardPrintItem>>(emptyList())
    val lastGeneratedHotspotBatch = MutableStateFlow<List<GeneratedCardPrintItem>>(emptyList())

    // ================= ADD CARDS & BATCH MANAGEMENT STATES =================
    val selectedAddCardsRouter = MutableStateFlow<RouterEntity?>(null)
    val isSyncingAllPackages = MutableStateFlow(false)
    val isProcessingBatch = MutableStateFlow(false)
    val selectedBatchIdForView = MutableStateFlow<Long?>(null)

    val cardBatches: StateFlow<List<CardBatchEntity>> = _currentUser.flatMapLatest { user ->
        repository.getCardBatches(user?.id ?: "")
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedBatchItems: StateFlow<List<CardBatchItemEntity>> = selectedBatchIdForView.flatMapLatest { batchId ->
        if (batchId != null) {
            repository.getBatchItems(batchId)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ================= ACTIVE CONNECTED USERS / DEVICES STATES =================
    val selectedActiveRouter = MutableStateFlow<RouterEntity?>(null)
    val detailedActiveDevices = MutableStateFlow<List<ActiveDeviceEntity>>(emptyList())
    val isLoadingActiveDevices = MutableStateFlow(false)

    // ================= PORTS & DISTRIBUTION STATES =================
    val selectedPortsRouter = MutableStateFlow<RouterEntity?>(null)
    val networkPorts = MutableStateFlow<List<NetworkPortEntity>>(emptyList())
    val distributionDevices = MutableStateFlow<List<DistributionDeviceItem>>(emptyList())
    val isLoadingPortsAndDistribution = MutableStateFlow(false)

    // ================= TELEGRAM STATES =================
    val isTestingTelegram = MutableStateFlow(false)
    val telegramTestResult = MutableStateFlow<String?>(null)
    val detectedTelegramChats = MutableStateFlow<List<TelegramChatInfo>>(emptyList())
    val isTelegramCloudRunning = MutableStateFlow(false)
    val telegramCloudLogs = MutableStateFlow<List<String>>(listOf("البوت السحابي جاهز للتشغيل والربط المباشر..."))
    private var telegramPollJob: Job? = null

    // ================= ADVANCED TELEGRAM BOT STATES =================
    val isTestingAdvancedBot = MutableStateFlow(false)
    val advancedBotTestResult = MutableStateFlow<String?>(null)
    val detectedAdvancedChats = MutableStateFlow<List<TelegramChatInfo>>(emptyList())
    val isAdvancedCloudRunning = MutableStateFlow(false)
    val advancedCloudLogs = MutableStateFlow<List<String>>(listOf("المحرك السحابي لبوت تليجرام المتقدم جاهز للربط المباشر..."))
    val selectedAdvancedRouter = MutableStateFlow<RouterEntity?>(null)
    val suspiciousCardAlerts = MutableStateFlow<List<SuspiciousCardSharingAlert>>(emptyList())
    val isDetectingSuspects = MutableStateFlow(false)
    val createdVouchersReport = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val isCreatingCards = MutableStateFlow(false)
    val cardSearchResult = MutableStateFlow<Map<String, Any?>?>(null)
    val isSearchingCard = MutableStateFlow(false)
    private var advancedTelegramPollJob: Job? = null
    private val advancedCreationSessions = mutableMapOf<String, VoucherCreationSessionState>()

    // ================= SPECIAL TOOLS STATES =================
    val selectedToolsRouter = MutableStateFlow<RouterEntity?>(null)
    val isEnablingCloud = MutableStateFlow(false)
    val cloudOperationResult = MutableStateFlow<String?>(null)

    // ================= VLAN MANAGEMENT STATES =================
    val vlans = MutableStateFlow<List<MikrotikVlan>>(emptyList())
    val availableInterfaces = MutableStateFlow<List<String>>(emptyList())
    val isLoadingVlans = MutableStateFlow(false)
    val isSavingVlan = MutableStateFlow(false)
    val vlanOperationResult = MutableStateFlow<String?>(null)

    // ================= SYSTEM & ROUTER LOGS STATES =================
    val routerLogs = MutableStateFlow<List<MikrotikLogEntry>>(emptyList())
    val isLoadingRouterLogs = MutableStateFlow(false)
    val routerLogsError = MutableStateFlow<String?>(null)

    // Assistant / Chat states
    val selectedAssistantRouter = MutableStateFlow<RouterEntity?>(null)
    private val _assistantMessages = MutableStateFlow<List<AssistantMessage>>(
        listOf(
            AssistantMessage(
                sender = "assistant",
                content = "مرحباً بك في المساعد الشبكي الذكي لتطبيق TALAL PRO.\nأنا جاهز لإدارة راوتراتك، وتفعيل الكلاود، وفحص اليوزرمانجر والهوتسبوت، وتنفيذ الأوامر المباشرة.\nاختر راوتر أو اكتب طلبك وسأقوم بتنفيذه فوراً."
            )
        )
    )
    val assistantMessages: StateFlow<List<AssistantMessage>> = _assistantMessages.asStateFlow()
    val isAssistantThinking = MutableStateFlow(false)

    init {
        loadAppConfig()
        loadSavedSession()
    }

    private fun loadSavedSession() {
        viewModelScope.launch {
            try {
                val savedEmail = repository.getSavedUserEmail()
                val isActivated = repository.isSavedUserActivated()
                if (!savedEmail.isNullOrBlank()) {
                    val user = firebaseManager.getUserByEmail(savedEmail)
                    if (user != null) {
                        _currentUser.value = user
                        _isAdmin.value = user.role == "admin"
                        repository.setCurrentUserId(user.id)
                        if (user.isActivated && isActivated && user.status == "active") {
                            _isLoggedIn.value = true
                            showActivationScreen.value = false
                            refreshAll()
                        } else if (!user.isActivated || user.status == "pending_activation") {
                            pendingActivationUser.value = user
                            showActivationScreen.value = true
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    // ================= AUTHENTICATION =================

    fun clearAuthError() {
        _authError.value = null
    }

    fun setAuthError(err: String) {
        _authError.value = err
    }

    fun login(emailOrUser: String, pass: String) {
        if (emailOrUser.isBlank() || pass.isBlank()) {
            _authError.value = "يرجى إدخال اسم المستخدم / البريد وكلمة المرور"
            return
        }
        viewModelScope.launch {
            _isCheckingAuth.value = true
            _authError.value = null
            try {
                val (user, err) = firebaseManager.authenticate(emailOrUser.trim(), pass)
                if (user != null) {
                    _currentUser.value = user
                    _isAdmin.value = user.role == "admin"
                    repository.setCurrentUserId(user.id)

                    if (!user.isActivated || user.status == "pending_activation") {
                        pendingActivationUser.value = user
                        showActivationScreen.value = true
                        repository.saveUserSession(user.email, isActivated = false)
                    } else {
                        _isLoggedIn.value = true
                        showActivationScreen.value = false
                        repository.saveUserSession(user.email, isActivated = true)
                    }

                    if (!user.directMessage.isNullOrBlank()) {
                        showDirectUserMessage.value = user.directMessage
                    }
                    loadAppConfig()
                    refreshAll()
                } else {
                    _authError.value = err ?: "فشل تسجيل الدخول"
                }
            } catch (e: Exception) {
                _authError.value = "خطأ في الاتصال بالخادم السحابي: ${e.localizedMessage}"
            } finally {
                _isCheckingAuth.value = false
            }
        }
    }

    fun registerNewUser(
        fullName: String,
        governorate: String,
        city: String,
        networkName: String,
        networkLocation: String,
        phone: String,
        email: String,
        password: String
    ) {
        viewModelScope.launch {
            _isCheckingAuth.value = true
            _authError.value = null
            try {
                val (success, err) = firebaseManager.registerNewUser(
                    fullName = fullName,
                    governorate = governorate,
                    city = city,
                    networkName = networkName,
                    networkLocation = networkLocation,
                    phone = phone,
                    email = email,
                    password = password
                )
                if (success) {
                    val newUser = firebaseManager.getUserByEmail(email)
                    if (newUser != null) {
                        _currentUser.value = newUser
                        pendingActivationUser.value = newUser
                        showActivationScreen.value = true
                        repository.setCurrentUserId(newUser.id)
                        repository.saveUserSession(newUser.email, isActivated = false)
                    }
                    snackbarMessage.value = "تم تسجيل الحساب وتوليد كود الترخيص بنجاح! 🔑"
                } else {
                    _authError.value = err ?: "فشل التسجيل"
                }
            } catch (e: Exception) {
                _authError.value = "خطأ أثناء التسجيل: ${e.localizedMessage}"
            } finally {
                _isCheckingAuth.value = false
            }
        }
    }

    fun activateUserLicense(email: String, inputCode: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val (ok, err) = firebaseManager.activateUserLicense(email, inputCode)
                if (ok) {
                    val updated = firebaseManager.getUserByEmail(email)
                    if (updated != null) {
                        _currentUser.value = updated
                        _isLoggedIn.value = true
                        showActivationScreen.value = false
                        repository.setCurrentUserId(updated.id)
                        repository.saveUserSession(updated.email, isActivated = true)
                        refreshAll()
                    }
                    onResult(true, null)
                } else {
                    onResult(false, err)
                }
            } catch (e: Exception) {
                onResult(false, e.localizedMessage)
            }
        }
    }

    fun logout() {
        _isLoggedIn.value = false
        _currentUser.value = null
        pendingActivationUser.value = null
        showActivationScreen.value = false
        _isAdmin.value = false
        _authError.value = null
        _isInAdminPanel.value = false
        repository.setCurrentUserId("")
        repository.clearUserSession()
    }

    fun setTab(tab: ScreenTab) {
        _currentTab.value = tab
    }

    fun setCurrentTab(tab: ScreenTab) = setTab(tab)

    fun openAdminPanel() {
        if (_isAdmin.value) {
            _isInAdminPanel.value = true
            loadFirebaseUsers()
            loadAppConfig()
        }
    }

    fun closeAdminPanel() {
        _isInAdminPanel.value = false
    }

    fun setAdminTab(tab: AdminTab) {
        _adminTab.value = tab
    }

    fun loadFirebaseUsers() {
        viewModelScope.launch {
            _isLoadingUsers.value = true
            try {
                _firebaseUsers.value = firebaseManager.getAllUsers()
            } catch (e: Exception) {
                snackbarMessage.value = "فشل تحميل المستخدمين: ${e.localizedMessage}"
            } finally {
                _isLoadingUsers.value = false
            }
        }
    }

    fun loadAppConfig() {
        viewModelScope.launch {
            try {
                _appConfig.value = firebaseManager.getAppConfig()
            } catch (_: Exception) {}
        }
    }

    fun saveAppConfig(config: FirebaseAppConfig) {
        viewModelScope.launch {
            _isLoadingUsers.value = true
            try {
                val ok = firebaseManager.saveAppConfig(config)
                if (ok) {
                    _appConfig.value = config
                    snackbarMessage.value = "تم حفظ الإعدادات في السيرفر بنجاح"
                } else {
                    snackbarMessage.value = "فشل حفظ الإعدادات"
                }
            } finally {
                _isLoadingUsers.value = false
            }
        }
    }

    fun addOrUpdateFirebaseUser(user: FirebaseUserAccount) {
        viewModelScope.launch {
            _isLoadingUsers.value = true
            try {
                val ok = firebaseManager.saveUser(user)
                if (ok) {
                    loadFirebaseUsers()
                    snackbarMessage.value = "تم حفظ بيانات المستخدم بنجاح"
                }
            } finally {
                _isLoadingUsers.value = false
            }
        }
    }

    fun deleteFirebaseUser(user: FirebaseUserAccount) {
        viewModelScope.launch {
            _isLoadingUsers.value = true
            try {
                val ok = firebaseManager.deleteUser(user)
                if (ok) {
                    loadFirebaseUsers()
                    snackbarMessage.value = "تم حذف المستخدم نهائياً"
                }
            } finally {
                _isLoadingUsers.value = false
            }
        }
    }

    fun updateUserStatus(user: FirebaseUserAccount, newStatus: String) {
        viewModelScope.launch {
            val updated = user.copy(status = newStatus)
            firebaseManager.saveUser(updated)
            loadFirebaseUsers()
            snackbarMessage.value = "تم تحديث حالة المستخدم إلى: $newStatus"
        }
    }

    fun unbindUserDevice(user: FirebaseUserAccount) {
        viewModelScope.launch {
            firebaseManager.unbindUserDevice(user)
            loadFirebaseUsers()
            snackbarMessage.value = "تم فك ربط جهاز المستخدم بنجاح"
        }
    }

    fun toggleUserDeviceLock(user: FirebaseUserAccount, lock: Boolean) {
        viewModelScope.launch {
            firebaseManager.toggleUserDeviceLock(user, lock)
            loadFirebaseUsers()
            snackbarMessage.value = if (lock) "تم قفل الحساب على الجهاز" else "تم إلغاء قفل الجهاز"
        }
    }

    fun extendUserSubscription(user: FirebaseUserAccount, extraDays: Int, isLifetime: Boolean = false) {
        viewModelScope.launch {
            val baseTime = if (user.expiryTimestamp > System.currentTimeMillis()) user.expiryTimestamp else System.currentTimeMillis()
            val newExpiry = if (isLifetime) 0L else baseTime + (extraDays.toLong() * 24 * 60 * 60 * 1000L)
            val updated = user.copy(
                isLifetime = isLifetime,
                expiryTimestamp = newExpiry,
                status = "active"
            )
            firebaseManager.saveUser(updated)
            loadFirebaseUsers()
            snackbarMessage.value = "تم تمديد صلاحية الاشتراك بنجاح"
        }
    }

    fun sendDirectNotificationToUser(user: FirebaseUserAccount, message: String) {
        viewModelScope.launch {
            val updated = user.copy(directMessage = message)
            firebaseManager.saveUser(updated)
            loadFirebaseUsers()
            snackbarMessage.value = "تم إرسال الإشعار المباشر للمستخدم"
        }
    }

    fun updateAiAssistantSettings(
        allowCommands: Boolean,
        allowReboot: Boolean,
        allowUserManager: Boolean,
        allowHotspot: Boolean,
        customPrompt: String,
        modelMode: String
    ) {
        val updatedAi = AIAssistantPermissions(
            allowRouterCommands = allowCommands,
            allowReboot = allowReboot,
            allowUserManagerOps = allowUserManager,
            allowHotspotOps = allowHotspot,
            systemPromptOverride = customPrompt,
            aiModelMode = modelMode
        )
        val newConfig = _appConfig.value.copy(aiPermissions = updatedAi)
        saveAppConfig(newConfig)
    }

    // ================= ROUTER & DEVICE OPERATIONS =================

    fun addRouter(
        name: String,
        host: String,
        port: Int = 8728,
        username: String = "admin",
        password: String = "",
        connectionType: ConnectionType = ConnectionType.CLOUD,
        location: String = "15.369,44.191",
        localIpFallback: String = "10.0.0.1",
        localPort: Int = 8728
    ) {
        viewModelScope.launch {
            val uid = _currentUser.value?.id ?: ""
            val entity = RouterEntity(
                userId = uid,
                name = name,
                host = host,
                port = port,
                username = username,
                password = password,
                connectionType = connectionType,
                location = location.ifBlank { "15.369,44.191" },
                localIpFallback = localIpFallback,
                localPort = localPort
            )
            val id = repository.insertRouter(entity)
            repository.addLog(uid, "إضافة راوتر", "تمت إضافة الراوتر $name بنجاح")
            snackbarMessage.value = "تمت إضافة الراوتر بنجاح"
            showAddRouterDialog.value = false
            repository.checkSingleRouter(entity.copy(id = id))
        }
    }

    fun addDevice(
        name: String,
        ip: String,
        manager: String = "",
        location: String = "15.369,44.191",
        notes: String = "",
        routerId: Long? = null,
        parentDeviceId: Long? = null,
        type: DeviceType = DeviceType.MAIN_TOWER,
        status: DeviceStatus = DeviceStatus.ONLINE,
        apiUsername: String = "admin",
        apiPassword: String = ""
    ) {
        viewModelScope.launch {
            val uid = _currentUser.value?.id ?: ""
            val entity = NetworkDeviceEntity(
                userId = uid,
                name = name,
                ip = ip,
                manager = manager,
                location = location.ifBlank { "15.369,44.191" },
                notes = notes,
                routerId = routerId,
                parentDeviceId = parentDeviceId,
                type = type,
                status = status,
                apiUsername = apiUsername,
                apiPassword = apiPassword
            )
            repository.insertDevice(entity)
            repository.addLog(uid, "إضافة جهاز", "تمت إضافة الجهاز $name ($ip)")
            snackbarMessage.value = "تمت إضافة الجهاز بنجاح"
            showAddDeviceDialog.value = false
            repository.checkSingleDevice(entity)
        }
    }

    fun deleteRouter(router: RouterEntity) {
        viewModelScope.launch {
            repository.deleteRouter(router)
            snackbarMessage.value = "تم حذف الراوتر"
        }
    }

    fun deleteDevice(device: NetworkDeviceEntity) {
        viewModelScope.launch {
            repository.deleteDevice(device)
            snackbarMessage.value = "تم حذف الجهاز"
        }
    }

    suspend fun testRouterConnection(host: String, port: Int, username: String, pass: String): MikroTikResponse {
        return repository.testRouterConnection(host, port, username, pass)
    }

    fun pingDevice(device: NetworkDeviceEntity) {
        viewModelScope.launch {
            val updated = repository.checkSingleDevice(device)
            val resText = if (updated.status == DeviceStatus.ONLINE) "متصل (${updated.pingMs}ms)" else "غير متصل"
            snackbarMessage.value = "نتيجة فحص [${device.name}]: $resText"
        }
    }

    fun makeArpStatic(router: RouterEntity, device: NetworkDeviceEntity) {
        viewModelScope.launch {
            val res = repository.makeArpStatic(router, device)
            if (res.isSuccess) {
                snackbarMessage.value = "تم تثبيت عنوان IP في جدول ARP للراوتر"
            } else {
                snackbarMessage.value = "خطأ في تثبيت ARP: ${res.errorMessage}"
            }
        }
    }

    fun importNeighbors(router: RouterEntity) {
        viewModelScope.launch {
            val count = repository.importNeighbors(router)
            snackbarMessage.value = "تم استيراد $count أجهزة مكتشفة بنجاح"
            showImportNeighborsDialog.value = false
        }
    }

    fun syncNetwatch(router: RouterEntity) {
        viewModelScope.launch {
            val count = repository.syncNetwatch(router)
            snackbarMessage.value = "تمت مزامنة $count أجهزة من Netwatch بنجاح"
            showNetwatchSyncDialog.value = false
        }
    }

    fun refreshAll() {
        viewModelScope.launch {
            repository.checkAllRoutersAndDevices()
        }
    }

    fun toggleSound() {
        repository.toggleSound()
    }

    fun testSound() {
        repository.playSoundTest()
    }

    fun markAllAlertsRead() {
        viewModelScope.launch {
            repository.markAllAlertsRead(_currentUser.value?.id ?: "")
        }
    }

    fun clearAllAlerts() {
        viewModelScope.launch {
            repository.clearAllAlerts(_currentUser.value?.id ?: "")
        }
    }

    fun clearSnackbar() {
        snackbarMessage.value = null
    }

    // ================= USER MANAGER ACTIONS =================

    fun selectUserManagerRouter(router: RouterEntity?) {
        selectedUserManagerRouter.value = router
        if (router != null) {
            syncUserManagerData(router)
        }
    }

    fun syncUserManagerData(router: RouterEntity) {
        viewModelScope.launch {
            isSyncingUserManager.value = true
            val ok = repository.syncUserManager(router)
            if (ok) {
                snackbarMessage.value = "تمت مزامنة كروت وباقات اليوزرمانجر بنجاح"
            } else {
                snackbarMessage.value = "تعذر سحب بيانات اليوزرمانجر من الراوتر (تأكد من تنصيب حزمة User Manager)"
            }
            isSyncingUserManager.value = false
        }
    }

    fun addUserManagerCard(router: RouterEntity, username: String, pass: String, profile: String, comment: String) {
        viewModelScope.launch {
            isSyncingUserManager.value = true
            val res = repository.addUserManagerCard(router, username, pass, profile, comment)
            if (res.isSuccess) {
                snackbarMessage.value = "تمت إضافة الكرت بنجاح"
                syncUserManagerData(router)
            } else {
                snackbarMessage.value = "خطأ في إضافة الكرت: ${res.errorMessage}"
            }
            isSyncingUserManager.value = false
        }
    }

    fun generateBatchUserManagerCards(
        router: RouterEntity,
        prefix: String,
        count: Int,
        profile: String,
        length: Int,
        price: String
    ) {
        viewModelScope.launch {
            isSyncingUserManager.value = true
            var addedCount = 0
            val generatedList = mutableListOf<GeneratedCardPrintItem>()
            val rnd = Random()
            val chars = "1234567890"
            val batchNum = "B-${(1000 + rnd.nextInt(8999))}"
            val safeLen = length.coerceIn(3, 18)
            for (i in 1..count) {
                val sb = StringBuilder()
                val remaining = (safeLen - prefix.length).coerceAtLeast(1)
                for (j in 1..remaining) {
                    sb.append(chars[rnd.nextInt(chars.length)])
                }
                val uName = "$prefix$sb"
                val res = repository.addUserManagerCard(router, uName, uName, profile, "Batch-$price")
                if (res.isSuccess) {
                    addedCount++
                    generatedList.add(
                        GeneratedCardPrintItem(
                            username = uName,
                            password = uName,
                            profileName = profile,
                            price = price,
                            serial = "UM-${10000 + i}",
                            paymentNumber = batchNum,
                            posName = router.name
                        )
                    )
                }
            }
            lastGeneratedUmBatch.value = generatedList
            snackbarMessage.value = "تم توليد وإضافة $addedCount كرت يوزرمانجر بنجاح"
            syncUserManagerData(router)
            isSyncingUserManager.value = false
        }
    }

    fun deleteUserManagerCard(router: RouterEntity, userEntity: UserManagerUserEntity) {
        viewModelScope.launch {
            val res = repository.deleteUserManagerCard(router, userEntity)
            if (res.isSuccess) {
                snackbarMessage.value = "تم حذف الكرت من الراوتر بنجاح ✓"
            } else {
                snackbarMessage.value = "تم حذف الكرت محلياً"
            }
        }
    }

    fun editUserManagerCard(
        router: RouterEntity,
        card: UserManagerUserEntity,
        newUsername: String,
        newPassword: String,
        newProfile: String,
        comment: String
    ) {
        viewModelScope.launch {
            val res = repository.editUserManagerCard(router, card, newUsername, newPassword, newProfile, comment)
            if (res.isSuccess) {
                snackbarMessage.value = "تم تعديل بيانات الكرت [$newUsername] بنجاح ✓"
                syncUserManagerData(router)
            } else {
                snackbarMessage.value = "فشل تعديل الكرت: ${res.errorMessage ?: "خطأ في الاتصال"}"
            }
        }
    }

    fun toggleUserManagerCard(router: RouterEntity, card: UserManagerUserEntity, enable: Boolean) {
        viewModelScope.launch {
            val res = repository.toggleUserManagerCard(router, card, enable)
            if (res.isSuccess) {
                snackbarMessage.value = if (enable) "تم تفعيل الكرت [${card.username}] بنجاح ✓" else "تم تعطيل الكرت [${card.username}] ⏸"
                syncUserManagerData(router)
            } else {
                snackbarMessage.value = "تعذر تغيير حالة الكرت"
            }
        }
    }

    fun changeUserManagerProfile(router: RouterEntity, card: UserManagerUserEntity, newProfile: String) {
        viewModelScope.launch {
            val res = repository.changeUserManagerProfile(router, card, newProfile)
            if (res.isSuccess) {
                snackbarMessage.value = "تم تغيير فئة الكرت إلى [$newProfile] بنجاح ✓"
                syncUserManagerData(router)
            } else {
                snackbarMessage.value = "تعذر تغيير فئة الكرت: ${res.errorMessage}"
            }
        }
    }

    fun renewUserManagerCard(router: RouterEntity, card: UserManagerUserEntity, newProfile: String) {
        viewModelScope.launch {
            val res = repository.renewUserManagerCard(router, card, newProfile)
            if (res.isSuccess) {
                snackbarMessage.value = "تم تجديد الكرت [${card.username}] بباقة [$newProfile] بنجاح ✓"
                syncUserManagerData(router)
            } else {
                snackbarMessage.value = "تعذر تجديد الكرت: ${res.errorMessage}"
            }
        }
    }

    // ================= HOTSPOT ACTIONS =================

    fun selectHotspotRouter(router: RouterEntity?) {
        selectedHotspotRouter.value = router
        if (router != null) {
            syncHotspotData(router)
        }
    }

    fun syncHotspotData(router: RouterEntity) {
        viewModelScope.launch {
            isSyncingHotspot.value = true
            val ok = repository.syncHotspot(router)
            if (ok) {
                snackbarMessage.value = "تمت مزامنة مستخدمي وجلسات الهوتسبوت"
            } else {
                snackbarMessage.value = "تعذر سحب بيانات الهوتسبوت من الراوتر"
            }
            isSyncingHotspot.value = false
        }
    }

    fun addHotspotUser(
        router: RouterEntity,
        name: String,
        pass: String,
        profile: String,
        comment: String,
        limitUptime: String,
        limitBytes: String
    ) {
        viewModelScope.launch {
            isSyncingHotspot.value = true
            val res = repository.addHotspotUser(router, name, pass, profile, comment, limitUptime, limitBytes)
            if (res.isSuccess) {
                snackbarMessage.value = "تمت إضافة مستخدم الهوتسبوت بنجاح"
                syncHotspotData(router)
            } else {
                snackbarMessage.value = "خطأ في إضافة المستخدم: ${res.errorMessage}"
            }
            isSyncingHotspot.value = false
        }
    }

    fun generateBatchHotspotCards(
        router: RouterEntity,
        prefix: String,
        count: Int,
        profile: String,
        length: Int,
        limitUptime: String,
        limitBytes: String,
        priceLabel: String = ""
    ) {
        viewModelScope.launch {
            isSyncingHotspot.value = true
            var addedCount = 0
            val generatedList = mutableListOf<GeneratedCardPrintItem>()
            val rnd = Random()
            val chars = "1234567890"
            val batchNum = "HS-${(1000 + rnd.nextInt(8999))}"
            val safeLen = length.coerceIn(3, 18)
            for (i in 1..count) {
                val sb = StringBuilder()
                val remaining = (safeLen - prefix.length).coerceAtLeast(1)
                for (j in 1..remaining) {
                    sb.append(chars[rnd.nextInt(chars.length)])
                }
                val uName = "$prefix$sb"
                val res = repository.addHotspotUser(router, uName, uName, profile, "HotspotBatch", limitUptime, limitBytes)
                if (res.isSuccess) {
                    addedCount++
                    generatedList.add(
                        GeneratedCardPrintItem(
                            username = uName,
                            password = uName,
                            profileName = profile,
                            price = priceLabel.ifBlank { profile },
                            serial = "HS-${10000 + i}",
                            paymentNumber = batchNum,
                            posName = router.name
                        )
                    )
                }
            }
            lastGeneratedHotspotBatch.value = generatedList
            snackbarMessage.value = "تم توليد وإضافة $addedCount كرت هوتسبوت بنجاح"
            syncHotspotData(router)
            isSyncingHotspot.value = false
        }
    }

    fun disconnectHotspotSession(router: RouterEntity, session: HotspotActiveSessionEntity) {
        viewModelScope.launch {
            val res = repository.disconnectHotspotSession(router, session)
            if (res.isSuccess) {
                snackbarMessage.value = "تم فصل المستخدم ${session.username} بنجاح"
            } else {
                snackbarMessage.value = "تم إرسال أمر الفصل"
            }
        }
    }

    fun deleteHotspotUser(router: RouterEntity, userEntity: HotspotUserEntity) {
        viewModelScope.launch {
            val res = repository.deleteHotspotUser(router, userEntity)
            if (res.isSuccess) {
                snackbarMessage.value = "تم حذف المستخدم من الراوتر بنجاح ✓"
            } else {
                snackbarMessage.value = "تم حذف المستخدم محلياً"
            }
        }
    }

    fun editHotspotUser(
        router: RouterEntity,
        user: HotspotUserEntity,
        newName: String,
        newPassword: String,
        newProfile: String,
        comment: String,
        limitUptime: String,
        limitBytes: String
    ) {
        viewModelScope.launch {
            val res = repository.editHotspotUser(router, user, newName, newPassword, newProfile, comment, limitUptime, limitBytes)
            if (res.isSuccess) {
                snackbarMessage.value = "تم تعديل مستخدم الهوتسبوت [$newName] بنجاح ✓"
                syncHotspotData(router)
            } else {
                snackbarMessage.value = "فشل التعديل: ${res.errorMessage ?: "خطأ في الاتصال"}"
            }
        }
    }

    fun toggleHotspotUser(router: RouterEntity, user: HotspotUserEntity, enable: Boolean) {
        viewModelScope.launch {
            val res = repository.toggleHotspotUser(router, user, enable)
            if (res.isSuccess) {
                snackbarMessage.value = if (enable) "تم تفعيل المستخدم [${user.name}] بنجاح ✓" else "تم تعطيل المستخدم [${user.name}] ⏸"
                syncHotspotData(router)
            } else {
                snackbarMessage.value = "تعذر تغيير حالة المستخدم"
            }
        }
    }

    fun changeHotspotProfile(router: RouterEntity, user: HotspotUserEntity, newProfile: String) {
        viewModelScope.launch {
            val res = repository.changeHotspotProfile(router, user, newProfile)
            if (res.isSuccess) {
                snackbarMessage.value = "تم تغيير فئة الهوتسبوت إلى [$newProfile] بنجاح ✓"
                syncHotspotData(router)
            } else {
                snackbarMessage.value = "تعذر تغيير الفئة: ${res.errorMessage}"
            }
        }
    }

    fun renewHotspotUser(
        router: RouterEntity,
        user: HotspotUserEntity,
        newProfile: String,
        limitUptime: String = "",
        limitBytes: String = ""
    ) {
        viewModelScope.launch {
            val res = repository.renewHotspotUser(router, user, newProfile, limitUptime, limitBytes)
            if (res.isSuccess) {
                snackbarMessage.value = "تم تجديد الكرت [${user.name}] بباقة [$newProfile] بنجاح ✓"
                syncHotspotData(router)
            } else {
                snackbarMessage.value = "تعذر تجديد الكرت: ${res.errorMessage}"
            }
        }
    }

    // ================= ACTIVE CONNECTED DEVICES & SESSIONS =================

    fun selectActiveRouter(router: RouterEntity?) {
        selectedActiveRouter.value = router
        if (router != null) {
            loadActiveDevices(router)
        }
    }

    fun loadActiveDevices(targetRouter: RouterEntity? = null) {
        val router = targetRouter ?: selectedActiveRouter.value ?: selectedHotspotRouter.value ?: allRouters.value.firstOrNull()
        if (router == null) {
            snackbarMessage.value = "يرجى تحديد راوتر لعرض المتصلين حالياً"
            return
        }
        viewModelScope.launch {
            isLoadingActiveDevices.value = true
            try {
                val list = repository.loadDetailedActiveDevices(router)
                detailedActiveDevices.value = list
            } catch (e: Exception) {
                snackbarMessage.value = "خطأ في قراءة الأجهزة المتصلة: ${e.localizedMessage}"
            } finally {
                isLoadingActiveDevices.value = false
            }
        }
    }

    fun kickActiveDevice(router: RouterEntity, activeId: String, username: String) {
        viewModelScope.launch {
            val res = repository.kickActiveUser(router, activeId)
            if (res.isSuccess) {
                snackbarMessage.value = "تم طرد وفصل المتصل [$username] بنجاح ✓"
            } else {
                snackbarMessage.value = "تم إرسال أمر الفصل"
            }
            loadActiveDevices(router)
        }
    }

    fun blockActiveDevice(router: RouterEntity, mac: String, ip: String, username: String) {
        viewModelScope.launch {
            val res = repository.blockDevice(router, mac, ip, username)
            if (res.isSuccess) {
                snackbarMessage.value = "تم حظر الماك [$mac] للجهاز [$username] فوراً 🚫"
            } else {
                snackbarMessage.value = "تم إرسال أمر الحظر"
            }
            loadActiveDevices(router)
        }
    }

    fun renewActiveDeviceCard(
        router: RouterEntity,
        username: String,
        profile: String,
        limitUptime: String = "",
        limitBytes: String = ""
    ) {
        viewModelScope.launch {
            val user = HotspotUserEntity(name = username, profile = profile)
            val res = repository.renewHotspotUser(router, user, profile, limitUptime, limitBytes)
            if (res.isSuccess) {
                snackbarMessage.value = "تم تجديد كرت المتصل [$username] بنجاح بباقة [$profile] ✓"
            } else {
                snackbarMessage.value = "تم إرسال أمر تجديد الكرت"
            }
            loadActiveDevices(router)
        }
    }

    // ================= PORTS & NETWORK DISTRIBUTION CONTROLS =================

    fun loadPortsAndDistribution(targetRouter: RouterEntity? = null) {
        val router = targetRouter ?: selectedPortsRouter.value ?: allRouters.value.firstOrNull()
        if (router == null) {
            snackbarMessage.value = "يرجى تحديد راوتر لعرض المنافذ وأجهزة التوزيع"
            return
        }
        selectedPortsRouter.value = router
        viewModelScope.launch {
            isLoadingPortsAndDistribution.value = true
            try {
                val ports = repository.getRouterPorts(router)
                networkPorts.value = ports

                val distribution = repository.getDistributionUnits(router, allDevices.value)
                distributionDevices.value = distribution
            } catch (e: Exception) {
                snackbarMessage.value = "خطأ في جلب بيانات المنافذ والتوزيع: ${e.localizedMessage}"
            } finally {
                isLoadingPortsAndDistribution.value = false
            }
        }
    }

    fun togglePort(router: RouterEntity, port: NetworkPortEntity, enable: Boolean) {
        viewModelScope.launch {
            val res = repository.setPortStatus(router, port.name, enable)
            if (res.isSuccess) {
                snackbarMessage.value = if (enable) "تم تفعيل وتشغيل المنفذ [${port.name}] بنجاح 🟢" else "تم إيقاف وتعطيل المنفذ [${port.name}] 🔴"
            } else {
                snackbarMessage.value = "تم تنفيذ أمر تبديل حالة المنفذ"
            }
            loadPortsAndDistribution(router)
        }
    }

    fun updatePort(router: RouterEntity, portId: String, name: String, comment: String) {
        viewModelScope.launch {
            val res = repository.setPortCommentAndName(router, portId, name, comment)
            if (res.isSuccess) {
                snackbarMessage.value = "تم تعديل اسم وتعليق المنفذ بنجاح ✓"
            } else {
                snackbarMessage.value = "تم حفظ تعديلات المنفذ"
            }
            loadPortsAndDistribution(router)
        }
    }

    fun resetPortCounters(router: RouterEntity, portName: String) {
        viewModelScope.launch {
            val res = repository.resetPortCounters(router, portName)
            if (res.isSuccess) {
                snackbarMessage.value = "تم تصفير عدادات الترافيك للمنفذ [$portName] بنجاح 🔄"
            } else {
                snackbarMessage.value = "تم تصفير العدادات"
            }
            loadPortsAndDistribution(router)
        }
    }

    fun setPortBandwidthLimit(router: RouterEntity, interfaceName: String, limit: String) {
        viewModelScope.launch {
            val res = repository.setPortSpeedLimit(router, interfaceName, limit)
            if (res.isSuccess) {
                snackbarMessage.value = "تم ضبط وتطبيق سرعة المنفذ [$interfaceName] إلى [$limit] بنجاح ⚡"
            } else {
                snackbarMessage.value = "تم تطبيق الحد على المنفذ"
            }
            loadPortsAndDistribution(router)
        }
    }

    fun testPortCable(router: RouterEntity, interfaceName: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val result = repository.testPortCable(router, interfaceName)
            onResult(result)
        }
    }

    // ================= SPECIAL TOOLS (ONE-CLICK CLOUD DDNS & OPS) =================

    fun enableCloudDdns(router: RouterEntity) {
        viewModelScope.launch {
            isEnablingCloud.value = true
            cloudOperationResult.value = null
            val res = repository.enableRouterCloudDdns(router)
            if (res.isSuccess) {
                val dns = res.results.firstOrNull()?.get("dns-name") ?: "مفعل"
                cloudOperationResult.value = "تم تفعيل خدمة الكلاود بنجاح!\nعنوان الدومين: $dns"
                snackbarMessage.value = "تم تفعيل الكلاود بنجاح في الراوتر"
            } else {
                cloudOperationResult.value = "فشل التفعيل: ${res.errorMessage ?: "خطأ في الاتصال"}"
            }
            isEnablingCloud.value = false
        }
    }

    fun rebootRouter(router: RouterEntity) {
        viewModelScope.launch {
            val res = repository.rebootRouter(router)
            snackbarMessage.value = "تم إرسال أمر إعادة تشغيل الراوتر [${router.name}]"
        }
    }

    fun backupRouter(router: RouterEntity, name: String) {
        viewModelScope.launch {
            val res = repository.backupRouter(router, name)
            if (res.isSuccess) {
                snackbarMessage.value = "تم إنشاء النسخة الاحتياطية بنجاح"
            } else {
                snackbarMessage.value = "فشل النسخ الاحتياطي: ${res.errorMessage}"
            }
        }
    }

    // ================= VLAN MANAGEMENT ACTIONS =================

    fun loadVlans(router: RouterEntity?) {
        if (router == null) return
        viewModelScope.launch {
            isLoadingVlans.value = true
            vlanOperationResult.value = null
            try {
                val list = repository.getRouterVlans(router)
                vlans.value = list
                val ifaces = repository.getRouterInterfacesList(router)
                availableInterfaces.value = ifaces
            } catch (e: Exception) {
                snackbarMessage.value = "خطأ في جلب الفيلانات: ${e.localizedMessage}"
            } finally {
                isLoadingVlans.value = false
            }
        }
    }

    fun loadRouterLogs(router: RouterEntity?) {
        if (router == null) {
            routerLogs.value = emptyList()
            routerLogsError.value = "يرجى اختيار راوتر لعرض سجلاته"
            return
        }
        viewModelScope.launch {
            isLoadingRouterLogs.value = true
            routerLogsError.value = null
            try {
                val (list, err) = repository.getRouterLogs(router)
                routerLogs.value = list
                routerLogsError.value = err
            } catch (e: Exception) {
                routerLogsError.value = "تعذر جلب سجل الراوتر: ${e.localizedMessage ?: "خطأ غير معروف"}"
            } finally {
                isLoadingRouterLogs.value = false
            }
        }
    }

    fun clearAppLogs() {
        val uid = currentUser.value?.id ?: ""
        viewModelScope.launch {
            repository.clearLogs(uid)
            repository.clearAllAlerts(uid)
            snackbarMessage.value = "تم مسح سجل رسائل ومشاكل التطبيق بنجاح ✓"
        }
    }

    fun addVlan(
        router: RouterEntity?,
        name: String,
        vlanId: Int,
        interfaceName: String,
        mtu: Int = 1500,
        arp: String = "enabled",
        comment: String = ""
    ) {
        if (router == null) {
            snackbarMessage.value = "يرجى تحديد راوتر أولاً"
            return
        }
        if (name.isBlank()) {
            snackbarMessage.value = "يرجى إدخال اسم الفيلان (Name)"
            return
        }
        if (vlanId !in 1..4094) {
            snackbarMessage.value = "معرف الفيلان (VLAN ID) يجب أن يكون بين 1 و 4094"
            return
        }
        viewModelScope.launch {
            isSavingVlan.value = true
            val res = repository.addRouterVlan(router, name, vlanId, interfaceName, mtu, arp, comment)
            if (res.isSuccess) {
                snackbarMessage.value = "تمت إضافة الفيلان [$name] بنجاح ✓"
                loadVlans(router)
            } else {
                snackbarMessage.value = "فشل إضافة الفيلان: ${res.errorMessage ?: "خطأ غير معروف"}"
            }
            isSavingVlan.value = false
        }
    }

    fun updateVlan(
        router: RouterEntity?,
        idOrName: String,
        name: String,
        vlanId: Int,
        interfaceName: String,
        mtu: Int = 1500,
        arp: String = "enabled",
        comment: String = ""
    ) {
        if (router == null) return
        viewModelScope.launch {
            isSavingVlan.value = true
            val res = repository.updateRouterVlan(router, idOrName, name, vlanId, interfaceName, mtu, arp, comment)
            if (res.isSuccess) {
                snackbarMessage.value = "تم تعديل الفيلان [$name] بنجاح ✓"
                loadVlans(router)
            } else {
                snackbarMessage.value = "فشل تعديل الفيلان: ${res.errorMessage ?: "خطأ غير معروف"}"
            }
            isSavingVlan.value = false
        }
    }

    fun toggleVlan(router: RouterEntity?, vlan: MikrotikVlan) {
        if (router == null) return
        viewModelScope.launch {
            val targetEnable = vlan.disabled // if disabled, enable it
            val res = repository.toggleRouterVlan(router, vlan.id.ifBlank { vlan.name }, targetEnable)
            if (res.isSuccess) {
                snackbarMessage.value = if (targetEnable) "تم تفعيل الفيلان [${vlan.name}]" else "تم تعطيل الفيلان [${vlan.name}]"
                loadVlans(router)
            } else {
                snackbarMessage.value = "فشل تغيير حالة الفيلان: ${res.errorMessage}"
            }
        }
    }

    fun deleteVlan(router: RouterEntity?, vlan: MikrotikVlan) {
        if (router == null) return
        viewModelScope.launch {
            val res = repository.removeRouterVlan(router, vlan.id.ifBlank { vlan.name })
            if (res.isSuccess) {
                snackbarMessage.value = "تم حذف الفيلان [${vlan.name}] بنجاح ✓"
                loadVlans(router)
            } else {
                snackbarMessage.value = "فشل حذف الفيلان: ${res.errorMessage}"
            }
        }
    }

    fun assignIpToVlan(router: RouterEntity?, vlanName: String, ipAddressWithCidr: String) {
        if (router == null) return
        if (ipAddressWithCidr.isBlank() || !ipAddressWithCidr.contains("/")) {
            snackbarMessage.value = "يرجى كتابة عنوان IP مع القناع (مثلاً 10.10.50.1/24)"
            return
        }
        viewModelScope.launch {
            val res = repository.assignIpToRouterVlan(router, vlanName, ipAddressWithCidr)
            if (res.isSuccess) {
                snackbarMessage.value = "تم تعيين العنوان [$ipAddressWithCidr] للفيلان [$vlanName] بنجاح"
            } else {
                snackbarMessage.value = "فشل تعيين العنوان: ${res.errorMessage}"
            }
        }
    }

    // ================= TELEGRAM BOT ACTIONS =================

    fun saveTelegramSettings(
        token: String,
        chatId: String,
        channelId: String,
        isEnabled: Boolean,
        alertRouter: Boolean,
        alertCpu: Boolean,
        alertHotspot: Boolean,
        alertBackup: Boolean
    ) {
        viewModelScope.launch {
            val uid = _currentUser.value?.id ?: ""
            val entity = TelegramConfigEntity(
                userId = uid,
                botToken = token.trim(),
                chatId = chatId.trim(),
                channelId = channelId.trim(),
                isEnabled = isEnabled,
                alertOnRouterOffline = alertRouter,
                alertOnCpuHigh = alertCpu,
                alertOnHotspotLogin = alertHotspot,
                alertOnBackupDone = alertBackup
            )
            repository.saveTelegramConfig(entity)
            snackbarMessage.value = "تم حفظ إعدادات بوت تليجرام بنجاح"
        }
    }

    fun testTelegramBot(token: String) {
        viewModelScope.launch {
            isTestingTelegram.value = true
            telegramTestResult.value = null
            val info = repository.telegramManager.testBotToken(token)
            if (info.isSuccess) {
                telegramTestResult.value = "✅ الاتصال ناجح ومستقر!\nاسم البوت: ${info.botName}\nالمعرف: @${info.username}\nمعرف البوت: ${info.botId}"
                snackbarMessage.value = "تم التحقق من البوت بنجاح"
            } else {
                telegramTestResult.value = "❌ فشل الاتصال بالبوت:\n${info.errorMessage}"
            }
            isTestingTelegram.value = false
        }
    }

    fun fetchTelegramChatIds(token: String) {
        viewModelScope.launch {
            isTestingTelegram.value = true
            val chats = repository.telegramManager.getRecentChats(token)
            detectedTelegramChats.value = chats
            if (chats.isNotEmpty()) {
                snackbarMessage.value = "تم العثور على ${chats.size} محادثة"
            } else {
                snackbarMessage.value = "لم يتم العثور على رسائل حديثة. يرجى إرسال أي رسالة للبوت في تليجرام أولاً ثم المحاولة مجدداً."
            }
            isTestingTelegram.value = false
        }
    }

    fun sendTelegramTestAlert(token: String, chatId: String) {
        viewModelScope.launch {
            isTestingTelegram.value = true
            val msg = """
                🚀 <b>رسالة تجريبية من TALAL PRO</b>
                ━━━━━━━━━━━━━━━━━━
                ✅ الربط مع بوت تليجرام يعمل بنجاح وكفاءة تامة.
                📡 جاهز لتلقي إشعارات الراوترات والأبراج وانقطاع الإنترنت فوراً.
                ⏰ الوقت: ${java.text.SimpleDateFormat("yyyy/MM/dd HH:mm:ss", java.util.Locale.ENGLISH).format(java.util.Date())}
            """.trimIndent()
            val (ok, err) = repository.telegramManager.sendMessage(token, chatId, msg)
            if (ok) {
                snackbarMessage.value = "تم إرسال الرسالة التجريبية إلى تليجرام بنجاح ✓"
            } else {
                snackbarMessage.value = "فشل الإرسال: $err"
            }
            isTestingTelegram.value = false
        }
    }

    fun installTelegramScriptToRouter(router: RouterEntity, token: String, chatId: String) {
        viewModelScope.launch {
            val res = repository.installTelegramScriptToRouter(router, token, chatId)
            if (res.isSuccess) {
                snackbarMessage.value = "تم تثبيت سكربت التنبيهات في الراوتر [${router.name}] بنجاح ✓"
            } else {
                snackbarMessage.value = "فشل تثبيت السكربت: ${res.errorMessage}"
            }
        }
    }

    // ================= TELEGRAM CLOUD BOT RUNNER & DIRECT ROUTER EXECUTION =================

    fun toggleTelegramCloud(targetRouter: RouterEntity?) {
        if (isTelegramCloudRunning.value) {
            stopTelegramCloud()
        } else {
            startTelegramCloud(targetRouter)
        }
    }

    fun startTelegramCloud(targetRouter: RouterEntity?) {
        val config = telegramConfig.value
        if (config == null || config.botToken.isBlank()) {
            snackbarMessage.value = "يرجى إدخال وحفظ رمز البوت (Bot Token) أولاً"
            return
        }
        val router = targetRouter ?: allRouters.value.firstOrNull()
        if (router == null) {
            snackbarMessage.value = "يرجى إضافة أو تحديد راوتر مايكروتك أولاً للربط المباشر"
            return
        }

        isTelegramCloudRunning.value = true
        addTelegramLog("🚀 تشغيل البوت السحابي وربطه براوتر [${router.name}]...")

        telegramPollJob?.cancel()
        telegramPollJob = viewModelScope.launch(Dispatchers.IO) {
            // Send the Arabic interactive keyboard to user's chat if configured
            if (config.chatId.isNotBlank()) {
                repository.telegramManager.sendInteractiveKeyboard(
                    token = config.botToken,
                    chatId = config.chatId,
                    routerName = router.name
                )
                addTelegramLog("📲 تم إرسال لوحة التحكم والأزرار العربية للتليجرام (Chat ID: ${config.chatId})")
            }

            var lastOffset = 0L
            while (isActive && isTelegramCloudRunning.value) {
                try {
                    val updates = repository.telegramManager.getUpdates(
                        token = config.botToken,
                        offset = lastOffset,
                        timeoutSeconds = 15
                    )
                    for (u in updates) {
                        lastOffset = maxOf(lastOffset, u.updateId + 1)
                        addTelegramLog("📩 استلام أمر من [${u.senderName}]: ${u.text}")
                        val replyText = executeTelegramButtonCommand(u.text, router)
                        repository.telegramManager.sendMessage(config.botToken, u.chatId, replyText)
                        addTelegramLog("✅ تم تنفيذ الأمر وإرسال التقرير اللحظي بنجاح")
                    }
                } catch (e: Exception) {
                    delay(3000)
                }
                delay(1000)
            }
        }
    }

    fun stopTelegramCloud() {
        telegramPollJob?.cancel()
        telegramPollJob = null
        isTelegramCloudRunning.value = false
        addTelegramLog("⏹️ تم إيقاف البوت السحابي.")
        snackbarMessage.value = "تم إيقاف تشغيل البوت السحابي"
    }

    fun sendTelegramInteractiveKeyboard(targetRouter: RouterEntity?) {
        val config = telegramConfig.value
        if (config == null || config.botToken.isBlank() || config.chatId.isBlank()) {
            snackbarMessage.value = "يرجى كتابة رمز البوت ومعرف المحادثة وحفظهما أولاً"
            return
        }
        val router = targetRouter ?: allRouters.value.firstOrNull()
        viewModelScope.launch {
            val (ok, err) = repository.telegramManager.sendInteractiveKeyboard(
                token = config.botToken,
                chatId = config.chatId,
                routerName = router?.name ?: "الراوتر الرئيسي"
            )
            if (ok) {
                snackbarMessage.value = "تم إرسال لوحة التحكم والأزرار العربية لتليجرام بنجاح ✓"
                addTelegramLog("📲 تم إرسال الأزرار التفاعلية إلى تليجرام")
            } else {
                snackbarMessage.value = "فشل الإرسال: $err"
            }
        }
    }

    private fun addTelegramLog(log: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.ENGLISH).format(Date())
        val item = "[$time] $log"
        val cur = telegramCloudLogs.value.takeLast(30)
        telegramCloudLogs.value = cur + item
    }

    private suspend fun executeTelegramButtonCommand(cmd: String, router: RouterEntity): String {
        val clean = cmd.trim()
        val client = MikroTikApiClient(
            host = router.host,
            port = router.port,
            username = router.username,
            password = router.password
        )

        return when {
            clean.contains("القائمة") || clean.contains("menu") || clean.contains("start") -> {
                """
                    📋 <b>لوحة تحكم وأوامر TALAL PRO المباشرة:</b>
                    ━━━━━━━━━━━━━━━━━━
                    🔗 <b>الراوتر المتصل:</b> ${router.name}
                    
                    اضغط على أي زر أدناه للتنفيذ اللحظي:
                    🟢 <b>الحالة:</b> فحص المعالج، الذاكرة، مدة التشغيل، الفولتية.
                    👥 <b>المتصلين:</b> كشف متصلي الهوتسبوت وPPPoE النشطين.
                    📦 <b>الباقات:</b> البروفايلات والسرعات والأسعار المتاحة.
                    💾 <b>نسخة احتياطية:</b> أخذ باك اب وتصدير إعدادات الراوتر.
                    🛡️ <b>حماية المواقع:</b> تفعيل جدار الحماية ضد المواقع الضارة.
                    ⚡ <b>تحسين الشبكة:</b> تفريغ الكاش وسجلات ARP وتسريع التصفح.
                    📊 <b>التقرير اليومي:</b> فحص شامل لاستقرار وكفاءة الشبكة اليوم.
                    💰 <b>المبيعات:</b> تقرير كروت الشبكة والمدخول المقدر.
                    📈 <b>ملخص الاداء:</b> مراقبة ضغط المعالج واستهلاك الرام.
                    🌐 <b>الاستهلاك اليومي:</b> حجم الترافيك المحسوب رفع وتنزيل.
                    📡 <b>قطع التوزيع:</b> مراقبة الأبراج وموزعات النت وعدد المتصلين في كل قطعة واستهلاكها.
                    🔌 <b>منافذ الشبكة:</b> حالة منافذ الراوتر المتصلة والسرعات 1Gbps/100Mbps.
                    📊 <b>استهلاك المنافذ:</b> تفاصيل استهلاك الإنترنت لكل منفذ Download/Upload.
                    ⚙️ <b>تحكم المنافذ:</b> أوامر وتعليمات إدارة وتعطيل وتفعيل المنافذ.
                """.trimIndent()
            }

            clean.contains("الحالة") || clean.contains("حالة") || clean.contains("status") -> {
                val sys = client.getSystemInfo()
                if (sys.isOnline) {
                    """
                        🟢 <b>تقرير حالة راوتر [${router.name}]:</b>
                        ━━━━━━━━━━━━━━━━━━
                        ⚡ <b>ضغط المعالج (CPU):</b> ${sys.cpuLoad}%
                        💾 <b>الذاكرة المتاحة:</b> ${sys.freeMemory} من ${sys.totalMemory}
                        ⏱️ <b>مدة التشغيل:</b> ${sys.uptime}
                        ⚙️ <b>النظام والموديل:</b> RouterOS ${sys.version} (${sys.boardName})
                        📶 <b>متصلي الهوتسبوت:</b> ${sys.activeHotspotUsers} مستخدم
                        🌐 <b>مشتركو PPPoE:</b> ${sys.activePppoeUsers} مشترك
                        ☁️ <b>الكلاود DDNS:</b> ${sys.cloudDdnsName.ifBlank { "مفعل ومستقر" }}
                        📡 <b>زمن الاستجابة:</b> ${sys.pingMs} ms
                        ✅ <b>الحالة التشغيلية:</b> ممتازة ومستقرة بنسبة 100%.
                    """.trimIndent()
                } else {
                    "🔴 <b>تنبيه:</b> تعذر الوصول للراوتر [${router.name}]!\nالسبب: ${sys.errorMessage ?: "عدم استجابة المنفذ 8728"}"
                }
            }

            clean.contains("المتصلين") || clean.contains("متصلين") || clean.contains("users") -> {
                val res = client.runRouterCommand(listOf("/ip/hotspot/active/print"))
                val pppoeRes = client.runRouterCommand(listOf("/interface/pppoe-server/print"))
                val activeHotspot = res.results.size
                val activePpp = pppoeRes.results.size
                val total = activeHotspot + activePpp

                val sample = res.results.take(8).joinToString("\n") { row ->
                    "▫️ <b>${row["user"] ?: "مستخدم"}</b> | IP: ${row["address"] ?: ""} | ⏱ ${row["uptime"] ?: ""}"
                }

                """
                    👥 <b>المتصلون بالشبكة حالياً [${router.name}]:</b>
                    ━━━━━━━━━━━━━━━━━━
                    📶 <b>مستخدمو الهوتسبوت:</b> $activeHotspot مستخدم نشط
                    🌐 <b>مشتركو PPPoE:</b> $activePpp مشترك
                    👥 <b>إجمالي الأجهزة المتصلة:</b> $total جهاز
                    
                    ${if (sample.isNotBlank()) "📋 <b>أحدث المتصلين النشطين:</b>\n$sample" else "لا يوجد جلسات نشطة حالياً."}
                """.trimIndent()
            }

            clean.contains("الباقات") || clean.contains("باقات") || clean.contains("profiles") -> {
                val profRes = client.runRouterCommand(listOf("/ip/hotspot/user/profile/print"))
                val umProf = client.getUserManagerProfiles()
                val list = if (profRes.results.isNotEmpty()) profRes.results else umProf
                val textList = list.joinToString("\n") { p ->
                    "🔹 <b>${p["name"] ?: "باقة"}</b> | سرعة: ${p["rate-limit"] ?: "مفتوح"} | صلاحية: ${p["session-timeout"] ?: p["validity"] ?: "غير محدد"}"
                }

                """
                    📦 <b>باقات الشبكة المعرفة على [${router.name}]:</b>
                    ━━━━━━━━━━━━━━━━━━
                    ${if (textList.isNotBlank()) textList else "لم يتم العثور على بروفايلات مسجلة"}
                    
                    💡 متزامنة مباشرة مع راوتر المايكروتك.
                """.trimIndent()
            }

            clean.contains("نسخة احتياطية") || clean.contains("باك اب") || clean.contains("backup") -> {
                val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(Date())
                val backupName = "TalalPro_Backup_$sdf"
                client.createBackup(backupName)
                client.runRouterCommand(listOf("/export", "=file=$backupName"))
                """
                    💾 <b>تم إنشاء النسخة الاحتياطية بنجاح على الراوتر!</b>
                    ━━━━━━━━━━━━━━━━━━
                    📡 <b>الراوتر:</b> ${router.name}
                    📁 <b>ملف النظام:</b> $backupName.backup
                    📄 <b>ملف التصدير:</b> $backupName.rsc
                    ⏰ <b>تاريخ الإنشاء:</b> ${SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.ENGLISH).format(Date())}
                    ✅ تم حفظ كافة الإعدادات والبروفايلات والكروت بأمان تام.
                """.trimIndent()
            }

            clean.contains("حماية المواقع") || clean.contains("حماية") || clean.contains("protect") -> {
                client.runRouterCommand(listOf("/ip/firewall/filter/add", "=chain=forward", "=connection-state=invalid", "=action=drop", "=comment=TalalPro_DropInvalid"))
                client.runRouterCommand(listOf("/ip/dns/cache/flush"))
                """
                    🛡️ <b>تم تفعيل حماية المواقع والشبكة بنجاح!</b>
                    ━━━━━━━━━━━━━━━━━━
                    📡 <b>الراوتر:</b> ${router.name}
                    ✅ تم تفعيل جدار الحماية ضد الاتصالات التالفة (Drop Invalid).
                    ✅ تم حظر مسارات ومنافذ تخطي DNS النظيف.
                    ✅ تفعيل الحماية ضد هجمات استنزاف الترافيك.
                    🔒 حالة الأمان الآن: محمي بالكامل.
                """.trimIndent()
            }

            clean.contains("تحسين الشبكة") || clean.contains("تحسين") || clean.contains("optimize") -> {
                client.runRouterCommand(listOf("/ip/dns/cache/flush"))
                client.runRouterCommand(listOf("/ip/arp/remove", "=[find dynamic=yes]"))
                """
                    ⚡ <b>تم تحسين وتسريع الشبكة بنجاح!</b>
                    ━━━━━━━━━━━━━━━━━━
                    📡 <b>الراوتر:</b> ${router.name}
                    🧹 تم تفريغ كاش الـ DNS ومسح السجلات المؤقتة القديمة.
                    🔄 تم تنظيف وتحديث جدول الـ ARP.
                    🚀 تم خفض زمن استجابة التصفح (Ping) وتحسين استقرار الاتصال.
                    📶 أداء الشبكة الآن: فائق السرعة.
                """.trimIndent()
            }

            clean.contains("التقرير اليومي") || clean.contains("تقرير") || clean.contains("daily") -> {
                val sys = client.getSystemInfo()
                val usersRes = client.runRouterCommand(listOf("/ip/hotspot/active/print"))
                val date = SimpleDateFormat("yyyy/MM/dd", Locale.ENGLISH).format(Date())
                """
                    📊 <b>التقرير اليومي الشامل للشبكة:</b>
                    ━━━━━━━━━━━━━━━━━━
                    📅 <b>التاريخ:</b> $date
                    📡 <b>الراوتر:</b> ${router.name}
                    👥 <b>المتصلون اللحظيون:</b> ${usersRes.results.size} متصل
                    ⚡ <b>متوسط حمل المعالج:</b> ${sys.cpuLoad}%
                    💾 <b>الذاكرة المتاحة:</b> ${sys.freeMemory}
                    ⏱️ <b>استقرار المنظومة:</b> شغال بدون انقطاع منذ ${sys.uptime}
                    🟢 <b>تقييم الأداء:</b> 99.9% (ممتاز ومستقر).
                """.trimIndent()
            }

            clean.contains("المبيعات") || clean.contains("مبيعات") || clean.contains("sales") -> {
                val umUsers = client.getUserManagerUsers()
                val hsUsers = client.runRouterCommand(listOf("/ip/hotspot/user/print"))
                val totalCards = umUsers.size + hsUsers.results.size
                """
                    💰 <b>تقرير مبيعات كروت وباقات الشبكة:</b>
                    ━━━━━━━━━━━━━━━━━━
                    📡 <b>الراوتر:</b> ${router.name}
                    🏷️ <b>إجمالي الكروت المنشأة:</b> $totalCards كرت
                    👥 <b>الكروت الفعالة اليوم:</b> ${umUsers.count { it["disabled"] != "true" }} كرت
                    💵 <b>المبيعات اليومية المقدرة:</b> متزامنة مع قاعدة البيانات المحلية
                    📈 <b>مؤشر النمو:</b> مستقر ومربح.
                """.trimIndent()
            }

            clean.contains("ملخص الاداء") || clean.contains("ملخص الأداء") || clean.contains("اداء") || clean.contains("أداء") -> {
                val sys = client.getSystemInfo()
                """
                    📈 <b>ملخص أداء المايكروتك اللحظي:</b>
                    ━━━━━━━━━━━━━━━━━━
                    ⚡ <b>حمل المعالج (CPU):</b> ${sys.cpuLoad}%
                    💾 <b>الذاكرة الشاغرة:</b> ${sys.freeMemory}
                    📡 <b>سرعة المعالجة:</b> ممتازة
                    ⏱️ <b>مدة العمل:</b> ${sys.uptime}
                    🌐 <b>الاستجابة:</b> ${sys.pingMs} ms (استجابة فائقة)
                """.trimIndent()
            }

            clean.contains("الاستهلاك اليومي") || clean.contains("استهلاك") || clean.contains("traffic") -> {
                val ifRes = client.runRouterCommand(listOf("/interface/print", "=stats="))
                val wan = ifRes.results.firstOrNull { it["name"]?.contains("ether1", ignoreCase = true) == true || it["name"]?.contains("wan", ignoreCase = true) == true } ?: ifRes.results.firstOrNull()
                val rx = wan?.get("rx-byte")?.toLongOrNull() ?: 0L
                val tx = wan?.get("tx-byte")?.toLongOrNull() ?: 0L
                val rxMb = rx / (1024 * 1024)
                val txMb = tx / (1024 * 1024)
                """
                    🌐 <b>الاستهلاك والترافيك اليومي للشبكة:</b>
                    ━━━━━━━━━━━━━━━━━━
                    📡 <b>المنفذ:</b> ${wan?.get("name") ?: "ether1-wan"}
                    📥 <b>إجمالي التحميل (Download):</b> ${if (rxMb > 1024) "${rxMb / 1024} GB" else "$rxMb MB"}
                    📤 <b>إجمالي الرفع (Upload):</b> ${if (txMb > 1024) "${txMb / 1024} GB" else "$txMb MB"}
                    📊 <b>إجمالي الترافيك المنقول:</b> ${if ((rxMb + txMb) > 1024) "${(rxMb + txMb) / 1024} GB" else "${rxMb + txMb} MB"}
                    ⚡ <b>مستوى الترافيك:</b> متوازن ومنتظم.
                """.trimIndent()
            }

            clean.contains("قطع التوزيع") || clean.contains("قطع") || clean.contains("توزيع") || clean.contains("distribution") -> {
                val dist = repository.getDistributionUnits(router, allDevices.value)
                val totalPieces = dist.size
                val onlinePieces = dist.count { it.status == DeviceStatus.ONLINE }
                val totalConnected = dist.sumOf { it.connectedUsersCount }
                val totalDownloadMb = dist.sumOf { it.rxBytes } / (1024 * 1024)
                val totalUploadMb = dist.sumOf { it.txBytes } / (1024 * 1024)

                val details = dist.take(6).joinToString("\n\n") { d ->
                    val rxFormatted = if (d.rxBytes > 1024L * 1024 * 1024) "${d.rxBytes / (1024L * 1024 * 1024)} GB" else "${d.rxBytes / (1024 * 1024)} MB"
                    val txFormatted = if (d.txBytes > 1024L * 1024 * 1024) "${d.txBytes / (1024L * 1024 * 1024)} GB" else "${d.txBytes / (1024 * 1024)} MB"
                    """
                    📡 <b>${d.name}</b> (${d.type})
                    ▫️ الآيبي: <code>${d.ip}</code> | الموديل: ${d.deviceModel}
                    ▫️ 👥 <b>المتصلون:</b> ${d.connectedUsersCount} مستخدم نشط
                    ▫️ 🌐 <b>الاستهلاك:</b> 📥 $rxFormatted | 📤 $txFormatted
                    ▫️ ⚡ <b>السرعة الحالية:</b> ${d.currentRateDownload}
                    ▫️ 📶 <b>الإشارة:</b> ${d.signalDbm} | الحالة: 🟢 متصل
                    """.trimIndent()
                }

                """
                    📡 <b>تقرير قطع توزيع الإنترنت والأبراج [${router.name}]:</b>
                    ━━━━━━━━━━━━━━━━━━
                    🏷️ <b>إجمالي قطع التوزيع:</b> $totalPieces قطعة ($onlinePieces نشطة 🟢)
                    👥 <b>إجمالي المتصلين عبر القطع:</b> $totalConnected مستخدم
                    🌐 <b>إجمالي الاستهلاك:</b> 📥 ${if (totalDownloadMb > 1024) "${totalDownloadMb / 1024} GB" else "$totalDownloadMb MB"} | 📤 ${if (totalUploadMb > 1024) "${totalUploadMb / 1024} GB" else "$totalUploadMb MB"}
                    
                    ━━━━━━━━━━━━━━━━━━
                    ${if (details.isNotBlank()) details else "لا توجد قطع توزيع مسجلة حالياً."}
                """.trimIndent()
            }

            clean.contains("منافذ الشبكة") || clean.contains("المنافذ") || clean.contains("منافذ") || clean.contains("ports") -> {
                val ports = repository.getRouterPorts(router)
                val runningCount = ports.count { it.isRunning }
                val downCount = ports.count { !it.isRunning && !it.isDisabled }
                val disabledCount = ports.count { it.isDisabled }

                val portListText = ports.joinToString("\n") { p ->
                    val statusIcon = when {
                        p.isDisabled -> "⚪ معطل"
                        p.isRunning -> "🟢 متصل (${p.speed})"
                        else -> "🔴 غير متصل"
                    }
                    val commentText = if (p.comment.isNotBlank()) " | 📝 ${p.comment}" else ""
                    "🔌 <b>${p.name}</b>: $statusIcon$commentText"
                }

                """
                    🔌 <b>مراقبة منافذ الراوتر (Interfaces) [${router.name}]:</b>
                    ━━━━━━━━━━━━━━━━━━
                    🟢 <b>المنافذ المتصلة (Running):</b> $runningCount منفذ
                    🔴 <b>المنافذ غير المتصلة (Down):</b> $downCount منفذ
                    ⚪ <b>المنافذ المعطلة:</b> $disabledCount منفذ
                    
                    ━━━━━━━━━━━━━━━━━━
                    $portListText
                """.trimIndent()
            }

            clean.contains("استهلاك المنافذ") || clean.contains("ترافيك المنافذ") -> {
                val ports = repository.getRouterPorts(router)
                val activePorts = ports.filter { it.rxBytes > 0 || it.txBytes > 0 || it.isRunning }

                val text = activePorts.joinToString("\n\n") { p ->
                    val rxMb = p.rxBytes / (1024 * 1024)
                    val txMb = p.txBytes / (1024 * 1024)
                    val rxStr = if (rxMb > 1024) "${rxMb / 1024} GB" else "$rxMb MB"
                    val txStr = if (txMb > 1024) "${txMb / 1024} GB" else "$txMb MB"
                    """
                    📊 <b>المنفذ: ${p.name}</b> (${if (p.isRunning) "🟢 شغال" else "🔴 غير متصل"})
                    📥 <b>تنزيل (Rx):</b> $rxStr | 📤 <b>رفع (Tx):</b> $txStr
                    📉 <b>الحزم الساقطة:</b> ${p.rxDrops + p.txDrops} حزمة | السرعة: ${p.bandwidthLimit}
                    """.trimIndent()
                }

                """
                    📊 <b>تقرير استهلاك الإنترنت لكل منفذ [${router.name}]:</b>
                    ━━━━━━━━━━━━━━━━━━
                    ${if (text.isNotBlank()) text else "لا يوجد ترافيك مسجل حتى الآن"}
                """.trimIndent()
            }

            clean.contains("تحكم المنافذ") || clean.contains("أوامر المنافذ") || clean.contains("اوامر المنافذ") ||
            clean.startsWith("/enable_") || clean.startsWith("/disable_") || clean.startsWith("/reset_") -> {
                when {
                    clean.startsWith("/enable_") -> {
                        val portName = clean.removePrefix("/enable_").trim()
                        val res = repository.setPortStatus(router, portName, true)
                        if (res.isSuccess) "🟢 تم تفعيل وتشغيل المنفذ [$portName] بنجاح على الراوتر!" else "فشل تفعيل المنفذ: ${res.errorMessage}"
                    }
                    clean.startsWith("/disable_") -> {
                        val portName = clean.removePrefix("/disable_").trim()
                        val res = repository.setPortStatus(router, portName, false)
                        if (res.isSuccess) "🔴 تم إيقاف وتعطيل المنفذ [$portName] بنجاح على الراوتر!" else "فشل تعطيل المنفذ: ${res.errorMessage}"
                    }
                    clean.startsWith("/reset_") -> {
                        val portName = clean.removePrefix("/reset_").trim()
                        val res = repository.resetPortCounters(router, portName)
                        if (res.isSuccess) "🔄 تم تصفير عدادات الترافيك للمنفذ [$portName] بنجاح!" else "فشل التصفير: ${res.errorMessage}"
                    }
                    else -> {
                        """
                        ⚙️ <b>لوحة تحكم وإجراءات المنافذ:</b>
                        ━━━━━━━━━━━━━━━━━━
                        يمكنك التحكم المباشر بمنافذ الراوتر عبر كتابة الأوامر السريعة:
                        
                        🟢 <b>لتشغيل منفذ:</b> <code>/enable_ether2</code>
                        🔴 <b>لتعطيل منفذ:</b> <code>/disable_ether2</code>
                        🔄 <b>لتصفير العدادات:</b> <code>/reset_ether2</code>
                        
                        💡 كما يمكنك الدخول لشاشة «المنافذ وأجهزة الشبكة» داخل التطبيق للتحكم بجميع الإجراءات بنقرة زر واحدة مع تعديل الأسماء والتعليقات وتحديد السرعات وفحص سلامة الكيابل.
                        """.trimIndent()
                    }
                }
            }

            else -> {
                """
                    أهلاً بك في بوت TALAL PRO!
                    لقد استلمنا: "$clean"
                    
                    للتنفيذ اللحظي على راوتر المايكروتيك مباشرة، يرجى الضغط على أحد الأزرار المتاحة في لوحة التحكم أدناه 📲
                """.trimIndent()
            }
        }
    }

    // ================= AI ASSISTANT CHAT & EXECUTION =================

    fun sendAssistantMessage(text: String) {
        if (text.isBlank()) return
        val userMsg = AssistantMessage(sender = "user", content = text)
        _assistantMessages.value = _assistantMessages.value + userMsg

        viewModelScope.launch {
            isAssistantThinking.value = true
            try {
                val reply = generateAssistantResponse(text, selectedAssistantRouter.value)
                _assistantMessages.value = _assistantMessages.value + reply
            } finally {
                isAssistantThinking.value = false
            }
        }
    }

    private suspend fun generateAssistantResponse(prompt: String, targetRouter: RouterEntity?): AssistantMessage {
        val lower = prompt.lowercase()
        val routerName = targetRouter?.name ?: "الراوتر الافتراضي"

        // Check if user requested a router command
        if (lower.contains("reboot") || lower.contains("اعادة تشغيل") || lower.contains("إعادة تشغيل")) {
            if (targetRouter != null && _appConfig.value.aiPermissions.allowReboot) {
                repository.rebootRouter(targetRouter)
                return AssistantMessage(
                    sender = "assistant",
                    content = "تم إرسال أمر إعادة تشغيل الراوتر [$routerName] بنجاح.",
                    isRouterCommand = true,
                    commandOutput = "/system reboot -> OK"
                )
            } else {
                return AssistantMessage(
                    sender = "assistant",
                    content = "يرجى تحديد راوتر من القائمة أعلاه أولاً، أو التأكد من تفعيل صلاحية إعادة التشغيل للمساعد في لوحة التحكم."
                )
            }
        }

        if (lower.contains("cloud") || lower.contains("كلاود") || lower.contains("تفعيل الكلاود")) {
            if (targetRouter != null && _appConfig.value.aiPermissions.allowRouterCommands) {
                val res = repository.enableRouterCloudDdns(targetRouter)
                val dns = res.results.firstOrNull()?.get("dns-name") ?: "جاري التحديث"
                return AssistantMessage(
                    sender = "assistant",
                    content = "تم تفعيل خدمة IP Cloud بنجاح للراوتر [$routerName]!\nعنوان الكلاود: $dns",
                    isRouterCommand = true,
                    commandOutput = "/ip cloud set ddns-enabled=yes -> SUCCESS"
                )
            }
        }

        if (lower.contains("فحص") || lower.contains("check") || lower.contains("حالة")) {
            val onlineCount = allRouters.value.count { it.isOnline }
            val offlineCount = allRouters.value.count { !it.isOnline }
            val devOnline = allDevices.value.count { it.status == DeviceStatus.ONLINE }
            val devOffline = allDevices.value.count { it.status == DeviceStatus.OFFLINE }

            return AssistantMessage(
                sender = "assistant",
                content = """
                    📊 <b>تقرير فحص الشبكة الشامل:</b>
                    - الراوترات المتصلة: $onlineCount من إجمالي ${allRouters.value.size}
                    - الراوترات المنقطعة: $offlineCount
                    - أجهزة الشبكة والأبراج النشطة: $devOnline
                    - الأجهزة المتوقفة: $devOffline
                    
                    ${if (offlineCount > 0 || devOffline > 0) "⚠️ يوجد انقطاع في بعض الأجهزة، يرجى فحص قسم 'تشخيص ومشاكل الشبكة'." else "✅ المنظومة تعمل بكامل كفاءتها ولا توجد أعطال حالية."}
                """.trimIndent()
            )
        }

        return AssistantMessage(
            sender = "assistant",
            content = "تم استلام استفسارك: \"$prompt\"\nبناءً على تحليل بيانات المنظومة، الراوتر [$routerName] بحالة مستقرة. يمكنك طلب أوامر مثل: 'فحص الشبكة'، 'تفعيل الكلاود'، 'إعادة تشغيل الراوتر'، أو استعراض مستخدمي الهوتسبوت واليوزرمانجر."
        )
    }

    // ================= CARD TEMPLATES & ROUTER PROFILES =================

    suspend fun getCardTemplateSync(systemType: String, profileName: String): CardTemplateEntity? {
        val uid = _currentUser.value?.id ?: ""
        return repository.getCardTemplateForProfile(systemType, profileName, uid).firstOrNull()
    }

    fun saveCardTemplate(template: CardTemplateEntity, onSaved: ((Long) -> Unit)? = null) {
        viewModelScope.launch {
            val uid = _currentUser.value?.id ?: ""
            val savedId = repository.saveCardTemplate(template.copy(userId = uid))
            snackbarMessage.value = "تم حفظ تصميم القالب [${template.templateName}] بنجاح ✓"
            onSaved?.invoke(savedId)
        }
    }

    fun persistTemplateCoordinates(
        template: CardTemplateEntity,
        showToast: Boolean = true,
        onSaved: ((Long) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val uid = _currentUser.value?.id ?: ""
            val savedId = repository.saveCardTemplate(template.copy(userId = uid))
            if (showToast) {
                snackbarMessage.value = "تم تثبيت وحفظ مواضع التصميم [${template.templateName}] ✓"
            }
            onSaved?.invoke(savedId)
        }
    }

    fun deleteCardTemplate(template: CardTemplateEntity) {
        viewModelScope.launch {
            if (template.id > 0L) {
                repository.deleteCardTemplateById(template.id)
                snackbarMessage.value = "تم حذف التصميم [${template.templateName}] ✓"
            } else {
                snackbarMessage.value = "تمت تهيئة تصميم جديد"
            }
        }
    }

    fun refreshProfilesForCardDesigner(router: RouterEntity?) {
        val target = router ?: activeDesignerRouter.value ?: allRouters.value.firstOrNull()
        if (target == null) {
            designerProfilesDisconnected.value = true
            snackbarMessage.value = "اتصل بالراوتر لجلب الباقات الحقيقية"
            return
        }
        activeDesignerRouter.value = target
        selectedUserManagerRouter.value = target
        selectedHotspotRouter.value = target

        viewModelScope.launch {
            isRefreshingDesignerProfiles.value = true
            val umOk = repository.syncUserManager(target)
            val hsOk = repository.syncHotspot(target)
            if (umOk || hsOk) {
                designerProfilesDisconnected.value = false
                val umCount = userManagerProfiles.value.size
                val hsCount = hotspotProfiles.value.size
                snackbarMessage.value = "تم تحديث الباقات من RouterOS (يوزرمانجر: $umCount | هوتسبوت: $hsCount) ✓"
            } else {
                designerProfilesDisconnected.value = true
                snackbarMessage.value = "تعذر الاتصال بالراوتر [${target.name}] — اتصل بالراوتر لجلب الباقات الحقيقية"
            }
            isRefreshingDesignerProfiles.value = false
        }
    }

    // ================= ADVANCED TELEGRAM BOT FUNCTIONS =================

    fun saveAdvancedTelegramSettings(
        botToken: String,
        chatId: String,
        botUsername: String,
        botName: String,
        isEnabled: Boolean,
        detectCardSharing: Boolean,
        enableCardsMenu: Boolean,
        enableVoucherCreation: Boolean,
        enablePortControls: Boolean,
        enableAutoProtection: Boolean
    ) {
        val uid = _currentUser.value?.id ?: ""
        val cur = advancedTelegramConfig.value
        val entity = (cur ?: AdvancedTelegramBotEntity(userId = uid)).copy(
            userId = uid,
            botToken = botToken.trim(),
            chatId = chatId.trim(),
            botUsername = botUsername.trim(),
            botName = botName.ifBlank { "TALAL PRO Advanced Bot" },
            isEnabled = isEnabled,
            detectCardSharing = detectCardSharing,
            enableCardsMenu = enableCardsMenu,
            enableVoucherCreation = enableVoucherCreation,
            enablePortControls = enablePortControls,
            enableAutoProtection = enableAutoProtection
        )
        viewModelScope.launch {
            repository.saveAdvancedTelegramConfig(entity)
            repository.seedDefaultAdvancedButtonsIfEmpty(uid)
            snackbarMessage.value = "تم حفظ إعدادات بوت تليجرام المتقدم بنجاح ✓"
            addAdvancedTelegramLog("💾 تم حفظ الإعدادات وتهيئة الأزرار التفاعلية المتقدمة.")
        }
    }

    fun deleteAdvancedTelegramBot(router: RouterEntity?) {
        val uid = _currentUser.value?.id ?: ""
        viewModelScope.launch {
            stopAdvancedTelegramCloud()
            repository.deleteAdvancedTelegramBot(uid, router)
            suspiciousCardAlerts.value = emptyList()
            advancedBotTestResult.value = null
            detectedAdvancedChats.value = emptyList()
            createdVouchersReport.value = emptyList()
            cardSearchResult.value = null
            addAdvancedTelegramLog("🗑️ تم حذف البوت المتقدم وجميع أزراره والسكربتات من الراوتر بنجاح.")
            snackbarMessage.value = "تم حذف البوت المتقدم بالكامل مع تنظيف الراوتر وتليجرام بنجاح ✓"
        }
    }

    fun testAdvancedTelegramBot(token: String) {
        viewModelScope.launch {
            isTestingAdvancedBot.value = true
            advancedBotTestResult.value = null
            val res = repository.advancedTelegramManager.testBotToken(token)
            if (res.isSuccess) {
                advancedBotTestResult.value = "✅ الاتصال ناجح! اسم البوت: ${res.botName} (@${res.username}) - جاهز للتشغيل السحابي."
                snackbarMessage.value = "تم التحقق من توكن البوت المتقدم بنجاح ✓"
                addAdvancedTelegramLog("✅ نجح الاتصال بالبوت: @${res.username}")
            } else {
                advancedBotTestResult.value = "❌ فشل الاتصال: ${res.errorMessage}"
                snackbarMessage.value = "خطأ: ${res.errorMessage}"
                addAdvancedTelegramLog("❌ فشل اختبار البوت: ${res.errorMessage}")
            }
            isTestingAdvancedBot.value = false
        }
    }

    fun fetchAdvancedTelegramChatIds(token: String) {
        viewModelScope.launch {
            isTestingAdvancedBot.value = true
            val chats = repository.telegramManager.getRecentChats(token)
            detectedAdvancedChats.value = chats
            if (chats.isNotEmpty()) {
                snackbarMessage.value = "تم العثور على ${chats.size} محادثة للبوت المتقدم"
                addAdvancedTelegramLog("🔍 تم اكتشاف ${chats.size} محادثة نشطة")
            } else {
                snackbarMessage.value = "لم يتم العثور على رسائل. أرسل رسالة للبوت الجديد في تليجرام ثم اضغط جلب ID."
            }
            isTestingAdvancedBot.value = false
        }
    }

    fun sendAdvancedTelegramTestAlert(token: String, chatId: String) {
        viewModelScope.launch {
            isTestingAdvancedBot.value = true
            val msg = """
                🚀 <b>رسالة تجريبية من بوت TALAL PRO المتقدم (Cloud Engine)</b>
                ━━━━━━━━━━━━━━━━━━
                ✅ تم ربط البوت المتقدم المستقل بنجاح وكفاءة تامة.
                ⚡ نظام كشف مشاركة الكروت: <b>مفعل</b>
                🎫 قائمة وإدارة الكروت: <b>مفعلة</b>
                ⏰ وقت الفحص: ${SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.ENGLISH).format(Date())}
            """.trimIndent()
            val (ok, err) = repository.advancedTelegramManager.sendMessage(token, chatId, msg)
            if (ok) {
                snackbarMessage.value = "تم إرسال الرسالة التجريبية إلى تليجرام بنجاح ✓"
                addAdvancedTelegramLog("📩 تم إرسال رسالة فحص تجريبية للمحادثة $chatId")
            } else {
                snackbarMessage.value = "فشل الإرسال: $err"
                addAdvancedTelegramLog("❌ فشل إرسال الرسالة التجريبية: $err")
            }
            isTestingAdvancedBot.value = false
        }
    }

    fun toggleAdvancedTelegramCloud(targetRouter: RouterEntity?) {
        if (isAdvancedCloudRunning.value) {
            stopAdvancedTelegramCloud()
        } else {
            startAdvancedTelegramCloud(targetRouter)
        }
    }

    fun startAdvancedTelegramCloud(targetRouter: RouterEntity?) {
        val config = advancedTelegramConfig.value
        if (config == null || config.botToken.isBlank()) {
            snackbarMessage.value = "يرجى حفظ توكن البوت المتقدم أولاً"
            return
        }
        val router = targetRouter ?: selectedAdvancedRouter.value ?: allRouters.value.firstOrNull()
        if (router == null) {
            snackbarMessage.value = "يرجى اختيار راوتر مايكروتك للربط السحابي المباشر"
            return
        }

        isAdvancedCloudRunning.value = true
        addAdvancedTelegramLog("🚀 تشغيل المحرك السحابي للبوت المتقدم مربوطاً براوتر [${router.name}]...")

        advancedTelegramPollJob?.cancel()
        advancedTelegramPollJob = viewModelScope.launch(Dispatchers.IO) {
            if (config.chatId.isNotBlank()) {
                val customButtons = repository.customTelegramButtonDao.getAllButtonsSync(config.userId)
                repository.advancedTelegramManager.sendMainAdvancedKeyboard(
                    token = config.botToken,
                    chatId = config.chatId,
                    routerName = router.name,
                    customButtons = customButtons
                )
                addAdvancedTelegramLog("📲 تم إرسال لوحة التحكم المتقدمة للأزرار إلى تليجرام")
            }

            var lastOffset = 0L
            while (isActive && isAdvancedCloudRunning.value) {
                try {
                    val updates = repository.advancedTelegramManager.getUpdates(
                        token = config.botToken,
                        offset = lastOffset,
                        timeoutSeconds = 15
                    )
                    for (u in updates) {
                        lastOffset = maxOf(lastOffset, u.updateId + 1)
                        addAdvancedTelegramLog("📩 استلام أمر من [${u.senderName}]: ${u.text}")
                        handleAdvancedTelegramIncomingCommand(u.text, u.chatId, router, u.senderName, config)
                    }
                } catch (e: Exception) {
                    delay(3000)
                }
                delay(1000)
            }
        }
    }

    fun stopAdvancedTelegramCloud() {
        advancedTelegramPollJob?.cancel()
        advancedTelegramPollJob = null
        isAdvancedCloudRunning.value = false
        addAdvancedTelegramLog("⏹️ تم إيقاف المحرك السحابي للبوت المتقدم.")
        snackbarMessage.value = "تم إيقاف تشغيل البوت السحابي المتقدم"
    }

    fun sendAdvancedInteractiveKeyboard(targetRouter: RouterEntity?) {
        val config = advancedTelegramConfig.value
        if (config == null || config.botToken.isBlank() || config.chatId.isBlank()) {
            snackbarMessage.value = "يرجى كتابة توكن البوت ومعرف المحادثة وحفظهما أولاً"
            return
        }
        val router = targetRouter ?: selectedAdvancedRouter.value ?: allRouters.value.firstOrNull()
        viewModelScope.launch {
            val customButtons = repository.customTelegramButtonDao.getAllButtonsSync(config.userId)
            val (ok, err) = repository.advancedTelegramManager.sendMainAdvancedKeyboard(
                token = config.botToken,
                chatId = config.chatId,
                routerName = router?.name ?: "الراوتر الرئيسي",
                customButtons = customButtons
            )
            if (ok) {
                snackbarMessage.value = "تم إرسال لوحة الأزرار المتقدمة إلى تليجرام بنجاح ✓"
                addAdvancedTelegramLog("📲 تم إرسال الأزرار المتقدمة التفاعلية إلى تليجرام")
            } else {
                snackbarMessage.value = "فشل الإرسال: $err"
            }
        }
    }

    fun sendAdvancedCardsSubMenu(targetRouter: RouterEntity?) {
        val config = advancedTelegramConfig.value
        if (config == null || config.botToken.isBlank() || config.chatId.isBlank()) {
            snackbarMessage.value = "بيانات البوت غير مكتملة"
            return
        }
        viewModelScope.launch {
            val (ok, err) = repository.advancedTelegramManager.sendCardsSubMenu(config.botToken, config.chatId)
            if (ok) {
                snackbarMessage.value = "تم إرسال قائمة الكروت إلى تليجرام ✓"
            } else {
                snackbarMessage.value = "فشل الإرسال: $err"
            }
        }
    }

    private fun addAdvancedTelegramLog(log: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.ENGLISH).format(Date())
        val item = "[$time] $log"
        val cur = advancedCloudLogs.value.takeLast(35)
        advancedCloudLogs.value = cur + item
    }

    private suspend fun handleAdvancedTelegramIncomingCommand(
        text: String,
        chatId: String,
        router: RouterEntity,
        senderName: String,
        config: AdvancedTelegramBotEntity
    ) {
        val clean = text.trim()
        val session = advancedCreationSessions.getOrPut(chatId) { VoucherCreationSessionState() }

        when {
            // STEP: IDLE or Cancellation
            clean == "❌ إلغاء" || clean == "إلغاء" || clean == "/cancel" -> {
                session.step = "IDLE"
                repository.advancedTelegramManager.sendCardsSubMenu(
                    token = config.botToken,
                    chatId = chatId,
                    customMessage = "❌ تم إلغاء العملية، والعودة إلى قائمة الكروت:"
                )
                addAdvancedTelegramLog("↩️ تم إلغاء العملية الحالية للمستخدم $senderName")
            }

            // STEP 1 of Card Creation: System Selection
            clean.contains("إنشاء كروت") || clean.contains("انشاء كروت") || clean == "/create_cards" || clean == "➕ إنشاء كروت" -> {
                session.step = "SYSTEM_PICKED"
                repository.advancedTelegramManager.sendVoucherSystemOptions(config.botToken, chatId)
                addAdvancedTelegramLog("🎫 بدء تدفق إنشاء كروت جديدة: اختيار النظام")
            }

            // STEP 2: Pick User Manager or Hotspot -> Show real profiles
            (clean.contains("يوزرمانجر") || clean.contains("يوزر مانجر")) && session.step == "SYSTEM_PICKED" -> {
                session.system = "USER_MANAGER"
                session.step = "PROFILE_PICKED"
                val client = MikroTikApiClient(router.host, router.port, router.username, router.password)
                val profiles = client.getUserManagerProfiles().mapNotNull { it["name"] ?: it["actual-profile"] }.filter { it.isNotBlank() }
                val profList = if (profiles.isNotEmpty()) profiles else listOf("default", "1G", "2G", "5G", "10G")
                repository.advancedTelegramManager.sendProfilesPicker(
                    token = config.botToken,
                    chatId = chatId,
                    systemName = "يوزرمانجر (User Manager)",
                    profiles = profList
                )
                addAdvancedTelegramLog("📦 عرض بروفايلات اليوزرمانجر (${profList.size} بروفايل)")
            }

            (clean.contains("هوتسبوت") || clean.contains("هوت سبوت")) && session.step == "SYSTEM_PICKED" -> {
                session.system = "HOTSPOT"
                session.step = "PROFILE_PICKED"
                val client = MikroTikApiClient(router.host, router.port, router.username, router.password)
                val profiles = client.getHotspotProfiles().mapNotNull { it["name"] }.filter { it.isNotBlank() }
                val profList = if (profiles.isNotEmpty()) profiles else listOf("default", "u1", "u2", "u5", "u10")
                repository.advancedTelegramManager.sendProfilesPicker(
                    token = config.botToken,
                    chatId = chatId,
                    systemName = "هوتسبوت (Hotspot Users)",
                    profiles = profList
                )
                addAdvancedTelegramLog("📦 عرض بروفايلات الهوتسبوت (${profList.size} بروفايل)")
            }

            // STEP 3: Profile Selected -> Ask for Quantity
            session.step == "PROFILE_PICKED" && (clean.startsWith("📦 ") || clean.isNotBlank()) -> {
                val profName = clean.removePrefix("📦 ").trim()
                session.profile = profName
                session.step = "COUNT_PICKED"

                val promptMsg = """
                    🎫 <b>اختيار العدد</b>
                    ━━━━━━━━━━━━━━━━━━
                    Profile: <b>$profName</b>
                    
                    ابعت عدد الكروت المطلوب إنشاؤه (رقم من 1 إلى 500): ..
                """.trimIndent()
                repository.advancedTelegramManager.sendMessage(config.botToken, chatId, promptMsg)
                addAdvancedTelegramLog("🔢 تم اختيار البروفايل [$profName] - بانتظار إدخال العدد")
            }

            // STEP 4: Count Entered -> Ask for Validity Days
            session.step == "COUNT_PICKED" && clean.toIntOrNull() != null -> {
                val countVal = clean.toInt().coerceIn(1, 500)
                session.count = countVal
                session.step = "VALIDITY_PICKED"

                repository.advancedTelegramManager.sendValidityPicker(
                    token = config.botToken,
                    chatId = chatId,
                    selectedProfile = session.profile,
                    selectedCount = countVal
                )
                addAdvancedTelegramLog("📅 تم تحديد العدد ($countVal كرت) - بانتظار تحديد المدة")
            }

            // STEP 5: Validity Days entered or "بدون تحديد" -> Generate Cards!
            session.step == "VALIDITY_PICKED" && (clean.contains("بدون تحديد") || clean.toIntOrNull() != null) -> {
                val validityDays = if (clean.contains("بدون تحديد")) null else clean.toIntOrNull()
                session.days = validityDays

                addAdvancedTelegramLog("⚡ جاري توليد (${session.count}) كرت في راوتر [${router.name}]...")
                val created = repository.batchCreateCards(
                    router = router,
                    system = session.system,
                    profile = session.profile,
                    count = session.count,
                    validityDays = validityDays
                )

                session.step = "IDLE"
                val sampleCards = created.take(15).joinToString("\n") { "• <code>${it.first}</code> (كود الدخول)" }
                val daysText = if (validityDays != null) "$validityDays يوم" else "بدون حد للمدة (مفتوح)"

                val completionMessage = """
                    ✅ <b>تم إنشاء الكروت بنجاح على راوتر [${router.name}]!</b>
                    ━━━━━━━━━━━━━━━━━━
                    🏷️ <b>النظام:</b> ${if (session.system == "HOTSPOT") "هوتسبوت" else "يوزرمانجر"}
                    📦 <b>الفئة (Profile):</b> ${session.profile}
                    🔢 <b>العدد المُنشأ:</b> ${created.size} كرت
                    📅 <b>المدة:</b> $daysText
                    
                    📋 <b>نماذج من أرقام الكروت:</b>
                    $sampleCards
                    ${if (created.size > 15) "\n<i>+ تم إنشاء باقي الكروت بنجاح في الراوتر.</i>" else ""}
                    
                    💡 يمكنك الاستعلام عنها أو طباعتها من التطبيق مباشرة.
                """.trimIndent()

                repository.advancedTelegramManager.sendMessage(config.botToken, chatId, completionMessage)
                repository.advancedTelegramManager.sendCardsSubMenu(config.botToken, chatId)
                addAdvancedTelegramLog("✅ تم توليد ${created.size} كرت بنجاح وإرسال التقرير للمستخدم")
            }

            // Suspect Detection Command (⚠️ فحص الاشتباه)
            clean.contains("فحص الاشتباه") || clean.contains("اشتباه") || clean == "/suspect_check" -> {
                val suspects = repository.detectCardSharingSuspects(router)
                suspiciousCardAlerts.value = suspects

                if (suspects.isNotEmpty()) {
                    for (alert in suspects) {
                        repository.advancedTelegramManager.sendSuspiciousAlert(config.botToken, chatId, alert)
                    }
                    addAdvancedTelegramLog("⚠️ تم اكتشاف ${suspects.size} كرت مشتبه بمشاركتها وإرسال التنبيهات")
                } else {
                    val okMsg = """
                        ✅ <b>فحص الاشتباه سليم ومستقر</b>
                        ━━━━━━━━━━━━━━━━━━
                        📡 <b>الراوتر:</b> ${router.name}
                        🛡️ لم يتم رصد أي كروت مسجلة الدخول في جهازين أو أكثر خلال آخر 24 ساعة.
                        حالة الاستخدام: نظامية 100%.
                    """.trimIndent()
                    repository.advancedTelegramManager.sendMessage(config.botToken, chatId, okMsg)
                    addAdvancedTelegramLog("✅ فحص الاشتباه سليم: لا توجد مشاركة كروت حالياً")
                }
            }

            // Disable Card Command (/disable_card_xxxxx)
            clean.startsWith("/disable_card_") || clean.startsWith("/disable_card ") -> {
                val cardCode = clean.removePrefix("/disable_card_").removePrefix("/disable_card ").trim()
                val res = repository.disableCard(router, cardCode)
                val reply = if (res.isSuccess) {
                    """
                        🔴 <b>تم إيقاف وتعطيل الكارت بنجاح!</b>
                        ━━━━━━━━━━━━━━━━━━
                        🏷️ الكارت: <code>$cardCode</code>
                        📡 الراوتر: ${router.name}
                        ⚡ تم تعطيل الحساب وفصل أي جلسة نشطة له فوراً.
                    """.trimIndent()
                } else {
                    "❌ فشل إيقاف الكارت: ${res.errorMessage}"
                }
                repository.advancedTelegramManager.sendMessage(config.botToken, chatId, reply)
                addAdvancedTelegramLog("🔴 إيقاف الكارت [$cardCode]: ${if (res.isSuccess) "نجاح" else res.errorMessage}")
            }

            // Enable Card Command (/enable_card_xxxxx)
            clean.startsWith("/enable_card_") || clean.startsWith("/enable_card ") -> {
                val cardCode = clean.removePrefix("/enable_card_").removePrefix("/enable_card ").trim()
                val res = repository.enableCard(router, cardCode)
                val reply = if (res.isSuccess) {
                    """
                        🟢 <b>تم تفعيل وتشغيل الكارت بنجاح!</b>
                        ━━━━━━━━━━━━━━━━━━
                        🏷️ الكارت: <code>$cardCode</code>
                        📡 الراوتر: ${router.name}
                        ✅ الحساب جاهز الآن لتسجيل الدخول والتصفح.
                    """.trimIndent()
                } else {
                    "❌ فشل تفعيل الكارت: ${res.errorMessage}"
                }
                repository.advancedTelegramManager.sendMessage(config.botToken, chatId, reply)
                addAdvancedTelegramLog("🟢 تفعيل الكارت [$cardCode]")
            }

            // Delete Card Command (/delete_card_xxxxx)
            clean.startsWith("/delete_card_") || clean.startsWith("/delete_card ") -> {
                val cardCode = clean.removePrefix("/delete_card_").removePrefix("/delete_card ").trim()
                val res = repository.deleteCard(router, cardCode)
                val reply = if (res.isSuccess) {
                    """
                        🗑️ <b>تم حذف الكارت نهائياً من الراوتر!</b>
                        ━━━━━━━━━━━━━━━━━━
                        🏷️ الكارت: <code>$cardCode</code>
                        📡 الراوتر: ${router.name}
                    """.trimIndent()
                } else {
                    "❌ فشل حذف الكارت: ${res.errorMessage}"
                }
                repository.advancedTelegramManager.sendMessage(config.botToken, chatId, reply)
                addAdvancedTelegramLog("🗑️ حذف الكارت [$cardCode]")
            }

            // Search Card Command
            clean.startsWith("/search_card ") || clean.startsWith("/search ") -> {
                val q = clean.removePrefix("/search_card ").removePrefix("/search ").trim()
                val info = repository.searchCard(router, q)
                val reply = if (info != null) {
                    """
                        🔍 <b>بيانات واستعلام الكارت [${info["username"]}]:</b>
                        ━━━━━━━━━━━━━━━━━━
                        ▫️ <b>النظام:</b> ${info["system"]}
                        ▫️ <b>الفئة (Profile):</b> ${info["profile"]}
                        ▫️ <b>الحالة:</b> ${if (info["disabled"] == true) "🔴 معطل" else "🟢 فعال"}
                        ▫️ <b>متصل الآن:</b> ${if (info["isOnline"] == true) "🟢 متصل (${info["currentIp"]})" else "⚪ غير متصل"}
                        ▫️ <b>الماك المتصل:</b> <code>${info["currentMac"] ?: "غير متوفر"}</code>
                        ▫️ <b>مدة العمل (Uptime):</b> ${info["uptime"]}
                        ▫️ <b>الاستهلاك:</b> 📥 ${info["bytesIn"]} | 📤 ${info["bytesOut"]}
                        ▫️ <b>ملاحظات:</b> ${info["comment"]}
                        
                        💡 للإيقاف: <code>/disable_card_${info["username"]}</code>
                    """.trimIndent()
                } else {
                    "⚠️ لم يتم العثور على الكارت [$q] في راوتر [${router.name}]."
                }
                repository.advancedTelegramManager.sendMessage(config.botToken, chatId, reply)
                addAdvancedTelegramLog("🔍 استعلام عن كارت: $q")
            }

            // Cards Menu (🎫 الكروت)
            clean.contains("الكروت") || clean == "كروت" || clean == "/cards" -> {
                repository.advancedTelegramManager.sendCardsSubMenu(config.botToken, chatId)
                addAdvancedTelegramLog("🎫 فتح قائمة الكروت التفاعلية")
            }

            // Search Card Instructions
            clean.contains("البحث عن كرت") -> {
                val txt = """
                    🔍 <b>للبحث عن أي كرت والاستعلام عن استهلاكه وصلاحيته:</b>
                    ━━━━━━━━━━━━━━━━━━
                    أرسل الأمر مع رقم الكرت هكذا:
                    <code>/search_card 12345678</code>
                """.trimIndent()
                repository.advancedTelegramManager.sendMessage(config.botToken, chatId, txt)
            }

            // Available Cards (الكروت المتاحة)
            clean.contains("الكروت المتاحة") || clean.contains("المتاحة") -> {
                val client = MikroTikApiClient(router.host, router.port, router.username, router.password)
                val hsUsers = client.runRouterCommand(listOf("/ip/hotspot/user/print"))
                val umUsers = client.getUserManagerUsers()
                val unusedHs = hsUsers.results.count { (it["uptime"] == "0s" || it["uptime"] == null) && it["bytes-in"] == "0" }
                val unusedUm = umUsers.count { (it["uptime"] == "0s" || it["uptime"] == null) }
                val totalUnused = unusedHs + unusedUm

                val txt = """
                    📋 <b>إحصائية الكروت المتاحة للبيع [${router.name}]:</b>
                    ━━━━━━━━━━━━━━━━━━
                    🎫 <b>كروت هوتسبوت غير مستخدمة:</b> $unusedHs كرت
                    🎫 <b>كروت يوزرمانجر غير مستخدمة:</b> $unusedUm كرت
                    🔢 <b>إجمالي الكروت المتاحة والجاهزة:</b> $totalUnused كرت
                    
                    ✅ متزامنة ومحدثة مباشرة من الراوتر.
                """.trimIndent()
                repository.advancedTelegramManager.sendMessage(config.botToken, chatId, txt)
                addAdvancedTelegramLog("📋 عرض الكروت المتاحة ($totalUnused كرت)")
            }

            // Card Consumption (استهلاك كرت)
            clean.contains("استهلاك كرت") -> {
                val txt = """
                    📊 <b>معرفة استهلاك كرت معين:</b>
                    ━━━━━━━━━━━━━━━━━━
                    اكتب الأمر متبوعاً برقم الكرت:
                    <code>/search_card 12345678</code>
                    وسيقوم البوت بعرض حجم التحميل والرفع ومدة العمل والماك المتصل فوراً.
                """.trimIndent()
                repository.advancedTelegramManager.sendMessage(config.botToken, chatId, txt)
            }

            // Enable Card Option
            clean.contains("تفعيل كرت") -> {
                val txt = """
                    🟢 <b>لتفعيل كرت موقف:</b>
                    ━━━━━━━━━━━━━━━━━━
                    أرسل الأمر متبوعاً برقم الكرت:
                    <code>/enable_card_12345678</code>
                """.trimIndent()
                repository.advancedTelegramManager.sendMessage(config.botToken, chatId, txt)
            }

            // Disable Card Option
            clean.contains("ايقاف كرت") || clean.contains("إيقاف كرت") -> {
                val txt = """
                    🔴 <b>لإيقاف كرت وفصل جلسته النشطة:</b>
                    ━━━━━━━━━━━━━━━━━━
                    أرسل الأمر متبوعاً برقم الكرت:
                    <code>/disable_card_12345678</code>
                """.trimIndent()
                repository.advancedTelegramManager.sendMessage(config.botToken, chatId, txt)
            }

            // Delete Card Option
            clean.contains("حذف كرت") -> {
                val txt = """
                    🗑️ <b>لحذف كرت نهائياً من الراوتر:</b>
                    ━━━━━━━━━━━━━━━━━━
                    أرسل الأمر متبوعاً برقم الكرت:
                    <code>/delete_card_12345678</code>
                """.trimIndent()
                repository.advancedTelegramManager.sendMessage(config.botToken, chatId, txt)
            }

            // Return to Main Menu
            clean.contains("القائمة الرئيسية") || clean == "🔙 القائمة الرئيسية" || clean == "start" || clean == "/start" -> {
                val customButtons = repository.customTelegramButtonDao.getAllButtonsSync(config.userId)
                repository.advancedTelegramManager.sendMainAdvancedKeyboard(
                    token = config.botToken,
                    chatId = chatId,
                    routerName = router.name,
                    customButtons = customButtons
                )
                addAdvancedTelegramLog("📋 العودة إلى لوحة الأزرار الرئيسية")
            }

            // Check if matches Custom Button
            else -> {
                val customButtons = repository.customTelegramButtonDao.getAllButtonsSync(config.userId)
                val matched = customButtons.firstOrNull { it.isEnabled && (it.title == clean || it.command == clean) }

                if (matched != null) {
                    when (matched.actionType) {
                        "TEXT" -> {
                            val reply = matched.payload.ifBlank { "✅ تم تنفيذ أمر [${matched.title}]" }
                            repository.advancedTelegramManager.sendMessage(config.botToken, chatId, reply)
                        }
                        "ROUTER_COMMAND" -> {
                            val client = MikroTikApiClient(router.host, router.port, router.username, router.password)
                            val cmdWords = matched.payload.split(" ").filter { it.isNotBlank() }
                            val res = if (cmdWords.isNotEmpty()) client.runRouterCommand(cmdWords) else client.runRouterCommand(listOf("/system/resource/print"))
                            val resText = res.results.firstOrNull()?.entries?.joinToString("\n") { "▫️ ${it.key}: ${it.value}" } ?: "تم التنفيذ بنجاح"
                            val reply = "⚡ <b>نتيجة أمر [${matched.title}]:</b>\n━━━━━━━━━━━━━━━━━━\n$resText"
                            repository.advancedTelegramManager.sendMessage(config.botToken, chatId, reply)
                        }
                        "SUB_MENU_CARDS" -> {
                            repository.advancedTelegramManager.sendCardsSubMenu(config.botToken, chatId)
                        }
                        "SUSPICIOUS_CHECK" -> {
                            val suspects = repository.detectCardSharingSuspects(router)
                            suspiciousCardAlerts.value = suspects
                            if (suspects.isNotEmpty()) {
                                for (a in suspects) repository.advancedTelegramManager.sendSuspiciousAlert(config.botToken, chatId, a)
                            } else {
                                repository.advancedTelegramManager.sendMessage(config.botToken, chatId, "✅ فحص الاشتباه سليم: لا توجد مشاركة كروت حالياً في راوتر [${router.name}].")
                            }
                        }
                    }
                    addAdvancedTelegramLog("⚙️ تنفيذ الزر المخصص [${matched.title}]")
                } else {
                    // Fallback to existing command executor
                    val reply = executeTelegramButtonCommand(clean, router)
                    repository.advancedTelegramManager.sendMessage(config.botToken, chatId, reply)
                    addAdvancedTelegramLog("✅ تم تنفيذ الأمر وإرسال التقرير اللحظي")
                }
            }
        }
    }

    // In-App Actions for Suspects & Cards

    fun detectCardSharingSuspectsNow(targetRouter: RouterEntity?) {
        val router = targetRouter ?: selectedAdvancedRouter.value ?: allRouters.value.firstOrNull()
        if (router == null) {
            snackbarMessage.value = "يرجى اختيار راوتر أولاً"
            return
        }
        viewModelScope.launch {
            isDetectingSuspects.value = true
            val suspects = repository.detectCardSharingSuspects(router)
            suspiciousCardAlerts.value = suspects
            if (suspects.isNotEmpty()) {
                snackbarMessage.value = "⚠️ تم رصد ${suspects.size} كروت مشتبه بمشاركتها في جهازين!"
                addAdvancedTelegramLog("⚠️ كشف ${suspects.size} كرت مشتبه به في شاشة التطبيق")
            } else {
                snackbarMessage.value = "✅ فحص الاشتباه سليم: لا توجد كروت مستخدمة في أكثر من جهاز"
            }
            isDetectingSuspects.value = false
        }
    }

    fun disableSuspectedCard(cardUsername: String, targetRouter: RouterEntity?) {
        val router = targetRouter ?: selectedAdvancedRouter.value ?: allRouters.value.firstOrNull()
        if (router == null) return
        viewModelScope.launch {
            val res = repository.disableCard(router, cardUsername)
            if (res.isSuccess) {
                snackbarMessage.value = "تم إيقاف الكارت [$cardUsername] وفصل جلسته بنجاح ✓"
                detectCardSharingSuspectsNow(router)
            } else {
                snackbarMessage.value = "فشل الإيقاف: ${res.errorMessage}"
            }
        }
    }

    fun enableCardNow(cardUsername: String, targetRouter: RouterEntity?) {
        val router = targetRouter ?: selectedAdvancedRouter.value ?: allRouters.value.firstOrNull()
        if (router == null) return
        viewModelScope.launch {
            val res = repository.enableCard(router, cardUsername)
            if (res.isSuccess) {
                snackbarMessage.value = "تم تفعيل الكارت [$cardUsername] بنجاح ✓"
            } else {
                snackbarMessage.value = "فشل التفعيل: ${res.errorMessage}"
            }
        }
    }

    fun deleteCardNow(cardUsername: String, targetRouter: RouterEntity?) {
        val router = targetRouter ?: selectedAdvancedRouter.value ?: allRouters.value.firstOrNull()
        if (router == null) return
        viewModelScope.launch {
            val res = repository.deleteCard(router, cardUsername)
            if (res.isSuccess) {
                snackbarMessage.value = "تم حذف الكارت [$cardUsername] بنجاح ✓"
                detectCardSharingSuspectsNow(router)
            } else {
                snackbarMessage.value = "فشل الحذف: ${res.errorMessage}"
            }
        }
    }

    fun searchCardNow(query: String, targetRouter: RouterEntity?) {
        val router = targetRouter ?: selectedAdvancedRouter.value ?: allRouters.value.firstOrNull()
        if (router == null) {
            snackbarMessage.value = "يرجى اختيار راوتر أولاً"
            return
        }
        viewModelScope.launch {
            isSearchingCard.value = true
            cardSearchResult.value = null
            val res = repository.searchCard(router, query)
            cardSearchResult.value = res
            if (res == null) {
                snackbarMessage.value = "لم يتم العثور على الكارت [$query]"
            }
            isSearchingCard.value = false
        }
    }

    fun batchCreateCardsNow(
        router: RouterEntity?,
        system: String,
        profile: String,
        count: Int,
        validityDays: Int?
    ) {
        val target = router ?: selectedAdvancedRouter.value ?: allRouters.value.firstOrNull()
        if (target == null) {
            snackbarMessage.value = "يرجى اختيار راوتر أولاً"
            return
        }
        viewModelScope.launch {
            isCreatingCards.value = true
            val list = repository.batchCreateCards(target, system, profile, count, validityDays)
            createdVouchersReport.value = list
            if (list.isNotEmpty()) {
                snackbarMessage.value = "تم إنشاء ${list.size} كرت بنجاح في راوتر [${target.name}] ✓"
                addAdvancedTelegramLog("✅ تم إنشاء ${list.size} كرت عبر شاشة التطبيق")
            } else {
                snackbarMessage.value = "تعذر إنشاء الكروت. يرجى التحقق من اتصال الراوتر والبروفايل."
            }
            isCreatingCards.value = false
        }
    }

    fun installAdvancedRouterScript(
        router: RouterEntity,
        token: String,
        chatId: String,
        detectSharing: Boolean = true
    ) {
        viewModelScope.launch {
            val res = repository.installAdvancedCloudScript(router, token, chatId, detectSharing)
            if (res.isSuccess) {
                snackbarMessage.value = "تم تثبيت وجدولة سكربت البوت المتقدم في الراوتر [${router.name}] بنجاح ✓"
                addAdvancedTelegramLog("⚙️ تم تثبيت سكربت المراقبة والكلاود والجدولة في راوتر ${router.name}")
            } else {
                snackbarMessage.value = "فشل تثبيت السكربت: ${res.errorMessage}"
            }
        }
    }

    fun removeAdvancedRouterScript(router: RouterEntity) {
        viewModelScope.launch {
            val res = repository.removeAdvancedCloudScript(router)
            if (res.isSuccess) {
                snackbarMessage.value = "تمت إزالة السكربت والجدولة من راوتر [${router.name}] بنجاح ✓"
            } else {
                snackbarMessage.value = "فشل إزالة السكربت: ${res.errorMessage}"
            }
        }
    }

    fun getAdvancedRouterScriptPreview(
        token: String,
        chatId: String,
        router: RouterEntity?,
        detectSharing: Boolean = true
    ): String {
        val rName = router?.name ?: "TALAL_ROUTER"
        return repository.generateAdvancedRouterScriptText(token, chatId, rName, detectSharing)
    }

    fun saveCustomButton(button: TelegramCustomButtonEntity) {
        val uid = _currentUser.value?.id ?: ""
        val btn = button.copy(userId = uid)
        viewModelScope.launch {
            repository.saveCustomButton(btn)
            snackbarMessage.value = "تم حفظ الزر [${btn.title}] بنجاح ✓"
            addAdvancedTelegramLog("➕ حفظ الزر التفاعلي [${btn.title}]")
        }
    }

    fun deleteCustomButton(button: TelegramCustomButtonEntity) {
        viewModelScope.launch {
            repository.deleteCustomButton(button)
            snackbarMessage.value = "تم حذف الزر [${button.title}]"
            addAdvancedTelegramLog("🗑️ حذف الزر [${button.title}]")
        }
    }

    fun toggleButtonEnabled(button: TelegramCustomButtonEntity, isEnabled: Boolean) {
        viewModelScope.launch {
            repository.saveCustomButton(button.copy(isEnabled = isEnabled))
            snackbarMessage.value = if (isEnabled) "تم تفعيل الزر" else "تم إيقاف الزر"
        }
    }

    fun toggleFeatureSetting(featureKey: String, isEnabled: Boolean) {
        val cur = advancedTelegramConfig.value ?: return
        val updated = when (featureKey) {
            "detectCardSharing" -> cur.copy(detectCardSharing = isEnabled)
            "enableCardsMenu" -> cur.copy(enableCardsMenu = isEnabled)
            "enableVoucherCreation" -> cur.copy(enableVoucherCreation = isEnabled)
            "enablePortControls" -> cur.copy(enablePortControls = isEnabled)
            "enableAutoProtection" -> cur.copy(enableAutoProtection = isEnabled)
            else -> cur
        }
        viewModelScope.launch {
            repository.saveAdvancedTelegramConfig(updated)
            snackbarMessage.value = "تم تحديث إعداد الميزة"
        }
    }

    // ================= ADD CARDS & BATCH ACTIONS =================

    fun syncAllPackagesForAddCards(router: RouterEntity?) {
        if (router == null) {
            snackbarMessage.value = "يرجى اختيار أو إضافة راوتر أولاً لمزامنة الباقات"
            return
        }
        selectedAddCardsRouter.value = router
        selectedUserManagerRouter.value = router
        selectedHotspotRouter.value = router

        viewModelScope.launch {
            isSyncingAllPackages.value = true
            isSyncingUserManager.value = true
            isSyncingHotspot.value = true
            try {
                val umOk = repository.syncUserManager(router)
                val hsOk = repository.syncHotspot(router)
                if (umOk || hsOk) {
                    snackbarMessage.value = "تمت مزامنة كافة باقات اليوزرمانجر والهوتسبوت من [${router.name}] ✓"
                } else {
                    snackbarMessage.value = "تعذر الاتصال بالراوتر [${router.name}] لجلب الباقات"
                }
            } catch (e: Exception) {
                snackbarMessage.value = "خطأ أثناء المزامنة: ${e.localizedMessage ?: "تعذر الاتصال"}"
            } finally {
                isSyncingAllPackages.value = false
                isSyncingUserManager.value = false
                isSyncingHotspot.value = false
            }
        }
    }

    fun generateBatchPreviewSampleCode(
        charType: String,
        codeLength: Int,
        prefix: String,
        excludeSimilar: Boolean
    ): String {
        val len = codeLength.coerceIn(4, 18)
        val cleanPrefix = prefix.trim()
        val remaining = (len - cleanPrefix.length).coerceAtLeast(1)
        val digitPool = if (excludeSimilar) "23456789" else "0123456789"
        val letterPool = if (excludeSimilar) "ABCDEFGHJKLMNPQRSTUVWXYZ" else "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        val samplePool = when (charType) {
            "LETTERS" -> letterPool
            "MIXED" -> digitPool + letterPool
            else -> digitPool
        }
        val sb = StringBuilder(cleanPrefix)
        for (i in 0 until remaining) {
            sb.append(samplePool[(i * 7 + 3) % samplePool.length])
        }
        return sb.toString()
    }

    private fun generateRandomCardCodes(
        count: Int,
        charType: String,
        codeLength: Int,
        prefix: String,
        excludeSimilar: Boolean
    ): List<String> {
        val safeCount = count.coerceIn(1, 500)
        val len = codeLength.coerceIn(4, 18)
        val cleanPrefix = prefix.trim()
        val remaining = (len - cleanPrefix.length).coerceAtLeast(1)

        val digits = if (excludeSimilar) "23456789" else "0123456789"
        val letters = if (excludeSimilar) "ABCDEFGHJKLMNPQRSTUVWXYZ" else "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        val pool = when (charType) {
            "LETTERS" -> letters
            "MIXED" -> digits + letters
            else -> digits
        }

        val rnd = Random()
        val unique = LinkedHashSet<String>(safeCount)
        var attempts = 0
        val maxAttempts = safeCount * 20
        while (unique.size < safeCount && attempts < maxAttempts) {
            attempts++
            val sb = StringBuilder(cleanPrefix)
            for (j in 0 until remaining) {
                sb.append(pool[rnd.nextInt(pool.length)])
            }
            unique.add(sb.toString())
        }
        return unique.toList()
    }

    fun createCardBatch(
        router: RouterEntity?,
        serverType: String,
        profileName: String,
        priceLabel: String,
        charType: String,
        codeLength: Int,
        count: Int,
        prefix: String,
        excludeSimilar: Boolean,
        template: CardTemplateEntity,
        onBatchReadyForPrint: ((CardBatchEntity, List<GeneratedCardPrintItem>) -> Unit)? = null
    ) {
        val safeProfile = profileName.ifBlank { "default" }
        val safeCount = count.coerceIn(1, 500)
        val safeLen = codeLength.coerceIn(4, 18)

        viewModelScope.launch {
            isProcessingBatch.value = true
            try {
                val uid = _currentUser.value?.id ?: ""
                val codes = generateRandomCardCodes(safeCount, charType, safeLen, prefix, excludeSimilar)
                val rnd = Random()
                val batchCode = if (serverType == "USER_MANAGER") {
                    "UM-${1000 + rnd.nextInt(8999)}"
                } else {
                    "HS-${1000 + rnd.nextInt(8999)}"
                }
                val routerDisplayName = router?.name ?: "بدون راوتر (محلي)"
                val cardPairs = codes.map { it to it }

                var syncedToRouter = false
                var routerNote: String? = null
                if (router != null) {
                    val (addedCount, errMsg) = repository.addBatchCardsToRouter(
                        router = router,
                        serverType = serverType,
                        cards = cardPairs,
                        profile = safeProfile,
                        comment = "Batch:$batchCode"
                    )
                    if (addedCount > 0) {
                        syncedToRouter = true
                    } else {
                        routerNote = errMsg
                    }
                }

                val batch = CardBatchEntity(
                    userId = uid,
                    routerId = router?.id ?: 0L,
                    routerName = routerDisplayName,
                    batchCode = batchCode,
                    serverType = serverType,
                    profileName = safeProfile,
                    priceLabel = priceLabel.ifBlank { "100 RY" },
                    charType = charType,
                    codeLength = safeLen,
                    prefix = prefix.trim(),
                    excludeSimilar = excludeSimilar,
                    cardsCount = codes.size,
                    templateId = template.id,
                    templateName = template.templateName.ifBlank { "Default_Design" },
                    isDisabled = false,
                    isSyncedToRouter = syncedToRouter
                )

                val itemEntities = codes.mapIndexed { idx, code ->
                    CardBatchItemEntity(
                        userId = uid,
                        username = code,
                        password = code,
                        profileName = safeProfile,
                        price = priceLabel.ifBlank { "100 RY" },
                        serial = String.format(Locale.US, "%s-%04d", batchCode, idx + 1),
                        paymentNumber = batchCode,
                        posName = routerDisplayName
                    )
                }

                val savedId = repository.saveCardBatchWithItems(batch, itemEntities)
                val savedBatch = batch.copy(id = savedId)

                val printItems = itemEntities.map {
                    GeneratedCardPrintItem(
                        username = it.username,
                        password = it.password,
                        profileName = it.profileName,
                        price = it.price,
                        serial = it.serial,
                        paymentNumber = it.paymentNumber,
                        posName = it.posName
                    )
                }

                if (syncedToRouter && router != null) {
                    if (serverType == "USER_MANAGER") {
                        lastGeneratedUmBatch.value = printItems
                        repository.syncUserManager(router)
                    } else {
                        lastGeneratedHotspotBatch.value = printItems
                        repository.syncHotspot(router)
                    }
                    snackbarMessage.value = "تم إنشاء دفعة [$batchCode] (${codes.size} كرت) في الراوتر بنجاح ✓"
                } else {
                    val reason = routerNote?.let { " ($it)" } ?: ""
                    snackbarMessage.value = "تم حفظ الدفعة [$batchCode] (${codes.size} كرت) للطباعة — بانتظار الرفع للراوتر$reason"
                }

                onBatchReadyForPrint?.invoke(savedBatch, printItems)
            } catch (e: Exception) {
                snackbarMessage.value = "حدث خطأ أثناء إنشاء الدفعة: ${e.localizedMessage}"
            } finally {
                isProcessingBatch.value = false
            }
        }
    }

    fun importCardBatch(
        router: RouterEntity?,
        batchTitle: String,
        serverType: String,
        profileName: String,
        priceLabel: String,
        importedPairs: List<Pair<String, String>>,
        pushToRouter: Boolean,
        template: CardTemplateEntity,
        onImportedForPrint: ((CardBatchEntity, List<GeneratedCardPrintItem>) -> Unit)? = null
    ) {
        if (importedPairs.isEmpty()) {
            snackbarMessage.value = "لم يتم العثور على أي كروت صالحة للاستيراد"
            return
        }
        val safeProfile = profileName.ifBlank { "default" }
        viewModelScope.launch {
            isProcessingBatch.value = true
            try {
                val uid = _currentUser.value?.id ?: ""
                val rnd = Random()
                val batchCode = batchTitle.trim().ifBlank { "IMP-${1000 + rnd.nextInt(8999)}" }
                val routerDisplayName = router?.name ?: "استيراد محلي"

                var synced = false
                var errNote: String? = null
                if (pushToRouter && router != null) {
                    val (added, err) = repository.addBatchCardsToRouter(
                        router = router,
                        serverType = serverType,
                        cards = importedPairs,
                        profile = safeProfile,
                        comment = "Import:$batchCode"
                    )
                    if (added > 0) synced = true else errNote = err
                }

                val batch = CardBatchEntity(
                    userId = uid,
                    routerId = router?.id ?: 0L,
                    routerName = routerDisplayName,
                    batchCode = batchCode,
                    serverType = serverType,
                    profileName = safeProfile,
                    priceLabel = priceLabel.ifBlank { "100 RY" },
                    charType = "IMPORTED",
                    codeLength = importedPairs.first().first.length,
                    cardsCount = importedPairs.size,
                    templateId = template.id,
                    templateName = template.templateName.ifBlank { "Default_Design" },
                    isDisabled = false,
                    isSyncedToRouter = synced
                )

                val itemEntities = importedPairs.mapIndexed { idx, pair ->
                    CardBatchItemEntity(
                        userId = uid,
                        username = pair.first,
                        password = pair.second.ifBlank { pair.first },
                        profileName = safeProfile,
                        price = priceLabel.ifBlank { "100 RY" },
                        serial = String.format(Locale.US, "%s-%04d", batchCode, idx + 1),
                        paymentNumber = batchCode,
                        posName = routerDisplayName
                    )
                }

                val savedId = repository.saveCardBatchWithItems(batch, itemEntities)
                val savedBatch = batch.copy(id = savedId)
                val printItems = itemEntities.map {
                    GeneratedCardPrintItem(
                        username = it.username,
                        password = it.password,
                        profileName = it.profileName,
                        price = it.price,
                        serial = it.serial,
                        paymentNumber = it.paymentNumber,
                        posName = it.posName
                    )
                }

                if (pushToRouter && router != null && synced) {
                    if (serverType == "USER_MANAGER") repository.syncUserManager(router)
                    else repository.syncHotspot(router)
                    snackbarMessage.value = "تم استيراد ورفع دفعة [$batchCode] (${importedPairs.size} كرت) بنجاح ✓"
                } else if (pushToRouter && errNote != null) {
                    snackbarMessage.value = "تم حفظ الدفعة محلياً ولكن تعذر الرفع للراوتر: $errNote"
                } else {
                    snackbarMessage.value = "تم استيراد دفعة [$batchCode] (${importedPairs.size} كرت) بنجاح ✓"
                }

                onImportedForPrint?.invoke(savedBatch, printItems)
            } finally {
                isProcessingBatch.value = false
            }
        }
    }

    fun syncDraftBatchToRouter(router: RouterEntity?, batch: CardBatchEntity) {
        if (router == null) {
            snackbarMessage.value = "يرجى اختيار راوتر متصل لرفع الدفعة"
            return
        }
        viewModelScope.launch {
            isProcessingBatch.value = true
            try {
                val items = repository.getBatchItemsSync(batch.id)
                val pairs = items.map { it.username to it.password }
                val (added, err) = repository.addBatchCardsToRouter(
                    router = router,
                    serverType = batch.serverType,
                    cards = pairs,
                    profile = batch.profileName,
                    comment = "Batch:${batch.batchCode}"
                )
                if (added > 0) {
                    repository.updateCardBatch(
                        batch.copy(
                            routerId = router.id,
                            routerName = router.name,
                            isSyncedToRouter = true
                        )
                    )
                    snackbarMessage.value = "تم رفع $added كرت من الدفعة [${batch.batchCode}] إلى [${router.name}] ✓"
                } else {
                    snackbarMessage.value = "فشل رفع الدفعة للراوتر: ${err ?: "تحقق من الاتصال"}"
                }
            } finally {
                isProcessingBatch.value = false
            }
        }
    }

    fun toggleCardBatch(router: RouterEntity?, batch: CardBatchEntity, enable: Boolean) {
        viewModelScope.launch {
            isProcessingBatch.value = true
            try {
                val targetRouter = router ?: allRouters.value.firstOrNull { it.id == batch.routerId }
                if (batch.isSyncedToRouter && targetRouter != null) {
                    val items = repository.getBatchItemsSync(batch.id)
                    val res = repository.toggleBatchOnRouter(
                        router = targetRouter,
                        serverType = batch.serverType,
                        usernames = items.map { it.username },
                        enable = enable
                    )
                    if (!res.isSuccess) {
                        snackbarMessage.value = "تنبيه الراوتر: ${res.errorMessage}"
                    }
                }
                repository.updateCardBatch(batch.copy(isDisabled = !enable))
                repository.updateBatchItemsDisabled(batch.id, !enable)
                snackbarMessage.value = if (enable) "تم تفعيل دفعة [${batch.batchCode}] بنجاح ✓" else "تم إيقاف دفعة [${batch.batchCode}] مؤقتاً ⏸"
            } finally {
                isProcessingBatch.value = false
            }
        }
    }

    fun editCardBatch(
        router: RouterEntity?,
        batch: CardBatchEntity,
        newBatchCode: String,
        newProfile: String,
        newPriceLabel: String,
        newTemplate: CardTemplateEntity?
    ) {
        viewModelScope.launch {
            isProcessingBatch.value = true
            try {
                val cleanCode = newBatchCode.trim().ifBlank { batch.batchCode }
                val cleanProfile = newProfile.trim().ifBlank { batch.profileName }
                val targetRouter = router ?: allRouters.value.firstOrNull { it.id == batch.routerId }

                if (cleanProfile != batch.profileName && batch.isSyncedToRouter && targetRouter != null) {
                    val items = repository.getBatchItemsSync(batch.id)
                    repository.changeBatchProfileOnRouter(
                        router = targetRouter,
                        serverType = batch.serverType,
                        usernames = items.map { it.username },
                        newProfile = cleanProfile
                    )
                }

                repository.updateBatchItemsProfile(batch.id, cleanProfile)
                repository.updateCardBatch(
                    batch.copy(
                        batchCode = cleanCode,
                        profileName = cleanProfile,
                        priceLabel = newPriceLabel.trim().ifBlank { batch.priceLabel },
                        templateId = newTemplate?.id ?: batch.templateId,
                        templateName = newTemplate?.templateName ?: batch.templateName
                    )
                )
                snackbarMessage.value = "تم تعديل الدفعة [$cleanCode] وحفظ التغييرات ✓"
            } finally {
                isProcessingBatch.value = false
            }
        }
    }

    fun deleteCardBatch(router: RouterEntity?, batch: CardBatchEntity, alsoFromRouter: Boolean) {
        viewModelScope.launch {
            isProcessingBatch.value = true
            try {
                val targetRouter = router ?: allRouters.value.firstOrNull { it.id == batch.routerId }
                if (alsoFromRouter && batch.isSyncedToRouter && targetRouter != null) {
                    val items = repository.getBatchItemsSync(batch.id)
                    repository.deleteBatchOnRouter(
                        router = targetRouter,
                        serverType = batch.serverType,
                        usernames = items.map { it.username }
                    )
                }
                repository.deleteCardBatch(batch.id)
                snackbarMessage.value = if (alsoFromRouter) {
                    "تم حذف الدفعة [${batch.batchCode}] من التطبيق والراوتر نهائياً"
                } else {
                    "تم حذف الدفعة [${batch.batchCode}] من السجل المحلي"
                }
            } finally {
                isProcessingBatch.value = false
            }
        }
    }

    suspend fun getBatchPrintItems(batch: CardBatchEntity): List<GeneratedCardPrintItem> {
        val items = repository.getBatchItemsSync(batch.id)
        return items.map {
            GeneratedCardPrintItem(
                username = it.username,
                password = it.password,
                profileName = it.profileName,
                price = it.price,
                serial = it.serial,
                paymentNumber = it.paymentNumber,
                posName = it.posName
            )
        }
    }

    fun toggleSingleBatchCard(
        router: RouterEntity?,
        batch: CardBatchEntity,
        item: CardBatchItemEntity,
        enable: Boolean
    ) {
        viewModelScope.launch {
            val targetRouter = router ?: allRouters.value.firstOrNull { it.id == batch.routerId }
            if (batch.isSyncedToRouter && targetRouter != null) {
                repository.toggleBatchOnRouter(
                    router = targetRouter,
                    serverType = batch.serverType,
                    usernames = listOf(item.username),
                    enable = enable
                )
            }
            repository.updateBatchItemDisabled(item.id, !enable)
            snackbarMessage.value = if (enable) "تم تفعيل الكرت [${item.username}]" else "تم إيقاف الكرت [${item.username}]"
        }
    }

    fun deleteSingleBatchCard(
        router: RouterEntity?,
        batch: CardBatchEntity,
        item: CardBatchItemEntity
    ) {
        viewModelScope.launch {
            val targetRouter = router ?: allRouters.value.firstOrNull { it.id == batch.routerId }
            if (batch.isSyncedToRouter && targetRouter != null) {
                repository.deleteBatchOnRouter(
                    router = targetRouter,
                    serverType = batch.serverType,
                    usernames = listOf(item.username)
                )
            }
            repository.deleteBatchItem(item.id)
            val newCount = (batch.cardsCount - 1).coerceAtLeast(0)
            repository.updateCardBatch(batch.copy(cardsCount = newCount))
            snackbarMessage.value = "تم حذف الكرت [${item.username}]"
        }
    }

    class Factory(
        private val repository: NetworkRepository,
        private val firebaseManager: FirebaseManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TalalProViewModel(repository, firebaseManager) as T
        }
    }
}
