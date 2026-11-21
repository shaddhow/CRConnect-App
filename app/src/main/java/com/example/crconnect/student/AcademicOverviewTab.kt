package com.example.crconnect.student

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.crconnect.ToastHelper
import java.util.Calendar

@Composable
fun AcademicOverviewTab(
    uiState: StudentPortalUiState,
    profile: UserProfileModel,
    classMap: Map<Int, List<ClassModel>>,
    onUpdateCgpa: (String) -> Unit = {},
    onUpdateSection: (String) -> Unit = {},
    onUpdateAdvisorInfo: (String, String) -> Unit = { _, _ -> },
    onUpdateCurrentTerm: (String) -> Unit = {},
    onRefreshHistory: () -> Unit = {}
) {
    val isDark = LocalIsDarkTheme.current
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    // Local dialog visibility states
    var showAddCourseDialog by remember { mutableStateOf(false) }
    var showTermDialog by remember { mutableStateOf(false) }
    var showSectionDialog by remember { mutableStateOf(false) }
    var showCgpaDialog by remember { mutableStateOf(false) }
    var showAdvisorPage by remember { mutableStateOf(false) }
    var showAttendanceSubPage by remember { mutableStateOf(false) }

    // Dialog Input states
    var newCourseCodeInput by remember { mutableStateOf("") }
    var newCourseFacultyInput by remember { mutableStateOf("") }
    var cgpaInput by remember(profile.cgpa) { mutableStateOf(profile.cgpa.ifBlank { "3.30" }) }
    var intakeInput by remember(profile.intake) { mutableStateOf(profile.intake.ifBlank { "55" }) }
    var sectionInput by remember(profile.section) { mutableStateOf(profile.section.ifBlank { "6" }) }
    var advisorNameInput by remember(profile.advisorName) { mutableStateOf(if (profile.advisorName.isNotBlank() && profile.advisorName != "--") profile.advisorName else "Nasrin Akter (NAA)") }
    var advisorEmailInput by remember(profile.advisorEmail) { mutableStateOf(if (profile.advisorEmail.isNotBlank() && profile.advisorEmail != "--") profile.advisorEmail else "nasrinshipa@bubt.edu.bd") }
    var customTermInput by remember(profile.currentTerm) { mutableStateOf(profile.currentTerm.ifBlank { "Fall '26" }) }

    val rawAdvName = profile.advisorName.trim()
    val rawAdvEmail = profile.advisorEmail.trim()
    val hasAdvCustom = rawAdvName.isNotBlank() && rawAdvName != "--"
    val advNameDisplay = if (hasAdvCustom) rawAdvName else "Nasrin Akter (NAA)"
    val advEmailDisplay = if (rawAdvEmail.isNotBlank() && rawAdvEmail != "--") rawAdvEmail else "nasrinshipa@bubt.edu.bd"

    // Manual Courses persistence
    val coursePrefs = remember { context.getSharedPreferences("manual_courses_prefs", Context.MODE_PRIVATE) }
    var manualCoursesList by remember {
        mutableStateOf(coursePrefs.getStringSet("manual_courses_list", emptySet())?.toList() ?: emptyList())
    }

    // Dynamic Attendance Calculation from classMap up to current day & time
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

    val fullProgress = if (evaluatedClasses.isNotEmpty()) dynamicAttendancePct / 100f else 0f
    val fullAnimatedProgress by animateFloatAsState(
        targetValue = fullProgress,
        animationSpec = tween(durationMillis = 600),
        label = "academicOverviewProgress"
    )

    // Combined course list: routine courses + manually added courses
    val enrolledCourseList = remember(classMap, manualCoursesList) {
        val fromRoutine = classMap.values.flatten()
            .groupBy { normalizeCourseCode(it.courseName) }
            .mapNotNull { (code, classes) ->
                if (code.isBlank()) null
                else {
                    val first = classes.first()
                    val teacher = first.courseName.substringAfter("(", "").substringBefore(")", "").trim()
                    CourseDisplayItem(code = code, faculty = teacher, weeklyClasses = classes.size, isManual = false)
                }
            }

        val fromManual = manualCoursesList.map { raw ->
            val code = normalizeCourseCode(raw)
            val teacher = raw.substringAfter("(", "").substringBefore(")", "").trim()
            val displayCode = if (code.isNotBlank()) code else raw
            CourseDisplayItem(code = displayCode, faculty = teacher, weeklyClasses = 0, isManual = true)
        }.filter { manualItem -> fromRoutine.none { it.code.equals(manualItem.code, true) } }

        (fromRoutine + fromManual).sortedBy { it.code }
    }

    // Main Scrollable Screen
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Screen Header
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "Academic Overview",
                fontFamily = PlusJakartaSansFontFamily,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Attendance, Term, Courses & CGPA",
                fontFamily = PlusJakartaSansFontFamily,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 1. Hero Academic Performance / Attendance Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            color = if (isDark) Color(0xFF131D31) else Color(0xFF0F172A),
            border = BorderStroke(1.dp, if (isDark) Color(0x3360A5FA) else Color(0x2238BDF8)),
            shadowElevation = 10.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                if (isDark) Color(0xFF0F172A) else Color(0xFF091E42),
                                if (isDark) Color(0xFF1E293B) else Color(0xFF172554),
                                if (isDark) Color(0xFF0F172A) else Color(0xFF1E3A8A)
                            )
                        )
                    )
                    .padding(20.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Top Row: Tag & Term Pill
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
                                imageVector = Icons.Default.School,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "BUBT ACADEMIC REPORT",
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF38BDF8),
                                letterSpacing = 0.8.sp
                            )
                        }

                        // Clickable Active Term Pill
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
                            modifier = Modifier.clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                showTermDialog = true
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = profile.currentTerm.ifBlank { "Fall '26" },
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Term",
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.size(11.dp)
                                )
                            }
                        }
                    }

                    // Attendance Gauge & Numbers Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Weekly Attendance rate",
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "$dynamicAttendancePct%",
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = when {
                                        dynamicAttendancePct >= 80 -> Color(0xFF34D399)
                                        dynamicAttendancePct >= 60 -> Color(0xFF60A5FA)
                                        else -> Color(0xFFF87171)
                                    }
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = (if (dynamicAttendancePct >= 75) Color(0xFF10B981) else Color(0xFFEF4444)).copy(alpha = 0.25f),
                                    border = BorderStroke(1.dp, (if (dynamicAttendancePct >= 75) Color(0xFF10B981) else Color(0xFFEF4444)).copy(alpha = 0.45f))
                                ) {
                                    Text(
                                        text = if (dynamicAttendancePct >= 75) "Good Standing" else "Needs Attention",
                                        fontFamily = PlusJakartaSansFontFamily,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (dynamicAttendancePct >= 75) Color(0xFF34D399) else Color(0xFFF87171),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "${attendedClasses.size} attended · ${missedClasses.size} missed (${evaluatedClasses.size} evaluated)",
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }

                        // Radial Gauge
                        AttendanceRadialGauge(
                            progress = fullAnimatedProgress,
                            percentage = dynamicAttendancePct,
                            isDark = true,
                            modifier = Modifier.size(92.dp)
                        )
                    }

                    // Bottom Quick Strip
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${enrolledCourseList.size} Courses Enrolled",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                        Text(
                            text = "•",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 11.sp
                        )
                        Text(
                            text = "Sunday Cycle Reset",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                        Text(
                            text = "•",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 11.sp
                        )
                        Text(
                            text = "Scale 4.00",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }

        // 2. Attendance Records & History Action Banner
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .clickable { showAttendanceSubPage = true },
            shape = RoundedCornerShape(20.dp),
            color = if (isDark) Color(0xFF131B2E) else Color.White,
            border = BorderStroke(1.dp, if (isDark) Color(0xFF24354D) else Color(0xFFE2E8F0)),
            shadowElevation = if (isDark) 0.dp else 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF2563EB).copy(alpha = if (isDark) 0.2f else 0.12f))
                        .border(1.dp, Color(0xFF2563EB).copy(alpha = 0.25f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Assessment,
                        contentDescription = "Attendance Records",
                        tint = Color(0xFF2563EB),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Attendance Records & History",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Subject-wise log, calendar & full records",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = if (isDark) Color(0x1A2563EB) else Color(0x102563EB),
                    border = BorderStroke(1.dp, Color(0xFF2563EB).copy(alpha = 0.2f))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = "Open",
                        tint = Color(0xFF2563EB),
                        modifier = Modifier
                            .padding(6.dp)
                            .size(18.dp)
                    )
                }
            }
        }

        // 3. Enrolled Courses Breakdown Section
       var isEnrolledCoursesExpanded by remember { mutableStateOf(true) }
        val arrowRotation by animateFloatAsState(
            targetValue = if (isEnrolledCoursesExpanded) 0f else -90f,
            animationSpec = tween(durationMillis = 200),
            label = "enrolledCoursesArrowRotation"
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        isEnrolledCoursesExpanded = !isEnrolledCoursesExpanded
                    }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    Text(
                        text = "Enrolled Courses (${enrolledCourseList.size})",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Active courses synced & added for this term",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    modifier = Modifier.size(36.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = if (isDark) Color(0xFF132242) else Color(0xFF0066FF).copy(alpha = 0.1f),
                    border = BorderStroke(
                        1.dp,
                        if (isDark) Color(0xFF2563EB).copy(alpha = 0.45f) else Color(0xFF0066FF).copy(alpha = 0.3f)
                    )
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isEnrolledCoursesExpanded) "Collapse courses" else "Expand courses",
                            tint = if (isDark) Color(0xFF60A5FA) else Color(0xFF0066FF),
                            modifier = Modifier
                                .size(20.dp)
                                .graphicsLayer { rotationZ = arrowRotation }
                        )
                    }
                }
            }

            if (isEnrolledCoursesExpanded) {
                if (enrolledCourseList.isNotEmpty()) {
                    val palette = listOf(
                        Color(0xFF2563EB),
                        Color(0xFF7C3AED),
                        Color(0xFF059669),
                        Color(0xFFD97706),
                        Color(0xFFE11D48),
                        Color(0xFF0891B2)
                    )

                    enrolledCourseList.forEachIndexed { index, item ->
                        val color = palette[index % palette.size]

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
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(11.dp))
                                            .background(color.copy(alpha = if (isDark) 0.2f else 0.12f))
                                            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(11.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.School,
                                            contentDescription = null,
                                            tint = color,
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }

                                    Text(
                                        text = item.code,
                                        fontFamily = PlusJakartaSansFontFamily,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                if (item.faculty.isNotBlank()) {
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = color.copy(alpha = if (isDark) 0.18f else 0.1f),
                                        border = BorderStroke(1.dp, color.copy(alpha = 0.28f)),
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = null,
                                                tint = color,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                text = "Faculty ${item.faculty}",
                                                fontFamily = PlusJakartaSansFontFamily,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = color
                                            )
                                        }
                                    }
                                }

                                if (item.isManual) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFF8B5CF6).copy(alpha = if (isDark) 0.2f else 0.12f),
                                            border = BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.3f))
                                        ) {
                                            Text(
                                                text = "Manual",
                                                fontFamily = PlusJakartaSansFontFamily,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF8B5CF6),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                val updated = manualCoursesList.filter {
                                                    !it.startsWith(item.code, ignoreCase = true)
                                                }
                                                manualCoursesList = updated
                                                coursePrefs.edit().putStringSet("manual_courses_list", updated.toSet()).apply()
                                                ToastHelper.show(context, "Removed ${item.code}")
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteOutline,
                                                contentDescription = "Remove",
                                                tint = Color(0xFFEF4444),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                } else {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isDark) Color(0x1AFFFFFF) else Color(0x0A000000)
                                    ) {
                                        Text(
                                            text = "${item.weeklyClasses} cl/wk",
                                            fontFamily = PlusJakartaSansFontFamily,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Surface(
                        modifier = Modifier.clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showAddCourseDialog = true
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isDark) Color(0xFF131C36) else Color(0xFF0066FF).copy(alpha = 0.1f),
                        border = BorderStroke(
                            1.dp,
                            if (isDark) Color(0xFF2563EB).copy(alpha = 0.35f) else Color(0xFF0066FF).copy(alpha = 0.35f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Course",
                                tint = if (isDark) Color(0xFF93C5FD) else Color(0xFF0066FF),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Add Course",
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFF93C5FD) else Color(0xFF0066FF)
                            )
                        }
                    }
                }
            }
        }

        // 4. Academic Profile & Assignment Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = if (isDark) Color(0xFF131B2E) else Color.White,
            border = BorderStroke(1.dp, if (isDark) Color(0xFF24354D) else Color(0xFFE2E8F0)),
            shadowElevation = if (isDark) 0.dp else 4.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Academic Assignment",
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Section Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showSectionDialog = true }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Intake & Section",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Department of CSE, BUBT",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF0066FF).copy(alpha = if (isDark) 0.18f else 0.1f),
                        border = BorderStroke(1.dp, Color(0xFF0066FF).copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "In ${profile.intake.ifBlank { "55" }} · Sec ${profile.section.ifBlank { "6" }}",
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0066FF)
                            )
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = Color(0xFF0066FF),
                                modifier = Modifier.size(11.dp)
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(if (isDark) Color(0x1AFFFFFF) else Color(0x0F000000))
                )

                // Current Term Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showTermDialog = true }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Current Term",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Active semester",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF7C3AED).copy(alpha = if (isDark) 0.18f else 0.1f),
                        border = BorderStroke(1.dp, Color(0xFF7C3AED).copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = profile.currentTerm.ifBlank { "Fall '26" },
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF7C3AED)
                            )
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = Color(0xFF7C3AED),
                                modifier = Modifier.size(11.dp)
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(if (isDark) Color(0x1AFFFFFF) else Color(0x0F000000))
                )

                // CGPA Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showCgpaDialog = true }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Academic Standing",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Cumulative GPA (Scale: 4.00)",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF59E0B).copy(alpha = if (isDark) 0.18f else 0.1f),
                        border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "${profile.cgpa.ifBlank { "3.30" }} CGPA",
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF59E0B)
                            )
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(11.dp)
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(if (isDark) Color(0x1AFFFFFF) else Color(0x0F000000))
                )

                // Advisor Section Header & Card
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF059669).copy(alpha = if (isDark) 0.2f else 0.12f))
                                .border(1.dp, Color(0xFF059669).copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SupervisorAccount,
                                contentDescription = null,
                                tint = Color(0xFF059669),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Academic Advisor",
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Assigned faculty mentor",
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF059669).copy(alpha = if (isDark) 0.18f else 0.1f),
                        border = BorderStroke(1.dp, Color(0xFF059669).copy(alpha = 0.3f)),
                        modifier = Modifier.clickable { showAdvisorPage = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "● Active",
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF059669)
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                contentDescription = null,
                                tint = Color(0xFF059669),
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = if (isDark) Color(0xFF1E283A) else Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, if (isDark) Color(0x33FFFFFF) else Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = advNameDisplay,
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF0066FF).copy(alpha = if (isDark) 0.2f else 0.1f),
                                border = BorderStroke(1.dp, Color(0xFF0066FF).copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = "CSE Dept",
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0066FF),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = "Assistant Professor & Academic Advisor",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text = "Department of CSE, BUBT",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = advEmailDisplay,
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0066FF),
                            modifier = Modifier.clickable {
                                val intent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("mailto:$advEmailDisplay")
                                }
                                context.startActivity(Intent.createChooser(intent, "Email Academic Advisor"))
                            }
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                                            data = Uri.parse("mailto:$advEmailDisplay")
                                        }
                                        context.startActivity(Intent.createChooser(intent, "Email Advisor"))
                                    },
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF059669).copy(alpha = if (isDark) 0.2f else 0.1f),
                                border = BorderStroke(1.dp, Color(0xFF059669).copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Email,
                                        contentDescription = null,
                                        tint = Color(0xFF059669),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Email Advisor",
                                        fontFamily = PlusJakartaSansFontFamily,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF059669)
                                    )
                                }
                            }

                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { showAdvisorPage = true },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isDark) Color(0xFF28364D) else Color(0xFFE2E8F0),
                                border = BorderStroke(1.dp, if (isDark) Color(0x33FFFFFF) else Color(0xFFCBD5E1))
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Full Profile",
                                        fontFamily = PlusJakartaSansFontFamily,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom clearance for floating bottom bar
        Spacer(modifier = Modifier.height(100.dp))
    }

    // Modal Dialogs
    if (showAddCourseDialog) {
        ModernGlassModal(
            onDismissRequest = { showAddCourseDialog = false },
            icon = Icons.Default.School,
            iconTint = Color(0xFF0066FF),
            iconBg = if (isDark) Color(0x260066FF) else Color(0x180066FF),
            title = "Add Enrolled Course",
            subtitle = "Manually add an active course to your overview",
            isDark = isDark,
            confirmText = "Add Course",
            confirmGradient = listOf(Color(0xFF0066FF), Color(0xFF0052CC)),
            onConfirm = {
                val cleanCode = newCourseCodeInput.trim()
                if (cleanCode.isNotBlank()) {
                    val cleanFaculty = newCourseFacultyInput.trim()
                    val fullEntry = if (cleanFaculty.isNotBlank()) "$cleanCode ($cleanFaculty)" else cleanCode
                    val updated = (manualCoursesList + fullEntry).distinct()
                    manualCoursesList = updated
                    coursePrefs.edit().putStringSet("manual_courses_list", updated.toSet()).apply()
                    newCourseCodeInput = ""
                    newCourseFacultyInput = ""
                    showAddCourseDialog = false
                    ToastHelper.show(context, "Course added successfully")
                } else {
                    ToastHelper.show(context, "Please enter a course code")
                }
            }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = newCourseCodeInput,
                    onValueChange = { newCourseCodeInput = it },
                    singleLine = true,
                    placeholder = { Text("e.g. CSE 311, CSE 400", fontFamily = PlusJakartaSansFontFamily, fontSize = 14.sp) },
                    label = { Text("Course Code / Name", fontFamily = PlusJakartaSansFontFamily, fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = null,
                            tint = Color(0xFF0066FF),
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = if (isDark) Color(0xFF1E283A) else Color(0xFFF8FAFC),
                        unfocusedContainerColor = if (isDark) Color(0xFF1E283A) else Color(0xFFF8FAFC),
                        focusedBorderColor = Color(0xFF0066FF),
                        unfocusedBorderColor = if (isDark) Color(0x33FFFFFF) else Color(0xFFE2E8F0)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = newCourseFacultyInput,
                    onValueChange = { newCourseFacultyInput = it },
                    singleLine = true,
                    placeholder = { Text("e.g. NAA, MJ, AAG (Optional)", fontFamily = PlusJakartaSansFontFamily, fontSize = 14.sp) },
                    label = { Text("Faculty Initial (Optional)", fontFamily = PlusJakartaSansFontFamily, fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color(0xFF0066FF),
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = if (isDark) Color(0xFF1E283A) else Color(0xFFF8FAFC),
                        unfocusedContainerColor = if (isDark) Color(0xFF1E283A) else Color(0xFFF8FAFC),
                        focusedBorderColor = Color(0xFF0066FF),
                        unfocusedBorderColor = if (isDark) Color(0x33FFFFFF) else Color(0xFFE2E8F0)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Quick course preset suggestions
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "POPULAR CSE COURSES",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0066FF),
                        letterSpacing = 0.5.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("CSE 311", "CSE 312", "CSE 323", "CSE 400").forEach { preset ->
                            val isSelected = newCourseCodeInput.trim() == preset
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) Color(0xFF0066FF) else (if (isDark) Color(0xFF1E283A) else Color(0xFFF1F5F9)),
                                border = BorderStroke(1.dp, if (isSelected) Color(0xFF0066FF) else (if (isDark) Color(0x33FFFFFF) else Color(0xFFE2E8F0))),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        newCourseCodeInput = preset
                                    }
                            ) {
                                Box(modifier = Modifier.padding(vertical = 7.dp), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = preset,
                                        fontFamily = PlusJakartaSansFontFamily,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCgpaDialog) {
        ModernGlassModal(
            onDismissRequest = { showCgpaDialog = false },
            icon = Icons.Default.WorkspacePremium,
            iconTint = Color(0xFFF59E0B),
            iconBg = if (isDark) Color(0x26F59E0B) else Color(0x18F59E0B),
            title = "Update CGPA",
            subtitle = "Enter your current cumulative GPA",
            isDark = isDark,
            confirmText = "Save CGPA",
            confirmGradient = listOf(Color(0xFFF59E0B), Color(0xFFD97706)),
            onConfirm = {
                if (cgpaInput.isNotBlank()) {
                    onUpdateCgpa(cgpaInput.trim())
                    showCgpaDialog = false
                }
            }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = cgpaInput,
                    onValueChange = { cgpaInput = it },
                    singleLine = true,
                    placeholder = { Text("e.g. 3.50", fontFamily = PlusJakartaSansFontFamily, fontSize = 14.sp) },
                    trailingIcon = { Text("/ 4.00", fontFamily = PlusJakartaSansFontFamily, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = if (isDark) Color(0xFF1E283A) else Color(0xFFF8FAFC),
                        unfocusedContainerColor = if (isDark) Color(0xFF1E283A) else Color(0xFFF8FAFC),
                        focusedBorderColor = Color(0xFFF59E0B),
                        unfocusedBorderColor = if (isDark) Color(0x33FFFFFF) else Color(0xFFE2E8F0)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Quick presets
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "QUICK SELECT PRESET",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF59E0B),
                        letterSpacing = 0.5.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("3.00", "3.25", "3.50", "3.75", "4.00").forEach { preset ->
                            val isSelected = cgpaInput.trim() == preset
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) Color(0xFFF59E0B) else (if (isDark) Color(0xFF1E283A) else Color(0xFFF1F5F9)),
                                border = BorderStroke(1.dp, if (isSelected) Color(0xFFF59E0B) else (if (isDark) Color(0x33FFFFFF) else Color(0xFFE2E8F0))),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        cgpaInput = preset
                                    }
                            ) {
                                Box(modifier = Modifier.padding(vertical = 7.dp), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = preset,
                                        fontFamily = PlusJakartaSansFontFamily,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSectionDialog) {
        ModernGlassModal(
            onDismissRequest = { showSectionDialog = false },
            icon = Icons.Default.Badge,
            iconTint = Color(0xFF0066FF),
            iconBg = if (isDark) Color(0x260066FF) else Color(0x180066FF),
            title = "Update Section & Intake",
            subtitle = "Keep your class routine synchronized",
            isDark = isDark,
            confirmText = "Save Details",
            confirmGradient = listOf(Color(0xFF0066FF), Color(0xFF0052CC)),
            onConfirm = {
                if (intakeInput.isNotBlank() && sectionInput.isNotBlank()) {
                    onUpdateSection(sectionInput.trim())
                    showSectionDialog = false
                }
            }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = intakeInput,
                    onValueChange = { intakeInput = it },
                    label = { Text("Intake Number", fontFamily = PlusJakartaSansFontFamily, fontSize = 12.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = if (isDark) Color(0xFF1E283A) else Color(0xFFF8FAFC),
                        unfocusedContainerColor = if (isDark) Color(0xFF1E283A) else Color(0xFFF8FAFC),
                        focusedBorderColor = Color(0xFF0066FF),
                        unfocusedBorderColor = if (isDark) Color(0x33FFFFFF) else Color(0xFFE2E8F0)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = sectionInput,
                    onValueChange = { sectionInput = it },
                    label = { Text("Section", fontFamily = PlusJakartaSansFontFamily, fontSize = 12.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = if (isDark) Color(0xFF1E283A) else Color(0xFFF8FAFC),
                        unfocusedContainerColor = if (isDark) Color(0xFF1E283A) else Color(0xFFF8FAFC),
                        focusedBorderColor = Color(0xFF0066FF),
                        unfocusedBorderColor = if (isDark) Color(0x33FFFFFF) else Color(0xFFE2E8F0)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    if (showTermDialog) {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val shortYearCurrent = (currentYear % 100).toString()
        val shortYearNext = ((currentYear + 1) % 100).toString()

        val activeTermOptions = listOf(
            "Spring '$shortYearCurrent",
            "Summer '$shortYearCurrent",
            "Fall '$shortYearCurrent",
            "Spring '$shortYearNext"
        )

        ModernGlassModal(
            onDismissRequest = { showTermDialog = false },
            icon = Icons.Default.School,
            iconTint = Color(0xFF7C3AED),
            iconBg = if (isDark) Color(0x267C3AED) else Color(0x187C3AED),
            title = "Update Current Term",
            subtitle = "Select or enter your active semester",
            isDark = isDark,
            confirmText = "Save Term",
            confirmGradient = listOf(Color(0xFF7C3AED), Color(0xFF6D28D9)),
            onConfirm = {
                if (customTermInput.isNotBlank()) {
                    onUpdateCurrentTerm(customTermInput.trim())
                    showTermDialog = false
                }
            }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = customTermInput,
                    onValueChange = { customTermInput = it },
                    singleLine = true,
                    placeholder = { Text("e.g. Spring '$shortYearCurrent, Fall '$shortYearCurrent", fontFamily = PlusJakartaSansFontFamily, fontSize = 14.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = null,
                            tint = Color(0xFF7C3AED),
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = if (isDark) Color(0xFF1E283A) else Color(0xFFF8FAFC),
                        unfocusedContainerColor = if (isDark) Color(0xFF1E283A) else Color(0xFFF8FAFC),
                        focusedBorderColor = Color(0xFF7C3AED),
                        unfocusedBorderColor = if (isDark) Color(0x33FFFFFF) else Color(0xFFE2E8F0)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "ACTIVE SEMESTERS",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF7C3AED),
                        letterSpacing = 0.5.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        activeTermOptions.forEach { opt ->
                            val isSelected = customTermInput.trim() == opt
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) Color(0xFF7C3AED) else (if (isDark) Color(0xFF1E283A) else Color(0xFFF1F5F9)),
                                border = BorderStroke(1.dp, if (isSelected) Color(0xFF7C3AED) else (if (isDark) Color(0x33FFFFFF) else Color(0xFFE2E8F0))),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        customTermInput = opt
                                    }
                            ) {
                                Box(modifier = Modifier.padding(vertical = 7.dp), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = opt,
                                        fontFamily = PlusJakartaSansFontFamily,
                                        fontSize = 10.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdvisorPage) {
        FullScreenSubPageDialog(
            onDismissRequest = { showAdvisorPage = false },
            isDark = isDark
        ) {
            BackHandler { showAdvisorPage = false }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        onClick = { showAdvisorPage = false }
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
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Academic Advisor Profile",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Advisor Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    color = if (isDark) Color(0xFF131B2E) else Color.White,
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF24354D) else Color(0xFFE2E8F0)),
                    shadowElevation = if (isDark) 0.dp else 4.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF059669).copy(alpha = 0.15f))
                                    .border(1.5.dp, Color(0xFF059669).copy(alpha = 0.4f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color(0xFF059669),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = advNameDisplay,
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Assistant Professor & Academic Advisor",
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Department of CSE, BUBT",
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(if (isDark) Color(0x1AFFFFFF) else Color(0x0F000000))
                        )

                        // Contact Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                                        data = Uri.parse("mailto:$advEmailDisplay")
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Email Academic Advisor"))
                                },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF0066FF).copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = null,
                                    tint = Color(0xFF0066FF),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Official Email",
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = advEmailDisplay,
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0066FF)
                                )
                            }
                        }

                        // Send Email Button
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("mailto:$advEmailDisplay")
                                }
                                context.startActivity(Intent.createChooser(intent, "Email Academic Advisor"))
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                        ) {
                            Icon(imageVector = Icons.Default.Email, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Send Email to Advisor",
                                fontFamily = PlusJakartaSansFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAttendanceSubPage) {
        FullScreenSubPageDialog(
            onDismissRequest = { showAttendanceSubPage = false },
            isDark = isDark
        ) {
            BackHandler { showAttendanceSubPage = false }
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        onClick = { showAttendanceSubPage = false }
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
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Attendance Records",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Weekly status & detailed breakdown",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                AttendanceTab(
                    uiState = uiState,
                    onRefreshHistory = onRefreshHistory
                )
            }
        }
    }
}

