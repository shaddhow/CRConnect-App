package com.example.crconnect.student

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

data class UserProfileModel(
    val name: String = "Student",
    val email: String = "student@bubt.edu.bd",
    val studentId: String = "--",
    val intake: String = "--",
    val section: String = "--",
    val department: String = "Dept. of CSE",
    val attendancePct: Int = 92,
    val cgpa: String = "3.85",
    val initials: String = "ST"
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

fun calculateCurrentWeekDays(): Pair<String, List<DayModel>> {
    val cal = Calendar.getInstance()
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

    val weekLabel = "This week · $startDayNum–$endDayNum $monthStr"
    return Pair(weekLabel, daysList)
}

private val defaultWeekData = calculateCurrentWeekDays()

data class StudentPortalUiState(
    val selectedTab: Int = 0, // Default to Candidates tab (index 0)
    val hasVoted: Boolean = false,
    val votedCandidateId: String? = null,
    val receiptId: String = "CR-839201",
    val selectedDayIndex: Int = getTodayDayIndex(),
    val candidates: List<CandidateModel> = emptyList(),
    val isGateOpen: Boolean = false,
    val timerText: String = "00:00",
    val timerStatus: String = "Closed",
    val weekLabel: String = defaultWeekData.first,
    val scheduleDays: List<DayModel> = defaultWeekData.second,
    val classMap: Map<Int, List<ClassModel>> = emptyMap(),
    val profile: UserProfileModel = UserProfileModel(),
    val isAddClassSheetOpen: Boolean = false,
    val snackbarMessage: String? = null
)
