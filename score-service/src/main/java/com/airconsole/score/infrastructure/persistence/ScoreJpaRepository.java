package com.airconsole.score.infrastructure.persistence;

import com.airconsole.common.enums.GameType;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScoreJpaRepository extends JpaRepository<ScoreJpaEntity, UUID> {

    Page<ScoreJpaEntity> findByGameTypeOrderByScoreValueDesc(GameType gameType, Pageable pageable);

    Page<ScoreJpaEntity> findByPlayerIdOrderByAchievedAtDesc(UUID playerId, Pageable pageable);
}
