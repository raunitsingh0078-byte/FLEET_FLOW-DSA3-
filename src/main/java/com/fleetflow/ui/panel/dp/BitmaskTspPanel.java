package com.fleetflow.ui.panel.dp;

import com.fleetflow.algorithm.dp.BitmaskTsp;
import com.fleetflow.ui.AlgorithmPanel;
import com.fleetflow.ui.Execution;
import com.fleetflow.util.Geo;
import com.fleetflow.util.ValidationException;
import com.fleetflow.visualization.CellState;
import com.fleetflow.visualization.GraphVisualizer;
import com.fleetflow.visualization.GraphVisualizer.GEdge;
import com.fleetflow.visualization.GraphVisualizer.GNode;
import com.fleetflow.visualization.MatrixVisualizer;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Bitmask-DP page: optimises a small delivery round trip (Held-Karp TSP) over real city distances. */
public final class BitmaskTspPanel extends AlgorithmPanel {

    private static final int MAX_STOPS = 7;
    private static final double W = 700;
    private static final double H = 300;

    private final TextArea stopsArea = new TextArea();
    private final GraphVisualizer map = new GraphVisualizer(W, H);
    private final MatrixVisualizer table = new MatrixVisualizer();

    public BitmaskTspPanel() {
        stopsArea.setPrefRowCount(6);
        stopsArea.setPromptText("One stop per line: Name latitude longitude   (the first line is the depot)");
        table.setPrefViewportHeight(260);
        assemble("Bitmask DP – Route Optimisation (TSP)",
                "Real problem: find the cheapest round trip that visits every delivery stop once. dp[mask][last] is the cheapest way to leave the depot, "
                        + "visit exactly the stops in the bit-mask and stand at 'last'; O(2ⁿ·n²), so it is used for small routes (≤ " + MAX_STOPS
                        + " stops here). Distances are great-circle kilometres.",
                stopsArea, new VBox(10, map, table));
        loadDemo();
    }

    @Override
    protected void loadDemo() {
        stopsArea.setText("Mumbai-Depot 19.076 72.878\nPune 18.520 73.856\nHyderabad 17.385 78.487\nBengaluru 12.972 77.594\nChennai 13.083 80.271\nAhmedabad 23.023 72.572");
    }

    private record Stop(String name, double lat, double lon) {
    }

    @Override
    protected Supplier<Execution> prepare() throws ValidationException {
        List<Stop> stops = new ArrayList<>();
        String raw = stopsArea.getText() == null ? "" : stopsArea.getText();
        int lineNo = 0;
        for (String line : raw.split("\\R")) {
            lineNo++;
            if (line.isBlank()) {
                continue;
            }
            String[] t = line.trim().split("\\s+");
            if (t.length < 3) {
                throw new ValidationException("Line " + lineNo + " must look like: Name latitude longitude");
            }
            try {
                double lat = Double.parseDouble(t[t.length - 2]);
                double lon = Double.parseDouble(t[t.length - 1]);
                if (lat < -90 || lat > 90 || lon < -180 || lon > 180) {
                    throw new ValidationException("Line " + lineNo + ": latitude must be in −90..90 and longitude in −180..180.");
                }
                stops.add(new Stop(String.join(" ", java.util.Arrays.copyOf(t, t.length - 2)), lat, lon));
            } catch (NumberFormatException ex) {
                throw new ValidationException("Line " + lineNo + ": latitude and longitude must be numbers.");
            }
        }
        if (stops.size() < 3 || stops.size() > MAX_STOPS) {
            throw new ValidationException("Please enter between 3 and " + MAX_STOPS + " stops (including the depot); you entered " + stops.size() + ".");
        }
        return () -> execute(stops);
    }

