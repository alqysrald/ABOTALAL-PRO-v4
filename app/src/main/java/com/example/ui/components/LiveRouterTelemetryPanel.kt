package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RouterEntity
import com.example.ui.DashboardLiveTelemetryState
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun LiveRouterTelemetryHeroPanel(
    telemetry: DashboardLiveTelemetryState,
    routers: List<RouterEntity>,
    onSelectRouter: (Long) -> Unit,
    onToggleStreaming: () -> Unit,
    onAddRouterClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "live_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    val borderBrush = Brush.linearGradient(
        colors = if (telemetry.isOnline) {
            listOf(Color(0xFF0EA5E9), Color(0xFF10B981), Color(0xFF1D4ED8))
        } else {
            listOf(Color(0xFF334155), Color(0xFF1E293B))
        }
    )

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF070D1D)),
        border = BorderStroke(1.4.dp, borderBrush),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF09132B),
                            Color(0xFF0D1B38),
                            Color(0xFF070E20)
                        )
                    )
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Header Row: Live Pulse Beacon + Title + Pause/Play Stream Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
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
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF0284C7), Color(0xFF059669))
                                )
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = "لوحة معلومات الراوتر المباشرة",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "مركز قياس المايكروتيك الحي",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (telemetry.isStreaming && telemetry.isOnline) {
                                    Color(0xFF064E3B)
                                } else {
                                    Color(0xFF1E293B)
                                }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when {
                                                    !telemetry.isStreaming -> Color(0xFFF59E0B)
                                                    telemetry.isOnline -> Color(0xFF34D399).copy(alpha = pulseAlpha)
                                                    else -> Color(0xFFEF4444)
                                                }
                                            )
                                    )
                                    Text(
                                        text = when {
                                            !telemetry.isStreaming -> "متوقف مؤقتاً"
                                            telemetry.isOnline -> "مباشر • 1ث"
                                            telemetry.hasRouters -> "غير متصل"
                                            else -> "بانتظار راوتر"
                                        },
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        Text(
                            text = if (telemetry.hasRouters) {
                                "${telemetry.routerName} • آخر قراءة ${telemetry.lastUpdatedTime}"
                            } else {
                                "قراءات لحظية للمعالج، الحرارة، الموديل، الإصدار ومدة الاتصال"
                            },
                            fontSize = 10.5.sp,
                            color = Color(0xFF94A3B8),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                FilledIconButton(
                    onClick = onToggleStreaming,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (telemetry.isStreaming) Color(0xFF0F294A) else Color(0xFF065F46),
                        contentColor = Color(0xFF38BDF8)
                    ),
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("live_telemetry_toggle_btn")
                ) {
                    Icon(
                        imageVector = if (telemetry.isStreaming) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (telemetry.isStreaming) "إيقاف البث الحي مؤقتاً" else "تشغيل البث الحي كل ثانية",
                        tint = Color.White
                    )
                }
            }

            // 2. Router Switcher Row (When multiple routers exist)
            if (routers.size > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    routers.forEach { router ->
                        val isSelected = telemetry.selectedRouterId == router.id
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) Color(0xFF0284C7) else Color(0xFF132242),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) Color(0xFF7DD3FC) else Color(0xFF23365E)
                            ),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .clickable { onSelectRouter(router.id) }
                                .testTag("telemetry_router_chip_${router.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (router.isOnline) Color(0xFF34D399) else Color(0xFFEF4444))
                                )
                                Text(
                                    text = router.name,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            // 3. Empty State (No Router Added Yet)
            if (routers.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF0F1C3A),
                    border = BorderStroke(1.dp, Color(0xFF1E3A8A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Router,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(38.dp)
                        )
                        Text(
                            text = "أضف راوتر MikroTik لعرض المقياس المباشر كل ثانية",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "سيتم عرض حمل المعالج، درجة الحرارة، موديل الراوتر، إصدار النظام، ومدة الاتصال برسومات حية لحظية.",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                        Button(
                            onClick = onAddRouterClick,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("telemetry_add_router_btn")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("إضافة راوتر مايكروتيك الآن", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                        }
                    }
                }
                return@Column
            }

            val snap = telemetry.snapshot

            // 4. Router Hardware Identity Ribbon (Model + RouterOS Version + Arch/CPU Specs)
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF0E1B37),
                border = BorderStroke(1.dp, Color(0xFF1E3460)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        IdentitySpecItem(
                            icon = Icons.Default.Memory,
                            label = "موديل المايكروتيك",
                            value = buildString {
                                append(snap?.boardName?.ifBlank { "MikroTik" } ?: "جاري القراءة...")
                                val arch = snap?.architectureName.orEmpty()
                                if (arch.isNotBlank() && !snap?.boardName.orEmpty().contains(arch, true)) {
                                    append(" ($arch)")
                                }
                            },
                            accentColor = Color(0xFF38BDF8),
                            modifier = Modifier.weight(1.1f)
                        )

                        IdentitySpecItem(
                            icon = Icons.Default.Verified,
                            label = "إصدار RouterOS",
                            value = snap?.version?.ifBlank { "--" } ?: "--",
                            accentColor = Color(0xFF34D399),
                            modifier = Modifier.weight(0.9f)
                        )
                    }

                    HorizontalDivider(color = Color(0xFF1E293B))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeveloperBoard,
                                contentDescription = null,
                                tint = Color(0xFFA78BFA),
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = if (snap != null && snap.isOnline) {
                                    "المعالج: ${snap.cpuModel} • ${snap.cpuCount} نواة" +
                                        if (snap.cpuFrequencyMhz > 0) " • ${snap.cpuFrequencyMhz} MHz" else ""
                                } else {
                                    snap?.errorMessage ?: "جاري الاتصال بالراوتر لقراءة المواصفات..."
                                },
                                fontSize = 10.5.sp,
                                color = if (snap?.isOnline == true) Color(0xFFCBD5E1) else Color(0xFFFCA5A5),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (!snap?.voltageStr.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF1E293B)
                            ) {
                                Text(
                                    text = "⚡ ${snap?.voltageStr}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFBBF24),
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 5. Dual Precision Canvas Radial Arc Gauges (CPU Load + CPU Temperature)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                RadialCpuLoadGaugeCard(
                    cpuLoad = if (snap?.isOnline == true) snap.cpuLoad else 0,
                    isOnline = snap?.isOnline == true,
                    modifier = Modifier.weight(1f)
                )
                RadialCpuTemperatureGaugeCard(
                    temperatureC = if (snap?.isOnline == true) snap.cpuTemperatureC else null,
                    isOnline = snap?.isOnline == true,
                    modifier = Modifier.weight(1f)
                )
            }

            // 6. Real-Time Area Wave Graph (Last 30 seconds CPU + Ping)
            LiveCpuWaveformChartCard(
                cpuHistory = telemetry.cpuHistory,
                pingHistory = telemetry.pingHistory,
                currentCpu = if (snap?.isOnline == true) snap.cpuLoad else 0,
                currentPing = snap?.pingMs ?: 0L,
                isOnline = snap?.isOnline == true
            )

            // 7. 4-Tile Core Live Telemetry Matrix (Uptime, RAM, Ping, Active Sessions)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TelemetryMetricTile(
                    icon = Icons.Default.Schedule,
                    label = "مدة الاتصال (Uptime)",
                    value = if (snap?.isOnline == true) snap.uptime else "--",
                    subValue = if (snap?.isOnline == true) "متصل باستقرار" else "منقطع",
                    accentColor = Color(0xFFFBBF24),
                    modifier = Modifier.weight(1f)
                )
                TelemetryMetricTile(
                    icon = Icons.Default.SdStorage,
                    label = "الذاكرة العشوائية (RAM)",
                    value = if (snap?.isOnline == true) "${snap.memoryUsagePercent}% مستخدم" else "--",
                    subValue = if (snap?.isOnline == true) "متاح ${snap.freeMemoryFormatted} / ${snap.totalMemoryFormatted}" else "--",
                    accentColor = Color(0xFF38BDF8),
                    progressFraction = if (snap?.isOnline == true) snap.memoryUsagePercent / 100f else null,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TelemetryMetricTile(
                    icon = Icons.Default.NetworkCheck,
                    label = "زمن الاستجابة (Ping)",
                    value = if (snap?.isOnline == true) "${snap.pingMs} ms" else "Timeout",
                    subValue = when {
                        snap?.isOnline != true -> "لا توجد استجابة"
                        snap.pingMs < 25 -> "اتصال فائق السرعة"
                        snap.pingMs < 80 -> "استجابة مستقرة"
                        else -> "زمن استجابة مرتفع"
                    },
                    accentColor = if (snap?.isOnline == true && snap.pingMs < 80) Color(0xFF34D399) else Color(0xFFF87171),
                    modifier = Modifier.weight(1f)
                )
                val totalSessions = (snap?.activeHotspotUsers ?: 0) + (snap?.activePppoeUsers ?: 0)
                TelemetryMetricTile(
                    icon = Icons.Default.Groups,
                    label = "المشتركين المتصلين",
                    value = if (snap?.isOnline == true) "$totalSessions متصل" else "--",
                    subValue = if (snap?.isOnline == true) "هوتسبوت: ${snap.activeHotspotUsers} • PPP: ${snap.activePppoeUsers}" else "--",
                    accentColor = Color(0xFFC084FC),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun IdentitySpecItem(
    icon: ImageVector,
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(accentColor.copy(alpha = 0.14f))
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(17.dp))
        }
        Column {
            Text(text = label, fontSize = 9.5.sp, color = Color(0xFF94A3B8))
            Text(
                text = value,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RadialCpuLoadGaugeCard(
    cpuLoad: Int,
    isOnline: Boolean,
    modifier: Modifier = Modifier
) {
    val safeLoad = cpuLoad.coerceIn(0, 100)
    val animatedSweep by animateFloatAsState(
        targetValue = if (isOnline) safeLoad / 100f else 0f,
        animationSpec = tween(durationMillis = 450),
        label = "cpu_arc"
    )
    val statusColor = when {
        !isOnline -> Color(0xFF64748B)
        safeLoad >= 80 -> Color(0xFFEF4444)
        safeLoad >= 55 -> Color(0xFFF59E0B)
        else -> Color(0xFF10B981)
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF0C1833),
        border = BorderStroke(1.dp, Color(0xFF1E325C)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "حمل المعالج (CPU)",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFCBD5E1)
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(116.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                    val strokeW = 11.dp.toPx()
                    val diameter = size.minDimension
                    val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
                    val arcSize = Size(diameter, diameter)

                    // Precision outer scale ticks
                    val radius = diameter / 2f + strokeW * 0.65f
                    val center = Offset(size.width / 2f, size.height / 2f)
                    for (i in 0..12) {
                        val angleDeg = 150.0 + (240.0 * (i / 12.0))
                        val rad = angleDeg * (PI / 180.0)
                        val inner = Offset(
                            center.x + ((diameter / 2f - strokeW * 0.55f) * cos(rad)).toFloat(),
                            center.y + ((diameter / 2f - strokeW * 0.55f) * sin(rad)).toFloat()
                        )
                        val outer = Offset(
                            center.x + ((radius - strokeW * 0.2f) * cos(rad)).toFloat(),
                            center.y + ((radius - strokeW * 0.2f) * sin(rad)).toFloat()
                        )
                        drawLine(
                            color = if (i / 12f <= animatedSweep && isOnline) statusColor.copy(alpha = 0.8f) else Color(0xFF334155),
                            start = inner,
                            end = outer,
                            strokeWidth = 1.5.dp.toPx()
                        )
                    }

                    // Background track arc
                    drawArc(
                        color = Color(0xFF1E293B),
                        startAngle = 150f,
                        sweepAngle = 240f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeW, cap = StrokeCap.Round)
                    )

                    // Active sweep arc
                    if (animatedSweep > 0.005f) {
                        drawArc(
                            brush = Brush.sweepGradient(
                                colors = listOf(Color(0xFF10B981), Color(0xFF38BDF8), statusColor)
                            ),
                            startAngle = 150f,
                            sweepAngle = 240f * animatedSweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeW, cap = StrokeCap.Round)
                        )

                        // Glowing tip dot
                        val tipAngleRad = (150.0 + 240.0 * animatedSweep) * (PI / 180.0)
                        val dotOffset = Offset(
                            center.x + ((diameter / 2f) * cos(tipAngleRad)).toFloat(),
                            center.y + ((diameter / 2f) * sin(tipAngleRad)).toFloat()
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 4.dp.toPx(),
                            center = dotOffset
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (isOnline) "$safeLoad%" else "--%",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = when {
                            !isOnline -> "منقطع"
                            safeLoad >= 80 -> "ضغط عالي"
                            safeLoad >= 55 -> "متوسط"
                            else -> "ممتاز"
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
            }
        }
    }
}

@Composable
private fun RadialCpuTemperatureGaugeCard(
    temperatureC: Float?,
    isOnline: Boolean,
    modifier: Modifier = Modifier
) {
    val hasSensor = isOnline && temperatureC != null && temperatureC > 0f
    // Map 20°C..85°C cleanly to 0f..1f
    val normalized = if (hasSensor) {
        ((temperatureC!! - 20f) / 65f).coerceIn(0.05f, 1f)
    } else 0f

    val animatedSweep by animateFloatAsState(
        targetValue = normalized,
        animationSpec = tween(durationMillis = 450),
        label = "temp_arc"
    )
    val tempColor = when {
        !hasSensor -> Color(0xFF64748B)
        temperatureC!! >= 72f -> Color(0xFFEF4444)
        temperatureC >= 58f -> Color(0xFFF59E0B)
        temperatureC >= 45f -> Color(0xFF38BDF8)
        else -> Color(0xFF34D399)
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF0C1833),
        border = BorderStroke(1.dp, Color(0xFF1E325C)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "حرارة المعالج",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFCBD5E1)
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(116.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                    val strokeW = 11.dp.toPx()
                    val diameter = size.minDimension
                    val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
                    val arcSize = Size(diameter, diameter)
                    val center = Offset(size.width / 2f, size.height / 2f)

                    drawArc(
                        color = Color(0xFF1E293B),
                        startAngle = 150f,
                        sweepAngle = 240f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeW, cap = StrokeCap.Round)
                    )

                    if (animatedSweep > 0.01f) {
                        drawArc(
                            brush = Brush.sweepGradient(
                                colors = listOf(Color(0xFF38BDF8), Color(0xFF34D399), Color(0xFFF59E0B), tempColor)
                            ),
                            startAngle = 150f,
                            sweepAngle = 240f * animatedSweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeW, cap = StrokeCap.Round)
                        )

                        val tipAngleRad = (150.0 + 240.0 * animatedSweep) * (PI / 180.0)
                        val dotOffset = Offset(
                            center.x + ((diameter / 2f) * cos(tipAngleRad)).toFloat(),
                            center.y + ((diameter / 2f) * sin(tipAngleRad)).toFloat()
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 4.dp.toPx(),
                            center = dotOffset
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (hasSensor) "${temperatureC!!.toInt()}°C" else if (isOnline) "N/A" else "--°C",
                        fontSize = 23.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = when {
                            !isOnline -> "منقطع"
                            !hasSensor -> "بدون حساس"
                            temperatureC!! >= 72f -> "حرارة مرتفعة"
                            temperatureC >= 58f -> "دافئ"
                            else -> "تبريد طبيعي"
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = tempColor
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveCpuWaveformChartCard(
    cpuHistory: List<Int>,
    pingHistory: List<Long>,
    currentCpu: Int,
    currentPing: Long,
    isOnline: Boolean
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF0C1833),
        border = BorderStroke(1.dp, Color(0xFF1E325C)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Timeline,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "المخطط اللحظي لأداء المعالج والاستجابة (آخر 30 ثانية)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF132242)
                ) {
                    Text(
                        text = if (isOnline) "CPU $currentCpu% • ${currentPing}ms" else "غير متصل",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8),
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(92.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF070F22))
                    .padding(horizontal = 6.dp, vertical = 8.dp)
            ) {
                val w = size.width
                val h = size.height

                // Dashed grid lines at 25%, 50%, 75%
                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                listOf(0.25f, 0.50f, 0.75f).forEach { frac ->
                    val y = h * frac
                    drawLine(
                        color = Color(0xFF1E293B),
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = dashEffect
                    )
                }

                val samples = if (cpuHistory.size >= 2) cpuHistory else listOf(0, currentCpu)
                val count = samples.size
                val stepX = if (count > 1) w / (count - 1).toFloat() else w

                val points = samples.mapIndexed { idx, value ->
                    val x = idx * stepX
                    val y = h - ((value.coerceIn(0, 100) / 100f) * (h * 0.9f)) - (h * 0.05f)
                    Offset(x, y)
                }

                // Draw smooth cubic Bezier curve for CPU %
                val linePath = Path().apply {
                    moveTo(points.first().x, points.first().y)
                    for (i in 1 until points.size) {
                        val prev = points[i - 1]
                        val curr = points[i]
                        val midX = (prev.x + curr.x) / 2f
                        cubicTo(midX, prev.y, midX, curr.y, curr.x, curr.y)
                    }
                }

                val fillPath = Path().apply {
                    addPath(linePath)
                    lineTo(points.last().x, h)
                    lineTo(points.first().x, h)
                    close()
                }

                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF38BDF8).copy(alpha = 0.36f),
                            Color(0xFF0EA5E9).copy(alpha = 0.02f)
                        )
                    )
                )

                drawPath(
                    path = linePath,
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color(0xFF34D399), Color(0xFF38BDF8))
                    ),
                    style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round)
                )

                // Draw subtle Ping ms overlay wave in amber if samples exist
                if (pingHistory.size >= 2) {
                    val maxPing = (pingHistory.maxOrNull() ?: 100L).coerceAtLeast(100L).toFloat()
                    val pingPts = pingHistory.mapIndexed { idx, p ->
                        val pStep = w / (pingHistory.size - 1).toFloat()
                        val x = idx * pStep
                        val norm = (p.toFloat() / maxPing).coerceIn(0f, 1f)
                        val y = h - (norm * (h * 0.75f)) - (h * 0.08f)
                        Offset(x, y)
                    }
                    val pingPath = Path().apply {
                        moveTo(pingPts.first().x, pingPts.first().y)
                        for (i in 1 until pingPts.size) {
                            val prev = pingPts[i - 1]
                            val curr = pingPts[i]
                            val midX = (prev.x + curr.x) / 2f
                            cubicTo(midX, prev.y, midX, curr.y, curr.x, curr.y)
                        }
                    }
                    drawPath(
                        path = pingPath,
                        color = Color(0xFFFBBF24).copy(alpha = 0.65f),
                        style = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Glowing live endpoint dot
                val lastPt = points.last()
                drawCircle(
                    color = Color(0xFF38BDF8).copy(alpha = 0.35f),
                    radius = 7.dp.toPx(),
                    center = lastPt
                )
                drawCircle(
                    color = Color.White,
                    radius = 3.2.dp.toPx(),
                    center = lastPt
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ChartLegendItem(color = Color(0xFF38BDF8), label = "حمل المعالج (%)")
                ChartLegendItem(color = Color(0xFFFBBF24), label = "استجابة البنج (ms)")
            }
        }
    }
}

@Composable
private fun ChartLegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(text = label, fontSize = 9.5.sp, color = Color(0xFF94A3B8))
    }
}

@Composable
private fun TelemetryMetricTile(
    icon: ImageVector,
    label: String,
    value: String,
    subValue: String,
    accentColor: Color,
    progressFraction: Float? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF0C1833),
        border = BorderStroke(1.dp, Color(0xFF1E325C)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(11.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(15.dp))
                Text(
                    text = label,
                    fontSize = 10.sp,
                    color = Color(0xFF94A3B8),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (progressFraction != null) {
                LinearProgressIndicator(
                    progress = { progressFraction.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = accentColor,
                    trackColor = Color(0xFF1E293B)
                )
            }
            Text(
                text = subValue,
                fontSize = 9.5.sp,
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
