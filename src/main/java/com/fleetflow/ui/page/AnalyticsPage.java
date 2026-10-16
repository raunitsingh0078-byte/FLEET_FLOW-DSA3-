package com.fleetflow.ui.page;

import com.fleetflow.algorithm.flow.Dinic;
import com.fleetflow.algorithm.flow.FlowResult;
import com.fleetflow.data.FleetRepository;
import com.fleetflow.data.RouteNetwork;
import com.fleetflow.datastructure.graph.FlowNetwork;
import com.fleetflow.model.Driver;
import com.fleetflow.model.Priority;
import com.fleetflow.model.Shipment;
import com.fleetflow.model.ShipmentStatus;
import com.fleetflow.model.Warehouse;
import com.fleetflow.ui.Charts;
import com.fleetflow.ui.Page;
import com.fleetflow.ui.Tables;
import com.fleetflow.ui.Ui;
import com.fleetflow.util.AlgorithmStats;
import com.fleetflow.util.Geo;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Analytics: KPIs and charts computed from the data, the flow network, and measured algorithm run statistics. */
public final class AnalyticsPage implements Page {

    private final FleetRepository repo = FleetRepository.get();
    private final VBox root = new VBox(16);

    @Override
    public String title() {
        return "Analytics";
    }

    @Override
    public String subtitle() {
        return "Shipment, fleet and network analytics · algorithm statistics are measured, never invented";
    }

    @Override
    public Node content() {
        root.setPadding(new Insets(4, 24, 24, 24));
        rebuild();
        return root;
    }

    @Override
    public void onShow() {
        rebuild();
    }

    private void rebuild() {
        root.getChildren().setAll(kpis(), charts(), statsCard());
    }

    private static VBox kpi(String value, String caption, String tip) {
        VBox card = new VBox(4, Ui.label(value, "stat-value"), Ui.label(caption, "stat-caption"));
        card.getStyleClass().addAll("card", "stat-card");
        card.setMaxWidth(Double.MAX_VALUE);
        Tooltip.install(card, new Tooltip(tip));
        return card;
    }

    private GridPane kpis() {
        List<Shipment> all = repo.shipments();
        long done = all.stream().filter(s -> s.status() == ShipmentStatus.DELIVERED).count();
        double km = all.stream().mapToDouble(s -> Geo.cityDistanceKm(s.source(), s.destination())).average().orElse(0);
        double kg = all.stream().mapToDouble(Shipment::weightKg).sum();
        String utilisation = "n/a";
        String bottleneck = "";
        FlowResult flow = routeFlow();
        if (flow != null) {
            int sourceCapacity = 0;
            for (RouteNetwork.Route r : RouteNetwork.routes()) if (r.from().equals(RouteNetwork.DEFAULT_SOURCE)) sourceCapacity += r.capacity();
            utilisation = sourceCapacity == 0 ? "n/a" : String.format(Locale.ROOT, "%.0f%%", 100.0 * flow.maxFlow() / sourceCapacity);
            bottleneck = "Dinic max flow " + flow.maxFlow() + " pallets/day";
        }
        GridPane g = new GridPane();
        g.setHgap(14);
        g.setVgap(14);
        Node[] cards = {
                kpi(String.valueOf(all.size()), "Shipment volume", String.format(Locale.ROOT, "%,.0f kg in total", kg)),
                kpi(all.isEmpty() ? "—" : String.format(Locale.ROOT, "%.0f%% / %.0f%%", 100.0 * done / all.size(), 100.0 * (all.size() - done) / all.size()), "Completed vs pending", "Delivered vs all other shipments"),
                kpi(String.format(Locale.ROOT, "%,.0f km", km), "Average delivery distance", "Great-circle distance source → destination city"),
                kpi(utilisation, "Network flow utilisation", "Max flow " + RouteNetwork.DEFAULT_SOURCE + "→" + RouteNetwork.DEFAULT_SINK + " ÷ capacity of the source's lanes. " + bottleneck)};
        for (int i = 0; i < cards.length; i++) g.add(cards[i], i, 0);
        for (int i = 0; i < 4; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(25);
            cc.setHgrow(javafx.scene.layout.Priority.ALWAYS);
            g.getColumnConstraints().add(cc);
        }
        return g;
    }

    private FlowResult routeFlow() {
        List<Warehouse> ws = new ArrayList<>(repo.warehouses());
        int s = -1;
        int t = -1;
        for (int i = 0; i < ws.size(); i++) {
            if (ws.get(i).id().equals(RouteNetwork.DEFAULT_SOURCE)) s = i;
            if (ws.get(i).id().equals(RouteNetwork.DEFAULT_SINK)) t = i;
        }
        if (s < 0 || t < 0) return null;
        FlowNetwork net = new FlowNetwork(ws.size());
        for (RouteNetwork.Route r : RouteNetwork.routes()) {
            int a = -1;
            int b = -1;
            for (int i = 0; i < ws.size(); i++) {
                if (ws.get(i).id().equals(r.from())) a = i;
                if (ws.get(i).id().equals(r.to())) b = i;
            }
            if (a >= 0 && b >= 0) net.addEdge(a, b, r.capacity());
        }
        return Dinic.maxFlow(net, s, t);
    }

