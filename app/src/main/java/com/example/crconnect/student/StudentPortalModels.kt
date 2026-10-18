package com.example.crconnect.student

import java.util.Calendar

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
    val room: String
)

data class UserProfileModel(
    val name: String = "Student",
    val email: String = "student@bubt.edu.bd",
    val studentId: String = "--",
    val intake: String = "--",
    val section: String = "--",
    val department: String = "Computer Science & Engineering",
    val attendancePct: Int = 92,
    val cgpa: String = "3.85",
    val initials: String = "ST"
)

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
    val scheduleDays: List<DayModel> = listOf(
        DayModel(0, "SAT", 10, false),
        DayModel(1, "SUN", 11, false),
        DayModel(2, "MON", 12, false),
        DayModel(3, "TUE", 13, false),
        DayModel(4, "WED", 14, false),
        DayModel(5, "THU", 15, false),
        DayModel(6, "FRI", 16, false)
    ),
    val classMap: Map<Int, List<ClassModel>> = emptyMap(),
    val profile: UserProfileModel = UserProfileModel(),
    val isAddClassSheetOpen: Boolean = false,
    val snackbarMessage: String? = null
)
