package com.example.crconnect.student

import android.content.Context
import android.content.SharedPreferences
import com.example.crconnect.SessionManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
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

    fun getAuthenticatedOrSessionUid(): String {
        val authUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        if (!authUid.isNullOrBlank()) {
            return authUid
        }
        val session = SessionManager(context)
        val id = session.userID
        val email = session.userEmail
        return when {
            id.isNotBlank() && id != "--" && !id.contains("@") -> id.replace(Regex("[^a-zA-Z0-9_]"), "_")
            email.isNotBlank() -> email.replace(Regex("[^a-zA-Z0-9_]"), "_")
            else -> "guest"
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
            val session = SessionManager(context)
            val isOwnerMatch = storedOwner.isNullOrBlank() || currentUserId.isBlank() ||
                    storedOwner == currentUserId ||
                    storedOwner == session.userID ||
                    storedOwner == session.userEmail
            if (isOwnerMatch) {
                json = prefs.getString(KEY_ENTRIES_JSON, null)
            }
        }
        if (json.isNullOrBlank()) {
            json = prefs.getString(KEY_ENTRIES_JSON, null)
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
            // SUN (3h double lab)
            ClassEntry("SUN", "CSE 210", "MDSS", "2517", "08:15 AM", "09:45 AM", intake = "55", section = "6"),
            ClassEntry("SUN", "CSE 210", "MDSS", "2517", "09:45 AM", "11:15 AM", intake = "55", section = "6"),
            // MON
            ClassEntry("MON", "CSE 207", "AAG", "2810", "01:15 PM", "02:45 PM", intake = "55", section = "6"),
            ClassEntry("MON", "CSE 341", "MJ", "2810", "02:45 PM", "04:15 PM", intake = "55", section = "6"),
            ClassEntry("MON", "CSE 209", "MDSS", "2808", "04:15 PM", "05:45 PM", intake = "55", section = "6"),
            // TUE
            ClassEntry("TUE", "CSE 341", "MJ", "2808", "09:45 AM", "11:15 AM", intake = "55", section = "6"),
            ClassEntry("TUE", "CSE 207", "AAG", "2808", "11:15 AM", "12:45 PM", intake = "55", section = "6"),
            // WED (3h double lab - both slots guaranteed!)
            ClassEntry("WED", "CSE 208", "AAG", "2517", "11:15 AM", "12:45 PM", intake = "55", section = "6"),
            ClassEntry("WED", "CSE 208", "AAG", "2517", "01:15 PM", "02:45 PM", intake = "55", section = "6"),
            // THR (3h double lab + theory)
            ClassEntry("THR", "CSE 342", "MJ", "2419", "01:15 PM", "02:45 PM", intake = "55", section = "6"),
            ClassEntry("THR", "CSE 342", "MJ", "2419", "02:45 PM", "04:15 PM", intake = "55", section = "6"),
            ClassEntry("THR", "CSE 209", "MDSS", "2318", "04:15 PM", "05:45 PM", intake = "55", section = "6")
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
        cal.firstDayOfWeek = Calendar.SUNDAY
        val year = cal.get(Calendar.YEAR)
        val week = cal.get(Calendar.WEEK_OF_YEAR)
        return "${year}_W${week}"
    }

    fun getSundayWeekDateRange(calendar: Calendar = Calendar.getInstance()): String {
        val c = (calendar.clone() as Calendar).apply {
            firstDayOfWeek = Calendar.SUNDAY
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        }
        val startDay = c.get(Calendar.DAY_OF_MONTH)
        val startMonthStr = SimpleDateFormat("MMM", Locale.US).format(c.time)
        val startYear = c.get(Calendar.YEAR)

        c.add(Calendar.DAY_OF_YEAR, 6)
        val endDay = c.get(Calendar.DAY_OF_MONTH)
        val endMonthStr = SimpleDateFormat("MMM", Locale.US).format(c.time)
        val endYear = c.get(Calendar.YEAR)

        return if (startMonthStr == endMonthStr && startYear == endYear) {
            "$startDay–$endDay $startMonthStr $startYear"
        } else if (startYear == endYear) {
            "$startDay $startMonthStr – $endDay $endMonthStr $startYear"
        } else {
            "$startDay $startMonthStr $startYear – $endDay $endMonthStr $endYear"
        }
    }

    fun getDateRangeForWeekKey(weekKey: String): String {
        val parts = weekKey.split("_W")
        if (parts.size == 2) {
            val year = parts[0].toIntOrNull()
            val week = parts[1].toIntOrNull()
            if (year != null && week != null) {
                val cal = Calendar.getInstance().apply {
                    firstDayOfWeek = Calendar.SUNDAY
                    set(Calendar.YEAR, year)
                    set(Calendar.WEEK_OF_YEAR, week)
                    set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
                }
                return getSundayWeekDateRange(cal)
            }
        }
        val fallbackCal = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.SUNDAY
            add(Calendar.WEEK_OF_YEAR, -1)
        }
        return getSundayWeekDateRange(fallbackCal)
    }

    fun checkAndResetWeeklyAttendance(): Boolean {
        val currentWeek = getCurrentWeekKey()
        val currentUserId = getCurrentUserId().replace(Regex("[^a-zA-Z0-9_]"), "_")
        val weekKey = KEY_LAST_ATTENDANCE_WEEK + "_" + currentUserId
        val attendanceKey = KEY_ATTENDANCE_PREFIX + currentUserId
        val statsKey = KEY_CURRENT_WEEK_STATS_PREFIX + currentUserId

        val lastWeek = prefs.getString(weekKey, null)
        if (lastWeek != null && lastWeek != currentWeek) {
            // A new Sunday has arrived! Automatically archive previous week's attendance summary
            val lastStatsJson = prefs.getString(statsKey, null)
            val previousArchive = if (lastStatsJson != null) {
                try {
                    gson.fromJson(lastStatsJson, WeeklyAttendanceArchive::class.java)
                } catch (e: Exception) {
                    null
                }
            } else null

            val prevDateRange = getDateRangeForWeekKey(lastWeek)

            val archiveToSave = if (previousArchive != null) {
                previousArchive.copy(
                    weekKey = lastWeek,
                    dateRange = if (previousArchive.dateRange.isNotBlank()) previousArchive.dateRange else prevDateRange
                )
            } else {
                WeeklyAttendanceArchive(
                    weekKey = lastWeek,
                    dateRange = prevDateRange,
                    attendedClasses = 0,
                    totalClasses = 0,
                    percentage = 0
                )
            }

            saveAttendanceArchive(archiveToSave)

            // Reset current week's attendance count & records to 0
            prefs.edit()
                .remove(attendanceKey)
                .remove(statsKey)
                .putString(weekKey, currentWeek)
                .apply()
            return true
        } else if (lastWeek == null) {
            prefs.edit().putString(weekKey, currentWeek).apply()
        }
        return false
    }

    fun getWeeklyAttendanceMap(): Map<String, String> {
        checkAndResetWeeklyAttendance()
        val currentUserId = getCurrentUserId().replace(Regex("[^a-zA-Z0-9_]"), "_")
        val attendanceKey = KEY_ATTENDANCE_PREFIX + currentUserId
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

    fun saveCurrentWeekSummary(attended: Int, total: Int, percentage: Int) {
        val currentUserId = getCurrentUserId().replace(Regex("[^a-zA-Z0-9_]"), "_")
        val statsKey = KEY_CURRENT_WEEK_STATS_PREFIX + currentUserId
        val dateRange = getSundayWeekDateRange()
        val summary = WeeklyAttendanceArchive(
            weekKey = getCurrentWeekKey(),
            dateRange = dateRange,
            attendedClasses = attended,
            totalClasses = total,
            percentage = percentage
        )
        prefs.edit().putString(statsKey, gson.toJson(summary)).apply()
    }

    fun getAttendanceArchives(): List<WeeklyAttendanceArchive> {
        val currentUserId = getCurrentUserId().replace(Regex("[^a-zA-Z0-9_]"), "_")
        val key = KEY_ATTENDANCE_HISTORY_PREFIX + currentUserId
        val json = prefs.getString(key, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<WeeklyAttendanceArchive>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveAttendanceArchive(archive: WeeklyAttendanceArchive) {
        val currentUserId = getCurrentUserId().replace(Regex("[^a-zA-Z0-9_]"), "_")
        val key = KEY_ATTENDANCE_HISTORY_PREFIX + currentUserId
        val currentList = getAttendanceArchives().toMutableList()
        val existingIndex = currentList.indexOfFirst { it.weekKey == archive.weekKey }
        if (existingIndex != -1) {
            currentList[existingIndex] = archive
        } else {
            currentList.add(0, archive)
        }
        prefs.edit().putString(key, gson.toJson(currentList)).apply()

        // Sync to Firebase Realtime Database (Primary) & Firestore (Secondary)
        val rawUid = getAuthenticatedOrSessionUid()
        if (rawUid.isNotBlank() && rawUid != "guest") {
            try {
                val rtdbRef = FirebaseDatabase.getInstance("https://crconnect-58521-default-rtdb.firebaseio.com")
                    .getReference("crconnect_db")
                rtdbRef.child("users").child(rawUid).child("attendance_history").child(archive.weekKey).setValue(archive)
            } catch (e: Exception) {
                // Silently fallback to persistent local prefs
            }

            try {
                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(rawUid)
                    .collection("attendance_history")
                    .document(archive.weekKey)
                    .set(archive)
            } catch (e: Exception) {
                // Silently fallback to persistent local prefs
            }
        }
    }

    fun syncAttendanceArchivesFromFirestore(onComplete: (List<WeeklyAttendanceArchive>) -> Unit = {}) {
        val rawUid = getAuthenticatedOrSessionUid()
        if (rawUid.isBlank() || rawUid == "guest") {
            onComplete(getAttendanceArchives())
            return
        }

        // 1. Primary Check: Firebase Realtime Database
        try {
            val rtdbRef = FirebaseDatabase.getInstance("https://crconnect-58521-default-rtdb.firebaseio.com")
                .getReference("crconnect_db")
            rtdbRef.child("users").child(rawUid).child("attendance_history")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists() && snapshot.childrenCount > 0) {
                            val rtdbList = mutableListOf<WeeklyAttendanceArchive>()
                            for (child in snapshot.children) {
                                val item = child.getValue(WeeklyAttendanceArchive::class.java)
                                if (item != null) rtdbList.add(item)
                            }
                            if (rtdbList.isNotEmpty()) {
                                val localList = getAttendanceArchives().toMutableList()
                                for (remote in rtdbList) {
                                    if (localList.none { it.weekKey == remote.weekKey }) {
                                        localList.add(remote)
                                    }
                                }
                                localList.sortByDescending { it.weekKey }
                                val currentUserId = getCurrentUserId().replace(Regex("[^a-zA-Z0-9_]"), "_")
                                val key = KEY_ATTENDANCE_HISTORY_PREFIX + currentUserId
                                prefs.edit().putString(key, gson.toJson(localList)).apply()
                                onComplete(localList)
                                return
                            }
                        }

                        // 2. Secondary Check: Firestore
                        checkFirestoreArchives(rawUid, onComplete)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        checkFirestoreArchives(rawUid, onComplete)
                    }
                })
        } catch (e: Exception) {
            checkFirestoreArchives(rawUid, onComplete)
        }
    }

    private fun checkFirestoreArchives(rawUid: String, onComplete: (List<WeeklyAttendanceArchive>) -> Unit) {
        try {
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(rawUid)
                .collection("attendance_history")
                .get()
                .addOnSuccessListener { snapshot ->
                    val remoteList = mutableListOf<WeeklyAttendanceArchive>()
                    for (doc in snapshot.documents) {
                        val item = doc.toObject(WeeklyAttendanceArchive::class.java)
                        if (item != null) remoteList.add(item)
                    }
                    if (remoteList.isNotEmpty()) {
                        val localList = getAttendanceArchives().toMutableList()
                        for (remote in remoteList) {
                            if (localList.none { it.weekKey == remote.weekKey }) {
                                localList.add(remote)
                            }
                        }
                        localList.sortByDescending { it.weekKey }
                        val currentUserId = getCurrentUserId().replace(Regex("[^a-zA-Z0-9_]"), "_")
                        val key = KEY_ATTENDANCE_HISTORY_PREFIX + currentUserId
                        prefs.edit().putString(key, gson.toJson(localList)).apply()
                        onComplete(localList)
                    } else {
                        onComplete(getAttendanceArchives())
                    }
                }
                .addOnFailureListener {
                    onComplete(getAttendanceArchives())
                }
        } catch (e: Exception) {
            onComplete(getAttendanceArchives())
        }
    }

    companion object {
        private const val PREF_NAME = "crconnect_routine_preferences"
        private const val KEY_ENTRIES_JSON = "key_parsed_class_entries"
        private const val KEY_USER_ENTRIES_PREFIX = "key_user_entries_"
        private const val KEY_OWNER_ID = "key_routine_owner_id"
        private const val KEY_ATTENDANCE_PREFIX = "key_weekly_attendance_"
        private const val KEY_LAST_ATTENDANCE_WEEK = "key_last_attendance_week_"
        private const val KEY_ATTENDANCE_HISTORY_PREFIX = "key_attendance_history_"
        private const val KEY_CURRENT_WEEK_STATS_PREFIX = "key_current_week_stats_"
    }
}
