package com.xfty.homeworkchecker.controller.settings;

import com.xfty.homeworkchecker.Idf;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;

/**
 * ResetThings — 重置页面控制器
 * <p>
 * 提供重置今日作业的功能：确认后标记 needHomeworkShowingAreaClear，
 * 在主界面下一次刷新时生效。
 * </p>
 */
public class ResetThings {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ResetThings.class);

    @FXML
    private Button clearAllHomeworkContentButton;

    /**
     * 重置今日作业：弹出确认对话框 → 标记清理标志 → 禁用按钮
     */
    @FXML
    private void onResetHomeworkClicked() {
        // 处理按钮点击事件
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(Idf.userLanguageBundle.getString("mainpage.confirmReset.title"));
        alert.setHeaderText(Idf.userLanguageBundle.getString("mainpage.confirmReset.header"));
        alert.setContentText(Idf.userLanguageBundle.getString("mainpage.confirmReset.content"));

        alert.showAndWait().ifPresent(response -> {
            if (response == javafx.scene.control.ButtonType.OK) {
                Idf.needHomeworkShowingAreaClear = true;

                clearAllHomeworkContentButton.setText("将在关闭此窗口时重置...");
                clearAllHomeworkContentButton.setDisable(true);


                logger.info("Today's homework content has been cleared");
            }
        });
    }
}
