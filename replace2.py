import os

fp = "C:/Users/shadd/StudioProjects/CRConnect-App/app/src/main/java/com/example/crconnect/StudentDashboardActivity.java"
with open(fp, "r", encoding="utf-8") as f:
    content = f.read()

replacements = {
    "Color.parseColor(\"#38BDF8\")": "Color.parseColor(\"#0066FF\")",
    "Color.parseColor(\"#8B5CF6\")": "Color.parseColor(\"#7C3AED\")",
    "Color.parseColor(\"#10B981\")": "Color.parseColor(\"#16A34A\")",
    "Color.parseColor(\"#F59E0B\")": "Color.parseColor(\"#F59E0B\")",
    "Color.parseColor(\"#F43F5E\")": "Color.parseColor(\"#EC4899\")",
    "Color.parseColor(\"#6366F1\")": "Color.parseColor(\"#06B6D4\")",
    "Color.parseColor(\"#171A29\")": "Color.parseColor(\"#FFFFFF\")",
    "Color.parseColor(\"#F8FAFC\")": "Color.parseColor(\"#0F172A\")",
    "Color.parseColor(\"#CBD5E1\")": "Color.parseColor(\"#64748B\")",
    "Color.parseColor(\"#94A3B8\")": "Color.parseColor(\"#64748B\")",
    "Color.parseColor(\"#EF4444\")": "Color.parseColor(\"#DC2626\")",
    "Color.parseColor(\"#2A2E3D\")": "Color.parseColor(\"#E2E8F0\")",
    "Color.parseColor(\"#334155\")": "Color.parseColor(\"#E2E8F0\")"
}

for k, v in replacements.items():
    content = content.replace(k, v)

with open(fp, "w", encoding="utf-8") as f:
    f.write(content)

print("Java colors replaced")