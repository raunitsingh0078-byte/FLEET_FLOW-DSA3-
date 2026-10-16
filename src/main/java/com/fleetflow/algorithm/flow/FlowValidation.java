package com.fleetflow.algorithm.flow;

import com.fleetflow.datastructure.graph.FlowNetwork;

/** Argument checks shared by the flow algorithms. */
final class FlowValidation {

    private FlowValidation() {
    }

    static void check(FlowNetwork net, int source, int sink) {
        if (net == null) {
            throw new IllegalArgumentException("network must not be null");
        }
        if (source < 0 || source >= net.nodeCount() || sink < 0 || sink >= net.nodeCount()) {
            throw new IllegalArgumentException("source or sink is not a node of the network");
        }
        if (source == sink) {
            throw new IllegalArgumentException("source and sink must be different nodes");
        }
    }
}
