package com.example.crconnect;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class TeacherDashboardActivity extends AppCompatActivity {

    private MaterialSwitch switchMasterVoting;
    private TextInputEditText etCandidateName;
    private MaterialButton btnAddCandidate, btnRunOff, btnResetVotes, btnLogout, btnClearName;
    private ImageView imgUserProfile;
    private TextView tvHeaderTitle, tvHeaderSubtitle, tvTotalCandidates;
    private LinearLayout containerAdminCandidates;
    private DatabaseReference dbRef;
    private SessionManager sessionManager;

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

        tvHeaderTitle.setText("Admin Console");
        tvHeaderSubtitle.setText("Teacher: " + sessionManager.getUserName());

        Glide.with(this).load(R.drawable.ic_app_main).circleCrop().into(imgUserProfile);

        btnLogout.setOnClickListener(v -> {
            sessionManager.logoutUser();
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
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
        });

        btnAddCandidate.setOnClickListener(v -> {
            String name = etCandidateName.getText() != null ? etCandidateName.getText().toString().trim() : "";
            if (name.isEmpty()) { 
                etCandidateName.setError("Required"); 
                etCandidateName.requestFocus();
                return; 
            }

            String cid = dbRef.child("candidates").push().getKey();
            if (cid != null) {
                Map<String, Object> map = new HashMap<>();
                map.put("name", name);
                map.put("votes", 0);

                // Clear input immediately so user can type a new name
                etCandidateName.setText("");
                etCandidateName.clearFocus();

                dbRef.child("candidates").child(cid).setValue(map)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Successfully added to student portal!", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Log.e("TeacherDashboard", "Failed to write candidate", e);
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
            }
        });

        dbRef.child("candidates").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                tvTotalCandidates.setText("Total: " + snapshot.getChildrenCount());
                containerAdminCandidates.removeAllViews();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    addAdminRow(ds.getKey(), ds.child("name").getValue(String.class), ds.child("votes").getValue(Integer.class));
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        btnResetVotes.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                .setTitle("Reset Election")
                .setMessage("Delete all candidates and votes?")
                .setPositiveButton("Reset", (d, w) -> dbRef.child("candidates").removeValue())
                .setNegativeButton("Cancel", null).show();
        });
    }

    private void addAdminRow(String candidateId, String name, Integer vts) {
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
        row.setPadding(24, 20, 24, 20);
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
        tvVotes.setPadding(0, 4, 0, 0);

        textCol.addView(tvName);
        textCol.addView(tvVotes);

        MaterialButton btnDelete = new MaterialButton(this);
        btnDelete.setText("Delete");
        btnDelete.setTextSize(12f);
        btnDelete.setCornerRadius(14);
        btnDelete.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#F87171")));
        btnDelete.setLayoutParams(new LinearLayout.LayoutParams(-2, -2));

        btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                .setTitle("Delete Candidate")
                .setMessage("Remove " + name + "?")
                .setPositiveButton("Delete", (d, w) -> {
                    if (candidateId != null) {
                        dbRef.child("candidates").child(candidateId).removeValue()
                            .addOnSuccessListener(aVoid -> Toast.makeText(this, "Candidate removed", Toast.LENGTH_SHORT).show());
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
        });

        row.addView(textCol);
        row.addView(btnDelete);
        card.addView(row);
        containerAdminCandidates.addView(card);
    }
}
