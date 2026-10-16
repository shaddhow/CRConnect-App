package com.example.crconnect.student

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
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

    ModalDrawerSheet(
        drawerContainerColor = Color.White,
        drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
        modifier = Modifier.width(300.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(scrollState)
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        ) {
            // Drawer Header (.dh)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFFE6F6FF), Color.White)
                        )
                    )
                    .drawBehind {
                        drawLine(
                            color = BorderColor,
                            start = Offset(0f, size.height),
                            end = Offset(size.width, size.height),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                    .statusBarsPadding()
                    .padding(20.dp)
            ) {
                // BUBT Logo Box (.dlogo)
                Box(
                    modifier = Modifier
                        .size(64.dp, 76.dp)
                        .shadow(4.dp, RoundedCornerShape(20.dp), ambientColor = Primary, spotColor = Primary)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White)
                        .border(1.dp, BorderColor, RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.bubt_logo),
                        contentDescription = "BUBT Crest",
                        modifier = Modifier.size(40.dp, 50.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = profile.name,
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TitleText
                )

                Text(
                    text = "Student · ${profile.department}",
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MutedText
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Navigation Items (.dn)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                DrawerNavItem(
                    label = "Candidates",
                    icon = Icons.Default.People,
                    isSelected = selectedTab == 0,
                    onClick = { onTabSelected(0) }
                )
                DrawerNavItem(
                    label = "Class routine",
                    icon = Icons.Default.CalendarToday,
                    isSelected = selectedTab == 1,
                    onClick = { onTabSelected(1) }
                )
                DrawerNavItem(
                    label = "Vote status",
                    icon = Icons.Default.Timer,
                    isSelected = selectedTab == 2,
                    onClick = { onTabSelected(2) }
                )
                DrawerNavItem(
                    label = "My profile",
                    icon = Icons.Default.Person,
                    isSelected = selectedTab == 3,
                    onClick = { onTabSelected(3) }
                )

                Divider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = BorderColor
                )

                DrawerNavItem(
                    label = "Logout account",
                    icon = Icons.Default.Logout,
                    isSelected = false,
                    isDanger = true,
                    onClick = onSignOut
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Footer (.dfoot)
            Text(
                text = "CRConnect · BUBT Department Network",
                fontFamily = PlusJakartaSansFontFamily,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MutedText,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }
    }
}

@Composable
private fun DrawerNavItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    isDanger: Boolean = false
) {
    val bg = when {
        isSelected -> TintLightBlue
        isDanger -> DangerTint
        else -> Color.Transparent
    }
    val contentColor = when {
        isSelected -> Primary
        isDanger -> ErrorColor
        else -> TitleText
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .clickable { onClick() }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (isSelected) Color.White else TintLightBlue),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isDanger) ErrorColor else Primary,
                modifier = Modifier.size(18.dp)
            )
        }

        Text(
            text = label,
            fontFamily = PlusJakartaSansFontFamily,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = contentColor
        )
    }
}
