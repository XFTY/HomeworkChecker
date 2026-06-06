package com.xfty.homeworkchecker.service.ui.mainPage;

import com.xfty.homeworkchecker.Idf;
import com.xfty.homeworkchecker.model.CardItem;
import com.xfty.homeworkchecker.service.HomeworkDatabase;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * TopButtonService — 顶栏按钮服务
 * <p>
 * 管理主界面顶栏的按钮功能：截屏（Canvas 合成 + 剪切板）、历史作业弹窗、设置弹窗、关于弹窗。
 * 每个功能作为独立静态内部类实现。
 * </p>
 */
public class TopButtonService {
    
    private static final Logger logger = LoggerFactory.getLogger(TopButtonService.class);
    
    /**
     * ScreenshotService — 截屏服务（内部类）
     * <p>
     * 将编辑区内容渲染为图片（含日期头部），保存到系统剪贴板，带按钮闪烁动画反馈。
     * </p>
     */
    public static class ScreenshotService {

        private final TextArea editMain;
        private final Button screenShotButton;
        private final ReminderCardService reminderCardService;
        private boolean animating;

        public ScreenshotService(TextArea editMain, Button screenShotButton,
                                 ReminderCardService reminderCardService) {
            this.editMain = editMain;
            this.screenShotButton = screenShotButton;
            this.reminderCardService = reminderCardService;
        }
        
