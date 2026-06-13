package com.xfty.homeworkchecker.service.ui.mainPage;

import com.xfty.homeworkchecker.Idf;
import com.xfty.homeworkchecker.model.CardItem;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.Background;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
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

public class ScreenshotService {

    private static final Logger logger = LoggerFactory.getLogger(ScreenshotService.class);

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

            Font contentFont = new Font(editMain.getFont().getFamily(), editMain.getFont().getSize() * renderScale);
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

            gc.setFill(Color.web("#1e1e1e"));
            gc.fillRect(0, 0, imageWidth, headerHeight);

            gc.setStroke(Color.web("#3a3a3a"));
            gc.setLineWidth(renderScale);
            gc.strokeLine(0, headerHeight - renderScale / 2, imageWidth, headerHeight - renderScale / 2);

            gc.setFont(dateFont);
            Text measureText = new Text(dateText);
            measureText.setFont(dateFont);
            double textHeight = measureText.getLayoutBounds().getHeight();
            gc.setFill(Color.WHITE);
            gc.fillText(dateText, 16 * renderScale, (headerHeight - textHeight) / 2 + textHeight * 0.85);

            gc.setFill(Color.web("#2d2d2d"));
            gc.fillRect(0, headerHeight, imageWidth, contentAreaHeight);

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
            String expanded = paragraph.replace("\t", "    ");
            StringBuilder currentLine = new StringBuilder();
            for (int i = 0; i < expanded.length(); i++) {
                String testStr = currentLine.toString() + expanded.charAt(i);
                measurer.setText(testStr);
                if (measurer.getLayoutBounds().getWidth() > maxWidth && !currentLine.isEmpty()) {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder(String.valueOf(expanded.charAt(i)));
                } else {
                    currentLine.append(expanded.charAt(i));
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

        timeline.getKeyFrames().setAll(
            new KeyFrame(Duration.ZERO,
                new KeyValue(bgColorProp, getButtonBackgroundColor(), Interpolator.LINEAR)),
            new KeyFrame(Duration.millis(fadeIn),
                new KeyValue(bgColorProp, lightGreen, Interpolator.LINEAR))
        );

        for (int i = 0; i < 3; i++) {
            double cycleStart = fadeIn + i * 2000;
            timeline.getKeyFrames().addAll(
                new KeyFrame(Duration.millis(cycleStart + 1000),
                    new KeyValue(bgColorProp, darkGreen, Interpolator.LINEAR)),
                new KeyFrame(Duration.millis(cycleStart + 2000),
                    new KeyValue(bgColorProp, lightGreen, Interpolator.LINEAR))
            );
        }

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

    private void showFailureAlert() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(Idf.userLanguageBundle.getString("mainpage.snapshot.failure.title"));
        alert.setHeaderText(Idf.userLanguageBundle.getString("mainpage.snapshot.failure.title"));
        alert.setContentText(Idf.userLanguageBundle.getString("mainpage.snapshot.failure.header"));
        alert.showAndWait();
    }
}
