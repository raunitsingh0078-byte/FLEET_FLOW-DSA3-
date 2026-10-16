package com.fleetflow.algorithm.flow;

import com.fleetflow.datastructure.graph.FlowNetwork;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Minimum s-t cut from a maximum flow (max-flow / min-cut theorem): after the last augmentation the nodes still
 * reachable from the source in the residual graph form the source side S; every original edge leaving S is saturated
 * and together these edges form a minimum cut – the logistics bottleneck.
 */
public final class MinCut {

    /**
     * @param sourceSide  true for nodes on the source side
     * @param cutEdges    forward-edge indices crossing from the source side to the sink side
     * @param capacity    total capacity of the cut (equals the maximum flow)
     */
    public record Result(boolean[] sourceSide, List<Integer> cutEdges, int capacity) {
    }

    private MinCut() {
    }

    /** @param afterMaxFlow a network on which a maximum flow has been computed ({@link FlowResult#network()}) */
    public static Result compute(FlowNetwork afterMaxFlow, int source) {
        int n = afterMaxFlow.nodeCount();
        boolean[] reachable = new boolean[n];
        Deque<Integer> queue = new ArrayDeque<>();
        queue.add(source);
        reachable[source] = true;
        while (!queue.isEmpty()) {
            int u = queue.poll();
            for (int e : afterMaxFlow.outgoing(u)) {
                FlowNetwork.Edge edge = afterMaxFlow.edge(e);
                if (edge.residual() > 0 && !reachable[edge.to()]) {
                    reachable[edge.to()] = true;
                    queue.add(edge.to());
                }
            }
        }
        List<Integer> cut = new ArrayList<>();
        int capacity = 0;
        for (int k = 0; k < afterMaxFlow.edgeCount(); k++) {
            FlowNetwork.Edge e = afterMaxFlow.forward(k);
            if (reachable[e.from()] && !reachable[e.to()] && e.capacity() > 0) {
                cut.add(k);
                capacity += e.capacity();
            }
        }
        return new Result(reachable, cut, capacity);
    }
}
