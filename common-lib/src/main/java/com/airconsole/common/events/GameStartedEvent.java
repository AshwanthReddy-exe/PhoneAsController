package com.airconsole.common.events;

import com.airconsole.common.enums.GameType;
import java.time.Instant;
import java.util.UUID;
import lombok.Value;

/**
 * Published when a game starts (host presses start).
 */
@Value
public class GameStartedEvent {

    UUID gameId;
    UUID roomId;
    GameType gameType;
    Instant startedAt;
}
