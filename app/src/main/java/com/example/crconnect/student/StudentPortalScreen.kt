package com.example.crconnect.student

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.example.crconnect.CustomToast
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

    val portalGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF70D6FF), // Top (0%)
            Color(0xFFC2EFFF), // Middle (50%)
            Color(0xFFFFFFFF)  // Bottom (100%)
        )
    )

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
                    onTabSelected = { tab ->
                        coroutineScope.launch {
                            drawerState.close()
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
                        initials = uiState.profile.initials,
                        onMenuClick = {
                            coroutineScope.launch { drawerState.open() }
                        },
                        onAvatarClick = {
                            viewModel.selectTab(3) // Profile Tab
                        },
                        onSignOutClick = onSignOut
                    )
                },
                bottomBar = {
                    StudentBottomNavigation(
                        selectedTab = uiState.selectedTab,
                        onTabSelected = { tab ->
                            viewModel.selectTab(tab)
                        }
                    )
                },
                snackbarHost = { SnackbarHost(snackbarHostState) },
                containerColor = Color.Transparent
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    Crossfade(
                        targetState = uiState.selectedTab,
                        animationSpec = tween(durationMillis = 200),
                        label = "StudentPortalTabTransition"
                    ) { tab ->
                        when (tab) {
                            0 -> CandidatesTab(
                                uiState = uiState,
                                onCastVote = { candidateId -> viewModel.castVote(candidateId) }
                            )
                            1 -> RoutineTab(
                                uiState = uiState,
                                onDaySelected = { day -> viewModel.selectScheduleDay(day) },
                                onOpenAddClass = { viewModel.openAddClassSheet() },
                                onCloseAddClass = { viewModel.closeAddClassSheet() },
                                onAddClass = { course, time, room ->
                                    viewModel.addClass(course, time, room)
                                },
                                onScannedEntriesAdded = { entries ->
                                    viewModel.saveScannedEntries(entries, context)
                                },
                                onShowToast = { msg -> viewModel.showToast(msg) }
                            )
                            2 -> StatusTab(
                                uiState = uiState,
                                onGoToCandidates = { viewModel.selectTab(0) }
                            )
                            3 -> ProfileTab(
                                profile = uiState.profile,
                                onEditProfilePicture = { viewModel.showToast("Edit profile picture (demo)") },
                                onSecuritySettings = { viewModel.showToast("Security settings (demo)") },
                                onSignOut = onSignOut
                            )
                        }
                    }
                }
            }
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
