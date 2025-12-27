package com.xfty.homeworkchecker.controller;

import com.xfty.homeworkchecker.Entry;
import com.xfty.homeworkchecker.Idf;
import com.xfty.homeworkchecker.service.HomeworkDatabase;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
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
    // 添加日志记录器
    private static final Logger logger = LoggerFactory.getLogger(LoadHistoryHomework.class);

    private final HomeworkDatabase homeworkDatabase = new HomeworkDatabase();

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
        // 初始化逻辑可以在这里添加

        getAllWeekdaysFileName();
    }

    @FXML
    private void handleConfirmButton() {
        logger.info("User confirmed date selection: {}", datePicker.getValue());
        
        LocalDate selectedDate = datePicker.getValue();
        if (selectedDate == null) {
            showAlert(Alert.AlertType.WARNING, Idf.userLanguageBundle.getString("loadhistory.alert.warning"), Idf.userLanguageBundle.getString("loadhistory.alert.needAvabileDate"));
            return;
        }
        
        // 格式化日期为YYYYMMDD形式
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        String formattedDate = selectedDate.format(formatter);
        
        // 提取年月日
        String year = formattedDate.substring(0, 4);
        String month = formattedDate.substring(4, 6);
        String day = formattedDate.substring(6, 8);
        
        // 调用HomeworkDatabase获取作业内容
        HomeworkDatabase homeworkDatabase = new HomeworkDatabase();
        String homeworkContent = homeworkDatabase.getHomeworkContext(year, month, day);
        
        if (homeworkContent == null) {
            showAlert(Alert.AlertType.INFORMATION, Idf.userLanguageBundle.getString("loadhistory.info"), Idf.userLanguageBundle.getString("loadhistory.notFound"));
        } else {
            // 创建模态窗口显示作业内容
            showHomeworkContent(homeworkContent, formattedDate);
        }
        
//        // 关闭当前窗口
//        Stage stage = (Stage) datePicker.getScene().getWindow();
//        stage.close();
    }
    
    /**
     * 处理"上周末"按钮点击事件
     */
    @FXML
    private void handleLastWeekButton() {
        logger.info("User clicked last week button");
        
        // 获取当前日期
        LocalDate today = LocalDate.now();
        
        // 计算本周第一天(周一)的日期
        LocalDate mondayEd = today.minusDays(today.getDayOfWeek().getValue() - 1);
        
        // 按优先级顺序检查上周日、上周六、上周五的文件
        String[] fileNames = {
            mondayEd.plusDays(-1).format(DateTimeFormatter.ofPattern("yyyyMMdd")), // 上周日
            mondayEd.plusDays(-2).format(DateTimeFormatter.ofPattern("yyyyMMdd")), // 上周六
            mondayEd.plusDays(-3).format(DateTimeFormatter.ofPattern("yyyyMMdd"))  // 上周五
        };

        String finalFilename = "unKnown";

        String homeworkContent = null;
        for (String fileName : fileNames) {
            homeworkContent = homeworkDatabase.getHomeworkContextByFileName(fileName);
            if (homeworkContent != null) {
                finalFilename = fileName;
                break; // 找到第一个存在的文件就停止查找
            }
        }
        
        if (homeworkContent == null) {
            showAlert(Alert.AlertType.INFORMATION, "提示", "没有找到该日期的作业内容");
        } else {
            // 创建模态窗口显示作业内容
            showHomeworkContent(homeworkContent, finalFilename);
        }
    }
    
    /**
     * 处理"周一"按钮点击事件
     */
    @FXML
    private void handleMonButton() {
        logger.info("User clicked Monday button");
        
        // 获取当前日期
        LocalDate today = LocalDate.now();
        
        // 计算本周第一天(周一)的日期
        LocalDate mondayEd = today.minusDays(today.getDayOfWeek().getValue() - 1);
        
        // 格式化为文件名
        String fileName = mondayEd.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        
        // 调用HomeworkDatabase获取作业内容
        String homeworkContent = homeworkDatabase.getHomeworkContextByFileName(fileName);
        
        if (homeworkContent == null) {
            showAlert(Alert.AlertType.INFORMATION, "提示", "没有找到该日期的作业内容");
        } else {
            // 创建模态窗口显示作业内容
            showHomeworkContent(homeworkContent, fileName);
        }
    }
    
    /**
     * 处理"周二"按钮点击事件
     */
    @FXML
    private void handleTusButton() {
        logger.info("User clicked Tuesday button");
        
        // 获取当前日期
        LocalDate today = LocalDate.now();
        
        // 计算本周第一天(周一)的日期
        LocalDate mondayEd = today.minusDays(today.getDayOfWeek().getValue() - 1);
        
        // 格式化为文件名
        String fileName = mondayEd.plusDays(1).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        
        // 调用HomeworkDatabase获取作业内容
        String homeworkContent = homeworkDatabase.getHomeworkContextByFileName(fileName);
        
        if (homeworkContent == null) {
            showAlert(Alert.AlertType.INFORMATION, "提示", "没有找到该日期的作业内容");
        } else {
            // 创建模态窗口显示作业内容
            showHomeworkContent(homeworkContent, fileName);
        }
    }
    
    /**
     * 处理"周三"按钮点击事件
     */
    @FXML
    private void handleWedButton() {
        logger.info("User clicked Wednesday button");
        
        // 获取当前日期
        LocalDate today = LocalDate.now();
        
        // 计算本周第一天(周一)的日期
        LocalDate mondayEd = today.minusDays(today.getDayOfWeek().getValue() - 1);
        
        // 格式化为文件名
        String fileName = mondayEd.plusDays(2).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        
        // 调用HomeworkDatabase获取作业内容
        String homeworkContent = homeworkDatabase.getHomeworkContextByFileName(fileName);
        
        if (homeworkContent == null) {
            showAlert(Alert.AlertType.INFORMATION, "提示", "没有找到该日期的作业内容");
        } else {
            // 创建模态窗口显示作业内容
            showHomeworkContent(homeworkContent, fileName);
        }
    }
    
    /**
     * 处理"周四"按钮点击事件
     */
    @FXML
    private void handleThuButton() {
        logger.info("User clicked Thursday button");
        
        // 获取当前日期
        LocalDate today = LocalDate.now();
        
        // 计算本周第一天(周一)的日期
        LocalDate mondayEd = today.minusDays(today.getDayOfWeek().getValue() - 1);
        
        // 格式化为文件名
        String fileName = mondayEd.plusDays(3).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        
        // 调用HomeworkDatabase获取作业内容
        String homeworkContent = homeworkDatabase.getHomeworkContextByFileName(fileName);
        
        if (homeworkContent == null) {
            showAlert(Alert.AlertType.INFORMATION, "提示", "没有找到该日期的作业内容");
        } else {
            // 创建模态窗口显示作业内容
            showHomeworkContent(homeworkContent, fileName);
        }
    }
    
    /**
     * 处理"周五"按钮点击事件
     */
    @FXML
    private void handleFriButton() {
        logger.info("User clicked Friday button");
        
        // 获取当前日期
        LocalDate today = LocalDate.now();
        
        // 计算本周第一天(周一)的日期
        LocalDate mondayEd = today.minusDays(today.getDayOfWeek().getValue() - 1);
        
        // 格式化为文件名
        String fileName = mondayEd.plusDays(4).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        
        // 调用HomeworkDatabase获取作业内容
        String homeworkContent = homeworkDatabase.getHomeworkContextByFileName(fileName);
        
        if (homeworkContent == null) {
            showAlert(Alert.AlertType.INFORMATION, "提示", "没有找到该日期的作业内容");
        } else {
            // 创建模态窗口显示作业内容
            showHomeworkContent(homeworkContent, fileName);
        }
    }
    
    /**
     * 显示作业内容的模态窗口
     * @param content 作业内容
     */
    private void showHomeworkContent(String content, String date) {
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
            stageNl.setTitle(Idf.userLanguageBundle.getString("loadhistory.window.title"));
            stageNl.initModality(Modality.APPLICATION_MODAL);
            stageNl.setScene(new Scene(root));

            Label showDate = (Label) root.lookup("#showDate");
            showDate.setText(date + " | "+Idf.userLanguageBundle.getString("loadhistory.window.titleD"));

            if (Idf.isMainPageMaximized) {
                stageNl.setMaximized(true);
            }
            
            // 获取TextArea并设置内容
            javafx.scene.control.TextArea editMain = (javafx.scene.control.TextArea) root.lookup("#editMain");
            if (editMain != null) {
                editMain.setText(content);
                editMain.setEditable(false); // 设置为只读模式
            }

            stageNl.setOnCloseRequest(windowEvent -> {
                stageNl.close();
            });

            stageNl.showAndWait();

        } catch (IOException e) {
            logger.error("Failed to load history homework checker window", e);
            showAlert(Alert.AlertType.ERROR, Idf.userLanguageBundle.getString("loadhistory.error"), Idf.userLanguageBundle.getString("loadhistory.failedToLoad") + e.getMessage());
        }
    }
    
    /**
     * 显示警告信息
     * @param alertType 警告类型
     * @param title 标题
     * @param content 内容
     */
    private void showAlert(Alert.AlertType alertType, String title, String content) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void getAllWeekdaysFileName() {
        // 获取当前日期
        LocalDate today = LocalDate.now();
        
        // 计算本周第一天(周一)的日期
        LocalDate mondayEd = today.minusDays(today.getDayOfWeek().getValue() - 1);

        // 创建一个包含所有工作日日期的有序列表
        List<String> weekdays = Arrays.asList(
            mondayEd.plusDays(-3).format(DateTimeFormatter.ofPattern("yyyyMMdd")), // 上周五
                mondayEd.plusDays(-2).format(DateTimeFormatter.ofPattern("yyyyMMdd")), // 上周六
            mondayEd.plusDays(-1).format(DateTimeFormatter.ofPattern("yyyyMMdd")), // 上周日
            mondayEd.format(DateTimeFormatter.ofPattern("yyyyMMdd")),              // 周一
            mondayEd.plusDays(1).format(DateTimeFormatter.ofPattern("yyyyMMdd")),  // 周二
            mondayEd.plusDays(2).format(DateTimeFormatter.ofPattern("yyyyMMdd")),  // 周三
            mondayEd.plusDays(3).format(DateTimeFormatter.ofPattern("yyyyMMdd")),  // 周四
            mondayEd.plusDays(4).format(DateTimeFormatter.ofPattern("yyyyMMdd"))   // 周五
        );

        List<Button> controlButtonButton =  Arrays.asList(
            lastweekBt, monBt, tusBt, wedBt, thuBt, friBt
        );

        // 修复ImageView列表顺序，确保与按钮列表匹配
        List<ImageView> controlButtonImageView =  Arrays.asList(
            lastweekIv, monIv, tusIv, wedIv, thuIv, firIv  // 注意：FXML中定义的是firIv而不是friIv
        );

        List<String> weekdaysHomeworkContext = new ArrayList<>();

        for (String weekday : weekdays) {
            weekdaysHomeworkContext.add(homeworkDatabase.getHomeworkContextByFileName(weekday));
        }

        // 处理上周末按钮：检查上周五、上周日中是否有任意一天有作业数据
        // 注意：根据原始代码，周末只包含上周五和上周日，不包含上周六
        boolean hasLastWeekendData = false;
        // 检查上周五(索引0)和上周日(索引1)是否有数据
        if (weekdaysHomeworkContext.get(0) != null || weekdaysHomeworkContext.get(1) != null) {
            hasLastWeekendData = true;
        }
        
        // 设置上周末按钮状态
        if (hasLastWeekendData) {
            lastweekIv.setImage(new Image(Objects.requireNonNull(Entry.class.getResourceAsStream("icon/light/green.png"))));
            lastweekBt.setDisable(false);
        } else {
            lastweekIv.setImage(new Image(Objects.requireNonNull(Entry.class.getResourceAsStream("icon/light/red.png"))));
            lastweekBt.setDisable(true);
        }

        // 处理周一到周五按钮 (索引2-6对应按钮索引0-4)
        for (int i = 2; i < weekdaysHomeworkContext.size(); i++) {
            int buttonIndex = i - 2; // 对应按钮列表的索引
            if (weekdaysHomeworkContext.get(i) != null) {
                // 有作业数据，设置绿灯并启用按钮
                controlButtonImageView.get(buttonIndex).setImage(new Image(Objects.requireNonNull(Entry.class.getResourceAsStream("icon/light/green.png"))));
                controlButtonButton.get(buttonIndex).setDisable(false);
            } else {
                // 无作业数据，设置红灯并禁用按钮
                controlButtonImageView.get(buttonIndex).setImage(new Image(Objects.requireNonNull(Entry.class.getResourceAsStream("icon/light/red.png"))));
                controlButtonButton.get(buttonIndex).setDisable(true);
            }
        }

        // 打印列表内容作为日志
        logger.info("Weekdays list: {}", weekdays);
    }
}