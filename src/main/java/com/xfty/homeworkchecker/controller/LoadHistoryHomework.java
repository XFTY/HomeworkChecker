package com.xfty.homeworkchecker.controller;

import com.xfty.homeworkchecker.Idf;
import com.xfty.homeworkchecker.service.ui.loadHistoryHomework.WeekdayCalculator;
import com.xfty.homeworkchecker.service.ui.loadHistoryHomework.HomeworkContentFetcher;
import com.xfty.homeworkchecker.service.ui.loadHistoryHomework.ButtonStateManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class LoadHistoryHomework implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(LoadHistoryHomework.class);

    // Service layer components
    private final WeekdayCalculator weekdayCalculator = new WeekdayCalculator();
    private final HomeworkContentFetcher contentFetcher = new HomeworkContentFetcher();
    private final ButtonStateManager buttonStateManager = new ButtonStateManager();

    @FXML
    private DatePicker datePicker;

    @FXML
    private Button lastweekBt;
    @FXML
    private Button monBt;
    @FXML
    private Button tusBt;
    @FXML
    private Button wedBt;
    @FXML
    private Button thuBt;
    @FXML
    private Button friBt;

    @FXML
    private ImageView lastweekIv;
    @FXML
    private ImageView monIv;
    @FXML
    private ImageView tusIv;
    @FXML
    private ImageView wedIv;
    @FXML
    private ImageView thuIv;
    @FXML
    private ImageView firIv;

    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("Initializing LoadHistoryHomework dialog");
        initializeWeekdayButtons();
    }

    @FXML
    private void handleConfirmButton() {
        LocalDate selectedDate = datePicker.getValue();
        if (selectedDate == null) {
            showAlert(Alert.AlertType.WARNING, 
                     Idf.userLanguageBundle.getString("loadhistory.alert.warning"), 
                     Idf.userLanguageBundle.getString("loadhistory.alert.needAvabileDate"));
            return;
        }
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        String formattedDate = selectedDate.format(formatter);
        
        String year = formattedDate.substring(0, 4);
        String month = formattedDate.substring(4, 6);
        String day = formattedDate.substring(6, 8);
        
        String homeworkContent = contentFetcher.getHomeworkContext(year, month, day);
        
        if (homeworkContent == null) {
            showAlert(Alert.AlertType.INFORMATION, 
                     Idf.userLanguageBundle.getString("loadhistory.info"), 
                     Idf.userLanguageBundle.getString("loadhistory.notFound"));
        } else {
            showHomeworkContent(homeworkContent, formattedDate);
        }
    }
    
    @FXML
    private void handleLastWeekButton() {
        String[] fileNames = weekdayCalculator.getLastWeekendFileNames();
        
        String finalFilename = "unKnown";
        String homeworkContent = null;
        
        for (String fileName : fileNames) {
            homeworkContent = contentFetcher.getHomeworkContextByFileName(fileName);
            if (homeworkContent != null) {
                finalFilename = fileName;
                break;
            }
        }
        
        if (homeworkContent == null) {
            showAlert(Alert.AlertType.INFORMATION, 
                     Idf.userLanguageBundle.getString("loadhistory.info"), 
                     Idf.userLanguageBundle.getString("loadhistory.notFound"));
        } else {
            showHomeworkContent(homeworkContent, finalFilename);
        }
    }
    
    @FXML
    private void handleMonButton() {
        String fileName = weekdayCalculator.getCurrentWeekMonday(LocalDate.now())
                                          .format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        handleWeekdayButtonClick(fileName);
    }
    
    @FXML
    private void handleTusButton() {
        String fileName = weekdayCalculator.getCurrentWeekMonday(LocalDate.now())
                                          .plusDays(1)
                                          .format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        handleWeekdayButtonClick(fileName);
    }
    
    @FXML
    private void handleWedButton() {
        String fileName = weekdayCalculator.getCurrentWeekMonday(LocalDate.now())
                                          .plusDays(2)
                                          .format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        handleWeekdayButtonClick(fileName);
    }
    
    @FXML
    private void handleThuButton() {
        String fileName = weekdayCalculator.getCurrentWeekMonday(LocalDate.now())
                                          .plusDays(3)
                                          .format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        handleWeekdayButtonClick(fileName);
    }
    
    @FXML
    private void handleFriButton() {
        String fileName = weekdayCalculator.getCurrentWeekMonday(LocalDate.now())
                                          .plusDays(4)
                                          .format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        handleWeekdayButtonClick(fileName);
    }
    
    private void handleWeekdayButtonClick(String fileName) {
        String homeworkContent = contentFetcher.getHomeworkContextByFileName(fileName);
        
        if (homeworkContent == null) {
            showAlert(Alert.AlertType.INFORMATION, 
                     Idf.userLanguageBundle.getString("loadhistory.info"), 
                     Idf.userLanguageBundle.getString("loadhistory.notFound"));
        } else {
            showHomeworkContent(homeworkContent, fileName);
        }
    }
    
    /**
     * Display homework content in a modal dialog
     */
    private void showHomeworkContent(String content, String date) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/xfty/homeworkchecker/fxml/historyHomeworkChecker.fxml"));
            if (Idf.userLanguage != null && Idf.userLanguage.getString("language") != null) {
                String languageCode = Idf.userLanguage.getString("language");
                String[] languageParts = languageCode.split("_");
                Locale locale = (languageParts.length == 2) ? 
                    new Locale.Builder().setLanguage(languageParts[0]).setRegion(languageParts[1]).build() : 
                    new Locale.Builder().setLanguage(languageParts[0]).build();
                loader.setResources(ResourceBundle.getBundle("com/xfty/homeworkchecker/i18n/language", locale));
            }
            Parent root = loader.load();
            
            Stage stageNl = new Stage();
            stageNl.setTitle(Idf.userLanguageBundle.getString("loadhistory.window.title"));
            stageNl.initModality(Modality.APPLICATION_MODAL);
            stageNl.setScene(new Scene(root));

            Label showDate = (Label) root.lookup("#showDate");
            showDate.setText(date + " | "+Idf.userLanguageBundle.getString("loadhistory.window.titleD"));

            if (Idf.isMainPageMaximized) {
                stageNl.setMaximized(true);
            }
            
            javafx.scene.control.TextArea editMain = (javafx.scene.control.TextArea) root.lookup("#editMain");
            if (editMain != null) {
                editMain.setText(content);
                editMain.setEditable(false);
            }

            stageNl.setOnCloseRequest(windowEvent -> stageNl.close());
            stageNl.showAndWait();

        } catch (IOException e) {
            logger.error("Failed to load history homework checker window", e);
            showAlert(Alert.AlertType.ERROR, 
                     Idf.userLanguageBundle.getString("loadhistory.error"), 
                     Idf.userLanguageBundle.getString("loadhistory.failedToLoad") + e.getMessage());
        }
    }
    
    private void showAlert(Alert.AlertType alertType, String title, String content) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /**
     * Initialize all weekday buttons state based on homework data availability
     */
    private void initializeWeekdayButtons() {
        String[] allWeekdays = weekdayCalculator.getAllWeekdaysFileNames();
        String[] homeworkContexts = contentFetcher.getHomeworkContextsByFileNames(allWeekdays);
        
        List<Button> buttons = Arrays.asList(lastweekBt, monBt, tusBt, wedBt, thuBt, friBt);
        List<ImageView> imageViews = Arrays.asList(lastweekIv, monIv, tusIv, wedIv, thuIv, firIv);
        
        boolean hasLastWeekendData = homeworkContexts[0] != null || homeworkContexts[1] != null;
        buttonStateManager.setButtonState(lastweekBt, lastweekIv, hasLastWeekendData);
        
        for (int i = 2; i < homeworkContexts.length; i++) {
            int buttonIndex = i - 2;
            boolean hasData = homeworkContexts[i] != null;
            buttonStateManager.setButtonState(buttons.get(buttonIndex), imageViews.get(buttonIndex), hasData);
        }
        
        logger.info("Initialized weekday buttons with {} files", allWeekdays.length);
    }
}
