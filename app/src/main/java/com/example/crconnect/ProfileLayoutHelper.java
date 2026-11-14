package com.example.crconnect;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller/Helper for layout_profile.xml and its fragments/activities.
 * Handles Student ID vs Email mapping, interactive credential copy dialogs,
 * and section editing synced directly to SessionManager and Cloud Firestore.
 */
public class ProfileLayoutHelper {

    public static void setupProfileView(@NonNull View view, @NonNull Context context, @NonNull SessionManager session) {
        TextView tvStudentId = view.findViewById(R.id.tvProfileStudentID);
        TextView tvEmail = view.findViewById(R.id.tvProfileEmail);
        TextView tvSection = view.findViewById(R.id.tvProfileSection);
        TextView tvIntake = view.findViewById(R.id.tvProfileIntake);
        TextView tvName = view.findViewById(R.id.tvProfileName);
        TextView tvDept = view.findViewById(R.id.tvProfileDept);

        View btnStudentId = view.findViewById(R.id.btnProfileStudentID);
        if (btnStudentId == null && tvStudentId != null && tvStudentId.getParent() instanceof View) {
            btnStudentId = (View) tvStudentId.getParent();
        }

        View btnSection = view.findViewById(R.id.btnProfileSection);
        if (btnSection == null && tvSection != null && tvSection.getParent() instanceof View) {
            btnSection = (View) tvSection.getParent();
            if (btnSection != null && btnSection.getParent() instanceof View && !(btnSection.getId() == R.id.btnProfileSection)) {
                btnSection = (View) btnSection.getParent();
            }
        }

        // 1. Map Name & Department
        if (tvName != null) {
            tvName.setText(session.getUserName().isEmpty() ? "Student" : session.getUserName());
        }
        if (tvDept != null) {
            String dept = session.getUserDept();
            tvDept.setText((dept != null && dept.contains("CSE")) ? "Department of CSE, BUBT" : (dept != null ? dept : "Department of CSE, BUBT"));
        }

        // 2. Map Email
        String email = session.getUserEmail();
        if (tvEmail != null) {
            tvEmail.setText(email.isEmpty() ? "student@bubt.edu.bd" : email);
        }

        // 3. Map Intake & Section
        if (tvIntake != null) {
            tvIntake.setText(session.getUserIntake().isEmpty() ? "--" : session.getUserIntake());
        }
        if (tvSection != null) {
            tvSection.setText(session.getUserSection().isEmpty() ? "--" : session.getUserSection());
        }

        // 4. Map Student ID (must never show email prefix)
        updateStudentIdDisplay(tvStudentId, session.getUserID(), email, context);

        // Fetch actual student ID from backend if session has an email or empty
        syncStudentIdFromBackend(tvStudentId, session, context);

        // 5. Click listener on Student ID Container
        if (btnStudentId != null) {
            btnStudentId.setClickable(true);
            btnStudentId.setFocusable(true);
            btnStudentId.setOnClickListener(v -> showStudentIdCredentialsDialog(context, session, tvStudentId));
        }

        // 6. Click listener on Section Container
        if (btnSection != null) {
            btnSection.setClickable(true);
            btnSection.setFocusable(true);
            btnSection.setOnClickListener(v -> showEditSectionDialog(context, session, tvSection));
        }
    }

    private static void updateStudentIdDisplay(TextView tvStudentId, String studentId, String email, Context context) {
        if (tvStudentId == null) return;
        boolean isInvalid = studentId == null || studentId.trim().isEmpty() || studentId.equals("--")
                || studentId.contains("@") || studentId.equalsIgnoreCase(email);
        if (isInvalid) {
            tvStudentId.setText("Not Set");
            tvStudentId.setTextColor(ContextCompat.getColor(context, R.color.brand_blue));
        } else {
            tvStudentId.setText(studentId.trim());
            tvStudentId.setTextColor(ContextCompat.getColor(context, R.color.title_text));
        }
    }

    private static void syncStudentIdFromBackend(TextView tvStudentId, SessionManager session, Context context) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        String uid = user.getUid();
        DatabaseReference rtdb = FirebaseDatabase.getInstance("https://crconnect-58521-default-rtdb.firebaseio.com").getReference("crconnect_db");

