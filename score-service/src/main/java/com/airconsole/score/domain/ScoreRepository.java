package com.airconsole.score.domain;

import com.airconsole.common.enums.GameType;
import java.util.List;
import java.util.UUID;

public interface ScoreRepository {

    Score save(Score score);

    void saveAll(List<Score> scores);

    List<Score> findTopByGameType(GameType gameType, int limit);

    List<Score> findByPlayerId(UUID playerId, int limit);
}
