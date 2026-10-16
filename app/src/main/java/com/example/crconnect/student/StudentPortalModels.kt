package com.example.crconnect.student

import java.util.Calendar

data class CandidateModel(
    val id: Int,
    val name: String,
    val department: String = "Dept. of CSE",
    val votes: Int
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
    val name: String = "Rahim Uddin",
    val email: String = "student@bubt.edu.bd",
    val studentId: String = "2112345",
    val intake: String = "55",
    val section: String = "6",
    val department: String = "Computer Science & Engineering",
    val attendancePct: Int = 92,
    val cgpa: String = "3.85",
    val initials: String = "RU"
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
    val selectedTab: Int = 0, // 0: Candidates, 1: Routine, 2: Status, 3: Profile
    val hasVoted: Boolean = false,
    val votedCandidateId: Int? = null,
    val receiptId: String = "CR-839201",
    val selectedDayIndex: Int = getTodayDayIndex(),
    val candidates: List<CandidateModel> = listOf(
        CandidateModel(0, "Rahim Uddin", "Dept. of CSE", 1),
        CandidateModel(1, "Nusrat Jahan", "Dept. of CSE", 0),
        CandidateModel(2, "Tanvir Ahmed", "Dept. of CSE", 0),
        CandidateModel(3, "Sadia Islam", "Dept. of CSE", 0)
    ),
    val scheduleDays: List<DayModel> = listOf(
        DayModel(0, "SAT", 10, true),
        DayModel(1, "SUN", 11, false),
        DayModel(2, "MON", 12, false),
        DayModel(3, "TUE", 13, true),
        DayModel(4, "WED", 14, true),
        DayModel(5, "THU", 15, true),
        DayModel(6, "FRI", 16, false)
    ),
    val classMap: Map<Int, List<ClassModel>> = mapOf(
        0 to listOf(
            ClassModel("1", "09:00 - 10:30", "CSE 301 · Algorithms", "Room 402"),
            ClassModel("2", "11:00 - 12:30", "CSE 305 · Database", "Lab 2")
        ),
        3 to listOf(
            ClassModel("3", "10:00 - 11:30", "MAT 211 · Numerical Methods", "Room 310")
        ),
        4 to listOf(
            ClassModel("4", "09:00 - 10:30", "CSE 301 · Algorithms", "Room 402")
        ),
        5 to listOf(
            ClassModel("5", "11:00 - 12:30", "CSE 305 · Database", "Lab 2")
        )
    ),
    val profile: UserProfileModel = UserProfileModel(),
    val isAddClassSheetOpen: Boolean = false,
    val snackbarMessage: String? = null
)
