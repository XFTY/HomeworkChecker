package com.xfty.homeworkchecker.service.ui.mainPage;

import com.xfty.homeworkchecker.Idf;
import com.xfty.homeworkchecker.model.CardItem;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * 警示卡片 UI 核心服务。
 *
 * <p>负责主界面右侧卡片区域的全部交互逻辑，包括：卡片的加载/渲染、新增、
 * 内联编辑与删除、拖拽排序，以及卡片容器的折叠/展开。数据持久化委托给
 * {@link ReminderCardService}，具体渲染与编辑控件构建委托给
 * {@link CardRenderer}，拖拽逻辑委托给 {@link CardDragHandler}。</p>
 *
 * <p>本类在 {@code MainPage} 构造时被创建，并通过构造函数注入所需的
 * JavaFX 控件引用与协作服务。</p>
 */
public class CardUiService {

    private static final Logger logger = LoggerFactory.getLogger(CardUiService.class);

    // ==================== UI 组件引用 ====================

    private final VBox cardList;                        // 卡片列表容器，每个子节点为一张卡片
    private final Button addCardButton;                 // "添加卡片"按钮
    private final StackPane emptyPlaceholder;           // 无卡片时显示的占位面板
    private final Label emptyHintLabel;                 // 占位面板中的提示文字
    private final VBox cardContainer;                   // 包裹卡片区域的整体容器（用于折叠/展开）
    private final ScrollPane cardScrollPane;            // 卡片区域的滚动面板
    private final AnchorPane showingMainArea;           // 主显示区域（作业展示）
    private final AnchorPane centerShowingArea;         // 中央弹层承载区域
    private final VBox centerOutBox;                    // 弹层外部容器
    private final Pane blackPane;                       // 弹层背景遮罩
    private final SplitPane mainSplitPane;              // 分隔主显示区与卡片区的 SplitPane
    private final PopupService popupService;            // 弹层服务
    private final ReminderCardService reminderCardService; // 卡片数据持久化服务
    private final Runnable onEditMainClicked;           // "编辑主区域"按钮的回调

    private final CardRenderer cardRenderer;            // 卡片渲染与编辑控件构建器
    private final CardDragHandler cardDragHandler;      // 卡片拖拽排序处理器

    // ==================== 卡片状态 ====================

    private final List<CardItem> currentCards = new ArrayList<>(); // 当前内存中的卡片数据
    private int editingCardIndex = -1;                  // 正在编辑的卡片下标，-1 表示无
    private boolean editingNewCard = false;             // 是否正在编辑一张新建的卡片
    private TextField cardTitleField;                   // 编辑中的标题输入框
    private TextArea cardContentField;                  // 编辑中的内容输入框
    private CardItem.Severity editingCardSeverity;      // 编辑中选中的严重性

    // ==================== 字体配置 ====================

    private double cardFontSize = 18;                   // 卡片字号
    private String cardFontFamily = "System";           // 卡片字体族

    // ==================== SplitPane 状态 ====================

    private double lastDividerPosition = 0.65;          // 折叠前分隔条的最后一个位置，用于恢复
    private boolean isCardExpanded = true;              // 卡片区域当前是否展开

    /**
     * 构造卡片 UI 服务。
     *
     * @param cardList            卡片列表容器
     * @param addCardButton       "添加卡片"按钮
     * @param emptyPlaceholder    空状态占位面板
     * @param emptyHintLabel      空状态提示文字
     * @param cardContainer       卡片区域整体容器
     * @param cardScrollPane      卡片区域滚动面板
     * @param showingMainArea     主显示区域
     * @param centerShowingArea   中央弹层承载区域
     * @param centerOutBox        弹层外部容器
     * @param blackPane           弹层背景遮罩
     * @param mainSplitPane       主分隔面板
     * @param popupService        弹层服务
     * @param reminderCardService 卡片数据持久化服务
     * @param onEditMainClicked   编辑主区域按钮的回调
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

        // 渲染器负责卡片 DOM 与编辑控件的构建
        this.cardRenderer = new CardRenderer(
            reminderCardService, popupService,
            showingMainArea, centerShowingArea, centerOutBox, blackPane
        );
        // 拖拽处理器在拖放结束时提交排序并刷新列表
        this.cardDragHandler = new CardDragHandler(
            cardList, this::commitReorderSilent, this::loadCards
        );
        cardDragHandler.setupDropTarget();
    }

    /**
     * 更新卡片字体/字号配置，并重置当前编辑状态（配置变更后重新渲染卡片）。
     */
    public void setFontConfig(String fontFamily, double fontSize) {
        this.cardFontFamily = fontFamily;
        this.cardFontSize = fontSize;
        editingCardIndex = -1;
        editingNewCard = false;
        cardTitleField = null;
        cardContentField = null;
        editingCardSeverity = null;
    }

