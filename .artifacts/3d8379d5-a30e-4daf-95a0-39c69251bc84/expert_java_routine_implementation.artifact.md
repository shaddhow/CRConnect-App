# Expert Java Developer Implementation Guide: BUBT Class Routine System

This document provides the complete, production-ready Java data models, local persistence helpers, dynamic room mapping logic, "UP NEXT" card (Design 3) controller with live countdown, and smooth animation transitions for the Android/Java application.

---

## 1. Data Model (`RoutineClassItem.java`)
```java
package com.example.crconnect;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

@Entity(tableName = "routine_classes")
public class RoutineClassItem {
    @PrimaryKey
    @NonNull
    public String id = "";
    public String day = "";
    public String subject = "";
    public String courseCode = "";
    public String timeSlot = "";
    public String startTime = "";
    public String endTime = "";
    public String room = "";
    public String teacher = "";
    public String faculty = "";
    public long timestamp = System.currentTimeMillis();

    public RoutineClassItem() {}

    public String getDisplayCourseCode() {
        if (courseCode != null && !courseCode.trim().isEmpty()) return courseCode;
        if (subject != null && !subject.trim().isEmpty()) return subject;
        return "Class";
    }

    public String getDisplayFaculty() {
        if (faculty != null && !faculty.trim().isEmpty()) return faculty;
        if (teacher != null && !teacher.trim().isEmpty()) return teacher;
        return "AAG";
    }

    // Dynamic Room Logic: 1st digit = Building, Last 3 digits = Room Number (e.g. "2517" -> "517")
    public String getRoomBadgeCode() {
        if (room == null || room.trim().isEmpty()) return "517";
        String r = room.replaceAll("[^0-9]", "").trim();
        if (r.matches("[0-9]{4}")) {
            return r.substring(1); // Last 3 digits
        }
        return r.isEmpty() ? "517" : r;
    }
}
```

---

## 2. Local Database Persistence (`RoutineLocalCache.java`)
```java
package com.example.crconnect;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class RoutineLocalCache {
    private static final String PREF_NAME = "bubt_routine_local_cache";
    private static final String KEY_CACHED_ROUTINE = "cached_routine_json";

    public static void saveRoutineLocally(Context context, List<RoutineClassItem> items) {
        if (context == null || items == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String json = gson.toJson(items);
        prefs.edit().putString(KEY_CACHED_ROUTINE, json).apply();
    }

    public static List<RoutineClassItem> loadRoutineLocally(Context context) {
        if (context == null) return new ArrayList<>();
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_CACHED_ROUTINE, null);
        if (json == null || json.isEmpty()) return new ArrayList<>();
        Gson gson = new Gson();
        Type type = new TypeToken<List<RoutineClassItem>>(){}.getType();
        List<RoutineClassItem> list = gson.fromJson(json, type);
        return list != null ? list : new ArrayList<>();
    }
}
```

---

## 3. Dynamic UI Controller & Smooth Animations (`RoutineActivity.java`)
```java
// Smooth ~300ms fade & slide transition when loading or updating cards
private void applySmoothTransition(View view) {
    if (view == null) return;
    view.setAlpha(0f);
    view.setTranslationY(20f);
    view.animate()
        .alpha(1f)
        .translationY(0f)
        .setDuration(300)
        .setInterpolator(new android.view.animation.DecelerateInterpolator())
        .start();
}

// Live Countdown Calculator: "Starts in Xh Ym"
private String calculateTimeUntilStart(String startTimeStr) {
    try {
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        Date classTime = sdf.parse(startTimeStr);
        if (classTime == null) return "Upcoming";

        Calendar nowCal = Calendar.getInstance();
        Calendar classCal = Calendar.getInstance();
        classCal.setTime(classTime);
        classCal.set(Calendar.YEAR, nowCal.get(Calendar.YEAR));
        classCal.set(Calendar.MONTH, nowCal.get(Calendar.MONTH));
        classCal.set(Calendar.DAY_OF_MONTH, nowCal.get(Calendar.DAY_OF_MONTH));

        long diffMs = classCal.getTimeInMillis() - nowCal.getTimeInMillis();
        if (diffMs < 0) return "Ongoing / Passed";

        long hours = diffMs / (1000 * 60 * 60);
        long minutes = (diffMs / (1000 * 60)) % 60;

        if (hours > 0) {
            return String.format(Locale.getDefault(), "Starts in %dh %dm", hours, minutes);
        } else {
            return String.format(Locale.getDefault(), "Starts in %dm", minutes);
        }
    } catch (Exception e) {
        return "Upcoming";
    }
}
```
