package com.xfty.homeworkchecker;

import com.xfty.homeworkchecker.controller.MainPage;
import com.xfty.homeworkchecker.service.FileInitManager;
import com.xfty.homeworkchecker.service.SingletonInstanceManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.concurrent.ScheduledExecutorService;


public class Entry extends Application {
    // 添加日志记录器
    private static final Logger logger = LoggerFactory.getLogger(Entry.class);
    private ScheduledExecutorService scheduler;
    private MainPage mainPageController;
    
    @Override
    public void start(Stage stage) throws IOException {
        initProgress();

        if (Idf.userLanguage != null && Idf.userLanguage.getString("language") != null) {
            String languageCode = Idf.userLanguage.getString("language");
            String[] languageParts = languageCode.split("_");
            Locale locale;
            if (languageParts.length == 2) {
                locale = new Locale.Builder().setLanguage(languageParts[0]).setRegion(languageParts[1]).build();
            } else {
                locale = new Locale.Builder().setLanguage(languageParts[0]).build();
            }

            Idf.userLanguageBundle = ResourceBundle.getBundle("com/xfty/homeworkchecker/i18n/language", locale);
        }

        System.out.println(Idf.userLanguageBundle);

        // 初始化进程前检查是否已经运行了实例
        if (!initSingletonCheck()) {
            // 已经有一个实例在运行，退出当前实例
            logger.warn("Another instance is already running. Exiting...");

            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle(Idf.userLanguageBundle.getString("entry.title.notice"));
            alert.setHeaderText(Idf.userLanguageBundle.getString("entry.header.notice"));
            alert.setContentText(Idf.userLanguageBundle.getString("entry.content.alreadyRunning"));
            // 设置窗口置顶
            alert.getDialogPane().getScene().getWindow().requestFocus();
            alert.showAndWait();

            System.exit(1);
            return;
        }

        String classpath = System.getProperty("java.class.path");
        System.out.println("Current ClassPath: ");
        System.out.println(classpath);

        FXMLLoader fxmlLoader = new FXMLLoader(Entry.class.getResource("fxml/mainPage.fxml"));
        // Set resource bundle for internationalization based on user language preference
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
        Scene scene = new Scene(fxmlLoader.load(), 1000, 600);

        stage.setTitle(Idf.userLanguageBundle.getString("entry.window.title"));
        stage.setScene(scene);
        stage.getIcons().add(new Image(Objects.requireNonNull(Entry.class.getResourceAsStream("icon/logo.png"))));
        stage.show();

        // 获取主页面控制器引用
        mainPageController = fxmlLoader.getController();
        
        // 添加窗口关闭事件监听器
        stage.setOnCloseRequest(windowEvent -> {
            logger.info("Application closing requested");
            Idf.isSoftwareClosing = true;
            
            // 清理资源
            if (mainPageController != null) {
                mainPageController.cleanup();
            }
            
            // 给一些时间让清理工作完成
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            logger.info("Application closed");
        });
    }

    /**
     * 初始化单例检查
     * @return true表示可以继续启动，false表示已有一个实例在运行
     */
    private boolean initSingletonCheck() {
        SingletonInstanceManager singletonManager = new SingletonInstanceManager();
        return singletonManager.acquireLock();
    }

    private void initProgress() {
        // 获取时间
        LocalDate today = LocalDate.now();
        Idf.year = String.valueOf(today.getYear());
        Idf.month = String.format("%02d", today.getMonthValue());
        Idf.day = String.format("%02d", today.getDayOfMonth());
        Idf.weekdays = String.valueOf(today.getDayOfWeek().getValue());

        logger.info("{} {} {} {}", Idf.year, Idf.month, Idf.day, Idf.weekdays);

        // 记录初始化进度
        logger.info("Initializing application progress...");
        
        // 检查并创建必要的目录和配置文件
        FileInitManager fileInitManager = new FileInitManager();
        fileInitManager.initializeUserDirectories();
    }
    
    public static void main(String[] args) {
        System.setProperty("file.encoding", "UTF-8");
        launch();
    }
}