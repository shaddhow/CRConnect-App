package com.example.crconnect;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText etFullName, etRegEmail, etStudentId, etTeacherCode, etRegPassword, etSection, etIntake, etDept;
    private RadioGroup radioGroupRole;
    private View layoutStudentFields, tilStudentId, tilTeacherCode;
    private RadioButton rbTeacher, rbStudent;
    private Button btnRegister;
    private TextView tvLoginLink;
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

        etFullName = findViewById(R.id.etFullName);
        etRegEmail = findViewById(R.id.etRegEmail);
        etStudentId = findViewById(R.id.etStudentId);
        etTeacherCode = findViewById(R.id.etTeacherCode);
        tilStudentId = findViewById(R.id.tilStudentId);
        tilTeacherCode = findViewById(R.id.tilTeacherCode);
        etRegPassword = findViewById(R.id.etRegPassword);
        etSection = findViewById(R.id.etSection);
        etIntake = findViewById(R.id.etIntake);
        etDept = findViewById(R.id.etDept);
        layoutStudentFields = findViewById(R.id.layoutStudentFields);
        radioGroupRole = findViewById(R.id.radioGroupRole);
        rbStudent = findViewById(R.id.rbStudent);
        rbTeacher = findViewById(R.id.rbTeacher);
        btnRegister = findViewById(R.id.btnRegister);
        tvLoginLink = findViewById(R.id.tvLoginLink);

        radioGroupRole.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbTeacher) {
                layoutStudentFields.setVisibility(View.GONE);
                tilStudentId.setVisibility(View.GONE);
                tilTeacherCode.setVisibility(View.VISIBLE);
                etStudentId.setText("");
            } else {
                layoutStudentFields.setVisibility(View.VISIBLE);
                tilStudentId.setVisibility(View.VISIBLE);
                tilTeacherCode.setVisibility(View.GONE);
                etTeacherCode.setText("");
            }
        });

        btnRegister.setOnClickListener(v -> performRegistration());
        tvLoginLink.setOnClickListener(v -> finish());

        // Auto-scroll up smoothly when password field gets focus in register screen
        ScrollView scrollViewRegister = findViewById(R.id.scrollViewRegister);
        etRegPassword.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && scrollViewRegister != null) {
                scrollViewRegister.postDelayed(() -> scrollViewRegister.smoothScrollTo(0, btnRegister.getBottom()), 200);
            }
        });
    }

    private void performRegistration() {
        String name = etFullName.getText() != null ? etFullName.getText().toString().trim() : "";
        String email = etRegEmail.getText() != null ? etRegEmail.getText().toString().trim() : "";
        String id = etIdCode.getText() != null ? etIdCode.getText().toString().trim() : "";
        String password = etRegPassword.getText() != null ? etRegPassword.getText().toString().trim() : "";
        String section = etSection.getText() != null ? etSection.getText().toString().trim() : "";
        String intake = etIntake.getText() != null ? etIntake.getText().toString().trim() : "";
        String dept = etDept.getText() != null ? etDept.getText().toString().trim() : "";

        if (name.isEmpty()) {
            etFullName.setError("Full Name is required!");
            etFullName.requestFocus();
            return;
        }

        if (email.isEmpty()) {
            etRegEmail.setError("Email is required!");
            etRegEmail.requestFocus();
            return;
        }

        if (!email.toLowerCase().endsWith("@bubt.edu.bd") && !email.toLowerCase().endsWith("@cse.bubt.edu.bd")) {
            etRegEmail.setError("Only official BUBT emails (@bubt.edu.bd or @cse.bubt.edu.bd) are allowed!");
            etRegEmail.requestFocus();
            return;
        }

        boolean isStudent = rbStudent != null && rbStudent.isChecked();

        if (id.isEmpty()) {
            etIdCode.setError(isStudent ? "Student ID is required!" : "Teacher Code/ID is required!");
            etIdCode.requestFocus();
            return;
        }

        if (isStudent) {
            if (section.isEmpty()) { etSection.setError("Required"); etSection.requestFocus(); return; }
            if (intake.isEmpty()) { etIntake.setError("Required"); etIntake.requestFocus(); return; }
            if (dept.isEmpty()) { etDept.setError("Required"); etDept.requestFocus(); return; }
        }

        if (password.length() < 6) {
            etRegPassword.setError("Password must be at least 6 characters!");
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
                String uid = authResult.getUser() != null ? authResult.getUser().getUid() : id;
                String sanitizedId = id.replace(".", "_").replace("@", "_").replace("#", "_").replace("$", "_").replace("[", "_").replace("]", "_");

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

                dbRef.child("users").child(uid).setValue(userMap);
                dbRef.child("users").child(sanitizedId).setValue(userMap);

                sessionManager.createLoginSession(name, email, id, role, isStudent ? section : "--", isStudent ? intake : "--", isStudent ? dept : "CSE");

                showSnackBar("✅ Registration Successful!", false);
                navigateBasedOnRole(role);
                finish();
            })
            .addOnFailureListener(e -> {
                btnRegister.setEnabled(true);
                btnRegister.setText("REGISTER");
                showSnackBar("⚠️ Registration Failed: " + e.getMessage(), true);
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

    private void navigateBasedOnRole(String role) {
        if ("Teacher/Admin".equals(role)) {
            startActivity(new Intent(this, TeacherDashboardActivity.class));
        } else {
            startActivity(new Intent(this, StudentDashboardActivity.class));
        }
    }
}
