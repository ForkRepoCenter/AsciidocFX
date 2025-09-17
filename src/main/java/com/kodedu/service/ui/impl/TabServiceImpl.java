package com.kodedu.service.ui.impl;

import com.kodedu.component.AlertHelper;
import com.kodedu.component.EditorPane;
import com.kodedu.component.ImageTab;
import com.kodedu.component.MenuItemBuilt;
import com.kodedu.component.MyTab;
import com.kodedu.config.StoredConfigBean;
import com.kodedu.controller.ApplicationController;
import com.kodedu.helper.ClipboardHelper;
import com.kodedu.helper.IOHelper;
import com.kodedu.helper.OSHelper;
import com.kodedu.helper.FxHelper;
import com.kodedu.other.Current;
import com.kodedu.other.ExtensionFilters;
import com.kodedu.other.Item;
import com.kodedu.service.DirectoryService;
import com.kodedu.service.GitFileService;
import com.kodedu.service.PathResolverService;
import com.kodedu.service.ThreadService;
import com.kodedu.service.ui.EditorService;
import com.kodedu.service.ui.TabService;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.value.ObservableObjectValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SingleSelectionModel;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Created by usta on 25.12.2014.
 */
@Component(TabService.label)
public class TabServiceImpl implements TabService {

    private final Logger logger = LoggerFactory.getLogger(TabService.class);

    @Autowired
    private ApplicationController controller;
    @Autowired
    private ThreadService threadService;
    @Autowired
    private EditorService editorService;
    @Autowired
    private DirectoryService directoryService;
    @Autowired
    private PathResolverService pathResolver;

    private final Current current;
    private final StoredConfigBean storedConfigBean;
    private final ApplicationContext applicationContext;

    @Value("${application.editor.url}")
    private String editorUrl;

    @Value("${application.epub.url}")
    private String epubUrl;

    private ObservableList<Optional<Path>> closedPaths = FXCollections.observableArrayList();

    @Autowired
    private GitFileService gitFileService;

    @Autowired
    private ClipboardHelper clipboardHelper;

    @Autowired
    public TabServiceImpl(final Current current, StoredConfigBean storedConfigBean, ApplicationContext applicationContext) {
        this.current = current;
        this.storedConfigBean = storedConfigBean;
        this.applicationContext = applicationContext;
    }

    @Override
    public void closeFirstNewTab(){
        ObservableList<Tab> tabs = controller.getTabPane().getTabs();
        if(!tabs.isEmpty()){
            MyTab myTab = (MyTab)tabs.get(0);
            if(myTab.isNew() && !myTab.isChanged()){
                myTab.close();
            }
        }
    }

    @Override
    public void addTab(Path path, Runnable... runnables) {

        ObservableList<Item> recentFiles = storedConfigBean.getRecentFiles();
        if (Files.notExists(path)) {
            recentFiles.remove(new Item(path));
            logger.debug("Path {} not found in the filesystem", path);
            return;
        }

        ObservableList<Tab> tabs = controller.getTabPane().getTabs();
        for (Tab tab : tabs) {
            if (tab instanceof MyTab myTab) {
                Path currentPath = myTab.getPath();
                if (Objects.nonNull(currentPath))
                    if (currentPath.equals(path)) {
                        myTab.select(); // Select already added tab
                        threadService.runActionLater(() -> {
                            for (Runnable runnable : runnables) {
                                runnable.run();
                            }
                        });
                        return;
                    }
            }
        }

        AnchorPane anchorPane = new AnchorPane();

        MyTab tab = createTab();
        tab.setTabText(path.getFileName().toString());
        EditorPane editorPane = tab.getEditorPane();

        threadService.runActionLater(() -> {
            TabPane tabPane = controller.getTabPane();
            tabPane.getTabs().add(tab);
            tab.select();
        });

        Node editorVBox = editorService.createEditorVBox(editorPane, tab);
        FxHelper.fitToParent(editorVBox);

        anchorPane.getChildren().add(editorVBox);
        tab.setContent(anchorPane);
        tab.setPath(path);

        Tooltip tip = new Tooltip(path.toString());
        Tooltip.install(tab.getGraphic(), tip);

        recentFiles.remove(new Item(path));
        recentFiles.add(0, new Item(path));

        editorPane.getHandleReadyTasks().clear();
        editorPane.getHandleReadyTasks().addAll(runnables);

        editorPane.load(String.format(editorUrl, controller.getPort()));
    }


