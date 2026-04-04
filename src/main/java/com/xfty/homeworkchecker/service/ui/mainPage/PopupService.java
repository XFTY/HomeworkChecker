package com.xfty.homeworkchecker.service.ui.mainPage;

import com.xfty.homeworkchecker.Idf;
import javafx.scene.Parent;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

/**
 * Popup service for managing popup windows and animations
 */
public class PopupService {
    
    private final AnchorPane centerShowingArea;
    private final Pane blackPane;
    private final VBox centerOutBox;
    private final AnchorPane showingMainArea;
    
    public PopupService(AnchorPane centerShowingArea,
                      Pane blackPane,
                      VBox centerOutBox,
                      AnchorPane showingMainArea) {
        this.centerShowingArea = centerShowingArea;
        this.blackPane = blackPane;
        this.centerOutBox = centerOutBox;
        this.showingMainArea = showingMainArea;
    }
    
    /**
     * Show a popup with fade-in animation and basic close handler
     * @param root Popup content
     * @param closeButtonId Close button ID selector
     * @return The close button object
     */
    public Circle showPopup(Parent root, String closeButtonId) {
        // Set opacity and scale for fade-in animation
        centerShowingArea.setOpacity(0);
        centerShowingArea.setScaleX(0.8);
        centerShowingArea.setScaleY(0.8);
        centerShowingArea.getChildren().clear();
        centerShowingArea.getChildren().add(root);
        
        GaussianBlur gaussianBlur = new GaussianBlur();
        gaussianBlur.setRadius(0);
        showingMainArea.setEffect(gaussianBlur);
        
        // Fade-in timeline
        javafx.animation.Timeline fadeInTimeline = new javafx.animation.Timeline();
        javafx.animation.Interpolator elasticInterpolator = javafx.animation.Interpolator.SPLINE(0.25, 0.1, 0.25, 1.0);
        
        // Blur keyframe
        javafx.animation.KeyFrame blurKeyFrame = new javafx.animation.KeyFrame(
            javafx.util.Duration.millis(400),
            new javafx.animation.KeyValue(gaussianBlur.radiusProperty(), 30)
        );
        
        // Opacity keyframe
        javafx.animation.KeyFrame opacityKeyFrame = new javafx.animation.KeyFrame(
            javafx.util.Duration.millis(400),
            new javafx.animation.KeyValue(centerShowingArea.opacityProperty(), 1, elasticInterpolator)
        );
        
        // Scale X keyframes
        javafx.animation.KeyFrame scaleXKeyFrame = new javafx.animation.KeyFrame(
            javafx.util.Duration.millis(300),
            new javafx.animation.KeyValue(centerShowingArea.scaleXProperty(), 1.1, elasticInterpolator)
        );
        
        javafx.animation.KeyFrame scaleXKeyFrame2 = new javafx.animation.KeyFrame(
            javafx.util.Duration.millis(500),
            new javafx.animation.KeyValue(centerShowingArea.scaleXProperty(), 1.0, elasticInterpolator)
        );
        
        // Scale Y keyframes
        javafx.animation.KeyFrame scaleYKeyFrame = new javafx.animation.KeyFrame(
            javafx.util.Duration.millis(300),
            new javafx.animation.KeyValue(centerShowingArea.scaleYProperty(), 1.1, elasticInterpolator)
        );
        
        javafx.animation.KeyFrame scaleYKeyFrame2 = new javafx.animation.KeyFrame(
            javafx.util.Duration.millis(500),
            new javafx.animation.KeyValue(centerShowingArea.scaleYProperty(), 1.0, elasticInterpolator)
        );
        
        // Black pane opacity keyframe
        javafx.animation.KeyFrame blackPaneKeyFrame = new javafx.animation.KeyFrame(
            javafx.util.Duration.millis(400),
            new javafx.animation.KeyValue(blackPane.opacityProperty(), 0.5, elasticInterpolator)
        );
        
        fadeInTimeline.getKeyFrames().addAll(
            blurKeyFrame, opacityKeyFrame, blackPaneKeyFrame,
            scaleXKeyFrame, scaleXKeyFrame2, scaleYKeyFrame, scaleYKeyFrame2
        );
        fadeInTimeline.play();
        
        blackPane.setOpacity(0);
        centerOutBox.setMouseTransparent(false);
        
        // Setup close button handler
        Circle windowCloseButton = (Circle) root.lookup(closeButtonId);
        windowCloseButton.setOnMouseClicked(event -> {
            closePopup();
            Idf.isPreviewWindowShowing = false;
        });
        
        return windowCloseButton;
    }
    
    /**
     * Close popup with fade-out animation
     */
    public void closePopup() {
        GaussianBlur gaussianBlur = (GaussianBlur) showingMainArea.getEffect();
        javafx.animation.Timeline fadeOutTimeline = new javafx.animation.Timeline();
        javafx.animation.Interpolator easeOutInterpolator = javafx.animation.Interpolator.EASE_OUT;
        
        // Blur out keyframe
        javafx.animation.KeyFrame blurOutKeyFrame = new javafx.animation.KeyFrame(
            javafx.util.Duration.millis(250),
            new javafx.animation.KeyValue(gaussianBlur.radiusProperty(), 0, easeOutInterpolator)
        );
        
        // Opacity out keyframe
        javafx.animation.KeyFrame opacityOutKeyFrame = new javafx.animation.KeyFrame(
            javafx.util.Duration.millis(250),
            new javafx.animation.KeyValue(centerShowingArea.opacityProperty(), 0, easeOutInterpolator)
        );
        
        // Scale out keyframes
        javafx.animation.KeyFrame scaleOutXKeyFrame = new javafx.animation.KeyFrame(
            javafx.util.Duration.millis(250),
            new javafx.animation.KeyValue(centerShowingArea.scaleXProperty(), 0.8, easeOutInterpolator)
        );
        
        javafx.animation.KeyFrame scaleOutYKeyFrame = new javafx.animation.KeyFrame(
            javafx.util.Duration.millis(250),
            new javafx.animation.KeyValue(centerShowingArea.scaleYProperty(), 0.8, easeOutInterpolator)
        );
        
        // Black pane opacity out keyframe
        javafx.animation.KeyFrame blackPaneOutKeyFrame = new javafx.animation.KeyFrame(
            javafx.util.Duration.millis(250),
            new javafx.animation.KeyValue(blackPane.opacityProperty(), 0, easeOutInterpolator)
        );
        
        fadeOutTimeline.getKeyFrames().addAll(
            blurOutKeyFrame, opacityOutKeyFrame, blackPaneOutKeyFrame,
            scaleOutXKeyFrame, scaleOutYKeyFrame
        );
        fadeOutTimeline.setOnFinished(e -> {
            centerShowingArea.getChildren().clear();
            centerOutBox.setMouseTransparent(true);
            showingMainArea.setEffect(null);
        });
        fadeOutTimeline.play();
    }
}
