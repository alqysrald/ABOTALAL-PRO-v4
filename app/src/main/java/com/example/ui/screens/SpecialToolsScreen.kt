package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.ScreenTab
import com.example.ui.TalalProViewModel

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

    val activeRouter = selectedRouter ?: routers.firstOrNull()

    LaunchedEffect(routers, selectedRouter) {
        if (selectedRouter == null && routers.isNotEmpty()) {
            viewModel.selectedToolsRouter.value = routers.first()
        }
    }

    // Expandable inline sections (Zero horizontal tabs as requested!)
    var showPackagesSection by remember { mutableStateOf(false) }
    var showVlanSection by remember { mutableStateOf(false) }
    var showLogSection by remember { mutableStateOf(false) }

    // Interactive Tool Dialogs
    var activeQuerySpec by remember { mutableStateOf<SpecialToolQuerySpec?>(null) }
    var showPppoeDialog by remember { mutableStateOf(false) }
    var showHtmlDirDialog by remember { mutableStateOf(false) }
    var showFreeNetDialog by remember { mutableStateOf(false) }
    var showBluetoothBlockDialog by remember { mutableStateOf(false) }
    var showPingDialog by remember { mutableStateOf(false) }
    var addressListSpec by remember { mutableStateOf<Triple<String, String, Color>?>(null) }
    var confirmReboot by remember { mutableStateOf(false) }
    var confirmShutdown by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0B132B))
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Luxury Header & Target Router Picker
            item {
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
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "الأدوات المميزة الشاملة",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                                Text(
                                    text = "التحكم الكامل بالباقات، الفيلانات، الجدار الناري، التشخيص، والشبكة",
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF6366F1).copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, Color(0xFF818CF8).copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = "Pro Suite ⚡",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFA5B4FC),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        var routerMenuOpen by remember { mutableStateOf(false) }
                        Box {
                            Surface(
                                onClick = { routerMenuOpen = true },
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF1E293B),
                                border = BorderStroke(1.dp, Color(0xFF334155)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("tools_router_picker")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .size(9.dp)
                                                .clip(CircleShape)
                                                .background(if (activeRouter?.isOnline == true) Color(0xFF10B981) else Color(0xFFF59E0B))
                                        )
                                        Text(
                                            text = activeRouter?.let { "الراوتر: ${it.name} (${it.host})" } ?: "اختر راوتر للتنفيذ...",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color(0xFF38BDF8))
                                }
                            }
                            DropdownMenu(
                                expanded = routerMenuOpen,
                                onDismissRequest = { routerMenuOpen = false },
                                modifier = Modifier.background(Color(0xFF1E293B))
                            ) {
                                routers.forEach { r ->
                                    DropdownMenuItem(
                                        text = { Text("${r.name} (${r.host})", color = Color.White) },
                                        onClick = {
                                            viewModel.selectedToolsRouter.value = r
                                            routerMenuOpen = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. إدارة الباقات (يوزرمانجر & هوتسبوت) — من لقطة الشاشة المرفقة
            item {
                ExpandableInlineCard(
                    title = "إدارة الباقات الشاملة (يوزرمانجر و هوتسبوت)",
                    subtitle = "إضافة، تعديل، تحديث، استيراد، وحذف الباقات مباشرة من الراوتر",
                    icon = Icons.Default.TableView,
                    accentColor = Color(0xFF06B6D4),
                    expanded = showPackagesSection,
                    onToggle = { showPackagesSection = !showPackagesSection },
                    testTag = "tools_expand_packages"
                ) {
                    PackageManagementContent(
                        viewModel = viewModel,
                        router = activeRouter,
                        initialMode = "USER_MANAGER"
                    )
                }
            }

            // 3. إدارة الفيلانات (VLANs)
            item {
                ExpandableInlineCard(
                    title = "إدارة الفيلانات المتكاملة (VLANs)",
                    subtitle = "عرض، إضافة، تعديل، وتعيين عناوين IP للفيلانات",
                    icon = Icons.Default.Lan,
                    accentColor = Color(0xFF818CF8),
                    expanded = showVlanSection,
                    onToggle = { showVlanSection = !showVlanSection },
                    testTag = "tools_expand_vlans"
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(modifier = Modifier.heightIn(max = 520.dp)) {
                            VlanManagementSection(viewModel = viewModel, router = activeRouter)
                        }
                    }
                }
            }

            // 4. سجل النظام والمشاكل بالعربية (Log)
            item {
                ExpandableInlineCard(
                    title = "سجل النظام والمشاكل بالعربية (Log)",
                    subtitle = "ترجمة ومراقبة جميع تنبيهات الراوتر وسجلات التطبيق",
                    icon = Icons.Default.Article,
                    accentColor = Color(0xFF10B981),
                    expanded = showLogSection,
                    onToggle = { showLogSection = !showLogSection },
                    testTag = "tools_expand_log"
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(modifier = Modifier.heightIn(max = 520.dp)) {
                            LogManagementSection(viewModel = viewModel, router = activeRouter)
                        }
                    }
                }
            }

            // 5. المميزات وإدارة البروبانـد والربط السريع (6 بطاقات من الصورة 620e5d90)
            item {
                ToolsGroupCard(
                    title = "الربط والمميزات المتقدمة السريعة",
                    count = 6,
                    accentColor = Color(0xFF38BDF8),
                    icon = Icons.Default.Bolt
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        FeatureRowBannerCard(
                            title = "حسابات البروبانـد (PPPoE)",
                            subtitle = "إدارة مستخدمي PPPoE وحسابات البروبانـد",
                            buttonLabel = "دخول للإدارة",
                            icon = Icons.Default.Assessment,
                            accentColor = Color(0xFFF59E0B),
                            onClick = { showPppoeDialog = true }
                        )
                        FeatureRowBannerCard(
                            title = "تغيير مجلد HTML للـ Hotspot",
                            subtitle = "تغيير مسار صفحة تسجيل الدخول للعملاء",
                            buttonLabel = "تغيير المجلد",
                            icon = Icons.Default.FolderOpen,
                            accentColor = Color(0xFF10B981),
                            onClick = { showHtmlDirDialog = true }
                        )
                        FeatureRowBannerCard(
                            title = "تفعيل الشبكة مجاناً (Free)",
                            subtitle = "تشغيل / إيقاف بوابة الكروت وفتح الشبكة مجاناً",
                            buttonLabel = "تحكم الآن",
                            icon = Icons.Default.Wifi,
                            accentColor = Color(0xFF8B5CF6),
                            onClick = { showFreeNetDialog = true }
                        )
                        FeatureRowBannerCard(
                            title = "إيقاف مشاركة الإنترنت (بلوتوث)",
                            subtitle = "تفعيل / إلغاء منع المشاركة عبر الجوال (TTL=1)",
                            buttonLabel = "تطبيق TTL=1",
                            icon = Icons.Default.BluetoothDisabled,
                            accentColor = Color(0xFFEF4444),
                            onClick = { showBluetoothBlockDialog = true }
                        )
                        FeatureRowBannerCard(
                            title = "عرض المنافذ واستهلاك الإنترنت",
                            subtitle = "مراقبة حركة المرور واستخدام البيانات لكل منفذ",
                            buttonLabel = "عرض المنافذ",
                            icon = Icons.Default.Speed,
                            accentColor = Color(0xFF0284C7),
                            onClick = { viewModel.setTab(ScreenTab.PORTS_AND_DEVICES) }
                        )

                        // Telegram Integration Banner
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF0B132B),
                            border = BorderStroke(1.dp, Color(0xFF0EA5E9).copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFF0EA5E9).copy(alpha = 0.2f))
                                    ) {
                                        Icon(Icons.Default.Send, contentDescription = null, tint = Color(0xFF38BDF8))
                                    }
                                    Column {
                                        Text("ربط وإشعارات التيليجرام", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                                        Text("استقبل تنبيهات المبيعات وحالة السيرفر مباشرة", fontSize = 11.sp, color = Color(0xFF94A3B8))
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { viewModel.setTab(ScreenTab.TELEGRAM) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                                    ) {
                                        Text("ضبط البوت", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                    Button(
                                        onClick = { viewModel.setTab(ScreenTab.ADVANCED_TELEGRAM) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                                    ) {
                                        Text("تخصيص المميزات", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 6. مراقبة الأداء (6) — من الصورة 2191b463
            item {
                val items = listOf(
                    ToolTileSpec("CPU / ذاكرة", Icons.Default.Memory, Color(0xFFF59E0B)) {
                        activeQuerySpec = SpecialToolQuerySpec("المعالج والذاكرة (CPU / RAM)", "موارد النظام الفورية", "/system/resource/print", Color(0xFFF59E0B))
                    },
                    ToolTileSpec("مراقبة المرور", Icons.Default.Traffic, Color(0xFF38BDF8)) {
                        activeQuerySpec = SpecialToolQuerySpec("مراقبة حركة المرور والمنافذ", "حالة المنافذ والترافيك", "/interface/print", Color(0xFF38BDF8))
                    },
                    ToolTileSpec("RouterBOARD", Icons.Default.DeveloperBoard, Color(0xFF60A5FA)) {
                        activeQuerySpec = SpecialToolQuerySpec("معلومات عتاد RouterBOARD", "الموديل والسيريال والفيرموير", "/system/routerboard/print", Color(0xFF60A5FA))
                    },
                    ToolTileSpec("اتصالات نشطة", Icons.Default.Link, Color(0xFFA855F7)) {
                        activeQuerySpec = SpecialToolQuerySpec("الاتصالات النشطة حالياً", "Active Hotspot & Connections", "/ip/hotspot/active/print", Color(0xFFA855F7))
                    },
                    ToolTileSpec("صحة الجهاز", Icons.Default.HealthAndSafety, Color(0xFF14B8A6)) {
                        activeQuerySpec = SpecialToolQuerySpec("صحة الراوتر والحرارة والجهد", "درجة الحرارة والفولتية", "/system/health/print", Color(0xFF14B8A6))
                    },
                    ToolTileSpec("الموارد", Icons.Default.SettingsInputComponent, Color(0xFF10B981)) {
                        activeQuerySpec = SpecialToolQuerySpec("تقرير الموارد والتخزين", "الأقراص ومدة التشغيل", "/system/resource/print", Color(0xFF10B981))
                    }
                )
                ToolsGroupCard(title = "مراقبة الأداء", count = 6, accentColor = Color(0xFF38BDF8), icon = Icons.Default.BarChart) {
                    TwoColumnToolGrid(items)
                }
            }

            // 7. أدوات التشخيص (10) — من الصورة 7d98f614
            item {
                val items = listOf(
                    ToolTileSpec("اختبار النطاق", Icons.Default.Speed, Color(0xFFF59E0B)) {
                        activeQuerySpec = SpecialToolQuerySpec("منافذ وسرعات النطاق", "Interface Traffic Stats", "/interface/print stats", Color(0xFFF59E0B))
                    },
                    ToolTileSpec("Ping", Icons.Default.NetworkPing, Color(0xFF38BDF8)) {
                        showPingDialog = true
                    },
                    ToolTileSpec("تنقيط الحزم", Icons.Default.TrackChanges, Color(0xFFEC4899)) {
                        activeQuerySpec = SpecialToolQuerySpec("طوابير الحزم (Queues)", "Packet & Rate Control", "/queue/simple/print", Color(0xFFEC4899))
                    },
                    ToolTileSpec("Torch", Icons.Default.FlashOn, Color(0xFF8B5CF6)) {
                        activeQuerySpec = SpecialToolQuerySpec("مراقب الاتصال الحية (Connections)", "Live Firewall Connections", "/ip/firewall/connection/print", Color(0xFF8B5CF6))
                    },
                    ToolTileSpec("مراقبة الشبكة", Icons.Default.Visibility, Color(0xFFEF4444)) {
                        activeQuerySpec = SpecialToolQuerySpec("أداة Netwatch لمراقبة العقد", "فحص استجابة الخوادم", "/tool/netwatch/print", Color(0xFFEF4444))
                    },
                    ToolTileSpec("الرسوم البيانية", Icons.Default.Timeline, Color(0xFF06B6D4)) {
                        activeQuerySpec = SpecialToolQuerySpec("الرسوم البيانية لحركة الشبكة", "Tool Graphing Interfaces", "/tool/graphing/interface/print", Color(0xFF06B6D4))
                    },
                    ToolTileSpec("بريد إلكتروني", Icons.Default.Email, Color(0xFF94A3B8)) {
                        activeQuerySpec = SpecialToolQuerySpec("إعدادات البريد الإلكتروني (E-mail)", "SMTP Router Configuration", "/tool/e-mail/print", Color(0xFF94A3B8))
                    },
                    ToolTileSpec("أجهزة البث (Neighbor)", Icons.Default.Sensors, Color(0xFFF87171)) {
                        activeQuerySpec = SpecialToolQuerySpec("الأجهزة المجاورة والمبثوثة (MNDP/CDP)", "Discovered Neighbors", "/ip/neighbor/print", Color(0xFFF87171))
                    },
                    ToolTileSpec("SMS", Icons.Default.Sms, Color(0xFFA855F7)) {
                        activeQuerySpec = SpecialToolQuerySpec("رسائل وإعدادات SMS", "RouterOS SMS Tool", "/tool/sms/print", Color(0xFFA855F7))
                    },
                    ToolTileSpec("Fetch", Icons.Default.DownloadForOffline, Color(0xFF10B981)) {
                        activeQuerySpec = SpecialToolQuerySpec("جلب الملفات والأوامر (Fetch & Files)", "قائمة ملفات الراوتر", "/file/print", Color(0xFF10B981))
                    }
                )
                ToolsGroupCard(title = "أدوات التشخيص (Tools)", count = 10, accentColor = Color(0xFF60A5FA), icon = Icons.Default.Build) {
                    TwoColumnToolGrid(items)
                }
            }

            // 8. جدار الحماية (6) — من الصورة 9617dd44
            item {
                val items = listOf(
                    ToolTileSpec("قواعد NAT", Icons.Default.CompareArrows, Color(0xFFF59E0B)) {
                        activeQuerySpec = SpecialToolQuerySpec("قواعد NAT والتوجيه", "/ip/firewall/nat", "/ip/firewall/nat/print", Color(0xFFF59E0B))
                    },
                    ToolTileSpec("قواعد التصفية", Icons.Default.FilterList, Color(0xFFEF4444)) {
                        activeQuerySpec = SpecialToolQuerySpec("قواعد التصفية (Filter Rules)", "/ip/firewall/filter", "/ip/firewall/filter/print", Color(0xFFEF4444))
                    },
                    ToolTileSpec("المواقع المحجوبة", Icons.Default.WebAssetOff, Color(0xFF60A5FA)) {
                        activeQuerySpec = SpecialToolQuerySpec("المواقع والنطاقات المحجوبة (DNS Static)", "/ip/dns/static", "/ip/dns/static/print", Color(0xFF60A5FA))
                    },
                    ToolTileSpec("قواعد Mangle", Icons.Default.Code, Color(0xFF10B981)) {
                        activeQuerySpec = SpecialToolQuerySpec("قواعد Mangle وتعديل الحزم", "/ip/firewall/mangle", "/ip/firewall/mangle/print", Color(0xFF10B981))
                    },
                    ToolTileSpec("الاتصالات النشطة", Icons.Default.Link, Color(0xFF06B6D4)) {
                        activeQuerySpec = SpecialToolQuerySpec("تتبع الاتصالات (Firewall Connections)", "Top Connections", "/ip/firewall/connection/print", Color(0xFF06B6D4))
                    },
                    ToolTileSpec("بروتوكولات Layer7", Icons.Default.Layers, Color(0xFFA855F7)) {
                        activeQuerySpec = SpecialToolQuerySpec("تصفية التطبيقات (Layer7 Protocol)", "/ip/firewall/layer7-protocol", "/ip/firewall/layer7-protocol/print", Color(0xFFA855F7))
                    }
                )
                ToolsGroupCard(title = "جدار الحماية (Firewall)", count = 6, accentColor = Color(0xFFEF4444), icon = Icons.Default.Security) {
                    TwoColumnToolGrid(items)
                }
            }

            // 9. قوائم الحظر والعبور (2) — من الصورة 9617dd44
            item {
                val items = listOf(
                    ToolTileSpec("القائمة البيضاء (Whitelist)", Icons.Default.Verified, Color(0xFF10B981)) {
                        addressListSpec = Triple("whitelist", "إدارة القائمة البيضاء (Whitelist)", Color(0xFF10B981))
                    },
                    ToolTileSpec("القائمة السوداء (Blacklist)", Icons.Default.Block, Color(0xFFEF4444)) {
                        addressListSpec = Triple("blacklist", "إدارة القائمة السوداء (Blacklist)", Color(0xFFEF4444))
                    }
                )
                ToolsGroupCard(title = "القوائم الحظر والعبور", count = 2, accentColor = Color(0xFF8B5CF6), icon = Icons.Default.ListAlt) {
                    TwoColumnToolGrid(items)
                }
            }

            // 10. إدارة النطاق الترددي Queue (1) — من الصورة 9617dd44
            item {
                val items = listOf(
                    ToolTileSpec("أسرع في استهلاك البيانات (Queues)", Icons.Default.Timer, Color(0xFF38BDF8)) {
                        activeQuerySpec = SpecialToolQuerySpec("تحديد السرعات (Simple Queues)", "مراقبة الاستهلاك والسرعات", "/queue/simple/print", Color(0xFF38BDF8))
                    }
                )
                ToolsGroupCard(title = "إدارة النطاق الترددي (Queue)", count = 1, accentColor = Color(0xFFF59E0B), icon = Icons.Default.Speed) {
                    TwoColumnToolGrid(items)
                }
            }

            // 11. إدارة النظام (System - 4) — من الصورة 3075f9d5
            item {
                val items = listOf(
                    ToolTileSpec("سجلات النظام", Icons.Default.History, Color(0xFF94A3B8)) {
                        showLogSection = true
                        viewModel.loadRouterLogs(activeRouter)
                    },
                    ToolTileSpec("المستخدمين والصلاحيات", Icons.Default.ManageAccounts, Color(0xFF60A5FA)) {
                        activeQuerySpec = SpecialToolQuerySpec("مستخدمو وصلاحيات النظام", "/user/print", "/user/print", Color(0xFF60A5FA))
                    },
                    ToolTileSpec("إعادة تشغيل", Icons.Default.RestartAlt, Color(0xFFEF4444)) {
                        confirmReboot = true
                    },
                    ToolTileSpec("نسخ احتياطي", Icons.Default.CloudUpload, Color(0xFF10B981)) {
                        activeRouter?.let { r ->
                            viewModel.backupRouter(r, "TALAL_PRO_${System.currentTimeMillis()}")
                        }
                    }
                )
                ToolsGroupCard(title = "إدارة النظام (System)", count = 4, accentColor = Color(0xFFF59E0B), icon = Icons.Default.Settings) {
                    TwoColumnToolGrid(items)
                }
            }

            // 12. إعدادات الشبكة (IP - 9) — من الصورة 3075f9d5
            item {
                val items = listOf(
                    ToolTileSpec("خادم DHCP", Icons.Default.Dns, Color(0xFFF59E0B)) {
                        activeQuerySpec = SpecialToolQuerySpec("خوادم DHCP Server", "/ip/dhcp-server/print", "/ip/dhcp-server/print", Color(0xFFF59E0B))
                    },
                    ToolTileSpec("IP Addresses", Icons.Default.Router, Color(0xFF10B981)) {
                        activeQuerySpec = SpecialToolQuerySpec("عناوين الآيبي على المنافذ", "/ip/address/print", "/ip/address/print", Color(0xFF10B981))
                    },
                    ToolTileSpec("DHCP Leases", Icons.Default.FormatListBulleted, Color(0xFFEC4899)) {
                        activeQuerySpec = SpecialToolQuerySpec("الأجهزة الموزع لها IP (DHCP Leases)", "/ip/dhcp-server/lease/print", "/ip/dhcp-server/lease/print", Color(0xFFEC4899))
                    },
                    ToolTileSpec("المزود للخدمة DHCP", Icons.Default.Code, Color(0xFF8B5CF6)) {
                        activeQuerySpec = SpecialToolQuerySpec("عميل المزود (DHCP Client)", "/ip/dhcp-client/print", "/ip/dhcp-client/print", Color(0xFF8B5CF6))
                    },
                    ToolTileSpec("NAT", Icons.Default.Public, Color(0xFFEF4444)) {
                        activeQuerySpec = SpecialToolQuerySpec("جدار NAT والمشاركة", "/ip/firewall/nat/print", "/ip/firewall/nat/print", Color(0xFFEF4444))
                    },
                    ToolTileSpec("خادم DNS", Icons.Default.Storage, Color(0xFF06B6D4)) {
                        activeQuerySpec = SpecialToolQuerySpec("إعدادات خادم DNS", "/ip/dns/print", "/ip/dns/print", Color(0xFF06B6D4))
                    },
                    ToolTileSpec("مجموعات العناوين", Icons.Default.Pool, Color(0xFF94A3B8)) {
                        activeQuerySpec = SpecialToolQuerySpec("نطاقات توزيع الآيبي (IP Pool)", "/ip/pool/print", "/ip/pool/print", Color(0xFF94A3B8))
                    },
                    ToolTileSpec("جدار الحماية", Icons.Default.Security, Color(0xFFA855F7)) {
                        activeQuerySpec = SpecialToolQuerySpec("ملخص جدار الحماية (Filter)", "/ip/firewall/filter/print", "/ip/firewall/filter/print", Color(0xFFA855F7))
                    },
                    ToolTileSpec("ARP", Icons.Default.SettingsEthernet, Color(0xFF60A5FA)) {
                        activeQuerySpec = SpecialToolQuerySpec("جدول العناوين الفيزيائية (ARP Table)", "/ip/arp/print", "/ip/arp/print", Color(0xFF60A5FA))
                    }
                )
                ToolsGroupCard(title = "إعدادات الشبكة (IP)", count = 9, accentColor = Color(0xFF10B981), icon = Icons.Default.Wifi) {
                    TwoColumnToolGrid(items)
                }
            }

            // 13. IP Cloud DDNS & Shutdown Card
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFF0F172A),
                    border = BorderStroke(1.dp, Color(0xFF1E293B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFF0284C7).copy(alpha = 0.2f))
                            ) {
                                Icon(Icons.Default.CloudSync, contentDescription = null, tint = Color(0xFF38BDF8))
                            }
                            Column {
                                Text("تفعيل سحابة MikroTik Cloud DDNS", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                                Text(
                                    text = activeRouter?.cloudDdnsName?.takeIf { it.isNotBlank() } ?: "ربط الراوتر عن بُعد بنقرة واحدة",
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }

                        if (!activeRouter?.cloudDdnsName.isNullOrBlank()) {
                            OutlinedButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(activeRouter!!.cloudDdnsName))
                                    viewModel.snackbarMessage.value = "تم نسخ عنوان الكلاود إلى الحافظة"
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(activeRouter!!.cloudDdnsName, color = Color(0xFF38BDF8), fontSize = 12.sp)
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { activeRouter?.let { viewModel.enableCloudDdns(it) } },
                                enabled = !isEnablingCloud && activeRouter != null,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                            ) {
                                Text(if (isEnablingCloud) "جاري التفعيل..." else "تفعيل الكلاود", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                            }

                            Button(
                                onClick = { confirmShutdown = true },
                                enabled = activeRouter != null,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                            ) {
                                Text("إيقاف تشغيل الراوتر", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                            }
                        }

                        cloudResult?.let { msg ->
                            Text(text = msg, color = Color(0xFF34D399), fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }

    // Dialogs
    activeQuerySpec?.let { spec ->
        LiveRouterCommandViewerDialog(
            viewModel = viewModel,
            router = activeRouter,
            spec = spec,
            onDismiss = { activeQuerySpec = null }
        )
    }

    if (showPppoeDialog) {
        PppoeBroadbandDialog(viewModel = viewModel, router = activeRouter, onDismiss = { showPppoeDialog = false })
    }

    if (showHtmlDirDialog) {
        HotspotHtmlDirDialog(
            onDismiss = { showHtmlDirDialog = false },
            onApply = { dir ->
                viewModel.setHotspotHtmlDirectory(activeRouter, dir)
                showHtmlDirDialog = false
            }
        )
    }

    if (showFreeNetDialog) {
        FreeNetworkControlDialog(
            onDismiss = { showFreeNetDialog = false },
            onToggleFree = { enable ->
                viewModel.toggleFreeNetworkMode(activeRouter, enable)
                showFreeNetDialog = false
            }
        )
    }

    if (showBluetoothBlockDialog) {
        BluetoothSharingControlDialog(
            onDismiss = { showBluetoothBlockDialog = false },
            onToggleBlock = { block ->
                viewModel.toggleBluetoothSharingBlock(activeRouter, block)
                showBluetoothBlockDialog = false
            }
        )
    }

    if (showPingDialog) {
        PingToolDialog(viewModel = viewModel, router = activeRouter, onDismiss = { showPingDialog = false })
    }

    addressListSpec?.let { (listName, title, color) ->
        AddressListManagerDialog(
            viewModel = viewModel,
            router = activeRouter,
            listName = listName,
            title = title,
            accentColor = color,
            onDismiss = { addressListSpec = null }
        )
    }

    if (confirmReboot && activeRouter != null) {
        AlertDialog(
            onDismissRequest = { confirmReboot = false },
            containerColor = Color(0xFF0F172A),
            titleContentColor = Color.White,
            textContentColor = Color(0xFFCBD5E1),
            title = { Text("إعادة تشغيل [${activeRouter.name}]") },
            text = { Text("هل تريد إرسال أمر إعادة التشغيل الفوري إلى الراوتر؟") },
            confirmButton = {
                Button(
                    onClick = { viewModel.rebootRouter(activeRouter); confirmReboot = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) { Text("إعادة تشغيل الآن", color = Color.White) }
            },
            dismissButton = { OutlinedButton(onClick = { confirmReboot = false }) { Text("إلغاء", color = Color.White) } }
        )
    }

    if (confirmShutdown && activeRouter != null) {
        AlertDialog(
            onDismissRequest = { confirmShutdown = false },
            containerColor = Color(0xFF0F172A),
            titleContentColor = Color.White,
            textContentColor = Color(0xFFCBD5E1),
            title = { Text("⚠️ إيقاف تشغيل [${activeRouter.name}]") },
            text = { Text("تحذير: سيؤدي هذا الأمر إلى إيقاف تشغيل الراوتر بالكامل (/system/shutdown).") },
            confirmButton = {
                Button(
                    onClick = { viewModel.shutdownRouter(activeRouter); confirmShutdown = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) { Text("إيقاف تشغيل الآن", color = Color.White) }
            },
            dismissButton = { OutlinedButton(onClick = { confirmShutdown = false }) { Text("إلغاء", color = Color.White) } }
        )
    }
}

private data class ToolTileSpec(
    val title: String,
    val icon: ImageVector,
    val accentColor: Color,
    val onClick: () -> Unit
)

@Composable
private fun ExpandableInlineCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    expanded: Boolean,
    onToggle: () -> Unit,
    testTag: String,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF0F172A),
        border = BorderStroke(1.dp, if (expanded) accentColor.copy(alpha = 0.6f) else Color(0xFF1E293B)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .testTag(testTag),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(accentColor.copy(alpha = 0.16f))
                    ) {
                        Icon(icon, contentDescription = title, tint = accentColor, modifier = Modifier.size(22.dp))
                    }
                    Column {
                        Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(subtitle, fontSize = 11.sp, color = Color(0xFF94A3B8))
                    }
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "طي القسم" else "فتح القسم",
                    tint = accentColor
                )
            }

            AnimatedVisibility(visible = expanded) {
                content()
            }
        }
    }
}

@Composable
private fun ToolsGroupCard(
    title: String,
    count: Int,
    accentColor: Color,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF0F172A),
        border = BorderStroke(1.dp, Color(0xFF1E293B)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                        modifier = Modifier
                            .size(width = 4.dp, height = 22.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(accentColor)
                    )
                    Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = accentColor.copy(alpha = 0.16f)
                ) {
                    Text(
                        text = "$count",
                        color = accentColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
            content()
        }
    }
}

@Composable
private fun TwoColumnToolGrid(items: List<ToolTileSpec>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowItems.forEach { item ->
                    Surface(
                        onClick = item.onClick,
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF1E293B),
                        border = BorderStroke(1.dp, item.accentColor.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 78.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(item.accentColor.copy(alpha = 0.18f))
                            ) {
                                Icon(item.icon, contentDescription = item.title, tint = item.accentColor, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = item.title,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun FeatureRowBannerCard(
    title: String,
    subtitle: String,
    buttonLabel: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF1E293B),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.38f)),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 68.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.18f))
            ) {
                Icon(icon, contentDescription = title, tint = accentColor, modifier = Modifier.size(22.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(subtitle, fontSize = 10.sp, color = Color(0xFF94A3B8), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = accentColor.copy(alpha = 0.18f),
                border = BorderStroke(0.8.dp, accentColor.copy(alpha = 0.5f))
            ) {
                Text(
                    text = buttonLabel,
                    color = accentColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                )
            }
        }
    }
}
