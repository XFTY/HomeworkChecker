package com.xfty.homeworkchecker.service.ui.mainPage;

import com.alibaba.fastjson.JSONObject;
import com.xfty.homeworkchecker.service.HomeworkDatabase;
import com.xfty.homeworkchecker.Idf;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.WritableImage;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Top button service for managing top bar button actions
 */
public class TopButtonService {
    
    private static final Logger logger = LoggerFactory.getLogger(TopButtonService.class);
    
    /**
     * Screenshot service for handling screenshot operations
     */
    public static class ScreenshotService {
        
        private final TextArea editMain;
        
        public ScreenshotService(TextArea editMain) {
            this.editMain = editMain;
        }
        
        /**
         * Take a screenshot of the edit main area and save it to clipboard
         */
        public void takeScreenshot() {
            logger.info("Taking screenshot");
            
            try {
                logger.debug("Creating snapshot of editMain");
                WritableImage editMainPage = editMain.snapshot(null, null);
                logger.debug("Snapshot created successfully, dimensions: {}x{}", 
                    editMainPage.getWidth(), editMainPage.getHeight());

                Clipboard clipboard = Clipboard.getSystemClipboard();
                logger.debug("Getting system clipboard");
                
                ClipboardContent content = new ClipboardContent();
                content.putImage(editMainPage);
                logger.debug("Image content added to clipboard");

                clipboard.setContent(content);
                logger.info("Screenshot saved to clipboard");

                showSuccessAlert();
                logger.debug("Screenshot success alert shown");

            } catch (Exception e) {
                logger.error("Failed to save screenshot", e);
                showFailureAlert();
                logger.debug("Screenshot failure alert shown");
            }
            
            logger.debug("Screenshot operation completed");
        }
        
        /**
         * Show success alert after screenshot
         */
        private void showSuccessAlert() {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(Idf.userLanguageBundle.getString("mainpage.snapshot.success.title"));
            alert.setHeaderText(Idf.userLanguageBundle.getString("mainpage.snapshot.success.title"));
            alert.setContentText(Idf.userLanguageBundle.getString("mainpage.snapshot.success.header"));
            alert.showAndWait();
        }
        
        /**
         * Show failure alert when screenshot fails
         */
        private void showFailureAlert() {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(Idf.userLanguageBundle.getString("mainpage.snapshot.failure.title"));
            alert.setHeaderText(Idf.userLanguageBundle.getString("mainpage.snapshot.failure.title"));
            alert.setContentText(Idf.userLanguageBundle.getString("mainpage.snapshot.failure.header"));
            alert.showAndWait();
        }
    }
    
    /**
     * History homework service for managing history homework dialog
     */
    public static class HistoryHomeworkService {
        
        private final PopupService popupService;
        
        public HistoryHomeworkService(PopupService popupService) {
            this.popupService = popupService;
        }
        
        /**
         * Open history homework dialog
         */
        public void openHistoryHomeworkDialog() {
            logger.info("Opening history homework dialog");
            
            try {
                logger.debug("Loading history homework FXML from: /com/xfty/homeworkchecker/fxml/loadHistoryHomework.fxml");
                FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/xfty/homeworkchecker/fxml/loadHistoryHomework.fxml")
                );
                
                // Set resource bundle for internationalization
                if (Idf.userLanguage != null && Idf.userLanguage.getString("language") != null) {
                    String languageCode = Idf.userLanguage.getString("language");
                    String[] languageParts = languageCode.split("_");
                    Locale locale = (languageParts.length == 2) ? 
                        new Locale.Builder().setLanguage(languageParts[0]).setRegion(languageParts[1]).build() : 
                        new Locale.Builder().setLanguage(languageParts[0]).build();
                    loader.setResources(ResourceBundle.getBundle(
                        "com/xfty/homeworkchecker/i18n/language", locale
                    ));
                    logger.debug("Applied language bundle for locale: {}", locale);
                }
                
                Parent root = loader.load();
                logger.info("History homework FXML loaded successfully");
                
                // Show popup with basic close handler
                popupService.showPopup(root, "#windowCloseButton");
                logger.debug("History homework dialog popup created");
                
            } catch (Exception e) {
                logger.error("Error opening history homework dialog", e);
            }
            
