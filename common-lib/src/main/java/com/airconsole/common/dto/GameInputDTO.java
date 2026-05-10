package com.airconsole.common.dto;

import java.util.UUID;
import lombok.Value;

/**
 * Controller input sent via SignalR / REST.
 * Lightweight — action is int opcode for latency.
 */
@Value
public class GameInputDTO {

    UUID playerId;
    UUID roomId;
    int action;       // opcode, not string — fast deserialization
    long timestamp;   // client timestamp for latency measurement
}
