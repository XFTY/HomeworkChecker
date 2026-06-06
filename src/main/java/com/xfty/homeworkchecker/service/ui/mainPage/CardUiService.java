package com.xfty.homeworkchecker.service.ui.mainPage;

import com.alibaba.fastjson2.JSONObject;
import com.xfty.homeworkchecker.Entry;
import com.xfty.homeworkchecker.Idf;
import com.xfty.homeworkchecker.model.CardItem;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.geometry.Insets;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * CardUiService — 提醒卡片 UI 渲染与交互服务
 * <p>
 * 负责卡片的完整生命周期管理：加载/渲染（renderCard）、编辑态切换（startEditCard）、
 * 悬浮操作按钮动画、添加按钮折叠/展开动画、空状态占位、SplitPane 分割条联动隐藏。
 * 是提醒卡片功能的核心实现。
 * </p>
 */
public class CardUiService {

    private static final Logger logger = LoggerFactory.getLogger(CardUiService.class);

    private static final DataFormat CARD_INDEX_FORMAT = new DataFormat("application/x-homeworkchecker-card-index");

    // ==================== UI 组件引用 ====================

    private final VBox cardList;
    private final Button addCardButton;
    private final StackPane emptyPlaceholder;
    private final Label emptyHintLabel;
    private final VBox cardContainer;
    private final ScrollPane cardScrollPane;
    private final AnchorPane showingMainArea;
    private final AnchorPane centerShowingArea;
    private final VBox centerOutBox;
    private final Pane blackPane;
    private final SplitPane mainSplitPane;
    private final PopupService popupService;
    private final ReminderCardService reminderCardService;
    private final Runnable onEditMainClicked;

    // ==================== 卡片状态 ====================

    /** 当前加载的卡片列表 */
    private final List<CardItem> currentCards = new ArrayList<>();
    /** 正在编辑的卡片索引（-1 表示无编辑） */
    private int editingCardIndex = -1;
    /** 是否为新创建的卡片编辑 */
    private boolean editingNewCard = false;
    /** 编辑中的标题输入框引用 */
    private TextField cardTitleField;
    /** 编辑中的内容输入框引用 */
    private TextArea cardContentField;

    /** 拖拽中的卡片节点 */
    private Node dragNode;
    /** 拖拽前卡片在数据列表中的原始索引 */
    private int dragOriginalIndex = -1;
    /** 上次预览移动到的目标索引，避免重复移动 */
    private int currentPreviewIndex = -1;
    /** 拖拽开始时各卡片的布局 Y 坐标（稳定基准，不受动画影响） */
    private final Map<Node, Double> originalCardYs = new HashMap<>();
    /** 拖拽开始时各卡片的布局高度 */
    private final Map<Node, Double> originalCardHeights = new HashMap<>();

    // ==================== 字体配置 ====================

    private double cardFontSize = 18;
    private String cardFontFamily = "System";

    // ==================== SplitPane 状态 ====================

    private double lastDividerPosition = 0.65;
    private boolean isCardExpanded = true;

    /**
     * 构造器：注入所有 UI 组件和依赖服务
     */
    public CardUiService(
            VBox cardList, Button addCardButton, StackPane emptyPlaceholder, Label emptyHintLabel,
            VBox cardContainer, ScrollPane cardScrollPane,
            AnchorPane showingMainArea, AnchorPane centerShowingArea,
            VBox centerOutBox, Pane blackPane,
            SplitPane mainSplitPane,
            PopupService popupService,
            ReminderCardService reminderCardService,
            Runnable onEditMainClicked) {
        this.cardList = cardList;
        this.addCardButton = addCardButton;
        this.emptyPlaceholder = emptyPlaceholder;
        this.emptyHintLabel = emptyHintLabel;
        this.cardContainer = cardContainer;
        this.cardScrollPane = cardScrollPane;
        this.showingMainArea = showingMainArea;
        this.centerShowingArea = centerShowingArea;
        this.centerOutBox = centerOutBox;
        this.blackPane = blackPane;
        this.mainSplitPane = mainSplitPane;
        this.popupService = popupService;
        this.reminderCardService = reminderCardService;
        this.onEditMainClicked = onEditMainClicked;

        setupCardListDropTarget();
    }

    /**
     * 设置卡片字体配置，重置编辑状态
     */
    public void setFontConfig(String fontFamily, double fontSize) {
        this.cardFontFamily = fontFamily;
        this.cardFontSize = fontSize;
        editingCardIndex = -1;
        editingNewCard = false;
        cardTitleField = null;
        cardContentField = null;
    }

