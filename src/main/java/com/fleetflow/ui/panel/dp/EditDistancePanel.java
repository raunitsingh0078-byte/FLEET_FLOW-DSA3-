package com.fleetflow.ui.panel.dp;

import com.fleetflow.algorithm.dp.EditDistance;
import com.fleetflow.data.FleetRepository;
import com.fleetflow.model.Shipment;
import com.fleetflow.ui.AlgorithmPanel;
import com.fleetflow.ui.Execution;
import com.fleetflow.ui.Ui;
import com.fleetflow.util.ValidationException;
import com.fleetflow.visualization.CellState;
import com.fleetflow.visualization.MatrixVisualizer;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Levenshtein / Damerau page: corrects a misspelt delivery address using the sample address database. */
public final class EditDistancePanel extends AlgorithmPanel {

    private static final Pattern WORD = Pattern.compile("[A-Za-z]{3,}");
    private static final int MAX_WORD = 18;

    private final TextField addressField = new TextField();
    private final CheckBox damerau = new CheckBox("Damerau: count swapping two adjacent letters as one edit");
    private final MatrixVisualizer matrix = new MatrixVisualizer();

    public EditDistancePanel() {
        addressField.setPromptText("Misspelt address, e.g. 42 MG Raod, Indranagar, Bengalru");
        HBox row = new HBox(8, Ui.label("Address", "kv-key"), addressField);
        HBox.setHgrow(addressField, Priority.ALWAYS);
        damerau.setSelected(true);
        matrix.setPrefViewportHeight(330);
        assemble("Levenshtein / Damerau-Levenshtein – Address Correction",
                "Real problem: customers mistype addresses. The edit distance is the minimum number of insertions, deletions, substitutions "
                        + "(and, for Damerau, adjacent swaps) turning one string into another, found with a DP over prefixes. Every word of the address "
                        + "is matched against words of the stored address database; the matrix shows the worst word and its optimal path.",
                new VBox(8, row, damerau), matrix);
        loadDemo();
    }

    @Override
    public void prefer(String algorithm) {
        damerau.setSelected(algorithm.contains("Damerau"));
    }

    @Override
    protected void loadDemo() {
        addressField.setText("42 MG Raod, Indranagar, Bengalru");
        damerau.setSelected(true);
    }

    private static List<String> dictionaryWords() {
        TreeSet<String> words = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (Shipment s : FleetRepository.get().shipments()) {
            Matcher m = WORD.matcher(s.address() + " " + s.destination());
            while (m.find()) {
                words.add(m.group());
            }
        }
        return new ArrayList<>(words);
    }

    @Override
    protected Supplier<Execution> prepare() throws ValidationException {
        String address = addressField.getText() == null ? "" : addressField.getText().trim();
        if (address.isEmpty()) {
            throw new ValidationException("Please enter an address to correct.");
        }
        if (address.length() > 120) {
            throw new ValidationException("The address is longer than 120 characters.");
        }
        boolean allowSwap = damerau.isSelected();
        List<String> dictionary = dictionaryWords();
        List<String> fullAddresses = FleetRepository.get().shipments().stream().map(Shipment::address).distinct().toList();
        if (dictionary.isEmpty()) {
            throw new ValidationException("The address database is empty, so there is nothing to compare with.");
        }
        return () -> execute(address, allowSwap, dictionary, fullAddresses);
    }

