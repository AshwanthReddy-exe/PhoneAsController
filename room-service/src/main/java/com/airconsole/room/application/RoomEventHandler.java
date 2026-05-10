package com.airconsole.room.application;

import com.airconsole.common.events.PlayerJoinedEvent;
import com.airconsole.common.events.PlayerLeftEvent;
import com.airconsole.room.domain.Room;
import com.airconsole.room.domain.RoomRepository;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Handles player events from Redis Streams.
 * Updates denormalized playerCount in Room.
 *
 * <p>Subscribed via RoomRedisConfig to {@code airconsole.stream.player}.
 */
@Component
public class RoomEventHandler {

    private static final Logger LOG = LoggerFactory.getLogger(RoomEventHandler.class);

    private final RoomRepository repository;

    public RoomEventHandler(RoomRepository repository) {
        this.repository = repository;
    }

    public void onPlayerJoined(PlayerJoinedEvent event) {
        LOG.debug("Player joined room {}", event.getRoomId());
        repository.findById(event.getRoomId())
            .map(Room::incrementPlayerCount)
            .ifPresent(repository::save);
    }

    public void onPlayerLeft(PlayerLeftEvent event) {
        LOG.debug("Player left room {}", event.getRoomId());
        repository.findById(event.getRoomId())
            .map(Room::decrementPlayerCount)
            .ifPresent(repository::save);
    }
}
