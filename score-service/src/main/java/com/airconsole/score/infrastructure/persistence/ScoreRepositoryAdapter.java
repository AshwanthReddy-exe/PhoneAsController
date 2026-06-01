package com.airconsole.score.infrastructure.persistence;

import com.airconsole.common.enums.GameType;
import com.airconsole.score.domain.Score;
import com.airconsole.score.domain.ScoreRepository;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
public class ScoreRepositoryAdapter implements ScoreRepository {

    private final ScoreJpaRepository jpaRepository;

    public ScoreRepositoryAdapter(ScoreJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Score save(Score score) {
        ScoreJpaEntity entity = toEntity(score);
        return toDomain(jpaRepository.save(entity));
    }

    @Override
    public void saveAll(List<Score> scores) {
        List<ScoreJpaEntity> entities = scores.stream()
            .map(this::toEntity)
            .collect(Collectors.toList());
        jpaRepository.saveAll(entities);
    }

    @Override
    public List<Score> findTopByGameType(GameType gameType, int limit) {
        return jpaRepository.findByGameTypeOrderByScoreValueDesc(gameType, PageRequest.of(0, limit))
            .stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Score> findByPlayerId(UUID playerId, int limit) {
        return jpaRepository.findByPlayerIdOrderByAchievedAtDesc(playerId, PageRequest.of(0, limit))
            .stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    private ScoreJpaEntity toEntity(Score score) {
        ScoreJpaEntity e = new ScoreJpaEntity();
        e.setScoreId(score.scoreId());
        e.setPlayerId(score.playerId());
        e.setGameType(score.gameType());
        e.setRoomId(score.roomId());
        e.setScoreValue(score.scoreValue());
        e.setAchievedAt(score.achievedAt());
        return e;
    }

    private Score toDomain(ScoreJpaEntity e) {
        return new Score(
            e.getScoreId(),
            e.getPlayerId(),
            e.getGameType(),
            e.getRoomId(),
            e.getScoreValue(),
            e.getAchievedAt()
        );
    }
}
