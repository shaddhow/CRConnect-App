package com.example.crconnect;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
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

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StudentDashboardActivity extends AppCompatActivity {

    private TextView tvVotingStatus, tvHeaderTitle, tvHeaderSubtitle, tvStudentCountdown;
    private TextView tvStudentName, tvStudentDetailsID, tvStudentIntake, tvStudentSection, tvStudentDept;
    private ImageView imgUserProfile;
    private MaterialButton btnLogout;
    private LinearLayout containerCandidates;
    private DatabaseReference dbRef;
    private SessionManager sessionManager;
    private ActivityResultLauncher<Intent> profileImagePickerLauncher;
    
    private boolean isVotingEnabled = false;
    private boolean hasAlreadyVoted = false;
    private DataSnapshot allCandidatesSnapshot;
    private String sanitizedStudentId;

    private String currentSectionId = "--";
    private DatabaseReference gateRef, voterRef, candidateRef;
    private ValueEventListener gateEventListener, voterEventListener, candidateEventListener;

    // Live Timer State & Handler
    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable;
    private long studentExpiryTime = 0;
    private boolean studentIsPaused = false;
    private boolean studentIsGateOpen = false;
    private long studentRemainingMs = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        sessionManager = new SessionManager(this);
        if (!checkUserAuthenticationAndRole()) {
            return;
        }
        
        String rawId = sessionManager.getUserID();
        sanitizedStudentId = rawId != null && !rawId.isEmpty() ? rawId.replace(".", "_").replace("@", "_").replace("#", "_").replace("$", "_").replace("[", "_").replace("]", "_") : "";

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
        tvStudentCountdown = findViewById(R.id.tvStudentCountdown);
        containerCandidates = findViewById(R.id.containerCandidates);
        
        // Profile Fields
        tvStudentName = findViewById(R.id.tvStudentName);
        tvStudentDetailsID = findViewById(R.id.tvStudentDetailsID);
        tvStudentIntake = findViewById(R.id.tvStudentIntake);
        tvStudentSection = findViewById(R.id.tvStudentSection);
        tvStudentDept = findViewById(R.id.tvStudentDept);

        tvHeaderTitle.setText("Election Portal");
        displayStudentProfile();

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

        // FCM Topic Subscription & Notification Permission Setup
        FirebaseMessaging.getInstance().subscribeToTopic("all_students")
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Log.d("StudentDashboard", "Subscribed to FCM topic: all_students");
                }
            });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        // Live Listener for Admin Broadcast Notifications
        dbRef.child("broadcast_notifications").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String title = snapshot.child("title").getValue(String.class);
                    String body = snapshot.child("body").getValue(String.class);
                    Long timestamp = snapshot.child("timestamp").getValue(Long.class);
                    
                    if (title != null && body != null && timestamp != null && (System.currentTimeMillis() - timestamp < 15000)) {
                        showNotificationSnackBar(title + ": " + body);
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        // Initialize Section-Specific Firebase Listeners (/voting_controls/{deptId}/{sectionId}/isGateOpen)
        setupSectionListeners(sessionManager.getUserDept(), sessionManager.getUserSection());
    }

    private String getSanitizedSection(String sec) {
        if (sec == null || sec.trim().isEmpty() || sec.equals("--")) return "--";
        return sec.trim().replace("/", "_").replace(" ", "_").replace(".", "_").replace("#", "_").replace("$", "_").replace("[", "_").replace("]", "_");
    }

    private String getSanitizedDepartment(String dept) {
        if (dept == null || dept.trim().isEmpty() || dept.equals("--")) return "Department_of_CSE_BUBT";
        return dept.trim().replace("/", "_").replace(" ", "_").replace(".", "_").replace("#", "_").replace("$", "_").replace("[", "_").replace("]", "_").replace("&", "_");
    }

    private synchronized void setupSectionListeners(String rawDept, String rawSection) {
        String deptId = getSanitizedDepartment(rawDept);
        String secId = getSanitizedSection(rawSection);
        String comboKey = deptId + "_" + secId;
        if (comboKey.equals(currentSectionId) && gateEventListener != null) {
            return;
        }

        if (gateRef != null && gateEventListener != null) {
            gateRef.removeEventListener(gateEventListener);
        }
        if (voterRef != null && voterEventListener != null) {
            voterRef.removeEventListener(voterEventListener);
        }
        if (candidateRef != null && candidateEventListener != null) {
            candidateRef.removeEventListener(candidateEventListener);
        }

        currentSectionId = comboKey;

        // 1. Section Voting Status Gate (/voting_controls/{deptId}/{sectionId})
        if (!secId.equals("--")) {
            gateRef = dbRef.child("voting_controls").child(deptId).child(secId);
        } else {
            gateRef = dbRef.child("master_voting_enabled");
        }

        gateEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Boolean gateOpen = snapshot.child("isGateOpen").getValue(Boolean.class);
                    if (gateOpen == null) gateOpen = snapshot.child("isOpen").getValue(Boolean.class);
                    if (gateOpen == null && snapshot.getValue() instanceof Boolean) {
                        gateOpen = (Boolean) snapshot.getValue();
                    }
                    Boolean paused = snapshot.child("isPaused").getValue(Boolean.class);
                    Long expiry = snapshot.child("expiryTime").getValue(Long.class);
                    Long remMs = snapshot.child("remainingTimeMs").getValue(Long.class);

                    studentIsGateOpen = (gateOpen != null && gateOpen);
                    studentIsPaused = (paused != null && paused);
                    studentExpiryTime = (expiry != null) ? expiry : 0L;
                    studentRemainingMs = (remMs != null) ? remMs : 0L;

                    updateStudentTimerAndStatusUI();
                } else {
                    // Fallback to master_voting_enabled
                    dbRef.child("master_voting_enabled").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snap) {
                            Boolean globalStatus = snap.getValue(Boolean.class);
                            studentIsGateOpen = (globalStatus != null && globalStatus);
                            studentIsPaused = false;
                            studentExpiryTime = 0L;
                            studentRemainingMs = 0L;
                            updateStudentTimerAndStatusUI();
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {}
                    });
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("StudentDashboard", "Gate error: " + error.getMessage());
            }
        };
        gateRef.addValueEventListener(gateEventListener);

        // 2. Anti-Dual Voting Security Lock (/voting_controls/{deptId}/{sectionId}/voters/{sanitizedStudentId})
        if (!secId.equals("--") && sanitizedStudentId != null && !sanitizedStudentId.isEmpty()) {
            voterRef = dbRef.child("voting_controls").child(deptId).child(secId).child("voters").child(sanitizedStudentId);
        } else if (sanitizedStudentId != null && !sanitizedStudentId.isEmpty()) {
            voterRef = dbRef.child("voters").child(sanitizedStudentId);
        }

        if (voterRef != null) {
            voterEventListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        hasAlreadyVoted = true;
                        updateStatusText();
                        refreshList();
                    } else {
                        // Check root voters node fallback
                        dbRef.child("voters").child(sanitizedStudentId).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot rootSnap) {
                                hasAlreadyVoted = rootSnap.exists();
                                updateStatusText();
                                refreshList();
                            }
                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {}
                        });
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("StudentDashboard", "Voter error: " + error.getMessage());
                }
            };
            voterRef.addValueEventListener(voterEventListener);
        }

        // 3. Candidate List & Section Vote Counts (/voting_controls/{deptId}/{sectionId}/candidates)
        if (!secId.equals("--")) {
            candidateRef = dbRef.child("voting_controls").child(deptId).child(secId).child("candidates");
        } else {
            candidateRef = dbRef.child("candidates");
        }

        candidateEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && snapshot.hasChildren()) {
                    allCandidatesSnapshot = snapshot;
                    refreshList();
                } else {
                    // Fallback to root candidates
                    dbRef.child("candidates").addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot rootSnap) {
                            allCandidatesSnapshot = rootSnap;
                            refreshList();
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {}
                    });
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("StudentDashboard", "Candidate error: " + error.getMessage());
            }
        };
        candidateRef.addValueEventListener(candidateEventListener);
    }

    private void displayStudentProfile() {
        // Display local cached values from SessionManager first
        tvStudentName.setText(sessionManager.getUserName());
        tvStudentDetailsID.setText(sessionManager.getUserID());
        tvStudentIntake.setText(sessionManager.getUserIntake());
        tvStudentSection.setText(sessionManager.getUserSection());
        tvStudentDept.setText(sessionManager.getUserDept());
        tvHeaderSubtitle.setText("Dashboard | " + sessionManager.getUserDept());

        String cachedProfileImg = sessionManager.getProfileImageUrl();
        if (cachedProfileImg != null && !cachedProfileImg.isEmpty()) {
            Glide.with(this).load(cachedProfileImg).placeholder(R.drawable.ic_app_main).error(R.drawable.ic_app_main).circleCrop().into(imgUserProfile);
        }

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
                    String assignedSection = snapshot.child("assignedSection").getValue(String.class);
                    String dept = snapshot.child("dept").getValue(String.class);
                    String email = snapshot.child("email").getValue(String.class);
                    String role = snapshot.child("role").getValue(String.class);
                    String profileImg = snapshot.child("profileImageUrl").getValue(String.class);

                    if (name != null) tvStudentName.setText(name);
                    if (id != null) tvStudentDetailsID.setText(id);
                    if (intake != null) tvStudentIntake.setText(intake);
                    if (section != null) tvStudentSection.setText(section);
                    if (dept != null) {
                        tvStudentDept.setText(dept);
                        tvHeaderSubtitle.setText("Dashboard | " + dept);
                    }
                    if (profileImg != null && !profileImg.isEmpty()) {
                        sessionManager.setProfileImageUrl(profileImg);
                        loadProfileImage(StudentDashboardActivity.this, profileImg, imgUserProfile);
                    }

                    sessionManager.createLoginSession(
                        name != null ? name : sessionManager.getUserName(),
                        email != null ? email : sessionManager.getUserEmail(),
                        id != null ? id : sessionManager.getUserID(),
                        role != null ? role : sessionManager.getUserRole(),
                        section != null ? section : sessionManager.getUserSection(),
                        assignedSection != null ? assignedSection : (section != null ? section : sessionManager.getAssignedSection()),
                        intake != null ? intake : sessionManager.getUserIntake(),
                        dept != null ? dept : sessionManager.getUserDept()
                    );
                    setupSectionListeners(dept != null ? dept : sessionManager.getUserDept(), section != null ? section : sessionManager.getUserSection());
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
                                String assignedSection = doc.getString("assignedSection");
                                String dept = doc.getString("dept");
                                String email = doc.getString("email");
                                String role = doc.getString("role");
                                String profileImg = doc.getString("profileImageUrl");

                                if (name != null) tvStudentName.setText(name);
                                if (id != null) tvStudentDetailsID.setText(id);
                                if (intake != null) tvStudentIntake.setText(intake);
                                if (section != null) tvStudentSection.setText(section);
                                if (dept != null) {
                                    tvStudentDept.setText(dept);
                                    tvHeaderSubtitle.setText("Dashboard | " + dept);
                                }
                                if (profileImg != null && !profileImg.isEmpty()) {
                                    sessionManager.setProfileImageUrl(profileImg);
                                    loadProfileImage(StudentDashboardActivity.this, profileImg, imgUserProfile);
                                }

                                sessionManager.createLoginSession(
                                    name != null ? name : sessionManager.getUserName(),
                                    email != null ? email : sessionManager.getUserEmail(),
                                    id != null ? id : sessionManager.getUserID(),
                                    role != null ? role : sessionManager.getUserRole(),
                                    section != null ? section : sessionManager.getUserSection(),
                                    assignedSection != null ? assignedSection : (section != null ? section : sessionManager.getAssignedSection()),
                                    intake != null ? intake : sessionManager.getUserIntake(),
                                    dept != null ? dept : sessionManager.getUserDept()
                                );
                                setupSectionListeners(dept != null ? dept : sessionManager.getUserDept(), section != null ? section : sessionManager.getUserSection());
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

    private void startStudentTimerLoop() {
        stopStudentTimerLoop();
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                updateStudentTimerAndStatusUI();
                timerHandler.postDelayed(this, 1000);
            }
        };
        timerHandler.post(timerRunnable);
    }

    private void stopStudentTimerLoop() {
        if (timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
            timerRunnable = null;
        }
    }

    private void updateStudentTimerAndStatusUI() {
        if (tvVotingStatus == null || tvStudentCountdown == null) return;

        boolean previousEnabled = isVotingEnabled;

        if (hasAlreadyVoted) {
            tvVotingStatus.setText("✓ Vote Secured & Recorded");
            tvVotingStatus.setTextColor(Color.parseColor("#10B981"));
            tvStudentCountdown.setText("Status: Vote Cast");
            tvStudentCountdown.setTextColor(Color.parseColor("#10B981"));
            isVotingEnabled = false;
        } else if (!studentIsGateOpen) {
            tvVotingStatus.setText("Voting is LOCKED by Teacher");
            tvVotingStatus.setTextColor(Color.parseColor("#EF4444"));
            tvStudentCountdown.setText("00:00 - Election Closed");
            tvStudentCountdown.setTextColor(Color.parseColor("#EF4444"));
            isVotingEnabled = false;
        } else if (studentIsPaused) {
            tvVotingStatus.setText("Voting is temporarily paused by the admin");
            tvVotingStatus.setTextColor(Color.parseColor("#F59E0B"));
            long rem = studentRemainingMs > 0 ? studentRemainingMs : Math.max(0, studentExpiryTime - System.currentTimeMillis());
            tvStudentCountdown.setText("PAUSED (" + formatTimeMs(rem) + " remaining)");
            tvStudentCountdown.setTextColor(Color.parseColor("#F59E0B"));
            isVotingEnabled = false;
        } else {
            long millisLeft = studentExpiryTime - System.currentTimeMillis();
            if (millisLeft <= 0) {
                tvVotingStatus.setText("Voting Time Expired");
                tvVotingStatus.setTextColor(Color.parseColor("#EF4444"));
                tvStudentCountdown.setText("00:00 - Time Expired");
                tvStudentCountdown.setTextColor(Color.parseColor("#EF4444"));
                isVotingEnabled = false;
            } else {
                tvVotingStatus.setText("Live Voting is OPEN");
                tvVotingStatus.setTextColor(Color.parseColor("#10B981"));
                tvStudentCountdown.setText("⏱️ " + formatTimeMs(millisLeft) + " remaining");
                tvStudentCountdown.setTextColor(Color.parseColor("#10B981"));
                isVotingEnabled = true;
            }
        }

        if (previousEnabled != isVotingEnabled) {
            refreshList();
        }
    }

    private void updateStatusText() {
        updateStudentTimerAndStatusUI();
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

    private int getSafeVotes(DataSnapshot ds) {
        try {
            if (ds == null || !ds.hasChild("votes")) return 0;
            Object val = ds.child("votes").getValue();
            if (val instanceof Long) return ((Long) val).intValue();
            if (val instanceof Integer) return (Integer) val;
            if (val instanceof String) return Integer.parseInt((String) val);
        } catch (Exception e) {
            Log.e("StudentDashboard", "Error parsing safe votes", e);
        }
        return 0;
    }

    private void refreshList() {
        if (allCandidatesSnapshot == null || containerCandidates == null || isFinishing() || isDestroyed()) return;
        try {
            containerCandidates.removeAllViews();

            List<DataSnapshot> candidateList = new ArrayList<>();
            int maxVotes = 1;
            int totalVotes = 0;
            for (DataSnapshot item : allCandidatesSnapshot.getChildren()) {
                candidateList.add(item);
                int vts = getSafeVotes(item);
                totalVotes += vts;
                if (vts > maxVotes) {
                    maxVotes = vts;
                }
            }

            candidateList.sort((d1, d2) -> {
                int votes1 = getSafeVotes(d1);
                int votes2 = getSafeVotes(d2);
                return Integer.compare(votes2, votes1); // Descending order
            });

            // Display Live Vote Distribution Chart
            if (!candidateList.isEmpty() && totalVotes > 0) {
                updateVoteDistributionChart(candidateList, totalVotes, containerCandidates);
            }

            int rank = 1;
            for (DataSnapshot item : candidateList) {
                String name = item.child("name").getValue(String.class);
                if (name == null) name = "Candidate";
                String dept = item.child("dept").getValue(String.class);
                if (dept == null || dept.isEmpty()) dept = "Department of CSE, BUBT";
                String manifesto = item.child("manifesto").getValue(String.class);
                if (manifesto == null || manifesto.isEmpty()) manifesto = "Committed to student welfare, transparent CR communication, and academic excellence.";
                int votes = getSafeVotes(item);
                String imageUrl = item.child("imageUrl").getValue(String.class);

                addCandidateCard(item.getKey(), name, dept, manifesto, votes, rank, maxVotes, imageUrl);
                rank++;
            }
        } catch (Exception e) {
            Log.e("StudentDashboard", "Error in refreshList", e);
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
            Log.e("StudentDashboard", "Error rendering vote chart", e);
        }
    }

    private void addCandidateCard(String id, String name, String dept, String manifesto, int votes, int rank, int maxVotes, String imageUrl) {
        try {
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

            ImageView imgCandidate = new ImageView(this);
            int size = (int) (52 * getResources().getDisplayMetrics().density);
            LinearLayout.LayoutParams imgLp = new LinearLayout.LayoutParams(size, size);
            imgLp.setMargins(0, 0, 20, 0);
            imgCandidate.setLayoutParams(imgLp);
            imgCandidate.setScaleType(ImageView.ScaleType.CENTER_CROP);

            loadCandidateImage(this, imageUrl, imgCandidate);

            topRow.addView(imgCandidate);

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
            tvVotes.setText("Votes: 0  •  Tap for Manifesto");
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
            } else if (studentIsPaused) {
                btnVote.setText("Paused");
                btnVote.setEnabled(false);
                btnVote.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#F59E0B")));
            } else if (!studentIsGateOpen || (studentExpiryTime > 0 && studentExpiryTime <= System.currentTimeMillis())) {
                btnVote.setText("Closed");
                btnVote.setEnabled(false);
                btnVote.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#64748B")));
            } else {
                btnVote.setText("Vote");
                btnVote.setEnabled(isVotingEnabled);
                btnVote.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#6366F1")));
            }
            btnVote.setCornerRadius(16);

            btnVote.setOnClickListener(v -> {
                try {
                    if (isFinishing() || isDestroyed()) return;
                    v.setPressed(true);
                    if (hasAlreadyVoted) {
                        Toast.makeText(this, "Security Alert: Dual voting is strictly blocked!", Toast.LENGTH_LONG).show();
                        return;
                    }
                    if (studentIsPaused) {
                        Toast.makeText(this, "Voting is temporarily paused by the admin", Toast.LENGTH_LONG).show();
                        return;
                    }
                    if (!studentIsGateOpen || (studentExpiryTime > 0 && studentExpiryTime <= System.currentTimeMillis())) {
                        Toast.makeText(this, "Voting is currently locked or closed.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    if (id != null && !id.isEmpty()) {
                        String vId = (sanitizedStudentId != null && !sanitizedStudentId.isEmpty()) ? sanitizedStudentId : "guest_voter";
                        String userDept = sessionManager.getUserDept();
                        String deptId = getSanitizedDepartment(userDept);
                        String sec = sessionManager.getUserSection();
                        String secId = getSanitizedSection(sec);

                        DatabaseReference voterCheckRef = !secId.equals("--") ?
                                dbRef.child("voting_controls").child(deptId).child(secId).child("voters").child(vId) :
                                dbRef.child("voters").child(vId);

                        voterCheckRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                try {
                                    if (isFinishing() || isDestroyed()) return;
                                    if (snapshot.exists()) {
                                        hasAlreadyVoted = true;
                                        Toast.makeText(StudentDashboardActivity.this, "Security Alert: You have already cast your vote!", Toast.LENGTH_LONG).show();
                                        refreshList();
                                    } else {
                                        // Also check root voters node fallback
                                        dbRef.child("voters").child(vId).addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot rootVoterSnap) {
                                                if (rootVoterSnap.exists()) {
                                                    hasAlreadyVoted = true;
                                                    Toast.makeText(StudentDashboardActivity.this, "Security Alert: You have already cast your vote!", Toast.LENGTH_LONG).show();
                                                    refreshList();
                                                } else {
                                                    Map<String, Object> updates = new HashMap<>();
                                                    updates.put("voters/" + vId, id);
                                                    updates.put("candidates/" + id + "/votes", votes + 1);

                                                    if (!secId.equals("--")) {
                                                        updates.put("voting_controls/" + deptId + "/" + secId + "/voters/" + vId, id);
                                                        updates.put("voting_controls/" + deptId + "/" + secId + "/candidates/" + id + "/votes", votes + 1);
                                                        updates.put("voting_controls/" + deptId + "/" + secId + "/votes/" + id, votes + 1);
                                                    }

                                                    dbRef.updateChildren(updates).addOnSuccessListener(aVoid -> {
                                                        if (isFinishing() || isDestroyed()) return;
                                                        hasAlreadyVoted = true;
                                                        Toast.makeText(StudentDashboardActivity.this, "Vote successfully recorded & secured!", Toast.LENGTH_SHORT).show();
                                                        refreshList();
                                                    }).addOnFailureListener(e -> {
                                                        Log.e("StudentDashboard", "Vote update failure", e);
                                                        if (!isFinishing() && !isDestroyed()) {
                                                            Toast.makeText(StudentDashboardActivity.this, "Transaction failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                        }
                                                    });
                                                }
                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError error) {
                                                Log.e("StudentDashboard", "Root voter check cancelled: " + error.getMessage());
                                            }
                                        });
                                    }
                                } catch (Exception e) {
                                    Log.e("StudentDashboard", "Error processing vote snapshot", e);
                                }
                            }
                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Log.e("StudentDashboard", "Voter check cancelled: " + error.getMessage());
                            }
                        });
                    }
                } catch (Exception e) {
                    Log.e("StudentDashboard", "Error in btnVote click listener", e);
                }
            });

            topRow.addView(textCol);
            topRow.addView(btnVote);
            row.addView(topRow);

            // Graphical Vote Count Progress Bar with Smooth Real-time Animation
            ProgressBar progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
            LinearLayout.LayoutParams pbParams = new LinearLayout.LayoutParams(-1, 20);
            pbParams.setMargins(0, 12, 0, 0);
            progressBar.setLayoutParams(pbParams);
            progressBar.setMax(Math.max(maxVotes, 10));
            progressBar.setProgress(0);
            progressBar.setProgressTintList(ColorStateList.valueOf(rank == 1 ? Color.parseColor("#38BDF8") : Color.parseColor("#8B5CF6")));
            row.addView(progressBar);

            ObjectAnimator progressAnim = ObjectAnimator.ofInt(progressBar, "progress", 0, votes);
            progressAnim.setDuration(800);
            progressAnim.setInterpolator(new DecelerateInterpolator());
            progressAnim.start();

            ValueAnimator countAnim = ValueAnimator.ofInt(0, votes);
            countAnim.setDuration(800);
            countAnim.addUpdateListener(animation -> {
                int val = (int) animation.getAnimatedValue();
                tvVotes.setText("Votes: " + val + "  •  Tap for Manifesto");
            });
            countAnim.start();

            card.addView(row);
            containerCandidates.addView(card);
        } catch (Exception e) {
            Log.e("StudentDashboard", "Error adding candidate card", e);
        }
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

    private void showNotificationSnackBar(String message) {
        View rootView = findViewById(android.R.id.content);
        if (rootView == null) return;
        Snackbar snackbar = Snackbar.make(rootView, message, Snackbar.LENGTH_LONG);
        View sbView = snackbar.getView();
        sbView.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#6366F1")));
        
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) sbView.getLayoutParams();
        params.setMargins(32, 0, 32, 32);
        sbView.setLayoutParams(params);
        
        TextView tv = sbView.findViewById(com.google.android.material.R.id.snackbar_text);
        if (tv != null) {
            tv.setTextColor(Color.parseColor("#F8FAFC"));
            tv.setTypeface(null, Typeface.BOLD);
        }
        snackbar.show();
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

    public static void loadCandidateImage(Context context, String base64OrUrl, ImageView imageView) {
        try {
            if (base64OrUrl != null && !base64OrUrl.isEmpty()) {
                if (base64OrUrl.startsWith("http")) {
                    Glide.with(context).load(base64OrUrl).placeholder(R.drawable.ic_app_main).error(R.drawable.ic_app_main).centerCrop().into(imageView);
                } else {
                    byte[] decodedBytes = Base64.decode(base64OrUrl, Base64.DEFAULT);
                    Glide.with(context).load(decodedBytes).placeholder(R.drawable.ic_app_main).error(R.drawable.ic_app_main).centerCrop().into(imageView);
                }
            } else {
                Glide.with(context).load(R.drawable.ic_app_main).centerCrop().into(imageView);
            }
        } catch (Exception e) {
            Glide.with(context).load(R.drawable.ic_app_main).centerCrop().into(imageView);
        }
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
            Log.e("StudentDashboard", "Exception in uploadProfilePictureToFirebase (Base64)", e);
            Toast.makeText(this, "Error updating profile picture: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkUserAuthenticationAndRole();
        startStudentTimerLoop();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopStudentTimerLoop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopStudentTimerLoop();
        if (gateRef != null && gateEventListener != null) {
            gateRef.removeEventListener(gateEventListener);
        }
        if (voterRef != null && voterEventListener != null) {
            voterRef.removeEventListener(voterEventListener);
        }
        if (candidateRef != null && candidateEventListener != null) {
            candidateRef.removeEventListener(candidateEventListener);
        }
    }

    private boolean checkUserAuthenticationAndRole() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null || currentUser.isAnonymous() || !sessionManager.isLoggedIn()) {
            // TEMPORARY DEV TESTING BYPASS: Auto-initialize test session if launched directly from Manifest
            sessionManager.createLoginSession(
                "Test Student (Dev)",
                "student@bubt.edu.bd",
                "22235103132",
                "Student",
                "5A",
                "5A",
                "47",
                "Department of CSE, BUBT"
            );
        }

        String role = sessionManager.getUserRole();
        if ("Teacher/Admin".equals(role) || "Teacher".equalsIgnoreCase(role)) {
            Intent intent = new Intent(this, TeacherDashboardActivity.class);
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
