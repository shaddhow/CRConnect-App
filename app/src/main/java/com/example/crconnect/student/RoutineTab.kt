package com.example.crconnect.student

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.text.SimpleDateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.compose.material.icons.filled.Star
import com.example.crconnect.RoutineCameraActivity
import com.example.crconnect.core.utils.HapticUtils
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun RoutineTab(
    uiState: StudentPortalUiState,
    onDaySelected: (Int) -> Unit,
    onOpenAddClass: () -> Unit,
    onCloseAddClass: () -> Unit,
    onAddClass: (courseName: String, timeSlot: String, room: String) -> Unit,
    onOpenNoticeSheet: () -> Unit = {},
    onDeleteRoutine: () -> Unit = {},
    onScannedEntriesAdded: (List<ClassEntry>) -> Unit = { _ -> },
    onLoadSampleRoutine: () -> Unit = {},
    onNavigateWeek: (Int) -> Unit = {},
    onToggleAttendance: (classId: String, targetState: String) -> Unit = { _, _ -> },
    onShowToast: (String) -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scrollState = rememberScrollState()
    val isDark = LocalIsDarkTheme.current
    var dragOffsetX by remember { mutableFloatStateOf(0f) }

    val selectedDay = uiState.scheduleDays.find { it.dayIndex == uiState.selectedDayIndex }
        ?: uiState.scheduleDays.last()
    val classesForSelectedDay = (uiState.classMap[uiState.selectedDayIndex] ?: emptyList())
        .sortedBy { startMinutes(it.timeSlot) }

    val isOverallRoutineEmpty = remember(uiState.classMap) {
        uiState.classMap.isEmpty() || uiState.classMap.values.all { it.isEmpty() }
    }

    var isOcrLoading by remember { mutableStateOf(false) }

    // Real-time ticker: updates every 10 seconds so greeting & up next info recalculate live
    var currentTimeMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(10_000L)
            currentTimeMillis = System.currentTimeMillis()
        }
    }

    val cal = remember(currentTimeMillis) {
        Calendar.getInstance().apply { timeInMillis = currentTimeMillis }
    }

    val greetingTitle = remember(cal) {
        getDynamicGreetingTitle(cal)
    }

    val greetingSubtitle = remember(cal, uiState.classMap) {
        getDynamicGreetingSubtitle(cal, uiState)
    }

    val upNextInfo = remember(currentTimeMillis, uiState.classMap, uiState.selectedDayIndex) {
        getUpNextClassInfo(uiState, currentTimeMillis)
    }

    fun processSelectedImage(uri: Uri) {
        onCloseAddClass()
        isOcrLoading = true
        RoutineOcrParser.processOcrImage(
            context = context,
            imageUri = uri,
            onSuccess = { entries ->
                isOcrLoading = false
                if (entries.isNotEmpty()) {
                    onScannedEntriesAdded(entries)
                    onShowToast("Routine processed successfully! (${entries.size} classes found)")
                } else {
                    onShowToast("No classes detected in image. Please try a clearer photo.")
                }
            },
            onFailure = { e ->
                isOcrLoading = false
                onShowToast("Failed to process routine image: ${e.message}")
            }
        )
    }

    val cameraActivityLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val capturedUriString = result.data?.getStringExtra(RoutineCameraActivity.EXTRA_CAPTURED_URI)
                ?: result.data?.dataString
            if (!capturedUriString.isNullOrEmpty()) {
                processSelectedImage(Uri.parse(capturedUriString))
            } else {
                val photoUri = result.data?.data
                if (photoUri != null) {
                    processSelectedImage(photoUri)
                }
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val intent = Intent(context, RoutineCameraActivity::class.java)
            cameraActivityLauncher.launch(intent)
        } else {
            onShowToast("Camera permission is required to scan routine")
        }
    }

    val galleryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            processSelectedImage(uri)
        }
    }

    val storagePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            galleryPickerLauncher.launch("image/*")
        } else {
            galleryPickerLauncher.launch("image/*")
        }
    }

    val onScanCameraClick = {
        val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            val intent = Intent(context, RoutineCameraActivity::class.java)
            cameraActivityLauncher.launch(intent)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val onChooseGalleryClick = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES)
            if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                galleryPickerLauncher.launch("image/*")
            } else {
                storagePermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
            }
        } else {
            val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
            if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                galleryPickerLauncher.launch("image/*")
            } else {
                storagePermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. GREETING ROW + ADD CLASS BUTTON
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Calendar icon container
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(if (isDark) Color(0x263B82F6) else Color(0x1A3B82F6)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = Color(0xFF3B82F6),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Greeting & Date
                Column {
                    Text(
                        text = greetingTitle,
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFFE8F0FE) else Color(0xFF1A2540)
                    )
                    Text(
                        text = greetingSubtitle,
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 13.5.sp,
                        color = if (isDark) Color(0xFF6B7F9E) else Color(0xFF8898B0)
                    )
                }
            }

            // Circular Add Button (+)
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .shadow(elevation = 6.dp, shape = CircleShape, ambientColor = Color(0x663B82F6), spotColor = Color(0x663B82F6))
                    .clip(CircleShape)
                    .background(Color(0xFF3B82F6))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onOpenAddClass() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add class",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // 3. "UP NEXT" HERO CARD
        upNextInfo?.let { info ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 10.dp,
                        shape = RoundedCornerShape(26.dp),
                        ambientColor = if (isDark) Color(0x66000000) else Color(0x400052CC),
                        spotColor = if (isDark) Color(0x66000000) else Color(0x400052CC)
                    ),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 185.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(
                            brush = if (isDark) {
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFF0F172A), Color(0xFF1E3A8A), Color(0xFF2563EB))
                                )
                            } else {
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFF0052CC), Color(0xFF0066FF), Color(0xFF2563EB))
                                )
                            }
                        )
                ) {
                    // Layered Background Decorative Circles
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clip(RoundedCornerShape(26.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 35.dp, y = (-25).dp)
                                .size(185.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.09f))
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .offset(x = 45.dp, y = 35.dp)
                                .size(160.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.06f))
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 30.dp),
                        verticalArrangement = Arrangement.spacedBy(22.dp)
                    ) {
                        // Card Top Row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = info.statusLabel,
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White.copy(alpha = 0.8f),
                                letterSpacing = 1.2.sp
                            )

                            // Glowing green dot
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .shadow(4.dp, CircleShape, ambientColor = Color(0xFF22C55E), spotColor = Color(0xFF22C55E))
                                    .clip(CircleShape)
                                    .background(Color(0xFF22C55E))
                            )

                            // Status pill
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color.White.copy(alpha = 0.18f))
                                    .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(20.dp))
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = info.statusPill,
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White.copy(alpha = 0.95f)
                                )
                            }
                        }

                        // Card Body Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = info.classModel.courseName,
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    lineHeight = 32.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = info.classModel.timeSlot,
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // White Room badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(Color.White)
                                    .padding(horizontal = 16.dp, vertical = 9.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "ROOM",
                                        fontFamily = PlusJakartaSansFontFamily,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0052CC),
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = extractCleanRoomNumber(info.classModel.room),
                                        fontFamily = PlusJakartaSansFontFamily,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF1A2540)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. CR CONTROL PANEL (Only for CR users)
        if (uiState.isCr) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 4.dp,
                        shape = RoundedCornerShape(20.dp),
                        ambientColor = Color(0x33D97706),
                        spotColor = Color(0x33D97706)
                    ),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF111827) else Color.White
                ),
                border = BorderStroke(1.dp, if (isDark) Color(0x1AFFFFFF) else Color(0x0F000000))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            GlassCrBadge()
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "CR Control Panel",
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 17.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "Sec " + uiState.profile.section,
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Text(
                        text = "As the official CR of Section ${uiState.profile.section}, you can broadcast urgent notices or extra class alerts to all classmates.",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 19.sp
                    )

                    Button(
                        onClick = onOpenNoticeSheet,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFD97706),
                            contentColor = Color.White
                        )
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Broadcast Class Notice",
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 5. WEEK STRIP CARD
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(if (isDark) Color(0xFF111827) else Color.White)
                .border(1.dp, if (isDark) Color(0x1AFFFFFF) else Color(0x0F000000), RoundedCornerShape(20.dp))
                .pointerInput(uiState.scheduleDays, uiState.selectedDayIndex) {
                    detectHorizontalDragGestures(
                        onDragStart = { dragOffsetX = 0f },
                        onHorizontalDrag = { _, dragAmount ->
                            dragOffsetX += dragAmount
                            val threshold = 18f
                            if (dragOffsetX < -threshold) {
                                // Swipe Left -> Next Day
                                val currentIndex = uiState.scheduleDays.indexOfFirst { it.dayIndex == uiState.selectedDayIndex }
                                if (currentIndex >= 0 && currentIndex < uiState.scheduleDays.size - 1) {
                                    HapticUtils.lightClick(view)
                                    onDaySelected(uiState.scheduleDays[currentIndex + 1].dayIndex)
                                    dragOffsetX = 0f
                                }
                            } else if (dragOffsetX > threshold) {
                                // Swipe Right -> Previous Day
                                val currentIndex = uiState.scheduleDays.indexOfFirst { it.dayIndex == uiState.selectedDayIndex }
                                if (currentIndex > 0) {
                                    HapticUtils.lightClick(view)
                                    onDaySelected(uiState.scheduleDays[currentIndex - 1].dayIndex)
                                    dragOffsetX = 0f
                                }
                            }
                        },
                        onDragEnd = {
                            dragOffsetX = 0f
                        }
                    )
                }
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Week Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = uiState.weekLabel,
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDark) Color(0xFF6B7F9E) else Color(0xFF8898B0)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (isDark) Color(0xFF1A2234) else Color(0xFFF1F5FB))
                                .clickable {
                                    HapticUtils.lightClick(view)
                                    onNavigateWeek(-1)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronLeft,
                                contentDescription = "Prev week",
                                tint = if (isDark) Color(0xFF6B7F9E) else Color(0xFF8898B0),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (isDark) Color(0xFF1A2234) else Color(0xFFF1F5FB))
                                .clickable {
                                    HapticUtils.lightClick(view)
                                    onNavigateWeek(1)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Next week",
                                tint = if (isDark) Color(0xFF6B7F9E) else Color(0xFF8898B0),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // 7 Days Grid Row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    uiState.scheduleDays.forEach { day ->
                        DayCellItem(
                            day = day,
                            isSelected = day.dayIndex == uiState.selectedDayIndex,
                            onClick = {
                                HapticUtils.lightClick(view)
                                onDaySelected(day.dayIndex)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 6. SECTION HEADER ROW
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "${getDayFullName(selectedDay.shortName)} classes",
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color(0xFFE8F0FE) else Color(0xFF1A2540)
                )
                Text(
                    text = formatTimeRangeForDay(classesForSelectedDay),
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 13.sp,
                    color = if (isDark) Color(0xFF6B7F9E) else Color(0xFF8898B0)
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isDark) Color(0x263B82F6) else Color(0x1A3B82F6))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (classesForSelectedDay.isNotEmpty()) "${classesForSelectedDay.size} ${if (classesForSelectedDay.size == 1) "class" else "classes"}" else "No classes",
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF3B82F6)
                )
            }
        }

        // 7. TIMELINE CLASS CARDS LIST
        if (classesForSelectedDay.isNotEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                classesForSelectedDay.forEachIndexed { index, classModel ->
                    val colorVariant = CardColorVariant.forIndex(index)
                    val isLastItem = index == classesForSelectedDay.size - 1

                    TimelineRowItem(
                        classModel = classModel,
                        colorVariant = colorVariant,
                        isLastItem = isLastItem,
                        onAttendanceChanged = { target ->
                            onToggleAttendance(classModel.id, target)
                        }
                    )
                }
            }
        } else if (isOverallRoutineEmpty) {
            // EMPTY STATE 2 — No routine at all (Onboarding)
            RoutineOnboardingEmptyCard(
                isDark = isDark,
                onScanRoutine = onScanCameraClick,
                onAddManually = onOpenAddClass
            )
        } else {
            // EMPTY STATE 1 — No classes on this specific day
            RoutineDayEmptyCard(
                isDark = isDark,
                dayName = getDayFullName(selectedDay.shortName),
                shortDay = selectedDay.shortName,
                onAddClass = onOpenAddClass
            )
        }

        Spacer(modifier = Modifier.height(80.dp))
    }

    // 8. ADD CLASS BOTTOM SHEET
    if (uiState.isAddClassSheetOpen) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = onCloseAddClass,
            sheetState = sheetState,
            containerColor = if (isDark) Color(0xFF111827) else Color.White,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            windowInsets = WindowInsets(0, 0, 0, 0),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp, bottom = 4.dp)
                        .size(width = 40.dp, height = 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (isDark) Color(0x33FFFFFF) else Color(0x33000000))
                )
            }
        ) {
            AddClassBottomSheetContent(
                selectedDayName = getDayFullName(selectedDay.shortName),
                initialManualMode = uiState.isAddClassManualMode,
                onAddClass = onAddClass,
                onScanCamera = onScanCameraClick,
                onChooseGallery = onChooseGalleryClick,
                onDeleteRoutine = {
                    onDeleteRoutine()
                    onCloseAddClass()
                },
                onShowToast = onShowToast
            )
        }
    }

    // 9. OCR LOADING DIALOG
    if (isOcrLoading) {
        Dialog(onDismissRequest = { }) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isDark) Color(0xFF111827) else Color.White)
                    .padding(28.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFF3B82F6),
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(44.dp)
                    )
                    Text(
                        text = "Reading your routine...",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFFE8F0FE) else Color(0xFF1A2540),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════
