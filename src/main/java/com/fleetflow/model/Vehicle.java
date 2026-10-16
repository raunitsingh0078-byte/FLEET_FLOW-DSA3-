package com.fleetflow.model;

/** A delivery vehicle. {@code driverId} is empty when nobody is assigned. */
public record Vehicle(String id, String type, int capacityKg, String location, VehicleStatus status,
                      String driverId, int fuelPercent) {
}
