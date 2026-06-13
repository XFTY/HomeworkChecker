package com.xfty.homeworkchecker.controller.setupWizard.wizard;

import javafx.scene.Parent;
import javafx.scene.control.Label;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ResourceBundle;

public class FinishStepHandler {

    private static final Logger logger = LoggerFactory.getLogger(FinishStepHandler.class);

    public void setupStep(Parent root, ResourceBundle stepBundle,
                          String selectedLanguageCode,
                          String selectedFontFamily, double selectedFontSize) {
        Label finishTitle = (Label) root.lookup("#finishTitle");
        Label finishDesc = (Label) root.lookup("#finishDesc");
        Label summaryLanguage = (Label) root.lookup("#summaryLanguage");
        Label summaryFont = (Label) root.lookup("#summaryFont");
        if (finishTitle != null) finishTitle.setText(stepBundle.getString("setup.finish.title"));
        if (finishDesc != null) finishDesc.setText(stepBundle.getString("setup.finish.description"));
        if (summaryLanguage != null) {
            summaryLanguage.setText(stepBundle.getString("setup.finish.language") + " " + getLanguageDisplayName(selectedLanguageCode));
        }
        if (summaryFont != null) {
            summaryFont.setText(stepBundle.getString("setup.finish.font") + " " + selectedFontFamily + ", " + String.format("%.0f", selectedFontSize));
        }
    }

    private String getLanguageDisplayName(String code) {
        return switch (code) {
            case "zh_CN" -> "简体中文";
            case "zh_HK" -> "繁體中文";
            case "en_US" -> "English";
            case "es_ES" -> "Español";
            case "fr_FR" -> "Français";
            case "ar_SA" -> "العربية";
            case "ru_RU" -> "Русский";
            case "bn_BD" -> "বাংলা";
            case "de_DE" -> "Deutsch";
            case "ja_JP" -> "日本語";
            case "pt_PT" -> "Português";
            default -> code;
        };
    }
}
