package com.airconsole.common.events;

import java.time.Instant;
import java.util.UUID;
import lombok.Value;

/**
 * Published when a room expires (30 min inactivity).
 */
@Value
public class RoomExpiredEvent {

    UUID roomId;
    String roomCode;
    Instant expiredAt;
}
