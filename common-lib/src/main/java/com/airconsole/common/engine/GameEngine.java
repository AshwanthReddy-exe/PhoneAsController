package com.airconsole.common.engine;

import com.airconsole.common.enums.GameType;
import com.airconsole.common.model.GameContext;
import com.airconsole.common.model.GameInput;
import com.airconsole.common.model.GameSnapshot;

/**
 * Contract for all game implementations.
 * Every game in {@code games/} module implements this.
 * Zero Spring dependencies — pure Java.
 */
public interface GameEngine {

    GameType getType();

    ControllerLayout getControllerLayout();

    void initialize(GameContext context);

    void processInput(GameInput input);

    void tick();

    GameSnapshot snapshot();

    boolean isFinished();
}
