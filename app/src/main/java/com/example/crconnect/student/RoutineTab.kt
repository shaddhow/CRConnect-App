package com.example.crconnect.student

import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.text.SimpleDateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineTab(
    uiState: StudentPortalUiState,
    onDaySelected: (Int) -> Unit,
    onOpenAddClass: () -> Unit,
    onCloseAddClass: () -> Unit,
    onAddClass: (courseName: String, timeSlot: String, room: String) -> Unit,
    onShowToast: (String) -> Unit
) {
    val scrollState = rememberScrollState()
    val selectedDay = uiState.scheduleDays.find { it.dayIndex == uiState.selectedDayIndex }
        ?: uiState.scheduleDays.last() // Default to Friday (index 6) to match HTML
    val classesForSelectedDay = uiState.classMap[uiState.selectedDayIndex] ?: emptyList()

    val greeting = remember {
        val hour = Calendar.getInstance()[Calendar.HOUR_OF_DAY]
        when {
            hour < 12 -> "Good morning"
            hour < 17 -> "Good day"
            else -> "Winding down for the day"
        }
    }

    val todayDateFormatted = remember {
        SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(Date())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Bar Header Row (.ct)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Calendar Icon Box (.ib)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(TintLightBlue),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = "Routine",
                    tint = Primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Greeting & Date Column (.who)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    text = greeting,
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TitleText
                )
                Text(
                    text = todayDateFormatted,
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MutedText
                )
            }

            // Add FAB Button (.fab)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .shadow(elevation = 4.dp, shape = CircleShape, ambientColor = Color(0x660066FF), spotColor = Color(0x660066FF))
                    .clip(CircleShape)
                    .background(Primary)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onOpenAddClass() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add class",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // Week Card (.card.c)
        DesignCard(padding = 24.dp) {
            // Header Row: "This week · 10–16 Oct" + square buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "This week · 10–16 Oct",
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TitleText
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Previous Week Square Button (.rbtn.sq)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White)
                            .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "Previous week",
                            tint = TitleText,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Next Week Square Button (.rbtn.sq)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White)
                            .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Next week",
                            tint = TitleText,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Day Selector Pills (.days) - Equally spaced 7-day strip across screen width (SAT-FRI)
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                uiState.scheduleDays.forEach { day ->
                    DayPill(
                        day = day,
                        isSelected = day.dayIndex == uiState.selectedDayIndex,
                        onClick = { onDaySelected(day.dayIndex) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Section Header Row (.ct)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${getDayFullName(selectedDay.shortName)} classes",
                fontFamily = PlusJakartaSansFontFamily,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TitleText
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (classesForSelectedDay.isNotEmpty()) TintLightBlue else Color(0xFFF1F5F9))
                    .border(1.dp, if (classesForSelectedDay.isNotEmpty()) TintLightBlueBorder else BorderColor, RoundedCornerShape(999.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (classesForSelectedDay.isNotEmpty()) "${classesForSelectedDay.size} ${if (classesForSelectedDay.size == 1) "class" else "classes"}" else "No classes",
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (classesForSelectedDay.isNotEmpty()) Primary else MutedText
                )
            }
        }

        // Class List or Empty State (scrolls smoothly with parent)
        if (classesForSelectedDay.isNotEmpty()) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                classesForSelectedDay.forEach { classModel ->
                    ClassCardItem(classModel = classModel)
                }
            }
        } else {
            DesignCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Circle Icon Box (.ib)
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(TintLightBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    Text(
                        text = "Nothing on ${getDayFullName(selectedDay.shortName)}",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TitleText
                    )

                    Text(
                        text = "Tap another day, or add a class to this one.",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MutedText,
                        textAlign = TextAlign.Center
                    )

                    PrimaryButton(
                        text = "+ Add a class on ${getDayShortTitle(selectedDay.shortName)}",
                        onClick = onOpenAddClass
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    // Add Class Bottom Sheet (#rsheet)
    if (uiState.isAddClassSheetOpen) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = onCloseAddClass,
            sheetState = sheetState,
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            windowInsets = WindowInsets(0, 0, 0, 0),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp, bottom = 4.dp)
                        .size(width = 40.dp, height = 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFFCBD5E1))
                )
            }
        ) {
            AddClassBottomSheetContent(
                selectedDayName = getDayFullName(selectedDay.shortName),
                onAddClass = onAddClass,
                onShowToast = onShowToast
            )
        }
    }
}

@Composable
private fun DayPill(
    day: DayModel,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(64.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) Primary else Color.White)
            .border(1.dp, if (isSelected) Primary else BorderColor, RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = day.shortName,
                fontFamily = PlusJakartaSansFontFamily,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color.White else MutedText
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = day.dateNumber.toString(),
                fontFamily = PlusJakartaSansFontFamily,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color.White else TitleText
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            day.hasClasses && isSelected -> Color.White
                            day.hasClasses -> Primary
                            else -> Color.Transparent
                        }
                    )
            )
        }
    }
}

