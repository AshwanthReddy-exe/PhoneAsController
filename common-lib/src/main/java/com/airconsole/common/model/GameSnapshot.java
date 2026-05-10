package com.airconsole.common.model;

import com.airconsole.common.enums.GameStatus;
import com.airconsole.common.enums.GameType;
import java.util.Map;
import java.util.UUID;
import lombok.Value;

/**
 * Serialized game state at one tick.
 * GameEngine.snapshot() returns this.
 * Protobuf-serialized for binary broadcast — low latency.
 */
@Value
public class GameSnapshot {

    UUID gameId;
    UUID roomId;
    GameType gameType;
    GameStatus status;
    long tickNumber;
    Map<UUID, Integer> scores;         // playerId → score
    byte[] statePayload;               // game-specific state (protobuf)
}
