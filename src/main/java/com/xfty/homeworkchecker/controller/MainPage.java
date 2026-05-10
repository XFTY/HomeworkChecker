package com.xfty.homeworkchecker.controller;

import com.xfty.homeworkchecker.Entry;
import com.xfty.homeworkchecker.Idf;
import com.xfty.homeworkchecker.model.CardItem;
import com.xfty.homeworkchecker.service.HomeworkDatabase;
import com.xfty.homeworkchecker.service.ui.mainPage.*;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;

public class MainPage implements Initializable {
    // 添加日志记录器
    private static final Logger logger = LoggerFactory.getLogger(MainPage.class);

    // 添加窗口 stage 引用
    private Stage primaryStage;

    @FXML
    private Button screenShotButton;
    @FXML
    private Button historyHomeworkButton;
    @FXML
    private Button settingsButton;
    @FXML
    private Button aboutButton;

    @FXML
    private Label titleLabel;

    @FXML
    private TextArea editMain;
    @FXML
    private HBox lockModelShowingArea;

    @FXML
    private ImageView lockStatusImageView;
    @FXML
    private Label lockStatusLabel;

    @FXML
    private Label versionDisplay;

    @FXML
    private Pane blackPane;

    @FXML
    private AnchorPane centerShowingArea;
    @FXML
    private HBox centerHighControl; // 控制中心窗口高度
    @FXML
    private VBox centerOutBox; // 一般用这个来修改中心窗口鼠标穿透性
    @FXML
    private AnchorPane showingMainArea;
    @FXML
    private SplitPane mainSplitPane;
    @FXML
    private VBox cardContainer;
    @FXML
    private ScrollPane cardScrollPane;
    @FXML
    private VBox cardList;
    @FXML
    private Label addCardLabel;
    @FXML
    private HBox addCardBox;

//    @FXML
//    private Button screenShotSuccess;

    private final List<String> cuteWarnings = Idf.cuteWarningsIdf;

    private double lastDividerPosition = 0.65;
    private boolean isCardExpanded = true;
    private int editingCardIndex = -1;
    private boolean editingNewCard = false;
    private double cardFontSize = 18;
    private String cardFontFamily = "System";

    private final ReminderCardService reminderCardService = new ReminderCardService();
    private List<CardItem> currentCards = new ArrayList<>();

    // 添加用于跟踪连续点击的变量
    private final String originalLockStatusLabel = "已锁定，点击锁头修改";

    HomeworkDatabase homeworkDatabase = new HomeworkDatabase();
    private final String initTemplate = Idf.initTemple;


    private final MainPageInitService mainPageInitService = new MainPageInitService();
    private TopButtonService.ScreenshotService screenshotService;
    private TopButtonService.HistoryHomeworkService historyHomeworkService;
    private TopButtonService.SettingsService settingsService;
    private TopButtonService.AboutService aboutService;
    private PopupService popupService;
    private EditMainService editMainService;
    private EditStateService editStateService;
    private WindowListener windowListener;


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        logger.info("Initializing MainPage...");
        
        // 初始化 Service 层对象
        popupService = new PopupService(
            centerShowingArea, blackPane, centerOutBox, showingMainArea
        );
        screenshotService = new TopButtonService.ScreenshotService(editMain, screenShotButton);
        historyHomeworkService = new TopButtonService.HistoryHomeworkService(popupService);
        settingsService = new TopButtonService.SettingsService(
            popupService, homeworkDatabase, editMain, this::clearTodayHomework
        );
        settingsService.setOnFontChanged(() -> {
            if (Idf.userConfig != null) {
                cardFontFamily = Idf.userConfig.getJSONObject("font")
                    .getJSONObject("fontFamily")
                    .getString("defaultFontFamily");
                cardFontSize = Idf.userConfig.getJSONObject("font")
                    .getJSONObject("textSize")
                    .getInteger("editMain") - 1;
                loadCards();
            }
        });
        aboutService = new TopButtonService.AboutService(popupService);
        editMainService = new EditMainService(lockStatusLabel, lockModelShowingArea);
        editMainService.setEditMain(editMain); // 设置TextArea引用以启用智能缩进
        editStateService = new EditStateService(editMain, homeworkDatabase, this::onLockModuleClicked, () -> editingCardIndex >= 0);
        windowListener = new WindowListener(titleLabel, editMain, this::onLockModuleClicked);
        
