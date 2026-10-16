package com.fleetflow.datastructure.graph;

import java.util.ArrayList;
import java.util.List;

/**
 * Directed flow network stored as paired residual edges: forward edge {@code 2k} (with the real capacity) and its
 * reverse edge {@code 2k + 1} (capacity 0). Pushing flow on one updates the other, so the residual capacity of
 * an edge is always {@code capacity - flow}.
 */
public final class FlowNetwork {

    /** One residual edge. */
    public static final class Edge {
        private final int from;
        private final int to;
        private final int capacity;
        private int flow;

        private Edge(int from, int to, int capacity) {
            this.from = from;
            this.to = to;
            this.capacity = capacity;
        }

        public int from() {
            return from;
        }

        public int to() {
            return to;
        }

        public int capacity() {
            return capacity;
        }

        public int flow() {
            return flow;
        }

        /** @return how much more flow can be pushed along this residual edge. */
        public int residual() {
            return capacity - flow;
        }
    }

    private final int nodeCount;
    private final List<Edge> edges = new ArrayList<>();
    private final List<List<Integer>> adjacency = new ArrayList<>();

    public FlowNetwork(int nodeCount) {
        if (nodeCount < 0) {
            throw new IllegalArgumentException("node count must not be negative");
        }
        this.nodeCount = nodeCount;
        for (int i = 0; i < nodeCount; i++) {
            adjacency.add(new ArrayList<>());
        }
    }

    /**
     * Adds a directed edge.
     *
     * @return the forward-edge index k (the residual edge ids are 2k and 2k+1)
     * @throws IllegalArgumentException for invalid endpoints, self-loops or negative capacity
     */
    public int addEdge(int from, int to, int capacity) {
        if (from < 0 || from >= nodeCount || to < 0 || to >= nodeCount) {
            throw new IllegalArgumentException("edge endpoint out of range: " + from + " -> " + to);
        }
        if (from == to) {
            throw new IllegalArgumentException("self-loops are not allowed");
        }
        if (capacity < 0) {
            throw new IllegalArgumentException("capacity must not be negative");
        }
        int id = edges.size();
        edges.add(new Edge(from, to, capacity));
        edges.add(new Edge(to, from, 0));
        adjacency.get(from).add(id);
        adjacency.get(to).add(id + 1);
        return id / 2;
    }

    public int nodeCount() {
        return nodeCount;
    }

    /** @return number of real (forward) edges. */
    public int edgeCount() {
        return edges.size() / 2;
    }

    /** @return the residual edge with the given raw id. */
    public Edge edge(int rawId) {
        return edges.get(rawId);
    }

    /** @return the forward edge k. */
    public Edge forward(int k) {
        return edges.get(2 * k);
    }

    /** @return raw ids of residual edges leaving {@code node}. */
    public List<Integer> outgoing(int node) {
        return adjacency.get(node);
    }

    /** Pushes {@code amount} units along raw edge {@code rawId} (and cancels the same amount on its partner). */
    public void push(int rawId, int amount) {
        edges.get(rawId).flow += amount;
        edges.get(rawId ^ 1).flow -= amount;
    }

    /** @return the current flow of every forward edge. */
    public int[] flows() {
        int[] result = new int[edgeCount()];
        for (int k = 0; k < result.length; k++) {
            result[k] = forward(k).flow;
        }
        return result;
    }

    /** @return a copy of this network with all flows reset to zero. */
    public FlowNetwork copyWithoutFlow() {
        FlowNetwork copy = new FlowNetwork(nodeCount);
        for (int k = 0; k < edgeCount(); k++) {
            copy.addEdge(forward(k).from, forward(k).to, forward(k).capacity);
        }
        return copy;
    }
}
