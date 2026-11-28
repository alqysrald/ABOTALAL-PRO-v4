package com.example.ui.components

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.firebase.FirebaseUserAccount
import com.example.service.RouterMonitorManager
import com.example.ui.ScreenTab
import com.example.ui.TalalProViewModel
import com.example.ui.dialogs.BackgroundMonitorSettingsDialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SideDrawerContent(
    viewModel: TalalProViewModel,
    onNavigate: (ScreenTab) -> Unit,
    onNavigateToAdmin: () -> Unit,
    onOpenPowerControl: () -> Unit,
    onCloseDrawer: () -> Unit
) {
    val isAdmin by viewModel.isAdmin.collectAsState()
    val isGuestModeActive by viewModel.isGuestModeActive.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val currentTab by viewModel.currentTab.collectAsState()
    val routers by viewModel.allRouters.collectAsState()
    val devices by viewModel.allDevices.collectAsState()
    val context = LocalContext.current
    val monitorState by RouterMonitorManager.state.collectAsState()
    var showBgSettingsDialog by remember { mutableStateOf(false) }

    val notifPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            RouterMonitorManager.startService(context)
            Toast.makeText(context, "تم تفعيل إشعار الخلفية ✓", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "يرجى منح صلاحية الإشعارات لعرض إشعار الحالة", Toast.LENGTH_SHORT).show()
        }
    }

    ModalDrawerSheet(
        drawerContainerColor = Color(0xFF0B132B),
        drawerContentColor = Color.White,
        modifier = Modifier.width(326.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Luxury Brand + Account Header
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF0F172A),
                border = BorderStroke(1.dp, Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF0F172A), Color(0xFF172554))
                            )
                        )
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF38BDF8).copy(alpha = 0.16f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Router,
                                contentDescription = "شعار TALAL PRO",
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "TALAL PRO",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF10B981).copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "v4.2 Pro",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF34D399),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "منظومة إدارة المايكروتك الشاملة",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    UserProfileDrawerCard(
                        currentUser = currentUser,
                        isAdmin = isAdmin,
                        isGuestMode = isGuestModeActive,
                        routerCount = routers.size,
                        deviceCount = devices.size
                    )
                }
            }

            // ADMIN CONTROL PANEL BUTTON (Lives cleanly inside the Side Drawer)
            if (isAdmin) {
                Surface(
                    onClick = {
                        onCloseDrawer()
                        onNavigateToAdmin()
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF450A0A).copy(alpha = 0.7f),
                    border = BorderStroke(1.2.dp, Color(0xFFEF4444).copy(alpha = 0.7f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("drawer_admin_panel_button")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFEF4444),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AdminPanelSettings,
                                    contentDescription = "لوحة التحكم",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "لوحة تحكم المدير العام (Admin)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFEE2E2)
                            )
                            Text(
                                text = "المستخدمين، التفعيل، الوضع المفتوح، والذكاء",
                                fontSize = 10.sp,
                                color = Color(0xFFFCA5A5)
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = null,
                            tint = Color(0xFFFCA5A5)
                        )
                    }
                }
            }

            // SECTION 1: القيادة والمراقبة الحية
            DrawerSectionCard(title = "القيادة والمراقبة الحية", accentColor = Color(0xFF38BDF8)) {
                DrawerNavTabItem(
                    icon = Icons.Default.GridView,
                    title = "لوحة القيادة المباشرة",
                    isSelected = currentTab == ScreenTab.DASHBOARD,
                    badge = "${routers.size} راوتر",
                    accentColor = Color(0xFF38BDF8),
                    onClick = { onNavigate(ScreenTab.DASHBOARD); onCloseDrawer() }
                )
                DrawerNavTabItem(
                    icon = Icons.Default.Wifi,
                    title = "المتصلين الآن (الرصيد والوقت)",
                    isSelected = currentTab == ScreenTab.ACTIVE_USERS,
                    badge = "دقيق 🟢",
                    accentColor = Color(0xFF10B981),
                    onClick = { onNavigate(ScreenTab.ACTIVE_USERS); onCloseDrawer() }
                )
                DrawerNavTabItem(
                    icon = Icons.Default.Dns,
                    title = "راوترات MikroTik",
                    isSelected = currentTab == ScreenTab.ROUTERS,
                    badge = null,
                    accentColor = Color(0xFF38BDF8),
                    onClick = { onNavigate(ScreenTab.ROUTERS); onCloseDrawer() }
                )
                DrawerNavTabItem(
                    icon = Icons.Default.Shield,
                    title = "تشخيص ومشاكل الشبكة",
                    isSelected = currentTab == ScreenTab.ISSUES,
                    badge = null,
                    accentColor = Color(0xFFF59E0B),
                    onClick = { onNavigate(ScreenTab.ISSUES); onCloseDrawer() }
                )
            }

            // SECTION 2: إدارة الكروت والباقات
            DrawerSectionCard(title = "الكروت والباقات والمشتركين", accentColor = Color(0xFFF59E0B)) {
                DrawerNavTabItem(
                    icon = Icons.Default.AddCard,
                    title = "إضافة كروت (توليد ورفع PDF)",
                    isSelected = currentTab == ScreenTab.ADD_CARDS,
                    badge = "الدفعات",
                    accentColor = Color(0xFFF43F5E),
                    onClick = { onNavigate(ScreenTab.ADD_CARDS); onCloseDrawer() }
                )
                DrawerNavTabItem(
                    icon = Icons.Default.Style,
                    title = "مصمم وقوالب الكروت",
                    isSelected = currentTab == ScreenTab.CARD_DESIGNER,
                    badge = "PDF",
                    accentColor = Color(0xFFA855F7),
                    onClick = { onNavigate(ScreenTab.CARD_DESIGNER); onCloseDrawer() }
                )
                DrawerNavTabItem(
                    icon = Icons.Default.CreditCard,
                    title = "اليوزر مانجر (User Manager)",
                    isSelected = currentTab == ScreenTab.USER_MANAGER,
                    badge = "كروت وباقات",
                    accentColor = Color(0xFF38BDF8),
                    onClick = { onNavigate(ScreenTab.USER_MANAGER); onCloseDrawer() }
                )
                DrawerNavTabItem(
                    icon = Icons.Default.WifiTethering,
                    title = "الهوت سبوت (Hotspot)",
                    isSelected = currentTab == ScreenTab.HOTSPOT,
                    badge = "جلسات وباقات",
                    accentColor = Color(0xFFF59E0B),
                    onClick = { onNavigate(ScreenTab.HOTSPOT); onCloseDrawer() }
                )
            }

            // SECTION 3: البنية التحتية والأدوات المميزة
            DrawerSectionCard(title = "الأدوات المميزة والبنية التحتية", accentColor = Color(0xFF6366F1)) {
                DrawerNavTabItem(
                    icon = Icons.Default.Lan,
                    title = "الأدوات المميزة الشاملة",
                    isSelected = currentTab == ScreenTab.TOOLS,
                    badge = "شامل ⚡",
                    accentColor = Color(0xFF818CF8),
                    onClick = { onNavigate(ScreenTab.TOOLS); onCloseDrawer() }
                )
                DrawerNavTabItem(
                    icon = Icons.Default.SettingsEthernet,
                    title = "المنافذ وأجهزة التوزيع",
                    isSelected = currentTab == ScreenTab.PORTS_AND_DEVICES,
                    badge = "مراقبة",
                    accentColor = Color(0xFF06B6D4),
                    onClick = { onNavigate(ScreenTab.PORTS_AND_DEVICES); onCloseDrawer() }
                )
                DrawerNavTabItem(
                    icon = Icons.Default.Hub,
                    title = "أجهزة الشبكة والأبراج",
                    isSelected = currentTab == ScreenTab.DEVICES,
                    badge = "${devices.size}",
                    accentColor = Color(0xFF38BDF8),
                    onClick = { onNavigate(ScreenTab.DEVICES); onCloseDrawer() }
                )
                DrawerNavTabItem(
                    icon = Icons.Default.Map,
                    title = "خريطة الشبكة والتوبولوجي",
                    isSelected = currentTab == ScreenTab.MAP,
                    badge = null,
                    accentColor = Color(0xFF10B981),
                    onClick = { onNavigate(ScreenTab.MAP); onCloseDrawer() }
                )
            }

            // SECTION 4: المساعد والأتمتة
            DrawerSectionCard(title = "المساعد الذكي وتليجرام", accentColor = Color(0xFF10B981)) {
                DrawerNavTabItem(
                    icon = Icons.Default.SmartToy,
                    title = "المساعد الشبكي (كامل الصلاحيات)",
                    isSelected = currentTab == ScreenTab.ASSISTANT,
                    badge = "CLI ⚡",
                    accentColor = Color(0xFF38BDF8),
                    onClick = { onNavigate(ScreenTab.ASSISTANT); onCloseDrawer() }
                )
                DrawerNavTabItem(
                    icon = Icons.Default.Send,
                    title = "ربط وتنبيهات تليجرام",
                    isSelected = currentTab == ScreenTab.TELEGRAM,
                    badge = null,
                    accentColor = Color(0xFF38BDF8),
                    onClick = { onNavigate(ScreenTab.TELEGRAM); onCloseDrawer() }
                )
                DrawerNavTabItem(
                    icon = Icons.Default.CloudSync,
                    title = "بوت تليجرام المتقدم (Pro)",
                    isSelected = currentTab == ScreenTab.ADVANCED_TELEGRAM,
                    badge = "سحابي",
                    accentColor = Color(0xFF10B981),
                    onClick = { onNavigate(ScreenTab.ADVANCED_TELEGRAM); onCloseDrawer() }
                )
            }

            // SECTION 5: إجراءات سريعة وإدارة الحساب
            DrawerSectionCard(title = "إجراءات سريعة والنظام", accentColor = Color(0xFF94A3B8)) {
                DrawerActionItem(
                    icon = Icons.Default.PowerSettingsNew,
                    title = "إعادة تشغيل / إيقاف الراوتر",
                    tint = Color(0xFFFBBF24),
                    onClick = {
                        onCloseDrawer()
                        onOpenPowerControl()
                    }
                )
                DrawerNavTabItem(
                    icon = Icons.Default.NotificationsActive,
                    title = "العمل بالخلفية وإشعار الحالة",
                    isSelected = monitorState.isServiceRunning,
                    badge = if (monitorState.isServiceRunning) "نشط 🟢" else "متوقف",
                    accentColor = if (monitorState.isServiceRunning) Color(0xFF10B981) else Color(0xFF64748B),
                    onClick = {
                        if (monitorState.isServiceRunning) {
                            showBgSettingsDialog = true
                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            !RouterMonitorManager.hasNotificationPermission(context)
                        ) {
                            notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            RouterMonitorManager.startService(context)
                            Toast.makeText(context, "تم تشغيل إشعار الخلفية ✓", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                DrawerActionItem(
                    icon = Icons.Default.Add,
                    title = "إضافة راوتر جديد",
                    tint = Color(0xFF38BDF8),
                    onClick = { onCloseDrawer(); viewModel.showAddRouterDialog.value = true }
                )
                DrawerActionItem(
                    icon = Icons.Default.AddCircleOutline,
                    title = "إضافة برج أو جهاز",
                    tint = Color(0xFF38BDF8),
                    onClick = { onCloseDrawer(); viewModel.showAddDeviceDialog.value = true }
                )
                DrawerActionItem(
                    icon = Icons.Default.Sync,
                    title = "فحص ومزامنة الشبكة الآن",
                    tint = Color(0xFF10B981),
                    onClick = { onCloseDrawer(); viewModel.refreshAll() }
                )
                DrawerActionItem(
                    icon = Icons.Default.Cloud,
                    title = "دليل ربط MikroTik Cloud",
                    tint = Color(0xFF818CF8),
                    onClick = { onCloseDrawer(); viewModel.showCloudGuideDialog.value = true }
                )
                DrawerActionItem(
                    icon = if (isGuestModeActive) Icons.Default.Login else Icons.Default.Logout,
                    title = if (isGuestModeActive) "تسجيل الدخول بحساب مشرف / تبديل الحساب" else "تسجيل الخروج",
                    tint = Color(0xFFF87171),
                    onClick = {
                        onCloseDrawer()
                        viewModel.logout()
                    }
                )
            }

            // Developer Info Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF0F172A),
                border = BorderStroke(1.dp, Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "إعداد وتصميم وتطوير المنظومة:",
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8)
                    )
                    Text(
                        text = "عبدالحميد داوؤد",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.Phone,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "778215553 - 700897775",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8)
                        )
                    }
                }
            }
        }
    }

    if (showBgSettingsDialog) {
        BackgroundMonitorSettingsDialog(
            routers = routers,
            onDismiss = { showBgSettingsDialog = false }
        )
    }
}

