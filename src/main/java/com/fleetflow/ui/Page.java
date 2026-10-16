package com.fleetflow.ui;

import javafx.scene.Node;

/** A screen shown in the main content area. */
public interface Page {

    String title();

    String subtitle();

    /** Builds the page's root node; called once and cached by the navigation controller. */
    Node content();

    /** Called every time the page becomes visible so it can refresh data-dependent widgets. */
    default void onShow() {
    }

    /**
     * Brings a specific algorithm to the front (used by Algorithm Explorer and Demo Mode).
     *
     * @param algorithm catalogue name of the algorithm
     * @param run       when true the algorithm's demo data is loaded and the algorithm is executed
     */
    default void focus(String algorithm, boolean run) {
    }
}
