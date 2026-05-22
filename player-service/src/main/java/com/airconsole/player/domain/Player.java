package com.airconsole.player.domain;

import com.airconsole.common.enums.PlayerRole;
import com.airconsole.common.enums.PlayerStatus;
import java.time.Instant;
import java.util.UUID;

/**
 * Aggregate root — player identity + session state.
 * Pure business logic, zero Spring annotations.
 */
public final class Player {

    private final UUID playerId;
    private final UUID roomId;
    private final String playerName;
    private final PlayerRole role;
    private final PlayerStatus status;
    private final String connectionId;    // SignalR connection ID
    private final Instant joinedAt;
    private final Instant lastSeenAt;     // updated on disconnect
    private final String jwtToken;

    private Player(Builder builder) {
        this.playerId = builder.playerId;
        this.roomId = builder.roomId;
        this.playerName = builder.playerName;
        this.role = builder.role;
        this.status = builder.status;
        this.connectionId = builder.connectionId;
        this.joinedAt = builder.joinedAt;
        this.lastSeenAt = builder.lastSeenAt;
        this.jwtToken = builder.jwtToken;
    }

    public static Player createNew(
            UUID roomId, String playerName, PlayerRole role, String jwtToken) {
        Instant now = Instant.now();
        return new Builder()
            .playerId(UUID.randomUUID())
            .roomId(roomId)
            .playerName(playerName)
            .role(role)
            .status(PlayerStatus.CONNECTED)
            .connectionId(null)
            .joinedAt(now)
            .lastSeenAt(now)
            .jwtToken(jwtToken)
            .build();
    }

    public Player withStatus(PlayerStatus newStatus) {
        Instant now = Instant.now();
        return new Builder(this)
            .status(newStatus)
            .lastSeenAt(newStatus == PlayerStatus.DISCONNECTED ? now : lastSeenAt)
            .build();
    }

    public Player withConnection(String connectionId) {
        return new Builder(this)
            .connectionId(connectionId)
            .status(connectionId != null ? PlayerStatus.CONNECTED : status)
            .lastSeenAt(connectionId != null ? joinedAt : lastSeenAt)
            .build();
    }

    public boolean canRejoin(Instant now, long windowSeconds) {
        return status == PlayerStatus.DISCONNECTED
            && !lastSeenAt.isBefore(now.minusSeconds(windowSeconds));
    }

    // Getters
    public UUID playerId() { return playerId; }
    public UUID roomId() { return roomId; }
    public String playerName() { return playerName; }
    public PlayerRole role() { return role; }
    public PlayerStatus status() { return status; }
    public String connectionId() { return connectionId; }
    public Instant joinedAt() { return joinedAt; }
    public Instant lastSeenAt() { return lastSeenAt; }
    public String jwtToken() { return jwtToken; }

    public static class Builder {
        private UUID playerId;
        private UUID roomId;
        private String playerName;
        private PlayerRole role;
        private PlayerStatus status;
        private String connectionId;
        private Instant joinedAt;
        private Instant lastSeenAt;
        private String jwtToken;

        public Builder() {}

        public Builder(Player p) {
            this.playerId = p.playerId;
            this.roomId = p.roomId;
            this.playerName = p.playerName;
            this.role = p.role;
            this.status = p.status;
            this.connectionId = p.connectionId;
            this.joinedAt = p.joinedAt;
            this.lastSeenAt = p.lastSeenAt;
            this.jwtToken = p.jwtToken;
        }

        public Builder playerId(UUID v) { this.playerId = v; return this; }
        public Builder roomId(UUID v) { this.roomId = v; return this; }
        public Builder playerName(String v) { this.playerName = v; return this; }
        public Builder role(PlayerRole v) { this.role = v; return this; }
        public Builder status(PlayerStatus v) { this.status = v; return this; }
        public Builder connectionId(String v) { this.connectionId = v; return this; }
        public Builder joinedAt(Instant v) { this.joinedAt = v; return this; }
        public Builder lastSeenAt(Instant v) { this.lastSeenAt = v; return this; }
        public Builder jwtToken(String v) { this.jwtToken = v; return this; }

        public Player build() {
            return new Player(this);
        }
    }
}
