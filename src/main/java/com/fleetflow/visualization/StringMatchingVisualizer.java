package com.fleetflow.visualization;

import java.util.ArrayList;
import java.util.List;

/**
 * Shows a pattern sliding over a text, with optional match highlighting and an LPS table.
 * Used by KMP and Rabin-Karp.
 */
public class StringMatchingVisualizer extends ArrayVisualizer {

    /**
     * Everything needed to draw one frame.
     *
     * @param alignment      text index that pattern[0] is aligned with
     * @param textIndex      text character being compared, or -1 for none
     * @param patternIndex   pattern character being compared, or -1 for none (also = matched-prefix length)
     * @param compareState   colour of the compared cells
     * @param foundMask      text positions that belong to an already reported match (may be null)
     * @param windowState    if not null, the whole window text[alignment..alignment+m) is drawn in this state
     * @param lps            LPS table to display under the pattern, or null
     * @param lpsHighlight   LPS index to highlight, or -1
     */
    public record View(String text, String pattern, int alignment, int textIndex, int patternIndex,
                       CellState compareState, boolean[] foundMask, CellState windowState, int[] lps,
                       int lpsHighlight) {
    }

    public void show(View v) {
        int n = v.text().length();
        int m = v.pattern().length();
        Cell[] indexRow = new Cell[n];
        Cell[] textRow = new Cell[n];
        Cell[] patternRow = new Cell[Math.max(n, v.alignment() + m)];
        for (int i = 0; i < n; i++) {
            indexRow[i] = cell(String.valueOf(i), CellState.MUTED);
            CellState state = CellState.NORMAL;
            if (v.foundMask() != null && v.foundMask()[i]) {
                state = CellState.DONE;
            }
            if (v.windowState() != null && i >= v.alignment() && i < v.alignment() + m) {
                state = v.windowState();
            } else if (v.patternIndex() > 0 && i >= v.alignment() && i < v.alignment() + v.patternIndex()) {
                state = CellState.DONE;
            }
            if (i == v.textIndex()) {
                state = v.compareState();
            }
            textRow[i] = cell(String.valueOf(v.text().charAt(i)), state);
        }
        for (int k = 0; k < m; k++) {
            CellState state = CellState.NORMAL;
            if (v.windowState() != null) {
                state = v.windowState();
            } else if (k < v.patternIndex()) {
                state = CellState.DONE;
            }
            if (k == v.patternIndex() && v.textIndex() >= 0) {
                state = v.compareState();
            }
            patternRow[v.alignment() + k] = cell(String.valueOf(v.pattern().charAt(k)), state);
        }
        List<String> names = new ArrayList<>(List.of("Index", "Text", "Pattern"));
        List<Cell[]> rows = new ArrayList<>(List.of(indexRow, textRow, patternRow));
        if (v.lps() != null && m > 0) {
            Cell[] patternCopy = new Cell[m];
            Cell[] lpsRow = new Cell[m];
            for (int k = 0; k < m; k++) {
                patternCopy[k] = cell(String.valueOf(v.pattern().charAt(k)), CellState.NORMAL);
                lpsRow[k] = cell(String.valueOf(v.lps()[k]), k == v.lpsHighlight() ? CellState.HIGHLIGHT : CellState.MUTED);
            }
            names.add("Pattern (LPS)");
            rows.add(patternCopy);
            names.add("LPS table");
            rows.add(lpsRow);
        }
        render(names, rows, v.textIndex() >= 0 ? v.textIndex() : v.alignment());
    }
}
