package com.xfty.homeworkchecker.controller.settings;

import com.xfty.homeworkchecker.Entry;
import com.xfty.homeworkchecker.Idf;
import com.xfty.homeworkchecker.service.HomeworkDatabase;
import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.Locale;
import java.util.ResourceBundle;

public class Index implements Initializable {
    @FXML
    private ScrollPane rightShowingArea;
    
    @FXML
    private AnchorPane fontSettingsButton;
    
    @FXML
    private AnchorPane languageSettingsButton;
    
    @FXML
    private AnchorPane initialDataButton;
    
    @FXML
    private AnchorPane dataBaseEditorButton;
    
    @FXML
    private AnchorPane resetButton;

    @FXML
    private AnchorPane updaterButton;
    
    @FXML
    private Circle windowCloseButton;

    @FXML
    private Circle windowZoomButton;

    private Locale locale;

    private String openPageCode;
    
    // 当前选中的按钮
    private AnchorPane currentSelectedButton;
    
    // 高亮颜色
    private static final String DEFAULT_BACKGROUND = "#1e1e1e";
    private static final String HIGHLIGHT_BACKGROUND = "#3a3a3a";
    private static final String HOVER_BACKGROUND = "#2a2a2a";
    private static final String PRESSED_BACKGROUND = "#4a4a4a";

    // 添加日志记录器
    private static final Logger logger = LoggerFactory.getLogger(Index.class);

    private final HomeworkDatabase homeworkDatabase = new HomeworkDatabase();

    private boolean isMaximized = false;
    private double originalPrefWidth = 818.0;
    private double originalPrefHeight = 509.0;

    @FXML
    private void onFontSettingsClicked() {
        logger.info("Entering onFontSettingsClicked, current openPageCode: {}", openPageCode);
        
        if (openPageCode != null && openPageCode.equals("homeworkArea")) {
            logger.debug("Font settings page already open, returning");
            return;
        }
        openPageCode = "homeworkArea";
        logger.debug("Setting openPageCode to: {}", openPageCode);
        
        // 高亮对应的按钮
        highlightButton(fontSettingsButton);

        try {
            FXMLLoader fxmlLoader = new FXMLLoader(Entry.class.getResource("fxml/settings/homeworkArea.fxml"));

            if (Idf.userLanguage != null && Idf.userLanguage.getString("language") != null) {
                fxmlLoader.setResources(ResourceBundle.getBundle("com/xfty/homeworkchecker/i18n/language", locale));
                logger.debug("Applied language bundle for locale: {}", locale);
            }

            Parent root = fxmlLoader.load();
            logger.info("Successfully loaded homeworkArea.fxml");
            
            // 应用淡入淡出动画
            applyFadeAnimation(root);
            logger.info("Applied fade animation for homeworkArea page");
        } catch (IOException e) {
            logger.error("Failed to load homeworkArea.fxml", e);
            throw new RuntimeException(e);
        }
        
        logger.info("Exiting onFontSettingsClicked");
    }

    @FXML
    private void onLanguageSettingsClicked() {
        logger.info("Entering onLanguageSettingsClicked, current openPageCode: {}", openPageCode);
        
        if (openPageCode != null && openPageCode.equals("languageChooser")) {
            logger.debug("Language settings page already open, returning");
            return;
        }
        openPageCode = "languageChooser";
        logger.debug("Setting openPageCode to: {}", openPageCode);
        
        // 高亮对应的按钮
        highlightButton(languageSettingsButton);

        try {
            FXMLLoader fxmlLoader = new FXMLLoader(Entry.class.getResource("fxml/languageChooser.fxml"));
            if (Idf.userLanguage != null && Idf.userLanguage.getString("language") != null) {
                fxmlLoader.setResources(ResourceBundle.getBundle("com/xfty/homeworkchecker/i18n/language", locale));
                logger.debug("Applied language bundle for locale: {}", locale);
            }

            Parent root = fxmlLoader.load();
            logger.info("Successfully loaded languageChooser.fxml");
            
            // 应用淡入淡出动画
            applyFadeAnimation(root);
            logger.info("Applied fade animation for languageChooser page");
        } catch (IOException e) {
            logger.error("Failed to load languageChooser.fxml", e);
            throw new RuntimeException(e);
        }
        
        logger.info("Exiting onLanguageSettingsClicked");
    }

