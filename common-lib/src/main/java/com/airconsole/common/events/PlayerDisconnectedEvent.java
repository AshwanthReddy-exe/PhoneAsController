package com.airconsole.common.events;

import java.time.Instant;
import java.util.UUID;
import lombok.Value;

/**
 * Published when a player disconnects (SignalR drop detected).
 * Triggers rejoin window (60s) in player-service.
 */
@Value
public class PlayerDisconnectedEvent {

    UUID playerId;
    UUID roomId;
    Instant disconnectedAt;
}
