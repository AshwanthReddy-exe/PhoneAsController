package com.airconsole.common.exceptions;

/**
 * Thrown when a player ID is unknown.
 */
public class PlayerNotFoundException extends AirConsoleException {

    public PlayerNotFoundException(String playerId) {
        super("Player not found: " + playerId);
    }
}
