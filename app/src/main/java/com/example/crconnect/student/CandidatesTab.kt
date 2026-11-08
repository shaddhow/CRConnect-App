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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
    Color(0xFF3B82F6),
    Color(0xFFA855F7),
    Color(0xFF22C55E),
    Color(0xFFF59E0B),
    Color(0xFFEC4899),
    Color(0xFF6366F1)
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
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Gate & Timer Status Banner (Only when voting is closed/locked)
        if (!uiState.isGateOpen) {
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
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "Election Gate Closed",
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Voting is currently locked by teacher",
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Timer pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.HourglassTop,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = uiState.timerText,
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Card 1: Live Vote Distribution
        DesignCard {
            Text(
                text = "Live vote distribution",
                fontFamily = PlusJakartaSansFontFamily,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.2).sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Distribution Top Bar Accent Line
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFF3B82F6))
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

            // Candidates Vote Legend
            if (uiState.candidates.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    uiState.candidates.forEachIndexed { index, candidate ->
                        val percentage = if (totalVotes > 0) (candidate.votes * 100) / totalVotes else 0
                        val color = CHART_COLORS[index % CHART_COLORS.size]
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
                                        .background(color)
                                )
                                Text(
                                    text = candidate.name,
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Text(
                                text = "${candidate.votes} votes ($percentage%)",
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Candidate Cards Stack
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
                            // Enlarged Number Badge (.num)
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = String.format(Locale.getDefault(), "%02d", index + 1),
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            // Candidate Name & Details
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = candidate.name,
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.2).sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${candidate.votes} votes · ${cleanDept(candidate.department)}",
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Vote Button
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
                                        .height(42.dp)
                                        .padding(horizontal = 4.dp)
                                ) {
                                    Text(
                                        text = "Voted",
                                        fontFamily = PlusJakartaSansFontFamily,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else {
                                Button(
                                    onClick = { onCastVote(candidate.id) },
                                    enabled = !uiState.hasVoted && uiState.isGateOpen,
                                    shape = RoundedCornerShape(12.dp),
                                    elevation = ButtonDefaults.buttonElevation(
                                        defaultElevation = 2.dp,
                                        pressedElevation = 1.dp
                                    ),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF3B82F6),
                                        contentColor = Color.White,
                                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier
                                        .height(42.dp)
                                        .padding(horizontal = 4.dp)
                                ) {
                                    Text(
                                        text = if (!uiState.isGateOpen) "Locked" else "Vote",
                                        fontFamily = PlusJakartaSansFontFamily,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (!uiState.hasVoted && uiState.isGateOpen) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Individual candidate progress bar line
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        ) {
                            if (percentage > 0) {
                                Box(
                                    modifier = Modifier
                                        .weight(percentage.toFloat())
                                        .fillMaxSize()
                                        .background(candidateColor)
                                )
                            } else if (index == 0 || isVotedForThis) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxSize()
                                        .background(candidateColor)
                                )
                            }
                            if (100 - percentage > 0 && !(index == 0 || isVotedForThis)) {
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

        Spacer(modifier = Modifier.height(84.dp))
    }
}

private fun cleanDept(dept: String): String {
    if (dept.isBlank()) return "Dept. of CSE"
    if (dept.contains("CSE", ignoreCase = true)) return "Dept. of CSE"
    return dept.replace("Department of", "Dept. of", ignoreCase = true)
}
