package com.fleetflow.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Collects real, measured execution statistics of every algorithm run in the application. */
public final class AlgorithmStats {

    /** Aggregated statistics of one algorithm. */
    public record Row(String algorithm, int runs, double lastMillis, double totalMillis, long lastComparisons) {
        public double averageMillis() {
            return runs == 0 ? 0 : totalMillis / runs;
        }
    }

    private static final Map<String, Row> ROWS = new LinkedHashMap<>();

    private AlgorithmStats() {
    }

    /** Records one measured run. */
    public static synchronized void record(String algorithm, long nanos, long comparisons) {
        double ms = nanos / 1_000_000.0;
        Row old = ROWS.get(algorithm);
        int runs = old == null ? 1 : old.runs() + 1;
        double total = old == null ? ms : old.totalMillis() + ms;
        ROWS.put(algorithm, new Row(algorithm, runs, ms, total, comparisons));
    }

    public static synchronized List<Row> snapshot() {
        return new ArrayList<>(ROWS.values());
    }
}
