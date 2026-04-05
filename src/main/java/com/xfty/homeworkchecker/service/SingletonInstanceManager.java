package com.xfty.homeworkchecker.service;

import javafx.application.Platform;
import javafx.stage.Stage;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.*;
import java.nio.file.StandardOpenOption;

public class SingletonInstanceManager {
    private static final Logger logger = LoggerFactory.getLogger(SingletonInstanceManager.class);
    private static final String LOCK_FILENAME = "progress.lock";
    private static final String ACTIVATION_SIGNAL_FILE = "repeatedly.start";
    
    private FileChannel channel;
    private FileLock lock;
    private File lockFile;
    private File homeworkCheckerDir;
    
    private WatchService watchService;
    private Thread watchThread;
    private Stage mainStage;

    /**
     * 尝试获取单例锁
     * @return true表示成功获取锁，false表示已有实例在运行
     */
    public boolean acquireLock() {
        try {
            // 获取用户文档目录下的homeworkChecker目录
            File userDir = FileUtils.getUserDirectory();
            homeworkCheckerDir = new File(userDir, "homeworkChecker");
            
            // 确保目录存在
            if (!homeworkCheckerDir.exists()) {
                FileUtils.forceMkdir(homeworkCheckerDir);
            }
            
            // 创建锁文件
            lockFile = new File(homeworkCheckerDir, LOCK_FILENAME);
            
            // 打开文件通道
            channel = FileChannel.open(
                Paths.get(lockFile.getAbsolutePath()),
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE
            );
            
            // 尝试获取独占锁
            lock = channel.tryLock();
            
            if (lock == null) {
                // 无法获取锁，说明已经有实例在运行
                logger.warn("Another instance is already running. Failed to acquire lock.");
                closeChannel(); // 关闭通道但不删除锁文件
                return false;
            } else {
                // 成功获取锁
                logger.info("Successfully acquired singleton lock. Lock file: {}", lockFile.getAbsolutePath());
                
                // 添加关闭时清理钩子
                Runtime.getRuntime().addShutdownHook(new Thread(this::releaseLock));
                return true;
            }
        } catch (IOException e) {
            logger.error("Error occurred while trying to acquire singleton lock", e);
            closeChannel();
            return false;
        }
    }
    
    /**
     * 启动WatchService监听激活信号
     * @param stage 主窗口引用
     */
    public void startWatchService(Stage stage) {
        this.mainStage = stage;
        
        try {
            watchService = FileSystems.getDefault().newWatchService();
            Path path = Paths.get(homeworkCheckerDir.getAbsolutePath());
            path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);
            
            // 启动后台监听线程
            watchThread = new Thread(this::watchActivationSignal, "ActivationSignalWatcher");
            watchThread.setDaemon(true);
            watchThread.start();
            
            logger.info("WatchService started for activation signal monitoring");
        } catch (IOException e) {
            logger.error("Failed to start WatchService", e);
        }
    }
    
    /**
     * 监听激活信号的后台任务
     */
    private void watchActivationSignal() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                WatchKey key = watchService.take(); // 阻塞等待事件
                
                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    
                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                        continue;
                    }
                    
                    // 检查是否是激活信号文件
                    Path fileName = (Path) event.context();
                    if (ACTIVATION_SIGNAL_FILE.equals(fileName.toString())) {
                        logger.info("Received activation signal from another instance");
                        
                        // 在JavaFX应用线程中激活窗口并短暂置顶
                        Platform.runLater(() -> {
                            if (mainStage != null) {
                                // 先置顶窗口
                                mainStage.setAlwaysOnTop(true);
                                mainStage.toFront();
                                mainStage.requestFocus();
                                logger.info("Window activated and brought to front with alwaysOnTop");
                                
                                // 1.5秒后取消置顶
                                javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(
                                    javafx.util.Duration.millis(1500)
                                );
                                pause.setOnFinished(e -> {
                                    mainStage.setAlwaysOnTop(false);
                                    logger.info("Window alwaysOnTop disabled");
                                });
                                pause.play();
                            }
                        });
                        
                        // 删除信号文件
                        File signalFile = new File(homeworkCheckerDir, ACTIVATION_SIGNAL_FILE);
                        if (signalFile.exists()) {
                            signalFile.delete();
                            logger.info("Activation signal file deleted");
                        }
                    }
                }
                
                // 重置key以继续接收事件
                boolean valid = key.reset();
                if (!valid) {
                    break;
                }
            } catch (InterruptedException e) {
                logger.info("WatchService thread interrupted");
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                logger.error("Error in WatchService", e);
            }
        }
    }
    
    /**
     * 释放锁
     */
    public void releaseLock() {
        // 先关闭WatchService
        if (watchThread != null) {
            watchThread.interrupt();
        }
        
        if (watchService != null) {
            try {
                watchService.close();
                logger.info("WatchService closed");
            } catch (IOException e) {
                logger.error("Error closing WatchService", e);
            }
        }
        
        try {
            if (lock != null && lock.isValid()) {
                lock.release();
                logger.info("Released singleton lock");
            }
        } catch (IOException e) {
            logger.error("Error releasing lock", e);
        } finally {
            closeChannel();
            
            // 删除锁文件
            if (lockFile != null && lockFile.exists()) {
                if (lockFile.delete()) {
                    logger.info("Deleted lock file: {}", lockFile.getAbsolutePath());
                } else {
                    logger.warn("Failed to delete lock file: {}", lockFile.getAbsolutePath());
                }
            }
        }
    }
    
    /**
     * 关闭文件通道
     */
    private void closeChannel() {
        try {
            if (channel != null && channel.isOpen()) {
                channel.close();
                logger.info("Closed file channel");
            }
        } catch (IOException e) {
            logger.error("Error closing file channel", e);
        }
    }
}