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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.crconnect.R

@Composable
fun StatusTab(
    uiState: StudentPortalUiState,
    onGoToCandidates: () -> Unit
) {
    val scrollState = rememberScrollState()
    val isDark = LocalIsDarkTheme.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (uiState.hasVoted) {
            // Success Banner (.banner)
            val bannerBg = if (isDark) Color(0xFF0D3B24) else SuccessTint
            val bannerBorder = if (isDark) Color(0xFF15803D).copy(alpha = 0.5f) else SuccessBorder
            val bannerTitleColor = if (isDark) Color.White else TitleText
            val bannerSubColor = if (isDark) Color(0xFF4ADE80) else SuccessText

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(bannerBg)
                    .border(1.dp, bannerBorder, RoundedCornerShape(16.dp))
                    .padding(18.dp)
            ) {
                // Circle Icon Box (.ib)
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF22C55E)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Success",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column {
                    Text(
                        text = "Vote secured & recorded",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = bannerTitleColor,
                        lineHeight = 22.sp
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "Status: Vote cast",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = bannerSubColor
                    )
                }
            }

            // Receipt Card (.card.c)
            DesignCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Receipt ID",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = uiState.receiptId.ifBlank { "REC-849201" },
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ballot",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Encrypted & anonymous",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFF4ADE80) else SuccessText
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Steps (.step)
                StepItem(title = "Account registered", isCompleted = true)
                Spacer(modifier = Modifier.height(14.dp))
                StepItem(title = "Identity verified", isCompleted = true)
                Spacer(modifier = Modifier.height(14.dp))
                StepItem(title = "Vote cast", isCompleted = true)
            }
        } else {
            // Warning Banner (.banner.w)
            val warningBg = if (isDark) Color(0xFF381500) else WarningTint
            val warningBorder = if (isDark) Color(0xFF92400E).copy(alpha = 0.5f) else WarningBorder
            val warningTitleColor = if (isDark) Color.White else TitleText
            val warningSubColor = if (isDark) Color(0xFFF59E0B) else WarningText

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(warningBg)
                    .border(1.dp, warningBorder, RoundedCornerShape(16.dp))
                    .padding(18.dp)
            ) {
                // Circle Icon Box (.ib)
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF59E0B)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_clock),
                        contentDescription = "Pending",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column {
                    Text(
                        text = "You have not voted yet",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = warningTitleColor,
                        lineHeight = 22.sp
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "Status: Pending",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = warningSubColor
                    )
                }
            }

            // Steps Card (.card.c)
            DesignCard {
                StepItem(title = "Account registered", isCompleted = true)
                Spacer(modifier = Modifier.height(14.dp))
                StepItem(title = "Identity verified", isCompleted = true)
                Spacer(modifier = Modifier.height(14.dp))
                StepItem(title = "Vote cast", isCompleted = false)

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onGoToCandidates,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF3B82F6),
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "Go to candidates",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(84.dp))
    }
}

@Composable
private fun StepItem(
    title: String,
    isCompleted: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (isCompleted) Color(0xFF22C55E) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            if (isCompleted) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        Text(
            text = title,
            fontFamily = PlusJakartaSansFontFamily,
            fontSize = 15.sp,
            fontWeight = if (isCompleted) FontWeight.Bold else FontWeight.Medium,
            color = if (isCompleted) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
