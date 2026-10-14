# CRConnect Application

CRConnect is a modern Android application developed for university class management, class representative elections, academic routines, and student-teacher interactions. The application is built using a dark glassmorphic design system and standard Android UI architecture.

---

## Architecture and Features

- Dark Glassmorphic User Interface: Designed with frosted translucent surfaces, subtle strokes, and consistent typography.
- Edge-to-Edge Display: Full-screen layout integration utilizing modern window insets for system bar compatibility.
- Role-Based Portals:
  - Student Portal: Access to class routines, candidate listings, voting status, and academic profile management.
  - Teacher Portal: Administrative controls for candidate management and election publishing workflows.
- Dynamic Navigation: Tab-driven navigation structure with state-managed backstack handling.

---

## Technology Stack

- Language: Java
- Layout & Design: Material Design 3, ConstraintLayout, Constraint Chains
- Architecture: View Binding, WindowInsets API, Fragment and Activity management
- Backend: Firebase Realtime Database and Authentication
- Development Environment: Android Studio

---

## Project Structure Overview

- Student Dashboard: Handles routine displays, candidate viewing, status tracking, and profile configurations.
- Teacher Control Panel: Provides administrative oversight for election procedures and candidate publishing.
- Adapters and Models: Structured data handling for recycler views and list representations.

---

## Getting Started

### Prerequisites
- Android Studio
- JDK 17 or higher
- Android SDK compatible with API Level 24 or above

### Installation and Setup

1. Clone the repository:
   ```bash
   git clone [https://github.com/shaddhow/CRConnect-App.git](https://github.com/shaddhow/CRConnect-App.git)