// DAY CELL ITEM
// ══════════════════════════════════════════════════════════════
@Composable
private fun DayCellItem(
    day: DayModel,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current

    val bgModifier = if (isSelected) {
        Modifier
            .background(Color(0xFF3B82F6))
            .shadow(6.dp, RoundedCornerShape(13.dp), ambientColor = Color(0x553B82F6), spotColor = Color(0x553B82F6))
    } else {
        Modifier.background(Color.Transparent)
    }

    val dayNameColor = when {
        isSelected -> Color.White.copy(alpha = 0.8f)
        isDark -> Color(0xFF6B7F9E)
        else -> Color(0xFFB0BCCF)
    }

    val dayNumColor = when {
        isSelected -> Color.White
        day.hasClasses -> Color(0xFF3B82F6)
        isDark -> Color(0xFF8898B0)
        else -> Color(0xFF6B7F9E)
    }

    val dotColor = when {
        isSelected -> Color.White.copy(alpha = 0.9f)
        day.hasClasses -> Color(0xFF3B82F6)
        isDark -> Color(0xFF2A3A52)
        else -> Color(0xFFCBD5E1)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(13.dp))
            .then(bgModifier)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = day.shortName,
                fontFamily = PlusJakartaSansFontFamily,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = dayNameColor,
                letterSpacing = 0.2.sp
            )

            Text(
                text = day.dateNumber.toString(),
                fontFamily = PlusJakartaSansFontFamily,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = dayNumColor,
                lineHeight = 18.sp
            )

            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════
