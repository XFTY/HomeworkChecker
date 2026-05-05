package com.xfty.homeworkchecker.controller;

import com.xfty.homeworkchecker.Entry;
import com.xfty.homeworkchecker.Idf;
import com.xfty.homeworkchecker.service.HomeworkDatabase;
import com.xfty.homeworkchecker.service.ui.mainPage.*;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
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

//    @FXML
//    private Button screenShotSuccess;

    private final List<String> cuteWarnings = Idf.cuteWarningsIdf;
    
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
        aboutService = new TopButtonService.AboutService(popupService);
        editMainService = new EditMainService(lockStatusLabel, lockModelShowingArea);
        editMainService.setEditMain(editMain); // 设置TextArea引用以启用智能缩进
        editStateService = new EditStateService(editMain, homeworkDatabase, this::onLockModuleClicked);
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
        logger.debug("Font set successfully");

        // UI 操作：最终设置编辑框状态
        editMain.setDisable(false);
        editMain.setEditable(false);
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
