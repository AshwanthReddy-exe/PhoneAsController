package com.airconsole.score.domain;

import com.airconsole.common.enums.GameType;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class LeaderboardService {

    private final ScoreRepository scoreRepository;

    public LeaderboardService(ScoreRepository scoreRepository) {
        this.scoreRepository = scoreRepository;
    }

    public void recordGameScores(UUID roomId, GameType gameType, Map<UUID, Integer> finalScores, Instant finishedAt) {
        List<Score> scores = finalScores.entrySet().stream()
            .map(entry -> Score.create(entry.getKey(), gameType, roomId, entry.getValue(), finishedAt))
            .collect(Collectors.toList());

        scoreRepository.saveAll(scores);
    }

    public List<Score> getTopScores(GameType gameType, int limit) {
        return scoreRepository.findTopByGameType(gameType, limit);
    }

    public List<Score> getPlayerHistory(UUID playerId, int limit) {
        return scoreRepository.findByPlayerId(playerId, limit);
    }
}
