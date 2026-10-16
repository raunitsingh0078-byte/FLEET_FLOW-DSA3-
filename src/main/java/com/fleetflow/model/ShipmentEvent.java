package com.fleetflow.model;

import java.time.LocalDateTime;

/** A tracking event that happened to a shipment (scan, dispatch, delay, delivery...). */
public record ShipmentEvent(LocalDateTime time, String shipmentId, String type, String detail) {
}