        /**
         * Take a screenshot of the edit main area and save it to clipboard
         */
        public void takeScreenshot() {
            logger.info("Taking screenshot");
            
            try {
                String textContent = editMain.getText();
                logger.debug("Text content length: {}", textContent.length());

                LocalDate today = LocalDate.now();
                String dateText = today.getYear() + "\u5E74"
                    + String.format("%02d", today.getMonthValue()) + "\u6708"
                    + String.format("%02d", today.getDayOfMonth()) + "\u65E5 "
                    + today.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.CHINESE);

                final double renderScale = 2.0;
                int imageWidth = (int) (editMain.getWidth() * renderScale);
                int headerHeight = (int) (52 * renderScale);
                int padding = (int) (12 * renderScale);
                int contentWidth = Math.max(imageWidth - padding * 2, 1);

                Font contentFont = new Font(editMain.getFont().getFamily(), 16 * renderScale);
                Font dateFont = new Font(16 * renderScale);

                List<String> wrappedLines = wrapText(textContent, contentFont, contentWidth);
                if (wrappedLines.isEmpty()) {
                    wrappedLines.add("");
                }
                double lineHeight = computeLineHeight(contentFont);
                int contentAreaHeight = (int) (wrappedLines.size() * lineHeight + padding * 2);

                int totalHeight = headerHeight + contentAreaHeight;

                Canvas canvas = new Canvas(imageWidth, totalHeight);
                GraphicsContext gc = canvas.getGraphicsContext2D();

                // Draw header background
                gc.setFill(Color.web("#1e1e1e"));
                gc.fillRect(0, 0, imageWidth, headerHeight);

                // Draw separator line
                gc.setStroke(Color.web("#3a3a3a"));
                gc.setLineWidth(renderScale);
                gc.strokeLine(0, headerHeight - renderScale / 2, imageWidth, headerHeight - renderScale / 2);

                // Draw date text
                gc.setFont(dateFont);
                Text measureText = new Text(dateText);
                measureText.setFont(dateFont);
                double textHeight = measureText.getLayoutBounds().getHeight();
                gc.setFill(Color.WHITE);
                gc.fillText(dateText, 16 * renderScale, (headerHeight - textHeight) / 2 + textHeight * 0.85);

                // Draw content background (same color as editMain)
                gc.setFill(Color.web("#2d2d2d"));
                gc.fillRect(0, headerHeight, imageWidth, contentAreaHeight);

                // Draw text content line by line
                gc.setFill(Color.WHITE);
                gc.setFont(contentFont);
                double y = headerHeight + padding + lineHeight * 0.85;
                for (String line : wrappedLines) {
                    if (!line.isEmpty()) {
                        gc.fillText(line, padding, y);
                    }
                    y += lineHeight;
                }

                WritableImage resultImage = canvas.snapshot(null, null);
                logger.debug("Composite image created with text content, dimensions: {}x{}",
                    resultImage.getWidth(), resultImage.getHeight());

                List<CardItem> cards = reminderCardService.readCards();
                List<CardItem> imageCards = new ArrayList<>();
                for (CardItem card : cards) {
                    if (card.getImagePath() != null && !card.getImagePath().isEmpty()) {
                        imageCards.add(card);
                    }
                }

                if (!imageCards.isEmpty()) {
                    int separatorHeight = (int) (40 * renderScale);
                    int imageSpacing = (int) (4 * renderScale);

                    List<Image> loadedImages = new ArrayList<>();
                    List<double[]> imageDims = new ArrayList<>();
                    int imagesTotalHeight = 0;
                    for (CardItem card : imageCards) {
                        Image img = reminderCardService.loadImageForCard(card.getImagePath());
                        if (img != null) {
                            double imgW = img.getWidth();
                            double imgH = img.getHeight();
                            double logicalContentWidth = contentWidth / renderScale;
                            if (imgW > logicalContentWidth) {
                                double scale = logicalContentWidth / imgW;
                                imgW = logicalContentWidth;
                                imgH *= scale;
                            }
                            imgW *= renderScale;
                            imgH *= renderScale;
                            loadedImages.add(img);
                            imageDims.add(new double[]{imgW, imgH});
                            imagesTotalHeight += separatorHeight + (int) imgH + imageSpacing;
                        }
                    }

                    if (!loadedImages.isEmpty()) {
                        int finalTotalHeight = totalHeight + imagesTotalHeight;
                        Canvas finalCanvas = new Canvas(imageWidth, finalTotalHeight);
                        GraphicsContext finalGc = finalCanvas.getGraphicsContext2D();

                        finalGc.drawImage(resultImage, 0, 0);

                        int currentY = totalHeight;
                        int imgIndex = 1;

                        for (int i = 0; i < loadedImages.size(); i++) {
                            Image img = loadedImages.get(i);
                            double[] dims = imageDims.get(i);
                            double imgW = dims[0];
                            double imgH = dims[1];

                            finalGc.setFill(Color.web("#252525"));
                            finalGc.fillRect(0, currentY, imageWidth, separatorHeight);

                            finalGc.setStroke(Color.web("#555555"));
                            finalGc.setLineWidth(renderScale);
                            finalGc.strokeLine(0, currentY + renderScale / 2, imageWidth, currentY + renderScale / 2);

                            String labelText = "\u56FE\u7247 " + imgIndex;
                            finalGc.setFont(contentFont);
                            Text labelMeasure = new Text(labelText);
                            labelMeasure.setFont(contentFont);
                            double labelH = labelMeasure.getLayoutBounds().getHeight();
                            finalGc.setFill(Color.web("#cccccc"));
                            finalGc.fillText(labelText, padding,
                                currentY + (separatorHeight - labelH) / 2 + labelH * 0.85);

                            currentY += separatorHeight;

                            double imgX = padding + (contentWidth - imgW) / 2;
                            finalGc.drawImage(img, imgX, currentY, imgW, imgH);

                            currentY += (int) imgH + imageSpacing;
                            imgIndex++;
                        }

                        resultImage = finalCanvas.snapshot(null, null);
                        logger.debug("Images appended to screenshot, final dimensions: {}x{}",
                            resultImage.getWidth(), resultImage.getHeight());
                    }
                }

                Clipboard clipboard = Clipboard.getSystemClipboard();
                logger.debug("Getting system clipboard");

                ClipboardContent content = new ClipboardContent();
                content.putImage(resultImage);
                logger.debug("Image content added to clipboard");

                clipboard.setContent(content);
                logger.info("Screenshot saved to clipboard with date overlay and text content");

                animateButtonSuccess();
                logger.debug("Screenshot success animation shown");

            } catch (Exception e) {
                logger.error("Failed to save screenshot", e);
                showFailureAlert();
                logger.debug("Screenshot failure alert shown");
            }
            
            logger.debug("Screenshot operation completed");
        }

        private List<String> wrapText(String text, Font font, double maxWidth) {
            List<String> lines = new ArrayList<>();
            Text measurer = new Text();
            measurer.setFont(font);
            for (String paragraph : text.split("\n", -1)) {
                if (paragraph.isEmpty()) {
                    lines.add("");
                    continue;
                }
                StringBuilder currentLine = new StringBuilder();
                for (int i = 0; i < paragraph.length(); i++) {
                    String testStr = currentLine.toString() + paragraph.charAt(i);
                    measurer.setText(testStr);
                    if (measurer.getLayoutBounds().getWidth() > maxWidth && !currentLine.isEmpty()) {
                        lines.add(currentLine.toString());
                        currentLine = new StringBuilder(String.valueOf(paragraph.charAt(i)));
                    } else {
                        currentLine.append(paragraph.charAt(i));
                    }
                }
                if (!currentLine.isEmpty()) {
                    lines.add(currentLine.toString());
                }
            }
            return lines;
        }

        private double computeLineHeight(Font font) {
            Text t = new Text("A");
            t.setFont(font);
            return t.getLayoutBounds().getHeight() + 4;
        }
        
