package com.example.crconnect.student

import android.graphics.BitmapFactory
import android.widget.ImageView
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.viewinterop.AndroidView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.signature.ObjectKey
import com.example.crconnect.ProfileImageCacheHelper
import java.io.File
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.crconnect.R
import com.example.crconnect.SessionManager

@Composable
fun StudentPortalDrawerContent(
    profile: UserProfileModel,
    selectedTab: Int,
    isGateOpen: Boolean = true,
    onTabSelected: (Int) -> Unit,
    onLockedItemClicked: () -> Unit = {},
    onSignOut: () -> Unit,
    currentThemeMode: String = SessionManager.THEME_SYSTEM,
    onThemeModeSelected: (String) -> Unit = {}
) {
    val scrollState = rememberScrollState()
    var localSelectedTab by remember(selectedTab) { mutableIntStateOf(selectedTab) }
    var isThemeExpanded by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerTonalElevation = 0.dp,
        drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
        modifier = Modifier
            .width(280.dp)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(scrollState)
                .statusBarsPadding()
        ) {
            // Drawer Header - Avatar & Student Profile Info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        localSelectedTab = 2
                        onTabSelected(2)
                    }
                    .padding(start = 24.dp, end = 20.dp, top = 24.dp, bottom = 20.dp)
            ) {
                // Rounded Blue Profile Avatar Container Box matching target design
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    val context = LocalContext.current
                    val bitmap = remember(profile.profileImageUrl) {
                        val path = profile.profileImageUrl
                        val userKey = profile.studentId.ifBlank { profile.email }
                        try {
                            if (path.isNotBlank() && !path.startsWith("http") && !path.startsWith("data:") && !path.startsWith("content:")) {
                                val file = File(path)
                                if (file.exists() && file.length() > 0) BitmapFactory.decodeFile(file.absolutePath) else null
                            } else {
                                val cached = ProfileImageCacheHelper.getValidLocalProfilePath(context, userKey)
                                if (cached != null) BitmapFactory.decodeFile(cached) else null
                            }
                        } catch (e: Exception) {
                            null
                        }
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
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = profile.name.ifBlank { "Student" },
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 22.sp
                    )
                    if (profile.isCr) {
                        Spacer(modifier = Modifier.width(8.dp))
                        GlassCrBadge()
                    }
                }

                val deptText = if (profile.department.contains("CSE", ignoreCase = true)) {
                    "Department of CSE, BUBT"
                } else {
                    profile.department
                }

                val roleLabel = if (profile.isCr) "Official CR" else "Student"

                Text(
                    text = "$roleLabel · $deptText",
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
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
                    isSelected = localSelectedTab == 0,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        isThemeExpanded = false
                        localSelectedTab = 0
                        onTabSelected(0)
                    }
                )

                Spacer(modifier = Modifier.height(4.dp))

                val isElectionLocked = !isGateOpen
                DrawerNavItem(
                    label = "Election portal",
                    iconRes = R.drawable.ic_people,
                    isSelected = localSelectedTab == 1,
                    isLocked = isElectionLocked,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        isThemeExpanded = false
                        if (isElectionLocked) {
                            onLockedItemClicked()
                        } else {
                            localSelectedTab = 1
                            onTabSelected(1)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(4.dp))

                DrawerNavItem(
                    label = "My profile",
                    iconRes = R.drawable.ic_nav_profile,
                    isSelected = localSelectedTab == 2,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        isThemeExpanded = false
                        localSelectedTab = 2
                        onTabSelected(2)
                    }
                )

                Spacer(modifier = Modifier.height(4.dp))

                // App Theme Expandable Menu Item
                val rotationDegrees by animateFloatAsState(
                    targetValue = if (isThemeExpanded) 180f else 0f,
                    label = "themeChevronRotation"
                )

                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (isThemeExpanded) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                else Color.Transparent
                            )
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                isThemeExpanded = !isThemeExpanded
                            }
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_moon),
                            contentDescription = "App Theme",
                            tint = if (isThemeExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Text(
                            text = "App theme",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )

                        Icon(
                            painter = painterResource(id = R.drawable.ic_chevron_down),
                            contentDescription = "Expand theme options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(18.dp)
                                .rotate(rotationDegrees)
                        )
                    }

                    AnimatedVisibility(visible = isThemeExpanded) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 8.dp, end = 8.dp, bottom = 8.dp, top = 6.dp),
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
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                }
                                val glassBorderColor = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outline
                                }
                                val textColor = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
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
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = label,
                                        fontFamily = PlusJakartaSansFontFamily,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                        color = textColor
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                DrawerNavItem(
                    label = "Logout account",
                    iconRes = R.drawable.ic_power_settings,
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
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            )
        }
    }
}

@Composable
private fun DrawerNavItem(
    label: String,
    iconRes: Int,
    isSelected: Boolean,
    isLocked: Boolean = false,
    onClick: () -> Unit,
    isDanger: Boolean = false
) {
    val bg = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    val isDark = LocalIsDarkTheme.current
    val dangerText = if (isDark) Color(0xFFFCA5A5) else Color(0xFFDC2626)
    val dangerBg = if (isDark) Color(0xFF450A0A) else Color(0xFFFEE2E2)
    val tintColor = when {
        isLocked -> DisabledText
        isDanger -> dangerText
        isSelected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val textColor = when {
        isLocked -> DisabledText
        isDanger -> dangerText
        isSelected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .clickable { onClick() }
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isDanger) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(dangerBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = label,
                    tint = dangerText,
                    modifier = Modifier.size(20.dp)
                )
            }
        } else {
            Box {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = label,
                    tint = tintColor,
                    modifier = Modifier.size(22.dp)
                )
                if (isLocked) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 6.dp, y = (-4).dp)
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(ErrorColor),
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
        }

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = label,
            fontFamily = PlusJakartaSansFontFamily,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}
