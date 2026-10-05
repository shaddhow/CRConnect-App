package com.example.crconnect;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
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

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
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

    private TextInputEditText etFullName, etRegEmail, etStudentId, etTeacherCode, etRegPassword, etSection, etIntake, etDept;
    private RadioGroup radioGroupRole;
    private View layoutStudentFields, layoutTeacherFields;
    private RadioButton rbTeacher, rbStudent;
    private Button btnRegister, btnBackToLogin;
    private MaterialButton btnScanIdCard;
    private TextView tvLoginLink;
    private MaterialCardView layoutFormCard, layoutSuccessCard;
    private SessionManager sessionManager;

    private ActivityResultLauncher<IntentSenderRequest> docScannerLauncher;
    private GmsDocumentScanner documentScanner;

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

        setContentView(R.layout.activity_register);

        // Safe Area Padding
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, Math.max(systemBars.bottom, ime.bottom));
            return WindowInsetsCompat.CONSUMED;
        });

        // Smooth transition on back press
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
        });

        etFullName = findViewById(R.id.etFullName);
        etRegEmail = findViewById(R.id.etRegEmail);
        etStudentId = findViewById(R.id.etStudentId);
        etTeacherCode = findViewById(R.id.etTeacherCode);
        etRegPassword = findViewById(R.id.etRegPassword);
        etSection = findViewById(R.id.etSection);
        etIntake = findViewById(R.id.etIntake);
        etDept = findViewById(R.id.etDept);
        layoutStudentFields = findViewById(R.id.layoutStudentFields);
        layoutTeacherFields = findViewById(R.id.layoutTeacherFields);
        radioGroupRole = findViewById(R.id.radioGroupRole);
        rbStudent = findViewById(R.id.rbStudent);
        rbTeacher = findViewById(R.id.rbTeacher);
        btnRegister = findViewById(R.id.btnRegister);
        btnScanIdCard = findViewById(R.id.btnScanIdCard);
        tvLoginLink = findViewById(R.id.tvLoginLink);
        layoutFormCard = findViewById(R.id.layoutFormCard);
        layoutSuccessCard = findViewById(R.id.layoutSuccessCard);
        btnBackToLogin = findViewById(R.id.btnBackToLogin);

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

        if (btnScanIdCard != null) {
            btnScanIdCard.setOnClickListener(v -> launchDocumentScanner());
        }

        btnBackToLogin.setOnClickListener(v -> {
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        radioGroupRole.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbTeacher) {
                layoutStudentFields.setVisibility(View.GONE);
                layoutTeacherFields.setVisibility(View.VISIBLE);
            } else {
                layoutStudentFields.setVisibility(View.VISIBLE);
                layoutTeacherFields.setVisibility(View.GONE);
            }
        });

        btnRegister.setOnClickListener(v -> performRegistration());
        tvLoginLink.setOnClickListener(v -> {
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        // Auto-scroll up smoothly when password field gets focus in register screen
        ScrollView scrollViewRegister = findViewById(R.id.scrollViewRegister);
        etRegPassword.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && scrollViewRegister != null) {
                scrollViewRegister.postDelayed(() -> scrollViewRegister.smoothScrollTo(0, btnRegister.getBottom()), 200);
            }
        });
    }

    private void launchDocumentScanner() {
        if (documentScanner == null) return;

        if (btnScanIdCard != null) {
            btnScanIdCard.setEnabled(false);
            btnScanIdCard.setText("Opening Scanner...");
        }

        documentScanner.getStartScanIntent(this)
            .addOnSuccessListener(intentSender -> {
                if (btnScanIdCard != null) {
                    btnScanIdCard.setEnabled(true);
                    btnScanIdCard.setText("Scan BUBT ID Card");
                }
                docScannerLauncher.launch(new IntentSenderRequest.Builder(intentSender).build());
            })
            .addOnFailureListener(e -> {
                if (btnScanIdCard != null) {
                    btnScanIdCard.setEnabled(true);
                    btnScanIdCard.setText("Scan BUBT ID Card");
                }
                Log.e("RegisterActivity", "Document scanner failed to start", e);
                Toast.makeText(this, "Failed to launch document scanner: " + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    private void processScannedDocumentUri(Uri imageUri) {
        try {
            InputImage image = InputImage.fromFilePath(this, imageUri);
            TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

            if (btnScanIdCard != null) {
                btnScanIdCard.setEnabled(false);
                btnScanIdCard.setText("Processing Document...");
            }

            recognizer.process(image)
                .addOnSuccessListener(visionText -> {
                    if (btnScanIdCard != null) {
                        btnScanIdCard.setEnabled(true);
                        btnScanIdCard.setText("Scan BUBT ID Card");
                    }
                    parseAndAutoFillIdCardText(visionText.getText());
                })
                .addOnFailureListener(e -> {
                    if (btnScanIdCard != null) {
                        btnScanIdCard.setEnabled(true);
                        btnScanIdCard.setText("Scan BUBT ID Card");
                    }
                    Log.e("RegisterActivity", "ML Kit OCR scan failed", e);
                    Toast.makeText(RegisterActivity.this, "Failed to extract text from document: " + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
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

        // 1. Loop through all the recognized text lines from the ML Kit result
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i] != null ? lines[i].trim() : "";
            
            // 2. Print every single line to Logcat using Log.d("OCR_DEBUG", line)
            Log.d("OCR_DEBUG", line);

            if (line.isEmpty()) continue;

            // 3. Look for the line that contains "ID-" or starts with "20" (like "20255103311")
            if (extractedId == null) {
                if (line.contains("ID-") || line.startsWith("20") || line.toUpperCase().contains("ID")) {
                    String cleanId = line.replaceAll("(?i)id[-:]?", "").replaceAll("[^0-9]", "").trim();
                    if (cleanId.length() >= 6) {
                        extractedId = cleanId;
                        idLineIndex = i;
                    }
                }
            }

            // 5. Ensure Intake and Department map correctly as before
            if (extractedIntake == null) {
                Matcher intakeMatcher = Pattern.compile("(?i)intake\\s*[:.-]?\\s*([0-9]{1,3})").matcher(line);
                if (intakeMatcher.find()) {
                    extractedIntake = intakeMatcher.group(1);
                }
            }

            if (extractedDept == null) {
                String lowerLine = line.toLowerCase();
                if (lowerLine.contains("cse") || lowerLine.contains("b.sc. engg. in cse") || lowerLine.contains("computer science in cse")) {
                    extractedDept = "CSE";
                }
            }
        }

        // 4. Look at the lines above the ID line. Grab the text that forms the student's name, strip out words like "STUDENT" or university headers
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
                    if (nameBuilder.length() > 0) {
                        nameBuilder.append(" ");
                    }
                    nameBuilder.append(cleaned);
                }
            }
            String candidateName = nameBuilder.toString().trim();
            if (!candidateName.isEmpty()) {
                extractedName = candidateName;
            }
        }

        // Assign to fields
        if (extractedName != null && !extractedName.isEmpty()) {
            etFullName.setText(extractedName);
        } else {
            etFullName.setText("");
        }

        if (extractedId != null && !extractedId.isEmpty()) {
            if (rbStudent != null && rbStudent.isChecked()) {
                etStudentId.setText(extractedId);
            } else if (etTeacherCode != null) {
                etTeacherCode.setText(extractedId);
            }
        } else {
            if (rbStudent != null && rbStudent.isChecked()) {
                etStudentId.setText("");
            } else if (etTeacherCode != null) {
                etTeacherCode.setText("");
            }
        }

        if (extractedIntake != null && etIntake != null) {
            etIntake.setText(extractedIntake);
        } else if (etIntake != null) {
            etIntake.setText("");
        }

        if (extractedDept != null && etDept != null) {
            etDept.setText(extractedDept);
        } else if (etDept != null) {
            etDept.setText("");
        }

        // Leave Section empty
        if (etSection != null) {
            etSection.setText("");
        }

        Toast.makeText(this, "OCR scan completed successfully.", Toast.LENGTH_SHORT).show();
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
        String name = etFullName.getText() != null ? etFullName.getText().toString().trim() : "";
        String email = etRegEmail.getText() != null ? etRegEmail.getText().toString().trim() : "";
        boolean isStudent = rbStudent != null && rbStudent.isChecked();
        
        String id = isStudent ? 
            (etStudentId != null && etStudentId.getText() != null ? etStudentId.getText().toString().trim() : "") :
            (etTeacherCode != null && etTeacherCode.getText() != null ? etTeacherCode.getText().toString().trim() : "");
            
        String password = etRegPassword.getText() != null ? etRegPassword.getText().toString().trim() : "";
        String section = (isStudent && etSection != null && etSection.getText() != null) ? etSection.getText().toString().trim() : "";
        String intake = (isStudent && etIntake != null && etIntake.getText() != null) ? etIntake.getText().toString().trim() : "";
        String dept = (isStudent && etDept != null && etDept.getText() != null) ? etDept.getText().toString().trim() : "";

        // 1. FULL NAME VALIDATION
        if (name.isEmpty()) {
            Toast.makeText(this, "Full Name is required!", Toast.LENGTH_SHORT).show();
            etFullName.setError("Full Name is required!");
            etFullName.requestFocus();
            return;
        }

        if (name.length() < 3 || name.length() > 50 || isDummyOrGibberish(name) || !name.matches("^[a-zA-Z.\\s-]+$")) {
            Toast.makeText(this, "Invalid Full Name: Please enter a legitimate name (e.g., John Doe)", Toast.LENGTH_LONG).show();
            etFullName.setError("Enter a legitimate full name (e.g., John Doe)");
            etFullName.requestFocus();
            return;
        }

        // 2. EMAIL VALIDATION
        if (email.isEmpty()) {
            Toast.makeText(this, "Email address is required!", Toast.LENGTH_SHORT).show();
            etRegEmail.setError("Email address is required!");
            etRegEmail.requestFocus();
            return;
        }

        boolean isValidEmailStructure = Patterns.EMAIL_ADDRESS.matcher(email).matches();
        if (!isValidEmailStructure) {
            Toast.makeText(this, "Invalid Email: Please enter a valid email address (e.g., user@gmail.com)", Toast.LENGTH_LONG).show();
            etRegEmail.setError("Enter a valid email address");
            etRegEmail.requestFocus();
            return;
        }

        // 3. ID / TEACHER CODE VALIDATION
        if (id.isEmpty()) {
            if (isStudent) {
                Toast.makeText(this, "Student ID is required!", Toast.LENGTH_SHORT).show();
                etStudentId.setError("Student ID is required!");
                etStudentId.requestFocus();
            } else {
                Toast.makeText(this, "Teacher Code/ID is required!", Toast.LENGTH_SHORT).show();
                etTeacherCode.setError("Teacher Code/ID is required!");
                etTeacherCode.requestFocus();
            }
            return;
        }

        if (isStudent) {
            // Student ID format validation (e.g., numeric BUBT Student ID like 20211103001)
            if (isDummyOrGibberish(id) || !id.matches("^[0-9-]{6,15}$")) {
                Toast.makeText(this, "Invalid Student ID: Please enter a legitimate BUBT Student ID (e.g., 20211103001)", Toast.LENGTH_LONG).show();
                etStudentId.setError("Enter a valid numeric BUBT Student ID");
                etStudentId.requestFocus();
                return;
            }
        } else {
            // Teacher Code format validation (e.g., CSE-101, T-501)
            if (isDummyOrGibberish(id) || id.length() < 3 || !id.matches("^[a-zA-Z0-9.-]{3,15}$")) {
                Toast.makeText(this, "Invalid Teacher Code: Please enter a valid Teacher Code (e.g., CSE-101 or T-501)", Toast.LENGTH_LONG).show();
                etTeacherCode.setError("Enter a valid Teacher Code (e.g., CSE-101)");
                etTeacherCode.requestFocus();
                return;
            }
        }

        // 4. STUDENT SPECIFIC FIELDS VALIDATION
        if (isStudent) {
            // Intake Validation (e.g., 55, 55/8)
            if (intake.isEmpty()) {
                Toast.makeText(this, "Intake is required!", Toast.LENGTH_SHORT).show();
                etIntake.setError("Intake is required!");
                etIntake.requestFocus();
                return;
            }
            if (isDummyOrGibberish(intake) || !intake.matches("^(Intake\\s*)?[0-9]{1,3}(/[0-9]{1,2})?$")) {
                Toast.makeText(this, "Invalid Intake: Please enter a valid intake number (e.g., 55 or 55/8)", Toast.LENGTH_LONG).show();
                etIntake.setError("Enter a valid intake format (e.g., 55 or 55/8)");
                etIntake.requestFocus();
                return;
            }

            // Section Validation (e.g., 8, Sec-A, 55/8)
            if (section.isEmpty()) {
                Toast.makeText(this, "Section is required!", Toast.LENGTH_SHORT).show();
                etSection.setError("Section is required!");
                etSection.requestFocus();
                return;
            }
            if (isDummyOrGibberish(section) || !section.matches("^[a-zA-Z0-9/\\s-]{1,10}$")) {
                Toast.makeText(this, "Invalid Section: Please enter a valid section (e.g., 8, Sec-A, or 55/8)", Toast.LENGTH_LONG).show();
                etSection.setError("Enter a valid section format (e.g., 8 or 55/8)");
                etSection.requestFocus();
                return;
            }

            // Department Validation (e.g., CSE, Department of CSE)
            if (dept.isEmpty()) {
                Toast.makeText(this, "Department is required!", Toast.LENGTH_SHORT).show();
                etDept.setError("Department is required!");
                etDept.requestFocus();
                return;
            }
            if (isDummyOrGibberish(dept) || dept.length() < 2 || !dept.matches("^[a-zA-Z.\\s,-]+$")) {
                Toast.makeText(this, "Invalid Department: Please enter a valid department (e.g., CSE or Department of CSE)", Toast.LENGTH_LONG).show();
                etDept.setError("Enter a valid department name (e.g., CSE)");
                etDept.requestFocus();
                return;
            }
        }

        // 5. STRONG PASSWORD REQUIREMENTS
        if (password.isEmpty()) {
            Toast.makeText(this, "Password is required!", Toast.LENGTH_SHORT).show();
            etRegPassword.setError("Password is required!");
            etRegPassword.requestFocus();
            return;
        }

        boolean hasLetter = password.matches(".*[a-zA-Z].*");
        boolean hasDigit = password.matches(".*[0-9].*");
        if (password.length() < 8 || !hasLetter || !hasDigit || isDummyOrGibberish(password)) {
            Toast.makeText(this, "Password Too Weak: Must be at least 8 characters long and contain both letters and numbers.", Toast.LENGTH_LONG).show();
            etRegPassword.setError("Minimum 8 characters with letters and numbers");
            etRegPassword.requestFocus();
            return;
        }

        String role = isStudent ? "Student" : "Teacher/Admin";

        // Show loading state on button
        btnRegister.setEnabled(false);
        btnRegister.setText("Creating Account...");

        FirebaseAuth auth = FirebaseAuth.getInstance();
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener(authResult -> {
                FirebaseUser user = authResult.getUser();
                if (user == null) {
                    btnRegister.setEnabled(true);
                    btnRegister.setText("REGISTER");
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
                        // Immediately sign out to prevent auto-login before email verification
                        FirebaseAuth.getInstance().signOut();
                        sessionManager.logoutUser();
                    });
                saveUserDataAndFinish(user.getUid(), name, email, id, role, isStudent, section, intake, dept);
            })
            .addOnFailureListener(e -> {
                Log.e("RegisterActivity", "FirebaseAuth error: " + e.getMessage(), e);
                btnRegister.setEnabled(true);
                btnRegister.setText("REGISTER");
                showSnackBar("Registration Failed: " + e.getMessage(), true);
            });
    }

    private void saveUserDataAndFinish(String uid, String name, String email, String id, String role, boolean isStudent, String section, String intake, String dept) {
        String sanitizedId = id.replace(".", "_").replace("@", "_").replace("#", "_").replace("$", "_").replace("[", "_").replace("]", "_");
        String sanitizedEmail = email.replace(".", "_").replace("@", "_").replace("#", "_").replace("$", "_").replace("[", "_").replace("]", "_");

        DatabaseReference dbRef = FirebaseDatabase.getInstance("https://crconnect-58521-default-rtdb.firebaseio.com").getReference("crconnect_db");

        Map<String, Object> userMap = new HashMap<>();
        userMap.put("name", name);
        userMap.put("email", email);
        userMap.put("id", id);
        userMap.put("role", role);
        userMap.put("section", isStudent ? section : "--");
        userMap.put("intake", isStudent ? intake : "--");
        userMap.put("dept", isStudent ? dept : "Department of CSE, BUBT");
        userMap.put("deviceId", Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID));
        userMap.put("registeredAt", System.currentTimeMillis());

        // 1. Dual-Database Persistence: Firebase Realtime Database
        if (uid != null && !uid.isEmpty()) {
            dbRef.child("users").child(uid).setValue(userMap);
        }
        dbRef.child("users").child(sanitizedId).setValue(userMap);
        dbRef.child("users").child(sanitizedEmail).setValue(userMap);

        // 2. Dual-Database Persistence: Firebase Firestore
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        if (uid != null && !uid.isEmpty()) {
            firestore.collection("users").document(uid).set(userMap);
        }
        firestore.collection("users").document(sanitizedId).set(userMap);

        // Ensure unverified user session is terminated locally and remotely
        FirebaseAuth.getInstance().signOut();
        sessionManager.logoutUser();

        runOnUiThread(() -> {
            if (layoutFormCard != null) layoutFormCard.setVisibility(View.GONE);
            if (layoutSuccessCard != null) layoutSuccessCard.setVisibility(View.VISIBLE);
        });
    }

    private void showSnackBar(String message, boolean isError) {
        View rootView = findViewById(android.R.id.content);
        Snackbar snackbar = Snackbar.make(rootView, message, Snackbar.LENGTH_LONG);
        View sbView = snackbar.getView();
        sbView.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(isError ? "#EF4444" : "#10B981")));
        
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) sbView.getLayoutParams();
        params.setMargins(32, 0, 32, 32);
        sbView.setLayoutParams(params);
        
        TextView tv = sbView.findViewById(com.google.android.material.R.id.snackbar_text);
        tv.setTextColor(Color.parseColor("#F8FAFC"));
        tv.setTypeface(null, Typeface.BOLD);
        snackbar.show();
    }
}