    /**
     * 当前是否有卡片正在编辑
     *
     * @return true 表示有卡片正在编辑
     */
    public boolean isEditingCard() {
        return editingCardIndex >= 0;
    }

    /**
     * 获取当前正在编辑的卡片索引
     *
     * @return 卡片索引，-1 表示无编辑
     */
    public int getEditingCardIndex() {
        return editingCardIndex;
    }

    /**
     * 从数据库加载所有卡片并重新渲染列表
     * 完成时根据是否为空切换空状态占位
     */
    public void loadCards() {
        editingCardIndex = -1;
        editingNewCard = false;
        cardTitleField = null;
        cardContentField = null;
        currentCards.clear();
        currentCards.addAll(reminderCardService.readCards());
        cardList.getChildren().clear();
        for (int i = 0; i < currentCards.size(); i++) {
            cardList.getChildren().add(renderCard(currentCards.get(i), i));
        }
        boolean isEmpty = currentCards.isEmpty();
        emptyPlaceholder.setVisible(isEmpty);
        emptyPlaceholder.setManaged(isEmpty);
        if (isEmpty) {
            updateEmptyHintText();
        }
    }

    /**
     * 获取下一个图片编号（现有图片卡片数 + 1）
     *
     * @return 下一个图片编号
     */
    public int getNextImageNumber() {
        int count = 0;
        for (CardItem item : currentCards) {
            if (item.getImagePath() != null && !item.getImagePath().isEmpty()) {
                count++;
            }
        }
        return count + 1;
    }

    /**
     * 启动时清理：移除标题和内容均为空的无效卡片
     */
    public void cleanupEmptyCardsOnStartup() {
        List<CardItem> cards = reminderCardService.readCards();
        boolean removed = cards.removeIf(card ->
            (card.getTitle() == null || card.getTitle().trim().isEmpty())
            && (card.getContent() == null || card.getContent().trim().isEmpty())
        );
        if (removed) {
            reminderCardService.writeCards(cards);
        }
    }

    /**
     * 清理未完成的新建卡片：若标题和内容均为空则自动删除
     */
    public void cleanupEditingCard() {
        if (editingNewCard && editingCardIndex >= 0) {
            String title = cardTitleField != null ? cardTitleField.getText().trim() : "";
            String content = cardContentField != null ? cardContentField.getText().trim() : "";
            if (title.isEmpty() && content.isEmpty()) {
                deleteCard(editingCardIndex);
            }
        }
    }

    /**
     * 移除所有非 persistent 的图片卡片，同时删除磁盘上的图片文件
     */
    public void removeTodayImageCards() {
        List<CardItem> cards = reminderCardService.readCards();
        boolean removed = false;
        var iterator = cards.iterator();
        while (iterator.hasNext()) {
            CardItem card = iterator.next();
            if (card.getImagePath() != null && !card.getImagePath().isEmpty() && !card.isPersistent()) {
                reminderCardService.deleteImageFile(card.getImagePath());
                iterator.remove();
                removed = true;
            }
        }
        if (removed) {
            reminderCardService.writeCards(cards);
        }
        loadCards();
    }

    /**
     * 根据当前编辑状态更新空状态占位文案（锁定/解锁不同提示）
     */
    private void updateEmptyHintText() {
        if (Idf.isEditable) {
            emptyHintLabel.setText(Idf.userLanguageBundle.getString("card.empty.hint.unlocked"));
        } else {
            emptyHintLabel.setText(Idf.userLanguageBundle.getString("card.empty.hint.locked"));
        }
    }

