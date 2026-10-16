package com.fleetflow.model;

/** A shipment document (delivery note, manifest...) whose text is indexed by the suffix structures. */
public record ShipmentDocument(String id, String shipmentId, String title, String text) {
}
