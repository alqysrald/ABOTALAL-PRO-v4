package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.firebase.AiProviderConfig
import com.example.data.firebase.FirebaseAppConfig
import com.example.data.firebase.FirebaseUserAccount
import com.example.data.utils.DeviceUtils
import com.example.ui.ScreenTab
import com.example.ui.TalalProViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class AdminNavTab(val title: String, val icon: ImageVector) {
    USERS("المستخدمين", Icons.Default.People),
    STATS("الإحصائيات", Icons.Default.Analytics),
    APP_CONFIG("الأقسام والواجهات", Icons.Default.Widgets),
    TRIAL_SETTINGS("الفترة المجانية", Icons.Default.HourglassTop),
    AI_CONTROL("تحكم الذكاء", Icons.Default.SmartToy)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminControlPanelScreen(
    viewModel: TalalProViewModel,
    onNavigateBack: () -> Unit
) {
    val users by viewModel.firebaseUsers.collectAsState()
    val appConfig by viewModel.appConfig.collectAsState()
    val isLoading by viewModel.isLoadingUsers.collectAsState()

    var currentTab by remember { mutableStateOf(AdminNavTab.USERS) }
    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("ALL") }

    // Dialogs
    var showAddUserDialog by remember { mutableStateOf(false) }
    var selectedUserForEdit by remember { mutableStateOf<FirebaseUserAccount?>(null) }
    var selectedUserForNotification by remember { mutableStateOf<FirebaseUserAccount?>(null) }
    var selectedUserForExtend by remember { mutableStateOf<FirebaseUserAccount?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadFirebaseUsers()
        viewModel.loadAppConfig()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFEF4444).copy(alpha = 0.15f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AdminPanelSettings,
                                    contentDescription = null,
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "لوحة تحكم المدير (Admin)",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimaryLight
                            )
                            Text(
                                text = "إدارة المستخدمين والأجهزة والاشتراكات وAI",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondaryLight
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "الرجوع للتطبيق",
                            tint = PrimaryNavy
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.setTab(ScreenTab.PORTS_AND_DEVICES)
                            onNavigateBack()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Hub,
                            contentDescription = "المنافذ وأجهزة الشبكة",
                            tint = Color(0xFF0284C7)
                        )
                    }
                    IconButton(
                        onClick = {
                            viewModel.loadFirebaseUsers()
                            viewModel.loadAppConfig()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "تحديث",
                            tint = PrimaryBlue
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp
            ) {
                AdminNavTab.values().forEach { tab ->
                    NavigationBarItem(
                        selected = currentTab == tab,
                        onClick = { currentTab = tab },
                        icon = { Icon(imageVector = tab.icon, contentDescription = tab.title) },
                        label = { Text(tab.title, fontSize = 10.sp, fontWeight = if (currentTab == tab) FontWeight.Bold else FontWeight.Normal) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PrimaryNavy,
                            selectedTextColor = PrimaryNavy,
                            indicatorColor = PrimaryBlue.copy(alpha = 0.15f),
                            unselectedIconColor = TextMutedLight,
                            unselectedTextColor = TextMutedLight
                        )
                    )
                }
            }
        },
        containerColor = Color(0xFFF8FAFC)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (currentTab) {
                AdminNavTab.USERS -> {
                    UsersManagementTab(
                        users = users,
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        statusFilter = statusFilter,
                        onStatusFilterChange = { statusFilter = it },
                        onAddUserClick = { showAddUserDialog = true },
                        onEditUser = { selectedUserForEdit = it },
                        onSendNotification = { selectedUserForNotification = it },
                        onExtendUser = { selectedUserForExtend = it },
                        onDeleteUser = { viewModel.deleteFirebaseUser(it) },
                        onToggleSuspend = { user ->
                            val newStatus = if (user.status == "suspended") "active" else "suspended"
                            viewModel.updateUserStatus(user, newStatus)
                        },
                        onUnbindDevice = { user ->
                            viewModel.unbindUserDevice(user)
                        }
                    )
                }
                AdminNavTab.STATS -> {
                    StatsManagementTab(
                        users = users,
                        appConfig = appConfig
                    )
                }
                AdminNavTab.APP_CONFIG -> {
                    AppConfigTab(
                        appConfig = appConfig,
                        onSaveConfig = { updated -> viewModel.saveAppConfig(updated) }
                    )
                }
                AdminNavTab.TRIAL_SETTINGS -> {
                    TrialSettingsTab(
                        appConfig = appConfig,
                        onSaveConfig = { updated -> viewModel.saveAppConfig(updated) }
                    )
                }
                AdminNavTab.AI_CONTROL -> {
                    AiControlTab(
                        appConfig = appConfig,
                        onSaveConfig = { updated -> viewModel.saveAppConfig(updated) }
                    )
                }
            }

            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                    color = PrimaryBlue
                )
            }
        }
    }

    if (showAddUserDialog) {
        AddUserDialog(
            defaultTrialDays = appConfig.freeTrialDaysDefault,
            onDismiss = { showAddUserDialog = false },
            onSave = { newUser ->
                viewModel.addOrUpdateFirebaseUser(newUser)
                showAddUserDialog = false
            }
        )
    }

    selectedUserForEdit?.let { user ->
        EditUserDialog(
            user = user,
            onDismiss = { selectedUserForEdit = null },
            onSave = { updated ->
                viewModel.addOrUpdateFirebaseUser(updated)
                selectedUserForEdit = null
            },
            onDelete = {
                viewModel.deleteFirebaseUser(user)
                selectedUserForEdit = null
            },
            onUnbind = {
                viewModel.unbindUserDevice(user)
                selectedUserForEdit = null
            }
        )
    }

    selectedUserForNotification?.let { user ->
        SendNotificationDialog(
            targetUser = user,
            onDismiss = { selectedUserForNotification = null },
            onSend = { msg ->
                viewModel.sendDirectNotificationToUser(user, msg)
                selectedUserForNotification = null
            }
        )
    }

    selectedUserForExtend?.let { user ->
        ExtendSubscriptionDialog(
            user = user,
            onDismiss = { selectedUserForExtend = null },
            onExtend = { days, isLifetime ->
                viewModel.extendUserSubscription(user, days, isLifetime)
                selectedUserForExtend = null
            }
        )
    }
}

