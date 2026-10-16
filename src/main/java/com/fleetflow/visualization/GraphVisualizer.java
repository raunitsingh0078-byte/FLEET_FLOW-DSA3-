package com.fleetflow.visualization;

import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Canvas based graph renderer: coloured nodes, directed or undirected edges with labels, dashed edges,
 * and curved offsets for antiparallel edge pairs. Exposes the canvas so pages can add mouse interaction.
 */
public class GraphVisualizer extends StackPane {

    /** A drawn vertex. */
    public static final class GNode {
        public final int id;
        public String label;
        public String sub = "";
        public double x;
        public double y;
        public CellState state = CellState.NORMAL;
        /** Optional highlight ring (e.g. source = MATCH, sink = MISMATCH). */
        public CellState ring;

        public GNode(int id, String label, double x, double y) {
            this.id = id;
            this.label = label;
            this.x = x;
            this.y = y;
        }
    }

    /** A drawn edge between the nodes with the given ids. */
    public static final class GEdge {
        public final int from;
        public final int to;
        public String label = "";
        public CellState state = CellState.NORMAL;
        public boolean directed = true;
        public boolean dashed;
        public double width = 1.8;

        public GEdge(int from, int to) {
            this.from = from;
            this.to = to;
        }
    }

    public static final double RADIUS = 21;

    private final Canvas canvas;
    private List<GNode> nodes = new ArrayList<>();
    private List<GEdge> edges = new ArrayList<>();

    public GraphVisualizer(double width, double height) {
        canvas = new Canvas(width, height);
        getChildren().add(canvas);
        getStyleClass().add("graph-canvas");
        setMinSize(width, height);
        setPrefSize(width, height);
        setMaxSize(width, height);
    }

    public Canvas canvas() {
        return canvas;
    }

    /** @return the node under the given pixel position, or null. */
    public GNode nodeAt(double x, double y) {
        for (int i = nodes.size() - 1; i >= 0; i--) {
            GNode n = nodes.get(i);
            if (Math.hypot(n.x - x, n.y - y) <= RADIUS + 3) {
                return n;
            }
        }
        return null;
    }

    public void render(List<GNode> newNodes, List<GEdge> newEdges) {
        nodes = newNodes;
        edges = newEdges;
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        g.setTextBaseline(VPos.CENTER);
        Set<Long> present = new HashSet<>();
        for (GEdge e : edges) {
            present.add(key(e.from, e.to));
        }
        for (GEdge e : edges) {
            drawEdge(g, e, present.contains(key(e.to, e.from)) && e.directed);
        }
        for (GNode n : nodes) {
            drawNode(g, n);
        }
    }

    private static long key(int a, int b) {
        return ((long) a << 32) | (b & 0xffffffffL);
    }

    private GNode find(int id) {
        for (GNode n : nodes) {
            if (n.id == id) {
                return n;
            }
        }
        return null;
    }

    private void drawEdge(GraphicsContext g, GEdge e, boolean antiparallel) {
        GNode a = find(e.from);
        GNode b = find(e.to);
        if (a == null || b == null || a == b) {
            return;
        }
        double dx = b.x - a.x;
        double dy = b.y - a.y;
        double len = Math.hypot(dx, dy);
        if (len < 1) {
            return;
        }
        double ux = dx / len;
        double uy = dy / len;
        double nx = -uy;
        double ny = ux;
        double off = antiparallel ? 9 : 0;
        double x1 = a.x + ux * RADIUS + nx * off;
        double y1 = a.y + uy * RADIUS + ny * off;
        double x2 = b.x - ux * (RADIUS + (e.directed ? 3 : 0)) + nx * off;
        double y2 = b.y - uy * (RADIUS + (e.directed ? 3 : 0)) + ny * off;
        Color color = Palette.edge(e.state);
        g.setStroke(color);
        g.setFill(color);
        g.setLineWidth(e.width);
        g.setLineDashes(e.dashed ? new double[]{6, 5} : null);
        g.strokeLine(x1, y1, x2, y2);
        g.setLineDashes((double[]) null);
        if (e.directed) {
            double ah = 9;
            double aw = 5;
            g.fillPolygon(new double[]{x2, x2 - ux * ah + nx * aw, x2 - ux * ah - nx * aw},
                    new double[]{y2, y2 - uy * ah + ny * aw, y2 - uy * ah - ny * aw}, 3);
        }
        if (e.label != null && !e.label.isEmpty()) {
            double lx = (x1 + x2) / 2 + nx * (antiparallel ? 9 : 11);
            double ly = (y1 + y2) / 2 + ny * (antiparallel ? 9 : 11);
            g.setFont(Font.font("Consolas", FontWeight.BOLD, 12));
            double w = 8.2 * e.label.length() + 8;
            g.setFill(Palette.background());
            g.fillRoundRect(lx - w / 2, ly - 9, w, 18, 8, 8);
            g.setFill(e.state == CellState.NORMAL ? Palette.ink() : color);
            g.setTextAlign(TextAlignment.CENTER);
            g.fillText(e.label, lx, ly);
        }
    }

    private void drawNode(GraphicsContext g, GNode n) {
        g.setFill(Palette.fill(n.state));
        g.fillOval(n.x - RADIUS, n.y - RADIUS, 2 * RADIUS, 2 * RADIUS);
        CellState ringState = n.ring != null ? n.ring : n.state;
        g.setStroke(Palette.stroke(ringState));
        g.setLineWidth(n.ring != null ? 4 : 2);
        g.strokeOval(n.x - RADIUS, n.y - RADIUS, 2 * RADIUS, 2 * RADIUS);
        g.setFill(Palette.text(n.state));
        g.setTextAlign(TextAlignment.CENTER);
        g.setFont(Font.font("Segoe UI", FontWeight.BOLD, n.label.length() > 3 ? 10 : 13));
        g.fillText(n.label, n.x, n.y);
        if (n.sub != null && !n.sub.isEmpty()) {
            g.setFill(Palette.mutedText());
            g.setFont(Font.font("Consolas", FontWeight.NORMAL, 11));
            g.fillText(n.sub, n.x, n.y + RADIUS + 11);
        }
    }
}
