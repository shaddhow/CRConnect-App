package com.example.crconnect.student.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.crconnect.student.ClassModel
import com.example.crconnect.R
import com.example.crconnect.StudentDashboardActivity
import java.util.Calendar
import java.util.Locale

object RoutineNotificationHelper {
    private const val TAG = "RoutineNotifHelper"

    const val CHANNEL_CLASS_REMINDERS = "crconnect_class_reminders"
    const val CHANNEL_CR_NOTICES = "crconnect_cr_notices"
    const val CHANNEL_ATTENDANCE = "crconnect_attendance_alerts"

    const val ACTION_CLASS_REMINDER = "com.example.crconnect.ACTION_CLASS_REMINDER"
    const val ACTION_MORNING_BRIEFING = "com.example.crconnect.ACTION_MORNING_BRIEFING"

    const val EXTRA_COURSE_NAME = "extra_course_name"
    const val EXTRA_TIME_SLOT = "extra_time_slot"
    const val EXTRA_ROOM = "extra_room"
    const val EXTRA_CLASS_COUNT = "extra_class_count"
    const val EXTRA_DAY_NAME = "extra_day_name"

    fun initNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java) ?: return

            // 1. Class Reminders Channel
            val reminderChannel = NotificationChannel(
                CHANNEL_CLASS_REMINDERS,
                "Class Reminders & Schedule",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Timely alerts 15 minutes before your upcoming classes and room assignments"
                enableVibration(true)
                setShowBadge(true)
            }

            // 2. CR Announcements Channel
            val noticeChannel = NotificationChannel(
                CHANNEL_CR_NOTICES,
                "CR Announcements & Urgent Notices",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Instant notifications when your CR broadcasts class changes, submissions, or notices"
                enableVibration(true)
                setShowBadge(true)
            }

            // 3. Attendance Alerts Channel
            val attendanceChannel = NotificationChannel(
                CHANNEL_ATTENDANCE,
                "Attendance & Academic Warnings",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Warning alerts if your course attendance drops below the 75% exam eligibility threshold"
                enableVibration(true)
                setShowBadge(true)
            }

            manager.createNotificationChannel(reminderChannel)
            manager.createNotificationChannel(noticeChannel)
            manager.createNotificationChannel(attendanceChannel)
        }
    }

    /**
     * Schedules alarms for all classes in the student's weekly routine.
     */
    fun scheduleAllRoutines(context: Context, classMap: Map<Int, List<ClassModel>>) {
        if (!NotificationRepository.isClassReminderEnabled(context)) {
            cancelAllRoutineAlarms(context)
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val reminderMins = NotificationRepository.getReminderMinutes(context)

        // 1. Cancel previous pending alarms
        cancelAllRoutineAlarms(context)

        var alarmRequestCode = 1000

        classMap.forEach { (dayIndex, classes) ->
            if (classes.isEmpty()) return@forEach
            val calendarDayOfWeek = mapBubtDayIndexToCalendarDay(dayIndex)

            // Schedule morning briefing at 8:00 AM on this class day
            if (NotificationRepository.isMorningBriefingEnabled(context)) {
                scheduleMorningBriefingAlarm(context, alarmManager, calendarDayOfWeek, dayIndex, classes, alarmRequestCode++)
            }

            // Schedule individual 15-min-before class alarms
            classes.forEach { classModel ->
                val timeParts = parseStartTimeMinutes(classModel.timeSlot)
                if (timeParts != null) {
                    val (hour, minute) = timeParts
                    val alarmCal = Calendar.getInstance().apply {
                        set(Calendar.DAY_OF_WEEK, calendarDayOfWeek)
                        set(Calendar.HOUR_OF_DAY, hour)
                        set(Calendar.MINUTE, minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                        add(Calendar.MINUTE, -reminderMins)

                        // If already passed for this week, set for next week
                        if (timeInMillis <= System.currentTimeMillis()) {
                            add(Calendar.DAY_OF_YEAR, 7)
                        }
                    }

                    val intent = Intent(context, RoutineAlarmReceiver::class.java).apply {
                        action = ACTION_CLASS_REMINDER
                        putExtra(EXTRA_COURSE_NAME, classModel.courseName)
                        putExtra(EXTRA_TIME_SLOT, classModel.timeSlot)
                        putExtra(EXTRA_ROOM, classModel.room)
                    }

                    val pendingIntent = PendingIntent.getBroadcast(
                        context,
                        alarmRequestCode++,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            alarmManager.setExactAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                alarmCal.timeInMillis,
                                pendingIntent
                            )
                        } else {
                            alarmManager.setExact(
                                AlarmManager.RTC_WAKEUP,
                                alarmCal.timeInMillis,
                                pendingIntent
                            )
                        }
                    } catch (e: SecurityException) {
                        Log.w(TAG, "Cannot set exact alarm, falling back to standard alarm: ${e.message}")
                        alarmManager.set(
                            AlarmManager.RTC_WAKEUP,
                            alarmCal.timeInMillis,
                            pendingIntent
                        )
                    }
                }
            }
        }
        Log.d(TAG, "Scheduled ${alarmRequestCode - 1000} alarms successfully")
    }

    private fun scheduleMorningBriefingAlarm(
        context: Context,
        alarmManager: AlarmManager,
        calendarDayOfWeek: Int,
        dayIndex: Int,
        classes: List<ClassModel>,
        requestCode: Int
    ) {
        val briefingCal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, calendarDayOfWeek)
            set(Calendar.HOUR_OF_DAY, 8)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 7)
            }
        }

        val firstClass = classes.firstOrNull()
        val intent = Intent(context, RoutineAlarmReceiver::class.java).apply {
            action = ACTION_MORNING_BRIEFING
            putExtra(EXTRA_DAY_NAME, getDayNameByIndex(dayIndex))
            putExtra(EXTRA_CLASS_COUNT, classes.size)
            putExtra(EXTRA_TIME_SLOT, firstClass?.timeSlot ?: "")
            putExtra(EXTRA_COURSE_NAME, firstClass?.courseName ?: "")
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            alarmManager.set(AlarmManager.RTC_WAKEUP, briefingCal.timeInMillis, pendingIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun cancelAllRoutineAlarms(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        for (i in 1000..1200) {
            val intent = Intent(context, RoutineAlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                i,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }
    }

    /**
     * Show high-priority heads-up notification 15 mins before an upcoming class.
     */
    fun showClassReminderNotification(
        context: Context,
        courseName: String,
        timeSlot: String,
        room: String
    ) {
        initNotificationChannels(context)

        val cleanCourse = if (courseName.contains("(")) courseName.substringBefore("(").trim() else courseName
        val cleanRoom = if (room.startsWith("Room", ignoreCase = true)) room else "Room $room"
        val cleanTime = timeSlot.split("-", "–").firstOrNull()?.trim() ?: timeSlot

        val title = "Class in 15 mins: $cleanCourse"
        val body = "Starts at $cleanTime in $cleanRoom. Get ready!"

        // Save to in-app notification center
        NotificationRepository.saveNotification(
            context,
            InAppNotificationModel(
                title = title,
                body = body,
                type = NotificationType.CLASS_REMINDER,
                actionData = timeSlot
            )
        )

        val intent = Intent(context, StudentDashboardActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            (System.currentTimeMillis() % 10000).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notification = NotificationCompat.Builder(context, CHANNEL_CLASS_REMINDERS)
            .setSmallIcon(R.drawable.ic_nav_routine)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$body\nCourse: $courseName\nAssigned: $cleanRoom"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setAutoCancel(true)
            .setSound(soundUri)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(courseName.hashCode(), notification)
    }

    /**
     * Show morning briefing notification at 8:00 AM on class days.
     */
    fun showMorningBriefingNotification(
        context: Context,
        dayName: String,
        classCount: Int,
        firstTime: String,
        firstCourse: String
    ) {
        initNotificationChannels(context)

        val title = "Good morning! Classes on $dayName"
        val body = if (classCount > 0) {
            "You have $classCount ${if (classCount == 1) "class" else "classes"} today, starting with $firstCourse at $firstTime."
        } else {
            "No classes scheduled for today. Enjoy your day off!"
        }

        NotificationRepository.saveNotification(
            context,
            InAppNotificationModel(
                title = title,
                body = body,
                type = NotificationType.CLASS_REMINDER
            )
        )

        val intent = Intent(context, StudentDashboardActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            (System.currentTimeMillis() % 10000).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_CLASS_REMINDERS)
            .setSmallIcon(R.drawable.ic_nav_routine)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(8001, notification)
    }

    /**
     * Show instant push notification when CR posts an announcement.
     */
    fun showCrNoticeNotification(
        context: Context,
        title: String,
        body: String,
        postedBy: String
    ) {
        if (!NotificationRepository.isCrAnnouncementEnabled(context)) return
        initNotificationChannels(context)

        val notifTitle = "CR Notice: $title"
        val notifBody = if (postedBy.isNotBlank()) "$body (by $postedBy)" else body

        NotificationRepository.saveNotification(
            context,
            InAppNotificationModel(
                title = notifTitle,
                body = notifBody,
                type = NotificationType.CR_NOTICE
            )
        )

        val intent = Intent(context, StudentDashboardActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            (System.currentTimeMillis() % 10000).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notification = NotificationCompat.Builder(context, CHANNEL_CR_NOTICES)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(notifTitle)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$body\n\nPosted by: $postedBy"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setSound(soundUri)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify((title + body).hashCode(), notification)
    }

    /**
     * Show warning notification when course attendance drops below 75%.
     */
    fun showAttendanceAlertNotification(
        context: Context,
        courseCode: String,
        percentage: Int
    ) {
        if (!NotificationRepository.isAttendanceAlertEnabled(context)) return
        initNotificationChannels(context)

        val title = "Attendance Alert: $courseCode"
        val body = "Your attendance is currently $percentage%. Attend upcoming classes to stay above the 75% exam safe zone."

        NotificationRepository.saveNotification(
            context,
            InAppNotificationModel(
                title = title,
                body = body,
                type = NotificationType.ATTENDANCE_ALERT
            )
        )

        val intent = Intent(context, StudentDashboardActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            (System.currentTimeMillis() % 10000).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ATTENDANCE)
            .setSmallIcon(R.drawable.ic_nav_attendance)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(courseCode.hashCode(), notification)
    }

    private fun mapBubtDayIndexToCalendarDay(dayIndex: Int): Int {
        return when (dayIndex) {
            0 -> Calendar.SATURDAY
            1 -> Calendar.SUNDAY
            2 -> Calendar.MONDAY
            3 -> Calendar.TUESDAY
            4 -> Calendar.WEDNESDAY
            5 -> Calendar.THURSDAY
            6 -> Calendar.FRIDAY
            else -> Calendar.SATURDAY
        }
    }

    private fun getDayNameByIndex(dayIndex: Int): String {
        return when (dayIndex) {
            0 -> "Saturday"
            1 -> "Sunday"
            2 -> "Monday"
            3 -> "Tuesday"
            4 -> "Wednesday"
            5 -> "Thursday"
            6 -> "Friday"
            else -> "Today"
        }
    }

    private fun parseStartTimeMinutes(timeSlot: String): Pair<Int, Int>? {
        return try {
            val startPart = timeSlot.split("-", "–").firstOrNull()?.trim() ?: return null
            val isPm = startPart.contains("PM", ignoreCase = true)
            val isAm = startPart.contains("AM", ignoreCase = true)
            val cleanTime = startPart.replace("AM", "", ignoreCase = true).replace("PM", "", ignoreCase = true).trim()
            val colonParts = cleanTime.split(":")
            var hour = colonParts[0].trim().toInt()
            val min = if (colonParts.size > 1) colonParts[1].trim().toInt() else 0
            if (isPm && hour < 12) hour += 12
            if (isAm && hour == 12) hour = 0
            Pair(hour, min)
        } catch (e: Exception) {
            null
        }
    }
}

