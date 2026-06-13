package com.xfty.homeworkchecker.service.ui.mainPage;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShakeAnimationHelper {

    private static final Logger logger = LoggerFactory.getLogger(ShakeAnimationHelper.class);

    private ShakeAnimationHelper() {
    }

    public static void addShakeKeyFrames(ObservableList<KeyFrame> keyFrames,
                                          double startTime, double amplitude, int cycles,
                                          Node target) {
        logger.trace("Adding {} shake keyframes starting at {} ms with amplitude {}", cycles, startTime, amplitude);

        double currentTime = startTime;
        for (int cycle = 0; cycle < cycles; cycle++) {
            keyFrames.add(new KeyFrame(
                Duration.millis(currentTime),
                new KeyValue(target.translateXProperty(), -amplitude)
            ));
            currentTime += 50;

            keyFrames.add(new KeyFrame(
                Duration.millis(currentTime),
                new KeyValue(target.translateXProperty(), 0)
            ));
            currentTime += 25;

            keyFrames.add(new KeyFrame(
                Duration.millis(currentTime),
                new KeyValue(target.translateXProperty(), amplitude)
            ));
            currentTime += 50;

            keyFrames.add(new KeyFrame(
                Duration.millis(currentTime),
                new KeyValue(target.translateXProperty(), 0)
            ));
            currentTime += 25;
        }

        logger.trace("Completed adding {} shake keyframes, ending at {} ms", cycles, currentTime);
    }
}
