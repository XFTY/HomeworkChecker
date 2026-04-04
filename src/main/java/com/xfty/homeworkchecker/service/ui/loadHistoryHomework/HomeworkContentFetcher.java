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
        this.homeworkDatabase = new HomeworkDatabase();
    }
    
    /**
     * 根据年月日获取作业内容
     * @param year 年 (4 位数字)
     * @param month 月 (2 位数字)
     * @param day 日 (2 位数字)
     * @return 作业内容，如果不存在则返回 null
     */
    public String getHomeworkContext(String year, String month, String day) {
        logger.debug("Fetching homework context for {}-{}-{}", year, month, day);
        return homeworkDatabase.getHomeworkContext(year, month, day);
    }
    
    /**
     * 根据文件名获取作业内容
     * @param fileName 文件名 (yyyyMMdd 格式)
     * @return 作业内容，如果不存在则返回 null
     */
    public String getHomeworkContextByFileName(String fileName) {
        logger.debug("Fetching homework context for file: {}", fileName);
        return homeworkDatabase.getHomeworkContextByFileName(fileName);
    }
    
    /**
     * 批量获取多个文件名的作业内容
     * @param fileNames 文件名数组
     * @return 作业内容数组，与输入文件名顺序对应，不存在的返回 null
     */
    public String[] getHomeworkContextsByFileNames(String[] fileNames) {
        logger.debug("Batch fetching homework contexts for {} files", fileNames.length);
        String[] results = new String[fileNames.length];
        
        for (int i = 0; i < fileNames.length; i++) {
            results[i] = getHomeworkContextByFileName(fileNames[i]);
        }
        
        return results;
    }
    
    /**
     * 检查指定日期是否有作业数据
     * @param fileName 文件名 (yyyyMMdd 格式)
     * @return 如果有作业数据返回 true，否则返回 false
     */
    public boolean hasHomeworkData(String fileName) {
        return getHomeworkContextByFileName(fileName) != null;
    }
}
