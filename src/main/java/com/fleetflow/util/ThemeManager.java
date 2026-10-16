package com.fleetflow.util;

import com.fleetflow.visualization.Palette;
import javafx.scene.Scene;
import javafx.scene.control.DialogPane;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.prefs.Preferences;

/** Switches between the dark and the light theme, remembers the choice and tells canvases to repaint. */
public final class ThemeManager {

    private static final String LIGHT_STYLESHEET =
            Objects.requireNonNull(ThemeManager.class.getResource("/css/fleetflow-light.css"), "fleetflow-light.css missing")
                    .toExternalForm();
    private static final String PREF_KEY = "lightTheme";

    private static final List<Runnable> LISTENERS = new ArrayList<>();
    private static Scene scene;
    private static boolean light;

    private ThemeManager() {
    }

    /** Attaches the manager to the main scene and restores the saved preference. */
    public static void init(Scene mainScene) {
        scene = mainScene;
        try {
            light = Preferences.userNodeForPackage(ThemeManager.class).getBoolean(PREF_KEY, false);
        } catch (RuntimeException e) {
            light = false; // preferences unavailable: fall back to dark
        }
        apply();
    }

    public static boolean isLight() {
        return light;
    }

    /** Toggles the theme, persists it and notifies listeners. */
    public static void toggle() {
        light = !light;
        try {
            Preferences.userNodeForPackage(ThemeManager.class).putBoolean(PREF_KEY, light);
        } catch (RuntimeException e) {
            // not persisting is harmless
        }
        apply();
        for (Runnable listener : List.copyOf(LISTENERS)) {
            listener.run();
        }
    }

    /** Registers a callback executed after every theme change (used by canvases to repaint). */
    public static void addListener(Runnable listener) {
        LISTENERS.add(listener);
    }

    /** Applies the current theme to a dialog. */
    public static void style(DialogPane pane) {
        pane.getStylesheets().add(Theme.STYLESHEET);
        if (light) {
            pane.getStylesheets().add(LIGHT_STYLESHEET);
        }
    }

    private static void apply() {
        Palette.setLight(light);
        if (scene == null) {
            return;
        }
        scene.getStylesheets().remove(LIGHT_STYLESHEET);
        if (light) {
            scene.getStylesheets().add(LIGHT_STYLESHEET);
        }
    }
}
