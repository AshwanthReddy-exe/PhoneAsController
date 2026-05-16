package com.airconsole.common.events;

import com.airconsole.common.enums.GameType;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.Value;

/**
 * Published when game ends (win condition met, all players dead, or room expired).
 * Score-service persists final scores.
 */
@Value
public class GameFinishedEvent {

    UUID gameId;
    UUID roomId;
    GameType gameType;
    UUID winnerId;                // null if no winner (e.g. tie)
    Map<UUID, Integer> finalScores;
    Instant finishedAt;
}
