package com.example.crconnect.student

import android.content.Context
import android.graphics.Rect
import android.net.Uri
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.Locale
import java.util.regex.Pattern
import kotlin.math.abs

object RoutineOcrParser {

    private const val TAG = "RoutineOcrParser"

    private val BUBT_TIME_SLOTS = arrayOf(
        "08:15 - 09:45",
        "09:45 - 11:15",
        "11:15 - 12:45",
        "01:15 - 02:45",
        "02:45 - 04:15",
        "04:15 - 05:45"
    )

    private val TIME_SLOT_PAIRS = listOf(
        Pair("08:15 AM", "09:45 AM"),
        Pair("09:45 AM", "11:15 AM"),
        Pair("11:15 AM", "12:45 PM"),
        Pair("01:15 PM", "02:45 PM"),
        Pair("02:45 PM", "04:15 PM"),
        Pair("04:15 PM", "05:45 PM")
    )

    fun processOcrImage(
        context: Context,
        imageUri: Uri,
        onSuccess: (List<ClassEntry>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        try {
            val inputImage = InputImage.fromFilePath(context, imageUri)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

            recognizer.process(inputImage)
                .addOnSuccessListener { visionText ->
                    val entries = parseRoutineVisionText(visionText)
                    onSuccess(entries)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "ML Kit OCR failed: ${e.message}", e)
                    onFailure(e)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading image for OCR: ${e.message}", e)
            onFailure(e)
        }
    }

    /**
     * Advanced 2D Spatial OCR Parser:
     * Handles both horizontal (Days=Rows, Time=Cols) and vertical (Days=Cols, Time=Rows) BUBT routines.
     */
    fun parseRoutineVisionText(visionText: Text): List<ClassEntry> {
        if (visionText.text.isBlank()) return emptyList()

        val allLines = mutableListOf<TextLineItem>()
        for (block in visionText.textBlocks) {
            for (line in block.lines) {
                val text = line.text.trim()
                val box = line.boundingBox
                if (text.isNotEmpty() && box != null) {
                    allLines.add(TextLineItem(text, box))
                }
            }
        }

        if (allLines.isEmpty()) {
            return parseRoutineOCR(visionText.text)
        }

        class DayPos(val code: String, val fullDay: String, val cx: Int, val cy: Int)
        val dayAnchors = mutableListOf<DayPos>()
        val dayCodesSeen = mutableSetOf<String>()

        val dayNamesMap = mapOf(
            "SAT" to "Saturday", "SUN" to "Sunday", "MON" to "Monday",
            "TUE" to "Tuesday", "WED" to "Wednesday", "THR" to "Thursday",
            "THU" to "Thursday", "THUR" to "Thursday", "FRI" to "Friday"
        )

        for (item in allLines) {
            val upper = item.text.uppercase(Locale.ROOT)
            for ((code, fullDay) in dayNamesMap) {
                if (upper.contains(code) && upper.length <= 12 && !dayCodesSeen.contains(fullDay)) {
                    dayAnchors.add(DayPos(code, fullDay, item.centerX, item.centerY))
                    dayCodesSeen.add(fullDay)
                    break
                }
            }
        }

        class TimePos(val label: Pair<String, String>, val cx: Int, val cy: Int)
        val timeAnchors = mutableListOf<TimePos>()

        val timeKeywords = listOf(
            Pair("08:15 AM", "09:45 AM") to listOf("08:15", "8:15"),
            Pair("09:45 AM", "11:15 AM") to listOf("09:45", "9:45"),
            Pair("11:15 AM", "12:45 PM") to listOf("11:15"),
            Pair("01:15 PM", "02:45 PM") to listOf("01:15", "1:15"),
            Pair("02:45 PM", "04:15 PM") to listOf("02:45", "2:45"),
            Pair("04:15 PM", "05:45 PM") to listOf("04:15", "4:15")
        )

        for (item in allLines) {
            for ((pair, kwList) in timeKeywords) {
                if (kwList.any { item.text.contains(it) }) {
                    timeAnchors.add(TimePos(pair, item.centerX, item.centerY))
                    break
                }
            }
        }

        if (dayAnchors.isEmpty() || timeAnchors.isEmpty()) {
            return parseRoutineOCR(visionText.text)
        }

        val minDayX = dayAnchors.minOf { it.cx }
        val maxDayX = dayAnchors.maxOf { it.cx }
        val minDayY = dayAnchors.minOf { it.cy }
        val maxDayY = dayAnchors.maxOf { it.cy }

        val dayXSpan = maxDayX - minDayX
        val dayYSpan = maxDayY - minDayY
        val isDaysAsColumns = dayXSpan > dayYSpan

        val coursePattern = Pattern.compile("(?i)((?:CSE|MAT|ENG|EEE|PHY|CHEM|BUS|STA|ACT|FIN|LAW|GED|TEX|CIV|[A-Z]{2,4})[-_ ]*[0-9]{3,4}[A-Z]?)")

        val resultList = mutableListOf<ClassEntry>()
        val seenKeys = mutableSetOf<String>()

        for (item in allLines) {
            val matcher = coursePattern.matcher(item.text)
            while (matcher.find()) {
                val rawCourse = matcher.group(1) ?: continue
                val formattedCourse = formatCourseCode(rawCourse)

                val bestDayCode = if (isDaysAsColumns) {
                    dayAnchors.minByOrNull { abs(it.cx - item.centerX) }?.code ?: "SAT"
                } else {
                    dayAnchors.minByOrNull { abs(it.cy - item.centerY) }?.code ?: "SAT"
                }

                val bestTimePair = if (isDaysAsColumns) {
                    timeAnchors.minByOrNull { abs(it.cy - item.centerY) }?.label ?: TIME_SLOT_PAIRS[0]
                } else {
                    timeAnchors.minByOrNull { abs(it.cx - item.centerX) }?.label ?: TIME_SLOT_PAIRS[0]
                }

                val nearbyLines = allLines.filter {
                    abs(it.centerX - item.centerX) < 220 && abs(it.centerY - item.centerY) < 140
                }

                var faculty = "TBA"
                var room = "TBA"

                for (near in nearbyLines) {
                    if (faculty == "TBA") {
                        val fcM = Pattern.compile("(?i)FC[:\\s]*([A-Za-z]{2,5})").matcher(near.text)
                        if (fcM.find()) {
                            faculty = fcM.group(1)?.uppercase(Locale.ROOT) ?: "TBA"
                        }
                    }
                    if (room == "TBA") {
                        val rM = Pattern.compile("(?i)(?:R[:\\s]|Room[:\\s]|R\\b[:\\s]*)([0-9]{3,4})").matcher(near.text)
                        if (rM.find()) {
                            room = rM.group(1) ?: "TBA"
                        }
                    }
                }

                if (faculty == "TBA") {
                    faculty = extractFacultyFallback(item.text, formattedCourse)
                }
                if (room == "TBA") {
                    room = extractRoomFallback(item.text)
                }

                val key = "${bestDayCode}_${formattedCourse}_${bestTimePair.first}"
                if (!seenKeys.contains(key)) {
                    seenKeys.add(key)
                    resultList.add(
                        ClassEntry(
                            day = bestDayCode,
                            course = formattedCourse,
                            faculty = faculty,
                            room = room,
                            startTime = bestTimePair.first,
                            endTime = bestTimePair.second
                        )
                    )
                }
            }
        }

        return if (resultList.isNotEmpty()) resultList else parseRoutineOCR(visionText.text)
    }

    /**
     * Fallback parsing function for raw unformatted text string.
     */
    fun parseRoutineOCR(rawText: String): List<ClassEntry> {
        if (rawText.isBlank()) return emptyList()

        val resultList = mutableListOf<ClassEntry>()
        val lines = rawText.split("\n", "\r")
        var currentDay = "SAT"
        var slotIndex = 0

        val coursePattern = Pattern.compile("(?i)((?:CSE|MAT|ENG|EEE|PHY|CHEM|BUS|STA|ACT|FIN|LAW|GED|TEX|CIV|[A-Z]{2,4})[-_ ]*[0-9]{3,4}[A-Z]?)")

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue

            val detectedDay = normalizeDayCode(trimmed)
            if (detectedDay != null && isStandaloneDayLine(trimmed)) {
                currentDay = detectedDay
                slotIndex = 0
                continue
            } else if (detectedDay != null) {
                currentDay = detectedDay
            }

            val matcher = coursePattern.matcher(trimmed)

            while (matcher.find()) {
                val rawCourse = matcher.group(1) ?: continue
                val formattedCourse = formatCourseCode(rawCourse)

                val startIdx = matcher.start()
                val endIdx = Math.min(trimmed.length, startIdx + 70)
                val chunk = trimmed.substring(startIdx, endIdx)

                val fcMatcher = Pattern.compile("(?i)FC[:\\s]*([A-Za-z]{2,5})").matcher(chunk)
                val faculty = if (fcMatcher.find()) {
                    fcMatcher.group(1)?.uppercase(Locale.ROOT) ?: "TBA"
                } else {
                    extractFacultyFallback(chunk, formattedCourse)
                }

                val roomMatcher = Pattern.compile("(?i)(?:R[:\\s]|Room[:\\s]|R\\b[:\\s]*)([0-9]{3,4})").matcher(chunk)
                val room = if (roomMatcher.find()) {
                    roomMatcher.group(1) ?: "TBA"
                } else {
                    extractRoomFallback(chunk)
                }

                val timePair = extractTimeSlotPair(chunk) ?: TIME_SLOT_PAIRS[slotIndex % TIME_SLOT_PAIRS.size]

                resultList.add(
                    ClassEntry(
                        day = currentDay,
                        course = formattedCourse,
                        faculty = faculty,
                        room = room,
                        startTime = timePair.first,
                        endTime = timePair.second
                    )
                )

                slotIndex++
            }
        }

        return resultList
    }

