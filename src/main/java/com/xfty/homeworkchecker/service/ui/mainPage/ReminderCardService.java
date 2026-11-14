package com.xfty.homeworkchecker.service.ui.mainPage;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.xfty.homeworkchecker.Idf;
import com.xfty.homeworkchecker.model.CardItem;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritablePixelFormat;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
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
 * ReminderCardService —— 提醒卡片数据持久化服务。
 * <p>
 * 卡片数据采用「双文件」存储：
 * <ul>
 *   <li><b>当日文件</b> {@code homeworkDatabase/YYYYMMDD}：JSON 对象，其中
 *       {@code warnings} 数组保存当天要展示的卡片，{@code dataSHA256} 保存
 *       「作业正文 + 卡片数组」的联合校验值；</li>
 *   <li><b>常驻文件</b> {@code homeworkDatabase/persistentCards.json}：JSON 数组，
 *       保存所有标记为 persistent 的卡片，使它们跨日期保留。</li>
 * </ul>
 * 读取时先读当日 {@code warnings}，再把常驻文件中「id 不重复」的卡片追加到末尾。
 * 写入时按卡片创建日期与 persistent 标记决定它进入当日文件还是常驻文件。
 * 本类不持有 UI 状态，所有方法都直接操作磁盘，因此调用方每次修改后需重新读取。
 * </p>
 */
public class ReminderCardService {
    private static final Logger logger = LoggerFactory.getLogger(ReminderCardService.class);
    /** 持久化卡片独立文件名 */
    private static final String PERSISTENT_FILE_NAME = "persistentCards.json";
    /** 当前数据库结构版本；>= 2 表示带 SHA-256 校验。 */
    private static final int DATABASE_VERSION = 2;
    /** 图片卡片未显式设置宽度时的默认宽度（像素）。 */
    private static final double DEFAULT_IMAGE_WIDTH = 200.0;

