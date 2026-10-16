package com.fleetflow.algorithm.suffix;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Sort-based suffix array construction plus binary-search pattern lookup.
 *
 * <p>Construction sorts the suffix start positions with a lexicographic suffix comparator
 * (O(n² log n) worst case, fine for documents). SA-IS ({@link SaIs}) builds the same array in O(n).</p>
 */
public final class SuffixArray {

    /** One binary-search probe: the pattern was compared with suffix {@code sa[mid]}. */
    public record Probe(int lo, int hi, int mid, int comparison) {
    }

    /**
     * Result of a pattern search.
     *
     * @param first      first rank whose suffix starts with the pattern (or insertion point)
     * @param last       last such rank; the range is empty when {@code first > last}
     * @param probes     binary-search trace (lower-bound search followed by upper-bound search)
     * @param comparisons number of suffix/pattern comparisons
     */
    public record SearchResult(int first, int last, List<Probe> probes, int comparisons) {
        public int count() {
            return Math.max(0, last - first + 1);
        }
    }

    private SuffixArray() {
    }

    /** Builds the suffix array by sorting all suffixes. */
    public static int[] build(String s) {
        Objects.requireNonNull(s, "s");
        Integer[] idx = new Integer[s.length()];
        for (int i = 0; i < idx.length; i++) {
            idx[i] = i;
        }
        Arrays.sort(idx, (a, b) -> compareSuffixes(s, a, b));
        int[] sa = new int[idx.length];
        for (int i = 0; i < sa.length; i++) {
            sa[i] = idx[i];
        }
        return sa;
    }

    private static int compareSuffixes(String s, int a, int b) {
        int n = s.length();
        while (a < n && b < n) {
            int c = Character.compare(s.charAt(a), s.charAt(b));
            if (c != 0) {
                return c;
            }
            a++;
            b++;
        }
        return Integer.compare(n - a, n - b); // the shorter (exhausted) suffix is smaller
    }

    /** Compares pattern with the prefix of the suffix starting at {@code pos}: <0, 0 (pattern is a prefix), >0. */
    private static int comparePrefix(String s, int pos, String pattern) {
        int i = 0;
        while (i < pattern.length() && pos + i < s.length()) {
            int c = Character.compare(pattern.charAt(i), s.charAt(pos + i));
            if (c != 0) {
                return c;
            }
            i++;
        }
        return i == pattern.length() ? 0 : 1; // pattern longer than the remaining suffix => pattern is greater
    }

    /** Finds the range of ranks whose suffixes start with {@code pattern} using two binary searches. */
    public static SearchResult search(String s, int[] sa, String pattern) {
        List<Probe> probes = new ArrayList<>();
        int comparisons = 0;
        int lo = 0;
        int hi = sa.length;
        while (lo < hi) { // lower bound: first suffix >= pattern (prefix-wise)
            int mid = (lo + hi) >>> 1;
            int c = comparePrefix(s, sa[mid], pattern);
            comparisons++;
            probes.add(new Probe(lo, hi - 1, mid, c));
            if (c > 0) {
                lo = mid + 1;
            } else {
                hi = mid;
            }
        }
        int first = lo;
        hi = sa.length;
        while (lo < hi) { // upper bound: first suffix whose prefix is > pattern
            int mid = (lo + hi) >>> 1;
            int c = comparePrefix(s, sa[mid], pattern);
            comparisons++;
            probes.add(new Probe(lo, hi - 1, mid, c));
            if (c >= 0) {
                lo = mid + 1;
            } else {
                hi = mid;
            }
        }
        return new SearchResult(first, lo - 1, probes, comparisons);
    }
}
