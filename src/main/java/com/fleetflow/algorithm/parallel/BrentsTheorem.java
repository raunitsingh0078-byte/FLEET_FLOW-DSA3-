package com.fleetflow.algorithm.parallel;

/**
 * Brent's theorem (greedy-scheduling bound): a computation with total work W (T₁) and span / critical path S (T∞) can be
 * scheduled on P processors in time T<sub>P</sub> ≤ (W − S)/P + S ≤ W/P + S = O(W/P + S). No schedule can beat the lower
 * bound max(W/P, S).
 */
public final class BrentsTheorem {

    /**
     * @param lower          max(W/P, S) – nothing can be faster
     * @param brentUpper     (W − S)/P + S – a greedy scheduler achieves this
     * @param looseUpper     W/P + S – the usual O(W/P + S) form
     * @param guaranteedSpeedup W / brentUpper
     * @param efficiency     guaranteedSpeedup / P
     */
    public record Row(int processors, double lower, double brentUpper, double looseUpper, double guaranteedSpeedup,
                      double efficiency) {
    }

    private BrentsTheorem() {
    }

    public static Row analyze(double work, double span, int processors) {
        if (work < 1 || span < 1 || span > work) {
            throw new IllegalArgumentException("require work ≥ span ≥ 1");
        }
        if (processors < 1) {
            throw new IllegalArgumentException("processors must be at least 1");
        }
        double lower = Math.max(work / processors, span);
        double brent = (work - span) / processors + span;
        double loose = work / processors + span;
        double speedup = work / brent;
        return new Row(processors, lower, brent, loose, speedup, speedup / processors);
    }

    /** @return the parallelism W/S: more processors than this cannot help asymptotically. */
    public static double parallelism(double work, double span) {
        return work / span;
    }
}
