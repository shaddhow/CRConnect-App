package com.example.crconnect;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import androidx.core.widget.NestedScrollView;
import android.widget.TextView;
import android.widget.Toast;

import android.os.Build;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import android.widget.LinearLayout;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.example.crconnect.core.utils.HapticUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
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
import android.provider.Settings;

public class MainActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etPassword;
    private TextInputLayout tilEmail;
    private RadioGroup radioGroupRole;
    private RadioButton rbStudent, rbTeacher;
    private Button btnLogin;
    private TextView tvRegisterLink, tvForgotPassword;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // --- STRICT EDGE-TO-EDGE FULL SCREEN ---
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getWindow().setNavigationBarContrastEnforced(false);
        }
        WindowInsetsControllerCompat insetsController = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (insetsController != null) {
            insetsController.setAppearanceLightStatusBars(false);
            insetsController.setAppearanceLightNavigationBars(false);
        }
        getWindow().setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.bg_gradient));

        setContentView(R.layout.activity_main);
        
        sessionManager = new SessionManager(this);
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null && !currentUser.isAnonymous()) {
            currentUser.reload().addOnCompleteListener(task -> {
                FirebaseUser refreshedUser = FirebaseAuth.getInstance().getCurrentUser();
                if (refreshedUser != null && refreshedUser.isEmailVerified()) {
                    if (sessionManager.isLoggedIn()) {
                        navigateBasedOnRole(sessionManager.getUserRole(), sessionManager.getUserEmail());
                        finish();
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
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
            // Strictly require active Firebase user; clear stale local session
            FirebaseAuth.getInstance().signOut();
            sessionManager.logoutUser();
        }

        // Safe Area Padding
        View scrollMain = findViewById(R.id.scrollViewMain);
        if (scrollMain != null) {
            ViewCompat.setOnApplyWindowInsetsListener(scrollMain, (v, insets) -> {
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
        radioGroupRole = findViewById(R.id.radioGroupRole);
        rbStudent = findViewById(R.id.rbStudent);
        rbTeacher = findViewById(R.id.rbTeacher);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegisterLink = findViewById(R.id.tvRegisterLink);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);

        // Professional keyboard-aware scaling & auto-scroll
        NestedScrollView scrollViewMain = findViewById(R.id.scrollViewMain);
        View layoutBranding = findViewById(R.id.layoutBranding);
        View rootView = findViewById(android.R.id.content);

        if (layoutBranding != null) {
            layoutBranding.post(() -> {
                layoutBranding.setPivotX(layoutBranding.getWidth() / 2f);
                layoutBranding.setPivotY(0f); // Scale from top downwards, centered horizontally
            });
        }

        if (rootView != null && layoutBranding != null) {
            rootView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
                Rect r = new Rect();
                rootView.getWindowVisibleDisplayFrame(r);
                int screenHeight = rootView.getRootView().getHeight();
                int keypadHeight = screenHeight - r.bottom;

                boolean isKeyboardOpen = keypadHeight > screenHeight * 0.15;

                if (isKeyboardOpen) {
                    layoutBranding.animate()
                        .scaleX(0.8f)
                        .scaleY(0.8f)
                        .setDuration(200)
                        .start();
                } else {
                    layoutBranding.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(200)
                        .start();
                }
            });
        }

        // Auto-scroll up smoothly when password field gets focus
        etPassword.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && scrollViewMain != null) {
                scrollViewMain.postDelayed(() -> scrollViewMain.smoothScrollTo(0, btnLogin.getBottom()), 200);
            }
        });

        // Role change dynamic hint logic
        radioGroupRole.setOnCheckedChangeListener((group, checkedId) -> {
            etEmail.setText("");
            etPassword.setText("");
            etEmail.setError(null);
            etPassword.setError(null);
            if (checkedId == R.id.rbTeacher) {
                tilEmail.setHint("Teacher Code or Email");
                etEmail.setHint("Teacher Code or Email");
            } else {
                tilEmail.setHint("Student ID or BUBT Email");
                etEmail.setHint("Student ID or BUBT Email");
            }
        });

        // Login Button Click Listener
        btnLogin.setOnClickListener(v -> performLogin());

        // Dev Bypass Buttons Click Listeners
        MaterialButton btnDevStudent = findViewById(R.id.btnDevStudent);
        MaterialButton btnDevTeacher = findViewById(R.id.btnDevTeacher);

        if (btnDevStudent != null) {
            btnDevStudent.setOnClickListener(v -> {
                sessionManager.createLoginSession(
                    "Test Student (Dev)",
                    "student@bubt.edu.bd",
                    "22235103132",
                    "Student",
                    "5A",
                    "5A",
                    "47",
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
                    "5A",
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

        // Forgot Password Click Listener
        tvForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());

        // Register Link Click Listener
        tvRegisterLink.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        setupHapticFeedback();
    }

    private void setupHapticFeedback() {
        if (btnLogin != null) HapticUtils.attachMedium(btnLogin);
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

    private void showForgotPasswordDialog() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        
        LinearLayout view = new LinearLayout(this);
        view.setOrientation(LinearLayout.VERTICAL);
        view.setPadding(48, 48, 48, 48);
        view.setBackground(ContextCompat.getDrawable(this, R.drawable.glass_card_bg));

        TextView tvTitle = new TextView(this);
        tvTitle.setText("Reset Password");
        tvTitle.setTextSize(22f);
        tvTitle.setTypeface(null, Typeface.BOLD);
        tvTitle.setTextColor(Color.parseColor("#F8FAFC"));

        TextView tvSubtitle = new TextView(this);
        tvSubtitle.setText("Enter your registered email address to receive a secure password reset link.");
        tvSubtitle.setTextSize(14f);
        tvSubtitle.setTextColor(Color.parseColor("#94A3B8"));
        tvSubtitle.setPadding(0, 12, 0, 24);

        final TextInputLayout tilResetEmail = new TextInputLayout(this, null, com.google.android.material.R.style.Widget_Material3_TextInputLayout_OutlinedBox);
        tilResetEmail.setHint("Email Address");
        tilResetEmail.setBoxStrokeColor(Color.parseColor("#6366F1"));
        tilResetEmail.setHintTextColor(ColorStateList.valueOf(Color.parseColor("#6366F1")));

        final TextInputEditText etResetEmail = new TextInputEditText(tilResetEmail.getContext());
        etResetEmail.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        etResetEmail.setTextColor(Color.parseColor("#FFFFFF"));
        etResetEmail.setTextSize(16f);

        String currentInput = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        if (currentInput.contains("@")) {
            etResetEmail.setText(currentInput);
        }

        tilResetEmail.addView(etResetEmail);

        LinearLayout btnLayout = new LinearLayout(this);
        btnLayout.setOrientation(LinearLayout.HORIZONTAL);
        btnLayout.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams btnLayoutLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        btnLayoutLp.setMargins(0, 24, 0, 0);
        btnLayout.setLayoutParams(btnLayoutLp);

        MaterialButton btnCancel = new MaterialButton(this);
        btnCancel.setText("Cancel");
        btnCancel.setTextColor(Color.parseColor("#FFFFFF"));
        btnCancel.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#334155")));
        btnCancel.setCornerRadius(12);
        btnCancel.setTypeface(null, Typeface.BOLD);
        btnCancel.setAllCaps(false);
        LinearLayout.LayoutParams lpCancel = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        lpCancel.setMargins(0, 0, 6, 0);
        btnCancel.setLayoutParams(lpCancel);
        btnCancel.setOnClickListener(v -> bottomSheetDialog.dismiss());

        MaterialButton btnSend = new MaterialButton(this);
        btnSend.setText("Send Reset Link");
        btnSend.setTextColor(Color.parseColor("#FFFFFF"));
        btnSend.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#6366F1")));
        btnSend.setCornerRadius(12);
        btnSend.setTypeface(null, Typeface.BOLD);
        btnSend.setAllCaps(false);
        LinearLayout.LayoutParams lpSend = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        lpSend.setMargins(6, 0, 0, 0);
        btnSend.setLayoutParams(lpSend);

        btnSend.setOnClickListener(v -> {
            String email = etResetEmail.getText() != null ? etResetEmail.getText().toString().trim() : "";
            
            if (email.isEmpty()) {
                Toast.makeText(MainActivity.this, "Email is required for password reset!", Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                .addOnSuccessListener(unused -> {
                    bottomSheetDialog.dismiss();
                    Toast.makeText(MainActivity.this, "Password reset link sent to your email. Please check your inbox.", Toast.LENGTH_LONG).show();
                    showSuccessDialog(email);
                })
                .addOnFailureListener(e -> {
                    Log.e("MainActivity", "Failed to send password reset email", e);
                    String errorMsg = e.getLocalizedMessage();
                    if (errorMsg != null && errorMsg.contains("there is no user record")) {
                        errorMsg = "No account found matching " + email + ". Please verify your email or register a new account.";
                    }
                    Toast.makeText(MainActivity.this, "Error: " + errorMsg, Toast.LENGTH_LONG).show();
                });
        });

        btnLayout.addView(btnCancel);
        btnLayout.addView(btnSend);

        view.addView(tvTitle);
        view.addView(tvSubtitle);
        view.addView(tilResetEmail);
        view.addView(btnLayout);

        bottomSheetDialog.setContentView(view);
        bottomSheetDialog.show();
    }

    private void showSuccessDialog(String email) {
        BottomSheetDialog successSheet = new BottomSheetDialog(this);
        LinearLayout view = new LinearLayout(this);
        view.setOrientation(LinearLayout.VERTICAL);
        view.setPadding(48, 48, 48, 48);
        view.setBackground(ContextCompat.getDrawable(this, R.drawable.glass_card_bg));

        TextView tvTitle = new TextView(this);
        tvTitle.setText("Reset Link Sent");
        tvTitle.setTextSize(22f);
        tvTitle.setTypeface(null, Typeface.BOLD);
        tvTitle.setTextColor(Color.parseColor("#10B981"));

        TextView tvMsg = new TextView(this);
        tvMsg.setText("Password reset link sent to " + email + ". Please check your inbox and spam folder.");
        tvMsg.setTextSize(15f);
        tvMsg.setTextColor(Color.parseColor("#94A3B8"));
        tvMsg.setPadding(0, 12, 0, 24);

        MaterialButton btnOk = new MaterialButton(this);
        btnOk.setText("OK");
        btnOk.setTextColor(Color.parseColor("#FFFFFF"));
        btnOk.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#10B981")));
        btnOk.setCornerRadius(16);
        btnOk.setLayoutParams(new LinearLayout.LayoutParams(-1, 56));
        btnOk.setOnClickListener(v -> successSheet.dismiss());

        view.addView(tvTitle);
        view.addView(tvMsg);
        view.addView(btnOk);

        successSheet.setContentView(view);
        successSheet.show();
    }

    private void performLogin() {
        String emailOrId = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

        if (emailOrId.isEmpty()) {
            etEmail.setError("ID/Email is required!");
            etEmail.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            etPassword.setError("Password is required!");
            etPassword.requestFocus();
            return;
        }

        Toast.makeText(this, "Authenticating...", Toast.LENGTH_SHORT).show();
        btnLogin.setEnabled(false);

        if (emailOrId.contains("@")) {
            authenticateWithEmail(emailOrId, password, emailOrId);
        } else {
            // If user typed Student ID or Teacher Code, look up their registered email from Firebase Realtime Database
            String sanitizedId = emailOrId.replace(".", "_").replace("@", "_").replace("#", "_").replace("$", "_").replace("[", "_").replace("]", "_");
            DatabaseReference dbRef = FirebaseDatabase.getInstance("https://crconnect-58521-default-rtdb.firebaseio.com").getReference("crconnect_db");

            dbRef.child("users").child(sanitizedId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String registeredEmail = snapshot.exists() ? snapshot.child("email").getValue(String.class) : null;
                    if (registeredEmail != null && !registeredEmail.isEmpty()) {
                        authenticateWithEmail(registeredEmail, password, emailOrId);
                    } else {
                        // Fallback attempt
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
        String assignedSection = "5A";
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
        finish();
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
                                                                    finish();
                                                                } else {
                                                                    String userEmail = refreshedUser.getEmail();
                                                                    if (isTeacherRole(null, userEmail) || (userEmail != null && userEmail.toLowerCase().endsWith("@bubt.edu.bd")) || rbTeacher.isChecked()) {
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
                                                                if (isTeacherRole(null, userEmail) || (userEmail != null && userEmail.toLowerCase().endsWith("@bubt.edu.bd")) || rbTeacher.isChecked()) {
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
        finish();
    }

    private void navigateBasedOnRole(String role, String email) {
        Intent intent;
        if (isTeacherRole(role, email)) {
            intent = new Intent(MainActivity.this, TeacherDashboardActivity.class);
        } else {
            intent = new Intent(MainActivity.this, StudentDashboardActivity.class);
        }
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}
