package com.fleetflow.visualization;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

import java.util.function.IntConsumer;

/**
 * Plays a pre-computed sequence of frames with play / pause / step / back / reset / speed controls.
 * The algorithm runs once and records its frames; this class only moves a cursor over them.
 */
public final class StepPlayer {

    private static final double BASE_DELAY_MS = 700;

    private final IntConsumer onFrame;
    private final Timeline timeline;
    private int frameCount;
    private int index;

    public StepPlayer(IntConsumer onFrame) {
        this.onFrame = onFrame;
        this.timeline = new Timeline(new KeyFrame(Duration.millis(BASE_DELAY_MS), e -> advanceOrStop()));
        this.timeline.setCycleCount(Animation.INDEFINITE);
    }

    /** Loads a new frame sequence and shows frame 0. */
    public void load(int frames) {
        timeline.stop();
        frameCount = frames;
        index = 0;
        if (frames > 0) {
            onFrame.accept(0);
        }
    }

    public int frameCount() {
        return frameCount;
    }

    public int index() {
        return index;
    }

    public boolean isPlaying() {
        return timeline.getStatus() == Animation.Status.RUNNING;
    }

    /** Plays from the current frame (restarts from the beginning if already at the end). */
    public void play() {
        if (frameCount == 0) {
            return;
        }
        if (index >= frameCount - 1) {
            seek(0);
        }
        timeline.play();
    }

    public void pause() {
        timeline.pause();
    }

    public void next() {
        timeline.pause();
        if (index < frameCount - 1) {
            seek(index + 1);
        }
    }

    public void previous() {
        timeline.pause();
        if (index > 0) {
            seek(index - 1);
        }
    }

    public void reset() {
        timeline.stop();
        if (frameCount > 0) {
            seek(0);
        }
    }

    /** Re-renders the current frame (e.g. after a display option changed). */
    public void refresh() {
        if (frameCount > 0) {
            onFrame.accept(index);
        }
    }

    /** @param multiplier 1.0 = normal speed, 2.0 = twice as fast. */
    public void setSpeed(double multiplier) {
        timeline.setRate(Math.max(0.1, multiplier));
    }

    private void seek(int target) {
        index = target;
        onFrame.accept(index);
    }

    private void advanceOrStop() {
        if (index < frameCount - 1) {
            seek(index + 1);
        }
        if (index >= frameCount - 1) {
            timeline.stop();
        }
    }
}
