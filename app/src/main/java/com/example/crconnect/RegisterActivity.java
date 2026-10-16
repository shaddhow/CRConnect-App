package com.example.crconnect;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.core.widget.NestedScrollView;

import com.example.crconnect.core.utils.HapticUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.documentscanner.GmsDocumentScanner;
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions;
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning;
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText etFullName, etRegEmail, etStudentId, etTeacherCode, etRegPassword, etIntake;
    private TextInputLayout tilRegEmail;
    private RadioGroup radioGroupRole;
    private View layoutStudentFields, layoutTeacherFields;
    private RadioButton rbTeacher, rbStudent;
    private Spinner spinnerAssignedSection, spinnerStudentSection, spinnerDept, spinnerTeacherDept;
    private Button btnRegister, btnSuccessBackToLogin;
    private ImageButton btnBackToLogin;
    private MaterialButton btnScanIdCard;
    private View btnScanIdCardContainer;
    private CheckBox cbTerms;
    private TextView tvLoginLink;
    private MaterialCardView layoutFormCard, layoutSuccessCard;
    private View meterBar1, meterBar2, meterBar3, meterBar4;
    private LinearLayout layoutStickyFooter;

    private ActivityResultLauncher<IntentSenderRequest> docScannerLauncher;
    private GmsDocumentScanner documentScanner;

    private static final String[] SECTIONS = {
        "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15"
    };

    private static final String[] DEPARTMENTS = {
        "Computer Science & Engineering (CSE)",
        "Electrical & Electronic Engineering (EEE)",
        "English (ENG)",
        "Law & Justice (L&J)",
        "Accounting (ACC)",
        "Finance (FIN)",
        "Management (MGT)",
        "Marketing (MKT)",
        "Civil Engineering (CE)",
        "Textile Engineering (TE)",
        "Mathematics & Statistics (M&S)",
        "Economics (ECO)",
        "Data Science & Engineering (DSE)"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // --- STRICT EDGE-TO-EDGE FULL SCREEN WITH LIGHT BACKGROUND STATUS BARS ---
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getWindow().setNavigationBarContrastEnforced(false);
        }
        WindowInsetsControllerCompat insetsController = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (insetsController != null) {
            insetsController.setAppearanceLightStatusBars(true);
            insetsController.setAppearanceLightNavigationBars(true);
        }
        getWindow().setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.bg_gradient));

        setContentView(R.layout.activity_register);

        // Binding UI elements
        etFullName = findViewById(R.id.etFullName);
        etRegEmail = findViewById(R.id.etRegEmail);
        tilRegEmail = findViewById(R.id.tilRegEmail);
        etStudentId = findViewById(R.id.etStudentId);
        etTeacherCode = findViewById(R.id.etTeacherCode);
        etRegPassword = findViewById(R.id.etRegPassword);
        etIntake = findViewById(R.id.etIntake);
        layoutStudentFields = findViewById(R.id.layoutStudentFields);
        layoutTeacherFields = findViewById(R.id.layoutTeacherFields);
        radioGroupRole = findViewById(R.id.radioGroupRole);
        rbStudent = findViewById(R.id.rbStudent);
        rbTeacher = findViewById(R.id.rbTeacher);
        btnRegister = findViewById(R.id.btnRegister);
        btnScanIdCard = findViewById(R.id.btnScanIdCard);
        btnScanIdCardContainer = findViewById(R.id.btnScanIdCardContainer);
        cbTerms = findViewById(R.id.cbTerms);
        tvLoginLink = findViewById(R.id.tvLoginLink);
        btnBackToLogin = findViewById(R.id.btnBackToLogin);
        btnSuccessBackToLogin = findViewById(R.id.btnSuccessBackToLogin);
        layoutFormCard = findViewById(R.id.layoutFormCard);
        layoutSuccessCard = findViewById(R.id.layoutSuccessCard);
        spinnerAssignedSection = findViewById(R.id.spinnerAssignedSection);
        spinnerStudentSection = findViewById(R.id.spinnerStudentSection);
        spinnerDept = findViewById(R.id.spinnerDept);
        spinnerTeacherDept = findViewById(R.id.spinnerTeacherDept);
        layoutStickyFooter = findViewById(R.id.layoutStickyFooter);

        meterBar1 = findViewById(R.id.meterBar1);
        meterBar2 = findViewById(R.id.meterBar2);
        meterBar3 = findViewById(R.id.meterBar3);
        meterBar4 = findViewById(R.id.meterBar4);

        // Safe Area Padding & Sticky Footer Keyboard Adjustment
        View scrollReg = findViewById(R.id.scrollViewRegister);
        if (layoutStickyFooter != null) {
            ViewCompat.setOnApplyWindowInsetsListener(layoutStickyFooter, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
                int bottomPadding = Math.max(systemBars.bottom, ime.bottom);
                v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), bottomPadding + 16);
                return WindowInsetsCompat.CONSUMED;
            });
        }

        if (scrollReg != null) {
            ViewCompat.setOnApplyWindowInsetsListener(scrollReg, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(v.getPaddingLeft(), systemBars.top, v.getPaddingRight(), v.getPaddingBottom());
                return WindowInsetsCompat.CONSUMED;
            });
        }

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
        });

        ArrayAdapter<String> sectionAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, SECTIONS);
        sectionAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        if (spinnerAssignedSection != null) spinnerAssignedSection.setAdapter(sectionAdapter);
        if (spinnerStudentSection != null) spinnerStudentSection.setAdapter(sectionAdapter);

        ArrayAdapter<String> deptAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, DEPARTMENTS);
        deptAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        if (spinnerDept != null) spinnerDept.setAdapter(deptAdapter);
        if (spinnerTeacherDept != null) spinnerTeacherDept.setAdapter(deptAdapter);

        // Configure Google Play Services Document Scanner API
        GmsDocumentScannerOptions scannerOptions = new GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)
            .setPageLimit(1)
            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .build();

        documentScanner = GmsDocumentScanning.getClient(scannerOptions);

        docScannerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartIntentSenderForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    GmsDocumentScanningResult scanningResult =
                        GmsDocumentScanningResult.fromActivityResultIntent(result.getData());
                    if (scanningResult != null && scanningResult.getPages() != null && !scanningResult.getPages().isEmpty()) {
                        GmsDocumentScanningResult.Page page = scanningResult.getPages().get(0);
                        processScannedDocumentUri(page.getImageUri());
                    }
                }
            }
        );

        if (btnScanIdCardContainer != null) {
            btnScanIdCardContainer.setOnClickListener(v -> launchDocumentScanner());
        } else if (btnScanIdCard != null) {
            btnScanIdCard.setOnClickListener(v -> launchDocumentScanner());
        }

        if (btnBackToLogin != null) {
            btnBackToLogin.setOnClickListener(v -> {
                finish();
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            });
        }

        if (btnSuccessBackToLogin != null) {
            btnSuccessBackToLogin.setOnClickListener(v -> {
                finish();
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            });
        }

        // Role Toggle Reset & Dynamic Hint Setup
        if (radioGroupRole != null) {
            radioGroupRole.setOnCheckedChangeListener((group, checkedId) -> {
                if (etFullName != null) {
                    etFullName.setText("");
                    etFullName.setError(null);
                }
                if (etRegEmail != null) {
                    etRegEmail.setText("");
                    etRegEmail.setError(null);
                }
                if (etStudentId != null) {
                    etStudentId.setText("");
                    etStudentId.setError(null);
                }
                if (etTeacherCode != null) {
                    etTeacherCode.setText("");
                    etTeacherCode.setError(null);
                }
                if (etRegPassword != null) {
                    etRegPassword.setText("");
                    etRegPassword.setError(null);
                }
                if (spinnerStudentSection != null) spinnerStudentSection.setSelection(0);
                if (spinnerAssignedSection != null) spinnerAssignedSection.setSelection(0);
                if (etIntake != null) {
                    etIntake.setText("");
                    etIntake.setError(null);
                }
                if (spinnerDept != null) spinnerDept.setSelection(0);
                if (spinnerTeacherDept != null) spinnerTeacherDept.setSelection(0);

                if (checkedId == R.id.rbTeacher) {
                    if (layoutStudentFields != null) layoutStudentFields.setVisibility(View.GONE);
                    if (layoutTeacherFields != null) layoutTeacherFields.setVisibility(View.VISIBLE);
                    if (tilRegEmail != null) {
                        tilRegEmail.setHint(getString(R.string.teacher_email));
                        tilRegEmail.setHelperText(getString(R.string.teacher_email_help));
                    }
                } else {
                    if (layoutStudentFields != null) layoutStudentFields.setVisibility(View.VISIBLE);
                    if (layoutTeacherFields != null) layoutTeacherFields.setVisibility(View.GONE);
                    if (tilRegEmail != null) {
                        tilRegEmail.setHint(getString(R.string.student_email));
                        tilRegEmail.setHelperText(getString(R.string.student_email_help));
                    }
                }
            });
        }

        // Terms Checkbox & Validation Listener
        if (cbTerms != null) {
            cbTerms.setOnCheckedChangeListener((buttonView, isChecked) -> validateFormAndEnableButton());
        }

        // Password Strength Meter Listener
        if (etRegPassword != null) {
            etRegPassword.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    updatePasswordStrengthMeter(s.toString());
                    validateFormAndEnableButton();
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        setupFormChangeListeners();

        if (btnRegister != null) btnRegister.setOnClickListener(v -> performRegistration());
        if (tvLoginLink != null) {
            tvLoginLink.setOnClickListener(v -> {
                finish();
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            });
        }

        setupButtonTouchFeedback(btnRegister);
        setupHapticFeedback();
    }

    private void setupButtonTouchFeedback(View btn) {
        if (btn == null) return;
        btn.setOnTouchListener((v, event) -> {
            float durationScale = Settings.Global.getFloat(getContentResolver(), Settings.Global.ANIMATOR_DURATION_SCALE, 1.0f);
            if (durationScale > 0f) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        v.animate().scaleX(0.98f).scaleY(0.98f).setDuration(100).start();
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start();
                        break;
                }
            }
            return false;
        });
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if (v instanceof TextInputEditText || v instanceof EditText) {
                Rect outRect = new Rect();
                v.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int) ev.getRawX(), (int) ev.getRawY())) {
                    v.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    }
                }
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    private void setupHapticFeedback() {
        if (btnRegister != null) HapticUtils.attachMedium(btnRegister);
        if (btnBackToLogin != null) HapticUtils.attachLight(btnBackToLogin);
        if (btnSuccessBackToLogin != null) HapticUtils.attachLight(btnSuccessBackToLogin);
        if (tvLoginLink != null) HapticUtils.attachLight(tvLoginLink);
        if (radioGroupRole != null) HapticUtils.attachLight(radioGroupRole);
        if (cbTerms != null) HapticUtils.attachLight(cbTerms);
    }

    private void setupFormChangeListeners() {
        TextWatcher tw = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateFormAndEnableButton();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        if (etFullName != null) etFullName.addTextChangedListener(tw);
        if (etRegEmail != null) etRegEmail.addTextChangedListener(tw);
        if (etStudentId != null) etStudentId.addTextChangedListener(tw);
        if (etTeacherCode != null) etTeacherCode.addTextChangedListener(tw);
        if (etIntake != null) etIntake.addTextChangedListener(tw);
    }

    private void validateFormAndEnableButton() {
        boolean nameOk = etFullName != null && etFullName.getText() != null && etFullName.getText().toString().trim().length() >= 3;
        boolean emailOk = etRegEmail != null && etRegEmail.getText() != null && Patterns.EMAIL_ADDRESS.matcher(etRegEmail.getText().toString().trim()).matches();
        boolean isStudent = rbStudent != null && rbStudent.isChecked();
        
        boolean idOk = isStudent ? 
            (etStudentId != null && etStudentId.getText() != null && etStudentId.getText().toString().trim().length() >= 6) :
            (etTeacherCode != null && etTeacherCode.getText() != null && etTeacherCode.getText().toString().trim().length() >= 3);
            
        boolean pwOk = etRegPassword != null && etRegPassword.getText() != null && etRegPassword.getText().toString().trim().length() >= 8;
        boolean termsOk = cbTerms != null && cbTerms.isChecked();

        boolean allValid = nameOk && emailOk && idOk && pwOk && termsOk;
        if (btnRegister != null) {
            btnRegister.setEnabled(allValid);
        }
    }

    private void updatePasswordStrengthMeter(String pw) {
        int score = 0;
        if (pw.length() >= 8) score++;
        if (pw.matches(".*[0-9].*")) score++;
        if (pw.matches(".*[A-Z].*")) score++;
        if (pw.matches(".*[^a-zA-Z0-9].*")) score++;

        int color = Color.parseColor("#E2E8F0");
        if (score == 1) color = Color.parseColor("#DC2626"); // Red
        else if (score == 2) color = Color.parseColor("#F59E0B"); // Amber
        else if (score == 3) color = Color.parseColor("#0066FF"); // Blue
        else if (score >= 4) color = Color.parseColor("#16A34A"); // Green

        if (meterBar1 != null) meterBar1.setBackgroundColor(score >= 1 ? color : Color.parseColor("#E2E8F0"));
        if (meterBar2 != null) meterBar2.setBackgroundColor(score >= 2 ? color : Color.parseColor("#E2E8F0"));
        if (meterBar3 != null) meterBar3.setBackgroundColor(score >= 3 ? color : Color.parseColor("#E2E8F0"));
        if (meterBar4 != null) meterBar4.setBackgroundColor(score >= 4 ? color : Color.parseColor("#E2E8F0"));
    }

    private void launchDocumentScanner() {
        if (documentScanner == null) return;

        documentScanner.getStartScanIntent(this)
            .addOnSuccessListener(intentSender -> docScannerLauncher.launch(new IntentSenderRequest.Builder(intentSender).build()))
            .addOnFailureListener(e -> {
                Log.e("RegisterActivity", "Document scanner failed to start", e);
                Toast.makeText(this, "Failed to launch document scanner: " + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    private void processScannedDocumentUri(Uri imageUri) {
        try {
            InputImage image = InputImage.fromFilePath(this, imageUri);
            TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

            recognizer.process(image)
                .addOnSuccessListener(visionText -> parseAndAutoFillIdCardText(visionText.getText()))
                .addOnFailureListener(e -> {
                    Log.e("RegisterActivity", "ML Kit OCR scan failed", e);
                    Toast.makeText(RegisterActivity.this, "Failed to extract text: " + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                });
        } catch (IOException e) {
            Log.e("RegisterActivity", "Failed to load document image for OCR", e);
            Toast.makeText(this, "Error opening document image file", Toast.LENGTH_SHORT).show();
        }
    }

    private void parseAndAutoFillIdCardText(String text) {
        if (text == null || text.trim().isEmpty()) {
            Toast.makeText(this, "No text detected on the ID Card image. Please try a clearer photo.", Toast.LENGTH_LONG).show();
            return;
        }

        String[] lines = text.split("\n");
        String extractedName = null;
        String extractedId = null;
        String extractedIntake = null;
        String extractedDept = null;
        int idLineIndex = -1;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i] != null ? lines[i].trim() : "";
            Log.d("OCR_DEBUG", line);

            if (line.isEmpty()) continue;

            if (extractedId == null) {
                if (line.contains("ID-") || line.startsWith("20") || line.toUpperCase().contains("ID")) {
                    String cleanId = line.replaceAll("(?i)id[-:]?", "").replaceAll("[^0-9]", "").trim();
                    if (cleanId.length() >= 6) {
                        extractedId = cleanId;
                        idLineIndex = i;
                    }
                }
            }

            if (extractedIntake == null) {
                Matcher intakeMatcher = Pattern.compile("(?i)intake\\s*[:.-]?\\s*([0-9]{1,3})").matcher(line);
                if (intakeMatcher.find()) {
                    extractedIntake = intakeMatcher.group(1);
                }
            }

            if (extractedDept == null) {
                String lowerLine = line.toLowerCase();
                if (lowerLine.contains("cse") || lowerLine.contains("computer science")) {
                    extractedDept = "Computer Science & Engineering (CSE)";
                } else if (lowerLine.contains("eee") || lowerLine.contains("electrical")) {
                    extractedDept = "Electrical & Electronic Engineering (EEE)";
                } else if (lowerLine.contains("eng") || lowerLine.contains("english")) {
                    extractedDept = "English (ENG)";
                } else if (lowerLine.contains("law") || lowerLine.contains("justice")) {
                    extractedDept = "Law & Justice (L&J)";
                } else if (lowerLine.contains("acc") || lowerLine.contains("accounting")) {
                    extractedDept = "Accounting (ACC)";
                } else if (lowerLine.contains("fin") || lowerLine.contains("finance")) {
                    extractedDept = "Finance (FIN)";
                } else if (lowerLine.contains("mgt") || lowerLine.contains("management")) {
                    extractedDept = "Management (MGT)";
                } else if (lowerLine.contains("mkt") || lowerLine.contains("marketing")) {
                    extractedDept = "Marketing (MKT)";
                } else if (lowerLine.contains("civil")) {
                    extractedDept = "Civil Engineering (CE)";
                } else if (lowerLine.contains("textile")) {
                    extractedDept = "Textile Engineering (TE)";
                } else if (lowerLine.contains("math") || lowerLine.contains("stat")) {
                    extractedDept = "Mathematics & Statistics (M&S)";
                } else if (lowerLine.contains("econ")) {
                    extractedDept = "Economics (ECO)";
                } else if (lowerLine.contains("dse") || lowerLine.contains("data science")) {
                    extractedDept = "Data Science & Engineering (DSE)";
                }
            }
        }

        if (idLineIndex != -1) {
            StringBuilder nameBuilder = new StringBuilder();
            int startIndex = Math.max(0, idLineIndex - 4);
            for (int i = startIndex; i < idLineIndex; i++) {
                String line = lines[i].trim();
                if (line.isEmpty()) continue;
                String upper = line.toUpperCase();
                if (upper.contains("STUDENT") || upper.contains("BANGLADESH") || upper.contains("UNIVERSITY") ||
                    upper.contains("BUSINESS") || upper.contains("TECHNOLOGY") || upper.contains("BUBT") ||
                    upper.contains("IDENTITY") || upper.contains("CARD") || upper.contains("CAMPUS")) {
                    continue;
                }
                String cleaned = line.replaceAll("(?i)^(name|student name|name\\s*:)\\s*[:.-]?", "").trim();
                if (!cleaned.isEmpty()) {
                    if (nameBuilder.length() > 0) nameBuilder.append(" ");
                    nameBuilder.append(cleaned);
                }
            }
            String candidateName = nameBuilder.toString().trim();
            if (!candidateName.isEmpty()) extractedName = candidateName;
        }

        if (extractedName != null && !extractedName.isEmpty() && etFullName != null) {
            etFullName.setText(extractedName);
        }

        if (extractedId != null && !extractedId.isEmpty()) {
            if (rbStudent != null && rbStudent.isChecked() && etStudentId != null) {
                etStudentId.setText(extractedId);
            } else if (etTeacherCode != null) {
                etTeacherCode.setText(extractedId);
            }
        }

        if (extractedIntake != null && etIntake != null) {
            etIntake.setText(extractedIntake);
        }

        if (extractedDept != null) {
            int matchIndex = 0;
            for (int i = 0; i < DEPARTMENTS.length; i++) {
                if (DEPARTMENTS[i].equalsIgnoreCase(extractedDept) || DEPARTMENTS[i].toUpperCase().contains(extractedDept.toUpperCase())) {
                    matchIndex = i;
                    break;
                }
            }
            if (spinnerDept != null) spinnerDept.setSelection(matchIndex);
            if (spinnerTeacherDept != null) spinnerTeacherDept.setSelection(matchIndex);
        }

        Toast.makeText(this, "ID Card scanned. Information auto-filled.", Toast.LENGTH_SHORT).show();
    }

    private boolean isDummyOrGibberish(String text) {
        if (text == null || text.trim().isEmpty()) return true;
        String lower = text.trim().toLowerCase();
        String[] dummyKeywords = {
            "test", "demo", "dummy", "admin", "qwerty", "asdf", "habijabi", "habi-jabi", 
            "xyz", "xxxx", "aaaa", "zzzz", "12345", "00000", "null", "undefined", "sample", "temp"
        };
        for (String kw : dummyKeywords) {
            if (lower.contains(kw)) return true;
        }
        return false;
    }

    private void performRegistration() {
        String name = etFullName != null && etFullName.getText() != null ? etFullName.getText().toString().trim() : "";
        String email = etRegEmail != null && etRegEmail.getText() != null ? etRegEmail.getText().toString().trim() : "";
        boolean isStudent = rbStudent != null && rbStudent.isChecked();
        
        String id = isStudent ? 
            (etStudentId != null && etStudentId.getText() != null ? etStudentId.getText().toString().trim() : "") :
            (etTeacherCode != null && etTeacherCode.getText() != null ? etTeacherCode.getText().toString().trim() : "");
            
        String password = etRegPassword != null && etRegPassword.getText() != null ? etRegPassword.getText().toString().trim() : "";
        String section = isStudent ? 
            (spinnerStudentSection != null && spinnerStudentSection.getSelectedItem() != null ? spinnerStudentSection.getSelectedItem().toString() : "1") :
            (spinnerAssignedSection != null && spinnerAssignedSection.getSelectedItem() != null ? spinnerAssignedSection.getSelectedItem().toString() : "1");
        String intake = (isStudent && etIntake != null && etIntake.getText() != null) ? etIntake.getText().toString().trim() : "";
        String dept = isStudent ?
            (spinnerDept != null && spinnerDept.getSelectedItem() != null ? spinnerDept.getSelectedItem().toString() : DEPARTMENTS[0]) :
            (spinnerTeacherDept != null && spinnerTeacherDept.getSelectedItem() != null ? spinnerTeacherDept.getSelectedItem().toString() : DEPARTMENTS[0]);

        if (name.isEmpty() || name.length() < 3 || isDummyOrGibberish(name)) {
            if (etFullName != null) {
                etFullName.setError("Enter a valid full name");
                etFullName.startAnimation(AnimationUtils.loadAnimation(this, R.anim.shake));
                etFullName.requestFocus();
            }
            return;
        }

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            if (etRegEmail != null) {
                etRegEmail.setError("Enter a valid email address");
                etRegEmail.startAnimation(AnimationUtils.loadAnimation(this, R.anim.shake));
                etRegEmail.requestFocus();
            }
            return;
        }

        if (!isStudent && !email.toLowerCase().endsWith("@bubt.edu.bd")) {
            if (etRegEmail != null) {
                etRegEmail.setError("Teacher email must end with @bubt.edu.bd");
                etRegEmail.startAnimation(AnimationUtils.loadAnimation(this, R.anim.shake));
                etRegEmail.requestFocus();
            }
            return;
        }

        if (id.isEmpty() || isDummyOrGibberish(id)) {
            if (isStudent && etStudentId != null) {
                etStudentId.setError("Enter a valid Student ID");
                etStudentId.startAnimation(AnimationUtils.loadAnimation(this, R.anim.shake));
                etStudentId.requestFocus();
            } else if (etTeacherCode != null) {
                etTeacherCode.setError("Enter a valid Teacher Code");
                etTeacherCode.startAnimation(AnimationUtils.loadAnimation(this, R.anim.shake));
                etTeacherCode.requestFocus();
            }
            return;
        }

        if (password.length() < 8 || !password.matches(".*[0-9].*")) {
            if (etRegPassword != null) {
                etRegPassword.setError("8+ characters with at least 1 number required");
                etRegPassword.startAnimation(AnimationUtils.loadAnimation(this, R.anim.shake));
                etRegPassword.requestFocus();
            }
            return;
        }

        String role = isStudent ? "Student" : "Teacher";
        String assignedSection = isStudent ? section : (spinnerAssignedSection != null && spinnerAssignedSection.getSelectedItem() != null ? spinnerAssignedSection.getSelectedItem().toString() : "1");

        btnRegister.setEnabled(false);
        btnRegister.setText("Creating Account...");

        if (!isStudent) {
            FirebaseFirestore.getInstance().collection("users")
                .whereEqualTo("assignedSection", assignedSection)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    boolean alreadyClaimed = false;
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String userRole = doc.getString("role");
                        if (userRole != null && (userRole.equalsIgnoreCase("Teacher") || userRole.equalsIgnoreCase("Teacher/Admin"))) {
                            alreadyClaimed = true;
                            break;
                        }
                    }
                    if (alreadyClaimed) {
                        btnRegister.setEnabled(true);
                        btnRegister.setText("Register");
                        Toast.makeText(RegisterActivity.this, "This section is already managed", Toast.LENGTH_SHORT).show();
                        showSnackBar("This section is already managed", true);
                    } else {
                        executeFirebaseAuth(email, password, name, id, role, isStudent, section, assignedSection, intake, dept);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("RegisterActivity", "Failed to check assigned section availability in Firestore", e);
                    executeFirebaseAuth(email, password, name, id, role, isStudent, section, assignedSection, intake, dept);
                });
        } else {
            executeFirebaseAuth(email, password, name, id, role, isStudent, section, assignedSection, intake, dept);
        }
    }

    private void executeFirebaseAuth(String email, String password, String name, String id, String role, boolean isStudent, String section, String assignedSection, String intake, String dept) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener(authResult -> {
                FirebaseUser user = authResult.getUser();
                if (user == null) {
                    btnRegister.setEnabled(true);
                    btnRegister.setText("Register");
                    showSnackBar("Registration Failed: Account creation error", true);
                    return;
                }
                user.sendEmailVerification()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.d("RegisterActivity", "Verification email sent to " + email);
                        } else {
                            Log.e("RegisterActivity", "Failed to send verification email", task.getException());
                        }
                    });
                
                auth.signOut();

                saveUserDataAndFinish(user.getUid(), name, email, id, role, isStudent, section, assignedSection, intake, dept);
            })
            .addOnFailureListener(e -> {
                Log.e("RegisterActivity", "FirebaseAuth error: " + e.getMessage(), e);
                btnRegister.setEnabled(true);
                btnRegister.setText("Register");
                showSnackBar("Registration Failed: " + e.getMessage(), true);
            });
    }

    private void saveUserDataAndFinish(String uid, String name, String email, String id, String role, boolean isStudent, String section, String assignedSection, String intake, String dept) {
        String sanitizedId = id.replace(".", "_").replace("@", "_").replace("#", "_").replace("$", "_").replace("[", "_").replace("]", "_");
        String sanitizedEmail = email.replace(".", "_").replace("@", "_").replace("#", "_").replace("$", "_").replace("[", "_").replace("]", "_");

        DatabaseReference dbRef = FirebaseDatabase.getInstance("https://crconnect-58521-default-rtdb.firebaseio.com").getReference("crconnect_db");

        Map<String, Object> userMap = new HashMap<>();
        userMap.put("name", name);
        userMap.put("email", email);
        userMap.put("id", id);
        userMap.put("role", role);
        userMap.put("section", isStudent ? section : "--");
        userMap.put("assignedSection", assignedSection);
        userMap.put("intake", isStudent ? intake : "--");
        userMap.put("dept", isStudent ? dept : "Department of CSE, BUBT");
        userMap.put("deviceId", Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID));
        userMap.put("registeredAt", System.currentTimeMillis());

        if (uid != null && !uid.isEmpty()) {
            dbRef.child("users").child(uid).setValue(userMap);
        }
        dbRef.child("users").child(sanitizedId).setValue(userMap);
        dbRef.child("users").child(sanitizedEmail).setValue(userMap);

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        if (uid != null && !uid.isEmpty()) {
            firestore.collection("users").document(uid).set(userMap);
        }
        firestore.collection("users").document(sanitizedId).set(userMap);
        firestore.collection("users").document(sanitizedEmail).set(userMap);

        runOnUiThread(() -> {
            Toast.makeText(RegisterActivity.this, "Verification email sent. Please verify before logging in.", Toast.LENGTH_LONG).show();
            showSnackBar("Verification email sent. Please verify before logging in.", false);

            if (layoutFormCard != null) layoutFormCard.setVisibility(View.GONE);
            if (layoutSuccessCard != null) layoutSuccessCard.setVisibility(View.VISIBLE);
            if (layoutStickyFooter != null) layoutStickyFooter.setVisibility(View.GONE);
        });
    }

    private void showSnackBar(String message, boolean isError) {
        View rootView = findViewById(android.R.id.content);
        if (rootView == null) return;
        Snackbar snackbar = Snackbar.make(rootView, message, Snackbar.LENGTH_LONG);
        View sbView = snackbar.getView();
        sbView.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(isError ? "#DC2626" : "#16A34A")));
        
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) sbView.getLayoutParams();
        params.setMargins(32, 0, 32, 32);
        sbView.setLayoutParams(params);
        
        TextView tv = sbView.findViewById(com.google.android.material.R.id.snackbar_text);
        if (tv != null) {
            tv.setTextColor(Color.parseColor("#FFFFFF"));
            tv.setTypeface(null, Typeface.BOLD);
        }
        snackbar.show();
    }
}
