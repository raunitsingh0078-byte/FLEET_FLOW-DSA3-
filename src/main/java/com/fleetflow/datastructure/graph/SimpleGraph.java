package com.fleetflow.datastructure.graph;

import java.util.ArrayList;
import java.util.List;

/** Small undirected simple graph (adjacency matrix) used by the NP-completeness demonstrations. */
public final class SimpleGraph {

    private final int vertexCount;
    private final boolean[][] adjacent;
    private final List<int[]> edgeList = new ArrayList<>();

    public SimpleGraph(int vertexCount) {
        if (vertexCount < 0) {
            throw new IllegalArgumentException("vertex count must not be negative");
        }
        this.vertexCount = vertexCount;
        this.adjacent = new boolean[vertexCount][vertexCount];
    }

    /**
     * Parses an edge list such as {@code "0-1, 1-2, 2-0"}.
     *
     * @throws IllegalArgumentException for malformed input, self-loops or vertices outside 0..n-1
     */
    public static SimpleGraph parse(int vertexCount, String edges) {
        SimpleGraph g = new SimpleGraph(vertexCount);
        if (edges == null || edges.isBlank()) {
            return g;
        }
        for (String part : edges.split("[,;\\n]")) {
            String p = part.trim();
            if (p.isEmpty()) {
                continue;
            }
            String[] ends = p.split("\\s*-\\s*");
            if (ends.length != 2) {
                throw new IllegalArgumentException("\"" + p + "\" is not an edge; write edges like 0-1");
            }
            try {
                g.addEdge(Integer.parseInt(ends[0].trim()), Integer.parseInt(ends[1].trim()));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("\"" + p + "\" contains a vertex that is not a number");
            }
        }
        return g;
    }

    /** Adds the undirected edge {u, v}; adding an existing edge has no effect. */
    public void addEdge(int u, int v) {
        if (u < 0 || v < 0 || u >= vertexCount || v >= vertexCount) {
            throw new IllegalArgumentException("vertex out of range in edge " + u + "-" + v + " (valid: 0.." + (vertexCount - 1) + ")");
        }
        if (u == v) {
            throw new IllegalArgumentException("self-loops are not allowed (" + u + "-" + v + ")");
        }
        if (!adjacent[u][v]) {
            adjacent[u][v] = true;
            adjacent[v][u] = true;
            edgeList.add(new int[]{Math.min(u, v), Math.max(u, v)});
        }
    }

    public int vertexCount() {
        return vertexCount;
    }

    public boolean hasEdge(int u, int v) {
        return adjacent[u][v];
    }

    /** @return the edges as {smaller, larger} pairs in insertion order. */
    public List<int[]> edges() {
        return edgeList;
    }

    public int edgeCount() {
        return edgeList.size();
    }

    /** @return the complement graph: {u, v} is an edge iff it is not an edge here. */
    public SimpleGraph complement() {
        SimpleGraph c = new SimpleGraph(vertexCount);
        for (int u = 0; u < vertexCount; u++) {
            for (int v = u + 1; v < vertexCount; v++) {
                if (!adjacent[u][v]) {
                    c.addEdge(u, v);
                }
            }
        }
        return c;
    }
}
