package com.fleetflow.ui.panel.suffix;

import com.fleetflow.algorithm.suffix.SuffixArray;
import com.fleetflow.ui.AlgorithmPanel;
import com.fleetflow.ui.Execution;
import com.fleetflow.ui.Ui;
import com.fleetflow.util.ValidationException;
import com.fleetflow.visualization.CellState;
import com.fleetflow.visualization.RowTableVisualizer;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/** Suffix Array page: builds and displays the sorted suffixes, then answers a query by binary search. */
public final class SuffixArrayPanel extends AlgorithmPanel {

    private final DocumentInput document = new DocumentInput(200);
    private final TextField queryField = new TextField();
    private final RowTableVisualizer table = new RowTableVisualizer("Rank", "SA[rank]", "Suffix");

    public SuffixArrayPanel() {
        queryField.setPromptText("Substring to search, e.g. Bengaluru");
        HBox query = new HBox(8, Ui.label("Query", "kv-key"), queryField);
        HBox.setHgrow(queryField, Priority.ALWAYS);
        assemble("Suffix Array",
                "Real problem: index shipment descriptions and delivery documents so any substring can be found without scanning. "
                        + "All suffixes are sorted; a substring is then located with two binary searches over the sorted suffixes.",
                new VBox(8, document, query), table);
        loadDemo();
    }

    @Override
    protected void loadDemo() {
        document.setText("42 MG Road, Indiranagar, Bengaluru 560038; 17 Linking Road, Bandra West, Mumbai; 42 MG Road, Indiranagar");
        queryField.setText("Indiranagar");
    }

    @Override
    protected Supplier<Execution> prepare() throws ValidationException {
        String text = document.read();
        String query = queryField.getText() == null ? "" : queryField.getText();
        return () -> execute(text, query);
    }

    private Execution execute(String text, String query) {
        int n = text.length();
        int[] sa = SuffixArray.build(text);
        SuffixArray.SearchResult found = query.isEmpty() ? null : SuffixArray.search(text, sa, query);
        int probes = found == null ? 0 : found.probes().size();
        int frames = n + probes + (found == null ? 0 : 1);

        String result = "Suffix array built for " + n + " suffixes."
                + (found == null ? "" : found.count() == 0 ? "\n\"" + query + "\" does not occur."
                : "\n\"" + query + "\" occurs " + found.count() + " time(s) at positions " + positionsOf(sa, found));
        String complexity = "Build (sort-based) O(n² log n) worst case, O(n log n) typical   |   Query O(m log n) = O(" + query.length()
                + " · log " + n + ")   |   Space O(n)";
        String info = "Suffixes are compared lexicographically; the shorter of two equal-prefix suffixes sorts first.";
        return new Execution(frames, f -> draw(text, sa, found, f), f -> narrate(text, sa, found, n, f), result, complexity,
                found == null ? -1 : found.comparisons(), frames, info);
    }

    private static List<Integer> positionsOf(int[] sa, SuffixArray.SearchResult r) {
        List<Integer> list = new ArrayList<>();
        for (int i = r.first(); i <= r.last(); i++) {
            list.add(sa[i]);
        }
        list.sort(Integer::compare);
        return list;
    }

    private void draw(String text, int[] sa, SuffixArray.SearchResult found, int frame) {
        int n = text.length();
        int revealed = Math.min(frame, n - 1);
        List<String[]> rows = new ArrayList<>();
        for (int r = 0; r <= revealed; r++) {
            String suffix = text.substring(sa[r]);
            rows.add(new String[]{String.valueOf(r), String.valueOf(sa[r]), suffix.length() > 60 ? suffix.substring(0, 60) + "…" : suffix});
        }
        Map<Integer, CellState> states = new HashMap<>();
        if (frame < n) {
            states.put(revealed, CellState.ACTIVE);
        } else if (found != null) {
            int probeIndex = frame - n;
            if (probeIndex < found.probes().size()) {
                SuffixArray.Probe p = found.probes().get(probeIndex);
                for (int r = p.lo(); r <= p.hi() && r < rows.size(); r++) {
                    states.put(r, CellState.WINDOW);
                }
                states.put(p.mid(), CellState.ACTIVE);
            } else {
                for (int r = found.first(); r <= found.last(); r++) {
                    states.put(r, CellState.MATCH);
                }
            }
        }
        table.show(rows, states);
        if (!rows.isEmpty()) {
            table.scrollTo(Math.max(0, (frame < n ? revealed : (frame - n < found.probes().size() ? found.probes().get(frame - n).mid() : Math.max(0, found.first()))) - 3));
        }
    }

    private static String narrate(String text, int[] sa, SuffixArray.SearchResult found, int n, int frame) {
        if (frame < n) {
            return "Rank " + frame + " of the sorted order is the suffix starting at position " + sa[frame]
                    + " (" + Arrays.toString(new int[]{sa[frame]}) + "). Suffix array entry SA[" + frame + "] = " + sa[frame] + ".";
        }
        int i = frame - n;
        if (i < found.probes().size()) {
            SuffixArray.Probe p = found.probes().get(i);
            String side = p.comparison() > 0 ? "pattern is greater than this suffix → search the upper half"
                    : p.comparison() == 0 ? "suffix starts with the pattern → keep looking for the boundary"
                    : "pattern is smaller → search the lower half";
            return "Binary search probe: rank " + p.mid() + " (window " + p.lo() + ".." + p.hi() + "): " + side + ".";
        }
        return found.count() == 0 ? "Search finished: no suffix starts with the pattern."
                : "Search finished: ranks " + found.first() + ".." + found.last() + " share the pattern as prefix.";
    }
}
