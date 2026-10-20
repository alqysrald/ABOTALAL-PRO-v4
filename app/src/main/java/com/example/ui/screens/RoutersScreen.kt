package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ConnectionType
import com.example.data.model.RouterEntity
import com.example.ui.TalalProViewModel
import com.example.ui.theme.*

@Composable
fun RoutersScreen(
    viewModel: TalalProViewModel,
    modifier: Modifier = Modifier
) {
    val routers by viewModel.allRouters.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    var showHelpAccordion by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0B132B))
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(vertical = 14.dp)
    ) {
        // 1. Header Card (Matches Screenshot 2)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF1E293B))),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "راوترات MikroTik",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "فحص تلقائي كل 60 ثانية — يتصل بالراوترات مباشرة",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF1E3A8A))
                        ) {
                            Icon(Icons.Default.Wifi, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(24.dp))
                        }
                    }

                    // Registered Routers Pill
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF064E3B))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF34D399))
                        )
                        Text(
                            text = "${routers.size} راوتر مسجل",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // 2. "شرح الاستخدام" Collapsible Accordion (Matches Screenshot 2)
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF1E293B))),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showHelpAccordion = !showHelpAccordion }
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.HelpOutline, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                            Text(
                                text = "شرح الاستخدام",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF38BDF8)
                            )
                        }
                        Icon(
                            imageVector = if (showHelpAccordion) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8)
                        )
                    }

                    AnimatedVisibility(visible = showHelpAccordion) {
                        Column(
                            modifier = Modifier.padding(top = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "• أضف الراوتر باستخدام عنوان MikroTik Cloud DDNS (مثال: xxxx.sn.mynetname.net).",
                                fontSize = 11.sp,
                                color = Color(0xFFCBD5E1)
                            )
                            Text(
                                text = "• تأكد من فتح المنفذ 8728 في /ip firewall filter للراوتر.",
                                fontSize = 11.sp,
                                color = Color(0xFFCBD5E1)
                            )
                            Text(
                                text = "• إذا كان لديك IP محلي مثل 10.0.0.1، ضعه كـ IP احتياطي للتبديل التلقائي أثناء الاتصال الداخلي.",
                                fontSize = 11.sp,
                                color = Color(0xFFCBD5E1)
                            )
                        }
                    }
                }
            }
        }

        // 3. 3 Action Buttons Row (Matches Screenshot 2)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Add Router Button (Cyan)
                Button(
                    onClick = { viewModel.showAddRouterDialog.value = true },
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    modifier = Modifier
                        .weight(1.3f)
                        .testTag("add_router_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Text("إضافة راوتر", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                // Connection Guide Button (Purple)
                Button(
                    onClick = { viewModel.showCloudGuideDialog.value = true },
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF581C87)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                    modifier = Modifier
                        .weight(1.2f)
                        .testTag("cloud_guide_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Link, contentDescription = null, tint = Color(0xFFE9D5FF), modifier = Modifier.size(16.dp))
                        Text("دليل الربط", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF3E8FF))
                    }
                }

                // Check All Button (Dark Blue)
                Button(
                    onClick = { viewModel.refreshAll() },
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                    modifier = Modifier
                        .weight(1.1f)
                        .testTag("check_all_routers_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Sync, contentDescription = null, tint = Color(0xFF93C5FD), modifier = Modifier.size(16.dp))
                        }
                        Text("فحص الكل", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        // 4. Router List or Empty State Box (Matches Screenshot 2)
        if (routers.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF334155))
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "لا توجد راوترات — اضغط «إضافة راوتر»",
                            fontSize = 13.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }
        } else {
            items(routers) { router ->
                RouterCardItem(
                    router = router,
                    onDelete = { viewModel.deleteRouter(router) },
                    onRefresh = { viewModel.refreshAll() },
                    onImportNeighbors = {
                        viewModel.importNeighbors(router)
                    }
                )
            }
        }
    }
}

@Composable
fun RouterCardItem(
    router: RouterEntity,
    onDelete: () -> Unit,
    onRefresh: () -> Unit,
    onImportNeighbors: () -> Unit
) {
    var expandedMenu by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (router.isOnline) Color(0xFF065F46) else Color(0xFF7F1D1D)
            )
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Row: Status Dot, Router Name, Connection Type, Menu
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
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(if (router.isOnline) Color(0xFF10B981) else Color(0xFFEF4444))
                    )
                    Column {
                        Text(
                            text = router.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = router.host,
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Type Pill
                    val typeLabel = when (router.connectionType) {
                        ConnectionType.CLOUD -> "كلاود"
                        ConnectionType.PUBLIC_IP -> "IP عام"
                        ConnectionType.SSTP -> "SSTP"
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF1E293B)
                    ) {
                        Text(
                            text = typeLabel,
                            fontSize = 10.sp,
                            color = Color(0xFF38BDF8),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // More Menu
                    Box {
                        IconButton(onClick = { expandedMenu = true }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.MoreVert, contentDescription = "خيارات", tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                        }
                        DropdownMenu(
                            expanded = expandedMenu,
                            onDismissRequest = { expandedMenu = false },
                            modifier = Modifier.background(Color(0xFF1E293B))
                        ) {
                            DropdownMenuItem(
                                text = { Text("استيراد أجهزة النيبرز", color = Color.White) },
                                leadingIcon = { Icon(Icons.Default.Hub, contentDescription = null, tint = Color(0xFF34D399)) },
                                onClick = {
                                    expandedMenu = false
                                    onImportNeighbors()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("حذف الراوتر", color = Color(0xFFEF4444)) },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFEF4444)) },
                                onClick = {
                                    expandedMenu = false
                                    onDelete()
                                }
                            )
                        }
                    }
                }
            }

            // Metrics Grid (CPU, Memory, Ping, Users)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1E293B))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "المعالج", fontSize = 10.sp, color = Color(0xFF94A3B8))
                    Text(text = "${router.cpuLoad}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (router.cpuLoad > 80) Color(0xFFEF4444) else Color(0xFF38BDF8))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "الاستجابة", fontSize = 10.sp, color = Color(0xFF94A3B8))
                    Text(text = "${router.pingMs}ms", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF34D399))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "الذاكرة الحرة", fontSize = 10.sp, color = Color(0xFF94A3B8))
                    Text(text = router.freeMemory, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "المستخدمين", fontSize = 10.sp, color = Color(0xFF94A3B8))
                    Text(text = "${router.activeHotspotUsers + router.activePppoeUsers}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFBBF24))
                }
            }

            if (router.lastError != null && !router.isOnline) {
                Text(
                    text = "⚠️ ${router.lastError}",
                    fontSize = 11.sp,
                    color = Color(0xFFF87171)
                )
            }
        }
    }
}
