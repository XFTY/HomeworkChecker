package com.xfty.homeworkchecker.service.ui.mainPage;

import com.xfty.homeworkchecker.Idf;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 窗口监听器服务类
 * 负责管理窗口状态监听和键盘快捷键处理
 */
public class WindowListener {
    
    private static final Logger logger = LoggerFactory.getLogger(WindowListener.class);
    
    // UI 组件引用
    private final Label titleLabel;
    private final TextArea editMain;
    private final Runnable onLockModuleCallback;
    
    // 窗口引用
    private Stage primaryStage;
    
    /**
     * 构造函数
     * @param titleLabel 标题标签，用于获取窗口引用
     * @param editMain 编辑区域，用于添加键盘监听
     * @param onLockModuleCallback 锁定模块回调方法
     */
    public WindowListener(Label titleLabel, TextArea editMain, Runnable onLockModuleCallback) {
        if (titleLabel == null) {
            logger.error("Cannot initialize WindowListener: titleLabel is null");
            throw new IllegalArgumentException("Title label cannot be null");
        }
        if (editMain == null) {
            logger.error("Cannot initialize WindowListener: editMain is null");
            throw new IllegalArgumentException("Edit main TextArea cannot be null");
        }
        if (onLockModuleCallback == null) {
            logger.error("Cannot initialize WindowListener: onLockModuleCallback is null");
            throw new IllegalArgumentException("Lock module callback cannot be null");
        }
        
        this.titleLabel = titleLabel;
        this.editMain = editMain;
        this.onLockModuleCallback = onLockModuleCallback;
        logger.debug("WindowListener initialized with titleLabel, editMain and lock module callback");
    }
    
    /**
     * 添加窗口状态监听器
     * 包括窗口最大化状态监听和键盘快捷键监听
     */
    public void addWindowListener() {
        logger.info("Setting up window listeners");
        
        // 使用 Platform.runLater 确保在 JavaFX 线程中执行，且窗口已经显示
        Platform.runLater(() -> {
            // 获取 primaryStage
            primaryStage = (Stage) titleLabel.getScene().getWindow();
            
            if (primaryStage != null) {
                logger.debug("Primary stage obtained successfully");
                
                // 添加窗口最大化状态变化监听器
                setupMaximizedListener();
                
                // 添加键盘快捷键监听器
                setupKeyboardShortcuts();
                
                logger.info("Window listeners added successfully");
            } else {
                logger.warn("Failed to obtain primary stage - it is null");
            }
        });
    }
    
    /**
     * 设置窗口最大化监听器
     */
    private void setupMaximizedListener() {
        try {
            if (primaryStage == null) {
                logger.warn("Cannot setup maximized listener: primary stage is null");
                return;
            }
            
            primaryStage.maximizedProperty().addListener((observable, oldValue, newValue) -> {
                Idf.isMainPageMaximized = newValue;
                logger.info("Window maximized state changed to: {}", newValue);
            });
            logger.trace("Maximized property listener added");
        } catch (Exception e) {
            logger.error("Error setting up maximized listener", e);
        }
    }
    
    /**
     * 设置键盘快捷键监听器
     * 监听 Ctrl+` 组合键用于锁定/解锁模块
     */
    private void setupKeyboardShortcuts() {
        try {
            if (primaryStage == null || primaryStage.getScene() == null) {
                logger.warn("Cannot setup keyboard shortcuts: primary stage or scene is null");
                return;
            }
            
            // 场景级别的键盘快捷键监听
            primaryStage.getScene().setOnKeyPressed(event -> {
                try {
                    // 检查是否按下了 Ctrl+` 组合键
                    if (!Idf.isPreviewWindowShowing) {
                        if (event.isControlDown() && event.getCode() == javafx.scene.input.KeyCode.BACK_QUOTE) {
                            logger.info("Ctrl+` shortcut pressed in scene");
                            handleLockModuleShortcut();
                            event.consume(); // 标记事件已被处理
                        }
                    }
                } catch (Exception e) {
                    logger.error("Error processing scene key press event", e);
                }
            });
            
            // 编辑区域级别的键盘快捷键监听
            if (editMain != null && editMain.getScene() != null) {
                editMain.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
                    try {
                        // 检查是否按下了 Ctrl+` 组合键
                        if (!Idf.isPreviewWindowShowing) {
                            if (event.isControlDown() && event.getCode() == javafx.scene.input.KeyCode.BACK_QUOTE) {
                                logger.info("Ctrl+` shortcut pressed in editMain");
                                handleLockModuleShortcut();
                                event.consume(); // 标记事件已被处理
                            }
                        }
                    } catch (Exception e) {
                        logger.error("Error processing editMain key press event", e);
                    }
                });
            } else {
                logger.warn("EditMain or its scene is null, cannot setup keyboard shortcut");
            }
            
            logger.trace("Keyboard shortcuts registered for scene and editMain");
        } catch (Exception e) {
            logger.error("Error setting up keyboard shortcuts", e);
        }
    }
    
    /**
     * 处理锁定模块快捷键
     */
    private void handleLockModuleShortcut() {
        logger.info("Handling lock module shortcut");
        if (onLockModuleCallback != null) {
            onLockModuleCallback.run();
            logger.debug("Lock module callback executed successfully");
        } else {
            logger.warn("Lock module callback is null, cannot execute shortcut action");
        }
    }
    
    /**
     * 获取主窗口引用
     * @return 主窗口 Stage
     */
    public Stage getPrimaryStage() {
        return primaryStage;
    }
}
