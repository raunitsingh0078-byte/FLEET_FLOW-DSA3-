package com.fleetflow.model;

/** Lifecycle state of a shipment. */
public enum ShipmentStatus {
    PENDING("Pending"), IN_TRANSIT("In Transit"), DELIVERED("Delivered"), DELAYED("Delayed");

    private final String label;

    ShipmentStatus(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
