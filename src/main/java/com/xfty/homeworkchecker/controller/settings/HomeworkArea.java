package com.xfty.homeworkchecker.controller.settings;

import com.xfty.homeworkchecker.Idf;
import com.xfty.homeworkchecker.service.ui.settings.HomeworkAreaService;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * HomeworkArea — 作业区字体/字号/图片卡片设置控制器
 * <p>
 * 负责 FXML 控件绑定与交互，业务逻辑委托给 {@link HomeworkAreaService}。
 * </p>
 */
public class HomeworkArea implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(HomeworkArea.class);

    private final HomeworkAreaService service = new HomeworkAreaService();

    @FXML
    private Slider editMainTextSize;
    @FXML
    private ChoiceBox<String> fontFamily;
    @FXML
    private TextArea editMainTest;
    @FXML
    private Label scrollValueDisplay;
    @FXML
    private Slider imageSliderMin;
    @FXML
    private Slider imageSliderMax;
    @FXML
    private Label imageSliderMinValue;
    @FXML
    private Label imageSliderMaxValue;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        if (!service.isConfigVersionSupported()) {
            logger.error("config softwareVersion not match");
            return;
        }

        editMainTextSize.adjustValue(service.getEditMainTextSize());

        fontFamily.getItems().addAll(Idf.fontFamilies);
        fontFamily.getSelectionModel().select(service.getDefaultFontFamily());

        editMainTextSize.valueProperty().addListener((observable, oldValue, newValue) -> {
            service.saveEditMainTextSize(newValue.intValue());
            scrollValueDisplay.setText(Idf.userLanguageBundle.getString("settings.scroolValueDisplayTitle") + (int) editMainTextSize.getValue());
            updateEditMainTextChanges();
        });

        scrollValueDisplay.setText(Idf.userLanguageBundle.getString("settings.scroolValueDisplayTitle") + (int) editMainTextSize.getValue());

        fontFamily.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                service.saveDefaultFontFamily(newValue);
                updateEditMainTextChanges();
            }
        });

        double minVal = service.getImageSliderMin();
        double maxVal = service.getImageSliderMax();
        imageSliderMin.setValue(minVal);
        imageSliderMax.setValue(maxVal);
        imageSliderMinValue.setText(String.valueOf((int) minVal));
        imageSliderMaxValue.setText(String.valueOf((int) maxVal));

        imageSliderMin.valueProperty().addListener((obs, old, val) -> {
            double v = val.doubleValue();
            imageSliderMinValue.setText(String.valueOf((int) v));
            service.saveImageSliderMin(v);
        });

        imageSliderMax.valueProperty().addListener((obs, old, val) -> {
            double v = val.doubleValue();
            imageSliderMaxValue.setText(String.valueOf((int) v));
            service.saveImageSliderMax(v);
        });

        updateEditMainTextChanges();
    }

    private void updateEditMainTextChanges() {
        service.applyPreviewFont(editMainTest, editMainTextSize.getValue());
    }
}
