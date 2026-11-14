package com.xfty.homeworkchecker.service.ui.mainPage;

import com.xfty.homeworkchecker.Idf;
import com.xfty.homeworkchecker.service.ui.motion.MotionEasing;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.Node;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * CardDragHandler —— 卡片列表的拖拽排序处理器。
 * <p>
 * 采用「拖拽预览 + 松手提交」两段式：
 * <ul>
 *   <li>拖动过程中实时重排列表节点（{@link #previewMove}），并用位移动画制造平滑效果；</li>
 *   <li>松手时才把最终顺序提交给持久化层（{@link #commitReorder}）。</li>
 * </ul>
 * 拖拽通过 JavaFX 的 Dragboard 传递被拖卡片的下标，因此同一列表内的所有卡片共享一套事件处理。
 * 编辑态、锁定态下不允许发起拖拽。
 * </p>
 */
public class CardDragHandler {

    private static final Logger logger = LoggerFactory.getLogger(CardDragHandler.class);
    /** 拖拽数据格式：承载被拖卡片在列表中的原始下标。 */
    private static final DataFormat CARD_INDEX_FORMAT = new DataFormat("application/x-homeworkchecker-card-index");
    /** 卡片根节点属性键：保存该卡片当前的位移动画，避免重复动画叠加。 */
    private static final String DRAG_ANIM_PROPERTY = "dragAnim";
    /** 拖拽预览与复位动画时长（毫秒，在原有节奏上放慢一倍）。 */
    private static final double DRAG_ANIM_MILLIS = 300;

    /** 卡片列表容器。 */
    private final VBox cardList;
    /** 排序提交回调：参数为（原下标, 目标下标）。 */
    private final BiConsumer<Integer, Integer> onReorder;
    /** 拖拽被取消时的刷新回调。 */
    private final Runnable onReload;
    /** 返回当前是否有卡片正在内联编辑；为 true 时禁止发起卡片拖拽。 */
    private final Supplier<Boolean> isEditingCard;

    /** 当前正在被拖拽的卡片节点；null 表示当前没有拖拽。 */
    private Node dragNode;
    /** 被拖卡片在拖拽开始时的原始下标。 */
    private int dragOriginalIndex = -1;
    /** 拖拽预览当前停留的位置，用于避免重复重排。 */
    private int currentPreviewIndex = -1;
    /** 拖拽开始时各卡片的 Y 坐标快照（用于计算重排后的动画偏移量）。 */
    private final Map<Node, Double> originalCardYs = new HashMap<>();
    /** 拖拽开始时各卡片的高度快照（用于计算目标插入位置）。 */
    private final Map<Node, Double> originalCardHeights = new HashMap<>();

    /**
     * 构造卡片拖拽处理器。
     *
     * @param cardList      卡片列表容器
     * @param onReorder     拖放完成后的排序提交回调
     * @param onReload      拖拽取消时的刷新回调
     * @param isEditingCard 返回是否有卡片正在内联编辑，用于禁止编辑期间拖拽
     */
    public CardDragHandler(VBox cardList, BiConsumer<Integer, Integer> onReorder, Runnable onReload,
                           Supplier<Boolean> isEditingCard) {
        this.cardList = cardList;
        this.onReorder = onReorder;
        this.onReload = onReload;
        this.isEditingCard = isEditingCard;
    }

    /**
     * 是否正在拖拽卡片。
     *
     * @return true 表示当前有卡片正在被拖动
     */
    public boolean isDragging() {
        return dragNode != null;
    }

    /**
     * 在卡片列表容器上注册「接收拖放」的全局事件。
     * <p>
     * DRAG_OVER 负责实时预览重排，DRAG_DROPPED 负责在松手时提交顺序。
     * 这两个事件注册在容器上而不是单张卡片上，因此拖到卡片之间的空隙也能正确响应。
     * </p>
     */
    public void setupDropTarget() {
        // 拖动经过列表时：根据鼠标纵向位置计算目标下标并即时预览
        cardList.addEventFilter(DragEvent.DRAG_OVER, event -> {
            if (dragNode == null || !event.getDragboard().hasContent(CARD_INDEX_FORMAT)) {
                return;
            }
            event.acceptTransferModes(TransferMode.MOVE);

            // 把场景坐标换算为列表内部坐标，避免受窗口位置影响
            double yInCardList = cardList.sceneToLocal(event.getSceneX(), event.getSceneY()).getY();
            int targetIdx = computeTargetIndex(yInCardList);
            if (targetIdx != currentPreviewIndex && targetIdx >= 0) {
                previewMove(targetIdx);
            }
            event.consume();
        });

        // 在列表空白区域松手：同样视为一次排序提交
        cardList.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasContent(CARD_INDEX_FORMAT) && dragNode != null) {
                double yInCardList = cardList.sceneToLocal(event.getSceneX(), event.getSceneY()).getY();
                int targetIdx = computeTargetIndex(yInCardList);
                // 位置有效且确实发生了变化，才提交排序
                if (targetIdx >= 0 && dragOriginalIndex >= 0 && targetIdx != dragOriginalIndex) {
                    commitReorder(dragOriginalIndex, targetIdx);
                    success = true;
                }
            }
            if (success) {
                dragNode = null;
            }
            event.setDropCompleted(success);
            event.consume();
        });
    }

    /**
     * 为单张卡片注册拖拽源事件（开始、放下、结束）。
     *
     * @param cardRoot 卡片根节点
     * @param index    卡片当前下标
     */
    public void setupDragHandlers(Node cardRoot, int index) {
        // 拖拽开始：锁定态或编辑态直接忽略，否则记录现场并启动拖拽
        cardRoot.setOnDragDetected(event -> {
            if (!Idf.isEditable) {
                return;
            }
            // 任意卡片处于内联编辑态时，禁止所有卡片发起拖拽，避免打断编辑与文字选择
            if (isEditingCard != null && isEditingCard.get()) {
                return;
            }
            dragNode = cardRoot;
            dragOriginalIndex = index;
            currentPreviewIndex = index;

            // 记录所有卡片在父容器中的初始位置和高度，供后续动画与命中计算使用
            originalCardYs.clear();
            originalCardHeights.clear();
            for (Node child : cardList.getChildren()) {
                originalCardYs.put(child, child.getBoundsInParent().getMinY());
                originalCardHeights.put(child, child.getBoundsInLocal().getHeight());
            }

            // 半透明表示该卡片正在被拖动
            cardRoot.setOpacity(0.5);
            Dragboard db = cardRoot.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
            content.put(CARD_INDEX_FORMAT, index);
            db.setContent(content);
            // 拖拽时跟随鼠标显示卡片快照，而不是默认的图标
            db.setDragView(cardRoot.snapshot(null, null));
            event.consume();
        });

        // 直接在卡片上松手：与在容器空白处松手走相同逻辑
        cardRoot.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasContent(CARD_INDEX_FORMAT) && dragNode != null) {
                double yInCardList = cardList.sceneToLocal(event.getSceneX(), event.getSceneY()).getY();
                int targetIdx = computeTargetIndex(yInCardList);
                if (targetIdx >= 0 && dragOriginalIndex >= 0 && targetIdx != dragOriginalIndex) {
                    commitReorder(dragOriginalIndex, targetIdx);
                    success = true;
                }
            }
            if (success) {
                dragNode = null;
            }
            event.setDropCompleted(success);
            event.consume();
        });

        // 拖拽结束（无论成功与否）：恢复透明度、清理现场；若未被接受则刷新列表还原顺序
        cardRoot.setOnDragDone(event -> {
            if (dragNode != null) {
                dragNode.setOpacity(1.0);
                if (!event.isAccepted() && onReload != null) {
                    onReload.run();
                }
            }
            dragNode = null;
            dragOriginalIndex = -1;
            currentPreviewIndex = -1;
            originalCardYs.clear();
            originalCardHeights.clear();
        });
    }

    /**
     * 根据鼠标在列表中的纵向位置，计算应插入的目标下标。
     * <p>
     * 算法：按原始 Y 坐标从上到下遍历卡片，累加「卡片高度 + 间距」；
     * 鼠标首次落在某张卡片垂直中点之上时，该卡片的下标即为插入位置。
     * 若鼠标在所有卡片下方，则插入到末尾。
     * </p>
     *
     * @param mouseYInCardList 鼠标相对列表容器的 Y 坐标
     * @return 目标插入下标（从 0 开始）
     */
    private int computeTargetIndex(double mouseYInCardList) {
        if (originalCardYs.isEmpty()) return currentPreviewIndex;

        double spacing = cardList.getSpacing();
        // 按原始 Y 坐标排序，保证计算顺序与界面从上到下一致
        List<Node> sorted = new ArrayList<>(originalCardYs.keySet());
        sorted.sort((a, b) -> Double.compare(originalCardYs.get(a), originalCardYs.get(b)));

        double cumY = 0;
        for (int j = 0; j < sorted.size(); j++) {
            Node card = sorted.get(j);
            double h = originalCardHeights.getOrDefault(card, 0.0);
            double midY = cumY + h / 2;
            // 鼠标越过某卡片中点之前 → 插入到该卡片之前
            if (mouseYInCardList < midY) {
                return j;
            }
            cumY += h + spacing;
        }
        // 鼠标在所有卡片下方 → 插入到最后一个位置
        return Math.max(0, sorted.size() - 1);
    }

    /**
     * 实时预览重排：把被拖卡片移动到目标下标，并让其他卡片以位移动画平滑让位。
     *
     * @param targetIdx 预览目标下标
     */
    private void previewMove(int targetIdx) {
        if (dragNode == null || targetIdx == currentPreviewIndex) return;
        int count = cardList.getChildren().size();
        // 少于两张卡片时不存在排序意义
        if (count < 2) return;

        // 记录重排前每张卡片的 Y 坐标（按当前子节点顺序）
        double[] oldY = new double[count];
        for (int i = 0; i < count; i++) {
            oldY[i] = cardList.getChildren().get(i).getBoundsInParent().getMinY();
        }

        int dragIdx = cardList.getChildren().indexOf(dragNode);
        if (dragIdx < 0) return;

        // 先停掉所有正在进行的位移动画，并把位移归零，避免动画相互叠加
        for (int i = 0; i < count; i++) {
            Node child = cardList.getChildren().get(i);
            Timeline existing = (Timeline) child.getProperties().get(DRAG_ANIM_PROPERTY);
            if (existing != null) existing.stop();
            child.setTranslateY(0);
        }

        // 真正调整子节点顺序（只改界面，尚未写盘）
        Node node = cardList.getChildren().remove(dragIdx);
        int clamped = Math.max(0, Math.min(targetIdx, cardList.getChildren().size()));
        cardList.getChildren().add(clamped, node);

        // 立即触发布局，才能拿到重排后的新 Y 坐标
        cardList.applyCss();
        cardList.layout();

        double[] newY = new double[count];
        for (int i = 0; i < count; i++) {
            newY[i] = cardList.getChildren().get(i).getBoundsInParent().getMinY();
        }

        // 用「旧位置 - 新位置」作为初始位移，再动画回到 0，制造平滑移动效果
        for (int i = 0; i < count; i++) {
            Node child = cardList.getChildren().get(i);
            double target = oldY[i] - newY[i];
            Timeline t = new Timeline(
                new KeyFrame(Duration.millis(DRAG_ANIM_MILLIS),
                    new KeyValue(child.translateYProperty(), 0, MotionEasing.SPRING)
                )
            );
            child.setTranslateY(target);
            child.getProperties().put(DRAG_ANIM_PROPERTY, t);
            t.play();
        }

        currentPreviewIndex = targetIdx;
    }

    /**
     * 提交排序：先等复位动画结束，再把最终顺序交给持久化回调。
     *
     * @param fromIndex 原下标
     * @param toIndex   目标下标
     */
    private void commitReorder(int fromIndex, int toIndex) {
        resetTranslateYThen(() -> {
            if (onReorder != null) {
                onReorder.accept(fromIndex, toIndex);
            }
        });
    }

    /**
     * 把所有卡片的位移归零，并在全部动画结束后执行回调。
     * <p>
     * 用计数器确保所有卡片动画都完成一次后才调用 {@code onDone}，
     * 避免在动画过程中提交排序导致界面跳动。
     * </p>
     *
     * @param onDone 全部复位后要执行的回调
     */
    private void resetTranslateYThen(Runnable onDone) {
        int count = cardList.getChildren().size();
        if (count == 0) {
            if (onDone != null) onDone.run();
            return;
        }
        int[] completed = {0};
        for (int i = 0; i < count; i++) {
            Node child = cardList.getChildren().get(i);
            Timeline existing = (Timeline) child.getProperties().get(DRAG_ANIM_PROPERTY);
            if (existing != null) existing.stop();
            Timeline t = new Timeline(
                new KeyFrame(Duration.millis(DRAG_ANIM_MILLIS),
                    new KeyValue(child.translateYProperty(), 0, MotionEasing.SPRING)
                )
            );
            child.getProperties().put(DRAG_ANIM_PROPERTY, t);
            t.setOnFinished(e -> {
                child.getProperties().remove(DRAG_ANIM_PROPERTY);
                completed[0]++;
                // 所有卡片的复位动画都结束后才执行回调
                if (onDone != null && completed[0] >= count) {
                    onDone.run();
                }
            });
            t.play();
        }
    }
}