        /**
         * Animate button to show screenshot success — green blink for 6 seconds
         * with fade-in (300ms) and fade-out (300ms) transitions
         */
        private void animateButtonSuccess() {
            if (animating) return;
            animating = true;

            String originalText = screenShotButton.getText();
            String originalStyle = screenShotButton.getStyle();

            Color lightGreen = Color.web("#66bb6a");
            Color darkGreen = Color.web("#1b5e20");

            String successText = Idf.userLanguageBundle.getString("mainpage.snapshot.success.button");
            screenShotButton.setText(successText);

            ObjectProperty<Color> bgColorProp = new SimpleObjectProperty<>(getButtonBackgroundColor());
            bgColorProp.addListener((obs, oldColor, newColor) -> {
                String webColor = String.format("#%02x%02x%02x",
                    (int) (newColor.getRed() * 255),
                    (int) (newColor.getGreen() * 255),
                    (int) (newColor.getBlue() * 255));
                screenShotButton.setStyle("-fx-background-color: " + webColor + "; -fx-text-fill: white;");
            });

            double fadeIn = 300;
            double blinkTotal = 6000;
            double fadeOut = 300;

            Timeline timeline = new Timeline();

            // Phase 1: Fade in (0→300ms) — bg: originalBg → lightGreen
            timeline.getKeyFrames().setAll(
                new KeyFrame(Duration.ZERO,
                    new KeyValue(bgColorProp, getButtonBackgroundColor(), Interpolator.LINEAR)),
                new KeyFrame(Duration.millis(fadeIn),
                    new KeyValue(bgColorProp, lightGreen, Interpolator.LINEAR))
            );

            // Phase 2: Blink (300→6300ms, 3 cycles of 2s)
            for (int i = 0; i < 3; i++) {
                double cycleStart = fadeIn + i * 2000;
                timeline.getKeyFrames().addAll(
                    new KeyFrame(Duration.millis(cycleStart + 1000),
                        new KeyValue(bgColorProp, darkGreen, Interpolator.LINEAR)),
                    new KeyFrame(Duration.millis(cycleStart + 2000),
                        new KeyValue(bgColorProp, lightGreen, Interpolator.LINEAR))
                );
            }

            // Phase 3: Fade out (6300→6600ms) — bg: lightGreen → originalBg
            timeline.getKeyFrames().addAll(
                new KeyFrame(Duration.millis(fadeIn + blinkTotal),
                    new KeyValue(bgColorProp, lightGreen, Interpolator.LINEAR)),
                new KeyFrame(Duration.millis(fadeIn + blinkTotal + fadeOut), e -> {
                    animating = false;
                    screenShotButton.setText(originalText);
                    screenShotButton.setStyle(originalStyle);
                },
                    new KeyValue(bgColorProp, getButtonBackgroundColor(), Interpolator.LINEAR))
            );

            timeline.play();
        }

        private Color getButtonBackgroundColor() {
            Background bg = screenShotButton.getBackground();
            if (bg != null && !bg.getFills().isEmpty()) {
                Paint fill = bg.getFills().get(0).getFill();
                if (fill instanceof Color) {
                    return (Color) fill;
                }
            }
            return Color.web("#2d2d2d");
        }
        
