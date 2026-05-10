package com.airconsole.player.domain;

import com.airconsole.common.enums.PlayerRole;
import com.airconsole.common.enums.PlayerStatus;
import com.airconsole.common.events.PlayerJoinedEvent;
import com.airconsole.common.events.PlayerLeftEvent;
import com.airconsole.common.events.PlayerRejoinedEvent;
import com.airconsole.player.application.PlayerEventPublisher;
import com.airconsole.player.application.dto.TokenResponse;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Domain orchestrator.
 * Handles registration, rejoin, token generation.
 */
public class PlayerService {

    private final PlayerRepository repository;
    private final PlayerEventPublisher eventPublisher;
    private final JwtIssuer jwtIssuer;
    private final RejoinWindowTracker rejoinTracker;

    public PlayerService(
            PlayerRepository repository,
            PlayerEventPublisher eventPublisher,
            JwtIssuer jwtIssuer) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.jwtIssuer = jwtIssuer;
        this.rejoinTracker = new RejoinWindowTracker();
    }

    /**
     * Register new player in room. Generates UUID + JWT.
     */
    public TokenResponse registerPlayer(UUID roomId, String playerName, boolean isHost) {
        PlayerRole role = isHost ? PlayerRole.HOST : PlayerRole.GUEST;

        // Create player skeleton (no token yet — need playerId in token)
        Player skeleton = Player.createNew(roomId, playerName, role, "TEMP");
        Player saved = repository.save(skeleton);

        // Issue real token with player ID
        String token = jwtIssuer.issue(saved.playerId(), roomId, role.name());
        Player withToken = new Player.Builder(saved).jwtToken(token).build();
        Player finalSaved = repository.save(withToken);

        eventPublisher.publish(new PlayerJoinedEvent(
            finalSaved.playerId(), finalSaved.roomId(),
            finalSaved.playerName(), finalSaved.role(),
            finalSaved.joinedAt()
        ));

        return new TokenResponse(finalSaved.playerId(), token, finalSaved.role().name());
    }

    /**
     * Reconnect within 60s window. Issues new token.
     */
    public Optional<TokenResponse> reconnectPlayer(UUID playerId) {
        return repository.findById(playerId)
            .filter(p -> p.status() == PlayerStatus.DISCONNECTED)
            .filter(p -> rejoinTracker.isWithinWindow(p.lastSeenAt()))
            .map(p -> {
                Player reconnected = p.withStatus(PlayerStatus.CONNECTED);
                Player saved = repository.save(reconnected);
                String newToken = jwtIssuer.issue(saved.playerId(), saved.roomId(), saved.role().name());
                Player withNewToken = repository.save(
                    new Player.Builder(saved).jwtToken(newToken).build()
                );
                eventPublisher.publish(new PlayerRejoinedEvent(
                    withNewToken.playerId(), withNewToken.roomId(),
                    Instant.now(), withNewToken.connectionId()
                ));
                return new TokenResponse(withNewToken.playerId(), newToken, withNewToken.role().name());
            });
    }

    /**
     * Mark player as disconnected (SignalR drop detected).
     */
    public void markDisconnected(UUID playerId) {
        repository.findById(playerId)
            .map(p -> p.withStatus(PlayerStatus.DISCONNECTED))
            .map(repository::save)
            .ifPresent(p -> {});
    }

    /**
     * Background task: scan disconnected players, expire those past 60s window.
     */
    @Scheduled(fixedDelay = 10000)
    public void checkRejoinWindows() {
        List<Player> expired = repository.findByStatus(PlayerStatus.DISCONNECTED).stream()
            .filter(p -> rejoinTracker.isExpired(p.lastSeenAt()))
            .toList();

        for (Player p : expired) {
            repository.deleteById(p.playerId());
            eventPublisher.publish(new PlayerLeftEvent(
                p.playerId(), p.roomId(), Instant.now()
            ));
        }
    }

    public List<Player> listPlayersInRoom(UUID roomId) {
        return repository.findByRoomId(roomId);
    }

    public void removePlayersByRoom(UUID roomId) {
        repository.deleteByRoomId(roomId);
    }
}
