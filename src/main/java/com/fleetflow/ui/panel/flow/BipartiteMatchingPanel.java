package com.fleetflow.ui.panel.flow;

import com.fleetflow.algorithm.flow.BipartiteMatching;
import com.fleetflow.algorithm.flow.Dinic;
import com.fleetflow.data.FleetRepository;
import com.fleetflow.datastructure.graph.FlowNetwork;
import com.fleetflow.model.Driver;
import com.fleetflow.model.Priority;
import com.fleetflow.model.Shipment;
import com.fleetflow.model.ShipmentStatus;
import com.fleetflow.ui.AlgorithmPanel;
import com.fleetflow.ui.Execution;
import com.fleetflow.util.ThemeManager;
import com.fleetflow.util.ValidationException;
import com.fleetflow.visualization.CellState;
import com.fleetflow.visualization.GraphLayouts;
import com.fleetflow.visualization.GraphVisualizer;
import com.fleetflow.visualization.GraphVisualizer.GEdge;
import com.fleetflow.visualization.GraphVisualizer.GNode;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/** Bipartite-matching page: assigns drivers (left) to delivery tasks (right) with a maximum matching. */
public final class BipartiteMatchingPanel extends AlgorithmPanel {

    private static final double W = 900;
    private static final double H = 400;
    private static final int MAX_DRIVERS = 8;
    private static final int MAX_TASKS = 10;

    private final TextArea input = new TextArea();
    private final GraphVisualizer graph = new GraphVisualizer(W, H);
    private Runnable redraw = () -> { };

    public BipartiteMatchingPanel() {
        input.setPrefRowCount(7);
        input.setPromptText("One driver per line:  Driver name: task1, task2, …   (tasks the driver is able to perform)");
        ScrollPane scroll = new ScrollPane(graph);
        scroll.getStyleClass().add("viz-scroll");
        scroll.setPrefViewportHeight(H + 8);
        assemble("Bipartite Matching – Driver ↔ Delivery Task Assignment",
                "Real problem: assign each delivery task to a qualified driver so that as many tasks as possible are served and nobody gets two. "
                        + "A driver–task edge means 'this driver may take this task' (from experience vs. priority). The algorithm searches an augmenting path for each "
                        + "driver; if the wanted task is taken, its owner is asked to move to another task (re-assignment). This is max-flow on a unit-capacity network.",
                input, scroll);
        ThemeManager.addListener(() -> redraw.run());
        loadDemo();
    }

    @Override
    protected void loadDemo() {
        FleetRepository repo = FleetRepository.get();
        List<Driver> drivers = repo.drivers().stream()
                .sorted(Comparator.comparingInt(Driver::assignedDeliveries).thenComparing(Driver::id)).limit(6).toList();
        List<Shipment> tasks = new ArrayList<>(repo.shipments().stream().filter(s -> s.status() == ShipmentStatus.PENDING).limit(7).toList());
        if (tasks.size() < 3) {
            tasks = new ArrayList<>(repo.shipments().stream().limit(7).toList());
        }
        StringBuilder sb = new StringBuilder();
        for (Driver d : drivers) {
            List<String> able = new ArrayList<>();
            for (Shipment s : tasks) {
                if (d.experienceYears() >= requiredExperience(s.priority()) && (s.priority() != Priority.URGENT || d.rating() >= 4.0)) {
                    able.add(s.id());
                }
            }
            sb.append(d.name()).append(": ").append(String.join(", ", able)).append('\n');
        }
        input.setText(sb.toString().trim());
        renderStatic();
    }

    /** Minimum driver experience (years) required for a shipment priority. */
    private static int requiredExperience(Priority p) {
        return switch (p) {
            case LOW -> 1;
            case NORMAL -> 3;
            case HIGH -> 6;
            case URGENT -> 9;
        };
    }

    private record Problem(List<String> drivers, List<String> tasks, List<List<Integer>> adjacency) {
    }

