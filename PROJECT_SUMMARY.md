# Project Summary: CRConnect

## 1. Overview & Tech Stack
- **Overview:** CRConnect is an Android application designed for Class Representatives (CRs), students, and teachers to manage class activities, attendance tracking, announcements, and live sessions/elections.
- **Tech Stack:**
  - **Language:** Java (Android SDK, Min SDK 24+, Target SDK 34+)
  - **UI Toolkit:** Android XML Views, Material Design 3, ConstraintLayout, Custom Glassmorphism Drawables & Animations
  - **Backend & Database:** Firebase Authentication, Cloud Firestore, Firebase Cloud Messaging (FCM)
  - **Local Persistence:** SharedPreferences via `SessionManager`
  - **Reporting:** Android `PdfDocument` for PDF report generation

---

## 2. App Modules & Key Features Implemented
- **Authentication & Onboarding:**
  - Email/Password login and registration with role-based routing (Student vs. Teacher/Admin).
  - Session persistence (`SessionManager`) and auto-login validation.
- **Student Dashboard (`StudentDashboardActivity`):**
  - View personal and academic profile (ID, Department, Section, Intake).
  - Attendance marking and real-time status updates.
  - Live announcements and notifications via FCM.
- **Teacher / Admin Dashboard (`TeacherDashboardActivity`):**
  - Session control center (Start, Pause, Stop live sessions/voting).
  - Real-time candidate and student attendance tracking.
  - Broadcast notifications and alerts to students.
- **PDF Report Generation (`PdfReportGenerator`):**
  - Generate and export formatted attendance and session summary PDFs.
- **Utilities & Notifications:**
  - Haptic feedback integration (`HapticUtils`) and custom push notification service (`MyFirebaseMessagingService`).

---

## 3. Database Schema / Firestore Models
- **`users` Collection:**
  - Document ID: User UID / Sanitized Email / Student ID
  - Fields:
    - `name` (String)
    - `email` (String)
    - `id` (String)
    - `role` (String: "Student" or "Teacher/Admin")
    - `department` / `dept` (String)
    - `section` (String)
    - `assignedSection` (String)
    - `intake` (String)
    - `profileImageUrl` (String / Base64)

---

## 4. File Structure Overview
```
app/src/main/
├── AndroidManifest.xml
├── java/com/example/crconnect/
│   ├── App.java
│   ├── MainActivity.java
│   ├── RegisterActivity.java
│   ├── StudentDashboardActivity.java
│   ├── TeacherDashboardActivity.java
│   ├── PdfReportGenerator.java
│   ├── SessionManager.java
│   ├── MyFirebaseMessagingService.java
│   └── core/utils/HapticUtils.java
└── res/
    ├── anim/ (fade and slide animations)
    ├── color/ (selectors and states)
    ├── drawable/ (glass cards, gradients, icons)
    ├── layout/ (activity main, dashboards, items, dialogs)
    ├── menu/ (bottom navigation and drawer menus)
    └── values/ (themes, colors, strings)
```

---

## 5. Pending / Upcoming Features
- Integration with advanced analytics graphs.
- Multi-language support (Localization).
- Enhanced offline mode with Room database caching.
- Biometric authentication login support.
