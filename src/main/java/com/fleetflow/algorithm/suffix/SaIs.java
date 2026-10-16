package com.fleetflow.algorithm.suffix;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;

/**
 * SA-IS: linear-time suffix array construction by induced sorting (Nong, Zhang &amp; Chan).
 *
 * <p>Idea: classify every suffix as S-type (smaller than its successor) or L-type; the
 * "leftmost S-type" (LMS) suffixes are sorted first (recursively, on a reduced string of LMS-substring names),
 * and all other suffixes are then <em>induced</em> from them with two linear scans.
 * Total time O(n), extra space O(n).</p>
 */
public final class SaIs {

    /**
     * Description of one recursion level, for visualisation.
     *
     * @param values    the (integer) string sorted at this level
     * @param types     'S' or 'L' per position
     * @param lms       positions of LMS suffixes
     * @param reducedLength length of the reduced problem passed down (0 if none)
     */
    public record Level(int[] values, String types, List<Integer> lms, int reducedLength) {
    }

    /** Suffix array plus the recursion trace. */
    public record Result(int[] sa, List<Level> levels) {
    }

    private SaIs() {
    }

    /** Builds the suffix array of {@code s}. */
    public static Result build(String s) {
        Objects.requireNonNull(s, "s");
        TreeSet<Character> alphabet = new TreeSet<>();
        for (char c : s.toCharArray()) {
            alphabet.add(c);
        }
        List<Character> sorted = new ArrayList<>(alphabet);
        int[] values = new int[s.length()];
        for (int i = 0; i < values.length; i++) {
            values[i] = sorted.indexOf(s.charAt(i));
        }
        List<Level> levels = new ArrayList<>();
        int[] sa = sais(values, Math.max(0, sorted.size() - 1), levels);
        return new Result(sa, levels);
    }

    private static int[] sais(int[] s, int upper, List<Level> levels) {
        int n = s.length;
        if (n == 0) {
            return new int[0];
        }
        if (n == 1) {
            levels.add(new Level(s.clone(), "S", List.of(), 0));
            return new int[]{0};
        }
        if (n == 2) {
            levels.add(new Level(s.clone(), s[0] < s[1] ? "SL" : "LL", List.of(), 0));
            return s[0] < s[1] ? new int[]{0, 1} : new int[]{1, 0};
        }
        int[] sa = new int[n];
        boolean[] isS = new boolean[n]; // isS[i]: suffix i is S-type
        for (int i = n - 2; i >= 0; i--) {
            isS[i] = s[i] == s[i + 1] ? isS[i + 1] : s[i] < s[i + 1];
        }
        int[] sumL = new int[upper + 1];
        int[] sumS = new int[upper + 1];
        for (int i = 0; i < n; i++) {
            if (!isS[i]) {
                sumS[s[i]]++;
            } else {
                sumL[s[i] + 1]++;
            }
        }
        for (int i = 0; i <= upper; i++) {
            sumS[i] += sumL[i];
            if (i < upper) {
                sumL[i + 1] += sumS[i];
            }
        }
        int[] lmsMap = new int[n + 1];
        Arrays.fill(lmsMap, -1);
        int m = 0;
        for (int i = 1; i < n; i++) {
            if (!isS[i - 1] && isS[i]) {
                lmsMap[i] = m++;
            }
        }
        int[] lms = new int[m];
        int k = 0;
        for (int i = 1; i < n; i++) {
            if (!isS[i - 1] && isS[i]) {
                lms[k++] = i;
            }
        }
        StringBuilder types = new StringBuilder();
        for (boolean b : isS) {
            types.append(b ? 'S' : 'L');
        }
        List<Integer> lmsList = new ArrayList<>();
        for (int p : lms) {
            lmsList.add(p);
        }
        levels.add(new Level(s.clone(), types.toString(), lmsList, m));

        induce(s, sa, isS, sumS, sumL, lms, upper);
        if (m > 0) {
            int[] sortedLms = new int[m];
            int cnt = 0;
            for (int v : sa) {
                if (lmsMap[v] != -1) {
                    sortedLms[cnt++] = v;
                }
            }
            int[] recS = new int[m];
            int recUpper = 0;
            recS[lmsMap[sortedLms[0]]] = 0;
            for (int i = 1; i < m; i++) { // name LMS substrings: equal substrings get equal names
                int l = sortedLms[i - 1];
                int r = sortedLms[i];
                int endL = lmsMap[l] + 1 < m ? lms[lmsMap[l] + 1] : n;
                int endR = lmsMap[r] + 1 < m ? lms[lmsMap[r] + 1] : n;
                boolean same = true;
                if (endL - l != endR - r) {
                    same = false;
                } else {
                    while (l < endL) {
                        if (s[l] != s[r]) {
                            break;
                        }
                        l++;
                        r++;
                    }
                    if (l == n || s[l] != s[r]) {
                        same = false;
                    }
                }
                if (!same) {
                    recUpper++;
                }
                recS[lmsMap[sortedLms[i]]] = recUpper;
            }
            int[] recSa = sais(recS, recUpper, levels);
            for (int i = 0; i < m; i++) {
                sortedLms[i] = lms[recSa[i]];
            }
            induce(s, sa, isS, sumS, sumL, sortedLms, upper);
        }
        return sa;
    }

    /** Places the given LMS suffixes at bucket ends, then induces L-type and S-type suffixes. */
    private static void induce(int[] s, int[] sa, boolean[] isS, int[] sumS, int[] sumL, int[] lms, int upper) {
        int n = s.length;
        Arrays.fill(sa, -1);
        int[] buf = Arrays.copyOf(sumS, upper + 1);
        for (int d : lms) {
            if (d == n) {
                continue;
            }
            sa[buf[s[d]]++] = d;
        }
        buf = Arrays.copyOf(sumL, upper + 1);
        sa[buf[s[n - 1]]++] = n - 1;
        for (int i = 0; i < n; i++) {
            int v = sa[i];
            if (v >= 1 && !isS[v - 1]) {
                sa[buf[s[v - 1]]++] = v - 1;
            }
        }
        buf = Arrays.copyOf(sumL, upper + 1);
        for (int i = n - 1; i >= 0; i--) {
            int v = sa[i];
            if (v >= 1 && isS[v - 1]) {
                sa[--buf[s[v - 1] + 1]] = v - 1;
            }
        }
    }
}
