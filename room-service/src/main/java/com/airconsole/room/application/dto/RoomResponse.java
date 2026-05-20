package com.airconsole.room.application.dto;

import com.airconsole.common.enums.RoomStatus;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO for room REST responses.
 */
public record RoomResponse(
    UUID roomId,
    String roomCode,
    RoomStatus status,
    UUID hostId,
    String gameType,
    int maxPlayers,
    int playerCount,
    Instant createdAt,
    Instant expiresAt
) {}