// TIMELINE ROW ITEM
// ══════════════════════════════════════════════════════════════
@Composable
private fun TimelineRowItem(
    classModel: ClassModel,
    colorVariant: CardColorVariant,
    isLastItem: Boolean,
    onAttendanceChanged: (String) -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    val (hourText, amPmText) = extractTimeParts(classModel.timeSlot)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Max),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Time Column
        Column(
            modifier = Modifier
                .width(52.dp)
                .padding(top = 16.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                text = hourText,
                fontFamily = PlusJakartaSansFontFamily,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color(0xFFE8F0FE) else Color(0xFF1A2540),
                lineHeight = 16.sp
            )
            Text(
                text = amPmText,
                fontFamily = PlusJakartaSansFontFamily,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = if (isDark) Color(0xFF6B7F9E) else Color(0xFF8898B0)
            )
        }

        // 2. Timeline Line & Dot
        Column(
            modifier = Modifier
                .padding(top = 16.dp, bottom = 12.dp)
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Status Dot
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .shadow(4.dp, CircleShape, ambientColor = colorVariant.dotColor, spotColor = colorVariant.dotColor)
                    .clip(CircleShape)
                    .background(colorVariant.dotColor)
            )

            // Continuous Dynamic Segment Line
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .width(2.dp)
                    .weight(1f)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        brush = if (isLastItem) {
                            Brush.verticalGradient(
                                colors = listOf(
                                    colorVariant.segmentColor,
                                    colorVariant.segmentColor.copy(alpha = 0.15f)
                                )
                            )
                        } else {
                            SolidColor(colorVariant.segmentColor)
                        }
                    )
            )
        }

        // 3. Class Card
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 12.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(
                    if (isDark) colorVariant.cardBgDark else colorVariant.cardBgLight
                )
                .border(
                    width = 1.dp,
                    color = if (isDark) colorVariant.borderDark else colorVariant.borderLight,
                    shape = RoundedCornerShape(18.dp)
                )
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                // Left Accent Color Bar
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(130.dp)
                        .clip(RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp))
                        .background(
                            brush = Brush.verticalGradient(colorVariant.gradientColors)
                        )
                )

                // Card Main Content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Subject Name
                    Text(
                        text = classModel.courseName,
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFFE8F0FE) else Color(0xFF1A2540),
                        lineHeight = 22.sp
                    )

                    // Room Subtitle
                    Text(
                        text = if (classModel.room.startsWith("Room", ignoreCase = true)) classModel.room else "Room ${classModel.room}",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 13.5.sp,
                        color = if (isDark) Color(0xFF6B7F9E) else Color(0xFF8898B0)
                    )

                    // Tags Row (Room & Duration)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        // Room Tag
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isDark) Color(0x1AFFFFFF) else Color(0x0D000000))
                                .border(1.dp, if (isDark) Color(0x12FFFFFF) else Color(0x0A000000), RoundedCornerShape(20.dp))
                                .padding(horizontal = 9.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = null,
                                tint = if (isDark) Color(0xFF6B7F9E) else Color(0xFF6B7F9E),
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = extractCleanRoomNumber(classModel.room),
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) Color(0xFF8898B0) else Color(0xFF6B7F9E)
                            )
                        }

                        // Duration Tag
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isDark) Color(0x1AFFFFFF) else Color(0x0D000000))
                                .border(1.dp, if (isDark) Color(0x12FFFFFF) else Color(0x0A000000), RoundedCornerShape(20.dp))
                                .padding(horizontal = 9.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = if (isDark) Color(0xFF6B7F9E) else Color(0xFF6B7F9E),
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = parseClassDuration(classModel.timeSlot),
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) Color(0xFF8898B0) else Color(0xFF6B7F9E)
                            )
                        }
                    }

                    // Attendance Buttons (Attended / Missed)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    ) {
                        val isAttended = classModel.attendanceState == "attended"
                        val isMissed = classModel.attendanceState == "missed"

                        // Attended Button
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .background(
                                    if (isAttended) Color(0xFF22C55E) else if (isDark) Color(0x1F22C55E) else Color(0x1422C55E)
                                )
                                .border(
                                    width = 1.5.dp,
                                    color = if (isAttended) Color(0xFF22C55E) else Color(0x5922C55E),
                                    shape = RoundedCornerShape(11.dp)
                                )
                                .clickable { onAttendanceChanged("attended") },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Attended",
                                    tint = if (isAttended) Color.White else Color(0xFF22C55E),
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "Attended",
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isAttended) Color.White else Color(0xFF22C55E)
                                )
                            }
                        }

                        // Missed Button
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .background(
                                    if (isMissed) Color(0xFFEF4444) else if (isDark) Color(0x1FEF4444) else Color(0x12EF4444)
                                )
                                .border(
                                    width = 1.5.dp,
                                    color = if (isMissed) Color(0xFFEF4444) else Color(0x4DEF4444),
                                    shape = RoundedCornerShape(11.dp)
                                )
                                .clickable { onAttendanceChanged("missed") },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Missed",
                                    tint = if (isMissed) Color.White else Color(0xFFEF4444),
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "Missed",
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isMissed) Color.White else Color(0xFFEF4444)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════
// HELPER DATA & FUNCTIONS
// ══════════════════════════════════════════════════════════════
private data class UpNextInfo(
    val classModel: ClassModel,
    val statusLabel: String,
    val statusPill: String,
    val isHappeningNow: Boolean
)

