package com.example.crconnect.student

data class ClassEntry(
    val day: String,        // e.g. "THR", "SAT", "SUN", "MON", "TUE", "WED", "FRI"
    val course: String,     // e.g. "CSE 210"
    val faculty: String,    // e.g. "MDSS"
    val room: String,       // e.g. "2517"
    val startTime: String,  // e.g. "08:15 AM"
    val endTime: String     // e.g. "09:45 AM"
)
