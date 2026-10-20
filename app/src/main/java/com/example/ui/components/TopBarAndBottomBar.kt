package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.ScreenTab
import com.example.ui.theme.*

@Composable
fun TalalProTopBar(
    isAdmin: Boolean,
    soundEnabled: Boolean,
    unreadAlertsCount: Int,
    onToggleDrawer: () -> Unit,
    onToggleSound: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenAdminPanel: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.White,
        contentColor = TextPrimaryLight,
        shadowElevation = 2.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Menu Icon (Left)
            IconButton(
                onClick = onToggleDrawer,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFF1F5F9))
                    .testTag("drawer_menu_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "القائمة",
                    tint = PrimaryNavy
                )
            }

            // Center group: Admin button (if admin) + Sound toggle + Notifications
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // ADMIN BUTTON (Shown only when admin is logged in)
                if (isAdmin) {
                    Surface(
                        onClick = onOpenAdminPanel,
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFFEF4444),
                        modifier = Modifier.testTag("admin_panel_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = "لوحة التحكم",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "لوحة التحكم",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Sound Toggle Button
                Surface(
                    onClick = onToggleSound,
                    shape = RoundedCornerShape(20.dp),
                    color = if (soundEnabled) Color(0xFFEDE9FE) else Color(0xFFF1F5F9),
                    border = borderIf(soundEnabled, Color(0xFF8B5CF6), 1.dp),
                    modifier = Modifier.testTag("toggle_sound_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = if (soundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                            contentDescription = "الصوت",
                            tint = if (soundEnabled) Color(0xFF7C3AED) else TextMutedLight,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (soundEnabled) "صوت مفعّل" else "صوت صامت",
                            color = if (soundEnabled) Color(0xFF6D28D9) else TextSecondaryLight,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Notifications Button
                Surface(
                    onClick = onOpenNotifications,
                    shape = RoundedCornerShape(20.dp),
                    color = if (unreadAlertsCount > 0) Color(0xFFDCFCE7) else Color(0xFFF1F5F9),
                    border = borderIf(unreadAlertsCount > 0, Color(0xFF10B981), 1.dp),
                    modifier = Modifier.testTag("notifications_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        BadgedBox(
                            badge = {
                                if (unreadAlertsCount > 0) {
                                    Badge(
                                        containerColor = Color(0xFFEF4444),
                                        contentColor = Color.White
                                    ) {
                                        Text(text = "$unreadAlertsCount", fontSize = 9.sp)
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "التنبيهات",
                                tint = if (unreadAlertsCount > 0) Color(0xFF059669) else TextSecondaryLight,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = "التنبيهات",
                            color = if (unreadAlertsCount > 0) Color(0xFF047857) else TextSecondaryLight,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Logout Pill (Right)
            Surface(
                onClick = onLogout,
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFFFEE2E2),
                modifier = Modifier.testTag("logout_button")
            ) {
                Text(
                    text = "خروج",
                    color = Color(0xFFB91C1C),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
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
        color = Color.White,
        contentColor = TextPrimaryLight,
        shadowElevation = 8.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(vertical = 6.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItemLight(
                icon = Icons.Default.GridView,
                label = "لوحة القيادة",
                isSelected = currentTab == ScreenTab.DASHBOARD,
                onClick = { onTabSelected(ScreenTab.DASHBOARD) },
                testTag = "tab_dashboard"
            )

            BottomNavItemLight(
                icon = Icons.Default.Dns,
                label = "الراوترات",
                isSelected = currentTab == ScreenTab.ROUTERS,
                onClick = { onTabSelected(ScreenTab.ROUTERS) },
                testTag = "tab_routers"
            )

            BottomNavItemLight(
                icon = Icons.Default.Hub,
                label = "أجهزة الشبكة",
                isSelected = currentTab == ScreenTab.DEVICES,
                onClick = { onTabSelected(ScreenTab.DEVICES) },
                testTag = "tab_devices"
            )

            BottomNavItemLight(
                icon = Icons.Default.Map,
                label = "الخريطة",
                isSelected = currentTab == ScreenTab.MAP,
                onClick = { onTabSelected(ScreenTab.MAP) },
                testTag = "tab_map"
            )

            BottomNavItemLight(
                icon = Icons.Default.SmartToy,
                label = "المساعد",
                isSelected = currentTab == ScreenTab.ASSISTANT,
                onClick = { onTabSelected(ScreenTab.ASSISTANT) },
                testTag = "tab_assistant"
            )

            BottomNavItemLight(
                icon = Icons.Default.Shield,
                label = "المشاكل",
                isSelected = currentTab == ScreenTab.ISSUES,
                onClick = { onTabSelected(ScreenTab.ISSUES) },
                testTag = "tab_issues"
            )
        }
    }
}

@Composable
private fun BottomNavItemLight(
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
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) PrimaryBlue.copy(alpha = 0.12f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .testTag(testTag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) PrimaryBlue else TextMutedLight,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) PrimaryBlue else TextSecondaryLight,
            maxLines = 1
        )
    }
}

private fun borderIf(condition: Boolean, color: Color, width: androidx.compose.ui.unit.Dp): androidx.compose.foundation.BorderStroke? {
    return if (condition) androidx.compose.foundation.BorderStroke(width, color) else null
}
