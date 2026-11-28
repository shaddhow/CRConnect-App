package com.example.crconnect.student

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.view.WindowManager
import android.widget.ImageView
import androidx.compose.foundation.Image
import androidx.compose.runtime.key
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.signature.ObjectKey
import com.example.crconnect.ProfileImageCacheHelper
import java.io.File
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.shadow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import java.util.Calendar
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Info
import androidx.compose.ui.text.style.TextAlign
import com.example.crconnect.core.utils.HapticUtils
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.crconnect.R
import com.example.crconnect.SessionManager
import com.example.crconnect.ToastHelper

@Composable
fun ProfileTab(
    profile: UserProfileModel,
    uiState: StudentPortalUiState = StudentPortalUiState(),
    classMap: Map<Int, List<ClassModel>> = emptyMap(),
    themeMode: String = SessionManager.THEME_SYSTEM,
    onThemeModeSelected: (String) -> Unit = {},
    onEditProfilePicture: () -> Unit = {},
    onUpdateCgpa: (String) -> Unit = {},
    onUpdateStudentId: (String) -> Unit = {},
    onUpdateSection: (String) -> Unit = {},
    onUpdateAdvisorInfo: (String, String) -> Unit = { _, _ -> },
    onUpdateCurrentTerm: (String) -> Unit = {},
    onSecuritySettings: () -> Unit = {},
    onCheckUpdate: () -> Unit = {},
    onShowUpdateDialog: () -> Unit = {},
    onNavigateToAttendance: () -> Unit = {},
    onNavigateToAcademics: () -> Unit = {},
    onSignOut: () -> Unit = {},
    onLogout: () -> Unit = onSignOut
) {
    val scrollState = rememberScrollState()
    var showAcademicOverviewPage by remember { mutableStateOf(false) }
    var showAdvisorPage by remember { mutableStateOf(false) }
    var showSettingsPage by remember { mutableStateOf(false) }
    var showNotificationSettingsDialog by remember { mutableStateOf(false) }
    var notificationsEnabled by remember { mutableStateOf(true) }

    var showSectionDialog by remember { mutableStateOf(false) }
    var sectionInput by remember(profile.section) { mutableStateOf(profile.section) }

    var showCgpaDialog by remember { mutableStateOf(false) }
    var cgpaInput by remember(profile.cgpa) { mutableStateOf(profile.cgpa) }

    var showAdvisorDialog by remember { mutableStateOf(false) }
    var advisorNameInput by remember(profile.advisorName) { mutableStateOf(profile.advisorName) }
    var advisorEmailInput by remember(profile.advisorEmail) { mutableStateOf(profile.advisorEmail) }

    var showTermDialog by remember { mutableStateOf(false) }
    var termInput by remember(profile.currentTerm) { mutableStateOf(profile.currentTerm) }

    val currentYear = remember { Calendar.getInstance().get(Calendar.YEAR) }
    val currShortYear = currentYear % 100
    val nextShortYear = (currentYear + 1) % 100
    val dynamicSemesters = remember(currShortYear, nextShortYear) {
        listOf(
            "Spring '$currShortYear",
            "Summer '$currShortYear",
            "Fall '$currShortYear",
            "Spring '$nextShortYear"
        )
    }

    var showAddCourseDialog by remember { mutableStateOf(false) }
    var newCourseCodeInput by remember { mutableStateOf("") }
    var newCourseFacultyInput by remember { mutableStateOf("") }

    val context = LocalContext.current
    val coursePrefs = remember(context) { context.getSharedPreferences("academic_courses_prefs", android.content.Context.MODE_PRIVATE) }
    var manualCoursesList by remember {
        mutableStateOf(coursePrefs.getStringSet("manual_courses_list", emptySet())?.toList() ?: emptyList())
    }
    val clipboardManager = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current
    val versionName = remember(context) {
        try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "1.0"
        } catch (e: Exception) {
            "1.0"
        }
    }

    val systemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        SessionManager.THEME_LIGHT -> false
        SessionManager.THEME_DARK -> true
        else -> systemDark
    }

    // Dynamic Attendance Calculation from classMap up to current day & time
    val allClasses = classMap.values.flatten()
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

    var showLogoutConfirmDialog by remember { mutableStateOf(false) }

    if (showLogoutConfirmDialog) {
        ModernGlassModal(
            onDismissRequest = { showLogoutConfirmDialog = false },
            icon = Icons.AutoMirrored.Filled.Logout,
            iconTint = Color(0xFFDC2626),
            iconBg = if (isDark) Color(0x26DC2626) else Color(0x18DC2626),
            title = "Log Out Account",
            subtitle = "You will need to sign in again to access the portal",
            isDark = isDark,
            confirmText = "Log Out",
            confirmGradient = listOf(Color(0xFFDC2626), Color(0xFFB91C1C)),
            onConfirm = {
                showLogoutConfirmDialog = false
                onLogout()
            },
            cancelText = "Stay Signed In"
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (isDark) Color(0x14DC2626) else Color(0x0CDC2626),
                border = BorderStroke(1.dp, Color(0xFFDC2626).copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFDC2626).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = profile.name.ifBlank { "Student Account" },
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "ID: ${profile.studentId.ifBlank { "--" }} · Intake ${profile.intake.ifBlank { "--" }}",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.WorkspacePremium,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        Text(
                            text = "/ 4.00",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.padding(end = 12.dp)
                        )
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = if (isDark) Color(0xFF1E283A) else Color(0xFFF8FAFC),
                        unfocusedContainerColor = if (isDark) Color(0xFF1E283A) else Color(0xFFF8FAFC),
                        focusedBorderColor = Color(0xFFF59E0B),
                        unfocusedBorderColor = if (isDark) Color(0x33FFFFFF) else Color(0xFFE2E8F0),
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Quick Preset Chips
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
                                        fontSize = 12.sp,
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

    if (showAdvisorDialog) {
        ModernGlassModal(
            onDismissRequest = { showAdvisorDialog = false },
            icon = Icons.Default.SupervisorAccount,
            iconTint = Color(0xFF059669),
            iconBg = if (isDark) Color(0x26059669) else Color(0x18059669),
            title = "Update Academic Advisor",
            subtitle = "Faculty advisor details & official email",
            isDark = isDark,
            confirmText = "Save Advisor",
            confirmGradient = listOf(Color(0xFF059669), Color(0xFF047857)),
            onConfirm = {
                if (advisorNameInput.isNotBlank()) {
                    onUpdateAdvisorInfo(advisorNameInput.trim(), advisorEmailInput.trim())
                    showAdvisorDialog = false
                }
            }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = advisorNameInput,
                    onValueChange = { advisorNameInput = it },
                    label = { Text("Advisor Full Name", fontFamily = PlusJakartaSansFontFamily, fontSize = 13.sp) },
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color(0xFF059669),
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = if (isDark) Color(0xFF1E283A) else Color(0xFFF8FAFC),
                        unfocusedContainerColor = if (isDark) Color(0xFF1E283A) else Color(0xFFF8FAFC),
                        focusedBorderColor = Color(0xFF059669),
                        unfocusedBorderColor = if (isDark) Color(0x33FFFFFF) else Color(0xFFE2E8F0)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = advisorEmailInput,
                    onValueChange = { advisorEmailInput = it },
                    label = { Text("Advisor Email Address", fontFamily = PlusJakartaSansFontFamily, fontSize = 13.sp) },
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null,
                            tint = Color(0xFF059669),
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = if (isDark) Color(0xFF1E283A) else Color(0xFFF8FAFC),
                        unfocusedContainerColor = if (isDark) Color(0xFF1E283A) else Color(0xFFF8FAFC),
                        focusedBorderColor = Color(0xFF059669),
                        unfocusedBorderColor = if (isDark) Color(0x33FFFFFF) else Color(0xFFE2E8F0)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Quick Fill Pill for BUBT CSE Advisor
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isDark) Color(0x1A059669) else Color(0x12059669),
                    border = BorderStroke(1.dp, Color(0xFF059669).copy(alpha = 0.25f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            advisorNameInput = "Nasrin Akter"
                            advisorEmailInput = "nasrinakter@bubt.edu.bd"
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SupervisorAccount,
                            contentDescription = null,
                            tint = Color(0xFF059669),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Quick Fill: Nasrin Akter (NAA)",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF059669)
                        )
                    }
                }
            }
        }
    }

    if (showTermDialog) {
        ModernGlassModal(
            onDismissRequest = { showTermDialog = false },
            icon = Icons.Default.School,
            iconTint = Color(0xFF8B5CF6),
            iconBg = if (isDark) Color(0x268B5CF6) else Color(0x188B5CF6),
            title = "Update Current Term",
            subtitle = "Select or enter your active semester",
            isDark = isDark,
            confirmText = "Save Term",
            confirmGradient = listOf(Color(0xFF8B5CF6), Color(0xFF6D28D9)),
            onConfirm = {
                if (termInput.isNotBlank()) {
                    onUpdateCurrentTerm(termInput.trim())
                    showTermDialog = false
                }
            }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = termInput,
                    onValueChange = { termInput = it },
                    singleLine = true,
                    placeholder = { Text("e.g. Spring '$currShortYear, Summer '$currShortYear", fontFamily = PlusJakartaSansFontFamily, fontSize = 14.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = null,
                            tint = Color(0xFF8B5CF6),
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = if (isDark) Color(0xFF1E283A) else Color(0xFFF8FAFC),
                        unfocusedContainerColor = if (isDark) Color(0xFF1E283A) else Color(0xFFF8FAFC),
                        focusedBorderColor = Color(0xFF8B5CF6),
                        unfocusedBorderColor = if (isDark) Color(0x33FFFFFF) else Color(0xFFE2E8F0),
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Quick Semester Chips
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "ACTIVE SEMESTERS ($currentYear)",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF8B5CF6),
                        letterSpacing = 0.5.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        dynamicSemesters.forEach { termChoice ->
                            val isSelected = termInput.trim().equals(termChoice, ignoreCase = true)
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) Color(0xFF8B5CF6) else (if (isDark) Color(0xFF1E283A) else Color(0xFFF1F5F9)),
                                border = BorderStroke(1.dp, if (isSelected) Color(0xFF8B5CF6) else (if (isDark) Color(0x33FFFFFF) else Color(0xFFE2E8F0))),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        termInput = termChoice
                                    }
                            ) {
                                Box(modifier = Modifier.padding(vertical = 7.dp), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = termChoice,
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

    if (showNotificationSettingsDialog) {
        com.example.crconnect.student.notifications.NotificationSettingsDialog(
            onDismiss = { showNotificationSettingsDialog = false }
        )
    }

    if (showSectionDialog) {
        ModernGlassModal(
            onDismissRequest = { showSectionDialog = false },
            icon = Icons.Default.Edit,
            iconTint = Color(0xFF0066FF),
            iconBg = if (isDark) Color(0x260066FF) else Color(0x180066FF),
            title = "Edit Class Section",
            subtitle = "Intake ${profile.intake.ifBlank { "--" }} academic assignment",
            isDark = isDark,
            confirmText = "Save Section",
            confirmGradient = listOf(Color(0xFF0066FF), Color(0xFF0052CC)),
            onConfirm = {
                val cleanSec = sectionInput.trim()
                if (cleanSec.isNotBlank()) {
                    onUpdateSection(cleanSec)
                    showSectionDialog = false
                } else {
                    ToastHelper.show(context, "Please enter a valid section")
                }
            },
            onCancel = {
                sectionInput = profile.section
                showSectionDialog = false
            }
        ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Full section & academic info card
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isDark) Color(0xFF161F30) else Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, if (isDark) Color(0x33FFFFFF) else Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "CURRENT ACADEMIC ASSIGNMENT",
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0066FF),
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = if (profile.department.contains("CSE", true)) "Department of CSE, BUBT" else profile.department,
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isDark) Color(0xFF1E293B) else Color.White,
                                    border = BorderStroke(1.dp, if (isDark) Color(0x33FFFFFF) else Color(0xFFE2E8F0)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(modifier = Modifier.padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "Intake: ${profile.intake.ifBlank { "--" }}",
                                            fontFamily = PlusJakartaSansFontFamily,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isDark) Color(0xFF1E293B) else Color.White,
                                    border = BorderStroke(1.dp, if (isDark) Color(0x33FFFFFF) else Color(0xFFE2E8F0)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(modifier = Modifier.padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "Section: ${profile.section.ifBlank { "--" }}",
                                            fontFamily = PlusJakartaSansFontFamily,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0066FF)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Section input field
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "New Section Number:",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = sectionInput,
                            onValueChange = { sectionInput = it.trim() },
                            singleLine = true,
                            placeholder = { Text("e.g. 6, 5, 5A, 1", fontFamily = PlusJakartaSansFontFamily, fontSize = 14.sp) },
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC),
                                unfocusedContainerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC),
                                focusedBorderColor = Color(0xFF0066FF),
                                unfocusedBorderColor = if (isDark) Color(0x33FFFFFF) else Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Quick Section Selector chips (1, 2, 3, 4, 5, 6, 7, 8)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Quick Select:",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("1", "2", "3", "4", "5", "6", "7", "8").forEach { secChoice ->
                                val isSelected = sectionInput.trim() == secChoice
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) Color(0xFF0066FF) else (if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC)),
                                    border = BorderStroke(1.dp, if (isSelected) Color(0xFF0066FF) else (if (isDark) Color(0x33FFFFFF) else Color(0xFFE2E8F0))),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { sectionInput = secChoice }
                                ) {
                                    Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                        Text(
                                            text = secChoice,
                                            fontFamily = PlusJakartaSansFontFamily,
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
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

    if (showAcademicOverviewPage) {
        FullScreenSubPageDialog(
            onDismissRequest = { showAcademicOverviewPage = false },
            isDark = isDark
        ) {
            BackHandler {
                showAcademicOverviewPage = false
            }

            val fullProgress = if (evaluatedClasses.isNotEmpty()) dynamicAttendancePct / 100f else 0f
            val fullAnimatedProgress by animateFloatAsState(
                targetValue = fullProgress,
                animationSpec = tween(durationMillis = 600),
                label = "academicOverviewProgress"
            )
            val dynamicCourseCount = computeUniqueCoursesCount(classMap)

            // Top App Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    onClick = { showAcademicOverviewPage = false }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Academic Overview",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Attendance, Term, Courses & CGPA",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

                    // Scrollable Content
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
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

                        // 1. Hero Academic Performance Card
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
                                // Background ambient glow
                                Box(
                                    modifier = Modifier
                                        .size(160.dp)
                                        .offset(x = 180.dp, y = (-40).dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.radialGradient(
                                                listOf(Color(0xFF38BDF8).copy(alpha = 0.16f), Color.Transparent)
                                            )
                                        )
                                )

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
                                            modifier = Modifier.clickable { showTermDialog = true }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = profile.currentTerm.ifBlank { "Spring '$currShortYear" },
                                                    fontFamily = PlusJakartaSansFontFamily,
                                                    fontSize = 11.sp,
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

                                    // Metric Split: Attendance (Left) & CGPA (Right)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        // Left: Attendance
                                        Surface(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { onNavigateToAcademics() },
                                            shape = RoundedCornerShape(16.dp),
                                            color = Color.White.copy(alpha = 0.06f),
                                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(14.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = "ATTENDANCE",
                                                    fontFamily = PlusJakartaSansFontFamily,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White.copy(alpha = 0.7f),
                                                    letterSpacing = 0.5.sp
                                                )

                                                Text(
                                                    text = if (evaluatedClasses.isNotEmpty()) "$dynamicAttendancePct%" else "New Wk",
                                                    fontFamily = PlusJakartaSansFontFamily,
                                                    fontSize = 28.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color.White
                                                )

                                                Surface(
                                                    shape = CircleShape,
                                                    color = if (evaluatedClasses.isEmpty()) {
                                                        Color.White.copy(alpha = 0.15f)
                                                    } else if (dynamicAttendancePct >= 80) {
                                                        Color(0xFF10B981).copy(alpha = 0.25f)
                                                    } else if (dynamicAttendancePct >= 60) {
                                                        Color(0xFF3B82F6).copy(alpha = 0.25f)
                                                    } else {
                                                        Color(0xFFEF4444).copy(alpha = 0.25f)
                                                    }
                                                ) {
                                                    Text(
                                                        text = if (evaluatedClasses.isEmpty()) {
                                                            "● New Week"
                                                        } else if (dynamicAttendancePct >= 80) {
                                                            "● Excellent"
                                                        } else if (dynamicAttendancePct >= 60) {
                                                            "● Good"
                                                        } else {
                                                            "● Needs Attn"
                                                        },
                                                        fontFamily = PlusJakartaSansFontFamily,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (evaluatedClasses.isEmpty()) {
                                                            Color.White.copy(alpha = 0.8f)
                                                        } else if (dynamicAttendancePct >= 80) {
                                                            Color(0xFF4ADE80)
                                                        } else if (dynamicAttendancePct >= 60) {
                                                            Color(0xFF60A5FA)
                                                        } else {
                                                            Color(0xFFF87171)
                                                        },
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                    )
                                                }

                                                Text(
                                                    text = if (evaluatedClasses.isNotEmpty()) "${attendedClasses.size} attended · ${missedClasses.size} missed" else "0 classes evaluated",
                                                    fontFamily = PlusJakartaSansFontFamily,
                                                    fontSize = 10.5.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = Color.White.copy(alpha = 0.65f)
                                                )
                                            }
                                        }

                                        // Right: CGPA
                                        Surface(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { showCgpaDialog = true },
                                            shape = RoundedCornerShape(16.dp),
                                            color = Color.White.copy(alpha = 0.06f),
                                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(14.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "ACADEMIC CGPA",
                                                        fontFamily = PlusJakartaSansFontFamily,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White.copy(alpha = 0.7f),
                                                        letterSpacing = 0.5.sp
                                                    )
                                                    Icon(
                                                        imageVector = Icons.Default.WorkspacePremium,
                                                        contentDescription = null,
                                                        tint = Color(0xFFF59E0B),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }

                                                Row(verticalAlignment = Alignment.Bottom) {
                                                    Text(
                                                        text = if (profile.cgpa.isNotBlank() && profile.cgpa != "--") profile.cgpa else "--",
                                                        fontFamily = PlusJakartaSansFontFamily,
                                                        fontSize = 28.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = Color(0xFFF59E0B)
                                                    )
                                                    Text(
                                                        text = " / 4.00",
                                                        fontFamily = PlusJakartaSansFontFamily,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White.copy(alpha = 0.55f),
                                                        modifier = Modifier.padding(bottom = 3.dp, start = 2.dp)
                                                    )
                                                }

                                                Surface(
                                                    shape = CircleShape,
                                                    color = Color(0xFFF59E0B).copy(alpha = 0.22f)
                                                ) {
                                                    Text(
                                                        text = if (profile.cgpa.isNotBlank() && profile.cgpa != "--") "Target 3.50" else "Tap to set",
                                                        fontFamily = PlusJakartaSansFontFamily,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFFFBBF24),
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                    )
                                                }

                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                                ) {
                                                    Text(
                                                        text = "Tap to edit CGPA",
                                                        fontFamily = PlusJakartaSansFontFamily,
                                                        fontSize = 10.5.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = Color(0xFF38BDF8)
                                                    )
                                                    Icon(
                                                        imageVector = Icons.Default.Edit,
                                                        contentDescription = null,
                                                        tint = Color(0xFF38BDF8),
                                                        modifier = Modifier.size(10.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Progress Bar
                                    LinearProgressIndicator(
                                        progress = { fullAnimatedProgress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(5.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = if (dynamicAttendancePct >= 80) Color(0xFF4ADE80) else if (dynamicAttendancePct >= 60) Color(0xFF60A5FA) else Color(0xFFF87171),
                                        trackColor = Color.White.copy(alpha = 0.15f)
                                    )

                                    // Bottom 3 Quick Metrics
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${enrolledCourseList.size.coerceAtLeast(dynamicCourseCount)} Courses Enrolled",
                                            fontFamily = PlusJakartaSansFontFamily,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White.copy(alpha = 0.8f)
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
                                            color = Color.White.copy(alpha = 0.6f)
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
                                .clickable { onNavigateToAcademics() },
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
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
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
                                    modifier = Modifier.clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        showAddCourseDialog = true
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF0066FF).copy(alpha = if (isDark) 0.18f else 0.1f),
                                    border = BorderStroke(1.dp, Color(0xFF0066FF).copy(alpha = 0.35f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Add Course",
                                            tint = Color(0xFF0066FF),
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Text(
                                            text = "Add Course",
                                            fontFamily = PlusJakartaSansFontFamily,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0066FF)
                                        )
                                    }
                                }
                            }

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
                                            // Left: Course Icon + Course Code
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

                                            // Center/Right: Faculty Pill (fills horizontal center cleanly)
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

                                            // Right: Load / Manual Badge
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
                                                            imageVector = Icons.Default.Delete,
                                                            contentDescription = "Delete",
                                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            } else {
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC),
                                                    border = BorderStroke(1.dp, if (isDark) Color(0x33FFFFFF) else Color(0xFFE2E8F0))
                                                ) {
                                                    Text(
                                                        text = "${item.weeklyClasses} cl/wk",
                                                        fontFamily = PlusJakartaSansFontFamily,
                                                        fontSize = 11.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isDark) Color(0xFF131B2E) else Color.White,
                                    border = BorderStroke(1.dp, if (isDark) Color(0xFF24354D) else Color(0xFFE2E8F0))
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(20.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.School,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Text(
                                            text = "No Courses Added Yet",
                                            fontFamily = PlusJakartaSansFontFamily,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Add your enrolled courses manually or wait for routine sync",
                                            fontFamily = PlusJakartaSansFontFamily,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFF0066FF),
                                            modifier = Modifier.clickable { showAddCourseDialog = true }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Add,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Text(
                                                    text = "Add Course Manually",
                                                    fontFamily = PlusJakartaSansFontFamily,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 4. Academic Profile & Assignment Card
                        val rawAdvName = profile.advisorName.trim()
                        val rawAdvEmail = profile.advisorEmail.trim()
                        val hasAdvCustom = rawAdvName.isNotBlank() && rawAdvName != "--"
                        val advNameDisplay = if (hasAdvCustom) rawAdvName else "Nasrin Akter (NAA)"
                        val advEmailDisplay = if (rawAdvEmail.isNotBlank() && rawAdvEmail != "--") rawAdvEmail else "nasrinshipa@bubt.edu.bd"

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
                                            text = if (profile.department.contains("CSE", true)) "Department of CSE, BUBT" else profile.department,
                                            fontFamily = PlusJakartaSansFontFamily,
                                            fontSize = 11.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF0066FF).copy(alpha = if (isDark) 0.18f else 0.1f),
                                        border = BorderStroke(1.dp, Color(0xFF0066FF).copy(alpha = 0.25f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = "In ${profile.intake.ifBlank { "--" }} · Sec ${profile.section.ifBlank { "--" }}",
                                                fontFamily = PlusJakartaSansFontFamily,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF0066FF)
                                            )
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Edit",
                                                tint = Color(0xFF0066FF),
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 5. Dedicated Full Academic Advisor Card
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
                                                .border(1.dp, Color(0xFF059669).copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
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
                                            text = "Department of Computer Science & Engineering, BUBT",
                                            fontFamily = PlusJakartaSansFontFamily,
                                            fontSize = 11.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                                        )

                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 3.dp)
                                                .height(1.dp)
                                                .background(if (isDark) Color(0x1AFFFFFF) else Color(0x0F000000))
                                        )

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Email,
                                                contentDescription = null,
                                                tint = Color(0xFF0066FF),
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = advEmailDisplay,
                                                fontFamily = PlusJakartaSansFontFamily,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF0066FF)
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                try {
                                                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                                                        data = Uri.parse("mailto:$advEmailDisplay")
                                                        putExtra(Intent.EXTRA_SUBJECT, "Query from Student: ${profile.name} (${profile.studentId})")
                                                    }
                                                    context.startActivity(intent)
                                                } catch (e: Exception) {
                                                    ToastHelper.show(context, "No email app found")
                                                }
                                            },
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFF059669).copy(alpha = if (isDark) 0.18f else 0.1f),
                                        border = BorderStroke(1.dp, Color(0xFF059669).copy(alpha = 0.3f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(vertical = 11.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Email,
                                                contentDescription = "Email",
                                                tint = Color(0xFF059669),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Email Advisor",
                                                fontFamily = PlusJakartaSansFontFamily,
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF059669)
                                            )
                                        }
                                    }

                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { showAdvisorPage = true },
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isDark) Color(0xFF1E283A) else Color(0xFFF1F5F9),
                                        border = BorderStroke(1.dp, if (isDark) Color(0x33FFFFFF) else Color(0xFFE2E8F0))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(vertical = 11.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = "Profile",
                                                tint = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Full Profile",
                                                fontFamily = PlusJakartaSansFontFamily,
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(96.dp))
                    }
        }
    }

    if (showAdvisorPage) {
        FullScreenSubPageDialog(
            onDismissRequest = { showAdvisorPage = false },
            isDark = isDark
        ) {
            BackHandler {
                showAdvisorPage = false
            }

            val clipboardManager = LocalClipboardManager.current
            val rawAdvEmail = profile.advisorEmail.trim()
            val rawAdvName = profile.advisorName.trim()
            val hasCustomAdvEmail = rawAdvEmail.isNotBlank() && rawAdvEmail != "--"
            val hasCustomAdvName = rawAdvName.isNotBlank() && rawAdvName != "--"
            val advisorEmail = if (hasCustomAdvEmail) rawAdvEmail else "nasrinshipa@bubt.edu.bd"
            val advisorDisplayName = if (hasCustomAdvName) rawAdvName else "Nasrin Akter (NAA)"

            // Top App Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(44.dp),
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
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Academic Advisor",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Advisor details & contact info",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

                    // Content
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Card 1: Advisor Main Details Card
                        DesignCard(padding = 20.dp) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Assigned Faculty Advisor",
                                        fontFamily = PlusJakartaSansFontFamily,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (profile.isCr) {
                                        Surface(
                                            modifier = Modifier.clickable { showAdvisorDialog = true },
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "Edit Advisor",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Text(
                                                    text = "Edit (CR)",
                                                    fontFamily = PlusJakartaSansFontFamily,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    } else {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                        ) {
                                            Text(
                                                text = "Advisor (CR Editable)",
                                                fontFamily = PlusJakartaSansFontFamily,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }

                                // Advisor Name Row
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .then(
                                            if (profile.advisorName.contains("Nasrin Akter", ignoreCase = true) || profile.advisorName.isBlank() || profile.advisorName == "--") {
                                                Modifier.clickable {
                                                    try {
                                                        val intent = Intent(
                                                            Intent.ACTION_VIEW,
                                                            Uri.parse("https://bubt.edu.bd/department/department-of-computer-science-engineering/faculty/profile/NAA")
                                                        )
                                                        context.startActivity(intent)
                                                    } catch (e: Exception) {
                                                        ToastHelper.show(context, "Could not open faculty profile")
                                                    }
                                                }
                                            } else Modifier
                                        )
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = "Advisor",
                                            fontFamily = PlusJakartaSansFontFamily,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "Department of CSE, BUBT",
                                            fontFamily = PlusJakartaSansFontFamily,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Normal,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = advisorDisplayName,
                                            fontFamily = PlusJakartaSansFontFamily,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        if (profile.advisorName.contains("Nasrin Akter", ignoreCase = true) || profile.advisorName.isBlank() || profile.advisorName == "--") {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                                contentDescription = "View Profile",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }

                                // Advisor Email Row
                                KeyValueRow(
                                    key = "Advisor Email",
                                    value = advisorEmail,
                                    valueColor = MaterialTheme.colorScheme.onSurface
                                )

                                // Action Buttons Row: Email & Copy
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            try {
                                                val intent = Intent(Intent.ACTION_SENDTO).apply {
                                                    data = Uri.parse("mailto:$advisorEmail")
                                                    putExtra(Intent.EXTRA_SUBJECT, "Query regarding academic advising")
                                                }
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                ToastHelper.show(context, "No email app found")
                                            }
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = MaterialTheme.colorScheme.primary
                                        )
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Email,
                                                contentDescription = "Email",
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = "Email Advisor",
                                                fontFamily = PlusJakartaSansFontFamily,
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(advisorEmail))
                                            ToastHelper.show(context, "Advisor email copied to clipboard")
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = MaterialTheme.colorScheme.onSurface
                                        )
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = "Copy",
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = "Copy Email",
                                                fontFamily = PlusJakartaSansFontFamily,
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Card 2: Guidance Notice
                        DesignCard(padding = 18.dp) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Academic Advising Support",
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Your assigned faculty advisor assists with semester registration approvals, graduation credit verification, and academic guidance. To update advisor records, consult your Class Representative (CR).",
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 17.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                    }
        }
    }

    if (showSettingsPage) {
        FullScreenSubPageDialog(
            onDismissRequest = { showSettingsPage = false },
            isDark = isDark
        ) {
            BackHandler {
                showSettingsPage = false
            }

            // Top App Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    onClick = { showSettingsPage = false }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Settings",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Theme, profile picture, notifications & support",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

                    // Content
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 1. Student Identity Header Card
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(22.dp),
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
                                // Avatar with camera edit badge
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clickable { onEditProfilePicture() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(CircleShape)
                                            .background(if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9))
                                            .border(1.5.dp, Color(0xFF0066FF).copy(alpha = 0.5f), CircleShape)
                                    ) {
                                        val imgVersion = ProfileImageCacheHelper.cacheVersion
                                        key(profile.profileImageUrl, imgVersion) {
                                            val bitmap = remember(profile.profileImageUrl, imgVersion) {
                                                ProfileImageCacheHelper.loadBitmapFromPathOrBase64(profile.profileImageUrl)
                                            }

                                            if (bitmap != null) {
                                                Image(
                                                    bitmap = bitmap.asImageBitmap(),
                                                    contentDescription = "Profile Picture",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else if (profile.profileImageUrl.isNotBlank()) {
                                                AndroidView(
                                                    factory = { ctx ->
                                                        ImageView(ctx).apply {
                                                            layoutParams = android.view.ViewGroup.LayoutParams(
                                                                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                                                android.view.ViewGroup.LayoutParams.MATCH_PARENT
                                                            )
                                                            scaleType = ImageView.ScaleType.CENTER_CROP
                                                            Glide.with(this)
                                                                .load(profile.profileImageUrl)
                                                                .circleCrop()
                                                                .signature(ObjectKey("${profile.profileImageUrl}_$imgVersion"))
                                                                .into(this)
                                                        }
                                                    },
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                Box(
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = profile.name.take(1).uppercase(),
                                                        fontFamily = PlusJakartaSansFontFamily,
                                                        fontSize = 20.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF0066FF)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Camera Edit Badge Pill
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .align(Alignment.BottomEnd)
                                            .clip(CircleShape)
                                            .background(Color(0xFF0066FF))
                                            .border(1.dp, Color.White, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CameraAlt,
                                            contentDescription = "Edit photo",
                                            tint = Color.White,
                                            modifier = Modifier.size(10.dp)
                                        )
                                    }
                                }

                                // Details
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = profile.name.ifBlank { "Student Account" },
                                        fontFamily = PlusJakartaSansFontFamily,
                                        fontSize = 15.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "ID: ${profile.studentId.ifBlank { "--" }} · In ${profile.intake.ifBlank { "--" }} Sec ${profile.section.ifBlank { "--" }}",
                                        fontFamily = PlusJakartaSansFontFamily,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF0066FF)
                                    )
                                    Text(
                                        text = if (profile.department.contains("CSE", true)) "Department of CSE, BUBT" else profile.department,
                                        fontFamily = PlusJakartaSansFontFamily,
                                        fontSize = 11.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // 2. Visual Theme & Display Card
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
                                    .padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFF8B5CF6).copy(alpha = if (isDark) 0.2f else 0.12f))
                                            .border(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Palette,
                                            contentDescription = null,
                                            tint = Color(0xFF8B5CF6),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "Appearance & Theme",
                                            fontFamily = PlusJakartaSansFontFamily,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Select your preferred visual interface mode",
                                            fontFamily = PlusJakartaSansFontFamily,
                                            fontSize = 11.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                GlassySegmentedThemeSwitcher(
                                    selectedTheme = themeMode,
                                    onThemeSelected = { mode ->
                                        onThemeModeSelected(mode)
                                    },
                                    isDark = isDark
                                )
                            }
                        }

                        // 3. Account & Security Card with Glassy Capsules
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
                                    .padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFF0066FF).copy(alpha = if (isDark) 0.2f else 0.12f))
                                            .border(1.dp, Color(0xFF0066FF).copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Security,
                                            contentDescription = null,
                                            tint = Color(0xFF0066FF),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "Account & Security",
                                            fontFamily = PlusJakartaSansFontFamily,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Biometrics, password reset & profile photo",
                                            fontFamily = PlusJakartaSansFontFamily,
                                            fontSize = 11.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                GlassyActionRow(
                                    icon = Icons.Default.Lock,
                                    iconTint = Color(0xFF10B981),
                                    title = "Security Settings & Biometrics",
                                    subtitle = "Biometric lock, password reset & student vault",
                                    onClick = { onSecuritySettings() },
                                    isDark = isDark
                                )

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(if (isDark) Color(0x1AFFFFFF) else Color(0x0F000000))
                                )

                                GlassyActionRow(
                                    icon = Icons.Default.CameraAlt,
                                    iconTint = Color(0xFF0066FF),
                                    title = "Change Profile Picture",
                                    subtitle = "Upload custom photo from camera or gallery",
                                    onClick = { onEditProfilePicture() },
                                    isDark = isDark
                                )
                            }
                        }

                        // 4. Alerts & Tactile Preferences
                        var hapticsEnabled by remember { mutableStateOf(true) }

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
                                    .padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFFF59E0B).copy(alpha = if (isDark) 0.2f else 0.12f))
                                            .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Notifications,
                                            contentDescription = null,
                                            tint = Color(0xFFF59E0B),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "Alerts & Preferences",
                                            fontFamily = PlusJakartaSansFontFamily,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Push alerts and physical tactile feedback",
                                            fontFamily = PlusJakartaSansFontFamily,
                                            fontSize = 11.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(Color(0xFFF59E0B).copy(alpha = if (isDark) 0.2f else 0.1f))
                                                .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Notifications,
                                                contentDescription = null,
                                                tint = Color(0xFFF59E0B),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Column(
                                            modifier = Modifier.clickable { showNotificationSettingsDialog = true },
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Text(
                                                text = "Class & Routine Alerts",
                                                fontFamily = PlusJakartaSansFontFamily,
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Tap to customize 15m alerts, morning briefing & safety",
                                                fontFamily = PlusJakartaSansFontFamily,
                                                fontSize = 11.5.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    GlassySmoothSwitch(
                                        checked = notificationsEnabled,
                                        onCheckedChange = {
                                            notificationsEnabled = it
                                            com.example.crconnect.student.notifications.NotificationRepository.setClassReminderEnabled(context, it)
                                        }
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(if (isDark) Color(0x1AFFFFFF) else Color(0x0F000000))
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(Color(0xFF8B5CF6).copy(alpha = if (isDark) 0.2f else 0.1f))
                                                .border(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Vibration,
                                                contentDescription = null,
                                                tint = Color(0xFF8B5CF6),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text(
                                                text = "Tactile Haptics",
                                                fontFamily = PlusJakartaSansFontFamily,
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Vibration feedback on tab switches & taps",
                                                fontFamily = PlusJakartaSansFontFamily,
                                                fontSize = 11.5.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    GlassySmoothSwitch(
                                        checked = hapticsEnabled,
                                        onCheckedChange = { hapticsEnabled = it }
                                    )
                                }
                            }
                        }

                        // 5. Routine Data & Cache Management
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
                                    .padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(Color(0xFF06B6D4).copy(alpha = if (isDark) 0.2f else 0.12f))
                                                .border(1.dp, Color(0xFF06B6D4).copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CloudSync,
                                                contentDescription = null,
                                                tint = Color(0xFF06B6D4),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text(
                                                text = "Sync Routine Schedule",
                                                fontFamily = PlusJakartaSansFontFamily,
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Clear offline cache and refresh routine",
                                                fontFamily = PlusJakartaSansFontFamily,
                                                fontSize = 11.5.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Surface(
                                        modifier = Modifier.clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            try {
                                                RoutineRepository(context).clearEntries()
                                            } catch (e: Exception) {}
                                            ToastHelper.show(context, "Routine schedule refreshed from server")
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFF06B6D4).copy(alpha = if (isDark) 0.18f else 0.1f),
                                        border = BorderStroke(1.dp, Color(0xFF06B6D4).copy(alpha = 0.3f))
                                    ) {
                                        Text(
                                            text = "Re-Sync",
                                            fontFamily = PlusJakartaSansFontFamily,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF06B6D4),
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // 6. University Portal & Support Card
                        val isUpdateAvailable = uiState.isUpdateAvailable
                        val updateUrl = uiState.updateUrl
                        val latestTag = uiState.latestVersionName.ifBlank { "v1.2.0" }

                        val infiniteTransition = rememberInfiniteTransition(label = "updatePulse")
                        val pulseAlpha by infiniteTransition.animateFloat(
                            initialValue = 0.35f,
                            targetValue = 1.0f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(800, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "pulseAlpha"
                        )

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
                                    .padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFF2563EB).copy(alpha = if (isDark) 0.2f else 0.12f))
                                            .border(1.dp, Color(0xFF2563EB).copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.School,
                                            contentDescription = null,
                                            tint = Color(0xFF2563EB),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "Portal & Support",
                                            fontFamily = PlusJakartaSansFontFamily,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "BUBT university links & app info",
                                            fontFamily = PlusJakartaSansFontFamily,
                                            fontSize = 11.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                GlassyActionRow(
                                    icon = Icons.Default.School,
                                    iconTint = Color(0xFF2563EB),
                                    title = "BUBT Notice Board Portal",
                                    subtitle = "Official university circulars & notice archive",
                                    onClick = {
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.bubt.edu.bd/home/all_notices"))
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            ToastHelper.show(context, "Could not open browser")
                                        }
                                    },
                                    isDark = isDark,
                                    trailingIcon = Icons.AutoMirrored.Filled.OpenInNew
                                )

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(if (isDark) Color(0x1AFFFFFF) else Color(0x0F000000))
                                )

                                // App Version Row
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            if (isUpdateAvailable) {
                                                onShowUpdateDialog()
                                            } else {
                                                onCheckUpdate()
                                            }
                                        }
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(Color(0xFF7C3AED).copy(alpha = if (isDark) 0.2f else 0.1f))
                                                .border(1.dp, Color(0xFF7C3AED).copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Info,
                                                contentDescription = null,
                                                tint = Color(0xFF7C3AED),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text(
                                                text = "CRConnect v$versionName",
                                                fontFamily = PlusJakartaSansFontFamily,
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "BUBT CSE Edition · Stable Build",
                                                fontFamily = PlusJakartaSansFontFamily,
                                                fontSize = 11.5.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    if (isUpdateAvailable) {
                                        Surface(
                                            shape = RoundedCornerShape(20.dp),
                                            color = if (isDark) Color(0xFF1E1B4B) else Color(0xFFEEF2FF),
                                            border = BorderStroke(
                                                1.dp,
                                                if (isDark) Color(0xFF6366F1).copy(alpha = 0.6f) else Color(0xFFC7D2FE)
                                            )
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
                                                        .background(Color(0xFF6366F1))
                                                )
                                                Text(
                                                    text = "Update",
                                                    fontFamily = PlusJakartaSansFontFamily,
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isDark) Color(0xFFA5B4FC) else Color(0xFF4338CA)
                                                )
                                            }
                                        }
                                    } else {
                                        Surface(
                                            shape = RoundedCornerShape(20.dp),
                                            color = if (isDark) Color(0xFF064E3B).copy(alpha = 0.3f) else Color(0xFFECFDF5),
                                            border = BorderStroke(
                                                1.dp,
                                                if (isDark) Color(0xFF059669).copy(alpha = 0.5f) else Color(0xFFA7F3D0)
                                            )
                                        ) {
                                            Text(
                                                text = "Latest",
                                                fontFamily = PlusJakartaSansFontFamily,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDark) Color(0xFF34D399) else Color(0xFF059669),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(96.dp))
                    }
        }
    }

    val screenBg = Color.Transparent

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(screenBg)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Card 1: Apple Wallet Style Glassmorphic Digital Pass
        val studentIdText = if (profile.studentId.isBlank() || profile.studentId == "--" || profile.studentId.contains("@") || profile.studentId.equals(profile.email, ignoreCase = true)) {
            "--"
        } else {
            profile.studentId
        }

        val cardGradients = if (isDark) {
            listOf(Color(0xFF0F172A), Color(0xFF1E3A8A), Color(0xFF1E293B))
        } else {
            listOf(Color(0xFF0040A8), Color(0xFF0052CC), Color(0xFF1D4ED8))
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = if (isDark) 0.dp else 12.dp,
                    shape = RoundedCornerShape(26.dp),
                    ambientColor = Color(0x400052CC),
                    spotColor = Color(0x400052CC)
                )
                .clip(RoundedCornerShape(26.dp))
                .background(Brush.linearGradient(cardGradients))
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = if (isDark) 0.2f else 0.35f),
                    shape = RoundedCornerShape(26.dp)
                )
        ) {
            // Ambient Background Circles
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(26.dp))
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 40.dp, y = (-30).dp)
                        .size(190.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 50.dp, y = 40.dp)
                        .size(160.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.05f))
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Top Ribbon: University Identity + Live Active Pill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.18f))
                                .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        Text(
                            text = "BUBT · STUDENT PASS",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White.copy(alpha = 0.9f),
                            letterSpacing = 1.2.sp
                        )
                    }

                    // Glass Active Pill
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xFF10B981).copy(alpha = 0.25f))
                            .border(1.dp, Color(0xFF34D399).copy(alpha = 0.5f), CircleShape)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF34D399))
                            )
                            Text(
                                text = "Active",
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Student Card Body: Avatar on Left + Details on Right
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Circular Avatar with Dual Glow Ring + Camera Badge
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clickable { onEditProfilePicture() },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.15f))
                                .border(2.5.dp, Color.White.copy(alpha = 0.8f), CircleShape)
                                .padding(2.5.dp)
                                .clip(CircleShape)
                        ) {
                            val imgVersion = ProfileImageCacheHelper.cacheVersion
                            key(profile.profileImageUrl, imgVersion) {
                                val bitmap = remember(profile.profileImageUrl, imgVersion) {
                                    ProfileImageCacheHelper.loadBitmapFromPathOrBase64(profile.profileImageUrl)
                                }

                                if (bitmap != null) {
                                    Image(
                                        bitmap = bitmap.asImageBitmap(),
                                        contentDescription = "Profile Picture",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                    )
                                } else if (profile.profileImageUrl.isNotBlank()) {
                                    AndroidView(
                                        factory = { ctx ->
                                            ImageView(ctx).apply {
                                                layoutParams = android.view.ViewGroup.LayoutParams(
                                                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                                                )
                                                scaleType = ImageView.ScaleType.CENTER_CROP
                                                setOnClickListener { onEditProfilePicture() }
                                                Glide.with(this)
                                                    .load(profile.profileImageUrl)
                                                    .circleCrop()
                                                    .signature(ObjectKey("${profile.profileImageUrl}_$imgVersion"))
                                                    .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                                                    .into(this)
                                            }
                                        },
                                        update = { view ->
                                            view.setOnClickListener { onEditProfilePicture() }
                                            Glide.with(view)
                                                .load(profile.profileImageUrl)
                                                .circleCrop()
                                                .signature(ObjectKey("${profile.profileImageUrl}_$imgVersion"))
                                                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                                                .into(view)
                                        },
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                    )
                                } else {
                                    AnimatedAvatarPlaceholder(size = 76.dp, iconSize = 36.dp)
                                }
                            }
                        }

                        // Camera Badge Overlay
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .offset(x = 2.dp, y = 2.dp)
                                .size(24.dp)
                                .shadow(3.dp, CircleShape)
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(1.dp, Color(0xFF0052CC), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Edit photo",
                                tint = Color(0xFF0052CC),
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }

                    // Student Information
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val formattedName = remember(profile.name) {
                            profile.name.replace(Regex("\\s*-\\s*"), "-").trim().ifBlank { "Student" }
                        }
                        Text(
                            text = formattedName,
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            maxLines = 2,
                            lineHeight = 22.sp,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = profile.email.ifBlank { "student@bubt.edu.bd" },
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        // Pills row: ID pill + Intake/Sec pill + CR
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // ID Pill with copy
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = 0.18f))
                                    .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                                    .clickable {
                                        if (studentIdText != "--") {
                                            clipboardManager.setText(AnnotatedString(studentIdText))
                                            ToastHelper.show(context, "ID copied: $studentIdText")
                                        }
                                    }
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "ID: $studentIdText",
                                        fontFamily = PlusJakartaSansFontFamily,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy ID",
                                        tint = Color.White.copy(alpha = 0.8f),
                                        modifier = Modifier.size(11.dp)
                                    )
                                }
                            }

                            // Intake & Section Pill (clickable to edit section)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = 0.18f))
                                    .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                                    .clickable {
                                        sectionInput = profile.section
                                        showSectionDialog = true
                                    }
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Text(
                                        text = "In ${profile.intake.ifBlank { "--" }} · Sec ${profile.section.ifBlank { "--" }}",
                                        fontFamily = PlusJakartaSansFontFamily,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Section",
                                        tint = Color.White.copy(alpha = 0.8f),
                                        modifier = Modifier.size(10.dp)
                                    )
                                }
                            }

                            if (profile.isCr) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFF59E0B).copy(alpha = 0.3f))
                                        .border(1.dp, Color(0xFFFBBF24).copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 6.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "⚡ CR",
                                        fontFamily = PlusJakartaSansFontFamily,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Card Bottom Strip: Department Identity
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.12f))
                        .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Badge,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (profile.department.contains("CSE", true)) "DEPARTMENT OF CSE · BUBT" else profile.department.uppercase(),
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.9f),
                            letterSpacing = 0.8.sp
                        )
                    }

                    Text(
                        text = "OFFICIAL DIGITAL PASS",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White.copy(alpha = 0.7f),
                        letterSpacing = 0.8.sp
                    )
                }
            }
        }

        // 2x2 Bento Quick Stats Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Bento Tile 1: CGPA
            BentoTileItem(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.WorkspacePremium,
                iconTint = Color(0xFFF59E0B),
                iconBg = if (isDark) Color(0x26F59E0B) else Color(0x18F59E0B),
                title = "ACADEMIC CGPA",
                value = if (profile.cgpa.isNotBlank() && profile.cgpa != "--") profile.cgpa else "--",
                badge = if (profile.cgpa.isNotBlank() && profile.cgpa != "--") "Target 3.50" else "Tap to set",
                isDark = isDark,
                onClick = { showCgpaDialog = true }
            )

            // Bento Tile 2: Attendance
            val attPctText = if (evaluatedClasses.isNotEmpty()) "$dynamicAttendancePct%" else "New Week"
            val attBadge = if (evaluatedClasses.isEmpty()) "0 Classes" else if (dynamicAttendancePct >= 80) "Excellent" else if (dynamicAttendancePct >= 60) "Good" else "Needs Attn"
            BentoTileItem(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.CalendarToday,
                iconTint = Color(0xFF10B981),
                iconBg = if (isDark) Color(0x2610B981) else Color(0x1810B981),
                title = "ATTENDANCE",
                value = attPctText,
                badge = attBadge,
                isDark = isDark,
                onClick = { showAcademicOverviewPage = true }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Bento Tile 3: Current Term
            BentoTileItem(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.School,
                iconTint = Color(0xFF8B5CF6),
                iconBg = if (isDark) Color(0x268B5CF6) else Color(0x188B5CF6),
                title = "CURRENT TERM",
                value = profile.currentTerm.ifBlank { "Spring '$currShortYear" },
                badge = "${computeUniqueCoursesCount(classMap)} Courses",
                isDark = isDark,
                onClick = { showTermDialog = true }
            )

            // Bento Tile 4: Academic Advisor
            val hasAdvisor = profile.advisorName.isNotBlank() && profile.advisorName.trim() != "--"
            val advisorBentoDisplay = if (hasAdvisor) profile.advisorName.trim() else "Nasrin Akter"
            BentoTileItem(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.SupervisorAccount,
                iconTint = Color(0xFF0066FF),
                iconBg = if (isDark) Color(0x260066FF) else Color(0x180066FF),
                title = "ADVISOR",
                value = advisorBentoDisplay,
                badge = if (hasAdvisor) "Assigned" else "Faculty",
                isDark = isDark,
                onClick = { showAdvisorPage = true }
            )
        }

        // Grouped Settings & Action Tile Container
        val hasAdvisor = profile.advisorName.isNotBlank() && profile.advisorName.trim() != "--"
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = if (isDark) Color(0xFF131B2E) else Color.White,
            border = BorderStroke(1.dp, if (isDark) Color(0xFF24354D) else Color(0xFFE2E8F0)),
            shadowElevation = if (isDark) 0.dp else 4.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Row 1: Academic Overview
                ProfileGroupRow(
                    icon = Icons.Default.School,
                    iconTint = Color(0xFF2563EB),
                    iconBg = if (isDark) Color(0x262563EB) else Color(0x142563EB),
                    title = "Academic Overview",
                    subtitle = "Attendance, term, courses & stats",
                    onClick = { onNavigateToAcademics() }
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(if (isDark) Color(0x1AFFFFFF) else Color(0x0F000000))
                )

                // Row 2: Academic Advisor Details
                ProfileGroupRow(
                    icon = Icons.Default.SupervisorAccount,
                    iconTint = Color(0xFF059669),
                    iconBg = if (isDark) Color(0x26059669) else Color(0x14059669),
                    title = "Academic Advisor",
                    subtitle = if (hasAdvisor) profile.advisorName.trim() else "Nasrin Akter (NAA) · Dept. Advisor",
                    onClick = { showAdvisorPage = true }
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(if (isDark) Color(0x1AFFFFFF) else Color(0x0F000000))
                )

                // Row 3: Settings & Security
                ProfileGroupRow(
                    icon = Icons.Default.Settings,
                    iconTint = Color(0xFF7C3AED),
                    iconBg = if (isDark) Color(0x267C3AED) else Color(0x147C3AED),
                    title = "Settings & Security",
                    subtitle = "Theme, profile picture & preferences",
                    onClick = { showSettingsPage = true }
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(if (isDark) Color(0x1AFFFFFF) else Color(0x0F000000))
                )

                // Row 4: Logout
                ProfileGroupRow(
                    icon = Icons.AutoMirrored.Filled.Logout,
                    iconTint = Color(0xFFDC2626),
                    iconBg = if (isDark) Color(0x26DC2626) else Color(0x14DC2626),
                    title = "Log Out",
                    subtitle = "Sign out of your student account",
                    isDanger = true,
                    onClick = { showLogoutConfirmDialog = true }
                )
            }
        }

        Spacer(modifier = Modifier.height(84.dp))
    }
}

