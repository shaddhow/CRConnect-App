package com.example.crconnect;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etPassword;
    private RadioGroup radioGroupRole;
    private RadioButton rbStudent, rbTeacher;
    private Button btnLogin;
    private TextView tvRegisterLink;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        sessionManager = new SessionManager(this);
        if (sessionManager.isLoggedIn()) {
            navigateBasedOnRole(sessionManager.getUserRole());
            finish();
            return;
        }

        // --- TRUE FULL SCREEN (GOOGLE STYLE) ---
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
        
        // Force the gradient background to the entire window (Removes white bars)
        getWindow().setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.bg_gradient));

        setContentView(R.layout.activity_main);

        // Safe Area Padding (Content won't touch the bars)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, Math.max(systemBars.bottom, ime.bottom));
            return WindowInsetsCompat.CONSUMED;
        });

        // Binding UI elements
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        radioGroupRole = findViewById(R.id.radioGroupRole);
        rbStudent = findViewById(R.id.rbStudent);
        rbTeacher = findViewById(R.id.rbTeacher);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegisterLink = findViewById(R.id.tvRegisterLink);

        // Role change dynamic hint logic
        radioGroupRole.setOnCheckedChangeListener((group, checkedId) -> {
            etEmail.setText("");
            etPassword.setText("");
            if (checkedId == R.id.rbTeacher) {
                etEmail.setHint("Teacher Code or Email");
            } else {
                etEmail.setHint("Student ID or BUBT Email");
            }
        });

        // Login Button Click Listener
        btnLogin.setOnClickListener(v -> performLogin());

        // Register Link Click Listener
        tvRegisterLink.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
            startActivity(intent);
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

        // --- DEVELOPMENT MOCK LOGIN ---
        // ফায়ারবেজ কনসোল সেটআপ করার আগে এই ডামি লগইন দিয়ে আপনি কাজ করতে পারবেন
        Toast.makeText(this, "Dev Mode: Logging in...", Toast.LENGTH_SHORT).show();
        
        String role = rbTeacher.isChecked() ? "Teacher/Admin" : "Student";
        String name = rbTeacher.isChecked() ? "Developer Teacher" : "Developer Student";
        
        // ডামি সেশন তৈরি
        sessionManager.createLoginSession(
            name, 
            emailOrId, 
            "2023-DEV-001", 
            role, 
            "55/8", 
            "55", 
            "CSE"
        );
        
        navigateBasedOnRole(role);
        finish();
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

    private void navigateBasedOnRole(String role) {
        if ("Teacher/Admin".equals(role)) {
            Intent intent = new Intent(MainActivity.this, TeacherDashboardActivity.class);
            startActivity(intent);
        } else {
            Intent intent = new Intent(MainActivity.this, StudentDashboardActivity.class);
            startActivity(intent);
        }
    }
}
