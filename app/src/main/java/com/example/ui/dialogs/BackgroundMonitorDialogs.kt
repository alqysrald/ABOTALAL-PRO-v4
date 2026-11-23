package com.example.ui.dialogs

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.RouterEntity
import com.example.service.BackgroundMonitorState
import com.example.service.RouterMonitorManager

@Composable
fun BackgroundMonitorControlCard(
    routers: List<RouterEntity>,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        RouterMonitorManager.ensureInitialized(context)
    }
    val monitorState by RouterMonitorManager.state.collectAsState()

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            RouterMonitorManager.startService(context)
            Toast.makeText(context, "تم تفعيل المراقبة بالخلفية وإشعار الحالة ✅", Toast.LENGTH_SHORT).show()
        } else {
            RouterMonitorManager.setEnabledPref(context, false)
            Toast.makeText(context, "يرجى السماح بصلاحية الإشعارات لعرض إشعار الحالة بالخلفية", Toast.LENGTH_LONG).show()
        }
    }

    val cpuFraction = (monitorState.cpuLoad.coerceIn(0, 100)) / 100f
    val cpuColor = when {
        !monitorState.isOnline -> Color(0xFF94A3B8)
        monitorState.cpuLoad >= 80 -> Color(0xFFF87171)
        monitorState.cpuLoad >= 50 -> Color(0xFFFBBF24)
        else -> Color(0xFF34D399)
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = BorderStroke(
            1.2.dp,
            if (monitorState.isServiceRunning) Color(0xFF0284C7) else Color(0xFF334155)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF0F172A), Color(0xFF13203C))
                    )
                )
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header + Toggle Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (monitorState.isServiceRunning) Color(0xFF0C4A6E) else Color(0xFF1E293B)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "إشعار الخلفية",
                            tint = if (monitorState.isServiceRunning) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "المراقبة بالخلفية وإشعار الحالة",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (monitorState.isServiceRunning) Color(0xFF065F46) else Color(0xFF334155)
                            ) {
                                Text(
                                    text = if (monitorState.isServiceRunning) "يعمل الآن" else "متوقف",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "يعرض تفاصيل الاتصال والمعالج في نافذة إشعارات الهاتف",
                            fontSize = 10.5.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                Switch(
                    checked = monitorState.isServiceRunning || monitorState.isEnabled,
                    onCheckedChange = { wantEnable ->
                        if (wantEnable) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                !RouterMonitorManager.hasNotificationPermission(context)
                            ) {
                                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                RouterMonitorManager.startService(context)
                                Toast.makeText(context, "تم تشغيل خدمة الخلفية ✅", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            RouterMonitorManager.stopService(context, disableAutoStart = true)
                            Toast.makeText(context, "تم إيقاف خدمة الخلفية", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF0284C7),
                        uncheckedThumbColor = Color(0xFFCBD5E1),
                        uncheckedTrackColor = Color(0xFF334155)
                    ),
                    modifier = Modifier.testTag("bg_monitor_switch")
                )
            }

            // Live Telemetry Box (Matches the Luxury Notification Shade Preview)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF0B132B),
                border = BorderStroke(1.dp, Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = monitorState.routerName,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1
                            )
                            Text(
                                text = monitorState.boardInfo,
                                fontSize = 10.sp,
                                color = Color(0xFF94A3B8),
                                maxLines = 1
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (monitorState.showPingAndUptime && monitorState.isOnline) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF1E293B)
                                ) {
                                    Text(
                                        text = "${monitorState.pingMs} ms",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF38BDF8),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (monitorState.isOnline) Color(0xFF064E3B) else Color(0xFF7F1D1D)
                            ) {
                                Text(
                                    text = when {
                                        !monitorState.hasRouters && routers.isEmpty() -> "لا يوجد راوتر"
                                        monitorState.isOnline -> "متصل ●"
                                        else -> "غير متصل ●"
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (monitorState.isOnline) Color(0xFF6EE7B7) else Color(0xFFFCA5A5),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    if (monitorState.showCpuInNotif) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "حمل المعالج (CPU Load)",
                                    fontSize = 10.5.sp,
                                    color = Color(0xFFCBD5E1)
                                )
                                Text(
                                    text = if (monitorState.isOnline) "${monitorState.cpuLoad}%" else "--%",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = cpuColor
                                )
                            }
                            LinearProgressIndicator(
                                progress = { if (monitorState.isOnline) cpuFraction else 0f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = cpuColor,
                                trackColor = Color(0xFF1E293B)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MonitorMiniStatTile(
                            label = "الرام المتاحة",
                            value = monitorState.freeMemory,
                            valueColor = Color(0xFF38BDF8),
                            modifier = Modifier.weight(1f)
                        )
                        if (monitorState.showUsersInNotif) {
                            MonitorMiniStatTile(
                                label = "المتصلين",
                                value = "${monitorState.activeUsers} نشط",
                                valueColor = Color(0xFF6EE7B7),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (monitorState.showPingAndUptime) {
                            MonitorMiniStatTile(
                                label = "التشغيل",
                                value = monitorState.uptime,
                                valueColor = Color(0xFFFDE68A),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "كل ${monitorState.intervalSeconds} ثانية • ${monitorState.onlineRouters}/${monitorState.totalRouters} راوتر",
                            fontSize = 9.5.sp,
                            color = Color(0xFF94A3B8)
                        )
                        Text(
                            text = "آخر تحديث: ${monitorState.lastUpdatedTime}",
                            fontSize = 9.5.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }

            // Action Controls Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            !RouterMonitorManager.hasNotificationPermission(context)
                        ) {
                            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            RouterMonitorManager.refreshNow(context)
                            Toast.makeText(context, "جاري تحديث تفاصيل الإشعار...", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("bg_monitor_refresh_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "تحديث الإشعار الآن",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (monitorState.isRefreshingNow) "جاري الفحص..." else "تحديث الآن",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = onOpenSettings,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("bg_monitor_settings_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "التحكم في الخلفية",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "التحكم بالإشعار",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8)
                    )
                }
            }
        }
    }
}

@Composable
private fun MonitorMiniStatTile(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF1E293B),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, fontSize = 9.sp, color = Color(0xFF94A3B8), maxLines = 1)
            Text(
                text = value,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = valueColor,
                maxLines = 1
            )
        }
    }
}

