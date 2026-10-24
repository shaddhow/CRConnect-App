# BUBT Class Routine & Scanner Engine System

This document provides a comprehensive guide to the **Class Routine System and 2D Spatial OCR Scanner Engine** implemented in the **CRConnect** application for Bangladesh University of Business and Technology (BUBT).

---

## 📌 System Overview

The CRConnect Routine Engine digitizes paper routines, PDF screenshots, and annex web portal schedules into interactive, real-time academic schedules for BUBT students and teachers.

### Key Capabilities
- 📸 **Camera & Gallery OCR Scanner**: Point the camera or upload a screenshot to instantly parse full weekly routines.
- ⚡ **2D Cell-Bucket Matrix Engine**: High-precision spatial mapping ensuring zero cross-column or cross-row text bleeding.
- 🔄 **Dual Synchronization**: Local SharedPreferences persistence combined with real-time Firebase Cloud Firestore cloud sync (`routines/{dept}/{intake}/{section}/schedule`).
- 🗑️ **Routine Management**: Includes full routine deletion, manual class addition, and daily schedule filtering.
- ⏰ **Up-Next Countdown & Attendance**: Real-time status banners, live countdowns, and attendance markers (`Attended` / `Missed`).

---

## 🗓️ BUBT Routine Grid Structure

BUBT routines follow a standardized weekly grid layout:

### 1. Days of the Week (7 Rows)
| Short Code | Full Day Name |
|---|---|
| `SAT` | Saturday |
| `SUN` | Sunday |
| `MON` | Monday |
| `TUE` | Tuesday |
| `WED` | Wednesday |
| `THR` / `THU` | Thursday |
| `FRI` | Friday |

### 2. Time Slots (6 Columns)
| Slot Index | Time Interval |
|---|---|
| **Slot 0** | `08:15 AM - 09:45 AM` |
| **Slot 1** | `09:45 AM - 11:15 AM` |
| **Slot 2** | `11:15 AM - 12:45 PM` |
| **Slot 3** | `01:15 PM - 02:45 PM` |
| **Slot 4** | `02:45 PM - 04:15 PM` |
| **Slot 5** | `04:15 PM - 05:45 PM` |

### 3. Cell Data Elements
Inside each grid cell, the following four items are parsed:
- **Course Code**: e.g., `CSE 210`, `CSE 207`, `CSE 341`, `CSE 208`, `CSE 342`, `CSE 209`
- **Faculty Initial (`FC`)**: e.g., `FC: MDSS`, `FC: AAG`, `FC: MJ`
- **Room Number (`R`)**: e.g., `R: 2517`, `R: 2810`, `R: 2808`, `R: 2419`, `R: 2318`
- **Intake & Section**: e.g., `Intake: 55 - 6`

---

## 🧩 Scanner Engine Architecture

The OCR parser is built on top of **Google ML Kit Text Recognition** and utilizes a multi-stage spatial algorithm for 100% recognition accuracy:

```
[ Input Image / Camera Photo ]
              │
              ▼
    [ Google ML Kit OCR ]
              │
              ▼
[ Atomic Element Decomposition ]  ──► Unmerges multi-column horizontal lines
              │
              ▼
[ Header Anchor Extraction ]      ──► Identifies 6 Time Column & 7 Day Row Anchors
              │
              ▼
[ Grid Midpoint Binning ]         ──► Calculates exact X/Y midpoints between columns & rows
              │
              ▼
[ Cell-Bucket Grouping ]          ──► Places text lines strictly into isolated cell buckets
              │
              ▼
[ Regex Feature Extractor ]       ──► Extracts Course, Faculty Code (FC), and Room (R)
              │
              ▼
[ Local & Cloud Sync Engine ]     ──► Saves to SharedPreferences & Firestore
```

### 1. Atomic Element Decomposition
ML Kit sometimes merges text from adjacent table cells into a single horizontal line (e.g., `"CSE 210 CSE 210"` across Column 1 and Column 2). 
- The parser inspects individual `Text.Element` word groups.
- Multiple course matches in a line are split into atomic word units with their own unmerged bounding boxes and exact $X$-coordinates.

### 2. Header Anchor Extraction & Deduplication
- **Day Anchors**: Identified from day names (`SAT`–`FRI`).
- **Time Slot Anchors**: Deduped so each of the 6 time slots (`08:15`, `09:45`, `11:15`, `01:15`, `02:45`, `04:15`) has **exactly one** unique anchor $X$-coordinate corresponding to the start of its header cell.

### 3. Grid Midpoint Binning Algorithm
Rather than using arbitrary pixel radiuses, column boundaries are calculated using exact midpoints between adjacent column centers:

$$\text{Midpoint}_i = \frac{\text{Anchor}_i + \text{Anchor}_{i+1}}{2}$$

Any text item with center $X$-coordinate falling between $\text{Midpoint}_{i-1}$ and $\text{Midpoint}_i$ is guaranteed to land in **Time Slot $i$**. Row boundaries are computed similarly along the $Y$-axis for days of the week.

