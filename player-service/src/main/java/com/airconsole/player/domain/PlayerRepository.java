package com.airconsole.player.domain;

import com.airconsole.common.enums.PlayerStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlayerRepository {

    Player save(Player player);

    Optional<Player> findById(UUID playerId);

    List<Player> findByRoomId(UUID roomId);

    List<Player> findByStatus(PlayerStatus status);

    void deleteById(UUID playerId);

    void deleteByRoomId(UUID roomId);
}
