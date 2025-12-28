package com.xfty.homeworkchecker.controller;

import com.xfty.homeworkchecker.Entry;
import com.xfty.homeworkchecker.Idf;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicBoolean;

public class About implements Initializable {
    private final Logger logger = LoggerFactory.getLogger(About.class);

    private int iconClickedCount = Idf.iconClickedCount;

    @FXML
    private Label versionDisplayAboutPage;

    @FXML
    private void onUpdateWhatButtonClicked() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/xfty/homeworkchecker/fxml/updateWhat.fxml"));
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

            Stage aboutStage = new Stage();
            aboutStage.initModality(Modality.APPLICATION_MODAL);
            aboutStage.setTitle(Idf.userLanguageBundle.getString("controller.about.title"));
            aboutStage.setScene(new Scene(root));
            aboutStage.getIcons().add(new Image(Objects.requireNonNull(Entry.class.getResourceAsStream("icon/logo.png"))));
            aboutStage.setResizable(false);

            aboutStage.showAndWait();
        } catch (Exception e) {
            logger.error("Error opening about homework dialog", e);
            e.printStackTrace(System.out);
        }
    }

    @FXML
    protected void onIconButtonClicked() {
        if (!Idf.isAboutIconTriggeredAgain) {
            if (!Idf.isAboutIconTriggered) {
                if (Objects.equals(iconClickedCount, 20)) {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("emm...");
                    alert.setHeaderText("你好闲啊");
                    alert.setContentText("能点那么多下......");
                    alert.showAndWait();

                    Idf.isAboutIconTriggered = true;
                }
            } else {
                if (Objects.equals(40, iconClickedCount)) {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("emm...");
                    alert.setHeaderText("你看你看，又那么闲");
                    alert.setContentText("写作业不好吗？");
                    alert.showAndWait();

                    Idf.isAboutIconTriggeredAgain = true;
                }
            }
        } else {
            if (Objects.equals(iconClickedCount, 60)) {
                if (eggSampleLogger("520131415926")) {
                    if (eggSampleLogger("7355608")) {
                        if (eggSampleLogger("start!")) {
                            try {
                                FXMLLoader fxmlLoader = new FXMLLoader(Entry.class.getResource("fxml/eggPlant.fxml"));

                                Parent root = fxmlLoader.load();

                                Stage aboutStage = new Stage();
                                aboutStage.initModality(Modality.APPLICATION_MODAL);
                                aboutStage.setScene(new Scene(root));
                                aboutStage.getIcons().add(new Image(Objects.requireNonNull(Entry.class.getResourceAsStream("icon/logo.png"))));
                                aboutStage.setResizable(false);

                                aboutStage.showAndWait();
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        } else {
                            eggFailure();
                        }
                    } else {
                        eggFailure();
                    }
                } else {
                    eggFailure();
                }
            }
        }

        iconClickedCount++;
    }

    private void eggFailure() {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("操作中断");
        alert.setHeaderText("操作中断");
        alert.setContentText("操作失败，因为您输入的指令无效... \n 请重试");
        alert.showAndWait();
    }

    private boolean eggSampleLogger(String ifResult) {
        AtomicBoolean isSuccess = new AtomicBoolean(false);
        javafx.scene.control.Dialog<String> dialog = new javafx.scene.control.Dialog<>();
        dialog.setTitle("Error Dialog");
        dialog.setHeaderText(null);

        javafx.scene.control.TextField textField = new javafx.scene.control.TextField();

        javafx.scene.control.DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.setContent(textField);
        dialogPane.getButtonTypes().addAll(javafx.scene.control.ButtonType.OK, javafx.scene.control.ButtonType.CANCEL);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == javafx.scene.control.ButtonType.OK) {
                return textField.getText();
            }
            return null;
        });

        // 显示对话框并获取结果
        java.util.Optional<String> result = dialog.showAndWait();

        // 处理结果
        result.ifPresent(inputText -> {
            // 在这里处理用户输入的内容
            if (Objects.equals(inputText, ifResult)) {
                isSuccess.set(true);
            }
        });

        if (isSuccess.get()) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        versionDisplayAboutPage.setText(Idf.userLanguageBundle.getString("controller.about.ver") + Idf.softwareVersion);
    }
}
