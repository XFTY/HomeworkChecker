package com.xfty.homeworkchecker.controller;

import com.xfty.homeworkchecker.Idf;
import com.xfty.homeworkchecker.service.ui.loadHistoryHomework.ButtonStateManagerService;
import com.xfty.homeworkchecker.service.ui.loadHistoryHomework.HomeworkContentFetcherService;
import com.xfty.homeworkchecker.service.ui.loadHistoryHomework.WeekdayCalculatorService;
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
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * LoadHistoryHomework — 历史作业查询对话框控制器
 * <p>
 * 提供 DatePicker 日期选择和星期快捷按钮（周一~周五 + 上周末），
 * 按钮以绿色（有数据）/红色（无数据）指示灯反馈数据状态。
 * </p>
 */
public class LoadHistoryHomework implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(LoadHistoryHomework.class);

    /** 星期计算服务 */
    private final WeekdayCalculatorService weekdayCalculatorService = new WeekdayCalculatorService();
    /** 作业内容获取服务 */
    private final HomeworkContentFetcherService contentFetcher = new HomeworkContentFetcherService();
    /** 按钮状态管理服务（绿灯/红灯） */
    private final ButtonStateManagerService buttonStateManagerService = new ButtonStateManagerService();

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

    
    /**
     * 初始化历史查询对话框：初始化星期快捷按钮状态
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("Initializing LoadHistoryHomework dialog");
        initializeWeekdayButtons();
    }

    /**
     * 确认按钮：读取 DatePicker 所选日期并查询对应作业内容
     */
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
    
    /**
     * 上周末按钮：遍历上周五六日的作业文件，找到第一个存在的数据
     */
    @FXML
    private void handleLastWeekButton() {
        String[] fileNames = weekdayCalculatorService.getLastWeekendFileNames();
        
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
    
    /**
     * 周一按钮
     */
    @FXML
    private void handleMonButton() {
        String fileName = weekdayCalculatorService.getCurrentWeekMonday(LocalDate.now())
                                          .format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        handleWeekdayButtonClick(fileName);
    }
    
    /**
     * 周二按钮
     */
    @FXML
    private void handleTusButton() {
        String fileName = weekdayCalculatorService.getCurrentWeekMonday(LocalDate.now())
                                          .plusDays(1)
                                          .format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        handleWeekdayButtonClick(fileName);
    }
    
    /**
     * 周三按钮
     */
    @FXML
    private void handleWedButton() {
        String fileName = weekdayCalculatorService.getCurrentWeekMonday(LocalDate.now())
                                          .plusDays(2)
                                          .format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        handleWeekdayButtonClick(fileName);
    }
    
    /**
     * 周四按钮
     */
    @FXML
    private void handleThuButton() {
        String fileName = weekdayCalculatorService.getCurrentWeekMonday(LocalDate.now())
                                          .plusDays(3)
                                          .format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        handleWeekdayButtonClick(fileName);
    }
    
    /**
     * 周五按钮
     */
    @FXML
    private void handleFriButton() {
        String fileName = weekdayCalculatorService.getCurrentWeekMonday(LocalDate.now())
                                          .plusDays(4)
                                          .format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        handleWeekdayButtonClick(fileName);
    }
    
    /**
     * 通用工作日按钮处理：根据文件名查询作业并展示
     */
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
    
    /**
     * 通用警告/信息弹窗
     */
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
        String[] allWeekdays = weekdayCalculatorService.getAllWeekdaysFileNames();
        String[] homeworkContexts = contentFetcher.getHomeworkContextsByFileNames(allWeekdays);
        
        List<Button> buttons = Arrays.asList(lastweekBt, monBt, tusBt, wedBt, thuBt, friBt);
        List<ImageView> imageViews = Arrays.asList(lastweekIv, monIv, tusIv, wedIv, thuIv, firIv);
        
        boolean hasLastWeekendData = homeworkContexts[0] != null || homeworkContexts[1] != null;
        buttonStateManagerService.setButtonState(lastweekBt, lastweekIv, hasLastWeekendData);
        
        for (int i = 2; i < homeworkContexts.length; i++) {
            int buttonIndex = i - 2;
            boolean hasData = homeworkContexts[i] != null;
            buttonStateManagerService.setButtonState(buttons.get(buttonIndex), imageViews.get(buttonIndex), hasData);
        }
        
        logger.info("Initialized weekday buttons with {} files", allWeekdays.length);
    }
}
