package com.airconsole.games.snake.domain;

import com.airconsole.common.engine.ControllerLayout;
import com.airconsole.common.enums.GameType;
import java.util.List;
import java.util.Map;

public class SnakeControllerLayout implements ControllerLayout {

    @Override
    public GameType getGameType() {
        return GameType.SNAKE;
    }

    @Override
    public List<Button> getButtons() {
        return List.of(
            new Button("up", "UP", 1, 50, 10, 20, 20, "dpad-up"),
            new Button("down", "DOWN", 2, 50, 70, 20, 20, "dpad-down"),
            new Button("left", "LEFT", 3, 20, 40, 20, 20, "dpad-left"),
            new Button("right", "RIGHT", 4, 80, 40, 20, 20, "dpad-right")
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
            "ArrowUp", "1",
            "ArrowDown", "2",
            "ArrowLeft", "3",
            "ArrowRight", "4",
            "w", "1",
            "s", "2",
            "a", "3",
            "d", "4"
        );
    }
}
