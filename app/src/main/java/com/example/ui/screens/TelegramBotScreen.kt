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
import com.example.ui.ScreenTab
import com.example.ui.TalalProViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelegramBotScreen(
    viewModel: TalalProViewModel,
    modifier: Modifier = Modifier
) {
    val routers by viewModel.allRouters.collectAsState()
    val telegramConfig by viewModel.telegramConfig.collectAsState()
    val isTesting by viewModel.isTestingTelegram.collectAsState()
    val testResult by viewModel.telegramTestResult.collectAsState()
    val detectedChats by viewModel.detectedTelegramChats.collectAsState()
    val isCloudRunning by viewModel.isTelegramCloudRunning.collectAsState()
    val cloudLogs by viewModel.telegramCloudLogs.collectAsState()
    val clipboardManager = LocalClipboardManager.current

    var botToken by remember(telegramConfig) { mutableStateOf(telegramConfig?.botToken ?: "") }
    var chatId by remember(telegramConfig) { mutableStateOf(telegramConfig?.chatId ?: "") }
    var channelId by remember(telegramConfig) { mutableStateOf(telegramConfig?.channelId ?: "") }
    var isEnabled by remember(telegramConfig) { mutableStateOf(telegramConfig?.isEnabled ?: true) }
    var alertRouter by remember(telegramConfig) { mutableStateOf(telegramConfig?.alertOnRouterOffline ?: true) }
    var alertCpu by remember(telegramConfig) { mutableStateOf(telegramConfig?.alertOnCpuHigh ?: true) }
    var alertHotspot by remember(telegramConfig) { mutableStateOf(telegramConfig?.alertOnHotspotLogin ?: false) }
    var alertBackup by remember(telegramConfig) { mutableStateOf(telegramConfig?.alertOnBackupDone ?: true) }

    var selectedRouterForScript by remember(routers) { mutableStateOf(routers.firstOrNull()) }
    var showGuideDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "ربط وتكامل بوت تليجرام (Telegram Bot)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimaryLight
                        )
                        Text(
                            text = "إرسال تنبيهات الراوترات والأبراج وانقطاع الإنترنت إلى تليجرام فوراً",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondaryLight
                        )
                    }
                },
                actions = {
                    FilledTonalButton(
                        onClick = { viewModel.setTab(ScreenTab.ADVANCED_TELEGRAM) },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color(0xFF10B981).copy(alpha = 0.15f),
                            contentColor = Color(0xFF047857)
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("البوت المتقدم ⚡", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
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
            // PROMINENT ADVANCED BOT BANNER & ENTRY
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF064E3B)),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    modifier = Modifier.clickable { viewModel.setTab(ScreenTab.ADVANCED_TELEGRAM) }
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF10B981),
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.CloudSync, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("قسم بوت تليجرام المتقدم (Pro Cloud)", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFF10B981)) {
                                            Text("جديد ومستقل ⚡", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                        }
                                    }
                                    Text("كشف اشتباه الكروت في جهازين • إدارة ومولد الكروت • محرر الأزرار", fontSize = 11.sp, color = Color(0xFFA7F3D0))
                                }
                            }
                        }

                        Text(
                            text = "شاشة مستقلة تماماً تمكنك من إضافة بوت جديد وإدارته بشكل سحابي من الراوتر مباشرة، مع استعلام الكروت وتوليدها وكشف التطفل ومشاركة الأجهزة.",
                            fontSize = 11.sp,
                            color = Color(0xFFD1FAE5),
                            lineHeight = 16.sp
                        )

                        Button(
                            onClick = { viewModel.setTab(ScreenTab.ADVANCED_TELEGRAM) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981), contentColor = Color.White),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.RocketLaunch, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("فتح قسم وشاشة بوت تليجرام المتقدم 🚀", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }

            // Main Credentials Card
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF0284C7).copy(alpha = 0.12f),
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(20.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("إعدادات رمز البوت والمحادثة", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PrimaryNavy)
                                    Text("أدخل التوكن الخاص ببوتك من @BotFather", fontSize = 11.sp, color = TextSecondaryLight)
                                }
                            }

                            Switch(
                                checked = isEnabled,
                                onCheckedChange = { isEnabled = it },
                                colors = SwitchDefaults.colors(checkedTrackColor = PrimaryBlue)
                            )
                        }

                        HorizontalDivider(color = BorderLight.copy(alpha = 0.6f))

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
                                onClick = { viewModel.fetchTelegramChatIds(botToken) },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                modifier = Modifier.align(Alignment.CenterVertically)
                            ) {
                                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("جلب ID", fontSize = 12.sp)
                            }
                        }

                        // Test Result or Detected Chats
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
                                onClick = { viewModel.testTelegramBot(botToken) },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("فحص الاتصال", fontSize = 12.sp)
                            }

                            Button(
                                onClick = { viewModel.sendTelegramTestAlert(botToken, chatId) },
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

                        // Save Settings Button
                        Button(
                            onClick = {
                                viewModel.saveTelegramSettings(botToken, chatId, channelId, isEnabled, alertRouter, alertCpu, alertHotspot, alertBackup)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("حفظ إعدادات بوت تليجرام", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // ================= CLOUD BOT & INTERACTIVE ARABIC BUTTONS =================
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isCloudRunning) Color(0xFFDCFCE7) else Color(0xFFF1F5F9),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.CloudSync,
                                            contentDescription = null,
                                            tint = if (isCloudRunning) Color(0xFF16A34A) else Color(0xFF64748B),
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "تشغيل البوت السحابي (Cloud Bot) والأزرار التفاعلية",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = PrimaryNavy
                                    )
                                    Text(
                                        text = if (isCloudRunning) "🟢 البوت متصل بالسحابة وينفذ الأوامر لحظياً في المايكروتك" else "🔴 الاستماع السحابي متوقف",
                                        fontSize = 11.sp,
                                        color = if (isCloudRunning) Color(0xFF16A34A) else Color(0xFFEF4444),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        Text(
                            text = "عند تشغيل البوت السحابي، يتم تفعيل الأزرار العربية داخل تليجرام، وبمجرد ضغطك على أي زر في تليجرام يتم تنفيذ الأمر الحقيقي مباشرة في راوتر المايكروتك بدون كتابة أي أوامر يدوية.",
                            fontSize = 12.sp,
                            color = TextSecondaryLight,
                            lineHeight = 18.sp
                        )

                        // 11 Arabic Buttons Preview Card
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF8FAFC),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "الأزرار التفاعلية المعتمدة داخل تليجرام (15 زر):",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryNavy
                                )

                                val buttons = listOf(
                                    Pair("📋 زر القائمة", "عرض قائمة الأزرار والتعليمات"),
                                    Pair("🟢 زر الحالة", "فحص المعالج والذاكرة والحرارة"),
                                    Pair("👥 زر المتصلين", "كشف متصلي الهوتسبوت وPPPoE"),
                                    Pair("📦 زر الباقات", "استعراض باقات وسرعات الراوتر"),
                                    Pair("💾 زر نسخة احتياطية", "حفظ وتصدير ملفات الباك اب"),
                                    Pair("🛡️ زر حماية المواقع", "تفعيل جدار الحماية ضد المواقع"),
                                    Pair("⚡ زر تحسين الشبكة", "تنظيف كاش DNS وسجلات ARP"),
                                    Pair("📊 زر التقرير اليومي", "تقرير شامل لأداء اليوم"),
                                    Pair("💰 زر المبيعات", "تقرير الكروت والمبيعات"),
                                    Pair("📈 زر ملخص الاداء", "ضغط المعالج واستقرار البينج"),
                                    Pair("🌐 زر الاستهلاك اليومي", "ترافيك الرفع والتنزيل MB/GB"),
                                    Pair("📡 زر قطع التوزيع", "مراقبة قطع التوزيع وعدد المتصلين"),
                                    Pair("🔌 زر منافذ الشبكة", "حالة المنافذ المتصلة وغير المتصلة"),
                                    Pair("📊 زر استهلاك المنافذ", "استهلاك الإنترنت لكل منفذ"),
                                    Pair("⚙️ زر تحكم المنافذ", "أوامر وإجراءات التحكم بالمنافذ")
                                )

                                buttons.chunked(2).forEach { row ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        row.forEach { (btnTitle, btnDesc) ->
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = Color.White,
                                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Column(modifier = Modifier.padding(8.dp)) {
                                                    Text(btnTitle, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = PrimaryNavy)
                                                    Text(btnDesc, fontSize = 9.sp, color = TextMutedLight)
                                                }
                                            }
                                        }
                                        if (row.size == 1) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }

                        // Action Controls: Start/Stop Cloud & Send Keypad to Telegram
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.toggleTelegramCloud(selectedRouterForScript)
                                },
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
                                onClick = {
                                    viewModel.sendTelegramInteractiveKeyboard(selectedRouterForScript)
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                modifier = Modifier.weight(1.4f).height(44.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("إرسال الأزرار للتليجرام 📲", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Real-time Cloud Logs Console
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
                                    Text("سجل تنفيذ الأوامر من تليجرام (Live Console):", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isCloudRunning) Color(0xFF22C55E) else Color(0xFFEF4444),
                                        modifier = Modifier.size(8.dp)
                                    ) {}
                                }

                                HorizontalDivider(color = Color(0xFF334155))

                                cloudLogs.takeLast(6).forEach { logLine ->
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

            // Push Script to Router
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("تثبيت سكربت التنبيهات في راوتر MikroTik بنقرة واحدة", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PrimaryNavy)
                        Text("يقوم التطبيق بإنشاء سكربت داخل الراوتر يقوم بالإرسال التلقائي إلى تليجرام عند حدوث أي طارئ", fontSize = 11.sp, color = TextSecondaryLight)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("الراوتر المراد التثبيت فيه:", fontSize = 12.sp, color = TextSecondaryLight)

                            var exp by remember { mutableStateOf(false) }
                            Box {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFF1F5F9),
                                    modifier = Modifier.clickable { exp = true }
                                ) {
                                    Text(
                                        text = selectedRouterForScript?.name ?: "اختر راوتر",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryNavy,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                                DropdownMenu(expanded = exp, onDismissRequest = { exp = false }) {
                                    routers.forEach { r ->
                                        DropdownMenuItem(
                                            text = { Text(r.name) },
                                            onClick = {
                                                selectedRouterForScript = r
                                                exp = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = {
                                selectedRouterForScript?.let { r ->
                                    viewModel.installTelegramScriptToRouter(r, botToken, chatId)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Terminal, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("تثبيت السكربت في الراوتر الآن", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Notification Rules / Triggers
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("قواعد وأحداث الإرسال التلقائي", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PrimaryNavy)

                        SwitchRow(title = "تنبيه عند انقطاع أو عودة اتصال الراوتر", checked = alertRouter, onChecked = { alertRouter = it })
                        HorizontalDivider(color = BorderLight.copy(alpha = 0.5f))
                        SwitchRow(title = "تنبيه عند ارتفاع حرارة أو حمل المعالج (CPU > 85%)", checked = alertCpu, onChecked = { alertCpu = it })
                        HorizontalDivider(color = BorderLight.copy(alpha = 0.5f))
                        SwitchRow(title = "تنبيه عند تسجيل دخول مستخدمين جدد في الهوتسبوت", checked = alertHotspot, onChecked = { alertHotspot = it })
                        HorizontalDivider(color = BorderLight.copy(alpha = 0.5f))
                        SwitchRow(title = "تنبيه عند اكتمال النسخ الاحتياطي التلقائي", checked = alertBackup, onChecked = { alertBackup = it })
                    }
                }
            }
        }
    }

    if (showGuideDialog) {
        TelegramGuideDialog(onDismiss = { showGuideDialog = false })
    }
}

@Composable
private fun SwitchRow(title: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
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
fun TelegramGuideDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("دليل إعداد بوت تليجرام", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PrimaryNavy)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                Text(
                    text = """
                        1️⃣ افتح تطبيق تليجرام وابحث عن <b>@BotFather</b>.
                        2️⃣ أرسل الأمر <code>/newbot</code> ثم اكتب اسم البوت واسم المستخدم.
                        3️⃣ سيظهر لك الرمز <b>(Bot Token)</b>، قم بنسخه ولصقه في التطبيق.
                        4️⃣ افتح البوت الجديد واضغط على <b>Start</b> وأرسل أي رسالة له.
                        5️⃣ اضغط في هذا التطبيق على زر <b>'جلب ID'</b> وسيتم التعرف على معرفك فوراً!
                    """.trimIndent(),
                    fontSize = 12.sp,
                    color = TextSecondaryLight,
                    lineHeight = 20.sp
                )

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("فهمت، إغلاق")
                }
            }
        }
    }
}
