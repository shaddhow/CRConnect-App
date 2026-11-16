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
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.launch

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
    onToggleAttendance: (classId: String, targetState: String, dayIndex: Int) -> Unit = { _, _, _ -> },
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

    val greetingTheme = remember(cal, uiState.profile.name) {
        getDynamicGreetingTheme(cal, uiState.profile.name)
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

    val screenBg = Color.Transparent

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(screenBg)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. GREETING ROW + ADD CLASS BUTTON
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Glassy Dynamic Time-of-Day Icon Badge
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            brush = Brush.linearGradient(
                                colors = if (isDark) {
                                    listOf(greetingTheme.bgDarkStart, greetingTheme.bgDarkEnd)
                                } else {
                                    listOf(greetingTheme.bgLightStart, greetingTheme.bgLightEnd)
                                }
                            )
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.linearGradient(
                                colors = if (isDark) {
                                    listOf(greetingTheme.borderDarkStart, greetingTheme.borderDarkEnd)
                                } else {
                                    listOf(greetingTheme.borderLightStart, greetingTheme.borderLightEnd)
                                }
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = greetingTheme.icon,
                        contentDescription = null,
                        tint = if (isDark) greetingTheme.iconTintDark else greetingTheme.iconTintLight,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Title & Subtitle (Clean Glassy Aesthetic, No cartoon emojis)
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = greetingTheme.title,
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A),
                        letterSpacing = (-0.2).sp
                    )
                    Text(
                        text = greetingSubtitle,
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Modern Quick Add (+) Glass Button
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .shadow(
                        elevation = 6.dp,
                        shape = CircleShape,
                        ambientColor = if (isDark) Color(0x663B82F6) else Color(0x402563EB),
                        spotColor = if (isDark) Color(0x663B82F6) else Color(0x402563EB)
                    )
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFF2563EB), Color(0xFF1D4ED8))
                        )
                    )
                    .border(
                        width = 1.dp,
                        color = Color(0x4DFFFFFF),
                        shape = CircleShape
                    )
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
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // 3. "UP NEXT" HERO CARD
        upNextInfo?.let { info ->
            val hasTwoPages = info.isTodayCompleted && info.nextClassModel != null

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

                    if (hasTwoPages) {
                        val pagerState = rememberPagerState(pageCount = { 2 })
                        val coroutineScope = rememberCoroutineScope()

                        LaunchedEffect(pagerState, uiState.selectedDayIndex, uiState.classMap) {
                            delay(3500L)
                            if (pagerState.currentPage == 0) {
                                pagerState.animateScrollToPage(1)
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier.fillMaxWidth()
                            ) { page ->
                                if (page == 0) {
                                    // PAGE 0: TODAY'S SCHEDULE COMPLETED
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(185.dp)
                                            .padding(horizontal = 24.dp, vertical = 24.dp),
                                        verticalArrangement = Arrangement.SpaceBetween
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
                                                    text = info.displayTitle ?: "All classes completed for today!",
                                                    fontFamily = PlusJakartaSansFontFamily,
                                                    fontSize = 20.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color.White,
                                                    lineHeight = 26.sp
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = info.displayTime ?: "${info.completedCount} classes done",
                                                    fontFamily = PlusJakartaSansFontFamily,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = Color.White.copy(alpha = 0.8f)
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(12.dp))

                                            // Status Badge: STATUS / DONE
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(18.dp))
                                                    .background(Color.White)
                                                    .padding(horizontal = 16.dp, vertical = 9.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text(
                                                        text = info.roomBadgeTitle,
                                                        fontFamily = PlusJakartaSansFontFamily,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF0052CC),
                                                        letterSpacing = 1.sp
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = info.roomBadgeValue,
                                                        fontFamily = PlusJakartaSansFontFamily,
                                                        fontSize = 22.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = Color(0xFF1A2540)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    // PAGE 1: UP NEXT CLASS DETAILS
                                    val targetClass = info.nextClassModel
                                    val cardStatusLabel = info.nextStatusLabel
                                    val cardStatusPill = info.nextStatusPill

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(185.dp)
                                            .padding(horizontal = 24.dp, vertical = 24.dp),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        // Card Top Row
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = cardStatusLabel,
                                                fontFamily = PlusJakartaSansFontFamily,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color.White.copy(alpha = 0.8f),
                                                letterSpacing = 1.2.sp
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
                                                    text = cardStatusPill,
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
                                                    text = targetClass?.courseName ?: "",
                                                    fontFamily = PlusJakartaSansFontFamily,
                                                    fontSize = 28.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color.White,
                                                    lineHeight = 32.sp
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = targetClass?.timeSlot ?: "",
                                                    fontFamily = PlusJakartaSansFontFamily,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = Color.White.copy(alpha = 0.8f)
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(12.dp))

                                            // Room Badge
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
                                                        text = if (targetClass != null) extractCleanRoomNumber(targetClass.room) else "TBA",
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

                            // PAGE INDICATOR DOTS
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                repeat(2) { pageIndex ->
                                    val isSelected = pagerState.currentPage == pageIndex
                                    Box(
                                        modifier = Modifier
                                            .size(if (isSelected) 8.dp else 7.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isSelected) Color.White else Color.White.copy(alpha = 0.35f)
                                            )
                                            .clickable {
                                                coroutineScope.launch {
                                                    pagerState.animateScrollToPage(pageIndex)
                                                }
                                            }
                                    )
                                }
                            }
                        }
                    } else {
                        // SINGLE PAGE HERO CARD (LIVE class / Upcoming class / Viewing another day)
                        val targetClass = info.classModel
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

                                // Glowing green dot (only for LIVE class)
                                if (info.isHappeningNow) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .shadow(4.dp, CircleShape, ambientColor = Color(0xFF22C55E), spotColor = Color(0xFF22C55E))
                                            .clip(CircleShape)
                                            .background(Color(0xFF22C55E))
                                    )
                                }

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
                                        text = targetClass?.courseName ?: info.displayTitle ?: "",
                                        fontFamily = PlusJakartaSansFontFamily,
                                        fontSize = if (targetClass != null) 28.sp else 20.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White,
                                        lineHeight = if (targetClass != null) 32.sp else 26.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = targetClass?.timeSlot ?: info.displayTime ?: "",
                                        fontFamily = PlusJakartaSansFontFamily,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                // Room / Status Badge
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(Color.White)
                                        .padding(horizontal = 16.dp, vertical = 9.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = if (targetClass != null) "ROOM" else info.roomBadgeTitle,
                                            fontFamily = PlusJakartaSansFontFamily,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0052CC),
                                            letterSpacing = 1.sp
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = if (targetClass != null) extractCleanRoomNumber(targetClass.room) else info.roomBadgeValue,
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
                        dayIndex = uiState.selectedDayIndex,
                        colorVariant = colorVariant,
                        isLastItem = isLastItem,
                        currentTimeMillis = currentTimeMillis,
                        onAttendanceChanged = { target ->
                            onToggleAttendance(classModel.id, target, uiState.selectedDayIndex)
                        }
                    )
                }
            }
        } else if (isOverallRoutineEmpty) {
            // EMPTY STATE 2 — No routine at all (Onboarding)
            RoutineOnboardingEmptyCard(
                isDark = isDark,
                onScanCamera = onScanCameraClick,
                onChooseGallery = onChooseGalleryClick,
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
                scheduleDays = uiState.scheduleDays,
                selectedDayIndex = uiState.selectedDayIndex,
                onSelectDay = { dayIdx -> onDaySelected(dayIdx) },
                existingClasses = uiState.classMap.values.flatten(),
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
    dayIndex: Int,
    colorVariant: CardColorVariant,
    isLastItem: Boolean,
    currentTimeMillis: Long = 0L,
    onAttendanceChanged: (String) -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    val timeRange = parseClassTimeRange(classModel.timeSlot)
    val timingStatus = getClassTimingStatus(dayIndex, classModel.timeSlot)
    val effectiveState = getEffectiveAttendanceState(dayIndex, classModel)
    val liveProgress = getClassLiveProgress(dayIndex, classModel.timeSlot)
    val animatedProgress by animateFloatAsState(
        targetValue = liveProgress,
        animationSpec = tween(600),
        label = "live_progress"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "live_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Max),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Time Column (Start Time + Ends Label + End Time)
        Column(
            modifier = Modifier
                .width(54.dp)
                .fillMaxHeight()
                .padding(top = 14.dp, bottom = 18.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Start Time
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(
                    text = timeRange.startHour,
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color(0xFFE8F0FE) else Color(0xFF1A2540),
                    lineHeight = 16.sp
                )
                Text(
                    text = timeRange.startAmPm,
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDark) Color(0xFF6B7F9E) else Color(0xFF8898B0)
                )
            }

            // End Time
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(
                    text = "ends",
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isDark) Color(0xFF4A5D78) else Color(0xFFA0AEC0)
                )
                Text(
                    text = timeRange.endHour,
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDark) Color(0xFF8898B0) else Color(0xFF64748B),
                    lineHeight = 14.sp
                )
                Text(
                    text = timeRange.endAmPm,
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isDark) Color(0xFF4A5D78) else Color(0xFFA0AEC0)
                )
            }
        }

        // 2. Timeline Line & Dots
        Column(
            modifier = Modifier
                .padding(top = 16.dp, bottom = 16.dp)
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

            // Dynamic Segment Line
            Box(
                modifier = Modifier
                    .padding(vertical = 3.dp)
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

            // End Dot (aligned with End Time)
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0))
                    .border(
                        1.dp,
                        colorVariant.dotColor.copy(alpha = 0.5f),
                        CircleShape
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max)
            ) {
                // Left Dynamic Accent / Live Progress Bar
                Box(
                    modifier = Modifier
                        .width(5.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp))
                        .background(
                            if (timingStatus == ClassTimingStatus.LIVE_NOW) {
                                colorVariant.gradientColors.first().copy(alpha = if (isDark) 0.22f else 0.15f)
                            } else if (timingStatus == ClassTimingStatus.PAST) {
                                colorVariant.gradientColors.first().copy(alpha = 0.35f)
                            } else {
                                Color.Transparent
                            }
                        )
                ) {
                    if (timingStatus == ClassTimingStatus.LIVE_NOW) {
                        val currentProgress = animatedProgress.coerceIn(0.06f, 1.0f)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(currentProgress)
                                .clip(RoundedCornerShape(topStart = 18.dp))
                                .background(
                                    brush = Brush.verticalGradient(
                                        listOf(
                                            Color(0xFF22C55E),
                                            Color(0xFF10B981),
                                            Color(0xFF06B6D4)
                                        )
                                    )
                                )
                        ) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .background(Color.White.copy(alpha = pulseAlpha))
                            )
                        }
                    } else if (timingStatus == ClassTimingStatus.PAST) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = Brush.verticalGradient(
                                        listOf(
                                            colorVariant.gradientColors.first().copy(alpha = 0.5f),
                                            colorVariant.gradientColors.last().copy(alpha = 0.3f)
                                        )
                                    )
                                )
                        )
                    } else {
                        // Future: classic vibrant accent stripe
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = Brush.verticalGradient(colorVariant.gradientColors)
                                )
                        )
                    }
                }

                // Card Main Content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Live Happening Now Banner
                    if (timingStatus == ClassTimingStatus.LIVE_NOW) {
                        val remainingMins = getClassRemainingMinutes(classModel.timeSlot)
                        val pct = (animatedProgress * 100).toInt().coerceIn(1, 100)
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isDark) Color(0x2622C55E) else Color(0x1A22C55E))
                                .border(1.dp, Color(0x5922C55E), RoundedCornerShape(20.dp))
                                .padding(horizontal = 9.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF22C55E))
                            )
                            Text(
                                text = "HAPPENING NOW · ${remainingMins}m left ($pct%)",
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFF4ADE80) else Color(0xFF15803D)
                            )
                        }
                    }

                    // Course Name & Faculty Badge
                    val courseName = classModel.courseName
                    val hasFaculty = courseName.contains("(") && courseName.contains(")")
                    val codePart = if (hasFaculty) courseName.substringBefore("(").trim() else courseName
                    val facultyPart = if (hasFaculty) courseName.substringAfter("(").substringBefore(")").trim() else ""

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = codePart,
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 17.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFFE8F0FE) else Color(0xFF1A2540),
                            lineHeight = 22.sp
                        )
                        if (facultyPart.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isDark) Color(0x2638BDF8) else Color(0x1F0284C7))
                                    .border(1.dp, if (isDark) Color(0x3338BDF8) else Color(0x260284C7), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 1.5.dp)
                            ) {
                                Text(
                                    text = facultyPart,
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFF38BDF8) else Color(0xFF0369A1)
                                )
                            }
                        }
                    }

                    // Room Subtitle
                    Text(
                        text = if (classModel.room.startsWith("Room", ignoreCase = true)) classModel.room else "Room ${classModel.room}",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 13.sp,
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
                                tint = if (isDark) Color(0xFF8898B0) else Color(0xFF6B7F9E),
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = extractCleanRoomNumber(classModel.room),
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) Color(0xFF8898B0) else Color(0xFF6B7F9E)
                            )
                        }

                        // Duration & End Time Tag
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
                                tint = if (isDark) Color(0xFF8898B0) else Color(0xFF6B7F9E),
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = "Ends ${timeRange.endHour} ${timeRange.endAmPm} · ${parseClassDuration(classModel.timeSlot)}",
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) Color(0xFF8898B0) else Color(0xFF6B7F9E)
                            )
                        }
                    }

                    // Attendance Section (Locked / Live aware)
                    when (timingStatus) {
                        ClassTimingStatus.LIVE_NOW -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
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

                        ClassTimingStatus.PAST -> {
                            val isAttended = effectiveState == "attended"
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 5.dp)
                                    .heightIn(min = 34.dp)
                                    .clip(RoundedCornerShape(11.dp))
                                    .background(
                                        if (isAttended) (if (isDark) Color(0x1F22C55E) else Color(0x1422C55E))
                                        else (if (isDark) Color(0x1FEF4444) else Color(0x12EF4444))
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isAttended) Color(0x5922C55E) else Color(0x4DEF4444),
                                        shape = RoundedCornerShape(11.dp)
                                    )
                                    .clickable { onAttendanceChanged(if (isAttended) "attended" else "missed") }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isAttended) Icons.Default.Check else Icons.Default.Close,
                                        contentDescription = null,
                                        tint = if (isAttended) Color(0xFF22C55E) else Color(0xFFEF4444),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = if (isAttended) "Attended" else "Auto-Missed",
                                        fontFamily = PlusJakartaSansFontFamily,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isAttended) Color(0xFF22C55E) else Color(0xFFEF4444)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Locked",
                                        tint = (if (isAttended) Color(0xFF22C55E) else Color(0xFFEF4444)).copy(alpha = 0.7f),
                                        modifier = Modifier.size(11.dp)
                                    )
                                }
                            }
                        }

                        ClassTimingStatus.FUTURE -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 5.dp)
                                    .heightIn(min = 34.dp)
                                    .clip(RoundedCornerShape(11.dp))
                                    .background(
                                        brush = Brush.linearGradient(
                                            colors = if (isDark) listOf(
                                                Color(0x181E293B),
                                                Color(0x0F0F172A)
                                            ) else listOf(
                                                Color(0x0C000000),
                                                Color(0x05000000)
                                            )
                                        )
                                    )
                                    .border(
                                        width = 1.dp,
                                        brush = Brush.linearGradient(
                                            colors = if (isDark) listOf(
                                                Color(0x3394A3B8),
                                                Color(0x1464748B)
                                            ) else listOf(
                                                Color(0x1F000000),
                                                Color(0x0D000000)
                                            )
                                        ),
                                        shape = RoundedCornerShape(11.dp)
                                    )
                                    .clickable { onAttendanceChanged("attended") }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    // Left: Frosted lock micro-badge + status
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(18.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isDark) Color(0x3364748B) else Color(0x1A475569)
                                                )
                                                .border(
                                                    width = 0.8.dp,
                                                    color = if (isDark) Color(0x4094A3B8) else Color(0x26475569),
                                                    shape = CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Lock,
                                                contentDescription = "Locked",
                                                tint = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                                                modifier = Modifier.size(9.5.dp)
                                            )
                                        }

                                        Text(
                                            text = "Attendance Locked",
                                            fontFamily = PlusJakartaSansFontFamily,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                        )
                                    }

                                    // Right: Glassy capsule indicating unlock time
                                    val unlockTimeText = if (timeRange.startHour.isNotBlank()) {
                                        "Opens ${timeRange.startHour} ${timeRange.startAmPm}".trim()
                                    } else {
                                        "Opens during class"
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(7.dp))
                                            .background(
                                                if (isDark) Color(0x2638BDF8) else Color(0x1A0284C7)
                                            )
                                            .border(
                                                width = 0.8.dp,
                                                color = if (isDark) Color(0x4038BDF8) else Color(0x330284C7),
                                                shape = RoundedCornerShape(7.dp)
                                            )
                                            .padding(horizontal = 7.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = unlockTimeText,
                                            fontFamily = PlusJakartaSansFontFamily,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
                                        )
                                    }
                                }
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
    val classModel: ClassModel? = null,
    val nextClassModel: ClassModel? = null,
    val statusLabel: String,
    val statusPill: String,
    val isHappeningNow: Boolean = false,
    val isTodayCompleted: Boolean = false,
    val completedCount: Int = 0,
    val displayTitle: String? = null,
    val displayTime: String? = null,
    val roomBadgeTitle: String = "ROOM",
    val roomBadgeValue: String = "TBA",
    val nextStatusLabel: String = "UP NEXT",
    val nextStatusPill: String = ""
)

