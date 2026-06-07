package com.xfty.homeworkchecker.service.ui.settings;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.xfty.homeworkchecker.Idf;
import com.xfty.homeworkchecker.service.HomeworkDatabase;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * DataBaseEditorService — 数据库编辑器业务逻辑层
 * <p>
 * 封装数据库文件扫描、作业数据读取、作业集删除、数据库大小计算等操作，
 * 将业务逻辑与 UI 控制器分离（MVC 架构）。
 * </p>
 */
public class DataBaseEditorService {

    private static final Logger logger = LoggerFactory.getLogger(DataBaseEditorService.class);
    private static final Pattern DATE_FILE_PATTERN = Pattern.compile("^\\d{8}$");

    private final HomeworkDatabase homeworkDatabase;

    public DataBaseEditorService() {
        this.homeworkDatabase = new HomeworkDatabase();
    }

    /**
     * 扫描 homeworkDatabase 目录，返回所有 YYYYMMDD 格式的文件名（按日期降序排列）
     *
     * @return 文件名列表
     */
    public List<String> scanHomeworkFiles() {
        List<String> result = new ArrayList<>();
        File databaseDir = getHomeworkDatabaseDir();
        if (!databaseDir.exists() || !databaseDir.isDirectory()) {
            logger.warn("Database directory does not exist: {}", databaseDir.getAbsolutePath());
            return result;
        }

        File[] files = databaseDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && DATE_FILE_PATTERN.matcher(file.getName()).matches()) {
                    result.add(file.getName());
                }
            }
        }

        result.sort((a, b) -> b.compareTo(a));
        logger.debug("Found {} homework files", result.size());
        return result;
    }

    /**
     * 获取指定文件名对应的作业数据
     *
     * @param fileName 文件名（YYYYMMDD）
     * @return HomeworkData 包含内容和警告卡片，无数据返回 null
     */
    public HomeworkData getHomeworkData(String fileName) {
        try {
            File dataFile = new File(getHomeworkDatabaseDir(), fileName);
            if (!dataFile.exists()) {
                return null;
            }

            String fileContent = FileUtils.readFileToString(dataFile, StandardCharsets.UTF_8);
            JSONObject json = JSON.parseObject(fileContent);

            String context = json.getString("context");
            JSONArray warnings = json.getJSONArray("warnings");
            if (warnings == null) {
                warnings = new JSONArray();
            }

            return new HomeworkData(context, warnings);
        } catch (Exception e) {
            logger.error("Error reading homework data for file: {}", fileName, e);
            return null;
        }
    }

    /**
     * 删除指定日期对应的作业集（数据文件 + 关联图片）
     *
     * @param fileName 文件名（YYYYMMDD）
     * @return DeleteResult 包含执行结果
     */
    public DeleteResult deleteHomeworkSet(String fileName) {
        DeleteResult result = new DeleteResult();

        try {
            File dataFile = new File(getHomeworkDatabaseDir(), fileName);
            if (!dataFile.exists()) {
                result.success = false;
                return result;
            }

            String fileContent = FileUtils.readFileToString(dataFile, StandardCharsets.UTF_8);
            JSONObject json = JSON.parseObject(fileContent);

            JSONArray warnings = json.getJSONArray("warnings");
            if (warnings != null && !warnings.isEmpty()) {
                for (int i = 0; i < warnings.size(); i++) {
                    JSONObject card = warnings.getJSONObject(i);
                    String imagePath = card.getString("imagePath");
                    if (imagePath != null && !imagePath.isEmpty()) {
                        File imageFile = new File(getHomeworkDatabaseDir(), imagePath);
                        if (imageFile.exists()) {
                            if (imageFile.delete()) {
                                logger.info("Deleted image: {}", imageFile.getAbsolutePath());
                            } else {
                                logger.warn("Failed to delete image: {}", imageFile.getAbsolutePath());
                            }
                        } else {
                            result.missingImages.add(imagePath);
                            logger.warn("Image file not found: {}", imageFile.getAbsolutePath());
                        }
                    }
                }
            }

            if (dataFile.delete()) {
                logger.info("Deleted homework data file: {}", fileName);
                result.success = true;
            } else {
                logger.warn("Failed to delete homework data file: {}", fileName);
                result.success = false;
            }
        } catch (Exception e) {
            logger.error("Error deleting homework set: {}", fileName, e);
            result.success = false;
        }

        return result;
    }

    /**
     * 计算 homeworkDatabase 目录下所有文件总大小
     *
     * @return 字节数
     */
    public long calculateDatabaseSize() {
        File databaseDir = getHomeworkDatabaseDir();
        return calculateDirSize(databaseDir);
    }

    /**
     * 获取 homeworkDatabase 目录的绝对路径
     *
     * @return 路径字符串
     */
    public String getDatabasePath() {
        return getHomeworkDatabaseDir().getAbsolutePath();
    }

    /**
     * 从 config.json 读取作业保留天数
     *
     * @return 保留天数，默认 30
     */
    public int getRetentionDays() {
        try {
            if (Idf.userConfig != null) {
                JSONObject dbConfig = Idf.userConfig.getJSONObject("homeworkDatabase");
                if (dbConfig != null) {
                    JSONObject autoCleanup = dbConfig.getJSONObject("autoCleanup");
                    if (autoCleanup != null) {
                        return autoCleanup.getIntValue("daysToKeep", 30);
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Error reading retention days from config", e);
        }
        return 30;
    }

    /**
     * 将作业保留天数写入 config.json
     *
     * @param days 保留天数
     */
    public void saveRetentionDays(int days) {
        try {
            if (Idf.userConfig == null) {
                logger.error("Cannot save retention days: userConfig is null");
                return;
            }

            JSONObject autoCleanup = ensureAutoCleanupConfig();
            autoCleanup.put("daysToKeep", days);
            homeworkDatabase.updateConfig(Idf.userConfig);
            logger.info("Saved retention days: {}", days);
        } catch (Exception e) {
            logger.error("Error saving retention days", e);
        }
    }

    /**
     * 读取自动清理功能是否启用
     *
     * @return true 表示已启用
     */
    public boolean isAutoCleanupEnabled() {
        try {
            if (Idf.userConfig != null) {
                JSONObject dbConfig = Idf.userConfig.getJSONObject("homeworkDatabase");
                if (dbConfig != null) {
                    JSONObject autoCleanup = dbConfig.getJSONObject("autoCleanup");
                    if (autoCleanup != null) {
                        return autoCleanup.getBooleanValue("enabled", false);
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Error reading auto-cleanup enabled state", e);
        }
        return false;
    }

    /**
     * 保存自动清理功能的启用开关
     *
     * @param enabled 是否启用
     */
    public void saveAutoCleanupEnabled(boolean enabled) {
        try {
            if (Idf.userConfig == null) {
                logger.error("Cannot save auto-cleanup state: userConfig is null");
                return;
            }
            JSONObject autoCleanup = ensureAutoCleanupConfig();
            autoCleanup.put("enabled", enabled);
            homeworkDatabase.updateConfig(Idf.userConfig);
            logger.info("Saved auto-cleanup enabled: {}", enabled);
        } catch (Exception e) {
            logger.error("Error saving auto-cleanup state", e);
        }
    }

    /**
     * 执行自动清理：删除超过保留天数的历史作业文件及关联图片。
     * 每次软件启动时调用。
     *
     * @return 清理的文件数量
     */
    public int performAutoCleanup() {
        if (!isAutoCleanupEnabled()) {
            logger.debug("Auto-cleanup is disabled, skipping");
            return 0;
        }

        int daysToKeep = getRetentionDays();
        if (daysToKeep <= 0) {
            logger.debug("Retention days is non-positive ({}), skipping auto-cleanup", daysToKeep);
            return 0;
        }

        java.time.LocalDate cutoff = java.time.LocalDate.now().minusDays(daysToKeep);
        String cutoffStr = cutoff.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        logger.info("Auto-cleanup: deleting files older than {} (retention: {} days)", cutoffStr, daysToKeep);

        int deletedCount = 0;
        List<String> files = scanHomeworkFiles();
        for (String fileName : files) {
            if (fileName.compareTo(cutoffStr) < 0) {
                DeleteResult result = deleteHomeworkSet(fileName);
                if (result.success) {
                    deletedCount++;
                    logger.info("Auto-cleanup: deleted {}", fileName);
                } else {
                    logger.warn("Auto-cleanup: failed to delete {}", fileName);
                }
            }
        }

        if (deletedCount > 0) {
            logger.info("Auto-cleanup completed: {} files deleted", deletedCount);
        } else {
            logger.debug("Auto-cleanup: no expired files found");
        }
        return deletedCount;
    }

    private JSONObject ensureAutoCleanupConfig() {
        JSONObject dbConfig = Idf.userConfig.getJSONObject("homeworkDatabase");
        if (dbConfig == null) {
            dbConfig = new JSONObject();
            Idf.userConfig.put("homeworkDatabase", dbConfig);
        }
        JSONObject autoCleanup = dbConfig.getJSONObject("autoCleanup");
        if (autoCleanup == null) {
            autoCleanup = new JSONObject();
            dbConfig.put("autoCleanup", autoCleanup);
        }
        return autoCleanup;
    }

    private File getHomeworkDatabaseDir() {
        File userDir = FileUtils.getUserDirectory();
        File homeworkCheckerDir = new File(userDir, "homeworkChecker");
        return new File(homeworkCheckerDir, "homeworkDatabase");
    }

    private long calculateDirSize(File dir) {
        if (!dir.exists() || !dir.isDirectory()) {
            return 0;
        }
        long total = 0;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    total += file.length();
                } else if (file.isDirectory()) {
                    total += calculateDirSize(file);
                }
            }
        }
        return total;
    }

    /**
     * 作业数据封装内部类
     */
    public static class HomeworkData {
        public final String context;
        public final JSONArray warnings;

        public HomeworkData(String context, JSONArray warnings) {
            this.context = context;
            this.warnings = warnings;
        }
    }

    /**
     * 删除结果封装内部类
     */
    public static class DeleteResult {
        public boolean success;
        public List<String> missingImages = new ArrayList<>();
    }
}
