package com.fleetflow.ui.panel.np;

import com.fleetflow.datastructure.graph.SimpleGraph;
import com.fleetflow.ui.Ui;
import com.fleetflow.util.ValidationException;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

/** Input row for a small undirected graph: number of vertices and an edge list such as {@code 0-1, 1-2}. */
final class GraphInput extends HBox {

    static final int MAX_VERTICES = 12;

    private final TextField vertices = new TextField();
    private final TextField edges = new TextField();

    GraphInput() {
        super(10);
        vertices.setPrefColumnCount(4);
        edges.setPromptText("Edges, e.g. 0-1, 1-2, 2-0");
        HBox.setHgrow(edges, Priority.ALWAYS);
        getChildren().addAll(Ui.label("Vertices", "kv-key"), vertices, Ui.label("Edges", "kv-key"), edges);
    }

    void set(int n, String edgeText) {
        vertices.setText(String.valueOf(n));
        edges.setText(edgeText);
    }

    SimpleGraph read() throws ValidationException {
        int n;
        try {
            n = Integer.parseInt(vertices.getText().trim());
        } catch (NumberFormatException | NullPointerException e) {
            throw new ValidationException("The number of vertices must be a whole number.");
        }
        if (n < 2 || n > MAX_VERTICES) {
            throw new ValidationException("Please use between 2 and " + MAX_VERTICES + " vertices so the exponential search stays fast.");
        }
        try {
            return SimpleGraph.parse(n, edges.getText());
        } catch (IllegalArgumentException e) {
            throw new ValidationException(e.getMessage());
        }
    }
}
