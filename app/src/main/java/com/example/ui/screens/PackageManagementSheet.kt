package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.HotspotProfileEntity
import com.example.data.model.RouterEntity
import com.example.data.model.UserManagerProfileEntity
import com.example.ui.TalalProViewModel

@Composable
fun PackageManagementContent(
    viewModel: TalalProViewModel,
    router: RouterEntity?,
    initialMode: String = "USER_MANAGER",
    isScrollableRoot: Boolean = true,
    modifier: Modifier = Modifier
) {
    val umProfiles by viewModel.userManagerProfiles.collectAsState()
    val hsProfiles by viewModel.hotspotProfiles.collectAsState()
    val isSyncingUm by viewModel.isSyncingUserManager.collectAsState()
    val isSyncingHs by viewModel.isSyncingHotspot.collectAsState()

    var mode by remember(initialMode) { mutableStateOf(initialMode) } // USER_MANAGER | HOTSPOT
    var selectedUm by remember { mutableStateOf<UserManagerProfileEntity?>(null) }
    var selectedHs by remember { mutableStateOf<HotspotProfileEntity?>(null) }

    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val hasSelection = if (mode == "USER_MANAGER") selectedUm != null else selectedHs != null
    val isBusy = isSyncingUm || isSyncingHs

    LaunchedEffect(router?.id) {
        if (router != null) {
            if (viewModel.selectedUserManagerRouter.value?.id != router.id) {
                viewModel.selectedUserManagerRouter.value = router
            }
            if (viewModel.selectedHotspotRouter.value?.id != router.id) {
                viewModel.selectedHotspotRouter.value = router
            }
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Header + Server Switch (User Manager | Hotspot)
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFF0F172A),
            border = BorderStroke(1.dp, Color(0xFF1E293B)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
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
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF38BDF8).copy(alpha = 0.16f))
                        ) {
                            Icon(Icons.Default.TableView, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(20.dp))
                        }
                        Column {
                            Text(
                                text = "إدارة الباقات (${if (mode == "USER_MANAGER") "يوزرمانجر" else "هوتسبوت"})",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = if (router != null) "الراوتر: ${router.name}" else "يرجى تحديد راوتر متصل",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    if (isBusy) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color(0xFF38BDF8), strokeWidth = 2.dp)
                    }
                }

                // Server Switch Pills
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E293B))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        onClick = {
                            mode = "USER_MANAGER"
                            selectedHs = null
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = if (mode == "USER_MANAGER") Color(0xFF0284C7) else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("pkg_mode_um")
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 8.dp)) {
                            Text(
                                text = "يوزرمانجر (${umProfiles.size})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (mode == "USER_MANAGER") Color.White else Color(0xFF94A3B8)
                            )
                        }
                    }
                    Surface(
                        onClick = {
                            mode = "HOTSPOT"
                            selectedUm = null
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = if (mode == "HOTSPOT") Color(0xFFD97706) else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("pkg_mode_hs")
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 8.dp)) {
                            Text(
                                text = "هوتسبوت (${hsProfiles.size})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (mode == "HOTSPOT") Color.White else Color(0xFF94A3B8)
                            )
                        }
                    }
                }

                // 5 Stacked Action Buttons matching IMG_20260907_211705_482 layout in Dark Luxury UX
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Delete Selected
                    Button(
                        onClick = { showDeleteConfirm = true },
                        enabled = hasSelection,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFDC2626),
                            disabledContainerColor = Color(0xFF7F1D1D).copy(alpha = 0.35f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("pkg_delete_btn")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "حذف الباقة", tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("حذف", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                    }

                    // Import / Sync Packages
                    Button(
                        onClick = { viewModel.syncAllPackagesForAddCards(router) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0E7490)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("pkg_import_btn")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (mode == "USER_MANAGER") "استيراد باقات اليوزرمانجر" else "استيراد باقات الهوتسبوت",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.White
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Refresh
                        Button(
                            onClick = {
                                if (router != null) {
                                    if (mode == "USER_MANAGER") viewModel.syncUserManagerData(router)
                                    else viewModel.syncHotspotData(router)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("pkg_refresh_btn")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تحديث", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                        }

                        // Edit Selected
                        Button(
                            onClick = { showEditDialog = true },
                            enabled = hasSelection,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF0F766E),
                                disabledContainerColor = Color(0xFF134E4A).copy(alpha = 0.4f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("pkg_edit_btn")
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تعديل", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                        }
                    }

                    // Add Package
                    Button(
                        onClick = { showAddDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 50.dp)
                            .testTag("pkg_add_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("إضافة باقة جديدة +", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                    }
                }

                // Packages Table matching IMG_20260907_211705_482
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF0B132B),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        // Table Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF164E63))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("الاسم", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1.3f))
                            Text("الوقت / الصلاحية", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                            Text("المشاركة / السرعة", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                        }

                        if (mode == "USER_MANAGER") {
                            if (umProfiles.isEmpty()) {
                                EmptyPackagesNotice("لا توجد باقات يوزرمانجر محملة — اضغط استيراد")
                            } else {
                                umProfiles.forEachIndexed { idx, prof ->
                                    val selected = selectedUm?.id == prof.id && selectedUm?.name == prof.name
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                when {
                                                    selected -> Color(0xFF0284C7).copy(alpha = 0.35f)
                                                    idx % 2 == 0 -> Color(0xFF0F172A)
                                                    else -> Color(0xFF131C31)
                                                }
                                            )
                                            .clickable { selectedUm = if (selected) null else prof }
                                            .padding(horizontal = 12.dp, vertical = 11.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1.3f)) {
                                            Text(
                                                text = prof.name,
                                                color = Color.White,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                                fontSize = 12.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (prof.price.isNotBlank() && prof.price != "0") {
                                                Text(
                                                    text = "${prof.price} ريال",
                                                    color = Color(0xFF34D399),
                                                    fontSize = 10.sp
                                                )
                                            }
                                        }
                                        Text(
                                            text = prof.validity.ifBlank { "مفتوح" },
                                            color = Color(0xFFE2E8F0),
                                            fontSize = 11.sp,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = "${prof.sharedUsers} • ${prof.rateLimitRx.ifBlank { "Auto" }}",
                                            color = Color(0xFF38BDF8),
                                            fontSize = 11.sp,
                                            textAlign = TextAlign.End,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    HorizontalDivider(color = Color(0xFF1E293B), thickness = 0.6.dp)
                                }
                            }
                        } else {
                            if (hsProfiles.isEmpty()) {
                                EmptyPackagesNotice("لا توجد باقات هوتسبوت محملة — اضغط استيراد")
                            } else {
                                hsProfiles.forEachIndexed { idx, prof ->
                                    val selected = selectedHs?.id == prof.id && selectedHs?.name == prof.name
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                when {
                                                    selected -> Color(0xFFD97706).copy(alpha = 0.35f)
                                                    idx % 2 == 0 -> Color(0xFF0F172A)
                                                    else -> Color(0xFF131C31)
                                                }
                                            )
                                            .clickable { selectedHs = if (selected) null else prof }
                                            .padding(horizontal = 12.dp, vertical = 11.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = prof.name,
                                            color = Color.White,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1.3f)
                                        )
                                        Text(
                                            text = prof.sessionTimeout.ifBlank { "0s" },
                                            color = Color(0xFFE2E8F0),
                                            fontSize = 11.sp,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = "${prof.sharedUsers} | ${prof.rateLimit.ifBlank { "-" }}",
                                            color = Color(0xFFFBBF24),
                                            fontSize = 11.sp,
                                            textAlign = TextAlign.End,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    HorizontalDivider(color = Color(0xFF1E293B), thickness = 0.6.dp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Package Dialog
    if (showAddDialog) {
        PackageEditorDialog(
            title = if (mode == "USER_MANAGER") "إضافة باقة يوزرمانجر جديدة" else "إضافة باقة هوتسبوت جديدة",
            mode = mode,
            initialName = "",
            initialValidityOrTimeout = if (mode == "USER_MANAGER") "30d" else "1d",
            initialPrice = "500",
            initialRateLimit = "4M/4M",
            initialSharedUsers = "1",
            isEdit = false,
            onDismiss = { showAddDialog = false },
            onSave = { name, validity, price, rate, shared ->
                if (mode == "USER_MANAGER") {
                    viewModel.addUserManagerPackage(router, name, validity, price, rate, shared)
                } else {
                    viewModel.addHotspotPackage(router, name, rate, shared, validity)
                }
                showAddDialog = false
            }
        )
    }

    // Edit Selected Package Dialog
    if (showEditDialog) {
        if (mode == "USER_MANAGER" && selectedUm != null) {
            val um = selectedUm!!
            PackageEditorDialog(
                title = "تعديل باقة اليوزرمانجر [${um.name}]",
                mode = mode,
                initialName = um.name,
                initialValidityOrTimeout = um.validity,
                initialPrice = um.price,
                initialRateLimit = um.rateLimitRx,
                initialSharedUsers = um.sharedUsers.toString(),
                isEdit = true,
                onDismiss = { showEditDialog = false },
                onSave = { newName, validity, price, _, shared ->
                    viewModel.editUserManagerPackage(router, um.name, newName, validity, price, shared)
                    selectedUm = null
                    showEditDialog = false
                }
            )
        } else if (mode == "HOTSPOT" && selectedHs != null) {
            val hs = selectedHs!!
            PackageEditorDialog(
                title = "تعديل باقة الهوتسبوت [${hs.name}]",
                mode = mode,
                initialName = hs.name,
                initialValidityOrTimeout = hs.sessionTimeout,
                initialPrice = "0",
                initialRateLimit = hs.rateLimit,
                initialSharedUsers = hs.sharedUsers.toString(),
                isEdit = true,
                onDismiss = { showEditDialog = false },
                onSave = { newName, timeout, _, rate, shared ->
                    viewModel.editHotspotPackage(router, hs.name, newName, rate, shared, timeout)
                    selectedHs = null
                    showEditDialog = false
                }
            )
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirm) {
        val targetName = if (mode == "USER_MANAGER") selectedUm?.name else selectedHs?.name
        if (targetName != null) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                containerColor = Color(0xFF0F172A),
                titleContentColor = Color.White,
                textContentColor = Color(0xFFCBD5E1),
                title = { Text("حذف الباقة [$targetName]", fontWeight = FontWeight.Bold) },
                text = { Text("هل أنت متأكد من حذف هذه الباقة نهائياً من راوتر MikroTik؟") },
                confirmButton = {
                    Button(
                        onClick = {
                            if (mode == "USER_MANAGER") {
                                viewModel.deleteUserManagerPackage(router, targetName)
                                selectedUm = null
                            } else {
                                viewModel.deleteHotspotPackage(router, targetName)
                                selectedHs = null
                            }
                            showDeleteConfirm = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                    ) {
                        Text("نعم، حذف", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showDeleteConfirm = false }) {
                        Text("إلغاء", color = Color.White)
                    }
                }
            )
        }
    }
}

@Composable
private fun EmptyPackagesNotice(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 22.dp, horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            fontSize = 12.sp,
            color = Color(0xFF94A3B8),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PackageEditorDialog(
    title: String,
    mode: String,
    initialName: String,
    initialValidityOrTimeout: String,
    initialPrice: String,
    initialRateLimit: String,
    initialSharedUsers: String,
    isEdit: Boolean,
    onDismiss: () -> Unit,
    onSave: (name: String, validity: String, price: String, rateLimit: String, sharedUsers: Int) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var validity by remember { mutableStateOf(initialValidityOrTimeout) }
    var price by remember { mutableStateOf(initialPrice) }
    var rateLimit by remember { mutableStateOf(initialRateLimit) }
    var sharedUsersText by remember { mutableStateOf(initialSharedUsers) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF0F172A),
            border = BorderStroke(1.dp, Color(0xFF334155)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم الباقة *", color = Color(0xFF94A3B8), fontSize = 12.sp) },
                    singleLine = true,
                    colors = darkFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = validity,
                    onValueChange = { validity = it },
                    label = {
                        Text(
                            if (mode == "USER_MANAGER") "الصلاحية (مثال: 30d أو 12h أو 720h)" else "انتهاء الجلسة (مثال: 1d أو 2h)",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    },
                    singleLine = true,
                    colors = darkFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                if (mode == "USER_MANAGER") {
                    OutlinedTextField(
                        value = price,
                        onValueChange = { price = it },
                        label = { Text("سعر الباقة (ريال)", color = Color(0xFF94A3B8), fontSize = 12.sp) },
                        singleLine = true,
                        colors = darkFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (!isEdit || mode == "HOTSPOT") {
                    OutlinedTextField(
                        value = rateLimit,
                        onValueChange = { rateLimit = it },
                        label = { Text("السرعة Rate Limit (مثال: 4M/4M)", color = Color(0xFF94A3B8), fontSize = 11.sp) },
                        singleLine = true,
                        colors = darkFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = sharedUsersText,
                    onValueChange = { v -> sharedUsersText = v.filter { it.isDigit() } },
                    label = { Text("عدد المستخدمين المشتركين (Shared Users)", color = Color(0xFF94A3B8), fontSize = 11.sp) },
                    singleLine = true,
                    colors = darkFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                    ) {
                        Text("إلغاء", color = Color.White)
                    }
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                val shared = sharedUsersText.toIntOrNull()?.coerceAtLeast(1) ?: 1
                                onSave(name.trim(), validity.trim(), price.trim(), rateLimit.trim(), shared)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                    ) {
                        Text(if (isEdit) "حفظ التعديل" else "إضافة للراوتر", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun darkFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedContainerColor = Color(0xFF1E293B),
    unfocusedContainerColor = Color(0xFF1E293B),
    focusedBorderColor = Color(0xFF38BDF8),
    unfocusedBorderColor = Color(0xFF334155)
)
