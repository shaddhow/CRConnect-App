package com.example.crconnect.student

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.crconnect.BuildConfig
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.regex.Pattern
import kotlin.math.roundToInt

data class OcrWord(
    val text: String,
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
)

internal data class TableGrid(
    val colCenters: List<Double>,
    val rowCenters: List<Double>
)

object RoutineOcrParser {

    private const val TAG = "RoutineOcrParser"

    private val TIME_SLOT_PAIRS = listOf(
        Pair("08:15 AM", "09:45 AM"),
        Pair("09:45 AM", "11:15 AM"),
        Pair("11:15 AM", "12:45 PM"),
        Pair("01:15 PM", "02:45 PM"),
        Pair("02:45 PM", "04:15 PM"),
        Pair("04:15 PM", "05:45 PM")
    )

    private val IGNORED_FACULTY_WORDS = setOf(
        "FC", "FC:", "FACULTY", "TEACHER", "INSTRUCTOR",
        "R", "R:", "RM", "ROOM", "LAB",
        "SAT", "SUN", "MON", "TUE", "WED", "THR", "THU", "THUR", "FRI",
        "SATURDAY", "SUNDAY", "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY",
        "INTAKE", "SEC", "SECTION", "BUBT", "CLASS", "DAY", "TIME", "BUILDING", "BLDG",
        "CSE", "MAT", "ENG", "EEE", "PHY", "CHEM", "BUS", "STA", "ACT", "FIN", "LAW", "GED", "TEX", "CIV"
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
                    if (BuildConfig.DEBUG) {
                        dumpOcrDebugInfo(inputImage.width, inputImage.height, visionText)
                    }
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
     * Thin adapter that converts ML Kit Text.Element to OcrWord and calls parseWords.
     */
    fun parseRoutineVisionText(visionText: Text): List<ClassEntry> {
        if (visionText.text.isBlank()) return emptyList()

        val words = mutableListOf<OcrWord>()
        for (block in visionText.textBlocks) {
            for (line in block.lines) {
                for (element in line.elements) {
                    val box = element.boundingBox ?: continue
                    words.add(OcrWord(element.text, box.left, box.top, box.right, box.bottom))
                }
            }
        }

        return parseWords(words, log = { Log.d("ROUTINE_ANCHORS", it) }).getOrDefault(emptyList())
    }

    /**
     * Pure function to calculate TableGrid (column and row centers).
     */
    internal fun computeTableGrid(
        words: List<OcrWord>,
        log: (String) -> Unit = {}
    ): Result<TableGrid> = runCatching {
        val timeRegex = Regex("""^\d{1,2}:\d{2}$""")
        val timeTokens = words.filter { timeRegex.matches(it.text.trim()) }
            .sortedBy { it.left }

        val colCenters = mutableListOf<Double>()

        if (timeTokens.size == 12) {
            for (i in 0 until 6) {
                val startWord = timeTokens[2 * i]
                val endWord = timeTokens[2 * i + 1]
                colCenters.add((startWord.left + endWord.right) / 2.0)
            }
        } else {
            val startTimes = listOf("08:15", "09:45", "11:15", "01:15", "02:45", "04:15")
            val foundStart = arrayOfNulls<OcrWord>(6)

            for (i in 0 until 6) {
                val target = startTimes[i]
                val matching = words.filter {
                    val trimmed = it.text.trim()
                    trimmed == target || trimmed == target.removePrefix("0")
                }
                if (matching.isNotEmpty()) {
                    foundStart[i] = matching.maxByOrNull { it.left }
                }
            }

            val foundIndices = (0 until 6).filter { foundStart[it] != null }
            if (foundIndices.size < 4) {
                error("Fewer than 4 column start times found (${foundIndices.size})")
            }

            val steps = mutableListOf<Double>()
            for (x in 0 until foundIndices.size) {
                for (y in x + 1 until foundIndices.size) {
                    val iA = foundIndices[x]
                    val iB = foundIndices[y]
                    val dist = (foundStart[iB]!!.left - foundStart[iA]!!.left).toDouble()
                    steps.add(dist / (iB - iA))
                }
            }
            val medianColWidth = median(steps)

            val tempStartLeft = DoubleArray(6)
            for (j in 0 until 6) {
                val refIndex = foundIndices.minByOrNull { Math.abs(it - j) }!!
                tempStartLeft[j] = foundStart[refIndex]!!.left + (j - refIndex) * medianColWidth
            }

            for (j in 0 until 6) {
                colCenters.add(tempStartLeft[j] + medianColWidth / 2.0)
            }
        }

        val medianColWidth = if (colCenters.size >= 2) {
            median((0 until colCenters.size - 1).map { colCenters[it + 1] - colCenters[it] })
        } else {
            160.0
        }

        val halfColWidth = medianColWidth / 2.0
        val maxDayLeft = colCenters[0] - halfColWidth

        val dayRowTargets = listOf(
            listOf("SAT"),
            listOf("SUN"),
            listOf("MON"),
            listOf("TUE"),
            listOf("WED"),
            listOf("THR", "THU", "THUR"),
            listOf("FRI")
        )

        val foundRowCenters = arrayOfNulls<Double>(7)

        for (r in 0 until 7) {
            val validNames = dayRowTargets[r]
            val candidate = words.firstOrNull { w ->
                val upper = w.text.trim().uppercase(Locale.ROOT)
                upper in validNames && w.left < maxDayLeft
            }
            if (candidate != null) {
                foundRowCenters[r] = (candidate.top + candidate.bottom) / 2.0
            }
        }

        val foundRowIndices = (0 until 7).filter { foundRowCenters[it] != null }
        if (foundRowIndices.isEmpty()) {
            error("No day rows found")
        }

        val finalRowCenters = DoubleArray(7)

        if (foundRowIndices.size == 7) {
            for (r in 0 until 7) {
                finalRowCenters[r] = foundRowCenters[r]!!
            }
        } else {
            val rowSteps = mutableListOf<Double>()
            for (x in 0 until foundRowIndices.size) {
                for (y in x + 1 until foundRowIndices.size) {
                    val rA = foundRowIndices[x]
                    val rB = foundRowIndices[y]
                    val dist = foundRowCenters[rB]!! - foundRowCenters[rA]!!
                    rowSteps.add(dist / (rB - rA))
                }
            }
            val medianRowHeight = median(rowSteps)

            for (m in 0 until 7) {
                val refIndex = foundRowIndices.minByOrNull { Math.abs(it - m) }!!
                finalRowCenters[m] = foundRowCenters[refIndex]!! + (m - refIndex) * medianRowHeight
            }
        }

        val rowCentersList = finalRowCenters.toList()

        log("Column centers: $colCenters")
        log("Row centers: $rowCentersList")

        TableGrid(colCenters = colCenters, rowCenters = rowCentersList)
    }

    /**
     * Pure function for parsing OCR words into ClassEntry list.
     */
    fun parseWords(
        words: List<OcrWord>,
        log: (String) -> Unit = {}
    ): Result<List<ClassEntry>> = runCatching {
        if (words.isEmpty()) return@runCatching emptyList()

        val rawTextFallback = words.joinToString(" ") { it.text }
        val gridResult = computeTableGrid(words, log)
        if (gridResult.isFailure) {
            return@runCatching fixFacultyReads(parseRoutineOCR(rawTextFallback))
        }

        val grid = gridResult.getOrThrow()
        val colCenters = grid.colCenters
        val rowCenters = grid.rowCenters

        val medianColSpacing = median((0 until colCenters.size - 1).map { colCenters[it + 1] - colCenters[it] })
        val medianRowSpacing = median((0 until rowCenters.size - 1).map { rowCenters[it + 1] - rowCenters[it] })

        val colLeftBounds = List(6) { i ->
            if (i == 0) colCenters[0] - medianColSpacing / 2.0
            else (colCenters[i - 1] + colCenters[i]) / 2.0
        }
        val colRightBounds = List(6) { i ->
            if (i == 5) colCenters[5] + medianColSpacing / 2.0
            else (colCenters[i] + colCenters[i + 1]) / 2.0
        }

        val rowTopBounds = List(7) { j ->
            if (j == 0) rowCenters[0] - medianRowSpacing / 2.0
            else (rowCenters[j - 1] + rowCenters[j]) / 2.0
        }
        val rowBottomBounds = List(7) { j ->
            if (j == 6) rowCenters[6] + medianRowSpacing / 2.0
            else (rowCenters[j] + rowCenters[j + 1]) / 2.0
        }

        val tableLeft = colLeftBounds[0]
        val tableRight = colRightBounds[5]
        val tableTop = rowTopBounds[0]
        val tableBottom = rowBottomBounds[6]

        val cellWordBuckets = mutableMapOf<Pair<Int, Int>, MutableList<OcrWord>>()

        for (word in words) {
            val cx = (word.left + word.right) / 2.0
            val cy = (word.top + word.bottom) / 2.0

            if (cx < tableLeft || cx > tableRight || cy < tableTop || cy > tableBottom) {
                continue
            }

            val colIndex = (0..5).firstOrNull { idx ->
                cx >= colLeftBounds[idx] && cx < colRightBounds[idx]
            } ?: if (cx >= colLeftBounds[5]) 5 else 0

            val rowIndex = (0..6).firstOrNull { idx ->
                cy >= rowTopBounds[idx] && cy < rowBottomBounds[idx]
            } ?: if (cy >= rowTopBounds[6]) 6 else 0

            cellWordBuckets.getOrPut(Pair(rowIndex, colIndex)) { mutableListOf() }.add(word)
        }

        val courseRegex = Regex("""(?i)\b(CSE|EEE|MAT|ENG|PHY|CHEM|BUS|STA|ACT|FIN|LAW|GED|TEX|CIV)[\s\-_]*(\d{3}[A-Z]?)\b""")
        val facultyRegex = Regex("""(?i)\bF\s*C\b\s*[:;.\-]?\s*([A-Za-z]{1,6})\b""")
        val roomRegex = Regex("""(?i)\b(?:R|Room|Rm)\b\s*[:.\-]?\s*(\d{3,4})\b""")
        val intakeRegex = Regex("""(?i)Intake\s*[:.]?\s*(\d{1,3})\s*[-–_/,]\s*(\d{1,3})""")

        val dayCodes = listOf("SAT", "SUN", "MON", "TUE", "WED", "THR", "FRI")
        val resultList = mutableListOf<ClassEntry>()
        val seenSlots = mutableSetOf<Pair<Int, Int>>()

        for (row in 0 until 7) {
            for (col in 0 until 6) {
                val bucketKey = Pair(row, col)
                val bucketWords = cellWordBuckets[bucketKey] ?: continue
                if (bucketWords.isEmpty()) continue

                val sortedWords = bucketWords.sortedWith(compareBy({ (it.top / 6.0).roundToInt() }, { it.left }))
                val bucketText = sortedWords.joinToString(" ") { it.text }

                log("Bucket ($row, $col): $bucketText")

                val matches = courseRegex.findAll(bucketText).toList()
                if (matches.isEmpty()) continue

                val blocks = if (matches.size == 1) {
                    listOf(Pair(matches[0], bucketText.substring(matches[0].range.start)))
                } else {
                    matches.mapIndexed { idx, match ->
                        val start = match.range.start
                        val end = if (idx + 1 < matches.size) matches[idx + 1].range.start else bucketText.length
                        Pair(match, bucketText.substring(start, end))
                    }
                }

                for ((match, blockText) in blocks) {
                    if (seenSlots.contains(bucketKey)) {
                        error("Multiple entries share same slot ($row, $col)")
                    }
                    seenSlots.add(bucketKey)

                    val dept = match.groupValues[1].uppercase(Locale.ROOT)
                    val num = match.groupValues[2].uppercase(Locale.ROOT)
                    val course = "$dept $num"

                    val facultyMatch = facultyRegex.find(blockText)
                    val rawFaculty = facultyMatch?.groupValues?.get(1)?.uppercase(Locale.ROOT)
                    val faculty = if (rawFaculty != null && !IGNORED_FACULTY_WORDS.contains(rawFaculty)) rawFaculty else null

                    val roomMatch = roomRegex.find(blockText)
                    val rawRoom = roomMatch?.groupValues?.get(1)
                    val room = if (rawRoom != null && !rawRoom.equals("ROOM", ignoreCase = true)) rawRoom else null

                    val intakeMatch = intakeRegex.find(blockText)
                    val intake = intakeMatch?.groupValues?.get(1)
                    val section = intakeMatch?.groupValues?.get(2)

                    val needsReview = (faculty == null || room == null)

                    val timePair = TIME_SLOT_PAIRS[col]

                    resultList.add(
                        ClassEntry(
                            day = dayCodes[row],
                            course = course,
                            faculty = faculty ?: "TBA",
                            room = room ?: "TBA",
                            startTime = timePair.first,
                            endTime = timePair.second,
                            intake = intake,
                            section = section,
                            needsReview = needsReview
                        )
                    )
                }
            }
        }

        val rawEntries = if (resultList.isNotEmpty()) resultList else parseRoutineOCR(rawTextFallback)
        fixFacultyReads(rawEntries)
    }

    private data class CourseGroupKey(
        val course: String,
        val intake: String?,
        val section: String?
    )

    fun fixFacultyReads(entries: List<ClassEntry>): List<ClassEntry> {
        if (entries.isEmpty()) return emptyList()

        val indexedGroups = entries.indices.groupBy { idx ->
            val entry = entries[idx]
            CourseGroupKey(entry.course, entry.intake, entry.section)
        }

        val result = entries.toMutableList()

        for ((_, indices) in indexedGroups) {
            val groupEntries = indices.map { entries[it] }
            val distinctFaculties = groupEntries.map { it.faculty }.distinct()

            if (distinctFaculties.size <= 1) {
                continue
            }

            if (canFixConfusables(distinctFaculties)) {
                val counts = groupEntries.groupingBy { it.faculty }.eachCount()
                val facultyComparator = createFacultyComparator(counts)
                val targetFaculty = distinctFaculties.maxWithOrNull(facultyComparator)!!

                for (idx in indices) {
                    val entry = entries[idx]
                    if (entry.faculty != targetFaculty) {
                        result[idx] = entry.copy(
                            faculty = targetFaculty,
                            correctedByOcrFix = true
                        )
                    }
                }
            } else {
                // Rule 3: differ by anything else -> keep each as read and set needsReview=true
                for (idx in indices) {
                    val entry = entries[idx]
                    result[idx] = entry.copy(
                        needsReview = true
                    )
                }
            }
        }

        return result
    }

    private fun canFixConfusables(faculties: List<String>): Boolean {
        if (faculties.isEmpty()) return true
        val firstLen = faculties[0].length
        if (faculties.any { it.length != firstLen }) return false

        for (i in faculties.indices) {
            for (j in i + 1 until faculties.size) {
                for (k in 0 until firstLen) {
                    if (!isConfusablePair(faculties[i][k], faculties[j][k])) {
                        return false
                    }
                }
            }
        }
        return true
    }

    private fun isConfusablePair(c1: Char, c2: Char): Boolean {
        if (c1 == c2) return true
        val pair = setOf(c1, c2)
        return pair == setOf('J', 'I') ||
                pair == setOf('I', 'L') ||
                pair == setOf('O', '0') ||
                pair == setOf('S', '5') ||
                pair == setOf('B', '8') ||
                pair == setOf('D', 'O')
    }

    private fun isPreferred(c1: Char, c2: Char): Boolean {
        return (c1 == 'J' && c2 == 'I') ||
                (c1 == 'L' && c2 == 'I') ||
                (c1 == 'O' && c2 == '0') ||
                (c1 == 'S' && c2 == '5') ||
                (c1 == 'B' && c2 == '8') ||
                (c1 == 'D' && c2 == 'O')
    }

    private fun createFacultyComparator(counts: Map<String, Int>): Comparator<String> {
        return Comparator { s1, s2 ->
            val count1 = counts[s1] ?: 0
            val count2 = counts[s2] ?: 0
            if (count1 != count2) {
                return@Comparator count1.compareTo(count2)
            }
            val len = minOf(s1.length, s2.length)
            for (k in 0 until len) {
                val c1 = s1[k]
                val c2 = s2[k]
                if (c1 != c2) {
                    if (isPreferred(c1, c2)) return@Comparator 1
                    if (isPreferred(c2, c1)) return@Comparator -1
                }
            }
            0
        }
    }

    private fun median(values: List<Double>): Double {
        if (values.isEmpty()) return 0.0
        val sorted = values.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 0) {
            (sorted[mid - 1] + sorted[mid]) / 2.0
        } else {
            sorted[mid]
        }
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
                val endIdx = Math.min(trimmed.length, startIdx + 80)
                val chunk = trimmed.substring(startIdx, endIdx)

                val fcMatcher = Pattern.compile("(?i)(?:FC|Faculty|Instructor|Teacher)[:\\s.-]*([A-Za-z]{2,5})\\b").matcher(chunk)
                val faculty = if (fcMatcher.find()) fcMatcher.group(1)?.uppercase(Locale.ROOT) else null

                val rMatcher = Pattern.compile("(?i)(?:R|Room|Rm)[:\\s.-]*([0-9]{3,4}|Lab[^0-9]*[0-9]+)\\b").matcher(chunk)
                val room = if (rMatcher.find()) rMatcher.group(1) else null

                val timePair = extractTimeSlotPair(chunk) ?: TIME_SLOT_PAIRS[slotIndex % TIME_SLOT_PAIRS.size]

                resultList.add(
                    ClassEntry(
                        day = currentDay,
                        course = formattedCourse,
                        faculty = faculty ?: "TBA",
                        room = room ?: "TBA",
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

    private fun dumpOcrDebugInfo(width: Int, height: Int, visionText: Text) {
        if (!BuildConfig.DEBUG) return

        val jsonArray = JSONArray()
        for (block in visionText.textBlocks) {
            for (line in block.lines) {
                for (element in line.elements) {
                    val box = element.boundingBox ?: continue
                    val obj = JSONObject()
                    obj.put("t", element.text)
                    obj.put("l", box.left)
                    obj.put("t2", box.top)
                    obj.put("r", box.right)
                    obj.put("b", box.bottom)
                    jsonArray.put(obj)
                }
            }
        }

        val fullMessage = "Image Size: ${width}x$height | Elements: $jsonArray"
        val chunkSize = 3000
        val totalLength = fullMessage.length
        val totalChunks = maxOf(1, (totalLength + chunkSize - 1) / chunkSize)

        for (i in 0 until totalChunks) {
            val start = i * chunkSize
            val end = minOf(start + chunkSize, totalLength)
            val chunk = fullMessage.substring(start, end)
            Log.d("ROUTINE_DUMP", "[${i + 1}/$totalChunks] $chunk")
        }
    }
}
