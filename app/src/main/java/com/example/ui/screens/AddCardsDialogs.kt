package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.*
import com.example.ui.TalalProViewModel
import com.example.ui.theme.PrimaryNavy
import com.example.ui.theme.TextSecondaryLight

val AddCardsMaroon = Color(0xFF6E1423)
val AddCardsMaroonSoft = Color(0xFFFDF2F4)
val AddCardsMaroonBorder = Color(0xFFE9C9CF)
val AddCardsEmerald = Color(0xFF10A35C)
val AddCardsSkyBg = Color(0xFFE0F2FE)
val AddCardsSkyText = Color(0xFF0284C7)

data class UnifiedPackageOption(
    val id: String,
    val name: String,
    val serverType: String, // "USER_MANAGER" or "HOTSPOT"
    val displayLabel: String,
    val priceLabel: String
)

fun buildUnifiedPackageOptions(
    umProfiles: List<UserManagerProfileEntity>,
    hsProfiles: List<HotspotProfileEntity>
): List<UnifiedPackageOption> {
    val umList = umProfiles.map { p ->
        val rawPrice = p.price.trim().filter { it.isDigit() || it == '.' }
        val priceLbl = if (rawPrice.isNotBlank() && rawPrice != "0") "$rawPrice RY" else "100 RY"
        val details = listOfNotNull(
            p.validity.takeIf { it.isNotBlank() },
            "${p.rateLimitRx}/${p.rateLimitTx}".takeIf { p.rateLimitRx.isNotBlank() }
        ).joinToString(" | ")
        val suffix = if (details.isNotBlank()) " ($details)" else ""
        UnifiedPackageOption(
            id = "UM_${p.id}_${p.name}",
            name = p.name,
            serverType = "USER_MANAGER",
            displayLabel = "${p.name}$suffix [UserManager] — $priceLbl",
            priceLabel = priceLbl
        )
    }

    val hsList = hsProfiles.map { p ->
        val digitsInName = Regex("(\\d{2,5})").find(p.name)?.value
        val priceLbl = if (!digitsInName.isNullOrBlank()) "$digitsInName RY" else "100 RY"
        val details = listOfNotNull(
            p.sessionTimeout.takeIf { it.isNotBlank() && it != "0s" },
            p.rateLimit.takeIf { it.isNotBlank() }
        ).joinToString(" | ")
        val suffix = if (details.isNotBlank()) " ($details)" else ""
        UnifiedPackageOption(
            id = "HS_${p.id}_${p.name}",
            name = p.name,
            serverType = "HOTSPOT",
            displayLabel = "${p.name}$suffix [Hotspot] — $priceLbl",
            priceLabel = priceLbl
        )
    }
    return umList + hsList
}

fun parseImportedCardLines(rawText: String): List<Pair<String, String>> {
    val pairs = mutableListOf<Pair<String, String>>()
    val seen = HashSet<String>()
    rawText.lineSequence().forEach { line ->
        val trimmed = line.trim()
        if (trimmed.isBlank()) return@forEach
        val lower = trimmed.lowercase()
        if (lower.startsWith("username") || lower.startsWith("user,") || lower.startsWith("#")) {
            return@forEach
        }
        val parts = when {
            trimmed.contains(",") -> trimmed.split(",", limit = 2)
            trimmed.contains(":") -> trimmed.split(":", limit = 2)
            trimmed.contains(";") -> trimmed.split(";", limit = 2)
            trimmed.contains("\t") -> trimmed.split("\t", limit = 2)
            else -> listOf(trimmed, trimmed)
        }
        val user = parts.getOrNull(0)?.trim()?.trim('"', '\'') ?: ""
        val pass = parts.getOrNull(1)?.trim()?.trim('"', '\'').takeUnless { it.isNullOrBlank() } ?: user
        if (user.isNotBlank() && seen.add(user)) {
            pairs.add(user to pass)
        }
    }
    return pairs
}

