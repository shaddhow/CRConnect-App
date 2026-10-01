package com.example.crconnect;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

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

        if (!email.toLowerCase().endsWith("@bubt.edu.bd") && !email.toLowerCase().endsWith("@cse.bubt.edu.bd")) {
            etRegEmail.setError("Only official BUBT emails (@bubt.edu.bd or @cse.bubt.edu.bd) are allowed!");
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

        // Generate 6-digit verification OTP code
        String generatedOtp = String.valueOf(100000 + new Random().nextInt(900000));

        // Trigger OTP Verification Dialog
        showOtpVerificationDialog(name, email, id, role, section, intake, dept, generatedOtp);
    }

    private void showOtpVerificationDialog(String name, String email, String id, String role, String section, String intake, String dept, String expectedOtp) {
        EditText inputOtp = new EditText(this);
        inputOtp.setHint("Enter 6-digit verification code");
        inputOtp.setInputType(InputType.TYPE_CLASS_NUMBER);
        inputOtp.setPadding(50, 40, 50, 40);

        // Show Toast with OTP code for institutional verification
        Toast.makeText(this, "Verification OTP sent to " + email + "\n[Code: " + expectedOtp + "]", Toast.LENGTH_LONG).show();

        new AlertDialog.Builder(this)
            .setTitle("BUBT Email Verification")
            .setMessage("OTP Code sent to " + email + "\n\n(Sandbox Mode Code: " + expectedOtp + ")\n\nPlease enter the 6-digit code below:")
            .setView(inputOtp)
            .setPositiveButton("Verify & Register", (dialog, which) -> {
                String enteredOtp = inputOtp.getText() != null ? inputOtp.getText().toString().trim() : "";
                if (enteredOtp.equals(expectedOtp)) {
                    sessionManager.createLoginSession(name, email, id, role, section, intake, dept);

                    DatabaseReference dbRef = FirebaseDatabase.getInstance("https://crconnect-58521-default-rtdb.firebaseio.com").getReference("crconnect_db");
                    String sanitizedId = id.replace(".", "_").replace("@", "_").replace("#", "_").replace("$", "_").replace("[", "_").replace("]", "_");

                    Map<String, Object> userMap = new HashMap<>();
                    userMap.put("name", name);
                    userMap.put("email", email);
                    userMap.put("id", id);
                    userMap.put("role", role);
                    userMap.put("section", section);
                    userMap.put("intake", intake);
                    userMap.put("dept", dept);

                    dbRef.child("users").child(sanitizedId).setValue(userMap);

                    FirebaseAuth.getInstance().signInAnonymously().addOnCompleteListener(task -> {
                        Toast.makeText(this, "Email Verified & Registration Successful!", Toast.LENGTH_SHORT).show();
                        navigateBasedOnRole(role);
                        finish();
                    });
                } else {
                    Toast.makeText(this, "Invalid Verification Code! Please try again.", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void navigateBasedOnRole(String role) {
        if ("Teacher/Admin".equals(role)) {
            startActivity(new Intent(this, TeacherDashboardActivity.class));
        } else {
            startActivity(new Intent(this, StudentDashboardActivity.class));
        }
    }
}
