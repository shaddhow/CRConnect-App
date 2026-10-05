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

import android.widget.LinearLayout;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
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
                if (refreshedUser != null) {
                    String refreshedEmail = refreshedUser.getEmail();
                    boolean isValidDomain = refreshedEmail != null &&
                        (refreshedEmail.toLowerCase().endsWith("@bubt.edu.bd") || refreshedEmail.toLowerCase().endsWith("@cse.bubt.edu.bd"));

                    if (!refreshedUser.isEmailVerified()) {
                        FirebaseAuth.getInstance().signOut();
                        sessionManager.logoutUser();
                        Toast.makeText(MainActivity.this, "Please verify your BUBT email first.", Toast.LENGTH_LONG).show();
                    } else if (!isValidDomain) {
                        FirebaseAuth.getInstance().signOut();
                        sessionManager.logoutUser();
                        Toast.makeText(MainActivity.this, "Only official BUBT emails (@bubt.edu.bd or @cse.bubt.edu.bd) are allowed.", Toast.LENGTH_LONG).show();
                    } else if (sessionManager.isLoggedIn()) {
                        navigateBasedOnRole(sessionManager.getUserRole());
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
        tvForgotPassword = findViewById(R.id.tvForgotPassword);

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

        // Forgot Password Click Listener
        tvForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());

        // Register Link Click Listener
        tvRegisterLink.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
    }

    private void showForgotPasswordDialog() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        
        LinearLayout view = new LinearLayout(this);
        view.setOrientation(LinearLayout.VERTICAL);
        view.setPadding(48, 48, 48, 48);
        view.setBackgroundColor(Color.parseColor("#171A29"));

        TextView tvTitle = new TextView(this);
        tvTitle.setText("🔑 Reset Password");
        tvTitle.setTextSize(22f);
        tvTitle.setTypeface(null, Typeface.BOLD);
        tvTitle.setTextColor(Color.parseColor("#F8FAFC"));

        TextView tvSubtitle = new TextView(this);
        tvSubtitle.setText("Enter your registered BUBT email (@bubt.edu.bd or @cse.bubt.edu.bd) to receive a secure password reset link.");
        tvSubtitle.setTextSize(14f);
        tvSubtitle.setTextColor(Color.parseColor("#94A3B8"));
        tvSubtitle.setPadding(0, 12, 0, 24);

        final TextInputLayout tilResetEmail = new TextInputLayout(this, null, com.google.android.material.R.style.Widget_Material3_TextInputLayout_OutlinedBox);
        tilResetEmail.setHint("BUBT Email Address");
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
        btnLayout.setGravity(Gravity.END);
        btnLayout.setPadding(0, 28, 0, 0);

        MaterialButton btnCancel = new MaterialButton(this, null, com.google.android.material.R.style.Widget_Material3_Button_TextButton);
        btnCancel.setText("Cancel");
        btnCancel.setTextColor(Color.parseColor("#94A3B8"));
        btnCancel.setOnClickListener(v -> bottomSheetDialog.dismiss());

        MaterialButton btnSend = new MaterialButton(this);
        btnSend.setText("Send Reset Link");
        btnSend.setTextColor(Color.parseColor("#FFFFFF"));
        btnSend.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#6366F1")));
        btnSend.setCornerRadius(16);
        LinearLayout.LayoutParams sendLp = new LinearLayout.LayoutParams(-2, -2);
        sendLp.setMargins(16, 0, 0, 0);
        btnSend.setLayoutParams(sendLp);

        btnSend.setOnClickListener(v -> {
            String email = etResetEmail.getText() != null ? etResetEmail.getText().toString().trim() : "";
            
            if (email.isEmpty()) {
                Toast.makeText(MainActivity.this, "Email is required for password reset!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!email.toLowerCase().endsWith("@bubt.edu.bd") && !email.toLowerCase().endsWith("@cse.bubt.edu.bd")) {
                Toast.makeText(MainActivity.this, "Please enter a valid BUBT email (@bubt.edu.bd or @cse.bubt.edu.bd)", Toast.LENGTH_LONG).show();
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
        view.setBackgroundColor(Color.parseColor("#171A29"));

        TextView tvTitle = new TextView(this);
        tvTitle.setText("✅ Reset Link Sent");
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

        final String email;
        if (emailOrId.contains("@")) {
            email = emailOrId;
        } else {
            email = emailOrId + "@bubt.edu.bd";
        }

        if (!email.toLowerCase().endsWith("@bubt.edu.bd") && !email.toLowerCase().endsWith("@cse.bubt.edu.bd")) {
            etEmail.setError("Only official BUBT emails (@bubt.edu.bd or @cse.bubt.edu.bd) are allowed!");
            etEmail.requestFocus();
            return;
        }

        Toast.makeText(this, "Authenticating...", Toast.LENGTH_SHORT).show();
        btnLogin.setEnabled(false);

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
                    if (refreshedUser == null || !refreshedUser.isEmailVerified()) {
                        FirebaseAuth.getInstance().signOut();
                        sessionManager.logoutUser();
                        btnLogin.setEnabled(true);
                        Toast.makeText(MainActivity.this, "Email not verified. Please check your inbox and verify your BUBT email before logging in.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    String userEmail = refreshedUser.getEmail();
                    if (userEmail == null || (!userEmail.toLowerCase().endsWith("@bubt.edu.bd") && !userEmail.toLowerCase().endsWith("@cse.bubt.edu.bd"))) {
                        FirebaseAuth.getInstance().signOut();
                        sessionManager.logoutUser();
                        btnLogin.setEnabled(true);
                        Toast.makeText(MainActivity.this, "Access Denied: Only official BUBT emails (@bubt.edu.bd or @cse.bubt.edu.bd) are allowed.", Toast.LENGTH_LONG).show();
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
                                String sanitizedId = emailOrId.replace(".", "_").replace("@", "_").replace("#", "_").replace("$", "_").replace("[", "_").replace("]", "_");
                                dbRef.child("users").child(sanitizedId).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot idSnapshot) {
                                        if (idSnapshot.exists()) {
                                            saveSessionAndNavigate(idSnapshot);
                                        } else {
                                            FirebaseFirestore.getInstance().collection("users").document(uid).get()
                                                .addOnSuccessListener(doc -> {
                                                    if (doc.exists()) {
                                                        String name = doc.getString("name");
                                                        String role = doc.getString("role");
                                                        String id = doc.getString("id");
                                                        String dbEmail = doc.getString("email");
                                                        String section = doc.getString("section");
                                                        String intake = doc.getString("intake");
                                                        String dept = doc.getString("dept");

                                                        sessionManager.createLoginSession(
                                                            name != null ? name : email,
                                                            dbEmail != null ? dbEmail : email,
                                                            id != null ? id : emailOrId,
                                                            role != null ? role : "Student",
                                                            section != null ? section : "--",
                                                            intake != null ? intake : "--",
                                                            dept != null ? dept : "Department of CSE, BUBT"
                                                        );
                                                        Toast.makeText(MainActivity.this, "Welcome back, " + (name != null ? name : email) + "!", Toast.LENGTH_SHORT).show();
                                                        navigateBasedOnRole(role != null ? role : "Student");
                                                        finish();
                                                    } else {
                                                        btnLogin.setEnabled(true);
                                                        FirebaseAuth.getInstance().signOut();
                                                        sessionManager.logoutUser();
                                                        Toast.makeText(MainActivity.this, "User profile not found in database. Please register first.", Toast.LENGTH_LONG).show();
                                                    }
                                                })
                                                .addOnFailureListener(e -> {
                                                    btnLogin.setEnabled(true);
                                                    FirebaseAuth.getInstance().signOut();
                                                    sessionManager.logoutUser();
                                                    Toast.makeText(MainActivity.this, "User profile not found. Please register first.", Toast.LENGTH_LONG).show();
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

    private void saveSessionAndNavigate(DataSnapshot snapshot) {
        String name = snapshot.child("name").getValue(String.class);
        String role = snapshot.child("role").getValue(String.class);
        String id = snapshot.child("id").getValue(String.class);
        String userEmail = snapshot.child("email").getValue(String.class);
        String section = snapshot.child("section").getValue(String.class);
        String intake = snapshot.child("intake").getValue(String.class);
        String dept = snapshot.child("dept").getValue(String.class);

        if (name == null) name = "User";
        if (role == null) role = "Student";
        if (id == null) id = "";
        if (userEmail == null) userEmail = "";
        if (section == null) section = "--";
        if (intake == null) intake = "--";
        if (dept == null) dept = "Department of CSE, BUBT";

        sessionManager.createLoginSession(name, userEmail, id, role, section, intake, dept);
        Toast.makeText(MainActivity.this, "Welcome back, " + name + "!", Toast.LENGTH_SHORT).show();
        navigateBasedOnRole(role);
        finish();
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
