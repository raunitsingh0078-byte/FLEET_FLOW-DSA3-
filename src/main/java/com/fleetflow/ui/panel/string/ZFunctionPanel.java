package com.fleetflow.ui.panel.string;

import com.fleetflow.algorithm.string.ZFunction;
import com.fleetflow.ui.AlgorithmPanel;
import com.fleetflow.ui.Execution;
import com.fleetflow.ui.Ui;
import com.fleetflow.util.ValidationException;
import com.fleetflow.visualization.ArrayVisualizer;
import com.fleetflow.visualization.ArrayVisualizer.Cell;
import com.fleetflow.visualization.CellState;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

/** Z-function page: detects repeated shipment patterns, or searches a pattern via pattern + '$' + text. */
public final class ZFunctionPanel extends AlgorithmPanel {

    private final TextField textField = new TextField();
    private final TextField patternField = new TextField();
    private final ArrayVisualizer visualizer = new ArrayVisualizer();

    public ZFunctionPanel() {
        textField.setPromptText("String to analyse");
        patternField.setPromptText("Optional pattern: leave empty to detect repetition");
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.add(Ui.label("String", "kv-key"), 0, 0);
        grid.add(textField, 1, 0);
        grid.add(Ui.label("Pattern (optional)", "kv-key"), 0, 1);
        grid.add(patternField, 1, 1);
        GridPane.setHgrow(textField, Priority.ALWAYS);
        GridPane.setHgrow(patternField, Priority.ALWAYS);
        assemble("Z-Function",
                "Real problem: detect repeated shipment patterns (e.g. a manifest that repeats the same route code). "
                        + "z[i] is the length of the longest substring starting at i that equals a prefix of the string. "
                        + "With a pattern given, the Z array of pattern + '$' + text finds every occurrence (z[i] = pattern length).",
                grid, visualizer);
        loadDemo();
    }

    @Override
    protected void loadDemo() {
        textField.setText("SHP01SHP01SHP01");
        patternField.setText("");
    }

    @Override
    protected Supplier<Execution> prepare() throws ValidationException {
        String text = StringInputs.requireText(textField.getText(), "string");
        String pattern = patternField.getText() == null ? "" : patternField.getText();
        if (!pattern.isEmpty()) {
            StringInputs.requireText(pattern, "pattern");
            if (pattern.length() > text.length()) {
                throw new ValidationException("The pattern is longer than the string, so it cannot occur in it.");
            }
        }
        return () -> execute(text, pattern);
    }

    private Execution execute(String text, String pattern) {
        boolean searching = !pattern.isEmpty();
        String combined = searching ? pattern + ZFunction.SEPARATOR + text : text;
        ZFunction.Result r = ZFunction.analyze(combined);
        int[] z = r.z();
        int n = combined.length();

        String result;
        if (searching) {
            List<Integer> matches = ZFunction.search(text, pattern);
            result = (matches.isEmpty() ? "Pattern not found." : "Pattern found at index " + matches)
                    + "\nZ = " + Arrays.toString(z);
        } else {
            ZFunction.Repetition rep = ZFunction.repetition(z);
            String repeatText;
            if (rep.period() == n) {
                repeatText = "No repetition: the string has no border, so its smallest period is the full length " + n + ".";
            } else if (rep.exactRepeat()) {
                repeatText = "Smallest period p = " + rep.period() + " → the string is \"" + text.substring(0, rep.period())
                        + "\" repeated " + rep.repeats() + " times.";
            } else {
                repeatText = "Smallest period p = " + rep.period() + " (the last repetition is only partial).";
            }
            result = repeatText + "\nZ = " + Arrays.toString(z);
        }
        String complexity = "Time O(n) = O(" + n + ")   |   Space O(n) for the Z array";
        String info = "Every position is visited once; the Z-box [l, r) lets many characters be skipped ("
                + r.comparisons() + " character comparisons for " + (n - 1) + " positions).";
        int frames = r.steps().size() + 1;
        return new Execution(frames, f -> draw(combined, r, f), f -> narrate(combined, r, f), result, complexity,
                r.comparisons(), r.steps().size(), info);
    }

    private void draw(String s, ZFunction.Result r, int frame) {
        int n = s.length();
        ZFunction.Step step = frame == 0 ? null : r.steps().get(frame - 1);
        Cell[] index = new Cell[n];
        Cell[] chars = new Cell[n];
        Cell[] zRow = new Cell[n];
        for (int i = 0; i < n; i++) {
            index[i] = ArrayVisualizer.cell(String.valueOf(i), CellState.MUTED);
            CellState charState = CellState.NORMAL;
            if (step != null) {
                if (i >= step.newWindowL() && i < step.newWindowR()) charState = CellState.WINDOW;
                if (i < step.value() || (i >= step.index() && i < step.index() + step.value())) charState = CellState.MATCH;
                if (i == step.index()) charState = CellState.ACTIVE;
            }
            char c = s.charAt(i);
            chars[i] = ArrayVisualizer.cell(c == ZFunction.SEPARATOR ? "$" : String.valueOf(c), charState);
            if (i <= frame) {
                zRow[i] = ArrayVisualizer.cell(String.valueOf(r.z()[i]), i == frame ? CellState.ACTIVE : CellState.DONE);
            } else {
                zRow[i] = ArrayVisualizer.cell("?", CellState.MUTED);
            }
        }
        visualizer.render(new ArrayList<>(List.of("Index", "String", "Z value")), new ArrayList<>(List.of(index, chars, zRow)), frame);
    }

    private static String narrate(String s, ZFunction.Result r, int frame) {
        if (frame == 0) {
            return "z[0] is defined as n = " + s.length() + ": the whole string trivially matches its own prefix.";
        }
        ZFunction.Step st = r.steps().get(frame - 1);
        String start = st.reused()
                ? "Index " + st.index() + " lies inside the Z-box [" + st.windowL() + ", " + st.windowR() + "), so z[" + (st.index() - st.windowL())
                + "] = " + r.z()[st.index() - st.windowL()] + " gives a head start without re-comparing. "
                : "Index " + st.index() + " is outside the Z-box, so compare characters from scratch. ";
        String end = "Result z[" + st.index() + "] = " + st.value() + ".";
        String box = st.newWindowR() != st.windowR() ? " The Z-box grows to [" + st.newWindowL() + ", " + st.newWindowR() + ")." : "";
        return start + end + box;
    }
}
