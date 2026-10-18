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
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;

public class TeacherProfileBottomSheet extends BottomSheetDialogFragment {

    public static final String TAG = "TeacherProfileBottomSheet";

    private Runnable onEditProfileClick;
    private Runnable onSecurityClick;
    private Runnable onLogoutClick;

    public static TeacherProfileBottomSheet newInstance(@Nullable Runnable onEditProfile, @Nullable Runnable onSecurity, @Nullable Runnable onLogout) {
        TeacherProfileBottomSheet fragment = new TeacherProfileBottomSheet();
        fragment.onEditProfileClick = onEditProfile;
        fragment.onSecurityClick = onSecurity;
        fragment.onLogoutClick = onLogout;
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
        return inflater.inflate(R.layout.bottom_sheet_teacher_profile, container, false);
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

        SessionManager sessionManager = new SessionManager(requireContext());

        TextView tvAvatarText = view.findViewById(R.id.tvProfileAvatarText);
        ImageView imgAvatar = view.findViewById(R.id.imgProfileAvatar);
        TextView tvName = view.findViewById(R.id.tvProfileName);
        TextView tvEmail = view.findViewById(R.id.tvProfileEmail);
        TextView tvDept = view.findViewById(R.id.tvProfileDept);

        TextView tvTeacherCode = view.findViewById(R.id.tvProfileTeacherCode);
        TextView tvRole = view.findViewById(R.id.tvProfileRole);
        TextView tvDepartment = view.findViewById(R.id.tvProfileDepartment);
        TextView tvAssignedSection = view.findViewById(R.id.tvProfileAssignedSection);
        TextView tvAccountStatus = view.findViewById(R.id.tvProfileAccountStatus);

        MaterialButton btnEditProfile = view.findViewById(R.id.btnEditProfile);
        MaterialButton btnSecurity = view.findViewById(R.id.btnSecuritySettings);
        MaterialButton btnLogout = view.findViewById(R.id.btnProfileLogout);

        String name = sessionManager.getUserName();
        String email = sessionManager.getUserEmail();
        String id = sessionManager.getUserID();
        String role = sessionManager.getUserRole();
        String dept = sessionManager.getUserDept();
        String assignedSec = sessionManager.getAssignedSection();
        String profileImg = sessionManager.getProfileImageUrl();

        if (tvName != null) tvName.setText(name);
        if (tvEmail != null) tvEmail.setText(email);
        if (tvTeacherCode != null) tvTeacherCode.setText(!id.isEmpty() ? id : "T-0142");
        if (tvRole != null) tvRole.setText(role);
        if (tvDepartment != null) tvDepartment.setText(dept);
        if (tvAssignedSection != null) tvAssignedSection.setText("Section " + assignedSec);
        if (tvDept != null) tvDept.setText("Verified teacher · Department of " + dept);

        if (tvAvatarText != null) {
            String initials = "TT";
            if (name != null && !name.trim().isEmpty()) {
                String[] parts = name.trim().split("\\s+");
                if (parts.length >= 2) {
                    initials = ("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase();
                } else if (parts.length == 1 && parts[0].length() >= 2) {
                    initials = parts[0].substring(0, 2).toUpperCase();
                }
            }
            tvAvatarText.setText(initials);
        }

        if (profileImg != null && !profileImg.isEmpty() && imgAvatar != null && tvAvatarText != null) {
            imgAvatar.setVisibility(View.VISIBLE);
            tvAvatarText.setVisibility(View.GONE);
            Glide.with(this).load(profileImg).placeholder(R.drawable.ic_app_main).error(R.drawable.ic_app_main).circleCrop().into(imgAvatar);
        }

        if (btnEditProfile != null) {
            btnEditProfile.setOnClickListener(v -> {
                dismiss();
                if (onEditProfileClick != null) onEditProfileClick.run();
            });
        }

        if (btnSecurity != null) {
            btnSecurity.setOnClickListener(v -> {
                dismiss();
                if (onSecurityClick != null) onSecurityClick.run();
            });
        }

        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                dismiss();
                if (onLogoutClick != null) onLogoutClick.run();
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
            Log.e(TAG, "Error showing TeacherProfileBottomSheet: " + e.getMessage(), e);
        }
    }
}
