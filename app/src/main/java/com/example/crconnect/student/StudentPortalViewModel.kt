package com.example.crconnect.student

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.crconnect.RoutineLocalCache
import com.example.crconnect.SessionManager
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Calendar
import java.util.Locale

class StudentPortalViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(StudentPortalUiState())
    val uiState: StateFlow<StudentPortalUiState> = _uiState.asStateFlow()

    private var dbRef: DatabaseReference? = null
    private var firestoreRegistration: ListenerRegistration? = null

    private var sanitizedDept: String = "Dept_of_CSE"
    private var sanitizedSec: String = "6"
    private var sanitizedStudentId: String = "voter"
    private var rawDept: String = "Dept. of CSE"
    private var rawSec: String = "6"
    private var rawIntake: String = "55"

    private var isInitialized = false
    private var isFirstAnnouncementLoad = true

    private var candidatesListener: ValueEventListener? = null
    private var gateControlListener: ValueEventListener? = null
    private var voterListener: ValueEventListener? = null
    private var announcementsListener: ChildEventListener? = null

    private val timerHandler = Handler(Looper.getMainLooper())
    private var timerRunnable: Runnable? = null
    private var endTimeMillis: Long = 0L

    fun initSession(context: Context) {
        if (isInitialized) return
        isInitialized = true

        val session = SessionManager(context)
        rawDept = session.userDept.ifBlank { "Dept. of CSE" }
        rawIntake = session.userIntake.ifBlank { "55" }
        val secStr = if (session.userRole.contains("Teacher", ignoreCase = true)) session.assignedSection else session.userSection
        rawSec = if (secStr.isNotBlank() && secStr != "--") secStr else "6"
        val rawId = session.userID.ifBlank { "student" }

        sanitizedDept = sanitizeDepartment(rawDept)
        sanitizedSec = sanitizeSection(rawSec)
        sanitizedStudentId = rawId.replace(Regex("[^a-zA-Z0-9_]"), "_")

        val studentName = session.userName.ifBlank { "Student" }
        val initials = computeInitials(studentName)

        val profileModel = UserProfileModel(
            name = studentName,
            email = session.userEmail.ifBlank { "student@bubt.edu.bd" },
            studentId = session.userID.ifBlank { "--" },
            intake = session.userIntake.ifBlank { "--" },
            section = rawSec,
            department = rawDept,
            initials = initials
        )

        val todayIdx = getTodayDayIndex()
        _uiState.update { it.copy(profile = profileModel, selectedDayIndex = todayIdx) }

        val repo = RoutineRepository(context)
        val savedEntries = repo.getClassEntries()
        if (savedEntries.isNotEmpty()) {
            loadRoutineEntriesIntoState(savedEntries)
        } else {
            loadFirestoreRoutineToCache(context)
        }

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

    private fun loadRoutineEntriesIntoState(entries: List<ClassEntry>) {
        try {
            if (entries.isEmpty()) return
            val daysList = listOf("SAT", "SUN", "MON", "TUE", "WED", "THR", "FRI")
            val loadedMap = mutableMapOf<Int, MutableList<ClassModel>>()

            for (entry in entries) {
                val dayIdx = daysList.indexOfFirst { it.equals(entry.day, ignoreCase = true) }
                val targetIdx = if (dayIdx != -1) dayIdx else 0
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
            val uid = FirebaseAuth.getInstance().currentUser?.uid
                ?: session.userID.ifBlank { return }

            if (uid.isBlank()) return

            val db = FirebaseFirestore.getInstance()
            db.collection("users")
                .document(uid)
                .collection("routine")
                .get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot != null && !snapshot.isEmpty) {
                        val entries = mutableListOf<ClassEntry>()
                        for (doc in snapshot.documents) {
                            val day = doc.getString("day") ?: ""
                            val course = doc.getString("courseCode") ?: doc.getString("subject") ?: ""
                            val faculty = doc.getString("faculty") ?: doc.getString("teacher") ?: "TBA"
                            val room = doc.getString("room") ?: "TBA"
                            val startTime = doc.getString("startTime") ?: "08:15 AM"
                            val endTime = doc.getString("endTime") ?: "09:45 AM"
                            val intake = doc.getString("intake") ?: rawIntake
                            val section = doc.getString("section") ?: rawSec
                            val needsReview = doc.getBoolean("needsReview") ?: false

                            if (day.isNotBlank() && course.isNotBlank()) {
                                entries.add(
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

                        if (entries.isNotEmpty()) {
                            val repo = RoutineRepository(context)
                            repo.replaceClassEntries(entries)
                            loadRoutineEntriesIntoState(entries)
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("StudentPortalViewModel", "Error loading routine from Firestore backup", e)
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
            val candRef = dbRef?.child("voting_controls")?.child(sanitizedDept)?.child(sanitizedSec)?.child("candidates")
            candidatesListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists() && snapshot.hasChildren()) {
                        parseCandidatesSnapshot(snapshot)
                    } else {
                        // Fallback to root candidates node
                        dbRef?.child("candidates")?.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(rootSnap: DataSnapshot) {
                                parseCandidatesSnapshot(rootSnap)
                            }
                            override fun onCancelled(error: DatabaseError) {}
                        })
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("StudentPortalViewModel", "Candidates listener cancelled: ${error.message}")
                }
            }
            candRef?.addValueEventListener(candidatesListener as ValueEventListener)

            // 2. Gate Control & Timer Listener
            val gateRef = dbRef?.child("voting_controls")?.child(sanitizedDept)?.child(sanitizedSec)
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
            gateRef?.addValueEventListener(gateControlListener as ValueEventListener)

            // 3. Voter Check Listener
            val voterRef = dbRef?.child("voting_controls")?.child(sanitizedDept)?.child(sanitizedSec)?.child("voters")?.child(sanitizedStudentId)
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
            val notifRef = dbRef?.child("voting_controls")?.child(sanitizedDept)?.child(sanitizedSec)?.child("announcements")
            announcementsListener = object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    if (isFirstAnnouncementLoad) return

                    val title = snapshot.child("title").getValue(String::class.java) ?: "Update"
                    val body = snapshot.child("body").getValue(String::class.java) ?: ""

                    _uiState.update { currentState ->
                        currentState.copy(snackbarMessage = "$title: $body")
                    }
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onChildRemoved(snapshot: DataSnapshot) {}
                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onCancelled(error: DatabaseError) {}
            }
            notifRef?.addChildEventListener(announcementsListener as ChildEventListener)

            // Allow child added events after 2 seconds to avoid popping toasts for historical announcements
            Handler(Looper.getMainLooper()).postDelayed({
                isFirstAnnouncementLoad = false
            }, 2000)

        } catch (e: Exception) {
            Log.e("StudentPortalViewModel", "Error setting up RTDB listeners", e)
        }
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
        val clean = sec.trim().replace(Regex("[^a-zA-Z0-9_]"), "_")
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

            if (snapshot.key == "candidates" && candSec != null && candSec.isNotBlank() && !candSec.equals("--", ignoreCase = true)) {
                if (!candSec.equals(rawSec, ignoreCase = true) && !candSec.equals(sanitizedSec, ignoreCase = true)) {
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
            val db = FirebaseFirestore.getInstance()
            val deptKey = getSanitizedKey(rawDept)
            val intakeKey = getSanitizedKey(rawIntake)
            val secKey = getSanitizedKey(rawSec)

            firestoreRegistration?.remove()
            firestoreRegistration = db.collection("routines")
                .document(deptKey)
                .collection(intakeKey)
                .document(secKey)
                .collection("schedule")
                .addSnapshotListener { querySnapshot, error ->
                    if (error != null || querySnapshot == null || querySnapshot.isEmpty) {
                        Log.d("StudentPortalViewModel", "Firestore schedule empty/offline, keeping local routine")
                        return@addSnapshotListener
                    }

                    val daysList = listOf("SAT", "SUN", "MON", "TUE", "WED", "THR", "FRI")
                    val classMapMutable = mutableMapOf<Int, MutableList<ClassModel>>()

                    for (doc in querySnapshot.documents) {
                        val dayStr = doc.getString("day") ?: continue
                        val courseCode = doc.getString("courseCode") ?: doc.getString("subject") ?: continue
                        val faculty = doc.getString("faculty") ?: doc.getString("teacher") ?: "TBA"
                        val roomStr = doc.getString("room") ?: "TBA"
                        val timeSlot = doc.getString("timeSlot") ?: "08:15 AM - 09:45 AM"

                        val dayIndex = daysList.indexOfFirst { it.equals(dayStr, ignoreCase = true) }
                        if (dayIndex == -1) continue

                        val courseDisplayName = if (faculty.isNotBlank() && faculty != "TBA" && !courseCode.contains("(")) {
                            "$courseCode ($faculty)"
                        } else {
                            courseCode
                        }

                        val roomDisplayName = if (roomStr.startsWith("Room") || roomStr.startsWith("TBA")) roomStr else "Room $roomStr"

                        val classItem = ClassModel(
                            id = doc.id,
                            timeSlot = timeSlot,
                            courseName = courseDisplayName,
                            room = roomDisplayName
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
        } catch (e: Exception) {
            Log.e("StudentPortalViewModel", "Error setting up Firestore routines", e)
        }
    }

    fun selectTab(tabIndex: Int) {
        _uiState.update { it.copy(selectedTab = tabIndex) }
    }

    fun selectScheduleDay(dayIndex: Int) {
        _uiState.update { it.copy(selectedDayIndex = dayIndex) }
    }

    fun castVote(candidateId: String) {
        if (_uiState.value.hasVoted) return
        if (!_uiState.value.isGateOpen) {
            showToast("Voting gate is currently closed")
            return
        }

        // Write vote to Firebase Realtime Database
        val secCandRef = dbRef?.child("voting_controls")?.child(sanitizedDept)?.child(sanitizedSec)?.child("candidates")?.child(candidateId)?.child("votes")
        val rootCandRef = dbRef?.child("candidates")?.child(candidateId)?.child("votes")
        val voterRef = dbRef?.child("voting_controls")?.child(sanitizedDept)?.child(sanitizedSec)?.child("voters")?.child(sanitizedStudentId)

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
            "voted" to true
        )
        voterRef?.setValue(voterData)

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

    fun openAddClassSheet() {
        _uiState.update { it.copy(isAddClassSheetOpen = true) }
    }

    fun closeAddClassSheet() {
        _uiState.update { it.copy(isAddClassSheetOpen = false) }
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

    fun deleteCurrentRoutine(context: Context? = null) {
        val ctx = context ?: FirebaseDatabase.getInstance().app.applicationContext

        // 1. Clear local SharedPreferences repositories
        val repo = RoutineRepository(ctx)
        repo.clearEntries()
        RoutineLocalCache.clearRoutineLocally(ctx)

        // 2. Clear Firestore schedule for this dept/intake/section
        try {
            val db = FirebaseFirestore.getInstance()
            val session = SessionManager(ctx)
            val deptKey = getSanitizedKey(session.userDept.ifBlank { rawDept })
            val intakeKey = getSanitizedKey(session.userIntake.ifBlank { rawIntake })
            val secKey = getSanitizedKey(session.userSection.ifBlank { rawSec })

            val scheduleRef = db.collection("routines")
                .document(deptKey)
                .collection(intakeKey)
                .document(secKey)
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

        // 3. Reset UI state schedule days & classMap immediately
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
            val db = FirebaseFirestore.getInstance()
            val session = SessionManager(context)
            val deptKey = getSanitizedKey(session.userDept.ifBlank { rawDept })
            val intakeKey = getSanitizedKey(session.userIntake.ifBlank { rawIntake })
            val secKey = getSanitizedKey(session.userSection.ifBlank { rawSec })

            val sortedEntries = entries.sortedBy { startMinutes(it.startTime) }

            val scheduleRef = db.collection("routines")
                .document(deptKey)
                .collection(intakeKey)
                .document(secKey)
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
                        "endTime" to entry.endTime
                    )
                    batch.set(newDocRef, docData)
                }
                batch.commit().addOnFailureListener { e ->
                    Log.e("StudentPortalViewModel", "Error batch saving scanned classes to Firestore", e)
                }
            }.addOnFailureListener { e ->
                Log.e("StudentPortalViewModel", "Error getting schedule docs for cleanup", e)
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

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun showToast(message: String) {
        _uiState.update { it.copy(snackbarMessage = message) }
    }

    override fun onCleared() {
        super.onCleared()
        stopTimerLoop()
        candidatesListener?.let { dbRef?.removeEventListener(it) }
        gateControlListener?.let { dbRef?.removeEventListener(it) }
        voterListener?.let { dbRef?.removeEventListener(it) }
        announcementsListener?.let { dbRef?.removeEventListener(it) }
        firestoreRegistration?.remove()
    }
}