        rtdb.child("users").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;
                String rawId = snapshot.child("id").getValue(String.class);
                if (rawId == null || rawId.contains("@")) {
                    rawId = snapshot.child("studentId").getValue(String.class);
                }
                if (rawId != null && !rawId.trim().isEmpty() && !rawId.contains("@") && !rawId.equals("--")) {
                    session.setUserID(rawId.trim());
                    updateStudentIdDisplay(tvStudentId, rawId.trim(), session.getUserEmail(), context);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        FirebaseFirestore.getInstance().collection("users").document(uid).get().addOnSuccessListener(doc -> {
            if (doc != null && doc.exists()) {
                String rawId = doc.getString("id");
                if (rawId == null || rawId.contains("@")) {
                    rawId = doc.getString("studentId");
                }
                if (rawId != null && !rawId.trim().isEmpty() && !rawId.contains("@") && !rawId.equals("--")) {
                    session.setUserID(rawId.trim());
                    updateStudentIdDisplay(tvStudentId, rawId.trim(), session.getUserEmail(), context);
                }
            }
        });
    }

    public static void showStudentIdCredentialsDialog(@NonNull Context context, @NonNull SessionManager session, TextView tvStudentId) {
        String studentId = session.getUserID();
        String email = session.getUserEmail().isEmpty() ? "student@bubt.edu.bd" : session.getUserEmail();
        boolean hasValidId = studentId != null && !studentId.trim().isEmpty() && !studentId.equals("--")
                && !studentId.contains("@") && !studentId.equalsIgnoreCase(email);

        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 48, 48, 48);
        layout.setBackgroundColor(Color.parseColor("#FFFFFF"));

        // Header Title
        TextView tvTitle = new TextView(context);
        tvTitle.setText("Student Account Details");
        tvTitle.setTextSize(18);
        tvTitle.setTextColor(Color.parseColor("#0F172A"));
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        layout.addView(tvTitle);

        TextView tvSub = new TextView(context);
        tvSub.setText("BUBT Academic Credentials");
        tvSub.setTextSize(12);
        tvSub.setTextColor(Color.parseColor("#64748B"));
        tvSub.setPadding(0, 4, 0, 24);
        layout.addView(tvSub);

        // Student ID Card Pill Box
        LinearLayout idCard = createPillCard(context);
        TextView lblId = createCardLabel(context, "STUDENT ID");
        idCard.addView(lblId);

        LinearLayout idRow = new LinearLayout(context);
        idRow.setOrientation(LinearLayout.HORIZONTAL);
        idRow.setGravity(android.view.Gravity.CENTER_VERTICAL);

        EditText etIdInput = new EditText(context);
        etIdInput.setText(hasValidId ? studentId : "");
        etIdInput.setHint("Enter BUBT ID (e.g. 22235103132)");
        etIdInput.setTextColor(Color.parseColor("#0F172A"));
        etIdInput.setTextSize(15);
        etIdInput.setBackgroundColor(Color.TRANSPARENT);
        etIdInput.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        idRow.addView(etIdInput);

        if (hasValidId) {
            ImageButton btnCopyId = createCopyButton(context, () -> {
                copyToClipboard(context, studentId, "Student ID copied to clipboard");
            });
            idRow.addView(btnCopyId);
        }
        idCard.addView(idRow);
        layout.addView(idCard);

