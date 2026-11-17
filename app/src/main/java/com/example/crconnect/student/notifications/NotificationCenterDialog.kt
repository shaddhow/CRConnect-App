package com.example.crconnect.student.notifications

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.crconnect.student.LocalIsDarkTheme
import com.example.crconnect.student.PlusJakartaSansFontFamily
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationCenterDialog(
    onDismiss: () -> Unit,
    onNavigateToRoutine: () -> Unit = {},
    onNavigateToAcademics: () -> Unit = {},
    onOpenSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val isDark = LocalIsDarkTheme.current
    var notifications by remember { mutableStateOf(NotificationRepository.getNotifications(context)) }
    var selectedFilter by remember { mutableStateOf("All") }

    val unreadCount = remember(notifications) { notifications.count { !it.isRead } }

    val filteredList = remember(notifications, selectedFilter) {
        when (selectedFilter) {
            "Classes" -> notifications.filter { it.type == NotificationType.CLASS_REMINDER }
            "Notices" -> notifications.filter { it.type == NotificationType.CR_NOTICE }
            "Alerts" -> notifications.filter { it.type == NotificationType.ATTENDANCE_ALERT }
            else -> notifications
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = if (isDark) Color(0xFF0F172A) else Color(0xFFFAFCFF),
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 6.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (isDark) Color(0x33FFFFFF) else Color(0x33000000))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 8.dp)
                .navigationBarsPadding()
        ) {
            // Header Row
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
                            .size(42.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isDark) Color(0x263B82F6) else Color(0x140066FF))
                            .border(1.dp, if (isDark) Color(0x333B82F6) else Color(0x260066FF), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = if (isDark) Color(0xFF60A5FA) else Color(0xFF0066FF),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Notifications",
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
                            )
                            if (unreadCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFFEF4444))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "$unreadCount new",
                                        fontFamily = PlusJakartaSansFontFamily,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                        Text(
                            text = "Routine alerts, CR notices & reminders",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 12.sp,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Preferences",
                            tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Filter Chips + Actions
            if (notifications.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val filters = listOf("All", "Classes", "Notices", "Alerts")
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(filters) { filter ->
                            val isSelected = selectedFilter == filter
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) {
                                    if (isDark) Color(0xFF1E3A8A) else Color(0xFF0066FF)
                                } else {
                                    if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
                                },
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) Color.Transparent
                                    else if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
                                ),
                                onClick = { selectedFilter = filter },
                                modifier = Modifier.height(32.dp)
                            ) {
                                Box(
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = filter,
                                        fontFamily = PlusJakartaSansFontFamily,
                                        fontSize = 11.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White
                                        else if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "Mark read",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDark) Color(0xFF60A5FA) else Color(0xFF0066FF),
                        modifier = Modifier
                            .clickable {
                                NotificationRepository.markAllAsRead(context)
                                notifications = NotificationRepository.getNotifications(context)
                            }
                            .padding(4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            // Body: Content-Adaptive (Sleek Empty State or Scrollable List)
            if (filteredList.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = if (isDark) Color(0xFF161F30) else Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF24354D) else Color(0xFFE2E8F0)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = if (isDark) listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                                        else listOf(Color(0xFFEFF6FF), Color(0xFFDBEAFE))
                                    )
                                )
                                .border(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFBFDBFE), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DoneAll,
                                contentDescription = null,
                                tint = if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Text(
                            text = "You're all caught up",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
                        )

                        Text(
                            text = if (notifications.isEmpty()) "No new alerts or CR notices right now." else "No items under '$selectedFilter'.",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 12.5.sp,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Compact Status Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isDark) Color(0x1F10B981) else Color(0x14059669))
                                .border(1.dp, if (isDark) Color(0x3310B981) else Color(0x24059669), RoundedCornerShape(20.dp))
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "Routine monitoring & class alarms active",
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) Color(0xFF34D399) else Color(0xFF059669)
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 440.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(filteredList, key = { it.id }) { item ->
                        NotificationItemRow(
                            item = item,
                            isDark = isDark,
                            onClick = {
                                if (!item.isRead) {
                                    NotificationRepository.markAsRead(context, item.id)
                                    notifications = NotificationRepository.getNotifications(context)
                                }
                                if (item.type == NotificationType.CLASS_REMINDER) {
                                    onDismiss()
                                    onNavigateToRoutine()
                                } else if (item.type == NotificationType.ATTENDANCE_ALERT) {
                                    onDismiss()
                                    onNavigateToAcademics()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationItemRow(
    item: InAppNotificationModel,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val (iconColor, bgColor, icon) = when (item.type) {
        NotificationType.CLASS_REMINDER -> Triple(
            if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB),
            if (isDark) Color(0x263B82F6) else Color(0x140066FF),
            Icons.Default.AccessTime
        )
        NotificationType.CR_NOTICE -> Triple(
            if (isDark) Color(0xFFA78BFA) else Color(0xFF7C3AED),
            if (isDark) Color(0x268B5CF6) else Color(0x147C3AED),
            Icons.Default.Campaign
        )
        NotificationType.ATTENDANCE_ALERT -> Triple(
            if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706),
            if (isDark) Color(0x26F59E0B) else Color(0x14D97706),
            Icons.Default.Warning
        )
        NotificationType.ELECTION_UPDATE -> Triple(
            if (isDark) Color(0xFF34D399) else Color(0xFF059669),
            if (isDark) Color(0x2610B981) else Color(0x14059669),
            Icons.Default.HowToVote
        )
        NotificationType.GENERAL -> Triple(
            if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
            if (isDark) Color(0x2664748B) else Color(0x1464748B),
            Icons.Default.Info
        )
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isDark) {
            if (item.isRead) Color(0xFF161F30).copy(alpha = 0.5f) else Color(0xFF1E293B)
        } else {
            if (item.isRead) Color(0xFFF8FAFC) else Color.White
        },
        border = BorderStroke(
            1.dp,
            if (!item.isRead) iconColor.copy(alpha = 0.35f)
            else (if (isDark) Color(0xFF334155).copy(alpha = 0.4f) else Color(0xFFE2E8F0))
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(13.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.title,
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 13.5.sp,
                        fontWeight = if (!item.isRead) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (!item.isRead) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(iconColor)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = item.body,
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 12.sp,
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = formatRelativeTimestamp(item.timestamp),
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 10.5.sp,
                    color = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
                )
            }
        }
    }
}

private fun formatRelativeTimestamp(timeMs: Long): String {
    val now = System.currentTimeMillis()
    val diffMs = now - timeMs
    val diffMins = diffMs / 60000
    val diffHours = diffMins / 60
    val diffDays = diffHours / 24

    return when {
        diffMins < 1 -> "Just now"
        diffMins < 60 -> "${diffMins}m ago"
        diffHours < 24 -> "${diffHours}h ago"
        diffDays == 1L -> "Yesterday"
        diffDays < 7L -> "${diffDays}d ago"
        else -> SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(timeMs))
    }
}
