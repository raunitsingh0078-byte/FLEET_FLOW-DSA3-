package com.fleetflow.visualization;

import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.beans.property.SimpleStringProperty;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** A table whose rows can be coloured by {@link CellState}; used for suffix arrays, tables of results, etc. */
public class RowTableVisualizer extends TableView<String[]> {

    private Map<Integer, CellState> states = new HashMap<>();

    /** @param headers column titles; every row passed to {@link #show} must have one entry per header */
    public RowTableVisualizer(String... headers) {
        for (int c = 0; c < headers.length; c++) {
            final int col = c;
            TableColumn<String[], String> column = new TableColumn<>(headers[c]);
            column.setCellValueFactory(cell -> new SimpleStringProperty(
                    col < cell.getValue().length ? cell.getValue()[col] : ""));
            column.setSortable(false);
            getColumns().add(column);
        }
        setColumnResizePolicy(CONSTRAINED_RESIZE_POLICY);
        setPrefHeight(280);
        setMinHeight(180);
        setPlaceholder(new javafx.scene.control.Label("No rows yet"));
        setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(String[] item, boolean empty) {
                super.updateItem(item, empty);
                CellState state = empty ? null : states.get(getIndex());
                setStyle(state == null ? "" : "-fx-background-color: " + Palette.css(state) + ";");
            }
        });
    }

    /**
     * Replaces the rows.
     *
     * @param rowStates row index to state (missing = default look)
     */
    public void show(List<String[]> rows, Map<Integer, CellState> rowStates) {
        this.states = new HashMap<>(rowStates);
        getItems().setAll(rows);
        refresh();
    }
}
