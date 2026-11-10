package com.example.crconnect.student

import android.content.Context
import android.content.SharedPreferences
import com.example.crconnect.SessionManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Calendar
import java.util.Locale

class RoutineRepository(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    private fun getCurrentUserId(): String {
        val session = SessionManager(context)
        val email = session.userEmail
        val id = session.userID
        return when {
            email.isNotBlank() -> email
            id.isNotBlank() -> id
            else -> ""
        }
    }

    fun saveClassEntries(newEntries: List<ClassEntry>) {
        val existing = getClassEntries().toMutableList()

        for (newEntry in newEntries) {
            val normDay = normalizeDay(newEntry.day)
            val entryWithNormDay = newEntry.copy(day = normDay)

            existing.removeAll {
                normalizeDay(it.day) == normDay &&
                        it.startTime == newEntry.startTime &&
                        it.course.equals(newEntry.course, ignoreCase = true)
            }
            existing.add(entryWithNormDay)
        }

        val sortedList = existing.sortedBy { startMinutes(it.startTime) }
        val json = gson.toJson(sortedList)
        val currentUserId = getCurrentUserId()
        val editor = prefs.edit()
            .putString(KEY_ENTRIES_JSON, json)
            .putString(KEY_OWNER_ID, currentUserId)
        if (currentUserId.isNotBlank()) {
            val sanitizedUser = currentUserId.replace(Regex("[^a-zA-Z0-9_]"), "_")
            editor.putString(KEY_USER_ENTRIES_PREFIX + sanitizedUser, json)
        }
        editor.apply()
    }

    fun replaceClassEntries(newEntries: List<ClassEntry>) {
        val normalizedList = newEntries.map { newEntry ->
            val normDay = normalizeDay(newEntry.day)
            newEntry.copy(day = normDay)
        }.sortedBy { startMinutes(it.startTime) }
        val json = gson.toJson(normalizedList)
        val currentUserId = getCurrentUserId()
        val editor = prefs.edit()
            .putString(KEY_ENTRIES_JSON, json)
            .putString(KEY_OWNER_ID, currentUserId)
        if (currentUserId.isNotBlank()) {
            val sanitizedUser = currentUserId.replace(Regex("[^a-zA-Z0-9_]"), "_")
            editor.putString(KEY_USER_ENTRIES_PREFIX + sanitizedUser, json)
        }
        editor.apply()
    }

    fun getClassEntries(): List<ClassEntry> {
        val currentUserId = getCurrentUserId()
        val sanitizedUser = if (currentUserId.isNotBlank()) currentUserId.replace(Regex("[^a-zA-Z0-9_]"), "_") else ""

        var json: String? = null
        if (sanitizedUser.isNotBlank()) {
            json = prefs.getString(KEY_USER_ENTRIES_PREFIX + sanitizedUser, null)
        }

        if (json.isNullOrBlank()) {
            val storedOwner = prefs.getString(KEY_OWNER_ID, null)
            if (storedOwner.isNullOrBlank() || currentUserId.isBlank() || storedOwner == currentUserId) {
                json = prefs.getString(KEY_ENTRIES_JSON, null)
            }
        }

        if (json.isNullOrBlank()) return emptyList()

        return try {
            val type = object : TypeToken<List<ClassEntry>>() {}.type
            gson.fromJson<List<ClassEntry>>(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getDefaultSampleEntries(): List<ClassEntry> {
        return listOf(
            ClassEntry("SUN", "CSE 101", "MDSS", "Room 602", "08:15 AM", "09:45 AM"),
            ClassEntry("SUN", "CSE 102", "AAN", "Room 603", "10:00 AM", "11:30 AM"),
            ClassEntry("MON", "CSE 201", "MHR", "Room 504", "08:15 AM", "09:45 AM"),
            ClassEntry("MON", "CSE 202", "MKA", "Room 505", "11:45 AM", "01:15 PM"),
            ClassEntry("TUE", "CSE 301", "JAB", "Room 401", "09:45 AM", "11:15 AM"),
            ClassEntry("TUE", "CSE 302", "NSR", "Room 402", "01:30 PM", "03:00 PM"),
            ClassEntry("WED", "CSE 401", "TAK", "Room 308", "08:15 AM", "09:45 AM"),
            ClassEntry("WED", "CSE 402", "SAH", "Room 309", "10:00 AM", "11:30 AM"),
            ClassEntry("THR", "CSE 405", "FHM", "Room 207", "11:45 AM", "01:15 PM")
        )
    }

    fun getEntriesForDay(day: String): List<ClassEntry> {
        val normTarget = normalizeDay(day)
        return getClassEntries().filter { normalizeDay(it.day) == normTarget }.sortedBy { startMinutes(it.startTime) }
    }

    fun getDaysWithClasses(): Set<String> {
        return getClassEntries().map { normalizeDay(it.day) }.toSet()
    }

    fun clearEntries() {
        val currentUserId = getCurrentUserId()
        val editor = prefs.edit().remove(KEY_ENTRIES_JSON).remove(KEY_OWNER_ID)
        if (currentUserId.isNotBlank()) {
            val sanitizedUser = currentUserId.replace(Regex("[^a-zA-Z0-9_]"), "_")
            editor.remove(KEY_USER_ENTRIES_PREFIX + sanitizedUser)
        }
        editor.apply()
    }

    fun clearActiveSessionOnly() {
        prefs.edit().remove(KEY_ENTRIES_JSON).remove(KEY_OWNER_ID).apply()
    }

    fun normalizeDay(day: String): String {
        val upper = day.trim().uppercase(Locale.ROOT)
        return when {
            upper.contains("SAT") -> "SAT"
            upper.contains("SUN") -> "SUN"
            upper.contains("MON") -> "MON"
            upper.contains("TUE") -> "TUE"
            upper.contains("WED") -> "WED"
            upper.contains("THR") || upper.contains("THU") || upper.contains("THUR") -> "THR"
            upper.contains("FRI") -> "FRI"
            else -> upper.take(3)
        }
    }

    fun getCurrentWeekKey(): String {
        val cal = Calendar.getInstance()
        cal.firstDayOfWeek = Calendar.SATURDAY
        val year = cal.get(Calendar.YEAR)
        val week = cal.get(Calendar.WEEK_OF_YEAR)
        return "${year}_W${week}"
    }

    fun getWeeklyAttendanceMap(): Map<String, String> {
        val currentWeek = getCurrentWeekKey()
        val currentUserId = getCurrentUserId().replace(Regex("[^a-zA-Z0-9_]"), "_")
        val weekKey = KEY_LAST_ATTENDANCE_WEEK + "_" + currentUserId
        val attendanceKey = KEY_ATTENDANCE_PREFIX + currentUserId

        val lastWeek = prefs.getString(weekKey, null)
        if (lastWeek != currentWeek) {
            // Automatic Weekly Reset!
            prefs.edit()
                .remove(attendanceKey)
                .putString(weekKey, currentWeek)
                .apply()
            return emptyMap()
        }

        val json = prefs.getString(attendanceKey, null) ?: return emptyMap()
        return try {
            val type = object : TypeToken<Map<String, String>>() {}.type
            gson.fromJson(json, type) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun saveWeeklyAttendanceState(classId: String, state: String?) {
        val currentWeek = getCurrentWeekKey()
        val currentUserId = getCurrentUserId().replace(Regex("[^a-zA-Z0-9_]"), "_")
        val weekKey = KEY_LAST_ATTENDANCE_WEEK + "_" + currentUserId
        val attendanceKey = KEY_ATTENDANCE_PREFIX + currentUserId

        val currentMap = getWeeklyAttendanceMap().toMutableMap()
        if (state == null) {
            currentMap.remove(classId)
        } else {
            currentMap[classId] = state
        }

        prefs.edit()
            .putString(attendanceKey, gson.toJson(currentMap))
            .putString(weekKey, currentWeek)
            .apply()
    }

    companion object {
        private const val PREF_NAME = "crconnect_routine_preferences"
        private const val KEY_ENTRIES_JSON = "key_parsed_class_entries"
        private const val KEY_USER_ENTRIES_PREFIX = "key_user_entries_"
        private const val KEY_OWNER_ID = "key_routine_owner_id"
        private const val KEY_ATTENDANCE_PREFIX = "key_weekly_attendance_"
        private const val KEY_LAST_ATTENDANCE_WEEK = "key_last_attendance_week_"
    }
}
