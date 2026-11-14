package com.xfty.homeworkchecker.model;

import java.util.UUID;

/**
 * CardItem —— 提醒卡片模型（纯数据对象）。
 * <p>
 * 表示一张提醒卡片的完整数据，包含严重性、标题、内容、时间戳和持久化标记。
 * 通过 UUID 唯一标识，支持 INFO/WARNING/CRITICAL 三级严重性。
 * </p>
 * <p>
 * 字段语义说明：
 * <ul>
 *   <li>{@link #timestamp}：展示给用户看的日期（{@code yyyy-MM-dd}）；</li>
 *   <li>{@link #createdDate}：判断卡片归属哪一天的内部标记（{@code yyyyMMdd}），
 *       写入时用于决定卡片进入当日数据库还是被顺延；</li>
 *   <li>{@link #persistent}：为 true 时卡片会跨日期保留，并同步到常驻文件；</li>
 *   <li>{@link #imagePath}：非空表示这是一张图片卡片，正文不显示，
 *       图片路径相对于 {@code homeworkDatabase} 目录。</li>
 * </ul>
 * 本类不包含任何 UI 或持久化逻辑，仅作为各层之间传递的数据载体。
 * </p>
 */
public class CardItem {

    /**
     * 卡片严重性级别，决定卡片配色与状态图标。
     */
    public enum Severity {
        /** 信息提示（蓝色） */
        INFO,
        /** 警告提示（黄色） */
        WARNING,
        /** 严重提示（红色） */
        CRITICAL
    }

    /** 卡片唯一标识（UUID），用于跨文件去重。 */
    private String id;
    /** 严重性级别，默认由构造器参数指定。 */
    private Severity severity;
    /** 标题（卡片第一行文本）。 */
    private String title;
    /** 正文内容（普通卡片显示，图片卡片不显示）。 */
    private String content;
    /** 时间戳（yyyy-MM-dd 格式），用于界面展示。 */
    private String timestamp;
    /** 创建日期（yyyyMMdd 格式），用于判断卡片属于哪一天。 */
    private String createdDate;
    /** 是否跨日期持久化保存。 */
    private boolean persistent;
    /** 图片路径（相对 homeworkDatabase 目录），null 表示非图片卡片。 */
    private String imagePath;
    /** 图片显示宽度（像素），默认 200。 */
    private double imageWidth = 200;

    /**
     * 空构造器：自动生成 UUID，其余字段保持默认值。
     * <p>
     * 主要供 JSON 反序列化使用（先创建空对象，再逐字段 set）。
     * </p>
     */
    public CardItem() {
        this.id = UUID.randomUUID().toString();
    }

    /**
     * 全参构造器（不含 createdDate 和 persistent）。
     * <p>
     * 用于创建普通文字卡片；createdDate 由调用方按当前日期另行设置，
     * persistent 默认为 false。
     * </p>
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

    /** 获取卡片唯一标识（UUID）。 */
    public String getId() { return id; }
    /** 设置卡片唯一标识（反序列化时使用）。 */
    public void setId(String id) { this.id = id; }

    /** 获取严重性级别。 */
    public Severity getSeverity() { return severity; }
    /** 设置严重性级别，决定卡片配色与图标。 */
    public void setSeverity(Severity severity) { this.severity = severity; }

    /** 获取标题。 */
    public String getTitle() { return title; }
    /** 设置标题。 */
    public void setTitle(String title) { this.title = title; }

    /** 获取正文内容。 */
    public String getContent() { return content; }
    /** 设置正文内容。 */
    public void setContent(String content) { this.content = content; }

    /** 获取展示用时间戳（yyyy-MM-dd）。 */
    public String getTimestamp() { return timestamp; }
    /** 设置展示用时间戳（yyyy-MM-dd）。 */
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    /** 获取创建日期（yyyyMMdd），用于判断卡片归属哪一天。 */
    public String getCreatedDate() { return createdDate; }
    /** 设置创建日期（yyyyMMdd）。 */
    public void setCreatedDate(String createdDate) { this.createdDate = createdDate; }

    /** 是否为跨日期持久化卡片。 */
    public boolean isPersistent() { return persistent; }
    /** 设置是否跨日期持久化。 */
    public void setPersistent(boolean persistent) { this.persistent = persistent; }

    /** 获取图片相对路径；null 或空串表示普通文字卡片。 */
    public String getImagePath() { return imagePath; }
    /** 设置图片相对路径。 */
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    /** 获取图片显示宽度（像素）。 */
    public double getImageWidth() { return imageWidth; }
    /** 设置图片显示宽度（像素）。 */
    public void setImageWidth(double imageWidth) { this.imageWidth = imageWidth; }
}
