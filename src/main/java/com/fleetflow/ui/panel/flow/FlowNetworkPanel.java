package com.fleetflow.ui.panel.flow;

import com.fleetflow.algorithm.flow.Dinic;
import com.fleetflow.algorithm.flow.EdmondsKarp;
import com.fleetflow.algorithm.flow.FlowResult;
import com.fleetflow.algorithm.flow.FlowStep;
import com.fleetflow.algorithm.flow.FordFulkerson;
import com.fleetflow.algorithm.flow.MinCut;
import com.fleetflow.data.FleetRepository;
import com.fleetflow.data.RouteNetwork;
import com.fleetflow.datastructure.graph.FlowNetwork;
import com.fleetflow.model.Warehouse;
import com.fleetflow.ui.AlgorithmPanel;
import com.fleetflow.ui.Execution;
import com.fleetflow.ui.Ui;
import com.fleetflow.util.Dialogs;
import com.fleetflow.util.ThemeManager;
import com.fleetflow.util.ValidationException;
import com.fleetflow.visualization.CellState;
import com.fleetflow.visualization.GraphVisualizer;
import com.fleetflow.visualization.GraphVisualizer.GEdge;
import com.fleetflow.visualization.GraphVisualizer.GNode;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Interactive Network Flow Simulator: the user builds a warehouse / distribution-centre / destination network on a
 * canvas, chooses source and sink and watches Ford-Fulkerson, Edmonds-Karp, Dinic or the min-cut analysis work step by step.
 */
public final class FlowNetworkPanel extends AlgorithmPanel {

    private static final double W = 900;
    private static final double H = 420;
    private static final int MAX_NODES = 14;
    private static final String FF = "Ford-Fulkerson";
    private static final String EK = "Edmonds-Karp";
    private static final String DINIC = "Dinic";
    private static final String CUT = "Minimum Cut";

    private enum Mode { ADD_WAREHOUSE, ADD_DC, ADD_DEST, ADD_EDGE, MOVE, SET_SOURCE, SET_SINK, DELETE }

    private enum Type {
        WAREHOUSE("W", "warehouse"), DC("DC", "dist. centre"), DEST("D", "destination");

        final String prefix;
        final String caption;

        Type(String prefix, String caption) {
            this.prefix = prefix;
            this.caption = caption;
        }
    }

    private static final class Node {
        final int id;
        final String label;
        final Type type;
        double x;
        double y;

        Node(int id, String label, Type type, double x, double y) {
            this.id = id;
            this.label = label;
            this.type = type;
            this.x = x;
            this.y = y;
        }
    }

    private static final class Edge {
        final int from;
        final int to;
        int cap;

        Edge(int from, int to, int cap) {
            this.from = from;
            this.to = to;
            this.cap = cap;
        }
    }

    /** Immutable copy of the editable network, safe to hand to the worker thread and to renderers. */
    private record Model(List<Node> nodes, List<int[]> edges, int source, int sink) {
        int indexOf(int nodeId) {
            for (int i = 0; i < nodes.size(); i++) {
                if (nodes.get(i).id == nodeId) {
                    return i;
                }
            }
            return -1;
        }
    }

    private final List<Node> nodes = new ArrayList<>();
    private final List<Edge> edges = new ArrayList<>();
    private int nextId = 1;
    private int source = -1;
    private int sink = -1;
    private Mode mode = Mode.MOVE;
    private Node pendingFrom;
    private Node dragging;
    private Runnable redraw = () -> { };
    /** True while an animation frame is shown (edge labels then read flow/capacity instead of capacity only). */
    private boolean animating;

    private final GraphVisualizer graph = new GraphVisualizer(W, H);
    private final ComboBox<String> algorithm = new ComboBox<>();
    private final CheckBox residualBox = new CheckBox("Show residual graph (remaining capacity + dashed back-edges)");
    private final CheckBox cutBox = new CheckBox("Highlight minimum cut at the end");

