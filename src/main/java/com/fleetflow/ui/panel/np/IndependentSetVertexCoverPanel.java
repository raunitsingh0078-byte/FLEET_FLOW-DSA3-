package com.fleetflow.ui.panel.np;

import com.fleetflow.algorithm.approximation.VertexCoverApprox;
import com.fleetflow.algorithm.np.CliqueSearch;
import com.fleetflow.algorithm.np.Reductions;
import com.fleetflow.datastructure.graph.SimpleGraph;
import com.fleetflow.ui.AlgorithmPanel;
import com.fleetflow.ui.Execution;
import com.fleetflow.util.ValidationException;
import com.fleetflow.visualization.CellState;
import com.fleetflow.visualization.GraphLayouts;
import com.fleetflow.visualization.GraphVisualizer;
import javafx.scene.control.ScrollPane;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/** INDEPENDENT-SET → VERTEX-COVER page: the complement of an independent set covers every edge. */
public final class IndependentSetVertexCoverPanel extends AlgorithmPanel {

    private static final double W = 900;
    private static final double H = 370;

    private final GraphInput input = new GraphInput();
    private final GraphVisualizer graph = new GraphVisualizer(W, H);

    public IndependentSetVertexCoverPanel() {
        ScrollPane scroll = new ScrollPane(graph);
        scroll.getStyleClass().add("viz-scroll");
        scroll.setPrefViewportHeight(H + 8);
        assemble("INDEPENDENT SET → VERTEX COVER",
                "Real problem: choosing conflict-free locations (independent set S) is the mirror image of choosing locations that watch every conflict (vertex cover). "
                        + "S is independent exactly when V \\ S touches every edge, so |S| + |V \\ S| = n and a MAXIMUM independent set gives a MINIMUM vertex cover.",
                input, scroll);
        loadDemo();
    }

    @Override
    protected void loadDemo() {
        input.set(8, "0-1, 0-2, 1-2, 2-3, 3-4, 4-5, 4-6, 5-6, 6-7");
    }

    @Override
    protected Supplier<Execution> prepare() throws ValidationException {
        SimpleGraph g = input.read();
        if (g.edgeCount() == 0) {
            throw new ValidationException("The graph has no edges, so there is nothing to cover. Add some edges.");
        }
        return () -> execute(g);
    }

    private Execution execute(SimpleGraph g) {
        List<Integer> independent = CliqueSearch.maximumClique(g.complement());
        List<Integer> cover = Reductions.vertexCoverFromIndependentSet(g, independent);
        List<Integer> optimum = VertexCoverApprox.optimal(g);
        boolean ok = Reductions.isIndependentSet(g, independent) && Reductions.isVertexCover(g, cover);
        String result = "Maximum independent set S = " + independent + " (size " + independent.size() + ")\nVertex cover V \\ S = " + cover + " (size " + cover.size() + ")"
                + "\nChecks: S independent and V \\ S covers all " + g.edgeCount() + " edges → " + (ok ? "✓" : "✗")
                + "\n|S| + |cover| = " + (independent.size() + cover.size()) + " = n = " + g.vertexCount()
                + "\nMinimum vertex cover by exhaustive search has size " + optimum.size() + (optimum.size() == cover.size() ? " – identical ✓" : " – MISMATCH ✗");
        String complexity = "Reduction O(n) – polynomial   |   Finding a maximum independent set is NP-hard (exponential search here)";
        int frames = 3 + g.edgeCount();
        return new Execution(frames, i -> draw(g, independent, cover, i), i -> narrate(g, independent, cover, i), result, complexity, g.edgeCount(), frames,
                "Exhaustive verification tried up to 2ⁿ = " + (1L << g.vertexCount()) + " subsets.");
    }

    private void draw(SimpleGraph g, List<Integer> independent, List<Integer> cover, int frame) {
        int n = g.vertexCount();
        CellState[] states = new CellState[n];
        if (frame >= 1) for (int v : independent) states[v] = CellState.MATCH;
        if (frame >= 2) for (int v : cover) states[v] = CellState.HIGHLIGHT;
        Set<Integer> coverSet = new HashSet<>(cover);
        int checked = frame - 3; // edges 0..checked have been verified
        List<int[]> edges = g.edges();
        GraphPaint.paint(graph, g, GraphLayouts.circle(n, W, H), null, states, (a, b) -> {
            for (int i = 0; i <= checked && i < edges.size(); i++) {
                if (edges.get(i)[0] == a && edges.get(i)[1] == b) {
                    return i == checked ? CellState.ACTIVE : CellState.DONE;
                }
            }
            return CellState.NORMAL;
        });
        if (coverSet.isEmpty()) {
            return;
        }
    }

    private static String narrate(SimpleGraph g, List<Integer> independent, List<Integer> cover, int frame) {
        if (frame == 0) {
            return "The graph G: edges are conflicts between locations.";
        }
        if (frame == 1) {
            return "Green: a maximum independent set S = " + independent + " – no two of these vertices are joined by an edge.";
        }
        if (frame == 2) {
            return "Purple: the complement V \\ S = " + cover + ". Every edge has at most one end in S, so its other end must lie in V \\ S – it is a vertex cover.";
        }
        int[] e = g.edges().get(frame - 3);
        int end = cover.contains(e[0]) ? e[0] : e[1];
        return "Edge " + e[0] + "–" + e[1] + " is covered by vertex " + end + " (in V \\ S). Checking every edge proves that V \\ S is a vertex cover.";
    }
}