@Composable
fun UsersManagementTab(
    users: List<FirebaseUserAccount>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    statusFilter: String,
    onStatusFilterChange: (String) -> Unit,
    onAddUserClick: () -> Unit,
    onEditUser: (FirebaseUserAccount) -> Unit,
    onSendNotification: (FirebaseUserAccount) -> Unit,
    onExtendUser: (FirebaseUserAccount) -> Unit,
    onDeleteUser: (FirebaseUserAccount) -> Unit,
    onToggleSuspend: (FirebaseUserAccount) -> Unit,
    onUnbindDevice: (FirebaseUserAccount) -> Unit
) {
    val filteredUsers = remember(users, searchQuery, statusFilter) {
        users.filter { user ->
            val matchSearch = searchQuery.isBlank() ||
                    user.email.contains(searchQuery, ignoreCase = true) ||
                    user.name.contains(searchQuery, ignoreCase = true) ||
                    user.phone.contains(searchQuery) ||
                    user.boundDeviceId.contains(searchQuery, ignoreCase = true)

            val matchFilter = when (statusFilter) {
                "ACTIVE" -> user.status == "active" && !user.isExpired
                "SUSPENDED" -> user.status == "suspended"
                "EXPIRED" -> user.isExpired
                "BANNED" -> user.status == "banned"
                else -> true
            }
            matchSearch && matchFilter
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "قائمة حسابات المشتركين",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimaryLight
                            )
                            Text(
                                text = "إجمالي الحسابات: ${users.size} | معروض: ${filteredUsers.size}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondaryLight
                            )
                        }

                        Button(
                            onClick = onAddUserClick,
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("إضافة مستخدم", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("بحث بالبريد أو الاسم أو الهاتف أو معرف الجهاز...", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMutedLight) },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFF8FAFC),
                            unfocusedContainerColor = Color(0xFFF8FAFC),
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = BorderLight
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = statusFilter == "ALL",
                            onClick = { onStatusFilterChange("ALL") },
                            label = { Text("الكل (${users.size})", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = statusFilter == "ACTIVE",
                            onClick = { onStatusFilterChange("ACTIVE") },
                            label = { Text("نشط (${users.count { it.status == "active" && !it.isExpired }})", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = statusFilter == "SUSPENDED",
                            onClick = { onStatusFilterChange("SUSPENDED") },
                            label = { Text("معلق (${users.count { it.status == "suspended" }})", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = statusFilter == "EXPIRED",
                            onClick = { onStatusFilterChange("EXPIRED") },
                            label = { Text("منتهي (${users.count { it.isExpired }})", fontSize = 11.sp) }
                        )
                    }
                }
            }
        }

        if (filteredUsers.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.GroupOff,
                            contentDescription = null,
                            tint = TextMutedLight,
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isBlank()) "لا يوجد مستخدمين مسجلين بعد" else "لا توجد نتائج مطابقة للبحث",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextSecondaryLight
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "اضغط على 'إضافة مستخدم' لإنشاء حساب جديد للمشتركين",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMutedLight
                        )
                    }
                }
            }
        } else {
            items(filteredUsers, key = { it.id }) { user ->
                UserCardItem(
                    user = user,
                    onEdit = { onEditUser(user) },
                    onSendNotification = { onSendNotification(user) },
                    onExtend = { onExtendUser(user) },
                    onDelete = { onDeleteUser(user) },
                    onToggleSuspend = { onToggleSuspend(user) },
                    onUnbindDevice = { onUnbindDevice(user) }
                )
            }
        }
    }
}

