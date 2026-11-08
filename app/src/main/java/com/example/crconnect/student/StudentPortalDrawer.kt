package com.example.crconnect.student

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.crconnect.R

@Composable
fun StudentPortalDrawerContent(
    profile: UserProfileModel,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onSignOut: () -> Unit
) {
    val scrollState = rememberScrollState()
    var localSelectedTab by remember(selectedTab) { mutableIntStateOf(selectedTab) }
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
                        localSelectedTab = 3
                        onTabSelected(3)
                    }
                    .padding(start = 24.dp, end = 20.dp, top = 24.dp, bottom = 20.dp)
            ) {
                // Rounded Blue Profile Avatar Container Box matching target design
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_nav_profile),
                        contentDescription = "Profile",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = profile.name.ifBlank { "Student" },
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
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
                    label = "Candidates",
                    iconRes = R.drawable.ic_people,
                    isSelected = localSelectedTab == 1,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        localSelectedTab = 1
                        onTabSelected(1)
                    }
                )

                Spacer(modifier = Modifier.height(4.dp))

                DrawerNavItem(
                    label = "Class routine",
                    iconRes = R.drawable.ic_nav_routine,
                    isSelected = localSelectedTab == 0,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        localSelectedTab = 0
                        onTabSelected(0)
                    }
                )

                Spacer(modifier = Modifier.height(4.dp))

                DrawerNavItem(
                    label = "Vote status",
                    iconRes = R.drawable.ic_nav_timer,
                    isSelected = localSelectedTab == 2,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        localSelectedTab = 2
                        onTabSelected(2)
                    }
                )

                Spacer(modifier = Modifier.height(4.dp))

                DrawerNavItem(
                    label = "My profile",
                    iconRes = R.drawable.ic_nav_profile,
                    isSelected = localSelectedTab == 3,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        localSelectedTab = 3
                        onTabSelected(3)
                    }
                )

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
    onClick: () -> Unit,
    isDanger: Boolean = false
) {
    val bg = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    val tintColor = when {
        isDanger -> Color(0xFFEF4444)
        isSelected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val textColor = when {
        isDanger -> Color(0xFFEF4444)
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
                    .background(Color(0xFF3B1319)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = label,
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(20.dp)
                )
            }
        } else {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = label,
                tint = tintColor,
                modifier = Modifier.size(22.dp)
            )
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