        logger.debug("ScreenshotService initialized with editMain reference");
        logger.debug("PopupService initialized with UI components");
        logger.debug("HistoryHomeworkService initialized");
        logger.debug("SettingsService initialized");
        logger.debug("AboutService initialized");
        logger.debug("EditStateService initialized with editMain and homeworkDatabase");
        
        // 记录当前日期和配置信息
        logger.debug("Current date: {}-{}-{}", Idf.year, Idf.month, Idf.day);
        logger.debug("Current editable state: {}", Idf.isEditable);
        logger.debug("User config is null: {}", Idf.userConfig == null);
        logger.debug("User language is null: {}", Idf.userLanguage == null);

        // UI 操作：设置版本号
        versionDisplay.setText("Ver: " + Idf.softwareVersion);
        logger.debug("Version display set to: {}", versionDisplay.getText());

        // UI 操作：添加样式
        historyHomeworkButton.getStyleClass().add("functional-button");
        logger.debug("Title label set to: {}", titleLabel.getText());

        // 修复输入法候选框位置问题
        logger.debug("Fixing input method position");
        // fixInputMethodPosition();

        // UI 操作：添加窗口状态改变监听器
        logger.debug("Adding window listener");
        windowListener.addWindowListener();

        // UI 操作：设置编辑框初始状态
        if (!Idf.isEditable) {
            editMain.setDisable(true);
            logger.debug("EditMain disabled on initialization");
        }

        // 使用 Service 层加载作业内容和标题
        MainPageInitService.Result result = mainPageInitService.loadHomeworkContent();
        editMain.setText(result.getContent());
        titleLabel.setText(result.getTitle());
        
        // UI 操作：设置字体
        MainPageInitService.FontConfig fontConfig = mainPageInitService.getFontConfig();
        editMain.setFont(new Font(fontConfig.getFontFamily(), fontConfig.getFontSize()));
        cardFontSize = fontConfig.getFontSize() - 1;
        cardFontFamily = fontConfig.getFontFamily();
        logger.debug("Font set successfully, card font size: {}, font family: {}", cardFontSize, cardFontFamily);

        // UI 操作：最终设置编辑框状态
        editMain.setDisable(false);
        editMain.setEditable(false);

        loadCards();
        if (!Idf.isEditable) {
            addCardLabel.setOpacity(0);
            addCardLabel.setVisible(false);
            addCardBox.setMinHeight(0);
            addCardBox.setPrefHeight(0);
            addCardBox.setMaxHeight(0);
        }

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

