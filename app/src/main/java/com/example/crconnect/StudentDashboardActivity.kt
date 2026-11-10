package com.example.crconnect

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatDelegate
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
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
import com.google.firebase.auth.FirebaseAuth
import java.util.concurrent.Executor

class StudentDashboardActivity : FragmentActivity() {

    private val viewModel: StudentPortalViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
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

        val sessionManager = SessionManager(this)
        if (sessionManager.isBiometricLockEnabled()) {
            checkBiometricLock()
        } else {
            loadDashboardContent()
        }
    }

    private fun checkBiometricLock() {
        val executor: Executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                loadDashboardContent()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                CustomToast.show(this@StudentDashboardActivity, "Authentication required: $errString")
                if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON || errorCode == BiometricPrompt.ERROR_USER_CANCELED) {
                    finish()
                } else {
                    checkBiometricLock()
                }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                CustomToast.show(this@StudentDashboardActivity, "Fingerprint not recognized. Try again.")
            }
        })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("CRConnect Biometric Lock")
            .setSubtitle("Authenticate to access Student Portal")
            .setNegativeButtonText("Cancel")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    private fun loadDashboardContent() {
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
                            val sessionManager = SessionManager(this@StudentDashboardActivity)
                            sessionManager.logoutUser()
                            FirebaseAuth.getInstance().signOut()
                            val intent = Intent(this@StudentDashboardActivity, MainActivity::class.java)
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
