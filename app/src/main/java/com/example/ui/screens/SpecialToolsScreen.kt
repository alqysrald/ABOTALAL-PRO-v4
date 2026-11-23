package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RouterEntity
import com.example.ui.TalalProViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpecialToolsScreen(
    viewModel: TalalProViewModel,
    modifier: Modifier = Modifier
) {
    val routers by viewModel.allRouters.collectAsState()
    val selectedRouter by viewModel.selectedToolsRouter.collectAsState()
    val isEnablingCloud by viewModel.isEnablingCloud.collectAsState()
    val cloudResult by viewModel.cloudOperationResult.collectAsState()
    val clipboardManager = LocalClipboardManager.current

    var activeRouter by remember(routers, selectedRouter) {
        mutableStateOf(selectedRouter ?: routers.firstOrNull())
    }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: VLANs, 1: Log, 2: General Tools

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "الأدوات المميزة (Tools)",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimaryLight
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF6366F1).copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "VLANs • Log ⚡",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4F46E5),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "إدارة الفيلانات، سجل المشاكل والأحداث بالعربية، وتفعيل الكلاود",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondaryLight
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF8FAFC),
        modifier = modifier
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Router Selector Card
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
                            Text("الراوتر المستهدف للتطبيق:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimaryLight)

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
                                            text = activeRouter?.name ?: "اختر راوتر...",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryNavy
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TextMutedLight)
                                    }
                                }

                                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                    routers.forEach { r ->
                                        DropdownMenuItem(
                                            text = { Text(r.name) },
                                            onClick = {
                                                activeRouter = r
                                                viewModel.selectedToolsRouter.value = r
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // TOOL TABS: VLANs, Log & General Tools
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        label = {
                            Text(
                                "🌐 الفيلانات (VLANs)",
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF4F46E5),
                            selectedLabelColor = Color.White,
                            containerColor = Color.White,
                            labelColor = PrimaryNavy
                        ),
                        modifier = Modifier.testTag("tools_tab_vlans")
                    )

                    FilterChip(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        label = {
                            Text(
                                "📋 السجل والمشاكل (Log)",
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF0F172A),
                            selectedLabelColor = Color.White,
                            containerColor = Color.White,
                            labelColor = PrimaryNavy
                        ),
                        modifier = Modifier.testTag("tools_tab_log")
                    )

                    FilterChip(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        label = {
                            Text(
                                "☁️ الكلاود والصيانة",
                                fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryBlue,
                            selectedLabelColor = Color.White,
                            containerColor = Color.White,
                            labelColor = PrimaryNavy
                        ),
                        modifier = Modifier.testTag("tools_tab_cloud")
                    )
                }
            }

            // TAB 0: VLAN MANAGEMENT SECTION
            if (selectedTab == 0) {
                item {
                    VlanManagementSection(
                        viewModel = viewModel,
                        router = activeRouter
                    )
                }
            }

            // TAB 1: SYSTEM & ROUTER LOG SECTION (IN ARABIC)
            if (selectedTab == 1) {
                item {
                    LogManagementSection(
                        viewModel = viewModel,
                        router = activeRouter
                    )
                }
            }

            // TAB 2: GENERAL TOOLS (CLOUD DDNS, BACKUP, REBOOT)
            if (selectedTab == 2) {
                // TOOL 1: ONE-CLICK IP CLOUD DDNS
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = PrimaryBlue.copy(alpha = 0.12f),
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.CloudSync, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(24.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "الأداة الأولى: تفعيل الكلاود في المايكروتك بنقرة واحدة",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = PrimaryNavy
                                )
                                Text(
                                    text = "تفعيل خدمة IP Cloud DDNS وتحديث الدومين وربطه بالتطبيق مباشرة",
                                    fontSize = 11.sp,
                                    color = TextSecondaryLight
                                )
                            }
                        }

                        HorizontalDivider(color = BorderLight.copy(alpha = 0.6f))

                        activeRouter?.let { router ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFF1F5F9),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("حالة الكلاود في الراوتر:", fontSize = 12.sp, color = TextSecondaryLight)
                                        Text(
                                            text = if (router.cloudStatus == "updated" || router.cloudStatus == "yes") "مفعل ومحدث ✓" else "غير مفعل ⚠️",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = if (router.cloudStatus == "updated" || router.cloudStatus == "yes") Color(0xFF10B981) else Color(0xFFF59E0B)
                                        )
                                    }

                                    if (router.cloudDdnsName.isNotBlank()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("عنوان الدومين (DNS Name):", fontSize = 11.sp, color = TextSecondaryLight)
                                                Text(router.cloudDdnsName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryNavy)
                                            }
                                            IconButton(
                                                onClick = {
                                                    clipboardManager.setText(AnnotatedString(router.cloudDdnsName))
                                                    viewModel.snackbarMessage.value = "تم نسخ عنوان الكلاود إلى الحافظة"
                                                }
                                            ) {
                                                Icon(Icons.Default.ContentCopy, contentDescription = "نسخ", tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                }
                            }

                            Button(
                                onClick = { viewModel.enableCloudDdns(router) },
                                enabled = !isEnablingCloud,
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                if (isEnablingCloud) {
                                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("جاري تفعيل وتحديث الكلاود في الراوتر...")
                                } else {
                                    Icon(Icons.Default.CloudDone, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("تفعيل الكلاود في الراوتر الآن (Enable Cloud)", fontWeight = FontWeight.Bold)
                                }
                            }

                            cloudResult?.let { msg ->
                                Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFDCFCE7), modifier = Modifier.fillMaxWidth()) {
                                    Text(text = msg, color = Color(0xFF047857), fontSize = 11.sp, modifier = Modifier.padding(10.dp))
                                }
                            }
                        } ?: Text("يرجى اختيار راوتر للبدء", color = TextSecondaryLight, fontSize = 12.sp)
                    }
                }
            }

            // TOOL 2: QUICK BACKUP
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF10B981).copy(alpha = 0.12f),
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Backup, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(24.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "الأداة الثانية: النسخ الاحتياطي السريع (Backup)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = PrimaryNavy
                                )
                                Text(
                                    text = "حفظ نسخة احتياطية كاملة من إعدادات الراوتر وملفات التكوين",
                                    fontSize = 11.sp,
                                    color = TextSecondaryLight
                                )
                            }
                        }

                        Button(
                            onClick = {
                                activeRouter?.let { r ->
                                    val backupName = "TALAL_PRO_${r.name.replace(" ", "_")}_${System.currentTimeMillis()}"
                                    viewModel.backupRouter(r, backupName)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().height(46.dp)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("أخذ نسخة احتياطية فورية من الراوتر", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // TOOL 3: REBOOT ROUTER
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFEF4444).copy(alpha = 0.12f),
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.RestartAlt, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(24.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "الأداة الثالثة: إعادة تشغيل الراوتر (Reboot)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = PrimaryNavy
                                )
                                Text(
                                    text = "إرسال أمر إعادة تشغيل فوري للراوتر المايكروتيك بأمان",
                                    fontSize = 11.sp,
                                    color = TextSecondaryLight
                                )
                            }
                        }

                        Button(
                            onClick = {
                                activeRouter?.let { r -> viewModel.rebootRouter(r) }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().height(46.dp)
                        ) {
                            Icon(Icons.Default.RestartAlt, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("إعادة تشغيل الراوتر الآن", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
}
