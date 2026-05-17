package com.airconsole.common.exceptions;

/**
 * Thrown when controller input or request payload is malformed or out of bounds.
 */
public class InvalidInputException extends AirConsoleException {

    public InvalidInputException(String message) {
        super(message);
    }
}
