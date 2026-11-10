package com.example.crconnect.student

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
                    TopHeader(
                        title = uiState.profile.name.ifBlank { "Student" },
                        name = uiState.profile.name,
                        department = uiState.profile.department,
                        initials = uiState.profile.initials,
                        profileImageUrl = uiState.profile.profileImageUrl,
                        isCr = uiState.isCr,
                        onMenuClick = {
                            coroutineScope.launch { drawerState.open() }
                        },
                        onAvatarClick = {
                            isNavVisible = true
                            viewModel.selectTab(2) // Profile Tab
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
                        val pagerState = rememberPagerState(
                            initialPage = uiState.selectedTab,
                            pageCount = { 3 }
                        )

                        LaunchedEffect(uiState.selectedTab) {
                            if (pagerState.currentPage != uiState.selectedTab) {
                                pagerState.scrollToPage(uiState.selectedTab)
                            }
                        }

                        LaunchedEffect(pagerState.currentPage, uiState.isGateOpen) {
                            if (pagerState.currentPage == 1 && !uiState.isGateOpen) {
                                val fallbackPage = if (uiState.selectedTab != 1) uiState.selectedTab else 0
                                pagerState.scrollToPage(fallbackPage)
                                CustomToast.show(context, "Election is currently locked by teacher")
                            } else if (uiState.selectedTab != pagerState.currentPage) {
                                viewModel.selectTab(pagerState.currentPage)
                            }
                        }

                        HorizontalPager(
                            state = pagerState,
                            userScrollEnabled = false,
                            modifier = Modifier.fillMaxSize()
                        ) { page ->
                            when (page) {
                                0 -> RoutineTab(
                                    uiState = uiState,
                                    onDaySelected = { day -> viewModel.selectScheduleDay(day) },
                                    onOpenAddClass = { viewModel.openAddClassSheet() },
                                    onCloseAddClass = { viewModel.closeAddClassSheet() },
                                    onAddClass = { course, time, room ->
                                        viewModel.addClass(course, time, room)
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
                                    onShowToast = { msg -> viewModel.showToast(msg) }
                                )
                                1 -> ElectionTab(
                                    uiState = uiState,
                                    onCastVote = { candidateId -> viewModel.castVote(candidateId) }
                                )
                                2 -> ProfileTab(
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
                                    onUpdateAdvisorInfo = { name, email -> viewModel.updateAdvisorInfo(name, email, context) },
                                    onUpdateCurrentTerm = { term -> viewModel.updateCurrentTerm(term, context) },
                                    onSecuritySettings = { isSecurityDialogOpen = true },
                                    onCheckUpdate = { viewModel.checkForAppUpdates(context, forceCheck = true) },
                                    onSignOut = onSignOut
                                )
                            }
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

                    StudentBottomNavigation(
                        selectedTab = uiState.selectedTab,
                        isGateOpen = uiState.isGateOpen,
                        onTabSelected = { tab ->
                            isNavVisible = true
                            viewModel.selectTab(tab)
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
