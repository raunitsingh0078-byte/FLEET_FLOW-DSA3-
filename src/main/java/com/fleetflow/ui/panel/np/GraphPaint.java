package com.fleetflow.ui.panel.np;

import com.fleetflow.datastructure.graph.SimpleGraph;
import com.fleetflow.visualization.CellState;
import com.fleetflow.visualization.GraphVisualizer;
import com.fleetflow.visualization.GraphVisualizer.GEdge;
import com.fleetflow.visualization.GraphVisualizer.GNode;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

/** Draws a {@link SimpleGraph} on a {@link GraphVisualizer} with per-node and per-edge states. */
final class GraphPaint {

    private GraphPaint() {
    }

    /**
     * @param states    state of each vertex (null = NORMAL)
     * @param edgeState state of an edge given its endpoints (null result = NORMAL, hidden if the function returns MUTED and hideMuted)
     */
    static void paint(GraphVisualizer viz, SimpleGraph g, double[][] pos, String[] labels, CellState[] states,
                      BiFunction<Integer, Integer, CellState> edgeState) {
        List<GNode> nodes = new ArrayList<>();
        for (int v = 0; v < g.vertexCount(); v++) {
            GNode n = new GNode(v, labels == null ? String.valueOf(v) : labels[v], pos[v][0], pos[v][1]);
            if (states != null && states[v] != null) {
                n.state = states[v];
            }
            nodes.add(n);
        }
        List<GEdge> edges = new ArrayList<>();
        for (int[] e : g.edges()) {
            GEdge edge = new GEdge(e[0], e[1]);
            edge.directed = false;
            CellState st = edgeState == null ? null : edgeState.apply(e[0], e[1]);
            if (st != null) {
                edge.state = st;
                if (st != CellState.NORMAL && st != CellState.MUTED) {
                    edge.width = 3.4;
                }
            }
            edges.add(edge);
        }
        viz.render(nodes, edges);
    }
}
