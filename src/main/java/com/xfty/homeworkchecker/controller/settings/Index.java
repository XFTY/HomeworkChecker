package com.xfty.homeworkchecker.controller.settings;

import com.xfty.homeworkchecker.Entry;
import com.xfty.homeworkchecker.Idf;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
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
    private AnchorPane aboutSettingsButton;

    @FXML
    private Circle windowCloseButton;
    @FXML
    private Circle windowZoomButton;

    private Locale locale;
    private String openPageCode;
    private IndexNavigationHelper navHelper;

    private boolean isMaximized = false;
    private double originalPrefWidth = 818.0;
    private double originalPrefHeight = 509.0;

    private static final Logger logger = LoggerFactory.getLogger(Index.class);

    @FXML
    private void onFontSettingsClicked() {
        loadSection("homeworkArea", "fxml/settings/homeworkArea.fxml", fontSettingsButton);
    }

    @FXML
    private void onLanguageSettingsClicked() {
        loadSection("languageChooser", "fxml/languageChooser.fxml", languageSettingsButton);
    }

    @FXML
    private void onSetInitialDataClicked() {
        loadSection("setInitialData", "/com/xfty/homeworkchecker/fxml/settings/initTemplateEditor.fxml", initialDataButton);
    }

    @FXML
    private void onDataBaseEditorClicked() {
        loadSection("dataBaseEditor", "/com/xfty/homeworkchecker/fxml/settings/dataBaseEditor.fxml", dataBaseEditorButton);
    }

    @FXML
    private void onResetClicked() {
        loadSection("reset", "/com/xfty/homeworkchecker/fxml/settings/reset.fxml", resetButton);
    }

    @FXML
    private void onUpdaterClicked() {
        loadSection("updater", "/com/xfty/homeworkchecker/fxml/settings/updater.fxml", updaterButton);
    }

    @FXML
    private void onAboutSettingsClicked() {
        loadSection("about", "/com/xfty/homeworkchecker/fxml/settings/about.fxml", aboutSettingsButton);
    }

    private void loadSection(String pageCode, String fxmlPath, AnchorPane button) {
        if (openPageCode != null && openPageCode.equals(pageCode)) {
            return;
        }
        openPageCode = pageCode;
        navHelper.highlightButton(button);

        try {
            FXMLLoader loader = new FXMLLoader(
                fxmlPath.startsWith("/")
                    ? getClass().getResource(fxmlPath)
                    : Entry.class.getResource(fxmlPath)
            );
            if (Idf.userLanguage != null && Idf.userLanguage.getString("language") != null) {
                loader.setResources(ResourceBundle.getBundle("com/xfty/homeworkchecker/i18n/language", locale));
            }
            Parent root = loader.load();
            navHelper.applyFadeAnimation(root);
        } catch (IOException e) {
            logger.error("Failed to load section: {}", fxmlPath, e);
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        Idf.settingsWindowCloseButton = windowCloseButton;
        navHelper = new IndexNavigationHelper(rightShowingArea);

        if (Idf.userLanguage != null && Idf.userLanguage.getString("language") != null) {
            String languageCode = Idf.userLanguage.getString("language");
            String[] languageParts = languageCode.split("_");
            if (languageParts.length == 2) {
                locale = new Locale.Builder().setLanguage(languageParts[0]).setRegion(languageParts[1]).build();
            } else {
                locale = new Locale.Builder().setLanguage(languageParts[0]).build();
            }
        }

        for (AnchorPane btn : new AnchorPane[]{
            fontSettingsButton, languageSettingsButton, initialDataButton,
            dataBaseEditorButton, resetButton, updaterButton, aboutSettingsButton
        }) {
            navHelper.setupButtonHoverEffect(btn);
        }

        windowZoomButton.setOnMouseClicked(event -> toggleWindowZoom());
    }

    @FXML
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
}
