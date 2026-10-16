package com.fleetflow.algorithm.parallel;

import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParallelAlgorithmsTest {

    // ---------------------------------------------------------------- Blelloch scan

    @Test
    void exclusiveScanMatchesSequentialPrefixSums() {
        Random rnd = new Random(6);
        for (int len = 0; len <= 70; len++) {
            long[] a = new long[len];
            for (int i = 0; i < len; i++) a[i] = rnd.nextInt(100) - 30;
            long[] expected = new long[len];
            long running = 0;
            for (int i = 0; i < len; i++) {
                expected[i] = running;
                running += a[i];
            }
            BlellochScan.Result r = BlellochScan.exclusiveScan(a);
            assertArrayEquals(expected, r.exclusive(), "length " + len);
        }
    }

    @Test
    void scanPerformsLinearWorkAndLogarithmicRounds() {
        BlellochScan.Result r = BlellochScan.exclusiveScan(new long[]{3, 1, 7, 0, 4, 1, 6, 3});
        assertArrayEquals(new long[]{0, 3, 4, 11, 11, 15, 16, 22}, r.exclusive());
        assertArrayEquals(new long[]{3, 4, 11, 11, 15, 16, 22, 25}, r.inclusive());
        assertEquals(8, r.paddedLength());
        assertEquals(2 * 7, r.additions()); // (n-1) up-sweep + (n-1) down-sweep
        assertEquals(6, r.rounds()); // 2 * log2(8)
    }

    @Test
    void scanOfEmptyAndSingleInput() {
        assertEquals(0, BlellochScan.exclusiveScan(new long[0]).exclusive().length);
        assertArrayEquals(new long[]{0}, BlellochScan.exclusiveScan(new long[]{42}).exclusive());
    }

    // ---------------------------------------------------------------- parallel reduce

    @Test
    void parallelReduceEqualsSequentialForAllThreadCounts() {
        Random rnd = new Random(12);
        ExecutorService pool = Executors.newFixedThreadPool(4);
        try {
            for (int trial = 0; trial < 40; trial++) {
                int[] data = new int[rnd.nextInt(5000) + 1];
                for (int i = 0; i < data.length; i++) data[i] = rnd.nextInt(2000) - 1000;
                for (ParallelReduce.Op op : ParallelReduce.Op.values()) {
                    long expected = ParallelReduce.sequential(data, op);
                    for (int chunks : new int[]{1, 2, 3, 4, 7, 16}) {
                        assertEquals(expected, ParallelReduce.parallel(data, op, chunks, pool), op + " chunks=" + chunks);
                    }
                }
            }
        } finally {
            pool.shutdown();
        }
    }

    @Test
    void parallelReduceEdgeCases() {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            assertEquals(0, ParallelReduce.parallel(new int[0], ParallelReduce.Op.SUM, 4, pool));
            assertEquals(5, ParallelReduce.parallel(new int[]{5}, ParallelReduce.Op.SUM, 8, pool));
            assertEquals(-3, ParallelReduce.parallel(new int[]{-3}, ParallelReduce.Op.MAX, 8, pool));
            assertThrows(IllegalArgumentException.class, () -> ParallelReduce.parallel(new int[]{1}, ParallelReduce.Op.SUM, 0, pool));
        } finally {
            pool.shutdown();
        }
    }

    // ---------------------------------------------------------------- Brent's theorem

    @Test
    void brentBoundsForKnownValues() {
        BrentsTheorem.Row r = BrentsTheorem.analyze(1000, 10, 4);
        assertEquals(250.0, r.lower(), 1e-9);
        assertEquals(257.5, r.brentUpper(), 1e-9);
        assertEquals(260.0, r.looseUpper(), 1e-9);
        assertEquals(1000 / 257.5, r.guaranteedSpeedup(), 1e-9);
    }

    @Test
    void boundsAreConsistentForManyProcessorCounts() {
        for (int p = 1; p <= 4096; p *= 2) {
            BrentsTheorem.Row r = BrentsTheorem.analyze(1_000_000, 40, p);
            assertTrue(r.lower() <= r.brentUpper() + 1e-9);
            assertTrue(r.brentUpper() <= r.looseUpper() + 1e-9);
            assertTrue(r.guaranteedSpeedup() <= p + 1e-9);
        }
        assertEquals(1_000_000.0, BrentsTheorem.analyze(1_000_000, 40, 1).brentUpper(), 1e-6); // one processor: T1 = W
        assertEquals(25_000.0, BrentsTheorem.parallelism(1_000_000, 40), 1e-9);
    }

    @Test
    void brentRejectsInvalidInput() {
        assertThrows(IllegalArgumentException.class, () -> BrentsTheorem.analyze(10, 20, 2));
        assertThrows(IllegalArgumentException.class, () -> BrentsTheorem.analyze(0, 0, 2));
        assertThrows(IllegalArgumentException.class, () -> BrentsTheorem.analyze(10, 2, 0));
    }
}
