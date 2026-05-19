package com.xfty.homeworkchecker.controller.setupWizard;

import com.xfty.homeworkchecker.Entry;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * SetupWizardController — 首次运行设置向导控制器（5 步）
 * <p>
 * 步骤：①欢迎（多语言轮播+动画）→ ②语言选择 → ③字体设置 → ④初始模板 → ⑤完成
 * 每一步通过 FXML 独立文件加载，带淡入动画效果。完成后回调 FileInitManager 初始化。
 * </p>
 */
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
    private Circle step1Circle;
    @FXML
    private Circle step2Circle;
    @FXML
    private Circle step3Circle;
    @FXML
    private Circle step4Circle;
    @FXML
    private Circle step5Circle;

    @FXML
    private Label step1Label;
    @FXML
    private Label step2Label;
    @FXML
    private Label step3Label;
    @FXML
    private Label step4Label;
    @FXML
    private Label step5Label;

    // ==================== 步骤状态 ====================

    /** 当前步骤索引（0-4） */
    private int currentStep = 0;
    private static final int TOTAL_STEPS = 5;

    // ==================== 用户选择 ====================

    /** 用户选择语言代码 */
    private String selectedLanguageCode;
    /** 用户选择字体族 */
    private String selectedFontFamily = "System";
    /** 用户选择字号 */
    private double selectedFontSize = 17.0;
    /** 用户输入初始模板 */
    private String initTemplate = "";

    /** 系统语言包（基于系统默认 Locale） */
    private ResourceBundle wizardBundle;
    /** 步骤语言包（用户选择语言后更新） */
    private ResourceBundle stepBundle;

    /** 向导完成回调 */
    private WizardFinishedCallback onFinished;

    // ==================== 动画引用 ====================

    /** 语言页面标题轮播动画 */
    private Timeline languageTitleTimeline;
    /** 欢迎页文字轮播动画 */
    private Timeline welcomeTimeline;
    /** 欢迎页入口序列动画 */
    private SequentialTransition welcomeEntranceAnimation;
    /** 欢迎图标浮动动画 */
    private TranslateTransition iconFloatAnimation;

    /** 步骤圆圈列表 */
    private List<Circle> stepCircles;
    /** 步骤标签列表 */
    private List<Label> stepLabels;

    /**
     * 初始化向导：加载系统 Locale 语言包，加载第 0 步（欢迎页）
     */
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        stepCircles = Arrays.asList(step1Circle, step2Circle, step3Circle, step4Circle, step5Circle);
        stepLabels = Arrays.asList(step1Label, step2Label, step3Label, step4Label, step5Label);
        loadSystemLocaleBundle();
        loadStep(currentStep);
        updateStepIndicator();
        updateNavigationButtons();
    }

    /**
     * 加载系统默认 Locale 的语言包
     */
    private void loadSystemLocaleBundle() {
        Locale systemLocale = Locale.getDefault();
        try {
            wizardBundle = ResourceBundle.getBundle("com/xfty/homeworkchecker/i18n/language", systemLocale);
        } catch (Exception e) {
            wizardBundle = ResourceBundle.getBundle("com/xfty/homeworkchecker/i18n/language", Locale.ENGLISH);
        }
        stepBundle = wizardBundle;
    }

    /**
     * 设置向导完成回调
     */
    public void setOnWizardFinished(WizardFinishedCallback callback) {
        this.onFinished = callback;
    }

    /**
     * 上一步按钮
     */
    @FXML
    private void onBackClicked() {
        if (currentStep > 0) {
            currentStep--;
            loadStep(currentStep);
            updateStepIndicator();
            updateNavigationButtons();
        }
    }

    /**
     * 下一步/完成按钮：最后一步触发回调，否则校验→收集数据→进入下一步
     */
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

    /**
     * 校验当前步骤数据是否完整（语言步骤必须选择语言）
     */
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

    /**
     * 收集当前步骤的用户输入：字体步骤获取字体/字号，模板步骤获取模板内容
     */
    private void collectCurrentStepData() {
        if (currentStep == 2) {
            Parent root = contentArea.getChildren().size() > 0 ? (Parent) contentArea.getChildren().get(0) : null;
            if (root != null) {
                ChoiceBox<String> fontFamilyChoice = (ChoiceBox<String>) root.lookup("#fontFamilyChoice");
                Slider fontSizeSlider = (Slider) root.lookup("#fontSizeSlider");
                if (fontFamilyChoice != null) {
                    selectedFontFamily = fontFamilyChoice.getValue();
                }
                if (fontSizeSlider != null) {
                    selectedFontSize = fontSizeSlider.getValue();
                }
            }
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

    /**
     * 加载指定步骤的 FXML 并调用对应设置方法
     */
    private void loadStep(int step) {
        if (languageTitleTimeline != null) {
            languageTitleTimeline.stop();
            languageTitleTimeline = null;
        }
        if (welcomeTimeline != null) {
            welcomeTimeline.stop();
            welcomeTimeline = null;
        }
        if (welcomeEntranceAnimation != null) {
            welcomeEntranceAnimation.stop();
            welcomeEntranceAnimation = null;
        }
        if (iconFloatAnimation != null) {
            iconFloatAnimation.stop();
            iconFloatAnimation = null;
        }

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

            if (step == 0) {
                setupWelcomeStep(stepRoot);
            } else if (step == 1) {
                setupLanguageStep(stepRoot);
            } else if (step == 2) {
                setupFontSettingsStep(stepRoot);
            } else if (step == 3) {
                setupTemplateStep(stepRoot);
            } else if (step == 4) {
                setupFinishStep(stepRoot);
            }

            applyFadeAnimation(stepRoot);
        } catch (IOException e) {
            logger.error("Failed to load step {}: {}", step, fxmlPaths[step], e);
        }
    }

    /**
     * 设置欢迎页（第 0 步）：Logo + 应用名 + 多语言欢迎语轮播动画 + 图标浮动动画
     */
    private void setupWelcomeStep(Parent root) {
        VBox rootContainer = (VBox) root.lookup("#rootContainer");
        Pane iconTextPane = (Pane) root.lookup("#iconTextPane");
        ImageView welcomeIcon = (ImageView) root.lookup("#welcomeIcon");
        Label appNameLabel = (Label) root.lookup("#appNameLabel");
        Label welcomeTitle = (Label) root.lookup("#welcomeTitle");
        Label welcomeSubtitle = (Label) root.lookup("#welcomeSubtitle");
        if (welcomeTitle == null || welcomeSubtitle == null) return;

        double paneW = iconTextPane.getPrefWidth();
        double gap = 15;
        Text measurer = new Text("HomeworkChecker");
        measurer.setFont(Font.font(28));
        double textW = measurer.getLayoutBounds().getWidth();
        double combinedW = 64 + gap + textW;
        double combinedStartX = (paneW - combinedW) / 2;
        double iconFinalX = combinedStartX;
        double textFinalX = combinedStartX + 64 + gap;

        welcomeIcon.setLayoutX(iconFinalX);
        welcomeIcon.setLayoutY((80 - 64) / 2);
        appNameLabel.setLayoutX(textFinalX);
        appNameLabel.setLayoutY((80 - 28) / 2);

        double iconCenterOffset = (paneW / 2 - 32) - iconFinalX;
        welcomeIcon.setTranslateX(iconCenterOffset);
        welcomeIcon.setScaleX(0.6);
        welcomeIcon.setScaleY(0.6);
        welcomeIcon.setOpacity(0);

        double iconFinalCenter = iconFinalX + 32;
        double textInitialTx = iconFinalCenter - textFinalX;
        appNameLabel.setTranslateX(textInitialTx);
        appNameLabel.setOpacity(0);

        welcomeTitle.setOpacity(0);
        welcomeSubtitle.setOpacity(0);

        String[][] welcomeWords = {
            {"zh_CN", "欢迎", "请按\u201c下一步\u201d继续", "上一步", "下一步"},
            {"zh_HK", "歡迎", "請按\u201c下一步\u201d繼續", "上一步", "下一步"},
            {"en_US", "Welcome", "Press \u201cNext\u201d to continue", "Back", "Next"},
            {"es_ES", "Bienvenido", "Presione \u201cSiguiente\u201d para continuar", "Atrás", "Siguiente"},
            {"fr_FR", "Bienvenue", "Appuyez sur \u201cSuivant\u201d pour continuer", "Retour", "Suivant"},
            {"ar_SA", "مرحبا", "اضغط على \u201cالتالي\u201d للمتابعة", "رجوع", "التالي"},
            {"ru_RU", "Добро пожаловать", "Нажмите \u201cДалее\u201d, чтобы продолжить", "Назад", "Далее"},
            {"bn_BD", "স্বাগতম", "চালিয়ে যেতে \u201cপরবর্তী\u201d টিপুন", "পিছনে", "পরবর্তী"},
            {"de_DE", "Willkommen", "Drücken Sie \u201cWeiter\u201d, um fortzufahren", "Zurück", "Weiter"},
            {"ja_JP", "ようこそ", "\u201c次へ\u201dを押して続行", "戻る", "次へ"},
            {"pt_PT", "Bem-vindo", "Prima \u201cSeguinte\u201d para continuar", "Voltar", "Seguinte"}
        };
        int[] idx = {1};
        welcomeTitle.setText(welcomeWords[0][1]);
        welcomeSubtitle.setText(welcomeWords[0][2]);
        backButton.setText(welcomeWords[0][3]);
        nextButton.setText(welcomeWords[0][4]);

        Interpolator easeOutCustom = Interpolator.SPLINE(0.0, 0.0, 0.58, 1.0);
        Interpolator easeInOutCustom = Interpolator.SPLINE(0.25, 0.1, 0.25, 1.0);

        Timeline phase1 = new Timeline(
            new KeyFrame(Duration.millis(1000),
                new KeyValue(welcomeIcon.opacityProperty(), 1, easeOutCustom),
                new KeyValue(welcomeIcon.scaleXProperty(), 1, easeOutCustom),
                new KeyValue(welcomeIcon.scaleYProperty(), 1, easeOutCustom)
            )
        );

        Timeline phase2 = new Timeline(
            new KeyFrame(Duration.millis(700),
                new KeyValue(welcomeIcon.translateXProperty(), 0, easeInOutCustom),
                new KeyValue(appNameLabel.translateXProperty(), 0, easeInOutCustom),
                new KeyValue(appNameLabel.opacityProperty(), 1, easeOutCustom)
            )
        );

        Timeline phase3 = new Timeline(
            new KeyFrame(Duration.millis(500),
                new KeyValue(rootContainer.translateYProperty(), -30, easeInOutCustom)
            )
        );

        Timeline phase4 = new Timeline(
            new KeyFrame(Duration.millis(500),
                new KeyValue(welcomeTitle.opacityProperty(), 1, easeOutCustom)
            )
        );

        Timeline phase5 = new Timeline(
            new KeyFrame(Duration.millis(500),
                new KeyValue(welcomeSubtitle.opacityProperty(), 1, easeOutCustom)
            )
        );

        welcomeEntranceAnimation = new SequentialTransition(phase1, phase2, phase3, phase4, phase5);

        iconFloatAnimation = new TranslateTransition(Duration.seconds(3), welcomeIcon);
        iconFloatAnimation.setByY(-5);
        iconFloatAnimation.setCycleCount(Timeline.INDEFINITE);
        iconFloatAnimation.setAutoReverse(true);

        welcomeTimeline = new Timeline(new KeyFrame(Duration.seconds(4), e -> {
            Timeline fadeOut = new Timeline(
                new KeyFrame(Duration.millis(250),
                    new KeyValue(welcomeTitle.opacityProperty(), 0),
                    new KeyValue(welcomeTitle.translateYProperty(), -12, Interpolator.EASE_OUT),
                    new KeyValue(welcomeSubtitle.opacityProperty(), 0),
                    new KeyValue(welcomeSubtitle.translateYProperty(), -12, Interpolator.EASE_OUT)
                )
            );
            fadeOut.setOnFinished(e2 -> {
                welcomeTitle.setText(welcomeWords[idx[0]][1]);
                welcomeSubtitle.setText(welcomeWords[idx[0]][2]);
                backButton.setText(welcomeWords[idx[0]][3]);
                nextButton.setText(welcomeWords[idx[0]][4]);
                idx[0] = (idx[0] + 1) % welcomeWords.length;
                welcomeTitle.setTranslateY(12);
                welcomeSubtitle.setTranslateY(12);
                Timeline fadeIn = new Timeline(
                    new KeyFrame(Duration.millis(250),
                        new KeyValue(welcomeTitle.opacityProperty(), 1),
                        new KeyValue(welcomeTitle.translateYProperty(), 0, Interpolator.EASE_OUT),
                        new KeyValue(welcomeSubtitle.opacityProperty(), 1),
                        new KeyValue(welcomeSubtitle.translateYProperty(), 0, Interpolator.EASE_OUT)
                    )
                );
                fadeIn.play();
            });
            fadeOut.play();
        }));
        welcomeTimeline.setCycleCount(Timeline.INDEFINITE);

        welcomeEntranceAnimation.setOnFinished(e -> {
            welcomeTitle.setTranslateY(0);
            welcomeSubtitle.setTranslateY(0);
            iconFloatAnimation.play();
            welcomeTimeline.play();
        });
        welcomeEntranceAnimation.play();
    }

    /**
     * 设置语言选择页（第 1 步）：11 个语言按钮 + 标题轮播
     */
    private void setupLanguageStep(Parent root) {
        GridPane grid = (GridPane) root.lookup("#languageGrid");
        if (grid == null) return;

        List<String[]> languages = Arrays.asList(
            new String[]{"chineseButton", "zh_CN"},
            new String[]{"traditionalChineseButton", "zh_HK"},
            new String[]{"englishButton", "en_US"},
            new String[]{"spanishButton", "es_ES"},
            new String[]{"frenchButton", "fr_FR"},
            new String[]{"arabicButton", "ar_SA"},
            new String[]{"russianButton", "ru_RU"},
            new String[]{"bengaliButton", "bn_BD"},
            new String[]{"germanButton", "de_DE"},
            new String[]{"japaneseButton", "ja_JP"},
            new String[]{"portugueseButton", "pt_PT"}
        );

        for (String[] lang : languages) {
            AnchorPane button = (AnchorPane) root.lookup("#" + lang[0]);
            if (button != null) {
                String code = lang[1];
                button.setOnMouseClicked(e -> selectLanguage(code, root));
                button.setOnMouseEntered(e -> {
                    if (!code.equals(selectedLanguageCode)) {
                        button.setStyle("-fx-background-color: #4a4a4a; -fx-background-radius: 10;");
                    }
                });
                button.setOnMouseExited(e -> {
                    if (!code.equals(selectedLanguageCode)) {
                        button.setStyle("-fx-background-color: #575757; -fx-background-radius: 10;");
                    }
                });
            }
        }

        Label titleLabel = (Label) root.lookup("#languageTitle");
        if (titleLabel != null) {
            String[] rotateLocales = {
                "zh_CN", "zh_HK", "en_US", "es_ES", "fr_FR",
                "ar_SA", "ru_RU", "bn_BD", "de_DE", "ja_JP", "pt_PT"
            };
            String firstParts[] = rotateLocales[0].split("_");
            Locale firstLoc = new Locale.Builder().setLanguage(firstParts[0]).setRegion(firstParts[1]).build();
            titleLabel.setText(ResourceBundle.getBundle("com/xfty/homeworkchecker/i18n/language", firstLoc).getString("setup.language.title"));
            int[] idx = {1};
            languageTitleTimeline = new Timeline(new KeyFrame(Duration.seconds(4), e -> {
                Timeline fadeOut = new Timeline(
                    new KeyFrame(Duration.millis(300), new KeyValue(titleLabel.opacityProperty(), 0))
                );
                fadeOut.setOnFinished(e2 -> {
                    String code = rotateLocales[idx[0]];
                    String[] parts = code.split("_");
                    Locale loc = new Locale.Builder().setLanguage(parts[0]).setRegion(parts[1]).build();
                    titleLabel.setText(ResourceBundle.getBundle("com/xfty/homeworkchecker/i18n/language", loc).getString("setup.language.title"));
                    idx[0] = (idx[0] + 1) % rotateLocales.length;
                    Timeline fadeIn = new Timeline(
                        new KeyFrame(Duration.millis(300), new KeyValue(titleLabel.opacityProperty(), 1))
                    );
                    fadeIn.play();
                });
                fadeOut.play();
            }));
            languageTitleTimeline.setCycleCount(Timeline.INDEFINITE);
            languageTitleTimeline.play();
        }
    }

    /**
     * 选择语言：更新 stepBundle、高亮选中按钮、刷新步骤标签和导航按钮文字
     */
    private void selectLanguage(String languageCode, Parent root) {
        selectedLanguageCode = languageCode;
        String[] parts = languageCode.split("_");
        Locale locale;
        if (parts.length == 2) {
            locale = new Locale.Builder().setLanguage(parts[0]).setRegion(parts[1]).build();
        } else {
            locale = new Locale.Builder().setLanguage(parts[0]).build();
        }
        stepBundle = ResourceBundle.getBundle("com/xfty/homeworkchecker/i18n/language", locale);

        GridPane grid = (GridPane) root.lookup("#languageGrid");
        if (grid != null) {
            for (Node node : grid.getChildren()) {
                if (node instanceof AnchorPane btn) {
                    btn.setStyle("-fx-background-color: #575757; -fx-background-radius: 10;");
                }
            }
        }
        AnchorPane selectedBtn = (AnchorPane) root.lookup("#" + getLanguageButtonId(languageCode));
        if (selectedBtn != null) {
            selectedBtn.setStyle("-fx-background-color: #3a3a3a; -fx-background-radius: 10;");
        }

        updateStepLabels();
        updateNavigationButtons();
    }

    /**
     * 根据语言代码获取对应的按钮 fx:id
     */
    private String getLanguageButtonId(String languageCode) {
        return switch (languageCode) {
            case "zh_CN" -> "chineseButton";
            case "zh_HK" -> "traditionalChineseButton";
            case "en_US" -> "englishButton";
            case "es_ES" -> "spanishButton";
            case "fr_FR" -> "frenchButton";
            case "ar_SA" -> "arabicButton";
            case "ru_RU" -> "russianButton";
            case "bn_BD" -> "bengaliButton";
            case "de_DE" -> "germanButton";
            case "ja_JP" -> "japaneseButton";
            case "pt_PT" -> "portugueseButton";
            default -> "englishButton";
        };
    }

    /**
     * 设置字体页（第 2 步）：字体选择框 + 字号滑块 + 实时预览
     */
    private void setupFontSettingsStep(Parent root) {
        ChoiceBox<String> fontFamilyChoice = (ChoiceBox<String>) root.lookup("#fontFamilyChoice");
        Slider fontSizeSlider = (Slider) root.lookup("#fontSizeSlider");
        Label fontSizeLabel = (Label) root.lookup("#fontSizeLabel");
        TextArea fontPreview = (TextArea) root.lookup("#fontPreview");

        if (fontFamilyChoice != null) {
            fontFamilyChoice.getItems().addAll(Font.getFamilies());
            fontFamilyChoice.setValue(selectedFontFamily);
            fontFamilyChoice.valueProperty().addListener((obs, old, val) -> {
                if (val != null) {
                    selectedFontFamily = val;
                    updateFontPreview(fontPreview);
                }
            });
        }

        if (fontSizeSlider != null) {
            fontSizeSlider.setValue(selectedFontSize);
            fontSizeSlider.valueProperty().addListener((obs, old, val) -> {
                selectedFontSize = val.doubleValue();
                if (fontSizeLabel != null) {
                    fontSizeLabel.setText(String.format("%.0f", val));
                }
                updateFontPreview(fontPreview);
            });
            if (fontSizeLabel != null) {
                fontSizeLabel.setText(String.format("%.0f", selectedFontSize));
            }
        }

        updateFontPreview(fontPreview);
    }

    /**
     * 更新字体预览区域
     */
    private void updateFontPreview(TextArea preview) {
        if (preview != null) {
            preview.setFont(new Font(selectedFontFamily, selectedFontSize));
        }
    }

    /**
     * 设置初始模板页（第 3 步）：加载文本编辑器并填入已有模板
     */
    private void setupTemplateStep(Parent root) {
        TextArea templateText = (TextArea) root.lookup("#templateText");
        if (templateText != null) {
            templateText.setText(initTemplate);
        }
    }

    /**
     * 设置完成页（第 4 步）：展示用户选择的语言和字体摘要
     */
    private void setupFinishStep(Parent root) {
        Label finishTitle = (Label) root.lookup("#finishTitle");
        Label finishDesc = (Label) root.lookup("#finishDesc");
        Label summaryLanguage = (Label) root.lookup("#summaryLanguage");
        Label summaryFont = (Label) root.lookup("#summaryFont");
        if (finishTitle != null) finishTitle.setText(stepBundle.getString("setup.finish.title"));
        if (finishDesc != null) finishDesc.setText(stepBundle.getString("setup.finish.description"));
        if (summaryLanguage != null) {
            summaryLanguage.setText(stepBundle.getString("setup.finish.language") + " " + getLanguageDisplayName(selectedLanguageCode));
        }
        if (summaryFont != null) {
            summaryFont.setText(stepBundle.getString("setup.finish.font") + " " + selectedFontFamily + ", " + String.format("%.0f", selectedFontSize));
        }
    }

    /**
     * 根据语言代码获取语言的本地化显示名称
     */
    private String getLanguageDisplayName(String code) {
        return switch (code) {
            case "zh_CN" -> "简体中文";
            case "zh_HK" -> "繁體中文";
            case "en_US" -> "English";
            case "es_ES" -> "Español";
            case "fr_FR" -> "Français";
            case "ar_SA" -> "العربية";
            case "ru_RU" -> "Русский";
            case "bn_BD" -> "বাংলা";
            case "de_DE" -> "Deutsch";
            case "ja_JP" -> "日本語";
            case "pt_PT" -> "Português";
            default -> code;
        };
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
