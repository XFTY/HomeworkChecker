package com.xfty.homeworkchecker.controller.setupWizard.wizard;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

public class LanguageStepHandler {

    private static final Logger logger = LoggerFactory.getLogger(LanguageStepHandler.class);

    private Runnable onLanguageChanged;
    private Timeline languageTitleTimeline;

    public void stopAnimation() {
        if (languageTitleTimeline != null) {
            languageTitleTimeline.stop();
            languageTitleTimeline = null;
        }
    }

    public void setOnLanguageChanged(Runnable callback) {
        this.onLanguageChanged = callback;
    }

    public void setupStep(Parent root, ResourceBundle wizardBundle) {
        GridPane grid = (GridPane) root.lookup("#languageGrid");
        if (grid == null) return;

        List<String[]> languages = Arrays.asList(
            new String[]{"chineseButton", "zh_CN"},
            new String[]{"traditionalChineseButton", "zh_HK"},
            new String[]{"englishButton", "en_US"},
            new String[]{"spanishButton", "es_ES"},
            new String[]{"frenchButton", "fr_FR"},
            new String[]{"arabicButton", "ar_SA"},
            new String[]{"russianButton", "ru_RU"},
            new String[]{"bengaliButton", "bn_BD"},
            new String[]{"germanButton", "de_DE"},
            new String[]{"japaneseButton", "ja_JP"},
            new String[]{"portugueseButton", "pt_PT"}
        );

        for (String[] lang : languages) {
            AnchorPane button = (AnchorPane) root.lookup("#" + lang[0]);
            if (button != null) {
                String code = lang[1];
                button.setOnMouseClicked(e -> selectLanguage(code, root));
                button.setOnMouseEntered(e -> {
                    String selectedCode = getSelectedLanguageCode();
                    if (!code.equals(selectedCode)) {
                        button.setStyle("-fx-background-color: #4a4a4a; -fx-background-radius: 10;");
                    }
                });
                button.setOnMouseExited(e -> {
                    String selectedCode = getSelectedLanguageCode();
                    if (!code.equals(selectedCode)) {
                        button.setStyle("-fx-background-color: #575757; -fx-background-radius: 10;");
                    }
                });
            }
        }

        Label titleLabel = (Label) root.lookup("#languageTitle");
        if (titleLabel != null) {
            String[] rotateLocales = {
                "zh_CN", "zh_HK", "en_US", "es_ES", "fr_FR",
                "ar_SA", "ru_RU", "bn_BD", "de_DE", "ja_JP", "pt_PT"
            };
            String firstParts[] = rotateLocales[0].split("_");
            Locale firstLoc = new Locale.Builder().setLanguage(firstParts[0]).setRegion(firstParts[1]).build();
            titleLabel.setText(ResourceBundle.getBundle("com/xfty/homeworkchecker/i18n/language", firstLoc).getString("setup.language.title"));
            int[] idx = {1};
            languageTitleTimeline = new Timeline(new KeyFrame(Duration.seconds(4), e -> {
                Timeline fadeOut = new Timeline(
                    new KeyFrame(Duration.millis(300), new KeyValue(titleLabel.opacityProperty(), 0))
                );
                fadeOut.setOnFinished(e2 -> {
                    String code = rotateLocales[idx[0]];
                    String[] parts = code.split("_");
                    Locale loc = new Locale.Builder().setLanguage(parts[0]).setRegion(parts[1]).build();
                    titleLabel.setText(ResourceBundle.getBundle("com/xfty/homeworkchecker/i18n/language", loc).getString("setup.language.title"));
                    idx[0] = (idx[0] + 1) % rotateLocales.length;
                    Timeline fadeIn = new Timeline(
                        new KeyFrame(Duration.millis(300), new KeyValue(titleLabel.opacityProperty(), 1))
                    );
                    fadeIn.play();
                });
                fadeOut.play();
            }));
            languageTitleTimeline.setCycleCount(Timeline.INDEFINITE);
            languageTitleTimeline.play();
        }
    }

    private String selectedLanguageCode;

    public String getSelectedLanguageCode() {
        return selectedLanguageCode;
    }

    public ResourceBundle selectLanguage(String languageCode, Parent root) {
        selectedLanguageCode = languageCode;
        String[] parts = languageCode.split("_");
        Locale locale;
        if (parts.length == 2) {
            locale = new Locale.Builder().setLanguage(parts[0]).setRegion(parts[1]).build();
        } else {
            locale = new Locale.Builder().setLanguage(parts[0]).build();
        }
        ResourceBundle bundle = ResourceBundle.getBundle("com/xfty/homeworkchecker/i18n/language", locale);

        GridPane grid = (GridPane) root.lookup("#languageGrid");
        if (grid != null) {
            for (Node node : grid.getChildren()) {
                if (node instanceof AnchorPane btn) {
                    btn.setStyle("-fx-background-color: #575757; -fx-background-radius: 10;");
                }
            }
        }
        AnchorPane selectedBtn = (AnchorPane) root.lookup("#" + getLanguageButtonId(languageCode));
        if (selectedBtn != null) {
            selectedBtn.setStyle("-fx-background-color: #3a3a3a; -fx-background-radius: 10;");
        }

        if (onLanguageChanged != null) {
            onLanguageChanged.run();
        }
        return bundle;
    }

    private String getLanguageButtonId(String languageCode) {
        return switch (languageCode) {
            case "zh_CN" -> "chineseButton";
            case "zh_HK" -> "traditionalChineseButton";
            case "en_US" -> "englishButton";
            case "es_ES" -> "spanishButton";
            case "fr_FR" -> "frenchButton";
            case "ar_SA" -> "arabicButton";
            case "ru_RU" -> "russianButton";
            case "bn_BD" -> "bengaliButton";
            case "de_DE" -> "germanButton";
            case "ja_JP" -> "japaneseButton";
            case "pt_PT" -> "portugueseButton";
            default -> "englishButton";
        };
    }
}
