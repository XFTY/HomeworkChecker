package com.xfty.homeworkchecker.service.ui.mainPage;

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
import java.util.List;
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

        contentArea.getChildren().addAll(titleLabel, contentLabel, timeRow);

        topRow.getChildren().addAll(iconView, contentArea);

        HBox buttonBar = new HBox(4);
        buttonBar.getStyleClass().add("card-button-bar");
        buttonBar.setMaxWidth(Double.MAX_VALUE);
        buttonBar.setOpacity(0);
        buttonBar.setManaged(false);
        buttonBar.setUserData(null);

        double btnIconSize = cardFontSize * 0.9;
        Button editBtn = buildIconButton("icon/card/编辑.png", btnIconSize, "card-action-button");
        Button deleteBtn = buildIconButton("icon/card/删除.png", btnIconSize, "card-action-button", "danger");

        int currentIndex = index;
        editBtn.setOnAction(e -> {
            editingCardIndex = currentIndex;
            startEditCard(currentIndex);
        });
        deleteBtn.setOnAction(e -> deleteCard(currentIndex));

        buttonBar.getChildren().addAll(editBtn, deleteBtn);

        cardBody.getChildren().addAll(topRow, buttonBar);
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

        return cardRoot;
    }

    /**
     * 进入卡片编辑模式：重建 DOM 为表单（严重性切换、标题输入、内容输入、持久化/保存/取消）
     *
     * @param index 卡片索引
     */
    private void startEditCard(int index) {
        CardItem item = currentCards.get(index);

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
}