    private Problem parse() throws ValidationException {
        List<String> drivers = new ArrayList<>();
        Map<String, Integer> taskIndex = new LinkedHashMap<>();
        List<List<String>> raw = new ArrayList<>();
        int lineNo = 0;
        for (String line : (input.getText() == null ? "" : input.getText()).split("\\R")) {
            lineNo++;
            if (line.isBlank()) continue;
            int colon = line.indexOf(':');
            if (colon <= 0) {
                throw new ValidationException("Line " + lineNo + " must look like  Driver name: task1, task2");
            }
            drivers.add(line.substring(0, colon).trim());
            List<String> tasks = new ArrayList<>();
            for (String t : line.substring(colon + 1).split(",")) {
                if (!t.isBlank()) {
                    tasks.add(t.trim());
                    taskIndex.putIfAbsent(t.trim(), taskIndex.size());
                }
            }
            raw.add(tasks);
        }
        if (drivers.isEmpty() || drivers.size() > MAX_DRIVERS) {
            throw new ValidationException("Please enter between 1 and " + MAX_DRIVERS + " drivers (one per line).");
        }
        if (taskIndex.isEmpty() || taskIndex.size() > MAX_TASKS) {
            throw new ValidationException("Please use between 1 and " + MAX_TASKS + " different tasks.");
        }
        List<List<Integer>> adjacency = new ArrayList<>();
        for (List<String> tasks : raw) {
            List<Integer> list = new ArrayList<>();
            for (String t : tasks) {
                if (!list.contains(taskIndex.get(t))) list.add(taskIndex.get(t));
            }
            adjacency.add(list);
        }
        return new Problem(drivers, new ArrayList<>(taskIndex.keySet()), adjacency);
    }

    @Override
    protected Supplier<Execution> prepare() throws ValidationException {
        Problem p = parse();
        return () -> execute(p);
    }

    private Execution execute(Problem p) {
        int l = p.drivers().size();
        int r = p.tasks().size();
        BipartiteMatching.Result res = BipartiteMatching.maximumMatching(l, r, p.adjacency());
        FlowNetwork net = new FlowNetwork(l + r + 2);
        int s = l + r;
        int t = s + 1;
        for (int u = 0; u < l; u++) {
            net.addEdge(s, u, 1);
            for (int v : p.adjacency().get(u)) net.addEdge(u, l + v, 1);
        }
        for (int v = 0; v < r; v++) net.addEdge(l + v, t, 1);
        int flowCheck = Dinic.maxFlow(net, s, t).maxFlow();

        StringBuilder sb = new StringBuilder("Matching size " + res.size() + " of " + l + " drivers / " + r + " tasks.");
        for (int u = 0; u < l; u++) {
            sb.append("\n  ").append(p.drivers().get(u)).append(res.matchOfLeft()[u] >= 0 ? " → " + p.tasks().get(res.matchOfLeft()[u]) : " → (no task)");
        }
        List<String> free = new ArrayList<>();
        for (int v = 0; v < r; v++) if (res.matchOfRight()[v] < 0) free.add(p.tasks().get(v));
        sb.append("\nUnassigned tasks: ").append(free.isEmpty() ? "none" : String.join(", ", free));
        String complexity = "Time O(V·E) = O(" + (l + r) + " · " + p.adjacency().stream().mapToInt(List::size).sum() + ")   |   Space O(V)";
        String info = "Cross-check with Dinic on the equivalent unit-capacity flow network: max flow = " + flowCheck
                + (flowCheck == res.size() ? " ✓ identical to the matching size." : " ✗ MISMATCH");
        int frames = res.steps().size() + 2;
        return new Execution(frames, f -> drawFrame(p, res, f), f -> narrate(p, res, f), sb.toString(), complexity, res.attempts(),
                res.steps().size(), info);
    }

    private void renderStatic() {
        try {
            Problem p = parse();
            redraw = () -> paint(p, new int[p.drivers().size()], -1, -1, -1, false);
        } catch (ValidationException e) {
            redraw = () -> graph.render(new ArrayList<>(), new ArrayList<>());
        }
        redraw.run();
    }

