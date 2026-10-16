package com.fleetflow.ui.panel.dp;

import com.fleetflow.algorithm.dp.MatrixChain;
import com.fleetflow.ui.AlgorithmPanel;
import com.fleetflow.ui.Execution;
import com.fleetflow.ui.Ui;
import com.fleetflow.util.ValidationException;
import com.fleetflow.visualization.CellState;
import com.fleetflow.visualization.MatrixVisualizer;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import java.util.Arrays;
import java.util.function.Supplier;

/** Matrix-chain page: finds the cheapest order in which to combine a sequence of logistics operations. */
public final class MatrixChainPanel extends AlgorithmPanel {

    private static final int MAX_MATRICES = 10;

    private final TextField dimsField = new TextField();
    private final MatrixVisualizer matrix = new MatrixVisualizer();

    public MatrixChainPanel() {
        dimsField.setPromptText("Dimensions p0, p1, …, pn (operation k transforms a p(k-1) × p(k) table)");
        HBox row = new HBox(8, Ui.label("Dimensions", "kv-key"), dimsField);
        HBox.setHgrow(dimsField, Priority.ALWAYS);
        matrix.setPrefViewportHeight(330);
        assemble("Matrix Chain Multiplication – Operation Sequencing",
                "Real problem: a sequence of logistics operations (e.g. consolidate → re-route → allocate) can be combined in different orders with very "
                        + "different costs. Modelled as multiplying matrices A1…An where Ak is p(k-1) × p(k); m[i][j] = min over splits k of "
                        + "m[i][k] + m[k+1][j] + p(i-1)·p(k)·p(j).",
                row, matrix);
        loadDemo();
    }

    @Override
    protected void loadDemo() {
        dimsField.setText("30, 35, 15, 5, 10, 20, 25");
    }

    @Override
    protected Supplier<Execution> prepare() throws ValidationException {
        String raw = dimsField.getText() == null ? "" : dimsField.getText().trim();
        if (raw.isEmpty()) {
            throw new ValidationException("Please enter the dimensions separated by commas.");
        }
        String[] parts = raw.split("[,\\s]+");
        int[] dims = new int[parts.length];
        try {
            for (int i = 0; i < parts.length; i++) {
                dims[i] = Integer.parseInt(parts[i]);
                if (dims[i] < 1 || dims[i] > 1000) {
                    throw new ValidationException("Every dimension must be between 1 and 1000.");
                }
            }
        } catch (NumberFormatException e) {
            throw new ValidationException("Dimensions must be whole numbers separated by commas.");
        }
        if (dims.length < 3 || dims.length > MAX_MATRICES + 1) {
            throw new ValidationException("Please enter between 3 and " + (MAX_MATRICES + 1) + " dimensions (2 to " + MAX_MATRICES + " operations).");
        }
        return () -> execute(dims);
    }

    private Execution execute(int[] dims) {
        MatrixChain.Result r = MatrixChain.compute(dims);
        int n = dims.length - 1;
        long naive = 0;
        long rows = dims[0];
        for (int k = 1; k < n; k++) { // left-to-right order for comparison
            naive += rows * dims[k] * dims[k + 1];
        }
        String result = "Minimum cost: " + r.minCost() + " scalar multiplications\nOptimal order: " + r.parenthesization();
        String complexity = "Time O(n³) = O(" + n + "³)   |   Space O(n²)";
        String info = "Multiplying strictly left-to-right would cost " + naive + " (" + (naive == r.minCost() ? "same as optimal" : String.format("%.1f× more", (double) naive / r.minCost())) + ").";
        long candidates = r.steps().stream().mapToLong(s -> s.candidates().length).sum();
        return new Execution(r.steps().size(), f -> draw(dims, r, f), f -> narrate(r, f), result, complexity, candidates, r.steps().size(), info);
    }

    private void draw(int[] dims, MatrixChain.Result r, int frame) {
        int n = dims.length - 1;
        String[] rowH = new String[n];
        String[] colH = new String[n];
        for (int i = 0; i < n; i++) {
            rowH[i] = "A" + (i + 1) + " " + dims[i] + "×" + dims[i + 1];
            colH[i] = "A" + (i + 1);
        }
        String[][] values = new String[n][n];
        CellState[][] states = new CellState[n][n];
        for (int i = 0; i < n; i++) {
            values[i][i] = "0";
            states[i][i] = CellState.MUTED;
        }
        for (int s = 0; s <= frame; s++) {
            MatrixChain.Step st = r.steps().get(s);
            values[st.i() - 1][st.j() - 1] = String.valueOf(st.cost());
            states[st.i() - 1][st.j() - 1] = CellState.NORMAL;
        }
        MatrixChain.Step cur = r.steps().get(frame);
        states[cur.i() - 1][cur.j() - 1] = CellState.ACTIVE;
        states[cur.i() - 1][cur.bestSplit() - 1] = CellState.WINDOW;
        states[cur.bestSplit()][cur.j() - 1] = CellState.WINDOW;
        if (frame == r.steps().size() - 1) {
            states[0][n - 1] = CellState.MATCH;
        }
        matrix.show(rowH, colH, values, states, cur.j() - 1);
    }

    private static String narrate(MatrixChain.Result r, int frame) {
        MatrixChain.Step s = r.steps().get(frame);
        return "m[" + s.i() + "][" + s.j() + "]: tried " + s.candidates().length + " split(s) " + Arrays.toString(s.candidates())
                + " → best split after A" + s.bestSplit() + " with cost " + s.cost() + " (the two amber cells are its sub-chains).";
    }
}
