package com.airconsole.game.domain;

import java.util.Optional;
import java.util.UUID;

public interface GameSessionRepository {

    void save(GameSession session);

    Optional<GameSession> findById(UUID gameId);

    Optional<GameSession> findByRoomId(UUID roomId);

    void delete(UUID gameId);
}
