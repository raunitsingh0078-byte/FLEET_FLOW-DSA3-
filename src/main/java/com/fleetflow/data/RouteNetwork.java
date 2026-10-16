package com.fleetflow.data;

import java.util.List;

/** The inter-warehouse transport network (edge = capacity in pallets per day) used by the flow demos. */
public final class RouteNetwork {

    /** A directed transport lane between two warehouses. */
    public record Route(String from, String to, int capacity) {
    }

    public static final String DEFAULT_SOURCE = "WH-02";
    public static final String DEFAULT_SINK = "WH-05";

    private RouteNetwork() {
    }

    /** @return the lanes of the demo network; maximum flow WH-02 → WH-05 is 32 with bottleneck at the sink. */
    public static List<Route> routes() {
        return List.of(
                new Route("WH-02", "WH-01", 20),
                new Route("WH-02", "WH-04", 15),
                new Route("WH-01", "WH-03", 12),
                new Route("WH-01", "WH-04", 8),
                new Route("WH-04", "WH-03", 10),
                new Route("WH-04", "WH-05", 14),
                new Route("WH-03", "WH-05", 18));
    }
}
