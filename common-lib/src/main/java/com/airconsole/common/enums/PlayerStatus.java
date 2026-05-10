package com.airconsole.common.enums;

/**
 * Connection state of a player.
 */
public enum PlayerStatus {
    CONNECTED,    // Actively in the room
    DISCONNECTED, // Temporarily offline (rejoin window open)
    RECONNECTING  // In the process of rejoining
}
