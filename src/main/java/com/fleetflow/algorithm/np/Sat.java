package com.fleetflow.algorithm.np;

import java.util.ArrayList;
import java.util.List;

/** Boolean satisfiability (SAT / 3-SAT) in conjunctive normal form, with a brute-force solver for small formulas. */
public final class Sat {

    /**
     * A CNF formula. A literal is {@code +v} for variable x<sub>v</sub> and {@code -v} for its negation (v is 1-based).
     */
    public record Formula(int variables, List<int[]> clauses) {
        /** @return true if every clause has at most three literals (a 3-SAT instance). */
        public boolean isThreeSat() {
            return clauses.stream().allMatch(c -> c.length <= 3);
        }
    }

    /** One assignment tried by the solver. */
    public record Attempt(boolean[] assignment, boolean[] clauseValues, boolean satisfied) {
    }

    /** Solver outcome. {@code attempts} lists the assignments tried up to and including the first satisfying one. */
    public record Result(boolean satisfiable, boolean[] model, List<Attempt> attempts, long solutionCount) {
    }

    private Sat() {
    }

    /**
     * Parses a formula. Clauses are separated by new lines, ';' or '&amp;'; literals by spaces, commas, '|' or 'v'.
     * A literal is written x3, !x3, ~x3, -x3 or just 3 / -3.
     *
     * @throws IllegalArgumentException if the text is empty or contains an invalid literal
     */
    public static Formula parse(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Please enter at least one clause.");
        }
        List<int[]> clauses = new ArrayList<>();
        int maxVar = 0;
        for (String clauseText : text.split("[\\n;&]")) {
            String cleaned = clauseText.replace("(", " ").replace(")", " ").trim();
            if (cleaned.isEmpty()) {
                continue;
            }
            List<Integer> literals = new ArrayList<>();
            for (String token : cleaned.split("[\\s,|∨]+")) {
                if (token.isEmpty() || token.equals("v") || token.equals("or")) {
                    continue;
                }
                boolean negated = false;
                String t = token;
                while (t.startsWith("!") || t.startsWith("~") || t.startsWith("-") || t.startsWith("¬")) {
                    negated = !negated;
                    t = t.substring(1);
                }
                if (t.startsWith("x") || t.startsWith("X")) {
                    t = t.substring(1);
                }
                int v;
                try {
                    v = Integer.parseInt(t);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("\"" + token + "\" is not a valid literal (use x1, !x2, -3 …)");
                }
                if (v < 1) {
                    throw new IllegalArgumentException("Variable numbers start at 1 (got \"" + token + "\").");
                }
                maxVar = Math.max(maxVar, v);
                literals.add(negated ? -v : v);
            }
            if (literals.isEmpty()) {
                throw new IllegalArgumentException("A clause must contain at least one literal.");
            }
            clauses.add(literals.stream().mapToInt(Integer::intValue).toArray());
        }
        if (clauses.isEmpty()) {
            throw new IllegalArgumentException("Please enter at least one clause.");
        }
        return new Formula(maxVar, clauses);
    }

    /** @return whether each clause is true under the assignment (assignment[i] is the value of x<sub>i+1</sub>). */
    public static boolean[] clauseValues(Formula f, boolean[] assignment) {
        boolean[] values = new boolean[f.clauses().size()];
        for (int c = 0; c < values.length; c++) {
            for (int lit : f.clauses().get(c)) {
                boolean value = assignment[Math.abs(lit) - 1];
                if (lit > 0 == value) {
                    values[c] = true;
                    break;
                }
            }
        }
        return values;
    }

    public static boolean satisfies(Formula f, boolean[] assignment) {
        for (boolean v : clauseValues(f, assignment)) {
            if (!v) {
                return false;
            }
        }
        return true;
    }

    /** Tries all 2<sup>n</sup> assignments in binary-counting order (x1 is the most significant bit). */
    public static Result solve(Formula f) {
        int n = f.variables();
        if (n > 20) {
            throw new IllegalArgumentException("brute force is limited to 20 variables");
        }
        List<Attempt> attempts = new ArrayList<>();
        boolean[] model = null;
        long solutions = 0;
        for (long bits = 0; bits < (1L << n); bits++) {
            boolean[] a = new boolean[n];
            for (int i = 0; i < n; i++) {
                a[i] = ((bits >> (n - 1 - i)) & 1) == 1;
            }
            boolean[] values = clauseValues(f, a);
            boolean ok = true;
            for (boolean v : values) {
                ok &= v;
            }
            if (ok) {
                solutions++;
            }
            if (model == null) {
                attempts.add(new Attempt(a, values, ok));
                if (ok) {
                    model = a;
                }
            }
        }
        return new Result(model != null, model, attempts, solutions);
    }
}
