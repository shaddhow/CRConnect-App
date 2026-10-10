package com.example.crconnect.core.utils;

import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.RadioGroup;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;

/**
 * Centralized Haptic Feedback Utility Helper for CRConnect.
 * Gracefully handles modern Android API levels (API 24 to API 35) using a dual-layer strategy:
 * 1. View.performHapticFeedback() as primary mechanism (respects system settings and view feedback).
 * 2. System Vibrator / VibratorManager fallback with predefined VibrationEffects or custom durations/amplitudes.
 */
public class HapticUtils {

    public enum HapticType {
        LIGHT,
        MEDIUM,
        TOGGLE,
        HEAVY
    }

    private HapticUtils() {
        // Private constructor to prevent instantiation
    }

    /**
     * Light click feedback: For subtle tab selections, navigation taps, radio buttons, and chip selections.
     * Uses HapticFeedbackConstants.KEYBOARD_TAP or VibrationEffect.EFFECT_CLICK / EFFECT_TICK.
     */
    public static void lightClick(View view) {
        if (view == null) return;
        boolean handled = false;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { // API 30+
                handled = view.performHapticFeedback(
                        HapticFeedbackConstants.CONFIRM,
                        HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
            }
            if (!handled) {
                handled = view.performHapticFeedback(
                        HapticFeedbackConstants.KEYBOARD_TAP,
                        HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
            }
        } catch (Exception ignored) {
            // Ignore potential view feedback exceptions
        }

        if (!handled) {
            vibrateFallback(view.getContext(), HapticType.LIGHT);
        }
    }

    /**
     * Medium click feedback: For standard primary button presses like "Export PDF", "Pause Voting", etc.
     * Uses HapticFeedbackConstants.VIRTUAL_KEY or VibrationEffect.EFFECT_DOUBLE_CLICK.
     */
    public static void mediumClick(View view) {
        if (view == null) return;
        boolean handled = false;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { // API 23+
                handled = view.performHapticFeedback(
                        HapticFeedbackConstants.CONTEXT_CLICK,
                        HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
            }
            if (!handled) {
                handled = view.performHapticFeedback(
                        HapticFeedbackConstants.VIRTUAL_KEY,
                        HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
            }
        } catch (Exception ignored) {
            // Ignore potential view feedback exceptions
        }

        if (!handled) {
            vibrateFallback(view.getContext(), HapticType.MEDIUM);
        }
    }

    /**
     * Toggle switch feedback: For toggle switches like "Live Voting Gate".
     * Uses HapticFeedbackConstants.TOGGLE_ON / CONFIRM or VibrationEffect.EFFECT_DOUBLE_CLICK.
     */
    public static void toggleSwitch(View view) {
        if (view == null) return;
        boolean handled = false;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // API 34+
                handled = view.performHapticFeedback(
                        HapticFeedbackConstants.TOGGLE_ON,
                        HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
            }
            if (!handled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { // API 30+
                handled = view.performHapticFeedback(
                        HapticFeedbackConstants.CONFIRM,
                        HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
            }
            if (!handled) {
                handled = view.performHapticFeedback(
                        HapticFeedbackConstants.VIRTUAL_KEY,
                        HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
            }
        } catch (Exception ignored) {
            // Ignore potential view feedback exceptions
        }

        if (!handled) {
            vibrateFallback(view.getContext(), HapticType.TOGGLE);
        }
    }

    /**
     * Heavy click feedback: For high-priority destructive/alert actions like "End Voting Now" or "Reset All".
     * Uses HapticFeedbackConstants.LONG_PRESS or HapticFeedbackConstants.REJECT or VibrationEffect.EFFECT_HEAVY_CLICK.
     */
    public static void heavyClick(View view) {
        if (view == null) return;
        boolean handled = false;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { // API 30+
                handled = view.performHapticFeedback(
                        HapticFeedbackConstants.REJECT,
                        HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
            }
            if (!handled) {
                handled = view.performHapticFeedback(
                        HapticFeedbackConstants.LONG_PRESS,
                        HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
            }
        } catch (Exception ignored) {
            // Ignore potential view feedback exceptions
        }

        if (!handled) {
            vibrateFallback(view.getContext(), HapticType.HEAVY);
        }
    }

    /**
     * System Vibrator fallback for devices or views where performHapticFeedback returns false.
     */
    private static void vibrateFallback(Context context, HapticType type) {
        if (context == null) return;
        try {
            Vibrator vibrator = getVibrator(context);
            if (vibrator == null || !vibrator.hasVibrator()) return;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { // API 29+
                int effectId;
                switch (type) {
                    case LIGHT:
                        effectId = VibrationEffect.EFFECT_TICK;
                        break;
                    case MEDIUM:
                        effectId = VibrationEffect.EFFECT_CLICK;
                        break;
                    case TOGGLE:
                        effectId = VibrationEffect.EFFECT_DOUBLE_CLICK;
                        break;
                    case HEAVY:
                    default:
                        effectId = VibrationEffect.EFFECT_HEAVY_CLICK;
                        break;
                }
                vibrator.vibrate(VibrationEffect.createPredefined(effectId));
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { // API 26+
                long duration;
                int amplitude;
                switch (type) {
                    case LIGHT:
                        duration = 10;
                        amplitude = 40;
                        break;
                    case MEDIUM:
                        duration = 20;
                        amplitude = 120;
                        break;
                    case TOGGLE:
                        duration = 25;
                        amplitude = 180;
                        break;
                    case HEAVY:
                    default:
                        duration = 45;
                        amplitude = 255;
                        break;
                }
                vibrator.vibrate(VibrationEffect.createOneShot(duration, amplitude));
            } else { // Legacy API 24+
                long duration;
                switch (type) {
                    case LIGHT:
                        duration = 10;
                        break;
                    case MEDIUM:
                        duration = 20;
                        break;
                    case TOGGLE:
                        duration = 30;
                        break;
                    case HEAVY:
                    default:
                        duration = 50;
                        break;
                }
                vibrator.vibrate(duration);
            }
        } catch (Exception ignored) {
            // Guard against missing permission or hardware errors gracefully
        }
    }

    private static Vibrator getVibrator(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // API 31+
            VibratorManager vibratorManager = (VibratorManager) context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            return vibratorManager != null ? vibratorManager.getDefaultVibrator() : null;
        } else {
            return (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        }
    }

    // --- PROGRAMMATIC LISTENER INTERCEPTORS AND WIRE HELPERS ---

    public static void attachLight(View view) {
        if (view == null) return;
        view.setHapticFeedbackEnabled(true);
        addTouchHaptic(view, HapticType.LIGHT);
    }

    public static void attachMedium(View view) {
        if (view == null) return;
        view.setHapticFeedbackEnabled(true);
        addTouchHaptic(view, HapticType.MEDIUM);
    }

    public static void attachHeavy(View view) {
        if (view == null) return;
        view.setHapticFeedbackEnabled(true);
        addTouchHaptic(view, HapticType.HEAVY);
    }

    public static void attachToggle(CompoundButton compoundButton) {
        if (compoundButton == null) return;
        compoundButton.setHapticFeedbackEnabled(true);
        addTouchHaptic(compoundButton, HapticType.TOGGLE);
    }

    public static void attachBottomNav(BottomNavigationView bottomNav) {
        if (bottomNav == null) return;
        bottomNav.setHapticFeedbackEnabled(true);
        addTouchHaptic(bottomNav, HapticType.LIGHT);
    }

    private static void addTouchHaptic(View view, HapticType type) {
        view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    triggerHaptic(v, type);
                }
                return false; // Return false so OnClickListener / OnCheckedChangeListener still fires
            }
        });
    }

    public static void triggerHaptic(View view, HapticType type) {
        if (type == null) return;
        switch (type) {
            case LIGHT:
                lightClick(view);
                break;
            case MEDIUM:
                mediumClick(view);
                break;
            case TOGGLE:
                toggleSwitch(view);
                break;
            case HEAVY:
                heavyClick(view);
                break;
        }
    }

    /**
     * Recursively attaches appropriate haptics to all views in a root layout hierarchy.
     */
    public static void attachHapticsToAllInteractiveViews(View view) {
        if (view == null) return;

        view.setHapticFeedbackEnabled(true);

        if (view instanceof CompoundButton) {
            attachToggle((CompoundButton) view);
        } else if (view instanceof BottomNavigationView) {
            attachBottomNav((BottomNavigationView) view);
        } else if (view instanceof MaterialButton) {
            MaterialButton btn = (MaterialButton) view;
            CharSequence text = btn.getText();
            String txt = text != null ? text.toString().toLowerCase() : "";
            if (txt.contains("end") || txt.contains("reset") || txt.contains("delete") || txt.contains("stop")) {
                attachHeavy(btn);
            } else if (txt.contains("+5") || txt.contains("+10") || txt.contains("-5") || txt.contains("reset input")) {
                attachLight(btn);
            } else {
                attachMedium(btn);
            }
        } else if (view instanceof RadioGroup) {
            RadioGroup rg = (RadioGroup) view;
            rg.setOnCheckedChangeListener((group, checkedId) -> lightClick(group));
        } else if (view.isClickable() || view.isFocusable()) {
            attachLight(view);
        }

        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                attachHapticsToAllInteractiveViews(group.getChildAt(i));
            }
        }
    }
}
