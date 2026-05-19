package com.xfty.homeworkchecker.service;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * DatabaseManager — 数据库文件清单服务
 * <p>
 * 获取 homeworkDatabase 目录下的所有文件名，用于数据库编辑器中的文件列表展示。
 * </p>
 */
public class DatabaseManager {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);

    /**
     * 获取 homeworkDatabase 目录下所有文件的文件名列表
     *
     * @return 文件名列表（按文件系统顺序）
     */
    public List<String> getAllDatabaseFileName() {
        List<String> fileNames = new ArrayList<>();
            
        // 获取用户文档目录下的 homeworkChecker 目录
        File userDir = FileUtils.getUserDirectory();
        File homeworkCheckerDir = new File(userDir, "homeworkChecker");
        File homeworkDatabaseDir = new File(homeworkCheckerDir, "homeworkDatabase");
            
        // 检查目录是否存在
        if (!homeworkDatabaseDir.exists() || !homeworkDatabaseDir.isDirectory()) {
            logger.warn("Database directory does not exist or is not a directory: {}", homeworkDatabaseDir.getAbsolutePath());
            return fileNames;
        }
            
        // 获取目录下所有文件
        File[] files = homeworkDatabaseDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    fileNames.add(file.getName());
                }
            }
        }
            
        logger.debug("Found {} database files", fileNames.size());
        return fileNames;
    }
}
