package com.xfty.homeworkchecker.controller.settings;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.TimeUnit;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import org.controlsfx.control.ToggleSwitch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.Background;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.util.Duration;



/**
 * CloudSync — 设置 → 云同步服务 页面控制器
 * <p>
 * 负责云同步总开关与服务器连接性测试：
 * <ul>
 *   <li>总开关关闭时禁用下方所有全局设置，开启后放开；</li>
 *   <li>「测试连接」按钮读取用户填写的服务器地址，经规范化后用 OkHttp 发起探测请求；</li>
 *   <li>连接成功时复用主界面「一键截屏」的成功动画（绿色闪烁）作为反馈。</li>
 * </ul>
 * </p>
 */
public class CloudSync implements Initializable{

    private static final Logger logger = LoggerFactory.getLogger(CloudSync.class);

    /** 连接测试进行中按钮显示的文案 */
    private static final String TESTING_TEXT = "测试中...";
    /** 连接成功后按钮短暂显示的文案 */
    private static final String SUCCESS_TEXT = "连接成功";

    /** 连接测试用的 HTTP 客户端，连接/读取超时均为 5 秒，避免长时间卡住后台线程 */
    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build();

    /** 连接测试状态：为 true 时忽略重复点击 */
    private boolean testing;
    /** 成功动画状态：为 true 时忽略重复点击，避免动画叠加 */
    private boolean animating;

    /** 云同步总开关 */
    @FXML 
    private ToggleSwitch cloudSyncToggleSwitch;

    /** 开关打开后才可用的全局设置容器 */
    @FXML 
    private VBox cGlobalSettings;

    /** 服务器 IP 地址输入框 */
    @FXML
    private TextField serverIpField;

    /** 「测试连接」按钮 */
    @FXML
    private Button testConnectButton;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 全局设置默认挡位：未开启云同步前不可操作
        cGlobalSettings.setDisable(true);

