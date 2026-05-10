package com.airconsole.score.api;

import com.airconsole.common.enums.GameType;
import com.airconsole.score.application.dto.ScoreResponse;
import com.airconsole.score.domain.LeaderboardService;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    public LeaderboardController(LeaderboardService leaderboardService) {
        this.leaderboardService = leaderboardService;
    }

    @GetMapping("/leaderboard/{gameType}")
    public ResponseEntity<List<ScoreResponse>> getLeaderboard(
            @PathVariable GameType gameType,
            @RequestParam(defaultValue = "10") int limit) {
        
        List<ScoreResponse> responses = leaderboardService.getTopScores(gameType, limit)
            .stream()
            .map(ScoreResponse::fromDomain)
            .collect(Collectors.toList());
            
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/history/{playerId}")
    public ResponseEntity<List<ScoreResponse>> getHistory(
            @PathVariable UUID playerId,
            @RequestParam(defaultValue = "10") int limit) {
            
        List<ScoreResponse> responses = leaderboardService.getPlayerHistory(playerId, limit)
            .stream()
            .map(ScoreResponse::fromDomain)
            .collect(Collectors.toList());
            
        return ResponseEntity.ok(responses);
    }
}
