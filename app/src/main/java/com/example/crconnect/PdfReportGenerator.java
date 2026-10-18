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

    private static float px(float v) {
        return v * 595f / 794f;
    }

    /**
     * Generates a professionally formatted PDF file matching the exact HTML design specification (A-E)
     * using PdfDocument Canvas rendering, and returns the real PDF File so PdfReadyBottomSheet appears.
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
        Log.d("PDF_DEBUG", "Generating PDF via Canvas/PdfDocument design engine");

        // Load Plus Jakarta Sans Typefaces (500, 600, 700, 800)
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
        int pageWidth = 595;
        int pageHeight = 842;
        float marginX = px(40f);

        // Pre-calculate page breakdown and row heights
        float usableHeight = pageHeight - px(36f) - px(4f);
        float fixedHeaderSectionHeight = px(6f) + px(28f) + px(56f) + px(24f) + px(1f) // accent + header
                + px(28f) + px(8f) + px(6f) + px(55f) + px(12f) + px(17f) + px(14f) + px(22f) + px(20f) // title + meta + badge
                + px(24f) + px(117f) + px(24f) // metrics
                + px(45f) + px(30f) + px(28f); // section heading + table header + table wrap margin
        float rowH = px(36f);
        float remainingForRows = usableHeight - fixedHeaderSectionHeight;
        int rowsOnPage1 = Math.max(1, (int)(remainingForRows / rowH));

        int totalPages = 1;
        if (candidateList != null && candidateList.size() > rowsOnPage1) {
            int remainingRows = candidateList.size() - rowsOnPage1;
            float page2RowH = usableHeight - (px(6f) + px(45f) + px(30f) + px(28f));
            int rowsPerPageElse = Math.max(1, (int)(page2RowH / rowH));
            totalPages = 1 + (int) Math.ceil((double) remainingRows / rowsPerPageElse);
        }

        int currentRowIndex = 0;
        for (int pageNum = 1; pageNum <= totalPages; pageNum++) {
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create();
            PdfDocument.Page page = pdfDocument.startPage(pageInfo);
            Canvas canvas = page.getCanvas();
            Paint paint = new Paint();
            paint.setAntiAlias(true);

            // Background
            canvas.drawColor(Color.WHITE);

            // ==========================================
            // TOP ACCENT BAR (6px gradient)
            // ==========================================
            Shader accentShader = new LinearGradient(
                    0, 0, pageWidth, 0,
                    new int[]{Color.parseColor("#0066FF"), Color.parseColor("#38BDF8"), Color.parseColor("#0B3A9E")},
                    new float[]{0.0f, 0.6f, 1.0f},
                    Shader.TileMode.CLAMP
            );
            paint.setShader(accentShader);
            canvas.drawRect(0, 0, pageWidth, px(6f), paint);
            paint.setShader(null);

            float currentY = px(6f);

            if (pageNum == 1) {
                // ==========================================
                // HEADER (padding 28/40/24, border-bottom 1 #E2E8F0, gradient 135deg #F8FAFF -> #EFF6FF)
                // ==========================================
                float headerTop = currentY;
                float headerHeight = px(28f) + px(56f) + px(24f);
                RectF headerRect = new RectF(0, headerTop, pageWidth, headerTop + headerHeight);

                Shader headerBgShader = new LinearGradient(
                        0, headerTop, pageWidth, headerTop + headerHeight,
                        new int[]{Color.parseColor("#F8FAFF"), Color.parseColor("#EFF6FF")},
                        null, Shader.TileMode.CLAMP
                );
                paint.setShader(headerBgShader);
                paint.setStyle(Paint.Style.FILL);
                canvas.drawRect(headerRect, paint);
                paint.setShader(null);

                // Header Bottom Border
                paint.setColor(Color.parseColor("#E2E8F0"));
                paint.setStrokeWidth(px(1f));
                canvas.drawLine(0, headerTop + headerHeight, pageWidth, headerTop + headerHeight, paint);

                float logoY = headerTop + px(28f);
                float logoSlotSize = px(56f);

                Bitmap logoBitmap = null;
                try {
                    logoBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.bubt_logo);
                } catch (Exception e) {
                    Log.e(TAG, "Error loading BUBT logo", e);
                }

                if (logoBitmap != null) {
                    float drawSize = px(70f);
                    float drawX = marginX + (logoSlotSize - drawSize) / 2f;
                    float drawY = logoY + (logoSlotSize - drawSize) / 2f;
                    RectF logoDst = new RectF(drawX, drawY, drawX + drawSize, drawY + drawSize);
                    canvas.drawBitmap(logoBitmap, null, logoDst, paint);
                } else {
                    paint.setColor(Color.parseColor("#0066FF"));
                    canvas.drawRoundRect(new RectF(marginX, logoY, marginX + logoSlotSize, logoY + logoSlotSize), px(16f), px(16f), paint);
                }

                // Header Left Text (Starts 18 after slot)
                float textStartX = marginX + logoSlotSize + px(18f);

                // University Name: 13/800 #0B3A9E, uppercase, letterSpacing .3
                paint.setTypeface(fontExtraBold);
                paint.setColor(Color.parseColor("#0B3A9E"));
                paint.setTextSize(px(13f));
                paint.setLetterSpacing(0.3f / 13f);
                canvas.drawText("BANGLADESH UNIVERSITY OF BUSINESS AND TECHNOLOGY", textStartX, logoY + px(12f), paint);
                paint.setLetterSpacing(0f);

                // Subtitle: 11/500 #64748B, 1 below
                paint.setTypeface(fontMedium);
                paint.setColor(Color.parseColor("#64748B"));
                paint.setTextSize(px(11f));
                canvas.drawText("Department of Computer Science and Engineering", textStartX, logoY + px(26f), paint);

                // Report chip: margin-top 6, padding 3x10, fully rounded, bg #E6F0FF, border 1 #BFD7FF, 10px icon + 6 gap + text 11/700 #0066FF
                String chipText = "Official Election Report · CRConnect System";
                paint.setTypeface(fontBold);
                paint.setTextSize(px(11f));
                float chipTextWidth = paint.measureText(chipText);
                float chipPadH = px(10f);
                float chipPadV = px(3f);
                float chipH = px(11f) + (2 * chipPadV);
                float chipW = chipTextWidth + (2 * chipPadH);
                float chipIconSize = px(10f);
                float chipGap = px(6f);
                float totalChipW = chipIconSize + chipGap + chipW;

                RectF chipRect = new RectF(textStartX, logoY + px(35f), textStartX + totalChipW + (2 * chipPadH), logoY + px(35f) + chipH);
                paint.setColor(Color.parseColor("#E6F0FF"));
                paint.setStyle(Paint.Style.FILL);
                canvas.drawRoundRect(chipRect, chipH / 2f, chipH / 2f, paint);

                paint.setColor(Color.parseColor("#BFD7FF"));
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(px(1f));
                canvas.drawRoundRect(chipRect, chipH / 2f, chipH / 2f, paint);

                Drawable docIcon = ContextCompat.getDrawable(context, R.drawable.ic_doc);
                if (docIcon != null) {
                    float iconLeft = textStartX + chipPadH;
                    float iconTop = chipRect.top + (chipH - chipIconSize) / 2f;
                    docIcon.setBounds((int)iconLeft, (int)iconTop, (int)(iconLeft + chipIconSize), (int)(iconTop + chipIconSize));
                    docIcon.draw(canvas);
                }

                paint.setStyle(Paint.Style.FILL);
                paint.setColor(Color.parseColor("#0066FF"));
                paint.setTypeface(fontBold);
                paint.setTextSize(px(11f));
                canvas.drawText(chipText, textStartX + chipPadH + chipIconSize + chipGap, chipRect.top + px(11f), paint);

                // Header Right, right-aligned to pageWidth - 40
                float rightX = pageWidth - marginX;

                // DOC ID: 11/700 #64748B letterSpacing .5
                String sec = sectionNo != null ? sectionNo : "6";
                String docId = "DOC #CR-2026-SEC" + sec + "-001";
                paint.setColor(Color.parseColor("#64748B"));
                paint.setTypeface(fontBold);
                paint.setTextSize(px(11f));
                paint.setLetterSpacing(0.5f / 11f);
                paint.setTextAlign(Paint.Align.RIGHT);
                canvas.drawText(docId, rightX, logoY + px(12f), paint);
                paint.setLetterSpacing(0f);

                // Digitally Verified pill: margin-top 6, bg #DCFCE7, border 1 #BBF7D0, padding 3x10, 6px dot #16A34A, gap 5, text 11/700 #166534
                String verifiedText = "Digitally Verified";
                paint.setTypeface(fontBold);
                paint.setTextSize(px(11f));
                float vTextW = paint.measureText(verifiedText);
                float vDotSize = px(6f);
                float vGap = px(5f);
                float vPadH = px(10f);
                float vPadV = px(3f);
                float vH = px(11f) + (2 * vPadV);
                float vW = vDotSize + vGap + vTextW + (2 * vPadH);
                float vLeft = rightX - vW;
                float vTop = logoY + px(35f);

                RectF verifiedRect = new RectF(vLeft, vTop, rightX, vTop + vH);
                paint.setColor(Color.parseColor("#DCFCE7"));
                paint.setStyle(Paint.Style.FILL);
                canvas.drawRoundRect(verifiedRect, vH / 2f, vH / 2f, paint);

                paint.setColor(Color.parseColor("#BBF7D0"));
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(px(1f));
                canvas.drawRoundRect(verifiedRect, vH / 2f, vH / 2f, paint);

                // Green dot
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(Color.parseColor("#16A34A"));
                canvas.drawCircle(vLeft + vPadH + (vDotSize / 2f), vTop + (vH / 2f), vDotSize / 2f, paint);

                // Verified text
                paint.setColor(Color.parseColor("#166534"));
                paint.setTypeface(fontBold);
                paint.setTextSize(px(11f));
                canvas.drawText(verifiedText, vLeft + vPadH + vDotSize + vGap, vTop + px(11f), paint);

                paint.setTextAlign(Paint.Align.LEFT);
                currentY += headerHeight;

                // ==========================================
                // TITLE BLOCK (padding 28/40/20)
                // ==========================================
                currentY += px(28f);

                // Label "Election Report" 11/700 #94A3B8 letterSpacing 1, mixed case, margin-bottom 6
                paint.setColor(Color.parseColor("#94A3B8"));
                paint.setTypeface(fontBold);
                paint.setTextSize(px(11f));
                paint.setLetterSpacing(1.0f / 11f);
                canvas.drawText("Election Report", marginX, currentY, paint);
                paint.setLetterSpacing(0f);

                currentY += px(6f) + px(11f);

                // Title 22/800 #0F172A, line-height 1.25, forced TWO lines
                paint.setColor(Color.parseColor("#0F172A"));
                paint.setTypeface(fontExtraBold);
                paint.setTextSize(px(22f));
                float lineH = px(22f) * 1.25f;
                canvas.drawText("Official Class Representative", marginX, currentY, paint);
                currentY += lineH;
                canvas.drawText("Election Results — Section " + sec, marginX, currentY, paint);

                currentY += px(12f);

                // Meta row (margin-top 12): 3 items side by side with gap 24
                String timeStamp = new SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.getDefault()).format(new Date());
                float metaX = marginX;
                metaX = drawMetaItem(canvas, paint, fontBold, fontSemiBold, "GENERATED", timeStamp, metaX, currentY);
                metaX += px(24f);
                metaX = drawMetaItem(canvas, paint, fontBold, fontSemiBold, "CERTIFIED BY", teacherName + " · " + teacherDept, metaX, currentY);
                metaX += px(24f);
                drawMetaItem(canvas, paint, fontBold, fontSemiBold, "ACADEMIC YEAR", "2025–2026 · Intake 55", metaX, currentY);

                currentY += px(14f) + px(22f);

                // Status badge (margin-top 14): padding 5x12, radius 8, 12/700, 7px dot, gap 6
                float badgeH = px(22f);
                String statusText = isGateOpen ? "Election Status: Live Voting Gate OPEN" : "Election Status: Live Voting Gate LOCKED";
                int sBg = isGateOpen ? Color.parseColor("#DCFCE7") : Color.parseColor("#FEF2F2");
                int sBorder = isGateOpen ? Color.parseColor("#BBF7D0") : Color.parseColor("#FECACA");
                int sTextCol = isGateOpen ? Color.parseColor("#166534") : Color.parseColor("#B91C1C");
                int sDotCol = isGateOpen ? Color.parseColor("#16A34A") : Color.parseColor("#DC2626");

                paint.setTypeface(fontBold);
                paint.setTextSize(px(12f));
                float sTextW = paint.measureText(statusText);
                float sDotSize = px(7f);
                float sGap = px(6f);
                float sPadX = px(12f);
                float sW = (2 * sPadX) + sDotSize + sGap + sTextW;

                RectF statusRect = new RectF(marginX, currentY, marginX + sW, currentY + badgeH);
                paint.setColor(sBg);
                paint.setStyle(Paint.Style.FILL);
                canvas.drawRoundRect(statusRect, px(8f), px(8f), paint);

                paint.setColor(sBorder);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(px(1f));
                canvas.drawRoundRect(statusRect, px(8f), px(8f), paint);

                paint.setStyle(Paint.Style.FILL);
                paint.setColor(sDotCol);
                canvas.drawCircle(marginX + sPadX + (sDotSize / 2f), currentY + (badgeH / 2f), sDotSize / 2f, paint);

                paint.setColor(sTextCol);
                paint.setTypeface(fontBold);
                paint.setTextSize(px(12f));
                canvas.drawText(statusText, marginX + sPadX + sDotSize + sGap, currentY + px(15f), paint);

                currentY += badgeH + px(20f);

                // ==========================================
                // METRIC CARDS (margin 0/40/24; 4 equal columns, gap 12)
                // ==========================================
                float metricsY = currentY;
                float totalUsableW = pageWidth - (2 * marginX);
                float mGap = px(12f);
                float cardW = (totalUsableW - (3 * mGap)) / 4f;
                float cardH = px(78f);

                drawMetricCard(context, canvas, paint, fontExtraBold, fontSemiBold, marginX, metricsY, cardW, cardH, "#0066FF", String.valueOf(candidateList != null ? candidateList.size() : 0), "Candidates", R.drawable.ic_metric_candidates, "#E6F0FF");
                drawMetricCard(context, canvas, paint, fontExtraBold, fontSemiBold, marginX + cardW + mGap, metricsY, cardW, cardH, "#16A34A", String.valueOf(totalVotes), "Votes Cast", R.drawable.ic_metric_votes, "#DCFCE7");
                drawMetricCard(context, canvas, paint, fontExtraBold, fontSemiBold, marginX + (cardW + mGap) * 2, metricsY, cardW, cardH, "#0EA5E9", String.valueOf(totalVoters), "Registered Voters", R.drawable.ic_metric_voters, "#E0F2FE");
                drawMetricCard(context, canvas, paint, fontExtraBold, fontSemiBold, marginX + (cardW + mGap) * 3, metricsY, cardW, cardH, "#D97706", turnout, "Turnout Rate", R.drawable.ic_metric_turnout, "#FEF3C7");

                currentY += cardH + px(24f);
            } else {
                currentY += px(24f);
            }

            // ==========================================
            // SECTION HEADING + TABLE
            // ==========================================
            float headingY = currentY;
            paint.setColor(Color.parseColor("#0F172A"));
            paint.setTypeface(fontExtraBold);
            paint.setTextSize(px(13f));
            paint.setLetterSpacing(0.2f / 13f);
            canvas.drawText("Candidate Rankings & Vote Distribution", marginX, headingY + px(10f), paint);
            paint.setLetterSpacing(0f);

            paint.setColor(Color.parseColor("#E2E8F0"));
            paint.setStrokeWidth(px(1f));
            float hTextW = paint.measureText("Candidate Rankings & Vote Distribution");
            canvas.drawLine(marginX + hTextW + px(10f), headingY + px(7f), pageWidth - marginX, headingY + px(7f), paint);

            currentY += px(24f);

            // Table Header Row
            float tableHeaderH = px(30f);
            RectF tableHeaderRect = new RectF(marginX, currentY, pageWidth - marginX, currentY + tableHeaderH);

            Shader thShader = new LinearGradient(
                    marginX, currentY, pageWidth - marginX, currentY,
                    new int[]{Color.parseColor("#0066FF"), Color.parseColor("#0052CC")},
                    null, Shader.TileMode.CLAMP
            );
            paint.setShader(thShader);
            paint.setStyle(Paint.Style.FILL);
            Path thPath = new Path();
            thPath.addRoundRect(tableHeaderRect, px(12f), px(12f), Path.Direction.CW);
            canvas.drawPath(thPath, paint);
            canvas.drawRect(new RectF(marginX, currentY + px(12f), pageWidth - marginX, currentY + tableHeaderH), paint);
            paint.setShader(null);

            float cRankX = marginX + px(12f);
            float cStatusX = marginX + px(54f);
            float cNameX = marginX + px(130f);
            float cDeptX = marginX + px(270f);
            float cVotesX = marginX + px(400f);
            float cShareX = pageWidth - marginX - px(12f);

            paint.setColor(Color.WHITE);
            paint.setTypeface(fontBold);
            paint.setTextSize(px(11f));
            paint.setLetterSpacing(0.4f / 11f);

            float thTextY = currentY + px(19f);
            canvas.drawText("Rank", cRankX, thTextY, paint);
            canvas.drawText("Status", cStatusX, thTextY, paint);
            canvas.drawText("Candidate Name", cNameX, thTextY, paint);
            canvas.drawText("Department", cDeptX, thTextY, paint);
            canvas.drawText("Votes", cVotesX, thTextY, paint);

            paint.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText("Share %", cShareX, thTextY, paint);
            paint.setTextAlign(Paint.Align.LEFT);
            paint.setLetterSpacing(0f);

            currentY += tableHeaderH;

            // Render Table Rows for this page
            int maxVotes = 1;
            if (candidateList != null) {
                for (CandidateResult c : candidateList) {
                    if (c.votes > maxVotes) maxVotes = c.votes;
                }
            }

            int rowsDrawnOnThisPage = 0;
            int maxRowsThisPage = (pageNum == 1) ? rowsOnPage1 : (int)((usableHeight - currentY - px(40f)) / rowH);
            if (maxRowsThisPage < 1) maxRowsThisPage = 1;

            if (candidateList != null) {
                while (currentRowIndex < candidateList.size() && rowsDrawnOnThisPage < maxRowsThisPage) {
                    CandidateResult c = candidateList.get(currentRowIndex);
                    boolean isWinner = (c.rank == 1);
                    RectF rowRect = new RectF(marginX, currentY, pageWidth - marginX, currentY + rowH);

                    if (isWinner) {
                        Shader rowBgShader = new LinearGradient(
                                marginX, currentY, pageWidth - marginX, currentY,
                                new int[]{Color.parseColor("#FFFBEB"), Color.WHITE},
                                null, Shader.TileMode.CLAMP
                        );
                        paint.setShader(rowBgShader);
                        canvas.drawRect(rowRect, paint);
                        paint.setShader(null);
                    } else if (currentRowIndex % 2 == 0) {
                        paint.setColor(Color.parseColor("#F8FAFC"));
                        canvas.drawRect(rowRect, paint);
                    } else {
                        paint.setColor(Color.WHITE);
                        canvas.drawRect(rowRect, paint);
                    }

                    if (currentRowIndex < candidateList.size() - 1) {
                        paint.setColor(Color.parseColor("#F1F5F9"));
                        paint.setStrokeWidth(px(1f));
                        canvas.drawLine(marginX, currentY + rowH, pageWidth - marginX, currentY + rowH, paint);
                    }

                    // 1. Rank Badge
                    float badgeSize = px(28f);
                    RectF rBadgeRect = new RectF(cRankX, currentY + px(4f), cRankX + badgeSize, currentY + px(4f) + badgeSize);
                    if (c.rank == 1) {
                        Shader badgeShader = new LinearGradient(
                                cRankX, currentY + px(4f), cRankX + badgeSize, currentY + px(4f) + badgeSize,
                                new int[]{Color.parseColor("#F59E0B"), Color.parseColor("#D97706")},
                                null, Shader.TileMode.CLAMP
                        );
                        paint.setShader(badgeShader);
                        canvas.drawRoundRect(rBadgeRect, px(8f), px(8f), paint);
                        paint.setShader(null);
                        paint.setColor(Color.WHITE);
                    } else {
                        paint.setColor(Color.parseColor("#E2E8F0"));
                        canvas.drawRoundRect(rBadgeRect, px(8f), px(8f), paint);
                        paint.setColor(Color.parseColor("#475569"));
                    }
                    paint.setTypeface(fontExtraBold);
                    paint.setTextSize(px(12f));
                    canvas.drawText(String.valueOf(c.rank), cRankX + px(9f), currentY + px(22f), paint);

                    // 2. Status Pill
                    String pillText = isWinner ? "Winner" : "Runner-up";
                    int pBg = isWinner ? Color.parseColor("#FEF3C7") : Color.parseColor("#F1F5F9");
                    int pBorder = isWinner ? Color.parseColor("#FDE68A") : Color.parseColor("#E2E8F0");
                    int pTextCol = isWinner ? Color.parseColor("#92400E") : Color.parseColor("#475569");

                    paint.setTypeface(fontBold);
                    paint.setTextSize(px(10f));
                    float pTextW = paint.measureText(pillText);
                    float pPadX = px(8f);
                    float pPadY = px(3f);
                    float pH = px(10f) + (2 * pPadY);
                    float pW = pTextW + (2 * pPadX);

                    RectF pRect = new RectF(cStatusX, currentY + px(9f), cStatusX + pW, currentY + px(9f) + pH);
                    paint.setColor(pBg);
                    paint.setStyle(Paint.Style.FILL);
                    canvas.drawRoundRect(pRect, pH / 2f, pH / 2f, paint);

                    paint.setColor(pBorder);
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(px(1f));
                    canvas.drawRoundRect(pRect, pH / 2f, pH / 2f, paint);

                    paint.setStyle(Paint.Style.FILL);
                    paint.setColor(pTextCol);
                    paint.setTypeface(fontBold);
                    paint.setTextSize(px(10f));
                    canvas.drawText(pillText, cStatusX + pPadX, currentY + px(21f), paint);

                    // 3. Candidate Name
                    paint.setColor(Color.parseColor("#0F172A"));
                    paint.setTextSize(px(12f));
                    paint.setTypeface(isWinner ? fontBold : fontSemiBold);
                    String truncName = truncateText(c.name, paint, px(130f));
                    canvas.drawText(truncName, cNameX, currentY + px(22f), paint);

                    // 4. Department
                    paint.setColor(Color.parseColor("#64748B"));
                    paint.setTextSize(px(12f));
                    paint.setTypeface(fontMedium);
                    String truncDept = truncateText(c.dept, paint, px(120f));
                    canvas.drawText(truncDept, cDeptX, currentY + px(22f), paint);

                    // 5. Votes & Progress Bar
                    paint.setColor(Color.parseColor("#0F172A"));
                    paint.setTextSize(px(14f));
                    paint.setTypeface(fontExtraBold);
                    canvas.drawText(String.valueOf(c.votes), cVotesX, currentY + px(18f), paint);

                    float barW = px(80f);
                    float barH = px(5f);
                    float barY = currentY + px(23f);
                    RectF barTrack = new RectF(cVotesX, barY, cVotesX + barW, barY + barH);
                    paint.setColor(Color.parseColor("#E2E8F0"));
                    canvas.drawRoundRect(barTrack, px(3f), px(3f), paint);

                    double share = totalVotes > 0 ? ((double) c.votes * 100.0 / totalVotes) : 0;
                    float fillW = barW * ((float) c.votes / maxVotes);
                    if (fillW > 0) {
                        RectF barFill = new RectF(cVotesX, barY, cVotesX + fillW, barY + barH);
                        Shader barShader = new LinearGradient(
                                cVotesX, barY, cVotesX + fillW, barY,
                                new int[]{Color.parseColor("#0066FF"), Color.parseColor("#38BDF8")},
                                null, Shader.TileMode.CLAMP
                        );
                        paint.setShader(barShader);
                        canvas.drawRoundRect(barFill, px(3f), px(3f), paint);
                        paint.setShader(null);
                    }

                    // 6. Share % Chip (right-aligned)
                    String shareStr = String.format(Locale.getDefault(), "%.1f%%", share);
                    paint.setTypeface(fontBold);
                    paint.setTextSize(px(11f));
                    float sW2 = paint.measureText(shareStr);
                    float sPadX2 = px(8f);
                    float sPadY2 = px(3f);
                    float sH2 = px(11f) + (2 * sPadY2);
                    float sBoxW = sW2 + (2 * sPadX2);
                    float sLeft = cShareX - sBoxW;
                    float sTop = currentY + px(9f);

                    RectF shareRect = new RectF(sLeft, sTop, sLeft + sBoxW, sTop + sH2);
                    paint.setColor(Color.parseColor("#E6F0FF"));
                    paint.setStyle(Paint.Style.FILL);
                    canvas.drawRoundRect(shareRect, px(6f), px(6f), paint);

                    paint.setColor(Color.parseColor("#0066FF"));
                    paint.setTypeface(fontBold);
                    paint.setTextSize(px(11f));
                    canvas.drawText(shareStr, sLeft + sPadX2, sTop + px(13f), paint);

                    currentY += rowH;
                    currentRowIndex++;
                    rowsDrawnOnThisPage++;
                }
            }

            // ==========================================
            // FOOTER & BOTTOM BAR
            // ==========================================
            float footerH = px(36f);
            float footerY = pageHeight - footerH - px(4f);
            RectF footerRect = new RectF(0, footerY, pageWidth, footerY + footerH);

            Shader footerBg = new LinearGradient(
                    0, footerY, pageWidth, footerY,
                    new int[]{Color.parseColor("#F8FAFF"), Color.parseColor("#EFF6FF")},
                    null, Shader.TileMode.CLAMP
            );
            paint.setShader(footerBg);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawRect(footerRect, paint);
            paint.setShader(null);

            // Footer Top Border
            paint.setColor(Color.parseColor("#E2E8F0"));
            paint.setStrokeWidth(px(1f));
            canvas.drawLine(0, footerY, pageWidth, footerY, paint);

            // Footer Left Text
            paint.setColor(Color.parseColor("#64748B"));
            paint.setTextSize(px(10f));
            paint.setTypeface(fontSemiBold);
            float fTextY = footerY + px(22f);

            canvas.drawText("Certified electronically via ", marginX, fTextY, paint);
            float cX = marginX + paint.measureText("Certified electronically via ");
            paint.setTypeface(fontBold);
            paint.setColor(Color.parseColor("#0066FF"));
            canvas.drawText("CRConnect", cX, fTextY, paint);
            float restX2 = cX + paint.measureText("CRConnect");
            paint.setTypeface(fontSemiBold);
            paint.setColor(Color.parseColor("#64748B"));
            canvas.drawText(" · BUBT Department Network · Secure Key: BUBT-CR-VERIFIED", restX2, fTextY, paint);

            // Footer Right Text ("Page X of Y")
            paint.setTextAlign(Paint.Align.RIGHT);
            paint.setColor(Color.parseColor("#94A3B8"));
            paint.setTypeface(fontSemiBold);
            canvas.drawText("Page " + pageNum + " of " + totalPages, pageWidth - marginX, fTextY, paint);
            paint.setTextAlign(Paint.Align.LEFT);

            // Bottom 4px Gradient Bar
            Shader bottomBarShader = new LinearGradient(
                    0, pageHeight - px(4f), pageWidth, pageHeight,
                    new int[]{Color.parseColor("#0066FF"), Color.parseColor("#38BDF8"), Color.parseColor("#0B3A9E")},
                    null, Shader.TileMode.CLAMP
            );
            paint.setShader(bottomBarShader);
            canvas.drawRect(0, pageHeight - px(4f), pageWidth, pageHeight, paint);
            paint.setShader(null);

            pdfDocument.finishPage(page);
        }

        // --- SAVE TO DISK ---
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
            Log.d("PDF_DEBUG", "PDF successfully written to: " + pdfFile.getAbsolutePath());
            return pdfFile;
        } catch (IOException e) {
            Log.e(TAG, "Error writing PDF document: " + e.getMessage(), e);
            pdfDocument.close();
            return null;
        }
    }

    private static float drawMetaItem(Canvas canvas, Paint paint, Typeface fontBold, Typeface fontSemiBold, String label, String value, float x, float y) {
        paint.setColor(Color.parseColor("#94A3B8"));
        paint.setTypeface(fontBold);
        paint.setTextSize(px(10f));
        paint.setLetterSpacing(0.8f / 10f);
        canvas.drawText(label, x, y, paint);
        paint.setLetterSpacing(0f);

        paint.setColor(Color.parseColor("#0F172A"));
        paint.setTypeface(fontSemiBold);
        paint.setTextSize(px(12f));
        canvas.drawText(value, x, y + px(15f), paint);

        return x + paint.measureText(value);
    }

    private static void drawMetricCard(Context context, Canvas canvas, Paint paint, Typeface fontExtraBold, Typeface fontSemiBold, float left, float top, float width, float height, String stripColorHex, String number, String label, int iconResId, String iconBgHex) {
        RectF cardRect = new RectF(left, top, left + width, top + height);

        canvas.save();
        Path cardPath = new Path();
        cardPath.addRoundRect(cardRect, px(16f), px(16f), Path.Direction.CW);
        canvas.clipPath(cardPath);

        // Card Background
        paint.setColor(Color.parseColor("#F8FAFC"));
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRect(cardRect, paint);

        // Top 3px Color Strip
        paint.setColor(Color.parseColor(stripColorHex));
        RectF stripRect = new RectF(left, top, left + width, top + px(3f));
        canvas.drawRect(stripRect, paint);
        canvas.restore();

        // Card Border
        paint.setColor(Color.parseColor("#E2E8F0"));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(px(1f));
        canvas.drawRoundRect(cardRect, px(16f), px(16f), paint);

        // Icon Box (32x32, radius 10)
        float iconBoxSize = px(32f);
        float iconBoxLeft = left + px(14f);
        float iconBoxTop = top + px(14f);
        RectF iconBoxRect = new RectF(iconBoxLeft, iconBoxTop, iconBoxLeft + iconBoxSize, iconBoxTop + iconBoxSize);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.parseColor(iconBgHex));
        canvas.drawRoundRect(iconBoxRect, px(10f), px(10f), paint);

        // Draw Icon
        Drawable icon = ContextCompat.getDrawable(context, iconResId);
        if (icon != null) {
            float iconSize = px(16f);
            float iconLeft = iconBoxLeft + (iconBoxSize - iconSize) / 2f;
            float iconTop = iconBoxTop + (iconBoxSize - iconSize) / 2f;
            icon.setBounds((int)iconLeft, (int)iconTop, (int)(iconLeft + iconSize), (int)(iconTop + iconSize));
            icon.draw(canvas);
        }

        // Number (28/800 line-height 1)
        paint.setColor(Color.parseColor(stripColorHex));
        paint.setTypeface(fontExtraBold);
        paint.setTextSize(px(28f));
        canvas.drawText(number, left + px(14f), top + px(65f), paint);

        // Label (11/600 #64748B, margin-top 4)
        paint.setColor(Color.parseColor("#64748B"));
        paint.setTypeface(fontSemiBold);
        paint.setTextSize(px(11f));
        canvas.drawText(label, left + px(14f), top + px(80f), paint);
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
