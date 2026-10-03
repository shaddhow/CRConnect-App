package com.example.crconnect;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
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

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StudentDashboardActivity extends AppCompatActivity {

    private TextView tvVotingStatus, tvHeaderTitle, tvHeaderSubtitle;
    private TextView tvStudentName, tvStudentDetailsID, tvStudentIntake, tvStudentSection, tvStudentDept;
    private ImageView imgUserProfile;
    private MaterialButton btnLogout;
    private LinearLayout containerCandidates;
    private DatabaseReference dbRef;
    private SessionManager sessionManager;
    
    private boolean isVotingEnabled = false;
    private boolean hasAlreadyVoted = false;
    private DataSnapshot allCandidatesSnapshot;
    private String sanitizedStudentId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        sessionManager = new SessionManager(this);
        
        String rawId = sessionManager.getUserID();
        sanitizedStudentId = rawId != null ? rawId.replace(".", "_").replace("@", "_").replace("#", "_").replace("$", "_").replace("[", "_").replace("]", "_") : "guest_user";

        // --- TRUE FULL SCREEN (GOOGLE STYLE) ---
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
        
        // Force the gradient background to the entire window (Removes white bars)
        getWindow().setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.bg_gradient));

        setContentView(R.layout.activity_student_dashboard);

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
                Intent intent = new Intent(StudentDashboardActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
        });

        dbRef = FirebaseDatabase.getInstance("https://crconnect-58521-default-rtdb.firebaseio.com").getReference("crconnect_db");

        // Bind UI
        imgUserProfile = findViewById(R.id.imgUserProfile);
        tvHeaderTitle = findViewById(R.id.tvHeaderTitle);
        tvHeaderSubtitle = findViewById(R.id.tvHeaderSubtitle);
        btnLogout = findViewById(R.id.btnLogout);
        tvVotingStatus = findViewById(R.id.tvVotingStatus);
        containerCandidates = findViewById(R.id.containerCandidates);
        
        // Profile Fields
        tvStudentName = findViewById(R.id.tvStudentName);
        tvStudentDetailsID = findViewById(R.id.tvStudentDetailsID);
        tvStudentIntake = findViewById(R.id.tvStudentIntake);
        tvStudentSection = findViewById(R.id.tvStudentSection);
        tvStudentDept = findViewById(R.id.tvStudentDept);

        tvHeaderTitle.setText("Election Portal");
        displayStudentProfile();

        Glide.with(this).load(R.drawable.ic_app_main).circleCrop().into(imgUserProfile);

        btnLogout.setOnClickListener(v -> {
            sessionManager.logoutUser();
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        // Check if student has already voted (Anti-Dual-Voting Security)
        dbRef.child("voters").child(sanitizedStudentId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                hasAlreadyVoted = snapshot.exists();
                updateStatusText();
                refreshList();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        // Live Sync Voting Enabled status
        dbRef.child("master_voting_enabled").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean status = snapshot.getValue(Boolean.class);
                isVotingEnabled = (status != null && status);
                updateStatusText();
                refreshList();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(StudentDashboardActivity.this, "DB Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        dbRef.child("candidates").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allCandidatesSnapshot = snapshot;
                refreshList();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(StudentDashboardActivity.this, "DB Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayStudentProfile() {
        // Display local cached values from SessionManager first
        tvStudentName.setText(sessionManager.getUserName());
        tvStudentDetailsID.setText(sessionManager.getUserID());
        tvStudentIntake.setText(sessionManager.getUserIntake());
        tvStudentSection.setText(sessionManager.getUserSection());
        tvStudentDept.setText(sessionManager.getUserDept());
        tvHeaderSubtitle.setText("Dashboard | " + sessionManager.getUserDept());

        // Fetch fresh profile details from Firebase Realtime Database & Firestore
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String uid = currentUser != null ? currentUser.getUid() : "";

        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String name = snapshot.child("name").getValue(String.class);
                    String id = snapshot.child("id").getValue(String.class);
                    String intake = snapshot.child("intake").getValue(String.class);
                    String section = snapshot.child("section").getValue(String.class);
                    String dept = snapshot.child("dept").getValue(String.class);
                    String email = snapshot.child("email").getValue(String.class);
                    String role = snapshot.child("role").getValue(String.class);

                    if (name != null) tvStudentName.setText(name);
                    if (id != null) tvStudentDetailsID.setText(id);
                    if (intake != null) tvStudentIntake.setText(intake);
                    if (section != null) tvStudentSection.setText(section);
                    if (dept != null) {
                        tvStudentDept.setText(dept);
                        tvHeaderSubtitle.setText("Dashboard | " + dept);
                    }

                    sessionManager.createLoginSession(
                        name != null ? name : sessionManager.getUserName(),
                        email != null ? email : sessionManager.getUserEmail(),
                        id != null ? id : sessionManager.getUserID(),
                        role != null ? role : sessionManager.getUserRole(),
                        section != null ? section : sessionManager.getUserSection(),
                        intake != null ? intake : sessionManager.getUserIntake(),
                        dept != null ? dept : sessionManager.getUserDept()
                    );
                } else if (!uid.isEmpty()) {
                    FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(uid)
                        .get()
                        .addOnSuccessListener(doc -> {
                            if (doc.exists()) {
                                String name = doc.getString("name");
                                String id = doc.getString("id");
                                String intake = doc.getString("intake");
                                String section = doc.getString("section");
                                String dept = doc.getString("dept");
                                String email = doc.getString("email");
                                String role = doc.getString("role");

                                if (name != null) tvStudentName.setText(name);
                                if (id != null) tvStudentDetailsID.setText(id);
                                if (intake != null) tvStudentIntake.setText(intake);
                                if (section != null) tvStudentSection.setText(section);
                                if (dept != null) {
                                    tvStudentDept.setText(dept);
                                    tvHeaderSubtitle.setText("Dashboard | " + dept);
                                }

                                sessionManager.createLoginSession(
                                    name != null ? name : sessionManager.getUserName(),
                                    email != null ? email : sessionManager.getUserEmail(),
                                    id != null ? id : sessionManager.getUserID(),
                                    role != null ? role : sessionManager.getUserRole(),
                                    section != null ? section : sessionManager.getUserSection(),
                                    intake != null ? intake : sessionManager.getUserIntake(),
                                    dept != null ? dept : sessionManager.getUserDept()
                                );
                            }
                        });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };

        if (!uid.isEmpty()) {
            dbRef.child("users").child(uid).addListenerForSingleValueEvent(listener);
        } else if (sanitizedStudentId != null && !sanitizedStudentId.isEmpty()) {
            dbRef.child("users").child(sanitizedStudentId).addListenerForSingleValueEvent(listener);
        }
    }

    private void updateStatusText() {
        if (hasAlreadyVoted) {
            tvVotingStatus.setText("✓ Vote Secured & Recorded");
            tvVotingStatus.setTextColor(Color.parseColor("#10B981"));
        } else if (!isVotingEnabled) {
            tvVotingStatus.setText("Voting is LOCKED by Teacher");
            tvVotingStatus.setTextColor(Color.parseColor("#EF4444"));
        } else {
            tvVotingStatus.setText("Live Voting is OPEN");
            tvVotingStatus.setTextColor(Color.parseColor("#10B981"));
        }
    }

    private void refreshList() {
        if (allCandidatesSnapshot == null || containerCandidates == null) return;
        containerCandidates.removeAllViews();

        // Convert snapshot to list for sorting by votes descending (Live Leaderboard)
        List<DataSnapshot> candidateList = new ArrayList<>();
        int maxVotes = 1;
        for (DataSnapshot item : allCandidatesSnapshot.getChildren()) {
            candidateList.add(item);
            Integer vts = item.child("votes").getValue(Integer.class);
            if (vts != null && vts > maxVotes) {
                maxVotes = vts;
            }
        }

        candidateList.sort((d1, d2) -> {
            Integer v1 = d1.child("votes").getValue(Integer.class);
            Integer v2 = d2.child("votes").getValue(Integer.class);
            int votes1 = v1 != null ? v1 : 0;
            int votes2 = v2 != null ? v2 : 0;
            return Integer.compare(votes2, votes1); // Descending order
        });

        int rank = 1;
        for (DataSnapshot item : candidateList) {
            String name = item.child("name").getValue(String.class);
            String dept = item.child("dept").getValue(String.class);
            if (dept == null || dept.isEmpty()) dept = "Department of CSE, BUBT";
            String manifesto = item.child("manifesto").getValue(String.class);
            if (manifesto == null || manifesto.isEmpty()) manifesto = "Committed to student welfare, transparent CR communication, and academic excellence.";
            Integer votes = item.child("votes").getValue(Integer.class);

            addCandidateCard(item.getKey(), name, dept, manifesto, votes, rank, maxVotes);
            rank++;
        }
    }

    private void addCandidateCard(String id, String name, String dept, String manifesto, Integer vts, int rank, int maxVotes) {
        int votes = vts != null ? vts : 0;
        MaterialCardView card = new MaterialCardView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, 0, 0, 16);
        card.setLayoutParams(params);
        card.setRadius(24);
        card.setCardElevation(8);
        card.setCardBackgroundColor(Color.parseColor("#171A29"));
        card.setStrokeWidth(2);
        card.setClickable(true);
        card.setFocusable(true);
        card.setOnClickListener(v -> showCandidateManifestoDialog(name, dept, manifesto));
        
        // Highlight Top 3 with smooth Neon Cyan and Soft Electric Purple borders
        if (rank == 1) card.setStrokeColor(Color.parseColor("#38BDF8")); // Neon Cyan
        else if (rank == 2) card.setStrokeColor(Color.parseColor("#8B5CF6")); // Soft Electric Purple
        else if (rank == 3) card.setStrokeColor(Color.parseColor("#6366F1")); // Indigo
        else card.setStrokeColor(Color.parseColor("#334155"));

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(28, 24, 28, 24);

        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout textCol = new LinearLayout(this);
        textCol.setOrientation(LinearLayout.VERTICAL);
        textCol.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));

        // Rank / Badge title
        String rankTitle = name;
        if (rank == 1) rankTitle = "🥇 " + name + " (1st Place)";
        else if (rank == 2) rankTitle = "🥈 " + name + " (2nd Place)";
        else if (rank == 3) rankTitle = "🥉 " + name + " (3rd Place)";

        TextView tvName = new TextView(this);
        tvName.setText(rankTitle);
        tvName.setTextSize(17f);
        tvName.setTypeface(null, Typeface.BOLD);
        tvName.setTextColor(Color.parseColor("#F8FAFC"));

        TextView tvVotes = new TextView(this);
        tvVotes.setText("Votes: " + votes + "  •  Tap for Manifesto");
        tvVotes.setTextSize(14f);
        tvVotes.setTextColor(Color.parseColor("#38BDF8"));
        tvVotes.setPadding(0, 4, 0, 8);

        textCol.addView(tvName);
        textCol.addView(tvVotes);

        MaterialButton btnVote = new MaterialButton(this);
        if (hasAlreadyVoted) {
            btnVote.setText("Voted");
            btnVote.setEnabled(false);
            btnVote.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#10B981")));
        } else {
            btnVote.setText("Vote");
            btnVote.setEnabled(isVotingEnabled);
            btnVote.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#6366F1")));
        }
        btnVote.setCornerRadius(16);

        btnVote.setOnClickListener(v -> {
            v.setPressed(true);
            if (hasAlreadyVoted) {
                Toast.makeText(this, "Security Alert: Dual voting is strictly blocked!", Toast.LENGTH_LONG).show();
                return;
            }

            if (id != null) {
                dbRef.child("voters").child(sanitizedStudentId).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            hasAlreadyVoted = true;
                            Toast.makeText(StudentDashboardActivity.this, "Security Alert: You have already cast your vote!", Toast.LENGTH_LONG).show();
                            refreshList();
                        } else {
                            Map<String, Object> updates = new HashMap<>();
                            updates.put("voters/" + sanitizedStudentId, id);
                            updates.put("candidates/" + id + "/votes", votes + 1);

                            dbRef.updateChildren(updates).addOnSuccessListener(aVoid -> {
                                hasAlreadyVoted = true;
                                Toast.makeText(StudentDashboardActivity.this, "Vote successfully recorded & secured!", Toast.LENGTH_SHORT).show();
                                refreshList();
                            }).addOnFailureListener(e -> {
                                Toast.makeText(StudentDashboardActivity.this, "Transaction failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
            }
        });

        topRow.addView(textCol);
        topRow.addView(btnVote);
        row.addView(topRow);

        // Graphical Vote Count Progress Bar
        ProgressBar progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        LinearLayout.LayoutParams pbParams = new LinearLayout.LayoutParams(-1, 20);
        pbParams.setMargins(0, 12, 0, 0);
        progressBar.setLayoutParams(pbParams);
        progressBar.setMax(Math.max(maxVotes, 10));
        progressBar.setProgress(votes);
        progressBar.setProgressTintList(ColorStateList.valueOf(rank == 1 ? Color.parseColor("#38BDF8") : Color.parseColor("#8B5CF6")));
        row.addView(progressBar);

        card.addView(row);
        containerCandidates.addView(card);
    }

    private void showCandidateManifestoDialog(String name, String dept, String manifesto) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        
        LinearLayout view = new LinearLayout(this);
        view.setOrientation(LinearLayout.VERTICAL);
        view.setPadding(56, 56, 56, 56);
        view.setBackgroundColor(Color.parseColor("#171A29"));

        TextView tvName = new TextView(this);
        tvName.setText(name);
        tvName.setTextSize(22f);
        tvName.setTypeface(null, Typeface.BOLD);
        tvName.setTextColor(Color.parseColor("#F8FAFC"));

        TextView tvDept = new TextView(this);
        tvDept.setText(dept);
        tvDept.setTextSize(15f);
        tvDept.setTextColor(Color.parseColor("#38BDF8"));
        tvDept.setPadding(0, 8, 0, 24);

        TextView tvManifestoTitle = new TextView(this);
        tvManifestoTitle.setText("Election Manifesto & Vision:");
        tvManifestoTitle.setTextSize(16f);
        tvManifestoTitle.setTypeface(null, Typeface.BOLD);
        tvManifestoTitle.setTextColor(Color.parseColor("#CBD5E1"));

        TextView tvManifestoBody = new TextView(this);
        tvManifestoBody.setText(manifesto);
        tvManifestoBody.setTextSize(14f);
        tvManifestoBody.setTextColor(Color.parseColor("#94A3B8"));
        tvManifestoBody.setPadding(0, 8, 0, 32);

        view.addView(tvName);
        view.addView(tvDept);
        view.addView(tvManifestoTitle);
        view.addView(tvManifestoBody);

        bottomSheetDialog.setContentView(view);
        bottomSheetDialog.show();
    }
}