    fun normalizeDayCode(text: String): String? {
        val upper = text.trim().uppercase(Locale.ROOT)
        return when {
            upper.contains("SAT") -> "SAT"
            upper.contains("SUN") -> "SUN"
            upper.contains("MON") -> "MON"
            upper.contains("TUE") -> "TUE"
            upper.contains("WED") -> "WED"
            upper.contains("THR") || upper.contains("THU") || upper.contains("THUR") -> "THR"
            upper.contains("FRI") -> "FRI"
            else -> null
        }
    }

    private fun isStandaloneDayLine(line: String): Boolean {
        val upper = line.trim().uppercase(Locale.ROOT)
        return upper in listOf(
            "SAT", "SUN", "MON", "TUE", "WED", "THR", "THU", "THUR",
            "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY", "MONDAY",
            "TUESDAY", "WEDNESDAY"
        )
    }

    private fun formatCourseCode(raw: String): String {
        val m = Pattern.compile("(?i)([A-Z]{2,4})[-_ ]*([0-9]{3,4}[A-Z]?)").matcher(raw.trim())
        if (m.find()) {
            val dept = m.group(1)?.uppercase(Locale.ROOT) ?: ""
            val num = m.group(2)?.uppercase(Locale.ROOT) ?: ""
            return "$dept $num"
        }
        return raw.trim().uppercase(Locale.ROOT)
    }