        // 启用toffleSwitch监听服务
        addCloudSyncToggleSwitchListener();


    }

    /**
     * 为总开关绑定监听：开启时放开全局设置，关闭时禁用全局设置并记录日志。
     */
    private void addCloudSyncToggleSwitchListener() {
        cloudSyncToggleSwitch.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                // 开关打开时的动作
               logger.info("CloudSyncToggleSwitch switch to open.");
               cGlobalSettings.setDisable(false);
            } else {
                // 开关关闭时的动作
                logger.info("CloudSyncToggleSwitch switch to close.");
                cGlobalSettings.setDisable(true);
            }
        });
    }
    
    /**
     * 「测试连接」按钮点击处理。
     * <p>
     * 流程：规范化用户输入 → 校验失败则弹窗提示 → 置为测试中并禁用按钮 →
     * 在后台守护线程用 OkHttp 探测 → 回到 UI 线程恢复按钮，成功播放动画 / 失败弹窗。
     * </p>
     */
    @FXML 
    protected void onTestIpConnectionButtonPressed() {
        // 正在测试或正在播放动画时忽略点击，防止并发测试与动画叠加
        if (testing || animating) {
            return;
        }

        // 规范化并校验用户输入的服务器地址
        String url = normalizeServerUrl(serverIpField.getText());
        if (url == null) {
            logger.warn("Invalid server address input: {}", serverIpField.getText());
            showConnectionAlert("地址无效",
                "请输入有效的服务器地址，例如 192.168.1.1、192.168.1.1:8080 或 http://192.168.1.1");
            return;
        }

        // 记录原始按钮文案，测试结束后恢复
        String originalText = testConnectButton.getText();
        testing = true;
        testConnectButton.setDisable(true);
        testConnectButton.setText(TESTING_TEXT);
        logger.info("Testing server connection to: {}", url);

        // 网络请求放到后台线程，避免阻塞 JavaFX UI 线程
        Thread testThread = new Thread(() -> {
            boolean reachable = false;
            int statusCode = -1;
            try {
                Request request = new Request.Builder().url(url).get().build();
                try (Response response = httpClient.newCall(request).execute()) {
                    // 能收到任意 HTTP 响应即视为可连通（自建服务根路径可能返回 404 等）
                    statusCode = response.code();
                    reachable = true;
                }
            } catch (IOException | IllegalArgumentException e) {
                // 连接超时、DNS 失败、URL 非法等都会走到这里，统一视为不可连通
                logger.warn("Server connection test failed for {}: {}", url, e.getMessage());
            }

            // 线程内结果需为 final/等效 final 才能在 runLater 中捕获
            final boolean result = reachable;
            final int code = statusCode;
            Platform.runLater(() -> {
                // 无论成败，先恢复按钮可点击状态与原文案
                testing = false;
                testConnectButton.setDisable(false);
                testConnectButton.setText(originalText);

                if (result) {
                    logger.info("Server connection test succeeded: {} (HTTP {})", url, code);
                    // 复用主界面「一键截屏」的成功动画
                    animateButtonSuccess();
                } else {
                    logger.warn("Server connection test failed: {}", url);
                    showConnectionAlert("连接失败",
                        "无法连接到服务器 " + url + "，请检查IP地址、端口以及网络环境后重试");
                }
            });
        }, "cloud-sync-connect-test");
        // 设为守护线程，应用退出时不阻止 JVM 结束
        testThread.setDaemon(true);
        testThread.start();
    }

    /**
     * 规范化用户输入的服务器地址：去除空白、补全默认 http:// 协议头，
     * 并使用 OkHttp 的 HttpUrl 校验主机与端口合法性。
     *
     * @param rawInput 用户原始输入
     * @return 合法的 URL 字符串，非法时返回 null
     */
    private String normalizeServerUrl(String rawInput) {
        if (rawInput == null) {
            return null;
        }
        // 去除所有空白字符，兼容用户输入时前后的空格、IP 与端口间的空格等
        String input = rawInput.replaceAll("\\s+", "");
        if (input.isEmpty()) {
            return null;
        }
        // 未携带协议头（如 192.168.1.1、192.168.1.1:8080）时补全默认 http://
        if (!input.matches("(?i)^[a-z][a-z0-9+.\\-]*://.*")) {
            input = "http://" + input;
        }
        try {
            // 交给 OkHttp 解析，自动校验主机、端口合法性并规范化（如补全末尾斜杠）
            HttpUrl parsed = HttpUrl.parse(input);
            if (parsed == null || parsed.host().isEmpty()) {
                return null;
            }
            return parsed.toString();
        } catch (IllegalArgumentException e) {
            // 端口越界等非法输入
            return null;
        }
    }

    /**
     * 弹出统一的连接结果提示框。
     *
     * @param header  提示标题
     * @param content 提示正文
     */
    private void showConnectionAlert(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("云同步服务");
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /**
     * 播放按钮成功动画，效果与主界面「一键截屏」成功后一致：
     * 先淡入浅绿，再在浅绿/暗绿之间闪烁 3 个循环，最后淡出恢复原始样式与文案。
     */
    private void animateButtonSuccess() {
        if (animating) {
            return;
        }
        animating = true;

        // 备份原始文案与内联样式，动画结束后还原
        String originalText = testConnectButton.getText();
        String originalStyle = testConnectButton.getStyle();

        Color lightGreen = Color.web("#66bb6a");
        Color darkGreen = Color.web("#1b5e20");

        testConnectButton.setText(SUCCESS_TEXT);

        // 用颜色属性驱动样式刷新，KeyFrame 只需改变该属性值
        ObjectProperty<Color> bgColorProp = new SimpleObjectProperty<>(getButtonBackgroundColor());
        bgColorProp.addListener((obs, oldColor, newColor) -> {
            String webColor = String.format("#%02x%02x%02x",
                (int) (newColor.getRed() * 255),
                (int) (newColor.getGreen() * 255),
                (int) (newColor.getBlue() * 255));
            testConnectButton.setStyle("-fx-background-color: " + webColor + "; -fx-text-fill: white;");
        });

        double fadeIn = 300;      // 淡入浅绿耗时
        double blinkTotal = 6000; // 闪烁总时长（3 个 2 秒循环）
        double fadeOut = 300;     // 淡出恢复耗时

        Timeline timeline = new Timeline();

        // 阶段一：从当前底色过渡到浅绿
        timeline.getKeyFrames().setAll(
            new KeyFrame(Duration.ZERO,
                new KeyValue(bgColorProp, getButtonBackgroundColor(), Interpolator.LINEAR)),
            new KeyFrame(Duration.millis(fadeIn),
                new KeyValue(bgColorProp, lightGreen, Interpolator.LINEAR))
        );

        // 阶段二：浅绿与暗绿交替闪烁 3 次
        for (int i = 0; i < 3; i++) {
            double cycleStart = fadeIn + i * 2000;
            timeline.getKeyFrames().addAll(
                new KeyFrame(Duration.millis(cycleStart + 1000),
                    new KeyValue(bgColorProp, darkGreen, Interpolator.LINEAR)),
                new KeyFrame(Duration.millis(cycleStart + 2000),
                    new KeyValue(bgColorProp, lightGreen, Interpolator.LINEAR))
            );
        }

        // 阶段三：淡出回原始底色，并在结束时还原文案与样式
        timeline.getKeyFrames().addAll(
            new KeyFrame(Duration.millis(fadeIn + blinkTotal),
                new KeyValue(bgColorProp, lightGreen, Interpolator.LINEAR)),
            new KeyFrame(Duration.millis(fadeIn + blinkTotal + fadeOut), e -> {
                animating = false;
                testConnectButton.setText(originalText);
                testConnectButton.setStyle(originalStyle);
            },
                new KeyValue(bgColorProp, getButtonBackgroundColor(), Interpolator.LINEAR))
        );

        timeline.play();
    }

    /**
     * 读取按钮当前背景色，取不到时回退到默认深灰 #2d2d2d。
     *
     * @return 按钮背景色
     */
    private Color getButtonBackgroundColor() {
        Background bg = testConnectButton.getBackground();
        if (bg != null && !bg.getFills().isEmpty()) {
            Paint fill = bg.getFills().get(0).getFill();
            if (fill instanceof Color) {
                return (Color) fill;
            }
        }
        return Color.web("#2d2d2d");
    }
}
