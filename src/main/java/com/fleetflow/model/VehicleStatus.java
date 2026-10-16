package com.fleetflow.model;

/** Operational state of a delivery vehicle. */
public enum VehicleStatus {
    AVAILABLE("Available"), LOADING("Loading"), IN_TRANSIT("In Transit"), MAINTENANCE("Maintenance");

    private final String label;

    VehicleStatus(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