private fun getDynamicGreetingTitle(cal: Calendar): String {
    val hour = cal.get(Calendar.HOUR_OF_DAY)
    return when {
        hour in 5..11 -> "Good morning"
        hour in 12..16 -> "Good afternoon"
        hour in 17..20 -> "Good evening"
        else -> "Good night"
    }
}

private fun getDynamicGreetingSubtitle(cal: Calendar, uiState: StudentPortalUiState): String {
    val dateFormatted = SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(cal.time)
    val todayIdx = getTodayDayIndex()
    val todayClasses = (uiState.classMap[todayIdx] ?: emptyList()).sortedBy { startMinutes(it.timeSlot) }

    if (todayClasses.isEmpty()) {
        return "$dateFormatted · No classes scheduled"
    }

    val currentMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)

    var hasClassInProgress = false
    var remainingClassesCount = 0

    for (item in todayClasses) {
        val startMins = startMinutes(item.timeSlot)
        val endMins = endMinutes(item.timeSlot)

        if (currentMinutes in startMins..endMins) {
            hasClassInProgress = true
        } else if (currentMinutes < startMins) {
            remainingClassesCount++
        }
    }

    return when {
        hasClassInProgress -> "$dateFormatted · Class in progress"
        remainingClassesCount > 0 -> {
            val label = if (remainingClassesCount == 1) "1 class remaining" else "$remainingClassesCount classes remaining"
            "$dateFormatted · $label"
        }
        else -> {
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            if (hour >= 20) {
                val tomorrowIdx = (todayIdx + 1) % 7
                val tomorrowClasses = (uiState.classMap[tomorrowIdx] ?: emptyList()).sortedBy { startMinutes(it.timeSlot) }
                val firstClassTime = tomorrowClasses.firstOrNull()?.timeSlot?.split("-", "–")?.firstOrNull()?.trim()
                if (!firstClassTime.isNullOrBlank()) {
                    "$dateFormatted · Tomorrow's first class at $firstClassTime"
                } else {
                    "$dateFormatted · All classes completed for today"
                }
            } else {
                "$dateFormatted · All classes completed for today"
            }
        }
    }
}

