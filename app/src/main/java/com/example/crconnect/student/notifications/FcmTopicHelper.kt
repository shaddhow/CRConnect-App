package com.example.crconnect.student.notifications

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging

object FcmTopicHelper {
    private const val TAG = "FcmTopicHelper"
    const val TOPIC_GLOBAL = "cr_notice_all"

    @JvmStatic
    fun sanitizeTopic(name: String): String {
        return name.trim().replace(Regex("[^a-zA-Z0-9-_.~%]"), "_")
    }

    @JvmStatic
    fun getSectionTopic(dept: String, sec: String): String {
        val sDept = sanitizeTopic(dept)
        val sSec = sanitizeTopic(sec)
        return "cr_notice_${sDept}_${sSec}"
    }

    @JvmStatic
    fun subscribeStudent(dept: String, sec: String) {
        val fcm = FirebaseMessaging.getInstance()

        // 1. Subscribe to Global Announcements
        fcm.subscribeToTopic(TOPIC_GLOBAL).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d(TAG, "Subscribed to global topic: $TOPIC_GLOBAL")
            } else {
                Log.w(TAG, "Failed to subscribe to global topic", task.exception)
            }
        }

        // 2. Subscribe to Section Announcements
        if (dept.isNotBlank() && sec.isNotBlank()) {
            val sectionTopic = getSectionTopic(dept, sec)
            fcm.subscribeToTopic(sectionTopic).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Subscribed to section topic: $sectionTopic")
                } else {
                    Log.w(TAG, "Failed to subscribe to section topic: $sectionTopic", task.exception)
                }
            }
        }
    }

    @JvmStatic
    fun unsubscribeStudent(dept: String, sec: String) {
        val fcm = FirebaseMessaging.getInstance()
        try {
            fcm.unsubscribeFromTopic(TOPIC_GLOBAL)
            if (dept.isNotBlank() && sec.isNotBlank()) {
                val sectionTopic = getSectionTopic(dept, sec)
                fcm.unsubscribeFromTopic(sectionTopic)
            }
            Log.d(TAG, "Unsubscribed from student topics")
        } catch (e: Exception) {
            Log.w(TAG, "Error unsubscribing student from topics", e)
        }
    }
}
