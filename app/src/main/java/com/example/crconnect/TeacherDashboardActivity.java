package com.example.crconnect;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ImageDecoder;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.core.widget.NestedScrollView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;
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

public class TeacherDashboardActivity extends AppCompatActivity {

    private MaterialSwitch switchMasterVoting;
    private TextInputEditText etCandidateName;
    private MaterialButton btnAddCandidate, btnRunOff, btnResetVotes, btnLogout, btnClearName, btnExportPdf;
    private ImageView imgUserProfile;
    private ActivityResultLauncher<Intent> profileImagePickerLauncher;
    private TextView tvHeaderTitle, tvHeaderSubtitle, tvTotalCandidates;
    private TextView tvStatTotalVotes, tvStatTotalVoters, tvStatTurnout;
    private LinearLayout containerAdminCandidates;

    // Live Election Control Views & State
    private TextView tvAdminTimerDisplay, tvAdminTimerStatus;
    private MaterialButton btnExtend5Min, btnExtend10Min, btnReduce5Min, btnPauseResume, btnEndVotingNow;

    private DatabaseReference votingControlRef;
    private ValueEventListener votingControlListener;
    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable;

    private long currentExpiryTime = 0;
    private boolean currentIsPaused = false;
    private boolean currentIsGateOpen = false;
    private long currentRemainingMs = 0;

    private DatabaseReference dbRef;
    private SessionManager sessionManager;
    private final Set<String> sectionCandidateIds = new HashSet<>();

    private String getTeacherAssignedSection() {
        if (sessionManager != null) {
            String sec = sessionManager.getAssignedSection();
            if (sec != null && !sec.isEmpty()) {
                return sec;
            }
        }
        return "--";
    }

    private String getTeacherDepartment() {
        if (sessionManager != null) {
            String dept = sessionManager.getUserDept();
            if (dept != null && !dept.isEmpty()) {
                return dept;
            }
        }
        return "Department of CSE, BUBT";
    }

    private String getSanitizedSection(String sec) {
        if (sec == null || sec.trim().isEmpty() || sec.equals("--")) return "--";
        return sec.trim().replace("/", "_").replace(" ", "_").replace(".", "_").replace("#", "_").replace("$", "_").replace("[", "_").replace("]", "_");
    }

    private String getSanitizedDepartment(String dept) {
        if (dept == null || dept.trim().isEmpty() || dept.equals("--")) return "Department_of_CSE_BUBT";
        return dept.trim().replace("/", "_").replace(" ", "_").replace(".", "_").replace("#", "_").replace("$", "_").replace("[", "_").replace("]", "_").replace("&", "_");
    }

    private void createDefaultTeacherProfileAndProceed() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String uid = currentUser != null ? currentUser.getUid() : "";
        String userEmail = currentUser != null && currentUser.getEmail() != null ? currentUser.getEmail() : (sessionManager != null && sessionManager.getUserEmail() != null ? sessionManager.getUserEmail() : "teacher@bubt.edu.bd");
        String name = sessionManager != null && sessionManager.getUserName() != null ? sessionManager.getUserName() : "Test Teacher (Dev)";
        String id = sessionManager != null && sessionManager.getUserID() != null ? sessionManager.getUserID() : "T1001";
        String assignedSection = sessionManager != null && sessionManager.getAssignedSection() != null ? sessionManager.getAssignedSection() : "5A";
        String dept = sessionManager != null && sessionManager.getUserDept() != null ? sessionManager.getUserDept() : "Department of CSE, BUBT";

        Map<String, Object> userMap = new HashMap<>();
        userMap.put("name", name);
        userMap.put("email", userEmail);
        userMap.put("id", id);
        userMap.put("role", "Teacher/Admin");
        userMap.put("section", "--");
        userMap.put("assignedSection", assignedSection);
        userMap.put("intake", "--");
        userMap.put("dept", dept);
        userMap.put("registeredAt", System.currentTimeMillis());

        DatabaseReference dbRef = FirebaseDatabase.getInstance("https://crconnect-58521-default-rtdb.firebaseio.com").getReference("crconnect_db");
        String sanitizedEmail = userEmail.replace(".", "_").replace("@", "_").replace("#", "_").replace("$", "_").replace("[", "_").replace("]", "_");
        String sanitizedId = id.replace(".", "_").replace("@", "_").replace("#", "_").replace("$", "_").replace("[", "_").replace("]", "_");

        if (!uid.isEmpty()) {
            dbRef.child("users").child(uid).setValue(userMap);
        }
        dbRef.child("users").child(sanitizedEmail).setValue(userMap);
        dbRef.child("users").child(sanitizedId).setValue(userMap);

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        if (!uid.isEmpty()) {
            firestore.collection("users").document(uid).set(userMap);
        }
        firestore.collection("users").document(sanitizedEmail).set(userMap);
        firestore.collection("users").document(sanitizedId).set(userMap);

        if (tvHeaderSubtitle != null) {
            tvHeaderSubtitle.setText(name + " (" + dept + " | Sec: " + assignedSection + ")");
        }
        if (tvHeaderTitle != null) {
            tvHeaderTitle.setText("Admin Console - Sec " + assignedSection);
        }
        
