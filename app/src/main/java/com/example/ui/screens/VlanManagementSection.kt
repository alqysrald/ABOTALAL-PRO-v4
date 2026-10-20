package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.MikrotikVlan
import com.example.data.model.RouterEntity
import com.example.ui.TalalProViewModel
import com.example.ui.theme.*

@Composable
fun VlanManagementSection(
    viewModel: TalalProViewModel,
    router: RouterEntity?,
    modifier: Modifier = Modifier
) {
    val vlans by viewModel.vlans.collectAsState()
    val availableIfaces by viewModel.availableInterfaces.collectAsState()
    val isLoading by viewModel.isLoadingVlans.collectAsState()
    val isSaving by viewModel.isSavingVlan.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingVlan by remember { mutableStateOf<MikrotikVlan?>(null) }
    var vlanToDelete by remember { mutableStateOf<MikrotikVlan?>(null) }
    var vlanForIpAssignment by remember { mutableStateOf<MikrotikVlan?>(null) }

    LaunchedEffect(router) {
        if (router != null) {
            viewModel.loadVlans(router)
        }
    }

    val filteredVlans = remember(vlans, searchQuery) {
        if (searchQuery.isBlank()) {
            vlans
        } else {
            vlans.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.vlanId.toString().contains(searchQuery) ||
                it.interfaceName.contains(searchQuery, ignoreCase = true) ||
                it.comment.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Summary & Actions Header Card
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF6366F1).copy(alpha = 0.12f),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Lan, contentDescription = null, tint = Color(0xFF6366F1), modifier = Modifier.size(24.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "إدارة شبكات الفيلان (VLANs)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = PrimaryNavy
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFEEF2FF)) {
                                    Text(
                                        text = "${vlans.size} فيلان",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF4F46E5),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "عرض وتوليد وتعديل الفيلانات وعزل الشبكات في راوتر المايكروتك",
                                fontSize = 11.sp,
                                color = TextSecondaryLight
                            )
                        }
                    }

                    IconButton(
                        onClick = { router?.let { viewModel.loadVlans(it) } },
                        enabled = !isLoading && router != null
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = PrimaryBlue)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "تحديث", tint = PrimaryBlue)
                        }
                    }
                }

                HorizontalDivider(color = BorderLight.copy(alpha = 0.5f))

                // Stats Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val activeCount = vlans.count { !it.disabled }
                    val disabledCount = vlans.count { it.disabled }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF0FDF4),
                        border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text("🟢 الفعالة: ", fontSize = 11.sp, color = Color(0xFF166534))
                            Text("$activeCount", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF15803D))
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFFFF1F2),
                        border = BorderStroke(1.dp, Color(0xFFFECDD3)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text("🔴 المعطلة: ", fontSize = 11.sp, color = Color(0xFF9F1239))
                            Text("$disabledCount", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFFBE123C))
                        }
                    }
                }

                // Search & Add Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("بحث عن فيلان (الاسم، المعرف، المنفذ)...", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(20.dp)) {
                                    Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    Button(
                        onClick = { showAddDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(52.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("إضافة فيلان", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // VLANs List / Empty State
        if (isLoading && vlans.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator(color = Color(0xFF4F46E5))
                    Text("جاري استعلام شبكات الفيلان من الراوتر...", fontSize = 12.sp, color = TextSecondaryLight)
                }
            }
        } else if (filteredVlans.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(shape = CircleShape, color = Color(0xFFF1F5F9), modifier = Modifier.size(56.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Lan, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(32.dp))
                        }
                    }
                    Text(
                        text = if (searchQuery.isNotBlank()) "لم يتم العثور على أي فيلان يطابق البحث" else "لا توجد شبكات فيلان (VLAN) مضافة في هذا الراوتر",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = PrimaryNavy
                    )
                    Text(
                        text = "يمكنك إضافة شبكة فيلان جديدة وتحديد رقم المعرف والمنفذ الأم لعزل حركة الشبكة فوراً.",
                        fontSize = 11.sp,
                        color = TextSecondaryLight
                    )
                    Button(
                        onClick = { showAddDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إنشاء أول فيلان الآن 🚀")
                    }
                }
            }
        } else {
            filteredVlans.forEach { vlan ->
                VlanCardItem(
                    vlan = vlan,
                    onToggle = { viewModel.toggleVlan(router, vlan) },
                    onEdit = { editingVlan = vlan },
                    onDelete = { vlanToDelete = vlan },
                    onAssignIp = { vlanForIpAssignment = vlan }
                )
            }
        }
    }

    // Dialog: Add / Edit VLAN
    if (showAddDialog || editingVlan != null) {
        val currentEdit = editingVlan
        AddEditVlanDialog(
            vlan = currentEdit,
            availableInterfaces = availableIfaces,
            isSaving = isSaving,
            onDismiss = {
                showAddDialog = false
                editingVlan = null
            },
            onSave = { name, vlanId, iface, mtu, arp, comment ->
                if (currentEdit == null) {
                    viewModel.addVlan(router, name, vlanId, iface, mtu, arp, comment)
                } else {
                    viewModel.updateVlan(router, currentEdit.id.ifBlank { currentEdit.name }, name, vlanId, iface, mtu, arp, comment)
                }
                showAddDialog = false
                editingVlan = null
            }
        )
    }

    // Dialog: Delete Confirmation
    vlanToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { vlanToDelete = null },
            title = { Text("حذف الفيلان [${target.name}]؟", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "هل أنت متأكد من حذف الفيلان [${target.name}] ذات المعرف (${target.vlanId}) المرتبطة بالمنفذ [${target.interfaceName}]؟ سيتم إزالتها نهائياً من راوتر المايكروتك.",
                    fontSize = 13.sp,
                    color = Color(0xFF475569)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteVlan(router, target)
                        vlanToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("نعم، احذف الفيلان")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { vlanToDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Dialog: Assign IP
    vlanForIpAssignment?.let { target ->
        AssignIpToVlanDialog(
            vlan = target,
            onDismiss = { vlanForIpAssignment = null },
            onConfirm = { ip ->
                viewModel.assignIpToVlan(router, target.name, ip)
                vlanForIpAssignment = null
            }
        )
    }
}

@Composable
fun VlanCardItem(
    vlan: MikrotikVlan,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAssignIp: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, if (vlan.disabled) Color(0xFFE2E8F0) else Color(0xFFC7D2FE)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (vlan.disabled) Color(0xFFF1F5F9) else Color(0xFF4F46E5),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "V${vlan.vlanId}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (vlan.disabled) Color(0xFF64748B) else Color.White
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = vlan.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (vlan.disabled) Color(0xFF64748B) else PrimaryNavy
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (vlan.disabled) Color(0xFFFEE2E2) else Color(0xFFDCFCE7)
                            ) {
                                Text(
                                    text = if (vlan.disabled) "معطل" else if (vlan.running) "نشط ومفعل 🟢" else "مفعل ✓",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (vlan.disabled) Color(0xFFDC2626) else Color(0xFF15803D),
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "المنفذ الأساسي: ${vlan.interfaceName} • MTU: ${vlan.mtu} • ARP: ${vlan.arp}",
                            fontSize = 11.sp,
                            color = TextSecondaryLight
                        )
                    }
                }

                // Toggle Switch
                Switch(
                    checked = !vlan.disabled,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF4F46E5))
                )
            }

            if (vlan.comment.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "💬 ${vlan.comment}",
                        fontSize = 11.sp,
                        color = Color(0xFF475569),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Throughput & Traffic stats
            if (vlan.rxByte > 0 || vlan.txByte > 0 || vlan.rxPacket > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "📥 استلام: ${formatBytes(vlan.rxByte)} (${vlan.rxPacket} حزمة)",
                        fontSize = 10.sp,
                        color = Color(0xFF059669)
                    )
                    Text(
                        text = "📤 إرسال: ${formatBytes(vlan.txByte)} (${vlan.txPacket} حزمة)",
                        fontSize = 10.sp,
                        color = Color(0xFF2563EB)
                    )
                }
            }

            HorizontalDivider(color = BorderLight.copy(alpha = 0.5f))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilledTonalButton(
                        onClick = onAssignIp,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color(0xFFEEF2FF),
                            contentColor = Color(0xFF4F46E5)
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Default.AddLink, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("تعيين IP", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = onEdit,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("تعديل", fontSize = 11.sp)
                    }
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "حذف", tint = Color(0xFFDC2626), modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditVlanDialog(
    vlan: MikrotikVlan?,
    availableInterfaces: List<String>,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (name: String, vlanId: Int, iface: String, mtu: Int, arp: String, comment: String) -> Unit
) {
    var name by remember { mutableStateOf(vlan?.name ?: "vlan10") }
    var vlanIdText by remember { mutableStateOf(vlan?.vlanId?.toString() ?: "10") }
    var selectedIface by remember {
        mutableStateOf(vlan?.interfaceName ?: availableInterfaces.firstOrNull() ?: "ether1")
    }
    var mtuText by remember { mutableStateOf(vlan?.mtu?.toString() ?: "1500") }
    var arp by remember { mutableStateOf(vlan?.arp ?: "enabled") }
    var comment by remember { mutableStateOf(vlan?.comment ?: "") }

    var ifaceDropdownExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (vlan == null) "إضافة شبكة فيلان جديدة" else "تعديل الفيلان [${vlan.name}]",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = PrimaryNavy
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                HorizontalDivider(color = BorderLight.copy(alpha = 0.6f))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم الفيلان (Name) *") },
                    placeholder = { Text("مثال: vlan10_hotspot") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Label, contentDescription = null, tint = Color(0xFF4F46E5)) }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = vlanIdText,
                        onValueChange = { vlanIdText = it },
                        label = { Text("معرف (VLAN ID) *") },
                        placeholder = { Text("1-4094") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = mtuText,
                        onValueChange = { mtuText = it },
                        label = { Text("حجم MTU") },
                        placeholder = { Text("1500") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                }

                // Interface Dropdown Selector
                Box {
                    OutlinedTextField(
                        value = selectedIface,
                        onValueChange = { selectedIface = it },
                        label = { Text("المنفذ الأساسي (Interface) *") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        readOnly = false,
                        trailingIcon = {
                            IconButton(onClick = { ifaceDropdownExpanded = true }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        }
                    )

                    DropdownMenu(
                        expanded = ifaceDropdownExpanded,
                        onDismissRequest = { ifaceDropdownExpanded = false }
                    ) {
                        val ifaces = if (availableInterfaces.isNotEmpty()) availableInterfaces else listOf("ether1", "ether2", "ether3", "ether4", "ether5", "bridge", "sfp1")
                        ifaces.forEach { iface ->
                            DropdownMenuItem(
                                text = { Text(iface) },
                                onClick = {
                                    selectedIface = iface
                                    ifaceDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("ملاحظة توضيحية (Comment)") },
                    placeholder = { Text("مثال: فيلان خاص بشبكة الضيوف") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = {
                        val idVal = vlanIdText.toIntOrNull() ?: 10
                        val mtuVal = mtuText.toIntOrNull() ?: 1500
                        onSave(name, idVal, selectedIface, mtuVal, arp, comment)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    enabled = !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (vlan == null) "إنشاء الفيلان في الراوتر 🚀" else "حفظ التعديلات في الراوتر ✓", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun AssignIpToVlanDialog(
    vlan: MikrotikVlan,
    onDismiss: () -> Unit,
    onConfirm: (ip: String) -> Unit
) {
    var ipText by remember { mutableStateOf("192.168.${vlan.vlanId}.1/24") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "تعيين عنوان IP لـ [${vlan.name}]",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = PrimaryNavy
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                Text(
                    text = "أدخل عنوان IP مع القناع (Subnet Mask) لتعيينه مباشرة لواجهة الفيلان بالراوتر:",
                    fontSize = 11.sp,
                    color = TextSecondaryLight
                )

                OutlinedTextField(
                    value = ipText,
                    onValueChange = { ipText = it },
                    label = { Text("عنوان IP مع القناع (CIDR) *") },
                    placeholder = { Text("مثال: 10.10.10.1/24") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                // Presets Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val presets = listOf("10.10.${vlan.vlanId}.1/24", "172.16.${vlan.vlanId}.1/24", "192.168.${vlan.vlanId}.1/24")
                    presets.forEach { pre ->
                        SuggestionChip(
                            onClick = { ipText = pre },
                            label = { Text(pre, fontSize = 10.sp) }
                        )
                    }
                }

                Button(
                    onClick = { onConfirm(ipText) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(46.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("تطبيق العنوان في الراوتر الآن", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> String.format("%.2f GB", gb)
        mb >= 1.0 -> String.format("%.2f MB", mb)
        kb >= 1.0 -> String.format("%.1f KB", kb)
        else -> "$bytes B"
    }
}
