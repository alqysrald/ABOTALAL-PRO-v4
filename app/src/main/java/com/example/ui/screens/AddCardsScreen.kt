package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.ScreenTab
import com.example.ui.TalalProViewModel
import com.example.ui.components.A4SheetPreviewDialog
import com.example.ui.components.LiveCardPreviewBox
import com.example.ui.theme.PrimaryNavy
import com.example.ui.theme.TextSecondaryLight
import com.example.ui.utils.CardPdfGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ceil

@Composable
fun AddCardsScreen(
    viewModel: TalalProViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val routers by viewModel.allRouters.collectAsState()
    val selectedAddCardsRouter by viewModel.selectedAddCardsRouter.collectAsState()
    val umProfiles by viewModel.userManagerProfiles.collectAsState()
    val hsProfiles by viewModel.hotspotProfiles.collectAsState()
    val cardTemplates by viewModel.cardTemplates.collectAsState()
    val cardBatches by viewModel.cardBatches.collectAsState()
    val isSyncingAll by viewModel.isSyncingAllPackages.collectAsState()
    val isProcessingBatch by viewModel.isProcessingBatch.collectAsState()
    val selectedBatchItems by viewModel.selectedBatchItems.collectAsState()
    val pendingBatchChoice by viewModel.pendingBatchForPushChoice.collectAsState()

    val activeRouter = selectedAddCardsRouter ?: routers.firstOrNull()

    // Ensure router flows remain synced when entering screen
    LaunchedEffect(routers, selectedAddCardsRouter) {
        if (selectedAddCardsRouter == null && routers.isNotEmpty()) {
            val first = routers.first()
            viewModel.selectedAddCardsRouter.value = first
            viewModel.selectedUserManagerRouter.value = first
            viewModel.selectedHotspotRouter.value = first
        } else if (selectedAddCardsRouter != null) {
            if (viewModel.selectedUserManagerRouter.value?.id != selectedAddCardsRouter?.id) {
                viewModel.selectedUserManagerRouter.value = selectedAddCardsRouter
            }
            if (viewModel.selectedHotspotRouter.value?.id != selectedAddCardsRouter?.id) {
                viewModel.selectedHotspotRouter.value = selectedAddCardsRouter
            }
        }
    }

    // Auto-sync profiles once if a router exists and local profile cache is empty
    var autoSyncedRouterId by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(activeRouter?.id) {
        if (activeRouter != null &&
            activeRouter.id != autoSyncedRouterId &&
            umProfiles.isEmpty() &&
            hsProfiles.isEmpty()
        ) {
            autoSyncedRouterId = activeRouter.id
            viewModel.syncAllPackagesForAddCards(activeRouter)
        }
    }

    // Available saved templates (fallback to Default_Design if user hasn't saved one yet)
    val availableTemplates = remember(cardTemplates) {
        if (cardTemplates.isNotEmpty()) cardTemplates else listOf(CardTemplateEntity(templateName = "Default_Design"))
    }
    var selectedTemplateId by remember { mutableStateOf<Long?>(null) }
    val activeTemplate = remember(availableTemplates, selectedTemplateId) {
        availableTemplates.firstOrNull { it.id == selectedTemplateId } ?: availableTemplates.first()
    }

    // Top Mode Tabs: 0 = إنشاء دفعة, 1 = استيراد دفعة
    var modeTab by remember { mutableIntStateOf(0) }

    // Package Filter & Selection
    var serverFilter by remember { mutableStateOf("ALL") } // "ALL", "USER_MANAGER", "HOTSPOT"
    val allPackages = remember(umProfiles, hsProfiles) {
        buildUnifiedPackageOptions(umProfiles, hsProfiles)
    }
    val filteredPackages = remember(allPackages, serverFilter) {
        when (serverFilter) {
            "USER_MANAGER" -> allPackages.filter { it.serverType == "USER_MANAGER" }
            "HOTSPOT" -> allPackages.filter { it.serverType == "HOTSPOT" }
            else -> allPackages
        }
    }

    var selectedPkgId by remember { mutableStateOf<String?>(null) }
    var serverType by remember { mutableStateOf("HOTSPOT") }
    var selectedProfileName by remember { mutableStateOf("default") }
    var priceText by remember { mutableStateOf("100 RY") }
    var customProfileOverride by remember { mutableStateOf("") }

    LaunchedEffect(filteredPackages) {
        val currentMatch = filteredPackages.firstOrNull { it.id == selectedPkgId }
        if (currentMatch == null && filteredPackages.isNotEmpty()) {
            val first = filteredPackages.first()
            selectedPkgId = first.id
            selectedProfileName = first.name
            serverType = first.serverType
            priceText = first.priceLabel
            customProfileOverride = ""
        }
    }

    // Batch Generation Options
    var charType by remember { mutableStateOf("DIGITS") } // DIGITS, LETTERS, MIXED
    var codeLengthInput by remember { mutableStateOf("9") }
    var prefixInput by remember { mutableStateOf("") }
    var batchCountInput by remember { mutableStateOf("51") }
    var excludeSimilar by remember { mutableStateOf(false) }

    // Sync template defaults when user explicitly picks a new template
    LaunchedEffect(activeTemplate.id, activeTemplate.cardsCount, activeTemplate.usernameDigits) {
        val tmplCount = (activeTemplate.columnsPerPage * activeTemplate.rowsPerPage).coerceIn(1, 500)
        batchCountInput = tmplCount.toString()
        codeLengthInput = activeTemplate.usernameDigits.coerceIn(4, 18).toString()
    }

    val safeCodeLength = (codeLengthInput.toIntOrNull() ?: 9).coerceIn(4, 18)
    val safeBatchCount = (batchCountInput.toIntOrNull() ?: 51).coerceIn(1, 500)
    val effectiveProfileName = customProfileOverride.ifBlank { selectedProfileName }

    // Load Template Bitmaps for Live Preview & PDF
    val bgBitmap = remember(activeTemplate.bgImageUri, activeTemplate.enableBackground) {
        if (activeTemplate.enableBackground && activeTemplate.bgImageUri.isNotBlank()) {
            CardPdfGenerator.loadBitmapFromPathOrUri(context, activeTemplate.bgImageUri)
        } else null
    }
    val logoBitmap = remember(activeTemplate.logoImageUri, activeTemplate.enableLogo) {
        if (activeTemplate.enableLogo && activeTemplate.logoImageUri.isNotBlank()) {
            CardPdfGenerator.loadBitmapFromPathOrUri(context, activeTemplate.logoImageUri)
        } else null
    }

    val sampleUsername = remember(charType, safeCodeLength, prefixInput, excludeSimilar) {
        viewModel.generateBatchPreviewSampleCode(charType, safeCodeLength, prefixInput, excludeSimilar)
    }
    val previewSampleCard = remember(sampleUsername, effectiveProfileName, priceText, activeRouter?.name) {
        GeneratedCardPrintItem(
            username = sampleUsername,
            password = sampleUsername,
            profileName = effectiveProfileName,
            price = priceText,
            serial = "BATCH-0001",
            paymentNumber = "PAY-1001",
            posName = activeRouter?.name ?: "TALAL PRO"
        )
    }

    // Dialog States
    var showA4Preview by remember { mutableStateOf(false) }
    var a4PreviewTemplate by remember { mutableStateOf(activeTemplate) }
    var a4PreviewCards by remember { mutableStateOf<List<GeneratedCardPrintItem>>(emptyList()) }
    var viewingBatch by remember { mutableStateOf<CardBatchEntity?>(null) }
    var editingBatch by remember { mutableStateOf<CardBatchEntity?>(null) }
    var deletingBatch by remember { mutableStateOf<CardBatchEntity?>(null) }

    // Dropdown Expanded States
    var routerDropdownExpanded by remember { mutableStateOf(false) }
    var pkgDropdownExpanded by remember { mutableStateOf(false) }
    var charDropdownExpanded by remember { mutableStateOf(false) }
    var templateDropdownExpanded by remember { mutableStateOf(false) }
    var batchHistorySearch by remember { mutableStateOf("") }

    fun exportAndOpenPdf(tmpl: CardTemplateEntity, cards: List<GeneratedCardPrintItem>) {
        if (cards.isEmpty()) {
            Toast.makeText(context, "لا توجد كروت للطباعة", Toast.LENGTH_SHORT).show()
            return
        }
        scope.launch {
            Toast.makeText(context, "جاري تجهيز ملف PDF بالبطاقات...", Toast.LENGTH_SHORT).show()
            val file = withContext(Dispatchers.IO) {
                CardPdfGenerator.generateA4CardsPdf(context, tmpl, cards)
            }
            if (file != null) {
                CardPdfGenerator.openPdfOnDevice(context, file)
            } else {
                Toast.makeText(context, "حدث خطأ أثناء إنشاء ملف PDF", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun resolveTemplateForBatch(batch: CardBatchEntity): CardTemplateEntity {
        return availableTemplates.firstOrNull { it.id == batch.templateId }
            ?: availableTemplates.firstOrNull { it.templateName == batch.templateName }
            ?: activeTemplate
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        // Compact Router Selector Strip at Top
        Surface(
            color = Color.White,
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(if (activeRouter?.isOnline == true) AddCardsEmerald else Color(0xFFF59E0B))
                        )
                        Text(
                            text = "الراوتر:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondaryLight
                        )
                        Box {
                            Surface(
                                onClick = { routerDropdownExpanded = true },
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFF1F5F9),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("add_cards_router_selector")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = activeRouter?.name ?: "اختر راوتر",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryNavy,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "اختيار الراوتر", modifier = Modifier.size(18.dp))
                                }
                            }
                            DropdownMenu(
                                expanded = routerDropdownExpanded,
                                onDismissRequest = { routerDropdownExpanded = false }
                            ) {
                                if (routers.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("+ إضافة راوتر جديد") },
                                        onClick = {
                                            routerDropdownExpanded = false
                                            viewModel.showAddRouterDialog.value = true
                                        }
                                    )
                                } else {
                                    routers.forEach { r ->
                                        DropdownMenuItem(
                                            text = { Text("${r.name} (${r.host})") },
                                            onClick = {
                                                viewModel.selectedAddCardsRouter.value = r
                                                viewModel.selectedUserManagerRouter.value = r
                                                viewModel.selectedHotspotRouter.value = r
                                                routerDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Text(
                        text = "إضافة كروت",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = AddCardsMaroon
                    )
                }

                // Top Mode Tabs (إنشاء دفعة | استيراد دفعة) matching screenshot
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    ModeTabButton(
                        title = "إنشاء دفعة",
                        icon = Icons.Default.CreditCard,
                        isSelected = modeTab == 0,
                        tag = "tab_create_batch",
                        onClick = { modeTab = 0 },
                        modifier = Modifier.weight(1f)
                    )
                    ModeTabButton(
                        title = "استيراد دفعة",
                        icon = Icons.Default.NoteAdd,
                        isSelected = modeTab == 1,
                        tag = "tab_import_batch",
                        onClick = { modeTab = 1 },
                        modifier = Modifier.weight(1f)
                    )
                }
                HorizontalDivider(color = Color(0xFFE2E8F0))
            }
        }

        // Scrollable Body
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (modeTab == 1) {
                ImportBatchTabContent(
                    activeRouter = activeRouter,
                    packages = allPackages,
                    templates = availableTemplates,
                    selectedTemplate = activeTemplate,
                    isProcessing = isProcessingBatch,
                    isSyncing = isSyncingAll,
                    onSyncAll = { viewModel.syncAllPackagesForAddCards(activeRouter) },
                    onSelectTemplate = { tmpl -> selectedTemplateId = tmpl.id },
                    onImportAndSave = { title, srv, prof, price, pairs, push ->
                        viewModel.importCardBatch(
                            router = activeRouter,
                            batchTitle = title,
                            serverType = srv,
                            profileName = prof,
                            priceLabel = price,
                            importedPairs = pairs,
                            pushToRouter = push,
                            template = activeTemplate
                        )
                    },
                    onImportAndPrint = { title, srv, prof, price, pairs, push ->
                        viewModel.importCardBatch(
                            router = activeRouter,
                            batchTitle = title,
                            serverType = srv,
                            profileName = prof,
                            priceLabel = price,
                            importedPairs = pairs,
                            pushToRouter = push,
                            template = activeTemplate,
                            onImportedForPrint = { _, printItems ->
                                exportAndOpenPdf(activeTemplate, printItems)
                            }
                        )
                    }
                )
            } else {
                // ================= CARD 1: خيارات الباقة والسيرفر =================
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Header + Sync All Packages button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FormatListBulleted,
                                    contentDescription = "أيقونة خيارات الباقة",
                                    tint = AddCardsSkyText
                                )
                                Text(
                                    text = "خيارات الباقة والسيرفر",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AddCardsMaroon
                                )
                            }

                            OutlinedButton(
                                onClick = { viewModel.syncAllPackagesForAddCards(activeRouter) },
                                enabled = !isSyncingAll,
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.dp, AddCardsMaroonBorder),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = AddCardsMaroonSoft,
                                    contentColor = AddCardsMaroon
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("sync_all_packages_button")
                            ) {
                                if (isSyncingAll) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = AddCardsMaroon)
                                } else {
                                    Icon(Icons.Default.Sync, contentDescription = "مزامنة الباقات", modifier = Modifier.size(15.dp))
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("مزامنة كافة الباقات", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        HorizontalDivider(color = Color(0xFFF1F5F9))

                        // Segmented Pills: الكل (N) | يوزرمانجر | هوتسبوت
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = Color(0xFFF1F5F9),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                ServerFilterPill(
                                    label = "الكل ( ${allPackages.size} )",
                                    selected = serverFilter == "ALL",
                                    tag = "filter_all_packages",
                                    onClick = { serverFilter = "ALL" },
                                    modifier = Modifier.weight(1f)
                                )
                                ServerFilterPill(
                                    label = "يوزرمانجر (${umProfiles.size})",
                                    selected = serverFilter == "USER_MANAGER",
                                    tag = "filter_um_packages",
                                    onClick = { serverFilter = "USER_MANAGER" },
                                    modifier = Modifier.weight(1f)
                                )
                                ServerFilterPill(
                                    label = "هوتسبوت (${hsProfiles.size})",
                                    selected = serverFilter == "HOTSPOT",
                                    tag = "filter_hs_packages",
                                    onClick = { serverFilter = "HOTSPOT" },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        // Dropdown: الباقة المختارة
                        Text(
                            text = "الباقة المختارة",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = AddCardsMaroon
                        )

                        Box {
                            OutlinedCard(
                                onClick = { pkgDropdownExpanded = true },
                                shape = RoundedCornerShape(24.dp),
                                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 50.dp)
                                    .testTag("selected_package_dropdown")
                            ) {
                                val selectedOption = filteredPackages.firstOrNull { it.id == selectedPkgId }
                                val displayTxt = when {
                                    customProfileOverride.isNotBlank() ->
                                        "$customProfileOverride [${if (serverType == "USER_MANAGER") "UserManager" else "Hotspot"}] — $priceText"
                                    selectedOption != null -> selectedOption.displayLabel
                                    else -> "لا توجد باقات متزامنة — اضغط مزامنة كافة الباقات"
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = displayTxt,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = PrimaryNavy,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "فتح قائمة الباقات", tint = TextSecondaryLight)
                                }
                            }

                            DropdownMenu(
                                expanded = pkgDropdownExpanded,
                                onDismissRequest = { pkgDropdownExpanded = false }
                            ) {
                                if (filteredPackages.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("مزامنة الباقات من الراوتر الآن...") },
                                        onClick = {
                                            pkgDropdownExpanded = false
                                            viewModel.syncAllPackagesForAddCards(activeRouter)
                                        }
                                    )
                                } else {
                                    filteredPackages.forEach { pkg ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = pkg.displayLabel,
                                                    fontSize = 12.sp,
                                                    fontWeight = if (pkg.id == selectedPkgId) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (pkg.id == selectedPkgId) AddCardsMaroon else PrimaryNavy
                                                )
                                            },
                                            onClick = {
                                                selectedPkgId = pkg.id
                                                selectedProfileName = pkg.name
                                                serverType = pkg.serverType
                                                priceText = pkg.priceLabel
                                                customProfileOverride = ""
                                                pkgDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        if (filteredPackages.isEmpty()) {
                            OutlinedTextField(
                                value = customProfileOverride,
                                onValueChange = { customProfileOverride = it },
                                label = { Text("اسم البروفايل يدوياً (عند عدم الاتصال)", fontSize = 11.sp) },
                                placeholder = { Text("default", fontSize = 12.sp) },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("manual_profile_input")
                            )
                        }

                        // نوع السيرفر + السعر Box
                        Text(
                            text = "نوع السيرفر",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = AddCardsMaroon
                        )

                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            color = Color.White,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 50.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    onClick = {
                                        serverType = if (serverType == "HOTSPOT") "USER_MANAGER" else "HOTSPOT"
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (serverType == "HOTSPOT") Color(0xFFFEF3C7) else Color(0xFFDBEAFE),
                                    modifier = Modifier
                                        .heightIn(min = 36.dp)
                                        .testTag("server_type_badge")
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                                        Text(
                                            text = if (serverType == "HOTSPOT") "Hotspot" else "User Manager",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (serverType == "HOTSPOT") Color(0xFFD97706) else Color(0xFF1D4ED8)
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "السعر:",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AddCardsMaroon
                                    )
                                    BasicTextField(
                                        value = priceText,
                                        onValueChange = { priceText = it },
                                        singleLine = true,
                                        textStyle = TextStyle(
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = AddCardsMaroon,
                                            textAlign = TextAlign.End
                                        ),
                                        modifier = Modifier
                                            .widthIn(min = 60.dp, max = 110.dp)
                                            .testTag("batch_price_input")
                                    )
                                }
                            }
                        }

                        // Row: نوع الأحرف | طول الكرت (عدد الخانات)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("نوع الأحرف", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AddCardsMaroon)
                                Box {
                                    OutlinedCard(
                                        onClick = { charDropdownExpanded = true },
                                        shape = RoundedCornerShape(22.dp),
                                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 50.dp)
                                            .testTag("char_type_dropdown")
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 14.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = when (charType) {
                                                    "LETTERS" -> "أحرف إنجليزية"
                                                    "MIXED" -> "أرقام وأحرف"
                                                    else -> "أرقام فقط"
                                                },
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = PrimaryNavy
                                            )
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = "نوع الأحرف", tint = TextSecondaryLight)
                                        }
                                    }
                                    DropdownMenu(expanded = charDropdownExpanded, onDismissRequest = { charDropdownExpanded = false }) {
                                        DropdownMenuItem(text = { Text("أرقام فقط") }, onClick = { charType = "DIGITS"; charDropdownExpanded = false })
                                        DropdownMenuItem(text = { Text("أحرف إنجليزية") }, onClick = { charType = "LETTERS"; charDropdownExpanded = false })
                                        DropdownMenuItem(text = { Text("أرقام وأحرف") }, onClick = { charType = "MIXED"; charDropdownExpanded = false })
                                    }
                                }
                            }

                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("طول الكرت (عدد الخانات)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AddCardsMaroon)
                                OutlinedTextField(
                                    value = codeLengthInput,
                                    onValueChange = { v -> codeLengthInput = v.filter { it.isDigit() }.take(2) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(22.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("code_length_input")
                                )
                            }
                        }

                        // Row: بادئة الكرت (اختياري) | عدد الكروت في الدفعة
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("بادئة الكرت (اختياري)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AddCardsMaroon)
                                OutlinedTextField(
                                    value = prefixInput,
                                    onValueChange = { prefixInput = it.take(6) },
                                    singleLine = true,
                                    placeholder = { Text("مثال: NET-", fontSize = 11.sp) },
                                    shape = RoundedCornerShape(22.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("card_prefix_input")
                                )
                            }

                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("عدد الكروت في الدفعة", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AddCardsMaroon)
                                OutlinedTextField(
                                    value = batchCountInput,
                                    onValueChange = { v -> batchCountInput = v.filter { it.isDigit() }.take(3) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(22.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("batch_count_input")
                                )
                            }
                        }

                        // Checkbox: استبعاد أحرف/أرقام متشابهة
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            color = Color.White,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .clickable { excludeSimilar = !excludeSimilar }
                                .testTag("exclude_similar_checkbox")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "استبعاد أحرف/أرقام متشابهة (O, 1/I/0)",
                                    fontSize = 12.sp,
                                    color = AddCardsMaroon,
                                    fontWeight = FontWeight.Medium
                                )
                                Checkbox(
                                    checked = excludeSimilar,
                                    onCheckedChange = { excludeSimilar = it }
                                )
                            }
                        }
                    }
                }

                // ================= CARD 2: قالب الطباعة والمعاينة الحية =================
                val cardsPerPage = (activeTemplate.columnsPerPage * activeTemplate.rowsPerPage).coerceAtLeast(1)
                val expectedPages = ceil(safeBatchCount.toDouble() / cardsPerPage.toDouble()).toInt().coerceAtLeast(1)
                val cellWidthMm = 210f / activeTemplate.columnsPerPage.coerceAtLeast(1)
                val cellHeightMm = 297f / activeTemplate.rowsPerPage.coerceAtLeast(1)
                val posXMm = (cellWidthMm * (activeTemplate.userXPercent / 100f)).toInt()
                val posYMm = (cellHeightMm * (activeTemplate.userYPercent / 100f)).toInt()

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
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
                            Text(
                                text = "قالب الطباعة والمعاينة الحية",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = AddCardsMaroon
                            )

                            TextButton(
                                onClick = { viewModel.setTab(ScreenTab.CARD_DESIGNER) },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("manage_designs_btn")
                            ) {
                                Text(
                                    text = "إدارة التصاميم",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AddCardsMaroon
                                )
                            }
                        }

                        HorizontalDivider(color = Color(0xFFF1F5F9))

                        Text(
                            text = "اختر قالب تصميم الكروت",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = AddCardsMaroon
                        )

                        Box {
                            OutlinedCard(
                                onClick = { templateDropdownExpanded = true },
                                shape = RoundedCornerShape(24.dp),
                                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 50.dp)
                                    .testTag("template_picker_dropdown")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${activeTemplate.templateName} (${activeTemplate.rowsPerPage} صفوف × ${activeTemplate.columnsPerPage} أعمدة = $cardsPerPage كرت/صفحة)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = PrimaryNavy,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "قائمة القوالب", tint = TextSecondaryLight)
                                }
                            }

                            DropdownMenu(
                                expanded = templateDropdownExpanded,
                                onDismissRequest = { templateDropdownExpanded = false }
                            ) {
                                availableTemplates.forEach { tmpl ->
                                    val perPage = tmpl.columnsPerPage * tmpl.rowsPerPage
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = "${tmpl.templateName} (${tmpl.rowsPerPage} صفوف × ${tmpl.columnsPerPage} أعمدة = $perPage كرت/صفحة)",
                                                fontSize = 12.sp,
                                                fontWeight = if (tmpl.id == activeTemplate.id) FontWeight.Bold else FontWeight.Normal,
                                                color = if (tmpl.id == activeTemplate.id) AddCardsMaroon else PrimaryNavy
                                            )
                                        },
                                        onClick = {
                                            selectedTemplateId = tmpl.id
                                            templateDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Live Card Preview Container
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 14.dp)
                            ) {
                                LiveCardPreviewBox(
                                    template = activeTemplate,
                                    bgBitmap = bgBitmap,
                                    logoBitmap = logoBitmap,
                                    sampleCard = previewSampleCard,
                                    boxHeight = 110.dp
                                )
                            }
                        }

                        // Coordinate Summary Pill
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = AddCardsMaroonSoft,
                            border = BorderStroke(1.dp, AddCardsMaroonBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "موقع اسم المستخدم:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AddCardsMaroon
                                )
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color.White,
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                ) {
                                    Text(
                                        text = "س: $posXMm مم | ص: $posYMm مم",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryNavy,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        // 2x2 Template Info Grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            TemplateStatTile(
                                title = "اسم القالب",
                                value = activeTemplate.templateName,
                                valueColor = AddCardsMaroon,
                                modifier = Modifier.weight(1f)
                            )
                            TemplateStatTile(
                                title = "الكروت بالصفحة",
                                value = "كرت $cardsPerPage",
                                valueColor = AddCardsMaroon,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            TemplateStatTile(
                                title = "مقاس الورق",
                                value = "A4 (210×297mm)",
                                valueColor = AddCardsMaroon,
                                modifier = Modifier.weight(1f)
                            )
                            TemplateStatTile(
                                title = "الصفحات المتوقعة",
                                value = "صفحة $expectedPages",
                                valueColor = AddCardsSkyText,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Preview PDF Sheet Button
                        OutlinedButton(
                            onClick = {
                                val sampleList = (1..safeBatchCount).map { idx ->
                                    val code = viewModel.generateBatchPreviewSampleCode(
                                        charType = charType,
                                        codeLength = safeCodeLength,
                                        prefix = prefixInput,
                                        excludeSimilar = excludeSimilar
                                    )
                                    val varied = if (idx == 1) code else {
                                        val tail = ((idx * 37) % 90 + 10).toString()
                                        if (code.length > 2) code.dropLast(2) + tail else code
                                    }
                                    GeneratedCardPrintItem(
                                        username = varied,
                                        password = varied,
                                        profileName = effectiveProfileName,
                                        price = priceText,
                                        serial = String.format(Locale.US, "PRV-%04d", idx),
                                        paymentNumber = "PREVIEW",
                                        posName = activeRouter?.name ?: "TALAL PRO"
                                    )
                                }
                                a4PreviewTemplate = activeTemplate
                                a4PreviewCards = sampleList
                                showA4Preview = true
                            },
                            shape = RoundedCornerShape(22.dp),
                            border = BorderStroke(1.dp, Color(0xFF7DD3FC)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = AddCardsSkyBg.copy(alpha = 0.7f),
                                contentColor = AddCardsSkyText
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("preview_template_pdf_btn")
                        ) {
                            Text(
                                text = "معاينة قالب الطباعة (PDF)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // ================= PRIMARY ACTION BUTTONS =================
                Button(
                    onClick = {
                        viewModel.createCardBatch(
                            router = activeRouter,
                            serverType = serverType,
                            profileName = effectiveProfileName,
                            priceLabel = priceText,
                            charType = charType,
                            codeLength = safeCodeLength,
                            count = safeBatchCount,
                            prefix = prefixInput,
                            excludeSimilar = excludeSimilar,
                            template = activeTemplate
                        )
                    },
                    enabled = !isProcessingBatch,
                    colors = ButtonDefaults.buttonColors(containerColor = AddCardsMaroon),
                    shape = RoundedCornerShape(26.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .testTag("create_batch_only_btn")
                ) {
                    if (isProcessingBatch) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = "+ إنشاء الدفعة فقط",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Button(
                    onClick = {
                        viewModel.createCardBatch(
                            router = activeRouter,
                            serverType = serverType,
                            profileName = effectiveProfileName,
                            priceLabel = priceText,
                            charType = charType,
                            codeLength = safeCodeLength,
                            count = safeBatchCount,
                            prefix = prefixInput,
                            excludeSimilar = excludeSimilar,
                            template = activeTemplate,
                            onBatchReadyForPrint = { _, printItems ->
                                exportAndOpenPdf(activeTemplate, printItems)
                            }
                        )
                    },
                    enabled = !isProcessingBatch,
                    colors = ButtonDefaults.buttonColors(containerColor = AddCardsEmerald),
                    shape = RoundedCornerShape(26.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .testTag("create_and_print_batch_btn")
                ) {
                    Text(
                        text = "إنشاء وطباعة الدفعة فوراً",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // ================= CARD 3: سجل أحدث الدفعات =================
                val visibleBatches = remember(cardBatches, batchHistorySearch) {
                    if (batchHistorySearch.isBlank()) cardBatches
                    else cardBatches.filter {
                        it.batchCode.contains(batchHistorySearch, ignoreCase = true) ||
                            it.profileName.contains(batchHistorySearch, ignoreCase = true) ||
                            it.routerName.contains(batchHistorySearch, ignoreCase = true)
                    }
                }

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
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
                            Text(
                                text = "سجل أحدث الدفعات",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = AddCardsMaroon
                            )
                            Text(
                                text = "إجمالي الدفعات: ${cardBatches.size}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondaryLight
                            )
                        }

                        if (cardBatches.isNotEmpty()) {
                            OutlinedTextField(
                                value = batchHistorySearch,
                                onValueChange = { batchHistorySearch = it },
                                placeholder = { Text("بحث برمز الدفعة أو الباقة...", fontSize = 11.sp) },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("batch_history_search_input")
                            )
                        }

                        // Maroon Table Header Bar matching screenshot
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = AddCardsMaroon,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("الدفعة", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.2f))
                                Text("العدد", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(0.7f))
                                Text("التاريخ", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(1.0f))
                                Text("الباقة", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(1.0f))
                                Text("السيرفر", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(0.8f))
                                Text("الإجراءات", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(1.0f))
                            }
                        }

                        if (visibleBatches.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 22.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "لا توجد دفعات بعد.",
                                    fontSize = 13.sp,
                                    color = TextSecondaryLight
                                )
                            }
                        } else {
                            visibleBatches.forEach { batch ->
                                BatchHistoryRow(
                                    batch = batch,
                                    onViewCards = {
                                        viewModel.selectedBatchIdForView.value = batch.id
                                        viewingBatch = batch
                                    },
                                    onPrintBatch = {
                                        val tmpl = resolveTemplateForBatch(batch)
                                        scope.launch {
                                            val items = viewModel.getBatchPrintItems(batch)
                                            exportAndOpenPdf(tmpl, items)
                                        }
                                    },
                                    onPreviewBatch = {
                                        val tmpl = resolveTemplateForBatch(batch)
                                        scope.launch {
                                            val items = viewModel.getBatchPrintItems(batch)
                                            a4PreviewTemplate = tmpl
                                            a4PreviewCards = items
                                            showA4Preview = true
                                        }
                                    },
                                    onEditBatch = { editingBatch = batch },
                                    onToggleBatch = {
                                        viewModel.toggleCardBatch(activeRouter, batch, batch.isDisabled)
                                    },
                                    onSyncHotspot = {
                                        viewModel.pushBatchToTargetServer(activeRouter, batch, "HOTSPOT")
                                    },
                                    onSyncUserManager = {
                                        viewModel.pushBatchToTargetServer(activeRouter, batch, "USER_MANAGER")
                                    },
                                    onDeleteBatch = { deletingBatch = batch }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // ================= DIALOGS =================
    if (showA4Preview) {
        val dlgBg = remember(a4PreviewTemplate.bgImageUri, a4PreviewTemplate.enableBackground) {
            if (a4PreviewTemplate.enableBackground && a4PreviewTemplate.bgImageUri.isNotBlank()) {
                CardPdfGenerator.loadBitmapFromPathOrUri(context, a4PreviewTemplate.bgImageUri)
            } else null
        }
        val dlgLogo = remember(a4PreviewTemplate.logoImageUri, a4PreviewTemplate.enableLogo) {
            if (a4PreviewTemplate.enableLogo && a4PreviewTemplate.logoImageUri.isNotBlank()) {
                CardPdfGenerator.loadBitmapFromPathOrUri(context, a4PreviewTemplate.logoImageUri)
            } else null
        }
        A4SheetPreviewDialog(
            template = a4PreviewTemplate,
            bgBitmap = dlgBg,
            logoBitmap = dlgLogo,
            cards = a4PreviewCards,
            onDismiss = { showA4Preview = false },
            onExportAndOpenPdf = {
                showA4Preview = false
                exportAndOpenPdf(a4PreviewTemplate, a4PreviewCards)
            }
        )
    }

    viewingBatch?.let { batch ->
        BatchCardsViewerDialog(
            batch = batch,
            items = selectedBatchItems,
            onDismiss = {
                viewingBatch = null
                viewModel.selectedBatchIdForView.value = null
            },
            onPrintPdf = {
                val tmpl = resolveTemplateForBatch(batch)
                val items = selectedBatchItems.map {
                    GeneratedCardPrintItem(
                        username = it.username,
                        password = it.password,
                        profileName = it.profileName,
                        price = it.price,
                        serial = it.serial,
                        paymentNumber = it.paymentNumber,
                        posName = it.posName
                    )
                }
                exportAndOpenPdf(tmpl, items)
            },
            onToggleSingleCard = { item, enable ->
                viewModel.toggleSingleBatchCard(activeRouter, batch, item, enable)
            },
            onDeleteSingleCard = { item ->
                viewModel.deleteSingleBatchCard(activeRouter, batch, item)
            }
        )
    }

    editingBatch?.let { batch ->
        EditBatchDialog(
            batch = batch,
            packages = allPackages,
            templates = availableTemplates,
            onDismiss = { editingBatch = null },
            onSave = { code, prof, price, tmpl ->
                viewModel.editCardBatch(activeRouter, batch, code, prof, price, tmpl)
                editingBatch = null
            }
        )
    }

    deletingBatch?.let { batch ->
        DeleteBatchConfirmDialog(
            batch = batch,
            onDismiss = { deletingBatch = null },
            onDelete = { alsoRouter ->
                viewModel.deleteCardBatch(activeRouter, batch, alsoRouter)
                deletingBatch = null
            }
        )
    }

    pendingBatchChoice?.let { (batch, printItems) ->
        BatchPostGenerationDialog(
            batch = batch,
            activeRouter = activeRouter,
            packages = allPackages,
            isProcessing = isProcessingBatch,
            onPushToHotspot = { chosenProf ->
                viewModel.pushBatchToTargetServer(activeRouter, batch, "HOTSPOT", chosenProf)
            },
            onPushToUserManager = { chosenProf ->
                viewModel.pushBatchToTargetServer(activeRouter, batch, "USER_MANAGER", chosenProf)
            },
            onPrintPdf = {
                val tmpl = resolveTemplateForBatch(batch)
                exportAndOpenPdf(tmpl, printItems)
            },
            onDismissKeepLocal = {
                viewModel.dismissBatchPushChoice()
            }
        )
    }
}

@Composable
private fun ModeTabButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    tag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contentColor = if (isSelected) AddCardsMaroon else Color(0xFF64748B)
    Column(
        modifier = modifier
            .heightIn(min = 52.dp)
            .clickable(onClick = onClick)
            .testTag(tag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = title, tint = contentColor, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = contentColor
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .width(72.dp)
                .height(3.dp)
                .background(
                    if (isSelected) AddCardsMaroon else Color.Transparent,
                    RoundedCornerShape(2.dp)
                )
        )
    }
}

@Composable
private fun ServerFilterPill(
    label: String,
    selected: Boolean,
    tag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (selected) Color.White else Color.Transparent,
        tonalElevation = if (selected) 2.dp else 0.dp,
        modifier = modifier
            .heightIn(min = 48.dp)
            .testTag(tag)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp)) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (selected) AddCardsMaroon else Color(0xFF475569),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TemplateStatTile(
    title: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(text = title, fontSize = 11.sp, color = TextSecondaryLight)
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun BatchHistoryRow(
    batch: CardBatchEntity,
    onViewCards: () -> Unit,
    onPrintBatch: () -> Unit,
    onPreviewBatch: () -> Unit,
    onEditBatch: () -> Unit,
    onToggleBatch: () -> Unit,
    onSyncHotspot: () -> Unit,
    onSyncUserManager: () -> Unit,
    onDeleteBatch: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val dateFmt = remember(batch.createdAt) {
        SimpleDateFormat("MM/dd HH:mm", Locale.ENGLISH).format(Date(batch.createdAt))
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (batch.isDisabled) Color(0xFFFEF2F2) else Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, if (batch.isDisabled) Color(0xFFFCA5A5) else Color(0xFFE2E8F0)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onViewCards)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1.2f)) {
                Text(
                    text = batch.batchCode,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryNavy,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = when {
                        batch.isDisabled -> "⏸ موقوفة"
                        batch.isSyncedToRouter -> "🟢 مرفوعة"
                        else -> "🟠 محلية"
                    },
                    fontSize = 9.sp,
                    color = when {
                        batch.isDisabled -> Color(0xFFDC2626)
                        batch.isSyncedToRouter -> AddCardsEmerald
                        else -> Color(0xFFD97706)
                    }
                )
            }

            Text(
                text = "${batch.cardsCount}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = AddCardsMaroon,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(0.7f)
            )

            Text(
                text = dateFmt,
                fontSize = 10.sp,
                color = TextSecondaryLight,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1.0f)
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1.0f)
            ) {
                Text(
                    text = batch.profileName,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PrimaryNavy,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = batch.priceLabel,
                    fontSize = 9.sp,
                    color = TextSecondaryLight
                )
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.weight(0.8f)
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (batch.serverType == "HOTSPOT") Color(0xFFFEF3C7) else Color(0xFFDBEAFE)
                ) {
                    Text(
                        text = if (batch.serverType == "HOTSPOT") "HS" else "UM",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (batch.serverType == "HOTSPOT") Color(0xFFD97706) else Color(0xFF1D4ED8),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Box(
                contentAlignment = Alignment.CenterEnd,
                modifier = Modifier.weight(1.0f)
            ) {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("batch_actions_${batch.id}")
                ) {
                    Icon(Icons.Default.MoreVert, contentDescription = "إجراءات الدفعة", tint = AddCardsMaroon)
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        leadingIcon = { Icon(Icons.Default.Visibility, contentDescription = null, tint = AddCardsSkyText) },
                        text = { Text("عرض الكروت والبحث") },
                        onClick = { menuExpanded = false; onViewCards() }
                    )
                    DropdownMenuItem(
                        leadingIcon = { Icon(Icons.Default.Print, contentDescription = null, tint = AddCardsEmerald) },
                        text = { Text("طباعة الدفعة PDF (${batch.templateName})") },
                        onClick = { menuExpanded = false; onPrintBatch() }
                    )
                    DropdownMenuItem(
                        leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = AddCardsSkyText) },
                        text = { Text("معاينة ورقة الطباعة A4") },
                        onClick = { menuExpanded = false; onPreviewBatch() }
                    )
                    DropdownMenuItem(
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = PrimaryNavy) },
                        text = { Text("تعديل الدفعة والباقة والقالب") },
                        onClick = { menuExpanded = false; onEditBatch() }
                    )
                    DropdownMenuItem(
                        leadingIcon = { Icon(Icons.Default.WifiTethering, contentDescription = null, tint = Color(0xFFD97706)) },
                        text = { Text(if (batch.isSyncedToRouter) "إعادة الإضافة إلى سيرفر الهوتسبوت" else "إضافة إلى سيرفر الهوتسبوت (Hotspot)") },
                        onClick = { menuExpanded = false; onSyncHotspot() }
                    )
                    DropdownMenuItem(
                        leadingIcon = { Icon(Icons.Default.CreditCard, contentDescription = null, tint = Color(0xFF0284C7)) },
                        text = { Text(if (batch.isSyncedToRouter) "إعادة الإضافة إلى سيرفر اليوزرمانجر" else "إضافة إلى سيرفر اليوزرمانجر (User Manager)") },
                        onClick = { menuExpanded = false; onSyncUserManager() }
                    )
                    DropdownMenuItem(
                        leadingIcon = {
                            Icon(
                                imageVector = if (batch.isDisabled) Icons.Default.PlayArrow else Icons.Default.Pause,
                                contentDescription = null,
                                tint = if (batch.isDisabled) AddCardsEmerald else Color(0xFFD97706)
                            )
                        },
                        text = { Text(if (batch.isDisabled) "تفعيل الدفعة" else "إيقاف الدفعة مؤقتاً") },
                        onClick = { menuExpanded = false; onToggleBatch() }
                    )
                    DropdownMenuItem(
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFDC2626)) },
                        text = { Text("حذف الدفعة", color = Color(0xFFDC2626)) },
                        onClick = { menuExpanded = false; onDeleteBatch() }
                    )
                }
            }
        }
    }
}
