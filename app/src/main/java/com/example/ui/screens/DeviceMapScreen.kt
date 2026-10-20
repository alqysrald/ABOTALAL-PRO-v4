package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DeviceStatus
import com.example.data.model.DeviceType
import com.example.data.model.NetworkDeviceEntity
import com.example.data.model.RouterEntity
import com.example.ui.TalalProViewModel
import com.example.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun DeviceMapScreen(
    viewModel: TalalProViewModel,
    modifier: Modifier = Modifier
) {
    val routers by viewModel.allRouters.collectAsState()
    val devices by viewModel.allDevices.collectAsState()
    val totalDevices by viewModel.totalDevicesCount.collectAsState()
    val onlineDevices by viewModel.onlineDevicesCount.collectAsState()
    val offlineDevices by viewModel.offlineDevicesCount.collectAsState()
    val maintenanceDevices by viewModel.maintenanceDevicesCount.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedTypeFilter by remember { mutableStateOf<DeviceType?>(null) }
    var showLinks by remember { mutableStateOf(true) }
    var isSatelliteMode by remember { mutableStateOf(true) }
    var selectedNodeName by remember { mutableStateOf<String?>(null) }
    var showHelpAccordion by remember { mutableStateOf(false) }

    val filteredDevices = devices.filter { d ->
        (selectedTypeFilter == null || d.type == selectedTypeFilter) &&
                (searchQuery.isBlank() || d.name.contains(searchQuery, ignoreCase = true) || d.ip.contains(searchQuery))
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0B132B))
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 14.dp)
    ) {
        // 1. "شرح الاستخدام" Accordion (Matches Screenshot 12)
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
                        Text(
                            text = "خريطة تفاعلية تعرض توبولوجي شبكة الأبراج والراوترات ونقاط البث مع روابط الاتصال وحالات الأجهزة في الوقت الحقيقي.",
                            fontSize = 11.sp,
                            color = Color(0xFFCBD5E1),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }

        // 2. Search Bar (Matches Screenshot 12)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF2563EB))
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.White)
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("بحث عن جهاز أو IP...", color = Color(0xFF64748B), fontSize = 12.sp) },
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
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 3. Map Mode & Filters Row (Matches Screenshot 12)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Neon / Satellite pill
                Surface(
                    onClick = { isSatelliteMode = !isSatelliteMode },
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF1E293B),
                    border = borderIf(isSatelliteMode, Color(0xFF38BDF8), 1.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (isSatelliteMode) "🛰️ القمر الصناعي" else "✨ خريطة النيون",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // Link Lines Toggle
                Surface(
                    onClick = { showLinks = !showLinks },
                    shape = RoundedCornerShape(20.dp),
                    color = if (showLinks) Color(0xFF065F46) else Color(0xFF1E293B),
                    border = borderIf(showLinks, Color(0xFF34D399), 1.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "خطوط الربط ✓",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // 4. Device Type Filters (الكل, راوتر, برج, AP, مودم) (Matches Screenshot 12)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TypeFilterChip(
                    title = "الكل",
                    isSelected = selectedTypeFilter == null,
                    onClick = { selectedTypeFilter = null },
                    modifier = Modifier.weight(1f)
                )
                TypeFilterChip(
                    title = "راوتر",
                    isSelected = selectedTypeFilter == DeviceType.ROUTER,
                    onClick = { selectedTypeFilter = DeviceType.ROUTER },
                    modifier = Modifier.weight(1f)
                )
                TypeFilterChip(
                    title = "برج",
                    isSelected = selectedTypeFilter == DeviceType.MAIN_TOWER,
                    onClick = { selectedTypeFilter = DeviceType.MAIN_TOWER },
                    modifier = Modifier.weight(1f)
                )
                TypeFilterChip(
                    title = "AP",
                    isSelected = selectedTypeFilter == DeviceType.AP,
                    onClick = { selectedTypeFilter = DeviceType.AP },
                    modifier = Modifier.weight(1f)
                )
                TypeFilterChip(
                    title = "مودم",
                    isSelected = selectedTypeFilter == DeviceType.MODEM,
                    onClick = { selectedTypeFilter = DeviceType.MODEM },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 5. Status Badges Summary Bar (Matches Screenshot 12)
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF1E293B))),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "حالة الشبكة", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            LegendStatusDot(text = "متصل: $onlineDevices", color = Color(0xFF10B981))
                            LegendStatusDot(text = "منقطع: $offlineDevices", color = Color(0xFFEF4444))
                            LegendStatusDot(text = "تحذير: $maintenanceDevices", color = Color(0xFFF59E0B))
                        }
                    }
                    Text(text = "يتم تحديث البيانات تلقائياً", fontSize = 9.sp, color = Color(0xFF94A3B8))
                }
            }
        }

        // 6. Interactive Network Canvas (Topology Radar / Map)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0A1128)),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF1E3A5F))),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    NetworkTopologyCanvas(
                        routers = routers,
                        devices = filteredDevices,
                        showLinks = showLinks,
                        isSatellite = isSatelliteMode,
                        onNodeClick = { name -> selectedNodeName = name }
                    )

                    // Overlay Map Legend / Details Pill
                    if (selectedNodeName != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF0F172A).copy(alpha = 0.9f),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "📍 تم اختيار: $selectedNodeName",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF38BDF8),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        // 7. Device Type Icons Legend Bar (Matches Screenshot 12)
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF1E293B))),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        TypeIconLegendItem(name = "برج", icon = Icons.Default.CellTower, color = Color(0xFF10B981))
                        TypeIconLegendItem(name = "راوتر", icon = Icons.Default.Dns, color = Color(0xFF8B5CF6))
                        TypeIconLegendItem(name = "AP", icon = Icons.Default.Wifi, color = Color(0xFF06B6D4))
                        TypeIconLegendItem(name = "مودم", icon = Icons.Default.Router, color = Color(0xFFF59E0B))
                    }

                    // Link styles
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Text(text = "— — متصل", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                        Text(text = "— — ضعيف", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B))
                        Text(text = "— — منقطع", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                    }
                }
            }
        }

        // 8. Help Guides for Setting Coordinates (Matches Screenshot 13)
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF1E293B))),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "📍 تحديد المواقع الجغرافية",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    // For Devices Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(text = "📱 للأجهزة: صفحة أجهزة الشبكة ← زر 📍 على الجهاز ← اختر الموقع", fontSize = 11.sp, color = Color(0xFF93C5FD))
                        }
                    }

                    // For Routers Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(text = "📡 للراوترات: صفحة راوترات MikroTik ← حقل الموقع ← 15.369,44.191", fontSize = 11.sp, color = Color(0xFF6EE7B7))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TypeFilterChip(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) Color(0xFF059669) else Color(0xFF1E293B),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier.padding(vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) Color.White else Color(0xFF94A3B8)
            )
        }
    }
}

