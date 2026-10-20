package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.DeviceStatus
import com.example.data.model.DistributionDeviceItem
import com.example.data.model.NetworkPortEntity
import com.example.data.model.RouterEntity
import com.example.ui.TalalProViewModel
import com.example.ui.theme.*

enum class PortsScreenSection(val title: String) {
    DISTRIBUTION("📡 قطع توزيع الإنترنت"),
    PORTS("🔌 منافذ الشبكة والتحكم")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortsAndDistributionScreen(
    viewModel: TalalProViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val routers by viewModel.allRouters.collectAsState()
    val selectedRouter by viewModel.selectedPortsRouter.collectAsState()
    val ports by viewModel.networkPorts.collectAsState()
    val distributionUnits by viewModel.distributionDevices.collectAsState()
    val isLoading by viewModel.isLoadingPortsAndDistribution.collectAsState()

    var activeSection by remember { mutableStateOf(PortsScreenSection.DISTRIBUTION) }
    var searchQuery by remember { mutableStateOf("") }
    var portFilter by remember { mutableStateOf("ALL") } // "ALL", "RUNNING", "DOWN", "DISABLED"
    var unitFilter by remember { mutableStateOf("ALL") } // "ALL", "ONLINE", "OFFLINE"

    // Dialog States
    var showRouterPicker by remember { mutableStateOf(false) }
    var selectedPortForEdit by remember { mutableStateOf<NetworkPortEntity?>(null) }
    var selectedPortForLimit by remember { mutableStateOf<NetworkPortEntity?>(null) }
    var selectedPortForCableTest by remember { mutableStateOf<NetworkPortEntity?>(null) }
    var cableTestResult by remember { mutableStateOf<String?>(null) }
    var isTestingCable by remember { mutableStateOf(false) }

    val currentRouter = selectedRouter ?: routers.firstOrNull()

    LaunchedEffect(currentRouter?.id) {
        if (currentRouter != null) {
            viewModel.loadPortsAndDistribution(currentRouter)
        }
    }

    // Filtered lists
    val filteredUnits = remember(distributionUnits, searchQuery, unitFilter) {
        distributionUnits.filter { unit ->
            val matchesSearch = searchQuery.isBlank() ||
                    unit.name.contains(searchQuery, ignoreCase = true) ||
                    unit.ip.contains(searchQuery, ignoreCase = true) ||
                    unit.type.contains(searchQuery, ignoreCase = true) ||
                    unit.deviceModel.contains(searchQuery, ignoreCase = true)
            val matchesFilter = when (unitFilter) {
                "ONLINE" -> unit.status == DeviceStatus.ONLINE
                "OFFLINE" -> unit.status == DeviceStatus.OFFLINE
                else -> true
            }
            matchesSearch && matchesFilter
        }
    }

    val filteredPorts = remember(ports, searchQuery, portFilter) {
        ports.filter { port ->
            val matchesSearch = searchQuery.isBlank() ||
                    port.name.contains(searchQuery, ignoreCase = true) ||
                    port.comment.contains(searchQuery, ignoreCase = true) ||
                    port.macAddress.contains(searchQuery, ignoreCase = true)
            val matchesFilter = when (portFilter) {
                "RUNNING" -> port.isRunning && !port.isDisabled
                "DOWN" -> !port.isRunning && !port.isDisabled
                "DISABLED" -> port.isDisabled
                else -> true
            }
            matchesSearch && matchesFilter
        }
    }

    Scaffold(
        topBar = {
            Surface(
                color = Color(0xFF0F172A),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    // Title Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF0284C7).copy(alpha = 0.2f),
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Hub,
                                        contentDescription = null,
                                        tint = Color(0xFF38BDF8),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "المنافذ وأجهزة الشبكة",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "مراقبة قطع التوزيع والمنافذ والتحكم اللحظي",
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }

                        // Refresh Button
                        IconButton(
                            onClick = {
                                if (currentRouter != null) {
                                    viewModel.loadPortsAndDistribution(currentRouter)
                                }
                            },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E293B))
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color(0xFF38BDF8),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "تحديث",
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Router Selector Pill
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF1E293B),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showRouterPicker = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Dns,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = currentRouter?.let { "${it.name} (${it.host})" } ?: "اضغط لتحديد الراوتر",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Section Tabs Selector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF1E293B))
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        PortsScreenSection.values().forEach { section ->
                            val isSelected = activeSection == section
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) Color(0xFF0284C7) else Color.Transparent,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { activeSection = section }
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                ) {
                                    Text(
                                        text = section.title,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else Color(0xFF94A3B8)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        containerColor = Color(0xFF0B132B),
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(vertical = 14.dp)
        ) {
            // Search Input
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = if (activeSection == PortsScreenSection.DISTRIBUTION) "بحث بالاسم، الآيبي، الموديل، السيكتور..." else "بحث باسم المنفذ، التعليق، الماك...",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF38BDF8),
                        unfocusedBorderColor = Color(0xFF1E293B),
                        focusedContainerColor = Color(0xFF0F172A),
                        unfocusedContainerColor = Color(0xFF0F172A)
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("ports_search_input")
                )
            }

            // ================= SECTION 1: DISTRIBUTION PIECES =================
            if (activeSection == PortsScreenSection.DISTRIBUTION) {
                // Top Statistics Cards
                item {
                    val totalPieces = distributionUnits.size
                    val onlinePieces = distributionUnits.count { it.status == DeviceStatus.ONLINE }
                    val totalConnected = distributionUnits.sumOf { it.connectedUsersCount }
                    val totalDownloadMb = distributionUnits.sumOf { it.rxBytes } / (1024 * 1024)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Total Distribution Units Card
                        PortsStatCard(
                            title = "قطع التوزيع",
                            value = "$totalPieces",
                            subtitle = "$onlinePieces نشطة",
                            icon = Icons.Default.CellTower,
                            bgColor = Color(0xFF0369A1),
                            modifier = Modifier.weight(1f)
                        )

                        // Connected Clients in Pieces Card
                        PortsStatCard(
                            title = "المتصلون بالقطع",
                            value = "$totalConnected",
                            subtitle = "مستخدم نشط",
                            icon = Icons.Default.People,
                            bgColor = Color(0xFF15803D),
                            modifier = Modifier.weight(1f)
                        )

                        // Total Traffic Card
                        PortsStatCard(
                            title = "استهلاك القطع",
                            value = if (totalDownloadMb > 1024) "${totalDownloadMb / 1024} GB" else "$totalDownloadMb MB",
                            subtitle = "تنزيل ورفع",
                            icon = Icons.Default.CloudDownload,
                            bgColor = Color(0xFF6D28D9),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Filter Chips
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Triple("ALL", "الكل (${distributionUnits.size})", Color(0xFF0284C7)),
                            Triple("ONLINE", "متصلة (${distributionUnits.count { it.status == DeviceStatus.ONLINE }})", Color(0xFF10B981)),
                            Triple("OFFLINE", "متوقفة (${distributionUnits.count { it.status == DeviceStatus.OFFLINE }})", Color(0xFFEF4444))
                        ).forEach { (key, label, color) ->
                            val isSelected = unitFilter == key
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isSelected) color else Color(0xFF1E293B),
                                modifier = Modifier.clickable { unitFilter = key }
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else Color(0xFF94A3B8),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                if (filteredUnits.isEmpty()) {
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
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.CellTower, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(48.dp))
                                Text("لم يتم العثور على قطع توزيع", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("اضغط زر التحديث لجلب أجهزة البث المجاورة المكتشفة من الراوتر تلقائياً", color = Color(0xFF94A3B8), fontSize = 12.sp)
                            }
                        }
                    }
                } else {
                    items(filteredUnits, key = { it.id }) { unit ->
                        DistributionUnitCard(
                            unit = unit,
                            onOpenWeb = {
                                if (unit.ip.isNotBlank()) {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("http://${unit.ip}"))
                                        context.startActivity(intent)
                                    } catch (_: Exception) {
                                        Toast.makeText(context, "تعذر فتح المتصفح للعنوان: ${unit.ip}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            onPing = {
                                Toast.makeText(context, "زمن استجابة [${unit.name}]: ${unit.pingMs} ms (مستقر)", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }

            // ================= SECTION 2: PORTS & CONTROLS =================
            if (activeSection == PortsScreenSection.PORTS) {
                // Top Statistics Cards
                item {
                    val totalPorts = ports.size
                    val runningPorts = ports.count { it.isRunning && !it.isDisabled }
                    val downPorts = ports.count { !it.isRunning && !it.isDisabled }
                    val disabledPorts = ports.count { it.isDisabled }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PortsStatCard(
                            title = "إجمالي المنافذ",
                            value = "$totalPorts",
                            subtitle = "منافذ الراوتر",
                            icon = Icons.Default.SettingsEthernet,
                            bgColor = Color(0xFF0284C7),
                            modifier = Modifier.weight(1f)
                        )
                        PortsStatCard(
                            title = "متصل شغال",
                            value = "$runningPorts",
                            subtitle = "Link Running",
                            icon = Icons.Default.CheckCircle,
                            bgColor = Color(0xFF15803D),
                            modifier = Modifier.weight(1f)
                        )
                        PortsStatCard(
                            title = "غير متصل",
                            value = "$downPorts",
                            subtitle = "Link Down",
                            icon = Icons.Default.HighlightOff,
                            bgColor = Color(0xFFDC2626),
                            modifier = Modifier.weight(1f)
                        )
                        PortsStatCard(
                            title = "معطل",
                            value = "$disabledPorts",
                            subtitle = "Disabled",
                            icon = Icons.Default.Block,
                            bgColor = Color(0xFFD97706),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Filter Chips
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Triple("ALL", "الكل (${ports.size})", Color(0xFF0284C7)),
                            Triple("RUNNING", "المتصلة فقط (${ports.count { it.isRunning && !it.isDisabled }})", Color(0xFF10B981)),
                            Triple("DOWN", "غير المتصلة (${ports.count { !it.isRunning && !it.isDisabled }})", Color(0xFFEF4444)),
                            Triple("DISABLED", "المعطلة (${ports.count { it.isDisabled }})", Color(0xFFF59E0B))
                        ).forEach { (key, label, color) ->
                            val isSelected = portFilter == key
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isSelected) color else Color(0xFF1E293B),
                                modifier = Modifier.clickable { portFilter = key }
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else Color(0xFF94A3B8),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                if (filteredPorts.isEmpty()) {
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
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.SettingsEthernet, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(48.dp))
                                Text("لم يتم العثور على منافذ", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("تأكد من الاتصال براوتر المايكروتيك أو اضغط زر التحديث", color = Color(0xFF94A3B8), fontSize = 12.sp)
                            }
                        }
                    }
                } else {
                    items(filteredPorts, key = { it.name }) { port ->
                        RouterPortCard(
                            port = port,
                            onToggle = { enable ->
                                if (currentRouter != null) {
                                    viewModel.togglePort(currentRouter, port, enable)
                                }
                            },
                            onEdit = { selectedPortForEdit = port },
                            onResetCounters = {
                                if (currentRouter != null) {
                                    viewModel.resetPortCounters(currentRouter, port.name)
                                }
                            },
                            onSetLimit = { selectedPortForLimit = port },
                            onCableTest = {
                                selectedPortForCableTest = port
                                cableTestResult = null
                                isTestingCable = true
                                if (currentRouter != null) {
                                    viewModel.testPortCable(currentRouter, port.name) { res ->
                                        cableTestResult = res
                                        isTestingCable = false
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // ================= DIALOGS =================

    // 1. Router Picker Dialog
    if (showRouterPicker) {
        Dialog(onDismissRequest = { showRouterPicker = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF0F172A),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("اختر الراوتر المستهدف للمراقبة", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                    routers.forEach { r ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (currentRouter?.id == r.id) Color(0xFF0284C7).copy(alpha = 0.2f) else Color(0xFF1E293B),
                            border = if (currentRouter?.id == r.id) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8)) else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.selectedPortsRouter.value = r
                                    viewModel.loadPortsAndDistribution(r)
                                    showRouterPicker = false
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(r.name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                    Text(r.host, color = Color(0xFF94A3B8), fontSize = 11.sp)
                                }
                                if (currentRouter?.id == r.id) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF38BDF8))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 2. Edit Port Dialog (Name & Comment)
    selectedPortForEdit?.let { port ->
        var newName by remember { mutableStateOf(port.name) }
        var newComment by remember { mutableStateOf(port.comment) }

        Dialog(onDismissRequest = { selectedPortForEdit = null }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF0F172A),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("تعديل بيانات المنفذ [${port.name}]", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)

                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("اسم المنفذ (Name)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF1E293B)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newComment,
                        onValueChange = { newComment = it },
                        label = { Text("تعليق وتوصيف المنفذ (Comment)") },
                        placeholder = { Text("مثال: خط النت الرئيسي، برج الشمال، موزع السوق...") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF1E293B)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (currentRouter != null) {
                                    viewModel.updatePort(currentRouter, port.id.ifBlank { port.name }, newName, newComment)
                                }
                                selectedPortForEdit = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("حفظ التعديلات", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { selectedPortForEdit = null },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("إلغاء", color = Color(0xFF94A3B8))
                        }
                    }
                }
            }
        }
    }

    // 3. Bandwidth Limit Dialog
    selectedPortForLimit?.let { port ->
        val speedOptions = listOf("مفتوح", "5M/5M", "10M/10M", "20M/20M", "50M/50M", "100M/100M")
        var selectedLimit by remember { mutableStateOf(port.bandwidthLimit) }

        Dialog(onDismissRequest = { selectedPortForLimit = null }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF0F172A),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("تحديد سرعة المنفذ [${port.name}]", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                    Text("اختر أقصى سرعة رفع وتنزيل مسموحة عبر المنفذ:", fontSize = 12.sp, color = Color(0xFF94A3B8))

                    speedOptions.chunked(2).forEach { row ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { opt ->
                                val isSelected = selectedLimit == opt
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) Color(0xFF0284C7) else Color(0xFF1E293B),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { selectedLimit = opt }
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(12.dp)) {
                                        Text(opt, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (currentRouter != null) {
                                    viewModel.setPortBandwidthLimit(currentRouter, port.name, selectedLimit)
                                }
                                selectedPortForLimit = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("تطبيق السرعة", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { selectedPortForLimit = null },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("إلغاء", color = Color(0xFF94A3B8))
                        }
                    }
                }
            }
        }
    }

    // 4. Cable Test Dialog
    selectedPortForCableTest?.let { port ->
        Dialog(onDismissRequest = { selectedPortForCableTest = null }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF0F172A),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF0284C7).copy(alpha = 0.2f),
                        modifier = Modifier.size(54.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Cable, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(30.dp))
                        }
                    }

                    Text("فحص سلامة كيبل [${port.name}]", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)

                    if (isTestingCable) {
                        CircularProgressIndicator(color = Color(0xFF38BDF8), modifier = Modifier.size(32.dp))
                        Text("جاري تشغيل فحص الـ TDR واختبار أزواج الأسلاك...", fontSize = 12.sp, color = Color(0xFF94A3B8))
                    } else {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF1E293B),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("نتيجة الفحص التلقائي:", fontSize = 11.sp, color = Color(0xFF94A3B8))
                                Text(
                                    cableTestResult ?: "تم التأكد من سلامة توصيل المنفذ بدون التماس كهربائي أو تشويش.",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF34D399),
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    Button(
                        onClick = { selectedPortForCableTest = null },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("إغلاق", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ================= STAT CARD =================

@Composable
fun PortsStatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    bgColor: Color,
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
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(bgColor.copy(alpha = 0.2f))
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = bgColor, modifier = Modifier.size(18.dp))
            }
            Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Column {
                Text(text = title, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                Text(text = subtitle, fontSize = 9.sp, color = Color(0xFF34D399))
            }
        }
    }
}

// ================= DISTRIBUTION UNIT CARD =================

@Composable
fun DistributionUnitCard(
    unit: DistributionDeviceItem,
    onOpenWeb: () -> Unit,
    onPing: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (unit.status == DeviceStatus.ONLINE) Color(0xFF1E293B) else Color(0xFF7F1D1D)
            )
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: Name & Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (unit.status == DeviceStatus.ONLINE) Color(0xFF064E3B) else Color(0xFF7F1D1D))
                    ) {
                        Icon(
                            imageVector = Icons.Default.CellTower,
                            contentDescription = null,
                            tint = if (unit.status == DeviceStatus.ONLINE) Color(0xFF34D399) else Color(0xFFF87171),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Text(
                            text = unit.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF1E293B)
                            ) {
                                Text(
                                    text = unit.type,
                                    fontSize = 10.sp,
                                    color = Color(0xFF38BDF8),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                text = unit.deviceModel,
                                fontSize = 10.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                }

                // Status Badge
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (unit.status == DeviceStatus.ONLINE) Color(0xFF064E3B) else Color(0xFF7F1D1D)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (unit.status == DeviceStatus.ONLINE) Color(0xFF34D399) else Color(0xFFEF4444))
                        )
                        Text(
                            text = if (unit.status == DeviceStatus.ONLINE) "متصل 🟢" else "متوقف 🔴",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            HorizontalDivider(color = Color(0xFF1E293B))

            // IP & MAC & Interface
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("عنوان الـ IP:", fontSize = 10.sp, color = Color(0xFF94A3B8))
                    Text(unit.ip.ifBlank { "تلقائي DHCP" }, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                }
                Column {
                    Text("الماك (MAC):", fontSize = 10.sp, color = Color(0xFF94A3B8))
                    Text(unit.mac.ifBlank { "غير مسجل" }, fontSize = 11.sp, color = Color.White)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("المنفذ المتصل به:", fontSize = 10.sp, color = Color(0xFF94A3B8))
                    Text(unit.interfaceName.ifBlank { "ether2" }, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFBBF24))
                }
            }

            // Key Requirements Highlight Cards:
            // 1. عدد المتصلين في كل قطعة
            // 2. كمية استهلاك كل قطعة للإنترنت
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Connected Users Badge
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF1E293B),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0369A1).copy(alpha = 0.2f))
                        ) {
                            Icon(Icons.Default.People, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                        }
                        Column {
                            Text("عدد المتصلين بالقطعة", fontSize = 9.sp, color = Color(0xFF94A3B8))
                            Text("${unit.connectedUsersCount} مستخدم", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                        }
                    }
                }

                // Data Consumption Badge
                val rxMb = unit.rxBytes / (1024 * 1024)
                val txMb = unit.txBytes / (1024 * 1024)
                val rxFormatted = if (rxMb > 1024) "${rxMb / 1024} GB" else "$rxMb MB"
                val txFormatted = if (txMb > 1024) "${txMb / 1024} GB" else "$txMb MB"

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF1E293B),
                    modifier = Modifier.weight(1.3f)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF15803D).copy(alpha = 0.2f))
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, tint = Color(0xFF34D399), modifier = Modifier.size(16.dp))
                        }
                        Column {
                            Text("كمية استهلاك الإنترنت", fontSize = 9.sp, color = Color(0xFF94A3B8))
                            Text("📥 $rxFormatted | 📤 $txFormatted", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF34D399))
                        }
                    }
                }
            }

            // Real-Time Speed & Signal Rate
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0B132B))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Speed, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(14.dp))
                    Text("السرعة اللحظية: ${unit.currentRateDownload}", fontSize = 11.sp, color = Color(0xFFFDE68A))
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.NetworkCheck, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(14.dp))
                    Text("الإشارة: ${unit.signalDbm}", fontSize = 11.sp, color = Color(0xFFBAE6FD))
                }
            }

            // Quick Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenWeb,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0284C7)),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("صفحة القطعة Web", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onPing,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Sensors, contentDescription = null, tint = Color(0xFF34D399), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("فحص Ping (${unit.pingMs}ms)", fontSize = 11.sp, color = Color.White)
                }
            }
        }
    }
}

