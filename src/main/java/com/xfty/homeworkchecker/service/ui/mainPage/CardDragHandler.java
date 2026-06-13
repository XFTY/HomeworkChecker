package com.xfty.homeworkchecker.service.ui.mainPage;

import com.xfty.homeworkchecker.Idf;
import javafx.animation.Interpolator;
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

public class CardDragHandler {

    private static final Logger logger = LoggerFactory.getLogger(CardDragHandler.class);
    private static final DataFormat CARD_INDEX_FORMAT = new DataFormat("application/x-homeworkchecker-card-index");

    private final VBox cardList;
    private final BiConsumer<Integer, Integer> onReorder;
    private final Runnable onReload;

    private Node dragNode;
    private int dragOriginalIndex = -1;
    private int currentPreviewIndex = -1;
    private final Map<Node, Double> originalCardYs = new HashMap<>();
    private final Map<Node, Double> originalCardHeights = new HashMap<>();

    public CardDragHandler(VBox cardList, BiConsumer<Integer, Integer> onReorder, Runnable onReload) {
        this.cardList = cardList;
        this.onReorder = onReorder;
        this.onReload = onReload;
    }

    public boolean isDragging() {
        return dragNode != null;
    }

    public void setupDropTarget() {
        cardList.addEventFilter(DragEvent.DRAG_OVER, event -> {
            if (dragNode == null || !event.getDragboard().hasContent(CARD_INDEX_FORMAT)) {
                return;
            }
            event.acceptTransferModes(TransferMode.MOVE);

            double yInCardList = cardList.sceneToLocal(event.getSceneX(), event.getSceneY()).getY();
            int targetIdx = computeTargetIndex(yInCardList);
            if (targetIdx != currentPreviewIndex && targetIdx >= 0) {
                previewMove(targetIdx);
            }
            event.consume();
        });

        cardList.setOnDragDropped(event -> {
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
    }

    public void setupDragHandlers(Node cardRoot, int index) {
        cardRoot.setOnDragDetected(event -> {
            if (!Idf.isEditable) {
                return;
            }
            dragNode = cardRoot;
            dragOriginalIndex = index;
            currentPreviewIndex = index;

            originalCardYs.clear();
            originalCardHeights.clear();
            for (Node child : cardList.getChildren()) {
                originalCardYs.put(child, child.getBoundsInParent().getMinY());
                originalCardHeights.put(child, child.getBoundsInLocal().getHeight());
            }

            cardRoot.setOpacity(0.5);
            Dragboard db = cardRoot.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
            content.put(CARD_INDEX_FORMAT, index);
            db.setContent(content);
            db.setDragView(cardRoot.snapshot(null, null));
            event.consume();
        });

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

    private int computeTargetIndex(double mouseYInCardList) {
        if (originalCardYs.isEmpty()) return currentPreviewIndex;

        double spacing = cardList.getSpacing();
        List<Node> sorted = new ArrayList<>(originalCardYs.keySet());
        sorted.sort((a, b) -> Double.compare(originalCardYs.get(a), originalCardYs.get(b)));

        double cumY = 0;
        for (int j = 0; j < sorted.size(); j++) {
            Node card = sorted.get(j);
            double h = originalCardHeights.getOrDefault(card, 0.0);
            double midY = cumY + h / 2;
            if (mouseYInCardList < midY) {
                return j;
            }
            cumY += h + spacing;
        }
        return Math.max(0, sorted.size() - 1);
    }

    private void previewMove(int targetIdx) {
        if (dragNode == null || targetIdx == currentPreviewIndex) return;
        int count = cardList.getChildren().size();
        if (count < 2) return;

        double[] oldY = new double[count];
        for (int i = 0; i < count; i++) {
            oldY[i] = cardList.getChildren().get(i).getBoundsInParent().getMinY();
        }

        int dragIdx = cardList.getChildren().indexOf(dragNode);
        if (dragIdx < 0) return;

        for (int i = 0; i < count; i++) {
            Node child = cardList.getChildren().get(i);
            Timeline existing = (Timeline) child.getProperties().get("dragAnim");
            if (existing != null) existing.stop();
            child.setTranslateY(0);
        }

        Node node = cardList.getChildren().remove(dragIdx);
        int clamped = Math.max(0, Math.min(targetIdx, cardList.getChildren().size()));
        cardList.getChildren().add(clamped, node);

        cardList.applyCss();
        cardList.layout();

        double[] newY = new double[count];
        for (int i = 0; i < count; i++) {
            newY[i] = cardList.getChildren().get(i).getBoundsInParent().getMinY();
        }

        for (int i = 0; i < count; i++) {
            Node child = cardList.getChildren().get(i);
            double target = oldY[i] - newY[i];
            Timeline t = new Timeline(
                new KeyFrame(Duration.millis(150),
                    new KeyValue(child.translateYProperty(), 0, Interpolator.EASE_OUT)
                )
            );
            child.setTranslateY(target);
            child.getProperties().put("dragAnim", t);
            t.play();
        }

        currentPreviewIndex = targetIdx;
    }

    private void commitReorder(int fromIndex, int toIndex) {
        resetTranslateYThen(() -> {
            if (onReorder != null) {
                onReorder.accept(fromIndex, toIndex);
            }
        });
    }

    private void resetTranslateYThen(Runnable onDone) {
        int count = cardList.getChildren().size();
        if (count == 0) {
            if (onDone != null) onDone.run();
            return;
        }
        int[] completed = {0};
        for (int i = 0; i < count; i++) {
            Node child = cardList.getChildren().get(i);
            Timeline existing = (Timeline) child.getProperties().get("dragAnim");
            if (existing != null) existing.stop();
            Timeline t = new Timeline(
                new KeyFrame(Duration.millis(150),
                    new KeyValue(child.translateYProperty(), 0, Interpolator.EASE_OUT)
                )
            );
            child.getProperties().put("dragAnim", t);
            t.setOnFinished(e -> {
                child.getProperties().remove("dragAnim");
                completed[0]++;
                if (onDone != null && completed[0] >= count) {
                    onDone.run();
                }
            });
            t.play();
        }
    }
}
