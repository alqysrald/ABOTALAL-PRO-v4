package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.mikrotik.MikroTikResponse
import com.example.data.model.RouterEntity
import com.example.ui.TalalProViewModel

data class SpecialToolQuerySpec(
    val title: String,
    val subtitle: String,
    val command: String,
    val accentColor: Color
)

@Composable
fun LiveRouterCommandViewerDialog(
    viewModel: TalalProViewModel,
    router: RouterEntity?,
    spec: SpecialToolQuerySpec,
    onDismiss: () -> Unit
) {
    var response by remember { mutableStateOf<MikroTikResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var customCmd by remember(spec.command) { mutableStateOf(spec.command) }

    fun runQuery(cmd: String) {
        isLoading = true
        viewModel.executeSpecialToolCommand(router, cmd) { res ->
            response = res
            isLoading = false
        }
    }

    LaunchedEffect(spec.command, router?.id) {
        runQuery(spec.command)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = Color(0xFF0F172A),
            border = BorderStroke(1.dp, Color(0xFF334155)),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 300.dp, max = 640.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(spec.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(
                            text = "${spec.subtitle} • ${router?.name ?: "لا يوجد راوتر"}",
                            fontSize = 11.sp,
                            color = spec.accentColor
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { runQuery(customCmd) },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "تحديث", tint = Color(0xFF38BDF8))
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color(0xFF94A3B8))
                        }
                    }
                }

                // Editable RouterOS command bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = customCmd,
                        onValueChange = { customCmd = it },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF38BDF8),
                            unfocusedTextColor = Color(0xFF38BDF8),
                            focusedContainerColor = Color(0xFF020617),
                            unfocusedContainerColor = Color(0xFF020617),
                            focusedBorderColor = spec.accentColor,
                            unfocusedBorderColor = Color(0xFF334155)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = { runQuery(customCmd) },
                        colors = ButtonDefaults.buttonColors(containerColor = spec.accentColor),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.heightIn(min = 50.dp)
                    ) {
                        Text("تنفيذ", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                    }
                }

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            CircularProgressIndicator(color = spec.accentColor)
                            Text("جاري جلب البيانات الحية من MikroTik...", fontSize = 12.sp, color = Color(0xFF94A3B8))
                        }
                    }
                } else {
                    val res = response
                    if (res == null || !res.isSuccess) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF7F1D1D).copy(alpha = 0.4f),
                            border = BorderStroke(1.dp, Color(0xFFEF4444)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = res?.errorMessage ?: "تعذر الاتصال بالراوتر",
                                color = Color(0xFFFCA5A5),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    } else if (res.results.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF1E293B),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "✓ تم التنفيذ بنجاح (لا توجد صفوف بيانات معروضة — ping: ${res.pingTimeMs}ms)",
                                color = Color(0xFF34D399),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } else {
                        Text(
                            text = "إجمالي السجلات: ${res.results.size} (${res.pingTimeMs}ms)",
                            fontSize = 11.sp,
                            color = Color(0xFF34D399),
                            fontWeight = FontWeight.Bold
                        )
                        LazyColumn(
                            modifier = Modifier.weight(1f, fill = false),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(res.results.take(80)) { idx, row ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF1E293B),
                                    border = BorderStroke(1.dp, Color(0xFF334155)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "#${idx + 1} ${row["name"] ?: row["user"] ?: row["address"] ?: row["identity"] ?: ""}",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                        row.entries
                                            .filter { it.key != ".id" && it.value.isNotBlank() }
                                            .take(8)
                                            .forEach { (k, v) ->
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        text = k,
                                                        fontSize = 10.sp,
                                                        fontFamily = FontFamily.Monospace,
                                                        color = Color(0xFF94A3B8)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = v,
                                                        fontSize = 11.sp,
                                                        fontFamily = FontFamily.Monospace,
                                                        color = Color(0xFF38BDF8)
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
    }
}

@Composable
fun PppoeBroadbandDialog(
    viewModel: TalalProViewModel,
    router: RouterEntity?,
    onDismiss: () -> Unit
) {
    var secrets by remember { mutableStateOf<List<Map<String, String>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var newName by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }
    var newProfile by remember { mutableStateOf("default") }

    fun loadSecrets() {
        isLoading = true
        viewModel.executeSpecialToolCommand(router, "/ppp/secret/print") { res ->
            if (res.isSuccess) {
                secrets = res.results
                errorMsg = null
            } else {
                errorMsg = res.errorMessage
            }
            isLoading = false
        }
    }

    LaunchedEffect(router?.id) { loadSecrets() }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = Color(0xFF0F172A),
            border = BorderStroke(1.dp, Color(0xFF334155)),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 640.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("إدارة حسابات البروبانـد (PPPoE)", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("الراوتر: ${router?.name ?: "غير محدد"} • الحسابات: ${secrets.size}", fontSize = 11.sp, color = Color(0xFF38BDF8))
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color(0xFF94A3B8))
                    }
                }

                // Quick add PPPoE user
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF1E293B),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("إضافة مشترك PPPoE جديد", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFBBF24))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(
                                value = newName,
                                onValueChange = { newName = it },
                                placeholder = { Text("اسم المستخدم", fontSize = 11.sp) },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = newPass,
                                onValueChange = { newPass = it },
                                placeholder = { Text("كلمة المرور", fontSize = 11.sp) },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = newProfile,
                                onValueChange = { newProfile = it },
                                placeholder = { Text("البروفايل", fontSize = 11.sp) },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = {
                                    if (newName.isNotBlank()) {
                                        val cmd = "/ppp/secret/add =name=${newName.trim()} =password=${newPass.ifBlank { newName.trim() }} =service=pppoe =profile=${newProfile.ifBlank { "default" }}"
                                        viewModel.executeSpecialToolCommand(router, cmd) {
                                            newName = ""
                                            newPass = ""
                                            loadSecrets()
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                                modifier = Modifier.heightIn(min = 50.dp)
                            ) {
                                Text("إضافة", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF38BDF8))
                    }
                } else if (errorMsg != null) {
                    Text(errorMsg!!, color = Color(0xFFFCA5A5), fontSize = 12.sp)
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(secrets) { _, sec ->
                            val uname = sec["name"] ?: ""
                            val sid = sec[".id"] ?: ""
                            val disabled = sec["disabled"] == "true"
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF1E293B),
                                border = BorderStroke(1.dp, if (disabled) Color(0xFFEF4444) else Color(0xFF334155)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(uname, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(
                                            text = "الباقة: ${sec["profile"] ?: "default"} • ${if (disabled) "معطل" else "فعال"}",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 11.sp
                                        )
                                    }
                                    Row {
                                        IconButton(onClick = {
                                            val cmd = if (disabled) "/ppp/secret/enable =.id=$sid" else "/ppp/secret/disable =.id=$sid"
                                            viewModel.executeSpecialToolCommand(router, cmd) { loadSecrets() }
                                        }) {
                                            Icon(
                                                if (disabled) Icons.Default.PlayArrow else Icons.Default.Pause,
                                                contentDescription = "تبديل الحالة",
                                                tint = if (disabled) Color(0xFF34D399) else Color(0xFFFBBF24)
                                            )
                                        }
                                        IconButton(onClick = {
                                            viewModel.executeSpecialToolCommand(router, "/ppp/secret/remove =.id=$sid") { loadSecrets() }
                                        }) {
                                            Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color(0xFFEF4444))
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
}

@Composable
fun HotspotHtmlDirDialog(
    onDismiss: () -> Unit,
    onApply: (String) -> Unit
) {
    var htmlDir by remember { mutableStateOf("flash/hotspot") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF0F172A),
            border = BorderStroke(1.dp, Color(0xFF334155)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("تغيير مجلد HTML للـ Hotspot", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(
                    text = "حدد مسار مجلد صفحة تسجيل الدخول على راوتر المايكروتك (مثل: flash/hotspot أو hotspot):",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8)
                )
                OutlinedTextField(
                    value = htmlDir,
                    onValueChange = { htmlDir = it },
                    singleLine = true,
                    label = { Text("مسار المجلد (HTML Directory)", fontSize = 11.sp, color = Color(0xFF94A3B8)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF1E293B),
                        unfocusedContainerColor = Color(0xFF1E293B)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                    ) { Text("إلغاء", color = Color.White) }
                    Button(
                        onClick = { onApply(htmlDir) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                    ) { Text("تطبيق التغيير", fontWeight = FontWeight.Bold, color = Color.White) }
                }
            }
        }
    }
}

@Composable
fun FreeNetworkControlDialog(
    onDismiss: () -> Unit,
    onToggleFree: (Boolean) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF0F172A),
            border = BorderStroke(1.dp, Color(0xFF334155)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("تفعيل الشبكة مجاناً (Free Hotspot)", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(
                    text = "يتحكم بتعطيل أو إعادة تشغيل سيرفر الهوتسبوت لفتح الإنترنت مجاناً بدون طلب كروت:",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8)
                )
                Button(
                    onClick = { onToggleFree(true) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                ) {
                    Text("🌐 فتح الشبكة مجاناً للجميع الآن", fontWeight = FontWeight.Bold, color = Color.White)
                }
                Button(
                    onClick = { onToggleFree(false) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                ) {
                    Text("🔐 إيقاف الوضع المجاني وإعادة بوابة الكروت", fontWeight = FontWeight.Bold, color = Color.White)
                }
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text("إلغاء", color = Color(0xFF94A3B8))
                }
            }
        }
    }
}

