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
import android.widget.FrameLayout;
import android.widget.Toast;

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

public class PdfReadyBottomSheet extends BottomSheetDialogFragment {

    public static final String TAG = "PdfReadyBottomSheet";
    private static final String ARG_FILE_PATH = "arg_file_path";

    private File pdfFile;

    public static PdfReadyBottomSheet newInstance(@NonNull File pdfFile) {
        PdfReadyBottomSheet fragment = new PdfReadyBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_FILE_PATH, pdfFile.getAbsolutePath());
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
            FrameLayout bottomSheet = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                bottomSheet.setBackground(new ColorDrawable(Color.TRANSPARENT));
                BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
            }
        });

        Window window = dialog.getWindow();
        if (window != null) {
            window.setDimAmount(0.4f); // Scrim rgba(15,23,42,.4)
            window.setWindowAnimations(com.google.android.material.R.style.Animation_Design_BottomSheetDialog);
        }

        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_pdf_ready, container, false);
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
                } else if (context != null) {
                    Toast.makeText(context, "PDF file is not available", Toast.LENGTH_SHORT).show();
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
                } else if (context != null) {
                    Toast.makeText(context, "PDF file is not available", Toast.LENGTH_SHORT).show();
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
                } else if (context != null) {
                    Toast.makeText(context, "PDF file is not available", Toast.LENGTH_SHORT).show();
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
            Log.e(TAG, "Error showing PdfReadyBottomSheet: " + e.getMessage(), e);
        }
    }
}
