# UI/UX Redesign Specification: From Image 1 to Image 2

This document details the layout adjustments, component updates, and code implementations needed to transform the routine dashboard to match Image 2's polished design.

---

## 1. Top "UP NEXT" Hero Card (Design 3)
- **Background & Shape:** Dark navy card (`#121622`) with 20dp rounded corners, purple stroke (`#7C3AED`), and a subtle circular background vector overlay.
- **Top Bar:** Left indicator dot (`#7C3AED`) + "UP NEXT" text badge; Right side "Starts in 10h 51m" pill container (`#1E293B`).
- **Left Side:** Bold course title (`CSE 208`, 22sp white), Faculty (`FC: AAG`, 13sp gray), and exact time range (`11:15 AM – 12:45 PM`).
- **Right Side:** Vertical white rounded card displaying `"ROOM"` in small uppercase text above bold black room code (`2517` or `517`).

---

## 2. Week Day Selector
- **Header Subtitle:** Display `"This week • 10–16 Oct"` centered between left (`‹`) and right (`›`) arrow navigation buttons.
- **Day Cards:** Active selected day highlighted with solid purple background (`#A78BFA`) and dark text (`#0F172A`).
- **Indicator Dots:** Subtle orange/coral indicator dot (`#FB923C`) under days that have scheduled classes.

---

## 3. Today's Timeline & Class Cards
- **Header:** `"Today"` with class summary text (`4 classes • 11:15 AM – 5:45 PM`).
- **Class Cards:** Dark blue container (`#121622`) with left accent border (`#7C3AED`).
- **Bottom Pill Badges:**
  - *Badge 1:* Room Icon + Room Number (`2517`).
  - *Badge 2:* Clock Icon + Class Duration (`1h 30m`).
- **Timeline Breaks:** Clean horizontal divider between classes showing break duration (`☕ 30 min break`).

---

## 4. Floating Bottom Navigation Bar
- Floating dark glassmorphism bottom bar (`#121622` with elevation) containing 4 tabs:
  1. **Today** (Calendar Icon)
  2. **Week** (Grid Icon)
  3. **Exams** (Checklist Icon)
  4. **Faculty** (People Icon)
