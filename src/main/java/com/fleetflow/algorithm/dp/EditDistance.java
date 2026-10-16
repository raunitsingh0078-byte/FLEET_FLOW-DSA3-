package com.fleetflow.algorithm.dp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Levenshtein distance and its Damerau extension (optimal string alignment: adjacent transposition costs 1).
 * The full DP matrix and the optimal edit path are kept so they can be visualised.
 */
public final class EditDistance {

    /** The edit operation chosen for a cell. */
    public enum Op { MATCH, SUBSTITUTE, INSERT, DELETE, TRANSPOSE }

    /**
     * @param matrix   matrix[i][j] = distance between a[0..i) and b[0..j)
     * @param ops      operation that produced each cell (null on the first row / column border)
     * @param path     cells (i, j) of one optimal alignment from (m, n) back to (0, 0)
     * @param script   edit operations in left-to-right order
     */
    public record Result(int distance, int[][] matrix, Op[][] ops, List<int[]> path, List<Op> script) {
    }

    /** A dictionary word with its distance to the query. */
    public record Suggestion(String word, int distance) {
    }

    private EditDistance() {
    }

    /**
     * Computes the distance between {@code a} and {@code b}.
     *
     * @param damerau whether adjacent transpositions ("ab" → "ba") count as one edit
     */
    public static Result compute(String a, String b, boolean damerau) {
        Objects.requireNonNull(a, "a");
        Objects.requireNonNull(b, "b");
        int m = a.length();
        int n = b.length();
        int[][] d = new int[m + 1][n + 1];
        Op[][] ops = new Op[m + 1][n + 1];
        for (int i = 1; i <= m; i++) {
            d[i][0] = i;
            ops[i][0] = Op.DELETE;
        }
        for (int j = 1; j <= n; j++) {
            d[0][j] = j;
            ops[0][j] = Op.INSERT;
        }
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                boolean same = a.charAt(i - 1) == b.charAt(j - 1);
                int best = d[i - 1][j - 1] + (same ? 0 : 1);
                Op op = same ? Op.MATCH : Op.SUBSTITUTE;
                if (d[i - 1][j] + 1 < best) {
                    best = d[i - 1][j] + 1;
                    op = Op.DELETE;
                }
                if (d[i][j - 1] + 1 < best) {
                    best = d[i][j - 1] + 1;
                    op = Op.INSERT;
                }
                if (damerau && i > 1 && j > 1 && a.charAt(i - 1) == b.charAt(j - 2) && a.charAt(i - 2) == b.charAt(j - 1)
                        && d[i - 2][j - 2] + 1 < best) {
                    best = d[i - 2][j - 2] + 1;
                    op = Op.TRANSPOSE;
                }
                d[i][j] = best;
                ops[i][j] = op;
            }
        }
        List<int[]> path = new ArrayList<>();
        List<Op> script = new ArrayList<>();
        int i = m;
        int j = n;
        path.add(new int[]{i, j});
        while (i > 0 || j > 0) {
            Op op = ops[i][j];
            script.add(0, op);
            switch (op) {
                case MATCH, SUBSTITUTE -> {
                    i--;
                    j--;
                }
                case DELETE -> i--;
                case INSERT -> j--;
                case TRANSPOSE -> {
                    i -= 2;
                    j -= 2;
                }
            }
            path.add(new int[]{i, j});
        }
        return new Result(d[m][n], d, ops, path, script);
    }

    /** Returns the {@code limit} dictionary entries closest to {@code input} (ties broken alphabetically). */
    public static List<Suggestion> suggest(String input, Collection<String> dictionary, int limit, boolean damerau) {
        List<Suggestion> all = new ArrayList<>();
        for (String word : dictionary) {
            all.add(new Suggestion(word, compute(input, word, damerau).distance()));
        }
        all.sort(Comparator.comparingInt(Suggestion::distance).thenComparing(Suggestion::word));
        return all.subList(0, Math.min(limit, all.size()));
    }
}
