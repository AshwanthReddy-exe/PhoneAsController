package com.airconsole.player.application.dto;

import com.airconsole.common.enums.PlayerRole;
import com.airconsole.common.enums.PlayerStatus;
import java.time.Instant;
import java.util.UUID;

public record PlayerListResponse(
    UUID playerId,
    String playerName,
    PlayerRole role,
    PlayerStatus status,
    Instant joinedAt
) {}
