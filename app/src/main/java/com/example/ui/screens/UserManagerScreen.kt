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
import androidx.compose.material.icons.automirrored.filled.Send
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
import com.example.data.model.CardTemplateEntity
import com.example.data.model.GeneratedCardPrintItem
import com.example.data.model.RouterEntity
import com.example.data.model.UserManagerProfileEntity
import com.example.data.model.UserManagerUserEntity
import com.example.ui.TalalProViewModel
import com.example.ui.components.A4SheetPreviewDialog
import com.example.ui.theme.*
import com.example.ui.utils.CardPdfGenerator

enum class UserManagerTab(val title: String) {
    CARDS("إدارة الكروت والمستخدمين"),
    PROFILES("الباقات والبروفايلات"),
    GENERATOR("توليد كروت بالدفعة")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserManagerScreen(
    viewModel: TalalProViewModel,
    modifier: Modifier = Modifier
) {
    val routers by viewModel.allRouters.collectAsState()
    val selectedRouter by viewModel.selectedUserManagerRouter.collectAsState()
    val users by viewModel.userManagerUsers.collectAsState()
    val profiles by viewModel.userManagerProfiles.collectAsState()
    val isSyncing by viewModel.isSyncingUserManager.collectAsState()
    val cardTemplates by viewModel.cardTemplates.collectAsState()
    val lastGeneratedBatch by viewModel.lastGeneratedUmBatch.collectAsState()

    var currentTab by remember { mutableStateOf(UserManagerTab.CARDS) }
    var searchQuery by remember { mutableStateOf("") }
    var showAddCardDialog by remember { mutableStateOf(false) }
    var selectedCardForPreview by remember { mutableStateOf<UserManagerUserEntity?>(null) }
    var selectedCardForEdit by remember { mutableStateOf<UserManagerUserEntity?>(null) }
    var selectedCardForProfileChange by remember { mutableStateOf<UserManagerUserEntity?>(null) }
    var selectedCardForRenew by remember { mutableStateOf<UserManagerUserEntity?>(null) }
    var selectedCardForDelete by remember { mutableStateOf<UserManagerUserEntity?>(null) }

    LaunchedEffect(routers) {
        if (selectedRouter == null && routers.isNotEmpty()) {
            viewModel.selectUserManagerRouter(routers.first())
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "إدارة اليوزر مانجر (User Manager)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimaryLight
                        )
                        Text(
                            text = "إدارة الكروت، الباقات، وتوليد الكروت مباشرة في المايكروتك",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondaryLight
                        )
                    }
                },
                actions = {
                    selectedRouter?.let { router ->
                        IconButton(onClick = { viewModel.syncUserManagerData(router) }) {
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
            // Router Selector Bar (Independent per router)
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
                        Icon(Icons.Default.Router, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("الراوتر المحدد:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimaryLight)
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
                                        viewModel.selectUserManagerRouter(r)
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

            // Category Grid Selector (شبكة أقسام احترافية)
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
                        // Section 1: Cards
                        UserManagerGridCard(
                            title = "إدارة الكروت والمستخدمين",
                            subtitle = "${users.size} كرت مسجل",
                            icon = Icons.Default.CreditCard,
                            isSelected = currentTab == UserManagerTab.CARDS,
                            gradient = listOf(Color(0xFF0284C7), Color(0xFF0369A1)),
                            modifier = Modifier.weight(1f),
                            onClick = { currentTab = UserManagerTab.CARDS }
                        )

                        // Section 2: Profiles
                        UserManagerGridCard(
                            title = "الباقات والبروفايلات",
                            subtitle = "${profiles.size} باقة نشطة",
                            icon = Icons.Default.Layers,
                            isSelected = currentTab == UserManagerTab.PROFILES,
                            gradient = listOf(Color(0xFF059669), Color(0xFF047857)),
                            modifier = Modifier.weight(1f),
                            onClick = { currentTab = UserManagerTab.PROFILES }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Section 3: Batch Generator
                        UserManagerGridCard(
                            title = "توليد كروت بالدفعة",
                            subtitle = "طباعة وتوليد فوري",
                            icon = Icons.Default.AutoAwesome,
                            isSelected = currentTab == UserManagerTab.GENERATOR,
                            gradient = listOf(Color(0xFFD97706), Color(0xFFB45309)),
                            modifier = Modifier.weight(1f),
                            onClick = { currentTab = UserManagerTab.GENERATOR }
                        )

                        // Section 4: Card Designer Shortcut
                        UserManagerGridCard(
                            title = "تصاميم وقوالب الطباعة",
                            subtitle = "معاينة وتصدير PDF",
                            icon = Icons.Default.Style,
                            isSelected = false,
                            gradient = listOf(Color(0xFF7C3AED), Color(0xFF6D28D9)),
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.setTab(com.example.ui.ScreenTab.CARD_DESIGNER) }
                        )
                    }
                }
            }

            // Tab Content
            when (currentTab) {
                UserManagerTab.CARDS -> {
                    UserManagerCardsContent(
                        users = users,
                        searchQuery = searchQuery,
                        onSearchChange = { searchQuery = it },
                        onAddCardClick = { showAddCardDialog = true },
                        onEditCard = { selectedCardForEdit = it },
                        onChangeProfile = { selectedCardForProfileChange = it },
                        onRenewCard = { selectedCardForRenew = it },
                        onToggleCard = { card, en ->
                            selectedRouter?.let { r -> viewModel.toggleUserManagerCard(r, card, en) }
                        },
                        onPreviewCard = { selectedCardForPreview = it },
                        onDeleteCard = { selectedCardForDelete = it }
                    )
                }
                UserManagerTab.PROFILES -> {
                    UserManagerProfilesContent(profiles = profiles)
                }
                UserManagerTab.GENERATOR -> {
                    selectedRouter?.let { r ->
                        UserManagerGeneratorContent(
                            router = r,
                            profiles = profiles,
                            cardTemplates = cardTemplates,
                            lastGeneratedBatch = lastGeneratedBatch,
                            onGenerate = { prefix, count, profile, len, price ->
                                viewModel.generateBatchUserManagerCards(r, prefix, count, profile, len, price)
                            }
                        )
                    } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("يرجى اختيار راوتر أولاً لتوليد الكروت", color = TextSecondaryLight)
                    }
                }
            }
        }
    }

    if (showAddCardDialog && selectedRouter != null) {
        AddUserManagerCardDialog(
            profiles = profiles,
            onDismiss = { showAddCardDialog = false },
            onSave = { u, p, prof, c ->
                viewModel.addUserManagerCard(selectedRouter!!, u, p, prof, c)
                showAddCardDialog = false
            }
        )
    }

    selectedCardForPreview?.let { card ->
        UserManagerCardVoucherDialog(
            card = card,
            routerName = selectedRouter?.name ?: "MikroTik Router",
            onDismiss = { selectedCardForPreview = null }
        )
    }

    selectedCardForEdit?.let { card ->
        EditUserManagerCardDialog(
            card = card,
            profiles = profiles,
            onDismiss = { selectedCardForEdit = null },
            onSave = { u, p, prof, c ->
                selectedRouter?.let { r ->
                    viewModel.editUserManagerCard(r, card, u, p, prof, c)
                }
                selectedCardForEdit = null
            }
        )
    }

    selectedCardForProfileChange?.let { card ->
        ChangeUserManagerProfileDialog(
            card = card,
            profiles = profiles,
            onDismiss = { selectedCardForProfileChange = null },
            onSave = { newProf ->
                selectedRouter?.let { r ->
                    viewModel.changeUserManagerProfile(r, card, newProf)
                }
                selectedCardForProfileChange = null
            }
        )
    }

    selectedCardForRenew?.let { card ->
        RenewUserManagerCardDialog(
            card = card,
            profiles = profiles,
            onDismiss = { selectedCardForRenew = null },
            onSave = { newProf ->
                selectedRouter?.let { r ->
                    viewModel.renewUserManagerCard(r, card, newProf)
                }
                selectedCardForRenew = null
            }
        )
    }

    selectedCardForDelete?.let { card ->
        AlertDialog(
            onDismissRequest = { selectedCardForDelete = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(32.dp)) },
            title = { Text("حذف كرت اليوزرمانجر", fontWeight = FontWeight.Bold) },
            text = { Text("هل أنت متأكد من رغبتك في حذف الكرت [${card.username}] نهائياً من راوتر المايكروتك؟") },
            confirmButton = {
                Button(
                    onClick = {
                        selectedRouter?.let { r -> viewModel.deleteUserManagerCard(r, card) }
                        selectedCardForDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("حذف الآن", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { selectedCardForDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
private fun UserManagerCardsContent(
    users: List<UserManagerUserEntity>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onAddCardClick: () -> Unit,
    onEditCard: (UserManagerUserEntity) -> Unit,
    onChangeProfile: (UserManagerUserEntity) -> Unit,
    onRenewCard: (UserManagerUserEntity) -> Unit,
    onToggleCard: (UserManagerUserEntity, Boolean) -> Unit,
    onPreviewCard: (UserManagerUserEntity) -> Unit,
    onDeleteCard: (UserManagerUserEntity) -> Unit
) {
    val filtered = remember(users, searchQuery) {
        if (searchQuery.isBlank()) users
        else users.filter {
            it.username.contains(searchQuery, ignoreCase = true) ||
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
                    placeholder = { Text("بحث عن كرت أو مستخدم...", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMutedLight) },
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Button(
                    onClick = onAddCardClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("إضافة كرت", fontSize = 12.sp)
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(30.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.CreditCardOff, contentDescription = null, tint = TextMutedLight, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("لا توجد كروت يوزرمانجر مسجلة", fontWeight = FontWeight.Bold, color = TextPrimaryLight)
                        Text("اضغط على 'إضافة كرت' أو استخدم 'توليد كروت بالدفعة' للبدء", fontSize = 11.sp, color = TextSecondaryLight)
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
                                    color = if (user.isDisabled) Color(0xFFFEE2E2) else PrimaryBlue.copy(alpha = 0.12f),
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.CreditCard,
                                            contentDescription = null,
                                            tint = if (user.isDisabled) Color(0xFFDC2626) else PrimaryBlue,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = user.username,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = PrimaryNavy
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(0xFFEFF6FF)
                                        ) {
                                            Text(
                                                text = user.profile,
                                                color = PrimaryBlue,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (user.isDisabled) Color(0xFFFEE2E2) else Color(0xFFDCFCE7)
                                        ) {
                                            Text(
                                                text = if (user.isDisabled) "معطل" else "مفعل",
                                                color = if (user.isDisabled) Color(0xFFDC2626) else Color(0xFF16A34A),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "كلمة المرور: ${user.password} | استهلاك: ${user.downloadBytes}",
                                        fontSize = 11.sp,
                                        color = TextSecondaryLight
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = Color(0xFFF1F5F9))

                        // Full suite of card actions: تعديل | تغيير الفئة | تفعيل/تعطيل | تجديد الكرت | طباعة | حذف
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 1. Edit
                            OutlinedButton(
                                onClick = { onEditCard(user) },
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
                                onClick = { onRenewCard(user) },
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
                                onClick = { onToggleCard(user, user.isDisabled) },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = if (user.isDisabled) Icons.Default.PlayArrow else Icons.Default.Pause,
                                    contentDescription = if (user.isDisabled) "تفعيل" else "تعطيل",
                                    tint = if (user.isDisabled) Color(0xFF16A34A) else Color(0xFFEA580C),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // 5. Preview & Print
                            IconButton(
                                onClick = { onPreviewCard(user) },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(Icons.Default.QrCode, contentDescription = "طباعة", tint = PrimaryNavy, modifier = Modifier.size(18.dp))
                            }

                            // 6. Delete
                            IconButton(
                                onClick = { onDeleteCard(user) },
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
private fun UserManagerProfilesContent(profiles: List<UserManagerProfileEntity>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("باقات وبروفايلات اليوزرمانجر", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimaryLight)
            Text("قائمة بالسرعات والصلاحيات والأسعار المعرفة في الراوتر", fontSize = 11.sp, color = TextSecondaryLight)
        }

        if (profiles.isEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(12.dp)) {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("لم يتم العثور على بروفايلات محملة من الراوتر", color = TextSecondaryLight)
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
                            Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFDCFCE7)) {
                                Text("السعر: ${prof.price} ريال", color = Color(0xFF047857), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "صلاحية الباقة: ${prof.validity} | السرعة: ${prof.rateLimitRx} / ${prof.rateLimitTx}", fontSize = 12.sp, color = TextSecondaryLight)
                    }
                }
            }
        }
    }
}

@Composable
private fun UserManagerGeneratorContent(
    router: RouterEntity,
    profiles: List<UserManagerProfileEntity>,
    cardTemplates: List<CardTemplateEntity>,
    lastGeneratedBatch: List<GeneratedCardPrintItem>,
    onGenerate: (prefix: String, count: Int, profile: String, len: Int, price: String) -> Unit
) {
    val context = LocalContext.current
    var selectedTemplate by remember(cardTemplates) {
        mutableStateOf(
            cardTemplates.firstOrNull { it.linkedUmProfile.isNotBlank() }
                ?: cardTemplates.firstOrNull()
        )
    }
    var templateMenuExpanded by remember { mutableStateOf(false) }
    var profileMenuExpanded by remember { mutableStateOf(false) }
    var showA4Preview by remember { mutableStateOf(false) }

    var prefix by remember { mutableStateOf("7") }
    var countText by remember { mutableStateOf((selectedTemplate?.cardsCount ?: 51).toString()) }
    var lengthText by remember { mutableStateOf((selectedTemplate?.usernameDigits ?: 10).toString()) }
    var selectedProfile by remember(profiles) {
        mutableStateOf(
            selectedTemplate?.linkedUmProfile?.takeIf { it.isNotBlank() }
                ?: profiles.firstOrNull()?.name
                ?: "default"
        )
    }
    var priceText by remember {
        mutableStateOf(
            selectedTemplate?.categoryChip?.filter { it.isDigit() }?.ifBlank { "500" } ?: "500"
        )
    }

    val activeTemplateForPrint = remember(selectedTemplate, lengthText, selectedProfile, countText, priceText) {
        (selectedTemplate ?: CardTemplateEntity()).copy(
            usernameDigits = lengthText.toIntOrNull()?.coerceIn(4, 18) ?: 10,
            linkedUmProfile = selectedProfile,
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
                                .testTag("um_template_picker_btn")
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
                                                "أرقام: ${tpl.usernameDigits} | ${tpl.columnsPerPage}×${tpl.rowsPerPage} | باقة UM: ${tpl.linkedUmProfile.ifBlank { "-" }}",
                                                fontSize = 11.sp,
                                                color = TextSecondaryLight
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedTemplate = tpl
                                        lengthText = tpl.usernameDigits.toString()
                                        countText = tpl.cardsCount.toString()
                                        if (tpl.linkedUmProfile.isNotBlank()) {
                                            selectedProfile = tpl.linkedUmProfile
                                        }
                                        val digitsPrice = tpl.categoryChip.filter { it.isDigit() }
                                        if (digitsPrice.isNotBlank()) {
                                            priceText = digitsPrice
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

        // Batch Generation Form
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("مولد كروت اليوزرمانجر الآلي", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PrimaryNavy)
                    Text("توليد مجموعة كروت بنقرة واحدة وإرسالها فوراً إلى الراوتر [${router.name}]", fontSize = 11.sp, color = TextSecondaryLight)

                    OutlinedTextField(
                        value = prefix,
                        onValueChange = { prefix = it },
                        label = { Text("بادئة الكرت (Prefix)") },
                        placeholder = { Text("مثال: 7 أو 8 أو 9") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = countText,
                        onValueChange = { countText = it },
                        label = { Text("عدد الكروت المراد توليدها") },
                        placeholder = { Text("مثال: 50 أو 100") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = lengthText,
                        onValueChange = { lengthText = it },
                        label = { Text("طول أرقام الكرت (متزامن مع القالب)") },
                        placeholder = { Text("مثال: 10 أرقام") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedProfile,
                            onValueChange = { selectedProfile = it },
                            label = { Text("اسم الباقة / البروفايل") },
                            placeholder = { Text("default") },
                            trailingIcon = {
                                if (profiles.isNotEmpty()) {
                                    IconButton(onClick = { profileMenuExpanded = true }) {
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = "اختر الباقة")
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
                                    text = { Text("${prof.name} (${prof.price} ريال - ${prof.validity})") },
                                    onClick = {
                                        selectedProfile = prof.name
                                        if (prof.price.isNotBlank() && prof.price != "0") {
                                            priceText = prof.price
                                        }
                                        profileMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it },
                        label = { Text("سعر بيع الكرت (ريال)") },
                        placeholder = { Text("500") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Button(
                        onClick = {
                            val count = countText.toIntOrNull() ?: 50
                            val len = lengthText.toIntOrNull() ?: 10
                            onGenerate(prefix, count, selectedProfile, len, priceText)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("um_generate_batch_btn")
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("توليد الكروت وإرسالها للراوتر الآن", fontWeight = FontWeight.Bold)
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
                                    text = "تم توليد ${lastGeneratedBatch.size} كرت بنجاح",
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
                                    .testTag("um_preview_a4_btn")
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
                                    .testTag("um_export_pdf_btn")
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
fun AddUserManagerCardDialog(
    profiles: List<UserManagerProfileEntity>,
    onDismiss: () -> Unit,
    onSave: (username: String, pass: String, profile: String, comment: String) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var profile by remember { mutableStateOf(profiles.firstOrNull()?.name ?: "default") }
    var comment by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("إضافة كرت يوزرمانجر جديد", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PrimaryNavy)

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("اسم المستخدم / رقم الكرت *") },
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
                    label = { Text("البروفايل / الباقة") },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("ملاحظات") },
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
                            if (username.isNotBlank() && password.isNotBlank()) {
                                onSave(username, password, profile, comment)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        Text("حفظ الكرت")
                    }
                }
            }
        }
    }
}

@Composable
fun UserManagerCardVoucherDialog(
    card: UserManagerUserEntity,
    routerName: String,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF1F5F9),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "شبكة $routerName", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = PrimaryNavy)
                        Text(text = "بطاقة اشتراك إنترنت", fontSize = 11.sp, color = TextSecondaryLight)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("اسم المستخدم:", fontSize = 12.sp, color = TextSecondaryLight)
                            Text(card.username, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PrimaryNavy)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("كلمة المرور:", fontSize = 12.sp, color = TextSecondaryLight)
                            Text(card.password, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("الباقة:", fontSize = 12.sp, color = TextSecondaryLight)
                            Text(card.profile, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimaryLight)
                        }
                    }
                }

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("إغلاق البطاقة")
                }
            }
        }
    }
}

@Composable
fun UserManagerGridCard(
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
            containerColor = if (isSelected) Color(0xFFF0FDF4).takeIf { gradient[0] == Color(0xFF059669) } ?: Color(0xFFEFF6FF) else Color.White
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
fun EditUserManagerCardDialog(
    card: UserManagerUserEntity,
    profiles: List<UserManagerProfileEntity>,
    onDismiss: () -> Unit,
    onSave: (username: String, password: String, profile: String, comment: String) -> Unit
) {
    var username by remember { mutableStateOf(card.username) }
    var password by remember { mutableStateOf(card.password) }
    var profile by remember { mutableStateOf(card.profile) }
    var comment by remember { mutableStateOf(card.comment) }
    var profileExpanded by remember { mutableStateOf(false) }

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
                    Text("تعديل كرت اليوزرمانجر", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PrimaryNavy)
                }

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("اسم المستخدم") },
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
                        label = { Text("البروفايل / الباقة") },
                        trailingIcon = {
                            IconButton(onClick = { profileExpanded = true }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    DropdownMenu(
                        expanded = profileExpanded,
                        onDismissRequest = { profileExpanded = false }
                    ) {
                        profiles.forEach { p ->
                            DropdownMenuItem(
                                text = { Text(p.name) },
                                onClick = {
                                    profile = p.name
                                    profileExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("ملاحظات") },
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
                            if (username.isNotBlank() && password.isNotBlank()) {
                                onSave(username, password, profile, comment)
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
fun ChangeUserManagerProfileDialog(
    card: UserManagerUserEntity,
    profiles: List<UserManagerProfileEntity>,
    onDismiss: () -> Unit,
    onSave: (newProfile: String) -> Unit
) {
    var selectedProfile by remember { mutableStateOf(card.profile) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("تغيير فئة / باقة الكرت", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PrimaryNavy)
                Text("الكرت: ${card.username} | الباقة الحالية: ${card.profile}", fontSize = 11.sp, color = TextSecondaryLight)

                LazyColumn(modifier = Modifier.heightIn(max = 240.dp)) {
                    if (profiles.isEmpty()) {
                        item {
                            Text("لا توجد بروفايلات متاحة. أدخل اسم الباقة يدوياً.", color = TextSecondaryLight, fontSize = 12.sp)
                        }
                    } else {
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
                                        Text("السعر: ${p.price} | الصلاحية: ${p.validity}", fontSize = 10.sp, color = TextSecondaryLight)
                                    }
                                    if (selectedProfile == p.name) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                                    }
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
                        Text("تطبيق الفئة الجديدة")
                    }
                }
            }
        }
    }
}

@Composable
fun RenewUserManagerCardDialog(
    card: UserManagerUserEntity,
    profiles: List<UserManagerProfileEntity>,
    onDismiss: () -> Unit,
    onSave: (newProfile: String) -> Unit
) {
    var selectedProfile by remember { mutableStateOf(card.profile) }

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
                    Text("تجديد كرت اليوزرمانجر", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PrimaryNavy)
                }

                Text(
                    text = "سيتم تصفير عدادات الاستهلاك وإعادة تفعيل الكرت [${card.username}] وربطه بالباقة المختارة مباشرة في الراوتر.",
                    fontSize = 11.sp,
                    color = TextSecondaryLight
                )

                LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
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
                                Text("صلاحية ${p.validity}", fontSize = 10.sp, color = TextSecondaryLight)
                            }
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("إلغاء") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onSave(selectedProfile) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                    ) {
                        Text("تجديد الكرت الآن ✓")
                    }
                }
            }
        }
    }
}


