package com.fleetflow.util;

/** Thrown when user input is invalid; the message is safe to show directly to the user. */
public class ValidationException extends Exception {

    public ValidationException(String message) {
        super(message);
    }
}