    @Override
    public void newDoc() {
        newDoc("");
    }

    @Override
    public void newDoc(final String content) {

        MyTab tab = this.createTab();
        EditorPane editorPane = tab.getEditorPane();
        editorPane.setInitialEditorValue(content);

        AnchorPane anchorPane = new AnchorPane();

        Node editorVBox = editorService.createEditorVBox(editorPane, tab);
        FxHelper.fitToParent(editorVBox);
        anchorPane.getChildren().add(editorVBox);

        tab.setContent(anchorPane);

        tab.setTabText("new *");
        TabPane tabPane = controller.getTabPane();
        tabPane.getTabs().add(tab);
        tab.select();

        editorPane.load(String.format(editorUrl, controller.getPort()));
    }

    @Override
    public void openDoc() {
        FileChooser fileChooser = directoryService.newFileChooser("Open File");

        if (OSHelper.isWindows()) {
            fileChooser.getExtensionFilters().add(ExtensionFilters.ASCIIDOC);
            fileChooser.getExtensionFilters().add(ExtensionFilters.MARKDOWN);
            fileChooser.getExtensionFilters().add(ExtensionFilters.ALL);
        }

        List<File> chosenFiles = fileChooser.showOpenMultipleDialog(controller.getStage());
        if (chosenFiles != null) {
            chosenFiles.stream().map(File::toPath).forEach(this::previewDocument);
            ObservableList<Item> recentFiles = storedConfigBean.getRecentFiles();
            chosenFiles.stream()
                    .map(e -> new Item(e.toPath()))
                    .filter(file -> !recentFiles.contains(file)).forEach(recentFiles::addAll);
            directoryService.setInitialDirectory(Optional.ofNullable(chosenFiles.get(0)));
        }
    }


    @Override
    public Path getSelectedTabPath() {
        TreeItem<Item> selectedItem = controller.getFileSystemView().getSelectionModel().getSelectedItem();
        Item value = selectedItem.getValue();
        Path path = value.getPath();
        return path;
    }


    // TODO: It is not a right place for this helper
    @Override
    public List<Path> getSelectedTabPaths() {
        ObservableList<TreeItem<Item>> treeItems = controller.getFileSystemView().getSelectionModel().getSelectedItems();
        return treeItems.stream()
                .map(TreeItem::getValue)
                .map(Item::getPath)
                .collect(Collectors.toList());
    }

