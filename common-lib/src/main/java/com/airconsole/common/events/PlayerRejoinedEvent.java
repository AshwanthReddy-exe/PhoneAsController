package com.airconsole.common.events;

import java.time.Instant;
import java.util.UUID;
import lombok.Value;

/**
 * Published when a disconnected player successfully reconnects within 60s window.
 */
@Value
public class PlayerRejoinedEvent {

    UUID playerId;
    UUID roomId;
    Instant rejoinedAt;
    String newConnectionId;
}
