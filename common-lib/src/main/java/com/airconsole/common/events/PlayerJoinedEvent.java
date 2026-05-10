package com.airconsole.common.events;

import com.airconsole.common.enums.PlayerRole;
import java.time.Instant;
import java.util.UUID;
import lombok.Value;

/**
 * Published when a player successfully joins a room.
 */
@Value
public class PlayerJoinedEvent {

    UUID playerId;
    UUID roomId;
    String playerName;
    PlayerRole role;
    Instant joinedAt;
}
