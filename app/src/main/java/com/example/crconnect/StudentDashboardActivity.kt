package com.example.crconnect

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.example.crconnect.student.StudentPortalScreen
import com.example.crconnect.student.StudentPortalTheme
import com.example.crconnect.student.StudentPortalViewModel

class StudentDashboardActivity : FragmentActivity() {

    private val viewModel: StudentPortalViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                Color.TRANSPARENT,
                Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.auto(
                Color.TRANSPARENT,
                Color.TRANSPARENT
            )
        )
        super.onCreate(savedInstanceState)

        viewModel.initSession(this)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        setContent {
            val uiState by viewModel.uiState.collectAsState()
            val isSystemDark = isSystemInDarkTheme()
            val isDarkTheme = when (uiState.themeMode) {
                SessionManager.THEME_LIGHT -> false
                SessionManager.THEME_DARK -> true
                else -> isSystemDark
            }

            val windowInsetsController = remember(window) {
                WindowCompat.getInsetsController(window, window.decorView)
            }
            SideEffect {
                windowInsetsController.isAppearanceLightStatusBars = !isDarkTheme
                windowInsetsController.isAppearanceLightNavigationBars = !isDarkTheme
            }

            StudentPortalTheme(darkTheme = isDarkTheme) {
                StudentPortalScreen(
                    viewModel = viewModel,
                    onSignOut = {
                        val bottomSheet = LogoutBottomSheet.newInstance(
                            "Confirm Logout",
                            "Are you sure you want to log out of Student Portal?",
                            "Logout"
                        ) {
                            val intent = Intent(this, MainActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        }
                        bottomSheet.showAllowingStateLoss(supportFragmentManager, LogoutBottomSheet.TAG)
                    }
                )
            }
        }
    }
}
