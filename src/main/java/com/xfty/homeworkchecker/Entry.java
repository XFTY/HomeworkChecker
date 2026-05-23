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

import com.xfty.homeworkchecker.service.HomeworkDatabase;
import javafx.animation.FadeTransition;
import javafx.scene.Parent;
import javafx.scene.control.TextArea;
import javafx.util.Duration;

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
        Idf.primaryStage = stage;

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
            Idf.mainPageController = mainPageController;

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
            Idf.reloadLanguageBundle(Idf.userLanguage.getString("language"));
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

    public static void rebuildScene() {
        Stage stage = Idf.primaryStage;
        if (stage == null) {
            logger.error("Primary stage is null, cannot rebuild scene");
            return;
        }

        Scene oldScene = stage.getScene();
        if (oldScene != null) {
            Parent oldRoot = oldScene.getRoot();
            FadeTransition fadeOut = new FadeTransition(Duration.millis(200), oldRoot);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setOnFinished(e -> buildNewScene(stage));
            fadeOut.play();
        } else {
            buildNewScene(stage);
        }
    }

    private static void buildNewScene(Stage stage) {
        try {
            // Save current homework content before rebuilding
            Scene oldScene = stage.getScene();
            if (oldScene != null) {
                TextArea editMain = (TextArea) oldScene.lookup("#editMain");
                if (editMain != null) {
                    HomeworkDatabase db = new HomeworkDatabase();
                    db.writeHomeworkContextByDay(editMain.getText());
                }
            }

            // Cleanup old controller
            if (Idf.mainPageController != null) {
                Idf.mainPageController.cleanup();
                Idf.mainPageController = null;
            }

            // Build new scene with current language bundle
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
            Parent newRoot = fxmlLoader.load();
            newRoot.setOpacity(0.0);
            Scene newScene = new Scene(newRoot, 1000, 600);

            stage.setTitle(Idf.userLanguageBundle != null ? Idf.userLanguageBundle.getString("entry.window.title") : "HomeworkChecker");
            stage.setScene(newScene);
            stage.getIcons().add(new Image(Objects.requireNonNull(Entry.class.getResourceAsStream("icon/logo.png"))));
            stage.show();

            // Fade in new scene
            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), newRoot);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.play();

            if (Idf.singletonManager != null) {
                Idf.singletonManager.startWatchService(stage);
            }

            MainPage newController = fxmlLoader.getController();
            Idf.mainPageController = newController;

            stage.setOnCloseRequest(windowEvent -> {
                logger.info("Application closing requested");
                Idf.isSoftwareClosing = true;
                if (newController != null) {
                    newController.cleanup();
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                logger.info("Application closed");
            });
        } catch (IOException e) {
            logger.error("Failed to rebuild scene", e);
        }
    }

    public static void main(String[] args) {
        System.setProperty("file.encoding", "UTF-8");
        launch();
    }
}