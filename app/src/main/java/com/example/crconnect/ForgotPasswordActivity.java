package com.example.crconnect;

import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.core.widget.NestedScrollView;


import com.example.crconnect.core.utils.HapticUtils;
import com.example.crconnect.core.utils.KeyboardUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ForgotPasswordActivity extends AppCompatActivity {

    private ImageButton fBack;
    private View badgeContainer;
    private ViewGroup paneContainer;
    private LinearLayout p1, p2;
    private TextInputLayout fId;
    private TextInputEditText fid;
    private MaterialButton send, done, btnResend;
    private TextView sentTo, again;
    private String lastSentEmail = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SessionManager sessionManager = new SessionManager(this);
        SessionManager.applyThemeMode(sessionManager.getThemeMode());
        super.onCreate(savedInstanceState);

        // --- STRICT EDGE-TO-EDGE FULL SCREEN ---
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
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

        setContentView(R.layout.activity_forgot_password);

        // Safe Area Padding
        View scrollForgot = findViewById(R.id.scrollViewForgot);
        if (scrollForgot instanceof NestedScrollView) {
            KeyboardUtils.setupSmoothKeyboardHandling(this, (NestedScrollView) scrollForgot);
        }

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
        });

        // Binding UI elements
        fBack = findViewById(R.id.fBack);
        badgeContainer = findViewById(R.id.badgeContainer);
        paneContainer = findViewById(R.id.paneContainer);
        p1 = findViewById(R.id.p1);
        p2 = findViewById(R.id.p2);
        fId = findViewById(R.id.fId);
        fid = findViewById(R.id.fid);
        send = findViewById(R.id.send);
        done = findViewById(R.id.done);
        btnResend = findViewById(R.id.btnResend);
        sentTo = findViewById(R.id.sentTo);
        again = findViewById(R.id.again);

        fBack.setOnClickListener(v -> {
            HapticUtils.lightClick(v);
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        send.setOnClickListener(v -> handleSendCode());

        btnResend.setOnClickListener(v -> {
            HapticUtils.mediumClick(v);
            if (!lastSentEmail.isEmpty()) {
                sendPasswordReset(lastSentEmail, true);
            }
        });

        again.setOnClickListener(v -> {
            HapticUtils.lightClick(v);
            switchPane(p2, p1);
        });

        done.setOnClickListener(v -> {
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        setupHapticFeedback();
    }

    private void setupHapticFeedback() {
        if (send != null) HapticUtils.attachMedium(send);
        if (done != null) HapticUtils.attachMedium(done);
        if (btnResend != null) HapticUtils.attachMedium(btnResend);
        if (fBack != null) HapticUtils.attachLight(fBack);
        if (again != null) HapticUtils.attachLight(again);
    }

    private void handleSendCode() {
        String input = fid.getText() != null ? fid.getText().toString().trim() : "";
        if (input.isEmpty()) {
            showFieldError(fId, "Enter your student email");
            return;
        }

        clearFieldError(fId);
        send.setEnabled(false);
        send.setText("Sending...");

        if (input.contains("@")) {
            sendPasswordReset(input, false);
        } else {
            String sanitizedId = input.replace(".", "_").replace("@", "_").replace("#", "_").replace("$", "_").replace("[", "_").replace("]", "_");
            DatabaseReference dbRef = FirebaseDatabase.getInstance("https://crconnect-58521-default-rtdb.firebaseio.com").getReference("crconnect_db");

            dbRef.child("users").child(sanitizedId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String registeredEmail = snapshot.exists() ? snapshot.child("email").getValue(String.class) : null;
                    if (registeredEmail != null && !registeredEmail.isEmpty()) {
                        sendPasswordReset(registeredEmail, false);
                    } else {
                        sendPasswordReset(input, false);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    sendPasswordReset(input, false);
                }
            });
        }
    }

    private void sendPasswordReset(String email, boolean isResend) {
        FirebaseAuth.getInstance().sendPasswordResetEmail(email)
            .addOnSuccessListener(unused -> {
                send.setEnabled(true);
                send.setText(R.string.send_reset_link);
                lastSentEmail = email;

                if (isResend) {
                    CustomToast.show(ForgotPasswordActivity.this, "Password reset email resent to " + email);
                } else {
                    CustomToast.show(ForgotPasswordActivity.this, "Reset link sent successfully!");
                    String masked = email.contains("@") ? email.replaceAll("^(.).*(@.*)$", "$1•••$2") : email;
                    sentTo.setText(getString(R.string.check_email_sub, masked));
                    switchPane(p1, p2);
                }
            })
            .addOnFailureListener(e -> {
                send.setEnabled(true);
                send.setText(R.string.send_reset_link);
                Log.e("ForgotPasswordActivity", "Failed to send reset email", e);
                showFieldError(fId, "Error: " + e.getLocalizedMessage());
                CustomToast.show(ForgotPasswordActivity.this, "Failed to send email: " + e.getLocalizedMessage());
            });
    }

    private void switchPane(View currentPane, View nextPane) {
        AutoTransition transition = new AutoTransition();
        transition.setDuration(250);
        TransitionManager.beginDelayedTransition(paneContainer, transition);

        currentPane.setVisibility(View.GONE);
        nextPane.setVisibility(View.VISIBLE);
    }

    private void showFieldError(TextInputLayout til, String message) {
        til.setError(message);
        View v = til.getEditText() != null ? til.getEditText() : til;
        v.startAnimation(AnimationUtils.loadAnimation(this, R.anim.shake));
    }

    private void clearFieldError(TextInputLayout til) {
        til.setError(null);
    }
}
