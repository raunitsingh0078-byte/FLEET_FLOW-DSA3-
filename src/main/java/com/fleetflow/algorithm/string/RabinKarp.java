package com.fleetflow.algorithm.string;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Rabin-Karp pattern matching with a rolling polynomial hash.
 *
 * <p>The hash of the window text[i..i+m-1] is updated in O(1) when the window slides. Only when the
 * hashes are equal are the characters compared (to rule out a "spurious hit"). Expected time O(n + m).</p>
 */
public final class RabinKarp {

    /** Alphabet size used as hash base. */
    public static final int BASE = 256;

    /**
     * One window position.
     *
     * @param windowHash hash of the current window
     * @param hashEqual  true if windowHash equals the pattern hash
     * @param verified   true if the characters really matched (only meaningful when hashEqual)
     */
    public record Step(int start, long windowHash, boolean hashEqual, boolean verified) {
        /** @return true for a hash collision: equal hash but different characters. */
        public boolean spurious() {
            return hashEqual && !verified;
        }
    }

    /** Result of a search. */
    public record Result(List<Integer> matches, long patternHash, int modulus, int hashComparisons,
                         int charComparisons, int spuriousHits, List<Step> steps) {
    }

    private RabinKarp() {
    }

    /**
     * Searches {@code pattern} in {@code text} using hash modulus {@code modulus}.
     *
     * @param modulus a value between 2 and 1,000,000,007; a small modulus makes collisions visible
     * @throws IllegalArgumentException if the modulus is out of range
     */
    public static Result search(String text, String pattern, int modulus) {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(pattern, "pattern");
        if (modulus < 2) {
            throw new IllegalArgumentException("modulus must be at least 2");
        }
        int n = text.length();
        int m = pattern.length();
        List<Integer> matches = new ArrayList<>();
        List<Step> steps = new ArrayList<>();
        if (m == 0 || m > n) {
            return new Result(matches, 0, modulus, 0, 0, 0, steps);
        }
        long high = 1; // BASE^(m-1) mod modulus, weight of the leading character
        for (int i = 1; i < m; i++) {
            high = (high * BASE) % modulus;
        }
        long patternHash = 0;
        long windowHash = 0;
        for (int i = 0; i < m; i++) {
            patternHash = (patternHash * BASE + pattern.charAt(i)) % modulus;
            windowHash = (windowHash * BASE + text.charAt(i)) % modulus;
        }
        int hashComparisons = 0;
        int charComparisons = 0;
        int spurious = 0;
        for (int i = 0; i + m <= n; i++) {
            hashComparisons++;
            boolean hashEqual = windowHash == patternHash;
            boolean verified = false;
            if (hashEqual) {
                int k = 0;
                while (k < m) {
                    charComparisons++;
                    if (text.charAt(i + k) != pattern.charAt(k)) {
                        break;
                    }
                    k++;
                }
                verified = k == m;
                if (verified) {
                    matches.add(i);
                } else {
                    spurious++;
                }
            }
            steps.add(new Step(i, windowHash, hashEqual, verified));
            if (i + m < n) { // roll: remove text[i], append text[i+m]
                long removed = (text.charAt(i) * high) % modulus;
                windowHash = ((windowHash - removed + modulus) % modulus * BASE + text.charAt(i + m)) % modulus;
            }
        }
        return new Result(matches, patternHash, modulus, hashComparisons, charComparisons, spurious, steps);
    }
}
