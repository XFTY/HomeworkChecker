package com.xfty.homeworkchecker.controller.settings;

import com.xfty.homeworkchecker.Idf;
import com.xfty.homeworkchecker.service.updater.HttpClientService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.web.WebView;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.TimeUnit;

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
    private String downloadUrl;
    private OkHttpClient downloadClient;
    
    /**
     * 初始化方法，设置按钮事件监听器
     */
    @FXML
    public void initialize() {
        // 获取资源束
        if (Idf.userLanguageBundle != null) {
            this.resourceBundle = Idf.userLanguageBundle;
        }
        
        // 设置检查更新按钮点击事件
        updaterCheckUpdate.setOnAction(event -> checkForUpdates());
        
        // 设置安装更新按钮点击事件
        updaterInstallNow.setOnAction(event -> installUpdate());
        
        // 初始状态：禁用安装按钮
        updaterInstallNow.setDisable(true);
        
        // 初始化下载客户端（信任所有证书）
        this.downloadClient = createUnsafeOkHttpClient();
        
        logger.info("Updater controller initialized");
    }
    
    /**
     * 创建信任所有证书的OkHttpClient（用于开发/测试环境）
     * @return OkHttpClient实例
     */
    private OkHttpClient createUnsafeOkHttpClient() {
        try {
            // 创建信任所有证书的TrustManager
            final TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType) {
                    }
                    
                    @Override
                    public void checkServerTrusted(X509Certificate[] chain, String authType) {
                    }
                    
                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[]{};
                    }
                }
            };
            
            // 安装信任所有证书的TrustManager
            final SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new SecureRandom());
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
            
            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            builder.sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0]);
            builder.hostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });
            builder.connectTimeout(30, TimeUnit.SECONDS);
            builder.readTimeout(30, TimeUnit.SECONDS);
            
            logger.info("Created OkHttpClient with SSL trust-all configuration");
            return builder.build();
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            logger.error("Failed to create unsafe OkHttpClient: {}", e.getMessage());
            // 返回默认客户端
            return new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .build();
        }
    }
    
    /**
     * 检查更新的主方法
     */
    private void checkForUpdates() {
        logger.info("Starting update check...");
        
        // 更新UI状态为检查中
        updateUIStateChecking();
        
        // 在新线程中执行网络请求，避免阻塞UI
        Thread updateThread = new Thread(() -> {
            HttpClientService httpClient = new HttpClientService();
            boolean success = httpClient.fetchAndParseLatestRelease();
            
            Platform.runLater(() -> {
                if (success) {
                    handleUpdateCheckSuccess(httpClient);
                } else {
                    handleUpdateCheckFailure();
                }
            });
        });
        
        updateThread.setDaemon(true);
        updateThread.start();
    }
    
    /**
     * 处理更新检查成功的情况
     * @param httpClient HTTP客户端服务实例
     */
    private void handleUpdateCheckSuccess(HttpClientService httpClient) {
        String latestVersion = httpClient.getReleaseTitle();
        String currentVersion = Idf.softwareVersion;
        
        logger.info("Current version: {}, Latest version: {}", currentVersion, latestVersion);
        
        if (latestVersion == null || latestVersion.isEmpty()) {
            logger.error("Failed to parse release title");
            showErrorMessage(getString("updater.error.parse"));
            resetToIdleState();
            return;
        }
        
        // 比较版本号
        if (currentVersion.equals(latestVersion)) {
            // 已是最新版本
            logger.info("Software is up to date");
            updaterTitle.setText(getString("updater.upToDate.title"));
            updaterSubTitle.setText(String.format(getString("updater.upToDate.content"), currentVersion));
            updaterProcess.setProgress(1.0);
            updaterInstallNow.setDisable(true);
            
            // 隐藏WebView
            updaterWebView.setOpacity(0.0);
        } else {
            // 有新版本可用
            logger.info("New version available: {}", latestVersion);
            updaterTitle.setText(getString("updater.newVersionAvailable.title"));
            updaterSubTitle.setText(String.format(getString("updater.newVersion.content"), latestVersion));
            updaterProcess.setProgress(1.0);
            updaterInstallNow.setDisable(false); // 启用安装按钮
            
            // 保存下载链接（查找包含"win"的.zip文件）
            List<String> urls = httpClient.getDownloadUrls();
            this.downloadUrl = findWindowsZipUrl(urls);
            
            if (this.downloadUrl == null) {
                logger.warn("No Windows .zip download URL found");
            } else {
                logger.info("Found download URL: {}", this.downloadUrl);
            }
            
            // 在WebView中显示更新日志
            String releaseBody = httpClient.getReleaseBody();
            String htmlContent = generateUpdateInfoHtml(latestVersion, releaseBody);
            updaterWebView.getEngine().loadContent(htmlContent);
            
            // 显示WebView
            updaterWebView.setOpacity(1.0);
        }
    }
    
    /**
     * 处理更新检查失败的情况
     */
    private void handleUpdateCheckFailure() {
        logger.error("Failed to check for updates");
        showErrorMessage(getString("updater.error.network"));
        resetToIdleState();
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
        
        // 检测操作系统
        String osName = System.getProperty("os.name").toLowerCase();
        logger.info("Detected OS: {}", osName);
        
        // Windows系统
        if (osName.contains("win")) {
            if (downloadUrl == null || downloadUrl.isEmpty()) {
                showErrorMessage("没有可用的下载链接");
                return;
            }
            
            // 开始下载
            startDownload(downloadUrl);
        } else {
            // 非Windows系统（MacOS、Linux等）
            showUnsupportedPlatformAlert();
        }
    }
    
    /**
     * 开始下载更新文件
     * @param url 下载链接
     */
    private void startDownload(String url) {
        logger.info("Starting download from: {}", url);
        
        // 禁用按钮和关闭按钮
        updaterInstallNow.setDisable(true);
        updaterCheckUpdate.setDisable(true);
        disableSettingsCloseButton(true);
        
        // 更新UI状态
        updaterTitle.setText("正在下载更新...");
        updaterSubTitle.setText("计算剩余时间...");
        updaterProcess.setProgress(0.0);
        
        // 创建下载请求
        Request request = new Request.Builder().url(url).build();
        
        // 记录下载开始时间
        long downloadStartTime = System.currentTimeMillis();
        
        // 异步下载
        downloadClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                logger.error("Download failed: {}", e.getMessage());
                Platform.runLater(() -> {
                    showErrorMessage("下载失败: " + e.getMessage());
                    resetDownloadState();
                });
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    logger.error("Download failed with status code: {}", response.code());
                    Platform.runLater(() -> {
                        showErrorMessage("下载失败，状态码: " + response.code());
                        resetDownloadState();
                    });
                    return;
                }
                
                try (InputStream inputStream = response.body().byteStream();
                     FileOutputStream outputStream = new FileOutputStream(getDownloadFilePath())) {
                    
                    byte[] buffer = new byte[8192];
                    long totalBytes = response.body().contentLength();
                    long downloadedBytes = 0;
                    int bytesRead;
                    
                    // 用于计算下载速度的变量
                    long lastUpdateTime = System.currentTimeMillis();
                    long lastDownloadedBytes = 0;
                    
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                        downloadedBytes += bytesRead;
                        
                        // 更新进度条和剩余时间
                        if (totalBytes > 0) {
                            double progress = (double) downloadedBytes / totalBytes;
                            
                            // 每500ms更新一次剩余时间显示
                            long currentTime = System.currentTimeMillis();
                            if (currentTime - lastUpdateTime >= 500) {
                                long remainingBytes = totalBytes - downloadedBytes;
                                long bytesDownloadedInInterval = downloadedBytes - lastDownloadedBytes;
                                long timeElapsedInInterval = currentTime - lastUpdateTime;
                                
                                if (timeElapsedInInterval > 0 && bytesDownloadedInInterval > 0) {
                                    // 计算下载速度（字节/毫秒）
                                    double downloadSpeed = (double) bytesDownloadedInInterval / timeElapsedInInterval;
                                    
                                    // 计算剩余时间（毫秒）
                                    long remainingTimeMs = (long) (remainingBytes / downloadSpeed);
                                    long remainingTimeSeconds = remainingTimeMs / 1000;
                                    
                                    // 转换为分钟
                                    long remainingMinutes = remainingTimeSeconds / 60;
                                    long remainingSeconds = remainingTimeSeconds % 60;
                                    
                                    String remainingTimeText;
                                    if (remainingTimeSeconds < 60) {
                                        remainingTimeText = "剩余时间<1分钟";
                                    } else if (remainingMinutes < 60) {
                                        remainingTimeText = String.format("剩余时间约%d分%d秒", remainingMinutes, remainingSeconds);
                                    } else {
                                        long hours = remainingMinutes / 60;
                                        long mins = remainingMinutes % 60;
                                        remainingTimeText = String.format("剩余时间约%d小时%d分", hours, mins);
                                    }
                                    
                                    final String finalRemainingTimeText = remainingTimeText;
                                    Platform.runLater(() -> {
                                        updaterProcess.setProgress(progress);
                                        updaterSubTitle.setText(finalRemainingTimeText);
                                    });
                                } else {
                                    Platform.runLater(() -> updaterProcess.setProgress(progress));
                                }
                                
                                // 更新上一次的状态
                                lastUpdateTime = currentTime;
                                lastDownloadedBytes = downloadedBytes;
                            } else {
                                Platform.runLater(() -> updaterProcess.setProgress(progress));
                            }
                        }
                    }
                    
                    logger.info("Download completed successfully");
                    Platform.runLater(() -> {
                        updaterTitle.setText("下载完成！");
                        updaterSubTitle.setText("文件已保存至: " + getDownloadFilePath().getAbsolutePath());
                        updaterProcess.setProgress(1.0);
                        
                        // 启用关闭按钮
                        disableSettingsCloseButton(false);
                        updaterCheckUpdate.setDisable(false);
                        
                        // TODO: 自动安装功能可以在这里添加
                    });
                    
                } catch (IOException e) {
                    logger.error("Error saving file: {}", e.getMessage());
                    Platform.runLater(() -> {
                        showErrorMessage("保存文件时出错: " + e.getMessage());
                        resetDownloadState();
                    });
                }
            }
        });
    }
    
    /**
     * 获取下载文件保存路径
     * @return 文件对象
     */
    private File getDownloadFilePath() {
        // 提取文件名
        String fileName = downloadUrl.substring(downloadUrl.lastIndexOf('/') + 1);
        
        // 保存到用户下载目录
        String userHome = System.getProperty("user.home");
        File downloadsDir = new File(userHome, "Downloads");
        
        if (!downloadsDir.exists()) {
            downloadsDir.mkdirs();
        }
        
        return new File(downloadsDir, fileName);
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
     * 查找包含"win"的.zip下载链接（不区分大小写）
     * @param urls 下载链接列表
     * @return 匹配的URL，如果没有找到则返回null
     */
    private String findWindowsZipUrl(List<String> urls) {
        if (urls == null || urls.isEmpty()) {
            return null;
        }
        
        for (String url : urls) {
            String lowerUrl = url.toLowerCase();
            if (lowerUrl.contains("win") && lowerUrl.endsWith(".zip")) {
                return url;
            }
        }
        
        return null;
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
     * 生成已是最新版本的HTML内容
     * @param version 当前版本号
     * @return HTML字符串
     */
    private String generateUpToDateHtml(String version) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<style>" +
                "body { font-family: 'Microsoft YaHei', Arial, sans-serif; padding: 20px; color: #ffffff; background-color: #1e1e1e; }" +
                ".container { text-align: center; margin-top: 50px; }" +
                ".icon { font-size: 64px; color: #4CAF50; margin-bottom: 20px; }" +
                ".title { font-size: 24px; margin-bottom: 10px; }" +
                ".version { font-size: 18px; color: #9c9c9c; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class='container'>" +
                "<div class='icon'>✓</div>" +
                "<div class='title'>" + getString("updater.upToDate.title") + "</div>" +
                "<div class='version'>" + version + "</div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }
    
    /**
     * 生成更新信息的HTML内容（支持Markdown渲染）
     * @param version 新版本号
     * @param markdownContent Markdown格式的更新日志
     * @return HTML字符串
     */
    private String generateUpdateInfoHtml(String version, String markdownContent) {
        // 使用CommonMark将Markdown转换为HTML
        Parser parser = Parser.builder().build();
        HtmlRenderer renderer = HtmlRenderer.builder().build();
        
        String htmlBody = "";
        if (markdownContent != null && !markdownContent.isEmpty()) {
            htmlBody = renderer.render(parser.parse(markdownContent));
        } else {
            htmlBody = "<p>No release notes available.</p>";
        }
        
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<style>" +
                "body { font-family: 'Microsoft YaHei', Arial, sans-serif; padding: 20px; color: #ffffff; background-color: #1e1e1e; }" +
                ".header { border-bottom: 2px solid #0080ff; padding-bottom: 10px; margin-bottom: 20px; }" +
                ".version { font-size: 28px; font-weight: bold; color: #0080ff; }" +
                ".label { font-size: 14px; color: #9c9c9c; margin-top: 5px; }" +
                "h1, h2, h3 { color: #0080ff; margin-top: 20px; }" +
                "p { line-height: 1.6; }" +
                "ul, ol { padding-left: 20px; }" +
                "li { margin: 5px 0; }" +
                "code { background-color: #2d2d2d; padding: 2px 6px; border-radius: 3px; }" +
                "pre { background-color: #2d2d2d; padding: 10px; border-radius: 5px; overflow-x: auto; }" +
                "a { color: #0080ff; text-decoration: none; }" +
                "a:hover { text-decoration: underline; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class='header'>" +
                "<div class='version'>" + version + "</div>" +
                "<div class='label'>Latest Version</div>" +
                "</div>" +
                htmlBody +
                "</body>" +
                "</html>";
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
