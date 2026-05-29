package com.airconsole.game.domain;

import com.airconsole.common.enums.GameStatus;
import com.airconsole.common.enums.GameType;
import com.airconsole.common.events.GameFinishedEvent;
import com.airconsole.common.events.GamePausedEvent;
import com.airconsole.common.events.GameResumedEvent;
import com.airconsole.common.events.GameStartedEvent;
import com.airconsole.common.model.GameContext;
import com.airconsole.common.model.GameInput;
import com.airconsole.game.application.GameEventPublisher;
import com.airconsole.game.domain.engine.EngineRegistry;
import com.airconsole.game.domain.engine.GameLoopScheduler;
import com.airconsole.game.domain.engine.GameStateManager;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Domain orchestrator — starts, stops, pauses, resumes games.
 */
public class GameOrchestrator {

    private static final Logger LOG = LoggerFactory.getLogger(GameOrchestrator.class);

    private final GameSessionRepository repository;
    private final EngineRegistry registry;
    private final GameLoopScheduler scheduler;
    private final GameStateManager stateManager;
    private final GameEventPublisher eventPublisher;

    public GameOrchestrator(
            GameSessionRepository repository,
            EngineRegistry registry,
            GameLoopScheduler scheduler,
            GameStateManager stateManager,
            GameEventPublisher eventPublisher) {
        this.repository = repository;
        this.registry = registry;
        this.scheduler = scheduler;
        this.stateManager = stateManager;
        this.eventPublisher = eventPublisher;
    }

    public UUID startGame(UUID roomId, GameType gameType, List<UUID> playerIds) {
        com.airconsole.common.engine.GameEngine engine = registry.get(gameType);
        UUID gameId = UUID.randomUUID();
        GameContext context = new GameContext(gameId, roomId, gameType, playerIds, 100);
        GameSession session = new GameSession(gameId, roomId, gameType, engine, context);
        session.start();
        repository.save(session);
        scheduler.startLoop(session);

        eventPublisher.publish(new GameStartedEvent(gameId, roomId, gameType, Instant.now()));
        LOG.info("Started game {} (type={})", gameId, gameType);
        return gameId;
    }

    public void pauseGame(UUID gameId, String reason) {
        repository.findById(gameId).ifPresent(session -> {
            session.pause();
            scheduler.stopLoop(gameId);
            eventPublisher.publish(new GamePausedEvent(gameId, session.roomId(), reason, Instant.now()));
            LOG.info("Paused game {}", gameId);
        });
    }

    public void resumeGame(UUID gameId) {
        repository.findById(gameId).ifPresent(session -> {
            session.resume();
            scheduler.startLoop(session);
            eventPublisher.publish(new GameResumedEvent(gameId, session.roomId(), Instant.now()));
            LOG.info("Resumed game {}", gameId);
        });
    }

    public void finishGame(UUID gameId, UUID winnerId, Map<UUID, Integer> finalScores) {
        repository.findById(gameId).ifPresent(session -> {
            session.finish(winnerId);
            scheduler.stopLoop(gameId);
            eventPublisher.publish(new GameFinishedEvent(
                gameId, session.roomId(), session.gameType(),
                winnerId, finalScores, Instant.now()
            ));
            LOG.info("Finished game {}", gameId);
        });
    }

    public void handleInput(GameInput input) {
        repository.findByRoomId(input.getRoomId()).ifPresent(session -> {
            if (session.status() == GameStatus.RUNNING) {
                session.enqueueInput(input);
            }
        });
    }

    public Object getControllerLayout(GameType gameType) {
        var engine = registry.get(gameType);
        var layout = engine.getControllerLayout();
        return Map.of(
            "gameType", layout.getGameType(),
            "buttons", layout.getButtons(),
            "hasDPad", layout.hasDPad(),
            "hasJoystick", layout.hasJoystick(),
            "keyboardMap", layout.getKeyboardMap()
        );
    }
}
