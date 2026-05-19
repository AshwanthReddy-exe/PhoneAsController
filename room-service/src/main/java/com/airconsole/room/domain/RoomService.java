package com.airconsole.room.domain;

import com.airconsole.common.enums.RoomStatus;
import com.airconsole.common.events.RoomCreatedEvent;
import com.airconsole.common.events.RoomExpiredEvent;
import com.airconsole.room.application.RoomEventPublisher;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Domain orchestrator.
 * Handles room lifecycle. No Spring, no infrastructure concerns.
 */
public class RoomService {

    private final RoomRepository repository;
    private final RoomEventPublisher eventPublisher;

    public RoomService(RoomRepository repository, RoomEventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    public Room createRoom(UUID hostId, String gameType, int maxPlayers) {
        RoomCode code = RoomCode.generate();
        while (repository.existsByCode(code.value())) {
            code = RoomCode.generate();
        }

        Room room = Room.createNew(code, hostId, gameType, maxPlayers);
        Room saved = repository.save(room);

        eventPublisher.publish(new RoomCreatedEvent(
            saved.roomId(),
            saved.roomCode().value(),
            null, // gameType as string -> enum conversion later if needed
            saved.hostId(),
            saved.maxPlayers(),
            saved.createdAt()
        ));

        return saved;
    }

    public Optional<Room> findByCode(String code) {
        return repository.findByCode(code);
    }

    public Optional<Room> joinRoom(String code) {
        Optional<Room> maybeRoom = repository.findByCode(code);
        if (maybeRoom.isEmpty() || !maybeRoom.get().canJoin()) {
            return Optional.empty();
        }
        Room updated = maybeRoom.get().incrementPlayerCount();
        return Optional.of(repository.save(updated));
    }

    public void deleteRoom(UUID roomId, UUID requestingHostId) {
        Room room = repository.findById(roomId)
            .orElseThrow(() -> new IllegalArgumentException("Room not found"));
        if (!room.hostId().equals(requestingHostId)) {
            throw new IllegalStateException("Only host can delete room");
        }
        repository.delete(roomId);
    }

    @Scheduled(fixedDelay = 60000)
    public int expireOldRooms() {
        var now = Instant.now();
        int expired = 0;
        for (Room room : repository.findAll()) {
            if (room.status() != RoomStatus.EXPIRED && now.isAfter(room.expiresAt())) {
                Room updated = room.markExpired();
                repository.save(updated);
                eventPublisher.publish(new RoomExpiredEvent(
                    updated.roomId(),
                    updated.roomCode().value(),
                    now
                ));
                expired++;
            }
        }
        return expired;
    }
}
