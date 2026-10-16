package com.fleetflow.ui;

import java.util.function.IntConsumer;
import java.util.function.IntFunction;

/**
 * Everything an algorithm run produces for the UI: how many frames it recorded, how to draw a frame,
 * how to narrate a frame, and the summary figures shown in the result panel.
 *
 * @param frames      number of recorded frames (0 if the run has no animation)
 * @param renderer    draws frame i on the JavaFX thread
 * @param narrator    explanation text for frame i
 * @param result      headline result
 * @param complexity  time / space complexity text
 * @param comparisons number of comparisons or -1 if not applicable
 * @param iterations  number of iterations / recorded steps
 * @param info        extra execution information
 */
public record Execution(int frames, IntConsumer renderer, IntFunction<String> narrator, String result,
                        String complexity, long comparisons, long iterations, String info) {
}
