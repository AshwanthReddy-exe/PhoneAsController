package com.airconsole.player.api;

import com.airconsole.player.application.dto.PlayerListResponse;
import com.airconsole.player.application.dto.TokenResponse;
import com.airconsole.player.api.request.RegisterPlayerRequest;
import com.airconsole.player.api.request.ReconnectPlayerRequest;
import com.airconsole.player.api.request.UpdateConnectionRequest;
import com.airconsole.player.domain.Player;
import com.airconsole.player.domain.PlayerService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/players")
public class PlayerController {

    private static final Logger LOG = LoggerFactory.getLogger(PlayerController.class);

    private final PlayerService playerService;

    public PlayerController(PlayerService playerService) {
        this.playerService = playerService;
    }

    @PostMapping("/register")
    public ResponseEntity<TokenResponse> register(
            @Valid @RequestBody RegisterPlayerRequest request) {
        LOG.info("Register player {} in room {}", request.playerName(), request.roomId());
        TokenResponse token = playerService.registerPlayer(
            request.roomId(), request.playerName(), false);
        return ResponseEntity.created(URI.create("/api/players/" + token.playerId()))
            .body(token);
    }

    @PostMapping("/reconnect")
    public ResponseEntity<TokenResponse> reconnect(
            @Valid @RequestBody ReconnectPlayerRequest request) {
        LOG.info("Reconnect player {}", request.playerId());
        Optional<TokenResponse> token = playerService.reconnectPlayer(request.playerId());
        return token.map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{roomId}/all")
    public ResponseEntity<List<PlayerListResponse>> listPlayers(@PathVariable UUID roomId) {
        List<Player> players = playerService.listPlayersInRoom(roomId);
        List<PlayerListResponse> response = players.stream()
            .map(p -> new PlayerListResponse(
                p.playerId(), p.playerName(), p.role(), p.status(), p.joinedAt()))
            .toList();
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{playerId}/connection")
    public ResponseEntity<Void> updateConnection(
            @PathVariable UUID playerId,
            @Valid @RequestBody UpdateConnectionRequest request) {
        LOG.info("Player {} connection changed to {}", playerId, request.connectionId());
        playerService.markDisconnected(playerId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{playerId}")
    public ResponseEntity<Void> deletePlayer(@PathVariable UUID playerId) {
        playerService.markDisconnected(playerId);
        return ResponseEntity.noContent().build();
    }
}
