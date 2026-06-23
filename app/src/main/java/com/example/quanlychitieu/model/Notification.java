package com.example.quanlychitieu.model;

import java.io.Serializable;

public class Notification implements Serializable {
    private String id;
    private String title;
    private String content;
    private boolean isRead;
    private String date;
    private long timestamp;

    public Notification() {}

    public Notification(String id, String title, String content, boolean isRead, String date, long timestamp) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.isRead = isRead;
        this.date = date;
        this.timestamp = timestamp;
    }
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getDate() { return date; }
    public long getTimestamp() { return timestamp; }
    public boolean getIsRead() {return isRead;}
    public void setIsRead(boolean isRead) {this.isRead = isRead;}
}
