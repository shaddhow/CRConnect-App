package com.example.crconnect.student

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class RoutineViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = RoutineRepository(application)

    private val _allEntries = MutableLiveData<List<ClassEntry>>()
    val allEntries: LiveData<List<ClassEntry>> = _allEntries

    private val _selectedDay = MutableLiveData<String>("THR")
    val selectedDay: LiveData<String> = _selectedDay

    private val _filteredClasses = MutableLiveData<List<ClassEntry>>()
    val filteredClasses: LiveData<List<ClassEntry>> = _filteredClasses

    private val _daysWithClasses = MutableLiveData<Set<String>>()
    val daysWithClasses: LiveData<Set<String>> = _daysWithClasses

    init {
        loadData()
    }

    fun loadData() {
        val entries = repository.getClassEntries()
        _allEntries.value = entries
        val activeDay = _selectedDay.value ?: "THR"
        _filteredClasses.value = repository.getEntriesForDay(activeDay)
        _daysWithClasses.value = repository.getDaysWithClasses()
    }

    fun selectDay(day: String) {
        val normDay = repository.normalizeDay(day)
        _selectedDay.value = normDay
        _filteredClasses.value = repository.getEntriesForDay(normDay)
    }

    fun saveScannedEntries(newEntries: List<ClassEntry>) {
        repository.saveClassEntries(newEntries)
        loadData()
    }

    fun clearRoutine() {
        repository.clearEntries()
        loadData()
    }
}
