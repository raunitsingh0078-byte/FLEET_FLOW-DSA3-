package com.fleetflow.ui.page;

import com.fleetflow.controller.NavigationController;
import com.fleetflow.ui.AlgorithmCatalog;
import com.fleetflow.ui.AlgorithmCatalog.Entry;
import com.fleetflow.ui.Page;
import com.fleetflow.ui.Ui;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Explorer of every implemented algorithm, grouped by module, each with a Run / Visualize button that jumps to its page. */
public final class AlgorithmExplorerPage implements Page {

    private final NavigationController navigation;
    private final VBox list = new VBox(18);
    private final TextField search = new TextField();
    private final ComboBox<String> moduleFilter = new ComboBox<>();

    public AlgorithmExplorerPage(NavigationController navigation) {
        this.navigation = navigation;
    }

    @Override
    public String title() {
        return "Algorithm Explorer";
    }

    @Override
    public String subtitle() {
        return "All " + AlgorithmCatalog.entries().size() + " implemented algorithms, grouped by module M1–M6";
    }

    @Override
    public Node content() {
        search.setPromptText("Search algorithm name or problem…");
        search.textProperty().addListener((o, a, b) -> rebuild());
        moduleFilter.getItems().add("All modules");
        moduleFilter.getItems().addAll(AlgorithmCatalog.M1, AlgorithmCatalog.M2, AlgorithmCatalog.M3, AlgorithmCatalog.M4, AlgorithmCatalog.M5, AlgorithmCatalog.M6);
        moduleFilter.setValue("All modules");
        moduleFilter.setOnAction(e -> rebuild());
        HBox controls = new HBox(10, search, moduleFilter);
        HBox.setHgrow(search, Priority.ALWAYS);
        rebuild();
        VBox root = new VBox(14, controls, list);
        root.setPadding(new Insets(4, 24, 24, 24));
        return root;
    }

    private void rebuild() {
        String q = search.getText() == null ? "" : search.getText().trim().toLowerCase();
        String moduleWanted = moduleFilter.getValue();
        Map<String, List<Entry>> byModule = new LinkedHashMap<>();
        for (Entry e : AlgorithmCatalog.entries()) {
            if (!q.isEmpty() && !e.name().toLowerCase().contains(q) && !e.problem().toLowerCase().contains(q)) continue;
            if (moduleWanted != null && !moduleWanted.equals("All modules") && !moduleWanted.equals(e.module())) continue;
            byModule.computeIfAbsent(e.module(), m -> new java.util.ArrayList<>()).add(e);
        }
        list.getChildren().clear();
        if (byModule.isEmpty()) {
            list.getChildren().add(Ui.label("No algorithm matches this search.", "muted"));
            return;
        }
        for (Map.Entry<String, List<Entry>> group : byModule.entrySet()) {
            list.getChildren().add(Ui.label(group.getKey(), "algo-title"));
            GridPane grid = new GridPane();
            grid.setHgap(14);
            grid.setVgap(14);
            int col = 0;
            int row = 0;
            for (Entry e : group.getValue()) {
                grid.add(card(e), col, row);
                col++;
                if (col == 2) {
                    col = 0;
                    row++;
                }
            }
            for (int i = 0; i < 2; i++) {
                javafx.scene.layout.ColumnConstraints cc = new javafx.scene.layout.ColumnConstraints();
                cc.setPercentWidth(50);
                cc.setHgrow(Priority.ALWAYS);
                grid.getColumnConstraints().add(cc);
            }
            list.getChildren().add(grid);
        }
    }

    private VBox card(Entry e) {
        Button run = Ui.button("▶ Run", "button-primary", "Open " + e.route() + " and run this algorithm with demo data");
        run.setOnAction(ev -> navigation.goTo(e.route(), e.name(), true));
        Button view = Ui.button("Visualize", "button-secondary", "Open " + e.route() + " without running it");
        view.setOnAction(ev -> navigation.goTo(e.route(), e.name(), false));
        GridPane facts = new GridPane();
        facts.setHgap(10);
        facts.setVgap(3);
        String[][] rows = {{"Problem", e.problem()}, {"Input", e.input()}, {"Output", e.output()}, {"Time", e.time()}, {"Space", e.space()}};
        for (int i = 0; i < rows.length; i++) {
            facts.add(Ui.label(rows[i][0] + ":", "kv-key"), 0, i);
            Ui.label(rows[i][1], "muted");
            javafx.scene.control.Label v = Ui.label(rows[i][1], "muted");
            v.setWrapText(true);
            facts.add(v, 1, i);
        }
        javafx.scene.control.Label explain = Ui.label(e.explanation(), "muted");
        explain.setWrapText(true);
        VBox card = Ui.card(Ui.label(e.name(), "section-title"), facts, explain, new HBox(8, run, view));
        card.setMaxWidth(Double.MAX_VALUE);
        return card;
    }
}