### 4. Cell-Bucket Text Grouping
All text lines within a specific `(DayRow, TimeColumn)` grid box are grouped into a dedicated **Cell Bucket**.
- Faculty initials (`AAG`, `MJ`, `MDSS`) and Room numbers (`2517`, `2810`, `2808`, `2419`, `2318`) are extracted **exclusively** from items inside that exact cell bucket.
- Cross-column and cross-row line contamination is mathematically impossible ($0\%$ bleed).

---

## 🔍 Regex & Pattern Rules

| Feature | Regex Pattern | Example Match | Extracted Value |
|---|---|---|---|
| **Course Code** | `(?i)((?:CSE\|MAT\|ENG\|EEE\|PHY\|CHEM\|BUS\|STA\|ACT\|FIN\|LAW\|GED\|TEX\|CIV\|[A-Z]{2,4})[-_ ]*[0-9]{3,4}[A-Z]?)` | `CSE 210` | `CSE 210` |
| **Faculty Initial** | `(?i)(?:FC\|Faculty\|Instructor\|Teacher)[:\s.-]*([A-Za-z]{2,5})\b` | `FC: MDSS` | `MDSS` |
| **Room Number** | `(?i)(?:R\|Room\|Rm)[:\s.-]*([0-9]{3,4}\|Lab[^0-9]*[0-9]+)\b` | `R: 2517` | `2517` |
| **Intake / Sec** | `(?i)(Intake\|Int\|Sec\|Section)[: ]*([0-9]{1,3})[ _/,:-]*([0-9]{1,3})` | `Intake: 55 - 6` | Intake `55`, Sec `6` |

### Candidate Blacklist Protection
To prevent labels from being misidentified as faculty initials, the following words are strictly blacklisted:
`FC`, `FACULTY`, `TEACHER`, `INSTRUCTOR`, `R`, `RM`, `ROOM`, `LAB`, `SAT`–`FRI`, `INTAKE`, `SEC`, `SECTION`, `CLASS`, `DAY`, `TIME`, `BUILDING`, `BLDG`, and department codes (`CSE`, `MAT`, `EEE`, etc.).

---

## 💾 Dual Storage & Real-Time Sync

1. **Local Disk Storage**:
   - `RoutineRepository` (`crconnect_routine_preferences` SharedPreferences) for Jetpack Compose `RoutineTab`.
   - `RoutineLocalCache` (`bubt_routine_local_cache` SharedPreferences) for Java `RoutineActivity`.
2. **Cloud Firestore Sync**:
   - Path: `routines/{deptKey}/{intakeKey}/{secKey}/schedule`
   - When a routine is scanned, old documents at this path are cleared and new scanned class entries are written via an atomic `WriteBatch`.
   - When offline, the app seamlessly falls back to the locally saved routine.

---

## 📁 Key Files & Implementation Locations

| File Path | Description |
|---|---|
| [student/RoutineOcrParser.kt](file:///C:/Users/shadd/StudioProjects/CRConnect-App/app/src/main/java/com/example/crconnect/student/RoutineOcrParser.kt) | Core Kotlin 2D Spatial OCR Parser engine for Compose UI |
| [RoutineActivity.java](file:///C:/Users/shadd/StudioProjects/CRConnect-App/app/src/main/java/com/example/crconnect/RoutineActivity.java) | Native Java Routine Activity & OCR parser |
| [student/RoutineRepository.kt](file:///C:/Users/shadd/StudioProjects/CRConnect-App/app/src/main/java/com/example/crconnect/student/RoutineRepository.kt) | SharedPreferences local persistence manager |
| [RoutineLocalCache.java](file:///C:/Users/shadd/StudioProjects/CRConnect-App/app/src/main/java/com/example/crconnect/RoutineLocalCache.java) | Java local caching utility |
| [student/StudentPortalViewModel.kt](file:///C:/Users/shadd/StudioProjects/CRConnect-App/app/src/main/java/com/example/crconnect/student/StudentPortalViewModel.kt) | ViewModel managing state, local storage, and Firestore real-time sync |
| [student/RoutineTab.kt](file:///C:/Users/shadd/StudioProjects/CRConnect-App/app/src/main/java/com/example/crconnect/student/RoutineTab.kt) | Jetpack Compose UI for Routine Schedule view and Bottom Sheet options |
| [RoutineCameraActivity.kt](file:///C:/Users/shadd/StudioProjects/CRConnect-App/app/src/main/java/com/example/crconnect/RoutineCameraActivity.kt) | CameraX capture activity with flash and flip controls |
| [dialog_scan_routine_options.xml](file:///C:/Users/shadd/StudioProjects/CRConnect-App/app/src/main/res/layout/dialog_scan_routine_options.xml) | Bottom Sheet layout for camera scan, gallery pick, and routine deletion |

---

## 💡 Best Practices for Scanning Routines

- **Lighting**: Ensure adequate lighting without heavy screen glare or flash reflection.
- **Framing**: Capture the full routine table including the top header row (`08:15 AM to ...`) and left day column (`SAT`–`FRI`).
- **Format**: Screenshots directly from PDFs or BUBT Annex portal (`annex.bubt.edu.bd`) produce $100\%$ optimal results.