    private void drawFrame(Problem p, BipartiteMatching.Result res, int frame) {
        int steps = res.steps().size();
        int[] match = new int[p.drivers().size()];
        java.util.Arrays.fill(match, -1);
        int left = -1;
        int right = -1;
        int currentStepKind = -1;
        boolean done = frame > steps;
        if (frame >= 1 && frame <= steps) {
            BipartiteMatching.Step st = res.steps().get(frame - 1);
            match = st.matchOfLeft();
            left = st.left();
            right = st.right();
            currentStepKind = st.kind().ordinal();
        } else if (done) {
            match = res.matchOfLeft();
        }
        final int[] m = match;
        final int fl = left;
        final int fr = right;
        final int fk = currentStepKind;
        redraw = () -> paint(p, m, fl, fr, fk, done);
        redraw.run();
    }

    private void paint(Problem p, int[] match, int left, int right, int kind, boolean done) {
        int l = p.drivers().size();
        int r = p.tasks().size();
        double[][] pos = GraphLayouts.bipartite(l, r, W, H);
        List<GNode> nodes = new ArrayList<>();
        boolean[] taskTaken = new boolean[r];
        for (int m : match) if (m >= 0) taskTaken[m] = true;
        for (int u = 0; u < l; u++) {
            GNode n = new GNode(u, "D" + (u + 1), pos[u][0], pos[u][1]);
            n.sub = p.drivers().get(u);
            n.state = u == left && !done ? CellState.ACTIVE : match[u] >= 0 ? CellState.MATCH : done ? CellState.MISMATCH : CellState.NORMAL;
            nodes.add(n);
        }
        for (int v = 0; v < r; v++) {
            GNode n = new GNode(l + v, "T" + (v + 1), pos[l + v][0], pos[l + v][1]);
            n.sub = p.tasks().get(v);
            n.state = v == right && !done ? CellState.ACTIVE : taskTaken[v] ? CellState.MATCH : done ? CellState.MISMATCH : CellState.NORMAL;
            nodes.add(n);
        }
        List<GEdge> edges = new ArrayList<>();
        for (int u = 0; u < l; u++) {
            for (int v : p.adjacency().get(u)) {
                GEdge e = new GEdge(u, l + v);
                e.directed = false;
                if (match[u] == v) {
                    e.state = CellState.MATCH;
                    e.width = 4.5;
                } else if (u == left && v == right && !done) {
                    e.state = kind == BipartiteMatching.Kind.FAIL.ordinal() ? CellState.MISMATCH : CellState.ACTIVE;
                    e.width = 3.5;
                }
                edges.add(e);
            }
        }
        graph.render(nodes, edges);
    }

    private static String narrate(Problem p, BipartiteMatching.Result res, int frame) {
        int steps = res.steps().size();
        if (frame == 0) {
            return "Start: no driver has a task. Each driver in turn will look for an augmenting path. Grey lines = 'driver may take this task'.";
        }
        if (frame > steps) {
            return "Done: green edges are the assignment (size " + res.size() + "). Red nodes are drivers without a task or tasks nobody can take – no augmenting path exists for them.";
        }
        BipartiteMatching.Step st = res.steps().get(frame - 1);
        String d = p.drivers().get(st.left());
        return switch (st.kind()) {
            case TRY -> d + " considers task " + p.tasks().get(st.right()) + " (each task is examined at most once per search).";
            case MATCH -> "Task " + p.tasks().get(st.right()) + " is free → assign it to " + d + ".";
            case REASSIGN -> "Task " + p.tasks().get(st.right()) + " was taken, but its owner could move to another task along an augmenting path → it now belongs to " + d
                    + ". The matching grew by one.";
            case FAIL -> "No augmenting path exists for " + d + ": every reachable task is held by a driver who cannot move. " + d + " stays unassigned.";
        };
    }
}
