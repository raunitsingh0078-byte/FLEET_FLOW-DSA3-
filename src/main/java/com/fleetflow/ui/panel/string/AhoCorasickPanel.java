package com.fleetflow.ui.panel.string;

import com.fleetflow.datastructure.trie.AhoCorasickAutomaton;
import com.fleetflow.ui.AlgorithmPanel;
import com.fleetflow.ui.Execution;
import com.fleetflow.ui.Ui;
import com.fleetflow.util.ValidationException;
import com.fleetflow.visualization.ArrayVisualizer;
import com.fleetflow.visualization.ArrayVisualizer.Cell;
import com.fleetflow.visualization.CellState;
import com.fleetflow.visualization.TreeVisualizer;
import com.fleetflow.visualization.TreeVisualizer.VNode;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/** Aho-Corasick page: builds a trie with failure links and scans the text for many patterns at once. */
public final class AhoCorasickPanel extends AlgorithmPanel {

    private static final int MAX_PATTERNS = 8;
    private static final int MAX_PATTERN_LENGTH = 12;

    private final TextField patternsField = new TextField();
    private final TextField textField = new TextField();
    private final CheckBox showLinks = new CheckBox("Show failure links (dashed)");
    private final TreeVisualizer trie = new TreeVisualizer(46);
    private final ArrayVisualizer strip = new ArrayVisualizer();

    public AhoCorasickPanel() {
        patternsField.setPromptText("Comma-separated keywords, e.g. delay, delayed, hub");
        textField.setPromptText("Text to scan");
        showLinks.setSelected(true);
        showLinks.setOnAction(e -> refreshVisualization());
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.add(Ui.label("Patterns", "kv-key"), 0, 0);
        grid.add(patternsField, 1, 0);
        grid.add(Ui.label("Text", "kv-key"), 0, 1);
        grid.add(textField, 1, 1);
        grid.add(showLinks, 1, 2);
        GridPane.setHgrow(patternsField, Priority.ALWAYS);
        GridPane.setHgrow(textField, Priority.ALWAYS);
        trie.setPrefViewportHeight(390);
        VBox visual = new VBox(10, trie, strip);
        assemble("Aho-Corasick",
                "Real problem: scan shipment status logs for many delivery-status keywords (delay, hub, delivered…) in ONE pass. "
                        + "All keywords are inserted into a trie; a BFS then adds failure links (dashed) that point to the longest proper "
                        + "suffix that is also a trie prefix, so the scan never moves backwards in the text. Case-sensitive.",
                grid, visual);
        loadDemo();
    }

    @Override
    protected void loadDemo() {
        patternsField.setText("delay, delayed, hub, deliver, live");
        textField.setText("shipment delayed at hub, delivered late");
    }

    @Override
    protected Supplier<Execution> prepare() throws ValidationException {
        String text = StringInputs.requireText(textField.getText(), "text");
        String raw = patternsField.getText() == null ? "" : patternsField.getText();
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        for (String part : raw.split(",")) {
            String p = part.trim();
            if (!p.isEmpty()) {
                unique.add(p);
            }
        }
        if (unique.isEmpty()) {
            throw new ValidationException("Please enter at least one pattern (separate several with commas).");
        }
        if (unique.size() > MAX_PATTERNS) {
            throw new ValidationException("Please use at most " + MAX_PATTERNS + " patterns so the trie stays readable.");
        }
        for (String p : unique) {
            if (p.length() > MAX_PATTERN_LENGTH) {
                throw new ValidationException("Pattern \"" + p + "\" is longer than " + MAX_PATTERN_LENGTH + " characters.");
            }
            if (p.indexOf('\u0000') >= 0) {
                throw new ValidationException("Patterns must not contain control characters.");
            }
        }
        List<String> patterns = new ArrayList<>(unique);
        return () -> execute(patterns, text);
    }