@Composable
private fun BentoTileItem(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    title: String,
    value: String,
    badge: String,
    isDark: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = if (isDark) Color(0xFF131B2E) else Color.White,
        border = BorderStroke(1.dp, if (isDark) Color(0xFF24354D) else Color(0xFFE2E8F0)),
        shadowElevation = if (isDark) 0.dp else 4.dp
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
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(iconBg)
                        .border(1.dp, iconTint.copy(alpha = 0.25f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = iconTint,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = iconTint.copy(alpha = if (isDark) 0.18f else 0.1f),
                    border = BorderStroke(1.dp, iconTint.copy(alpha = 0.25f))
                ) {
                    Text(
                        text = badge,
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = iconTint,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = value,
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

data class CourseDisplayItem(
    val code: String,
    val faculty: String,
    val weeklyClasses: Int,
    val isManual: Boolean
)

private fun getBUBTCourseTitle(code: String): String? {
    val clean = code.uppercase().replace(" ", "").trim()
    return when {
        clean.contains("CSE207") -> "Data Structures"
        clean.contains("CSE208") -> "Data Structures Lab"
        clean.contains("CSE209") -> "Digital Logic Design"
        clean.contains("CSE210") -> "Digital Logic Design Lab"
        clean.contains("CSE341") -> "Software Engineering"
        clean.contains("CSE342") -> "Software Engineering Lab"
        clean.contains("CSE101") -> "Computer Concepts"
        clean.contains("CSE111") -> "Structured Programming"
        clean.contains("CSE112") -> "Programming Lab"
        clean.contains("CSE121") -> "Discrete Mathematics"
        clean.contains("CSE123") -> "Data Structures"
        clean.contains("CSE124") -> "Data Structures Lab"
        clean.contains("CSE211") -> "Object Oriented Programming"
        clean.contains("CSE212") -> "OOP Lab"
        clean.contains("CSE221") -> "Algorithms"
        clean.contains("CSE222") -> "Algorithms Lab"
        clean.contains("CSE231") -> "Database Systems"
        clean.contains("CSE232") -> "Database Lab"
        clean.contains("CSE311") -> "Design Patterns"
        clean.contains("CSE313") -> "Operating Systems"
        clean.contains("CSE314") -> "OS Lab"
        clean.contains("CSE323") -> "Computer Networks"
        clean.contains("CSE324") -> "Networking Lab"
        clean.contains("CSE400") -> "Capstone Project / Thesis"
        clean.contains("CSE401") -> "Artificial Intelligence"
        clean.contains("CSE402") -> "AI Lab"
        clean.contains("MAT") -> "Mathematics"
        clean.contains("PHY") -> "Physics"
        clean.contains("ENG") -> "English"
        else -> null
    }
}

@Composable
private fun GlassySegmentedThemeSwitcher(
    selectedTheme: String,
    onThemeSelected: (String) -> Unit,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val options = listOf(
        Triple(SessionManager.THEME_SYSTEM, "System", Icons.Default.PhoneAndroid),
        Triple(SessionManager.THEME_LIGHT, "Light", Icons.Default.LightMode),
        Triple(SessionManager.THEME_DARK, "Dark", Icons.Default.DarkMode)
    )

    val selectedIndex = when (selectedTheme) {
        SessionManager.THEME_LIGHT -> 1
        SessionManager.THEME_DARK -> 2
        else -> 0
    }

    val containerBg = if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9)
    val containerBorder = if (isDark) Color(0xFF24334B) else Color(0xFFE2E8F0)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(42.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(containerBg)
            .border(BorderStroke(1.dp, containerBorder), RoundedCornerShape(12.dp))
            .padding(3.dp)
    ) {
        val totalWidth = maxWidth
        val tabWidth = totalWidth / 3

        val indicatorOffset by animateDpAsState(
            targetValue = tabWidth * selectedIndex,
            animationSpec = spring(
                dampingRatio = 0.78f,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "themePillOffset"
        )

        // Sliding Active Indicator Pill with Glow Shadow & Gradient
        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .width(tabWidth)
                .fillMaxHeight()
                .shadow(elevation = 3.dp, shape = RoundedCornerShape(9.dp))
                .clip(RoundedCornerShape(9.dp))
                .background(
                    androidx.compose.ui.graphics.Brush.horizontalGradient(
                        listOf(Color(0xFF0066FF), Color(0xFF2563EB))
                    )
                )
        )

        // Tab Items with animated colors
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            options.forEachIndexed { index, (mode, label, icon) ->
                val isSelected = selectedIndex == index
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = tween(durationMillis = 200),
                    label = "tabTextColor"
                )
                val iconTint by animateColorAsState(
                    targetValue = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    animationSpec = tween(durationMillis = 200),
                    label = "tabIconTint"
                )

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(9.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (!isSelected) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onThemeSelected(mode)
                            }
                        },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = iconTint,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = label,
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 12.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = textColor
                    )
                }
            }
        }
    }
}