    @Override
    public MyTab createTab() {

        final MyTab tab = applicationContext.getBean(MyTab.class);

        tab.setOnCloseRequest(event -> {
            event.consume();
            tab.close();
        });

        MenuItem closeMenu = new MenuItem("Close");
        closeMenu.setOnAction(actionEvent -> {
            tab.close();
        });

        MenuItem closeAll = new MenuItem("Close All");
        closeAll.setOnAction(controller::closeAllTabs);

        MenuItem closeOthers = new MenuItem("Close Others");
        closeOthers.setOnAction(event -> {

            ObservableList<Tab> blackList = FXCollections.observableArrayList();
            blackList.addAll(tab.getTabPane().getTabs());
            blackList.remove(tab);

            blackList.stream()
                    .filter(t -> t instanceof MyTab)
                    .map(t -> (MyTab) t).sorted((mo1, mo2) -> {
                if (mo1.isNew() && !mo2.isNew())
                    return -1;
                else if (mo2.isNew() && !mo1.isNew()) {
                    return 1;
                }
                return 0;
            }).forEach(myTab -> {

                if (event.isConsumed())
                    return;

                ButtonType close = myTab.close();
                if (close == ButtonType.CANCEL)
                    event.consume();
            });
        });

        MenuItem selectNextTab = new MenuItem("Select Next Tab");
        selectNextTab.setOnAction(actionEvent -> {
            TabPane tabPane = tab.getTabPane();
            if (tabPane.getSelectionModel().isSelected(tabPane.getTabs().size() - 1))
                tabPane.getSelectionModel().selectFirst();
            else
                tabPane.getSelectionModel().selectNext();
        });

        MenuItem previousTab = new MenuItem("Select Previous Tab");
        previousTab.setOnAction(actionEvent -> {
            SingleSelectionModel<Tab> selectionModel = tab.getTabPane().getSelectionModel();
            if (selectionModel.isSelected(0))
                selectionModel.selectLast();
            else
                selectionModel.selectPrevious();
        });

        MenuItem reopenClosedTab = new MenuItem("Reopen Closed Tab");
        reopenClosedTab.setOnAction(actionEvent -> {
            if (closedPaths.size() > 0) {
                int index = closedPaths.size() - 1;
                closedPaths.get(index).filter(pathResolver::isAsciidoc).ifPresent(this::addTab);
                closedPaths.get(index).filter(pathResolver::isMarkdown).ifPresent(this::addTab);
                closedPaths.get(index).filter(pathResolver::isImage).ifPresent(this::addImageTab);
                closedPaths.remove(index);
            }
        });

        MenuItem browseMenu = new MenuItem("Browse");

        browseMenu.setOnAction(event -> {
            current.currentPath()
                    .ifPresent(controller::browseFileOrFolder);
        });

        MenuItem showHistoryMenu = MenuItemBuilt.item("Show History").click(e -> {
            Path path = tab.getPath();
            Path workingDirectory = directoryService.workingDirectory();
            threadService.runTaskLater(() -> {
                if (Objects.nonNull(path) && Objects.nonNull(workingDirectory)) {
                    gitFileService.showFileHistory(workingDirectory, path);
                }
            });
        });

        MenuItem copyItem = MenuItemBuilt.item("Copy").click(event -> {
            Optional.ofNullable(tab.getPath())
                    .ifPresent(path -> clipboardHelper.copyFiles(Arrays.asList(path)));
        });

        MenuItem copyPathItem = MenuItemBuilt.item("Copy Path").click(event -> {
            Optional.ofNullable(tab.getPath())
                    .map(Path::toString)
                    .ifPresent(clipboardHelper::cutCopy);
        });

        MenuItem newFileMenu = new MenuItem("New File");
        newFileMenu.setOnAction(controller::newDoc);

        MenuItem reloadMenuItem = new MenuItem("Reload");
        reloadMenuItem.setOnAction(event -> {
            tab.load();
        });

        MenuItem gotoWorkdir = new MenuItem("Go to Workdir");
        gotoWorkdir.setOnAction(event -> {
            current.currentPath().map(Path::getParent).ifPresent(directoryService::changeWorkigDir);
        });

        ContextMenu contextMenu = new ContextMenu();
        contextMenu.getItems().addAll(closeMenu, closeAll, closeOthers, new SeparatorMenuItem(),
                selectNextTab, previousTab, reopenClosedTab, new SeparatorMenuItem(), reloadMenuItem,
                new SeparatorMenuItem(), gotoWorkdir, new SeparatorMenuItem(),
                showHistoryMenu, browseMenu, copyItem, copyPathItem, newFileMenu);

        tab.contextMenuProperty().setValue(contextMenu);
        Label label = tab.getLabel();

        label.setOnMouseClicked(mouseEvent -> {
            if (mouseEvent.getButton().equals(MouseButton.SECONDARY)) {
                tab.select();
            } else if (mouseEvent.getClickCount() > 1) {
                controller.adjustSplitPane();
            }
        });


        return tab;
    }

