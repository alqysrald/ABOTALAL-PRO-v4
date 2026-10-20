package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.model.CardTemplateEntity
import com.example.ui.TalalProViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

enum class CardSystemType(val title: String, val systemKey: String) {
    USER_MANAGER("نظام اليوزر مانجر (User Manager)", "USER_MANAGER"),
    HOTSPOT("نظام الهوتسبوت (Hotspot)", "HOTSPOT")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardDesignerScreen(
    viewModel: TalalProViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedSystem by remember { mutableStateOf(CardSystemType.USER_MANAGER) }
    val userManagerProfiles by viewModel.userManagerProfiles.collectAsState()
    val hotspotProfiles by viewModel.hotspotProfiles.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val currentUserId = currentUser?.id ?: ""

    val availableProfiles = if (selectedSystem == CardSystemType.USER_MANAGER) {
        userManagerProfiles.map { it.name }.ifEmpty { listOf("1 Hour - 100MB", "1 Day - 1GB", "1 Week - 5GB", "1 Month - 30GB", "Default Profile") }
    } else {
        hotspotProfiles.map { it.name }.ifEmpty { listOf("default", "1Hour_500MB", "24Hours_Unlimited", "Weekly_VIP", "Monthly_10G") }
    }

    var selectedProfileName by remember { mutableStateOf(availableProfiles.firstOrNull() ?: "Default") }

    // Card Dimensions & Layout Settings
    var columnsCount by remember { mutableIntStateOf(3) }
    var rowsCount by remember { mutableIntStateOf(5) }
    var networkHeaderTitle by remember { mutableStateOf("شبكة طلال نت للتزويد الذكي") }
    var profilePriceText by remember { mutableStateOf("السعر: 200 ريال | الصلاحية: 24 ساعة") }

    // Draggable PIN Settings
    var pinOffsetX by remember { mutableFloatStateOf(80f) }
    var pinOffsetY by remember { mutableFloatStateOf(95f) }
    var pinFontSizeSp by remember { mutableFloatStateOf(16f) }
    var pinTextColor by remember { mutableStateOf(Color(0xFF0F172A)) }
    var showPinBackgroundBox by remember { mutableStateOf(true) }
    var showQrCode by remember { mutableStateOf(true) }
    var showSerialNumber by remember { mutableStateOf(true) }

    // Card Theme & Custom Background
    var cardThemePreset by remember { mutableIntStateOf(0) }
    var customBgBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var customBgUriString by remember { mutableStateOf<String?>(null) }

    // Active View Mode: Single Card Live Design vs A4 Sheet Preview
    var previewMode by remember { mutableIntStateOf(0) } // 0: Design Card, 1: A4 Sheet Preview

    // Photo picker for background image
    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bmp = BitmapFactory.decodeStream(inputStream)
                customBgBitmap = bmp
                customBgUriString = uri.toString()
                Toast.makeText(context, "تم استيراد خلفية الكرت بنجاح ✓", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "فشل قراءة الصورة: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Load saved template when system or profile changes
    LaunchedEffect(selectedSystem, selectedProfileName) {
        val saved = viewModel.getCardTemplateSync(selectedSystem.systemKey, selectedProfileName)
        if (saved != null) {
            pinOffsetX = saved.pinXPercent * 3.5f
            pinOffsetY = saved.pinYPercent * 1.5f
            pinFontSizeSp = saved.pinFontSize.toFloat()
            columnsCount = saved.columnsPerPage
            rowsCount = saved.rowsPerPage
            networkHeaderTitle = saved.templateName.ifBlank { networkHeaderTitle }
            profilePriceText = saved.priceTagText.ifBlank { profilePriceText }
            showQrCode = saved.showQrCode
            customBgUriString = saved.bgImageUri.takeIf { it.isNotBlank() }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "مصمم ومحرر قوالب الكروت",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimaryLight
                        )
                        Text(
                            text = "تخصيص كامل لمعاينة الكرت، تحريك الرقم، وضبط مقاسات الطباعة PDF",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondaryLight
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val template = CardTemplateEntity(
                                userId = currentUserId,
                                systemType = selectedSystem.systemKey,
                                profileName = selectedProfileName,
                                templateName = networkHeaderTitle,
                                priceTagText = profilePriceText,
                                pinXPercent = pinOffsetX / 3.5f,
                                pinYPercent = pinOffsetY / 1.5f,
                                pinFontSize = pinFontSizeSp.toInt(),
                                columnsPerPage = columnsCount,
                                rowsPerPage = rowsCount,
                                showQrCode = showQrCode,
                                bgImageUri = customBgUriString ?: ""
                            )
                            viewModel.saveCardTemplate(template)
                            Toast.makeText(context, "تم حفظ قالب كروت $selectedProfileName بنجاح ✓", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "حفظ القالب", tint = PrimaryBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF1F5F9),
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // System Selector Tabs (User Manager vs Hotspot)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    CardSystemType.values().forEach { sys ->
                        val isSelected = selectedSystem == sys
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) Brush.linearGradient(listOf(PrimaryNavy, PrimaryBlue))
                                    else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                                )
                                .clickable {
                                    selectedSystem = sys
                                    selectedProfileName = availableProfiles.firstOrNull() ?: "Default"
                                }
                        ) {
                            Text(
                                text = sys.title,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else TextSecondaryLight
                            )
                        }
                    }
                }
            }

            // Profile Selector Dropdown / Row
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "اختر البروفايل / الباقة لتخصيص قالب الكرت الخاص بها:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryLight
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        availableProfiles.take(4).forEach { pName ->
                            val isChosen = selectedProfileName == pName
                            FilterChip(
                                selected = isChosen,
                                onClick = { selectedProfileName = pName },
                                label = { Text(pName, fontSize = 11.sp, fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Normal) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryBlue.copy(alpha = 0.15f),
                                    selectedLabelColor = PrimaryBlue
                                )
                            )
                        }
                    }
                }
            }

            // Mode Toggle: Live Interactive Card Canvas VS A4 Page Preview
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { previewMode = 0 },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (previewMode == 0) PrimaryBlue else Color.White,
                        contentColor = if (previewMode == 0) Color.White else PrimaryNavy
                    ),
                    modifier = Modifier.weight(1f).height(46.dp)
                ) {
                    Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("تصميم الكرت والتفاعل", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = { previewMode = 1 },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (previewMode == 1) PrimaryBlue else Color.White,
                        contentColor = if (previewMode == 1) Color.White else PrimaryNavy
                    ),
                    modifier = Modifier.weight(1f).height(46.dp)
                ) {
                    Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("معاينة صفحة A4 والطباعة", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (previewMode == 0) {
                // ================= INTERACTIVE CARD DESIGN CANVAS =================
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "منطقة معاينة الكرت التفاعلية (اسحب الرقم بيدك):",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryNavy
                            )
                            IconButton(onClick = { pickImageLauncher.launch("image/*") }) {
                                Icon(Icons.Default.AddPhotoAlternate, contentDescription = "استيراد خلفية", tint = PrimaryBlue)
                            }
                        }

                        // The Interactive Draggable Card Container
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    when (cardThemePreset) {
                                        0 -> Brush.linearGradient(listOf(Color(0xFF0F172A), Color(0xFF1E3A8A)))
                                        1 -> Brush.linearGradient(listOf(Color(0xFF047857), Color(0xFF065F46)))
                                        2 -> Brush.linearGradient(listOf(Color(0xFF7C2D12), Color(0xFF991B1B)))
                                        else -> Brush.linearGradient(listOf(Color(0xFF312E81), Color(0xFF4C1D95)))
                                    }
                                )
                                .border(2.dp, Color(0xFF38BDF8).copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                        ) {
                            // Header bar on card
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = networkHeaderTitle,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "الباقة: $selectedProfileName",
                                        fontSize = 10.sp,
                                        color = Color(0xFF38BDF8)
                                    )
                                }
                                Icon(Icons.Default.Wifi, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }

                            // Optional QR Code Icon box
                            if (showQrCode) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(12.dp)
                                        .size(44.dp)
                                        .background(Color.White, RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.QrCode2, contentDescription = null, tint = Color(0xFF0F172A), modifier = Modifier.size(36.dp))
                                }
                            }

                            // Optional Price & Validity Footer
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = profilePriceText,
                                    fontSize = 10.sp,
                                    color = Color(0xFFE2E8F0),
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (showSerialNumber) {
                                    Text(
                                        text = "S/N: 2026-98741",
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }

                            // DRAGGABLE PIN BOX WITH GESTURES
                            Box(
                                modifier = Modifier
                                    .offset { IntOffset(pinOffsetX.roundToInt(), pinOffsetY.roundToInt()) }
                                    .pointerInput(Unit) {
                                        detectDragGestures { change, dragAmount ->
                                            change.consume()
                                            pinOffsetX = (pinOffsetX + dragAmount.x).coerceIn(10f, 350f)
                                            pinOffsetY = (pinOffsetY + dragAmount.y).coerceIn(10f, 150f)
                                        }
                                    }
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (showPinBackgroundBox) Color.White else Color.Transparent)
                                    .border(1.dp, Color(0xFF38BDF8), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.DragIndicator, contentDescription = "اسحب", tint = PrimaryBlue, modifier = Modifier.size(14.dp))
                                    Text(
                                        text = "PIN: 84920481",
                                        fontSize = pinFontSizeSp.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontFamily = FontFamily.Monospace,
                                        color = if (showPinBackgroundBox) pinTextColor else Color.White
                                    )
                                }
                            }
                        }

                        // Fine Adjustment Sliders for PIN Position
                        Text(
                            text = "تحكم دقيق بإحداثيات رقم الكرت وحجم الخط:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryLight
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "الموضع الأفقي X: ${pinOffsetX.toInt()}", fontSize = 11.sp, color = TextSecondaryLight)
                                Slider(
                                    value = pinOffsetX,
                                    onValueChange = { pinOffsetX = it },
                                    valueRange = 0f..350f
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "الموضع الرأسي Y: ${pinOffsetY.toInt()}", fontSize = 11.sp, color = TextSecondaryLight)
                                Slider(
                                    value = pinOffsetY,
                                    onValueChange = { pinOffsetY = it },
                                    valueRange = 0f..150f
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "حجم خط الرقم: ${pinFontSizeSp.toInt()}sp", fontSize = 11.sp, color = TextSecondaryLight)
                                Slider(
                                    value = pinFontSizeSp,
                                    onValueChange = { pinFontSizeSp = it },
                                    valueRange = 10f..26f
                                )
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(text = "خيارات إضافية:", fontSize = 11.sp, color = TextSecondaryLight)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = showPinBackgroundBox, onCheckedChange = { showPinBackgroundBox = it })
                                    Text(text = "خلفية بيضاء للرقم", fontSize = 11.sp)
                                }
                            }
                        }

                        // Text fields for card texts
                        OutlinedTextField(
                            value = networkHeaderTitle,
                            onValueChange = { networkHeaderTitle = it },
                            label = { Text("عنوان أعلى الكرت / اسم الشبكة", fontSize = 12.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = profilePriceText,
                            onValueChange = { profilePriceText = it },
                            label = { Text("نص السعر والصلاحية", fontSize = 12.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Theme Presets Selector
                        Text(
                            text = "سمات وألوان خلفية الكرت:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryLight
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val themes = listOf("أزرق ليلي", "أخضر زمردي", "عنابي ملكي", "بنفسجي نيون")
                            themes.forEachIndexed { idx, name ->
                                FilterChip(
                                    selected = cardThemePreset == idx,
                                    onClick = { cardThemePreset = idx },
                                    label = { Text(name, fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                }
            } else {
                // ================= A4 PAGE PRINT PREVIEW & PDF EXPORT =================
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "معاينة صفحة الطباعة A4 وتوزيع الأعمدة:",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryNavy
                        )

                        // A4 Sheet Canvas Preview
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp)
                                .background(Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            // White A4 Sheet simulation (ratio 1:1.414)
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .aspectRatio(1f / 1.414f)
                                    .shadow(6.dp, RoundedCornerShape(4.dp))
                                    .background(Color.White, RoundedCornerShape(4.dp))
                                    .padding(8.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    for (r in 0 until rowsCount) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().weight(1f),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            for (c in 0 until columnsCount) {
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .fillMaxHeight()
                                                        .border(0.5.dp, Color(0xFF94A3B8), RoundedCornerShape(2.dp))
                                                        .background(Color(0xFFF1F5F9)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Column(
                                                        horizontalAlignment = Alignment.CenterHorizontally,
                                                        verticalArrangement = Arrangement.Center
                                                    ) {
                                                        Text(
                                                            text = selectedProfileName.take(6),
                                                            fontSize = 6.sp,
                                                            color = PrimaryNavy,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                        Text(
                                                            text = "PIN: ****${(r * columnsCount + c + 1) % 99}",
                                                            fontSize = 5.sp,
                                                            fontFamily = FontFamily.Monospace,
                                                            color = PrimaryBlue
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Grid Columns and Rows Controllers
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "عدد الأعمدة في الصفحة: $columnsCount", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    listOf(2, 3, 4).forEach { col ->
                                        FilterChip(
                                            selected = columnsCount == col,
                                            onClick = { columnsCount = col },
                                            label = { Text("$col أعمدة", fontSize = 11.sp) }
                                        )
                                    }
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "عدد الصفوف في الصفحة: $rowsCount", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    listOf(4, 5, 8, 10).forEach { r ->
                                        FilterChip(
                                            selected = rowsCount == r,
                                            onClick = { rowsCount = r },
                                            label = { Text("$r صفوف", fontSize = 11.sp) }
                                        )
                                    }
                                }
                            }
                        }

                        Text(
                            text = "إجمالي الكروت في كل ورقة A4: ${columnsCount * rowsCount} كرت",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue
                        )

                        // Export to PDF Button
                        Button(
                            onClick = {
                                scope.launch(Dispatchers.IO) {
                                    val pdfFile = generateA4Pdf(
                                        context = context,
                                        systemName = selectedSystem.title,
                                        profileName = selectedProfileName,
                                        headerTitle = networkHeaderTitle,
                                        priceTitle = profilePriceText,
                                        cols = columnsCount,
                                        rows = rowsCount
                                    )
                                    withContext(Dispatchers.Main) {
                                        if (pdfFile != null) {
                                            Toast.makeText(context, "تم توليد وتصدير ملف PDF بنجاح في مجلد التطبيق ✓", Toast.LENGTH_LONG).show()
                                            openOrSharePdf(context, pdfFile)
                                        } else {
                                            Toast.makeText(context, "فشل إنشاء ملف PDF", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("export_pdf_button")
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("توليد وتصدير ملف الطباعة PDF 🖨️", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

private fun generateA4Pdf(
    context: Context,
    systemName: String,
    profileName: String,
    headerTitle: String,
    priceTitle: String,
    cols: Int,
    rows: Int
): File? {
    try {
        val doc = PdfDocument()
        // Standard A4 size in 72 DPI points: 595 x 842
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = doc.startPage(pageInfo)
        val canvas = page.canvas

        val paint = Paint().apply {
            isAntiAlias = true
        }

        val margin = 20f
        val availableW = 595f - (2 * margin)
        val availableH = 842f - (2 * margin) - 40f // leave 40pt for page header

        val cardW = availableW / cols
        val cardH = availableH / rows

        // Page Header
        paint.color = android.graphics.Color.DKGRAY
        paint.textSize = 10f
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("TALAL PRO Network System - $systemName - باقة [$profileName]", 595f - margin, 18f, paint)

        val totalCards = cols * rows
        var cardIndex = 1

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val left = margin + (c * cardW) + 2
                val top = 30f + margin + (r * cardH) + 2
                val right = left + cardW - 4
                val bottom = top + cardH - 4

                // Draw Card Outer Frame
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 1f
                paint.color = android.graphics.Color.LTGRAY
                canvas.drawRect(left, top, right, bottom, paint)

                // Draw Card Header
                paint.style = Paint.Style.FILL
                paint.color = android.graphics.Color.rgb(15, 23, 42)
                paint.textSize = 8f
                paint.textAlign = Paint.Align.CENTER
                canvas.drawText(headerTitle, (left + right) / 2f, top + 14f, paint)

                // Draw Profile name
                paint.color = android.graphics.Color.rgb(2, 132, 199)
                paint.textSize = 7f
                canvas.drawText(profileName, (left + right) / 2f, top + 24f, paint)

                // Draw PIN Box
                val pinBoxTop = top + (cardH * 0.45f)
                paint.style = Paint.Style.FILL
                paint.color = android.graphics.Color.rgb(241, 245, 249)
                canvas.drawRect(left + 8, pinBoxTop, right - 8, pinBoxTop + 18, paint)

                paint.color = android.graphics.Color.BLACK
                paint.textSize = 9f
                paint.isFakeBoldText = true
                val dummyPin = "PIN: %08d".format(cardIndex * 7139 % 99999999)
                canvas.drawText(dummyPin, (left + right) / 2f, pinBoxTop + 13f, paint)
                paint.isFakeBoldText = false

                // Draw Footer Price
                paint.color = android.graphics.Color.GRAY
                paint.textSize = 6f
                canvas.drawText(priceTitle, (left + right) / 2f, bottom - 6f, paint)

                cardIndex++
            }
        }

        doc.finishPage(page)

        // Save into app documents folder
        val appFolder = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "TalalPro/Cards")
        if (!appFolder.exists()) {
            appFolder.mkdirs()
        }
        val file = File(appFolder, "TalalPro_Cards_${System.currentTimeMillis()}.pdf")
        val out = FileOutputStream(file)
        doc.writeTo(out)
        doc.close()
        out.close()
        return file
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}

private fun openOrSharePdf(context: Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(Intent.createChooser(intent, "عرض وطباعة ملف الكروت PDF"))
    } catch (e: Exception) {
        Toast.makeText(context, "تم حفظ الملف في: ${file.absolutePath}", Toast.LENGTH_LONG).show()
    }
}
