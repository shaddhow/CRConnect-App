package com.example.crconnect;

import java.util.Locale;

public class StudentItem {
    private String id;
    private String name;
    private String email;
    private String intake;
    private String section;
    private String dept;
    private String profileImageUrl;
    private boolean hasVoted;
    private boolean isCr;

    public StudentItem() {
    }

    public StudentItem(String id, String name, String email, String intake, String section, String dept, String profileImageUrl, boolean hasVoted, boolean isCr) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.intake = intake;
        this.section = section;
        this.dept = dept;
        this.profileImageUrl = profileImageUrl;
        this.hasVoted = hasVoted;
        this.isCr = isCr;
    }

    public String getId() {
        return id != null ? id : "";
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name != null && !name.isEmpty() ? name : "Student";
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email != null ? email : "";
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getIntake() {
        return intake != null ? intake : "--";
    }

    public void setIntake(String intake) {
        this.intake = intake;
    }

    public String getSection() {
        return section != null ? section : "--";
    }

    public void setSection(String section) {
        this.section = section;
    }

    public String getDept() {
        return dept != null ? dept : "CSE";
    }

    public void setDept(String dept) {
        this.dept = dept;
    }

    public String getProfileImageUrl() {
        return profileImageUrl != null ? profileImageUrl : "";
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public boolean isHasVoted() {
        return hasVoted;
    }

    public void setHasVoted(boolean hasVoted) {
        this.hasVoted = hasVoted;
    }

    public boolean isCr() {
        return isCr;
    }

    public void setCr(boolean cr) {
        isCr = cr;
    }

    public String getInitials() {
        String cleanName = getName().trim();
        if (cleanName.isEmpty()) return "ST";
        String[] parts = cleanName.split("\\s+");
        if (parts.length >= 2) {
            return ("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase(Locale.getDefault());
        } else if (parts.length == 1 && parts[0].length() >= 2) {
            return parts[0].substring(0, 2).toUpperCase(Locale.getDefault());
        } else if (parts.length == 1 && parts[0].length() == 1) {
            return parts[0].toUpperCase(Locale.getDefault());
        }
        return "ST";
    }
}
