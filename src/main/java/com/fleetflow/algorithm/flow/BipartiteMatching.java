package com.fleetflow.algorithm.flow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Maximum bipartite matching by augmenting paths (Kuhn's algorithm) – the unit-capacity special case of max flow.
 * For each left vertex a DFS looks for an augmenting path that may re-assign right vertices that are already taken.
 * Time O(V·E).
 */
public final class BipartiteMatching {

    /** Kind of recorded event. */
    public enum Kind { TRY, MATCH, REASSIGN, FAIL }

    /**
     * @param left        left vertex being processed
     * @param right       right vertex considered (-1 for FAIL)
     * @param matchOfLeft snapshot: matched right vertex of each left vertex (-1 = unmatched)
     * @param size        matching size after this step
     */
    public record Step(Kind kind, int left, int right, int[] matchOfLeft, int size) {
    }

    /** Result with the final matching and the trace. */
    public record Result(int size, int[] matchOfLeft, int[] matchOfRight, List<Step> steps, long attempts) {
    }

    private BipartiteMatching() {
    }

    /**
     * @param adjacency for each left vertex the list of right vertices it may be assigned to
     */
    public static Result maximumMatching(int leftCount, int rightCount, List<List<Integer>> adjacency) {
        if (adjacency.size() != leftCount) {
            throw new IllegalArgumentException("adjacency must have one list per left vertex");
        }
        for (List<Integer> list : adjacency) {
            for (int v : list) {
                if (v < 0 || v >= rightCount) {
                    throw new IllegalArgumentException("right vertex out of range: " + v);
                }
            }
        }
        int[] matchLeft = new int[leftCount];
        int[] matchRight = new int[rightCount];
        Arrays.fill(matchLeft, -1);
        Arrays.fill(matchRight, -1);
        List<Step> steps = new ArrayList<>();
        long[] attempts = new long[1];
        int size = 0;
        for (int u = 0; u < leftCount; u++) {
            boolean[] visited = new boolean[rightCount];
            if (augment(u, u, adjacency, visited, matchLeft, matchRight, steps, attempts, size)) {
                size++;
            } else {
                steps.add(new Step(Kind.FAIL, u, -1, matchLeft.clone(), size));
            }
        }
        return new Result(size, matchLeft, matchRight, steps, attempts[0]);
    }

    private static boolean augment(int u, int root, List<List<Integer>> adj, boolean[] visited, int[] matchLeft,
                                   int[] matchRight, List<Step> steps, long[] attempts, int size) {
        for (int v : adj.get(u)) {
            if (visited[v]) {
                continue;
            }
            visited[v] = true;
            attempts[0]++;
            steps.add(new Step(Kind.TRY, u, v, matchLeft.clone(), size));
            if (matchRight[v] == -1) {
                matchRight[v] = u;
                matchLeft[u] = v;
                steps.add(new Step(Kind.MATCH, u, v, matchLeft.clone(), size + (u == root ? 1 : 0)));
                return true;
            }
            int previousOwner = matchRight[v];
            if (augment(previousOwner, root, adj, visited, matchLeft, matchRight, steps, attempts, size)) {
                matchRight[v] = u;
                matchLeft[u] = v;
                steps.add(new Step(Kind.REASSIGN, u, v, matchLeft.clone(), size + 1));
                return true;
            }
        }
        return false;
    }
}
