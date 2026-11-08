package com.example.crconnect.student

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
import com.example.crconnect.CustomToast
import com.example.crconnect.SessionManager
import kotlinx.coroutines.launch

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
        val headerTitle = when (uiState.selectedTab) {
            0 -> "Class Routine"
            1 -> "CR Candidates"
            2 -> "Election Status"
            3 -> "My Profile"
            else -> "Class Routine"
        }

        ModalNavigationDrawer(
            drawerState = drawerState,
            scrimColor = Color(0x660F172A),
            drawerContent = {
                StudentPortalDrawerContent(
                    profile = uiState.profile,
                    selectedTab = uiState.selectedTab,
                    onTabSelected = { tab ->
                        coroutineScope.launch {
                            drawerState.close()
                            isNavVisible = true
                            viewModel.selectTab(tab)
                        }
                    },
                    onSignOut = {
                        coroutineScope.launch {
                            drawerState.close()
                            onSignOut()
                        }
                    }
                )
            }
        ) {
            Scaffold(
                topBar = {
                    TopHeader(
                        title = headerTitle,
                        name = uiState.profile.name,
                        department = uiState.profile.department,
                        initials = uiState.profile.initials,
                        isCr = uiState.isCr,
                        onMenuClick = {
                            coroutineScope.launch { drawerState.open() }
                        },
                        onAvatarClick = {
                            isNavVisible = true
                            viewModel.selectTab(3) // Profile Tab
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
                        Crossfade(
                            targetState = uiState.selectedTab,
                            animationSpec = tween(durationMillis = 200),
                            label = "StudentPortalTabTransition"
                        ) { tab ->
                            when (tab) {
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
                                    onShowToast = { msg -> viewModel.showToast(msg) }
                                )
                                1 -> CandidatesTab(
                                    uiState = uiState,
                                    onCastVote = { candidateId -> viewModel.castVote(candidateId) }
                                )
                                2 -> StatusTab(
                                    uiState = uiState,
                                    onGoToCandidates = {
                                        isNavVisible = true
                                        viewModel.selectTab(1)
                                    }
                                )
                                3 -> ProfileTab(
                                    profile = uiState.profile,
                                    themeMode = uiState.themeMode,
                                    onThemeModeSelected = { mode -> viewModel.setThemeMode(context, mode) },
                                    onEditProfilePicture = { viewModel.showToast("Edit profile picture (demo)") },
                                    onSecuritySettings = { viewModel.showToast("Security settings (demo)") },
                                    onSignOut = onSignOut
                                )
                            }
                        }
                    }

                    StudentBottomNavigation(
                        selectedTab = uiState.selectedTab,
                        onTabSelected = { tab ->
                            isNavVisible = true
                            viewModel.selectTab(tab)
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
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Send instant alert to all classmates",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
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
