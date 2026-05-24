package com.airconsole.player.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.airconsole.common.enums.PlayerRole;
import com.airconsole.common.enums.PlayerStatus;
import com.airconsole.player.domain.Player;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PlayerTest {

    @Test
    void createNewPlayer() {
        UUID roomId = UUID.randomUUID();
        Player p = Player.createNew(roomId, "Alice", PlayerRole.HOST, "token");

        assertThat(p.playerId()).isNotNull();
        assertThat(p.roomId()).isEqualTo(roomId);
        assertThat(p.playerName()).isEqualTo("Alice");
        assertThat(p.role()).isEqualTo(PlayerRole.HOST);
        assertThat(p.status()).isEqualTo(PlayerStatus.CONNECTED);
        assertThat(p.connectionId()).isNull();
        assertThat(p.jwtToken()).isEqualTo("token");
        assertThat(p.joinedAt()).isBeforeOrEqualTo(Instant.now());
    }

    @Test
    void withStatusDisconnected() {
        Player p = Player.createNew(UUID.randomUUID(), "Bob", PlayerRole.GUEST, "tok");
        Player disconnected = p.withStatus(PlayerStatus.DISCONNECTED);

        assertThat(disconnected.status()).isEqualTo(PlayerStatus.DISCONNECTED);
        assertThat(p.status()).isEqualTo(PlayerStatus.CONNECTED); // immutable
    }

    @Test
    void withConnection() {
        Player p = Player.createNew(UUID.randomUUID(), "Charlie", PlayerRole.GUEST, "tok");
        Player connected = p.withConnection("conn-123");

        assertThat(connected.connectionId()).isEqualTo("conn-123");
        assertThat(connected.status()).isEqualTo(PlayerStatus.CONNECTED);
    }

    @Test
    void canRejoinWithinWindow() {
        Player p = Player.createNew(UUID.randomUUID(), "Dave", PlayerRole.GUEST, "tok")
            .withStatus(PlayerStatus.DISCONNECTED);

        assertThat(p.canRejoin(Instant.now(), 60)).isTrue();
    }

    @Test
    void cannotRejoinAfterWindow() {
        Player p = Player.createNew(UUID.randomUUID(), "Eve", PlayerRole.GUEST, "tok")
            .withStatus(PlayerStatus.DISCONNECTED);

        Instant future = Instant.now().plusSeconds(120);
        assertThat(p.canRejoin(future, 60)).isFalse();
    }
}
