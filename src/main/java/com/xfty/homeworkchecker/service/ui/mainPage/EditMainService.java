package com.xfty.homeworkchecker.service.ui.mainPage;

import com.xfty.homeworkchecker.Idf;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.ObservableList;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * 编辑主页业务逻辑服务类
 * 负责处理与编辑区域相关的业务逻辑，包括可爱模式、抖动动画等
 */
public class EditMainService {
    
    private static final Logger logger = LoggerFactory.getLogger(EditMainService.class);
    
    // 常量定义
    private static final int CLICK_THRESHOLD = 3;
    private static final long TIME_WINDOW = 6000; // 6 seconds
    private static final String ORIGINAL_LOCK_STATUS_LABEL = "已锁定，点击锁头修改";
    
    // 成员变量
    private final List<String> cuteWarnings;
    private final Random random = new Random();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    
    // 状态跟踪变量
    private int clickCount = 0;
    private long lastClickTime = 0;
    private boolean isInCuteMode = false;
    private Timeline shakeAnimation;
    
    // UI 组件引用
    private final Label lockStatusLabel;
    private final HBox lockModelShowingArea;
    private TextArea editMain;
    
//    // 缩进状态跟踪
//    private String currentIndentation = "";
    
    // 图片粘贴回调：接收 JavaFX Image，返回分配的图片编号
    private Function<Image, Integer> imagePasteHandler;

    private ContextMenuHelper contextMenuHelper;

    /**
     * 构造函数（兼容旧版本）
     * @param lockStatusLabel 锁定状态标签
     * @param lockModelShowingArea 锁定模式显示区域
     */
    public EditMainService(Label lockStatusLabel, HBox lockModelShowingArea) {
        if (lockStatusLabel == null || lockModelShowingArea == null) {
            logger.error("Cannot initialize EditMainService: lockStatusLabel or lockModelShowingArea is null");
            throw new IllegalArgumentException("Lock status label and lock model showing area cannot be null");
        }
        
        this.lockStatusLabel = lockStatusLabel;
        this.lockModelShowingArea = lockModelShowingArea;
        this.cuteWarnings = Idf.cuteWarningsIdf;
        logger.debug("EditMainService initialized with lockStatusLabel and lockModelShowingArea");
    }
    
    /**
     * 设置图片粘贴回调，当用户粘贴图片到编辑区时触发
     * @param handler 接收 Image 对象，返回分配的图片编号
     */
    public void setImagePasteHandler(Function<Image, Integer> handler) {
        this.imagePasteHandler = handler;
    }

    /**
     * 设置TextArea引用并初始化按键监听
     * @param editMain 编辑文本区域
     */
    public void setEditMain(TextArea editMain) {
        if (editMain == null) {
            logger.error("Cannot set editMain: textarea is null");
            return;
        }
        
        this.editMain = editMain;
        this.contextMenuHelper = new ContextMenuHelper(editMain, imagePasteHandler);
        contextMenuHelper.setupKeyPressHandler();
        contextMenuHelper.setupContextMenu();
        logger.debug("EditMain set and key press handler initialized");
    }
    
