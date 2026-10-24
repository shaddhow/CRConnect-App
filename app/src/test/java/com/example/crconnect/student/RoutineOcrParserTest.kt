package com.example.crconnect.student

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RoutineOcrParserTest {

    private val slotStartTimes = listOf(
        "08:15 AM",
        "09:45 AM",
        "11:15 AM",
        "01:15 PM",
        "02:45 PM",
        "04:15 PM"
    )

    private data class ExpectedEntry(
        val day: String,
        val slotIndex: Int,
        val course: String,
        val room: String,
        val validFaculties: Set<String>,
        val intake: String = "55",
        val section: String = "6"
    )

    @Test
    fun testParseWordsWithSampleElements() {
        val inputStream = javaClass.classLoader?.getResourceAsStream("routine_sample_elements.json")
            ?: error("Resource routine_sample_elements.json not found")
        val jsonText = inputStream.bufferedReader().use { it.readText() }
        val rootObj = JSONObject(jsonText)
        val elementsArray = rootObj.getJSONArray("elements")

        val words = mutableListOf<OcrWord>()
        for (i in 0 until elementsArray.length()) {
            val obj = elementsArray.getJSONObject(i)
            val t = obj.getString("t")
            val l = obj.getInt("l")
            val t2 = obj.getInt("t2")
            val r = obj.getInt("r")
            val b = obj.getInt("b")
            words.add(OcrWord(text = t, left = l, top = t2, right = r, bottom = b))
        }

        val gridResult = RoutineOcrParser.computeTableGrid(words, log = { println(it) })
        assertTrue("Expected computeTableGrid to return success", gridResult.isSuccess)

        val grid = gridResult.getOrThrow()
        println("Column Centers: ${grid.colCenters}")
        println("Row Centers: ${grid.rowCenters}")

        val expectedColCenters = listOf(171.5, 338.5, 503.5, 665.5, 827.5, 989.5)
        val expectedRowCenters = listOf(286.0, 369.0, 451.0, 534.0, 617.0, 699.0, 782.0)

        assertEquals("Column centers count", expectedColCenters.size, grid.colCenters.size)
        for (i in expectedColCenters.indices) {
            assertEquals("Column center $i", expectedColCenters[i], grid.colCenters[i], 1.0)
        }

        assertEquals("Row centers count", expectedRowCenters.size, grid.rowCenters.size)
        for (i in expectedRowCenters.indices) {
            assertEquals("Row center $i", expectedRowCenters[i], grid.rowCenters[i], 1.0)
        }

        val parseResult = RoutineOcrParser.parseWords(words, log = { println(it) })
        assertTrue("Expected parseWords to return success", parseResult.isSuccess)
        val entries = parseResult.getOrThrow()

        println("Parsed ${entries.size} entries:")
        entries.forEach { println(it) }

        val expectedEntries = listOf(
            ExpectedEntry("SUN", 0, "CSE 210", "2517", setOf("MDSS")),
            ExpectedEntry("SUN", 1, "CSE 210", "2517", setOf("MDSS")),
            ExpectedEntry("MON", 3, "CSE 207", "2810", setOf("AAG")),
            ExpectedEntry("MON", 4, "CSE 341", "2810", setOf("MJ")),
            ExpectedEntry("MON", 5, "CSE 209", "2808", setOf("MDSS")),
            ExpectedEntry("TUE", 1, "CSE 341", "2808", setOf("MJ")),
            ExpectedEntry("TUE", 2, "CSE 207", "2808", setOf("AAG")),
            ExpectedEntry("WED", 2, "CSE 208", "2517", setOf("AAG")),
            ExpectedEntry("WED", 3, "CSE 208", "2517", setOf("AAG")),
            ExpectedEntry("THR", 3, "CSE 342", "2419", setOf("MJ")),
            ExpectedEntry("THR", 4, "CSE 342", "2419", setOf("MJ")),
            ExpectedEntry("THR", 5, "CSE 209", "2318", setOf("MDSS"))
        )

        assertEquals("Expected exactly 12 entries", expectedEntries.size, entries.size)

        for (i in expectedEntries.indices) {
            val exp = expectedEntries[i]
            val act = entries[i]

            val actSlotIndex = slotStartTimes.indexOf(act.startTime)

            assertEquals("Entry $i day", exp.day, act.day)
            assertEquals("Entry $i slot", exp.slotIndex, actSlotIndex)
            assertEquals("Entry $i course", exp.course, act.course)
            assertEquals("Entry $i room", exp.room, act.room)
            assertTrue(
                "Entry $i faculty '${act.faculty}' not in valid set ${exp.validFaculties}",
                exp.validFaculties.contains(act.faculty)
            )
            assertEquals("Entry $i intake", exp.intake, act.intake)
            assertEquals("Entry $i section", exp.section, act.section)
        }
    }

    @Test
    fun testFixFacultyReadsMajorityVote() {
        val entries = listOf(
            ClassEntry("MON", "CSE 101", "MJ", "101", "08:15 AM", "09:45 AM", "55", "1"),
            ClassEntry("TUE", "CSE 101", "MJ", "101", "08:15 AM", "09:45 AM", "55", "1"),
            ClassEntry("WED", "CSE 101", "MI", "101", "08:15 AM", "09:45 AM", "55", "1")
        )
        val fixed = RoutineOcrParser.fixFacultyReads(entries)
        assertEquals(3, fixed.size)
        assertEquals("MJ", fixed[0].faculty)
        assertEquals("MJ", fixed[1].faculty)
        assertEquals("MJ", fixed[2].faculty)
        assertEquals(false, fixed[0].correctedByOcrFix)
        assertEquals(false, fixed[1].correctedByOcrFix)
        assertEquals(true, fixed[2].correctedByOcrFix)
    }

    @Test
    fun testFixFacultyReadsNonConfusableKeepBoth() {
        val entries = listOf(
            ClassEntry("MON", "CSE 102", "AB", "101", "08:15 AM", "09:45 AM", "55", "1"),
            ClassEntry("TUE", "CSE 102", "AC", "101", "08:15 AM", "09:45 AM", "55", "1")
        )
        val fixed = RoutineOcrParser.fixFacultyReads(entries)
        assertEquals(2, fixed.size)
        assertEquals("AB", fixed[0].faculty)
        assertEquals(true, fixed[0].needsReview)
        assertEquals("AC", fixed[1].faculty)
        assertEquals(true, fixed[1].needsReview)
    }

    @Test
    fun testFixFacultyReadsTieCanonicalResolution() {
        val entries = listOf(
            ClassEntry("MON", "CSE 103", "MI", "101", "08:15 AM", "09:45 AM", "55", "1"),
            ClassEntry("TUE", "CSE 103", "MJ", "101", "08:15 AM", "09:45 AM", "55", "1")
        )
        val fixed = RoutineOcrParser.fixFacultyReads(entries)
        assertEquals(2, fixed.size)
        assertEquals("MJ", fixed[0].faculty)
        assertEquals(true, fixed[0].correctedByOcrFix)
        assertEquals("MJ", fixed[1].faculty)
        assertEquals(false, fixed[1].correctedByOcrFix)
    }

    @Test
    fun testTimeSortingByStartMinutes() {
        val input = listOf("04:15 PM", "01:15 PM", "02:45 PM", "08:15 AM")
        val expected = listOf("08:15 AM", "01:15 PM", "02:45 PM", "04:15 PM")
        val actual = input.sortedBy { startMinutes(it) }
        assertEquals(expected, actual)
    }

    @Test
    fun testStartMinutesParsing() {
        assertEquals(15, startMinutes("12:15 AM"))
        assertEquals(720, startMinutes("12:00 PM"))
        assertEquals(750, startMinutes("12:30 PM"))
        assertEquals(795, startMinutes("01:15 PM"))
        assertEquals(495, startMinutes("08:15 AM"))
        assertEquals(975, startMinutes("04:15 PM - 05:45 PM"))
    }
}