private fun getUpNextClassInfo(uiState: StudentPortalUiState, currentTimeMillis: Long = System.currentTimeMillis()): UpNextInfo? {
    val todayIdx = getTodayDayIndex()
    val todayClasses = (uiState.classMap[todayIdx] ?: emptyList()).sortedBy { startMinutes(it.timeSlot) }
    if (todayClasses.isEmpty()) return null

    val cal = Calendar.getInstance().apply { timeInMillis = currentTimeMillis }
    val currentMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)

    for (item in todayClasses) {
        val startMins = startMinutes(item.timeSlot)
        val endMins = endMinutes(item.timeSlot)

        if (currentMinutes in startMins..endMins) {
            val remainMins = endMins - currentMinutes
            val pillText = if (remainMins >= 60) "${remainMins / 60}h ${remainMins % 60}m left" else "${remainMins}m left"
            return UpNextInfo(
                classModel = item,
                statusLabel = "HAPPENING NOW",
                statusPill = pillText,
                isHappeningNow = true
            )
        } else if (currentMinutes < startMins) {
            val diffMins = startMins - currentMinutes
            val pillText = if (diffMins >= 60) "Starts in ${diffMins / 60}h ${diffMins % 60}m" else "Starts in ${diffMins}m"
            return UpNextInfo(
                classModel = item,
                statusLabel = "UP NEXT",
                statusPill = pillText,
                isHappeningNow = false
            )
        }
    }

    return null
}

private fun endMinutes(timeSlot: String): Int {
    val parts = timeSlot.split("-", "–")
    if (parts.size < 2) return startMinutes(timeSlot) + 90
    val parsed = startMinutes(parts[1].trim())
    return if (parsed > 0) parsed else startMinutes(timeSlot) + 90
}

