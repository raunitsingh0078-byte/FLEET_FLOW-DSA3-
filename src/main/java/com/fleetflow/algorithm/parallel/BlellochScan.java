package com.fleetflow.algorithm.parallel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Blelloch work-efficient parallel prefix sum (exclusive scan). The array is padded to a power of two n and viewed as a
 * balanced binary tree: the <em>up-sweep</em> computes partial sums in the tree (n − 1 additions, log₂ n parallel rounds),
 * the root is set to 0, and the <em>down-sweep</em> pushes prefixes down (n − 1 additions, log₂ n rounds).
 * Work O(n), span O(log n). All additions inside one round are independent and could run in parallel.
 */
public final class BlellochScan {

    /** Phase of the algorithm. */
    public enum Phase { INPUT, UPSWEEP, ZERO_ROOT, DOWNSWEEP }

    /**
     * @param level     tree level (distance 2^level between the combined nodes) for sweeps
     * @param leftIdx   indices of the left operands of the additions / swaps in this round
     * @param rightIdx  indices that are updated in this round
     * @param snapshot  array after the round
     */
    public record Step(Phase phase, int level, int[] leftIdx, int[] rightIdx, long[] snapshot) {
    }

    /** @param exclusive exclusive prefix sums of the (unpadded) input; @param additions total additions performed */
    public record Result(long[] exclusive, long[] inclusive, int paddedLength, List<Step> steps, long additions, int rounds) {
    }

    private BlellochScan() {
    }

    public static Result exclusiveScan(long[] input) {
        int n = 1;
        while (n < Math.max(1, input.length)) {
            n <<= 1;
        }
        long[] a = Arrays.copyOf(input, n);
        List<Step> steps = new ArrayList<>();
        steps.add(new Step(Phase.INPUT, -1, new int[0], new int[0], a.clone()));
        long additions = 0;
        int rounds = 0;
        int levels = Integer.numberOfTrailingZeros(n);
        for (int d = 0; d < levels; d++) { // up-sweep (reduce)
            int stride = 1 << (d + 1);
            List<Integer> lefts = new ArrayList<>();
            List<Integer> rights = new ArrayList<>();
            for (int i = 0; i < n; i += stride) {
                int left = i + (1 << d) - 1;
                int right = i + stride - 1;
                a[right] += a[left];
                additions++;
                lefts.add(left);
                rights.add(right);
            }
            rounds++;
            steps.add(new Step(Phase.UPSWEEP, d, toArray(lefts), toArray(rights), a.clone()));
        }
        a[n - 1] = 0; // clear the root
        steps.add(new Step(Phase.ZERO_ROOT, levels, new int[0], new int[]{n - 1}, a.clone()));
        for (int d = levels - 1; d >= 0; d--) { // down-sweep
            int stride = 1 << (d + 1);
            List<Integer> lefts = new ArrayList<>();
            List<Integer> rights = new ArrayList<>();
            for (int i = 0; i < n; i += stride) {
                int left = i + (1 << d) - 1;
                int right = i + stride - 1;
                long t = a[left];
                a[left] = a[right];
                a[right] += t;
                additions++;
                lefts.add(left);
                rights.add(right);
            }
            rounds++;
            steps.add(new Step(Phase.DOWNSWEEP, d, toArray(lefts), toArray(rights), a.clone()));
        }
        long[] exclusive = Arrays.copyOf(a, input.length);
        long[] inclusive = new long[input.length];
        for (int i = 0; i < input.length; i++) {
            inclusive[i] = exclusive[i] + input[i];
        }
        return new Result(exclusive, inclusive, n, steps, additions, rounds);
    }

    private static int[] toArray(List<Integer> list) {
        return list.stream().mapToInt(Integer::intValue).toArray();
    }
}
