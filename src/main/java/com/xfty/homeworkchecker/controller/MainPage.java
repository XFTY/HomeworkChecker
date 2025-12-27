package com.xfty.homeworkchecker.controller;

import com.xfty.homeworkchecker.Entry;
import com.xfty.homeworkchecker.Idf;
import com.xfty.homeworkchecker.service.HomeworkDatabase;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Point2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.InputMethodEvent;
import javafx.scene.input.InputMethodRequests;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MainPage implements Initializable {
    // 添加日志记录器
    private static final Logger logger = LoggerFactory.getLogger(MainPage.class);

    // 添加窗口stage引用
    private Stage primaryStage;

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

    private final List<String> cuteWarnings = Idf.cuteWarningsIdf;

    // 添加用于跟踪连续点击的变量
    private int clickCount = 0;
    private long lastClickTime = 0;
    private final String originalLockStatusLabel = "已锁定，点击锁头修改";
    private final Random random = new Random();
    private final int CLICK_THRESHOLD = 3;
    private final long TIME_WINDOW = 6000; // 6秒
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private boolean isInCuteMode = false;

    HomeworkDatabase homeworkDatabase = new HomeworkDatabase();
    private final String initTemplate = Idf.initTemple;

    /**
     * 修复输入法候选框位置问题
     * 通过自定义InputMethodRequests实现，使输入法候选框能正确跟随光标位置
     */
    private void fixInputMethodPosition() {
        editMain.setInputMethodRequests(new InputMethodRequests() {
            @Override
            public Point2D getTextLocation(int offset) {
                // 使用默认实现返回TextArea的位置
                return editMain.localToScene(0, 0);
            }

            @Override
            public int getLocationOffset(int x, int y) {
                return 0;
            }

            @Override
            public void cancelLatestCommittedText() {
                // 默认实现即可
            }

            @Override
            public String getSelectedText() {
                return editMain.getSelectedText();
            }
        });
        
        // 监听光标位置变化，强制更新输入法位置
        editMain.caretPositionProperty().addListener((observable, oldValue, newValue) -> {
            // 触发输入法位置更新
            editMain.layout();
        });

        editMain.addEventFilter(InputMethodEvent.INPUT_METHOD_TEXT_CHANGED, event -> {
            editMain.positionCaret(editMain.getCaretPosition());
        });
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        logger.info("Initializing MainPage...");

        versionDisplay.setText("Ver: " + Idf.softwareVersion);

        historyHomeworkButton.getStyleClass().add("functional-button");
        logger.debug("Title label set to: {}", titleLabel.getText());

        // 修复输入法候选框位置问题
        fixInputMethodPosition();

        // 添加窗口状态改变监听器
        addWindowListener();

        if (!Idf.isEditable) {
            editMain.setDisable(true);
            logger.debug("EditMain disabled on initialization");
        }

        // 添加null检查避免空指针异常
        if (Idf.userConfig != null) {
            String todayHomeworkContext = homeworkDatabase.getTodayHomeworkContext();
            if (todayHomeworkContext == null || todayHomeworkContext.isEmpty()) {
                // 获取当前日期
                LocalDate today = LocalDate.now();
                
                // 只有在星期五、六、日时才激活周末作业加载功能
                int dayOfWeek = today.getDayOfWeek().getValue();
                if (dayOfWeek == 5 || dayOfWeek == 6 || dayOfWeek == 7) { // 星期五(5)、星期六(6)、星期日(7)
                    // 计算本周第一天(周一)的日期
                    LocalDate monday = today.minusDays(today.getDayOfWeek().getValue() - 1);

                    // 按优先级顺序检查周日、周六、周五的文件
                    String[] fileNames = {
                            monday.plusDays(6).format(DateTimeFormatter.ofPattern("yyyyMMdd")), // 周日
                            monday.plusDays(5).format(DateTimeFormatter.ofPattern("yyyyMMdd")), // 周六
                            monday.plusDays(4).format(DateTimeFormatter.ofPattern("yyyyMMdd"))  // 周五
                    };

                    String homeworkContent = null;
                    for (String fileName : fileNames) {
                        homeworkContent = homeworkDatabase.getHomeworkContextByFileName(fileName);
                        if (homeworkContent != null) {
                            break; // 找到第一个存在的文件就停止查找
                        }
                    }

                    if (homeworkContent != null) {
                        editMain.setText(homeworkContent);
                        titleLabel.setText(Idf.year + Idf.userLanguageBundle.getString("mainpage.year") + Idf.month + Idf.userLanguageBundle.getString("mainpage.month") + Idf.day + Idf.userLanguageBundle.getString("mainpage.format.weekend"));
                    } else {
                        // 读取initTemple.txt文件内容并设置到editMain中
                        try {
                            // String initTemplate = new String(Objects.requireNonNull(Entry.class.getResourceAsStream("/initTemple.txt")).readAllBytes());
                            editMain.setText(initTemplate);
                            titleLabel.setText(Idf.year + Idf.userLanguageBundle.getString("mainpage.year") + Idf.month + Idf.userLanguageBundle.getString("mainpage.month") + Idf.day + Idf.userLanguageBundle.getString("mainpage.format.weekday"));
                        } catch (Exception e) {
                            logger.error("Failed to load init template", e);
                            editMain.setText(""); // 出错时设置为空字符串
                        }
                    }
                } else {
                    // 非周末时间，直接加载模板文件
                    try {
                        // String initTemplate = new String(Objects.requireNonNull(Entry.class.getResourceAsStream("/initTemple.txt")).readAllBytes());
                        editMain.setText(initTemplate);
                        titleLabel.setText(Idf.year + Idf.userLanguageBundle.getString("mainpage.year") + Idf.month + Idf.userLanguageBundle.getString("mainpage.month") + Idf.day + Idf.userLanguageBundle.getString("mainpage.format.weekday"));
                    } catch (Exception e) {
                        logger.error("Failed to load init template", e);
                        editMain.setText(""); // 出错时设置为空字符串
                    }
                }
            } else {
                editMain.setText(todayHomeworkContext);
                titleLabel.setText(Idf.year + Idf.userLanguageBundle.getString("mainpage.year") + Idf.month + Idf.userLanguageBundle.getString("mainpage.month") + Idf.day + Idf.userLanguageBundle.getString("mainpage.format.weekday"));
            }

            editMain.setFont(new Font(
                Idf.userConfig.getJSONObject("font")
                    .getJSONObject("fontFamily")
                    .getString("defaultFontFamily"),
                Idf.userConfig.getJSONObject("font")
                    .getJSONObject("textSize")
                    .getInteger("editMain")));
        } else {
            logger.warn("User configuration is null, using default settings");
            // 如果配置为null，使用默认文本和字体
            String todayHomeworkContext = homeworkDatabase.getTodayHomeworkContext();
            if (todayHomeworkContext == null || todayHomeworkContext.isEmpty()) {
                // 即使配置为null，也要判断是否为周末来决定加载哪种内容
                LocalDate today = LocalDate.now();
                int dayOfWeek = today.getDayOfWeek().getValue();
                
                if (dayOfWeek == 5 || dayOfWeek == 6 || dayOfWeek == 7) { // 星期五、六、日
                    // 尝试加载周末作业
                    LocalDate monday = today.minusDays(today.getDayOfWeek().getValue() - 1);
                    
                    String[] fileNames = {
                            monday.plusDays(6).format(DateTimeFormatter.ofPattern("yyyyMMdd")), // 周日
                            monday.plusDays(5).format(DateTimeFormatter.ofPattern("yyyyMMdd")), // 周六
                            monday.plusDays(4).format(DateTimeFormatter.ofPattern("yyyyMMdd"))  // 周五
                    };
                    
                    String homeworkContent = null;
                    for (String fileName : fileNames) {
                        homeworkContent = homeworkDatabase.getHomeworkContextByFileName(fileName);
                        if (homeworkContent != null) {
                            break;
                        }
                    }
                    
                    if (homeworkContent != null) {
                        editMain.setText(homeworkContent);
                        titleLabel.setText(Idf.year + Idf.userLanguageBundle.getString("mainpage.year") + Idf.month + Idf.userLanguageBundle.getString("mainpage.month") + Idf.day + Idf.userLanguageBundle.getString("mainpage.format.weekend"));
                    } else {
                        editMain.setText("");
                    }
                } else {
                    // 工作日或无法确定是否为周末时保持空内容
                    editMain.setText("");
                }
            } else {
                editMain.setText(todayHomeworkContext);
            }
            editMain.setFont(new Font("System", 14)); // 默认字体
        }

        editMain.setDisable(false);
        editMain.setEditable(false);
        logger.info("MainPage initialized successfully");
    }

    /**
     * 添加窗口状态监听器，用于监听窗口最大化状态变化
     */
    private void addWindowListener() {
        // 使用Platform.runLater确保在JavaFX线程中执行，且窗口已经显示
        Platform.runLater(() -> {
            // 获取primaryStage
            primaryStage = (Stage) titleLabel.getScene().getWindow();
            
            if (primaryStage != null) {
                // 添加窗口最大化状态变化监听器
                primaryStage.maximizedProperty().addListener((observable, oldValue, newValue) -> {
                    Idf.isMainPageMaximized = newValue;
                    logger.debug("Window maximized state changed to: {}", newValue);
                });
                
                // 添加键盘快捷键监听器
                primaryStage.getScene().setOnKeyPressed(event -> {
                    // 检查是否按下了Ctrl+`组合键
                    if (!Idf.isPreviewWindowShowing) {
                        if (event.isControlDown() && event.getCode() == javafx.scene.input.KeyCode.BACK_QUOTE) {
                            logger.debug("Ctrl+` shortcut pressed");
                            onLockModuleClicked();
                            event.consume(); // 标记事件已被处理
                        }
                    }
                });

                editMain.setOnKeyPressed(event -> {
                    // 检查是否按下了Ctrl+`组合键
                    if (!Idf.isPreviewWindowShowing) {
                        if (event.isControlDown() && event.getCode() == javafx.scene.input.KeyCode.BACK_QUOTE) {
                            logger.debug("Ctrl+` shortcut pressed in editMain");
                            onLockModuleClicked();
                            event.consume(); // 标记事件已被处理
                        }
                    }
                });
            }
        });
    }

    @FXML
    protected void onEditMainClicked() {
        // 如果动画正在播放，则忽略新地触发事件
        if (shakeAnimation != null && shakeAnimation.getStatus() == javafx.animation.Animation.Status.RUNNING) {
            return;
        }

        if (!Idf.isEditable) {
            long currentTime = System.currentTimeMillis();
            
            // 检查是否在时间窗口内
            if (currentTime - lastClickTime <= TIME_WINDOW) {
                clickCount++;
            } else {
                // 超过时间窗口，重置计数
                clickCount = 1;
            }
            
            lastClickTime = currentTime;
            
            // 如果达到点击阈值
            if (clickCount >= CLICK_THRESHOLD) {
                // 设置随机提示文本
                int randomIndex = random.nextInt(cuteWarnings.size());
                lockStatusLabel.setText(cuteWarnings.get(randomIndex));
                isInCuteMode = true;
                
                // 安排6秒后检查是否需要恢复
                scheduler.schedule(this::checkAndRestoreLabel, TIME_WINDOW, TimeUnit.MILLISECONDS);
            }
            
            // 创建特定的左右抖动动画效果
            // 前三个周期幅度保持一致，第四个周期开始衰减，第五个周期停止
            double amplitude = 15.0;  // 固定振幅
            
            // 使用Timeline实现平滑动画
            shakeAnimation = new javafx.animation.Timeline();
            shakeAnimation.setCycleCount(1);
            
            // 定义关键帧列表
            javafx.collections.ObservableList<javafx.animation.KeyFrame> keyFrames = shakeAnimation.getKeyFrames();
            
            double currentTimeAnim = 0;
            
            // 添加关键帧的辅助方法
            addShakeKeyFrames(keyFrames, currentTimeAnim, amplitude, 3); // 前3个完整周期
            currentTimeAnim += 300; // 3个周期的时间 (3 * (50+25+50+25))
            
            // 第四个周期，幅度开始衰减（减小到原来的一半）
            addShakeKeyFrames(keyFrames, currentTimeAnim, amplitude * 0.5, 1);
            currentTimeAnim += 150; // 1个周期的时间 (50+25+50+25)
            
            // 第五个周期，幅度进一步减小（减小到原来的四分之一）
            addShakeKeyFrames(keyFrames, currentTimeAnim, amplitude * 0.25, 1);
            
            shakeAnimation.play();
        } else {
            editMain.requestFocus();
        }
    }

    // 检查是否需要恢复标签文本
    private void checkAndRestoreLabel() {
        long currentTime = System.currentTimeMillis();
        // 检查距离上次点击是否已经超过时间窗口
        if (currentTime - lastClickTime > TIME_WINDOW && isInCuteMode) {
            Platform.runLater(() -> {
                lockStatusLabel.setText(originalLockStatusLabel);
                isInCuteMode = false;
                clickCount = 0; // 重置点击计数
            });
        }
    }

    // 辅助方法：添加指定次数的抖动关键帧
    private void addShakeKeyFrames(javafx.collections.ObservableList<javafx.animation.KeyFrame> keyFrames, 
                               double startTime, double amplitude, int cycles) {
        double currentTime = startTime;
        for (int cycle = 0; cycle < cycles; cycle++) {
            // 向左移动
            keyFrames.add(new javafx.animation.KeyFrame(
                javafx.util.Duration.millis(currentTime),
                new javafx.animation.KeyValue(lockModelShowingArea.translateXProperty(), -amplitude)
            ));
            currentTime += 50;
            
            // 回到中心
            keyFrames.add(new javafx.animation.KeyFrame(
                javafx.util.Duration.millis(currentTime),
                new javafx.animation.KeyValue(lockModelShowingArea.translateXProperty(), 0)
            ));
            currentTime += 25;
            
            // 向右移动
            keyFrames.add(new javafx.animation.KeyFrame(
                javafx.util.Duration.millis(currentTime),
                new javafx.animation.KeyValue(lockModelShowingArea.translateXProperty(), amplitude)
            ));
            currentTime += 50;
            
            // 回到中心
            keyFrames.add(new javafx.animation.KeyFrame(
                javafx.util.Duration.millis(currentTime),
                new javafx.animation.KeyValue(lockModelShowingArea.translateXProperty(), 0)
            ));
            currentTime += 25;
        }
    }

    @FXML
    public void onLockModuleClicked() {
        logger.debug("LockModule clicked. Current editable state: {}", Idf.isEditable);
        if (Idf.isEditable) {
            // editMain.setDisable(true);
            editMain.setEditable(false);
            lockStatusImageView.setImage(new Image(Objects.requireNonNull(Entry.class.getResourceAsStream("icon/lock/lock.png"))));
            lockStatusLabel.setText(Idf.userLanguageBundle.getString("main.locked"));
            logger.info("Module locked");

            Idf.homeworkContextCache = editMain.getText();

            homeworkDatabase.writeHomeworkContextByDay(Idf.homeworkContextCache);

            Idf.isEditable = false;
        } else {
            // editMain.setDisable(false);
            editMain.setEditable(true);
            editMain.requestFocus();
            lockStatusImageView.setImage(new Image(Objects.requireNonNull(Entry.class.getResourceAsStream("icon/lock/unlock.png"))));
            lockStatusLabel.setText(Idf.userLanguageBundle.getString("main.unlocked"));
            logger.info("Module unlocked and focus requested");

            // 设置看门狗程序防止有人改完作业不锁定
            unlockCounter();

            Idf.isEditable = true;
        }
        logger.debug("New editable state: {}", Idf.isEditable);
    }

    private void unlockCounter() {
        new Thread(() -> {
            logger.info("Starting unlock counter watchdog thread");
            int timings = 60;
            logger.debug("Initial timing value: {} seconds", timings);
            
            while (true) {
                if (Idf.isSoftwareClosing) {
                    logger.info("Software is closing, triggering lock module");
                    Platform.runLater(this::onLockModuleClicked);
                    break;
                }
                if (!Idf.isEditable) {
                    logger.info("user locked module, exiting thread ...");
                    break;
                }

                try {
                    logger.debug("Waiting for 5 seconds before next check");
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    logger.error("Unlock counter thread interrupted", e);
                    // 当线程被中断时，检查是否应该关闭软件
                    if (Idf.isSoftwareClosing) {
                        Platform.runLater(this::onLockModuleClicked);
                    }
                    break;
                }

                String editMainText = editMain.getText();
                logger.debug("Current editMain text length: {}", editMainText.length());

                if (!Objects.equals(Idf.homeworkContextCache, editMainText)) {
                    Idf.homeworkContextCache = editMainText;
                    homeworkDatabase.writeHomeworkContextByDay(Idf.homeworkContextCache);
                    timings = 30;
                    logger.info("Content changed, resetting timings to {} seconds", timings);
                } else {
                    timings = timings - 5;
                    logger.debug("Content unchanged, decrementing timings to {} seconds", timings);
                }

                if (Objects.equals(timings, 0)) {
                    logger.info("Timings reached zero, auto-locking module");
                    Platform.runLater(this::onLockModuleClicked);
                    break;
                }
            }
            logger.info("Unlock counter watchdog thread finished");
        }, "UnlockCounterThread").start();
    }

    @FXML
    protected void onHistoryHomeworkButtonPressed() {
        try {
            logger.info("Opening history homework dialog");
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/xfty/homeworkchecker/fxml/loadHistoryHomework.fxml"));
            // Set resource bundle for internationalization
            if (Idf.userLanguage != null && Idf.userLanguage.getString("language") != null) {
                String languageCode = Idf.userLanguage.getString("language");
                String[] languageParts = languageCode.split("_");
                Locale locale = (languageParts.length == 2) ? 
                    new Locale.Builder().setLanguage(languageParts[0]).setRegion(languageParts[1]).build() : 
                    new Locale.Builder().setLanguage(languageParts[0]).build();
                loader.setResources(ResourceBundle.getBundle("com/xfty/homeworkchecker/i18n/language", locale));
            }
            Parent root = loader.load();

            // 设置全局变量
            Idf.isPreviewWindowShowing = true;
            
            // 显示弹窗并处理关闭事件
            showPopupWithCloseHandler(root, "#windowCloseButton");
            
        } catch (Exception e) {
            logger.error("Error opening history homework dialog", e);
        }
    }

    @FXML
    protected void onSettingsButtonClicked() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/xfty/homeworkchecker/fxml/settings.fxml"));
            // Set resource bundle for internationalization
            if (Idf.userLanguage != null && Idf.userLanguage.getString("language") != null) {
                String languageCode = Idf.userLanguage.getString("language");
                String[] languageParts = languageCode.split("_");
                Locale locale = (languageParts.length == 2) ? 
                    new Locale.Builder().setLanguage(languageParts[0]).setRegion(languageParts[1]).build() : 
                    new Locale.Builder().setLanguage(languageParts[0]).build();
                loader.setResources(ResourceBundle.getBundle("com/xfty/homeworkchecker/i18n/language", locale));
            }
            Parent root = loader.load();

            // 设置全局变量
            Idf.isPreviewWindowShowing = true;

            Circle windowCloseButton = showPopupWithCloseHandler(root, "#windowCloseButton");

            // 延迟执行按钮查找，确保界面已完全渲染
            Platform.runLater(() -> {
                try {
                    // 通过lookup方法找到按钮并添加点击监听器
                    Button clearButton = (Button) root.lookup("#clearAllHomeworkContentButton");
                    if (clearButton != null) {
                        logger.debug("Clear button found");
                        clearButton.setOnAction(buttonEvent -> {
                            // 处理按钮点击事件
                            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                            alert.setTitle(Idf.userLanguageBundle.getString("mainpage.confirmReset.title"));
                            alert.setHeaderText(Idf.userLanguageBundle.getString("mainpage.confirmReset.header"));
                            alert.setContentText(Idf.userLanguageBundle.getString("mainpage.confirmReset.content"));

                            alert.showAndWait().ifPresent(response -> {
                                if (response == javafx.scene.control.ButtonType.OK) {
                                    // 执行重置操作
                                    clearTodayHomework();
                                    logger.info("Today's homework content has been cleared");
                                }
                            });
                        });
                    }
                } catch (Exception e) {
                    logger.error("Error setting up clear button listener", e);
                }
            });

            // 特殊关闭处理
            windowCloseButton.setOnMouseClicked(mouseEvent -> {
                homeworkDatabase.updateConfig(Idf.userConfig);
                logger.info("user Closed settings window");
                
                // 添加null检查避免空指针异常
                if (Idf.userConfig != null) {
                    editMain.setFont(new Font(
                        Idf.userConfig.getJSONObject("font")
                            .getJSONObject("fontFamily")
                            .getString("defaultFontFamily"),
                        Idf.userConfig.getJSONObject("font")
                            .getJSONObject("textSize")
                            .getInteger("editMain")));
                }
                
                closePopup();
                
                // 设置全局变量
                Idf.isPreviewWindowShowing = false;
            });

        } catch (Exception e) {
            logger.error("Error opening settings window", e);
        }
    }

    private void clearTodayHomework() {
        // String initTemplate = new String(Objects.requireNonNull(Entry.class.getResourceAsStream("/initTemple.txt")).readAllBytes());
        logger.debug("initTemple: {}", initTemplate);
        if (!Idf.isEditable) {
            editMain.setEditable(true);
            editMain.setText(initTemplate);
            editMain.setEditable(false);
        } else {
            editMain.setText(initTemplate);
        }
    }

    @FXML
    protected void onAboutButtonPressed() {
        logger.info("Opening about homework dialog");

        // 设置全局变量
        Idf.isPreviewWindowShowing = true;

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/xfty/homeworkchecker/fxml/about.fxml"));
            // Set resource bundle for internationalization
            if (Idf.userLanguage != null && Idf.userLanguage.getString("language") != null) {
                String languageCode = Idf.userLanguage.getString("language");
                String[] languageParts = languageCode.split("_");
                Locale locale = (languageParts.length == 2) ? 
                    new Locale.Builder().setLanguage(languageParts[0]).setRegion(languageParts[1]).build() : 
                    new Locale.Builder().setLanguage(languageParts[0]).build();
                loader.setResources(ResourceBundle.getBundle("com/xfty/homeworkchecker/i18n/language", locale));
            }
            Parent root = loader.load();

            // 显示弹窗并处理关闭事件
            showPopupWithCloseHandler(root, "#windowCloseButton");

        } catch (Exception e) {
            logger.error("Error opening about homework dialog", e);
        }
    }
    
    /**
     * 显示弹窗并处理基础的关闭事件
     * @param root 弹窗内容
     * @param closeButtonId 关闭按钮ID
     * @return 关闭按钮对象
     */
    private Circle showPopupWithCloseHandler(Parent root, String closeButtonId) {
        // 设置透明度和缩放，准备淡入动画
        centerShowingArea.setOpacity(0);
        centerShowingArea.setScaleX(0.8);
        centerShowingArea.setScaleY(0.8);
        centerShowingArea.getChildren().clear();
        centerShowingArea.getChildren().add(root);

        GaussianBlur gaussianBlur = new GaussianBlur();
        gaussianBlur.setRadius(0); // 初始模糊半径为0
        showingMainArea.setEffect(gaussianBlur);

        // 淡入动画
        javafx.animation.Timeline fadeInTimeline = new javafx.animation.Timeline();
        
        // 使用弹性插值器创建更生动的效果
        javafx.animation.Interpolator elasticInterpolator = javafx.animation.Interpolator.SPLINE(0.25, 0.1, 0.25, 1.0);
        
        // 模糊度增加的关键帧
        javafx.animation.KeyFrame blurKeyFrame = new javafx.animation.KeyFrame(
            javafx.util.Duration.millis(400),
            new javafx.animation.KeyValue(gaussianBlur.radiusProperty(), 30)
        );
        
        // 透明度增加的关键帧
        javafx.animation.KeyFrame opacityKeyFrame = new javafx.animation.KeyFrame(
            javafx.util.Duration.millis(400),
            new javafx.animation.KeyValue(centerShowingArea.opacityProperty(), 1, elasticInterpolator)
        );
        
        // 缩放动画关键帧
        javafx.animation.KeyFrame scaleXKeyFrame = new javafx.animation.KeyFrame(
            javafx.util.Duration.millis(300),
            new javafx.animation.KeyValue(centerShowingArea.scaleXProperty(), 1.1, elasticInterpolator)
        );
        
        javafx.animation.KeyFrame scaleXKeyFrame2 = new javafx.animation.KeyFrame(
            javafx.util.Duration.millis(500),
            new javafx.animation.KeyValue(centerShowingArea.scaleXProperty(), 1.0, elasticInterpolator)
        );
        
        javafx.animation.KeyFrame scaleYKeyFrame = new javafx.animation.KeyFrame(
            javafx.util.Duration.millis(300),
            new javafx.animation.KeyValue(centerShowingArea.scaleYProperty(), 1.1, elasticInterpolator)
        );
        
        javafx.animation.KeyFrame scaleYKeyFrame2 = new javafx.animation.KeyFrame(
            javafx.util.Duration.millis(500),
            new javafx.animation.KeyValue(centerShowingArea.scaleYProperty(), 1.0, elasticInterpolator)
        );
        
        // 黑色遮罩层透明度增加的关键帧
        javafx.animation.KeyFrame blackPaneKeyFrame = new javafx.animation.KeyFrame(
            javafx.util.Duration.millis(400),
            new javafx.animation.KeyValue(blackPane.opacityProperty(), 0.5, elasticInterpolator)
        );

        fadeInTimeline.getKeyFrames().addAll(
            blurKeyFrame, 
            opacityKeyFrame, 
            blackPaneKeyFrame,
            scaleXKeyFrame,
            scaleXKeyFrame2,
            scaleYKeyFrame,
            scaleYKeyFrame2
        );
        fadeInTimeline.play();

        blackPane.setOpacity(0); // 初始设置为完全透明
        centerOutBox.setMouseTransparent(false);

        Circle windowCloseButton = (Circle) root.lookup(closeButtonId);
        
        // 设置窗口关闭事件
        windowCloseButton.setOnMouseClicked(event -> {
            closePopup();
            
            // 设置全局变量
            Idf.isPreviewWindowShowing = false;
        });
        
        return windowCloseButton;
    }
    
    /**
     * 关闭弹窗的动画效果
     */
    private void closePopup() {
        // 淡出动画
        GaussianBlur gaussianBlur = (GaussianBlur) showingMainArea.getEffect();
        javafx.animation.Timeline fadeOutTimeline = new javafx.animation.Timeline();
        
        // 使用缓动插值器
        javafx.animation.Interpolator easeOutInterpolator = javafx.animation.Interpolator.EASE_OUT;
        
        // 模糊度减少的关键帧
        javafx.animation.KeyFrame blurOutKeyFrame = new javafx.animation.KeyFrame(
            javafx.util.Duration.millis(250),
            new javafx.animation.KeyValue(gaussianBlur.radiusProperty(), 0, easeOutInterpolator)
        );
        
        // 透明度减少的关键帧
        javafx.animation.KeyFrame opacityOutKeyFrame = new javafx.animation.KeyFrame(
            javafx.util.Duration.millis(250),
            new javafx.animation.KeyValue(centerShowingArea.opacityProperty(), 0, easeOutInterpolator)
        );
        
        // 缩放动画关键帧（添加一点收缩效果）
        javafx.animation.KeyFrame scaleOutXKeyFrame = new javafx.animation.KeyFrame(
            javafx.util.Duration.millis(250),
            new javafx.animation.KeyValue(centerShowingArea.scaleXProperty(), 0.8, easeOutInterpolator)
        );
        
        javafx.animation.KeyFrame scaleOutYKeyFrame = new javafx.animation.KeyFrame(
            javafx.util.Duration.millis(250),
            new javafx.animation.KeyValue(centerShowingArea.scaleYProperty(), 0.8, easeOutInterpolator)
        );
        
        // 黑色遮罩层透明度减少的关键帧
        javafx.animation.KeyFrame blackPaneOutKeyFrame = new javafx.animation.KeyFrame(
            javafx.util.Duration.millis(250),
            new javafx.animation.KeyValue(blackPane.opacityProperty(), 0, easeOutInterpolator)
        );
        
        fadeOutTimeline.getKeyFrames().addAll(
            blurOutKeyFrame, 
            opacityOutKeyFrame, 
            blackPaneOutKeyFrame,
            scaleOutXKeyFrame,
            scaleOutYKeyFrame
        );
        fadeOutTimeline.setOnFinished(e -> {
            centerShowingArea.getChildren().clear();
            centerOutBox.setMouseTransparent(true);
            showingMainArea.setEffect(null);
        });
        fadeOutTimeline.play();
    }

    @FXML
    protected void onEditInitTempleButtonPressed() {
        try {
            // 加载FXML文件
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/xfty/homeworkchecker/fxml/historyHomeworkChecker.fxml"));
            // Set resource bundle for internationalization
            if (Idf.userLanguage != null && Idf.userLanguage.getString("language") != null) {
                String languageCode = Idf.userLanguage.getString("language");
                String[] languageParts = languageCode.split("_");
                Locale locale = (languageParts.length == 2) ? 
                    new Locale.Builder().setLanguage(languageParts[0]).setRegion(languageParts[1]).build() : 
                    new Locale.Builder().setLanguage(languageParts[0]).build();
                loader.setResources(ResourceBundle.getBundle("com/xfty/homeworkchecker/i18n/language", locale));
            }
            Parent root = loader.load();

            // 创建新的舞台
            Stage stageNl = new Stage();
            stageNl.setTitle(Idf.userLanguageBundle.getString("mainpage.initTemple.title"));
            stageNl.initModality(Modality.APPLICATION_MODAL);
            stageNl.setScene(new Scene(root));

            Label showDate = (Label) root.lookup("#showDate");
            showDate.setText(Idf.userLanguageBundle.getString("mainpage.initTemple.title"));

            TextArea editMain = (TextArea) root.lookup("#editMain");
            editMain.setText(Idf.initTemple);

            Label statusDisplay = (Label) root.lookup("#statusDisplay");
            statusDisplay.setText(Idf.userLanguageBundle.getString("mainpage.initTemple.description"));

            stageNl.setOnCloseRequest(windowEvent -> {
                Idf.initTemple = editMain.getText();
                homeworkDatabase.changeInitTemple(Idf.initTemple);
            });

            stageNl.showAndWait();

        } catch (Exception e) {
            logger.error("Failed to open init template editor", e);
        }
    }

    @FXML
    protected void onEditLanguageButtonPressed() {
        try {
            // 加载FXML文件
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/xfty/homeworkchecker/fxml/languageChooser.fxml"));
            // Set resource bundle for internationalization
            if (Idf.userLanguage != null && Idf.userLanguage.getString("language") != null) {
                String languageCode = Idf.userLanguage.getString("language");
                String[] languageParts = languageCode.split("_");
                Locale locale = (languageParts.length == 2) ? 
                    new Locale.Builder().setLanguage(languageParts[0]).setRegion(languageParts[1]).build() : 
                    new Locale.Builder().setLanguage(languageParts[0]).build();
                loader.setResources(ResourceBundle.getBundle("com/xfty/homeworkchecker/i18n/language", locale));
            }
            Parent root = loader.load();

            // 创建新的舞台
            Stage stageNl = new Stage();
            stageNl.setTitle("语言 | Language ");
            stageNl.initModality(Modality.APPLICATION_MODAL);
            Scene scene = new Scene(root);
            
            // 添加CSS样式表
            scene.getStylesheets().add(getClass().getResource("/com/xfty/homeworkchecker/theme/darkness/language-button.css").toExternalForm());
            
            stageNl.setScene(scene);
            stageNl.showAndWait();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // 在类中添加动画变量声明
    private javafx.animation.Timeline shakeAnimation;

    // 添加清理资源的方法
    public void cleanup() {
        logger.info("Cleaning up resources...");
        
        // 关闭计划任务执行器
        if (scheduler != null && !scheduler.isShutdown()) {
            logger.info("Shutting down scheduler...");
            scheduler.shutdown();
            try {
                // 等待最多5秒让现有任务完成
                if (!scheduler.awaitTermination(1, TimeUnit.SECONDS)) {
                    logger.warn("Scheduler did not terminate in time, forcing shutdown");
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                logger.error("Interrupted while waiting for scheduler termination", e);
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        logger.info("Resource cleanup completed");
    }
}