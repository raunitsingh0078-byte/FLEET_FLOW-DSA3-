package com.fleetflow.ui.panel.np;

import com.fleetflow.algorithm.np.Sat;
import com.fleetflow.ui.AlgorithmPanel;
import com.fleetflow.ui.Execution;
import com.fleetflow.ui.Ui;
import com.fleetflow.util.ValidationException;
import com.fleetflow.visualization.ArrayVisualizer;
import com.fleetflow.visualization.ArrayVisualizer.Cell;
import com.fleetflow.visualization.CellState;
import com.fleetflow.visualization.RowTableVisualizer;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
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

/** SAT / 3-SAT page: brute-force solving and certificate checking of Boolean formulas from delivery constraints. */
public final class SatPanel extends AlgorithmPanel {

    private static final int MAX_VARS = 12;
    private static final int MAX_CLAUSES = 14;
    private static final String SOLVE = "Solve by trying all assignments";
    private static final String CHECK = "Check my assignment";

    private final TextArea clausesArea = new TextArea();
    private final ComboBox<String> mode = new ComboBox<>();
    private final TextField assignmentField = new TextField();
    private final ArrayVisualizer assignmentView = new ArrayVisualizer();
    private final RowTableVisualizer clauseTable = new RowTableVisualizer("Clause", "Literal values", "Value");

    public SatPanel() {
        clausesArea.setPrefRowCount(4);
        clausesArea.setPromptText("One clause per line, literals separated by spaces. Use x1 !x2 x3  (or 1 -2 3)");
        mode.getItems().addAll(SOLVE, CHECK);
        mode.setValue(SOLVE);
        assignmentField.setPromptText("For 'check': values of x1, x2, … e.g. 1 0 1  or  x1=T, x2=F");
        HBox.setHgrow(assignmentField, Priority.ALWAYS);
        assignmentView.setPrefViewportHeight(100);
        assignmentView.setMinHeight(90);
        assemble("SAT / 3-SAT – Delivery Constraint Satisfiability",
                "Real problem: 'driver A must not take both orders 1 and 2, order 3 needs vehicle X or Y…' are Boolean constraints; deciding whether all can hold "
                        + "at once is SAT, NP-complete even when every clause has 3 literals. Checking a given assignment takes O(m) (this is the NP certificate); "
                        + "finding one may need all 2ⁿ assignments. Educational demonstration – exponential problems are restricted to small input sizes.",
                new VBox(8, clausesArea, new HBox(10, Ui.label("Mode", "kv-key"), mode, Ui.label("Assignment", "kv-key"), assignmentField)),
                new VBox(10, assignmentView, clauseTable));
        loadDemo();
    }

    @Override
    protected void loadDemo() {
        clausesArea.setText("x1 x2 !x3\n!x1 x3 x2\n!x2 !x3 x1\nx3 x1 x2");
        mode.setValue(SOLVE);
        assignmentField.setText("1 0 1");
    }

    private boolean[] parseAssignment(int variables) throws ValidationException {
        boolean[] a = new boolean[variables];
        boolean[] set = new boolean[variables];
        String raw = assignmentField.getText() == null ? "" : assignmentField.getText().trim();
        if (raw.isEmpty()) {
            throw new ValidationException("Please enter an assignment such as  1 0 1  (one value for each of the " + variables + " variables).");
        }
        int next = 0;
        for (String token : raw.split("[\\s,]+")) {
            int idx = next;
            String value = token;
            if (token.contains("=")) {
                String[] p = token.split("=");
                try {
                    idx = Integer.parseInt(p[0].replaceAll("[xX]", "")) - 1;
                } catch (NumberFormatException e) {
                    throw new ValidationException("\"" + token + "\" is not a valid assignment like x1=1.");
                }
                value = p.length > 1 ? p[1] : "";
            } else {
                next++;
            }
            if (idx < 0 || idx >= variables) {
                throw new ValidationException("Variable index in \"" + token + "\" is outside x1..x" + variables + ".");
            }
            switch (value.toUpperCase()) {
                case "1", "T", "TRUE" -> a[idx] = true;
                case "0", "F", "FALSE" -> a[idx] = false;
                default -> throw new ValidationException("Values must be 1/0 or T/F (got \"" + token + "\").");
            }
            set[idx] = true;
        }
        for (int i = 0; i < variables; i++) {
            if (!set[i]) {
                throw new ValidationException("No value given for x" + (i + 1) + ".");
            }
        }
        return a;
    }

    @Override
    protected Supplier<Execution> prepare() throws ValidationException {
        Sat.Formula f;
        try {
            f = Sat.parse(clausesArea.getText());
        } catch (IllegalArgumentException e) {
            throw new ValidationException(e.getMessage());
        }
        if (f.variables() > MAX_VARS) {
            throw new ValidationException("The formula uses " + f.variables() + " variables; at most " + MAX_VARS + " are supported.");
        }
        if (f.clauses().size() > MAX_CLAUSES) {
            throw new ValidationException("Please use at most " + MAX_CLAUSES + " clauses.");
        }
        boolean check = CHECK.equals(mode.getValue());
        boolean[] mine = check ? parseAssignment(f.variables()) : null;
        Sat.Formula formula = f;
        return () -> execute(formula, mine);
    }

