package com.fleetflow.ui.page;

import com.fleetflow.algorithm.flow.Dinic;
import com.fleetflow.algorithm.flow.FlowResult;
import com.fleetflow.algorithm.flow.MinCut;
import com.fleetflow.data.FleetRepository;
import com.fleetflow.data.RouteNetwork;
import com.fleetflow.datastructure.graph.FlowNetwork;
import com.fleetflow.model.Warehouse;
import com.fleetflow.ui.Page;
import com.fleetflow.ui.Tables;
import com.fleetflow.ui.Ui;
import com.fleetflow.util.Dialogs;
import com.fleetflow.util.ThemeManager;
import com.fleetflow.visualization.CellState;
import com.fleetflow.visualization.GraphVisualizer;
import com.fleetflow.visualization.GraphVisualizer.GEdge;
import com.fleetflow.visualization.GraphVisualizer.GNode;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Warehouse table plus the logistics network map, with max-flow (Dinic) and bottleneck (min-cut) analysis on the real lanes. */
public final class WarehousesPage implements Page {

    private static final double W = 900;
    private static final double H = 380;

    private final FleetRepository repo = FleetRepository.get();
    private final GraphVisualizer map = new GraphVisualizer(W, H);
    private final ComboBox<String> sourceBox = new ComboBox<>();
    private final ComboBox<String> sinkBox = new ComboBox<>();
    private final Label result = Ui.label("Pick a source and a destination warehouse and press “Analyse flow”.", "result-value");
    private FlowResult lastFlow;
    private MinCut.Result lastCut;
    private List<Warehouse> lastWarehouses = List.of();

    @Override
    public String title() {
        return "Warehouses";
    }

    @Override
    public String subtitle() {
        return "Warehouse status, transport lanes and bottleneck analysis (Dinic max-flow + min-cut)";
    }

    @Override
    public Node content() {
        TableView<Warehouse> table = new TableView<>(repo.warehouses());
        table.getColumns().add(Tables.column("Warehouse ID", 90, Warehouse::id));
        table.getColumns().add(Tables.column("Name", 230, Warehouse::name));
        table.getColumns().add(Tables.column("Location", 120, Warehouse::location));
        table.getColumns().add(Tables.column("Capacity", 80, w -> String.format(Locale.ROOT, "%,d", w.capacity())));
        table.getColumns().add(Tables.column("Current load", 90, w -> String.format(Locale.ROOT, "%,d", w.currentLoad())));
        table.getColumns().add(Tables.column("Load %", 70, w -> String.format(Locale.ROOT, "%.1f%%", w.loadPercent())));
        table.getColumns().add(Tables.column("Status", 100, w -> w.status().toString()));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(230);

        sourceBox.setValue(RouteNetwork.DEFAULT_SOURCE);
        sinkBox.setValue(RouteNetwork.DEFAULT_SINK);
        Button analyse = Ui.button("▶ Analyse flow", "button-primary", "Run Dinic's algorithm and highlight the minimum cut");
        analyse.setOnAction(e -> analyse());
        Button reset = Ui.button("↺ Reset view", "button-secondary", "Show the plain network again");
        reset.setOnAction(e -> {
            lastFlow = null;
            lastCut = null;
            result.setText("Pick a source and a destination warehouse and press “Analyse flow”.");
            paint();
        });
        FlowPane controls = new FlowPane(10, 8, Ui.label("From", "kv-key"), sourceBox, Ui.label("To", "kv-key"), sinkBox, analyse, reset);
        ScrollPane scroll = new ScrollPane(map);
        scroll.getStyleClass().add("viz-scroll");
        scroll.setPrefViewportHeight(H + 8);
        result.setWrapText(true);
        result.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);

