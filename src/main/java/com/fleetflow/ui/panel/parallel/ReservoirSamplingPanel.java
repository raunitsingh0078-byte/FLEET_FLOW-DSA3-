package com.fleetflow.ui.panel.parallel;

import com.fleetflow.algorithm.randomized.ReservoirSampling;
import com.fleetflow.ui.AlgorithmPanel;
import com.fleetflow.ui.Execution;
import com.fleetflow.ui.Ui;
import com.fleetflow.util.ValidationException;
import com.fleetflow.visualization.ArrayVisualizer;
import com.fleetflow.visualization.ArrayVisualizer.Cell;
import com.fleetflow.visualization.CellState;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

/** Reservoir-sampling page: keeps a fair sample of a live vehicle-event stream using O(k) memory. */
public final class ReservoirSamplingPanel extends AlgorithmPanel {

    private static final String[] EVENT_TYPES = {"speed alert", "low fuel", "door opened", "harsh braking", "GPS signal lost",
            "engine temperature high", "idle > 15 min", "route deviation"};

    private final TextField lengthField = new TextField("24");
    private final TextField sizeField = new TextField("5");
    private final TextField seedField = new TextField("7");
    private final ArrayVisualizer view = new ArrayVisualizer();

    public ReservoirSamplingPanel() {
        lengthField.setPrefColumnCount(6);
        sizeField.setPrefColumnCount(4);
        seedField.setPrefColumnCount(6);
        view.setPrefViewportHeight(160);
        assemble("Reservoir Sampling – Vehicle Event Stream",
                "Real problem: vehicles emit an endless stream of events; we want a fair sample of k events without storing the stream or knowing its length. "
                        + "The first k events fill the reservoir; event number i > k replaces a random slot with probability k/i. "
                        + "Afterwards every event has exactly the same chance k/N of being in the sample.",
                new HBox(12, Ui.label("Stream length N", "kv-key"), lengthField, Ui.label("Reservoir size k", "kv-key"), sizeField, Ui.label("Seed", "kv-key"), seedField),
                view);
        loadDemo();
    }

    @Override
    protected void loadDemo() {
        lengthField.setText("24");
        sizeField.setText("5");
        seedField.setText("7");
    }

    private static String eventText(long seed, int i) {
        Random r = new Random(seed * 1_000_003L + i);
        return String.format("VEH-%02d %s", 1 + r.nextInt(10), EVENT_TYPES[r.nextInt(EVENT_TYPES.length)]);
    }

    @Override
    protected Supplier<Execution> prepare() throws ValidationException {
        int n;
        int k;
        long seed;
        try {
            n = Integer.parseInt(lengthField.getText().trim());
            k = Integer.parseInt(sizeField.getText().trim());
            seed = Long.parseLong(seedField.getText().trim());
        } catch (NumberFormatException | NullPointerException e) {
            throw new ValidationException("Stream length, reservoir size and seed must be whole numbers.");
        }
        if (n < 1 || n > 200) throw new ValidationException("The stream length must be between 1 and 200.");
        if (k < 1 || k > 12) throw new ValidationException("The reservoir size must be between 1 and 12.");
        if (k > n) throw new ValidationException("The reservoir cannot be larger than the stream (k ≤ N).");
        return () -> execute(n, k, seed);
    }

    private Execution execute(int n, int k, long seed) {
        ReservoirSampling.Result r = ReservoirSampling.sample(n, k, new Random(seed));
        double[] freq = ReservoirSampling.inclusionFrequency(n, k, 5000, seed + 1);
        double min = 1;
        double max = 0;
        for (double f : freq) {
            min = Math.min(min, f);
            max = Math.max(max, f);
        }
        StringBuilder sb = new StringBuilder("Final sample of " + k + " events (out of " + n + "):");
        for (int item : r.reservoir()) sb.append("\n  #").append(item).append("  ").append(eventText(seed, item));
        String complexity = "Time O(N) (one pass, O(1) per event)   |   Space O(k) – independent of the stream length";
        String info = String.format("Fairness check over 5000 independent runs: every event was sampled with frequency between %.3f and %.3f; theory says exactly k/N = %.3f.",
                min, max, (double) k / n);
        return new Execution(r.steps().size(), f -> draw(k, r, f), f -> narrate(seed, k, r, f), sb.toString(), complexity, -1, r.steps().size(), info);
    }

    private void draw(int k, ReservoirSampling.Result r, int frame) {
        ReservoirSampling.Step st = r.steps().get(frame);
        Cell[] slot = new Cell[k];
        Cell[] items = new Cell[k];
        Cell[] incoming = new Cell[Math.max(k, 1)];
        for (int i = 0; i < k; i++) {
            slot[i] = ArrayVisualizer.cell(String.valueOf(i), CellState.MUTED);
            int item = st.reservoir()[i];
            items[i] = ArrayVisualizer.cell(item == 0 ? "–" : String.valueOf(item), i == st.slot() ? CellState.ACTIVE : item == 0 ? CellState.MUTED : CellState.DONE);
        }
        incoming[0] = ArrayVisualizer.cell(String.valueOf(st.item()), st.slot() >= 0 ? CellState.MATCH : CellState.MISMATCH);
        view.render(List.of("Reservoir slot", "Event # in slot", "Incoming event #"), Arrays.asList(slot, items, incoming), -1);
    }

    private static String narrate(long seed, int k, ReservoirSampling.Result r, int frame) {
        ReservoirSampling.Step st = r.steps().get(frame);
        String head = "Event #" + st.item() + " (" + eventText(seed, st.item()) + "): ";
        if (st.item() <= k) return head + "the reservoir is not full yet, so it is stored in slot " + st.slot() + ".";
        return st.slot() >= 0 ? head + String.format("keep-probability k/i = %d/%d = %.3f; the random draw says KEEP → it replaces event #%d in slot %d.", k, st.item(), st.probability(), st.replaced(), st.slot())
                : head + String.format("keep-probability k/i = %d/%d = %.3f; the random draw says SKIP → the reservoir is unchanged.", k, st.item(), st.probability());
    }
}
