package com.xfty.homeworkchecker.service.ui.mainPage;

import com.xfty.homeworkchecker.Idf;
import com.xfty.homeworkchecker.model.CardItem;
import com.xfty.homeworkchecker.service.ui.motion.MotionEasing;
import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;
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
 * JavaFX 控件引用与协作服务。典型调用链：</p>
 * <pre>
 * MainPage.onAddCard()            → onAddCard() → startEditCard()
 * MainPage.onscreenShotButtonPressed()
 *                                 → checkActiveCardEditing(截图)
 * 卡片「保存」按钮                 → saveEditCard() → 退出编辑动画
 * 卡片「删除」按钮                 → deleteCardWithAnimation() → 收起动画 → deleteCard() → loadCards()
 * 卡片拖放结束                     → commitReorderSilent() → loadCards()
 * </pre>
 *
 * <p>编辑状态由一组字段共同描述：{@link #editingCardIndex} 表示正在编辑哪张卡片，
 * {@link #editingNewCard} 区分「新建卡片」与「编辑已有卡片」，
 * 输入框与严重性字段缓存当前编辑值，供切换场景时读取。</p>
 */
public class CardUiService {

    private static final Logger logger = LoggerFactory.getLogger(CardUiService.class);

    /** 按钮栏透明度动画时长（保持原有节奏）。 */
    private static final Duration BUTTON_BAR_FADE_DURATION = Duration.millis(200);
    /** 按钮栏高度弹簧动画时长（在原有节奏上放慢一倍）。 */
    private static final Duration BUTTON_BAR_HEIGHT_ANIM_DURATION = Duration.millis(400);
    /** 按钮栏当前动画在节点属性中的键，用于快速切换时停止旧动画。 */
    private static final String BUTTON_BAR_ANIM_KEY = "cardButtonBarAnim";
    /** 添加/收起「添加卡片」按钮的淡入淡出时长。 */
    private static final Duration ADD_BUTTON_FADE_DURATION = Duration.millis(150);
    /** 编辑期间其他卡片「编辑」按钮的渐隐/渐显时长。 */
    private static final Duration EDIT_BUTTON_FADE_DURATION = Duration.millis(150);
    /** 编辑按钮透明度动画在节点属性中的键，用于快速切换时停止旧动画。 */
    private static final String EDIT_BUTTON_ANIM_KEY = "cardEditButtonAnim";
    /** 新增卡片时「让出空间 + 新卡片放大」的动画时长（在原有节奏上放慢一倍）。 */
    private static final Duration CARD_INSERT_ANIM_DURATION = Duration.millis(560);
    /** 取消新建卡片时「卡片缩小 + 空间收回」的反向动画时长，与插入动画保持一致。 */
    private static final Duration CARD_REMOVE_ANIM_DURATION = CARD_INSERT_ANIM_DURATION;
    /** 删除已有卡片时的收起动画时长，保持操作轻快。 */
    private static final Duration CARD_DELETE_ANIM_DURATION = Duration.millis(300);
    /** 卡片移除动画在节点属性中的键，用于动画结束后清理引用。 */
    private static final String CARD_REMOVE_ANIM_KEY = "cardRemoveAnim";
    /** 编辑态卡片展开/收缩的高度弹簧动画时长。 */
    private static final Duration CARD_EDIT_HEIGHT_ANIM_DURATION = Duration.millis(400);
    /** 内容渐隐时长：在弹簧前段快速淡出，避免线性拖到弹簧收束尾部。 */
    private static final Duration CARD_EDIT_FADE_OUT_DURATION = Duration.millis(180);
    /** 内容渐显时长：与高度动画剩余时间对齐，使淡入和弹簧收束同时结束。 */
    private static final Duration CARD_EDIT_FADE_IN_DURATION =
        CARD_EDIT_HEIGHT_ANIM_DURATION.subtract(CARD_EDIT_FADE_OUT_DURATION);
    /** 编辑切换动画在卡片节点属性中的键，用于中断和清理。 */
    private static final String CARD_EDIT_ANIM_KEY = "cardEditAnim";
    /** 编辑中切换警示程度时，卡片背景色的渐变时长。 */
    private static final Duration SEVERITY_COLOR_FADE_DURATION = Duration.millis(220);
    /** 警示程度颜色渐变动画在节点属性中的键，用于快速切换时停止旧动画。 */
    private static final String SEVERITY_COLOR_ANIM_KEY = "cardSeverityColorAnim";
    /** SplitPane 分隔条比例达到该值时折叠卡片区域。 */
    private static final double CARD_COLLAPSE_THRESHOLD = 0.90;

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
        // 拖拽处理器在拖放结束时提交排序并刷新列表；编辑任意卡片期间禁止拖拽
        this.cardDragHandler = new CardDragHandler(
            cardList, this::commitReorderSilent, this::loadCards, this::isEditingCard
        );
        cardDragHandler.setupDropTarget();

        // 锁定/解锁时同步卡片的悬停样式，并播放按钮栏展开/收起动画
        Idf.editableProperty.addListener((obs, oldVal, unlocked) -> updateCardLockState(unlocked));
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

    /**
     * 响应锁定状态变化：更新所有卡片的悬停样式，并播放按钮栏动画。
     *
     * @param unlocked {@code true} 表示已解锁，{@code false} 表示已锁定
     */
    private void updateCardLockState(boolean unlocked) {
        for (Node card : cardList.getChildren()) {
            applyCardLockedStyle(card, unlocked);
            Region buttonBar = (Region) card.getProperties().get(CardRenderer.BUTTON_BAR_PROPERTY);
            if (buttonBar != null && card instanceof Region cardRoot) {
                animateButtonBar(cardRoot, buttonBar, unlocked);
            }
        }
    }

    /**
     * 根据锁定状态为单张卡片增删 {@code locked} 样式类。
     * 锁定状态下卡片保持原色、悬停不高亮；解锁后恢复悬停效果。
     */
    private void applyCardLockedStyle(Node card, boolean unlocked) {
        if (unlocked) {
            card.getStyleClass().remove("locked");
        } else if (!card.getStyleClass().contains("locked")) {
            // 避免重复添加同名样式类
            card.getStyleClass().add("locked");
        }
    }

    /**
     * 渐隐除正在编辑卡片外的所有「编辑」按钮。
     * <p>
     * 编辑态下只允许操作当前卡片，其他卡片的编辑按钮淡出并禁止点击；
     * 按钮仍保留在布局中，避免按钮栏因节点消失而整体跳动。
     * </p>
     *
     * @param editingIndex 正在编辑的卡片下标，该卡片的按钮不参与渐隐
     */
    private void fadeOutOtherEditButtons(int editingIndex) {
        for (int i = 0; i < cardList.getChildren().size(); i++) {
            // 正在编辑的卡片稍后会被替换为编辑控件，无需处理
            if (i == editingIndex) continue;
            Node editButton = getEditButton(cardList.getChildren().get(i));
            if (editButton == null) continue;
            // 淡出期间立即禁止点击，避免透明按钮被误触
            editButton.setMouseTransparent(true);
            animateEditButtonOpacity(editButton, 0.0);
        }
    }

    /**
     * 让所有卡片的「编辑」按钮渐显。
     * <p>
     * 列表刷新后新渲染的按钮默认不透明，因此先归零再播放动画，才能看到渐显过程。
     * </p>
     */
    private void fadeInAllEditButtons() {
        fadeInOtherEditButtons(-1);
    }

    /**
     * 让除指定卡片外的「编辑」按钮渐显。
     * <p>
     * 编辑退出动画会把目标卡片的只读内容整体渐显，其编辑按钮已包含在内，
     * 因此需要排除该卡片，避免按钮先变透明再重新渐显造成闪烁。
     * </p>
     *
     * @param excludeIndex 不参与渐显的卡片下标，-1 表示处理全部卡片
     */
    private void fadeInOtherEditButtons(int excludeIndex) {
        for (int i = 0; i < cardList.getChildren().size(); i++) {
            if (i == excludeIndex) continue;
            Node editButton = getEditButton(cardList.getChildren().get(i));
            if (editButton == null) continue;
            // 新节点默认 opacity=1，先置零，动画才能从透明过渡到不透明
            editButton.setOpacity(0.0);
            // 渐显开始即恢复点击，避免动画结束后按钮短暂不可用
            editButton.setMouseTransparent(false);
            animateEditButtonOpacity(editButton, 1.0);
        }
    }

    /**
     * 从卡片根节点取出「编辑」按钮引用。
     *
     * @param cardNode 卡片根节点
     * @return 编辑按钮；图片卡片（无编辑按钮）或未登记时返回 null
     */
    private Node getEditButton(Node cardNode) {
        Object button = cardNode.getProperties().get(CardRenderer.EDIT_BUTTON_PROPERTY);
        return button instanceof Node node ? node : null;
    }

    /**
     * 播放「编辑」按钮的透明度动画。
     * 若该按钮已有动画正在运行则先停止，保证快速切换时从当前透明度继续。
     *
     * @param editButton     编辑按钮节点
     * @param targetOpacity  目标透明度（0 为渐隐，1 为渐显）
     */
    private void animateEditButtonOpacity(Node editButton, double targetOpacity) {
        Timeline running = (Timeline) editButton.getProperties().get(EDIT_BUTTON_ANIM_KEY);
        if (running != null) {
            running.stop();
        }

        Timeline animation = new Timeline(
            new KeyFrame(EDIT_BUTTON_FADE_DURATION,
                new KeyValue(editButton.opacityProperty(), targetOpacity, Interpolator.EASE_BOTH)
            )
        );
        // 动画结束后移除引用，避免长期占用节点属性
        animation.setOnFinished(event -> editButton.getProperties().remove(EDIT_BUTTON_ANIM_KEY));
        editButton.getProperties().put(EDIT_BUTTON_ANIM_KEY, animation);
        animation.play();
    }

    /**
     * 播放单张卡片按钮栏的展开或收起动画。
     * 若已有动画正在运行则先停止，保证快速切换时动画从当前状态继续。
     *
     * @param cardRoot  卡片根节点，与按钮栏同步做高度动画
     * @param buttonBar 卡片按钮栏
     * @param unlocked  {@code true} 表示展开，{@code false} 表示收起
     */
    private void animateButtonBar(Region cardRoot, Region buttonBar, boolean unlocked) {
        // 编辑态下旧按钮栏已脱离卡片，直接跳过，避免动画作用到无效节点
        if (buttonBar.getParent() == null || buttonBar.getParent().getParent() != cardRoot) {
            return;
        }

        Timeline running = (Timeline) buttonBar.getProperties().get(BUTTON_BAR_ANIM_KEY);
        if (running != null) {
            running.stop();
        }

        Timeline animation = unlocked
            ? createButtonBarExpandAnimation(cardRoot, buttonBar)
            : createButtonBarCollapseAnimation(cardRoot, buttonBar);
        buttonBar.getProperties().put(BUTTON_BAR_ANIM_KEY, animation);
        animation.play();
    }

    /**
     * 创建按钮栏展开动画：按钮栏高度从 0 增长到目标高度，卡片根节点同步长高。
     * <p>
     * 仅动画按钮栏自身时，卡片高度依赖父容器重新布局；动画结束再把按钮栏移出布局，
     * 会丢掉最后一帧尚未布局的高度并产生跳变。因此卡片根节点也参与高度动画，
     * 保证任意时刻的卡片高度都与按钮栏状态一致。
     * </p>
     */
    private Timeline createButtonBarExpandAnimation(Region cardRoot, Region buttonBar) {
        // 先记录当前（锁定态）卡片与按钮栏状态，作为动画起点
        double startCardHeight = Math.max(cardRoot.getHeight(), 1);
        double startBarHeight = buttonBar.isManaged() ? Math.max(buttonBar.getHeight(), 0) : 0;
        double startOpacity = buttonBar.getOpacity();

        // 临时恢复自动尺寸，测量完全展开后的按钮栏与卡片目标高度
        buttonBar.setMinHeight(Region.USE_COMPUTED_SIZE);
        buttonBar.setPrefHeight(Region.USE_COMPUTED_SIZE);
        buttonBar.setMaxHeight(Region.USE_COMPUTED_SIZE);
        cardRoot.setMinHeight(Region.USE_COMPUTED_SIZE);
        cardRoot.setPrefHeight(Region.USE_COMPUTED_SIZE);
        cardRoot.setMaxHeight(Region.USE_COMPUTED_SIZE);
        buttonBar.setVisible(true);
        buttonBar.setManaged(true);
        buttonBar.applyCss();
        cardRoot.applyCss();
        double targetHeight = buttonBar.prefHeight(-1);
        double targetCardHeight = measureCardHeight(cardRoot);

        buttonBar.setMinHeight(0);
        // 首选高度与最大高度从当前状态同步动画，让弹簧过冲能真正体现在布局高度上
        buttonBar.setPrefHeight(startBarHeight);
        buttonBar.setMaxHeight(startBarHeight);
        buttonBar.setOpacity(startOpacity);
        // 卡片根节点从当前锁定高度同步长高，避免最后一帧跳变
        cardRoot.setMinHeight(0);
        cardRoot.setPrefHeight(startCardHeight);
        cardRoot.setMaxHeight(startCardHeight);

        Timeline expand = new Timeline(
            // 高度放慢一倍，透明度仍使用原有 200ms 节奏
            new KeyFrame(BUTTON_BAR_HEIGHT_ANIM_DURATION,
                // 高度使用类 iOS 弹簧曲线：缓慢展开并带轻微回弹
                new KeyValue(buttonBar.prefHeightProperty(), targetHeight, MotionEasing.SPRING),
                new KeyValue(buttonBar.maxHeightProperty(), targetHeight, MotionEasing.SPRING),
                new KeyValue(cardRoot.prefHeightProperty(), targetCardHeight, MotionEasing.SPRING),
                new KeyValue(cardRoot.maxHeightProperty(), targetCardHeight, MotionEasing.SPRING)
            ),
            new KeyFrame(BUTTON_BAR_FADE_DURATION,
                // 透明度保持线性渐变
                new KeyValue(buttonBar.opacityProperty(), 1, Interpolator.LINEAR)
            )
        );
        expand.setOnFinished(event -> finishButtonBarAnimation(cardRoot, buttonBar, true));
        return expand;
    }

    /**
     * 创建按钮栏收起动画：按钮栏高度从当前高度收缩到 0，卡片根节点同步缩回。
     * <p>
     * 收起前先测量按钮栏移出布局后的卡片目标高度（同时包含 VBox 间距的变化），
     * 再让卡片根节点直接动画到该高度，避免动画结束后卡片突然“顿”一下。
     * </p>
     */
    private Timeline createButtonBarCollapseAnimation(Region cardRoot, Region buttonBar) {
        double startCardHeight = Math.max(cardRoot.getHeight(), 1);
        double startHeight = buttonBar.getHeight();
        if (startHeight <= 0) {
            startHeight = Math.max(buttonBar.prefHeight(-1), 1);
        }

        // 临时把按钮栏移出布局，测量卡片收起后的真实目标高度
        buttonBar.setManaged(false);
        buttonBar.setVisible(false);
        cardRoot.setMinHeight(Region.USE_COMPUTED_SIZE);
        cardRoot.setPrefHeight(Region.USE_COMPUTED_SIZE);
        cardRoot.setMaxHeight(Region.USE_COMPUTED_SIZE);
        cardRoot.applyCss();
        double targetCardHeight = measureCardHeight(cardRoot);
        // 恢复按钮栏，随后立即改为显式动画起点
        buttonBar.setManaged(true);
        buttonBar.setVisible(true);

        buttonBar.setMinHeight(0);
        // 从当前高度同步收缩首选高度与最大高度，弹簧只做无过冲收束
        buttonBar.setPrefHeight(startHeight);
        buttonBar.setMaxHeight(startHeight);
        cardRoot.setMinHeight(0);
        cardRoot.setPrefHeight(startCardHeight);
        cardRoot.setMaxHeight(startCardHeight);

        Timeline collapse = new Timeline(
            // 高度放慢一倍，透明度仍使用原有 200ms 节奏
            new KeyFrame(BUTTON_BAR_HEIGHT_ANIM_DURATION,
                // 收起使用无过冲曲线，避免高度出现负值
                new KeyValue(buttonBar.prefHeightProperty(), 0, MotionEasing.SPRING_EXIT),
                new KeyValue(buttonBar.maxHeightProperty(), 0, MotionEasing.SPRING_EXIT),
                new KeyValue(cardRoot.prefHeightProperty(), targetCardHeight, MotionEasing.SPRING_EXIT),
                new KeyValue(cardRoot.maxHeightProperty(), targetCardHeight, MotionEasing.SPRING_EXIT)
            ),
            new KeyFrame(BUTTON_BAR_FADE_DURATION,
                // 透明度保持线性渐变
                new KeyValue(buttonBar.opacityProperty(), 0, Interpolator.LINEAR)
            )
        );
        collapse.setOnFinished(event -> finishButtonBarAnimation(cardRoot, buttonBar, false));
        return collapse;
    }

    /**
     * 测量卡片根节点在给定宽度下的首选高度。
     *
     * @param cardRoot 卡片根节点
     * @return 至少为 1 的首选高度
     */
    private double measureCardHeight(Region cardRoot) {
        double width = cardRoot.getWidth();
        if (width <= 0) {
            width = cardList.getWidth();
        }
        double height = width > 0 ? cardRoot.prefHeight(width) : cardRoot.prefHeight(-1);
        return Math.max(height, 1);
    }

    /**
     * 按钮栏动画结束后的状态复位。
     *
     * @param cardRoot  卡片根节点
     * @param buttonBar 卡片按钮栏
     * @param expanded  {@code true} 表示展开动画结束，{@code false} 表示收起动画结束
     */
    private void finishButtonBarAnimation(Region cardRoot, Region buttonBar, boolean expanded) {
        if (!expanded) {
            // 先移出布局，再恢复卡片自动高度；此时卡片已收束到目标高度，不会跳变
            buttonBar.setVisible(false);
            buttonBar.setManaged(false);
        }
        // 恢复自动高度，避免限制后续字体/内容变化时的布局
        buttonBar.setMinHeight(Region.USE_COMPUTED_SIZE);
        buttonBar.setPrefHeight(Region.USE_COMPUTED_SIZE);
        buttonBar.setMaxHeight(Region.USE_COMPUTED_SIZE);
        cardRoot.setMinHeight(Region.USE_COMPUTED_SIZE);
        cardRoot.setPrefHeight(Region.USE_COMPUTED_SIZE);
        cardRoot.setMaxHeight(Region.USE_COMPUTED_SIZE);
        buttonBar.getProperties().remove(BUTTON_BAR_ANIM_KEY);
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
        // 先记录刷新前是否处于编辑态：若刚结束编辑，重建列表后需要让编辑按钮渐显
        boolean wasEditing = editingCardIndex >= 0;
        reloadCards();
        // 编辑结束后重建的按钮默认不透明，统一从透明渐显，形成平滑的恢复效果
        if (wasEditing) {
            fadeInAllEditButtons();
        }
    }

    /**
     * 从持久化服务重新读取卡片数据并重建卡片列表，但不播放编辑按钮渐显动画。
     * <p>
     * 供需要在刷新后立即恢复编辑态的场景使用（例如编辑期间删除其他卡片），
     * 避免按钮先渐显又立刻渐隐造成闪烁。
     * </p>
     */
    private void reloadCards() {
        // 先停止所有编辑切换动画，避免回调继续操作即将被移除的节点
        for (Node card : cardList.getChildren()) {
            stopCardEditAnimation(card);
        }
        // 动画中断时可能仍处于交互禁用状态，统一恢复
        setCardEditInteractionLocked(false);

        // 任何一次全量刷新都会结束当前编辑态，避免输入框引用指向已被移除的节点
        resetEditingState();
        // 以磁盘数据为准重建内存列表，保证界面与持久化一致
        currentCards.clear();
        currentCards.addAll(reminderCardService.readCards());
        cardList.getChildren().clear();
        for (int i = 0; i < currentCards.size(); i++) {
            cardList.getChildren().add(renderCard(currentCards.get(i), i));
        }
        // 没有卡片时显示占位提示；有卡片时隐藏占位并释放布局空间
        boolean isEmpty = currentCards.isEmpty();
        emptyPlaceholder.setVisible(isEmpty);
        emptyPlaceholder.setManaged(isEmpty);
        if (isEmpty) {
            updateEmptyHintText();
        }
    }

    /**
     * 停止卡片上的编辑切换动画并清理节点属性。
     *
     * @param card 卡片根节点
     */
    private void stopCardEditAnimation(Node card) {
        Object animation = card.getProperties().remove(CARD_EDIT_ANIM_KEY);
        if (animation instanceof Animation playable) {
            playable.stop();
        }
    }

    /**
     * 停止卡片按钮栏的展开/收起动画。
     * <p>
     * 进入编辑时卡片高度将交给编辑切换动画控制，必须先停掉可能仍在运行的
     * 按钮栏高度动画，避免两个 Timeline 同时修改同一个高度属性。
     * </p>
     *
     * @param cardRoot 卡片根节点
     */
    private void stopCardButtonBarAnimation(Region cardRoot) {
        Object buttonBar = cardRoot.getProperties().get(CardRenderer.BUTTON_BAR_PROPERTY);
        if (buttonBar instanceof Region bar) {
            Object animation = bar.getProperties().remove(BUTTON_BAR_ANIM_KEY);
            if (animation instanceof Timeline timeline) {
                timeline.stop();
            }
        }
    }

    /**
     * 统一重置编辑态字段。
     * <p>
     * 全量刷新与退出编辑动画结束时都需要清空输入框引用和编辑标记，
     * 避免后续逻辑读取到已移除节点中的数据。
     * </p>
     */
    private void resetEditingState() {
        editingCardIndex = -1;
        editingNewCard = false;
        cardTitleField = null;
        cardContentField = null;
        editingCardSeverity = null;
    }

    /**
     * 设置编辑切换动画期间的卡片交互状态。
     * <p>
     * 高度动画过程中卡片节点仍在重新布局，暂时禁止点击可以避免
     * 重复保存、取消或删除导致索引错位。
     * </p>
     *
     * @param locked {@code true} 表示禁用卡片点击与新增按钮
     */
    private void setCardEditInteractionLocked(boolean locked) {
        cardList.setMouseTransparent(locked);
        addCardButton.setDisable(locked);
    }

    /**
     * 测量卡片内容体在卡片当前宽度下的首选高度。
     *
     * @param body     卡片内容体（只读或编辑）
     * @param cardRoot 卡片根节点，用于获取可用宽度
     * @return 至少为 1 的首选高度
     */
    private double measureCardBodyHeight(Region body, Region cardRoot) {
        // 确保新增的未托管测量节点已经应用主题 CSS
        cardRoot.applyCss();
        body.applyCss();

        double width = cardRoot.getWidth();
        if (width <= 0) width = cardList.getWidth();
        if (width <= 0) width = cardList.getPrefWidth();
        // 扣掉卡片根节点内边距，得到内容体实际可用宽度
        width -= cardRoot.getInsets().getLeft() + cardRoot.getInsets().getRight();
        width = Math.max(width, 1);
        return Math.max(body.prefHeight(width), 1);
    }

    /**
     * 将单条卡片数据渲染为可加入列表的 VBox 节点。
     * <p>
     * 把本类的回调与当前字体配置一并传给 {@link CardRenderer}，
     * 使渲染器无需反向依赖本类。
     * </p>
     *
     * @param item  卡片数据
     * @param index 卡片下标
     * @return 卡片节点
     */
    private VBox renderCard(CardItem item, int index) {
        return cardRenderer.renderCard(
            item, index, cardFontFamily, cardFontSize,
            onEditMainClicked,
            this::startEditCard,
            this::deleteCardWithAnimation,
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
        // 标题与正文都为空（或只有空白字符）的卡片视为无效，启动时统一清理
        boolean removed = cards.removeIf(card ->
            (card.getTitle() == null || card.getTitle().trim().isEmpty())
            && (card.getContent() == null || card.getContent().trim().isEmpty())
        );
        // 只有确实删除了卡片才回写，避免无意义的磁盘 IO
        if (removed) {
            reminderCardService.writeCards(cards);
        }
    }

    /**
     * 清理正在编辑的新卡片：若新建卡片标题与内容均为空，则将其删除。
     */
    public void cleanupEditingCard() {
        // 仅处理「正在编辑的新建卡片」；编辑已有卡片时不应在退出时删除原卡片
        if (editingNewCard && editingCardIndex >= 0) {
            String title = cardTitleField != null ? cardTitleField.getText().trim() : "";
            String content = cardContentField != null ? cardContentField.getText().trim() : "";
            // 新建卡片完全没填内容 → 退出时静默删除，避免留下空卡片
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
        // 用迭代器边遍历边删除，避免 ConcurrentModificationException
        var iterator = cards.iterator();
        while (iterator.hasNext()) {
            CardItem card = iterator.next();
            // 只清理「带图片且非持久化」的临时图片卡片；常驻图片卡片需要保留
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
     * 根据当前是否可编辑，设置空状态提示文案（可编辑/锁定两种）。
     * <p>
     * 锁定状态下提示用户先解锁，解锁状态下提示可以直接添加卡片。
     * </p>
     */
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
        // 没有卡片在编辑：直接继续后续操作
        if (editingCardIndex < 0) {
            onProceed.run();
            return true;
        }

        // 读取编辑框当前内容（可能为 null，故做空值兜底）
        String title = cardTitleField != null ? cardTitleField.getText().trim() : "";
        String content = cardContentField != null ? cardContentField.getText().trim() : "";

        if (editingNewCard) {
            // ========== 情况 A：正在编辑一张新建的卡片 ==========
            if (title.isEmpty() && content.isEmpty()) {
                // A1：完全没填 → 直接删除空卡片，继续操作
                deleteCard(editingCardIndex);
                onProceed.run();
                return true;
            } else if (!title.isEmpty() && !content.isEmpty()) {
                // A2：标题和正文都填了 → 弹框让用户选择「保存」或「丢弃」
                int idx = editingCardIndex;
                cardRenderer.showCardSaveConfirmDialog(
                    () -> {
                        // 保存：写回卡片 → 持久化 → 刷新列表 → 关闭弹框 → 继续操作
                        CardItem item = currentCards.get(idx);
                        item.setTitle(title);
                        item.setContent(content);
                        if (editingCardSeverity != null) {
                            item.setSeverity(editingCardSeverity);
                        }
                        reminderCardService.updateCard(idx, item);
                        // 不在这里提前清空编辑状态：loadCards 会统一重置并触发编辑按钮渐显
                        loadCards();
                        hideConfirmDialogSync();
                        onProceed.run();
                    },
                    () -> {
                        // 丢弃：删除这张新建卡片 → 关闭弹框 → 继续操作
                        deleteCard(idx);
                        hideConfirmDialogSync();
                        onProceed.run();
                    },
                    () -> popupService.closePopup(),
                    true
                );
                return false;
            } else {
                // A3：只填了标题或正文之一 → 内容不完整，只能丢弃（保存按钮改为「返回编辑」）
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
            // ========== 情况 B：正在编辑一张已有卡片 ==========
            final int idx = editingCardIndex;
            final CardItem item = currentCards.get(idx);

            // 丢弃修改并继续：从磁盘重载（还原原内容并结束编辑态）、关闭弹框、继续操作
            Runnable resetAndProceed = () -> {
                // 编辑状态由 loadCards 统一重置，同时触发其他卡片编辑按钮渐显
                loadCards();
                hideConfirmDialogSync();
                onProceed.run();
            };
            // 保存修改并继续：内容完整才写回，否则等同于丢弃
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
                // B1：内容完整 → 提供保存 / 丢弃 / 取消三个选项
                cardRenderer.showCardSaveConfirmDialog(
                    saveAndProceed,
                    resetAndProceed,
                    () -> popupService.closePopup(),
                    true
                );
            } else {
                // B2：内容不完整 → 没有可保存内容，只提供「丢弃」
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
     * 新增卡片：先处理当前编辑状态，再创建一条空卡片并播放插入动画，动画结束后进入编辑。
     */
    public void onAddCard() {
        // 先处理可能存在的编辑态；处理完成（同步或弹框确认后）再执行闭包
        checkActiveCardEditing(() -> {
            // 创建一张空的 INFO 卡片，时间戳为今天，创建日期用于「是否属于今天」判断
            CardItem newItem = new CardItem(
                CardItem.Severity.INFO, "", "", ReminderCardService.generateTimestamp()
            );
            newItem.setCreatedDate(Idf.year + Idf.month + Idf.day);
            // 先落盘再播放动画，动画结束后统一刷新列表
            reminderCardService.addCardAtTop(newItem);
            playCardInsertAnimation(newItem);
        });
    }

    /**
     * 播放新增卡片的插入动画。
     * <p>
     * 在列表顶部插入一个高度为 0 的占位容器，让已有卡片先保持原位；
     * 随后占位容器高度从 0 增长到新卡片高度，已有卡片随布局平滑下移。
     * 新卡片放在占位容器中央，缩放开销与容器高度同步，从中心由小变大，
     * 最终正好铺满让出的空间。
     * </p>
     *
     * @param item 已持久化的新卡片数据
     */
    private void playCardInsertAnimation(CardItem item) {
        // 动画期间禁止操作旧卡片节点：此时列表下标尚未加入新卡片，点击会导致索引错位
        cardList.setMouseTransparent(true);
        addCardButton.setDisable(true);
        // 空状态占位会与新卡片重叠，先隐藏；动画结束后由 loadCards 恢复正确状态
        emptyPlaceholder.setVisible(false);
        emptyPlaceholder.setManaged(false);
        // 新卡片插入在顶部，确保动画在可视区域内
        cardScrollPane.setVvalue(0);

        // 构建与最终编辑态一致的临时卡片，保证动画结束后的高度不跳变
        VBox animatedCard = buildInsertAnimationCard(item, 0);

        // 插入占位容器：初始高度 0，已有卡片保持原位
        StackPane insertionSlot = new StackPane(animatedCard);
        insertionSlot.setMinHeight(0);
        insertionSlot.setPrefHeight(0);
        insertionSlot.setMaxHeight(0);
        cardList.getChildren().add(0, insertionSlot);

        // 先应用 CSS 并布局，才能测量编辑态卡片的真实高度
        cardList.applyCss();
        cardList.layout();
        double listWidth = cardList.getWidth();
        if (listWidth <= 0) {
            // 极端情况下尚未布局完成，退化为使用首选宽度避免测量为 0
            listWidth = cardList.getPrefWidth();
        }
        double contentWidth = Math.max(
            listWidth - cardList.getInsets().getLeft() - cardList.getInsets().getRight(), 1);
        double targetHeight = Math.max(animatedCard.prefHeight(contentWidth), 1);

        // 固定卡片布局高度，视觉尺寸完全交给 scale 控制，避免被占位容器压扁
        animatedCard.setMinHeight(targetHeight);
        animatedCard.setPrefHeight(targetHeight);
        animatedCard.setMaxHeight(targetHeight);
        animatedCard.setScaleX(0.0);
        animatedCard.setScaleY(0.0);

        Timeline animation = new Timeline(
            new KeyFrame(CARD_INSERT_ANIM_DURATION,
                // 首选高度与最大高度同步使用弹簧曲线，允许过冲并真实参与 VBox 布局
                new KeyValue(insertionSlot.prefHeightProperty(), targetHeight, MotionEasing.SPRING),
                new KeyValue(insertionSlot.maxHeightProperty(), targetHeight, MotionEasing.SPRING),
                // 缩放使用同一条弹簧曲线，保证任意时刻卡片视觉高度与预留空间一致
                new KeyValue(animatedCard.scaleXProperty(), 1.0, MotionEasing.SPRING),
                new KeyValue(animatedCard.scaleYProperty(), 1.0, MotionEasing.SPRING)
            )
        );
        animation.setOnFinished(event -> {
            // 恢复交互并重建列表：同步新卡片数据、修正所有卡片按钮的下标
            cardList.setMouseTransparent(false);
            addCardButton.setDisable(false);
            loadCards();
            if (!currentCards.isEmpty()) {
                // 动画结束后让顶部新卡片进入编辑态
                editingNewCard = true;
                editingCardIndex = 0;
                cardScrollPane.setVvalue(0);
                // 插入动画已经把卡片展开到编辑高度，这里静默替换内容即可
                startEditCard(0, false);
            }
        });
        animation.play();
    }

    /**
     * 播放删除卡片时的收起动画，动画结束后再真正删除数据并刷新列表。
     *
     * @param index 要删除的卡片下标
     */
    private void deleteCardWithAnimation(int index) {
        // 索引失效时退化为直接删除，避免操作不存在的节点
        if (index < 0 || index >= cardList.getChildren().size()) {
            deleteCard(index);
            return;
        }
        // 节点已经在播放删除动画时忽略重复点击
        if (cardList.getChildren().get(index).getProperties().containsKey(CARD_REMOVE_ANIM_KEY)) {
            return;
        }
        playCardRemoveAnimation(index, CARD_DELETE_ANIM_DURATION);
    }

    /**
     * 播放新建卡片取消时的反向收起动画。
     * <p>
     * 插入动画是「顶部空间撑开 + 卡片放大」，取消时按相反方向执行：
     * 卡片高度收缩到 0、缩放回到 0，下方卡片随空间收回向上移动；
     * 动画结束后再删除数据并刷新列表，避免卡片瞬间消失造成跳变。
     * </p>
     *
     * @param index 要移除的卡片下标
     */
    private void playCardRemoveAnimation(int index) {
        playCardRemoveAnimation(index, CARD_REMOVE_ANIM_DURATION);
    }

    /**
     * 播放卡片收起动画，动画结束后删除数据并刷新列表。
     *
     * @param index    要移除的卡片下标
     * @param duration 动画时长
     */
    private void playCardRemoveAnimation(int index, Duration duration) {
        // 索引失效时直接删除，避免访问不存在的节点
        if (index < 0 || index >= cardList.getChildren().size()) {
            deleteCard(index);
            return;
        }

        Region cardNode = (Region) cardList.getChildren().get(index);
        // 防止重复点击触发多个动画与多次删除
        if (cardNode.getProperties().containsKey(CARD_REMOVE_ANIM_KEY)) {
            return;
        }
        // 动画期间禁止继续点击卡片或新增按钮，避免重复触发删除
        cardList.setMouseTransparent(true);
        addCardButton.setDisable(true);

        // 先完成一次布局，拿到卡片当前真实高度；尚未布局时退化为按内容测量
        cardList.applyCss();
        cardList.layout();
        double startHeight = cardNode.getHeight();
        if (startHeight <= 0) {
            double listWidth = cardList.getWidth();
            if (listWidth <= 0) {
                listWidth = cardList.getPrefWidth();
            }
            double contentWidth = Math.max(
                listWidth - cardList.getInsets().getLeft() - cardList.getInsets().getRight(), 1);
            startHeight = Math.max(cardNode.prefHeight(contentWidth), 1);
        }

        // 固定初始高度，再同步收缩首选/最大高度，避免内容把卡片重新撑开
        cardNode.setMinHeight(0);
        cardNode.setPrefHeight(startHeight);
        cardNode.setMaxHeight(startHeight);
        cardNode.setScaleX(1.0);
        cardNode.setScaleY(1.0);

        Timeline animation = new Timeline(
            new KeyFrame(duration,
                // 高度与缩放使用同一条非线性减速曲线同步收缩，不使用弹簧回弹
                new KeyValue(cardNode.prefHeightProperty(), 0, MotionEasing.EASE_OUT_CUBIC),
                new KeyValue(cardNode.maxHeightProperty(), 0, MotionEasing.EASE_OUT_CUBIC),
                new KeyValue(cardNode.scaleXProperty(), 0.0, MotionEasing.EASE_OUT_CUBIC),
                new KeyValue(cardNode.scaleYProperty(), 0.0, MotionEasing.EASE_OUT_CUBIC)
            )
        );
        animation.setOnFinished(event -> {
            cardNode.getProperties().remove(CARD_REMOVE_ANIM_KEY);
            // 动画结束后删除数据并重建列表，交互状态由重建后的节点接管
            cardList.setMouseTransparent(false);
            addCardButton.setDisable(false);
            deleteCard(index);
        });
        cardNode.getProperties().put(CARD_REMOVE_ANIM_KEY, animation);
        animation.play();
    }

    /**
     * 构建插入动画使用的编辑态卡片节点。
     * <p>
     * 结构与 {@link #startEditCard(int)} 最终生成的节点一致，使测量高度准确；
     * 动画期间整张卡片不可操作，因此保存/取消回调使用空实现。
     * </p>
     *
     * @param item  新卡片数据
     * @param index 卡片下标
     * @return 含编辑控件的卡片根节点
     */
    private VBox buildInsertAnimationCard(CardItem item, int index) {
        VBox cardRoot = new VBox();
        cardRoot.getStyleClass().addAll("card-item", CardRenderer.severityStyleClass(item.getSeverity()));
        cardRoot.setFillWidth(true);

        VBox editBody = cardRenderer.createEditCardBody(
            item, index, cardFontFamily, cardFontSize,
            () -> { }, () -> { },
            null, null, severity -> { }
        );
        cardRoot.getChildren().add(editBody);
        return cardRoot;
    }

    /**
     * 以淡出动画折叠「添加卡片」按钮，并更新空状态提示。
     * <p>
     * 锁定状态下不允许新增卡片，因此按钮需要淡出并从布局中移除。
     * </p>
     */
    public void collapseAddCardBox() {
        addCardButton.setManaged(false);
        Timeline fadeOut = new Timeline(
            new KeyFrame(ADD_BUTTON_FADE_DURATION,
                new KeyValue(addCardButton.opacityProperty(), 0, Interpolator.EASE_OUT)
            )
        );
        fadeOut.setOnFinished(e -> {
            addCardButton.setVisible(false);
            // 无卡片时占位提示需要从「可添加」切换为「请先解锁」
            if (currentCards.isEmpty()) {
                updateEmptyHintText();
            }
        });
        fadeOut.play();
    }

    /**
     * 以淡入动画展开「添加卡片」按钮，并更新空状态提示。
     * <p>
     * 解锁后恢复新增卡片能力。
     * </p>
     */
    public void expandAddCardBox() {
        addCardButton.setVisible(true);
        addCardButton.setManaged(true);
        Timeline fadeIn = new Timeline(
            new KeyFrame(ADD_BUTTON_FADE_DURATION,
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
     * 为 SplitPane 分隔条注册监听：当拖动位置比例达到 {@link #CARD_COLLAPSE_THRESHOLD}
     * 及以上时折叠卡片区域，回到阈值以下时恢复展开，并记录折叠前的位置。
     * <p>
     * 折叠通过同时设置 {@code managed=false} 和 {@code visible=false} 实现，
     * 这样卡片区域既不显示也不占用布局空间，主区可获得全部宽度。
     * </p>
     */
    public void setupSplitPaneListener() {
        mainSplitPane.getDividers().get(0).positionProperty().addListener((obs, old, val) -> {
            double pos = val.doubleValue();

            if (pos >= CARD_COLLAPSE_THRESHOLD && isCardExpanded) {
                // 折叠：记录折叠前位置，便于理论上恢复（当前实现仅保存）
                lastDividerPosition = old.doubleValue();
                isCardExpanded = false;
                cardContainer.setManaged(false);
                cardContainer.setVisible(false);
            } else if (pos < CARD_COLLAPSE_THRESHOLD && !isCardExpanded) {
                // 恢复展开
                isCardExpanded = true;
                cardContainer.setManaged(true);
                cardContainer.setVisible(true);
            }
        });
    }

    /**
     * 将指定下标的卡片切换为内联编辑态（图片卡片不可编辑）。
     * 已有卡片会播放展开动画，新建卡片由插入动画结束后静默进入编辑。
     *
     * @param index 卡片下标
     */
    private void startEditCard(int index) {
        startEditCard(index, true);
    }

    /**
     * 将指定下标的卡片切换为内联编辑态。
     *
     * @param index   卡片下标
     * @param animate {@code true} 表示播放展开与渐隐/渐显动画；
     *                {@code false} 表示直接替换内容（新建卡片插入后使用）
     */
    private void startEditCard(int index, boolean animate) {
        if (index < 0 || index >= currentCards.size() || index >= cardList.getChildren().size()) {
            return;
        }
        CardItem item = currentCards.get(index);
        // 图片卡片没有可编辑的文字内容，直接忽略编辑请求
        if (item.getImagePath() != null && !item.getImagePath().isEmpty()) return;

        Node cardNode = cardList.getChildren().get(index);
        if (!(cardNode instanceof VBox cardRoot) || cardRoot.getChildren().isEmpty()) return;
        // 同一张卡片已有切换动画时直接忽略，避免重复点击打乱状态
        if (cardRoot.getProperties().containsKey(CARD_EDIT_ANIM_KEY)) return;

        // 进入编辑前停止按钮栏展开/收起动画，避免两套高度动画同时修改卡片高度
        stopCardButtonBarAnimation(cardRoot);

        // 记录编辑状态：下标、是否为新建、当前严重性
        this.editingCardIndex = index;
        this.editingCardSeverity = item.getSeverity();

        // 编辑期间渐隐其他卡片的编辑按钮，避免用户同时进入多张卡片的编辑态
        fadeOutOtherEditButtons(index);

        // 严重性可能在编辑中被切换，移除旧的配色类后重新附加
        cardRoot.getStyleClass().removeAll("info", "warning", "critical");
        cardRoot.getStyleClass().add(CardRenderer.severityStyleClass(item.getSeverity()));

        // 创建编辑控件，并把输入框/严重性回调登记到本类字段，供切换场景时读取
        VBox editBody = cardRenderer.createEditCardBody(
            item, index, cardFontFamily, cardFontSize,
            () -> saveEditCard(index),
            () -> cancelEditCard(),
            tf -> this.cardTitleField = tf,
            ta -> this.cardContentField = ta,
            sev -> {
                this.editingCardSeverity = sev;
                // 立即切换卡片配色，并以渐变方式从旧颜色过渡到目标主题色
                applySeverityWithColorFade(cardRoot, sev);
            }
        );

        if (!animate) {
            // 静默路径：直接替换内容，并把可能残留的显式高度恢复为自动尺寸
            cardRoot.getChildren().setAll(editBody);
            resetCardRootHeight(cardRoot);
            return;
        }

        VBox readOnlyBody = (VBox) cardRoot.getChildren().get(0);
        readOnlyBody.setOpacity(1.0);
        // 编辑体先作为未托管子节点加入，仅用于 CSS 应用和高度测量
        editBody.setManaged(false);
        editBody.setVisible(true);
        editBody.setOpacity(0);
        cardRoot.getChildren().add(editBody);

        double targetHeight = measureCardBodyHeight(editBody, cardRoot);
        double startHeight = Math.max(cardRoot.getHeight(), 1);

        // 固定卡片高度，让父级 VBox 随动画平滑下移后续卡片
        cardRoot.setMinHeight(0);
        cardRoot.setPrefHeight(startHeight);
        cardRoot.setMaxHeight(startHeight);
        // 动画期间禁止卡片交互，避免保存/取消被重复触发
        setCardEditInteractionLocked(true);

        Timeline heightAnimation = new Timeline(
            new KeyFrame(CARD_EDIT_HEIGHT_ANIM_DURATION,
                // 高度使用弹簧曲线，允许轻微回弹
                new KeyValue(cardRoot.prefHeightProperty(), targetHeight, MotionEasing.SPRING),
                new KeyValue(cardRoot.maxHeightProperty(), targetHeight, MotionEasing.SPRING)
            )
        );
        Timeline fadeOut = new Timeline(
            new KeyFrame(CARD_EDIT_FADE_OUT_DURATION,
                event -> {
                    // 只读内容淡出后在弹簧前段完成切换，后续渐显与弹簧收束重叠
                    if (cardRoot.getParent() != cardList) return;
                    cardRoot.getChildren().remove(readOnlyBody);
                    readOnlyBody.setOpacity(1.0);
                    editBody.setManaged(true);
                    editBody.setOpacity(0);
                },
                // 淡出使用无过冲弹簧曲线，与高度动画的加速/减速节奏一致
                new KeyValue(readOnlyBody.opacityProperty(), 0.0, MotionEasing.SPRING_FADE)
            )
        );
        // 渐显紧接淡出，并恰好填满高度动画剩余时间
        ParallelTransition animation = new ParallelTransition(
            heightAnimation,
            new SequentialTransition(fadeOut, createEditFadeIn(editBody))
        );
        animation.setOnFinished(event -> {
            cardRoot.getProperties().remove(CARD_EDIT_ANIM_KEY);
            if (cardRoot.getParent() == cardList) {
                resetCardRootHeight(cardRoot);
            }
            setCardEditInteractionLocked(false);
        });
        cardRoot.getProperties().put(CARD_EDIT_ANIM_KEY, animation);
        animation.play();
    }

    /**
     * 切换卡片的警示程度，并让背景色从旧颜色平滑渐变到新主题色。
     * <p>
     * 颜色取自卡片当前的 CSS 计算背景色，因此 darkness/light/paper 三套主题都能得到
     * 各自的正确配色。动画结束后清除程序化设置的背景，让 CSS 重新接管
     * （包括悬停高亮等状态样式）。
     * </p>
     *
     * @param cardRoot 卡片根节点
     * @param severity 目标警示程度
     */
    private void applySeverityWithColorFade(VBox cardRoot, CardItem.Severity severity) {
        // 快速连续切换时先停止上一段渐变动画，避免多个动画同时改写背景色
        stopSeverityColorAnimation(cardRoot);

        // 渐变起点：切换样式类之前的当前背景色
        Color from = readCardBackgroundColor(cardRoot);

        // 先切换样式类并立即计算 CSS，取得目标主题下的最终背景色
        // 清除上一段动画可能残留的程序化背景，确保取到的是样式表计算值
        cardRoot.setBackground(null);
        cardRoot.getStyleClass().removeAll("info", "warning", "critical");
        cardRoot.getStyleClass().add(CardRenderer.severityStyleClass(severity));
        cardRoot.applyCss();
        Color to = readCardBackgroundColor(cardRoot);

        // 无法读取颜色或颜色未变化时无需播放动画
        if (from == null || to == null || from.equals(to)) {
            return;
        }

        // 保留 CSS 定义的圆角与内边距，仅替换填充色
        BackgroundFill template = cardRoot.getBackground().getFills().get(0);
        ObjectProperty<Color> colorProp = new SimpleObjectProperty<>(from);
        colorProp.addListener((obs, oldColor, newColor) -> cardRoot.setBackground(
            new Background(new BackgroundFill(newColor, template.getRadii(), template.getInsets()))
        ));
        // 以旧色作为起始帧，避免动画启动前目标色短暂闪现
        cardRoot.setBackground(new Background(
            new BackgroundFill(from, template.getRadii(), template.getInsets())));

        Timeline timeline = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(colorProp, from, Interpolator.LINEAR)),
            new KeyFrame(SEVERITY_COLOR_FADE_DURATION,
                new KeyValue(colorProp, to, MotionEasing.EASE_OUT_CUBIC))
        );
        timeline.setOnFinished(event -> {
            // 清除程序化背景并重新标记样式类为脏，让样式表重新接管背景色（恢复 hover 等状态）
            cardRoot.getProperties().remove(SEVERITY_COLOR_ANIM_KEY);
            cardRoot.setBackground(null);
            cardRoot.getStyleClass().removeAll("info", "warning", "critical");
            cardRoot.getStyleClass().add(CardRenderer.severityStyleClass(severity));
            cardRoot.applyCss();
        });
        cardRoot.getProperties().put(SEVERITY_COLOR_ANIM_KEY, timeline);
        timeline.play();
    }

    /**
     * 停止卡片上正在播放的警示程度颜色渐变动画。
     *
     * @param cardRoot 卡片根节点
     */
    private void stopSeverityColorAnimation(VBox cardRoot) {
        Object animation = cardRoot.getProperties().remove(SEVERITY_COLOR_ANIM_KEY);
        if (animation instanceof Timeline timeline) {
            timeline.stop();
        }
    }

    /**
     * 读取卡片根节点当前背景填充中的颜色。
     *
     * @param cardRoot 卡片根节点
     * @return 首个背景填充的颜色；不存在或非纯色时返回 {@code null}
     */
    private Color readCardBackgroundColor(Region cardRoot) {
        Background background = cardRoot.getBackground();
        if (background == null || background.getFills().isEmpty()) {
            return null;
        }
        Paint fill = background.getFills().get(0).getFill();
        return fill instanceof Color color ? color : null;
    }

    /**
     * 创建内容渐显动画。
     *
     * @param body 需要渐显的只读或编辑内容体
     * @return 从完全透明过渡到不透明的动画
     */
    private Timeline createEditFadeIn(Region body) {
        return new Timeline(
            new KeyFrame(CARD_EDIT_FADE_IN_DURATION,
                // 渐显同样使用无过冲弹簧曲线，与弹簧收束保持同一节奏
                new KeyValue(body.opacityProperty(), 1.0, MotionEasing.SPRING_FADE)
            )
        );
    }

    /**
     * 把卡片根节点的高度恢复为自动计算尺寸。
     *
     * @param cardRoot 卡片根节点
     */
    private void resetCardRootHeight(Region cardRoot) {
        cardRoot.setMinHeight(Region.USE_COMPUTED_SIZE);
        cardRoot.setPrefHeight(Region.USE_COMPUTED_SIZE);
        cardRoot.setMaxHeight(Region.USE_COMPUTED_SIZE);
    }

    /**
     * 保存指定卡片的编辑内容。标题或内容为空时不保存，直接返回。
     * <p>
     * 写盘完成后播放编辑退出动画，动画结束后才把只读内容换回卡片。
     * </p>
     */
    private void saveEditCard(int index) {
        if (index < 0 || index >= currentCards.size()) return;
        CardItem item = currentCards.get(index);
        String title = cardTitleField != null ? cardTitleField.getText().trim() : "";
        String content = cardContentField != null ? cardContentField.getText().trim() : "";
        // 标题或正文为空时视为无效输入：不写盘，也不退出编辑态
        if (title.isEmpty() || content.isEmpty()) return;
        item.setTitle(title);
        item.setContent(content);
        // 注意：严重性已由编辑控件直接写到 item 上，这里无需再次设置
        reminderCardService.updateCard(index, item);
        animateEditCardExit(index, item);
    }

    /**
     * 取消卡片编辑：若取消的是新建卡片则将其删除，否则播放退出动画还原只读卡片。
     * <p>
     * 编辑已有卡片时，输入过程中只修改了内存对象；退出动画结束后会用磁盘数据
     * 替换内存对象，因此未保存的修改会被自然丢弃。
     * </p>
     */
    private void cancelEditCard() {
        // 新建卡片取消 = 播反向收起动画，动画结束后再删除这张卡片
        if (editingNewCard && editingCardIndex >= 0) {
            playCardRemoveAnimation(editingCardIndex);
            return;
        }
        if (editingCardIndex < 0) return;
        int index = editingCardIndex;
        List<CardItem> diskCards = reminderCardService.readCards();
        // 防御性检查：磁盘列表异常时退化为直接刷新
        if (index < 0 || index >= diskCards.size()) {
            loadCards();
            return;
        }
        animateEditCardExit(index, diskCards.get(index));
    }

    /**
     * 播放编辑退出动画：编辑内容渐隐，卡片以弹簧曲线收缩到只读高度，
     * 随后切换到目标只读内容并渐显。
     *
     * @param index      卡片下标
     * @param targetItem 退出后的卡片数据（保存时为已更新对象，取消时为磁盘对象）
     */
    private void animateEditCardExit(int index, CardItem targetItem) {
        if (index < 0 || index >= cardList.getChildren().size()) {
            loadCards();
            return;
        }
        Node cardNode = cardList.getChildren().get(index);
        if (!(cardNode instanceof VBox cardRoot) || cardRoot.getChildren().isEmpty()) {
            loadCards();
            return;
        }
        // 已有退出动画时忽略重复点击
        if (cardRoot.getProperties().containsKey(CARD_EDIT_ANIM_KEY)) return;

        VBox editBody = (VBox) cardRoot.getChildren().get(0);

        // 临时渲染目标只读卡片，从中取出内容体并复制交互所需属性
        VBox targetRoot = renderCard(targetItem, index);
        if (targetRoot.getChildren().isEmpty()) {
            loadCards();
            return;
        }
        VBox targetBody = (VBox) targetRoot.getChildren().get(0);
        targetRoot.getChildren().remove(targetBody);
        targetBody.setManaged(false);
        targetBody.setVisible(true);
        targetBody.setOpacity(0);
        cardRoot.getChildren().add(targetBody);

        double targetHeight = measureCardBodyHeight(targetBody, cardRoot);
        double startHeight = Math.max(cardRoot.getHeight(), 1);

        // 收缩时编辑体可能高于卡片当前高度，用裁剪防止内容溢出到相邻卡片
        Rectangle editClip = new Rectangle();
        editClip.widthProperty().bind(cardRoot.widthProperty());
        editClip.heightProperty().bind(cardRoot.heightProperty());
        editBody.setClip(editClip);
        // 目标只读内容在弹簧前段就切换进来，同样需要裁剪到当前卡片高度
        Rectangle targetClip = new Rectangle();
        targetClip.widthProperty().bind(cardRoot.widthProperty());
        targetClip.heightProperty().bind(cardRoot.heightProperty());

        cardRoot.setMinHeight(0);
        cardRoot.setPrefHeight(startHeight);
        cardRoot.setMaxHeight(startHeight);
        setCardEditInteractionLocked(true);

        Timeline heightAnimation = new Timeline(
            new KeyFrame(CARD_EDIT_HEIGHT_ANIM_DURATION,
                // 收起使用无过冲曲线，避免高度越过目标值
                new KeyValue(cardRoot.prefHeightProperty(), targetHeight, MotionEasing.SPRING_EXIT),
                new KeyValue(cardRoot.maxHeightProperty(), targetHeight, MotionEasing.SPRING_EXIT)
            )
        );
        Timeline fadeOut = new Timeline(
            new KeyFrame(CARD_EDIT_FADE_OUT_DURATION,
                event -> {
                    // 编辑内容淡出后在弹簧前段切换只读内容，渐显与剩余收缩重叠
                    if (cardRoot.getParent() != cardList) return;

                    cardRoot.getChildren().remove(editBody);
                    editBody.setClip(null);
                    editBody.setOpacity(1.0);
                    targetBody.setManaged(true);
                    targetBody.setOpacity(0);
                    targetBody.setClip(targetClip);

                    // 把目标只读卡片的按钮栏/编辑按钮引用与严重性样式迁移到现有卡片根节点
                    copyCardProperty(targetRoot, cardRoot, CardRenderer.BUTTON_BAR_PROPERTY);
                    copyCardProperty(targetRoot, cardRoot, CardRenderer.EDIT_BUTTON_PROPERTY);
                    // 退出编辑前停止可能仍在播放的颜色渐变，避免它与最终配色冲突
                    stopSeverityColorAnimation(cardRoot);
                    cardRoot.getStyleClass().removeAll("info", "warning", "critical");
                    cardRoot.getStyleClass().add(CardRenderer.severityStyleClass(targetItem.getSeverity()));

                    // 以目标数据替换内存对象；取消时即丢弃未保存修改
                    if (index < currentCards.size()) {
                        currentCards.set(index, targetItem);
                    }
                    resetEditingState();
                    // 目标卡片的编辑按钮包含在只读内容体里，随整体渐显，无需单独渐显
                    fadeInOtherEditButtons(index);
                },
                // 淡出使用无过冲弹簧曲线，与收缩动画的加速/减速节奏一致
                new KeyValue(editBody.opacityProperty(), 0.0, MotionEasing.SPRING_FADE)
            )
        );
        // 渐显紧接淡出，并恰好填满高度动画剩余时间
        ParallelTransition animation = new ParallelTransition(
            heightAnimation,
            new SequentialTransition(fadeOut, createEditFadeIn(targetBody))
        );
        animation.setOnFinished(event -> {
            cardRoot.getProperties().remove(CARD_EDIT_ANIM_KEY);
            if (cardRoot.getParent() == cardList) {
                resetCardRootHeight(cardRoot);
            }
            // 动画结束后移除只读内容裁剪，恢复由布局正常约束
            targetBody.setClip(null);
            setCardEditInteractionLocked(false);
        });
        cardRoot.getProperties().put(CARD_EDIT_ANIM_KEY, animation);
        animation.play();
    }

    /**
     * 把源卡片根节点上的属性复制到目标卡片根节点。
     * 源节点没有该属性时同时移除目标节点上的旧引用。
     *
     * @param source 源卡片根节点
     * @param target 目标卡片根节点
     * @param key    属性键
     */
    private void copyCardProperty(Node source, Node target, String key) {
        Object value = source.getProperties().get(key);
        if (value == null) {
            target.getProperties().remove(key);
        } else {
            target.getProperties().put(key, value);
        }
    }

    /**
     * 删除指定下标的卡片并刷新列表，同时重置编辑状态。
     * <p>
     * 注意：本方法只删除卡片数据，不负责删除图片文件；
     * 图片卡片的文件清理在 {@link #removeTodayImageCards()} 中统一处理。
     * </p>
     */
    private void deleteCard(int index) {
        // 编辑某张卡片时删除的是另一张卡片：保留当前编辑态与未保存输入
        if (editingCardIndex >= 0 && index != editingCardIndex) {
            deleteCardPreservingEdit(index);
            return;
        }
        reminderCardService.deleteCard(index);
        // 删除后编辑对象已失效；loadCards 会重置编辑态，
        // 并在删除前处于编辑态时触发其他卡片编辑按钮渐显
        loadCards();
    }

    /**
     * 删除一张非编辑中的卡片，同时保留当前卡片的编辑状态和未保存输入。
     * <p>
     * 先捕获编辑框中的草稿内容与严重性，删除目标卡片后重建列表，
     * 再把草稿写回对应的内存对象并重新进入编辑态。草稿只存在于内存中，
     * 不会因为这次删除而落盘。
     * </p>
     *
     * @param index 要删除的卡片下标（不等于 {@link #editingCardIndex}）
     */
    private void deleteCardPreservingEdit(int index) {
        // 防御性检查：下标非法时直接忽略
        if (index < 0 || index >= currentCards.size()) return;

        int editingIndex = editingCardIndex;
        CardItem editingItem = currentCards.get(editingIndex);

        // 捕获编辑中的草稿：标题/正文尚未写盘，严重性与持久化开关也只在内存中
        String draftTitle = cardTitleField != null ? cardTitleField.getText() : "";
        String draftContent = cardContentField != null ? cardContentField.getText() : "";
        CardItem.Severity draftSeverity = editingCardSeverity != null
            ? editingCardSeverity : editingItem.getSeverity();
        boolean draftPersistent = editingItem.isPersistent();
        boolean draftIsNewCard = editingNewCard;

        // 从磁盘删除目标卡片；随后重建列表，使所有节点的按钮下标重新对应
        reminderCardService.deleteCard(index);

        // 被删除的卡片位于编辑卡片之前时，编辑卡片的列表下标需要前移一位
        int newEditingIndex = index < editingIndex ? editingIndex - 1 : editingIndex;
        reloadCards();

        // 把草稿写回重建后的内存对象（不落盘），再重新进入编辑态
        CardItem restoredItem = currentCards.get(newEditingIndex);
        restoredItem.setTitle(draftTitle);
        restoredItem.setContent(draftContent);
        restoredItem.setSeverity(draftSeverity);
        restoredItem.setPersistent(draftPersistent);

        editingNewCard = draftIsNewCard;
        // 列表刚重建完成，直接恢复编辑界面，避免重复播放展开动画
        startEditCard(newEditingIndex, false);
    }

    /**
     * 提交卡片排序结果（静默，不弹出提示）。
     * 在内存与持久化中同步移动卡片位置后刷新列表，越界或位置未变时不做处理。
     *
     * @param fromIndex 被移动卡片的原下标
     * @param toIndex   目标下标
     */
    private void commitReorderSilent(int fromIndex, int toIndex) {
        // 位置未变或下标越界时直接返回，避免无意义的写盘与刷新
        if (fromIndex == toIndex) return;
        if (fromIndex < 0 || fromIndex >= currentCards.size()) return;
        if (toIndex < 0 || toIndex >= currentCards.size()) return;

        // 先在内存列表完成移动，再让持久化层做同样的移动，保证两者顺序一致
        CardItem moved = currentCards.remove(fromIndex);
        currentCards.add(toIndex, moved);
        reminderCardService.moveCard(fromIndex, toIndex);
        loadCards();
    }
}
