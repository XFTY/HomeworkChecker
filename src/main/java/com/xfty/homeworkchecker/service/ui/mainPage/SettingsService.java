package com.xfty.homeworkchecker.service.ui.mainPage;

import com.xfty.homeworkchecker.Idf;
import com.xfty.homeworkchecker.service.HomeworkDatabase;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.ResourceBundle;

public class SettingsService {

    private static final Logger logger = LoggerFactory.getLogger(SettingsService.class);

    private final PopupService popupService;
    private final HomeworkDatabase homeworkDatabase;
    private final TextArea editMain;
    private final Runnable clearTodayHomeworkCallback;
    private Runnable onFontChanged;

    public SettingsService(PopupService popupService,
                          HomeworkDatabase homeworkDatabase,
                          TextArea editMain,
                          Runnable clearTodayHomeworkCallback) {
        this.popupService = popupService;
        this.homeworkDatabase = homeworkDatabase;
        this.editMain = editMain;
        this.clearTodayHomeworkCallback = clearTodayHomeworkCallback;
    }

    public void setOnFontChanged(Runnable onFontChanged) {
        this.onFontChanged = onFontChanged;
    }

    public void openSettingsDialog() {
        logger.info("Opening settings window");

        try {
            logger.debug("Loading settings FXML from: /com/xfty/homeworkchecker/fxml/settings/index.fxml");
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/xfty/homeworkchecker/fxml/settings/index.fxml")
            );

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

            Circle windowCloseButton = popupService.showPopup(root, "#windowCloseButton");
            logger.debug("Popup with close handler created");

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

            windowCloseButton.setOnMouseClicked(mouseEvent -> {
                logger.info("Settings window close button clicked");
                homeworkDatabase.updateConfig(Idf.userConfig);
                logger.info("User closed settings window");

                AnchorPane centerShowingArea = (AnchorPane) root.getScene().lookup("#centerShowingArea");
                HBox centerHighControl = (HBox) root.getScene().lookup("#centerHighControl");
                if (centerShowingArea != null) {
                    centerShowingArea.setPrefSize(364.0, 306.0);
                    centerShowingArea.setMaxWidth(-1);
                    centerShowingArea.setMaxHeight(-1);
                    HBox.setHgrow(centerShowingArea, null);
                }
                if (centerHighControl != null) {
                    centerHighControl.setMaxHeight(-1);
                    VBox.setVgrow(centerHighControl, null);
                }

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
                    if (onFontChanged != null) {
                        onFontChanged.run();
                    }
                }

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
