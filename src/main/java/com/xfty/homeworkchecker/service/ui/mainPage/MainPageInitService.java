package com.xfty.homeworkchecker.service.ui.mainPage;

import com.xfty.homeworkchecker.Idf;
import com.xfty.homeworkchecker.service.HomeworkDatabase;
import javafx.scene.control.TextArea;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * MainPage 初始化服务类
 * 负责处理主页初始化时的业务逻辑
 */
public class MainPageInitService {
    private static final Logger logger = LoggerFactory.getLogger(MainPageInitService.class);
    
    private final HomeworkDatabase homeworkDatabase;
    private final String initTemplate;
    
    public MainPageInitService() {
        this.homeworkDatabase = new HomeworkDatabase();
        this.initTemplate = Idf.initTemple;
    }
    
    /**
     * 加载作业内容和标题
     * @return 包含作业内容和标题的 Result 对象
     */
    public Result loadHomeworkContent() {
        logger.info("Loading homework content...");
        
        try {
            String content;
            String title;
            
            if (Idf.userConfig != null) {
                logger.debug("Loading with user config");
                content = loadContentWithConfig();
                title = buildTitle(content != null && !content.isEmpty());
            } else {
                logger.warn("User configuration is null, using default settings");
                content = loadContentWithoutConfig();
                title = buildTitle(content != null && !content.isEmpty());
            }
            
            return new Result(content, title);
        } catch (Exception e) {
            logger.error("Failed to load homework content", e);
            return new Result("", "Error loading content");
        }
    }
    
    /**
     * 有用户配置时加载内容
     */
    private String loadContentWithConfig() {
        String todayHomeworkContext = homeworkDatabase.getTodayHomeworkContext();
        logger.debug("Today homework context is null or empty: {}", todayHomeworkContext == null || todayHomeworkContext.isEmpty());
        
        if (todayHomeworkContext == null || todayHomeworkContext.isEmpty()) {
            return loadHomeworkByDate();
        } else {
            logger.info("Today's homework loaded from database");
            return todayHomeworkContext;
        }
    }
    
    /**
     * 无用户配置时加载内容
     */
    private String loadContentWithoutConfig() {
        String todayHomeworkContext = homeworkDatabase.getTodayHomeworkContext();
        logger.debug("Today homework context with null config: {}", todayHomeworkContext != null);
        
        if (todayHomeworkContext == null || todayHomeworkContext.isEmpty()) {
            return loadHomeworkByDate();
        } else {
            logger.info("Today's homework loaded from database with null config");
            return todayHomeworkContext;
        }
    }
    
    /**
     * 根据日期加载作业内容（周末或工作日）
     */
    private String loadHomeworkByDate() {
        LocalDate today = LocalDate.now();
        int dayOfWeek = today.getDayOfWeek().getValue();
        logger.debug("Current date: {}, day of week: {}", today, dayOfWeek);
        
        // 判断是否为周末（星期五、六、日）
        if (dayOfWeek == 5 || dayOfWeek == 6 || dayOfWeek == 7) {
            logger.debug("Weekend mode activated, loading weekend homework");
            return loadWeekendHomework();
        } else {
            logger.debug("Weekday mode, loading init template");
            return loadInitTemplate();
        }
    }
    
    /**
     * 加载周末作业
     */
    private String loadWeekendHomework() {
        LocalDate today = LocalDate.now();
        LocalDate monday = today.minusDays(today.getDayOfWeek().getValue() - 1);
        
        // 按优先级顺序检查周日、周六、周五的文件
        String[] fileNames = {
                monday.plusDays(6).format(DateTimeFormatter.ofPattern("yyyyMMdd")), // 周日
                monday.plusDays(5).format(DateTimeFormatter.ofPattern("yyyyMMdd")), // 周六
                monday.plusDays(4).format(DateTimeFormatter.ofPattern("yyyyMMdd"))  // 周五
        };
        
        String homeworkContent = null;
        for (String fileName : fileNames) {
            logger.debug("Checking homework file: {}", fileName);
            homeworkContent = homeworkDatabase.getHomeworkContextByFileName(fileName);
            if (homeworkContent != null) {
                logger.info("Found weekend homework in file: {}", fileName);
                break;
            } else {
                logger.debug("Weekend homework file not found: {}", fileName);
            }
        }
        
        if (homeworkContent != null) {
            logger.info("Weekend homework loaded successfully");
            return homeworkContent;
        } else {
            logger.debug("No weekend homework found, loading init template");
            return loadInitTemplate();
        }
    }
    
