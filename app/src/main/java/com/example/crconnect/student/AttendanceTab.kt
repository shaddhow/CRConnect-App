package com.example.crconnect.student

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar

@Composable
fun AttendanceTab(
    uiState: StudentPortalUiState,
    onRefreshHistory: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = LocalIsDarkTheme.current
    val repo = RoutineRepository(context)
    val currentWeekRange = repo.getSundayWeekDateRange(Calendar.getInstance())

    // 1. Calculate live attendance for current Sunday-to-Saturday week
    val classMap = uiState.classMap
    val allClassesWithDay = classMap.flatMap { (dayIdx, classes) ->
        classes.map { dayIdx to it }
    }
    val evaluatedClasses = allClassesWithDay.filter { (dayIdx, item) ->
        val status = getClassTimingStatus(dayIdx, item.timeSlot)
        status == ClassTimingStatus.PAST || item.attendanceState != null
    }
    val attendedClasses = evaluatedClasses.filter { (dayIdx, item) ->
        getEffectiveAttendanceState(dayIdx, item) == "attended"
    }
    val missedClasses = evaluatedClasses.filter { (dayIdx, item) ->
        getEffectiveAttendanceState(dayIdx, item) == "missed"
    }
    val dynamicAttendancePct = if (evaluatedClasses.isNotEmpty()) {
        ((attendedClasses.size.toFloat() / evaluatedClasses.size) * 100).toInt()
    } else {
        0
    }

    val progress = if (evaluatedClasses.isNotEmpty()) dynamicAttendancePct / 100f else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 600),
        label = "liveAttendanceProgress"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. Hero Radial Progress Gauge Card ---
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = if (isDark) 0.dp else 4.dp,
                        shape = RoundedCornerShape(26.dp)
                    ),
                shape = RoundedCornerShape(26.dp),
                color = if (isDark) Color(0xFF131B2E) else Color.White,
                border = BorderStroke(1.dp, if (isDark) Color(0xFF24354D) else Color(0xFFE2E8F0))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header Row: Live indicator pill & current week date range
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF10B981).copy(alpha = if (isDark) 0.18f else 0.12f),
                            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = if (isDark) 0.35f else 0.25f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(if (isDark) Color(0xFF4ADE80) else Color(0xFF10B981))
                                )
                                Text(
                                    text = "WEEK LIVE",
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFF4ADE80) else Color(0xFF10B981),
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, if (isDark) Color(0x33FFFFFF) else Color(0xFFE2E8F0))
                        ) {
                            Text(
                                text = currentWeekRange,
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Main Metric Row: Details on left, Circular Donut Gauge on right
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text(
                                text = "Weekly Attendance",
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Text(
                                text = if (evaluatedClasses.isNotEmpty()) {
                                    "${attendedClasses.size} of ${evaluatedClasses.size} classes attended"
                                } else {
                                    "0 classes evaluated yet"
                                },
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Status Chip
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (evaluatedClasses.isEmpty()) {
                                    if (isDark) Color(0xFF1E283A) else Color(0xFFF1F5F9)
                                } else if (dynamicAttendancePct >= 80) {
                                    Color(0xFF10B981).copy(alpha = if (isDark) 0.2f else 0.12f)
                                } else if (dynamicAttendancePct >= 60) {
                                    Color(0xFF3B82F6).copy(alpha = if (isDark) 0.2f else 0.12f)
                                } else {
                                    Color(0xFFEF4444).copy(alpha = if (isDark) 0.2f else 0.12f)
                                },
                                border = BorderStroke(
                                    1.dp,
                                    if (evaluatedClasses.isEmpty()) {
                                        if (isDark) Color(0x33FFFFFF) else Color(0xFFE2E8F0)
                                    } else if (dynamicAttendancePct >= 80) {
                                        Color(0xFF10B981).copy(alpha = 0.3f)
                                    } else if (dynamicAttendancePct >= 60) {
                                        Color(0xFF3B82F6).copy(alpha = 0.3f)
                                    } else {
                                        Color(0xFFEF4444).copy(alpha = 0.3f)
                                    }
                                ),
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (evaluatedClasses.isEmpty()) {
                                                    Color.Gray
                                                } else if (dynamicAttendancePct >= 80) {
                                                    Color(0xFF10B981)
                                                } else if (dynamicAttendancePct >= 60) {
                                                    Color(0xFF3B82F6)
                                                } else {
                                                    Color(0xFFEF4444)
                                                }
                                            )
                                    )
                                    Text(
                                        text = if (evaluatedClasses.isEmpty()) {
                                            "New Week Cycle"
                                        } else if (dynamicAttendancePct >= 80) {
                                            "Excellent · On Track"
                                        } else if (dynamicAttendancePct >= 60) {
                                            "Good · Passing"
                                        } else {
                                            "Needs Attention"
                                        },
                                        fontFamily = PlusJakartaSansFontFamily,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (evaluatedClasses.isEmpty()) {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        } else if (dynamicAttendancePct >= 80) {
                                            if (isDark) Color(0xFF4ADE80) else Color(0xFF10B981)
                                        } else if (dynamicAttendancePct >= 60) {
                                            if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB)
                                        } else {
                                            if (isDark) Color(0xFFF87171) else Color(0xFFEF4444)
                                        }
                                    )
                                }
                            }
                        }

                        // Right: Circular Donut Radial Gauge
                        AttendanceRadialGauge(
                            percentage = dynamicAttendancePct,
                            progress = animatedProgress,
                            isDark = isDark
                        )
                    }
                }
            }
        }

        // --- 2. 3 Bento Metric Tiles ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                BentoMetricTile(
                    label = "Attended",
                    value = "${attendedClasses.size}",
                    subtitle = if (evaluatedClasses.isNotEmpty()) "${(attendedClasses.size * 100 / evaluatedClasses.size)}% of week" else "0%",
                    accentColor = if (isDark) Color(0xFF4ADE80) else Color(0xFF10B981),
                    icon = Icons.Default.CheckCircle,
                    isDark = isDark,
                    modifier = Modifier.weight(1f)
                )

                BentoMetricTile(
                    label = "Missed",
                    value = "${missedClasses.size}",
                    subtitle = if (evaluatedClasses.isNotEmpty()) "${(missedClasses.size * 100 / evaluatedClasses.size)}% of week" else "0%",
                    accentColor = if (isDark) Color(0xFFF87171) else Color(0xFFEF4444),
                    icon = Icons.Default.Cancel,
                    isDark = isDark,
                    modifier = Modifier.weight(1f)
                )

                BentoMetricTile(
                    label = "Total",
                    value = "${evaluatedClasses.size}",
                    subtitle = "Classes eval",
                    accentColor = if (isDark) Color(0xFF60A5FA) else Color(0xFF0066FF),
                    icon = Icons.Default.FactCheck,
                    isDark = isDark,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // --- 3. Weekly Class-Wise Log Breakdown Section ---
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, bottom = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Weekly Class Breakdown (${evaluatedClasses.size})",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Detailed log of routine classes evaluated this week",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (evaluatedClasses.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = if (isDark) Color(0xFF131B2E) else Color.White,
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF24354D) else Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = "No Classes Evaluated Yet",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Class records will populate here automatically as their schedule times pass or attendance is taken.",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(evaluatedClasses, key = { (dayIdx, item) -> "${dayIdx}_${item.id}_${item.timeSlot}" }) { (dayIdx, classModel) ->
                val isAttended = getEffectiveAttendanceState(dayIdx, classModel) == "attended"
                ClassAttendanceLogItem(
                    dayIndex = dayIdx,
                    classModel = classModel,
                    isAttended = isAttended,
                    isDark = isDark
                )
            }
        }

        // --- 4. Sunday Reset Explanation Banner ---
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = if (isDark) Color(0xFF131B2E) else Color.White,
                border = BorderStroke(1.dp, if (isDark) Color(0xFF24354D) else Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF0066FF).copy(alpha = if (isDark) 0.2f else 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Info",
                            tint = Color(0xFF0066FF),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "Sunday Cycle Auto-Archive",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Weekly attendance runs Sunday to Saturday. Every Sunday at 00:00, previous week is saved to archives and resets to 0%.",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        // --- 5. History Section Header ---
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, bottom = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "History",
                        tint = if (isDark) Color(0xFF60A5FA) else Color(0xFF0066FF),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Saved Weekly Attendance",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = (if (isDark) Color(0xFF60A5FA) else Color(0xFF0066FF)).copy(alpha = if (isDark) 0.18f else 0.1f),
                    border = BorderStroke(1.dp, (if (isDark) Color(0xFF60A5FA) else Color(0xFF0066FF)).copy(alpha = 0.25f))
                ) {
                    Text(
                        text = "${uiState.attendanceArchives.size} Archived",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFF60A5FA) else Color(0xFF0066FF),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }

        // --- 6. Saved List of Previous Weeks ---
        if (uiState.attendanceArchives.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = if (isDark) Color(0xFF131B2E) else Color.White,
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF24354D) else Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 28.dp, horizontal = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = (if (isDark) Color(0xFF3B82F6) else Color(0xFF0066FF)).copy(alpha = if (isDark) 0.2f else 0.1f),
                            modifier = Modifier.size(50.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = "Archive",
                                    tint = if (isDark) Color(0xFF60A5FA) else Color(0xFF0066FF),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Text(
                            text = "No Previous Weeks Archived Yet",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Completed week summaries (date range, attended/total classes, percentage) will be archived here automatically each Sunday.",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            lineHeight = 17.sp
                        )
                    }
                }
            }
        } else {
            items(uiState.attendanceArchives, key = { it.weekKey }) { archive ->
                ArchiveWeekCard(archive = archive, isDark = isDark)
            }
        }

        // Bottom spacing for navigation bar
        item {
            Spacer(modifier = Modifier.height(96.dp))
        }
    }
}