@Composable
fun LegendStatusDot(text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(color))
        Text(text = text, fontSize = 10.sp, color = Color(0xFFCBD5E1))
    }
}

@Composable
fun TypeIconLegendItem(
    name: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.2f))
                .border(1.dp, color, CircleShape)
        ) {
            Icon(imageVector = icon, contentDescription = name, tint = color, modifier = Modifier.size(18.dp))
        }
        Text(text = name, fontSize = 11.sp, color = Color.White)
    }
}

@Composable
fun NetworkTopologyCanvas(
    routers: List<RouterEntity>,
    devices: List<NetworkDeviceEntity>,
    showLinks: Boolean,
    isSatellite: Boolean,
    onNodeClick: (String) -> Unit
) {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(routers, devices) {
                detectTapGestures { offset ->
                    // Find closest node
                    onNodeClick("جهاز شبكة نشط")
                }
            }
    ) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val radius = size.minDimension * 0.38f

        // Draw radar rings
        val ringColor = if (isSatellite) Color(0xFF1E3A5F) else Color(0xFF134E4A)
        drawCircle(color = ringColor.copy(alpha = 0.4f), radius = radius, center = Offset(centerX, centerY), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f))
        drawCircle(color = ringColor.copy(alpha = 0.3f), radius = radius * 0.66f, center = Offset(centerX, centerY), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f))
        drawCircle(color = ringColor.copy(alpha = 0.2f), radius = radius * 0.33f, center = Offset(centerX, centerY), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f))

        // Center Router Node
        val centerColor = Color(0xFF38BDF8)
        drawCircle(color = centerColor.copy(alpha = 0.25f), radius = 24f, center = Offset(centerX, centerY))
        drawCircle(color = centerColor, radius = 10f, center = Offset(centerX, centerY))

        // Draw Device Nodes around center
        val totalNodes = if (devices.isNotEmpty()) devices.size else if (routers.isNotEmpty()) routers.size else 4
        for (i in 0 until totalNodes) {
            val angle = (2 * Math.PI * i / totalNodes).toFloat()
            val nodeRadius = radius * (0.7f + (i % 3) * 0.12f)
            val nx = centerX + nodeRadius * cos(angle)
            val ny = centerY + nodeRadius * sin(angle)

            val device = devices.getOrNull(i)
            val isOnline = device?.status == DeviceStatus.ONLINE || (device == null && i % 2 == 0)
            val nodeColor = if (isOnline) Color(0xFF10B981) else Color(0xFFEF4444)

            // Connection Link Line
            if (showLinks) {
                drawLine(
                    color = if (isOnline) Color(0xFF10B981).copy(alpha = 0.5f) else Color(0xFFEF4444).copy(alpha = 0.4f),
                    start = Offset(centerX, centerY),
                    end = Offset(nx, ny),
                    strokeWidth = 2f,
                    pathEffect = if (!isOnline) PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f) else null
                )
            }

            // Node Glow & Core
            drawCircle(color = nodeColor.copy(alpha = 0.3f), radius = 16f, center = Offset(nx, ny))
            drawCircle(color = nodeColor, radius = 8f, center = Offset(nx, ny))
        }
    }
}

private fun borderIf(condition: Boolean, color: Color, width: androidx.compose.ui.unit.Dp): androidx.compose.foundation.BorderStroke? {
    return if (condition) androidx.compose.foundation.BorderStroke(width, color) else null
}
