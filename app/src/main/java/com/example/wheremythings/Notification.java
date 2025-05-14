package com.example.wheremythings;

public class Notification {
    private long timestamp;
    private String matchedReportId;
    private String matchedReportType;

    public Notification() {
        // Default constructor required for calls to DataSnapshot.getValue(Notification.class)
    }

    public Notification(long timestamp, String matchedReportId, String matchedReportType) {
        this.timestamp = timestamp;
        this.matchedReportId = matchedReportId;
        this.matchedReportType = matchedReportType;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getMatchedReportId() {
        return matchedReportId;
    }

    public String getMatchedReportType() {
        return matchedReportType;
    }
}