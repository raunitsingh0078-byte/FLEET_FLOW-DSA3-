package com.fleetflow.visualization;

import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ScrollPane;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import java.util.List;

/**
 * Draws several labelled rows of cells (arrays, strings, DP rows) on a canvas. A {@code null} cell
 * leaves a gap, which is how shifted rows (e.g. a pattern sliding over text) are drawn.
 */
public class ArrayVisualizer extends ScrollPane {

    /** One cell: its text and semantic state. */
    public record Cell(String text, CellState state) {
    }

    private static final double CELL = 34;
    private static final double GAP = 3;
    private static final double LABEL_WIDTH = 96;
    private static final double ROW_GAP = 12;
    private static final double MAX_CANVAS = 8000;

    private final Canvas canvas = new Canvas(10, 10);

    public ArrayVisualizer() {
        getStyleClass().add("viz-scroll");
        setContent(canvas);
        setPrefViewportHeight(290);
        setMinHeight(200);
    }

    public static Cell cell(String text, CellState state) {
        return new Cell(text, state);
    }

    /**
     * Redraws the whole grid.
     *
     * @param rowNames one label per row
     * @param rows     cells per row; entries may be null for gaps
     */
    public void render(List<String> rowNames, List<Cell[]> rows) {
        render(rowNames, rows, -1);
    }

    /**
     * Redraws the grid and scrolls horizontally so that {@code focusColumn} stays visible.
     *
     * @param focusColumn column to keep in view, or -1 to leave the scroll position unchanged
     */
    public void render(List<String> rowNames, List<Cell[]> rows, int focusColumn) {
        int maxCols = 0;
        for (Cell[] row : rows) {
            maxCols = Math.max(maxCols, row.length);
        }
        double width = Math.min(MAX_CANVAS, LABEL_WIDTH + maxCols * (CELL + GAP) + 24);
        double height = Math.max(60, rows.size() * (CELL + ROW_GAP) + 16);
        canvas.setWidth(width);
        canvas.setHeight(height);
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.clearRect(0, 0, width, height);
        g.setTextBaseline(VPos.CENTER);
        for (int r = 0; r < rows.size(); r++) {
            double y = 8 + r * (CELL + ROW_GAP);
            g.setFill(Palette.mutedText());
            g.setTextAlign(TextAlignment.LEFT);
            g.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 12));
            g.fillText(rowNames.get(r), 6, y + CELL / 2);
            Cell[] row = rows.get(r);
            for (int c = 0; c < row.length; c++) {
                if (row[c] != null) {
                    drawCell(g, LABEL_WIDTH + c * (CELL + GAP), y, row[c]);
                }
            }
        }
        scrollTo(focusColumn, width);
    }

    private void scrollTo(int column, double canvasWidth) {
        double viewport = getViewportBounds().getWidth();
        if (column < 0 || viewport <= 0 || canvasWidth <= viewport) {
            return;
        }
        double x = LABEL_WIDTH + column * (CELL + GAP);
        setHvalue(Math.max(0, Math.min(1, (x - viewport / 2) / (canvasWidth - viewport))));
    }

    private void drawCell(GraphicsContext g, double x, double y, Cell cell) {
        g.setFill(Palette.fill(cell.state()));
        g.fillRoundRect(x, y, CELL, CELL, 8, 8);
        g.setStroke(Palette.stroke(cell.state()));
        g.setLineWidth(cell.state() == CellState.NORMAL || cell.state() == CellState.MUTED ? 1 : 2);
        g.strokeRoundRect(x, y, CELL, CELL, 8, 8);
        g.setFill(Palette.text(cell.state()));
        g.setTextAlign(TextAlignment.CENTER);
        String text = cell.text();
        double size = text.length() <= 2 ? 15 : text.length() == 3 ? 12 : 9;
        g.setFont(Font.font("Consolas", FontWeight.BOLD, size));
        g.fillText(text, x + CELL / 2, y + CELL / 2);
    }
}
