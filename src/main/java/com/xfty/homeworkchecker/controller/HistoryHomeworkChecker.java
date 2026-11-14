package com.xfty.homeworkchecker.controller;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.xfty.homeworkchecker.Entry;
import com.xfty.homeworkchecker.Idf;
import com.xfty.homeworkchecker.model.CardItem;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.UUID;

/**
 * HistoryHomeworkChecker —— 历史作业查看界面的控制器。
 * <p>
 * 与主界面的可编辑卡片不同，这里只做「只读预览」：
 * <ul>
 *   <li>上方 {@link #editMain} 显示历史作业正文；</li>
 *   <li>下方 {@link #cardList} 逐张显示当天的警示卡片（含图片卡片）；</li>
 *   <li>没有编辑、删除、拖拽等交互按钮。</li>
 * </ul>
 * 卡片数据由历史查询流程以 JSON 形式传入（{@link #setHomeworkData}），
 * 这里自行解析为 {@link CardItem} 后渲染，避免依赖主界面的卡片服务。
 * </p>
 */
public class HistoryHomeworkChecker implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(HistoryHomeworkChecker.class);

    /** 历史作业正文显示区。 */
    @FXML
    private TextArea editMain;

    /** 卡片区域容器，无卡片时整体隐藏。 */
    @FXML
    private VBox cardContainer;

    /** 卡片区域滚动容器。 */
    @FXML
    private ScrollPane cardScrollPane;

    /** 卡片列表容器，逐张存放渲染后的卡片节点。 */
    @FXML
    private VBox cardList;

    /** 卡片基础字号，默认 14，初始化时按用户配置覆盖。 */
    private double cardFontSize = 14;
    /** 卡片字体家族，默认 System，初始化时按用户配置覆盖。 */
    private String cardFontFamily = "System";

    /**
     * FXML 初始化：从用户配置读取字体与字号，应用到正文和卡片。
     * <p>
     * 卡片字号取正文减 1，且最小不低于 11px，避免配置过小时无法阅读。
     * </p>
     */
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        if (Idf.userConfig != null) {
            String fontFamily = Idf.userConfig.getJSONObject("font")
                .getJSONObject("fontFamily")
                .getString("defaultFontFamily");
            int fontSize = Idf.userConfig.getJSONObject("font")
                .getJSONObject("textSize")
                .getInteger("editMain");
            editMain.setFont(new Font(fontFamily, fontSize));
            cardFontFamily = fontFamily;
            cardFontSize = Math.max(fontSize - 1, 11);
        }
    }

    /**
     * 获取历史作业正文控件（供调用方做额外处理，例如只读设置）。
     *
     * @return 正文 TextArea
     */
    public TextArea getEditMain() {
        return editMain;
    }

    /**
     * 填充历史作业数据并渲染卡片。
     *
     * @param content  历史作业正文
     * @param warnings 历史警示卡片 JSON 数组；为空时不显示卡片区域
     */
    public void setHomeworkData(String content, JSONArray warnings) {
        editMain.setText(content);
        if (warnings != null && !warnings.isEmpty()) {
            // 有卡片才让卡片区域参与布局，否则完全隐藏以免占用空间
            cardContainer.setVisible(true);
            cardContainer.setManaged(true);
            renderCards(warnings);
        }
    }

    /**
     * 清空并重新渲染全部历史卡片。
     *
     * @param warnings 卡片 JSON 数组
     */
    private void renderCards(JSONArray warnings) {
        cardList.getChildren().clear();
        for (int i = 0; i < warnings.size(); i++) {
            JSONObject cardObj = warnings.getJSONObject(i);
            CardItem item = parseCardItem(cardObj);
            cardList.getChildren().add(renderCard(item));
        }
    }

    /**
     * 把历史记录中的卡片 JSON 解析为 CardItem。
     * <p>
     * 与主界面解析逻辑保持一致；缺失 id 时生成新 UUID，缺失 imageWidth 时使用默认 200。
     * 未知严重性统一按 INFO 处理。
     * </p>
     *
     * @param cardObj 卡片 JSON 对象
     * @return 解析后的卡片数据
     */
    private CardItem parseCardItem(JSONObject cardObj) {
        CardItem item = new CardItem();
        String id = cardObj.getString("id");
        item.setId(id != null ? id : UUID.randomUUID().toString());
        String sev = cardObj.getString("severity");
        if ("WARNING".equals(sev)) {
            item.setSeverity(CardItem.Severity.WARNING);
        } else if ("CRITICAL".equals(sev)) {
            item.setSeverity(CardItem.Severity.CRITICAL);
        } else {
            item.setSeverity(CardItem.Severity.INFO);
        }
        item.setTitle(cardObj.getString("title"));
        item.setContent(cardObj.getString("content"));
        item.setImagePath(cardObj.getString("imagePath"));
        Double iw = cardObj.getDouble("imageWidth");
        item.setImageWidth(iw != null ? iw : 200.0);
        item.setTimestamp(cardObj.getString("timestamp"));
        item.setCreatedDate(cardObj.getString("createdDate"));
        item.setPersistent(cardObj.getBooleanValue("persistent"));
        return item;
    }

    /**
     * 把一张历史卡片渲染为只读节点。
     * <p>
     * 结构与主界面卡片基本一致（严重性图标 + 标题 + 正文/图片 + 时间戳），
     * 但不包含操作按钮栏、图片宽度滑杆和拖拽事件。
     * </p>
     *
     * @param item 卡片数据
     * @return 可加入 {@link #cardList} 的卡片节点
     */
    private VBox renderCard(CardItem item) {
        // 严重性图标比正文略大
        double iconSize = cardFontSize * 1.3;

        VBox cardRoot = new VBox();
        // 按严重性附加 info/warning/critical 样式，决定卡片配色
        cardRoot.getStyleClass().addAll("card-item", severityStyleClass(item.getSeverity()));
        cardRoot.setFillWidth(true);

        VBox cardBody = new VBox();
        cardBody.getStyleClass().add("card-body");

        // 顶部一行：左侧状态图标，右侧内容
        HBox topRow = new HBox(8);
        topRow.setAlignment(javafx.geometry.Pos.TOP_LEFT);

        ImageView iconView = buildSeverityIcon(item.getSeverity(), iconSize);

        // 内容区：标题 → 正文或图片 → 时间戳
        VBox contentArea = new VBox(3);
        contentArea.getStyleClass().add("card-content-area");
        HBox.setHgrow(contentArea, Priority.ALWAYS);

        Label titleLabel = new Label(item.getTitle());
        titleLabel.getStyleClass().add("card-title");
        titleLabel.setFont(Font.font(cardFontFamily, FontWeight.BOLD, cardFontSize));

        // 正文字号比标题小 2px，最小 11px
        Label contentLabel = new Label(item.getContent());
        contentLabel.getStyleClass().add("card-content");
        contentLabel.setFont(Font.font(cardFontFamily, FontWeight.NORMAL, Math.max(cardFontSize - 2, 11)));

        Label timeLabel = new Label(item.getTimestamp());
        timeLabel.getStyleClass().add("card-timestamp");

        // 时间戳行：时间图标 + 时间文本（持久化卡片再加「常驻」标签）
        HBox timeRow = new HBox(4);
        timeRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        timeRow.getStyleClass().add("card-timestamp-row");

        ImageView timeIcon = buildIcon("icon/card/时间.png", cardFontSize * 0.8);
        timeRow.getChildren().addAll(timeIcon, timeLabel);

        if (item.isPersistent()) {
            timeRow.getStyleClass().add("persistent");
            Label persistentLabel = new Label(
                Idf.userLanguageBundle.getString("card.persistent.label"));
            persistentLabel.getStyleClass().add("card-persistent-label");
            timeRow.getChildren().add(persistentLabel);
        }

        contentArea.getChildren().add(titleLabel);

        // 图片卡片显示图片（按保存的宽度等比缩放），普通卡片显示正文
        String imgPath = item.getImagePath();
        if (imgPath != null && !imgPath.isEmpty()) {
            Image img = loadImageForCard(imgPath);
            if (img != null) {
                ImageView imageView = new ImageView(img);
                imageView.setPreserveRatio(true);
                imageView.setFitWidth(item.getImageWidth());
                imageView.setPickOnBounds(true);
                imageView.getStyleClass().add("card-image");

                VBox imageBox = new VBox(imageView);
                imageBox.getStyleClass().add("card-image-container");
                imageBox.setFillWidth(true);
                contentArea.getChildren().add(imageBox);
            }
        } else {
            contentArea.getChildren().add(contentLabel);
        }

        contentArea.getChildren().add(timeRow);
        topRow.getChildren().addAll(iconView, contentArea);
        cardBody.getChildren().add(topRow);
        cardRoot.getChildren().add(cardBody);

        return cardRoot;
    }

    /**
     * 从磁盘加载历史卡片图片。
     * <p>
     * 路径相对于 {@code ~/homeworkChecker/homeworkDatabase}；
     * 文件不存在或加载失败时返回 null，调用方会跳过图片显示。
     * </p>
     *
     * @param imagePath 相对路径（如 {@code images/uuid.png}）
     * @return JavaFX 图片，失败返回 null
     */
    private Image loadImageForCard(String imagePath) {
        if (imagePath == null || imagePath.isEmpty()) return null;
        try {
            File userDir = FileUtils.getUserDirectory();
            File imageFile = new File(
                new File(new File(userDir, "homeworkChecker"), "homeworkDatabase"),
                imagePath);
            if (!imageFile.exists()) {
                logger.warn("History image file not found: {}", imageFile.getAbsolutePath());
                return null;
            }
            return new Image(imageFile.toURI().toString());
        } catch (Exception e) {
            logger.error("Error loading history image from path: {}", imagePath, e);
            return null;
        }
    }

    /**
     * 根据严重性选择对应的状态图标。
     *
     * @param severity 严重性级别
     * @param size     图标边长（像素）
     * @return 状态图标节点
     */
    private ImageView buildSeverityIcon(CardItem.Severity severity, double size) {
        String iconPath = switch (severity) {
            case WARNING -> "icon/card/警告.png";
            case CRITICAL -> "icon/card/严重.png";
            default -> "icon/card/提示.png";
        };
        return buildIcon(iconPath, size);
    }

    /**
     * 从 classpath 加载图标并设置尺寸。
     *
     * @param path 资源类路径
     * @param size 图标边长（像素）
     * @return 设置好尺寸的 ImageView
     * @throws NullPointerException 当资源不存在时（提前暴露资源缺失问题）
     */
    private ImageView buildIcon(String path, double size) {
        ImageView view = new ImageView(new Image(Objects.requireNonNull(
            Entry.class.getResourceAsStream(path))));
        view.getStyleClass().add("card-icon");
        view.setFitWidth(size);
        view.setFitHeight(size);
        return view;
    }

    /**
     * 把严重性枚举映射为 CSS 样式类名，供卡片配色使用。
     *
     * @param severity 严重性级别
     * @return {@code "warning"}、{@code "critical"} 或 {@code "info"}
     */
    private String severityStyleClass(CardItem.Severity severity) {
        return switch (severity) {
            case WARNING -> "warning";
            case CRITICAL -> "critical";
            default -> "info";
        };
    }
}
