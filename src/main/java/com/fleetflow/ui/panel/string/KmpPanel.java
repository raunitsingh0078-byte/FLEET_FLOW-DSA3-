package com.fleetflow.ui.panel.string;

import com.fleetflow.algorithm.string.KmpMatcher;
import com.fleetflow.data.FleetRepository;
import com.fleetflow.ui.AlgorithmPanel;
import com.fleetflow.ui.Execution;
import com.fleetflow.ui.Ui;
import com.fleetflow.util.ValidationException;
import com.fleetflow.visualization.CellState;
import com.fleetflow.visualization.StringMatchingVisualizer;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

import java.util.Arrays;
import java.util.function.Supplier;

/** KMP page: searches shipment IDs and delivery keywords and animates comparisons, LPS table and fallbacks. */
public final class KmpPanel extends AlgorithmPanel {

    private final TextField textField = new TextField();
    private final TextField patternField = new TextField();
    private final StringMatchingVisualizer visualizer = new StringMatchingVisualizer();

    public KmpPanel() {
        textField.setPromptText("Text to search, e.g. a list of shipment IDs or a delivery description");
        patternField.setPromptText("Pattern, e.g. SHP-2041");
        Button useShipments = Ui.button("Use shipment IDs from data", "button-secondary",
                "Fill the text with the first 8 shipment IDs of the current data set");
        useShipments.setOnAction(e -> textField.setText(FleetRepository.get().shipmentIdManifest(8)));

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.add(Ui.label("Text", "kv-key"), 0, 0);
        grid.add(textField, 1, 0);
        grid.add(useShipments, 2, 0);
        grid.add(Ui.label("Pattern", "kv-key"), 0, 1);
        grid.add(patternField, 1, 1);
        GridPane.setHgrow(textField, Priority.ALWAYS);
        assemble("KMP – Knuth-Morris-Pratt",
                "Real problem: find a shipment ID or delivery keyword inside thousands of records without re-reading text. "
                        + "KMP precomputes the LPS table so that after a mismatch the pattern pointer falls back inside the pattern "
                        + "and the text pointer never moves backwards.",
                grid, visualizer);
        loadDemo();
    }

    @Override
    protected void loadDemo() {
        textField.setText("SHP-2041 SHP-2042 SHP-2041 DEL-SHP-2043 SHP-2041X");
        patternField.setText("SHP-2041");
    }

    @Override
    protected Supplier<Execution> prepare() throws ValidationException {
        String text = StringInputs.requireText(textField.getText(), "text");
        String pattern = StringInputs.requireText(patternField.getText(), "pattern");
        if (pattern.length() > text.length()) {
            throw new ValidationException("The pattern is longer than the text, so it cannot occur in it.");
        }
        return () -> execute(text, pattern);
    }

    private Execution execute(String text, String pattern) {
        KmpMatcher.Result r = KmpMatcher.search(text, pattern);
        int n = text.length();
        int m = pattern.length();
        String result = (r.matches().isEmpty() ? "No occurrence found."
                : r.matches().size() + " occurrence(s) at index " + r.matches())
                + "\nLPS = " + Arrays.toString(r.lps());
        String complexity = "Time O(n + m) = O(" + n + " + " + m + ")   |   Space O(m) for the LPS table";
        String info = "Naive matching may need up to n·m = " + (long) n * m + " comparisons; KMP used " + r.comparisons()
                + " (never more than 2n = " + 2L * n + ").";
        return new Execution(r.steps().size(), f -> visualizer.show(view(text, pattern, r, f)),
                f -> narrate(text, pattern, r, f), result, complexity, r.comparisons(), r.steps().size(), info);
    }

    private static StringMatchingVisualizer.View view(String text, String pattern, KmpMatcher.Result r, int frame) {
        int m = pattern.length();
        KmpMatcher.Step s = r.steps().get(frame);
        boolean[] found = new boolean[text.length()];
        for (int i = 0; i <= frame; i++) {
            KmpMatcher.Step earlier = r.steps().get(i);
            if (earlier.kind() == KmpMatcher.Kind.FOUND) {
                for (int k = 0; k < m; k++) {
                    found[earlier.textIndex() - m + 1 + k] = true;
                }
            }
        }
        CellState compare = s.kind() == KmpMatcher.Kind.MISMATCH ? CellState.MISMATCH : CellState.MATCH;
        int lpsHighlight = switch (s.kind()) {
            case MISMATCH -> s.patternIndex() > 0 ? s.patternIndex() - 1 : -1;
            case FOUND -> m - 1;
            case MATCH -> -1;
        };
        return new StringMatchingVisualizer.View(text, pattern, s.alignment(), s.textIndex(), s.patternIndex(),
                compare, found, null, r.lps(), lpsHighlight);
    }

    private static String narrate(String text, String pattern, KmpMatcher.Result r, int frame) {
        KmpMatcher.Step s = r.steps().get(frame);
        char t = text.charAt(s.textIndex());
        char p = pattern.charAt(s.patternIndex());
        String head = "Comparison #" + s.comparisons() + ": text[" + s.textIndex() + "]='" + t + "' vs pattern["
                + s.patternIndex() + "]='" + p + "' → ";
        return switch (s.kind()) {
            case MATCH -> head + "match. The matched prefix length becomes " + s.nextPatternIndex() + ".";
            case FOUND -> head + "match, and the whole pattern is matched! Occurrence starts at index "
                    + (s.textIndex() - pattern.length() + 1) + ". Pattern pointer falls back to LPS[" + (pattern.length() - 1)
                    + "] = " + s.nextPatternIndex() + " so overlapping matches are not missed.";
            case MISMATCH -> s.patternIndex() == 0
                    ? head + "mismatch with nothing matched yet, so simply advance the text pointer."
                    : head + "mismatch. Instead of restarting, use LPS[" + (s.patternIndex() - 1) + "] = "
                    + s.nextPatternIndex() + ": keep that many matched characters and retry the same text character.";
        };
    }
}
