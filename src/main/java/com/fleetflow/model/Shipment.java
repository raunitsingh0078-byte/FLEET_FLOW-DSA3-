package com.fleetflow.model;

import java.time.LocalDate;

/**
 * A customer shipment. Records are immutable; editing a shipment replaces it in the repository.
 *
 * @param driverId  assigned driver id, or an empty string when unassigned
 * @param vehicleId assigned vehicle id, or an empty string when unassigned
 */
public record Shipment(String id, String packageCode, String customer, String source, String destination,
                       String address, String description, ShipmentStatus status, Priority priority,
                       double weightKg, String driverId, String vehicleId, LocalDate date) {
}
