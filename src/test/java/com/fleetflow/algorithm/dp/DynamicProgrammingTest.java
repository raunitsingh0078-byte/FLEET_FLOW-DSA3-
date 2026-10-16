package com.fleetflow.algorithm.dp;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DynamicProgrammingTest {

    // ---------------------------------------------------------------- edit distance

    @Test
    void levenshteinKnownValues() {
        assertEquals(3, EditDistance.compute("kitten", "sitting", false).distance());
        assertEquals(0, EditDistance.compute("same", "same", false).distance());
        assertEquals(4, EditDistance.compute("", "abcd", false).distance());
        assertEquals(4, EditDistance.compute("abcd", "", false).distance());
        assertEquals(0, EditDistance.compute("", "", false).distance());
    }

    @Test
    void damerauCountsTranspositionAsOneEdit() {
        assertEquals(2, EditDistance.compute("ab", "ba", false).distance());
        assertEquals(1, EditDistance.compute("ab", "ba", true).distance());
    }

    @Test
    void editScriptLengthMatchesDistanceAndPathEndsAtOrigin() {
        EditDistance.Result r = EditDistance.compute("Bengaluru", "Bangalore", false);
        long edits = r.script().stream().filter(op -> op != EditDistance.Op.MATCH).count();
        assertEquals(r.distance(), edits);
        int[] last = r.path().get(r.path().size() - 1);
        assertEquals(0, last[0]);
        assertEquals(0, last[1]);
    }

    @Test
    void suggestionsAreSortedByDistance() {
        var s = EditDistance.suggest("Bengalru", List.of("Mumbai", "Bengaluru", "Bengal", "Delhi"), 2, true);
        assertEquals("Bengaluru", s.get(0).word());
        assertTrue(s.get(0).distance() <= s.get(1).distance());
    }

    // ---------------------------------------------------------------- bitmask DP

    private static double bruteForce(double[][] d) {
        int n = d.length;
        List<Integer> rest = new ArrayList<>();
        for (int i = 1; i < n; i++) rest.add(i);
        return permute(d, rest, 0, 0);
    }

    private static double permute(double[][] d, List<Integer> rest, int last, double cost) {
        if (rest.isEmpty()) return cost + d[last][0];
        double best = Double.POSITIVE_INFINITY;
        for (int i = 0; i < rest.size(); i++) {
            int next = rest.remove(i);
            best = Math.min(best, permute(d, rest, next, cost + d[last][next]));
            rest.add(i, next);
        }
        return best;
    }

    @Test
    void bitmaskTspMatchesBruteForce() {
        Random rnd = new Random(3);
        for (int trial = 0; trial < 25; trial++) {
            int n = 2 + rnd.nextInt(6);
            double[][] d = new double[n][n];
            for (int i = 0; i < n; i++) for (int j = 0; j < n; j++) d[i][j] = i == j ? 0 : 1 + rnd.nextInt(50);
            BitmaskTsp.Result r = BitmaskTsp.solve(d);
            assertEquals(bruteForce(d), r.cost(), 1e-9);
            assertEquals(0, r.route()[0]);
            assertEquals(0, r.route()[n]);
            double sum = 0;
            for (int i = 0; i < n; i++) sum += d[r.route()[i]][r.route()[i + 1]];
            assertEquals(r.cost(), sum, 1e-9);
        }
    }

    @Test
    void bitmaskTspRejectsBadSizes() {
        assertThrows(IllegalArgumentException.class, () -> BitmaskTsp.solve(new double[1][1]));
        assertThrows(IllegalArgumentException.class, () -> BitmaskTsp.solve(new double[17][17]));
    }

    // ---------------------------------------------------------------- matrix chain

    @Test
    void matrixChainClassicExample() {
        MatrixChain.Result r = MatrixChain.compute(new int[]{30, 35, 15, 5, 10, 20, 25});
        assertEquals(15125, r.minCost());
        assertEquals("((A1(A2A3))((A4A5)A6))", r.parenthesization());
    }

    @Test
    void matrixChainEdgeCases() {
        assertEquals(0, MatrixChain.compute(new int[]{5, 7}).minCost());
        assertEquals("A1", MatrixChain.compute(new int[]{5, 7}).parenthesization());
        assertEquals(10 * 20 * 30, MatrixChain.compute(new int[]{10, 20, 30}).minCost());
        assertThrows(IllegalArgumentException.class, () -> MatrixChain.compute(new int[]{4}));
    }

    // ---------------------------------------------------------------- optimal BST

    private static long bruteBst(long[] f, int i, int j) {
        if (i > j) return 0;
        long sum = 0;
        for (int k = i; k <= j; k++) sum += f[k];
        long best = Long.MAX_VALUE;
        for (int r = i; r <= j; r++) best = Math.min(best, bruteBst(f, i, r - 1) + bruteBst(f, r + 1, j));
        return best + sum;
    }

    @Test
    void optimalBstMatchesRecursiveBruteForce() {
        Random rnd = new Random(11);
        for (int trial = 0; trial < 30; trial++) {
            int n = 1 + rnd.nextInt(8);
            String[] keys = new String[n];
            long[] f = new long[n];
            for (int i = 0; i < n; i++) {
                keys[i] = "k" + i;
                f[i] = rnd.nextInt(30);
            }
            assertEquals(bruteBst(f, 0, n - 1), OptimalBst.compute(keys, f).minCost());
        }
    }

    @Test
    void optimalBstSmallExampleAndValidation() {
        assertEquals(10, OptimalBst.compute(new String[]{"a", "b", "c"}, new long[]{1, 2, 3}).minCost());
        assertEquals("a", OptimalBst.compute(new String[]{"a"}, new long[]{5}).tree().key);
        assertThrows(IllegalArgumentException.class, () -> OptimalBst.compute(new String[0], new long[0]));
    }
}
