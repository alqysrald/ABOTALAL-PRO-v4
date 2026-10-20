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
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DeviceStatus
import com.example.data.model.DeviceType
import com.example.data.model.NetworkDeviceEntity
import com.example.ui.DeviceFilter
import com.example.ui.TalalProViewModel
import com.example.ui.theme.*

@Composable
fun NetworkDevicesScreen(
    viewModel: TalalProViewModel,
    modifier: Modifier = Modifier
) {
    val routers by viewModel.allRouters.collectAsState()
    val allDevices by viewModel.allDevices.collectAsState()
    val filteredDevices by viewModel.filteredDevices.collectAsState()
    val totalDevices by viewModel.totalDevicesCount.collectAsState()
    val onlineDevices by viewModel.onlineDevicesCount.collectAsState()
    val offlineDevices by viewModel.offlineDevicesCount.collectAsState()
    val maintenanceDevices by viewModel.maintenanceDevicesCount.collectAsState()

    val searchQuery by viewModel.deviceSearchQuery.collectAsState()
    val activeFilter by viewModel.deviceFilter.collectAsState()

    var showHelpAccordion by remember { mutableStateOf(false) }

    val issuesCount = offlineDevices + maintenanceDevices

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0B132B))
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 14.dp)
    ) {
        // 1. Header (Matches Screenshot 7)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "أجهزة الشبكة",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "عرض وإدارة جميع الأبراج والمودمات والأجهزة المكتشفة",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1E293B))
                ) {
                    Icon(Icons.Default.DesktopWindows, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(20.dp))
                }
            }
        }

        // 2. 4 Top Stat Boxes (Matches Screenshot 7)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Issues
                DeviceMetricBox(
                    title = "مشاكل الشبكة",
                    count = "$issuesCount",
                    unit = "جهاز",
                    color = Color(0xFFF59E0B),
                    icon = Icons.Default.Warning,
                    modifier = Modifier.weight(1f)
                )
                // Offline
                DeviceMetricBox(
                    title = "غير متصلة",
                    count = "$offlineDevices",
                    unit = "جهاز",
                    color = Color(0xFFEF4444),
                    icon = Icons.Default.Circle,
                    modifier = Modifier.weight(1f)
                )
                // Online
                DeviceMetricBox(
                    title = "متصلة",
                    count = "$onlineDevices",
                    unit = "جهاز",
                    color = Color(0xFF10B981),
                    icon = Icons.Default.Circle,
                    modifier = Modifier.weight(1f)
                )
                // Total
                DeviceMetricBox(
                    title = "إجمالي الأجهزة",
                    count = "$totalDevices",
                    unit = "جهاز",
                    color = Color(0xFF38BDF8),
                    icon = Icons.Default.DesktopWindows,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 3. "شرح الاستخدام" Accordion (Matches Screenshot 7)
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
                                text = "• زر «كشف» / «استيراد النيبرز»: يقوم بجلب الأجهزة المجاورة من راوتر المايكروتيك مباشرة.",
                                fontSize = 11.sp,
                                color = Color(0xFFCBD5E1)
                            )
                            Text(
                                text = "• زر «تفعيل Netwatch»: يرسل الأجهزة المختارة كأهداف مراقبة حية داخل نظام RouterOS.",
                                fontSize = 11.sp,
                                color = Color(0xFFCBD5E1)
                            )
                            Text(
                                text = "• زر «تثبيت ARP»: يجعل عناوين الـ IP و MAC ثابتة في جدول ARP لمنع تعارض العناوين.",
                                fontSize = 11.sp,
                                color = Color(0xFFCBD5E1)
                            )
                        }
                    }
                }
            }
        }

        // 4. Action Buttons Grid (Matches Screenshot 7)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Row 1
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ActionPillButton(
                        text = "كشف",
                        icon = Icons.Default.Search,
                        bgColor = Color(0xFF059669),
                        onClick = { viewModel.showImportNeighborsDialog.value = true },
                        modifier = Modifier.weight(1f)
                    )
                    ActionPillButton(
                        text = "+ إضافة جهاز",
                        bgColor = Color(0xFF1E293B),
                        onClick = { viewModel.showAddDeviceDialog.value = true },
                        modifier = Modifier.weight(1.2f)
                    )
                    ActionPillButton(
                        text = "تحديث Netwatch",
                        icon = Icons.Default.Sync,
                        bgColor = Color(0xFF1E293B),
                        onClick = { viewModel.showNetwatchSyncDialog.value = true },
                        modifier = Modifier.weight(1.4f)
                    )
                    ActionPillButton(
                        text = "تفعيل Netwatch",
                        icon = Icons.Default.Sensors,
                        bgColor = Color(0xFF1E3A8A),
                        onClick = { viewModel.showNetwatchSyncDialog.value = true },
                        modifier = Modifier.weight(1.4f)
                    )
                }

                // Row 2
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ActionPillButton(
                        text = "مراقبة",
                        icon = Icons.Default.NotificationsOff,
                        bgColor = Color(0xFF1E293B),
                        onClick = { viewModel.refreshAll() },
                        modifier = Modifier.weight(1f)
                    )
                    ActionPillButton(
                        text = "تحديث",
                        icon = Icons.Default.Sync,
                        bgColor = Color(0xFF1E293B),
                        onClick = { viewModel.refreshAll() },
                        modifier = Modifier.weight(1f)
                    )
                    ActionPillButton(
                        text = "تثبيت ARP",
                        icon = Icons.Default.Lock,
                        bgColor = Color(0xFF064E3B),
                        onClick = {
                            val r = routers.firstOrNull()
                            val d = allDevices.firstOrNull()
                            if (r != null && d != null) {
                                viewModel.makeArpStatic(r, d)
                            }
                        },
                        modifier = Modifier.weight(1.2f)
                    )
                    ActionPillButton(
                        text = "استيراد النيبرز",
                        icon = Icons.Default.Hub,
                        bgColor = Color(0xFF4C1D95),
                        onClick = { viewModel.showImportNeighborsDialog.value = true },
                        modifier = Modifier.weight(1.3f)
                    )
                }

                // Row 3
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ActionPillButton(
                        text = "IP ↓",
                        bgColor = Color(0xFF1E3A8A),
                        onClick = { viewModel.refreshAll() },
                        modifier = Modifier.weight(1f)
                    )
                    ActionPillButton(
                        text = "تحديد",
                        icon = Icons.Default.CheckBox,
                        bgColor = Color(0xFF451A03),
                        onClick = {},
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 5. Search Bar (Matches Screenshot 7)
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.deviceSearchQuery.value = it },
                placeholder = { Text("بحث بالاسم / IP / المسؤول / الراوتر...", color = Color(0xFF64748B), fontSize = 12.sp) },
                trailingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF94A3B8)) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color(0xFF111D36),
                    unfocusedContainerColor = Color(0xFF111D36),
                    focusedBorderColor = Color(0xFF38BDF8),
                    unfocusedBorderColor = Color(0xFF1E293B)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("device_search_input")
            )
        }

        // 6. Filter Chips Row (Matches Screenshot 7)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                DeviceFilterChip(
                    label = "الكل ($totalDevices)",
                    isSelected = activeFilter == com.example.ui.DeviceFilter.ALL,
                    onClick = { viewModel.deviceFilter.value = com.example.ui.DeviceFilter.ALL },
                    modifier = Modifier.weight(1f)
                )
                DeviceFilterChip(
                    label = "متصلة ($onlineDevices)",
                    icon = Icons.Default.Check,
                    isSelected = activeFilter == com.example.ui.DeviceFilter.ONLINE,
                    onClick = { viewModel.deviceFilter.value = com.example.ui.DeviceFilter.ONLINE },
                    modifier = Modifier.weight(1.1f)
                )
                DeviceFilterChip(
                    label = "طافية ($offlineDevices)",
                    icon = Icons.Default.Close,
                    isSelected = activeFilter == com.example.ui.DeviceFilter.OFFLINE,
                    onClick = { viewModel.deviceFilter.value = com.example.ui.DeviceFilter.OFFLINE },
                    modifier = Modifier.weight(1.1f)
                )
                DeviceFilterChip(
                    label = "مشاكل ($issuesCount)",
                    icon = Icons.Default.Warning,
                    isSelected = activeFilter == com.example.ui.DeviceFilter.ISSUES,
                    onClick = { viewModel.deviceFilter.value = com.example.ui.DeviceFilter.ISSUES },
                    modifier = Modifier.weight(1.1f)
                )
            }
        }

        // 7. Device List or Empty State Box (Matches Screenshot 7)
        if (filteredDevices.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF1E293B))),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "لا توجد أجهزة",
                            fontSize = 14.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }
        } else {
            items(filteredDevices) { device ->
                DeviceCardItem(
                    device = device,
                    onPing = { viewModel.pingDevice(device) },
                    onDelete = { viewModel.deleteDevice(device) }
                )
            }
        }
    }
}

