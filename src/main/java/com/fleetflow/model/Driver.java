package com.fleetflow.model;

/** A delivery driver. */
public record Driver(String id, String name, boolean available, int assignedDeliveries, double rating,
                     int experienceYears) {
}
