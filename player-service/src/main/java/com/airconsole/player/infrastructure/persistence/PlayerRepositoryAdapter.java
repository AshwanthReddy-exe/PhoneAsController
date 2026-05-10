package com.airconsole.player.infrastructure.persistence;

import com.airconsole.common.enums.PlayerRole;
import com.airconsole.common.enums.PlayerStatus;
import com.airconsole.player.domain.Player;
import com.airconsole.player.domain.PlayerRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class PlayerRepositoryAdapter implements PlayerRepository {

    private final PlayerJpaRepository jpaRepository;

    public PlayerRepositoryAdapter(PlayerJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Player save(Player player) {
        return toDomain(jpaRepository.save(toEntity(player)));
    }

    @Override
    public Optional<Player> findById(UUID playerId) {
        return jpaRepository.findById(playerId).map(this::toDomain);
    }

    @Override
    public List<Player> findByRoomId(UUID roomId) {
        return jpaRepository.findByRoomId(roomId).stream()
            .map(this::toDomain)
            .toList();
    }

    @Override
    public List<Player> findByStatus(PlayerStatus status) {
        return jpaRepository.findByStatus(status).stream()
            .map(this::toDomain)
            .toList();
    }

    @Override
    public void deleteById(UUID playerId) {
        jpaRepository.deleteById(playerId);
    }

    @Override
    public void deleteByRoomId(UUID roomId) {
        jpaRepository.deleteByRoomId(roomId);
    }

    private PlayerJpaEntity toEntity(Player p) {
        PlayerJpaEntity e = new PlayerJpaEntity();
        e.setPlayerId(p.playerId());
        e.setRoomId(p.roomId());
        e.setPlayerName(p.playerName());
        e.setRole(p.role());
        e.setStatus(p.status());
        e.setConnectionId(p.connectionId());
        e.setJoinedAt(p.joinedAt());
        e.setLastSeenAt(p.lastSeenAt());
        e.setJwtToken(p.jwtToken());
        return e;
    }

    private Player toDomain(PlayerJpaEntity e) {
        return new Player.Builder()
            .playerId(e.getPlayerId())
            .roomId(e.getRoomId())
            .playerName(e.getPlayerName())
            .role(e.getRole())
            .status(e.getStatus())
            .connectionId(e.getConnectionId())
            .joinedAt(e.getJoinedAt())
            .lastSeenAt(e.getLastSeenAt())
            .jwtToken(e.getJwtToken())
            .build();
    }
}
