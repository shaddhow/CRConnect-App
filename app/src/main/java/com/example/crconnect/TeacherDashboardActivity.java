package com.example.crconnect;

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
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;

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
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class TeacherDashboardActivity extends AppCompatActivity {

    private MaterialSwitch switchMasterVoting;
    private TextInputEditText etCandidateName;
    private MaterialButton btnAddCandidate, btnRunOff, btnResetVotes, btnLogout, btnClearName, btnExportPdf;
    private ImageView imgUserProfile;
    private ActivityResultLauncher<Intent> profileImagePickerLauncher;
    private TextView tvHeaderTitle, tvHeaderSubtitle, tvTotalCandidates;
    private TextView tvStatTotalVotes, tvStatTotalVoters, tvStatTurnout;
    private LinearLayout containerAdminCandidates;
    private DatabaseReference dbRef;
    private SessionManager sessionManager;

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

        tvHeaderTitle.setText("Admin Console");

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
                    if (name == null) name = sessionManager.getUserName();
                    if (dept == null) dept = sessionManager.getUserDept();
                    tvHeaderSubtitle.setText(name + " (" + dept + ")");

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
                                if (name == null) name = sessionManager.getUserName();
                                if (dept == null) dept = sessionManager.getUserDept();
                                tvHeaderSubtitle.setText(name + " (" + dept + ")");

                                if (profileImg != null && !profileImg.isEmpty()) {
                                    sessionManager.setProfileImageUrl(profileImg);
                                    Glide.with(TeacherDashboardActivity.this).load(profileImg).placeholder(R.drawable.ic_app_main).error(R.drawable.ic_app_main).circleCrop().into(imgUserProfile);
                                }
                            } else {
                                redirectToLogin("Teacher profile not found in database. Access denied.");
                            }
                        });
                } else {
                    redirectToLogin("Teacher profile not found in database. Access denied.");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                redirectToLogin("Database error verifying teacher account: " + error.getMessage());
            }
        };

        if (!uid.isEmpty()) {
            dbRef.child("users").child(uid).addListenerForSingleValueEvent(teacherListener);
        } else if (!sanitizedTeacherId.isEmpty()) {
            dbRef.child("users").child(sanitizedTeacherId).addListenerForSingleValueEvent(teacherListener);
        } else {
            redirectToLogin("Teacher profile ID not found.");
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

            String notifTitle = isChecked ? "🟢 Live Voting is OPEN" : "🔴 Live Voting is LOCKED";
            String notifBody = isChecked ?
                    "Election admin " + sessionManager.getUserName() + " has opened live voting! Cast your vote now." :
                    "Live voting has been locked by election admin " + sessionManager.getUserName() + ".";
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
        dbRef.child("candidates").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                try {
                    if (isFinishing() || isDestroyed()) return;
                    tvTotalCandidates.setText("Total: " + snapshot.getChildrenCount());
                    containerAdminCandidates.removeAllViews();
                    
                    int totalVotes = 0;
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        int vts = getSafeVotes(ds);
                        totalVotes += vts;
                        String imageUrl = ds.child("imageUrl").getValue(String.class);
                        String cName = ds.child("name").getValue(String.class);
                        if (cName == null) cName = "Candidate";
                        addAdminRow(ds.getKey(), cName, vts, imageUrl);
                    }
                    tvStatTotalVotes.setText(String.valueOf(totalVotes));
                } catch (Exception e) {
                    Log.e("TeacherDashboard", "Error loading candidates list", e);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("TeacherDashboard", "Candidates query cancelled: " + error.getMessage());
            }
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

        // Safety Confirmation Bottom Sheet for Reset All (Material Design 3 & Glassmorphism)
        btnResetVotes.setOnClickListener(v -> {
            BottomSheetDialog sheet = new BottomSheetDialog(this);
            LinearLayout view = new LinearLayout(this);
            view.setOrientation(LinearLayout.VERTICAL);
            view.setPadding(48, 48, 48, 48);
            view.setBackgroundColor(Color.parseColor("#171A29"));

            TextView title = new TextView(this);
            title.setText("⚠️ Reset All Election Data?");
            title.setTextSize(20f);
            title.setTypeface(null, Typeface.BOLD);
            title.setTextColor(Color.parseColor("#EF4444"));

            TextView msg = new TextView(this);
            msg.setText("This will permanently delete all published candidates, votes, and voter security logs. This action cannot be undone.");
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
                Map<String, Object> resetMap = new HashMap<>();
                resetMap.put("candidates", null);
                resetMap.put("voters", null);
                dbRef.updateChildren(resetMap).addOnSuccessListener(unused -> {
                    showSnackBar("Election data successfully reset.", false);
                    logAdminAction("Reset All Election Votes & Candidates");
                    broadcastNotificationToStudents(
                        "⚠️ Election Portal Reset",
                        "Election portal data has been reset by the teacher.",
                        "RESET"
                    );
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
            BottomSheetDialog sheet = new BottomSheetDialog(this);
            LinearLayout view = new LinearLayout(this);
            view.setOrientation(LinearLayout.VERTICAL);
            view.setPadding(48, 48, 48, 48);
            view.setBackgroundColor(Color.parseColor("#171A29"));

            TextView title = new TextView(this);
            title.setText("⚡ Initialize Run-Off / Tie-Breaker?");
            title.setTextSize(20f);
            title.setTypeface(null, Typeface.BOLD);
            title.setTextColor(Color.parseColor("#8B5CF6"));

            TextView msg = new TextView(this);
            msg.setText("This will reset all vote counts to 0 while keeping the candidate list for a run-off election round. All voter security locks will be cleared. Proceed?");
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
                            updates.put(ds.getKey() + "/votes", 0);
                        }
                        dbRef.child("candidates").updateChildren(updates);
                        dbRef.child("voters").removeValue();
                        showSnackBar("Run-off election round initialized!", false);
                        logAdminAction("Initialized Run-Off Election Round");
                        broadcastNotificationToStudents(
                            "⚡ Run-Off Round Initialized",
                            "A run-off election round has been initiated. All vote locks are cleared!",
                            "RUN_OFF"
                        );
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

    private void saveCandidateToDatabase(String name) {
        String cid = dbRef.child("candidates").push().getKey();
        if (cid != null) {
            Map<String, Object> map = new HashMap<>();
            map.put("name", name);
            map.put("votes", 0);
            map.put("imageUrl", "");
            map.put("dept", "Department of CSE, BUBT");
            map.put("manifesto", "Committed to student welfare, transparent CR communication, and academic support sessions.");

            etCandidateName.setText("");
            etCandidateName.clearFocus();
            btnAddCandidate.setEnabled(true);

            dbRef.child("candidates").child(cid).setValue(map)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Successfully added to student portal!", Toast.LENGTH_SHORT).show();
                    logAdminAction("Added New Candidate: " + name);
                    broadcastNotificationToStudents(
                        "📢 New Candidate Published",
                        name + " has been added to the student election portal. Check out their manifesto!",
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
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(28, 22, 28, 22);
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
                    dbRef.child("candidates").child(candidateId).removeValue()
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

    private void broadcastNotificationToStudents(String title, String body, String actionType) {
        String notifId = dbRef.child("notifications_queue").push().getKey();
        Map<String, Object> notifMap = new HashMap<>();
        notifMap.put("title", title);
        notifMap.put("body", body);
        notifMap.put("topic", "all_students");
        notifMap.put("actionType", actionType);
        notifMap.put("sender", sessionManager.getUserName());
        notifMap.put("timestamp", ServerValue.TIMESTAMP);

        if (notifId != null) {
            dbRef.child("notifications_queue").child(notifId).setValue(notifMap);
        }
        dbRef.child("broadcast_notifications").setValue(notifMap);
    }

    private void exportElectionResultsToPdf() {
        Toast.makeText(this, "Generating Election PDF Report...", Toast.LENGTH_SHORT).show();

        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                DataSnapshot candidatesSnap = snapshot.child("candidates");
                DataSnapshot votersSnap = snapshot.child("voters");
                Boolean gateStatus = snapshot.child("master_voting_enabled").getValue(Boolean.class);

                int totalVotes = 0;
                List<PdfReportGenerator.CandidateResult> rawList = new ArrayList<>();

                for (DataSnapshot ds : candidatesSnap.getChildren()) {
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

                int totalVoters = (int) votersSnap.getChildrenCount();
                int estimatedTotalStudents = 50;
                int turnoutInt = (int) Math.min(100, (totalVoters * 100) / estimatedTotalStudents);
                String turnoutStr = turnoutInt + "%";

                String teacherName = sessionManager.getUserName();
                String teacherDept = sessionManager.getUserDept();

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
                    logAdminAction("Exported Election Results to PDF Report");

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
    }

    private boolean checkUserAuthenticationAndRole() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null || currentUser.isAnonymous()) {
            redirectToLogin("Authentication required. Please log in.");
            return false;
        }

        // TEMPORARY TESTING BYPASS: Email verification and domain check disabled
        /*
        if (!currentUser.isEmailVerified()) {
            redirectToLogin("Email not verified. Please verify your BUBT email first.");
            return false;
        }

        String email = currentUser.getEmail();
        if (email == null || (!email.toLowerCase().endsWith("@bubt.edu.bd") && !email.toLowerCase().endsWith("@cse.bubt.edu.bd"))) {
            redirectToLogin("Only official BUBT emails (@bubt.edu.bd or @cse.bubt.edu.bd) are authorized.");
            return false;
        }
        */

        if (!sessionManager.isLoggedIn()) {
            redirectToLogin("Session expired. Please log in again.");
            return false;
        }

        String role = sessionManager.getUserRole();
        if (!"Teacher/Admin".equals(role)) {
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