    /**
     * 检查是否有卡片正在编辑，若有则根据输入状态弹出保存确认对话框
     *
     * @param onProceed 确认后执行的回调
     * @return true 表示无需等待（直接执行回调），false 表示需要等待对话框交互
     */
    public boolean checkActiveCardEditing(Runnable onProceed) {
        if (editingCardIndex < 0) {
            onProceed.run();
            return true;
        }

        String title = cardTitleField != null ? cardTitleField.getText().trim() : "";
        String content = cardContentField != null ? cardContentField.getText().trim() : "";

        if (editingNewCard) {
            if (title.isEmpty() && content.isEmpty()) {
                deleteCard(editingCardIndex);
                onProceed.run();
                return true;
            } else if (!title.isEmpty() && !content.isEmpty()) {
                int idx = editingCardIndex;
                showCardSaveConfirmDialog(
                    () -> {
                        CardItem item = currentCards.get(idx);
                        item.setTitle(title);
                        item.setContent(content);
                        reminderCardService.updateCard(idx, item);
                        editingCardIndex = -1;
                        editingNewCard = false;
                        cardTitleField = null;
                        cardContentField = null;
                        loadCards();
                        hideConfirmDialogSync();
                        onProceed.run();
                    },
                    () -> {
                        deleteCard(idx);
                        hideConfirmDialogSync();
                        onProceed.run();
                    },
                    () -> popupService.closePopup(),
                    true
                );
                return false;
            } else {
                int idx = editingCardIndex;
                showCardSaveConfirmDialog(
                    () -> popupService.closePopup(),
                    () -> {
                        deleteCard(idx);
                        hideConfirmDialogSync();
                        onProceed.run();
                    },
                    () -> popupService.closePopup(),
                    false
                );
                return false;
            }
        } else {
            editingCardIndex = -1;
            editingNewCard = false;
            cardTitleField = null;
            cardContentField = null;
            loadCards();
            onProceed.run();
            return true;
        }
    }

    /**
     * 添加新卡片：创建 CardItem 并直接进入编辑模式
     */
    public void onAddCard() {
        checkActiveCardEditing(() -> {
            CardItem newItem = new CardItem(
                CardItem.Severity.INFO, "", "", ReminderCardService.generateTimestamp()
            );
            newItem.setCreatedDate(Idf.year + Idf.month + Idf.day);
            reminderCardService.addCard(newItem);
            loadCards();
            editingNewCard = true;
            editingCardIndex = currentCards.size() - 1;
            startEditCard(editingCardIndex);
        });
    }

    /**
     * 折叠添加卡片按钮：淡出动画 + 150ms 后隐藏
     */
    public void collapseAddCardBox() {
        addCardButton.setManaged(false);
        Timeline fadeOut = new Timeline(
            new KeyFrame(Duration.millis(150),
                new KeyValue(addCardButton.opacityProperty(), 0, Interpolator.EASE_OUT)
            )
        );
        fadeOut.setOnFinished(e -> {
            addCardButton.setVisible(false);
            if (currentCards.isEmpty()) {
                updateEmptyHintText();
            }
        });
        fadeOut.play();
    }

    /**
     * 展开添加卡片按钮：淡入动画 + 150ms
     */
    public void expandAddCardBox() {
        addCardButton.setVisible(true);
        addCardButton.setManaged(true);
        Timeline fadeIn = new Timeline(
            new KeyFrame(Duration.millis(150),
                new KeyValue(addCardButton.opacityProperty(), 1, Interpolator.EASE_OUT)
            )
        );
        fadeIn.setOnFinished(e -> {
            if (currentCards.isEmpty()) {
                updateEmptyHintText();
            }
        });
        fadeIn.play();
    }

    /**
     * 设置 SplitPane 分割条监听器：拖到 90% 时自动隐藏卡片区域
     */
    public void setupSplitPaneListener() {
        mainSplitPane.getDividers().get(0).positionProperty().addListener((obs, old, val) -> {
            double pos = val.doubleValue();

            if (pos >= 0.90 && isCardExpanded) {
                lastDividerPosition = old.doubleValue();
                isCardExpanded = false;
                cardContainer.setManaged(false);
                cardContainer.setVisible(false);
            } else if (pos < 0.90 && !isCardExpanded) {
                isCardExpanded = true;
                cardContainer.setManaged(true);
                cardContainer.setVisible(true);
            }
        });
    }

