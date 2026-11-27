package com.example.crconnect.student

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import com.example.crconnect.student.notifications.NotificationCenterDialog
import com.example.crconnect.student.notifications.NotificationRepository
import com.example.crconnect.student.notifications.RoutineNotificationHelper
import com.yalantis.ucrop.UCrop
import java.io.File
import java.io.FileOutputStream
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.crconnect.CustomToast
import com.example.crconnect.SessionManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StudentPortalScreen(
    viewModel: StudentPortalViewModel,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedImageForCrop by remember { mutableStateOf<Uri?>(null) }
    var isSecurityDialogOpen by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val sourceUri = result.data?.data
            if (sourceUri != null) {
                selectedImageForCrop = sourceUri
            } else {
                viewModel.showToast("Could not load selected image")
            }
        }
    }

    if (selectedImageForCrop != null) {
        ProfileCropDialog(
            context = context,
            imageUri = selectedImageForCrop!!,
            onDismiss = { selectedImageForCrop = null },
            onCropSuccess = { croppedUri ->
                selectedImageForCrop = null
                viewModel.updateProfileImage(croppedUri, context)
            }
        )
    }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { msg ->
            CustomToast.show(context, msg)
            viewModel.clearSnackbar()
        }
    }

    var isNotificationCenterOpen by remember { mutableStateOf(false) }
    var isElectionSubPageOpen by remember { mutableStateOf(false) }
    var unreadNotificationCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        RoutineNotificationHelper.initNotificationChannels(context)
        unreadNotificationCount = NotificationRepository.getUnreadCount(context)
    }

    LaunchedEffect(uiState.classMap) {
        if (uiState.classMap.isNotEmpty()) {
            RoutineNotificationHelper.scheduleAllRoutines(context, uiState.classMap)
        }
    }

    var isNavVisible by remember { mutableStateOf(true) }
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                if (delta < -12f && isNavVisible) {
                    isNavVisible = false
                } else if (delta > 12f && !isNavVisible) {
                    isNavVisible = true
                }
                return Offset.Zero
            }
        }
    }

    val navOffsetY by animateFloatAsState(
        targetValue = if (isNavVisible) 0f else 180f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "StudentNavHideAnimation"
    )

    val systemDark = isSystemInDarkTheme()
    val isDark = when (uiState.themeMode) {
        SessionManager.THEME_LIGHT -> false
        SessionManager.THEME_DARK -> true
        else -> systemDark
    }
    val portalGradient = getPortalGradient(isDark)

    // Handle Android system back navigation:
    // 1. If drawer is open, close drawer
    // 2. If on another tab (Profile / Election / Attendance), return to Routine tab (0)
    // 3. If on Routine tab, normal system back (exit)
    BackHandler(enabled = drawerState.isOpen || uiState.selectedTab != 0) {
        if (drawerState.isOpen) {
            coroutineScope.launch { drawerState.close() }
        } else if (uiState.selectedTab != 0) {
            isNavVisible = true
            viewModel.selectTab(0)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = portalGradient)
    ) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            scrimColor = Color(0x660F172A),
            drawerContent = {
                StudentPortalDrawerContent(
                    profile = uiState.profile,
                    selectedTab = uiState.selectedTab,
                    isGateOpen = uiState.isGateOpen,
                    onTabSelected = { tab ->
                        coroutineScope.launch {
                            drawerState.close()
                            isNavVisible = true
                            viewModel.selectTab(tab)
                        }
                    },
                    onLockedItemClicked = {
                        CustomToast.show(context, "Election is currently locked by teacher")
                    },
                    onOpenElection = {
                        coroutineScope.launch { drawerState.close() }
                        isElectionSubPageOpen = true
                    },
                    onSignOut = {
                        coroutineScope.launch {
                            drawerState.close()
                            onSignOut()
                        }
                    },
                    currentThemeMode = uiState.themeMode,
                    onThemeModeSelected = { mode ->
                        viewModel.setThemeMode(context, mode)
                    }
                )
            }
        ) {
            Scaffold(
                topBar = {
                    val isProfileTab = (uiState.selectedTab == 3)

                    TopHeader(
                        title = uiState.profile.name.ifBlank { "Student" },
                        name = uiState.profile.name,
                        department = uiState.profile.department,
                        initials = uiState.profile.initials,
                        profileImageUrl = uiState.profile.profileImageUrl,
                        isCr = uiState.isCr,
                        isProfileTab = isProfileTab,
                        unreadNotificationCount = unreadNotificationCount,
                        onMenuClick = {
                            coroutineScope.launch { drawerState.open() }
                        },
                        onAvatarClick = {
                            isNavVisible = true
                            viewModel.selectTab(3) // Profile Tab
                        },
                        onNotificationClick = {
                            isNotificationCenterOpen = true
                        },
                        onSignOutClick = onSignOut
                    )
                },
                snackbarHost = { SnackbarHost(snackbarHostState) },
                containerColor = Color.Transparent
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(nestedScrollConnection)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = paddingValues.calculateTopPadding())
                    ) {
                        when (uiState.selectedTab) {
                            0 -> RoutineTab(
                                uiState = uiState,
                                onDaySelected = { day -> viewModel.selectScheduleDay(day) },
                                onOpenAddClass = { viewModel.openAddClassSheet() },
                                onOpenEditClass = { classModel, dayIndex ->
                                    viewModel.openEditClassSheet(classModel, dayIndex)
                                },
                                onCloseAddClass = { viewModel.closeAddClassSheet() },
                                onAddClass = { course, time, room, colorIndex ->
                                    viewModel.addClass(course, time, room, colorIndex, context)
                                },
                                onUpdateClass = { originalClass, course, time, room, targetDayIndex, colorIndex ->
                                    viewModel.updateClass(
                                        originalClass = originalClass,
                                        originalDayIndex = uiState.editingClassDayIndex ?: uiState.selectedDayIndex,
                                        updatedCourseName = course,
                                        updatedTimeSlot = time,
                                        updatedRoom = room,
                                        targetDayIndex = targetDayIndex,
                                        colorIndex = colorIndex,
                                        context = context
                                    )
                                },
                                onDeleteSingleClass = { classModel, dayIndex ->
                                    viewModel.deleteSingleClass(classModel, dayIndex, context)
                                },
                                onOpenNoticeSheet = { viewModel.openNoticeSheet() },
                                onDeleteRoutine = {
                                    viewModel.deleteCurrentRoutine(context)
                                },
                                onScannedEntriesAdded = { entries ->
                                    viewModel.saveScannedEntries(entries, context)
                                },
                                onLoadSampleRoutine = {
                                    viewModel.loadSampleRoutine(context)
                                },
                                onNavigateWeek = { delta -> viewModel.navigateWeek(delta) },
                                onToggleAttendance = { classId, state, dayIdx -> viewModel.toggleAttendance(classId, state, dayIdx, context) },
                                onOpenElection = { isElectionSubPageOpen = true },
                                onShowToast = { msg -> viewModel.showToast(msg) }
                            )
                            1 -> AcademicOverviewTab(
                                uiState = uiState,
                                profile = uiState.profile,
                                classMap = uiState.classMap,
                                onUpdateCgpa = { newCgpa -> viewModel.updateCgpa(newCgpa, context) },
                                onUpdateSection = { newSection -> viewModel.updateSection(newSection, context) },
                                onUpdateAdvisorInfo = { name, email -> viewModel.updateAdvisorInfo(name, email, context) },
                                onUpdateCurrentTerm = { term -> viewModel.updateCurrentTerm(term, context) },
                                onRefreshHistory = { viewModel.loadAttendanceHistory(context) }
                            )
                            2 -> ElectionTab(
                                uiState = uiState,
                                onCastVote = { candidateId -> viewModel.castVote(candidateId) }
                            )
                            3 -> ProfileTab(
                                profile = uiState.profile,
                                uiState = uiState,
                                classMap = uiState.classMap,
                                themeMode = uiState.themeMode,
                                onThemeModeSelected = { mode -> viewModel.setThemeMode(context, mode) },
                                onEditProfilePicture = {
                                    val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                                        type = "image/*"
                                    }
                                    galleryLauncher.launch(intent)
                                },
                                onUpdateCgpa = { newCgpa -> viewModel.updateCgpa(newCgpa, context) },
                                onUpdateStudentId = { newId -> viewModel.updateStudentId(newId, context) },
                                onUpdateSection = { newSection -> viewModel.updateSection(newSection, context) },
                                onUpdateAdvisorInfo = { name, email -> viewModel.updateAdvisorInfo(name, email, context) },
                                onUpdateCurrentTerm = { term -> viewModel.updateCurrentTerm(term, context) },
                                onSecuritySettings = { isSecurityDialogOpen = true },
                                onCheckUpdate = { viewModel.checkForAppUpdates(context, forceCheck = true) },
                                onShowUpdateDialog = { viewModel.showUpdateDialog() },
                                onNavigateToAttendance = { viewModel.selectTab(1) },
                                onNavigateToAcademics = { viewModel.selectTab(1) },
                                onSignOut = onSignOut,
                                onLogout = onSignOut
                            )
                        }
                    }

                    if (isSecurityDialogOpen) {
                        SecuritySettingsDialog(
                            profile = uiState.profile,
                            onDismiss = { isSecurityDialogOpen = false },
                            onResetPassword = { viewModel.sendPasswordResetEmail(context) },
                            onShowToast = { msg -> viewModel.showToast(msg) }
                        )
                    }

                    if (isNotificationCenterOpen) {
                        NotificationCenterDialog(
                            onDismiss = {
                                isNotificationCenterOpen = false
                                unreadNotificationCount = NotificationRepository.getUnreadCount(context)
                            },
                            onNavigateToRoutine = { viewModel.selectTab(0) },
                            onNavigateToAcademics = { viewModel.selectTab(1) }
                        )
                    }

                    if (uiState.isUpdateDialogVisible && uiState.isUpdateAvailable) {
                        AppUpdateDialog(
                            uiState = uiState,
                            onDismiss = { viewModel.dismissUpdateDialog() },
                            onDownloadAndInstall = { viewModel.startApkDownloadAndInstall(context) }
                        )
                    }

                    if (uiState.isUpToDateDialogVisible) {
                        AppUpToDateDialog(
                            versionName = uiState.currentAppVersion,
                            onDismiss = { viewModel.dismissUpToDateDialog() }
                        )
                    }

                    if (isElectionSubPageOpen) {
                        FullScreenSubPageDialog(
                            onDismissRequest = { isElectionSubPageOpen = false },
                            isDark = isDark
                        ) {
                            BackHandler { isElectionSubPageOpen = false }
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(portalGradient)
                                    .statusBarsPadding()
                                    .navigationBarsPadding()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        modifier = Modifier.size(40.dp),
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.surface,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                        onClick = { isElectionSubPageOpen = false }
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
                                            text = "CR Election Portal",
                                            fontFamily = PlusJakartaSansFontFamily,
                                            fontSize = 18.sp,
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Live Candidate Roster & Electronic Ballot",
                                            fontFamily = PlusJakartaSansFontFamily,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (uiState.isGateOpen) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFEF4444).copy(alpha = 0.15f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, if (uiState.isGateOpen) Color(0xFF10B981).copy(alpha = 0.3f) else Color(0xFFEF4444).copy(alpha = 0.3f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(if (uiState.isGateOpen) Color(0xFF10B981) else Color(0xFFEF4444))
                                            )
                                            Text(
                                                text = if (uiState.isGateOpen) "Voting Open" else "Closed",
                                                fontFamily = PlusJakartaSansFontFamily,
                                                fontSize = 11.sp,
                                                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                                                color = if (uiState.isGateOpen) Color(0xFF10B981) else Color(0xFFEF4444)
                                            )
                                        }
                                    }
                                }

                                ElectionTab(
                                    uiState = uiState,
                                    onCastVote = { candidateId -> viewModel.castVote(candidateId) }
                                )
                            }
                        }
                    }

                    StudentBottomNavigation(
                        selectedTab = uiState.selectedTab,
                        isGateOpen = uiState.isGateOpen,
                        onTabSelected = { tab ->
                            isNavVisible = true
                            if (tab == 2 && !uiState.isGateOpen) {
                                CustomToast.show(context, "Election is currently locked by teacher")
                            } else {
                                viewModel.selectTab(tab)
                            }
                        },
                        onLockedTabClicked = {
                            CustomToast.show(context, "Election is currently locked by teacher")
                        },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .graphicsLayer {
                                translationY = navOffsetY
                                alpha = if (navOffsetY >= 160f) 0f else 1f - (navOffsetY / 180f)
                            }
                    )

                    if (uiState.isNoticeSheetOpen) {
                        GlassNoticeBottomSheet(
                            onDismiss = { viewModel.closeNoticeSheet() },
                            onPublish = { title, body -> viewModel.publishNotice(title, body) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassNoticeBottomSheet(
    onDismiss: () -> Unit,
    onPublish: (title: String, body: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFEF3C7))
                            .border(1.dp, Color(0xFFFCD34D), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Broadcast Class Notice",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Send instant alert to all classmates",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Notice Title (e.g. Extra Class / Exam Room)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = body,
                onValueChange = { body = it },
                label = { Text("Notice Description / Details") },
                minLines = 3,
                maxLines = 5,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            PrimaryButton(
                text = "Broadcast Notice",
                onClick = {
                    onPublish(title, body)
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun StudentPortalPreview() {
    StudentPortalTheme {
        StudentPortalScreen(
            viewModel = StudentPortalViewModel(),
            onSignOut = {}
        )
    }
}
