package com.airconsole.room.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository port (hexagonal architecture).
 * Infrastructure provides adapter implementation.
 */
public interface RoomRepository {

    Room save(Room room);

    Optional<Room> findByCode(String roomCode);

    Optional<Room> findById(UUID roomId);

    boolean existsByCode(String roomCode);

    void delete(UUID roomId);

    List<Room> findAll();
}
