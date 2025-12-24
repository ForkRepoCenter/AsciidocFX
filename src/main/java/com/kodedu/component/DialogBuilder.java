package com.kodedu.component;

import javafx.scene.control.Alert;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

/**
 * Created by usta on 16.03.2015.
 */
public final class DialogBuilder extends TextDialog {

    public final static String FILE_NAME_REGEX = ".*\\S.*";
    public final static String FOLDER_NAME_REGEX = ".*\\S.*";
    public final static String LINE_COLUMN_REGEX = "\\d+:\\d+|\\d+";

    public DialogBuilder(String content, String title) {
        super(content, title);
        showAlwaysOnTop(getDialogPane());
    }

    public static DialogBuilder newFileDialog() {
        DialogBuilder dialog = new DialogBuilder("输入新文件名 ", "创建新文件 ");
        dialog.setToolTip("输入新文件名");

        dialog.setKeyReleaseEvent(FILE_NAME_REGEX);
        return dialog;
    }

    public static DialogBuilder newFolderDialog() {
        DialogBuilder dialog = new DialogBuilder("输入新目录名 ", "创建新目录 ");
        dialog.setToolTip("输入新目录名");
        dialog.setKeyReleaseEvent(FOLDER_NAME_REGEX);
        return dialog;
    }

    public static DialogBuilder newJumpLineDialog() {
        DialogBuilder dialog = new DialogBuilder("", "转到 行/列 ");
        dialog.setToolTip("输入 line:column");
        dialog.setKeyReleaseEvent(LINE_COLUMN_REGEX);
        TextField editor = dialog.getEditor();
        editor.setText("0:0");
        editor.selectAll();
        editor.requestFocus();
        return dialog;
    }

    private static void showAlwaysOnTop(DialogPane dialogPane) {
        ((Stage) dialogPane.getScene().getWindow()).setAlwaysOnTop(true);
    }

    private static void showAlwaysOnTop(Alert alert) {
        showAlwaysOnTop(alert.getDialogPane());
    }

}
