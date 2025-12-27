package com.xfty.homeworkchecker.controller;

import com.xfty.homeworkchecker.Idf;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TextArea;
import javafx.scene.text.Font;

import java.net.URL;
import java.util.Locale;
import java.util.ResourceBundle;

public class HistoryHomeworkChecker implements Initializable {
    @FXML
    private TextArea editMain;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Apply font settings
        if (Idf.userConfig != null) {
            editMain.setFont(new Font(
                Idf.userConfig.getJSONObject("font")
                    .getJSONObject("fontFamily")
                    .getString("defaultFontFamily"),
                Idf.userConfig.getJSONObject("font")
                    .getJSONObject("textSize")
                    .getInteger("editMain")));
        }
    }
}
