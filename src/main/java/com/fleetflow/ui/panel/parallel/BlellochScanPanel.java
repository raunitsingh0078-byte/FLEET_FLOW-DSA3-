package com.fleetflow.ui.panel.parallel;

import com.fleetflow.algorithm.parallel.BlellochScan;
import com.fleetflow.data.FleetRepository;
import com.fleetflow.ui.AlgorithmPanel;
import com.fleetflow.ui.Execution;
import com.fleetflow.ui.Ui;
import com.fleetflow.util.ValidationException;
import com.fleetflow.visualization.ArrayVisualizer;
import com.fleetflow.visualization.ArrayVisualizer.Cell;
import com.fleetflow.visualization.CellState;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

/** Blelloch scan page: parallel prefix sums (cumulative shipment statistics) with up-sweep and down-sweep. */
public final class BlellochScanPanel extends AlgorithmPanel {

    private static final int MAX_VALUES = 16;

    private final TextField valuesField = new TextField();
    private final ArrayVisualizer view = new ArrayVisualizer();

    public BlellochScanPanel() {
        valuesField.setPromptText("Daily shipment counts, separated by commas (up to " + MAX_VALUES + ")");
        HBox.setHgrow(valuesField, Priority.ALWAYS);
        view.setPrefViewportHeight(170);
        assemble("Blelloch Scan – Cumulative Shipment Statistics",
                "Real problem: cumulative totals (running number of shipments per day) for a huge data set. Blelloch's scan treats the array as a binary tree: the UP-SWEEP builds "
                        + "partial sums (right child += left child), the root is cleared, and the DOWN-SWEEP hands prefixes back down. Every round's additions are independent, so "
                        + "n elements need only 2·log₂ n parallel rounds and 2(n−1) additions (work-efficient).",
                new HBox(10, Ui.label("Values", "kv-key"), valuesField), view);
        loadDemo();
    }

    @Override
    protected void loadDemo() {
        LocalDate today = LocalDate.now();
        StringBuilder sb = new StringBuilder();
        for (int i = 7; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            long count = FleetRepository.get().shipments().stream().filter(s -> s.date().equals(day)).count();
            sb.append(sb.length() == 0 ? "" : ", ").append(count);
        }
        valuesField.setText(sb.toString());
    }

    @Override
    protected Supplier<Execution> prepare() throws ValidationException {
        String raw = valuesField.getText() == null ? "" : valuesField.getText().trim();
        if (raw.isEmpty()) throw new ValidationException("Please enter at least one value.");
        String[] parts = raw.split("[,\\s]+");
        if (parts.length > MAX_VALUES) throw new ValidationException("Please enter at most " + MAX_VALUES + " values.");
        long[] values = new long[parts.length];
        try {
            for (int i = 0; i < parts.length; i++) {
                values[i] = Long.parseLong(parts[i]);
                if (Math.abs(values[i]) > 1_000_000) throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            throw new ValidationException("Values must be whole numbers with absolute value up to 1,000,000.");
        }
        return () -> execute(values);
    }

    private Execution execute(long[] values) {
        BlellochScan.Result r = BlellochScan.exclusiveScan(values);
        long[] seq = new long[values.length];
        long run = 0;
        for (int i = 0; i < values.length; i++) {
            seq[i] = run;
            run += values[i];
        }
        boolean ok = Arrays.equals(seq, r.exclusive());
        String result = "Exclusive scan:  " + Arrays.toString(r.exclusive()) + "\nInclusive (cumulative): " + Arrays.toString(r.inclusive())
                + "\nVerified against the sequential prefix sums: " + (ok ? "identical ✓" : "MISMATCH ✗");
        String complexity = "Work O(n) = " + r.additions() + " additions   |   Span O(log n) = " + r.rounds() + " parallel rounds   |   Sequential scan: n−1 = " + (values.length - 1) + " dependent additions";
        String info = "Padded to " + r.paddedLength() + " elements (next power of two). Additions inside one round touch disjoint pairs, so they can run simultaneously.";
        return new Execution(r.steps().size(), f -> draw(r, f), f -> narrate(r, f), result, complexity, r.additions(), r.steps().size(), info);
    }

    private void draw(BlellochScan.Result r, int frame) {
        BlellochScan.Step st = r.steps().get(frame);
        int n = st.snapshot().length;
        boolean last = frame == r.steps().size() - 1;
        Cell[] idx = new Cell[n];
        Cell[] val = new Cell[n];
        Cell[] mark = new Cell[n];
        for (int i = 0; i < n; i++) {
            idx[i] = ArrayVisualizer.cell(String.valueOf(i), CellState.MUTED);
            CellState s = last ? CellState.MATCH : CellState.NORMAL;
            if (!last && st.phase() == BlellochScan.Phase.ZERO_ROOT && i == n - 1) s = CellState.WINDOW;
            for (int l : st.leftIdx()) if (l == i) {
                s = CellState.HIGHLIGHT;
                mark[i] = ArrayVisualizer.cell("L", CellState.MUTED);
            }
            for (int rr : st.rightIdx()) if (rr == i && st.phase() != BlellochScan.Phase.ZERO_ROOT) {
                s = CellState.ACTIVE;
                mark[i] = ArrayVisualizer.cell("R", CellState.MUTED);
            }
            val[i] = ArrayVisualizer.cell(String.valueOf(st.snapshot()[i]), s);
        }
        view.render(List.of("Index", "Array", "Pair (L / R)"), Arrays.asList(idx, val, mark), -1);
    }

    private static String narrate(BlellochScan.Result r, int frame) {
        BlellochScan.Step st = r.steps().get(frame);
        StringBuilder pairs = new StringBuilder();
        for (int i = 0; i < st.leftIdx().length; i++) {
            pairs.append(i == 0 ? "" : ", ").append("(").append(st.leftIdx()[i]).append("→").append(st.rightIdx()[i]).append(")");
        }
        return switch (st.phase()) {
            case INPUT -> "Input padded with zeros to a power of two. The array is viewed as a balanced binary tree.";
            case UPSWEEP -> "Up-sweep, distance " + (1 << st.level()) + ": in parallel a[R] += a[L] for the pairs " + pairs + ". After the last level the root holds the total.";
            case ZERO_ROOT -> "The total is discarded: the last element (root) is set to 0 – the identity of addition – to start the down-sweep.";
            case DOWNSWEEP -> "Down-sweep, distance " + (1 << st.level()) + ": for the pairs " + pairs + " do t = a[L]; a[L] = a[R]; a[R] += t. The prefix sum flows down the tree."
                    + (frame == r.steps().size() - 1 ? " The array now holds the exclusive scan." : "");
        };
    }
}