    private Execution execute(List<Stop> stops) {
        int n = stops.size();
        double[][] dist = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                dist[i][j] = Geo.haversineKm(stops.get(i).lat(), stops.get(i).lon(), stops.get(j).lat(), stops.get(j).lon());
            }
        }
        BitmaskTsp.Result r = BitmaskTsp.solve(dist);
        int masks = 1 << (n - 1);
        int frames = masks + 1;
        StringBuilder route = new StringBuilder();
        for (int city : r.route()) {
            route.append(route.length() == 0 ? "" : " → ").append(stops.get(city).name());
        }
        double naive = 0;
        for (int i = 0; i < n; i++) {
            naive += dist[i][(i + 1) % n];
        }
        String result = String.format("Minimum round-trip cost: %.1f km%nBest route: %s", r.cost(), route);
        String complexity = "Time O(2ⁿ·n²) = O(" + (1L << n) + "·" + n * n + ")   |   Space O(2ⁿ·n) = " + (1L << n) * n + " DP entries";
        String info = String.format("%d DP relaxations. Visiting the stops in the order entered would cost %.1f km, so DP saves %.1f km.",
                r.transitions(), naive, naive - r.cost());
        return new Execution(frames, f -> draw(stops, r, f), f -> narrate(stops, r, f), result, complexity, r.transitions(), frames, info);
    }

    private static int maskOf(int frame) {
        return 2 * frame + 1; // frame k shows the k-th mask that contains city 0
    }

    private void draw(List<Stop> stops, BitmaskTsp.Result r, int frame) {
        int n = stops.size();
        int masks = 1 << (n - 1);
        boolean closing = frame >= masks;
        int currentMask = maskOf(Math.min(frame, masks - 1));
        // partial route for the current mask
        List<Integer> partial = new ArrayList<>();
        if (closing) {
            for (int c : r.route()) partial.add(c);
        } else if (currentMask != 1) {
            int best = -1;
            for (int c = 1; c < n; c++) {
                if ((currentMask & (1 << c)) != 0 && (best < 0 || r.dp()[currentMask][c] < r.dp()[currentMask][best])) best = c;
            }
            int mask = currentMask;
            int cur = best;
            while (cur != 0 && cur >= 0) {
                partial.add(0, cur);
                int prev = r.parent()[mask][cur];
                mask ^= 1 << cur;
                cur = prev;
            }
            partial.add(0, 0);
        } else {
            partial.add(0);
        }
        double minLat = 90, maxLat = -90, minLon = 180, maxLon = -180;
        for (Stop s : stops) {
            minLat = Math.min(minLat, s.lat());
            maxLat = Math.max(maxLat, s.lat());
            minLon = Math.min(minLon, s.lon());
            maxLon = Math.max(maxLon, s.lon());
        }
        List<GNode> nodes = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            Stop s = stops.get(i);
            double x = 60 + (W - 120) * (maxLon == minLon ? 0.5 : (s.lon() - minLon) / (maxLon - minLon));
            double y = 40 + (H - 90) * (maxLat == minLat ? 0.5 : 1 - (s.lat() - minLat) / (maxLat - minLat));
            GNode node = new GNode(i, closing || partial.contains(i) ? String.valueOf(partial.indexOf(i)) : String.valueOf(i), x, y);
            node.sub = s.name();
            node.state = partial.contains(i) ? CellState.ACTIVE : (!closing && (currentMask & (1 << i)) != 0 ? CellState.DONE : CellState.NORMAL);
            if (i == 0) node.ring = CellState.MATCH;
            nodes.add(node);
        }
        List<GEdge> edges = new ArrayList<>();
        for (int k = 0; k + 1 < partial.size(); k++) {
            GEdge e = new GEdge(partial.get(k), partial.get(k + 1));
            e.state = closing ? CellState.MATCH : CellState.ACTIVE;
            e.width = 3;
            e.label = String.format("%.0f", r.dp()[0][0] == 0 && false ? 0 : distance(stops, partial.get(k), partial.get(k + 1)));
            edges.add(e);
        }
        map.render(nodes, edges);

        // DP table: rows are masks containing city 0, revealed up to the current one
        int shownRows = Math.min(frame, masks - 1) + 1;
        String[] rowH = new String[shownRows];
        String[] colH = new String[n];
        String[][] values = new String[shownRows][n];
        CellState[][] states = new CellState[shownRows][n];
        for (int c = 0; c < n; c++) colH[c] = "→" + c;
        for (int k = 0; k < shownRows; k++) {
            int mask = maskOf(k);
            StringBuilder bits = new StringBuilder(Integer.toBinaryString(mask));
            while (bits.length() < n) bits.insert(0, '0');
            rowH[k] = bits.toString();
            for (int c = 0; c < n; c++) {
                double v = r.dp()[mask][c];
                values[k][c] = v == BitmaskTsp.INF ? "∞" : String.valueOf(Math.round(v));
                states[k][c] = v == BitmaskTsp.INF ? CellState.MUTED : (k == shownRows - 1 && !closing ? CellState.ACTIVE : CellState.NORMAL);
            }
        }
        table.show(rowH, colH, values, states, -1);
        table.setVvalue(1.0);
    }

    private static double distance(List<Stop> stops, int a, int b) {
        return Geo.haversineKm(stops.get(a).lat(), stops.get(a).lon(), stops.get(b).lat(), stops.get(b).lon());
    }

    private static String narrate(List<Stop> stops, BitmaskTsp.Result r, int frame) {
        int n = stops.size();
        int masks = 1 << (n - 1);
        if (frame >= masks) {
            return String.format("All stops visited: close the tour by returning to the depot. Minimum total cost = %.1f km.", r.cost());
        }
        int mask = maskOf(frame);
        List<String> visited = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            if ((mask & (1 << i)) != 0) visited.add(stops.get(i).name());
        }
        return "State mask " + Integer.toBinaryString(mask) + " = visited {" + String.join(", ", visited)
                + "}. Each column holds the cheapest cost of ending at that stop; the map shows the best partial route for this subset.";
    }
}