    public FlowNetworkPanel() {
        algorithm.getItems().addAll(FF, EK, DINIC, CUT);
        algorithm.setValue(DINIC);
        residualBox.setOnAction(e -> refreshVisualization());
        cutBox.setSelected(true);
        cutBox.setOnAction(e -> refreshVisualization());

        ToggleGroup group = new ToggleGroup();
        FlowPane tools = new FlowPane(8, 8);
        for (Mode m : Mode.values()) {
            ToggleButton b = new ToggleButton(toolName(m));
            b.setToggleGroup(group);
            b.setOnAction(e -> {
                mode = m;
                pendingFrom = null;
                if (!b.isSelected()) {
                    b.setSelected(true);
                }
            });
            if (m == Mode.MOVE) {
                b.setSelected(true);
            }
            tools.getChildren().add(b);
        }
        Button classic = Ui.button("Load classic network", "button-secondary", "Six-node textbook network (max flow 23)");
        classic.setOnAction(e -> loadClassic());
        Button logistics = Ui.button("Load warehouse network", "button-secondary", "The real inter-warehouse lanes from the fleet data");
        logistics.setOnAction(e -> loadLogistics());
        Button clear = Ui.button("Clear", "button-danger", "Remove every node and edge");
        clear.setOnAction(e -> {
            clearAll();
            renderStatic();
        });
        FlowPane actions = new FlowPane(8, 8, new javafx.scene.control.Label("Algorithm"), algorithm, classic, logistics, clear);
        FlowPane options = new FlowPane(16, 6, residualBox, cutBox);

        graph.canvas().setOnMousePressed(e -> onPress(e.getX(), e.getY()));
        graph.canvas().setOnMouseDragged(e -> onDrag(e.getX(), e.getY()));
        graph.canvas().setOnMouseReleased(e -> dragging = null);
        ScrollPane scroll = new ScrollPane(graph);
        scroll.getStyleClass().add("viz-scroll");
        scroll.setPrefViewportHeight(H + 8);
        scroll.setPannable(false);

        VBox inputs = new VBox(8, Ui.label("Tool (click the canvas): pick a tool, then click an empty spot to add a node, click two nodes to connect them "
                + "(you will be asked for the capacity), or drag nodes in Move mode.", "muted"), tools, actions, options);
        javafx.scene.control.Label hint = (javafx.scene.control.Label) inputs.getChildren().get(0);
        hint.setWrapText(true);
        hint.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
        assemble("Network Flow Simulator – Max Flow, Min Cut and Bottlenecks",
                "Real problem: how many pallets can move from the source warehouse to the destination per day, and which lanes are the bottleneck? "
                        + "Ford-Fulkerson pushes flow along any augmenting path, Edmonds-Karp always uses the shortest one (BFS), and Dinic builds a BFS level "
                        + "graph and saturates it with a blocking flow. The minimum cut (max-flow min-cut theorem) is the set of saturated lanes that separates "
                        + "source from sink.",
                inputs, scroll);
        ThemeManager.addListener(() -> redraw.run());
        loadClassic();
    }

    private static String toolName(Mode m) {
        return switch (m) {
            case ADD_WAREHOUSE -> "＋ Warehouse";
            case ADD_DC -> "＋ Distribution centre";
            case ADD_DEST -> "＋ Destination";
            case ADD_EDGE -> "➜ Add edge";
            case MOVE -> "✥ Move";
            case SET_SOURCE -> "◉ Set source";
            case SET_SINK -> "◎ Set sink";
            case DELETE -> "✕ Delete";
        };
    }

    @Override
    public void prefer(String name) {
        if (algorithm.getItems().contains(name)) {
            algorithm.setValue(name);
        }
    }

    // ------------------------------------------------------------------ editing

    private void clearAll() {
        nodes.clear();
        edges.clear();
        source = -1;
        sink = -1;
        pendingFrom = null;
    }

    private Node addNode(Type type, double x, double y) {
        int n = 1;
        while (true) {
            String candidate = type.prefix + n;
            final int number = n;
            if (nodes.stream().noneMatch(node -> node.label.equals(type.prefix + number))) {
                Node node = new Node(nextId++, candidate, type, x, y);
                nodes.add(node);
                return node;
            }
            n++;
        }
    }

    private Node byId(int id) {
        for (Node n : nodes) {
            if (n.id == id) {
                return n;
            }
        }
        return null;
    }

    private void addEdge(Node from, Node to, int cap) {
        for (Edge e : edges) {
            if (e.from == from.id && e.to == to.id) {
                e.cap = cap;
                return;
            }
        }
        edges.add(new Edge(from.id, to.id, cap));
    }

