# CRConnect Firebase Cloud Functions (Background Push Notifications)

This directory contains the Firebase Cloud Function script that automatically triggers and dispatches high-priority **FCM Push Notifications** to students' devices whenever a CR or Teacher posts a new announcement in Firebase Realtime Database.

---

## How It Works

1. **Auto Trigger:** When a notice is written to `/voting_controls/{dept}/{sec}/announcements/{noticeId}`.
2. **Topic Routing:** Dispatches the notification payload directly to the section's topic: `cr_notice_{sanitizedDept}_{sanitizedSec}` (e.g. `cr_notice_CSE_55_A`).
3. **Delivery:** Google Play Services delivers the notification to all students in that section even if their app is completely closed or device screen is locked.

---

## 1-Minute Deployment Instructions

### Prerequisites
1. Install [Node.js](https://nodejs.org/) (version 18 or 20 recommended).
2. Install the Firebase CLI:
   ```bash
   npm install -g firebase-tools
   ```
3. Log in to your Firebase account:
   ```bash
   firebase login
   ```

### Deploying the Functions
1. Open a terminal in this `firebase_functions` directory:
   ```bash
   cd firebase_functions
   ```
2. Initialize or link your project (if not linked):
   ```bash
   firebase use --add
   # Select your Firebase project: crconnect-58521
   ```
3. Deploy the function:
   ```bash
   firebase deploy --only functions
   ```
Done! Any notice posted by a CR will now instantly trigger push notifications to all students in that section.