@Composable
private fun DrawerSectionCard(
    title: String,
    accentColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF0F172A),
        border = BorderStroke(1.dp, Color(0xFF1E293B)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 3.dp, height = 14.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(accentColor)
                )
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
            }
            content()
        }
    }
}

@Composable
private fun UserProfileDrawerCard(
    currentUser: FirebaseUserAccount?,
    isAdmin: Boolean,
    isGuestMode: Boolean,
    routerCount: Int,
    deviceCount: Int
) {
    if (isGuestMode) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF0284C7).copy(alpha = 0.16f),
            border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "وضع التشغيل المفتوح",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF10B981)
                    ) {
                        Text(
                            text = "بدون تسجيل",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = "📡 $routerCount راوتر مسجل • 🏢 $deviceCount جهاز",
                    fontSize = 10.sp,
                    color = Color(0xFFCBD5E1)
                )
            }
        }
        return
    }

    if (currentUser == null) return

    val remainingDays = computeRemainingDays(currentUser)

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.Black.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, Color(0xFF334155)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.AccountCircle, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                    Text(
                        text = currentUser.name.ifBlank { currentUser.email.substringBefore("@") },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isAdmin) Color(0xFFEF4444) else Color(0xFF10B981)
                ) {
                    Text(
                        text = if (isAdmin) "مدير عام" else if (currentUser.isLifetime) "دائم" else "نشط",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                text = currentUser.email,
                fontSize = 10.sp,
                color = Color(0xFF94A3B8),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "صلاحية الاشتراك:",
                        fontSize = 9.sp,
                        color = Color(0xFF94A3B8)
                    )
                    val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.ENGLISH)
                    val expFormatted = if (currentUser.isLifetime) {
                        "صلاحية مفتوحة (دائم)"
                    } else if (currentUser.expiryTimestamp > 0) {
                        sdf.format(Date(currentUser.expiryTimestamp))
                    } else {
                        "غير محدد"
                    }
                    Text(
                        text = expFormatted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (currentUser.isLifetime || (remainingDays != null && remainingDays > 7)) Color(0xFF059669) else Color(0xFFDC2626)
                ) {
                    Text(
                        text = when {
                            currentUser.isLifetime -> "غير محدود"
                            remainingDays != null && remainingDays > 0 -> "متبقي $remainingDays يوم"
                            remainingDays != null && remainingDays == 0 -> "ينتهي اليوم"
                            remainingDays != null && remainingDays < 0 -> "منتهي"
                            else -> "نشط"
                        },
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "📡 $routerCount راوتر مسجل",
                    fontSize = 9.sp,
                    color = Color(0xFFCBD5E1)
                )
                Text(text = "•", fontSize = 9.sp, color = Color(0xFF64748B))
                Text(
                    text = "🏢 $deviceCount برج وجهاز",
                    fontSize = 9.sp,
                    color = Color(0xFFCBD5E1)
                )
            }
        }
    }
}

