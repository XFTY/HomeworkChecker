package com.xfty.homeworkchecker.controller.settings;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.animation.Transition;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IndexNavigationHelper {

    private static final Logger logger = LoggerFactory.getLogger(IndexNavigationHelper.class);

    static final String DEFAULT_BACKGROUND = "#1e1e1e";
    static final String HIGHLIGHT_BACKGROUND = "#3a3a3a";
    static final String HOVER_BACKGROUND = "#2a2a2a";
    static final String PRESSED_BACKGROUND = "#4a4a4a";

    private final ScrollPane rightShowingArea;
    private AnchorPane currentSelectedButton;

    public IndexNavigationHelper(ScrollPane rightShowingArea) {
        this.rightShowingArea = rightShowingArea;
    }

    public void applyFadeAnimation(Parent newContent) {
        logger.debug("Starting fade animation for new content");

        Node currentContent = rightShowingArea.getContent();
        Interpolator easeInOutInterpolator = Interpolator.SPLINE(0.42, 0, 0.58, 1.0);

        if (currentContent != null) {
            logger.debug("Fading out current content");
            Timeline fadeOutTimeline = new Timeline();
            KeyValue fadeOutOpacity = new KeyValue(currentContent.opacityProperty(), 0, easeInOutInterpolator);
            KeyFrame fadeOutFrame = new KeyFrame(Duration.millis(150), fadeOutOpacity);
            fadeOutTimeline.getKeyFrames().add(fadeOutFrame);

            fadeOutTimeline.setOnFinished(event -> {
                logger.debug("Fade out completed, setting new content");
                rightShowingArea.setContent(newContent);
                newContent.setOpacity(0);

                Timeline fadeInTimeline = new Timeline();
                KeyValue fadeInOpacity = new KeyValue(newContent.opacityProperty(), 1, easeInOutInterpolator);
                KeyFrame fadeInFrame = new KeyFrame(Duration.millis(200), fadeInOpacity);
                fadeInTimeline.getKeyFrames().add(fadeInFrame);
                fadeInTimeline.play();
                logger.debug("Started fade in animation for new content");
            });
            fadeOutTimeline.play();
        } else {
            logger.debug("No current content, setting new content and starting fade in");
            rightShowingArea.setContent(newContent);
            newContent.setOpacity(0);

            Timeline fadeInTimeline = new Timeline();
            KeyValue fadeInOpacity = new KeyValue(newContent.opacityProperty(), 1, easeInOutInterpolator);
            KeyFrame fadeInFrame = new KeyFrame(Duration.millis(200), fadeInOpacity);
            fadeInTimeline.getKeyFrames().add(fadeInFrame);
            fadeInTimeline.play();
            logger.debug("Started fade in animation for new content");
        }
        logger.debug("Fade animation process initiated");
    }

    public void highlightButton(AnchorPane selectedButton) {
        logger.debug("Highlighting button: {}", selectedButton);

        if (currentSelectedButton != null) {
            animateBackgroundColor(currentSelectedButton, DEFAULT_BACKGROUND, null);
        }

        if (selectedButton != null) {
            animateBackgroundColor(selectedButton, HIGHLIGHT_BACKGROUND, null);
            currentSelectedButton = selectedButton;
        }
        logger.debug("Button highlighted successfully");
    }

    public void setupButtonHoverEffect(AnchorPane button) {
        final Timeline[] currentAnimation = {null};

        button.setOnMouseEntered(event -> {
            if (button != currentSelectedButton) {
                animateBackgroundColor(button, HOVER_BACKGROUND, currentAnimation);
            }
        });

        button.setOnMouseExited(event -> {
            if (button != currentSelectedButton) {
                animateBackgroundColor(button, DEFAULT_BACKGROUND, currentAnimation);
            }
        });

        button.setOnMousePressed(event -> {
            animateBackgroundColor(button, PRESSED_BACKGROUND, currentAnimation);
        });

        button.setOnMouseReleased(event -> {
            if (button == currentSelectedButton) {
                animateBackgroundColor(button, HIGHLIGHT_BACKGROUND, currentAnimation);
            } else {
                animateBackgroundColor(button, DEFAULT_BACKGROUND, currentAnimation);
            }
        });
    }

    public void animateBackgroundColor(AnchorPane button, String targetColorHex, Timeline[] currentAnimation) {
        if (currentAnimation != null && currentAnimation[0] != null) {
            currentAnimation[0].stop();
        }

        Color targetColor = Color.web(targetColorHex);
        Color startColor;
        try {
            String currentStyle = button.getStyle();
            if (currentStyle != null && currentStyle.contains("-fx-background-color:")) {
                int start = currentStyle.indexOf("#");
                if (start != -1) {
                    int end = Math.min(start + 7, currentStyle.length());
                    String hexColor = currentStyle.substring(start, end);
                    startColor = Color.web(hexColor);
                } else {
                    startColor = Color.web(DEFAULT_BACKGROUND);
                }
            } else {
                startColor = Color.web(DEFAULT_BACKGROUND);
            }
        } catch (Exception e) {
            startColor = Color.web(DEFAULT_BACKGROUND);
        }

        final Color finalStartColor = startColor;
        Transition colorTransition = new Transition() {
            {
                setCycleDuration(Duration.millis(150));
                setInterpolator(Interpolator.EASE_BOTH);
            }

            @Override
            protected void interpolate(double frac) {
                double red = finalStartColor.getRed() + (targetColor.getRed() - finalStartColor.getRed()) * frac;
                double green = finalStartColor.getGreen() + (targetColor.getGreen() - finalStartColor.getGreen()) * frac;
                double blue = finalStartColor.getBlue() + (targetColor.getBlue() - finalStartColor.getBlue()) * frac;

                Color currentColor = Color.color(red, green, blue);
                String hexColor = toHexColor(currentColor);
                button.setStyle("-fx-background-color: " + hexColor + "; -fx-background-radius: 10;");
            }
        };
        colorTransition.play();

        if (currentAnimation != null) {
            currentAnimation[0] = new Timeline(new KeyFrame(Duration.millis(150)));
        }
    }

    private static String toHexColor(Color color) {
        int r = (int) Math.round(color.getRed() * 255);
        int g = (int) Math.round(color.getGreen() * 255);
        int b = (int) Math.round(color.getBlue() * 255);
        return String.format("#%02x%02x%02x", r, g, b);
    }
}
