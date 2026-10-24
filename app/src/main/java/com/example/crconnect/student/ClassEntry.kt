package com.example.crconnect.student

import java.util.Locale

data class ClassEntry(
    val day: String,        // e.g. "THR", "SAT", "SUN", "MON", "TUE", "WED", "FRI"
    val course: String,     // e.g. "CSE 210"
    val faculty: String,    // e.g. "MDSS"
    val room: String,       // e.g. "2517"
    val startTime: String,  // e.g. "08:15 AM"
    val endTime: String,    // e.g. "09:45 AM"
    val intake: String? = null,
    val section: String? = null,
    val needsReview: Boolean = false,
    val correctedByOcrFix: Boolean = false
)

fun startMinutes(time: String): Int {
    if (time.isBlank()) return 0
    val startTimeStr = time.split("-")[0].trim()
    val match = Regex("""(?i)(\d{1,2}):(\d{2})\s*(AM|PM)?""").find(startTimeStr) ?: return 0
    val hour = match.groupValues[1].toIntOrNull() ?: return 0
    val minute = match.groupValues[2].toIntOrNull() ?: return 0
    val ampm = match.groupValues[3].uppercase(Locale.ROOT)

    val hour24 = when {
        ampm == "PM" -> if (hour == 12) 12 else hour + 12
        ampm == "AM" -> if (hour == 12) 0 else hour
        else -> if (hour == 12) 0 else hour
    }

    return hour24 * 60 + minute
}

