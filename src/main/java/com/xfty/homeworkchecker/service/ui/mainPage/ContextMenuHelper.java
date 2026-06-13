package com.xfty.homeworkchecker.service.ui.mainPage;

import com.xfty.homeworkchecker.Idf;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;

public class ContextMenuHelper {

    private static final Logger logger = LoggerFactory.getLogger(ContextMenuHelper.class);

    private final TextArea editMain;
    private final Function<Image, Integer> imagePasteHandler;
    private ContextMenu contextMenu;

    public ContextMenuHelper(TextArea editMain, Function<Image, Integer> imagePasteHandler) {
        this.editMain = editMain;
        this.imagePasteHandler = imagePasteHandler;
    }

    public void setupKeyPressHandler() {
        if (editMain == null) {
            logger.warn("Cannot setup key press handler: editMain is null");
            return;
        }

        editMain.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.isControlDown() && event.getCode() == KeyCode.V) {
                handleImagePaste(event);
            }
        });

        logger.debug("Key press handler registered for editMain");
    }

    public void setupContextMenu() {
        if (editMain == null) {
            logger.warn("Cannot setup context menu: editMain is null");
            return;
        }

        contextMenu = new ContextMenu();
        contextMenu.getStyleClass().add("edit-main-context-menu");

        MenuItem cutItem = new MenuItem();
        MenuItem copyItem = new MenuItem();
        MenuItem pasteItem = new MenuItem();
        MenuItem pasteImageItem = new MenuItem();
        MenuItem deleteItem = new MenuItem();
        MenuItem selectAllItem = new MenuItem();

        cutItem.setText(ctxMenuText("editmain.contextmenu.cut", "剪切") + "\tCtrl+X");
        copyItem.setText(ctxMenuText("editmain.contextmenu.copy", "复制") + "\tCtrl+C");
        pasteItem.setText(ctxMenuText("editmain.contextmenu.paste", "粘贴") + "\tCtrl+V");
        pasteImageItem.setText(ctxMenuText("editmain.contextmenu.pasteImage", "粘贴图片") + "\tCtrl+V");
        deleteItem.setText(ctxMenuText("editmain.contextmenu.delete", "删除"));
        selectAllItem.setText(ctxMenuText("editmain.contextmenu.selectAll", "全选") + "\tCtrl+A");

        cutItem.setOnAction(e -> editMain.cut());
        copyItem.setOnAction(e -> editMain.copy());
        pasteItem.setOnAction(e -> editMain.paste());
        pasteImageItem.setOnAction(e -> handleImagePasteFromClipboard());
        deleteItem.setOnAction(e -> editMain.replaceSelection(""));
        selectAllItem.setOnAction(e -> editMain.selectAll());

        contextMenu.getItems().addAll(
            cutItem, copyItem, pasteItem, pasteImageItem,
            new SeparatorMenuItem(), deleteItem, selectAllItem
        );

        editMain.addEventHandler(ContextMenuEvent.CONTEXT_MENU_REQUESTED, event -> {
            boolean hasSelection = !editMain.getSelectedText().isEmpty();
            boolean editable = Idf.isEditable;
            Clipboard clipboard = Clipboard.getSystemClipboard();
            boolean hasString = clipboard.hasString();
            boolean hasImage = clipboard.hasImage();

            cutItem.setDisable(!editable || !hasSelection);
            copyItem.setDisable(!hasSelection);
            pasteItem.setDisable(!editable || !hasString);
            pasteImageItem.setDisable(!editable || !hasImage);
            deleteItem.setDisable(!editable || !hasSelection);
            selectAllItem.setDisable(editMain.getText().isEmpty());

            editMain.requestFocus();
            contextMenu.show(editMain, event.getScreenX(), event.getScreenY());
            event.consume();
        });

        editMain.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            if (contextMenu.isShowing()) {
                contextMenu.hide();
            }
        });

        logger.debug("Context menu initialized for editMain");
    }

    private String ctxMenuText(String key, String fallback) {
        if (Idf.userLanguageBundle != null && Idf.userLanguageBundle.containsKey(key)) {
            return Idf.userLanguageBundle.getString(key);
        }
        return fallback;
    }

    private void handleImagePaste(KeyEvent event) {
        if (handleImagePasteFromClipboard()) {
            event.consume();
        }
    }

    private boolean handleImagePasteFromClipboard() {
        if (!Idf.isEditable || imagePasteHandler == null) {
            return false;
        }
        Clipboard clipboard = Clipboard.getSystemClipboard();
        if (!clipboard.hasImage()) {
            return false;
        }
        Image image = clipboard.getImage();
        if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0) {
            return false;
        }
        Integer number = imagePasteHandler.apply(image);
        if (number != null) {
            int pos = editMain.getCaretPosition();
            String text = "【详见图片(" + number + ")】";
            editMain.insertText(pos, text);
            editMain.positionCaret(pos + text.length());
            editMain.requestFocus();
            logger.info("Image pasted as card #{}, inserted placeholder at position {}", number, pos);
            return true;
        }
        return false;
    }
}
