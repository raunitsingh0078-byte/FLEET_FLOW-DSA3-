package com.fleetflow.algorithm.flow;

import com.fleetflow.datastructure.graph.FlowNetwork;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;

/**
 * Edmonds-Karp: Ford-Fulkerson where every augmenting path is a <em>shortest</em> path (found by BFS).
 * This bounds the number of augmentations by O(V·E), giving O(V·E²) time independent of the capacities.
 */
public final class EdmondsKarp {

    private EdmondsKarp() {
    }

    public static FlowResult maxFlow(FlowNetwork input, int source, int sink) {
        FlowValidation.check(input, source, sink);
        FlowNetwork net = input.copyWithoutFlow();
        List<FlowStep> steps = new ArrayList<>();
        long scans = 0;
        int total = 0;
        int augmentations = 0;
        while (true) {
            int[] parentEdge = new int[net.nodeCount()];
            Arrays.fill(parentEdge, -1);
            boolean[] seen = new boolean[net.nodeCount()];
            Deque<Integer> queue = new ArrayDeque<>();
            queue.add(source);
            seen[source] = true;
            while (!queue.isEmpty() && !seen[sink]) {
                int u = queue.poll();
                for (int e : net.outgoing(u)) {
                    scans++;
                    FlowNetwork.Edge edge = net.edge(e);
                    if (edge.residual() > 0 && !seen[edge.to()]) {
                        seen[edge.to()] = true;
                        parentEdge[edge.to()] = e;
                        queue.add(edge.to());
                    }
                }
            }
            if (!seen[sink]) {
                break;
            }
            total += FlowSupport.augment(net, source, sink, parentEdge, ++augmentations, total, steps);
        }
        return new FlowResult("Edmonds-Karp (BFS)", total, steps, augmentations, 0, scans, net);
    }
}
