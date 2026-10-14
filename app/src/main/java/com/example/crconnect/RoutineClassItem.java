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
        if (courseCode != null && !courseCode.trim().isEmpty()) return courseCode.trim();
        if (subject != null && !subject.trim().isEmpty()) return subject.trim();
        return "Class";
    }

    public String getFaculty() {
        if (faculty != null && !faculty.trim().isEmpty()) return faculty.trim();
        if (teacher != null && !teacher.trim().isEmpty()) return teacher.trim();
        return "Faculty";
    }

    public String getStartTime() {
        if (startTime != null && !startTime.trim().isEmpty()) return startTime.trim();
        if (timeSlot != null && timeSlot.contains("-")) return timeSlot.split("-")[0].trim();
        return "";
    }

    public String getEndTime() {
        if (endTime != null && !endTime.trim().isEmpty()) return endTime.trim();
        if (timeSlot != null && timeSlot.contains("-")) return timeSlot.split("-")[1].trim();
        return "";
    }

    public String getRoomNumber() {
        if (room != null && !room.trim().isEmpty()) {
            String r = room.replace("Room", "").replace("ROOM", "").replace("R:", "").trim();
            if (!r.isEmpty()) return r;
        }
        return "TBA";
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

