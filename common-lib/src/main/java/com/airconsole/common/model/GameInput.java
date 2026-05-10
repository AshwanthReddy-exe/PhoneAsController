package com.airconsole.common.model;

import java.util.UUID;
import lombok.Value;

/**
 * Normalized controller input inside game engine.
 * Immutable, zero-allocation friendly.
 */
@Value
public class GameInput {

    UUID playerId;
    UUID roomId;
    int action;       // opcode from ControllerLayout
    long tickNumber;  // which tick this input targets
}
