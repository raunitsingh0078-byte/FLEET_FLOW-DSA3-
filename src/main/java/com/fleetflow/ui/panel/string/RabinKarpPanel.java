package com.fleetflow.ui.panel.string;

import com.fleetflow.algorithm.string.RabinKarp;
import com.fleetflow.ui.AlgorithmPanel;
import com.fleetflow.ui.Execution;
import com.fleetflow.ui.Ui;
import com.fleetflow.util.ValidationException;
import com.fleetflow.visualization.CellState;
import com.fleetflow.visualization.StringMatchingVisualizer;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

import java.util.function.Supplier;

/** Rabin-Karp page: shows the rolling hash, hash comparisons, verification and spurious hits. */
public final class RabinKarpPanel extends AlgorithmPanel {

    private final TextField textField = new TextField();
    private final TextField patternField = new TextField();
    private final TextField modulusField = new TextField();
    private final StringMatchingVisualizer visualizer = new StringMatchingVisualizer();

    public RabinKarpPanel() {
        textField.setPromptText("Text containing package codes");
        patternField.setPromptText("Package code to find, e.g. PKG-ABC-1001");
        modulusField.setPromptText("Hash modulus (2 .. 1000000007)");
        modulusField.setPrefColumnCount(12);
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.add(Ui.label("Text", "kv-key"), 0, 0);
        grid.add(textField, 1, 0);
        grid.add(Ui.label("Pattern", "kv-key"), 0, 1);
        grid.add(patternField, 1, 1);
        grid.add(Ui.label("Hash modulus", "kv-key"), 0, 2);
        grid.add(modulusField, 1, 2);
        GridPane.setHgrow(textField, Priority.ALWAYS);
        GridPane.setHgrow(patternField, Priority.ALWAYS);
        assemble("Rabin-Karp",
                "Real problem: look up a package code among many codes quickly. A polynomial rolling hash of the current window is "
                        + "updated in O(1) as the window slides; characters are compared only when the hash equals the pattern hash. "
                        + "Use a small modulus (e.g. 7) to see spurious hash collisions being caught by verification.",
                grid, visualizer);
        loadDemo();
    }

    @Override
    protected void loadDemo() {
        textField.setText("PKG-ABC-1001 PKG-XYZ-2002 PKG-ABC-1001 PKG-ABD-1001");
        patternField.setText("PKG-ABC-1001");
        modulusField.setText("101");
    }

    @Override
    protected Supplier<Execution> prepare() throws ValidationException {
        String text = StringInputs.requireText(textField.getText(), "text");
        String pattern = StringInputs.requireText(patternField.getText(), "pattern");
        if (pattern.length() > text.length()) {
            throw new ValidationException("The pattern is longer than the text, so it cannot occur in it.");
        }
        int modulus = StringInputs.parseInt(modulusField.getText() == null ? "" : modulusField.getText(),
                "The hash modulus", 2, 1_000_000_007);
        return () -> execute(text, pattern, modulus);
    }

    private Execution execute(String text, String pattern, int modulus) {
        RabinKarp.Result r = RabinKarp.search(text, pattern, modulus);
        String result = (r.matches().isEmpty() ? "No occurrence found." : r.matches().size() + " occurrence(s) at index "
                + r.matches()) + "\nPattern hash = " + r.patternHash() + " (mod " + modulus + ")";
        String complexity = "Expected time O(n + m); worst case O(n·m) when many hashes collide   |   Space O(1)";
        String info = r.hashComparisons() + " hash comparisons, " + r.charComparisons()
                + " character comparisons (only after equal hashes), " + r.spuriousHits() + " spurious hit(s).";
        long comparisons = (long) r.hashComparisons() + r.charComparisons();
        return new Execution(r.steps().size(), f -> visualizer.show(view(text, pattern, r, f)),
                f -> narrate(text, pattern, r, f), result, complexity, comparisons, r.steps().size(), info);
    }

    private static StringMatchingVisualizer.View view(String text, String pattern, RabinKarp.Result r, int frame) {
        RabinKarp.Step s = r.steps().get(frame);
        int m = pattern.length();
        boolean[] found = new boolean[text.length()];
        for (int i = 0; i < frame; i++) {
            RabinKarp.Step earlier = r.steps().get(i);
            if (earlier.verified()) {
                for (int k = 0; k < m; k++) {
                    found[earlier.start() + k] = true;
                }
            }
        }
        CellState window = s.hashEqual() ? (s.verified() ? CellState.MATCH : CellState.MISMATCH) : CellState.WINDOW;
        return new StringMatchingVisualizer.View(text, pattern, s.start(), -1, -1, CellState.NORMAL, found, window,
                null, -1);
    }

    private static String narrate(String text, String pattern, RabinKarp.Result r, int frame) {
        RabinKarp.Step s = r.steps().get(frame);
        String window = text.substring(s.start(), s.start() + pattern.length());
        String head = "Window [" + s.start() + ", " + (s.start() + pattern.length()) + ") = \"" + window + "\", hash = "
                + s.windowHash() + " vs pattern hash " + r.patternHash() + " → ";
        String verdict;
        if (!s.hashEqual()) {
            verdict = "hashes differ, so the window is rejected without comparing any characters.";
        } else if (s.verified()) {
            verdict = "hashes equal; character check confirms a real MATCH at index " + s.start() + ".";
        } else {
            verdict = "hashes equal but the characters differ: a SPURIOUS HIT (hash collision) caught by verification.";
        }
        String roll = frame < r.steps().size() - 1
                ? " Next: roll the hash = (hash − leading char × BASE^(m−1)) × " + RabinKarp.BASE + " + incoming char."
                : "";
        return head + verdict + roll;
    }
}