private fun computeRemainingDays(user: FirebaseUserAccount): Int? {
    if (user.isLifetime) return 9999
    if (user.expiryTimestamp <= 0L) return null
    val diff = user.expiryTimestamp - System.currentTimeMillis()
    return (diff / (1000L * 60L * 60L * 24L)).toInt()
}

@Composable
private fun DrawerNavTabItem(
    icon: ImageVector,
    title: String,
    isSelected: Boolean,
    badge: String? = null,
    accentColor: Color = Color(0xFF38BDF8),
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) accentColor.copy(alpha = 0.16f) else Color.Transparent,
        border = if (isSelected) BorderStroke(1.dp, accentColor.copy(alpha = 0.5f)) else null,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) accentColor.copy(alpha = 0.25f) else Color(0xFF1E293B))
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = if (isSelected) accentColor else Color(0xFFCBD5E1),
                    modifier = Modifier.size(17.dp)
                )
            }
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else Color(0xFFE2E8F0),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (badge != null) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = accentColor.copy(alpha = 0.18f)
                ) {
                    Text(
                        text = badge,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DrawerActionItem(
    icon: ImageVector,
    title: String,
    tint: Color = Color(0xFF38BDF8),
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(tint.copy(alpha = 0.14f))
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = tint,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = if (tint == Color(0xFFF87171)) tint else Color(0xFFE2E8F0),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
