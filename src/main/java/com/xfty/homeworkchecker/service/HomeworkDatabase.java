package com.xfty.homeworkchecker.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.xfty.homeworkchecker.Idf;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HomeworkDatabase {
    private static final Logger logger = LoggerFactory.getLogger(HomeworkDatabase.class);

    public String getTodayHomeworkContext() {
        return getHomeworkContext(Idf.year, Idf.month, Idf.day);
    }

    public String getHomeworkContext(String year, String month, String day) {
        // 构造日期文件名 (格式: YYYYMMDD)
        String dateFileName = year + month + day;
        return getHomeworkContextByFile(dateFileName);
    }

    /**
     * 通过文件名直接获取作业内容
     *
     * @param fileName 文件名
     * @return 作业内容
     */
    public String getHomeworkContextByFileName(String fileName) {
        return getHomeworkContextByFile(fileName);
    }

    private String getHomeworkContextByFile(String fileName) {
        try {
            File userDir = FileUtils.getUserDirectory();
            File homeworkCheckerDir = new File(userDir, "homeworkChecker");
            File homeworkDatabaseDir = new File(homeworkCheckerDir, "homeworkDatabase");

            File dateFile = new File(homeworkDatabaseDir, fileName);

            if (!dateFile.exists()) {
                logger.info("Homework file {} does not exist", fileName);
                return null;
            }

            String fileContent = FileUtils.readFileToString(dateFile, StandardCharsets.UTF_8);
            JSONObject jsonObject = JSON.parseObject(fileContent);

            String context = jsonObject.getString("context");
            String storedSHA256 = jsonObject.getString("dataSHA256");
            int dbVersion = jsonObject.getIntValue("databaseVersion", 1);

            if (dbVersion >= 2) {
                String warningsJson = jsonObject.getJSONArray("warnings") != null
                    ? jsonObject.getJSONArray("warnings").toJSONString() : "[]";
                String calculatedSHA256 = calculateCombinedSHA256(context, warningsJson);
                if (!calculatedSHA256.equals(storedSHA256)) {
                    logger.warn("File integrity check failed for V2 file {}", fileName);
                }
            } else {
                String calculatedSHA256 = calculateSHA256(context);
                if (!calculatedSHA256.equals(storedSHA256)) {
                    logger.warn("File integrity check failed for V1 file {}", fileName);
                }
            }

            return context;
        } catch (Exception e) {
            logger.error("Error reading homework context from file {}", fileName, e);
            return null;
        }
    }

    public void writeHomeworkContextByDay(String context) {
        try {
            File userDir = FileUtils.getUserDirectory();
            File homeworkCheckerDir = new File(userDir, "homeworkChecker");
            File homeworkDatabaseDir = new File(homeworkCheckerDir, "homeworkDatabase");

            String dateFileName = Idf.year + Idf.month + Idf.day;
            File dateFile = new File(homeworkDatabaseDir, dateFileName);

            JSONArray existingWarnings = null;
            if (dateFile.exists()) {
                String oldContent = FileUtils.readFileToString(dateFile, StandardCharsets.UTF_8);
                JSONObject oldJson = JSON.parseObject(oldContent);
                existingWarnings = oldJson.getJSONArray("warnings");
                FileUtils.write(dateFile, "", StandardCharsets.UTF_8);
            } else {
                FileUtils.touch(dateFile);
            }

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("databaseVersion", 2);
            jsonObject.put("context", context);

            if (existingWarnings != null && !existingWarnings.isEmpty()) {
                jsonObject.put("warnings", existingWarnings);
            } else {
                jsonObject.put("warnings", new JSONArray());
            }

            JSONObject dataDate = new JSONObject();
            dataDate.put("year", Idf.year);
            dataDate.put("month", Idf.month);
            dataDate.put("day", Idf.day);
            jsonObject.put("dataDate", dataDate);

            String warningsJson = jsonObject.getJSONArray("warnings").toJSONString();
            String combinedHash = calculateCombinedSHA256(context, warningsJson);
            jsonObject.put("dataSHA256", combinedHash);

            FileUtils.write(dateFile, jsonObject.toJSONString(), StandardCharsets.UTF_8);
            logger.info("Successfully wrote homework context to file: {}", dateFile.getAbsolutePath());
        } catch (IOException e) {
            logger.error("Error handling homework database file", e);
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle(Idf.userLanguageBundle.getString("homeworkdatabase.saveError"));
                alert.setHeaderText(Idf.userLanguageBundle.getString("homeworkdatabase.cannotSaveHomework"));
                alert.setContentText(Idf.userLanguageBundle.getString("homeworkdatabase.saveContentError") + e.getMessage());
                alert.showAndWait();
            });
        }
    }
    
    public void updateConfig(JSONObject jsonObject) {
        try {
            // 获取用户文档目录下的homeworkChecker目录
            File userDir = FileUtils.getUserDirectory();
            File homeworkCheckerDir = new File(userDir, "homeworkChecker");
            
            // 构造config.json文件路径
            File configFile = new File(homeworkCheckerDir, "config.json");
            
            // 将Idf.userConfig写入到config.json文件
            FileUtils.write(configFile, jsonObject.toJSONString(), StandardCharsets.UTF_8);
            logger.info("Successfully updated config.json file: {}", configFile.getAbsolutePath());
        } catch (IOException e) {
            logger.error("Error updating config.json file", e);
            // 在UI线程中显示错误警告
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle(Idf.userLanguageBundle.getString("homeworkdatabase.saveConfigError"));
                alert.setHeaderText(Idf.userLanguageBundle.getString("homeworkdatabase.unableToSaveConfig"));
                alert.setContentText(Idf.userLanguageBundle.getString("homeworkdatabase.errorSavingConfig") + e.getMessage());
                alert.showAndWait();
            });
        }
    }

    /**
     * 更改initTemple.txt文件的内容
     *
     * @param changeArgs 新的内容
     */
    public void changeInitTemple(String changeArgs) {
        try {
            // 获取用户文档目录下的homeworkChecker目录
            File userDir = FileUtils.getUserDirectory();
            File homeworkCheckerDir = new File(userDir, "homeworkChecker");

            // 构造initTemple.txt文件路径
            File initTempleFile = new File(homeworkCheckerDir, "initTemple.txt");

            // 将新内容写入initTemple.txt文件
            FileUtils.write(initTempleFile, changeArgs, StandardCharsets.UTF_8);
            logger.info("Successfully updated initTemple.txt file: {}", initTempleFile.getAbsolutePath());
            
            // 更新Idf.initTemple变量
            Idf.initTemple = changeArgs;
        } catch (IOException e) {
            logger.error("Error updating initTemple.txt file", e);
            // 在UI线程中显示错误警告
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle(Idf.userLanguageBundle.getString("homeworkdatabase.saveTemplateError"));
                alert.setHeaderText(Idf.userLanguageBundle.getString("homeworkdatabase.unableToSaveTemplate"));
                alert.setContentText(Idf.userLanguageBundle.getString("homeworkdatabase.errorSavingTemplate") + e.getMessage());
                alert.showAndWait();
            });
        }
    }

    /**
     * 更改config/language.json文件的内容
     *
     * @param changeArgs 新的内容
     */
    public void changeLanguage(JSONObject changeArgs) {
        try {
            // 获取用户文档目录下的homeworkChecker目录
            File userDir = FileUtils.getUserDirectory();
            File homeworkCheckerDir = new File(userDir, "homeworkChecker");

            // 构造config/language.json文件路径
            File languageFile = new File(homeworkCheckerDir, "config/language.json");

            // 将新内容写入language.json文件
            FileUtils.write(languageFile, changeArgs.toJSONString(), StandardCharsets.UTF_8);
            logger.info("Successfully updated config/language.json file: {}", languageFile.getAbsolutePath());
            
            // 更新Idf.userLanguage变量
            Idf.userLanguage = changeArgs;
        } catch (IOException e) {
            logger.error("Error updating config/language.json file", e);
            // 在UI线程中显示错误警告
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle(Idf.userLanguageBundle.getString("homeworkdatabase.saveLanguageError"));
                alert.setHeaderText(Idf.userLanguageBundle.getString("homeworkdatabase.unableToSaveLanguage"));
                alert.setContentText(Idf.userLanguageBundle.getString("homeworkdatabase.errorSavingLanguage") + e.getMessage());
                alert.showAndWait();
            });
        }
    }

    /**
     * 计算字符串的SHA-256哈希值
     *
     * @param input 输入字符串
     * @return SHA-256哈希值
     */
    public static String calculateSHA256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();

            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            logger.error("SHA-256 algorithm not available", e);
            return "";
        }
    }

    public static String calculateCombinedSHA256(String context, String warningsJson) {
        return calculateSHA256(context + "||warnings||" + warningsJson);
    }

    /**
     * 显示文件被篡改警告
     * 
     * @return 用户是否同意继续读取
     */
    private boolean showTamperWarning() {
        // 使用数组来存储用户的选择结果，因为需要在内部类中修改
        final boolean[] userConsents = {false};
        final boolean[] dialogClosed = {false};

        // 在JavaFX应用线程中创建并显示对话框
        javafx.application.Platform.runLater(() -> {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING);
            alert.setTitle(Idf.userLanguageBundle.getString("homeworkdatabase.fileDor"));
            alert.setHeaderText(Idf.userLanguageBundle.getString("homeworkdatabase.fileDor.title"));
            alert.setContentText(Idf.userLanguageBundle.getString("homeworkdatabase.fileDor.content"));

            // 添加确定和取消按钮
            javafx.scene.control.ButtonType continueButtonType = new javafx.scene.control.ButtonType(Idf.userLanguageBundle.getString("homeworkdatabase.fileDor.button.confirm"));
            javafx.scene.control.ButtonType cancelButtonType = new javafx.scene.control.ButtonType(Idf.userLanguageBundle.getString("homeworkdatabase.fileDor.button.cancel"), javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE);
            
            alert.getButtonTypes().setAll(continueButtonType, cancelButtonType);

            // 显示对话框并等待用户响应
            javafx.scene.control.ButtonType result = alert.showAndWait().orElse(cancelButtonType);

            // 根据用户选择设置结果
            userConsents[0] = result == continueButtonType;
            dialogClosed[0] = true;
            
            // 唤醒等待的线程
            synchronized (dialogClosed) {
                dialogClosed.notify();
            }
        });

        // 等待对话框关闭
        synchronized (dialogClosed) {
            while (!dialogClosed[0]) {
                try {
                    dialogClosed.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }

        return userConsents[0];
    }
}