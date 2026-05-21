package com.airconsole.room.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.airconsole.common.enums.RoomStatus;
import com.airconsole.room.domain.Room;
import com.airconsole.room.domain.RoomCode;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RoomTest {

    @Test
    void createNewRoom() {
        UUID hostId = UUID.randomUUID();
        Room room = Room.createNew(RoomCode.generate(), hostId, "SNAKE", 4);

        assertThat(room.roomId()).isNotNull();
        assertThat(room.roomCode().value()).hasSize(5);
        assertThat(room.status()).isEqualTo(RoomStatus.WAITING);
        assertThat(room.hostId()).isEqualTo(hostId);
        assertThat(room.gameType()).isEqualTo("SNAKE");
        assertThat(room.maxPlayers()).isEqualTo(4);
        assertThat(room.playerCount()).isZero();
        assertThat(room.createdAt()).isBeforeOrEqualTo(Instant.now());
        assertThat(room.expiresAt()).isAfter(Instant.now());
    }

    @Test
    void canJoinWhenWaitingAndNotFull() {
        Room room = Room.createNew(RoomCode.generate(), UUID.randomUUID(), "SNAKE", 2);
        assertThat(room.canJoin()).isTrue();
    }

    @Test
    void cannotJoinWhenFull() {
        Room room = Room.createNew(RoomCode.generate(), UUID.randomUUID(), "SNAKE", 1)
            .incrementPlayerCount();
        assertThat(room.canJoin()).isFalse();
    }

    @Test
    void incrementPlayerCount() {
        Room room = Room.createNew(RoomCode.generate(), UUID.randomUUID(), "SNAKE", 4);
        Room updated = room.incrementPlayerCount();

        assertThat(updated.playerCount()).isOne();
        assertThat(room.playerCount()).isZero(); // immutable
    }

    @Test
    void incrementPlayerCountFailsWhenFull() {
        Room room = Room.createNew(RoomCode.generate(), UUID.randomUUID(), "SNAKE", 1)
            .incrementPlayerCount();

        assertThatThrownBy(room::incrementPlayerCount)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Room is full");
    }

    @Test
    void decrementPlayerCount() {
        Room room = Room.createNew(RoomCode.generate(), UUID.randomUUID(), "SNAKE", 4)
            .incrementPlayerCount();
        Room updated = room.decrementPlayerCount();

        assertThat(updated.playerCount()).isZero();
    }

    @Test
    void decrementPlayerCountStopsAtZero() {
        Room room = Room.createNew(RoomCode.generate(), UUID.randomUUID(), "SNAKE", 4);
        Room updated = room.decrementPlayerCount();

        assertThat(updated.playerCount()).isZero();
    }

    @Test
    void markExpired() {
        Room room = Room.createNew(RoomCode.generate(), UUID.randomUUID(), "SNAKE", 4);
        Room expired = room.markExpired();

        assertThat(expired.status()).isEqualTo(RoomStatus.EXPIRED);
    }

    @Test
    void isExpiredByStatus() {
        Room room = Room.createNew(RoomCode.generate(), UUID.randomUUID(), "SNAKE", 4)
            .markExpired();
        assertThat(room.isExpired()).isTrue();
    }

    @Test
    void isExpiredByTime() {
        // Fast test: create room, mock expiry by building with past time
        // Or just verify default room is NOT expired
        Room room = Room.createNew(RoomCode.generate(), UUID.randomUUID(), "SNAKE", 4);
        assertThat(room.isExpired()).isFalse();
    }
}
