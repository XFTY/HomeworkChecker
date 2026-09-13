package com.xfty.homeworkchecker;

import com.xfty.homeworkchecker.controller.MainPage;
import com.xfty.homeworkchecker.controller.setupWizard.SetupWizardController;
import com.xfty.homeworkchecker.service.FileInitManager;
import com.xfty.homeworkchecker.service.SingletonInstanceManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xfty.homeworkchecker.service.HomeworkDatabase;
import javafx.animation.FadeTransition;
import javafx.scene.Parent;
import javafx.scene.control.TextArea;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.concurrent.ScheduledExecutorService;


/**
 * 应用程序启动入口。
 *
 * <p>继承 JavaFX 的 {@link Application}，由 {@link #main(String[])} 通过
 * {@code launch()} 触发 JavaFX 运行时后回调 {@link #start(Stage)}。</p>
 *
 * <p>启动流程分为两条分支：</p>
 * <ul>
 *   <li><b>首次运行</b>（用户目录下不存在 {@code homeworkChecker} 文件夹）：
 *       展示 5 步初始化向导，收集语言/字体/模板等信息后再进入主界面；</li>
 *   <li><b>正常启动</b>：初始化用户目录与语言包，完成单例校验后直接进入主界面。</li>
 * </ul>
 *
 * <p>此外，本类还负责：单例锁与激活信号文件的处理（防止多开并唤起已有窗口）、
 * 系统日期缓存到 {@link Idf}，以及运行时（切换语言等）重建主场景
 * {@link #rebuildScene()}。</p>
 */
public class Entry extends Application {
    /** 日志记录器 */
    private static final Logger logger = LoggerFactory.getLogger(Entry.class);
    /** 预留的调度线程池字段，当前未在本类中使用 */
    private ScheduledExecutorService scheduler;
    /** 主界面控制器引用，用于窗口关闭时执行资源清理 */
    private MainPage mainPageController;
    
    /**
     * JavaFX 应用启动回调，负责根据是否首次运行决定进入向导还是主界面。
     *
     * @param stage JavaFX 提供的主舞台（窗口）
     * @throws IOException 加载 FXML 失败时抛出（实际由内部方法捕获记录）
     */
    @Override
    public void start(Stage stage) throws IOException {
        // 缓存当前系统日期到全局状态池 Idf（年/月/日/星期）
        initDate();
        // 将主舞台登记到全局，便于其他模块（如场景重建）访问
        Idf.primaryStage = stage;

        FileInitManager fileInitManager = new FileInitManager();
        // 通过用户目录下是否存在 homeworkChecker 文件夹判断是否首次运行
        boolean firstRun = fileInitManager.isFirstRun();

        if (firstRun) {
            // 首次运行：进入初始化向导，向导完成后再启动主界面
            showSetupWizard(stage, fileInitManager);
        } else {
            // 正常启动：确保用户目录/配置文件完整
            fileInitManager.initializeUserDirectories();
            // 加载用户语言包到 Idf.userLanguageBundle
            loadUserLanguageBundle();
            // 单例校验：若已有实例在运行，则写激活信号文件后退出当前进程
            if (!initSingletonCheck()) {
                logger.warn("Another instance is already running. Exiting silently...");
                System.exit(0);
                return;
            }
            // 加载并展示主界面
            showMainPage(stage);
        }
    }

