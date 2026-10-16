package com.fleetflow.data;

import com.fleetflow.model.Driver;
import com.fleetflow.model.Shipment;
import com.fleetflow.model.ShipmentDocument;
import com.fleetflow.model.ShipmentEvent;
import com.fleetflow.model.Vehicle;
import com.fleetflow.model.Warehouse;

import java.util.List;

/** Immutable bundle of one complete generated data set. */
public record FleetData(List<Shipment> shipments, List<Vehicle> vehicles, List<Driver> drivers,
                        List<Warehouse> warehouses, List<ShipmentEvent> events,
                        List<ShipmentDocument> documents) {
}
