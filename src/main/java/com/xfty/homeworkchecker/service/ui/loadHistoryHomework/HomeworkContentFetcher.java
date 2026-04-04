package com.xfty.homeworkchecker.service.ui.loadHistoryHomework;

import com.xfty.homeworkchecker.service.HomeworkDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 作业内容获取器 - 负责从数据库获取作业内容
 */
public class HomeworkContentFetcher {
    
    private static final Logger logger = LoggerFactory.getLogger(HomeworkContentFetcher.class);
    private final HomeworkDatabase homeworkDatabase;
    
    public HomeworkContentFetcher() {
        try {
            this.homeworkDatabase = new HomeworkDatabase();
            logger.debug("HomeworkContentFetcher initialized successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize HomeworkContentFetcher - database initialization failed", e);
            throw new RuntimeException("Failed to initialize HomeworkContentFetcher", e);
        }
    }
    
    /**
     * 根据年月日获取作业内容
     * @param year 年 (4 位数字)
     * @param month 月 (2 位数字)
     * @param day 日 (2 位数字)
     * @return 作业内容，如果不存在则返回 null
     */
    public String getHomeworkContext(String year, String month, String day) {
        if (year == null || month == null || day == null) {
            logger.warn("Invalid date parameters: year={}, month={}, day={}", year, month, day);
            return null;
        }
        
        logger.debug("Fetching homework context for {}-{}-{}", year, month, day);
        try {
            return homeworkDatabase.getHomeworkContext(year, month, day);
        } catch (Exception e) {
            logger.error("Error fetching homework context for {}-{}-{}", year, month, day, e);
            return null;
        }
    }
    
    /**
     * 根据文件名获取作业内容
     * @param fileName 文件名 (yyyyMMdd 格式)
     * @return 作业内容，如果不存在则返回 null
     */
    public String getHomeworkContextByFileName(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            logger.warn("Invalid file name: {}", fileName);
            return null;
        }
        
        logger.debug("Fetching homework context for file: {}", fileName);
        try {
            return homeworkDatabase.getHomeworkContextByFileName(fileName);
        } catch (Exception e) {
            logger.error("Error fetching homework context for file: {}", fileName, e);
            return null;
        }
    }
    
    /**
     * 批量获取多个文件名的作业内容
     * @param fileNames 文件名数组
     * @return 作业内容数组，与输入文件名顺序对应，不存在的返回 null
     */
    public String[] getHomeworkContextsByFileNames(String[] fileNames) {
        if (fileNames == null) {
            logger.error("Cannot fetch homework contexts: fileNames array is null");
            return new String[0];
        }
        
        logger.debug("Batch fetching homework contexts for {} files", fileNames.length);
        try {
            String[] results = new String[fileNames.length];
            
            for (int i = 0; i < fileNames.length; i++) {
                try {
                    results[i] = getHomeworkContextByFileName(fileNames[i]);
                } catch (Exception e) {
                    logger.error("Error fetching homework context for file index {}: {}", i, fileNames[i], e);
                    results[i] = null;
                }
            }
            
            return results;
        } catch (Exception e) {
            logger.error("Unexpected error in batch fetching homework contexts", e);
            return new String[0];
        }
    }
    
    /**
     * 检查指定日期是否有作业数据
     * @param fileName 文件名 (yyyyMMdd 格式)
     * @return 如果有作业数据返回 true，否则返回 false
     */
    public boolean hasHomeworkData(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            logger.debug("No homework data for invalid file name: {}", fileName);
            return false;
        }
        
        try {
            return getHomeworkContextByFileName(fileName) != null;
        } catch (Exception e) {
            logger.error("Error checking homework data for file: {}", fileName, e);
            return false;
        }
    }
}
