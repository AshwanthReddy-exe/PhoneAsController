package com.airconsole.score.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.airconsole.common.enums.GameType;
import com.airconsole.score.domain.LeaderboardService;
import com.airconsole.score.domain.Score;
import com.airconsole.score.domain.ScoreRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LeaderboardServiceTest {

    @Mock
    private ScoreRepository scoreRepository;

    private LeaderboardService leaderboardService;

    @BeforeEach
    void setUp() {
        leaderboardService = new LeaderboardService(scoreRepository);
    }

    @Test
    void shouldRecordGameScores() {
        UUID roomId = UUID.randomUUID();
        UUID player1 = UUID.randomUUID();
        UUID player2 = UUID.randomUUID();
        Instant finishedAt = Instant.now();

        Map<UUID, Integer> finalScores = Map.of(
            player1, 100,
            player2, 200
        );

        leaderboardService.recordGameScores(roomId, GameType.SNAKE, finalScores, finishedAt);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Score>> captor = ArgumentCaptor.forClass(List.class);
        verify(scoreRepository).saveAll(captor.capture());

        List<Score> savedScores = captor.getValue();
        assertThat(savedScores).hasSize(2);
        assertThat(savedScores).extracting(Score::playerId).containsExactlyInAnyOrder(player1, player2);
        assertThat(savedScores).extracting(Score::scoreValue).containsExactlyInAnyOrder(100, 200);
        assertThat(savedScores).extracting(Score::gameType).containsOnly(GameType.SNAKE);
    }

    @Test
    void shouldGetTopScores() {
        UUID p1 = UUID.randomUUID();
        Score s1 = Score.create(p1, GameType.SNAKE, UUID.randomUUID(), 500, Instant.now());
        when(scoreRepository.findTopByGameType(GameType.SNAKE, 10)).thenReturn(List.of(s1));

        List<Score> topScores = leaderboardService.getTopScores(GameType.SNAKE, 10);

        assertThat(topScores).hasSize(1);
        assertThat(topScores.get(0).scoreValue()).isEqualTo(500);
    }
}
