package com.fleetflow.ui.panel.parallel;

import com.fleetflow.algorithm.parallel.ParallelReduce;
import com.fleetflow.ui.AlgorithmPanel;
import com.fleetflow.ui.Execution;
import com.fleetflow.ui.Ui;
import com.fleetflow.util.ValidationException;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

/**
 * Parallel-reduce page: aggregates a large synthetic shipment-weight array sequentially and with 1, 2, 4… worker threads and
 * shows the <em>measured</em> times. Nothing is simulated: the numbers depend on this computer.
 */
public final class ParallelReducePanel extends AlgorithmPanel {

    private static final int WARMUP = 2;
    private static final int REPETITIONS = 5;

    private final TextField sizeField = new TextField("8000000");
    private final ComboBox<ParallelReduce.Op> opBox = new ComboBox<>();
    private final BarChart<String, Number> chart;

    public ParallelReducePanel() {
        opBox.getItems().addAll(ParallelReduce.Op.values());
        opBox.setValue(ParallelReduce.Op.SUM);
        sizeField.setPrefColumnCount(10);
        NumberAxis y = new NumberAxis();
        y.setLabel("median time (ms)");
        chart = new BarChart<>(new CategoryAxis(), y);
        chart.setAnimated(false);
        chart.setLegendVisible(false);
        chart.setPrefHeight(300);
        chart.setTitle("Measured reduction time (lower is better)");
        assemble("Parallel Reduce – Logistics Data Aggregation",
                "Real problem: aggregate millions of shipment records (total weight, heaviest parcel). The array is cut into chunks, every chunk is reduced by a worker thread of an "
                        + "ExecutorService, and the partial results are combined – valid because + and max are associative. This page times a sequential loop and the parallel version "
                        + "on THIS machine (median of " + REPETITIONS + " runs after " + WARMUP + " warm-up runs); speed-up depends on core count and memory bandwidth.",
                new HBox(12, Ui.label("Elements", "kv-key"), sizeField, Ui.label("Operator", "kv-key"), opBox), chart);
        loadDemo();
    }

    @Override
    protected void loadDemo() {
        sizeField.setText("8000000");
        opBox.setValue(ParallelReduce.Op.SUM);
    }

    @Override
    protected Supplier<Execution> prepare() throws ValidationException {
        int n;
        try {
            n = Integer.parseInt(sizeField.getText().trim().replace(",", ""));
        } catch (NumberFormatException | NullPointerException e) {
            throw new ValidationException("The number of elements must be a whole number.");
        }
        if (n < 1_000 || n > 40_000_000) {
            throw new ValidationException("Please use between 1,000 and 40,000,000 elements (larger arrays may exhaust memory).");
        }
        ParallelReduce.Op op = opBox.getValue() == null ? ParallelReduce.Op.SUM : opBox.getValue();
        return () -> execute(n, op);
    }

    private static long medianNanos(Runnable task) {
        for (int i = 0; i < WARMUP; i++) task.run();
        long[] t = new long[REPETITIONS];
        for (int i = 0; i < REPETITIONS; i++) {
            long s = System.nanoTime();
            task.run();
            t[i] = System.nanoTime() - s;
        }
        Arrays.sort(t);
        return t[REPETITIONS / 2];
    }

    private Execution execute(int n, ParallelReduce.Op op) {
        int[] data = new int[n];
        Random rnd = new Random(2024);
        for (int i = 0; i < n; i++) data[i] = 1 + rnd.nextInt(300); // synthetic shipment weights in kg
        int cores = Runtime.getRuntime().availableProcessors();
        TreeSet<Integer> threadCounts = new TreeSet<>(List.of(1, 2, 4));
        threadCounts.add(Math.min(cores, 16));
        long expected = ParallelReduce.sequential(data, op);

        List<String> labels = new ArrayList<>();
        List<Double> millis = new ArrayList<>();
        labels.add("sequential");
        millis.add(medianNanos(() -> ParallelReduce.sequential(data, op)) / 1e6);
        for (int t : threadCounts) {
            ExecutorService pool = Executors.newFixedThreadPool(t);
            try {
                long got = ParallelReduce.parallel(data, op, t, pool);
                if (got != expected) {
                    throw new IllegalStateException("parallel result " + got + " differs from sequential " + expected);
                }
                labels.add(t + (t == 1 ? " thread" : " threads"));
                millis.add(medianNanos(() -> ParallelReduce.parallel(data, op, t, pool)) / 1e6);
            } finally {
                pool.shutdown();
            }
        }
        double seq = millis.get(0);
        double best = millis.stream().skip(1).mapToDouble(Double::doubleValue).min().orElse(seq);
        String result = op + " of " + String.format("%,d", n) + " weights = " + String.format("%,d", expected) + " (identical in every configuration ✓)\n"
                + String.format("Sequential %.3f ms; best parallel %.3f ms → speed-up %.2f×", seq, best, seq / best);
        String complexity = "Work O(n)   |   Span O(n/P + log P) with P workers   |   Space O(P) partial results";
        String info = "This machine reports " + cores + " logical processor(s). Very small arrays are dominated by thread hand-off cost, so parallel can be slower than sequential; "
                + "memory bandwidth also limits the speed-up of simple reductions. All times are measured, none are estimated.";
        return new Execution(labels.size(), f -> draw(labels, millis, f), f -> narrate(labels, millis, f), result, complexity, -1, labels.size(), info);
    }

    private void draw(List<String> labels, List<Double> millis, int frame) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (int i = 0; i <= frame; i++) {
            series.getData().add(new XYChart.Data<>(labels.get(i), millis.get(i)));
        }
        chart.getData().setAll(List.of(series));
    }

    private static String narrate(List<String> labels, List<Double> millis, int frame) {
        if (frame == 0) {
            return String.format("Baseline: a single-threaded loop needs %.3f ms (median).", millis.get(0));
        }
        double seq = millis.get(0);
        double t = millis.get(frame);
        return String.format("%s: the array is split into equal chunks, each reduced by its own worker; partial results are combined. Median %.3f ms → speed-up %.2f× over sequential.",
                labels.get(frame), t, seq / t);
    }
}
