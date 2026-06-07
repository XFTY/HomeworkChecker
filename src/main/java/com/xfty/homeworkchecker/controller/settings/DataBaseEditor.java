package com.xfty.homeworkchecker.controller.settings;

import com.alibaba.fastjson2.JSONArray;
import com.xfty.homeworkchecker.Entry;
import com.xfty.homeworkchecker.Idf;
import com.xfty.homeworkchecker.controller.HistoryHomeworkChecker;
import com.xfty.homeworkchecker.service.ui.settings.DataBaseEditorService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * DataBaseEditor — 数据库编辑器控制器
 * <p>
 * 管理数据库信息面板（大小/路径）、自动化设置（保留天数）和历史作业集列表
 * （查看/删除）的交互逻辑。使用 MVC 架构，业务逻辑委托给
 * {@link DataBaseEditorService}。
 * </p>
 */
public class DataBaseEditor implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(DataBaseEditor.class);

    @FXML
    private Label databaseSizeLabel;
    @FXML
    private Label databaseSizeUnitLabel;
    @FXML
    private Label databaseUsedLabel;
    @FXML
    private Label databasePathLabel;
    @FXML
    private CheckBox autoCleanupCheckBox;
    @FXML
    private TextField retentionDaysField;
    @FXML
    private Button openDirectoryButton;
    @FXML
    private ScrollPane historyScrollPane;
    @FXML
    private VBox historyListVBox;

    private final DataBaseEditorService service = new DataBaseEditorService();
    private ResourceBundle bundle;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        bundle = resourceBundle != null ? resourceBundle : Idf.userLanguageBundle;
        if (bundle == null) {
            bundle = ResourceBundle.getBundle("com/xfty/homeworkchecker/i18n/language", Locale.getDefault());
        }

        initAutoCleanupCheckBox();
        initRetentionDaysField();
        updateDatabaseInfo();
        buildHistoryList();
    }

    private void initAutoCleanupCheckBox() {
        boolean enabled = service.isAutoCleanupEnabled();
        autoCleanupCheckBox.setSelected(enabled);
        retentionDaysField.setDisable(!enabled);

        autoCleanupCheckBox.setOnAction(e -> {
            boolean selected = autoCleanupCheckBox.isSelected();
            service.saveAutoCleanupEnabled(selected);
            retentionDaysField.setDisable(!selected);
        });
    }

    private void initRetentionDaysField() {
        int days = service.getRetentionDays();
        retentionDaysField.setText(String.valueOf(days));

        retentionDaysField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                saveRetentionDays();
            }
        });

        retentionDaysField.setOnAction(e -> saveRetentionDays());
    }

    private void saveRetentionDays() {
        try {
            String text = retentionDaysField.getText().trim();
            if (text.isEmpty()) {
                retentionDaysField.setText(String.valueOf(service.getRetentionDays()));
                return;
            }
            int days = Integer.parseInt(text);
            if (days <= 0) {
                showAlert(Alert.AlertType.WARNING, getString("settings.databaseeditor.invalidDays"),
                        getString("settings.databaseeditor.invalidDays"));
                retentionDaysField.setText(String.valueOf(service.getRetentionDays()));
                return;
            }
            service.saveRetentionDays(days);
        } catch (NumberFormatException e) {
            retentionDaysField.setText(String.valueOf(service.getRetentionDays()));
        }
    }

    @FXML
    private void onOpenDirectory() {
        try {
            String path = service.getDatabasePath();
            File dir = new File(path);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            Desktop.getDesktop().open(dir);
            logger.info("Opened database directory: {}", path);
        } catch (IOException e) {
            logger.error("Failed to open database directory", e);
        }
    }

    private void updateDatabaseInfo() {
        long totalBytes = service.calculateDatabaseSize();
        String sizeText;
        String unitText;

        if (totalBytes < 1024) {
            sizeText = String.valueOf(totalBytes);
            unitText = "/B";
        } else if (totalBytes < 1024 * 1024) {
            double kb = totalBytes / 1024.0;
            sizeText = formatWithSignificantDigits(kb);
            unitText = "/KB";
        } else if (totalBytes < 1024L * 1024 * 1024) {
            double mb = totalBytes / (1024.0 * 1024.0);
            sizeText = formatWithSignificantDigits(mb);
            unitText = "/MB";
        } else {
            double gb = totalBytes / (1024.0 * 1024.0 * 1024.0);
            sizeText = formatWithSignificantDigits(gb);
            unitText = "/GB";
        }

        databaseSizeLabel.setText(sizeText);
        databaseSizeUnitLabel.setText(unitText);
        databasePathLabel.setText(service.getDatabasePath());
    }

    private String formatWithSignificantDigits(double value) {
        if (value >= 1000) {
            return String.format("%.0f", value);
        } else if (value >= 100) {
            return String.format("%.1f", value);
        } else if (value >= 10) {
            return String.format("%.2f", value);
        } else {
            return String.format("%.3f", value);
        }
    }

    private void buildHistoryList() {
        historyListVBox.getChildren().clear();

        List<String> files = service.scanHomeworkFiles();
        if (files.isEmpty()) {
            Label emptyLabel = new Label(getString("settings.databaseeditor.noHistory"));
            emptyLabel.setTextFill(javafx.scene.paint.Color.web("#7a7a7a"));
            emptyLabel.setFont(new Font(13));
            emptyLabel.setAlignment(Pos.CENTER);
            emptyLabel.setMaxWidth(Double.MAX_VALUE);
            emptyLabel.setStyle("-fx-padding: 20 0 20 0;");
            historyListVBox.getChildren().add(emptyLabel);
            return;
        }

        for (String fileName : files) {
            historyListVBox.getChildren().add(createHistoryItem(fileName));
        }
    }

    private HBox createHistoryItem(String fileName) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-background-color: #2a2a2a; -fx-background-radius: 10; -fx-padding: 8 12 8 12;");
        row.setPrefHeight(44);

        String formattedDate = formatDateString(fileName);
        Label dateLabel = new Label(formattedDate);
        dateLabel.setTextFill(javafx.scene.paint.Color.WHITE);
        dateLabel.setFont(new Font(14));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button viewBtn = new Button(getString("settings.databaseeditor.view"));
        viewBtn.getStyleClass().add("functional-button");
        viewBtn.getStylesheets().add(Entry.class.getResource("theme/darkness/button/functional-button.css").toExternalForm());
        viewBtn.setFont(new Font(12));
        viewBtn.setOnAction(e -> onViewHomework(fileName));

        Button deleteBtn = new Button(getString("settings.databaseeditor.delete"));
        deleteBtn.getStyleClass().add("danger-button");
        deleteBtn.getStylesheets().add(Entry.class.getResource("theme/darkness/button/danger-button.css").toExternalForm());
        deleteBtn.setFont(new Font(12));
        deleteBtn.setOnAction(e -> onDeleteHomework(fileName));

        row.getChildren().addAll(dateLabel, spacer, viewBtn, deleteBtn);
        return row;
    }

    private void onViewHomework(String fileName) {
        DataBaseEditorService.HomeworkData data = service.getHomeworkData(fileName);
        if (data == null || data.context == null) {
            showAlert(Alert.AlertType.INFORMATION,
                    getString("loadhistory.info"),
                    getString("loadhistory.notFound"));
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/xfty/homeworkchecker/fxml/historyHomeworkChecker.fxml"));
            if (bundle != null) {
                loader.setResources(bundle);
            }
            Parent root = loader.load();
            HistoryHomeworkChecker controller = loader.getController();
            controller.setHomeworkData(data.context, data.warnings);

            Stage stage = new Stage();
            stage.setTitle(getString("loadhistory.window.title"));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));

            Label showDate = (Label) root.lookup("#showDate");
            if (showDate != null) {
                showDate.setText(fileName + " | " + getString("loadhistory.window.titleD"));
            }

            if (Idf.isMainPageMaximized) {
                stage.setMaximized(true);
            }

            stage.setOnCloseRequest(e -> stage.close());
            stage.showAndWait();
        } catch (IOException e) {
            logger.error("Failed to load history homework checker", e);
            showAlert(Alert.AlertType.ERROR,
                    getString("loadhistory.error"),
                    getString("loadhistory.failedToLoad") + e.getMessage());
        }
    }

    private void onDeleteHomework(String fileName) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle(getString("settings.databaseeditor.deleteConfirm.title"));
        confirmAlert.setHeaderText(getString("settings.databaseeditor.deleteConfirm.header"));
        confirmAlert.setContentText(getString("settings.databaseeditor.deleteConfirm.content"));

        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                DataBaseEditorService.DeleteResult result = service.deleteHomeworkSet(fileName);

                if (!result.missingImages.isEmpty()) {
                    Platform.runLater(() -> {
                        String imageList = String.join("\n", result.missingImages);
                        String content = getString("settings.databaseeditor.imageNotFound.content")
                                .replace("{0}", imageList);

                        Alert missingAlert = new Alert(Alert.AlertType.WARNING);
                        missingAlert.setTitle(getString("settings.databaseeditor.imageNotFound.title"));
                        missingAlert.setHeaderText(getString("settings.databaseeditor.imageNotFound.header"));
                        missingAlert.setContentText(content);
                        missingAlert.showAndWait();
                    });
                }

                updateDatabaseInfo();
                buildHistoryList();
                logger.info("Deleted homework set: {}", fileName);
            }
        });
    }

    private String formatDateString(String yyyyMMdd) {
        try {
            LocalDate date = LocalDate.parse(yyyyMMdd, DateTimeFormatter.ofPattern("yyyyMMdd"));
            return date.getYear() + getYearUnit()
                    + String.format("%02d", date.getMonthValue()) + getMonthUnit()
                    + String.format("%02d", date.getDayOfMonth()) + getDayUnit();
        } catch (Exception e) {
            return yyyyMMdd;
        }
    }

    private String getYearUnit() {
        return getStringSafe("mainpage.year", "\u5E74");
    }

    private String getMonthUnit() {
        return getStringSafe("mainpage.month", "\u6708");
    }

    private String getDayUnit() {
        return getStringSafe("mainpage.day", "\u65E5");
    }

    private String getStringSafe(String key, String fallback) {
        try {
            return bundle != null && bundle.containsKey(key) ? bundle.getString(key) : fallback;
        } catch (Exception e) {
            return fallback;
        }
    }

    private String getString(String key) {
        try {
            if (bundle != null && bundle.containsKey(key)) {
                return bundle.getString(key);
            }
        } catch (Exception e) {
            logger.warn("Resource key not found: {}", key);
        }
        return key;
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
