package com.airconsole.common.exceptions;

/**
 * Thrown when a room code or ID does not resolve to an existing room.
 */
public class RoomNotFoundException extends AirConsoleException {

    public RoomNotFoundException(String roomCodeOrId) {
        super("Room not found: " + roomCodeOrId);
    }
}
