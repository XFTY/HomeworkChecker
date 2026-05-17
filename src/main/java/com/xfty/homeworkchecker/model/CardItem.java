package com.xfty.homeworkchecker.model;

import java.util.UUID;

public class CardItem {
    public enum Severity {
        INFO, WARNING, CRITICAL
    }

    private String id;
    private Severity severity;
    private String title;
    private String content;
    private String timestamp;
    private String createdDate;
    private boolean persistent;

    public CardItem() {
        this.id = UUID.randomUUID().toString();
    }

    public CardItem(Severity severity, String title, String content, String timestamp) {
        this.id = UUID.randomUUID().toString();
        this.severity = severity;
        this.title = title;
        this.content = content;
        this.timestamp = timestamp;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Severity getSeverity() { return severity; }
    public void setSeverity(Severity severity) { this.severity = severity; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public String getCreatedDate() { return createdDate; }
    public void setCreatedDate(String createdDate) { this.createdDate = createdDate; }

    public boolean isPersistent() { return persistent; }
    public void setPersistent(boolean persistent) { this.persistent = persistent; }
}
