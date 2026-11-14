package com.xfty.homeworkchecker.service.ui.mainPage;

import com.xfty.homeworkchecker.Idf;
import com.xfty.homeworkchecker.model.CardItem;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.Background;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * ScreenshotService —— 「一键截屏」服务。
 * <p>
 * 不使用系统截屏，而是把主编辑区的文字和图片卡片重新绘制到 JavaFX Canvas 上，
 * 再转成图片放入系统剪贴板。这样即使窗口被遮挡也能得到完整内容，且输出比例可控。
 * 输出内容自上而下依次为：
 * <ol>
 *   <li>顶部日期栏（深色背景）；</li>
 *   <li>作业正文（按编辑区宽度自动换行）；</li>
 *   <li>所有图片卡片（每张图片前带「图片 N」分隔条）。</li>
 * </ol>
 * 截图成功后按钮播放绿色闪烁动画作为反馈，失败则弹出提示框。
 * </p>
 */
public class ScreenshotService {

    private static final Logger logger = LoggerFactory.getLogger(ScreenshotService.class);

    /** 主编辑区，截图内容来源。 */
    private final TextArea editMain;
    /** 截屏按钮，成功/失败动画与提示的载体。 */
    private final Button screenShotButton;
    /** 卡片服务，用于读取图片卡片和加载图片文件。 */
    private final ReminderCardService reminderCardService;
    /** 是否正在播放成功动画，防止重复触发动画。 */
    private boolean animating;

    /**
     * 构造截屏服务。
     *
     * @param editMain            主编辑区
     * @param screenShotButton    截屏按钮
     * @param reminderCardService 卡片服务
     */
    public ScreenshotService(TextArea editMain, Button screenShotButton,
                             ReminderCardService reminderCardService) {
        this.editMain = editMain;
        this.screenShotButton = screenShotButton;
        this.reminderCardService = reminderCardService;
    }

