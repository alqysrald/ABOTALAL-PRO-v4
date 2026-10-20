package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.RouterEntity
import com.example.data.model.SuspiciousCardSharingAlert
import com.example.data.model.TelegramCustomButtonEntity
import com.example.ui.TalalProViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedTelegramBotScreen(
    viewModel: TalalProViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val routers by viewModel.allRouters.collectAsState()
    val advancedConfig by viewModel.advancedTelegramConfig.collectAsState()
    val customButtons by viewModel.advancedCustomButtons.collectAsState()
    val isTesting by viewModel.isTestingAdvancedBot.collectAsState()
    val testResult by viewModel.advancedBotTestResult.collectAsState()
    val detectedChats by viewModel.detectedAdvancedChats.collectAsState()
    val isCloudRunning by viewModel.isAdvancedCloudRunning.collectAsState()
    val cloudLogs by viewModel.advancedCloudLogs.collectAsState()
    val suspiciousAlerts by viewModel.suspiciousCardAlerts.collectAsState()
    val isDetectingSuspects by viewModel.isDetectingSuspects.collectAsState()
    val createdCards by viewModel.createdVouchersReport.collectAsState()
    val isCreatingCards by viewModel.isCreatingCards.collectAsState()
    val cardSearchResult by viewModel.cardSearchResult.collectAsState()
    val isSearchingCard by viewModel.isSearchingCard.collectAsState()

    val clipboardManager = LocalClipboardManager.current

    var selectedRouter by remember(routers) { mutableStateOf(routers.firstOrNull()) }
    var selectedSection by remember { mutableIntStateOf(0) }

    // Dialogs state
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showScriptDialog by remember { mutableStateOf(false) }
    var showAddButtonDialog by remember { mutableStateOf(false) }
    var editingButton by remember { mutableStateOf<TelegramCustomButtonEntity?>(null) }
    var showGuideDialog by remember { mutableStateOf(false) }

    // Inputs initialized from saved entity
    var botToken by remember(advancedConfig) { mutableStateOf(advancedConfig?.botToken ?: "") }
    var chatId by remember(advancedConfig) { mutableStateOf(advancedConfig?.chatId ?: "") }
    var botUsername by remember(advancedConfig) { mutableStateOf(advancedConfig?.botUsername ?: "") }
    var botName by remember(advancedConfig) { mutableStateOf(advancedConfig?.botName ?: "TALAL PRO Advanced Bot") }
    var isEnabled by remember(advancedConfig) { mutableStateOf(advancedConfig?.isEnabled ?: true) }
    var detectSharing by remember(advancedConfig) { mutableStateOf(advancedConfig?.detectCardSharing ?: true) }
    var enableCardsMenu by remember(advancedConfig) { mutableStateOf(advancedConfig?.enableCardsMenu ?: true) }
    var enableVouchers by remember(advancedConfig) { mutableStateOf(advancedConfig?.enableVoucherCreation ?: true) }
    var enablePorts by remember(advancedConfig) { mutableStateOf(advancedConfig?.enablePortControls ?: true) }
    var enableAutoProtection by remember(advancedConfig) { mutableStateOf(advancedConfig?.enableAutoProtection ?: true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "بوت تليجرام المتقدم (Pro Cloud)",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimaryLight
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF10B981).copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "مستقل ⚡",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF059669),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "محرك سحابي مستقل، كشف مشاركة الكروت، وتوليد وتحكم تلقائي",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondaryLight
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = PrimaryNavy)
                    }
                },
                actions = {
                    IconButton(onClick = { showGuideDialog = true }) {
                        Icon(Icons.Default.HelpOutline, contentDescription = "دليل الاستخدام", tint = PrimaryBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF8FAFC),
        modifier = modifier
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header: Selected Router & Cloud Status Pill
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isCloudRunning) Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.CloudSync,
                                            contentDescription = null,
                                            tint = if (isCloudRunning) Color(0xFF16A34A) else Color(0xFFDC2626),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("حالة المحرك السحابي للبوت المتقدم", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = PrimaryNavy)
                                    Text(
                                        text = if (isCloudRunning) "🟢 البوت متصل وينفذ الأوامر لحظياً في المايكروتك" else "🔴 الاستماع السحابي متوقف",
                                        fontSize = 11.sp,
                                        color = if (isCloudRunning) Color(0xFF16A34A) else Color(0xFFDC2626),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            // Router selection menu
                            var exp by remember { mutableStateOf(false) }
                            Box {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFF1F5F9),
                                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                    modifier = Modifier.clickable { exp = true }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Router, contentDescription = null, modifier = Modifier.size(14.dp), tint = PrimaryBlue)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(selectedRouter?.name ?: "اختر راوتر", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryNavy)
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF64748B))
                                    }
                                }
                                DropdownMenu(expanded = exp, onDismissRequest = { exp = false }) {
                                    routers.forEach { r ->
                                        DropdownMenuItem(
                                            text = { Text(r.name) },
                                            onClick = {
                                                selectedRouter = r
                                                viewModel.selectedAdvancedRouter.value = r
                                                exp = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Independent Bot Notice Banner
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFEFF6FF),
                            border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "هذا التبويب مستقل تماماً عن البوت السابق، يمكنك إنشاء بوت تليجرام جديد أو ربط بوت خاص بالعمليات المتقدمة، مع خيار حذف البوت وتنظيف الراوتر وتليجرام بنقرة واحدة.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF1E40AF),
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }
            }

            // Navigation Tabs
            item {
                val tabs = listOf(
                    "⚙️ إعدادات البوت",
                    "☁️ المحرك والسكربت",
                    "⚠️ مراقبة الاشتباه",
                    "🎫 إدارة الكروت",
                    "🎛️ محرر الأزرار"
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(tabs.indices.toList()) { index ->
                        val isSelected = selectedSection == index
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedSection = index },
                            label = { Text(tabs[index], fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryBlue,
                                selectedLabelColor = Color.White,
                                containerColor = Color.White,
                                labelColor = PrimaryNavy
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) PrimaryBlue else Color(0xFFCBD5E1)
                            )
                        )
                    }
                }
            }

            // SECTION 0: Bot Credentials & Deletion
            if (selectedSection == 0) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("بيانات واعتماد بوت تليجرام المستقل", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PrimaryNavy)
                                    Text("أدخل التوكن الخاص بالبوت المتقدم من @BotFather", fontSize = 11.sp, color = TextSecondaryLight)
                                }
                                Switch(
                                    checked = isEnabled,
                                    onCheckedChange = { isEnabled = it },
                                    colors = SwitchDefaults.colors(checkedTrackColor = PrimaryBlue)
                                )
                            }

                            HorizontalDivider(color = BorderLight.copy(alpha = 0.6f))

                            OutlinedTextField(
                                value = botName,
                                onValueChange = { botName = it },
                                label = { Text("اسم البوت المتقدم") },
                                placeholder = { Text("مثال: بوت طلال الذكي") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true,
                                leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, tint = PrimaryBlue) }
                            )

                            OutlinedTextField(
                                value = botToken,
                                onValueChange = { botToken = it },
                                label = { Text("رمز البوت (Bot Token) *") },
                                placeholder = { Text("123456789:ABCdefGhIJKlmNoPQRsTUVwxyZ") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true,
                                leadingIcon = { Icon(Icons.Default.Key, contentDescription = null, tint = PrimaryBlue) }
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = chatId,
                                    onValueChange = { chatId = it },
                                    label = { Text("معرف المحادثة (Chat ID) *") },
                                    placeholder = { Text("مثال: 123456789") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    singleLine = true
                                )

                                Button(
                                    onClick = { viewModel.fetchAdvancedTelegramChatIds(botToken) },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                    modifier = Modifier.align(Alignment.CenterVertically)
                                ) {
                                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("جلب ID", fontSize = 12.sp)
                                }
                            }

                            // Detected Chats
                            if (detectedChats.isNotEmpty()) {
                                Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFEFF6FF), modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("المحادثات المكتشفة من البوت (اضغط لاختيار ID):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryNavy)
                                        detectedChats.forEach { c ->
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color.White,
                                                modifier = Modifier.fillMaxWidth().clickable { chatId = c.chatId }
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text("${c.titleOrUsername} (${c.type})", fontSize = 12.sp, color = TextPrimaryLight)
                                                    Text(c.chatId, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Action Buttons: Test Connection & Send Test Alert
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.testAdvancedTelegramBot(botToken) },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("فحص الاتصال", fontSize = 12.sp)
                                }

                                Button(
                                    onClick = { viewModel.sendAdvancedTelegramTestAlert(botToken, chatId) },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("إرسال تجريبي", fontSize = 12.sp)
                                }
                            }

                            testResult?.let { msg ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (msg.contains("✅")) Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = msg,
                                        color = if (msg.contains("✅")) Color(0xFF047857) else Color(0xFFB91C1C),
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }

                            // Save Button
                            Button(
                                onClick = {
                                    viewModel.saveAdvancedTelegramSettings(
                                        botToken = botToken,
                                        chatId = chatId,
                                        botUsername = botUsername,
                                        botName = botName,
                                        isEnabled = isEnabled,
                                        detectCardSharing = detectSharing,
                                        enableCardsMenu = enableCardsMenu,
                                        enableVoucherCreation = enableVouchers,
                                        enablePortControls = enablePorts,
                                        enableAutoProtection = enableAutoProtection
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("حفظ إعدادات البوت المتقدم", fontWeight = FontWeight.Bold)
                            }

                            // COMPLETE BOT DELETION BUTTON
                            OutlinedButton(
                                onClick = { showDeleteConfirmDialog = true },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                                border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.DeleteForever, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("حذف البوت المتقدم بالكامل وتنظيف الراوتر وتليجرام", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // SECTION 1: Cloud Engine & Autonomous Router Script
            if (selectedSection == 1) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text("تشغيل المحرك السحابي والتلقائي (Cloud Autonomous Engine)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PrimaryNavy)
                            Text(
                                text = "يمكن للبوت العمل عبر الكلاود باستمرار حتى لو كان التطبيق مغلقاً؛ حيث يتم تثبيت سكربت وجدولة داخل راوتر MikroTik ليقوم الراوتر نفسه بمراقبة الكروت والأجهزة وإرسال التنبيهات والأوامر مباشرة لتليجرام.",
                                fontSize = 12.sp,
                                color = TextSecondaryLight,
                                lineHeight = 18.sp
                            )

                            // Start/Stop Cloud & Send Interactive Keypad
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.toggleAdvancedTelegramCloud(selectedRouter) },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isCloudRunning) Color(0xFFEF4444) else Color(0xFF16A34A)
                                    ),
                                    modifier = Modifier.weight(1.2f).height(44.dp)
                                ) {
                                    Icon(
                                        if (isCloudRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isCloudRunning) "إيقاف السحابة" else "تشغيل البوت السحابي",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Button(
                                    onClick = { viewModel.sendAdvancedInteractiveKeyboard(selectedRouter) },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                    modifier = Modifier.weight(1.4f).height(44.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("إرسال الأزرار للتليجرام 📲", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            HorizontalDivider(color = BorderLight.copy(alpha = 0.6f))

                            // Router Script Installation Actions
                            Text("تثبيت السكربت التلقائي في راوتر MikroTik:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = PrimaryNavy)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        selectedRouter?.let { r ->
                                            viewModel.installAdvancedRouterScript(r, botToken, chatId, detectSharing)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("تثبيت السكربت بالراوتر ⚡", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = { showScriptDialog = true },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("عرض ونسخ السكربت 📄", fontSize = 11.sp)
                                }
                            }

                            // Live Cloud Console
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF0F172A),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("سجل أحداث وتنفيذ البوت المتقدم (Live Cloud Console):", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Surface(
                                            shape = CircleShape,
                                            color = if (isCloudRunning) Color(0xFF22C55E) else Color(0xFFEF4444),
                                            modifier = Modifier.size(8.dp)
                                        ) {}
                                    }

                                    HorizontalDivider(color = Color(0xFF334155))

                                    cloudLogs.takeLast(7).forEach { logLine ->
                                        Text(
                                            text = logLine,
                                            color = if (logLine.contains("✅")) Color(0xFF4ADE80) else if (logLine.contains("🚀")) Color(0xFF38BDF8) else Color(0xFFE2E8F0),
                                            fontSize = 11.sp,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // SECTION 2: Suspicious Card Sharing Detection
            if (selectedSection == 2) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("مراقبة اشتباه مشاركة الكارت (Multi-Device Suspect)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PrimaryNavy)
                                    Text("كشف الكروت المسجلة في جهازين مختلفين (MACs) خلال 24 ساعة", fontSize = 11.sp, color = TextSecondaryLight)
                                }
                                Switch(
                                    checked = detectSharing,
                                    onCheckedChange = {
                                        detectSharing = it
                                        viewModel.toggleFeatureSetting("detectCardSharing", it)
                                    },
                                    colors = SwitchDefaults.colors(checkedTrackColor = PrimaryBlue)
                                )
                            }

                            // Trigger manual suspect detection scan
                            Button(
                                onClick = { viewModel.detectCardSharingSuspectsNow(selectedRouter) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isDetectingSuspects
                            ) {
                                if (isDetectingSuspects) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.WarningAmber, contentDescription = null)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("فحص الكروت المشتبه بها الآن في الراوتر 🔍", fontWeight = FontWeight.Bold)
                            }

                            // List of Detected Suspect Cards
                            if (suspiciousAlerts.isNotEmpty()) {
                                Text("الكروت المرصودة حالياً (${suspiciousAlerts.size} كرت):", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFDC2626))
                                suspiciousAlerts.forEach { alert ->
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFFFFF1F2),
                                        border = BorderStroke(1.dp, Color(0xFFFECDD3)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFE11D48), modifier = Modifier.size(18.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("اشتباه مشاركة كارت", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF9F1239))
                                                }
                                                Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFE11D48).copy(alpha = 0.15f)) {
                                                    Text("${alert.macAddresses.size} أجهزة", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF9F1239), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                                }
                                            }

                                            Text("الكارت: <b>${alert.cardUsername}</b>", fontSize = 12.sp, color = TextPrimaryLight)
                                            Text("ظهر على ${alert.macAddresses.size} أجهزة خلال آخر 24 ساعة:", fontSize = 11.sp, color = TextSecondaryLight)
                                            alert.macAddresses.forEach { mac ->
                                                Text("• <code>$mac</code>", fontSize = 11.sp, color = PrimaryNavy, fontWeight = FontWeight.Medium)
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Button(
                                                    onClick = { viewModel.disableSuspectedCard(alert.cardUsername, selectedRouter) },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("إيقاف الكرت وفصل الجلسة", fontSize = 11.sp)
                                                }

                                                OutlinedButton(
                                                    onClick = { viewModel.deleteCardNow(alert.cardUsername, selectedRouter) },
                                                    shape = RoundedCornerShape(8.dp),
                                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF64748B)),
                                                    modifier = Modifier.weight(0.7f)
                                                ) {
                                                    Text("حذف الكرت", fontSize = 11.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFF1F5F9), modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = "✅ لا توجد أي كروت مشتبه بمشاركتها عبر أجهزة متعددة حالياً في راوتر [${selectedRouter?.name ?: "الرئيسي"}].",
                                        fontSize = 12.sp,
                                        color = Color(0xFF475569),
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // SECTION 3: Cards Management Hub (Create & Search)
            if (selectedSection == 3) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text("إدارة ومولد الكروت التفاعلي (Cards Hub 🎫)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PrimaryNavy)

                            // Quick Search Card
                            var cardSearchText by remember { mutableStateOf("") }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = cardSearchText,
                                    onValueChange = { cardSearchText = it },
                                    label = { Text("بحث واستعلام عن كرت معين") },
                                    placeholder = { Text("أدخل رقم الكرت...") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    singleLine = true
                                )
                                Button(
                                    onClick = { viewModel.searchCardNow(cardSearchText, selectedRouter) },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                                    modifier = Modifier.align(Alignment.CenterVertically)
                                ) {
                                    Icon(Icons.Default.Search, contentDescription = null)
                                }
                            }

                            // Card search result display
                            cardSearchResult?.let { res ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFF0FDF4),
                                    border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("نتيجة الاستعلام عن الكرت [${res["username"]}]:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF166534))
                                        Text("النظام: ${res["system"]} | الفئة: ${res["profile"]} | الحالة: ${if (res["disabled"] == true) "🔴 معطل" else "🟢 فعال"}", fontSize = 11.sp, color = TextPrimaryLight)
                                        Text("مدة العمل: ${res["uptime"]} | التحميل: ${res["bytesIn"]} | الرفع: ${res["bytesOut"]}", fontSize = 11.sp, color = TextSecondaryLight)
                                        if (res["isOnline"] == true) {
                                            Text("🟢 متصل الآن بالراوتر! الماك: ${res["currentMac"]}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D))
                                        }

                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Button(
                                                onClick = { viewModel.disableSuspectedCard(res["username"].toString(), selectedRouter) },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text("إيقاف الكرت", fontSize = 11.sp)
                                            }
                                            Button(
                                                onClick = { viewModel.enableCardNow(res["username"].toString(), selectedRouter) },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text("تفعيل الكرت", fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(color = BorderLight.copy(alpha = 0.5f))

                            // Interactive In-App Card Generator Flow
                            Text("توليد كروت جديدة في الراوتر بنقرة واحدة (🎫 إنشاء كروت):", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = PrimaryNavy)

                            var selectedSystem by remember { mutableStateOf("HOTSPOT") }
                            var selectedProfile by remember { mutableStateOf("default") }
                            var countText by remember { mutableStateOf("50") }
                            var validityDaysText by remember { mutableStateOf("") }
                            var isUnlimitedDays by remember { mutableStateOf(true) }

                            // System selector
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = selectedSystem == "HOTSPOT",
                                    onClick = { selectedSystem = "HOTSPOT" },
                                    label = { Text("هوتسبوت (Hotspot)") },
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = selectedSystem == "USER_MANAGER",
                                    onClick = { selectedSystem = "USER_MANAGER" },
                                    label = { Text("يوزرمانجر (User Manager)") },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            OutlinedTextField(
                                value = selectedProfile,
                                onValueChange = { selectedProfile = it },
                                label = { Text("اسم البروفايل (Profile) الفعلي بالراوتر *") },
                                placeholder = { Text("مثال: 200RY أو 1G") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true
                            )

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = countText,
                                    onValueChange = { countText = it },
                                    label = { Text("العدد (من 1 إلى 500) *") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    singleLine = true
                                )

                                OutlinedTextField(
                                    value = if (isUnlimitedDays) "بدون تحديد" else validityDaysText,
                                    onValueChange = {
                                        validityDaysText = it
                                        isUnlimitedDays = false
                                    },
                                    label = { Text("صلاحية الأيام") },
                                    enabled = !isUnlimitedDays,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    singleLine = true
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = isUnlimitedDays,
                                    onCheckedChange = { isUnlimitedDays = it }
                                )
                                Text("بدون حد للمدة (صلاحية مفتوحة)", fontSize = 12.sp, color = TextPrimaryLight)
                            }

                            Button(
                                onClick = {
                                    val count = countText.toIntOrNull() ?: 10
                                    val days = if (isUnlimitedDays) null else validityDaysText.toIntOrNull()
                                    viewModel.batchCreateCardsNow(selectedRouter, selectedSystem, selectedProfile, count, days)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().height(46.dp),
                                enabled = !isCreatingCards
                            ) {
                                if (isCreatingCards) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.AddCard, contentDescription = null)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("إنشاء الكروت في الراوتر الآن 🚀", fontWeight = FontWeight.Bold)
                            }

                            // Created cards report
                            if (createdCards.isNotEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFF5F3FF),
                                    border = BorderStroke(1.dp, Color(0xFFDDD6FE)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("✅ تم إنشاء ${createdCards.size} كرت بنجاح في راوتر [${selectedRouter?.name ?: "الرئيسي"}]:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF6D28D9))
                                        createdCards.take(8).forEach { (code, pin) ->
                                            Text("• كود: <code>$code</code> | PIN: <code>$pin</code>", fontSize = 11.sp, color = PrimaryNavy)
                                        }
                                        if (createdCards.size > 8) {
                                            Text("+ تم إنشاء باقي الكروت بنجاح.", fontSize = 10.sp, color = TextSecondaryLight)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // SECTION 4: Custom Buttons Editor & Feature Toggles
            if (selectedSection == 4) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("محرر الأزرار المتقدمة وتخصيصها (Buttons Editor)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PrimaryNavy)
                                    Text("إضافة أزرار جديدة، تعديل الأوامر، وتفعيل أو إيقاف أي ميزة", fontSize = 11.sp, color = TextSecondaryLight)
                                }
                                Button(
                                    onClick = { showAddButtonDialog = true },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("زر جديد", fontSize = 11.sp)
                                }
                            }

                            HorizontalDivider(color = BorderLight.copy(alpha = 0.5f))

                            // Features Switches
                            Text("مفاتيح تشغيل وإيقاف الخصائص:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PrimaryNavy)
                            SwitchRowItem(title = "كشف مشاركة الكروت في جهازين (Suspect Detection)", checked = detectSharing, onChecked = {
                                detectSharing = it
                                viewModel.toggleFeatureSetting("detectCardSharing", it)
                            })
                            SwitchRowItem(title = "قائمة وخيارات الكروت (Cards Menu Hub)", checked = enableCardsMenu, onChecked = {
                                enableCardsMenu = it
                                viewModel.toggleFeatureSetting("enableCardsMenu", it)
                            })
                            SwitchRowItem(title = "توليد الكروت التفاعلي (Voucher Creation Flow)", checked = enableVouchers, onChecked = {
                                enableVouchers = it
                                viewModel.toggleFeatureSetting("enableVoucherCreation", it)
                            })
                            SwitchRowItem(title = "أوامر تحكم المنافذ (Port Controls)", checked = enablePorts, onChecked = {
                                enablePorts = it
                                viewModel.toggleFeatureSetting("enablePortControls", it)
                            })

                            HorizontalDivider(color = BorderLight.copy(alpha = 0.5f))

                            // Buttons List
                            Text("الأزرار التفاعلية المعتمدة في البوت المتقدم (${customButtons.size} زر):", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PrimaryNavy)

                            customButtons.forEach { btn ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (btn.isEnabled) Color.White else Color(0xFFF8FAFC),
                                    border = BorderStroke(1.dp, if (btn.isEnabled) Color(0xFFE2E8F0) else Color(0xFFCBD5E1)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(btn.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (btn.isEnabled) PrimaryNavy else Color(0xFF94A3B8))
                                                if (btn.isBuiltIn) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFE0F2FE)) {
                                                        Text("أساسي", fontSize = 9.sp, color = Color(0xFF0369A1), modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                                    }
                                                }
                                            }
                                            Text("الأمر: <code>${btn.command}</code> • ${btn.description}", fontSize = 11.sp, color = TextSecondaryLight)
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(onClick = { editingButton = btn }, modifier = Modifier.size(32.dp)) {
                                                Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                                            }
                                            if (!btn.isBuiltIn) {
                                                IconButton(onClick = { viewModel.deleteCustomButton(btn) }, modifier = Modifier.size(32.dp)) {
                                                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                                                }
                                            }
                                            Switch(
                                                checked = btn.isEnabled,
                                                onCheckedChange = { viewModel.toggleButtonEnabled(btn, it) },
                                                colors = SwitchDefaults.colors(checkedTrackColor = PrimaryBlue),
                                                modifier = Modifier.size(36.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("حذف بوت تليجرام المتقدم بالكامل؟", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "سيتم حذف البوت المتقدم المستقل وجميع إعداداته وأزراره المخصصة، وإلغاء الـ Webhook من تليجرام، وإزالة السكربت والجدولة من راوتر المايكروتك تماماً.",
                    fontSize = 13.sp,
                    color = Color(0xFF475569)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAdvancedTelegramBot(selectedRouter)
                        showDeleteConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("نعم، احذف البوت ونظف كل شيء")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Script Preview Dialog
    if (showScriptDialog) {
        val scriptText = viewModel.getAdvancedRouterScriptPreview(botToken, chatId, selectedRouter, detectSharing)
        Dialog(onDismissRequest = { showScriptDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("سكربت المايكروتك السحابي التلقائي", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PrimaryNavy)
                        IconButton(onClick = { showScriptDialog = false }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "إغلاق")
                        }
                    }

                    Text("انسخ هذا الكود والصقه في Terminal الراوتر للتثبيت اليدوي، أو اضغط زر التثبيت المباشر بالراوتر.", fontSize = 11.sp, color = TextSecondaryLight)

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF0F172A),
                        modifier = Modifier.fillMaxWidth().heightIn(max = 260.dp)
                    ) {
                        LazyColumn(modifier = Modifier.padding(10.dp)) {
                            item {
                                Text(scriptText, color = Color(0xFF38BDF8), fontSize = 10.sp, lineHeight = 15.sp)
                            }
                        }
                    }

                    Button(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(scriptText))
                            viewModel.snackbarMessage.value = "تم نسخ السكربت للحافظة بنجاح ✓"
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("نسخ السكربت للحافظة", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Add / Edit Button Dialog
    if (showAddButtonDialog || editingButton != null) {
        AddEditCustomButtonDialog(
            existing = editingButton,
            onDismiss = {
                showAddButtonDialog = false
                editingButton = null
            },
            onSave = { btn ->
                viewModel.saveCustomButton(btn)
                showAddButtonDialog = false
                editingButton = null
            }
        )
    }

    // Guide Dialog
    if (showGuideDialog) {
        TelegramGuideDialog(onDismiss = { showGuideDialog = false })
    }
}

@Composable
private fun SwitchRowItem(title: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 12.sp, color = TextPrimaryLight, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChecked, colors = SwitchDefaults.colors(checkedTrackColor = PrimaryBlue))
    }
}

@Composable
fun AddEditCustomButtonDialog(
    existing: TelegramCustomButtonEntity?,
    onDismiss: () -> Unit,
    onSave: (TelegramCustomButtonEntity) -> Unit
) {
    var title by remember { mutableStateOf(existing?.title ?: "") }
    var command by remember { mutableStateOf(existing?.command ?: "") }
    var description by remember { mutableStateOf(existing?.description ?: "") }
    var actionType by remember { mutableStateOf(existing?.actionType ?: "TEXT") }
    var payload by remember { mutableStateOf(existing?.payload ?: "") }
    var isEnabled by remember { mutableStateOf(existing?.isEnabled ?: true) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (existing == null) "إضافة زر تفاعلي جديد" else "تعديل الزر التفاعلي",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = PrimaryNavy
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("اسم الزر الذي يظهر في تليجرام *") },
                    placeholder = { Text("مثال: ⚡ ريستارت الراوتر") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = command,
                    onValueChange = { command = it },
                    label = { Text("نص الأمر أو الأمر البرمجي *") },
                    placeholder = { Text("مثال: ريستارت أو /reboot") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )

                Text("نوع الإجراء المرتبط بالزر:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryNavy)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = actionType == "TEXT",
                        onClick = { actionType = "TEXT" },
                        label = { Text("رد نصي", fontSize = 10.sp) }
                    )
                    FilterChip(
                        selected = actionType == "ROUTER_COMMAND",
                        onClick = { actionType = "ROUTER_COMMAND" },
                        label = { Text("أمر راوتر", fontSize = 10.sp) }
                    )
                    FilterChip(
                        selected = actionType == "SUB_MENU_CARDS",
                        onClick = { actionType = "SUB_MENU_CARDS" },
                        label = { Text("قائمة كروت", fontSize = 10.sp) }
                    )
                }

                OutlinedTextField(
                    value = payload,
                    onValueChange = { payload = it },
                    label = { Text("محتوى الرد أو أمر الراوتر (Payload)") },
                    placeholder = { Text(if (actionType == "TEXT") "اكتب نص الرد..." else "مثال: /system reboot") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    minLines = 2
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("وصف مختصر للزر") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("إلغاء")
                    }
                    Button(
                        onClick = {
                            if (title.isNotBlank() && command.isNotBlank()) {
                                onSave(
                                    TelegramCustomButtonEntity(
                                        id = existing?.id ?: "btn_${System.currentTimeMillis()}",
                                        title = title.trim(),
                                        command = command.trim(),
                                        description = description.trim(),
                                        actionType = actionType,
                                        payload = payload.trim(),
                                        isEnabled = isEnabled,
                                        isBuiltIn = existing?.isBuiltIn ?: false,
                                        sortOrder = existing?.sortOrder ?: 99
                                    )
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("حفظ الزر")
                    }
                }
            }
        }
    }
}