private fun extractTimeParts(timeSlot: String): Pair<String, String> {
    val startTime = timeSlot.split("-", "–").firstOrNull()?.trim() ?: timeSlot
    val parts = startTime.split("\\s+".toRegex())
    val hourPart = parts.firstOrNull() ?: startTime
    val amPmPart = if (parts.size > 1) parts[1].uppercase(Locale.ROOT) else "AM"
    return Pair(hourPart, amPmPart)
}

private fun extractCleanRoomNumber(room: String): String {
    val clean = room.replace("Room", "", ignoreCase = true).replace("ROOM", "", ignoreCase = true).trim()
    return clean.ifEmpty { "TBA" }
}

private enum class CardColorVariant(
    val dotColor: Color,
    val segmentColor: Color,
    val gradientColors: List<Color>,
    val cardBgDark: Color,
    val cardBgLight: Color,
    val borderDark: Color,
    val borderLight: Color
) {
    GREEN(
        dotColor = Color(0xFF22C55E),
        segmentColor = Color(0x3322C55E),
        gradientColors = listOf(Color(0xFF22C55E), Color(0xFF10B981)),
        cardBgDark = Color(0x1222C55E),
        cardBgLight = Color(0x1222C55E),
        borderDark = Color(0x2622C55E),
        borderLight = Color(0x2622C55E)
    ),
    BLUE(
        dotColor = Color(0xFF3B82F6),
        segmentColor = Color(0x333B82F6),
        gradientColors = listOf(Color(0xFF3B82F6), Color(0xFF6366F1)),
        cardBgDark = Color(0x123B82F6),
        cardBgLight = Color(0x123B82F6),
        borderDark = Color(0x263B82F6),
        borderLight = Color(0x263B82F6)
    ),
    AMBER(
        dotColor = Color(0xFFF59E0B),
        segmentColor = Color(0x33F59E0B),
        gradientColors = listOf(Color(0xFFF59E0B), Color(0xFFEA580C)),
        cardBgDark = Color(0x12F59E0B),
        cardBgLight = Color(0x12F59E0B),
        borderDark = Color(0x26F59E0B),
        borderLight = Color(0x26F59E0B)
    ),
    RED(
        dotColor = Color(0xFFEF4444),
        segmentColor = Color(0x33EF4444),
        gradientColors = listOf(Color(0xFFEF4444), Color(0xFFDC2626)),
        cardBgDark = Color(0x12EF4444),
        cardBgLight = Color(0x12EF4444),
        borderDark = Color(0x26EF4444),
        borderLight = Color(0x26EF4444)
    );

    companion object {
        fun forIndex(index: Int): CardColorVariant {
            val values = values()
            return values[index % values.size]
        }
    }
}

private fun getDayFullName(shortName: String): String {
    return when (shortName) {
        "SAT" -> "Saturday"
        "SUN" -> "Sunday"
        "MON" -> "Monday"
        "TUE" -> "Tuesday"
        "WED" -> "Wednesday"
        "THU" -> "Thursday"
        "FRI" -> "Friday"
        else -> shortName
    }
}

