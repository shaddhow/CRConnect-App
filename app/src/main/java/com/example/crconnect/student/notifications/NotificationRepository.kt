package com.example.crconnect.student.notifications

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

object NotificationRepository {
    private const val PREFS_NAME = "crconnect_notifications_store"
    private const val KEY_NOTIFICATIONS = "notifications_list"
    private const val KEY_CLASS_REMINDER_ENABLED = "pref_class_reminder_enabled"
    private const val KEY_REMINDER_MINUTES = "pref_reminder_minutes"
    private const val KEY_MORNING_BRIEFING_ENABLED = "pref_morning_briefing_enabled"
    private const val KEY_CR_ANNOUNCEMENT_ENABLED = "pref_cr_announcement_enabled"
    private const val KEY_ATTENDANCE_ALERT_ENABLED = "pref_attendance_alert_enabled"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    @Synchronized
    fun getNotifications(context: Context): List<InAppNotificationModel> {
        val prefs = getPrefs(context)
        val jsonStr = prefs.getString(KEY_NOTIFICATIONS, null) ?: return emptyList()
        val list = mutableListOf<InAppNotificationModel>()
        try {
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val typeStr = obj.optString("type", NotificationType.GENERAL.name)
                val type = try {
                    NotificationType.valueOf(typeStr)
                } catch (e: Exception) {
                    NotificationType.GENERAL
                }
                list.add(
                    InAppNotificationModel(
                        id = obj.optString("id", System.currentTimeMillis().toString()),
                        title = obj.optString("title", ""),
                        body = obj.optString("body", ""),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        type = type,
                        isRead = obj.optBoolean("isRead", false),
                        actionData = if (obj.has("actionData")) obj.optString("actionData") else null
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list.sortedByDescending { it.timestamp }
    }

    @Synchronized
    fun saveNotification(context: Context, item: InAppNotificationModel) {
        val currentList = getNotifications(context).toMutableList()
        // Deduplicate recent identical notification within 5 minutes
        val isDuplicate = currentList.any { 
            it.title == item.title && it.body == item.body && kotlin.math.abs(it.timestamp - item.timestamp) < 300000L 
        }
        if (isDuplicate) return

        currentList.add(0, item)
        // Keep max 50 recent items to maintain fast performance
        val trimmed = if (currentList.size > 50) currentList.take(50) else currentList
        saveList(context, trimmed)
    }

    @Synchronized
    fun markAllAsRead(context: Context) {
        val currentList = getNotifications(context).map { it.copy(isRead = true) }
        saveList(context, currentList)
    }

    @Synchronized
    fun markAsRead(context: Context, id: String) {
        val currentList = getNotifications(context).map {
            if (it.id == id) it.copy(isRead = true) else it
        }
        saveList(context, currentList)
    }

    @Synchronized
    fun clearAll(context: Context) {
        getPrefs(context).edit().remove(KEY_NOTIFICATIONS).apply()
    }

    @Synchronized
    fun getUnreadCount(context: Context): Int {
        return getNotifications(context).count { !it.isRead }
    }

    private fun saveList(context: Context, list: List<InAppNotificationModel>) {
        try {
            val jsonArray = JSONArray()
            for (item in list) {
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("title", item.title)
                    put("body", item.body)
                    put("timestamp", item.timestamp)
                    put("type", item.type.name)
                    put("isRead", item.isRead)
                    item.actionData?.let { put("actionData", it) }
                }
                jsonArray.put(obj)
            }
            getPrefs(context).edit().putString(KEY_NOTIFICATIONS, jsonArray.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // User Notification Preferences
    fun isClassReminderEnabled(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_CLASS_REMINDER_ENABLED, true)

    fun setClassReminderEnabled(context: Context, enabled: Boolean) =
        getPrefs(context).edit().putBoolean(KEY_CLASS_REMINDER_ENABLED, enabled).apply()

    fun getReminderMinutes(context: Context): Int =
        getPrefs(context).getInt(KEY_REMINDER_MINUTES, 15)

    fun setReminderMinutes(context: Context, minutes: Int) =
        getPrefs(context).edit().putInt(KEY_REMINDER_MINUTES, minutes).apply()

    fun isMorningBriefingEnabled(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_MORNING_BRIEFING_ENABLED, true)

    fun setMorningBriefingEnabled(context: Context, enabled: Boolean) =
        getPrefs(context).edit().putBoolean(KEY_MORNING_BRIEFING_ENABLED, enabled).apply()

    fun isCrAnnouncementEnabled(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_CR_ANNOUNCEMENT_ENABLED, true)

    fun setCrAnnouncementEnabled(context: Context, enabled: Boolean) =
        getPrefs(context).edit().putBoolean(KEY_CR_ANNOUNCEMENT_ENABLED, enabled).apply()

    fun isAttendanceAlertEnabled(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_ATTENDANCE_ALERT_ENABLED, true)

    fun setAttendanceAlertEnabled(context: Context, enabled: Boolean) =
        getPrefs(context).edit().putBoolean(KEY_ATTENDANCE_ALERT_ENABLED, enabled).apply()
}

