package com.fleetflow.ui.panel.np;

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
import javafx.scene.layout.HBox;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/** CLIQUE → INDEPENDENT-SET page: shows a graph and its complement side by side. */
public final class CliqueIndependentSetPanel extends AlgorithmPanel {

    private static final double W = 440;
    private static final double H = 340;

    private final GraphInput input = new GraphInput();
    private final GraphVisualizer original = new GraphVisualizer(W, H);
    private final GraphVisualizer complement = new GraphVisualizer(W, H);

    public CliqueIndependentSetPanel() {
        HBox both = new HBox(14, original, complement);
        ScrollPane scroll = new ScrollPane(both);
        scroll.getStyleClass().add("viz-scroll");
        scroll.setPrefViewportHeight(H + 24);
        assemble("CLIQUE → INDEPENDENT SET",
                "Real problem: 'a group of mutually compatible depots' (clique) versus 'a group of mutually conflicting depots that can be served independently' "
                        + "(independent set). Complementing the graph (adding exactly the missing edges) turns every clique of G into an independent set of the same size "
                        + "in Ḡ and vice-versa, so the two problems are equivalent. Left: graph G. Right: complement Ḡ.",
                input, scroll);
        loadDemo();
    }

    @Override
    protected void loadDemo() {
        input.set(7, "0-1, 0-2, 1-2, 1-3, 2-3, 3-4, 4-5, 5-6, 4-6");
    }

    @Override
    protected Supplier<Execution> prepare() throws ValidationException {
        SimpleGraph g = input.read();
        return () -> execute(g);
    }

    private Execution execute(SimpleGraph g) {
        SimpleGraph c = Reductions.cliqueToIndependentSet(g);
        List<Integer> max = CliqueSearch.maximumClique(g);
        CliqueSearch.Result search = CliqueSearch.findClique(g, Math.max(1, max.size()));
        boolean ok = Reductions.isClique(g, max) && Reductions.isIndependentSet(c, max);
        String result = "Maximum clique of G = " + max + " (size " + max.size() + ")\nThe same vertices form an independent set of size " + max.size()
                + " in the complement Ḡ: " + (ok ? "verified ✓ (no two of them are adjacent in Ḡ)" : "verification failed ✗")
                + "\nG has " + g.edgeCount() + " edges, Ḡ has " + c.edgeCount() + " (together " + (g.edgeCount() + c.edgeCount()) + " = n(n−1)/2 = "
                + g.vertexCount() * (g.vertexCount() - 1) / 2 + ").";
        String complexity = "Reduction O(n²) – polynomial   |   Clique search here exponential O(2ⁿ) in the worst case";
        int frames = 1 + search.trace().size() + 1;
        return new Execution(frames, i -> draw(g, c, max, search, i), i -> narrate(max, search, i), result, complexity, search.trace().size(),
                search.trace().size(), "Backtracking examined " + search.trace().size() + " partial clique(s).");
    }

    private void draw(SimpleGraph g, SimpleGraph c, List<Integer> max, CliqueSearch.Result search, int frame) {
        int n = g.vertexCount();
        boolean isFinal = frame >= 1 + search.trace().size();
        Set<Integer> set = new HashSet<>();
        CellState st = CellState.ACTIVE;
        if (frame >= 1 && !isFinal) {
            for (int v : search.trace().get(frame - 1)) set.add(v);
        } else if (isFinal) {
            set.addAll(max);
            st = CellState.MATCH;
        }
        CellState[] states = new CellState[n];
        for (int v : set) states[v] = st;
        double[][] pos = GraphLayouts.circle(n, W, H);
        final CellState hi = st;
        GraphPaint.paint(original, g, pos, null, states, (a, b) -> set.contains(a) && set.contains(b) ? hi : CellState.NORMAL);
        GraphPaint.paint(complement, c, pos, null, states, (a, b) -> CellState.NORMAL);
    }

    private static String narrate(List<Integer> max, CliqueSearch.Result search, int frame) {
        if (frame == 0) {
            return "Graph G (left) and its complement Ḡ (right) on the same vertices. An edge appears in Ḡ exactly when it is missing in G.";
        }
        if (frame < 1 + search.trace().size()) {
            int[] p = search.trace().get(frame - 1);
            return "Searching for a clique of size " + max.size() + ": current candidate " + java.util.Arrays.toString(p)
                    + ". In G these vertices are pairwise adjacent only if it is a clique; in Ḡ the same vertices are then pairwise NON-adjacent.";
        }
        return "Result: " + max + " is a clique in G (all pairs joined) and an independent set in Ḡ (no pair joined) – the reduction is correct.";
    }
}
