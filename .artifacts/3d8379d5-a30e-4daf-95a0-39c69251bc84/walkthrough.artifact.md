# Walkthrough - CRConnect Pixel-Accurate PDF Generator

Polished the `PdfReportGenerator` Canvas/PdfDocument engine to match `docs/CRConnect_PDF_Design.html` with absolute pixel accuracy across rules A-E while fully preserving the `PdfReadyBottomSheet` actions (**Print / Save PDF**, **Share PDF**, **Open PDF**).

## Changes Made

### 1. High-Fidelity Canvas PDF Engine (`PdfReportGenerator.java`)
- Implemented precise scaling (`px(v) = v * 595f / 794f`) mapping A4 CSS px to PDF points.
- Bundled and applied Plus Jakarta Sans static TTFs (`medium`, `semibold`, `bold`, `extrabold`).
- Styled Header (A), Title block (B), Metric cards (C) with top color strips and vector icon boxes, Section heading + Table (D) with rank badges, status pills, gradient vote bars, share chips, multi-page pagination with repeated table headers and footer page numbering ("Page X of Y"), and Footer (E).

### 2. Action Suite Integration (`PdfReadyBottomSheet`)
- Ensured `generatePdfDocument()` compiles and writes the PDF report to disk synchronously and returns the valid `File` object.
- Fully wired **Print / Save PDF**, **Share PDF**, and **Open PDF** in `ExportPdfBottomSheet` to interact with the generated PDF file.