private fun getStudentShortName(fullName: String): String {
    val clean = fullName.trim()
    if (clean.isBlank() || clean.equals("Student", ignoreCase = true) || clean.equals("--", ignoreCase = true)) {
        return ""
    }
    val tokens = clean.split(Regex("[\\s\\-_.]+")).filter { it.isNotBlank() }
    if (tokens.isEmpty()) return ""
    val candidate = if (tokens.last().length > 2) {
        tokens.last()
    } else {
        tokens.firstOrNull { it.length > 2 } ?: tokens.first()
    }
    return candidate.lowercase(Locale.ROOT).replaceFirstChar { it.uppercase(Locale.ROOT) }
}

private data class GreetingTheme(
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val bgDarkStart: Color,
    val bgDarkEnd: Color,
    val bgLightStart: Color,
    val bgLightEnd: Color,
    val borderDarkStart: Color,
    val borderDarkEnd: Color,
    val borderLightStart: Color,
    val borderLightEnd: Color,
    val iconTintDark: Color,
    val iconTintLight: Color
)

private fun getDynamicGreetingTheme(cal: Calendar, fullName: String = ""): GreetingTheme {
    val hour = cal.get(Calendar.HOUR_OF_DAY)
    val shortName = getStudentShortName(fullName)
    val nameSuffix = if (shortName.isNotBlank()) ", $shortName" else ""

    return when {
        // Morning (5:00 AM - 11:59 AM): Golden/Amber Glass
        hour in 5..11 -> GreetingTheme(
            title = "Good morning$nameSuffix",
            icon = Icons.Default.WbSunny,
            bgDarkStart = Color(0x33F59E0B),
            bgDarkEnd = Color(0x14D97706),
            bgLightStart = Color(0x26F59E0B),
            bgLightEnd = Color(0x12FEF3C7),
            borderDarkStart = Color(0x59FBBF24),
            borderDarkEnd = Color(0x1FF59E0B),
            borderLightStart = Color(0x4DF59E0B),
            borderLightEnd = Color(0x20FDE68A),
            iconTintDark = Color(0xFFFBBF24),
            iconTintLight = Color(0xFFD97706)
        )
        // Afternoon (12:00 PM - 4:59 PM): Sky Blue / Cyan Glass
        hour in 12..16 -> GreetingTheme(
            title = "Good afternoon$nameSuffix",
            icon = Icons.Default.LocalCafe,
            bgDarkStart = Color(0x330EA5E9),
            bgDarkEnd = Color(0x140284C7),
            bgLightStart = Color(0x260284C7),
            bgLightEnd = Color(0x12E0F2FE),
            borderDarkStart = Color(0x5938BDF8),
            borderDarkEnd = Color(0x1F0EA5E9),
            borderLightStart = Color(0x4D0284C7),
            borderLightEnd = Color(0x20BAE6FD),
            iconTintDark = Color(0xFF38BDF8),
            iconTintLight = Color(0xFF0284C7)
        )
        // Evening (5:00 PM - 8:59 PM): Sunset Rose / Purple Glass
        hour in 17..20 -> GreetingTheme(
            title = "Good evening$nameSuffix",
            icon = Icons.Default.WbTwilight,
            bgDarkStart = Color(0x33EC4899),
            bgDarkEnd = Color(0x148B5CF6),
            bgLightStart = Color(0x26EC4899),
            bgLightEnd = Color(0x12FDF2F8),
            borderDarkStart = Color(0x59F472B6),
            borderDarkEnd = Color(0x1FA855F7),
            borderLightStart = Color(0x4DEC4899),
            borderLightEnd = Color(0x20FBCFE8),
            iconTintDark = Color(0xFFF472B6),
            iconTintLight = Color(0xFFDB2777)
        )
        // Night (9:00 PM - 4:59 AM): Starry Indigo / Violet Glass
        else -> GreetingTheme(
            title = "Good night$nameSuffix",
            icon = Icons.Default.Bedtime,
            bgDarkStart = Color(0x336366F1),
            bgDarkEnd = Color(0x144338CA),
            bgLightStart = Color(0x264F46E5),
            bgLightEnd = Color(0x12EEF2FF),
            borderDarkStart = Color(0x59818CF8),
            borderDarkEnd = Color(0x1F6366F1),
            borderLightStart = Color(0x4D6366F1),
            borderLightEnd = Color(0x20C7D2FE),
            iconTintDark = Color(0xFFA5B4FC),
            iconTintLight = Color(0xFF4F46E5)
        )
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
    val firstClass = todayClasses.first()
    val firstStartMins = startMinutes(firstClass.timeSlot)

    // 1. Before first class of the day
    if (currentMinutes < firstStartMins) {
        val firstTimeStr = firstClass.timeSlot.split("-", "–").firstOrNull()?.trim() ?: ""
        val countText = if (todayClasses.size == 1) "1 class today" else "${todayClasses.size} classes today"
        return if (firstTimeStr.isNotBlank()) {
            "$dateFormatted · Starts at $firstTimeStr · $countText"
        } else {
            "$dateFormatted · $countText"
        }
    }

    // 2. Check if a class is happening right now
    val isClassNow = todayClasses.any { cls ->
        val s = startMinutes(cls.timeSlot)
        val e = endMinutes(cls.timeSlot)
        currentMinutes in s..e
    }
    if (isClassNow) {
        return "$dateFormatted · Class in progress"
    }

    // 3. During the day: how many classes remain?
    val remainingClassesCount = todayClasses.count { cls ->
        startMinutes(cls.timeSlot) > currentMinutes
    }
    if (remainingClassesCount > 0) {
        val label = if (remainingClassesCount == 1) "1 class remaining" else "$remainingClassesCount classes remaining"
        return "$dateFormatted · $label"
    }

    // 4. After all classes have ended
    return "$dateFormatted · All classes completed"
}

private fun getUpNextClassInfo(uiState: StudentPortalUiState, currentTimeMillis: Long = System.currentTimeMillis()): UpNextInfo? {
    if (uiState.classMap.isEmpty() || uiState.classMap.values.all { it.isEmpty() }) {
        return null
    }

    val todayIdx = getTodayDayIndex()
    val selectedIdx = uiState.selectedDayIndex
    val isTodaySelected = selectedIdx == todayIdx

    val cal = Calendar.getInstance().apply { timeInMillis = currentTimeMillis }
    val currentMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)

    if (isTodaySelected) {
        val todayClasses = (uiState.classMap[todayIdx] ?: emptyList()).sortedBy { startMinutes(it.timeSlot) }
        if (todayClasses.isEmpty()) {
            return UpNextInfo(
                classModel = null,
                statusLabel = "TODAY",
                statusPill = "Day Off",
                isHappeningNow = false,
                displayTitle = "No classes scheduled for today",
                displayTime = "Enjoy your break!",
                roomBadgeTitle = "STATUS",
                roomBadgeValue = "OFF"
            )
        }

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
                    isHappeningNow = true,
                    roomBadgeTitle = "ROOM",
                    roomBadgeValue = extractCleanRoomNumber(item.room)
                )
            } else if (currentMinutes < startMins) {
                val diffMins = startMins - currentMinutes
                val pillText = if (diffMins >= 60) "Starts in ${diffMins / 60}h ${diffMins % 60}m" else "Starts in ${diffMins}m"
                return UpNextInfo(
                    classModel = item,
                    statusLabel = "UP NEXT TODAY",
                    statusPill = pillText,
                    isHappeningNow = false,
                    roomBadgeTitle = "ROOM",
                    roomBadgeValue = extractCleanRoomNumber(item.room)
                )
            }
        }

        // All classes today completed!
        // Search forward up to 6 days for the next class
        var foundNextClass: ClassModel? = null
        var foundNextDayLabel = ""
        var foundNextPill = ""

        for (offset in 1..6) {
            val futureDayIdx = (todayIdx + offset) % 7
            val futureClasses = (uiState.classMap[futureDayIdx] ?: emptyList()).sortedBy { startMinutes(it.timeSlot) }
            if (futureClasses.isNotEmpty()) {
                val nextClass = futureClasses.first()
                foundNextClass = nextClass
                val futureDayObj = uiState.scheduleDays.find { it.dayIndex == futureDayIdx }
                val dayShort = futureDayObj?.shortName ?: ""
                
                val startMins = startMinutes(nextClass.timeSlot)
                val totalRemainingMinutes = (offset * 1440) + startMins - currentMinutes

                val remainingPillText = if (totalRemainingMinutes > 0) {
                    val hours = totalRemainingMinutes / 60
                    val mins = totalRemainingMinutes % 60
                    if (hours >= 24) {
                        val days = hours / 24
                        val remHours = hours % 24
                        if (remHours > 0) "Starts in ${days}d ${remHours}h" else "Starts in ${days}d"
                    } else if (hours > 0) {
                        if (mins > 0) "Starts in ${hours}h ${mins}m" else "Starts in ${hours}h"
                    } else {
                        "Starts in ${mins}m"
                    }
                } else {
                    "Starts soon"
                }

                if (offset == 1) {
                    foundNextDayLabel = "UP NEXT"
                    foundNextPill = remainingPillText
                } else {
                    foundNextDayLabel = "UP NEXT · ${dayShort.uppercase(Locale.ROOT)}"
                    foundNextPill = remainingPillText
                }
                break
            }
        }

        return UpNextInfo(
            classModel = null,
            nextClassModel = foundNextClass,
            statusLabel = "TODAY'S SCHEDULE",
            statusPill = "Completed",
            isHappeningNow = false,
            isTodayCompleted = true,
            completedCount = todayClasses.size,
            displayTitle = "All classes completed for today!",
            displayTime = "${todayClasses.size} ${if (todayClasses.size == 1) "class" else "classes"} done",
            roomBadgeTitle = "STATUS",
            roomBadgeValue = "DONE",
            nextStatusLabel = foundNextDayLabel.ifEmpty { "UP NEXT" },
            nextStatusPill = foundNextPill.ifEmpty { "Next class" }
        )
    } else {
        val selectedDayObj = uiState.scheduleDays.find { it.dayIndex == selectedIdx }
        val dayName = selectedDayObj?.shortName?.let { getDayFullName(it) } ?: "Selected Day"
        val selectedClasses = (uiState.classMap[selectedIdx] ?: emptyList()).sortedBy { startMinutes(it.timeSlot) }

        if (selectedClasses.isEmpty()) {
            return UpNextInfo(
                classModel = null,
                statusLabel = dayName.uppercase(Locale.ROOT),
                statusPill = "Day Off",
                isHappeningNow = false,
                displayTitle = "No classes scheduled for $dayName",
                displayTime = "Free day",
                roomBadgeTitle = "STATUS",
                roomBadgeValue = "OFF"
            )
        }

        val firstClass = selectedClasses.first()
        return UpNextInfo(
            classModel = firstClass,
            statusLabel = "SCHEDULED FOR ${dayName.uppercase(Locale.ROOT)}",
            statusPill = "${selectedClasses.size} ${if (selectedClasses.size == 1) "class" else "classes"}",
            isHappeningNow = false,
            roomBadgeTitle = "ROOM",
            roomBadgeValue = extractCleanRoomNumber(firstClass.room)
        )
    }
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
    scheduleDays: List<DayModel> = emptyList(),
    selectedDayIndex: Int = 0,
    onSelectDay: (Int) -> Unit = {},
    existingClasses: List<ClassModel> = emptyList(),
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
    var courseError by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var selectedAccentIndex by remember { mutableStateOf(0) }
    val sheetScrollState = rememberScrollState()
    val isDark = LocalIsDarkTheme.current

    val accentPalettes = remember {
        listOf(
            listOf(Color(0xFF3B82F6), Color(0xFF6366F1)), // Electric Blue / Indigo
            listOf(Color(0xFF10B981), Color(0xFF059669)), // Emerald Green
            listOf(Color(0xFFF59E0B), Color(0xFFEA580C)), // Amber Orange
            listOf(Color(0xFF8B5CF6), Color(0xFF7C3AED)), // Royal Violet
            listOf(Color(0xFFF43F5E), Color(0xFFE11D48))  // Coral Rose
        )
    }
    val currentPalette = accentPalettes[selectedAccentIndex.coerceIn(0, accentPalettes.size - 1)]

    val popularTimeSlots = remember {
        listOf(
            "08:30 AM - 10:00 AM",
            "10:05 AM - 11:35 AM",
            "11:40 AM - 01:10 PM",
            "01:45 PM - 03:15 PM",
            "03:20 PM - 04:50 PM"
        )
    }

    val recentCourses = remember(existingClasses) {
        existingClasses.map { it.courseName.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .take(6)
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = {
                Text(
                    text = "Delete Class Routine?",
                    fontFamily = PlusJakartaSansFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = "This will remove your entire class routine schedule permanently. Are you sure you want to proceed?",
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDeleteRoutine()
                    }
                ) {
                    Text(
                        text = "Delete Routine",
                        color = Color(0xFFDC2626),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel", fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .verticalScroll(sheetScrollState)
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Row: Modern Segmented Switcher & Safe Delete Action
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mode Switcher Pills
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Manual Entry Tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isManualMode) {
                                if (isDark) Color(0xFF334155) else Color.White
                            } else Color.Transparent
                        )
                        .then(
                            if (isManualMode) Modifier.shadow(2.dp, RoundedCornerShape(10.dp))
                            else Modifier
                        )
                        .clickable { isManualMode = true }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = if (isManualMode) (if (isDark) Color(0xFF60A5FA) else Color(0xFF0066FF))
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Manual Entry",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 13.sp,
                            fontWeight = if (isManualMode) FontWeight.Bold else FontWeight.Medium,
                            color = if (isManualMode) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // AI Routine Scan Tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (!isManualMode) {
                                if (isDark) Color(0xFF334155) else Color.White
                            } else Color.Transparent
                        )
                        .then(
                            if (!isManualMode) Modifier.shadow(2.dp, RoundedCornerShape(10.dp))
                            else Modifier
                        )
                        .clickable { isManualMode = false }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = if (!isManualMode) Color(0xFF0284C7)
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "AI Routine Scan",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 13.sp,
                            fontWeight = if (!isManualMode) FontWeight.Bold else FontWeight.Medium,
                            color = if (!isManualMode) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Delete Routine Action Button
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isDark) Color(0x24EF4444) else Color(0xFFFEE2E2))
                    .border(
                        1.dp,
                        if (isDark) Color(0x40EF4444) else Color(0xFFFCA5A5),
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { showDeleteConfirmDialog = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Routine",
                    tint = if (isDark) Color(0xFFFCA5A5) else Color(0xFFDC2626),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Horizontal Day Selector Chips
        if (scheduleDays.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                scheduleDays.forEach { day ->
                    val isSelected = day.dayIndex == selectedDayIndex
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) {
                                    Brush.horizontalGradient(colors = currentPalette)
                                } else {
                                    SolidColor(if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC))
                                }
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) Color.Transparent
                                else if (isDark) Color(0x26FFFFFF) else Color(0x1F000000),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { onSelectDay(day.dayIndex) }
                            .padding(horizontal = 14.dp, vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = day.shortName,
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        AnimatedContent(
            targetState = isManualMode,
            transitionSpec = {
                fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180))
            },
            label = "SheetModeSwitch"
        ) { manualMode ->
            if (manualMode) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. DYNAMIC LIVE CARD PREVIEW
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (isDark) Color(0xFF182234) else Color(0xFFF8FAFD))
                            .border(
                                width = 1.2.dp,
                                color = currentPalette.first().copy(alpha = if (isDark) 0.4f else 0.3f),
                                shape = RoundedCornerShape(18.dp)
                            )
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(currentPalette.first())
                                )
                                Text(
                                    text = "LIVE CARD PREVIEW",
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = currentPalette.first(),
                                    letterSpacing = 0.5.sp
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(currentPalette.first().copy(alpha = 0.12f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = selectedDayName,
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = currentPalette.first()
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Left Color Accent Bar
                            Box(
                                modifier = Modifier
                                    .width(4.5.dp)
                                    .height(64.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Brush.verticalGradient(currentPalette))
                            )

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = if (courseName.isNotBlank()) {
                                        if (faculty.isNotBlank()) "$courseName ($faculty)" else courseName
                                    } else "Course Code (e.g., CSE 210)",
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (courseName.isNotBlank()) MaterialTheme.colorScheme.onSurface
                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    maxLines = 1
                                )

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AccessTime,
                                            contentDescription = null,
                                            tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = timeSlot.ifBlank { "08:15 AM - 09:45 AM" },
                                            fontFamily = PlusJakartaSansFontFamily,
                                            fontSize = 12.sp,
                                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Home,
                                            contentDescription = null,
                                            tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = if (room.isNotBlank()) {
                                                if (room.startsWith("Room", ignoreCase = true)) room else "Room $room"
                                            } else "Room TBA",
                                            fontFamily = PlusJakartaSansFontFamily,
                                            fontSize = 12.sp,
                                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 2. CARD COLOR PALETTE PICKER
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Card Theme Accent",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            accentPalettes.forEachIndexed { index, palette ->
                                val isSelected = selectedAccentIndex == index
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(Brush.linearGradient(palette))
                                        .then(
                                            if (isSelected) {
                                                Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                            } else Modifier
                                        )
                                        .clickable { selectedAccentIndex = index },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 3. RECENT COURSES QUICK FILL CHIPS
                    if (recentCourses.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Quick Fill from Routine",
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                recentCourses.forEach { courseStr ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isDark) Color(0xFF1E293B) else Color(0xFFEFF6FF))
                                            .border(
                                                1.dp,
                                                if (isDark) Color(0x333B82F6) else Color(0xFFBFDBFE),
                                                RoundedCornerShape(10.dp)
                                            )
                                            .clickable {
                                                val parenStart = courseStr.indexOf('(')
                                                val parenEnd = courseStr.indexOf(')')
                                                if (parenStart != -1 && parenEnd != -1 && parenEnd > parenStart) {
                                                    courseName = courseStr.substring(0, parenStart).trim()
                                                    faculty = courseStr.substring(parenStart + 1, parenEnd).trim()
                                                } else {
                                                    courseName = courseStr
                                                }
                                                courseError = false
                                            }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = courseStr,
                                            fontFamily = PlusJakartaSansFontFamily,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDark) Color(0xFF93C5FD) else Color(0xFF1D4ED8)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 4. COURSE CODE & FACULTY INPUT ROW
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
                                    tint = currentPalette.first(),
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            modifier = Modifier.weight(1.3f),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = currentPalette.first(),
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
                                    tint = currentPalette.first(),
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            modifier = Modifier.weight(0.9f),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = currentPalette.first(),
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
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }

                    // 5. POPULAR TIME SLOTS CHIPS
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Popular Time Slots",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            popularTimeSlots.forEach { slot ->
                                val isSelected = timeSlot == slot
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isSelected) (if (isDark) Color(0xFF1E3A8A) else Color(0xFFDBEAFE))
                                            else (if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9))
                                        )
                                        .border(
                                            1.dp,
                                            if (isSelected) (if (isDark) Color(0xFF60A5FA) else Color(0xFF3B82F6))
                                            else Color.Transparent,
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable { timeSlot = slot }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = slot,
                                        fontFamily = PlusJakartaSansFontFamily,
                                        fontSize = 11.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) (if (isDark) Color(0xFF93C5FD) else Color(0xFF1D4ED8))
                                        else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    // 6. TIME & ROOM INPUT ROW
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
                                    tint = currentPalette.first(),
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            modifier = Modifier.weight(1.2f),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = currentPalette.first(),
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
                                    tint = currentPalette.first(),
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            modifier = Modifier.weight(0.9f),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = currentPalette.first(),
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                focusedContainerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC),
                                unfocusedContainerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // 7. PRIMARY SAVE BUTTON
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .shadow(
                                elevation = 6.dp,
                                shape = RoundedCornerShape(14.dp),
                                ambientColor = currentPalette.first().copy(alpha = 0.35f),
                                spotColor = currentPalette.first().copy(alpha = 0.35f)
                            )
                            .clip(RoundedCornerShape(14.dp))
                            .background(brush = Brush.linearGradient(colors = currentPalette))
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
                                    onAddClass(
                                        fullDisplay,
                                        timeSlot.ifBlank { "08:15 AM - 09:45 AM" },
                                        room.ifBlank { "Room TBA" }
                                    )
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Save Class to Schedule",
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Photo in, routine out",
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Point at your printed routine or pick a screenshot. Reads securely on your phone using AI OCR.",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.95f),
                            lineHeight = 19.sp
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
                                text = "Scan with Camera",
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                brush = if (isDark) {
                                    Brush.horizontalGradient(
                                        colors = listOf(Color(0x280066FF), Color(0x1F70D6FF))
                                    )
                                } else {
                                    Brush.horizontalGradient(
                                        colors = listOf(Color(0xFFEFF6FF), Color(0xFFE0F2FE))
                                    )
                                }
                            )
                            .border(
                                width = 1.5.dp,
                                brush = if (isDark) {
                                    Brush.horizontalGradient(
                                        colors = listOf(Color(0x5560A5FA), Color(0x3338BDF8))
                                    )
                                } else {
                                    Brush.horizontalGradient(
                                        colors = listOf(Color(0xFFBAE6FD), Color(0xFF93C5FD))
                                    )
                                },
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { onChooseGallery() },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(if (isDark) Color(0x3360A5FA) else Color(0x200066FF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Image,
                                    contentDescription = null,
                                    tint = if (isDark) Color(0xFF93C5FD) else Color(0xFF0066FF),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                text = "Scan Routine Photo from Gallery",
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFF93C5FD) else Color(0xFF0066FF)
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isDark) Color(0x3338BDF8) else Color(0xFFDBEAFE))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "AI OCR",
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
                                )
                            }
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
                            .height(50.dp),
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
                            fontSize = 15.sp,
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
    onScanCamera: () -> Unit,
    onChooseGallery: () -> Unit,
    onAddManually: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isDark) 0.dp else 4.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = Color.Black.copy(alpha = 0.06f),
                spotColor = Color.Black.copy(alpha = 0.06f)
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Compact Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(
                            brush = Brush.linearGradient(
                                colors = if (isDark) {
                                    listOf(Color(0xFF3B82F6).copy(alpha = 0.3f), Color(0xFF6366F1).copy(alpha = 0.2f))
                                } else {
                                    listOf(Color(0xFFDBEAFE), Color(0xFFE0E7FF))
                                }
                            )
                        )
                        .border(
                            1.dp,
                            if (isDark) Color(0xFF60A5FA).copy(alpha = 0.4f) else Color(0xFFBFDBFE),
                            RoundedCornerShape(13.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = if (isDark) Color(0xFF93C5FD) else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Set Up Your Class Routine",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            shape = CircleShape,
                            color = (if (isDark) Color(0xFF4ADE80) else Color(0xFF16A34A)).copy(alpha = if (isDark) 0.2f else 0.12f)
                        ) {
                            Text(
                                text = "Easy Setup",
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFF4ADE80) else Color(0xFF16A34A),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "Scan routine photo to auto-fill or enter manually",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 2. Side-by-Side Dual Smart Scan Cards (Camera & Gallery)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Card A: 📸 Scan with Camera
                BentoScanOptionCard(
                    modifier = Modifier.weight(1f),
                    isDark = isDark,
                    icon = Icons.Default.CameraAlt,
                    accentColor = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
                    gradientColors = if (isDark) {
                        listOf(Color(0xFF0C2138), Color(0xFF0F172A))
                    } else {
                        listOf(Color(0xFFF0F9FF), Color(0xFFE0F2FE))
                    },
                    borderColor = if (isDark) Color(0xFF38BDF8).copy(alpha = 0.35f) else Color(0xFFBAE6FD),
                    badgeText = "Fastest",
                    title = "Take Photo",
                    subtitle = "Scan printed sheet",
                    onClick = onScanCamera
                )

                // Card B: 🖼️ Upload from Gallery
                BentoScanOptionCard(
                    modifier = Modifier.weight(1f),
                    isDark = isDark,
                    icon = Icons.Default.Image,
                    accentColor = if (isDark) Color(0xFF818CF8) else Color(0xFF4F46E5),
                    gradientColors = if (isDark) {
                        listOf(Color(0xFF1E1B4B).copy(alpha = 0.8f), Color(0xFF0F172A))
                    } else {
                        listOf(Color(0xFFEEF2FF), Color(0xFFE0E7FF))
                    },
                    borderColor = if (isDark) Color(0xFF818CF8).copy(alpha = 0.35f) else Color(0xFFC7D2FE),
                    badgeText = "Gallery",
                    title = "Upload Photo",
                    subtitle = "From saved images",
                    onClick = onChooseGallery
                )
            }

            // 3. Compact Manual Entry Row
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onAddManually() },
                shape = RoundedCornerShape(14.dp),
                color = if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f) else Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.22f else 0.1f),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Or add classes manually",
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Type course codes, room numbers & timings yourself",
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun BentoScanOptionCard(
    modifier: Modifier = Modifier,
    isDark: Boolean,
    icon: ImageVector,
    accentColor: Color,
    gradientColors: List<Color>,
    borderColor: Color,
    badgeText: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Box(
            modifier = Modifier
                .background(brush = Brush.linearGradient(colors = gradientColors))
                .padding(12.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                color = accentColor.copy(alpha = if (isDark) 0.25f else 0.15f),
                                shape = RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = accentColor.copy(alpha = if (isDark) 0.22f else 0.12f)
                    ) {
                        Text(
                            text = badgeText,
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
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
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
