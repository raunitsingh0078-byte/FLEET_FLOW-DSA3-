package com.fleetflow.algorithm.parallel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * Data-parallel reduction: the array is cut into equally sized chunks, every chunk is reduced by a worker thread of an
 * {@link ExecutorService}, and the partial results are combined. Valid for associative operators such as + and max.
 */
public final class ParallelReduce {

    /** Supported associative operators. */
    public enum Op {
        SUM, MAX;

        long identity() {
            return this == SUM ? 0 : Long.MIN_VALUE;
        }

        long apply(long a, long b) {
            return this == SUM ? a + b : Math.max(a, b);
        }
    }

    private ParallelReduce() {
    }

    /** Single-threaded reduction. */
    public static long sequential(int[] data, Op op) {
        return reduceRange(data, 0, data.length, op);
    }

    /**
     * Reduces with {@code chunks} tasks on the given pool.
     *
     * @throws IllegalStateException if a worker fails or the calling thread is interrupted
     */
    public static long parallel(int[] data, Op op, int chunks, ExecutorService pool) {
        if (chunks < 1) {
            throw new IllegalArgumentException("chunks must be at least 1");
        }
        int n = data.length;
        int parts = Math.min(chunks, Math.max(1, n));
        List<Future<Long>> futures = new ArrayList<>();
        for (int c = 0; c < parts; c++) {
            final int from = (int) ((long) n * c / parts);
            final int to = (int) ((long) n * (c + 1) / parts);
            futures.add(pool.submit(() -> reduceRange(data, from, to, op)));
        }
        long result = op.identity();
        try {
            for (Future<Long> f : futures) {
                result = op.apply(result, f.get());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted while waiting for workers", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("worker failed: " + e.getCause(), e.getCause());
        }
        return result;
    }

    private static long reduceRange(int[] data, int from, int to, Op op) {
        long acc = op.identity();
        if (op == Op.SUM) {
            for (int i = from; i < to; i++) {
                acc += data[i];
            }
        } else {
            for (int i = from; i < to; i++) {
                acc = Math.max(acc, data[i]);
            }
        }
        return acc;
    }
}
