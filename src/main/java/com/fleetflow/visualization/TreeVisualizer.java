package com.fleetflow.visualization;

import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ScrollPane;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Draws a rooted tree (trie, suffix tree, binary search tree, scan tree...) with per-node state colours,
 * optional edge labels and optional dashed "link" arrows (e.g. Aho-Corasick failure links).
 */
public class TreeVisualizer extends ScrollPane {

    /** A drawable tree node. */
    public static final class VNode {
        private final int id;
        private final String label;
        private final String edgeLabel;
        private final boolean terminal;
        private final List<VNode> children = new ArrayList<>();

        public VNode(int id, String label, String edgeLabel, boolean terminal) {
            this.id = id;
            this.label = label;
            this.edgeLabel = edgeLabel;
            this.terminal = terminal;
        }

        public VNode add(VNode child) {
            children.add(child);
            return child;
        }

        public int id() {
            return id;
        }
    }

    private static final double LEVEL_GAP = 74;
    private static final double MARGIN = 40;
    private static final double MAX_CANVAS = 8000;

    private final Canvas canvas = new Canvas(10, 10);
    private final double slotWidth;
    private final Map<Integer, double[]> positions = new HashMap<>();
    private final Map<Integer, Double> nodeWidths = new HashMap<>();
    private double nextSlot;
    private int maxDepth;

    /** @param slotWidth horizontal room reserved per leaf, in pixels */
    public TreeVisualizer(double slotWidth) {
        this.slotWidth = slotWidth;
        getStyleClass().add("viz-scroll");
        setContent(canvas);
        setPrefViewportHeight(300);
        setMinHeight(200);
    }

    /**
     * Redraws the tree.
     *
     * @param states node id to state (missing = NORMAL)
     * @param links  dashed arrows as {fromNodeId, toNodeId}
     */
    public void render(VNode root, Map<Integer, CellState> states, List<int[]> links) {
        positions.clear();
        nodeWidths.clear();
        nextSlot = 0;
        maxDepth = 0;
        layout(root, 0);
        double width = Math.min(MAX_CANVAS, Math.max(300, nextSlot * slotWidth + 2 * MARGIN));
        double height = (maxDepth + 1) * LEVEL_GAP + 2 * MARGIN - 20;
        canvas.setWidth(width);
        canvas.setHeight(height);
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.clearRect(0, 0, width, height);
        g.setTextBaseline(VPos.CENTER);
        drawEdges(g, root);
        for (int[] link : links) {
            drawLink(g, link[0], link[1]);
        }
        drawNodes(g, root, states);
    }

    /** Assigns leaf slots left-to-right; a parent sits above the middle of its children. */
    private double layout(VNode node, int depth) {
        maxDepth = Math.max(maxDepth, depth);
        double x;
        if (node.children.isEmpty()) {
            x = nextSlot + 0.5;
            nextSlot += 1;
        } else {
            double first = 0;
            double last = 0;
            for (int i = 0; i < node.children.size(); i++) {
                double cx = layout(node.children.get(i), depth + 1);
                if (i == 0) first = cx;
                last = cx;
            }
            x = (first + last) / 2;
        }
        positions.put(node.id, new double[]{MARGIN + x * slotWidth, MARGIN + depth * LEVEL_GAP});
        nodeWidths.put(node.id, Math.max(32, 8.5 * node.label.length() + 14));
        return x;
    }

    private void drawEdges(GraphicsContext g, VNode node) {
        double[] p = positions.get(node.id);
        for (VNode child : node.children) {
            double[] c = positions.get(child.id);
            g.setStroke(Palette.treeEdge());
            g.setLineWidth(1.6);
            g.strokeLine(p[0], p[1], c[0], c[1]);
            if (child.edgeLabel != null && !child.edgeLabel.isEmpty()) {
                String label = child.edgeLabel.length() > 14 ? child.edgeLabel.substring(0, 13) + "…" : child.edgeLabel;
                double w = 7.2 * label.length() + 10;
                double lx = c[0];
                double ly = c[1] - 30;
                g.setFill(Palette.background());
                g.fillRoundRect(lx - w / 2, ly - 8, w, 16, 8, 8);
                g.setFill(Palette.mutedText());
                g.setTextAlign(TextAlignment.CENTER);
                g.setFont(Font.font("Consolas", FontWeight.NORMAL, 11));
                g.fillText(label, lx, ly);
            }
            drawEdges(g, child);
        }
    }

    private void drawLink(GraphicsContext g, int from, int to) {
        double[] a = positions.get(from);
        double[] b = positions.get(to);
        if (a == null || b == null) {
            return;
        }
        g.setStroke(Palette.link());
        g.setLineWidth(1.3);
        g.setLineDashes(5, 4);
        g.setLineCap(StrokeLineCap.ROUND);
        g.beginPath();
        g.moveTo(a[0], a[1]);
        g.quadraticCurveTo((a[0] + b[0]) / 2 - 30, (a[1] + b[1]) / 2 - 30, b[0], b[1]);
        g.stroke();
        g.setLineDashes((double[]) null);
    }

    private void drawNodes(GraphicsContext g, VNode node, Map<Integer, CellState> states) {
        double[] p = positions.get(node.id);
        double w = nodeWidths.get(node.id);
        double h = 30;
        CellState state = states.getOrDefault(node.id, CellState.NORMAL);
        g.setFill(Palette.fill(state));
        g.fillRoundRect(p[0] - w / 2, p[1] - h / 2, w, h, h, h);
        g.setStroke(Palette.stroke(state));
        g.setLineWidth(node.terminal ? 3 : 1.6);
        g.strokeRoundRect(p[0] - w / 2, p[1] - h / 2, w, h, h, h);
        if (node.terminal) {
            g.setLineWidth(1);
            g.strokeRoundRect(p[0] - w / 2 - 3, p[1] - h / 2 - 3, w + 6, h + 6, h + 6, h + 6);
        }
        g.setFill(Palette.text(state));
        g.setTextAlign(TextAlignment.CENTER);
        g.setFont(Font.font("Consolas", FontWeight.BOLD, 13));
        g.fillText(node.label, p[0], p[1]);
        for (VNode child : node.children) {
            drawNodes(g, child, states);
        }
    }
}
