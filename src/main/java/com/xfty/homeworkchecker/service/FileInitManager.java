package com.xfty.homeworkchecker.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.xfty.homeworkchecker.Idf;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class FileInitManager {
    private static final Logger logger = LoggerFactory.getLogger(FileInitManager.class);

    public void initializeUserDirectories() {
        try {
            // 获取用户文档目录
            File userDir = FileUtils.getUserDirectory();
            logger.info("User directory: {}", userDir.getAbsolutePath());

            // 构建 homeworkChecker 目录路径
            File homeworkCheckerDir = new File(userDir, "homeworkChecker");

            // 检查 homeworkChecker 目录是否存在，如果不存在则创建
            if (!homeworkCheckerDir.exists()) {
                logger.info("homeworkChecker directory does not exist, creating...");
                FileUtils.forceMkdir(homeworkCheckerDir);

                // 创建 homeworkDatabase 目录
                File homeworkDatabaseDir = new File(homeworkCheckerDir, "homeworkDatabase");
                FileUtils.forceMkdir(homeworkDatabaseDir);
                logger.info("Created homeworkDatabase directory");

                // 创建 config.json 文件
                File configFile = new File(homeworkCheckerDir, "config.json");
                // 从classpath中读取资源文件
                String modelConfigContent = getResourceFileContent("/modelConfig.json");
                FileUtils.writeStringToFile(configFile, modelConfigContent, StandardCharsets.UTF_8);
                logger.info("Created config.json with default content");
                
                // 创建 config 目录
                File configDir = new File(homeworkCheckerDir, "config");
                FileUtils.forceMkdir(configDir);
                logger.info("Created config directory");

                // 创建 initTemple.txt 文件
                File initTempleFile = new File(homeworkCheckerDir, "initTemple.txt");
                String initTempleContent = getResourceFileContent("/initTemple.txt");
                FileUtils.writeStringToFile(initTempleFile, initTempleContent, StandardCharsets.UTF_8);
                logger.info("Created initTemple.txt with default content");

                // 创建 config/language.json 文件
                File languageDir = new File(configDir, "language.json");
                String languageContent = getResourceFileContent("/language.json");
                FileUtils.writeStringToFile(languageDir, languageContent, StandardCharsets.UTF_8);
                logger.info("Created config/language.json with default content");
            } else {
                logger.info("homeworkChecker directory already exists");

                // 检查 homeworkDatabase 目录是否存在，如果不存在则创建
                File homeworkDatabaseDir = new File(homeworkCheckerDir, "homeworkDatabase");
                if (!homeworkDatabaseDir.exists()) {
                    FileUtils.forceMkdir(homeworkDatabaseDir);
                    logger.info("Created missing homeworkDatabase directory");
                }

                // 检查 config.json 是否有所有必需的键
                checkConfigFile(homeworkCheckerDir);
                
                // 检查 config 目录是否存在，如果不存在则创建
                checkAndCreateConfigDir(homeworkCheckerDir);
                
                // 检查 initTemple.txt 是否存在，如果不存在则创建
                checkAndCreateInitTempleFile(homeworkCheckerDir);
                
                // 检查 config/language.json 是否存在，如果不存在则创建
                checkAndCreateLanguageFile(homeworkCheckerDir);
            }

            // 读取 config.json 并存入 Idf.userConfig
            loadUserConfig(homeworkCheckerDir);
            
            // 读取 initTemple.txt 并存入 Idf.initTemple
            loadInitTemple(homeworkCheckerDir);
            
            // 读取 config/language.json 并存入 Idf.userLanguage
            loadUserLanguage(homeworkCheckerDir);
        } catch (Exception e) {
            logger.error("Error initializing user directories", e);
        }
    }

    private void checkConfigFile(File homeworkCheckerDir) {
        try {
            File configFile = new File(homeworkCheckerDir, "config.json");

            if (!configFile.exists()) {
                // 如果 config.json 不存在，创建它
                String modelConfigContent = getResourceFileContent("/modelConfig.json");
                FileUtils.writeStringToFile(configFile, modelConfigContent, StandardCharsets.UTF_8);
                logger.info("Created missing config.json with default content");
                return;
            }

            // 读取两个配置文件
            String configContent = FileUtils.readFileToString(configFile, StandardCharsets.UTF_8);
            String modelConfigContent = getResourceFileContent("/modelConfig.json");

            // 解析为 JSON 对象
            JSONObject configJson = JSON.parseObject(configContent);
            JSONObject modelConfigJson = JSON.parseObject(modelConfigContent);

            // 检查 config.json 是否包含 modelConfig.json 中的所有键
            checkKeys(modelConfigJson, configJson, "");

            logger.info("Configuration file check completed");
        } catch (Exception e) {
            logger.error("Error checking config file", e);
        }
    }

    private void checkKeys(JSONObject modelJson, JSONObject targetJson, String path) {
        for (String key : modelJson.keySet()) {
            String currentPath = path.isEmpty() ? key : path + "." + key;

            if (!targetJson.containsKey(key)) {
                logger.warn("Missing key in config.json: {}", currentPath);
                continue;
            }

            // 如果值是嵌套对象，递归检查
            if (modelJson.get(key) instanceof JSONObject) {
                Object targetValue = targetJson.get(key);
                if (targetValue instanceof JSONObject) {
                    checkKeys((JSONObject) modelJson.get(key), (JSONObject) targetValue, currentPath);
                } else {
                    logger.warn("Expected object for key: {}", currentPath);
                }
            }
        }
    }

    private void loadUserConfig(File homeworkCheckerDir) {
        try {
            File configFile = new File(homeworkCheckerDir, "config.json");
            if (configFile.exists()) {
                String configContent = FileUtils.readFileToString(configFile, StandardCharsets.UTF_8);
                Idf.userConfig = JSON.parseObject(configContent);
                logger.info("Loaded user configuration into Idf.userConfig");
            } else {
                logger.warn("config.json not found when trying to load user configuration");
            }
        } catch (Exception e) {
            logger.error("Error loading user configuration", e);
        }
    }
    
    /**
     * 检查并创建 config 目录
     * @param homeworkCheckerDir homeworkChecker 目录
     */
    private void checkAndCreateConfigDir(File homeworkCheckerDir) {
        try {
            File configDir = new File(homeworkCheckerDir, "config");
            if (!configDir.exists()) {
                FileUtils.forceMkdir(configDir);
                logger.info("Created missing config directory");
            }
        } catch (Exception e) {
            logger.error("Error checking or creating config directory", e);
        }
    }
    
    /**
     * 检查并创建 initTemple.txt 文件
     * @param homeworkCheckerDir homeworkChecker 目录
     */
    private void checkAndCreateInitTempleFile(File homeworkCheckerDir) {
        try {
            File initTempleFile = new File(homeworkCheckerDir, "initTemple.txt");
            if (!initTempleFile.exists()) {
                String initTempleContent = getResourceFileContent("/initTemple.txt");
                FileUtils.writeStringToFile(initTempleFile, initTempleContent, StandardCharsets.UTF_8);
                logger.info("Created missing initTemple.txt with default content");
            }
        } catch (Exception e) {
            logger.error("Error checking or creating initTemple.txt", e);
        }
    }
    
    /**
     * 检查并创建 config/language.json 文件
     * @param homeworkCheckerDir homeworkChecker 目录
     */
    private void checkAndCreateLanguageFile(File homeworkCheckerDir) {
        try {
            File languageFile = new File(homeworkCheckerDir, "config/language.json");
            if (!languageFile.exists()) {
                String languageContent = getResourceFileContent("/language.json");
                FileUtils.writeStringToFile(languageFile, languageContent, StandardCharsets.UTF_8);
                logger.info("Created missing config/language.json with default content");
            }
        } catch (Exception e) {
            logger.error("Error checking or creating config/language.json", e);
        }
    }
    
    /**
     * 加载 initTemple.txt 内容到 Idf.initTemple
     * @param homeworkCheckerDir homeworkChecker 目录
     */
    private void loadInitTemple(File homeworkCheckerDir) {
        try {
            File initTempleFile = new File(homeworkCheckerDir, "initTemple.txt");
            if (initTempleFile.exists()) {
                Idf.initTemple = FileUtils.readFileToString(initTempleFile, StandardCharsets.UTF_8);
                logger.info("Loaded init temple content into Idf.initTemple");
            } else {
                logger.warn("initTemple.txt not found when trying to load init temple content");
            }
        } catch (Exception e) {
            logger.error("Error loading init temple content", e);
        }
    }
    
    /**
     * 加载 config/language.json 内容到 Idf.userLanguage
     * @param homeworkCheckerDir homeworkChecker 目录
     */
    private void loadUserLanguage(File homeworkCheckerDir) {
        try {
            File languageFile = new File(homeworkCheckerDir, "config/language.json");
            if (languageFile.exists()) {
                String languageContent = FileUtils.readFileToString(languageFile, StandardCharsets.UTF_8);
                Idf.userLanguage = JSON.parseObject(languageContent);
                logger.info("Loaded user language configuration into Idf.userLanguage");
            } else {
                logger.warn("config/language.json not found when trying to load user language configuration");
            }
        } catch (Exception e) {
            logger.error("Error loading user language configuration", e);
        }
    }
    
    /**
     * 从classpath中读取资源文件内容
     * @param resourcePath 资源文件路径
     * @return 文件内容字符串
     */
    private String getResourceFileContent(String resourcePath) throws IOException {
        try (InputStream inputStream = getClass().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IOException("Resource not found: " + resourcePath);
            }
            return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        }
    }
}