package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.TalalProViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartAssistantScreen(
    viewModel: TalalProViewModel,
    modifier: Modifier = Modifier
) {
    val routers by viewModel.allRouters.collectAsState()
    val messages by viewModel.assistantMessages.collectAsState()
    val selectedRouter by viewModel.selectedAssistantRouter.collectAsState()
    val isThinking by viewModel.isAssistantThinking.collectAsState()

    var routerDropdownExpanded by remember { mutableStateOf(false) }
    var inputQuery by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val quickCommands = remember {
        listOf(
            "فحص المتصلين الآن",
            "فحص الأجهزة والمنافذ",
            "تحليل الراوتر",
            "منع مشاركة البلوتوث TTL=1",
            "تفعيل الكلاود",
            "تنظيف كاش DNS",
            "نسخة احتياطية",
            "/system resource print",
            "/ip hotspot active print",
            "إعادة تشغيل الراوتر",
            "إيقاف تشغيل الراوتر"
        )
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0B132B))
            .padding(horizontal = 14.dp)
            .padding(bottom = 10.dp)
    ) {
        // 1. Luxury Assistant Header
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = BorderStroke(1.dp, Color(0xFF1E293B)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "المساعد الشبكي الشامل",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF10B981).copy(alpha = 0.2f),
                                border = BorderStroke(0.8.dp, Color(0xFF10B981).copy(alpha = 0.6f))
                            ) {
                                Text(
                                    text = "كامل الصلاحيات ⚡",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF34D399),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "ينفذ جميع أوامر MikroTik CLI (ابدأ بـ /) والتحكم الكامل بالتطبيق والراوتر",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1E3A8A))
                    ) {
                        Icon(Icons.Default.SmartToy, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(24.dp))
                    }
                }

                // Quick Direct Action Buttons: فحص المتصلين الآن + فحص الأجهزة والمنافذ
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.sendAssistantMessage("فحص المتصلين الآن") },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("assistant_check_active_btn")
                    ) {
                        Icon(Icons.Default.Wifi, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("فحص المتصلين الآن", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Button(
                        onClick = { viewModel.sendAssistantMessage("فحص الأجهزة والمنافذ") },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("assistant_check_ports_btn")
                    ) {
                        Icon(Icons.Default.SettingsEthernet, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("الأجهزة والمنافذ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                // Router Selector Dropdown
                ExposedDropdownMenuBox(
                    expanded = routerDropdownExpanded,
                    onExpandedChange = { routerDropdownExpanded = !routerDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedRouter?.let { "${it.name} (${it.host})" } ?: "— استهداف الراوتر النشط تلقائياً —",
                        onValueChange = {},
                        readOnly = true,
                        leadingIcon = { Icon(Icons.Default.Hub, contentDescription = null, tint = Color(0xFF38BDF8)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = routerDropdownExpanded) },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0xFF1E293B),
                            unfocusedContainerColor = Color(0xFF1E293B),
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF334155)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = routerDropdownExpanded,
                        onDismissRequest = { routerDropdownExpanded = false },
                        modifier = Modifier.background(Color(0xFF1E293B))
                    ) {
                        DropdownMenuItem(
                            text = { Text("— اختيار تلقائي للراوتر المتصل —", color = Color.White) },
                            onClick = {
                                viewModel.selectedAssistantRouter.value = null
                                routerDropdownExpanded = false
                            }
                        )
                        routers.forEach { r ->
                            DropdownMenuItem(
                                text = { Text("${r.name} (${r.host})", color = Color.White) },
                                onClick = {
                                    viewModel.selectedAssistantRouter.value = r
                                    routerDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // Scrollable Quick Command Chips
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(quickCommands) { cmd ->
                SuggestionChip(
                    onClick = {
                        if (cmd.startsWith("/")) {
                            inputQuery = cmd
                        } else {
                            viewModel.sendAssistantMessage(cmd)
                        }
                    },
                    label = {
                        Text(
                            text = cmd,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (cmd.startsWith("/")) Color(0xFF38BDF8) else Color.White
                        )
                    },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = Color(0xFF1E293B)
                    ),
                    border = BorderStroke(1.dp, Color(0xFF334155))
                )
            }
        }

        // 2. Chat Conversation Feed
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 6.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                val isUser = msg.sender == "user"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                ) {
                    Card(
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isUser) 16.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 16.dp
                        ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isUser) Color(0xFF1D4ED8) else Color(0xFF0F172A)
                        ),
                        border = if (isUser) null else BorderStroke(1.dp, Color(0xFF1E293B)),
                        modifier = Modifier.widthIn(max = 340.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = msg.content,
                                fontSize = 13.sp,
                                color = Color.White,
                                lineHeight = 19.sp
                            )

                            if (!msg.commandOutput.isNullOrBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF020617),
                                    border = BorderStroke(1.dp, Color(0xFF1E293B)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = msg.commandOutput,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFF34D399),
                                        lineHeight = 16.sp,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (isThinking) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                            border = BorderStroke(1.dp, Color(0xFF1E293B))
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color(0xFF38BDF8), strokeWidth = 2.dp)
                                Text("جاري تنفيذ الأمر على الراوتر...", fontSize = 11.sp, color = Color(0xFF94A3B8))
                            }
                        }
                    }
                }
            }
        }

        // 3. Message & Arbitrary CLI Input Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = {
                    if (inputQuery.isNotBlank()) {
                        val q = inputQuery
                        inputQuery = ""
                        viewModel.sendAssistantMessage(q)
                    }
                },
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF059669))
                    .testTag("send_assistant_query_button")
            ) {
                Icon(Icons.Default.Send, contentDescription = "إرسال وتنفيذ الأمر", tint = Color.White, modifier = Modifier.size(20.dp))
            }

            OutlinedTextField(
                value = inputQuery,
                onValueChange = { inputQuery = it },
                placeholder = {
                    Text(
                        "اكتب أي أمر مثل: /ip address print أو فحص المتصلين...",
                        color = Color(0xFF64748B),
                        fontSize = 11.sp
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color(0xFF0F172A),
                    unfocusedContainerColor = Color(0xFF0F172A),
                    focusedBorderColor = Color(0xFF38BDF8),
                    unfocusedBorderColor = Color(0xFF1E293B)
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("assistant_input")
            )
        }
    }
}
