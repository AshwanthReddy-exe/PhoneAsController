package com.airconsole.game.api;

import com.airconsole.common.enums.GameType;
import com.airconsole.common.model.GameInput;
import com.airconsole.game.domain.GameOrchestrator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/games")
public class GameController {

    private static final Logger LOG = LoggerFactory.getLogger(GameController.class);

    private final GameOrchestrator orchestrator;

    public GameController(GameOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @PostMapping("/start")
    public ResponseEntity<Map<String, UUID>> startGame(
            @RequestParam UUID roomId,
            @RequestParam GameType gameType,
            @RequestBody List<UUID> playerIds) {
        LOG.info("Starting game {} for room {}", gameType, roomId);
        UUID gameId = orchestrator.startGame(roomId, gameType, playerIds);
        return ResponseEntity.ok(Map.of("gameId", gameId));
    }

    @PostMapping("/{gameId}/pause")
    public ResponseEntity<Void> pauseGame(
            @PathVariable UUID gameId,
            @RequestParam String reason) {
        orchestrator.pauseGame(gameId, reason);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{gameId}/resume")
    public ResponseEntity<Void> resumeGame(@PathVariable UUID gameId) {
        orchestrator.resumeGame(gameId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{gameId}/finish")
    public ResponseEntity<Void> finishGame(
            @PathVariable UUID gameId,
            @RequestParam UUID winnerId,
            @RequestBody Map<UUID, Integer> finalScores) {
        orchestrator.finishGame(gameId, winnerId, finalScores);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/input")
    public ResponseEntity<Void> handleInput(@RequestBody GameInput input) {
        orchestrator.handleInput(input);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{gameType}/layout")
    public ResponseEntity<?> getControllerLayout(@PathVariable GameType gameType) {
        var layout = orchestrator.getControllerLayout(gameType);
        return ResponseEntity.ok(layout);
    }
}
