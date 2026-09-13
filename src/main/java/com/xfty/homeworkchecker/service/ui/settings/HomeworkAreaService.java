package com.xfty.homeworkchecker.service.ui.settings;

import com.alibaba.fastjson2.JSONObject;
import com.xfty.homeworkchecker.Idf;
import com.xfty.homeworkchecker.service.HomeworkDatabase;
import javafx.scene.control.TextArea;
import javafx.scene.text.Font;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * HomeworkAreaService — 作业区字体/字号/图片卡片设置业务逻辑层
 * <p>
 * 封装用户配置的读取与持久化（字体、字号、图片卡片滑块范围），
 * 将业务逻辑与 UI 控制器分离（MVC 架构）。
 * </p>
 */
public class HomeworkAreaService {

    private static final Logger logger = LoggerFactory.getLogger(HomeworkAreaService.class);

    public static final String DEFAULT_FONT_FAMILY = "Microsoft YaHei UI";
    public static final double DEFAULT_IMAGE_SLIDER_MIN = 50.0;
    public static final double DEFAULT_IMAGE_SLIDER_MAX = 800.0;

    private final HomeworkDatabase homeworkDatabase = new HomeworkDatabase();

    /**
     * 判断用户配置版本是否受支持
     */
    public boolean isConfigVersionSupported() {
        return Objects.equals(Idf.userConfig.get("configVersion"), 1);
    }

    /**
     * 读取编辑区字号
     */
    public int getEditMainTextSize() {
        return Idf.userConfig.getJSONObject("font").getJSONObject("textSize").getIntValue("editMain");
    }

    /**
     * 读取默认字体，缺失时返回默认字体
     */
    public String getDefaultFontFamily() {
        try {
            return Idf.userConfig.getJSONObject("font").getJSONObject("fontFamily").getString("defaultFontFamily");
        } catch (Exception e) {
            return DEFAULT_FONT_FAMILY;
        }
    }

    /**
     * 读取图片卡片最小滑块值，缺失时返回默认值
     */
    public double getImageSliderMin() {
        JSONObject imageCardConfig = ensureImageCardConfig();
        Double value = imageCardConfig.getDouble("sliderMin");
        return value != null ? value : DEFAULT_IMAGE_SLIDER_MIN;
    }

    /**
     * 读取图片卡片最大滑块值，缺失时返回默认值
     */
    public double getImageSliderMax() {
        JSONObject imageCardConfig = ensureImageCardConfig();
        Double value = imageCardConfig.getDouble("sliderMax");
        return value != null ? value : DEFAULT_IMAGE_SLIDER_MAX;
    }

    /**
     * 持久化编辑区字号
     */
    public void saveEditMainTextSize(int size) {
        Idf.userConfig.getJSONObject("font").getJSONObject("textSize").put("editMain", size);
        homeworkDatabase.updateConfig(Idf.userConfig);
    }

    /**
     * 持久化默认字体
     */
    public void saveDefaultFontFamily(String family) {
        Idf.userConfig.getJSONObject("font").getJSONObject("fontFamily").put("defaultFontFamily", family);
        logger.debug("FontFamily change to: {}", family);
        homeworkDatabase.updateConfig(Idf.userConfig);
    }

    /**
     * 持久化图片卡片最小滑块值
     */
    public void saveImageSliderMin(double value) {
        ensureImageCardConfig().put("sliderMin", value);
        homeworkDatabase.updateConfig(Idf.userConfig);
    }

    /**
     * 持久化图片卡片最大滑块值
     */
    public void saveImageSliderMax(double value) {
        ensureImageCardConfig().put("sliderMax", value);
        homeworkDatabase.updateConfig(Idf.userConfig);
    }

    /**
     * 将当前默认字体与指定字号应用到预览文本区
     */
    public void applyPreviewFont(TextArea textArea, double size) {
        textArea.setFont(new Font(getDefaultFontFamily(), size));
    }

    /**
     * 确保 imageCard 配置存在并补齐默认值
     */
    private JSONObject ensureImageCardConfig() {
        if (!Idf.userConfig.containsKey("imageCard")) {
            Idf.userConfig.put("imageCard", new JSONObject());
        }
        JSONObject imageCardConfig = Idf.userConfig.getJSONObject("imageCard");
        if (!imageCardConfig.containsKey("sliderMin")) {
            imageCardConfig.put("sliderMin", DEFAULT_IMAGE_SLIDER_MIN);
        }
        if (!imageCardConfig.containsKey("sliderMax")) {
            imageCardConfig.put("sliderMax", DEFAULT_IMAGE_SLIDER_MAX);
        }
        return imageCardConfig;
    }
}
