package com.example.crconnect.student

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import java.io.ByteArrayOutputStream
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.crconnect.ProfileImageCacheHelper
import com.example.crconnect.RoutineLocalCache
import com.example.crconnect.SessionManager
import com.example.crconnect.ToastHelper
import com.example.crconnect.core.utils.AppUpdateHelper
import com.example.crconnect.student.notifications.FcmTopicHelper
import com.example.crconnect.student.notifications.RoutineNotificationHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar
import java.util.Locale

class StudentPortalViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(StudentPortalUiState())
    val uiState: StateFlow<StudentPortalUiState> = _uiState.asStateFlow()

    val uniqueCourseCount: StateFlow<Int> = _uiState
        .map { state -> computeUniqueCoursesCount(state.classMap) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    private var dbRef: DatabaseReference? = null
    private var firestoreRegistration: ListenerRegistration? = null
    private var rtdbRoutineListener: ValueEventListener? = null
    private var rtdbRoutineRef: DatabaseReference? = null

    private var sanitizedDept: String = "Dept_of_CSE"
    private var sanitizedSec: String = "6"
    private var sanitizedStudentId: String = "voter"
    private var rawDept: String = "Dept. of CSE"
    private var rawSec: String = "6"
    private var rawIntake: String = "55"

    private var isInitialized = false
    private var isFirstAnnouncementLoad = true
    private var appContext: Context? = null

    // Realtime Database listeners & references
    private var candidatesRef: DatabaseReference? = null
    private var candidatesListener: ValueEventListener? = null

    private var gateControlRef: DatabaseReference? = null
    private var gateControlListener: ValueEventListener? = null

    private var voterRef: DatabaseReference? = null
    private var voterListener: ValueEventListener? = null

    private var announcementsRef: DatabaseReference? = null
    private var announcementsListener: ChildEventListener? = null

    private var userRoleRef: DatabaseReference? = null
    private var userRoleListener: ValueEventListener? = null

    private var userProfileImgRef: DatabaseReference? = null
    private var userProfileImgListener: ValueEventListener? = null

    private var userCgpaRef: DatabaseReference? = null
    private var userCgpaListener: ValueEventListener? = null

    private var userLiveProfileRef: DatabaseReference? = null
    private var userLiveProfileListener: ValueEventListener? = null

    private val timerHandler = Handler(Looper.getMainLooper())
    private var timerRunnable: Runnable? = null
    private var endTimeMillis: Long = 0L

    fun initSession(context: Context) {
        if (isInitialized) return
        isInitialized = true
        appContext = context.applicationContext

        val session = SessionManager(context)
        val storedDept = session.userDept.ifBlank { "Department of CSE, BUBT" }
        rawDept = if (storedDept.equals("English (ENG)", ignoreCase = true) || storedDept.equals("ENG", ignoreCase = true)) {
            val correctedDept = "Department of CSE, BUBT"
            session.setUserDept(correctedDept)
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            val dbRef = FirebaseDatabase.getInstance("https://crconnect-58521-default-rtdb.firebaseio.com").getReference("crconnect_db")
            if (uid != null) {
                dbRef.child("users").child(uid).child("dept").setValue(correctedDept)
                dbRef.child("users").child(uid).child("department").setValue(correctedDept)
            }
            val rawId = session.userID.ifBlank { "student" }.replace(Regex("[^a-zA-Z0-9_]"), "_")
            if (rawId.isNotBlank()) {
                dbRef.child("users").child(rawId).child("dept").setValue(correctedDept)
                dbRef.child("users").child(rawId).child("department").setValue(correctedDept)
            }
            correctedDept
        } else {
            storedDept
        }
        rawIntake = session.userIntake.ifBlank { "55" }
        val secStr = if (session.userRole.contains("Teacher", ignoreCase = true)) session.assignedSection else session.userSection
        rawSec = if (secStr.isNotBlank() && secStr != "--") secStr else "6"
        val storedId = session.userID
        val validStudentId = if (storedId.isNotBlank() && storedId != "--" && !storedId.contains("@") && !storedId.equals(session.userEmail, ignoreCase = true)) {
            storedId
        } else {
            "--"
        }
        val rawId = if (validStudentId != "--") validStudentId else "student"

        sanitizedDept = sanitizeDepartment(rawDept)
        sanitizedSec = sanitizeSection(rawSec)
        sanitizedStudentId = rawId.replace(Regex("[^a-zA-Z0-9_]"), "_")

        // Auto subscribe to section FCM announcements topic
        FcmTopicHelper.subscribeStudent(sanitizedDept, sanitizedSec)

        val studentName = session.userName.ifBlank { "Student" }
        val initials = computeInitials(studentName)
        val isUserCr = session.userRole.contains("CR", ignoreCase = true)

        var term = session.currentTerm
        if (term.contains("Spring 2025 (Year 3)", ignoreCase = true)) {
            term = "--"
            session.setCurrentTerm("--")
        }

        val authUid = FirebaseAuth.getInstance().currentUser?.uid
        val localCachedPath = ProfileImageCacheHelper.getValidLocalProfilePath(
            context,
            validStudentId,
            storedId,
            session.userEmail,
            authUid
        )
        val resolvedPhotoUrl = session.profileImageUrl.ifBlank { localCachedPath ?: "" }
        if (session.profileImageUrl.isBlank() && !localCachedPath.isNullOrBlank()) {
            session.setProfileImageUrl(localCachedPath)
        }

        val profileModel = UserProfileModel(
            name = studentName,
            email = session.userEmail.ifBlank { "student@gmail.com" },
            studentId = validStudentId,
            intake = session.userIntake.ifBlank { "--" },
            section = rawSec,
            department = rawDept,
            initials = initials,
            cgpa = session.studentCgpa,
            profileImageUrl = resolvedPhotoUrl,
            isCr = isUserCr,
            advisorName = session.advisorName.ifBlank { "Nasrin Akter" },
            advisorEmail = session.advisorEmail.ifBlank { "nasrinshipa@bubt.edu.bd" },
            currentTerm = term.ifBlank { "--" }
        )

        val todayIdx = getTodayDayIndex()
        val currentThemeMode = session.studentThemeMode
        _uiState.update {
            it.copy(
                profile = profileModel,
                isCr = isUserCr,
                selectedDayIndex = todayIdx,
                themeMode = currentThemeMode
            )
        }

        // Check for app updates on GitHub Releases in background
        checkForAppUpdates(context, forceCheck = false)

        // Background caching of profile image if it's a remote URL or Base64
        val rawPhotoUrl = resolvedPhotoUrl
        if (rawPhotoUrl.isNotBlank() && (rawPhotoUrl.startsWith("http://") || rawPhotoUrl.startsWith("https://") || rawPhotoUrl.startsWith("content://") || rawPhotoUrl.startsWith("data:") || rawPhotoUrl.contains("base64,"))) {
            ProfileImageCacheHelper.saveAndCacheProfileImage(context, rawPhotoUrl, validStudentId, storedId, session.userEmail, authUid) { cachedPath ->
                if (cachedPath.isNotBlank() && cachedPath != rawPhotoUrl) {
                    session.setProfileImageUrl(cachedPath)
                    _uiState.update { currentState ->
                        currentState.copy(profile = currentState.profile.copy(profileImageUrl = cachedPath))
                    }
                }
            }
        }

        val repo = RoutineRepository(context)
        repo.checkAndResetWeeklyAttendance()
        loadAttendanceHistory(context)

        val savedEntries = repo.getClassEntries().filterNot { entry ->
            (entry.course == "CSE 301 · Algorithms" || entry.course == "CSE 305 · Database" || entry.course == "MAT 211 · Numerical Methods") && entry.faculty == "TBA"
        }
        if (savedEntries.isNotEmpty()) {
            repo.replaceClassEntries(savedEntries)
            loadRoutineEntriesIntoState(savedEntries, context)
        }
        // Background sync: load latest cloud routine silently without blocking UI
        loadFirestoreRoutineToCache(context)

        setupFirebaseRealtimeDatabase()
    }

    fun getTodayDayIndex(): Int {
        val cal = Calendar.getInstance()
        return when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SATURDAY -> 0
            Calendar.SUNDAY -> 1
            Calendar.MONDAY -> 2
            Calendar.TUESDAY -> 3
            Calendar.WEDNESDAY -> 4
            Calendar.THURSDAY -> 5
            Calendar.FRIDAY -> 6
            else -> 0
        }
    }

    private fun loadRoutineEntriesIntoState(entries: List<ClassEntry>, context: Context? = null) {
        try {
            if (entries.isEmpty()) return
            val daysList = listOf("SAT", "SUN", "MON", "TUE", "WED", "THR", "FRI")
            val loadedMap = mutableMapOf<Int, MutableList<ClassModel>>()

            val attendanceMap = context?.let { RoutineRepository(it).getWeeklyAttendanceMap() } ?: emptyMap()

            for (entry in entries) {
                val normDay = when {
                    entry.day.contains("SAT", ignoreCase = true) -> "SAT"
                    entry.day.contains("SUN", ignoreCase = true) -> "SUN"
                    entry.day.contains("MON", ignoreCase = true) -> "MON"
                    entry.day.contains("TUE", ignoreCase = true) -> "TUE"
                    entry.day.contains("WED", ignoreCase = true) -> "WED"
                    entry.day.contains("TH", ignoreCase = true) -> "THR"
                    entry.day.contains("FRI", ignoreCase = true) -> "FRI"
                    else -> entry.day.trim().uppercase(Locale.ROOT)
                }
                val dayIdx = daysList.indexOfFirst { it.equals(normDay, ignoreCase = true) }
                val targetIdx = if (dayIdx != -1) dayIdx else 0
                val timeSlot = "${entry.startTime} - ${entry.endTime}"
                val courseDisplay = if (entry.faculty.isNotBlank() && entry.faculty != "TBA" && !entry.course.contains("(")) {
                    "${entry.course} (${entry.faculty})"
                } else {
                    entry.course
                }
                val roomDisplay = if (entry.room.startsWith("Room") || entry.room.startsWith("TBA")) entry.room else "Room ${entry.room}"
                val classId = "${entry.day}_${entry.course}_${entry.startTime}"

                val classModel = ClassModel(
                    id = classId,
                    timeSlot = timeSlot,
                    courseName = courseDisplay,
                    room = roomDisplay,
                    attendanceState = attendanceMap[classId]
                )
                val currentList = loadedMap.getOrPut(targetIdx) { mutableListOf() }
                if (!currentList.any { it.courseName == classModel.courseName && it.timeSlot == classModel.timeSlot }) {
                    currentList.add(classModel)
                }
            }

            loadedMap.values.forEach { list ->
                list.sortBy { startMinutes(it.timeSlot) }
            }

            val updatedDays = _uiState.value.scheduleDays.map { day ->
                val hasClasses = (loadedMap[day.dayIndex]?.isNotEmpty() == true)
                day.copy(hasClasses = hasClasses)
            }

            _uiState.update { currentState ->
                currentState.copy(
                    classMap = loadedMap,
                    scheduleDays = updatedDays
                )
            }
        } catch (e: Exception) {
            Log.e("StudentPortalViewModel", "Error updating routine entries state", e)
        }
    }

    private fun loadFirestoreRoutineToCache(context: Context) {
        try {
            val session = SessionManager(context)
            val authUid = FirebaseAuth.getInstance().currentUser?.uid
            val uid = if (!authUid.isNullOrBlank()) authUid else session.userID.ifBlank { "" }

            val rtdb = FirebaseDatabase.getInstance("https://crconnect-58521-default-rtdb.firebaseio.com")
            val rtdbRef = rtdb.getReference("crconnect_db")
            val db = FirebaseFirestore.getInstance()
            val deptKey = sanitizeDepartment(session.userDept.ifBlank { rawDept })
            val rawDeptKey = getSanitizedKey(session.userDept.ifBlank { rawDept })
            val intakeKey = getSanitizedKey(session.userIntake.ifBlank { rawIntake })
            val secKey = sanitizeSection(session.userSection.ifBlank { rawSec })
            val rawSecKey = getSanitizedKey(session.userSection.ifBlank { rawSec })

            fun parseRtdbSnapshotToEntries(snapshot: DataSnapshot): List<ClassEntry> {
                val list = mutableListOf<ClassEntry>()
                for (child in snapshot.children) {
                    val day = child.child("day").getValue(String::class.java) ?: ""
                    val course = child.child("courseCode").getValue(String::class.java)
                        ?: child.child("course").getValue(String::class.java)
                        ?: child.child("subject").getValue(String::class.java) ?: ""
                    val faculty = child.child("faculty").getValue(String::class.java)
                        ?: child.child("teacher").getValue(String::class.java) ?: "TBA"
                    val room = child.child("room").getValue(String::class.java) ?: "TBA"
                    var startTime = child.child("startTime").getValue(String::class.java) ?: "08:15 AM"
                    var endTime = child.child("endTime").getValue(String::class.java) ?: "09:45 AM"
                    val timeSlot = child.child("timeSlot").getValue(String::class.java)
                    if (timeSlot != null && timeSlot.contains("-")) {
                        val parts = timeSlot.split("-")
                        if (parts.size == 2) {
                            startTime = parts[0].trim()
                            endTime = parts[1].trim()
                        }
                    }
                    val intake = child.child("intake").getValue(String::class.java) ?: rawIntake
                    val section = child.child("section").getValue(String::class.java) ?: rawSec
                    val needsReview = child.child("needsReview").getValue(Boolean::class.java) ?: false

                    if (day.isNotBlank() && course.isNotBlank()) {
                        list.add(
                            ClassEntry(
                                day = day,
                                course = course,
                                faculty = faculty,
                                room = room,
                                startTime = startTime,
                                endTime = endTime,
                                intake = intake,
                                section = section,
                                needsReview = needsReview
                            )
                        )
                    }
                }
                return list
            }

            fun parseDocsToEntries(docs: List<com.google.firebase.firestore.DocumentSnapshot>): List<ClassEntry> {
                val list = mutableListOf<ClassEntry>()
                for (doc in docs) {
                    val day = doc.getString("day") ?: ""
                    val course = doc.getString("courseCode") ?: doc.getString("subject") ?: ""
                    val faculty = doc.getString("faculty") ?: doc.getString("teacher") ?: "TBA"
                    val room = doc.getString("room") ?: "TBA"
                    var startTime = doc.getString("startTime") ?: "08:15 AM"
                    var endTime = doc.getString("endTime") ?: "09:45 AM"
                    val timeSlot = doc.getString("timeSlot")
                    if (timeSlot != null && timeSlot.contains("-")) {
                        val parts = timeSlot.split("-")
                        if (parts.size == 2) {
                            startTime = parts[0].trim()
                            endTime = parts[1].trim()
                        }
                    }
                    val intake = doc.getString("intake") ?: rawIntake
                    val section = doc.getString("section") ?: rawSec
                    val needsReview = doc.getBoolean("needsReview") ?: false

                    if (day.isNotBlank() && course.isNotBlank()) {
                        list.add(
                            ClassEntry(
                                day = day,
                                course = course,
                                faculty = faculty,
                                room = room,
                                startTime = startTime,
                                endTime = endTime,
                                intake = intake,
                                section = section,
                                needsReview = needsReview
                            )
                        )
                    }
                }
                return list
            }

            fun applyLoadedEntries(entries: List<ClassEntry>) {
                if (entries.isNotEmpty()) {
                    val repo = RoutineRepository(context)
                    val localEntries = repo.getClassEntries()
                    // Never overwrite if local routine has more classes (protecting against truncated or older cloud data)
                    if (localEntries.size > entries.size) {
                        return
                    }
                    repo.replaceClassEntries(entries)
                    loadRoutineEntriesIntoState(entries, context)
                }
            }

            // Fallback: load section's central schedule if personal routine is empty
            fun loadSectionScheduleFallback() {
                // First try RTDB section schedule
                rtdbRef.child("routines").child(deptKey).child(intakeKey).child(secKey).child("schedule")
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (snapshot.exists() && snapshot.childrenCount > 0) {
                                val entries = parseRtdbSnapshotToEntries(snapshot)
                                if (entries.isNotEmpty()) {
                                    applyLoadedEntries(entries)
                                    return
                                }
                            }
                            // Also try RTDB sections/{deptKey}/{secKey}/schedule
                            rtdbRef.child("sections").child(deptKey).child(secKey).child("schedule")
                                .addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(sSnapshot: DataSnapshot) {
                                        if (sSnapshot.exists() && sSnapshot.childrenCount > 0) {
                                            val entries = parseRtdbSnapshotToEntries(sSnapshot)
                                            if (entries.isNotEmpty()) {
                                                applyLoadedEntries(entries)
                                                return
                                            }
                                        }
                                        // Finally try Firestore if available
                                        try {
                                            db.collection("routines")
                                                .document(rawDeptKey)
                                                .collection(intakeKey)
                                                .document(rawSecKey)
                                                .collection("schedule")
                                                .get()
                                                .addOnSuccessListener { secSnapshot ->
                                                    if (secSnapshot != null && !secSnapshot.isEmpty) {
                                                        val entries = parseDocsToEntries(secSnapshot.documents)
                                                        applyLoadedEntries(entries)
                                                    }
                                                }
                                                .addOnFailureListener { e ->
                                                    Log.d("StudentPortalViewModel", "Firestore section schedule offline: ${e.message}")
                                                }
                                        } catch (e: Exception) {
                                            // Silently handle
                                        }
                                    }
                                    override fun onCancelled(error: DatabaseError) {}
                                })
                        }
                        override fun onCancelled(error: DatabaseError) {}
                    })
            }

            // 1. Primary & Candidate User Documents from RTDB (strictly sanitized for RTDB paths)
            val candidateUserIds = mutableListOf<String>()
            val idKey = session.userID.trim()
            if (idKey.isNotBlank() && idKey != "--") {
                val sanitizedId = idKey.replace(Regex("[^a-zA-Z0-9_]"), "_")
                if (sanitizedId.isNotBlank() && !candidateUserIds.contains(sanitizedId)) {
                    candidateUserIds.add(sanitizedId)
                }
            }
            if (!authUid.isNullOrBlank()) {
                val sanitizedAuthUid = authUid.replace(Regex("[^a-zA-Z0-9_]"), "_")
                if (sanitizedAuthUid.isNotBlank() && !candidateUserIds.contains(sanitizedAuthUid)) {
                    candidateUserIds.add(sanitizedAuthUid)
                }
            }
            val emailKey = session.userEmail.trim()
            if (emailKey.isNotBlank()) {
                val sanitizedEmail = emailKey.replace(Regex("[^a-zA-Z0-9_]"), "_")
                if (sanitizedEmail.isNotBlank() && !candidateUserIds.contains(sanitizedEmail)) {
                    candidateUserIds.add(sanitizedEmail)
                }
            }

            fun tryNextCandidateRtdb(index: Int) {
                if (index >= candidateUserIds.size) {
                    loadSectionScheduleFallback()
                    return
                }
                val currentTargetId = candidateUserIds[index]
                rtdbRef.child("users").child(currentTargetId).child("routine")
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (snapshot.exists() && snapshot.childrenCount > 0) {
                                val entries = parseRtdbSnapshotToEntries(snapshot)
                                if (entries.isNotEmpty()) {
                                    applyLoadedEntries(entries)
                                    return
                                }
                            }
                            tryNextCandidateRtdb(index + 1)
                        }

                        override fun onCancelled(error: DatabaseError) {
                            tryNextCandidateRtdb(index + 1)
                        }
                    })
            }

            if (candidateUserIds.isNotEmpty()) {
                tryNextCandidateRtdb(0)
            } else {
                loadSectionScheduleFallback()
            }
        } catch (e: Exception) {
            Log.e("StudentPortalViewModel", "Error in loadFirestoreRoutineToCache", e)
        }
    }

    private fun setupFirebaseRealtimeDatabase() {
        try {
            val rtdb = FirebaseDatabase.getInstance("https://crconnect-58521-default-rtdb.firebaseio.com")
            dbRef = rtdb.getReference("crconnect_db")

            // 1. Candidates Listener
            candidatesListener?.let { candidatesRef?.removeEventListener(it) }
            candidatesRef = dbRef?.child("voting_controls")?.child(sanitizedDept)?.child(sanitizedSec)?.child("candidates")
            candidatesListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    parseCandidatesSnapshot(snapshot)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("StudentPortalViewModel", "Candidates listener cancelled: ${error.message}")
                }
            }
            candidatesRef?.addValueEventListener(candidatesListener as ValueEventListener)

            // 2. Gate Control & Timer Listener
            gateControlListener?.let { gateControlRef?.removeEventListener(it) }
            gateControlRef = dbRef?.child("voting_controls")?.child(sanitizedDept)?.child(sanitizedSec)
            gateControlListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) return

                    val isOpen = snapshot.child("isGateOpen").getValue(Boolean::class.java) ?: false
                    val statusStr = snapshot.child("status").getValue(String::class.java) ?: if (isOpen) "Open" else "Closed"
                    val endMs = snapshot.child("endTimeMillis").getValue(Long::class.java) ?: 0L

                    endTimeMillis = endMs

                    _uiState.update { currentState ->
                        currentState.copy(
                            isGateOpen = isOpen,
                            timerStatus = statusStr
                        )
                    }

                    if (isOpen && endTimeMillis > System.currentTimeMillis()) {
                        startTimerLoop()
                    } else {
                        stopTimerLoop()
                        _uiState.update { it.copy(timerText = "00:00") }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("StudentPortalViewModel", "Gate listener cancelled: ${error.message}")
                }
            }
            gateControlRef?.addValueEventListener(gateControlListener as ValueEventListener)

            // 3. Voter Check Listener (Uses Auth UID or sanitized student ID for 100% collision-free voter tracking)
            voterListener?.let { voterRef?.removeEventListener(it) }
            val currentUid = FirebaseAuth.getInstance().currentUser?.uid
            val voterCheckKey = currentUid?.takeIf { it.isNotBlank() } ?: sanitizedStudentId
            voterRef = dbRef?.child("voting_controls")?.child(sanitizedDept)?.child(sanitizedSec)?.child("voters")?.child(voterCheckKey)
            voterListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val voted = snapshot.exists()
                    val candidateIdStr = snapshot.child("candidateId").getValue(String::class.java)
                    _uiState.update { currentState ->
                        currentState.copy(
                            hasVoted = voted,
                            votedCandidateId = candidateIdStr ?: currentState.votedCandidateId
                        )
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            }
            voterRef?.addValueEventListener(voterListener as ValueEventListener)

            // 4. Announcements / Notifications Listener
            announcementsListener?.let { announcementsRef?.removeEventListener(it) }
            announcementsRef = dbRef?.child("voting_controls")?.child(sanitizedDept)?.child(sanitizedSec)?.child("announcements")
            announcementsListener = object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    if (isFirstAnnouncementLoad) return

                    val title = snapshot.child("title").getValue(String::class.java) ?: "Update"
                    val body = snapshot.child("body").getValue(String::class.java) ?: ""
                    val postedBy = snapshot.child("postedBy").getValue(String::class.java) ?: ""

                    val appCtx = FirebaseDatabase.getInstance().app.applicationContext
                    RoutineNotificationHelper.showCrNoticeNotification(
                        context = appCtx,
                        title = title,
                        body = body,
                        postedBy = postedBy
                    )

                    _uiState.update { currentState ->
                        currentState.copy(snackbarMessage = "$title: $body")
                    }
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onChildRemoved(snapshot: DataSnapshot) {}
                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onCancelled(error: DatabaseError) {}
            }
            announcementsRef?.addChildEventListener(announcementsListener as ChildEventListener)

            // Allow child added events after 2 seconds to avoid popping toasts for historical announcements
            Handler(Looper.getMainLooper()).postDelayed({
                isFirstAnnouncementLoad = false
            }, 2000)

            // 5. Live User Role Listener & Profile Image Listener
            val authUser = FirebaseAuth.getInstance().currentUser
            if (authUser != null) {
                userRoleListener?.let { userRoleRef?.removeEventListener(it) }
                userRoleRef = dbRef?.child("users")?.child(authUser.uid)?.child("role")
                userRoleListener = object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val roleStr = snapshot.getValue(String::class.java) ?: ""
                        val isUserCr = roleStr.contains("CR", ignoreCase = true)
                        _uiState.update { currentState ->
                            currentState.copy(
                                isCr = isUserCr,
                                profile = currentState.profile.copy(isCr = isUserCr)
                            )
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {}
                }
                userRoleRef?.addValueEventListener(userRoleListener as ValueEventListener)

                // 6. Live Student Profile & ID Sync
                val userEmail = _uiState.value.profile.email
                val sanitizedEmailKey = userEmail.replace("@", "_").replace(".", "_")

                userProfileImgListener?.let { userProfileImgRef?.removeEventListener(it) }
                userProfileImgRef = dbRef?.child("users")?.child(authUser.uid)?.child("profileImageUrl")
                userProfileImgListener = object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val dbImg = snapshot.getValue(String::class.java) ?: ""
                        if (isValidProfileImageSource(dbImg) && dbImg != _uiState.value.profile.profileImageUrl) {
                            val context = FirebaseDatabase.getInstance().app.applicationContext
                            ProfileImageCacheHelper.saveAndCacheProfileImage(context, dbImg, authUser.uid, sanitizedStudentId, userEmail) { localPath ->
                                val finalPath = if (localPath.isNotBlank()) localPath else dbImg
                                SessionManager(context).setProfileImageUrl(finalPath)
                                _uiState.update { currentState ->
                                    currentState.copy(
                                        profile = currentState.profile.copy(profileImageUrl = finalPath)
                                    )
                                }
                            }
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {}
                }
                userProfileImgRef?.addValueEventListener(userProfileImgListener as ValueEventListener)

                userCgpaListener?.let { userCgpaRef?.removeEventListener(it) }
                userCgpaRef = dbRef?.child("users")?.child(authUser.uid)?.child("cgpa")
                userCgpaListener = object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val dbCgpa = snapshot.getValue(String::class.java) ?: ""
                        if (dbCgpa.isNotBlank() && dbCgpa != "--" && dbCgpa != _uiState.value.profile.cgpa) {
                            val context = FirebaseDatabase.getInstance().app.applicationContext
                            SessionManager(context).setStudentCgpa(dbCgpa)
                            _uiState.update { currentState ->
                                currentState.copy(
                                    profile = currentState.profile.copy(cgpa = dbCgpa)
                                )
                            }
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {}
                }
                userCgpaRef?.addValueEventListener(userCgpaListener as ValueEventListener)

                userLiveProfileListener?.let { userLiveProfileRef?.removeEventListener(it) }
                userLiveProfileListener = object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (!snapshot.exists()) return
                        val rawId = snapshot.child("id").getValue(String::class.java)
                            ?: snapshot.child("studentId").getValue(String::class.java)
                            ?: snapshot.child("student_id").getValue(String::class.java)

                        if (!rawId.isNullOrBlank() && !rawId.contains("@") && rawId != "--") {
                            val context = FirebaseDatabase.getInstance().app.applicationContext
                            SessionManager(context).setUserID(rawId)
                            sanitizedStudentId = rawId.replace(Regex("[^a-zA-Z0-9_]"), "_")
                            _uiState.update { currentState ->
                                currentState.copy(
                                    profile = currentState.profile.copy(studentId = rawId)
                                )
                            }
                        }

                        val liveImg = snapshot.child("profileImageUrl").getValue(String::class.java) ?: ""
                        if (isValidProfileImageSource(liveImg) && liveImg != _uiState.value.profile.profileImageUrl) {
                            val context = FirebaseDatabase.getInstance().app.applicationContext
                            ProfileImageCacheHelper.saveAndCacheProfileImage(context, liveImg, authUser.uid, sanitizedStudentId, userEmail) { localPath ->
                                val finalPath = if (localPath.isNotBlank()) localPath else liveImg
                                SessionManager(context).setProfileImageUrl(finalPath)
                                _uiState.update { currentState ->
                                    currentState.copy(
                                        profile = currentState.profile.copy(profileImageUrl = finalPath)
                                    )
                                }
                            }
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {}
                }

                userLiveProfileRef = dbRef?.child("users")?.child(authUser.uid)
                userLiveProfileRef?.addValueEventListener(userLiveProfileListener as ValueEventListener)
                if (sanitizedEmailKey.isNotBlank()) {
                    dbRef?.child("users")?.child(sanitizedEmailKey)?.addListenerForSingleValueEvent(userLiveProfileListener as ValueEventListener)
                }
                if (sanitizedStudentId.isNotBlank() && sanitizedStudentId != "student") {
                    dbRef?.child("users")?.child(sanitizedStudentId)?.addListenerForSingleValueEvent(userLiveProfileListener as ValueEventListener)
                }

                FirebaseFirestore.getInstance().collection("users").document(authUser.uid).get().addOnSuccessListener { doc ->
                    if (doc != null && doc.exists()) {
                        val rawId = doc.getString("id") ?: doc.getString("studentId") ?: doc.getString("student_id")
                        if (!rawId.isNullOrBlank() && !rawId.contains("@") && rawId != "--") {
                            val context = FirebaseDatabase.getInstance().app.applicationContext
                            SessionManager(context).setUserID(rawId)
                            sanitizedStudentId = rawId.replace(Regex("[^a-zA-Z0-9_]"), "_")
                            _uiState.update { currentState ->
                                currentState.copy(
                                    profile = currentState.profile.copy(studentId = rawId)
                                )
                            }
                        }

                        val cloudImg = doc.getString("profileImageUrl") ?: ""
                        if (isValidProfileImageSource(cloudImg) && cloudImg != _uiState.value.profile.profileImageUrl) {
                            val context = FirebaseDatabase.getInstance().app.applicationContext
                            ProfileImageCacheHelper.saveAndCacheProfileImage(context, cloudImg, authUser.uid, sanitizedStudentId, userEmail) { localPath ->
                                val finalPath = if (localPath.isNotBlank()) localPath else cloudImg
                                SessionManager(context).setProfileImageUrl(finalPath)
                                _uiState.update { currentState ->
                                    currentState.copy(
                                        profile = currentState.profile.copy(profileImageUrl = finalPath)
                                    )
                                }
                            }
                        }
                    } else if (userEmail.isNotBlank()) {
                        FirebaseFirestore.getInstance().collection("users").whereEqualTo("email", userEmail.trim()).get()
                            .addOnSuccessListener { querySnap ->
                                if (querySnap != null && !querySnap.isEmpty) {
                                    val d = querySnap.documents[0]
                                    val rawId = d.getString("id") ?: d.getString("studentId") ?: d.getString("student_id")
                                    if (!rawId.isNullOrBlank() && !rawId.contains("@") && rawId != "--") {
                                        val context = FirebaseDatabase.getInstance().app.applicationContext
                                        SessionManager(context).setUserID(rawId)
                                        sanitizedStudentId = rawId.replace(Regex("[^a-zA-Z0-9_]"), "_")
                                        _uiState.update { currentState ->
                                            currentState.copy(
                                                profile = currentState.profile.copy(studentId = rawId)
                                            )
                                        }
                                    }

                                    val cloudImg = d.getString("profileImageUrl") ?: ""
                                    if (isValidProfileImageSource(cloudImg) && cloudImg != _uiState.value.profile.profileImageUrl) {
                                        val context = FirebaseDatabase.getInstance().app.applicationContext
                                        ProfileImageCacheHelper.saveAndCacheProfileImage(context, cloudImg, authUser.uid, sanitizedStudentId, userEmail) { localPath ->
                                            val finalPath = if (localPath.isNotBlank()) localPath else cloudImg
                                            SessionManager(context).setProfileImageUrl(finalPath)
                                            _uiState.update { currentState ->
                                                currentState.copy(
                                                    profile = currentState.profile.copy(profileImageUrl = finalPath)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                    }
                }
            }

        } catch (e: Exception) {
            Log.e("StudentPortalViewModel", "Error setting up RTDB listeners", e)
        }
    }

    private fun isValidProfileImageSource(source: String?): Boolean {
        if (source.isNullOrBlank()) return false
        val trimmed = source.trim()
        if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true) ||
            trimmed.startsWith("data:image", ignoreCase = true) ||
            trimmed.contains("base64,", ignoreCase = true)
        ) {
            return true
        }
        val f = File(trimmed)
        return f.exists() && f.length() > 0
    }

    private fun sanitizeDepartment(dept: String): String {
        if (dept.isBlank() || dept == "--") return "CSE"
        val normalized = dept.trim().uppercase(Locale.getDefault())
        return when {
            normalized.contains("CSE") || normalized.contains("COMPUTER SCIENCE") -> "CSE"
            normalized.contains("EEE") || normalized.contains("ELECTRICAL") -> "EEE"
            normalized.contains("BBA") || normalized.contains("BUSINESS") -> "BBA"
            normalized.contains("ENG") || normalized.contains("ENGLISH") -> "ENG"
            normalized.contains("LAW") || normalized.contains("JUSTICE") -> "LAW"
            normalized.contains("TEXTILE") || normalized.contains("TE") -> "TE"
            normalized.contains("CIVIL") || normalized.contains("CE") -> "CE"
            normalized.contains("DATA SCIENCE") || normalized.contains("DSE") -> "DSE"
            normalized.contains("MATH") || normalized.contains("MNS") || normalized.contains("STAT") -> "MNS"
            normalized.contains("ECO") || normalized.contains("ECONOMICS") -> "ECO"
            normalized.contains("ACC") || normalized.contains("ACCOUNTING") -> "ACC"
            normalized.contains("FIN") || normalized.contains("FINANCE") -> "FIN"
            normalized.contains("MGT") || normalized.contains("MANAGEMENT") -> "MGT"
            normalized.contains("MKT") || normalized.contains("MARKETING") -> "MKT"
            else -> {
                val clean = dept.trim().replace(Regex("[^a-zA-Z0-9_]"), "_")
                if (clean.isBlank()) "CSE" else clean
            }
        }
    }

    private fun sanitizeSection(sec: String): String {
        if (sec.isBlank() || sec == "--" || sec.equals("5A", ignoreCase = true)) return "6"
        val digitsOnly = sec.replace(Regex("(?i)^section\\s*|^sec\\.?\\s*"), "").trim()
        val clean = digitsOnly.replace(Regex("[^a-zA-Z0-9_]"), "_")
        return if (clean.isBlank()) "6" else clean
    }

    private fun parseCandidatesSnapshot(snapshot: DataSnapshot) {
        val list = mutableListOf<CandidateModel>()
        for (ds in snapshot.children) {
            val key = ds.key ?: continue
            val name = ds.child("name").getValue(String::class.java)
                ?: ds.child("candidateName").getValue(String::class.java)
                ?: "Candidate"
            val dept = ds.child("department").getValue(String::class.java)
                ?: ds.child("dept").getValue(String::class.java)
                ?: rawDept
            val votesVal = ds.child("votes").getValue(Long::class.java)?.toInt()
                ?: ds.child("votes").getValue(Int::class.java)
                ?: 0
            val manifesto = ds.child("manifesto").getValue(String::class.java) ?: ""
            val photoUrl = ds.child("photoUrl").getValue(String::class.java) ?: ""
            val imageUrl = ds.child("imageUrl").getValue(String::class.java) ?: ""
            val finalPhotoUrl = if (imageUrl.isNotBlank()) imageUrl else photoUrl

            val candSec = ds.child("section").getValue(String::class.java)
                ?: ds.child("assignedSection").getValue(String::class.java)

            if (candSec != null && candSec.isNotBlank() && !candSec.equals("--", ignoreCase = true)) {
                val cleanCandSec = sanitizeSection(candSec)
                if (!candSec.equals(rawSec, ignoreCase = true) &&
                    !candSec.equals(sanitizedSec, ignoreCase = true) &&
                    !cleanCandSec.equals(sanitizedSec, ignoreCase = true)) {
                    continue
                }
            }

            list.add(
                CandidateModel(
                    id = key,
                    name = name,
                    department = dept,
                    votes = votesVal,
                    manifesto = manifesto,
                    photoUrl = finalPhotoUrl
                )
            )
        }

        _uiState.update { currentState ->
            currentState.copy(candidates = list)
        }
    }

    private fun startTimerLoop() {
        stopTimerLoop()
        timerRunnable = object : Runnable {
            override fun run() {
                val now = System.currentTimeMillis()
                val diff = endTimeMillis - now
                if (diff <= 0) {
                    _uiState.update { it.copy(timerText = "00:00", timerStatus = "Closed", isGateOpen = false) }
                    stopTimerLoop()
                    return
                }

                val totalSeconds = diff / 1000
                val minutes = totalSeconds / 60
                val seconds = totalSeconds % 60
                val formatted = String.format(Locale.US, "%02d:%02d", minutes, seconds)

                _uiState.update { it.copy(timerText = formatted) }
                timerHandler.postDelayed(this, 1000)
            }
        }
        timerHandler.post(timerRunnable!!)
    }

    private fun stopTimerLoop() {
        timerRunnable?.let { timerHandler.removeCallbacks(it) }
        timerRunnable = null
    }

    private fun setupFirestoreRoutines() {
        try {
            val rtdb = FirebaseDatabase.getInstance("https://crconnect-58521-default-rtdb.firebaseio.com")
            val rtdbRoot = rtdb.getReference("crconnect_db")
            val deptKey = sanitizeDepartment(rawDept)
            val intakeKey = getSanitizedKey(rawIntake)
            val secKey = sanitizeSection(rawSec)

            // 1. RTDB Realtime Routine Listener (Primary)
            rtdbRoutineListener?.let { rtdbRoutineRef?.removeEventListener(it) }
            rtdbRoutineRef = rtdbRoot.child("routines").child(deptKey).child(intakeKey).child(secKey).child("schedule")
            rtdbRoutineListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists() || snapshot.childrenCount == 0L) return
                    val daysList = listOf("SAT", "SUN", "MON", "TUE", "WED", "THR", "FRI")
                    val classMapMutable = mutableMapOf<Int, MutableList<ClassModel>>()

                    // Read local weekly attendance cache to preserve checked attendance state
                    val attendanceMap = appContext?.let { RoutineRepository(it).getWeeklyAttendanceMap() } ?: emptyMap()

                    for (child in snapshot.children) {
                        val dayStr = child.child("day").getValue(String::class.java) ?: continue
                        val courseCode = child.child("courseCode").getValue(String::class.java)
                            ?: child.child("course").getValue(String::class.java)
                            ?: child.child("subject").getValue(String::class.java) ?: continue
                        val faculty = child.child("faculty").getValue(String::class.java)
                            ?: child.child("teacher").getValue(String::class.java) ?: "TBA"
                        val roomStr = child.child("room").getValue(String::class.java) ?: "TBA"
                        val timeSlot = child.child("timeSlot").getValue(String::class.java) ?: "08:15 AM - 09:45 AM"

                        val dayIndex = daysList.indexOfFirst { it.equals(dayStr, ignoreCase = true) }
                        if (dayIndex == -1) continue

                        val courseDisplayName = if (faculty.isNotBlank() && faculty != "TBA" && !courseCode.contains("(")) {
                            "$courseCode ($faculty)"
                        } else {
                            courseCode
                        }

                        val roomDisplayName = if (roomStr.startsWith("Room") || roomStr.startsWith("TBA")) roomStr else "Room $roomStr"

                        val classId = child.key ?: "${dayStr}_${courseCode}"
                        val existingAttendance = _uiState.value.classMap[dayIndex]?.find {
                            it.id == classId || (it.courseName == courseDisplayName && it.timeSlot == timeSlot)
                        }?.attendanceState
                        val finalAttendance = attendanceMap[classId] ?: existingAttendance

                        val classItem = ClassModel(
                            id = classId,
                            timeSlot = timeSlot,
                            courseName = courseDisplayName,
                            room = roomDisplayName,
                            attendanceState = finalAttendance
                        )

                        classMapMutable.getOrPut(dayIndex) { mutableListOf() }.add(classItem)
                    }

                    if (classMapMutable.isNotEmpty()) {
                        classMapMutable.values.forEach { list ->
                            list.sortBy { startMinutes(it.timeSlot) }
                        }

                        val updatedDays = _uiState.value.scheduleDays.map { day ->
                            val hasClasses = (classMapMutable[day.dayIndex]?.isNotEmpty() == true)
                            day.copy(hasClasses = hasClasses)
                        }

                        _uiState.update { currentState ->
                            currentState.copy(
                                classMap = classMapMutable,
                                scheduleDays = updatedDays
                            )
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.d("StudentPortalViewModel", "RTDB routine listener cancelled: ${error.message}")
                }
            }
            rtdbRoutineRef?.addValueEventListener(rtdbRoutineListener as ValueEventListener)

            // 2. Firestore Listener as secondary fallback
            try {
                val db = FirebaseFirestore.getInstance()
                val rawDeptKey = getSanitizedKey(rawDept)
                val rawSecKey = getSanitizedKey(rawSec)

                firestoreRegistration?.remove()
                firestoreRegistration = db.collection("routines")
                    .document(rawDeptKey)
                    .collection(intakeKey)
                    .document(rawSecKey)
                    .collection("schedule")
                    .addSnapshotListener { querySnapshot, error ->
                        if (error != null || querySnapshot == null || querySnapshot.isEmpty) {
                            return@addSnapshotListener
                        }
                    }
            } catch (e: Exception) {
                // Silently handle Firestore permissions
            }
        } catch (e: Exception) {
            Log.e("StudentPortalViewModel", "Error setting up routines listener", e)
        }
    }

    fun selectTab(tabIndex: Int) {
        _uiState.update { it.copy(selectedTab = tabIndex) }
    }

    fun selectScheduleDay(dayIndex: Int) {
        _uiState.update { it.copy(selectedDayIndex = dayIndex) }
    }

    fun navigateWeek(delta: Int) {
        val newOffset = _uiState.value.currentWeekOffset + delta
        val weekData = calculateCurrentWeekDays(newOffset)
        _uiState.update { currentState ->
            currentState.copy(
                currentWeekOffset = newOffset,
                weekLabel = weekData.first,
                scheduleDays = weekData.second.map { day ->
                    day.copy(hasClasses = currentState.classMap[day.dayIndex]?.isNotEmpty() == true)
                }
            )
        }
    }

    fun toggleAttendance(classId: String, targetState: String, dayIndex: Int, context: Context? = null) {
        var updatedCourseName: String? = null
        var finalState: String? = null

        _uiState.update { state ->
            val updatedMap = state.classMap.mapValues { (dIdx, classes) ->
                if (dIdx == dayIndex) {
                    classes.map { item ->
                        if (item.id == classId) {
                            val newState = if (item.attendanceState == targetState) null else targetState
                            finalState = newState
                            updatedCourseName = item.courseName
                            item.copy(attendanceState = newState)
                        } else item
                    }
                } else classes
            }
            state.copy(classMap = updatedMap)
        }

        if (context != null) {
            val repo = RoutineRepository(context)
            repo.saveWeeklyAttendanceState(classId, finalState)

            // Calculate & persist current week's live summary
            val currentMap = _uiState.value.classMap
            val allClassesWithDay = currentMap.flatMap { (dIdx, classes) -> classes.map { dIdx to it } }
            val evaluated = allClassesWithDay.filter { (dIdx, item) ->
                val status = getClassTimingStatus(dIdx, item.timeSlot)
                status == ClassTimingStatus.PAST || item.attendanceState != null
            }
            val attended = evaluated.count { (dIdx, item) -> getEffectiveAttendanceState(dIdx, item) == "attended" }
            val pct = if (evaluated.isNotEmpty()) ((attended.toFloat() / evaluated.size) * 100).toInt() else 0
            repo.saveCurrentWeekSummary(attended, evaluated.size, pct)

            val courseDisplay = updatedCourseName?.takeIf { it.isNotBlank() } ?: "class"
            val cleanCourse = courseDisplay.split("(").firstOrNull()?.trim() ?: courseDisplay

            when (finalState) {
                "attended" -> {
                    ToastHelper.showSuccess(
                        context,
                        "Attendance Recorded",
                        "Class marked as attended for $cleanCourse."
                    )
                }
                "missed" -> {
                    ToastHelper.showWarning(
                        context,
                        "Class Missed",
                        "Class marked as missed for $cleanCourse."
                    )
                }
                else -> {
                    ToastHelper.showInfo(
                        context,
                        "Attendance Cleared",
                        "Attendance mark cleared for $cleanCourse."
                    )
                }
            }
        }
    }

    fun castVote(candidateId: String) {
        if (_uiState.value.hasVoted) return
        if (!_uiState.value.isGateOpen) {
            showToast("Voting gate is currently closed")
            return
        }

        val currentUid = FirebaseAuth.getInstance().currentUser?.uid
        val voterKey = currentUid?.takeIf { it.isNotBlank() }
            ?: sanitizedStudentId.takeIf { it.isNotBlank() && it != "voter" }
            ?: "voter_${System.currentTimeMillis()}"

        // Write vote to Firebase Realtime Database
        val secCandRef = dbRef?.child("voting_controls")?.child(sanitizedDept)?.child(sanitizedSec)?.child("candidates")?.child(candidateId)?.child("votes")
        val rootCandRef = dbRef?.child("candidates")?.child(candidateId)?.child("votes")
        val voterDbRef = dbRef?.child("voting_controls")?.child(sanitizedDept)?.child(sanitizedSec)?.child("voters")?.child(voterKey)

        secCandRef?.runTransaction(object : Transaction.Handler {
            override fun doTransaction(mutableData: MutableData): Transaction.Result {
                val currentVotes = mutableData.getValue(Int::class.java) ?: 0
                mutableData.value = currentVotes + 1
                return Transaction.success(mutableData)
            }

            override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {}
        })

        rootCandRef?.runTransaction(object : Transaction.Handler {
            override fun doTransaction(mutableData: MutableData): Transaction.Result {
                val currentVotes = mutableData.getValue(Int::class.java) ?: 0
                mutableData.value = currentVotes + 1
                return Transaction.success(mutableData)
            }

            override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {}
        })

        val voterData = mapOf(
            "timestamp" to System.currentTimeMillis(),
            "candidateId" to candidateId,
            "voted" to true,
            "studentId" to sanitizedStudentId,
            "uid" to (currentUid ?: "")
        )
        voterDbRef?.setValue(voterData)

        // Also record under sanitizedStudentId if valid and distinct for CR dashboard visibility
        if (sanitizedStudentId.isNotBlank() && sanitizedStudentId != "voter" && sanitizedStudentId != voterKey) {
            dbRef?.child("voting_controls")?.child(sanitizedDept)?.child(sanitizedSec)?.child("voters")?.child(sanitizedStudentId)?.setValue(voterData)
        }

        _uiState.update { currentState ->
            val updatedCandidates = currentState.candidates.map { candidate ->
                if (candidate.id == candidateId) candidate.copy(votes = candidate.votes + 1)
                else candidate
            }

            currentState.copy(
                hasVoted = true,
                votedCandidateId = candidateId,
                candidates = updatedCandidates,
                snackbarMessage = "Vote recorded successfully"
            )
        }
    }

    fun openAddClassSheet(manualMode: Boolean = true) {
        _uiState.update { it.copy(isAddClassSheetOpen = true, isAddClassManualMode = manualMode) }
    }

    fun closeAddClassSheet() {
        _uiState.update { it.copy(isAddClassSheetOpen = false) }
    }

    fun openNoticeSheet() {
        _uiState.update { it.copy(isNoticeSheetOpen = true) }
    }

    fun closeNoticeSheet() {
        _uiState.update { it.copy(isNoticeSheetOpen = false) }
    }

    fun publishNotice(title: String, body: String) {
        if (title.isBlank() || body.isBlank()) {
            _uiState.update { it.copy(snackbarMessage = "Please enter notice title and text") }
            return
        }
        val notifRef = dbRef?.child("voting_controls")?.child(sanitizedDept)?.child(sanitizedSec)?.child("announcements")
        val newNoticeRef = notifRef?.push()
        val noticeMap = mapOf(
            "title" to title.trim(),
            "body" to body.trim(),
            "timestamp" to System.currentTimeMillis(),
            "postedBy" to (_uiState.value.profile.name + " (CR)")
        )
        newNoticeRef?.setValue(noticeMap)?.addOnSuccessListener {
            _uiState.update { 
                it.copy(
                    isNoticeSheetOpen = false,
                    snackbarMessage = "Notice broadcasted to Section $sanitizedSec!"
                )
            }
        }?.addOnFailureListener { e ->
            _uiState.update { 
                it.copy(snackbarMessage = "Failed to post notice: ${e.message}")
            }
        }
    }

    fun addClass(courseName: String, timeSlot: String, room: String) {
        _uiState.update { currentState ->
            val currentDayIndex = currentState.selectedDayIndex
            val currentClasses = currentState.classMap[currentDayIndex] ?: emptyList()
            val newClass = ClassModel(
                id = System.currentTimeMillis().toString(),
                timeSlot = timeSlot.ifBlank { "09:00 - 10:30" },
                courseName = courseName,
                room = room.ifBlank { "Room TBA" }
            )
            val updatedClassList = (currentClasses + newClass).sortedBy { startMinutes(it.timeSlot) }
            val updatedClassMap = currentState.classMap.toMutableMap().apply {
                put(currentDayIndex, updatedClassList)
            }

            val updatedDays = currentState.scheduleDays.map { day ->
                if (day.dayIndex == currentDayIndex) day.copy(hasClasses = true)
                else day
            }

            currentState.copy(
                classMap = updatedClassMap,
                scheduleDays = updatedDays,
                isAddClassSheetOpen = false,
                snackbarMessage = "Class added"
            )
        }
    }

    fun saveScannedEntries(entries: List<ClassEntry>, context: Context? = null) {
        val ctx = context ?: FirebaseDatabase.getInstance().app.applicationContext
        val sortedEntries = entries.sortedBy { startMinutes(it.startTime) }
        val repo = RoutineRepository(ctx)
        // Overwrite saved routine entries permanently with newly scanned entries
        repo.replaceClassEntries(sortedEntries)

        val daysList = listOf("SAT", "SUN", "MON", "TUE", "WED", "THR", "FRI")
        val updatedMap = mutableMapOf<Int, MutableList<ClassModel>>()

        for (entry in sortedEntries) {
            val dayIdx = daysList.indexOfFirst { it.equals(entry.day, ignoreCase = true) }
            val targetIdx = if (dayIdx != -1) dayIdx else getTodayDayIndex()
            val timeSlot = "${entry.startTime} - ${entry.endTime}"
            val courseDisplay = if (entry.faculty.isNotBlank() && entry.faculty != "TBA" && !entry.course.contains("(")) {
                "${entry.course} (${entry.faculty})"
            } else {
                entry.course
            }
            val roomDisplay = if (entry.room.startsWith("Room") || entry.room.startsWith("TBA")) entry.room else "Room ${entry.room}"

            val classModel = ClassModel(
                id = "${entry.day}_${entry.course}_${entry.startTime}",
                timeSlot = timeSlot,
                courseName = courseDisplay,
                room = roomDisplay
            )
            val currentList = updatedMap.getOrPut(targetIdx) { mutableListOf() }
            if (!currentList.any { it.courseName == classModel.courseName && it.timeSlot == classModel.timeSlot }) {
                currentList.add(classModel)
            }
        }

        updatedMap.values.forEach { list ->
            list.sortBy { startMinutes(it.timeSlot) }
        }

        val todayIdx = getTodayDayIndex()

        val updatedDays = _uiState.value.scheduleDays.map { day ->
            val hasClasses = (updatedMap[day.dayIndex]?.isNotEmpty() == true)
            day.copy(hasClasses = hasClasses)
        }

        _uiState.update { currentState ->
            currentState.copy(
                classMap = updatedMap,
                scheduleDays = updatedDays,
                selectedDayIndex = todayIdx, // Automatically display today's routine after scanning
                isAddClassSheetOpen = false,
                snackbarMessage = "Scanned routine processed (${entries.size} classes saved)"
            )
        }

        // Sync to Firestore under routines/{dept}/{intake}/{section}/schedule
        saveScannedClassesToFirestore(entries, ctx)
    }

    fun loadSampleRoutine(context: Context? = null) {
        val ctx = context ?: FirebaseDatabase.getInstance().app.applicationContext
        val repo = RoutineRepository(ctx)
        val sampleEntries = repo.getDefaultSampleEntries()
        if (sampleEntries.isNotEmpty()) {
            saveScannedEntries(sampleEntries, ctx)
            _uiState.update { currentState ->
                currentState.copy(
                    snackbarMessage = "Loaded sample BUBT routine (${sampleEntries.size} classes)"
                )
            }
        }
    }

    fun deleteCurrentRoutine(context: Context? = null) {
        val ctx = context ?: FirebaseDatabase.getInstance().app.applicationContext

        // 1. Clear local SharedPreferences repositories
        val repo = RoutineRepository(ctx)
        repo.clearEntries()
        RoutineLocalCache.clearRoutineLocally(ctx)

        val session = SessionManager(ctx)
        val deptKey = sanitizeDepartment(session.userDept.ifBlank { rawDept })
        val rawDeptKey = getSanitizedKey(session.userDept.ifBlank { rawDept })
        val intakeKey = getSanitizedKey(session.userIntake.ifBlank { rawIntake })
        val secKey = sanitizeSection(session.userSection.ifBlank { rawSec })
        val rawSecKey = getSanitizedKey(session.userSection.ifBlank { rawSec })
        val authUid = FirebaseAuth.getInstance().currentUser?.uid

        // 2. Clear from Firebase Realtime Database (Primary)
        try {
            val rtdbRef = FirebaseDatabase.getInstance("https://crconnect-58521-default-rtdb.firebaseio.com").getReference("crconnect_db")
            if (session.userID.isNotBlank() && session.userID != "--") {
                val rawId = session.userID.replace(Regex("[^a-zA-Z0-9_]"), "_")
                rtdbRef.child("users").child(rawId).child("routine").removeValue()
            }
            if (!authUid.isNullOrBlank()) {
                val sanitizedAuthUid = authUid.replace(Regex("[^a-zA-Z0-9_]"), "_")
                rtdbRef.child("users").child(sanitizedAuthUid).child("routine").removeValue()
            }
            val emailKey = session.userEmail.trim()
            if (emailKey.isNotBlank()) {
                val sanitizedEmail = emailKey.replace(Regex("[^a-zA-Z0-9_]"), "_")
                rtdbRef.child("users").child(sanitizedEmail).child("routine").removeValue()
            }
            rtdbRef.child("routines").child(deptKey).child(intakeKey).child(secKey).child("schedule").removeValue()
        } catch (e: Exception) {
            Log.e("StudentPortalViewModel", "Error deleting routine from RTDB", e)
        }

        // 3. Clear Firestore schedule as secondary
        try {
            val db = FirebaseFirestore.getInstance()
            val scheduleRef = db.collection("routines")
                .document(rawDeptKey)
                .collection(intakeKey)
                .document(rawSecKey)
                .collection("schedule")

            scheduleRef.get().addOnSuccessListener { snapshot ->
                val batch = db.batch()
                for (doc in snapshot.documents) {
                    batch.delete(doc.reference)
                }
                batch.commit().addOnFailureListener { e ->
                    Log.e("StudentPortalViewModel", "Error deleting routine docs from Firestore", e)
                }
            }.addOnFailureListener { e ->
                Log.e("StudentPortalViewModel", "Error fetching routine docs for deletion", e)
            }
        } catch (e: Exception) {
            Log.e("StudentPortalViewModel", "Error in deleteCurrentRoutine", e)
        }

        // 4. Reset UI state schedule days & classMap immediately
        val updatedDays = _uiState.value.scheduleDays.map { day ->
            day.copy(hasClasses = false)
        }

        _uiState.update { currentState ->
            currentState.copy(
                classMap = emptyMap(),
                scheduleDays = updatedDays,
                isAddClassSheetOpen = false,
                snackbarMessage = "Current routine deleted"
            )
        }
    }

    private fun saveScannedClassesToFirestore(entries: List<ClassEntry>, context: Context) {
        try {
            val session = SessionManager(context)
            val deptKey = sanitizeDepartment(session.userDept.ifBlank { rawDept })
            val rawDeptKey = getSanitizedKey(session.userDept.ifBlank { rawDept })
            val intakeKey = getSanitizedKey(session.userIntake.ifBlank { rawIntake })
            val secKey = sanitizeSection(session.userSection.ifBlank { rawSec })
            val rawSecKey = getSanitizedKey(session.userSection.ifBlank { rawSec })
            val authUid = FirebaseAuth.getInstance().currentUser?.uid
            val uid = if (!authUid.isNullOrBlank()) authUid else session.userID.ifBlank { "" }

            val sortedEntries = entries.sortedBy { startMinutes(it.startTime) }

            // 1. Primary Sync: Firebase Realtime Database
            val rtdbRef = FirebaseDatabase.getInstance("https://crconnect-58521-default-rtdb.firebaseio.com").getReference("crconnect_db")
            val rtdbEntriesMap = mutableMapOf<String, Any>()
            for ((idx, entry) in sortedEntries.withIndex()) {
                val itemKey = "class_${idx}_${entry.day}_${entry.startTime.replace(" ", "").replace(":", "")}"
                val docData = hashMapOf(
                    "courseCode" to entry.course,
                    "subject" to entry.course,
                    "course" to entry.course,
                    "faculty" to entry.faculty,
                    "teacher" to entry.faculty,
                    "timeSlot" to "${entry.startTime} - ${entry.endTime}",
                    "room" to entry.room,
                    "day" to entry.day,
                    "startTime" to entry.startTime,
                    "endTime" to entry.endTime,
                    "intake" to (entry.intake.orEmpty().ifBlank { rawIntake }),
                    "section" to (entry.section.orEmpty().ifBlank { rawSec }),
                    "needsReview" to entry.needsReview
                )
                rtdbEntriesMap[itemKey] = docData
            }

            // 1. Primary Sync: Firebase Realtime Database (wipe clean previous routine before writing new data)
            if (session.userID.isNotBlank() && session.userID != "--") {
                val rawId = session.userID.replace(Regex("[^a-zA-Z0-9_]"), "_")
                val userRoutineRef = rtdbRef.child("users").child(rawId).child("routine")
                userRoutineRef.removeValue().addOnCompleteListener {
                    userRoutineRef.setValue(rtdbEntriesMap)
                }
            }
            if (!authUid.isNullOrBlank()) {
                val sanitizedAuthUid = authUid.replace(Regex("[^a-zA-Z0-9_]"), "_")
                val authRoutineRef = rtdbRef.child("users").child(sanitizedAuthUid).child("routine")
                authRoutineRef.removeValue().addOnCompleteListener {
                    authRoutineRef.setValue(rtdbEntriesMap)
                }
            }
            val emailKey = session.userEmail.trim()
            if (emailKey.isNotBlank()) {
                val sanitizedEmail = emailKey.replace(Regex("[^a-zA-Z0-9_]"), "_")
                val emailRoutineRef = rtdbRef.child("users").child(sanitizedEmail).child("routine")
                emailRoutineRef.removeValue().addOnCompleteListener {
                    emailRoutineRef.setValue(rtdbEntriesMap)
                }
            }

            // Also cleanly wipe and update section schedules so stale classes are wiped out
            val sectionRoutineRef = rtdbRef.child("routines").child(deptKey).child(intakeKey).child(secKey).child("schedule")
            sectionRoutineRef.removeValue().addOnCompleteListener {
                sectionRoutineRef.setValue(rtdbEntriesMap)
            }
            val legacySectionRef = rtdbRef.child("sections").child(deptKey).child(secKey).child("schedule")
            legacySectionRef.removeValue().addOnCompleteListener {
                legacySectionRef.setValue(rtdbEntriesMap)
            }

            // 2. Secondary Sync: Cloud Firestore (with graceful fallback)
            try {
                val db = FirebaseFirestore.getInstance()
                val scheduleRef = db.collection("routines")
                    .document(rawDeptKey)
                    .collection(intakeKey)
                    .document(rawSecKey)
                    .collection("schedule")

                scheduleRef.get().addOnSuccessListener { snapshot ->
                    val batch = db.batch()
                    for (doc in snapshot.documents) {
                        batch.delete(doc.reference)
                    }
                    for (entry in sortedEntries) {
                        val newDocRef = scheduleRef.document()
                        val docData = hashMapOf(
                            "courseCode" to entry.course,
                            "subject" to entry.course,
                            "faculty" to entry.faculty,
                            "teacher" to entry.faculty,
                            "timeSlot" to "${entry.startTime} - ${entry.endTime}",
                            "room" to entry.room,
                            "day" to entry.day,
                            "startTime" to entry.startTime,
                            "endTime" to entry.endTime,
                            "intake" to (entry.intake.orEmpty().ifBlank { rawIntake }),
                            "section" to (entry.section.orEmpty().ifBlank { rawSec }),
                            "needsReview" to entry.needsReview
                        )
                        batch.set(newDocRef, docData)
                    }
                    batch.commit().addOnFailureListener { e ->
                        Log.d("StudentPortalViewModel", "Firestore batch save offline: ${e.message}")
                    }
                }.addOnFailureListener { e ->
                    Log.d("StudentPortalViewModel", "Firestore schedule cleanup offline: ${e.message}")
                }

                if (uid.isNotBlank() && uid != "guest") {
                    val userRoutineRef = db.collection("users").document(uid).collection("routine")
                    userRoutineRef.get().addOnSuccessListener { userSnapshot ->
                        val userBatch = db.batch()
                        for (doc in userSnapshot.documents) {
                            userBatch.delete(doc.reference)
                        }
                        for (entry in sortedEntries) {
                            val newDocRef = userRoutineRef.document()
                            val docData = hashMapOf(
                                "courseCode" to entry.course,
                                "subject" to entry.course,
                                "faculty" to entry.faculty,
                                "teacher" to entry.faculty,
                                "timeSlot" to "${entry.startTime} - ${entry.endTime}",
                                "room" to entry.room,
                                "day" to entry.day,
                                "startTime" to entry.startTime,
                                "endTime" to entry.endTime,
                                "intake" to (entry.intake.orEmpty().ifBlank { rawIntake }),
                                "section" to (entry.section.orEmpty().ifBlank { rawSec }),
                                "needsReview" to entry.needsReview
                            )
                            userBatch.set(newDocRef, docData)
                        }
                        userBatch.commit().addOnFailureListener { e ->
                            Log.d("StudentPortalViewModel", "Firestore user routine offline: ${e.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.d("StudentPortalViewModel", "Firestore secondary sync skipped: ${e.message}")
            }
        } catch (e: Exception) {
            Log.e("StudentPortalViewModel", "Error in saveScannedClassesToFirestore", e)
        }
    }

    private fun getSanitizedKey(key: String): String {
        if (key.isBlank()) return "default"
        return key.trim().lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9_]"), "_")
            .replace(Regex("_+"), "_")
    }

    fun setThemeMode(context: Context, mode: String) {
        val session = SessionManager(context)
        session.setStudentThemeMode(mode)
        _uiState.update { it.copy(themeMode = mode) }
    }

    fun updateCgpa(newCgpa: String, context: Context) {
        val session = SessionManager(context)
        session.setStudentCgpa(newCgpa)

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        val dbRef = FirebaseDatabase.getInstance("https://crconnect-58521-default-rtdb.firebaseio.com").getReference("crconnect_db")
        if (uid != null) {
            dbRef.child("users").child(uid).child("cgpa").setValue(newCgpa)
        }
        if (sanitizedStudentId.isNotBlank()) {
            dbRef.child("users").child(sanitizedStudentId).child("cgpa").setValue(newCgpa)
        }

        val firestore = FirebaseFirestore.getInstance()
        if (uid != null) {
            firestore.collection("users").document(uid).update("cgpa", newCgpa)
        }
        if (sanitizedStudentId.isNotBlank()) {
            firestore.collection("users").document(sanitizedStudentId).update("cgpa", newCgpa)
        }

        _uiState.update { currentState ->
            currentState.copy(
                profile = currentState.profile.copy(cgpa = newCgpa)
            )
        }
        showToast("CGPA updated successfully")
    }

    fun updateAdvisorInfo(advisorName: String, advisorEmail: String, context: Context) {
        if (!_uiState.value.profile.isCr) {
            showToast("Only Class Representatives (CR) can update academic advisor")
            return
        }
        val session = SessionManager(context)
        session.setAdvisorName(advisorName)
        session.setAdvisorEmail(advisorEmail)

        _uiState.update { currentState ->
            currentState.copy(
                profile = currentState.profile.copy(
                    advisorName = advisorName,
                    advisorEmail = advisorEmail
                )
            )
        }

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        val dbRef = FirebaseDatabase.getInstance("https://crconnect-58521-default-rtdb.firebaseio.com").getReference("crconnect_db")
        if (uid != null) {
            dbRef.child("users").child(uid).child("advisorName").setValue(advisorName)
            dbRef.child("users").child(uid).child("advisorEmail").setValue(advisorEmail)
        }
        if (sanitizedStudentId.isNotBlank()) {
            dbRef.child("users").child(sanitizedStudentId).child("advisorName").setValue(advisorName)
            dbRef.child("users").child(sanitizedStudentId).child("advisorEmail").setValue(advisorEmail)
        }

        val firestore = FirebaseFirestore.getInstance()
        if (uid != null) {
            firestore.collection("users").document(uid).update(
                mapOf(
                    "advisorName" to advisorName,
                    "advisorEmail" to advisorEmail
                )
            )
        }
        if (sanitizedStudentId.isNotBlank()) {
            firestore.collection("users").document(sanitizedStudentId).update(
                mapOf(
                    "advisorName" to advisorName,
                    "advisorEmail" to advisorEmail
                )
            )
        }

        showToast("Academic advisor updated successfully")
    }

    fun updateCurrentTerm(newTerm: String, context: Context) {
        val session = SessionManager(context)
        session.setCurrentTerm(newTerm)

        _uiState.update { currentState ->
            currentState.copy(
                profile = currentState.profile.copy(
                    currentTerm = newTerm
                )
            )
        }

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        val dbRef = FirebaseDatabase.getInstance("https://crconnect-58521-default-rtdb.firebaseio.com").getReference("crconnect_db")
        if (uid != null) {
            dbRef.child("users").child(uid).child("currentTerm").setValue(newTerm)
        }
        if (sanitizedStudentId.isNotBlank()) {
            dbRef.child("users").child(sanitizedStudentId).child("currentTerm").setValue(newTerm)
        }

        val firestore = FirebaseFirestore.getInstance()
        if (uid != null) {
            firestore.collection("users").document(uid).update("currentTerm", newTerm)
        }
        if (sanitizedStudentId.isNotBlank()) {
            firestore.collection("users").document(sanitizedStudentId).update("currentTerm", newTerm)
        }

        showToast("Current term updated successfully")
    }

    fun updateProfileImage(uri: Uri, context: Context) {
        try {
            val file = if (uri.scheme == "file" && uri.path != null) {
                File(uri.path!!)
            } else {
                val inputStream = context.contentResolver.openInputStream(uri)
                val targetFile = File(context.filesDir, "profile_pic_${System.currentTimeMillis()}.jpg")
                val outputStream = FileOutputStream(targetFile)
                inputStream?.use { input ->
                    outputStream.use { output ->
                        input.copyTo(output)
                    }
                }
                targetFile
            }

            val absolutePath = file.absolutePath
            val session = SessionManager(context)
            session.setProfileImageUrl(absolutePath)

            val uid = FirebaseAuth.getInstance().currentUser?.uid
            val userEmail = session.userEmail
            val sanitizedEmailKey = userEmail.replace("@", "_").replace(".", "_")

            // Sync with local cache manager immediately for smooth zero-lag UI
            ProfileImageCacheHelper.saveAndCacheProfileImage(context, absolutePath, session.userID, userEmail, uid) {}

            _uiState.update { currentState ->
                currentState.copy(
                    profile = currentState.profile.copy(profileImageUrl = absolutePath)
                )
            }
            showToast("Profile picture updated successfully")

            // Encode compact Base64 JPEG to guarantee instant persistence across logout/login and all devices
            val base64Image = try {
                val bmp = BitmapFactory.decodeFile(absolutePath)
                if (bmp != null) {
                    val maxDim = 320
                    val scaled = if (bmp.width > maxDim || bmp.height > maxDim) {
                        val factor = maxDim.toFloat() / maxOf(bmp.width, bmp.height)
                        Bitmap.createScaledBitmap(bmp, (bmp.width * factor).toInt(), (bmp.height * factor).toInt(), true)
                    } else {
                        bmp
                    }
                    val baos = ByteArrayOutputStream()
                    scaled.compress(Bitmap.CompressFormat.JPEG, 70, baos)
                    val data = baos.toByteArray()
                    "data:image/jpeg;base64," + Base64.encodeToString(data, Base64.NO_WRAP)
                } else null
            } catch (e: Exception) {
                null
            }

            val dbRef = FirebaseDatabase.getInstance("https://crconnect-58521-default-rtdb.firebaseio.com").getReference("crconnect_db")
            val firestore = FirebaseFirestore.getInstance()

            if (!base64Image.isNullOrBlank()) {
                val imgUpdate = mapOf<String, Any>("profileImageUrl" to base64Image)
                if (uid != null) {
                    dbRef.child("users").child(uid).child("profileImageUrl").setValue(base64Image)
                    firestore.collection("users").document(uid).set(imgUpdate, SetOptions.merge())
                }
                if (sanitizedStudentId.isNotBlank() && sanitizedStudentId != "student") {
                    dbRef.child("users").child(sanitizedStudentId).child("profileImageUrl").setValue(base64Image)
                    firestore.collection("users").document(sanitizedStudentId).set(imgUpdate, SetOptions.merge())
                }
                if (sanitizedEmailKey.isNotBlank()) {
                    dbRef.child("users").child(sanitizedEmailKey).child("profileImageUrl").setValue(base64Image)
                    firestore.collection("users").document(sanitizedEmailKey).set(imgUpdate, SetOptions.merge())
                }
            }

            // Asynchronously upload to Firebase Storage to obtain cloud HTTPS URL for multi-device sync
            val storageRef = FirebaseStorage.getInstance().reference
            val storageKey = uid ?: sanitizedStudentId.ifBlank { "user_${System.currentTimeMillis()}" }
            val photoRef = storageRef.child("profile_images/${storageKey}.jpg")

            photoRef.putFile(Uri.fromFile(file))
                .addOnSuccessListener {
                    photoRef.downloadUrl.addOnSuccessListener { downloadUri ->
                        val remoteUrl = downloadUri.toString()
                        if (uid != null) {
                            dbRef.child("users").child(uid).child("profileImageUrl").setValue(remoteUrl)
                            firestore.collection("users").document(uid).update("profileImageUrl", remoteUrl)
                        }
                        if (sanitizedStudentId.isNotBlank()) {
                            dbRef.child("users").child(sanitizedStudentId).child("profileImageUrl").setValue(remoteUrl)
                            firestore.collection("users").document(sanitizedStudentId).update("profileImageUrl", remoteUrl)
                        }
                        if (sanitizedEmailKey.isNotBlank()) {
                            dbRef.child("users").child(sanitizedEmailKey).child("profileImageUrl").setValue(remoteUrl)
                            firestore.collection("users").document(sanitizedEmailKey).update("profileImageUrl", remoteUrl)
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.w("StudentPortalViewModel", "Firebase storage upload failed, keeping base64/local image: ${e.message}")
                }
        } catch (e: Exception) {
            showToast("Failed to update profile picture")
        }
    }

    fun updateSection(newSection: String, context: Context) {
        val session = SessionManager(context)
        session.setUserSection(newSection)

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        val dbRef = FirebaseDatabase.getInstance("https://crconnect-58521-default-rtdb.firebaseio.com").getReference("crconnect_db")
        if (uid != null) {
            dbRef.child("users").child(uid).child("section").setValue(newSection)
        }
        if (sanitizedStudentId.isNotBlank()) {
            dbRef.child("users").child(sanitizedStudentId).child("section").setValue(newSection)
        }

        val firestore = FirebaseFirestore.getInstance()
        if (uid != null) {
            firestore.collection("users").document(uid).update("section", newSection)
        }
        if (sanitizedStudentId.isNotBlank()) {
            firestore.collection("users").document(sanitizedStudentId).update("section", newSection)
        }

        _uiState.update { currentState ->
            currentState.copy(
                profile = currentState.profile.copy(section = newSection)
            )
        }
        showToast("Section updated successfully to $newSection")
    }

    fun updateStudentId(newId: String, context: Context) {
        val cleanId = newId.trim()
        if (cleanId.isBlank() || cleanId.contains("@")) {
            showToast("Invalid Student ID format")
            return
        }

        val session = SessionManager(context)
        session.setUserID(cleanId)
        sanitizedStudentId = cleanId.replace(Regex("[^a-zA-Z0-9_]"), "_")

        _uiState.update { currentState ->
            currentState.copy(
                profile = currentState.profile.copy(studentId = cleanId)
            )
        }

        val authUser = FirebaseAuth.getInstance().currentUser
        val uid = authUser?.uid
        val email = _uiState.value.profile.email
        val sanitizedEmail = email.replace("@", "_").replace(".", "_")
        val dbRef = FirebaseDatabase.getInstance("https://crconnect-58521-default-rtdb.firebaseio.com").getReference("crconnect_db")

        if (uid != null) {
            dbRef.child("users").child(uid).child("id").setValue(cleanId)
            dbRef.child("users").child(uid).child("studentId").setValue(cleanId)
        }
        if (sanitizedEmail.isNotBlank()) {
            dbRef.child("users").child(sanitizedEmail).child("id").setValue(cleanId)
            dbRef.child("users").child(sanitizedEmail).child("studentId").setValue(cleanId)
        }
        dbRef.child("users").child(sanitizedStudentId).child("id").setValue(cleanId)

        val firestore = FirebaseFirestore.getInstance()
        val updateMap = mapOf<String, Any>(
            "id" to cleanId,
            "studentId" to cleanId
        )
        val mergeOpt = com.google.firebase.firestore.SetOptions.merge()
        if (uid != null) {
            firestore.collection("users").document(uid).set(updateMap, mergeOpt)
        }
        if (sanitizedEmail.isNotBlank()) {
            firestore.collection("users").document(sanitizedEmail).set(updateMap, mergeOpt)
        }
        firestore.collection("users").document(sanitizedStudentId).set(updateMap, mergeOpt)

        showToast("Student ID updated successfully")
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun showToast(message: String) {
        _uiState.update { it.copy(snackbarMessage = message) }
    }

    private var lastUpdateCheckTime: Long = 0L

    fun checkForAppUpdates(context: Context, forceCheck: Boolean = false) {
        val currentTime = System.currentTimeMillis()
        if (!forceCheck && (currentTime - lastUpdateCheckTime) < 5 * 60 * 1000L) {
            return
        }

        if (forceCheck) {
            showToast("Checking for updates...")
        }

        lastUpdateCheckTime = currentTime

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val url = URL("https://api.github.com/repos/shaddhow/CRConnect-App/releases/latest")
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                conn.setRequestProperty("User-Agent", "CRConnect-AndroidApp")
                conn.setRequestProperty("Accept", "application/vnd.github.v3+json")

                if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                    val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(responseText)
                    val tagName = json.optString("tag_name", "").trim()
                    val htmlUrl = json.optString("html_url", "https://github.com/shaddhow/CRConnect-App/releases/latest")
                    val releaseBody = json.optString("body", "").trim()
                    val releaseName = json.optString("name", "").trim()

                    // Locate direct .apk download URL from release assets
                    var directApkUrl: String? = null
                    val assets = json.optJSONArray("assets")
                    if (assets != null) {
                        for (i in 0 until assets.length()) {
                            val asset = assets.optJSONObject(i) ?: continue
                            val assetName = asset.optString("name", "")
                            if (assetName.endsWith(".apk", ignoreCase = true)) {
                                directApkUrl = asset.optString("browser_download_url")
                                break
                            }
                        }
                    }

                    val cleanTag = tagName.removePrefix("v").removePrefix("V")
                    val currentVersion = try {
                        val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                        pInfo.versionName ?: "1.1.0"
                    } catch (e: Exception) {
                        "1.1.0"
                    }

                    val isNewer = isVersionNewer(cleanTag, currentVersion)
                    val isForce = releaseBody.contains("[force]", ignoreCase = true) ||
                            releaseName.contains("[force]", ignoreCase = true)

                    withContext(Dispatchers.Main) {
                        _uiState.update { currentState ->
                            currentState.copy(
                                isUpdateAvailable = isNewer,
                                isUpdateDialogVisible = isNewer,
                                isUpToDateDialogVisible = if (forceCheck && !isNewer) true else currentState.isUpToDateDialogVisible,
                                currentAppVersion = currentVersion,
                                latestVersionName = if (tagName.isNotBlank()) tagName else "v$cleanTag",
                                updateUrl = if (htmlUrl.isNotBlank()) htmlUrl else "https://github.com/shaddhow/CRConnect-App/releases/latest",
                                apkDownloadUrl = directApkUrl,
                                releaseNotes = releaseBody,
                                isForceUpdate = isForce,
                                isCheckingUpdate = false
                            )
                        }

                        if (forceCheck) {
                            if (isNewer) {
                                showToast("New update available: $tagName! Tap Update Now.")
                            }
                        }
                    }
                } else {
                    if (forceCheck) {
                        withContext(Dispatchers.Main) {
                            showToast("Could not check for updates. Try again later.")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("StudentPortalViewModel", "Update check failed", e)
                if (forceCheck) {
                    withContext(Dispatchers.Main) {
                        showToast("Unable to connect to GitHub releases.")
                    }
                }
            }
        }
    }

    fun showUpdateDialog() {
        _uiState.update { it.copy(isUpdateDialogVisible = true) }
    }

    fun dismissUpdateDialog() {
        if (!_uiState.value.isForceUpdate) {
            _uiState.update { it.copy(isUpdateDialogVisible = false) }
        }
    }

    fun dismissUpToDateDialog() {
        _uiState.update { it.copy(isUpToDateDialogVisible = false) }
    }

    fun startApkDownloadAndInstall(context: Context) {
        val apkUrl = _uiState.value.apkDownloadUrl
        if (apkUrl.isNullOrBlank()) {
            // Fallback to opening browser release page if no direct APK asset was uploaded to GitHub release
            try {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(_uiState.value.updateUrl)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(browserIntent)
            } catch (e: Exception) {
                showToast("Unable to open update link")
            }
            return
        }

        val cachedFile = _uiState.value.downloadedApkFile
        if (cachedFile != null && cachedFile.exists() && cachedFile.length() > 0) {
            installDownloadedApk(context, cachedFile)
            return
        }

        _uiState.update {
            it.copy(
                isDownloadingApk = true,
                downloadProgress = 0f,
                downloadedMb = 0f,
                totalMb = 0f,
                updateErrorMessage = null
            )
        }

        viewModelScope.launch {
            val result = AppUpdateHelper.downloadApk(context, apkUrl) { progress, downloaded, total ->
                _uiState.update {
                    it.copy(
                        downloadProgress = progress,
                        downloadedMb = downloaded,
                        totalMb = total
                    )
                }
            }

            result.fold(
                onSuccess = { file ->
                    _uiState.update {
                        it.copy(
                            isDownloadingApk = false,
                            downloadProgress = 1f,
                            downloadedApkFile = file
                        )
                    }
                    installDownloadedApk(context, file)
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isDownloadingApk = false,
                            updateErrorMessage = error.message ?: "Download failed. Tap to retry."
                        )
                    }
                }
            )
        }
    }

    fun installDownloadedApk(context: Context, file: File) {
        if (!AppUpdateHelper.canInstallPackages(context)) {
            AppUpdateHelper.openInstallPermissionSettings(context)
            showToast("Please allow CRConnect to install updates, then tap Install again.")
            return
        }
        val launched = AppUpdateHelper.launchPackageInstaller(context, file)
        if (!launched) {
            showToast("Failed to launch package installer")
        }
    }

    private fun isVersionNewer(latest: String, current: String): Boolean {
        if (latest.isBlank()) return false
        val latestParts = latest.split(".").mapNotNull { it.toIntOrNull() }
        val currentParts = current.split(".").mapNotNull { it.toIntOrNull() }

        val maxLength = Math.max(latestParts.size, currentParts.size)
        for (i in 0 until maxLength) {
            val l = latestParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }

    fun sendPasswordResetEmail(context: Context) {
        val email = _uiState.value.profile.email
        if (email.isBlank()) {
            showToast("No email associated with profile")
            return
        }
        FirebaseAuth.getInstance().sendPasswordResetEmail(email)
            .addOnSuccessListener {
                showToast("Password reset email sent to $email")
            }
            .addOnFailureListener { e ->
                showToast("Failed to send reset email: ${e.localizedMessage}")
            }
    }

    fun loadAttendanceHistory(context: Context) {
        val repo = RoutineRepository(context)
        val localArchives = repo.getAttendanceArchives()
        _uiState.update { it.copy(attendanceArchives = localArchives) }

        repo.syncAttendanceArchivesFromFirestore { updatedList ->
            _uiState.update { it.copy(attendanceArchives = updatedList) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopTimerLoop()
        candidatesListener?.let { candidatesRef?.removeEventListener(it) }
        gateControlListener?.let { gateControlRef?.removeEventListener(it) }
        voterListener?.let { voterRef?.removeEventListener(it) }
        announcementsListener?.let { announcementsRef?.removeEventListener(it) }
        userRoleListener?.let { userRoleRef?.removeEventListener(it) }
        userProfileImgListener?.let { userProfileImgRef?.removeEventListener(it) }
        userCgpaListener?.let { userCgpaRef?.removeEventListener(it) }
        userLiveProfileListener?.let { userLiveProfileRef?.removeEventListener(it) }
        rtdbRoutineListener?.let { rtdbRoutineRef?.removeEventListener(it) }
        firestoreRegistration?.remove()
    }
}
