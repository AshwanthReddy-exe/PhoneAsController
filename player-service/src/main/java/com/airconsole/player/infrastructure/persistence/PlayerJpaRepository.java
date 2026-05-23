package com.airconsole.player.infrastructure.persistence;

import com.airconsole.common.enums.PlayerStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlayerJpaRepository extends JpaRepository<PlayerJpaEntity, UUID> {

    List<PlayerJpaEntity> findByRoomId(UUID roomId);

    List<PlayerJpaEntity> findByStatus(PlayerStatus status);

    void deleteByRoomId(UUID roomId);
}
