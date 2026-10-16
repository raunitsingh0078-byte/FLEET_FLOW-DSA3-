package com.fleetflow.algorithm.suffix;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.TreeMap;

/** Kasai's O(n) LCP-array construction and repeated-substring extraction. */
public final class Lcp {

    /**
     * One iteration of Kasai's loop (processing text position {@code textPos}).
     *
     * @param rank   rank of that suffix in the suffix array
     * @param h      LCP value found for this rank
     * @param reused true if h was inherited from the previous position (h - 1) instead of starting at 0
     */
    public record Step(int textPos, int rank, int h, boolean reused) {
    }

    /** Kasai result: {@code lcp[i]} = LCP of suffixes sa[i-1] and sa[i] (lcp[0] = 0). */
    public record Result(int[] lcp, int[] rank, List<Step> steps, int comparisons) {
    }

    /** A substring that occurs several times. */
    public record Repeat(String text, List<Integer> positions) {
    }

    private Lcp() {
    }

    /** Computes the LCP array in O(n): the key invariant is lcp(rank[i+1]) &gt;= lcp(rank[i]) - 1. */
    public static Result kasai(String s, int[] sa) {
        Objects.requireNonNull(s, "s");
        int n = s.length();
        int[] rank = new int[n];
        for (int i = 0; i < n; i++) {
            rank[sa[i]] = i;
        }
        int[] lcp = new int[n];
        List<Step> steps = new ArrayList<>();
        int h = 0;
        int comparisons = 0;
        for (int i = 0; i < n; i++) {
            boolean reused = h > 0;
            if (rank[i] > 0) {
                int j = sa[rank[i] - 1];
                while (i + h < n && j + h < n) {
                    comparisons++;
                    if (s.charAt(i + h) != s.charAt(j + h)) {
                        break;
                    }
                    h++;
                }
                lcp[rank[i]] = h;
                steps.add(new Step(i, rank[i], h, reused));
                if (h > 0) {
                    h--;
                }
            } else {
                steps.add(new Step(i, 0, 0, false));
                h = 0;
            }
        }
        return new Result(lcp, rank, steps, comparisons);
    }

    /**
     * Finds maximal repeated substrings of length at least {@code minLength}: groups of consecutive suffixes
     * (in suffix-array order) whose LCP with their neighbour is at least minLength.
     */
    public static List<Repeat> repeats(String s, int[] sa, int[] lcp, int minLength) {
        TreeMap<String, List<Integer>> found = new TreeMap<>();
        int n = sa.length;
        int i = 1;
        while (i < n) {
            if (lcp[i] >= minLength) {
                int start = i - 1;
                int minLcp = lcp[i];
                int end = i;
                while (end + 1 < n && lcp[end + 1] >= minLength) {
                    end++;
                    minLcp = Math.min(minLcp, lcp[end]);
                }
                String sub = s.substring(sa[start], sa[start] + minLcp);
                List<Integer> positions = new ArrayList<>();
                for (int r = start; r <= end; r++) {
                    positions.add(sa[r]);
                }
                positions.sort(Integer::compare);
                found.put(sub, positions);
                i = end + 1;
            } else {
                i++;
            }
        }
        List<Repeat> result = new ArrayList<>();
        found.forEach((text, positions) -> result.add(new Repeat(text, positions)));
        result.sort((a, b) -> Integer.compare(b.text().length(), a.text().length()));
        return result;
    }
}
