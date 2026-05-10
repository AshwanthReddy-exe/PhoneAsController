package com.airconsole.common.exceptions;

/**
 * Base exception for all AirConsole domain errors.
 * Every custom exception extends this so callers can catch everything at once.
 */
public class AirConsoleException extends RuntimeException {

    public AirConsoleException(String message) {
        super(message);
    }

    public AirConsoleException(String message, Throwable cause) {
        super(message, cause);
    }
}