    @FXML
    private void onSetInitialDataClicked() {
        logger.info("Entering onSetInitialDataClicked, current openPageCode: {}", openPageCode);
        
        if (openPageCode != null && openPageCode.equals("setInitialData")) {
            logger.debug("Initial data page already open, returning");
            return;
        }
        openPageCode = "setInitialData";
        logger.debug("Setting openPageCode to: {}", openPageCode);
        
        // 高亮对应的按钮
        highlightButton(initialDataButton);
        
        try {
            // 加载 FXML 文件
            logger.debug("Loading FXML file for initial data editor");
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/xfty/homeworkchecker/fxml/historyHomeworkChecker.fxml"));
            // Set resource bundle for internationalization
            if (Idf.userLanguage != null && Idf.userLanguage.getString("language") != null) {
                loader.setResources(ResourceBundle.getBundle("com/xfty/homeworkchecker/i18n/language", locale));
                logger.debug("Applied language bundle for locale: {}", locale);
            }
            Parent root = loader.load();
            logger.info("Successfully loaded historyHomeworkChecker.fxml");

            // 创建新的舞台
//            Stage stageNl = new Stage();
//            stageNl.setTitle(Idf.userLanguageBundle.getString("mainpage.initTemple.title"));
//            stageNl.initModality(Modality.APPLICATION_MODAL);
//            stageNl.setScene(new Scene(root));

            Label showDate = (Label) root.lookup("#showDate");
            showDate.setText(Idf.userLanguageBundle.getString("mainpage.initTemple.title"));
            logger.debug("Set title for showDate label");

            TextArea editMain = (TextArea) root.lookup("#editMain");
            editMain.setText(Idf.initTemple);
            editMain.setFont(new Font(18));
            logger.debug("Set initial template text and font");

            Label statusDisplay = (Label) root.lookup("#statusDisplay");
            statusDisplay.setText(Idf.userLanguageBundle.getString("mainpage.initTemple.description"));
            logger.debug("Set description for statusDisplay label");

            // 添加监听器，当文本内容变化时立即执行保存操作
            logger.debug("Adding text change listener for template editing");
            editMain.textProperty().addListener((observable, oldValue, newValue) -> {
                logger.debug("Template text changed from length {} to length {}", 
                    oldValue != null ? oldValue.length() : 0, 
                    newValue != null ? newValue.length() : 0);
                Idf.initTemple = newValue;
                homeworkDatabase.changeInitTemple(Idf.initTemple);
                logger.info("Init template updated and saved");
            });

//            stageNl.setOnCloseRequest(windowEvent -> {
//                Idf.initTemple = editMain.getText();
//                homeworkDatabase.changeInitTemple(Idf.initTemple);
//            });
//
//            stageNl.showAndWait();

            // 应用淡入淡出动画
            applyFadeAnimation(root);
            logger.info("Applied fade animation for initial data editor");

        } catch (Exception e) {
            logger.error("Failed to open init template editor", e);
        }
        
        logger.info("Exiting onSetInitialDataClicked");
    }

