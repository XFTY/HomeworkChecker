package com.xfty.homeworkchecker.controller.settings;

import com.xfty.homeworkchecker.Idf;
import com.xfty.homeworkchecker.service.ui.settings.UpdaterService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.web.WebView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.text.MessageFormat;
import java.util.ResourceBundle;

/**
 * Updater — 更新页面控制器
 * <p>
 * 管理检查更新按钮和安装按钮的交互，通过 WebView 显示更新日志 HTML，
 * 通过 ProgressBar 显示下载进度。安装过程调用 UpdaterService 完成。
 * </p>
 */
public class Updater {
    
    private static final Logger logger = LoggerFactory.getLogger(Updater.class);
    
    @FXML
    private Label updaterTitle;

    @FXML
    private Label updaterSubTitle;

    @FXML
    private ProgressBar updaterProcess;

    @FXML
    private WebView updaterWebView;

    @FXML
    private Button updaterInstallNow;

    @FXML
    private Button updaterCheckUpdate;
    
    private ResourceBundle resourceBundle;
    private UpdaterService updaterService;
    
    /**
     * 初始化方法，设置按钮事件监听器
     */
    @FXML
    public void initialize() {
        // 获取资源束
        if (Idf.userLanguageBundle != null) {
            this.resourceBundle = Idf.userLanguageBundle;
        }
        
        // 创建更新服务实例
        this.updaterService = new UpdaterService();
        
        // 设置检查更新按钮点击事件
        updaterCheckUpdate.setOnAction(event -> checkForUpdates());
        
        // 设置安装更新按钮点击事件
        updaterInstallNow.setOnAction(event -> installUpdate());
        
        // 初始状态：禁用安装按钮
        updaterInstallNow.setDisable(true);
        
        logger.info("Updater controller initialized");
    }
    
    /**
     * 检查更新的主方法
     */
    private void checkForUpdates() {
        updaterService.checkForUpdates(new UpdaterService.UpdateCheckCallback() {
            @Override
            public void onChecking() {
                updateUIStateChecking();
            }
            
            @Override
            public void onUpToDate(String version) {
                updaterTitle.setText(getString("updater.upToDate.title"));
                updaterSubTitle.setText(MessageFormat.format(getString("updater.upToDate.content"), version));
                updaterProcess.setProgress(1.0);
                updaterInstallNow.setDisable(true);
                
                // 隐藏WebView
                updaterWebView.setOpacity(0.0);
            }
            
            @Override
            public void onUpdateAvailable(String version, String updateInfoHtml) {
                updaterTitle.setText(version);
                updaterSubTitle.setText("下方为详情信息");
                updaterProcess.setProgress(1.0);
                updaterInstallNow.setDisable(false); // 启用安装按钮
                
                // 在WebView中显示更新日志
                updaterWebView.getEngine().loadContent(updateInfoHtml);
                
                // 显示WebView
                updaterWebView.setOpacity(1.0);
            }
            
            @Override
            public void onError(String errorMessage) {
                showErrorMessage(errorMessage);
                resetToIdleState();
            }
        });
    }
    
    /**
     * 更新UI状态为检查中
     */
    private void updateUIStateChecking() {
        updaterTitle.setText(getString("updater.checking"));
        updaterSubTitle.setText(getString("updater.connecting"));
        updaterProcess.setProgress(-1); // 不确定进度
        updaterInstallNow.setDisable(true);
        updaterCheckUpdate.setDisable(true);
    }
    
    /**
     * 重置为空闲状态
     */
    private void resetToIdleState() {
        updaterTitle.setText(getString("updater.checking"));
        updaterSubTitle.setText(getString("updater.connecting"));
        updaterProcess.setProgress(0.0);
        updaterCheckUpdate.setDisable(false);
    }
    
    /**
     * 安装更新
     */
    private void installUpdate() {
        logger.info("Install update button clicked");
        
        updaterService.installUpdate(new UpdaterService.InstallCallback() {
            @Override
            public void onDownloadStarted() {
                // 禁用按钮和关闭按钮
                updaterInstallNow.setDisable(true);
                updaterCheckUpdate.setDisable(true);
                disableSettingsCloseButton(true);
                
                // 更新UI状态
                updaterTitle.setText("正在下载更新...");
                updaterSubTitle.setText("计算剩余时间...");
                updaterProcess.setProgress(0.0);
            }
            
            @Override
            public void onDownloadProgress(double progress, String remainingTimeText) {
                updaterProcess.setProgress(progress);
                if (remainingTimeText != null) {
                    updaterSubTitle.setText(remainingTimeText);
                }
            }
            
            @Override
            public void onDownloadCompleted(File installerFile) {
                updaterTitle.setText("下载完成！");
                updaterSubTitle.setText("正在启动安装程序...");
                updaterProcess.setProgress(1.0);
                
                // 启用关闭按钮
                disableSettingsCloseButton(false);
                updaterCheckUpdate.setDisable(false);
                
                // 运行下载的.msi文件并退出程序
                updaterService.runInstallerAndExit(installerFile);
            }
            
            @Override
            public void onFileExists(File installerFile) {
                updaterTitle.setText("文件已存在");
                updaterSubTitle.setText("正在启动安装程序...");
                updaterService.runInstallerAndExit(installerFile);
            }
            
            @Override
            public void onUnsupportedPlatform() {
                showUnsupportedPlatformAlert();
            }
            
            @Override
            public void onError(String errorMessage) {
                showErrorMessage(errorMessage);
                resetDownloadState();
            }
        });
    }
    
    /**
     * 禁用/启用设置窗口关闭按钮
     * @param disable true为禁用，false为启用
     */
    private void disableSettingsCloseButton(boolean disable) {
        if (Idf.settingsWindowCloseButton != null) {
            Platform.runLater(() -> {
                Idf.settingsWindowCloseButton.setMouseTransparent(disable);
                Idf.settingsWindowCloseButton.setOpacity(disable ? 0.3 : 1.0);
                logger.info("Settings close button {}", disable ? "disabled" : "enabled");
            });
        }
    }
    
    /**
     * 重置下载状态
     */
    private void resetDownloadState() {
        updaterInstallNow.setDisable(false);
        updaterCheckUpdate.setDisable(false);
        disableSettingsCloseButton(false);
        updaterProcess.setProgress(0.0);
    }
    
    /**
     * 显示不支持平台的提示
     */
    private void showUnsupportedPlatformAlert() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Unsupported Platform");
        alert.setHeaderText("Automatic update not supported on this platform");
        alert.setContentText("Automatic update is only supported on Windows.\n\n" +
                "Please visit the following link to download the update manually:\n" +
                "https://github.com/XFTY/Homeworkchecker/releases");
        alert.showAndWait();
    }
    
    /**
     * 显示错误消息
     * @param message 错误消息内容
     */
    private void showErrorMessage(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    /**
     * 从ResourceBundle获取字符串
     * @param key 键名
     * @return 对应的字符串值
     */
    private String getString(String key) {
        if (resourceBundle != null && resourceBundle.containsKey(key)) {
            return resourceBundle.getString(key);
        }
        // 如果找不到资源束或键，返回key本身
        logger.warn("Resource bundle key not found: {}", key);
        return key;
    }
}
