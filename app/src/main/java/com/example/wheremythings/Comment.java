package com.example.wheremythings;

import java.util.HashMap;

public class Comment {
    private String id;
    private String userId;
    private String username;
    private String text;
    private long timestamp;
    private HashMap<String, Reply> replies; // 巢狀留言

    public Comment() {}

    public String getUsername() {
        return username;
    }
    public Comment(String id, String userId, String username, String text, long timestamp) {
        this.id = id;
        this.userId = userId;
        this.username = username;
        this.text = text;
        this.timestamp = timestamp;
        this.replies = new HashMap<>();
    }


    public HashMap<String, Reply> getReplies() {
        return replies;
    }

    public void setReplies(HashMap<String, Reply> replies) {
        this.replies = replies;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
