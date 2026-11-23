package com.example

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
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

    val showAddRouter by viewModel.showAddRouterDialog.collectAsState()
    val showCloudGuide by viewModel.showCloudGuideDialog.collectAsState()
    val showAddDevice by viewModel.showAddDeviceDialog.collectAsState()
    val showImportNeighbors by viewModel.showImportNeighborsDialog.collectAsState()
    val showNetwatchSync by viewModel.showNetwatchSyncDialog.collectAsState()
    val showNotifications by viewModel.showNotificationsDialog.collectAsState()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

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
                onCloseDrawer = { scope.launch { drawerState.close() } }
            )
        }
    ) {
        Scaffold(
            topBar = {
                TalalProTopBar(
                    isAdmin = isAdmin,
                    soundEnabled = soundEnabled,
                    unreadAlertsCount = unreadAlertsCount,
                    onToggleDrawer = {
                        scope.launch {
                            if (drawerState.isOpen) drawerState.close() else drawerState.open()
                        }
                    },
                    onToggleSound = { viewModel.toggleSound() },
                    onOpenNotifications = { viewModel.showNotificationsDialog.value = true },
                    onOpenAdminPanel = { navState = MainNavigationState.ADMIN_PANEL },
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
}
