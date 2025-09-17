package com.kodedu.other;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TableColumn;
import javafx.util.Callback;

import java.util.function.Function;

public class RecordCellFactory implements Callback<TableColumn.CellDataFeatures<RevContent, String>, ObservableValue<String>> {

    private final Function<RevContent, String> revContentConsumer;

    public RecordCellFactory(Function<RevContent, String> revContentConsumer) {
        this.revContentConsumer = revContentConsumer;
    }

    @Override
    public ObservableValue<String> call(TableColumn.CellDataFeatures<RevContent, String> param) {
        RevContent value = param.getValue();
        String content = revContentConsumer.apply(value);
        return new SimpleStringProperty(content);
    }
}
