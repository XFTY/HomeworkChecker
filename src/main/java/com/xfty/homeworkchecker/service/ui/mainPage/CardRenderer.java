package com.xfty.homeworkchecker.service.ui.mainPage;

import com.alibaba.fastjson2.JSONObject;
import com.xfty.homeworkchecker.Entry;
import com.xfty.homeworkchecker.Idf;
import com.xfty.homeworkchecker.model.CardItem;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * CardRenderer —— 提醒卡片的界面构建器。
 * <p>
 * 该类只负责「把数据画成界面」，不负责持久化和列表状态管理：
 * <ul>
 *   <li>{@link #renderCard}：把一张 {@link CardItem} 渲染成只读卡片节点；</li>
 *   <li>{@link #createEditCardBody}：把一张卡片渲染成内联编辑控件；</li>
 *   <li>{@link #showCardSaveConfirmDialog}：编辑中切换场景时弹出的保存/丢弃确认框；</li>
 *   <li>以及若干图标、按钮、严重性样式的小型工厂方法。</li>
 * </ul>
 * 持久化操作（读卡、改卡、删图）通过构造时注入的 {@link ReminderCardService} 完成；
 * 弹窗展示通过 {@link PopupService} 完成，因此本类不直接操作窗口。
 * </p>
 */
public class CardRenderer {

    private static final Logger logger = LoggerFactory.getLogger(CardRenderer.class);

    /** 卡片根节点上保存按钮栏引用的属性键，供锁定状态切换时播放展开/收起动画。 */
    public static final String BUTTON_BAR_PROPERTY = "cardButtonBar";
    /** 卡片根节点上保存「编辑」按钮引用的属性键，供编辑期间对其他卡片的编辑按钮做渐隐/渐显。 */
    public static final String EDIT_BUTTON_PROPERTY = "cardEditButton";

    /** 卡片数据与图片读写服务，用于读取图片、保存图片宽度等。 */
    private final ReminderCardService reminderCardService;
    /** 弹窗服务，用于展示保存确认框。 */
    private final PopupService popupService;
    /** 主显示区容器，弹窗关闭时需要清除其高斯模糊效果。 */
    private final AnchorPane showingMainArea;
    /** 中央弹窗内容容器，弹窗关闭时需要清空其子节点。 */
    private final AnchorPane centerShowingArea;
    /** 弹窗外层容器，弹窗关闭时恢复其鼠标事件响应。 */
    private final VBox centerOutBox;
    /** 弹窗背后的黑色遮罩，关闭弹窗时将其透明度归零。 */
    private final Pane blackPane;

    /**
     * 构造卡片渲染器。
     *
     * @param reminderCardService 卡片数据与图片读写服务
     * @param popupService        弹窗服务
     * @param showingMainArea     主显示区容器（用于清除模糊效果）
     * @param centerShowingArea   中央弹窗内容容器（用于清空弹窗内容）
     * @param centerOutBox        弹窗外层容器（用于恢复鼠标事件响应）
     * @param blackPane           弹窗背后的黑色遮罩
     */
    public CardRenderer(ReminderCardService reminderCardService, PopupService popupService,
                        AnchorPane showingMainArea, AnchorPane centerShowingArea,
                        VBox centerOutBox, Pane blackPane) {
        this.reminderCardService = reminderCardService;
        this.popupService = popupService;
        this.showingMainArea = showingMainArea;
        this.centerShowingArea = centerShowingArea;
        this.centerOutBox = centerOutBox;
        this.blackPane = blackPane;
    }

    /**
     * 把一张卡片渲染成只读的卡片节点（预览态，不含输入框）。
     * <p>
     * 布局结构：
     * <pre>
     * card-item（根节点，按严重性附加 info/warning/critical 样式）
     *   └─ card-body
     *        ├─ topRow（严重性图标 + 内容区）
     *        ├─ sliderBox（仅图片卡片：图片宽度调节滑杆）
     *        └─ buttonBar（编辑/删除按钮，锁定状态下隐藏）
     * </pre>
     * 图片卡片不显示正文，只显示图片；普通卡片显示标题、正文和时间戳。
     * </p>
     *
     * @param item              卡片数据
     * @param index             卡片在列表中的下标，点击按钮时回传给回调
     * @param cardFontFamily    卡片字体家族
     * @param cardFontSize      卡片基础字号（标题字号）
     * @param onEditMainClicked 锁定状态下点击卡片的回调（通常进入主编辑区）
     * @param onEdit            点击「编辑」按钮的回调，参数为卡片下标
     * @param onDelete          点击「删除」按钮的回调，参数为卡片下标
     * @param dragHandler       拖拽处理器；为 null 时不启用拖拽
     * @return 可直接加入卡片列表的根节点
     */
    public VBox renderCard(CardItem item, int index,
                           String cardFontFamily, double cardFontSize,
                           Runnable onEditMainClicked,
                           java.util.function.Consumer<Integer> onEdit,
                           java.util.function.Consumer<Integer> onDelete,
                           CardDragHandler dragHandler) {
        // 严重性图标比正文略大，视觉上作为卡片的「状态标识」
        double iconSize = cardFontSize * 1.3;

        VBox cardRoot = new VBox();
        // 同时附加通用卡片样式和严重性样式（info/warning/critical 决定配色）
        cardRoot.getStyleClass().addAll("card-item", severityStyleClass(item.getSeverity()));
        // 锁定状态下追加 locked 样式类，使卡片悬停时保持原样（不高亮）
        if (!Idf.isEditable) {
            cardRoot.getStyleClass().add("locked");
        }
        cardRoot.setFillWidth(true);

        // 用局部变量捕获下标，供各按钮的事件闭包使用（避免后续 index 变化影响闭包）
        int currentIndex = index;

        VBox cardBody = new VBox();
        cardBody.getStyleClass().add("card-body");

        // 顶部一行：左侧严重性图标，右侧标题/正文/时间戳
        HBox topRow = new HBox(8);
        topRow.setAlignment(Pos.TOP_LEFT);

        ImageView iconView = buildSeverityIcon(item.getSeverity(), iconSize);

        // 内容区纵向排列：标题 → 正文或图片 → 时间戳行
        VBox contentArea = new VBox(3);
        contentArea.getStyleClass().add("card-content-area");
        // 内容区占据 topRow 的剩余宽度，保证长文本可以撑满卡片
        HBox.setHgrow(contentArea, Priority.ALWAYS);

        Label titleLabel = new Label(item.getTitle());
        titleLabel.getStyleClass().add("card-title");
        titleLabel.setFont(Font.font(cardFontFamily, FontWeight.BOLD, cardFontSize));

        // 正文字号比标题小 2px，但最小不低于 11px，避免缩小时无法阅读
        Label contentLabel = new Label(item.getContent());
        contentLabel.getStyleClass().add("card-content");
        contentLabel.setFont(Font.font(cardFontFamily, FontWeight.NORMAL, Math.max(cardFontSize - 2, 11)));

        // 时间戳显示卡片创建时间（yyyy-MM-dd）
        Label timeLabel = new Label(item.getTimestamp());
        timeLabel.getStyleClass().add("card-timestamp");

        // 时间戳行：时间图标 + 时间文本，持久化卡片再追加「常驻」标签
        HBox timeRow = new HBox(4);
        timeRow.setAlignment(Pos.CENTER_LEFT);
        timeRow.getStyleClass().add("card-timestamp-row");

        ImageView timeIcon = buildIcon("icon/card/时间.png", cardFontSize * 0.8);
        timeRow.getChildren().addAll(timeIcon, timeLabel);

        // persistent 表示该卡片跨日期保留，界面上用额外标签提示用户
        if (item.isPersistent()) {
            timeRow.getStyleClass().add("persistent");
            String persistentText = Idf.userLanguageBundle.getString("card.persistent.label");
            Label persistentLabel = new Label(persistentText);
            persistentLabel.getStyleClass().add("card-persistent-label");
            timeRow.getChildren().add(persistentLabel);
        }

        contentArea.getChildren().add(titleLabel);

        // sliderControls 非空表示这是一张图片卡片，需要在按钮栏上方插入图片宽度滑杆
        Node sliderControls = null;
        String imgPath = item.getImagePath();
        if (imgPath != null && !imgPath.isEmpty()) {
            // 图片卡片：加载磁盘图片，显示图片本身
            Image img = reminderCardService.loadImageForCard(imgPath);
            if (img != null) {
                // 使用卡片保存的显示宽度，并保持等比缩放，避免图片变形
                double savedWidth = item.getImageWidth();
                ImageView imageView = new ImageView(img);
                imageView.setPreserveRatio(true);
                imageView.setFitWidth(savedWidth);
                imageView.setPickOnBounds(true);
                imageView.getStyleClass().add("card-image");

                VBox imageBox = new VBox(imageView);
                imageBox.getStyleClass().add("card-image-container");
                imageBox.setFillWidth(true);
                contentArea.getChildren().add(imageBox);

                Separator separator = new Separator();
                separator.getStyleClass().add("card-image-separator");

                Label resizeLabel = new Label(
                    Idf.userLanguageBundle.getString("card.image.resize")
                );
                resizeLabel.getStyleClass().add("card-image-resize-label");

                // 滑杆范围：默认 50~800 像素，可由 config.json 的 imageCard.sliderMin/sliderMax 覆盖
                double sMin = 50, sMax = 800;
                try {
                    JSONObject ic = Idf.userConfig.getJSONObject("imageCard");
                    if (ic != null) {
                        Double v = ic.getDouble("sliderMin");
                        if (v != null) sMin = v;
                        v = ic.getDouble("sliderMax");
                        if (v != null) sMax = v;
                    }
                } catch (Exception e) { /* 配置缺失或格式错误时使用默认范围 */ }
                Slider sizeSlider = new Slider(sMin, sMax, savedWidth);
                sizeSlider.setShowTickLabels(false);
                sizeSlider.setShowTickMarks(false);
                // 主刻度间隔 50px，每个主刻度再分 4 个小刻度（即每格 10px）
                sizeSlider.setMajorTickUnit(50);
                sizeSlider.setMinorTickCount(4);
                sizeSlider.setPrefWidth(Double.MAX_VALUE);
                sizeSlider.getStyleClass().add("card-image-slider");
                // 锁定时禁用滑杆，防止误改
                sizeSlider.disableProperty().bind(Idf.editableProperty.not());
                // 拖动过程中实时缩放图片（只改界面，不写磁盘）
                sizeSlider.valueProperty().addListener((obs, old, val) ->
                    imageView.setFitWidth(val.doubleValue())
                );
                // 拖动结束时才持久化宽度，避免拖动过程中频繁写文件
                sizeSlider.valueChangingProperty().addListener((obs, wasChanging, isChanging) -> {
                    if (!isChanging) {
                        double val = sizeSlider.getValue();
                        // 重新从磁盘读取最新卡片，避免使用可能已过期的内存对象
                        CardItem cardItem = reminderCardService.readCards().get(currentIndex);
                        cardItem.setImageWidth(val);
                        reminderCardService.updateCard(currentIndex, cardItem);
                    }
                });

                VBox sliderBox = new VBox(4, separator, resizeLabel, sizeSlider);
                sliderBox.setPadding(new Insets(0, 10, 0, 10));
                sliderBox.setFillWidth(true);
                sliderControls = sliderBox;
            }
        } else {
            // 普通文字卡片：显示正文
            contentArea.getChildren().add(contentLabel);
        }

        // 时间戳始终放在内容区末尾（标题/正文之后）
        contentArea.getChildren().add(timeRow);

        topRow.getChildren().addAll(iconView, contentArea);

        // 按钮栏：解锁时显示编辑/删除按钮，锁定状态下隐藏
        HBox buttonBar = new HBox(4);
        buttonBar.getStyleClass().add("card-button-bar");
        buttonBar.setMaxWidth(Double.MAX_VALUE);
        // 初始按当前锁定状态决定按钮栏是否显示；后续状态切换由 CardUiService 播放动画
        boolean unlocked = Idf.editableProperty.get();
        buttonBar.setVisible(unlocked);
        buttonBar.setManaged(unlocked);
        buttonBar.setOpacity(unlocked ? 1.0 : 0.0);

        // 展开/收起动画期间高度可能小于按钮栏内容高度，用裁剪避免按钮溢出到相邻卡片
        Rectangle buttonBarClip = new Rectangle();
        buttonBarClip.widthProperty().bind(buttonBar.widthProperty());
        buttonBarClip.heightProperty().bind(buttonBar.heightProperty());
        buttonBar.setClip(buttonBarClip);

        // 登记按钮栏引用，便于 CardUiService 在锁定状态变化时找到并播放动画
        cardRoot.getProperties().put(BUTTON_BAR_PROPERTY, buttonBar);

        // 按钮图标略小于正文基础字号
        double btnIconSize = cardFontSize * 0.9;
        Button deleteBtn = buildIconButton("icon/card/删除.png", btnIconSize, "card-action-button", "danger");
        deleteBtn.setOnAction(e -> onDelete.accept(currentIndex));

        // 图片卡片只允许删除（图片内容本身无法内联编辑），普通卡片提供编辑 + 删除
        boolean isImageCard = imgPath != null && !imgPath.isEmpty();
        if (isImageCard) {
            buttonBar.getChildren().add(deleteBtn);
        } else {
            Button editBtn = buildIconButton("icon/card/编辑.png", btnIconSize, "card-action-button");
            editBtn.setOnAction(e -> onEdit.accept(currentIndex));
            // 登记编辑按钮引用：某张卡片进入编辑态时，CardUiService 会渐隐其他卡片的编辑按钮
            cardRoot.getProperties().put(EDIT_BUTTON_PROPERTY, editBtn);
            buttonBar.getChildren().addAll(editBtn, deleteBtn);
        }

        // 图片卡片把滑杆插在内容和按钮栏之间；普通卡片只有内容和按钮栏
        if (sliderControls != null) {
            cardBody.getChildren().addAll(topRow, sliderControls, buttonBar);
        } else {
            cardBody.getChildren().addAll(topRow, buttonBar);
        }
        cardRoot.getChildren().add(cardBody);

        // 锁定状态下点击整张卡片，等同于点击主编辑区进入编辑（方便教师直接改作业）
        cardRoot.setOnMouseClicked(e -> {
            if (!Idf.isEditable && onEditMainClicked != null) {
                onEditMainClicked.run();
            }
        });

        // 解锁时启用拖拽排序；拖放结束由 CardDragHandler 回调提交新顺序
        if (dragHandler != null) {
            dragHandler.setupDragHandlers(cardRoot, index);
        }

        return cardRoot;
    }

    /**
     * 创建卡片的内联编辑控件（替换只读卡片节点，用于编辑标题、正文、严重性和持久化标记）。
     * <p>
     * 编辑控件本身不直接持久化数据：点击「保存」时先把输入内容写回传入的 {@code item}，
     * 再调用 {@code onSave}；由调用方（{@link CardUiService#saveEditCard}）负责写盘。
     * 因此本方法只关注「编辑界面怎么画、事件怎么转发」。
     * </p>
     *
     * @param item                 被编辑的卡片数据（控件会直接修改其字段）
     * @param index                卡片下标（保留参数，便于后续扩展）
     * @param cardFontFamily       卡片字体家族
     * @param cardFontSize         卡片基础字号
     * @param onSave               保存回调（标题与内容都非空时才会触发）
     * @param onCancel             取消回调
     * @param titleFieldConsumer   把标题输入框回传给调用方，便于切换场景时读取内容
     * @param contentFieldConsumer 把正文输入框回传给调用方，便于切换场景时读取内容
     * @param severityConsumer     严重性切换回调，参数为最新选中的严重性
     * @return 可直接替换原卡片内容的编辑节点
     */
    public VBox createEditCardBody(CardItem item, int index,
                                   String cardFontFamily, double cardFontSize,
                                   Runnable onSave, Runnable onCancel,
                                   java.util.function.Consumer<TextField> titleFieldConsumer,
                                   java.util.function.Consumer<TextArea> contentFieldConsumer,
                                   java.util.function.Consumer<CardItem.Severity> severityConsumer) {
        // 编辑态同样使用略大的严重性图标，保持与只读卡片一致的视觉结构
        double iconSize = cardFontSize * 1.3;
        VBox editBody = new VBox(4);
        editBody.getStyleClass().add("card-body");

        HBox topRow = new HBox(8);

        ImageView iconView = buildSeverityIcon(item.getSeverity(), iconSize);

        // 编辑区纵向排列：严重性选择栏 → 标题输入框 → 正文输入框
        VBox editArea = new VBox(4);
        editArea.getStyleClass().add("card-content-area");
        HBox.setHgrow(editArea, Priority.ALWAYS);

        // 三个严重性按钮互斥，同一时刻只能选中一个
        ToggleGroup severityGroup = new ToggleGroup();
        HBox severityBar = new HBox(6);
        severityBar.getStyleClass().add("card-severity-bar");

        ToggleButton infoBtn = buildSeverityToggle("icon/card/提示.png", iconSize * 0.85, severityGroup);
        infoBtn.setUserData(CardItem.Severity.INFO);
        ToggleButton warningBtn = buildSeverityToggle("icon/card/警告.png", iconSize * 0.75, severityGroup);
        warningBtn.setUserData(CardItem.Severity.WARNING);
        ToggleButton criticalBtn = buildSeverityToggle("icon/card/严重.png", iconSize * 0.75, severityGroup);
        criticalBtn.setUserData(CardItem.Severity.CRITICAL);

        switch (item.getSeverity()) {
            case WARNING -> warningBtn.setSelected(true);
            case CRITICAL -> criticalBtn.setSelected(true);
            default -> infoBtn.setSelected(true);
        }

        severityGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (severityConsumer != null && newToggle != null
                    && newToggle.getUserData() instanceof CardItem.Severity severity) {
                severityConsumer.accept(severity);
            }
        });

        severityBar.getChildren().addAll(infoBtn, warningBtn, criticalBtn);

        // 标题输入框：单行，预填当前标题
        TextField titleField = new TextField(item.getTitle());
        titleField.getStyleClass().add("card-edit-input");
        titleField.setPromptText(Idf.userLanguageBundle.getString("card.title.placeholder"));
        titleField.setFont(Font.font(cardFontFamily, cardFontSize));

        // 正文输入框：多行、自动换行，字号与只读正文保持一致
        TextArea contentField = new TextArea(item.getContent());
        contentField.getStyleClass().add("card-edit-input");
        contentField.setPromptText(Idf.userLanguageBundle.getString("card.content.placeholder"));
        contentField.setWrapText(true);
        contentField.setFont(Font.font(cardFontFamily, Math.max(cardFontSize - 2, 11)));
        // 让正文输入框占满编辑区的剩余高度
        VBox.setVgrow(contentField, Priority.ALWAYS);

        // 把输入框引用交给调用方，供「切换场景前检查未保存内容」时读取
        if (titleFieldConsumer != null) titleFieldConsumer.accept(titleField);
        if (contentFieldConsumer != null) contentFieldConsumer.accept(contentField);

        editArea.getChildren().addAll(severityBar, titleField, contentField);

        // 编辑态按钮栏始终可见（编辑只会在解锁状态下发生）
        HBox buttonBar = new HBox(4);
        buttonBar.getStyleClass().add("card-button-bar");
        buttonBar.setManaged(true);
        buttonBar.setVisible(true);

        // 持久化开关：按钮文案随当前状态切换，点击立即翻转内存标记
        // 注意：这里只改内存，真正落盘发生在点击「保存」时（或取消后由重新加载丢弃）
        Button persistentBtn = new Button(
            item.isPersistent()
                ? Idf.userLanguageBundle.getString("card.cancel.persistent")
                : Idf.userLanguageBundle.getString("card.set.persistent")
        );
        persistentBtn.getStyleClass().addAll("card-edit-text-button", "persistent-toggle");
        persistentBtn.setOnAction(e -> {
            item.setPersistent(!item.isPersistent());
            persistentBtn.setText(
                item.isPersistent()
                    ? Idf.userLanguageBundle.getString("card.cancel.persistent")
                    : Idf.userLanguageBundle.getString("card.set.persistent")
            );
        });

        Button saveBtn = new Button(Idf.userLanguageBundle.getString("card.save"));
        saveBtn.getStyleClass().addAll("card-edit-text-button", "primary");
        Button cancelBtn = new Button(Idf.userLanguageBundle.getString("card.cancel"));
        cancelBtn.getStyleClass().addAll("card-edit-text-button", "danger");

        // 保存：把输入框内容写回卡片对象，然后交给调用方持久化。
        // 标题或正文为空时视为无效输入，不触发保存（也不提示，保持静默）。
        saveBtn.setOnAction(e -> {
            item.setTitle(titleField.getText().trim());
            item.setContent(contentField.getText().trim());
            CardItem.Severity newSeverity = CardItem.Severity.INFO;
            Toggle sel = severityGroup.getSelectedToggle();
            if (sel != null && sel.getUserData() instanceof CardItem.Severity sev) {
                newSeverity = sev;
            }
            item.setSeverity(newSeverity);
            if (!item.getTitle().isEmpty() && !item.getContent().isEmpty()) {
                onSave.run();
            }
        });
        cancelBtn.setOnAction(e -> onCancel.run());

        // 按钮顺序：持久化开关（左） → 保存 → 取消（右）
        buttonBar.getChildren().addAll(persistentBtn, saveBtn, cancelBtn);

        topRow.getChildren().addAll(iconView, editArea);
        buttonBar.setMaxWidth(Double.MAX_VALUE);
        editBody.getChildren().addAll(topRow, buttonBar);

        return editBody;
    }

    /**
     * 展示「是否保存正在编辑的卡片」确认框。
     * <p>
     * 用于用户在有未完成编辑时切换场景（截屏、锁定、打开设置等）。
     * 三个按钮分别对应三个回调：保存、丢弃、取消（留在当前页继续编辑）。
     * 当 {@code hasFullContent} 为 {@code false}（标题或正文只填了一半）时，
     * 保存按钮的文案会变成「返回编辑」，因为此时没有可保存的完整内容。
     * </p>
     *
     * @param onSave         点击「保存」时执行
     * @param onDiscard      点击「丢弃」时执行
     * @param onCancel       点击「取消」时执行
     * @param hasFullContent 标题与正文是否都已填写
     */
    public void showCardSaveConfirmDialog(Runnable onSave, Runnable onDiscard, Runnable onCancel, boolean hasFullContent) {
        // 弹窗根容器：深色背景 + 20px 圆角，固定 400x200 便于居中展示
        AnchorPane root = new AnchorPane();
        root.setStyle("-fx-background-color: #1e1e1e; -fx-background-radius: 20;");
        root.setPrefSize(400, 200);

        // 标题：白色 18px，距顶部与左侧各 20px
        Label titleLabel = new Label(Idf.userLanguageBundle.getString("card.save.confirm.title"));
        titleLabel.setTextFill(Color.WHITE);
        titleLabel.setFont(new Font(18));
        AnchorPane.setTopAnchor(titleLabel, 20.0);
        AnchorPane.setLeftAnchor(titleLabel, 20.0);

        // 提示文案：浅灰色 14px，允许自动换行，占据标题下方区域
        Label messageLabel = new Label(Idf.userLanguageBundle.getString("card.save.confirm.message"));
        messageLabel.setTextFill(Color.web("#cccccc"));
        messageLabel.setFont(new Font(14));
        messageLabel.setWrapText(true);
        AnchorPane.setTopAnchor(messageLabel, 55.0);
        AnchorPane.setLeftAnchor(messageLabel, 20.0);
        AnchorPane.setRightAnchor(messageLabel, 20.0);

        // 有完整内容时按钮写「保存」，否则写「返回编辑」（语义上等同取消）
        String saveText = hasFullContent
            ? Idf.userLanguageBundle.getString("card.save.confirm.save")
            : Idf.userLanguageBundle.getString("card.save.confirm.return.edit");
        Button saveBtn = new Button(saveText);
        saveBtn.getStyleClass().addAll("card-edit-text-button", "primary");
        saveBtn.setOnAction(e -> onSave.run());
        // 绿色主按钮（#4CAF50）
        saveBtn.setStyle("-fx-text-fill: white; -fx-background-color: #4CAF50; -fx-background-radius: 5; -fx-padding: 6 16;");

        Button discardBtn = new Button(Idf.userLanguageBundle.getString("card.save.confirm.discard"));
        discardBtn.getStyleClass().addAll("card-edit-text-button", "danger");
        discardBtn.setOnAction(e -> onDiscard.run());
        // 红色危险按钮（#f44336）
        discardBtn.setStyle("-fx-text-fill: white; -fx-background-color: #f44336; -fx-background-radius: 5; -fx-padding: 6 16;");

        Button cancelBtn = new Button(Idf.userLanguageBundle.getString("card.cancel"));
        cancelBtn.getStyleClass().addAll("card-edit-text-button");
        cancelBtn.setOnAction(e -> onCancel.run());
        // 灰色次要按钮（#555555）
        cancelBtn.setStyle("-fx-text-fill: white; -fx-background-color: #555555; -fx-background-radius: 5; -fx-padding: 6 16;");

        // 按钮靠右排列，距底部与左右各 20px
        HBox buttonBar = new HBox(10, saveBtn, discardBtn, cancelBtn);
        buttonBar.setAlignment(Pos.CENTER_RIGHT);
        AnchorPane.setBottomAnchor(buttonBar, 20.0);
        AnchorPane.setLeftAnchor(buttonBar, 20.0);
        AnchorPane.setRightAnchor(buttonBar, 20.0);

        root.getChildren().addAll(titleLabel, messageLabel, buttonBar);

        popupService.showPopup(root, null);
    }

    /**
     * 同步关闭保存确认框（不做淡出动画）。
     * <p>
     * 依次清除主区域的模糊效果、清空中央弹窗内容、恢复外层容器的鼠标响应、隐藏黑色遮罩。
     * 用于确认框按钮回调中，需要立即关闭并继续后续操作的场景。
     * </p>
     */
    public void hideConfirmDialogSync() {
        showingMainArea.setEffect(null);
        centerShowingArea.getChildren().clear();
        centerOutBox.setMouseTransparent(true);
        blackPane.setOpacity(0);
    }

    /**
     * 构建一个严重性选择按钮（图标 + 互斥切换）。
     *
     * @param iconPath 图标资源路径
     * @param size     图标边长（像素）
     * @param group    所属的互斥切换组
     * @return 带图标、已加入切换组的 ToggleButton
     */
    private ToggleButton buildSeverityToggle(String iconPath, double size, ToggleGroup group) {
        ImageView iconView = buildIcon(iconPath, size);
        ToggleButton btn = new ToggleButton();
        btn.setGraphic(iconView);
        btn.setToggleGroup(group);
        btn.getStyleClass().add("card-severity-toggle");
        return btn;
    }

    /**
     * 根据严重性构建对应的状态图标。
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
     * @param path 资源类路径（相对于 classpath 根）
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
     * 构建一个纯图标按钮。
     *
     * @param iconPath     图标资源路径
     * @param size         图标边长（像素）
     * @param styleClasses 追加到按钮上的样式类
     * @return 带图标的 Button
     */
    public Button buildIconButton(String iconPath, double size, String... styleClasses) {
        ImageView iconView = buildIcon(iconPath, size);
        Button btn = new Button();
        btn.setGraphic(iconView);
        btn.getStyleClass().addAll(styleClasses);
        return btn;
    }

    /**
     * 把严重性枚举映射为 CSS 样式类名，供卡片配色使用。
     *
     * @param severity 严重性级别
     * @return {@code "warning"}、{@code "critical"} 或 {@code "info"}
     */
    public static String severityStyleClass(CardItem.Severity severity) {
        return switch (severity) {
            case WARNING -> "warning";
            case CRITICAL -> "critical";
            default -> "info";
        };
    }
}
