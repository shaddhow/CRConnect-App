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

public class EndVotingBottomSheet extends BottomSheetDialogFragment {

    public static final String TAG = "EndVotingBottomSheet";

    private Runnable onConfirmRunnable;

    public static EndVotingBottomSheet newInstance(@Nullable Runnable onConfirm) {
        EndVotingBottomSheet fragment = new EndVotingBottomSheet();
        fragment.setOnConfirmRunnable(onConfirm);
        return fragment;
    }

    public void setOnConfirmRunnable(@Nullable Runnable onConfirm) {
        this.onConfirmRunnable = onConfirm;
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
        return inflater.inflate(R.layout.bottom_sheet_end_voting, container, false);
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

        MaterialButton btnCancel = view.findViewById(R.id.btnCancelEndVoting);
        MaterialButton btnConfirm = view.findViewById(R.id.btnConfirmEndVoting);

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dismiss());
        }

        if (btnConfirm != null) {
            btnConfirm.setOnClickListener(v -> {
                dismiss();
                if (onConfirmRunnable != null) {
                    onConfirmRunnable.run();
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
            Log.e(TAG, "Error showing EndVotingBottomSheet: " + e.getMessage(), e);
        }
    }
}
