package com.fleetflow.util;

import java.util.Objects;

/** Locates the shared JavaFX stylesheet. */
public final class Theme {

    public static final String STYLESHEET =
            Objects.requireNonNull(Theme.class.getResource("/css/fleetflow.css"), "fleetflow.css missing")
                    .toExternalForm();

    private Theme() {
    }
}
