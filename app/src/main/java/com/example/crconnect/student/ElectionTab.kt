package com.example.crconnect.student

import java.util.Locale
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.crconnect.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ElectionTab(
    uiState: StudentPortalUiState,
    onCastVote: (String) -> Unit
) {
    var selectedSubTab by remember { mutableIntStateOf(0) }
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()
    val isDark = LocalIsDarkTheme.current

    LaunchedEffect(pagerState.currentPage) {
        if (selectedSubTab != pagerState.currentPage) {
            selectedSubTab = pagerState.currentPage
        }
    }

    LaunchedEffect(selectedSubTab) {
        if (pagerState.currentPage != selectedSubTab) {
            coroutineScope.launch {
                pagerState.animateScrollToPage(selectedSubTab)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Sleek Glassy Segmented Sub-Tab Switcher (Status & Receipt first, Candidates & Vote second)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .shadow(
                    elevation = if (isDark) 0.dp else 4.dp,
                    shape = RoundedCornerShape(18.dp),
                    ambientColor = Color(0x0F000000),
                    spotColor = Color(0x0F000000)
                ),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sub-Tab 0: Status & Receipt (Now First!)
                val isTab0Selected = selectedSubTab == 0
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isTab0Selected) {
                                if (isDark) Color(0xFF1E293B) else Color(0xFF0066FF)
                            } else Color.Transparent
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            selectedSubTab = 0
                            coroutineScope.launch { pagerState.animateScrollToPage(0) }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Assessment,
                            contentDescription = null,
                            tint = if (isTab0Selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Status & Receipt",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 14.sp,
                            fontWeight = if (isTab0Selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isTab0Selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Sub-Tab 1: Candidates & Vote (Now Second!)
                val isTab1Selected = selectedSubTab == 1
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isTab1Selected) {
                                if (isDark) Color(0xFF1E293B) else Color(0xFF0066FF)
                            } else Color.Transparent
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            selectedSubTab = 1
                            coroutineScope.launch { pagerState.animateScrollToPage(1) }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.HowToVote,
                            contentDescription = null,
                            tint = if (isTab1Selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Candidates & Vote",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 14.sp,
                            fontWeight = if (isTab1Selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isTab1Selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // 2. Swipeable Horizontal Pager
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) { page ->
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(bottom = 84.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (page == 0) {
                    StatusSubTabContent(
                        uiState = uiState,
                        onGoToCandidates = {
                            selectedSubTab = 1
                            coroutineScope.launch { pagerState.animateScrollToPage(1) }
                        }
                    )
                } else {
                    CandidatesSubTabContent(
                        uiState = uiState,
                        onCastVote = onCastVote
                    )
                }
            }
        }
    }
}

@Composable
private fun CandidatesSubTabContent(
    uiState: StudentPortalUiState,
    onCastVote: (String) -> Unit
) {
    val totalVotes = uiState.candidates.sumOf { it.votes }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Gate & Timer Status Banner (Only when voting is closed/locked)
        if (!uiState.isGateOpen) {
            DesignCard(padding = 20.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "Election Gate Closed",
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Voting is currently locked by teacher",
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Timer pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
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
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Live Vote Distribution Card
        DesignCard(padding = 20.dp) {
            Text(
                text = "Live vote distribution",
                fontFamily = PlusJakartaSansFontFamily,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.2).sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Distribution Top Bar Accent Line
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
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

            Spacer(modifier = Modifier.height(16.dp))

            // Candidates Vote Legend
            if (uiState.candidates.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(color)
                                )
                                Text(
                                    text = candidate.name,
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontSize = 15.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Text(
                                text = "${candidate.votes} votes ($percentage%)",
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 14.5.sp,
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
                    fontSize = 14.5.sp,
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
                                    .shadow(elevation = 6.dp, shape = RoundedCornerShape(22.dp), ambientColor = Primary, spotColor = Primary)
                                    .border(1.5.dp, Primary, RoundedCornerShape(22.dp))
                            } else Modifier
                        )
                ) {
                    DesignCard(padding = 20.dp) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = String.format(Locale.getDefault(), "%02d", index + 1),
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = candidate.name,
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.2).sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "${candidate.votes} votes · ${cleanDept(candidate.department)}",
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

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

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
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
    }
}

@Composable
private fun StatusSubTabContent(
    uiState: StudentPortalUiState,
    onGoToCandidates: () -> Unit
) {
    val isDark = LocalIsDarkTheme.current

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (uiState.hasVoted) {
            val bannerBg = if (isDark) Color(0xFF0D3B24) else SuccessTint
            val bannerBorder = if (isDark) Color(0xFF15803D).copy(alpha = 0.5f) else SuccessBorder
            val bannerTitleColor = if (isDark) Color.White else TitleText
            val bannerSubColor = if (isDark) Color(0xFF4ADE80) else SuccessText

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(bannerBg)
                    .border(1.dp, bannerBorder, RoundedCornerShape(18.dp))
                    .padding(20.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF22C55E)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Success",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column {
                    Text(
                        text = "Vote secured & recorded",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = bannerTitleColor,
                        lineHeight = 24.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Status: Vote cast successfully",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = bannerSubColor
                    )
                }
            }

            DesignCard(padding = 20.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Receipt ID",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = uiState.receiptId.ifBlank { "REC-849201" },
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ballot",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Encrypted & anonymous",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFF4ADE80) else SuccessText
                    )
                }

                Spacer(modifier = Modifier.height(22.dp))

                StepItem(title = "Account registered", isCompleted = true)
                Spacer(modifier = Modifier.height(16.dp))
                StepItem(title = "Identity verified", isCompleted = true)
                Spacer(modifier = Modifier.height(16.dp))
                StepItem(title = "Vote cast", isCompleted = true)
            }
        } else {
            val warningBg = if (isDark) Color(0xFF381500) else WarningTint
            val warningBorder = if (isDark) Color(0xFF92400E).copy(alpha = 0.5f) else WarningBorder
            val warningTitleColor = if (isDark) Color.White else TitleText
            val warningSubColor = if (isDark) Color(0xFFF59E0B) else WarningText

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(warningBg)
                    .border(1.dp, warningBorder, RoundedCornerShape(18.dp))
                    .padding(20.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF59E0B)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_clock),
                        contentDescription = "Pending",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column {
                    Text(
                        text = "You have not voted yet",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = warningTitleColor,
                        lineHeight = 24.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Status: Pending your vote",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = warningSubColor
                    )
                }
            }

            DesignCard(padding = 20.dp) {
                StepItem(title = "Account registered", isCompleted = true)
                Spacer(modifier = Modifier.height(16.dp))
                StepItem(title = "Identity verified", isCompleted = true)
                Spacer(modifier = Modifier.height(16.dp))
                StepItem(title = "Vote cast", isCompleted = false)

                Spacer(modifier = Modifier.height(26.dp))

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
                        text = "View candidates & vote",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun StepItem(
    title: String,
    isCompleted: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(if (isCompleted) Color(0xFF22C55E) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            if (isCompleted) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(15.dp)
                )
            }
        }

        Text(
            text = title,
            fontFamily = PlusJakartaSansFontFamily,
            fontSize = 15.5.sp,
            fontWeight = if (isCompleted) FontWeight.Bold else FontWeight.Medium,
            color = if (isCompleted) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun cleanDept(dept: String): String {
    return if (dept.isNotBlank()) {
        if (dept.contains("CSE", ignoreCase = true)) "Dept. of CSE" else dept
    } else {
        "Dept. of CSE"
    }
}
