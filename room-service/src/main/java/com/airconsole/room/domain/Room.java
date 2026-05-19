package com.airconsole.room.domain;

import com.airconsole.common.enums.RoomStatus;
import java.time.Instant;
import java.util.UUID;

/**
 * Aggregate root — pure business logic, zero Spring annotations.
 * Immutable after creation (only playerCount and status mutate).
 */
public final class Room {

    private final UUID roomId;
    private final RoomCode roomCode;
    private final UUID hostId;
    private final RoomStatus status;
    private final String gameType;   // GameType.name() as string, not enum
    private final int maxPlayers;
    private final int playerCount;
    private final Instant createdAt;
    private final Instant expiresAt;

    // Thresholds
    private static final int DEFAULT_EXPIRY_MINUTES = 30;
    private static final int DEFAULT_MAX_PLAYERS = 8;

    private Room(Builder builder) {
        this.roomId = builder.roomId;
        this.roomCode = builder.roomCode;
        this.hostId = builder.hostId;
        this.status = builder.status;
        this.gameType = builder.gameType;
        this.maxPlayers = builder.maxPlayers <= 0 ? DEFAULT_MAX_PLAYERS : builder.maxPlayers;
        this.playerCount = builder.playerCount;
        this.createdAt = builder.createdAt;
        this.expiresAt = builder.expiresAt;
    }

    public static Room createNew(RoomCode code, UUID hostId, String gameType, int maxPlayers) {
        Instant now = Instant.now();
        return new Builder()
            .roomId(UUID.randomUUID())
            .roomCode(code)
            .hostId(hostId)
            .status(RoomStatus.WAITING)
            .gameType(gameType)
            .maxPlayers(maxPlayers > 0 ? maxPlayers : DEFAULT_MAX_PLAYERS)
            .playerCount(0)
            .createdAt(now)
            .expiresAt(now.plusSeconds(DEFAULT_EXPIRY_MINUTES * 60L))
            .build();
    }

    public Room incrementPlayerCount() {
        if (playerCount >= maxPlayers) {
            throw new IllegalStateException("Room is full");
        }
        return new Builder(this).playerCount(playerCount + 1).build();
    }

    public Room decrementPlayerCount() {
        if (playerCount <= 0) {
            return this; // idempotent
        }
        return new Builder(this).playerCount(playerCount - 1).build();
    }

    public Room markExpired() {
        return new Builder(this).status(RoomStatus.EXPIRED).build();
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt) || status == RoomStatus.EXPIRED;
    }

    public boolean canJoin() {
        return status == RoomStatus.WAITING && playerCount < maxPlayers && !isExpired();
    }

    // Getters
    public UUID roomId() { return roomId; }
    public RoomCode roomCode() { return roomCode; }
    public UUID hostId() { return hostId; }
    public RoomStatus status() { return status; }
    public String gameType() { return gameType; }
    public int maxPlayers() { return maxPlayers; }
    public int playerCount() { return playerCount; }
    public Instant createdAt() { return createdAt; }
    public Instant expiresAt() { return expiresAt; }

    public static class Builder {
        private UUID roomId;
        private RoomCode roomCode;
        private UUID hostId;
        private RoomStatus status;
        private String gameType;
        private int maxPlayers;
        private int playerCount;
        private Instant createdAt;
        private Instant expiresAt;

        public Builder() {}

        public Builder(Room room) {
            this.roomId = room.roomId;
            this.roomCode = room.roomCode;
            this.hostId = room.hostId;
            this.status = room.status;
            this.gameType = room.gameType;
            this.maxPlayers = room.maxPlayers;
            this.playerCount = room.playerCount;
            this.createdAt = room.createdAt;
            this.expiresAt = room.expiresAt;
        }

        public Builder roomId(UUID v) { this.roomId = v; return this; }
        public Builder roomCode(RoomCode v) { this.roomCode = v; return this; }
        public Builder hostId(UUID v) { this.hostId = v; return this; }
        public Builder status(RoomStatus v) { this.status = v; return this; }
        public Builder gameType(String v) { this.gameType = v; return this; }
        public Builder maxPlayers(int v) { this.maxPlayers = v; return this; }
        public Builder playerCount(int v) { this.playerCount = v; return this; }
        public Builder createdAt(Instant v) { this.createdAt = v; return this; }
        public Builder expiresAt(Instant v) { this.expiresAt = v; return this; }

        public Room build() {
            return new Room(this);
        }
    }
}
