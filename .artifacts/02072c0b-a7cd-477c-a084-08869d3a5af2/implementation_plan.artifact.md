# Candidate Management System Implementation Plan

This plan addresses three tasks: building a candidate push feature, upgrading it to a production-grade full-stack system, and refining the Android application with Firebase sync and updated UI.

## User Review Required

> [!IMPORTANT]
> The request combines **Full-Stack Web** (React, Node.js, MongoDB) and **Android** (Java, Firebase) requirements. I will provide the Web code as modular files for reference and implement the Android refinements directly in the project.

> [!TIP]
> For the "Eye-Catching" UI, I will use a modern vibrant blue gradient and Material 3 components to ensure the app looks professional and appealing.

## Proposed Changes

### Android Application (UI & Logic)

#### [MODIFY] [activity_main.xml](file:///C:/Users/snaja/AndroidStudioProjects/CRConnect/app/src/main/res/layout/activity_main.xml)
- Update the home screen to display the **App Logo** and **BUBT Logo** side-by-side.
- Apply the updated eye-catching background gradient.

#### [MODIFY] [bg_gradient.xml](file:///C:/Users/snaja/AndroidStudioProjects/CRConnect/app/src/main/res/drawable/bg_gradient.xml)
- Update the gradient colors to be more vibrant (e.g., Deep Blue to Electric Blue).

#### [MODIFY] [TeacherDashboardActivity.java](file:///C:/Users/snaja/AndroidStudioProjects/CRConnect/app/src/main/java/com/example/crconnect/TeacherDashboardActivity.java)
- Ensure the "Auto-Flow" (clear input, focus retention) is seamless.
- Confirm the scrollable list at the bottom correctly displays candidates and vote counts in real-time.

---

### Full-Stack Web Reference (Modular Backend & Frontend)

#### [NEW] `backend/server.js`
- Express server setup with Socket.io and JWT middleware.
#### [NEW] `backend/models/Candidate.js`
- Mongoose schema for MongoDB.
#### [NEW] `frontend/AdminConsole.jsx`
- React component with focus retention and real-time updates via Socket.io.

## Verification Plan

### Automated Tests
- N/A (Manual UI verification preferred for these tasks).

### Manual Verification
- **Logos**: Check `MainActivity` layout to ensure logos are side-by-side.
- **Auto-Flow**: In `TeacherDashboardActivity`, type a name, press Enter/Click Push, and verify the field clears and cursor stays focused.
- **Sync**: Verify that adding a candidate in the Admin Console immediately updates the Student Dashboard list.
- **Visuals**: Review the color scheme for an "eye-catching" effect.
