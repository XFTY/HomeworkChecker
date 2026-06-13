package com.xfty.homeworkchecker.controller.setupWizard.wizard;

import javafx.scene.Parent;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.scene.text.Font;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

public class FontSettingsStepHandler {

    private static final Logger logger = LoggerFactory.getLogger(FontSettingsStepHandler.class);

    private final Consumer<String> onFontFamilyChanged;
    private final Consumer<Double> onFontSizeChanged;

    public FontSettingsStepHandler(Consumer<String> onFontFamilyChanged, Consumer<Double> onFontSizeChanged) {
        this.onFontFamilyChanged = onFontFamilyChanged;
        this.onFontSizeChanged = onFontSizeChanged;
    }

    public void setupStep(Parent root, String currentFontFamily, double currentFontSize) {
        ChoiceBox<String> fontFamilyChoice = (ChoiceBox<String>) root.lookup("#fontFamilyChoice");
        Slider fontSizeSlider = (Slider) root.lookup("#fontSizeSlider");
        Label fontSizeLabel = (Label) root.lookup("#fontSizeLabel");
        TextArea fontPreview = (TextArea) root.lookup("#fontPreview");

        if (fontFamilyChoice != null) {
            fontFamilyChoice.getItems().addAll(Font.getFamilies());
            fontFamilyChoice.setValue(currentFontFamily);
            fontFamilyChoice.valueProperty().addListener((obs, old, val) -> {
                if (val != null) {
                    onFontFamilyChanged.accept(val);
                    updateFontPreview(fontPreview, val, getCurrentFontSize());
                }
            });
        }

        if (fontSizeSlider != null) {
            fontSizeSlider.setValue(currentFontSize);
            fontSizeSlider.valueProperty().addListener((obs, old, val) -> {
                double size = val.doubleValue();
                onFontSizeChanged.accept(size);
                if (fontSizeLabel != null) {
                    fontSizeLabel.setText(String.format("%.0f", val));
                }
                updateFontPreview(fontPreview, getCurrentFontFamily(), size);
            });
            if (fontSizeLabel != null) {
                fontSizeLabel.setText(String.format("%.0f", currentFontSize));
            }
        }

        updateFontPreview(fontPreview, currentFontFamily, currentFontSize);
    }

    private String currentFontFamily = "System";

    public String getCurrentFontFamily() {
        return currentFontFamily;
    }

    public void setCurrentFontFamily(String family) {
        this.currentFontFamily = family;
    }

    private double currentFontSize = 17.0;

    public double getCurrentFontSize() {
        return currentFontSize;
    }

    public void setCurrentFontSize(double size) {
        this.currentFontSize = size;
    }

    private void updateFontPreview(TextArea preview, String family, double size) {
        if (preview != null) {
            preview.setFont(new Font(family, size));
        }
    }
}
