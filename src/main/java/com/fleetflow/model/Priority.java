package com.fleetflow.model;

/** Delivery priority of a shipment. */
public enum Priority {
    LOW("Low"), NORMAL("Normal"), HIGH("High"), URGENT("Urgent");

    private final String label;

    Priority(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
