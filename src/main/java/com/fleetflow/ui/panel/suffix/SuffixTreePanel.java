package com.fleetflow.ui.panel.suffix;

import com.fleetflow.datastructure.suffix.SuffixTree;
import com.fleetflow.ui.AlgorithmPanel;
import com.fleetflow.ui.Execution;
import com.fleetflow.ui.Ui;
import com.fleetflow.util.ValidationException;
import com.fleetflow.visualization.CellState;
import com.fleetflow.visualization.TreeVisualizer;
import com.fleetflow.visualization.TreeVisualizer.VNode;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/** Suffix Tree page: animates inserting suffixes one by one, then walks a substring query down the tree. */
public final class SuffixTreePanel extends AlgorithmPanel {

    private static final int MAX_TEXT = 24;

    private final DocumentInput document = new DocumentInput(MAX_TEXT);
    private final TextField queryField = new TextField();
    private final TreeVisualizer tree = new TreeVisualizer(112);

    public SuffixTreePanel() {
        queryField.setPromptText("Substring to search");
        HBox query = new HBox(8, Ui.label("Query", "kv-key"), queryField);
        HBox.setHgrow(queryField, Priority.ALWAYS);
        tree.setPrefViewportHeight(400);
        assemble("Suffix Tree",
                "Real problem: answer substring queries on a shipment text in O(m). Every suffix ends in its own leaf (numbered by start position); "
                        + "edges carry substrings, so the pattern is matched by walking down from the root. "
                        + "Texts are limited to " + MAX_TEXT + " characters so the tree stays readable; '$' is the terminator.",
                new VBox(8, document, query), tree);
        loadDemo();
    }

    @Override
    protected void loadDemo() {
        document.setText("SHP-01 SHP-02");
        queryField.setText("SHP-0");
    }

    @Override
    protected Supplier<Execution> prepare() throws ValidationException {
        String text = document.read();
        String query = queryField.getText() == null ? "" : queryField.getText();
        return () -> execute(text, query);
    }

    private Execution execute(String text, String query) {
        int suffixes = text.length() + 1;
        SuffixTree full = new SuffixTree(text);
        SuffixTree.Query q = query.isEmpty() ? null : full.search(query);
        int frames = suffixes + (q == null ? 0 : 1);
        String result = "Suffix tree with " + full.nodeCount() + " nodes for \"" + text + "$\"."
                + (q == null ? "" : q.found() ? "\n\"" + query + "\" occurs " + q.positions().size() + " time(s) at positions " + q.positions()
                : "\n\"" + query + "\" does not occur in the text.");
        String complexity = "Construction O(n²) here (Ukkonen: O(n))   |   Query O(m) = O(" + query.length() + ")   |   Space O(n) nodes";
        String info = "The query walked " + (q == null ? 0 : q.path().size() - 1) + " edge(s) using " + (q == null ? 0 : q.comparisons()) + " character comparisons.";
        return new Execution(frames, f -> draw(text, full, q, f), f -> narrate(text, q, f), result, complexity,
                q == null ? -1 : q.comparisons(), frames, info);
    }

    private void draw(String text, SuffixTree full, SuffixTree.Query q, int frame) {
        int suffixes = text.length() + 1;
        boolean queryFrame = frame >= suffixes;
        SuffixTree shown = queryFrame ? full : new SuffixTree(text, frame + 1);
        Map<Integer, CellState> states = new HashMap<>();
        if (queryFrame && q != null) {
            for (int id : q.path()) {
                states.put(id, q.found() ? CellState.MATCH : CellState.MISMATCH);
            }
        }
        tree.render(convert(shown, shown.root(), "", states, queryFrame ? -1 : frame), states, List.of());
    }

    private static VNode convert(SuffixTree t, SuffixTree.Node node, String label, Map<Integer, CellState> states, int newest) {
        VNode v = new VNode(node.id(), node.isLeaf() ? "L" + node.suffixIndex() : node.id() == 0 ? "root" : "•", label, node.isLeaf());
        if (node.isLeaf() && node.suffixIndex() == newest) {
            states.put(node.id(), CellState.ACTIVE);
        }
        for (SuffixTree.Node child : node.children().values()) {
            v.add(convert(t, child, t.edgeLabel(child), states, newest));
        }
        return v;
    }

    private static String narrate(String text, SuffixTree.Query q, int frame) {
        int suffixes = text.length() + 1;
        if (frame < suffixes) {
            String suffix = (text + "$").substring(frame);
            return "Insert suffix " + frame + " = \"" + suffix + "\": follow existing edges as far as they match, split an edge where they diverge, and attach a new leaf L" + frame + ".";
        }
        return q.found() ? "Query matched by walking " + (q.path().size() - 1) + " edge(s) from the root. Leaves below the last node give all occurrences: " + q.positions() + "."
                : "The walk stopped because no edge continues with the next pattern character: the substring does not occur.";
    }
}
