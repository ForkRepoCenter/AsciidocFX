package com.kodedu.component;

/**
 * Created by usta on 16.03.2015.
 */
public final class RenameDialog extends TextDialog {

    public RenameDialog(String content, String title) {
        super(content, title);
    }

    public static RenameDialog create() {
        RenameDialog dialog = new RenameDialog("输入新文件名 ", "重命名文件 ");
        dialog.setKeyReleaseEvent("^[^\\\\/:?*\"<>|]+$");
        return dialog;
    }
}
