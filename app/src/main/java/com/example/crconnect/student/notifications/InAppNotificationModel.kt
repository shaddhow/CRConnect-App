package com.example.crconnect.student.notifications

enum class NotificationType {
    CLASS_REMINDER,
    CR_NOTICE,
    ATTENDANCE_ALERT,
    ELECTION_UPDATE,
    GENERAL
}

data class InAppNotificationModel @JvmOverloads constructor(
    val id: String = System.currentTimeMillis().toString(),
    val title: String,
    val body: String,
    val timestamp: Long = System.currentTimeMillis(),
    val type: NotificationType = NotificationType.GENERAL,
    val isRead: Boolean = false,
    val actionData: String? = null
)

