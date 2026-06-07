package com.xfty.homeworkchecker.controller.settings;

import com.xfty.homeworkchecker.Idf;
import com.xfty.homeworkchecker.service.HomeworkDatabase;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.text.Font;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ResourceBundle;

public class InitTemplateEditor {

    private static final Logger logger = LoggerFactory.getLogger(InitTemplateEditor.class);

    @FXML
    private Label showDate;

    @FXML
    private Label statusDisplay;

    @FXML
    private TextArea editMain;

    private final HomeworkDatabase homeworkDatabase = new HomeworkDatabase();

    @FXML
    public void initialize() {
        if (Idf.userLanguageBundle != null) {
            showDate.setText(Idf.userLanguageBundle.getString("mainpage.initTemple.title"));
            statusDisplay.setText(Idf.userLanguageBundle.getString("mainpage.initTemple.description"));
        }

        editMain.setText(Idf.initTemple);
        editMain.setFont(new Font(18));

        editMain.textProperty().addListener((observable, oldValue, newValue) -> {
            Idf.initTemple = newValue;
            homeworkDatabase.changeInitTemple(Idf.initTemple);
            logger.info("Init template updated and saved");
        });

        logger.info("InitTemplateEditor initialized");
    }
}
