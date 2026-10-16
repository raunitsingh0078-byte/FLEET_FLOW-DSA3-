package com.fleetflow.algorithm.string;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Knuth-Morris-Pratt pattern matching.
 *
 * <p>The LPS ("longest proper prefix which is also a suffix") table lets the matcher fall back
 * inside the pattern after a mismatch instead of re-reading text characters, giving
 * O(n + m) time and O(m) extra space.</p>
 */
public final class KmpMatcher {

    /** What happened during one character comparison. */
    public enum Kind { MATCH, MISMATCH, FOUND }

    /**
     * One recorded comparison of {@code text[textIndex]} with {@code pattern[patternIndex]}.
     *
     * @param comparisons      total comparisons made up to and including this one
     * @param nextPatternIndex pattern pointer after this step (fallback target on a mismatch)
     */
    public record Step(Kind kind, int textIndex, int patternIndex, int comparisons, int nextPatternIndex) {
        /** @return the text position that pattern[0] is currently aligned with. */
        public int alignment() {
            return textIndex - patternIndex;
        }
    }

    /** Result of a search: match start positions, the LPS table, comparison count and the step trace. */
    public record Result(List<Integer> matches, int[] lps, int comparisons, List<Step> steps) {
    }

    private KmpMatcher() {
    }

    /**
     * Builds the LPS table of a pattern.
     *
     * @param pattern the pattern (may be empty)
     * @return lps[i] = length of the longest proper prefix of pattern[0..i] that is also its suffix
     */
    public static int[] computeLps(String pattern) {
        Objects.requireNonNull(pattern, "pattern");
        int m = pattern.length();
        int[] lps = new int[m];
        int len = 0;
        int i = 1;
        while (i < m) {
            if (pattern.charAt(i) == pattern.charAt(len)) {
                lps[i++] = ++len;
            } else if (len > 0) {
                len = lps[len - 1]; // reuse the previously computed border
            } else {
                lps[i++] = 0;
            }
        }
        return lps;
    }

    /**
     * Finds every (possibly overlapping) occurrence of {@code pattern} in {@code text}.
     * An empty pattern or a pattern longer than the text yields no matches.
     */
    public static Result search(String text, String pattern) {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(pattern, "pattern");
        int n = text.length();
        int m = pattern.length();
        int[] lps = computeLps(pattern);
        List<Integer> matches = new ArrayList<>();
        List<Step> steps = new ArrayList<>();
        if (m == 0 || m > n) {
            return new Result(matches, lps, 0, steps);
        }
        int comparisons = 0;
        int j = 0; // number of pattern characters currently matched
        for (int i = 0; i < n; i++) {
            while (true) {
                comparisons++;
                if (text.charAt(i) == pattern.charAt(j)) {
                    if (j + 1 == m) {
                        steps.add(new Step(Kind.FOUND, i, j, comparisons, lps[m - 1]));
                        matches.add(i - m + 1);
                        j = lps[m - 1]; // continue so overlapping matches are found
                    } else {
                        steps.add(new Step(Kind.MATCH, i, j, comparisons, j + 1));
                        j++;
                    }
                    break;
                }
                int fallback = j > 0 ? lps[j - 1] : 0;
                steps.add(new Step(Kind.MISMATCH, i, j, comparisons, fallback));
                if (j == 0) {
                    break; // nothing to fall back to: advance the text pointer
                }
                j = fallback; // retry the same text character with a shorter matched prefix
            }
        }
        return new Result(matches, lps, comparisons, steps);
    }
}
