package com.xfty.homeworkchecker.controller.settings;

import com.xfty.homeworkchecker.Entry;
import com.xfty.homeworkchecker.Idf;
import com.xfty.homeworkchecker.controller.MainPage;
import com.xfty.homeworkchecker.service.HomeworkDatabase;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Pane;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.Locale;
import java.util.ResourceBundle;
import javafx.util.Duration;

public class Index implements Initializable {
    @FXML
    private ScrollPane rightShowingArea;

    private Locale locale;

    private String openPageCode;

    // 添加日志记录器
    private static final Logger logger = LoggerFactory.getLogger(Index.class);

    private final HomeworkDatabase homeworkDatabase = new HomeworkDatabase();

    @FXML
    private void onFontSettingsClicked() {
        logger.info("Entering onFontSettingsClicked, current openPageCode: {}", openPageCode);
        
        if (openPageCode != null && openPageCode.equals("homeworkArea")) {
            logger.debug("Font settings page already open, returning");
            return;
        }
        openPageCode = "homeworkArea";
        logger.debug("Setting openPageCode to: {}", openPageCode);

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
        
        try {
            // 加载FXML文件
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

    /**
     * 为ScrollPane应用淡入淡出动画效果
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

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        if (Idf.userLanguage != null && Idf.userLanguage.getString("language") != null) {
            String languageCode = Idf.userLanguage.getString("language");
            String[] languageParts = languageCode.split("_");
            if (languageParts.length == 2) {
                locale = new Locale.Builder().setLanguage(languageParts[0]).setRegion(languageParts[1]).build();
            } else {
                locale = new Locale.Builder().setLanguage(languageParts[0]).build();
            }
        }
    }
}