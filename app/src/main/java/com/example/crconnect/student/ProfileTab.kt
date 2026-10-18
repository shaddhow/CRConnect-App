package com.example.crconnect.student

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import android.graphics.BlurMaskFilter
import android.graphics.Paint
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
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
    val portalGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF70D6FF), // Top (0%)
            Color(0xFFC2EFFF), // Middle (50%)
            Color(0xFFFFFFFF)  // Bottom (100%)
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = portalGradient)
            .verticalScroll(scrollState)
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 16.dp)
    ) {
        // Card 1: User Main Info & Avatar (.card.c)
        DesignCard(padding = 24.dp) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Large Avatar Box with shadow layers and rings
                Box(
                    modifier = Modifier
                        .size(98.dp)
                        .drawBehind {
                            val paint = Paint().apply {
                                isAntiAlias = true
                                color = android.graphics.Color.argb((0.25f * 255).toInt(), 0, 102, 255)
                                maskFilter = BlurMaskFilter(28.dp.toPx(), BlurMaskFilter.Blur.NORMAL)
                            }
                            drawIntoCanvas { canvas ->
                                canvas.nativeCanvas.save()
                                canvas.nativeCanvas.translate(0f, 12.dp.toPx())
                                canvas.nativeCanvas.drawCircle(
                                    center.x,
                                    center.y,
                                    44.dp.toPx(),
                                    paint
                                )
                                canvas.nativeCanvas.restore()
                            }

                            drawCircle(
                                color = Color.White,
                                radius = 49.dp.toPx()
                            )

                            drawCircle(
                                color = Color(0xFFE2E8F0),
                                radius = 46.5.dp.toPx()
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0066FF)),
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
                }

                // Below avatar, center aligned: Name, Email, Department chip
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = profile.name,
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )

                    Text(
                        text = profile.email,
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF64748B)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Surface(
                        modifier = Modifier.padding(top = 2.dp),
                        shape = RoundedCornerShape(999.dp),
                        color = Color(0xFFE6F0FF),
                        border = BorderStroke(1.dp, Color(0xFFBFD7FF))
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Department of CSE, BUBT",
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0066FF)
                            )
                        }
                    }
                }

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

        Spacer(modifier = Modifier.height(16.dp))

        // Card 2: Overview & Progress (.card.c)
        DesignCard(padding = 20.dp) {
            val progress = profile.attendancePct / 100f
            val animatedProgress by animateFloatAsState(
                targetValue = progress,
                animationSpec = tween(durationMillis = 600)
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                KeyValueRow(
                    key = "Verification status",
                    value = "Verified · Locked",
                    valueColor = Color(0xFF16A34A)
                )
                KeyValueRow(
                    key = "Total enrolled courses",
                    value = "6",
                    valueColor = TitleText
                )
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    KeyValueRow(
                        key = "Attendance overview",
                        value = "${profile.attendancePct}% (Excellent)",
                        valueColor = Color(0xFF0066FF)
                    )
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = Color(0xFF0066FF),
                        trackColor = Color(0xFFE2E8F0)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Card 3: Action Buttons (.card.c)
        DesignCard(padding = 20.dp) {
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
                        containerColor = Color(0xFFFEF2F2),
                        contentColor = Color(0xFFB91C1C)
                    ),
                    border = BorderStroke(1.5.dp, Color(0xFFFECACA))
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
