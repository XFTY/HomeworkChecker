package com.xfty.homeworkchecker.service.ui.mainPage;

import com.xfty.homeworkchecker.Idf;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.ResourceBundle;

public class HistoryHomeworkService {

    private static final Logger logger = LoggerFactory.getLogger(HistoryHomeworkService.class);

    private final PopupService popupService;

    public HistoryHomeworkService(PopupService popupService) {
        this.popupService = popupService;
    }

    public void openHistoryHomeworkDialog() {
        logger.info("Opening history homework dialog");

        try {
            logger.debug("Loading history homework FXML from: /com/xfty/homeworkchecker/fxml/loadHistoryHomework.fxml");
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/xfty/homeworkchecker/fxml/loadHistoryHomework.fxml")
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
            logger.info("History homework FXML loaded successfully");

            popupService.showPopup(root, "#windowCloseButton");
            logger.debug("History homework dialog popup created");

        } catch (Exception e) {
            logger.error("Error opening history homework dialog", e);
        }

        logger.debug("History homework button press handler completed");
    }
}
