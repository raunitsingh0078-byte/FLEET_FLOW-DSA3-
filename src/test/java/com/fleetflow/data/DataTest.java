package com.fleetflow.data;

import com.fleetflow.model.Shipment;
import com.fleetflow.model.ShipmentStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataTest {

    @Test
    void demoDataMeetsTheProjectRequirements() {
        FleetData d = DemoDataGenerator.generate(42);
        assertTrue(d.shipments().size() >= 20);
        assertTrue(d.vehicles().size() >= 10);
        assertTrue(d.drivers().size() >= 10);
        assertTrue(d.warehouses().size() >= 5);
        assertTrue(d.events().size() > 20);
        assertTrue(d.documents().size() >= 5);
        assertEquals(d.shipments().size(), new HashSet<>(d.shipments().stream().map(Shipment::id).toList()).size(), "shipment ids are unique");
    }

    @Test
    void demoDataIsDeterministicAndConsistent() {
        assertEquals(DemoDataGenerator.generate(42).shipments(), DemoDataGenerator.generate(42).shipments());
        FleetData d = DemoDataGenerator.generate(7);
        for (Shipment s : d.shipments()) {
            if (s.status() == ShipmentStatus.PENDING) {
                assertTrue(s.driverId().isEmpty() && s.vehicleId().isEmpty());
            }
            if (s.status() == ShipmentStatus.IN_TRANSIT) {
                assertTrue(!s.driverId().isEmpty() && !s.vehicleId().isEmpty());
            }
        }
    }

    @Test
    void routeNetworkReferencesExistingWarehouses() {
        var ids = DemoDataGenerator.generate(42).warehouses().stream().map(w -> w.id()).toList();
        for (RouteNetwork.Route r : RouteNetwork.routes()) {
            assertTrue(ids.contains(r.from()) && ids.contains(r.to()));
        }
    }

    @Test
    void csvRoundTripKeepsEveryFieldIncludingQuotesAndCommas(@TempDir Path dir) throws IOException {
        List<Shipment> original = new java.util.ArrayList<>(DemoDataGenerator.generate(42).shipments());
        Shipment tricky = original.get(0);
        original.set(0, new Shipment(tricky.id(), tricky.packageCode(), "Acme, \"Ltd\"", tricky.source(), tricky.destination(),
                "12 Main St, Apt \"4\"", "line, with, commas", tricky.status(), tricky.priority(), 12.5, tricky.driverId(), tricky.vehicleId(), tricky.date()));
        Path file = dir.resolve("shipments.csv");
        ShipmentCsvStore.save(file, original);
        assertEquals(original, ShipmentCsvStore.load(file));
    }

    @Test
    void malformedCsvIsReportedNotSwallowed(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("bad.csv");
        Files.writeString(file, "header\n\"only\",\"three\",\"fields\"\n");
        IOException e = assertThrows(IOException.class, () -> ShipmentCsvStore.load(file));
        assertTrue(e.getMessage().contains("line 2"));
        Files.writeString(file, "header\n" + "\"a\",\"b\",\"c\",\"d\",\"e\",\"f\",\"g\",\"NOPE\",\"LOW\",\"1\",\"\",\"\",\"2026-01-01\"\n");
        assertThrows(IOException.class, () -> ShipmentCsvStore.load(file));
    }
}
