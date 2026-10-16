package com.example.crconnect.student

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ProfileTab(
    profile: UserProfileModel,
    onEditProfilePicture: () -> Unit,
    onSecuritySettings: () -> Unit,
    onSignOut: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Card 1: User Main Info & Avatar (.card.c)
        DesignCard {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Large Avatar Circle (.pav)
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .shadow(elevation = 8.dp, shape = CircleShape, ambientColor = Primary, spotColor = Primary)
                        .clip(CircleShape)
                        .background(Primary)
                        .border(4.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = profile.initials,
                        color = Color.White,
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = profile.name,
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TitleText
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = profile.email,
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MutedText
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Department Chip (.chip)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(TintLightBlue)
                        .border(1.dp, TintLightBlueBorder, RoundedCornerShape(999.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Department of CSE, BUBT",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Primary
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Info Tiles Row (.tiles)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    InfoTile(
                        label = "Student ID",
                        value = profile.studentId,
                        modifier = Modifier.weight(1f)
                    )
                    InfoTile(
                        label = "Intake",
                        value = profile.intake,
                        modifier = Modifier.weight(1f)
                    )
                    InfoTile(
                        label = "Section",
                        value = profile.section,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Card 2: Overview & Progress (.card.c)
        DesignCard {
            KeyValueRow(key = "Verification status", value = "Verified · Locked", valueColor = SuccessText)
            Spacer(modifier = Modifier.height(12.dp))
            KeyValueRow(key = "Total enrolled courses", value = "6", valueColor = TitleText)
            Spacer(modifier = Modifier.height(12.dp))
            KeyValueRow(key = "Attendance overview", value = "${profile.attendancePct}% (Excellent)", valueColor = Primary)

            Spacer(modifier = Modifier.height(12.dp))

            // Attendance Progress Bar (.bar2)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(BorderColor)
            ) {
                Box(
                    modifier = Modifier
                        .weight(profile.attendancePct.toFloat())
                        .fillMaxSize()
                        .background(Primary)
                )
                if (100 - profile.attendancePct > 0) {
                    Box(
                        modifier = Modifier
                            .weight((100 - profile.attendancePct).toFloat())
                            .fillMaxSize()
                    )
                }
            }
        }

        // Card 3: Action Buttons (.card.c)
        DesignCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SecondaryButton(
                    text = "Edit profile picture",
                    onClick = onEditProfilePicture
                )

                SecondaryButton(
                    text = "Security settings",
                    onClick = onSecuritySettings
                )

                // Log out Danger Button (.btn.danger)
                Button(
                    onClick = onSignOut,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DangerTint,
                        contentColor = ErrorColor
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = SolidColor(DangerBorder))
                ) {
                    Text(
                        text = "Log out",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun InfoTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFF8FAFC))
            .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
            .padding(vertical = 12.dp, horizontal = 6.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                fontFamily = PlusJakartaSansFontFamily,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MutedText,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontFamily = PlusJakartaSansFontFamily,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TitleText,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun KeyValueRow(
    key: String,
    value: String,
    valueColor: Color = TitleText
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = key,
            fontFamily = PlusJakartaSansFontFamily,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MutedText
        )
        Text(
            text = value,
            fontFamily = PlusJakartaSansFontFamily,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}
