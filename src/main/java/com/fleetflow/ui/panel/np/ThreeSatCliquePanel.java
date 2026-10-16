package com.fleetflow.ui.panel.np;

import com.fleetflow.algorithm.np.CliqueSearch;
import com.fleetflow.algorithm.np.Reductions;
import com.fleetflow.algorithm.np.Sat;
import com.fleetflow.datastructure.graph.SimpleGraph;
import com.fleetflow.ui.AlgorithmPanel;
import com.fleetflow.ui.Execution;
import com.fleetflow.util.ValidationException;
import com.fleetflow.visualization.CellState;
import com.fleetflow.visualization.GraphVisualizer;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/** 3-SAT → CLIQUE page: builds the reduction graph and finds the clique that encodes a satisfying assignment. */
public final class ThreeSatCliquePanel extends AlgorithmPanel {

    private static final double W = 900;
    private static final double H = 360;
    private static final int MAX_CLAUSES = 5;

    private final TextArea clausesArea = new TextArea();
    private final GraphVisualizer graph = new GraphVisualizer(W, H);

    public ThreeSatCliquePanel() {
        clausesArea.setPrefRowCount(4);
        clausesArea.setPromptText("3-CNF: one clause per line, at most 3 literals each, at most " + MAX_CLAUSES + " clauses");
        ScrollPane scroll = new ScrollPane(graph);
        scroll.getStyleClass().add("viz-scroll");
        scroll.setPrefViewportHeight(H + 8);
        assemble("3-SAT → CLIQUE (polynomial-time reduction)",
                "Real problem: prove that clique is at least as hard as 3-SAT. Every literal occurrence becomes a vertex (one column per clause). Two vertices are joined "
                        + "when they are in different clauses and not contradictory (x vs ¬x). A clique of size k = #clauses picks one true literal per clause – "
                        + "consistently – so the formula is satisfiable exactly when such a clique exists. Educational demonstration – exponential problems are restricted to small input sizes.",
                clausesArea, scroll);
        loadDemo();
    }

    @Override
    protected void loadDemo() {
        clausesArea.setText("x1 x2 x3\n!x1 x2 !x3\nx1 !x2 x3");
    }

    @Override
    protected Supplier<Execution> prepare() throws ValidationException {
        Sat.Formula f;
        try {
            f = Sat.parse(clausesArea.getText());
        } catch (IllegalArgumentException e) {
            throw new ValidationException(e.getMessage());
        }
        if (!f.isThreeSat()) {
            throw new ValidationException("This reduction needs a 3-CNF formula: every clause may have at most 3 literals.");
        }
        if (f.clauses().size() > MAX_CLAUSES || f.variables() > 8) {
            throw new ValidationException("Please use at most " + MAX_CLAUSES + " clauses and 8 variables so the graph stays readable.");
        }
        Sat.Formula formula = f;
        return () -> execute(formula);
    }

    private static String literalName(int lit) {
        return (lit < 0 ? "¬" : "") + "x" + Math.abs(lit);
    }

    private Execution execute(Sat.Formula f) {
        Reductions.ThreeSatGraph tg = Reductions.threeSatToClique(f);
        int k = tg.clauses();
        CliqueSearch.Result search = CliqueSearch.findClique(tg.graph(), k);
        boolean brute = Sat.solve(f).satisfiable();
        StringBuilder result = new StringBuilder("Reduction graph: " + tg.graph().vertexCount() + " vertices, " + tg.graph().edgeCount() + " edges; target clique size k = " + k + ".");
        if (search.clique() != null) {
            boolean[] a = Reductions.assignmentFromClique(f, tg, search.clique());
            result.append("\nA ").append(k).append("-clique exists: ");
            for (int v : search.clique()) result.append(literalName(tg.literalOf()[v])).append("(C").append(tg.clauseOf()[v] + 1).append(") ");
            result.append("\nAssignment read from the clique: ");
            for (int i = 0; i < a.length; i++) result.append("x").append(i + 1).append("=").append(a[i] ? "T " : "F ");
            result.append("\nCheck against the formula: ").append(Sat.satisfies(f, a) ? "satisfied ✓" : "NOT satisfied ✗");
        } else {
            result.append("\nNo ").append(k).append("-clique exists ⇒ the formula is UNSATISFIABLE (brute force agrees: ").append(!brute).append(").");
        }
        String complexity = "Reduction O(m²) – polynomial   |   Clique search here O(n^k) backtracking (exponential in k)";
        int frames = 2 + search.trace().size() + 1;
        return new Execution(frames, i -> draw(f, tg, search, i), i -> narrate(f, tg, search, i), result.toString(), complexity,
                search.trace().size(), search.trace().size(), "Backtracking examined " + search.trace().size() + " partial clique(s).");
    }