@Composable
private fun ClassCardItem(classModel: ClassModel) {
    DesignCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Left Time Slot Badge (.tm)
            Box(
                modifier = Modifier
                    .width(72.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(TintLightBlue)
                    .padding(vertical = 10.dp, horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = classModel.timeSlot.replace(" - ", "\n"),
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Primary,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )
            }

            // Right Course Name & Room (.nm)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = classModel.courseName,
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TitleText,
                    lineHeight = 22.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = classModel.room,
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MutedText
                )
            }
        }
    }
}

@Composable
private fun AddClassBottomSheetContent(
    selectedDayName: String,
    onAddClass: (courseName: String, timeSlot: String, room: String) -> Unit,
    onShowToast: (String) -> Unit
) {
    var isManualMode by remember { mutableStateOf(false) }
    var courseName by remember { mutableStateOf("") }
    var timeSlot by remember { mutableStateOf("") }
    var room by remember { mutableStateOf("") }
    val sheetScrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(sheetScrollState)
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (!isManualMode) {
            // Scan Mode (.shero)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Primary)
                    .padding(18.dp)
            ) {
                Text(
                    text = "Photo in, routine out",
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Point at your printed routine or pick a screenshot. Reads on your phone. Nothing is uploaded.",
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.92f),
                    lineHeight = 20.sp
                )
            }

            PrimaryButton(
                text = "Scan with camera",
                onClick = { },
                icon = Icons.Default.CameraAlt
            )

            SecondaryButton(
                text = "Choose from gallery",
                onClick = { },
                icon = Icons.Default.Image
            )

            Text(
                text = "Scanning tips",
                fontFamily = PlusJakartaSansFontFamily,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MutedText
            )

            // Tips 2x2 grid (.tips)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TipChip("Good light", Icons.Default.WbSunny, Modifier.weight(1f))
                    TipChip("Fit whole routine", Icons.Default.Crop, Modifier.weight(1f))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TipChip("Screenshots from PDFs", Icons.Default.Description, Modifier.weight(1f))
                    TipChip("Review before added", Icons.Default.RemoveRedEye, Modifier.weight(1f))
                }
            }

            TextLink(
                text = "Or add classes manually",
                onClick = { isManualMode = true },
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            // Manual Entry Mode
            Text(
                text = "Add a class on $selectedDayName",
                fontFamily = PlusJakartaSansFontFamily,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TitleText
            )

            OutlinedTextField(
                value = courseName,
                onValueChange = { courseName = it },
                label = { Text("Course Name & Code") },
                placeholder = { Text("e.g. CSE 301 · Algorithms") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = InputBorderColor
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = timeSlot,
                    onValueChange = { timeSlot = it },
                    label = { Text("Time") },
                    placeholder = { Text("09:00 - 10:30") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = InputBorderColor
                    )
                )

                OutlinedTextField(
                    value = room,
                    onValueChange = { room = it },
                    label = { Text("Room") },
                    placeholder = { Text("Room 402") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = InputBorderColor
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            PrimaryButton(
                text = "Add class",
                onClick = {
                    if (courseName.isNotBlank()) {
                        onAddClass(courseName, timeSlot, room)
                    }
                }
            )

            TextLink(
                text = "Back",
                onClick = { isManualMode = false },
                color = MutedText,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun TipChip(
    text: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color.White)
            .border(1.dp, BorderColor, RoundedCornerShape(999.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Primary,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = text,
            fontFamily = PlusJakartaSansFontFamily,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = TitleText
        )
    }
}

private fun getDayFullName(shortName: String): String {
    return when (shortName) {
        "SAT" -> "Saturday"
        "SUN" -> "Sunday"
        "MON" -> "Monday"
        "TUE" -> "Tuesday"
        "WED" -> "Wednesday"
        "THU" -> "Thursday"
        "FRI" -> "Friday"
        else -> shortName
    }
}

private fun getDayShortTitle(shortName: String): String {
    return when (shortName) {
        "SAT" -> "Sat"
        "SUN" -> "Sun"
        "MON" -> "Mon"
        "TUE" -> "Tue"
        "WED" -> "Wed"
        "THU" -> "Thu"
        "FRI" -> "Fri"
        else -> shortName
    }
}

@Preview(showBackground = true)
@Composable
fun RoutineTabPreview() {
    StudentPortalTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF70D6FF),
                            Color(0xFFC2EFFF),
                            Color(0xFFFFFFFF)
                        )
                    )
                )
        ) {
            RoutineTab(
                uiState = StudentPortalUiState(selectedTab = 1, selectedDayIndex = 6),
                onDaySelected = {},
                onOpenAddClass = {},
                onCloseAddClass = {},
                onAddClass = { _, _, _ -> },
                onShowToast = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RoutineTabWithSheetPreview() {
    StudentPortalTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF70D6FF),
                            Color(0xFFC2EFFF),
                            Color(0xFFFFFFFF)
                        )
                    )
                )
        ) {
            RoutineTab(
                uiState = StudentPortalUiState(selectedTab = 1, selectedDayIndex = 6, isAddClassSheetOpen = true),
                onDaySelected = {},
                onOpenAddClass = {},
                onCloseAddClass = {},
                onAddClass = { _, _, _ -> },
                onShowToast = {}
            )
        }
    }
}