    private void loadClassic() {
        clearAll();
        Node s = addNode(Type.WAREHOUSE, 70, 210);
        Node v1 = addNode(Type.DC, 300, 90);
        Node v2 = addNode(Type.DC, 300, 330);
        Node v3 = addNode(Type.DC, 600, 90);
        Node v4 = addNode(Type.DC, 600, 330);
        Node t = addNode(Type.DEST, 830, 210);
        source = s.id;
        sink = t.id;
        int[][] lanes = {{0, 1, 16}, {0, 2, 13}, {1, 3, 12}, {2, 1, 4}, {2, 4, 14}, {3, 2, 9}, {3, 5, 20}, {4, 3, 7}, {4, 5, 4}};
        Node[] all = {s, v1, v2, v3, v4, t};
        for (int[] l : lanes) {
            addEdge(all[l[0]], all[l[1]], l[2]);
        }
        renderStatic();
    }

    private void loadLogistics() {
        clearAll();
        Map<String, Node> byName = new HashMap<>();
        for (Warehouse w : FleetRepository.get().warehouses()) {
            Node n = new Node(nextId++, w.id(), w.id().equals(RouteNetwork.DEFAULT_SOURCE) ? Type.WAREHOUSE
                    : w.id().equals(RouteNetwork.DEFAULT_SINK) ? Type.DEST : Type.DC, 60 + w.mapX() * (W - 120), 40 + w.mapY() * (H - 80));
            nodes.add(n);
            byName.put(w.id(), n);
        }
        for (RouteNetwork.Route r : RouteNetwork.routes()) {
            if (byName.containsKey(r.from()) && byName.containsKey(r.to())) {
                addEdge(byName.get(r.from()), byName.get(r.to()), r.capacity());
            }
        }
        source = byName.containsKey(RouteNetwork.DEFAULT_SOURCE) ? byName.get(RouteNetwork.DEFAULT_SOURCE).id : -1;
        sink = byName.containsKey(RouteNetwork.DEFAULT_SINK) ? byName.get(RouteNetwork.DEFAULT_SINK).id : -1;
        renderStatic();
    }

    private void onPress(double x, double y) {
        GNode hit = graph.nodeAt(x, y);
        Node node = hit == null ? null : byId(hit.id);
        switch (mode) {
            case ADD_WAREHOUSE -> placeNode(Type.WAREHOUSE, node, x, y);
            case ADD_DC -> placeNode(Type.DC, node, x, y);
            case ADD_DEST -> placeNode(Type.DEST, node, x, y);
            case ADD_EDGE -> {
                if (node == null) {
                    pendingFrom = null;
                } else if (pendingFrom == null) {
                    pendingFrom = node;
                } else if (pendingFrom == node) {
                    pendingFrom = null;
                } else {
                    askCapacityAndAdd(pendingFrom, node);
                    pendingFrom = null;
                }
            }
            case MOVE -> dragging = node;
            case SET_SOURCE -> {
                if (node != null) {
                    if (node.id == sink) {
                        Dialogs.warning("Invalid choice", "The source and the sink must be different nodes.");
                    } else {
                        source = node.id;
                    }
                }
            }
            case SET_SINK -> {
                if (node != null) {
                    if (node.id == source) {
                        Dialogs.warning("Invalid choice", "The source and the sink must be different nodes.");
                    } else {
                        sink = node.id;
                    }
                }
            }
            case DELETE -> deleteAt(node, x, y);
        }
        renderStatic();
    }

    private void placeNode(Type type, Node existing, double x, double y) {
        if (existing != null) {
            return;
        }
        if (nodes.size() >= MAX_NODES) {
            Dialogs.warning("Network too large", "The simulator supports at most " + MAX_NODES + " nodes so the animation stays readable.");
            return;
        }
        addNode(type, Math.max(30, Math.min(W - 30, x)), Math.max(30, Math.min(H - 40, y)));
    }

    private void onDrag(double x, double y) {
        if (mode == Mode.MOVE && dragging != null) {
            dragging.x = Math.max(25, Math.min(W - 25, x));
            dragging.y = Math.max(25, Math.min(H - 35, y));
            renderStatic();
        }
    }

