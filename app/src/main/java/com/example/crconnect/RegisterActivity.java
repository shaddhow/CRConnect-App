package com.example.crconnect;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText etFullName, etRegEmail, etIdCode, etRegPassword, etSection, etIntake, etDept;
    private RadioGroup radioGroupRole;
    private View layoutStudentFields;
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
        etIdCode = findViewById(R.id.etIdCode);
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
            } else {
                layoutStudentFields.setVisibility(View.VISIBLE);
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

        boolean isStudent = rbStudent != null && rbStudent.isChecked();

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
        
        // --- DEVELOPMENT MOCK REGISTRATION ---
        Toast.makeText(this, "Dev Mode: Registration Successful!", Toast.LENGTH_SHORT).show();
        sessionManager.createLoginSession(name, email, id, role, section, intake, dept);

        FirebaseAuth.getInstance().signInAnonymously().addOnCompleteListener(task -> {
            navigateBasedOnRole(role);
            finish();
        });
    }

    private void navigateBasedOnRole(String role) {
        if ("Teacher/Admin".equals(role)) {
            startActivity(new Intent(this, TeacherDashboardActivity.class));
        } else {
            startActivity(new Intent(this, StudentDashboardActivity.class));
        }
    }
}