    @FXML
    private void onDataBaseEditorClicked() {
        logger.info("Entering onDataBaseEditorClicked, current openPageCode: {}", openPageCode);
        
        if (openPageCode != null && openPageCode.equals("dataBaseEditor")) {
            logger.debug("Database editor page already open, returning");
            return;
        }
        openPageCode = "dataBaseEditor";
        logger.debug("Setting openPageCode to: {}", openPageCode);
        
        // 高亮对应的按钮
        highlightButton(dataBaseEditorButton);

        try {
            logger.debug("Loading FXML file for database editor");
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/xfty/homeworkchecker/fxml/settings/dataBaseEditor.fxml"));
            if (Idf.userLanguage != null && Idf.userLanguage.getString("language") != null) {
                loader.setResources(ResourceBundle.getBundle("com/xfty/homeworkchecker/i18n/language", locale));
                logger.debug("Applied language bundle for locale: {}", locale);
            }
            Parent root = loader.load();
            logger.info("Successfully loaded dataBaseEditor.fxml");
            
            // 应用淡入淡出动画
            applyFadeAnimation(root);
            logger.info("Applied fade animation for database editor");
        } catch (Exception e) {
            logger.error("Failed to open database editor", e);
        }
        
        logger.info("Exiting onDataBaseEditorClicked");
    }

    @FXML
    private void onResetClicked() {
        logger.info("Entering onResetClicked, current openPageCode: {}", openPageCode);
        
        if (openPageCode != null && openPageCode.equals("reset")) {
            logger.debug("Reset page already open, returning");
            return;
        }
        openPageCode = "reset";
        logger.debug("Setting openPageCode to: {}", openPageCode);
        
        // 高亮对应的按钮
        highlightButton(resetButton);

        try {
            logger.debug("Loading FXML file for reset settings");
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/xfty/homeworkchecker/fxml/settings/reset.fxml"));
            if (Idf.userLanguage != null && Idf.userLanguage.getString("language") != null) {
                loader.setResources(ResourceBundle.getBundle("com/xfty/homeworkchecker/i18n/language", locale));
                logger.debug("Applied language bundle for locale: {}", locale);
            }
            Parent root = loader.load();
            logger.info("Successfully loaded reset.fxml");
            
            // 应用淡入淡出动画
            applyFadeAnimation(root);
            logger.info("Applied fade animation for reset page");
        } catch (Exception e) {
            logger.error("Failed to open reset settings", e);
        }
        
        logger.info("Exiting onResetClicked");
    }

    @FXML
    private void onUpdaterClicked() {
        logger.info("Entering onUpdaterClicked, current openPageCode: {}", openPageCode);
        
        if (openPageCode != null && openPageCode.equals("updater")) {
            logger.debug("Updater page already open, returning");
            return;
        }
        openPageCode = "updater";
        logger.debug("Setting openPageCode to: {}", openPageCode);
        
        // 高亮对应的按钮
        highlightButton(updaterButton);

        try {
            logger.debug("Loading FXML file for updater");
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/xfty/homeworkchecker/fxml/settings/updater.fxml"));
            if (Idf.userLanguage != null && Idf.userLanguage.getString("language") != null) {
                loader.setResources(ResourceBundle.getBundle("com/xfty/homeworkchecker/i18n/language", locale));
                logger.debug("Applied language bundle for locale: {}", locale);
            }
            Parent root = loader.load();
            logger.info("Successfully loaded updater.fxml");
            
            // 应用淡入淡出动画
            applyFadeAnimation(root);
            logger.info("Applied fade animation for updater page");
        } catch (Exception e) {
            logger.error("Failed to open updater", e);
        }
        
        logger.info("Exiting onUpdaterClicked");
    }