    private Execution execute(List<String> patterns, String text) {
        AhoCorasickAutomaton ac = new AhoCorasickAutomaton(patterns);
        AhoCorasickAutomaton.Result r = ac.search(text);
        Map<Integer, String> spelling = new HashMap<>();
        VNode root = buildTree(ac.root(), "", spelling);
        List<int[]> links = new ArrayList<>();
        for (AhoCorasickAutomaton.Node node : ac.nodes()) {
            if (node != ac.root() && node.fail() != ac.root()) {
                links.add(new int[]{node.id(), node.fail().id()});
            }
        }
        StringBuilder found = new StringBuilder();
        for (AhoCorasickAutomaton.Match m : r.matches()) {
            found.append("\"").append(m.pattern()).append("\" at ").append(m.start()).append("–").append(m.end()).append("   ");
        }
        String result = r.matches().isEmpty() ? "No pattern found." : r.matches().size() + " match(es): " + found.toString().trim();
        int total = patterns.stream().mapToInt(String::length).sum();
        String complexity = "Time O(n + m + z) with n=" + text.length() + ", m=" + total + " (sum of pattern lengths), z="
                + r.matches().size() + " matches   |   Space O(m · alphabet)";
        String info = "Trie has " + ac.nodes().size() + " states; " + r.transitions() + " automaton transitions for "
                + text.length() + " text characters (each failure-link hop is one extra transition).";
        return new Execution(r.steps().size(), f -> draw(ac, r, root, links, text, f),
                f -> narrate(r, spelling, f), result, complexity, r.transitions(), r.steps().size(), info);
    }

    private static VNode buildTree(AhoCorasickAutomaton.Node node, String prefix, Map<Integer, String> spelling) {
        String spell = node.depth() == 0 ? "" : prefix + node.character();
        spelling.put(node.id(), spell);
        VNode v = new VNode(node.id(), node.depth() == 0 ? "root" : String.valueOf(node.character()), null,
                node.patternEnd() != null);
        for (AhoCorasickAutomaton.Node child : node.children().values()) {
            v.add(buildTree(child, spell, spelling));
        }
        return v;
    }

    private void draw(AhoCorasickAutomaton ac, AhoCorasickAutomaton.Result r, VNode root, List<int[]> links,
                      String text, int frame) {
        AhoCorasickAutomaton.Step step = r.steps().get(frame);
        Map<Integer, CellState> states = new HashMap<>();
        boolean[] matchedChars = new boolean[text.length()];
        for (AhoCorasickAutomaton.Match m : r.matches()) {
            if (m.end() <= frame) {
                for (int i = m.start(); i <= m.end(); i++) {
                    matchedChars[i] = true;
                }
                states.put(ac.nodeFor(m.pattern()).id(), CellState.DONE);
            }
        }
        states.put(step.fromState(), CellState.HIGHLIGHT);
        states.put(step.toState(), CellState.ACTIVE);
        for (String p : step.matched()) {
            states.put(ac.nodeFor(p).id(), CellState.MATCH);
        }
        trie.render(root, states, showLinks.isSelected() ? links : List.of());

        int n = text.length();
        Cell[] index = new Cell[n];
        Cell[] chars = new Cell[n];
        Cell[] stateRow = new Cell[n];
        for (int i = 0; i < n; i++) {
            index[i] = ArrayVisualizer.cell(String.valueOf(i), CellState.MUTED);
            CellState cs = matchedChars[i] ? CellState.MATCH : CellState.NORMAL;
            if (i == frame) cs = CellState.ACTIVE;
            chars[i] = ArrayVisualizer.cell(String.valueOf(text.charAt(i)), cs);
            stateRow[i] = i <= frame ? ArrayVisualizer.cell(String.valueOf(r.steps().get(i).toState()),
                    i == frame ? CellState.ACTIVE : CellState.DONE) : null;
        }
        strip.render(List.of("Index", "Text", "Automaton state"), Arrays.asList(index, chars, stateRow), frame);
    }

    private static String narrate(AhoCorasickAutomaton.Result r, Map<Integer, String> spelling, int frame) {
        AhoCorasickAutomaton.Step s = r.steps().get(frame);
        StringBuilder sb = new StringBuilder("Read text[" + s.textIndex() + "] = '" + s.character() + "' in state "
                + s.fromState() + " (\"" + spelling.get(s.fromState()) + "\"). ");
        if (s.failHops() > 0) {
            sb.append("No matching edge, so follow ").append(s.failHops()).append(" failure link(s) first. ");
        }
        sb.append("Now in state ").append(s.toState()).append(" (\"").append(spelling.get(s.toState())).append("\").");
        if (!s.matched().isEmpty()) {
            sb.append(" MATCH: ").append(String.join(", ", s.matched())).append(" ends here.");
        }
        return sb.toString();
    }
}
