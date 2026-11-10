package com.example.crconnect.student

import com.example.crconnect.SessionManager
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class CandidateModel(
    val id: String,
    val name: String,
    val department: String = "Dept. of CSE",
    val votes: Int = 0,
    val manifesto: String = "",
    val photoUrl: String = ""
)

data class DayModel(
    val dayIndex: Int,
    val shortName: String,
    val dateNumber: Int,
    val hasClasses: Boolean
)

data class ClassModel(
    val id: String,
    val timeSlot: String,
    val courseName: String,
    val room: String,
    val attendanceState: String? = null
)

enum class ClassTimingStatus {
    LIVE_NOW,
    PAST,
    FUTURE
}

fun endMinutes(timeSlot: String): Int {
    val parts = timeSlot.split("-", "–")
    if (parts.size < 2) return startMinutes(timeSlot) + 90
    val parsed = startMinutes(parts[1].trim())
    return if (parsed > 0) parsed else startMinutes(timeSlot) + 90
}

fun getClassTimingStatus(dayIndex: Int, timeSlot: String): ClassTimingStatus {
    val cal = Calendar.getInstance()
    val todayIdx = when (cal.get(Calendar.DAY_OF_WEEK)) {
        Calendar.SATURDAY -> 0
        Calendar.SUNDAY -> 1
        Calendar.MONDAY -> 2
        Calendar.TUESDAY -> 3
        Calendar.WEDNESDAY -> 4
        Calendar.THURSDAY -> 5
        Calendar.FRIDAY -> 6
        else -> 0
    }

    if (dayIndex < todayIdx) {
        return ClassTimingStatus.PAST
    } else if (dayIndex > todayIdx) {
        return ClassTimingStatus.FUTURE
    } else {
        val currentMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val startMins = startMinutes(timeSlot)
        val endMins = endMinutes(timeSlot)

        return when {
            currentMinutes < startMins -> ClassTimingStatus.FUTURE
            currentMinutes > endMins -> ClassTimingStatus.PAST
            else -> ClassTimingStatus.LIVE_NOW
        }
    }
}

fun getEffectiveAttendanceState(dayIndex: Int, classModel: ClassModel): String? {
    if (classModel.attendanceState == "attended") {
        return "attended"
    }
    val status = getClassTimingStatus(dayIndex, classModel.timeSlot)
    if (status == ClassTimingStatus.PAST) {
        return "missed"
    }
    return classModel.attendanceState
}

data class UserProfileModel(
    val name: String = "Student",
    val email: String = "student@bubt.edu.bd",
    val studentId: String = "--",
    val intake: String = "--",
    val section: String = "--",
    val department: String = "Dept. of CSE",
    val attendancePct: Int = 92,
    val cgpa: String = "--",
    val initials: String = "ST",
    val profileImageUrl: String = "",
    val isCr: Boolean = false,
    val advisorName: String = "Nasrin Akter",
    val advisorEmail: String = "nasrinshipa@bubt.edu.bd",
    val currentTerm: String = "--"
)

fun computeInitials(name: String): String {
    val cleanName = name.trim()
    if (cleanName.isBlank()) return "ST"
    val parts = cleanName.split("\\s+".toRegex()).filter { it.isNotBlank() }
    return when {
        parts.size >= 2 -> "${parts[0].first()}${parts[1].first()}".uppercase(Locale.getDefault())
        parts.size == 1 && parts[0].length >= 2 -> parts[0].take(2).uppercase(Locale.getDefault())
        parts.size == 1 && parts[0].length == 1 -> parts[0].uppercase(Locale.getDefault())
        else -> "ST"
    }
}

fun getTodayDayIndex(): Int {
    val dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
    return when (dayOfWeek) {
        Calendar.SATURDAY -> 0
        Calendar.SUNDAY -> 1
        Calendar.MONDAY -> 2
        Calendar.TUESDAY -> 3
        Calendar.WEDNESDAY -> 4
        Calendar.THURSDAY -> 5
        Calendar.FRIDAY -> 6
        else -> 6
    }
}

