package com.example.crconnect.student.notifications

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.crconnect.student.GlassySmoothSwitch
import com.example.crconnect.student.LocalIsDarkTheme
import com.example.crconnect.student.PlusJakartaSansFontFamily

@Composable
fun NotificationSettingsDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isDark = LocalIsDarkTheme.current

    var classReminder by remember { mutableStateOf(NotificationRepository.isClassReminderEnabled(context)) }
    var morningBriefing by remember { mutableStateOf(NotificationRepository.isMorningBriefingEnabled(context)) }
    var crAnnouncements by remember { mutableStateOf(NotificationRepository.isCrAnnouncementEnabled(context)) }
    var attendanceAlerts by remember { mutableStateOf(NotificationRepository.isAttendanceAlertEnabled(context)) }

    val dialogBg = if (isDark) Color(0xFF131B2E) else Color.White
    val dialogBorder = if (isDark) Color(0xFF24354D) else Color(0xFFE2E8F0)
    val cardBg = if (isDark) Color(0xFF192339) else Color(0xFFF8FAFC)
    val cardBorder = if (isDark) Color(0xFF283A55) else Color(0xFFE2E8F0)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .shadow(
                    elevation = if (isDark) 24.dp else 18.dp,
                    shape = RoundedCornerShape(30.dp),
                    spotColor = Color(0xFF0066FF).copy(alpha = if (isDark) 0.3f else 0.18f)
                ),
            shape = RoundedCornerShape(30.dp),
            color = dialogBg,
            border = BorderStroke(1.dp, dialogBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(Color(0xFF1D4ED8), Color(0xFF0066FF))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "Notification Settings",
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
                            )
                            Text(
                                text = "Customize alerts & class reminders",
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 12.sp,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier.size(34.dp),
                        shape = CircleShape,
                        color = cardBg,
                        border = BorderStroke(1.dp, cardBorder),
                        onClick = onDismiss
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }

                // Warning Banner if System Notifications are blocked
                val hasSystemPermission = remember {
                    NotificationPermissionHelper.hasNotificationPermission(context)
                }
                if (!hasSystemPermission) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isDark) Color(0xFF7F1D1D).copy(alpha = 0.35f) else Color(0xFFFEF2F2),
                        border = BorderStroke(1.dp, if (isDark) Color(0xFF991B1B) else Color(0xFFFECACA)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (isDark) Color(0xFFF87171) else Color(0xFFDC2626),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "System alerts blocked",
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFFFCA5A5) else Color(0xFFB91C1C)
                                )
                                Text(
                                    text = "Enable in phone settings for status bar alerts.",
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontSize = 11.sp,
                                    color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF475569)
                                )
                            }
                            TextButton(
                                onClick = {
                                    NotificationPermissionHelper.openNotificationSettings(context)
                                },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Enable",
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFF60A5FA) else Color(0xFF0066FF)
                                )
                            }
                        }
                    }
                }

                // Toggle Card
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = cardBg,
                    border = BorderStroke(1.dp, cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        NotificationToggleRow(
                            icon = Icons.Default.AccessTime,
                            iconTint = if (isDark) Color(0xFF60A5FA) else Color(0xFF0066FF),
                            iconBg = if (isDark) Color(0x263B82F6) else Color(0x140066FF),
                            title = "Class reminders",
                            subtitle = "Alert 15m before class with room",
                            checked = classReminder,
                            isDark = isDark,
                            onCheckedChange = {
                                classReminder = it
                                NotificationRepository.setClassReminderEnabled(context, it)
                            }
                        )

                        HorizontalDivider(color = cardBorder, thickness = 1.dp)

                        NotificationToggleRow(
                            icon = Icons.Default.WbSunny,
                            iconTint = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706),
                            iconBg = if (isDark) Color(0x26F59E0B) else Color(0x14D97706),
                            title = "Morning briefing",
                            subtitle = "Daily 8:00 AM class summary",
                            checked = morningBriefing,
                            isDark = isDark,
                            onCheckedChange = {
                                morningBriefing = it
                                NotificationRepository.setMorningBriefingEnabled(context, it)
                            }
                        )

                        HorizontalDivider(color = cardBorder, thickness = 1.dp)

                        NotificationToggleRow(
                            icon = Icons.Default.Campaign,
                            iconTint = if (isDark) Color(0xFFA78BFA) else Color(0xFF7C3AED),
                            iconBg = if (isDark) Color(0x268B5CF6) else Color(0x147C3AED),
                            title = "CR announcements",
                            subtitle = "Instant push for class updates",
                            checked = crAnnouncements,
                            isDark = isDark,
                            onCheckedChange = {
                                crAnnouncements = it
                                NotificationRepository.setCrAnnouncementEnabled(context, it)
                            }
                        )

                        HorizontalDivider(color = cardBorder, thickness = 1.dp)

                        NotificationToggleRow(
                            icon = Icons.Default.VerifiedUser,
                            iconTint = if (isDark) Color(0xFF34D399) else Color(0xFF059669),
                            iconBg = if (isDark) Color(0x2610B981) else Color(0x14059669),
                            title = "Attendance warnings",
                            subtitle = "Alert when below 75% safe zone",
                            checked = attendanceAlerts,
                            isDark = isDark,
                            onCheckedChange = {
                                attendanceAlerts = it
                                NotificationRepository.setAttendanceAlertEnabled(context, it)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationToggleRow(
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    isDark: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontFamily = PlusJakartaSansFontFamily,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
            )
            Text(
                text = subtitle,
                fontFamily = PlusJakartaSansFontFamily,
                fontSize = 11.5.sp,
                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
            )
        }

        GlassySmoothSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
