package com.airconsole.games.pong.domain;

import com.airconsole.common.engine.ControllerLayout;
import com.airconsole.common.enums.GameType;
import java.util.List;
import java.util.Map;

public class PongControllerLayout implements ControllerLayout {

    @Override
    public GameType getGameType() {
        return GameType.PONG;
    }

    @Override
    public List<Button> getButtons() {
        return List.of(
            new Button("left", "LEFT", 3, 25, 50, 30, 30, "dpad-left"),
            new Button("right", "RIGHT", 4, 75, 50, 30, 30, "dpad-right")
        );
    }

    @Override
    public boolean hasDPad() {
        return true;
    }

    @Override
    public boolean hasJoystick() {
        return false;
    }

    @Override
    public Map<String, String> getKeyboardMap() {
        return Map.of(
            "ArrowUp",   "3",
            "ArrowDown", "4",
            "w",         "3",
            "s",         "4"
        );
    }
}