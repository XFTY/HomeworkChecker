package com.xfty.homeworkchecker.service;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class SingletonInstanceManager {
    private static final Logger logger = LoggerFactory.getLogger(SingletonInstanceManager.class);
    private static final String LOCK_FILENAME = "progress.lock";
    
    private FileChannel channel;
    private FileLock lock;
    private File lockFile;

    /**
     * 尝试获取单例锁
     * @return true表示成功获取锁，false表示已有实例在运行
     */
    public boolean acquireLock() {
        try {
            // 获取用户文档目录下的homeworkChecker目录
            File userDir = FileUtils.getUserDirectory();
            File homeworkCheckerDir = new File(userDir, "homeworkChecker");
            
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
     * 释放锁
     */
    public void releaseLock() {
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