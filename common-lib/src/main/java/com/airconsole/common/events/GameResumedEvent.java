package com.airconsole.common.events;

import java.time.Instant;
import java.util.UUID;
import lombok.Value;

/**
 * Published when game resumes (player rejoined within 60s window).
 */
@Value
public class GameResumedEvent {

    UUID gameId;
    UUID roomId;
    Instant resumedAt;
}
