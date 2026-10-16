package com.fleetflow.visualization;

import javafx.scene.paint.Color;

/** Colour design tokens shared by all canvas visualisers; every colour has a dark-theme and a light-theme variant. */
public final class Palette {

    private static boolean light;

    /** Muted / secondary text colour (kept as a constant-like accessor for readability at call sites). */
    public static Color mutedText() {
        return light ? Color.web("#5f6c8a") : Color.web("#8592ae");
    }

    private Palette() {
    }

    /** Switches all visualisers between the dark (false) and light (true) variants. */
    public static void setLight(boolean value) {
        light = value;
    }

    /** @return main text colour to draw directly on the card background. */
    public static Color ink() {
        return light ? Color.web("#1c2540") : Color.web("#e7ecf7");
    }

    /** @return the card background colour, used behind small canvas labels. */
    public static Color background() {
        return light ? Color.web("#ffffff") : Color.web("#0d1220");
    }

    /** @return the amber colour used for dashed link arrows. */
    public static Color link() {
        return light ? Color.web("#c77d0a") : Color.web("#f5a623");
    }

    public static Color treeEdge() {
        return light ? Color.web("#9aa9cc") : Color.web("#3a4a75");
    }

    public static Color fill(CellState state) {
        return switch (state) {
            case NORMAL -> light ? Color.web("#e8edf8") : Color.web("#1b2440");
            case ACTIVE -> Color.web("#2f6bea");
            case MATCH -> Color.web("#12a37e");
            case MISMATCH -> Color.web("#d64560");
            case WINDOW -> light ? Color.web("#f0c46a") : Color.web("#9a6b12");
            case DONE -> light ? Color.web("#bfe9dc") : Color.web("#1d5a4b");
            case MUTED -> light ? Color.web("#f3f5fb") : Color.web("#121a2e");
            case HIGHLIGHT -> Color.web("#7c4fe0");
        };
    }

    public static Color stroke(CellState state) {
        return switch (state) {
            case NORMAL -> light ? Color.web("#a9b7d6") : Color.web("#34426b");
            case ACTIVE -> light ? Color.web("#1f4fc0") : Color.web("#7fb0ff");
            case MATCH -> light ? Color.web("#0b7a5d") : Color.web("#5df0c4");
            case MISMATCH -> light ? Color.web("#a82a44") : Color.web("#ff8fa3");
            case WINDOW -> light ? Color.web("#b4841f") : Color.web("#f5c15a");
            case DONE -> light ? Color.web("#4fb597") : Color.web("#2f8f78");
            case MUTED -> light ? Color.web("#d4dcec") : Color.web("#222c47");
            case HIGHLIGHT -> light ? Color.web("#5b34b8") : Color.web("#b79bff");
        };
    }

    /** Text colour readable on top of {@link #fill}. */
    public static Color text(CellState state) {
        if (state == CellState.MUTED) {
            return mutedText();
        }
        if (light && (state == CellState.NORMAL || state == CellState.DONE || state == CellState.WINDOW)) {
            return Color.web("#1c2540");
        }
        return light ? Color.WHITE : Color.web("#e7ecf7");
    }

    /** @return the fill colour of a state as a CSS hex string (for row styling). */
    public static String css(CellState state) {
        Color c = fill(state);
        return String.format("#%02x%02x%02x", (int) Math.round(c.getRed() * 255), (int) Math.round(c.getGreen() * 255),
                (int) Math.round(c.getBlue() * 255));
    }

    /** Edge colour of a state; medium-contrast so thin lines are visible on both backgrounds. */
    public static Color edge(CellState state) {
        return switch (state) {
            case NORMAL -> light ? Color.web("#8a9bc4") : Color.web("#5f74a8");
            case ACTIVE -> light ? Color.web("#2f6bea") : Color.web("#7fb0ff");
            case MATCH -> light ? Color.web("#0e9f78") : Color.web("#5df0c4");
            case MISMATCH -> light ? Color.web("#d6304d") : Color.web("#ff8fa3");
            case WINDOW -> light ? Color.web("#c98f10") : Color.web("#f5c15a");
            case DONE -> light ? Color.web("#2c9a7c") : Color.web("#3aa88e");
            case MUTED -> light ? Color.web("#cdd6ea") : Color.web("#2b3757");
            case HIGHLIGHT -> light ? Color.web("#7c4fe0") : Color.web("#b79bff");
        };
    }
}
