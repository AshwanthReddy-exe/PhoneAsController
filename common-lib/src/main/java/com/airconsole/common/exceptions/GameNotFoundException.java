package com.airconsole.common.exceptions;

/**
 * Thrown when a game session or engine is not found.
 */
public class GameNotFoundException extends AirConsoleException {

    public GameNotFoundException(String gameId) {
        super("Game not found: " + gameId);
    }
}
