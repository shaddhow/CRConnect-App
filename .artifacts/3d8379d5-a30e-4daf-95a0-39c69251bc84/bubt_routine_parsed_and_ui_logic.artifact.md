# BUBT Official Class Schedule - Parsed JSON & UI Implementation Guide

This document contains the structured JSON representation of the parsed BUBT class schedule along with the dynamic UI rendering logic for the **"UP NEXT" Card (Design 3)**, chronological daily class list, duplicate removal, and dynamic room parsing logic.

---

## 1. Structured JSON Data Model

```json
{
  "department": "CSE",
  "intake": "55",
  "section": "6",
  "schedule": [
    {
      "day": "Saturday",
      "courseCode": "CSE 207",
      "subject": "Data Structures",
      "timeSlot": "09:45 AM - 11:15 AM",
      "startTime": "09:45 AM",
      "endTime": "11:15 AM",
      "roomCode": "2517",
      "building": "2",
      "roomNumber": "517",
      "roomDisplay": "Room 517 (Bldg 2)",
      "faculty": "AAG"
    },
    {
      "day": "Saturday",
      "courseCode": "CSE 208",
      "subject": "Data Structures Lab",
      "timeSlot": "11:15 AM - 01:00 PM",
      "startTime": "11:15 AM",
      "endTime": "01:00 PM",
      "roomCode": "2408",
      "building": "2",
      "roomNumber": "408",
      "roomDisplay": "Room 408 (Bldg 2)",
      "faculty": "MRH"
    },
    {
      "day": "Sunday",
      "courseCode": "MAT 201",
      "subject": "Complex Variable & Statistics",
      "timeSlot": "08:15 AM - 09:45 AM",
      "startTime": "08:15 AM",
      "endTime": "09:45 AM",
      "roomCode": "1302",
      "building": "1",
      "roomNumber": "302",
      "roomDisplay": "Room 302 (Bldg 1)",
      "faculty": "SAB"
    }
  ]
}
```

---

## 2. Core Implementation Logic

### A. Dynamic Room Parsing (Building & Last 3 Digits Room Code)
```java
private String formatRoomBadge(String room) {
    if (room == null || room.trim().isEmpty()) return "517";
    String r = room.replaceAll("[^0-9]", "").trim();
    if (r.matches("[0-9]{4}")) {
        // 1st digit = Building, last 3 digits = Room Number
        return r.substring(1); // e.g. "2517" -> "517"
    }
    return r.isEmpty() ? "808" : r;
}
```

### B. Duplicate Removal & Chronological Sorting
```java
// Deduplicate by day + timeSlot + courseCode
List<RoutineClassItem> uniqueClasses = new ArrayList<>();
Set<String> seenKeys = new HashSet<>();
for (RoutineClassItem item : dayClasses) {
    String key = (item.day + "_" + item.timeSlot + "_" + item.getDisplayCourseCode()).toLowerCase();
    if (!seenKeys.contains(key)) {
        seenKeys.add(key);
        uniqueClasses.add(item);
    }
}

// Sort chronologically by start time
Collections.sort(uniqueClasses, (a, b) -> {
    String timeA = a.startTime != null ? a.startTime : "";
    String timeB = b.startTime != null ? b.startTime : "";
    return timeA.compareTo(timeB);
});
```

### C. "UP NEXT" Card Countdown & Rendering (Design 3)
```java
private void updateUpNextBanner(RoutineClassItem nextClass) {
    if (nextClass == null) {
        cardUpNext.setVisibility(View.GONE);
        return;
    }

    tvUpNextSubject.setText(nextClass.getDisplayCourseCode());
    tvUpNextTime.setText(String.format("%s – %s", nextClass.startTime, nextClass.endTime));

    // Faculty Initial display
    String faculty = nextClass.getDisplayFaculty();
    tvUpNextFaculty.setText("FC: " + faculty);

    // Room Badge (Last 3 digits)
    String roomBadge = formatRoomBadge(nextClass.room);
    tvUpNextRoom.setText("ROOM " + roomBadge);

    cardUpNext.setVisibility(View.VISIBLE);
}
```
