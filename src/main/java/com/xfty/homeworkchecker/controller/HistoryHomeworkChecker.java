package com.xfty.homeworkchecker.controller;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.xfty.homeworkchecker.Entry;
import com.xfty.homeworkchecker.Idf;
import com.xfty.homeworkchecker.model.CardItem;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.UUID;

public class HistoryHomeworkChecker implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(HistoryHomeworkChecker.class);

    @FXML
    private TextArea editMain;

    @FXML
    private VBox cardContainer;

    @FXML
    private ScrollPane cardScrollPane;

    @FXML
    private VBox cardList;

    private double cardFontSize = 14;
    private String cardFontFamily = "System";

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        if (Idf.userConfig != null) {
            String fontFamily = Idf.userConfig.getJSONObject("font")
                .getJSONObject("fontFamily")
                .getString("defaultFontFamily");
            int fontSize = Idf.userConfig.getJSONObject("font")
                .getJSONObject("textSize")
                .getInteger("editMain");
            editMain.setFont(new Font(fontFamily, fontSize));
            cardFontFamily = fontFamily;
            cardFontSize = Math.max(fontSize - 1, 11);
        }
    }

    public void setHomeworkData(String content, JSONArray warnings) {
        editMain.setText(content);
        if (warnings != null && !warnings.isEmpty()) {
            cardContainer.setVisible(true);
            cardContainer.setManaged(true);
            renderCards(warnings);
        }
    }

    private void renderCards(JSONArray warnings) {
        cardList.getChildren().clear();
        for (int i = 0; i < warnings.size(); i++) {
            JSONObject cardObj = warnings.getJSONObject(i);
            CardItem item = parseCardItem(cardObj);
            cardList.getChildren().add(renderCard(item));
        }
    }

    private CardItem parseCardItem(JSONObject cardObj) {
        CardItem item = new CardItem();
        String id = cardObj.getString("id");
        item.setId(id != null ? id : UUID.randomUUID().toString());
        String sev = cardObj.getString("severity");
        if ("WARNING".equals(sev)) {
            item.setSeverity(CardItem.Severity.WARNING);
        } else if ("CRITICAL".equals(sev)) {
            item.setSeverity(CardItem.Severity.CRITICAL);
        } else {
            item.setSeverity(CardItem.Severity.INFO);
        }
        item.setTitle(cardObj.getString("title"));
        item.setContent(cardObj.getString("content"));
        item.setImagePath(cardObj.getString("imagePath"));
        Double iw = cardObj.getDouble("imageWidth");
        item.setImageWidth(iw != null ? iw : 200.0);
        item.setTimestamp(cardObj.getString("timestamp"));
        item.setCreatedDate(cardObj.getString("createdDate"));
        item.setPersistent(cardObj.getBooleanValue("persistent"));
        return item;
    }

    private VBox renderCard(CardItem item) {
        double iconSize = cardFontSize * 1.3;

        VBox cardRoot = new VBox();
        cardRoot.getStyleClass().addAll("card-item", severityStyleClass(item.getSeverity()));
        cardRoot.setFillWidth(true);

        VBox cardBody = new VBox();
        cardBody.getStyleClass().add("card-body");

        HBox topRow = new HBox(8);
        topRow.setAlignment(javafx.geometry.Pos.TOP_LEFT);

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
        timeRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        timeRow.getStyleClass().add("card-timestamp-row");

        ImageView timeIcon = buildIcon("icon/card/时间.png", cardFontSize * 0.8);
        timeRow.getChildren().addAll(timeIcon, timeLabel);

        if (item.isPersistent()) {
            timeRow.getStyleClass().add("persistent");
            Label persistentLabel = new Label(
                Idf.userLanguageBundle.getString("card.persistent.label"));
            persistentLabel.getStyleClass().add("card-persistent-label");
            timeRow.getChildren().add(persistentLabel);
        }

        contentArea.getChildren().add(titleLabel);

        String imgPath = item.getImagePath();
        if (imgPath != null && !imgPath.isEmpty()) {
            Image img = loadImageForCard(imgPath);
            if (img != null) {
                ImageView imageView = new ImageView(img);
                imageView.setPreserveRatio(true);
                imageView.setFitWidth(item.getImageWidth());
                imageView.setPickOnBounds(true);
                imageView.getStyleClass().add("card-image");

                VBox imageBox = new VBox(imageView);
                imageBox.getStyleClass().add("card-image-container");
                imageBox.setFillWidth(true);
                contentArea.getChildren().add(imageBox);
            }
        } else {
            contentArea.getChildren().add(contentLabel);
        }

        contentArea.getChildren().add(timeRow);
        topRow.getChildren().addAll(iconView, contentArea);
        cardBody.getChildren().add(topRow);
        cardRoot.getChildren().add(cardBody);

        return cardRoot;
    }

    private Image loadImageForCard(String imagePath) {
        if (imagePath == null || imagePath.isEmpty()) return null;
        try {
            File userDir = FileUtils.getUserDirectory();
            File imageFile = new File(
                new File(new File(userDir, "homeworkChecker"), "homeworkDatabase"),
                imagePath);
            if (!imageFile.exists()) {
                logger.warn("History image file not found: {}", imageFile.getAbsolutePath());
                return null;
            }
            return new Image(imageFile.toURI().toString());
        } catch (Exception e) {
            logger.error("Error loading history image from path: {}", imagePath, e);
            return null;
        }
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

    private String severityStyleClass(CardItem.Severity severity) {
        return switch (severity) {
            case WARNING -> "warning";
            case CRITICAL -> "critical";
            default -> "info";
        };
    }
}
