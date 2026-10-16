package com.fleetflow.ui.page;

import com.fleetflow.algorithm.string.KmpMatcher;
import com.fleetflow.algorithm.string.RabinKarp;
import com.fleetflow.data.FleetRepository;
import com.fleetflow.model.Driver;
import com.fleetflow.model.Priority;
import com.fleetflow.model.Shipment;
import com.fleetflow.model.ShipmentStatus;
import com.fleetflow.ui.Page;
import com.fleetflow.ui.ShipmentForm;
import com.fleetflow.ui.Tables;
import com.fleetflow.ui.Ui;
import com.fleetflow.util.Dialogs;
import com.fleetflow.util.ThemeManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/** Shipment management: add, edit, delete, details, filter, sort and search (KMP / Rabin-Karp). */
public final class ShipmentsPage implements Page {

    private static final String KMP = "KMP";
    private static final String RK = "Rabin-Karp";
    private static final int RK_MODULUS = 1_000_000_007;

    private final FleetRepository repo = FleetRepository.get();
    private final FilteredList<Shipment> filtered = new FilteredList<>(repo.shipments());
    private final TableView<Shipment> table = new TableView<>();
    private final TextField searchField = new TextField();
    private final ComboBox<String> algorithmBox = new ComboBox<>();
    private final ComboBox<Object> statusFilter = new ComboBox<>();
    private final ComboBox<Object> priorityFilter = new ComboBox<>();
    private final Label searchStats = Ui.label("", "muted");
    private final Label countLabel = Ui.label("", "muted");
    private Set<String> searchMatches = null;

    @Override
    public String title() {
        return "Shipments";
    }

    @Override
    public String subtitle() {
        return "Manage shipments · search runs KMP or Rabin-Karp over every record";
    }

