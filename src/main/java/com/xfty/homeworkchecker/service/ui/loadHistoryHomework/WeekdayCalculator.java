package com.xfty.homeworkchecker.service.ui.loadHistoryHomework;

import com.xfty.homeworkchecker.service.HomeworkDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 工作日计算器 - 负责计算本周和上周的日期
 */
public class WeekdayCalculator {
    
    private static final Logger logger = LoggerFactory.getLogger(WeekdayCalculator.class);
    private static final DateTimeFormatter FILE_NAME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    
    /**
     * 获取本周一的日期
     * @param referenceDate 参考日期（通常为当前日期）
     * @return 本周一的日期，如果输入为 null 则返回 null
     */
    public LocalDate getCurrentWeekMonday(LocalDate referenceDate) {
        if (referenceDate == null) {
            logger.warn("Cannot calculate Monday: referenceDate is null");
            return null;
        }
        
        logger.trace("Calculating Monday for reference date: {}", referenceDate);
        try {
            LocalDate result = referenceDate.minusDays(referenceDate.getDayOfWeek().getValue() - 1);
            logger.debug("Monday calculated: {}", result);
            return result;
        } catch (Exception e) {
            logger.error("Error calculating Monday for reference date: {}", referenceDate, e);
            return null;
        }
    }
    
    /**
     * 格式化日期为文件名格式 (yyyyMMdd)
     * @param date 日期
     * @return 格式化后的文件名，如果输入为 null 则返回 null
     */
    public String formatToFileName(LocalDate date) {
        if (date == null) {
            logger.warn("Cannot format null date to filename");
            return null;
        }
        
        logger.trace("Formatted date {} to filename: {}", date, date.format(FILE_NAME_FORMATTER));
        try {
            return date.format(FILE_NAME_FORMATTER);
        } catch (Exception e) {
            logger.error("Error formatting date to filename: {}", date, e);
            return null;
        }
    }
    
    /**
     * 获取本周所有工作日的文件名列表
     * @return 包含周一至周五的文件名数组，如果发生错误返回空数组
     */
    public String[] getCurrentWeekdaysFileNames() {
        logger.debug("Generating current week file names");
        try {
            LocalDate today = LocalDate.now();
            LocalDate monday = getCurrentWeekMonday(today);
            
            if (monday == null) {
                logger.error("Failed to generate current week file names: Monday calculation failed");
                return new String[0];
            }
            
            String[] fileNames = new String[5];
            for (int i = 0; i < 5; i++) {
                fileNames[i] = formatToFileName(monday.plusDays(i));
            }
            
            logger.debug("Generated current week file names: {}", (Object[]) fileNames);
            return fileNames;
        } catch (Exception e) {
            logger.error("Error generating current week file names", e);
            return new String[0];
        }
    }
    
    /**
     * 获取上周末相关文件名称（上周五、上周六、上周日）
     * @return 包含上周末日期的文件名数组，如果发生错误返回空数组
     */
    public String[] getLastWeekendFileNames() {
        logger.debug("Generating last weekend file names");
        try {
            LocalDate today = LocalDate.now();
            LocalDate monday = getCurrentWeekMonday(today);
            
            if (monday == null) {
                logger.error("Failed to generate last weekend file names: Monday calculation failed");
                return new String[0];
            }
            
            // 按优先级顺序：上周日、上周六、上周五
            String[] fileNames = {
                formatToFileName(monday.minusDays(1)), // 上周日
                formatToFileName(monday.minusDays(2)), // 上周六
                formatToFileName(monday.minusDays(3))  // 上周五
            };
            
            logger.debug("Generated last weekend file names: {}", (Object[]) fileNames);
            return fileNames;
        } catch (Exception e) {
            logger.error("Error generating last weekend file names", e);
            return new String[0];
        }
    }
    
    /**
     * 获取完整的工作周文件名称列表（用于初始化按钮状态）
     * 顺序：上周五、上周六、上周日、周一、周二、周三、周四、周五
     * @return 完整的周文件名称列表，如果发生错误返回空数组
     */
    public String[] getAllWeekdaysFileNames() {
        logger.debug("Generating all weekdays file names");
        try {
            LocalDate today = LocalDate.now();
            LocalDate monday = getCurrentWeekMonday(today);
            
            if (monday == null) {
                logger.error("Failed to generate all weekdays file names: Monday calculation failed");
                return new String[0];
            }
            
            String[] fileNames = new String[] {
                formatToFileName(monday.minusDays(3)), // 上周五
                formatToFileName(monday.minusDays(2)), // 上周六
                formatToFileName(monday.minusDays(1)), // 上周日
                formatToFileName(monday),              // 周一
                formatToFileName(monday.plusDays(1)),  // 周二
                formatToFileName(monday.plusDays(2)),  // 周三
                formatToFileName(monday.plusDays(3)),  // 周四
                formatToFileName(monday.plusDays(4))   // 周五
            };
            
            logger.debug("Generated all weekdays file names: {}", (Object[]) fileNames);
            return fileNames;
        } catch (Exception e) {
            logger.error("Error generating all weekdays file names", e);
            return new String[0];
        }
    }
}
