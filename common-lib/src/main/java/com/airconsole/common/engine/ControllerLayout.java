package com.airconsole.common.engine;

import com.airconsole.common.enums.GameType;
import java.util.List;
import java.util.Map;

/**
 * Defines controller buttons for a game.
 * Backend returns this as JSON. Frontend renders dynamically.
 * New games implement this — zero frontend code changes needed.
 */
public interface ControllerLayout {

    GameType getGameType();

    List<Button> getButtons();

    boolean hasDPad();

    boolean hasJoystick();

    Map<String, String> getKeyboardMap();

    /**
     * Single button definition.
     */
    record Button(
        String id,
        String label,
        int actionCode,
        int x, int y, int w, int h,
        String style   // e.g. "dpad", "primary", "secondary"
    ) {}
}