    /**
     * 展示首次运行初始化向导（5 步）。
     *
     * <p>加载 {@code setupWizard.fxml} 并注册完成回调：用户点击最后一步的“完成”后，
     * 先落盘首次运行配置（{@link FileInitManager#initializeFirstRun}），
     * 再加载语言包、执行单例校验，最终切换到主界面。</p>
     *
     * @param stage           主舞台
     * @param fileInitManager 负责首次运行配置落盘的初始化管理器
     */
    private void showSetupWizard(Stage stage, FileInitManager fileInitManager) {
        try {
            FXMLLoader loader = new FXMLLoader(Entry.class.getResource("fxml/setupWizard.fxml"));
            Scene wizardScene = new Scene(loader.load(), 900, 650);
            SetupWizardController controller = loader.getController();

            // 向导完成回调：参数依次为语言代码、字体族、编辑区字号、初始模板内容
            controller.setOnWizardFinished((languageCode, fontFamily, fontSize, initTemplate) -> {
                // 依据向导收集的信息创建目录并写入 config.json / initTemple.txt / language.json
                fileInitManager.initializeFirstRun(languageCode, fontFamily, fontSize, initTemplate);
                // 读取刚写入的语言配置，刷新全局语言包
                loadUserLanguageBundle();
                // 与正常启动一致，仍需做单例校验
                if (!initSingletonCheck()) {
                    logger.warn("Another instance is already running. Exiting silently...");
                    System.exit(0);
                    return;
                }
                // 进入主界面
                showMainPage(stage);
            });

            stage.setTitle("HomeworkChecker " + Idf.softwareVersion + " - 初始化");
            stage.setScene(wizardScene);
            stage.getIcons().add(new Image(Objects.requireNonNull(Entry.class.getResourceAsStream("icon/logo.png"))));
            stage.show();
        } catch (IOException e) {
            logger.error("Failed to load setup wizard", e);
        }
    }

