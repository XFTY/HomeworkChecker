package com.xfty.homeworkchecker.model;

import java.util.UUID;

/**
 * CardItem — 提醒卡片模型
 * <p>
 * 表示一张提醒卡片的完整数据，包含严重性、标题、内容、时间戳和持久化标记。
 * 通过 UUID 唯一标识，支持 INFO/WARNING/CRITICAL 三级严重性。
 * </p>
 */
public class CardItem {

    /**
     * 卡片严重性级别
     */
    public enum Severity {
        /** 信息提示（蓝色） */
        INFO,
        /** 警告提示（黄色） */
        WARNING,
        /** 严重提示（红色） */
        CRITICAL
    }

    /** 卡片唯一标识（UUID） */
    private String id;
    /** 严重性级别 */
    private Severity severity;
    /** 标题 */
    private String title;
    /** 正文内容 */
    private String content;
    /** 时间戳（yyyy-MM-dd 格式） */
    private String timestamp;
    /** 创建日期（yyyyMMdd 格式） */
    private String createdDate;
    /** 是否跨日期持久化保存 */
    private boolean persistent;
    /** 图片路径（相对 homeworkDatabase 目录），null 表示非图片卡片 */
    private String imagePath;
    /** 图片显示宽度（像素），默认 200 */
    private double imageWidth = 200;

    /** 空构造器，自动生成 UUID */
    public CardItem() {
        this.id = UUID.randomUUID().toString();
    }

    /**
     * 全参构造器（不含 createdDate 和 persistent）
     *
     * @param severity  严重性
     * @param title     标题
     * @param content   内容
     * @param timestamp 时间戳
     */
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

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    public double getImageWidth() { return imageWidth; }
    public void setImageWidth(double imageWidth) { this.imageWidth = imageWidth; }
}