    /**
     * 处理编辑区域点击事件
     * 当在非编辑模式下点击时，触发可爱模式和抖动动画
     */
    public void onEditMainClicked() {
        logger.debug("onEditMainClicked called. Current editable state: {}", Idf.isEditable);
        
        // 如果动画正在播放，则忽略新的触发事件
        if (shakeAnimation != null && shakeAnimation.getStatus() == javafx.animation.Animation.Status.RUNNING) {
            logger.debug("Ignoring click - shake animation is currently running");
            return;
        }

        try {
            if (!Idf.isEditable) {
                long currentTime = System.currentTimeMillis();
                
                // 检查是否在时间窗口内
                if (currentTime - lastClickTime <= TIME_WINDOW) {
                    clickCount++;
                    logger.debug("Click detected within time window. Click count: {}", clickCount);
                } else {
                    // 超过时间窗口，重置计数
                    clickCount = 1;
                    logger.debug("Time window exceeded, resetting click count to 1");
                }
                
                lastClickTime = currentTime;
                
                // 如果达到点击阈值
                if (clickCount >= CLICK_THRESHOLD) {
                    logger.info("Click threshold reached ({} clicks), activating cute mode", clickCount);
                    
                    // 设置随机提示文本
                    if (cuteWarnings != null && !cuteWarnings.isEmpty()) {
                        int randomIndex = random.nextInt(cuteWarnings.size());
                        lockStatusLabel.setText(cuteWarnings.get(randomIndex));
                        isInCuteMode = true;
                        logger.debug("Cute mode activated with random warning message (index: {})", randomIndex);
                        
                        // 安排 6 秒后检查是否需要恢复
                        scheduler.schedule(this::checkAndRestoreLabel, TIME_WINDOW, TimeUnit.MILLISECONDS);
                        logger.debug("Scheduled label restore check in {} ms", TIME_WINDOW);
                    } else {
                        logger.warn("Cute warnings list is empty or null, cannot set warning message");
                    }
                }
                
                // 创建特定的左右抖动动画效果
                // 前三个周期幅度保持一致，第四个周期开始衰减，第五个周期停止
                double amplitude = 15.0;  // 固定振幅
                
                // 使用 Timeline 实现平滑动画
                shakeAnimation = new Timeline();
                shakeAnimation.setCycleCount(1);
                
                // 定义关键帧列表
                ObservableList<KeyFrame> keyFrames = shakeAnimation.getKeyFrames();
                
                double currentTimeAnim = 0;
                
                ShakeAnimationHelper.addShakeKeyFrames(keyFrames, currentTimeAnim, amplitude, 3, lockModelShowingArea);
                currentTimeAnim += 300;

                ShakeAnimationHelper.addShakeKeyFrames(keyFrames, currentTimeAnim, amplitude * 0.5, 1, lockModelShowingArea);
                currentTimeAnim += 150;

                ShakeAnimationHelper.addShakeKeyFrames(keyFrames, currentTimeAnim, amplitude * 0.25, 1, lockModelShowingArea);
                
                shakeAnimation.play();
                logger.debug("Shake animation started with amplitude: {}, total duration: {} ms", amplitude, currentTimeAnim);
            } else {
                logger.debug("In editable mode, requesting focus for editMain");
            }
        } catch (Exception e) {
            logger.error("Error processing edit main click event", e);
        }
    }
    
    /**
     * 检查是否需要恢复标签文本
     * 当距离上次点击超过时间窗口且处于可爱模式时，恢复原始标签文本
     */
    private void checkAndRestoreLabel() {
        long currentTime = System.currentTimeMillis();
        logger.debug("Checking if label restoration is needed. Time since last click: {} ms", currentTime - lastClickTime);
        
        // 检查距离上次点击是否已经超过时间窗口
        if (currentTime - lastClickTime > TIME_WINDOW && isInCuteMode) {
            logger.info("Restoring original lock status label after cute mode timeout");
            javafx.application.Platform.runLater(() -> {
                lockStatusLabel.setText(ORIGINAL_LOCK_STATUS_LABEL);
                isInCuteMode = false;
                clickCount = 0; // 重置点击计数
                logger.debug("Label restored to original text and cute mode deactivated");
            });
        } else {
            logger.debug("Label restoration not needed - conditions not met");
        }
    }
    
    /**
     * 清理资源
     * 关闭计划任务执行器
     */
    public void cleanup() {
        logger.info("Starting EditMainService resource cleanup...");
        
        if (scheduler != null && !scheduler.isShutdown()) {
            logger.debug("Shutting down scheduler...");
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(1, TimeUnit.SECONDS)) {
                    logger.warn("Scheduler did not terminate in time, forcing shutdown");
                    scheduler.shutdownNow();
                } else {
                    logger.debug("Scheduler shutdown completed gracefully");
                }
            } catch (InterruptedException e) {
                logger.error("Interrupted while waiting for scheduler termination", e);
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        } else {
            logger.debug("Scheduler is null or already shutdown");
        }
        
        logger.info("EditMainService resource cleanup completed");
    }
}
