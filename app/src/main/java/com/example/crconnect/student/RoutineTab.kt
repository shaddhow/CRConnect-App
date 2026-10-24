package com.example.crconnect.student

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.text.SimpleDateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.example.crconnect.RoutineCameraActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineTab(
    uiState: StudentPortalUiState,
    onDaySelected: (Int) -> Unit,
    onOpenAddClass: () -> Unit,
    onCloseAddClass: () -> Unit,
    onAddClass: (courseName: String, timeSlot: String, room: String) -> Unit,
    onDeleteRoutine: () -> Unit = {},
    onScannedEntriesAdded: (List<ClassEntry>) -> Unit = { _ -> },
    onShowToast: (String) -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val selectedDay = uiState.scheduleDays.find { it.dayIndex == uiState.selectedDayIndex }
        ?: uiState.scheduleDays.last()
    val classesForSelectedDay = (uiState.classMap[uiState.selectedDayIndex] ?: emptyList())
        .sortedBy { startMinutes(it.timeSlot) }

    var isOcrLoading by remember { mutableStateOf(false) }

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

    fun processSelectedImage(uri: Uri) {
        onCloseAddClass()
        isOcrLoading = true
        RoutineOcrParser.processOcrImage(
            context = context,
            imageUri = uri,
            onSuccess = { entries ->
                isOcrLoading = false
                if (entries.isNotEmpty()) {
                    onScannedEntriesAdded(entries)
                    onShowToast("Routine processed successfully! (${entries.size} classes found)")
                } else {
                    onShowToast("No classes detected in image. Please try a clearer photo.")
                }
            },
            onFailure = { e ->
                isOcrLoading = false
                onShowToast("Failed to process routine image: ${e.message}")
            }
        )
    }

    val cameraActivityLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val capturedUriString = result.data?.getStringExtra(RoutineCameraActivity.EXTRA_CAPTURED_URI)
                ?: result.data?.dataString
            if (!capturedUriString.isNullOrEmpty()) {
                processSelectedImage(Uri.parse(capturedUriString))
            } else {
                val photoUri = result.data?.data
                if (photoUri != null) {
                    processSelectedImage(photoUri)
                }
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val intent = Intent(context, RoutineCameraActivity::class.java)
            cameraActivityLauncher.launch(intent)
        } else {
            onShowToast("Camera permission is required to scan routine")
        }
    }

    val galleryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            processSelectedImage(uri)
        }
    }

    val storagePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            galleryPickerLauncher.launch("image/*")
        } else {
            galleryPickerLauncher.launch("image/*")
        }
    }

    val onScanCameraClick = {
        val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            val intent = Intent(context, RoutineCameraActivity::class.java)
            cameraActivityLauncher.launch(intent)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val onChooseGalleryClick = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES)
            if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                galleryPickerLauncher.launch("image/*")
            } else {
                storagePermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
            }
        } else {
            val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
            if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                galleryPickerLauncher.launch("image/*")
            } else {
                storagePermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = uiState.weekLabel,
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TitleText
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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

        // Class List or Empty State
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
                onScanCamera = onScanCameraClick,
                onChooseGallery = onChooseGalleryClick,
                onDeleteRoutine = {
                    onDeleteRoutine()
                    onCloseAddClass()
                },
                onShowToast = onShowToast
            )
        }
    }

    // OCR Loading Spinner Modal Dialog ("Reading your routine...")
    if (isOcrLoading) {
        Dialog(onDismissRequest = { }) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
                    .padding(28.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(
                        color = Primary,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "Reading your routine...",
                        fontFamily = PlusJakartaSansFontFamily,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TitleText,
                        textAlign = TextAlign.Center
                    )
                }
            }
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
private fun ClassCardItem(
    classModel: ClassModel,
    onAttendanceChanged: (String?) -> Unit = {}
) {
    var attendanceState by remember(classModel.id) { mutableStateOf(classModel.attendanceState) }

    DesignCard {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Left Time Box (.tm)
            Box(
                modifier = Modifier
                    .width(72.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(TintLightBlue)
                    .padding(vertical = 10.dp, horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = formatTimeSlotDisplay(classModel.timeSlot),
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Primary,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )
            }

            // Right Course Details (.nm)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Course Title
                Text(
                    text = classModel.courseName,
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TitleText,
                    lineHeight = 22.sp
                )

                // Room Subtitle
                Text(
                    text = if (classModel.room.startsWith("Room ", ignoreCase = true)) classModel.room else "Room ${classModel.room}",
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MutedText
                )

                // Pills Row (.pill-row) - Room Pill & Duration Pill
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    // Room Pill
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(TintLightBlue)
                            .border(1.dp, TintLightBlueBorder, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = extractCleanRoomNumber(classModel.room),
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Primary
                        )
                    }

                    // Duration Pill
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(TintLightBlue)
                            .border(1.dp, TintLightBlueBorder, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = calculateDurationText(classModel.timeSlot),
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Primary
                        )
                    }
                }

                // Attended / Missed Action Row (.att-row)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 6.dp)
                ) {
                    val isAttended = attendanceState == "att"
                    val isMissed = attendanceState == "mis"

                    // Attended Button (.att-btn.att)
                    Row(
                        modifier = Modifier
                            .height(30.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isAttended) Color(0xFFDCFCE7) else Color.Transparent)
                            .border(
                                width = 1.5.dp,
                                color = if (isAttended) Color(0xFF4ADE80) else Color(0xFF86EFAC),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                val newState = if (isAttended) null else "att"
                                attendanceState = newState
                                onAttendanceChanged(newState)
                            }
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Attended",
                            tint = if (isAttended) Color(0xFF166534) else Color(0xFF16A34A),
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = "Attended",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isAttended) Color(0xFF166534) else Color(0xFF16A34A)
                        )
                    }

                    // Missed Button (.att-btn.mis)
                    Row(
                        modifier = Modifier
                            .height(30.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isMissed) Color(0xFFFEE2E2) else Color.Transparent)
                            .border(
                                width = 1.5.dp,
                                color = if (isMissed) Color(0xFFF87171) else Color(0xFFFCA5A5),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                val newState = if (isMissed) null else "mis"
                                attendanceState = newState
                                onAttendanceChanged(newState)
                            }
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Missed",
                            tint = if (isMissed) Color(0xFF991B1B) else Color(0xFFDC2626),
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = "Missed",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isMissed) Color(0xFF991B1B) else Color(0xFFDC2626)
                        )
                    }
                }
            }
        }
    }
}

