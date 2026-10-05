package com.example.crconnect;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText etFullName, etRegEmail, etStudentId, etTeacherCode, etRegPassword, etSection, etIntake, etDept;
    private RadioGroup radioGroupRole;
    private View layoutStudentFields, layoutTeacherFields;
    private RadioButton rbTeacher, rbStudent;
    private Button btnRegister, btnBackToLogin;
    private TextView tvLoginLink;
    private MaterialCardView layoutFormCard, layoutSuccessCard;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        sessionManager = new SessionManager(this);

        // --- TRUE FULL SCREEN (GOOGLE STYLE) ---
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
        
        // Force the gradient background to the entire window (Removes white bars)
        getWindow().setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.bg_gradient));

        setContentView(R.layout.activity_register);

        // Safe Area Padding
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, Math.max(systemBars.bottom, ime.bottom));
            return WindowInsetsCompat.CONSUMED;
        });

        // Smooth transition on back press
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
        });

        etFullName = findViewById(R.id.etFullName);
        etRegEmail = findViewById(R.id.etRegEmail);
        etStudentId = findViewById(R.id.etStudentId);
        etTeacherCode = findViewById(R.id.etTeacherCode);
        etRegPassword = findViewById(R.id.etRegPassword);
        etSection = findViewById(R.id.etSection);
        etIntake = findViewById(R.id.etIntake);
        etDept = findViewById(R.id.etDept);
        layoutStudentFields = findViewById(R.id.layoutStudentFields);
        layoutTeacherFields = findViewById(R.id.layoutTeacherFields);
        radioGroupRole = findViewById(R.id.radioGroupRole);
        rbStudent = findViewById(R.id.rbStudent);
        rbTeacher = findViewById(R.id.rbTeacher);
        btnRegister = findViewById(R.id.btnRegister);
        tvLoginLink = findViewById(R.id.tvLoginLink);
        layoutFormCard = findViewById(R.id.layoutFormCard);
        layoutSuccessCard = findViewById(R.id.layoutSuccessCard);
        btnBackToLogin = findViewById(R.id.btnBackToLogin);

        btnBackToLogin.setOnClickListener(v -> {
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        radioGroupRole.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbTeacher) {
                layoutStudentFields.setVisibility(View.GONE);
                layoutTeacherFields.setVisibility(View.VISIBLE);
            } else {
                layoutStudentFields.setVisibility(View.VISIBLE);
                layoutTeacherFields.setVisibility(View.GONE);
            }
        });

        btnRegister.setOnClickListener(v -> performRegistration());
        tvLoginLink.setOnClickListener(v -> {
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        // Auto-scroll up smoothly when password field gets focus in register screen
        ScrollView scrollViewRegister = findViewById(R.id.scrollViewRegister);
        etRegPassword.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && scrollViewRegister != null) {
                scrollViewRegister.postDelayed(() -> scrollViewRegister.smoothScrollTo(0, btnRegister.getBottom()), 200);
            }
        });
    }

    private boolean isDummyOrGibberish(String text) {
        if (text == null || text.trim().isEmpty()) return true;
        String lower = text.trim().toLowerCase();
        String[] dummyKeywords = {
            "test", "demo", "dummy", "admin", "qwerty", "asdf", "habijabi", "habi-jabi", 
            "xyz", "xxxx", "aaaa", "zzzz", "12345", "00000", "null", "undefined", "sample", "temp"
        };
        for (String kw : dummyKeywords) {
            if (lower.contains(kw)) return true;
        }
        return false;
    }

    private void performRegistration() {
        String name = etFullName.getText() != null ? etFullName.getText().toString().trim() : "";
        String email = etRegEmail.getText() != null ? etRegEmail.getText().toString().trim() : "";
        boolean isStudent = rbStudent != null && rbStudent.isChecked();
        
        String id = isStudent ? 
            (etStudentId != null && etStudentId.getText() != null ? etStudentId.getText().toString().trim() : "") :
            (etTeacherCode != null && etTeacherCode.getText() != null ? etTeacherCode.getText().toString().trim() : "");
            
        String password = etRegPassword.getText() != null ? etRegPassword.getText().toString().trim() : "";
        String section = (isStudent && etSection != null && etSection.getText() != null) ? etSection.getText().toString().trim() : "";
        String intake = (isStudent && etIntake != null && etIntake.getText() != null) ? etIntake.getText().toString().trim() : "";
        String dept = (isStudent && etDept != null && etDept.getText() != null) ? etDept.getText().toString().trim() : "";

        // 1. FULL NAME VALIDATION
        if (name.isEmpty()) {
            Toast.makeText(this, "Full Name is required!", Toast.LENGTH_SHORT).show();
            etFullName.setError("Full Name is required!");
            etFullName.requestFocus();
            return;
        }

        if (name.length() < 3 || name.length() > 50 || isDummyOrGibberish(name) || !name.matches("^[a-zA-Z.\\s-]+$")) {
            Toast.makeText(this, "Invalid Full Name: Please enter a legitimate name (e.g., John Doe)", Toast.LENGTH_LONG).show();
            etFullName.setError("Enter a legitimate full name (e.g., John Doe)");
            etFullName.requestFocus();
            return;
        }

        // 2. OFFICIAL BUBT EMAIL ENFORCEMENT
        if (email.isEmpty()) {
            Toast.makeText(this, "BUBT Email is required!", Toast.LENGTH_SHORT).show();
            etRegEmail.setError("BUBT Email is required!");
            etRegEmail.requestFocus();
            return;
        }

        String lowerEmail = email.toLowerCase();
        boolean isValidDomain = lowerEmail.endsWith("@bubt.edu.bd") || lowerEmail.endsWith("@cse.bubt.edu.bd");
        boolean isValidEmailStructure = Patterns.EMAIL_ADDRESS.matcher(email).matches();
        String emailUsername = email.contains("@") ? email.split("@")[0] : email;

        if (!isValidEmailStructure || !isValidDomain || isDummyOrGibberish(emailUsername)) {
            Toast.makeText(this, "Invalid Email: Only official BUBT emails (@bubt.edu.bd or @cse.bubt.edu.bd) are allowed!", Toast.LENGTH_LONG).show();
            etRegEmail.setError("Must be an official BUBT email (@bubt.edu.bd or @cse.bubt.edu.bd)");
            etRegEmail.requestFocus();
            return;
        }

        // 3. ID / TEACHER CODE VALIDATION
        if (id.isEmpty()) {
            if (isStudent) {
                Toast.makeText(this, "Student ID is required!", Toast.LENGTH_SHORT).show();
                etStudentId.setError("Student ID is required!");
                etStudentId.requestFocus();
            } else {
                Toast.makeText(this, "Teacher Code/ID is required!", Toast.LENGTH_SHORT).show();
                etTeacherCode.setError("Teacher Code/ID is required!");
                etTeacherCode.requestFocus();
            }
            return;
        }

        if (isStudent) {
            // Student ID format validation (e.g., numeric BUBT Student ID like 20211103001)
            if (isDummyOrGibberish(id) || !id.matches("^[0-9-]{6,15}$")) {
                Toast.makeText(this, "Invalid Student ID: Please enter a legitimate BUBT Student ID (e.g., 20211103001)", Toast.LENGTH_LONG).show();
                etStudentId.setError("Enter a valid numeric BUBT Student ID");
                etStudentId.requestFocus();
                return;
            }
        } else {
            // Teacher Code format validation (e.g., CSE-101, T-501)
            if (isDummyOrGibberish(id) || id.length() < 3 || !id.matches("^[a-zA-Z0-9.-]{3,15}$")) {
                Toast.makeText(this, "Invalid Teacher Code: Please enter a valid Teacher Code (e.g., CSE-101 or T-501)", Toast.LENGTH_LONG).show();
                etTeacherCode.setError("Enter a valid Teacher Code (e.g., CSE-101)");
                etTeacherCode.requestFocus();
                return;
            }
        }

        // 4. STUDENT SPECIFIC FIELDS VALIDATION
        if (isStudent) {
            // Intake Validation (e.g., 55, 55/8)
            if (intake.isEmpty()) {
                Toast.makeText(this, "Intake is required!", Toast.LENGTH_SHORT).show();
                etIntake.setError("Intake is required!");
                etIntake.requestFocus();
                return;
            }
            if (isDummyOrGibberish(intake) || !intake.matches("^(Intake\\s*)?[0-9]{1,3}(/[0-9]{1,2})?$")) {
                Toast.makeText(this, "Invalid Intake: Please enter a valid intake number (e.g., 55 or 55/8)", Toast.LENGTH_LONG).show();
                etIntake.setError("Enter a valid intake format (e.g., 55 or 55/8)");
                etIntake.requestFocus();
                return;
            }

            // Section Validation (e.g., 8, Sec-A, 55/8)
            if (section.isEmpty()) {
                Toast.makeText(this, "Section is required!", Toast.LENGTH_SHORT).show();
                etSection.setError("Section is required!");
                etSection.requestFocus();
                return;
            }
            if (isDummyOrGibberish(section) || !section.matches("^[a-zA-Z0-9/\\s-]{1,10}$")) {
                Toast.makeText(this, "Invalid Section: Please enter a valid section (e.g., 8, Sec-A, or 55/8)", Toast.LENGTH_LONG).show();
                etSection.setError("Enter a valid section format (e.g., 8 or 55/8)");
                etSection.requestFocus();
                return;
            }

            // Department Validation (e.g., CSE, Department of CSE)
            if (dept.isEmpty()) {
                Toast.makeText(this, "Department is required!", Toast.LENGTH_SHORT).show();
                etDept.setError("Department is required!");
                etDept.requestFocus();
                return;
            }
            if (isDummyOrGibberish(dept) || dept.length() < 2 || !dept.matches("^[a-zA-Z.\\s,-]+$")) {
                Toast.makeText(this, "Invalid Department: Please enter a valid department (e.g., CSE or Department of CSE)", Toast.LENGTH_LONG).show();
                etDept.setError("Enter a valid department name (e.g., CSE)");
                etDept.requestFocus();
                return;
            }
        }

        // 5. STRONG PASSWORD REQUIREMENTS
        if (password.isEmpty()) {
            Toast.makeText(this, "Password is required!", Toast.LENGTH_SHORT).show();
            etRegPassword.setError("Password is required!");
            etRegPassword.requestFocus();
            return;
        }

        boolean hasLetter = password.matches(".*[a-zA-Z].*");
        boolean hasDigit = password.matches(".*[0-9].*");
        if (password.length() < 8 || !hasLetter || !hasDigit || isDummyOrGibberish(password)) {
            Toast.makeText(this, "Password Too Weak: Must be at least 8 characters long and contain both letters and numbers.", Toast.LENGTH_LONG).show();
            etRegPassword.setError("Minimum 8 characters with letters and numbers");
            etRegPassword.requestFocus();
            return;
        }

        String role = isStudent ? "Student" : "Teacher/Admin";

        // Show loading state on button
        btnRegister.setEnabled(false);
        btnRegister.setText("Creating Account...");

        FirebaseAuth auth = FirebaseAuth.getInstance();
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener(authResult -> {
                FirebaseUser user = authResult.getUser();
                if (user == null) {
                    btnRegister.setEnabled(true);
                    btnRegister.setText("REGISTER");
                    showSnackBar("⚠️ Registration Failed: Account creation error", true);
                    return;
                }
                user.sendEmailVerification()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.d("RegisterActivity", "Verification email sent to " + email);
                        } else {
                            Log.e("RegisterActivity", "Failed to send verification email", task.getException());
                        }
                    });
                saveUserDataAndFinish(user.getUid(), name, email, id, role, isStudent, section, intake, dept);
            })
            .addOnFailureListener(e -> {
                Log.e("RegisterActivity", "FirebaseAuth error: " + e.getMessage(), e);
                btnRegister.setEnabled(true);
                btnRegister.setText("REGISTER");
                showSnackBar("⚠️ Registration Failed: " + e.getMessage(), true);
            });
    }

    private void saveUserDataAndFinish(String uid, String name, String email, String id, String role, boolean isStudent, String section, String intake, String dept) {
        String sanitizedId = id.replace(".", "_").replace("@", "_").replace("#", "_").replace("$", "_").replace("[", "_").replace("]", "_");
        String sanitizedEmail = email.replace(".", "_").replace("@", "_").replace("#", "_").replace("$", "_").replace("[", "_").replace("]", "_");

        DatabaseReference dbRef = FirebaseDatabase.getInstance("https://crconnect-58521-default-rtdb.firebaseio.com").getReference("crconnect_db");

        Map<String, Object> userMap = new HashMap<>();
        userMap.put("name", name);
        userMap.put("email", email);
        userMap.put("id", id);
        userMap.put("role", role);
        userMap.put("section", isStudent ? section : "--");
        userMap.put("intake", isStudent ? intake : "--");
        userMap.put("dept", isStudent ? dept : "Department of CSE, BUBT");
        userMap.put("deviceId", Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID));

        // 1. Save to Firebase Realtime Database
        if (uid != null && !uid.isEmpty()) {
            dbRef.child("users").child(uid).setValue(userMap);
        }
        dbRef.child("users").child(sanitizedId).setValue(userMap);
        dbRef.child("users").child(sanitizedEmail).setValue(userMap);

        // 2. Save to Firebase Firestore
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        if (uid != null && !uid.isEmpty()) {
            firestore.collection("users").document(uid).set(userMap);
        }
        firestore.collection("users").document(sanitizedId).set(userMap);

        FirebaseAuth.getInstance().signOut();
        sessionManager.logoutUser();

        runOnUiThread(() -> {
            if (layoutFormCard != null) layoutFormCard.setVisibility(View.GONE);
            if (layoutSuccessCard != null) layoutSuccessCard.setVisibility(View.VISIBLE);
        });
    }

    private void showSnackBar(String message, boolean isError) {
        View rootView = findViewById(android.R.id.content);
        Snackbar snackbar = Snackbar.make(rootView, message, Snackbar.LENGTH_LONG);
        View sbView = snackbar.getView();
        sbView.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(isError ? "#EF4444" : "#10B981")));
        
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) sbView.getLayoutParams();
        params.setMargins(32, 0, 32, 32);
        sbView.setLayoutParams(params);
        
        TextView tv = sbView.findViewById(com.google.android.material.R.id.snackbar_text);
        tv.setTextColor(Color.parseColor("#F8FAFC"));
        tv.setTypeface(null, Typeface.BOLD);
        snackbar.show();
    }
}