        VBox root = new VBox(16, Ui.card(Ui.label("WAREHOUSES & DISTRIBUTION CENTRES", "section-title"), table),
                Ui.card(Ui.label("LOGISTICS NETWORK (lane capacity in pallets/day; node colour = load: green < 60%, amber < 85%, red ≥ 85%)", "section-title"),
                        controls, scroll, result));
        root.setPadding(new Insets(4, 24, 24, 24));
        ThemeManager.addListener(this::paint);
        refreshChoices();
        paint();
        return root;
    }

    @Override
    public void onShow() {
        refreshChoices();
        paint();
    }

    private void refreshChoices() {
        String src = sourceBox.getValue();
        String dst = sinkBox.getValue();
        sourceBox.getItems().setAll(repo.warehouses().stream().map(Warehouse::id).toList());
        sinkBox.getItems().setAll(repo.warehouses().stream().map(Warehouse::id).toList());
        sourceBox.setValue(src != null && sourceBox.getItems().contains(src) ? src : RouteNetwork.DEFAULT_SOURCE);
        sinkBox.setValue(dst != null && sinkBox.getItems().contains(dst) ? dst : RouteNetwork.DEFAULT_SINK);
    }

    private void analyse() {
        String src = sourceBox.getValue();
        String dst = sinkBox.getValue();
        if (src == null || dst == null) {
            Dialogs.warning("Choose warehouses", "Please pick both a source and a destination warehouse.");
            return;
        }
        if (src.equals(dst)) {
            Dialogs.warning("Choose different warehouses", "The source and the destination must be different warehouses.");
            return;
        }
        List<Warehouse> ws = new ArrayList<>(repo.warehouses());
        FlowNetwork net = new FlowNetwork(ws.size());
        for (RouteNetwork.Route r : RouteNetwork.routes()) {
            int a = indexOf(ws, r.from());
            int b = indexOf(ws, r.to());
            if (a >= 0 && b >= 0) net.addEdge(a, b, r.capacity());
        }
        int s = indexOf(ws, src);
        int t = indexOf(ws, dst);
        lastFlow = Dinic.maxFlow(net, s, t);
        lastCut = MinCut.compute(lastFlow.network(), s);
        StringBuilder cut = new StringBuilder();
        List<RouteNetwork.Route> routes = RouteNetwork.routes();
        int edgeIndex = 0;
        for (RouteNetwork.Route r : routes) {
            if (indexOf(ws, r.from()) >= 0 && indexOf(ws, r.to()) >= 0) {
                if (lastCut.cutEdges().contains(edgeIndex)) cut.append(r.from()).append("→").append(r.to()).append(" (").append(r.capacity()).append(")  ");
                edgeIndex++;
            }
        }
        result.setText("Maximum flow " + src + " → " + dst + " = " + lastFlow.maxFlow() + " pallets/day (Dinic: " + lastFlow.phases() + " phase(s), "
                + lastFlow.augmentations() + " augmenting paths).\nBottleneck (minimum cut, capacity " + lastCut.capacity() + "): "
                + (cut.length() == 0 ? "no path between these warehouses" : cut.toString().trim()));
        paint();
    }

    private static int indexOf(List<Warehouse> ws, String id) {
        for (int i = 0; i < ws.size(); i++) if (ws.get(i).id().equals(id)) return i;
        return -1;
    }

    private void paint() {
        List<Warehouse> ws = new ArrayList<>(repo.warehouses());
        lastWarehouses = ws;
        List<GNode> nodes = new ArrayList<>();
        for (int i = 0; i < ws.size(); i++) {
            Warehouse w = ws.get(i);
            GNode n = new GNode(i, w.id(), 60 + w.mapX() * (W - 120), 40 + w.mapY() * (H - 110));
            n.sub = w.location().split(",")[0] + " · " + Math.round(w.loadPercent()) + "%";
            n.state = w.loadPercent() >= 85 ? CellState.MISMATCH : w.loadPercent() >= 60 ? CellState.WINDOW : CellState.DONE;
            if (lastFlow != null && w.id().equals(sourceBox.getValue())) n.ring = CellState.MATCH;
            if (lastFlow != null && w.id().equals(sinkBox.getValue())) n.ring = CellState.HIGHLIGHT;
            nodes.add(n);
        }
        List<GEdge> edges = new ArrayList<>();
        int k = 0;
        int[] flows = lastFlow == null ? null : lastFlow.network().flows();
        for (RouteNetwork.Route r : RouteNetwork.routes()) {
            int a = indexOf(ws, r.from());
            int b = indexOf(ws, r.to());
            if (a < 0 || b < 0) continue;
            GEdge e = new GEdge(a, b);
            e.label = flows == null ? String.valueOf(r.capacity()) : flows[k] + "/" + r.capacity();
            if (flows != null && flows[k] > 0) e.state = flows[k] == r.capacity() ? CellState.WINDOW : CellState.DONE;
            if (lastCut != null && lastCut.cutEdges().contains(k)) {
                e.state = CellState.MISMATCH;
                e.width = 4.5;
                e.label += " ✂";
            }
            edges.add(e);
            k++;
        }
        map.render(nodes, edges);
    }
}
