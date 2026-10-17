package com.example.crconnect;

import android.os.Bundle;

import androidx.annotation.NonNull;

import java.io.File;

public class PdfReadyBottomSheet extends ExportPdfBottomSheet {

    public static final String TAG = "PdfReadyBottomSheet";

    public static PdfReadyBottomSheet newInstance(@NonNull File pdfFile) {
        PdfReadyBottomSheet fragment = new PdfReadyBottomSheet();
        Bundle args = new Bundle();
        args.putString("arg_file_path", pdfFile.getAbsolutePath());
        fragment.setArguments(args);
        return fragment;
    }
}
