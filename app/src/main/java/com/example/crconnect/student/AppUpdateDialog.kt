package com.example.crconnect.student

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun AppUpdateDialog(
    uiState: StudentPortalUiState,
    onDismiss: () -> Unit,
    onDownloadAndInstall: () -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    val isForceUpdate = uiState.isForceUpdate
    val isDownloading = uiState.isDownloadingApk
    val isDownloaded = uiState.downloadedApkFile != null && uiState.downloadedApkFile.exists()

    // Smooth pulse animation for the icon glow
    val infiniteTransition = rememberInfiniteTransition(label = "pulseAnim")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.07f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val changelogItems = remember(uiState.releaseNotes) {
        parseCleanChangelog(uiState.releaseNotes)
    }

    Dialog(
        onDismissRequest = {
            if (!isForceUpdate && !isDownloading) {
                onDismiss()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = !isForceUpdate && !isDownloading,
            dismissOnClickOutside = !isForceUpdate && !isDownloading,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .shadow(
                    elevation = if (isDark) 24.dp else 16.dp,
                    shape = RoundedCornerShape(32.dp),
                    spotColor = if (isDark) Color(0xFF6366F1).copy(alpha = 0.35f) else Color(0xFF4F46E5).copy(alpha = 0.2f)
                )
                .clip(RoundedCornerShape(32.dp))
                .border(
                    width = 1.2.dp,
                    brush = if (isDark) {
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF818CF8).copy(alpha = 0.6f),
                                Color(0xFF6366F1).copy(alpha = 0.25f),
                                Color(0xFF1E293B).copy(alpha = 0.4f)
                            )
                        )
                    } else {
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFFA5B4FC).copy(alpha = 0.8f),
                                Color(0xFFE0E7FF),
                                Color(0xFFC7D2FE).copy(alpha = 0.5f)
                            )
                        )
                    },
                    shape = RoundedCornerShape(32.dp)
                ),
            shape = RoundedCornerShape(32.dp),
            color = if (isDark) Color(0xFF0B0F19) else Color(0xFFFFFFFF),
            tonalElevation = 6.dp
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                // Top ambient gradient glow banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    if (isDark) Color(0xFF4F46E5).copy(alpha = 0.22f) else Color(0xFF6366F1).copy(alpha = 0.12f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top Bar: Badge and Close button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Version Pill Badge
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isDark) Color(0xFF1E1B4B).copy(alpha = 0.85f) else Color(0xFFEEF2FF),
                            border = BorderStroke(
                                1.dp,
                                if (isDark) Color(0xFF6366F1).copy(alpha = 0.5f) else Color(0xFFC7D2FE)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(if (isForceUpdate) Color(0xFFEF4444) else Color(0xFF10B981))
                                )
                                Text(
                                    text = if (isForceUpdate) "Mandatory Release" else "New Version Available",
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFFA5B4FC) else Color(0xFF4338CA)
                                )
                            }
                        }

                        // Close Button (Hidden if force update or currently downloading)
                        if (!isForceUpdate && !isDownloading) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9))
                                    .clickable { onDismiss() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Multi-layer Glowing Hero Icon
                    Box(
                        modifier = Modifier
                            .size(78.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Outer glowing ripple ring
                        Box(
                            modifier = Modifier
                                .size(78.dp)
                                .scale(pulseScale)
                                .clip(CircleShape)
                                .background(
                                    if (isDark) {
                                        Color(0xFF6366F1).copy(alpha = 0.18f)
                                    } else {
                                        Color(0xFF4F46E5).copy(alpha = 0.12f)
                                    }
                                )
                        )

                        // Core Gradient Orb
                        Box(
                            modifier = Modifier
                                .size(62.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            Color(0xFF4F46E5),
                                            Color(0xFF7C3AED),
                                            Color(0xFF06B6D4)
                                        )
                                    )
                                )
                                .border(
                                    width = 1.5.dp,
                                    color = Color.White.copy(alpha = 0.35f),
                                    shape = CircleShape
                                )
                                .shadow(8.dp, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isDownloaded) Icons.Default.DownloadDone else Icons.Default.SystemUpdate,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Version Title with Gradient Accent
                    Text(
                        text = "CRConnect ${uiState.latestVersionName}",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(5.dp))

                    Text(
                        text = if (isForceUpdate) {
                            "A critical update is required to continue using CRConnect. Update now to access your schedule and portal features."
                        } else {
                            "A new build is ready with routine enhancements, faculty updates, and performance improvements."
                        },
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 12.5.sp,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569),
                        textAlign = TextAlign.Center,
                        lineHeight = 17.5.sp,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Enhanced Changelog Card (Parsed cleanly without raw markdown)
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = if (isDark) Color(0xFF111827).copy(alpha = 0.85f) else Color(0xFFF8FAFC),
                        border = BorderStroke(
                            1.dp,
                            if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 70.dp, max = 135.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF6366F1))
                                )
                                Text(
                                    text = "WHAT'S NEW",
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.5.sp,
                                    color = if (isDark) Color(0xFFA5B4FC) else Color(0xFF4F46E5)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            if (changelogItems.isNotEmpty()) {
                                changelogItems.forEach { item ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 3.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Text(
                                            text = "✦",
                                            fontSize = 9.sp,
                                            color = if (isDark) Color(0xFF818CF8) else Color(0xFF6366F1),
                                            modifier = Modifier.padding(top = 2.dp, end = 8.dp)
                                        )
                                        Text(
                                            text = item,
                                            fontFamily = PlusJakartaSansFontFamily,
                                            fontSize = 12.sp,
                                            color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155),
                                            lineHeight = 16.5.sp
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = "General stability fixes, performance improvements, and optimized routine notification sync.",
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontSize = 12.sp,
                                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }

                    // Error Message Banner (if download interrupted)
                    AnimatedVisibility(
                        visible = uiState.updateErrorMessage != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isDark) Color(0xFF7F1D1D).copy(alpha = 0.35f) else Color(0xFFFEF2F2),
                            border = BorderStroke(1.dp, if (isDark) Color(0xFF991B1B) else Color(0xFFFECACA)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (isDark) Color(0xFFF87171) else Color(0xFFDC2626),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = uiState.updateErrorMessage ?: "",
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontSize = 11.5.sp,
                                    color = if (isDark) Color(0xFFFCA5A5) else Color(0xFFB91C1C)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Download Progress Section
                    if (isDownloading) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Downloading APK update...",
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                                )
                                Text(
                                    text = "${(uiState.downloadProgress * 100).toInt()}%",
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isDark) Color(0xFF818CF8) else Color(0xFF4F46E5)
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Glowing Gradient Progress Bar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(9.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(uiState.downloadProgress.coerceIn(0.01f, 1f))
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(
                                                    Color(0xFF4F46E5),
                                                    Color(0xFF7C3AED),
                                                    Color(0xFF06B6D4)
                                                )
                                            )
                                        )
                                )
                            }

                            Spacer(modifier = Modifier.height(5.dp))

                            if (uiState.totalMb > 0f) {
                                Text(
                                    text = "${"%.1f".format(uiState.downloadedMb)} MB / ${"%.1f".format(uiState.totalMb)} MB",
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontSize = 11.sp,
                                    color = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    // Bottom Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // "Later" ghost button (hidden for force update)
                        if (!isForceUpdate) {
                            OutlinedButton(
                                onClick = onDismiss,
                                enabled = !isDownloading,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(
                                    1.2.dp,
                                    if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
                                ),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                )
                            ) {
                                Text(
                                    text = "Later",
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Gradient "Download & Install / Install Now" Button
                        Button(
                            onClick = onDownloadAndInstall,
                            enabled = !isDownloading,
                            modifier = Modifier
                                .weight(if (isForceUpdate) 1f else 1.35f)
                                .height(50.dp)
                                .shadow(
                                    elevation = if (isDownloading) 0.dp else 10.dp,
                                    shape = RoundedCornerShape(16.dp),
                                    spotColor = Color(0xFF4F46E5).copy(alpha = 0.5f)
                                ),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        if (isDownloading) {
                                            Brush.horizontalGradient(
                                                listOf(
                                                    Color(0xFF64748B),
                                                    Color(0xFF475569)
                                                )
                                            )
                                        } else {
                                            Brush.horizontalGradient(
                                                listOf(
                                                    Color(0xFF4F46E5),
                                                    Color(0xFF7C3AED)
                                                )
                                            )
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = when {
                                            isDownloading -> Icons.Default.Download
                                            isDownloaded -> Icons.Default.DownloadDone
                                            else -> Icons.Default.Download
                                        },
                                        contentDescription = null,
                                        modifier = Modifier.size(17.dp)
                                    )
                                    Spacer(modifier = Modifier.width(7.dp))
                                    Text(
                                        text = when {
                                            isDownloading -> "Downloading..."
                                            isDownloaded -> "Install Now"
                                            else -> "Update Now"
                                        },
                                        fontFamily = PlusJakartaSansFontFamily,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
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

@Composable
fun AppUpToDateDialog(
    versionName: String,
    onDismiss: () -> Unit
) {
    val isDark = LocalIsDarkTheme.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .wrapContentHeight()
                .shadow(
                    elevation = if (isDark) 24.dp else 16.dp,
                    shape = RoundedCornerShape(30.dp),
                    spotColor = Color(0xFF10B981).copy(alpha = 0.35f)
                )
                .clip(RoundedCornerShape(30.dp))
                .border(
                    width = 1.2.dp,
                    brush = if (isDark) {
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF10B981).copy(alpha = 0.6f),
                                Color(0xFF059669).copy(alpha = 0.2f),
                                Color(0xFF1E293B)
                            )
                        )
                    } else {
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF6EE7B7).copy(alpha = 0.8f),
                                Color(0xFFA7F3D0).copy(alpha = 0.4f)
                            )
                        )
                    },
                    shape = RoundedCornerShape(30.dp)
                ),
            shape = RoundedCornerShape(30.dp),
            color = if (isDark) Color(0xFF0B0F19) else Color.White,
            tonalElevation = 8.dp
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                // Top emerald ambient glow
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    if (isDark) Color(0xFF059669).copy(alpha = 0.22f) else Color(0xFF10B981).copy(alpha = 0.12f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Emerald Check Badge
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        Color(0xFF10B981),
                                        Color(0xFF059669)
                                    )
                                )
                            )
                            .border(
                                width = 1.5.dp,
                                color = Color.White.copy(alpha = 0.4f),
                                shape = CircleShape
                            )
                            .shadow(10.dp, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "You're All Up to Date!",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isDark) Color(0xFF064E3B).copy(alpha = 0.45f) else Color(0xFFECFDF5),
                        border = BorderStroke(
                            1.dp,
                            if (isDark) Color(0xFF059669).copy(alpha = 0.6f) else Color(0xFFA7F3D0)
                        )
                    ) {
                        Text(
                            text = "Version v$versionName · Official Build",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFF34D399) else Color(0xFF059669),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "You are running the newest official version of CRConnect with all recent features, routine sync, and security patches.",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 12.5.sp,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                        textAlign = TextAlign.Center,
                        lineHeight = 17.5.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .shadow(
                                elevation = 8.dp,
                                shape = RoundedCornerShape(16.dp),
                                spotColor = Color(0xFF10B981).copy(alpha = 0.4f)
                            ),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            Color(0xFF10B981),
                                            Color(0xFF059669)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Great!",
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Utility to parse GitHub Release markdown body into clean, user-friendly bullet points.
 */
private fun parseCleanChangelog(rawMarkdown: String): List<String> {
    if (rawMarkdown.isBlank()) return emptyList()

    return rawMarkdown.lines()
        .map { it.trim() }
        .filter { it.isNotBlank() && !it.startsWith("#") }
        .map { line ->
            line.removePrefix("-")
                .removePrefix("*")
                .trim()
                .replace("**", "")
                .replace("__", "")
                .replace("`", "")
        }
        .filter { it.isNotBlank() && it.length > 3 }
        .take(5)
}