@Composable
private fun AddClassBottomSheetContent(
    selectedDayName: String,
    initialManualMode: Boolean = true,
    onAddClass: (courseName: String, timeSlot: String, room: String) -> Unit,
    onScanCamera: () -> Unit,
    onChooseGallery: () -> Unit,
    onDeleteRoutine: () -> Unit,
    onShowToast: (String) -> Unit
) {
    var isManualMode by remember(initialManualMode) { mutableStateOf(initialManualMode) }
    var courseName by remember { mutableStateOf("") }
    var faculty by remember { mutableStateOf("") }
    var timeSlot by remember { mutableStateOf("") }
    var room by remember { mutableStateOf("") }
    val sheetScrollState = rememberScrollState()
    val isDark = LocalIsDarkTheme.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .verticalScroll(sheetScrollState)
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        AnimatedContent(
            targetState = isManualMode,
            transitionSpec = {
                fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180))
            },
            label = "SheetModeSwitch"
        ) { manualMode ->
            if (manualMode) {
                // Manual Entry Mode - Professional, Glassy & Emoji-Free Form
                var courseError by remember { mutableStateOf(false) }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Glassy Header Row
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
                                    .background(if (isDark) Color(0x260066FF) else Color(0x1A0066FF))
                                    .border(1.dp, if (isDark) Color(0x403B82F6) else Color(0x330066FF), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = if (isDark) Color(0xFF60A5FA) else Color(0xFF0066FF),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = "Add Class Manually",
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Schedule a new class for $selectedDayName",
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Glassy Day Pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isDark) Color(0x1FFFFFFF) else Color(0xFFF1F5FB))
                                .border(1.dp, if (isDark) Color(0x1AFFFFFF) else Color(0x0F000000), RoundedCornerShape(20.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = selectedDayName,
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFF60A5FA) else Color(0xFF0066FF)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // Course Name Input & Faculty Input Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        OutlinedTextField(
                            value = courseName,
                            onValueChange = {
                                courseName = it
                                if (it.isNotBlank()) courseError = false
                            },
                            singleLine = true,
                            isError = courseError,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            label = { Text("Course Code") },
                            placeholder = { Text("e.g., CSE 210") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = if (isDark) Color(0xFF60A5FA) else Color(0xFF0066FF),
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            modifier = Modifier.weight(1.3f),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF0066FF),
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                errorBorderColor = Color(0xFFDC2626),
                                focusedContainerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC),
                                unfocusedContainerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC)
                            )
                        )

                        OutlinedTextField(
                            value = faculty,
                            onValueChange = { faculty = it },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            label = { Text("Faculty") },
                            placeholder = { Text("e.g., MDSS") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = if (isDark) Color(0xFF60A5FA) else Color(0xFF0066FF),
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            modifier = Modifier.weight(0.9f),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF0066FF),
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                focusedContainerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC),
                                unfocusedContainerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC)
                            )
                        )
                    }

                    if (courseError) {
                        Text(
                            text = "Please enter the course name",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 12.sp,
                            color = Color(0xFFDC2626),
                            modifier = Modifier.padding(start = 4.dp, top = 0.dp)
                        )
                    }

                    // Time & Room Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = timeSlot,
                            onValueChange = { timeSlot = it },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            label = { Text("Time Slot") },
                            placeholder = { Text("08:15 AM - 09:45 AM") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = null,
                                    tint = if (isDark) Color(0xFF60A5FA) else Color(0xFF0066FF),
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF0066FF),
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                focusedContainerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC),
                                unfocusedContainerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC)
                            )
                        )

                        OutlinedTextField(
                            value = room,
                            onValueChange = { room = it },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            label = { Text("Room No") },
                            placeholder = { Text("Room 602") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Home,
                                    contentDescription = null,
                                    tint = if (isDark) Color(0xFF60A5FA) else Color(0xFF0066FF),
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF0066FF),
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                focusedContainerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC),
                                unfocusedContainerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Primary Save Button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .shadow(
                                elevation = 6.dp,
                                shape = RoundedCornerShape(14.dp),
                                ambientColor = Color(0x400066FF),
                                spotColor = Color(0x400066FF)
                            )
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(Color(0xFF0066FF), Color(0xFF0052CC))
                                )
                            )
                            .clickable {
                                if (courseName.isBlank()) {
                                    courseError = true
                                } else {
                                    courseError = false
                                    val fullDisplay = if (faculty.isNotBlank()) {
                                        val trimmedFac = faculty.trim()
                                        if (trimmedFac.startsWith("(") && trimmedFac.endsWith(")")) {
                                            "${courseName.trim()} $trimmedFac"
                                        } else {
                                            "${courseName.trim()} ($trimmedFac)"
                                        }
                                    } else {
                                        courseName.trim()
                                    }
                                    onAddClass(fullDisplay, timeSlot.ifBlank { "08:15 AM - 09:45 AM" }, room.ifBlank { "Room TBA" })
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Save Class to Schedule",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // Divider & Glassy Gallery Button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(1.dp)
                                .background(if (isDark) Color(0x1AFFFFFF) else Color(0x1A000000))
                        )
                        Text(
                            text = "OR",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(1.dp)
                                .background(if (isDark) Color(0x1AFFFFFF) else Color(0x1A000000))
                        )
                    }

                    // Glassy Secondary Button: Choose from gallery
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isDark) Color(0x1AFFFFFF) else Color(0xFFF1F5FB))
                            .border(
                                1.dp,
                                if (isDark) Color(0x1AFFFFFF) else Color(0xFFE2E8F0),
                                RoundedCornerShape(14.dp)
                            )
                            .clickable { onChooseGallery() },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = null,
                                tint = if (isDark) Color(0xFF60A5FA) else Color(0xFF0066FF),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Choose from gallery",
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFF60A5FA) else Color(0xFF0066FF)
                            )
                        }
                    }
                }
            } else {
            // Scan Mode
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(Color(0xFF0066FF), Color(0xFF0052CC))
                            )
                        )
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Photo in, routine out",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Point at your printed routine or pick a screenshot. Reads on your phone. Nothing is uploaded.",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.95f),
                        lineHeight = 20.sp
                    )
                }

                Button(
                    onClick = onScanCamera,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0066FF),
                        contentColor = Color.White
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Scan with camera",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                OutlinedButton(
                    onClick = onChooseGallery,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = Color(0xFF0066FF)
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null,
                            tint = Color(0xFF0066FF),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Choose from gallery",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0066FF)
                        )
                    }
                }

                Text(
                    text = "Scanning tips",
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Tips grid
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TipChip("Good lighting", Icons.Default.WbSunny, Modifier.weight(1f))
                        TipChip("Flat surface", Icons.Default.Crop, Modifier.weight(1f))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TipChip("All in frame", Icons.Default.RemoveRedEye, Modifier.weight(1f))
                        TipChip("Hold steady", Icons.Default.Check, Modifier.weight(1f))
                    }
                }

                OutlinedButton(
                    onClick = { isManualMode = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = Color(0xFF0066FF)
                    )
                ) {
                    Text(
                        text = "Enter manually instead",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0066FF)
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun TipChip(
    text: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF3B82F6),
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = text,
            fontFamily = PlusJakartaSansFontFamily,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// ══════════════════════════════════════════════════════════════
// EMPTY STATE COMPONENTS (Onboarding & Empty Day)
// ══════════════════════════════════════════════════════════════

@Composable
fun RoutineOnboardingEmptyCard(
    isDark: Boolean,
    onScanRoutine: () -> Unit,
    onAddManually: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isDark) 0.dp else 4.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = Color(0x14000000),
                spotColor = Color(0x14000000)
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF111827) else Color.White
        ),
        border = BorderStroke(1.dp, if (isDark) Color(0x1AFFFFFF) else Color(0x0F000000))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // Soft icon container with brand blue glow border
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            brush = if (isDark) {
                                Brush.linearGradient(
                                    colors = listOf(Color(0x330066FF), Color(0x1F3B82F6))
                                )
                            } else {
                                Brush.linearGradient(
                                    colors = listOf(Color(0x1A0066FF), Color(0x103B82F6))
                                )
                            }
                        )
                        .border(
                            1.dp,
                            if (isDark) Color(0x403B82F6) else Color(0x330066FF),
                            RoundedCornerShape(24.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = if (isDark) Color(0xFF60A5FA) else Color(0xFF0066FF),
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Title
                Text(
                    text = "Your routine starts here",
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isDark) Color(0xFFE8F0FE) else Color(0xFF1A2540),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Subtitle
                Text(
                    text = "Scan a photo of your class routine and the app fills it in — or add classes yourself.",
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Normal,
                    color = if (isDark) Color(0xFF6B7F9E) else Color(0xFF8898B0),
                    textAlign = TextAlign.Center,
                    lineHeight = 21.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Primary CTA Button: Scan my routine (Brand Blue Gradient)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .shadow(
                            elevation = 6.dp,
                            shape = RoundedCornerShape(16.dp),
                            ambientColor = Color(0x550066FF),
                            spotColor = Color(0x550066FF)
                        )
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(Color(0xFF0066FF), Color(0xFF0052CC))
                            )
                        )
                        .clickable { onScanRoutine() },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Crop,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Scan my routine",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Secondary Full-Width Button: Add manually
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isDark) Color(0x1AFFFFFF) else Color(0xFFF1F5FB))
                        .border(
                            1.dp,
                            if (isDark) Color(0x1AFFFFFF) else Color(0xFFE2E8F0),
                            RoundedCornerShape(14.dp)
                        )
                        .clickable { onAddManually() },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = if (isDark) Color(0xFF60A5FA) else Color(0xFF0066FF),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Add manually",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFF60A5FA) else Color(0xFF0066FF)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RoutineDayEmptyCard(
    isDark: Boolean,
    dayName: String,
    shortDay: String,
    onAddClass: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isDark) 0.dp else 2.dp,
                shape = RoundedCornerShape(22.dp),
                ambientColor = Color(0x0F000000),
                spotColor = Color(0x0F000000)
            ),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF111827) else Color.White
        ),
        border = BorderStroke(1.dp, if (isDark) Color(0x1AFFFFFF) else Color(0x0F000000))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isDark) Color(0x260066FF) else Color(0x1A0066FF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = if (isDark) Color(0xFF60A5FA) else Color(0xFF0066FF),
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "No classes on $dayName",
                fontFamily = PlusJakartaSansFontFamily,
                fontSize = 19.5.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color(0xFFE8F0FE) else Color(0xFF1A2540),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Select another day or add a class to your schedule.",
                fontFamily = PlusJakartaSansFontFamily,
                fontSize = 14.5.sp,
                color = if (isDark) Color(0xFF6B7F9E) else Color(0xFF8898B0),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(22.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .shadow(
                        elevation = 4.dp,
                        shape = RoundedCornerShape(14.dp),
                        ambientColor = Color(0x400066FF),
                        spotColor = Color(0x400066FF)
                    )
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFF0066FF), Color(0xFF0052CC))
                        )
                    )
                    .clickable { onAddClass() },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Add a class on ${shortDay.take(3)}",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RoutineTabPreview() {
    StudentPortalTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF70D6FF),
                            Color(0xFFC2EFFF),
                            Color(0xFFFFFFFF)
                        )
                    )
                )
        ) {
            RoutineTab(
                uiState = StudentPortalUiState(selectedTab = 0, selectedDayIndex = 3),
                onDaySelected = {},
                onOpenAddClass = {},
                onCloseAddClass = {},
                onAddClass = { _, _, _ -> },
                onShowToast = {}
            )
        }
    }
}
