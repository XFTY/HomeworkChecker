package com.xfty.homeworkchecker.service.ui.mainPage;

import com.xfty.homeworkchecker.Entry;
import com.xfty.homeworkchecker.Idf;
import com.xfty.homeworkchecker.service.HomeworkDatabase;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * LockService — 锁定/解锁模块服务
 * <p>
 * 处理编辑区的锁定与解锁：更新锁图标、切换编辑状态、缓存作业内容并写入数据库。
 * </p>
 */
public class LockService {

    private static final Logger logger = LoggerFactory.getLogger(LockService.class);

    private final TextArea editMain;
    private final ImageView lockStatusImageView;
    private final Label lockStatusLabel;
    private final HomeworkDatabase homeworkDatabase;

    /**
     * @param editMain            编辑文本区域
     * @param lockStatusImageView 锁状态图标
     * @param lockStatusLabel     锁状态文字标签
     * @param homeworkDatabase    数据库操作实例
     */
    public LockService(TextArea editMain, ImageView lockStatusImageView,
                       Label lockStatusLabel, HomeworkDatabase homeworkDatabase) {
        this.editMain = editMain;
        this.lockStatusImageView = lockStatusImageView;
        this.lockStatusLabel = lockStatusLabel;
        this.homeworkDatabase = homeworkDatabase;
    }

    /**
     * 锁定编辑区：禁用编辑、更新图标为锁定态、缓存并保存当前内容
     */
    public void lock() {
        logger.info("Locking module");
        editMain.setEditable(false);
        lockStatusImageView.setImage(new Image(Objects.requireNonNull(Entry.class.getResourceAsStream("icon/lock/lock.png"))));
        lockStatusLabel.setText(Idf.userLanguageBundle.getString("main.locked"));
        logger.info("Module locked");

        Idf.homeworkContextCache = editMain.getText();
        logger.debug("Cached homework context length: {}", Idf.homeworkContextCache.length());

        homeworkDatabase.writeHomeworkContextByDay(Idf.homeworkContextCache);
        logger.debug("Homework context saved to database");

        Idf.isEditable = false;
        Idf.editableProperty.set(false);
        logger.debug("Editable state set to false");
    }

    /**
     * 解锁编辑区：启用编辑、请求焦点、更新图标为解锁态
     */
    public void unlock() {
        logger.info("Unlocking module");
        editMain.setEditable(true);
        editMain.requestFocus();
        lockStatusImageView.setImage(new Image(Objects.requireNonNull(Entry.class.getResourceAsStream("icon/lock/unlock.png"))));
        lockStatusLabel.setText(Idf.userLanguageBundle.getString("main.unlocked"));
        logger.info("Module unlocked and focus requested");

        Idf.isEditable = true;
        Idf.editableProperty.set(true);
        logger.debug("Editable state set to true");
    }
}