@Composable
fun BluetoothSharingControlDialog(
    onDismiss: () -> Unit,
    onToggleBlock: (Boolean) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF0F172A),
            border = BorderStroke(1.dp, Color(0xFF334155)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("منع مشاركة الإنترنت (بلوتوث / نقطة اتصال TTL=1)", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(
                    text = "يضيف قاعدة Mangle Change TTL=1 على الراوتر لمنع المشتركين من بث أو مشاركة الإنترنت عبر البلوتوث أو Hotspot الهاتف:",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8)
                )
                Button(
                    onClick = { onToggleBlock(true) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                ) {
                    Text("🛡️ تفعيل منع مشاركة الإنترنت (TTL=1)", fontWeight = FontWeight.Bold, color = Color.White)
                }
                Button(
                    onClick = { onToggleBlock(false) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                ) {
                    Text("🔓 إلغاء حظر المشاركة", fontWeight = FontWeight.Bold, color = Color.White)
                }
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text("إلغاء", color = Color(0xFF94A3B8))
                }
            }
        }
    }
}

@Composable
fun PingToolDialog(
    viewModel: TalalProViewModel,
    router: RouterEntity?,
    onDismiss: () -> Unit
) {
    var targetIp by remember { mutableStateOf("8.8.8.8") }
    var count by remember { mutableStateOf("4") }
    var output by remember { mutableStateOf("") }
    var running by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF0F172A),
            border = BorderStroke(1.dp, Color(0xFF334155)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("أداة فحص الاتصال Ping", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = targetIp,
                        onValueChange = { targetIp = it },
                        label = { Text("العنوان (IP / Domain)", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1.5f)
                    )
                    OutlinedTextField(
                        value = count,
                        onValueChange = { count = it.filter { c -> c.isDigit() } },
                        label = { Text("العدد", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(0.7f)
                    )
                }
                Button(
                    onClick = {
                        running = true
                        viewModel.executeSpecialToolCommand(router, "/ping =address=${targetIp.trim()} =count=${count.ifBlank { "4" }}") { res ->
                            running = false
                            output = if (res.isSuccess) {
                                res.results.joinToString("\n") { row ->
                                    "seq=${row["seq"] ?: "-"} host=${row["host"] ?: targetIp} time=${row["time"] ?: "timeout"} ttl=${row["ttl"] ?: "-"}"
                                }.ifBlank { "تم التنفيذ بدون استجابة" }
                            } else {
                                "⚠️ ${res.errorMessage}"
                            }
                        }
                    },
                    enabled = !running,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                ) {
                    if (running) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    else Text("تشغيل Ping الآن", fontWeight = FontWeight.Bold, color = Color.White)
                }
                if (output.isNotBlank()) {
                    Surface(color = Color(0xFF020617), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = output,
                            color = Color(0xFF34D399),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("إغلاق", color = Color(0xFF94A3B8))
                }
            }
        }
    }
}

