package com.fleetflow.ui.page;

import com.fleetflow.controller.NavigationController;
import com.fleetflow.ui.AlgorithmCatalog;
import com.fleetflow.ui.AlgorithmCatalog.Entry;
import com.fleetflow.ui.Page;
import com.fleetflow.ui.Ui;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;

/** Presentation Demo Mode: one-click walk through ten prepared algorithms in classroom order, each with the full pipeline. */
public final class DemoModePage implements Page {

    private static final String[] PIPELINE = {"REAL PROBLEM", "DSA TECHNIQUE", "INPUT", "ALGORITHM", "VISUALIZATION", "OUTPUT", "COMPLEXITY", "LOGISTICS APPLICATION"};

    private final NavigationController navigation;
    private int current = 0;

    public DemoModePage(NavigationController navigation) {
        this.navigation = navigation;
    }

    @Override
    public String title() {
        return "Presentation Demo Mode";
    }

    @Override
    public String subtitle() {
        return "A ready classroom walkthrough of ten algorithms across all six modules";
    }

    @Override
    public Node content() {
        Label pipeline = Ui.label(String.join("  →  ", PIPELINE), "muted");
        pipeline.setWrapText(true);
        VBox steps = new VBox(10);
        List<String> sequence = AlgorithmCatalog.demoSequence();
        for (int i = 0; i < sequence.size(); i++) {
            Entry e = AlgorithmCatalog.byName(sequence.get(i));
            steps.getChildren().add(demoRow(i + 1, e));
        }
        Button restart = Ui.button("↺ Restart from step 1", "button-secondary", "Reset the demo sequence");
        restart.setOnAction(ev -> current = 0);
        VBox root = new VBox(14,
                Ui.label("Suggested presentation pipeline for every algorithm:", "section-title"), pipeline,
                Ui.label("Click “Run next” to jump to each algorithm in order, with its demo data loaded and executed automatically.", "muted"),
                restart, steps);
        root.setPadding(new Insets(4, 24, 24, 24));
        return root;
    }

    private HBox demoRow(int number, Entry e) {
        Label num = Ui.label(String.valueOf(number), "stat-value");
        num.setMinWidth(36);
        VBox text = new VBox(2, Ui.label(e.name(), "algo-title"), Ui.label(e.module() + " · " + e.route() + " · " + e.application(), "muted"));
        ((Label) text.getChildren().get(1)).setWrapText(true);
        HBox.setHgrow(text, Priority.ALWAYS);
        Button run = Ui.button("▶ Run this step", "button-primary", "Open " + e.route() + " and run " + e.name() + " with demo data");
        run.setOnAction(ev -> {
            navigation.goTo(e.route(), e.name(), true);
            current = number;
        });
        HBox row = new HBox(14, num, text, run);
        row.getStyleClass().add("card");
        row.setPadding(new Insets(10, 14, 10, 14));
        row.setStyle(current == number ? "-fx-border-color: -ff-accent;" : "");
        return row;
    }
}
