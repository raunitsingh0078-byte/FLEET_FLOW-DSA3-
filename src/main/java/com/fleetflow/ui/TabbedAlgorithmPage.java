package com.fleetflow.ui;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.VBox;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Base class of pages that hold one {@link AlgorithmPanel} per tab. */
public abstract class TabbedAlgorithmPage implements Page {

    private final TabPane tabs = new TabPane();
    private final Map<String, Tab> tabByAlgorithm = new LinkedHashMap<>();
    private final Map<String, AlgorithmPanel> panelByAlgorithm = new LinkedHashMap<>();

    /**
     * Adds a tab.
     *
     * @param algorithms catalogue names of all algorithms this panel can demonstrate
     */
    protected final void addPanel(String tabTitle, AlgorithmPanel panel, String... algorithms) {
        Tab tab = new Tab(tabTitle, panel);
        tab.setClosable(false);
        tabs.getTabs().add(tab);
        for (String algorithm : algorithms) {
            tabByAlgorithm.put(algorithm, tab);
            panelByAlgorithm.put(algorithm, panel);
        }
    }

    @Override
    public Node content() {
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        VBox root = new VBox(tabs);
        root.setPadding(new Insets(4, 24, 24, 24));
        return root;
    }

    @Override
    public void focus(String algorithm, boolean run) {
        Tab tab = tabByAlgorithm.get(algorithm);
        if (tab == null) {
            return;
        }
        tabs.getSelectionModel().select(tab);
        AlgorithmPanel panel = panelByAlgorithm.get(algorithm);
        panel.prefer(algorithm);
        if (run) {
            panel.runDemo();
        }
    }

    /** @return every panel of this page (used by tests and the smoke harness). */
    public List<AlgorithmPanel> panels() {
        return List.copyOf(panelByAlgorithm.values().stream().distinct().toList());
    }
}