    /**
     * 读取所有卡片：当日数据库中的 {@code warnings} + {@code persistentCards.json} 中的常驻卡片。
     * <p>
     * 处理步骤：
     * <ol>
     *   <li>读取当日文件，若数据库版本 >= 2 则先校验 SHA-256；校验失败只记录警告，不阻断读取；</li>
     *   <li>把 {@code warnings} 逐条解析为 {@link CardItem}；</li>
     *   <li>读取常驻文件，按 {@code id} 去重后追加（避免同一张常驻卡片在当日文件中已存在时重复显示）。</li>
     * </ol>
     * 任何 IO/解析异常都会被捕获并返回当前已读取到的卡片，保证界面不因坏文件崩溃。
     * </p>
     *
     * @return 合并后的卡片列表（当日卡片在前，常驻卡片在后）
     */
    public List<CardItem> readCards() {
        List<CardItem> cards = new ArrayList<>();
        try {
            File homeworkDatabaseDir = getHomeworkDatabaseDir();
            // 当日文件名就是「年月日」拼接，例如 20261114
            String dateFileName = Idf.year + Idf.month + Idf.day;
            File dateFile = new File(homeworkDatabaseDir, dateFileName);

            if (dateFile.exists()) {
                String fileContent = FileUtils.readFileToString(dateFile, StandardCharsets.UTF_8);
                JSONObject jsonObject = JSON.parseObject(fileContent);

                // 旧版本文件可能没有 databaseVersion，缺省按 1 处理（不校验）
                int dbVersion = jsonObject.getIntValue("databaseVersion", 1);
                String context = jsonObject.getString("context");
                JSONArray warningsArray = jsonObject.getJSONArray("warnings");

                if (warningsArray != null && !warningsArray.isEmpty()) {
                    if (dbVersion >= 2) {
                        // 重新计算「作业正文 + 卡片 JSON」的联合哈希，与文件中保存值比对
                        String warningsJson = warningsArray.toJSONString();
                        String storedSHA256 = jsonObject.getString("dataSHA256");
                        String calculatedSHA256 = calculateCombinedSHA256(context, warningsJson);
                        if (!calculatedSHA256.equals(storedSHA256)) {
                            // 校验失败仅告警，仍继续读取，避免用户数据完全不可见
                            logger.warn("Card data integrity check failed for file {}", dateFileName);
                        }
                    }

                    // 逐条反序列化当日卡片
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
                        // 常驻卡片可能同时存在于当日文件中，按 id 去重
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
            // 读取失败时返回已成功解析的部分，并在日志中记录原因
            logger.error("Error reading cards from file", e);
        }
        return cards;
    }

    /**
     * 写入所有卡片：当日卡片写入数据库文件 {@code warnings} 数组 + SHA-256，
     * 持久化卡片同步到 {@code persistentCards.json}。
     * <p>
     * 日期归属规则：
     * <ul>
     *   <li>卡片的 {@code createdDate} 为空或等于今天 → 属于今天，写入当日文件；</li>
     *   <li>{@code createdDate} 是旧日期且不是持久化卡片 → 把日期改写为今天后再写入
     *       （相当于「旧卡顺延到今天」）；</li>
     *   <li>持久化卡片无论日期都写入当日文件，同时同步到常驻文件。</li>
     * </ul>
     * 写入当日文件时会保留其中的 {@code context}（作业正文），只替换 {@code warnings}
     * 并重算联合哈希，避免误覆盖作业内容。
     * </p>
     *
     * @param cards 完整的卡片列表（会按上述规则筛选后写入）
     */
    public void writeCards(List<CardItem> cards) {
        try {
            File homeworkDatabaseDir = getHomeworkDatabaseDir();
            String today = Idf.year + Idf.month + Idf.day;
            File dateFile = new File(homeworkDatabaseDir, today);

            JSONObject jsonObject;
            String context;

            if (dateFile.exists()) {
                // 已存在：复用原 JSON（保留 context、dataDate 等字段），只更新卡片部分
                String fileContent = FileUtils.readFileToString(dateFile, StandardCharsets.UTF_8);
                jsonObject = JSON.parseObject(fileContent);
                context = jsonObject.getString("context");
            } else {
                // 不存在：创建最小结构，context 为空，稍后由作业保存流程补齐
                jsonObject = new JSONObject();
                context = "";
                jsonObject.put("databaseVersion", DATABASE_VERSION);
                jsonObject.put("context", "");
                JSONObject dataDate = new JSONObject();
                dataDate.put("year", Idf.year);
                dataDate.put("month", Idf.month);
                dataDate.put("day", Idf.day);
                jsonObject.put("dataDate", dataDate);
            }

            // 始终升级到当前数据库版本，便于后续启用哈希校验
            jsonObject.put("databaseVersion", DATABASE_VERSION);

            JSONArray warningsArray = new JSONArray();
            List<CardItem> persistentCards = new ArrayList<>();
            for (CardItem item : cards) {
                String cd = item.getCreatedDate();
                boolean belongsToday = (cd == null || cd.equals(today));

                // 旧日期且非常驻的卡片：直接改写成今天，使其继续出现在今日列表中
                if (!belongsToday && !item.isPersistent() && cd != null) {
                    item.setCreatedDate(today);
                    belongsToday = true;
                }

                // 今日卡片与常驻卡片都进入当日文件
                if (belongsToday || item.isPersistent()) {
                    JSONObject cardObj = serializeCardItem(item);
                    warningsArray.add(cardObj);
                }
                // 常驻卡片额外收集一份，稍后写入独立文件
                if (item.isPersistent()) {
                    persistentCards.add(item);
                }
            }
            jsonObject.put("warnings", warningsArray);

            // 重新计算「作业正文 + 卡片数组」的联合哈希，保证数据完整性可校验
            String warningsJson = warningsArray.toJSONString();
            String combinedHash = calculateCombinedSHA256(context, warningsJson);
            jsonObject.put("dataSHA256", combinedHash);

            FileUtils.write(dateFile, jsonObject.toJSONString(), StandardCharsets.UTF_8);
            logger.info("Successfully wrote {} cards to file {}", warningsArray.size(), dateFile.getAbsolutePath());

            // 同步常驻卡片到独立文件
            syncPersistentCards(homeworkDatabaseDir, cards);
        } catch (IOException e) {
            logger.error("Error writing cards to file", e);
        }
    }

    /**
     * 将持久化卡片写入独立的 {@code persistentCards.json} 文件。
     * <p>
     * 使用 {@code Map<id, CardItem>} 而不是直接列表，是为了在传入列表中存在重复 id 时
     * 只保留最后一次出现的卡片，避免常驻文件出现重复项。
     * </p>
     *
     * @param homeworkDatabaseDir 数据库目录
     * @param currentCards        当前完整卡片列表
     */
    private void syncPersistentCards(File homeworkDatabaseDir, List<CardItem> currentCards) {
        try {
            File persistentFile = new File(homeworkDatabaseDir, PERSISTENT_FILE_NAME);
            Map<String, CardItem> persistentMap = new HashMap<>();

            // 以 id 为键收集所有常驻卡片
            for (CardItem item : currentCards) {
                if (item.isPersistent()) {
                    persistentMap.put(item.getId(), item);
                }
            }

            // 序列化为 JSON 数组后整体覆盖写入
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
     * 将 JSONObject 反序列化为 CardItem 对象。
     * <p>
     * 对缺失字段做了兜底：
     * <ul>
     *   <li>缺失 {@code id} → 生成新 UUID；</li>
     *   <li>未知严重性 → 按 INFO 处理；</li>
     *   <li>缺失 {@code imageWidth} → 使用默认宽度 200px。</li>
     * </ul>
     * </p>
     *
     * @param cardObj 卡片 JSON 对象
     * @return 解析后的卡片数据
     */
    private CardItem parseCardItem(JSONObject cardObj) {
        CardItem item = new CardItem();
        String id = cardObj.getString("id");
        item.setId(id != null ? id : UUID.randomUUID().toString());
        // 严重性字符串 → 枚举，未知值一律当作 INFO，避免旧数据导致解析失败
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
        item.setImagePath(cardObj.getString("imagePath"));
        Double iw = cardObj.getDouble("imageWidth");
        item.setImageWidth(iw != null ? iw : DEFAULT_IMAGE_WIDTH);
        item.setTimestamp(cardObj.getString("timestamp"));
        item.setCreatedDate(cardObj.getString("createdDate"));
        // 缺失 persistent 字段时按 false 处理
        item.setPersistent(cardObj.getBooleanValue("persistent"));
        return item;
    }

    /**
     * 将 CardItem 序列化为 JSONObject。
     * <p>
     * 对 null 文本字段写入空字符串，保证 JSON 结构稳定、便于后续读取。
     * </p>
     *
     * @param item 卡片数据
     * @return 可直接存入 JSON 数组的对象
     */
    private JSONObject serializeCardItem(CardItem item) {
        JSONObject cardObj = new JSONObject();
        cardObj.put("id", item.getId());
        cardObj.put("severity", item.getSeverity().name());
        cardObj.put("title", item.getTitle() != null ? item.getTitle() : "");
        cardObj.put("content", item.getContent() != null ? item.getContent() : "");
        cardObj.put("imagePath", item.getImagePath());
        cardObj.put("imageWidth", item.getImageWidth());
        cardObj.put("timestamp", item.getTimestamp() != null ? item.getTimestamp() : "");
        cardObj.put("createdDate", item.getCreatedDate());
        cardObj.put("persistent", item.isPersistent());
        return cardObj;
    }

    /**
     * 获取作业数据库目录：{@code <用户目录>/homeworkChecker/homeworkDatabase}。
     *
     * @return homeworkDatabase 目录的 File 对象（不保证已存在）
     */
    private File getHomeworkDatabaseDir() {
        File userDir = FileUtils.getUserDirectory();
        File homeworkCheckerDir = new File(userDir, "homeworkChecker");
        return new File(homeworkCheckerDir, "homeworkDatabase");
    }

    /**
     * 计算作业正文与卡片数组联合后的 SHA-256 校验值。
     * <p>
     * 拼接格式固定为 {@code context + "||warnings||" + warningsJson}，
     * 分隔符用于避免正文与 JSON 边界被意外混淆。返回小写十六进制字符串。
     * </p>
     *
     * @param context      作业内容
     * @param warningsJson 卡片数据 JSON 字符串
     * @return SHA-256 十六进制字符串；算法不可用时返回空字符串
     */
    public static String calculateCombinedSHA256(String context, String warningsJson) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            // 用固定分隔符隔开正文和卡片 JSON，防止不同内容的拼接结果发生碰撞
            String combined = context + "||warnings||" + warningsJson;
            byte[] hashBytes = digest.digest(combined.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            // 每个字节转两位十六进制（不足补前导 0）
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
     * 生成当前日期字符串（{@code yyyy-MM-dd} 格式），用作卡片显示时间戳。
     *
     * @return 日期字符串
     */
    public static String generateTimestamp() {
        return LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    /**
     * 将 JavaFX Image 保存为 PNG 文件到 {@code homeworkDatabase/images/} 目录。
     * <p>
     * 由于 JavaFX 的 {@link Image} 不能直接交给 ImageIO，这里通过 {@link PixelReader}
     * 逐像素读取 ARGB 值并复制到 {@link BufferedImage}，再写出 PNG。
     * 文件名使用 UUID，避免并发或重复粘贴时互相覆盖。
     * </p>
     *
     * @param image 要保存的图片
     * @return 相对路径（如 {@code "images/uuid.png"}），失败返回 null
     */
    public String saveImageToDisk(Image image) {
        try {
            File imageDir = new File(getHomeworkDatabaseDir(), "images");
            // 目录不存在时递归创建
            if (!imageDir.exists()) {
                imageDir.mkdirs();
            }

            // 使用 UUID 作为文件名，保证唯一
            String fileName = UUID.randomUUID().toString() + ".png";
            File imageFile = new File(imageDir, fileName);

            int width = (int) image.getWidth();
            int height = (int) image.getHeight();
            PixelReader reader = image.getPixelReader();

            // 以 ARGB 格式创建 BufferedImage，保留透明通道
            BufferedImage bImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bImage.setRGB(x, y, reader.getArgb(x, y));
                }
            }

            ImageIO.write(bImage, "png", imageFile);
            logger.info("Image saved: {}", imageFile.getAbsolutePath());
            // 返回相对 homeworkDatabase 的路径，便于数据库迁移/移动
            return "images/" + fileName;
        } catch (Exception e) {
            logger.error("Error saving image to disk", e);
            return null;
        }
    }

    /**
     * 删除磁盘上指定路径的图片文件。
     * <p>
     * 仅删除文件，不修改卡片数据；通常由「删除图片卡片」或「清空今日图片」流程调用。
     * 路径为空或文件不存在时静默返回。
     * </p>
     *
     * @param imagePath 相对路径（如 {@code "images/uuid.png"}）
     */
    public void deleteImageFile(String imagePath) {
        // 空路径直接忽略，避免删除数据库目录本身
        if (imagePath == null || imagePath.isEmpty()) return;
        try {
            File imageFile = new File(getHomeworkDatabaseDir(), imagePath);
            if (imageFile.exists() && imageFile.delete()) {
                logger.info("Deleted image file: {}", imageFile.getAbsolutePath());
            }
        } catch (Exception e) {
            logger.error("Error deleting image file: {}", imagePath, e);
        }
    }

    /**
     * 根据相对路径加载图片，用于卡片展示。
     *
     * @param imagePath 相对路径（如 {@code "images/uuid.png"}）
     * @return JavaFX Image 对象；路径为空、文件不存在或加载失败时返回 null
     */
    public Image loadImageForCard(String imagePath) {
        if (imagePath == null || imagePath.isEmpty()) return null;
        try {
            File imageFile = new File(getHomeworkDatabaseDir(), imagePath);
            if (!imageFile.exists()) {
                logger.warn("Image file not found: {}", imageFile.getAbsolutePath());
                return null;
            }
            return new Image(imageFile.toURI().toString());
        } catch (Exception e) {
            logger.error("Error loading image from path: {}", imagePath, e);
            return null;
        }
    }

    /**
     * 在列表末尾添加一张新卡片。
     * <p>
     * 采用「读取完整列表 → 追加 → 整体写回」的方式，
     * 因此列表中的其他卡片也会随之重新落盘。
     * </p>
     *
     * @param item 卡片对象
     */
    public void addCard(CardItem item) {
        List<CardItem> cards = readCards();
        cards.add(item);
        writeCards(cards);
    }

    /**
     * 在列表最上方添加一张新卡片（用于新建警示卡片时置顶显示）。
     * <p>
     * 与 {@link #addCard(CardItem)} 一样采用「读取完整列表 → 插入 → 整体写回」，
     * 区别是插入到下标 0，使新卡片在界面和持久化数据中都排在最前面。
     * </p>
     *
     * @param item 卡片对象
     */
    public void addCardAtTop(CardItem item) {
        List<CardItem> cards = readCards();
        // 插入到列表头部：writeCards 按列表顺序落盘，因此重启后仍保持置顶
        cards.add(0, item);
        writeCards(cards);
    }

    /**
     * 用新数据替换指定下标的卡片。
     * <p>
     * 下标越界时静默忽略，不抛异常，方便 UI 层在列表变化后安全调用。
     * </p>
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
     * 删除指定下标的卡片。
     * <p>
     * 只从数据中移除；若该卡片带图片，调用方需额外调用
     * {@link #deleteImageFile(String)} 清理图片文件。
     * </p>
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

    /**
     * 将卡片从 {@code fromIndex} 移动到 {@code toIndex}（拖拽排序）。
     * <p>
     * 先把卡片从原位置移除，再插入目标位置；两个下标都做越界检查。
     * 若两者相同则直接返回，避免无意义的写盘。
     * </p>
     *
     * @param fromIndex 源位置
     * @param toIndex   目标位置
     */
    public void moveCard(int fromIndex, int toIndex) {
        if (fromIndex == toIndex) return;
        List<CardItem> cards = readCards();
        if (fromIndex >= 0 && fromIndex < cards.size()
            && toIndex >= 0 && toIndex < cards.size()) {
            CardItem item = cards.remove(fromIndex);
            cards.add(toIndex, item);
            writeCards(cards);
        }
    }
}
