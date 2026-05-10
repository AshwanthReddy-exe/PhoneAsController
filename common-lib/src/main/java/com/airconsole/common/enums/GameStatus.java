package com.airconsole.common.enums;

/**
 * State of an individual game session.
 */
public enum GameStatus {
    WAITING,  // Initialized, not yet started
    RUNNING,  // Active tick loop
    PAUSED,   // Waiting for reconnect
    FINISHED  // Completed
}
