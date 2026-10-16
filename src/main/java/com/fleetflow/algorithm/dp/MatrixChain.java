package com.fleetflow.algorithm.dp;

import java.util.ArrayList;
import java.util.List;

/**
 * Matrix-chain multiplication order by interval DP: {@code m[i][j]} is the minimum number of scalar multiplications
 * needed for A<sub>i</sub>…A<sub>j</sub>, with A<sub>k</sub> of size dims[k-1] × dims[k]. Time O(n³), space O(n²).
 */
public final class MatrixChain {

    /**
     * One filled cell.
     *
     * @param candidates cost of every split k = i..j-1 that was tried
     */
    public record Step(int i, int j, int bestSplit, long cost, long[] candidates) {
    }

    /** Result with the parenthesisation, DP tables (1-based indices) and the fill trace. */
    public record Result(long minCost, long[][] cost, int[][] split, String parenthesization, List<Step> steps) {
    }

    private MatrixChain() {
    }

    public static Result compute(int[] dims) {
        if (dims.length < 2) {
            throw new IllegalArgumentException("at least one matrix (two dimensions) is required");
        }
        int n = dims.length - 1;
        long[][] m = new long[n + 1][n + 1];
        int[][] s = new int[n + 1][n + 1];
        List<Step> steps = new ArrayList<>();
        for (int len = 2; len <= n; len++) {
            for (int i = 1; i + len - 1 <= n; i++) {
                int j = i + len - 1;
                m[i][j] = Long.MAX_VALUE;
                long[] candidates = new long[j - i];
                for (int k = i; k < j; k++) {
                    long q = m[i][k] + m[k + 1][j] + (long) dims[i - 1] * dims[k] * dims[j];
                    candidates[k - i] = q;
                    if (q < m[i][j]) {
                        m[i][j] = q;
                        s[i][j] = k;
                    }
                }
                steps.add(new Step(i, j, s[i][j], m[i][j], candidates));
            }
        }
        return new Result(m[1][n], m, s, parenthesize(s, 1, n), steps);
    }

    private static String parenthesize(int[][] s, int i, int j) {
        if (i == j) {
            return "A" + i;
        }
        return "(" + parenthesize(s, i, s[i][j]) + parenthesize(s, s[i][j] + 1, j) + ")";
    }
}
