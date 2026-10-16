package com.fleetflow.ui.panel.suffix;

import com.fleetflow.algorithm.suffix.SaIs;
import com.fleetflow.algorithm.suffix.SuffixArray;
import com.fleetflow.ui.AlgorithmPanel;
import com.fleetflow.ui.Execution;
import com.fleetflow.util.ValidationException;
import com.fleetflow.visualization.ArrayVisualizer;
import com.fleetflow.visualization.ArrayVisualizer.Cell;
import com.fleetflow.visualization.CellState;
import com.fleetflow.visualization.RowTableVisualizer;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.function.Supplier;

/** SA-IS page: shows S/L classification and LMS positions of every recursion level, then the final suffix array. */
public final class SaIsPanel extends AlgorithmPanel {

    private final DocumentInput document = new DocumentInput(120);
    private final ArrayVisualizer levelView = new ArrayVisualizer();
    private final RowTableVisualizer saTable = new RowTableVisualizer("Rank", "SA[rank]", "Suffix");

    public SaIsPanel() {
        levelView.setPrefViewportHeight(190);
        saTable.setPrefHeight(220);
        assemble("SA-IS (linear-time suffix array)",
                "Real problem: index very large logistics documents in O(n). Every suffix is typed S (smaller than the next) or L (larger). "
                        + "LMS suffixes (an S right after an L) are named and sorted recursively, then all other suffixes are induced from them "
                        + "with two linear scans. The result is verified against the sort-based suffix array.",
                document, new VBox(10, levelView, saTable));
        loadDemo();
    }

    @Override
    protected void loadDemo() {
        document.setText("SHP-2041 to Bengaluru; SHP-2042 to Bengaluru");
    }

    @Override
    protected Supplier<Execution> prepare() throws ValidationException {
        String text = document.read();
        return () -> execute(text);
    }

    private Execution execute(String text) {
        SaIs.Result r = SaIs.build(text);
        boolean verified = Arrays.equals(r.sa(), SuffixArray.build(text));
        int frames = r.levels().size() + 1;
        String result = "SA = " + Arrays.toString(r.sa()) + "\nVerification against sort-based construction: " + (verified ? "IDENTICAL ✓" : "MISMATCH ✗");
        String complexity = "Time O(n)   |   Space O(n)   |   recursion depth " + r.levels().size() + " (each level at most halves the problem)";
        StringBuilder info = new StringBuilder();
        for (int i = 0; i < r.levels().size(); i++) {
            SaIs.Level lv = r.levels().get(i);
            info.append("Level ").append(i).append(": n=").append(lv.values().length).append(", LMS suffixes=").append(lv.lms().size()).append("; ");
        }
        return new Execution(frames, f -> draw(text, r, f), f -> narrate(r, f), result, complexity, -1, frames, info.toString().trim());
    }

    private void draw(String text, SaIs.Result r, int frame) {
        if (frame < r.levels().size()) {
            SaIs.Level lv = r.levels().get(frame);
            int n = lv.values().length;
            Cell[] idx = new Cell[n];
            Cell[] val = new Cell[n];
            Cell[] type = new Cell[n];
            for (int i = 0; i < n; i++) {
                idx[i] = ArrayVisualizer.cell(String.valueOf(i), CellState.MUTED);
                String shown = frame == 0 ? String.valueOf(text.charAt(i)) : String.valueOf(lv.values()[i]);
                val[i] = ArrayVisualizer.cell(shown, CellState.NORMAL);
                boolean lms = lv.lms().contains(i);
                char t = lv.types().charAt(i);
                type[i] = ArrayVisualizer.cell(lms ? "S*" : String.valueOf(t), lms ? CellState.HIGHLIGHT : t == 'S' ? CellState.DONE : CellState.NORMAL);
            }
            levelView.render(List.of("Index", frame == 0 ? "Text" : "Reduced str", "Type (S*=LMS)"), Arrays.asList(idx, val, type), -1);
            saTable.show(new ArrayList<>(), new HashMap<>());
        } else {
            int n = r.sa().length;
            Cell[] idx = new Cell[n];
            Cell[] sa = new Cell[n];
            for (int i = 0; i < n; i++) {
                idx[i] = ArrayVisualizer.cell(String.valueOf(i), CellState.MUTED);
                sa[i] = ArrayVisualizer.cell(String.valueOf(r.sa()[i]), CellState.MATCH);
            }
            levelView.render(List.of("Rank", "SA[rank]"), Arrays.asList(idx, sa), -1);
            List<String[]> rows = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                String s = text.substring(r.sa()[i]);
                rows.add(new String[]{String.valueOf(i), String.valueOf(r.sa()[i]), s.length() > 60 ? s.substring(0, 60) + "…" : s});
            }
            saTable.show(rows, new HashMap<>());
        }
    }

    private static String narrate(SaIs.Result r, int frame) {
        if (frame < r.levels().size()) {
            SaIs.Level lv = r.levels().get(frame);
            String head = frame == 0 ? "Level 0 (the original text): " : "Recursion level " + frame + " (reduced string of LMS-substring names): ";
            return head + "classify each position as S or L scanning right-to-left (a suffix is S if it is smaller than the next one). "
                    + lv.lms().size() + " LMS position(s) (purple) are found"
                    + (lv.reducedLength() > 0 ? "; their substrings are named and sorted recursively (reduced length " + lv.reducedLength() + ")." : ".");
        }
        return "Final step: the sorted LMS suffixes are placed at bucket ends, then L-type suffixes are induced left-to-right and S-type right-to-left. The suffix array is complete.";
    }
}