    private fun extractFacultyFallback(text: String, courseCode: String): String {
        val coursePrefix = courseCode.split(" ")[0]
        val matcher = Pattern.compile("([A-Z]{2,5})").matcher(text)
        while (matcher.find()) {
            val candidate = matcher.group(1)
            if (candidate != null &&
                candidate != coursePrefix &&
                !listOf("SAT", "SUN", "MON", "TUE", "WED", "THR", "THU", "THUR", "FRI", "INTAKE", "SEC", "ROOM", "LAB", "BUBT", "CLASS").contains(candidate.uppercase(Locale.ROOT))
            ) {
                return candidate.uppercase(Locale.ROOT)
            }
        }
        return "TBA"
    }

    private fun extractRoomFallback(text: String): String {
        val matcher = Pattern.compile("([0-9]{3,4})").matcher(text)
        while (matcher.find()) {
            val num = matcher.group(1)
            if (num != null && !num.startsWith("55") && num != "2024" && num != "2025" && num != "2026") {
                return num
            }
        }
        return "TBA"
    }

    private fun extractTimeSlotPair(text: String): Pair<String, String>? {
        val m = Pattern.compile("([0-1]?[0-9]:[0-5][0-9])\\s*[–\\-sTOto]+\\s*([0-1]?[0-9]:[0-5][0-9])").matcher(text)
        if (m.find()) {
            val start = formatTime12H(m.group(1) ?: "")
            val end = formatTime12H(m.group(2) ?: "")
            if (start.isNotEmpty() && end.isNotEmpty()) {
                return Pair(start, end)
            }
        }
        return null
    }

    private fun formatTime12H(timeStr: String): String {
        if (timeStr.isBlank()) return ""
        try {
            val parts = timeStr.trim().split(":")
            if (parts.size == 2) {
                var hour = parts[0].toInt()
                val min = parts[1].toInt()
                val period = if (hour in 8..11) "AM" else "PM"
                if (hour > 12) hour -= 12
                return String.format(Locale.US, "%02d:%02d %s", hour, min, period)
            }
        } catch (ignored: Exception) {}
        return timeStr
    }

    private class TextLineItem(val text: String, val box: Rect) {
        val centerX: Int = box.centerX()
        val centerY: Int = box.centerY()
    }
}