    /**
     * 为 ScrollPane 应用淡入淡出动画效果
     * @param newContent 新内容节点
     */
    private void applyFadeAnimation(Parent newContent) {
        logger.debug("Starting fade animation for new content");
            
        // 获取当前内容
        Node currentContent = rightShowingArea.getContent();
            
        // 使用非线性插值器
        Interpolator easeInOutInterpolator = Interpolator.SPLINE(0.42, 0, 0.58, 1.0);
            
        if (currentContent != null) {
            logger.debug("Fading out current content");
            // 淡出当前内容
            Timeline fadeOutTimeline = new Timeline();
            KeyValue fadeOutOpacity = new KeyValue(currentContent.opacityProperty(), 0, easeInOutInterpolator);
            KeyFrame fadeOutFrame = new KeyFrame(Duration.millis(150), fadeOutOpacity);
            fadeOutTimeline.getKeyFrames().add(fadeOutFrame);
                
            fadeOutTimeline.setOnFinished(event -> {
                logger.debug("Fade out completed, setting new content");
                // 设置新内容
                rightShowingArea.setContent(newContent);
                    
                // 初始设置新内容为透明
                newContent.setOpacity(0);
                    
                // 淡入新内容
                Timeline fadeInTimeline = new Timeline();
                KeyValue fadeInOpacity = new KeyValue(newContent.opacityProperty(), 1, easeInOutInterpolator);
                KeyFrame fadeInFrame = new KeyFrame(Duration.millis(200), fadeInOpacity);
                fadeInTimeline.getKeyFrames().add(fadeInFrame);
                    
                fadeInTimeline.play();
                logger.debug("Started fade in animation for new content");
            });
                
            fadeOutTimeline.play();
        } else {
            logger.debug("No current content, setting new content and starting fade in");
            // 如果没有当前内容，直接设置新内容并播放淡入动画
            rightShowingArea.setContent(newContent);
            newContent.setOpacity(0);
                
            Timeline fadeInTimeline = new Timeline();
            KeyValue fadeInOpacity = new KeyValue(newContent.opacityProperty(), 1, easeInOutInterpolator);
            KeyFrame fadeInFrame = new KeyFrame(Duration.millis(200), fadeInOpacity);
            fadeInTimeline.getKeyFrames().add(fadeInFrame);
                
            fadeInTimeline.play();
            logger.debug("Started fade in animation for new content");
        }
            
        logger.debug("Fade animation process initiated");
    }
        
    /**
     * 高亮选中的按钮，同时恢复其他按钮的默认状态
     * @param selectedButton 选中的按钮
     */
    private void highlightButton(AnchorPane selectedButton) {
        logger.debug("Highlighting button: {}", selectedButton);
            
        // 恢复之前选中的按钮
        if (currentSelectedButton != null) {
            animateBackgroundColor(currentSelectedButton, DEFAULT_BACKGROUND, new Timeline[]{null});
        }
            
        // 高亮当前选中的按钮
        if (selectedButton != null) {
            animateBackgroundColor(selectedButton, HIGHLIGHT_BACKGROUND, new Timeline[]{null});
            currentSelectedButton = selectedButton;
        }
            
        logger.debug("Button highlighted successfully");
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // 保存关闭按钮引用到Idf
        Idf.settingsWindowCloseButton = windowCloseButton;
        logger.info("Settings window close button reference saved");
        
        if (Idf.userLanguage != null && Idf.userLanguage.getString("language") != null) {
            String languageCode = Idf.userLanguage.getString("language");
            String[] languageParts = languageCode.split("_");
            if (languageParts.length == 2) {
                locale = new Locale.Builder().setLanguage(languageParts[0]).setRegion(languageParts[1]).build();
            } else {
                locale = new Locale.Builder().setLanguage(languageParts[0]).build();
            }
        }
        
        // 为所有按钮添加悬停效果
        setupButtonHoverEffect(fontSettingsButton);
        setupButtonHoverEffect(languageSettingsButton);
        setupButtonHoverEffect(initialDataButton);
        setupButtonHoverEffect(dataBaseEditorButton);
        setupButtonHoverEffect(resetButton);
        setupButtonHoverEffect(updaterButton);

        // 设置缩放按钮点击事件
        windowZoomButton.setOnMouseClicked(event -> toggleWindowZoom());
    }

