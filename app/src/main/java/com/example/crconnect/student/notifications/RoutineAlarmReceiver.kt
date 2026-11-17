package com.example.crconnect.student.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.crconnect.RoutineLocalCache
import com.example.crconnect.student.ClassModel

class RoutineAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return
        val action = intent.action ?: return
        Log.d("RoutineAlarmReceiver", "Alarm received: $action")

        when (action) {
            RoutineNotificationHelper.ACTION_CLASS_REMINDER -> {
                val courseName = intent.getStringExtra(RoutineNotificationHelper.EXTRA_COURSE_NAME) ?: "Upcoming Class"
                val timeSlot = intent.getStringExtra(RoutineNotificationHelper.EXTRA_TIME_SLOT) ?: "Upcoming"
                val room = intent.getStringExtra(RoutineNotificationHelper.EXTRA_ROOM) ?: "TBA"

                RoutineNotificationHelper.showClassReminderNotification(
                    context = context,
                    courseName = courseName,
                    timeSlot = timeSlot,
                    room = room
                )
            }
            RoutineNotificationHelper.ACTION_MORNING_BRIEFING -> {
                val dayName = intent.getStringExtra(RoutineNotificationHelper.EXTRA_DAY_NAME) ?: "Today"
                val classCount = intent.getIntExtra(RoutineNotificationHelper.EXTRA_CLASS_COUNT, 0)
                val firstTime = intent.getStringExtra(RoutineNotificationHelper.EXTRA_TIME_SLOT) ?: ""
                val firstCourse = intent.getStringExtra(RoutineNotificationHelper.EXTRA_COURSE_NAME) ?: ""

                RoutineNotificationHelper.showMorningBriefingNotification(
                    context = context,
                    dayName = dayName,
                    classCount = classCount,
                    firstTime = firstTime,
                    firstCourse = firstCourse
                )
            }
            Intent.ACTION_BOOT_COMPLETED -> {
                Log.d("RoutineAlarmReceiver", "Device rebooted. Rescheduling routine alarms...")
                // Reload routine from local cache and reschedule
                try {
                    val cachedRoutine = RoutineLocalCache.loadRoutineLocally(context)
                    if (cachedRoutine != null && cachedRoutine.isNotEmpty()) {
                        val daysList = listOf("SAT", "SUN", "MON", "TUE", "WED", "THR", "FRI")
                        val classMap = mutableMapOf<Int, MutableList<ClassModel>>()
                        cachedRoutine.forEach { entry ->
                            val dIdx = daysList.indexOfFirst { it.equals(entry.getDay(), ignoreCase = true) }
                            if (dIdx >= 0) {
                                val code = entry.getDisplayCourseCode()
                                val slot = if (!entry.timeSlot.isNullOrBlank()) entry.timeSlot else "${entry.getStartTime()} - ${entry.getEndTime()}"
                                val faculty = entry.getDisplayFaculty()
                                val room = entry.getRoomNumber()
                                classMap.getOrPut(dIdx) { mutableListOf() }.add(
                                    ClassModel(
                                        id = "${code}_${slot}",
                                        timeSlot = slot,
                                        courseName = if (faculty.isNotBlank() && faculty != "Faculty") "$code ($faculty)" else code,
                                        room = room
                                    )
                                )
                            }
                        }
                        RoutineNotificationHelper.scheduleAllRoutines(context, classMap)
                    }
                } catch (e: Exception) {
                    Log.e("RoutineAlarmReceiver", "Failed to reschedule on boot", e)
                }
            }
        }
    }
}