        if (sessionManager != null) {
            sessionManager.createLoginSession(name, userEmail, id, "Teacher/Admin", "--", assignedSection, "--", dept);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        sessionManager = new SessionManager(this);
        if (!checkUserAuthenticationAndRole()) {
            return;
        }
        
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
        btnExportPdf = findViewById(R.id.btnExportPdf);

        btnExportPdf.setOnClickListener(v -> exportElectionResultsToPdf());

        // Live Election Control UI Bindings
        tvAdminTimerDisplay = findViewById(R.id.tvAdminTimerDisplay);
        tvAdminTimerStatus = findViewById(R.id.tvAdminTimerStatus);
        btnExtend5Min = findViewById(R.id.btnExtend5Min);
        btnExtend10Min = findViewById(R.id.btnExtend10Min);
        btnReduce5Min = findViewById(R.id.btnReduce5Min);
        btnPauseResume = findViewById(R.id.btnPauseResume);
        btnEndVotingNow = findViewById(R.id.btnEndVotingNow);

        if (btnExtend5Min != null) btnExtend5Min.setOnClickListener(v -> extendElectionTime(5 * 60 * 1000L));
        if (btnExtend10Min != null) btnExtend10Min.setOnClickListener(v -> extendElectionTime(10 * 60 * 1000L));
        if (btnReduce5Min != null) btnReduce5Min.setOnClickListener(v -> reduceElectionTime(5 * 60 * 1000L));
        if (btnPauseResume != null) btnPauseResume.setOnClickListener(v -> togglePauseResume());
        if (btnEndVotingNow != null) btnEndVotingNow.setOnClickListener(v -> endVotingNow());

        setupVotingControlListener();

        String initialAssignedSec = getTeacherAssignedSection();
        tvHeaderTitle.setText(initialAssignedSec.equals("--") ? "Admin Console" : "Admin Console - Sec " + initialAssignedSec);
        tvHeaderSubtitle.setText(sessionManager.getUserName() + " (" + sessionManager.getUserDept() + " | Sec: " + initialAssignedSec + ")");

        NestedScrollView scrollViewAdmin = findViewById(R.id.scrollViewAdmin);
        etCandidateName.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && scrollViewAdmin != null) {
                scrollViewAdmin.postDelayed(() -> scrollViewAdmin.smoothScrollTo(0, btnAddCandidate.getBottom()), 200);
            }
        });
        
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
                    String profileImg = snapshot.child("profileImageUrl").getValue(String.class);
                    String assignedSection = snapshot.child("assignedSection").getValue(String.class);
                    if (name == null) name = sessionManager.getUserName();
                    if (dept == null) dept = sessionManager.getUserDept();
                    if (assignedSection == null || assignedSection.isEmpty() || assignedSection.equals("--")) {
                        assignedSection = sessionManager.getAssignedSection();
                    } else {
                        sessionManager.setAssignedSection(assignedSection);
                    }

                    tvHeaderSubtitle.setText(name + " (" + dept + " | Sec: " + assignedSection + ")");
                    tvHeaderTitle.setText("Admin Console - Sec " + assignedSection);

                    if (profileImg != null && !profileImg.isEmpty()) {
                        sessionManager.setProfileImageUrl(profileImg);
                        loadProfileImage(TeacherDashboardActivity.this, profileImg, imgUserProfile);
                    }

                    sessionManager.createLoginSession(
                        name,
                        snapshot.child("email").getValue(String.class) != null ? snapshot.child("email").getValue(String.class) : sessionManager.getUserEmail(),
                        snapshot.child("id").getValue(String.class) != null ? snapshot.child("id").getValue(String.class) : sessionManager.getUserID(),
                        "Teacher/Admin",
                        "--",
                        assignedSection,
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
                                String profileImg = doc.getString("profileImageUrl");
                                String assignedSection = doc.getString("assignedSection");
                                if (name == null) name = sessionManager.getUserName();
                                if (dept == null) dept = sessionManager.getUserDept();
                                if (assignedSection == null || assignedSection.isEmpty() || assignedSection.equals("--")) {
                                    assignedSection = sessionManager.getAssignedSection();
                                } else {
                                    sessionManager.setAssignedSection(assignedSection);
                                }

                                tvHeaderSubtitle.setText(name + " (" + dept + " | Sec: " + assignedSection + ")");
                                tvHeaderTitle.setText("Admin Console - Sec " + assignedSection);

                                if (profileImg != null && !profileImg.isEmpty()) {
                                    sessionManager.setProfileImageUrl(profileImg);
                                    Glide.with(TeacherDashboardActivity.this).load(profileImg).placeholder(R.drawable.ic_app_main).error(R.drawable.ic_app_main).circleCrop().into(imgUserProfile);
                                }

                                sessionManager.createLoginSession(
                                    name,
                                    doc.getString("email") != null ? doc.getString("email") : sessionManager.getUserEmail(),
                                    doc.getString("id") != null ? doc.getString("id") : sessionManager.getUserID(),
                                    "Teacher/Admin",
                                    "--",
                                    assignedSection,
                                    "--",
                                    dept
                                );
                            } else {
                                createDefaultTeacherProfileAndProceed();
                            }
                        })
                        .addOnFailureListener(e -> {
                            createDefaultTeacherProfileAndProceed();
                        });
                } else {
                    createDefaultTeacherProfileAndProceed();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                createDefaultTeacherProfileAndProceed();
            }
        };

        if (!uid.isEmpty()) {
            dbRef.child("users").child(uid).addListenerForSingleValueEvent(teacherListener);
        } else if (!sanitizedTeacherId.isEmpty()) {
            dbRef.child("users").child(sanitizedTeacherId).addListenerForSingleValueEvent(teacherListener);
        } else {
            createDefaultTeacherProfileAndProceed();
        }

        String cachedProfileImg = sessionManager.getProfileImageUrl();
        if (cachedProfileImg != null && !cachedProfileImg.isEmpty()) {
            Glide.with(this).load(cachedProfileImg).placeholder(R.drawable.ic_app_main).error(R.drawable.ic_app_main).circleCrop().into(imgUserProfile);
        } else {
            Glide.with(this).load(R.drawable.ic_app_main).circleCrop().into(imgUserProfile);
        }

        profileImagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        uploadProfilePictureToFirebase(imageUri);
                    }
                }
            }
        );

        imgUserProfile.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            profileImagePickerLauncher.launch(intent);
        });

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

        String teacherDept = getTeacherDepartment();
        String sanitizedDept = getSanitizedDepartment(teacherDept);
        String teacherSec = getTeacherAssignedSection();
        String sanitizedSec = getSanitizedSection(teacherSec);

        DatabaseReference gateRef = !sanitizedSec.equals("--") ?
                dbRef.child("voting_controls").child(sanitizedDept).child(sanitizedSec).child("isGateOpen") :
                dbRef.child("master_voting_enabled");

        gateRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean isEnabled = snapshot.getValue(Boolean.class);
                if (isEnabled != null) {
                    switchMasterVoting.setChecked(isEnabled);
                } else {
                    dbRef.child("master_voting_enabled").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snap) {
                            Boolean globalEnabled = snap.getValue(Boolean.class);
                            if (globalEnabled != null) switchMasterVoting.setChecked(globalEnabled);
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {}
                    });
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        switchMasterVoting.setOnCheckedChangeListener((btn, isChecked) -> {
            String currentDept = getTeacherDepartment();
            String currentSanitizedDept = getSanitizedDepartment(currentDept);
            String currentSec = getTeacherAssignedSection();
            String currentSanitizedSec = getSanitizedSection(currentSec);

            if (!currentSanitizedSec.equals("--")) {
                DatabaseReference ctrlRef = dbRef.child("voting_controls").child(currentSanitizedDept).child(currentSanitizedSec);
                Map<String, Object> updates = new HashMap<>();
                updates.put("isGateOpen", isChecked);
                updates.put("isOpen", isChecked);
                if (isChecked) {
                    updates.put("isPaused", false);
                    updates.put("remainingTimeMs", 0L);
                    if (currentExpiryTime <= System.currentTimeMillis()) {
                        long defaultExpiry = System.currentTimeMillis() + (15 * 60 * 1000L);
                        updates.put("expiryTime", defaultExpiry);
                    }
                } else {
                    updates.put("isPaused", false);
                    updates.put("expiryTime", System.currentTimeMillis());
                    updates.put("remainingTimeMs", 0L);
                }
                ctrlRef.updateChildren(updates);
                dbRef.child("sections").child(currentSanitizedDept).child(currentSanitizedSec).child("master_voting_enabled").setValue(isChecked);
            }
            dbRef.child("master_voting_enabled").setValue(isChecked);

            logAdminAction(isChecked ? "Enabled Master Voting for Section " + currentSec : "Disabled Master Voting for Section " + currentSec);

            String notifTitle = isChecked ? "🟢 Live Voting is OPEN (Sec " + currentSec + ")" : "🔴 Live Voting is LOCKED (Sec " + currentSec + ")";
            String notifBody = isChecked ?
                    "Election admin " + sessionManager.getUserName() + " has opened live voting for Section " + currentSec + "! Cast your vote now." :
                    "Live voting for Section " + currentSec + " has been locked by election admin " + sessionManager.getUserName() + ".";
            broadcastNotificationToStudents(notifTitle, notifBody, "GATE_CONTROL");
        });

        btnAddCandidate.setOnClickListener(v -> {
            try {
                String name = etCandidateName.getText() != null ? etCandidateName.getText().toString().trim() : "";
                if (name.isEmpty()) { 
                    etCandidateName.setError("Required"); 
                    etCandidateName.requestFocus();
                    return; 
                }

                btnAddCandidate.setEnabled(false);
                Toast.makeText(this, "Publishing candidate...", Toast.LENGTH_SHORT).show();

                saveCandidateToDatabase(name);
            } catch (Exception e) {
                Log.e("TeacherDashboard", "Error in addCandidate button click", e);
                btnAddCandidate.setEnabled(true);
            }
        });

        // Live Statistics and Candidate List
        DatabaseReference candidateRef = !sanitizedSec.equals("--") ?
                dbRef.child("voting_controls").child(sanitizedDept).child(sanitizedSec).child("candidates") :
                dbRef.child("candidates");

        candidateRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && snapshot.hasChildren()) {
                    displayAdminCandidatesSnapshot(snapshot);
                } else {
                    dbRef.child("candidates").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot rootSnap) {
                            displayAdminCandidatesSnapshot(rootSnap);
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {}
                    });
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("TeacherDashboard", "Candidates query cancelled: " + error.getMessage());
            }
        });

        // Live Voters Statistics Count
        DatabaseReference votersRef = !sanitizedSec.equals("--") ?
                dbRef.child("voting_controls").child(sanitizedDept).child(sanitizedSec).child("voters") :
                dbRef.child("voters");

        votersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && snapshot.hasChildren()) {
                    displayVotersCountSnapshot(snapshot);
                } else {
                    dbRef.child("voters").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot rootSnap) {
                            displayVotersCountSnapshot(rootSnap);
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {}
                    });
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        // Safety Confirmation Bottom Sheet for Reset All (Material Design 3 & Glassmorphism)
        btnResetVotes.setOnClickListener(v -> {
            String activeSec = getTeacherAssignedSection();
            BottomSheetDialog sheet = new BottomSheetDialog(this);
            LinearLayout view = new LinearLayout(this);
            view.setOrientation(LinearLayout.VERTICAL);
            view.setPadding(48, 48, 48, 48);
            view.setBackgroundColor(Color.parseColor("#171A29"));

            TextView title = new TextView(this);
            title.setText("⚠️ Reset Section " + activeSec + " Election Data?");
            title.setTextSize(20f);
            title.setTypeface(null, Typeface.BOLD);
            title.setTextColor(Color.parseColor("#EF4444"));

            TextView msg = new TextView(this);
            msg.setText("This will permanently delete published candidates, votes, and voter security locks for Section " + activeSec + ". Other sections will not be affected. Proceed?");
            msg.setTextSize(14f);
            msg.setTextColor(Color.parseColor("#94A3B8"));
            msg.setPadding(0, 12, 0, 24);

            LinearLayout btnLayout = new LinearLayout(this);
            btnLayout.setOrientation(LinearLayout.HORIZONTAL);
            btnLayout.setGravity(Gravity.END);

            MaterialButton btnCancel = new MaterialButton(this, null, com.google.android.material.R.style.Widget_Material3_Button_TextButton);
            btnCancel.setText("Cancel");
            btnCancel.setTextColor(Color.parseColor("#94A3B8"));
            btnCancel.setOnClickListener(dt -> sheet.dismiss());

            MaterialButton btnConfirm = new MaterialButton(this);
            btnConfirm.setText("Yes, Reset");
            btnConfirm.setTextColor(Color.parseColor("#FFFFFF"));
            btnConfirm.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#EF4444")));
            btnConfirm.setCornerRadius(16);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
            lp.setMargins(16, 0, 0, 0);
            btnConfirm.setLayoutParams(lp);

            btnConfirm.setOnClickListener(dt -> {
                sheet.dismiss();
                dbRef.child("candidates").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Map<String, Object> resetMap = new HashMap<>();
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            String candSec = ds.child("section").getValue(String.class);
                            if (candSec == null) candSec = ds.child("assignedSection").getValue(String.class);
                            if (candSec == null || candSec.equals("--") || candSec.equalsIgnoreCase(activeSec) || activeSec.equals("--")) {
                                resetMap.put("candidates/" + ds.getKey(), null);
                            }
                        }

                        String activeDept = getTeacherDepartment();
                        String sanitizedDept = getSanitizedDepartment(activeDept);
                        String sanitizedSec = getSanitizedSection(activeSec);
                        if (!sanitizedSec.equals("--")) {
                            resetMap.put("voting_controls/" + sanitizedDept + "/" + sanitizedSec + "/candidates", null);
                            resetMap.put("voting_controls/" + sanitizedDept + "/" + sanitizedSec + "/votes", null);
                            resetMap.put("voting_controls/" + sanitizedDept + "/" + sanitizedSec + "/voters", null);
                            resetMap.put("voting_controls/" + sanitizedDept + "/" + sanitizedSec + "/isGateOpen", false);
                            resetMap.put("sections/" + sanitizedDept + "/" + sanitizedSec + "/candidates", null);
                            resetMap.put("sections/" + sanitizedDept + "/" + sanitizedSec + "/voters", null);
                        }

                        dbRef.child("voters").addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot vSnap) {
                                for (DataSnapshot vDs : vSnap.getChildren()) {
                                    String vSec = vDs.child("section").getValue(String.class);
                                    if (vSec == null) vSec = vDs.child("assignedSection").getValue(String.class);
                                    Object vVal = vDs.getValue();
                                    boolean isThisSec = (vSec != null && vSec.equalsIgnoreCase(activeSec)) ||
                                            (vVal instanceof String && sectionCandidateIds.contains((String) vVal));
                                    if (isThisSec) {
                                        resetMap.put("voters/" + vDs.getKey(), null);
                                    }
                                }

                                if (!resetMap.isEmpty()) {
                                    dbRef.updateChildren(resetMap).addOnSuccessListener(unused -> {
                                        showSnackBar("Election data for Section " + activeSec + " reset successfully.", false);
                                        logAdminAction("Reset Election Votes & Candidates for Section " + activeSec);
                                        broadcastNotificationToStudents(
                                            "⚠️ Election Portal Reset (Sec " + activeSec + ")",
                                            "Election portal data for Section " + activeSec + " has been reset by the teacher.",
                                            "RESET"
                                        );
                                    });
                                } else {
                                    showSnackBar("No election data found for Section " + activeSec + " to reset.", false);
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {}
                        });
                    }

                    @Override
                            public void onCancelled(@NonNull DatabaseError error) {}
                });
            });

            btnLayout.addView(btnCancel);
            btnLayout.addView(btnConfirm);
            view.addView(title);
            view.addView(msg);
            view.addView(btnLayout);
            sheet.setContentView(view);
            sheet.show();
        });

        // Safety Confirmation Bottom Sheet for Run-Off (Material Design 3 & Glassmorphism)
        btnRunOff.setOnClickListener(v -> {
            String activeSec = getTeacherAssignedSection();
            BottomSheetDialog sheet = new BottomSheetDialog(this);
            LinearLayout view = new LinearLayout(this);
            view.setOrientation(LinearLayout.VERTICAL);
            view.setPadding(48, 48, 48, 48);
            view.setBackgroundColor(Color.parseColor("#171A29"));

            TextView title = new TextView(this);
            title.setText("⚡ Initialize Run-Off for Section " + activeSec + "?");
            title.setTextSize(20f);
            title.setTypeface(null, Typeface.BOLD);
            title.setTextColor(Color.parseColor("#8B5CF6"));

            TextView msg = new TextView(this);
            msg.setText("This will reset vote counts to 0 for candidates in Section " + activeSec + " while keeping the candidate list. Voter security locks for Section " + activeSec + " will be cleared. Proceed?");
            msg.setTextSize(14f);
            msg.setTextColor(Color.parseColor("#94A3B8"));
            msg.setPadding(0, 12, 0, 24);

            LinearLayout btnLayout = new LinearLayout(this);
            btnLayout.setOrientation(LinearLayout.HORIZONTAL);
            btnLayout.setGravity(Gravity.END);

            MaterialButton btnCancel = new MaterialButton(this, null, com.google.android.material.R.style.Widget_Material3_Button_TextButton);
            btnCancel.setText("Cancel");
            btnCancel.setTextColor(Color.parseColor("#94A3B8"));
            btnCancel.setOnClickListener(dt -> sheet.dismiss());

            MaterialButton btnConfirm = new MaterialButton(this);
            btnConfirm.setText("Proceed");
            btnConfirm.setTextColor(Color.parseColor("#FFFFFF"));
            btnConfirm.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#8B5CF6")));
            btnConfirm.setCornerRadius(16);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
            lp.setMargins(16, 0, 0, 0);
            btnConfirm.setLayoutParams(lp);

            btnConfirm.setOnClickListener(dt -> {
                sheet.dismiss();
                dbRef.child("candidates").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Map<String, Object> updates = new HashMap<>();
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            String candSec = ds.child("section").getValue(String.class);
                            if (candSec == null) candSec = ds.child("assignedSection").getValue(String.class);
                            if (candSec == null || candSec.equals("--") || candSec.equalsIgnoreCase(activeSec) || activeSec.equals("--")) {
                                updates.put("candidates/" + ds.getKey() + "/votes", 0);
                                String activeDept = getTeacherDepartment();
                                String sanitizedDept = getSanitizedDepartment(activeDept);
                                String sanitizedSec = getSanitizedSection(activeSec);
                                if (!sanitizedSec.equals("--")) {
                                    updates.put("voting_controls/" + sanitizedDept + "/" + sanitizedSec + "/candidates/" + ds.getKey() + "/votes", 0);
                                    updates.put("sections/" + sanitizedDept + "/" + sanitizedSec + "/candidates/" + ds.getKey() + "/votes", 0);
                                }
                            }
                        }

                        dbRef.child("voters").addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot vSnap) {
                                for (DataSnapshot vDs : vSnap.getChildren()) {
                                    String vSec = vDs.child("section").getValue(String.class);
                                    if (vSec == null) vSec = vDs.child("assignedSection").getValue(String.class);
                                    Object vVal = vDs.getValue();
                                    boolean isThisSec = (vSec != null && vSec.equalsIgnoreCase(activeSec)) ||
                                            (vVal instanceof String && sectionCandidateIds.contains((String) vVal));
                                    if (isThisSec) {
                                        updates.put("voters/" + vDs.getKey(), null);
                                    }
                                }

                                String activeDept = getTeacherDepartment();
                                String sanitizedDept = getSanitizedDepartment(activeDept);
                                String sanitizedSec = getSanitizedSection(activeSec);
                                if (!sanitizedSec.equals("--")) {
                                    updates.put("voting_controls/" + sanitizedDept + "/" + sanitizedSec + "/voters", null);
                                    updates.put("sections/" + sanitizedDept + "/" + sanitizedSec + "/voters", null);
                                }

                                if (!updates.isEmpty()) {
                                    dbRef.updateChildren(updates).addOnSuccessListener(unused -> {
                                        showSnackBar("Run-off election round initialized for Section " + activeSec + "!", false);
                                        logAdminAction("Initialized Run-Off Election Round for Section " + activeSec);
                                        broadcastNotificationToStudents(
                                            "⚡ Run-Off Round Initialized (Sec " + activeSec + ")",
                                            "A run-off election round has been initiated for Section " + activeSec + ". All vote locks are cleared!",
                                            "RUN_OFF"
                                        );
                                    });
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {}
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
            });

            btnLayout.addView(btnCancel);
            btnLayout.addView(btnConfirm);
            view.addView(title);
            view.addView(msg);
            view.addView(btnLayout);
            sheet.setContentView(view);
            sheet.show();
        });
    }

    private void displayAdminCandidatesSnapshot(DataSnapshot snapshot) {
        try {
            if (isFinishing() || isDestroyed()) return;
            String activeSec = getTeacherAssignedSection();
            sectionCandidateIds.clear();

            int totalCandidates = 0;
            int totalVotes = 0;
            containerAdminCandidates.removeAllViews();

            List<DataSnapshot> candidateList = new ArrayList<>();
            for (DataSnapshot ds : snapshot.getChildren()) {
                String candSec = ds.child("section").getValue(String.class);
                if (candSec == null) candSec = ds.child("assignedSection").getValue(String.class);

                if (candSec != null && !candSec.equals("--") && !activeSec.equals("--") && !candSec.equalsIgnoreCase(activeSec)) {
                    continue;
                }

                String candId = ds.getKey();
                if (candId != null) {
                    sectionCandidateIds.add(candId);
                }

                candidateList.add(ds);
                totalCandidates++;
                int vts = getSafeVotes(ds);
                totalVotes += vts;
            }

            // Display Live Vote Distribution Chart in Teacher Dashboard
            if (!candidateList.isEmpty() && totalVotes > 0) {
                updateVoteDistributionChart(candidateList, totalVotes, containerAdminCandidates);
            }

            for (DataSnapshot ds : candidateList) {
                String candId = ds.getKey();
                int vts = getSafeVotes(ds);
                String imageUrl = ds.child("imageUrl").getValue(String.class);
                String cName = ds.child("name").getValue(String.class);
                if (cName == null) cName = "Candidate";
                addAdminRow(candId, cName, vts, imageUrl);
            }

            tvTotalCandidates.setText("Total (Sec " + activeSec + "): " + totalCandidates);
            tvStatTotalVotes.setText(String.valueOf(totalVotes));
        } catch (Exception e) {
            Log.e("TeacherDashboard", "Error loading candidates list", e);
        }
    }

    private void updateVoteDistributionChart(List<DataSnapshot> candidateList, int totalVotes, LinearLayout container) {
        try {
            MaterialCardView chartCard = new MaterialCardView(this);
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, -2);
            cardParams.setMargins(0, 0, 0, 16);
            chartCard.setLayoutParams(cardParams);
            chartCard.setRadius(24);
            chartCard.setCardElevation(6);
            chartCard.setCardBackgroundColor(Color.parseColor("#171A29"));
            chartCard.setStrokeWidth(2);
            chartCard.setStrokeColor(Color.parseColor("#38BDF8"));

            LinearLayout chartLayout = new LinearLayout(this);
            chartLayout.setOrientation(LinearLayout.VERTICAL);
            chartLayout.setPadding(28, 24, 28, 24);

            TextView tvChartTitle = new TextView(this);
            tvChartTitle.setText("📊 Live Vote Distribution Chart");
            tvChartTitle.setTextSize(16f);
            tvChartTitle.setTypeface(null, Typeface.BOLD);
            tvChartTitle.setTextColor(Color.parseColor("#F8FAFC"));
            tvChartTitle.setPadding(0, 0, 0, 12);
            chartLayout.addView(tvChartTitle);

            LinearLayout stackedBar = new LinearLayout(this);
            stackedBar.setOrientation(LinearLayout.HORIZONTAL);
            stackedBar.setLayoutParams(new LinearLayout.LayoutParams(-1, 24));
            stackedBar.setWeightSum(Math.max(totalVotes, 1));

            int[] colors = {
                Color.parseColor("#38BDF8"), // Cyan
                Color.parseColor("#8B5CF6"), // Purple
                Color.parseColor("#10B981"), // Emerald
                Color.parseColor("#F59E0B"), // Amber
                Color.parseColor("#F43F5E"), // Rose
                Color.parseColor("#6366F1")  // Indigo
            };

            LinearLayout legendLayout = new LinearLayout(this);
            legendLayout.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams legendParams = new LinearLayout.LayoutParams(-1, -2);
            legendParams.setMargins(0, 16, 0, 0);
            legendLayout.setLayoutParams(legendParams);

            int index = 0;
            for (DataSnapshot item : candidateList) {
                int vts = getSafeVotes(item);
                if (vts < 0) vts = 0;
                String cName = item.child("name").getValue(String.class);
                if (cName == null) cName = "Candidate";

                int color = colors[index % colors.length];

                View segment = new View(this);
                LinearLayout.LayoutParams segParams = new LinearLayout.LayoutParams(0, -1, (float) Math.max(vts, 0.1));
                if (index > 0) segParams.setMargins(4, 0, 0, 0);
                segment.setLayoutParams(segParams);
                segment.setBackgroundColor(color);
                stackedBar.addView(segment);

                LinearLayout legendRow = new LinearLayout(this);
                legendRow.setOrientation(LinearLayout.HORIZONTAL);
                legendRow.setGravity(Gravity.CENTER_VERTICAL);
                legendRow.setPadding(0, 4, 0, 4);

                View dot = new View(this);
                int dotSize = (int) (10 * getResources().getDisplayMetrics().density);
                LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(dotSize, dotSize);
                dotParams.setMargins(0, 0, 12, 0);
                dot.setLayoutParams(dotParams);
                dot.setBackgroundColor(color);

                TextView tvLegendText = new TextView(this);
                int percentage = totalVotes > 0 ? (vts * 100) / totalVotes : 0;
                tvLegendText.setText(cName + ": " + vts + " votes (" + percentage + "%)");
                tvLegendText.setTextSize(13f);
                tvLegendText.setTextColor(Color.parseColor("#CBD5E1"));

                legendRow.addView(dot);
                legendRow.addView(tvLegendText);
                legendLayout.addView(legendRow);

                index++;
            }

            chartLayout.addView(stackedBar);
            chartLayout.addView(legendLayout);
            chartCard.addView(chartLayout);
            container.addView(chartCard);
        } catch (Exception e) {
            Log.e("TeacherDashboard", "Error rendering vote chart", e);
        }
    }

    private void displayVotersCountSnapshot(DataSnapshot snapshot) {
        try {
            if (isFinishing() || isDestroyed()) return;
            String activeSec = getTeacherAssignedSection();
            long totalVoters = 0;

            for (DataSnapshot ds : snapshot.getChildren()) {
                String vSec = ds.child("section").getValue(String.class);
                if (vSec == null) vSec = ds.child("assignedSection").getValue(String.class);

                boolean matchesSection;
                if (vSec != null && !vSec.equals("--")) {
                    matchesSection = vSec.equalsIgnoreCase(activeSec);
                } else {
                    Object votedVal = ds.getValue();
                    if (votedVal instanceof String) {
                        String votedCandId = (String) votedVal;
                        matchesSection = sectionCandidateIds.contains(votedCandId);
                    } else if (ds.hasChild("candidateId")) {
                        String votedCandId = ds.child("candidateId").getValue(String.class);
                        matchesSection = sectionCandidateIds.contains(votedCandId);
                    } else {
                        matchesSection = sectionCandidateIds.isEmpty() || activeSec.equals("--");
                    }
                }

                if (matchesSection) {
                    totalVoters++;
                }
            }

            tvStatTotalVoters.setText(String.valueOf(totalVoters));
            int estimatedTotalStudents = 50; 
            int turnout = (int) Math.min(100, (totalVoters * 100) / estimatedTotalStudents);
            tvStatTurnout.setText(turnout + "%");
        } catch (Exception e) {
            Log.e("TeacherDashboard", "Error counting voters", e);
        }
    }

    private void saveCandidateToDatabase(String name) {
        String teacherDept = getTeacherDepartment();
        String sanitizedDept = getSanitizedDepartment(teacherDept);
        String teacherSec = getTeacherAssignedSection();
        String cid = dbRef.child("candidates").push().getKey();
        if (cid != null) {
            Map<String, Object> map = new HashMap<>();
            map.put("name", name);
            map.put("votes", 0);
            map.put("imageUrl", "");
            map.put("dept", teacherDept);
            map.put("section", teacherSec);
            map.put("assignedSection", teacherSec);
            map.put("manifesto", "Committed to student welfare, transparent CR communication, and academic support sessions.");

            etCandidateName.setText("");
            etCandidateName.clearFocus();
            btnAddCandidate.setEnabled(true);

            Map<String, Object> childUpdates = new HashMap<>();
            childUpdates.put("candidates/" + cid, map);
            String sanitizedSec = getSanitizedSection(teacherSec);
            if (!sanitizedSec.equals("--")) {
                childUpdates.put("voting_controls/" + sanitizedDept + "/" + sanitizedSec + "/candidates/" + cid, map);
                childUpdates.put("voting_controls/" + sanitizedDept + "/" + sanitizedSec + "/votes/" + cid, 0);
                childUpdates.put("sections/" + sanitizedDept + "/" + sanitizedSec + "/candidates/" + cid, map);
            }

            dbRef.updateChildren(childUpdates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Successfully added candidate to Section " + teacherSec + "!", Toast.LENGTH_SHORT).show();
                    logAdminAction("Added New Candidate (" + name + ") for Section " + teacherSec);
                    broadcastNotificationToStudents(
                        "📢 New Candidate Published (Sec " + teacherSec + ")",
                        name + " has been added to Section " + teacherSec + " election portal. Check out their manifesto!",
                        "NEW_CANDIDATE"
                    );
                })
                .addOnFailureListener(e -> {
                    Log.e("TeacherDashboard", "Failed to write candidate", e);
                    btnAddCandidate.setEnabled(true);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
        } else {
            btnAddCandidate.setEnabled(true);
        }
    }

    private void addAdminRow(String candidateId, String name, Integer vts, String imageUrl) {
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
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(28, 22, 28, 22);

        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout textCol = new LinearLayout(this);
        textCol.setOrientation(LinearLayout.VERTICAL);
        textCol.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));

        TextView tvName = new TextView(this);
        tvName.setText(name);
        tvName.setTextSize(17f);
        tvName.setTextColor(Color.parseColor("#F8FAFC"));
        tvName.setTypeface(null, Typeface.BOLD);

        TextView tvVotes = new TextView(this);
        tvVotes.setText("0 Votes");
        tvVotes.setTextSize(14f);
        tvVotes.setTextColor(Color.parseColor("#38BDF8"));
        tvVotes.setPadding(0, 6, 0, 0);
        tvVotes.setTypeface(null, Typeface.BOLD);

        textCol.addView(tvName);
        textCol.addView(tvVotes);

        MaterialButton btnDelete = new MaterialButton(this);
        btnDelete.setText("Delete");
        btnDelete.setTextSize(13f);
        btnDelete.setCornerRadius(16);
        btnDelete.setStrokeWidth(2);
        btnDelete.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#EF4444")));
        btnDelete.setTextColor(Color.parseColor("#EF4444"));
        btnDelete.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
        btnDelete.setLayoutParams(new LinearLayout.LayoutParams(-2, -2));

        btnDelete.setOnClickListener(v -> {
            BottomSheetDialog sheet = new BottomSheetDialog(this);
            LinearLayout view = new LinearLayout(this);
            view.setOrientation(LinearLayout.VERTICAL);
            view.setPadding(48, 48, 48, 48);
            view.setBackgroundColor(Color.parseColor("#171A29"));

            TextView title = new TextView(this);
            title.setText("Delete Candidate");
            title.setTextSize(20f);
            title.setTypeface(null, Typeface.BOLD);
            title.setTextColor(Color.parseColor("#F87171"));

            TextView msg = new TextView(this);
            msg.setText("Remove " + name + "?");
            msg.setTextSize(14f);
            msg.setTextColor(Color.parseColor("#94A3B8"));
            msg.setPadding(0, 12, 0, 24);

            LinearLayout btnLayout = new LinearLayout(this);
            btnLayout.setOrientation(LinearLayout.HORIZONTAL);
            btnLayout.setGravity(Gravity.END);

            MaterialButton btnCancel = new MaterialButton(this, null, com.google.android.material.R.style.Widget_Material3_Button_TextButton);
            btnCancel.setText("Cancel");
            btnCancel.setTextColor(Color.parseColor("#94A3B8"));
            btnCancel.setOnClickListener(dt -> sheet.dismiss());

            MaterialButton btnConfirm = new MaterialButton(this);
            btnConfirm.setText("Delete");
            btnConfirm.setTextColor(Color.parseColor("#FFFFFF"));
            btnConfirm.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#F87171")));
            btnConfirm.setCornerRadius(16);
            LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(-2, -2);
            btnLp.setMargins(16, 0, 0, 0);
            btnConfirm.setLayoutParams(btnLp);

            btnConfirm.setOnClickListener(dt -> {
                sheet.dismiss();
                if (candidateId != null) {
                    String teacherDept = getTeacherDepartment();
                    String sanitizedDept = getSanitizedDepartment(teacherDept);
                    String teacherSec = getTeacherAssignedSection();
                    String sanitizedSec = getSanitizedSection(teacherSec);
                    Map<String, Object> removeUpdates = new HashMap<>();
                    removeUpdates.put("candidates/" + candidateId, null);
                    if (!sanitizedSec.equals("--")) {
                        removeUpdates.put("voting_controls/" + sanitizedDept + "/" + sanitizedSec + "/candidates/" + candidateId, null);
                        removeUpdates.put("voting_controls/" + sanitizedDept + "/" + sanitizedSec + "/votes/" + candidateId, null);
                        removeUpdates.put("sections/" + sanitizedDept + "/" + sanitizedSec + "/candidates/" + candidateId, null);
                    }
                    dbRef.updateChildren(removeUpdates)
                        .addOnSuccessListener(aVoid -> showSnackBar("Candidate removed", false));
                }
            });

            btnLayout.addView(btnCancel);
            btnLayout.addView(btnConfirm);
            view.addView(title);
            view.addView(msg);
            view.addView(btnLayout);
            sheet.setContentView(view);
            sheet.show();
        });

        topRow.addView(textCol);
        topRow.addView(btnDelete);
        row.addView(topRow);

        // Animated Progress Bar with Smooth Real-time Animation
        ProgressBar progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        LinearLayout.LayoutParams pbParams = new LinearLayout.LayoutParams(-1, 16);
        pbParams.setMargins(0, 12, 0, 0);
        progressBar.setLayoutParams(pbParams);
        progressBar.setMax(50);
        progressBar.setProgress(0);
        progressBar.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#38BDF8")));
        row.addView(progressBar);

        int finalVotes = vts != null ? vts : 0;
        ObjectAnimator progressAnim = ObjectAnimator.ofInt(progressBar, "progress", 0, finalVotes);
        progressAnim.setDuration(800);
        progressAnim.setInterpolator(new DecelerateInterpolator());
        progressAnim.start();

        ValueAnimator countAnim = ValueAnimator.ofInt(0, finalVotes);
        countAnim.setDuration(800);
        countAnim.addUpdateListener(animation -> {
            int val = (int) animation.getAnimatedValue();
            tvVotes.setText(val + " Votes");
        });
        countAnim.start();

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
        String teacherDept = getTeacherDepartment();
        String sanitizedDept = getSanitizedDepartment(teacherDept);
        String teacherSec = getTeacherAssignedSection();

        String logId = dbRef.child("audit_logs").push().getKey();
        if (logId != null) {
            Map<String, Object> logMap = new HashMap<>();
            logMap.put("teacherName", teacherName);
            logMap.put("teacherEmail", teacherEmail);
            logMap.put("teacherId", teacherId);
            logMap.put("assignedSection", teacherSec);
            logMap.put("action", actionDescription);
            logMap.put("timestamp", ServerValue.TIMESTAMP);

            dbRef.child("audit_logs").child(logId).setValue(logMap);
            String sanitizedSec = getSanitizedSection(teacherSec);
            if (!sanitizedSec.equals("--")) {
                dbRef.child("sections").child(sanitizedDept).child(sanitizedSec).child("audit_logs").child(logId).setValue(logMap);
            }
        }
    }

    private void broadcastNotificationToStudents(String title, String body, String actionType) {
        String teacherDept = getTeacherDepartment();
        String sanitizedDept = getSanitizedDepartment(teacherDept);
        String teacherSec = getTeacherAssignedSection();
        String sanitizedSec = getSanitizedSection(teacherSec);
        String notifId = dbRef.child("notifications_queue").push().getKey();

        Map<String, Object> notifMap = new HashMap<>();
        notifMap.put("title", title);
        notifMap.put("body", body);
        notifMap.put("topic", !sanitizedSec.equals("--") ? "section_" + sanitizedSec : "all_students");
        notifMap.put("section", teacherSec);
        notifMap.put("actionType", actionType);
        notifMap.put("sender", sessionManager.getUserName());
        notifMap.put("timestamp", ServerValue.TIMESTAMP);

        if (notifId != null) {
            dbRef.child("notifications_queue").child(notifId).setValue(notifMap);
            if (!sanitizedSec.equals("--")) {
                dbRef.child("sections").child(sanitizedDept).child(sanitizedSec).child("notifications_queue").child(notifId).setValue(notifMap);
            }
        }
        dbRef.child("broadcast_notifications").setValue(notifMap);
        if (!sanitizedSec.equals("--")) {
            dbRef.child("sections").child(sanitizedDept).child(sanitizedSec).child("broadcast_notifications").setValue(notifMap);
        }
    }

    private void exportElectionResultsToPdf() {
        String teacherDept = getTeacherDepartment();
        String sanitizedDept = getSanitizedDepartment(teacherDept);
        String teacherSec = getTeacherAssignedSection();
        Toast.makeText(this, "Generating Election PDF Report for Section " + teacherSec + "...", Toast.LENGTH_SHORT).show();

        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String sanitizedSec = getSanitizedSection(teacherSec);
                Boolean gateStatus = null;
                if (!sanitizedSec.equals("--") && snapshot.child("voting_controls").child(sanitizedDept).child(sanitizedSec).hasChild("isGateOpen")) {
                    gateStatus = snapshot.child("voting_controls").child(sanitizedDept).child(sanitizedSec).child("isGateOpen").getValue(Boolean.class);
                }
                if (gateStatus == null && !sanitizedSec.equals("--") && snapshot.child("sections").child(sanitizedDept).child(sanitizedSec).hasChild("master_voting_enabled")) {
                    gateStatus = snapshot.child("sections").child(sanitizedDept).child(sanitizedSec).child("master_voting_enabled").getValue(Boolean.class);
                }
                if (gateStatus == null) {
                    gateStatus = snapshot.child("master_voting_enabled").getValue(Boolean.class);
                }

                DataSnapshot candidatesSnap = (!sanitizedSec.equals("--") && snapshot.child("voting_controls").child(sanitizedDept).child(sanitizedSec).hasChild("candidates")) ?
                        snapshot.child("voting_controls").child(sanitizedDept).child(sanitizedSec).child("candidates") :
                        snapshot.child("candidates");

                DataSnapshot votersSnap = (!sanitizedSec.equals("--") && snapshot.child("voting_controls").child(sanitizedDept).child(sanitizedSec).hasChild("voters")) ?
                        snapshot.child("voting_controls").child(sanitizedDept).child(sanitizedSec).child("voters") :
                        snapshot.child("voters");

                int totalVotes = 0;
                List<PdfReportGenerator.CandidateResult> rawList = new ArrayList<>();

                for (DataSnapshot ds : candidatesSnap.getChildren()) {
                    String candSec = ds.child("section").getValue(String.class);
                    if (candSec == null) candSec = ds.child("assignedSection").getValue(String.class);

                    // Filter strictly for teacher's assigned section
                    if (candSec != null && !candSec.equals("--") && !teacherSec.equals("--") && !candSec.equalsIgnoreCase(teacherSec)) {
                        continue;
                    }

                    String name = ds.child("name").getValue(String.class);
                    String dept = ds.child("dept").getValue(String.class);
                    if (name == null) name = "Candidate";
                    if (dept == null || dept.isEmpty()) dept = "Department of CSE, BUBT";
                    Integer vts = ds.child("votes").getValue(Integer.class);
                    int votes = vts != null ? vts : 0;
                    totalVotes += votes;

                    rawList.add(new PdfReportGenerator.CandidateResult(name, dept, votes, 0));
                }

                rawList.sort((c1, c2) -> Integer.compare(c2.votes, c1.votes));

                List<PdfReportGenerator.CandidateResult> rankedList = new ArrayList<>();
                int rank = 1;
                for (PdfReportGenerator.CandidateResult c : rawList) {
                    rankedList.add(new PdfReportGenerator.CandidateResult(c.name, c.dept, c.votes, rank));
                    rank++;
                }

                int totalVoters = 0;
                for (DataSnapshot vDs : votersSnap.getChildren()) {
                    String vSec = vDs.child("section").getValue(String.class);
                    if (vSec == null) vSec = vDs.child("assignedSection").getValue(String.class);
                    Object vVal = vDs.getValue();
                    boolean matchesSec = (vSec != null && vSec.equalsIgnoreCase(teacherSec)) ||
                            (vVal instanceof String && sectionCandidateIds.contains((String) vVal)) ||
                            (sectionCandidateIds.isEmpty() || teacherSec.equals("--"));
                    if (matchesSec) {
                        totalVoters++;
                    }
                }

                int estimatedTotalStudents = 50;
                int turnoutInt = (int) Math.min(100, (totalVoters * 100) / estimatedTotalStudents);
                String turnoutStr = turnoutInt + "%";

                String teacherName = sessionManager.getUserName();
                String teacherDept = sessionManager.getUserDept() + " (Section " + teacherSec + ")";

                File pdfFile = PdfReportGenerator.generatePdfDocument(
                        TeacherDashboardActivity.this,
                        teacherName,
                        teacherDept,
                        totalVotes,
                        totalVoters,
                        turnoutStr,
                        gateStatus != null && gateStatus,
                        rankedList
                );

                if (pdfFile != null) {
                    logAdminAction("Exported Election Results to PDF Report for Section " + teacherSec);

                    BottomSheetDialog pdfSheet = new BottomSheetDialog(TeacherDashboardActivity.this);
                    View view = getLayoutInflater().inflate(R.layout.dialog_pdf_ready, null);

                    MaterialButton btnPrint = view.findViewById(R.id.btnPrintPdf);
                    MaterialButton btnShare = view.findViewById(R.id.btnSharePdf);
                    MaterialButton btnOpen = view.findViewById(R.id.btnOpenPdf);

                    btnPrint.setOnClickListener(dt -> {
                        pdfSheet.dismiss();
                        PdfReportGenerator.printPdfDocument(TeacherDashboardActivity.this, pdfFile);
                    });

                    btnShare.setOnClickListener(dt -> {
                        pdfSheet.dismiss();
                        PdfReportGenerator.sharePdfFile(TeacherDashboardActivity.this, pdfFile);
                    });

                    btnOpen.setOnClickListener(dt -> {
                        pdfSheet.dismiss();
                        try {
                            Uri fileUri = FileProvider.getUriForFile(
                                TeacherDashboardActivity.this,
                                "com.example.crconnect.fileprovider",
                                pdfFile
                            );
                            Intent openIntent = new Intent(Intent.ACTION_VIEW);
                            openIntent.setDataAndType(fileUri, "application/pdf");
                            openIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            openIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            TeacherDashboardActivity.this.startActivity(openIntent);
                        } catch (Exception e) {
                            Log.e("TeacherDashboard", "Error opening PDF", e);
                            Toast.makeText(TeacherDashboardActivity.this, "No PDF viewer app found or error opening file", Toast.LENGTH_SHORT).show();
                        }
                    });

                    pdfSheet.setContentView(view);
                    pdfSheet.show();
                } else {
                    Toast.makeText(TeacherDashboardActivity.this, "Error compiling PDF document", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(TeacherDashboardActivity.this, "Failed to load data for PDF: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String uriToBase64(Uri uri) {
        try {
            if (uri == null) return "";
            Bitmap bitmap;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.Source source = ImageDecoder.createSource(getContentResolver(), uri);
                bitmap = ImageDecoder.decodeBitmap(source);
            } else {
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
            }
            if (bitmap != null) {
                int maxSize = 200;
                int width = bitmap.getWidth();
                int height = bitmap.getHeight();
                float ratio = (float) width / (float) height;
                if (ratio > 1) {
                    width = maxSize;
                    height = (int) (width / ratio);
                } else {
                    height = maxSize;
                    width = (int) (height * ratio);
                }
                Bitmap resized = Bitmap.createScaledBitmap(bitmap, width, height, true);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                resized.compress(Bitmap.CompressFormat.JPEG, 75, baos);
                byte[] bytes = baos.toByteArray();
                return Base64.encodeToString(bytes, Base64.DEFAULT);
            }
        } catch (Exception e) {
            Log.e("Base64Util", "Error converting uri to base64", e);
        }
        return "";
    }

    public static void loadProfileImage(Context context, String base64OrUrl, ImageView imageView) {
        try {
            if (base64OrUrl != null && !base64OrUrl.isEmpty()) {
                if (base64OrUrl.startsWith("http")) {
                    Glide.with(context).load(base64OrUrl).placeholder(R.drawable.ic_app_main).error(R.drawable.ic_app_main).circleCrop().into(imageView);
                } else {
                    byte[] decodedBytes = Base64.decode(base64OrUrl, Base64.DEFAULT);
                    Glide.with(context).load(decodedBytes).placeholder(R.drawable.ic_app_main).error(R.drawable.ic_app_main).circleCrop().into(imageView);
                }
            } else {
                Glide.with(context).load(R.drawable.ic_app_main).circleCrop().into(imageView);
            }
        } catch (Exception e) {
            Glide.with(context).load(R.drawable.ic_app_main).circleCrop().into(imageView);
        }
    }

    private void uploadProfilePictureToFirebase(Uri imageUri) {
        try {
            if (imageUri == null) return;
            String base64Img = uriToBase64(imageUri);
            if (base64Img.isEmpty()) {
                Toast.makeText(this, "Failed to process image", Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            String uid = currentUser != null ? currentUser.getUid() : "";
            String rawId = sessionManager != null ? sessionManager.getUserID() : "";
            String sanitizedId = rawId != null ? rawId.replace(".", "_").replace("@", "_").replace("#", "_").replace("$", "_").replace("[", "_").replace("]", "_") : "user";

            sessionManager.setProfileImageUrl(base64Img);
            loadProfileImage(this, base64Img, imgUserProfile);

            Map<String, Object> updateMap = new HashMap<>();
            updateMap.put("profileImageUrl", base64Img);

            if (!uid.isEmpty()) {
                dbRef.child("users").child(uid).updateChildren(updateMap);
                FirebaseFirestore.getInstance().collection("users").document(uid).update(updateMap);
            }
            if (!sanitizedId.isEmpty()) {
                dbRef.child("users").child(sanitizedId).updateChildren(updateMap);
                FirebaseFirestore.getInstance().collection("users").document(sanitizedId).update(updateMap);
            }

            Toast.makeText(this, "Profile picture updated successfully!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e("TeacherDashboard", "Exception in uploadProfilePictureToFirebase (Base64)", e);
            Toast.makeText(this, "Error updating profile picture: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private int getSafeVotes(DataSnapshot ds) {
        try {
            if (ds == null || !ds.hasChild("votes")) return 0;
            Object val = ds.child("votes").getValue();
            if (val instanceof Long) return ((Long) val).intValue();
            if (val instanceof Integer) return (Integer) val;
            if (val instanceof String) return Integer.parseInt((String) val);
        } catch (Exception e) {
            Log.e("TeacherDashboard", "Error parsing safe votes", e);
        }
        return 0;
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkUserAuthenticationAndRole();
        startAdminTimerLoop();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopAdminTimerLoop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAdminTimerLoop();
        if (votingControlRef != null && votingControlListener != null) {
            votingControlRef.removeEventListener(votingControlListener);
        }
    }

    private synchronized void setupVotingControlListener() {
        String dept = getTeacherDepartment();
        String sanitizedDept = getSanitizedDepartment(dept);
        String sec = getTeacherAssignedSection();
        String sanitizedSec = getSanitizedSection(sec);

        if (sanitizedSec.equals("--")) return;

        if (votingControlRef != null && votingControlListener != null) {
            votingControlRef.removeEventListener(votingControlListener);
        }

        votingControlRef = dbRef.child("voting_controls").child(sanitizedDept).child(sanitizedSec);
        votingControlListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Boolean gateOpen = snapshot.child("isGateOpen").getValue(Boolean.class);
                    if (gateOpen == null) gateOpen = snapshot.child("isOpen").getValue(Boolean.class);
                    Boolean paused = snapshot.child("isPaused").getValue(Boolean.class);
                    Long expiry = snapshot.child("expiryTime").getValue(Long.class);
                    Long remMs = snapshot.child("remainingTimeMs").getValue(Long.class);

                    currentIsGateOpen = (gateOpen != null && gateOpen);
                    currentIsPaused = (paused != null && paused);
                    currentExpiryTime = (expiry != null) ? expiry : 0L;
                    currentRemainingMs = (remMs != null) ? remMs : 0L;

                    if (switchMasterVoting != null) {
                        boolean shouldCheck = currentIsGateOpen && !currentIsPaused && (currentExpiryTime > System.currentTimeMillis() || currentRemainingMs > 0);
                        if (switchMasterVoting.isChecked() != shouldCheck) {
                            switchMasterVoting.setOnCheckedChangeListener(null);
                            switchMasterVoting.setChecked(shouldCheck);
                            rebindSwitchListener();
                        }
                    }

                    updateAdminTimerUI();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        votingControlRef.addValueEventListener(votingControlListener);
    }

    private void rebindSwitchListener() {
        if (switchMasterVoting == null) return;
        switchMasterVoting.setOnCheckedChangeListener((btn, isChecked) -> {
            String currentDept = getTeacherDepartment();
            String currentSanitizedDept = getSanitizedDepartment(currentDept);
            String currentSec = getTeacherAssignedSection();
            String currentSanitizedSec = getSanitizedSection(currentSec);

            if (!currentSanitizedSec.equals("--")) {
                DatabaseReference ctrlRef = dbRef.child("voting_controls").child(currentSanitizedDept).child(currentSanitizedSec);
                Map<String, Object> updates = new HashMap<>();
                updates.put("isGateOpen", isChecked);
                updates.put("isOpen", isChecked);
                if (isChecked) {
                    updates.put("isPaused", false);
                    updates.put("remainingTimeMs", 0L);
                    if (currentExpiryTime <= System.currentTimeMillis()) {
                        long defaultExpiry = System.currentTimeMillis() + (15 * 60 * 1000L);
                        updates.put("expiryTime", defaultExpiry);
                    }
                } else {
                    updates.put("isPaused", false);
                    updates.put("expiryTime", System.currentTimeMillis());
                    updates.put("remainingTimeMs", 0L);
                }
                ctrlRef.updateChildren(updates);
                dbRef.child("sections").child(currentSanitizedDept).child(currentSanitizedSec).child("master_voting_enabled").setValue(isChecked);
            }
            dbRef.child("master_voting_enabled").setValue(isChecked);

            logAdminAction(isChecked ? "Enabled Master Voting for Section " + currentSec : "Disabled Master Voting for Section " + currentSec);

            String notifTitle = isChecked ? "🟢 Live Voting is OPEN (Sec " + currentSec + ")" : "🔴 Live Voting is LOCKED (Sec " + currentSec + ")";
            String notifBody = isChecked ?
                    "Election admin " + sessionManager.getUserName() + " has opened live voting for Section " + currentSec + "! Cast your vote now." :
                    "Live voting for Section " + currentSec + " has been locked by election admin " + sessionManager.getUserName() + ".";
            broadcastNotificationToStudents(notifTitle, notifBody, "GATE_CONTROL");
        });
    }

    private void startAdminTimerLoop() {
        stopAdminTimerLoop();
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                updateAdminTimerUI();
                timerHandler.postDelayed(this, 1000);
            }
        };
        timerHandler.post(timerRunnable);
    }

    private void stopAdminTimerLoop() {
        if (timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
            timerRunnable = null;
        }
    }

    private void updateAdminTimerUI() {
        if (tvAdminTimerDisplay == null || tvAdminTimerStatus == null) return;

        if (!currentIsGateOpen) {
            tvAdminTimerDisplay.setText("00:00");
            tvAdminTimerDisplay.setTextColor(Color.parseColor("#EF4444"));
            tvAdminTimerStatus.setText("CLOSED 🔴");
            tvAdminTimerStatus.setTextColor(Color.parseColor("#EF4444"));

            if (btnPauseResume != null) {
                btnPauseResume.setText("⏸️ Pause Voting");
                btnPauseResume.setEnabled(false);
            }
            if (btnExtend5Min != null) btnExtend5Min.setEnabled(true);
            if (btnExtend10Min != null) btnExtend10Min.setEnabled(true);
            if (btnReduce5Min != null) btnReduce5Min.setEnabled(false);
            if (btnEndVotingNow != null) btnEndVotingNow.setEnabled(false);
            return;
        }

        if (currentIsPaused) {
            long rem = currentRemainingMs > 0 ? currentRemainingMs : Math.max(0, currentExpiryTime - System.currentTimeMillis());
            tvAdminTimerDisplay.setText(formatTimeMs(rem));
            tvAdminTimerDisplay.setTextColor(Color.parseColor("#F59E0B"));
            tvAdminTimerStatus.setText("PAUSED ⏸️");
            tvAdminTimerStatus.setTextColor(Color.parseColor("#F59E0B"));

            if (btnPauseResume != null) {
                btnPauseResume.setText("▶️ Resume Voting");
                btnPauseResume.setEnabled(true);
            }
            if (btnExtend5Min != null) btnExtend5Min.setEnabled(true);
            if (btnExtend10Min != null) btnExtend10Min.setEnabled(true);
            if (btnReduce5Min != null) btnReduce5Min.setEnabled(true);
            if (btnEndVotingNow != null) btnEndVotingNow.setEnabled(true);
            return;
        }

        long millisLeft = currentExpiryTime - System.currentTimeMillis();
        if (millisLeft <= 0) {
            tvAdminTimerDisplay.setText("00:00");
            tvAdminTimerDisplay.setTextColor(Color.parseColor("#EF4444"));
            tvAdminTimerStatus.setText("ENDED 🔴");
            tvAdminTimerStatus.setTextColor(Color.parseColor("#EF4444"));

            if (btnPauseResume != null) {
                btnPauseResume.setText("⏸️ Pause Voting");
                btnPauseResume.setEnabled(false);
            }
            if (btnExtend5Min != null) btnExtend5Min.setEnabled(true);
            if (btnExtend10Min != null) btnExtend10Min.setEnabled(true);
            if (btnReduce5Min != null) btnReduce5Min.setEnabled(false);
            if (btnEndVotingNow != null) btnEndVotingNow.setEnabled(false);

            if (currentIsGateOpen) {
                currentIsGateOpen = false;
                String dept = getSanitizedDepartment(getTeacherDepartment());
                String sec = getSanitizedSection(getTeacherAssignedSection());
                if (!sec.equals("--")) {
                    Map<String, Object> autoEndMap = new HashMap<>();
                    autoEndMap.put("isGateOpen", false);
                    autoEndMap.put("isOpen", false);
                    autoEndMap.put("isPaused", false);
                    dbRef.child("voting_controls").child(dept).child(sec).updateChildren(autoEndMap);
                }
            }
        } else {
            tvAdminTimerDisplay.setText(formatTimeMs(millisLeft));
            tvAdminTimerDisplay.setTextColor(Color.parseColor("#10B981"));
            tvAdminTimerStatus.setText("ACTIVE 🟢");
            tvAdminTimerStatus.setTextColor(Color.parseColor("#10B981"));

            if (btnPauseResume != null) {
                btnPauseResume.setText("⏸️ Pause Voting");
                btnPauseResume.setEnabled(true);
            }
            if (btnExtend5Min != null) btnExtend5Min.setEnabled(true);
            if (btnExtend10Min != null) btnExtend10Min.setEnabled(true);
            if (btnReduce5Min != null) btnReduce5Min.setEnabled(true);
            if (btnEndVotingNow != null) btnEndVotingNow.setEnabled(true);
        }
    }

    private String formatTimeMs(long millis) {
        if (millis < 0) millis = 0;
        long seconds = (millis / 1000) % 60;
        long minutes = (millis / (1000 * 60)) % 60;
        long hours = millis / (1000 * 60 * 60);
        if (hours > 0) {
            return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format(Locale.US, "%02d:%02d", minutes, seconds);
        }
    }

    private void extendElectionTime(long deltaMs) {
        String dept = getSanitizedDepartment(getTeacherDepartment());
        String sec = getSanitizedSection(getTeacherAssignedSection());
        if (sec.equals("--")) {
            showSnackBar("No section assigned.", true);
            return;
        }
        DatabaseReference ref = dbRef.child("voting_controls").child(dept).child(sec);
        Map<String, Object> updates = new HashMap<>();

        if (!currentIsGateOpen) {
            long newExpiry = System.currentTimeMillis() + deltaMs;
            updates.put("isGateOpen", true);
            updates.put("isOpen", true);
            updates.put("isPaused", false);
            updates.put("expiryTime", newExpiry);
            updates.put("remainingTimeMs", 0L);
        } else if (currentIsPaused) {
            long newRemaining = currentRemainingMs + deltaMs;
            long newExpiry = System.currentTimeMillis() + newRemaining;
            updates.put("remainingTimeMs", newRemaining);
            updates.put("expiryTime", newExpiry);
        } else {
            long base = Math.max(System.currentTimeMillis(), currentExpiryTime);
            long newExpiry = base + deltaMs;
            updates.put("expiryTime", newExpiry);
            updates.put("isGateOpen", true);
            updates.put("isOpen", true);
        }

        ref.updateChildren(updates).addOnSuccessListener(aVoid -> {
            int mins = (int) (deltaMs / 60000);
            showSnackBar("Time extended by +" + mins + " minutes.", false);
            logAdminAction("Extended election time by +" + mins + " minutes for Sec " + getTeacherAssignedSection());
            broadcastNotificationToStudents(
                "⏱️ Election Time Extended",
                "Admin extended election time by +" + mins + " minutes for Section " + getTeacherAssignedSection() + ".",
                "TIMER_UPDATE"
            );
        });
    }

    private void reduceElectionTime(long deltaMs) {
        String dept = getSanitizedDepartment(getTeacherDepartment());
        String sec = getSanitizedSection(getTeacherAssignedSection());
        if (sec.equals("--")) {
            showSnackBar("No section assigned.", true);
            return;
        }
        DatabaseReference ref = dbRef.child("voting_controls").child(dept).child(sec);
        Map<String, Object> updates = new HashMap<>();

        if (currentIsPaused) {
            long newRemaining = Math.max(0, currentRemainingMs - deltaMs);
            if (newRemaining <= 0) {
                updates.put("isGateOpen", false);
                updates.put("isOpen", false);
                updates.put("isPaused", false);
                updates.put("expiryTime", System.currentTimeMillis());
                updates.put("remainingTimeMs", 0L);
            } else {
                updates.put("remainingTimeMs", newRemaining);
                updates.put("expiryTime", System.currentTimeMillis() + newRemaining);
            }
        } else if (currentIsGateOpen) {
            long newExpiry = currentExpiryTime - deltaMs;
            if (newExpiry <= System.currentTimeMillis()) {
                updates.put("isGateOpen", false);
                updates.put("isOpen", false);
                updates.put("isPaused", false);
                updates.put("expiryTime", System.currentTimeMillis());
                updates.put("remainingTimeMs", 0L);
            } else {
                updates.put("expiryTime", newExpiry);
            }
        }

        ref.updateChildren(updates).addOnSuccessListener(aVoid -> {
            showSnackBar("Time reduced by -5 minutes.", false);
            logAdminAction("Reduced election time by -5 minutes for Sec " + getTeacherAssignedSection());
            broadcastNotificationToStudents(
                "⏱️ Election Time Adjusted",
                "Admin reduced election time by -5 minutes for Section " + getTeacherAssignedSection() + ".",
                "TIMER_UPDATE"
            );
        });
    }

    private void togglePauseResume() {
        String dept = getSanitizedDepartment(getTeacherDepartment());
        String sec = getSanitizedSection(getTeacherAssignedSection());
        if (sec.equals("--")) {
            showSnackBar("No section assigned.", true);
            return;
        }
        DatabaseReference ref = dbRef.child("voting_controls").child(dept).child(sec);
        Map<String, Object> updates = new HashMap<>();

        if (!currentIsPaused) {
            long rem = Math.max(0, currentExpiryTime - System.currentTimeMillis());
            updates.put("isPaused", true);
            updates.put("remainingTimeMs", rem);
            ref.updateChildren(updates).addOnSuccessListener(aVoid -> {
                showSnackBar("Voting is temporarily paused.", false);
                logAdminAction("Paused live voting for Sec " + getTeacherAssignedSection());
                broadcastNotificationToStudents(
                    "⏸️ Voting Paused",
                    "Voting is temporarily paused by the admin for Section " + getTeacherAssignedSection() + ".",
                    "PAUSE_RESUME"
                );
            });
        } else {
            long rem = currentRemainingMs > 0 ? currentRemainingMs : Math.max(0, currentExpiryTime - System.currentTimeMillis());
            long newExpiry = System.currentTimeMillis() + rem;
            updates.put("isPaused", false);
            updates.put("expiryTime", newExpiry);
            updates.put("remainingTimeMs", 0L);
            updates.put("isGateOpen", true);
            updates.put("isOpen", true);
            ref.updateChildren(updates).addOnSuccessListener(aVoid -> {
                showSnackBar("Voting resumed.", false);
                logAdminAction("Resumed live voting for Sec " + getTeacherAssignedSection());
                broadcastNotificationToStudents(
                    "▶️ Voting Resumed",
                    "Voting has resumed for Section " + getTeacherAssignedSection() + "! Cast your vote now.",
                    "PAUSE_RESUME"
                );
            });
        }
    }

    private void endVotingNow() {
        String dept = getSanitizedDepartment(getTeacherDepartment());
        String sec = getSanitizedSection(getTeacherAssignedSection());
        if (sec.equals("--")) {
            showSnackBar("No section assigned.", true);
            return;
        }

        new MaterialAlertDialogBuilder(this)
            .setTitle("⏹️ End Voting Now?")
            .setMessage("Are you sure you want to manually end voting now for Section " + getTeacherAssignedSection() + "? This will immediately close election access for all students.")
            .setPositiveButton("Yes, End Now", (dialog, which) -> {
                DatabaseReference ref = dbRef.child("voting_controls").child(dept).child(sec);
                Map<String, Object> updates = new HashMap<>();
                updates.put("isGateOpen", false);
                updates.put("isOpen", false);
                updates.put("isPaused", false);
                updates.put("expiryTime", System.currentTimeMillis());
                updates.put("remainingTimeMs", 0L);

                ref.updateChildren(updates).addOnSuccessListener(aVoid -> {
                    showSnackBar("Voting ended manually.", false);
                    logAdminAction("Manually ended voting for Sec " + getTeacherAssignedSection());
                    broadcastNotificationToStudents(
                        "🔴 Voting Closed",
                        "Voting for Section " + getTeacherAssignedSection() + " has been ended by the admin.",
                        "ELECTION_ENDED"
                    );
                });
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private boolean checkUserAuthenticationAndRole() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null || currentUser.isAnonymous() || !sessionManager.isLoggedIn()) {
            // TEMPORARY DEV TESTING BYPASS: Auto-initialize test session if launched directly from Manifest
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
        }

        String role = sessionManager.getUserRole();
        if (!"Teacher/Admin".equals(role) && !"Teacher".equalsIgnoreCase(role)) {
            Toast.makeText(this, "Unauthorized access: Teacher privileges required.", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(this, StudentDashboardActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return false;
        }

        return true;
    }

    private void redirectToLogin(String message) {
        FirebaseAuth.getInstance().signOut();
        sessionManager.logoutUser();
        if (message != null && !message.isEmpty()) {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        }
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
