package com.fleetflow.ui;

import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.TableColumn;

import java.util.function.Function;

/** Helper for building read-only table columns from lambdas. */
public final class Tables {

    private Tables() {
    }

    public static <T> TableColumn<T, String> column(String title, double width, Function<T, String> extractor) {
        TableColumn<T, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new SimpleStringProperty(extractor.apply(cell.getValue())));
        column.setPrefWidth(width);
        return column;
    }
}
