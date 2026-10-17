package com.example.crconnect;

import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.example.crconnect.core.utils.HapticUtils;
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
    private LinearLayout p1, p2, p3;
    private TextInputLayout fId, fPw;
    private TextInputEditText fid, npw;
    private MaterialButton send, reset, done;
    private TextView sentTo, again;
    private final EditText[] otpInputs = new EditText[6];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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
            insetsController.setAppearanceLightStatusBars(true);
            insetsController.setAppearanceLightNavigationBars(true);
        }
        getWindow().setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.bg_gradient));

        setContentView(R.layout.activity_forgot_password);

        // Safe Area Padding
        View scrollForgot = findViewById(R.id.scrollViewForgot);
        if (scrollForgot != null) {
            ViewCompat.setOnApplyWindowInsetsListener(scrollForgot, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, Math.max(systemBars.bottom, ime.bottom));
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

        // Binding UI elements
        fBack = findViewById(R.id.fBack);
        badgeContainer = findViewById(R.id.badgeContainer);
        paneContainer = findViewById(R.id.paneContainer);
        p1 = findViewById(R.id.p1);
        p2 = findViewById(R.id.p2);
        p3 = findViewById(R.id.p3);
        fId = findViewById(R.id.fId);
        fPw = findViewById(R.id.fPw);
        fid = findViewById(R.id.fid);
        npw = findViewById(R.id.npw);
        send = findViewById(R.id.send);
        reset = findViewById(R.id.reset);
        done = findViewById(R.id.done);
        sentTo = findViewById(R.id.sentTo);
        again = findViewById(R.id.again);

        otpInputs[0] = findViewById(R.id.otp1);
        otpInputs[1] = findViewById(R.id.otp2);
        otpInputs[2] = findViewById(R.id.otp3);
        otpInputs[3] = findViewById(R.id.otp4);
        otpInputs[4] = findViewById(R.id.otp5);
        otpInputs[5] = findViewById(R.id.otp6);

        setupOtpListeners();

        fBack.setOnClickListener(v -> {
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        send.setOnClickListener(v -> handleSendCode());
        again.setOnClickListener(v -> switchPane(p2, p1, false));
        reset.setOnClickListener(v -> handleResetPassword());
        done.setOnClickListener(v -> {
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        setupHapticFeedback();
    }

    private void setupHapticFeedback() {
        if (send != null) HapticUtils.attachMedium(send);
        if (reset != null) HapticUtils.attachMedium(reset);
        if (done != null) HapticUtils.attachMedium(done);
        if (fBack != null) HapticUtils.attachLight(fBack);
        if (again != null) HapticUtils.attachLight(again);
    }

    private void setupOtpListeners() {
        for (int i = 0; i < otpInputs.length; i++) {
            final int index = i;
            if (otpInputs[index] == null) continue;
            otpInputs[index].addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (s.length() == 1 && index < otpInputs.length - 1 && otpInputs[index + 1] != null) {
                        otpInputs[index + 1].requestFocus();
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });

            otpInputs[index].setOnKeyListener((v, keyCode, event) -> {
                if (keyCode == KeyEvent.KEYCODE_DEL && event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (otpInputs[index].getText().length() == 0 && index > 0 && otpInputs[index - 1] != null) {
                        otpInputs[index - 1].requestFocus();
                    }
                }
                return false;
            });
        }
    }

    private void handleSendCode() {
        String input = fid.getText() != null ? fid.getText().toString().trim() : "";
        if (input.isEmpty()) {
            showFieldError(fId, "Enter your ID or email");
            return;
        }

        clearFieldError(fId);
        send.setEnabled(false);

        if (input.contains("@")) {
            sendPasswordReset(input);
        } else {
            String sanitizedId = input.replace(".", "_").replace("@", "_").replace("#", "_").replace("$", "_").replace("[", "_").replace("]", "_");
            DatabaseReference dbRef = FirebaseDatabase.getInstance("https://crconnect-58521-default-rtdb.firebaseio.com").getReference("crconnect_db");

            dbRef.child("users").child(sanitizedId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String registeredEmail = snapshot.exists() ? snapshot.child("email").getValue(String.class) : null;
                    if (registeredEmail != null && !registeredEmail.isEmpty()) {
                        sendPasswordReset(registeredEmail);
                    } else {
                        sendPasswordReset(input);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    sendPasswordReset(input);
                }
            });
        }
    }

    private void sendPasswordReset(String email) {
        FirebaseAuth.getInstance().sendPasswordResetEmail(email)
            .addOnSuccessListener(unused -> {
                send.setEnabled(true);
                String masked = email.contains("@") ? email.replaceAll("^(.).*(@.*)$", "$1•••$2") : email;
                sentTo.setText(getString(R.string.enter_code_sub) + " (" + masked + ")");
                switchPane(p1, p2, false);
                if (otpInputs[0] != null) otpInputs[0].requestFocus();
            })
            .addOnFailureListener(e -> {
                send.setEnabled(true);
                Log.e("ForgotPasswordActivity", "Failed to send reset email", e);
                showFieldError(fId, "Error: " + e.getLocalizedMessage());
            });
    }

    private void handleResetPassword() {
        StringBuilder sb = new StringBuilder();
        for (EditText et : otpInputs) {
            if (et != null) sb.append(et.getText().toString().trim());
        }
        String otpCode = sb.toString();

        if (otpCode.length() < 6) {
            return;
        }

        String password = npw.getText() != null ? npw.getText().toString().trim() : "";
        if (password.length() < 8 || !password.matches(".*[0-9].*")) {
            showFieldError(fPw, "Use 8+ characters with at least 1 number");
            return;
        }

        clearFieldError(fPw);
        reset.setEnabled(false);

        // Transition to Step 3: Success
        switchPane(p2, p3, true);
    }

    private void switchPane(View currentPane, View nextPane, boolean hideBadge) {
        AutoTransition transition = new AutoTransition();
        transition.setDuration(250);
        TransitionManager.beginDelayedTransition(paneContainer, transition);

        currentPane.setVisibility(View.GONE);
        nextPane.setVisibility(View.VISIBLE);

        if (hideBadge) {
            badgeContainer.setVisibility(View.GONE);
        } else {
            badgeContainer.setVisibility(View.VISIBLE);
        }
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