    @Override
    public void previewDocument(Path path) {
        if (Objects.isNull(path)) {
            logger.error("Null path cannot be viewed");
            return;
        }

        if (Files.isDirectory(path)) {
            if (path.equals(directoryService.workingDirectory())) {
                directoryService.changeWorkigDir(path.getParent());
            } else {
                directoryService.changeWorkigDir(path);
            }
        } else if (pathResolver.isImage(path)) {
            addImageTab(path);
        } else if (pathResolver.isEpub(path)) {
            current.setCurrentEpubPath(path);
            controller.browseInDesktop(String.format(epubUrl, controller.getPort()));
        } else if (pathResolver.isPDF(path) || pathResolver.isArchive(path) || pathResolver.isVideo(path) || pathResolver.isOffice(path)) {
            controller.openInDesktop(path);
        } else {
// TODO: Charset check
            Optional<Long> size = IOHelper.size(path);

            int hangFileSizeLimit = controller.getHangFileSizeLimit();

            if (size.isPresent()) {
                if (size.get() > hangFileSizeLimit * 1024 * 1024) {
                    processHangAlert(AlertHelper.sizeHangAlert(path, hangFileSizeLimit), path);
                } else {
                    addTab(path);
                }
            } else {
                processHangAlert(AlertHelper.nosizeAlert(path, hangFileSizeLimit), path);
            }
        }

    }

    private void processHangAlert(Optional<ButtonType> buttonType, Path path) {
        ButtonType btnType = buttonType.orElse(ButtonType.CANCEL);
        if (btnType == AlertHelper.OPEN_IN_APP) {
            controller.saveAllTabs();
            addTab(path);
        } else if (btnType == AlertHelper.OPEN_EXTERNAL) {
            controller.openInDesktop(path);
        }
    }

    @Override
    public void addImageTab(Path imagePath) {

        ImageTab tab = new ImageTab(imagePath);

        final TabPane previewTabPane = controller.getTabPane();
        if (previewTabPane.getTabs().contains(tab)) {
            previewTabPane.getSelectionModel().select(tab);
            return;
        }

        Image image = new Image(IOHelper.pathToUrl(imagePath));
        ImageView imageView = new ImageView(image);
        imageView.setPreserveRatio(true);

        imageView.setFitWidth(previewTabPane.getWidth());

        previewTabPane.widthProperty().addListener((observable, oldValue, newValue) -> {
            imageView.setFitWidth(previewTabPane.getWidth());
        });

        Tooltip tip = new Tooltip(imagePath.toString());
        Tooltip.install(tab.getGraphic(), tip);

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setContent(imageView);
        scrollPane.addEventFilter(ScrollEvent.SCROLL, e -> {
            if (e.isShortcutDown() && e.getDeltaY() > 0) {
                // zoom in
                imageView.setFitWidth(imageView.getFitWidth() + 16.0);
            } else if (e.isControlDown() && e.getDeltaY() < 0) {
                // zoom out
                imageView.setFitWidth(imageView.getFitWidth() - 16.0);
            }
        });

        tab.setContent(scrollPane);

        previewTabPane.getTabs().add(tab);
        previewTabPane.getSelectionModel().select(tab);
    }

    @Override
    public void initializeTabChangeListener(TabPane tabPane) {

        ReadOnlyObjectProperty<Tab> itemProperty = tabPane.getSelectionModel().selectedItemProperty();

        tabPane.setOnMouseReleased(event -> {
            Optional.ofNullable(itemProperty)
                    .map(ObservableObjectValue::get)
                    .filter(e -> e instanceof MyTab)
                    .map(e -> (MyTab) e)
                    .map(MyTab::getEditorPane)
                    .ifPresent(EditorPane::focus);
        });

        itemProperty.addListener((observable, oldValue, selectedTab) -> {
            Optional.ofNullable(selectedTab)
                    .filter(e -> e instanceof MyTab)
                    .map(e -> (MyTab) e)
                    .map(MyTab::getEditorPane)
                    .filter(EditorPane::getReady)
                    .ifPresent(editorPane -> {
                        editorPane.resizeAceEditor();
                        editorPane.updatePreviewUrl();
                    });
        });
    }

    @Override
    public ObservableList<Optional<Path>> getClosedPaths() {
        return closedPaths;
    }

    @Override
    public void applyForEachMyTab(Consumer<MyTab> consumer, List<? extends Tab> tabs) {
        for (Tab tab : tabs) {
            if (tab instanceof MyTab) {
                MyTab myTab = (MyTab) tab;
                consumer.accept(myTab);
            }
        }
    }

    @Override
    public void applyForEachMyTab(Consumer<MyTab> consumer) {
        ObservableList<Tab> tabs = controller.getTabPane().getTabs();
        applyForEachMyTab(consumer, tabs);
    }

}