    private Execution execute(Sat.Formula f, boolean[] mine) {
        Sat.Result all = Sat.solve(f);
        String shape = f.isThreeSat() ? "3-SAT instance (every clause has ≤ 3 literals)" : "general SAT instance (some clause has more than 3 literals)";
        String result;
        List<Sat.Attempt> attempts = new ArrayList<>();
        int frames;
        if (mine != null) {
            boolean[] values = Sat.clauseValues(f, mine);
            attempts.add(new Sat.Attempt(mine, values, Sat.satisfies(f, mine)));
            frames = f.clauses().size();
            result = "Your assignment " + describe(mine) + " is " + (attempts.get(0).satisfied() ? "SATISFYING ✓" : "NOT satisfying ✗ (" + countFalse(values) + " clause(s) false)")
                    + "\nThe formula is " + (all.satisfiable() ? "satisfiable, e.g. " + describe(all.model()) : "UNSATISFIABLE") + " (" + all.solutionCount() + " of " + (1L << f.variables()) + " assignments satisfy it).";
        } else {
            attempts = all.attempts();
            frames = attempts.size();
            result = all.satisfiable() ? "SATISFIABLE ✓ – first model found after " + attempts.size() + " assignment(s): " + describe(all.model())
                    + "\nTotal satisfying assignments: " + all.solutionCount() + " of " + (1L << f.variables())
                    : "UNSATISFIABLE ✗ – all " + (1L << f.variables()) + " assignments were tried and each falsifies at least one clause.";
        }
        String complexity = "Verify one assignment O(m·k) (polynomial: SAT ∈ NP)   |   Brute-force search O(2ⁿ·m) = O(" + (1L << f.variables()) + "·" + f.clauses().size() + ")   |   3-SAT is NP-complete (Cook–Levin)";
        List<Sat.Attempt> shown = attempts;
        return new Execution(frames, i -> draw(f, shown, mine != null, i), i -> narrate(f, shown, mine != null, i), result + "\nThis is a " + shape + ".", complexity,
                (long) attempts.size() * f.clauses().size(), attempts.size(), "n = " + f.variables() + " variables, m = " + f.clauses().size() + " clauses.");
    }

    private static String describe(boolean[] a) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < a.length; i++) sb.append(i == 0 ? "" : ", ").append("x").append(i + 1).append("=").append(a[i] ? "T" : "F");
        return sb.toString();
    }

    private static int countFalse(boolean[] v) {
        int c = 0;
        for (boolean b : v) if (!b) c++;
        return c;
    }

    private static String clauseText(int[] clause) {
        StringBuilder sb = new StringBuilder("(");
        for (int i = 0; i < clause.length; i++) sb.append(i == 0 ? "" : " ∨ ").append(clause[i] < 0 ? "¬" : "").append("x").append(Math.abs(clause[i]));
        return sb.append(")").toString();
    }

    private void draw(Sat.Formula f, List<Sat.Attempt> attempts, boolean check, int frame) {
        Sat.Attempt at = check ? attempts.get(0) : attempts.get(frame);
        int n = f.variables();
        Cell[] names = new Cell[n];
        Cell[] vals = new Cell[n];
        for (int i = 0; i < n; i++) {
            names[i] = ArrayVisualizer.cell("x" + (i + 1), CellState.MUTED);
            vals[i] = ArrayVisualizer.cell(at.assignment()[i] ? "T" : "F", at.assignment()[i] ? CellState.MATCH : CellState.MISMATCH);
        }
        assignmentView.render(List.of("Variable", "Value"), Arrays.asList(names, vals), -1);
        int shownClauses = check ? frame + 1 : f.clauses().size();
        List<String[]> rows = new ArrayList<>();
        Map<Integer, CellState> states = new HashMap<>();
        for (int c = 0; c < shownClauses; c++) {
            int[] clause = f.clauses().get(c);
            StringBuilder lv = new StringBuilder();
            for (int lit : clause) {
                boolean value = at.assignment()[Math.abs(lit) - 1];
                lv.append(lit > 0 == value ? "T " : "F ");
            }
            rows.add(new String[]{clauseText(clause), lv.toString().trim(), at.clauseValues()[c] ? "TRUE ✓" : "FALSE ✗"});
            states.put(c, at.clauseValues()[c] ? CellState.DONE : CellState.MISMATCH);
        }
        clauseTable.show(rows, states);
    }

    private static String narrate(Sat.Formula f, List<Sat.Attempt> attempts, boolean check, int frame) {
        if (check) {
            Sat.Attempt at = attempts.get(0);
            return "Evaluate clause " + (frame + 1) + " " + clauseText(f.clauses().get(frame)) + " under the assignment: it is "
                    + (at.clauseValues()[frame] ? "TRUE (at least one literal is true)." : "FALSE (every literal is false) – the assignment fails.");
        }
        Sat.Attempt at = attempts.get(frame);
        return "Assignment #" + (frame + 1) + " (binary counting): " + describe(at.assignment()) + " → "
                + (at.satisfied() ? "all clauses are true: SATISFYING assignment found!" : countFalse(at.clauseValues()) + " clause(s) false, so try the next assignment.");
    }
}
