package com.xfty.homeworkchecker.controller;

import com.xfty.homeworkchecker.Idf;
import com.xfty.homeworkchecker.service.HomeworkDatabase;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;

public class Settings implements Initializable {
    private final Logger logger = LoggerFactory.getLogger(Settings.class);

    HomeworkDatabase homeworkDatabase = new HomeworkDatabase();

    @FXML
    private Slider editMainTextSize;
    @FXML
    private ChoiceBox fontFamily;
    @FXML
    private TextArea editMainTest;
    @FXML
    private Label scrollValueDisplay;


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        if (Objects.equals(Idf.userConfig.get("configVersion"), 1)) {
            editMainTextSize.adjustValue(Idf.userConfig.getJSONObject("font").getJSONObject("textSize").getDouble("editMain"));

            fontFamily.getItems().addAll(Idf.fontFamilies);
            try {
                fontFamily.getSelectionModel().select(Idf.userConfig.getJSONObject("font").getJSONObject("fontFamily").getString("defaultFontFamily"));
            } catch (Exception e) {
                fontFamily.getSelectionModel().select("Microsoft YaHei UI");
            }

            // 监听 editMainTextSize 的变化
            editMainTextSize.valueProperty().addListener((observable, oldValue, newValue) -> {
                Idf.userConfig.getJSONObject("font").getJSONObject("textSize").put("editMain", newValue.intValue());
                homeworkDatabase.updateConfig(Idf.userConfig);

                scrollValueDisplay.setText(Idf.userLanguageBundle.getString("settings.scroolValueDisplayTitle") + (int) editMainTextSize.getValue());

                updateEditMainTextChanges();
            });

            // 设置初始值
            scrollValueDisplay.setText(Idf.userLanguageBundle.getString("settings.scroolValueDisplayTitle") + (int) editMainTextSize.getValue());
            
            // 监听 fontFamily 的变化
            fontFamily.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue != null) {
                    Idf.userConfig.getJSONObject("font").getJSONObject("fontFamily").put("defaultFontFamily", newValue.toString());
                    logger.debug("FontFamily change to: {}", newValue.toString());

                    homeworkDatabase.updateConfig(Idf.userConfig);
                    updateEditMainTextChanges();
                }
            });

            editMainTest.setFont(new Font(Idf.userConfig.getJSONObject("font").getJSONObject("fontFamily").getString("defaultFontFamily"), editMainTextSize.getValue()));
        } else {
            logger.error("config softwareVersion not match");
        }
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

    private void updateEditMainTextChanges() {
        editMainTest.setFont(new Font(Idf.userConfig.getJSONObject("font").getJSONObject("fontFamily").getString("defaultFontFamily"), Idf.userConfig.getJSONObject("font").getJSONObject("textSize").getInteger("editMain")));
    }
}