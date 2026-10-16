package com.fleetflow.ui.panel.dp;

import com.fleetflow.algorithm.dp.OptimalBst;
import com.fleetflow.ui.AlgorithmPanel;
import com.fleetflow.ui.Execution;
import com.fleetflow.ui.Ui;
import com.fleetflow.util.ValidationException;
import com.fleetflow.visualization.CellState;
import com.fleetflow.visualization.MatrixVisualizer;
import com.fleetflow.visualization.TreeVisualizer;
import com.fleetflow.visualization.TreeVisualizer.VNode;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Supplier;

/** Optimal-BST page: arranges frequently accessed shipment keys to minimise the expected search cost. */
public final class OptimalBstPanel extends AlgorithmPanel {

    private static final int MAX_KEYS = 9;

    private final TextField keysField = new TextField();
    private final MatrixVisualizer matrix = new MatrixVisualizer();
    private final TreeVisualizer tree = new TreeVisualizer(90);

    public OptimalBstPanel() {
        keysField.setPromptText("key:frequency, key:frequency, …");
        HBox row = new HBox(8, Ui.label("Keys", "kv-key"), keysField);
        HBox.setHgrow(keysField, Priority.ALWAYS);
        matrix.setPrefViewportHeight(240);
        tree.setPrefViewportHeight(260);
        assemble("Optimal Binary Search Tree – Hot Shipment Keys",
                "Real problem: some shipment IDs are looked up far more often than others. An optimal BST puts hot keys near the root to minimise "
                        + "Σ frequency × depth. cost[i][j] = Σ freq(i..j) + min over roots r of cost[i][r-1] + cost[r+1][j]. "
                        + "The tree shows the best subtree for the interval being computed.",
                row, new VBox(10, matrix, tree));
        loadDemo();
    }

    @Override
    protected void loadDemo() {
        keysField.setText("SHP-2003:34, SHP-2005:8, SHP-2008:50, SHP-2010:21, SHP-2014:5, SHP-2017:42");
    }

    @Override
    protected Supplier<Execution> prepare() throws ValidationException {
        String raw = keysField.getText() == null ? "" : keysField.getText().trim();
        if (raw.isEmpty()) {
            throw new ValidationException("Please enter keys as key:frequency pairs.");
        }
        TreeMap<String, Long> sorted = new TreeMap<>();
        for (String part : raw.split(",")) {
            String p = part.trim();
            if (p.isEmpty()) continue;
            int colon = p.lastIndexOf(':');
            if (colon <= 0 || colon == p.length() - 1) {
                throw new ValidationException("\"" + p + "\" is not in the form key:frequency.");
            }
            try {
                long f = Long.parseLong(p.substring(colon + 1).trim());
                if (f < 0) throw new ValidationException("Frequencies must not be negative.");
                if (sorted.put(p.substring(0, colon).trim(), f) != null) {
                    throw new ValidationException("Key \"" + p.substring(0, colon).trim() + "\" appears twice.");
                }
            } catch (NumberFormatException e) {
                throw new ValidationException("The frequency in \"" + p + "\" is not a whole number.");
            }
        }
        if (sorted.isEmpty() || sorted.size() > MAX_KEYS) {
            throw new ValidationException("Please enter between 1 and " + MAX_KEYS + " keys.");
        }
        String[] keys = sorted.keySet().toArray(new String[0]);
        long[] freq = sorted.values().stream().mapToLong(Long::longValue).toArray();
        return () -> execute(keys, freq);
    }

    private Execution execute(String[] keys, long[] freq) {
        OptimalBst.Result r = OptimalBst.compute(keys, freq);
        long balanced = balanced(freq, 0, keys.length - 1, 1);
        String result = "Minimum expected search cost (Σ freq × depth): " + r.minCost() + "\nRoot key: " + r.tree().key
                + "\nKeys are stored in sorted order: " + String.join(", ", keys);
        int n = keys.length;
        String complexity = "Time O(n³) = O(" + n + "³) (O(n²) with Knuth's optimisation)   |   Space O(n²)";
        String info = "A perfectly balanced BST would cost " + balanced + " (" + (balanced == r.minCost() ? "equal to optimal" : String.format("%.2f× the optimum", (double) balanced / r.minCost())) + ").";
        long comparisons = 0;
        for (OptimalBst.Step s : r.steps()) comparisons += s.j() - s.i() + 1;
        return new Execution(r.steps().size(), f -> draw(keys, freq, r, f), f -> narrate(keys, r, f), result, complexity, comparisons, r.steps().size(), info);
    }

    private static long balanced(long[] f, int i, int j, int depth) {
        if (i > j) return 0;
        int mid = (i + j) / 2;
        return f[mid] * depth + balanced(f, i, mid - 1, depth + 1) + balanced(f, mid + 1, j, depth + 1);
    }

    private void draw(String[] keys, long[] freq, OptimalBst.Result r, int frame) {
        int n = keys.length;
        String[] rowH = new String[n];
        String[] colH = new String[n];
        for (int i = 0; i < n; i++) {
            rowH[i] = keys[i] + " (" + freq[i] + ")";
            colH[i] = String.valueOf(i);
        }
        String[][] values = new String[n][n];
        CellState[][] states = new CellState[n][n];
        for (int s = 0; s <= frame; s++) {
            OptimalBst.Step st = r.steps().get(s);
            values[st.i()][st.j()] = String.valueOf(st.cost());
            states[st.i()][st.j()] = CellState.NORMAL;
        }
        OptimalBst.Step cur = r.steps().get(frame);
        states[cur.i()][cur.j()] = frame == r.steps().size() - 1 ? CellState.MATCH : CellState.ACTIVE;
        matrix.show(rowH, colH, values, states, cur.j());
        Map<Integer, CellState> nodeStates = new HashMap<>();
        VNode root = subtree(keys, freq, r.root(), cur.i(), cur.j(), nodeStates);
        nodeStates.put(cur.root(), CellState.ACTIVE);
        tree.render(root, nodeStates, List.of());
    }

    private static VNode subtree(String[] keys, long[] freq, int[][] root, int i, int j, Map<Integer, CellState> states) {
        int rt = root[i][j];
        VNode v = new VNode(rt, keys[rt] + " (" + freq[rt] + ")", null, false);
        if (rt > i) v.add(subtree(keys, freq, root, i, rt - 1, states));
        if (rt < j) v.add(subtree(keys, freq, root, rt + 1, j, states));
        return v;
    }

    private static String narrate(String[] keys, OptimalBst.Result r, int frame) {
        OptimalBst.Step s = r.steps().get(frame);
        return "Interval " + keys[s.i()] + " … " + keys[s.j()] + ": trying each key as root, the best is " + keys[s.root()]
                + " with total cost " + s.cost() + ". The tree on the right is the optimal subtree of this interval.";
    }
}
