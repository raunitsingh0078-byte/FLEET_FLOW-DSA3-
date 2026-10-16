package com.fleetflow.ui.panel.string;

import com.fleetflow.util.ValidationException;

/** Shared input validation for the string-algorithm panels. */
final class StringInputs {

    /** The cell grid is drawn on a canvas, so texts are limited to keep the animation readable. */
    static final int MAX_TEXT = 160;

    private StringInputs() {
    }

    static String requireText(String value, String fieldName) throws ValidationException {
        if (value == null || value.isEmpty()) {
            throw new ValidationException("Please enter the " + fieldName + ".");
        }
        if (value.length() > MAX_TEXT) {
            throw new ValidationException("The " + fieldName + " has " + value.length() + " characters; the animation supports at most "
                    + MAX_TEXT + ".");
        }
        if (value.indexOf('\u0000') >= 0) {
            throw new ValidationException("The " + fieldName + " contains an unsupported control character.");
        }
        return value;
    }

    static int parseInt(String value, String fieldName, int min, int max) throws ValidationException {
        try {
            int parsed = Integer.parseInt(value.trim());
            if (parsed < min || parsed > max) {
                throw new ValidationException(fieldName + " must be between " + min + " and " + max + ".");
            }
            return parsed;
        } catch (NumberFormatException ex) {
            throw new ValidationException(fieldName + " must be a whole number (got \"" + value + "\").");
        }
    }
}
