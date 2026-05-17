package com.airconsole.common.model;

import com.airconsole.common.enums.GameType;
import java.util.List;
import java.util.UUID;
import lombok.Value;

/**
 * Context passed to GameEngine.initialize() when a game starts.
 * Room config + player list.
 */
@Value
public class GameContext {

    UUID gameId;
    UUID roomId;
    GameType gameType;
    List<UUID> playerIds;
    int tickRate;       // ms per tick (100 for Snake, 16 for Pong)
}
