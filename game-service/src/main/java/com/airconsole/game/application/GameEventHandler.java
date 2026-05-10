package com.airconsole.game.application;

import com.airconsole.common.events.PlayerDisconnectedEvent;
import com.airconsole.common.events.PlayerRejoinedEvent;
import com.airconsole.common.events.RoomExpiredEvent;
import com.airconsole.game.domain.GameOrchestrator;
import com.airconsole.game.domain.GameSession;
import com.airconsole.game.domain.GameSessionRepository;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Handles player events from messaging.
 * Pauses game on disconnect, resumes on reconnect.
 * Terminates game on room expired.
 */
@Component
public class GameEventHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GameEventHandler.class);

    private final GameOrchestrator orchestrator;
    private final GameSessionRepository repository;

    public GameEventHandler(GameOrchestrator orchestrator, GameSessionRepository repository) {
        this.orchestrator = orchestrator;
        this.repository = repository;
    }

    public void onPlayerDisconnected(PlayerDisconnectedEvent event)
    {
        LOG.debug("Player disconnected: playerId=" + event.getPlayerId() + ", roomId=" + event.getRoomId());
        repository.findByRoomId(event.getRoomId())
                .ifPresent(session -> orchestrator.pauseGame(session.gameId(), "player_disconnected"));
    }

    public void onPlayerRejoined(PlayerRejoinedEvent event)
    {
        LOG.debug("Player rejoined: playerId=" + event.getPlayerId() + ", roomId=" + event.getRoomId());
        repository.findByRoomId(event.getRoomId())
                .ifPresent(session -> orchestrator.resumeGame(session.gameId()));
    }

    public void onRoomExpired(RoomExpiredEvent event)
    {
        LOG.info("Room expired: roomId=" + event.getRoomId() + ", roomCode=" + event.getRoomCode());
        repository.findByRoomId(event.getRoomId())
                .ifPresent(session -> {
                    orchestrator.finishGame(session.gameId(), null, java.util.Collections.emptyMap());
                    repository.delete(session.gameId());
                });
    }
}