    /**
     * 加载初始化模板
     */
    private String loadInitTemplate() {
        try {
            logger.info("Init template loaded successfully");
            return initTemplate;
        } catch (Exception e) {
            logger.error("Failed to load init template", e);
            return "";
        }
    }
    
    /**
     * 构建标题
     * @param hasHomework 是否有作业
     * @return 标题字符串，如果发生错误返回默认标题
     */
    private String buildTitle(boolean hasHomework) {
        try {
            String formatKey = hasHomework ? "mainpage.format.weekday" : "mainpage.format.weekday";
            
            // 判断是否为周末来设置标题
            LocalDate today = LocalDate.now();
            int dayOfWeek = today.getDayOfWeek().getValue();
            
            if (dayOfWeek == 5 || dayOfWeek == 6 || dayOfWeek == 7) {
                formatKey = hasHomework ? "mainpage.format.weekend" : "mainpage.format.weekday";
            }
            
            if (Idf.userLanguageBundle == null) {
                logger.error("User language bundle is null, cannot build title");
                return "Homework Checker";
            }
            
            return Idf.year + 
                   Idf.userLanguageBundle.getString("mainpage.year") + 
                   Idf.month + 
                   Idf.userLanguageBundle.getString("mainpage.month") + 
                   Idf.day + 
                   Idf.userLanguageBundle.getString(formatKey);
        } catch (Exception e) {
            logger.error("Error building title", e);
            return "Homework Checker - Error";
        }
    }
    
    /**
     * 获取字体配置
     * @return FontConfig 对象
     */
    public FontConfig getFontConfig() {
        if (Idf.userConfig != null) {
            try {
                String fontFamily = Idf.userConfig.getJSONObject("font")
                    .getJSONObject("fontFamily")
                    .getString("defaultFontFamily");
                int fontSize = Idf.userConfig.getJSONObject("font")
                    .getJSONObject("textSize")
                    .getInteger("editMain");
                logger.debug("Getting font config: {} with size: {}", fontFamily, fontSize);
                return new FontConfig(fontFamily, fontSize);
            } catch (Exception e) {
                logger.warn("Failed to get font config from userConfig, using default", e);
            }
        }
        
        logger.debug("Using default font config");
        return new FontConfig("System", 14);
    }
    
    /**
     * 内部类：封装返回结果
     */
    public static class Result {
        private final String content;
        private final String title;
        
        public Result(String content, String title) {
            this.content = content;
            this.title = title;
        }
        
        public String getContent() {
            return content;
        }
        
        public String getTitle() {
            return title;
        }
    }
    
    public void clearTodayHomework(TextArea editMain) {
        logger.info("Clearing today's homework");
        if (!Idf.isEditable) {
            editMain.setEditable(true);
            editMain.setText(initTemplate);
            editMain.setEditable(false);
        } else {
            editMain.setText(initTemplate);
        }
        logger.debug("Clear homework operation completed");
    }

    /**
     * 内部类：封装字体配置
     */
    public static class FontConfig {
        private final String fontFamily;
        private final int fontSize;
        
        public FontConfig(String fontFamily, int fontSize) {
            this.fontFamily = fontFamily;
            this.fontSize = fontSize;
        }
        
        public String getFontFamily() {
            return fontFamily;
        }
        
        public int getFontSize() {
            return fontSize;
        }
    }
}