    /**
     * 执行截屏：绘制日期栏 + 正文 + 图片卡片，并写入系统剪贴板。
     * <p>
     * 绘制分两阶段：先画「日期栏 + 正文」得到基础图，再在基础图下方追加图片卡片。
     * 之所以分两阶段，是因为图片高度需要等加载完才知道，无法预先确定画布总高度。
     * 任何异常都会被捕获并弹出失败提示，不影响主界面。
     * </p>
     */
    public void takeScreenshot() {
        logger.info("Taking screenshot");

        try {
            // 截图内容直接取自编辑区当前文本，而不是屏幕像素
            String textContent = editMain.getText();
            logger.debug("Text content length: {}", textContent.length());

            // 日期栏文案：yyyy年MM月dd日 星期X
            LocalDate today = LocalDate.now();
            String dateText = today.getYear() + "\u5E74"
                + String.format("%02d", today.getMonthValue()) + "\u6708"
                + String.format("%02d", today.getDayOfMonth()) + "\u65E5 "
                + today.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.CHINESE);

            // 以 2 倍分辨率绘制，保证截图在截图缩放/高分屏下依然清晰
            final double renderScale = 2.0;
            // 画布宽度跟随编辑区宽度，并按缩放倍数放大
            int imageWidth = (int) (editMain.getWidth() * renderScale);
            // 顶部日期栏高度 52 逻辑像素
            int headerHeight = (int) (52 * renderScale);
            // 正文左右/上下内边距 12 逻辑像素
            int padding = (int) (12 * renderScale);
            // 正文可用宽度，至少保留 1 像素防止除零
            int contentWidth = Math.max(imageWidth - padding * 2, 1);

            // 正文字体与日期字体都按缩放倍数放大，保证绘制比例一致
            Font contentFont = new Font(editMain.getFont().getFamily(), editMain.getFont().getSize() * renderScale);
            Font dateFont = new Font(16 * renderScale);

            // 按可绘制宽度对正文手动换行（Canvas 不会自动换行）
            List<String> wrappedLines = wrapText(textContent, contentFont, contentWidth);
            if (wrappedLines.isEmpty()) {
                wrappedLines.add("");
            }
            double lineHeight = computeLineHeight(contentFont);
            // 正文区域高度 = 行数 × 行高 + 上下内边距
            int contentAreaHeight = (int) (wrappedLines.size() * lineHeight + padding * 2);

            // 基础图总高度 = 日期栏 + 正文区
            int totalHeight = headerHeight + contentAreaHeight;

            Canvas canvas = new Canvas(imageWidth, totalHeight);
            GraphicsContext gc = canvas.getGraphicsContext2D();

            // 日期栏背景（#1e1e1e）
            gc.setFill(Color.web("#1e1e1e"));
            gc.fillRect(0, 0, imageWidth, headerHeight);

            // 日期栏底部 1 逻辑像素的分隔线（#3a3a3a）
            gc.setStroke(Color.web("#3a3a3a"));
            gc.setLineWidth(renderScale);
            gc.strokeLine(0, headerHeight - renderScale / 2, imageWidth, headerHeight - renderScale / 2);

            // 日期文字：水平左对齐 16 逻辑像素，垂直方向用 0.85 行高做视觉居中修正
            gc.setFont(dateFont);
            Text measureText = new Text(dateText);
            measureText.setFont(dateFont);
            double textHeight = measureText.getLayoutBounds().getHeight();
            gc.setFill(Color.WHITE);
            gc.fillText(dateText, 16 * renderScale, (headerHeight - textHeight) / 2 + textHeight * 0.85);

            // 正文背景（#2d2d2d）
            gc.setFill(Color.web("#2d2d2d"));
            gc.fillRect(0, headerHeight, imageWidth, contentAreaHeight);

            // 逐行绘制正文；0.85 行高同样是基线位置修正
            gc.setFill(Color.WHITE);
            gc.setFont(contentFont);
            double y = headerHeight + padding + lineHeight * 0.85;
            for (String line : wrappedLines) {
                if (!line.isEmpty()) {
                    gc.fillText(line, padding, y);
                }
                y += lineHeight;
            }

            // 把基础画布快照成图片，后续再在它下面拼接图片卡片
            WritableImage resultImage = canvas.snapshot(null, null);
            logger.debug("Composite image created with text content, dimensions: {}x{}",
                resultImage.getWidth(), resultImage.getHeight());

            // 收集所有图片卡片（按列表顺序）
            List<CardItem> cards = reminderCardService.readCards();
            List<CardItem> imageCards = new ArrayList<>();
            for (CardItem card : cards) {
                if (card.getImagePath() != null && !card.getImagePath().isEmpty()) {
                    imageCards.add(card);
                }
            }

            if (!imageCards.isEmpty()) {
                // 每张图片上方分隔条高度 40 逻辑像素，图片之间额外留 4 逻辑像素间距
                int separatorHeight = (int) (40 * renderScale);
                int imageSpacing = (int) (4 * renderScale);

                List<Image> loadedImages = new ArrayList<>();
                List<double[]> imageDims = new ArrayList<>();
                int imagesTotalHeight = 0;
                for (CardItem card : imageCards) {
                    Image img = reminderCardService.loadImageForCard(card.getImagePath());
                    if (img != null) {
                        // 先按逻辑像素计算：图片宽度不能超过正文可用宽度，超出则等比缩小
                        double imgW = img.getWidth();
                        double imgH = img.getHeight();
                        double logicalContentWidth = contentWidth / renderScale;
                        if (imgW > logicalContentWidth) {
                            double scale = logicalContentWidth / imgW;
                            imgW = logicalContentWidth;
                            imgH *= scale;
                        }
                        // 再换算成实际绘制像素（乘渲染倍数）
                        imgW *= renderScale;
                        imgH *= renderScale;
                        loadedImages.add(img);
                        imageDims.add(new double[]{imgW, imgH});
                        imagesTotalHeight += separatorHeight + (int) imgH + imageSpacing;
                    }
                }

                if (!loadedImages.isEmpty()) {
                    // 新建一张更高的画布：上方原样贴基础图，下方逐张绘制图片
                    int finalTotalHeight = totalHeight + imagesTotalHeight;
                    Canvas finalCanvas = new Canvas(imageWidth, finalTotalHeight);
                    GraphicsContext finalGc = finalCanvas.getGraphicsContext2D();

                    // 先把「日期栏 + 正文」基础图贴到顶部
                    finalGc.drawImage(resultImage, 0, 0);

                    int currentY = totalHeight;
                    int imgIndex = 1;

                    for (int i = 0; i < loadedImages.size(); i++) {
                        Image img = loadedImages.get(i);
                        double[] dims = imageDims.get(i);
                        double imgW = dims[0];
                        double imgH = dims[1];

                        // 分隔条背景（#252525），顶部一条 1 逻辑像素高光（#555555）
                        finalGc.setFill(Color.web("#252525"));
                        finalGc.fillRect(0, currentY, imageWidth, separatorHeight);

                        finalGc.setStroke(Color.web("#555555"));
                        finalGc.setLineWidth(renderScale);
                        finalGc.strokeLine(0, currentY + renderScale / 2, imageWidth, currentY + renderScale / 2);

                        // 分隔条文字：「图片 N」，浅灰色，垂直居中
                        String labelText = "\u56FE\u7247 " + imgIndex;
                        finalGc.setFont(contentFont);
                        Text labelMeasure = new Text(labelText);
                        labelMeasure.setFont(contentFont);
                        double labelH = labelMeasure.getLayoutBounds().getHeight();
                        finalGc.setFill(Color.web("#cccccc"));
                        finalGc.fillText(labelText, padding,
                            currentY + (separatorHeight - labelH) / 2 + labelH * 0.85);

                        currentY += separatorHeight;

                        // 图片在正文宽度内水平居中绘制
                        double imgX = padding + (contentWidth - imgW) / 2;
                        finalGc.drawImage(img, imgX, currentY, imgW, imgH);

                        currentY += (int) imgH + imageSpacing;
                        imgIndex++;
                    }

                    resultImage = finalCanvas.snapshot(null, null);
                    logger.debug("Images appended to screenshot, final dimensions: {}x{}",
                        resultImage.getWidth(), resultImage.getHeight());
                }
            }

            // 把最终图片放入系统剪贴板，用户可直接粘贴到聊天工具/文档
            Clipboard clipboard = Clipboard.getSystemClipboard();
            logger.debug("Getting system clipboard");

            ClipboardContent content = new ClipboardContent();
            content.putImage(resultImage);
            logger.debug("Image content added to clipboard");

            clipboard.setContent(content);
            logger.info("Screenshot saved to clipboard with date overlay and text content");

            // 成功反馈：按钮绿色闪烁动画
            animateButtonSuccess();
            logger.debug("Screenshot success animation shown");

        } catch (Exception e) {
            logger.error("Failed to save screenshot", e);
            showFailureAlert();
            logger.debug("Screenshot failure alert shown");
        }

