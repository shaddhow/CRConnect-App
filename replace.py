import os

files_to_process = [
    "C:/Users/shadd/StudioProjects/CRConnect-App/app/src/main/res/layout/activity_student_dashboard.xml",
    "C:/Users/shadd/StudioProjects/CRConnect-App/app/src/main/res/layout/layout_profile.xml"
]

replacements = {
    "@drawable/bg_glassy_dark": "@drawable/bg_gradient",
    "app:cardBackgroundColor=\"#121622\"": "app:cardBackgroundColor=\"#FFFFFF\"",
    "app:cardBackgroundColor=\"@color/surface\"": "app:cardBackgroundColor=\"#FFFFFF\"",
    "app:cardBackgroundColor=\"@color/glass_card\"": "app:cardBackgroundColor=\"#FFFFFF\"",
    "app:strokeColor=\"#30FFFFFF\"": "app:strokeColor=\"#E2E8F0\"",
    "app:strokeColor=\"@color/card_stroke\"": "app:strokeColor=\"#E2E8F0\"",
    "android:textColor=\"#FFFFFF\"": "android:textColor=\"#0F172A\"",
    "android:textColor=\"#9CA3AF\"": "android:textColor=\"#64748B\"",
    "android:textColor=\"#94A3B8\"": "android:textColor=\"#64748B\"",
    "android:background=\"@color/surface\"": "android:background=\"#FFFFFF\"",
    "android:background=\"@drawable/glass_card_bg\"": "android:background=\"#F8FAFC\"",
    "android:textColor=\"#38BDF8\"": "android:textColor=\"#0066FF\"",
    "app:strokeColor=\"#38BDF8\"": "app:strokeColor=\"#0066FF\"",
    "android:textColor=\"#A78BFA\"": "android:textColor=\"#0066FF\"",
    "app:strokeColor=\"#A78BFA\"": "app:strokeColor=\"#0066FF\"",
    "android:background=\"@color/input_bg\"": "android:background=\"#F1F5F9\"",
    "@color/bottom_nav_item_color": "@color/primary"
}

for fp in files_to_process:
    with open(fp, "r", encoding="utf-8") as f:
        content = f.read()

    for k, v in replacements.items():
        content = content.replace(k, v)

    with open(fp, "w", encoding="utf-8") as f:
        f.write(content)

print("Replaced colors for light theme")