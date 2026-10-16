package com.fleetflow.ui.panel.parallel;

import com.fleetflow.algorithm.randomized.RandomizedQuickSort;
import com.fleetflow.data.FleetRepository;
import com.fleetflow.model.Shipment;
import com.fleetflow.ui.AlgorithmPanel;
import com.fleetflow.ui.Execution;
import com.fleetflow.ui.Ui;
import com.fleetflow.util.ValidationException;
import com.fleetflow.visualization.ArrayVisualizer;
import com.fleetflow.visualization.ArrayVisualizer.Cell;
import com.fleetflow.visualization.CellState;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/** Randomized QuickSort page: ranks shipments by weight while showing pivot choice and partitioning. */
public final class RandomizedQuickSortPanel extends AlgorithmPanel {

    private static final int MAX_VALUES = 24;

    private final TextField valuesField = new TextField();
    private final TextField seedField = new TextField("7");
    private final CheckBox randomPivot = new CheckBox("Random pivot (unchecked: always the last element)");
    private final ArrayVisualizer view = new ArrayVisualizer();

    public RandomizedQuickSortPanel() {
        valuesField.setPromptText("Shipment weights in kg, separated by commas");
        Button fromData = Ui.button("Use shipment weights", "button-secondary", "Take the weights of the first 12 shipments");
        fromData.setOnAction(e -> valuesField.setText(weights()));
        randomPivot.setSelected(true);
        seedField.setPrefColumnCount(6);
        HBox.setHgrow(valuesField, Priority.ALWAYS);
        view.setPrefViewportHeight(160);
        assemble("Randomized QuickSort – Shipment Ranking",
                "Real problem: rank shipments by weight. QuickSort partitions the list around a pivot; choosing the pivot uniformly at random makes the expected number of "
                        + "comparisons O(n log n) for every input, whereas a fixed pivot (last element) degrades to O(n²) on already-sorted data. "
                        + "The seed makes a run reproducible.",
                new VBox(8, new HBox(8, Ui.label("Values", "kv-key"), valuesField, fromData),
                        new HBox(16, randomPivot, Ui.label("Seed", "kv-key"), seedField)),
                view);
        loadDemo();
    }

    private static String weights() {
        List<Shipment> list = FleetRepository.get().shipments();
        StringBuilder sb = new StringBuilder();
        for (Shipment s : list.subList(0, Math.min(12, list.size()))) {
            sb.append(sb.length() == 0 ? "" : ", ").append(Math.round(s.weightKg()));
        }
        return sb.toString();
    }

    @Override
    protected void loadDemo() {
        valuesField.setText(weights());
        seedField.setText("7");
        randomPivot.setSelected(true);
    }