        /**
         * Show failure alert when screenshot fails
         */
        private void showFailureAlert() {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(Idf.userLanguageBundle.getString("mainpage.snapshot.failure.title"));
            alert.setHeaderText(Idf.userLanguageBundle.getString("mainpage.snapshot.failure.title"));
            alert.setContentText(Idf.userLanguageBundle.getString("mainpage.snapshot.failure.header"));
            alert.showAndWait();
        }
    }
    
    /**
     * History homework service for managing history homework dialog
     */
    public static class HistoryHomeworkService {
        
        private final PopupService popupService;
        
        public HistoryHomeworkService(PopupService popupService) {
            this.popupService = popupService;
        }
        
        /**
         * Open history homework dialog
         */
        public void openHistoryHomeworkDialog() {
            logger.info("Opening history homework dialog");
            
            try {
                logger.debug("Loading history homework FXML from: /com/xfty/homeworkchecker/fxml/loadHistoryHomework.fxml");
                FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/xfty/homeworkchecker/fxml/loadHistoryHomework.fxml")
                );
                
                // Set resource bundle for internationalization
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
                
                // Show popup with basic close handler
                popupService.showPopup(root, "#windowCloseButton");
                logger.debug("History homework dialog popup created");
                
            } catch (Exception e) {
                logger.error("Error opening history homework dialog", e);
            }
            
            logger.debug("History homework button press handler completed");
        }
    }
    
    /**
     * Settings service for managing settings dialog
     */
    public static class SettingsService {
        
        private final PopupService popupService;
        private final HomeworkDatabase homeworkDatabase;
        private final TextArea editMain;
        private final Runnable clearTodayHomeworkCallback;
        private Runnable onFontChanged;
        
        public SettingsService(PopupService popupService, 
                              HomeworkDatabase homeworkDatabase,
                              TextArea editMain,
                              Runnable clearTodayHomeworkCallback) {
            this.popupService = popupService;
            this.homeworkDatabase = homeworkDatabase;
            this.editMain = editMain;
            this.clearTodayHomeworkCallback = clearTodayHomeworkCallback;
        }

        public void setOnFontChanged(Runnable onFontChanged) {
            this.onFontChanged = onFontChanged;
        }
        
        /**
         * Open settings dialog with complex close handler
         */
        public void openSettingsDialog() {
            logger.info("Opening settings window");
            
            try {
                logger.debug("Loading settings FXML from: /com/xfty/homeworkchecker/fxml/settings/index.fxml");
                FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/xfty/homeworkchecker/fxml/settings/index.fxml")
                );
                
                // Set resource bundle for internationalization
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
                logger.info("Settings FXML loaded successfully");
                
                // Show popup and get close button
                Circle windowCloseButton = popupService.showPopup(root, "#windowCloseButton");
                logger.debug("Popup with close handler created");
                
                // Setup clear button handler
                Platform.runLater(() -> {
                    try {
                        logger.debug("Looking up clear button");
                        Button clearButton = (Button) root.lookup("#clearAllHomeworkContentButton");
                        if (clearButton != null) {
                            logger.debug("Clear button found");
                            clearButton.setOnAction(buttonEvent -> {
                                logger.info("Clear button clicked");
                                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                                alert.setTitle(Idf.userLanguageBundle.getString("mainpage.confirmReset.title"));
                                alert.setHeaderText(Idf.userLanguageBundle.getString("mainpage.confirmReset.header"));
                                alert.setContentText(Idf.userLanguageBundle.getString("mainpage.confirmReset.content"));
                                
                                alert.showAndWait().ifPresent(response -> {
                                    if (response == javafx.scene.control.ButtonType.OK) {
                                        logger.info("User confirmed clear operation");
                                        clearTodayHomeworkCallback.run();
                                        logger.info("Today's homework content has been cleared");
                                    } else {
                                        logger.debug("User cancelled clear operation");
                                    }
                                });
                            });
                        } else {
                            logger.debug("Clear button not found in settings window");
                        }
                    } catch (Exception e) {
                        logger.error("Error setting up clear button listener", e);
                    }
                });
                
                // Setup custom close handler
                windowCloseButton.setOnMouseClicked(mouseEvent -> {
                    logger.info("Settings window close button clicked");
                    homeworkDatabase.updateConfig(Idf.userConfig);
                    logger.info("User closed settings window");

                    // Reset centerShowingArea layout state (in case settings was zoomed)
                    AnchorPane centerShowingArea = (AnchorPane) root.getScene().lookup("#centerShowingArea");
                    HBox centerHighControl = (HBox) root.getScene().lookup("#centerHighControl");
                    if (centerShowingArea != null) {
                        centerShowingArea.setPrefSize(364.0, 306.0);
                        centerShowingArea.setMaxWidth(-1);
                        centerShowingArea.setMaxHeight(-1);
                        HBox.setHgrow(centerShowingArea, null);
                    }
                    if (centerHighControl != null) {
                        centerHighControl.setMaxHeight(-1);
                        VBox.setVgrow(centerHighControl, null);
                    }
                    
                    // Update font if config is not null
                    if (Idf.userConfig != null) {
                        logger.debug("Updating font after settings closed");
                        editMain.setFont(new Font(
                            Idf.userConfig.getJSONObject("font")
                                .getJSONObject("fontFamily")
                                .getString("defaultFontFamily"),
                            Idf.userConfig.getJSONObject("font")
                                .getJSONObject("textSize")
                                .getInteger("editMain")));
                        logger.debug("Font updated after settings closed");
                        if (onFontChanged != null) {
                            onFontChanged.run();
                        }
                    }
                    
                    // Clear homework if needed
                    if (Idf.needHomeworkShowingAreaClear) {
                        logger.info("Need to clear homework showing area after settings");
                        clearTodayHomeworkCallback.run();
                        logger.info("Today's homework content has been cleared [v2]");
                        Idf.needHomeworkShowingAreaClear = false;
                    }
                    
                    popupService.closePopup();
                    logger.debug("Popup closed");
                    
                    Idf.isPreviewWindowShowing = false;
                    logger.debug("Set isPreviewWindowShowing to false");
                });
                
            } catch (Exception e) {
                logger.error("Error opening settings window", e);
            }
            
            logger.debug("Settings button click handler completed");
        }
    }
}