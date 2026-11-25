package com.example.crconnect.student

import com.example.crconnect.SessionManager
import java.io.File
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

data class WeeklyAttendanceArchive(
    val weekKey: String = "",
    val dateRange: String = "",
    val attendedClasses: Int = 0,
    val totalClasses: Int = 0,
    val percentage: Int = 0,
    val archivedAt: Long = System.currentTimeMillis()
)

fun getDayOffsetFromSunday(dayIndex: Int): Int {
    return when (dayIndex) {
        1 -> 0 // Sunday
        2 -> 1 // Monday
        3 -> 2 // Tuesday
        4 -> 3 // Wednesday
        5 -> 4 // Thursday
        6 -> 5 // Friday
        0 -> 6 // Saturday
        else -> dayIndex
    }
}

fun getTodayOffsetFromSunday(): Int {
    val cal = Calendar.getInstance()
    return when (cal.get(Calendar.DAY_OF_WEEK)) {
        Calendar.SUNDAY -> 0
        Calendar.MONDAY -> 1
        Calendar.TUESDAY -> 2
        Calendar.WEDNESDAY -> 3
        Calendar.THURSDAY -> 4
        Calendar.FRIDAY -> 5
        Calendar.SATURDAY -> 6
        else -> 0
    }
}

fun getClassTimingStatus(dayIndex: Int, timeSlot: String): ClassTimingStatus {
    val todayOffset = getTodayOffsetFromSunday()
    val classDayOffset = getDayOffsetFromSunday(dayIndex)

    if (classDayOffset < todayOffset) {
        return ClassTimingStatus.PAST
    } else if (classDayOffset > todayOffset) {
        return ClassTimingStatus.FUTURE
    } else {
        val cal = Calendar.getInstance()
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

data class ClassTimeRange(
    val startHour: String,
    val startAmPm: String,
    val endHour: String,
    val endAmPm: String,
    val fullFormattedRange: String
)

fun parseClassTimeRange(timeSlot: String): ClassTimeRange {
    val parts = timeSlot.split("-", "–").map { it.trim() }.filter { it.isNotBlank() }
    val startRaw = parts.firstOrNull() ?: "09:00 AM"
    val endRaw = if (parts.size >= 2) parts[1] else null

    fun parseParts(timeStr: String): Pair<String, String> {
        val match = Regex("""(?i)(\d{1,2}:\d{2})\s*(AM|PM)?""").find(timeStr)
        if (match != null) {
            val hhmm = match.groupValues[1]
            val ampm = if (match.groupValues[2].isNotBlank()) match.groupValues[2].uppercase(Locale.ROOT) else ""
            return Pair(hhmm, ampm)
        }
        val tokens = timeStr.split("\\s+".toRegex())
        val h = tokens.firstOrNull() ?: timeStr
        val a = if (tokens.size > 1) tokens[1].uppercase(Locale.ROOT) else ""
        return Pair(h, a)
    }

    val (sHour, sAmPm) = parseParts(startRaw)
    val effectiveStartAmPm = sAmPm.ifEmpty { "AM" }

    val (eHour, eAmPm) = if (endRaw != null) {
        val (h, a) = parseParts(endRaw)
        Pair(h, a.ifEmpty { effectiveStartAmPm })
    } else {
        val sMin = startMinutes(startRaw)
        val eMin = sMin + 90
        val eHour24 = (eMin / 60) % 24
        val eMinute = eMin % 60
        val isPm = eHour24 >= 12
        val h12 = when {
            eHour24 == 0 -> 12
            eHour24 > 12 -> eHour24 - 12
            else -> eHour24
        }
        val hhmm = String.format(Locale.US, "%02d:%02d", h12, eMinute)
        val a = if (isPm) "PM" else "AM"
        Pair(hhmm, a)
    }

    val fullRange = "$sHour $effectiveStartAmPm – $eHour $eAmPm"
    return ClassTimeRange(
        startHour = sHour,
        startAmPm = effectiveStartAmPm,
        endHour = eHour,
        endAmPm = eAmPm,
        fullFormattedRange = fullRange
    )
}

fun getClassLiveProgress(dayIndex: Int, timeSlot: String): Float {
    val status = getClassTimingStatus(dayIndex, timeSlot)
    return when (status) {
        ClassTimingStatus.PAST -> 1.0f
        ClassTimingStatus.FUTURE -> 0.0f
        ClassTimingStatus.LIVE_NOW -> {
            val cal = Calendar.getInstance()
            val currentMins = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
            val sMins = startMinutes(timeSlot)
            val eMins = endMinutes(timeSlot)
            val duration = (eMins - sMins).coerceAtLeast(1)
            val elapsed = (currentMins - sMins).coerceIn(0, duration)
            (elapsed.toFloat() / duration.toFloat()).coerceIn(0.06f, 1.0f)
        }
    }
}

fun getClassRemainingMinutes(timeSlot: String): Int {
    val cal = Calendar.getInstance()
    val currentMins = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    val eMins = endMinutes(timeSlot)
    return (eMins - currentMins).coerceAtLeast(0)
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

fun normalizeCourseCode(rawCourse: String): String {
    if (rawCourse.isBlank()) return ""
    // Strip faculty in parentheses e.g. "CSE 301 (NAA)" -> "CSE 301"
    val withoutFaculty = rawCourse.split("(").firstOrNull()?.trim() ?: rawCourse.trim()
    // Split course code and title separated by dot/dash/colon, e.g. "CSE 301 · Algorithms" -> "CSE 301"
    val clean = withoutFaculty.split("·", " - ", ":").firstOrNull()?.trim() ?: withoutFaculty
    return clean.replace("\\s+".toRegex(), " ").uppercase(Locale.getDefault())
}

fun computeUniqueCoursesCount(classMap: Map<Int, List<ClassModel>>): Int {
    val allClasses = classMap.values.flatten()
    return allClasses
        .map { normalizeCourseCode(it.courseName) }
        .filter { it.isNotBlank() }
        .distinct()
        .size
}

data class UserProfileModel(
    val name: String = "Student",
    val email: String = "student@gmail.com",
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
    val isCheckingUpdate: Boolean = false,
    val isUpdateDialogVisible: Boolean = false,
    val isUpToDateDialogVisible: Boolean = false,
    val currentAppVersion: String = "1.1.0",
    val apkDownloadUrl: String? = null,
    val releaseNotes: String = "",
    val isForceUpdate: Boolean = false,
    val isDownloadingApk: Boolean = false,
    val downloadProgress: Float = 0f,
    val downloadedMb: Float = 0f,
    val totalMb: Float = 0f,
    val downloadedApkFile: File? = null,
    val updateErrorMessage: String? = null,
    val uniqueCourseCount: Int = 0,
    val attendanceArchives: List<WeeklyAttendanceArchive> = emptyList()
)
