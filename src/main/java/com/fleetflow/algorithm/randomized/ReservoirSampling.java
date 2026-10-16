package com.fleetflow.algorithm.randomized;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Reservoir sampling (Algorithm R): keeps a uniform random sample of k items from a stream of unknown length using
 * O(k) memory. The first k items fill the reservoir; item number i &gt; k replaces a random reservoir slot with
 * probability k/i, which makes every item of the stream equally likely (k/N) to be in the final sample.
 */
public final class ReservoirSampling {

    /**
     * @param item        1-based number of the arriving item
     * @param slot        reservoir slot that received the item, or -1 if it was skipped
     * @param replaced    number of the item that was evicted (0 if the slot was empty or the item skipped)
     * @param probability probability k/i with which the item was to be kept
     * @param reservoir   snapshot of the reservoir (item numbers, 0 = empty slot)
     */
    public record Step(int item, int slot, int replaced, double probability, int[] reservoir) {
    }

    /** Final sample plus trace. */
    public record Result(int[] reservoir, List<Step> steps) {
    }

    private ReservoirSampling() {
    }

    /** Samples k items from a stream of {@code streamLength} items numbered 1..N. */
    public static Result sample(int streamLength, int k, Random rnd) {
        if (k < 1) {
            throw new IllegalArgumentException("reservoir size must be at least 1");
        }
        if (streamLength < 0) {
            throw new IllegalArgumentException("stream length must not be negative");
        }
        int[] reservoir = new int[k];
        List<Step> steps = new ArrayList<>();
        for (int i = 1; i <= streamLength; i++) {
            if (i <= k) {
                reservoir[i - 1] = i;
                steps.add(new Step(i, i - 1, 0, 1.0, reservoir.clone()));
            } else {
                int r = rnd.nextInt(i); // uniform in 0..i-1; keep iff r < k, i.e. with probability k/i
                if (r < k) {
                    int evicted = reservoir[r];
                    reservoir[r] = i;
                    steps.add(new Step(i, r, evicted, (double) k / i, reservoir.clone()));
                } else {
                    steps.add(new Step(i, -1, 0, (double) k / i, reservoir.clone()));
                }
            }
        }
        return new Result(reservoir, steps);
    }

    /** Empirically measures how often each item 1..N ends up in the sample over many independent runs. */
    public static double[] inclusionFrequency(int streamLength, int k, int trials, long seed) {
        Random rnd = new Random(seed);
        int[] counts = new int[streamLength + 1];
        for (int t = 0; t < trials; t++) {
            for (int item : sample(streamLength, k, rnd).reservoir()) {
                if (item > 0) {
                    counts[item]++;
                }
            }
        }
        double[] freq = new double[streamLength];
        for (int i = 1; i <= streamLength; i++) {
            freq[i - 1] = (double) counts[i] / trials;
        }
        return freq;
    }
}
