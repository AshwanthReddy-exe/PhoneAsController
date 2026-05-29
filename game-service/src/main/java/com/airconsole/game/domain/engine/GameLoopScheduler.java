package com.airconsole.game.domain.engine;

import com.airconsole.common.model.GameContext;
import com.airconsole.common.model.GameInput;
import com.airconsole.common.model.GameSnapshot;
import com.airconsole.game.domain.GameSession;
import com.airconsole.game.application.GameEventPublisher;
import com.airconsole.common.events.GameStateUpdatedEvent;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.jctools.queues.MpscUnboundedXaddArrayQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * One dedicated thread per active room.
 * Tick: drain input queue → processInput → tick → snapshot → publish.
 */
public final class GameLoopScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(GameLoopScheduler.class);

    private final EngineRegistry registry;
    private final GameEventPublisher eventPublisher;
    private final ConcurrentMap<UUID, ScheduledExecutorService> roomLoops = new ConcurrentHashMap<>();

    public GameLoopScheduler(EngineRegistry registry, GameEventPublisher eventPublisher) {
        this.registry = registry;
        this.eventPublisher = eventPublisher;
    }

    public void startLoop(GameSession session) {
        UUID gameId = session.gameId();
        if (roomLoops.containsKey(gameId)) {
            return; // already running
        }

        com.airconsole.common.engine.GameEngine engine = session.engine();
        com.airconsole.common.model.GameContext context = session.context();
        org.jctools.queues.MpscUnboundedXaddArrayQueue<com.airconsole.common.model.GameInput> inputQueue = session.inputQueue();
        int tickRateMs = context.getTickRate();

        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(
            r -> new Thread(r, "game-loop-" + gameId)
        );

        executor.scheduleAtFixedRate(() -> tick(session, engine, inputQueue), tickRateMs, tickRateMs, TimeUnit.MILLISECONDS);

        roomLoops.put(gameId, executor);
        LOG.info("Started game loop for {} with tick {}ms", gameId, tickRateMs);
    }

    public void stopLoop(UUID gameId) {
        var executor = roomLoops.remove(gameId);
        if (executor != null) {
            executor.shutdownNow();
            LOG.info("Stopped game loop for {}", gameId);
        }
    }

    private void tick(
            GameSession session,
            com.airconsole.common.engine.GameEngine engine,
            org.jctools.queues.MpscUnboundedXaddArrayQueue<com.airconsole.common.model.GameInput> queue) {
        try {
            com.airconsole.common.model.GameInput input;
            while ((input = queue.relaxedPoll()) != null) {
                engine.processInput(input);
            }
            engine.tick();
            if (engine.isFinished()) {
                LOG.info("Game {} finished naturally — stopping loop", session.gameId());
                stopLoop(session.gameId());
                return;
            }
            com.airconsole.common.model.GameSnapshot snapshot = engine.snapshot();
            session.updateSnapshot(snapshot);
            
            eventPublisher.publish(new GameStateUpdatedEvent(
                session.gameId(),
                session.roomId(),
                snapshot,
                snapshot.getTickNumber(),
                Instant.now()
            ));
        } catch (Exception ex) {
            LOG.error("Tick error for game {}", session.gameId(), ex);
        }
    }
}
