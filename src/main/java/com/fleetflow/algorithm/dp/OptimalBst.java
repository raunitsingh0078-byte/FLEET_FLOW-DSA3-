package com.fleetflow.algorithm.dp;

import java.util.ArrayList;
import java.util.List;

/**
 * Optimal binary search tree for keys with access frequencies (successful searches only).
 * {@code cost[i][j]} = minimum weighted path length of a BST on keys i..j where the root has depth 1:
 * cost[i][j] = sum(freq[i..j]) + min over roots r of (cost[i][r-1] + cost[r+1][j]). Time O(n³), space O(n²).
 */
public final class OptimalBst {

    /** A node of the resulting tree. */
    public static final class Node {
        public final int index;
        public final String key;
        public final long frequency;
        public Node left;
        public Node right;

        Node(int index, String key, long frequency) {
            this.index = index;
            this.key = key;
            this.frequency = frequency;
        }
    }

    /** One filled DP cell. */
    public record Step(int i, int j, int root, long cost) {
    }

    /** cost and root tables are 0-based over keys; cost[i][j] for i &gt; j is 0. */
    public record Result(long minCost, long[][] cost, int[][] root, Node tree, List<Step> steps) {
    }

    private OptimalBst() {
    }

    /**
     * @param keys keys in ascending order
     * @param freq access frequency of each key (non-negative)
     */
    public static Result compute(String[] keys, long[] freq) {
        int n = keys.length;
        if (n == 0 || freq.length != n) {
            throw new IllegalArgumentException("keys and frequencies must be non-empty and of equal length");
        }
        long[] prefix = new long[n + 1];
        for (int i = 0; i < n; i++) {
            prefix[i + 1] = prefix[i] + freq[i];
        }
        long[][] cost = new long[n + 2][n + 2]; // shifted by one so cost[i][i-1] = 0 is addressable
        int[][] root = new int[n + 2][n + 2];
        List<Step> steps = new ArrayList<>();
        for (int len = 1; len <= n; len++) {
            for (int i = 0; i + len - 1 < n; i++) {
                int j = i + len - 1;
                long best = Long.MAX_VALUE;
                int bestRoot = i;
                for (int r = i; r <= j; r++) {
                    long left = r > i ? cost[i][r - 1] : 0;
                    long right = r < j ? cost[r + 1][j] : 0;
                    if (left + right < best) {
                        best = left + right;
                        bestRoot = r;
                    }
                }
                cost[i][j] = best + prefix[j + 1] - prefix[i];
                root[i][j] = bestRoot;
                steps.add(new Step(i, j, bestRoot, cost[i][j]));
            }
        }
        return new Result(cost[0][n - 1], cost, root, build(keys, freq, root, 0, n - 1), steps);
    }

    private static Node build(String[] keys, long[] freq, int[][] root, int i, int j) {
        if (i > j) {
            return null;
        }
        int r = root[i][j];
        Node node = new Node(r, keys[r], freq[r]);
        node.left = build(keys, freq, root, i, r - 1);
        node.right = build(keys, freq, root, r + 1, j);
        return node;
    }
}
