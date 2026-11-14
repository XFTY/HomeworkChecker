package com.xfty.homeworkchecker.service.ui.mainPage;

import com.xfty.homeworkchecker.Idf;
import com.xfty.homeworkchecker.service.HomeworkDatabase;
import javafx.application.Platform;
import javafx.scene.control.TextArea;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Edit state management service for handling editable state and auto-lock functionality
 */
public class EditStateService {
    
    private static final Logger logger = LoggerFactory.getLogger(EditStateService.class);
    
    // Constants
    private static final int INITIAL_TIMINGS = 60; // seconds
    private static final int CHECK_INTERVAL = 5000; // 5 seconds
    
    // Component references
    private final TextArea editMain;
    private final HomeworkDatabase homeworkDatabase;
    private final Runnable onLockModuleClicked;
    private final Supplier<Boolean> isEditingCard;
    
    // State tracking
    private volatile boolean isRunning = false;
    // 当前看门狗线程引用，停止时通过中断它来保证同一时刻只有一个看门狗在运行
    private volatile Thread watchdogThread;
    
    /**
     * Constructor
     * @param editMain The text area for editing
     * @param homeworkDatabase Database manager for saving homework context
     * @param onLockModuleClicked Callback to trigger lock module action
     * @param isEditingCard Supplier returning true when a card is being edited
     */
    public EditStateService(TextArea editMain, 
                           HomeworkDatabase homeworkDatabase,
                           Runnable onLockModuleClicked,
                           Supplier<Boolean> isEditingCard) {
        if (editMain == null) {
            logger.error("Cannot initialize EditStateService: editMain is null");
            throw new IllegalArgumentException("EditMain TextArea cannot be null");
        }
        if (homeworkDatabase == null) {
            logger.error("Cannot initialize EditStateService: homeworkDatabase is null");
            throw new IllegalArgumentException("HomeworkDatabase cannot be null");
        }
        if (onLockModuleClicked == null) {
            logger.error("Cannot initialize EditStateService: onLockModuleClicked is null");
            throw new IllegalArgumentException("OnLockModuleClicked callback cannot be null");
        }
        
        this.editMain = editMain;
        this.homeworkDatabase = homeworkDatabase;
        this.onLockModuleClicked = onLockModuleClicked;
        this.isEditingCard = isEditingCard;
        logger.debug("EditStateService initialized");
    }
    
    /**
     * Start the unlock counter watchdog thread
     * This monitors content changes and auto-locks after timeout
     */
    public synchronized void startUnlockCounter() {
        logger.info("Starting unlock counter watchdog");
        
        // 启动前先确保旧的看门狗线程已停止，避免多个看门狗并行倒计时
        terminateWatchdogThread();
        
        isRunning = true;
        
        Thread thread = new Thread(() -> {
            logger.info("Unlock counter watchdog thread started");
            int timings = INITIAL_TIMINGS;
            logger.debug("Initial timing value: {} seconds", timings);
            
            // 使用运行标志作为循环条件，停止后可立即退出，无需等待下一轮状态检查
            while (isRunning) {
                logger.debug("Unlock counter check, remaining time: {} seconds", timings);
                
                // Check if software is closing
                if (Idf.isSoftwareClosing) {
                    logger.info("Software is closing, triggering lock module");
                    Platform.runLater(onLockModuleClicked);
                    break;
                }
                
                // Check if user manually locked the module
                if (!Idf.isEditable) {
                    logger.info("User locked module, exiting thread");
                    break;
                }

                try {
                    logger.debug("Waiting for {} ms before next check", CHECK_INTERVAL);
                    Thread.sleep(CHECK_INTERVAL);
                } catch (InterruptedException e) {
                    // 主动停止（锁定/清理）会中断本线程，属正常流程；仅软件关闭时才需要触发锁定
                    if (Idf.isSoftwareClosing) {
                        logger.debug("Software is closing, locking module from interrupted thread");
                        Platform.runLater(onLockModuleClicked);
                    } else {
                        logger.debug("Unlock counter thread interrupted for stop");
                    }
                    break;
                }

                String editMainText = editMain.getText();
                logger.debug("Current editMain text length: {}", editMainText.length());

                // Check if content has changed
                if (!Objects.equals(Idf.homeworkContextCache, editMainText)) {
                    logger.info("Content changed, updating cache and saving to database");
                    Idf.homeworkContextCache = editMainText;
                    try {
                        homeworkDatabase.writeHomeworkContextByDay(Idf.homeworkContextCache);
                        timings = Idf.watchdogIdleTimeoutSeconds;
                        logger.info("Content changed, resetting timings to {} seconds", timings);
                    } catch (Exception e) {
                        logger.error("Failed to save homework context to database", e);
                        // Continue with timing decrement even if save fails
                        timings = timings - 5;
                    }
                } else {
                    if (isEditingCard != null && isEditingCard.get()) {
                        logger.debug("Card being edited, skipping auto-lock countdown");
                        continue;
                    }
                    timings = timings - 5;
                    logger.debug("Content unchanged, decrementing timings to {} seconds", timings);
                }

                // Auto-lock when timeout reached
                if (timings == 0) {
                    logger.info("Timings reached zero, auto-locking module");
                    Platform.runLater(onLockModuleClicked);
                    break;
                }
            }
            
            // 仅当当前线程仍是登记的看门狗时才清除状态，避免旧线程误清新线程的运行标志
            synchronized (this) {
                if (Thread.currentThread() == watchdogThread) {
                    isRunning = false;
                    watchdogThread = null;
                }
            }
            logger.info("Unlock counter watchdog thread finished");
        }, "UnlockCounterThread");
        
        // 先登记线程引用再启动，确保停止逻辑能拿到线程并中断其休眠
        watchdogThread = thread;
        thread.start();
    }
    
    /**
     * Stop the unlock counter thread
     */
    public synchronized void stopUnlockCounter() {
        logger.info("Stopping unlock counter");
        terminateWatchdogThread();
    }
    
    /**
     * 终止当前看门狗线程：清除运行标志与线程引用，并中断其休眠
     * 由 startUnlockCounter/stopUnlockCounter 复用，保证同一时刻只有一个看门狗
     */
    private synchronized void terminateWatchdogThread() {
        isRunning = false;
        // 先清空引用再中断，避免旧线程退出时干扰随后启动的新线程
        Thread thread = watchdogThread;
        watchdogThread = null;
        if (thread != null && thread.isAlive()) {
            thread.interrupt();
        }
    }
    
    /**
     * Check if the counter is currently running
     * @return true if running, false otherwise
     */
    public boolean isRunning() {
        return isRunning;
    }
}
