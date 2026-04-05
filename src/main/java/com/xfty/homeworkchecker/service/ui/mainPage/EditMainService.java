package com.xfty.homeworkchecker.service.ui.mainPage;

import com.xfty.homeworkchecker.Idf;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.collections.ObservableList;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
    
    // 缩进状态跟踪
    private String currentIndentation = "";
    
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
     * 设置TextArea引用并初始化按键监听
     * @param editMain 编辑文本区域
     */
    public void setEditMain(TextArea editMain) {
        if (editMain == null) {
            logger.error("Cannot set editMain: textarea is null");
            return;
        }
        
        this.editMain = editMain;
        setupKeyPressHandler();
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
                
                // 添加关键帧的辅助方法
                addShakeKeyFrames(keyFrames, currentTimeAnim, amplitude, 3); // 前 3 个完整周期
                currentTimeAnim += 300; // 3 个周期的时间 (3 * (50+25+50+25))
                
                // 第四个周期，幅度开始衰减（减小到原来的一半）
                addShakeKeyFrames(keyFrames, currentTimeAnim, amplitude * 0.5, 1);
                currentTimeAnim += 150; // 1 个周期的时间 (50+25+50+25)
                
                // 第五个周期，幅度进一步减小（减小到原来的四分之一）
                addShakeKeyFrames(keyFrames, currentTimeAnim, amplitude * 0.25, 1);
                
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
     * 辅助方法：添加指定次数的抖动关键帧
     * @param keyFrames 关键帧列表
     * @param startTime 开始时间
     * @param amplitude 振幅
     * @param cycles 周期数
     */
    private void addShakeKeyFrames(ObservableList<KeyFrame> keyFrames, 
                                   double startTime, double amplitude, int cycles) {
        logger.trace("Adding {} shake keyframes starting at {} ms with amplitude {}", cycles, startTime, amplitude);
        
        double currentTime = startTime;
        for (int cycle = 0; cycle < cycles; cycle++) {
            // 向左移动
            keyFrames.add(new KeyFrame(
                Duration.millis(currentTime),
                new KeyValue(lockModelShowingArea.translateXProperty(), -amplitude)
            ));
            currentTime += 50;
            
            // 回到中心
            keyFrames.add(new KeyFrame(
                Duration.millis(currentTime),
                new KeyValue(lockModelShowingArea.translateXProperty(), 0)
            ));
            currentTime += 25;
            
            // 向右移动
            keyFrames.add(new KeyFrame(
                Duration.millis(currentTime),
                new KeyValue(lockModelShowingArea.translateXProperty(), amplitude)
            ));
            currentTime += 50;
            
            // 回到中心
            keyFrames.add(new KeyFrame(
                Duration.millis(currentTime),
                new KeyValue(lockModelShowingArea.translateXProperty(), 0)
            ));
            currentTime += 25;
        }
        
        logger.trace("Completed adding {} shake keyframes, ending at {} ms", cycles, currentTime);
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
    
    /**
     * 设置按键事件处理器，监听回车键以自动填充缩进
     */
    private void setupKeyPressHandler() {
        if (editMain == null) {
            logger.warn("Cannot setup key press handler: editMain is null");
            return;
        }
        
        editMain.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER) {
                handleEnterKey(event);
            }
        });
        
        logger.debug("Key press handler registered for editMain");
    }
    
    /**
     * 处理回车键事件，自动填充当前行的缩进
     * @param event 按键事件
     */
    private void handleEnterKey(KeyEvent event) {
        if (editMain == null) {
            return;
        }
        
        try {
            int caretPosition = editMain.getCaretPosition();
            String text = editMain.getText();
            
            // 找到当前行的起始位置
            int lineStart = text.lastIndexOf('\n', caretPosition - 1);
            lineStart = (lineStart == -1) ? 0 : lineStart + 1;
            
            // 找到当前行的结束位置（光标位置或行尾）
            int lineEnd = text.indexOf('\n', caretPosition);
            lineEnd = (lineEnd == -1) ? text.length() : lineEnd;
            
            // 提取从行首到光标位置的所有空白字符（包括中间的）
            StringBuilder indent = new StringBuilder();
            boolean foundNonWhitespace = false;
            
            for (int i = lineStart; i < caretPosition; i++) {
                char c = text.charAt(i);
                if (c == ' ' || c == '\t') {
                    // 如果已经遇到过非空白字符，后续的空白也计入缩进
                    indent.append(c);
                } else {
                    foundNonWhitespace = true;
                }
            }
            
            // 如果光标后面还有内容，检查是否应该继续缩进
            // 只有当光标在行尾或者后面都是空白时才应用缩进
            if (caretPosition < lineEnd) {
                String afterCursor = text.substring(caretPosition, lineEnd).trim();
                if (!afterCursor.isEmpty()) {
                    // 光标后面有非空白内容，不自动缩进
                    logger.trace("Cursor has non-whitespace content after it, skipping auto-indent");
                    return;
                }
            }
            
            currentIndentation = indent.toString();
            logger.trace("Detected indentation: '{}', foundNonWhitespace: {}", currentIndentation, foundNonWhitespace);
            
            // 延迟插入缩进，确保在默认换行行为之后执行
            javafx.application.Platform.runLater(() -> {
                if (!currentIndentation.isEmpty()) {
                    int newCaretPosition = editMain.getCaretPosition();
                    editMain.insertText(newCaretPosition, currentIndentation);
                    editMain.positionCaret(newCaretPosition + currentIndentation.length());
                    logger.debug("Auto-indented with: '{}'", currentIndentation);
                }
            });
            
        } catch (Exception e) {
            logger.error("Error handling enter key for auto-indentation", e);
        }
    }
}
