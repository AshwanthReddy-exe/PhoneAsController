package com.airconsole.common.dto;

import com.airconsole.common.enums.GameType;
import com.airconsole.common.enums.RoomStatus;
import java.time.Instant;
import java.util.UUID;
import lombok.Value;

/**
 * Room data returned by REST API.
 */
@Value
public class RoomDTO {

    UUID roomId;
    String roomCode;
    RoomStatus status;
    UUID hostId;
    GameType gameType;
    int maxPlayers;
    int playerCount;
    Instant createdAt;
    Instant expiresAt;
}
