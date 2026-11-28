package com.example.crconnect.student

import android.graphics.BitmapFactory
import android.widget.ImageView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.signature.ObjectKey
import com.example.crconnect.ProfileImageCacheHelper
import com.example.crconnect.R
import com.example.crconnect.SessionManager
import java.io.File

@Composable
fun StudentPortalDrawerContent(
    profile: UserProfileModel,
    selectedTab: Int,
    isGateOpen: Boolean = true,
    unreadNotificationCount: Int = 0,
    onTabSelected: (Int) -> Unit,
    onLockedItemClicked: () -> Unit = {},
    onOpenElection: () -> Unit = {},
    onOpenNotifications: () -> Unit = {},
    onSignOut: () -> Unit,
    currentThemeMode: String = SessionManager.THEME_SYSTEM,
    onThemeModeSelected: (String) -> Unit = {}
) {
    val scrollState = rememberScrollState()
    var localSelectedTab by remember(selectedTab) { mutableIntStateOf(selectedTab) }
    var isThemeExpanded by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val isDark = LocalIsDarkTheme.current

    ModalDrawerSheet(
        drawerContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color(0xFFFAFCFF),
        drawerTonalElevation = 0.dp,
        drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
        modifier = Modifier
            .width(284.dp)
            .border(
                width = 1.dp,
                color = if (isDark) Color(0xFF334155).copy(alpha = 0.5f) else Color(0xFFE2E8F0),
                shape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(scrollState)
                .statusBarsPadding()
        ) {
            // Drawer Header - Frosted Glass Profile Card
            val formattedName = profile.name.replace("\\s*-\\s*".toRegex(), "-").trim().ifBlank { "Student" }
            val deptText = if (profile.department.contains("CSE", ignoreCase = true)) {
                "Department of CSE, BUBT"
            } else {
                profile.department.ifBlank { "Department of CSE, BUBT" }
            }
            val roleLabel = if (profile.isCr) "Official CR" else "Student"

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 14.dp, top = 16.dp, bottom = 12.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        brush = if (isDark) {
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF1E293B).copy(alpha = 0.65f),
                                    Color(0xFF0F172A).copy(alpha = 0.45f)
                                )
                            )
                        } else {
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFFFFFFF),
                                    Color(0xFFF1F5F9)
                                )
                            )
                        }
                    )
                    .border(
                        width = 1.dp,
                        color = if (isDark) Color(0xFF334155).copy(alpha = 0.6f) else Color(0xFFE2E8F0),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        localSelectedTab = 3
                        onTabSelected(3)
                    }
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Profile Avatar with Glass Ring
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .border(
                                    width = 2.dp,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                    shape = CircleShape
                                )
                                .background(MaterialTheme.colorScheme.surface),
                            contentAlignment = Alignment.Center
                        ) {
                            val bitmap = remember(profile.profileImageUrl) {
                                ProfileImageCacheHelper.loadBitmapFromPathOrBase64(profile.profileImageUrl)
                            }

                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "Profile",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                )
                            } else if (profile.profileImageUrl.isNotBlank()) {
                                AndroidView(
                                    factory = { ctx ->
                                        ImageView(ctx).apply {
                                            scaleType = ImageView.ScaleType.CENTER_CROP
                                            Glide.with(ctx)
                                                .load(profile.profileImageUrl)
                                                .circleCrop()
                                                .signature(ObjectKey(profile.profileImageUrl))
                                                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                                                .into(this)
                                        }
                                    },
                                    update = { view ->
                                        Glide.with(view.context)
                                            .load(profile.profileImageUrl)
                                            .circleCrop()
                                            .signature(ObjectKey(profile.profileImageUrl))
                                            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                                            .into(view)
                                    },
                                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                                )
                            } else {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_nav_profile),
                                    contentDescription = "Profile",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        // Glass Status Micro-Pill
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isDark) Color(0x2210B981) else Color(0x18059669),
                            border = BorderStroke(1.dp, if (isDark) Color(0x4410B981) else Color(0x33059669))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF10B981))
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = if (profile.isCr) "CR PASS" else "STUDENT",
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFF34D399) else Color(0xFF059669),
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = formattedName,
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 21.sp,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (profile.isCr) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(modifier = Modifier.padding(top = 2.dp)) {
                                GlassCrBadge()
                            }
                        }
                    }

                    Text(
                        text = "$roleLabel · $deptText",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Drawer Menu Items
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
            ) {
                DrawerNavItem(
                    label = "Class routine",
                    iconRes = R.drawable.ic_nav_routine,
                    accentColor = Color(0xFF2563EB),
                    isSelected = localSelectedTab == 0,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        isThemeExpanded = false
                        localSelectedTab = 0
                        onTabSelected(0)
                    }
                )

                Spacer(modifier = Modifier.height(6.dp))

                DrawerNavItem(
                    label = "Academics",
                    iconRes = R.drawable.ic_nav_attendance,
                    accentColor = Color(0xFF2563EB),
                    isSelected = localSelectedTab == 1,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        isThemeExpanded = false
                        localSelectedTab = 1
                        onTabSelected(1)
                    }
                )

                Spacer(modifier = Modifier.height(6.dp))

                val isElectionLocked = !isGateOpen
                DrawerNavItem(
                    label = "Election portal",
                    iconRes = R.drawable.ic_people,
                    accentColor = Color(0xFF7C3AED),
                    isSelected = false,
                    isLocked = isElectionLocked,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        isThemeExpanded = false
                        if (isElectionLocked) {
                            onLockedItemClicked()
                        } else {
                            onOpenElection()
                        }
                    }
                )

                Spacer(modifier = Modifier.height(6.dp))

                DrawerNavItem(
                    label = "My profile",
                    iconRes = R.drawable.ic_nav_profile,
                    accentColor = Color(0xFF4F46E5),
                    isSelected = localSelectedTab == 3,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        isThemeExpanded = false
                        localSelectedTab = 3
                        onTabSelected(3)
                    }
                )

                Spacer(modifier = Modifier.height(6.dp))

                DrawerNavItem(
                    label = "Notifications",
                    iconVector = Icons.Default.Notifications,
                    accentColor = Color(0xFF0284C7),
                    isSelected = false,
                    trailingContent = if (unreadNotificationCount > 0) {
                        {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFEF4444))
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (unreadNotificationCount > 99) "99+" else unreadNotificationCount.toString(),
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    } else null,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        isThemeExpanded = false
                        onOpenNotifications()
                    }
                )

                Spacer(modifier = Modifier.height(6.dp))

                // App Theme Expandable Menu Item
                val rotationDegrees by animateFloatAsState(
                    targetValue = if (isThemeExpanded) 180f else 0f,
                    label = "themeChevronRotation"
                )

                DrawerNavItem(
                    label = "App theme",
                    iconRes = R.drawable.ic_moon,
                    accentColor = Color(0xFFD97706),
                    isSelected = isThemeExpanded,
                    trailingContent = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_chevron_down),
                            contentDescription = "Expand theme options",
                            tint = if (isThemeExpanded) Color(0xFFD97706) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(18.dp)
                                .rotate(rotationDegrees)
                        )
                    },
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        isThemeExpanded = !isThemeExpanded
                    }
                )

                AnimatedVisibility(visible = isThemeExpanded) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 4.dp, end = 4.dp, bottom = 6.dp, top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val themes = listOf(
                            Triple(SessionManager.THEME_LIGHT, "Light", R.drawable.ic_theme_light),
                            Triple(SessionManager.THEME_DARK, "Dark", R.drawable.ic_theme_dark),
                            Triple(SessionManager.THEME_SYSTEM, "System", R.drawable.ic_theme_system)
                        )

                        themes.forEach { (mode, label, iconRes) ->
                            val isSelected = currentThemeMode.equals(mode, ignoreCase = true)
                            val glassBg = if (isSelected) {
                                if (isDark) Color(0xFFD97706).copy(alpha = 0.22f) else Color(0xFFFEF3C7)
                            } else {
                                if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f) else Color(0xFFF1F5F9)
                            }
                            val glassBorderColor = if (isSelected) {
                                Color(0xFFD97706).copy(alpha = if (isDark) 0.6f else 0.4f)
                            } else {
                                if (isDark) Color(0xFF334155).copy(alpha = 0.5f) else Color(0xFFE2E8F0)
                            }
                            val themeTextColor = if (isSelected) {
                                if (isDark) Color(0xFFFBBF24) else Color(0xFFB45309)
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                            val themeIconTint = if (isSelected) {
                                if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706)
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(glassBg)
                                    .border(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = glassBorderColor,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        isThemeExpanded = false
                                        onThemeModeSelected(mode)
                                    }
                                    .padding(vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = iconRes),
                                    contentDescription = label,
                                    tint = themeIconTint,
                                    modifier = Modifier.size(20.dp)
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = label,
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                    color = themeTextColor
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                DrawerNavItem(
                    label = "Logout account",
                    iconRes = R.drawable.ic_power_settings,
                    accentColor = Color(0xFFDC2626),
                    isSelected = false,
                    isDanger = true,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSignOut()
                    }
                )
            }

            // Flex spacer pushing footer to bottom
            Spacer(modifier = Modifier.weight(1f))

            // Footer
            Text(
                text = "CRConnect · BUBT Department Network",
                fontFamily = PlusJakartaSansFontFamily,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
            )
        }
    }
}

