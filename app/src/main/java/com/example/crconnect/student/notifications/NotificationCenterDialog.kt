package com.example.crconnect.student.notifications

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.crconnect.student.FullScreenSubPageDialog
import com.example.crconnect.student.LocalIsDarkTheme
import com.example.crconnect.student.PlusJakartaSansFontFamily
import com.example.crconnect.student.getPortalGradient
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NotificationCenterDialog(
    onDismiss: () -> Unit,
    onNavigateToRoutine: () -> Unit = {},
    onNavigateToAcademics: () -> Unit = {},
    onOpenSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val isDark = LocalIsDarkTheme.current
    val portalGradient = getPortalGradient(isDark)
    var notifications by remember { mutableStateOf(NotificationRepository.getNotifications(context)) }
    var selectedFilter by remember { mutableStateOf("All") }

    val unreadCount = remember(notifications) { notifications.count { !it.isRead } }
    val classReminderOn = remember { NotificationRepository.isClassReminderEnabled(context) }
    val crNoticeOn = remember { NotificationRepository.isCrAnnouncementEnabled(context) }

    val filteredList = remember(notifications, selectedFilter) {
        when (selectedFilter) {
            "Classes" -> notifications.filter { it.type == NotificationType.CLASS_REMINDER }
            "Notices" -> notifications.filter { it.type == NotificationType.CR_NOTICE }
            "Alerts" -> notifications.filter { it.type == NotificationType.ATTENDANCE_ALERT }
            else -> notifications
        }
    }

    FullScreenSubPageDialog(
        onDismissRequest = onDismiss,
        isDark = isDark
    ) {
        BackHandler { onDismiss() }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(portalGradient)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(42.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    onClick = onDismiss
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Notifications",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Routine alerts, CR notices & updates",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    modifier = Modifier.size(42.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    onClick = onOpenSettings
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Notification Preferences",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Hero Summary Card (matching RoutineTab's blue gradient hero card)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .shadow(
                        elevation = if (isDark) 0.dp else 12.dp,
                        shape = RoundedCornerShape(24.dp),
                        spotColor = Color(0xFF0066FF).copy(alpha = 0.25f)
                    )
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = if (isDark) {
                                listOf(Color(0xFF1E3A8A), Color(0xFF0F172A))
                            } else {
                                listOf(Color(0xFF1D4ED8), Color(0xFF0066FF))
                            }
                        )
                    )
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(24.dp)
                    )
            ) {
                // Decorative subtle circles matching RoutineTab hero card
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 28.dp, y = (-28).dp)
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .offset(x = (-24).dp, y = 32.dp)
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.06f))
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "INBOX STATUS",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                            color = Color.White.copy(alpha = 0.78f)
                        )

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color.White.copy(alpha = 0.16f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
                            onClick = onOpenSettings
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "Preferences",
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = when {
                            unreadCount > 0 -> "$unreadCount Unread Alert${if (unreadCount > 1) "s" else ""}"
                            notifications.isNotEmpty() -> "All Caught Up"
                            else -> "Quiet Right Now"
                        },
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = when {
                            notifications.isNotEmpty() -> "${notifications.size} total saved · Tap any item to view"
                            else -> "Class reminders & CR broadcasts appear here"
                        },
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 12.5.sp,
                        color = Color.White.copy(alpha = 0.82f)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Active status chips
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HeroStatusChip(
                            label = if (classReminderOn) "Class Alerts On" else "Class Alerts Off",
                            active = classReminderOn
                        )
                        HeroStatusChip(
                            label = if (crNoticeOn) "CR Notices On" else "CR Notices Off",
                            active = crNoticeOn
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Filter Chips + Clear / Mark Read Row
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val filters = listOf("All", "Classes", "Notices", "Alerts")
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(filters) { filter ->
                            val isSelected = selectedFilter == filter
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) Color(0xFF0066FF) else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outline
                                ),
                                onClick = { selectedFilter = filter },
                                modifier = Modifier.height(34.dp)
                            ) {
                                Box(
                                    modifier = Modifier.padding(horizontal = 14.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = filter,
                                        fontFamily = PlusJakartaSansFontFamily,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                if (notifications.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recent activity",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (unreadCount > 0) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isDark) Color(0x1F3B82F6) else Color(0x140066FF),
                                    onClick = {
                                        NotificationRepository.markAllAsRead(context)
                                        notifications = NotificationRepository.getNotifications(context)
                                    }
                                ) {
                                    Text(
                                        text = "Mark read",
                                        fontFamily = PlusJakartaSansFontFamily,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isDark) Color(0xFF60A5FA) else Color(0xFF0066FF),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isDark) Color(0x1FEF4444) else Color(0x14DC2626),
                                onClick = {
                                    NotificationRepository.clearAll(context)
                                    notifications = emptyList()
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = null,
                                        tint = if (isDark) Color(0xFFFCA5A5) else Color(0xFFDC2626),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Clear all",
                                        fontFamily = PlusJakartaSansFontFamily,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isDark) Color(0xFFFCA5A5) else Color(0xFFDC2626)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Body: Empty State or Scrollable List
            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isDark) Color(0x263B82F6) else Color(0x140066FF))
                                    .border(
                                        1.dp,
                                        if (isDark) Color(0x333B82F6) else Color(0x260066FF),
                                        RoundedCornerShape(16.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsNone,
                                    contentDescription = null,
                                    tint = if (isDark) Color(0xFF60A5FA) else Color(0xFF0066FF),
                                    modifier = Modifier.size(26.dp)
                                )
                            }

                            Text(
                                text = if (notifications.isEmpty()) "No notifications yet" else "Nothing in $selectedFilter",
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 15.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Text(
                                text = if (notifications.isEmpty()) {
                                    "When your CR posts an announcement or a class reminder triggers, it will be saved here."
                                } else {
                                    "Try another filter tab to view earlier notifications."
                                },
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 12.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
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
private fun HeroStatusChip(
    label: String,
    active: Boolean
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.14f))
            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(if (active) Color(0xFF4ADE80) else Color(0xFFFBBF24))
        )
        Text(
            text = label,
            fontFamily = PlusJakartaSansFontFamily,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
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
            if (isDark) Color(0xFF60A5FA) else Color(0xFF0066FF),
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
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (!item.isRead) iconColor.copy(alpha = 0.45f) else MaterialTheme.colorScheme.outline
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(19.dp)
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
                        fontSize = 14.sp,
                        fontWeight = if (!item.isRead) FontWeight.Bold else FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (!item.isRead) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(iconColor)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = item.body,
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 17.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = formatRelativeTimestamp(item.timestamp),
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
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
