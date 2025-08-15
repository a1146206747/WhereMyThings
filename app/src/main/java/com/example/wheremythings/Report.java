package com.example.wheremythings;

import java.util.HashMap;

public class Report {
    private String id;
    private String uid;
    private String predictedClass;
    private String reportType;
    private String location;
    private String description;
    private String imageUrl;
    private long timestamp;
    private HashMap<String, Float> embedding;
    private HashMap<String, Float> color;

    private String nlpCategory;
    private String nlpColor;
    private String nlpLocation;

    public Report() {
        // Required for calls to DataSnapshot.getValue(Report.class)
    }

    public Report(String id, String uid, String predictedClass, String reportType, String location,
                  String description, String imageUrl, long timestamp, HashMap<String, Float> embedding) {
        this.id = id;
        this.uid = uid;
        this.predictedClass = predictedClass;
        this.reportType = reportType;
        this.location = location;
        this.description = description;
        this.imageUrl = imageUrl;
        this.timestamp = timestamp;
        this.embedding = embedding;
    }

    // Getters and setters for core fields
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getPredictedClass() { return predictedClass; }
    public void setPredictedClass(String predictedClass) { this.predictedClass = predictedClass; }

    public String getReportType() { return reportType; }
    public void setReportType(String reportType) { this.reportType = reportType; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public HashMap<String, Float> getEmbedding() { return embedding; }
    public void setEmbedding(HashMap<String, Float> embedding) { this.embedding = embedding; }

    public HashMap<String, Float> getColor() { return color; }
    public void setColor(HashMap<String, Float> color) { this.color = color; }

    // Getters and setters for NLP attributes

    public String getNlpCategory() { return nlpCategory; }

    public void setNlpCategory(String nlpCategory) { this.nlpCategory = nlpCategory; }

    public String getNlpColor() { return nlpColor; }

    public void setNlpColor(String nlpColor) { this.nlpColor = nlpColor; }

    public String getNlpLocation() { return nlpLocation; }

    public void setNlpLocation(String nlpLocation) { this.nlpLocation = nlpLocation; }
}