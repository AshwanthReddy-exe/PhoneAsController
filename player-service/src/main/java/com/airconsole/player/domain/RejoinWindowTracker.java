package com.airconsole.player.domain;

import java.time.Instant;

/**
 * Pure logic for 60-second rejoin window.
 * Service calls this every 10s via scheduler.
 */
public final class RejoinWindowTracker {

    public static final long WINDOW_SECONDS = 60L;

    public boolean isExpired(Instant lastSeen) {
        return Instant.now().isAfter(lastSeen.plusSeconds(WINDOW_SECONDS));
    }

    public boolean isWithinWindow(Instant lastSeen) {
        return !isExpired(lastSeen);
    }

    public long remainingSeconds(Instant lastSeen) {
        long remaining = WINDOW_SECONDS
            - (Instant.now().getEpochSecond() - lastSeen.getEpochSecond());
        return Math.max(remaining, 0L);
    }
}
