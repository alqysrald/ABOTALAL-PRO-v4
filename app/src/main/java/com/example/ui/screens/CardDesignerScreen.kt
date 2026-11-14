package com.example.ui.screens

import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.CardTemplateEntity
import com.example.data.model.GeneratedCardPrintItem
import com.example.ui.TalalProViewModel
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.utils.CardPdfGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CardDesignerScreen(
    viewModel: TalalProViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val routers by viewModel.allRouters.collectAsState()
    val activeRouter by viewModel.activeDesignerRouter.collectAsState()
    val umProfiles by viewModel.userManagerProfiles.collectAsState()
    val hsProfiles by viewModel.hotspotProfiles.collectAsState()
    val savedTemplates by viewModel.cardTemplates.collectAsState()
    val isRefreshingProfiles by viewModel.isRefreshingDesignerProfiles.collectAsState()
    val profilesDisconnected by viewModel.designerProfilesDisconnected.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    // Ensure router is initialized for profile dropdowns
    LaunchedEffect(routers) {
        if (activeRouter == null && routers.isNotEmpty()) {
            val first = routers.first()
            viewModel.activeDesignerRouter.value = first
            if (viewModel.selectedUserManagerRouter.value == null) {
                viewModel.selectedUserManagerRouter.value = first
            }
            if (viewModel.selectedHotspotRouter.value == null) {
                viewModel.selectedHotspotRouter.value = first
            }
        }
    }

    // Active template state (all 16 sections live-synced!)
    var template by remember {
        mutableStateOf(
            CardTemplateEntity(
                userId = currentUser?.id ?: ""
            )
        )
    }

    // Load initial saved template if one exists on first open
    var initializedFromDb by remember { mutableStateOf(false) }
    LaunchedEffect(savedTemplates) {
        if (!initializedFromDb && savedTemplates.isNotEmpty()) {
            template = savedTemplates.first()
            initializedFromDb = true
        }
    }

    // Cached bitmaps for Live Preview
    var bgBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var logoBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(template.bgImageUri, template.enableBackground) {
        bgBitmap = if (template.bgImageUri.isNotBlank()) {
            withContext(Dispatchers.IO) {
                CardPdfGenerator.loadBitmapFromPathOrUri(context, template.bgImageUri)
            }
        } else null
    }

    LaunchedEffect(template.logoImageUri, template.enableLogo) {
        logoBitmap = if (template.logoImageUri.isNotBlank()) {
            withContext(Dispatchers.IO) {
                CardPdfGenerator.loadBitmapFromPathOrUri(context, template.logoImageUri)
            }
        } else null
    }

    // Zero-permission Photo Pickers (Play Policy Compliant)
    val bgPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                val savedPath = CardPdfGenerator.persistPickedImage(context, uri, "card_bg")
                withContext(Dispatchers.Main) {
                    if (savedPath != null) {
                        template = template.copy(
                            bgImageUri = savedPath,
                            enableBackground = true
                        )
                        Toast.makeText(context, "تم رفع وحفظ خلفية الكرت بنجاح ✓", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "تعذر قراءة الصورة المختارة", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    val logoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                val savedPath = CardPdfGenerator.persistPickedImage(context, uri, "card_logo")
                withContext(Dispatchers.Main) {
                    if (savedPath != null) {
                        template = template.copy(
                            logoImageUri = savedPath,
                            enableLogo = true
                        )
                        Toast.makeText(context, "تم استيراد شعار الكرت بنجاح ✓", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "تعذر قراءة الشعار المختار", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    // Dialog states
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showSavedGalleryDialog by remember { mutableStateOf(false) }
    var showA4PreviewDialog by remember { mutableStateOf(false) }
    var isExportingPdf by remember { mutableStateOf(false) }

    // Live sample card reflecting digit length & category price
    val sampleCard = remember(
        template.usernameDigits,
        template.categoryChip,
        template.linkedUmProfile,
        template.linkedHotspotProfile
    ) {
        val code = CardPdfGenerator.sampleUsername(template.usernameDigits)
        GeneratedCardPrintItem(
            username = code,
            password = code.takeLast(6),
            profileName = template.linkedUmProfile.ifBlank { template.linkedHotspotProfile.ifBlank { "default" } },
            price = template.categoryChip.replace("فئة ", ""),
            serial = "SN-0001",
            paymentNumber = "B-1024",
            posName = activeRouter?.name ?: "نقطة البيع"
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF1F5F9))
    ) {
        // ================= SCROLLABLE DESIGNER SECTIONS (TOP TO BOTTOM) =================
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. شريط الإجراءات الرئيسي (Action Bar) - أعلى القسم
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // زر الحذف (Delete) - لون أحمر
                            Button(
                                onClick = { showDeleteConfirm = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFDC2626),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                                modifier = Modifier
                                    .weight(0.9f)
                                    .height(48.dp)
                                    .testTag("delete_design_button")
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "حذف", modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("حذف", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            // زر جديد (New) - لون أبيض
                            OutlinedButton(
                                onClick = {
                                    template = CardTemplateEntity(userId = currentUser?.id ?: "")
                                    Toast.makeText(context, "تم فتح تصميم جديد (Default_Design)", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = Color.White,
                                    contentColor = PrimaryNavy
                                ),
                                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                                modifier = Modifier
                                    .weight(0.9f)
                                    .height(48.dp)
                                    .testTag("new_design_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "جديد", modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("جديد", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            // زر حفظ التصميم (Save Design) - لون أزرق/تركوازي
                            Button(
                                onClick = {
                                    val systemResolved = when {
                                        template.linkedUmProfile.isNotBlank() && template.linkedHotspotProfile.isNotBlank() -> "BOTH"
                                        template.linkedHotspotProfile.isNotBlank() -> "HOTSPOT"
                                        else -> "USER_MANAGER"
                                    }
                                    val primaryProfile = template.linkedUmProfile.ifBlank {
                                        template.linkedHotspotProfile.ifBlank { "default" }
                                    }
                                    val toSave = template.copy(
                                        userId = currentUser?.id ?: "",
                                        templateName = template.templateName.ifBlank { "Default_Design" },
                                        systemType = systemResolved,
                                        profileName = primaryProfile
                                    )
                                    viewModel.saveCardTemplate(toSave) { newId ->
                                        template = toSave.copy(id = newId)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF0891B2),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(48.dp)
                                    .testTag("save_design_button")
                            ) {
                                Icon(Icons.Default.Save, contentDescription = "حفظ التصميم", modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("حفظ التصميم", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }

                        // Quick switcher for saved templates if any exist
                        if (savedTemplates.isNotEmpty()) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(savedTemplates, key = { it.id }) { item ->
                                    val isCurrent = item.id == template.id && template.id != 0L
                                    FilterChip(
                                        selected = isCurrent,
                                        onClick = { template = item },
                                        label = {
                                            Text(
                                                "${item.templateName} (${item.categoryChip})",
                                                fontSize = 11.sp,
                                                maxLines = 1
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.Style,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. قسم قالب التصميم (Template Settings)
            item {
                DesignerSectionCard(
                    title = "قالب التصميم",
                    icon = Icons.Default.Lightbulb,
                    iconTint = Color(0xFFF59E0B)
                ) {
                    OutlinedTextField(
                        value = template.templateName,
                        onValueChange = { template = template.copy(templateName = it) },
                        label = { Text("اسم التصميم") },
                        placeholder = { Text("Default_Design") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("template_name_input")
                    )

                    Text(
                        text = "اختيار الفئة (لتحديد نوع القالب):",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondaryLight
                    )

                    val categories = listOf(
                        "فئة 100 ريال",
                        "فئة 200 ريال",
                        "فئة 250 ريال",
                        "فئة 300 ريال",
                        "فئة 500 ريال",
                        "فئة 1000 ريال",
                        "فئة 3000 ريال"
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        categories.forEach { cat ->
                            val selected = template.categoryChip == cat
                            FilterChip(
                                selected = selected,
                                onClick = { template = template.copy(categoryChip = cat) },
                                label = {
                                    Text(
                                        text = cat,
                                        fontSize = 11.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF0891B2),
                                    selectedLabelColor = Color.White,
                                    containerColor = Color(0xFFF8FAFC),
                                    labelColor = PrimaryNavy
                                )
                            )
                        }
                    }
                }
            }

            // 3. قسم الربط بالباقة (Package Linking)
            item {
                DesignerSectionCard(
                    title = "الربط بالباقة",
                    icon = Icons.Default.Link,
                    iconTint = PrimaryBlue
                ) {
                    // Active Router Picker
                    if (routers.isNotEmpty()) {
                        var routerMenuExpanded by remember { mutableStateOf(false) }
                        Box {
                            OutlinedCard(
                                onClick = { routerMenuExpanded = true },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                colors = CardDefaults.outlinedCardColors(containerColor = Color(0xFFF8FAFC)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Router, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "الراوتر: ${activeRouter?.name ?: "اختر راوتر"}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryNavy
                                        )
                                    }
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            }
                            DropdownMenu(
                                expanded = routerMenuExpanded,
                                onDismissRequest = { routerMenuExpanded = false }
                            ) {
                                routers.forEach { r ->
                                    DropdownMenuItem(
                                        text = { Text("${r.name} (${r.host})") },
                                        onClick = {
                                            routerMenuExpanded = false
                                            viewModel.refreshProfilesForCardDesigner(r)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // User Manager Profile Dropdown
                    var umExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = umExpanded,
                        onExpandedChange = { umExpanded = !umExpanded }
                    ) {
                        OutlinedTextField(
                            value = template.linkedUmProfile.ifBlank { "اختر باقة User Manager..." },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("ربطه مع باقة User Manager") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = umExpanded) },
                            leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, tint = Color(0xFF0284C7)) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = umExpanded,
                            onDismissRequest = { umExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("بدون ربط (غير محدد)", color = TextSecondaryLight) },
                                onClick = {
                                    template = template.copy(linkedUmProfile = "")
                                    umExpanded = false
                                }
                            )
                            if (umProfiles.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("لا توجد باقات محملة — اضغط تحديث الباقات", fontSize = 12.sp, color = Color(0xFFD97706)) },
                                    onClick = { umExpanded = false }
                                )
                            } else {
                                umProfiles.distinctBy { it.name }.forEach { prof ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(prof.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                Text("السعر: ${prof.price} | الصلاحية: ${prof.validity}", fontSize = 11.sp, color = TextSecondaryLight)
                                            }
                                        },
                                        onClick = {
                                            template = template.copy(linkedUmProfile = prof.name)
                                            umExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Hotspot Profile Dropdown
                    var hsExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = hsExpanded,
                        onExpandedChange = { hsExpanded = !hsExpanded }
                    ) {
                        OutlinedTextField(
                            value = template.linkedHotspotProfile.ifBlank { "اختر باقة Hotspot..." },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("ربطه مع باقة Hotspot") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = hsExpanded) },
                            leadingIcon = { Icon(Icons.Default.WifiTethering, contentDescription = null, tint = Color(0xFFEA580C)) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = hsExpanded,
                            onDismissRequest = { hsExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("بدون ربط (غير محدد)", color = TextSecondaryLight) },
                                onClick = {
                                    template = template.copy(linkedHotspotProfile = "")
                                    hsExpanded = false
                                }
                            )
                            if (hsProfiles.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("لا توجد بروفايلات هوتسبوت محملة — اضغط تحديث", fontSize = 12.sp, color = Color(0xFFD97706)) },
                                    onClick = { hsExpanded = false }
                                )
                            } else {
                                hsProfiles.distinctBy { it.name }.forEach { prof ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(prof.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                Text("السرعة: ${prof.rateLimit} | مشتركين: ${prof.sharedUsers}", fontSize = 11.sp, color = TextSecondaryLight)
                                            }
                                        },
                                        onClick = {
                                            template = template.copy(linkedHotspotProfile = prof.name)
                                            hsExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Refresh Profiles Button
                    val infiniteTransition = rememberInfiniteTransition(label = "refreshSpin")
                    val angle by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "spinAngle"
                    )

                    Button(
                        onClick = { viewModel.refreshProfilesForCardDesigner(activeRouter) },
                        enabled = !isRefreshingProfiles,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("refresh_profiles_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier
                                .size(18.dp)
                                .then(if (isRefreshingProfiles) Modifier.rotate(angle) else Modifier)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isRefreshingProfiles) "جاري تحديث الباقات من RouterOS..." else "تحديث الباقات من RouterOS",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    // Helper notice if disconnected or profiles missing
                    if (profilesDisconnected || routers.isEmpty() || (umProfiles.isEmpty() && hsProfiles.isEmpty())) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFFFFBEB),
                            border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.WifiOff, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "اتصل بالراوتر لجلب الباقات الحقيقية",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF92400E)
                                )
                            }
                        }
                    }
                }
            }

            // 4. قسم بيانات الصفحة (Page Data & Print Settings)
            item {
                DesignerSectionCard(
                    title = "بيانات الصفحة",
                    icon = Icons.Default.Settings,
                    iconTint = Color(0xFF475569)
                ) {
                    IntStepperRow(
                        label = "عدد الأعمدة",
                        value = template.columnsPerPage,
                        minValue = 1,
                        maxValue = 10,
                        onValueChange = { cols ->
                            template = template.copy(
                                columnsPerPage = cols,
                                cardsCount = cols * template.rowsPerPage
                            )
                        }
                    )

                    IntStepperRow(
                        label = "عدد الصفوف",
                        value = template.rowsPerPage,
                        minValue = 1,
                        maxValue = 30,
                        onValueChange = { rows ->
                            template = template.copy(
                                rowsPerPage = rows,
                                cardsCount = template.columnsPerPage * rows
                            )
                        }
                    )

                    IntStepperRow(
                        label = "عدد الكروت",
                        value = template.cardsCount,
                        minValue = 1,
                        maxValue = 500,
                        onValueChange = { cnt ->
                            template = template.copy(cardsCount = cnt)
                        }
                    )

                    IntStepperRow(
                        label = "عدد أرقام اسم المستخدم",
                        value = template.usernameDigits,
                        minValue = 4,
                        maxValue = 18,
                        onValueChange = { digits ->
                            template = template.copy(usernameDigits = digits)
                        }
                    )

                    HorizontalDivider(color = BorderLight.copy(alpha = 0.5f))

                    DesignerToggleRow(
                        label = "مضاعفة الطباعة لمقاس الكروت حسب A4",
                        checked = template.scaleToA4,
                        onCheckedChange = { template = template.copy(scaleToA4 = it) }
                    )

                    Text(
                        text = "هامش بين الكروت",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryNavy
                    )

                    FloatStepperRow(
                        label = "أفقي (مم)",
                        value = template.marginHorizontalMm,
                        minValue = 0.0f,
                        maxValue = 20.0f,
                        step = 0.5f,
                        onValueChange = { template = template.copy(marginHorizontalMm = it) }
                    )

                    FloatStepperRow(
                        label = "رأسي (مم)",
                        value = template.marginVerticalMm,
                        minValue = 0.0f,
                        maxValue = 20.0f,
                        step = 0.5f,
                        onValueChange = { template = template.copy(marginVerticalMm = it) }
                    )

                    HorizontalDivider(color = BorderLight.copy(alpha = 0.5f))

                    DesignerToggleRow(
                        label = "ملاحظة على الصفحة",
                        checked = template.showPageNote,
                        onCheckedChange = { template = template.copy(showPageNote = it) }
                    )

                    AnimatedVisibility(visible = template.showPageNote) {
                        OutlinedTextField(
                            value = template.pageNote,
                            onValueChange = { template = template.copy(pageNote = it) },
                            label = { Text("ملاحظة على الصفحة") },
                            placeholder = { Text("اكتب ملاحظة تظهر أعلى صفحة الطباعة...") },
                            minLines = 3,
                            maxLines = 5,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // 5. قسم الخلفية (Background)
            item {
                DesignerSectionCard(
                    title = "الخلفية",
                    icon = Icons.Default.Image,
                    iconTint = Color(0xFF0D9488),
                    trailingSwitchChecked = template.enableBackground,
                    onTrailingSwitchChange = { template = template.copy(enableBackground = it) }
                ) {
                    DesignerToggleRow(
                        label = "تفعيل خلفية الكرت",
                        checked = template.enableBackground,
                        onCheckedChange = { template = template.copy(enableBackground = it) }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                template = template.copy(bgImageUri = "", enableBackground = false)
                                bgBitmap = null
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color(0xFFF1F5F9),
                                contentColor = Color(0xFF64748B)
                            ),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(0.8f)
                                .height(48.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("مسح", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                bgPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1.4f)
                                .height(48.dp)
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("اختيار صورة ورفعها", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Text(
                        text = "تحفظ الخلفية ضمن التصميم على الخادم، وتملأ مساحة الكرت بدون حواف إضافية",
                        fontSize = 11.sp,
                        color = TextSecondaryLight,
                        lineHeight = 16.sp
                    )
                }
            }

            // 6. قسم حواف الكرت (Card Borders)
            item {
                DesignerSectionCard(
                    title = "حواف الكرت",
                    icon = Icons.Default.CropSquare,
                    iconTint = Color(0xFF1E293B),
                    trailingSwitchChecked = template.enableBorder,
                    onTrailingSwitchChange = { template = template.copy(enableBorder = it) }
                ) {
                    DesignerToggleRow(
                        label = "تفعيل الإطار",
                        checked = template.enableBorder,
                        onCheckedChange = { template = template.copy(enableBorder = it) }
                    )

                    AnimatedVisibility(visible = template.enableBorder) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            ColorPickerFieldRow(
                                label = "لون الإطار",
                                colorHex = template.borderColorHex,
                                onColorSelected = { template = template.copy(borderColorHex = it) }
                            )

                            FloatStepperRow(
                                label = "حجم الإطار (مم)",
                                value = template.borderSizeMm,
                                minValue = 0.1f,
                                maxValue = 5.0f,
                                step = 0.1f,
                                onValueChange = { template = template.copy(borderSizeMm = it) }
                            )
                        }
                    }
                }
            }

            // 7. قسم عناصر الكرت (Card Elements)
            item {
                DesignerSectionCard(
                    title = "عناصر الكرت",
                    icon = Icons.Default.FormatListBulleted,
                    iconTint = Color(0xFF6366F1)
                ) {
                    Text(
                        text = "عرض الوحدة / العنوان",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryNavy
                    )

                    DesignerToggleRow(
                        label = "إظهار اسم المستخدم",
                        checked = template.showUsername,
                        onCheckedChange = { template = template.copy(showUsername = it) }
                    )

                    DesignerToggleRow(
                        label = "تفعيل عنوان مخصص",
                        checked = template.enableCustomTitle,
                        onCheckedChange = { template = template.copy(enableCustomTitle = it) }
                    )

                    AnimatedVisibility(visible = template.enableCustomTitle) {
                        OutlinedTextField(
                            value = template.customTitleText,
                            onValueChange = { template = template.copy(customTitleText = it) },
                            label = { Text("نص العنوان") },
                            placeholder = { Text("أدخل عنوان الكرت...") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    HorizontalDivider(color = BorderLight.copy(alpha = 0.5f))

                    Text(
                        text = "إعدادات الخط",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryNavy
                    )

                    ColorPickerFieldRow(
                        label = "لون الخط",
                        colorHex = template.fontColorHex,
                        onColorSelected = { template = template.copy(fontColorHex = it) }
                    )

                    FloatStepperRow(
                        label = "حجم الخط",
                        value = template.fontSize,
                        minValue = 5.0f,
                        maxValue = 30.0f,
                        step = 0.5f,
                        onValueChange = { template = template.copy(fontSize = it) }
                    )

                    DesignerToggleRow(
                        label = "الخط غامق",
                        checked = template.isFontBold,
                        onCheckedChange = { template = template.copy(isFontBold = it) }
                    )
                }
            }

            // 8. قسم رقم الصفحة (Page Number) + تحريك كود المستخدم يدوياً
            item {
                DesignerSectionCard(
                    title = "رقم الصفحة",
                    icon = Icons.Default.FormatListNumbered,
                    iconTint = Color(0xFF0284C7),
                    trailingSwitchChecked = template.showPageNumber,
                    onTrailingSwitchChange = { template = template.copy(showPageNumber = it) }
                ) {
                    DesignerToggleRow(
                        label = "إظهار رقم الصفحة",
                        checked = template.showPageNumber,
                        onCheckedChange = { template = template.copy(showPageNumber = it) }
                    )

                    Text(
                        text = "تحريك كود المستخدم يدويا",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryNavy
                    )

                    DPadController(
                        title = "موضع كود المستخدم داخل الكرت",
                        xPercent = template.userXPercent,
                        yPercent = template.userYPercent,
                        onMove = { nx, ny ->
                            template = template.copy(userXPercent = nx, userYPercent = ny)
                        }
                    )
                }
            }

            // 9. قسم كلمة المرور (Password Settings)
            item {
                PositionedElementSectionCard(
                    sectionTitle = "كلمة المرور",
                    toggleLabel = "إظهار كلمة المرور",
                    textFieldLabel = "نص كلمة المرور",
                    sizeStepperLabel = "حجم كلمة المرور",
                    icon = Icons.Default.Key,
                    iconTint = Color(0xFF7C3AED),
                    isEnabled = template.showPassword,
                    onToggleChange = { template = template.copy(showPassword = it) },
                    textValue = template.passwordText,
                    onTextChange = { template = template.copy(passwordText = it) },
                    fontSize = template.passwordFontSize,
                    onFontSizeChange = { template = template.copy(passwordFontSize = it) },
                    xPercent = template.passwordXPercent,
                    yPercent = template.passwordYPercent,
                    onMove = { nx, ny ->
                        template = template.copy(passwordXPercent = nx, passwordYPercent = ny)
                    }
                )
            }

            // 10. قسم رقم الدفعة (Payment Number)
            item {
                PositionedElementSectionCard(
                    sectionTitle = "رقم الدفعة",
                    toggleLabel = "إظهار رقم الدفعة",
                    textFieldLabel = "نص رقم الدفعة",
                    sizeStepperLabel = "حجم رقم الدفعة",
                    icon = Icons.Default.ConfirmationNumber,
                    iconTint = Color(0xFF0891B2),
                    isEnabled = template.showPayment,
                    onToggleChange = { template = template.copy(showPayment = it) },
                    textValue = template.paymentText,
                    onTextChange = { template = template.copy(paymentText = it) },
                    fontSize = template.paymentFontSize,
                    onFontSizeChange = { template = template.copy(paymentFontSize = it) },
                    xPercent = template.paymentXPercent,
                    yPercent = template.paymentYPercent,
                    onMove = { nx, ny ->
                        template = template.copy(paymentXPercent = nx, paymentYPercent = ny)
                    }
                )
            }

            // 11. قسم الرقم التسلسلي (Serial Number)
            item {
                PositionedElementSectionCard(
                    sectionTitle = "الرقم التسلسلي",
                    toggleLabel = "إظهار الرقم التسلسلي",
                    textFieldLabel = "نص الرقم التسلسلي",
                    sizeStepperLabel = "حجم الرقم التسلسلي",
                    icon = Icons.Default.Pin,
                    iconTint = Color(0xFF059669),
                    isEnabled = template.showSerial,
                    onToggleChange = { template = template.copy(showSerial = it) },
                    textValue = template.serialText,
                    onTextChange = { template = template.copy(serialText = it) },
                    fontSize = template.serialFontSize,
                    onFontSizeChange = { template = template.copy(serialFontSize = it) },
                    xPercent = template.serialXPercent,
                    yPercent = template.serialYPercent,
                    onMove = { nx, ny ->
                        template = template.copy(serialXPercent = nx, serialYPercent = ny)
                    }
                )
            }

            // 12. قسم سعر الباقة (Package Price)
            item {
                PositionedElementSectionCard(
                    sectionTitle = "سعر الباقة",
                    toggleLabel = "إظهار سعر الباقة",
                    textFieldLabel = "نص سعر الباقة",
                    sizeStepperLabel = "حجم سعر الباقة",
                    icon = Icons.Default.Sell,
                    iconTint = Color(0xFFEA580C),
                    isEnabled = template.showPrice,
                    onToggleChange = { template = template.copy(showPrice = it) },
                    textValue = template.priceTagText,
                    onTextChange = { template = template.copy(priceTagText = it) },
                    fontSize = template.priceFontSize,
                    onFontSizeChange = { template = template.copy(priceFontSize = it) },
                    xPercent = template.priceXPercent,
                    yPercent = template.priceYPercent,
                    onMove = { nx, ny ->
                        template = template.copy(priceXPercent = nx, priceYPercent = ny)
                    }
                )
            }

            // 13. قسم نقطة البيع (Point of Sale - POS)
            item {
                PositionedElementSectionCard(
                    sectionTitle = "نقطة البيع",
                    toggleLabel = "إظهار نقطة البيع",
                    textFieldLabel = "نص نقطة البيع",
                    sizeStepperLabel = "حجم نقطة البيع",
                    icon = Icons.Default.Storefront,
                    iconTint = Color(0xFFDB2777),
                    isEnabled = template.showPos,
                    onToggleChange = { template = template.copy(showPos = it) },
                    textValue = template.posText,
                    onTextChange = { template = template.copy(posText = it) },
                    fontSize = template.posFontSize,
                    onFontSizeChange = { template = template.copy(posFontSize = it) },
                    xPercent = template.posXPercent,
                    yPercent = template.posYPercent,
                    onMove = { nx, ny ->
                        template = template.copy(posXPercent = nx, posYPercent = ny)
                    }
                )
            }

            // 14. قسم الباركود (Barcode)
            item {
                DesignerSectionCard(
                    title = "الباركود",
                    icon = Icons.Default.QrCode2,
                    iconTint = Color(0xFF0F172A),
                    trailingSwitchChecked = template.enableBarcode,
                    onTrailingSwitchChange = { template = template.copy(enableBarcode = it) }
                ) {
                    DesignerToggleRow(
                        label = "تفعيل الباركود",
                        checked = template.enableBarcode,
                        onCheckedChange = { template = template.copy(enableBarcode = it) }
                    )

                    AnimatedVisibility(visible = template.enableBarcode) {
                        DPadController(
                            title = "تحريك الباركود داخل الكرت",
                            xPercent = template.barcodeXPercent,
                            yPercent = template.barcodeYPercent,
                            onMove = { nx, ny ->
                                template = template.copy(barcodeXPercent = nx, barcodeYPercent = ny)
                            }
                        )
                    }
                }
            }

            // 15. قسم الشعار (Logo)
            item {
                DesignerSectionCard(
                    title = "الشعار",
                    icon = Icons.Default.Verified,
                    iconTint = Color(0xFF2563EB),
                    trailingSwitchChecked = template.enableLogo,
                    onTrailingSwitchChange = { template = template.copy(enableLogo = it) }
                ) {
                    DesignerToggleRow(
                        label = "تفعيل شعار الكرت",
                        checked = template.enableLogo,
                        onCheckedChange = { template = template.copy(enableLogo = it) }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (template.logoImageUri.isNotBlank()) {
                            OutlinedButton(
                                onClick = {
                                    template = template.copy(logoImageUri = "", enableLogo = false)
                                    logoBitmap = null
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(48.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("مسح", fontSize = 12.sp)
                            }
                        }

                        Button(
                            onClick = {
                                logoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("اختيار شعار", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    FloatStepperRow(
                        label = "العرض (مم)",
                        value = template.logoWidthMm,
                        minValue = 2.0f,
                        maxValue = 50.0f,
                        step = 0.5f,
                        onValueChange = { template = template.copy(logoWidthMm = it) }
                    )

                    FloatStepperRow(
                        label = "الارتفاع (مم)",
                        value = template.logoHeightMm,
                        minValue = 2.0f,
                        maxValue = 50.0f,
                        step = 0.5f,
                        onValueChange = { template = template.copy(logoHeightMm = it) }
                    )

                    AnimatedVisibility(visible = template.enableLogo) {
                        DPadController(
                            title = "تحريك الشعار داخل الكرت",
                            xPercent = template.logoXPercent,
                            yPercent = template.logoYPercent,
                            onMove = { nx, ny ->
                                template = template.copy(logoXPercent = nx, logoYPercent = ny)
                            }
                        )
                    }
                }
            }

            // 16. قسم تصنيفات الخلفيات (Background Categories)
            item {
                DesignerSectionCard(
                    title = "تصنيفات الخلفيات",
                    icon = Icons.Default.PhotoLibrary,
                    iconTint = Color(0xFF9333EA)
                ) {
                    Text(
                        text = "احفظ الخلفية داخل قالب باسم وفئة، ثم أعد اختياره من قائمة القوالب في أي وقت",
                        fontSize = 12.sp,
                        color = TextSecondaryLight,
                        lineHeight = 18.sp
                    )

                    Button(
                        onClick = { showSavedGalleryDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("عرض الخلفيات المحفوظة", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        // ================= STICKY PREVIEW FOOTER (منطقة المعاينة الثابتة) =================
        Surface(
            color = Color.White,
            shadowElevation = 12.dp,
            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Visibility,
                            contentDescription = "معاينة الكرت",
                            tint = PrimaryBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "معاينة الكرت",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = PrimaryNavy
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFE0F2FE)
                    ) {
                        Text(
                            text = "معاينة ثابتة",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                // Live Preview Box
                LiveCardPreviewBox(
                    template = template,
                    bgBitmap = bgBitmap,
                    logoBitmap = logoBitmap,
                    sampleCard = sampleCard,
                    boxHeight = 104.dp
                )

                // Wide Green PDF Preview & Print Button
                Button(
                    onClick = { showA4PreviewDialog = true },
                    enabled = !isExportingPdf,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF16A34A),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("export_pdf_button")
                ) {
                    if (isExportingPdf) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "معاينة وطباعة PDF",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("حذف التصميم [${template.templateName}]؟", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "هل تريد حذف هذا التصميم نهائياً من قائمة القوالب المحفوظة؟",
                    fontSize = 13.sp,
                    color = TextSecondaryLight
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCardTemplate(template)
                        template = CardTemplateEntity(userId = currentUser?.id ?: "")
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("نعم، احذف")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirm = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Saved Templates & Backgrounds Gallery Dialog
    if (showSavedGalleryDialog) {
        SavedTemplatesGalleryDialog(
            templates = savedTemplates,
            onDismiss = { showSavedGalleryDialog = false },
            onLoadFullTemplate = { chosen ->
                template = chosen
                showSavedGalleryDialog = false
                Toast.makeText(context, "تم تحميل القالب [${chosen.templateName}] ✓", Toast.LENGTH_SHORT).show()
            },
            onApplyBackgroundOnly = { chosen ->
                template = template.copy(
                    bgImageUri = chosen.bgImageUri,
                    enableBackground = chosen.bgImageUri.isNotBlank(),
                    categoryChip = chosen.categoryChip
                )
                showSavedGalleryDialog = false
                Toast.makeText(context, "تم تطبيق خلفية [${chosen.templateName}] ✓", Toast.LENGTH_SHORT).show()
            },
            onDeleteTemplate = { chosen ->
                viewModel.deleteCardTemplate(chosen)
            }
        )
    }

    // A4 Preview & Phone PDF Export Dialog
    if (showA4PreviewDialog) {
        val sampleBatch = remember(template) {
            CardPdfGenerator.generateSampleCards(template)
        }
        A4SheetPreviewDialog(
            template = template,
            bgBitmap = bgBitmap,
            logoBitmap = logoBitmap,
            cards = sampleBatch,
            onDismiss = { showA4PreviewDialog = false },
            onExportAndOpenPdf = {
                showA4PreviewDialog = false
                isExportingPdf = true
                scope.launch(Dispatchers.IO) {
                    val file = CardPdfGenerator.generateA4CardsPdf(context, template, sampleBatch)
                    withContext(Dispatchers.Main) {
                        isExportingPdf = false
                        if (file != null) {
                            Toast.makeText(context, "تم توليد ملف PDF بنجاح ✓", Toast.LENGTH_SHORT).show()
                            CardPdfGenerator.openPdfOnDevice(context, file)
                        } else {
                            Toast.makeText(context, "تعذر إنشاء ملف PDF", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        )
    }
}

@Composable
private fun SavedTemplatesGalleryDialog(
    templates: List<CardTemplateEntity>,
    onDismiss: () -> Unit,
    onLoadFullTemplate: (CardTemplateEntity) -> Unit,
    onApplyBackgroundOnly: (CardTemplateEntity) -> Unit,
    onDeleteTemplate: (CardTemplateEntity) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
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
                        text = "القوالب والخلفيات المحفوظة (${templates.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = PrimaryNavy
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                if (templates.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "لا توجد قوالب محفوظة بعد.\nاحفظ تصميماً بالخلفية من الشاشة الرئيسية ليظهر هنا.",
                            fontSize = 12.sp,
                            color = TextSecondaryLight
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 360.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(templates, key = { it.id }) { item ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.templateName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = PrimaryNavy,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${item.categoryChip} • ${item.columnsPerPage}×${item.rowsPerPage} (${item.cardsCount} كرت)",
                                                fontSize = 11.sp,
                                                color = TextSecondaryLight
                                            )
                                        }
                                        IconButton(
                                            onClick = { onDeleteTemplate(item) },
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.DeleteOutline,
                                                contentDescription = "حذف",
                                                tint = Color(0xFFDC2626),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Button(
                                            onClick = { onLoadFullTemplate(item) },
                                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f).height(34.dp)
                                        ) {
                                            Text("تحميل القالب كاملاً", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }

                                        if (item.bgImageUri.isNotBlank()) {
                                            OutlinedButton(
                                                onClick = { onApplyBackgroundOnly(item) },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.weight(1f).height(34.dp)
                                            ) {
                                                Text("تطبيق الخلفية فقط", fontSize = 11.sp)
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
