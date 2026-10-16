package com.fleetflow.ui.panel.parallel;

import com.fleetflow.algorithm.parallel.BrentsTheorem;
import com.fleetflow.ui.AlgorithmPanel;
import com.fleetflow.ui.Execution;
import com.fleetflow.ui.Ui;
import com.fleetflow.util.ValidationException;
import com.fleetflow.visualization.CellState;
import com.fleetflow.visualization.RowTableVisualizer;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/** Brent's-theorem page: work W, span S, processors P → execution-time bounds and speed-up. */
public final class BrentPanel extends AlgorithmPanel {

    private final TextField workField = new TextField("100000");
    private final TextField spanField = new TextField("200");
    private final TextField maxPField = new TextField("1024");
    private final LineChart<String, Number> chart;
    private final RowTableVisualizer table = new RowTableVisualizer("P", "Lower bound max(W/P, S)", "Brent bound (W−S)/P + S", "W/P + S", "Guaranteed speed-up", "Efficiency");

    public BrentPanel() {
        NumberAxis y = new NumberAxis();
        y.setLabel("speed-up");
        chart = new LineChart<>(new CategoryAxis(), y);
        chart.setAnimated(false);
        chart.setTitle("Speed-up guaranteed by Brent's theorem vs. ideal");
        chart.setPrefHeight(280);
        table.setPrefHeight(230);
        assemble("Brent's Theorem – Work, Span and Processors",
                "Real problem: how many processors are worth buying for a logistics job? A parallel computation has total work W (time on one processor) and span S (longest chain of "
                        + "dependent steps). A greedy scheduler on P processors finishes in T_P ≤ (W − S)/P + S, i.e. T_P = O(W/P + S). Speed-up can never exceed the parallelism W/S, so "
                        + "processors beyond that add nothing. Examples: parallel reduce has W = n, S = log n; Blelloch scan W = 2n, S = 2 log n.",
                new HBox(12, Ui.label("Work W", "kv-key"), workField, Ui.label("Span S", "kv-key"), spanField, Ui.label("Max processors", "kv-key"), maxPField),
                new VBox(10, chart, table));
        loadDemo();
    }

    @Override
    protected void loadDemo() {
        workField.setText("100000");
        spanField.setText("200");
        maxPField.setText("1024");
    }

    @Override
    protected Supplier<Execution> prepare() throws ValidationException {
        double w;
        double s;
        int maxP;
        try {
            w = Double.parseDouble(workField.getText().trim());
            s = Double.parseDouble(spanField.getText().trim());
            maxP = Integer.parseInt(maxPField.getText().trim());
        } catch (NumberFormatException | NullPointerException e) {
            throw new ValidationException("Work, span and the processor limit must be numbers.");
        }
        if (w < 1 || s < 1) throw new ValidationException("Work and span must be at least 1.");
        if (s > w) throw new ValidationException("The span cannot exceed the work (S ≤ W): the critical path is part of the total work.");
        if (maxP < 1 || maxP > 65536) throw new ValidationException("The maximum number of processors must be between 1 and 65536.");
        return () -> execute(w, s, maxP);
    }

    private Execution execute(double w, double s, int maxP) {
        List<BrentsTheorem.Row> rows = new ArrayList<>();
        for (long p = 1; p <= maxP; p *= 2) rows.add(BrentsTheorem.analyze(w, s, (int) p));
        if (rows.get(rows.size() - 1).processors() != maxP) rows.add(BrentsTheorem.analyze(w, s, maxP));
        double parallelism = BrentsTheorem.parallelism(w, s);
        BrentsTheorem.Row last = rows.get(rows.size() - 1);
        String result = String.format("Parallelism W/S = %.1f → speed-up can never exceed %.1f.%nWith P = %d processors: T_P ≤ %.1f (Brent), no schedule beats %.1f; guaranteed speed-up %.1f× (efficiency %.0f%%).",
                parallelism, parallelism, last.processors(), last.brentUpper(), last.lower(), last.guaranteedSpeedup(), last.efficiency() * 100);
        String complexity = "T_P = O(W/P + S)   |   T₁ = W, T∞ = S   |   greedy-scheduler bound (W − S)/P + S";
        String info = String.format("W = %.0f, S = %.0f. Up to about P ≈ W/S = %.0f processors help; beyond that the span S dominates the time.", w, s, parallelism);
        return new Execution(rows.size(), f -> draw(rows, w, s, f), f -> narrate(rows, w, s, f), result, complexity, -1, rows.size(), info);
    }

    private void draw(List<BrentsTheorem.Row> rows, double w, double s, int frame) {
        XYChart.Series<String, Number> guaranteed = new XYChart.Series<>();
        guaranteed.setName("Guaranteed W / T_P (Brent)");
        XYChart.Series<String, Number> ideal = new XYChart.Series<>();
        ideal.setName("Ideal linear speed-up P");
        XYChart.Series<String, Number> cap = new XYChart.Series<>();
        cap.setName("Upper limit min(P, W/S)");
        List<String[]> table = new ArrayList<>();
        Map<Integer, CellState> states = new HashMap<>();
        for (int i = 0; i <= frame; i++) {
            BrentsTheorem.Row r = rows.get(i);
            String label = String.valueOf(r.processors());
            guaranteed.getData().add(new XYChart.Data<>(label, r.guaranteedSpeedup()));
            ideal.getData().add(new XYChart.Data<>(label, r.processors()));
            cap.getData().add(new XYChart.Data<>(label, Math.min(r.processors(), w / s)));
            table.add(new String[]{label, String.format("%.1f", r.lower()), String.format("%.1f", r.brentUpper()), String.format("%.1f", r.looseUpper()),
                    String.format("%.2f×", r.guaranteedSpeedup()), String.format("%.0f%%", r.efficiency() * 100)});
            states.put(i, i == frame ? CellState.ACTIVE : CellState.NORMAL);
        }
        chart.getData().setAll(List.of(ideal, cap, guaranteed));
        this.table.show(table, states);
    }

    private static String narrate(List<BrentsTheorem.Row> rows, double w, double s, int frame) {
        BrentsTheorem.Row r = rows.get(frame);
        return String.format("P = %d: work term W/P = %.1f, span term S = %.0f → T_P ≤ %.1f. Speed-up ≥ %.2f× (efficiency %.0f%%). %s",
                r.processors(), w / r.processors(), s, r.brentUpper(), r.guaranteedSpeedup(), r.efficiency() * 100,
                r.processors() > w / s ? "P already exceeds the parallelism W/S – extra processors are mostly idle." : "The work term still dominates, so more processors keep helping.");
    }
}
