# CRConnect Application

CRConnect is a specialized, production-ready native Android application built for Bangladesh University of Business and Technology (BUBT). It digitizes university class representative (CR) elections, routines, and academic management, replacing manual paper-based processes with an automated, role-tailored platform for Students and Faculty.

---

## Key System Features

### Authentication & Domain Enforcement
- Academic Domain Verification: Restricts sign-ups strictly to institutional domains (`@bubt.edu.bd` and `@cse.bubt.edu.bd`).
- Verification Gate: Enforces Firebase email verification protocols prior to granting dashboard authorization.
- Dual Data Syncing: Dual persistence layer syncing user credentials across Firebase Realtime Database and Cloud Firestore.

### Student Voting & Academic Portal
- Live Leaderboard: Real-time candidate ranking ordered by vote count with live dynamic progress indicators.
- Single-Ballot Protection: Transaction-level voter checks enforcing strict one-student-one-vote mechanics.
- Master Voting Gate: Automatically validates administrative election toggles before permitting ballot submission.
- Candidate Manifestos: Bottom sheet interactive views displaying candidate vision statements and profiles.

### Teacher & Administrative Console
- Master Election Switch: Global toggle to open, pause, or lock voting operations system-wide in real time.
- Lifecycle Management: Modules to onboard, publish, or remove candidates with safety confirmations.
- Election Analytics: Real-time metric tracking for total votes cast, unique voter count, and turnout percentages.
- Run-Off & Emergency Workflows: Tie-breaker mechanism to reset vote metrics while maintaining registered candidates, complete with full reset confirmation protection.

### Document Generation & Reporting
- Native A4 PDF Engine: Custom canvas rendering for official A4 result sheets generated directly on-device using Android Graphics APIs.
- Certified Reports: Includes official institution branding, timestamping, candidate rank breakdowns, and turnout percentage shares.
- Direct Sharing & Printing: Native integration with Android PrintManager and secure file sharing via FileProvider URIs.

### Infrastructure & Offline Support
- Local Persistence: Disk persistence enabled for uninterrupted functionality during network drops.
- FCM Push Notifications: High-priority broadcast notification channels for instant university-wide announcements.

---

## Technical Stack

- Architecture: Native Android Architecture, View Binding, Dynamic Insets API
- Programming Language: Java
- UI/UX Guidelines: Material Design 3, Dark Glassmorphism, Constraint Layout Chains
- Backend Services: Firebase Realtime Database, Cloud Firestore, Firebase Authentication
- Storage & Utilities: Glide, Base64 Encoding, Native PDF Print Framework

---

## Project Structure

* `activities` - Core Portal Activities (Student & Teacher Dashboards)
* `adapters` - RecyclerView Adapters for Candidates, Routines, and Statuses
* `models` - Data Models for Users, Candidates, Votes, and Logs
* `pdf` - Native Canvas PDF Generator Implementation
* `services` - Firebase Messaging Service & Background Receivers
* `utils` - WindowInsets, SessionManager, and UI Utilities

---

## Getting Started

### Prerequisites
- Android Studio
- JDK 17 or higher
- Physical or Virtual Android Device (API Level 24+)

### Installation

1. Clone the repository:
   ```bash
   git clone [https://github.com/shaddhow/CRConnect-App.git](https://github.com/shaddhow/CRConnect-App.git)
