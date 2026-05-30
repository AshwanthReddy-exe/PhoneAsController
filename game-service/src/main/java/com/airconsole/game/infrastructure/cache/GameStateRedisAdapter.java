package com.airconsole.game.infrastructure.cache;

import com.airconsole.game.domain.GameSession;
import com.airconsole.game.domain.GameSessionRepository;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * In-memory game session cache. Primary storage — Redis only checkouts.
 */
@Component
public class GameStateRedisAdapter implements GameSessionRepository {

    private final Map<UUID, GameSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void save(GameSession session) {
        sessions.put(session.gameId(), session);
    }

    @Override
    public Optional<GameSession> findById(UUID gameId) {
        return Optional.ofNullable(sessions.get(gameId));
    }

    @Override
    public Optional<GameSession> findByRoomId(UUID roomId) {
        return sessions.values().stream()
            .filter(s -> s.roomId().equals(roomId))
            .findFirst();
    }

    @Override
    public void delete(UUID gameId) {
        sessions.remove(gameId);
    }
}
