package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DeviceStatus
import com.example.ui.TalalProViewModel

@Composable
fun NetworkIssuesScreen(
    viewModel: TalalProViewModel,
    modifier: Modifier = Modifier
) {
    val routers by viewModel.allRouters.collectAsState()
    val devices by viewModel.allDevices.collectAsState()
    val offlineDevices = devices.filter { it.status == DeviceStatus.OFFLINE || it.hasIssue }
    val offlineRouters = routers.filter { !it.isOnline }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0B132B))
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(vertical = 14.dp)
    ) {
        // Header Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF1E293B))),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "مركز تشخيص ومشاكل الشبكة",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "تحليل فوري للانقطاعات وحالات عدم الاستجابة",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF7F1D1D))
                    ) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFFFCA5A5), modifier = Modifier.size(22.dp))
                    }
                }
            }
        }

        // Summary Card
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = if (offlineRouters.isNotEmpty()) Color(0xFF450A0A) else Color(0xFF064E3B)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("الراوترات المنقطعة", fontSize = 11.sp, color = Color(0xFFCBD5E1))
                        Text("${offlineRouters.size}", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = if (offlineDevices.isNotEmpty()) Color(0xFF451A03) else Color(0xFF064E3B)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("الأجهزة الطافية", fontSize = 11.sp, color = Color(0xFFCBD5E1))
                        Text("${offlineDevices.size}", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        // Action Button: Recheck All
        item {
            Button(
                onClick = { viewModel.refreshAll() },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White)
                    Text("إعادة فحص كل الأجهزة والراوترات الآن", fontWeight = FontWeight.Bold)
                }
            }
        }

        // List of issues
        if (offlineRouters.isEmpty() && offlineDevices.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF064E3B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF34D399), modifier = Modifier.size(36.dp))
                        Text("الشبكة مستقرة تماماً", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("جميع الراوترات والأجهزة والأبراج متصلة وتعمل بصورة ممتازة.", fontSize = 12.sp, color = Color(0xFFECFDF5))
                    }
                }
            }
        } else {
            if (offlineRouters.isNotEmpty()) {
                item {
                    Text("انقطاعات الراوترات:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF87171))
                }
                items(offlineRouters) { r ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF450A0A)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(r.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("انقطاع الاتصال", fontSize = 11.sp, color = Color(0xFFFCA5A5))
                            }
                            Text(r.host, fontSize = 11.sp, color = Color(0xFFE2E8F0))
                            if (r.lastError != null) {
                                Text("السبب: ${r.lastError}", fontSize = 10.sp, color = Color(0xFFFECACA))
                            }
                        }
                    }
                }
            }

            if (offlineDevices.isNotEmpty()) {
                item {
                    Text("الأجهزة والأبراج المتوقفة:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFBBF24))
                }
                items(offlineDevices) { d ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF451A03)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(d.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("IP: ${d.ip}", fontSize = 11.sp, color = Color(0xFF38BDF8))
                                if (d.issueReason.isNotBlank()) {
                                    Text(d.issueReason, fontSize = 10.sp, color = Color(0xFFFDE68A))
                                }
                            }
                            Button(
                                onClick = { viewModel.pingDevice(d) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("فحص Ping", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
