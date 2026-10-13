package com.example.crconnect;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RoutineActivity extends AppCompatActivity {

    private SessionManager sessionManager;
    private TextView tvGreetingTitle, tvHeaderDate;
    private View cardUpNext;
    private TextView tvUpNextSubject, tvUpNextTime, tvUpNextRoom, tvUpNextCountdown;
    private TextView tvDailyRoutineTitle, tvNoRoutineClasses;
    private RecyclerView recyclerViewRoutine;
    private FloatingActionButton fabAddRoutineClass;
    private LinearLayout containerDayPicker;

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
        setContentView(R.layout.activity_routine);

        sessionManager = new SessionManager(this);

        tvGreetingTitle = findViewById(R.id.tvGreetingTitle);
        tvHeaderDate = findViewById(R.id.tvHeaderDate);
        cardUpNext = findViewById(R.id.cardUpNext);
        tvUpNextSubject = findViewById(R.id.tvUpNextSubject);
        tvUpNextTime = findViewById(R.id.tvUpNextTime);
        tvUpNextRoom = findViewById(R.id.tvUpNextRoom);
        tvUpNextCountdown = findViewById(R.id.tvUpNextCountdown);
        tvDailyRoutineTitle = findViewById(R.id.tvDailyRoutineTitle);
        tvNoRoutineClasses = findViewById(R.id.tvNoRoutineClasses);
        recyclerViewRoutine = findViewById(R.id.recyclerViewRoutine);
        fabAddRoutineClass = findViewById(R.id.fabAddRoutineClass);
        containerDayPicker = findViewById(R.id.containerDayPicker);

        View btnRoutineBack = findViewById(R.id.btnRoutineBack);
        View btnScanRoutine = findViewById(R.id.btnScanRoutine);
        TextView btnPrevWeek = findViewById(R.id.btnPrevWeek);
        TextView btnNextWeek = findViewById(R.id.btnNextWeek);

        View headerView = findViewById(R.id.layoutRoutineHeader);
        if (headerView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(headerView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(v.getPaddingLeft(), systemBars.top + v.getPaddingTop(), v.getPaddingRight(), v.getPaddingBottom());
                return insets;
            });
        }

        if (btnRoutineBack != null) {
            btnRoutineBack.setOnClickListener(v -> finish());
        }

        if (btnScanRoutine != null) {
            btnScanRoutine.setOnClickListener(v -> showScanOptionsBottomSheet());
        }

        if (btnPrevWeek != null) {
            btnPrevWeek.setOnClickListener(v -> {
                weekOffset--;
                setupDayPicker();
                loadRoutineForDay(selectedDay);
            });
        }

        if (btnNextWeek != null) {
            btnNextWeek.setOnClickListener(v -> {
                weekOffset++;
                setupDayPicker();
                loadRoutineForDay(selectedDay);
            });
        }

        String role = sessionManager.getUserRole();
        boolean canEdit = role != null && (role.equalsIgnoreCase("CR") || role.equalsIgnoreCase("Teacher/Admin") || role.equalsIgnoreCase("Teacher"));
        if (canEdit) {
            fabAddRoutineClass.setVisibility(View.VISIBLE);
            fabAddRoutineClass.setOnClickListener(v -> showScanOptionsBottomSheet());
        } else {
            fabAddRoutineClass.setVisibility(View.GONE);
        }

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
        String dept = getSanitizedKey(sessionManager.getUserDept());
        String intake = getSanitizedKey(sessionManager.getUserIntake());
        String section = getSanitizedKey(sessionManager.getUserSection());

        FirebaseFirestore.getInstance().collection("routines")
                .document(dept)
                .collection(intake)
                .document(section)
                .collection("schedule")
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot == null || querySnapshot.isEmpty()) {
                        if (cardUpNext != null) cardUpNext.setVisibility(View.GONE);
                        tvNoRoutineClasses.setVisibility(View.VISIBLE);
                        recyclerViewRoutine.setVisibility(View.GONE);
                        showScanOptionsBottomSheet();
                    } else {
                        loadRoutineForDay(selectedDay);
                    }
                })
                .addOnFailureListener(e -> {
                    if (cardUpNext != null) cardUpNext.setVisibility(View.GONE);
                    tvNoRoutineClasses.setVisibility(View.VISIBLE);
                    recyclerViewRoutine.setVisibility(View.GONE);
                    showScanOptionsBottomSheet();
                });
    }

    private void updateHeaderGreetingAndDate() {
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);

        String quote;
        if (hour >= 0 && hour < 5) {
            quote = "Burning the midnight oil";
        } else if (hour >= 5 && hour < 12) {
            quote = "Rise and shine";
        } else if (hour >= 12 && hour < 17) {
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

    private void setupDayPicker() {
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
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(24, 16, 24, 16);
        layout.setTag(dayName);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMarginEnd(12);
        layout.setLayoutParams(params);

        TextView tvDay = new TextView(this);
        tvDay.setText(dayName.substring(0, 3).toUpperCase());
        tvDay.setTextSize(11);
        tvDay.setTypeface(null, Typeface.BOLD);
        tvDay.setGravity(Gravity.CENTER);

        TextView tvNum = new TextView(this);
        tvNum.setText(dayNum);
        tvNum.setTextSize(14);
        tvNum.setTypeface(null, Typeface.BOLD);
        tvNum.setGravity(Gravity.CENTER);

        layout.addView(tvDay);
        layout.addView(tvNum);

        layout.setOnClickListener(v -> {
            selectedDay = dayName;
            highlightSelectedDay(dayName);
            loadRoutineForDay(dayName);
        });

        return layout;
    }

    private void highlightSelectedDay(String activeDay) {
        for (int idx = 0; idx < containerDayPicker.getChildCount(); idx++) {
            View v = containerDayPicker.getChildAt(idx);
            if (v instanceof LinearLayout) {
                LinearLayout pill = (LinearLayout) v;
                Object tag = pill.getTag();
                boolean matches = tag != null && matchesSelectedDay(tag.toString(), activeDay);

                TextView tvDay = (TextView) pill.getChildAt(0);
                TextView tvNum = (TextView) pill.getChildAt(1);

                if (matches) {
                    pill.setBackground(ContextCompat.getDrawable(this, R.drawable.glass_badge_bg));
                    pill.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.primary));
                    tvDay.setTextColor(ContextCompat.getColor(this, R.color.white));
                    tvNum.setTextColor(ContextCompat.getColor(this, R.color.white));
                } else {
                    pill.setBackground(ContextCompat.getDrawable(this, R.drawable.glass_card_bg));
                    pill.setBackgroundTintList(null);
                    tvDay.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
                    tvNum.setTextColor(ContextCompat.getColor(this, R.color.white));
                }
            }
        }
        tvDailyRoutineTitle.setText(String.format("%s Classes", activeDay));
    }

    private boolean matchesSelectedDay(String classDay, String targetDay) {
        if (classDay == null || targetDay == null) return false;
        String c = classDay.trim().toLowerCase();
        String t = targetDay.trim().toLowerCase();

        // Exact match
        if (c.equals(t)) return true;

        // Prefix match (first 3 letters, e.g. "tue" matches "tuesday")
        if (c.length() >= 3 && t.length() >= 3) {
            if (c.substring(0, 3).equals(t.substring(0, 3))) {
                return true;
            }
        }

        // Thursday variations (thu, thr, thurs, thursday)
        if ((c.startsWith("th") || c.startsWith("thr")) && (t.startsWith("th") || t.startsWith("thr"))) {
            return true;
        }

        return c.contains(t) || t.contains(c);
    }

    private String getSanitizedKey(String val) {
        if (val == null || val.isEmpty()) return "default";
        return val.replace(".", "_").replace("/", "_").replace("#", "_").replace("$", "_").replace("[", "_").replace("]", "_").trim();
    }

    private void loadRoutineForDay(String day) {
        selectedDay = day;
        String dept = getSanitizedKey(sessionManager.getUserDept());
        String intake = getSanitizedKey(sessionManager.getUserIntake());
        String section = getSanitizedKey(sessionManager.getUserSection());

        // 1. Safely remove existing listener before attaching a new one
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
                    // Ignore errors or null snapshots
                    if (error != null || querySnapshot == null) {
                        return;
                    }

                    // 2. Parse and filter classes with robust day matching and doc.getString("day") fallback
                    List<RoutineClassItem> dayClasses = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        RoutineClassItem item = doc.toObject(RoutineClassItem.class);
                        if (item != null) {
                            item.id = doc.getId();

                            // Null-safe day extraction with fallback to doc.getString("day")
                            String itemDay = doc.getString("day");
                            if (itemDay == null || itemDay.trim().isEmpty()) {
                                itemDay = item.getDay();
                            }
                            if (itemDay == null || itemDay.trim().isEmpty()) {
                                itemDay = item.day;
                            }
                            if (itemDay != null && !itemDay.trim().isEmpty()) {
                                item.day = itemDay.trim();
                            }

                            // Flexible day matching (full names & 3-letter abbreviations like TUE / Tuesday)
                            if (item.day != null && matchesSelectedDay(item.day, day)) {
                                dayClasses.add(item);
                            }
                        }
                    }

                    // 3. Protect against transient empty snapshots clearing locally loaded OCR items
                    if (dayClasses.isEmpty() && !routineList.isEmpty()) {
                        if (querySnapshot.getMetadata().isFromCache() || querySnapshot.getMetadata().hasPendingWrites()) {
                            // Transient cache read or in-flight write; keep existing UI items intact
                            return;
                        }
                    }

                    // Update adapter list
                    routineList.clear();
                    routineList.addAll(dayClasses);
                    if (routineAdapter != null) {
                        routineAdapter.notifyDataSetChanged();
                    }

                    // 4. Correct UI visibility toggling
                    if (!routineList.isEmpty()) {
                        if (cardUpNext != null) cardUpNext.setVisibility(View.VISIBLE);
                        if (tvNoRoutineClasses != null) tvNoRoutineClasses.setVisibility(View.GONE);
                        if (recyclerViewRoutine != null) recyclerViewRoutine.setVisibility(View.VISIBLE);
                    } else {
                        if (cardUpNext != null) cardUpNext.setVisibility(View.GONE);
                        if (tvNoRoutineClasses != null) tvNoRoutineClasses.setVisibility(View.VISIBLE);
                        if (recyclerViewRoutine != null) recyclerViewRoutine.setVisibility(View.GONE);
                    }
                });
    }

    private void updateUpNextBanner() {
        String today = getTodayDayName();
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
                    List<RoutineClassItem> todayList = new ArrayList<>();
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
                                if (itemDay != null && matchesSelectedDay(itemDay, today)) {
                                    todayList.add(item);
                                }
                            }
                        }
                    }
                    if (!todayList.isEmpty()) {
                        RoutineClassItem next = todayList.get(0);
                        tvUpNextSubject.setText(next.getDisplayCourseCode());
                        tvUpNextTime.setText(next.timeSlot != null ? next.timeSlot : "");

                        String roomVal = next.room != null ? next.room.replace("Room", "").replace("ROOM", "").replace("R:", "").trim() : "";
                        tvUpNextRoom.setText(roomVal.isEmpty() ? "ROOM" : "ROOM " + roomVal);
                        if (cardUpNext != null) cardUpNext.setVisibility(View.VISIBLE);
                    } else if (querySnapshot != null && !querySnapshot.isEmpty()) {
                        RoutineClassItem anyClass = querySnapshot.getDocuments().get(0).toObject(RoutineClassItem.class);
                        if (anyClass != null) {
                            tvUpNextSubject.setText(anyClass.getDisplayCourseCode());
                            tvUpNextTime.setText(anyClass.timeSlot != null ? anyClass.timeSlot : "");
                            String roomVal = anyClass.room != null ? anyClass.room.replace("Room", "").replace("ROOM", "").replace("R:", "").trim() : "";
                            tvUpNextRoom.setText(roomVal.isEmpty() ? "ROOM" : "ROOM " + roomVal);
                            if (cardUpNext != null) cardUpNext.setVisibility(View.VISIBLE);
                        } else {
                            if (cardUpNext != null) cardUpNext.setVisibility(View.GONE);
                        }
                    } else {
                        if (cardUpNext != null) cardUpNext.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    if (cardUpNext != null) cardUpNext.setVisibility(View.GONE);
                });
    }

    private void startUpNextCountdownTimer() {
        countdownRunnable = new Runnable() {
            @Override
            public void run() {
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
                tvUpNextCountdown.setText("Live • " + sdf.format(new Date()));
                countdownHandler.postDelayed(this, 1000);
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
            Toast.makeText(this, "Failed to open camera: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void processRoutineImageUri(Uri imageUri) {
        try {
            if (imageUri == null) return;
            InputImage image = InputImage.fromFilePath(this, imageUri);
            TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

            Toast.makeText(this, "Scanning routine image with ML Kit OCR...", Toast.LENGTH_SHORT).show();

            recognizer.process(image)
                    .addOnSuccessListener(visionText -> runOnUiThread(() -> {
                        try {
                            if (isFinishing() || isDestroyed()) return;
                            parseAndSaveRoutineOcrVisionText(visionText);
                        } catch (Exception e) {
                            Log.e("RoutineActivity", "Error processing OCR result", e);
                            Toast.makeText(RoutineActivity.this, "Error processing OCR result: " + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }))
                    .addOnFailureListener(e -> runOnUiThread(() -> {
                        try {
                            if (isFinishing() || isDestroyed()) return;
                            Toast.makeText(RoutineActivity.this, "OCR scan failed: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                        } catch (Exception ex) {
                            Log.e("RoutineActivity", "Error handling OCR failure", ex);
                        }
                    }));
        } catch (Exception e) {
            Toast.makeText(this, "Error loading routine image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
                runOnUiThread(() -> Toast.makeText(this, "No text detected in the routine image.", Toast.LENGTH_LONG).show());
                return;
            }

            String fullText = visionText.getText();

            // 1. Extract Intake & Section if present e.g. "Intake: 55 - 6"
            if (sessionManager != null) {
                Matcher intakeMatcher = Pattern.compile("(?i)(?:Intake|Int|Sec|Section)[:\\s]*(\\d{1,3})\\s*[-_/,:]?\\s*(\\d{1,3})").matcher(fullText);
                if (intakeMatcher.find()) {
                    String intakeStr = intakeMatcher.group(1);
                    String secStr = intakeMatcher.group(2);
                    if (intakeStr != null && secStr != null) {
                        sessionManager.setUserIntake(intakeStr.trim());
                        sessionManager.setUserSection(secStr.trim());
                    }
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
                    if (!activeDayParsed.isEmpty()) {
                        routineList.clear();
                        routineList.addAll(activeDayParsed);
                        if (routineAdapter != null) routineAdapter.notifyDataSetChanged();
                        if (tvNoRoutineClasses != null) tvNoRoutineClasses.setVisibility(View.GONE);
                        if (recyclerViewRoutine != null) recyclerViewRoutine.setVisibility(View.VISIBLE);
                        if (cardUpNext != null) cardUpNext.setVisibility(View.VISIBLE);
                    } else if (!parsedClasses.isEmpty()) {
                        routineList.clear();
                        routineList.addAll(parsedClasses);
                        if (routineAdapter != null) routineAdapter.notifyDataSetChanged();
                        if (tvNoRoutineClasses != null) tvNoRoutineClasses.setVisibility(View.GONE);
                        if (recyclerViewRoutine != null) recyclerViewRoutine.setVisibility(View.VISIBLE);
                        if (cardUpNext != null) cardUpNext.setVisibility(View.VISIBLE);
                    }

                    Toast.makeText(RoutineActivity.this, "Extracted " + parsedClasses.size() + " classes via Matrix Grid Mapping!", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Log.e("RoutineActivity", "Error updating UI in parseAndSaveRoutineOcrVisionText", e);
                }
            });

            // Save batch to Firestore atomically, update snapshot listener, and show persistent success dialog
            saveRoutineClassesBatch(parsedClasses, () -> runOnUiThread(() -> {
                try {
                    if (isFinishing() || isDestroyed()) return;
                    loadRoutineForDay(selectedDay);
                    showScanSuccessDialog(parsedClasses);
                } catch (Exception e) {
                    Log.e("RoutineActivity", "Error handling batch save callback", e);
                }
            }));
        } catch (Exception e) {
            Log.e("RoutineActivity", "Error in parseAndSaveRoutineOcrVisionText", e);
            runOnUiThread(() -> {
                try {
                    if (!isFinishing() && !isDestroyed()) {
                        Toast.makeText(RoutineActivity.this, "Error parsing OCR vision text: " + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception ignored) {}
            });
        }
    }

    private boolean containsCourseCodePattern(String text) {
        if (text == null) return false;
        return text.matches(".*(?i)(CSE|MAT|ENG|EEE|PHY|CHEM|BUS|STA|ACT|FIN|LAW|GED|TEX|CIV)\\s*[-_]?\\s*\\d{3,4}.*") ||
                text.matches(".*[A-Z]{2,4}\\s*[-_]?\\s*\\d{3,4}.*");
    }

    private String extractCourseCode(String text) {
        if (text == null) return null;
        Matcher m = Pattern.compile("(?i)\\b((?:CSE|MAT|ENG|EEE|PHY|CHEM|BUS|STA|ACT|FIN|LAW|GED|TEX|CIV|[A-Z]{2,4})\\s*[-_]?\\s*\\d{3,4}[A-Z]?)\\b").matcher(text);
        if (m.find()) {
            String matched = m.group(1);
            if (matched != null) {
                return matched.toUpperCase().replaceAll("\\s+", " ");
            }
        }
        return text.trim();
    }

    private String extractFacultyCode(String text) {
        if (text == null) return null;
        Matcher m1 = Pattern.compile("(?i)(?:FC:|Faculty:|Instructor:|Teacher:)\\s*([A-Za-z]{2,5})").matcher(text);
        if (m1.find()) {
            return m1.group(1).toUpperCase();
        }
        Matcher m2 = Pattern.compile("\\b([A-Z]{2,5})\\b").matcher(text);
        while (m2.find()) {
            String candidate = m2.group(1);
            if (!isBlacklistedKeyword(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private String extractRoomNumber(String text) {
        if (text == null) return null;
        Matcher m1 = Pattern.compile("(?i)(?:R:|Room|R)?\\s*(\\d{3,4}|Lab\\s*\\d+)").matcher(text);
        if (m1.find()) {
            String rm = m1.group(1);
            if (rm != null && !rm.isEmpty() && !rm.equalsIgnoreCase("ROOM")) {
                return rm;
            }
        }
        Matcher m2 = Pattern.compile("\\b(\\d{3,4})\\b").matcher(text);
        if (m2.find()) {
            return m2.group(1);
        }
        return null;
    }

    private String formatRoomWithBuilding(String room) {
        if (room == null || room.trim().isEmpty()) return "Room 808 (Bldg 2)";
        String r = room.replace("Room", "").replace("ROOM", "").replace("R:", "").trim();
        if (r.matches("\\d{4}")) {
            char bldg = r.charAt(0);
            String roomNum = r.substring(1);
            return "Room " + roomNum + " (Bldg " + bldg + ")";
        }
        return r;
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
                runOnUiThread(() -> Toast.makeText(this, "No text detected in the routine image.", Toast.LENGTH_LONG).show());
                return;
            }

            // Try extracting Intake & Section if present e.g. "Intake: 48 - 2"
            if (ocrText.toLowerCase().contains("intake") && sessionManager != null) {
                try {
                    Matcher m = Pattern.compile("(?i)Intake[:\\s]*(\\d+)\\s*[-_,]\\s*(\\d+)").matcher(ocrText);
                    if (m.find()) {
                        String intakeStr = m.group(1);
                        String secStr = m.group(2);
                        if (intakeStr != null && secStr != null) {
                            sessionManager.setUserIntake(intakeStr);
                            sessionManager.setUserSection(secStr);
                        }
                    }
                } catch (Exception ignored) {
                }
            }

            String[] lines = ocrText.split("\n");
            String currentDay = selectedDay;
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

                if (trimmed.matches(".*(?i)(CSE|MAT|ENG|EEE|PHY|CHEM|BUS|STA|ACT|FIN|LAW|GED|TEX|CIV)\\s*[-_]?\\s*\\d{3,4}.*") ||
                        trimmed.matches(".*[A-Z]{2,4}\\s*[-_]?\\s*\\d{3,4}.*")) {

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
                    if (!activeDayParsed.isEmpty()) {
                        routineList.clear();
                        routineList.addAll(activeDayParsed);
                        if (routineAdapter != null) routineAdapter.notifyDataSetChanged();
                        if (tvNoRoutineClasses != null) tvNoRoutineClasses.setVisibility(View.GONE);
                        if (recyclerViewRoutine != null) recyclerViewRoutine.setVisibility(View.VISIBLE);
                        if (cardUpNext != null) cardUpNext.setVisibility(View.VISIBLE);
                    } else if (!parsedClasses.isEmpty()) {
                        routineList.clear();
                        routineList.addAll(parsedClasses);
                        if (routineAdapter != null) routineAdapter.notifyDataSetChanged();
                        if (tvNoRoutineClasses != null) tvNoRoutineClasses.setVisibility(View.GONE);
                        if (recyclerViewRoutine != null) recyclerViewRoutine.setVisibility(View.VISIBLE);
                        if (cardUpNext != null) cardUpNext.setVisibility(View.VISIBLE);
                    }

                    Toast.makeText(RoutineActivity.this, "Saving scanned routine (" + parsedClasses.size() + " classes)...", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Log.e("RoutineActivity", "Error updating UI in parseAndSaveRoutineOcrText", e);
                }
            });

            // Save batch to Firestore atomically, update snapshot listener, and show persistent success dialog
            saveRoutineClassesBatch(parsedClasses, () -> runOnUiThread(() -> {
                try {
                    if (isFinishing() || isDestroyed()) return;
                    loadRoutineForDay(selectedDay);
                    showScanSuccessDialog(parsedClasses);
                } catch (Exception e) {
                    Log.e("RoutineActivity", "Error in batch save callback", e);
                }
            }));
        } catch (Exception e) {
            Log.e("RoutineActivity", "Error in parseAndSaveRoutineOcrText", e);
            runOnUiThread(() -> {
                try {
                    if (!isFinishing() && !isDestroyed()) {
                        Toast.makeText(RoutineActivity.this, "Error parsing OCR text: " + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception ignored) {}
            });
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

            String dept = sessionManager != null ? getSanitizedKey(sessionManager.getUserDept()) : "default";
            String intake = sessionManager != null ? getSanitizedKey(sessionManager.getUserIntake()) : "default";
            String section = sessionManager != null ? getSanitizedKey(sessionManager.getUserSection()) : "default";

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            WriteBatch batch = db.batch();

            for (RoutineClassItem item : items) {
                if (item == null) continue;
                String validDay = normalizeDayName(item.day);
                if (validDay == null) validDay = item.day != null ? item.day : selectedDay;

                String startTime = item.startTime != null ? item.startTime : "08:15 AM";
                String endTime = item.endTime != null ? item.endTime : "09:45 AM";
                if ((item.startTime == null || item.endTime == null) && item.timeSlot != null && item.timeSlot.contains("-")) {
                    String[] parts = item.timeSlot.split("-");
                    startTime = parts[0].trim();
                    endTime = parts[1].trim();
                }

                Map<String, Object> map = new HashMap<>();
                map.put("day", validDay);
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
                            Toast.makeText(RoutineActivity.this, "Failed to sync routine to cloud: " + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
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

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(view)
                .create();

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
            Toast.makeText(this, "Saving class...", Toast.LENGTH_SHORT).show();

            String validDay = normalizeDayName(day);
            if (validDay == null) validDay = day;

            saveRoutineClassToFirestore(validDay, subject, timeSlot, room, teacher)
                    .addOnSuccessListener(docRef -> {
                        Toast.makeText(this, "Class added successfully!", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        loadRoutineForDay(selectedDay);
                    })
                    .addOnFailureListener(e -> {
                        btnSave.setEnabled(true);
                        Toast.makeText(this, "Failed to add class: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });

        dialog.show();
    }

    public static class RoutineClassItem {
        public String id;
        public String subject;
        public String courseCode;
        public String timeSlot;
        public String startTime;
        public String endTime;
        public String room;
        public String teacher;
        public String faculty;
        public String day;

        public RoutineClassItem() {
        }

        public String getDisplayCourseCode() {
            if (courseCode != null && !courseCode.trim().isEmpty()) return courseCode;
            if (subject != null && !subject.trim().isEmpty()) return subject;
            return "Class";
        }

        public String getDisplayFaculty() {
            if (faculty != null && !faculty.trim().isEmpty()) return faculty;
            if (teacher != null && !teacher.trim().isEmpty()) return teacher;
            return "Faculty";
        }

        public String getDay() {
            return day;
        }
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

            String rawRoom = item.room != null ? item.room.replace("Room", "").replace("ROOM", "").replace("R:", "").trim() : "--";
            if (rawRoom.isEmpty()) rawRoom = "--";
            holder.tvRoom.setText(rawRoom);

            String rawTeacher = displayFac.replace("Instructor:", "").replace("Faculty:", "").replace("FC:", "").trim();
            if (rawTeacher.isEmpty()) rawTeacher = "Faculty";
            holder.tvTeacher.setText("FC: " + rawTeacher);

            holder.tvDuration.setText(calculateDuration(timeSlot));

            if (canEdit) {
                holder.btnDelete.setVisibility(View.VISIBLE);
                holder.btnDelete.setOnClickListener(v -> {
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
                                    Toast.makeText(activity, "Class deleted", Toast.LENGTH_SHORT).show();
                                    activity.loadRoutineForDay(activity.selectedDay);
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
