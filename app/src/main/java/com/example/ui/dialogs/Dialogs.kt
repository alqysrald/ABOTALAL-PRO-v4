package com.example.ui.dialogs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.*
import com.example.ui.TalalProViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRouterDialog(
    viewModel: TalalProViewModel,
    onDismiss: () -> Unit
) {
    var connectionType by remember { mutableStateOf(ConnectionType.CLOUD) }
    var routerName by remember { mutableStateOf("") }
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("8728") }
    var username by remember { mutableStateOf("admin") }
    var password by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("15.369,44.191") }
    var localIpFallback by remember { mutableStateOf("10.0.0.1") }
    var localPort by remember { mutableStateOf("8728") }

    var isTesting by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }
    var testSuccess by remember { mutableStateOf<Boolean?>(null) }
    val scope = rememberCoroutineScope()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF1E293B))),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "إضافة راوتر MikroTik",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color(0xFF94A3B8))
                    }
                }

                // Connection Type Selector
                Text(
                    text = "نوع الاتصال",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8),
                    modifier = Modifier.padding(top = 4.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ConnectionTypeTab(
                        title = "كلاود",
                        icon = Icons.Default.Cloud,
                        isSelected = connectionType == ConnectionType.CLOUD,
                        onClick = { connectionType = ConnectionType.CLOUD },
                        modifier = Modifier.weight(1f)
                    )
                    ConnectionTypeTab(
                        title = "IP عام",
                        icon = Icons.Default.Language,
                        isSelected = connectionType == ConnectionType.PUBLIC_IP,
                        onClick = { connectionType = ConnectionType.PUBLIC_IP },
                        modifier = Modifier.weight(1f)
                    )
                    ConnectionTypeTab(
                        title = "SSTP",
                        icon = Icons.Default.VpnKey,
                        isSelected = connectionType == ConnectionType.SSTP,
                        onClick = { connectionType = ConnectionType.SSTP },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Connection Type Description Pill
                val desc = when (connectionType) {
                    ConnectionType.CLOUD -> "اتصال مباشر عبر عنوان RouterOS Cloud — لا يحتاج إلى وكيل أو نفق VPN"
                    ConnectionType.PUBLIC_IP -> "IP عام أو DDNS مع فتح المنفذ 8728 في الجدار الناري للراوتر"
                    ConnectionType.SSTP -> "ربط عبر خادم SSTP — أدخل عنوان الخادم وبيانات حساب VPN"
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1E293B))
                        .padding(10.dp)
                ) {
                    Text(
                        text = desc,
                        fontSize = 11.sp,
                        color = Color(0xFF93C5FD),
                        lineHeight = 16.sp
                    )
                }

                // Router Name Field
                OutlinedTextField(
                    value = routerName,
                    onValueChange = { routerName = it },
                    label = { Text("اسم الراوتر / الفرع", color = Color(0xFF94A3B8)) },
                    singleLine = true,
                    colors = dialogTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                // Host Field (Cloud DNS or IP)
                val hostLabel = when (connectionType) {
                    ConnectionType.CLOUD -> "Cloud DNS (مثال: xxxx.sn.mynetname.net)"
                    ConnectionType.PUBLIC_IP -> "IP العام / DDNS"
                    ConnectionType.SSTP -> "عنوان خادم VPN / DDNS"
                }
                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it },
                    label = { Text(hostLabel, color = Color(0xFF94A3B8)) },
                    singleLine = true,
                    colors = dialogTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                // User & Password Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("اسم المستخدم", color = Color(0xFF94A3B8)) },
                        singleLine = true,
                        colors = dialogTextFieldColors(),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("كلمة المرور", color = Color(0xFF94A3B8)) },
                        singleLine = true,
                        colors = dialogTextFieldColors(),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Port & Location Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = port,
                        onValueChange = { port = it },
                        label = { Text("المنفذ (8728 / 8729)", color = Color(0xFF94A3B8)) },
                        singleLine = true,
                        colors = dialogTextFieldColors(),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("الموقع lat,lng", color = Color(0xFF94A3B8)) },
                        singleLine = true,
                        colors = dialogTextFieldColors(),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Local IP Fallback Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "🏠 IP محلي احتياطي (اختياري)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF67E8F9)
                        )
                        Text(
                            text = "إذا أضفت IP محلياً — يتصل تلقائياً محلياً داخل الشبكة، وكلاود خارجها.",
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = localIpFallback,
                                onValueChange = { localIpFallback = it },
                                label = { Text("IP المحلي (مثال: 10.0.0.1)", color = Color(0xFF94A3B8)) },
                                singleLine = true,
                                colors = dialogTextFieldColors(),
                                modifier = Modifier.weight(2f)
                            )
                            OutlinedTextField(
                                value = localPort,
                                onValueChange = { localPort = it },
                                label = { Text("المنفذ", color = Color(0xFF94A3B8)) },
                                singleLine = true,
                                colors = dialogTextFieldColors(),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Test Connection Feedback
                AnimatedVisibility(visible = testResult != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (testSuccess == true) Color(0xFF064E3B) else Color(0xFF7F1D1D)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = testResult ?: "",
                            fontSize = 12.sp,
                            color = Color.White,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Cancel
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFCBD5E1)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("إلغاء")
                    }

                    // Test Connection Button
                    Button(
                        onClick = {
                            if (host.isBlank()) {
                                testResult = "يرجى كتابة عنوان الراوتر أولاً"
                                testSuccess = false
                                return@Button
                            }
                            isTesting = true
                            testResult = null
                            scope.launch {
                                val res = viewModel.testRouterConnection(
                                    host = host,
                                    port = port.toIntOrNull() ?: 8728,
                                    username = username,
                                    pass = password
                                )
                                isTesting = false
                                testSuccess = res.isSuccess
                                testResult = if (res.isSuccess) {
                                    "✓ تم الاتصال بنجاح (${res.pingTimeMs}ms)"
                                } else {
                                    "✗ فشل الاتصال: ${res.errorMessage}"
                                }
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D4ED8)),
                        modifier = Modifier.weight(1.2f)
                    ) {
                        if (isTesting) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text("فحص الاتصال")
                        }
                    }

                    // Save Button
                    Button(
                        onClick = {
                            if (routerName.isBlank() || host.isBlank()) {
                                testResult = "يرجى كتابة اسم الراوتر والعنوان"
                                testSuccess = false
                                return@Button
                            }
                            viewModel.addRouter(
                                name = routerName,
                                connectionType = connectionType,
                                host = host,
                                port = port.toIntOrNull() ?: 8728,
                                username = username,
                                password = password,
                                location = location,
                                localIpFallback = localIpFallback,
                                localPort = localPort.toIntOrNull() ?: 8728
                            )
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("حفظ", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ConnectionTypeTab(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) Color(0xFF1E3A8A) else Color(0xFF1E293B),
        border = borderIf(isSelected, Color(0xFF38BDF8), 1.5.dp),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isSelected) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
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
fun CloudGuideDialog(
    onDismiss: () -> Unit,
    onNavigateToAddRouter: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    var copiedText by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF1E293B))),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📡 تفعيل MikroTik Cloud",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color(0xFF94A3B8))
                    }
                }

                Text(
                    text = "بعد التفعيل يحصل الراوتر على اسم DNS عام، ويتصل التطبيق به من أي مكان بدون وكيل.",
                    fontSize = 12.sp,
                    color = Color(0xFFCBD5E1),
                    lineHeight = 18.sp
                )

                // Step 1
                GuideStepCard(
                    stepNum = "١",
                    title = "افتح Winbox أو SSH على الراوتر",
                    subtitle = "الراوتر المحلي: 10.0.0.1 أو عنوان IP الحالي"
                )

                // Step 2
                GuideStepCard(
                    stepNum = "٢",
                    title = "فعّل MikroTik Cloud DDNS",
                    command = "/ip cloud set ddns-enabled=yes",
                    hint = "ينتظر 10-30 ثانية ريثما يتصل بخادم كلاود",
                    onCopy = {
                        clipboardManager.setText(AnnotatedString("/ip cloud set ddns-enabled=yes"))
                        copiedText = "تم نسخ أمر تفعيل الكلاود"
                    }
                )

                // Step 3
                GuideStepCard(
                    stepNum = "٣",
                    title = "احصل على اسم DNS العام",
                    command = "/ip cloud print",
                    hint = "انسخ قيمة dns-name — تبدو هكذا: xxxx.sn.mynetname.net",
                    onCopy = {
                        clipboardManager.setText(AnnotatedString("/ip cloud print"))
                        copiedText = "تم نسخ أمر طباعة الكلاود"
                    }
                )

                // Step 4
                GuideStepCard(
                    stepNum = "٤",
                    title = "حدّث الراوتر في التطبيق",
                    subtitle = "اضغط إضافة أو تعديل الراوتر — ضع اسم DNS في حقل الكلاود"
                )

                // Firewall Notice
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "💡 ملاحظة مهمة (فتح منفذ API 8728):",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFBBF24)
                        )
                        Text(
                            text = "تأكد أن المنفذ 8728 مفتوح في firewall الراوتر:",
                            fontSize = 11.sp,
                            color = Color(0xFFCBD5E1)
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF0F172A),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val cmd = "/ip firewall filter add chain=input protocol=tcp dst-port=8728 action=accept"
                                    clipboardManager.setText(AnnotatedString(cmd))
                                    copiedText = "تم نسخ أمر فتح المنفذ"
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = "/ip firewall filter add chain=input protocol=tcp dst-port=8728 action=accept",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF38BDF8),
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }

                AnimatedVisibility(visible = copiedText != null) {
                    Text(
                        text = copiedText ?: "",
                        fontSize = 11.sp,
                        color = Color(0xFF34D399),
                        modifier = Modifier.padding(4.dp)
                    )
                }

                // Big Action Button
                Button(
                    onClick = {
                        onDismiss()
                        onNavigateToAddRouter()
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("✏️ تعديل الراوتر الآن ← تغيير IP إلى DNS الكلاود", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun GuideStepCard(
    stepNum: String,
    title: String,
    subtitle: String? = null,
    command: String? = null,
    hint: String? = null,
    onCopy: (() -> Unit)? = null
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF3B82F6))
            ) {
                Text(text = stepNum, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                if (subtitle != null) {
                    Text(text = subtitle, fontSize = 11.sp, color = Color(0xFF94A3B8))
                }
                if (command != null) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF0F172A),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCopy?.invoke() }
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = command,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF34D399)
                            )
                            Icon(Icons.Default.ContentCopy, contentDescription = "نسخ", tint = Color(0xFF94A3B8), modifier = Modifier.size(14.dp))
                        }
                    }
                }
                if (hint != null) {
                    Text(text = hint, fontSize = 10.sp, color = Color(0xFF94A3B8))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDeviceDialog(
    viewModel: TalalProViewModel,
    routers: List<RouterEntity>,
    onDismiss: () -> Unit
) {
    var deviceName by remember { mutableStateOf("") }
    var ip by remember { mutableStateOf("") }
    var manager by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("15.369,44.191") }
    var notes by remember { mutableStateOf("") }
    var selectedRouterId by remember { mutableStateOf<Long?>(null) }
    var deviceType by remember { mutableStateOf(DeviceType.MAIN_TOWER) }
    var status by remember { mutableStateOf(DeviceStatus.ONLINE) }
    var apiUsername by remember { mutableStateOf("admin") }
    var apiPassword by remember { mutableStateOf("") }

    var routerExpanded by remember { mutableStateOf(false) }
    var typeExpanded by remember { mutableStateOf(false) }
    var statusExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF1E293B))),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "إضافة جهاز جديد",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color(0xFF94A3B8))
                    }
                }

                // Name *
                OutlinedTextField(
                    value = deviceName,
                    onValueChange = { deviceName = it },
                    label = { Text("الاسم *", color = Color(0xFF94A3B8)) },
                    singleLine = true,
                    colors = dialogTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                // IP & Manager Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = ip,
                        onValueChange = { ip = it },
                        label = { Text("عنوان IP", color = Color(0xFF94A3B8)) },
                        singleLine = true,
                        colors = dialogTextFieldColors(),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = manager,
                        onValueChange = { manager = it },
                        label = { Text("المسؤول", color = Color(0xFF94A3B8)) },
                        singleLine = true,
                        colors = dialogTextFieldColors(),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Location with GPS Pin Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("الموقع (lat,lng)", color = Color(0xFF94A3B8)) },
                        singleLine = true,
                        colors = dialogTextFieldColors(),
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { location = "15.369,44.191" },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF065F46))
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = "موقع", tint = Color(0xFF34D399))
                    }
                }

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات", color = Color(0xFF94A3B8)) },
                    colors = dialogTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                // Parent Router Dropdown
                Text(text = "الراوتر", fontSize = 12.sp, color = Color(0xFF94A3B8))
                ExposedDropdownMenuBox(
                    expanded = routerExpanded,
                    onExpandedChange = { routerExpanded = !routerExpanded }
                ) {
                    val currentRouterName = routers.find { it.id == selectedRouterId }?.name ?: "— بدون راوتر —"
                    OutlinedTextField(
                        value = currentRouterName,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = routerExpanded) },
                        colors = dialogTextFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = routerExpanded,
                        onDismissRequest = { routerExpanded = false },
                        modifier = Modifier.background(Color(0xFF1E293B))
                    ) {
                        DropdownMenuItem(
                            text = { Text("— بدون راوتر —", color = Color.White) },
                            onClick = {
                                selectedRouterId = null
                                routerExpanded = false
                            }
                        )
                        routers.forEach { r ->
                            DropdownMenuItem(
                                text = { Text(r.name, color = Color.White) },
                                onClick = {
                                    selectedRouterId = r.id
                                    routerExpanded = false
                                }
                            )
                        }
                    }
                }

                // Device Type Dropdown
                Text(text = "النوع", fontSize = 12.sp, color = Color(0xFF94A3B8))
                ExposedDropdownMenuBox(
                    expanded = typeExpanded,
                    onExpandedChange = { typeExpanded = !typeExpanded }
                ) {
                    val typeLabel = when (deviceType) {
                        DeviceType.MAIN_TOWER -> "برج رئيسي"
                        DeviceType.ROUTER -> "راوتر"
                        DeviceType.AP -> "نقطة بث AP"
                        DeviceType.MODEM -> "مودم"
                    }
                    OutlinedTextField(
                        value = typeLabel,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                        colors = dialogTextFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = typeExpanded,
                        onDismissRequest = { typeExpanded = false },
                        modifier = Modifier.background(Color(0xFF1E293B))
                    ) {
                        DeviceType.values().forEach { t ->
                            val lbl = when (t) {
                                DeviceType.MAIN_TOWER -> "برج رئيسي"
                                DeviceType.ROUTER -> "راوتر"
                                DeviceType.AP -> "نقطة بث AP"
                                DeviceType.MODEM -> "مودم"
                            }
                            DropdownMenuItem(
                                text = { Text(lbl, color = Color.White) },
                                onClick = {
                                    deviceType = t
                                    typeExpanded = false
                                }
                            )
                        }
                    }
                }

                // Device Status Dropdown
                Text(text = "الحالة", fontSize = 12.sp, color = Color(0xFF94A3B8))
                ExposedDropdownMenuBox(
                    expanded = statusExpanded,
                    onExpandedChange = { statusExpanded = !statusExpanded }
                ) {
                    val statusLabel = when (status) {
                        DeviceStatus.ONLINE -> "متصل"
                        DeviceStatus.OFFLINE -> "غير متصل / طافية"
                        DeviceStatus.MAINTENANCE -> "صيانة"
                    }
                    OutlinedTextField(
                        value = statusLabel,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExpanded) },
                        colors = dialogTextFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = statusExpanded,
                        onDismissRequest = { statusExpanded = false },
                        modifier = Modifier.background(Color(0xFF1E293B))
                    ) {
                        DeviceStatus.values().forEach { s ->
                            val lbl = when (s) {
                                DeviceStatus.ONLINE -> "متصل"
                                DeviceStatus.OFFLINE -> "غير متصل / طافية"
                                DeviceStatus.MAINTENANCE -> "صيانة"
                            }
                            DropdownMenuItem(
                                text = { Text(lbl, color = Color.White) },
                                onClick = {
                                    status = s
                                    statusExpanded = false
                                }
                            )
                        }
                    }
                }

                // Optional API Credentials Box
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "🔐 بيانات الدخول للجهاز (اختياري)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFBBF24)
                        )
                        OutlinedTextField(
                            value = apiUsername,
                            onValueChange = { apiUsername = it },
                            label = { Text("اسم المستخدم", color = Color(0xFF94A3B8)) },
                            singleLine = true,
                            colors = dialogTextFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = apiPassword,
                            onValueChange = { apiPassword = it },
                            label = { Text("كلمة المرور (اتركه فارغاً إن لم تحتج)", color = Color(0xFF94A3B8)) },
                            singleLine = true,
                            colors = dialogTextFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFCBD5E1)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("إلغاء")
                    }

                    Button(
                        onClick = {
                            if (deviceName.isBlank() || ip.isBlank()) return@Button
                            viewModel.addDevice(
                                name = deviceName,
                                ip = ip,
                                manager = manager,
                                location = location,
                                notes = notes,
                                routerId = selectedRouterId,
                                parentDeviceId = null,
                                type = deviceType,
                                status = status,
                                apiUsername = apiUsername,
                                apiPassword = apiPassword
                            )
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                        modifier = Modifier.weight(1.5f)
                    ) {
                        Text("+ إضافة", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportNeighborsDialog(
    viewModel: TalalProViewModel,
    routers: List<RouterEntity>,
    onDismiss: () -> Unit
) {
    var selectedRouter by remember { mutableStateOf<RouterEntity?>(routers.firstOrNull()) }
    var routerExpanded by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }
    var importResult by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF1E293B))),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Hub, contentDescription = null, tint = Color(0xFF34D399))
                        Text(
                            text = "استيراد أجهزة النيبرز",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color(0xFF94A3B8))
                    }
                }

                Text(
                    text = "اختر الراوتر المطلوب. سيجلب النظام جميع الأجهزة الظاهرة في /ip neighbor ويضيف الجديدة فقط إلى قائمة الأجهزة.",
                    fontSize = 12.sp,
                    color = Color(0xFFCBD5E1),
                    lineHeight = 18.sp
                )

                if (routers.isEmpty()) {
                    Text(
                        text = "لا توجد راوترات مسجلة يمكنك الوصول إليها.",
                        fontSize = 12.sp,
                        color = Color(0xFFEF4444)
                    )
                } else {
                    ExposedDropdownMenuBox(
                        expanded = routerExpanded,
                        onExpandedChange = { routerExpanded = !routerExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedRouter?.name ?: "-- اختر الراوتر --",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = routerExpanded) },
                            colors = dialogTextFieldColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = routerExpanded,
                            onDismissRequest = { routerExpanded = false },
                            modifier = Modifier.background(Color(0xFF1E293B))
                        ) {
                            routers.forEach { r ->
                                DropdownMenuItem(
                                    text = { Text(r.name, color = Color.White) },
                                    onClick = {
                                        selectedRouter = r
                                        routerExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                AnimatedVisibility(visible = importResult != null) {
                    Text(
                        text = importResult ?: "",
                        fontSize = 12.sp,
                        color = Color(0xFF34D399)
                    )
                }

                Button(
                    onClick = {
                        val r = selectedRouter ?: return@Button
                        viewModel.importNeighbors(r)
                    },
                    enabled = selectedRouter != null && !isImporting,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isImporting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("📥 سحب وإضافة كل أجهزة النيبرز", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetwatchSyncDialog(
    viewModel: TalalProViewModel,
    routers: List<RouterEntity>,
    devices: List<NetworkDeviceEntity>,
    onDismiss: () -> Unit
) {
    var selectedRouter by remember { mutableStateOf<RouterEntity?>(routers.firstOrNull()) }
    var routerExpanded by remember { mutableStateOf(false) }
    var isSyncing by remember { mutableStateOf(false) }
    var syncResult by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF1E293B))),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "مزامنة Netwatch",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color(0xFF94A3B8))
                    }
                }

                Text(
                    text = "اختر الراوتر لجلب حالات Netwatch ومزامنة الأجهزة للمراقبة اللحظية:",
                    fontSize = 12.sp,
                    color = Color(0xFFCBD5E1)
                )

                ExposedDropdownMenuBox(
                    expanded = routerExpanded,
                    onExpandedChange = { routerExpanded = !routerExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedRouter?.name ?: "-- اختر الراوتر --",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = routerExpanded) },
                        colors = dialogTextFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = routerExpanded,
                        onDismissRequest = { routerExpanded = false },
                        modifier = Modifier.background(Color(0xFF1E293B))
                    ) {
                        routers.forEach { r ->
                            DropdownMenuItem(
                                text = { Text(r.name, color = Color.White) },
                                onClick = {
                                    selectedRouter = r
                                    routerExpanded = false
                                }
                            )
                        }
                    }
                }

                AnimatedVisibility(visible = syncResult != null) {
                    Text(
                        text = syncResult ?: "",
                        fontSize = 12.sp,
                        color = Color(0xFF34D399)
                    )
                }

                Button(
                    onClick = {
                        val r = selectedRouter ?: return@Button
                        viewModel.syncNetwatch(r)
                    },
                    enabled = selectedRouter != null && !isSyncing,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("بدء المزامنة", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationsDialog(
    alerts: List<AlertEntity>,
    onDismiss: () -> Unit,
    onTestSound: () -> Unit,
    onMarkAllRead: () -> Unit,
    onClearAll: () -> Unit,
    onViewAllIssues: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF1E293B))),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Notifications, contentDescription = null, tint = Color(0xFFFBBF24))
                        Text(
                            text = "التنبيهات الأخيرة",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color(0xFF94A3B8))
                    }
                }

                // Action Buttons (Mark Read, Clear, Sound Test)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        onClick = onMarkAllRead,
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF064E3B),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF34D399), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تحديد كمقروء", fontSize = 10.sp, color = Color.White)
                        }
                    }

                    Surface(
                        onClick = onClearAll,
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF7F1D1D),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFFCA5A5), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("مسح المقروء", fontSize = 10.sp, color = Color.White)
                        }
                    }

                    Surface(
                        onClick = onTestSound,
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF1E3A8A),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.VolumeUp, contentDescription = null, tint = Color(0xFF93C5FD), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تجربة الصوت", fontSize = 10.sp, color = Color.White)
                        }
                    }
                }

                Text(
                    text = "اضغط على التنبيه للتفاصيل — يُعلّم كمقروء تلقائياً",
                    fontSize = 10.sp,
                    color = Color(0xFF94A3B8)
                )

                // Alerts list
                if (alerts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "لا توجد تنبيهات جديدة", color = Color(0xFF94A3B8), fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(alerts) { alert ->
                            Card(
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (alert.severity == AlertSeverity.ERROR) Color(0xFF450A0A)
                                    else if (alert.severity == AlertSeverity.WARNING) Color(0xFF451A03)
                                    else Color(0xFF1E293B)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = alert.title,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = alert.message,
                                        fontSize = 11.sp,
                                        color = Color(0xFFE2E8F0)
                                    )
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        onDismiss()
                        onViewAllIssues()
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF065F46)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("عرض كل المشاكل", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun dialogTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color(0xFFE2E8F0),
    focusedBorderColor = Color(0xFF38BDF8),
    unfocusedBorderColor = Color(0xFF334155),
    focusedContainerColor = Color(0xFF1E293B),
    unfocusedContainerColor = Color(0xFF1E293B),
    cursorColor = Color(0xFF38BDF8)
)

private fun borderIf(condition: Boolean, color: Color, width: androidx.compose.ui.unit.Dp): androidx.compose.foundation.BorderStroke? {
    return if (condition) androidx.compose.foundation.BorderStroke(width, color) else null
}
