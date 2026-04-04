package com.xfty.homeworkchecker.service.ui.loadHistoryHomework;

import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import java.util.Objects;

/**
 * 按钮状态管理器 - 负责管理按钮和 ImageView 的状态（启用/禁用，绿灯/红灯）
 */
public class ButtonStateManager {
    
    private static final Image GREEN_IMAGE;
    private static final Image RED_IMAGE;
    
    static {
        // Load images using Entry class classloader to ensure correct resource path
        GREEN_IMAGE = new Image(Objects.requireNonNull(
            com.xfty.homeworkchecker.Entry.class.getResourceAsStream("/com/xfty/homeworkchecker/icon/light/green.png")));
        RED_IMAGE = new Image(Objects.requireNonNull(
            com.xfty.homeworkchecker.Entry.class.getResourceAsStream("/com/xfty/homeworkchecker/icon/light/red.png")));
    }
    
    /**
     * 设置按钮的状态（启用/禁用）和对应的 ImageView 图标（绿灯/红灯）
     * @param button 按钮对象
     * @param imageView ImageView 对象
     * @param hasData 是否有数据
     */
    public void setButtonState(Button button, ImageView imageView, boolean hasData) {
        if (hasData) {
            enableButton(button, imageView);
        } else {
            disableButton(button, imageView);
        }
    }
    
    /**
     * 启用按钮并设置绿灯图标
     * @param button 按钮对象
     * @param imageView ImageView 对象
     */
    public void enableButton(Button button, ImageView imageView) {
        imageView.setImage(GREEN_IMAGE);
        button.setDisable(false);
    }
    
    /**
     * 禁用按钮并设置红灯图标
     * @param button 按钮对象
     * @param imageView ImageView 对象
     */
    public void disableButton(Button button, ImageView imageView) {
        imageView.setImage(RED_IMAGE);
        button.setDisable(true);
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
        for (int i = 0; i < Math.min(buttons.size(), Math.min(imageViews.size(), hasDataList.size())); i++) {
            setButtonState(buttons.get(i), imageViews.get(i), hasDataList.get(i));
        }
    }
}
