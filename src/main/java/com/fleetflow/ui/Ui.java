package com.fleetflow.ui;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/** Factory helpers that keep widget styling consistent across pages. */
public final class Ui {

    private Ui() {
    }

    public static Label label(String text, String... styleClasses) {
        Label label = new Label(text);
        label.getStyleClass().addAll(styleClasses);
        return label;
    }

    public static Button button(String text, String styleClass, String tooltip) {
        Button button = new Button(text);
        button.getStyleClass().add(styleClass);
        if (tooltip != null) {
            button.setTooltip(new Tooltip(tooltip));
        }
        return button;
    }

    /** A rounded panel with the design-system card style. */
    public static VBox card(Node... children) {
        VBox box = new VBox(10, children);
        box.getStyleClass().add("card");
        return box;
    }

    public static HBox row(double spacing, Node... children) {
        HBox box = new HBox(spacing, children);
        box.setPadding(Insets.EMPTY);
        return box;
    }
}
