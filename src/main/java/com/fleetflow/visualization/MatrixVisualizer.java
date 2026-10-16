package com.fleetflow.visualization;

import java.util.ArrayList;
import java.util.List;

/** Draws a DP matrix with row / column headers on top of {@link ArrayVisualizer}. */
public class MatrixVisualizer extends ArrayVisualizer {

    /**
     * @param rowHeaders one label per matrix row
     * @param colHeaders one label per matrix column
     * @param values     text of every cell; null entries are drawn as blanks
     * @param states     state of every cell (null = NORMAL)
     * @param focusCol   column to keep in view, or -1
     */
    public void show(String[] rowHeaders, String[] colHeaders, String[][] values, CellState[][] states, int focusCol) {
        List<String> names = new ArrayList<>();
        List<Cell[]> rows = new ArrayList<>();
        Cell[] header = new Cell[colHeaders.length];
        for (int c = 0; c < colHeaders.length; c++) {
            header[c] = cell(colHeaders[c], CellState.MUTED);
        }
        names.add("");
        rows.add(header);
        for (int r = 0; r < rowHeaders.length; r++) {
            Cell[] row = new Cell[colHeaders.length];
            for (int c = 0; c < colHeaders.length; c++) {
                String text = values[r][c];
                if (text != null) {
                    CellState st = states[r][c] == null ? CellState.NORMAL : states[r][c];
                    row[c] = cell(text, st);
                }
            }
            names.add(rowHeaders[r]);
            rows.add(row);
        }
        render(names, rows, focusCol);
    }
}
