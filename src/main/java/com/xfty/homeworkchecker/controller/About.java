package com.xfty.homeworkchecker.controller;

import com.xfty.homeworkchecker.Entry;
import com.xfty.homeworkchecker.Idf;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;

public class About implements Initializable {
    private final Logger logger = LoggerFactory.getLogger(About.class);

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

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        versionDisplayAboutPage.setText(Idf.userLanguageBundle.getString("controller.about.ver") + Idf.softwareVersion);
    }
}
