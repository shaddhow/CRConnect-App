package com.example.crconnect.student

import java.util.Locale
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
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val CHART_COLORS = listOf(
    Color(0xFF6C63FF),
    Color(0xFF0066FF),
    Color(0xFF00C9A7),
    Color(0xFFFF6B6B),
    Color(0xFFF59E0B),
    Color(0xFF8B5CF6)
)

@Composable
fun CandidatesTab(
    uiState: StudentPortalUiState,
    onCastVote: (String) -> Unit
) {
    val scrollState = rememberScrollState()
    val totalVotes = uiState.candidates.sumOf { it.votes }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Card 0: Live Gate & Timer Status Banner
        DesignCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (uiState.isGateOpen) SuccessTint else WarningTint),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (uiState.isGateOpen) Icons.Default.HowToVote else Icons.Default.Lock,
                            contentDescription = null,
                            tint = if (uiState.isGateOpen) SuccessColor else Color(0xFFD97706),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = if (uiState.isGateOpen) "Election Gate Open" else "Election Gate Closed",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TitleText
                        )
                        Text(
                            text = if (uiState.isGateOpen) "Status: ${uiState.timerStatus}" else "Voting is currently locked by teacher",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MutedText
                        )
                    }
                }

                // Timer pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (uiState.isGateOpen) TintLightBlue else Color(0xFFF1F5F9))
                        .border(1.dp, if (uiState.isGateOpen) TintLightBlueBorder else BorderColor, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.HourglassTop,
                            contentDescription = null,
                            tint = if (uiState.isGateOpen) Primary else MutedText,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = uiState.timerText,
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (uiState.isGateOpen) Primary else MutedText
                        )
                    }
                }
            }
        }

        // Card 1: Live Vote Distribution (.card.c.chart)
        DesignCard {
            Text(
                text = "Live vote distribution",
                fontFamily = PlusJakartaSansFontFamily,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.2).sp,
                color = TitleText
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Multi-color progress bar (.dist)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color(0xFFE5E7EB))
            ) {
                if (totalVotes > 0) {
                    uiState.candidates.forEachIndexed { index, candidate ->
                        val weight = candidate.votes.toFloat() / totalVotes
                        if (weight > 0f) {
                            Box(
                                modifier = Modifier
                                    .weight(weight)
                                    .fillMaxSize()
                                    .background(CHART_COLORS[index % CHART_COLORS.size])
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Legend (#slegend) with space-between alignment and 10dp gap
            if (uiState.candidates.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    uiState.candidates.forEachIndexed { index, candidate ->
                        val percentage = if (totalVotes > 0) (candidate.votes * 100) / totalVotes else 0
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(CHART_COLORS[index % CHART_COLORS.size])
                                )
                                Text(
                                    text = candidate.name,
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TitleText
                                )
                            }

                            Text(
                                text = "${candidate.votes} votes ($percentage%)",
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = MutedText
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = "No candidates registered yet.",
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MutedText
                )
            }
        }

        // Candidates Cards Stack (#scands) - 16dp spacing
        if (uiState.candidates.isNotEmpty()) {
            uiState.candidates.forEachIndexed { index, candidate ->
                val percentage = if (totalVotes > 0) (candidate.votes * 100) / totalVotes else 0
                val isVotedForThis = uiState.hasVoted && uiState.votedCandidateId == candidate.id
                val candidateColor = CHART_COLORS[index % CHART_COLORS.size]

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (isVotedForThis) {
                                Modifier
                                    .shadow(elevation = 6.dp, shape = RoundedCornerShape(20.dp), ambientColor = Primary, spotColor = Primary)
                                    .border(1.5.dp, Primary, RoundedCornerShape(20.dp))
                            } else Modifier
                        )
                ) {
                    DesignCard {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Enlarged 48x48 Number Badge (.num)
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(TintLightBlue),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = String.format(Locale.getDefault(), "%02d", index + 1),
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Primary
                                )
                            }

                            // Candidate Name & Details (.nm)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = candidate.name,
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = (-0.2).sp,
                                    color = TitleText
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "${candidate.votes} votes · ${candidate.department}",
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MutedText
                                )
                            }

                            // Vibrant Blue Vote Button with pure white text (#FFFFFF)
                            if (isVotedForThis) {
                                Button(
                                    onClick = { },
                                    enabled = false,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        disabledContainerColor = SuccessTint,
                                        disabledContentColor = SuccessText
                                    ),
                                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = SolidColor(SuccessBorder)),
                                    modifier = Modifier
                                        .height(44.dp)
                                        .padding(horizontal = 4.dp)
                                ) {
                                    Text(
                                        text = "Voted",
                                        fontFamily = PlusJakartaSansFontFamily,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else {
                                Button(
                                    onClick = { onCastVote(candidate.id) },
                                    enabled = !uiState.hasVoted && uiState.isGateOpen,
                                    shape = RoundedCornerShape(12.dp),
                                    elevation = ButtonDefaults.buttonElevation(
                                        defaultElevation = 4.dp,
                                        pressedElevation = 2.dp
                                    ),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF0062FF),
                                        contentColor = Color.White,
                                        disabledContainerColor = DisabledBg,
                                        disabledContentColor = DisabledText
                                    ),
                                    modifier = Modifier
                                        .height(44.dp)
                                        .padding(horizontal = 4.dp)
                                ) {
                                    Text(
                                        text = if (!uiState.isGateOpen) "Locked" else "Vote",
                                        fontFamily = PlusJakartaSansFontFamily,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Individual candidate progress bar with assigned color & 6dp track height
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color(0xFFE5E7EB))
                        ) {
                            if (percentage > 0) {
                                Box(
                                    modifier = Modifier
                                        .weight(percentage.toFloat())
                                        .fillMaxSize()
                                        .background(candidateColor)
                                )
                            } else if (isVotedForThis) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxSize()
                                        .background(candidateColor)
                                )
                            }
                            if (100 - percentage > 0 && !isVotedForThis) {
                                Box(
                                    modifier = Modifier
                                        .weight((100 - percentage).toFloat())
                                        .fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
