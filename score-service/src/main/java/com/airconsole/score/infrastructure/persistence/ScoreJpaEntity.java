package com.airconsole.score.infrastructure.persistence;

import com.airconsole.common.enums.GameType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "scores")
@Getter
@Setter
public class ScoreJpaEntity {

    @Id
    @Column(name = "score_id")
    private UUID scoreId;

    @Column(name = "player_id", nullable = false)
    private UUID playerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "game_type", nullable = false)
    private GameType gameType;

    @Column(name = "room_id", nullable = false)
    private UUID roomId;

    @Column(name = "score_value", nullable = false)
    private int scoreValue;

    @Column(name = "achieved_at", nullable = false)
    private Instant achievedAt;
}
