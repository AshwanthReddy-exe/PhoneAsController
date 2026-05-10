package com.airconsole.common.exceptions;

/**
 * Thrown on state conflicts (e.g., trying to start a game that is already running).
 */
public class ConflictException extends AirConsoleException {

    public ConflictException(String message) {
        super(message);
    }
}
