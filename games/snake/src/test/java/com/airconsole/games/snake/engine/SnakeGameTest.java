package com.airconsole.games.snake.engine;

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

class SnakeGameTest {

    private SnakeGame game;

    @BeforeEach
    void setUp() {
        game = new SnakeGame();
    }

    @Test
    void shouldInitializeCorrectly() {
        UUID p1 = UUID.randomUUID();
        GameContext ctx = new GameContext(UUID.randomUUID(), UUID.randomUUID(), GameType.SNAKE, List.of(p1), 100);
        game.initialize(ctx);

        assertThat(game.isFinished()).isFalse();
        GameSnapshot snap = game.snapshot();
        assertThat(snap.getStatus()).isEqualTo(GameStatus.RUNNING);
        assertThat(snap.getScores()).containsKey(p1);
    }

    @Test
    void shouldProcessInputAndTick() {
        UUID p1 = UUID.randomUUID();
        GameContext ctx = new GameContext(UUID.randomUUID(), UUID.randomUUID(), GameType.SNAKE, List.of(p1), 100);
        game.initialize(ctx);

        game.processInput(new GameInput(p1, ctx.getRoomId(), 1, 1)); // UP
        game.tick();

        GameSnapshot snap = game.snapshot();
        assertThat(snap.getTickNumber()).isEqualTo(1);
    }
}
