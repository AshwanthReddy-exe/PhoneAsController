package com.airconsole.player.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.airconsole.common.enums.PlayerRole;
import com.airconsole.common.enums.PlayerStatus;
import com.airconsole.common.events.PlayerJoinedEvent;
import com.airconsole.player.application.PlayerEventPublisher;
import com.airconsole.player.application.dto.TokenResponse;
import com.airconsole.player.domain.JwtIssuer;
import com.airconsole.player.domain.Player;
import com.airconsole.player.domain.PlayerRepository;
import com.airconsole.player.domain.PlayerService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PlayerServiceTest {

    PlayerRepository repository;
    PlayerEventPublisher eventPublisher;
    JwtIssuer jwtIssuer;
    PlayerService playerService;

    @BeforeEach
    void setUp() {
        repository = mock(PlayerRepository.class);
        eventPublisher = mock(PlayerEventPublisher.class);
        jwtIssuer = mock(JwtIssuer.class);
        playerService = new PlayerService(repository, eventPublisher, jwtIssuer);
    }

    @Test
    void registerPlayer() {
        UUID roomId = UUID.randomUUID();
        when(jwtIssuer.issue(any(), any(), any())).thenReturn("token-123");
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TokenResponse token = playerService.registerPlayer(roomId, "Alice", true);

        assertThat(token.token()).isEqualTo("token-123");
        assertThat(token.role()).isEqualTo("HOST");
        verify(eventPublisher).publish(any(PlayerJoinedEvent.class));
    }

    @Test
    void reconnectPlayerSuccess() {
        UUID playerId = UUID.randomUUID();
        Player player = Player.createNew(UUID.randomUUID(), "Bob", PlayerRole.GUEST, "tok")
            .withStatus(PlayerStatus.DISCONNECTED);
        when(repository.findById(playerId)).thenReturn(Optional.of(player));
        when(jwtIssuer.issue(any(), any(), any())).thenReturn("new-token");
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Optional<TokenResponse> token = playerService.reconnectPlayer(playerId);

        assertThat(token).isPresent();
        assertThat(token.get().token()).isEqualTo("new-token");
    }

    @Test
    void reconnectPlayerFailsAfterWindow() {
        UUID playerId = UUID.randomUUID();
        Player player = new Player.Builder()
            .playerId(playerId)
            .roomId(UUID.randomUUID())
            .playerName("Charlie")
            .role(PlayerRole.GUEST)
            .status(PlayerStatus.DISCONNECTED)
            .connectionId(null)
            .joinedAt(java.time.Instant.now().minusSeconds(120))
            .lastSeenAt(java.time.Instant.now().minusSeconds(120))
            .jwtToken("tok")
            .build();

        when(repository.findById(playerId)).thenReturn(Optional.of(player));

        Optional<TokenResponse> token = playerService.reconnectPlayer(playerId);

        assertThat(token).isEmpty();
    }

    @Test
    void markDisconnected() {
        UUID playerId = UUID.randomUUID();
        Player player = Player.createNew(UUID.randomUUID(), "Dave", PlayerRole.GUEST, "tok");
        when(repository.findById(playerId)).thenReturn(Optional.of(player));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        playerService.markDisconnected(playerId);

        verify(repository).save(any(Player.class));
    }
}