            logger.debug("History homework button press handler completed");
        }
    }
    
    /**
     * Settings service for managing settings dialog
     */
    public static class SettingsService {
        
        private final PopupService popupService;
        private final HomeworkDatabase homeworkDatabase;
        private final TextArea editMain;
        private final Runnable clearTodayHomeworkCallback;
        
        public SettingsService(PopupService popupService, 
                              HomeworkDatabase homeworkDatabase,
                              TextArea editMain,
                              Runnable clearTodayHomeworkCallback) {
            this.popupService = popupService;
            this.homeworkDatabase = homeworkDatabase;
            this.editMain = editMain;
            this.clearTodayHomeworkCallback = clearTodayHomeworkCallback;
        }
        
        /**
         * Open settings dialog with complex close handler
         */
        public void openSettingsDialog() {
            logger.info("Opening settings window");
            
            try {
                logger.debug("Loading settings FXML from: /com/xfty/homeworkchecker/fxml/settings/index.fxml");
                FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/xfty/homeworkchecker/fxml/settings/index.fxml")
                );
                
                // Set resource bundle for internationalization
                if (Idf.userLanguage != null && Idf.userLanguage.getString("language") != null) {
                    String languageCode = Idf.userLanguage.getString("language");
                    String[] languageParts = languageCode.split("_");
                    Locale locale = (languageParts.length == 2) ? 
                        new Locale.Builder().setLanguage(languageParts[0]).setRegion(languageParts[1]).build() : 
                        new Locale.Builder().setLanguage(languageParts[0]).build();
                    loader.setResources(ResourceBundle.getBundle(
                        "com/xfty/homeworkchecker/i18n/language", locale
                    ));
                    logger.debug("Applied language bundle for locale: {}", locale);
                }
                
                Parent root = loader.load();
                logger.info("Settings FXML loaded successfully");
                
                // Show popup and get close button
                Circle windowCloseButton = popupService.showPopup(root, "#windowCloseButton");
                logger.debug("Popup with close handler created");
                
                // Setup clear button handler
                Platform.runLater(() -> {
                    try {
                        logger.debug("Looking up clear button");
                        Button clearButton = (Button) root.lookup("#clearAllHomeworkContentButton");
                        if (clearButton != null) {
                            logger.debug("Clear button found");
                            clearButton.setOnAction(buttonEvent -> {
                                logger.info("Clear button clicked");
                                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                                alert.setTitle(Idf.userLanguageBundle.getString("mainpage.confirmReset.title"));
                                alert.setHeaderText(Idf.userLanguageBundle.getString("mainpage.confirmReset.header"));
                                alert.setContentText(Idf.userLanguageBundle.getString("mainpage.confirmReset.content"));
                                
                                alert.showAndWait().ifPresent(response -> {
                                    if (response == javafx.scene.control.ButtonType.OK) {
                                        logger.info("User confirmed clear operation");
                                        clearTodayHomeworkCallback.run();
                                        logger.info("Today's homework content has been cleared");
                                    } else {
                                        logger.debug("User cancelled clear operation");
                                    }
                                });
                            });
                        } else {
                            logger.debug("Clear button not found in settings window");
                        }
                    } catch (Exception e) {
                        logger.error("Error setting up clear button listener", e);
                    }
                });
                
                // Setup custom close handler
                windowCloseButton.setOnMouseClicked(mouseEvent -> {
                    logger.info("Settings window close button clicked");
                    homeworkDatabase.updateConfig(Idf.userConfig);
                    logger.info("User closed settings window");
                    
                    // Update font if config is not null
                    if (Idf.userConfig != null) {
                        logger.debug("Updating font after settings closed");
                        editMain.setFont(new Font(
                            Idf.userConfig.getJSONObject("font")
                                .getJSONObject("fontFamily")
                                .getString("defaultFontFamily"),
                            Idf.userConfig.getJSONObject("font")
                                .getJSONObject("textSize")
                                .getInteger("editMain")));
                        logger.debug("Font updated after settings closed");
                    }
                    
                    // Clear homework if needed
                    if (Idf.needHomeworkShowingAreaClear) {
                        logger.info("Need to clear homework showing area after settings");
                        clearTodayHomeworkCallback.run();
                        logger.info("Today's homework content has been cleared [v2]");
                        Idf.needHomeworkShowingAreaClear = false;
                    }
                    
                    popupService.closePopup();
                    logger.debug("Popup closed");
                    
                    Idf.isPreviewWindowShowing = false;
                    logger.debug("Set isPreviewWindowShowing to false");
                });
                
            } catch (Exception e) {
                logger.error("Error opening settings window", e);
            }
            
            logger.debug("Settings button click handler completed");
        }
    }
    
    /**
     * About service for managing about dialog
     */
    public static class AboutService {
        
        private final PopupService popupService;
        
        public AboutService(PopupService popupService) {
            this.popupService = popupService;
        }
        
        /**
         * Open about dialog
         */
        public void openAboutDialog() {
            logger.info("Opening about homework dialog");
            
            try {
                logger.debug("Loading about FXML from: /com/xfty/homeworkchecker/fxml/about.fxml");
                FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/xfty/homeworkchecker/fxml/about.fxml")
                );
                
                // Set resource bundle for internationalization
                if (Idf.userLanguage != null && Idf.userLanguage.getString("language") != null) {
                    String languageCode = Idf.userLanguage.getString("language");
                    String[] languageParts = languageCode.split("_");
                    Locale locale = (languageParts.length == 2) ? 
                        new Locale.Builder().setLanguage(languageParts[0]).setRegion(languageParts[1]).build() : 
                        new Locale.Builder().setLanguage(languageParts[0]).build();
                    loader.setResources(ResourceBundle.getBundle(
                        "com/xfty/homeworkchecker/i18n/language", locale
                    ));
                    logger.debug("Applied language bundle for locale: {}", locale);
                }
                
                Parent root = loader.load();
                logger.info("About FXML loaded successfully");
                
                // Show popup with basic close handler
                popupService.showPopup(root, "#windowCloseButton");
                logger.debug("About dialog popup created");
                
            } catch (Exception e) {
                logger.error("Error opening about homework dialog", e);
            }
            
            logger.debug("About button press handler completed");
        }
    }
}