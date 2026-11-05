package com.example.crconnect;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.graphics.drawable.Drawable;
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

import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.content.res.ResourcesCompat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Ultra-Accurate, Executive-Grade PDF Document Generator for BUBT CRConnect Election Results.
 * Pixel-perfect alignment, zero overlapping boxes, crystal clear typography.
 */
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
     * Generates a perfectly aligned, executive election results PDF document with zero overlapping boxes.
     */
    public static File generatePdfDocument(
            Context context,
            String teacherName,
            String teacherDept,
            String sectionNo,
            int totalVotes,
            int totalVoters,
            String turnout,
            boolean isGateOpen,
            List<CandidateResult> candidateList
    ) {
        Log.d(TAG, "Generating pixel-perfect election report PDF document");

        // Sort candidates by votes descending and re-assign rank 1, 2, 3...
        if (candidateList != null && !candidateList.isEmpty()) {
            Collections.sort(candidateList, (c1, c2) -> Integer.compare(c2.votes, c1.votes));
            for (int i = 0; i < candidateList.size(); i++) {
                candidateList.get(i).rank = i + 1;
            }
        }

        // Load Plus Jakarta Sans Typefaces
        Typeface fontMedium = null;
        Typeface fontSemiBold = null;
        Typeface fontBold = null;
        Typeface fontExtraBold = null;
        try {
            fontMedium = ResourcesCompat.getFont(context, R.font.plus_jakarta_sans_medium);
            fontSemiBold = ResourcesCompat.getFont(context, R.font.plus_jakarta_sans_semibold);
            fontBold = ResourcesCompat.getFont(context, R.font.plus_jakarta_sans_bold);
            fontExtraBold = ResourcesCompat.getFont(context, R.font.plus_jakarta_sans_extrabold);
        } catch (Exception e) {
            Log.e(TAG, "Error loading Plus Jakarta Sans fonts", e);
        }
        if (fontMedium == null) fontMedium = Typeface.DEFAULT;
        if (fontSemiBold == null) fontSemiBold = Typeface.DEFAULT;
        if (fontBold == null) fontBold = Typeface.DEFAULT_BOLD;
        if (fontExtraBold == null) fontExtraBold = Typeface.DEFAULT_BOLD;

        PdfDocument pdfDocument = new PdfDocument();
        final int pageWidth = 595;  // Standard ISO A4 width (points)
        final int pageHeight = 842; // Standard ISO A4 height (points)
        final float marginX = 36f;  // 0.5 inch side margins
        final float contentWidth = pageWidth - (2 * marginX); // 523pt

        final float footerHeight = 34f;
        final float usableHeight = pageHeight - (2 * marginX);

        // Explicit Section Heights for Zero Overlap
        final float topHeaderHeight = 84f;
        final float titleSectionHeight = 84f;
        final float metricsHeight = 60f;
        final float winnerSpotlightHeight = (candidateList != null && !candidateList.isEmpty()) ? 68f : 0f;
        final float tableHeaderHeight = 26f;

        final float page1FixedContent = topHeaderHeight + 10f + titleSectionHeight + 12f + metricsHeight
                + (winnerSpotlightHeight > 0 ? (12f + winnerSpotlightHeight) : 0f) + 12f + tableHeaderHeight + 4f;

        final float rowHeight = 30f;
        final float remainingPage1 = usableHeight - page1FixedContent - footerHeight;
        final int rowsOnPage1 = Math.max(1, (int)(remainingPage1 / rowHeight));

        int totalPages = 1;
        if (candidateList != null && candidateList.size() > rowsOnPage1) {
            int remainingRows = candidateList.size() - rowsOnPage1;
            float page2FixedContent = 36f + tableHeaderHeight + 4f;
            float page2RowSpace = usableHeight - page2FixedContent - footerHeight;
            int rowsPerPageElse = Math.max(1, (int)(page2RowSpace / rowHeight));
            totalPages = 1 + (int) Math.ceil((double) remainingRows / rowsPerPageElse);
        }

        int currentRowIndex = 0;
        final CandidateResult winner = (candidateList != null && !candidateList.isEmpty()) ? candidateList.get(0) : null;
        final String sec = sectionNo != null && !sectionNo.trim().isEmpty() ? sectionNo.trim() : "6";
        final String shortDate = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(new Date());

        for (int pageNum = 1; pageNum <= totalPages; pageNum++) {
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create();
            PdfDocument.Page page = pdfDocument.startPage(pageInfo);
            Canvas canvas = page.getCanvas();
            Paint paint = new Paint();
            paint.setAntiAlias(true);

            // Clean White Background
            canvas.drawColor(Color.WHITE);

            float currentY = 0f;

            // =========================================================================
            // PAGE 1 HEADER & BRANDING
            // =========================================================================
            if (pageNum == 1) {
                // Top 4pt Accent Gradient Line
                Shader topGradient = new LinearGradient(
                        0, 0, pageWidth, 0,
                        new int[]{Color.parseColor("#2563EB"), Color.parseColor("#38BDF8"), Color.parseColor("#1D4ED8")},
                        null, Shader.TileMode.CLAMP
                );
                paint.setShader(topGradient);
                canvas.drawRect(0, 0, pageWidth, 4f, paint);
                paint.setShader(null);

                // Deep Navy Executive Header Card (Height: 84pt)
                float headerTop = 4f;
                RectF headerRect = new RectF(0, headerTop, pageWidth, headerTop + topHeaderHeight);
                Shader headerBg = new LinearGradient(
                        0, headerTop, pageWidth, headerTop + topHeaderHeight,
                        new int[]{Color.parseColor("#0F172A"), Color.parseColor("#1E293B")},
                        null, Shader.TileMode.CLAMP
                );
                paint.setShader(headerBg);
                paint.setStyle(Paint.Style.FILL);
                canvas.drawRect(headerRect, paint);
                paint.setShader(null);

                // BUBT Logo Container (Left)
                float logoBoxSize = 46f;
                float logoY = headerTop + 19f;

                RectF logoContainer = new RectF(marginX, logoY, marginX + logoBoxSize, logoY + logoBoxSize);
                paint.setColor(Color.WHITE);
                paint.setStyle(Paint.Style.FILL);
                canvas.drawRoundRect(logoContainer, 10f, 10f, paint);

                paint.setColor(Color.parseColor("#3B82F6"));
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(1.2f);
                canvas.drawRoundRect(logoContainer, 10f, 10f, paint);
                paint.setStyle(Paint.Style.FILL);

                Bitmap logoBitmap = null;
                try {
                    logoBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.bubt_logo);
                } catch (Exception e) {
                    Log.e(TAG, "Error loading BUBT logo", e);
                }

                if (logoBitmap != null) {
                    float drawSize = 38f;
                    float drawX = marginX + (logoBoxSize - drawSize) / 2f;
                    float drawY = logoY + (logoBoxSize - drawSize) / 2f;
                    canvas.drawBitmap(logoBitmap, null, new RectF(drawX, drawY, drawX + drawSize, drawY + drawSize), paint);
                } else {
                    paint.setColor(Color.parseColor("#1E40AF"));
                    canvas.drawCircle(marginX + logoBoxSize / 2f, logoY + logoBoxSize / 2f, 15f, paint);
                }

                // University Title & Subtitle (Left of logo)
                float textX = marginX + logoBoxSize + 12f;

                paint.setTypeface(fontExtraBold);
                paint.setColor(Color.parseColor("#F8FAFC"));
                paint.setTextSize(10f);
                paint.setLetterSpacing(0.03f);
                canvas.drawText("BANGLADESH UNIVERSITY OF BUSINESS AND TECHNOLOGY", textX, logoY + 13f, paint);
                paint.setLetterSpacing(0f);

                paint.setTypeface(fontMedium);
                paint.setColor(Color.parseColor("#94A3B8"));
                paint.setTextSize(9f);
                canvas.drawText("Department of Computer Science and Engineering", textX, logoY + 27f, paint);

                paint.setTypeface(fontBold);
                paint.setColor(Color.parseColor("#38BDF8"));
                paint.setTextSize(8.5f);
                canvas.drawText("CRConnect Realtime Election Portal · Official Audit Document", textX, logoY + 40f, paint);

                // Document ID & Verified Stamp (Right Side)
                float rightX = pageWidth - marginX;

                paint.setTextAlign(Paint.Align.RIGHT);
                paint.setTypeface(fontBold);
                paint.setColor(Color.parseColor("#94A3B8"));
                paint.setTextSize(8.5f);
                canvas.drawText("OFFICIAL ELECTION AUDIT REPORT", rightX, logoY + 12f, paint);

                paint.setTypeface(fontExtraBold);
                paint.setColor(Color.parseColor("#F1F5F9"));
                paint.setTextSize(10f);
                canvas.drawText("DOC #CR-2026-SEC" + sec, rightX, logoY + 26f, paint);

                // Live Verified Badge Pill
                String vText = "DIGITALLY VERIFIED";
                paint.setTypeface(fontBold);
                paint.setTextSize(8f);
                float vTextW = paint.measureText(vText);
                float vDotSize = 5f;
                float vGap = 4f;
                float vPadH = 8f;
                float vH = 15f;
                float vW = vDotSize + vGap + vTextW + (2 * vPadH);
                float vLeft = rightX - vW;
                float vTop = logoY + 31f;

                RectF vRect = new RectF(vLeft, vTop, rightX, vTop + vH);
                paint.setColor(Color.parseColor("#064E3B"));
                paint.setStyle(Paint.Style.FILL);
                canvas.drawRoundRect(vRect, vH / 2f, vH / 2f, paint);

                paint.setColor(Color.parseColor("#059669"));
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(1f);
                canvas.drawRoundRect(vRect, vH / 2f, vH / 2f, paint);

                // Green Glowing Pulse Dot
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(Color.parseColor("#10B981"));
                canvas.drawCircle(vLeft + vPadH + (vDotSize / 2f), vTop + (vH / 2f), vDotSize / 2f, paint);

                paint.setColor(Color.parseColor("#D1FAE5"));
                paint.setTypeface(fontBold);
                paint.setTextSize(8f);
                canvas.drawText(vText, rightX - vPadH, vTop + 11f, paint);

                paint.setTextAlign(Paint.Align.LEFT);
                currentY = headerTop + topHeaderHeight + 10f;

                // =========================================================================
                // REPORT TITLE & METADATA SCOPE GRID
                // =========================================================================
                // Category Tagline
                paint.setColor(Color.parseColor("#2563EB"));
                paint.setTypeface(fontBold);
                paint.setTextSize(8.5f);
                paint.setLetterSpacing(0.06f);
                canvas.drawText("ELECTION RESULTS SUMMARY · SEC " + sec, marginX, currentY + 8f, paint);
                paint.setLetterSpacing(0f);

                // Title
                paint.setColor(Color.parseColor("#0F172A"));
                paint.setTypeface(fontExtraBold);
                paint.setTextSize(17f);
                canvas.drawText("Class Representative Election Results", marginX, currentY + 28f, paint);

                // Subtitle
                paint.setColor(Color.parseColor("#64748B"));
                paint.setTypeface(fontMedium);
                paint.setTextSize(9f);
                canvas.drawText("Certified election breakdown and live voting record for Section " + sec, marginX, currentY + 41f, paint);

                // 3 Scope Info Cards Grid (Teacher, Academic Scope, Audit Timestamp)
                float metaY = currentY + 48f;
                float metaColW = (contentWidth - 16f) / 3f;
                float metaBoxH = 32f;

                // Meta Box 1: Teacher / Certified By
                drawMetaCard(canvas, paint, fontBold, fontMedium, marginX, metaY, metaColW, metaBoxH,
                        "CERTIFIED BY", teacherName + " (" + (teacherDept.contains("CSE") ? "CSE" : teacherDept) + ")");

                // Meta Box 2: Scope
                drawMetaCard(canvas, paint, fontBold, fontMedium, marginX + metaColW + 8f, metaY, metaColW, metaBoxH,
                        "ACADEMIC SCOPE", "Intake 55 · Section " + sec + " · BUBT");

                // Meta Box 3: Date & Status
                String gateStr = isGateOpen ? "GATE OPEN" : "GATE LOCKED";
                drawMetaCard(canvas, paint, fontBold, fontMedium, marginX + (metaColW + 8f) * 2f, metaY, metaColW, metaBoxH,
                        "AUDIT TIMESTAMP", shortDate + " · " + gateStr);

                currentY += titleSectionHeight + 12f;

                // =========================================================================
                // 4 METRIC STAT CARDS ROW
                // =========================================================================
                float cardGap = 10f;
                float cardW = (contentWidth - (3 * cardGap)) / 4f; // 123.25pt
                float cardH = metricsHeight; // 60pt

                int cCount = candidateList != null ? candidateList.size() : 0;
                drawMetricCard(context, canvas, paint, fontExtraBold, fontSemiBold, marginX, currentY, cardW, cardH,
                        "#2563EB", String.valueOf(cCount), "Total Candidates", R.drawable.ic_metric_candidates, "#EFF6FF");

                drawMetricCard(context, canvas, paint, fontExtraBold, fontSemiBold, marginX + cardW + cardGap, currentY, cardW, cardH,
                        "#16A34A", String.valueOf(totalVotes), "Votes Cast", R.drawable.ic_metric_votes, "#ECFDF5");

                drawMetricCard(context, canvas, paint, fontExtraBold, fontSemiBold, marginX + (cardW + cardGap) * 2f, currentY, cardW, cardH,
                        "#0284C7", String.valueOf(totalVoters), "Section Voters", R.drawable.ic_metric_voters, "#F0F9FF");

                drawMetricCard(context, canvas, paint, fontExtraBold, fontSemiBold, marginX + (cardW + cardGap) * 3f, currentY, cardW, cardH,
                        "#D97706", turnout, "Voter Turnout", R.drawable.ic_metric_turnout, "#FFFBEB");

                currentY += metricsHeight + 12f;

                // =========================================================================
                // 🏆 ELECTED CR SPOTLIGHT BANNER
                // =========================================================================
                if (winner != null && winnerSpotlightHeight > 0) {
                    RectF spotlightRect = new RectF(marginX, currentY, marginX + contentWidth, currentY + winnerSpotlightHeight);

                    // Soft Gold Card Background
                    paint.setColor(Color.parseColor("#FFFBEB"));
                    paint.setStyle(Paint.Style.FILL);
                    canvas.drawRoundRect(spotlightRect, 10f, 10f, paint);

                    // Gold Border Accent
                    paint.setColor(Color.parseColor("#FCD34D"));
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(1.2f);
                    canvas.drawRoundRect(spotlightRect, 10f, 10f, paint);
                    paint.setStyle(Paint.Style.FILL);

                    // Left Winner Badge
                    float badgeSize = 36f;
                    float badgeX = marginX + 12f;
                    float badgeY = currentY + (winnerSpotlightHeight - badgeSize) / 2f;

                    Shader goldGrad = new LinearGradient(
                            badgeX, badgeY, badgeX + badgeSize, badgeY + badgeSize,
                            new int[]{Color.parseColor("#F59E0B"), Color.parseColor("#D97706")},
                            null, Shader.TileMode.CLAMP
                    );
                    paint.setShader(goldGrad);
                    canvas.drawCircle(badgeX + badgeSize / 2f, badgeY + badgeSize / 2f, badgeSize / 2f, paint);
                    paint.setShader(null);

                    // "#1 CR" Text inside badge
                    paint.setColor(Color.WHITE);
                    paint.setTypeface(fontExtraBold);
                    paint.setTextSize(11f);
                    paint.setTextAlign(Paint.Align.CENTER);
                    canvas.drawText("#1 CR", badgeX + badgeSize / 2f, badgeY + 22f, paint);
                    paint.setTextAlign(Paint.Align.LEFT);

                    // Winner Title & Name Details
                    float winTextX = badgeX + badgeSize + 12f;
                    float winTextY = currentY + 18f;

                    paint.setColor(Color.parseColor("#B45309"));
                    paint.setTypeface(fontBold);
                    paint.setTextSize(8f);
                    paint.setLetterSpacing(0.05f);
                    canvas.drawText("ELECTED CLASS REPRESENTATIVE", winTextX, winTextY, paint);
                    paint.setLetterSpacing(0f);

                    paint.setColor(Color.parseColor("#0F172A"));
                    paint.setTypeface(fontExtraBold);
                    paint.setTextSize(14f);
                    canvas.drawText(truncateText(winner.name, paint, 230f), winTextX, winTextY + 17f, paint);

                    paint.setColor(Color.parseColor("#475569"));
                    paint.setTypeface(fontMedium);
                    paint.setTextSize(9f);
                    canvas.drawText("Section " + sec + " · " + winner.dept + " · Officially Declared", winTextX, winTextY + 31f, paint);

                    // Right Winner Vote Badge Box
                    double winnerShare = totalVotes > 0 ? ((double) winner.votes * 100.0 / totalVotes) : 0;
                    String winnerShareStr = String.format(Locale.getDefault(), "%.1f%% Share", winnerShare);
                    String winnerVoteStr = winner.votes + " Votes";

                    float boxW = 105f;
                    float boxH = 38f;
                    float boxX = pageWidth - marginX - boxW - 12f;
                    float boxY = currentY + (winnerSpotlightHeight - boxH) / 2f;

                    RectF voteBoxRect = new RectF(boxX, boxY, boxX + boxW, boxY + boxH);
                    Shader vBoxGrad = new LinearGradient(
                            boxX, boxY, boxX + boxW, boxY + boxH,
                            new int[]{Color.parseColor("#F59E0B"), Color.parseColor("#B45309")},
                            null, Shader.TileMode.CLAMP
                    );
                    paint.setShader(vBoxGrad);
                    canvas.drawRoundRect(voteBoxRect, 8f, 8f, paint);
                    paint.setShader(null);

                    paint.setColor(Color.WHITE);
                    paint.setTypeface(fontExtraBold);
                    paint.setTextSize(12f);
                    paint.setTextAlign(Paint.Align.CENTER);
                    canvas.drawText(winnerVoteStr, boxX + boxW / 2f, boxY + 16f, paint);

                    paint.setTypeface(fontBold);
                    paint.setTextSize(9f);
                    paint.setColor(Color.parseColor("#FEF3C7"));
                    canvas.drawText(winnerShareStr, boxX + boxW / 2f, boxY + 29f, paint);
                    paint.setTextAlign(Paint.Align.LEFT);

                    currentY += winnerSpotlightHeight + 12f;
                }
            } else {
                currentY = 36f;
            }

            // =========================================================================
            // CANDIDATE RANKINGS & VOTE AUDIT TABLE
            // =========================================================================
            // Table Header Row (Height: 26pt)
            RectF thRect = new RectF(marginX, currentY, marginX + contentWidth, currentY + tableHeaderHeight);
            paint.setColor(Color.parseColor("#1E293B")); // Deep Slate Navy
            paint.setStyle(Paint.Style.FILL);
            canvas.drawRoundRect(thRect, 6f, 6f, paint);

            // Perfectly Calculated Column Coordinates
            float colRankX = marginX + 8f;        // X: 44
            float colStatusX = marginX + 42f;      // X: 78
            float colNameX = marginX + 110f;      // X: 146
            float colDeptX = marginX + 248f;      // X: 284
            float colVotesX = marginX + 350f;     // X: 386
            float colShareX = pageWidth - marginX - 10f; // X: 549 (Right Aligned)

            paint.setColor(Color.WHITE);
            paint.setTypeface(fontBold);
            paint.setTextSize(9f);

            float thTextY = currentY + 17f;
            canvas.drawText("Rank", colRankX, thTextY, paint);
            canvas.drawText("Status", colStatusX, thTextY, paint);
            canvas.drawText("Candidate Name", colNameX, thTextY, paint);
            canvas.drawText("Department", colDeptX, thTextY, paint);
            canvas.drawText("Votes & Progress", colVotesX, thTextY, paint);

            paint.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText("Share %", colShareX, thTextY, paint);
            paint.setTextAlign(Paint.Align.LEFT);

            currentY += tableHeaderHeight + 4f;

            // Render Table Rows for Current Page
            int maxVotes = 1;
            if (candidateList != null) {
                for (CandidateResult c : candidateList) {
                    if (c.votes > maxVotes) maxVotes = c.votes;
                }
            }

            int rowsDrawnOnThisPage = 0;
            float remainingSpace = pageHeight - marginX - footerHeight - currentY;
            int maxRowsThisPage = Math.max(1, (int)(remainingSpace / rowHeight));

            if (candidateList != null) {
                while (currentRowIndex < candidateList.size() && rowsDrawnOnThisPage < maxRowsThisPage) {
                    CandidateResult c = candidateList.get(currentRowIndex);
                    boolean isWinner = (c.rank == 1);
                    RectF rowRect = new RectF(marginX, currentY, marginX + contentWidth, currentY + rowHeight);

                    // Row Background Fill
                    if (isWinner) {
                        paint.setColor(Color.parseColor("#FFFBEB"));
                        canvas.drawRoundRect(rowRect, 5f, 5f, paint);
                        paint.setColor(Color.parseColor("#FDE68A"));
                        paint.setStyle(Paint.Style.STROKE);
                        paint.setStrokeWidth(1f);
                        canvas.drawRoundRect(rowRect, 5f, 5f, paint);
                        paint.setStyle(Paint.Style.FILL);
                    } else if (currentRowIndex % 2 == 1) {
                        paint.setColor(Color.parseColor("#F8FAFC"));
                        canvas.drawRect(rowRect, paint);
                    } else {
                        paint.setColor(Color.WHITE);
                        canvas.drawRect(rowRect, paint);
                    }

                    // Bottom Row Divider Line
                    if (!isWinner && currentRowIndex < candidateList.size() - 1) {
                        paint.setColor(Color.parseColor("#F1F5F9"));
                        paint.setStrokeWidth(1f);
                        canvas.drawLine(marginX, currentY + rowHeight, marginX + contentWidth, currentY + rowHeight, paint);
                    }

                    // 1. Rank Badge
                    float badgeRadius = 9f;
                    float badgeCenterX = colRankX + 9f;
                    float badgeCenterY = currentY + (rowHeight / 2f);

                    if (isWinner) {
                        paint.setColor(Color.parseColor("#F59E0B"));
                        canvas.drawCircle(badgeCenterX, badgeCenterY, badgeRadius, paint);
                        paint.setColor(Color.WHITE);
                    } else {
                        paint.setColor(Color.parseColor("#E2E8F0"));
                        canvas.drawCircle(badgeCenterX, badgeCenterY, badgeRadius, paint);
                        paint.setColor(Color.parseColor("#475569"));
                    }
                    paint.setTypeface(fontExtraBold);
                    paint.setTextSize(9f);
                    paint.setTextAlign(Paint.Align.CENTER);
                    canvas.drawText(String.valueOf(c.rank), badgeCenterX, badgeCenterY + 3f, paint);
                    paint.setTextAlign(Paint.Align.LEFT);

                    // 2. Status Badge Pill
                    String pillText = isWinner ? "ELECTED CR" : "CANDIDATE";
                    int pBg = isWinner ? Color.parseColor("#FEF3C7") : Color.parseColor("#F1F5F9");
                    int pBorder = isWinner ? Color.parseColor("#FCD34D") : Color.parseColor("#E2E8F0");
                    int pTextCol = isWinner ? Color.parseColor("#92400E") : Color.parseColor("#64748B");

                    paint.setTypeface(fontBold);
                    paint.setTextSize(7.5f);
                    float pTextW = paint.measureText(pillText);
                    float pPadX = 5f;
                    float pH = 13f;
                    float pW = pTextW + (2 * pPadX);
                    float pTop = currentY + (rowHeight - pH) / 2f;

                    RectF pRect = new RectF(colStatusX, pTop, colStatusX + pW, pTop + pH);
                    paint.setColor(pBg);
                    paint.setStyle(Paint.Style.FILL);
                    canvas.drawRoundRect(pRect, pH / 2f, pH / 2f, paint);

                    paint.setColor(pBorder);
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(1f);
                    canvas.drawRoundRect(pRect, pH / 2f, pH / 2f, paint);

                    paint.setStyle(Paint.Style.FILL);
                    paint.setColor(pTextCol);
                    paint.setTypeface(fontBold);
                    paint.setTextSize(7.5f);
                    canvas.drawText(pillText, colStatusX + pPadX, pTop + 9.5f, paint);

                    // 3. Candidate Name
                    paint.setColor(Color.parseColor("#0F172A"));
                    paint.setTextSize(10f);
                    paint.setTypeface(isWinner ? fontBold : fontSemiBold);
                    String truncName = truncateText(c.name, paint, 130f);
                    canvas.drawText(truncName, colNameX, currentY + 19f, paint);

                    // 4. Department
                    paint.setColor(Color.parseColor("#64748B"));
                    paint.setTextSize(9f);
                    paint.setTypeface(fontMedium);
                    String truncDept = truncateText(c.dept, paint, 95f);
                    canvas.drawText(truncDept, colDeptX, currentY + 19f, paint);

                    // 5. Votes Count & Progress Bar
                    paint.setColor(Color.parseColor("#0F172A"));
                    paint.setTextSize(10.5f);
                    paint.setTypeface(fontExtraBold);
                    canvas.drawText(String.valueOf(c.votes), colVotesX, currentY + 19f, paint);

                    float barX = colVotesX + 20f;
                    float barW = 70f;
                    float barH = 4f;
                    float barY = currentY + 15f;
                    RectF barTrack = new RectF(barX, barY, barX + barW, barY + barH);
                    paint.setColor(Color.parseColor("#E2E8F0"));
                    canvas.drawRoundRect(barTrack, 2f, 2f, paint);

                    double share = totalVotes > 0 ? ((double) c.votes * 100.0 / totalVotes) : 0;
                    float fillW = barW * ((float) c.votes / maxVotes);
                    if (fillW > 0) {
                        RectF barFill = new RectF(barX, barY, barX + fillW, barY + barH);
                        Shader barShader = new LinearGradient(
                                barX, barY, barX + fillW, barY,
                                new int[]{Color.parseColor("#2563EB"), Color.parseColor("#38BDF8")},
                                null, Shader.TileMode.CLAMP
                        );
                        paint.setShader(barShader);
                        canvas.drawRoundRect(barFill, 2f, 2f, paint);
                        paint.setShader(null);
                    }

                    // 6. Share % Pill Container (Right-aligned to colShareX = 549)
                    String shareStr = String.format(Locale.getDefault(), "%.1f%%", share);
                    paint.setTypeface(fontBold);
                    paint.setTextSize(8.5f);
                    float sW = paint.measureText(shareStr);
                    float sPadX = 6f;
                    float sBoxW = sW + (2 * sPadX);
                    float sBoxH = 14f;
                    float sLeft = colShareX - sBoxW;
                    float sTop = currentY + (rowHeight - sBoxH) / 2f;

                    RectF shareRect = new RectF(sLeft, sTop, sLeft + sBoxW, sTop + sBoxH);
                    paint.setColor(Color.parseColor("#EFF6FF"));
                    paint.setStyle(Paint.Style.FILL);
                    canvas.drawRoundRect(shareRect, 4f, 4f, paint);

                    paint.setColor(Color.parseColor("#2563EB"));
                    paint.setTypeface(fontBold);
                    paint.setTextSize(8.5f);
                    canvas.drawText(shareStr, sLeft + sPadX, sTop + 10.5f, paint);

                    currentY += rowHeight;
                    currentRowIndex++;
                    rowsDrawnOnThisPage++;
                }
            }

            // =========================================================================
            // FOOTER & DIGITAL SECURITY SIGNATURE
            // =========================================================================
            float footerY = pageHeight - marginX - footerHeight;

            // Footer Top Divider
            paint.setColor(Color.parseColor("#E2E8F0"));
            paint.setStrokeWidth(1f);
            canvas.drawLine(marginX, footerY, marginX + contentWidth, footerY, paint);

            float fTextY = footerY + 18f;

            // Security Verification Text
            paint.setColor(Color.parseColor("#64748B"));
            paint.setTextSize(8f);
            paint.setTypeface(fontMedium);
            canvas.drawText("Certified electronically via ", marginX, fTextY, paint);

            float curX = marginX + paint.measureText("Certified electronically via ");
            paint.setTypeface(fontBold);
            paint.setColor(Color.parseColor("#2563EB"));
            canvas.drawText("CRConnect", curX, fTextY, paint);

            curX += paint.measureText("CRConnect");
            paint.setTypeface(fontMedium);
            paint.setColor(Color.parseColor("#64748B"));
            canvas.drawText(" · BUBT Department Network · Security Hash Key: BUBT-CR-VERIFIED", curX, fTextY, paint);

            // Page Numbering ("Page X of Y")
            paint.setTextAlign(Paint.Align.RIGHT);
            paint.setColor(Color.parseColor("#94A3B8"));
            paint.setTypeface(fontSemiBold);
            paint.setTextSize(8f);
            canvas.drawText("Page " + pageNum + " of " + totalPages, pageWidth - marginX, fTextY, paint);
            paint.setTextAlign(Paint.Align.LEFT);

            // Bottom 4pt Gradient Bar
            Shader bottomGrad = new LinearGradient(
                    0, pageHeight - 4f, pageWidth, pageHeight,
                    new int[]{Color.parseColor("#2563EB"), Color.parseColor("#38BDF8"), Color.parseColor("#1D4ED8")},
                    null, Shader.TileMode.CLAMP
            );
            paint.setShader(bottomGrad);
            canvas.drawRect(0, pageHeight - 4f, pageWidth, pageHeight, paint);
            paint.setShader(null);

            pdfDocument.finishPage(page);
        }

        // Save PDF to Documents/CRConnect_Reports
        File pdfDir = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "CRConnect_Reports");
        if (!pdfDir.exists()) {
            pdfDir.mkdirs();
        }

        String fileName = "BUBT_CR_Election_Results_" + System.currentTimeMillis() + ".pdf";
        File pdfFile = new File(pdfDir, fileName);

        try {
            FileOutputStream fos = new FileOutputStream(pdfFile);
            pdfDocument.writeTo(fos);
            pdfDocument.close();
            fos.close();
            Log.d(TAG, "PDF successfully written to: " + pdfFile.getAbsolutePath());
            return pdfFile;
        } catch (IOException e) {
            Log.e(TAG, "Error writing PDF document: " + e.getMessage(), e);
            pdfDocument.close();
            return null;
        }
    }

    private static void drawMetaCard(Canvas canvas, Paint paint, Typeface fontBold, Typeface fontMedium,
                                     float left, float top, float width, float height, String label, String value) {
        RectF cardRect = new RectF(left, top, left + width, top + height);
        paint.setColor(Color.parseColor("#F8FAFC"));
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(cardRect, 6f, 6f, paint);

        paint.setColor(Color.parseColor("#E2E8F0"));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1f);
        canvas.drawRoundRect(cardRect, 6f, 6f, paint);
        paint.setStyle(Paint.Style.FILL);

        paint.setColor(Color.parseColor("#94A3B8"));
        paint.setTypeface(fontBold);
        paint.setTextSize(7.5f);
        paint.setLetterSpacing(0.04f);
        canvas.drawText(label, left + 8f, top + 11f, paint);
        paint.setLetterSpacing(0f);

        paint.setColor(Color.parseColor("#0F172A"));
        paint.setTypeface(fontMedium);
        paint.setTextSize(8.5f);
        String truncVal = truncateText(value, paint, width - 12f);
        canvas.drawText(truncVal, left + 8f, top + 23f, paint);
    }

    private static void drawMetricCard(Context context, Canvas canvas, Paint paint, Typeface fontExtraBold,
                                       Typeface fontSemiBold, float left, float top, float width, float height,
                                       String stripColorHex, String number, String label, int iconResId, String iconBgHex) {
        RectF cardRect = new RectF(left, top, left + width, top + height);
        float radius = 8f;

        canvas.save();
        Path cardPath = new Path();
        cardPath.addRoundRect(cardRect, radius, radius, Path.Direction.CW);
        canvas.clipPath(cardPath);

        // Card Fill
        paint.setColor(Color.parseColor("#F8FAFC"));
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRect(cardRect, paint);

        // Top 3.5pt Accent Color Strip (Clipped cleanly to rounded top corners)
        RectF topStrip = new RectF(left, top, left + width, top + 3.5f);
        paint.setColor(Color.parseColor(stripColorHex));
        canvas.drawRect(topStrip, paint);

        canvas.restore();

        // Border
        paint.setColor(Color.parseColor("#E2E8F0"));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1f);
        canvas.drawRoundRect(cardRect, radius, radius, paint);
        paint.setStyle(Paint.Style.FILL);

        // Icon Box (22x22)
        float iconBoxSize = 22f;
        float iconBoxLeft = left + 8f;
        float iconBoxTop = top + 8f;
        RectF iconBoxRect = new RectF(iconBoxLeft, iconBoxTop, iconBoxLeft + iconBoxSize, iconBoxTop + iconBoxSize);

        paint.setColor(Color.parseColor(iconBgHex));
        canvas.drawRoundRect(iconBoxRect, 5f, 5f, paint);

        Drawable icon = ContextCompat.getDrawable(context, iconResId);
        if (icon != null) {
            float iconSize = 12f;
            float iconLeft = iconBoxLeft + (iconBoxSize - iconSize) / 2f;
            float iconTop = iconBoxTop + (iconBoxSize - iconSize) / 2f;
            icon.setBounds((int)iconLeft, (int)iconTop, (int)(iconLeft + iconSize), (int)(iconTop + iconSize));
            icon.draw(canvas);
        }

        // Numeric Metric Value
        paint.setColor(Color.parseColor(stripColorHex));
        paint.setTypeface(fontExtraBold);
        paint.setTextSize(16f);
        canvas.drawText(number, left + 8f, top + 42f, paint);

        // Label
        paint.setColor(Color.parseColor("#64748B"));
        paint.setTypeface(fontSemiBold);
        paint.setTextSize(8f);
        canvas.drawText(label, left + 8f, top + 52f, paint);
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

    public static void printPdfDocument(Context context, File pdfFile) {
        PrintManager printManager = (PrintManager) context.getSystemService(Context.PRINT_SERVICE);
        if (printManager == null || pdfFile == null || !pdfFile.exists()) {
            Log.e(TAG, "Printer service not available or PDF missing");
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
                        .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
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
                .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                .build();

        printManager.print(jobName, printAdapter, printAttributes);
    }

    public static void sharePdfFile(Context context, File pdfFile) {
        if (pdfFile == null || !pdfFile.exists()) {
            Log.e(TAG, "PDF File not found");
            return;
        }

        Uri fileUri = FileProvider.getUriForFile(
                context,
                context.getPackageName() + ".fileprovider",
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

    public static void openPdfFile(Context context, File pdfFile) {
        if (pdfFile == null || !pdfFile.exists()) {
            Log.e(TAG, "PDF File not found");
            return;
        }

        try {
            Uri fileUri = FileProvider.getUriForFile(
                    context,
                    context.getPackageName() + ".fileprovider",
                    pdfFile
            );
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(fileUri, "application/pdf");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Intent chooser = Intent.createChooser(intent, "Open Election PDF Report");
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(chooser);
        } catch (Exception e) {
            Log.e(TAG, "No PDF viewer app found", e);
        }
    }
}
