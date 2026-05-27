package com.airconsole.games.pong.engine;

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

class PongGameTest {

    private PongGame game;

    @BeforeEach
    void setUp() {
        game = new PongGame();
    }

    @Test
    void shouldInitializeCorrectly() {
        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();
        GameContext ctx = new GameContext(UUID.randomUUID(), UUID.randomUUID(), GameType.PONG, List.of(p1, p2), 16);
        game.initialize(ctx);

        assertThat(game.isFinished()).isFalse();
        GameSnapshot snap = game.snapshot();
        assertThat(snap.getStatus()).isEqualTo(GameStatus.RUNNING);
        assertThat(snap.getScores()).containsKey(p1).containsKey(p2);
    }

    @Test
    void shouldProcessInputAndTick() {
        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();
        GameContext ctx = new GameContext(UUID.randomUUID(), UUID.randomUUID(), GameType.PONG, List.of(p1, p2), 16);
        game.initialize(ctx);

        game.processInput(new GameInput(p1, ctx.getRoomId(), 3, System.currentTimeMillis()));
        game.tick();

        GameSnapshot snap = game.snapshot();
        assertThat(snap.getTickNumber()).isEqualTo(1);
    }

    @Test
    void shouldScoreWhenBallPassesPaddle() {
        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();
        GameContext ctx = new GameContext(UUID.randomUUID(), UUID.randomUUID(), GameType.PONG, List.of(p1, p2), 16);
        game.initialize(ctx);

        // Run enough ticks for a score to happen naturally
        for (int i = 0; i < 5000; i++) {
            game.tick();
            if (game.isFinished()) {
                break;
            }
        }

        GameSnapshot snap = game.snapshot();
        assertThat(snap.getScores().values().stream().mapToInt(Integer::intValue).sum()).isGreaterThan(0);
    }
}
