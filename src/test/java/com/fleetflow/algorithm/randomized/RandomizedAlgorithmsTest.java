package com.fleetflow.algorithm.randomized;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RandomizedAlgorithmsTest {

    // ---------------------------------------------------------------- quicksort

    @Test
    void sortsRandomArraysInBothModes() {
        Random rnd = new Random(1);
        for (int trial = 0; trial < 200; trial++) {
            int[] a = new int[rnd.nextInt(40)];
            for (int i = 0; i < a.length; i++) a[i] = rnd.nextInt(20) - 10;
            int[] expected = a.clone();
            Arrays.sort(expected);
            assertArrayEquals(expected, RandomizedQuickSort.sort(a, true, trial, false).sorted());
            assertArrayEquals(expected, RandomizedQuickSort.sort(a, false, trial, false).sorted());
        }
    }

    @Test
    void edgeCasesEmptySingleAndDuplicates() {
        assertEquals(0, RandomizedQuickSort.sort(new int[0], true, 1, true).sorted().length);
        assertArrayEquals(new int[]{5}, RandomizedQuickSort.sort(new int[]{5}, true, 1, true).sorted());
        int[] same = new int[2000];
        Arrays.fill(same, 7);
        assertArrayEquals(same, RandomizedQuickSort.sort(same, true, 3, false).sorted());
    }

    @Test
    void inputIsNotModified() {
        int[] a = {3, 1, 2};
        RandomizedQuickSort.sort(a, true, 1, false);
        assertArrayEquals(new int[]{3, 1, 2}, a);
    }

    @Test
    void randomPivotAvoidsQuadraticBehaviourOnSortedInput() {
        int n = 3000;
        int[] sorted = new int[n];
        for (int i = 0; i < n; i++) sorted[i] = i;
        long random = RandomizedQuickSort.sort(sorted, true, 5, false).comparisons();
        long fixed = RandomizedQuickSort.sort(sorted, false, 5, false).comparisons();
        assertTrue(fixed >= (long) n * (n - 1) / 2, "last-element pivot is quadratic on sorted input");
        assertTrue(random < 20L * n * Math.log(n) / Math.log(2), "random pivot stays near n log n but was " + random);
    }

    @Test
    void sameSeedGivesSameTrace() {
        int[] a = {9, 4, 7, 1, 8, 2};
        assertEquals(RandomizedQuickSort.sort(a, true, 42, true).steps().size(), RandomizedQuickSort.sort(a, true, 42, true).steps().size());
        assertFalse(RandomizedQuickSort.sort(a, true, 42, true).steps().isEmpty());
        assertTrue(RandomizedQuickSort.sort(a, true, 42, false).steps().isEmpty());
    }

    // ---------------------------------------------------------------- reservoir sampling

    @Test
    void reservoirHasKDistinctItemsFromTheStream() {
        ReservoirSampling.Result r = ReservoirSampling.sample(100, 10, new Random(4));
        assertEquals(10, r.reservoir().length);
        assertEquals(10, Arrays.stream(r.reservoir()).distinct().count());
        assertTrue(Arrays.stream(r.reservoir()).allMatch(x -> x >= 1 && x <= 100));
        assertEquals(100, r.steps().size());
    }

    @Test
    void streamShorterThanReservoirKeepsEverything() {
        ReservoirSampling.Result r = ReservoirSampling.sample(3, 5, new Random(1));
        assertArrayEquals(new int[]{1, 2, 3, 0, 0}, r.reservoir());
    }

    @Test
    void everyItemIsEquallyLikelyToBeSampled() {
        double[] freq = ReservoirSampling.inclusionFrequency(20, 5, 20000, 99);
        for (double f : freq) {
            assertEquals(0.25, f, 0.02);
        }
    }

    @Test
    void reservoirRejectsInvalidArguments() {
        assertThrows(IllegalArgumentException.class, () -> ReservoirSampling.sample(10, 0, new Random()));
        assertThrows(IllegalArgumentException.class, () -> ReservoirSampling.sample(-1, 2, new Random()));
    }

    // ---------------------------------------------------------------- Miller-Rabin

    private static boolean trialDivision(long n) {
        if (n < 2) return false;
        for (long d = 2; d * d <= n; d++) if (n % d == 0) return false;
        return true;
    }

    @Test
    void deterministicTestMatchesTrialDivision() {
        for (long n = 0; n < 5000; n++) assertEquals(trialDivision(n), MillerRabin.isPrime(n), "n=" + n);
    }

    @Test
    void knownLargePrimesAndComposites() {
        assertTrue(MillerRabin.isPrime(1_000_000_007L));
        assertTrue(MillerRabin.isPrime(2_305_843_009_213_693_951L)); // 2^61 - 1
        assertFalse(MillerRabin.isPrime(1_000_000_007L * 3));
        assertFalse(MillerRabin.isPrime(3_215_031_751L)); // strong pseudoprime to bases 2,3,5,7
        assertFalse(MillerRabin.isPrime(561)); // Carmichael number
    }

    @Test
    void randomisedTestNeverRejectsAPrime() {
        Random rnd = new Random(8);
        for (long p : new long[]{2, 3, 5, 97, 7919, 1_000_000_007L, 2_305_843_009_213_693_951L}) {
            assertTrue(MillerRabin.test(p, 5, rnd).probablyPrime(), "prime " + p);
        }
    }

    @Test
    void randomisedTestFindsWitnessesForCompositesWithManyRounds() {
        Random rnd = new Random(2);
        for (long c : new long[]{0, 1, 4, 9, 91, 561, 1105, 1_000_000_007L * 3}) {
            assertFalse(MillerRabin.test(c, 30, rnd).probablyPrime(), "composite " + c);
        }
        MillerRabin.Result r = MillerRabin.test(561, 30, new Random(3));
        assertTrue(r.rounds().get(r.rounds().size() - 1).witness());
    }

    @Test
    void millerRabinValidatesInput() {
        assertThrows(IllegalArgumentException.class, () -> MillerRabin.test(-5, 3, new Random()));
        assertThrows(IllegalArgumentException.class, () -> MillerRabin.test(7, 0, new Random()));
        assertThrows(IllegalArgumentException.class, () -> MillerRabin.test(MillerRabin.MAX_N + 1, 3, new Random()));
    }
}
