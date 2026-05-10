package com.airconsole.game.domain;

import com.airconsole.common.enums.GameStatus;
import com.airconsole.common.enums.GameType;
import com.airconsole.common.engine.GameEngine;
import com.airconsole.common.model.GameContext;
import com.airconsole.common.model.GameInput;
import com.airconsole.common.model.GameSnapshot;
import java.time.Instant;
import java.util.UUID;

/**
 * Aggregate root — one running or paused game.
 * Holds input queue + scheduling context.
 */
public class GameSession {

    private final UUID gameId;
    private final UUID roomId;
    private final GameType gameType;
    private final GameEngine engine;
    private final GameContext context;
    private final org.jctools.queues.MpscUnboundedXaddArrayQueue<GameInput> inputQueue;
    private GameSnapshot latestSnapshot;
    private Instant startedAt;
    private Instant pausedAt;
    private GameStatus status;

    public GameSession(UUID gameId, UUID roomId, GameType type, GameEngine engine, GameContext context) {
        this.gameId = gameId;
        this.roomId = roomId;
        this.gameType = type;
        this.engine = engine;
        this.context = context;
        this.inputQueue = new org.jctools.queues.MpscUnboundedXaddArrayQueue<>(256);
        this.status = GameStatus.WAITING;
    }

    public void start() {
        this.status = GameStatus.RUNNING;
        this.startedAt = Instant.now();
        engine.initialize(context);
    }

    public void pause() {
        this.status = GameStatus.PAUSED;
        this.pausedAt = Instant.now();
    }

    public void resume() {
        this.status = GameStatus.RUNNING;
    }

    public void finish(UUID winnerId) {
        this.status = GameStatus.FINISHED;
    }

    public void enqueueInput(GameInput input) {
        inputQueue.relaxedOffer(input);
    }

    public void updateSnapshot(GameSnapshot snapshot) {
        this.latestSnapshot = snapshot;
    }

    // Getters
    public UUID gameId() { return gameId; }
    public UUID roomId() { return roomId; }
    public GameType gameType() { return gameType; }
    public GameEngine engine() { return engine; }
    public GameContext context() { return context; }
    public org.jctools.queues.MpscUnboundedXaddArrayQueue<GameInput> inputQueue() { return inputQueue; }
    public GameSnapshot latestSnapshot() { return latestSnapshot; }
    public GameStatus status() { return status; }
    public Instant startedAt() { return startedAt; }
    public Instant pausedAt() { return pausedAt; }
}
