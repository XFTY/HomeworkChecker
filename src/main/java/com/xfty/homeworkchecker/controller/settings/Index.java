package com.xfty.homeworkchecker.controller.settings;

import com.xfty.homeworkchecker.Entry;
import com.xfty.homeworkchecker.Idf;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.ScrollPane;

import java.io.IOException;
import java.net.URL;
import java.util.Locale;
import java.util.ResourceBundle;

public class Index implements Initializable {
    @FXML
    private ScrollPane rightShowingArea;

    @FXML
    private void onFontSettingsClicked() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(Entry.class.getResource("fxml/settings/homeworkArea.fxml"));

            if (Idf.userLanguage != null && Idf.userLanguage.getString("language") != null) {
                String languageCode = Idf.userLanguage.getString("language");
                String[] languageParts = languageCode.split("_");
                Locale locale;
                if (languageParts.length == 2) {
                    locale = new Locale.Builder().setLanguage(languageParts[0]).setRegion(languageParts[1]).build();
                } else {
                    locale = new Locale.Builder().setLanguage(languageParts[0]).build();
                }

                // Idf.userLanguageBundle = ResourceBundle.getBundle("com/xfty/homeworkchecker/i18n/language", locale);
                fxmlLoader.setResources(ResourceBundle.getBundle("com/xfty/homeworkchecker/i18n/language", locale));
            }

            Parent root = fxmlLoader.load();

            rightShowingArea.setContent(root);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

    }
}
