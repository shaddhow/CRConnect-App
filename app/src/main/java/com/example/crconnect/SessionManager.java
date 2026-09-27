package com.example.crconnect;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREF_NAME = "CRConnectSession";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_NAME = "userName";
    private static final String KEY_EMAIL = "userEmail";
    private static final String KEY_ID = "userID";
    private static final String KEY_ROLE = "userRole";
    private static final String KEY_SECTION = "userSection";
    private static final String KEY_INTAKE = "userIntake";
    private static final String KEY_DEPT = "userDept";

    private SharedPreferences pref;
    private SharedPreferences.Editor editor;
    private Context context;

    public SessionManager(Context context) {
        this.context = context;
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    public void createLoginSession(String name, String email, String id, String role, String section, String intake, String dept) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_NAME, name);
        editor.putString(KEY_EMAIL, email);
        editor.putString(KEY_ID, id);
        editor.putString(KEY_ROLE, role);
        editor.putString(KEY_SECTION, section);
        editor.putString(KEY_INTAKE, intake);
        editor.putString(KEY_DEPT, dept);
        editor.commit();
    }

    public boolean isLoggedIn() {
        return pref.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public String getUserName() { return pref.getString(KEY_NAME, "User"); }
    public String getUserEmail() { return pref.getString(KEY_EMAIL, ""); }
    public String getUserID() { return pref.getString(KEY_ID, ""); }
    public String getUserRole() { return pref.getString(KEY_ROLE, "Student"); }
    public String getUserSection() { return pref.getString(KEY_SECTION, "--"); }
    public String getUserIntake() { return pref.getString(KEY_INTAKE, "--"); }
    public String getUserDept() { return pref.getString(KEY_DEPT, "--"); }

    public void logoutUser() {
        editor.clear();
        editor.commit();
    }
}
