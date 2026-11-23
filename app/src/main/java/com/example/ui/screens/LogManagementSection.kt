package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AlertSeverity
import com.example.data.model.DeviceStatus
import com.example.data.model.LogSeverity
import com.example.data.model.MikrotikLogEntry
import com.example.data.model.RouterEntity
import com.example.ui.TalalProViewModel
import com.example.ui.theme.*
import com.example.ui.utils.ArabicLogTranslator

private enum class LogFilterMode(val label: String) {
    ALL("الكل"),
    PROBLEMS("المشاكل والأخطاء"),
    WARNINGS("التحذيرات"),
    ROUTER_ONLY("سجل الراوتر"),
    APP_ONLY("سجل التطبيق")
}

@Composable
fun LogManagementSection(
    viewModel: TalalProViewModel,
    router: RouterEntity?,
    modifier: Modifier = Modifier
) {
    val routerLogs by viewModel.routerLogs.collectAsState()
    val isLoading by viewModel.isLoadingRouterLogs.collectAsState()
    val routerError by viewModel.routerLogsError.collectAsState()
    val recentLogs by viewModel.recentLogs.collectAsState()
    val allAlerts by viewModel.allAlerts.collectAsState()
    val allRouters by viewModel.allRouters.collectAsState()
    val allDevices by viewModel.allDevices.collectAsState()

    var filterMode by remember { mutableStateOf(LogFilterMode.ALL) }
    var searchQuery by remember { mutableStateOf("") }
    var showRawOriginal by remember { mutableStateOf(false) }
    var visibleLimit by remember { mutableIntStateOf(60) }
    var showClearConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(router?.id) {
        if (router != null) {
            viewModel.loadRouterLogs(router)
        }
    }

    // Build unified Arabic log feed from MikroTik + App alerts + Offline/Error diagnostics + Recent Actions
    val unifiedLogs = remember(routerLogs, recentLogs, allAlerts, allRouters, allDevices) {
        val combined = mutableListOf<MikrotikLogEntry>()

        // 1. Live Router Connection Errors (if any)
        allRouters.forEach { r ->
            if (!r.isOnline || !r.lastError.isNullOrBlank()) {
                val errText = r.lastError?.takeIf { it.isNotBlank() }
                    ?.let { ArabicLogTranslator.translateAppErrorToArabic(it) }
                    ?: "الراوتر غير متصل أو لا يستجيب لطلبات الشبكة"
                combined.add(
                    MikrotikLogEntry(
                        id = "r_err_${r.id}",
                        time = if (r.lastChecked > 0L) ArabicLogTranslator.formatTimestamp(r.lastChecked) else "الآن",
                        topics = "router,error",
                        topicsAr = "اتصال الراوتر • ${r.name}",
                        originalMessage = r.lastError ?: "Offline",
                        arabicMessage = "مشكلة في راوتر [${r.name}]: $errText",
                        severity = LogSeverity.ERROR,
                        source = "التطبيق"
                    )
                )
            }
        }

        // 2. Network Devices with Issues / Offline
        allDevices.filter { it.hasIssue || it.status != DeviceStatus.ONLINE }.forEach { dev ->
            val issueMsg = dev.issueReason.takeIf { it.isNotBlank() }
                ?.let { ArabicLogTranslator.translateAppErrorToArabic(it) }
                ?: if (dev.status == DeviceStatus.OFFLINE) "الجهاز طافي / منقطع عن الشبكة" else "الجهاز في وضع الصيانة أو التحذير"
            combined.add(
                MikrotikLogEntry(
                    id = "dev_err_${dev.id}",
                    time = ArabicLogTranslator.formatTimestamp(dev.lastSeen),
                    topics = "device,warning",
                    topicsAr = "مراقبة الأجهزة • ${dev.name}",
                    originalMessage = "${dev.name} (${dev.ip})",
                    arabicMessage = "تنبيه جهاز [${dev.name} - ${dev.ip}]: $issueMsg",
                    severity = if (dev.status == DeviceStatus.OFFLINE) LogSeverity.ERROR else LogSeverity.WARNING,
                    source = "التطبيق"
                )
            )
        }

        // 3. Alerts from AlertEntity
        allAlerts.forEach { alert ->
            val sev = when (alert.severity) {
                AlertSeverity.ERROR -> LogSeverity.ERROR
                AlertSeverity.WARNING -> LogSeverity.WARNING
                AlertSeverity.INFO -> LogSeverity.INFO
            }
            combined.add(
                MikrotikLogEntry(
                    id = "alert_${alert.id}",
                    time = ArabicLogTranslator.formatTimestamp(alert.timestamp),
                    topics = "alert",
                    topicsAr = ArabicLogTranslator.translateAppErrorToArabic(alert.title),
                    originalMessage = alert.message,
                    arabicMessage = ArabicLogTranslator.translateAppErrorToArabic(alert.message),
                    severity = sev,
                    source = "التطبيق"
                )
            )
        }

        // 4. Local App Operation & Error Logs (NetworkLogEntity)
        recentLogs.forEach { log ->
            val actionAr = ArabicLogTranslator.translateAppErrorToArabic(log.action)
            val detailsAr = ArabicLogTranslator.translateAppErrorToArabic(log.details)
            combined.add(
                MikrotikLogEntry(
                    id = "app_log_${log.id}",
                    time = ArabicLogTranslator.formatTimestamp(log.timestamp),
                    topics = if (log.isSuccess) "app,info" else "app,error",
                    topicsAr = actionAr,
                    originalMessage = log.details,
                    arabicMessage = detailsAr,
                    severity = if (!log.isSuccess || detailsAr.contains("خطأ") || detailsAr.contains("فشل")) {
                        LogSeverity.ERROR
                    } else {
                        LogSeverity.INFO
                    },
                    source = "التطبيق"
                )
            )
        }

        // 5. Real RouterOS Logs (/log/print)
        combined.addAll(routerLogs)

        combined
    }

    val errorCount = remember(unifiedLogs) {
        unifiedLogs.count { it.severity == LogSeverity.ERROR || it.severity == LogSeverity.CRITICAL }
    }
    val warningCount = remember(unifiedLogs) {
        unifiedLogs.count { it.severity == LogSeverity.WARNING }
    }

    val filteredLogs = remember(unifiedLogs, filterMode, searchQuery) {
        unifiedLogs.filter { entry ->
            val matchesFilter = when (filterMode) {
                LogFilterMode.ALL -> true
                LogFilterMode.PROBLEMS -> entry.severity == LogSeverity.ERROR || entry.severity == LogSeverity.CRITICAL
                LogFilterMode.WARNINGS -> entry.severity == LogSeverity.WARNING
                LogFilterMode.ROUTER_ONLY -> entry.source == "الراوتر"
                LogFilterMode.APP_ONLY -> entry.source == "التطبيق"
            }
            val matchesQuery = searchQuery.isBlank() ||
                entry.arabicMessage.contains(searchQuery, ignoreCase = true) ||
                entry.topicsAr.contains(searchQuery, ignoreCase = true) ||
                entry.originalMessage.contains(searchQuery, ignoreCase = true) ||
                entry.time.contains(searchQuery, ignoreCase = true)
            matchesFilter && matchesQuery
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header & Statistics Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF0F172A), Color(0xFF1E3A8A))
                                    )
                                )
                        ) {
                            Icon(
                                Icons.Default.ReceiptLong,
                                contentDescription = "سجل الأحداث",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "سجل النظام والمشاكل (Log)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = PrimaryNavy
                            )
                            Text(
                                text = "عرض جميع الأخطاء، التنبيهات، ورسائل المايكروتك باللغة العربية",
                                fontSize = 11.sp,
                                color = TextSecondaryLight
                            )
                        }
                    }

                    IconButton(
                        onClick = { viewModel.loadRouterLogs(router) },
                        enabled = !isLoading,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("refresh_logs_btn")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = PrimaryBlue)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "تحديث السجل", tint = PrimaryBlue)
                        }
                    }
                }

                // Summary Counters Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (errorCount > 0) Color(0xFFFEF2F2) else Color(0xFFF0FDF4),
                        border = BorderStroke(1.dp, if (errorCount > 0) Color(0xFFFECACA) else Color(0xFFBBF7D0)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "$errorCount",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = if (errorCount > 0) Color(0xFFDC2626) else Color(0xFF16A34A)
                            )
                            Text("أخطاء ومشاكل", fontSize = 10.sp, color = TextSecondaryLight)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFFFFBEB),
                        border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "$warningCount",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = Color(0xFFD97706)
                            )
                            Text("تحذيرات", fontSize = 10.sp, color = TextSecondaryLight)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFEFF6FF),
                        border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "${unifiedLogs.size}",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = PrimaryBlue
                            )
                            Text("إجمالي السجلات", fontSize = 10.sp, color = TextSecondaryLight)
                        }
                    }
                }

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("ابحث في المشاكل أو اسم الكرت أو IP...", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMutedLight) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "مسح البحث")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("logs_search_input")
                )

                // Filter Chips (Horizontal Scroll)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LogFilterMode.entries.forEach { mode ->
                        val selected = filterMode == mode
                        FilterChip(
                            selected = selected,
                            onClick = { filterMode = mode },
                            label = {
                                Text(
                                    text = mode.label,
                                    fontSize = 11.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = when (mode) {
                                    LogFilterMode.PROBLEMS -> Color(0xFFDC2626)
                                    LogFilterMode.WARNINGS -> Color(0xFFD97706)
                                    else -> PrimaryNavy
                                },
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("log_filter_${mode.name.lowercase()}")
                        )
                    }
                }

                // Action Buttons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { showRawOriginal = !showRawOriginal },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                    ) {
                        Icon(
                            if (showRawOriginal) Icons.Default.VisibilityOff else Icons.Default.Translate,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (showRawOriginal) "إخفاء النص الأصلي" else "إظهار النص الأصلي",
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }

                    OutlinedButton(
                        onClick = { showClearConfirm = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                        border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("clear_app_logs_btn")
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("مسح سجل التطبيق", fontSize = 11.sp, maxLines = 1)
                    }
                }
            }
        }

        // Router Connection Error Notice (if router couldn't be reached for live /log/print)
        AnimatedVisibility(visible = !routerError.isNullOrBlank()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFFEF2F2),
                border = BorderStroke(1.dp, Color(0xFFFECACA)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.WarningAmber, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "تعذر جلب سجل الراوتر المباشر (${router?.name ?: "غير محدد"})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color(0xFF991B1B)
                        )
                        Text(
                            text = routerError ?: "",
                            fontSize = 11.sp,
                            color = Color(0xFFB91C1C)
                        )
                    }
                }
            }
        }

        // Logs List
        if (filteredLogs.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(30.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (filterMode == LogFilterMode.PROBLEMS) Icons.Default.VerifiedUser else Icons.Default.History,
                        contentDescription = null,
                        tint = if (filterMode == LogFilterMode.PROBLEMS) Color(0xFF16A34A) else TextMutedLight,
                        modifier = Modifier.size(44.dp)
                    )
                    Text(
                        text = if (filterMode == LogFilterMode.PROBLEMS) {
                            "لا توجد مشاكل أو أخطاء مسجلة حالياً"
                        } else {
                            "لا توجد رسائل مطابقة في السجل"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = PrimaryNavy
                    )
                    Text(
                        text = "اضغط على زر التحديث بالأعلى لسحب أحدث رسائل الراوتر وتفسيرها بالعربية",
                        fontSize = 11.sp,
                        color = TextSecondaryLight
                    )
                }
            }
        } else {
            val displayed = filteredLogs.take(visibleLimit)
            displayed.forEach { entry ->
                LogEntryCard(entry = entry, showRawOriginal = showRawOriginal)
            }

            if (filteredLogs.size > visibleLimit) {
                OutlinedButton(
                    onClick = { visibleLimit += 60 },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("عرض المزيد (${filteredLogs.size - visibleLimit} سجل متبقٍ)", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            icon = { Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = Color(0xFFDC2626)) },
            title = { Text("مسح سجل رسائل التطبيق؟", fontWeight = FontWeight.Bold) },
            text = { Text("سيتم حذف السجلات والتنبيهات المحفوظة محلياً داخل التطبيق مع الإبقاء على سجل الراوتر المباشر.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAppLogs()
                        showClearConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("نعم، امسح السجل")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showClearConfirm = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
private fun LogEntryCard(
    entry: MikrotikLogEntry,
    showRawOriginal: Boolean
) {
    val (accentColor, bgTint, icon) = when (entry.severity) {
        LogSeverity.CRITICAL -> Triple(Color(0xFF991B1B), Color(0xFFFEF2F2), Icons.Default.GppBad)
        LogSeverity.ERROR -> Triple(Color(0xFFDC2626), Color(0xFFFEF2F2), Icons.Default.ErrorOutline)
        LogSeverity.WARNING -> Triple(Color(0xFFD97706), Color(0xFFFFFBEB), Icons.Default.WarningAmber)
        LogSeverity.INFO -> Triple(Color(0xFF0284C7), Color(0xFFF0F9FF), Icons.Default.Info)
    }

    val severityLabel = when (entry.severity) {
        LogSeverity.CRITICAL -> "حرج"
        LogSeverity.ERROR -> "مشكلة / خطأ"
        LogSeverity.WARNING -> "تحذير"
        LogSeverity.INFO -> "إشعار"
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.25f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                shape = CircleShape,
                color = bgTint,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = severityLabel, tint = accentColor, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = bgTint
                        ) {
                            Text(
                                text = severityLabel,
                                color = accentColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFF1F5F9)
                        ) {
                            Text(
                                text = entry.source,
                                color = PrimaryNavy,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Text(
                            text = entry.topicsAr,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryNavy,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Text(
                        text = entry.time,
                        fontSize = 10.sp,
                        color = TextMutedLight
                    )
                }

                // Arabic Translated Problem / Message
                Text(
                    text = entry.arabicMessage,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    color = TextPrimaryLight,
                    fontWeight = if (entry.severity == LogSeverity.ERROR || entry.severity == LogSeverity.CRITICAL) {
                        FontWeight.SemiBold
                    } else {
                        FontWeight.Normal
                    }
                )

                if (showRawOriginal && entry.originalMessage.isNotBlank() && entry.originalMessage != entry.arabicMessage) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFF8FAFC),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "الأصل: ${entry.originalMessage}",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextSecondaryLight,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
