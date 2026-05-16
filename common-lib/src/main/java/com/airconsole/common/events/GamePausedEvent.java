package com.airconsole.common.events;

import java.time.Instant;
import java.util.UUID;
import lombok.Value;

/**
 * Published when game pauses (player disconnected mid-match).
 */
@Value
public class GamePausedEvent {

    UUID gameId;
    UUID roomId;
    String reason;        // e.g. "player.disconnected"
    Instant pausedAt;
}
