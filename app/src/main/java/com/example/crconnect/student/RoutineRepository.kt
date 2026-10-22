package com.example.crconnect.student

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Locale

class RoutineRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

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

        val json = gson.toJson(existing)
        prefs.edit().putString(KEY_ENTRIES_JSON, json).apply()
    }

    fun getClassEntries(): List<ClassEntry> {
        val json = prefs.getString(KEY_ENTRIES_JSON, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<ClassEntry>>() {}.type
            gson.fromJson<List<ClassEntry>>(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getEntriesForDay(day: String): List<ClassEntry> {
        val normTarget = normalizeDay(day)
        return getClassEntries().filter { normalizeDay(it.day) == normTarget }
    }

    fun getDaysWithClasses(): Set<String> {
        return getClassEntries().map { normalizeDay(it.day) }.toSet()
    }

    fun clearEntries() {
        prefs.edit().remove(KEY_ENTRIES_JSON).apply()
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

    companion object {
        private const val PREF_NAME = "crconnect_routine_preferences"
        private const val KEY_ENTRIES_JSON = "key_parsed_class_entries"
    }
}