    private GridPane charts() {
        Map<String, Number> byDestination = new LinkedHashMap<>();
        for (String city : Geo.CITIES.keySet()) {
            long c = repo.shipments().stream().filter(s -> s.destination().equals(city)).count();
            if (c > 0) byDestination.put(city, c);
        }
        Map<String, Number> done = new LinkedHashMap<>();
        Map<String, Number> open = new LinkedHashMap<>();
        for (Priority p : Priority.values()) {
            done.put(p.toString(), repo.shipments().stream().filter(s -> s.priority() == p && s.status() == ShipmentStatus.DELIVERED).count());
            open.put(p.toString(), repo.shipments().stream().filter(s -> s.priority() == p && s.status() != ShipmentStatus.DELIVERED).count());
        }
        Map<String, Number> allocation = new LinkedHashMap<>();
        for (Driver d : repo.drivers()) allocation.put(d.id().replace("DRV-", "D"), d.assignedDeliveries());
        Map<String, Number> load = new LinkedHashMap<>();
        Map<String, Number> capacity = new LinkedHashMap<>();
        for (Warehouse w : repo.warehouses()) {
            load.put(w.id(), w.currentLoad());
            capacity.put(w.id(), w.capacity());
        }
        Map<String, Number> laneCap = new LinkedHashMap<>();
        Map<String, Number> laneFlow = new LinkedHashMap<>();
        FlowResult flow = routeFlow();
        int[] flows = flow == null ? new int[0] : flow.network().flows();
        int k = 0;
        for (RouteNetwork.Route r : RouteNetwork.routes()) {
            String label = r.from().replace("WH-", "") + "→" + r.to().replace("WH-", "");
            laneCap.put(label, r.capacity());
            laneFlow.put(label, k < flows.length ? flows[k] : 0);
            k++;
        }
        Map<String, Map<String, Number>> priority = new LinkedHashMap<>();
        priority.put("Delivered", done);
        priority.put("Pending / active", open);
        Map<String, Map<String, Number>> loadSeries = new LinkedHashMap<>();
        loadSeries.put("Current load", load);
        loadSeries.put("Capacity", capacity);
        Map<String, Map<String, Number>> laneSeries = new LinkedHashMap<>();
        laneSeries.put("Lane capacity", laneCap);
        laneSeries.put("Flow (max-flow solution)", laneFlow);

        GridPane g = new GridPane();
        g.setHgap(14);
        g.setVgap(14);
        Node[] cards = {
                chartCard(Charts.bar("Shipment volume by destination", "Shipments", "Shipments", byDestination, true)),
                chartCard(Charts.grouped("Completed vs pending by priority", "Shipments", priority, true, true)),
                chartCard(Charts.bar("Driver allocation (active deliveries)", "Deliveries", "Deliveries", allocation, true)),
                chartCard(Charts.grouped("Warehouse load vs capacity", "Units", loadSeries, false, true)),
                chartCard(Charts.grouped("Network flow utilisation per lane (Dinic, " + RouteNetwork.DEFAULT_SOURCE + " → " + RouteNetwork.DEFAULT_SINK + ")", "Pallets/day", laneSeries, true, true))};
        for (int i = 0; i < cards.length; i++) g.add(cards[i], i % 2, i / 2);
        for (int i = 0; i < 2; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(50);
            cc.setHgrow(javafx.scene.layout.Priority.ALWAYS);
            g.getColumnConstraints().add(cc);
        }
        return g;
    }

    private static VBox chartCard(Node chart) {
        VBox card = Ui.card(chart);
        card.setMinHeight(300);
        return card;
    }

    private VBox statsCard() {
        TableView<AlgorithmStats.Row> table = new TableView<>();
        table.getColumns().add(Tables.column("Algorithm", 320, AlgorithmStats.Row::algorithm));
        table.getColumns().add(Tables.column("Runs", 60, r -> String.valueOf(r.runs())));
        table.getColumns().add(Tables.column("Last run (ms)", 110, r -> String.format(Locale.ROOT, "%.3f", r.lastMillis())));
        table.getColumns().add(Tables.column("Average (ms)", 110, r -> String.format(Locale.ROOT, "%.3f", r.averageMillis())));
        table.getColumns().add(Tables.column("Last comparisons / scans", 170, r -> r.lastComparisons() < 0 ? "n/a" : String.format(Locale.ROOT, "%,d", r.lastComparisons())));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getItems().setAll(AlgorithmStats.snapshot());
        table.setPrefHeight(300);
        table.setPlaceholder(new Label("No algorithm has been run yet – use Demo Mode or any algorithm page, then come back."));
        return Ui.card(Ui.label("ALGORITHM EXECUTION STATISTICS (measured in this session)", "section-title"), table);
    }
}
