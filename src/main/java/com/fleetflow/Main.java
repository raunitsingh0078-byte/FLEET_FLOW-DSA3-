package com.fleetflow;

import com.fleetflow.controller.NavigationController;
import com.fleetflow.ui.AppShell;
import com.fleetflow.ui.PageCatalog;
import com.fleetflow.util.Dialogs;
import com.fleetflow.util.Theme;
import com.fleetflow.util.ThemeManager;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

/** FleetFlow application entry point. */
public class Main extends Application {

    @Override
    public void start(Stage stage) {
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
            error.printStackTrace();
            Platform.runLater(() -> Dialogs.error("Unexpected problem", String.valueOf(error.getMessage())));
        });
        NavigationController navigation = new NavigationController();
        navigation.setRoutes(PageCatalog.routes(navigation));
        AppShell shell = new AppShell(navigation);
        Scene scene = new Scene(shell, 1400, 880);
        scene.getStylesheets().add(Theme.STYLESHEET);
        ThemeManager.init(scene);
        stage.setTitle("FleetFlow – Intelligent Logistics Management Platform");
        stage.setMinWidth(1100);
        stage.setMinHeight(700);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