        logger.debug("Screenshot operation completed");
    }

    /**
     * 按最大绘制宽度对文本手动换行（Canvas 的 fillText 不会自动换行）。
     * <p>
     * 处理规则：
     * <ul>
     *   <li>先按 {@code \n} 拆分成段落，空段落保留为空行；</li>
     *   <li>制表符统一替换为 4 个空格，保证宽度计算稳定；</li>
     *   <li>逐字符累加测量宽度，超过 {@code maxWidth} 就另起一行。</li>
     * </ul>
     * 逐字符而不是按单词换行，是为了兼容中文等没有空格分隔的语言。
     * </p>
     *
     * @param text     原始文本
     * @param font     绘制字体
     * @param maxWidth 单行最大宽度（像素）
     * @return 换行后的每一行文本
     */
    private List<String> wrapText(String text, Font font, double maxWidth) {
        List<String> lines = new ArrayList<>();
        Text measurer = new Text();
        measurer.setFont(font);
        // -1 保证末尾的空行也会被保留
        for (String paragraph : text.split("\n", -1)) {
            if (paragraph.isEmpty()) {
                lines.add("");
                continue;
            }
            // 制表符按 4 个空格宽度处理
            String expanded = paragraph.replace("\t", "    ");
            StringBuilder currentLine = new StringBuilder();
            for (int i = 0; i < expanded.length(); i++) {
                String testStr = currentLine.toString() + expanded.charAt(i);
                measurer.setText(testStr);
                // 加上当前字符后超宽，且当前行已有内容 → 先提交当前行，把该字符放到新行
                if (measurer.getLayoutBounds().getWidth() > maxWidth && !currentLine.isEmpty()) {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder(String.valueOf(expanded.charAt(i)));
                } else {
                    currentLine.append(expanded.charAt(i));
                }
            }
            if (!currentLine.isEmpty()) {
                lines.add(currentLine.toString());
            }
        }
        return lines;
    }

    /**
     * 计算一行文字的行高。
     * <p>
     * 在字体实际高度基础上额外加 4 像素行距，避免相邻两行贴在一起。
     * </p>
     *
     * @param font 绘制字体
     * @return 行高（像素）
     */
    private double computeLineHeight(Font font) {
        Text t = new Text("A");
        t.setFont(font);
        return t.getLayoutBounds().getHeight() + 4;
    }

    /**
     * 播放截图成功动画：按钮文字切换为「已复制」，背景绿色闪烁 3 次后恢复原样。
     * <p>
     * 通过一个 {@link ObjectProperty} 承载颜色，再用 Timeline 驱动它做插值，
     * 监听器负责把当前颜色转成 CSS 应用到按钮上。动画期间 {@link #animating} 为 true，
     * 防止重复点击导致多个动画叠加。
     * </p>
     */
    private void animateButtonSuccess() {
        // 动画未结束时忽略再次触发
        if (animating) return;
        animating = true;

        // 记录原始文字和样式，动画结束后恢复
        String originalText = screenShotButton.getText();
        String originalStyle = screenShotButton.getStyle();

        // 浅绿（闪烁高亮）与深绿（闪烁暗部）
        Color lightGreen = Color.web("#66bb6a");
        Color darkGreen = Color.web("#1b5e20");

        String successText = Idf.userLanguageBundle.getString("mainpage.snapshot.success.button");
        screenShotButton.setText(successText);

        // 用一个颜色属性驱动动画；监听器实时把颜色同步到按钮样式
        ObjectProperty<Color> bgColorProp = new SimpleObjectProperty<>(getButtonBackgroundColor());
        bgColorProp.addListener((obs, oldColor, newColor) -> {
            String webColor = String.format("#%02x%02x%02x",
                (int) (newColor.getRed() * 255),
                (int) (newColor.getGreen() * 255),
                (int) (newColor.getBlue() * 255));
            screenShotButton.setStyle("-fx-background-color: " + webColor + "; -fx-text-fill: white;");
        });

        // 时间参数（毫秒）：300ms 淡入，闪烁总时长 6000ms，300ms 淡出
        double fadeIn = 300;
        double blinkTotal = 6000;
        double fadeOut = 300;

        Timeline timeline = new Timeline();

        // 第一段：从原始背景色渐变到浅绿
        timeline.getKeyFrames().setAll(
            new KeyFrame(Duration.ZERO,
                new KeyValue(bgColorProp, getButtonBackgroundColor(), Interpolator.LINEAR)),
            new KeyFrame(Duration.millis(fadeIn),
                new KeyValue(bgColorProp, lightGreen, Interpolator.LINEAR))
        );

        // 第二段：3 个闪烁周期，每周期 2000ms（1000ms 转到深绿，1000ms 转回浅绿）
        for (int i = 0; i < 3; i++) {
            double cycleStart = fadeIn + i * 2000;
            timeline.getKeyFrames().addAll(
                new KeyFrame(Duration.millis(cycleStart + 1000),
                    new KeyValue(bgColorProp, darkGreen, Interpolator.LINEAR)),
                new KeyFrame(Duration.millis(cycleStart + 2000),
                    new KeyValue(bgColorProp, lightGreen, Interpolator.LINEAR))
            );
        }

        // 第三段：淡出回原背景色，并在动画结束时恢复按钮文字和样式
        timeline.getKeyFrames().addAll(
            new KeyFrame(Duration.millis(fadeIn + blinkTotal),
                new KeyValue(bgColorProp, lightGreen, Interpolator.LINEAR)),
            new KeyFrame(Duration.millis(fadeIn + blinkTotal + fadeOut), e -> {
                animating = false;
                screenShotButton.setText(originalText);
                screenShotButton.setStyle(originalStyle);
            },
                new KeyValue(bgColorProp, getButtonBackgroundColor(), Interpolator.LINEAR))
        );

        timeline.play();
    }

    /**
     * 读取截屏按钮当前的背景色，用于动画起点/终点。
     * <p>
     * 若按钮没有背景或背景不是纯色，则回退为默认深灰 {@code #2d2d2d}。
     * </p>
     *
     * @return 按钮当前背景色
     */
    private Color getButtonBackgroundColor() {
        Background bg = screenShotButton.getBackground();
        if (bg != null && !bg.getFills().isEmpty()) {
            Paint fill = bg.getFills().get(0).getFill();
            if (fill instanceof Color) {
                return (Color) fill;
            }
        }
        return Color.web("#2d2d2d");
    }

    /**
     * 截屏失败时弹出提示框。
     */
    private void showFailureAlert() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(Idf.userLanguageBundle.getString("mainpage.snapshot.failure.title"));
        alert.setHeaderText(Idf.userLanguageBundle.getString("mainpage.snapshot.failure.title"));
        alert.setContentText(Idf.userLanguageBundle.getString("mainpage.snapshot.failure.header"));
        alert.showAndWait();
    }
}
