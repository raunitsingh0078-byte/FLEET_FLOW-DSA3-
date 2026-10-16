package com.fleetflow.ui.page;

import com.fleetflow.data.FleetRepository;
import com.fleetflow.model.Driver;
import com.fleetflow.model.Vehicle;
import com.fleetflow.ui.Page;
import com.fleetflow.ui.Tables;
import com.fleetflow.ui.Ui;
import com.fleetflow.ui.panel.flow.BipartiteMatchingPanel;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.Locale;

/** Vehicle and driver tables plus the bipartite-matching driver assignment demo. */
public final class VehiclesDriversPage implements Page {

    private final FleetRepository repo = FleetRepository.get();

    @Override
    public String title() {
        return "Vehicles & Drivers";
    }

    @Override
    public String subtitle() {
        return "Fleet status, driver availability and optimal driver–task assignment (bipartite matching)";
    }

    @Override
    public Node content() {
        TableView<Vehicle> vehicles = new TableView<>(repo.vehicles());
        vehicles.getColumns().add(Tables.column("Vehicle ID", 80, Vehicle::id));
        vehicles.getColumns().add(Tables.column("Type", 105, Vehicle::type));
        vehicles.getColumns().add(Tables.column("Capacity (kg)", 90, v -> String.format(Locale.ROOT, "%,d", v.capacityKg())));
        vehicles.getColumns().add(Tables.column("Location", 90, Vehicle::location));
        vehicles.getColumns().add(Tables.column("Status", 90, v -> v.status().toString()));
        vehicles.getColumns().add(Tables.column("Driver", 130, v -> v.driverId().isEmpty() ? "—" : nameOf(v.driverId())));
        vehicles.getColumns().add(Tables.column("Fuel / energy", 130, v -> bar(v.fuelPercent())));
        vehicles.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        vehicles.setPrefHeight(340);

        TableView<Driver> drivers = new TableView<>(repo.drivers());
        drivers.getColumns().add(Tables.column("Driver ID", 75, Driver::id));
        drivers.getColumns().add(Tables.column("Name", 130, Driver::name));
        drivers.getColumns().add(Tables.column("Availability", 90, d -> d.available() ? "● Available" : "○ On duty"));
        drivers.getColumns().add(Tables.column("Active deliveries", 100, d -> String.valueOf(d.assignedDeliveries())));
        drivers.getColumns().add(Tables.column("Rating", 60, d -> String.format(Locale.ROOT, "★ %.1f", d.rating())));
        drivers.getColumns().add(Tables.column("Experience", 85, d -> d.experienceYears() + " yrs"));
        drivers.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        drivers.setPrefHeight(340);

        VBox vehicleCard = Ui.card(Ui.label("VEHICLES", "section-title"), vehicles);
        VBox driverCard = Ui.card(Ui.label("DRIVERS", "section-title"), drivers);
        HBox.setHgrow(vehicleCard, Priority.ALWAYS);
        HBox.setHgrow(driverCard, Priority.ALWAYS);
        vehicleCard.setPrefWidth(500);
        driverCard.setPrefWidth(500);
        HBox tables = new HBox(14, vehicleCard, driverCard);

        VBox root = new VBox(16, tables, Ui.label("DRIVER ↔ TASK ASSIGNMENT (BIPARTITE MATCHING)", "section-title"), new BipartiteMatchingPanel());
        root.setPadding(new Insets(4, 24, 24, 24));
        return root;
    }

    private String nameOf(String driverId) {
        return repo.drivers().stream().filter(d -> d.id().equals(driverId)).findFirst().map(d -> driverId + " " + d.name()).orElse(driverId);
    }

    private static String bar(int percent) {
        int blocks = Math.round(percent / 20f);
        return "▮".repeat(blocks) + "▯".repeat(5 - blocks) + "  " + percent + "%";
    }
}
