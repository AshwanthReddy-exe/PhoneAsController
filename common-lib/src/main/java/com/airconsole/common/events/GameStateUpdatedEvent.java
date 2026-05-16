package com.airconsole.common.events;

import com.airconsole.common.model.GameSnapshot;
import java.time.Instant;
import java.util.UUID;
import lombok.Value;

/**
 * Published on every game tick (~100ms Snake, ~16ms Pong).
 * Stateless services (notification, score) react to this.
 */
@Value
public class GameStateUpdatedEvent {

    UUID gameId;
    UUID roomId;
    GameSnapshot snapshot;    // binary protobuf, not json, latency optimization
    long tickNumber;
    Instant tickTimestamp;
}
