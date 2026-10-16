package com.fleetflow.ui;

import com.fleetflow.controller.NavigationController;
import com.fleetflow.controller.NavigationController.Route;
import com.fleetflow.util.ThemeManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.HashMap;
import java.util.Map;

/** Main window layout: brand + sidebar navigation on the left, top bar and page host on the right. */
public final class AppShell extends BorderPane {

    private final NavigationController navigation;
    private final StackPane pageHost = new StackPane();
    private final Label pageTitle = Ui.label("", "page-title");
    private final Label pageSubtitle = Ui.label("", "muted");
    private final Map<Route, Button> buttons = new HashMap<>();
    private final Map<Route, ScrollPane> scrollers = new HashMap<>();

    public AppShell(NavigationController navigation) {
        this.navigation = navigation;
        setLeft(buildSidebar());
        setCenter(buildMain());
        navigation.setNavigator(this::navigate);
        navigation.currentProperty().addListener((obs, old, route) -> markSelected(route));
        navigate(navigation.routes().get(0));
    }

    private VBox buildSidebar() {
        Label brand = Ui.label("🚚 FLEETFLOW", "brand-title");
        Label tagline = Ui.label("Intelligent Logistics\nManagement Platform", "brand-subtitle");
        VBox nav = new VBox(4);
        for (Route route : navigation.routes()) {
            Button button = new Button(route.icon() + "   " + route.label());
            button.getStyleClass().add("nav-item");
            button.setMaxWidth(Double.MAX_VALUE);
            button.setOnAction(e -> navigate(route));
            buttons.put(route, button);
            nav.getChildren().add(button);
        }
        ScrollPane scroll = new ScrollPane(nav);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("nav-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        VBox sidebar = new VBox(6, new VBox(2, brand, tagline), scroll);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPadding(new Insets(20, 12, 12, 12));
        sidebar.setPrefWidth(272);
        sidebar.setMinWidth(272);
        return sidebar;
    }

    private BorderPane buildMain() {
        VBox titles = new VBox(2, pageTitle, pageSubtitle);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label admin = Ui.label("👤 Admin", "chip");
        Label status = Ui.label("● System online", "chip-ok");
        Button themeToggle = new Button();
        themeToggle.getStyleClass().addAll("button-secondary", "theme-toggle");
        Runnable refreshLabel = () -> themeToggle.setText(ThemeManager.isLight() ? "🌙 Dark mode" : "☀ Light mode");
        themeToggle.setTooltip(new javafx.scene.control.Tooltip("Switch between the dark and the light theme"));
        themeToggle.setOnAction(e -> {
            ThemeManager.toggle();
            refreshLabel.run();
        });
        ThemeManager.addListener(refreshLabel);
        refreshLabel.run();
        HBox topBar = new HBox(14, titles, spacer, status, themeToggle, admin);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.getStyleClass().add("topbar");
        topBar.setPadding(new Insets(14, 24, 14, 24));
        BorderPane main = new BorderPane();
        main.setTop(topBar);
        main.setCenter(pageHost);
        return main;
    }

    private void navigate(Route route) {
        Page page = navigation.open(route);
        pageTitle.setText(page.title());
        pageSubtitle.setText(page.subtitle());
        ScrollPane scroll = scrollers.computeIfAbsent(route, r -> {
            ScrollPane created = new ScrollPane(page.content());
            created.setFitToWidth(true);
            created.getStyleClass().add("page-scroll");
            return created;
        });
        pageHost.getChildren().setAll(scroll);
    }

    private void markSelected(Route selected) {
        buttons.forEach((route, button) -> {
            button.getStyleClass().remove("selected");
            if (route.equals(selected)) {
                button.getStyleClass().add("selected");
            }
        });
    }
}