    /**
     * 渲染单张卡片 DOM：图标 + 标题 + 内容 + 时间行 + 悬浮操作按钮 + 悬浮动画
     *
     * @param item  卡片数据
     * @param index 卡片在列表中的索引
     * @return 卡片根节点 VBox
     */
    private VBox renderCard(CardItem item, int index) {
        double iconSize = cardFontSize * 1.3;

        VBox cardRoot = new VBox();
        cardRoot.getStyleClass().addAll("card-item", severityStyleClass(item.getSeverity()));
        cardRoot.setFillWidth(true);

        int currentIndex = index;

        VBox cardBody = new VBox();
        cardBody.getStyleClass().add("card-body");

        HBox topRow = new HBox(8);
        topRow.setAlignment(javafx.geometry.Pos.TOP_LEFT);

        ImageView iconView = buildSeverityIcon(item.getSeverity(), iconSize);

        VBox contentArea = new VBox(3);
        contentArea.getStyleClass().add("card-content-area");
        HBox.setHgrow(contentArea, Priority.ALWAYS);

        Label titleLabel = new Label(item.getTitle());
        titleLabel.getStyleClass().add("card-title");
        titleLabel.setFont(Font.font(cardFontFamily, FontWeight.BOLD, cardFontSize));

        Label contentLabel = new Label(item.getContent());
        contentLabel.getStyleClass().add("card-content");
        contentLabel.setFont(Font.font(cardFontFamily, FontWeight.NORMAL, Math.max(cardFontSize - 2, 11)));

        Label timeLabel = new Label(item.getTimestamp());
        timeLabel.getStyleClass().add("card-timestamp");

        HBox timeRow = new HBox(4);
        timeRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        timeRow.getStyleClass().add("card-timestamp-row");

        ImageView timeIcon = buildIcon("icon/card/时间.png", cardFontSize * 0.8);
        timeRow.getChildren().addAll(timeIcon, timeLabel);

        if (item.isPersistent()) {
            timeRow.getStyleClass().add("persistent");
            String persistentText = Idf.userLanguageBundle.getString("card.persistent.label");
            Label persistentLabel = new Label(persistentText);
            persistentLabel.getStyleClass().add("card-persistent-label");
            timeRow.getChildren().add(persistentLabel);
        }

        contentArea.getChildren().add(titleLabel);

        Node sliderControls = null;
        String imgPath = item.getImagePath();
        if (imgPath != null && !imgPath.isEmpty()) {
            Image img = reminderCardService.loadImageForCard(imgPath);
            if (img != null) {
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

                double sMin = 50, sMax = 800;
                try {
                    JSONObject ic = Idf.userConfig.getJSONObject("imageCard");
                    if (ic != null) {
                        Double v = ic.getDouble("sliderMin");
                        if (v != null) sMin = v;
                        v = ic.getDouble("sliderMax");
                        if (v != null) sMax = v;
                    }
                } catch (Exception e) { /* use defaults */ }
                Slider sizeSlider = new Slider(sMin, sMax, savedWidth);
                sizeSlider.setShowTickLabels(false);
                sizeSlider.setShowTickMarks(false);
                sizeSlider.setMajorTickUnit(50);
                sizeSlider.setMinorTickCount(4);
                sizeSlider.setPrefWidth(Double.MAX_VALUE);
                sizeSlider.getStyleClass().add("card-image-slider");
                sizeSlider.disableProperty().bind(Idf.editableProperty.not());
                sizeSlider.valueProperty().addListener((obs, old, val) ->
                    imageView.setFitWidth(val.doubleValue())
                );
                sizeSlider.valueChangingProperty().addListener((obs, wasChanging, isChanging) -> {
                    if (!isChanging) {
                        double val = sizeSlider.getValue();
                        CardItem cardItem = currentCards.get(currentIndex);
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
            contentArea.getChildren().add(contentLabel);
        }

        contentArea.getChildren().add(timeRow);

        topRow.getChildren().addAll(iconView, contentArea);

        HBox buttonBar = new HBox(4);
        buttonBar.getStyleClass().add("card-button-bar");
        buttonBar.setMaxWidth(Double.MAX_VALUE);
        buttonBar.setOpacity(0);
        buttonBar.setManaged(false);
        buttonBar.setUserData(null);

        double btnIconSize = cardFontSize * 0.9;
        Button deleteBtn = buildIconButton("icon/card/删除.png", btnIconSize, "card-action-button", "danger");
        deleteBtn.setOnAction(e -> deleteCard(currentIndex));

        boolean isImageCard = imgPath != null && !imgPath.isEmpty();
        if (isImageCard) {
            buttonBar.getChildren().add(deleteBtn);
        } else {
            Button editBtn = buildIconButton("icon/card/编辑.png", btnIconSize, "card-action-button");
            editBtn.setOnAction(e -> {
                editingCardIndex = currentIndex;
                startEditCard(currentIndex);
            });
            buttonBar.getChildren().addAll(editBtn, deleteBtn);
        }

        if (sliderControls != null) {
            cardBody.getChildren().addAll(topRow, sliderControls, buttonBar);
        } else {
            cardBody.getChildren().addAll(topRow, buttonBar);
        }
        cardRoot.getChildren().add(cardBody);

        cardRoot.setOnMouseEntered(e -> {
            if (editingCardIndex != currentIndex && Idf.isEditable) {
                Timeline running = (Timeline) buttonBar.getUserData();
                if (running != null) {
                    running.stop();
                }
                buttonBar.setManaged(true);
                Timeline fadeIn = new Timeline(
                    new KeyFrame(Duration.millis(200),
                        new KeyValue(buttonBar.opacityProperty(), 1, Interpolator.EASE_OUT)
                    )
                );
                fadeIn.setOnFinished(ev -> buttonBar.setUserData(null));
                buttonBar.setUserData(fadeIn);
                fadeIn.play();
            }
        });
        cardRoot.setOnMouseExited(e -> {
            if (editingCardIndex != currentIndex) {
                Timeline running = (Timeline) buttonBar.getUserData();
                if (running != null) {
                    running.stop();
                }
                Timeline fadeOut = new Timeline(
                    new KeyFrame(Duration.millis(200),
                        new KeyValue(buttonBar.opacityProperty(), 0, Interpolator.EASE_IN)
                    )
                );
                fadeOut.setOnFinished(ev -> {
                    buttonBar.setManaged(false);
                    buttonBar.setUserData(null);
                });
                buttonBar.setUserData(fadeOut);
                fadeOut.play();
            }
        });

        cardRoot.setOnMouseClicked(e -> {
            if (!Idf.isEditable && onEditMainClicked != null) {
                onEditMainClicked.run();
            }
        });

        setupCardDragHandlers(cardRoot, index);

        return cardRoot;
    }

    /**
     * 进入卡片编辑模式：重建 DOM 为表单（严重性切换、标题输入、内容输入、持久化/保存/取消）
     *
     * @param index 卡片索引
     */
    private void startEditCard(int index) {
        CardItem item = currentCards.get(index);
        if (item.getImagePath() != null && !item.getImagePath().isEmpty()) return;

        VBox cardRoot = (VBox) cardList.getChildren().get(index);
        cardRoot.getChildren().clear();
        cardRoot.getStyleClass().removeAll("info", "warning", "critical");
        cardRoot.getStyleClass().addAll("card-item", severityStyleClass(item.getSeverity()));

        double iconSize = cardFontSize * 1.3;
        VBox editBody = new VBox(4);
        editBody.getStyleClass().add("card-body");

        HBox topRow = new HBox(8);

        ImageView iconView = buildSeverityIcon(item.getSeverity(), iconSize);

        VBox editArea = new VBox(4);
        editArea.getStyleClass().add("card-content-area");
        HBox.setHgrow(editArea, Priority.ALWAYS);

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

        severityGroup.selectedToggleProperty().addListener((obs, old, sel) -> {
            if (sel != null && sel.getUserData() instanceof CardItem.Severity sev) {
                cardRoot.getStyleClass().removeAll("info", "warning", "critical");
                cardRoot.getStyleClass().add(severityStyleClass(sev));
            }
        });

        severityBar.getChildren().addAll(infoBtn, warningBtn, criticalBtn);

        TextField titleField = new TextField(item.getTitle());
        titleField.getStyleClass().add("card-edit-input");
        titleField.setPromptText(Idf.userLanguageBundle.getString("card.title.placeholder"));
        titleField.setFont(Font.font(cardFontFamily, cardFontSize));

        TextArea contentField = new TextArea(item.getContent());
        contentField.getStyleClass().add("card-edit-input");
        contentField.setPromptText(Idf.userLanguageBundle.getString("card.content.placeholder"));
        contentField.setWrapText(true);
        contentField.setFont(Font.font(cardFontFamily, Math.max(cardFontSize - 2, 11)));
        VBox.setVgrow(contentField, Priority.ALWAYS);

        cardTitleField = titleField;
        cardContentField = contentField;

        editArea.getChildren().addAll(severityBar, titleField, contentField);

        HBox buttonBar = new HBox(4);
        buttonBar.getStyleClass().add("card-button-bar");
        buttonBar.setManaged(true);
        buttonBar.setVisible(true);

        Button persistentBtn = new Button(
            item.isPersistent()
                ? Idf.userLanguageBundle.getString("card.cancel.persistent")
                : Idf.userLanguageBundle.getString("card.set.persistent")
        );
        persistentBtn.getStyleClass().addAll("card-edit-text-button", "persistent-toggle");
        int currentIndex = index;
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

        saveBtn.setOnAction(e -> {
            CardItem.Severity newSeverity = CardItem.Severity.INFO;
            Toggle sel = severityGroup.getSelectedToggle();
            if (sel != null && sel.getUserData() instanceof CardItem.Severity sev) {
                newSeverity = sev;
            }
            saveEditCard(currentIndex, newSeverity, titleField.getText(), contentField.getText(), item.isPersistent());
        });
        cancelBtn.setOnAction(e -> cancelEditCard());

        buttonBar.getChildren().addAll(persistentBtn, saveBtn, cancelBtn);

        topRow.getChildren().addAll(iconView, editArea);
        buttonBar.setMaxWidth(Double.MAX_VALUE);
        editBody.getChildren().addAll(topRow, buttonBar);
        cardRoot.getChildren().add(editBody);
    }

    /**
     * 保存卡片编辑：更新数据 → 重新加载列表
     */
    private void saveEditCard(int index, CardItem.Severity severity, String newTitle, String newContent, boolean persistent) {
        String title = newTitle.trim();
        String content = newContent.trim();
        if (title.isEmpty() || content.isEmpty()) return;
        CardItem item = currentCards.get(index);
        item.setSeverity(severity);
        item.setTitle(title);
        item.setContent(content);
        item.setPersistent(persistent);
        reminderCardService.updateCard(index, item);
        editingCardIndex = -1;
        editingNewCard = false;
        cardTitleField = null;
        cardContentField = null;
        loadCards();
    }

    /**
     * 取消编辑：新建空卡片时直接删除，已有卡片则重新加载列表
     */
    private void cancelEditCard() {
        if (editingNewCard && editingCardIndex >= 0) {
            deleteCard(editingCardIndex);
            return;
        }
        if (editingCardIndex >= 0) {
            loadCards();
        }
        editingCardIndex = -1;
        editingNewCard = false;
        cardTitleField = null;
        cardContentField = null;
    }

    /**
     * 删除指定索引的卡片
     */
    private void deleteCard(int index) {
        reminderCardService.deleteCard(index);
        editingCardIndex = -1;
        editingNewCard = false;
        cardTitleField = null;
        cardContentField = null;
        loadCards();
    }

    /**
     * 显示卡片保存确认对话框（通过 PopupService 弹窗）
     *
     * @param onSave         保存回调
     * @param onDiscard      放弃回调
     * @param onCancel       取消回调
     * @param hasFullContent 是否已有完整内容（影响保存按钮文案）
     */
    private void showCardSaveConfirmDialog(Runnable onSave, Runnable onDiscard, Runnable onCancel, boolean hasFullContent) {
        AnchorPane root = new AnchorPane();
        root.setStyle("-fx-background-color: #1e1e1e; -fx-background-radius: 20;");
        root.setPrefSize(400, 200);

        Label titleLabel = new Label(Idf.userLanguageBundle.getString("card.save.confirm.title"));
        titleLabel.setTextFill(Color.WHITE);
        titleLabel.setFont(new Font(18));
        AnchorPane.setTopAnchor(titleLabel, 20.0);
        AnchorPane.setLeftAnchor(titleLabel, 20.0);

        Label messageLabel = new Label(Idf.userLanguageBundle.getString("card.save.confirm.message"));
        messageLabel.setTextFill(Color.web("#cccccc"));
        messageLabel.setFont(new Font(14));
        messageLabel.setWrapText(true);
        AnchorPane.setTopAnchor(messageLabel, 55.0);
        AnchorPane.setLeftAnchor(messageLabel, 20.0);
        AnchorPane.setRightAnchor(messageLabel, 20.0);

        String saveText = hasFullContent
            ? Idf.userLanguageBundle.getString("card.save.confirm.save")
            : Idf.userLanguageBundle.getString("card.save.confirm.return.edit");
        Button saveBtn = new Button(saveText);
        saveBtn.getStyleClass().addAll("card-edit-text-button", "primary");
        saveBtn.setOnAction(e -> onSave.run());
        saveBtn.setStyle("-fx-text-fill: white; -fx-background-color: #4CAF50; -fx-background-radius: 5; -fx-padding: 6 16;");

        Button discardBtn = new Button(Idf.userLanguageBundle.getString("card.save.confirm.discard"));
        discardBtn.getStyleClass().addAll("card-edit-text-button", "danger");
        discardBtn.setOnAction(e -> onDiscard.run());
        discardBtn.setStyle("-fx-text-fill: white; -fx-background-color: #f44336; -fx-background-radius: 5; -fx-padding: 6 16;");

        Button cancelBtn = new Button(Idf.userLanguageBundle.getString("card.cancel"));
        cancelBtn.getStyleClass().addAll("card-edit-text-button");
        cancelBtn.setOnAction(e -> onCancel.run());
        cancelBtn.setStyle("-fx-text-fill: white; -fx-background-color: #555555; -fx-background-radius: 5; -fx-padding: 6 16;");

        HBox buttonBar = new HBox(10, saveBtn, discardBtn, cancelBtn);
        buttonBar.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        AnchorPane.setBottomAnchor(buttonBar, 20.0);
        AnchorPane.setLeftAnchor(buttonBar, 20.0);
        AnchorPane.setRightAnchor(buttonBar, 20.0);

        root.getChildren().addAll(titleLabel, messageLabel, buttonBar);

        popupService.showPopup(root, null);
    }

    /**
     * 隐藏确认对话框并清除背景虚化效果
     */
    private void hideConfirmDialogSync() {
        showingMainArea.setEffect(null);
        centerShowingArea.getChildren().clear();
        centerOutBox.setMouseTransparent(true);
        blackPane.setOpacity(0);
    }

    /**
     * 构建严重性切换按钮（INFO/WARNING/CRITICAL）
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
     * 根据严重性构建对应的图标 ImageView
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
     * 从 classpath 加载图标并设置大小
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
     * 构建图标按钮（用于编辑/删除按钮）
     */
    private Button buildIconButton(String iconPath, double size, String... styleClasses) {
        ImageView iconView = buildIcon(iconPath, size);
        Button btn = new Button();
        btn.setGraphic(iconView);
        btn.getStyleClass().addAll(styleClasses);
        return btn;
    }

    /**
     * 获取严重性对应的 CSS 样式类名
     */
    private String severityStyleClass(CardItem.Severity severity) {
        return switch (severity) {
            case WARNING -> "warning";
            case CRITICAL -> "critical";
            default -> "info";
        };
    }

    /**
     * 为 cardList 设置 drop 区域，使用 event filter 统一拦截 DRAG_OVER
     */
    private void setupCardListDropTarget() {
        cardList.addEventFilter(javafx.scene.input.DragEvent.DRAG_OVER, event -> {
            if (dragNode == null || !event.getDragboard().hasContent(CARD_INDEX_FORMAT)) {
                return;
            }
            event.acceptTransferModes(TransferMode.MOVE);

            double yInCardList = cardList.sceneToLocal(event.getSceneX(), event.getSceneY()).getY();
            int targetIdx = computeTargetIndex(yInCardList);
            if (targetIdx != currentPreviewIndex && targetIdx >= 0) {
                previewMove(targetIdx);
            }
            event.consume();
        });

        cardList.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasContent(CARD_INDEX_FORMAT) && dragNode != null) {
                double yInCardList = cardList.sceneToLocal(event.getSceneX(), event.getSceneY()).getY();
                int targetIdx = computeTargetIndex(yInCardList);
                if (targetIdx >= 0 && dragOriginalIndex >= 0 && targetIdx != dragOriginalIndex) {
                    commitReorder(dragOriginalIndex, targetIdx);
                    success = true;
                }
            }
            if (success) {
                dragNode = null;
            }
            event.setDropCompleted(success);
            event.consume();
        });
    }

    /**
     * 为单张卡片根节点设置拖拽事件处理器
     */
    private void setupCardDragHandlers(VBox cardRoot, int index) {
        cardRoot.setOnDragDetected(event -> {
            if (!Idf.isEditable || editingCardIndex >= 0) {
                return;
            }
            dragNode = cardRoot;
            dragOriginalIndex = index;
            currentPreviewIndex = index;

            originalCardYs.clear();
            originalCardHeights.clear();
            for (Node child : cardList.getChildren()) {
                originalCardYs.put(child, child.getBoundsInParent().getMinY());
                originalCardHeights.put(child, child.getBoundsInLocal().getHeight());
            }

            cardRoot.setOpacity(0.5);
            Dragboard db = cardRoot.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
            content.put(CARD_INDEX_FORMAT, index);
            db.setContent(content);
            db.setDragView(cardRoot.snapshot(null, null));
            event.consume();
        });

        cardRoot.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasContent(CARD_INDEX_FORMAT) && dragNode != null) {
                double yInCardList = cardList.sceneToLocal(event.getSceneX(), event.getSceneY()).getY();
                int targetIdx = computeTargetIndex(yInCardList);
                if (targetIdx >= 0 && dragOriginalIndex >= 0 && targetIdx != dragOriginalIndex) {
                    commitReorder(dragOriginalIndex, targetIdx);
                    success = true;
                }
            }
            if (success) {
                dragNode = null;
            }
            event.setDropCompleted(success);
            event.consume();
        });

        cardRoot.setOnDragDone(event -> {
            if (dragNode != null) {
                dragNode.setOpacity(1.0);
                if (!event.isAccepted()) {
                    loadCards();
                }
            }
            dragNode = null;
            dragOriginalIndex = -1;
            currentPreviewIndex = -1;
            originalCardYs.clear();
            originalCardHeights.clear();
        });
    }

    /**
     * 基于拖拽开始时记录的各卡片原始 Y 和高度计算目标插入索引（不受动画/DOM修改影响）
     */
    private int computeTargetIndex(double mouseYInCardList) {
        if (originalCardYs.isEmpty()) return currentPreviewIndex;

        double spacing = cardList.getSpacing();
        List<Node> sorted = new ArrayList<>(originalCardYs.keySet());
        sorted.sort((a, b) -> Double.compare(originalCardYs.get(a), originalCardYs.get(b)));

        double cumY = 0;
        for (int j = 0; j < sorted.size(); j++) {
            Node card = sorted.get(j);
            double h = originalCardHeights.getOrDefault(card, 0.0);
            double midY = cumY + h / 2;
            if (mouseYInCardList < midY) {
                return j;
            }
            cumY += h + spacing;
        }
        return Math.max(0, sorted.size() - 1);
    }

    /**
     * 预览移动：重排 cardList 子节点并用 translateY 过渡动画
     */
    private void previewMove(int targetIdx) {
        if (dragNode == null || targetIdx == currentPreviewIndex) return;
        int count = cardList.getChildren().size();
        if (count < 2) return;

        double[] oldY = new double[count];
        for (int i = 0; i < count; i++) {
            oldY[i] = cardList.getChildren().get(i).getBoundsInParent().getMinY();
        }

        int dragIdx = cardList.getChildren().indexOf(dragNode);
        if (dragIdx < 0) return;

        double[] savedTY = new double[count];
        for (int i = 0; i < count; i++) {
            Node child = cardList.getChildren().get(i);
            Timeline existing = (Timeline) child.getProperties().get("dragAnim");
            if (existing != null) existing.stop();
            savedTY[i] = child.getTranslateY();
            child.setTranslateY(0);
        }

        Node node = cardList.getChildren().remove(dragIdx);
        int clamped = Math.max(0, Math.min(targetIdx, cardList.getChildren().size()));
        cardList.getChildren().add(clamped, node);

        cardList.applyCss();
        cardList.layout();

        double[] newY = new double[count];
        for (int i = 0; i < count; i++) {
            newY[i] = cardList.getChildren().get(i).getBoundsInParent().getMinY();
        }

        for (int i = 0; i < count; i++) {
            Node child = cardList.getChildren().get(i);
            double target = oldY[i] - newY[i];
            Timeline t = new Timeline(
                new KeyFrame(Duration.millis(150),
                    new KeyValue(child.translateYProperty(), 0, Interpolator.EASE_OUT)
                )
            );
            child.setTranslateY(target);
            child.getProperties().put("dragAnim", t);
            t.play();
        }

        currentPreviewIndex = targetIdx;
    }

    /**
     * 确认重排：持久化当前顺序并重新加载
     */
    private void commitReorder(int fromIndex, int toIndex) {
        if (fromIndex == toIndex) return;
        if (fromIndex < 0 || fromIndex >= currentCards.size()) return;
        if (toIndex < 0 || toIndex >= currentCards.size()) return;

        resetTranslateYThen(() -> {
            CardItem moved = currentCards.remove(fromIndex);
            currentCards.add(toIndex, moved);
            reminderCardService.moveCard(fromIndex, toIndex);
            loadCards();
        });
    }

    /**
     * 将所有卡片的 translateY 动画回 0，动画结束后执行回调
     */
    private void resetTranslateYThen(Runnable onDone) {
        int count = cardList.getChildren().size();
        if (count == 0) {
            if (onDone != null) onDone.run();
            return;
        }
        int[] completed = {0};
        for (int i = 0; i < count; i++) {
            Node child = cardList.getChildren().get(i);
            Timeline existing = (Timeline) child.getProperties().get("dragAnim");
            if (existing != null) existing.stop();
            Timeline t = new Timeline(
                new KeyFrame(Duration.millis(150),
                    new KeyValue(child.translateYProperty(), 0, Interpolator.EASE_OUT)
                )
            );
            child.getProperties().put("dragAnim", t);
            t.setOnFinished(e -> {
                child.getProperties().remove("dragAnim");
                completed[0]++;
                if (onDone != null && completed[0] >= count) {
                    onDone.run();
                }
            });
            t.play();
        }
    }
}
