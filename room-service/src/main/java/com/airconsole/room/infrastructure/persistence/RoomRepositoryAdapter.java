package com.airconsole.room.infrastructure.persistence;

import com.airconsole.room.domain.Room;
import com.airconsole.room.domain.RoomCode;
import com.airconsole.room.domain.RoomRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Hexagonal adapter — bridges domain RoomRepository port to JPA.
 */
@Component
public class RoomRepositoryAdapter implements RoomRepository {

    private final RoomJpaRepository jpaRepository;

    public RoomRepositoryAdapter(RoomJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Room save(Room room) {
        RoomJpaEntity entity = toEntity(room);
        RoomJpaEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Room> findByCode(String roomCode) {
        return jpaRepository.findByRoomCode(roomCode.toUpperCase())
            .map(this::toDomain);
    }

    @Override
    public Optional<Room> findById(UUID roomId) {
        return jpaRepository.findById(roomId)
            .map(this::toDomain);
    }

    @Override
    public boolean existsByCode(String roomCode) {
        return jpaRepository.existsByRoomCode(roomCode.toUpperCase());
    }

    @Override
    public void delete(UUID roomId) {
        jpaRepository.deleteById(roomId);
    }

    @Override
    public List<Room> findAll() {
        return jpaRepository.findAll().stream().map(this::toDomain).toList();
    }

    private RoomJpaEntity toEntity(Room room) {
        RoomJpaEntity e = new RoomJpaEntity();
        e.setRoomId(room.roomId());
        e.setRoomCode(room.roomCode().value());
        e.setStatus(room.status());
        e.setHostId(room.hostId());
        e.setGameType(room.gameType());
        e.setMaxPlayers(room.maxPlayers());
        e.setPlayerCount(room.playerCount());
        e.setCreatedAt(room.createdAt());
        e.setExpiresAt(room.expiresAt());
        return e;
    }

    private Room toDomain(RoomJpaEntity e) {
        return new Room.Builder()
            .roomId(e.getRoomId())
            .roomCode(RoomCode.of(e.getRoomCode()))
            .hostId(e.getHostId())
            .status(e.getStatus())
            .gameType(e.getGameType())
            .maxPlayers(e.getMaxPlayers())
            .playerCount(e.getPlayerCount())
            .createdAt(e.getCreatedAt())
            .expiresAt(e.getExpiresAt())
            .build();
    }
}
