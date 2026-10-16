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
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.crconnect.core.utils.HapticUtils;
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

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
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
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;

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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class TeacherDashboardActivity extends AppCompatActivity {

    private MaterialSwitch switchMasterVoting;
    private TextInputEditText etCandidateName;
    private MaterialButton btnAddCandidate, btnRunOff, btnResetVotes, btnLogout, btnClearName, btnExportPdf;
    private boolean isGeneratingPdf = false;
    private ImageView imgUserProfile;
    private ActivityResultLauncher<Intent> profileImagePickerLauncher;
    private TextView tvHeaderTitle, tvHeaderSubtitle, tvTotalCandidates;
    private TextView tvStatTotalVotes, tvStatTotalVoters, tvStatTurnout;
    private LinearLayout containerAdminCandidates;
    private RecyclerView rvAdminCandidates;
    private CandidateAdapter candidateAdapter;
    private BottomNavigationView bottomNavTeacher;
    private DrawerLayout drawerLayoutTeacher;
    private NavigationView navViewTeacher;
    private boolean isProgrammaticScroll = false;

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
            sessionManager.setAssignedSection("6");
        }
        return "6";
    }

    private void sanitizeSectionAndBindHeader() {
        String currentSection = getTeacherAssignedSection();
        if (tvHeaderTitle != null) {
            tvHeaderTitle.setText("Admin Console · Sec " + currentSection);
        }
        if (tvHeaderSubtitle != null && sessionManager != null) {
            tvHeaderSubtitle.setText(sessionManager.getUserName() + " (" + sessionManager.getUserDept() + " | Sec: " + currentSection + ")");
        }
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
        super.onCreate(savedInstanceState);
        
        sessionManager = new SessionManager(this);
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
            insetsController.setAppearanceLightStatusBars(true);
            insetsController.setAppearanceLightNavigationBars(true);
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

        btnExportPdf.setOnClickListener(v -> exportElectionResultsToPdf());

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
            rvAdminCandidates.setAdapter(candidateAdapter);
        }

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
        if (btnEndVotingNow != null) btnEndVotingNow.setOnClickListener(v -> endVotingNow());
        if (btnManageCR != null) {
            btnManageCR.setOnClickListener(v -> showManageCRDialog());
            HapticUtils.attachLight(btnManageCR);
        }

        setupVotingControlListener();

        bottomNavTeacher = findViewById(R.id.bottomNavTeacher);
        setupHapticFeedback();
        if (bottomNavTeacher != null) {
            ViewCompat.setOnApplyWindowInsetsListener(bottomNavTeacher, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(0, 0, 0, systemBars.bottom);
                return insets;
            });
        }

        // Bottom nav selection follows scroll (Requirement 5)
        NestedScrollView scrollViewAdmin = findViewById(R.id.scrollViewAdmin);
        if (scrollViewAdmin != null && bottomNavTeacher != null) {
            scrollViewAdmin.setOnScrollChangeListener((NestedScrollView.OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                if (isProgrammaticScroll) return;

                View cardStats = findViewById(R.id.cardLiveStats);
                View cardGate = findViewById(R.id.cardVotingGate);
                View cardCandHeader = findViewById(R.id.tvPublishedCandidatesHeader);

                if (cardStats == null || cardGate == null) return;

                View child = scrollViewAdmin.getChildAt(0);
                if (child != null) {
                    int maxScrollY = child.getHeight() - scrollViewAdmin.getHeight();
                    if (scrollY >= maxScrollY - 20) {
                        if (bottomNavTeacher.getSelectedItemId() != R.id.nav_teacher_candidates) {
                            bottomNavTeacher.setSelectedItemId(R.id.nav_teacher_candidates);
                        }
                        return;
                    }
                }

                int statsTop = cardStats.getTop();
                int gateTop = cardGate.getTop();
                int candTop = cardCandHeader != null ? cardCandHeader.getTop() : (findViewById(R.id.cardAddCandidate) != null ? findViewById(R.id.cardAddCandidate).getTop() : Integer.MAX_VALUE);

                int distStats = Math.abs(scrollY - statsTop);
                int distGate = Math.abs(scrollY - gateTop);
                int distCand = Math.abs(scrollY - candTop);

                int targetId;
                if (distStats <= distGate && distStats <= distCand) {
                    targetId = R.id.nav_teacher_stats;
                } else if (distGate <= distStats && distGate <= distCand) {
                    targetId = R.id.nav_teacher_control;
                } else {
                    targetId = R.id.nav_teacher_candidates;
                }

                if (bottomNavTeacher.getSelectedItemId() != targetId) {
                    bottomNavTeacher.setSelectedItemId(targetId);
                }
            });

            bottomNavTeacher.setOnItemSelectedListener(item -> {
                HapticUtils.lightClick(bottomNavTeacher);
                int id = item.getItemId();

                if (id == R.id.nav_teacher_stats) {
                    View targetView = findViewById(R.id.cardLiveStats);
                    if (targetView != null) {
                        isProgrammaticScroll = true;
                        scrollViewAdmin.smoothScrollTo(0, targetView.getTop());
                        scrollViewAdmin.postDelayed(() -> isProgrammaticScroll = false, 500);
                    }
                    return true;
                } else if (id == R.id.nav_teacher_control) {
                    View targetView = findViewById(R.id.cardVotingGate);
                    if (targetView != null) {
                        isProgrammaticScroll = true;
                        scrollViewAdmin.smoothScrollTo(0, targetView.getTop());
                        scrollViewAdmin.postDelayed(() -> isProgrammaticScroll = false, 500);
                    }
                    return true;
                } else if (id == R.id.nav_teacher_candidates) {
                    View targetView = findViewById(R.id.cardAddCandidate);
                    if (targetView != null) {
                        isProgrammaticScroll = true;
                        scrollViewAdmin.smoothScrollTo(0, targetView.getTop());
                        scrollViewAdmin.postDelayed(() -> isProgrammaticScroll = false, 500);
                    }
                    return true;
                } else if (id == R.id.nav_teacher_pdf) {
                    exportElectionResultsToPdf();
                    return true;
                } else if (id == R.id.nav_teacher_logout) {
                    sessionManager.logoutUser();
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                    return true;
                }
                return false;
            });
        }

        sanitizeSectionAndBindHeader();

        if (scrollViewAdmin != null) {
            ViewCompat.setOnApplyWindowInsetsListener(scrollViewAdmin, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
                v.setPadding(systemBars.left, 0, systemBars.right, Math.max(0, ime.bottom));
                return WindowInsetsCompat.CONSUMED;
            });
        }

        etCandidateName.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && scrollViewAdmin != null) {
                View cardAddCandidate = findViewById(R.id.cardAddCandidate);
                if (cardAddCandidate != null) {
                    scrollViewAdmin.postDelayed(() -> scrollViewAdmin.smoothScrollTo(0, Math.max(0, cardAddCandidate.getTop() - 150)), 200);
                }
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
                    if (assignedSection == null || assignedSection.isEmpty() || assignedSection.equalsIgnoreCase("5A") || assignedSection.equals("--")) {
                        assignedSection = "6";
                    }
                    sessionManager.setAssignedSection(assignedSection);

                    tvHeaderSubtitle.setText(name + " (" + dept + " | Sec: " + assignedSection + ")");
                    tvHeaderTitle.setText("Admin Console · Sec " + assignedSection);

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
                                if (assignedSection == null || assignedSection.isEmpty() || assignedSection.equalsIgnoreCase("5A") || assignedSection.equals("--")) {
                                    assignedSection = "6";
                                }
                                sessionManager.setAssignedSection(assignedSection);

                                tvHeaderSubtitle.setText(name + " (" + dept + " | Sec: " + assignedSection + ")");
                                tvHeaderTitle.setText("Admin Console · Sec " + assignedSection);

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

        MaterialButton btnOpenDrawer = findViewById(R.id.btnOpenDrawer);
        drawerLayoutTeacher = findViewById(R.id.drawerLayoutTeacher);
        navViewTeacher = findViewById(R.id.navViewTeacher);

        if (btnOpenDrawer != null) {
            btnOpenDrawer.setOnClickListener(v -> {
                if (drawerLayoutTeacher != null) {
                    if (drawerLayoutTeacher.isDrawerOpen(GravityCompat.START)) {
                        drawerLayoutTeacher.closeDrawer(GravityCompat.START);
                    } else {
                        drawerLayoutTeacher.openDrawer(GravityCompat.START);
                    }
                }
            });
        }

        if (navViewTeacher != null) {
            View headerView = navViewTeacher.getHeaderView(0);
            if (headerView != null) {
                TextView tvNavName = headerView.findViewById(R.id.tvNavHeaderName);
                TextView tvNavRole = headerView.findViewById(R.id.tvNavHeaderRole);
                ImageView imgNavProfile = headerView.findViewById(R.id.imgNavHeaderProfile);
                if (tvNavName != null) tvNavName.setText(sessionManager.getUserName());
                if (tvNavRole != null) tvNavRole.setText("Teacher / Admin • " + sessionManager.getUserDept());
                String cachedImg = sessionManager.getProfileImageUrl();
                if (imgNavProfile != null && cachedImg != null && !cachedImg.isEmpty()) {
                    Glide.with(this).load(cachedImg).placeholder(R.drawable.ic_app_main).error(R.drawable.ic_app_main).circleCrop().into(imgNavProfile);
                }
            }

            navViewTeacher.setNavigationItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.drawer_teacher_stats) {
                    View targetView = findViewById(R.id.cardLiveStats);
                    if (scrollViewAdmin != null && targetView != null) {
                        scrollViewAdmin.smoothScrollTo(0, targetView.getTop());
                        targetView.requestFocus();
                    }
                } else if (id == R.id.drawer_teacher_control) {
                    View targetView = findViewById(R.id.cardLiveElectionControls);
                    if (scrollViewAdmin != null && targetView != null) {
                        scrollViewAdmin.smoothScrollTo(0, targetView.getTop());
                        targetView.requestFocus();
                    }
                } else if (id == R.id.drawer_teacher_candidates) {
                    View targetView = findViewById(R.id.cardAddCandidate);
                    if (scrollViewAdmin != null && targetView != null) {
                        scrollViewAdmin.smoothScrollTo(0, targetView.getTop());
                        targetView.requestFocus();
                    }
                } else if (id == R.id.drawer_teacher_manage_cr) {
                    showManageCRDialog();
                } else if (id == R.id.drawer_teacher_pdf) {
                    if (drawerLayoutTeacher != null) {
                        drawerLayoutTeacher.closeDrawer(GravityCompat.START);
                    }
                    new Handler(Looper.getMainLooper()).postDelayed(this::exportElectionResultsToPdf, 200);
                    return true;
                } else if (id == R.id.drawer_teacher_logout) {
                    sessionManager.logoutUser();
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                }
                if (drawerLayoutTeacher != null) {
                    drawerLayoutTeacher.closeDrawer(GravityCompat.START);
                }
                return true;
            });
        }

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
                showSnackBar("Publishing candidate...", false);

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

        // Safety Confirmation Bottom Sheet for Reset All
        btnResetVotes.setOnClickListener(v -> {
            String activeSec = getTeacherAssignedSection();
            BottomSheetDialog sheet = new BottomSheetDialog(this);
            LinearLayout view = new LinearLayout(this);
            view.setOrientation(LinearLayout.VERTICAL);
            view.setPadding(48, 48, 48, 48);
            view.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_sheet));

            TextView title = new TextView(this);
            title.setText("Reset Section " + activeSec + " Election Data?");
            title.setTextSize(20f);
            title.setTypeface(null, Typeface.BOLD);
            title.setTextColor(Color.parseColor("#DC2626"));

            TextView msg = new TextView(this);
            msg.setText("This will permanently delete published candidates, votes, and voter security locks for Section " + activeSec + ". Other sections will not be affected. Proceed?");
            msg.setTextSize(14f);
            msg.setTextColor(Color.parseColor("#64748B"));
            msg.setPadding(0, 12, 0, 24);

            LinearLayout btnLayout = new LinearLayout(this);
            btnLayout.setOrientation(LinearLayout.HORIZONTAL);
            btnLayout.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams btnLayoutLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            btnLayoutLp.setMargins(0, 8, 0, 0);
            btnLayout.setLayoutParams(btnLayoutLp);

            MaterialButton btnCancel = new MaterialButton(this);
            btnCancel.setText("Cancel");
            btnCancel.setTextColor(Color.parseColor("#0F172A"));
            btnCancel.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#F1F5F9")));
            btnCancel.setCornerRadius(12);
            btnCancel.setTypeface(null, Typeface.BOLD);
            btnCancel.setAllCaps(false);
            LinearLayout.LayoutParams lpCancel = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            lpCancel.setMargins(0, 0, 6, 0);
            btnCancel.setLayoutParams(lpCancel);
            btnCancel.setOnClickListener(dt -> sheet.dismiss());

            MaterialButton btnConfirm = new MaterialButton(this);
            btnConfirm.setText("Yes, Reset");
            btnConfirm.setTextColor(Color.parseColor("#FFFFFF"));
            btnConfirm.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#DC2626")));
            btnConfirm.setCornerRadius(12);
            btnConfirm.setTypeface(null, Typeface.BOLD);
            btnConfirm.setAllCaps(false);
            LinearLayout.LayoutParams lpConfirm = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            lpConfirm.setMargins(6, 0, 0, 0);
            btnConfirm.setLayoutParams(lpConfirm);

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
                                            "Election Portal Reset (Sec " + activeSec + ")",
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

        // Safety Confirmation Bottom Sheet for Run-Off
        btnRunOff.setOnClickListener(v -> {
            String activeSec = getTeacherAssignedSection();
            BottomSheetDialog sheet = new BottomSheetDialog(this);
            LinearLayout view = new LinearLayout(this);
            view.setOrientation(LinearLayout.VERTICAL);
            view.setPadding(48, 48, 48, 48);
            view.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_sheet));

            TextView title = new TextView(this);
            title.setText("Initialize Run-Off for Section " + activeSec + "?");
            title.setTextSize(20f);
            title.setTypeface(null, Typeface.BOLD);
            title.setTextColor(Color.parseColor("#0066FF"));

            TextView msg = new TextView(this);
            msg.setText("This will reset vote counts to 0 for candidates in Section " + activeSec + " while keeping the candidate list. Voter security locks for Section " + activeSec + " will be cleared. Proceed?");
            msg.setTextSize(14f);
            msg.setTextColor(Color.parseColor("#64748B"));
            msg.setPadding(0, 12, 0, 24);

            LinearLayout btnLayout = new LinearLayout(this);
            btnLayout.setOrientation(LinearLayout.HORIZONTAL);
            btnLayout.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams btnLayoutLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            btnLayoutLp.setMargins(0, 8, 0, 0);
            btnLayout.setLayoutParams(btnLayoutLp);

            MaterialButton btnCancel = new MaterialButton(this);
            btnCancel.setText("Cancel");
            btnCancel.setTextColor(Color.parseColor("#0F172A"));
            btnCancel.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#F1F5F9")));
            btnCancel.setCornerRadius(12);
            btnCancel.setTypeface(null, Typeface.BOLD);
            btnCancel.setAllCaps(false);
            LinearLayout.LayoutParams lpCancel = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            lpCancel.setMargins(0, 0, 6, 0);
            btnCancel.setLayoutParams(lpCancel);
            btnCancel.setOnClickListener(dt -> sheet.dismiss());

            MaterialButton btnConfirm = new MaterialButton(this);
            btnConfirm.setText("Proceed");
            btnConfirm.setTextColor(Color.parseColor("#FFFFFF"));
            btnConfirm.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#0066FF")));
            btnConfirm.setCornerRadius(12);
            btnConfirm.setTypeface(null, Typeface.BOLD);
            btnConfirm.setAllCaps(false);
            LinearLayout.LayoutParams lpConfirm = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            lpConfirm.setMargins(6, 0, 0, 0);
            btnConfirm.setLayoutParams(lpConfirm);

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

        if (bottomNavTeacher != null) {
            HapticUtils.attachBottomNav(bottomNavTeacher);
        }
    }

    private void displayAdminCandidatesSnapshot(DataSnapshot snapshot) {
        try {
            if (isFinishing() || isDestroyed()) return;
            String activeSec = getTeacherAssignedSection();
            sectionCandidateIds.clear();

            int totalCandidates = 0;
            int totalVotes = 0;

            List<Candidate> candidateList = new ArrayList<>();
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

            tvTotalCandidates.setText("Total (Sec " + activeSec + "): " + totalCandidates);
            tvStatTotalVotes.setText(String.valueOf(totalVotes));
        } catch (Exception e) {
            Log.e("TeacherDashboard", "Error loading candidates list", e);
        }
    }

    private void updateVoteDistributionChart(List<Candidate> candidateList, int totalVotes, LinearLayout container) {
        try {
            if (candidateList == null || container == null) return;

            // Chart Card (Requirement 1: padding 20dp, 16dp gap between title, bar, legend)
            MaterialCardView chartCard = new MaterialCardView(this);
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, -2);
            cardParams.setMargins(0, 0, 0, dpToPx(16));
            chartCard.setLayoutParams(cardParams);
            chartCard.setRadius(dpToPx(20));
            chartCard.setCardElevation(dpToPx(2));
            chartCard.setCardBackgroundColor(Color.parseColor("#FFFFFF"));
            chartCard.setStrokeWidth(dpToPx(1));
            chartCard.setStrokeColor(Color.parseColor("#E2E8F0"));

            LinearLayout chartLayout = new LinearLayout(this);
            chartLayout.setOrientation(LinearLayout.VERTICAL);
            int p20 = dpToPx(20);
            chartLayout.setPadding(p20, p20, p20, p20);

            // Title: "Live vote distribution" (sentence case), 15sp, w700 (bold), #0F172A
            TextView tvChartTitle = new TextView(this);
            tvChartTitle.setText("Live vote distribution");
            tvChartTitle.setTextSize(15f);
            tvChartTitle.setTypeface(null, Typeface.BOLD);
            tvChartTitle.setTextColor(Color.parseColor("#0F172A"));
            LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(-2, -2);
            titleLp.setMargins(0, 0, 0, dpToPx(16));
            tvChartTitle.setLayoutParams(titleLp);
            chartLayout.addView(tvChartTitle);

            // Distribution bar: 10dp tall, fully rounded (5dp), 2dp gap between segments
            FrameLayout barTrackFrame = new FrameLayout(this);
            LinearLayout.LayoutParams trackLp = new LinearLayout.LayoutParams(-1, dpToPx(10));
            trackLp.setMargins(0, 0, 0, dpToPx(16));
            barTrackFrame.setLayoutParams(trackLp);

            GradientDrawable trackBg = new GradientDrawable();
            trackBg.setCornerRadius(dpToPx(5));
            trackBg.setColor(Color.parseColor("#E2E8F0"));
            barTrackFrame.setBackground(trackBg);

            LinearLayout stackedBar = new LinearLayout(this);
            stackedBar.setOrientation(LinearLayout.HORIZONTAL);
            stackedBar.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));

            GradientDrawable barClipBg = new GradientDrawable();
            barClipBg.setCornerRadius(dpToPx(5));
            barClipBg.setColor(Color.TRANSPARENT);
            stackedBar.setBackground(barClipBg);
            stackedBar.setClipToOutline(true);

            int[] colors = {
                Color.parseColor("#0066FF"), // Blue
                Color.parseColor("#7C3AED"), // Purple
                Color.parseColor("#16A34A"), // Green
                Color.parseColor("#F59E0B"), // Amber
                Color.parseColor("#EC4899"), // Pink
                Color.parseColor("#06B6D4")  // Cyan
            };

            if (totalVotes == 0) {
                // Track #E2E8F0 when there are no votes
                View emptyTrack = new View(this);
                emptyTrack.setLayoutParams(new LinearLayout.LayoutParams(-1, -1));
                emptyTrack.setBackgroundColor(Color.parseColor("#E2E8F0"));
                stackedBar.addView(emptyTrack);
            } else {
                stackedBar.setWeightSum(totalVotes);
                int segIndex = 0;
                for (int i = 0; i < candidateList.size(); i++) {
                    Candidate c = candidateList.get(i);
                    int vts = Math.max(0, c.getVotes());
                    if (vts <= 0) continue;

                    int color = colors[i % colors.length];
                    View segment = new View(this);
                    LinearLayout.LayoutParams segParams = new LinearLayout.LayoutParams(0, -1, (float) vts);
                    if (segIndex > 0) {
                        segParams.setMargins(dpToPx(2), 0, 0, 0); // 2dp gap between segments
                    }
                    segment.setLayoutParams(segParams);
                    segment.setBackgroundColor(color);
                    stackedBar.addView(segment);
                    segIndex++;
                }
            }

            barTrackFrame.addView(stackedBar);
            chartLayout.addView(barTrackFrame);

            // Legend: one row per candidate, 8dp gap, 13sp
            // Row = 10dp square (3dp radius) in palette color + candidate name (#0F172A) + right-aligned muted text (#64748B) "N votes (P%)"
            LinearLayout legendLayout = new LinearLayout(this);
            legendLayout.setOrientation(LinearLayout.VERTICAL);
            legendLayout.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));

            for (int i = 0; i < candidateList.size(); i++) {
                Candidate c = candidateList.get(i);
                int vts = Math.max(0, c.getVotes());
                String cName = c.getName() != null ? c.getName() : "Candidate";

                int color = colors[i % colors.length];
                int percentage = totalVotes > 0 ? (int) Math.round((double) vts * 100.0 / totalVotes) : 0;

                LinearLayout legendRow = new LinearLayout(this);
                legendRow.setOrientation(LinearLayout.HORIZONTAL);
                legendRow.setGravity(Gravity.CENTER_VERTICAL);
                LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(-1, -2);
                if (i < candidateList.size() - 1) {
                    rowLp.setMargins(0, 0, 0, dpToPx(8)); // 8dp gap between rows
                }
                legendRow.setLayoutParams(rowLp);

                // 10dp square (3dp radius) in palette color
                View square = new View(this);
                int sqSize = dpToPx(10);
                LinearLayout.LayoutParams sqParams = new LinearLayout.LayoutParams(sqSize, sqSize);
                sqParams.setMargins(0, 0, dpToPx(8), 0);
                square.setLayoutParams(sqParams);

                GradientDrawable sqBg = new GradientDrawable();
                sqBg.setCornerRadius(dpToPx(3));
                sqBg.setColor(color);
                square.setBackground(sqBg);

                // Candidate Name (#0F172A, 13sp)
                TextView tvName = new TextView(this);
                tvName.setText(cName);
                tvName.setTextSize(13f);
                tvName.setTextColor(Color.parseColor("#0F172A"));
                tvName.setSingleLine(true);
                tvName.setEllipsize(TextUtils.TruncateAt.END);
                LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(0, -2, 1f);
                tvName.setLayoutParams(nameParams);

                // Right-aligned muted text (#64748B) "N votes (P%)"
                TextView tvMutedText = new TextView(this);
                tvMutedText.setText(vts + " votes (" + percentage + "%)");
                tvMutedText.setTextSize(13f);
                tvMutedText.setTextColor(Color.parseColor("#64748B"));
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
                    showSnackBar("Successfully added candidate to Section " + teacherSec + "!", false);
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
                    showSnackBar("Error: " + e.getMessage(), true);
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

    private void showSnackBar(String message, boolean isError) {
        try {
            View rootView = findViewById(android.R.id.content);
            if (rootView != null) {
                Snackbar snackbar = Snackbar.make(rootView, message, Snackbar.LENGTH_LONG);
                if (isError) {
                    snackbar.setBackgroundTint(Color.parseColor("#DC2626"));
                } else {
                    snackbar.setBackgroundTint(Color.parseColor("#0F172A"));
                }
                snackbar.setTextColor(Color.WHITE);
                snackbar.show();
            } else {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        }
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
            showSnackBar("Cannot extend time: Live voting is currently Closed. Open the gate first.", true);
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
            showSnackBar("Election time extended by +" + minutesAdded + " minutes for Section " + teacherSec, false);
            logAdminAction("Extended Election Time by +" + minutesAdded + " min for Section " + teacherSec);
            broadcastNotificationToStudents(
                "⏰ Voting Time Extended (Sec " + teacherSec + ")",
                "Election admin added +" + minutesAdded + " minutes of voting time for Section " + teacherSec + "!",
                "TIMER_EXTEND"
            );
        });
    }

    private void reduceElectionTime(long durationToReduceMs) {
        if (!currentIsGateOpen) {
            showSnackBar("Live voting is currently Closed.", true);
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
            showSnackBar("Election time reduced by -" + minutesReduced + " minutes", false);
            logAdminAction("Reduced Election Time by -" + minutesReduced + " min");
        });
    }

    private void togglePauseResume() {
        if (!currentIsGateOpen) {
            showSnackBar("Cannot pause/resume: Live voting is Closed.", true);
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
                showSnackBar("Live voting RESUMED for Section " + teacherSec, false);
                logAdminAction("Resumed Live Voting for Section " + teacherSec);
                broadcastNotificationToStudents(
                    "▶️ Voting Resumed (Sec " + teacherSec + ")",
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
                showSnackBar("Live voting PAUSED for Section " + teacherSec, false);
                logAdminAction("Paused Live Voting for Section " + teacherSec);
                broadcastNotificationToStudents(
                    "⏸️ Voting Paused (Sec " + teacherSec + ")",
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
            showSnackBar("Live voting ENDED for Section " + teacherSec + ".", false);
            logAdminAction("Forcefully Ended Live Voting for Section " + teacherSec);
            broadcastNotificationToStudents(
                "🔴 Voting Concluded (Sec " + teacherSec + ")",
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

            DatabaseReference studentsRef = !sanitizedSec.equals("--") ?
                    dbRef.child("sections").child(sanitizedDept).child(sanitizedSec).child("students") :
                    dbRef.child("users");

            studentsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    List<String> studentNames = new ArrayList<>();
                    List<String> studentUids = new ArrayList<>();
                    List<Boolean> isCrList = new ArrayList<>();

                    for (DataSnapshot ds : snapshot.getChildren()) {
                        String role = ds.child("role").getValue(String.class);
                        String sec = ds.child("section").getValue(String.class);
                        if (sec == null) sec = ds.child("assignedSection").getValue(String.class);

                        boolean isStudentRole = role == null || role.equalsIgnoreCase("Student") || role.equalsIgnoreCase("CR");
                        boolean isThisSec = sec == null || sec.equals("--") || sec.equalsIgnoreCase(activeSec) || activeSec.equals("--");

                        if (isStudentRole && isThisSec) {
                            String sName = ds.child("name").getValue(String.class);
                            String sId = ds.child("id").getValue(String.class);
                            if (sName == null) sName = "Student";
                            boolean isCr = "CR".equalsIgnoreCase(role);

                            studentNames.add(sName + (sId != null ? " (" + sId + ")" : "") + (isCr ? " [CR]" : ""));
                            studentUids.add(ds.getKey());
                            isCrList.add(isCr);
                        }
                    }

                    if (studentNames.isEmpty()) {
                        showSnackBar("No students registered in Section " + activeSec + " yet.", true);
                        return;
                    }

                    CharSequence[] items = studentNames.toArray(new CharSequence[0]);
                    new MaterialAlertDialogBuilder(TeacherDashboardActivity.this)
                        .setTitle("Manage CR (Promote / Demote) - Sec " + activeSec)
                        .setItems(items, (dialog, which) -> {
                            String targetUid = studentUids.get(which);
                            boolean currentlyCr = isCrList.get(which);
                            String newRole = currentlyCr ? "Student" : "CR";

                            Map<String, Object> roleUpdates = new HashMap<>();
                            roleUpdates.put("users/" + targetUid + "/role", newRole);
                            if (!sanitizedSec.equals("--")) {
                                roleUpdates.put("sections/" + sanitizedDept + "/" + sanitizedSec + "/students/" + targetUid + "/role", newRole);
                            }

                            dbRef.updateChildren(roleUpdates).addOnSuccessListener(unused -> {
                                String msgStr = currentlyCr ? "Demoted from CR to Student." : "Promoted to Class Representative (CR)!";
                                showSnackBar("Updated student role: " + msgStr, false);
                                logAdminAction((currentlyCr ? "Demoted CR " : "Promoted CR ") + studentNames.get(which));
                            });
                        })
                        .setNegativeButton("Close", null)
                        .show();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
        } catch (Exception e) {
            Log.e("TeacherDashboard", "Error showing Manage CR dialog", e);
        }
    }

    private void setPdfExportLoading(boolean loading) {
        isGeneratingPdf = loading;
        if (btnExportPdf != null) {
            if (loading) {
                try {
                    CircularProgressIndicatorSpec spec = new CircularProgressIndicatorSpec(
                            this, null, 0, com.google.android.material.R.style.Widget_Material3_CircularProgressIndicator_ExtraSmall);
                    spec.indicatorColors = new int[]{Color.WHITE};
                    IndeterminateDrawable<CircularProgressIndicatorSpec> progressDrawable =
                            IndeterminateDrawable.createCircularDrawable(this, spec);

                    btnExportPdf.setIcon(progressDrawable);
                } catch (Exception ignored) {
                }
                btnExportPdf.setText("Generating…");
                btnExportPdf.setEnabled(false);
            } else {
                btnExportPdf.setIcon(null);
                btnExportPdf.setText("Export results to PDF");
                btnExportPdf.setEnabled(true);
            }
        }
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
            showSnackBar("Generating official election PDF report...", false);
            String activeSec = getTeacherAssignedSection();
            String activeDept = getTeacherDepartment();

            dbRef.child("candidates").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    List<PdfReportGenerator.CandidateResult> candidateResults = new ArrayList<>();
                    int totalVotes = 0;
                    int rank = 1;

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
                                rank++
                            ));
                        }
                    }

                    final int finalTotalVotes = totalVotes;
                    Executors.newSingleThreadExecutor().execute(() -> {
                        try {
                            File pdfFile = PdfReportGenerator.generatePdfDocument(
                                TeacherDashboardActivity.this,
                                sessionManager != null ? sessionManager.getUserName() : "Teacher Admin",
                                activeDept,
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
                                    showSnackBar("PDF Report generated: " + pdfFile.getName(), false);

                                    if (getSupportFragmentManager().findFragmentByTag(PdfReadyBottomSheet.TAG) == null) {
                                        PdfReadyBottomSheet bottomSheet = PdfReadyBottomSheet.newInstance(pdfFile);
                                        bottomSheet.showAllowingStateLoss(getSupportFragmentManager(), PdfReadyBottomSheet.TAG);
                                        Log.d("TeacherDashboard", "PdfReadyBottomSheet shown");
                                    }
                                } else {
                                    Log.e("TeacherDashboard", "PDF generation failed: pdfFile is null or missing");
                                    Toast.makeText(TeacherDashboardActivity.this, "PDF generation failed: File was not created", Toast.LENGTH_LONG).show();
                                }
                            });
                        } catch (Exception e) {
                            Log.e("TeacherDashboard", "Error generating PDF document: " + e.getMessage(), e);
                            runOnUiThread(() -> {
                                setPdfExportLoading(false);
                                if (!isFinishing() && !isDestroyed()) {
                                    Toast.makeText(TeacherDashboardActivity.this, "PDF generation failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    });
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("TeacherDashboard", "PDF data fetching cancelled: " + error.getMessage(), error.toException());
                    runOnUiThread(() -> {
                        setPdfExportLoading(false);
                        if (!isFinishing() && !isDestroyed()) {
                            Toast.makeText(TeacherDashboardActivity.this, "PDF generation error: " + error.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            });
        } catch (Exception e) {
            Log.e("TeacherDashboard", "Error starting PDF export: " + e.getMessage(), e);
            setPdfExportLoading(false);
            Toast.makeText(this, "PDF generation failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
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
            Toast.makeText(this, "PDF saved to: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        }
    }

    private void uploadProfilePictureToFirebase(Uri imageUri) {
        try {
            showSnackBar("Uploading profile picture...", false);
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

            showSnackBar("Profile picture updated successfully!", false);
        } catch (Exception e) {
            Log.e("TeacherDashboard", "Error uploading profile image", e);
            showSnackBar("Failed to upload profile picture.", true);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timerHandler != null && timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
        }
        if (votingControlRef != null && votingControlListener != null) {
            votingControlRef.removeEventListener(votingControlListener);
        }
    }
}
