package com.airconsole.score.domain;

import com.airconsole.common.enums.GameType;
import java.time.Instant;
import java.util.UUID;

public record Score(
    UUID scoreId,
    UUID playerId,
    GameType gameType,
    UUID roomId,
    int scoreValue,
    Instant achievedAt
) {
    public static Score create(UUID playerId, GameType gameType, UUID roomId, int scoreValue, Instant achievedAt) {
        return new Score(UUID.randomUUID(), playerId, gameType, roomId, scoreValue, achievedAt);
    }
}
