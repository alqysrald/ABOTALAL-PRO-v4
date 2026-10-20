package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RouterEntity
import com.example.ui.TalalProViewModel
import com.example.ui.theme.*

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
            .padding(bottom = 12.dp)
    ) {
        // 1. Header Card (Matches Screenshot 14)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF1E293B))),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
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
                    Column {
                        Text(
                            text = "المساعد الشبكي الذكي",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "إدارة وتحليل وتنفيذ الأوامر المباشرة على شبكات MikroTik",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                        Text(
                            text = "778215553 | 700897775",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF1E3A8A))
                    ) {
                        Icon(Icons.Default.SmartToy, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(24.dp))
                    }
                }

                // Analyze Router Button
                Button(
                    onClick = {
                        val r = selectedRouter ?: routers.firstOrNull()
                        if (r != null) {
                            viewModel.sendAssistantMessage("تحليل الراوتر ${r.name}")
                        } else {
                            viewModel.sendAssistantMessage("تحليل عام للشبكة")
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Bolt, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Text("تحليل الراوتر المباشر", fontWeight = FontWeight.Bold)
                    }
                }

                // Router Selector Dropdown
                ExposedDropdownMenuBox(
                    expanded = routerDropdownExpanded,
                    onExpandedChange = { routerDropdownExpanded = !routerDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedRouter?.name ?: "اختر الراوتر للتحليل",
                        onValueChange = {},
                        readOnly = true,
                        leadingIcon = { Icon(Icons.Default.Hub, contentDescription = null, tint = Color(0xFF38BDF8)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = routerDropdownExpanded) },
                        shape = RoundedCornerShape(10.dp),
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
                            text = { Text("— تحليل عام لكل الشبكة —", color = Color.White) },
                            onClick = {
                                viewModel.selectedAssistantRouter.value = null
                                routerDropdownExpanded = false
                            }
                        )
                        routers.forEach { r ->
                            DropdownMenuItem(
                                text = { Text(r.name, color = Color.White) },
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

        // 2. Chat Conversation Feed
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(messages) { msg ->
                val isUser = msg.sender == "user"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                ) {
                    Card(
                        shape = RoundedCornerShape(
                            topStart = 14.dp,
                            topEnd = 14.dp,
                            bottomStart = if (isUser) 14.dp else 2.dp,
                            bottomEnd = if (isUser) 2.dp else 14.dp
                        ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isUser) Color(0xFF1D4ED8) else Color(0xFF0F172A)
                        ),
                        border = if (isUser) null else CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF1E293B))),
                        modifier = Modifier.widthIn(max = 320.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = msg.content,
                                fontSize = 13.sp,
                                color = Color.White,
                                lineHeight = 19.sp
                            )
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
                            modifier = Modifier.padding(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color(0xFF38BDF8), strokeWidth = 2.dp)
                                Text("جاري تحليل بيانات الراوتر...", fontSize = 11.sp, color = Color(0xFF94A3B8))
                            }
                        }
                    }
                }
            }
        }

        // 3. Message Input Field (Matches Screenshot 14)
        Row(
            modifier = Modifier.fillMaxWidth(),
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
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF059669))
                    .testTag("send_assistant_query_button")
            ) {
                Icon(Icons.Default.Send, contentDescription = "إرسال", tint = Color.White, modifier = Modifier.size(20.dp))
            }

            OutlinedTextField(
                value = inputQuery,
                onValueChange = { inputQuery = it },
                placeholder = { Text("اكتب سؤالك العام هنا...", color = Color(0xFF64748B), fontSize = 12.sp) },
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