fun calculateCurrentWeekDays(weekOffset: Int = 0): Pair<String, List<DayModel>> {
    val cal = Calendar.getInstance()
    if (weekOffset != 0) {
        cal.add(Calendar.WEEK_OF_YEAR, weekOffset)
    }
    val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
    val daysFromSaturday = when (dayOfWeek) {
        Calendar.SATURDAY -> 0
        Calendar.SUNDAY -> 1
        Calendar.MONDAY -> 2
        Calendar.TUESDAY -> 3
        Calendar.WEDNESDAY -> 4
        Calendar.THURSDAY -> 5
        Calendar.FRIDAY -> 6
        else -> 0
    }
    cal.add(Calendar.DAY_OF_YEAR, -daysFromSaturday)

    val shortNames = listOf("SAT", "SUN", "MON", "TUE", "WED", "THU", "FRI")
    val daysList = mutableListOf<DayModel>()
    var startDayNum = 0
    var endDayNum = 0
    var monthStr = ""

    val dateFormat = SimpleDateFormat("MMM", Locale.US)

    for (i in 0..6) {
        val dateNum = cal.get(Calendar.DAY_OF_MONTH)
        if (i == 0) {
            startDayNum = dateNum
            monthStr = dateFormat.format(cal.time)
        }
        if (i == 6) {
            endDayNum = dateNum
        }
        daysList.add(
            DayModel(
                dayIndex = i,
                shortName = shortNames[i],
                dateNumber = dateNum,
                hasClasses = false
            )
        )
        cal.add(Calendar.DAY_OF_YEAR, 1)
    }

    val weekPrefix = when (weekOffset) {
        0 -> "This week"
        1 -> "Next week"
        -1 -> "Last week"
        else -> "Week"
    }
    val weekLabel = "$weekPrefix · $startDayNum–$endDayNum $monthStr"
    return Pair(weekLabel, daysList)
}

fun parseClassDuration(timeSlot: String): String {
    val parts = timeSlot.split("-", "–")
    if (parts.size < 2) return "1h 30m"
    val startMin = startMinutes(parts[0].trim())
    val endMin = startMinutes(parts[1].trim())
    if (startMin < 0 || endMin <= startMin) return "1h 30m"
    val diff = endMin - startMin
    val h = diff / 60
    val m = diff % 60
    return when {
        h > 0 && m > 0 -> "${h}h ${m}m"
        h > 0 -> "${h}h"
        else -> "${m}m"
    }
}

fun formatTimeRangeForDay(classes: List<ClassModel>): String {
    if (classes.isEmpty()) return "No classes"
    val first = classes.first().timeSlot.split("-", "–").firstOrNull()?.trim() ?: ""
    val last = classes.last().timeSlot.split("-", "–").lastOrNull()?.trim() ?: ""
    if (first.isBlank() || last.isBlank()) return "${classes.size} classes scheduled"
    return "$first – $last"
}

private val defaultWeekData = calculateCurrentWeekDays()

data class StudentPortalUiState(
    val selectedTab: Int = 0, // Default to Routine tab (index 0)
    val themeMode: String = SessionManager.THEME_SYSTEM,
    val hasVoted: Boolean = false,
    val votedCandidateId: String? = null,
    val receiptId: String = "CR-839201",
    val selectedDayIndex: Int = getTodayDayIndex(),
    val currentWeekOffset: Int = 0,
    val candidates: List<CandidateModel> = emptyList(),
    val isGateOpen: Boolean = false,
    val timerText: String = "00:00",
    val timerStatus: String = "Closed",
    val weekLabel: String = defaultWeekData.first,
    val scheduleDays: List<DayModel> = defaultWeekData.second,
    val classMap: Map<Int, List<ClassModel>> = emptyMap(),
    val profile: UserProfileModel = UserProfileModel(),
    val isAddClassSheetOpen: Boolean = false,
    val isAddClassManualMode: Boolean = true,
    val isNoticeSheetOpen: Boolean = false,
    val isCr: Boolean = false,
    val snackbarMessage: String? = null,
    val isUpdateAvailable: Boolean = false,
    val latestVersionName: String = "",
    val updateUrl: String = "https://github.com/shaddhow/CRConnect-App/releases/latest",
    val isCheckingUpdate: Boolean = false
)
