package com.fleetflow.model;

/** Operational state of a warehouse / distribution centre. */
public enum WarehouseStatus {
    OPERATIONAL("Operational"), CONGESTED("Congested"), MAINTENANCE("Maintenance");

    private final String label;

    WarehouseStatus(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