    private void toggleWindowZoom() {
        AnchorPane centerShowingArea = (AnchorPane) windowZoomButton.getScene().lookup("#centerShowingArea");
        HBox centerHighControl = (HBox) windowZoomButton.getScene().lookup("#centerHighControl");
        AnchorPane settingsRoot = (AnchorPane) windowCloseButton.getParent().getParent().getParent();

        if (settingsRoot == null) {
            logger.error("Could not find settings root anchor pane");
            return;
        }

        Interpolator spring = Interpolator.SPLINE(0.34, 0.9, 0.64, 1.0);
        Interpolator easeOut = Interpolator.SPLINE(0.25, 0.1, 0.25, 1.0);

        if (!isMaximized) {
            originalPrefWidth = settingsRoot.getPrefWidth();
            originalPrefHeight = settingsRoot.getPrefHeight();

            settingsRoot.setPrefSize(-1, -1);
            AnchorPane.setTopAnchor(settingsRoot, 0.0);
            AnchorPane.setBottomAnchor(settingsRoot, 0.0);
            AnchorPane.setLeftAnchor(settingsRoot, 0.0);
            AnchorPane.setRightAnchor(settingsRoot, 0.0);

            if (centerShowingArea != null) {
                centerShowingArea.setPrefSize(-1, -1);
                HBox.setHgrow(centerShowingArea, Priority.ALWAYS);
                centerShowingArea.setMaxWidth(Double.MAX_VALUE);
                centerShowingArea.setMaxHeight(Double.MAX_VALUE);
            }

            if (centerHighControl != null) {
                centerHighControl.setMaxHeight(Double.MAX_VALUE);
                VBox.setVgrow(centerHighControl, Priority.ALWAYS);
            }

            settingsRoot.setScaleX(0.93);
            settingsRoot.setScaleY(0.93);
            settingsRoot.setOpacity(0.6);

            new Timeline(
                new KeyFrame(Duration.millis(350),
                    new KeyValue(settingsRoot.scaleXProperty(), 1.0, spring),
                    new KeyValue(settingsRoot.scaleYProperty(), 1.0, spring),
                    new KeyValue(settingsRoot.opacityProperty(), 1.0, easeOut)
                )
            ).play();

            logger.info("Settings window maximized");
            isMaximized = true;
        } else {
            Timeline shrink = new Timeline(
                new KeyFrame(Duration.millis(120),
                    new KeyValue(settingsRoot.scaleXProperty(), 0.93, easeOut),
                    new KeyValue(settingsRoot.scaleYProperty(), 0.93, easeOut),
                    new KeyValue(settingsRoot.opacityProperty(), 0.0, easeOut)
                )
            );
            shrink.setOnFinished(e -> {
                settingsRoot.setPrefSize(originalPrefWidth, originalPrefHeight);
                AnchorPane.setTopAnchor(settingsRoot, null);
                AnchorPane.setBottomAnchor(settingsRoot, null);
                AnchorPane.setLeftAnchor(settingsRoot, null);
                AnchorPane.setRightAnchor(settingsRoot, null);

                if (centerShowingArea != null) {
                    centerShowingArea.setPrefSize(-1, -1);
                    HBox.setHgrow(centerShowingArea, null);
                    centerShowingArea.setMaxWidth(-1);
                    centerShowingArea.setMaxHeight(-1);
                }

                if (centerHighControl != null) {
                    centerHighControl.setMaxHeight(-1);
                    VBox.setVgrow(centerHighControl, null);
                }

                settingsRoot.setScaleX(0.93);
                settingsRoot.setScaleY(0.93);
                settingsRoot.setOpacity(0.0);

                new Timeline(
                    new KeyFrame(Duration.millis(200),
                        new KeyValue(settingsRoot.scaleXProperty(), 1.0, easeOut),
                        new KeyValue(settingsRoot.scaleYProperty(), 1.0, easeOut),
                        new KeyValue(settingsRoot.opacityProperty(), 1.0, easeOut)
                    )
                ).play();
            });
            shrink.play();

            logger.info("Settings window restored");
            isMaximized = false;
        }
    }
    
