package com.fleetflow.visualization;

/** Node placement helpers returning {x, y} pixel pairs. */
public final class GraphLayouts {

    private GraphLayouts() {
    }

    /** Places {@code count} nodes evenly on an ellipse. */
    public static double[][] circle(int count, double width, double height) {
        double[][] p = new double[count][2];
        double cx = width / 2;
        double cy = height / 2;
        double rx = width / 2 - 60;
        double ry = height / 2 - 50;
        for (int i = 0; i < count; i++) {
            double angle = -Math.PI / 2 + 2 * Math.PI * i / Math.max(1, count);
            p[i][0] = cx + rx * Math.cos(angle);
            p[i][1] = cy + ry * Math.sin(angle);
        }
        return p;
    }

    /** Two columns: {@code left} nodes on the left, {@code right} nodes on the right. Left nodes come first. */
    public static double[][] bipartite(int left, int right, double width, double height) {
        double[][] p = new double[left + right][2];
        for (int i = 0; i < left; i++) {
            p[i][0] = width * 0.25;
            p[i][1] = 40 + (height - 80) * (i + 0.5) / left;
        }
        for (int j = 0; j < right; j++) {
            p[left + j][0] = width * 0.75;
            p[left + j][1] = 40 + (height - 80) * (j + 0.5) / right;
        }
        return p;
    }
}
