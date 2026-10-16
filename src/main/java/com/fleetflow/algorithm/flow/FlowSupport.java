package com.fleetflow.algorithm.flow;

import com.fleetflow.datastructure.graph.FlowNetwork;

import java.util.List;

/** Shared augmentation logic of the three max-flow algorithms. */
final class FlowSupport {

    private FlowSupport() {
    }

    static int augment(FlowNetwork net, int source, int sink, int[] parentEdge, int number, int totalBefore,
                       List<FlowStep> steps) {
        return augment(net, source, sink, parentEdge, number, totalBefore, steps, number);
    }

    /**
     * Pushes the bottleneck along the path described by {@code parentEdge} (walking back from the sink),
     * records a step and returns the pushed amount.
     */
    static int augment(FlowNetwork net, int source, int sink, int[] parentEdge, int number, int totalBefore,
                       List<FlowStep> steps, int phaseLabel) {
        int bottleneck = Integer.MAX_VALUE;
        int length = 0;
        for (int v = sink; v != source; v = net.edge(parentEdge[v]).from()) {
            bottleneck = Math.min(bottleneck, net.edge(parentEdge[v]).residual());
            length++;
        }
        int[] edges = new int[length];
        int[] nodes = new int[length + 1];
        int idx = length;
        nodes[idx] = sink;
        for (int v = sink; v != source; v = net.edge(parentEdge[v]).from()) {
            net.push(parentEdge[v], bottleneck);
            edges[--idx] = parentEdge[v];
            nodes[idx] = net.edge(parentEdge[v]).from();
        }
        steps.add(new FlowStep(FlowStep.Kind.AUGMENT, phaseLabel, null, edges, nodes, bottleneck,
                totalBefore + bottleneck, net.flows()));
        return bottleneck;
    }
}
