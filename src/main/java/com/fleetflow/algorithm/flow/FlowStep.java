package com.fleetflow.algorithm.flow;

/**
 * One recorded event of a max-flow run.
 *
 * @param kind       PHASE (Dinic level graph built) or AUGMENT (flow pushed along a path)
 * @param phase      Dinic phase number (1-based), or the augmentation number for the other algorithms
 * @param levels     BFS level of every node for PHASE steps (-1 = unreachable); otherwise null
 * @param pathEdges  raw residual-edge ids of the augmenting path (empty for PHASE steps)
 * @param pathNodes  node ids along the path (empty for PHASE steps)
 * @param bottleneck amount of flow pushed along the path
 * @param totalFlow  flow value after this step
 * @param flows      flow of every forward edge after this step
 */
public record FlowStep(Kind kind, int phase, int[] levels, int[] pathEdges, int[] pathNodes, int bottleneck,
                       int totalFlow, int[] flows) {

    /** Type of event. */
    public enum Kind { PHASE, AUGMENT }
}
