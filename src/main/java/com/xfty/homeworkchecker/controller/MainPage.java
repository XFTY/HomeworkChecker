package com.xfty.homeworkchecker.controller;

import com.xfty.homeworkchecker.Idf;
import com.xfty.homeworkchecker.model.CardItem;
import com.xfty.homeworkchecker.service.HomeworkDatabase;
import com.xfty.homeworkchecker.service.ui.mainPage.CardUiService;
import com.xfty.homeworkchecker.service.ui.mainPage.EditMainService;
import com.xfty.homeworkchecker.service.ui.mainPage.EditStateService;
import com.xfty.homeworkchecker.service.ui.mainPage.LockService;
import com.xfty.homeworkchecker.service.ui.mainPage.MainPageInitService;
import com.xfty.homeworkchecker.service.ui.mainPage.PopupService;
import com.xfty.homeworkchecker.service.ui.mainPage.ReminderCardService;
import com.xfty.homeworkchecker.service.ui.mainPage.TopButtonService;
import com.xfty.homeworkchecker.service.ui.settings.DataBaseEditorService;
import com.xfty.homeworkchecker.service.ui.mainPage.WindowListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ResourceBundle;

public class MainPage implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(MainPage.class);

    @FXML
    private Button screenShotButton;
    @FXML
    private Button historyHomeworkButton;
    @FXML
    private Button settingsButton;

    @FXML
    private Label titleLabel;

    @FXML
    private TextArea editMain;
    @FXML
    private HBox lockModelShowingArea;

    @FXML
    private ImageView lockStatusImageView;
    @FXML
    private Label lockStatusLabel;

    @FXML
    private Label versionDisplay;

    @FXML
    private Pane blackPane;

    @FXML
    private AnchorPane centerShowingArea;
    @FXML
    private HBox centerHighControl;
    @FXML
    private VBox centerOutBox;
    @FXML
    private AnchorPane showingMainArea;
    @FXML
    private SplitPane mainSplitPane;
    @FXML
    private VBox cardContainer;
    @FXML
    private ScrollPane cardScrollPane;
    @FXML
    private VBox cardList;
    @FXML
    private Button addCardButton;
    @FXML
    private StackPane emptyPlaceholder;
    @FXML
    private Label emptyHintLabel;

    HomeworkDatabase homeworkDatabase = new HomeworkDatabase();

    private final MainPageInitService mainPageInitService = new MainPageInitService();
    private TopButtonService.ScreenshotService screenshotService;
    private TopButtonService.HistoryHomeworkService historyHomeworkService;
    private TopButtonService.SettingsService settingsService;
    private PopupService popupService;
    private EditMainService editMainService;
    private EditStateService editStateService;
    private WindowListener windowListener;
    private CardUiService cardUiService;
    private LockService lockService;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        logger.info("Initializing MainPage...");

        popupService = new PopupService(
            centerShowingArea, blackPane, centerOutBox, showingMainArea
        );

        ReminderCardService reminderCardService = new ReminderCardService();

        screenshotService = new TopButtonService.ScreenshotService(
            editMain, screenShotButton, reminderCardService);
        historyHomeworkService = new TopButtonService.HistoryHomeworkService(popupService);
        editMainService = new EditMainService(lockStatusLabel, lockModelShowingArea);
        editMainService.setEditMain(editMain);

        cardUiService = new CardUiService(
            cardList, addCardButton, emptyPlaceholder, emptyHintLabel,
            cardContainer, cardScrollPane,
            showingMainArea, centerShowingArea, centerOutBox, blackPane,
            mainSplitPane,
            popupService, reminderCardService,
            editMainService::onEditMainClicked
        );

        editMainService.setImagePasteHandler(image -> {
            String imagePath = reminderCardService.saveImageToDisk(image);
            if (imagePath == null) {
                logger.warn("Failed to save pasted image to disk");
                return null;
            }
            int num = cardUiService.getNextImageNumber();
            CardItem card = new CardItem(
                CardItem.Severity.INFO,
                "图片 " + num,
                "",
                ReminderCardService.generateTimestamp()
            );
            card.setImagePath(imagePath);
            card.setCreatedDate(Idf.year + Idf.month + Idf.day);
            reminderCardService.addCard(card);
            cardUiService.loadCards();
            logger.info("Created image card #{} with path: {}", num, imagePath);
            return num;
        });

        lockService = new LockService(
            editMain, lockStatusImageView, lockStatusLabel, homeworkDatabase
        );

        settingsService = new TopButtonService.SettingsService(
            popupService, homeworkDatabase, editMain,
            () -> {
                mainPageInitService.clearTodayHomework(editMain);
                cardUiService.removeTodayImageCards();
            }
        );
        settingsService.setOnFontChanged(() -> {
            if (Idf.userConfig != null) {
                cardUiService.setFontConfig(
                    Idf.userConfig.getJSONObject("font")
                        .getJSONObject("fontFamily")
                        .getString("defaultFontFamily"),
                    Idf.userConfig.getJSONObject("font")
                        .getJSONObject("textSize")
                        .getInteger("editMain") - 1
                );
                cardUiService.loadCards();
            }
        });
        editStateService = new EditStateService(
            editMain, homeworkDatabase, this::onLockModuleClicked,
            cardUiService::isEditingCard
        );
        windowListener = new WindowListener(titleLabel, editMain, this::onLockModuleClicked);

        logger.debug("ScreenshotService initialized with editMain reference");
        logger.debug("PopupService initialized with UI components");
        logger.debug("HistoryHomeworkService initialized");
        logger.debug("SettingsService initialized");
        logger.debug("EditStateService initialized with editMain and homeworkDatabase");

        logger.debug("Current date: {}-{}-{}", Idf.year, Idf.month, Idf.day);
        logger.debug("Current editable state: {}", Idf.isEditable);
        logger.debug("User config is null: {}", Idf.userConfig == null);
        logger.debug("User language is null: {}", Idf.userLanguage == null);

        versionDisplay.setText("Ver: " + Idf.softwareVersion);
        logger.debug("Version display set to: {}", versionDisplay.getText());

        historyHomeworkButton.getStyleClass().add("functional-button");
        logger.debug("Title label set to: {}", titleLabel.getText());

        logger.debug("Adding window listener");
        windowListener.addWindowListener();

        if (!Idf.isEditable) {
            editMain.setDisable(true);
            logger.debug("EditMain disabled on initialization");
        }

        new DataBaseEditorService().performAutoCleanup();

        MainPageInitService.Result result = mainPageInitService.loadHomeworkContent();
        editMain.setText(result.getContent());
        titleLabel.setText(result.getTitle());

        MainPageInitService.FontConfig fontConfig = mainPageInitService.getFontConfig();
        editMain.setFont(new Font(fontConfig.getFontFamily(), fontConfig.getFontSize()));
        cardUiService.setFontConfig(fontConfig.getFontFamily(), fontConfig.getFontSize() - 1);
        logger.debug("Font set successfully, card font size: {}, font family: {}",
            fontConfig.getFontSize() - 1, fontConfig.getFontFamily());

        editMain.setDisable(false);
        editMain.setEditable(false);

        cardUiService.cleanupEmptyCardsOnStartup();
        cardUiService.loadCards();
        if (!Idf.isEditable) {
            addCardButton.setManaged(false);
            addCardButton.setVisible(false);
        }

        cardUiService.setupSplitPaneListener();

        logger.info("MainPage initialized successfully");
    }

    @FXML
    protected void onscreenShotButtonPressed() {
        cardUiService.checkActiveCardEditing(() -> screenshotService.takeScreenshot());
    }

    @FXML
    protected void onEditMainClicked() {
        editMainService.onEditMainClicked();
    }

    @FXML
    public void onLockModuleClicked() {
        logger.info("LockModule clicked. Current editable state: {}", Idf.isEditable);
        logger.debug("Current editMain text length: {}", editMain.getText().length());

        if (Idf.isEditable) {
            cardUiService.checkActiveCardEditing(() -> {
                lockService.lock();
                editStateService.stopUnlockCounter();
                cardUiService.collapseAddCardBox();
            });
        } else {
            lockService.unlock();
            editStateService.startUnlockCounter();
            cardUiService.expandAddCardBox();
        }
        logger.debug("New editable state: {}", Idf.isEditable);
    }

    @FXML
    protected void onHistoryHomeworkButtonPressed() {
        cardUiService.checkActiveCardEditing(() -> {
            Idf.isPreviewWindowShowing = true;
            logger.debug("Set isPreviewWindowShowing to true");
            historyHomeworkService.openHistoryHomeworkDialog();
        });
    }

    @FXML
    protected void onSettingsButtonClicked() {
        cardUiService.checkActiveCardEditing(() -> {
            Idf.isPreviewWindowShowing = true;
            logger.debug("Set isPreviewWindowShowing to true");
            settingsService.openSettingsDialog();
        });
    }

    @FXML
    protected void onAddCard() {
        cardUiService.onAddCard();
    }

    public void cleanup() {
        logger.info("Starting resource cleanup...");

        if (cardUiService != null) {
            cardUiService.cleanupEditingCard();
            logger.info("CardUiService cleanup completed");
        }

        if (editMainService != null) {
            editMainService.cleanup();
            logger.info("EditMainService cleanup completed");
        }

        if (editStateService != null) {
            editStateService.stopUnlockCounter();
            logger.info("EditStateService cleanup completed");
        }

        logger.info("Resource cleanup completed");
    }
}