@Composable
fun AttendanceRadialGauge(
    percentage: Int,
    progress: Float,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val trackColor = if (isDark) Color(0xFF1E283A) else Color(0xFFE2E8F0)
    val gradientColors = when {
        percentage >= 80 -> listOf(Color(0xFF10B981), Color(0xFF34D399))
        percentage >= 60 -> listOf(Color(0xFF2563EB), Color(0xFF60A5FA))
        else -> listOf(Color(0xFFEF4444), Color(0xFFF97316))
    }
    val textColor = when {
        percentage >= 80 -> if (isDark) Color(0xFF4ADE80) else Color(0xFF10B981)
        percentage >= 60 -> if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB)
        else -> if (isDark) Color(0xFFF87171) else Color(0xFFEF4444)
    }

    Box(
        modifier = modifier.size(108.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 9.dp.toPx()
            val diameter = size.minDimension - strokeWidth
            val topLeft = androidx.compose.ui.geometry.Offset(strokeWidth / 2, strokeWidth / 2)
            val arcSize = androidx.compose.ui.geometry.Size(diameter, diameter)

            // Background Track
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Animated Progress Arc
            if (progress > 0.005f) {
                drawArc(
                    brush = Brush.sweepGradient(gradientColors),
                    startAngle = -90f,
                    sweepAngle = progress * 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "$percentage%",
                fontFamily = PlusJakartaSansFontFamily,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = textColor
            )
            Text(
                text = "WEEK",
                fontFamily = PlusJakartaSansFontFamily,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
private fun BentoMetricTile(
    label: String,
    value: String,
    subtitle: String,
    accentColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = if (isDark) Color(0xFF131B2E) else Color.White,
        border = BorderStroke(1.dp, if (isDark) Color(0xFF24354D) else Color(0xFFE2E8F0)),
        shadowElevation = if (isDark) 0.dp else 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentColor.copy(alpha = if (isDark) 0.2f else 0.12f))
                        .border(1.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(15.dp)
                    )
                }
                Text(
                    text = label.uppercase(),
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.5.sp
                )
            }

            Text(
                text = value,
                fontFamily = PlusJakartaSansFontFamily,
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = accentColor
            )

            Text(
                text = subtitle,
                fontFamily = PlusJakartaSansFontFamily,
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ClassAttendanceLogItem(
    dayIndex: Int,
    classModel: ClassModel,
    isAttended: Boolean,
    isDark: Boolean
) {
    val dayName = getDayShortName(dayIndex)
    val statusColor = if (isAttended) Color(0xFF10B981) else Color(0xFFEF4444)
    val statusText = if (isAttended) "Attended" else "Missed"

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (isDark) Color(0xFF131B2E) else Color.White,
        border = BorderStroke(1.dp, if (isDark) Color(0xFF24354D) else Color(0xFFE2E8F0)),
        shadowElevation = if (isDark) 0.dp else 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Day badge pill
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(statusColor.copy(alpha = if (isDark) 0.18f else 0.1f))
                    .border(1.dp, statusColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = dayName,
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = statusColor
                )
            }

            // Course Details
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = classModel.courseName,
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "${classModel.timeSlot} · Room ${classModel.room}",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Status Badge
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = statusColor.copy(alpha = if (isDark) 0.18f else 0.1f),
                border = BorderStroke(1.dp, statusColor.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (isAttended) Icons.Default.CheckCircle else Icons.Default.Cancel,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = statusText,
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
            }
        }
    }
}

