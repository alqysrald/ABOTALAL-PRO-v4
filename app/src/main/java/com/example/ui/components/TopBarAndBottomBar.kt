package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.ScreenTab
import com.example.ui.theme.*

@Composable
fun TalalProTopBar(
    soundEnabled: Boolean,
    unreadAlertsCount: Int,
    onToggleDrawer: () -> Unit,
    onToggleSound: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenPowerControl: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFF0F172A),
        contentColor = Color.White,
        shadowElevation = 6.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Right/Start: SVG Menu Button + Brand Identity
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        onClick = onToggleDrawer,
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF1E293B),
                        border = BorderStroke(1.dp, Color(0xFF334155)),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .size(48.dp)
                            .testTag("drawer_menu_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_svg_menu),
                                contentDescription = "فتح القائمة الجانبية",
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "TALAL PRO",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.8.sp,
                                maxLines = 1
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF0284C7).copy(alpha = 0.25f),
                                border = BorderStroke(0.8.dp, Color(0xFF38BDF8).copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = "v4.2",
                                    color = Color(0xFF38BDF8),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = "إدارة شبكات المايكروتك",
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Left/End: Luxury SVG Action Icon Strip (Sound | Notifications | Power Control | Logout)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // 1. Sound Toggle SVG Icon
                    Surface(
                        onClick = onToggleSound,
                        shape = RoundedCornerShape(14.dp),
                        color = if (soundEnabled) Color(0xFF4C1D95).copy(alpha = 0.45f) else Color(0xFF1E293B),
                        border = BorderStroke(
                            1.dp,
                            if (soundEnabled) Color(0xFF8B5CF6) else Color(0xFF334155)
                        ),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .size(48.dp)
                            .testTag("toggle_sound_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(
                                    id = if (soundEnabled) R.drawable.ic_svg_sound_on else R.drawable.ic_svg_sound_off
                                ),
                                contentDescription = if (soundEnabled) "إيقاف صوت التنبيهات" else "تفعيل صوت التنبيهات",
                                tint = if (soundEnabled) Color(0xFFC4B5FD) else Color(0xFF94A3B8),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // 2. Notifications SVG Icon with Badge
                    Surface(
                        onClick = onOpenNotifications,
                        shape = RoundedCornerShape(14.dp),
                        color = if (unreadAlertsCount > 0) Color(0xFF065F46).copy(alpha = 0.45f) else Color(0xFF1E293B),
                        border = BorderStroke(
                            1.dp,
                            if (unreadAlertsCount > 0) Color(0xFF10B981) else Color(0xFF334155)
                        ),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .size(48.dp)
                            .testTag("notifications_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            BadgedBox(
                                badge = {
                                    if (unreadAlertsCount > 0) {
                                        Badge(
                                            containerColor = Color(0xFFEF4444),
                                            contentColor = Color.White
                                        ) {
                                            Text(
                                                text = if (unreadAlertsCount > 99) "99+" else "$unreadAlertsCount",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_svg_notifications),
                                    contentDescription = "عرض الإشعارات والتنبيهات",
                                    tint = if (unreadAlertsCount > 0) Color(0xFF34D399) else Color(0xFFCBD5E1),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // 3. Router Power (Shutdown / Reboot) SVG Icon
                    Surface(
                        onClick = onOpenPowerControl,
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF78350F).copy(alpha = 0.35f),
                        border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.7f)),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .size(48.dp)
                            .testTag("router_power_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_svg_power),
                                contentDescription = "إيقاف أو إعادة تشغيل الراوتر",
                                tint = Color(0xFFFBBF24),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // 4. Logout SVG Icon
                    Surface(
                        onClick = onLogout,
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF7F1D1D).copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.65f)),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .size(48.dp)
                            .testTag("logout_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_svg_logout),
                                contentDescription = "تسجيل الخروج أو تبديل الحساب",
                                tint = Color(0xFFFCA5A5),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color(0xFF1E293B),
                                Color(0xFF38BDF8).copy(alpha = 0.45f),
                                Color(0xFF1E293B)
                            )
                        )
                    )
            )
        }
    }
}

@Composable
fun TalalProBottomBar(
    currentTab: ScreenTab,
    onTabSelected: (ScreenTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFF0F172A),
        contentColor = Color.White,
        shadowElevation = 10.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            HorizontalDivider(color = Color(0xFF1E293B), thickness = 1.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(vertical = 6.dp, horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomNavItemLuxury(
                    icon = Icons.Default.GridView,
                    label = "القيادة",
                    isSelected = currentTab == ScreenTab.DASHBOARD,
                    onClick = { onTabSelected(ScreenTab.DASHBOARD) },
                    testTag = "tab_dashboard"
                )

                BottomNavItemLuxury(
                    icon = Icons.Default.Dns,
                    label = "الراوترات",
                    isSelected = currentTab == ScreenTab.ROUTERS,
                    onClick = { onTabSelected(ScreenTab.ROUTERS) },
                    testTag = "tab_routers"
                )

                BottomNavItemLuxury(
                    icon = Icons.Default.Hub,
                    label = "الأجهزة",
                    isSelected = currentTab == ScreenTab.DEVICES,
                    onClick = { onTabSelected(ScreenTab.DEVICES) },
                    testTag = "tab_devices"
                )

                BottomNavItemLuxury(
                    icon = Icons.Default.Map,
                    label = "الخريطة",
                    isSelected = currentTab == ScreenTab.MAP,
                    onClick = { onTabSelected(ScreenTab.MAP) },
                    testTag = "tab_map"
                )

                BottomNavItemLuxury(
                    icon = Icons.Default.SmartToy,
                    label = "المساعد",
                    isSelected = currentTab == ScreenTab.ASSISTANT,
                    onClick = { onTabSelected(ScreenTab.ASSISTANT) },
                    testTag = "tab_assistant"
                )

                BottomNavItemLuxury(
                    icon = Icons.Default.Shield,
                    label = "المشاكل",
                    isSelected = currentTab == ScreenTab.ISSUES,
                    onClick = { onTabSelected(ScreenTab.ISSUES) },
                    testTag = "tab_issues"
                )
            }
        }
    }
}

@Composable
private fun BottomNavItemLuxury(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) Color(0xFF38BDF8).copy(alpha = 0.16f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag(testTag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) Color(0xFF38BDF8) else Color(0xFF94A3B8),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color(0xFF38BDF8) else Color(0xFFCBD5E1),
            maxLines = 1
        )
    }
}