    private void askCapacityAndAdd(Node from, Node to) {
        TextInputDialog dialog = new TextInputDialog("10");
        dialog.setTitle("Edge capacity");
        dialog.setHeaderText("Capacity of the lane " + from.label + " → " + to.label);
        dialog.setContentText("Capacity (whole number 1–999):");
        ThemeManager.style(dialog.getDialogPane());
        Optional<String> answer = dialog.showAndWait();
        if (answer.isEmpty()) {
            return;
        }
        try {
            int cap = Integer.parseInt(answer.get().trim());
            if (cap < 1 || cap > 999) {
                throw new NumberFormatException();
            }
            addEdge(from, to, cap);
        } catch (NumberFormatException ex) {
            Dialogs.warning("Invalid capacity", "Please enter a whole number between 1 and 999.");
        }
    }

    private void deleteAt(Node node, double x, double y) {
        if (node != null) {
            edges.removeIf(e -> e.from == node.id || e.to == node.id);
            nodes.remove(node);
            if (source == node.id) source = -1;
            if (sink == node.id) sink = -1;
            return;
        }
        Edge best = null;
        double bestDistance = 9;
        for (Edge e : edges) {
            Node a = byId(e.from);
            Node b = byId(e.to);
            double d = distanceToSegment(x, y, a.x, a.y, b.x, b.y);
            if (d < bestDistance) {
                bestDistance = d;
                best = e;
            }
        }
        if (best != null) {
            edges.remove(best);
        }
    }

    private static double distanceToSegment(double px, double py, double ax, double ay, double bx, double by) {
        double dx = bx - ax;
        double dy = by - ay;
        double len2 = dx * dx + dy * dy;
        double t = len2 == 0 ? 0 : Math.max(0, Math.min(1, ((px - ax) * dx + (py - ay) * dy) / len2));
        return Math.hypot(px - (ax + t * dx), py - (ay + t * dy));
    }

    // ------------------------------------------------------------------ static drawing

    private void renderStatic() {
        Model m = snapshot();
        redraw = () -> {
            animating = false;
            paint(m, new int[m.edges().size()], null, null, null, null, false);
        };
        redraw.run();
    }

    private Model snapshot() {
        List<Node> nodeCopy = new ArrayList<>();
        for (Node n : nodes) {
            nodeCopy.add(new Node(n.id, n.label, n.type, n.x, n.y));
        }
        List<int[]> edgeCopy = new ArrayList<>();
        for (Edge e : edges) {
            edgeCopy.add(new int[]{e.from, e.to, e.cap});
        }
        return new Model(nodeCopy, edgeCopy, source, sink);
    }

    /**
     * Draws a network.
     *
     * @param pathEdges  forward-edge flags to highlight as the current augmenting path (may be null)
     * @param pathNodes  node-index flags on the path (may be null)
     * @param levels     Dinic levels per node index (may be null)
     * @param cut        minimum cut to highlight (may be null)
     */
    private void paint(Model m, int[] flows, boolean[] pathEdges, boolean[] pathNodes, int[] levels, MinCut.Result cut,
                       boolean levelGraph) {
        List<GNode> gn = new ArrayList<>();
        for (int i = 0; i < m.nodes().size(); i++) {
            Node n = m.nodes().get(i);
            GNode g = new GNode(n.id, n.label, n.x, n.y);
            g.sub = levels != null ? (levels[i] < 0 ? "L –" : "L" + levels[i]) : n.type.caption;
            if (pathNodes != null && pathNodes[i]) g.state = CellState.ACTIVE;
            if (cut != null) g.state = cut.sourceSide()[i] ? CellState.DONE : CellState.NORMAL;
            if (n.id == m.source()) {
                g.ring = CellState.MATCH;
                g.sub = (levels == null ? "SOURCE" : g.sub + " · SOURCE");
            }
            if (n.id == m.sink()) {
                g.ring = CellState.MISMATCH;
                g.sub = (levels == null ? "SINK" : g.sub + " · SINK");
            }
            if (pendingFrom != null && pendingFrom.id == n.id) g.ring = CellState.HIGHLIGHT;
            gn.add(g);
        }
        boolean residual = residualBox.isSelected();
        List<GEdge> ge = new ArrayList<>();
        for (int k = 0; k < m.edges().size(); k++) {
            int[] e = m.edges().get(k);
            int cap = e[2];
            int flow = flows[k];
            GEdge g = new GEdge(e[0], e[1]);
            g.label = residual ? String.valueOf(cap - flow) : (animating ? flow + "/" + cap : String.valueOf(cap));
            g.state = flow == 0 ? CellState.NORMAL : flow == cap ? CellState.WINDOW : CellState.DONE;
            if (pathEdges != null && pathEdges[k]) {
                g.state = CellState.ACTIVE;
                g.width = 4;
            }
            if (cut != null && cut.cutEdges().contains(k)) {
                g.state = CellState.MISMATCH;
                g.width = 4.5;
                g.label = g.label + " ✂";
            }
            if (levels != null && levelGraph) {
                int a = m.indexOf(e[0]);
                int b = m.indexOf(e[1]);
                if (cap - flow > 0 && levels[a] >= 0 && levels[b] == levels[a] + 1) {
                    g.state = CellState.WINDOW;
                    g.width = 3.4;
                }
            }
            ge.add(g);
            if (residual && flow > 0) { // back-edge of the residual graph
                GEdge back = new GEdge(e[1], e[0]);
                back.label = String.valueOf(flow);
                back.dashed = true;
                back.state = CellState.HIGHLIGHT;
                ge.add(back);
            }
        }
        graph.render(gn, ge);
    }

