package com.example.crconnect;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class TeacherDashboardActivity extends AppCompatActivity {

    private MaterialSwitch switchMasterVoting;
    private TextInputEditText etCandidateName;
    private MaterialButton btnAddCandidate, btnRunOff, btnResetVotes, btnLogout, btnClearName;
    private ImageView imgUserProfile;
    private TextView tvHeaderTitle, tvHeaderSubtitle, tvTotalCandidates;
    private TextView tvStatTotalVotes, tvStatTotalVoters, tvStatTurnout;
    private LinearLayout containerAdminCandidates;
    private DatabaseReference dbRef;
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

        setContentView(R.layout.activity_teacher_dashboard);

        // Safe Area Padding
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        // Pressing back from dashboard returns to login page
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                sessionManager.logoutUser();
                Intent intent = new Intent(TeacherDashboardActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
        });

        dbRef = FirebaseDatabase.getInstance("https://crconnect-58521-default-rtdb.firebaseio.com").getReference("crconnect_db");

        // UI Bindings
        switchMasterVoting = findViewById(R.id.switchMasterVoting);
        etCandidateName = findViewById(R.id.etCandidateName);
        btnAddCandidate = findViewById(R.id.btnAddCandidate);
        btnRunOff = findViewById(R.id.btnRunOff);
        btnResetVotes = findViewById(R.id.btnResetVotes);
        btnClearName = findViewById(R.id.btnClearName);
        containerAdminCandidates = findViewById(R.id.containerAdminCandidates);
        imgUserProfile = findViewById(R.id.imgUserProfile);
        tvHeaderTitle = findViewById(R.id.tvHeaderTitle);
        tvHeaderSubtitle = findViewById(R.id.tvHeaderSubtitle);
        btnLogout = findViewById(R.id.btnLogout);
        tvTotalCandidates = findViewById(R.id.tvTotalCandidates);
        
        tvStatTotalVotes = findViewById(R.id.tvStatTotalVotes);
        tvStatTotalVoters = findViewById(R.id.tvStatTotalVoters);
        tvStatTurnout = findViewById(R.id.tvStatTurnout);

        tvHeaderTitle.setText("Admin Console");
        
        // Dynamic Teacher Profile Loading from Firebase Realtime Database & Firestore
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String uid = currentUser != null ? currentUser.getUid() : "";
        String rawTeacherId = sessionManager.getUserID();
        String sanitizedTeacherId = rawTeacherId != null ? rawTeacherId.replace(".", "_").replace("@", "_").replace("#", "_").replace("$", "_").replace("[", "_").replace("]", "_") : "";

        ValueEventListener teacherListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String name = snapshot.child("name").getValue(String.class);
                    String dept = snapshot.child("dept").getValue(String.class);
                    if (name == null) name = sessionManager.getUserName();
                    if (dept == null) dept = sessionManager.getUserDept();
                    tvHeaderSubtitle.setText(name + " (" + dept + ")");

                    sessionManager.createLoginSession(
                        name,
                        snapshot.child("email").getValue(String.class) != null ? snapshot.child("email").getValue(String.class) : sessionManager.getUserEmail(),
                        snapshot.child("id").getValue(String.class) != null ? snapshot.child("id").getValue(String.class) : sessionManager.getUserID(),
                        "Teacher/Admin",
                        "--",
                        "--",
                        dept
                    );
                } else if (!uid.isEmpty()) {
                    FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(uid)
                        .get()
                        .addOnSuccessListener(doc -> {
                            if (doc.exists()) {
                                String name = doc.getString("name");
                                String dept = doc.getString("dept");
                                if (name == null) name = sessionManager.getUserName();
                                if (dept == null) dept = sessionManager.getUserDept();
                                tvHeaderSubtitle.setText(name + " (" + dept + ")");
                            } else {
                                tvHeaderSubtitle.setText("Teacher: " + sessionManager.getUserName());
                            }
                        });
                } else {
                    tvHeaderSubtitle.setText("Teacher: " + sessionManager.getUserName());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                tvHeaderSubtitle.setText("Teacher: " + sessionManager.getUserName());
            }
        };

        if (!uid.isEmpty()) {
            dbRef.child("users").child(uid).addListenerForSingleValueEvent(teacherListener);
        } else if (!sanitizedTeacherId.isEmpty()) {
            dbRef.child("users").child(sanitizedTeacherId).addListenerForSingleValueEvent(teacherListener);
        } else {
            tvHeaderSubtitle.setText("Teacher: " + sessionManager.getUserName());
        }

        Glide.with(this).load(R.drawable.ic_app_main).circleCrop().into(imgUserProfile);

        btnLogout.setOnClickListener(v -> {
            sessionManager.logoutUser();
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        // Clear Name Button
        btnClearName.setOnClickListener(v -> {
            etCandidateName.setText("");
            etCandidateName.clearFocus();
        });

        dbRef.child("master_voting_enabled").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean isEnabled = snapshot.getValue(Boolean.class);
                if (isEnabled != null) switchMasterVoting.setChecked(isEnabled);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        switchMasterVoting.setOnCheckedChangeListener((btn, isChecked) -> {
            dbRef.child("master_voting_enabled").setValue(isChecked);
            logAdminAction(isChecked ? "Enabled Master Voting" : "Disabled Master Voting");
        });

        btnAddCandidate.setOnClickListener(v -> {
            String name = etCandidateName.getText() != null ? etCandidateName.getText().toString().trim() : "";
            if (name.isEmpty()) { 
                etCandidateName.setError("Required"); 
                etCandidateName.requestFocus();
                return; 
            }

            String cid = dbRef.child("candidates").push().getKey();
            if (cid != null) {
                Map<String, Object> map = new HashMap<>();
                map.put("name", name);
                map.put("votes", 0);
                map.put("dept", "Department of CSE, BUBT");
                map.put("manifesto", "Committed to student welfare, transparent CR communication, and academic support sessions.");

                etCandidateName.setText("");
                etCandidateName.clearFocus();

                dbRef.child("candidates").child(cid).setValue(map)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Successfully added to student portal!", Toast.LENGTH_SHORT).show();
                        logAdminAction("Added New Candidate: " + name);
                    })
                    .addOnFailureListener(e -> {
                        Log.e("TeacherDashboard", "Failed to write candidate", e);
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
            }
        });

        // Live Statistics and Candidate List
        dbRef.child("candidates").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                tvTotalCandidates.setText("Total: " + snapshot.getChildrenCount());
                containerAdminCandidates.removeAllViews();
                
                int totalVotes = 0;
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Integer vts = ds.child("votes").getValue(Integer.class);
                    if (vts != null) totalVotes += vts;
                    addAdminRow(ds.getKey(), ds.child("name").getValue(String.class), vts);
                }
                tvStatTotalVotes.setText(String.valueOf(totalVotes));
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        // Live Voters Statistics Count
        dbRef.child("voters").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long totalVoters = snapshot.getChildrenCount();
                tvStatTotalVoters.setText(String.valueOf(totalVoters));
                
                // Assuming estimated 50 active registered student voters for turnout percentage
                int estimatedTotalStudents = 50; 
                int turnout = (int) Math.min(100, (totalVoters * 100) / estimatedTotalStudents);
                tvStatTurnout.setText(turnout + "%");
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        // Safety Confirmation Dialog for Reset All (Material Design 3)
        btnResetVotes.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(this)
                .setTitle("⚠️ Reset All Election Data?")
                .setMessage("This will permanently delete all published candidates, votes, and voter security logs. This action cannot be undone.")
                .setPositiveButton("Yes, Reset", (d, w) -> {
                    Map<String, Object> resetMap = new HashMap<>();
                    resetMap.put("candidates", null);
                    resetMap.put("voters", null);
                    dbRef.updateChildren(resetMap).addOnSuccessListener(unused -> {
                        showSnackBar("Election data successfully reset.", false);
                        logAdminAction("Reset All Election Votes & Candidates");
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
        });

        // Safety Confirmation Dialog for Run-Off (Material Design 3)
        btnRunOff.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(this)
                .setTitle("⚡ Initialize Run-Off / Tie-Breaker?")
                .setMessage("This will reset all vote counts to 0 while keeping the candidate list for a run-off election round. All voter security locks will be cleared. Proceed?")
                .setPositiveButton("Proceed", (d, w) -> {
                    dbRef.child("candidates").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            Map<String, Object> updates = new HashMap<>();
                            for (DataSnapshot ds : snapshot.getChildren()) {
                                updates.put(ds.getKey() + "/votes", 0);
                            }
                            dbRef.child("candidates").updateChildren(updates);
                            dbRef.child("voters").removeValue();
                            showSnackBar("Run-off election round initialized!", false);
                            logAdminAction("Initialized Run-Off Election Round");
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {}
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
        });
    }

    private void addAdminRow(String candidateId, String name, Integer vts) {
        MaterialCardView card = new MaterialCardView(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 0, 0, 16);
        card.setLayoutParams(lp);
        card.setRadius(24);
        card.setCardElevation(8);
        card.setCardBackgroundColor(Color.parseColor("#171A29"));
        card.setStrokeWidth(2);
        card.setStrokeColor(Color.parseColor("#334155"));

        LinearLayout row = new LinearLayout(this);
        row.setPadding(24, 20, 24, 20);
        row.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout textCol = new LinearLayout(this);
        textCol.setOrientation(LinearLayout.VERTICAL);
        textCol.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));

        TextView tvName = new TextView(this);
        tvName.setText(name);
        tvName.setTextSize(17f);
        tvName.setTextColor(Color.parseColor("#F8FAFC"));
        tvName.setTypeface(null, Typeface.BOLD);

        TextView tvVotes = new TextView(this);
        tvVotes.setText((vts != null ? vts : 0) + " Votes");
        tvVotes.setTextSize(14f);
        tvVotes.setTextColor(Color.parseColor("#38BDF8"));
        tvVotes.setPadding(0, 4, 0, 0);

        textCol.addView(tvName);
        textCol.addView(tvVotes);

        MaterialButton btnDelete = new MaterialButton(this);
        btnDelete.setText("Delete");
        btnDelete.setTextSize(12f);
        btnDelete.setCornerRadius(16);
        btnDelete.setStrokeWidth(2);
        btnDelete.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#F87171")));
        btnDelete.setTextColor(Color.parseColor("#F87171"));
        btnDelete.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
        btnDelete.setLayoutParams(new LinearLayout.LayoutParams(-2, -2));

        btnDelete.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(this)
                .setTitle("Delete Candidate")
                .setMessage("Remove " + name + "?")
                .setPositiveButton("Delete", (d, w) -> {
                    if (candidateId != null) {
                        dbRef.child("candidates").child(candidateId).removeValue()
                            .addOnSuccessListener(aVoid -> showSnackBar("Candidate removed", false));
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
        });

        row.addView(textCol);
        row.addView(btnDelete);
        card.addView(row);
        containerAdminCandidates.addView(card);
    }

    private void showSnackBar(String message, boolean isError) {
        View rootView = findViewById(android.R.id.content);
        Snackbar snackbar = Snackbar.make(rootView, (isError ? "⚠️  " : "✅  ") + message, Snackbar.LENGTH_LONG);
        View sbView = snackbar.getView();
        sbView.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(isError ? "#E6EF4444" : "#E610B981")));
        
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) sbView.getLayoutParams();
        params.setMargins(32, 0, 32, 32);
        sbView.setLayoutParams(params);
        
        TextView tv = sbView.findViewById(com.google.android.material.R.id.snackbar_text);
        tv.setTextColor(Color.parseColor("#F8FAFC"));
        tv.setTypeface(null, Typeface.BOLD);
        snackbar.show();
    }

    private void logAdminAction(String actionDescription) {
        String teacherName = sessionManager.getUserName();
        String teacherEmail = sessionManager.getUserEmail();
        String teacherId = sessionManager.getUserID();

        String logId = dbRef.child("audit_logs").push().getKey();
        if (logId != null) {
            Map<String, Object> logMap = new HashMap<>();
            logMap.put("teacherName", teacherName);
            logMap.put("teacherEmail", teacherEmail);
            logMap.put("teacherId", teacherId);
            logMap.put("action", actionDescription);
            logMap.put("timestamp", ServerValue.TIMESTAMP);

            dbRef.child("audit_logs").child(logId).setValue(logMap);
        }
    }
}
