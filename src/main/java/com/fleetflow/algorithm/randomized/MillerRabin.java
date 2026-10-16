package com.fleetflow.algorithm.randomized;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Miller-Rabin probabilistic primality test. Write n − 1 = 2<sup>s</sup>·d with d odd. For a base a, n passes the round
 * if a<sup>d</sup> ≡ 1 or a<sup>2^r·d</sup> ≡ −1 (mod n) for some 0 ≤ r &lt; s; otherwise a is a <em>witness</em> that n is composite.
 * A composite passes a random round with probability ≤ 1/4, so k rounds give an error probability ≤ 4<sup>−k</sup>.
 * A prime never fails. Supports n up to 4·10<sup>18</sup> (modular products use overflow-safe double-and-add).
 */
public final class MillerRabin {

    /** Largest supported input (keeps 2·n inside a signed 64-bit long). */
    public static final long MAX_N = 4_000_000_000_000_000_000L;

    private static final long[] DETERMINISTIC_BASES = {2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37};

    /**
     * One round.
     *
     * @param sequence   the values a^d, a^(2d), a^(4d) … mod n that were computed
     * @param witness    true if this base proves n composite
     */
    public record Round(long base, long[] sequence, boolean witness) {
    }

    /**
     * @param reason   explanation for trivial cases (n &lt; 2, even …); null when rounds were needed
     */
    public record Result(long n, boolean probablyPrime, int s, long d, List<Round> rounds, String reason) {
    }

    private MillerRabin() {
    }

    /** Runs {@code rounds} rounds with uniformly random bases from 2..n-2. */
    public static Result test(long n, int rounds, Random rnd) {
        if (n < 0 || n > MAX_N) {
            throw new IllegalArgumentException("n must be between 0 and " + MAX_N);
        }
        if (rounds < 1) {
            throw new IllegalArgumentException("at least one round is required");
        }
        if (n < 2) {
            return new Result(n, false, 0, 0, List.of(), n + " is smaller than 2, hence not prime");
        }
        if (n < 4) {
            return new Result(n, true, 0, 0, List.of(), n + " is a small prime");
        }
        if (n % 2 == 0) {
            return new Result(n, false, 0, 0, List.of(), n + " is even and greater than 2");
        }
        int s = 0;
        long d = n - 1;
        while (d % 2 == 0) {
            d /= 2;
            s++;
        }
        List<Round> list = new ArrayList<>();
        for (int i = 0; i < rounds; i++) {
            long a = 2 + (long) (rnd.nextDouble() * (n - 3)); // 2 .. n-2
            a = Math.min(a, n - 2);
            Round r = round(n, a, s, d);
            list.add(r);
            if (r.witness()) {
                return new Result(n, false, s, d, list, null);
            }
        }
        return new Result(n, true, s, d, list, null);
    }

    /** Deterministic primality test for every n ≤ 4·10^18 using the first twelve prime bases. */
    public static boolean isPrime(long n) {
        if (n < 2) {
            return false;
        }
        for (long p : DETERMINISTIC_BASES) {
            if (n == p) {
                return true;
            }
            if (n % p == 0) {
                return false;
            }
        }
        int s = 0;
        long d = n - 1;
        while (d % 2 == 0) {
            d /= 2;
            s++;
        }
        for (long a : DETERMINISTIC_BASES) {
            if (round(n, a, s, d).witness()) {
                return false;
            }
        }
        return true;
    }

    private static Round round(long n, long a, int s, long d) {
        List<Long> seq = new ArrayList<>();
        long x = powMod(a, d, n);
        seq.add(x);
        boolean passes = x == 1 || x == n - 1;
        for (int r = 1; r < s && !passes; r++) {
            x = mulMod(x, x, n);
            seq.add(x);
            if (x == n - 1) {
                passes = true;
            }
        }
        return new Round(a, seq.stream().mapToLong(Long::longValue).toArray(), !passes);
    }

    /** Modular exponentiation by repeated squaring. */
    static long powMod(long base, long exp, long mod) {
        long result = 1 % mod;
        long b = base % mod;
        while (exp > 0) {
            if ((exp & 1) == 1) {
                result = mulMod(result, b, mod);
            }
            b = mulMod(b, b, mod);
            exp >>= 1;
        }
        return result;
    }

    /** (a·b) mod m without overflow, by binary "double and add" (requires m ≤ MAX_N). */
    static long mulMod(long a, long b, long m) {
        long result = 0;
        a %= m;
        while (b > 0) {
            if ((b & 1) == 1) {
                result += a;
                if (result >= m) {
                    result -= m;
                }
            }
            a <<= 1;
            if (a >= m) {
                a -= m;
            }
            b >>= 1;
        }
        return result;
    }
}
