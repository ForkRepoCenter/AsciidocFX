package com.kodedu.component;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.ListView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Screen;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static javafx.scene.control.Alert.AlertType;

/**
 * Created by usta on 06.03.2015.
 */
public final class AlertHelper {

    public static final ButtonType LOAD_FILE_SYSTEM_CHANGES = new ButtonType("加载文件系统更改");
    public static final ButtonType KEEP_MEMORY_CHANGES = new ButtonType("保留内存更改");

    public static final ButtonType OPEN_IN_APP = new ButtonType("无论如何打开");
    public static final ButtonType OPEN_EXTERNAL = new ButtonType("打开外部");

    static Alert buildDeleteAlertDialog(List<Path> pathsLabel) {
        Alert deleteAlert = new WindowModalAlert(Alert.AlertType.WARNING, null, ButtonType.YES, ButtonType.CANCEL);
        deleteAlert.setHeaderText("是否要删除所选路径?");
        DialogPane dialogPane = deleteAlert.getDialogPane();

        ObservableList<Path> paths = Optional.ofNullable(pathsLabel)
                .map(FXCollections::observableList)
                .orElse(FXCollections.emptyObservableList());

        if (paths.isEmpty()) {
            dialogPane.setContentText("未选择任何文件。");
            deleteAlert.getButtonTypes().clear();
            deleteAlert.getButtonTypes().add(ButtonType.CANCEL);
            return deleteAlert;
        }

        ListView<Path> listView = new ListView<>(paths);
        listView.setId("listOfPaths");

        GridPane gridPane = new GridPane();
        gridPane.addRow(0, listView);
        GridPane.setHgrow(listView, Priority.ALWAYS);

        double minWidth = 200.0;
        double maxWidth = Screen.getScreens().stream()
                .mapToDouble(s -> s.getBounds().getWidth() / 3)
                .min().orElse(minWidth);

        double prefWidth = paths.stream()
                .map(String::valueOf)
                .mapToDouble(s -> s.length() * 7)
                .max()
                .orElse(maxWidth);

        double minHeight = IntStream.of(paths.size())
                .map(e -> e * 70)
                .filter(e -> e <= 300 && e >= 70)
                .findFirst()
                .orElse(200);

        gridPane.setMinWidth(minWidth);
        gridPane.setPrefWidth(prefWidth);
        gridPane.setPrefHeight(minHeight);
        dialogPane.setContent(gridPane);
        return deleteAlert;
    }

    public static Optional<ButtonType> deleteAlert(List<Path> pathsLabel) {
        return buildDeleteAlertDialog(pathsLabel).showAndWait();
    }

    public static Optional<ButtonType> showAlert(String alertMessage) {
        AlertDialog deleteAlert = new AlertDialog(AlertType.WARNING, null, ButtonType.YES, ButtonType.CANCEL);
        deleteAlert.setHeaderText(alertMessage);
        return deleteAlert.showAndWait();
    }

    public static Optional<ButtonType> showYesNoAlert(String alertMessage) {
        AlertDialog deleteAlert = new AlertDialog(AlertType.WARNING, null, ButtonType.YES, ButtonType.NO);
        deleteAlert.setHeaderText(alertMessage);
        return deleteAlert.showAndWait();
    }

    public static void okayAlert(String alertMessage) {
        AlertDialog deleteAlert = new AlertDialog(AlertType.WARNING, null, ButtonType.OK);
        deleteAlert.setHeaderText(alertMessage);
        deleteAlert.show();
    }

    public static Optional<ButtonType> nullDirectoryAlert() {
        AlertDialog deleteAlert = new AlertDialog(AlertType.WARNING, null, ButtonType.OK);
        deleteAlert.setHeaderText("请选择目录");
        return deleteAlert.showAndWait();
    }

    public static Optional<ButtonType> notImplementedDialog() {
        AlertDialog alert = new AlertDialog(AlertType.WARNING, null, ButtonType.OK);
        alert.setHeaderText("此功能不适用于Markdown。");
        return alert.showAndWait();
    }

