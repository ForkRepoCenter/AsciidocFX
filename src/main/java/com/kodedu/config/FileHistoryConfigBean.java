package com.kodedu.config;

import com.dooapp.fxform.FXForm;
import com.dooapp.fxform.builder.FXFormBuilder;
import com.dooapp.fxform.view.factory.DefaultFactoryProvider;
import com.kodedu.config.factory.FileChooserEditableFactory;
import com.kodedu.controller.ApplicationController;
import com.kodedu.helper.IOHelper;
import com.kodedu.service.ThreadService;
import com.kodedu.service.ui.TabService;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonReader;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.Reader;
import java.nio.file.Path;
import java.util.ResourceBundle;

@Component
public class FileHistoryConfigBean extends ConfigurationBase {

    private BooleanProperty enableFileHistory = new SimpleBooleanProperty(true);
    private BooleanProperty saveOnEachFileSave = new SimpleBooleanProperty(true);
    private IntegerProperty savePeriodically = new SimpleIntegerProperty(-1);
    private BooleanProperty saveUnderCurrentDir = new SimpleBooleanProperty(false);
    private BooleanProperty saveUnderGlobalDir = new SimpleBooleanProperty(true);

    private Logger logger = LoggerFactory.getLogger(FileHistoryConfigBean.class);

    private final ApplicationController controller;
    private final ThreadService threadService;
    private final TabService tabService;

    private final Button saveButton = new Button("Save");
    private final Button loadButton = new Button("Load");
    private final Label infoLabel = new Label();

    @Autowired
    public FileHistoryConfigBean(ApplicationController controller, ThreadService threadService, TabService tabService) {
        super(controller, threadService);
        this.controller = controller;
        this.threadService = threadService;
        this.tabService = tabService;
    }


    @Override
    public String formName() {
        return "File History Settings";
    }

    @Override
    public VBox createForm() {

        FXForm editorConfigForm = new FXFormBuilder<>()
                .resourceBundle(ResourceBundle.getBundle("fileHistoryConfig"))
                .includeAndReorder(
                        "enableFileHistory",
                        "saveOnEachFileSave",
                        "savePeriodically",
                        "saveUnderCurrentDir",
                        "saveUnderGlobalDir"
                )
                .build();

        DefaultFactoryProvider editorConfigFormProvider = new DefaultFactoryProvider();

        FileChooserEditableFactory fileChooserEditableFactory = new FileChooserEditableFactory();
        editorConfigForm.setEditorFactoryProvider(editorConfigFormProvider);

        fileChooserEditableFactory.setOnEdit(tabService::addTab);

        editorConfigForm.setSource(this);

        VBox vBox = new VBox();
        vBox.getChildren().add(editorConfigForm);

        saveButton.setOnAction(this::save);
        loadButton.setOnAction(this::load);
        HBox box = new HBox(5, saveButton, loadButton, infoLabel);
        box.setPadding(new Insets(0, 0, 15, 5));
        vBox.getChildren().add(box);

        return vBox;
    }

    @Override
    public Path getConfigPath() {
        return super.resolveConfigPath("file_history_config.json");
    }

    @Override
    public void load(Path configPath, ActionEvent... actionEvent) {

        fadeOut(infoLabel, "Loading...");

        createConfigFileIfNotExist(configPath);

        Reader fileReader = IOHelper.fileReader(configPath);
        JsonReader jsonReader = Json.createReader(fileReader);

        JsonObject jsonObject = jsonReader.readObject();

        boolean enableFileHistory = jsonObject.getBoolean("enableFileHistory", this.enableFileHistory.getValue());
        boolean saveOnEachFileSave = jsonObject.getBoolean("saveOnEachFileSave", this.saveOnEachFileSave.getValue());
        int savePeriodically = jsonObject.getInt("savePeriodically", this.savePeriodically.getValue());
        boolean saveUnderCurrentDir = jsonObject.getBoolean("saveUnderCurrentDir", this.saveUnderCurrentDir.getValue());
        boolean saveUnderGlobalDir = jsonObject.getBoolean("saveUnderGlobalDir", this.saveUnderGlobalDir.getValue());

        IOHelper.close(jsonReader, fileReader);

        threadService.runActionLater(() -> {

            this.setEnableFileHistory(enableFileHistory);
            this.setSaveOnEachFileSave(saveOnEachFileSave);
            this.setSavePeriodically(savePeriodically);
            this.setSaveUnderCurrentDir(saveUnderCurrentDir);
            this.setSaveUnderGlobalDir(saveUnderGlobalDir);

            fadeOut(infoLabel, "Loaded...");

        });
    }

    @Override
    public void save(ActionEvent... actionEvent) {

        infoLabel.setText("Saving...");
        saveJson(getJSON());
        fadeOut(infoLabel, "Saved...");
    }

    @Override
    public JsonObject getJSON() {
        JsonObjectBuilder objectBuilder = Json.createObjectBuilder();

        objectBuilder
                .add("enableFileHistory", isEnableFileHistory())
                .add("saveOnEachFileSave", isSaveOnEachFileSave())
                .add("savePeriodically", getSavePeriodically())
                .add("saveUnderCurrentDir", isSaveUnderCurrentDir())
                .add("saveUnderGlobalDir", isSaveUnderGlobalDir());

        return objectBuilder.build();
    }

    public boolean isEnableFileHistory() {
        return enableFileHistory.get();
    }

    public BooleanProperty enableFileHistoryProperty() {
        return enableFileHistory;
    }

    public void setEnableFileHistory(boolean enableFileHistory) {
        this.enableFileHistory.set(enableFileHistory);
    }

    public boolean isSaveOnEachFileSave() {
        return saveOnEachFileSave.get();
    }

    public BooleanProperty saveOnEachFileSaveProperty() {
        return saveOnEachFileSave;
    }

    public void setSaveOnEachFileSave(boolean saveOnEachFileSave) {
        this.saveOnEachFileSave.set(saveOnEachFileSave);
    }

    public int getSavePeriodically() {
        return savePeriodically.get();
    }

    public IntegerProperty savePeriodicallyProperty() {
        return savePeriodically;
    }

    public void setSavePeriodically(int savePeriodically) {
        this.savePeriodically.set(savePeriodically);
    }

    public boolean isSaveUnderCurrentDir() {
        return saveUnderCurrentDir.get();
    }

    public BooleanProperty saveUnderCurrentDirProperty() {
        return saveUnderCurrentDir;
    }

    public void setSaveUnderCurrentDir(boolean saveUnderCurrentDir) {
        this.saveUnderCurrentDir.set(saveUnderCurrentDir);
    }

    public boolean isSaveUnderGlobalDir() {
        return saveUnderGlobalDir.get();
    }

    public BooleanProperty saveUnderGlobalDirProperty() {
        return saveUnderGlobalDir;
    }

    public void setSaveUnderGlobalDir(boolean saveUnderGlobalDir) {
        this.saveUnderGlobalDir.set(saveUnderGlobalDir);
    }
}
