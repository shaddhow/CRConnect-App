package com.example.crconnect;

public class Candidate {
    private String id;
    private String name;
    private int votes;
    private String dept;
    private String imageUrl;
    private String section;
    private String manifesto;

    public Candidate() {}

    public Candidate(String id, String name, int votes, String dept, String imageUrl, String section, String manifesto) {
        this.id = id;
        this.name = name;
        this.votes = votes;
        this.dept = dept;
        this.imageUrl = imageUrl;
        this.section = section;
        this.manifesto = manifesto;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getVotes() {
        return votes;
    }

    public void setVotes(int votes) {
        this.votes = votes;
    }

    public String getDept() {
        return dept;
    }

    public void setDept(String dept) {
        this.dept = dept;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public String getManifesto() {
        return manifesto;
    }

    public void setManifesto(String manifesto) {
        this.manifesto = manifesto;
    }
}