@Composable
private fun DrawerNavItem(
    label: String,
    iconRes: Int? = null,
    iconVector: ImageVector? = null,
    accentColor: Color,
    isSelected: Boolean,
    isLocked: Boolean = false,
    isDanger: Boolean = false,
    trailingContent: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    val dangerText = if (isDark) Color(0xFFFCA5A5) else Color(0xFFDC2626)
    val dangerBg = if (isDark) Color(0xFF450A0A) else Color(0xFFFEE2E2)
    val effectiveAccent = if (isDanger) dangerText else accentColor

    val rowBgBrush = if (isSelected) {
        if (isDark) {
            Brush.horizontalGradient(
                colors = listOf(
                    effectiveAccent.copy(alpha = 0.22f),
                    effectiveAccent.copy(alpha = 0.08f)
                )
            )
        } else {
            Brush.horizontalGradient(
                colors = listOf(
                    effectiveAccent.copy(alpha = 0.12f),
                    effectiveAccent.copy(alpha = 0.04f)
                )
            )
        }
    } else {
        Brush.horizontalGradient(
            colors = listOf(Color.Transparent, Color.Transparent)
        )
    }

    val rowBorderColor = if (isSelected) {
        effectiveAccent.copy(alpha = if (isDark) 0.35f else 0.22f)
    } else {
        Color.Transparent
    }

    val textColor = when {
        isLocked -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        isDanger -> dangerText
        isSelected -> if (isDark) Color.White else effectiveAccent
        else -> MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(rowBgBrush)
            .border(
                width = 1.dp,
                color = rowBorderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Frosted Glass Icon Capsule with unclipped badge container
        Box(
            modifier = Modifier.size(40.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isDanger) {
                            dangerBg
                        } else if (isSelected) {
                            effectiveAccent.copy(alpha = if (isDark) 0.32f else 0.18f)
                        } else {
                            effectiveAccent.copy(alpha = if (isDark) 0.14f else 0.08f)
                        }
                    )
                    .border(
                        width = 1.dp,
                        color = if (isDanger) {
                            dangerText.copy(alpha = if (isDark) 0.3f else 0.2f)
                        } else if (isSelected) {
                            effectiveAccent.copy(alpha = if (isDark) 0.55f else 0.35f)
                        } else {
                            effectiveAccent.copy(alpha = if (isDark) 0.24f else 0.15f)
                        },
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                val iconTint = when {
                    isLocked -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                    isDanger -> dangerText
                    isSelected -> effectiveAccent
                    else -> effectiveAccent.copy(alpha = if (isDark) 0.95f else 0.85f)
                }
                if (iconVector != null) {
                    Icon(
                        imageVector = iconVector,
                        contentDescription = label,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                } else if (iconRes != null) {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = label,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (isLocked) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 3.dp, y = (-3).dp)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEF4444))
                        .border(1.5.dp, if (isDark) Color(0xFF1E293B) else Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = Color.White,
                        modifier = Modifier.size(9.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Text(
            text = label,
            fontFamily = PlusJakartaSansFontFamily,
            fontSize = 15.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
            color = textColor,
            modifier = Modifier.weight(1f)
        )

        if (trailingContent != null) {
            trailingContent()
        } else if (isLocked) {
            // High visibility "Locked" glass badge
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isDark) Color(0x2E000000) else Color(0x0DEF4444),
                border = BorderStroke(1.dp, if (isDark) Color(0x44EF4444) else Color(0x33EF4444))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = if (isDark) Color(0xFFFCA5A5) else Color(0xFFDC2626),
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Locked",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFFFCA5A5) else Color(0xFFDC2626)
                    )
                }
            }
        } else if (isSelected) {
            // Subtle glowing active indicator dot
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(effectiveAccent)
            )
        }
    }
}
