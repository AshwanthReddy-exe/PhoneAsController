package com.airconsole.common.events;

import java.time.Instant;
import java.util.UUID;
import lombok.Value;

/**
 * Published when a player's rejoin window expires (60s elapsed without reconnect).
 */
@Value
public class PlayerLeftEvent {

    UUID playerId;
    UUID roomId;
    Instant leftAt;
}
