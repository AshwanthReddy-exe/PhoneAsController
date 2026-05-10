package com.airconsole.room.api.request;

import jakarta.validation.constraints.NotBlank;

/**
 * POST /api/rooms/join request body.
 */
public record JoinRoomRequest(
    @NotBlank String roomCode
) {}
