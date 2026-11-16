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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.signature.ObjectKey
import com.example.crconnect.ProfileImageCacheHelper
import java.io.File
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material.icons.filled.Assessment
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
    onNavigateToAttendance: () -> Unit = {},
    onSignOut: () -> Unit = {},
    onLogout: () -> Unit = onSignOut
) {
    val scrollState = rememberScrollState()
    var showAcademicOverviewPage by remember { mutableStateOf(false) }
    var showAdvisorPage by remember { mutableStateOf(false) }
    var showSettingsPage by remember { mutableStateOf(false) }
    var showAttendanceSubPage by remember { mutableStateOf(false) }
    var notificationsEnabled by remember { mutableStateOf(true) }
    var showStudentIdDialog by remember { mutableStateOf(false) }

    var showSectionDialog by remember { mutableStateOf(false) }
    var sectionInput by remember(profile.section) { mutableStateOf(profile.section) }

    var showCgpaDialog by remember { mutableStateOf(false) }
    var cgpaInput by remember(profile.cgpa) { mutableStateOf(profile.cgpa) }

    var showAdvisorDialog by remember { mutableStateOf(false) }
    var advisorNameInput by remember(profile.advisorName) { mutableStateOf(profile.advisorName) }
    var advisorEmailInput by remember(profile.advisorEmail) { mutableStateOf(profile.advisorEmail) }

    var showTermDialog by remember { mutableStateOf(false) }
    var termInput by remember(profile.currentTerm) { mutableStateOf(profile.currentTerm) }

    val context = LocalContext.current
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

    if (showCgpaDialog) {
        AlertDialog(
            onDismissRequest = { showCgpaDialog = false },
            title = {
                Text(
                    text = "Update CGPA",
                    fontFamily = PlusJakartaSansFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Enter your current CGPA:",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = cgpaInput,
                        onValueChange = { cgpaInput = it },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = if (isDark) Color(0xFF1F2937) else Color(0xFFF8FAFC),
                            unfocusedContainerColor = if (isDark) Color(0xFF1F2937) else Color(0xFFF8FAFC),
                            disabledContainerColor = if (isDark) Color(0xFF1F2937) else Color(0xFFF8FAFC),
                            focusedBorderColor = Color(0xFF0066FF),
                            unfocusedBorderColor = if (isDark) Color(0x33FFFFFF) else Color(0xFFCBD5E1),
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (cgpaInput.isNotBlank()) {
                            onUpdateCgpa(cgpaInput.trim())
                            showCgpaDialog = false
                        }
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Save", fontFamily = PlusJakartaSansFontFamily, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showCgpaDialog = false },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Cancel", fontFamily = PlusJakartaSansFontFamily, fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (showAdvisorDialog) {
        AlertDialog(
            onDismissRequest = { showAdvisorDialog = false },
            title = {
                Text(
                    text = "Update Academic Advisor",
                    fontFamily = PlusJakartaSansFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Enter advisor details (CR Access):",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = advisorNameInput,
                        onValueChange = { advisorNameInput = it },
                        label = { Text("Advisor Name") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = advisorEmailInput,
                        onValueChange = { advisorEmailInput = it },
                        label = { Text("Advisor Email") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (advisorNameInput.isNotBlank()) {
                            onUpdateAdvisorInfo(advisorNameInput.trim(), advisorEmailInput.trim())
                            showAdvisorDialog = false
                        }
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Save", fontFamily = PlusJakartaSansFontFamily, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showAdvisorDialog = false },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Cancel", fontFamily = PlusJakartaSansFontFamily, fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (showTermDialog) {
        AlertDialog(
            onDismissRequest = { showTermDialog = false },
            title = {
                Text(
                    text = "Update Current Term",
                    fontFamily = PlusJakartaSansFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Enter current term/semester:",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = termInput,
                        onValueChange = { termInput = it },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (termInput.isNotBlank()) {
                            onUpdateCurrentTerm(termInput.trim())
                            showTermDialog = false
                        }
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Save", fontFamily = PlusJakartaSansFontFamily, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showTermDialog = false },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Cancel", fontFamily = PlusJakartaSansFontFamily, fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (showStudentIdDialog) {
        val clipboardManager = LocalClipboardManager.current
        val rawId = profile.studentId
        val isValidId = rawId.isNotBlank() && rawId != "--" && !rawId.contains("@") && !rawId.equals(profile.email, ignoreCase = true)

        AlertDialog(
            onDismissRequest = {
                showStudentIdDialog = false
            },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF0066FF).copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Badge,
                            contentDescription = "ID Card",
                            tint = Color(0xFF0066FF),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Student ID & Email",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Academic Credentials & Details",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // --- 1. Student ID Capsule Box ---
                    Surface(
                        shape = CircleShape,
                        color = if (isDark) Color(0xFF161F30) else Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, if (isDark) Color(0x33FFFFFF) else Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "STUDENT ID",
                                        fontFamily = PlusJakartaSansFontFamily,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0066FF),
                                        letterSpacing = 0.5.sp
                                    )
                                    if (isValidId) {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xFF10B981).copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = "Verified",
                                                fontFamily = PlusJakartaSansFontFamily,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF10B981),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    } else {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xFFEF4444).copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = "Not Set",
                                                fontFamily = PlusJakartaSansFontFamily,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFEF4444),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = if (isValidId) rawId else "Not Set",
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isValidId) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (isValidId) {
                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(rawId))
                                        ToastHelper.show(context, "Student ID copied to clipboard")
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy ID",
                                        tint = Color(0xFF0066FF),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    // --- 2. Email Address Capsule Box ---
                    Surface(
                        shape = CircleShape,
                        color = if (isDark) Color(0xFF161F30) else Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, if (isDark) Color(0x33FFFFFF) else Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "REGISTERED EMAIL",
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0066FF),
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = profile.email.ifBlank { "student@gmail.com" },
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(profile.email))
                                    ToastHelper.show(context, "Email copied to clipboard")
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy Email",
                                    tint = Color(0xFF0066FF),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // --- 3. Academic Details Chips ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, if (isDark) Color(0x33FFFFFF) else Color(0xFFE2E8F0)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "Intake ${profile.intake.ifBlank { "--" }}",
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Surface(
                            shape = CircleShape,
                            color = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0),
                            border = BorderStroke(1.dp, if (isDark) Color(0x33FFFFFF) else Color(0xFFE2E8F0)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "Section ${profile.section.ifBlank { "--" }}",
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Surface(
                            shape = CircleShape,
                            color = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0),
                            border = BorderStroke(1.dp, if (isDark) Color(0x33FFFFFF) else Color(0xFFE2E8F0)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (profile.department.contains("CSE", true)) "CSE" else profile.department,
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showStudentIdDialog = false },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0066FF))
                ) {
                    Text("Done", fontFamily = PlusJakartaSansFontFamily, fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = if (isDark) Color(0xFF121622) else Color(0xFFFFFFFF)
        )
    }

    if (showSectionDialog) {
        AlertDialog(
            onDismissRequest = { showSectionDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF0066FF).copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Section",
                            tint = Color(0xFF0066FF),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Edit Class Section",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Intake ${profile.intake.ifBlank { "--" }} • Current Section ${profile.section.ifBlank { "--" }}",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            text = {
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
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleanSec = sectionInput.trim()
                        if (cleanSec.isNotBlank()) {
                            onUpdateSection(cleanSec)
                            showSectionDialog = false
                        } else {
                            ToastHelper.show(context, "Please enter a valid section")
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0066FF))
                ) {
                    Text("Save Section", fontFamily = PlusJakartaSansFontFamily, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        sectionInput = profile.section
                        showSectionDialog = false
                    },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, if (isDark) Color(0x33FFFFFF) else Color(0xFFE2E8F0))
                ) {
                    Text("Cancel", fontFamily = PlusJakartaSansFontFamily, fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = if (isDark) Color(0xFF121622) else Color(0xFFFFFFFF)
        )
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
                        // 1. Weekly Attendance overview Card
                        DesignCard(
                            padding = 20.dp,
                            onClick = { showAttendanceSubPage = true }
                        ) {
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
                                        text = "Weekly Attendance overview",
                                        fontFamily = PlusJakartaSansFontFamily,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Surface(
                                        shape = CircleShape,
                                        color = if (evaluatedClasses.isEmpty()) {
                                            MaterialTheme.colorScheme.surfaceVariant
                                        } else if (dynamicAttendancePct >= 80) {
                                            Color(0xFF10B981).copy(alpha = if (isDark) 0.2f else 0.15f)
                                        } else if (dynamicAttendancePct >= 60) {
                                            Color(0xFF3B82F6).copy(alpha = if (isDark) 0.2f else 0.15f)
                                        } else {
                                            Color(0xFFEF4444).copy(alpha = if (isDark) 0.2f else 0.15f)
                                        }
                                    ) {
                                        Text(
                                            text = if (evaluatedClasses.isEmpty()) {
                                                "New Week"
                                            } else if (dynamicAttendancePct >= 80) {
                                                "Excellent"
                                            } else if (dynamicAttendancePct >= 60) {
                                                "Good"
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
                                            },
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                KeyValueRow(
                                    key = "Attendance Rate",
                                    value = if (evaluatedClasses.isNotEmpty()) {
                                        "$dynamicAttendancePct% (${if (dynamicAttendancePct >= 80) "Excellent" else if (dynamicAttendancePct >= 60) "Good" else "Needs Attention"})"
                                    } else {
                                        "0% (0/0 classes)"
                                    },
                                    valueColor = if (evaluatedClasses.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else if (dynamicAttendancePct >= 80) (if (isDark) Color(0xFF4ADE80) else Color(0xFF16A34A)) else if (dynamicAttendancePct >= 60) (if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB)) else (if (isDark) Color(0xFFF87171) else Color(0xFFDC2626))
                                )

                                LinearProgressIndicator(
                                    progress = { fullAnimatedProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = if (dynamicAttendancePct >= 80) (if (isDark) Color(0xFF4ADE80) else Color(0xFF22C55E)) else if (dynamicAttendancePct >= 60) (if (isDark) Color(0xFF60A5FA) else Color(0xFF3B82F6)) else (if (isDark) Color(0xFFF87171) else Color(0xFFEF4444)),
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (evaluatedClasses.isNotEmpty()) {
                                            "${attendedClasses.size} attended · ${missedClasses.size} missed (${evaluatedClasses.size} evaluated)"
                                        } else {
                                            "New week (Sunday start) · 0/0 classes"
                                        },
                                        fontFamily = PlusJakartaSansFontFamily,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Resets Sunday",
                                        fontFamily = PlusJakartaSansFontFamily,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }

                        // 2. Attendance Records & History Action Card
                        DesignCard(
                            onClick = { showAttendanceSubPage = true },
                            padding = 16.dp
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .background(
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.22f else 0.12f),
                                            shape = RoundedCornerShape(14.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Assessment,
                                        contentDescription = "Attendance Records",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Text(
                                        text = "Attendance Records & History",
                                        fontFamily = PlusJakartaSansFontFamily,
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Subject-wise log, calendar & full records",
                                        fontFamily = PlusJakartaSansFontFamily,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .background(
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.18f else 0.08f),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                        contentDescription = "Open",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        // 3. Current Term Card
                        DesignCard(padding = 20.dp) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Academic Term",
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

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
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "Tap to edit current semester/term",
                                            fontFamily = PlusJakartaSansFontFamily,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Normal,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = profile.currentTerm.ifBlank { "Winter 2026 (Year 2)" },
                                            fontFamily = PlusJakartaSansFontFamily,
                                            fontSize = 14.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit Term",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // 4. Total Enrolled Courses Card
                        DesignCard(padding = 20.dp) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Course Schedule",
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = "Total enrolled courses",
                                            fontFamily = PlusJakartaSansFontFamily,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "Automatically synced with your Routine schedule",
                                            fontFamily = PlusJakartaSansFontFamily,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Normal,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                    ) {
                                        Text(
                                            text = "$dynamicCourseCount",
                                            fontFamily = PlusJakartaSansFontFamily,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // 5. CGPA (current) Card
                        DesignCard(padding = 20.dp) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Academic Standing",
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

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
                                            text = "CGPA (current)",
                                            fontFamily = PlusJakartaSansFontFamily,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "Grading scale: 4.00 · Tap to update",
                                            fontFamily = PlusJakartaSansFontFamily,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Normal,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = profile.cgpa.ifBlank { "--" },
                                            fontFamily = PlusJakartaSansFontFamily,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit CGPA",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                    }
        }
    }

    if (showAttendanceSubPage) {
        FullScreenSubPageDialog(
            onDismissRequest = { showAttendanceSubPage = false },
            isDark = isDark
        ) {
            BackHandler {
                showAttendanceSubPage = false
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
                    onClick = { showAttendanceSubPage = false }
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
                        text = "Attendance Records",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Weekly status & detailed breakdown",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            AttendanceTab(
                uiState = uiState,
                modifier = Modifier.fillMaxSize()
            )
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
            val advisorEmail = profile.advisorEmail.ifBlank { "nasrinshipa@bubt.edu.bd" }

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
                                            text = profile.advisorName.ifBlank { "Nasrin Akter" },
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
                        // 1. Appearance & Theme Card
                        DesignCard(padding = 20.dp) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = "Appearance & Theme",
                                        fontFamily = PlusJakartaSansFontFamily,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Select your preferred color theme",
                                        fontFamily = PlusJakartaSansFontFamily,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    ThemeOptionButton(
                                        label = "System",
                                        selected = themeMode == SessionManager.THEME_SYSTEM,
                                        onClick = { onThemeModeSelected(SessionManager.THEME_SYSTEM) },
                                        modifier = Modifier.weight(1f)
                                    )
                                    ThemeOptionButton(
                                        label = "Light",
                                        selected = themeMode == SessionManager.THEME_LIGHT,
                                        onClick = { onThemeModeSelected(SessionManager.THEME_LIGHT) },
                                        modifier = Modifier.weight(1f)
                                    )
                                    ThemeOptionButton(
                                        label = "Dark",
                                        selected = themeMode == SessionManager.THEME_DARK,
                                        onClick = { onThemeModeSelected(SessionManager.THEME_DARK) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        // 2. Account & Profile Customization Card
                        DesignCard(padding = 20.dp) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Text(
                                    text = "Account & Profile",
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                // Change Profile Picture option
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            onEditProfilePicture()
                                        }
                                        .padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primaryContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CameraAlt,
                                                contentDescription = "Camera",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text(
                                                text = "Change Profile Picture",
                                                fontFamily = PlusJakartaSansFontFamily,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Upload photo from camera or gallery",
                                                fontFamily = PlusJakartaSansFontFamily,
                                                fontSize = 11.5.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                        contentDescription = "Edit photo",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                // Security Settings option
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            onSecuritySettings()
                                        }
                                        .padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primaryContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Lock,
                                                contentDescription = "Security",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text(
                                                text = "Security Settings",
                                                fontFamily = PlusJakartaSansFontFamily,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Password reset & biometric preferences",
                                                fontFamily = PlusJakartaSansFontFamily,
                                                fontSize = 11.5.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                        contentDescription = "Open Security",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        // 3. Preferences & Support Card
                        DesignCard(padding = 20.dp) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Preferences & Support",
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = "Class & Notice Alerts",
                                            fontFamily = PlusJakartaSansFontFamily,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Receive push alerts for routines & exams",
                                            fontFamily = PlusJakartaSansFontFamily,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Switch(
                                        checked = notificationsEnabled,
                                        onCheckedChange = { notificationsEnabled = it }
                                    )
                                }

                                // App Version & Blinking Update Alert Row
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

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            if (isUpdateAvailable) {
                                                try {
                                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(updateUrl))
                                                    context.startActivity(intent)
                                                } catch (e: Exception) {
                                                    ToastHelper.show(context, "Could not open update link")
                                                }
                                            } else {
                                                onCheckUpdate()
                                            }
                                        }
                                        .padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "App Version",
                                        fontFamily = PlusJakartaSansFontFamily,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    if (isUpdateAvailable) {
                                        Surface(
                                            shape = RoundedCornerShape(999.dp),
                                            color = Color(0xFF0066FF).copy(alpha = pulseAlpha),
                                            border = BorderStroke(1.dp, Color(0xFF00E5FF))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = "Update Available ($latestTag) ⚡",
                                                    fontFamily = PlusJakartaSansFontFamily,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                                    contentDescription = "Update Now",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            }
                                        }
                                    } else {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = "CRConnect v$versionName (BUBT)",
                                                fontFamily = PlusJakartaSansFontFamily,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
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
        // Card 1: Smart Student ID Badge (Apple Wallet / Physical Card style)
        val displayStudentId = if (profile.studentId.isBlank() || profile.studentId == "--" || profile.studentId.contains("@") || profile.studentId.equals(profile.email, ignoreCase = true)) {
            "Tap to set"
        } else {
            profile.studentId
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = if (isDark) 0.dp else 8.dp,
                    shape = RoundedCornerShape(22.dp),
                    ambientColor = Color(0x12000000),
                    spotColor = Color(0x12000000)
                )
                .clip(RoundedCornerShape(22.dp))
                .background(if (isDark) Color(0xFF131B2E) else Color(0xFFFFFFFF))
                .border(
                    width = 1.dp,
                    color = if (isDark) Color(0xFF24354D) else Color(0xFFE2E8F0),
                    shape = RoundedCornerShape(22.dp)
                )
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Top Accent Stripe
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.5.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF0052CC), Color(0xFF3B82F6), Color(0xFF06B6D4))
                            )
                        )
                )

                // 1. Header Ribbon: University Identity + Status
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isDark) Color(0x263B82F6) else Color(0x140066FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = null,
                                tint = if (isDark) Color(0xFF60A5FA) else Color(0xFF0066FF),
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        Text(
                            text = "BUBT · STUDENT PASS",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                            letterSpacing = 1.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (isDark) Color(0x2622C55E) else Color(0x1422C55E))
                            .border(1.dp, if (isDark) Color(0x4022C55E) else Color(0x3322C55E), CircleShape)
                            .padding(horizontal = 9.dp, vertical = 3.5.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF22C55E))
                            )
                            Text(
                                text = "Active",
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFF4ADE80) else Color(0xFF15803D)
                            )
                        }
                    }
                }

                // Hairline Divider
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(if (isDark) Color(0x1AFFFFFF) else Color(0x0F000000))
                )

                // 2. Horizontal Student ID Body (Photo on Left, Details on Right)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Circular Photo on Left
                    Box(
                        modifier = Modifier
                            .size(86.dp)
                            .clickable { onEditProfilePicture() },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(82.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9))
                                .border(
                                    2.dp,
                                    if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1),
                                    CircleShape
                                )
                                .padding(2.dp)
                                .clip(CircleShape)
                        ) {
                            key(profile.profileImageUrl) {
                                val bitmap = remember(profile.profileImageUrl) {
                                    val path = profile.profileImageUrl
                                    try {
                                        if (path.isNotBlank()) {
                                            if (!path.startsWith("http") && !path.startsWith("data:") && !path.startsWith("content:")) {
                                                val file = File(path)
                                                if (file.exists() && file.length() > 0) BitmapFactory.decodeFile(file.absolutePath) else null
                                            } else null
                                        } else null
                                    } catch (e: Exception) {
                                        null
                                    }
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
                                                scaleType = ImageView.ScaleType.CENTER_CROP
                                                setOnClickListener { onEditProfilePicture() }
                                                Glide.with(ctx)
                                                    .load(profile.profileImageUrl)
                                                    .circleCrop()
                                                    .signature(ObjectKey(profile.profileImageUrl))
                                                    .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                                                    .into(this)
                                            }
                                        },
                                        update = { view ->
                                            view.setOnClickListener { onEditProfilePicture() }
                                            Glide.with(view.context)
                                                .load(profile.profileImageUrl)
                                                .circleCrop()
                                                .signature(ObjectKey(profile.profileImageUrl))
                                                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                                                .into(view)
                                        },
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                    )
                                } else {
                                    AnimatedAvatarPlaceholder(size = 80.dp, iconSize = 38.dp)
                                }
                            }
                        }

                        // Camera Badge Overlay
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .offset(x = 1.dp, y = 1.dp)
                                .size(24.dp)
                                .shadow(2.dp, CircleShape)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0xFF0F172A) else Color.White)
                                .border(1.5.dp, Color(0xFF0066FF), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Edit photo",
                                tint = Color(0xFF0066FF),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }

                    // Student Details on Right
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = profile.name.ifBlank { "Student" },
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 16.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFFE8F0FE) else Color(0xFF0F172A),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = profile.email.ifBlank { "student@gmail.com" },
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isDark) Color(0xFF8898B0) else Color(0xFF64748B),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        // Student ID Pill (Clickable to edit)
                        Row(
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isDark) Color(0x243B82F6) else Color(0x140066FF))
                                .border(1.dp, if (isDark) Color(0x403B82F6) else Color(0x2E0066FF), RoundedCornerShape(8.dp))
                                .clickable { showStudentIdDialog = true }
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "ID: $displayStudentId",
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFF60A5FA) else Color(0xFF0066FF)
                            )
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = if (isDark) Color(0xFF60A5FA) else Color(0xFF0066FF),
                                modifier = Modifier.size(10.dp)
                            )
                        }

                        // Intake & Section Row
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            // Intake Tag
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isDark) Color(0x1AFFFFFF) else Color(0x0A000000))
                                    .border(1.dp, if (isDark) Color(0x14FFFFFF) else Color(0x10000000), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 7.dp, vertical = 2.5.dp)
                            ) {
                                Text(
                                    text = "Intake ${profile.intake}",
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                                )
                            }

                            // Section Tag (Clickable to edit)
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isDark) Color(0x1AFFFFFF) else Color(0x0A000000))
                                    .border(1.dp, if (isDark) Color(0x14FFFFFF) else Color(0x10000000), RoundedCornerShape(6.dp))
                                    .clickable {
                                        sectionInput = profile.section
                                        showSectionDialog = true
                                    }
                                    .padding(horizontal = 7.dp, vertical = 2.5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = "Sec ${profile.section}",
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                                )
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = if (isDark) Color(0xFF8898B0) else Color(0xFF64748B),
                                    modifier = Modifier.size(10.dp)
                                )
                            }

                            // CR Badge (if applicable)
                            if (profile.isCr) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0x26F59E0B))
                                        .border(1.dp, Color(0x66F59E0B), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.5.dp)
                                ) {
                                    Text(
                                        text = "⚡ CR",
                                        fontFamily = PlusJakartaSansFontFamily,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFFD97706)
                                    )
                                }
                            }
                        }
                    }
                }

                // Hairline Divider
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(if (isDark) Color(0x1AFFFFFF) else Color(0x0F000000))
                )

                // 3. Footer Barcode & Official Branding Strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isDark) Color(0x0AFFFFFF) else Color(0x05000000))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val barWidths = listOf(2, 3, 1, 4, 1, 3, 2, 1, 4, 2, 3, 1, 2, 4, 1, 3, 2)
                        barWidths.forEach { w ->
                            Box(
                                modifier = Modifier
                                    .width(w.dp)
                                    .height(13.dp)
                                    .background(if (isDark) Color(0xFF475569) else Color(0xFF94A3B8))
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = displayStudentId,
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
                            letterSpacing = 0.8.sp
                        )
                    }

                    Text(
                        text = "CSE · BUBT DIGITAL ID",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFF475569) else Color(0xFFA0AEC0),
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }

        // Card 2: Live Bento Tile - Academic Overview
        DesignCard(
            padding = 16.dp,
            onClick = { showAcademicOverviewPage = true }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF2563EB), Color(0xFF1D4ED8))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = "Academic Overview",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Academic Overview",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 15.sp,
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

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (profile.cgpa.isNotBlank() && profile.cgpa != "--") {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = "CGPA ${profile.cgpa}",
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = "Open Academic Overview",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // Card 3: Live Bento Tile - Academic Advisor
        DesignCard(
            padding = 16.dp,
            onClick = { showAdvisorPage = true }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF059669), Color(0xFF047857))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SupervisorAccount,
                            contentDescription = "Academic Advisor",
                            tint = Color.White,
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
                            text = profile.advisorName.ifBlank { "Advisor details & contact info" },
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = "Assigned",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFF4ADE80) else Color(0xFF059669),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = "Open Academic Advisor",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // Card 4: Live Bento Tile - Settings & Security
        DesignCard(
            padding = 16.dp,
            onClick = { showSettingsPage = true }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF7C3AED), Color(0xFF6D28D9))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Settings & Security",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 15.sp,
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

                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = "Open Settings",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // Log out Full-Width Refined Action Button
        val logoutBg = if (isDark) Color(0xFF2C1014) else Color(0xFFFEF2F2)
        val logoutStroke = if (isDark) Color(0xFF7F1D1D) else Color(0xFFFECACA)
        val logoutText = if (isDark) Color(0xFFEF4444) else Color(0xFFB91C1C)

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .clickable { onLogout() },
            shape = RoundedCornerShape(16.dp),
            color = logoutBg,
            border = BorderStroke(1.dp, logoutStroke)
        ) {
            Row(
                modifier = Modifier.padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    tint = logoutText,
                    modifier = Modifier.size(19.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Log out",
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = logoutText
                )
            }
        }

        Spacer(modifier = Modifier.height(84.dp))
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
private fun FullScreenSubPageDialog(
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
