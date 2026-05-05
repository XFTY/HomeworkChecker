package com.xfty.homeworkchecker;

import com.xfty.homeworkchecker.controller.MainPage;
import com.xfty.homeworkchecker.controller.setupWizard.SetupWizardController;
import com.xfty.homeworkchecker.service.FileInitManager;
import com.xfty.homeworkchecker.service.SingletonInstanceManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
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
        initDate();

        FileInitManager fileInitManager = new FileInitManager();
        boolean firstRun = fileInitManager.isFirstRun();

        if (firstRun) {
            showSetupWizard(stage, fileInitManager);
        } else {
            fileInitManager.initializeUserDirectories();
            loadUserLanguageBundle();
            if (!initSingletonCheck()) {
                logger.warn("Another instance is already running. Exiting silently...");
                System.exit(0);
                return;
            }
            showMainPage(stage);
        }
    }

    private void showSetupWizard(Stage stage, FileInitManager fileInitManager) {
        try {
            FXMLLoader loader = new FXMLLoader(Entry.class.getResource("fxml/setupWizard.fxml"));
            Scene wizardScene = new Scene(loader.load(), 900, 650);
            SetupWizardController controller = loader.getController();

            controller.setOnWizardFinished((languageCode, fontFamily, fontSize, initTemplate) -> {
                fileInitManager.initializeFirstRun(languageCode, fontFamily, fontSize, initTemplate);
                loadUserLanguageBundle();
                if (!initSingletonCheck()) {
                    logger.warn("Another instance is already running. Exiting silently...");
                    System.exit(0);
                    return;
                }
                showMainPage(stage);
            });

            stage.setTitle("HomeworkChecker " + Idf.softwareVersion + " - 初始化");
            stage.setScene(wizardScene);
            stage.getIcons().add(new Image(Objects.requireNonNull(Entry.class.getResourceAsStream("icon/logo.png"))));
            stage.show();
        } catch (IOException e) {
            logger.error("Failed to load setup wizard", e);
        }
    }

    private void showMainPage(Stage stage) {
        try {
            String languageCode = Idf.userLanguage != null ? Idf.userLanguage.getString("language") : null;

            FXMLLoader fxmlLoader = new FXMLLoader(Entry.class.getResource("fxml/mainPage.fxml"));
            if (languageCode != null) {
                String[] languageParts = languageCode.split("_");
                Locale locale;
                if (languageParts.length == 2) {
                    locale = new Locale.Builder().setLanguage(languageParts[0]).setRegion(languageParts[1]).build();
                } else {
                    locale = new Locale.Builder().setLanguage(languageParts[0]).build();
                }
                fxmlLoader.setResources(ResourceBundle.getBundle("com/xfty/homeworkchecker/i18n/language", locale));
            }
            Scene scene = new Scene(fxmlLoader.load(), 1000, 600);

            stage.setTitle(Idf.userLanguageBundle != null ? Idf.userLanguageBundle.getString("entry.window.title") : "HomeworkChecker");
            stage.setScene(scene);
            stage.getIcons().add(new Image(Objects.requireNonNull(Entry.class.getResourceAsStream("icon/logo.png"))));
            stage.show();

            if (Idf.singletonManager != null) {
                Idf.singletonManager.startWatchService(stage);
            }

            mainPageController = fxmlLoader.getController();

            stage.setOnCloseRequest(windowEvent -> {
                logger.info("Application closing requested");
                Idf.isSoftwareClosing = true;
                if (mainPageController != null) {
                    mainPageController.cleanup();
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                logger.info("Application closed");
            });
        } catch (IOException e) {
            logger.error("Failed to load main page", e);
        }
    }

    private void loadUserLanguageBundle() {
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
    }

    private void initDate() {
        LocalDate today = LocalDate.now();
        Idf.year = String.valueOf(today.getYear());
        Idf.month = String.format("%02d", today.getMonthValue());
        Idf.day = String.format("%02d", today.getDayOfMonth());
        Idf.weekdays = String.valueOf(today.getDayOfWeek().getValue());
        logger.info("{} {} {} {}", Idf.year, Idf.month, Idf.day, Idf.weekdays);
    }

    private boolean initSingletonCheck() {
        SingletonInstanceManager singletonManager = new SingletonInstanceManager();
        boolean acquired = singletonManager.acquireLock();
        if (acquired) {
            Idf.singletonManager = singletonManager;
        } else {
            createRepeatedStartFile();
        }
        return acquired;
    }

    private void createRepeatedStartFile() {
        try {
            File userDir = FileUtils.getUserDirectory();
            File homeworkCheckerDir = new File(userDir, "homeworkChecker");
            if (!homeworkCheckerDir.exists()) {
                FileUtils.forceMkdir(homeworkCheckerDir);
            }
            File signalFile = new File(homeworkCheckerDir, "repeatedly.start");
            Files.writeString(Paths.get(signalFile.getAbsolutePath()),
                String.valueOf(System.currentTimeMillis()));
            logger.info("Created activation signal file: {}", signalFile.getAbsolutePath());
            Thread.sleep(200);
        } catch (IOException | InterruptedException e) {
            logger.error("Error creating activation signal file", e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public static void main(String[] args) {
        System.setProperty("file.encoding", "UTF-8");
        launch();
    }
}