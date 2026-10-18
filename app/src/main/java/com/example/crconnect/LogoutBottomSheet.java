package com.example.crconnect;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;

public class LogoutBottomSheet extends BottomSheetDialogFragment {

    public static final String TAG = "LogoutBottomSheet";
    private static final String ARG_TITLE = "arg_title";
    private static final String ARG_MSG = "arg_msg";
    private static final String ARG_CONFIRM = "arg_confirm";

    private Runnable onLogoutConfirm;

    public static LogoutBottomSheet newInstance(@Nullable String title, @Nullable String message, @Nullable String confirmText, @Nullable Runnable onConfirm) {
        LogoutBottomSheet fragment = new LogoutBottomSheet();
        Bundle args = new Bundle();
        if (title != null) args.putString(ARG_TITLE, title);
        if (message != null) args.putString(ARG_MSG, message);
        if (confirmText != null) args.putString(ARG_CONFIRM, confirmText);
        fragment.setArguments(args);
        fragment.setOnLogoutConfirm(onConfirm);
        return fragment;
    }

    public static LogoutBottomSheet newInstance(@Nullable Runnable onConfirm) {
        return newInstance("Confirm logout", "Are you sure you want to log out of Admin Console?", "Logout", onConfirm);
    }

    public void setOnLogoutConfirm(@Nullable Runnable onConfirm) {
        this.onLogoutConfirm = onConfirm;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NORMAL, R.style.CustomBottomSheetDialogTheme);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        dialog.setOnShowListener(dialogInterface -> {
            BottomSheetDialog d = (BottomSheetDialog) dialogInterface;
            View bottomSheet = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                bottomSheet.setBackgroundResource(R.drawable.bg_bottom_sheet);
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
            }
        });

        Window window = dialog.getWindow();
        if (window != null) {
            WindowCompat.setDecorFitsSystemWindows(window, false);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setStatusBarColor(Color.TRANSPARENT);
            window.setNavigationBarColor(Color.TRANSPARENT);
            window.setDimAmount(0.45f);
            window.setWindowAnimations(com.google.android.material.R.style.Animation_Design_BottomSheetDialog);
        }

        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_logout, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final int basePaddingBottom = view.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets navBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), basePaddingBottom + navBars.bottom);
            return insets;
        });

        TextView tvTitle = view.findViewById(R.id.tvLogoutTitle);
        TextView tvMsg = view.findViewById(R.id.tvLogoutMsg);
        MaterialButton btnCancel = view.findViewById(R.id.btnCancelLogout);
        MaterialButton btnConfirm = view.findViewById(R.id.btnConfirmLogout);

        if (getArguments() != null) {
            String title = getArguments().getString(ARG_TITLE);
            String msg = getArguments().getString(ARG_MSG);
            String confirmText = getArguments().getString(ARG_CONFIRM);

            if (title != null && tvTitle != null) tvTitle.setText(title);
            if (msg != null && tvMsg != null) tvMsg.setText(msg);
            if (confirmText != null && btnConfirm != null) btnConfirm.setText(confirmText);
        }

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dismiss());
        }

        if (btnConfirm != null) {
            btnConfirm.setOnClickListener(v -> {
                dismiss();
                if (onLogoutConfirm != null) {
                    onLogoutConfirm.run();
                }
            });
        }
    }

    public void showAllowingStateLoss(@NonNull FragmentManager manager, @Nullable String tag) {
        if (manager.isStateSaved() || manager.isDestroyed()) return;
        try {
            FragmentTransaction ft = manager.beginTransaction();
            ft.add(this, tag);
            ft.commitAllowingStateLoss();
        } catch (Exception e) {
            Log.e(TAG, "Error showing LogoutBottomSheet: " + e.getMessage(), e);
        }
    }
}
