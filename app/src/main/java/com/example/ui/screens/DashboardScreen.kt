package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DeviceStatus
import com.example.data.model.NetworkDeviceEntity
import com.example.data.model.RouterEntity
import com.example.ui.ScreenTab
import com.example.ui.TalalProViewModel
import com.example.ui.dialogs.BackgroundMonitorControlCard
import com.example.ui.dialogs.BackgroundMonitorSettingsDialog
import com.example.ui.theme.*

@Composable
fun DashboardScreen(
    viewModel: TalalProViewModel,
    modifier: Modifier = Modifier
) {
    val routers by viewModel.allRouters.collectAsState()
    val devices by viewModel.allDevices.collectAsState()
    val totalDevices by viewModel.totalDevicesCount.collectAsState()
    val onlineDevices by viewModel.onlineDevicesCount.collectAsState()
    val offlineDevices by viewModel.offlineDevicesCount.collectAsState()
    val maintenanceDevices by viewModel.maintenanceDevicesCount.collectAsState()
    val routersCount by viewModel.routersCount.collectAsState()
    val onlineRoutersCount by viewModel.onlineRoutersCount.collectAsState()

    var showHelpAccordion by remember { mutableStateOf(false) }
    var showBgMonitorSettings by remember { mutableStateOf(false) }

    val activeUsersTotal = routers.sumOf { it.activeHotspotUsers + it.activePppoeUsers }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0B132B))
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(vertical = 14.dp)
    ) {
        // 1. Main Header Card (Matches Screenshot 1)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF1E293B))),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Title & Subtitle
                        Column {
                            Text(
                                text = "لوحة القيادة",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "نظرة عامة مباشرة على الشبكة والأجهزة",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }

                        // ECG / Pulse Icon
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF064E3B))
                        ) {
                            Icon(Icons.Default.ShowChart, contentDescription = null, tint = Color(0xFF34D399), modifier = Modifier.size(22.dp))
                        }
                    }

                    // Status Pill "الشبكة مستقرة"
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (offlineDevices > 0) Color(0xFF7F1D1D) else Color(0xFF064E3B))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (offlineDevices > 0) Color(0xFFEF4444) else Color(0xFF34D399))
                        )
                        Text(
                            text = if (offlineDevices > 0) "يوجد أجهزة متوقفة ($offlineDevices)" else "الشبكة مستقرة",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // Stat Badges (0 راوترات, 0 أجهزة, 0 متصل الآن)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickBadge(
                            text = "$routersCount راوترات",
                            icon = Icons.Default.Dns,
                            bgColor = Color(0xFF1E3A8A),
                            tint = Color(0xFF93C5FD),
                            modifier = Modifier.weight(1f)
                        )
                        QuickBadge(
                            text = "$totalDevices أجهزة",
                            icon = Icons.Default.Devices,
                            bgColor = Color(0xFF064E3B),
                            tint = Color(0xFF6EE7B7),
                            modifier = Modifier.weight(1f)
                        )
                        QuickBadge(
                            text = "$onlineDevices متصل الآن",
                            icon = Icons.Default.Language,
                            bgColor = Color(0xFF3B0764),
                            tint = Color(0xFFE9D5FF),
                            modifier = Modifier.weight(1.1f)
                        )
                    }
                }
            }
        }

        // 2. "شرح الاستخدام" Collapsible Accordion (Matches Screenshot 1)
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF1E293B))),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showHelpAccordion = !showHelpAccordion }
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.HelpOutline, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                            Text(
                                text = "شرح الاستخدام",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF38BDF8)
                            )
                        }
                        Icon(
                            imageVector = if (showHelpAccordion) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8)
                        )
                    }

                    AnimatedVisibility(visible = showHelpAccordion) {
                        Column(
                            modifier = Modifier.padding(top = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "1. انتقل إلى تبويب «الراوترات» وأضف راوترك عبر عنوان MikroTik Cloud DDNS أو IP العام.",
                                fontSize = 11.sp,
                                color = Color(0xFFCBD5E1)
                            )
                            Text(
                                text = "2. استخدم زر «استيراد النيبرز» في تبويب «أجهزة الشبكة» لسحب كل الأجهزة والأبراج تلقائياً.",
                                fontSize = 11.sp,
                                color = Color(0xFFCBD5E1)
                            )
                            Text(
                                text = "3. راقب حالات الاتصال والإنذارات الفورية مع الأصوات التنبيهية في الخلفية.",
                                fontSize = 11.sp,
                                color = Color(0xFFCBD5E1)
                            )
                        }
                    }
                }
            }
        }

        // Background Monitoring & Luxury Notification Card
        item {
            BackgroundMonitorControlCard(
                routers = routers,
                onOpenSettings = { showBgMonitorSettings = true }
            )
        }

        // 3. 3 Grid Metric Cards (Matches Screenshot 1)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Total Devices Card
                MetricCard(
                    title = "إجمالي الأجهزة",
                    value = "$totalDevices",
                    subtitle = "$onlineDevices متصل",
                    icon = Icons.Default.ShowChart,
                    iconBg = Color(0xFF064E3B),
                    iconTint = Color(0xFF34D399),
                    modifier = Modifier.weight(1f)
                )

                // Active in Routers Card
                MetricCard(
                    title = "أكتف في الراوترات",
                    value = "$activeUsersTotal",
                    subtitle = "من $routersCount راوتر",
                    icon = Icons.Default.Wifi,
                    iconBg = Color(0xFF1E3A8A),
                    iconTint = Color(0xFF38BDF8),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // MikroTik Servers Card
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF1E293B))),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF1E3A8A))
                    ) {
                        Icon(Icons.Default.Dns, contentDescription = null, tint = Color(0xFF93C5FD), modifier = Modifier.size(24.dp))
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "$routersCount",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8)
                        )
                        Text(
                            text = "سيرفرات MikroTik",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }
        }

        // NEW LUXURY SECTION & BUTTON: "المنافذ وأجهزة الشبكة"
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF0284C7))),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.setTab(ScreenTab.PORTS_AND_DEVICES) }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF0369A1).copy(alpha = 0.25f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Hub,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "المنافذ وأجهزة الشبكة",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFF0284C7)
                                    ) {
                                        Text(
                                            text = "قسم جديد ⚡",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "مراقبة قطع التوزيع، المنافذ المتصلة، واستهلاك الإنترنت",
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8)
                        )
                    }

                    // 3 Feature Badges in the card
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF1E293B),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("قطع التوزيع", fontSize = 10.sp, color = Color(0xFF94A3B8))
                                Text("الأبراج والموزعات", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF1E293B),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("المنافذ والكيابل", fontSize = 10.sp, color = Color(0xFF94A3B8))
                                Text("تحكم وفحص مباشر", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF34D399))
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF1E293B),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("استهلاك الإنترنت", fontSize = 10.sp, color = Color(0xFF94A3B8))
                                Text("لكل قطعة ومنفذ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFBBF24))
                            }
                        }
                    }

                    Button(
                        onClick = { viewModel.setTab(ScreenTab.PORTS_AND_DEVICES) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(42.dp)
                    ) {
                        Icon(Icons.Default.DashboardCustomize, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("فتح قسم المنافذ وأجهزة الشبكة", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        // Quick Access Card: Add Cards & Batch Printing
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF6E1423))),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.setTab(ScreenTab.ADD_CARDS) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF6E1423).copy(alpha = 0.35f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddCard,
                                contentDescription = "إضافة كروت",
                                tint = Color(0xFFFDA4AF),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "قسم إضافة كروت وتوليد الدفعات",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "توليد، استيراد، مزامنة الباقات وطباعة PDF بالقالب المصمم",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    Button(
                        onClick = { viewModel.setTab(ScreenTab.ADD_CARDS) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6E1423)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text("فتح", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        // 4. "حالة الأجهزة" Section with Progress Bars (Matches Screenshot 1)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF1E293B))),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "حالة الأجهزة",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    // Online Devices Bar
                    DeviceStatusBar(
                        label = "متصل",
                        count = onlineDevices,
                        total = if (totalDevices > 0) totalDevices else 1,
                        color = Color(0xFF10B981),
                        icon = Icons.Default.Wifi
                    )

                    // Offline Devices Bar
                    DeviceStatusBar(
                        label = "غير متصل",
                        count = offlineDevices,
                        total = if (totalDevices > 0) totalDevices else 1,
                        color = Color(0xFFEF4444),
                        icon = Icons.Default.WifiOff
                    )

                    // Maintenance Devices Bar
                    DeviceStatusBar(
                        label = "صيانة",
                        count = maintenanceDevices,
                        total = if (totalDevices > 0) totalDevices else 1,
                        color = Color(0xFFF59E0B),
                        icon = Icons.Default.Build
                    )
                }
            }
        }

        // 5. "أحدث الأجهزة" Section (Matches Screenshot 1)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF1E293B))),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "أحدث الأجهزة",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        TextButton(onClick = { viewModel.setTab(ScreenTab.DEVICES) }) {
                            Text("عرض الكل", fontSize = 11.sp, color = Color(0xFF38BDF8))
                        }
                    }

                    if (devices.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "لا توجد أجهزة مسجلة — اضغط زر + لإضافة جهاز جديد",
                                fontSize = 12.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    } else {
                        devices.take(4).forEach { dev ->
                            DashboardDeviceItem(
                                device = dev,
                                onPing = { viewModel.pingDevice(dev) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showBgMonitorSettings) {
        BackgroundMonitorSettingsDialog(
            routers = routers,
            onDismiss = { showBgMonitorSettings = false }
        )
    }
}

@Composable
fun QuickBadge(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    bgColor: Color,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bgColor,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(13.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = text, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconBg: Color,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF1E293B))),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(iconBg)
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
                }
                Text(
                    text = value,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF38BDF8)
                )
            }

            Column {
                Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(text = subtitle, fontSize = 10.sp, color = Color(0xFF34D399))
            }
        }
    }
}

@Composable
fun DeviceStatusBar(
    label: String,
    count: Int,
    total: Int,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
                Text(text = label, fontSize = 12.sp, color = Color(0xFFCBD5E1))
            }
            Text(text = "$count", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
        }

        val progress = (count.toFloat() / total.toFloat()).coerceIn(0f, 1f)
        LinearProgressIndicator(
            progress = { if (count == 0) 0f else progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = Color(0xFF1E293B)
        )
    }
}

@Composable
fun DashboardDeviceItem(
    device: NetworkDeviceEntity,
    onPing: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF1E293B),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (device.status == DeviceStatus.ONLINE) Color(0xFF10B981) else Color(0xFFEF4444))
                )
                Column {
                    Text(text = device.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(text = device.ip, fontSize = 11.sp, color = Color(0xFF94A3B8))
                }
            }

            IconButton(onClick = onPing, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Refresh, contentDescription = "فحص", tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
            }
        }
    }
}
