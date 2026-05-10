package com.airconsole.room.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoomJpaRepository extends JpaRepository<RoomJpaEntity, UUID> {

    Optional<RoomJpaEntity> findByRoomCode(String roomCode);

    boolean existsByRoomCode(String roomCode);
}
