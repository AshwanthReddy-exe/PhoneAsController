package com.airconsole.games.trivia.domain;

import com.airconsole.common.engine.ControllerLayout;
import com.airconsole.common.enums.GameType;
import java.util.List;
import java.util.Map;

public class TriviaControllerLayout implements ControllerLayout {

    @Override
    public GameType getGameType() {
        return GameType.TRIVIA;
    }

    @Override
    public List<Button> getButtons() {
        return List.of(
            new Button("optionA", "A", 5, 10, 30, 35, 12, "option"),
            new Button("optionB", "B", 6, 55, 30, 35, 12, "option"),
            new Button("optionC", "C", 7, 10, 70, 35, 12, "option"),
            new Button("optionD", "D", 8, 55, 70, 35, 12, "option")
        );
    }

    @Override
    public boolean hasDPad() {
        return false;
    }

    @Override
    public boolean hasJoystick() {
        return false;
    }

    @Override
    public Map<String, String> getKeyboardMap() {
        return Map.of(
            "a", "5",
            "b", "6",
            "c", "7",
            "d", "8",
            "1", "5",
            "2", "6",
            "3", "7",
            "4", "8"
        );
    }
}