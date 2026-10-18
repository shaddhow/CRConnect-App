package com.example.crconnect.core.utils;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;

import com.google.android.material.bottomsheet.BottomSheetDialog;

/**
 * Utility helper for smooth soft-keyboard handling, window insets,
 * and automatic focus auto-scrolling across screens and dialogs.
 */
public class KeyboardUtils {

    private KeyboardUtils() {
        // Private constructor for utility class
    }

    /**
     * Configures a NestedScrollView to handle system bars and IME (soft keyboard) insets smoothly.
     * Also automatically attaches focus auto-scrolling to all input fields inside the scroll view.
     *
     * @param activity   The parent activity.
     * @param scrollView The scroll view containing the form or input fields.
     */
    public static void setupSmoothKeyboardHandling(@NonNull Activity activity, @NonNull NestedScrollView scrollView) {
        setupSmoothKeyboardHandling(activity, scrollView, (View[]) null);
    }

    /**
     * Configures a NestedScrollView and optional extra views (such as sticky footers or bottom navs)
     * to smoothly adjust padding when the soft keyboard appears or disappears.
     *
     * @param activity        The parent activity.
     * @param scrollView      The primary scroll view.
     * @param additionalViews Optional extra views whose bottom padding should adjust with IME insets.
     */
    public static void setupSmoothKeyboardHandling(@NonNull Activity activity,
                                                   @NonNull NestedScrollView scrollView,
                                                   View... additionalViews) {
        Window window = activity.getWindow();
        if (window != null) {
            WindowCompat.setDecorFitsSystemWindows(window, false);
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }

        final int initialPaddingBottom = scrollView.getPaddingBottom();
        final int initialPaddingTop = scrollView.getPaddingTop();
        final int initialPaddingLeft = scrollView.getPaddingLeft();
        final int initialPaddingRight = scrollView.getPaddingRight();

        ViewCompat.setOnApplyWindowInsetsListener(scrollView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());

            int bottomPadding = Math.max(systemBars.bottom, ime.bottom) + initialPaddingBottom;
            v.setPadding(initialPaddingLeft, systemBars.top + initialPaddingTop, initialPaddingRight, bottomPadding);

            if (ime.bottom > 0) {
                View currentFocus = activity.getCurrentFocus();
                if (currentFocus instanceof EditText) {
                    smoothScrollToFocusedView(scrollView, currentFocus);
                }
            }
            return WindowInsetsCompat.CONSUMED;
        });

        if (additionalViews != null) {
            for (View extraView : additionalViews) {
                if (extraView == null) continue;
                final int extraInitialBottom = extraView.getPaddingBottom();
                ViewCompat.setOnApplyWindowInsetsListener(extraView, (v, insets) -> {
                    Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
                    int bottomPadding = Math.max(systemBars.bottom, ime.bottom) + extraInitialBottom;
                    v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), bottomPadding);
                    return WindowInsetsCompat.CONSUMED;
                });
            }
        }

        attachAutoScrollOnFocus(scrollView, scrollView);
    }

    /**
     * Recursively attaches focus listeners to all EditTexts within a ViewGroup hierarchy
     * so that gaining focus triggers a smooth auto-scroll to keep the input field centered above the soft keyboard.
     *
     * @param scrollView The parent NestedScrollView.
     * @param root       The container to traverse.
     */
    public static void attachAutoScrollOnFocus(@NonNull NestedScrollView scrollView, @NonNull ViewGroup root) {
        for (int i = 0; i < root.getChildCount(); i++) {
            View child = root.getChildAt(i);
            if (child instanceof EditText) {
                child.setOnFocusChangeListener((v, hasFocus) -> {
                    if (hasFocus) {
                        scrollView.postDelayed(() -> smoothScrollToFocusedView(scrollView, v), 150);
                    }
                });
            } else if (child instanceof ViewGroup) {
                attachAutoScrollOnFocus(scrollView, (ViewGroup) child);
            }
        }
    }

    /**
     * Smoothly scrolls the NestedScrollView to bring the target focused view comfortably into view.
     *
     * @param scrollView  The parent NestedScrollView.
     * @param focusedView The view currently focused.
     */
    public static void smoothScrollToFocusedView(@NonNull NestedScrollView scrollView, @NonNull View focusedView) {
        scrollView.post(() -> {
            Rect rect = new Rect();
            focusedView.getDrawingRect(rect);
            scrollView.offsetDescendantRectToMyCoords(focusedView, rect);

            int offset = (int) (100 * scrollView.getResources().getDisplayMetrics().density);
            int targetY = Math.max(0, rect.top - offset);
            scrollView.smoothScrollTo(0, targetY);
        });
    }

    /**
     * Configures a custom Dialog to adjust softly when keyboard is opened,
     * ensuring input fields inside dialogs pop up smoothly above the keyboard.
     *
     * @param dialog        The custom Dialog instance.
     * @param containerView The root layout of the dialog content.
     */
    public static void setupDialogKeyboardHandling(@NonNull Dialog dialog, @NonNull View containerView) {
        Window window = dialog.getWindow();
        if (window != null) {
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }

        final int initialBottom = containerView.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(containerView, (v, insets) -> {
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), initialBottom + ime.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        if (containerView instanceof NestedScrollView) {
            attachAutoScrollOnFocus((NestedScrollView) containerView, (ViewGroup) containerView);
        } else if (containerView instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) containerView).getChildCount(); i++) {
                View child = ((ViewGroup) containerView).getChildAt(i);
                if (child instanceof NestedScrollView) {
                    attachAutoScrollOnFocus((NestedScrollView) child, (ViewGroup) child);
                }
            }
        }
    }

    /**
     * Configures a BottomSheetDialog window to adjust smoothly with the soft keyboard.
     *
     * @param dialog        The BottomSheetDialog instance.
     * @param containerView The sheet content view.
     */
    public static void setupBottomSheetKeyboardHandling(@NonNull BottomSheetDialog dialog, @NonNull View containerView) {
        Window window = dialog.getWindow();
        if (window != null) {
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }

        final int initialBottom = containerView.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(containerView, (v, insets) -> {
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), initialBottom + ime.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }
}