    // ------------------------------------------------------------------ algorithm execution

    @Override
    protected void loadDemo() {
        loadClassic();
    }

    @Override
    protected Supplier<Execution> prepare() throws ValidationException {
        if (nodes.size() < 2) {
            throw new ValidationException("Add at least two nodes (a source and a sink) to the network first.");
        }
        if (source < 0 || sink < 0) {
            throw new ValidationException("Please choose both a source (◉ Set source) and a sink (◎ Set sink).");
        }
        if (edges.isEmpty()) {
            throw new ValidationException("The network has no edges. Use ➜ Add edge to connect nodes.");
        }
        Model model = snapshot();
        String name = algorithm.getValue() == null ? DINIC : algorithm.getValue();
        boolean cutWanted = cutBox.isSelected() || CUT.equals(name);
        return () -> execute(model, name, cutWanted);
    }

    private Execution execute(Model m, String name, boolean cutWanted) {
        FlowNetwork net = new FlowNetwork(m.nodes().size());
        for (int[] e : m.edges()) {
            net.addEdge(m.indexOf(e[0]), m.indexOf(e[1]), e[2]);
        }
        int s = m.indexOf(m.source());
        int t = m.indexOf(m.sink());
        long t0 = System.nanoTime();
        FlowResult ff = FordFulkerson.maxFlow(net, s, t);
        long t1 = System.nanoTime();
        FlowResult ek = EdmondsKarp.maxFlow(net, s, t);
        long t2 = System.nanoTime();
        FlowResult dn = Dinic.maxFlow(net, s, t);
        long t3 = System.nanoTime();
        FlowResult chosen = switch (name) {
            case FF -> ff;
            case EK -> ek;
            default -> dn;
        };
        MinCut.Result cut = MinCut.compute(chosen.network(), s);
        StringBuilder result = new StringBuilder("Maximum flow = " + chosen.maxFlow() + " units");
        result.append(String.format(" via %d augmenting path(s)", chosen.augmentations()));
        if (chosen.phases() > 0) result.append(" in ").append(chosen.phases()).append(" phase(s)");
        StringBuilder cutText = new StringBuilder();
        for (int k : cut.cutEdges()) {
            int[] e = m.edges().get(k);
            cutText.append(m.nodes().get(m.indexOf(e[0])).label).append("→").append(m.nodes().get(m.indexOf(e[1])).label)
                    .append(" (").append(e[2]).append(")  ");
        }
        result.append("\nMinimum cut / bottleneck capacity = ").append(cut.capacity()).append(cutText.length() == 0 ? " (no s–t path exists)" : ": " + cutText.toString().trim());
        String complexity = switch (name) {
            case FF -> "Ford-Fulkerson O(E·|f*|) – pseudo-polynomial";
            case EK -> "Edmonds-Karp O(V·E²) – independent of capacities";
            case CUT -> "Min-cut = one BFS on the residual graph after max flow: O(V + E)";
            default -> "Dinic O(V²·E)  (O(E·√V) for unit capacities)";
        };
        String info = String.format("Comparison on this network (edge scans / augmentations / measured time): Ford-Fulkerson %d / %d / %.3f ms | Edmonds-Karp %d / %d / %.3f ms | Dinic %d / %d (%d phases) / %.3f ms",
                ff.edgeScans(), ff.augmentations(), (t1 - t0) / 1e6, ek.edgeScans(), ek.augmentations(), (t2 - t1) / 1e6,
                dn.edgeScans(), dn.augmentations(), dn.phases(), (t3 - t2) / 1e6);
        int frames = chosen.steps().size() + 2;
        return new Execution(frames, f -> drawFrame(m, chosen, cut, cutWanted, f), f -> narrate(m, chosen, cut, cutWanted, f),
                result.toString(), complexity, chosen.edgeScans(), chosen.steps().size(), info);
    }

