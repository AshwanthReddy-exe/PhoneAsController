package com.airconsole.common.dto;

import com.airconsole.common.enums.PlayerRole;
import com.airconsole.common.enums.PlayerStatus;
import java.time.Instant;
import java.util.UUID;
import lombok.Value;

/**
 * Player data returned by REST API.
 */
@Value
public class PlayerDTO {

    UUID playerId;
    UUID roomId;
    String playerName;
    PlayerRole role;
    PlayerStatus status;
    Instant joinedAt;
    Instant lastSeenAt;
}