    public static Optional<ButtonType> saveAlert() {
        AlertDialog saveAlert = new AlertDialog();
        saveAlert.setHeaderText("此文档未保存。您想关闭它吗?");
        return saveAlert.showAndWait();
    }

    public static Optional<ButtonType> conflictAlert(Path path) {
        Alert alert = new WindowModalAlert(Alert.AlertType.WARNING);
        alert.setTitle("文件缓存冲突");
        alert.setHeaderText(String.format("已对内存和磁盘中的 '%s' 进行了更改", path));
        alert.getButtonTypes().clear();
        alert.getButtonTypes().addAll(LOAD_FILE_SYSTEM_CHANGES, KEEP_MEMORY_CHANGES, ButtonType.CANCEL);
        return alert.showAndWait();
    }

    public static Optional<ButtonType> sizeHangAlert(Path path, int hangFileSizeLimit) {
        Alert alert = new WindowModalAlert(Alert.AlertType.WARNING);
        alert.setTitle(String.format("文件大小 > %dMB", hangFileSizeLimit));
        alert.setHeaderText(String.format("这可能会导致应用程序无响应", path));
        alert.getButtonTypes().clear();
        alert.getButtonTypes().addAll(OPEN_IN_APP, OPEN_EXTERNAL, ButtonType.CANCEL);
        return alert.showAndWait();
    }

    public static Optional<ButtonType> nosizeAlert(Path path, int hangFileSizeLimit) {
        Alert alert = new WindowModalAlert(Alert.AlertType.WARNING);
        alert.setTitle("无文件大小");
        alert.setHeaderText(String.format("可能会导致应用程序无响应如果实际大小 > %dMB", path, hangFileSizeLimit));
        alert.getButtonTypes().clear();
        alert.getButtonTypes().addAll(OPEN_IN_APP, OPEN_EXTERNAL, ButtonType.CANCEL);
        return alert.showAndWait();
    }

    public static void showDuplicateWarning(List<String> duplicatePaths, Path lib) {
        Alert alert = new WindowModalAlert(Alert.AlertType.WARNING);

        DialogPane dialogPane = alert.getDialogPane();

        ListView listView = new ListView();
        listView.getStyleClass().clear();
        ObservableList items = listView.getItems();
        items.addAll(duplicatePaths);
        listView.setEditable(false);

        dialogPane.setContent(listView);

        alert.setTitle("发现重复的JAR文件");
        alert.setHeaderText(String.format("发现重复的JAR，可能会导致意外行为。\n\n" +
                "请手动从这些对中删除旧版本。 \n" +
                "JAR文件位于 %s 目录。", lib));
        alert.getButtonTypes().clear();
        alert.getButtonTypes().addAll(ButtonType.OK);
        alert.showAndWait();
    }

    public static Optional<String> showOldConfiguration(List<String> paths) {
        Alert alert = new WindowModalAlert(AlertType.INFORMATION);

        DialogPane dialogPane = alert.getDialogPane();

        ListView listView = new ListView();
        listView.getStyleClass().clear();
        ObservableList items = listView.getItems();
        items.addAll(paths);
        listView.setEditable(false);

        dialogPane.setContent(listView);

        alert.setTitle("加载以前的配置?");
        alert.setHeaderText(String.format("您有以前AsciidocFX版本的配置文件\n\n" +
                "选择要加载的配置 \n" +
                "或继续使用新配置"));
        alert.getButtonTypes().clear();
        alert.getButtonTypes().addAll(ButtonType.APPLY);
        alert.getButtonTypes().addAll(ButtonType.CANCEL);
        ButtonType buttonType = alert.showAndWait().orElse(ButtonType.CANCEL);

        Object selectedItem = listView.getSelectionModel().getSelectedItem();
        return (buttonType == ButtonType.APPLY) ?
                Optional.ofNullable((String) selectedItem) :
                Optional.empty();
    }
}
