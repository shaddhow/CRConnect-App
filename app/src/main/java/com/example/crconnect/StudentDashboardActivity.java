package com.example.crconnect;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class StudentDashboardActivity extends AppCompatActivity {

    private TextView tvVotingStatus, tvHeaderTitle, tvHeaderSubtitle;
    private TextView tvStudentName, tvStudentDetailsID, tvStudentIntake, tvStudentSection, tvStudentDept;
    private ImageView imgUserProfile;
    private MaterialButton btnLogout;
    private LinearLayout containerCandidates;
    private DatabaseReference dbRef;
    private SessionManager sessionManager;
    
    private boolean isVotingEnabled = false;
    private DataSnapshot allCandidatesSnapshot;

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

        setContentView(R.layout.activity_student_dashboard);

        // Safe Area Padding
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        dbRef = FirebaseDatabase.getInstance().getReference("crconnect_db");

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
        });

        // Live Sync
        dbRef.child("master_voting_enabled").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean status = snapshot.getValue(Boolean.class);
                isVotingEnabled = (status != null && status);
                updateStatusText();
                refreshList();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        dbRef.child("candidates").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allCandidatesSnapshot = snapshot;
                refreshList();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void displayStudentProfile() {
        tvStudentName.setText(sessionManager.getUserName());
        tvStudentDetailsID.setText(sessionManager.getUserID());
        tvStudentIntake.setText(sessionManager.getUserIntake());
        tvStudentSection.setText(sessionManager.getUserSection());
        tvStudentDept.setText(sessionManager.getUserDept());
        tvHeaderSubtitle.setText("Dashboard | " + sessionManager.getUserDept());
    }

    private void updateStatusText() {
        if (!isVotingEnabled) {
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
        for (DataSnapshot item : allCandidatesSnapshot.getChildren()) {
            addCandidateCard(item.getKey(), item.child("name").getValue(String.class), 
                item.child("votes").getValue(Integer.class));
        }
    }

    private void addCandidateCard(String id, String name, Integer vts) {
        int votes = vts != null ? vts : 0;
        MaterialCardView card = new MaterialCardView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, 0, 0, 16);
        card.setLayoutParams(params);
        card.setRadius(24);
        card.setCardElevation(4);
        card.setCardBackgroundColor(Color.WHITE);

        LinearLayout row = new LinearLayout(this);
        row.setPadding(24, 20, 24, 20);
        row.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout textCol = new LinearLayout(this);
        textCol.setOrientation(LinearLayout.VERTICAL);
        textCol.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));

        TextView tvName = new TextView(this);
        tvName.setText(name);
        tvName.setTextSize(17f);
        tvName.setTypeface(null, Typeface.BOLD);
        tvName.setTextColor(Color.parseColor("#1E293B"));

        TextView tvVotes = new TextView(this);
        tvVotes.setText("Current Votes: " + votes);
        tvVotes.setTextSize(14f);
        tvVotes.setTextColor(Color.parseColor("#64748B"));

        textCol.addView(tvName);
        textCol.addView(tvVotes);

        MaterialButton btnVote = new MaterialButton(this);
        btnVote.setText("Vote");
        btnVote.setCornerRadius(16);
        btnVote.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#2563EB")));
        btnVote.setEnabled(isVotingEnabled);

        btnVote.setOnClickListener(v -> {
            if (id != null) {
                dbRef.child("candidates").child(id).child("votes").setValue(votes + 1);
                Toast.makeText(this, "Vote Recorded!", Toast.LENGTH_SHORT).show();
            }
        });

        row.addView(textCol);
        row.addView(btnVote);
        card.addView(row);
        containerCandidates.addView(card);
    }
}
