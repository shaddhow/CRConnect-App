package com.example.crconnect.student

import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.crconnect.SessionManager

@Composable
fun SecuritySettingsDialog(
    profile: UserProfileModel,
    onDismiss: () -> Unit,
    onResetPassword: () -> Unit,
    onShowToast: (String) -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val sessionManager = remember { SessionManager(context) }
    var biometricEnabled by remember { mutableStateOf(sessionManager.isBiometricLockEnabled()) }
    val isDark = LocalIsDarkTheme.current

    val dialogBg = if (isDark) Color(0xFF131B2E) else Color.White
    val dialogBorder = if (isDark) Color(0xFF24354D) else Color(0xFFE2E8F0)
    val cardBg = if (isDark) Color(0xFF192339) else Color(0xFFF8FAFC)
    val cardBorder = if (isDark) Color(0xFF283A55) else Color(0xFFE2E8F0)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(28.dp),
            color = dialogBg,
            border = BorderStroke(1.dp, dialogBorder),
            shadowElevation = if (isDark) 0.dp else 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Top Header Row with Glassy Shield Capsule
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF0066FF).copy(alpha = if (isDark) 0.2f else 0.12f))
                            .border(1.dp, Color(0xFF0066FF).copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Security",
                            tint = Color(0xFF0066FF),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Account Security",
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF10B981).copy(alpha = if (isDark) 0.18f else 0.1f),
                                border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = "100% Protected",
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF10B981),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = "Protected by Firebase Auth & BUBT Vault",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(if (isDark) Color(0x1AFFFFFF) else Color(0x0F000000))
                )

                // Security Status Cards
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // 1. Firebase Authentication Card
                    GlassySecurityRow(
                        icon = Icons.Default.CheckCircle,
                        iconTint = Color(0xFF10B981),
                        title = "Firebase Authentication",
                        subtitle = if (profile.email.isNotBlank()) profile.email else "Verified Student Account",
                        badge = "Verified",
                        badgeColor = Color(0xFF10B981),
                        cardBg = cardBg,
                        cardBorder = cardBorder,
                        isDark = isDark
                    )

                    // 2. Anti-Dual Voting Protection Card
                    GlassySecurityRow(
                        icon = Icons.Default.HowToVote,
                        iconTint = Color(0xFF0066FF),
                        title = "Anti-Dual Voting Lock",
                        subtitle = "One Student ID = Exactly One Ballot",
                        badge = "Enforced",
                        badgeColor = Color(0xFF0066FF),
                        cardBg = cardBg,
                        cardBorder = cardBorder,
                        isDark = isDark
                    )

                    // 3. Biometric App Lock Card
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = cardBg,
                        border = BorderStroke(1.dp, cardBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF8B5CF6).copy(alpha = if (isDark) 0.2f else 0.12f))
                                        .border(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Fingerprint,
                                        contentDescription = null,
                                        tint = Color(0xFF8B5CF6),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = "Biometric App Lock",
                                        fontFamily = PlusJakartaSansFontFamily,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Require fingerprint / face ID",
                                        fontFamily = PlusJakartaSansFontFamily,
                                        fontSize = 11.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            GlassySmoothSwitch(
                                checked = biometricEnabled,
                                onCheckedChange = { checked ->
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    if (checked) {
                                        val fragmentActivity = context.findFragmentActivity()
                                        if (fragmentActivity != null) {
                                            val executor = ContextCompat.getMainExecutor(context)
                                            val biometricPrompt = BiometricPrompt(
                                                fragmentActivity,
                                                executor,
                                                object : BiometricPrompt.AuthenticationCallback() {
                                                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                                        super.onAuthenticationSucceeded(result)
                                                        sessionManager.setBiometricLockEnabled(true)
                                                        biometricEnabled = true
                                                        onShowToast("Biometric Lock Enabled Successfully!")
                                                    }
                                                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                                                        super.onAuthenticationError(errorCode, errString)
                                                        onShowToast("Biometric error: $errString")
                                                        biometricEnabled = false
                                                    }
                                                    override fun onAuthenticationFailed() {
                                                        super.onAuthenticationFailed()
                                                        onShowToast("Biometric authentication failed")
                                                        biometricEnabled = false
                                                    }
                                                }
                                            )
                                            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                                                .setTitle("CRConnect Biometric Security")
                                                .setSubtitle("Authenticate with fingerprint or face ID to enable lock")
                                                .setNegativeButtonText("Cancel")
                                                .build()
                                            biometricPrompt.authenticate(promptInfo)
                                        } else {
                                            sessionManager.setBiometricLockEnabled(true)
                                            biometricEnabled = true
                                            onShowToast("Biometric Lock Enabled")
                                        }
                                    } else {
                                        sessionManager.setBiometricLockEnabled(false)
                                        biometricEnabled = false
                                        onShowToast("Biometric Lock Disabled")
                                    }
                                }
                            )
                        }
                    }

                    // 4. Password Reset Button
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onResetPassword()
                                onDismiss()
                            },
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF0066FF).copy(alpha = if (isDark) 0.14f else 0.08f),
                        border = BorderStroke(1.dp, Color(0xFF0066FF).copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0xFF0066FF),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Send Password Reset Email",
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0066FF)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Done Full Width Gradient Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF0066FF), Color(0xFF0052CC))
                            )
                        )
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onDismiss()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Done",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun GlassySecurityRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    badge: String,
    badgeColor: Color,
    cardBg: Color,
    cardBorder: Color,
    isDark: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconTint.copy(alpha = if (isDark) 0.2f else 0.12f))
                    .border(1.dp, iconTint.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(19.dp)
                )
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
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
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = badgeColor.copy(alpha = if (isDark) 0.18f else 0.1f),
                border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.3f))
            ) {
                Text(
                    text = badge,
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = badgeColor,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                )
            }
        }
    }
}

private fun Context.findFragmentActivity(): FragmentActivity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is FragmentActivity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
