package com.xfty.homeworkchecker.service.ui.mainPage;

import com.xfty.homeworkchecker.Idf;
import com.xfty.homeworkchecker.model.CardItem;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class CardUiService {

    private static final Logger logger = LoggerFactory.getLogger(CardUiService.class);

    // ==================== UI 组件引用 ====================

    private final VBox cardList;
    private final Button addCardButton;
    private final StackPane emptyPlaceholder;
    private final Label emptyHintLabel;
    private final VBox cardContainer;
    private final ScrollPane cardScrollPane;
    private final AnchorPane showingMainArea;
    private final AnchorPane centerShowingArea;
    private final VBox centerOutBox;
    private final Pane blackPane;
    private final SplitPane mainSplitPane;
    private final PopupService popupService;
    private final ReminderCardService reminderCardService;
    private final Runnable onEditMainClicked;

    private final CardRenderer cardRenderer;
    private final CardDragHandler cardDragHandler;

    // ==================== 卡片状态 ====================

    private final List<CardItem> currentCards = new ArrayList<>();
    private int editingCardIndex = -1;
    private boolean editingNewCard = false;
    private TextField cardTitleField;
    private TextArea cardContentField;

    // ==================== 字体配置 ====================

    private double cardFontSize = 18;
    private String cardFontFamily = "System";

    // ==================== SplitPane 状态 ====================

    private double lastDividerPosition = 0.65;
    private boolean isCardExpanded = true;

    public CardUiService(
            VBox cardList, Button addCardButton, StackPane emptyPlaceholder, Label emptyHintLabel,
            VBox cardContainer, ScrollPane cardScrollPane,
            AnchorPane showingMainArea, AnchorPane centerShowingArea,
            VBox centerOutBox, Pane blackPane,
            SplitPane mainSplitPane,
            PopupService popupService,
            ReminderCardService reminderCardService,
            Runnable onEditMainClicked) {
        this.cardList = cardList;
        this.addCardButton = addCardButton;
        this.emptyPlaceholder = emptyPlaceholder;
        this.emptyHintLabel = emptyHintLabel;
        this.cardContainer = cardContainer;
        this.cardScrollPane = cardScrollPane;
        this.showingMainArea = showingMainArea;
        this.centerShowingArea = centerShowingArea;
        this.centerOutBox = centerOutBox;
        this.blackPane = blackPane;
        this.mainSplitPane = mainSplitPane;
        this.popupService = popupService;
        this.reminderCardService = reminderCardService;
        this.onEditMainClicked = onEditMainClicked;

        this.cardRenderer = new CardRenderer(
            reminderCardService, popupService,
            showingMainArea, centerShowingArea, centerOutBox, blackPane
        );
        this.cardDragHandler = new CardDragHandler(
            cardList, this::commitReorderSilent, this::loadCards
        );
        cardDragHandler.setupDropTarget();
    }

    public void setFontConfig(String fontFamily, double fontSize) {
        this.cardFontFamily = fontFamily;
        this.cardFontSize = fontSize;
        editingCardIndex = -1;
        editingNewCard = false;
        cardTitleField = null;
        cardContentField = null;
    }

    public boolean isEditingCard() {
        return editingCardIndex >= 0;
    }

    public int getEditingCardIndex() {
        return editingCardIndex;
    }

    public void loadCards() {
        editingCardIndex = -1;
        editingNewCard = false;
        cardTitleField = null;
        cardContentField = null;
        currentCards.clear();
        currentCards.addAll(reminderCardService.readCards());
        cardList.getChildren().clear();
        for (int i = 0; i < currentCards.size(); i++) {
            cardList.getChildren().add(renderCard(currentCards.get(i), i));
        }
        boolean isEmpty = currentCards.isEmpty();
        emptyPlaceholder.setVisible(isEmpty);
        emptyPlaceholder.setManaged(isEmpty);
        if (isEmpty) {
            updateEmptyHintText();
        }
    }

    private VBox renderCard(CardItem item, int index) {
        return cardRenderer.renderCard(
            item, index, cardFontFamily, cardFontSize, editingCardIndex, editingNewCard,
            onEditMainClicked,
            this::startEditCard,
            this::deleteCard,
            cardDragHandler
        );
    }

    public int getNextImageNumber() {
        int count = 0;
        for (CardItem item : currentCards) {
            if (item.getImagePath() != null && !item.getImagePath().isEmpty()) {
                count++;
            }
        }
        return count + 1;
    }

    public void cleanupEmptyCardsOnStartup() {
        List<CardItem> cards = reminderCardService.readCards();
        boolean removed = cards.removeIf(card ->
            (card.getTitle() == null || card.getTitle().trim().isEmpty())
            && (card.getContent() == null || card.getContent().trim().isEmpty())
        );
        if (removed) {
            reminderCardService.writeCards(cards);
        }
    }

    public void cleanupEditingCard() {
        if (editingNewCard && editingCardIndex >= 0) {
            String title = cardTitleField != null ? cardTitleField.getText().trim() : "";
            String content = cardContentField != null ? cardContentField.getText().trim() : "";
            if (title.isEmpty() && content.isEmpty()) {
                deleteCard(editingCardIndex);
            }
        }
    }

    public void removeTodayImageCards() {
        List<CardItem> cards = reminderCardService.readCards();
        boolean removed = false;
        var iterator = cards.iterator();
        while (iterator.hasNext()) {
            CardItem card = iterator.next();
            if (card.getImagePath() != null && !card.getImagePath().isEmpty() && !card.isPersistent()) {
                reminderCardService.deleteImageFile(card.getImagePath());
                iterator.remove();
                removed = true;
            }
        }
        if (removed) {
            reminderCardService.writeCards(cards);
        }
        loadCards();
    }

    private void updateEmptyHintText() {
        if (Idf.isEditable) {
            emptyHintLabel.setText(Idf.userLanguageBundle.getString("card.empty.hint.unlocked"));
        } else {
            emptyHintLabel.setText(Idf.userLanguageBundle.getString("card.empty.hint.locked"));
        }
    }

    public boolean checkActiveCardEditing(Runnable onProceed) {
        if (editingCardIndex < 0) {
            onProceed.run();
            return true;
        }

        String title = cardTitleField != null ? cardTitleField.getText().trim() : "";
        String content = cardContentField != null ? cardContentField.getText().trim() : "";

        if (editingNewCard) {
            if (title.isEmpty() && content.isEmpty()) {
                deleteCard(editingCardIndex);
                onProceed.run();
                return true;
            } else if (!title.isEmpty() && !content.isEmpty()) {
                int idx = editingCardIndex;
                cardRenderer.showCardSaveConfirmDialog(
                    () -> {
                        CardItem item = currentCards.get(idx);
                        item.setTitle(title);
                        item.setContent(content);
                        reminderCardService.updateCard(idx, item);
                        editingCardIndex = -1;
                        editingNewCard = false;
                        cardTitleField = null;
                        cardContentField = null;
                        loadCards();
                        hideConfirmDialogSync();
                        onProceed.run();
                    },
                    () -> {
                        deleteCard(idx);
                        hideConfirmDialogSync();
                        onProceed.run();
                    },
                    () -> popupService.closePopup(),
                    true
                );
                return false;
            } else {
                int idx = editingCardIndex;
                cardRenderer.showCardSaveConfirmDialog(
                    () -> popupService.closePopup(),
                    () -> {
                        deleteCard(idx);
                        hideConfirmDialogSync();
                        onProceed.run();
                    },
                    () -> popupService.closePopup(),
                    false
                );
                return false;
            }
        } else {
            editingCardIndex = -1;
            editingNewCard = false;
            cardTitleField = null;
            cardContentField = null;
            loadCards();
            onProceed.run();
            return true;
        }
    }

    private void hideConfirmDialogSync() {
        cardRenderer.hideConfirmDialogSync();
    }

    public void onAddCard() {
        checkActiveCardEditing(() -> {
            CardItem newItem = new CardItem(
                CardItem.Severity.INFO, "", "", ReminderCardService.generateTimestamp()
            );
            newItem.setCreatedDate(Idf.year + Idf.month + Idf.day);
            reminderCardService.addCard(newItem);
            loadCards();
            editingNewCard = true;
            editingCardIndex = currentCards.size() - 1;
            startEditCard(editingCardIndex);
        });
    }

    public void collapseAddCardBox() {
        addCardButton.setManaged(false);
        Timeline fadeOut = new Timeline(
            new KeyFrame(Duration.millis(150),
                new KeyValue(addCardButton.opacityProperty(), 0, Interpolator.EASE_OUT)
            )
        );
        fadeOut.setOnFinished(e -> {
            addCardButton.setVisible(false);
            if (currentCards.isEmpty()) {
                updateEmptyHintText();
            }
        });
        fadeOut.play();
    }

    public void expandAddCardBox() {
        addCardButton.setVisible(true);
        addCardButton.setManaged(true);
        Timeline fadeIn = new Timeline(
            new KeyFrame(Duration.millis(150),
                new KeyValue(addCardButton.opacityProperty(), 1, Interpolator.EASE_OUT)
            )
        );
        fadeIn.setOnFinished(e -> {
            if (currentCards.isEmpty()) {
                updateEmptyHintText();
            }
        });
        fadeIn.play();
    }

    public void setupSplitPaneListener() {
        mainSplitPane.getDividers().get(0).positionProperty().addListener((obs, old, val) -> {
            double pos = val.doubleValue();

            if (pos >= 0.90 && isCardExpanded) {
                lastDividerPosition = old.doubleValue();
                isCardExpanded = false;
                cardContainer.setManaged(false);
                cardContainer.setVisible(false);
            } else if (pos < 0.90 && !isCardExpanded) {
                isCardExpanded = true;
                cardContainer.setManaged(true);
                cardContainer.setVisible(true);
            }
        });
    }

    private void startEditCard(int index) {
        CardItem item = currentCards.get(index);
        if (item.getImagePath() != null && !item.getImagePath().isEmpty()) return;

        VBox cardRoot = (VBox) cardList.getChildren().get(index);
        cardRoot.getChildren().clear();
        cardRoot.getStyleClass().removeAll("info", "warning", "critical");
        cardRoot.getStyleClass().addAll("card-item", CardRenderer.severityStyleClass(item.getSeverity()));

        VBox editBody = cardRenderer.createEditCardBody(
            item, index, cardFontFamily, cardFontSize,
            () -> saveEditCard(index),
            () -> cancelEditCard(),
            tf -> this.cardTitleField = tf,
            ta -> this.cardContentField = ta
        );
        cardRoot.getChildren().add(editBody);
    }

    private void saveEditCard(int index) {
        CardItem item = currentCards.get(index);
        String title = cardTitleField != null ? cardTitleField.getText().trim() : "";
        String content = cardContentField != null ? cardContentField.getText().trim() : "";
        if (title.isEmpty() || content.isEmpty()) return;
        item.setTitle(title);
        item.setContent(content);
        reminderCardService.updateCard(index, item);
        editingCardIndex = -1;
        editingNewCard = false;
        cardTitleField = null;
        cardContentField = null;
        loadCards();
    }

    private void cancelEditCard() {
        if (editingNewCard && editingCardIndex >= 0) {
            deleteCard(editingCardIndex);
            return;
        }
        if (editingCardIndex >= 0) {
            loadCards();
        }
        editingCardIndex = -1;
        editingNewCard = false;
        cardTitleField = null;
        cardContentField = null;
    }

    private void deleteCard(int index) {
        reminderCardService.deleteCard(index);
        editingCardIndex = -1;
        editingNewCard = false;
        cardTitleField = null;
        cardContentField = null;
        loadCards();
    }

    private void commitReorderSilent(int fromIndex, int toIndex) {
        if (fromIndex == toIndex) return;
        if (fromIndex < 0 || fromIndex >= currentCards.size()) return;
        if (toIndex < 0 || toIndex >= currentCards.size()) return;

        CardItem moved = currentCards.remove(fromIndex);
        currentCards.add(toIndex, moved);
        reminderCardService.moveCard(fromIndex, toIndex);
        loadCards();
    }
}
