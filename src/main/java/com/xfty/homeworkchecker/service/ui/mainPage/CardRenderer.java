package com.xfty.homeworkchecker.service.ui.mainPage;

import com.alibaba.fastjson2.JSONObject;
import com.xfty.homeworkchecker.Entry;
import com.xfty.homeworkchecker.Idf;
import com.xfty.homeworkchecker.model.CardItem;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class CardRenderer {

    private static final Logger logger = LoggerFactory.getLogger(CardRenderer.class);

    private final ReminderCardService reminderCardService;
    private final PopupService popupService;
    private final AnchorPane showingMainArea;
    private final AnchorPane centerShowingArea;
    private final VBox centerOutBox;
    private final Pane blackPane;

    public CardRenderer(ReminderCardService reminderCardService, PopupService popupService,
                        AnchorPane showingMainArea, AnchorPane centerShowingArea,
                        VBox centerOutBox, Pane blackPane) {
        this.reminderCardService = reminderCardService;
        this.popupService = popupService;
        this.showingMainArea = showingMainArea;
        this.centerShowingArea = centerShowingArea;
        this.centerOutBox = centerOutBox;
        this.blackPane = blackPane;
    }

    public VBox renderCard(CardItem item, int index,
                           String cardFontFamily, double cardFontSize,
                           int editingCardIndex, boolean isCardEditing,
                           Runnable onEditMainClicked,
                           java.util.function.Consumer<Integer> onEdit,
                           java.util.function.Consumer<Integer> onDelete,
                           CardDragHandler dragHandler) {
        double iconSize = cardFontSize * 1.3;

        VBox cardRoot = new VBox();
        cardRoot.getStyleClass().addAll("card-item", severityStyleClass(item.getSeverity()));
        cardRoot.setFillWidth(true);

        int currentIndex = index;

        VBox cardBody = new VBox();
        cardBody.getStyleClass().add("card-body");

        HBox topRow = new HBox(8);
        topRow.setAlignment(Pos.TOP_LEFT);

        ImageView iconView = buildSeverityIcon(item.getSeverity(), iconSize);

        VBox contentArea = new VBox(3);
        contentArea.getStyleClass().add("card-content-area");
        HBox.setHgrow(contentArea, Priority.ALWAYS);

        Label titleLabel = new Label(item.getTitle());
        titleLabel.getStyleClass().add("card-title");
        titleLabel.setFont(Font.font(cardFontFamily, FontWeight.BOLD, cardFontSize));

        Label contentLabel = new Label(item.getContent());
        contentLabel.getStyleClass().add("card-content");
        contentLabel.setFont(Font.font(cardFontFamily, FontWeight.NORMAL, Math.max(cardFontSize - 2, 11)));

        Label timeLabel = new Label(item.getTimestamp());
        timeLabel.getStyleClass().add("card-timestamp");

        HBox timeRow = new HBox(4);
        timeRow.setAlignment(Pos.CENTER_LEFT);
        timeRow.getStyleClass().add("card-timestamp-row");

        ImageView timeIcon = buildIcon("icon/card/时间.png", cardFontSize * 0.8);
        timeRow.getChildren().addAll(timeIcon, timeLabel);

        if (item.isPersistent()) {
            timeRow.getStyleClass().add("persistent");
            String persistentText = Idf.userLanguageBundle.getString("card.persistent.label");
            Label persistentLabel = new Label(persistentText);
            persistentLabel.getStyleClass().add("card-persistent-label");
            timeRow.getChildren().add(persistentLabel);
        }

        contentArea.getChildren().add(titleLabel);

        Node sliderControls = null;
        String imgPath = item.getImagePath();
        if (imgPath != null && !imgPath.isEmpty()) {
            Image img = reminderCardService.loadImageForCard(imgPath);
            if (img != null) {
                double savedWidth = item.getImageWidth();
                ImageView imageView = new ImageView(img);
                imageView.setPreserveRatio(true);
                imageView.setFitWidth(savedWidth);
                imageView.setPickOnBounds(true);
                imageView.getStyleClass().add("card-image");

                VBox imageBox = new VBox(imageView);
                imageBox.getStyleClass().add("card-image-container");
                imageBox.setFillWidth(true);
                contentArea.getChildren().add(imageBox);

                Separator separator = new Separator();
                separator.getStyleClass().add("card-image-separator");

                Label resizeLabel = new Label(
                    Idf.userLanguageBundle.getString("card.image.resize")
                );
                resizeLabel.getStyleClass().add("card-image-resize-label");

                double sMin = 50, sMax = 800;
                try {
                    JSONObject ic = Idf.userConfig.getJSONObject("imageCard");
                    if (ic != null) {
                        Double v = ic.getDouble("sliderMin");
                        if (v != null) sMin = v;
                        v = ic.getDouble("sliderMax");
                        if (v != null) sMax = v;
                    }
                } catch (Exception e) { /* use defaults */ }
                Slider sizeSlider = new Slider(sMin, sMax, savedWidth);
                sizeSlider.setShowTickLabels(false);
                sizeSlider.setShowTickMarks(false);
                sizeSlider.setMajorTickUnit(50);
                sizeSlider.setMinorTickCount(4);
                sizeSlider.setPrefWidth(Double.MAX_VALUE);
                sizeSlider.getStyleClass().add("card-image-slider");
                sizeSlider.disableProperty().bind(Idf.editableProperty.not());
                sizeSlider.valueProperty().addListener((obs, old, val) ->
                    imageView.setFitWidth(val.doubleValue())
                );
                sizeSlider.valueChangingProperty().addListener((obs, wasChanging, isChanging) -> {
                    if (!isChanging) {
                        double val = sizeSlider.getValue();
                        CardItem cardItem = reminderCardService.readCards().get(currentIndex);
                        cardItem.setImageWidth(val);
                        reminderCardService.updateCard(currentIndex, cardItem);
                    }
                });

                VBox sliderBox = new VBox(4, separator, resizeLabel, sizeSlider);
                sliderBox.setPadding(new Insets(0, 10, 0, 10));
                sliderBox.setFillWidth(true);
                sliderControls = sliderBox;
            }
        } else {
            contentArea.getChildren().add(contentLabel);
        }

        contentArea.getChildren().add(timeRow);

        topRow.getChildren().addAll(iconView, contentArea);

        HBox buttonBar = new HBox(4);
        buttonBar.getStyleClass().add("card-button-bar");
        buttonBar.setMaxWidth(Double.MAX_VALUE);
        buttonBar.setOpacity(0);
        buttonBar.setManaged(false);
        buttonBar.setUserData(null);

        double btnIconSize = cardFontSize * 0.9;
        Button deleteBtn = buildIconButton("icon/card/删除.png", btnIconSize, "card-action-button", "danger");
        deleteBtn.setOnAction(e -> onDelete.accept(currentIndex));

        boolean isImageCard = imgPath != null && !imgPath.isEmpty();
        if (isImageCard) {
            buttonBar.getChildren().add(deleteBtn);
        } else {
            Button editBtn = buildIconButton("icon/card/编辑.png", btnIconSize, "card-action-button");
            editBtn.setOnAction(e -> onEdit.accept(currentIndex));
            buttonBar.getChildren().addAll(editBtn, deleteBtn);
        }

        if (sliderControls != null) {
            cardBody.getChildren().addAll(topRow, sliderControls, buttonBar);
        } else {
            cardBody.getChildren().addAll(topRow, buttonBar);
        }
        cardRoot.getChildren().add(cardBody);

        cardRoot.setOnMouseEntered(e -> {
            if (editingCardIndex != currentIndex && Idf.isEditable) {
                Timeline running = (Timeline) buttonBar.getUserData();
                if (running != null) {
                    running.stop();
                }
                buttonBar.setManaged(true);
                Timeline fadeIn = new Timeline(
                    new KeyFrame(Duration.millis(200),
                        new KeyValue(buttonBar.opacityProperty(), 1, Interpolator.EASE_OUT)
                    )
                );
                fadeIn.setOnFinished(ev -> buttonBar.setUserData(null));
                buttonBar.setUserData(fadeIn);
                fadeIn.play();
            }
        });
        cardRoot.setOnMouseExited(e -> {
            if (editingCardIndex != currentIndex) {
                Timeline running = (Timeline) buttonBar.getUserData();
                if (running != null) {
                    running.stop();
                }
                Timeline fadeOut = new Timeline(
                    new KeyFrame(Duration.millis(200),
                        new KeyValue(buttonBar.opacityProperty(), 0, Interpolator.EASE_IN)
                    )
                );
                fadeOut.setOnFinished(ev -> {
                    buttonBar.setManaged(false);
                    buttonBar.setUserData(null);
                });
                buttonBar.setUserData(fadeOut);
                fadeOut.play();
            }
        });

        cardRoot.setOnMouseClicked(e -> {
            if (!Idf.isEditable && onEditMainClicked != null) {
                onEditMainClicked.run();
            }
        });

        if (dragHandler != null) {
            dragHandler.setupDragHandlers(cardRoot, index);
        }

        return cardRoot;
    }

    public VBox createEditCardBody(CardItem item, int index,
                                   String cardFontFamily, double cardFontSize,
                                   Runnable onSave, Runnable onCancel,
                                   java.util.function.Consumer<TextField> titleFieldConsumer,
                                   java.util.function.Consumer<TextArea> contentFieldConsumer) {
        double iconSize = cardFontSize * 1.3;
        VBox editBody = new VBox(4);
        editBody.getStyleClass().add("card-body");

        HBox topRow = new HBox(8);

        ImageView iconView = buildSeverityIcon(item.getSeverity(), iconSize);

        VBox editArea = new VBox(4);
        editArea.getStyleClass().add("card-content-area");
        HBox.setHgrow(editArea, Priority.ALWAYS);

        ToggleGroup severityGroup = new ToggleGroup();
        HBox severityBar = new HBox(6);
        severityBar.getStyleClass().add("card-severity-bar");

        ToggleButton infoBtn = buildSeverityToggle("icon/card/提示.png", iconSize * 0.85, severityGroup);
        infoBtn.setUserData(CardItem.Severity.INFO);
        ToggleButton warningBtn = buildSeverityToggle("icon/card/警告.png", iconSize * 0.75, severityGroup);
        warningBtn.setUserData(CardItem.Severity.WARNING);
        ToggleButton criticalBtn = buildSeverityToggle("icon/card/严重.png", iconSize * 0.75, severityGroup);
        criticalBtn.setUserData(CardItem.Severity.CRITICAL);

        switch (item.getSeverity()) {
            case WARNING -> warningBtn.setSelected(true);
            case CRITICAL -> criticalBtn.setSelected(true);
            default -> infoBtn.setSelected(true);
        }

        severityBar.getChildren().addAll(infoBtn, warningBtn, criticalBtn);

        TextField titleField = new TextField(item.getTitle());
        titleField.getStyleClass().add("card-edit-input");
        titleField.setPromptText(Idf.userLanguageBundle.getString("card.title.placeholder"));
        titleField.setFont(Font.font(cardFontFamily, cardFontSize));

        TextArea contentField = new TextArea(item.getContent());
        contentField.getStyleClass().add("card-edit-input");
        contentField.setPromptText(Idf.userLanguageBundle.getString("card.content.placeholder"));
        contentField.setWrapText(true);
        contentField.setFont(Font.font(cardFontFamily, Math.max(cardFontSize - 2, 11)));
        VBox.setVgrow(contentField, Priority.ALWAYS);

        if (titleFieldConsumer != null) titleFieldConsumer.accept(titleField);
        if (contentFieldConsumer != null) contentFieldConsumer.accept(contentField);

        editArea.getChildren().addAll(severityBar, titleField, contentField);

        HBox buttonBar = new HBox(4);
        buttonBar.getStyleClass().add("card-button-bar");
        buttonBar.setManaged(true);
        buttonBar.setVisible(true);

        Button persistentBtn = new Button(
            item.isPersistent()
                ? Idf.userLanguageBundle.getString("card.cancel.persistent")
                : Idf.userLanguageBundle.getString("card.set.persistent")
        );
        persistentBtn.getStyleClass().addAll("card-edit-text-button", "persistent-toggle");
        persistentBtn.setOnAction(e -> {
            item.setPersistent(!item.isPersistent());
            persistentBtn.setText(
                item.isPersistent()
                    ? Idf.userLanguageBundle.getString("card.cancel.persistent")
                    : Idf.userLanguageBundle.getString("card.set.persistent")
            );
        });

        Button saveBtn = new Button(Idf.userLanguageBundle.getString("card.save"));
        saveBtn.getStyleClass().addAll("card-edit-text-button", "primary");
        Button cancelBtn = new Button(Idf.userLanguageBundle.getString("card.cancel"));
        cancelBtn.getStyleClass().addAll("card-edit-text-button", "danger");

        saveBtn.setOnAction(e -> {
            item.setTitle(titleField.getText().trim());
            item.setContent(contentField.getText().trim());
            CardItem.Severity newSeverity = CardItem.Severity.INFO;
            Toggle sel = severityGroup.getSelectedToggle();
            if (sel != null && sel.getUserData() instanceof CardItem.Severity sev) {
                newSeverity = sev;
            }
            item.setSeverity(newSeverity);
            if (!item.getTitle().isEmpty() && !item.getContent().isEmpty()) {
                onSave.run();
            }
        });
        cancelBtn.setOnAction(e -> onCancel.run());

        buttonBar.getChildren().addAll(persistentBtn, saveBtn, cancelBtn);

        topRow.getChildren().addAll(iconView, editArea);
        buttonBar.setMaxWidth(Double.MAX_VALUE);
        editBody.getChildren().addAll(topRow, buttonBar);

        return editBody;
    }

    public void showCardSaveConfirmDialog(Runnable onSave, Runnable onDiscard, Runnable onCancel, boolean hasFullContent) {
        AnchorPane root = new AnchorPane();
        root.setStyle("-fx-background-color: #1e1e1e; -fx-background-radius: 20;");
        root.setPrefSize(400, 200);

        Label titleLabel = new Label(Idf.userLanguageBundle.getString("card.save.confirm.title"));
        titleLabel.setTextFill(Color.WHITE);
        titleLabel.setFont(new Font(18));
        AnchorPane.setTopAnchor(titleLabel, 20.0);
        AnchorPane.setLeftAnchor(titleLabel, 20.0);

        Label messageLabel = new Label(Idf.userLanguageBundle.getString("card.save.confirm.message"));
        messageLabel.setTextFill(Color.web("#cccccc"));
        messageLabel.setFont(new Font(14));
        messageLabel.setWrapText(true);
        AnchorPane.setTopAnchor(messageLabel, 55.0);
        AnchorPane.setLeftAnchor(messageLabel, 20.0);
        AnchorPane.setRightAnchor(messageLabel, 20.0);

        String saveText = hasFullContent
            ? Idf.userLanguageBundle.getString("card.save.confirm.save")
            : Idf.userLanguageBundle.getString("card.save.confirm.return.edit");
        Button saveBtn = new Button(saveText);
        saveBtn.getStyleClass().addAll("card-edit-text-button", "primary");
        saveBtn.setOnAction(e -> onSave.run());
        saveBtn.setStyle("-fx-text-fill: white; -fx-background-color: #4CAF50; -fx-background-radius: 5; -fx-padding: 6 16;");

        Button discardBtn = new Button(Idf.userLanguageBundle.getString("card.save.confirm.discard"));
        discardBtn.getStyleClass().addAll("card-edit-text-button", "danger");
        discardBtn.setOnAction(e -> onDiscard.run());
        discardBtn.setStyle("-fx-text-fill: white; -fx-background-color: #f44336; -fx-background-radius: 5; -fx-padding: 6 16;");

        Button cancelBtn = new Button(Idf.userLanguageBundle.getString("card.cancel"));
        cancelBtn.getStyleClass().addAll("card-edit-text-button");
        cancelBtn.setOnAction(e -> onCancel.run());
        cancelBtn.setStyle("-fx-text-fill: white; -fx-background-color: #555555; -fx-background-radius: 5; -fx-padding: 6 16;");

        HBox buttonBar = new HBox(10, saveBtn, discardBtn, cancelBtn);
        buttonBar.setAlignment(Pos.CENTER_RIGHT);
        AnchorPane.setBottomAnchor(buttonBar, 20.0);
        AnchorPane.setLeftAnchor(buttonBar, 20.0);
        AnchorPane.setRightAnchor(buttonBar, 20.0);

        root.getChildren().addAll(titleLabel, messageLabel, buttonBar);

        popupService.showPopup(root, null);
    }

    public void hideConfirmDialogSync() {
        showingMainArea.setEffect(null);
        centerShowingArea.getChildren().clear();
        centerOutBox.setMouseTransparent(true);
        blackPane.setOpacity(0);
    }

    private ToggleButton buildSeverityToggle(String iconPath, double size, ToggleGroup group) {
        ImageView iconView = buildIcon(iconPath, size);
        ToggleButton btn = new ToggleButton();
        btn.setGraphic(iconView);
        btn.setToggleGroup(group);
        btn.getStyleClass().add("card-severity-toggle");
        return btn;
    }

    private ImageView buildSeverityIcon(CardItem.Severity severity, double size) {
        String iconPath = switch (severity) {
            case WARNING -> "icon/card/警告.png";
            case CRITICAL -> "icon/card/严重.png";
            default -> "icon/card/提示.png";
        };
        return buildIcon(iconPath, size);
    }

    private ImageView buildIcon(String path, double size) {
        ImageView view = new ImageView(new Image(Objects.requireNonNull(
            Entry.class.getResourceAsStream(path))));
        view.getStyleClass().add("card-icon");
        view.setFitWidth(size);
        view.setFitHeight(size);
        return view;
    }

    public Button buildIconButton(String iconPath, double size, String... styleClasses) {
        ImageView iconView = buildIcon(iconPath, size);
        Button btn = new Button();
        btn.setGraphic(iconView);
        btn.getStyleClass().addAll(styleClasses);
        return btn;
    }

    public static String severityStyleClass(CardItem.Severity severity) {
        return switch (severity) {
            case WARNING -> "warning";
            case CRITICAL -> "critical";
            default -> "info";
        };
    }
}