    @Override
    public Node content() {
        searchField.setPromptText("Search ID, package code, customer, address, description …");
        searchField.setPrefColumnCount(28);
        algorithmBox.getItems().addAll(KMP, RK);
        algorithmBox.setValue(KMP);
        statusFilter.getItems().add("All statuses");
        statusFilter.getItems().addAll((Object[]) ShipmentStatus.values());
        statusFilter.setValue("All statuses");
        priorityFilter.getItems().add("All priorities");
        priorityFilter.getItems().addAll((Object[]) Priority.values());
        priorityFilter.setValue("All priorities");

        searchField.textProperty().addListener((o, a, b) -> applyFilters());
        algorithmBox.setOnAction(e -> applyFilters());
        statusFilter.setOnAction(e -> applyFilters());
        priorityFilter.setOnAction(e -> applyFilters());

        Button add = Ui.button("＋ Add", "button-primary", "Add a new shipment");
        Button edit = Ui.button("✎ Edit", "button-secondary", "Edit the selected shipment");
        Button delete = Ui.button("✕ Delete", "button-danger", "Delete the selected shipment");
        Button details = Ui.button("ⓘ Details", "button-secondary", "Show all details of the selected shipment");
        Button reset = Ui.button("↺ Reset demo data", "button-secondary", "Discard saved changes and restore the prepared demo shipments");
        for (Button b : new Button[]{edit, delete, details}) b.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        add.setOnAction(e -> addShipment());
        edit.setOnAction(e -> editShipment());
        delete.setOnAction(e -> deleteShipment());
        details.setOnAction(e -> showDetails());
        reset.setOnAction(e -> resetData());

        buildTable();
        SortedList<Shipment> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sorted);
        table.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && table.getSelectionModel().getSelectedItem() != null) showDetails();
        });
        table.setPrefHeight(520);
        filtered.addListener((javafx.collections.ListChangeListener<Shipment>) c -> updateCount());

        FlowPane filters = new FlowPane(10, 8, searchField, Ui.label("Search with", "kv-key"), algorithmBox, statusFilter, priorityFilter);
        FlowPane actions = new FlowPane(8, 8, add, edit, delete, details, reset);
        VBox card = Ui.card(filters, searchStats, actions, table, countLabel);
        VBox.setVgrow(table, javafx.scene.layout.Priority.ALWAYS);
        VBox root = new VBox(card);
        root.setPadding(new Insets(4, 24, 24, 24));
        applyFilters();
        return root;
    }

    @Override
    public void onShow() {
        String problem = repo.takeStorageProblem();
        if (problem != null) {
            Dialogs.warning("Saved data problem", problem);
        }
        applyFilters();
    }

    private void buildTable() {
        table.getColumns().add(Tables.column("Shipment ID", 90, Shipment::id));
        table.getColumns().add(Tables.column("Package code", 105, Shipment::packageCode));
        table.getColumns().add(Tables.column("Customer", 130, Shipment::customer));
        table.getColumns().add(Tables.column("Source", 85, Shipment::source));
        table.getColumns().add(Tables.column("Destination", 90, Shipment::destination));
        table.getColumns().add(Tables.column("Delivery address", 230, Shipment::address));
        table.getColumns().add(Tables.column("Status", 85, s -> s.status().toString()));
        table.getColumns().add(Tables.column("Priority", 70, s -> s.priority().toString()));
        TableColumn<Shipment, String> weight = Tables.column("Weight (kg)", 85, s -> String.format(Locale.ROOT, "%.1f", s.weightKg()));
        weight.setComparator((a, b) -> Double.compare(Double.parseDouble(a), Double.parseDouble(b)));
        table.getColumns().add(weight);
        table.getColumns().add(Tables.column("Driver", 130, s -> driverName(s.driverId())));
        table.getColumns().add(Tables.column("Vehicle", 75, s -> s.vehicleId().isEmpty() ? "—" : s.vehicleId()));
        table.getColumns().add(Tables.column("Date", 90, s -> s.date().toString()));
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("No shipments match the current search / filters"));
    }

    private String driverName(String id) {
        if (id.isEmpty()) return "Unassigned";
        return repo.drivers().stream().filter(d -> d.id().equals(id)).findFirst().map(Driver::name).orElse(id);
    }

    // ------------------------------------------------------------------ search / filter

    private static String searchableText(Shipment s) {
        return String.join(" ", s.id(), s.packageCode(), s.customer(), s.source(), s.destination(), s.address(), s.description(),
                s.status().toString(), s.priority().toString(), s.driverId(), s.vehicleId()).toLowerCase(Locale.ROOT);
    }

    /** Runs the chosen string-matching algorithm over all records and rebuilds the table predicate. */
    private void applyFilters() {
        String pattern = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        searchMatches = null;
        if (pattern.isEmpty()) {
            searchStats.setText("Type to search – the table is filtered with " + algorithmBox.getValue() + ".");
        } else {
            long comparisons = 0;
            Set<String> hits = new HashSet<>();
            long start = System.nanoTime();
            boolean kmp = KMP.equals(algorithmBox.getValue());
            for (Shipment s : repo.shipments()) {
                String text = searchableText(s);
                if (kmp) {
                    KmpMatcher.Result r = KmpMatcher.search(text, pattern);
                    comparisons += r.comparisons();
                    if (!r.matches().isEmpty()) hits.add(s.id());
                } else {
                    RabinKarp.Result r = RabinKarp.search(text, pattern, RK_MODULUS);
                    comparisons += (long) r.hashComparisons() + r.charComparisons();
                    if (!r.matches().isEmpty()) hits.add(s.id());
                }
            }
            double ms = (System.nanoTime() - start) / 1e6;
            searchMatches = hits;
            searchStats.setText(String.format("%s scanned %d record(s): %d match(es), %,d %s comparisons, %.3f ms (measured).", algorithmBox.getValue(),
                    repo.shipments().size(), hits.size(), comparisons, kmp ? "character" : "hash + character", ms));
        }
        Object st = statusFilter.getValue();
        Object pr = priorityFilter.getValue();
        filtered.setPredicate(s -> (searchMatches == null || searchMatches.contains(s.id()))
                && (!(st instanceof ShipmentStatus) || s.status() == st)
                && (!(pr instanceof Priority) || s.priority() == pr));
        updateCount();
    }

    private void updateCount() {
        countLabel.setText("Showing " + filtered.size() + " of " + repo.shipments().size() + " shipments  ·  click a column header to sort  ·  double-click a row for details");
    }

    // ------------------------------------------------------------------ actions

    private void persist() {
        String problem = repo.saveShipments();
        if (problem != null) {
            Dialogs.warning("Not saved", problem);
        }
    }

    private void addShipment() {
        Optional<Shipment> result = ShipmentForm.show(null);
        result.ifPresent(s -> {
            repo.shipments().add(s);
            persist();
            applyFilters();
            table.getSelectionModel().select(s);
        });
    }

    private void editShipment() {
        Shipment selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        ShipmentForm.show(selected).ifPresent(updated -> {
            int index = repo.shipments().indexOf(selected);
            if (index >= 0) repo.shipments().set(index, updated);
            persist();
            applyFilters();
        });
    }

    private void deleteShipment() {
        Shipment selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete shipment " + selected.id() + " (" + selected.customer() + ")? This cannot be undone.",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Delete shipment");
        confirm.setHeaderText("Are you sure?");
        ThemeManager.style(confirm.getDialogPane());
        if (confirm.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            repo.shipments().remove(selected);
            persist();
            applyFilters();
        }
    }

    private void showDetails() {
        Shipment s = table.getSelectionModel().getSelectedItem();
        if (s == null) return;
        Dialogs.info("Shipment " + s.id(), String.join("\n",
                "Package code:  " + s.packageCode(), "Customer:  " + s.customer(), "Route:  " + s.source() + " → " + s.destination(),
                "Address:  " + s.address(), "Description:  " + s.description(), "Status:  " + s.status() + "   Priority:  " + s.priority(),
                String.format(Locale.ROOT, "Weight:  %.1f kg", s.weightKg()), "Driver:  " + driverName(s.driverId()),
                "Vehicle:  " + (s.vehicleId().isEmpty() ? "—" : s.vehicleId()), "Date:  " + s.date()));
    }

    private void resetData() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Discard all saved changes and restore the prepared demo data?", ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Reset demo data");
        confirm.setHeaderText("Restore demo data?");
        ThemeManager.style(confirm.getDialogPane());
        if (confirm.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            repo.resetToDemoData();
            applyFilters();
        }
    }
}
