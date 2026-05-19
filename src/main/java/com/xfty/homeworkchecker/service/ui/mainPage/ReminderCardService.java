package com.xfty.homeworkchecker.service.ui.mainPage;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.xfty.homeworkchecker.Idf;
import com.xfty.homeworkchecker.model.CardItem;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * ReminderCardService — 提醒卡片数据持久化服务
 * <p>
 * 负责卡片的 JSON 文件读写：当日卡片写入 YYYYMMDD 文件的 warnings 数组，
 * 持久化卡片同步至独立的 persistentCards.json。提供 SHA-256 完整性校验。
 * </p>
 */
public class ReminderCardService {
    private static final Logger logger = LoggerFactory.getLogger(ReminderCardService.class);
    /** 持久化卡片独立文件名 */
    private static final String PERSISTENT_FILE_NAME = "persistentCards.json";

    /**
     * 读取所有卡片（当日数据库中的 warnings + persistentCards.json 中的常驻卡片）
     *
     * @return 卡片列表
     */
    public List<CardItem> readCards() {
        List<CardItem> cards = new ArrayList<>();
        try {
            File homeworkDatabaseDir = getHomeworkDatabaseDir();
            String dateFileName = Idf.year + Idf.month + Idf.day;
            File dateFile = new File(homeworkDatabaseDir, dateFileName);

            if (dateFile.exists()) {
                String fileContent = FileUtils.readFileToString(dateFile, StandardCharsets.UTF_8);
                JSONObject jsonObject = JSON.parseObject(fileContent);

                int dbVersion = jsonObject.getIntValue("databaseVersion", 1);
                String context = jsonObject.getString("context");
                JSONArray warningsArray = jsonObject.getJSONArray("warnings");

                if (warningsArray != null && !warningsArray.isEmpty()) {
                    if (dbVersion >= 2) {
                        String warningsJson = warningsArray.toJSONString();
                        String storedSHA256 = jsonObject.getString("dataSHA256");
                        String calculatedSHA256 = calculateCombinedSHA256(context, warningsJson);
                        if (!calculatedSHA256.equals(storedSHA256)) {
                            logger.warn("Card data integrity check failed for file {}", dateFileName);
                        }
                    }

                    for (int i = 0; i < warningsArray.size(); i++) {
                        JSONObject cardObj = warningsArray.getJSONObject(i);
                        CardItem item = parseCardItem(cardObj);
                        cards.add(item);
                    }
                }
            }

            File persistentFile = new File(homeworkDatabaseDir, PERSISTENT_FILE_NAME);
            if (persistentFile.exists()) {
                String persistentContent = FileUtils.readFileToString(persistentFile, StandardCharsets.UTF_8);
                JSONArray persistentArray = JSON.parseArray(persistentContent);
                if (persistentArray != null) {
                    for (int i = 0; i < persistentArray.size(); i++) {
                        JSONObject cardObj = persistentArray.getJSONObject(i);
                        CardItem item = parseCardItem(cardObj);
                        boolean duplicate = false;
                        for (CardItem existing : cards) {
                            if (existing.getId().equals(item.getId())) {
                                duplicate = true;
                                break;
                            }
                        }
                        if (!duplicate) {
                            cards.add(item);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error reading cards from file", e);
        }
        return cards;
    }

    /**
     * 写入所有卡片：当日卡片写入数据库文件 warnings 数组 + SHA-256，
     * 持久化卡片同步到 persistentCards.json
     *
     * @param cards 完整的卡片列表
     */
    public void writeCards(List<CardItem> cards) {
        try {
            File homeworkDatabaseDir = getHomeworkDatabaseDir();
            String today = Idf.year + Idf.month + Idf.day;
            File dateFile = new File(homeworkDatabaseDir, today);

            JSONObject jsonObject;
            String context;

            if (dateFile.exists()) {
                String fileContent = FileUtils.readFileToString(dateFile, StandardCharsets.UTF_8);
                jsonObject = JSON.parseObject(fileContent);
                context = jsonObject.getString("context");
            } else {
                jsonObject = new JSONObject();
                context = "";
                jsonObject.put("databaseVersion", 2);
                jsonObject.put("context", "");
                JSONObject dataDate = new JSONObject();
                dataDate.put("year", Idf.year);
                dataDate.put("month", Idf.month);
                dataDate.put("day", Idf.day);
                jsonObject.put("dataDate", dataDate);
            }

            jsonObject.put("databaseVersion", 2);

            JSONArray warningsArray = new JSONArray();
            List<CardItem> persistentCards = new ArrayList<>();
            for (CardItem item : cards) {
                String cd = item.getCreatedDate();
                boolean belongsToday = (cd == null || cd.equals(today));
                if (belongsToday || item.isPersistent()) {
                    JSONObject cardObj = serializeCardItem(item);
                    warningsArray.add(cardObj);
                }
                if (item.isPersistent()) {
                    persistentCards.add(item);
                }
            }
            jsonObject.put("warnings", warningsArray);

            String warningsJson = warningsArray.toJSONString();
            String combinedHash = calculateCombinedSHA256(context, warningsJson);
            jsonObject.put("dataSHA256", combinedHash);

            FileUtils.write(dateFile, jsonObject.toJSONString(), StandardCharsets.UTF_8);
            logger.info("Successfully wrote {} cards to file {}", warningsArray.size(), dateFile.getAbsolutePath());

            syncPersistentCards(homeworkDatabaseDir, cards);
        } catch (IOException e) {
            logger.error("Error writing cards to file", e);
        }
    }

    /**
     * 将持久化卡片写入独立的 persistentCards.json 文件
     */
    private void syncPersistentCards(File homeworkDatabaseDir, List<CardItem> currentCards) {
        try {
            File persistentFile = new File(homeworkDatabaseDir, PERSISTENT_FILE_NAME);
            Map<String, CardItem> persistentMap = new HashMap<>();

            for (CardItem item : currentCards) {
                if (item.isPersistent()) {
                    persistentMap.put(item.getId(), item);
                }
            }

            JSONArray outArray = new JSONArray();
            for (CardItem item : persistentMap.values()) {
                outArray.add(serializeCardItem(item));
            }
            FileUtils.write(persistentFile, outArray.toJSONString(), StandardCharsets.UTF_8);
            logger.info("Synced {} persistent cards", outArray.size());
        } catch (IOException e) {
            logger.error("Error syncing persistent cards", e);
        }
    }

    /**
     * 将 JSONObject 反序列化为 CardItem 对象
     */
    private CardItem parseCardItem(JSONObject cardObj) {
        CardItem item = new CardItem();
        String id = cardObj.getString("id");
        item.setId(id != null ? id : UUID.randomUUID().toString());
        String sev = cardObj.getString("severity");
        if ("WARNING".equals(sev)) {
            item.setSeverity(CardItem.Severity.WARNING);
        } else if ("CRITICAL".equals(sev)) {
            item.setSeverity(CardItem.Severity.CRITICAL);
        } else {
            item.setSeverity(CardItem.Severity.INFO);
        }
        item.setTitle(cardObj.getString("title"));
        item.setContent(cardObj.getString("content"));
        item.setTimestamp(cardObj.getString("timestamp"));
        item.setCreatedDate(cardObj.getString("createdDate"));
        item.setPersistent(cardObj.getBooleanValue("persistent"));
        return item;
    }

    /**
     * 将 CardItem 序列化为 JSONObject
     */
    private JSONObject serializeCardItem(CardItem item) {
        JSONObject cardObj = new JSONObject();
        cardObj.put("id", item.getId());
        cardObj.put("severity", item.getSeverity().name());
        cardObj.put("title", item.getTitle() != null ? item.getTitle() : "");
        cardObj.put("content", item.getContent() != null ? item.getContent() : "");
        cardObj.put("timestamp", item.getTimestamp() != null ? item.getTimestamp() : "");
        cardObj.put("createdDate", item.getCreatedDate());
        cardObj.put("persistent", item.isPersistent());
        return cardObj;
    }

    /**
     * 获取 homeworkDatabase 目录的 File 对象
     */
    private File getHomeworkDatabaseDir() {
        File userDir = FileUtils.getUserDirectory();
        File homeworkCheckerDir = new File(userDir, "homeworkChecker");
        return new File(homeworkCheckerDir, "homeworkDatabase");
    }

    /**
     * 计算 context 和 warnings JSON 联合后的 SHA-256 校验值
     *
     * @param context      作业内容
     * @param warningsJson 卡片数据 JSON 字符串
     * @return SHA-256 十六进制字符串
     */
    public static String calculateCombinedSHA256(String context, String warningsJson) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String combined = context + "||warnings||" + warningsJson;
            byte[] hashBytes = digest.digest(combined.getBytes(StandardCharsets.UTF_8));
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

    /**
     * 生成当前日期字符串（yyyy-MM-dd 格式）
     *
     * @return 日期字符串
     */
    public static String generateTimestamp() {
        return LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    /**
     * 添加一张新卡片
     *
     * @param item 卡片对象
     */
    public void addCard(CardItem item) {
        List<CardItem> cards = readCards();
        cards.add(item);
        writeCards(cards);
    }

    /**
     * 更新指定索引的卡片
     *
     * @param index 卡片索引
     * @param item  新的卡片数据
     */
    public void updateCard(int index, CardItem item) {
        List<CardItem> cards = readCards();
        if (index >= 0 && index < cards.size()) {
            cards.set(index, item);
            writeCards(cards);
        }
    }

    /**
     * 删除指定索引的卡片
     *
     * @param index 卡片索引
     */
    public void deleteCard(int index) {
        List<CardItem> cards = readCards();
        if (index >= 0 && index < cards.size()) {
            cards.remove(index);
            writeCards(cards);
        }
    }
}
