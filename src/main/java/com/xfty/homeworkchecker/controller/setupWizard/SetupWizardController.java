package com.xfty.homeworkchecker.controller.setupWizard;

import com.xfty.homeworkchecker.Entry;
import com.xfty.homeworkchecker.controller.setupWizard.wizard.FinishStepHandler;
import com.xfty.homeworkchecker.controller.setupWizard.wizard.FontSettingsStepHandler;
import com.xfty.homeworkchecker.controller.setupWizard.wizard.LanguageStepHandler;
import com.xfty.homeworkchecker.controller.setupWizard.wizard.WelcomeStepHandler;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

public class SetupWizardController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(SetupWizardController.class);

    // ==================== FXML 注入 ====================

    @FXML
    private StackPane contentArea;
    @FXML
    private Button backButton;
    @FXML
    private Button nextButton;

    // ==================== 步骤指示器 ====================

    @FXML
    private Circle step1Circle, step2Circle, step3Circle, step4Circle, step5Circle;
    @FXML
    private Label step1Label, step2Label, step3Label, step4Label, step5Label;

    // ==================== 步骤状态 ====================

    private int currentStep = 0;
    private static final int TOTAL_STEPS = 5;

    // ==================== 用户选择 ====================

    private String selectedLanguageCode;
    private String selectedFontFamily = "System";
    private double selectedFontSize = 17.0;
    private String initTemplate = "";
    private ResourceBundle wizardBundle;
    private ResourceBundle stepBundle;

    private WizardFinishedCallback onFinished;
    private List<Circle> stepCircles;
    private List<Label> stepLabels;

    // ==================== 步骤处理器 ====================

    private WelcomeStepHandler welcomeStepHandler;
    private LanguageStepHandler languageStepHandler;
    private FontSettingsStepHandler fontSettingsStepHandler;
    private FinishStepHandler finishStepHandler;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        stepCircles = Arrays.asList(step1Circle, step2Circle, step3Circle, step4Circle, step5Circle);
        stepLabels = Arrays.asList(step1Label, step2Label, step3Label, step4Label, step5Label);

        welcomeStepHandler = new WelcomeStepHandler(backButton, nextButton);
        languageStepHandler = new LanguageStepHandler();
        languageStepHandler.setOnLanguageChanged(() -> {
            updateStepLabels();
            updateNavigationButtons();
        });
        fontSettingsStepHandler = new FontSettingsStepHandler(
            family -> { selectedFontFamily = family; fontSettingsStepHandler.setCurrentFontFamily(family); },
            size -> { selectedFontSize = size; fontSettingsStepHandler.setCurrentFontSize(size); }
        );
        finishStepHandler = new FinishStepHandler();

        loadSystemLocaleBundle();
        loadStep(currentStep);
        updateStepIndicator();
        updateNavigationButtons();
    }

    private void loadSystemLocaleBundle() {
        Locale systemLocale = Locale.getDefault();
        try {
            wizardBundle = ResourceBundle.getBundle("com/xfty/homeworkchecker/i18n/language", systemLocale);
        } catch (Exception e) {
            wizardBundle = ResourceBundle.getBundle("com/xfty/homeworkchecker/i18n/language", Locale.ENGLISH);
        }
        stepBundle = wizardBundle;
    }

    public void setOnWizardFinished(WizardFinishedCallback callback) {
        this.onFinished = callback;
    }

    @FXML
    private void onBackClicked() {
        if (currentStep > 0) {
            currentStep--;
            loadStep(currentStep);
            updateStepIndicator();
            updateNavigationButtons();
        }
    }

    @FXML
    private void onNextClicked() {
        if (currentStep == TOTAL_STEPS - 1) {
            if (onFinished != null) {
                onFinished.onFinished(selectedLanguageCode, selectedFontFamily, selectedFontSize, initTemplate);
            }
            return;
        }

        if (!validateCurrentStep()) {
            return;
        }

        collectCurrentStepData();
        currentStep++;
        loadStep(currentStep);
        updateStepIndicator();
        updateNavigationButtons();
    }

    private boolean validateCurrentStep() {
        if (currentStep == 1 && selectedLanguageCode == null) {
            ResourceBundle bundle = stepBundle != null ? stepBundle : wizardBundle;
            if (bundle != null) {
                showAlert(bundle.getString("setup.language.alert.title"), bundle.getString("setup.language.alert.message"));
            }
            return false;
        }
        return true;
    }

    private void collectCurrentStepData() {
        if (currentStep == 2) {
            selectedFontFamily = fontSettingsStepHandler.getCurrentFontFamily();
            selectedFontSize = fontSettingsStepHandler.getCurrentFontSize();
        } else if (currentStep == 3) {
            Parent root = contentArea.getChildren().size() > 0 ? (Parent) contentArea.getChildren().get(0) : null;
            if (root != null) {
                TextArea templateText = (TextArea) root.lookup("#templateText");
                if (templateText != null) {
                    initTemplate = templateText.getText();
                }
            }
        }
    }

    private void loadStep(int step) {
        welcomeStepHandler.stopAnimations();
        languageStepHandler.stopAnimation();

        String[] fxmlPaths = {
            "fxml/setupWizard/welcome.fxml",
            "fxml/setupWizard/language.fxml",
            "fxml/setupWizard/fontSettings.fxml",
            "fxml/setupWizard/initialTemplate.fxml",
            "fxml/setupWizard/finish.fxml"
        };

        try {
            FXMLLoader loader = new FXMLLoader(Entry.class.getResource(fxmlPaths[step]));
            if (step >= 2 && stepBundle != null) {
                loader.setResources(stepBundle);
            }
            if (step == 1 && wizardBundle != null) {
                loader.setResources(wizardBundle);
            }
            Parent stepRoot = loader.load();

            switch (step) {
                case 0 -> welcomeStepHandler.setupStep(stepRoot);
                case 1 -> languageStepHandler.setupStep(stepRoot, wizardBundle);
                case 2 -> setupTemplateStepContent(stepRoot);
                case 3 -> setupInitialTemplateStep(stepRoot);
                case 4 -> finishStepHandler.setupStep(stepRoot, stepBundle, selectedLanguageCode,
                    selectedFontFamily, selectedFontSize);
            }

            applyFadeAnimation(stepRoot);
        } catch (IOException e) {
            logger.error("Failed to load step {}: {}", step, fxmlPaths[step], e);
        }
    }

    private void setupTemplateStepContent(Parent root) {
        fontSettingsStepHandler.setupStep(root, selectedFontFamily, selectedFontSize);
    }

    private void setupInitialTemplateStep(Parent root) {
        TextArea templateText = (TextArea) root.lookup("#templateText");
        if (templateText != null) {
            templateText.setText(initTemplate);
        }
    }

    private void applyFadeAnimation(Parent newContent) {
        newContent.setOpacity(0);
        contentArea.getChildren().clear();
        contentArea.getChildren().add(newContent);

        Timeline fadeIn = new Timeline();
        KeyValue kv = new KeyValue(newContent.opacityProperty(), 1, Interpolator.SPLINE(0.42, 0, 0.58, 1.0));
        KeyFrame kf = new KeyFrame(Duration.millis(200), kv);
        fadeIn.getKeyFrames().add(kf);
        fadeIn.play();
    }

    private void updateStepIndicator() {
        for (int i = 0; i < TOTAL_STEPS; i++) {
            Circle circle = stepCircles.get(i);
            if (circle == null) continue;
            if (i < currentStep) {
                circle.setFill(Color.web("#4CAF50"));
                circle.setStroke(Color.web("#4CAF50"));
            } else if (i == currentStep) {
                circle.setFill(Color.web("#0080ff"));
                circle.setStroke(Color.web("#0080ff"));
            } else {
                circle.setFill(Color.web("#1e1e1e"));
                circle.setStroke(Color.web("#575757"));
            }
        }
        updateStepLabels();
    }

    private void updateStepLabels() {
        String[] keys = {"setup.step.welcome", "setup.step.language", "setup.step.font",
                         "setup.step.template", "setup.step.finish"};
        ResourceBundle bundle = stepBundle != null ? stepBundle : wizardBundle;
        for (int i = 0; i < TOTAL_STEPS; i++) {
            Label label = stepLabels.get(i);
            if (label != null && bundle != null) {
                try {
                    label.setText(bundle.getString(keys[i]));
                } catch (Exception e) {
                    logger.warn("Missing i18n key: {}", keys[i]);
                }
            }
        }
    }

    private void updateNavigationButtons() {
        ResourceBundle bundle = stepBundle != null ? stepBundle : wizardBundle;
        if (backButton != null) {
            backButton.setDisable(currentStep == 0);
            if (bundle != null) {
                try {
                    backButton.setText(bundle.getString("setup.button.back"));
                } catch (Exception e) {
                    backButton.setText("Back");
                }
            }
        }
        if (nextButton != null) {
            if (currentStep == TOTAL_STEPS - 1) {
                if (bundle != null) {
                    try {
                        nextButton.setText(bundle.getString("setup.button.finish"));
                    } catch (Exception e) {
                        nextButton.setText("Finish");
                    }
                }
            } else {
                if (bundle != null) {
                    try {
                        nextButton.setText(bundle.getString("setup.button.next"));
                    } catch (Exception e) {
                        nextButton.setText("Next");
                    }
                }
            }
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FunctionalInterface
    public interface WizardFinishedCallback {
        void onFinished(String languageCode, String fontFamily, double fontSize, String initTemplate);
    }
}
