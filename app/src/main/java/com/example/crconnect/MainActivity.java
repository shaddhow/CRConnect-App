package com.example.crconnect;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.core.widget.NestedScrollView;

import com.example.crconnect.core.utils.HapticUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etPassword;
    private TextInputLayout tilEmail, tilPassword;
    private RadioGroup radioGroupRole;
    private RadioButton rbStudent, rbTeacher;
    private Button btnLogin;
    private TextView tvRegisterLink, tvForgotPassword;
    private SessionManager sessionManager;
    private NestedScrollView scrollViewMain;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // --- STRICT EDGE-TO-EDGE FULL SCREEN WITH LIGHT BACKGROUND STATUS BARS ---
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getWindow().setNavigationBarContrastEnforced(false);
        }
        WindowInsetsControllerCompat insetsController = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (insetsController != null) {
            insetsController.setAppearanceLightStatusBars(true);
            insetsController.setAppearanceLightNavigationBars(true);
        }
        getWindow().setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.bg_gradient));

        setContentView(R.layout.activity_main);
        
        sessionManager = new SessionManager(this);
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null && !currentUser.isAnonymous()) {
            currentUser.reload().addOnCompleteListener(task -> {
                if (isFinishing() || isDestroyed()) return;
                FirebaseUser refreshedUser = FirebaseAuth.getInstance().getCurrentUser();
                if (task.isSuccessful() && refreshedUser != null && refreshedUser.isEmailVerified()) {
                    if (sessionManager.isLoggedIn()) {
                        navigateBasedOnRole(sessionManager.getUserRole(), sessionManager.getUserEmail());
                    } else {
                        FirebaseAuth.getInstance().signOut();
                        sessionManager.logoutUser();
                    }
                } else {
                    FirebaseAuth.getInstance().signOut();
                    sessionManager.logoutUser();
                }
            });
        } else {
            FirebaseAuth.getInstance().signOut();
            sessionManager.logoutUser();
        }

        // Safe Area Padding
        scrollViewMain = findViewById(R.id.scrollViewMain);
        if (scrollViewMain != null) {
            ViewCompat.setOnApplyWindowInsetsListener(scrollViewMain, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, Math.max(systemBars.bottom, ime.bottom));
                return WindowInsetsCompat.CONSUMED;
            });
        }

        // Double press back to exit from login page
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            private long backPressedTime;
            private Toast backToast;

            @Override
            public void handleOnBackPressed() {
                if (backPressedTime + 2000 > System.currentTimeMillis()) {
                    if (backToast != null) backToast.cancel();
                    finishAffinity();
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                } else {
                    backToast = Toast.makeText(MainActivity.this, "Press back again to exit", Toast.LENGTH_SHORT);
                    backToast.show();
                }
                backPressedTime = System.currentTimeMillis();
            }
        });

        // Binding UI elements
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        radioGroupRole = findViewById(R.id.radioGroupRole);
        rbStudent = findViewById(R.id.rbStudent);
        rbTeacher = findViewById(R.id.rbTeacher);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegisterLink = findViewById(R.id.tvRegisterLink);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        TextView tvDontHaveAccount = findViewById(R.id.tvDontHaveAccount);

        if (tvRegisterLink != null) tvRegisterLink.getPaint().setAntiAlias(true);
        if (tvForgotPassword != null) tvForgotPassword.getPaint().setAntiAlias(true);
        if (tvDontHaveAccount != null) tvDontHaveAccount.getPaint().setAntiAlias(true);

        View layoutBranding = findViewById(R.id.layoutBranding);
        View rootView = findViewById(android.R.id.content);

        if (layoutBranding != null) {
            layoutBranding.post(() -> {
                layoutBranding.setPivotX(layoutBranding.getWidth() / 2f);
                layoutBranding.setPivotY(0f);
            });
        }

        if (rootView != null && layoutBranding != null) {
            rootView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
                Rect r = new Rect();
                rootView.getWindowVisibleDisplayFrame(r);
                int screenHeight = rootView.getRootView().getHeight();
                int keypadHeight = screenHeight - r.bottom;

                boolean isKeyboardOpen = keypadHeight > screenHeight * 0.15;
                float durationScale = Settings.Global.getFloat(getContentResolver(), Settings.Global.ANIMATOR_DURATION_SCALE, 1.0f);

                if (durationScale > 0f) {
                    if (isKeyboardOpen) {
                        layoutBranding.animate().scaleX(0.85f).scaleY(0.85f).setDuration(200).start();
                    } else {
                        layoutBranding.animate().scaleX(1f).scaleY(1f).setDuration(200).start();
                    }
                } else {
                    float target = isKeyboardOpen ? 0.85f : 1f;
                    layoutBranding.setScaleX(target);
                    layoutBranding.setScaleY(target);
                }
            });
        }

        // Auto-scroll up smoothly when password or email field gets focus
        if (etPassword != null) {
            etPassword.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus && scrollViewMain != null) {
                    scrollViewMain.postDelayed(() -> scrollViewMain.smoothScrollTo(0, btnLogin.getBottom() + 100), 150);
                }
            });
        }

        // Role change dynamic hint logic
        if (radioGroupRole != null) {
            radioGroupRole.setOnCheckedChangeListener((group, checkedId) -> {
                if (etEmail != null) {
                    etEmail.setText("");
                    etEmail.setError(null);
                }
                if (etPassword != null) {
                    etPassword.setText("");
                    etPassword.setError(null);
                }
                if (tilEmail != null) {
                    if (checkedId == R.id.rbTeacher) {
                        tilEmail.setHint(getString(R.string.teacher_code_or_email));
                    } else {
                        tilEmail.setHint(getString(R.string.student_id_or_email));
                    }
                }
            });
        }

        // Button Touch Feedback (0.98 Scale on Press)
        setupButtonTouchFeedback(btnLogin);

        // Login Button Click Listener
        if (btnLogin != null) btnLogin.setOnClickListener(v -> performLogin());

        // Dev Bypass Buttons
        MaterialButton btnDevStudent = findViewById(R.id.btnDevStudent);
        MaterialButton btnDevTeacher = findViewById(R.id.btnDevTeacher);

        if (btnDevStudent != null) {
            btnDevStudent.setOnClickListener(v -> {
                sessionManager.createLoginSession(
                    "Test Student (Dev)",
                    "student@bubt.edu.bd",
                    "22235103132",
                    "Student",
                    "6",
                    "6",
                    "50",
                    "Department of CSE, BUBT"
                );
                Toast.makeText(MainActivity.this, "Bypassing to Student Dashboard...", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(MainActivity.this, StudentDashboardActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            });
        }

        if (btnDevTeacher != null) {
            btnDevTeacher.setOnClickListener(v -> {
                sessionManager.createLoginSession(
                    "Test Teacher (Dev)",
                    "teacher@bubt.edu.bd",
                    "T1001",
                    "Teacher/Admin",
                    "--",
                    "6",
                    "--",
                    "Department of CSE, BUBT"
                );
                Toast.makeText(MainActivity.this, "Bypassing to Teacher Dashboard...", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(MainActivity.this, TeacherDashboardActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            });
        }

        // Forgot Password Click Listener -> Opens Dedicated 3-Step Forgot Password Screen
        if (tvForgotPassword != null) {
            tvForgotPassword.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, ForgotPasswordActivity.class);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            });
        }

        // Register Link Click Listener
        if (tvRegisterLink != null) {
            tvRegisterLink.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            });
        }

        setupHapticFeedback();
    }

    private void setupButtonTouchFeedback(View btn) {
        if (btn == null) return;
        btn.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                HapticUtils.mediumClick(v);
            }
            float durationScale = Settings.Global.getFloat(getContentResolver(), Settings.Global.ANIMATOR_DURATION_SCALE, 1.0f);
            if (durationScale > 0f) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        v.animate().scaleX(0.98f).scaleY(0.98f).setDuration(100).start();
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start();
                        break;
                }
            }
            return false;
        });
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if (v instanceof TextInputEditText || v instanceof EditText) {
                Rect outRect = new Rect();
                v.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int) ev.getRawX(), (int) ev.getRawY())) {
                    v.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    }
                }
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    private void setupHapticFeedback() {
        MaterialButton btnDevStudent = findViewById(R.id.btnDevStudent);
        MaterialButton btnDevTeacher = findViewById(R.id.btnDevTeacher);
        if (btnDevStudent != null) HapticUtils.attachLight(btnDevStudent);
        if (btnDevTeacher != null) HapticUtils.attachLight(btnDevTeacher);
        if (radioGroupRole != null) HapticUtils.attachLight(radioGroupRole);
        if (rbStudent != null) HapticUtils.attachLight(rbStudent);
        if (rbTeacher != null) HapticUtils.attachLight(rbTeacher);
        if (tvForgotPassword != null) HapticUtils.attachLight(tvForgotPassword);
        if (tvRegisterLink != null) HapticUtils.attachLight(tvRegisterLink);
    }

    private void performLogin() {
        String emailOrId = etEmail != null && etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword != null && etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

        if (emailOrId.isEmpty()) {
            if (tilEmail != null) tilEmail.setError("ID/Email is required!");
            if (etEmail != null) {
                etEmail.startAnimation(AnimationUtils.loadAnimation(this, R.anim.shake));
                etEmail.requestFocus();
            }
            return;
        }

        if (password.isEmpty()) {
            if (tilPassword != null) tilPassword.setError("Password is required!");
            if (etPassword != null) {
                etPassword.startAnimation(AnimationUtils.loadAnimation(this, R.anim.shake));
                etPassword.requestFocus();
            }
            return;
        }

        if (tilEmail != null) tilEmail.setError(null);
        if (tilPassword != null) tilPassword.setError(null);

        Toast.makeText(this, "Authenticating...", Toast.LENGTH_SHORT).show();
        btnLogin.setEnabled(false);

        if (emailOrId.contains("@")) {
            authenticateWithEmail(emailOrId, password, emailOrId);
        } else {
            String sanitizedId = emailOrId.replace(".", "_").replace("@", "_").replace("#", "_").replace("$", "_").replace("[", "_").replace("]", "_");
            DatabaseReference dbRef = FirebaseDatabase.getInstance("https://crconnect-58521-default-rtdb.firebaseio.com").getReference("crconnect_db");

            dbRef.child("users").child(sanitizedId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String registeredEmail = snapshot.exists() ? snapshot.child("email").getValue(String.class) : null;
                    if (registeredEmail != null && !registeredEmail.isEmpty()) {
                        authenticateWithEmail(registeredEmail, password, emailOrId);
                    } else {
                        authenticateWithEmail(emailOrId, password, emailOrId);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    authenticateWithEmail(emailOrId, password, emailOrId);
                }
            });
        }
    }

    private void createDefaultTeacherAndNavigate(String uid, String email, String originalInput) {
        String name = email != null ? email.split("@")[0] : "Teacher";
        String assignedSection = "6";
        String dept = "Department of CSE, BUBT";
        
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("name", name);
        userMap.put("email", email);
        userMap.put("id", originalInput != null && !originalInput.isEmpty() ? originalInput : "T1001");
        userMap.put("role", "Teacher/Admin");
        userMap.put("section", "--");
        userMap.put("assignedSection", assignedSection);
        userMap.put("intake", "--");
        userMap.put("dept", dept);
        userMap.put("deviceId", Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID));
        userMap.put("registeredAt", System.currentTimeMillis());

        DatabaseReference dbRef = FirebaseDatabase.getInstance("https://crconnect-58521-default-rtdb.firebaseio.com").getReference("crconnect_db");
        String safeEmail = email != null ? email : "";
        String safeInput = originalInput != null ? originalInput : "";
        String sanitizedEmail = safeEmail.replace(".", "_").replace("@", "_").replace("#", "_").replace("$", "_").replace("[", "_").replace("]", "_");
        String sanitizedInput = safeInput.replace(".", "_").replace("@", "_").replace("#", "_").replace("$", "_").replace("[", "_").replace("]", "_");

        if (uid != null && !uid.isEmpty()) {
            dbRef.child("users").child(uid).setValue(userMap);
        }
        dbRef.child("users").child(sanitizedEmail).setValue(userMap);
        if (!sanitizedInput.isEmpty()) {
            dbRef.child("users").child(sanitizedInput).setValue(userMap);
        }

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        if (uid != null && !uid.isEmpty()) {
            firestore.collection("users").document(uid).set(userMap);
        }
        firestore.collection("users").document(sanitizedEmail).set(userMap);
        if (!sanitizedInput.isEmpty()) {
            firestore.collection("users").document(sanitizedInput).set(userMap);
        }

        sessionManager.createLoginSession(
            name,
            email,
            originalInput != null && !originalInput.isEmpty() ? originalInput : "T1001",
            "Teacher/Admin",
            "--",
            assignedSection,
            "--",
            dept
        );
        Toast.makeText(MainActivity.this, "Welcome, " + name + " (Teacher Profile Initialized)", Toast.LENGTH_SHORT).show();
        navigateBasedOnRole("Teacher/Admin", email);
    }

    private void authenticateWithEmail(String email, String password, String originalInput) {
        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
            .addOnSuccessListener(authResult -> {
                FirebaseUser user = authResult.getUser();
                if (user == null) {
                    btnLogin.setEnabled(true);
                    Toast.makeText(MainActivity.this, "Authentication failed: User session invalid", Toast.LENGTH_LONG).show();
                    return;
                }

                user.reload().addOnCompleteListener(reloadTask -> {
                    FirebaseUser refreshedUser = FirebaseAuth.getInstance().getCurrentUser();
                    if (refreshedUser == null) {
                        FirebaseAuth.getInstance().signOut();
                        sessionManager.logoutUser();
                        btnLogin.setEnabled(true);
                        Toast.makeText(MainActivity.this, "Authentication failed", Toast.LENGTH_LONG).show();
                        return;
                    }

                    if (!refreshedUser.isEmailVerified()) {
                        FirebaseAuth.getInstance().signOut();
                        sessionManager.logoutUser();
                        btnLogin.setEnabled(true);
                        Toast.makeText(MainActivity.this, "Email Verification Required: Please check your inbox and verify your email address before logging in.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    String uid = refreshedUser.getUid();
                    DatabaseReference dbRef = FirebaseDatabase.getInstance("https://crconnect-58521-default-rtdb.firebaseio.com").getReference("crconnect_db");

                    dbRef.child("users").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                saveSessionAndNavigate(snapshot);
                            } else {
                                String userEmail = refreshedUser.getEmail();
                                String sanitizedEmail = userEmail != null ? userEmail.replace(".", "_").replace("@", "_").replace("#", "_").replace("$", "_").replace("[", "_").replace("]", "_") : "";
                                String sanitizedInput = originalInput.replace(".", "_").replace("@", "_").replace("#", "_").replace("$", "_").replace("[", "_").replace("]", "_");

                                dbRef.child("users").child(sanitizedInput).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot inputSnapshot) {
                                        if (inputSnapshot.exists()) {
                                            saveSessionAndNavigate(inputSnapshot);
                                        } else {
                                            dbRef.child("users").child(sanitizedEmail).addListenerForSingleValueEvent(new ValueEventListener() {
                                                @Override
                                                public void onDataChange(@NonNull DataSnapshot emailSnapshot) {
                                                    if (emailSnapshot.exists()) {
                                                        saveSessionAndNavigate(emailSnapshot);
                                                    } else {
                                                        FirebaseFirestore.getInstance().collection("users").document(uid).get()
                                                            .addOnSuccessListener(doc -> {
                                                                if (doc.exists()) {
                                                                    String name = doc.getString("name");
                                                                    String role = doc.getString("role");
                                                                    String id = doc.getString("id");
                                                                    String dbEmail = doc.getString("email");
                                                                    String section = doc.getString("section");
                                                                    String assignedSection = doc.getString("assignedSection");
                                                                    String intake = doc.getString("intake");
                                                                    String dept = doc.getString("dept");

                                                                    String resolvedRole = role != null ? role : ((dbEmail != null && dbEmail.toLowerCase().endsWith("@bubt.edu.bd")) ? "Teacher" : "Student");
                                                                    sessionManager.createLoginSession(
                                                                        name != null ? name : email,
                                                                        dbEmail != null ? dbEmail : email,
                                                                        id != null ? id : originalInput,
                                                                        resolvedRole,
                                                                        section != null ? section : "--",
                                                                        assignedSection != null ? assignedSection : (section != null ? section : "--"),
                                                                        intake != null ? intake : "--",
                                                                        dept != null ? dept : "Department of CSE, BUBT"
                                                                    );
                                                                    Toast.makeText(MainActivity.this, "Welcome back, " + (name != null ? name : email) + "!", Toast.LENGTH_SHORT).show();
                                                                    navigateBasedOnRole(resolvedRole, dbEmail != null ? dbEmail : email);
                                                                } else {
                                                                    String userEmail = refreshedUser.getEmail();
                                                                    if (isTeacherRole(null, userEmail) || (userEmail != null && userEmail.toLowerCase().endsWith("@bubt.edu.bd")) || (rbTeacher != null && rbTeacher.isChecked())) {
                                                                        createDefaultTeacherAndNavigate(uid, userEmail != null ? userEmail : email, originalInput);
                                                                    } else {
                                                                        btnLogin.setEnabled(true);
                                                                        FirebaseAuth.getInstance().signOut();
                                                                        sessionManager.logoutUser();
                                                                        Toast.makeText(MainActivity.this, "User profile not found in database. Please register first.", Toast.LENGTH_LONG).show();
                                                                    }
                                                                }
                                                            })
                                                            .addOnFailureListener(e -> {
                                                                String userEmail = refreshedUser.getEmail();
                                                                if (isTeacherRole(null, userEmail) || (userEmail != null && userEmail.toLowerCase().endsWith("@bubt.edu.bd")) || (rbTeacher != null && rbTeacher.isChecked())) {
                                                                    createDefaultTeacherAndNavigate(uid, userEmail != null ? userEmail : email, originalInput);
                                                                } else {
                                                                    btnLogin.setEnabled(true);
                                                                    FirebaseAuth.getInstance().signOut();
                                                                    sessionManager.logoutUser();
                                                                    Toast.makeText(MainActivity.this, "User profile not found. Please register first.", Toast.LENGTH_LONG).show();
                                                                }
                                                            });
                                                    }
                                                }

                                                @Override
                                                public void onCancelled(@NonNull DatabaseError error) {
                                                    btnLogin.setEnabled(true);
                                                    Toast.makeText(MainActivity.this, "Database error: " + error.getMessage(), Toast.LENGTH_LONG).show();
                                                }
                                            });
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        btnLogin.setEnabled(true);
                                        Toast.makeText(MainActivity.this, "Database error: " + error.getMessage(), Toast.LENGTH_LONG).show();
                                    }
                                });
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            btnLogin.setEnabled(true);
                            Toast.makeText(MainActivity.this, "Database error: " + error.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                });
            })
            .addOnFailureListener(e -> {
                btnLogin.setEnabled(true);
                Log.e("MainActivity", "Firebase Auth login failed", e);
                String errorMsg = e.getLocalizedMessage();
                if (errorMsg != null && (errorMsg.contains("no user record") || errorMsg.contains("password is invalid") || errorMsg.contains("malformed"))) {
                    errorMsg = "Invalid email/ID or password. Please check your credentials or register.";
                }
                Toast.makeText(MainActivity.this, "Login Failed: " + errorMsg, Toast.LENGTH_LONG).show();
            });
    }

    private boolean isTeacherRole(String role, String email) {
        if (role != null) {
            String lowerRole = role.toLowerCase();
            if (lowerRole.contains("teacher") || lowerRole.contains("admin")) {
                return true;
            }
        }
        if (email != null && email.toLowerCase().endsWith("@bubt.edu.bd")) {
            return true;
        }
        return false;
    }

    private void saveSessionAndNavigate(DataSnapshot snapshot) {
        String name = snapshot.child("name").getValue(String.class);
        String role = snapshot.child("role").getValue(String.class);
        String id = snapshot.child("id").getValue(String.class);
        String userEmail = snapshot.child("email").getValue(String.class);
        String section = snapshot.child("section").getValue(String.class);
        String assignedSection = snapshot.child("assignedSection").getValue(String.class);
        String intake = snapshot.child("intake").getValue(String.class);
        String dept = snapshot.child("dept").getValue(String.class);

        if (name == null) name = "User";
        if (userEmail == null) userEmail = "";
        if (role == null || role.trim().isEmpty()) {
            role = isTeacherRole(null, userEmail) ? "Teacher" : "Student";
        }
        if (id == null) id = "";
        if (section == null) section = "--";
        if (assignedSection == null) assignedSection = section;
        if (intake == null) intake = "--";
        if (dept == null) dept = "Department of CSE, BUBT";

        sessionManager.createLoginSession(name, userEmail, id, role, section, assignedSection, intake, dept);
        Toast.makeText(MainActivity.this, "Welcome back, " + name + "!", Toast.LENGTH_SHORT).show();
        navigateBasedOnRole(role, userEmail);
    }

    private void navigateBasedOnRole(String role, String email) {
        Intent intent;
        if (isTeacherRole(role, email)) {
            intent = new Intent(MainActivity.this, TeacherDashboardActivity.class);
        } else {
            intent = new Intent(MainActivity.this, StudentDashboardActivity.class);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}
