package com.example.crconnect;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
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

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import android.util.Log;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etPassword;
    private TextInputLayout tilEmail;
    private RadioGroup radioGroupRole;
    private RadioButton rbStudent, rbTeacher;
    private Button btnLogin;
    private TextView tvRegisterLink;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // --- TRUE FULL SCREEN (GOOGLE STYLE) ---
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
        
        // Force the gradient background to the entire window (Removes white bars / black flashes)
        getWindow().setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.bg_gradient));

        setContentView(R.layout.activity_main);
        
        sessionManager = new SessionManager(this);
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null && !currentUser.isAnonymous()) {
            currentUser.reload().addOnCompleteListener(task -> {
                FirebaseUser refreshedUser = FirebaseAuth.getInstance().getCurrentUser();
                if (refreshedUser != null && !refreshedUser.isEmailVerified()) {
                    FirebaseAuth.getInstance().signOut();
                    sessionManager.logoutUser();
                    Toast.makeText(MainActivity.this, "Please verify your email first.", Toast.LENGTH_LONG).show();
                } else if (sessionManager.isLoggedIn()) {
                    navigateBasedOnRole(sessionManager.getUserRole());
                    finish();
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                }
            });
        } else if (sessionManager.isLoggedIn()) {
            navigateBasedOnRole(sessionManager.getUserRole());
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            return;
        }

        // Safe Area Padding (Content won't touch the bars)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, Math.max(systemBars.bottom, ime.bottom));
            return WindowInsetsCompat.CONSUMED;
        });

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

        // Professional keyboard-aware scaling & auto-scroll
        ScrollView scrollViewMain = findViewById(R.id.scrollViewMain);
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
            if (checkedId == R.id.rbTeacher) {
                tilEmail.setHint("Teacher Code or Email");
            } else {
                tilEmail.setHint("Student ID or BUBT Email");
            }
        });

        // Login Button Click Listener
        btnLogin.setOnClickListener(v -> performLogin());

        // Google Sign In Click Listener
        MaterialButton btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);
        btnGoogleSignIn.setOnClickListener(v -> performGoogleSignIn());

        // Register Link Click Listener
        tvRegisterLink.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
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
        
        Toast.makeText(this, "Logging in...", Toast.LENGTH_SHORT).show();

        String sanitizedId = emailOrId.replace(".", "_").replace("@", "_").replace("#", "_").replace("$", "_").replace("[", "_").replace("]", "_");

        FirebaseDatabase.getInstance("https://crconnect-58521-default-rtdb.firebaseio.com")
            .getReference("crconnect_db")
            .child("users")
            .child(sanitizedId)
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String name, role, id, section, intake, dept, userEmail;
                    if (snapshot.exists()) {
                        name = snapshot.child("name").getValue(String.class);
                        role = snapshot.child("role").getValue(String.class);
                        id = snapshot.child("id").getValue(String.class);
                        userEmail = snapshot.child("email").getValue(String.class);
                        section = snapshot.child("section").getValue(String.class);
                        intake = snapshot.child("intake").getValue(String.class);
                        dept = snapshot.child("dept").getValue(String.class);
                    } else {
                        role = rbTeacher.isChecked() ? "Teacher/Admin" : "Student";
                        name = rbTeacher.isChecked() ? "Teacher " + emailOrId : "Student " + emailOrId;
                        id = emailOrId;
                        userEmail = emailOrId.contains("@") ? emailOrId : emailOrId + "@bubt.edu.bd";
                        section = "55/8";
                        intake = "55";
                        dept = "CSE";
                    }

                    if (userEmail == null || userEmail.isEmpty()) {
                        userEmail = emailOrId.contains("@") ? emailOrId : emailOrId + "@bubt.edu.bd";
                    }

                    final String finalName = name != null ? name : emailOrId;
                    final String finalRole = role != null ? role : "Student";
                    final String finalId = id != null ? id : emailOrId;
                    final String finalEmail = userEmail;
                    final String finalSection = section != null ? section : "--";
                    final String finalIntake = intake != null ? intake : "--";
                    final String finalDept = dept != null ? dept : "CSE";

                    FirebaseAuth.getInstance().signInWithEmailAndPassword(finalEmail, password)
                        .addOnSuccessListener(authResult -> {
                            FirebaseUser user = authResult.getUser();
                            if (user != null && !user.isEmailVerified()) {
                                FirebaseAuth.getInstance().signOut();
                                sessionManager.logoutUser();
                                Toast.makeText(MainActivity.this, "Please verify your email first.", Toast.LENGTH_LONG).show();
                                return;
                            }

                            String uid = user != null ? user.getUid() : "";
                            if (!uid.isEmpty()) {
                                FirebaseDatabase.getInstance("https://crconnect-58521-default-rtdb.firebaseio.com")
                                    .getReference("crconnect_db")
                                    .child("users")
                                    .child(uid)
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot uidSnapshot) {
                                            if (uidSnapshot.exists()) {
                                                String fetchName = uidSnapshot.child("name").getValue(String.class);
                                                String fetchRole = uidSnapshot.child("role").getValue(String.class);
                                                String fetchId = uidSnapshot.child("id").getValue(String.class);
                                                String fetchEmail = uidSnapshot.child("email").getValue(String.class);
                                                String fetchSection = uidSnapshot.child("section").getValue(String.class);
                                                String fetchIntake = uidSnapshot.child("intake").getValue(String.class);
                                                String fetchDept = uidSnapshot.child("dept").getValue(String.class);

                                                sessionManager.createLoginSession(
                                                    fetchName != null ? fetchName : finalName,
                                                    fetchEmail != null ? fetchEmail : finalEmail,
                                                    fetchId != null ? fetchId : finalId,
                                                    fetchRole != null ? fetchRole : finalRole,
                                                    fetchSection != null ? fetchSection : finalSection,
                                                    fetchIntake != null ? fetchIntake : finalIntake,
                                                    fetchDept != null ? fetchDept : finalDept
                                                );
                                                Toast.makeText(MainActivity.this, "Welcome " + (fetchName != null ? fetchName : finalName), Toast.LENGTH_SHORT).show();
                                                navigateBasedOnRole(fetchRole != null ? fetchRole : finalRole);
                                                finish();
                                            } else {
                                                // Fallback to Firestore
                                                FirebaseFirestore.getInstance()
                                                    .collection("users")
                                                    .document(uid)
                                                    .get()
                                                    .addOnSuccessListener(doc -> {
                                                        if (doc.exists()) {
                                                            String fetchName = doc.getString("name");
                                                            String fetchRole = doc.getString("role");
                                                            String fetchId = doc.getString("id");
                                                            String fetchEmail = doc.getString("email");
                                                            String fetchSection = doc.getString("section");
                                                            String fetchIntake = doc.getString("intake");
                                                            String fetchDept = doc.getString("dept");

                                                            sessionManager.createLoginSession(
                                                                fetchName != null ? fetchName : finalName,
                                                                fetchEmail != null ? fetchEmail : finalEmail,
                                                                fetchId != null ? fetchId : finalId,
                                                                fetchRole != null ? fetchRole : finalRole,
                                                                fetchSection != null ? fetchSection : finalSection,
                                                                fetchIntake != null ? fetchIntake : finalIntake,
                                                                fetchDept != null ? fetchDept : finalDept
                                                            );
                                                            Toast.makeText(MainActivity.this, "Welcome " + (fetchName != null ? fetchName : finalName), Toast.LENGTH_SHORT).show();
                                                            navigateBasedOnRole(fetchRole != null ? fetchRole : finalRole);
                                                            finish();
                                                        } else {
                                                            sessionManager.createLoginSession(finalName, finalEmail, finalId, finalRole, finalSection, finalIntake, finalDept);
                                                            Toast.makeText(MainActivity.this, "Welcome " + finalName, Toast.LENGTH_SHORT).show();
                                                            navigateBasedOnRole(finalRole);
                                                            finish();
                                                        }
                                                    })
                                                    .addOnFailureListener(fsErr -> {
                                                        sessionManager.createLoginSession(finalName, finalEmail, finalId, finalRole, finalSection, finalIntake, finalDept);
                                                        Toast.makeText(MainActivity.this, "Welcome " + finalName, Toast.LENGTH_SHORT).show();
                                                        navigateBasedOnRole(finalRole);
                                                        finish();
                                                    });
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError dbError) {
                                            sessionManager.createLoginSession(finalName, finalEmail, finalId, finalRole, finalSection, finalIntake, finalDept);
                                            Toast.makeText(MainActivity.this, "Welcome " + finalName, Toast.LENGTH_SHORT).show();
                                            navigateBasedOnRole(finalRole);
                                            finish();
                                        }
                                    });
                            } else {
                                sessionManager.createLoginSession(finalName, finalEmail, finalId, finalRole, finalSection, finalIntake, finalDept);
                                Toast.makeText(MainActivity.this, "Welcome " + finalName, Toast.LENGTH_SHORT).show();
                                navigateBasedOnRole(finalRole);
                                finish();
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.w("MainActivity", "Firebase Auth sign-in failed: " + e.getMessage());
                            FirebaseAuth.getInstance().signInAnonymously()
                                .addOnSuccessListener(anonResult -> {
                                    sessionManager.createLoginSession(finalName, finalEmail, finalId, finalRole, finalSection, finalIntake, finalDept);
                                    Toast.makeText(MainActivity.this, "Welcome " + finalName, Toast.LENGTH_SHORT).show();
                                    navigateBasedOnRole(finalRole);
                                    finish();
                                })
                                .addOnFailureListener(anonErr -> {
                                    Log.w("MainActivity", "Firebase Auth anonymous sign-in failed: " + anonErr.getMessage());
                                    sessionManager.createLoginSession(finalName, finalEmail, finalId, finalRole, finalSection, finalIntake, finalDept);
                                    Toast.makeText(MainActivity.this, "Welcome " + finalName, Toast.LENGTH_SHORT).show();
                                    navigateBasedOnRole(finalRole);
                                    finish();
                                });
                        });
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(MainActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void fetchUserDataAndSaveSession(String uid) {
        FirebaseDatabase.getInstance()
                .getReference("crconnect_db")
                .child("users")
                .child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String name = snapshot.child("name").getValue(String.class);
                    String email = snapshot.child("email").getValue(String.class);
                    String id = snapshot.child("id").getValue(String.class);
                    String role = snapshot.child("role").getValue(String.class);
                    String section = snapshot.child("section").getValue(String.class);
                    String intake = snapshot.child("intake").getValue(String.class);
                    String dept = snapshot.child("dept").getValue(String.class);

                    sessionManager.createLoginSession(name, email, id, role, section, intake, dept);
                    
                    Toast.makeText(MainActivity.this, "Welcome " + name, Toast.LENGTH_SHORT).show();
                    navigateBasedOnRole(role);
                    finish();
                } else {
                    Toast.makeText(MainActivity.this, "User profile not found in database", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, "Database Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void performGoogleSignIn() {
        Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private void navigateBasedOnRole(String role) {
        Intent intent;
        if ("Teacher/Admin".equals(role)) {
            intent = new Intent(MainActivity.this, TeacherDashboardActivity.class);
        } else {
            intent = new Intent(MainActivity.this, StudentDashboardActivity.class);
        }
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}