    private void drawFrame(Model m, FlowResult r, MinCut.Result cut, boolean cutWanted, int frame) {
        int steps = r.steps().size();
        int edgeCount = m.edges().size();
        int[] flows = new int[edgeCount];
        boolean[] pathEdges = null;
        boolean[] pathNodes = null;
        int[] levels = null;
        boolean finalFrame = frame > steps;
        if (frame >= 1 && frame <= steps) {
            FlowStep step = r.steps().get(frame - 1);
            flows = step.flows();
            if (step.kind() == FlowStep.Kind.AUGMENT) {
                pathEdges = new boolean[edgeCount];
                pathNodes = new boolean[m.nodes().size()];
                for (int raw : step.pathEdges()) pathEdges[raw >> 1] = true;
                for (int node : step.pathNodes()) pathNodes[node] = true;
            } else {
                levels = step.levels();
            }
        } else if (finalFrame) {
            flows = r.network().flows();
        }
        final MinCut.Result cutToShow = finalFrame && cutWanted ? cut : null;
        final int[] fl = flows;
        final boolean[] pe = pathEdges;
        final boolean[] pn = pathNodes;
        final int[] lv = levels;
        redraw = () -> {
            animating = true;
            paint(m, fl, pe, pn, lv, cutToShow, lv != null);
        };
        redraw.run();
    }

    private static String narrate(Model m, FlowResult r, MinCut.Result cut, boolean cutWanted, int frame) {
        int steps = r.steps().size();
        String s = m.nodes().get(m.indexOf(m.source())).label;
        String t = m.nodes().get(m.indexOf(m.sink())).label;
        if (frame == 0) {
            return "Start: all flows are 0. Source " + s + ", sink " + t + ". Edge labels show the capacity of each lane. Algorithm: " + r.algorithm() + ".";
        }
        if (frame > steps) {
            return "Finished: no augmenting path remains, so the flow " + r.maxFlow() + " is maximum. "
                    + (cutWanted ? "The red ✂ edges form the minimum cut with capacity " + cut.capacity() + " – the network bottleneck (source side in green)." : "");
        }
        FlowStep st = r.steps().get(frame - 1);
        if (st.kind() == FlowStep.Kind.PHASE) {
            return "Phase " + st.phase() + ": BFS assigns a level to every node (shown under the nodes). Only lanes that go from level i to i+1 and still "
                    + "have spare capacity (amber) belong to the level graph; the blocking flow will saturate them. Flow so far: " + st.totalFlow() + ".";
        }
        StringBuilder path = new StringBuilder();
        for (int node : st.pathNodes()) {
            path.append(path.length() == 0 ? "" : " → ").append(m.nodes().get(node).label);
        }
        boolean usesBack = Arrays.stream(st.pathEdges()).anyMatch(e -> (e & 1) == 1);
        String prefix = r.phases() > 0 ? "Phase " + st.phase() + " – " : "";
        return prefix + "Augmenting path " + path + ": bottleneck (smallest residual capacity) = " + st.bottleneck()
                + (usesBack ? " (uses a residual back-edge, i.e. it re-routes earlier flow)" : "") + ". Flow added: " + st.bottleneck()
                + ", current max flow: " + st.totalFlow() + ".";
    }
}
