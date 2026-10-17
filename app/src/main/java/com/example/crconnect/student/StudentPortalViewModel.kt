package com.example.crconnect.student

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class StudentPortalViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(StudentPortalUiState())
    val uiState: StateFlow<StudentPortalUiState> = _uiState.asStateFlow()

    fun selectTab(tabIndex: Int) {
        _uiState.update { it.copy(selectedTab = tabIndex) }
    }

    fun selectScheduleDay(dayIndex: Int) {
        _uiState.update { it.copy(selectedDayIndex = dayIndex) }
    }

    fun castVote(candidateId: Int) {
        _uiState.update { currentState ->
            if (currentState.hasVoted) return@update currentState

            val updatedCandidates = currentState.candidates.map { candidate ->
                if (candidate.id == candidateId) {
                    candidate.copy(votes = candidate.votes + 1)
                } else {
                    candidate
                }
            }

            currentState.copy(
                hasVoted = true,
                votedCandidateId = candidateId,
                candidates = updatedCandidates,
                snackbarMessage = "Vote secured"
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
                if (day.dayIndex == currentDayIndex) {
                    day.copy(hasClasses = true)
                } else {
                    day
                }
            }

            currentState.copy(
                classMap = updatedClassMap,
                scheduleDays = updatedDays,
                isAddClassSheetOpen = false,
                snackbarMessage = "Class added"
            )
        }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun showToast(message: String) {
        // No-op for demo toasts to ensure only allowed user toasts are shown
    }
}
