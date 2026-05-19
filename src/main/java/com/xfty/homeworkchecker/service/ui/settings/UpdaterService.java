package com.xfty.homeworkchecker.service.ui.settings;

import com.xfty.homeworkchecker.Idf;
import com.xfty.homeworkchecker.service.updater.HttpClientService;
import javafx.application.Platform;
import okhttp3.*;
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

/**
 * UpdaterService — 更新业务服务
 * <p>
 * 负责完整的更新流程：GitHub API 版本检查、OkHttp 下载 .msi 安装包、
 * 下载进度/剩余时间计算、msiexec 安装并退出应用。
 * </p>
 */
public class UpdaterService {
    
    private static final Logger logger = LoggerFactory.getLogger(UpdaterService.class);
    
    private ResourceBundle resourceBundle;
    private String downloadUrl;
    private OkHttpClient downloadClient;
    
    /**
     * 构造函数
     */
    public UpdaterService() {
        // 获取资源束
        if (Idf.userLanguageBundle != null) {
            this.resourceBundle = Idf.userLanguageBundle;
        }
        
        // 初始化下载客户端（信任所有证书）
        this.downloadClient = createUnsafeOkHttpClient();
        
        logger.info("UpdaterService initialized");
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
     * 检查更新的业务逻辑
     * @param uiCallback UI回调接口
     */
    public void checkForUpdates(UpdateCheckCallback uiCallback) {
        logger.info("Starting update check...");
        
        // 通知UI进入检查状态
        uiCallback.onChecking();
        
        // 在新线程中执行网络请求，避免阻塞UI
        Thread updateThread = new Thread(() -> {
            HttpClientService httpClient = new HttpClientService();
            boolean success = httpClient.fetchAndParseLatestRelease();
            
            Platform.runLater(() -> {
                if (success) {
                    handleUpdateCheckSuccess(httpClient, uiCallback);
                } else {
                    handleUpdateCheckFailure(uiCallback);
                }
            });
        });
        
        updateThread.setDaemon(true);
        updateThread.start();
    }
    
    /**
     * 处理更新检查成功的情况
     * @param httpClient HTTP客户端服务实例
     * @param uiCallback UI回调接口
     */
    private void handleUpdateCheckSuccess(HttpClientService httpClient, UpdateCheckCallback uiCallback) {
        String latestVersion = httpClient.getReleaseTitle();
        String currentVersion = Idf.softwareVersion;
        
        logger.info("Current version: {}, Latest version: {}", currentVersion, latestVersion);
        
        if (latestVersion == null || latestVersion.isEmpty()) {
            logger.error("Failed to parse release title");
            uiCallback.onError(getString("updater.error.parse"));
            return;
        }
        
        // 比较版本号
        if (currentVersion.equals(latestVersion)) {
            // 已是最新版本
            logger.info("Software is up to date");
            uiCallback.onUpToDate(currentVersion);
        } else {
            // 有新版本可用
            logger.info("New version available: {}", latestVersion);
            
            // 保存下载链接（查找包含"win"的.msi文件）
            List<String> urls = httpClient.getDownloadUrls();
            this.downloadUrl = findWindowsMsiUrl(urls);
            
            if (this.downloadUrl == null) {
                logger.warn("No Windows .msi download URL found");
            } else {
                logger.info("Found download URL: {}", this.downloadUrl);
            }
            
            // 在WebView中显示更新日志
            String releaseBody = httpClient.getReleaseBody();
            String htmlContent = generateUpdateInfoHtml(latestVersion, releaseBody);
            
            uiCallback.onUpdateAvailable(latestVersion, htmlContent);
        }
    }
    
    /**
     * 处理更新检查失败的情况
     * @param uiCallback UI回调接口
     */
    private void handleUpdateCheckFailure(UpdateCheckCallback uiCallback) {
        logger.error("Failed to check for updates");
        uiCallback.onError(getString("updater.error.network"));
    }
    
    /**
     * 安装更新的业务逻辑
     * @param installCallback 安装回调接口
     */
    public void installUpdate(InstallCallback installCallback) {
        logger.info("Install update requested");
        
        // 检测操作系统
        String osName = System.getProperty("os.name").toLowerCase();
        logger.info("Detected OS: {}", osName);
        
        // Windows系统
        if (osName.contains("win")) {
            if (downloadUrl == null || downloadUrl.isEmpty()) {
                installCallback.onError("没有可用的下载链接");
                return;
            }
            
            // 检查文件是否已经下载过
            File installerFile = getDownloadFilePath();
            if (installerFile.exists()) {
                logger.info("Installer file already exists: {}", installerFile.getAbsolutePath());
                installCallback.onFileExists(installerFile);
                return;
            }
            
            // 开始下载
            startDownload(downloadUrl, installCallback);
        } else {
            // 非Windows系统（MacOS、Linux等）
            installCallback.onUnsupportedPlatform();
        }
    }
    
    /**
     * 开始下载更新文件
     * @param url 下载链接
     * @param installCallback 安装回调接口
     */
    private void startDownload(String url, InstallCallback installCallback) {
        logger.info("Starting download from: {}", url);
        
        // 通知UI下载开始
        installCallback.onDownloadStarted();
        
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
                    installCallback.onError("下载失败: " + e.getMessage());
                });
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    logger.error("Download failed with status code: {}", response.code());
                    Platform.runLater(() -> {
                        installCallback.onError("下载失败，状态码: " + response.code());
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
                                    
                                    Platform.runLater(() -> {
                                        installCallback.onDownloadProgress(progress, remainingTimeText);
                                    });
                                } else {
                                    Platform.runLater(() -> {
                                        installCallback.onDownloadProgress(progress, null);
                                    });
                                }
                                
                                // 更新上一次的状态
                                lastUpdateTime = currentTime;
                                lastDownloadedBytes = downloadedBytes;
                            } else {
                                Platform.runLater(() -> {
                                    installCallback.onDownloadProgress(progress, null);
                                });
                            }
                        }
                    }
                    
                    logger.info("Download completed successfully");
                    Platform.runLater(() -> {
                        installCallback.onDownloadCompleted(getDownloadFilePath());
                    });
                    
                } catch (IOException e) {
                    logger.error("Error saving file: {}", e.getMessage());
                    Platform.runLater(() -> {
                        installCallback.onError("保存文件时出错: " + e.getMessage());
                    });
                }
            }
        });
    }
    
    /**
     * 运行安装程序并退出应用
     * @param installerFile 安装程序文件
     */
    public void runInstallerAndExit(File installerFile) {
        if (!installerFile.exists()) {
            logger.error("Installer file not found: {}", installerFile.getAbsolutePath());
            return;
        }
        
        try {
            logger.info("Starting installer: {}", installerFile.getAbsolutePath());
            
            // 使用Runtime.exec()启动msiexec来安装.msi文件
            String installerPath = installerFile.getAbsolutePath();
            ProcessBuilder processBuilder = new ProcessBuilder("msiexec", "/i", installerPath);
            processBuilder.start();
            
            logger.info("Installer launched, shutting down application...");
            
            // 启动后台线程执行完全退出和清理
            Thread shutdownThread = new Thread(() -> {
                try {
                    Thread.sleep(2000);
                    
                    logger.info("Cleaning up resources before exit...");
                    
                    // 关闭下载客户端，释放连接池
                    if (downloadClient != null) {
                        downloadClient.dispatcher().executorService().shutdown();
                        downloadClient.connectionPool().evictAll();
                        if (downloadClient.cache() != null) {
                            downloadClient.cache().close();
                        }
                    }
                    
                    logger.info("Exiting application now");
                    
                    // 强制退出JVM，确保所有资源都被释放
                    System.exit(0);
                } catch (Exception e) {
                    logger.error("Error during shutdown: {}", e.getMessage());
                    // 即使出错也强制退出
                    System.exit(0);
                }
            });
            shutdownThread.setDaemon(true);
            shutdownThread.start();
            
        } catch (IOException e) {
            logger.error("Failed to launch installer: {}", e.getMessage());
        }
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
     * 查找包含"win"的.msi下载链接（不区分大小写）
     * @param urls 下载链接列表
     * @return 匹配的URL，如果没有找到则返回null
     */
    private String findWindowsMsiUrl(List<String> urls) {
        if (urls == null || urls.isEmpty()) {
            return null;
        }
        
        for (String url : urls) {
            String lowerUrl = url.toLowerCase();
            if (lowerUrl.contains("win") && lowerUrl.endsWith(".msi")) {
                return url;
            }
        }
        
        return null;
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
    
    /**
     * 更新检查回调接口
     */
    public interface UpdateCheckCallback {
        /**
         * 正在检查更新
         */
        void onChecking();
        
        /**
         * 已是最新版本
         * @param version 当前版本号
         */
        void onUpToDate(String version);
        
        /**
         * 有更新可用
         * @param version 新版本号
         * @param updateInfoHtml 更新信息HTML
         */
        void onUpdateAvailable(String version, String updateInfoHtml);
        
        /**
         * 检查更新出错
         * @param errorMessage 错误消息
         */
        void onError(String errorMessage);
    }
    
    /**
     * 安装回调接口
     */
    public interface InstallCallback {
        /**
         * 下载开始
         */
        void onDownloadStarted();
        
        /**
         * 下载进度更新
         * @param progress 进度（0.0-1.0）
         * @param remainingTimeText 剩余时间文本（可能为null）
         */
        void onDownloadProgress(double progress, String remainingTimeText);
        
        /**
         * 下载完成
         * @param installerFile 安装文件
         */
        void onDownloadCompleted(File installerFile);
        
        /**
         * 文件已存在
         * @param installerFile 安装文件
         */
        void onFileExists(File installerFile);
        
        /**
         * 不支持的平台
         */
        void onUnsupportedPlatform();
        
        /**
         * 出错
         * @param errorMessage 错误消息
         */
        void onError(String errorMessage);
    }
}

