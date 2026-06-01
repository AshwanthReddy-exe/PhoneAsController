package com.airconsole.score.application.dto;

import com.airconsole.common.enums.GameType;
import com.airconsole.score.domain.Score;
import java.time.Instant;
import java.util.UUID;

public record ScoreResponse(
    UUID scoreId,
    UUID playerId,
    GameType gameType,
    UUID roomId,
    int scoreValue,
    Instant achievedAt
) {
    public static ScoreResponse fromDomain(Score score) {
        return new ScoreResponse(
            score.scoreId(),
            score.playerId(),
            score.gameType(),
            score.roomId(),
            score.scoreValue(),
            score.achievedAt()
        );
    }
}
