package com.fleetflow.algorithm.flow;

import com.fleetflow.datastructure.graph.FlowNetwork;

import java.util.List;

/**
 * Outcome of a max-flow computation.
 *
 * @param network      the network (a private copy) holding the final flow, usable for residual analysis / min-cut
 * @param augmentations number of augmenting paths
 * @param phases       number of Dinic phases (0 for the other algorithms)
 * @param edgeScans    number of residual edges examined (a machine-independent cost measure)
 */
public record FlowResult(String algorithm, int maxFlow, List<FlowStep> steps, int augmentations, int phases,
                         long edgeScans, FlowNetwork network) {
}
