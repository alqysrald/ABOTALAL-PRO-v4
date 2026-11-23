package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.firebase.FirebaseUserAccount
import com.example.ui.ScreenTab
import com.example.ui.TalalProViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@Composable
fun SideDrawerContent(
    viewModel: TalalProViewModel,
    onNavigate: (ScreenTab) -> Unit,
    onNavigateToAdmin: () -> Unit,
    onCloseDrawer: () -> Unit
) {
    val isAdmin by viewModel.isAdmin.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val currentTab by viewModel.currentTab.collectAsState()
    val routers by viewModel.allRouters.collectAsState()
    val devices by viewModel.allDevices.collectAsState()

    ModalDrawerSheet(
        drawerContainerColor = Color(0xFFF8FAFC),
        drawerContentColor = TextPrimaryLight,
        modifier = Modifier.width(320.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(PrimaryNavy, PrimaryBlue)
                        )
                    )
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f))
                        ) {
                            Icon(
                                Icons.Default.Router,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "TALAL PRO",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Text(
                                text = "الإصدار v4.0 Pro - نظام إدارة المايكروتك الشامل",
                                fontSize = 11.sp,
                                color = Color(0xFFDBEAFE)
                            )
                        }
                    }

                    // User Profile Detail Card in Side Drawer
                    UserProfileDrawerCard(currentUser = currentUser, isAdmin = isAdmin, routerCount = routers.size, deviceCount = devices.size)
                }
            }

            // ADMIN CONTROL PANEL BUTTON (Appears ONLY when logged in as Admin)
            if (isAdmin) {
                Surface(
                    onClick = {
                        onCloseDrawer()
                        onNavigateToAdmin()
                    },
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFFEE2E2),
                    border = BorderStroke(1.2.dp, Color(0xFFFCA5A5)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFEF4444),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AdminPanelSettings,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "لوحة تحكم المدير العام (Admin)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF991B1B)
                            )
                            Text(
                                text = "المستخدمين، الأجهزة، تمديد الاشتراك، إعدادات AI",
                                fontSize = 10.sp,
                                color = Color(0xFFB91C1C)
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = null,
                            tint = Color(0xFFEF4444)
                        )
                    }
                }
            }

            Text(
                text = "الأقسام الرئيسية والخدمات",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondaryLight,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )

            // Main Nav Items
            DrawerNavTabItem(
                icon = Icons.Default.GridView,
                title = "لوحة القيادة المباشرة",
                isSelected = currentTab == ScreenTab.DASHBOARD,
                badge = "${routers.size} راوتر",
                onClick = {
                    onNavigate(ScreenTab.DASHBOARD)
                    onCloseDrawer()
                }
            )

            DrawerNavTabItem(
                icon = Icons.Default.Dns,
                title = "راوترات MikroTik",
                isSelected = currentTab == ScreenTab.ROUTERS,
                badge = null,
                onClick = {
                    onNavigate(ScreenTab.ROUTERS)
                    onCloseDrawer()
                }
            )

            // USER MANAGER SECTION (As Requested)
            DrawerNavTabItem(
                icon = Icons.Default.CreditCard,
                title = "قسم اليوزر مانجر (User Manager)",
                isSelected = currentTab == ScreenTab.USER_MANAGER,
                badge = "كروت وباقات",
                badgeColor = PrimaryBlue,
                onClick = {
                    onNavigate(ScreenTab.USER_MANAGER)
                    onCloseDrawer()
                }
            )

            // HOTSPOT SECTION (As Requested)
            DrawerNavTabItem(
                icon = Icons.Default.WifiTethering,
                title = "قسم الهوت سبوت (Hotspot)",
                isSelected = currentTab == ScreenTab.HOTSPOT,
                badge = "مستخدمين وجلسات",
                badgeColor = Color(0xFFEA580C),
                onClick = {
                    onNavigate(ScreenTab.HOTSPOT)
                    onCloseDrawer()
                }
            )

            // ACTIVE CONNECTED USERS & DEVICES
            DrawerNavTabItem(
                icon = Icons.Default.Wifi,
                title = "المتصلين الآن (Active Devices)",
                isSelected = currentTab == ScreenTab.ACTIVE_USERS,
                badge = "مباشر 🟢",
                badgeColor = Color(0xFF16A34A),
                onClick = {
                    onNavigate(ScreenTab.ACTIVE_USERS)
                    onCloseDrawer()
                }
            )

            // ADD CARDS & BATCH GENERATION
            DrawerNavTabItem(
                icon = Icons.Default.AddCard,
                title = "قسم إضافة كروت (الدفعات)",
                isSelected = currentTab == ScreenTab.ADD_CARDS,
                badge = "توليد وطباعة 🎫",
                badgeColor = Color(0xFF6E1423),
                onClick = {
                    onNavigate(ScreenTab.ADD_CARDS)
                    onCloseDrawer()
                }
            )

            // CARD DESIGNER & PDF PRINTING
            DrawerNavTabItem(
                icon = Icons.Default.Style,
                title = "مصمم وقوالب الكروت (Card Designer)",
                isSelected = currentTab == ScreenTab.CARD_DESIGNER,
                badge = "طباعة PDF",
                badgeColor = Color(0xFF8B5CF6),
                onClick = {
                    onNavigate(ScreenTab.CARD_DESIGNER)
                    onCloseDrawer()
                }
            )

            DrawerNavTabItem(
                icon = Icons.Default.Hub,
                title = "أجهزة الشبكة والأبراج",
                isSelected = currentTab == ScreenTab.DEVICES,
                badge = "${devices.size} جهاز",
                onClick = {
                    onNavigate(ScreenTab.DEVICES)
                    onCloseDrawer()
                }
            )

            // PORTS & NETWORK DISTRIBUTION SECTION (New)
            DrawerNavTabItem(
                icon = Icons.Default.SettingsEthernet,
                title = "المنافذ وأجهزة الشبكة",
                isSelected = currentTab == ScreenTab.PORTS_AND_DEVICES,
                badge = "توزيع ومنافذ ⚡",
                badgeColor = Color(0xFF0284C7),
                onClick = {
                    onNavigate(ScreenTab.PORTS_AND_DEVICES)
                    onCloseDrawer()
                }
            )

            DrawerNavTabItem(
                icon = Icons.Default.Map,
                title = "خريطة الشبكة والتوبولوجي",
                isSelected = currentTab == ScreenTab.MAP,
                badge = null,
                onClick = {
                    onNavigate(ScreenTab.MAP)
                    onCloseDrawer()
                }
            )

            DrawerNavTabItem(
                icon = Icons.Default.Lan,
                title = "أدوات مميزة (VLANs & Log)",
                isSelected = currentTab == ScreenTab.TOOLS,
                badge = "VLANs • Log ⚡",
                badgeColor = Color(0xFF6366F1),
                onClick = {
                    onNavigate(ScreenTab.TOOLS)
                    onCloseDrawer()
                }
            )

            DrawerNavTabItem(
                icon = Icons.Default.Send,
                title = "ربط وتنبيهات تليجرام (Telegram)",
                isSelected = currentTab == ScreenTab.TELEGRAM,
                badge = "بوت وتنبيهات",
                badgeColor = Color(0xFF0284C7),
                onClick = {
                    onNavigate(ScreenTab.TELEGRAM)
                    onCloseDrawer()
                }
            )

            DrawerNavTabItem(
                icon = Icons.Default.CloudSync,
                title = "بوت تليجرام المتقدم (Pro Cloud)",
                isSelected = currentTab == ScreenTab.ADVANCED_TELEGRAM,
                badge = "مستقل ⚡",
                badgeColor = Color(0xFF10B981),
                onClick = {
                    onNavigate(ScreenTab.ADVANCED_TELEGRAM)
                    onCloseDrawer()
                }
            )

            DrawerNavTabItem(
                icon = Icons.Default.SmartToy,
                title = "المساعد الشبكي الذكي (AI)",
                isSelected = currentTab == ScreenTab.ASSISTANT,
                badge = null,
                onClick = {
                    onNavigate(ScreenTab.ASSISTANT)
                    onCloseDrawer()
                }
            )

            DrawerNavTabItem(
                icon = Icons.Default.Shield,
                title = "تشخيص ومشاكل الشبكة",
                isSelected = currentTab == ScreenTab.ISSUES,
                badge = null,
                onClick = {
                    onNavigate(ScreenTab.ISSUES)
                    onCloseDrawer()
                }
            )

            HorizontalDivider(color = BorderLight, modifier = Modifier.padding(vertical = 4.dp))

            Text(
                text = "إجراءات سريعة",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondaryLight,
                modifier = Modifier.padding(start = 4.dp)
            )

            DrawerActionItem(
                icon = Icons.Default.Cloud,
                title = "دليل ربط MikroTik Cloud",
                onClick = {
                    onCloseDrawer()
                    viewModel.showCloudGuideDialog.value = true
                }
            )

            DrawerActionItem(
                icon = Icons.Default.Add,
                title = "إضافة راوتر جديد",
                onClick = {
                    onCloseDrawer()
                    viewModel.showAddRouterDialog.value = true
                }
            )

            DrawerActionItem(
                icon = Icons.Default.AddCircleOutline,
                title = "إضافة برج أو جهاز",
                onClick = {
                    onCloseDrawer()
                    viewModel.showAddDeviceDialog.value = true
                }
            )

            DrawerActionItem(
                icon = Icons.Default.Sync,
                title = "فحص ومزامنة الشبكة الآن",
                onClick = {
                    onCloseDrawer()
                    viewModel.refreshAll()
                }
            )

            DrawerActionItem(
                icon = Icons.Default.Logout,
                title = "تسجيل الخروج",
                color = Color(0xFFEF4444),
                onClick = {
                    onCloseDrawer()
                    viewModel.logout()
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Developer Info Card (As Requested: عبدالحميد داوؤد - 778215553 - 700897775)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, BorderLight),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "إعداد وتصميم وتطوير المنظومة:",
                        fontSize = 10.sp,
                        color = TextSecondaryLight
                    )
                    Text(
                        text = "عبدالحميد داوؤد",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryNavy
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(13.dp))
                        Text(
                            text = "778215553 - 700897775",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UserProfileDrawerCard(
    currentUser: FirebaseUserAccount?,
    isAdmin: Boolean,
    routerCount: Int,
    deviceCount: Int
) {
    if (currentUser == null) return

    val remainingDays = computeRemainingDays(currentUser)

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.Black.copy(alpha = 0.28f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Name / Email & Admin Badge
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
                    Icon(Icons.Default.AccountCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Text(
                        text = currentUser.name.ifBlank { currentUser.email.substringBefore("@") },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1
                    )
                }

                if (isAdmin) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFFEF4444)
                    ) {
                        Text(
                            text = "مدير عام",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF10B981)
                    ) {
                        Text(
                            text = if (currentUser.isLifetime) "دائم" else "نشط",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Text(
                text = currentUser.email,
                fontSize = 10.sp,
                color = Color(0xFFCBD5E1),
                maxLines = 1
            )

            HorizontalDivider(color = Color.White.copy(alpha = 0.15f))

            // Subscription Details
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
                    val expFormatted = if (currentUser.isLifetime) "صلاحية مفتوحة (دائم)" else if (currentUser.expiryTimestamp > 0) sdf.format(Date(currentUser.expiryTimestamp)) else "غير محدد"
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

            // Stats summary: Routers and Devices
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "📡 $routerCount راوتر مسجل",
                    fontSize = 9.sp,
                    color = Color(0xFFE2E8F0)
                )
                Text(
                    text = "•",
                    fontSize = 9.sp,
                    color = Color(0xFF94A3B8)
                )
                Text(
                    text = "🏢 $deviceCount برج وجهاز",
                    fontSize = 9.sp,
                    color = Color(0xFFE2E8F0)
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
    badgeColor: Color = PrimaryBlue,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) PrimaryBlue.copy(alpha = 0.12f) else Color.Transparent,
        border = if (isSelected) BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.3f)) else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isSelected) PrimaryBlue else PrimaryNavy,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) PrimaryBlue else PrimaryNavy,
                modifier = Modifier.weight(1f)
            )
            if (badge != null) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = badgeColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = badge,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor,
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
    color: Color = PrimaryNavy,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (color == PrimaryNavy) PrimaryBlue else color,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                color = color
            )
        }
    }
}