    /** 判断当前是否有卡片处于编辑态。 */
    public boolean isEditingCard() {
        return editingCardIndex >= 0;
    }

    /** 返回正在编辑的卡片下标，无编辑时为 -1。 */
    public int getEditingCardIndex() {
        return editingCardIndex;
    }

    /**
     * 从持久化服务重新读取卡片数据并刷新整个卡片列表 UI。
     * 同时重置所有编辑状态，并根据卡片数量切换空状态占位面板。
     */
    public void loadCards() {
        editingCardIndex = -1;
        editingNewCard = false;
        cardTitleField = null;
        cardContentField = null;
        editingCardSeverity = null;
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

    /** 将单条卡片数据渲染为可加入列表的 VBox 节点。 */
    private VBox renderCard(CardItem item, int index) {
        return cardRenderer.renderCard(
            item, index, cardFontFamily, cardFontSize, editingCardIndex, editingNewCard,
            onEditMainClicked,
            this::startEditCard,
            this::deleteCard,
            cardDragHandler
        );
    }

    /**
     * 计算下一张图片卡片的编号：以当前已有图片卡片数量 + 1 作为编号。
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
     * 启动时清理空卡片：删除标题与内容均为空白的卡片，并在有变更时回写持久化。
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
     * 清理正在编辑的新卡片：若新建卡片标题与内容均为空，则将其删除。
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
     * 移除今日生成的临时图片卡片（非持久化且带图片路径），
     * 同时删除对应图片文件，最后刷新卡片列表。
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

    /** 根据当前是否可编辑，设置空状态提示文案（可编辑/锁定两种）。 */
    private void updateEmptyHintText() {
        if (Idf.isEditable) {
            emptyHintLabel.setText(Idf.userLanguageBundle.getString("card.empty.hint.unlocked"));
        } else {
            emptyHintLabel.setText(Idf.userLanguageBundle.getString("card.empty.hint.locked"));
        }
    }

    /**
     * 在切换到其他操作前处理当前正在编辑的卡片。
     *
     * <p>若无卡片处于编辑态，则直接执行 {@code onProceed} 并返回 {@code true}。
     * 若正在编辑新建卡片：标题和内容都为空则直接删除；都非空则弹出保存确认框，
     * 由用户决定保存或删除；只有其一非空时也弹出确认框，但仅提供删除选项。
     * 编辑已有卡片时同样弹出保存确认框，由用户决定保存或放弃修改。</p>
     *
     * @param onProceed 编辑状态处理完成后要执行的回调
     * @return {@code true} 表示 {@code onProceed} 已同步执行完成；
     *         {@code false} 表示已弹出确认框，需等待用户选择后再异步继续
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
                cardRenderer.showCardSaveConfirmDialog(
                    () -> {
                        CardItem item = currentCards.get(idx);
                        item.setTitle(title);
                        item.setContent(content);
                        if (editingCardSeverity != null) {
                            item.setSeverity(editingCardSeverity);
                        }
                        reminderCardService.updateCard(idx, item);
                        editingCardIndex = -1;
                        editingNewCard = false;
                        cardTitleField = null;
                        cardContentField = null;
                        editingCardSeverity = null;
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
                cardRenderer.showCardSaveConfirmDialog(
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
            final int idx = editingCardIndex;
            final CardItem item = currentCards.get(idx);
            Runnable resetAndProceed = () -> {
                editingCardIndex = -1;
                editingNewCard = false;
                cardTitleField = null;
                cardContentField = null;
                editingCardSeverity = null;
                loadCards();
                hideConfirmDialogSync();
                onProceed.run();
            };
            Runnable saveAndProceed = () -> {
                if (!title.isEmpty() && !content.isEmpty()) {
                    item.setTitle(title);
                    item.setContent(content);
                    if (editingCardSeverity != null) {
                        item.setSeverity(editingCardSeverity);
                    }
                    reminderCardService.updateCard(idx, item);
                }
                resetAndProceed.run();
            };

            if (!title.isEmpty() && !content.isEmpty()) {
                cardRenderer.showCardSaveConfirmDialog(
                    saveAndProceed,
                    resetAndProceed,
                    () -> popupService.closePopup(),
                    true
                );
            } else {
                cardRenderer.showCardSaveConfirmDialog(
                    () -> popupService.closePopup(),
                    resetAndProceed,
                    () -> popupService.closePopup(),
                    false
                );
            }
            return false;
        }
    }

    /** 同步隐藏卡片保存确认对话框。 */
    private void hideConfirmDialogSync() {
        cardRenderer.hideConfirmDialogSync();
    }

    /**
     * 新增卡片：先处理当前编辑状态，再创建一条空卡片并立即进入编辑。
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

    /** 以淡出动画折叠"添加卡片"按钮，并更新空状态提示。 */
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

    /** 以淡入动画展开"添加卡片"按钮，并更新空状态提示。 */
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
     * 为 SplitPane 分隔条注册监听：当拖动位置比例达到 0.90 及以上时折叠卡片区域，
     * 回到 0.90 以下时恢复展开，并记录折叠前的位置。
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
     * 将指定下标的卡片切换为内联编辑态（图片卡片不可编辑）。
     * 清空原卡片节点并替换为编辑控件。
     */
    private void startEditCard(int index) {
        CardItem item = currentCards.get(index);
        if (item.getImagePath() != null && !item.getImagePath().isEmpty()) return;

        this.editingCardIndex = index;
        this.editingCardSeverity = item.getSeverity();

        VBox cardRoot = (VBox) cardList.getChildren().get(index);
        cardRoot.getChildren().clear();
        cardRoot.getStyleClass().removeAll("info", "warning", "critical");
        cardRoot.getStyleClass().addAll("card-item", CardRenderer.severityStyleClass(item.getSeverity()));

        VBox editBody = cardRenderer.createEditCardBody(
            item, index, cardFontFamily, cardFontSize,
            () -> saveEditCard(index),
            () -> cancelEditCard(),
            tf -> this.cardTitleField = tf,
            ta -> this.cardContentField = ta,
            sev -> this.editingCardSeverity = sev
        );
        cardRoot.getChildren().add(editBody);
    }

    /**
     * 保存指定卡片的编辑内容。标题或内容为空时不保存，直接返回。
     */
    private void saveEditCard(int index) {
        CardItem item = currentCards.get(index);
        String title = cardTitleField != null ? cardTitleField.getText().trim() : "";
        String content = cardContentField != null ? cardContentField.getText().trim() : "";
        if (title.isEmpty() || content.isEmpty()) return;
        item.setTitle(title);
        item.setContent(content);
        reminderCardService.updateCard(index, item);
        editingCardIndex = -1;
        editingNewCard = false;
        cardTitleField = null;
        cardContentField = null;
        loadCards();
    }

    /**
     * 取消卡片编辑：若取消的是新建卡片则将其删除，否则重新加载以还原卡片显示。
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
     * 删除指定下标的卡片并刷新列表，同时重置编辑状态。
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
     * 提交卡片排序结果（静默，不弹出提示）。
     * 在内存与持久化中同步移动卡片位置后刷新列表，越界或位置未变时不做处理。
     *
     * @param fromIndex 被移动卡片的原下标
     * @param toIndex   目标下标
     */
    private void commitReorderSilent(int fromIndex, int toIndex) {
        if (fromIndex == toIndex) return;
        if (fromIndex < 0 || fromIndex >= currentCards.size()) return;
        if (toIndex < 0 || toIndex >= currentCards.size()) return;

        CardItem moved = currentCards.remove(fromIndex);
        currentCards.add(toIndex, moved);
        reminderCardService.moveCard(fromIndex, toIndex);
        loadCards();
    }
}