private fun formatTimeSlotDisplay(timeSlot: String): String {
    if (timeSlot.contains(" - ")) return timeSlot.replace(" - ", "\n")
    if (timeSlot.contains("–")) return timeSlot.replace("–", "\n")
    return timeSlot
}

private fun extractCleanRoomNumber(room: String): String {
    val clean = room.replace("Room", "", ignoreCase = true).replace("ROOM", "", ignoreCase = true).trim()
    return clean.ifEmpty { "TBA" }
}

private fun calculateDurationText(timeSlot: String): String {
    return "1h 30m"
}

@Composable
private fun AddClassBottomSheetContent(
    selectedDayName: String,
    onAddClass: (courseName: String, timeSlot: String, room: String) -> Unit,
    onScanCamera: () -> Unit,
    onChooseGallery: () -> Unit,
    onDeleteRoutine: () -> Unit,
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
                onClick = onScanCamera,
                icon = Icons.Default.CameraAlt
            )

            SecondaryButton(
                text = "Choose from gallery",
                onClick = onChooseGallery,
                icon = Icons.Default.Image
            )

            // Delete Current Routine Option
            OutlinedButton(
                onClick = onDeleteRoutine,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.5.dp, Color(0xFFEF4444)),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFFDC2626)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete routine",
                    modifier = Modifier.size(20.dp),
                    tint = Color(0xFFDC2626)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Delete current routine",
                    fontFamily = PlusJakartaSansFontFamily,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFDC2626)
                )
            }

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

@Composable
private fun ReviewRoutineBottomSheetContent(
    scannedEntries: List<ClassEntry>,
    onSaveRoutine: () -> Unit,
    onCancel: () -> Unit
) {
    val sheetScrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(sheetScrollState)
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Review Scanned Routine",
            fontFamily = PlusJakartaSansFontFamily,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TitleText
        )
        Text(
            text = "${scannedEntries.size} classes detected. Review your schedule before saving.",
            fontFamily = PlusJakartaSansFontFamily,
            fontSize = 13.sp,
            color = MutedText
        )

        Spacer(modifier = Modifier.height(4.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            scannedEntries.forEach { entry ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(TintLightBlue)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Primary)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = entry.day.uppercase(),
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Text(
                                text = entry.course,
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TitleText
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${entry.startTime} - ${entry.endTime}  •  ${if (entry.room.startsWith("Room")) entry.room else "Room ${entry.room}"}  •  FC: ${entry.faculty}",
                            fontFamily = PlusJakartaSansFontFamily,
                            fontSize = 12.sp,
                            color = MutedText
                        )
                    }
                    if (entry.needsReview) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFFEF3C7))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Needs Review",
                                fontFamily = PlusJakartaSansFontFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFD97706)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        PrimaryButton(
            text = "Save routine",
            onClick = onSaveRoutine,
            icon = Icons.Default.Check
        )

        SecondaryButton(
            text = "Discard",
            onClick = onCancel
        )
    }
}
