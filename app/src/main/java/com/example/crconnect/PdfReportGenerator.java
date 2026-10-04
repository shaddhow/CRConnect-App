package com.example.crconnect;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.print.PrintManager;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PdfReportGenerator {

    private static final String TAG = "PdfReportGenerator";

    public static class CandidateResult {
        public String name;
        public String dept;
        public int votes;
        public int rank;

        public CandidateResult(String name, String dept, int votes, int rank) {
            this.name = name;
            this.dept = dept;
            this.votes = votes;
            this.rank = rank;
        }
    }

    /**
     * Generates a clean, professionally formatted PDF file on disk.
     */
    public static File generatePdfDocument(
            Context context,
            String teacherName,
            String teacherDept,
            int totalVotes,
            int totalVoters,
            String turnout,
            boolean isGateOpen,
            List<CandidateResult> candidateList
    ) {
        PdfDocument pdfDocument = new PdfDocument();
        int pageWidth = 595;  // Standard A4 width in points (72 dpi)
        int pageHeight = 842; // Standard A4 height in points (72 dpi)

        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
        PdfDocument.Page page = pdfDocument.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        Paint paint = new Paint();
        paint.setAntiAlias(true);

        // --- BACKGROUND ---
        canvas.drawColor(Color.WHITE);

        // --- TOP ACCENT BARS ---
        paint.setColor(Color.parseColor("#0F172A")); // Deep Slate Navy
        canvas.drawRect(0, 0, pageWidth, 16, paint);

        paint.setColor(Color.parseColor("#0284C7")); // Neon Cyan Accent
        canvas.drawRect(0, 16, pageWidth, 21, paint);

        int startX = 36;
        int maxRightX = pageWidth - startX; // 559 pt
        int currentY = 48;

        // --- 1. INSTITUTION HEADER ---
        paint.setColor(Color.parseColor("#0F172A"));
        paint.setTextSize(14f);
        paint.setFakeBoldText(true);
        canvas.drawText("BANGLADESH UNIVERSITY OF BUSINESS AND TECHNOLOGY", startX, currentY, paint);

        currentY += 16;
        paint.setColor(Color.parseColor("#0284C7"));
        paint.setTextSize(10f);
        paint.setFakeBoldText(true);
        canvas.drawText("BUBT Department Network • CRConnect System", startX, currentY, paint);

        currentY += 14;
        paint.setColor(Color.parseColor("#CBD5E1"));
        paint.setStrokeWidth(1.2f);
        canvas.drawLine(startX, currentY, maxRightX, currentY, paint);

        // --- 2. REPORT TITLE ---
        currentY += 24;
        paint.setColor(Color.parseColor("#0F172A"));
        paint.setTextSize(15f);
        paint.setFakeBoldText(true);
        canvas.drawText("OFFICIAL CLASS REPRESENTATIVE ELECTION REPORT", startX, currentY, paint);

        // --- 3. METADATA BLOCK ---
        currentY += 16;
        paint.setColor(Color.parseColor("#64748B"));
        paint.setTextSize(9f);
        paint.setFakeBoldText(false);

        String timeStamp = new SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.getDefault()).format(new Date());
        canvas.drawText("Generated On: " + timeStamp, startX, currentY, paint);

        currentY += 13;
        canvas.drawText("Certified By: " + teacherName + " (" + teacherDept + ")", startX, currentY, paint);

        currentY += 13;
        String gateText = isGateOpen ? "Live Voting Gate: OPEN 🟢" : "Live Voting Gate: LOCKED 🔴";
        paint.setColor(Color.parseColor(isGateOpen ? "#15803D" : "#B91C1C"));
        paint.setFakeBoldText(true);
        canvas.drawText("Election Status: " + gateText, startX, currentY, paint);

        // --- 4. EXECUTIVE SUMMARY CARD ---
        currentY += 18;
        int summaryBoxHeight = 68;
        RectF summaryRect = new RectF(startX, currentY, maxRightX, currentY + summaryBoxHeight);

        paint.setColor(Color.parseColor("#F8FAFC"));
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(summaryRect, 8, 8, paint);

        paint.setColor(Color.parseColor("#E2E8F0"));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1f);
        canvas.drawRoundRect(summaryRect, 8, 8, paint);

        paint.setStyle(Paint.Style.FILL);
        int summaryY = currentY + 22;
        int totalUsableWidth = maxRightX - startX;
        int colWidth = totalUsableWidth / 4;

        // Metric 1: Total Candidates
        drawMetric(canvas, paint, "CANDIDATES", String.valueOf(candidateList.size()), startX + 12, summaryY, Color.parseColor("#0F172A"));

        // Metric 2: Total Votes Cast
        drawMetric(canvas, paint, "VOTES CAST", String.valueOf(totalVotes), startX + colWidth + 8, summaryY, Color.parseColor("#0284C7"));

        // Metric 3: Total Registered Voters
        drawMetric(canvas, paint, "RECORDED VOTERS", String.valueOf(totalVoters), startX + (colWidth * 2) + 4, summaryY, Color.parseColor("#10B981"));

        // Metric 4: Turnout Rate
        drawMetric(canvas, paint, "TURNOUT RATE", turnout, startX + (colWidth * 3), summaryY, Color.parseColor("#D97706"));

        currentY += summaryBoxHeight + 24;

        // --- 5. CANDIDATES RESULTS TABLE ---
        paint.setColor(Color.parseColor("#0F172A"));
        paint.setTextSize(12f);
        paint.setFakeBoldText(true);
        canvas.drawText("OFFICIAL CANDIDATE RANKINGS & VOTE DISTRIBUTION", startX, currentY, paint);

        currentY += 12;

        int tableHeaderHeight = 24;
        RectF headerRect = new RectF(startX, currentY, maxRightX, currentY + tableHeaderHeight);
        paint.setColor(Color.parseColor("#1E293B"));
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRect(headerRect, paint);

        // Table Header Columns (X offsets defined with clean non-overlapping margins)
        int colRankX = startX + 10;      // X: 46  (Width ~45pt)
        int colStatusX = startX + 55;    // X: 91  (Width ~65pt)
        int colNameX = startX + 125;     // X: 161 (Width ~155pt)
        int colDeptX = startX + 285;     // X: 321 (Width ~115pt)
        int colVotesX = startX + 410;    // X: 446 (Width ~50pt)
        int colShareX = startX + 468;    // X: 504 (Width ~50pt)

        paint.setColor(Color.WHITE);
        paint.setTextSize(9.5f);
        paint.setFakeBoldText(true);

        canvas.drawText("RANK", colRankX, currentY + 16, paint);
        canvas.drawText("STATUS", colStatusX, currentY + 16, paint);
        canvas.drawText("CANDIDATE NAME", colNameX, currentY + 16, paint);
        canvas.drawText("DEPARTMENT", colDeptX, currentY + 16, paint);
        canvas.drawText("VOTES", colVotesX, currentY + 16, paint);
        canvas.drawText("SHARE %", colShareX, currentY + 16, paint);

        currentY += tableHeaderHeight;

        // --- DATA ROWS ---
        int rowHeight = 28;
        int maxAllowedY = pageHeight - 75; // Stop well before footer
        int index = 0;

        for (CandidateResult c : candidateList) {
            if (currentY + rowHeight > maxAllowedY) {
                break; // Keep report neatly on 1 single page without footer collision
            }

            boolean isWinner = (c.rank == 1 && c.votes > 0);

            // Row Background
            RectF rowRect = new RectF(startX, currentY, maxRightX, currentY + rowHeight);
            if (isWinner) {
                paint.setColor(Color.parseColor("#FEF3C7")); // Soft Gold Highlight
            } else if (index % 2 == 0) {
                paint.setColor(Color.parseColor("#F8FAFC")); // Subtle Alternate Light Fill
            } else {
                paint.setColor(Color.WHITE);
            }
            paint.setStyle(Paint.Style.FILL);
            canvas.drawRect(rowRect, paint);

            // Row Border
            paint.setColor(Color.parseColor("#E2E8F0"));
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(0.8f);
            canvas.drawRect(rowRect, paint);

            // Row Text Fill
            paint.setStyle(Paint.Style.FILL);

            // Column 1: Rank Text
            paint.setColor(isWinner ? Color.parseColor("#92400E") : Color.parseColor("#334155"));
            paint.setFakeBoldText(true);
            paint.setTextSize(9.5f);
            canvas.drawText("#" + c.rank, colRankX, currentY + 18, paint);

            // Column 2: Status Tag / Badge
            if (isWinner) {
                // Draw Gold Badge Box
                RectF badgeRect = new RectF(colStatusX, currentY + 6, colStatusX + 58, currentY + 22);
                paint.setColor(Color.parseColor("#D97706"));
                paint.setStyle(Paint.Style.FILL);
                canvas.drawRoundRect(badgeRect, 4, 4, paint);

                paint.setColor(Color.WHITE);
                paint.setTextSize(8f);
                paint.setFakeBoldText(true);
                canvas.drawText("WINNER 🏆", colStatusX + 5, currentY + 17, paint);
            } else {
                paint.setColor(Color.parseColor("#64748B"));
                paint.setTextSize(9f);
                paint.setFakeBoldText(false);
                canvas.drawText("RUNNER-UP", colStatusX, currentY + 18, paint);
            }

            // Column 3: Candidate Name (Truncate cleanly if name is very long to prevent collision)
            paint.setColor(isWinner ? Color.parseColor("#92400E") : Color.parseColor("#0F172A"));
            paint.setTextSize(9.5f);
            paint.setFakeBoldText(isWinner);
            String truncatedName = truncateText(c.name, paint, 150);
            canvas.drawText(truncatedName, colNameX, currentY + 18, paint);

            // Column 4: Department (Truncate if needed)
            paint.setColor(Color.parseColor("#475569"));
            paint.setFakeBoldText(false);
            paint.setTextSize(9f);
            String truncatedDept = truncateText(c.dept, paint, 115);
            canvas.drawText(truncatedDept, colDeptX, currentY + 18, paint);

            // Column 5: Votes
            paint.setColor(isWinner ? Color.parseColor("#92400E") : Color.parseColor("#0F172A"));
            paint.setFakeBoldText(true);
            paint.setTextSize(9.5f);
            canvas.drawText(String.valueOf(c.votes), colVotesX, currentY + 18, paint);

            // Column 6: Share %
            double share = totalVotes > 0 ? ((double) c.votes * 100.0 / totalVotes) : 0;
            String shareStr = String.format(Locale.getDefault(), "%.1f%%", share);
            paint.setColor(Color.parseColor("#0284C7"));
            canvas.drawText(shareStr, colShareX, currentY + 18, paint);

            currentY += rowHeight;
            index++;
        }

        // --- 6. FOOTER SECTION ---
        int footerY = pageHeight - 42;
        paint.setColor(Color.parseColor("#CBD5E1"));
        paint.setStrokeWidth(1f);
        canvas.drawLine(startX, footerY, maxRightX, footerY, paint);

        footerY += 16;
        paint.setColor(Color.parseColor("#94A3B8"));
        paint.setTextSize(8.5f);
        paint.setFakeBoldText(false);
        canvas.drawText("Certified electronically via CRConnect BUBT Network • Secure Key: BUBT-CR-VERIFIED", startX, footerY, paint);

        paint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText("Page 1 of 1", maxRightX, footerY, paint);
        paint.setTextAlign(Paint.Align.LEFT); // Reset alignment

        pdfDocument.finishPage(page);

        // --- SAVE TO DISK ---
        File pdfDir = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "CRConnect_Reports");
        if (!pdfDir.exists()) {
            boolean created = pdfDir.mkdirs();
            Log.d(TAG, "Directory created: " + created);
        }

        String fileName = "BUBT_CR_Election_Results_" + System.currentTimeMillis() + ".pdf";
        File pdfFile = new File(pdfDir, fileName);

        try {
            FileOutputStream fos = new FileOutputStream(pdfFile);
            pdfDocument.writeTo(fos);
            pdfDocument.close();
            fos.close();
            return pdfFile;
        } catch (IOException e) {
            Log.e(TAG, "Error writing PDF document: " + e.getMessage(), e);
            pdfDocument.close();
            return null;
        }
    }

    /**
     * Native Android Print Spooler Integration.
     * Opens system print UI / native PDF saver directly without 3rd party overlay issues.
     */
    public static void printPdfDocument(Context context, File pdfFile) {
        PrintManager printManager = (PrintManager) context.getSystemService(Context.PRINT_SERVICE);
        if (printManager == null || pdfFile == null || !pdfFile.exists()) {
            Toast.makeText(context, "Printer service not available", Toast.LENGTH_SHORT).show();
            return;
        }

        String jobName = "BUBT_CR_Election_Results_" + System.currentTimeMillis();

        PrintDocumentAdapter printAdapter = new PrintDocumentAdapter() {
            @Override
            public void onLayout(PrintAttributes oldAttributes, PrintAttributes newAttributes,
                                 CancellationSignal cancellationSignal,
                                 LayoutResultCallback callback, Bundle extras) {
                if (cancellationSignal.isCanceled()) {
                    callback.onLayoutCancelled();
                    return;
                }
                PrintDocumentInfo info = new PrintDocumentInfo.Builder(jobName + ".pdf")
                        .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                        .setPageCount(1)
                        .build();
                callback.onLayoutFinished(info, true);
            }

            @Override
            public void onWrite(PageRange[] pages, ParcelFileDescriptor destination,
                                CancellationSignal cancellationSignal,
                                WriteResultCallback callback) {
                InputStream input = null;
                OutputStream output = null;
                try {
                    input = new FileInputStream(pdfFile);
                    output = new FileOutputStream(destination.getFileDescriptor());

                    byte[] buf = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = input.read(buf)) > 0) {
                        if (cancellationSignal.isCanceled()) {
                            callback.onWriteCancelled();
                            return;
                        }
                        output.write(buf, 0, bytesRead);
                    }
                    callback.onWriteFinished(new PageRange[]{PageRange.ALL_PAGES});
                } catch (Exception e) {
                    Log.e(TAG, "Error writing to print spooler: " + e.getMessage(), e);
                    callback.onWriteFailed(e.getMessage());
                } finally {
                    try {
                        if (input != null) input.close();
                        if (output != null) output.close();
                    } catch (IOException ignored) {}
                }
            }
        };

        PrintAttributes printAttributes = new PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                .setResolution(new PrintAttributes.Resolution("pdf", "pdf", 300, 300))
                .setMinMargins(new PrintAttributes.Margins(0, 0, 0, 0))
                .build();

        printManager.print(jobName, printAdapter, printAttributes);
    }

    /**
     * Share PDF file using Android FileProvider & Intent chooser.
     */
    public static void sharePdfFile(Context context, File pdfFile) {
        if (pdfFile == null || !pdfFile.exists()) {
            Toast.makeText(context, "PDF File not found", Toast.LENGTH_SHORT).show();
            return;
        }

        Uri fileUri = FileProvider.getUriForFile(
                context,
                "com.example.crconnect.fileprovider",
                pdfFile
        );

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("application/pdf");
        shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "BUBT CR Election Results Report");
        shareIntent.putExtra(Intent.EXTRA_TEXT, "Attached is the official Class Representative Election Results Report generated from CRConnect.");
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        Intent chooser = Intent.createChooser(shareIntent, "Share or Save Election PDF Report");
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(chooser);
    }

    private static void drawMetric(Canvas canvas, Paint paint, String label, String value, float x, float y, int valueColor) {
        paint.setColor(Color.parseColor("#64748B"));
        paint.setTextSize(8f);
        paint.setFakeBoldText(true);
        canvas.drawText(label, x, y, paint);

        paint.setColor(valueColor);
        paint.setTextSize(15f);
        paint.setFakeBoldText(true);
        canvas.drawText(value, x, y + 18, paint);
    }

    private static String truncateText(String text, Paint paint, float maxWidth) {
        if (text == null) return "";
        if (paint.measureText(text) <= maxWidth) {
            return text;
        }
        String ellipsis = "...";
        float ellipsisWidth = paint.measureText(ellipsis);
        int length = text.length();

        while (length > 0) {
            String sub = text.substring(0, length);
            if (paint.measureText(sub) + ellipsisWidth <= maxWidth) {
                return sub + ellipsis;
            }
            length--;
        }
        return ellipsis;
    }
}
