package com.airconsole.room.api.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * POST /api/rooms request body.
 */
public record CreateRoomRequest(
    @NotBlank String gameType,
    @NotNull UUID hostId,
    @Min(2) @Max(16) int maxPlayers
) {}