private fun getDayShortName(dayIndex: Int): String {
    return when (dayIndex) {
        0 -> "SAT"
        1 -> "SUN"
        2 -> "MON"
        3 -> "TUE"
        4 -> "WED"
        5 -> "THU"
        6 -> "FRI"
        else -> "DAY"
    }
}

@Composable
private fun ArchiveWeekCard(
    archive: WeeklyAttendanceArchive,
    isDark: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = if (isDark) Color(0xFF131B2E) else Color.White,
        border = BorderStroke(1.dp, if (isDark) Color(0xFF24354D) else Color(0xFFE2E8F0)),
        shadowElevation = if (isDark) 0.dp else 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Date range & Percentage chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = archive.dateRange.ifBlank { archive.weekKey },
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Week ID: ${archive.weekKey}",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (archive.percentage >= 80) {
                        Color(0xFF10B981).copy(alpha = if (isDark) 0.2f else 0.15f)
                    } else if (archive.percentage >= 60) {
                        Color(0xFF3B82F6).copy(alpha = if (isDark) 0.2f else 0.15f)
                    } else {
                        Color(0xFFEF4444).copy(alpha = if (isDark) 0.2f else 0.15f)
                    },
                    border = BorderStroke(
                        1.dp,
                        if (archive.percentage >= 80) Color(0xFF10B981).copy(alpha = 0.3f)
                        else if (archive.percentage >= 60) Color(0xFF3B82F6).copy(alpha = 0.3f)
                        else Color(0xFFEF4444).copy(alpha = 0.3f)
                    )
                ) {
                    Text(
                        text = "${archive.percentage}%",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (archive.percentage >= 80) {
                            if (isDark) Color(0xFF4ADE80) else Color(0xFF10B981)
                        } else if (archive.percentage >= 60) {
                            if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB)
                        } else {
                            if (isDark) Color(0xFFF87171) else Color(0xFFEF4444)
                        },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Progress Indicator
            LinearProgressIndicator(
                progress = { (archive.percentage / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (archive.percentage >= 80) {
                    if (isDark) Color(0xFF4ADE80) else Color(0xFF22C55E)
                } else if (archive.percentage >= 60) {
                    if (isDark) Color(0xFF60A5FA) else Color(0xFF3B82F6)
                } else {
                    if (isDark) Color(0xFFF87171) else Color(0xFFEF4444)
                },
                trackColor = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
            )

            // Bottom details row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${archive.attendedClasses} attended / ${archive.totalClasses} total classes",
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF10B981).copy(alpha = if (isDark) 0.18f else 0.1f),
                    border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Saved",
                            tint = if (isDark) Color(0xFF4ADE80) else Color(0xFF10B981),
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = "Saved",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFF4ADE80) else Color(0xFF10B981)
                        )
                    }
                }
            }
        }
    }
}
