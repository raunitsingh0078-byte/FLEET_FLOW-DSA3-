package com.fleetflow.model;

/**
 * A warehouse or distribution centre.
 *
 * @param mapX horizontal position on the logistics map, normalised to 0..1
 * @param mapY vertical position on the logistics map, normalised to 0..1
 */
public record Warehouse(String id, String name, String location, int capacity, int currentLoad,
                        WarehouseStatus status, double mapX, double mapY) {

    /** @return load as a percentage of capacity (0..100+). */
    public double loadPercent() {
        return capacity == 0 ? 0 : 100.0 * currentLoad / capacity;
    }
}
