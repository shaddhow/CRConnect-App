package com.example.crconnect.student

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.crconnect.SessionManager
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
        val secStr = if (session.userRole.contains("Teacher", ignoreCase = true)) session.assignedSection else session.userSection
        rawSec = if (secStr.isNotBlank() && secStr != "--") secStr else "6"
        val rawId = session.userID.ifBlank { "student" }

        sanitizedDept = sanitizeDepartment(rawDept)
        sanitizedSec = sanitizeSection(rawSec)
        sanitizedStudentId = rawId.replace(Regex("[^a-zA-Z0-9_]"), "_")

        val initials = if (session.userName.isNotBlank()) {
            val parts = session.userName.trim().split(" ")
            if (parts.size >= 2) "${parts[0].first()}${parts[1].first()}".uppercase(Locale.getDefault())
            else session.userName.take(2).uppercase(Locale.getDefault())
        } else "ST"

        val profileModel = UserProfileModel(
            name = session.userName.ifBlank { "Student" },
            email = session.userEmail.ifBlank { "student@bubt.edu.bd" },
            studentId = session.userID.ifBlank { "--" },
            intake = session.userIntake.ifBlank { "--" },
            section = rawSec,
            department = rawDept,
            initials = initials
        )

        _uiState.update { it.copy(profile = profileModel) }

        setupFirebaseRealtimeDatabase()
        setupFirestoreRoutines()
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
            firestoreRegistration = db.collection("routines")
                .addSnapshotListener { querySnapshot, error ->
                    if (error != null || querySnapshot == null) {
                        Log.e("StudentPortalViewModel", "Error fetching routines", error)
                        return@addSnapshotListener
                    }

                    val classMapMutable = mutableMapOf<Int, MutableList<ClassModel>>()

                    for (doc in querySnapshot.documents) {
                        val dayStr = doc.getString("day") ?: doc.getString("validDay") ?: continue
                        val course = doc.getString("courseCode") ?: doc.getString("subject") ?: doc.getString("courseName") ?: "Course"
                        val timeSlot = doc.getString("timeSlot") ?: "09:00 - 10:30"
                        val room = doc.getString("room") ?: "Room TBA"
                        val docDept = doc.getString("dept") ?: doc.getString("department") ?: ""
                        val docSec = doc.getString("section") ?: doc.getString("sec") ?: ""

                        // Filter by department and section if provided in Firestore routine
                        if (docDept.isNotBlank() && !docDept.equals(rawDept, ignoreCase = true) && !docDept.equals(sanitizedDept, ignoreCase = true)) {
                            continue
                        }
                        if (docSec.isNotBlank() && docSec != rawSec && docSec != sanitizedSec) {
                            continue
                        }

                        val dayIndex = when (dayStr.uppercase(Locale.getDefault())) {
                            "SAT", "SATURDAY" -> 0
                            "SUN", "SUNDAY" -> 1
                            "MON", "MONDAY" -> 2
                            "TUE", "TUESDAY" -> 3
                            "WED", "WEDNESDAY" -> 4
                            "THU", "THURSDAY" -> 5
                            "FRI", "FRIDAY" -> 6
                            else -> continue
                        }

                        val classItem = ClassModel(
                            id = doc.id,
                            timeSlot = timeSlot,
                            courseName = course,
                            room = room
                        )

                        classMapMutable.getOrPut(dayIndex) { mutableListOf() }.add(classItem)
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
            val updatedClassList = currentClasses + newClass
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
        val repo = RoutineRepository(context ?: FirebaseDatabase.getInstance().app.applicationContext)
        repo.saveClassEntries(entries)

        val daysList = listOf("SAT", "SUN", "MON", "TUE", "WED", "THR", "FRI")
        val updatedMap = _uiState.value.classMap.toMutableMap()

        for (entry in entries) {
            val dayIdx = daysList.indexOfFirst { it.equals(entry.day, ignoreCase = true) }
            val targetIdx = if (dayIdx != -1) dayIdx else _uiState.value.selectedDayIndex
            val timeSlot = "${entry.startTime} - ${entry.endTime}"
            val classModel = ClassModel(
                id = "${entry.day}_${entry.course}_${entry.startTime}",
                timeSlot = timeSlot,
                courseName = "${entry.course} (${entry.faculty})",
                room = if (entry.room.startsWith("Room")) entry.room else "Room ${entry.room}"
            )
            val currentList = updatedMap[targetIdx] ?: emptyList()
            updatedMap[targetIdx] = (currentList + classModel).distinctBy { "${it.courseName}_${it.timeSlot}" }
        }

        val updatedDays = _uiState.value.scheduleDays.map { day ->
            val hasClasses = (updatedMap[day.dayIndex]?.isNotEmpty() == true)
            day.copy(hasClasses = hasClasses)
        }

        _uiState.update { currentState ->
            currentState.copy(
                classMap = updatedMap,
                scheduleDays = updatedDays,
                isAddClassSheetOpen = false,
                snackbarMessage = "Scanned routine processed (${entries.size} classes saved)"
            )
        }
    }

    private fun saveScannedClassesToFirestore(classList: List<ClassModel>) {
        try {
            val db = FirebaseFirestore.getInstance()
            val daysList = listOf("Saturday", "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday")

            for (item in classList) {
                val docData = hashMapOf(
                    "courseCode" to item.courseName,
                    "subject" to item.courseName,
                    "timeSlot" to item.timeSlot,
                    "room" to item.room,
                    "dept" to rawDept,
                    "department" to rawDept,
                    "section" to rawSec,
                    "sec" to rawSec,
                    "day" to daysList[_uiState.value.selectedDayIndex]
                )
                db.collection("routines").add(docData)
            }
        } catch (e: Exception) {
            Log.e("StudentPortalViewModel", "Error saving scanned classes to Firestore", e)
        }
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
