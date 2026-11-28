package com.example.ui.screens

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
import com.example.data.model.*
import com.example.ui.TalalProViewModel
import com.example.ui.components.A4SheetPreviewDialog
import com.example.ui.theme.*
import com.example.ui.utils.CardPdfGenerator

enum class HotspotTab(val title: String) {
    USERS("مستخدمي الهوتسبوت"),
    ACTIVE("الجلسات النشطة المتصلة"),
    PROFILES("الباقات والسرعات"),
    GENERATOR("توليد كروت هوتسبوت")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HotspotScreen(
    viewModel: TalalProViewModel,
    modifier: Modifier = Modifier
) {
    val routers by viewModel.allRouters.collectAsState()
    val selectedRouter by viewModel.selectedHotspotRouter.collectAsState()
    val users by viewModel.hotspotUsers.collectAsState()
    val activeSessions by viewModel.hotspotActiveSessions.collectAsState()
    val profiles by viewModel.hotspotProfiles.collectAsState()
    val isSyncing by viewModel.isSyncingHotspot.collectAsState()
    val cardTemplates by viewModel.cardTemplates.collectAsState()
    val lastGeneratedBatch by viewModel.lastGeneratedHotspotBatch.collectAsState()

    var currentTab by remember { mutableStateOf(HotspotTab.USERS) }
    var searchQuery by remember { mutableStateOf("") }
    var showAddUserDialog by remember { mutableStateOf(false) }
    var selectedUserForEdit by remember { mutableStateOf<HotspotUserEntity?>(null) }
    var selectedUserForProfileChange by remember { mutableStateOf<HotspotUserEntity?>(null) }
    var selectedUserForRenew by remember { mutableStateOf<HotspotUserEntity?>(null) }
    var selectedUserForDelete by remember { mutableStateOf<HotspotUserEntity?>(null) }

    LaunchedEffect(routers) {
        if (selectedRouter == null && routers.isNotEmpty()) {
            viewModel.selectHotspotRouter(routers.first())
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "إدارة الهوتسبوت (Hotspot Manager)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimaryLight
                        )
                        Text(
                            text = "إدارة مستخدمي الكروت، فصل الجلسات، وسرعات البروفايلات",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondaryLight
                        )
                    }
                },
                actions = {
                    selectedRouter?.let { router ->
                        IconButton(onClick = { viewModel.syncHotspotData(router) }) {
                            Icon(Icons.Default.Sync, contentDescription = "مزامنة", tint = PrimaryBlue)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF8FAFC),
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Router Selector Bar (Per Router independent)
            Surface(
                color = Color.White,
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Wifi, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("راوتر الهوتسبوت:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimaryLight)
                    }

                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF1F5F9),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
                            modifier = Modifier.clickable { expanded = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedRouter?.name ?: "اختر راوتر...",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = PrimaryNavy
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TextMutedLight)
                            }
                        }

                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            routers.forEach { r ->
                                DropdownMenuItem(
                                    text = { Text(r.name, fontWeight = if (r.id == selectedRouter?.id) FontWeight.Bold else FontWeight.Normal) },
                                    onClick = {
                                        viewModel.selectHotspotRouter(r)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (isSyncing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = PrimaryBlue)
            }

            // Category Grid Selector (شبكة أقسام الهوتسبوت الاحترافية)
            Surface(
                color = Color.White,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Section 1: Hotspot Users
                        HotspotGridCard(
                            title = "مستخدمي الهوتسبوت",
                            subtitle = "${users.size} مستخدم وكارت",
                            icon = Icons.Default.Group,
                            isSelected = currentTab == HotspotTab.USERS,
                            gradient = listOf(Color(0xFF0284C7), Color(0xFF0369A1)),
                            modifier = Modifier.weight(1f),
                            onClick = { currentTab = HotspotTab.USERS }
                        )

                        // Section 2: Active Sessions
                        HotspotGridCard(
                            title = "الجلسات النشطة المتصلة",
                            subtitle = "${activeSessions.size} متصل الآن",
                            icon = Icons.Default.OnlinePrediction,
                            isSelected = currentTab == HotspotTab.ACTIVE,
                            gradient = listOf(Color(0xFF16A34A), Color(0xFF15803D)),
                            modifier = Modifier.weight(1f),
                            onClick = { currentTab = HotspotTab.ACTIVE }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Section 3: Profiles
                        HotspotGridCard(
                            title = "الباقات والسرعات",
                            subtitle = "${profiles.size} باقة محددة",
                            icon = Icons.Default.Speed,
                            isSelected = currentTab == HotspotTab.PROFILES,
                            gradient = listOf(Color(0xFFEA580C), Color(0xFFC2410C)),
                            modifier = Modifier.weight(1f),
                            onClick = { currentTab = HotspotTab.PROFILES }
                        )

                        // Section 4: Generator
                        HotspotGridCard(
                            title = "توليد كروت بالدفعة",
                            subtitle = "توليد وطباعة فورية",
                            icon = Icons.Default.AutoAwesome,
                            isSelected = currentTab == HotspotTab.GENERATOR,
                            gradient = listOf(Color(0xFF8B5CF6), Color(0xFF7C3AED)),
                            modifier = Modifier.weight(1f),
                            onClick = { currentTab = HotspotTab.GENERATOR }
                        )
                    }
                }
            }

            // Tab Content
            when (currentTab) {
                HotspotTab.USERS -> {
                    HotspotUsersContent(
                        users = users,
                        searchQuery = searchQuery,
                        onSearchChange = { searchQuery = it },
                        onAddUserClick = { showAddUserDialog = true },
                        onEditUser = { selectedUserForEdit = it },
                        onChangeProfile = { selectedUserForProfileChange = it },
                        onRenewUser = { selectedUserForRenew = it },
                        onToggleUser = { user, en ->
                            selectedRouter?.let { r -> viewModel.toggleHotspotUser(r, user, en) }
                        },
                        onDeleteUser = { selectedUserForDelete = it }
                    )
                }
                HotspotTab.ACTIVE -> {
                    HotspotActiveContent(
                        sessions = activeSessions,
                        onOpenFullActiveScreen = {
                            viewModel.setTab(com.example.ui.ScreenTab.ACTIVE_USERS)
                        },
                        onDisconnect = { session ->
                            selectedRouter?.let { r -> viewModel.disconnectHotspotSession(r, session) }
                        }
                    )
                }
                HotspotTab.PROFILES -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF0B132B))
                            .padding(14.dp)
                    ) {
                        androidx.compose.foundation.lazy.LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item {
                                PackageManagementContent(
                                    viewModel = viewModel,
                                    router = selectedRouter,
                                    initialMode = "HOTSPOT"
                                )
                            }
                        }
                    }
                }
                HotspotTab.GENERATOR -> {
                    selectedRouter?.let { r ->
                        HotspotGeneratorContent(
                            router = r,
                            profiles = profiles,
                            cardTemplates = cardTemplates,
                            lastGeneratedBatch = lastGeneratedBatch,
                            onGenerate = { prefix, count, profile, len, uptime, bytes, priceLabel ->
                                viewModel.generateBatchHotspotCards(r, prefix, count, profile, len, uptime, bytes, priceLabel)
                            }
                        )
                    } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("يرجى اختيار راوتر أولاً لتوليد كروت الهوتسبوت", color = TextSecondaryLight)
                    }
                }
            }
        }
    }

    if (showAddUserDialog && selectedRouter != null) {
        AddHotspotUserDialog(
            profiles = profiles,
            onDismiss = { showAddUserDialog = false },
            onSave = { name, pass, prof, comment, uptime, bytes ->
                viewModel.addHotspotUser(selectedRouter!!, name, pass, prof, comment, uptime, bytes)
                showAddUserDialog = false
            }
        )
    }

    selectedUserForEdit?.let { user ->
        EditHotspotUserDialog(
            user = user,
            profiles = profiles,
            onDismiss = { selectedUserForEdit = null },
            onSave = { n, p, prof, c, u, b ->
                selectedRouter?.let { r ->
                    viewModel.editHotspotUser(r, user, n, p, prof, c, u, b)
                }
                selectedUserForEdit = null
            }
        )
    }

    selectedUserForProfileChange?.let { user ->
        ChangeHotspotProfileDialog(
            user = user,
            profiles = profiles,
            onDismiss = { selectedUserForProfileChange = null },
            onSave = { newProf ->
                selectedRouter?.let { r ->
                    viewModel.changeHotspotProfile(r, user, newProf)
                }
                selectedUserForProfileChange = null
            }
        )
    }

    selectedUserForRenew?.let { user ->
        RenewHotspotUserDialog(
            user = user,
            profiles = profiles,
            onDismiss = { selectedUserForRenew = null },
            onSave = { newProf, up, by ->
                selectedRouter?.let { r ->
                    viewModel.renewHotspotUser(r, user, newProf, up, by)
                }
                selectedUserForRenew = null
            }
        )
    }

    selectedUserForDelete?.let { user ->
        AlertDialog(
            onDismissRequest = { selectedUserForDelete = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(32.dp)) },
            title = { Text("حذف مستخدم الهوتسبوت", fontWeight = FontWeight.Bold) },
            text = { Text("هل أنت متأكد من رغبتك في حذف المستخدم [${user.name}] نهائياً من راوتر المايكروتك؟") },
            confirmButton = {
                Button(
                    onClick = {
                        selectedRouter?.let { r -> viewModel.deleteHotspotUser(r, user) }
                        selectedUserForDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("حذف الآن", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { selectedUserForDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
private fun HotspotUsersContent(
    users: List<HotspotUserEntity>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onAddUserClick: () -> Unit,
    onEditUser: (HotspotUserEntity) -> Unit,
    onChangeProfile: (HotspotUserEntity) -> Unit,
    onRenewUser: (HotspotUserEntity) -> Unit,
    onToggleUser: (HotspotUserEntity, Boolean) -> Unit,
    onDeleteUser: (HotspotUserEntity) -> Unit
) {
    val filtered = remember(users, searchQuery) {
        if (searchQuery.isBlank()) users
        else users.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.profile.contains(searchQuery, ignoreCase = true) ||
            it.comment.contains(searchQuery, ignoreCase = true)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = { Text("بحث عن مستخدم هوتسبوت...", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMutedLight) },
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Button(
                    onClick = onAddUserClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("إضافة مستخدم", fontSize = 12.sp)
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().padding(top = 20.dp)) {
                    Column(modifier = Modifier.padding(30.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.WifiOff, contentDescription = null, tint = TextMutedLight, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("لا يوجد مستخدمي هوتسبوت مسجلين", fontWeight = FontWeight.Bold, color = TextPrimaryLight)
                        Text("اضغط على 'إضافة مستخدم' أو قم بتوليد كروت جديدة", fontSize = 11.sp, color = TextSecondaryLight)
                    }
                }
            }
        } else {
            items(filtered, key = { it.id }) { user ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(14.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (user.isDisabled) Color(0xFFFCA5A5) else Color(0xFFE2E8F0)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (user.isDisabled) Color(0xFFFEE2E2) else if (user.isOnline) Color(0xFFDCFCE7) else PrimaryBlue.copy(alpha = 0.12f),
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (user.isDisabled) Icons.Default.PersonOff else if (user.isOnline) Icons.Default.Wifi else Icons.Default.Person,
                                            contentDescription = null,
                                            tint = if (user.isDisabled) Color(0xFFDC2626) else if (user.isOnline) Color(0xFF16A34A) else PrimaryBlue,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = user.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PrimaryNavy)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        if (user.isOnline) {
                                            Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFDCFCE7)) {
                                                Text("متصل الآن", color = Color(0xFF047857), fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (user.isDisabled) Color(0xFFFEE2E2) else Color(0xFFEFF6FF)
                                        ) {
                                            Text(
                                                text = if (user.isDisabled) "معطل" else user.profile,
                                                color = if (user.isDisabled) Color(0xFFDC2626) else PrimaryBlue,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "كلمة المرور: ${user.password} | استهلاك: ${user.bytesIn} / ${user.bytesOut}",
                                        fontSize = 11.sp,
                                        color = TextSecondaryLight
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = Color(0xFFF1F5F9))

                        // Hotspot Actions Bar: تعديل | تغيير الفئة | تفعيل/تعطيل | تجديد الكرت | حذف
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 1. Edit
                            OutlinedButton(
                                onClick = { onEditUser(user) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                modifier = Modifier.weight(1f).height(34.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("تعديل", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }

                            // 2. Change Profile
                            OutlinedButton(
                                onClick = { onChangeProfile(user) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                modifier = Modifier.weight(1.1f).height(34.dp)
                            ) {
                                Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("تغيير الفئة", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }

                            // 3. Renew Card
                            Button(
                                onClick = { onRenewUser(user) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                modifier = Modifier.weight(1f).height(34.dp)
                            ) {
                                Icon(Icons.Default.Autorenew, contentDescription = null, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("تجديد", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }

                            // 4. Toggle Enable/Disable
                            IconButton(
                                onClick = { onToggleUser(user, user.isDisabled) },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = if (user.isDisabled) Icons.Default.PlayArrow else Icons.Default.Pause,
                                    contentDescription = if (user.isDisabled) "تفعيل" else "تعطيل",
                                    tint = if (user.isDisabled) Color(0xFF16A34A) else Color(0xFFEA580C),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // 5. Delete
                            IconButton(
                                onClick = { onDeleteUser(user) },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HotspotActiveContent(
    sessions: List<HotspotActiveSessionEntity>,
    onOpenFullActiveScreen: () -> Unit,
    onDisconnect: (HotspotActiveSessionEntity) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            // Dedicated Banner to jump to the new professional Active Users & Devices Screen!
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF0FDF4),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0)),
                modifier = Modifier.fillMaxWidth().clickable { onOpenFullActiveScreen() }
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = Color(0xFFDCFCE7), modifier = Modifier.size(36.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Wifi, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("فتح صفحة المتصلين الآن الاحترافية 🚀", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF15803D))
                            Text("كشف نوع الأجهزة، الصلاحية، الرصيد المتبقي، طرد، وحظر فوري", fontSize = 10.sp, color = TextSecondaryLight)
                        }
                    }
                    Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color(0xFF16A34A))
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("الجلسات النشطة في الراوتر", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimaryLight)
                Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFDCFCE7)) {
                    Text("المتصلين: ${sessions.size}", color = Color(0xFF047857), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }
        }

        if (sessions.isEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(12.dp)) {
                    Box(modifier = Modifier.fillMaxWidth().padding(30.dp), contentAlignment = Alignment.Center) {
                        Text("لا توجد جلسات نشطة متصلة في الوقت الحالي", color = TextSecondaryLight)
                    }
                }
            }
        } else {
            items(sessions, key = { it.id }) { s ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = s.username, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = PrimaryNavy)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = s.ipAddress, fontSize = 11.sp, color = PrimaryBlue)
                            }
                            Text(
                                text = "MAC: ${s.macAddress} | مدة الاتصال: ${s.uptime}",
                                fontSize = 11.sp,
                                color = TextSecondaryLight
                            )
                        }

                        Button(
                            onClick = { onDisconnect(s) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFEE2E2)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("فصل الجلسة", color = Color(0xFFB91C1C), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HotspotProfilesContent(profiles: List<HotspotProfileEntity>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("باقات وبروفايلات الهوتسبوت", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimaryLight)
            Text("السرعات المحددة وعدد الأجهزة المسموحة لكل باقة", fontSize = 11.sp, color = TextSecondaryLight)
        }

        if (profiles.isEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(12.dp)) {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("لم يتم العثور على بروفايلات هوتسبوت محملة", color = TextSecondaryLight)
                    }
                }
            }
        } else {
            items(profiles, key = { it.id }) { prof ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = prof.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PrimaryNavy)
                            Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFEFF6FF)) {
                                Text("السرعة: ${prof.rateLimit}", color = PrimaryBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "المستخدمين المشتركين: ${prof.sharedUsers} | انتهاء الجلسة: ${prof.sessionTimeout}", fontSize = 12.sp, color = TextSecondaryLight)
                    }
                }
            }
        }
    }
}

@Composable
private fun HotspotGeneratorContent(
    router: RouterEntity,
    profiles: List<HotspotProfileEntity>,
    cardTemplates: List<CardTemplateEntity>,
    lastGeneratedBatch: List<GeneratedCardPrintItem>,
    onGenerate: (prefix: String, count: Int, profile: String, len: Int, uptime: String, bytes: String, priceLabel: String) -> Unit
) {
    val context = LocalContext.current
    var selectedTemplate by remember(cardTemplates) {
        mutableStateOf(
            cardTemplates.firstOrNull { it.linkedHotspotProfile.isNotBlank() }
                ?: cardTemplates.firstOrNull()
        )
    }
    var templateMenuExpanded by remember { mutableStateOf(false) }
    var profileMenuExpanded by remember { mutableStateOf(false) }
    var showA4Preview by remember { mutableStateOf(false) }

    var prefix by remember { mutableStateOf("1") }
    var countText by remember { mutableStateOf((selectedTemplate?.cardsCount ?: 51).toString()) }
    var lengthText by remember { mutableStateOf((selectedTemplate?.usernameDigits ?: 10).toString()) }
    var selectedProfile by remember(profiles) {
        mutableStateOf(
            selectedTemplate?.linkedHotspotProfile?.takeIf { it.isNotBlank() }
                ?: profiles.firstOrNull()?.name
                ?: "default"
        )
    }
    var limitUptime by remember { mutableStateOf("2h") }
    var limitBytes by remember { mutableStateOf("500M") }

    val activeTemplateForPrint = remember(selectedTemplate, lengthText, selectedProfile, countText) {
        (selectedTemplate ?: CardTemplateEntity()).copy(
            usernameDigits = lengthText.toIntOrNull()?.coerceIn(4, 18) ?: 10,
            linkedHotspotProfile = selectedProfile,
            cardsCount = countText.toIntOrNull()?.coerceIn(1, 500) ?: 51
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Template Sync Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(1.dp, Color(0xFFCCFBF1))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFCCFBF1),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Style, contentDescription = null, tint = Color(0xFF0F766E), modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("استيراد قالب التصميم والطباعة", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PrimaryNavy)
                            Text("يزامن طول الأرقام والباقة وتنسيق PDF تلقائياً مع مصمم الكروت", fontSize = 11.sp, color = TextSecondaryLight)
                        }
                    }

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { templateMenuExpanded = true },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("hs_template_picker_btn")
                        ) {
                            Icon(Icons.Default.DesignServices, contentDescription = null, tint = Color(0xFF0F766E), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = selectedTemplate?.let { "${it.templateName} (${it.categoryChip})" }
                                    ?: "قالب افتراضي (3×17 • 10 أرقام)",
                                modifier = Modifier.weight(1f),
                                color = PrimaryNavy,
                                fontWeight = FontWeight.SemiBold
                            )
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = PrimaryNavy)
                        }

                        DropdownMenu(
                            expanded = templateMenuExpanded,
                            onDismissRequest = { templateMenuExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.85f)
                        ) {
                            DropdownMenuItem(
                                text = { Text("استخدام إعدادات مخصصة (بدون قالب)", fontSize = 13.sp) },
                                onClick = {
                                    selectedTemplate = null
                                    templateMenuExpanded = false
                                }
                            )
                            cardTemplates.forEach { tpl ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text("${tpl.templateName} (${tpl.categoryChip})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text(
                                                "أرقام: ${tpl.usernameDigits} | ${tpl.columnsPerPage}×${tpl.rowsPerPage} | باقة HS: ${tpl.linkedHotspotProfile.ifBlank { "-" }}",
                                                fontSize = 11.sp,
                                                color = TextSecondaryLight
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedTemplate = tpl
                                        lengthText = tpl.usernameDigits.toString()
                                        countText = tpl.cardsCount.toString()
                                        if (tpl.linkedHotspotProfile.isNotBlank()) {
                                            selectedProfile = tpl.linkedHotspotProfile
                                        }
                                        templateMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    selectedTemplate?.let { tpl ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AssistChip(
                                onClick = {},
                                label = { Text("طول الكود: ${tpl.usernameDigits} رقم", fontSize = 11.sp) }
                            )
                            AssistChip(
                                onClick = {},
                                label = { Text("التخطيط: ${tpl.columnsPerPage}×${tpl.rowsPerPage}", fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }
        }

        // Hotspot Batch Form
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("مولد كروت الهوتسبوت السريع", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PrimaryNavy)
                    Text("إنشاء وإرسال دفعة كروت هوتسبوت مباشرة إلى الراوتر [${router.name}]", fontSize = 11.sp, color = TextSecondaryLight)

                    OutlinedTextField(
                        value = prefix,
                        onValueChange = { prefix = it },
                        label = { Text("بادئة الكرت") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = countText,
                        onValueChange = { countText = it },
                        label = { Text("عدد الكروت") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = lengthText,
                        onValueChange = { lengthText = it },
                        label = { Text("طول أرقام الكرت (متزامن مع القالب)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedProfile,
                            onValueChange = { selectedProfile = it },
                            label = { Text("اسم بروفايل الهوتسبوت") },
                            trailingIcon = {
                                if (profiles.isNotEmpty()) {
                                    IconButton(onClick = { profileMenuExpanded = true }) {
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = "اختر البروفايل")
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )

                        DropdownMenu(
                            expanded = profileMenuExpanded,
                            onDismissRequest = { profileMenuExpanded = false }
                        ) {
                            profiles.forEach { prof ->
                                DropdownMenuItem(
                                    text = { Text("${prof.name} (السرعة: ${prof.rateLimit})") },
                                    onClick = {
                                        selectedProfile = prof.name
                                        profileMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = limitUptime,
                        onValueChange = { limitUptime = it },
                        label = { Text("الحد الزمني (Uptime Limit)") },
                        placeholder = { Text("مثال: 2h أو 1d أو اتركه فارغاً") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = limitBytes,
                        onValueChange = { limitBytes = it },
                        label = { Text("حد البيانات (Data Limit)") },
                        placeholder = { Text("مثال: 500M أو 1G") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Button(
                        onClick = {
                            val count = countText.toIntOrNull() ?: 50
                            val len = lengthText.toIntOrNull() ?: 10
                            val priceTag = selectedTemplate?.categoryChip?.replace("فئة ", "") ?: selectedProfile
                            onGenerate(prefix, count, selectedProfile, len, limitUptime, limitBytes, priceTag)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("hs_generate_batch_btn")
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("توليد كروت الهوتسبوت وإرسالها للراوتر", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Batch PDF Export Card
        if (lastGeneratedBatch.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFF86EFAC))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "تم توليد ${lastGeneratedBatch.size} كرت هوتسبوت بنجاح",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color(0xFF15803D)
                                )
                                Text(
                                    text = "جاهزة للطباعة وفق قالب: ${activeTemplateForPrint.templateName}",
                                    fontSize = 11.sp,
                                    color = TextSecondaryLight
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showA4Preview = true },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("hs_preview_a4_btn")
                            ) {
                                Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("معاينة A4", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    val pdf = CardPdfGenerator.generateA4CardsPdf(
                                        context = context,
                                        template = activeTemplateForPrint,
                                        cardsInput = lastGeneratedBatch
                                    )
                                    if (pdf != null) {
                                        CardPdfGenerator.openPdfOnDevice(context, pdf)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1.2f)
                                    .height(48.dp)
                                    .testTag("hs_export_pdf_btn")
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("طباعة PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showA4Preview) {
        val bgBmp = remember(activeTemplateForPrint.bgImageUri, activeTemplateForPrint.enableBackground) {
            if (activeTemplateForPrint.enableBackground && activeTemplateForPrint.bgImageUri.isNotBlank()) {
                CardPdfGenerator.loadBitmapFromPathOrUri(context, activeTemplateForPrint.bgImageUri)
            } else null
        }
        val logoBmp = remember(activeTemplateForPrint.logoImageUri, activeTemplateForPrint.enableLogo) {
            if (activeTemplateForPrint.enableLogo && activeTemplateForPrint.logoImageUri.isNotBlank()) {
                CardPdfGenerator.loadBitmapFromPathOrUri(context, activeTemplateForPrint.logoImageUri)
            } else null
        }
        A4SheetPreviewDialog(
            template = activeTemplateForPrint,
            bgBitmap = bgBmp,
            logoBitmap = logoBmp,
            cards = lastGeneratedBatch,
            onDismiss = { showA4Preview = false },
            onExportAndOpenPdf = {
                val pdf = CardPdfGenerator.generateA4CardsPdf(
                    context = context,
                    template = activeTemplateForPrint,
                    cardsInput = lastGeneratedBatch
                )
                showA4Preview = false
                if (pdf != null) {
                    CardPdfGenerator.openPdfOnDevice(context, pdf)
                }
            }
        )
    }
}

@Composable
fun AddHotspotUserDialog(
    profiles: List<HotspotProfileEntity>,
    onDismiss: () -> Unit,
    onSave: (name: String, pass: String, profile: String, comment: String, limitUptime: String, limitBytes: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var profile by remember { mutableStateOf(profiles.firstOrNull()?.name ?: "default") }
    var comment by remember { mutableStateOf("") }
    var limitUptime by remember { mutableStateOf("") }
    var limitBytes by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("إضافة مستخدم هوتسبوت جديد", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PrimaryNavy)

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم المستخدم / الكرت *") },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("كلمة المرور *") },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = profile,
                    onValueChange = { profile = it },
                    label = { Text("البروفايل") },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = limitUptime,
                    onValueChange = { limitUptime = it },
                    label = { Text("الحد الزمني (مثال: 1d أو 3h)") },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("إلغاء") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                onSave(name, password.ifBlank { name }, profile, comment, limitUptime, limitBytes)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        Text("حفظ المستخدم")
                    }
                }
            }
        }
    }
}

@Composable
fun HotspotGridCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    gradient: List<Color>,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFFF0FDF4).takeIf { gradient[0] == Color(0xFF16A34A) } ?: Color(0xFFEFF6FF) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 3.dp else 1.dp),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) gradient[0] else BorderLight
        ),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(gradient))
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isSelected) PrimaryNavy else TextPrimaryLight,
                    maxLines = 1
                )
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = if (isSelected) gradient[0] else TextSecondaryLight,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun EditHotspotUserDialog(
    user: HotspotUserEntity,
    profiles: List<HotspotProfileEntity>,
    onDismiss: () -> Unit,
    onSave: (name: String, pass: String, profile: String, comment: String, limitUptime: String, limitBytes: String) -> Unit
) {
    var name by remember { mutableStateOf(user.name) }
    var password by remember { mutableStateOf(user.password) }
    var profile by remember { mutableStateOf(user.profile) }
    var comment by remember { mutableStateOf(user.comment) }
    var limitUptime by remember { mutableStateOf(user.limitUptime) }
    var limitBytes by remember { mutableStateOf(user.limitBytesTotal) }
    var expanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = PrimaryBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تعديل مستخدم الهوتسبوت", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PrimaryNavy)
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم المستخدم / الكرت") },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("كلمة المرور") },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Box {
                    OutlinedTextField(
                        value = profile,
                        onValueChange = { profile = it },
                        label = { Text("البروفايل") },
                        trailingIcon = {
                            IconButton(onClick = { expanded = true }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        profiles.forEach { p ->
                            DropdownMenuItem(
                                text = { Text(p.name) },
                                onClick = {
                                    profile = p.name
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = limitUptime,
                    onValueChange = { limitUptime = it },
                    label = { Text("الحد الزمني (مثال: 1d أو 3h)") },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = limitBytes,
                    onValueChange = { limitBytes = it },
                    label = { Text("حد الترافيك (Bytes)") },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("إلغاء") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                onSave(name, password, profile, comment, limitUptime, limitBytes)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        Text("حفظ التعديلات")
                    }
                }
            }
        }
    }
}

@Composable
fun ChangeHotspotProfileDialog(
    user: HotspotUserEntity,
    profiles: List<HotspotProfileEntity>,
    onDismiss: () -> Unit,
    onSave: (newProfile: String) -> Unit
) {
    var selectedProfile by remember { mutableStateOf(user.profile) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("تغيير باقة / فئة الهوتسبوت", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PrimaryNavy)
                Text("المستخدم: ${user.name} | الباقة الحالية: ${user.profile}", fontSize = 11.sp, color = TextSecondaryLight)

                LazyColumn(modifier = Modifier.heightIn(max = 240.dp)) {
                    items(profiles) { p ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedProfile == p.name) Color(0xFFEFF6FF) else Color(0xFFF8FAFC),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (selectedProfile == p.name) PrimaryBlue else Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { selectedProfile = p.name }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(p.name, fontWeight = FontWeight.Bold, color = PrimaryNavy)
                                    Text("السرعة: ${p.rateLimit} | الجلسة: ${p.sessionTimeout}", fontSize = 10.sp, color = TextSecondaryLight)
                                }
                                if (selectedProfile == p.name) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("إلغاء") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onSave(selectedProfile) },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        Text("تطبيق الباقة")
                    }
                }
            }
        }
    }
}

@Composable
fun RenewHotspotUserDialog(
    user: HotspotUserEntity,
    profiles: List<HotspotProfileEntity>,
    onDismiss: () -> Unit,
    onSave: (newProfile: String, limitUptime: String, limitBytes: String) -> Unit
) {
    var selectedProfile by remember { mutableStateOf(user.profile) }
    var limitUptime by remember { mutableStateOf(user.limitUptime) }
    var limitBytes by remember { mutableStateOf(user.limitBytesTotal) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Autorenew, contentDescription = null, tint = Color(0xFF0284C7))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تجديد كرت الهوتسبوت", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PrimaryNavy)
                }

                Text(
                    text = "سيتم تصفير العدادات وإعادة فتح التصفح للمستخدم [${user.name}] بالباقة والحدود الجديدة:",
                    fontSize = 11.sp,
                    color = TextSecondaryLight
                )

                LazyColumn(modifier = Modifier.heightIn(max = 160.dp)) {
                    items(profiles) { p ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedProfile == p.name) Color(0xFFE0F2FE) else Color(0xFFF8FAFC),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (selectedProfile == p.name) Color(0xFF0284C7) else Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { selectedProfile = p.name }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(p.name, fontWeight = FontWeight.Bold, color = PrimaryNavy)
                                Text("سرعة ${p.rateLimit}", fontSize = 10.sp, color = TextSecondaryLight)
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = limitUptime,
                    onValueChange = { limitUptime = it },
                    label = { Text("الحد الزمني (اختياري مثل: 1d أو 12h)") },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = limitBytes,
                    onValueChange = { limitBytes = it },
                    label = { Text("حد الترافيك (اختياري بالبايت)") },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("إلغاء") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onSave(selectedProfile, limitUptime, limitBytes) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                    ) {
                        Text("تجديد الكرت الآن ✓")
                    }
                }
            }
        }
    }
}


