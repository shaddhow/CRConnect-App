package com.example.crconnect;

public class RoutineClassItem {
    public String id;
    public String subject;
    public String courseCode;
    public String timeSlot;
    public String startTime;
    public String endTime;
    public String room;
    public String teacher;
    public String faculty;
    public String day;
    public long timestamp = System.currentTimeMillis();

    public RoutineClassItem() {
    }

    public String getCourseCode() {
        if (courseCode != null && !courseCode.trim().isEmpty()) return courseCode;
        if (subject != null && !subject.trim().isEmpty()) return subject;
        return "CSE 208";
    }

    public String getFaculty() {
        if (faculty != null && !faculty.trim().isEmpty()) return faculty;
        if (teacher != null && !teacher.trim().isEmpty()) return teacher;
        return "AAG";
    }

    public String getStartTime() {
        if (startTime != null && !startTime.trim().isEmpty()) return startTime;
        if (timeSlot != null && timeSlot.contains("-")) return timeSlot.split("-")[0].trim();
        return "11:15 AM";
    }

    public String getEndTime() {
        if (endTime != null && !endTime.trim().isEmpty()) return endTime;
        if (timeSlot != null && timeSlot.contains("-")) return timeSlot.split("-")[1].trim();
        return "12:45 PM";
    }

    public String getRoomNumber() {
        if (room == null || room.trim().isEmpty()) return "2517";
        String r = room.replace("Room", "").replace("ROOM", "").replace("R:", "").trim();
        try {
            if (r.matches("[0-9]{4}")) {
                return r; // e.g. "2517"
            }
        } catch (Exception ignored) {}
        return r.isEmpty() ? "2517" : r;
    }

    public String getDisplayCourseCode() {
        return getCourseCode();
    }

    public String getDisplayFaculty() {
        return getFaculty();
    }

    public String getDay() {
        return day;
    }
}
