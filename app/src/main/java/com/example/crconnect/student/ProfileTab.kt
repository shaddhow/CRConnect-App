package com.example.crconnect.student

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.ImageView
import androidx.compose.foundation.Image
import androidx.compose.runtime.key
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.shadow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
    onUpdateAdvisorInfo: (String, String) -> Unit = { _, _ -> },
    onUpdateCurrentTerm: (String) -> Unit = {},
    onSecuritySettings: () -> Unit = {},
    onCheckUpdate: () -> Unit = {},
    onSignOut: () -> Unit = {}
) {
    val scrollState = rememberScrollState()
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
        100
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Card 1: User Main Info & Avatar (.card.c)
        DesignCard(padding = 20.dp) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Large Avatar Circle (80dp x 80dp) with Camera Upload Tap Overlay
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clickable { onEditProfilePicture() },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(Color.Transparent),
                        contentAlignment = Alignment.Center
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
                                        .clickable { onEditProfilePicture() }
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
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                AnimatedAvatarPlaceholder(size = 80.dp, iconSize = 40.dp)
                            }
                        }
                    }

                    // Camera Badge Overlay protruding outside the bottom-right edge
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 2.dp, y = 2.dp)
                            .size(30.dp)
                            .shadow(4.dp, CircleShape)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(2.dp, Color(0xFF0066FF), CircleShape)
                            .clickable { onEditProfilePicture() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Edit photo",
                            tint = Color(0xFF0066FF),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                // Below avatar: Name, Email, Department chip
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = profile.name.ifBlank { "Student" },
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = profile.email.ifBlank { "student@bubt.edu.bd" },
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (profile.department.contains("CSE", ignoreCase = true)) "Department of CSE, BUBT" else profile.department,
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // 3 Stat Tiles (Student ID, Intake, Section)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    InfoTile(
                        label = "Student ID",
                        value = profile.studentId,
                        modifier = Modifier.weight(1f)
                    )
                    InfoTile(
                        label = "Intake",
                        value = profile.intake,
                        modifier = Modifier.weight(1f)
                    )
                    InfoTile(
                        label = "Section",
                        value = profile.section,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Card 2: Overview & Academic Details
        DesignCard(padding = 20.dp) {
            val progress = dynamicAttendancePct / 100f
            val animatedProgress by animateFloatAsState(
                targetValue = progress,
                animationSpec = tween(durationMillis = 600)
            )

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
                        text = "Verification status",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val isDark = isSystemInDarkTheme()
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (isDark) Color(0xFF0D3B24) else Color(0xFFDCFCE7))
                            .border(
                                1.dp,
                                if (isDark) Color(0xFF15803D).copy(alpha = 0.5f) else Color(0xFFBBF7D0),
                                CircleShape
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Verified · Locked",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFF4ADE80) else Color(0xFF166534)
                        )
                    }
                }

                KeyValueRow(
                    key = "Total enrolled courses",
                    value = "${maxOf(6, allClasses.map { it.courseName }.distinct().size)}",
                    valueColor = MaterialTheme.colorScheme.onSurface
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    KeyValueRow(
                        key = "Weekly Attendance overview",
                        value = if (evaluatedClasses.isNotEmpty()) {
                            "$dynamicAttendancePct% (${if (dynamicAttendancePct >= 80) "Excellent" else if (dynamicAttendancePct >= 60) "Good" else "Needs Attention"})"
                        } else {
                            "100% (Weekly Reset)"
                        },
                        valueColor = if (dynamicAttendancePct >= 80) Color(0xFF16A34A) else if (dynamicAttendancePct >= 60) Color(0xFF3B82F6) else Color(0xFFDC2626)
                    )
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (dynamicAttendancePct >= 80) Color(0xFF22C55E) else if (dynamicAttendancePct >= 60) Color(0xFF3B82F6) else Color(0xFFEF4444),
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
                                "No class time passed yet this week"
                            },
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                        )
                        Text(
                            text = "Resets weekly",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }

                // Interactive CGPA Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showCgpaDialog = true }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CGPA (current)",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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

        // Card: Theme Mode Selector
        DesignCard(padding = 20.dp) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Appearance & Theme",
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
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

        // Card: Academic Advisor & Term Info
        DesignCard(padding = 20.dp) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Academic Advisor & Term",
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
                        Text(
                            text = "Advisor (CR Editable)",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                // Interactive Current Term Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showTermDialog = true }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Current Term",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = profile.currentTerm,
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 14.sp,
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
                val context = LocalContext.current
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .then(
                            if (profile.advisorName.contains("Nasrin Akter", ignoreCase = true)) {
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
                    Text(
                        text = "Advisor",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = profile.advisorName.ifBlank { "Nasrin Akter" },
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (profile.advisorName.contains("Nasrin Akter", ignoreCase = true) || profile.advisorName.isBlank() || profile.advisorName == "--") {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = "View Profile",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
                KeyValueRow(
                    key = "Advisor Email",
                    value = profile.advisorEmail.ifBlank { "nasrinshipa@bubt.edu.bd" },
                    valueColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Card: Preferences & Support
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
                
                var notificationsEnabled by remember { mutableStateOf(true) }
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

        // Card 3: Action Buttons (Security settings, Log out)
        DesignCard(padding = 20.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Security settings
                OutlinedButton(
                    onClick = onSecuritySettings,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = Color(0xFF3B82F6)
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color(0xFF3B82F6),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Security settings",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF3B82F6)
                        )
                    }
                }

                // Log out Danger Button (Theme-aware Danger styling)
                val dangerBg = if (isDark) Color(0xFF450A0A) else Color(0xFFFEE2E2)
                val dangerText = if (isDark) Color(0xFFF87171) else Color(0xFFDC2626)
                val dangerStroke = if (isDark) Color(0xFF991B1B) else Color(0xFFFCA5A5)

                Button(
                    onClick = onSignOut,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = dangerBg,
                        contentColor = dangerText
                    ),
                    border = BorderStroke(1.5.dp, dangerStroke)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = null,
                            tint = dangerText,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Log out",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = dangerText
                        )
                    }
                }
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
    onClick: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 12.dp, horizontal = 4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                fontFamily = PlusJakartaSansFontFamily,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value.ifBlank { "--" },
                fontFamily = PlusJakartaSansFontFamily,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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