    /**
     * 为按钮设置悬停和按下效果（带动画）
     * @param button 要设置效果的按钮
     */
    private void setupButtonHoverEffect(AnchorPane button) {
        // 存储当前正在运行的动画，避免冲突
        final Timeline[] currentAnimation = {null};
        
        button.setOnMouseEntered(event -> {
            // 如果不是当前选中的按钮，则应用悬停颜色
            if (button != currentSelectedButton) {
                animateBackgroundColor(button, HOVER_BACKGROUND, currentAnimation);
            }
        });
        
        button.setOnMouseExited(event -> {
            // 如果不是当前选中的按钮，则恢复默认颜色
            if (button != currentSelectedButton) {
                animateBackgroundColor(button, DEFAULT_BACKGROUND, currentAnimation);
            }
            // 如果是选中的按钮，不做任何操作，保持选中状态
        });
        
        button.setOnMousePressed(event -> {
            // 应用按下颜色（包括选中按钮）
            animateBackgroundColor(button, PRESSED_BACKGROUND, currentAnimation);
        });
        
        button.setOnMouseReleased(event -> {
            // 释放鼠标后恢复到之前的状态
            if (button == currentSelectedButton) {
                // 如果是选中按钮，恢复到选中色
                animateBackgroundColor(button, HIGHLIGHT_BACKGROUND, currentAnimation);
            } else {
                // 如果不是选中按钮，恢复到默认色
                animateBackgroundColor(button, DEFAULT_BACKGROUND, currentAnimation);
            }
        });
    }
    
    /**
     * 为背景颜色变化添加动画效果
     * @param button 目标按钮
     * @param targetColorHex 目标颜色的十六进制值
     * @param currentAnimation 当前动画引用数组
     */
    private void animateBackgroundColor(AnchorPane button, String targetColorHex, Timeline[] currentAnimation) {
        // 停止之前的动画
        if (currentAnimation[0] != null) {
            currentAnimation[0].stop();
        }
        
        // 解析目标颜色
        Color targetColor = Color.web(targetColorHex);
        
        // 获取当前背景颜色
        Color startColor;
        try {
            String currentStyle = button.getStyle();
            if (currentStyle != null && currentStyle.contains("-fx-background-color:")) {
                int start = currentStyle.indexOf("#");
                if (start != -1) {
                    int end = Math.min(start + 7, currentStyle.length());
                    String hexColor = currentStyle.substring(start, end);
                    startColor = Color.web(hexColor);
                } else {
                    startColor = Color.web(DEFAULT_BACKGROUND);
                }
            } else {
                startColor = Color.web(DEFAULT_BACKGROUND);
            }
        } catch (Exception e) {
            startColor = Color.web(DEFAULT_BACKGROUND);
        }
        
        // 创建颜色过渡动画
        final Color finalStartColor = startColor;
        Transition colorTransition = new Transition() {
            {
                setCycleDuration(Duration.millis(150));
                setInterpolator(Interpolator.EASE_BOTH);
            }
            
            @Override
            protected void interpolate(double frac) {
                // 计算插值颜色
                double red = finalStartColor.getRed() + (targetColor.getRed() - finalStartColor.getRed()) * frac;
                double green = finalStartColor.getGreen() + (targetColor.getGreen() - finalStartColor.getGreen()) * frac;
                double blue = finalStartColor.getBlue() + (targetColor.getBlue() - finalStartColor.getBlue()) * frac;
                
                Color currentColor = Color.color(red, green, blue);
                String hexColor = toHexColor(currentColor);
                
                // 更新按钮背景色
                button.setStyle("-fx-background-color: " + hexColor + "; -fx-background-radius: 10;");
            }
        };
        
        // 播放动画
        colorTransition.play();
        
        // 保存动画引用（用于可能的中断）
        currentAnimation[0] = new Timeline(new KeyFrame(Duration.millis(150)));
    }
    
    /**
     * 将 Color 对象转换为十六进制颜色字符串
     * @param color 颜色对象
     * @return 十六进制颜色字符串
     */
    private String toHexColor(Color color) {
        int r = (int) Math.round(color.getRed() * 255);
        int g = (int) Math.round(color.getGreen() * 255);
        int b = (int) Math.round(color.getBlue() * 255);
        return String.format("#%02x%02x%02x", r, g, b);
    }
}