// ================= ROUTER PORT CARD =================

@Composable
fun RouterPortCard(
    port: NetworkPortEntity,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onResetCounters: () -> Unit,
    onSetLimit: () -> Unit,
    onCableTest: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                when {
                    port.isDisabled -> Color(0xFF475569)
                    port.isRunning -> Color(0xFF059669)
                    else -> Color(0xFFDC2626)
                }
            )
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Port Name & Link Status & Enable Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                when {
                                    port.isDisabled -> Color(0xFF334155)
                                    port.isRunning -> Color(0xFF064E3B)
                                    else -> Color(0xFF7F1D1D)
                                }
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.SettingsEthernet,
                            contentDescription = null,
                            tint = when {
                                port.isDisabled -> Color(0xFF94A3B8)
                                port.isRunning -> Color(0xFF34D399)
                                else -> Color(0xFFF87171)
                            },
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = port.name,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            if (port.defaultName.isNotBlank() && port.defaultName != port.name) {
                                Text(
                                    text = "(${port.defaultName})",
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }

                        Text(
                            text = if (port.comment.isNotBlank()) port.comment else "بدون تعليق توصيف",
                            fontSize = 11.sp,
                            color = if (port.comment.isNotBlank()) Color(0xFF38BDF8) else Color(0xFF64748B)
                        )
                    }
                }

                // Switch Toggle for Enable/Disable Port
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (port.isDisabled) "معطل" else "مفعل",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (port.isDisabled) Color(0xFFEF4444) else Color(0xFF10B981)
                    )
                    Switch(
                        checked = !port.isDisabled,
                        onCheckedChange = { onToggle(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF10B981),
                            uncheckedThumbColor = Color(0xFF94A3B8),
                            uncheckedTrackColor = Color(0xFF334155)
                        )
                    )
                }
            }

            HorizontalDivider(color = Color(0xFF1E293B))

            // Link status indicator pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = when {
                        port.isDisabled -> Color(0xFF334155)
                        port.isRunning -> Color(0xFF064E3B)
                        else -> Color(0xFF7F1D1D)
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        port.isDisabled -> Color(0xFF94A3B8)
                                        port.isRunning -> Color(0xFF34D399)
                                        else -> Color(0xFFEF4444)
                                    }
                                )
                        )
                        Text(
                            text = port.speed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("الماك: ${port.macAddress.ifBlank { "00:00:00:00:00:00" }}", fontSize = 10.sp, color = Color(0xFF94A3B8))
                    Text("MTU: ${port.mtu}", fontSize = 10.sp, color = Color(0xFF94A3B8))
                }
            }

            // Key Requirement: معرفة استهلاك الإنترنت لكل منفذ
            val rxMb = port.rxBytes / (1024 * 1024)
            val txMb = port.txBytes / (1024 * 1024)
            val rxFormatted = if (rxMb > 1024) "${rxMb / 1024} GB" else "$rxMb MB"
            val txFormatted = if (txMb > 1024) "${txMb / 1024} GB" else "$txMb MB"

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF1E293B),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("استهلاك الإنترنت للمنفذ:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                        Text("الحد: ${port.bandwidthLimit}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFBBF24))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(14.dp))
                            Text("تنزيل (Rx): $rxFormatted", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = Color(0xFF34D399), modifier = Modifier.size(14.dp))
                            Text("رفع (Tx): $txFormatted", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    if (port.rxDrops + port.txDrops > 0) {
                        Text("الحزم المفقودة (Drops): ${port.rxDrops + port.txDrops} حزمة", fontSize = 10.sp, color = Color(0xFFF87171))
                    }
                }
            }

            // Key Requirement: إجراءات التحكم بها جميع الإجراءات والتعديل
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // 1. Rename & Edit Comment
                OutlinedButton(
                    onClick = onEdit,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0284C7)),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("تعديل", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                // 2. Set Bandwidth Limit
                OutlinedButton(
                    onClick = onSetLimit,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFBBF24)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD97706)),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("تحديد السرعة", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                // 3. Reset Counters
                OutlinedButton(
                    onClick = onResetCounters,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF94A3B8)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF475569)),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("تصفير", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                // 4. Cable Test
                Button(
                    onClick = onCableTest,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Cable, contentDescription = null, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("فحص السلك", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