    private Execution execute(String address, boolean allowSwap, List<String> dictionary, List<String> fullAddresses) {
        Matcher m = WORD.matcher(address);
        StringBuilder corrected = new StringBuilder();
        int last = 0;
        String worstWord = null;
        String worstMatch = null;
        int worstDistance = -1;
        String firstWord = null;
        String firstMatch = null;
        int comparisons = 0;
        while (m.find()) {
            String word = m.group();
            corrected.append(address, last, m.start());
            last = m.end();
            List<EditDistance.Suggestion> best = EditDistance.suggest(word.toLowerCase(Locale.ROOT),
                    dictionary.stream().map(w -> w.toLowerCase(Locale.ROOT)).toList(), 1, allowSwap);
            comparisons += dictionary.size();
            String match = dictionary.stream().filter(w -> w.equalsIgnoreCase(best.get(0).word())).findFirst().orElse(word);
            int limit = Math.max(2, word.length() / 3);
            boolean accept = best.get(0).distance() <= limit;
            corrected.append(accept ? match : word);
            if (firstWord == null) {
                firstWord = word;
                firstMatch = match;
            }
            if (accept && best.get(0).distance() > worstDistance && best.get(0).distance() > 0 && word.length() <= MAX_WORD) {
                worstDistance = best.get(0).distance();
                worstWord = word;
                worstMatch = match;
            }
        }
        corrected.append(address.substring(last));
        if (worstWord == null) {
            worstWord = firstWord == null ? "address" : firstWord;
            worstMatch = firstWord == null ? "address" : firstMatch;
            worstDistance = 0;
        }
        String a = worstWord.toLowerCase(Locale.ROOT).substring(0, Math.min(MAX_WORD, worstWord.length()));
        String b = worstMatch.toLowerCase(Locale.ROOT).substring(0, Math.min(MAX_WORD, worstMatch.length()));
        EditDistance.Result r = EditDistance.compute(a, b, allowSwap);

        List<EditDistance.Suggestion> top = EditDistance.suggest(address.toLowerCase(Locale.ROOT),
                fullAddresses.stream().map(s -> s.toLowerCase(Locale.ROOT)).toList(), 3, allowSwap);
        StringBuilder result = new StringBuilder("Corrected address: " + corrected);
        result.append("\nVisualised word: \"").append(worstWord).append("\" → \"").append(worstMatch).append("\" (distance ").append(r.distance()).append(")");
        result.append("\nClosest stored addresses:");
        for (EditDistance.Suggestion s : top) {
            String original = fullAddresses.stream().filter(x -> x.equalsIgnoreCase(s.word())).findFirst().orElse(s.word());
            result.append("\n  d=").append(s.distance()).append("  ").append(original);
        }
        int m1 = a.length();
        int n1 = b.length();
        int frames = Math.max(1, m1 * n1) + 1;
        String complexity = "Time O(m·n) per comparison = O(" + m1 + "·" + n1 + ")   |   Space O(m·n) (O(min(m,n)) if the path is not needed)";
        String info = "Compared each address word with " + dictionary.size() + " database words, plus " + fullAddresses.size()
                + " full addresses. The " + (allowSwap ? "Damerau (optimal string alignment)" : "plain Levenshtein") + " variant was used.";
        final String fa = a;
        final String fb = b;
        return new Execution(frames, f -> draw(fa, fb, r, f), f -> narrate(fa, fb, r, f), result.toString(), complexity,
                comparisons, (long) m1 * n1, info);
    }

    private void draw(String a, String b, EditDistance.Result r, int frame) {
        int m = a.length();
        int n = b.length();
        String[] rowH = new String[m + 1];
        String[] colH = new String[n + 1];
        rowH[0] = "ε";
        colH[0] = "ε";
        for (int i = 1; i <= m; i++) rowH[i] = String.valueOf(a.charAt(i - 1));
        for (int j = 1; j <= n; j++) colH[j] = String.valueOf(b.charAt(j - 1));
        String[][] values = new String[m + 1][n + 1];
        CellState[][] states = new CellState[m + 1][n + 1];
        for (int i = 0; i <= m; i++) {
            for (int j = 0; j <= n; j++) {
                int order = i == 0 || j == 0 ? -1 : (i - 1) * n + (j - 1);
                if (order < frame || order == -1) {
                    values[i][j] = String.valueOf(r.matrix()[i][j]);
                    states[i][j] = order == -1 ? CellState.MUTED : CellState.NORMAL;
                }
            }
        }
        boolean pathFrame = frame >= m * n;
        if (!pathFrame && m * n > 0) {
            int i = frame / n + 1;
            int j = frame % n + 1;
            values[i][j] = String.valueOf(r.matrix()[i][j]);
            states[i][j] = CellState.ACTIVE;
            states[i - 1][j] = CellState.WINDOW;
            states[i][j - 1] = CellState.WINDOW;
            states[i - 1][j - 1] = CellState.WINDOW;
        }
        if (pathFrame) {
            for (int[] cell : r.path()) {
                states[cell[0]][cell[1]] = CellState.MATCH;
            }
        }
        matrix.show(rowH, colH, values, states, pathFrame ? -1 : frame % Math.max(1, n) + 1);
    }

    private static String narrate(String a, String b, EditDistance.Result r, int frame) {
        int m = a.length();
        int n = b.length();
        if (frame >= m * n) {
            StringBuilder sb = new StringBuilder("Traceback from the bottom-right cell along the recorded choices (green) gives the optimal edit script: ");
            for (EditDistance.Op op : r.script()) {
                sb.append(op.name().charAt(0)).append(' ');
            }
            return sb.append("(M=match, S=substitute, I=insert, D=delete, T=transpose). Distance = ").append(r.distance()).append(".").toString();
        }
        int i = frame / n + 1;
        int j = frame % n + 1;
        int[][] d = r.matrix();
        String choice = switch (r.ops()[i][j]) {
            case MATCH -> "letters equal → copy diagonal " + d[i - 1][j - 1];
            case SUBSTITUTE -> "substitute → diagonal " + d[i - 1][j - 1] + " + 1";
            case DELETE -> "delete '" + a.charAt(i - 1) + "' → above " + d[i - 1][j] + " + 1";
            case INSERT -> "insert '" + b.charAt(j - 1) + "' → left " + d[i][j - 1] + " + 1";
            case TRANSPOSE -> "swap adjacent letters → cell two diagonals up " + d[i - 2][j - 2] + " + 1";
        };
        return "Cell (" + i + "," + j + "): '" + a.charAt(i - 1) + "' vs '" + b.charAt(j - 1) + "': " + choice + " ⇒ D[" + i + "][" + j + "] = " + d[i][j] + ".";
    }
}
