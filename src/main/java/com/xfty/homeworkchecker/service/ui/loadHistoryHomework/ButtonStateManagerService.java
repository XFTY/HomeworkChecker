package com.xfty.homeworkchecker.service.ui.loadHistoryHomework;

import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Objects;

/**
 * 按钮状态管理器 - 负责管理按钮和 ImageView 的状态（启用/禁用，绿灯/红灯）
 */
public class ButtonStateManagerService {
    
    private static final Logger logger = LoggerFactory.getLogger(ButtonStateManagerService.class);
    
    private static final Image GREEN_IMAGE;
    private static final Image RED_IMAGE;
    
    static {
        Image greenImage = null;
        Image redImage = null;
        
        try {
            // Use URL instead of InputStream for better error handling
            greenImage = new Image(Objects.requireNonNull(
                com.xfty.homeworkchecker.Entry.class.getResource("/com/xfty/homeworkchecker/icon/light/green.png")).toExternalForm());
            redImage = new Image(Objects.requireNonNull(
                com.xfty.homeworkchecker.Entry.class.getResource("/com/xfty/homeworkchecker/icon/light/red.png")).toExternalForm());
            logger.debug("Button images loaded successfully");
        } catch (NullPointerException e) {
            logger.error("Failed to load button images. Check if image files exist in resources", e);
        } catch (Exception e) {
            logger.error("Unexpected error while loading button images", e);
        }
        
        GREEN_IMAGE = greenImage;
        RED_IMAGE = redImage;
    }
    
    /**
     * 设置按钮的状态（启用/禁用）和对应的 ImageView 图标（绿灯/红灯）
     * @param button 按钮对象
     * @param imageView ImageView 对象
     * @param hasData 是否有数据
     */
    public void setButtonState(Button button, ImageView imageView, boolean hasData) {
        if (button == null || imageView == null) {
            logger.warn("Cannot set button state: button or imageView is null");
            return;
        }
        
        logger.debug("Setting button state: enabled={}", hasData);
        try {
            if (hasData) {
                enableButton(button, imageView);
            } else {
                disableButton(button, imageView);
            }
        } catch (Exception e) {
            logger.error("Error setting button state", e);
        }
    }
    
    /**
     * 启用按钮并设置绿灯图标
     * @param button 按钮对象
     * @param imageView ImageView 对象
     */
    public void enableButton(Button button, ImageView imageView) {
        if (GREEN_IMAGE == null) {
            logger.error("Green image not loaded, cannot enable button");
            return;
        }
        
        logger.trace("Enabling button with green light");
        try {
            imageView.setImage(GREEN_IMAGE);
            button.setDisable(false);
        } catch (Exception e) {
            logger.error("Failed to enable button", e);
        }
    }
    
    /**
     * 禁用按钮并设置红灯图标
     * @param button 按钮对象
     * @param imageView ImageView 对象
     */
    public void disableButton(Button button, ImageView imageView) {
        if (RED_IMAGE == null) {
            logger.error("Red image not loaded, cannot disable button");
            return;
        }
        
        logger.trace("Disabling button with red light");
        try {
            imageView.setImage(RED_IMAGE);
            button.setDisable(true);
        } catch (Exception e) {
            logger.error("Failed to disable button", e);
        }
    }
    
    /**
     * 批量设置多个按钮的状态
     * @param buttons 按钮列表
     * @param imageViews ImageView 列表
     * @param hasDataList 数据存在状态列表
     */
    public void setButtonsState(java.util.List<Button> buttons, 
                                java.util.List<ImageView> imageViews, 
                                java.util.List<Boolean> hasDataList) {
        if (buttons == null || imageViews == null || hasDataList == null) {
            logger.error("Cannot set buttons state: one or more input lists are null");
            return;
        }
        
        logger.debug("Batch setting {} buttons state", buttons.size());
        try {
            int size = Math.min(buttons.size(), Math.min(imageViews.size(), hasDataList.size()));
            for (int i = 0; i < size; i++) {
                setButtonState(buttons.get(i), imageViews.get(i), hasDataList.get(i));
            }
            logger.debug("Batch button state setting completed");
        } catch (IndexOutOfBoundsException e) {
            logger.error("Index out of bounds while setting button states", e);
        } catch (Exception e) {
            logger.error("Unexpected error in batch button state setting", e);
        }
    }
}
