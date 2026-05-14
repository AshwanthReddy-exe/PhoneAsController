package com.airconsole.common.enums;

/**
 * Lifecycle states of a game room.
 */
public enum RoomStatus {
    WAITING,   // Room created, waiting for players
    PLAYING,   // Game is active
    FINISHED,  // Game completed normally
    EXPIRED    // Room timed out (30 min inactivity)
}
