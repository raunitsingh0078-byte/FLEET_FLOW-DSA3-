package com.fleetflow.algorithm.dp;

import java.util.Arrays;

/**
 * Held-Karp bitmask dynamic programme for the travelling-salesman route problem.
 * {@code dp[mask][last]} = cheapest way to leave city 0, visit exactly the cities in {@code mask} and stand at {@code last}.
 * Time O(2ⁿ · n²), space O(2ⁿ · n) – only usable for small n, which is exactly the "small-scale route optimisation" use case.
 */
public final class BitmaskTsp {

    public static final double INF = Double.POSITIVE_INFINITY;

    /**
     * @param dp           the DP table
     * @param route        optimal round trip starting and ending at city 0
     * @param transitions  number of relaxations performed
     */
    public record Result(double cost, int[] route, double[][] dp, int[][] parent, long transitions) {
    }

    private BitmaskTsp() {
    }

    /** Solves the round-trip problem starting from city 0 on a complete cost matrix. */
    public static Result solve(double[][] dist) {
        int n = dist.length;
        if (n < 2) {
            throw new IllegalArgumentException("at least two cities are required");
        }
        if (n > 16) {
            throw new IllegalArgumentException("bitmask DP is limited to 16 cities");
        }
        int full = (1 << n) - 1;
        double[][] dp = new double[1 << n][n];
        int[][] parent = new int[1 << n][n];
        for (double[] row : dp) {
            Arrays.fill(row, INF);
        }
        for (int[] row : parent) {
            Arrays.fill(row, -1);
        }
        dp[1][0] = 0;
        long transitions = 0;
        for (int mask = 1; mask <= full; mask += 2) { // only masks containing city 0
            for (int last = 0; last < n; last++) {
                if (dp[mask][last] == INF) {
                    continue;
                }
                for (int next = 1; next < n; next++) {
                    if ((mask & (1 << next)) != 0) {
                        continue;
                    }
                    transitions++;
                    int nm = mask | (1 << next);
                    double c = dp[mask][last] + dist[last][next];
                    if (c < dp[nm][next]) {
                        dp[nm][next] = c;
                        parent[nm][next] = last;
                    }
                }
            }
        }
        double best = INF;
        int bestLast = 0;
        for (int last = 1; last < n; last++) {
            double c = dp[full][last] + dist[last][0];
            if (c < best) {
                best = c;
                bestLast = last;
            }
        }
        int[] route = new int[n + 1];
        int mask = full;
        int cur = bestLast;
        for (int pos = n - 1; pos >= 1; pos--) {
            route[pos] = cur;
            int prev = parent[mask][cur];
            mask ^= 1 << cur;
            cur = prev;
        }
        route[0] = 0;
        route[n] = 0;
        return new Result(best, route, dp, parent, transitions);
    }
}
