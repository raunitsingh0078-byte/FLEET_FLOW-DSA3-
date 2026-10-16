package com.fleetflow.data;

import com.fleetflow.model.Driver;
import com.fleetflow.model.Shipment;
import com.fleetflow.model.ShipmentDocument;
import com.fleetflow.model.ShipmentEvent;
import com.fleetflow.model.Vehicle;
import com.fleetflow.model.Warehouse;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

/** In-memory application data store shared by all pages. Observable so tables update automatically. */
public final class FleetRepository {

    private static final long DEMO_SEED = 42L;
    private static final FleetRepository INSTANCE = new FleetRepository();

    private final ObservableList<Shipment> shipments = FXCollections.observableArrayList();
    private final ObservableList<Vehicle> vehicles = FXCollections.observableArrayList();
    private final ObservableList<Driver> drivers = FXCollections.observableArrayList();
    private final ObservableList<Warehouse> warehouses = FXCollections.observableArrayList();
    private final ObservableList<ShipmentEvent> events = FXCollections.observableArrayList();
    private final ObservableList<ShipmentDocument> documents = FXCollections.observableArrayList();

    private final Path storageFile = ShipmentCsvStore.defaultPath();
    private String lastStorageProblem;

    private FleetRepository() {
        reloadDemoData();
        loadSavedShipments();
    }

    /** Loads user-edited shipments if a saved file exists; a damaged file is ignored (demo data stays). */
    private void loadSavedShipments() {
        if (!Files.exists(storageFile)) {
            return;
        }
        try {
            shipments.setAll(ShipmentCsvStore.load(storageFile));
        } catch (IOException | RuntimeException e) {
            lastStorageProblem = "Saved shipments could not be read (" + e.getMessage() + "); demo data is shown instead.";
        }
    }

    /** Saves the shipments; returns an error message, or null on success. */
    public String saveShipments() {
        try {
            ShipmentCsvStore.save(storageFile, shipments);
            return null;
        } catch (IOException | RuntimeException e) {
            return "The changes could not be saved to " + storageFile + ": " + e.getMessage();
        }
    }

    /** Deletes the saved file and restores the prepared demo data. */
    public void resetToDemoData() {
        try {
            Files.deleteIfExists(storageFile);
        } catch (IOException e) {
            lastStorageProblem = "The saved file could not be deleted: " + e.getMessage();
        }
        reloadDemoData();
    }

    /** @return a problem found while loading saved data (shown once), or null. */
    public String takeStorageProblem() {
        String p = lastStorageProblem;
        lastStorageProblem = null;
        return p;
    }

    public static FleetRepository get() {
        return INSTANCE;
    }

    /** Replaces everything with the deterministic demo data set. */
    public void reloadDemoData() {
        FleetData data = DemoDataGenerator.generate(DEMO_SEED);
        shipments.setAll(data.shipments());
        vehicles.setAll(data.vehicles());
        drivers.setAll(data.drivers());
        warehouses.setAll(data.warehouses());
        events.setAll(data.events());
        documents.setAll(data.documents());
    }

    public ObservableList<Shipment> shipments() {
        return shipments;
    }

    public ObservableList<Vehicle> vehicles() {
        return vehicles;
    }

    public ObservableList<Driver> drivers() {
        return drivers;
    }

    public ObservableList<Warehouse> warehouses() {
        return warehouses;
    }

    public ObservableList<ShipmentEvent> events() {
        return events;
    }

    public ObservableList<ShipmentDocument> documents() {
        return documents;
    }

    /** @return the ids of the first {@code limit} shipments joined by spaces (used as search text demos). */
    public String shipmentIdManifest(int limit) {
        return shipments.stream().limit(limit).map(Shipment::id).collect(Collectors.joining(" "));
    }
}
