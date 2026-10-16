package com.fleetflow.algorithm.randomized;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * QuickSort with Lomuto partitioning and a <em>random pivot</em>: because the pivot is chosen uniformly at random,
 * no fixed input (such as an already sorted list) is consistently bad, giving expected O(n log n) comparisons.
 * The recursion always descends into the smaller side and loops on the larger one, so the stack depth is O(log n).
 */
public final class RandomizedQuickSort {

    /** Kind of a recorded event. */
    public enum Kind { PIVOT, COMPARE, SWAP, PLACE }

    /**
     * @param lo,hi       the sub-array being partitioned
     * @param i           end of the "≤ pivot" region (PLACE: final index of the pivot)
     * @param j           scan position
     * @param snapshot    copy of the whole array after this event
     * @param comparisons comparisons so far
     */
    public record Step(Kind kind, int lo, int hi, int i, int j, int[] snapshot, long comparisons) {
    }

    /** Result of a sort. */
    public record Result(int[] sorted, long comparisons, long swaps, List<Step> steps, int maxDepth) {
    }

    private RandomizedQuickSort() {
    }

    /**
     * @param randomPivot true = uniformly random pivot; false = always the last element (classic, deterministic)
     * @param record      whether to record animation steps (only for small arrays)
     */
    public static Result sort(int[] input, boolean randomPivot, long seed, boolean record) {
        Objects.requireNonNull(input, "input");
        State s = new State(input.clone(), randomPivot, new Random(seed), record);
        s.sort(0, input.length - 1, 1);
        return new Result(s.a, s.comparisons, s.swaps, s.steps, s.maxDepth);
    }

    private static final class State {
        final int[] a;
        final boolean randomPivot;
        final Random rnd;
        final boolean record;
        final List<Step> steps = new ArrayList<>();
        long comparisons;
        long swaps;
        int maxDepth;

        State(int[] a, boolean randomPivot, Random rnd, boolean record) {
            this.a = a;
            this.randomPivot = randomPivot;
            this.rnd = rnd;
            this.record = record;
        }

        void sort(int lo, int hi, int depth) {
            maxDepth = Math.max(maxDepth, depth);
            while (lo < hi) {
                int p = partition(lo, hi);
                if (p - lo < hi - p) { // recurse on the smaller half, loop on the larger
                    sort(lo, p - 1, depth + 1);
                    lo = p + 1;
                } else {
                    sort(p + 1, hi, depth + 1);
                    hi = p - 1;
                }
            }
            if (lo == hi && record) {
                steps.add(new Step(Kind.PLACE, lo, hi, lo, lo, a.clone(), comparisons)); // single element is in place
            }
        }

        int partition(int lo, int hi) {
            if (randomPivot) {
                int r = lo + rnd.nextInt(hi - lo + 1);
                swap(r, hi);
            }
            int pivot = a[hi];
            log(Kind.PIVOT, lo, hi, lo - 1, lo);
            int i = lo - 1;
            for (int j = lo; j < hi; j++) {
                comparisons++;
                log(Kind.COMPARE, lo, hi, i, j);
                if (a[j] <= pivot) {
                    i++;
                    if (i != j) {
                        swap(i, j);
                        log(Kind.SWAP, lo, hi, i, j);
                    }
                }
            }
            swap(i + 1, hi);
            log(Kind.PLACE, lo, hi, i + 1, i + 1);
            return i + 1;
        }

        void swap(int x, int y) {
            if (x != y) {
                int t = a[x];
                a[x] = a[y];
                a[y] = t;
                swaps++;
            }
        }

        void log(Kind kind, int lo, int hi, int i, int j) {
            if (record) {
                steps.add(new Step(kind, lo, hi, i, j, a.clone(), comparisons));
            }
        }
    }
}
