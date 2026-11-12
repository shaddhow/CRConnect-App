package com.example.crconnect;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class ManageSectionsBottomSheet extends BottomSheetDialogFragment {

    public static final String TAG = "ManageSectionsBottomSheet";

    public interface OnSaveListener {
        void onSave(String sections, String intakes);
    }

    private OnSaveListener listener;
    private Runnable onReleaseListener;
    private String initialSections = "";
    private String initialIntakes = "";

    public static ManageSectionsBottomSheet newInstance(String sections, String intakes, OnSaveListener listener) {
        return newInstance(sections, intakes, listener, null);
    }

    public static ManageSectionsBottomSheet newInstance(String sections, String intakes, OnSaveListener listener, Runnable onReleaseListener) {
        ManageSectionsBottomSheet fragment = new ManageSectionsBottomSheet();
        fragment.initialSections = sections != null ? sections : "";
        fragment.initialIntakes = intakes != null ? intakes : "";
        fragment.listener = listener;
        fragment.onReleaseListener = onReleaseListener;
        return fragment;
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
        return inflater.inflate(R.layout.bottom_sheet_manage_sections, container, false);
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

        TextInputEditText etSections = view.findViewById(R.id.etAssignedSections);
        TextInputEditText etIntakes = view.findViewById(R.id.etAssignedIntakes);
        MaterialButton btnSave = view.findViewById(R.id.btnSaveTeacherSections);
        MaterialButton btnCancel = view.findViewById(R.id.btnCancelTeacherSections);

        if (etSections != null) etSections.setText(initialSections);
        if (etIntakes != null) etIntakes.setText(initialIntakes);

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dismissAllowingStateLoss());
        }

        MaterialButton btnRelease = view.findViewById(R.id.btnReleaseActiveSectionSheet);
        if (btnRelease != null) {
            btnRelease.setOnClickListener(v -> {
                dismissAllowingStateLoss();
                if (onReleaseListener != null) {
                    onReleaseListener.run();
                }
            });
        }

        if (btnSave != null) {
            btnSave.setOnClickListener(v -> {
                String sec = etSections != null && etSections.getText() != null ? etSections.getText().toString().trim() : "";
                String intake = etIntakes != null && etIntakes.getText() != null ? etIntakes.getText().toString().trim() : "";
                if (listener != null) {
                    listener.onSave(sec, intake);
                }
                dismissAllowingStateLoss();
            });
        }
    }
}