@Composable
fun UserCardItem(
    user: FirebaseUserAccount,
    onEdit: () -> Unit,
    onSendNotification: () -> Unit,
    onExtend: () -> Unit,
    onDelete: () -> Unit,
    onToggleSuspend: () -> Unit,
    onUnbindDevice: () -> Unit
) {
    val sdf = remember { SimpleDateFormat("yyyy/MM/dd", Locale.ENGLISH) }
    val isSuspended = user.status == "suspended"
    val isBanned = user.status == "banned"

    val statusColor = when {
        isBanned -> Color(0xFFEF4444)
        isSuspended -> Color(0xFFF59E0B)
        user.isExpired -> Color(0xFF6B7280)
        else -> Color(0xFF10B981)
    }

    val statusLabel = when {
        isBanned -> "محظور ⛔"
        isSuspended -> "معلق ⏸"
        user.isExpired -> "منتهي الصلاحية ⌛"
        user.isLifetime -> "مدى الحياة ♾️"
        else -> "نشط ✓"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = CircleShape,
                        color = if (user.role == "admin") Color(0xFFEF4444).copy(alpha = 0.15f) else PrimaryBlue.copy(alpha = 0.1f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (user.role == "admin") Icons.Default.Shield else Icons.Default.Person,
                                contentDescription = null,
                                tint = if (user.role == "admin") Color(0xFFEF4444) else PrimaryBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = user.name.ifBlank { "مستخدم" },
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimaryLight
                            )
                            if (user.role == "admin") {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFFEF4444).copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "المدير",
                                        color = Color(0xFFEF4444),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = user.email,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondaryLight
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = statusColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = statusLabel,
                        color = statusColor,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = BorderLight.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(10.dp))

            // User & Device details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "كلمة المرور: ${user.password}", fontSize = 11.sp, color = TextSecondaryLight)
                    if (user.phone.isNotBlank()) {
                        Text(text = "الهاتف: ${user.phone}", fontSize = 11.sp, color = TextSecondaryLight)
                    }
                    // Device Binding info
                    if (user.boundDeviceId.isNotBlank()) {
                        Text(
                            text = "📱 الجهاز: ${user.deviceModel.ifBlank { "مقترن" }}",
                            fontSize = 11.sp,
                            color = PrimaryNavy,
                            fontWeight = FontWeight.Medium
                        )
                    } else {
                        Text(
                            text = "📱 الجهاز: غير مقترن بجهاز بعد",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    val expDate = if (user.isLifetime) "غير محدود (مدى الحياة)" else if (user.expiryTimestamp > 0) sdf.format(Date(user.expiryTimestamp)) else "غير محدد"
                    Text(
                        text = "الانتهاء: $expDate",
                        fontSize = 11.sp,
                        color = if (user.isExpired) Color(0xFFEF4444) else TextSecondaryLight,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "أقصى راوترات: ${user.maxRouters} | أجهزة مسموحة: ${user.maxAllowedDevices}",
                        fontSize = 10.sp,
                        color = TextMutedLight
                    )
                }
            }

            if (user.networkName.isNotBlank() || user.governorate.isNotBlank() || user.city.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFF0FDF4),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = "🏢 شبكة: ${user.networkName.ifBlank { "غير محدد" }} | ${user.governorate} - ${user.city}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF166534)
                        )
                        if (user.networkLocation.isNotBlank()) {
                            Text(
                                text = "📍 إحداثيات الموقع: ${user.networkLocation}",
                                fontSize = 10.sp,
                                color = Color(0xFF15803D)
                            )
                        }
                    }
                }
            }

            val activationCode = remember(user.licenseRequestCode, user.email) {
                DeviceUtils.generateActivationCode(user.licenseRequestCode.ifBlank { user.email })
            }
            val cardContext = LocalContext.current

            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (!user.isActivated || user.status == "pending_activation") Color(0xFFFEF2F2) else Color(0xFFF8FAFC),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (!user.isActivated || user.status == "pending_activation") Color(0xFFFCA5A5) else Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (!user.isActivated || user.status == "pending_activation") "⚠️ حساب بانتظار الترخيص والتفعيل" else "🔑 كود ترخيص الجهاز والحساب:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (!user.isActivated || user.status == "pending_activation") Color(0xFFB91C1C) else PrimaryNavy
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = PrimaryNavy.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = activationCode,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = PrimaryNavy,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (user.licenseRequestCode.isNotBlank()) {
                        Text(
                            text = "بصمة الجهاز / طلب الترخيص: ${user.licenseRequestCode}",
                            fontSize = 10.sp,
                            color = TextSecondaryLight
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val clipboard = cardContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Activation Code", activationCode))
                                Toast.makeText(cardContext, "تم نسخ كود التفعيل: $activationCode", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("نسخ الكود", fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                val cleanPhone = if (user.phone.startsWith("7")) "967${user.phone}" else user.phone
                                val msg = """
                                    مرحباً بك ${user.name} في تطبيق TALAL PRO 📡
                                    تم اعتماد وتفعيل حسابك بنجاح!
                                    
                                    🔑 كود التفعيل الخاص بجهازك هو:
                                    $activationCode
                                    
                                    يرجى إدخاله في خانة التفعيل بالتطبيق للاستمتاع بكافة المزايا.
                                """.trimIndent()
                                try {
                                    val uri = Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=${Uri.encode(msg)}")
                                    val i = Intent(Intent.ACTION_VIEW, uri)
                                    cardContext.startActivity(i)
                                } catch (e: Exception) {
                                    Toast.makeText(cardContext, "تعذر فتح تطبيق واتساب", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.weight(1.3f)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("إرسال لواتساب 📲", fontSize = 11.sp)
                        }
                    }
                }
            }

            if (!user.directMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFFEF3C7),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "إشعار موجه: ${user.directMessage}",
                        fontSize = 11.sp,
                        color = Color(0xFF92400E),
                        modifier = Modifier.padding(6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Device Unbind button
                if (user.boundDeviceId.isNotBlank()) {
                    OutlinedButton(
                        onClick = onUnbindDevice,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEA580C))
                    ) {
                        Icon(Icons.Default.PhonelinkErase, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("فك الجهاز", fontSize = 11.sp)
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onExtend,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.HourglassTop,
                            contentDescription = "تمديد الصلاحية",
                            tint = PrimaryBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onSendNotification,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "إرسال إشعار",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onToggleSuspend,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isSuspended) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = if (isSuspended) "تفعيل" else "تعليق",
                            tint = if (isSuspended) Color(0xFF10B981) else Color(0xFFF59E0B),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "تعديل",
                            tint = PrimaryNavy,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    if (user.role != "admin" && !user.email.equals("abdualhamid100@gmail.com", ignoreCase = true)) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "حذف",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatsManagementTab(
    users: List<FirebaseUserAccount>,
    appConfig: FirebaseAppConfig
) {
    val totalUsers = users.size
    val activeUsers = users.count { it.status == "active" && !it.isExpired }
    val suspendedUsers = users.count { it.status == "suspended" }
    val expiredUsers = users.count { it.isExpired }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "إحصائيات المنظومة والمستخدمين",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = TextPrimaryLight
            )
            Text(
                text = "متابعة نشاط الحسابات وحالات التراخيص وفترات التجربة من Firebase مباشرة",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondaryLight
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    title = "إجمالي الحسابات",
                    value = "$totalUsers",
                    icon = Icons.Default.People,
                    color = PrimaryBlue,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "الحسابات النشطة",
                    value = "$activeUsers",
                    icon = Icons.Default.CheckCircle,
                    color = Color(0xFF10B981),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    title = "الحسابات المعلقة",
                    value = "$suspendedUsers",
                    icon = Icons.Default.PauseCircle,
                    color = Color(0xFFF59E0B),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "المنتهية الصلاحية",
                    value = "$expiredUsers",
                    icon = Icons.Default.TimerOff,
                    color = Color(0xFFEF4444),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "تفاصيل الإعدادات والحالة العامة",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimaryLight
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    StatRow(label = "الربط السحابي (Firebase RTDB)", value = "متصل بنجاح ✓")
                    HorizontalDivider(color = BorderLight.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 8.dp))
                    StatRow(label = "الفترة المجانية الافتراضية", value = if (appConfig.isTrialEnabled) "مفعلة (${appConfig.freeTrialDaysDefault} يوم)" else "معطلة")
                    HorizontalDivider(color = BorderLight.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 8.dp))
                    StatRow(label = "وضع الصيانة العام", value = if (appConfig.isMaintenanceMode) "قيد التشغيل ⚠️" else "متوقف (طبيعي) ✓")
                    HorizontalDivider(color = BorderLight.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 8.dp))
                    StatRow(label = "المطور المعتمد", value = appConfig.developerName)
                }
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, fontSize = 12.sp, color = TextSecondaryLight)
                Surface(
                    shape = CircleShape,
                    color = color.copy(alpha = 0.12f),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryLight
            )
        }
    }
}

