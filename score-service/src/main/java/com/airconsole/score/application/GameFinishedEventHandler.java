package com.airconsole.score.application;

import com.airconsole.common.events.GameFinishedEvent;
import com.airconsole.score.domain.LeaderboardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class GameFinishedEventHandler {

    private static final Logger log = LoggerFactory.getLogger(GameFinishedEventHandler.class);
    
    private final LeaderboardService leaderboardService;

    public GameFinishedEventHandler(LeaderboardService leaderboardService) {
        this.leaderboardService = leaderboardService;
    }

    public void handle(GameFinishedEvent event) {
        log.info("Received GameFinishedEvent for gameId: {}", event.getGameId());
        
        if (event.getFinalScores() != null && !event.getFinalScores().isEmpty()) {
            leaderboardService.recordGameScores(
                event.getRoomId(),
                event.getGameType(),
                event.getFinalScores(),
                event.getFinishedAt()
            );
            log.info("Recorded {} scores for game {}", event.getFinalScores().size(), event.getGameType());
        } else {
            log.warn("No scores provided in GameFinishedEvent for gameId: {}", event.getGameId());
        }
    }
}
