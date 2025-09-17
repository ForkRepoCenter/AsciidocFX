package com.kodedu.component;

import com.kodedu.helper.FxHelper;
import com.kodedu.other.RecordCellFactory;
import com.kodedu.other.RevContent;
import com.kodedu.service.PortInfo;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.control.*;
import javafx.scene.web.WebView;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Component
public class DiffEditor extends AbstractWebView {

    @Value("${application.diffeditor.url}")
    private String diffEditorUrl;

    private final TableView<RevContent> tableView = new TableView<>();

    private final TreeView<RevContent> treeView = new TreeView();

    private List<RevContent> revList = new ArrayList<>();
    private Map<String, List<RevContent>> revMap = new HashMap<>();

    @Override
    protected String getViewUrl() {
        return String.format(diffEditorUrl, PortInfo.getPort());
    }

    public void onContentLoaded() {
        WebView webView = getWebView();
        SplitPane horizantalSplitPane = new SplitPane(tableView, treeView);
        horizantalSplitPane.setOrientation(Orientation.HORIZONTAL);
        horizantalSplitPane.setDividerPositions(0.7, 0.3);

        SplitPane verticalSplitPane = new SplitPane(webView, horizantalSplitPane);
        verticalSplitPane.setOrientation(Orientation.VERTICAL);
        verticalSplitPane.setDividerPositions(0.7, 0.3);

        this.getChildren().add(verticalSplitPane);
        initTableView();
        initTreeView();
        FxHelper.fitToParent(verticalSplitPane);
    }

    private void initTreeView() {
        treeView.setCellFactory(param -> new TreeCell() {
            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    if (item instanceof RevContent r) {
                        setText(r.getRevPath());
                    } else {
                        setText(item.toString());
                    }
                }
            }
        });
        treeView.setOnMouseClicked(event -> {
            TreeItem<RevContent> selectedItem = treeView.getSelectionModel().getSelectedItem();
            if (Objects.nonNull(selectedItem)) {
                Object value = selectedItem.getValue();
                if (value instanceof RevContent revContent) {
//                    updateContent(revContent.getContent(), revContent.getContent());
                    updateContent(revContent);
                }
            }
        });
    }

    private void initTableView() {

        TableColumn<RevContent, Number> indexColumn = new TableColumn<>("#");
        indexColumn.setCellValueFactory(column -> new ReadOnlyIntegerWrapper(tableView.getItems().indexOf(column.getValue()) + 1));

        TableColumn<RevContent, String> timeColumn = new TableColumn<>("Commit Time");
        timeColumn.setCellValueFactory(new RecordCellFactory(r -> r.getCommitDateTime().toString()));

        TableColumn<RevContent, String> nameColumn = new TableColumn<>("Commit ID");
        nameColumn.setCellValueFactory(new RecordCellFactory(r -> r.getCommitId()));

        TableColumn<RevContent, String> messageColumn = new TableColumn<>("Commit Message");
        messageColumn.setCellValueFactory(new RecordCellFactory(r -> r.getCommitMessage()));

        timeColumn.setPrefWidth(150);
        nameColumn.setPrefWidth(100);
        messageColumn.prefWidthProperty()
                .bind(tableView.widthProperty()
                        .subtract(timeColumn.widthProperty())
                        .subtract(nameColumn.widthProperty()));


        tableView.getColumns().add(indexColumn);
        tableView.getColumns().add(timeColumn);
        tableView.getColumns().add(nameColumn);
        tableView.getColumns().add(messageColumn);
        tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        tableView.getSelectionModel().getSelectedCells().addListener((ListChangeListener.Change<? extends TablePosition> change) -> {
            processSelections();
            processSelection();
        });

        tableView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            processSelections();
            processSelection();
        });

    }

    private void processSelection() {
        ObservableList<RevContent> selectedItems = tableView.getSelectionModel().getSelectedItems();

        if (selectedItems.size() == 1) {
            int index = tableView.getSelectionModel().getSelectedIndex();
            showDiffForItem(index, selectedItems.getFirst());
        }
    }

    private void showDiffForItem(int index, RevContent rowData) {
//        if (index < tableView.getItems().size() - 1) { // Bir oncekinin path i baska bir dosya olabilir
//            RevContent nextRowData = tableView.getItems().get(index + 1);
//            if (Objects.equals(nextRowData.getRevPath(), rowData.getRevPath())) { // consider if empty
//                updateContent(nextRowData.getContent(), rowData.getContent());
//                return;
//            }
//        }
        updateContent(rowData);
    }

    private void processSelections() {
        ObservableList<RevContent> revContents = tableView.getSelectionModel().getSelectedItems();

        TreeItem rootItem = new TreeItem("/");
        rootItem.setExpanded(true);

        Map<Path, TreeItem<RevContent>> directoryMap = new HashMap<>();
        Set<Path> processedFiles = new HashSet<>();

        for (RevContent revContent : revContents) {
            List<String> revPaths = revContent.getRevPaths();
            for (String revPath : revPaths) {
                Path path = Paths.get(revPath);
                if (processedFiles.contains(path)) {
                    continue;
                }
                processedFiles.add(path);
                TreeItem<RevContent> parent = rootItem;

                // Iterate over each segment of the path
                for (int i = 0; i < path.getNameCount(); i++) {
                    Path segment = path.getName(i);

                    // Check if the segment is a directory
                    if (i < path.getNameCount() - 1) {
                        if (!directoryMap.containsKey(segment)) {
                            TreeItem directoryItem = new TreeItem(segment.toString());
                            directoryItem.setExpanded(true);
                            directoryMap.put(segment, directoryItem);
                            parent.getChildren().add(directoryItem);
                            parent = directoryItem;
                        } else {
                            parent = directoryMap.get(segment);
                        }
                    } else { // Last segment is a file
                        TreeItem<RevContent> fileItem = new TreeItem<>(revContent);
                        fileItem.setExpanded(true);
                        parent.getChildren().add(fileItem);
                    }
                }
            }

        }

        TreeItem<RevContent> root = treeView.getRoot();
        if (Objects.nonNull(root)) {
            root.getChildren().clear();
        }

        threadService.runActionLater(() -> {
            treeView.setRoot(rootItem);
        }, true);
    }

    public void updateContent(String left, String right) {
        threadService.runActionLater(() -> {
            call("updateContent", left, right);
        });
    }

    public void updateContent(List<RevContent> revList, Map<String, List<RevContent>> revMap) {
        this.revList = revList;
        this.revMap = revMap;
        threadService.runActionLater(() -> {
            ObservableList<RevContent> data = FXCollections.observableArrayList(revList);
            if (data.size() > 1) {
                RevContent revContent = data.get(1);
                updateContent(revContent);
            }
            tableView.setItems(data);
            showStage();
            getWebView().requestFocus();
        });
    }

    private void updateContent(RevContent revContent) {
        String singlePath = revContent.getSinglePath();
        if (Objects.nonNull(singlePath)) {
            List<RevContent> revContents = revMap.getOrDefault(singlePath, List.of());
            List<RevContent> lastTwo = revContents.stream()
                    .dropWhile(r -> r != revContent).limit(2)
                    .collect(Collectors.toList());
            if (lastTwo.size() > 1) {
                RevContent first = lastTwo.get(0);
                RevContent second = lastTwo.get(1);
                updateContent(second.getContent(singlePath), first.getContent(singlePath));
            } else if (lastTwo.size() == 1) {
                RevContent first = lastTwo.get(0);
                updateContent("", first.getContent(singlePath));
            } else {
                AlertHelper.showAlert("Content not found");
            }
        }
    }
}
