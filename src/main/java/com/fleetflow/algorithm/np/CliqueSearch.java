package com.fleetflow.algorithm.np;

import com.fleetflow.datastructure.graph.SimpleGraph;

import java.util.ArrayList;
import java.util.List;

/** Exponential-time clique search by backtracking (only for the small graphs of the educational demos). */
public final class CliqueSearch {

    /**
     * @param clique the clique found, or null if none of the requested size exists
     * @param trace  every partial clique that was examined, in order
     */
    public record Result(List<Integer> clique, List<int[]> trace) {
    }

    private CliqueSearch() {
    }

    /** Looks for a clique with exactly {@code k} vertices, extending partial cliques with higher-numbered vertices. */
    public static Result findClique(SimpleGraph g, int k) {
        List<int[]> trace = new ArrayList<>();
        List<Integer> current = new ArrayList<>();
        List<Integer> found = search(g, k, 0, current, trace);
        return new Result(found, trace);
    }

    private static List<Integer> search(SimpleGraph g, int k, int from, List<Integer> current, List<int[]> trace) {
        trace.add(current.stream().mapToInt(Integer::intValue).toArray());
        if (current.size() == k) {
            return new ArrayList<>(current);
        }
        if (current.size() + (g.vertexCount() - from) < k) {
            return null; // not enough vertices left: prune
        }
        for (int v = from; v < g.vertexCount(); v++) {
            boolean ok = true;
            for (int u : current) {
                if (!g.hasEdge(u, v)) {
                    ok = false;
                    break;
                }
            }
            if (ok) {
                current.add(v);
                List<Integer> r = search(g, k, v + 1, current, trace);
                if (r != null) {
                    return r;
                }
                current.remove(current.size() - 1);
            }
        }
        return null;
    }

    /** @return a maximum clique (empty list for a graph without vertices). */
    public static List<Integer> maximumClique(SimpleGraph g) {
        for (int k = g.vertexCount(); k >= 1; k--) {
            List<Integer> c = findClique(g, k).clique();
            if (c != null) {
                return c;
            }
        }
        return new ArrayList<>();
    }
}
