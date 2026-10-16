package com.fleetflow.algorithm.flow;

import com.fleetflow.datastructure.graph.FlowNetwork;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;

/**
 * Dinic's algorithm. Each <em>phase</em> builds a BFS level graph of the residual network and then saturates it with a
 * <em>blocking flow</em> found by DFS with per-node edge pointers (dead ends are never revisited). The shortest
 * augmenting-path length strictly increases every phase, so there are at most V phases: O(V²·E) overall
 * (O(E·√V) on unit-capacity networks).
 */
public final class Dinic {

    private Dinic() {
    }

    public static FlowResult maxFlow(FlowNetwork input, int source, int sink) {
        FlowValidation.check(input, source, sink);
        FlowNetwork net = input.copyWithoutFlow();
        List<FlowStep> steps = new ArrayList<>();
        long scans = 0;
        int total = 0;
        int augmentations = 0;
        int phase = 0;
        int n = net.nodeCount();
        while (true) {
            int[] level = new int[n];
            Arrays.fill(level, -1);
            level[source] = 0;
            Deque<Integer> queue = new ArrayDeque<>();
            queue.add(source);
            while (!queue.isEmpty()) {
                int u = queue.poll();
                for (int e : net.outgoing(u)) {
                    scans++;
                    FlowNetwork.Edge edge = net.edge(e);
                    if (edge.residual() > 0 && level[edge.to()] < 0) {
                        level[edge.to()] = level[u] + 1;
                        queue.add(edge.to());
                    }
                }
            }
            if (level[sink] < 0) {
                break;
            }
            phase++;
            steps.add(new FlowStep(FlowStep.Kind.PHASE, phase, level.clone(), new int[0], new int[0], 0, total, net.flows()));
            int[] pointer = new int[n];
            while (true) { // blocking flow: keep finding paths in the level graph
                int[] parentEdge = blockingPath(net, source, sink, level, pointer);
                if (parentEdge == null) {
                    break;
                }
                total += FlowSupport.augment(net, source, sink, parentEdge, ++augmentations, total, steps, phase);
            }
            for (int p : pointer) {
                scans += p;
            }
        }
        return new FlowResult("Dinic", total, steps, augmentations, phase, scans, net);
    }

    /** Finds one source→sink path in the level graph using the edge pointers, or null if the level graph is blocked. */
    private static int[] blockingPath(FlowNetwork net, int source, int sink, int[] level, int[] pointer) {
        int n = net.nodeCount();
        int[] parentEdge = new int[n];
        Arrays.fill(parentEdge, -1);
        List<Integer> path = new ArrayList<>();
        int u = source;
        while (u != sink) {
            boolean advanced = false;
            while (pointer[u] < net.outgoing(u).size()) {
                int e = net.outgoing(u).get(pointer[u]);
                FlowNetwork.Edge edge = net.edge(e);
                if (edge.residual() > 0 && level[edge.to()] == level[u] + 1) {
                    path.add(e);
                    parentEdge[edge.to()] = e;
                    u = edge.to();
                    advanced = true;
                    break;
                }
                pointer[u]++;
            }
            if (!advanced) {
                if (u == source) {
                    return null;
                }
                level[u] = -1; // dead end: never enter this node again in this phase
                int back = path.remove(path.size() - 1);
                u = net.edge(back).from();
                pointer[u]++;
            }
        }
        return parentEdge;
    }
}