    @Override
    protected Supplier<Execution> prepare() throws ValidationException {
        String raw = valuesField.getText() == null ? "" : valuesField.getText().trim();
        if (raw.isEmpty()) {
            throw new ValidationException("Please enter the values to sort, separated by commas.");
        }
        String[] parts = raw.split("[,\\s]+");
        if (parts.length > MAX_VALUES) {
            throw new ValidationException("Please enter at most " + MAX_VALUES + " values so the animation stays readable.");
        }
        int[] values = new int[parts.length];
        try {
            for (int i = 0; i < parts.length; i++) {
                values[i] = Integer.parseInt(parts[i]);
                if (Math.abs(values[i]) > 99999) throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            throw new ValidationException("Values must be whole numbers between -99999 and 99999.");
        }
        long seed;
        try {
            seed = Long.parseLong(seedField.getText().trim());
        } catch (NumberFormatException | NullPointerException e) {
            throw new ValidationException("The seed must be a whole number.");
        }
        boolean random = randomPivot.isSelected();
        return () -> execute(values, random, seed);
    }

    private Execution execute(int[] values, boolean random, long seed) {
        RandomizedQuickSort.Result r = RandomizedQuickSort.sort(values, random, seed, true);
        RandomizedQuickSort.Result other = RandomizedQuickSort.sort(values, !random, seed, false);
        int[] desc = new int[r.sorted().length];
        for (int i = 0; i < desc.length; i++) desc[i] = r.sorted()[desc.length - 1 - i];
        String result = "Sorted ascending: " + Arrays.toString(r.sorted()) + "\nRanking (heaviest first): " + Arrays.toString(desc);
        String complexity = "Expected time O(n log n) with a random pivot; worst case O(n²) (probability vanishingly small)   |   Space O(log n) recursion";
        String info = (random ? "Random pivot" : "Last-element pivot") + ": " + r.comparisons() + " comparisons, " + r.swaps() + " swaps, max recursion depth " + r.maxDepth()
                + ". The " + (random ? "last-element" : "random") + " strategy on the same input needed " + other.comparisons() + " comparisons.";
        int frames = r.steps().size() + 2;
        return new Execution(frames, f -> draw(values, r, f), f -> narrate(values, r, random, f), result, complexity, r.comparisons(), r.steps().size(), info);
    }

    private void draw(int[] values, RandomizedQuickSort.Result r, int frame) {
        int steps = r.steps().size();
        int n = values.length;
        int[] arr = frame == 0 ? values : frame > steps ? r.sorted() : r.steps().get(frame - 1).snapshot();
        Set<Integer> fixed = new HashSet<>();
        for (int s = 0; s < Math.min(frame, steps); s++) {
            RandomizedQuickSort.Step st = r.steps().get(s);
            if (st.kind() == RandomizedQuickSort.Kind.PLACE) fixed.add(st.i());
        }
        if (frame > steps) for (int i = 0; i < n; i++) fixed.add(i);
        Cell[] idx = new Cell[n];
        Cell[] val = new Cell[n];
        Cell[] ptr = new Cell[n];
        RandomizedQuickSort.Step st = frame >= 1 && frame <= steps ? r.steps().get(frame - 1) : null;
        for (int k = 0; k < n; k++) {
            idx[k] = ArrayVisualizer.cell(String.valueOf(k), CellState.MUTED);
            CellState state = CellState.NORMAL;
            String mark = "";
            if (st != null && !fixed.contains(k)) {
                if (k >= st.lo() && k <= st.hi()) state = CellState.WINDOW;
                if (st.kind() != RandomizedQuickSort.Kind.PIVOT && k >= st.lo() && k <= st.i() && st.kind() != RandomizedQuickSort.Kind.PLACE) state = CellState.DONE;
                if (k == st.hi() && st.kind() != RandomizedQuickSort.Kind.PLACE) {
                    state = CellState.ACTIVE;
                    mark = "P";
                }
                if (st.kind() == RandomizedQuickSort.Kind.COMPARE && k == st.j()) {
                    state = CellState.HIGHLIGHT;
                    mark = "j";
                }
                if (st.kind() == RandomizedQuickSort.Kind.SWAP && (k == st.i() || k == st.j())) state = CellState.MISMATCH;
            }
            if (fixed.contains(k)) state = CellState.MATCH;
            val[k] = ArrayVisualizer.cell(String.valueOf(arr[k]), state);
            ptr[k] = mark.isEmpty() ? null : ArrayVisualizer.cell(mark, CellState.MUTED);
        }
        view.render(List.of("Index", "Weight (kg)", "P=pivot, j=scan"), Arrays.asList(idx, val, ptr), st == null ? -1 : Math.max(st.j(), st.lo()));
    }

    private static String narrate(int[] values, RandomizedQuickSort.Result r, boolean random, int frame) {
        int steps = r.steps().size();
        if (frame == 0) return "Unsorted input. " + (random ? "Each partition will pick its pivot uniformly at random." : "Each partition will use the last element as pivot.");
        if (frame > steps) return "Sorted! Green cells were each fixed as a pivot (or a single element) and never move again. Total comparisons: " + r.comparisons() + ".";
        RandomizedQuickSort.Step st = r.steps().get(frame - 1);
        int pivot = st.snapshot()[st.hi()];
        return switch (st.kind()) {
            case PIVOT -> "Partition range [" + st.lo() + ".." + st.hi() + "]: " + (random ? "a random element was chosen and swapped to the end" : "the last element is the pivot")
                    + ". Pivot = " + pivot + " (marked P).";
            case COMPARE -> "Compare a[" + st.j() + "] = " + st.snapshot()[st.j()] + " with the pivot " + pivot + " → "
                    + (st.snapshot()[st.j()] <= pivot ? "≤ pivot, it joins the left region." : "> pivot, it stays on the right.") + " Comparisons so far: " + st.comparisons() + ".";
            case SWAP -> "Swap a[" + st.i() + "] and a[" + st.j() + "] to grow the region of elements ≤ pivot (green).";
            case PLACE -> "Put the pivot into its final position " + st.i() + ": everything to its left is ≤ and everything to its right is >. It is now fixed (green).";
        };
    }
}
