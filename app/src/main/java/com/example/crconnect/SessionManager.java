package com.example.crconnect;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.appcompat.app.AppCompatDelegate;

import com.example.crconnect.student.RoutineRepository;

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
    private static final String KEY_CGPA = "student_cgpa";
    private static final String KEY_ADVISOR_NAME = "advisor_name";
    private static final String KEY_ADVISOR_EMAIL = "advisor_email";
    private static final String KEY_CURRENT_TERM = "student_current_term";
    private static final String KEY_BIOMETRIC_LOCK = "biometric_lock_enabled";

    private static final String KEY_REMEMBER_STUDENT_ENABLED = "remember_student_enabled";
    private static final String KEY_REMEMBER_STUDENT_ID = "remember_student_id";
    private static final String KEY_REMEMBER_STUDENT_PASS = "remember_student_pass";

    private static final String KEY_REMEMBER_TEACHER_ENABLED = "remember_teacher_enabled";
    private static final String KEY_REMEMBER_TEACHER_ID = "remember_teacher_id";
    private static final String KEY_REMEMBER_TEACHER_PASS = "remember_teacher_pass";

    private final SharedPreferences pref;
    private final SharedPreferences.Editor editor;
    private final Context context;

    public SessionManager(Context context) {
        this.context = context != null ? context.getApplicationContext() : null;
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    public void createLoginSession(String name, String email, String id, String role, String section, String assignedSection, String intake, String dept) {
        createLoginSession(name, email, id, role, section, assignedSection, null, intake, null, dept);
    }

    public void createLoginSession(String name, String email, String id, String role, String section, String assignedSection, String assignedSections, String intake, String assignedIntakes, String dept) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_NAME, name);
        editor.putString(KEY_EMAIL, email);
        editor.putString(KEY_ID, id);
        editor.putString(KEY_ROLE, role);
        editor.putString(KEY_SECTION, section);

        String currentAssignedSec = pref.getString(KEY_ASSIGNED_SECTION, "--");
        String currentAssignedSecs = pref.getString(KEY_ASSIGNED_SECTIONS, currentAssignedSec);
        String finalSec = (assignedSection != null && !assignedSection.equals("--")) ? assignedSection : currentAssignedSec;
        String finalSecs = (assignedSections != null && !assignedSections.isEmpty() && !assignedSections.equals("--")) ? assignedSections :
                ((assignedSection != null && !assignedSection.equals("--") && !assignedSection.isEmpty()) ? assignedSection : currentAssignedSecs);

        editor.putString(KEY_ASSIGNED_SECTION, finalSec);
        editor.putString(KEY_ASSIGNED_SECTIONS, finalSecs);

        String currentIntake = pref.getString(KEY_INTAKE, "--");
        String currentIntakes = pref.getString(KEY_ASSIGNED_INTAKES, currentIntake);
        String finalIntake = (intake != null && !intake.equals("--")) ? intake : currentIntake;
        String finalIntakes = (assignedIntakes != null && !assignedIntakes.isEmpty() && !assignedIntakes.equals("--")) ? assignedIntakes :
                ((intake != null && !intake.equals("--") && !intake.isEmpty()) ? intake : currentIntakes);

        editor.putString(KEY_INTAKE, finalIntake);
        editor.putString(KEY_ASSIGNED_INTAKES, finalIntakes);

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

    public void setUserDept(String dept) {
        editor.putString(KEY_DEPT, dept);
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
        editor.putString(KEY_ASSIGNED_SECTIONS, sections != null ? sections : "");
        if (sections != null && !sections.isEmpty() && !sections.equals("--")) {
            String currentSec = getAssignedSection();
            if (currentSec == null || currentSec.equals("--") || currentSec.isEmpty() || !sections.contains(currentSec)) {
                String firstSec = sections.split("[,;\\s]+")[0].replaceAll("(?i)^section\\s*|^sec\\.?\\s*", "").trim();
                editor.putString(KEY_ASSIGNED_SECTION, firstSec);
            }
        }
        editor.commit();
    }

    public void setAssignedIntakes(String intakes) {
        editor.putString(KEY_ASSIGNED_INTAKES, intakes != null ? intakes : "");
        if (intakes != null && !intakes.isEmpty() && !intakes.equals("--")) {
            String current = getUserIntake();
            if (current == null || current.equals("--") || current.isEmpty() || !intakes.contains(current)) {
                String firstIntake = intakes.split("[,;\\s]+")[0].trim();
                editor.putString(KEY_INTAKE, firstIntake);
            }
        }
        editor.commit();
    }

    public String getStudentCgpa() {
        return pref.getString(KEY_CGPA, "--");
    }

    public void setStudentCgpa(String cgpa) {
        editor.putString(KEY_CGPA, cgpa);
        editor.commit();
    }

    public String getAdvisorName() {
        String intake = getUserIntake();
        String section = getUserSection();
        if (intake != null && intake.contains("55") && section != null && (section.contains("6") || section.contains("5A"))) {
            return pref.getString(KEY_ADVISOR_NAME, "Nasrin Akter");
        }
        return pref.getString(KEY_ADVISOR_NAME, "--");
    }

    public void setAdvisorName(String name) {
        editor.putString(KEY_ADVISOR_NAME, name);
        editor.commit();
    }

    public String getAdvisorEmail() {
        String intake = getUserIntake();
        String section = getUserSection();
        if (intake != null && intake.contains("55") && section != null && (section.contains("6") || section.contains("5A"))) {
            return pref.getString(KEY_ADVISOR_EMAIL, "nasrinshipa@bubt.edu.bd");
        }
        return pref.getString(KEY_ADVISOR_EMAIL, "--");
    }

    public void setAdvisorEmail(String email) {
        editor.putString(KEY_ADVISOR_EMAIL, email);
        editor.commit();
    }

    public String getCurrentTerm() {
        return pref.getString(KEY_CURRENT_TERM, "--");
    }

    public void setCurrentTerm(String term) {
        editor.putString(KEY_CURRENT_TERM, term);
        editor.commit();
    }

    public boolean isBiometricLockEnabled() {
        return pref.getBoolean(KEY_BIOMETRIC_LOCK, false);
    }

    public void setBiometricLockEnabled(boolean enabled) {
        editor.putBoolean(KEY_BIOMETRIC_LOCK, enabled);
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

    public void saveStudentCredentials(String idOrEmail, String password) {
        editor.putBoolean(KEY_REMEMBER_STUDENT_ENABLED, true);
        editor.putString(KEY_REMEMBER_STUDENT_ID, idOrEmail);
        editor.putString(KEY_REMEMBER_STUDENT_PASS, password);
        editor.commit();
    }

    public void clearStudentCredentials() {
        editor.putBoolean(KEY_REMEMBER_STUDENT_ENABLED, false);
        editor.remove(KEY_REMEMBER_STUDENT_ID);
        editor.remove(KEY_REMEMBER_STUDENT_PASS);
        editor.commit();
    }

    public void saveTeacherCredentials(String idOrEmail, String password) {
        editor.putBoolean(KEY_REMEMBER_TEACHER_ENABLED, true);
        editor.putString(KEY_REMEMBER_TEACHER_ID, idOrEmail);
        editor.putString(KEY_REMEMBER_TEACHER_PASS, password);
        editor.commit();
    }

    public void clearTeacherCredentials() {
        editor.putBoolean(KEY_REMEMBER_TEACHER_ENABLED, false);
        editor.remove(KEY_REMEMBER_TEACHER_ID);
        editor.remove(KEY_REMEMBER_TEACHER_PASS);
        editor.commit();
    }

    public boolean isRememberMeEnabled(String role) {
        if ("Teacher".equalsIgnoreCase(role) || "Teacher/Admin".equalsIgnoreCase(role)) {
            return pref.getBoolean(KEY_REMEMBER_TEACHER_ENABLED, false);
        }
        return pref.getBoolean(KEY_REMEMBER_STUDENT_ENABLED, false);
    }

    public String getSavedStudentId() {
        return pref.getString(KEY_REMEMBER_STUDENT_ID, "");
    }

    public String getSavedStudentPassword() {
        return pref.getString(KEY_REMEMBER_STUDENT_PASS, "");
    }

    public String getSavedTeacherId() {
        return pref.getString(KEY_REMEMBER_TEACHER_ID, "");
    }

    public String getSavedTeacherPassword() {
        return pref.getString(KEY_REMEMBER_TEACHER_PASS, "");
    }

    public void logoutUser() {
        boolean rememberStudent = isRememberMeEnabled("Student");
        String savedStudentId = getSavedStudentId();
        String savedStudentPass = getSavedStudentPassword();

        boolean rememberTeacher = isRememberMeEnabled("Teacher");
        String savedTeacherId = getSavedTeacherId();
        String savedTeacherPass = getSavedTeacherPassword();

        editor.clear();
        editor.commit();

        if (rememberStudent) {
            saveStudentCredentials(savedStudentId, savedStudentPass);
        }
        if (rememberTeacher) {
            saveTeacherCredentials(savedTeacherId, savedTeacherPass);
        }

        if (context != null) {
            try {
                RoutineRepository repo = new RoutineRepository(context);
                repo.clearActiveSessionOnly();
                RoutineLocalCache.clearRoutineLocally(context);
            } catch (Exception e) {
                Log.e("SessionManager", "Error clearing routine cache on logout", e);
            }
        }
    }
}
