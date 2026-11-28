package com.example

import android.Manifest
import android.app.Activity
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.service.RouterMonitorManager
import com.example.ui.ScreenTab
import com.example.ui.TalalProViewModel
import com.example.ui.components.SideDrawerContent
import com.example.ui.components.TalalProBottomBar
import com.example.ui.components.TalalProTopBar
import com.example.ui.dialogs.*
import com.example.ui.screens.*
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.PrimaryNavy
import com.example.ui.theme.TalalProTheme
import kotlinx.coroutines.launch

enum class MainNavigationState {
    APP_MAIN,
    ADMIN_PANEL
}

class MainActivity : ComponentActivity() {

    private val viewModel: TalalProViewModel by viewModels {
        val app = application as TalalProApplication
        TalalProViewModel.Factory(app.repository, app.firebaseManager)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            // Force RTL Arabic Layout Direction as requested
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                TalalProTheme(darkTheme = false) {
                    TalalProMainApp(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun TalalProMainApp(viewModel: TalalProViewModel) {
    val context = LocalContext.current
    var isSplashVisible by remember { mutableStateOf(true) }

    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val isAdmin by viewModel.isAdmin.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val showActivationScreen by viewModel.showActivationScreen.collectAsState()
    val pendingActivationUser by viewModel.pendingActivationUser.collectAsState()
    val currentTab by viewModel.currentTab.collectAsState()
    val soundEnabled by viewModel.soundEnabled.collectAsState()
    val unreadAlertsCount by viewModel.unreadAlertsCount.collectAsState()
    val alerts by viewModel.allAlerts.collectAsState()
    val routers by viewModel.allRouters.collectAsState()
    val devices by viewModel.allDevices.collectAsState()
    val snackbarMsg by viewModel.snackbarMessage.collectAsState()

    var navState by remember { mutableStateOf(MainNavigationState.APP_MAIN) }
    var showExitDialog by remember { mutableStateOf(false) }
    var showPowerControlDialog by remember { mutableStateOf(false) }

    val showAddRouter by viewModel.showAddRouterDialog.collectAsState()
    val showCloudGuide by viewModel.showCloudGuideDialog.collectAsState()
    val showAddDevice by viewModel.showAddDeviceDialog.collectAsState()
    val showImportNeighbors by viewModel.showImportNeighborsDialog.collectAsState()
    val showNetwatchSync by viewModel.showNetwatchSyncDialog.collectAsState()
    val showNotifications by viewModel.showNotificationsDialog.collectAsState()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted && RouterMonitorManager.isEnabled(context)) {
            RouterMonitorManager.startService(context)
        }
    }

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            RouterMonitorManager.ensureInitialized(context)
            if (RouterMonitorManager.isEnabled(context)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    !RouterMonitorManager.hasNotificationPermission(context)
                ) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    RouterMonitorManager.startService(context)
                }
            }
        }
    }

    LaunchedEffect(snackbarMsg) {
        if (snackbarMsg != null) {
            snackbarHostState.showSnackbar(snackbarMsg ?: "")
            viewModel.clearSnackbar()
        }
    }

    if (isSplashVisible) {
        SplashScreen(onTimeout = { isSplashVisible = false })
        return
    }

    if (!isLoggedIn) {
        if (showActivationScreen || (currentUser != null && !currentUser!!.isActivated)) {
            ActivationScreen(
                viewModel = viewModel,
                user = pendingActivationUser ?: currentUser,
                onActivated = {
                    // Automatically enters main dashboard
                },
                onLogout = { viewModel.logout() }
            )
        } else {
            LoginScreen(viewModel = viewModel)
        }
        return
    }

    if (navState == MainNavigationState.ADMIN_PANEL && isAdmin) {
        BackHandler {
            navState = MainNavigationState.APP_MAIN
        }
        AdminControlPanelScreen(
            viewModel = viewModel,
            onNavigateBack = { navState = MainNavigationState.APP_MAIN }
        )
        return
    }

    // Back button handling: If drawer open -> close drawer; If subscreen -> return to dashboard; If dashboard -> show exit confirmation dialog
    BackHandler {
        if (drawerState.isOpen) {
            scope.launch { drawerState.close() }
        } else if (currentTab != ScreenTab.DASHBOARD) {
            viewModel.setTab(ScreenTab.DASHBOARD)
        } else {
            showExitDialog = true
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            SideDrawerContent(
                viewModel = viewModel,
                onNavigate = { tab -> viewModel.setTab(tab) },
                onNavigateToAdmin = {
                    navState = MainNavigationState.ADMIN_PANEL
                },
                onOpenPowerControl = { showPowerControlDialog = true },
                onCloseDrawer = { scope.launch { drawerState.close() } }
            )
        }
    ) {
        Scaffold(
            topBar = {
                TalalProTopBar(
                    soundEnabled = soundEnabled,
                    unreadAlertsCount = unreadAlertsCount,
                    onToggleDrawer = {
                        scope.launch {
                            if (drawerState.isOpen) drawerState.close() else drawerState.open()
                        }
                    },
                    onToggleSound = { viewModel.toggleSound() },
                    onOpenNotifications = { viewModel.showNotificationsDialog.value = true },
                    onOpenPowerControl = { showPowerControlDialog = true },
                    onLogout = { viewModel.logout() }
                )
            },
            bottomBar = {
                TalalProBottomBar(
                    currentTab = currentTab,
                    onTabSelected = { tab -> viewModel.setTab(tab) }
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            modifier = Modifier.fillMaxSize()
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                AnimatedContent(
                    targetState = currentTab,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "TabContentTransition"
                ) { tab ->
                    when (tab) {
                        ScreenTab.DASHBOARD -> DashboardScreen(viewModel = viewModel)
                        ScreenTab.ROUTERS -> RoutersScreen(viewModel = viewModel)
                        ScreenTab.DEVICES -> NetworkDevicesScreen(viewModel = viewModel)
                        ScreenTab.PORTS_AND_DEVICES -> PortsAndDistributionScreen(viewModel = viewModel)
                        ScreenTab.USER_MANAGER -> UserManagerScreen(viewModel = viewModel)
                        ScreenTab.HOTSPOT -> HotspotScreen(viewModel = viewModel)
                        ScreenTab.ACTIVE_USERS -> ActiveUsersScreen(viewModel = viewModel)
                        ScreenTab.ADD_CARDS -> AddCardsScreen(viewModel = viewModel)
                        ScreenTab.CARD_DESIGNER -> CardDesignerScreen(viewModel = viewModel)
                        ScreenTab.MAP -> DeviceMapScreen(viewModel = viewModel)
                        ScreenTab.TOOLS -> SpecialToolsScreen(viewModel = viewModel)
                        ScreenTab.TELEGRAM -> TelegramBotScreen(viewModel = viewModel)
                        ScreenTab.ADVANCED_TELEGRAM -> AdvancedTelegramBotScreen(
                            viewModel = viewModel,
                            onNavigateBack = { viewModel.setTab(ScreenTab.TELEGRAM) }
                        )
                        ScreenTab.ASSISTANT -> SmartAssistantScreen(viewModel = viewModel)
                        ScreenTab.ISSUES -> NetworkIssuesScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }

    // Exit Confirmation Dialog
    if (showExitDialog) {
        Dialog(onDismissRequest = { showExitDialog = false }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                tonalElevation = 8.dp,
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFEE2E2),
                        modifier = Modifier.size(52.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.ExitToApp,
                                contentDescription = null,
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Text(
                        text = "الخروج من التطبيق",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryNavy
                    )

                    Text(
                        text = "هل أنت متأكد من رغبتك في إغلاق تطبيق TALAL PRO والخروج؟",
                        fontSize = 13.sp,
                        color = Color(0xFF475569),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showExitDialog = false },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("إلغاء", fontSize = 13.sp)
                        }

                        Button(
                            onClick = {
                                showExitDialog = false
                                (context as? Activity)?.finish()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("خروج", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Dialogs
    if (showAddRouter) {
        AddRouterDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.showAddRouterDialog.value = false }
        )
    }

    if (showCloudGuide) {
        CloudGuideDialog(
            onDismiss = { viewModel.showCloudGuideDialog.value = false },
            onNavigateToAddRouter = {
                viewModel.showCloudGuideDialog.value = false
                viewModel.showAddRouterDialog.value = true
            }
        )
    }

    if (showAddDevice) {
        AddDeviceDialog(
            viewModel = viewModel,
            routers = routers,
            onDismiss = { viewModel.showAddDeviceDialog.value = false }
        )
    }

    if (showImportNeighbors) {
        ImportNeighborsDialog(
            viewModel = viewModel,
            routers = routers,
            onDismiss = { viewModel.showImportNeighborsDialog.value = false }
        )
    }

    if (showNetwatchSync) {
        NetwatchSyncDialog(
            viewModel = viewModel,
            routers = routers,
            devices = devices,
            onDismiss = { viewModel.showNetwatchSyncDialog.value = false }
        )
    }

    if (showNotifications) {
        NotificationsDialog(
            alerts = alerts,
            onDismiss = { viewModel.showNotificationsDialog.value = false },
            onTestSound = { viewModel.testSound() },
            onMarkAllRead = { viewModel.markAllAlertsRead() },
            onClearAll = { viewModel.clearAllAlerts() },
            onViewAllIssues = {
                viewModel.showNotificationsDialog.value = false
                viewModel.setTab(ScreenTab.ISSUES)
            }
        )
    }

    if (showPowerControlDialog) {
        RouterPowerControlDialog(
            routers = routers,
            onDismiss = { showPowerControlDialog = false },
            onReboot = { r ->
                viewModel.rebootRouter(r)
                showPowerControlDialog = false
            },
            onShutdown = { r ->
                viewModel.shutdownRouter(r)
                showPowerControlDialog = false
            }
        )
    }
}

@Composable
private fun RouterPowerControlDialog(
    routers: List<com.example.data.model.RouterEntity>,
    onDismiss: () -> Unit,
    onReboot: (com.example.data.model.RouterEntity) -> Unit,
    onShutdown: (com.example.data.model.RouterEntity) -> Unit
) {
    var selectedRouter by remember(routers) { mutableStateOf(routers.firstOrNull { it.isOnline } ?: routers.firstOrNull()) }
    var routerDropdownExpanded by remember { mutableStateOf(false) }
    var pendingConfirmAction by remember { mutableStateOf<String?>(null) } // "REBOOT" or "SHUTDOWN"

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = Color(0xFF0F172A),
            border = BorderStroke(1.dp, Color(0xFF334155)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF59E0B).copy(alpha = 0.2f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.PowerSettingsNew,
                                    contentDescription = null,
                                    tint = Color(0xFFFBBF24),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "التحكم بطاقة الراوتر",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "إعادة تشغيل أو إيقاف تشغيل راوتر المايكروتك",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                }

                if (routers.isEmpty()) {
                    Text(
                        text = "لا توجد راوترات مضافة حالياً. أضف راوتر أولاً.",
                        color = Color(0xFFFCA5A5),
                        fontSize = 12.sp
                    )
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    ) {
                        Text("إغلاق", color = Color.White)
                    }
                } else {
                    Box {
                        OutlinedCard(
                            onClick = { routerDropdownExpanded = true },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFF334155)),
                            colors = CardDefaults.outlinedCardColors(containerColor = Color(0xFF1E293B)),
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedRouter?.let { "${it.name} (${it.host})" } ?: "اختر الراوتر",
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = if (selectedRouter?.isOnline == true) "متصل" else "غير متصل",
                                    color = if (selectedRouter?.isOnline == true) Color(0xFF34D399) else Color(0xFFFCA5A5),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        DropdownMenu(
                            expanded = routerDropdownExpanded,
                            onDismissRequest = { routerDropdownExpanded = false },
                            modifier = Modifier.background(Color(0xFF1E293B))
                        ) {
                            routers.forEach { r ->
                                DropdownMenuItem(
                                    text = { Text("${r.name} (${r.host})", color = Color.White) },
                                    onClick = {
                                        selectedRouter = r
                                        routerDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    if (pendingConfirmAction != null && selectedRouter != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF7F1D1D).copy(alpha = 0.55f),
                            border = BorderStroke(1.dp, Color(0xFFEF4444)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = if (pendingConfirmAction == "SHUTDOWN") {
                                        "⚠️ تأكيد إيقاف تشغيل [${selectedRouter!!.name}] كلياً؟ لن يعمل الراوتر إلا بعد إعادة تشغيله يدوياً بالكهرباء!"
                                    } else {
                                        "🔄 تأكيد إعادة تشغيل [${selectedRouter!!.name}] الآن؟ سينقطع اتصال المشتركين لمدة 30 ثانية."
                                    },
                                    color = Color(0xFFFEE2E2),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { pendingConfirmAction = null },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                                    ) {
                                        Text("تراجع", color = Color.White, fontSize = 12.sp)
                                    }
                                    Button(
                                        onClick = {
                                            val r = selectedRouter!!
                                            if (pendingConfirmAction == "SHUTDOWN") onShutdown(r) else onReboot(r)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                                    ) {
                                        Text("تأكيد التنفيذ", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    } else {
                        Button(
                            onClick = { pendingConfirmAction = "REBOOT" },
                            enabled = selectedRouter != null,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        ) {
                            Icon(Icons.Default.RestartAlt, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("إعادة تشغيل الراوتر (Reboot)", fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        Button(
                            onClick = { pendingConfirmAction = "SHUTDOWN" },
                            enabled = selectedRouter != null,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        ) {
                            Icon(Icons.Default.PowerSettingsNew, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("إيقاف تشغيل الراوتر نهائياً (Shutdown)", fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        ) {
                            Text("إلغاء", color = Color(0xFFCBD5E1))
                        }
                    }
                }
            }
        }
    }
}

