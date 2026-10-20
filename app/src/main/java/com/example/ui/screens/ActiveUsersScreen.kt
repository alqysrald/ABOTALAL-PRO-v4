package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.ActiveDeviceEntity
import com.example.data.model.HotspotProfileEntity
import com.example.data.model.RouterEntity
import com.example.ui.TalalProViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveUsersScreen(
    viewModel: TalalProViewModel,
    modifier: Modifier = Modifier
) {
    val routers by viewModel.allRouters.collectAsState()
    val selectedRouter by viewModel.selectedActiveRouter.collectAsState()
    val activeDevices by viewModel.detailedActiveDevices.collectAsState()
    val isLoading by viewModel.isLoadingActiveDevices.collectAsState()
    val hotspotProfiles by viewModel.hotspotProfiles.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, PHONE, PC, TV
    var showRouterDropdown by remember { mutableStateOf(false) }

    // Dialog States
    var deviceToKick by remember { mutableStateOf<ActiveDeviceEntity?>(null) }
    var deviceToBlock by remember { mutableStateOf<ActiveDeviceEntity?>(null) }
    var deviceToRenew by remember { mutableStateOf<ActiveDeviceEntity?>(null) }

    // Auto-select first router if none selected
    LaunchedEffect(routers) {
        if (selectedRouter == null && routers.isNotEmpty()) {
            val defaultRouter = routers.first()
            viewModel.selectActiveRouter(defaultRouter)
        }
    }

    val clipboardManager = LocalClipboardManager.current

    val filteredDevices = remember(activeDevices, searchQuery, selectedFilter) {
        activeDevices.filter { dev ->
            val matchSearch = searchQuery.isBlank() ||
                    dev.username.contains(searchQuery, ignoreCase = true) ||
                    dev.ipAddress.contains(searchQuery, ignoreCase = true) ||
                    dev.macAddress.contains(searchQuery, ignoreCase = true) ||
                    dev.hostName.contains(searchQuery, ignoreCase = true) ||
                    dev.deviceType.contains(searchQuery, ignoreCase = true)

            val matchFilter = when (selectedFilter) {
                "PHONE" -> dev.deviceType.contains("هاتف") || dev.deviceType.contains("iOS") || dev.deviceType.contains("Android")
                "PC" -> dev.deviceType.contains("كمبيوتر") || dev.deviceType.contains("لابتوب") || dev.deviceType.contains("Windows")
                "TV" -> dev.deviceType.contains("شاشة") || dev.deviceType.contains("تلفاز") || dev.deviceType.contains("TV")
                else -> true
            }

            matchSearch && matchFilter
        }
    }

    Scaffold(
        topBar = {
            Surface(
                color = Color.White,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFDCFCE7),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Wifi,
                                        contentDescription = null,
                                        tint = Color(0xFF16A34A),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "المتصلين الآن (Active Users & Devices)",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryNavy
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (activeDevices.isNotEmpty()) Color(0xFF16A34A) else Color(0xFFE2E8F0),
                                        modifier = Modifier.size(8.dp)
                                    ) {}
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${activeDevices.size} جهاز متصل حالياً",
                                        fontSize = 11.sp,
                                        color = if (activeDevices.isNotEmpty()) Color(0xFF16A34A) else TextSecondaryLight,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        // Router Selector Menu
                        Box {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFF1F5F9),
                                border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
                                modifier = Modifier.clickable { showRouterDropdown = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Dns, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = selectedRouter?.name ?: "اختر الراوتر...",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryNavy
                                    )
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TextMutedLight)
                                }
                            }

                            DropdownMenu(
                                expanded = showRouterDropdown,
                                onDismissRequest = { showRouterDropdown = false }
                            ) {
                                routers.forEach { r ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = if (r.isOnline) Color(0xFF22C55E) else Color(0xFFEF4444),
                                                    modifier = Modifier.size(8.dp)
                                                ) {}
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(r.name, fontWeight = if (r.id == selectedRouter?.id) FontWeight.Bold else FontWeight.Normal)
                                            }
                                        },
                                        onClick = {
                                            viewModel.selectActiveRouter(r)
                                            showRouterDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // KPI Stats Cards
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ActiveKpiCard(
                            title = "إجمالي المتصلين",
                            value = "${activeDevices.size}",
                            icon = Icons.Default.People,
                            color = PrimaryBlue,
                            modifier = Modifier.weight(1f)
                        )
                        ActiveKpiCard(
                            title = "الهواتف الذكية",
                            value = "${activeDevices.count { it.deviceType.contains("هاتف") || it.deviceType.contains("iOS") || it.deviceType.contains("Android") }}",
                            icon = Icons.Default.PhoneAndroid,
                            color = Color(0xFF10B981),
                            modifier = Modifier.weight(1f)
                        )
                        ActiveKpiCard(
                            title = "كمبيوتر وأخرى",
                            value = "${activeDevices.count { !it.deviceType.contains("هاتف") && !it.deviceType.contains("Android") }}",
                            icon = Icons.Default.Laptop,
                            color = Color(0xFF8B5CF6),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search & Filter Row
            Surface(
                color = Color.White,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("بحث باسم الكرت، الآيبي، الماك، أو الجهاز...", fontSize = 12.sp) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMutedLight) },
                            trailingIcon = {
                                if (searchQuery.isNotBlank()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "مسح", tint = TextMutedLight, modifier = Modifier.size(16.dp))
                                    }
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = { selectedRouter?.let { viewModel.loadActiveDevices(it) } },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFEFF6FF))
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "تحديث", tint = PrimaryBlue)
                        }
                    }

                    // Filter Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedFilter == "ALL",
                            onClick = { selectedFilter = "ALL" },
                            label = { Text("الكل (${activeDevices.size})", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = selectedFilter == "PHONE",
                            onClick = { selectedFilter = "PHONE" },
                            label = { Text("هواتف", fontSize = 11.sp) },
                            leadingIcon = { Icon(Icons.Default.PhoneIphone, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        )
                        FilterChip(
                            selected = selectedFilter == "PC",
                            onClick = { selectedFilter = "PC" },
                            label = { Text("كمبيوتر", fontSize = 11.sp) },
                            leadingIcon = { Icon(Icons.Default.Laptop, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        )
                        FilterChip(
                            selected = selectedFilter == "TV",
                            onClick = { selectedFilter = "TV" },
                            label = { Text("شاشات وأخرى", fontSize = 11.sp) },
                            leadingIcon = { Icon(Icons.Default.Tv, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        )
                    }
                }
            }

            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = PrimaryBlue)
            }

            // Devices List
            if (filteredDevices.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.WifiOff,
                            contentDescription = null,
                            tint = Color(0xFFCBD5E1),
                            modifier = Modifier.size(60.dp)
                        )
                        Text(
                            text = if (searchQuery.isNotBlank()) "لم يتم العثور على أجهزة مطابقة للبحث" else "لا توجد أجهزة متصلة حالياً بالراوتر",
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryLight,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "تأكد من عمل سيرفر الهوتسبوت أو اضغط تحديث لفحص المتصلين الجدد",
                            fontSize = 11.sp,
                            color = TextSecondaryLight,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = { selectedRouter?.let { viewModel.loadActiveDevices(it) } },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تحديث قائمة المتصلين الآن", fontSize = 12.sp)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredDevices, key = { "${it.id}_${it.macAddress}_${it.ipAddress}" }) { dev ->
                        ActiveDeviceCardItem(
                            device = dev,
                            onKick = { deviceToKick = dev },
                            onBlock = { deviceToBlock = dev },
                            onRenew = { deviceToRenew = dev },
                            onCopyText = { text, msg ->
                                clipboardManager.setText(AnnotatedString(text))
                                viewModel.snackbarMessage.value = msg
                            }
                        )
                    }
                }
            }
        }
    }

    // ================= DIALOGS =================

    // 1. Kick Dialog
    deviceToKick?.let { dev ->
        AlertDialog(
            onDismissRequest = { deviceToKick = null },
            icon = { Icon(Icons.Default.ExitToApp, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(32.dp)) },
            title = { Text("طرد وفصل المتصل", fontWeight = FontWeight.Bold) },
            text = {
                Text("هل أنت متأكد من رغبتك في طرد وفصل جلسة المتصل [${dev.username}] ذو الآيبي ${dev.ipAddress} فوراً؟")
            },
            confirmButton = {
                Button(
                    onClick = {
                        selectedRouter?.let { r ->
                            viewModel.kickActiveDevice(r, dev.id, dev.username)
                        }
                        deviceToKick = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("نعم، طرد وفصل", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { deviceToKick = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // 2. Block Dialog
    deviceToBlock?.let { dev ->
        AlertDialog(
            onDismissRequest = { deviceToBlock = null },
            icon = { Icon(Icons.Default.Block, contentDescription = null, tint = Color(0xFFB91C1C), modifier = Modifier.size(32.dp)) },
            title = { Text("حظر الجهاز (Blacklist)", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("سيتم حظر عنوان الـ MAC التالي ومنعه تماماً من دخول الشبكة:")
                    Surface(color = Color(0xFFFEE2E2), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "${dev.macAddress}\nاسم الجهاز: ${dev.hostName.ifBlank { dev.deviceType }}",
                            color = Color(0xFF991B1B),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                    Text("يمكنك لاحقاً إلغاء الحظر من قسم جدار الحماية (IP Binding) في الراوتر.", fontSize = 11.sp, color = TextSecondaryLight)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        selectedRouter?.let { r ->
                            viewModel.blockActiveDevice(r, dev.macAddress, dev.ipAddress, dev.username)
                        }
                        deviceToBlock = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB91C1C))
                ) {
                    Text("تأكيد الحظر 🚫", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { deviceToBlock = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // 3. Renew Card Dialog
    deviceToRenew?.let { dev ->
        RenewActiveCardDialog(
            device = dev,
            profiles = hotspotProfiles,
            onDismiss = { deviceToRenew = null },
            onConfirmRenew = { selectedProfile, uptime, bytes ->
                selectedRouter?.let { r ->
                    viewModel.renewActiveDeviceCard(r, dev.username, selectedProfile, uptime, bytes)
                }
                deviceToRenew = null
            }
        )
    }
}

@Composable
private fun ActiveKpiCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.08f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.2f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = color.copy(alpha = 0.2f),
                modifier = Modifier.size(30.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = color)
                Text(title, fontSize = 10.sp, color = TextSecondaryLight, maxLines = 1)
            }
        }
    }
}

@Composable
private fun ActiveDeviceCardItem(
    device: ActiveDeviceEntity,
    onKick: () -> Unit,
    onBlock: () -> Unit,
    onRenew: () -> Unit,
    onCopyText: (String, String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header: Device Icon + Device Name + Type Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = PrimaryBlue.copy(alpha = 0.12f),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            val icon = when {
                                device.deviceType.contains("iOS") || device.deviceType.contains("iPhone") -> Icons.Default.PhoneIphone
                                device.deviceType.contains("Android") || device.deviceType.contains("هاتف") -> Icons.Default.PhoneAndroid
                                device.deviceType.contains("كمبيوتر") || device.deviceType.contains("Windows") -> Icons.Default.Laptop
                                device.deviceType.contains("شاشة") || device.deviceType.contains("TV") -> Icons.Default.Tv
                                else -> Icons.Default.Devices
                            }
                            Icon(icon, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(24.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = device.username,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryNavy
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFDCFCE7)
                            ) {
                                Text(
                                    text = "متصل",
                                    color = Color(0xFF15803D),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = if (device.hostName.isNotBlank()) "${device.hostName} • ${device.deviceType}" else device.deviceType,
                            fontSize = 11.sp,
                            color = TextSecondaryLight
                        )
                    }
                }

                // Connection Type Badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFEFF6FF)
                ) {
                    Text(
                        text = if (device.type == "USER_MANAGER") "يوزرمانجر" else "هوتسبوت",
                        color = PrimaryBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9))

            // IP & MAC Row with Click-to-Copy
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF8FAFC),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.weight(1f).clickable {
                        onCopyText(device.ipAddress, "تم نسخ عنوان IP: ${device.ipAddress}")
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.PinDrop, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(device.ipAddress, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = PrimaryNavy)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF8FAFC),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.weight(1.3f).clickable {
                        onCopyText(device.macAddress, "تم نسخ عنوان MAC: ${device.macAddress}")
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Fingerprint, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(device.macAddress, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = PrimaryNavy, maxLines = 1)
                    }
                }
            }

            // Connection Stats: Remaining balance, Uptime, Quota
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFF8FAFC),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Timer, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("مدة الاتصال: ", fontSize = 11.sp, color = TextSecondaryLight)
                            Text(device.uptime, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryNavy)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.HourglassTop, contentDescription = null, tint = Color(0xFFEAB308), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("الوقت والصلاحية: ", fontSize = 11.sp, color = TextSecondaryLight)
                            Text(device.remainingTime, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryNavy)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DataUsage, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("الرصيد المتبقي: ", fontSize = 11.sp, color = TextSecondaryLight)
                            Text(device.remainingQuota, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.SwapVert, contentDescription = null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("الاستهلاك: ", fontSize = 11.sp, color = TextSecondaryLight)
                            Text(device.totalBytes, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = PrimaryNavy)
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Layers, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("الباقة الحالية: ", fontSize = 11.sp, color = TextSecondaryLight)
                        Text(device.profile, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                    }
                }
            }

            // Real-time Action Buttons (طرد، حظر، تجديد الكرت)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. Kick Button
                Button(
                    onClick = onKick,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFEE2E2)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f).height(38.dp)
                ) {
                    Icon(Icons.Default.ExitToApp, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("طرد", color = Color(0xFFDC2626), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                // 2. Block Button
                Button(
                    onClick = onBlock,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFF1F2)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f).height(38.dp)
                ) {
                    Icon(Icons.Default.Block, contentDescription = null, tint = Color(0xFFE11D48), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("حظر", color = Color(0xFFE11D48), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                // 3. Renew Card Button
                Button(
                    onClick = onRenew,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1.3f).height(38.dp)
                ) {
                    Icon(Icons.Default.Autorenew, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("تجديد الكرت", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun RenewActiveCardDialog(
    device: ActiveDeviceEntity,
    profiles: List<HotspotProfileEntity>,
    onDismiss: () -> Unit,
    onConfirmRenew: (profile: String, limitUptime: String, limitBytes: String) -> Unit
) {
    var selectedProfile by remember { mutableStateOf(device.profile.ifBlank { profiles.firstOrNull()?.name ?: "default" }) }
    var limitUptime by remember { mutableStateOf("") }
    var limitBytes by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = PrimaryBlue.copy(alpha = 0.15f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Autorenew, contentDescription = null, tint = PrimaryBlue)
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("تجديد كرت المتصل", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PrimaryNavy)
                        Text("المستخدم: ${device.username}", fontSize = 11.sp, color = TextSecondaryLight)
                    }
                }

                Text(
                    text = "اختر باقة أو بروفايل لإعادة شحن وتجديد رصيد الكرت وتصفير عدادات الاستهلاك فوراً في الراوتر:",
                    fontSize = 12.sp,
                    color = TextSecondaryLight
                )

                // Profile Selector Dropdown
                Box {
                    OutlinedTextField(
                        value = selectedProfile,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("فئة / باقة الكرت الجديدة") },
                        trailingIcon = {
                            IconButton(onClick = { expanded = true }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().clickable { expanded = true }
                    )

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        if (profiles.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("default") },
                                onClick = {
                                    selectedProfile = "default"
                                    expanded = false
                                }
                            )
                        } else {
                            profiles.forEach { p ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(p.name, fontWeight = FontWeight.Bold)
                                            Text("السرعة: ${p.rateLimit}", fontSize = 10.sp, color = TextSecondaryLight)
                                        }
                                    },
                                    onClick = {
                                        selectedProfile = p.name
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = limitUptime,
                    onValueChange = { limitUptime = it },
                    label = { Text("صلاحية الوقت (اختياري مثل: 1d أو 12h)") },
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = limitBytes,
                    onValueChange = { limitBytes = it },
                    label = { Text("حد الترافيك بالبايت (اختياري مثل: 1073741824 = 1GB)") },
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("إلغاء")
                    }
                    Button(
                        onClick = {
                            onConfirmRenew(selectedProfile, limitUptime, limitBytes)
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("تجديد الآن ✓", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
