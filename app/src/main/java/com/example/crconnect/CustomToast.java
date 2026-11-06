package com.example.crconnect;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class CustomToast {

    public enum ToastType {
        INFO,
        SUCCESS,
        WARNING,
        ERROR
    }

    private static final String TAG_CUSTOM_TOAST = "CRCONNECT_CUSTOM_TOAST_TAG";

    public static void show(Context context, String message) {
        show(context, message, detectType(message), null);
    }

    public static void show(Context context, String message, ToastType type) {
        show(context, message, type, null);
    }

    public static void showWithEmoji(Context context, String message, String customEmoji) {
        show(context, message, detectType(message), customEmoji);
    }

    public static void showSuccess(Context context, String message) {
        show(context, message, ToastType.SUCCESS, "✨");
    }

    public static void showError(Context context, String message) {
        show(context, message, ToastType.ERROR, "🚨");
    }

    public static void showWarning(Context context, String message) {
        show(context, message, ToastType.WARNING, "⚠️");
    }

    public static void showInfo(Context context, String message) {
        show(context, message, ToastType.INFO, "💡");
    }

    public static void show(Context context, String message, ToastType type, String customEmoji) {
        if (context == null || message == null || message.trim().isEmpty()) return;

        Activity activity = getActivity(context);
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            showFallbackToast(context, message);
            return;
        }

        ToastType finalType = (type != null) ? type : detectType(message);
        String finalEmoji = (customEmoji != null && !customEmoji.trim().isEmpty()) ? customEmoji : getDefaultEmoji(finalType);

        activity.runOnUiThread(() -> displayBottomToast(activity, message, finalType, finalEmoji));
    }

    private static void displayBottomToast(Activity activity, String message, ToastType type, String emoji) {
        try {
            ViewGroup contentRoot = activity.findViewById(android.R.id.content);
            final ViewGroup root = (contentRoot != null) ? contentRoot : (ViewGroup) activity.getWindow().getDecorView();

            if (root == null) {
                showFallbackToast(activity, message);
                return;
            }

            // Remove existing custom toast if present
            View existingView = root.findViewWithTag(TAG_CUSTOM_TOAST);
            if (existingView != null) {
                root.removeView(existingView);
            }

            int accentColor;
            switch (type) {
                case SUCCESS:
                    accentColor = Color.parseColor("#10B981"); // Glass Emerald Green
                    break;
                case ERROR:
                    accentColor = Color.parseColor("#EF4444"); // Glass Coral Red
                    break;
                case WARNING:
                    accentColor = Color.parseColor("#F59E0B"); // Glass Amber Gold
                    break;
                case INFO:
                default:
                    accentColor = Color.parseColor("#0066FF"); // CRConnect Glass Electric Blue
                    break;
            }

            // Outer Container View (Modern Dark Glass Floating Pill)
            LinearLayout container = new LinearLayout(activity);
            container.setTag(TAG_CUSTOM_TOAST);
            container.setOrientation(LinearLayout.HORIZONTAL);
            container.setGravity(Gravity.CENTER_VERTICAL);
            container.setClickable(true);
            container.setFocusable(true);

            // Shape background: Deep Navy Slate Glass Gradient (#1E293B -> #0F172A) with accent border
            GradientDrawable glassBackground = new GradientDrawable(
                    GradientDrawable.Orientation.TL_BR,
                    new int[]{Color.parseColor("#1E293B"), Color.parseColor("#0F172A")}
            );
            glassBackground.setCornerRadius(dpToPx(activity, 22));
            glassBackground.setStroke(dpToPx(activity, 1.8f), adjustAlpha(accentColor, 0.85f));
            container.setBackground(glassBackground);

            // Padding and Elevation
            int padH = dpToPx(activity, 16);
            int padV = dpToPx(activity, 11);
            container.setPadding(padH, padV, padH, padV);
            container.setElevation(dpToPx(activity, 16));
            container.setClipToOutline(true);

            // Glassy Emoji Badge Container
            FrameLayout emojiFrame = new FrameLayout(activity);
            int emojiBgSize = dpToPx(activity, 32);
            LinearLayout.LayoutParams emojiFrameParams = new LinearLayout.LayoutParams(emojiBgSize, emojiBgSize);
            emojiFrameParams.gravity = Gravity.CENTER_VERTICAL;
            emojiFrame.setLayoutParams(emojiFrameParams);

            // Translucent Glass Circle Badge Background with tint
            GradientDrawable emojiGlassBg = new GradientDrawable();
            emojiGlassBg.setShape(GradientDrawable.OVAL);
            emojiGlassBg.setColor(adjustAlpha(accentColor, 0.22f));
            emojiGlassBg.setStroke(dpToPx(activity, 1f), adjustAlpha(accentColor, 0.6f));
            emojiFrame.setBackground(emojiGlassBg);

            TextView emojiTextView = new TextView(activity);
            emojiTextView.setText(emoji);
            emojiTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f);
            emojiTextView.setGravity(Gravity.CENTER);
            emojiFrame.addView(emojiTextView, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            ));

            container.addView(emojiFrame);

            // Text View (Crisp White for maximum dark glass contrast)
            TextView textView = new TextView(activity);
            textView.setText(message);
            textView.setTextColor(Color.WHITE);
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
            textView.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            textView.setMaxLines(3);
            textView.setEllipsize(TextUtils.TruncateAt.END);

            LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            textParams.setMargins(dpToPx(activity, 12), 0, 0, 0);
            textView.setLayoutParams(textParams);

            container.addView(textView);

            // Bottom LayoutParams (Positioned floating above bottom navigation bar - 92dp)
            FrameLayout.LayoutParams containerParams = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            containerParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
            containerParams.bottomMargin = dpToPx(activity, 92);
            int marginHoriz = dpToPx(activity, 20);
            containerParams.leftMargin = marginHoriz;
            containerParams.rightMargin = marginHoriz;

            root.addView(container, containerParams);

            // Entrance animation (Slide up smoothly from bottom)
            container.setTranslationY(dpToPx(activity, 100));
            container.setAlpha(0f);
            container.animate()
                    .translationY(0f)
                    .alpha(1f)
                    .setDuration(340)
                    .setInterpolator(new OvershootInterpolator(0.7f))
                    .start();

            // Dismiss logic
            Handler handler = new Handler(Looper.getMainLooper());
            Runnable dismissRunnable = () -> dismissToast(root, container);

            container.setOnClickListener(v -> {
                handler.removeCallbacks(dismissRunnable);
                dismissToast(root, container);
            });

            handler.postDelayed(dismissRunnable, 3200);

        } catch (Exception e) {
            showFallbackToast(activity, message);
        }
    }

    private static void dismissToast(ViewGroup root, View toastView) {
        if (root == null || toastView == null) return;
        try {
            toastView.animate()
                    .translationY(dpToPx(toastView.getContext(), 100))
                    .alpha(0f)
                    .setDuration(240)
                    .setInterpolator(new AccelerateInterpolator())
                    .withEndAction(() -> {
                        try {
                            root.removeView(toastView);
                        } catch (Exception ignored) {}
                    })
                    .start();
        } catch (Exception e) {
            try {
                root.removeView(toastView);
            } catch (Exception ignored) {}
        }
    }

    private static Activity getActivity(Context context) {
        if (context == null) return null;
        Context curr = context;
        if (curr instanceof Activity) return (Activity) curr;
        while (curr instanceof ContextWrapper) {
            Context base = ((ContextWrapper) curr).getBaseContext();
            if (base instanceof Activity) {
                return (Activity) base;
            }
            if (curr == base) break;
            curr = base;
        }
        return null;
    }

    private static ToastType detectType(String message) {
        if (message == null) return ToastType.INFO;
        String lower = message.toLowerCase();
        if (lower.contains("failed") || lower.contains("error") || lower.contains("cannot") ||
                lower.contains("invalid") || lower.contains("denied") || lower.contains("missing") ||
                lower.contains("wrong") || lower.contains("problem")) {
            return ToastType.ERROR;
        }
        if (lower.contains("success") || lower.contains("updated") || lower.contains("saved") ||
                lower.contains("deleted") || lower.contains("reset") || lower.contains("assigned") ||
                lower.contains("published") || lower.contains("resent") || lower.contains("added") ||
                lower.contains("locked") || lower.contains("connected")) {
            return ToastType.SUCCESS;
        }
        if (lower.contains("warning") || lower.contains("closed") || lower.contains("ended") ||
                lower.contains("required") || lower.contains("no candidates") || lower.contains("not found") ||
                lower.contains("please try again") || lower.contains("permission")) {
            return ToastType.WARNING;
        }
        return ToastType.INFO;
    }

    private static String getDefaultEmoji(ToastType type) {
        switch (type) {
            case SUCCESS:
                return "✨";
            case ERROR:
                return "🚨";
            case WARNING:
                return "⚠️";
            case INFO:
            default:
                return "💡";
        }
    }

    private static int adjustAlpha(int color, float factor) {
        int alpha = Math.round(Color.alpha(color) * factor);
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        return Color.argb(alpha, red, green, blue);
    }

    private static void showFallbackToast(Context context, String message) {
        try {
            Toast.makeText(context.getApplicationContext(), message, Toast.LENGTH_SHORT).show();
        } catch (Exception ignored) {}
    }

    private static int dpToPx(Context context, float dp) {
        return Math.round(dp * context.getResources().getDisplayMetrics().density);
    }
}
