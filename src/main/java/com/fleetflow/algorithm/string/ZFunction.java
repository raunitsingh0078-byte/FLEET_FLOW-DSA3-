package com.fleetflow.algorithm.string;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Z-function: z[i] is the length of the longest substring starting at i that is also a prefix of s.
 * Runs in O(n) by maintaining the right-most match window [l, r).
 */
public final class ZFunction {

    /** Separator used when searching a pattern: it must not occur in either input. */
    public static final char SEPARATOR = '\u0000';

    /**
     * One outer-loop iteration.
     *
     * @param index      position i being computed
     * @param windowL    left end of the Z-box before this step
     * @param windowR    exclusive right end of the Z-box before this step
     * @param value      resulting z[i]
     * @param reused     true if z[i - l] inside the Z-box was used to skip comparisons
     * @param newWindowL left end of the Z-box after this step
     * @param newWindowR exclusive right end of the Z-box after this step
     */
    public record Step(int index, int windowL, int windowR, int value, boolean reused, int newWindowL,
                       int newWindowR) {
    }

    /** Result of {@link #analyze(String)}. */
    public record Result(int[] z, int comparisons, List<Step> steps) {
    }

    /** Repetition summary of a string derived from its Z array. */
    public record Repetition(int period, boolean exactRepeat, int repeats) {
    }

    private ZFunction() {
    }

    /** Computes the Z array (z[0] is defined as n). */
    public static int[] compute(String s) {
        return analyze(s).z();
    }

    /** Computes the Z array while recording each step. */
    public static Result analyze(String s) {
        Objects.requireNonNull(s, "s");
        int n = s.length();
        int[] z = new int[n];
        List<Step> steps = new ArrayList<>();
        if (n == 0) {
            return new Result(z, 0, steps);
        }
        z[0] = n;
        int l = 0;
        int r = 0;
        int comparisons = 0;
        for (int i = 1; i < n; i++) {
            int k = 0;
            boolean reused = false;
            int oldL = l;
            int oldR = r;
            if (i < r) {
                k = Math.min(r - i, z[i - l]);
                reused = true;
            }
            while (i + k < n) {
                comparisons++;
                if (s.charAt(k) == s.charAt(i + k)) {
                    k++;
                } else {
                    break;
                }
            }
            z[i] = k;
            if (i + k > r) {
                l = i;
                r = i + k;
            }
            steps.add(new Step(i, oldL, oldR, k, reused, l, r));
        }
        return new Result(z, comparisons, steps);
    }

    /** Finds all occurrences of {@code pattern} in {@code text} by running Z on pattern + separator + text. */
    public static List<Integer> search(String text, String pattern) {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(pattern, "pattern");
        List<Integer> matches = new ArrayList<>();
        int m = pattern.length();
        if (m == 0 || m > text.length()) {
            return matches;
        }
        int[] z = compute(pattern + SEPARATOR + text);
        for (int i = m + 1; i < z.length; i++) {
            if (z[i] == m) {
                matches.add(i - m - 1);
            }
        }
        return matches;
    }

    /**
     * Detects the smallest period p of the string (smallest p with p + z[p] == n).
     * If p divides n the string is an exact repetition of its first p characters.
     */
    public static Repetition repetition(int[] z) {
        int n = z.length;
        for (int p = 1; p < n; p++) {
            if (p + z[p] == n) {
                boolean exact = n % p == 0;
                return new Repetition(p, exact, exact ? n / p : 1);
            }
        }
        return new Repetition(n, false, 1);
    }
}
