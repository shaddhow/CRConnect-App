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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.crconnect.student.LocalIsDarkTheme
import com.example.crconnect.student.PlusJakartaSansFontFamily
import com.example.crconnect.student.GlassySmoothSwitch

@Composable
fun NotificationSettingsDialog(
    onDismiss: () -> Unit,
    onTestNotification: () -> Unit = {}
) {
    val context = LocalContext.current
    val isDark = LocalIsDarkTheme.current

    var classReminder by remember { mutableStateOf(NotificationRepository.isClassReminderEnabled(context)) }
    var morningBriefing by remember { mutableStateOf(NotificationRepository.isMorningBriefingEnabled(context)) }
    var crAnnouncements by remember { mutableStateOf(NotificationRepository.isCrAnnouncementEnabled(context)) }
    var attendanceAlerts by remember { mutableStateOf(NotificationRepository.isAttendanceAlertEnabled(context)) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .clip(RoundedCornerShape(26.dp))
                .border(
                    width = 1.dp,
                    color = if (isDark) Color(0xFF334155).copy(alpha = 0.6f) else Color(0xFFE2E8F0),
                    shape = RoundedCornerShape(26.dp)
                ),
            shape = RoundedCornerShape(26.dp),
            color = if (isDark) Color(0xFF0F172A) else Color.White,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0x333B82F6) else Color(0x1A2563EB)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "Notification Preferences",
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
                            )
                            Text(
                                text = "Customize routine & notice alerts",
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 11.5.sp,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                        )
                    }
                }

                HorizontalDivider(
                    color = if (isDark) Color(0xFF334155).copy(alpha = 0.5f) else Color(0xFFE2E8F0),
                    thickness = 1.dp
                )

                // Item 1: Class Reminders
                NotificationToggleRow(
                    title = "Class reminders (15m before)",
                    subtitle = "Get alerted with room & start time before class starts",
                    checked = classReminder,
                    isDark = isDark,
                    onCheckedChange = {
                        classReminder = it
                        NotificationRepository.setClassReminderEnabled(context, it)
                    }
                )

                // Item 2: Morning Briefing
                NotificationToggleRow(
                    title = "Morning routine briefing (8:00 AM)",
                    subtitle = "Daily summary of today's classes and timing",
                    checked = morningBriefing,
                    isDark = isDark,
                    onCheckedChange = {
                        morningBriefing = it
                        NotificationRepository.setMorningBriefingEnabled(context, it)
                    }
                )

                // Item 3: CR Announcements
                NotificationToggleRow(
                    title = "CR announcements & urgent notices",
                    subtitle = "Immediate push alerts when CR broadcasts class changes",
                    checked = crAnnouncements,
                    isDark = isDark,
                    onCheckedChange = {
                        crAnnouncements = it
                        NotificationRepository.setCrAnnouncementEnabled(context, it)
                    }
                )

                // Item 4: Low Attendance Warning
                NotificationToggleRow(
                    title = "Attendance safety warnings",
                    subtitle = "Alert when any course drops below 75% exam safe zone",
                    checked = attendanceAlerts,
                    isDark = isDark,
                    onCheckedChange = {
                        attendanceAlerts = it
                        NotificationRepository.setAttendanceAlertEnabled(context, it)
                    }
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Test Notification Button
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        RoutineNotificationHelper.showClassReminderNotification(
                            context = context,
                            courseName = "CSE 341 (MJ)",
                            timeSlot = "09:45 AM - 11:15 AM",
                            room = "2808"
                        )
                        onTestNotification()
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 11.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = null,
                            tint = if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Send test class reminder",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    isDark: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                fontFamily = PlusJakartaSansFontFamily,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = subtitle,
                fontFamily = PlusJakartaSansFontFamily,
                fontSize = 11.sp,
                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                lineHeight = 15.sp
            )
        }

        GlassySmoothSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

