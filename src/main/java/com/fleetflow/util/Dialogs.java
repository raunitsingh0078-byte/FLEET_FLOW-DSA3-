package com.fleetflow.util;

import javafx.scene.control.Alert;

/** Small helpers for user-friendly, themed alerts. */
public final class Dialogs {

    private Dialogs() {
    }

    public static void error(String header, String message) {
        show(Alert.AlertType.ERROR, "Error", header, message);
    }

    public static void warning(String header, String message) {
        show(Alert.AlertType.WARNING, "Please check your input", header, message);
    }

    public static void info(String header, String message) {
        show(Alert.AlertType.INFORMATION, "FleetFlow", header, message);
    }

    private static void show(Alert.AlertType type, String title, String header, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(message);
        ThemeManager.style(alert.getDialogPane());
        alert.showAndWait();
    }
}