@Composable
fun AddressListManagerDialog(
    viewModel: TalalProViewModel,
    router: RouterEntity?,
    listName: String,
    title: String,
    accentColor: Color,
    onDismiss: () -> Unit
) {
    var entries by remember { mutableStateOf<List<Map<String, String>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var newAddress by remember { mutableStateOf("") }
    var newComment by remember { mutableStateOf("") }

    fun loadEntries() {
        isLoading = true
        viewModel.executeSpecialToolCommand(router, "/ip/firewall/address-list/print ?list=$listName") { res ->
            entries = res.results
            isLoading = false
        }
    }

    LaunchedEffect(router?.id, listName) { loadEntries() }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF0F172A),
            border = BorderStroke(1.dp, Color(0xFF334155)),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color(0xFF94A3B8)) }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newAddress,
                        onValueChange = { newAddress = it },
                        placeholder = { Text("IP أو نطاق", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            if (newAddress.isNotBlank()) {
                                val cmd = "/ip/firewall/address-list/add =list=$listName =address=${newAddress.trim()} =comment=${newComment.ifBlank { "TALAL_PRO" }}"
                                viewModel.executeSpecialToolCommand(router, cmd) {
                                    newAddress = ""
                                    newComment = ""
                                    loadEntries()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        modifier = Modifier.heightIn(min = 50.dp)
                    ) { Text("إضافة", color = Color.White, fontWeight = FontWeight.Bold) }
                }

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = accentColor)
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (entries.isEmpty()) {
                            item { Text("القائمة [$listName] فارغة حالياً", color = Color(0xFF94A3B8), fontSize = 12.sp) }
                        } else {
                            itemsIndexed(entries) { _, item ->
                                val id = item[".id"] ?: ""
                                Surface(color = Color(0xFF1E293B), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(item["address"] ?: "-", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            Text(item["comment"] ?: listName, color = Color(0xFF94A3B8), fontSize = 10.sp)
                                        }
                                        IconButton(onClick = {
                                            viewModel.executeSpecialToolCommand(router, "/ip/firewall/address-list/remove =.id=$id") { loadEntries() }
                                        }) {
                                            Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color(0xFFEF4444))
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
}