    /**
     * 加载并展示主界面 {@code mainPage.fxml}，同时完成后续的运行时接线。
     *
     * <p>具体工作：</p>
     * <ol>
     *   <li>根据 {@link Idf#userLanguage} 构建 {@link Locale} 并绑定到 FXMLLoader，
     *       使 FXML 中的国际化资源正确解析；</li>
     *   <li>创建场景、设置标题与图标并显示窗口；</li>
     *   <li>启动单例管理器的 WatchService，监听其他实例发出的激活信号；</li>
     *   <li>保存控制器引用到全局，并注册窗口关闭时的资源清理逻辑。</li>
     * </ol>
     *
     * @param stage 主舞台
     */
    private void showMainPage(Stage stage) {
        try {
            // 读取用户语言代码，如 "zh_CN"；为空则使用 FXML 默认资源
            String languageCode = Idf.userLanguage != null ? Idf.userLanguage.getString("language") : null;

            FXMLLoader fxmlLoader = new FXMLLoader(Entry.class.getResource("fxml/mainPage.fxml"));
            if (languageCode != null) {
                // 将 "语言_地区" 形式的代码拆分为 Locale（长度为 2 表示带地区）
                String[] languageParts = languageCode.split("_");
                Locale locale;
                if (languageParts.length == 2) {
                    locale = new Locale.Builder().setLanguage(languageParts[0]).setRegion(languageParts[1]).build();
                } else {
                    locale = new Locale.Builder().setLanguage(languageParts[0]).build();
                }
                fxmlLoader.setResources(ResourceBundle.getBundle("com/xfty/homeworkchecker/i18n/language", locale));
            }
            Scene scene = new Scene(fxmlLoader.load(), 1000, 600);

            stage.setTitle(Idf.userLanguageBundle != null ? Idf.userLanguageBundle.getString("entry.window.title") : "HomeworkChecker");
            stage.setScene(scene);
            stage.getIcons().add(new Image(Objects.requireNonNull(Entry.class.getResourceAsStream("icon/logo.png"))));
            stage.show();

            // 启动激活信号监听：其他实例启动时会使本窗口短暂置顶
            if (Idf.singletonManager != null) {
                Idf.singletonManager.startWatchService(stage);
            }

            // 保存控制器引用（本类字段用于关闭回调，Idf 静态字段供其他模块访问）
            mainPageController = fxmlLoader.getController();
            Idf.mainPageController = mainPageController;

            // 窗口关闭请求：标记关闭状态、清理主界面资源，并短暂等待以确保收尾完成
            stage.setOnCloseRequest(windowEvent -> {
                logger.info("Application closing requested");
                Idf.isSoftwareClosing = true;
                if (mainPageController != null) {
                    mainPageController.cleanup();
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                logger.info("Application closed");
            });
        } catch (IOException e) {
            logger.error("Failed to load main page", e);
        }
    }

    /**
     * 依据 {@link Idf#userLanguage} 中的语言代码刷新全局语言包 {@link Idf#userLanguageBundle}。
     * 若尚未读取到语言配置则不做任何处理。
     */
    private void loadUserLanguageBundle() {
        if (Idf.userLanguage != null && Idf.userLanguage.getString("language") != null) {
            Idf.reloadLanguageBundle(Idf.userLanguage.getString("language"));
        }
    }

    /**
     * 将当前系统日期缓存到全局状态池 {@link Idf}。
     *
     * <p>月份与日期补零为两位字符串，星期使用 ISO 编号（周一=1 ... 周日=7），
     * 这些值被作业数据库的按天存储逻辑使用。</p>
     */
    private void initDate() {
        LocalDate today = LocalDate.now();
        Idf.year = String.valueOf(today.getYear());
        Idf.month = String.format("%02d", today.getMonthValue());
        Idf.day = String.format("%02d", today.getDayOfMonth());
        Idf.weekdays = String.valueOf(today.getDayOfWeek().getValue());
        logger.info("{} {} {} {}", Idf.year, Idf.month, Idf.day, Idf.weekdays);
    }

    /**
     * 执行单例校验。
     *
     * <p>尝试获取进程独占的文件锁：成功则把管理器保存到 {@link Idf#singletonManager}
     * 以便后续启动 WatchService；失败说明已有实例在运行，此时写入激活信号文件
     * 请求已有窗口前置。</p>
     *
     * @return {@code true} 表示成功获取锁（可继续启动）；{@code false} 表示已有实例
     */
    private boolean initSingletonCheck() {
        SingletonInstanceManager singletonManager = new SingletonInstanceManager();
        boolean acquired = singletonManager.acquireLock();
        if (acquired) {
            Idf.singletonManager = singletonManager;
        } else {
            createRepeatedStartFile();
        }
        return acquired;
    }

    /**
     * 创建激活信号文件 {@code repeatedly.start}。
     *
     * <p>文件内容为当前时间戳。已有实例的 WatchService 监听到该文件创建后，
     * 会把其窗口置顶并请求焦点；随后已有实例会自行删除该文件。</p>
     */
    private void createRepeatedStartFile() {
        try {
            File userDir = FileUtils.getUserDirectory();
            File homeworkCheckerDir = new File(userDir, "homeworkChecker");
            // 确保目录存在（极端情况下可能尚未创建）
            if (!homeworkCheckerDir.exists()) {
                FileUtils.forceMkdir(homeworkCheckerDir);
            }
            File signalFile = new File(homeworkCheckerDir, "repeatedly.start");
            Files.writeString(Paths.get(signalFile.getAbsolutePath()),
                String.valueOf(System.currentTimeMillis()));
            logger.info("Created activation signal file: {}", signalFile.getAbsolutePath());
            // 稍作等待，给已有实例的 WatchService 留出响应时间后再退出
            Thread.sleep(200);
        } catch (IOException | InterruptedException e) {
            logger.error("Error creating activation signal file", e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 重建主场景（切换语言等场景下调用），带淡出/淡入过渡动画。
     *
     * <p>先重置预览窗口标记，若旧场景存在则对其根节点执行 200ms 淡出，
     * 动画结束后再调用 {@link #buildNewScene(Stage)}；无旧场景时直接重建。</p>
     */
    public static void rebuildScene() {
        Idf.isPreviewWindowShowing = false;

        Stage stage = Idf.primaryStage;
        if (stage == null) {
            logger.error("Primary stage is null, cannot rebuild scene");
            return;
        }

        Scene oldScene = stage.getScene();
        if (oldScene != null) {
            // 旧场景淡出，动画结束回调中构建新场景，避免切换突兀
            Parent oldRoot = oldScene.getRoot();
            FadeTransition fadeOut = new FadeTransition(Duration.millis(200), oldRoot);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setOnFinished(e -> buildNewScene(stage));
            fadeOut.play();
        } else {
            buildNewScene(stage);
        }
    }

    /**
     * 实际构建并替换为新主场景（静态，供淡出动画回调调用）。
     *
     * <p>步骤：保存旧编辑区内容 → 清理旧控制器 → 按当前语言包重新加载
     * {@code mainPage.fxml} → 替换场景并淡入 → 重启 WatchService、更新控制器引用、
     * 重新注册窗口关闭回调。</p>
     *
     * @param stage 主舞台
     */
    private static void buildNewScene(Stage stage) {
        try {
            // Save current homework content before rebuilding
            // 重建前先持久化旧编辑区内容，避免切换场景丢失未保存的作业
            Scene oldScene = stage.getScene();
            if (oldScene != null) {
                TextArea editMain = (TextArea) oldScene.lookup("#editMain");
                if (editMain != null) {
                    HomeworkDatabase db = new HomeworkDatabase();
                    db.writeHomeworkContextByDay(editMain.getText());
                }
            }

            // Cleanup old controller
            // 清理旧控制器占用的资源（编辑状态看门狗、卡片编辑等）
            if (Idf.mainPageController != null) {
                Idf.mainPageController.cleanup();
                Idf.mainPageController = null;
            }

            // Build new scene with current language bundle
            // 使用当前语言包重新加载主界面 FXML
            String languageCode = Idf.userLanguage != null ? Idf.userLanguage.getString("language") : null;

            FXMLLoader fxmlLoader = new FXMLLoader(Entry.class.getResource("fxml/mainPage.fxml"));
            if (languageCode != null) {
                String[] languageParts = languageCode.split("_");
                Locale locale;
                if (languageParts.length == 2) {
                    locale = new Locale.Builder().setLanguage(languageParts[0]).setRegion(languageParts[1]).build();
                } else {
                    locale = new Locale.Builder().setLanguage(languageParts[0]).build();
                }
                fxmlLoader.setResources(ResourceBundle.getBundle("com/xfty/homeworkchecker/i18n/language", locale));
            }
            Parent newRoot = fxmlLoader.load();
            // 初始透明，随后淡入，实现平滑过渡
            newRoot.setOpacity(0.0);
            Scene newScene = new Scene(newRoot, 1000, 600);

            stage.setTitle(Idf.userLanguageBundle != null ? Idf.userLanguageBundle.getString("entry.window.title") : "HomeworkChecker");
            stage.setScene(newScene);
            stage.getIcons().add(new Image(Objects.requireNonNull(Entry.class.getResourceAsStream("icon/logo.png"))));
            stage.show();

            // Fade in new scene
            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), newRoot);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.play();

            // 新场景同样需要重新挂载激活信号监听
            if (Idf.singletonManager != null) {
                Idf.singletonManager.startWatchService(stage);
            }

            // 更新全局控制器引用
            MainPage newController = fxmlLoader.getController();
            Idf.mainPageController = newController;

            // 新场景重新注册关闭回调（旧回调随旧场景一起失效）
            stage.setOnCloseRequest(windowEvent -> {
                logger.info("Application closing requested");
                Idf.isSoftwareClosing = true;
                if (newController != null) {
                    newController.cleanup();
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                logger.info("Application closed");
            });
        } catch (IOException e) {
            logger.error("Failed to rebuild scene", e);
        }
    }

    /**
     * 程序主入口：强制 UTF-8 编码后交由 JavaFX 启动运行时。
     *
     * @param args 命令行参数（透传给 JavaFX）
     */
    public static void main(String[] args) {
        // 固定文件编码，保证中文等非 ASCII 内容读写正确
        System.setProperty("file.encoding", "UTF-8");
        launch();
    }
}