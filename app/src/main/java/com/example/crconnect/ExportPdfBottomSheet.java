package com.example.crconnect;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;

import java.io.File;

public class ExportPdfBottomSheet extends BottomSheetDialogFragment {

    public static final String TAG = "ExportPdfBottomSheet";
    private static final String ARG_FILE_PATH = "arg_file_path";

    private File pdfFile;

    public static ExportPdfBottomSheet newInstance(@NonNull File pdfFile) {
        ExportPdfBottomSheet fragment = new ExportPdfBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_FILE_PATH, pdfFile.getAbsolutePath());
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NORMAL, R.style.CustomBottomSheetDialogTheme);
        if (getArguments() != null) {
            String path = getArguments().getString(ARG_FILE_PATH);
            if (path != null) {
                pdfFile = new File(path);
            }
        }
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
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setDimAmount(0.45f);
            window.setWindowAnimations(com.google.android.material.R.style.Animation_Design_BottomSheetDialog);
        }

        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_export_pdf, container, false);
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

        MaterialButton btnPrintPdf = view.findViewById(R.id.btnPrintPdf);
        MaterialButton btnSharePdf = view.findViewById(R.id.btnSharePdf);
        MaterialButton btnOpenPdf = view.findViewById(R.id.btnOpenPdf);

        if (btnPrintPdf != null) {
            btnPrintPdf.setOnClickListener(v -> {
                Log.d(TAG, "Print / Save PDF option selected");
                Context context = getContext();
                if (context != null && pdfFile != null && pdfFile.exists()) {
                    PdfReportGenerator.printPdfDocument(context, pdfFile);
                }
                dismiss();
            });
        }

        if (btnSharePdf != null) {
            btnSharePdf.setOnClickListener(v -> {
                Log.d(TAG, "Share PDF option selected");
                Context context = getContext();
                if (context != null && pdfFile != null && pdfFile.exists()) {
                    PdfReportGenerator.sharePdfFile(context, pdfFile);
                }
                dismiss();
            });
        }

        if (btnOpenPdf != null) {
            btnOpenPdf.setOnClickListener(v -> {
                Log.d(TAG, "Open PDF option selected");
                Context context = getContext();
                if (context != null && pdfFile != null && pdfFile.exists()) {
                    PdfReportGenerator.openPdfFile(context, pdfFile);
                }
                dismiss();
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
            Log.e(TAG, "Error showing ExportPdfBottomSheet: " + e.getMessage(), e);
        }
    }
}
