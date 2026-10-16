package com.fleetflow.controller;

import com.fleetflow.ui.Page;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Owns the list of routes, lazily creates pages and remembers which one is currently shown. */
public final class NavigationController {

    /** A sidebar entry. */
    public record Route(String icon, String label, Supplier<Page> factory) {
    }

    private List<Route> routes = List.of();
    private final Map<Route, Page> cache = new HashMap<>();
    private final ObjectProperty<Route> current = new SimpleObjectProperty<>();
    private Consumer<Route> navigator = route -> { };

    /** Registers the sidebar routes (done once at start-up, after pages that need this controller exist). */
    public void setRoutes(List<Route> routes) {
        this.routes = List.copyOf(routes);
    }

    /** Installed by the shell so that pages can request navigation. */
    public void setNavigator(Consumer<Route> navigator) {
        this.navigator = navigator;
    }

    /**
     * Navigates to a page by its sidebar label and optionally focuses / runs one algorithm on it.
     *
     * @param label     sidebar label, e.g. "Network Flow"
     * @param algorithm algorithm to bring to the front (may be null)
     * @param run       whether to load demo data and execute it
     */
    public void goTo(String label, String algorithm, boolean run) {
        for (Route route : routes) {
            if (route.label().equals(label)) {
                navigator.accept(route);
                if (algorithm != null) {
                    cache.get(route).focus(algorithm, run);
                }
                return;
            }
        }
    }

    public List<Route> routes() {
        return routes;
    }

    public ObjectProperty<Route> currentProperty() {
        return current;
    }

    /** Returns the (cached) page for a route and notifies it that it is now visible. */
    public Page open(Route route) {
        Page page = cache.computeIfAbsent(route, r -> r.factory().get());
        current.set(route);
        page.onShow();
        return page;
    }
}
