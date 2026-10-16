package com.fleetflow.ui.panel.np;

import com.fleetflow.algorithm.approximation.VertexCoverApprox;
import com.fleetflow.algorithm.np.Reductions;
import com.fleetflow.datastructure.graph.SimpleGraph;
import com.fleetflow.ui.AlgorithmPanel;
import com.fleetflow.ui.Execution;
import com.fleetflow.util.ValidationException;
import com.fleetflow.visualization.CellState;
import com.fleetflow.visualization.GraphLayouts;
import com.fleetflow.visualization.GraphVisualizer;
import javafx.scene.control.ScrollPane;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/** Vertex-cover 2-approximation page: identifies important conflict locations with a guaranteed factor-2 bound. */
public final class VertexCoverPanel extends AlgorithmPanel {

    private static final double W = 900;
    private static final double H = 370;

    private final GraphInput input = new GraphInput();
    private final GraphVisualizer graph = new GraphVisualizer(W, H);

    public VertexCoverPanel() {
        ScrollPane scroll = new ScrollPane(graph);
        scroll.getStyleClass().add("viz-scroll");
        scroll.setPrefViewportHeight(H + 8);
        assemble("Vertex Cover – 2-Approximation",
                "Real problem: pick a small set of locations (e.g. checkpoints) that touches every conflicting lane. Minimum vertex cover is NP-hard, but this greedy rule is "
                        + "guaranteed within a factor 2: repeatedly take any uncovered edge and add BOTH endpoints. The chosen edges are a matching, and any cover needs at least one "
                        + "endpoint per matching edge, so the result is at most 2·OPT.",
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
            throw new ValidationException("The graph has no edges, so the cover is empty. Add some edges.");
        }
        return () -> execute(g);
    }

    private Execution execute(SimpleGraph g) {
        VertexCoverApprox.Result r = VertexCoverApprox.approximate(g);
        List<Integer> opt = VertexCoverApprox.optimal(g);
        boolean valid = Reductions.isVertexCover(g, r.cover());
        String result = "Approximate cover " + sorted(r.cover()) + " (size " + r.cover().size() + ") from " + r.matching().size() + " matching edge(s)\nOptimal cover "
                + opt + " (size " + opt.size() + ")\nApproximation ratio " + String.format("%.2f", (double) r.cover().size() / opt.size()) + " ≤ 2 → "
                + (r.cover().size() <= 2 * opt.size() ? "guarantee holds ✓" : "violated ✗") + "; the cover is " + (valid ? "valid ✓" : "INVALID ✗");
        String complexity = "Approximation O(V + E) (one pass over the edges) with ratio ≤ 2   |   Exact minimum cover here O(2ⁿ·E), NP-hard";
        int frames = r.steps().size() + 2;
        return new Execution(frames, i -> draw(g, r, opt, i), i -> narrate(g, r, opt, i), result, complexity, g.edgeCount(), r.steps().size(),
                "Picked edges (matching): " + describeEdges(r.matching()));
    }

    private static List<Integer> sorted(List<Integer> list) {
        List<Integer> copy = new ArrayList<>(list);
        copy.sort(Integer::compare);
        return copy;
    }

    private static String describeEdges(List<int[]> edges) {
        StringBuilder sb = new StringBuilder();
        for (int[] e : edges) sb.append(e[0]).append("–").append(e[1]).append("  ");
        return sb.toString().trim();
    }

    private void draw(SimpleGraph g, VertexCoverApprox.Result r, List<Integer> opt, int frame) {
        int n = g.vertexCount();
        int steps = r.steps().size();
        boolean isFinal = frame > steps;
        Set<Integer> cover = new HashSet<>();
        Set<Long> covered = new HashSet<>();
        for (int s = 0; s < Math.min(frame, steps); s++) {
            VertexCoverApprox.Step st = r.steps().get(s);
            cover.add(st.u());
            cover.add(st.v());
            for (int[] e : st.newlyCovered()) covered.add(key(e[0], e[1]));
        }
        VertexCoverApprox.Step current = frame >= 1 && frame <= steps ? r.steps().get(frame - 1) : null;
        CellState[] states = new CellState[n];
        for (int v : cover) states[v] = isFinal ? CellState.MATCH : CellState.DONE;
        if (current != null) {
            states[current.u()] = CellState.ACTIVE;
            states[current.v()] = CellState.ACTIVE;
        }
        if (isFinal) for (int v : opt) if (states[v] == null) states[v] = CellState.WINDOW;
        GraphPaint.paint(graph, g, GraphLayouts.circle(n, W, H), null, states, (a, b) -> {
            if (current != null && ((a == current.u() && b == current.v()) || (a == current.v() && b == current.u()))) return CellState.ACTIVE;
            return covered.contains(key(a, b)) ? CellState.DONE : CellState.NORMAL;
        });
    }

    private static long key(int a, int b) {
        return ((long) Math.min(a, b) << 32) | Math.max(a, b);
    }

    private static String narrate(SimpleGraph g, VertexCoverApprox.Result r, List<Integer> opt, int frame) {
        int steps = r.steps().size();
        if (frame == 0) {
            return "Start: the cover is empty and all " + g.edgeCount() + " edges are uncovered.";
        }
        if (frame > steps) {
            return "Every edge is covered. Green = approximate cover (" + r.cover().size() + " vertices); amber outlines vertices that an optimal cover of size " + opt.size()
                    + " would use instead. The ratio is at most 2 because the picked edges share no endpoints.";
        }
        VertexCoverApprox.Step s = r.steps().get(frame - 1);
        return "Pick uncovered edge " + s.u() + "–" + s.v() + " and add BOTH endpoints to the cover. This covers " + s.newlyCovered().size()
                + " edge(s) (all edges touching " + s.u() + " or " + s.v() + "). Cover size is now " + s.coverSize() + ".";
    }
}
