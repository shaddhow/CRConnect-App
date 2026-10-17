package com.example.crconnect;

import com.example.crconnect.core.utils.HapticUtils;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import android.app.Dialog;
import android.graphics.drawable.ColorDrawable;
import android.view.Window;
import android.view.WindowManager;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.WriteBatch;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class RoutineActivity extends AppCompatActivity {

    private SessionManager sessionManager;
    private TextView tvGreetingTitle, tvHeaderDate;
    private View cardUpNext;
    private TextView tvUpNextSubject, tvUpNextCourse, tvUpNextFaculty, tvUpNextTime, tvUpNextRoom, tvUpNextCountdown;
    private TextView tvDailyRoutineTitle, tvNoRoutineClasses;
    private RecyclerView recyclerViewRoutine;
    private FloatingActionButton fabAddRoutineClass;
    private BottomNavigationView bottomNavRoutine;
    private LinearLayout containerDayPicker;

    private View layoutEmptyState;
    private TextView tvEmptyStateTitle, tvEmptyStateSubtitle;
    private MaterialButton btnAddClassEmptyState;

    private final String[] DAYS = {"Saturday", "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday"};
    private String selectedDay = "Saturday";
    private int weekOffset = 0;

    private final List<RoutineClassItem> routineList = new ArrayList<>();
    private RoutineAdapter routineAdapter;
    private ListenerRegistration routineListenerRegistration;

    private final Handler countdownHandler = new Handler(Looper.getMainLooper());
    private Runnable countdownRunnable;

    private ActivityResultLauncher<String> galleryLauncher;
    private ActivityResultLauncher<Intent> cameraLauncher;
    private Uri photoUri;

    // 6 BUBT Routine Grid Time Slot Columns
    private final String[] BUBT_TIME_SLOTS = {
            "08:15 AM - 09:45 AM",
            "09:45 AM - 11:15 AM",
            "11:15 AM - 12:45 PM",
            "01:15 PM - 02:45 PM",
            "02:45 PM - 04:15 PM",
            "04:15 PM - 05:45 PM"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getWindow().setNavigationBarContrastEnforced(false);
        }
        WindowInsetsControllerCompat insetsController = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (insetsController != null) {
            insetsController.setAppearanceLightStatusBars(false);
            insetsController.setAppearanceLightNavigationBars(false);
        }

        setContentView(R.layout.activity_routine);

        sessionManager = new SessionManager(this);

        tvGreetingTitle = findViewById(R.id.tvGreetingTitle);
        tvHeaderDate = findViewById(R.id.tvHeaderDate);
        cardUpNext = findViewById(R.id.cardUpNext);
        tvUpNextSubject = findViewById(R.id.tvUpNextSubject);
        tvUpNextCourse = tvUpNextSubject;
        tvUpNextFaculty = findViewById(R.id.tvUpNextFaculty);
        tvUpNextTime = findViewById(R.id.tvUpNextTime);
        tvUpNextRoom = findViewById(R.id.tvUpNextRoom);
        tvUpNextCountdown = findViewById(R.id.tvUpNextCountdown);
        tvDailyRoutineTitle = findViewById(R.id.tvDailyRoutineTitle);
        tvNoRoutineClasses = findViewById(R.id.tvNoRoutineClasses);
        recyclerViewRoutine = findViewById(R.id.recyclerViewRoutine);
        fabAddRoutineClass = findViewById(R.id.fabAddRoutineClass);
        containerDayPicker = findViewById(R.id.containerDayPicker);

        layoutEmptyState = findViewById(R.id.layoutEmptyState);
        tvEmptyStateTitle = findViewById(R.id.tvEmptyStateTitle);
        tvEmptyStateSubtitle = findViewById(R.id.tvEmptyStateSubtitle);
        btnAddClassEmptyState = findViewById(R.id.btnAddClassEmptyState);

        if (btnAddClassEmptyState != null) {
            btnAddClassEmptyState.setOnClickListener(v -> {
                HapticUtils.mediumClick(v);
                showScanOptionsBottomSheet();
            });
        }

        View btnRoutineBack = findViewById(R.id.btnRoutineBack);
        View btnScanRoutine = findViewById(R.id.btnScanRoutine);
        TextView btnPrevWeek = findViewById(R.id.btnPrevWeek);
        TextView btnNextWeek = findViewById(R.id.btnNextWeek);

        View headerView = findViewById(R.id.layoutRoutineHeader);
        if (headerView != null) {
            final int initialTopPadding = headerView.getPaddingTop();
            ViewCompat.setOnApplyWindowInsetsListener(headerView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(v.getPaddingLeft(), initialTopPadding + systemBars.top, v.getPaddingRight(), v.getPaddingBottom());
                return insets;
            });
        }

        View scrollViewRoutine = findViewById(R.id.scrollViewRoutine);
        if (scrollViewRoutine != null) {
            final int initialBottomPadding = scrollViewRoutine.getPaddingBottom();
            ViewCompat.setOnApplyWindowInsetsListener(scrollViewRoutine, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), initialBottomPadding + systemBars.bottom);
                return WindowInsetsCompat.CONSUMED;
            });
        }

        if (fabAddRoutineClass != null) {
            ViewCompat.setOnApplyWindowInsetsListener(fabAddRoutineClass, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
                int baseMargin = (int) (24 * getResources().getDisplayMetrics().density);
                lp.bottomMargin = baseMargin + systemBars.bottom;
                v.setLayoutParams(lp);
                return insets;
            });
        }

        bottomNavRoutine = findViewById(R.id.bottomNavRoutine);
        if (bottomNavRoutine != null) {
            ViewCompat.setOnApplyWindowInsetsListener(bottomNavRoutine, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(0, 0, 0, systemBars.bottom);
                return insets;
            });

            String userRole = sessionManager != null ? sessionManager.getUserRole() : null;
            if (userRole != null && (userRole.toLowerCase().contains("teacher") || userRole.toLowerCase().contains("admin"))) {
                bottomNavRoutine.getMenu().clear();
                bottomNavRoutine.inflateMenu(R.menu.bottom_nav_menu_teacher);
                bottomNavRoutine.setOnItemSelectedListener(item -> {
                    int id = item.getItemId();
                    Intent intent = new Intent(RoutineActivity.this, TeacherDashboardActivity.class);
                    intent.putExtra("EXTRA_TARGET_TAB", id);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                    return true;
                });
            } else {
                bottomNavRoutine.setSelectedItemId(R.id.nav_schedule);
                bottomNavRoutine.setOnItemSelectedListener(item -> {
                    int id = item.getItemId();
                    if (id == R.id.nav_schedule) {
                        if (scrollViewRoutine instanceof NestedScrollView) {
                            ((NestedScrollView) scrollViewRoutine).smoothScrollTo(0, 0);
                        }
                        return true;
                    }
                    Intent intent = new Intent(RoutineActivity.this, StudentDashboardActivity.class);
                    intent.putExtra("EXTRA_TARGET_TAB", id);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                    return true;
                });
            }
            HapticUtils.attachBottomNav(bottomNavRoutine);
        }

        if (btnRoutineBack != null) {
            btnRoutineBack.setOnClickListener(v -> {
                HapticUtils.lightClick(v);
                finish();
            });
        }

        if (btnScanRoutine != null) {
            btnScanRoutine.setOnClickListener(v -> {
                HapticUtils.mediumClick(v);
                showScanOptionsBottomSheet();
            });
        }

        if (btnPrevWeek != null) {
            btnPrevWeek.setOnClickListener(v -> {
                HapticUtils.lightClick(v);
                weekOffset--;
                setupDayPicker();
                loadRoutineForDay(selectedDay);
            });
        }

        if (btnNextWeek != null) {
            btnNextWeek.setOnClickListener(v -> {
                HapticUtils.lightClick(v);
                weekOffset++;
                setupDayPicker();
                loadRoutineForDay(selectedDay);
            });
        }

        String role = sessionManager.getUserRole();
        boolean canEdit = role != null && (role.equalsIgnoreCase("CR") || role.equalsIgnoreCase("Teacher/Admin") || role.equalsIgnoreCase("Teacher"));
        if (canEdit) {
            fabAddRoutineClass.setVisibility(View.VISIBLE);
            fabAddRoutineClass.setOnClickListener(v -> {
                HapticUtils.mediumClick(v);
                showScanOptionsBottomSheet();
            });
        } else {
            fabAddRoutineClass.setVisibility(View.GONE);
        }

        // Attach global haptics across all interactive views in activity layout
        HapticUtils.attachHapticsToAllInteractiveViews(findViewById(android.R.id.content));

        recyclerViewRoutine.setLayoutManager(new LinearLayoutManager(this));
        routineAdapter = new RoutineAdapter(routineList, canEdit, this);
        recyclerViewRoutine.setAdapter(routineAdapter);

        galleryLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                processRoutineImageUri(uri);
            }
        });

        cameraLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && photoUri != null) {
                processRoutineImageUri(photoUri);
            }
        });

        updateHeaderGreetingAndDate();

        selectedDay = getTodayDayName();
        setupDayPicker();
        checkInitialRoutineState();
        startUpNextCountdownTimer();
    }

    private void checkInitialRoutineState() {
        // 1. Check local storage / cache first for instant persistence on launch
        List<RoutineClassItem> cachedItems = RoutineLocalCache.loadRoutineLocally(this);
        List<RoutineClassItem> dayClasses = new ArrayList<>();
        if (cachedItems != null && !cachedItems.isEmpty()) {
            for (RoutineClassItem item : cachedItems) {
                if (item != null && item.day != null && matchesSelectedDay(item.day, selectedDay)) {
                    dayClasses.add(item);
                }
            }
            sortClassesChronologically(dayClasses);
        }

        routineList.clear();
        routineList.addAll(dayClasses);
        if (routineAdapter != null) {
            routineAdapter.notifyDataSetChanged();
        }

        updateUiVisibilityAndSummary();
        updateUpNextBanner();
        loadRoutineForDay(selectedDay);
    }

    private void updateUiVisibilityAndSummary() {
        if (tvDailyRoutineTitle != null) {
            tvDailyRoutineTitle.setText(String.format("%s Classes", selectedDay));
        }

        TextView tvClassSummary = findViewById(R.id.tvClassSummary);

        if (!routineList.isEmpty()) {
            if (recyclerViewRoutine != null) recyclerViewRoutine.setVisibility(View.VISIBLE);
            if (layoutEmptyState != null) layoutEmptyState.setVisibility(View.GONE);
            if (tvNoRoutineClasses != null) tvNoRoutineClasses.setVisibility(View.GONE);

            if (tvClassSummary != null) {
                String startTime = routineList.get(0).getStartTime();
                String endTime = routineList.get(routineList.size() - 1).getEndTime();
                String summaryStr = routineList.size() + (routineList.size() == 1 ? " class" : " classes");
                if (!startTime.isEmpty() && !endTime.isEmpty()) {
                    summaryStr += " • " + startTime + " – " + endTime;
                }
                tvClassSummary.setText(summaryStr);
                tvClassSummary.setVisibility(View.VISIBLE);
            }
        } else {
            if (recyclerViewRoutine != null) recyclerViewRoutine.setVisibility(View.GONE);
            if (layoutEmptyState != null) layoutEmptyState.setVisibility(View.VISIBLE);
            if (tvNoRoutineClasses != null) tvNoRoutineClasses.setVisibility(View.GONE);

            if (tvEmptyStateTitle != null) {
                tvEmptyStateTitle.setText("Nothing on " + selectedDay);
            }

            if (btnAddClassEmptyState != null) {
                String shortDay = selectedDay.length() >= 3 ? selectedDay.substring(0, 3) : selectedDay;
                btnAddClassEmptyState.setText("+ Add a class on " + shortDay);
            }

            if (tvClassSummary != null) {
                tvClassSummary.setText("No classes");
                tvClassSummary.setVisibility(View.VISIBLE);
            }
        }
    }

    private void updateHeaderGreetingAndDate() {
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);

        String quote;
        if (hour < 5) {
            quote = "Burning the midnight oil";
        } else if (hour < 12) {
            quote = "Rise and shine";
        } else if (hour < 17) {
            quote = "Afternoon hustle";
        } else {
            quote = "Winding down for the day";
        }

        if (tvGreetingTitle != null) {
            tvGreetingTitle.setText(quote);
        }

        if (tvHeaderDate != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, MMM d", Locale.getDefault());
            tvHeaderDate.setText(dateFormat.format(new Date()));
        }
    }

    private String getTodayDayName() {
        Calendar calendar = Calendar.getInstance();
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        switch (dayOfWeek) {
            case Calendar.SATURDAY:
                return "Saturday";
            case Calendar.SUNDAY:
                return "Sunday";
            case Calendar.MONDAY:
                return "Monday";
            case Calendar.TUESDAY:
                return "Tuesday";
            case Calendar.WEDNESDAY:
                return "Wednesday";
            case Calendar.THURSDAY:
                return "Thursday";
            case Calendar.FRIDAY:
                return "Friday";
            default:
                return "Saturday";
        }
    }

    private boolean checkIfDayHasClasses(String dayName) {
        List<RoutineClassItem> cached = RoutineLocalCache.loadRoutineLocally(this);
        if (cached != null) {
            for (RoutineClassItem item : cached) {
                if (item != null && item.day != null && matchesSelectedDay(item.day, dayName)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void setupDayPicker() {
        if (containerDayPicker == null) return;
        containerDayPicker.removeAllViews();

        Calendar cal = Calendar.getInstance();
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        int daysFromSat = (dayOfWeek % 7);
        cal.add(Calendar.DAY_OF_YEAR, -daysFromSat + (weekOffset * 7));

        SimpleDateFormat numFormat = new SimpleDateFormat("d", Locale.getDefault());

        for (String dayName : DAYS) {
            String dayNum = numFormat.format(cal.getTime());
            cal.add(Calendar.DAY_OF_YEAR, 1);

            View pillView = createDayPillView(dayName, dayNum);
            containerDayPicker.addView(pillView);
        }

        highlightSelectedDay(selectedDay);
    }

    private View createDayPillView(String dayName, String dayNum) {
        View pillView = getLayoutInflater().inflate(R.layout.item_day_pill, containerDayPicker, false);
        pillView.setTag(dayName);

        TextView tvDay = pillView.findViewById(R.id.tvDayName);
        TextView tvNum = pillView.findViewById(R.id.tvDayNum);
        View viewDot = pillView.findViewById(R.id.viewDot);

        if (tvDay != null) {
            String shortDay = dayName.length() >= 3 ? dayName.substring(0, 3).toUpperCase() : dayName.toUpperCase();
            tvDay.setText(shortDay);
        }

        if (tvNum != null) {
            tvNum.setText(dayNum);
        }

        boolean hasClasses = checkIfDayHasClasses(dayName);
        if (viewDot != null) {
            viewDot.setVisibility(hasClasses ? View.VISIBLE : View.INVISIBLE);
        }

        pillView.setOnClickListener(v -> {
            HapticUtils.lightClick(v);
            selectedDay = dayName;
            highlightSelectedDay(dayName);
            loadRoutineForDay(dayName);
        });

        return pillView;
    }

    private void highlightSelectedDay(String activeDay) {
        if (containerDayPicker == null) return;
        for (int idx = 0; idx < containerDayPicker.getChildCount(); idx++) {
            View pillView = containerDayPicker.getChildAt(idx);
            Object tag = pillView.getTag();
            boolean isSelected = tag != null && matchesSelectedDay(tag.toString(), activeDay);

            TextView tvDay = pillView.findViewById(R.id.tvDayName);
            TextView tvNum = pillView.findViewById(R.id.tvDayNum);
            View viewDot = pillView.findViewById(R.id.viewDot);

            if (isSelected) {
                pillView.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_day_pill_active));
                if (tvDay != null) tvDay.setTextColor(Color.WHITE);
                if (tvNum != null) tvNum.setTextColor(Color.WHITE);
                if (viewDot != null) {
                    viewDot.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_indicator_dot_white));
                }
            } else {
                pillView.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_day_pill_inactive));
                if (tvDay != null) tvDay.setTextColor(Color.parseColor("#64748B"));
                if (tvNum != null) tvNum.setTextColor(Color.parseColor("#0F172A"));
                if (viewDot != null) {
                    viewDot.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_indicator_dot));
                }
            }
        }

        if (tvDailyRoutineTitle != null) {
            tvDailyRoutineTitle.setText(String.format("%s Classes", activeDay));
        }
    }

    private boolean matchesSelectedDay(String classDay, String targetDay) {
        if (classDay == null || targetDay == null) return false;
        String c = normalizeDayName(classDay);
        String t = normalizeDayName(targetDay);
        if (c != null && t != null) {
            return c.equalsIgnoreCase(t);
        }
        String cTrim = classDay.trim();
        String tTrim = targetDay.trim();
        if (cTrim.equalsIgnoreCase(tTrim)) return true;
        if (cTrim.length() >= 3 && tTrim.length() >= 3) {
            return cTrim.substring(0, 3).equalsIgnoreCase(tTrim.substring(0, 3));
        }
        return false;
    }

    private String getSanitizedKey(String val) {
        if (val == null || val.isEmpty()) return "default";
        return val.replace(".", "_").replace("/", "_").replace("#", "_").replace("$", "_").replace("[", "_").replace("]", "_").trim();
    }

    public void refreshDashboard() {
        runOnUiThread(() -> {
            try {
                if (isFinishing() || isDestroyed()) return;
                setupDayPicker();
                loadRoutineForDay(selectedDay);
                updateUpNextBanner();
            } catch (Exception e) {
                Log.e("RoutineActivity", "Error in refreshDashboard", e);
            }
        });
    }

    private List<RoutineClassItem> filterClassesForDay(List<RoutineClassItem> allItems, String targetDay) {
        List<RoutineClassItem> dayClasses = new ArrayList<>();
        if (allItems == null || targetDay == null) return dayClasses;

        for (RoutineClassItem item : allItems) {
            if (item == null || item.day == null) continue;

            String code = item.getDisplayCourseCode();
            if (code != null && (code.toLowerCase().contains("scanned notice") || code.toLowerCase().contains("notice:"))) {
                continue;
            }

            String norm = normalizeDayName(item.day);
            if (norm != null && matchesSelectedDay(norm, targetDay)) {
                dayClasses.add(item);
            }
        }
        return dayClasses;
    }

    private void loadRoutineForDay(String day) {
        selectedDay = day;
        highlightSelectedDay(day);

        String dept = getSanitizedKey(sessionManager.getUserDept());
        String intake = getSanitizedKey(sessionManager.getUserIntake());
        String section = getSanitizedKey(sessionManager.getUserSection());

        if (routineListenerRegistration != null) {
            routineListenerRegistration.remove();
            routineListenerRegistration = null;
        }

        routineListenerRegistration = FirebaseFirestore.getInstance().collection("routines")
                .document(dept)
                .collection(intake)
                .document(section)
                .collection("schedule")
                .addSnapshotListener((querySnapshot, error) -> {
                    List<RoutineClassItem> allItems = new ArrayList<>();

                    if (error == null && querySnapshot != null && !querySnapshot.isEmpty()) {
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            RoutineClassItem item = doc.toObject(RoutineClassItem.class);
                            if (item != null) {
                                item.id = doc.getId();
                                String itemDay = doc.getString("day");
                                if (itemDay == null || itemDay.trim().isEmpty()) {
                                    itemDay = item.getDay();
                                }
                                if (itemDay != null && !itemDay.trim().isEmpty()) {
                                    item.day = itemDay.trim();
                                }
                                allItems.add(item);
                            }
                        }
                    }

                    // Fallback to local cache if Firestore is empty or offline
                    if (allItems.isEmpty()) {
                        List<RoutineClassItem> cached = RoutineLocalCache.loadRoutineLocally(this);
                        if (cached != null) {
                            allItems.addAll(cached);
                        }
                    }

                    // Strict day filtering
                    List<RoutineClassItem> dayClasses = filterClassesForDay(allItems, day);

                    // Deduplicate by timeSlot + courseCode
                    List<RoutineClassItem> uniqueClasses = new ArrayList<>();
                    Set<String> seenKeys = new HashSet<>();
                    for (RoutineClassItem item : dayClasses) {
                        String key = ((item.day != null ? item.day : "") + "_" + (item.timeSlot != null ? item.timeSlot : "") + "_" + item.getDisplayCourseCode()).toLowerCase();
                        if (!seenKeys.contains(key)) {
                            seenKeys.add(key);
                            uniqueClasses.add(item);
                        }
                    }
                    dayClasses = uniqueClasses;

                    // Sort chronologically
                    sortClassesChronologically(dayClasses);

                    // Update routineList
                    routineList.clear();
                    routineList.addAll(dayClasses);
                    if (routineAdapter != null) {
                        routineAdapter.notifyDataSetChanged();
                    }

                    updateUiVisibilityAndSummary();
                    updateUpNextBannerWithItems(allItems);
                });
    }

    private void updateUpNextBanner() {
        String dept = getSanitizedKey(sessionManager.getUserDept());
        String intake = getSanitizedKey(sessionManager.getUserIntake());
        String section = getSanitizedKey(sessionManager.getUserSection());

        FirebaseFirestore.getInstance().collection("routines")
                .document(dept)
                .collection(intake)
                .document(section)
                .collection("schedule")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<RoutineClassItem> allItems = new ArrayList<>();
                    if (querySnapshot != null) {
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            RoutineClassItem item = doc.toObject(RoutineClassItem.class);
                            if (item != null) {
                                item.id = doc.getId();
                                String itemDay = doc.getString("day");
                                if (itemDay == null || itemDay.trim().isEmpty()) {
                                    itemDay = item.getDay();
                                }
                                if (itemDay != null) {
                                    item.day = itemDay;
                                }
                                allItems.add(item);
                            }
                        }
                    }
                    updateUpNextBannerWithItems(allItems);
                })
                .addOnFailureListener(e -> updateUpNextBannerWithItems(new ArrayList<>()));
    }

    private int parseTimeToMinutes(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) return -1;
        String cleaned = timeStr.trim().toUpperCase();
        if (cleaned.contains("-")) {
            cleaned = cleaned.split("-")[0].trim();
        }

        String[] formats = new String[]{"hh:mm a", "h:mm a", "HH:mm", "H:mm"};
        for (String fmt : formats) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(fmt, Locale.US);
                Date date = sdf.parse(cleaned);
                if (date != null) {
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(date);
                    return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);
                }
            } catch (Exception ignored) {}
        }
        return -1;
    }

    private void sortClassesChronologically(List<RoutineClassItem> list) {
        if (list == null || list.isEmpty()) return;
        Collections.sort(list, (a, b) -> {
            int startA = parseTimeToMinutes(a.getStartTime());
            int startB = parseTimeToMinutes(b.getStartTime());
            if (startA != -1 && startB != -1) {
                return Integer.compare(startA, startB);
            }
            return 0;
        });
    }

    private void updateUpNextBannerWithItems(List<RoutineClassItem> allItems) {
        if (cardUpNext == null) return;

        if (allItems == null || allItems.isEmpty()) {
            allItems = RoutineLocalCache.loadRoutineLocally(this);
        }

        if (allItems == null || allItems.isEmpty()) {
            cardUpNext.setVisibility(View.GONE);
            return;
        }

        // Filter out notice/corrupted OCR strings from Up Next calculation
        List<RoutineClassItem> validScheduleItems = new ArrayList<>();
        for (RoutineClassItem item : allItems) {
            if (item == null) continue;
            String code = item.getDisplayCourseCode();
            if (code != null && (code.toLowerCase().contains("scanned notice") || code.toLowerCase().contains("notice:"))) {
                continue;
            }
            validScheduleItems.add(item);
        }
        allItems = validScheduleItems;

        if (allItems.isEmpty()) {
            cardUpNext.setVisibility(View.GONE);
            return;
        }

        String todayName = getTodayDayName();
        Calendar nowCal = Calendar.getInstance();
        int nowMinutes = nowCal.get(Calendar.HOUR_OF_DAY) * 60 + nowCal.get(Calendar.MINUTE);

        // Filter today's classes
        List<RoutineClassItem> todayClasses = new ArrayList<>();
        for (RoutineClassItem item : allItems) {
            if (item != null && item.day != null && matchesSelectedDay(item.day, todayName)) {
                todayClasses.add(item);
            }
        }
        sortClassesChronologically(todayClasses);

        RoutineClassItem selectedUpNext = null;
        boolean isLiveNow = false;
        int diffMinutes = -1;
        int dayOffset = 0;

        // Check today's classes first
        for (RoutineClassItem item : todayClasses) {
            int startMins = parseTimeToMinutes(item.getStartTime());
            int endMins = parseTimeToMinutes(item.getEndTime());
            if (startMins == -1) startMins = 8 * 60 + 15;
            if (endMins == -1 || endMins <= startMins) endMins = startMins + 90;

            if (nowMinutes < endMins) {
                selectedUpNext = item;
                if (nowMinutes >= startMins) {
                    isLiveNow = true;
                    diffMinutes = endMins - nowMinutes;
                } else {
                    isLiveNow = false;
                    diffMinutes = startMins - nowMinutes;
                }
                break;
            }
        }

        // If no class remains for today, find class on upcoming days
        if (selectedUpNext == null) {
            int todayIdx = -1;
            for (int i = 0; i < DAYS.length; i++) {
                if (matchesSelectedDay(DAYS[i], todayName)) {
                    todayIdx = i;
                    break;
                }
            }
            if (todayIdx != -1) {
                for (int offset = 1; offset < 7; offset++) {
                    String nextDayName = DAYS[(todayIdx + offset) % 7];
                    List<RoutineClassItem> nextDayClasses = new ArrayList<>();
                    for (RoutineClassItem item : allItems) {
                        if (item != null && item.day != null && matchesSelectedDay(item.day, nextDayName)) {
                            nextDayClasses.add(item);
                        }
                    }
                    if (!nextDayClasses.isEmpty()) {
                        sortClassesChronologically(nextDayClasses);
                        selectedUpNext = nextDayClasses.get(0);
                        dayOffset = offset;
                        break;
                    }
                }
            }
        }

        if (selectedUpNext != null) {
            tvUpNextSubject.setText(selectedUpNext.getDisplayCourseCode());
            tvUpNextFaculty.setText("FC: " + selectedUpNext.getDisplayFaculty());

            String startStr = selectedUpNext.getStartTime();
            String endStr = selectedUpNext.getEndTime();
            if (!startStr.isEmpty() && !endStr.isEmpty()) {
                tvUpNextTime.setText(startStr + " – " + endStr);
            } else if (selectedUpNext.timeSlot != null) {
                tvUpNextTime.setText(selectedUpNext.timeSlot);
            } else {
                tvUpNextTime.setText("--");
            }

            tvUpNextRoom.setText(selectedUpNext.getRoomNumber());

            TextView tvPill = findViewById(R.id.tvUpNextStatusPill);

            if (dayOffset == 0) {
                if (isLiveNow) {
                    if (tvPill != null) tvPill.setText("LIVE NOW");
                    tvUpNextCountdown.setText("Ends in " + formatMinutes(diffMinutes));
                } else {
                    if (tvPill != null) tvPill.setText("UP NEXT");
                    tvUpNextCountdown.setText("Starts in " + formatMinutes(diffMinutes));
                }
            } else if (dayOffset == 1) {
                if (tvPill != null) tvPill.setText("UP NEXT");
                tvUpNextCountdown.setText("Tomorrow • " + selectedUpNext.getStartTime());
            } else {
                if (tvPill != null) tvPill.setText("UP NEXT");
                String dayNameDisplay = selectedUpNext.day != null ? selectedUpNext.day : "Upcoming";
                tvUpNextCountdown.setText(dayNameDisplay + " • " + selectedUpNext.getStartTime());
            }

            cardUpNext.setVisibility(View.VISIBLE);
        } else {
            cardUpNext.setVisibility(View.GONE);
        }
    }

    private String formatMinutes(int totalMins) {
        if (totalMins <= 0) return "1m";
        int hours = totalMins / 60;
        int mins = totalMins % 60;
        if (hours > 0 && mins > 0) {
            return hours + "h " + mins + "m";
        } else if (hours > 0) {
            return hours + "h";
        } else {
            return mins + "m";
        }
    }

    private void startUpNextCountdownTimer() {
        if (countdownRunnable != null) {
            countdownHandler.removeCallbacks(countdownRunnable);
        }

        countdownRunnable = new Runnable() {
            @Override
            public void run() {
                updateUpNextBanner();
                countdownHandler.postDelayed(this, 30000);
            }
        };
        countdownHandler.post(countdownRunnable);
    }

    private void showScanOptionsBottomSheet() {
        View view = getLayoutInflater().inflate(R.layout.dialog_scan_routine_options, null);
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        bottomSheetDialog.setContentView(view);

        view.findViewById(R.id.btnScanCamera).setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            launchCameraForRoutine();
        });

        view.findViewById(R.id.btnChooseGallery).setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            galleryLauncher.launch("image/*");
        });

        view.findViewById(R.id.btnAddManual).setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            showAddClassDialog();
        });

        bottomSheetDialog.show();
    }

    private void launchCameraForRoutine() {
        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.TITLE, "Routine_Scan_" + System.currentTimeMillis());
            values.put(MediaStore.Images.Media.DESCRIPTION, "Scanned class routine image");
            photoUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            cameraLauncher.launch(intent);
        } catch (Exception e) {
            Log.e("RoutineActivity", "Failed to open camera: " + e.getMessage(), e);
        }
    }

    private void processRoutineImageUri(Uri imageUri) {
        try {
            if (imageUri == null) return;
            InputImage image = InputImage.fromFilePath(this, imageUri);
            TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

            recognizer.process(image)
                    .addOnSuccessListener(visionText -> runOnUiThread(() -> {
                        try {
                            if (isFinishing() || isDestroyed()) return;
                            parseAndSaveRoutineOcrVisionText(visionText);
                        } catch (Exception e) {
                            Log.e("RoutineActivity", "Error processing OCR result", e);
                        }
                    }))
                    .addOnFailureListener(e -> runOnUiThread(() -> {
                        try {
                            if (isFinishing() || isDestroyed()) return;
                            Log.e("RoutineActivity", "OCR scan failed: " + e.getLocalizedMessage(), e);
                        } catch (Exception ex) {
                            Log.e("RoutineActivity", "Error handling OCR failure", ex);
                        }
                    }));
        } catch (Exception e) {
            Log.e("RoutineActivity", "Error loading routine image: " + e.getMessage(), e);
        }
    }

    private String normalizeDayName(String rawText) {
        if (rawText == null || rawText.trim().isEmpty()) return null;
        String upper = rawText.trim().toUpperCase();

        if (upper.contains("SAT")) return "Saturday";
        if (upper.contains("SUN")) return "Sunday";
        if (upper.contains("MON")) return "Monday";
        if (upper.contains("TUE")) return "Tuesday";
        if (upper.contains("WED")) return "Wednesday";
        if (upper.contains("THR") || upper.contains("THU")) return "Thursday";
        if (upper.contains("FRI")) return "Friday";

        return null;
    }

    private void parseAndSaveRoutineOcrVisionText(Text visionText) {
        try {
            if (isFinishing() || isDestroyed()) return;
            if (visionText == null || visionText.getText() == null || visionText.getText().trim().isEmpty()) {
                return;
            }

            String fullText = visionText.getText();

            // 1. Extract Intake & Section if present e.g. "Intake: 55 - 6"
            if (sessionManager != null) {
                try {
                    Matcher intakeMatcher = Pattern.compile("(?i)(Intake|Int|Sec|Section)[: ]*([0-9]{1,3})[ _/,:-]*([0-9]{1,3})").matcher(fullText);
                    if (intakeMatcher.find()) {
                        String intakeStr = intakeMatcher.group(2);
                        String secStr = intakeMatcher.group(3);
                        if (intakeStr != null && secStr != null) {
                            sessionManager.setUserIntake(intakeStr.trim());
                            sessionManager.setUserSection(secStr.trim());
                        }
                    }
                } catch (PatternSyntaxException e) {
                    Log.e("RoutineActivity", "PatternSyntaxException in intake matching 1", e);
                } catch (Exception e) {
                    Log.e("RoutineActivity", "Error in intake matching 1", e);
                }
            }

            // 2. Collect all text lines with bounding boxes
            List<TextLineItem> allLines = new ArrayList<>();
            for (Text.TextBlock block : visionText.getTextBlocks()) {
                for (Text.Line line : block.getLines()) {
                    String lineText = line.getText().trim();
                    Rect box = line.getBoundingBox();
                    if (!lineText.isEmpty() && box != null) {
                        allLines.add(new TextLineItem(lineText, box));
                    }
                }
            }

            if (allLines.isEmpty()) {
                parseAndSaveRoutineOcrText(fullText);
                return;
            }

            // 3. ROW DETECTION: Find Day Header Anchors (Y-coordinates)
            List<DayAnchor> dayAnchors = new ArrayList<>();
            Map<String, DayAnchor> dayMap = new HashMap<>();

            for (TextLineItem lineItem : allLines) {
                String normDay = normalizeDayName(lineItem.text);
                if (normDay != null && !dayMap.containsKey(normDay)) {
                    DayAnchor anchor = new DayAnchor(normDay, lineItem.centerY, lineItem.box);
                    dayAnchors.add(anchor);
                    dayMap.put(normDay, anchor);
                }
            }

            // Sort Day Anchors top to bottom by Y-coordinate
            Collections.sort(dayAnchors, (a, b) -> Integer.compare(a.centerY, b.centerY));

            if (dayAnchors.isEmpty()) {
                parseAndSaveRoutineOcrText(fullText);
                return;
            }

            // 4. COLUMN DETECTION: Find Time Slot Headers (X-coordinates)
            List<TimeAnchor> timeAnchors = new ArrayList<>();
            for (TextLineItem lineItem : allLines) {
                String txt = lineItem.text;
                for (int i = 0; i < BUBT_TIME_SLOTS.length; i++) {
                    String slotHeader = BUBT_TIME_SLOTS[i];
                    String startPart = slotHeader.split("-")[0].trim().substring(0, 5);
                    if (txt.contains(startPart)) {
                        timeAnchors.add(new TimeAnchor(i, lineItem.centerX, lineItem.box));
                        break;
                    }
                }
            }
            Collections.sort(timeAnchors, (a, b) -> Integer.compare(a.centerX, b.centerX));

            // 5. MATRIX INTERSECTION MAPPING (Row x Column 2D Grid)
            int numRows = dayAnchors.size();
            int numCols = BUBT_TIME_SLOTS.length;

            int[] rowYMin = new int[numRows];
            int[] rowYMax = new int[numRows];

            for (int i = 0; i < numRows; i++) {
                DayAnchor cur = dayAnchors.get(i);
                if (i == 0) {
                    int dist = (numRows > 1) ? (dayAnchors.get(1).centerY - cur.centerY) : 160;
                    rowYMin[i] = cur.centerY - (dist / 2);
                } else {
                    rowYMin[i] = (cur.centerY + dayAnchors.get(i - 1).centerY) / 2;
                }

                if (i == numRows - 1) {
                    int dist = (numRows > 1) ? (cur.centerY - dayAnchors.get(i - 1).centerY) : 160;
                    rowYMax[i] = cur.centerY + (dist / 2);
                } else {
                    rowYMax[i] = (cur.centerY + dayAnchors.get(i + 1).centerY) / 2;
                }
            }

            int[] colXMin = new int[numCols];
            int[] colXMax = new int[numCols];

            if (timeAnchors.size() >= 2) {
                for (int j = 0; j < numCols; j++) {
                    int colCenter = -1;
                    for (TimeAnchor ta : timeAnchors) {
                        if (ta.slotIndex == j) {
                            colCenter = ta.centerX;
                            break;
                        }
                    }
                    if (colCenter == -1) {
                        int firstX = timeAnchors.get(0).centerX;
                        int lastX = timeAnchors.get(timeAnchors.size() - 1).centerX;
                        int spanSlots = Math.max(1, timeAnchors.get(timeAnchors.size() - 1).slotIndex - timeAnchors.get(0).slotIndex);
                        int stepX = (lastX - firstX) / spanSlots;
                        colCenter = firstX + (j - timeAnchors.get(0).slotIndex) * stepX;
                    }

                    int halfWidth = 110;
                    colXMin[j] = colCenter - halfWidth;
                    colXMax[j] = colCenter + halfWidth;
                }
            } else {
                int minX = Integer.MAX_VALUE;
                int maxX = Integer.MIN_VALUE;

                for (TextLineItem li : allLines) {
                    if (normalizeDayName(li.text) == null) {
                        if (li.box.left < minX) minX = li.box.left;
                        if (li.box.right > maxX) maxX = li.box.right;
                    }
                }

                if (minX >= maxX) {
                    minX = 100;
                    maxX = 1000;
                }

                double colWidth = (maxX - minX) / (double) numCols;
                for (int j = 0; j < numCols; j++) {
                    colXMin[j] = (int) (minX + j * colWidth);
                    colXMax[j] = (int) (minX + (j + 1) * colWidth);
                }
            }

            List<RoutineClassItem> parsedClasses = new ArrayList<>();

            // Intersect each 2D Cell Matrix[Row i][Col j]
            for (int i = 0; i < numRows; i++) {
                DayAnchor dayAnchor = dayAnchors.get(i);
                int yMin = rowYMin[i];
                int yMax = rowYMax[i];

                for (int j = 0; j < numCols; j++) {
                    int xMin = colXMin[j];
                    int xMax = colXMax[j];

                    List<TextLineItem> cellLines = new ArrayList<>();
                    for (TextLineItem li : allLines) {
                        if (li.centerY >= yMin && li.centerY <= yMax && li.centerX >= xMin && li.centerX <= xMax) {
                            cellLines.add(li);
                        }
                    }

                    if (cellLines.isEmpty()) continue;

                    StringBuilder cellTextBuilder = new StringBuilder();
                    for (TextLineItem li : cellLines) {
                        cellTextBuilder.append(li.text).append(" ");
                    }
                    String cellText = cellTextBuilder.toString().trim();

                    String courseCode = extractCourseCode(cellText);
                    if (courseCode != null && containsCourseCodePattern(cellText)) {
                        String faculty = extractFacultyCode(cellText);
                        if (faculty == null) faculty = "AAG";

                        String rawRoom = extractRoomNumber(cellText);
                        if (rawRoom == null) rawRoom = "2808";

                        String formattedRoom = formatRoomWithBuilding(rawRoom);
                        String timeSlot = BUBT_TIME_SLOTS[j];

                        RoutineClassItem item = new RoutineClassItem();
                        item.day = dayAnchor.dayName;
                        item.courseCode = courseCode;
                        item.subject = courseCode;
                        item.timeSlot = timeSlot;
                        item.room = formattedRoom;
                        item.faculty = faculty;
                        item.teacher = faculty;

                        String[] timeParts = timeSlot.split("-");
                        if (timeParts.length == 2) {
                            item.startTime = timeParts[0].trim();
                            item.endTime = timeParts[1].trim();
                        }

                        parsedClasses.add(item);
                    }
                }
            }

            if (parsedClasses.isEmpty()) {
                parseAndSaveRoutineOcrText(fullText);
                return;
            }

            // Direct Local List Population (Immediate UI Feedback)
            List<RoutineClassItem> activeDayParsed = new ArrayList<>();
            for (RoutineClassItem item : parsedClasses) {
                if (matchesSelectedDay(item.day, selectedDay)) {
                    activeDayParsed.add(item);
                }
            }

            runOnUiThread(() -> {
                try {
                    if (isFinishing() || isDestroyed()) return;
                    routineList.clear();
                    routineList.addAll(activeDayParsed);
                    if (routineAdapter != null) routineAdapter.notifyDataSetChanged();
                    updateUiVisibilityAndSummary();
                } catch (Exception e) {
                    Log.e("RoutineActivity", "Error updating UI in parseAndSaveRoutineOcrVisionText", e);
                }
            });

            // Save batch to Firestore atomically, update snapshot listener, refresh active UI, and show success dialog
            saveRoutineClassesBatch(parsedClasses, () -> runOnUiThread(() -> {
                try {
                    if (isFinishing() || isDestroyed()) return;
                    refreshDashboard();
                    showScanSuccessDialog(parsedClasses);
                } catch (Exception e) {
                    Log.e("RoutineActivity", "Error handling batch save callback", e);
                }
            }));
        } catch (Exception e) {
            Log.e("RoutineActivity", "Error in parseAndSaveRoutineOcrVisionText", e);
        }
    }

    private boolean containsCourseCodePattern(String text) {
        if (text == null) return false;
        try {
            return text.matches(".*(?i)(CSE|MAT|ENG|EEE|PHY|CHEM|BUS|STA|ACT|FIN|LAW|GED|TEX|CIV)[-_ ]*[0-9]{3,4}.*") ||
                    text.matches(".*[A-Z]{2,4}[-_ ]*[0-9]{3,4}.*");
        } catch (PatternSyntaxException e) {
            Log.e("RoutineActivity", "PatternSyntaxException in containsCourseCodePattern", e);
            return false;
        } catch (Exception e) {
            Log.e("RoutineActivity", "Error in containsCourseCodePattern", e);
            return false;
        }
    }

    private String extractCourseCode(String text) {
        if (text == null) return null;
        try {
            Matcher m = Pattern.compile("(?i)((?:CSE|MAT|ENG|EEE|PHY|CHEM|BUS|STA|ACT|FIN|LAW|GED|TEX|CIV|[A-Z]{2,4})[-_ ]*[0-9]{3,4}[A-Z]?)").matcher(text);
            if (m.find()) {
                String matched = m.group(1);
                if (matched != null) {
                    return matched.toUpperCase().replaceAll("[ \t\n\r]+", " ");
                }
            }
        } catch (PatternSyntaxException e) {
            Log.e("RoutineActivity", "PatternSyntaxException in extractCourseCode", e);
        } catch (Exception e) {
            Log.e("RoutineActivity", "Error in extractCourseCode", e);
        }
        return text.trim();
    }

    private String extractFacultyCode(String text) {
        if (text == null) return null;
        try {
            Matcher m1 = Pattern.compile("(?i)(FC:|Faculty:|Instructor:|Teacher:)[^a-zA-Z]*([a-zA-Z]{2,5})").matcher(text);
            if (m1.find()) {
                String f = m1.group(2);
                if (f != null) return f.toUpperCase();
            }
        } catch (PatternSyntaxException e) {
            Log.e("RoutineActivity", "PatternSyntaxException in extractFacultyCode m1", e);
        } catch (Exception e) {
            Log.e("RoutineActivity", "Error in extractFacultyCode m1", e);
        }
        try {
            Matcher m2 = Pattern.compile("([A-Z]{2,5})").matcher(text);
            while (m2.find()) {
                String candidate = m2.group(1);
                if (!isBlacklistedKeyword(candidate)) {
                    return candidate;
                }
            }
        } catch (PatternSyntaxException e) {
            Log.e("RoutineActivity", "PatternSyntaxException in extractFacultyCode m2", e);
        } catch (Exception e) {
            Log.e("RoutineActivity", "Error in extractFacultyCode m2", e);
        }
        return null;
    }

    private String extractRoomNumber(String text) {
        if (text == null) return null;
        try {
            Matcher m1 = Pattern.compile("(?i)(R:|Room|R)?[^0-9]*([0-9]{3,4}|Lab[^0-9]*[0-9]+)").matcher(text);
            if (m1.find()) {
                String rm = m1.group(2);
                if (rm != null && !rm.isEmpty() && !rm.equalsIgnoreCase("ROOM")) {
                    return rm;
                }
            }
        } catch (PatternSyntaxException e) {
            Log.e("RoutineActivity", "PatternSyntaxException in extractRoomNumber m1", e);
        } catch (Exception e) {
            Log.e("RoutineActivity", "Error in extractRoomNumber m1", e);
        }
        try {
            Matcher m2 = Pattern.compile("([0-9]{3,4})").matcher(text);
            if (m2.find()) {
                return m2.group(1);
            }
        } catch (PatternSyntaxException e) {
            Log.e("RoutineActivity", "PatternSyntaxException in extractRoomNumber m2", e);
        } catch (Exception e) {
            Log.e("RoutineActivity", "Error in extractRoomNumber m2", e);
        }
        return null;
    }

    private String formatRoomWithBuilding(String room) {
        if (room == null || room.trim().isEmpty()) return "Room 808 (Bldg 2)";
        String r = room.replace("Room", "").replace("ROOM", "").replace("R:", "").trim();
        try {
            if (r.matches("[0-9]{4}")) {
                char bldg = r.charAt(0);
                String roomNum = r.substring(1);
                return "Room " + roomNum + " (Bldg " + bldg + ")";
            }
        } catch (PatternSyntaxException e) {
            Log.e("RoutineActivity", "PatternSyntaxException in formatRoomWithBuilding", e);
        } catch (Exception e) {
            Log.e("RoutineActivity", "Error in formatRoomWithBuilding", e);
        }
        return r;
    }

    private String formatRoomBadge(String room) {
        if (room == null || room.trim().isEmpty()) return "517";
        String r = room.replace("Room", "").replace("ROOM", "").replace("R:", "").trim();
        try {
            if (r.matches("[0-9]{4}")) {
                return r.substring(1); // Last 3 digits e.g. "517"
            }
        } catch (Exception ignored) {}
        return r.isEmpty() ? "517" : r;
    }

    private boolean isBlacklistedKeyword(String word) {
        if (word == null) return true;
        String w = word.toUpperCase();
        return w.equals("CSE") || w.equals("MAT") || w.equals("ENG") || w.equals("EEE") ||
               w.equals("PHY") || w.equals("CHEM") || w.equals("BUS") || w.equals("STA") ||
               w.equals("ACT") || w.equals("FIN") || w.equals("LAW") || w.equals("GED") ||
               w.equals("TEX") || w.equals("CIV") || w.equals("SAT") || w.equals("SUN") ||
               w.equals("MON") || w.equals("TUE") || w.equals("WED") || w.equals("THU") ||
               w.equals("THR") || w.equals("FRI") || w.equals("ROOM") || w.equals("LAB") ||
               w.equals("BUBT") || w.equals("INTAKE") || w.equals("SEC") || w.equals("CLASS");
    }

    private static class DayAnchor {
        String dayName;
        int centerY;
        Rect box;

        DayAnchor(String dayName, int centerY, Rect box) {
            this.dayName = dayName;
            this.centerY = centerY;
            this.box = box;
        }
    }

    private static class TimeAnchor {
        int slotIndex;
        int centerX;
        Rect box;

        TimeAnchor(int slotIndex, int centerX, Rect box) {
            this.slotIndex = slotIndex;
            this.centerX = centerX;
            this.box = box;
        }
    }

    private static class TextLineItem {
        String text;
        Rect box;
        int centerX;
        int centerY;

        TextLineItem(String text, Rect box) {
            this.text = text;
            this.box = box;
            this.centerX = box != null ? box.centerX() : 0;
            this.centerY = box != null ? box.centerY() : 0;
        }
    }

    private void parseAndSaveRoutineOcrText(String ocrText) {
        try {
            if (isFinishing() || isDestroyed()) return;
            if (ocrText == null || ocrText.trim().isEmpty()) {
                return;
            }

            // Try extracting Intake & Section if present e.g. "Intake: 48 - 2"
            if (ocrText.toLowerCase().contains("intake") && sessionManager != null) {
                try {
                    Matcher m = Pattern.compile("(?i)Intake[^0-9]*([0-9]+)[^0-9]*([0-9]+)").matcher(ocrText);
                    if (m.find()) {
                        String intakeStr = m.group(1);
                        String secStr = m.group(2);
                        if (intakeStr != null && secStr != null) {
                            sessionManager.setUserIntake(intakeStr);
                            sessionManager.setUserSection(secStr);
                        }
                    }
                } catch (PatternSyntaxException e) {
                    Log.e("RoutineActivity", "PatternSyntaxException in OCR text intake matching", e);
                } catch (Exception ignored) {
                }
            }

            String[] lines = ocrText.split("\n");
            String currentDay = "Saturday";
            int slotIndex = 0;

            List<RoutineClassItem> parsedClasses = new ArrayList<>();

            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) continue;

                String normalizedDay = normalizeDayName(trimmed);
                if (normalizedDay != null) {
                    currentDay = normalizedDay;
                    slotIndex = 0;
                    continue;
                }

                boolean matchesCode = false;
                try {
                    matchesCode = trimmed.matches(".*(?i)(CSE|MAT|ENG|EEE|PHY|CHEM|BUS|STA|ACT|FIN|LAW|GED|TEX|CIV)[-_ ]*[0-9]{3,4}.*") ||
                            trimmed.matches(".*[A-Z]{2,4}[-_ ]*[0-9]{3,4}.*");
                } catch (PatternSyntaxException e) {
                    Log.e("RoutineActivity", "PatternSyntaxException in OCR line matching", e);
                } catch (Exception e) {
                    Log.e("RoutineActivity", "Error in OCR line matching", e);
                }

                if (matchesCode) {

                    String courseCode = extractCourseCode(trimmed);
                    String timeSlot = BUBT_TIME_SLOTS[slotIndex % BUBT_TIME_SLOTS.length];
                    String room = extractRoomNumber(trimmed);
                    if (room == null) room = "2808";
                    String faculty = extractFacultyCode(trimmed);
                    if (faculty == null) faculty = "AAG";

                    RoutineClassItem item = new RoutineClassItem();
                    item.day = currentDay;
                    item.courseCode = courseCode;
                    item.subject = courseCode;
                    item.timeSlot = timeSlot;
                    item.room = room;
                    item.faculty = faculty;
                    item.teacher = faculty;

                    String[] timeParts = timeSlot.split("-");
                    if (timeParts.length == 2) {
                        item.startTime = timeParts[0].trim();
                        item.endTime = timeParts[1].trim();
                    }

                    parsedClasses.add(item);
                    slotIndex++;
                }
            }

            if (parsedClasses.isEmpty()) {
                RoutineClassItem noticeItem = new RoutineClassItem();
                noticeItem.day = currentDay;
                String noticeText = "Scanned Notice: " + (ocrText.length() > 30 ? ocrText.substring(0, 30) + "..." : ocrText);
                noticeItem.courseCode = noticeText;
                noticeItem.subject = noticeText;
                noticeItem.timeSlot = "08:15 AM - 09:45 AM";
                noticeItem.startTime = "08:15 AM";
                noticeItem.endTime = "09:45 AM";
                noticeItem.room = "2808";
                noticeItem.faculty = "AAG";
                noticeItem.teacher = "AAG";
                parsedClasses.add(noticeItem);
            }

            // Direct Local List Population (Immediate UI Feedback)
            List<RoutineClassItem> activeDayParsed = new ArrayList<>();
            for (RoutineClassItem item : parsedClasses) {
                if (matchesSelectedDay(item.day, selectedDay)) {
                    activeDayParsed.add(item);
                }
            }

            runOnUiThread(() -> {
                try {
                    if (isFinishing() || isDestroyed()) return;
                    routineList.clear();
                    routineList.addAll(activeDayParsed);
                    if (routineAdapter != null) routineAdapter.notifyDataSetChanged();
                    updateUiVisibilityAndSummary();
                } catch (Exception e) {
                    Log.e("RoutineActivity", "Error updating UI in parseAndSaveRoutineOcrText", e);
                }
            });

            // Save batch to Firestore atomically, update snapshot listener, refresh active UI, and show success dialog
            saveRoutineClassesBatch(parsedClasses, () -> runOnUiThread(() -> {
                try {
                    if (isFinishing() || isDestroyed()) return;
                    refreshDashboard();
                    showScanSuccessDialog(parsedClasses);
                } catch (Exception e) {
                    Log.e("RoutineActivity", "Error in batch save callback", e);
                }
            }));
        } catch (Exception e) {
            Log.e("RoutineActivity", "Error in parseAndSaveRoutineOcrText", e);
        }
    }

    private void showScanSuccessDialog(List<RoutineClassItem> scannedClasses) {
        try {
            if (isFinishing() || isDestroyed()) return;

            View dialogView = getLayoutInflater().inflate(R.layout.dialog_scan_success, null, false);

            TextView tvTitle = dialogView.findViewById(R.id.tvSuccessTitle);
            TextView tvSubtitle = dialogView.findViewById(R.id.tvSuccessSubtitle);
            LinearLayout containerClasses = dialogView.findViewById(R.id.containerSuccessClasses);
            MaterialButton btnViewSchedule = dialogView.findViewById(R.id.btnViewSchedule);
            MaterialButton btnScanAnother = dialogView.findViewById(R.id.btnScanAnother);

            String dept = sessionManager != null ? sessionManager.getUserDept() : null;
            String intake = sessionManager != null ? sessionManager.getUserIntake() : null;
            String section = sessionManager != null ? sessionManager.getUserSection() : null;

            int count = scannedClasses != null ? scannedClasses.size() : 0;

            if (tvTitle != null) {
                tvTitle.setText(String.format(Locale.getDefault(), "Routine Saved! (%d Classes)", count));
            }

            if (tvSubtitle != null) {
                tvSubtitle.setText(String.format(Locale.getDefault(), "Saved for %s • Intake %s • Section %s",
                        dept != null ? dept : "BUBT",
                        intake != null ? intake : "--",
                        section != null ? section : "--"));
            }

            if (containerClasses != null && scannedClasses != null) {
                containerClasses.removeAllViews();
                int maxShow = Math.min(count, 8);
                for (int i = 0; i < maxShow; i++) {
                    RoutineClassItem item = scannedClasses.get(i);
                    if (item == null) continue;
                    TextView tvItem = new TextView(this);
                    tvItem.setText(String.format("• %s (%s) - %s | %s | FC: %s",
                            item.getDisplayCourseCode(),
                            item.day != null ? item.day.substring(0, Math.min(3, item.day.length())).toUpperCase() : "DAY",
                            item.timeSlot != null ? item.timeSlot : "--",
                            item.room != null ? item.room : "Room --",
                            item.getDisplayFaculty()));
                    tvItem.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
                    tvItem.setTextSize(13);
                    tvItem.setPadding(0, 6, 0, 6);
                    containerClasses.addView(tvItem);
                }
                if (count > 8) {
                    TextView tvMore = new TextView(this);
                    tvMore.setText(String.format(Locale.getDefault(), "+ %d more classes saved", count - 8));
                    tvMore.setTextColor(ContextCompat.getColor(this, R.color.primary));
                    tvMore.setTextSize(12);
                    tvMore.setTypeface(null, Typeface.BOLD);
                    tvMore.setPadding(0, 8, 0, 4);
                    containerClasses.addView(tvMore);
                }
            }

            BottomSheetDialog dialog = new BottomSheetDialog(this);
            dialog.setContentView(dialogView);
            dialog.setCancelable(true);
            dialog.setCanceledOnTouchOutside(true);

            if (btnViewSchedule != null) {
                btnViewSchedule.setOnClickListener(v -> {
                    try {
                        dialog.dismiss();
                        loadRoutineForDay(selectedDay);
                    } catch (Exception e) {
                        Log.e("RoutineActivity", "Error dismissing success dialog", e);
                    }
                });
            }

            if (btnScanAnother != null) {
                btnScanAnother.setOnClickListener(v -> {
                    try {
                        dialog.dismiss();
                        showScanOptionsBottomSheet();
                    } catch (Exception e) {
                        Log.e("RoutineActivity", "Error showing scan options", e);
                    }
                });
            }

            dialog.show();
        } catch (Exception e) {
            Log.e("RoutineActivity", "Error showing scan success dialog", e);
        }
    }

    private void saveRoutineClassesBatch(List<RoutineClassItem> items, Runnable onComplete) {
        try {
            if (items == null || items.isEmpty()) {
                if (onComplete != null) {
                    runOnUiThread(() -> {
                        try {
                            if (!isFinishing() && !isDestroyed()) {
                                onComplete.run();
                            }
                        } catch (Exception e) {
                            Log.e("RoutineActivity", "Error running onComplete", e);
                        }
                    });
                }
                return;
            }

            // Sanitize & normalize items strictly (skip unparsed/invalid day items)
            List<RoutineClassItem> cleanItems = new ArrayList<>();
            for (RoutineClassItem item : items) {
                if (item == null) continue;
                String normDay = normalizeDayName(item.day);
                if (normDay == null) continue; // Do NOT force to selectedDay!
                item.day = normDay;
                cleanItems.add(item);
            }

            if (cleanItems.isEmpty()) {
                if (onComplete != null) {
                    runOnUiThread(() -> {
                        try {
                            if (!isFinishing() && !isDestroyed()) {
                                onComplete.run();
                            }
                        } catch (Exception e) {
                            Log.e("RoutineActivity", "Error running onComplete", e);
                        }
                    });
                }
                return;
            }

            // Save clean list locally immediately for instant persistence across app restarts
            RoutineLocalCache.saveRoutineLocally(RoutineActivity.this, cleanItems);

            String dept = sessionManager != null ? getSanitizedKey(sessionManager.getUserDept()) : "default";
            String intake = sessionManager != null ? getSanitizedKey(sessionManager.getUserIntake()) : "default";
            String section = sessionManager != null ? getSanitizedKey(sessionManager.getUserSection()) : "default";

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            WriteBatch batch = db.batch();

            for (RoutineClassItem item : cleanItems) {
                String startTime = item.startTime != null ? item.startTime : "08:15 AM";
                String endTime = item.endTime != null ? item.endTime : "09:45 AM";
                if ((item.startTime == null || item.endTime == null) && item.timeSlot != null && item.timeSlot.contains("-")) {
                    String[] parts = item.timeSlot.split("-");
                    startTime = parts[0].trim();
                    endTime = parts[1].trim();
                }

                Map<String, Object> map = new HashMap<>();
                map.put("day", item.day);
                map.put("subject", item.getDisplayCourseCode());
                map.put("courseCode", item.getDisplayCourseCode());
                map.put("timeSlot", item.timeSlot != null ? item.timeSlot : startTime + " - " + endTime);
                map.put("startTime", startTime);
                map.put("endTime", endTime);
                map.put("room", item.room != null ? item.room : "2808");
                map.put("teacher", item.getDisplayFaculty());
                map.put("faculty", item.getDisplayFaculty());
                map.put("timestamp", System.currentTimeMillis());

                DocumentReference docRef = db.collection("routines")
                        .document(dept)
                        .collection(intake)
                        .document(section)
                        .collection("schedule")
                        .document();

                batch.set(docRef, map);
            }

            batch.commit()
                    .addOnSuccessListener(aVoid -> runOnUiThread(() -> {
                        try {
                            if (isFinishing() || isDestroyed()) return;
                            if (onComplete != null) onComplete.run();
                        } catch (Exception e) {
                            Log.e("RoutineActivity", "Error in saveRoutineClassesBatch onSuccess", e);
                        }
                    }))
                    .addOnFailureListener(e -> runOnUiThread(() -> {
                        try {
                            if (isFinishing() || isDestroyed()) return;
                            if (onComplete != null) onComplete.run();
                        } catch (Exception ex) {
                            Log.e("RoutineActivity", "Error in saveRoutineClassesBatch onFailure", ex);
                        }
                    }));
        } catch (Exception e) {
            Log.e("RoutineActivity", "Error in saveRoutineClassesBatch", e);
            if (onComplete != null) {
                runOnUiThread(() -> {
                    try {
                        if (!isFinishing() && !isDestroyed()) {
                            onComplete.run();
                        }
                    } catch (Exception ex) {
                        Log.e("RoutineActivity", "Error running onComplete fallback", ex);
                    }
                });
            }
        }
    }

    private Task<DocumentReference> saveRoutineClassToFirestore(String day, String courseCode, String timeSlot, String room, String faculty) {
        String dept = getSanitizedKey(sessionManager.getUserDept());
        String intake = getSanitizedKey(sessionManager.getUserIntake());
        String section = getSanitizedKey(sessionManager.getUserSection());

        String validDay = normalizeDayName(day);
        if (validDay == null) validDay = selectedDay;

        String startTime = "08:15 AM";
        String endTime = "09:45 AM";
        if (timeSlot != null && timeSlot.contains("-")) {
            String[] parts = timeSlot.split("-");
            startTime = parts[0].trim();
            endTime = parts[1].trim();
        }

        Map<String, Object> map = new HashMap<>();
        map.put("day", validDay);
        map.put("subject", courseCode);
        map.put("courseCode", courseCode);
        map.put("timeSlot", timeSlot);
        map.put("startTime", startTime);
        map.put("endTime", endTime);
        map.put("room", room);
        map.put("teacher", faculty);
        map.put("faculty", faculty);
        map.put("timestamp", System.currentTimeMillis());

        return FirebaseFirestore.getInstance().collection("routines")
                .document(dept)
                .collection(intake)
                .document(section)
                .collection("schedule")
                .add(map);
    }

    private void showAddClassDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_add_routine_class, null);
        Spinner spinnerDay = view.findViewById(R.id.spinnerRoutineDay);
        TextInputEditText etSubject = view.findViewById(R.id.etRoutineSubject);
        TextInputEditText etTime = view.findViewById(R.id.etRoutineTime);
        TextInputEditText etRoom = view.findViewById(R.id.etRoutineRoom);
        TextInputEditText etTeacher = view.findViewById(R.id.etRoutineTeacher);
        MaterialButton btnSave = view.findViewById(R.id.btnSaveRoutineClass);

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, DAYS);
        spinnerDay.setAdapter(spinnerAdapter);

        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(view);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                (int) (getResources().getDisplayMetrics().widthPixels * 0.88),
                WindowManager.LayoutParams.WRAP_CONTENT
            );
            dialog.getWindow().setDimAmount(0.4f);
        }

        btnSave.setOnClickListener(v -> {
            String day = spinnerDay.getSelectedItem().toString();
            String subject = etSubject.getText() != null ? etSubject.getText().toString().trim() : "";
            String timeSlot = etTime.getText() != null ? etTime.getText().toString().trim() : "";
            String room = etRoom.getText() != null ? etRoom.getText().toString().trim() : "";
            String teacher = etTeacher.getText() != null ? etTeacher.getText().toString().trim() : "";

            if (subject.isEmpty()) {
                etSubject.setError("Required");
                etSubject.requestFocus();
                return;
            }
            if (timeSlot.isEmpty()) {
                etTime.setError("Required");
                etTime.requestFocus();
                return;
            }

            btnSave.setEnabled(false);

            String validDay = normalizeDayName(day);
            if (validDay == null) validDay = day;

            saveRoutineClassToFirestore(validDay, subject, timeSlot, room, teacher)
                    .addOnSuccessListener(docRef -> {
                        CustomToast.show(this, "Class added");
                        dialog.dismiss();
                        loadRoutineForDay(selectedDay);
                    })
                    .addOnFailureListener(e -> {
                        btnSave.setEnabled(true);
                    });
        });

        dialog.show();
    }



    public static class RoutineAdapter extends RecyclerView.Adapter<RoutineAdapter.ViewHolder> {
        private final List<RoutineClassItem> list;
        private final boolean canEdit;
        private final RoutineActivity activity;

        public RoutineAdapter(List<RoutineClassItem> list, boolean canEdit, RoutineActivity activity) {
            this.list = list;
            this.canEdit = canEdit;
            this.activity = activity;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_routine_class, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            RoutineClassItem item = list.get(position);

            String startTime = item.startTime;
            if (startTime == null || startTime.trim().isEmpty()) {
                if (item.timeSlot != null && item.timeSlot.contains("-")) {
                    startTime = item.timeSlot.split("-")[0].trim();
                } else if (item.timeSlot != null && !item.timeSlot.trim().isEmpty()) {
                    startTime = item.timeSlot.trim();
                } else {
                    startTime = "--";
                }
            }

            String displayCode = item.getDisplayCourseCode();
            String displayFac = item.getDisplayFaculty();
            String timeSlot = item.timeSlot != null ? item.timeSlot : (item.startTime != null && item.endTime != null ? item.startTime + " - " + item.endTime : "--");

            holder.tvTimelineTime.setText(startTime);
            holder.tvSubject.setText(displayCode);
            holder.tvTime.setText(timeSlot);

            String roomNum = item != null ? item.getRoomNumber() : "2517";
            holder.tvRoom.setText("ROOM " + roomNum);

            String rawTeacher = displayFac.replace("Instructor:", "").replace("Faculty:", "").replace("FC:", "").trim();
            if (rawTeacher.isEmpty()) rawTeacher = "Faculty";
            holder.tvTeacher.setText("FC: " + rawTeacher);

            holder.tvDuration.setText(calculateDuration(timeSlot));

            if (canEdit) {
                holder.btnDelete.setVisibility(View.VISIBLE);
                holder.btnDelete.setOnClickListener(v -> {
                    HapticUtils.heavyClick(v);
                    String dept = activity.getSanitizedKey(activity.sessionManager.getUserDept());
                    String intake = activity.getSanitizedKey(activity.sessionManager.getUserIntake());
                    String section = activity.getSanitizedKey(activity.sessionManager.getUserSection());

                    if (item.id != null && !item.id.isEmpty()) {
                        FirebaseFirestore.getInstance().collection("routines")
                                .document(dept)
                                .collection(intake)
                                .document(section)
                                .collection("schedule")
                                .document(item.id)
                                .delete()
                                .addOnSuccessListener(aVoid -> {
                                    activity.refreshDashboard();
                                });
                    }
                });
            } else {
                holder.btnDelete.setVisibility(View.GONE);
            }
        }

        private String calculateDuration(String timeSlot) {
            if (timeSlot == null || !timeSlot.contains("-")) return "1h 30m";
            try {
                String[] parts = timeSlot.split("-");
                SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.US);
                Date d1 = sdf.parse(parts[0].trim());
                Date d2 = sdf.parse(parts[1].trim());
                if (d1 != null && d2 != null) {
                    long diffMs = d2.getTime() - d1.getTime();
                    if (diffMs < 0) diffMs += 24 * 60 * 60 * 1000;
                    long mins = diffMs / (60 * 1000);
                    long hours = mins / 60;
                    long remMins = mins % 60;
                    if (hours > 0 && remMins > 0) return hours + "h " + remMins + "m";
                    if (hours > 0) return hours + "h";
                    return remMins + "m";
                }
            } catch (Exception ignored) {
            }
            return "1h 30m";
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTimelineTime, tvSubject, tvTime, tvRoom, tvTeacher, tvDuration;
            ImageView btnDelete;

            ViewHolder(View itemView) {
                super(itemView);
                tvTimelineTime = itemView.findViewById(R.id.tvTimelineTime);
                tvSubject = itemView.findViewById(R.id.tvRoutineSubject);
                tvTime = itemView.findViewById(R.id.tvRoutineTime);
                tvRoom = itemView.findViewById(R.id.tvRoutineRoom);
                tvTeacher = itemView.findViewById(R.id.tvRoutineTeacher);
                tvDuration = itemView.findViewById(R.id.tvRoutineDuration);
                btnDelete = itemView.findViewById(R.id.btnDeleteRoutine);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (routineListenerRegistration != null) {
            routineListenerRegistration.remove();
        }
        if (countdownHandler != null && countdownRunnable != null) {
            countdownHandler.removeCallbacks(countdownRunnable);
        }
    }
}
