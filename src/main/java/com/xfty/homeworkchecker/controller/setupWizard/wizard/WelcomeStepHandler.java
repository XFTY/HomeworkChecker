package com.xfty.homeworkchecker.controller.setupWizard.wizard;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WelcomeStepHandler {

    private static final Logger logger = LoggerFactory.getLogger(WelcomeStepHandler.class);

    private final Button backButton;
    private final Button nextButton;

    private Timeline welcomeTimeline;
    private SequentialTransition welcomeEntranceAnimation;
    private TranslateTransition iconFloatAnimation;

    public WelcomeStepHandler(Button backButton, Button nextButton) {
        this.backButton = backButton;
        this.nextButton = nextButton;
    }

    public void stopAnimations() {
        if (welcomeTimeline != null) { welcomeTimeline.stop(); welcomeTimeline = null; }
        if (welcomeEntranceAnimation != null) { welcomeEntranceAnimation.stop(); welcomeEntranceAnimation = null; }
        if (iconFloatAnimation != null) { iconFloatAnimation.stop(); iconFloatAnimation = null; }
    }

    public void setupStep(Parent root) {
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
}