@Composable
private fun GlassyActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    isDark: Boolean,
    trailingIcon: androidx.compose.ui.graphics.vector.ImageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconTint.copy(alpha = if (isDark) 0.2f else 0.1f))
                    .border(1.dp, iconTint.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(19.dp)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Icon(
            imageVector = trailingIcon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun ProfileGroupRow(
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    title: String,
    subtitle: String,
    isDanger: Boolean = false,
    onClick: () -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    val dangerColor = if (isDark) Color(0xFFFCA5A5) else Color(0xFFDC2626)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBg)
                    .border(1.dp, iconTint.copy(alpha = 0.25f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDanger) dangerColor else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun ModernGlassModal(
    onDismissRequest: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    iconBg: Color,
    title: String,
    subtitle: String,
    isDark: Boolean,
    confirmText: String = "Save Changes",
    confirmGradient: List<Color> = listOf(iconTint, iconTint.copy(alpha = 0.85f)),
    onConfirm: () -> Unit,
    cancelText: String = "Cancel",
    onCancel: () -> Unit = onDismissRequest,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val view = LocalView.current

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismissRequest() },
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { /* Catch clicks inside modal */ },
                shape = RoundedCornerShape(26.dp),
                color = if (isDark) Color(0xFF131A2A) else Color(0xFFFFFFFF),
                border = BorderStroke(
                    1.dp,
                    if (isDark) Color(0x33FFFFFF) else Color(0xFFE2E8F0)
                ),
                shadowElevation = if (isDark) 0.dp else 16.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(13.dp))
                                .background(iconBg)
                                .border(1.dp, iconTint.copy(alpha = 0.25f), RoundedCornerShape(13.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = iconTint,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title,
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = subtitle,
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Surface(
                            modifier = Modifier.size(32.dp),
                            shape = CircleShape,
                            color = if (isDark) Color(0x1AFFFFFF) else Color(0x0A000000),
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onDismissRequest()
                            }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // Divider
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(if (isDark) Color(0x1AFFFFFF) else Color(0x0D000000))
                    )

                    // Body
                    content()

                    // Action Buttons Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onCancel()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, if (isDark) Color(0x33FFFFFF) else Color(0xFFE2E8F0)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isDark) Color(0x0FFFFFFF) else Color(0xFFF8FAFC),
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Text(
                                text = cancelText,
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                HapticUtils.lightClick(view)
                                onConfirm()
                            },
                            modifier = Modifier
                                .weight(1.3f)
                                .height(46.dp)
                                .shadow(8.dp, RoundedCornerShape(14.dp), ambientColor = iconTint.copy(alpha = 0.35f), spotColor = iconTint.copy(alpha = 0.45f)),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.horizontalGradient(confirmGradient),
                                        RoundedCornerShape(14.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = confirmText,
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    showEditIcon: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 12.dp, horizontal = 6.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = label.uppercase(),
                fontFamily = PlusJakartaSansFontFamily,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                letterSpacing = 0.5.sp
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = value.ifBlank { "--" },
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (value == "Tap to set" || value == "Not Set") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (showEditIcon) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun KeyValueRow(
    key: String,
    value: String,
    valueColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = key,
            fontFamily = PlusJakartaSansFontFamily,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontFamily = PlusJakartaSansFontFamily,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}

@Composable
private fun ThemeOptionButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontFamily = PlusJakartaSansFontFamily,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

@Composable
fun FullScreenSubPageDialog(
    onDismissRequest: () -> Unit,
    isDark: Boolean,
    content: @Composable () -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val view = LocalView.current
        val dialogWindow = remember(view) {
            var current: android.view.ViewParent? = view.parent
            var foundWindow: android.view.Window? = (view as? DialogWindowProvider)?.window
            while (foundWindow == null && current != null) {
                if (current is DialogWindowProvider) {
                    foundWindow = current.window
                    break
                }
                current = current.parent
            }
            foundWindow
        }

        SideEffect {
            dialogWindow?.let { win ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    win.attributes = win.attributes.apply {
                        layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                    }
                }
                WindowCompat.setDecorFitsSystemWindows(win, false)
                win.statusBarColor = android.graphics.Color.TRANSPARENT
                win.navigationBarColor = android.graphics.Color.TRANSPARENT
                win.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    win.isNavigationBarContrastEnforced = false
                }
                val insetsController = WindowCompat.getInsetsController(win, win.decorView)
                insetsController.isAppearanceLightStatusBars = !isDark
                insetsController.isAppearanceLightNavigationBars = !isDark
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush = getPortalGradient(isDark))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                content()
            }
        }
    }
}
