package com.fleetflow.ui.panel.suffix;

import com.fleetflow.algorithm.suffix.Lcp;
import com.fleetflow.algorithm.suffix.SuffixArray;
import com.fleetflow.ui.AlgorithmPanel;
import com.fleetflow.ui.Execution;
import com.fleetflow.ui.Ui;
import com.fleetflow.util.ValidationException;
import com.fleetflow.visualization.CellState;
import com.fleetflow.visualization.RowTableVisualizer;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/** LCP / Kasai page: computes the LCP array in text order and extracts repeated addresses / descriptions. */
public final class LcpPanel extends AlgorithmPanel {

    private final DocumentInput document = new DocumentInput(200);
    private final TextField minLengthField = new TextField("8");
    private final RowTableVisualizer table = new RowTableVisualizer("Rank", "SA[rank]", "LCP", "Suffix");

    public LcpPanel() {
        minLengthField.setPrefColumnCount(5);
        HBox min = new HBox(8, Ui.label("Minimum repeat length", "kv-key"), minLengthField);
        assemble("LCP Array (Kasai)",
                "Real problem: detect repeated addresses or duplicated descriptions. LCP[i] is the longest common prefix of two neighbouring "
                        + "suffixes in sorted order; large LCP values mark repeated substrings. Kasai computes all LCP values in O(n) by "
                        + "processing suffixes in text order and reusing h − 1 from the previous step.",
                new VBox(8, document, min), table);
        loadDemo();
    }

    @Override
    protected void loadDemo() {
        document.setText("42 MG Road, Indiranagar, Bengaluru | 17 Linking Road, Mumbai | 42 MG Road, Indiranagar, Bengaluru");
        minLengthField.setText("10");
    }

    @Override
    protected Supplier<Execution> prepare() throws ValidationException {
        String text = document.read();
        int min;
        try {
            min = Integer.parseInt(minLengthField.getText().trim());
        } catch (NumberFormatException e) {
            throw new ValidationException("The minimum repeat length must be a whole number.");
        }
        if (min < 1 || min > text.length()) {
            throw new ValidationException("The minimum repeat length must be between 1 and " + text.length() + ".");
        }
        return () -> execute(text, min);
    }

    private Execution execute(String text, int min) {
        int[] sa = SuffixArray.build(text);
        Lcp.Result k = Lcp.kasai(text, sa);
        List<Lcp.Repeat> repeats = Lcp.repeats(text, sa, k.lcp(), min);
        StringBuilder sb = new StringBuilder("LCP = " + Arrays.toString(k.lcp()));
        if (repeats.isEmpty()) {
            sb.append("\nNo substring of length ≥ ").append(min).append(" is repeated.");
        }
        for (Lcp.Repeat rep : repeats) {
            sb.append("\nRepeated ").append(rep.positions().size()).append("×: \"").append(rep.text()).append("\" at ").append(rep.positions());
        }
        String complexity = "Kasai O(n) (each h decreases by at most 1 per step)   |   Space O(n) for rank and LCP";
        String info = k.comparisons() + " character comparisons for " + text.length() + " positions (≤ 2n = " + 2L * text.length() + ").";
        int frames = k.steps().size();
        return new Execution(frames, f -> draw(text, sa, k, f), f -> narrate(text, k, f), sb.toString(), complexity,
                k.comparisons(), frames, info);
    }

    private void draw(String text, int[] sa, Lcp.Result k, int frame) {
        Map<Integer, CellState> states = new HashMap<>();
        boolean[] known = new boolean[sa.length];
        for (int i = 0; i <= frame; i++) {
            known[k.steps().get(i).rank()] = true;
        }
        Lcp.Step current = k.steps().get(frame);
        List<String[]> rows = new ArrayList<>();
        for (int r = 0; r < sa.length; r++) {
            String suffix = text.substring(sa[r]);
            rows.add(new String[]{String.valueOf(r), String.valueOf(sa[r]), known[r] || r == 0 ? String.valueOf(k.lcp()[r]) : "?",
                    suffix.length() > 55 ? suffix.substring(0, 55) + "…" : suffix});
            if (known[r] && k.lcp()[r] > 0) {
                states.put(r, CellState.DONE);
            }
        }
        states.put(current.rank(), CellState.ACTIVE);
        table.show(rows, states);
        table.scrollTo(Math.max(0, current.rank() - 3));
    }

    private static String narrate(String text, Lcp.Result k, int frame) {
        Lcp.Step s = k.steps().get(frame);
        if (s.rank() == 0) {
            return "Text position " + s.textPos() + " has rank 0 (smallest suffix): no predecessor, LCP = 0 and h resets to 0.";
        }
        return "Text position " + s.textPos() + " (rank " + s.rank() + "): " + (s.reused() ? "start from the inherited h (previous h − 1) and " : "start from h = 0 and ")
                + "extend while characters match with the predecessor suffix → LCP[" + s.rank() + "] = " + s.h() + ".";
    }
}
