package com.airconsole.games.trivia.engine;

import static org.assertj.core.api.Assertions.assertThat;

import com.airconsole.common.enums.GameStatus;
import com.airconsole.common.enums.GameType;
import com.airconsole.common.model.GameContext;
import com.airconsole.common.model.GameInput;
import com.airconsole.common.model.GameSnapshot;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TriviaGameTest {

    private TriviaGame game;

    @BeforeEach
    void setUp() {
        game = new TriviaGame();
    }

    @Test
    void shouldInitializeCorrectly() {
        UUID p1 = UUID.randomUUID();
        GameContext ctx = new GameContext(UUID.randomUUID(), UUID.randomUUID(), GameType.TRIVIA, List.of(p1), 1000);
        game.initialize(ctx);

        assertThat(game.isFinished()).isFalse();
        GameSnapshot snap = game.snapshot();
        assertThat(snap.getStatus()).isEqualTo(GameStatus.RUNNING);
        assertThat(snap.getScores()).containsKey(p1);
    }

    @Test
    void shouldProcessAnswerInput() {
        UUID p1 = UUID.randomUUID();
        GameContext ctx = new GameContext(UUID.randomUUID(), UUID.randomUUID(), GameType.TRIVIA, List.of(p1), 1000);
        game.initialize(ctx);

        game.processInput(new GameInput(p1, ctx.getRoomId(), 5, System.currentTimeMillis()));
        game.tick();

        GameSnapshot snap = game.snapshot();
        assertThat(snap.getTickNumber()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void shouldFinishAllRounds() {
        UUID p1 = UUID.randomUUID();
        GameContext ctx = new GameContext(UUID.randomUUID(), UUID.randomUUID(), GameType.TRIVIA, List.of(p1), 1000);
        game.initialize(ctx);

        // Fast-forward through 5 rounds at 1-second ticks
        for (int i = 0; i < 100; i++) {
            game.tick();
            if (game.isFinished()) {
                break;
            }
        }

        assertThat(game.isFinished()).isTrue();
    }
}
