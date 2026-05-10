package com.airconsole.common.events;

import com.airconsole.common.enums.GameType;
import java.time.Instant;
import java.util.UUID;
import lombok.Value;

/**
 * Published when a new room is created.
 */
@Value
public class RoomCreatedEvent {

    UUID roomId;
    String roomCode;
    GameType gameType;
    UUID hostId;
    int maxPlayers;
    Instant createdAt;
}
