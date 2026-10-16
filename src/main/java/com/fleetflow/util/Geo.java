package com.fleetflow.util;

import java.util.LinkedHashMap;
import java.util.Map;

/** Geographic helpers for the logistics demo data. */
public final class Geo {

    /** City name to {latitude, longitude}. */
    public static final Map<String, double[]> CITIES = new LinkedHashMap<>();

    static {
        CITIES.put("Mumbai", new double[]{19.076, 72.878});
        CITIES.put("Delhi", new double[]{28.614, 77.209});
        CITIES.put("Bengaluru", new double[]{12.972, 77.594});
        CITIES.put("Hyderabad", new double[]{17.385, 78.487});
        CITIES.put("Chennai", new double[]{13.083, 80.271});
        CITIES.put("Pune", new double[]{18.520, 73.856});
        CITIES.put("Kolkata", new double[]{22.573, 88.364});
        CITIES.put("Ahmedabad", new double[]{23.023, 72.572});
    }

    private Geo() {
    }

    /** Great-circle distance in kilometres. */
    public static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double r = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * r * Math.asin(Math.sqrt(a));
    }

    /** Distance between two known cities, or 0 if either is unknown. */
    public static double cityDistanceKm(String from, String to) {
        double[] a = CITIES.get(from);
        double[] b = CITIES.get(to);
        return a == null || b == null ? 0 : haversineKm(a[0], a[1], b[0], b[1]);
    }
}
