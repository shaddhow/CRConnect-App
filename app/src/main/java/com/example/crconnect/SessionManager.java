package com.example.crconnect;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;

public class SessionManager {
    public static final String THEME_SYSTEM = "system";
    public static final String THEME_LIGHT = "light";
    public static final String THEME_DARK = "dark";

    private static final String PREF_NAME = "CRConnectSession";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_NAME = "userName";
    private static final String KEY_EMAIL = "userEmail";
    private static final String KEY_ID = "userID";
    private static final String KEY_ROLE = "userRole";
    private static final String KEY_SECTION = "userSection";
    private static final String KEY_ASSIGNED_SECTION = "assignedSection";
    private static final String KEY_INTAKE = "userIntake";
    private static final String KEY_DEPT = "userDept";
    private static final String KEY_PROFILE_IMAGE = "profileImageUrl";
    private static final String KEY_DARK_MODE = "dark_mode";
    private static final String KEY_THEME_MODE = "themeMode";
    private static final String KEY_STUDENT_THEME_MODE = "studentThemeMode";
    private static final String KEY_TEACHER_THEME_MODE = "teacherThemeMode";

    private final SharedPreferences pref;
    private final SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    public void createLoginSession(String name, String email, String id, String role, String section, String assignedSection, String intake, String dept) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_NAME, name);
        editor.putString(KEY_EMAIL, email);
        editor.putString(KEY_ID, id);
        editor.putString(KEY_ROLE, role);
        editor.putString(KEY_SECTION, section);
        editor.putString(KEY_ASSIGNED_SECTION, assignedSection);
        editor.putString(KEY_INTAKE, intake);
        editor.putString(KEY_DEPT, dept);
        editor.commit();
    }

    public void createLoginSession(String name, String email, String id, String role, String section, String intake, String dept) {
        createLoginSession(name, email, id, role, section, "--", intake, dept);
    }

    public boolean isLoggedIn() {
        return pref.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public String getUserName() { return pref.getString(KEY_NAME, "User"); }
    private static final String KEY_ASSIGNED_SECTIONS = "assignedSections";
    private static final String KEY_ASSIGNED_INTAKES = "assignedIntakes";

    public String getUserEmail() { return pref.getString(KEY_EMAIL, ""); }
    public String getUserID() { return pref.getString(KEY_ID, ""); }
    public String getUserRole() { return pref.getString(KEY_ROLE, "Student"); }
    public String getUserSection() { return pref.getString(KEY_SECTION, "--"); }
    public String getAssignedSection() { return pref.getString(KEY_ASSIGNED_SECTION, "--"); }
    public String getAssignedSections() { return pref.getString(KEY_ASSIGNED_SECTIONS, getAssignedSection()); }
    public String getAssignedIntakes() { return pref.getString(KEY_ASSIGNED_INTAKES, getUserIntake()); }
    public String getUserIntake() { return pref.getString(KEY_INTAKE, "--"); }
    public String getUserDept() { return pref.getString(KEY_DEPT, "--"); }
    public String getProfileImageUrl() { return pref.getString(KEY_PROFILE_IMAGE, ""); }

    public void setProfileImageUrl(String url) {
        editor.putString(KEY_PROFILE_IMAGE, url);
        editor.commit();
    }

    public void setUserIntake(String intake) {
        editor.putString(KEY_INTAKE, intake);
        editor.commit();
    }

    public void setUserSection(String section) {
        editor.putString(KEY_SECTION, section);
        editor.commit();
    }

    public void setAssignedSection(String assignedSection) {
        editor.putString(KEY_ASSIGNED_SECTION, assignedSection);
        editor.commit();
    }

    public void setAssignedSections(String sections) {
        editor.putString(KEY_ASSIGNED_SECTIONS, sections);
        editor.commit();
    }

    public void setAssignedIntakes(String intakes) {
        editor.putString(KEY_ASSIGNED_INTAKES, intakes);
        editor.commit();
    }

    public String getThemeMode() {
        return pref.getString(KEY_THEME_MODE, THEME_SYSTEM);
    }

    public void setThemeMode(String mode) {
        boolean isDark = THEME_DARK.equalsIgnoreCase(mode);
        editor.putString(KEY_THEME_MODE, mode);
        editor.putBoolean(KEY_DARK_MODE, isDark);
        editor.commit();
        applyThemeMode(mode);
    }

    public String getStudentThemeMode() {
        return pref.getString(KEY_STUDENT_THEME_MODE, getThemeMode());
    }

    public void setStudentThemeMode(String mode) {
        editor.putString(KEY_STUDENT_THEME_MODE, mode);
        editor.commit();
        setThemeMode(mode);
    }

    public String getTeacherThemeMode() {
        return pref.getString(KEY_TEACHER_THEME_MODE, getThemeMode());
    }

    public void setTeacherThemeMode(String mode) {
        editor.putString(KEY_TEACHER_THEME_MODE, mode);
        editor.commit();
        setThemeMode(mode);
    }

    public static void applyThemeMode(String mode) {
        if (THEME_LIGHT.equalsIgnoreCase(mode)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else if (THEME_DARK.equalsIgnoreCase(mode)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }
    }

    public void setDarkMode(boolean isDarkMode) {
        editor.putBoolean(KEY_DARK_MODE, isDarkMode);
        editor.putString(KEY_THEME_MODE, isDarkMode ? THEME_DARK : THEME_LIGHT);
        editor.commit();
        AppCompatDelegate.setDefaultNightMode(
            isDarkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );
    }

    public boolean isDarkMode() {
        return pref.getBoolean(KEY_DARK_MODE, false);
    }

    public void logoutUser() {
        editor.clear();
        editor.commit();
    }
}