@Composable
fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 13.sp, color = TextSecondaryLight)
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimaryLight)
    }
}

@Composable
fun AppConfigTab(
    appConfig: FirebaseAppConfig,
    onSaveConfig: (FirebaseAppConfig) -> Unit
) {
    var config by remember(appConfig) { mutableStateOf(appConfig) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "التحكم بأقسام وواجهات التطبيق",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = TextPrimaryLight
            )
            Text(
                text = "تفعيل أو تعطيل الأقسام والخصائص لجميع مستخدمي التطبيق من السيرفر مباشرة",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondaryLight
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "الأقسام والوحدات الرئيسية",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimaryLight
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    ConfigSwitchRow(
                        title = "قسم راوترات MikroTik",
                        subtitle = "استعراض وإضافة وإدارة راوترات المايكروتيك",
                        checked = config.enabledTabs.contains("ROUTERS"),
                        onCheckedChange = { checked ->
                            val current = config.enabledTabs.toMutableList()
                            if (checked) { if (!current.contains("ROUTERS")) current.add("ROUTERS") } else current.remove("ROUTERS")
                            config = config.copy(enabledTabs = current)
                        }
                    )
                    HorizontalDivider(color = BorderLight.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 8.dp))

                    ConfigSwitchRow(
                        title = "قسم أجهزة الشبكة (الأبراج والمودمات)",
                        subtitle = "فحص واستيراد ومراقبة الأجهزة والمستشعرات",
                        checked = config.enabledTabs.contains("DEVICES"),
                        onCheckedChange = { checked ->
                            val current = config.enabledTabs.toMutableList()
                            if (checked) { if (!current.contains("DEVICES")) current.add("DEVICES") } else current.remove("DEVICES")
                            config = config.copy(enabledTabs = current)
                        }
                    )
                    HorizontalDivider(color = BorderLight.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 8.dp))

                    ConfigSwitchRow(
                        title = "خريطة الشبكة التفاعلية",
                        subtitle = "عرض الأجهزة ومواقعها وطوبولوجيا الربط على الخريطة",
                        checked = config.enabledTabs.contains("MAP"),
                        onCheckedChange = { checked ->
                            val current = config.enabledTabs.toMutableList()
                            if (checked) { if (!current.contains("MAP")) current.add("MAP") } else current.remove("MAP")
                            config = config.copy(enabledTabs = current)
                        }
                    )
                    HorizontalDivider(color = BorderLight.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 8.dp))

                    ConfigSwitchRow(
                        title = "المساعد الذكي (AI Assistant)",
                        subtitle = "التحليل الذكي لمشاكل الشبكة واقتراحات الحلول",
                        checked = config.enabledTabs.contains("ASSISTANT"),
                        onCheckedChange = { checked ->
                            val current = config.enabledTabs.toMutableList()
                            if (checked) { if (!current.contains("ASSISTANT")) current.add("ASSISTANT") } else current.remove("ASSISTANT")
                            config = config.copy(enabledTabs = current)
                        }
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "الإعدادات العامة ورسائل النظام",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimaryLight
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    ConfigSwitchRow(
                        title = "وضع الصيانة العام",
                        subtitle = "إيقاف التطبيق مؤقتاً لجميع المستخدمين وإظهار رسالة الصيانة",
                        checked = config.isMaintenanceMode,
                        onCheckedChange = { config = config.copy(isMaintenanceMode = it) }
                    )

                    HorizontalDivider(color = BorderLight.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 10.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (config.allowGuestAccess) Color(0xFFECFDF5) else Color(0xFFF8FAFC),
                        border = BorderStroke(
                            1.dp,
                            if (config.allowGuestAccess) Color(0xFF10B981) else Color(0xFFCBD5E1)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            ConfigSwitchRow(
                                title = "تشغيل التطبيق بدون التسجيل والدخول",
                                subtitle = "السماح بفتح التطبيق وإدارة الشبكة مباشرة للجميع بدون شاشة تسجيل الدخول أو التفعيل",
                                checked = config.allowGuestAccess,
                                onCheckedChange = { enabled ->
                                    val updated = config.copy(allowGuestAccess = enabled)
                                    config = updated
                                    onSaveConfig(updated)
                                }
                            )
                            Text(
                                text = if (config.allowGuestAccess) {
                                    "🟢 الحالة الحالية: مفعّل — يدخل التطبيق مباشرة بدون طلب حساب"
                                } else {
                                    "🔒 الحالة الحالية: معطّل — يتطلب تسجيل الدخول وتفعيل الحساب"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (config.allowGuestAccess) Color(0xFF047857) else TextSecondaryLight,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = config.systemNotice,
                        onValueChange = { config = config.copy(systemNotice = it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("شريط الإعلانات العام في أعلى التطبيق") },
                        placeholder = { Text("رسالة عامة تظهر لجميع المشتركين...") },
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { onSaveConfig(config) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("حفظ التغييرات في الفايربيس", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun AiControlTab(
    appConfig: FirebaseAppConfig,
    onSaveConfig: (FirebaseAppConfig) -> Unit
) {
    var aiPermissions by remember(appConfig) { mutableStateOf(appConfig.aiPermissions) }
    var providers by remember(appConfig) { mutableStateOf(appConfig.aiProviders) }
    var showAddProviderDialog by remember { mutableStateOf(false) }
    var testingProviderId by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "التحكم بالمساعد الشبكي الذكي (AI Integration)",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = TextPrimaryLight
            )
            Text(
                text = "ربط وتكامل مع Gemini و OpenAI ووكلاء AI مخصصين وفحص صحة التوكن والصلاحيات الكاملة للراوتر",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondaryLight
            )
        }

        // 1. AI Providers & Agents Management
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
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
                        Column {
                            Text(
                                text = "وكلاء ونماذج الذكاء الاصطناعي (AI Providers)",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimaryLight
                            )
                            Text(
                                text = "يمكنك إضافة أكثر من وكيل و API والتبديل بينهم وفحص الاتصال",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondaryLight
                            )
                        }

                        Button(
                            onClick = { showAddProviderDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("إضافة وكيل", fontSize = 12.sp)
                        }
                    }

                    HorizontalDivider(color = BorderLight.copy(alpha = 0.5f))

                    providers.forEach { provider ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (provider.isDefault) Color(0xFFEFF6FF) else Color(0xFFF8FAFC),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (provider.isDefault) PrimaryBlue else Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = CircleShape,
                                            color = if (provider.isDefault) PrimaryBlue else Color(0xFF64748B),
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    Icons.Default.SmartToy,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(provider.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = PrimaryNavy)
                                                if (provider.isDefault) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Surface(shape = RoundedCornerShape(4.dp), color = PrimaryBlue.copy(alpha = 0.15f)) {
                                                        Text("الوكيل الافتراضي", color = PrimaryBlue, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                                    }
                                                }
                                            }
                                            Text("النوع: ${provider.type} | الموديل: ${provider.modelName}", fontSize = 10.sp, color = TextSecondaryLight)
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (provider.lastTestStatus.contains("✓") || provider.lastTestStatus.contains("متصل")) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)
                                    ) {
                                        Text(
                                            text = provider.lastTestStatus,
                                            color = if (provider.lastTestStatus.contains("✓") || provider.lastTestStatus.contains("متصل")) Color(0xFF166534) else Color(0xFFB91C1C),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = "رابط الـ API: ${provider.apiUrl}",
                                    fontSize = 10.sp,
                                    color = TextMutedLight
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Test Connection Button
                                    OutlinedButton(
                                        onClick = {
                                            testingProviderId = provider.id
                                            scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                                val startTime = System.currentTimeMillis()
                                                var isSuccess = false
                                                var statusMsg = ""
                                                try {
                                                    val client = okhttp3.OkHttpClient.Builder()
                                                        .connectTimeout(8, java.util.concurrent.TimeUnit.SECONDS)
                                                        .readTimeout(8, java.util.concurrent.TimeUnit.SECONDS)
                                                        .build()
                                                    val req = okhttp3.Request.Builder()
                                                        .url(provider.apiUrl)
                                                        .get()
                                                        .build()
                                                    val resp = client.newCall(req).execute()
                                                    val latency = System.currentTimeMillis() - startTime
                                                    if (resp.code in 200..404) {
                                                        isSuccess = true
                                                        statusMsg = "متصل بنجاح (${latency}ms) ✓"
                                                    } else if (resp.code == 401 || resp.code == 403) {
                                                        statusMsg = "التوكن منتهي أو غير صالح (401)"
                                                    } else {
                                                        statusMsg = "استجابة سيرفر: ${resp.code}"
                                                    }
                                                } catch (e: Exception) {
                                                    statusMsg = "فشل الاتصال: ${e.localizedMessage ?: "عدم استجابة"}"
                                                }

                                                val updatedList = providers.map { p ->
                                                    if (p.id == provider.id) {
                                                        p.copy(
                                                            lastTestStatus = statusMsg,
                                                            lastTestTimestamp = System.currentTimeMillis(),
                                                            lastLatencyMs = System.currentTimeMillis() - startTime
                                                        )
                                                    } else p
                                                }
                                                providers = updatedList
                                                onSaveConfig(appConfig.copy(aiProviders = updatedList))
                                                testingProviderId = null
                                            }
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        if (testingProviderId == provider.id) {
                                            CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp)
                                        } else {
                                            Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(14.dp))
                                        }
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("فحص الاتصال والتوكن", fontSize = 11.sp)
                                    }

                                    if (!provider.isDefault) {
                                        Button(
                                            onClick = {
                                                val updatedList = providers.map { p ->
                                                    p.copy(isDefault = p.id == provider.id)
                                                }
                                                providers = updatedList
                                                onSaveConfig(appConfig.copy(aiProviders = updatedList))
                                                Toast.makeText(context, "تم تعيين [${provider.name}] كوكيل افتراضي", Toast.LENGTH_SHORT).show()
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text("تعيين كافتراضي", fontSize = 11.sp)
                                        }

                                        IconButton(
                                            onClick = {
                                                val updatedList = providers.filter { it.id != provider.id }
                                                providers = updatedList
                                                onSaveConfig(appConfig.copy(aiProviders = updatedList))
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Full RouterOS Permissions (Activated upon User Request)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "صلاحيات تنفيذ أوامر RouterOS في المايكروتك",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimaryLight
                    )
                    Text(
                        text = "المساعد الذكي مبرمج على الصلاحية الكاملة للتنفيذ التلقائي ولكن عند طلب وموافقة المستخدم فقط",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF0284C7)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    ConfigSwitchRow(
                        title = "تنفيذ أوامر الفحص والتشخيص اللحظي",
                        subtitle = "فحص المعالج، الرام، درجات الحرارة، والفولتية عند طلب المستخدم",
                        checked = aiPermissions.allowRouterCommands,
                        onCheckedChange = { aiPermissions = aiPermissions.copy(allowRouterCommands = it) }
                    )
                    HorizontalDivider(color = BorderLight.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 8.dp))

                    ConfigSwitchRow(
                        title = "أمر إعادة تشغيل الراوتر (Router Reboot)",
                        subtitle = "إمكانية تنفيذ إعادة تشغيل الراوتر عند طوارئ الشبكة بطلب المستخدم",
                        checked = aiPermissions.allowReboot,
                        onCheckedChange = { aiPermissions = aiPermissions.copy(allowReboot = it) }
                    )
                    HorizontalDivider(color = BorderLight.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 8.dp))

                    ConfigSwitchRow(
                        title = "إدارة مستخدمي اليوزرمانجر والهوتسبوت",
                        subtitle = "إضافة كروت، فصل جلسات، أو ضبط بروفايلات الشبكة عبر المساعد",
                        checked = aiPermissions.allowUserManagerOps,
                        onCheckedChange = { aiPermissions = aiPermissions.copy(allowUserManagerOps = it, allowHotspotOps = it) }
                    )
                    HorizontalDivider(color = BorderLight.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 8.dp))

                    ConfigSwitchRow(
                        title = "حماية الشبكة وحظر المواقع الضارة (Firewall Rules)",
                        subtitle = "تطبيق قواعد الأمان وحظر المواقع غير المرغوبة وتسريب DNS",
                        checked = true,
                        onCheckedChange = { }
                    )
                    HorizontalDivider(color = BorderLight.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 8.dp))

                    ConfigSwitchRow(
                        title = "تسريع وتحسين التصفح (Flush DNS & ARP)",
                        subtitle = "تنظيف الكاش وتحديث جداول التوجيه لتحسين استجابة الشبكة",
                        checked = true,
                        onCheckedChange = { }
                    )
                }
            }
        }

        // 3. System Prompt Customization
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "التعليمات البرمجية وهوية المساعد (System Instructions)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimaryLight
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = aiPermissions.systemPromptOverride,
                        onValueChange = { aiPermissions = aiPermissions.copy(systemPromptOverride = it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("التعليمات المخصصة للمساعد (Custom System Prompt)") },
                        placeholder = { Text("أنت خبير شبكات المايكروتيك لتطبيق TALAL PRO...") },
                        shape = RoundedCornerShape(10.dp),
                        minLines = 4
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            onSaveConfig(appConfig.copy(aiPermissions = aiPermissions, aiProviders = providers))
                            Toast.makeText(context, "تم حفظ إعدادات المساعد والوكلاء بنجاح ✓", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("حفظ كافة إعدادات المساعد والوكلاء", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showAddProviderDialog) {
        AddAiProviderDialog(
            onDismiss = { showAddProviderDialog = false },
            onAdd = { newProvider ->
                val updated = providers + newProvider
                providers = updated
                onSaveConfig(appConfig.copy(aiProviders = updated))
                showAddProviderDialog = false
                Toast.makeText(context, "تمت إضافة الوكيل [${newProvider.name}] بنجاح", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun AddAiProviderDialog(
    onDismiss: () -> Unit,
    onAdd: (AiProviderConfig) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("OPENAI") }
    var apiUrl by remember { mutableStateOf("https://api.openai.com/v1") }
    var apiKey by remember { mutableStateOf("") }
    var modelName by remember { mutableStateOf("gpt-4o") }
    var systemPrompt by remember { mutableStateOf("أنت المساعد الذكي وخبير شبكات المايكروتك لتطبيق TALAL PRO.") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp)
        ) {
            LazyColumn(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    Text("إضافة وكيل ذكاء اصطناعي جديد (AI Agent)", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PrimaryNavy)
                    Text("اربط التطبيق مع أي سيرفر أو API خارجي مثل OpenAI، DeepSeek، Claude، أو سيرفرك الخاص", fontSize = 11.sp, color = TextSecondaryLight)
                }

                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("اسم الوكيل / الخدمة *") },
                        placeholder = { Text("مثال: وكيل سيرفر العملات / DeepSeek") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                item {
                    Text("نوع المنظومة / البروتوكول:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        FilterChip(
                            selected = type == "OPENAI",
                            onClick = {
                                type = "OPENAI"
                                apiUrl = "https://api.openai.com/v1"
                                modelName = "gpt-4o"
                            },
                            label = { Text("OpenAI", fontSize = 10.sp) }
                        )
                        FilterChip(
                            selected = type == "GEMINI",
                            onClick = {
                                type = "GEMINI"
                                apiUrl = "https://generativelanguage.googleapis.com/v1beta"
                                modelName = "gemini-2.5-flash"
                            },
                            label = { Text("Gemini", fontSize = 10.sp) }
                        )
                        FilterChip(
                            selected = type == "CUSTOM_PROXY",
                            onClick = {
                                type = "CUSTOM_PROXY"
                                apiUrl = "https://your-custom-ai-proxy.com/api"
                                modelName = "custom-agent"
                            },
                            label = { Text("وكيل مخصص", fontSize = 10.sp) }
                        )
                        FilterChip(
                            selected = type == "OLLAMA",
                            onClick = {
                                type = "OLLAMA"
                                apiUrl = "http://192.168.1.100:11434"
                                modelName = "llama3"
                            },
                            label = { Text("Ollama محلي", fontSize = 10.sp) }
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = apiUrl,
                        onValueChange = { apiUrl = it },
                        label = { Text("رابط الـ API Endpoint *") },
                        placeholder = { Text("https://...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                item {
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        label = { Text("رمز التوكن / API Key") },
                        placeholder = { Text("sk-...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                item {
                    OutlinedTextField(
                        value = modelName,
                        onValueChange = { modelName = it },
                        label = { Text("اسم النموذج (Model Name)") },
                        placeholder = { Text("gpt-4o, claude-3-5, gemini-flash...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("إلغاء")
                        }

                        Button(
                            onClick = {
                                if (name.isNotBlank() && apiUrl.isNotBlank()) {
                                    val newP = AiProviderConfig(
                                        name = name.trim(),
                                        type = type,
                                        apiUrl = apiUrl.trim(),
                                        apiKey = apiKey.trim(),
                                        modelName = modelName.trim(),
                                        systemPrompt = systemPrompt.trim(),
                                        isDefault = false
                                    )
                                    onAdd(newP)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                        ) {
                            Text("إضافة وحفظ", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConfigSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimaryLight)
            Text(text = subtitle, fontSize = 11.sp, color = TextSecondaryLight)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = PrimaryBlue
            )
        )
    }
}

@Composable
fun TrialSettingsTab(
    appConfig: FirebaseAppConfig,
    onSaveConfig: (FirebaseAppConfig) -> Unit
) {
    var trialEnabled by remember(appConfig) { mutableStateOf(appConfig.isTrialEnabled) }
    var trialDaysText by remember(appConfig) { mutableStateOf(appConfig.freeTrialDaysDefault.toString()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "التحكم بصلاحية الفترة المجانية",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = TextPrimaryLight
            )
            Text(
                text = "تحديد صلاحية الاشتراك التجريبي للمستخدمين الجدد وفترة السماح قبل التجديد",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondaryLight
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    ConfigSwitchRow(
                        title = "تفعيل الفترة المجانية للمستخدمين الجدد",
                        subtitle = "منح المستخدم صلاحية استخدام محددة تلقائياً عند التسجيل",
                        checked = trialEnabled,
                        onCheckedChange = { trialEnabled = it }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = trialDaysText,
                        onValueChange = { trialDaysText = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("عدد أيام الفترة المجانية (أيام)") },
                        placeholder = { Text("مثال: 7 أو 14 أو 30") },
                        shape = RoundedCornerShape(10.dp),
                        leadingIcon = { Icon(Icons.Default.HourglassBottom, contentDescription = null) }
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            val days = trialDaysText.toIntOrNull() ?: 30
                            onSaveConfig(appConfig.copy(isTrialEnabled = trialEnabled, freeTrialDaysDefault = days))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("حفظ إعدادات الفترة المجانية", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun AddUserDialog(
    defaultTrialDays: Int,
    onDismiss: () -> Unit,
    onSave: (FirebaseUserAccount) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var daysText by remember { mutableStateOf(defaultTrialDays.toString()) }
    var isLifetime by remember { mutableStateOf(false) }
    var maxDevicesText by remember { mutableStateOf("1") }
    var maxRoutersText by remember { mutableStateOf("10") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp)
        ) {
            LazyColumn(modifier = Modifier.padding(20.dp)) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "إضافة حساب مشترك جديد",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimaryLight
                        )
                        IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "إغلاق")
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                item {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it; errorMsg = null },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("البريد الإلكتروني *") },
                        placeholder = { Text("user@example.com") },
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; errorMsg = null },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("كلمة المرور *") },
                        placeholder = { Text("أدخل كلمة المرور") },
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("اسم المشترك / الشبكة") },
                        placeholder = { Text("مثال: شبكة النور") },
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("رقم الهاتف للتواصل") },
                        placeholder = { Text("770000000") },
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = maxDevicesText,
                            onValueChange = { maxDevicesText = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("أقصى هواتف") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = maxRoutersText,
                            onValueChange = { maxRoutersText = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("أقصى راوترات") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("اشتراك مدى الحياة ♾️", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Switch(checked = isLifetime, onCheckedChange = { isLifetime = it })
                    }

                    if (!isLifetime) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = daysText,
                            onValueChange = { daysText = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("صلاحية الحساب (بالأيام)") },
                            placeholder = { Text("30") },
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )
                    }

                    errorMsg?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = it, color = Color(0xFFEF4444), fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (email.isBlank() || password.isBlank()) {
                                errorMsg = "يرجى كتابة البريد الإلكتروني وكلمة المرور"
                                return@Button
                            }
                            val days = daysText.toIntOrNull() ?: 30
                            val maxDev = maxDevicesText.toIntOrNull() ?: 1
                            val maxRout = maxRoutersText.toIntOrNull() ?: 10
                            val exp = if (isLifetime) 0L else System.currentTimeMillis() + (days.toLong() * 24 * 60 * 60 * 1000)
                            val newUser = FirebaseUserAccount(
                                email = email.trim(),
                                password = password.trim(),
                                name = name.trim(),
                                phone = phone.trim(),
                                isLifetime = isLifetime,
                                expiryTimestamp = exp,
                                trialDays = days,
                                maxAllowedDevices = maxDev,
                                maxRouters = maxRout,
                                status = "active",
                                role = "user"
                            )
                            onSave(newUser)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("إنشاء الحساب وحفظه بالفايربيس", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun EditUserDialog(
    user: FirebaseUserAccount,
    onDismiss: () -> Unit,
    onSave: (FirebaseUserAccount) -> Unit,
    onDelete: () -> Unit,
    onUnbind: () -> Unit
) {
    var name by remember { mutableStateOf(user.name) }
    var phone by remember { mutableStateOf(user.phone) }
    var password by remember { mutableStateOf(user.password) }
    var status by remember { mutableStateOf(user.status) }
    var role by remember { mutableStateOf(user.role) }
    var isActivated by remember { mutableStateOf(user.isActivated) }
    var allowDualAccounts by remember { mutableStateOf(user.allowedAccountsOnDevice >= 2) }
    var isLifetime by remember { mutableStateOf(user.isLifetime) }
    var maxDevicesText by remember { mutableStateOf(user.maxAllowedDevices.toString()) }
    var maxRoutersText by remember { mutableStateOf(user.maxRouters.toString()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp)
        ) {
            LazyColumn(modifier = Modifier.padding(20.dp)) {
                item {
                    Text(
                        text = "تعديل حساب: ${user.email}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimaryLight
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                item {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("كلمة المرور") },
                        shape = RoundedCornerShape(10.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("الاسم") },
                        shape = RoundedCornerShape(10.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("الهاتف") },
                        shape = RoundedCornerShape(10.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = maxDevicesText,
                            onValueChange = { maxDevicesText = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("أقصى هواتف") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = maxRoutersText,
                            onValueChange = { maxRoutersText = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("أقصى راوترات") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(text = "حالة الحساب:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        FilterChip(
                            selected = status == "active",
                            onClick = { status = "active" },
                            label = { Text("نشط", fontSize = 10.sp) }
                        )
                        FilterChip(
                            selected = status == "suspended",
                            onClick = { status = "suspended" },
                            label = { Text("معلق", fontSize = 10.sp) }
                        )
                        FilterChip(
                            selected = status == "banned",
                            onClick = { status = "banned" },
                            label = { Text("محظور", fontSize = 10.sp) }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(text = "صلاحيات الحساب (الدور):", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        FilterChip(
                            selected = role == "user",
                            onClick = { role = "user" },
                            label = { Text("مستخدم عادي", fontSize = 10.sp) }
                        )
                        FilterChip(
                            selected = role == "moderator",
                            onClick = { role = "moderator" },
                            label = { Text("مشرف شبكة", fontSize = 10.sp) }
                        )
                        FilterChip(
                            selected = role == "admin",
                            onClick = { role = "admin" },
                            label = { Text("مدير إداري", fontSize = 10.sp) }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("السماح بحسابين على نفس الجهاز 📱", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Switch(checked = allowDualAccounts, onCheckedChange = { allowDualAccounts = it })
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("الحساب معتمد ومفعل بالترخيص ✓", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Switch(checked = isActivated, onCheckedChange = { isActivated = it })
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("اشتراك مدى الحياة ♾️", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Switch(checked = isLifetime, onCheckedChange = { isLifetime = it })
                    }

                    if (user.boundDeviceId.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = onUnbind,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEA580C)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.PhonelinkErase, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("فك ربط الجهاز الحالي للمستخدم")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val updated = user.copy(
                                name = name,
                                phone = phone,
                                password = password,
                                status = status,
                                role = role,
                                isActivated = isActivated,
                                allowedAccountsOnDevice = if (allowDualAccounts) 2 else 1,
                                isLifetime = isLifetime,
                                maxAllowedDevices = if (allowDualAccounts) 2 else (maxDevicesText.toIntOrNull() ?: user.maxAllowedDevices),
                                maxRouters = maxRoutersText.toIntOrNull() ?: user.maxRouters
                            )
                            onSave(updated)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("حفظ التعديلات", fontWeight = FontWeight.Bold)
                    }

                    if (user.role != "admin" && !user.email.equals("abdualhamid100@gmail.com", ignoreCase = true)) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = onDelete,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("حذف هذا الحساب نهائياً")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExtendSubscriptionDialog(
    user: FirebaseUserAccount,
    onDismiss: () -> Unit,
    onExtend: (days: Int, isLifetime: Boolean) -> Unit
) {
    var addDaysText by remember { mutableStateOf("30") }
    var isLifetime by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "تمديد صلاحية: ${user.name.ifBlank { user.email }}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimaryLight
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("ترقية لمدى الحياة ♾️", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Switch(checked = isLifetime, onCheckedChange = { isLifetime = it })
                }

                if (!isLifetime) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = addDaysText,
                        onValueChange = { addDaysText = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("عدد الأيام الإضافية") },
                        placeholder = { Text("مثال: 30 أو 60 أو 365") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val days = addDaysText.toIntOrNull() ?: 30
                        onExtend(days, isLifetime)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.HourglassTop, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تأكيد التمديد", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SendNotificationDialog(
    targetUser: FirebaseUserAccount,
    onDismiss: () -> Unit,
    onSend: (message: String) -> Unit
) {
    var message by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "إرسال إشعار إلى: ${targetUser.name.ifBlank { targetUser.email }}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimaryLight
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("نص الرسالة / الإشعار الموجه للمستخدم") },
                    placeholder = { Text("أدخل تفاصيل التنبيه أو التجديد...") },
                    shape = RoundedCornerShape(10.dp),
                    minLines = 3
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (message.isNotBlank()) {
                            onSend(message.trim())
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("إرسال الإشعار الآن", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
