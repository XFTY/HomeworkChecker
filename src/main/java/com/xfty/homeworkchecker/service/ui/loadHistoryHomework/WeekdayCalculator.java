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
     * @return 本周一的日期
     */
    public LocalDate getCurrentWeekMonday(LocalDate referenceDate) {
        return referenceDate.minusDays(referenceDate.getDayOfWeek().getValue() - 1);
    }
    
    /**
     * 格式化日期为文件名格式 (yyyyMMdd)
     * @param date 日期
     * @return 格式化后的文件名
     */
    public String formatToFileName(LocalDate date) {
        return date.format(FILE_NAME_FORMATTER);
    }
    
    /**
     * 获取本周所有工作日的文件名列表
     * @return 包含周一至周五的文件名数组
     */
    public String[] getCurrentWeekdaysFileNames() {
        LocalDate today = LocalDate.now();
        LocalDate monday = getCurrentWeekMonday(today);
        
        String[] fileNames = new String[5];
        for (int i = 0; i < 5; i++) {
            fileNames[i] = formatToFileName(monday.plusDays(i));
        }
        
        logger.debug("Generated current week file names: {}", (Object[]) fileNames);
        return fileNames;
    }
    
    /**
     * 获取上周末相关文件名称（上周五、上周六、上周日）
     * @return 包含上周末日期的文件名数组
     */
    public String[] getLastWeekendFileNames() {
        LocalDate today = LocalDate.now();
        LocalDate monday = getCurrentWeekMonday(today);
        
        // 按优先级顺序：上周日、上周六、上周五
        String[] fileNames = {
            formatToFileName(monday.minusDays(1)), // 上周日
            formatToFileName(monday.minusDays(2)), // 上周六
            formatToFileName(monday.minusDays(3))  // 上周五
        };
        
        logger.debug("Generated last weekend file names: {}", (Object[]) fileNames);
        return fileNames;
    }
    
    /**
     * 获取完整的工作周文件名称列表（用于初始化按钮状态）
     * 顺序：上周五、上周六、上周日、周一、周二、周三、周四、周五
     * @return 完整的周文件名称列表
     */
    public String[] getAllWeekdaysFileNames() {
        LocalDate today = LocalDate.now();
        LocalDate monday = getCurrentWeekMonday(today);
        
        return new String[] {
            formatToFileName(monday.minusDays(3)), // 上周五
            formatToFileName(monday.minusDays(2)), // 上周六
            formatToFileName(monday.minusDays(1)), // 上周日
            formatToFileName(monday),              // 周一
            formatToFileName(monday.plusDays(1)),  // 周二
            formatToFileName(monday.plusDays(2)),  // 周三
            formatToFileName(monday.plusDays(3)),  // 周四
            formatToFileName(monday.plusDays(4))   // 周五
        };
    }
}
