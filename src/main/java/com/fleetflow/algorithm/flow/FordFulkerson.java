package com.fleetflow.algorithm.flow;

import com.fleetflow.datastructure.graph.FlowNetwork;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Ford-Fulkerson: repeatedly find <em>any</em> augmenting path in the residual graph (here by depth-first search)
 * and push the bottleneck amount along it. With integer capacities it needs at most |f*| augmentations,
 * so the running time is O(E · |f*|) – pseudo-polynomial.
 */
public final class FordFulkerson {

    private FordFulkerson() {
    }

    public static FlowResult maxFlow(FlowNetwork input, int source, int sink) {
        FlowValidation.check(input, source, sink);
        FlowNetwork net = input.copyWithoutFlow();
        List<FlowStep> steps = new ArrayList<>();
        long scans = 0;
        int total = 0;
        int augmentations = 0;
        while (true) {
            int n = net.nodeCount();
            int[] parentEdge = new int[n];
            java.util.Arrays.fill(parentEdge, -1);
            boolean[] visited = new boolean[n];
            int[] iterator = new int[n];
            Deque<Integer> stack = new ArrayDeque<>();
            stack.push(source);
            visited[source] = true;
            while (!stack.isEmpty() && !visited[sink]) {
                int u = stack.peek();
                boolean advanced = false;
                while (iterator[u] < net.outgoing(u).size()) {
                    int e = net.outgoing(u).get(iterator[u]++);
                    scans++;
                    FlowNetwork.Edge edge = net.edge(e);
                    if (edge.residual() > 0 && !visited[edge.to()]) {
                        visited[edge.to()] = true;
                        parentEdge[edge.to()] = e;
                        stack.push(edge.to());
                        advanced = true;
                        break;
                    }
                }
                if (!advanced) {
                    stack.pop();
                }
            }
            if (!visited[sink]) {
                break;
            }
            total += FlowSupport.augment(net, source, sink, parentEdge, ++augmentations, total, steps);
        }
        return new FlowResult("Ford-Fulkerson (DFS)", total, steps, augmentations, 0, scans, net);
    }
}