@Composable
fun ImportBatchTabContent(
    activeRouter: RouterEntity?,
    packages: List<UnifiedPackageOption>,
    templates: List<CardTemplateEntity>,
    selectedTemplate: CardTemplateEntity,
    isProcessing: Boolean,
    isSyncing: Boolean,
    onSyncAll: () -> Unit,
    onSelectTemplate: (CardTemplateEntity) -> Unit,
    onImportAndSave: (
        batchTitle: String,
        serverType: String,
        profileName: String,
        priceLabel: String,
        pairs: List<Pair<String, String>>,
        pushToRouter: Boolean
    ) -> Unit,
    onImportAndPrint: (
        batchTitle: String,
        serverType: String,
        profileName: String,
        priceLabel: String,
        pairs: List<Pair<String, String>>,
        pushToRouter: Boolean
    ) -> Unit
) {
    val context = LocalContext.current
    var batchTitle by remember { mutableStateOf("") }
    var rawCardsText by remember { mutableStateOf("") }
    var pushToRouter by remember { mutableStateOf(activeRouter != null) }
    var serverType by remember { mutableStateOf(packages.firstOrNull()?.serverType ?: "HOTSPOT") }
    var selectedPkgName by remember { mutableStateOf(packages.firstOrNull()?.name ?: "default") }
    var priceLabel by remember { mutableStateOf(packages.firstOrNull()?.priceLabel ?: "100 RY") }
    var pkgMenuExpanded by remember { mutableStateOf(false) }
    var tmplMenuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(packages) {
        if (packages.isNotEmpty() && packages.none { it.name == selectedPkgName }) {
            selectedPkgName = packages.first().name
            serverType = packages.first().serverType
            priceLabel = packages.first().priceLabel
        }
    }

    val parsedPairs = remember(rawCardsText) { parseImportedCardLines(rawCardsText) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
                if (content.isNotBlank()) {
                    rawCardsText = content
                    Toast.makeText(context, "تم قراءة ${parseImportedCardLines(content).size} كرت من الملف", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "تعذر قراءة الملف: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Card 1: File / Manual Import & Target Package
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.UploadFile,
                            contentDescription = "استيراد ملف كروت",
                            tint = AddCardsMaroon
                        )
                        Text(
                            text = "استيراد دفعة كروت (TXT / CSV)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = AddCardsMaroon
                        )
                    }

                    OutlinedButton(
                        onClick = onSyncAll,
                        enabled = !isSyncing,
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, AddCardsMaroonBorder),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = AddCardsMaroonSoft,
                            contentColor = AddCardsMaroon
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("import_sync_packages_btn")
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = AddCardsMaroon)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "مزامنة الباقات", modifier = Modifier.size(15.dp))
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("تحديث الباقات", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Target package picker
                Text("الباقة المرتبطة بالدفعة المستوردة", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryNavy)
                Box {
                    OutlinedCard(
                        onClick = { pkgMenuExpanded = true },
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("import_package_dropdown")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$selectedPkgName [${if (serverType == "USER_MANAGER") "UserManager" else "Hotspot"}] — $priceLabel",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PrimaryNavy,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "اختيار الباقة", tint = TextSecondaryLight)
                        }
                    }

                    DropdownMenu(
                        expanded = pkgMenuExpanded,
                        onDismissRequest = { pkgMenuExpanded = false }
                    ) {
                        if (packages.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("لا توجد باقات متزامنة — اضغط تحديث الباقات") },
                                onClick = { pkgMenuExpanded = false }
                            )
                        } else {
                            packages.forEach { pkg ->
                                DropdownMenuItem(
                                    text = { Text(pkg.displayLabel, fontSize = 12.sp) },
                                    onClick = {
                                        selectedPkgName = pkg.name
                                        serverType = pkg.serverType
                                        priceLabel = pkg.priceLabel
                                        pkgMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Batch title & price row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = batchTitle,
                        onValueChange = { batchTitle = it },
                        label = { Text("اسم الدفعة (اختياري)", fontSize = 11.sp) },
                        placeholder = { Text("IMP-2026", fontSize = 12.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("import_batch_name_input")
                    )
                    OutlinedTextField(
                        value = priceLabel,
                        onValueChange = { priceLabel = it },
                        label = { Text("السعر على الكرت", fontSize = 11.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(0.8f)
                            .testTag("import_price_input")
                    )
                }

                // Pick file button
                OutlinedButton(
                    onClick = { filePicker.launch(arrayOf("text/*", "application/csv", "*/*")) },
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.2.dp, AddCardsSkyText),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = AddCardsSkyBg.copy(alpha = 0.45f),
                        contentColor = AddCardsSkyText
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("import_pick_file_btn")
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = "اختيار ملف كروت")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("استيراد من ملف (.txt / .csv) عبر مستعرض الملفات", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedTextField(
                    value = rawCardsText,
                    onValueChange = { rawCardsText = it },
                    label = { Text("أو الصق أكواد الكروت هنا (كل كرت في سطر)", fontSize = 11.sp) },
                    placeholder = { Text("778215553\n778215554,1234\n778215555:pass", fontSize = 11.sp) },
                    minLines = 5,
                    maxLines = 8,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("import_cards_textarea")
                )

                // Parsed badge + push to router switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (parsedPairs.isNotEmpty()) Color(0xFFDCFCE7) else Color(0xFFF1F5F9)
                    ) {
                        Text(
                            text = "الكروت الجاهزة للاستيراد: ${parsedPairs.size} كرت",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (parsedPairs.isNotEmpty()) Color(0xFF15803D) else TextSecondaryLight,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("رفع للراوتر", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = PrimaryNavy)
                        Switch(
                            checked = pushToRouter,
                            onCheckedChange = { pushToRouter = it },
                            modifier = Modifier.testTag("import_push_router_switch")
                        )
                    }
                }

                // Template selector for imported batch printing
                Text("قالب الطباعة المستخدم للدفعة", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryNavy)
                Box {
                    OutlinedCard(
                        onClick = { tmplMenuExpanded = true },
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("import_template_dropdown")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${selectedTemplate.templateName} (${selectedTemplate.rowsPerPage}×${selectedTemplate.columnsPerPage} = ${selectedTemplate.rowsPerPage * selectedTemplate.columnsPerPage} كرت/صفحة)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PrimaryNavy,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "اختيار القالب", tint = TextSecondaryLight)
                        }
                    }

                    DropdownMenu(
                        expanded = tmplMenuExpanded,
                        onDismissRequest = { tmplMenuExpanded = false }
                    ) {
                        templates.forEach { t ->
                            DropdownMenuItem(
                                text = { Text("${t.templateName} (${t.rowsPerPage * t.columnsPerPage} كرت/صفحة)") },
                                onClick = {
                                    onSelectTemplate(t)
                                    tmplMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        Button(
            onClick = {
                onImportAndSave(batchTitle, serverType, selectedPkgName, priceLabel, parsedPairs, pushToRouter)
            },
            enabled = parsedPairs.isNotEmpty() && !isProcessing,
            colors = ButtonDefaults.buttonColors(containerColor = AddCardsMaroon),
            shape = RoundedCornerShape(26.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .testTag("import_save_only_btn")
        ) {
            if (isProcessing) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
            }
            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("استيراد وحفظ الدفعة (${parsedPairs.size} كرت)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        Button(
            onClick = {
                onImportAndPrint(batchTitle, serverType, selectedPkgName, priceLabel, parsedPairs, pushToRouter)
            },
            enabled = parsedPairs.isNotEmpty() && !isProcessing,
            colors = ButtonDefaults.buttonColors(containerColor = AddCardsEmerald),
            shape = RoundedCornerShape(26.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .testTag("import_and_print_btn")
        ) {
            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("استيراد وطباعة الدفعة PDF فوراً", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

@Composable
fun BatchCardsViewerDialog(
    batch: CardBatchEntity,
    items: List<CardBatchItemEntity>,
    onDismiss: () -> Unit,
    onPrintPdf: () -> Unit,
    onToggleSingleCard: (CardBatchItemEntity, Boolean) -> Unit,
    onDeleteSingleCard: (CardBatchItemEntity) -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var query by remember { mutableStateOf("") }

    val filtered = remember(items, query) {
        if (query.isBlank()) items else items.filter {
            it.username.contains(query, ignoreCase = true) ||
                it.serial.contains(query, ignoreCase = true)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp)
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
                        Text(
                            text = "كروت الدفعة ${batch.batchCode}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = AddCardsMaroon
                        )
                        Text(
                            text = "${batch.profileName} • ${if (batch.serverType == "USER_MANAGER") "User Manager" else "Hotspot"} • ${items.size} كرت",
                            fontSize = 11.sp,
                            color = TextSecondaryLight
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق نافذة الكروت")
                    }
                }

                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("ابحث برقم الكرت أو التسلسل...", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("batch_viewer_search_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onPrintPdf,
                        colors = ButtonDefaults.buttonColors(containerColor = AddCardsEmerald),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("batch_viewer_print_btn")
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("طباعة الدفعة PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (filtered.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("لا توجد كروت مطابقة للبحث", fontSize = 12.sp, color = TextSecondaryLight)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filtered, key = { it.id }) { card ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (card.isDisabled) Color(0xFFFEF2F2) else Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, if (card.isDisabled) Color(0xFFFCA5A5) else Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = card.username,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            color = if (card.isDisabled) Color(0xFF991B1B) else PrimaryNavy
                                        )
                                        Text(
                                            text = "التسلسل: ${card.serial} • ${if (card.isDisabled) "موقوف" else "فعال"}",
                                            fontSize = 10.sp,
                                            color = TextSecondaryLight
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = {
                                                clipboard.setText(AnnotatedString(card.username))
                                                Toast.makeText(context, "تم نسخ الكرت ${card.username}", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.minimumInteractiveComponentSize()
                                        ) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = "نسخ الكرت", tint = AddCardsSkyText, modifier = Modifier.size(18.dp))
                                        }
                                        IconButton(
                                            onClick = { onToggleSingleCard(card, card.isDisabled) },
                                            modifier = Modifier.minimumInteractiveComponentSize()
                                        ) {
                                            Icon(
                                                imageVector = if (card.isDisabled) Icons.Default.PlayArrow else Icons.Default.Pause,
                                                contentDescription = if (card.isDisabled) "تفعيل الكرت" else "إيقاف الكرت",
                                                tint = if (card.isDisabled) AddCardsEmerald else Color(0xFFD97706),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = { onDeleteSingleCard(card) },
                                            modifier = Modifier.minimumInteractiveComponentSize()
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "حذف الكرت", tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
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
fun EditBatchDialog(
    batch: CardBatchEntity,
    packages: List<UnifiedPackageOption>,
    templates: List<CardTemplateEntity>,
    onDismiss: () -> Unit,
    onSave: (
        newBatchCode: String,
        newProfile: String,
        newPriceLabel: String,
        newTemplate: CardTemplateEntity?
    ) -> Unit
) {
    var batchCode by remember { mutableStateOf(batch.batchCode) }
    var profileName by remember { mutableStateOf(batch.profileName) }
    var priceLabel by remember { mutableStateOf(batch.priceLabel) }
    var selectedTmpl by remember {
        mutableStateOf(templates.firstOrNull { it.id == batch.templateId } ?: templates.firstOrNull())
    }
    var profExpanded by remember { mutableStateOf(false) }
    var tmplExpanded by remember { mutableStateOf(false) }

    val matchingPackages = remember(packages, batch.serverType) {
        val exact = packages.filter { it.serverType == batch.serverType }
        exact.ifEmpty { packages }
    }

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
                        text = "تعديل بيانات الدفعة",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = AddCardsMaroon
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                OutlinedTextField(
                    value = batchCode,
                    onValueChange = { batchCode = it },
                    label = { Text("رمز / اسم الدفعة") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("الباقة (يتم التحديث على الراوتر أيضاً إذا كانت مرفوعة)", fontSize = 11.sp, color = TextSecondaryLight)
                Box {
                    OutlinedCard(
                        onClick = { profExpanded = true },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(profileName, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = PrimaryNavy)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    }
                    DropdownMenu(expanded = profExpanded, onDismissRequest = { profExpanded = false }) {
                        matchingPackages.forEach { pkg ->
                            DropdownMenuItem(
                                text = { Text(pkg.displayLabel, fontSize = 12.sp) },
                                onClick = {
                                    profileName = pkg.name
                                    priceLabel = pkg.priceLabel
                                    profExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = priceLabel,
                    onValueChange = { priceLabel = it },
                    label = { Text("سعر الفئة (RY)") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("قالب الطباعة المرتبط", fontSize = 11.sp, color = TextSecondaryLight)
                Box {
                    OutlinedCard(
                        onClick = { tmplExpanded = true },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(selectedTmpl?.templateName ?: batch.templateName, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    }
                    DropdownMenu(expanded = tmplExpanded, onDismissRequest = { tmplExpanded = false }) {
                        templates.forEach { t ->
                            DropdownMenuItem(
                                text = { Text("${t.templateName} (${t.rowsPerPage}×${t.columnsPerPage})") },
                                onClick = {
                                    selectedTmpl = t
                                    tmplExpanded = false
                                }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                    ) {
                        Text("إلغاء")
                    }
                    Button(
                        onClick = { onSave(batchCode, profileName, priceLabel, selectedTmpl) },
                        colors = ButtonDefaults.buttonColors(containerColor = AddCardsMaroon),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                    ) {
                        Text("حفظ التعديلات", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun DeleteBatchConfirmDialog(
    batch: CardBatchEntity,
    onDismiss: () -> Unit,
    onDelete: (alsoFromRouter: Boolean) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFEE2E2))
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(26.dp))
                }
                Text(
                    text = "حذف الدفعة [${batch.batchCode}]",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryNavy
                )
                Text(
                    text = "تحتوي هذه الدفعة على ${batch.cardsCount} كرت (${batch.profileName}). اختر نوع الحذف:",
                    fontSize = 12.sp,
                    color = TextSecondaryLight
                )

                if (batch.isSyncedToRouter) {
                    Button(
                        onClick = { onDelete(true) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("delete_batch_router_and_app_btn")
                    ) {
                        Text("حذف من التطبيق والراوتر نهائياً", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                OutlinedButton(
                    onClick = { onDelete(false) },
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFFDC2626)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("delete_batch_app_only_btn")
                ) {
                    Text("حذف من سجل التطبيق فقط", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text("تراجع", color = TextSecondaryLight)
                }
            }
        }
    }
}
