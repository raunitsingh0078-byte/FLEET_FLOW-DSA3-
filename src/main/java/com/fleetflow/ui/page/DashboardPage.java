package com.fleetflow.ui.page;

import com.fleetflow.data.FleetRepository;
import com.fleetflow.model.Shipment;
import com.fleetflow.model.ShipmentEvent;
import com.fleetflow.model.ShipmentStatus;
import com.fleetflow.model.Vehicle;
import com.fleetflow.model.VehicleStatus;
import com.fleetflow.model.Warehouse;
import com.fleetflow.model.WarehouseStatus;
import com.fleetflow.ui.Charts;
import com.fleetflow.ui.Page;
import com.fleetflow.ui.Tables;
import com.fleetflow.ui.Ui;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Main dashboard: KPI cards, five live charts computed from the repository, and recent events. */
public final class DashboardPage implements Page {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd MMM");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd MMM HH:mm");
    private static final double BOTTLENECK_LOAD_PERCENT = 85.0;

    private final FleetRepository repo = FleetRepository.get();
    private final VBox root = new VBox(18);

    @Override
    public String title() {
        return "FLEETFLOW DASHBOARD";
    }

    @Override
    public String subtitle() {
        return "Live overview of shipments, fleet, warehouses and network health";
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
        root.getChildren().setAll(statGrid(), chartGrid());
    }

    // ------------------------------------------------------------------ KPI cards

    private GridPane statGrid() {
        List<Shipment> shipments = repo.shipments();
        long pending = shipments.stream().filter(s -> s.status() == ShipmentStatus.PENDING).count();
        long done = shipments.stream().filter(s -> s.status() == ShipmentStatus.DELIVERED).count();
        long active = repo.vehicles().stream()
                .filter(v -> v.status() == VehicleStatus.IN_TRANSIT || v.status() == VehicleStatus.LOADING).count();
        long load = repo.warehouses().stream().mapToLong(Warehouse::currentLoad).sum();
        long capacity = repo.warehouses().stream().mapToLong(Warehouse::capacity).sum();
        long bottlenecks = repo.warehouses().stream()
                .filter(w -> w.loadPercent() >= BOTTLENECK_LOAD_PERCENT || w.status() == WarehouseStatus.CONGESTED)
                .count();

        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(14);
        Node[] cards = {
                statCard("📦", String.valueOf(shipments.size()), "Total Shipments", "#4c8dff", "All shipments in the system"),
                statCard("🚚", String.valueOf(active), "Active Vehicles", "#21d4a4", "Vehicles in transit or loading"),
                statCard("🧑‍✈️", String.valueOf(repo.drivers().size()), "Drivers", "#a78bfa", "Registered drivers"),
                statCard("🏭", String.valueOf(repo.warehouses().size()), "Warehouses", "#f5a623", "Warehouses and distribution centres"),
                statCard("⏳", String.valueOf(pending), "Pending Deliveries", "#f5a623", "Shipments not yet dispatched"),
                statCard("✅", String.valueOf(done), "Completed Deliveries", "#21d4a4", "Delivered shipments"),
                statCard("🌐", capacity == 0 ? "0%" : String.format("%.1f%%", 100.0 * load / capacity),
                        "Network Utilization", "#4c8dff", "Total warehouse load / total capacity"),
                statCard("⚠️", String.valueOf(bottlenecks), "Bottlenecks", "#ff5c77",
                        "Warehouses at or above " + (int) BOTTLENECK_LOAD_PERCENT + "% load or marked congested")};
        for (int i = 0; i < cards.length; i++) {
            grid.add(cards[i], i % 4, i / 4);
        }
        columns(grid, 4);
        return grid;
    }

    private static VBox statCard(String icon, String value, String caption, String accent, String tip) {
        Label valueLabel = Ui.label(value, "stat-value");
        valueLabel.setStyle("-fx-text-fill: " + accent + ";");
        VBox card = new VBox(4, Ui.label(icon, "stat-icon"), valueLabel, Ui.label(caption, "stat-caption"));
        card.getStyleClass().addAll("card", "stat-card");
        card.setMaxWidth(Double.MAX_VALUE);
        Tooltip.install(card, new Tooltip(tip));
        return card;
    }

    private static void columns(GridPane grid, int count) {
        for (int i = 0; i < count; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(100.0 / count);
            cc.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().add(cc);
        }
    }

    // ------------------------------------------------------------------ charts

    private GridPane chartGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(14);
        grid.add(chartCard(statusChart()), 0, 0);
        grid.add(chartCard(dailyChart()), 1, 0);
        grid.add(chartCard(vehicleChart()), 0, 1);
        grid.add(chartCard(performanceChart()), 1, 1);
        grid.add(chartCard(warehouseChart()), 0, 2);
        grid.add(eventsCard(), 1, 2);
        columns(grid, 2);
        return grid;
    }

    private static VBox chartCard(Node chart) {
        VBox card = Ui.card(chart);
        card.setMinHeight(300);
        return card;
    }

    private PieChart statusChart() {
        Map<ShipmentStatus, Long> counts = new LinkedHashMap<>();
        for (ShipmentStatus s : ShipmentStatus.values()) {
            counts.put(s, repo.shipments().stream().filter(x -> x.status() == s).count());
        }
        Map<String, Number> labelled = new LinkedHashMap<>();
        counts.forEach((status, count) -> labelled.put(status + " (" + count + ")", count));
        PieChart chart = Charts.pie("Shipment Status", labelled);
        chart.setLegendVisible(true);
        return chart;
    }

    private List<LocalDate> lastSevenDays() {
        List<LocalDate> days = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = 6; i >= 0; i--) {
            days.add(today.minusDays(i));
        }
        return days;
    }

    private BarChart<String, Number> dailyChart() {
        Map<String, Number> data = new LinkedHashMap<>();
        for (LocalDate day : lastSevenDays()) {
            data.put(day.format(DAY), repo.shipments().stream().filter(s -> s.date().equals(day)).count());
        }
        return Charts.bar("Daily Shipments", "Shipments", "Shipments", data, true);
    }

    private BarChart<String, Number> vehicleChart() {
        Map<String, Number> data = new LinkedHashMap<>();
        for (Vehicle v : repo.vehicles()) {
            double carried = repo.shipments().stream()
                    .filter(s -> s.vehicleId().equals(v.id())
                            && (s.status() == ShipmentStatus.IN_TRANSIT || s.status() == ShipmentStatus.DELAYED))
                    .mapToDouble(Shipment::weightKg).sum();
            double percent = v.capacityKg() == 0 ? 0 : Math.min(100, 100.0 * carried / v.capacityKg());
            data.put(v.id().replace("VEH-", "V"), Math.round(percent * 10) / 10.0);
        }
        return Charts.bar("Vehicle Utilization (% of capacity carried)", "Load %", "Utilization", data, false);
    }

    private LineChart<String, Number> performanceChart() {
        XYChart.Series<String, Number> delivered = new XYChart.Series<>();
        delivered.setName("Delivered");
        XYChart.Series<String, Number> delayed = new XYChart.Series<>();
        delayed.setName("Delayed");
        for (LocalDate day : lastSevenDays()) {
            String label = day.format(DAY);
            delivered.getData().add(new XYChart.Data<>(label, repo.shipments().stream()
                    .filter(s -> s.date().equals(day) && s.status() == ShipmentStatus.DELIVERED).count()));
            delayed.getData().add(new XYChart.Data<>(label, repo.shipments().stream()
                    .filter(s -> s.date().equals(day) && s.status() == ShipmentStatus.DELAYED).count()));
        }
        NumberAxis y = new NumberAxis();
        y.setLabel("Shipments");
        y.setMinorTickVisible(false);
        y.setTickUnit(1);
        LineChart<String, Number> chart = new LineChart<>(new CategoryAxis(), y);
        chart.setTitle("Delivery Performance");
        chart.getData().add(delivered);
        chart.getData().add(delayed);
        chart.setAnimated(false);
        return chart;
    }

    private BarChart<String, Number> warehouseChart() {
        Map<String, Number> data = new LinkedHashMap<>();
        for (Warehouse w : repo.warehouses()) {
            data.put(w.id(), Math.round(w.loadPercent() * 10) / 10.0);
        }
        return Charts.bar("Warehouse Activity (load %)", "Load %", "Load", data, false);
    }

    // ------------------------------------------------------------------ events

    private VBox eventsCard() {
        TableView<ShipmentEvent> table = new TableView<>();
        table.getColumns().add(Tables.column("Time", 105, e -> e.time().format(TIME)));
        table.getColumns().add(Tables.column("Shipment", 85, ShipmentEvent::shipmentId));
        table.getColumns().add(Tables.column("Event", 120, ShipmentEvent::type));
        table.getColumns().add(Tables.column("Detail", 300, ShipmentEvent::detail));
        table.getItems().setAll(repo.events().stream().limit(12).toList());
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(260);
        VBox card = Ui.card(Ui.label("Recent Shipment Events", "section-title"), table);
        card.setMinHeight(300);
        VBox.setVgrow(table, Priority.ALWAYS);
        return card;
    }
}