        logger.info("MainPage initialized successfully");
    }

    @FXML
    protected void onscreenShotButtonPressed() {
        screenshotService.takeScreenshot();
    }

    @FXML
    protected void onEditMainClicked() {
        editMainService.onEditMainClicked();
    }

    @FXML
    public void onLockModuleClicked() {
        logger.info("LockModule clicked. Current editable state: {}", Idf.isEditable);
        logger.debug("Current editMain text length: {}", editMain.getText().length());
        
        if (Idf.isEditable) {
            logger.info("Locking module");
            // editMain.setDisable(true);
            editMain.setEditable(false);
            lockStatusImageView.setImage(new Image(Objects.requireNonNull(Entry.class.getResourceAsStream("icon/lock/lock.png"))));
            lockStatusLabel.setText(Idf.userLanguageBundle.getString("main.locked"));
            logger.info("Module locked");

            Idf.homeworkContextCache = editMain.getText();
            logger.debug("Cached homework context length: {}", Idf.homeworkContextCache.length());

            homeworkDatabase.writeHomeworkContextByDay(Idf.homeworkContextCache);
            logger.debug("Homework context saved to database");

            Idf.isEditable = false;
            collapseAddCardBox();
            logger.debug("Editable state set to false");
        } else {
            logger.info("Unlocking module");
            // editMain.setDisable(false);
            editMain.setEditable(true);
            editMain.requestFocus();
            lockStatusImageView.setImage(new Image(Objects.requireNonNull(Entry.class.getResourceAsStream("icon/lock/unlock.png"))));
            lockStatusLabel.setText(Idf.userLanguageBundle.getString("main.unlocked"));
            logger.info("Module unlocked and focus requested");

            // 设置看门狗程序防止有人改完作业不锁定
            logger.debug("Starting unlock counter watchdog");
            editStateService.startUnlockCounter();

            Idf.isEditable = true;
            expandAddCardBox();
            logger.debug("Editable state set to true");
        }
        logger.debug("New editable state: {}", Idf.isEditable);
    }

    @FXML
    protected void onHistoryHomeworkButtonPressed() {
        Idf.isPreviewWindowShowing = true;
        logger.debug("Set isPreviewWindowShowing to true");
        historyHomeworkService.openHistoryHomeworkDialog();
    }

    @FXML
    protected void onSettingsButtonClicked() {
        Idf.isPreviewWindowShowing = true;
        logger.debug("Set isPreviewWindowShowing to true");
        settingsService.openSettingsDialog();
    }

    private void clearTodayHomework() {
        logger.info("Clearing today's homework");
        // String initTemplate = new String(Objects.requireNonNull(Entry.class.getResourceAsStream("/initTemple.txt")).readAllBytes());
        logger.debug("initTemple: {}", Idf.initTemple);
        logger.debug("Current editable state: {}", Idf.isEditable);
        
        if (!Idf.isEditable) {
            logger.debug("Setting editMain to editable to clear content");
            editMain.setEditable(true);
            editMain.setText(Idf.initTemple);
            editMain.setEditable(false);
            logger.info("Homework cleared and editMain set back to non-editable");
        } else {
            editMain.setText(Idf.initTemple);
            logger.info("Homework cleared while in editable mode");
        }
        
        logger.debug("Clear homework operation completed");
    }

    @FXML
    protected void onAboutButtonPressed() {
        Idf.isPreviewWindowShowing = true;
        logger.debug("Set isPreviewWindowShowing to true");
        aboutService.openAboutDialog();
    }

    @FXML
    protected void onAddCard() {
        CardItem newItem = new CardItem(
            CardItem.Severity.INFO, "", "", ReminderCardService.generateTimestamp()
        );
        newItem.setCreatedDate(Idf.year + Idf.month + Idf.day);
        reminderCardService.addCard(newItem);
        loadCards();
        editingNewCard = true;
        editingCardIndex = currentCards.size() - 1;
        startEditCard(editingCardIndex);
    }

    private void collapseAddCardBox() {
        double h = addCardBox.getHeight();
        if (h <= 0) h = addCardBox.prefHeight(-1);
        addCardBox.setMaxHeight(h);
        addCardBox.setPrefHeight(h);

        Timeline fadeLabel = new Timeline(
            new KeyFrame(Duration.millis(100),
                new KeyValue(addCardLabel.opacityProperty(), 0, Interpolator.EASE_OUT)
            )
        );

        Timeline collapse = new Timeline(
            new KeyFrame(Duration.millis(250),
                new KeyValue(addCardBox.prefHeightProperty(), 0, Interpolator.EASE_BOTH)
            )
        );
        collapse.setOnFinished(e -> {
            addCardBox.setMaxHeight(0);
            addCardLabel.setVisible(false);
        });

        new SequentialTransition(fadeLabel, collapse).play();
    }

    private void expandAddCardBox() {
        addCardBox.setMaxHeight(Region.USE_COMPUTED_SIZE);
        addCardBox.setPrefHeight(0);
        addCardLabel.setOpacity(0);
        addCardLabel.setVisible(true);

        double h = addCardBox.prefHeight(-1);
        Timeline expand = new Timeline(
            new KeyFrame(Duration.millis(250),
                new KeyValue(addCardBox.prefHeightProperty(), h, Interpolator.EASE_BOTH)
            )
        );
        expand.setOnFinished(e -> addCardBox.setPrefHeight(Region.USE_COMPUTED_SIZE));

        Timeline fadeLabel = new Timeline(
            new KeyFrame(Duration.millis(100),
                new KeyValue(addCardLabel.opacityProperty(), 1, Interpolator.EASE_OUT)
            )
        );

        new SequentialTransition(expand, fadeLabel).play();
    }

    private void loadCards() {
        currentCards = reminderCardService.readCards();
        cardList.getChildren().clear();
        for (int i = 0; i < currentCards.size(); i++) {
            cardList.getChildren().add(renderCard(currentCards.get(i), i));
        }
    }

    private VBox renderCard(CardItem item, int index) {
        double iconSize = cardFontSize * 1.3;

        VBox cardRoot = new VBox();
        cardRoot.getStyleClass().addAll("card-item", severityStyleClass(item.getSeverity()));
        cardRoot.setFillWidth(true);

        HBox cardBody = new HBox();
        cardBody.getStyleClass().add("card-body");

        ImageView iconView = buildSeverityIcon(item.getSeverity(), iconSize);

        VBox contentArea = new VBox(3);
        contentArea.getStyleClass().add("card-content-area");
        HBox.setHgrow(contentArea, Priority.ALWAYS);

        Label titleLabel = new Label(item.getTitle());
        titleLabel.getStyleClass().add("card-title");
        titleLabel.setFont(Font.font(cardFontFamily, javafx.scene.text.FontWeight.BOLD, cardFontSize));

        Label contentLabel = new Label(item.getContent());
        contentLabel.getStyleClass().add("card-content");
        contentLabel.setFont(Font.font(cardFontFamily, javafx.scene.text.FontWeight.NORMAL, Math.max(cardFontSize - 2, 11)));

        Label timeLabel = new Label(item.getTimestamp());
        timeLabel.getStyleClass().add("card-timestamp");

        contentArea.getChildren().addAll(titleLabel, contentLabel, timeLabel);

        HBox buttonBar = new HBox(4);
        buttonBar.getStyleClass().add("card-button-bar");
        buttonBar.setVisible(false);
        buttonBar.setManaged(false);

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

        cardBody.getChildren().addAll(iconView, contentArea, buttonBar);
        cardRoot.getChildren().add(cardBody);

        cardRoot.setOnMouseEntered(e -> {
            if (editingCardIndex != currentIndex && Idf.isEditable) {
                buttonBar.setVisible(true);
                buttonBar.setManaged(true);
            }
        });
        cardRoot.setOnMouseExited(e -> {
            if (editingCardIndex != currentIndex) {
                buttonBar.setVisible(false);
                buttonBar.setManaged(false);
            }
        });
        cardRoot.setOnMouseClicked(e -> {
            if (!Idf.isEditable) {
                editMainService.onEditMainClicked();
            }
        });

        return cardRoot;
    }

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
        ToggleButton criticalBtn = buildSeverityToggle("icon/card/严重.png", iconSize * 0.55, severityGroup);
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

        TextField contentField = new TextField(item.getContent());
        contentField.getStyleClass().add("card-edit-input");
        contentField.setPromptText(Idf.userLanguageBundle.getString("card.content.placeholder"));
        contentField.setFont(Font.font(cardFontFamily, Math.max(cardFontSize - 2, 11)));

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

        int currentIndex = index;
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
        loadCards();
    }

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
    }

    private void deleteCard(int index) {
        reminderCardService.deleteCard(index);
        editingCardIndex = -1;
        editingNewCard = false;
        loadCards();
    }

    private ToggleButton buildSeverityToggle(String iconPath, double size, ToggleGroup group) {
        ImageView iconView = buildIcon(iconPath, size);
        ToggleButton btn = new ToggleButton();
        btn.setGraphic(iconView);
        btn.setToggleGroup(group);
        btn.getStyleClass().add("card-severity-toggle");
        return btn;
    }

    private ImageView buildSeverityIcon(CardItem.Severity severity, double size) {
        String iconPath = switch (severity) {
            case WARNING -> "icon/card/警告.png";
            case CRITICAL -> "icon/card/严重.png";
            default -> "icon/card/提示.png";
        };
        return buildIcon(iconPath, size);
    }

    private ImageView buildIcon(String path, double size) {
        ImageView view = new ImageView(new Image(Objects.requireNonNull(
            Entry.class.getResourceAsStream(path))));
        view.getStyleClass().add("card-icon");
        view.setFitWidth(size);
        view.setFitHeight(size);
        return view;
    }

    private Button buildIconButton(String iconPath, double size, String... styleClasses) {
        ImageView iconView = buildIcon(iconPath, size);
        Button btn = new Button();
        btn.setGraphic(iconView);
        btn.getStyleClass().addAll(styleClasses);
        return btn;
    }

    private String severityStyleClass(CardItem.Severity severity) {
        return switch (severity) {
            case WARNING -> "warning";
            case CRITICAL -> "critical";
            default -> "info";
        };
    }

    // 添加清理资源的方法
    public void cleanup() {
        logger.info("Starting resource cleanup...");
            
        // 关闭计划任务执行器
        if (editMainService != null) {
            editMainService.cleanup();
            logger.info("EditMainService cleanup completed");
        }
        
        if (editStateService != null) {
            editStateService.stopUnlockCounter();
            logger.info("EditStateService cleanup completed");
        }
            
        logger.info("Resource cleanup completed");
    }
}