@Composable
fun DeviceMetricBox(
    title: String,
    count: String,
    unit: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF1E293B))),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(10.dp))
                Text(text = title, fontSize = 9.sp, color = color, maxLines = 1)
            }
            Text(text = count, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(text = unit, fontSize = 9.sp, color = Color(0xFF64748B))
        }
    }
}

@Composable
fun ActionPillButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    bgColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = bgColor,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = text,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1
            )
        }
    }
}

@Composable
fun DeviceFilterChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) Color(0xFF059669) else Color(0xFF111D36),
        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) Color.White else Color(0xFF94A3B8),
                    modifier = Modifier.size(11.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
            }
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) Color.White else Color(0xFF94A3B8),
                maxLines = 1
            )
        }
    }
}

@Composable
fun DeviceCardItem(
    device: NetworkDeviceEntity,
    onPing: () -> Unit,
    onDelete: () -> Unit
) {
    var expandedMenu by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (device.status == DeviceStatus.ONLINE) Color(0xFF065F46) else Color(0xFF7F1D1D)
            )
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                if (device.status == DeviceStatus.ONLINE) Color(0xFF10B981)
                                else if (device.status == DeviceStatus.OFFLINE) Color(0xFFEF4444)
                                else Color(0xFFF59E0B)
                            )
                    )
                    Column {
                        Text(
                            text = device.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "IP: ${device.ip}",
                            fontSize = 11.sp,
                            color = Color(0xFF38BDF8)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val typeLabel = when (device.type) {
                        DeviceType.MAIN_TOWER -> "برج رئيسي"
                        DeviceType.ROUTER -> "راوتر"
                        DeviceType.AP -> "نقطة AP"
                        DeviceType.MODEM -> "مودم"
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF1E293B)
                    ) {
                        Text(
                            text = typeLabel,
                            fontSize = 9.sp,
                            color = Color(0xFF34D399),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    IconButton(onClick = onPing, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Refresh, contentDescription = "فحص Ping", tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                    }

                    Box {
                        IconButton(onClick = { expandedMenu = true }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.MoreVert, contentDescription = "خيارات", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                        }
                        DropdownMenu(
                            expanded = expandedMenu,
                            onDismissRequest = { expandedMenu = false },
                            modifier = Modifier.background(Color(0xFF1E293B))
                        ) {
                            DropdownMenuItem(
                                text = { Text("فحص الاستجابة الآن", color = Color.White) },
                                onClick = {
                                    expandedMenu = false
                                    onPing()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("حذف الجهاز", color = Color(0xFFEF4444)) },
                                onClick = {
                                    expandedMenu = false
                                    onDelete()
                                }
                            )
                        }
                    }
                }
            }

            if (device.notes.isNotBlank() || device.manager.isNotBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (device.manager.isNotBlank()) {
                        Text(text = "المسؤول: ${device.manager}", fontSize = 10.sp, color = Color(0xFF94A3B8))
                    }
                    if (device.pingMs > 0 && device.status == DeviceStatus.ONLINE) {
                        Text(text = "الاستجابة: ${device.pingMs}ms", fontSize = 10.sp, color = Color(0xFF34D399))
                    }
                }
            }
        }
    }
}