        // Space
        View space = new View(context);
        space.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 24));
        layout.addView(space);

        // Email Card Pill Box
        LinearLayout emailCard = createPillCard(context);
        TextView lblEmail = createCardLabel(context, "REGISTERED EMAIL");
        emailCard.addView(lblEmail);

        LinearLayout emailRow = new LinearLayout(context);
        emailRow.setOrientation(LinearLayout.HORIZONTAL);
        emailRow.setGravity(android.view.Gravity.CENTER_VERTICAL);

        TextView tvEmailVal = new TextView(context);
        tvEmailVal.setText(email);
        tvEmailVal.setTextColor(Color.parseColor("#0F172A"));
        tvEmailVal.setTextSize(14);
        tvEmailVal.setTypeface(null, android.graphics.Typeface.BOLD);
        tvEmailVal.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        emailRow.addView(tvEmailVal);

        ImageButton btnCopyEmail = createCopyButton(context, () -> {
            copyToClipboard(context, email, "Email copied to clipboard");
        });
        emailRow.addView(btnCopyEmail);
        emailCard.addView(emailRow);
        layout.addView(emailCard);

        // Buttons
        LinearLayout btnRow = new LinearLayout(context);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setGravity(android.view.Gravity.END);
        btnRow.setPadding(0, 36, 0, 0);

        MaterialButton btnCancel = new MaterialButton(context, null, com.google.android.material.R.attr.borderlessButtonStyle);
        btnCancel.setText("Close");
        btnCancel.setTextColor(Color.parseColor("#64748B"));
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnRow.addView(btnCancel);

        MaterialButton btnSave = new MaterialButton(context);
        btnSave.setText("Save ID");
        btnSave.setBackgroundColor(Color.parseColor("#0066FF"));
        btnSave.setTextColor(Color.WHITE);
        btnSave.setCornerRadius(16);
        btnSave.setOnClickListener(v -> {
            String newId = etIdInput.getText().toString().trim();
            if (newId.isEmpty() || newId.contains("@")) {
                CustomToast.show(context, "Please enter a valid Student ID");
                return;
            }
            session.setUserID(newId);
            updateStudentIdDisplay(tvStudentId, newId, session.getUserEmail(), context);
            syncStudentIdToBackend(newId, session);
            CustomToast.show(context, "Student ID updated successfully");
            dialog.dismiss();
        });
        btnRow.addView(btnSave);

        layout.addView(btnRow);

        dialog.setContentView(layout);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout((int) (context.getResources().getDisplayMetrics().widthPixels * 0.9), ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        dialog.show();
    }

    public static void showEditSectionDialog(@NonNull Context context, @NonNull SessionManager session, TextView tvSection) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 48, 48, 48);
        layout.setBackgroundColor(Color.parseColor("#FFFFFF"));

        // Title
        TextView tvTitle = new TextView(context);
        tvTitle.setText("Edit Class Section");
        tvTitle.setTextSize(18);
        tvTitle.setTextColor(Color.parseColor("#0F172A"));
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        layout.addView(tvTitle);

        // Current Details Subtitle
        TextView tvSub = new TextView(context);
        tvSub.setText("Intake " + session.getUserIntake() + " • Current Section " + session.getUserSection() + " • Dept. of CSE");
        tvSub.setTextSize(12);
        tvSub.setTextColor(Color.parseColor("#64748B"));
        tvSub.setPadding(0, 4, 0, 20);
        layout.addView(tvSub);

        // Section Input Box Pill
        LinearLayout pillCard = createPillCard(context);
        TextView lblSec = createCardLabel(context, "ENTER NEW SECTION");
        pillCard.addView(lblSec);

        EditText etSection = new EditText(context);
        etSection.setText(session.getUserSection().equals("--") ? "" : session.getUserSection());
        etSection.setHint("e.g. 6, 5, 5A, 1");
        etSection.setTextColor(Color.parseColor("#0F172A"));
        etSection.setTextSize(16);
        etSection.setTypeface(null, android.graphics.Typeface.BOLD);
        etSection.setBackgroundColor(Color.TRANSPARENT);
        pillCard.addView(etSection);
        layout.addView(pillCard);

        // Quick select row (1, 2, 3, 4, 5, 6, 7, 8)
        TextView tvQuick = new TextView(context);
        tvQuick.setText("Quick Select:");
        tvQuick.setTextSize(11);
        tvQuick.setTextColor(Color.parseColor("#64748B"));
        tvQuick.setPadding(0, 16, 0, 8);
        tvQuick.setTypeface(null, android.graphics.Typeface.BOLD);
        layout.addView(tvQuick);

        LinearLayout quickRow = new LinearLayout(context);
        quickRow.setOrientation(LinearLayout.HORIZONTAL);
        String[] options = {"1", "2", "3", "4", "5", "6", "7", "8"};
        for (String opt : options) {
            TextView chip = new TextView(context);
            chip.setText(opt);
            chip.setTextSize(13);
            chip.setPadding(20, 12, 20, 12);
            chip.setBackgroundColor(Color.parseColor("#F8FAFC"));
            chip.setTextColor(Color.parseColor("#0F172A"));
            chip.setTypeface(null, android.graphics.Typeface.BOLD);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            lp.setMargins(4, 0, 4, 0);
            chip.setLayoutParams(lp);
            chip.setGravity(android.view.Gravity.CENTER);
            chip.setOnClickListener(v -> etSection.setText(opt));
            quickRow.addView(chip);
        }
        layout.addView(quickRow);

        // Buttons
        LinearLayout btnRow = new LinearLayout(context);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setGravity(android.view.Gravity.END);
        btnRow.setPadding(0, 32, 0, 0);

        MaterialButton btnCancel = new MaterialButton(context, null, com.google.android.material.R.attr.borderlessButtonStyle);
        btnCancel.setText("Cancel");
        btnCancel.setTextColor(Color.parseColor("#64748B"));
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnRow.addView(btnCancel);

        MaterialButton btnSave = new MaterialButton(context);
        btnSave.setText("Save Section");
        btnSave.setBackgroundColor(Color.parseColor("#0066FF"));
        btnSave.setTextColor(Color.WHITE);
        btnSave.setCornerRadius(16);
        btnSave.setOnClickListener(v -> {
            String newSection = etSection.getText().toString().trim();
            if (newSection.isEmpty()) {
                CustomToast.show(context, "Please enter a valid section");
                return;
            }
            session.setUserSection(newSection);
            if (tvSection != null) {
                tvSection.setText(newSection);
            }
            syncSectionToBackend(newSection, session);
            CustomToast.show(context, "Section updated successfully to " + newSection);
            dialog.dismiss();
        });
        btnRow.addView(btnSave);

        layout.addView(btnRow);

        dialog.setContentView(layout);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout((int) (context.getResources().getDisplayMetrics().widthPixels * 0.9), ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        dialog.show();
    }

    private static LinearLayout createPillCard(Context context) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(24, 16, 24, 16);
        android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
        gd.setColor(Color.parseColor("#F8FAFC"));
        gd.setCornerRadius(16);
        gd.setStroke(2, Color.parseColor("#E2E8F0"));
        card.setBackground(gd);
        return card;
    }

    private static TextView createCardLabel(Context context, String text) {
        TextView tv = new TextView(context);
        tv.setText(text);
        tv.setTextSize(10);
        tv.setTextColor(Color.parseColor("#0066FF"));
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        tv.setLetterSpacing(0.05f);
        tv.setPadding(0, 0, 0, 4);
        return tv;
    }

    private static ImageButton createCopyButton(Context context, Runnable onCopy) {
        ImageButton btn = new ImageButton(context);
        btn.setImageResource(android.R.drawable.ic_menu_save);
        btn.setColorFilter(Color.parseColor("#0066FF"));
        btn.setBackgroundColor(Color.TRANSPARENT);
        btn.setPadding(8, 8, 8, 8);
        btn.setOnClickListener(v -> onCopy.run());
        return btn;
    }

    private static void copyToClipboard(Context context, String text, String toastMsg) {
        ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) {
            ClipData clip = ClipData.newPlainText("CRConnect", text);
            cm.setPrimaryClip(clip);
            CustomToast.show(context, toastMsg);
        }
    }

    private static void syncStudentIdToBackend(String newId, SessionManager session) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String uid = user != null ? user.getUid() : "";
        String email = session.getUserEmail();
        String sanitizedEmail = email.replace("@", "_").replace(".", "_");
        String sanitizedId = newId.replace(".", "_").replace("@", "_");

        DatabaseReference rtdb = FirebaseDatabase.getInstance("https://crconnect-58521-default-rtdb.firebaseio.com").getReference("crconnect_db");
        if (!uid.isEmpty()) {
            rtdb.child("users").child(uid).child("id").setValue(newId);
            rtdb.child("users").child(uid).child("studentId").setValue(newId);
        }
        if (!sanitizedEmail.isEmpty()) {
            rtdb.child("users").child(sanitizedEmail).child("id").setValue(newId);
            rtdb.child("users").child(sanitizedEmail).child("studentId").setValue(newId);
        }
        rtdb.child("users").child(sanitizedId).child("id").setValue(newId);

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        Map<String, Object> map = new HashMap<>();
        map.put("id", newId);
        map.put("studentId", newId);

        if (!uid.isEmpty()) {
            firestore.collection("users").document(uid).set(map, SetOptions.merge());
        }
        if (!sanitizedEmail.isEmpty()) {
            firestore.collection("users").document(sanitizedEmail).set(map, SetOptions.merge());
        }
        firestore.collection("users").document(sanitizedId).set(map, SetOptions.merge());
    }

    private static void syncSectionToBackend(String newSection, SessionManager session) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String uid = user != null ? user.getUid() : "";
        String studentId = session.getUserID();
        String sanitizedStudentId = studentId.replace(".", "_").replace("@", "_");
        String email = session.getUserEmail();
        String sanitizedEmail = email.replace("@", "_").replace(".", "_");

        DatabaseReference rtdb = FirebaseDatabase.getInstance("https://crconnect-58521-default-rtdb.firebaseio.com").getReference("crconnect_db");
        if (!uid.isEmpty()) {
            rtdb.child("users").child(uid).child("section").setValue(newSection);
        }
        if (!sanitizedStudentId.isEmpty()) {
            rtdb.child("users").child(sanitizedStudentId).child("section").setValue(newSection);
        }
        if (!sanitizedEmail.isEmpty()) {
            rtdb.child("users").child(sanitizedEmail).child("section").setValue(newSection);
        }

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        Map<String, Object> map = new HashMap<>();
        map.put("section", newSection);

        if (!uid.isEmpty()) {
            firestore.collection("users").document(uid).set(map, SetOptions.merge());
        }
        if (!sanitizedStudentId.isEmpty()) {
            firestore.collection("users").document(sanitizedStudentId).set(map, SetOptions.merge());
        }
        if (!sanitizedEmail.isEmpty()) {
            firestore.collection("users").document(sanitizedEmail).set(map, SetOptions.merge());
        }
    }
}
