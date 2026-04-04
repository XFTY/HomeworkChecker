package com.xfty.homeworkchecker.service.ui.mainPage;

import com.xfty.homeworkchecker.Idf;
import com.xfty.homeworkchecker.service.HomeworkDatabase;
import javafx.application.Platform;
import javafx.scene.control.TextArea;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Edit state management service for handling editable state and auto-lock functionality
 */
public class EditStateService {
    
    private static final Logger logger = LoggerFactory.getLogger(EditStateService.class);
    
    // Constants
    private static final int INITIAL_TIMINGS = 60; // seconds
    private static final int RESET_TIMINGS = 30; // seconds
    private static final int CHECK_INTERVAL = 5000; // 5 seconds
    
    // Component references
    private final TextArea editMain;
    private final HomeworkDatabase homeworkDatabase;
    private final Runnable onLockModuleClicked;
    
    // State tracking
    private volatile boolean isRunning = false;
    
    /**
     * Constructor
     * @param editMain The text area for editing
     * @param homeworkDatabase Database manager for saving homework context
     * @param onLockModuleClicked Callback to trigger lock module action
     */
    public EditStateService(TextArea editMain, 
                           HomeworkDatabase homeworkDatabase,
                           Runnable onLockModuleClicked) {
        this.editMain = editMain;
        this.homeworkDatabase = homeworkDatabase;
        this.onLockModuleClicked = onLockModuleClicked;
        logger.debug("EditStateService initialized");
    }
    
    /**
     * Start the unlock counter watchdog thread
     * This monitors content changes and auto-locks after timeout
     */
    public void startUnlockCounter() {
        logger.info("Starting unlock counter watchdog");
        
        if (isRunning) {
            logger.warn("Unlock counter is already running");
            return;
        }
        
        isRunning = true;
        
        new Thread(() -> {
            logger.info("Unlock counter watchdog thread started");
            int timings = INITIAL_TIMINGS;
            logger.debug("Initial timing value: {} seconds", timings);
            
            while (true) {
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
                    logger.warn("Unlock counter thread interrupted", e);
                    // When interrupted, check if software should be closed
                    if (Idf.isSoftwareClosing) {
                        logger.debug("Software is closing, locking module from interrupted thread");
                        Platform.runLater(onLockModuleClicked);
                    }
                    break;
                }

                String editMainText = editMain.getText();
                logger.debug("Current editMain text length: {}", editMainText.length());

                // Check if content has changed
                if (!Objects.equals(Idf.homeworkContextCache, editMainText)) {
                    logger.info("Content changed, updating cache and saving to database");
                    Idf.homeworkContextCache = editMainText;
                    homeworkDatabase.writeHomeworkContextByDay(Idf.homeworkContextCache);
                    timings = RESET_TIMINGS;
                    logger.info("Content changed, resetting timings to {} seconds", timings);
                } else {
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
            
            isRunning = false;
            logger.info("Unlock counter watchdog thread finished");
        }, "UnlockCounterThread").start();
    }
    
    /**
     * Stop the unlock counter thread
     */
    public void stopUnlockCounter() {
        logger.info("Stopping unlock counter");
        isRunning = false;
    }
    
    /**
     * Check if the counter is currently running
     * @return true if running, false otherwise
     */
    public boolean isRunning() {
        return isRunning;
    }
}