    private double[][] positions(Reductions.ThreeSatGraph tg) {
        int n = tg.graph().vertexCount();
        double[][] pos = new double[n][2];
        int[] rowInClause = new int[tg.clauses()];
        for (int v = 0; v < n; v++) {
            int c = tg.clauseOf()[v];
            pos[v][0] = tg.clauses() == 1 ? W / 2 : 90 + c * (W - 180) / (tg.clauses() - 1);
            pos[v][1] = 70 + rowInClause[c]++ * 110;
        }
        return pos;
    }

    private void draw(Sat.Formula f, Reductions.ThreeSatGraph tg, CliqueSearch.Result search, int frame) {
        int n = tg.graph().vertexCount();
        String[] labels = new String[n];
        for (int v = 0; v < n; v++) labels[v] = literalName(tg.literalOf()[v]);
        Set<Integer> highlight = new HashSet<>();
        boolean edgesVisible = frame >= 1;
        boolean isFinal = frame >= 2 + search.trace().size();
        CellState hs = CellState.ACTIVE;
        if (frame >= 2 && !isFinal) {
            for (int v : search.trace().get(frame - 2)) highlight.add(v);
        } else if (isFinal && search.clique() != null) {
            highlight.addAll(search.clique());
            hs = CellState.MATCH;
        }
        CellState[] states = new CellState[n];
        for (int v : highlight) states[v] = hs;
        final CellState edgeHi = hs == CellState.MATCH ? CellState.MATCH : CellState.ACTIVE;
        SimpleGraph shown = edgesVisible ? tg.graph() : new SimpleGraph(n);
        GraphPaint.paint(graph, shown, positions(tg), labels, states,
                (a, b) -> highlight.contains(a) && highlight.contains(b) ? edgeHi : CellState.NORMAL);
    }

    private static String narrate(Sat.Formula f, Reductions.ThreeSatGraph tg, CliqueSearch.Result search, int frame) {
        if (frame == 0) {
            return "Step 1 – one vertex per literal occurrence; each column is one clause (a clique must take at most one vertex from every column).";
        }
        if (frame == 1) {
            return "Step 2 – connect literals of different clauses unless they contradict each other (x and ¬x). An edge means 'these two choices can both be true'.";
        }
        if (frame < 2 + search.trace().size()) {
            int[] partial = search.trace().get(frame - 2);
            StringBuilder sb = new StringBuilder();
            for (int v : partial) sb.append(literalName(tg.literalOf()[v])).append(' ');
            return "Step 3 – backtracking search for a " + tg.clauses() + "-clique. Current partial clique: " + (partial.length == 0 ? "(empty)" : sb.toString().trim())
                    + ". Extend it with a vertex adjacent to all chosen ones, or backtrack.";
        }
        return search.clique() != null ? "Clique of size " + tg.clauses() + " found (green): one consistent true literal per clause, so setting those literals true satisfies the formula."
                : "The search is exhausted: no clique of size " + tg.clauses() + " exists, hence no satisfying assignment.";
    }
}
