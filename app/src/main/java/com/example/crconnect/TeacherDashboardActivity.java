package com.example.crconnect;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ImageDecoder;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.crconnect.core.utils.HapticUtils;
import com.example.crconnect.core.utils.KeyboardUtils;
import com.google.android.material.progressindicator.CircularProgressIndicatorSpec;
import com.google.android.material.progressindicator.IndeterminateDrawable;

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
import java.util.concurrent.Executors;

import android.app.Dialog;
import android.graphics.drawable.ColorDrawable;
import android.util.TypedValue;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ScrollView;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.core.widget.NestedScrollView;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class TeacherDashboardActivity extends AppCompatActivity {

    private MaterialSwitch switchMasterVoting;
    private TextInputEditText etCandidateName;
    private MaterialButton btnAddCandidate, btnRunOff, btnResetVotes, btnLogout, btnClearName, btnExportPdf, btnExportPdfSub;
    private boolean isGeneratingPdf = false;
    private ImageView imgUserProfile;
    private ActivityResultLauncher<Intent> profileImagePickerLauncher;
    private TextView tvHeaderTitle, tvHeaderSubtitle, tvTotalCandidates;
    private TextView tvStatTotalVotes, tvStatTotalVoters, tvStatTurnout;
    private LinearLayout containerAdminCandidates;
    private RecyclerView rvAdminCandidates;
    private CandidateAdapter candidateAdapter;
    private List<Candidate> currentCandidateList = new ArrayList<>();
    private static final String[] PALETTE_HEX = {
        "#6C63FF", // Indigo/Purple
        "#0066FF", // Blue
        "#00C9A7", // Teal
        "#FF6B6B", // Red/Coral
        "#F59E0B", // Amber
        "#8B5CF6"  // Violet
    };
    private View customBottomNavAdmin;
    private View navTabStats, navTabControls, navTabProfile, navTabLogout;
    private View pillStats, pillControls, pillProfile, pillLogout;
    private ImageView iconStats, iconControls, iconProfile, iconLogout;
    private TextView labelStats, labelControls, labelProfile, labelLogout;
    private int currentActiveTab = 0; // Default active tab: Stats (index 0)
    private int currentActiveDrawerIndex = 0;
    private boolean isBottomNavVisible = true;
    private DrawerLayout drawerLayoutTeacher;
    private NavigationView navViewTeacher;
    private boolean isProgrammaticScroll = false;

    private Button btnSec4, btnSec6, btnSec7;
    private LinearLayout containerSectionButtons;
    private TextView tvTpAssignedCount;
    private View btnEditTeacherSections;
    private final List<Button> dynamicSectionButtons = new ArrayList<>();
    private DatabaseReference activeCandidateRef, activeVotersRef, activeGateRef;
    private ValueEventListener activeCandidateListener, activeVotersListener, activeGateListener;
    private int activeSectionStudentCount = 48;
    private TextView tvTpBigSecNumber, tvTpSecName, tvTpStudents, tvTpVotes, tvTpTurnout;
    private MaterialButton btnTpEditProfile, btnTpSecurity, btnTpLogout;

    // Live Election Control Views & State
    private TextView tvAdminTimerDisplay, tvAdminTimerStatus;
    private MaterialButton btnExtend5Min, btnExtend10Min, btnReduce5Min, btnPauseResume, btnEndVotingNow, btnManageCR;

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

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private String getTeacherAssignedSection() {
        if (sessionManager != null) {
            String sec = sessionManager.getAssignedSection();
            if (sec != null && !sec.isEmpty() && !sec.equalsIgnoreCase("5A") && !sec.equals("--")) {
                return sec;
            }
        }
        return "";
    }

    private void sanitizeSectionAndBindHeader() {
        String currentSection = getTeacherAssignedSection();
        if (tvHeaderTitle != null) {
            tvHeaderTitle.setText("Admin Console · Sec " + currentSection);
        }
        if (tvHeaderSubtitle != null && sessionManager != null) {
            tvHeaderSubtitle.setText(sessionManager.getUserName() + " (" + sessionManager.getUserDept() + " | Sec: " + currentSection + ")");
        }
        switchSection(currentSection);
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
        if (sec == null || sec.trim().isEmpty() || sec.equals("--") || sec.equalsIgnoreCase("5A")) return "6";
        String digitsOnly = sec.replaceAll("(?i)^section\\s*|^sec\\.?\\s*", "").trim();
        String clean = digitsOnly.replaceAll("[^a-zA-Z0-9_]", "_");
        return clean.isEmpty() ? "6" : clean;
    }

    private String getSanitizedDepartment(String dept) {
        if (dept == null || dept.trim().isEmpty() || dept.equals("--")) return "CSE";
        String normalized = dept.trim().toUpperCase(Locale.US);
        if (normalized.contains("CSE") || normalized.contains("COMPUTER SCIENCE")) return "CSE";
        if (normalized.contains("EEE") || normalized.contains("ELECTRICAL")) return "EEE";
        if (normalized.contains("BBA") || normalized.contains("BUSINESS")) return "BBA";
        if (normalized.contains("ENG") || normalized.contains("ENGLISH")) return "ENG";
        if (normalized.contains("LAW") || normalized.contains("JUSTICE")) return "LAW";
        if (normalized.contains("TEXTILE") || normalized.contains("TE")) return "TE";
        if (normalized.contains("CIVIL") || normalized.contains("CE")) return "CE";
        if (normalized.contains("DATA SCIENCE") || normalized.contains("DSE")) return "DSE";
        if (normalized.contains("MATH") || normalized.contains("MNS") || normalized.contains("STAT")) return "MNS";
        if (normalized.contains("ECO") || normalized.contains("ECONOMICS")) return "ECO";
        if (normalized.contains("ACC") || normalized.contains("ACCOUNTING")) return "ACC";
        if (normalized.contains("FIN") || normalized.contains("FINANCE")) return "FIN";
        if (normalized.contains("MGT") || normalized.contains("MANAGEMENT")) return "MGT";
        if (normalized.contains("MKT") || normalized.contains("MARKETING")) return "MKT";
        String clean = dept.trim().replaceAll("[^a-zA-Z0-9_]", "_");
        return clean.isEmpty() ? "CSE" : clean;
    }

    private void createDefaultTeacherProfileAndProceed() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String uid = currentUser != null ? currentUser.getUid() : "";
        String userEmail = currentUser != null && currentUser.getEmail() != null ? currentUser.getEmail() : (sessionManager != null && sessionManager.getUserEmail() != null ? sessionManager.getUserEmail() : "teacher@bubt.edu.bd");
        String name = sessionManager != null && sessionManager.getUserName() != null ? sessionManager.getUserName() : "Test Teacher (Dev)";
        String id = sessionManager != null && sessionManager.getUserID() != null ? sessionManager.getUserID() : "T1001";
        String assignedSection = sessionManager != null && sessionManager.getAssignedSection() != null ? sessionManager.getAssignedSection() : "6";
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
            tvHeaderTitle.setText("Admin Console · Sec " + assignedSection);
        }
        
        if (sessionManager != null) {
            sessionManager.createLoginSession(name, userEmail, id, "Teacher/Admin", "--", assignedSection, "--", dept);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        sessionManager = new SessionManager(this);
        SessionManager.applyThemeMode(sessionManager.getTeacherThemeMode());

        super.onCreate(savedInstanceState);
        
        if (!checkUserAuthenticationAndRole()) {
            return;
        }
        
        // --- STRICT EDGE-TO-EDGE FULL SCREEN ---
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getWindow().setNavigationBarContrastEnforced(false);
        }
        WindowInsetsControllerCompat insetsController = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (insetsController != null) {
            boolean isDark = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
            insetsController.setAppearanceLightStatusBars(!isDark);
            insetsController.setAppearanceLightNavigationBars(!isDark);
        }
        getWindow().setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.bg_gradient));

        setContentView(R.layout.activity_teacher_dashboard);

        // Header Pinned at Top Window Insets
        View includeHeader = findViewById(R.id.includeHeader);
        if (includeHeader != null) {
            ViewCompat.setOnApplyWindowInsetsListener(includeHeader, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(dpToPx(20), systemBars.top + dpToPx(8), dpToPx(20), dpToPx(8));
                return insets;
            });
        }

        View containerHeaderAndBody = findViewById(R.id.containerHeaderAndBody);
        if (containerHeaderAndBody != null) {
            ViewCompat.setOnApplyWindowInsetsListener(containerHeaderAndBody, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, 0, systemBars.right, 0);
                return insets;
            });
        }

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
        rvAdminCandidates = findViewById(R.id.rvAdminCandidates);
        imgUserProfile = findViewById(R.id.imgUserProfile);
        tvHeaderTitle = findViewById(R.id.tvHeaderTitle);
        tvHeaderSubtitle = findViewById(R.id.tvHeaderSubtitle);
        btnLogout = findViewById(R.id.btnLogout);
        tvTotalCandidates = findViewById(R.id.tvTotalCandidates);
        
        tvStatTotalVotes = findViewById(R.id.tvStatTotalVotes);
        tvStatTotalVoters = findViewById(R.id.tvStatTotalVoters);
        tvStatTurnout = findViewById(R.id.tvStatTurnout);
        btnExportPdf = findViewById(R.id.btnExportPdf);
        btnExportPdfSub = findViewById(R.id.btnExportPdfSub);

        if (btnExportPdf != null) {
            btnExportPdf.setOnClickListener(v -> {
                HapticUtils.mediumClick(v);
                animateButtonBounce(v, this::exportElectionResultsToPdf);
            });
        }
        if (btnExportPdfSub != null) {
            btnExportPdfSub.setOnClickListener(v -> {
                HapticUtils.mediumClick(v);
                animateButtonBounce(v, this::exportElectionResultsToPdf);
            });
        }

        // Candidate List RecyclerView Setup (Requirement 6)
        if (rvAdminCandidates != null) {
            rvAdminCandidates.setLayoutManager(new LinearLayoutManager(this));
            rvAdminCandidates.setNestedScrollingEnabled(false);
            DefaultItemAnimator animator = new DefaultItemAnimator();
            animator.setChangeDuration(250);
            animator.setAddDuration(250);
            animator.setRemoveDuration(250);
            rvAdminCandidates.setItemAnimator(animator);

            candidateAdapter = new CandidateAdapter(this, true); // isAdminView = true
            candidateAdapter.setOnCandidateClickListener(new CandidateAdapter.OnCandidateClickListener() {
                @Override
                public void onVoteClick(Candidate candidate) {}

                @Override
                public void onDeleteClick(Candidate candidate) {
                    showDeleteCandidateDialog(candidate);
                }

                @Override
                public void onItemClick(Candidate candidate) {
                    showCandidateOptionsDialog(candidate);
                }

                @Override
                public void onTestVoteClick(Candidate candidate) {
                    handleTestVote(candidate);
                }
            });
            rvAdminCandidates.setAdapter(candidateAdapter);
        }

        // Initialize Admin Candidates View immediately with default data model
        displayAdminCandidatesSnapshot(null);

        // Live Election Control UI Bindings
        tvAdminTimerDisplay = findViewById(R.id.tvAdminTimerDisplay);
        tvAdminTimerStatus = findViewById(R.id.tvAdminTimerStatus);
        btnExtend5Min = findViewById(R.id.btnExtend5Min);
        btnExtend10Min = findViewById(R.id.btnExtend10Min);
        btnReduce5Min = findViewById(R.id.btnReduce5Min);
        btnPauseResume = findViewById(R.id.btnPauseResume);
        btnEndVotingNow = findViewById(R.id.btnEndVotingNow);
        btnManageCR = findViewById(R.id.btnManageCR);

        if (btnExtend5Min != null) btnExtend5Min.setOnClickListener(v -> extendElectionTime(5 * 60 * 1000L));
        if (btnExtend10Min != null) btnExtend10Min.setOnClickListener(v -> extendElectionTime(10 * 60 * 1000L));
        if (btnReduce5Min != null) btnReduce5Min.setOnClickListener(v -> reduceElectionTime(5 * 60 * 1000L));
        if (btnPauseResume != null) btnPauseResume.setOnClickListener(v -> togglePauseResume());
        if (btnEndVotingNow != null) {
            btnEndVotingNow.setOnClickListener(v -> {
                EndVotingBottomSheet bottomSheet = EndVotingBottomSheet.newInstance(this::endVotingNow);
                bottomSheet.showAllowingStateLoss(getSupportFragmentManager(), EndVotingBottomSheet.TAG);
            });
        }
        if (btnManageCR != null) {
            btnManageCR.setOnClickListener(v -> showManageCRDialog());
            HapticUtils.attachLight(btnManageCR);
        }

        setupVotingControlListener();

        // Custom Bottom Navigation Bar Bindings (3 main tabs: Election, Profile, Logout)
        customBottomNavAdmin = findViewById(R.id.customBottomNavAdmin);
        navTabControls = findViewById(R.id.navTabControls);
        navTabProfile = findViewById(R.id.navTabProfile);
        navTabLogout = findViewById(R.id.navTabLogout);

        pillControls = findViewById(R.id.pillControls);
        pillProfile = findViewById(R.id.pillProfile);
        pillLogout = findViewById(R.id.pillLogout);

        iconControls = findViewById(R.id.iconControls);
        iconProfile = findViewById(R.id.iconProfile);
        iconLogout = findViewById(R.id.iconLogout);

        labelControls = findViewById(R.id.labelControls);
        labelProfile = findViewById(R.id.labelProfile);
        labelLogout = findViewById(R.id.labelLogout);

        setupHapticFeedback();

        if (navTabControls != null) {
            navTabControls.setOnClickListener(v -> {
                HapticUtils.lightClick(v);
                selectAdminTab(0, true);
            });
        }
        if (navTabProfile != null) {
            navTabProfile.setOnClickListener(v -> {
                HapticUtils.lightClick(v);
                selectAdminTab(1, true);
            });
        }
        if (navTabLogout != null) {
            navTabLogout.setOnClickListener(v -> {
                HapticUtils.lightClick(v);
                selectAdminTab(2, true);
            });
        }

        // Sub-toggle button bindings for Election tab
        MaterialButton btnSubToggleTiming = findViewById(R.id.btnSubToggleTiming);
        MaterialButton btnSubToggleCandidates = findViewById(R.id.btnSubToggleCandidates);
        MaterialButton btnSubToggleExport = findViewById(R.id.btnSubToggleExport);

        if (btnSubToggleTiming != null) {
            btnSubToggleTiming.setOnClickListener(v -> {
                HapticUtils.lightClick(v);
                selectElectionSubTab(0);
            });
        }
        if (btnSubToggleCandidates != null) {
            btnSubToggleCandidates.setOnClickListener(v -> {
                HapticUtils.lightClick(v);
                selectElectionSubTab(1);
            });
        }
        if (btnSubToggleExport != null) {
            btnSubToggleExport.setOnClickListener(v -> {
                HapticUtils.lightClick(v);
                selectElectionSubTab(2);
            });
        }

        handleTargetTabFromIntent(getIntent());

        sanitizeSectionAndBindHeader();

        NestedScrollView scrollViewAdmin = findViewById(R.id.scrollViewAdmin);
        if (scrollViewAdmin != null) {
            KeyboardUtils.setupSmoothKeyboardHandling(this, scrollViewAdmin, customBottomNavAdmin);
            scrollViewAdmin.setOnScrollChangeListener((NestedScrollView.OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                int dy = scrollY - oldScrollY;
                if (dy > 12 && isBottomNavVisible) {
                    hideCustomBottomNav();
                } else if (dy < -12 && !isBottomNavVisible) {
                    showCustomBottomNav();
                }
            });
        }
        
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
                    String assignedSections = snapshot.child("assignedSections").getValue(String.class);
                    String assignedIntakes = snapshot.child("assignedIntakes").getValue(String.class);
                    String assignedSection = snapshot.child("assignedSection").getValue(String.class);
                    if (name == null) name = sessionManager.getUserName();
                    if (dept == null) dept = sessionManager.getUserDept();
                    if (assignedSections != null && !assignedSections.isEmpty()) {
                        sessionManager.setAssignedSections(assignedSections);
                    }
                    if (assignedIntakes != null && !assignedIntakes.isEmpty()) {
                        sessionManager.setAssignedIntakes(assignedIntakes);
                    }
                    if (assignedSection != null && !assignedSection.isEmpty() && !assignedSection.equalsIgnoreCase("5A") && !assignedSection.equals("--")) {
                        sessionManager.setAssignedSection(assignedSection);
                    }

                    setupDynamicSectionButtons();
                    String currentSec = getTeacherAssignedSection();
                    tvHeaderSubtitle.setText(name + " (" + dept + (currentSec.isEmpty() ? "" : " | Sec: " + currentSec) + ")");
                    tvHeaderTitle.setText("Admin Console" + (currentSec.isEmpty() ? "" : " · Sec " + currentSec));

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
                        sessionManager.getAssignedSection(),
                        sessionManager.getAssignedIntakes(),
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
                                String assignedSections = doc.getString("assignedSections");
                                String assignedIntakes = doc.getString("assignedIntakes");
                                String assignedSection = doc.getString("assignedSection");
                                if (name == null) name = sessionManager.getUserName();
                                if (dept == null) dept = sessionManager.getUserDept();
                                if (assignedSections != null && !assignedSections.isEmpty()) {
                                    sessionManager.setAssignedSections(assignedSections);
                                }
                                if (assignedIntakes != null && !assignedIntakes.isEmpty()) {
                                    sessionManager.setAssignedIntakes(assignedIntakes);
                                }
                                if (assignedSection != null && !assignedSection.isEmpty() && !assignedSection.equalsIgnoreCase("5A") && !assignedSection.equals("--")) {
                                    sessionManager.setAssignedSection(assignedSection);
                                }

                                setupDynamicSectionButtons();
                                String currentSec = getTeacherAssignedSection();
                                tvHeaderSubtitle.setText(name + " (" + dept + (currentSec.isEmpty() ? "" : " | Sec: " + currentSec) + ")");
                                tvHeaderTitle.setText("Admin Console" + (currentSec.isEmpty() ? "" : " · Sec " + currentSec));

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
                                    sessionManager.getAssignedSection(),
                                    sessionManager.getAssignedIntakes(),
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

        containerSectionButtons = findViewById(R.id.containerSectionButtons);
        tvTpAssignedCount = findViewById(R.id.tvTpAssignedCount);
        btnEditTeacherSections = findViewById(R.id.btnEditTeacherSections);

        btnSec4 = findViewById(R.id.btnSec4);
        btnSec6 = findViewById(R.id.btnSec6);
        btnSec7 = findViewById(R.id.btnSec7);
        tvTpBigSecNumber = findViewById(R.id.tvTpBigSecNumber);
        tvTpSecName = findViewById(R.id.tvTpSecName);
        tvTpStudents = findViewById(R.id.tvTpStudents);
        tvTpVotes = findViewById(R.id.tvTpVotes);
        tvTpTurnout = findViewById(R.id.tvTpTurnout);
        btnTpEditProfile = findViewById(R.id.btnTpEditProfile);
        btnTpSecurity = findViewById(R.id.btnTpSecurity);
        btnTpLogout = findViewById(R.id.btnTpLogout);

        if (btnEditTeacherSections != null) {
            btnEditTeacherSections.setOnClickListener(v -> showManageTeacherSectionsDialog());
        }

        setupDynamicSectionButtons();

        if (btnTpEditProfile != null) {
            btnTpEditProfile.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                profileImagePickerLauncher.launch(intent);
            });
        }
        if (btnTpSecurity != null) {
            btnTpSecurity.setOnClickListener(v -> CustomToast.show(this, "Security settings (Secured by Firebase Auth & Admin Role)"));
        }
        if (btnTpLogout != null) {
            btnTpLogout.setOnClickListener(v -> showLogoutConfirmationDialog());
        }

        if (includeHeader != null) {
            includeHeader.setOnClickListener(v -> showProfileTab());
        }
        if (imgUserProfile != null) {
            imgUserProfile.setOnClickListener(v -> showProfileTab());
        }
        if (tvHeaderTitle != null) {
            tvHeaderTitle.setOnClickListener(v -> showProfileTab());
        }
        if (tvHeaderSubtitle != null) {
            tvHeaderSubtitle.setOnClickListener(v -> showProfileTab());
        }

        setupCustomDrawer();

        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> showLogoutConfirmationDialog());
        }

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

            String notifTitle = isChecked ? "Live Voting is OPEN (Sec " + currentSec + ")" : "Live Voting is LOCKED (Sec " + currentSec + ")";
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

        // Safety Confirmation Dialog for Reset All
        btnResetVotes.setOnClickListener(v -> {
            String activeSec = getTeacherAssignedSection();
            ResetVotesBottomSheet bottomSheet = ResetVotesBottomSheet.newInstance(() -> {
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
                                        CustomToast.show(TeacherDashboardActivity.this, "All votes reset");
                                        logAdminAction("Reset Election Votes & Candidates for Section " + activeSec);
                                        broadcastNotificationToStudents(
                                            "Election Portal Reset (Sec " + activeSec + ")",
                                            "Election portal data for Section " + activeSec + " has been reset by the teacher.",
                                            "RESET"
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
            bottomSheet.showAllowingStateLoss(getSupportFragmentManager(), ResetVotesBottomSheet.TAG);
        });

        // Safety Confirmation Dialog for Run-Off
        btnRunOff.setOnClickListener(v -> {
            String activeSec = getTeacherAssignedSection();
            RunOffBottomSheet bottomSheet = RunOffBottomSheet.newInstance(() -> {
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
                                        logAdminAction("Initialized Run-Off Election Round for Section " + activeSec);
                                        broadcastNotificationToStudents(
                                            "Run-Off Round Initialized (Sec " + activeSec + ")",
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
            bottomSheet.showAllowingStateLoss(getSupportFragmentManager(), RunOffBottomSheet.TAG);
        });

        setupHapticFeedback();
    }

    private void setupHapticFeedback() {
        HapticUtils.attachToggle(switchMasterVoting);
        HapticUtils.attachMedium(btnAddCandidate);
        HapticUtils.attachMedium(btnRunOff);
        HapticUtils.attachHeavy(btnResetVotes);
        HapticUtils.attachLight(btnClearName);
        HapticUtils.attachLight(btnLogout);
        HapticUtils.attachMedium(btnExportPdf);

        HapticUtils.attachLight(btnExtend5Min);
        HapticUtils.attachLight(btnExtend10Min);
        HapticUtils.attachLight(btnReduce5Min);
        HapticUtils.attachMedium(btnPauseResume);
        HapticUtils.attachHeavy(btnEndVotingNow);

        MaterialButton btnOpenDrawer = findViewById(R.id.btnOpenDrawer);
        if (btnOpenDrawer != null) {
            HapticUtils.attachLight(btnOpenDrawer);
        }
        if (imgUserProfile != null) {
            HapticUtils.attachLight(imgUserProfile);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleTargetTabFromIntent(intent);
    }

    private void handleTargetTabFromIntent(Intent intent) {
        if (intent != null && intent.hasExtra("EXTRA_TARGET_TAB")) {
            int targetId = intent.getIntExtra("EXTRA_TARGET_TAB", R.id.nav_teacher_candidates);
            if (targetId == R.id.nav_teacher_stats || targetId == R.id.nav_teacher_control || targetId == 0 || targetId == 1) {
                selectAdminTab(0, true);
                selectElectionSubTab(0);
            } else if (targetId == R.id.nav_teacher_candidates || targetId == 2) {
                selectAdminTab(0, true);
                selectElectionSubTab(1);
            } else if (targetId == R.id.nav_teacher_pdf || targetId == 3) {
                selectAdminTab(0, true);
                selectElectionSubTab(2);
            } else if (targetId == R.id.nav_teacher_logout || targetId == 4) {
                selectAdminTab(2, true);
            }
        } else {
            selectAdminTab(0, false); // Default active tab: Election (index 0)
        }
    }

    private void setupCustomDrawer() {
        drawerLayoutTeacher = findViewById(R.id.drawerLayoutTeacher);
        if (drawerLayoutTeacher != null) {
            // Semi-transparent black overlay rgba(15,23,42,0.4) behind drawer
            drawerLayoutTeacher.setScrimColor(Color.parseColor("#660F172A"));
            drawerLayoutTeacher.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
                @Override
                public void onDrawerOpened(View drawerView) {
                    updateDrawerActiveState(currentActiveDrawerIndex);
                }
            });
        }

        MaterialButton btnOpenDrawer = findViewById(R.id.btnOpenDrawer);
        if (btnOpenDrawer != null) {
            btnOpenDrawer.setOnClickListener(v -> {
                if (drawerLayoutTeacher != null) {
                    if (drawerLayoutTeacher.isDrawerOpen(GravityCompat.START)) {
                        drawerLayoutTeacher.closeDrawer(GravityCompat.START);
                    } else {
                        updateDrawerActiveState(currentActiveDrawerIndex);
                        drawerLayoutTeacher.openDrawer(GravityCompat.START);
                    }
                }
            });
        }

        // Header View Bindings
        TextView tvNavName = findViewById(R.id.tvNavHeaderName);
        TextView tvNavRole = findViewById(R.id.tvNavHeaderRole);
        ImageView imgNavProfile = findViewById(R.id.imgNavHeaderProfile);

        if (tvNavName != null && sessionManager != null && sessionManager.getUserName() != null && !sessionManager.getUserName().isEmpty()) {
            tvNavName.setText(sessionManager.getUserName());
        }
        if (tvNavRole != null && sessionManager != null) {
            String dept = sessionManager.getUserDept();
            if (dept == null || dept.isEmpty()) dept = "Department of CSE, BUBT";
            tvNavRole.setText("Teacher / Admin · " + dept);
        }
        if (imgNavProfile != null && sessionManager != null) {
            String cachedImg = sessionManager.getProfileImageUrl();
            if (cachedImg != null && !cachedImg.isEmpty()) {
                Glide.with(this).load(cachedImg).placeholder(R.drawable.ic_crconnect_logo).error(R.drawable.ic_crconnect_logo).circleCrop().into(imgNavProfile);
            }
        }

        // Menu Items Click Bindings
        View drawerHeaderContainer = findViewById(R.id.drawerHeaderContainer);

        View.OnClickListener profileClickListener = v -> {
            showProfileTab();
            if (drawerLayoutTeacher != null) drawerLayoutTeacher.closeDrawer(GravityCompat.START);
        };

        if (drawerHeaderContainer != null) drawerHeaderContainer.setOnClickListener(profileClickListener);
        if (imgNavProfile != null) imgNavProfile.setOnClickListener(profileClickListener);
        if (tvNavName != null) tvNavName.setOnClickListener(profileClickListener);
        if (tvNavRole != null) tvNavRole.setOnClickListener(profileClickListener);

        View drawerItemStats = findViewById(R.id.drawerItemStats);
        View drawerItemControl = findViewById(R.id.drawerItemControl);
        View drawerItemCandidates = findViewById(R.id.drawerItemCandidates);
        View drawerItemManageCR = findViewById(R.id.drawerItemManageCR);
        View drawerItemPdf = findViewById(R.id.drawerItemPdf);
        View drawerItemLogout = findViewById(R.id.drawerItemLogout);

        if (drawerItemStats != null) {
            drawerItemStats.setOnClickListener(v -> {
                selectAdminTab(0, false);
                selectElectionSubTab(0);
                updateDrawerActiveState(0);
                if (drawerLayoutTeacher != null) drawerLayoutTeacher.closeDrawer(GravityCompat.START);
            });
        }
        if (drawerItemControl != null) {
            drawerItemControl.setOnClickListener(v -> {
                selectAdminTab(0, false);
                selectElectionSubTab(0);
                updateDrawerActiveState(1);
                NestedScrollView scrollViewAdmin = findViewById(R.id.scrollViewAdmin);
                View cardVotingGate = findViewById(R.id.cardVotingGate);
                if (scrollViewAdmin != null && cardVotingGate != null) {
                    scrollViewAdmin.post(() -> scrollViewAdmin.smoothScrollTo(0, Math.max(0, cardVotingGate.getTop() - dpToPx(16))));
                }
                if (drawerLayoutTeacher != null) drawerLayoutTeacher.closeDrawer(GravityCompat.START);
            });
        }
        if (drawerItemCandidates != null) {
            drawerItemCandidates.setOnClickListener(v -> {
                selectAdminTab(0, false);
                selectElectionSubTab(1);
                updateDrawerActiveState(2);
                if (drawerLayoutTeacher != null) drawerLayoutTeacher.closeDrawer(GravityCompat.START);
            });
        }
        if (drawerItemManageCR != null) {
            drawerItemManageCR.setOnClickListener(v -> {
                updateDrawerActiveState(3);
                if (drawerLayoutTeacher != null) drawerLayoutTeacher.closeDrawer(GravityCompat.START);
                showManageCRDialog();
            });
        }
        if (drawerItemPdf != null) {
            drawerItemPdf.setOnClickListener(v -> {
                selectAdminTab(0, false);
                selectElectionSubTab(2);
                updateDrawerActiveState(4);
                if (drawerLayoutTeacher != null) drawerLayoutTeacher.closeDrawer(GravityCompat.START);
                exportElectionResultsToPdf();
            });
        }
        if (drawerItemLogout != null) {
            drawerItemLogout.setOnClickListener(v -> {
                if (drawerLayoutTeacher != null) drawerLayoutTeacher.closeDrawer(GravityCompat.START);
                showLogoutConfirmationDialog();
            });
        }

        // Expandable Theme Selection Menu Item & Buttons
        View drawerItemTheme = findViewById(R.id.drawerItemTheme);
        View containerTeacherThemeOptions = findViewById(R.id.containerTeacherThemeOptions);
        ImageView drawerChevronTheme = findViewById(R.id.drawerChevronTheme);

        if (drawerItemTheme != null && containerTeacherThemeOptions != null) {
            drawerItemTheme.setOnClickListener(v -> {
                boolean isExpanded = containerTeacherThemeOptions.getVisibility() == View.VISIBLE;
                containerTeacherThemeOptions.setVisibility(isExpanded ? View.GONE : View.VISIBLE);
                if (drawerChevronTheme != null) {
                    drawerChevronTheme.animate().rotation(isExpanded ? 0f : 180f).setDuration(200).start();
                }
            });
        }

        View btnThemeLight = findViewById(R.id.btnTeacherThemeLight);
        View btnThemeDark = findViewById(R.id.btnTeacherThemeDark);
        View btnThemeSystem = findViewById(R.id.btnTeacherThemeSystem);

        updateTeacherThemeButtons(btnThemeLight, btnThemeDark, btnThemeSystem);

        if (btnThemeLight != null) {
            btnThemeLight.setOnClickListener(v -> changeTeacherTheme(SessionManager.THEME_LIGHT));
        }
        if (btnThemeDark != null) {
            btnThemeDark.setOnClickListener(v -> changeTeacherTheme(SessionManager.THEME_DARK));
        }
        if (btnThemeSystem != null) {
            btnThemeSystem.setOnClickListener(v -> changeTeacherTheme(SessionManager.THEME_SYSTEM));
        }
    }

    private void updateTeacherThemeButtons(View btnLight, View btnDark, View btnSystem) {
        if (sessionManager == null) sessionManager = new SessionManager(this);
        String currentMode = sessionManager.getTeacherThemeMode();

        applyThemeButtonStyle(btnLight, SessionManager.THEME_LIGHT.equalsIgnoreCase(currentMode));
        applyThemeButtonStyle(btnDark, SessionManager.THEME_DARK.equalsIgnoreCase(currentMode));
        applyThemeButtonStyle(btnSystem, SessionManager.THEME_SYSTEM.equalsIgnoreCase(currentMode));
    }

    private void applyThemeButtonStyle(View btn, boolean isSelected) {
        if (btn == null) return;
        if (isSelected) {
            btn.setBackgroundResource(R.drawable.bg_glassy_option_selected);
        } else {
            btn.setBackgroundResource(R.drawable.bg_glassy_option_unselected);
        }
    }

    private void changeTeacherTheme(String mode) {
        if (sessionManager == null) sessionManager = new SessionManager(this);
        if (mode.equalsIgnoreCase(sessionManager.getTeacherThemeMode())) return;
        sessionManager.setTeacherThemeMode(mode);
        if (drawerLayoutTeacher != null) drawerLayoutTeacher.closeDrawer(GravityCompat.START);
        recreate();
    }

    private void updateDrawerActiveState(int activeIndex) {
        if (activeIndex >= 0) {
            currentActiveDrawerIndex = activeIndex;
        }
        View drawerHeaderContainer = findViewById(R.id.drawerHeaderContainer);
        View itemStats = findViewById(R.id.drawerItemStats);
        View itemControl = findViewById(R.id.drawerItemControl);
        View itemCandidates = findViewById(R.id.drawerItemCandidates);
        View itemManageCR = findViewById(R.id.drawerItemManageCR);
        View itemPdf = findViewById(R.id.drawerItemPdf);

        ImageView iconStats = findViewById(R.id.drawerIconStats);
        ImageView iconControl = findViewById(R.id.drawerIconControl);
        ImageView iconCandidates = findViewById(R.id.drawerIconCandidates);
        ImageView iconManageCR = findViewById(R.id.drawerIconManageCR);
        ImageView iconPdf = findViewById(R.id.drawerIconPdf);

        TextView labelStats = findViewById(R.id.drawerLabelStats);
        TextView labelControl = findViewById(R.id.drawerLabelControl);
        TextView labelCandidates = findViewById(R.id.drawerLabelCandidates);
        TextView labelManageCR = findViewById(R.id.drawerLabelManageCR);
        TextView labelPdf = findViewById(R.id.drawerLabelPdf);

        boolean isDark = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        int inactiveColor = isDark ? Color.parseColor("#94A3B8") : Color.parseColor("#64748B");
        int activeColor = ContextCompat.getColor(this, R.color.primary);
        int titleColor = isDark ? Color.parseColor("#F1F5F9") : Color.parseColor("#0F172A");

        if (itemStats != null) itemStats.setBackgroundColor(Color.TRANSPARENT);
        if (itemControl != null) itemControl.setBackgroundColor(Color.TRANSPARENT);
        if (itemCandidates != null) itemCandidates.setBackgroundColor(Color.TRANSPARENT);
        if (itemManageCR != null) itemManageCR.setBackgroundColor(Color.TRANSPARENT);
        if (itemPdf != null) itemPdf.setBackgroundColor(Color.TRANSPARENT);

        if (iconStats != null) iconStats.setColorFilter(inactiveColor);
        if (iconControl != null) iconControl.setColorFilter(inactiveColor);
        if (iconCandidates != null) iconCandidates.setColorFilter(inactiveColor);
        if (iconManageCR != null) iconManageCR.setColorFilter(inactiveColor);
        if (iconPdf != null) iconPdf.setColorFilter(inactiveColor);

        if (labelStats != null) labelStats.setTextColor(titleColor);
        if (labelControl != null) labelControl.setTextColor(titleColor);
        if (labelCandidates != null) labelCandidates.setTextColor(titleColor);
        if (labelManageCR != null) labelManageCR.setTextColor(titleColor);
        if (labelPdf != null) labelPdf.setTextColor(titleColor);

        switch (activeIndex) {
            case 0:
                if (itemStats != null) itemStats.setBackgroundResource(R.drawable.bg_drawer_pill_active);
                if (iconStats != null) iconStats.setColorFilter(activeColor);
                if (labelStats != null) labelStats.setTextColor(activeColor);
                break;
            case 1:
                if (itemControl != null) itemControl.setBackgroundResource(R.drawable.bg_drawer_pill_active);
                if (iconControl != null) iconControl.setColorFilter(activeColor);
                if (labelControl != null) labelControl.setTextColor(activeColor);
                break;
            case 2:
                if (itemCandidates != null) itemCandidates.setBackgroundResource(R.drawable.bg_drawer_pill_active);
                if (iconCandidates != null) iconCandidates.setColorFilter(activeColor);
                if (labelCandidates != null) labelCandidates.setTextColor(activeColor);
                break;
            case 3:
                if (itemManageCR != null) itemManageCR.setBackgroundResource(R.drawable.bg_drawer_pill_active);
                if (iconManageCR != null) iconManageCR.setColorFilter(activeColor);
                if (labelManageCR != null) labelManageCR.setTextColor(activeColor);
                break;
            case 4:
                if (itemPdf != null) itemPdf.setBackgroundResource(R.drawable.bg_drawer_pill_active);
                if (iconPdf != null) iconPdf.setColorFilter(activeColor);
                if (labelPdf != null) labelPdf.setTextColor(activeColor);
                break;
        }
    }

    private void selectElectionSubTab(int subIndex) {
        if (subIndex == 1) {
            updateDrawerActiveState(2);
        } else if (subIndex == 2) {
            updateDrawerActiveState(4);
        } else if (subIndex == 0) {
            if (currentActiveDrawerIndex != 0 && currentActiveDrawerIndex != 1) {
                updateDrawerActiveState(0);
            } else {
                updateDrawerActiveState(currentActiveDrawerIndex);
            }
        }

        View layoutSubTiming = findViewById(R.id.layoutSubTiming);
        View layoutSubCandidates = findViewById(R.id.layoutSubCandidates);
        View layoutSubExport = findViewById(R.id.layoutSubExport);

        MaterialButton btnSubToggleTiming = findViewById(R.id.btnSubToggleTiming);
        MaterialButton btnSubToggleCandidates = findViewById(R.id.btnSubToggleCandidates);
        MaterialButton btnSubToggleExport = findViewById(R.id.btnSubToggleExport);

        int activeBg = ContextCompat.getColor(this, R.color.primary);
        int activeText = Color.parseColor("#FFFFFF");
        int inactiveBg = Color.TRANSPARENT;
        int inactiveText = Color.parseColor("#64748B");

        if (btnSubToggleTiming != null) {
            btnSubToggleTiming.setBackgroundTintList(ColorStateList.valueOf(subIndex == 0 ? activeBg : inactiveBg));
            btnSubToggleTiming.setTextColor(subIndex == 0 ? activeText : inactiveText);
        }
        if (btnSubToggleCandidates != null) {
            btnSubToggleCandidates.setBackgroundTintList(ColorStateList.valueOf(subIndex == 1 ? activeBg : inactiveBg));
            btnSubToggleCandidates.setTextColor(subIndex == 1 ? activeText : inactiveText);
        }
        if (btnSubToggleExport != null) {
            btnSubToggleExport.setBackgroundTintList(ColorStateList.valueOf(subIndex == 2 ? activeBg : inactiveBg));
            btnSubToggleExport.setTextColor(subIndex == 2 ? activeText : inactiveText);
        }

        if (layoutSubTiming != null) layoutSubTiming.setVisibility(subIndex == 0 ? View.VISIBLE : View.GONE);
        if (layoutSubCandidates != null) layoutSubCandidates.setVisibility(subIndex == 1 ? View.VISIBLE : View.GONE);
        if (layoutSubExport != null) layoutSubExport.setVisibility(subIndex == 2 ? View.VISIBLE : View.GONE);
    }

    private void hideCustomBottomNav() {
        if (customBottomNavAdmin == null || !isBottomNavVisible) return;
        isBottomNavVisible = false;
        int hideDistance = customBottomNavAdmin.getHeight() > 0 ? customBottomNavAdmin.getHeight() + dpToPx(30) : dpToPx(120);
        customBottomNavAdmin.animate()
                .translationY(hideDistance)
                .alpha(0f)
                .setDuration(300)
                .setInterpolator(new FastOutSlowInInterpolator())
                .start();
    }

    private void showCustomBottomNav() {
        if (customBottomNavAdmin == null || isBottomNavVisible) return;
        isBottomNavVisible = true;
        customBottomNavAdmin.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(300)
                .setInterpolator(new FastOutSlowInInterpolator())
                .start();
    }

    private void selectAdminTab(int tabIndex, boolean triggerAction) {
        showCustomBottomNav();
        View layoutTabControls = findViewById(R.id.layoutTabControls);
        View layoutTabProfile = findViewById(R.id.layoutTabProfile);

        boolean isDark = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        int activeColor = isDark ? Color.parseColor("#3B82F6") : Color.parseColor("#0066FF");
        int inactiveColor = isDark ? Color.parseColor("#2A3A52") : Color.parseColor("#B0BCCF");

        if (pillControls != null) pillControls.setBackgroundResource(android.R.color.transparent);
        if (pillProfile != null) pillProfile.setBackgroundResource(android.R.color.transparent);
        if (pillLogout != null) pillLogout.setBackgroundResource(android.R.color.transparent);

        if (iconControls != null) iconControls.setColorFilter(inactiveColor);
        if (iconProfile != null) iconProfile.setColorFilter(inactiveColor);
        if (iconLogout != null) iconLogout.setColorFilter(inactiveColor);

        if (labelControls != null) labelControls.setTextColor(inactiveColor);
        if (labelProfile != null) labelProfile.setTextColor(inactiveColor);
        if (labelLogout != null) labelLogout.setTextColor(inactiveColor);

        NestedScrollView scrollViewAdmin = findViewById(R.id.scrollViewAdmin);

        switch (tabIndex) {
            case 0: // Election tab (Live Election Stats + Timing + Candidates + Export)
                currentActiveTab = 0;
                if (layoutTabControls != null) layoutTabControls.setVisibility(View.VISIBLE);
                if (layoutTabProfile != null) layoutTabProfile.setVisibility(View.GONE);

                if (iconControls != null) iconControls.setColorFilter(activeColor);
                if (labelControls != null) labelControls.setTextColor(activeColor);
                if (scrollViewAdmin != null) scrollViewAdmin.scrollTo(0, 0);
                break;

            case 1: // Profile tab
                currentActiveTab = 1;
                updateDrawerActiveState(-1);
                if (layoutTabControls != null) layoutTabControls.setVisibility(View.GONE);
                if (layoutTabProfile != null) layoutTabProfile.setVisibility(View.VISIBLE);

                if (iconProfile != null) iconProfile.setColorFilter(activeColor);
                if (labelProfile != null) labelProfile.setTextColor(activeColor);
                if (scrollViewAdmin != null) scrollViewAdmin.scrollTo(0, 0);
                break;

            case 2: // Logout tab
            case 3:
                if (iconLogout != null) iconLogout.setColorFilter(activeColor);
                if (labelLogout != null) labelLogout.setTextColor(activeColor);
                if (triggerAction) {
                    showLogoutConfirmationDialog();
                }
                break;
        }
    }

    private void showLogoutConfirmationDialog() {
        LogoutBottomSheet bottomSheet = LogoutBottomSheet.newInstance(() -> {
            sessionManager.logoutUser();
            Intent intent = new Intent(TeacherDashboardActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
        bottomSheet.showAllowingStateLoss(getSupportFragmentManager(), LogoutBottomSheet.TAG);
    }

    private List<Candidate> getDefaultCandidates() {
        return new ArrayList<>();
    }

    private void showCandidateOptionsDialog(Candidate candidate) {
        if (candidate == null) return;
        try {
            String activeSec = getTeacherAssignedSection();
            String activeDept = getTeacherDepartment();
            String sanitizedDept = getSanitizedDepartment(activeDept);
            String sanitizedSec = getSanitizedSection(activeSec);

            dbRef.child("users").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot usersSnapshot) {
                    if (isFinishing() || isDestroyed()) return;

                    String cName = candidate.getName() != null ? candidate.getName() : "Candidate";
                    String keyName = cName.trim().toLowerCase(Locale.US);

                    String currentRole = "Student";
                    String targetUid = candidate.getId();

                    for (DataSnapshot ds : usersSnapshot.getChildren()) {
                        String uid = ds.getKey();
                        String name = ds.child("name").getValue(String.class);
                        String role = ds.child("role").getValue(String.class);
                        String id = ds.child("id").getValue(String.class);

                        if (name != null && name.trim().toLowerCase(Locale.US).equals(keyName)) {
                            currentRole = role != null ? role : "Student";
                            targetUid = uid;
                            break;
                        }
                        if (id != null && candidate.getId() != null && id.trim().equals(candidate.getId().trim())) {
                            currentRole = role != null ? role : "Student";
                            targetUid = uid;
                            break;
                        }
                    }

                    final boolean isCr = "CR".equalsIgnoreCase(currentRole);
                    final String finalTargetUid = targetUid;

                    boolean isDark = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
                    int titleTextColor = ContextCompat.getColor(TeacherDashboardActivity.this, R.color.title_text);
                    int mutedTextColor = ContextCompat.getColor(TeacherDashboardActivity.this, R.color.muted_text);
                    int borderColor = ContextCompat.getColor(TeacherDashboardActivity.this, R.color.slate_border_light);

                    Dialog dialog = new Dialog(TeacherDashboardActivity.this);
                    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

                    LinearLayout layout = new LinearLayout(TeacherDashboardActivity.this);
                    layout.setOrientation(LinearLayout.VERTICAL);
                    layout.setPadding(40, 40, 40, 40);
                    layout.setBackgroundResource(R.drawable.bg_dialog_rounded);

                    // Candidate Title & Badge Header Row
                    LinearLayout headerRow = new LinearLayout(TeacherDashboardActivity.this);
                    headerRow.setOrientation(LinearLayout.HORIZONTAL);
                    headerRow.setGravity(Gravity.CENTER_VERTICAL);

                    TextView tvTitle = new TextView(TeacherDashboardActivity.this);
                    tvTitle.setText(cName);
                    tvTitle.setTextSize(18f);
                    tvTitle.setTypeface(null, Typeface.BOLD);
                    tvTitle.setTextColor(titleTextColor);
                    LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
                    tvTitle.setLayoutParams(titleLp);
                    headerRow.addView(tvTitle);

                    // Status Pill
                    TextView rolePill = new TextView(TeacherDashboardActivity.this);
                    rolePill.setText(isCr ? "CR DECLARED" : "CANDIDATE");
                    rolePill.setTextSize(10f);
                    rolePill.setTypeface(null, Typeface.BOLD);
                    rolePill.setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4));

                    GradientDrawable pillBg = new GradientDrawable();
                    pillBg.setCornerRadius(dpToPx(6));
                    if (isCr) {
                        pillBg.setColor(Color.parseColor(isDark ? "#381500" : "#FEF3C7"));
                        pillBg.setStroke(dpToPx(1), Color.parseColor(isDark ? "#92400E" : "#FCD34D"));
                        rolePill.setTextColor(Color.parseColor(isDark ? "#F59E0B" : "#92400E"));
                    } else {
                        pillBg.setColor(Color.parseColor(isDark ? "#1A2E4A" : "#EFF6FF"));
                        pillBg.setStroke(dpToPx(1), Color.parseColor(isDark ? "#1E3A6E" : "#BFD7FF"));
                        rolePill.setTextColor(Color.parseColor(isDark ? "#60A5FA" : "#1D4ED8"));
                    }
                    rolePill.setBackground(pillBg);
                    headerRow.addView(rolePill);

                    layout.addView(headerRow);

                    // Subtitle
                    TextView tvSub = new TextView(TeacherDashboardActivity.this);
                    String voteUnit = candidate.getVotes() == 1 ? " vote · " : " votes · ";
                    tvSub.setText(candidate.getVotes() + voteUnit + (candidate.getDept() != null ? candidate.getDept() : activeDept));
                    tvSub.setTextSize(12.5f);
                    tvSub.setTextColor(mutedTextColor);
                    tvSub.setPadding(0, dpToPx(4), 0, dpToPx(12));
                    layout.addView(tvSub);

                    // Divider
                    View divider = new View(TeacherDashboardActivity.this);
                    divider.setBackgroundColor(borderColor);
                    layout.addView(divider, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(1)));

                    // Box 1: Promote / Demote CR
                    int pTitleColor = isCr ? (isDark ? Color.parseColor("#F59E0B") : Color.parseColor("#B45309"))
                                           : (isDark ? Color.parseColor("#4ADE80") : Color.parseColor("#15803D"));
                    int pSubColor   = isCr ? (isDark ? Color.parseColor("#FCD34D") : Color.parseColor("#92400E"))
                                           : (isDark ? Color.parseColor("#86EFAC") : Color.parseColor("#166534"));
                    int pBgColor    = isCr ? (isDark ? Color.parseColor("#26381500") : Color.parseColor("#FFFBEB"))
                                           : (isDark ? Color.parseColor("#26052E16") : Color.parseColor("#F0FDF4"));
                    int pBorderColor= isCr ? (isDark ? Color.parseColor("#92400E") : Color.parseColor("#FDE68A"))
                                           : (isDark ? Color.parseColor("#15803D") : Color.parseColor("#BBF7D0"));
                    int pIconBgColor= isCr ? (isDark ? Color.parseColor("#381500") : Color.parseColor("#FEF3C7"))
                                           : (isDark ? Color.parseColor("#14532D") : Color.parseColor("#DCFCE7"));
                    int pIconTint   = isCr ? (isDark ? Color.parseColor("#F59E0B") : Color.parseColor("#D97706"))
                                           : (isDark ? Color.parseColor("#4ADE80") : Color.parseColor("#16A34A"));

                    View boxPromote = createOptionBox(
                            TeacherDashboardActivity.this,
                            isCr ? "Demote from CR to Student" : "Promote to Class Representative",
                            isCr ? "Revert candidate's role back to general student" : "Grant official CR permissions for Section " + activeSec,
                            R.drawable.ic_users,
                            pTitleColor, pSubColor, pBgColor, pBorderColor, pIconBgColor, pIconTint,
                            v -> {
                                dialog.dismiss();
                                String newRole = isCr ? "Student" : "CR";
                                Map<String, Object> roleUpdates = new HashMap<>();
                                if (finalTargetUid != null && !finalTargetUid.isEmpty()) {
                                    roleUpdates.put("users/" + finalTargetUid + "/role", newRole);
                                }
                                if (!sanitizedSec.equals("--") && finalTargetUid != null && !finalTargetUid.isEmpty()) {
                                    roleUpdates.put("sections/" + sanitizedDept + "/" + sanitizedSec + "/students/" + finalTargetUid + "/role", newRole);
                                }

                                if (!roleUpdates.isEmpty()) {
                                    dbRef.updateChildren(roleUpdates).addOnSuccessListener(unused -> {
                                        String msg = (isCr ? "Demoted " : "Promoted ") + cName + " to " + newRole + " successfully!";
                                        CustomToast.show(TeacherDashboardActivity.this, msg);
                                        logAdminAction(msg);
                                    }).addOnFailureListener(e -> {
                                        CustomToast.show(TeacherDashboardActivity.this, "Error updating role: " + e.getMessage());
                                    });
                                } else {
                                    CustomToast.show(TeacherDashboardActivity.this, "Cannot update role: Student record not found");
                                }
                            }
                    );
                    layout.addView(boxPromote);

                    // Box 2: Edit Candidate Name
                    int eTitleColor = isDark ? Color.parseColor("#60A5FA") : Color.parseColor("#1D4ED8");
                    int eSubColor   = isDark ? Color.parseColor("#93C5FD") : Color.parseColor("#1E40AF");
                    int eBgColor    = isDark ? Color.parseColor("#261E293B") : Color.parseColor("#EFF6FF");
                    int eBorderColor= isDark ? Color.parseColor("#334155") : Color.parseColor("#BFD7FF");
                    int eIconBgColor= isDark ? Color.parseColor("#1E293B") : Color.parseColor("#DBEAFE");
                    int eIconTint   = isDark ? Color.parseColor("#60A5FA") : Color.parseColor("#2563EB");

                    View boxEdit = createOptionBox(
                            TeacherDashboardActivity.this,
                            "Edit Candidate Name",
                            "Update candidate name across all election portal records",
                            R.drawable.ic_file_text,
                            eTitleColor, eSubColor, eBgColor, eBorderColor, eIconBgColor, eIconTint,
                            v -> {
                                dialog.dismiss();
                                showEditCandidateNameDialog(candidate);
                            }
                    );
                    layout.addView(boxEdit);

                    // Box 3: Delete Candidate
                    int dTitleColor = isDark ? Color.parseColor("#F87171") : Color.parseColor("#B91C1C");
                    int dSubColor   = isDark ? Color.parseColor("#FCA5A5") : Color.parseColor("#991B1B");
                    int dBgColor    = isDark ? Color.parseColor("#26450A0A") : Color.parseColor("#FEF2F2");
                    int dBorderColor= isDark ? Color.parseColor("#991B1B") : Color.parseColor("#FECACA");
                    int dIconBgColor= isDark ? Color.parseColor("#450A0A") : Color.parseColor("#FEE2E2");
                    int dIconTint   = isDark ? Color.parseColor("#F87171") : Color.parseColor("#DC2626");

                    View boxDelete = createOptionBox(
                            TeacherDashboardActivity.this,
                            "Delete Candidate",
                            "Remove candidate permanently from election portal",
                            R.drawable.ic_delete,
                            dTitleColor, dSubColor, dBgColor, dBorderColor, dIconBgColor, dIconTint,
                            v -> {
                                dialog.dismiss();
                                showDeleteCandidateDialog(candidate);
                            }
                    );
                    layout.addView(boxDelete);

                    // Cancel button
                    MaterialButton btnClose = new MaterialButton(TeacherDashboardActivity.this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
                    btnClose.setText("Cancel");
                    btnClose.setCornerRadius(dpToPx(12));
                    btnClose.setStrokeWidth(dpToPx(1));
                    btnClose.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(TeacherDashboardActivity.this, R.color.card_stroke)));
                    btnClose.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(TeacherDashboardActivity.this, R.color.input_bg)));
                    btnClose.setTextColor(titleTextColor);
                    btnClose.setTextSize(14f);
                    btnClose.setTypeface(null, Typeface.BOLD);
                    LinearLayout.LayoutParams closeLp = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, (int) (44 * getResources().getDisplayMetrics().density));
                    closeLp.setMargins(0, dpToPx(16), 0, 0);
                    btnClose.setLayoutParams(closeLp);
                    btnClose.setOnClickListener(vClose -> dialog.dismiss());
                    layout.addView(btnClose);

                    dialog.setContentView(layout);
                    if (dialog.getWindow() != null) {
                        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                        dialog.getWindow().setLayout(
                                (int) (getResources().getDisplayMetrics().widthPixels * 0.90),
                                WindowManager.LayoutParams.WRAP_CONTENT
                        );
                        dialog.getWindow().setDimAmount(0.45f);
                    }
                    dialog.show();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    CustomToast.show(TeacherDashboardActivity.this, "Error loading user profile: " + error.getMessage());
                }
            });
        } catch (Exception e) {
            Log.e("TeacherDashboard", "Error showing candidate options dialog", e);
        }
    }

    private View createOptionBox(
            Context context,
            String title,
            String subtitle,
            int iconResId,
            int titleColor,
            int subtitleColor,
            int bgColor,
            int borderColor,
            int iconBgColor,
            int iconTint,
            View.OnClickListener onClickListener
    ) {
        MaterialCardView card = new MaterialCardView(context);
        card.setRadius(dpToPx(14));
        card.setStrokeWidth(dpToPx(1));
        card.setStrokeColor(borderColor);
        card.setCardBackgroundColor(bgColor);
        card.setCardElevation(0f);
        card.setUseCompatPadding(false);

        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardLp.setMargins(0, dpToPx(10), 0, 0);
        card.setLayoutParams(cardLp);

        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dpToPx(16), dpToPx(14), dpToPx(16), dpToPx(14));

        // Left Icon Container
        FrameLayout iconBox = new FrameLayout(context);
        GradientDrawable iconBg = new GradientDrawable();
        iconBg.setCornerRadius(dpToPx(10));
        iconBg.setColor(iconBgColor);
        iconBox.setBackground(iconBg);

        int sz38 = dpToPx(38);
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(sz38, sz38);
        iconLp.setMarginEnd(dpToPx(14));
        iconBox.setLayoutParams(iconLp);

        ImageView imgIcon = new ImageView(context);
        imgIcon.setImageResource(iconResId);
        imgIcon.setColorFilter(iconTint);
        int sz20 = dpToPx(20);
        FrameLayout.LayoutParams imgLp = new FrameLayout.LayoutParams(sz20, sz20, Gravity.CENTER);
        imgIcon.setLayoutParams(imgLp);
        iconBox.addView(imgIcon);

        row.addView(iconBox);

        // Text Column
        LinearLayout textCol = new LinearLayout(context);
        textCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        textCol.setLayoutParams(textLp);

        TextView tvTitle = new TextView(context);
        tvTitle.setText(title);
        tvTitle.setTextSize(14f);
        tvTitle.setTypeface(null, Typeface.BOLD);
        tvTitle.setTextColor(titleColor);
        textCol.addView(tvTitle);

        TextView tvSub = new TextView(context);
        tvSub.setText(subtitle);
        tvSub.setTextSize(11f);
        tvSub.setTextColor(subtitleColor);
        tvSub.setPadding(0, dpToPx(2), 0, 0);
        textCol.addView(tvSub);

        row.addView(textCol);

        // Right Arrow
        ImageView arrow = new ImageView(context);
        arrow.setImageResource(R.drawable.ic_chevron_right);
        arrow.setColorFilter(titleColor);
        int sz16 = dpToPx(16);
        LinearLayout.LayoutParams arrowLp = new LinearLayout.LayoutParams(sz16, sz16);
        arrow.setLayoutParams(arrowLp);
        row.addView(arrow);

        card.addView(row);
        card.setOnClickListener(onClickListener);

        return card;
    }

    private void showEditCandidateNameDialog(Candidate candidate) {
        if (candidate == null || candidate.getId() == null) return;
        try {
            Dialog dialog = new Dialog(TeacherDashboardActivity.this);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

            LinearLayout layout = new LinearLayout(TeacherDashboardActivity.this);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(48, 48, 48, 48);
            layout.setBackgroundResource(R.drawable.bg_dialog_rounded);

            int titleTextColor = ContextCompat.getColor(TeacherDashboardActivity.this, R.color.title_text);

            TextView tvTitle = new TextView(TeacherDashboardActivity.this);
            tvTitle.setText("Edit Candidate Name");
            tvTitle.setTextSize(18f);
            tvTitle.setTypeface(null, Typeface.BOLD);
            tvTitle.setTextColor(titleTextColor);
            tvTitle.setPadding(0, 0, 0, 16);
            layout.addView(tvTitle);

            EditText etName = new EditText(TeacherDashboardActivity.this);
            etName.setText(candidate.getName());
            if (candidate.getName() != null) etName.setSelection(candidate.getName().length());
            etName.setTextSize(15f);
            etName.setTextColor(titleTextColor);
            etName.setHintTextColor(ContextCompat.getColor(TeacherDashboardActivity.this, R.color.muted_text));
            etName.setPadding(24, 24, 24, 24);
            etName.setBackgroundResource(R.drawable.edit_text_bg);
            layout.addView(etName);

            LinearLayout btnLayout = new LinearLayout(TeacherDashboardActivity.this);
            btnLayout.setOrientation(LinearLayout.HORIZONTAL);
            btnLayout.setPadding(0, 32, 0, 0);

            MaterialButton btnCancel = new MaterialButton(TeacherDashboardActivity.this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
            btnCancel.setText("Cancel");
            btnCancel.setCornerRadius(dpToPx(12));
            btnCancel.setStrokeWidth(dpToPx(1));
            btnCancel.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(TeacherDashboardActivity.this, R.color.card_stroke)));
            btnCancel.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(TeacherDashboardActivity.this, R.color.input_bg)));
            btnCancel.setTextColor(titleTextColor);
            btnCancel.setTextSize(14f);
            btnCancel.setTypeface(null, Typeface.BOLD);
            LinearLayout.LayoutParams cancelLp = new LinearLayout.LayoutParams(0, (int) (44 * getResources().getDisplayMetrics().density), 1f);
            cancelLp.setMarginEnd(12);
            btnCancel.setLayoutParams(cancelLp);
            btnCancel.setOnClickListener(v -> dialog.dismiss());
            btnLayout.addView(btnCancel);

            Button btnSave = new Button(TeacherDashboardActivity.this);
            btnSave.setText("Save");
            btnSave.setTextColor(Color.WHITE);
            btnSave.setBackgroundResource(R.drawable.bg_btn_primary);
            LinearLayout.LayoutParams saveLp = new LinearLayout.LayoutParams(0, (int) (44 * getResources().getDisplayMetrics().density), 1f);
            saveLp.setMarginStart(12);
            btnSave.setLayoutParams(saveLp);

            btnSave.setOnClickListener(v -> {
                String newName = etName.getText() != null ? etName.getText().toString().trim() : "";
                if (newName.isEmpty()) {
                    etName.setError("Name cannot be empty");
                    return;
                }

                dialog.dismiss();
                updateCandidateNameInDatabase(candidate, newName);
            });
            btnLayout.addView(btnSave);

            layout.addView(btnLayout);

            dialog.setContentView(layout);
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                dialog.getWindow().setLayout(
                        (int) (getResources().getDisplayMetrics().widthPixels * 0.88),
                        WindowManager.LayoutParams.WRAP_CONTENT
                );
                dialog.getWindow().setDimAmount(0.4f);
            }
            dialog.show();
        } catch (Exception e) {
            Log.e("TeacherDashboard", "Error showing edit candidate dialog", e);
        }
    }

    private void updateCandidateNameInDatabase(Candidate candidate, String newName) {
        if (candidate == null || candidate.getId() == null || newName == null || newName.trim().isEmpty()) return;
        String nameStr = newName.trim();
        try {
            String activeSec = getTeacherAssignedSection();
            String activeDept = getTeacherDepartment();
            String sanitizedDept = getSanitizedDepartment(activeDept);
            String sanitizedSec = getSanitizedSection(activeSec);
            String cid = candidate.getId();

            candidate.setName(nameStr);
            if (currentCandidateList != null && candidateAdapter != null) {
                int totalVotes = 0;
                for (Candidate c : currentCandidateList) {
                    totalVotes += Math.max(0, c.getVotes());
                }
                candidateAdapter.updateCandidates(currentCandidateList, totalVotes);
                LinearLayout containerChart = findViewById(R.id.containerChart);
                if (containerChart != null) {
                    containerChart.removeAllViews();
                    if (!currentCandidateList.isEmpty()) {
                        updateVoteDistributionChart(currentCandidateList, totalVotes, containerChart);
                    }
                }
            }

            Map<String, Object> updates = new HashMap<>();
            updates.put("candidates/" + cid + "/name", nameStr);
            if (!sanitizedSec.equals("--")) {
                updates.put("voting_controls/" + sanitizedDept + "/" + sanitizedSec + "/candidates/" + cid + "/name", nameStr);
                updates.put("sections/" + sanitizedDept + "/" + sanitizedSec + "/candidates/" + cid + "/name", nameStr);
            }

            dbRef.updateChildren(updates).addOnSuccessListener(aVoid -> {
                CustomToast.show(TeacherDashboardActivity.this, "Candidate name updated to " + nameStr);
                logAdminAction("Updated Candidate Name: " + nameStr);
            }).addOnFailureListener(e -> {
                CustomToast.show(TeacherDashboardActivity.this, "Failed to update candidate name: " + e.getMessage());
            });
        } catch (Exception e) {
            Log.e("TeacherDashboard", "Error updating candidate name", e);
        }
    }

    private void deleteCandidateFromDatabase(Candidate candidate) {
        if (candidate == null || candidate.getId() == null) return;
        try {
            String activeSec = getTeacherAssignedSection();
            String activeDept = getTeacherDepartment();
            String sanitizedDept = getSanitizedDepartment(activeDept);
            String sanitizedSec = getSanitizedSection(activeSec);
            String cid = candidate.getId();

            if (currentCandidateList != null) {
                currentCandidateList.removeIf(c -> c.getId() != null && c.getId().equals(cid));
                int totalVotes = 0;
                for (Candidate c : currentCandidateList) {
                    totalVotes += Math.max(0, c.getVotes());
                }
                if (candidateAdapter != null) {
                    candidateAdapter.updateCandidates(currentCandidateList, totalVotes);
                }
                if (tvTotalCandidates != null) {
                    tvTotalCandidates.setText("Total (Sec " + activeSec + "): " + currentCandidateList.size());
                }
                if (tvStatTotalVotes != null) {
                    tvStatTotalVotes.setText(String.valueOf(totalVotes));
                }
                LinearLayout containerChart = findViewById(R.id.containerChart);
                if (containerChart != null) {
                    containerChart.removeAllViews();
                    if (!currentCandidateList.isEmpty()) {
                        updateVoteDistributionChart(currentCandidateList, totalVotes, containerChart);
                    }
                }
            }

            Map<String, Object> updates = new HashMap<>();
            updates.put("candidates/" + cid, null);
            if (!sanitizedSec.equals("--")) {
                updates.put("voting_controls/" + sanitizedDept + "/" + sanitizedSec + "/candidates/" + cid, null);
                updates.put("voting_controls/" + sanitizedDept + "/" + sanitizedSec + "/votes/" + cid, null);
                updates.put("sections/" + sanitizedDept + "/" + sanitizedSec + "/candidates/" + cid, null);
            }

            dbRef.updateChildren(updates).addOnSuccessListener(aVoid -> {
                CustomToast.show(TeacherDashboardActivity.this, "Candidate deleted successfully");
                logAdminAction("Deleted Candidate: " + candidate.getName());
            }).addOnFailureListener(e -> {
                CustomToast.show(TeacherDashboardActivity.this, "Failed to delete candidate: " + e.getMessage());
            });
        } catch (Exception e) {
            Log.e("TeacherDashboard", "Error deleting candidate from database", e);
        }
    }

    private void showDeleteCandidateDialog(Candidate candidate) {
        if (candidate == null) return;
        LogoutBottomSheet bottomSheet = LogoutBottomSheet.newInstance(
            "Delete Candidate",
            "Are you sure you want to delete candidate " + candidate.getName() + "?",
            "Delete",
            () -> {
                deleteCandidateFromDatabase(candidate);
            }
        );
        bottomSheet.showAllowingStateLoss(getSupportFragmentManager(), LogoutBottomSheet.TAG);
    }

    private void handleTestVote(Candidate candidate) {
        if (candidate == null || currentCandidateList == null) return;

        // Increment candidate vote count
        candidate.setVotes(candidate.getVotes() + 1);

        // Recalculate total votes
        int totalVotes = 0;
        for (Candidate c : currentCandidateList) {
            totalVotes += Math.max(0, c.getVotes());
        }

        // Refresh Chart & Cards
        LinearLayout containerChart = findViewById(R.id.containerChart);
        if (containerChart != null) {
            updateVoteDistributionChart(currentCandidateList, totalVotes, containerChart);
        }
        if (candidateAdapter != null) {
            candidateAdapter.updateCandidates(currentCandidateList, totalVotes);
        }

        // Update Stats Tab Values: Votes cast & Turnout % (totalVotes / 48 * 100)
        tvStatTotalVotes.setText(String.valueOf(totalVotes));
        int turnout = (int) Math.round((double) totalVotes / 48.0 * 100.0);
        tvStatTurnout.setText(turnout + "%");

        // Sync with Firebase if candidate has a valid push ID
        if (candidate.getId() != null && !candidate.getId().isEmpty() && !candidate.getId().startsWith("cand_")) {
            String activeSec = getTeacherAssignedSection();
            String teacherDept = getTeacherDepartment();
            String sanitizedDept = getSanitizedDepartment(teacherDept);
            DatabaseReference cRef = !activeSec.equals("--") ?
                    dbRef.child("voting_controls").child(sanitizedDept).child(activeSec).child("candidates").child(candidate.getId()) :
                    dbRef.child("candidates").child(candidate.getId());
            cRef.child("votes").setValue(candidate.getVotes());
        }

        HapticUtils.lightClick(rvAdminCandidates);
    }

    private void displayAdminCandidatesSnapshot(DataSnapshot snapshot) {
        try {
            if (isFinishing() || isDestroyed()) return;
            String activeSec = getTeacherAssignedSection();
            sectionCandidateIds.clear();

            int totalCandidates = 0;
            int totalVotes = 0;

            List<Candidate> candidateList = new ArrayList<>();
            if (snapshot != null && snapshot.exists() && snapshot.hasChildren()) {
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

                    int vts = getSafeVotes(ds);
                    totalVotes += vts;
                    totalCandidates++;

                    String cName = ds.child("name").getValue(String.class);
                    if (cName == null) cName = "Candidate";
                    String dept = ds.child("dept").getValue(String.class);
                    if (dept == null || dept.isEmpty()) dept = "Department of CSE, BUBT";
                    String imageUrl = ds.child("imageUrl").getValue(String.class);
                    String manifesto = ds.child("manifesto").getValue(String.class);

                    Candidate c = new Candidate(candId, cName, vts, dept, imageUrl, candSec, manifesto);
                    candidateList.add(c);
                }
            }

            currentCandidateList = candidateList;

            // Display Live Vote Distribution Chart
            LinearLayout containerChart = findViewById(R.id.containerChart);
            if (containerChart != null) {
                containerChart.removeAllViews();
                if (!candidateList.isEmpty()) {
                    updateVoteDistributionChart(candidateList, totalVotes, containerChart);
                }
            }

            // Update Candidate Adapter for Admin Cards
            if (candidateAdapter != null) {
                candidateAdapter.updateCandidates(candidateList, totalVotes);
            }

            tvTotalCandidates.setText("Total (Sec " + activeSec + "): " + candidateList.size());
            tvStatTotalVotes.setText(String.valueOf(totalVotes));

            // Update Turnout % = totalVotes / 48 * 100
            int turnout = (int) Math.round((double) totalVotes / 48.0 * 100.0);
            tvStatTurnout.setText(turnout + "%");
        } catch (Exception e) {
            Log.e("TeacherDashboard", "Error loading candidates list", e);
        }
    }

    private void updateVoteDistributionChart(List<Candidate> candidateList, int totalVotes, LinearLayout container) {
        try {
            if (candidateList == null || container == null) return;

            container.removeAllViews();

            int cardBgColor = ContextCompat.getColor(this, R.color.card_bg);
            int strokeColor = ContextCompat.getColor(this, R.color.card_stroke);
            int titleColor = ContextCompat.getColor(this, R.color.title_text);
            int mutedTextColor = ContextCompat.getColor(this, R.color.muted_text);
            int trackColor = ContextCompat.getColor(this, R.color.segmented_bg);

            // Chart Card
            MaterialCardView chartCard = new MaterialCardView(this);
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, -2);
            cardParams.setMargins(0, 0, 0, dpToPx(16));
            chartCard.setLayoutParams(cardParams);
            chartCard.setRadius(dpToPx(20));
            chartCard.setCardElevation(dpToPx(2));
            chartCard.setCardBackgroundColor(cardBgColor);
            chartCard.setStrokeWidth(dpToPx(1));
            chartCard.setStrokeColor(strokeColor);

            LinearLayout chartLayout = new LinearLayout(this);
            chartLayout.setOrientation(LinearLayout.VERTICAL);
            int p20 = dpToPx(20);
            chartLayout.setPadding(p20, p20, p20, p20);

            // Title: "Live vote distribution"
            TextView tvChartTitle = new TextView(this);
            tvChartTitle.setText("Live vote distribution");
            tvChartTitle.setTextSize(15f);
            tvChartTitle.setTypeface(null, Typeface.BOLD);
            tvChartTitle.setTextColor(titleColor);
            LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(-2, -2);
            titleLp.setMargins(0, 0, 0, dpToPx(16));
            tvChartTitle.setLayoutParams(titleLp);
            chartLayout.addView(tvChartTitle);

            // Horizontal Segmented Bar
            FrameLayout barTrackFrame = new FrameLayout(this);
            LinearLayout.LayoutParams trackLp = new LinearLayout.LayoutParams(-1, dpToPx(10));
            trackLp.setMargins(0, 0, 0, dpToPx(16));
            barTrackFrame.setLayoutParams(trackLp);

            GradientDrawable trackBg = new GradientDrawable();
            trackBg.setCornerRadius(dpToPx(5));
            trackBg.setColor(trackColor);
            barTrackFrame.setBackground(trackBg);

            LinearLayout stackedBar = new LinearLayout(this);
            stackedBar.setOrientation(LinearLayout.HORIZONTAL);
            stackedBar.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));

            GradientDrawable barClipBg = new GradientDrawable();
            barClipBg.setCornerRadius(dpToPx(5));
            barClipBg.setColor(Color.TRANSPARENT);
            stackedBar.setBackground(barClipBg);
            stackedBar.setClipToOutline(true);

            if (totalVotes == 0) {
                int candidateCount = candidateList.size();
                if (candidateCount > 0) {
                    stackedBar.setWeightSum(candidateCount);
                    for (int i = 0; i < candidateCount; i++) {
                        View placeholderSegment = new View(this);
                        LinearLayout.LayoutParams segParams = new LinearLayout.LayoutParams(0, -1, 1f);
                        if (i > 0) {
                            segParams.setMargins(dpToPx(2), 0, 0, 0);
                        }
                        placeholderSegment.setLayoutParams(segParams);
                        placeholderSegment.setBackgroundColor(trackColor);
                        stackedBar.addView(placeholderSegment);
                    }
                } else {
                    View emptyTrack = new View(this);
                    emptyTrack.setLayoutParams(new LinearLayout.LayoutParams(-1, -1));
                    emptyTrack.setBackgroundColor(trackColor);
                    stackedBar.addView(emptyTrack);
                }
            } else {
                stackedBar.setWeightSum(totalVotes);
                int segIndex = 0;
                for (int i = 0; i < candidateList.size(); i++) {
                    Candidate c = candidateList.get(i);
                    int vts = Math.max(0, c.getVotes());
                    if (vts <= 0) continue;

                    int color = Color.parseColor(PALETTE_HEX[i % PALETTE_HEX.length]);
                    View segment = new View(this);
                    LinearLayout.LayoutParams segParams = new LinearLayout.LayoutParams(0, -1, (float) vts);
                    if (segIndex > 0) {
                        segParams.setMargins(dpToPx(2), 0, 0, 0);
                    }
                    segment.setLayoutParams(segParams);
                    segment.setBackgroundColor(color);

                    ValueAnimator anim = ValueAnimator.ofFloat(0f, (float) vts);
                    anim.setDuration(400);
                    anim.setInterpolator(new DecelerateInterpolator());
                    anim.addUpdateListener(animation -> {
                        float animatedWeight = (float) animation.getAnimatedValue();
                        segParams.weight = animatedWeight;
                        segment.setLayoutParams(segParams);
                    });
                    anim.start();

                    stackedBar.addView(segment);
                    segIndex++;
                }
            }

            barTrackFrame.addView(stackedBar);
            chartLayout.addView(barTrackFrame);

            // Legend: one row per candidate
            LinearLayout legendLayout = new LinearLayout(this);
            legendLayout.setOrientation(LinearLayout.VERTICAL);
            legendLayout.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));

            for (int i = 0; i < candidateList.size(); i++) {
                Candidate c = candidateList.get(i);
                int vts = Math.max(0, c.getVotes());
                String cName = c.getName() != null ? c.getName() : "Candidate";

                int color = Color.parseColor(PALETTE_HEX[i % PALETTE_HEX.length]);
                int percentage = totalVotes > 0 ? (int) Math.round((double) vts * 100.0 / totalVotes) : 0;

                LinearLayout legendRow = new LinearLayout(this);
                legendRow.setOrientation(LinearLayout.HORIZONTAL);
                legendRow.setGravity(Gravity.CENTER_VERTICAL);
                LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(-1, -2);
                if (i < candidateList.size() - 1) {
                    rowLp.setMargins(0, 0, 0, dpToPx(8));
                }
                legendRow.setLayoutParams(rowLp);

                View square = new View(this);
                int sqSize = dpToPx(10);
                LinearLayout.LayoutParams sqParams = new LinearLayout.LayoutParams(sqSize, sqSize);
                sqParams.setMargins(0, 0, dpToPx(8), 0);
                square.setLayoutParams(sqParams);

                GradientDrawable sqBg = new GradientDrawable();
                sqBg.setCornerRadius(dpToPx(3));
                sqBg.setColor(color);
                square.setBackground(sqBg);

                TextView tvName = new TextView(this);
                tvName.setText(cName);
                tvName.setTextSize(13f);
                tvName.setTextColor(titleColor);
                tvName.setSingleLine(true);
                tvName.setEllipsize(TextUtils.TruncateAt.END);
                LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(0, -2, 1f);
                tvName.setLayoutParams(nameParams);

                TextView tvMutedText = new TextView(this);
                String voteText = vts + (vts == 1 ? " vote (" : " votes (") + percentage + "%)";
                tvMutedText.setText(voteText);
                tvMutedText.setTextSize(13f);
                tvMutedText.setTextColor(mutedTextColor);
                tvMutedText.setGravity(Gravity.END);

                legendRow.addView(square);
                legendRow.addView(tvName);
                legendRow.addView(tvMutedText);
                legendLayout.addView(legendRow);
            }

            chartLayout.addView(legendLayout);
            chartCard.addView(chartLayout);
            container.addView(chartCard);
        } catch (Exception e) {
            Log.e("TeacherDashboard", "Error updating chart", e);
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
                    CustomToast.show(TeacherDashboardActivity.this, "Published to dashboard");
                    logAdminAction("Added New Candidate (" + name + ") for Section " + teacherSec);
                    broadcastNotificationToStudents(
                        "New Candidate Published (Sec " + teacherSec + ")",
                        name + " has been added to Section " + teacherSec + " election portal. Check out their manifesto!",
                        "NEW_CANDIDATE"
                    );
                })
                .addOnFailureListener(e -> {
                    Log.e("TeacherDashboard", "Failed to write candidate", e);
                    btnAddCandidate.setEnabled(true);
                });
        } else {
            btnAddCandidate.setEnabled(true);
        }
    }

    private int getSafeVotes(DataSnapshot snapshot) {
        if (snapshot == null) return 0;
        try {
            if (snapshot.child("votes").exists()) {
                Object val = snapshot.child("votes").getValue();
                if (val instanceof Long) return ((Long) val).intValue();
                if (val instanceof Integer) return (Integer) val;
                if (val instanceof String) return Integer.parseInt((String) val);
            }
        } catch (Exception ignored) {}
        return 0;
    }

    private boolean checkUserAuthenticationAndRole() {
        if (sessionManager == null) sessionManager = new SessionManager(this);
        if (!sessionManager.isLoggedIn()) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return false;
        }
        return true;
    }

    private void logAdminAction(String action) {
        try {
            String teacherDept = getTeacherDepartment();
            String sanitizedDept = getSanitizedDepartment(teacherDept);
            String teacherSec = getTeacherAssignedSection();
            String sanitizedSec = getSanitizedSection(teacherSec);

            Map<String, Object> log = new HashMap<>();
            log.put("adminName", sessionManager != null ? sessionManager.getUserName() : "Teacher");
            log.put("adminEmail", sessionManager != null ? sessionManager.getUserEmail() : "");
            log.put("action", action);
            log.put("timestamp", ServerValue.TIMESTAMP);
            log.put("section", teacherSec);

            String logId = dbRef.child("admin_logs").push().getKey();
            if (logId != null) {
                dbRef.child("admin_logs").child(logId).setValue(log);
                if (!sanitizedSec.equals("--")) {
                    dbRef.child("voting_controls").child(sanitizedDept).child(sanitizedSec).child("admin_logs").child(logId).setValue(log);
                }
            }
        } catch (Exception e) {
            Log.e("TeacherDashboard", "Error logging admin action", e);
        }
    }

    private void broadcastNotificationToStudents(String title, String body, String type) {
        try {
            String teacherDept = getTeacherDepartment();
            String sanitizedDept = getSanitizedDepartment(teacherDept);
            String teacherSec = getTeacherAssignedSection();
            String sanitizedSec = getSanitizedSection(teacherSec);

            Map<String, Object> notif = new HashMap<>();
            notif.put("title", title);
            notif.put("body", body);
            notif.put("type", type);
            notif.put("timestamp", ServerValue.TIMESTAMP);
            notif.put("sender", sessionManager != null ? sessionManager.getUserName() : "Teacher Admin");
            notif.put("section", teacherSec);

            String notifId = dbRef.child("announcements").push().getKey();
            if (notifId != null) {
                dbRef.child("announcements").child(notifId).setValue(notif);
                if (!sanitizedSec.equals("--")) {
                    dbRef.child("voting_controls").child(sanitizedDept).child(sanitizedSec).child("announcements").child(notifId).setValue(notif);
                    dbRef.child("sections").child(sanitizedDept).child(sanitizedSec).child("announcements").child(notifId).setValue(notif);
                }
            }
        } catch (Exception e) {
            Log.e("TeacherDashboard", "Error broadcasting notification", e);
        }
    }

    private void setupVotingControlListener() {
        String teacherDept = getTeacherDepartment();
        String sanitizedDept = getSanitizedDepartment(teacherDept);
        String teacherSec = getTeacherAssignedSection();
        String sanitizedSec = getSanitizedSection(teacherSec);

        votingControlRef = !sanitizedSec.equals("--") ?
                dbRef.child("voting_controls").child(sanitizedDept).child(sanitizedSec) :
                dbRef.child("voting_controls").child("default");

        votingControlListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Boolean isOpen = snapshot.child("isOpen").getValue(Boolean.class);
                    if (isOpen == null) isOpen = snapshot.child("isGateOpen").getValue(Boolean.class);
                    Boolean isPaused = snapshot.child("isPaused").getValue(Boolean.class);
                    Long expiry = snapshot.child("expiryTime").getValue(Long.class);
                    Long remaining = snapshot.child("remainingTimeMs").getValue(Long.class);

                    currentIsGateOpen = isOpen != null && isOpen;
                    currentIsPaused = isPaused != null && isPaused;
                    currentExpiryTime = expiry != null ? expiry : 0;
                    currentRemainingMs = remaining != null ? remaining : 0;

                    updateTimerUI();
                } else {
                    currentIsGateOpen = false;
                    currentIsPaused = false;
                    currentExpiryTime = 0;
                    currentRemainingMs = 0;
                    updateTimerUI();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };

        votingControlRef.addValueEventListener(votingControlListener);
        startTimerLoop();
    }

    private void startTimerLoop() {
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                updateTimerUI();
                timerHandler.postDelayed(this, 1000);
            }
        };
        timerHandler.post(timerRunnable);
    }

    private void updateTimerUI() {
        if (tvAdminTimerDisplay == null || tvAdminTimerStatus == null) return;

        if (!currentIsGateOpen) {
            tvAdminTimerDisplay.setText("00:00");
            tvAdminTimerStatus.setText("Closed");
            tvAdminTimerStatus.setTextColor(Color.parseColor("#92400E"));
            tvAdminTimerStatus.setBackgroundResource(R.drawable.bg_chip_warn);
            if (btnPauseResume != null) {
                btnPauseResume.setText("Pause voting");
                btnPauseResume.setEnabled(false);
            }
            return;
        }

        if (btnPauseResume != null) btnPauseResume.setEnabled(true);

        if (currentIsPaused) {
            long secondsLeft = currentRemainingMs / 1000;
            if (secondsLeft < 0) secondsLeft = 0;
            long minutes = secondsLeft / 60;
            long seconds = secondsLeft % 60;
            tvAdminTimerDisplay.setText(String.format(Locale.US, "%02d:%02d", minutes, seconds));

            tvAdminTimerStatus.setText("Paused");
            tvAdminTimerStatus.setTextColor(Color.parseColor("#92400E"));
            tvAdminTimerStatus.setBackgroundResource(R.drawable.bg_chip_warn);
            if (btnPauseResume != null) btnPauseResume.setText("Resume voting");
            return;
        }

        if (btnPauseResume != null) btnPauseResume.setText("Pause voting");

        long now = System.currentTimeMillis();
        long diff = currentExpiryTime - now;

        if (diff <= 0) {
            tvAdminTimerDisplay.setText("00:00");
            tvAdminTimerStatus.setText("Closed");
            tvAdminTimerStatus.setTextColor(Color.parseColor("#92400E"));
            tvAdminTimerStatus.setBackgroundResource(R.drawable.bg_chip_warn);
        } else {
            long secondsLeft = diff / 1000;
            long minutes = secondsLeft / 60;
            long seconds = secondsLeft % 60;
            tvAdminTimerDisplay.setText(String.format(Locale.US, "%02d:%02d", minutes, seconds));

            tvAdminTimerStatus.setText("Open");
            tvAdminTimerStatus.setTextColor(Color.parseColor("#166534"));
            tvAdminTimerStatus.setBackgroundResource(R.drawable.bg_chip_ok);
        }
    }

    private void extendElectionTime(long durationToAddMs) {
        String teacherDept = getTeacherDepartment();
        String sanitizedDept = getSanitizedDepartment(teacherDept);
        String teacherSec = getTeacherAssignedSection();
        String sanitizedSec = getSanitizedSection(teacherSec);

        if (!currentIsGateOpen) {
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        if (currentIsPaused) {
            long newRemaining = currentRemainingMs + durationToAddMs;
            updates.put("remainingTimeMs", newRemaining);
        } else {
            long base = Math.max(System.currentTimeMillis(), currentExpiryTime);
            long newExpiry = base + durationToAddMs;
            updates.put("expiryTime", newExpiry);
        }

        votingControlRef.updateChildren(updates).addOnSuccessListener(aVoid -> {
            int minutesAdded = (int) (durationToAddMs / (60 * 1000L));
            logAdminAction("Extended Election Time by +" + minutesAdded + " min for Section " + teacherSec);
            broadcastNotificationToStudents(
                "Voting Time Extended (Sec " + teacherSec + ")",
                "Election admin added +" + minutesAdded + " minutes of voting time for Section " + teacherSec + "!",
                "TIMER_EXTEND"
            );
        });
    }

    private void reduceElectionTime(long durationToReduceMs) {
        if (!currentIsGateOpen) {
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        if (currentIsPaused) {
            long newRemaining = Math.max(0, currentRemainingMs - durationToReduceMs);
            updates.put("remainingTimeMs", newRemaining);
        } else {
            long newExpiry = Math.max(System.currentTimeMillis(), currentExpiryTime - durationToReduceMs);
            updates.put("expiryTime", newExpiry);
        }

        votingControlRef.updateChildren(updates).addOnSuccessListener(aVoid -> {
            int minutesReduced = (int) (durationToReduceMs / (60 * 1000L));
            logAdminAction("Reduced Election Time by -" + minutesReduced + " min");
        });
    }

    private void togglePauseResume() {
        if (!currentIsGateOpen) {
            return;
        }

        String teacherSec = getTeacherAssignedSection();
        Map<String, Object> updates = new HashMap<>();
        if (currentIsPaused) {
            // Resume
            long newExpiry = System.currentTimeMillis() + currentRemainingMs;
            updates.put("isPaused", false);
            updates.put("expiryTime", newExpiry);
            updates.put("remainingTimeMs", 0L);

            votingControlRef.updateChildren(updates).addOnSuccessListener(aVoid -> {
                logAdminAction("Resumed Live Voting for Section " + teacherSec);
                broadcastNotificationToStudents(
                    "Voting Resumed (Sec " + teacherSec + ")",
                    "Live voting has resumed for Section " + teacherSec + "!",
                    "TIMER_RESUME"
                );
            });
        } else {
            // Pause
            long remaining = Math.max(0, currentExpiryTime - System.currentTimeMillis());
            updates.put("isPaused", true);
            updates.put("remainingTimeMs", remaining);

            votingControlRef.updateChildren(updates).addOnSuccessListener(aVoid -> {
                logAdminAction("Paused Live Voting for Section " + teacherSec);
                broadcastNotificationToStudents(
                    "Voting Paused (Sec " + teacherSec + ")",
                    "Live voting is temporarily paused for Section " + teacherSec + " by the election admin.",
                    "TIMER_PAUSE"
                );
            });
        }
    }

    private void endVotingNow() {
        String teacherSec = getTeacherAssignedSection();
        Map<String, Object> updates = new HashMap<>();
        updates.put("isOpen", false);
        updates.put("isGateOpen", false);
        updates.put("isPaused", false);
        updates.put("expiryTime", System.currentTimeMillis());
        updates.put("remainingTimeMs", 0L);

        votingControlRef.updateChildren(updates).addOnSuccessListener(aVoid -> {
            CustomToast.show(TeacherDashboardActivity.this, "Voting ended");
            logAdminAction("Forcefully Ended Live Voting for Section " + teacherSec);
            broadcastNotificationToStudents(
                "Voting Concluded (Sec " + teacherSec + ")",
                "Live voting for Section " + teacherSec + " has concluded! Final results are published.",
                "TIMER_END"
            );
        });
    }

    private void showManageCRDialog() {
        try {
            String activeSec = getTeacherAssignedSection();
            String activeDept = getTeacherDepartment();
            String sanitizedDept = getSanitizedDepartment(activeDept);
            String sanitizedSec = getSanitizedSection(activeSec);

            DatabaseReference candRef = !sanitizedSec.equals("--") ?
                    dbRef.child("voting_controls").child(sanitizedDept).child(sanitizedSec).child("candidates") :
                    dbRef.child("candidates");

            candRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot candSnapshot) {
                    List<Candidate> candidateList = new ArrayList<>();
                    if (candSnapshot.exists() && candSnapshot.hasChildren()) {
                        for (DataSnapshot ds : candSnapshot.getChildren()) {
                            String candSec = ds.child("section").getValue(String.class);
                            if (candSec == null) candSec = ds.child("assignedSection").getValue(String.class);

                            if (candSec != null && !candSec.equals("--") && !activeSec.equals("--") && !candSec.equalsIgnoreCase(activeSec)) {
                                continue;
                            }

                            String candId = ds.getKey();
                            String cName = ds.child("name").getValue(String.class);
                            if (cName == null) cName = "Candidate";
                            String dept = ds.child("dept").getValue(String.class);
                            int vts = getSafeVotes(ds);
                            Candidate c = new Candidate(candId, cName, vts, dept, null, candSec, null);
                            candidateList.add(c);
                        }
                    }

                    if (candidateList.isEmpty() && !sanitizedSec.equals("--")) {
                        dbRef.child("candidates").addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot rootCandSnap) {
                                List<Candidate> fallbackList = new ArrayList<>();
                                for (DataSnapshot ds : rootCandSnap.getChildren()) {
                                    String candSec = ds.child("section").getValue(String.class);
                                    if (candSec == null) candSec = ds.child("assignedSection").getValue(String.class);
                                    if (candSec != null && !candSec.equals("--") && !activeSec.equals("--") && !candSec.equalsIgnoreCase(activeSec)) {
                                        continue;
                                    }
                                    String candId = ds.getKey();
                                    String cName = ds.child("name").getValue(String.class);
                                    if (cName == null) cName = "Candidate";
                                    int vts = getSafeVotes(ds);
                                    fallbackList.add(new Candidate(candId, cName, vts, "Department of CSE, BUBT", null, candSec, null));
                                }
                                buildAndShowManageCRDialogForCandidates(fallbackList, activeSec, sanitizedDept, sanitizedSec);
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                CustomToast.show(TeacherDashboardActivity.this, "Error loading candidates: " + error.getMessage());
                            }
                        });
                    } else {
                        buildAndShowManageCRDialogForCandidates(candidateList, activeSec, sanitizedDept, sanitizedSec);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    CustomToast.show(TeacherDashboardActivity.this, "Error loading candidates: " + error.getMessage());
                }
            });
        } catch (Exception e) {
            Log.e("TeacherDashboard", "Error showing manage CR dialog", e);
            CustomToast.show(TeacherDashboardActivity.this, "Error opening Manage CR: " + e.getMessage());
        }
    }

    private void buildAndShowManageCRDialogForCandidates(List<Candidate> candidateList, String activeSec, String sanitizedDept, String sanitizedSec) {
        if (candidateList == null || candidateList.isEmpty()) {
            CustomToast.show(TeacherDashboardActivity.this, "No candidates found for Section " + activeSec);
            return;
        }

        dbRef.child("users").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot usersSnapshot) {
                Map<String, String> candidateRoles = new HashMap<>();
                Map<String, String> candidateUids = new HashMap<>();

                for (DataSnapshot ds : usersSnapshot.getChildren()) {
                    String uid = ds.getKey();
                    String name = ds.child("name").getValue(String.class);
                    String role = ds.child("role").getValue(String.class);
                    String id = ds.child("id").getValue(String.class);

                    if (name != null) candidateRoles.put(name.trim().toLowerCase(), role != null ? role : "Student");
                    if (name != null) candidateUids.put(name.trim().toLowerCase(), uid);
                    if (id != null) candidateUids.put(id.trim(), uid);
                }

                boolean isDark = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
                int titleTextColor = ContextCompat.getColor(TeacherDashboardActivity.this, R.color.title_text);
                int mutedTextColor = ContextCompat.getColor(TeacherDashboardActivity.this, R.color.muted_text);

                Dialog dialog = new Dialog(TeacherDashboardActivity.this);
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

                LinearLayout layout = new LinearLayout(TeacherDashboardActivity.this);
                layout.setOrientation(LinearLayout.VERTICAL);
                layout.setPadding(dpToPx(20), dpToPx(20), dpToPx(20), dpToPx(20));
                layout.setBackgroundResource(R.drawable.bg_dialog_rounded);

                // Header Row (Icon + Title & Subtitle)
                LinearLayout headerRow = new LinearLayout(TeacherDashboardActivity.this);
                headerRow.setOrientation(LinearLayout.HORIZONTAL);
                headerRow.setGravity(Gravity.CENTER_VERTICAL);
                headerRow.setPadding(0, 0, 0, dpToPx(12));

                FrameLayout iconBox = new FrameLayout(TeacherDashboardActivity.this);
                int sz40 = dpToPx(40);
                LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(sz40, sz40);
                iconLp.setMarginEnd(dpToPx(12));
                iconBox.setLayoutParams(iconLp);
                iconBox.setBackgroundResource(R.drawable.bg_icon_square_blue);

                ImageView imgIcon = new ImageView(TeacherDashboardActivity.this);
                imgIcon.setImageResource(R.drawable.ic_users);
                imgIcon.setColorFilter(Color.parseColor("#0066FF"));
                int sz20 = dpToPx(20);
                iconBox.addView(imgIcon, new FrameLayout.LayoutParams(sz20, sz20, Gravity.CENTER));
                headerRow.addView(iconBox);

                LinearLayout titleCol = new LinearLayout(TeacherDashboardActivity.this);
                titleCol.setOrientation(LinearLayout.VERTICAL);
                LinearLayout.LayoutParams titleColLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
                titleCol.setLayoutParams(titleColLp);

                TextView tvTitle = new TextView(TeacherDashboardActivity.this);
                tvTitle.setText("Manage CR Permissions");
                tvTitle.setTextSize(17f);
                tvTitle.setTypeface(null, Typeface.BOLD);
                tvTitle.setTextColor(titleTextColor);
                titleCol.addView(tvTitle);

                TextView tvSub = new TextView(TeacherDashboardActivity.this);
                tvSub.setText("Section " + activeSec + " · Tap a candidate to promote or demote");
                tvSub.setTextSize(12f);
                tvSub.setTextColor(mutedTextColor);
                tvSub.setPadding(0, dpToPx(2), 0, 0);
                titleCol.addView(tvSub);

                headerRow.addView(titleCol);
                layout.addView(headerRow);

                // Glass Divider Line
                View divider = new View(TeacherDashboardActivity.this);
                divider.setBackgroundColor(ContextCompat.getColor(TeacherDashboardActivity.this, R.color.slate_border_light));
                LinearLayout.LayoutParams divLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(1));
                divLp.setMargins(0, 0, 0, dpToPx(12));
                layout.addView(divider, divLp);

                // Candidates Scroll View
                ScrollView scrollView = new ScrollView(TeacherDashboardActivity.this);
                scrollView.setVerticalScrollBarEnabled(false);
                LinearLayout container = new LinearLayout(TeacherDashboardActivity.this);
                container.setOrientation(LinearLayout.VERTICAL);

                for (int i = 0; i < candidateList.size(); i++) {
                    Candidate candidate = candidateList.get(i);
                    String cName = candidate.getName();
                    String keyName = cName != null ? cName.trim().toLowerCase() : "";
                    String currentRole = candidateRoles.containsKey(keyName) ? candidateRoles.get(keyName) : "Student";
                    boolean isCr = "CR".equalsIgnoreCase(currentRole);
                    String targetUid = candidateUids.containsKey(keyName) ? candidateUids.get(keyName) : candidate.getId();

                    View candCard = createManageCRCandidateCard(
                            TeacherDashboardActivity.this,
                            candidate,
                            isCr,
                            isDark,
                            vItem -> {
                                dialog.dismiss();
                                boolean currentlyCr = isCr;
                                String newRole = currentlyCr ? "Student" : "CR";

                                Map<String, Object> roleUpdates = new HashMap<>();
                                if (targetUid != null && !targetUid.isEmpty()) {
                                    roleUpdates.put("users/" + targetUid + "/role", newRole);
                                }
                                if (!sanitizedSec.equals("--") && targetUid != null && !targetUid.isEmpty()) {
                                    roleUpdates.put("sections/" + sanitizedDept + "/" + sanitizedSec + "/students/" + targetUid + "/role", newRole);
                                }

                                if (!roleUpdates.isEmpty()) {
                                    dbRef.updateChildren(roleUpdates).addOnSuccessListener(unused -> {
                                        String actionText = (currentlyCr ? "Demoted " : "Promoted ") + cName + " to " + newRole;
                                        CustomToast.show(TeacherDashboardActivity.this, actionText + " successfully!");
                                        logAdminAction(actionText);
                                    }).addOnFailureListener(e -> {
                                        CustomToast.show(TeacherDashboardActivity.this, "Failed to update role: " + e.getMessage());
                                    });
                                } else {
                                    CustomToast.show(TeacherDashboardActivity.this, "Cannot update role for candidate: UID missing");
                                }
                            }
                    );
                    container.addView(candCard);
                }

                scrollView.addView(container);
                layout.addView(scrollView, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.0f));

                // Close Button
                MaterialButton btnClose = new MaterialButton(TeacherDashboardActivity.this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
                btnClose.setText("Close Window");
                btnClose.setCornerRadius(dpToPx(12));
                btnClose.setStrokeWidth(dpToPx(1));
                btnClose.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(TeacherDashboardActivity.this, R.color.card_stroke)));
                btnClose.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(TeacherDashboardActivity.this, R.color.input_bg)));
                btnClose.setTextColor(titleTextColor);
                btnClose.setTextSize(14f);
                btnClose.setTypeface(null, Typeface.BOLD);
                LinearLayout.LayoutParams closeLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, (int) (46 * getResources().getDisplayMetrics().density));
                closeLp.setMargins(0, dpToPx(14), 0, 0);
                btnClose.setLayoutParams(closeLp);
                btnClose.setOnClickListener(vClose -> dialog.dismiss());
                layout.addView(btnClose);

                dialog.setContentView(layout);
                if (dialog.getWindow() != null) {
                    dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                    dialog.getWindow().setLayout(
                            (int) (getResources().getDisplayMetrics().widthPixels * 0.90),
                            WindowManager.LayoutParams.WRAP_CONTENT
                    );
                    dialog.getWindow().setDimAmount(0.50f);
                }
                dialog.show();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                CustomToast.show(TeacherDashboardActivity.this, "Error loading user profile: " + error.getMessage());
            }
        });
    }

    private View createManageCRCandidateCard(
            Context context,
            Candidate candidate,
            boolean isCr,
            boolean isDark,
            View.OnClickListener onClickListener
    ) {
        MaterialCardView card = new MaterialCardView(context);
        card.setRadius(dpToPx(14));
        card.setStrokeWidth(dpToPx(1));
        card.setStrokeColor(Color.parseColor(isDark ? (isCr ? "#92400E" : "#1E293B") : (isCr ? "#FDE68A" : "#E2E8F0")));
        card.setCardBackgroundColor(Color.parseColor(isDark ? (isCr ? "#26381500" : "#131B2E") : (isCr ? "#FFFBEB" : "#F8FAFC")));
        card.setCardElevation(dpToPx(1));

        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardLp.setMargins(0, dpToPx(6), 0, dpToPx(6));
        card.setLayoutParams(cardLp);

        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12));

        // 1. Avatar Circle (40x40dp)
        FrameLayout avatarBox = new FrameLayout(context);
        int sz40 = dpToPx(40);
        LinearLayout.LayoutParams avatarLp = new LinearLayout.LayoutParams(sz40, sz40);
        avatarLp.setMarginEnd(dpToPx(12));
        avatarBox.setLayoutParams(avatarLp);

        GradientDrawable avatarBg = new GradientDrawable();
        avatarBg.setShape(GradientDrawable.OVAL);
        avatarBg.setColor(Color.parseColor(isCr ? (isDark ? "#D97706" : "#F59E0B") : "#0066FF"));
        avatarBox.setBackground(avatarBg);

        TextView tvInitials = new TextView(context);
        String cName = candidate.getName() != null ? candidate.getName() : "Student";
        tvInitials.setText(computeInitials(cName));
        tvInitials.setTextSize(14f);
        tvInitials.setTypeface(null, Typeface.BOLD);
        tvInitials.setTextColor(Color.WHITE);
        tvInitials.setGravity(Gravity.CENTER);
        avatarBox.addView(tvInitials, new FrameLayout.LayoutParams(-1, -1));

        row.addView(avatarBox);

        // 2. Info Column (Candidate Name + Votes/Dept)
        LinearLayout infoCol = new LinearLayout(context);
        infoCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams infoLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        infoCol.setLayoutParams(infoLp);

        TextView tvName = new TextView(context);
        tvName.setText(cName);
        tvName.setTextSize(15f);
        tvName.setTypeface(null, Typeface.BOLD);
        tvName.setTextColor(ContextCompat.getColor(context, R.color.title_text));
        tvName.setSingleLine(true);
        tvName.setEllipsize(TextUtils.TruncateAt.END);
        infoCol.addView(tvName);

        TextView tvMeta = new TextView(context);
        int vts = Math.max(0, candidate.getVotes());
        String voteUnit = vts == 1 ? " vote · " : " votes · ";
        String dept = candidate.getDept() != null && !candidate.getDept().isEmpty() ? candidate.getDept() : "Dept. of CSE";
        tvMeta.setText(vts + voteUnit + dept);
        tvMeta.setTextSize(12f);
        tvMeta.setTextColor(ContextCompat.getColor(context, R.color.muted_text));
        tvMeta.setPadding(0, dpToPx(2), 0, 0);
        infoCol.addView(tvMeta);

        row.addView(infoCol);

        // 3. Action / Status Pill Badge
        TextView badgeTv = new TextView(context);
        badgeTv.setTextSize(11f);
        badgeTv.setTypeface(null, Typeface.BOLD);
        badgeTv.setPadding(dpToPx(10), dpToPx(6), dpToPx(10), dpToPx(6));

        GradientDrawable badgeBg = new GradientDrawable();
        badgeBg.setCornerRadius(dpToPx(8));
        if (isCr) {
            badgeTv.setText("CR DECLARED");
            badgeBg.setColor(Color.parseColor(isDark ? "#381500" : "#FEF3C7"));
            badgeBg.setStroke(dpToPx(1), Color.parseColor(isDark ? "#92400E" : "#FCD34D"));
            badgeTv.setTextColor(Color.parseColor(isDark ? "#F59E0B" : "#92400E"));
        } else {
            badgeTv.setText("+ PROMOTE");
            badgeBg.setColor(Color.parseColor(isDark ? "#1A2E4A" : "#EFF6FF"));
            badgeBg.setStroke(dpToPx(1), Color.parseColor(isDark ? "#1E3A6E" : "#BFD7FF"));
            badgeTv.setTextColor(Color.parseColor(isDark ? "#60A5FA" : "#0066FF"));
        }
        badgeTv.setBackground(badgeBg);
        row.addView(badgeTv);

        card.addView(row);
        card.setOnClickListener(onClickListener);
        return card;
    }

    private String computeInitials(String name) {
        if (name == null || name.trim().isEmpty()) return "ST";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase(Locale.US);
        } else {
            return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase(Locale.US);
        }
    }

    private void animateButtonBounce(View view, Runnable onAnimationEnd) {
        if (view == null) {
            if (onAnimationEnd != null) onAnimationEnd.run();
            return;
        }
        view.animate()
                .scaleX(0.93f)
                .scaleY(0.93f)
                .setDuration(100)
                .withEndAction(() -> {
                    view.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(120)
                            .withEndAction(() -> {
                                if (onAnimationEnd != null) onAnimationEnd.run();
                            })
                            .start();
                })
                .start();
    }

    private void updatePdfButtonLoadingState(MaterialButton btn, boolean loading) {
        if (btn == null) return;
        if (loading) {
            try {
                CircularProgressIndicatorSpec spec = new CircularProgressIndicatorSpec(
                        this, null, 0, com.google.android.material.R.style.Widget_Material3_CircularProgressIndicator_ExtraSmall);
                spec.indicatorColors = new int[]{Color.WHITE};
                IndeterminateDrawable<CircularProgressIndicatorSpec> progressDrawable =
                        IndeterminateDrawable.createCircularDrawable(this, spec);

                btn.setIcon(progressDrawable);
                btn.setIconTint(ColorStateList.valueOf(Color.WHITE));
                btn.setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_START);
            } catch (Exception ignored) {
            }
            btn.setText("Compiling PDF report…");
            btn.setEnabled(false);

            ObjectAnimator pulseAnim = ObjectAnimator.ofFloat(btn, "alpha", 1.0f, 0.6f, 1.0f);
            pulseAnim.setDuration(800);
            pulseAnim.setRepeatCount(ValueAnimator.INFINITE);
            pulseAnim.start();
            btn.setTag(R.id.btnExportPdf, pulseAnim);
        } else {
            ObjectAnimator pulseAnim = (ObjectAnimator) btn.getTag(R.id.btnExportPdf);
            if (pulseAnim != null) {
                pulseAnim.cancel();
                btn.setTag(R.id.btnExportPdf, null);
            }
            btn.setAlpha(1.0f);
            btn.setIcon(null);
            btn.setText("Export results to PDF");
            btn.setEnabled(true);
        }
    }

    private void setPdfExportLoading(boolean loading) {
        isGeneratingPdf = loading;
        updatePdfButtonLoadingState(btnExportPdf, loading);
        updatePdfButtonLoadingState(btnExportPdfSub, loading);
    }

    private void exportElectionResultsToPdf() {
        if (isGeneratingPdf) {
            Log.d("TeacherDashboard", "PDF generation already in progress, ignoring duplicate tap");
            return;
        }

        if (getSupportFragmentManager().findFragmentByTag(PdfReadyBottomSheet.TAG) != null) {
            Log.d("TeacherDashboard", "PdfReadyBottomSheet is already showing, ignoring duplicate tap");
            return;
        }

        Log.d("TeacherDashboard", "PDF generation started");
        setPdfExportLoading(true);

        try {
            String activeSec = getTeacherAssignedSection();
            String activeDept = getTeacherDepartment();

            dbRef.child("candidates").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    List<PdfReportGenerator.CandidateResult> candidateResults = new ArrayList<>();
                    int totalVotes = 0;

                    for (DataSnapshot ds : snapshot.getChildren()) {
                        String candSec = ds.child("section").getValue(String.class);
                        if (candSec == null) candSec = ds.child("assignedSection").getValue(String.class);

                        if (candSec == null || candSec.equals("--") || candSec.equalsIgnoreCase(activeSec) || activeSec.equals("--")) {
                            String cName = ds.child("name").getValue(String.class);
                            int vts = getSafeVotes(ds);
                            totalVotes += vts;

                            candidateResults.add(new PdfReportGenerator.CandidateResult(
                                cName != null ? cName : "Candidate",
                                activeDept,
                                vts,
                                0
                            ));
                        }
                    }

                    // Sort candidate results by votes descending
                    candidateResults.sort((c1, c2) -> Integer.compare(c2.votes, c1.votes));
                    for (int i = 0; i < candidateResults.size(); i++) {
                        candidateResults.get(i).rank = i + 1;
                    }

                    final int finalTotalVotes = totalVotes;
                    Executors.newSingleThreadExecutor().execute(() -> {
                        try {
                            File pdfFile = PdfReportGenerator.generatePdfDocument(
                                TeacherDashboardActivity.this,
                                sessionManager != null ? sessionManager.getUserName() : "Teacher Admin",
                                activeDept,
                                activeSec,
                                finalTotalVotes,
                                50,
                                ((int) Math.min(100, (finalTotalVotes * 100) / 50)) + "%",
                                currentIsGateOpen,
                                candidateResults
                            );

                            runOnUiThread(() -> {
                                setPdfExportLoading(false);

                                if (isFinishing() || isDestroyed()) {
                                    Log.w("TeacherDashboard", "Activity finishing or destroyed, skipping PDF sheet presentation");
                                    return;
                                }

                                if (pdfFile != null && pdfFile.exists()) {
                                    Log.d("TeacherDashboard", "PDF file written: " + pdfFile.getAbsolutePath());

                                    if (getSupportFragmentManager().findFragmentByTag(PdfReadyBottomSheet.TAG) == null) {
                                        PdfReadyBottomSheet bottomSheet = PdfReadyBottomSheet.newInstance(pdfFile);
                                        bottomSheet.showAllowingStateLoss(getSupportFragmentManager(), PdfReadyBottomSheet.TAG);
                                        Log.d("TeacherDashboard", "PdfReadyBottomSheet shown");
                                    }
                                } else {
                                    Log.e("TeacherDashboard", "PDF generation failed: pdfFile is null or missing");
                                }
                            });
                        } catch (Exception e) {
                            Log.e("TeacherDashboard", "Error generating PDF document: " + e.getMessage(), e);
                            runOnUiThread(() -> {
                                setPdfExportLoading(false);
                            });
                        }
                    });
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("TeacherDashboard", "PDF data fetching cancelled: " + error.getMessage(), error.toException());
                    runOnUiThread(() -> {
                        setPdfExportLoading(false);
                    });
                }
            });
        } catch (Exception e) {
            Log.e("TeacherDashboard", "Error starting PDF export: " + e.getMessage(), e);
            setPdfExportLoading(false);
        }
    }

    private void openPdfFile(File file) {
        try {
            Uri fileUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(fileUri, "application/pdf");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Open Election PDF Report"));
        } catch (Exception e) {
            Log.e("TeacherDashboard", "No PDF viewer app found", e);
        }
    }

    private void uploadProfilePictureToFirebase(Uri imageUri) {
        try {
            Bitmap bitmap;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(getContentResolver(), imageUri));
            } else {
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
            byte[] data = baos.toByteArray();
            String base64Image = "data:image/jpeg;base64," + Base64.encodeToString(data, Base64.NO_WRAP);

            sessionManager.setProfileImageUrl(base64Image);
            Glide.with(this).load(bitmap).circleCrop().into(imgUserProfile);

            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            String uid = currentUser != null ? currentUser.getUid() : "";
            String rawTeacherId = sessionManager.getUserID();
            String sanitizedTeacherId = rawTeacherId != null ? rawTeacherId.replace(".", "_").replace("@", "_").replace("#", "_").replace("$", "_").replace("[", "_").replace("]", "_") : "";

            Map<String, Object> imgUpdate = new HashMap<>();
            imgUpdate.put("profileImageUrl", base64Image);

            if (!uid.isEmpty()) {
                dbRef.child("users").child(uid).updateChildren(imgUpdate);
                FirebaseFirestore.getInstance().collection("users").document(uid).update(imgUpdate);
            }
            if (!sanitizedTeacherId.isEmpty()) {
                dbRef.child("users").child(sanitizedTeacherId).updateChildren(imgUpdate);
                FirebaseFirestore.getInstance().collection("users").document(sanitizedTeacherId).update(imgUpdate);
            }
        } catch (Exception e) {
            Log.e("TeacherDashboard", "Error uploading profile image", e);
        }
    }

    private void loadProfileImage(Context context, String urlOrBase64, ImageView imageView) {
        try {
            if (urlOrBase64 == null || urlOrBase64.isEmpty() || imageView == null) return;
            if (urlOrBase64.startsWith("data:image")) {
                String cleanBase64 = urlOrBase64.substring(urlOrBase64.indexOf(",") + 1);
                byte[] decodedBytes = Base64.decode(cleanBase64, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                Glide.with(context).load(bitmap).placeholder(R.drawable.ic_app_main).error(R.drawable.ic_app_main).circleCrop().into(imageView);
            } else {
                Glide.with(context).load(urlOrBase64).placeholder(R.drawable.ic_app_main).error(R.drawable.ic_app_main).circleCrop().into(imageView);
            }
        } catch (Exception e) {
            Log.e("TeacherDashboard", "Error decoding profile image", e);
        }
    }

    private void showProfileTab() {
        selectAdminTab(1, false);
    }

    private void detachLiveFirebaseSectionListeners() {
        if (activeCandidateRef != null && activeCandidateListener != null) {
            activeCandidateRef.removeEventListener(activeCandidateListener);
            activeCandidateRef = null;
            activeCandidateListener = null;
        }
        if (activeVotersRef != null && activeVotersListener != null) {
            activeVotersRef.removeEventListener(activeVotersListener);
            activeVotersRef = null;
            activeVotersListener = null;
        }
        if (activeGateRef != null && activeGateListener != null) {
            activeGateRef.removeEventListener(activeGateListener);
            activeGateRef = null;
            activeGateListener = null;
        }
        if (votingControlRef != null && votingControlListener != null) {
            votingControlRef.removeEventListener(votingControlListener);
            votingControlRef = null;
        }
    }

    private void fetchSectionStudentCount(String sec) {
        String sanitizedSec = getSanitizedSection(sec);
        dbRef.child("users").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isFinishing() || isDestroyed()) return;
                int count = 0;
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String uSec = ds.child("section").getValue(String.class);
                    if (uSec == null) uSec = ds.child("assignedSection").getValue(String.class);
                    String role = ds.child("role").getValue(String.class);

                    if (role != null && role.equalsIgnoreCase("Teacher")) {
                        continue;
                    }

                    if (uSec != null && !uSec.equals("--")) {
                        if (getSanitizedSection(uSec).equalsIgnoreCase(sanitizedSec)) {
                            count++;
                        }
                    } else if (sec.equals("6")) {
                        count++;
                    }
                }
                activeSectionStudentCount = Math.max(1, count);
                if (tvTpStudents != null) {
                    tvTpStudents.setText(String.valueOf(activeSectionStudentCount));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void attachLiveFirebaseSectionListeners(String sec) {
        detachLiveFirebaseSectionListeners();

        String teacherDept = getTeacherDepartment();
        String sanitizedDept = getSanitizedDepartment(teacherDept);
        String sanitizedSec = getSanitizedSection(sec);

        activeGateRef = !sanitizedSec.equals("--") ?
                dbRef.child("voting_controls").child(sanitizedDept).child(sanitizedSec).child("isGateOpen") :
                dbRef.child("master_voting_enabled");

        activeGateListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean isEnabled = snapshot.getValue(Boolean.class);
                if (isEnabled != null) {
                    if (switchMasterVoting != null) switchMasterVoting.setChecked(isEnabled);
                } else {
                    dbRef.child("master_voting_enabled").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snap) {
                            Boolean globalEnabled = snap.getValue(Boolean.class);
                            if (globalEnabled != null && switchMasterVoting != null) switchMasterVoting.setChecked(globalEnabled);
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {}
                    });
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        activeGateRef.addValueEventListener(activeGateListener);

        activeCandidateRef = !sanitizedSec.equals("--") ?
                dbRef.child("voting_controls").child(sanitizedDept).child(sanitizedSec).child("candidates") :
                dbRef.child("candidates");

        activeCandidateListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                displayAdminCandidatesSnapshot(snapshot);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("TeacherDashboard", "Candidates query cancelled: " + error.getMessage());
            }
        };
        activeCandidateRef.addValueEventListener(activeCandidateListener);

        activeVotersRef = !sanitizedSec.equals("--") ?
                dbRef.child("voting_controls").child(sanitizedDept).child(sanitizedSec).child("voters") :
                dbRef.child("voters");

        activeVotersListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                displayVotersCountSnapshot(snapshot);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        activeVotersRef.addValueEventListener(activeVotersListener);

        votingControlRef = !sanitizedSec.equals("--") ?
                dbRef.child("voting_controls").child(sanitizedDept).child(sanitizedSec) :
                dbRef.child("voting_controls").child("default");

        votingControlListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Boolean isOpen = snapshot.child("isOpen").getValue(Boolean.class);
                    if (isOpen == null) isOpen = snapshot.child("isGateOpen").getValue(Boolean.class);
                    Boolean isPaused = snapshot.child("isPaused").getValue(Boolean.class);
                    Long expiry = snapshot.child("expiryTime").getValue(Long.class);
                    Long remaining = snapshot.child("remainingTimeMs").getValue(Long.class);

                    currentIsGateOpen = isOpen != null && isOpen;
                    currentIsPaused = isPaused != null && isPaused;
                    currentExpiryTime = expiry != null ? expiry : 0;
                    currentRemainingMs = remaining != null ? remaining : 0;

                    updateTimerUI();
                } else {
                    currentIsGateOpen = false;
                    currentIsPaused = false;
                    currentExpiryTime = 0;
                    currentRemainingMs = 0;
                    updateTimerUI();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        votingControlRef.addValueEventListener(votingControlListener);

        fetchSectionStudentCount(sec);
    }

    private void setupDynamicSectionButtons() {
        if (containerSectionButtons == null) return;
        containerSectionButtons.removeAllViews();
        dynamicSectionButtons.clear();

        String rawSections = sessionManager != null ? sessionManager.getAssignedSections() : "";
        if (rawSections.equals("--") || rawSections.equalsIgnoreCase("5A")) rawSections = "";

        List<String> sectionList = new ArrayList<>();
        if (!rawSections.trim().isEmpty()) {
            String[] parts = rawSections.split("[,;\\s]+");
            for (String p : parts) {
                String clean = p.replaceAll("(?i)^section\\s*|^sec\\.?\\s*", "").trim();
                if (!clean.isEmpty() && !sectionList.contains(clean)) {
                    sectionList.add(clean);
                }
            }
        }

        if (tvTpAssignedCount != null) {
            tvTpAssignedCount.setText(sectionList.size() + (sectionList.size() == 1 ? " section" : " sections"));
        }

        if (sectionList.isEmpty()) {
            MaterialButton btnAdd = new MaterialButton(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dpToPx(42)
            );
            btnAdd.setLayoutParams(lp);
            btnAdd.setText("➕ Manage Assigned Sections");
            btnAdd.setTextColor(Color.WHITE);
            btnAdd.setTextSize(14f);
            btnAdd.setTypeface(null, Typeface.BOLD);
            btnAdd.setCornerRadius(dpToPx(12));
            btnAdd.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#0284C7")));
            btnAdd.setOnClickListener(v -> showManageTeacherSectionsDialog());
            containerSectionButtons.addView(btnAdd);

            if (tvTpBigSecNumber != null) tvTpBigSecNumber.setText("--");
            if (tvTpSecName != null) tvTpSecName.setText("No Section Assigned");
            return;
        }

        String activeSec = getTeacherAssignedSection();
        if (activeSec.isEmpty() || !sectionList.contains(activeSec)) {
            activeSec = sectionList.get(0);
            if (sessionManager != null) sessionManager.setAssignedSection(activeSec);
        }

        int total = sectionList.size();
        for (int i = 0; i < total; i++) {
            final String sec = sectionList.get(i);
            MaterialButton button = new MaterialButton(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    0,
                    dpToPx(40),
                    1.0f
            );
            if (i > 0) lp.leftMargin = dpToPx(4);
            if (i < total - 1) lp.rightMargin = dpToPx(4);

            button.setLayoutParams(lp);
            button.setText("Sec " + sec);
            button.setTextSize(13f);
            button.setTypeface(null, Typeface.BOLD);
            button.setCornerRadius(dpToPx(12));
            button.setInsetTop(0);
            button.setInsetBottom(0);

            boolean isSelected = sec.equals(activeSec);
            updateSectionButtonState(button, isSelected);

            button.setOnClickListener(v -> {
                switchSection(sec);
                refreshDynamicSectionButtonSelection(sec);
            });

            containerSectionButtons.addView(button);
            dynamicSectionButtons.add(button);
        }

        switchSection(activeSec);
    }

    private void refreshDynamicSectionButtonSelection(String activeSec) {
        String rawSections = sessionManager != null ? sessionManager.getAssignedSections() : "";
        List<String> sectionList = new ArrayList<>();
        if (!rawSections.trim().isEmpty()) {
            String[] parts = rawSections.split("[,;\\s]+");
            for (String p : parts) {
                String clean = p.replaceAll("(?i)^section\\s*|^sec\\.?\\s*", "").trim();
                if (!clean.isEmpty() && !sectionList.contains(clean)) {
                    sectionList.add(clean);
                }
            }
        }
        for (int i = 0; i < dynamicSectionButtons.size() && i < sectionList.size(); i++) {
            Button btn = dynamicSectionButtons.get(i);
            String sec = sectionList.get(i);
            updateSectionButtonState(btn, sec.equals(activeSec));
        }
    }

    private void showManageTeacherSectionsDialog() {
        if (getSupportFragmentManager().findFragmentByTag(ManageSectionsBottomSheet.TAG) != null) {
            return;
        }

        String currentSections = sessionManager != null ? sessionManager.getAssignedSections() : "";
        if (currentSections.equals("--")) currentSections = "";
        String currentIntakes = sessionManager != null ? sessionManager.getAssignedIntakes() : "";
        if (currentIntakes.equals("--")) currentIntakes = "";

        ManageSectionsBottomSheet bottomSheet = ManageSectionsBottomSheet.newInstance(
                currentSections,
                currentIntakes,
                this::saveTeacherAssignedSectionsAndIntakes
        );
        bottomSheet.show(getSupportFragmentManager(), ManageSectionsBottomSheet.TAG);
    }

    private void saveTeacherAssignedSectionsAndIntakes(String secInput, String intakeInput) {
        if (sessionManager != null) {
            sessionManager.setAssignedSections(secInput);
            sessionManager.setAssignedIntakes(intakeInput);
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String uid = currentUser != null ? currentUser.getUid() : "";
        String rawTeacherId = sessionManager != null ? sessionManager.getUserID() : "";
        String sanitizedTeacherId = rawTeacherId != null ? rawTeacherId.replace(".", "_").replace("@", "_").replace("#", "_").replace("$", "_").replace("[", "_").replace("]", "_") : "";

        Map<String, Object> updates = new HashMap<>();
        updates.put("assignedSections", secInput);
        updates.put("assignedIntakes", intakeInput);

        List<String> parsedSecs = new ArrayList<>();
        if (!secInput.trim().isEmpty()) {
            for (String s : secInput.split("[,;\\s]+")) {
                String clean = s.replaceAll("(?i)^section\\s*|^sec\\.?\\s*", "").trim();
                if (!clean.isEmpty() && !parsedSecs.contains(clean)) parsedSecs.add(clean);
            }
        }

        String activeSec = parsedSecs.isEmpty() ? "" : parsedSecs.get(0);
        if (sessionManager != null) sessionManager.setAssignedSection(activeSec);
        updates.put("assignedSection", activeSec);

        if (!uid.isEmpty()) {
            dbRef.child("users").child(uid).updateChildren(updates);
            FirebaseFirestore.getInstance().collection("users").document(uid).update(updates);
        }
        if (!sanitizedTeacherId.isEmpty()) {
            dbRef.child("users").child(sanitizedTeacherId).updateChildren(updates);
        }

        setupDynamicSectionButtons();
        CustomToast.show(this, "Assigned sections updated!");
    }

    private void switchSection(String sec) {
        if (sessionManager != null) {
            sessionManager.setAssignedSection(sec);
        }

        String intakeStr = sessionManager != null ? sessionManager.getAssignedIntakes() : "";
        String name = sec.isEmpty() ? "No Section Selected" : "Section " + sec + (intakeStr.isEmpty() ? "" : " · Intake " + intakeStr);

        if (tvTpBigSecNumber != null) tvTpBigSecNumber.setText(sec.isEmpty() ? "--" : sec);
        if (tvTpSecName != null) tvTpSecName.setText(name);

        if (tvHeaderTitle != null) {
            tvHeaderTitle.setText("Admin Console" + (sec.isEmpty() ? "" : " · Sec " + sec));
        }
        if (tvHeaderSubtitle != null && sessionManager != null) {
            tvHeaderSubtitle.setText(sessionManager.getUserName() + " (" + sessionManager.getUserDept() + (sec.isEmpty() ? "" : " | Sec: " + sec) + ")");
        }

        refreshDynamicSectionButtonSelection(sec);

        if (!sec.isEmpty()) {
            attachLiveFirebaseSectionListeners(sec);
        }
    }

    private void updateSectionButtonState(Button button, boolean isSelected) {
        if (button == null) return;
        int selectedBg = Color.WHITE;
        int selectedText = Color.parseColor("#0066FF");
        int unselectedBg = Color.TRANSPARENT;
        int unselectedText = Color.WHITE;
        int selectedStroke = Color.WHITE;
        int unselectedStroke = Color.parseColor("#80FFFFFF");

        button.setBackgroundTintList(ColorStateList.valueOf(isSelected ? selectedBg : unselectedBg));
        button.setTextColor(isSelected ? selectedText : unselectedText);
        if (button instanceof MaterialButton) {
            ((MaterialButton) button).setStrokeColor(ColorStateList.valueOf(isSelected ? selectedStroke : unselectedStroke));
        }
    }

    private void showTeacherProfileBottomSheet() {
        if (getSupportFragmentManager().findFragmentByTag(TeacherProfileBottomSheet.TAG) != null) {
            return;
        }
        TeacherProfileBottomSheet bottomSheet = TeacherProfileBottomSheet.newInstance(
            () -> {
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                profileImagePickerLauncher.launch(intent);
            },
            () -> {
                CustomToast.show(this, "Security settings (Secured by Firebase Auth & Admin Role)");
            },
            this::showLogoutConfirmationDialog,
            this::showManageTeacherSectionsDialog
        );
        bottomSheet.showAllowingStateLoss(getSupportFragmentManager(), TeacherProfileBottomSheet.TAG);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timerHandler != null && timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
        }
        detachLiveFirebaseSectionListeners();
    }
}