@Composable
fun BackgroundMonitorSettingsDialog(
    routers: List<RouterEntity>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val monitorState by RouterMonitorManager.state.collectAsState()

    var selectedInterval by remember(monitorState.intervalSeconds) { mutableIntStateOf(monitorState.intervalSeconds) }
    var selectedRouterId by remember(monitorState.targetRouterId) { mutableLongStateOf(monitorState.targetRouterId) }
    var showCpu by remember(monitorState.showCpuInNotif) { mutableStateOf(monitorState.showCpuInNotif) }
    var showUsers by remember(monitorState.showUsersInNotif) { mutableStateOf(monitorState.showUsersInNotif) }
    var showPingUptime by remember(monitorState.showPingAndUptime) { mutableStateOf(monitorState.showPingAndUptime) }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            RouterMonitorManager.startService(context)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF0F172A),
            border = BorderStroke(1.dp, Color(0xFF1E3A8A)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
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
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8)
                        )
                        Text(
                            text = "إعدادات العمل بالخلفية والإشعار",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color(0xFF94A3B8))
                    }
                }

                // Service active switch
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1E293B),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "تشغيل البرنامج في الخلفية",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "إبقاء إشعار الحالة الفخم في شريط الإشعارات",
                                fontSize = 10.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                        Switch(
                            checked = monitorState.isServiceRunning || monitorState.isEnabled,
                            onCheckedChange = { enable ->
                                if (enable) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                        !RouterMonitorManager.hasNotificationPermission(context)
                                    ) {
                                        notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        RouterMonitorManager.startService(context)
                                    }
                                } else {
                                    RouterMonitorManager.stopService(context, disableAutoStart = true)
                                }
                            },
                            modifier = Modifier.testTag("bg_dialog_enable_switch")
                        )
                    }
                }

                // Update Interval Selector
                Text(
                    text = "سرعة تحديث البيانات في الإشعار:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF38BDF8)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(10, 15, 30, 60).forEach { sec ->
                        val selected = selectedInterval == sec
                        FilterChip(
                            selected = selected,
                            onClick = { selectedInterval = sec },
                            label = {
                                Text(
                                    text = "$sec ث",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF0284C7),
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFF1E293B),
                                labelColor = Color(0xFFCBD5E1)
                            ),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("interval_chip_$sec")
                        )
                    }
                }

                // Target Router Selector
                Text(
                    text = "الراوتر المعروض في الإشعار:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF38BDF8)
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    RouterSelectOptionRow(
                        title = "تلقائي (أول راوتر متصل)",
                        subtitle = "يتحول تلقائياً للراوتر النشط",
                        selected = selectedRouterId == 0L,
                        onClick = { selectedRouterId = 0L }
                    )
                    routers.forEach { router ->
                        RouterSelectOptionRow(
                            title = router.name,
                            subtitle = "${router.host} • ${if (router.isOnline) "متصل" else "غير متصل"}",
                            selected = selectedRouterId == router.id,
                            onClick = { selectedRouterId = router.id }
                        )
                    }
                }

                // Details visibility toggles
                Text(
                    text = "العناصر الظاهرة داخل الإشعار:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF38BDF8)
                )
                MonitorToggleOptionRow(
                    title = "تفاصيل وحمل المعالج (CPU Load)",
                    checked = showCpu,
                    onCheckedChange = { showCpu = it }
                )
                MonitorToggleOptionRow(
                    title = "عدد المتصلين بالشبكة (Active Users)",
                    checked = showUsers,
                    onCheckedChange = { showUsers = it }
                )
                MonitorToggleOptionRow(
                    title = "البنج (Ping) ومدة التشغيل (Uptime)",
                    checked = showPingUptime,
                    onCheckedChange = { showPingUptime = it }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                    ) {
                        Text("إلغاء", color = Color(0xFFCBD5E1))
                    }
                    Button(
                        onClick = {
                            RouterMonitorManager.updateSettings(
                                context = context,
                                intervalSeconds = selectedInterval,
                                targetRouterId = selectedRouterId,
                                showCpuInNotif = showCpu,
                                showUsersInNotif = showUsers,
                                showPingAndUptime = showPingUptime
                            )
                            Toast.makeText(context, "تم حفظ إعدادات الإشعار والخلفية ✅", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1.3f)
                            .heightIn(min = 48.dp)
                            .testTag("bg_dialog_save_btn")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("حفظ وتطبيق", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun RouterSelectOptionRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (selected) Color(0xFF0C4A6E) else Color(0xFF1E293B),
        border = BorderStroke(1.dp, if (selected) Color(0xFF38BDF8) else Color(0xFF334155)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (selected) Color(0xFF38BDF8) else Color(0xFF64748B))
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(text = subtitle, fontSize = 10.sp, color = Color(0xFF94A3B8))
            }
            if (selected) {
                Icon(Icons.Default.CheckCircle, contentDescription = "محدد", tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun MonitorToggleOptionRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF1E293B),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 12.sp,
                color = Color(0xFFE2E8F0),
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}
