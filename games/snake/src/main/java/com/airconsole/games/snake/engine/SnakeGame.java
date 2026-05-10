package com.airconsole.games.snake.engine;

import com.airconsole.common.engine.ControllerLayout;
import com.airconsole.common.engine.GameEngine;
import com.airconsole.common.enums.GameStatus;
import com.airconsole.common.enums.GameType;
import com.airconsole.common.model.GameContext;
import com.airconsole.common.model.GameInput;
import com.airconsole.common.model.GameSnapshot;
import com.airconsole.common.util.EventSerializer;
import com.airconsole.games.snake.domain.SnakeControllerLayout;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SnakeGame implements GameEngine {

    // Grid Size
    public static final int GRID_WIDTH = 30;
    public static final int GRID_HEIGHT = 30;

    private GameContext context;
    private final SnakeControllerLayout layout = new SnakeControllerLayout();
    private GameStatus status = GameStatus.WAITING;
    private long tickNumber = 0;
    private final Map<UUID, Integer> scores = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<GameInput> inputQueue = new ConcurrentLinkedQueue<>();
    private final Map<UUID, Snake> snakes = new ConcurrentHashMap<>();
    private Point food;
    private final Random random = new Random();
    private final String[] colors = {"#FF0055", "#00FF66", "#00CCFF", "#FFFF00", "#FF9900", "#FF00FF"};

    @Override
    public GameType getType() {
        return GameType.SNAKE;
    }

    @Override
    public ControllerLayout getControllerLayout() {
        return layout;
    }

    @Override
    public void initialize(GameContext context) {
        this.context = context;
        int colorIdx = 0;
        for (UUID playerId : context.getPlayerIds()) {
            scores.put(playerId, 0);
            
            // Random start position
            int startX = random.nextInt(GRID_WIDTH - 10) + 5;
            int startY = random.nextInt(GRID_HEIGHT - 10) + 5;
            
            String color = colors[colorIdx % colors.length];
            colorIdx++;

            Snake snake = new Snake(playerId, new Point(startX, startY), 4, color); // 4 = RIGHT
            snakes.put(playerId, snake);
        }
        
        spawnFood();
        this.status = GameStatus.RUNNING;
    }

    private void spawnFood() {
        while (true) {
            int x = random.nextInt(GRID_WIDTH);
            int y = random.nextInt(GRID_HEIGHT);
            Point p = new Point(x, y);
            
            boolean collision = false;
            for (Snake s : snakes.values()) {
                if (s.getBody().contains(p)) {
                    collision = true;
                    break;
                }
            }
            if (!collision) {
                food = p;
                break;
            }
        }
    }

    @Override
    public void processInput(GameInput input) {
        if (status == GameStatus.RUNNING) {
            inputQueue.add(input);
        }
    }

    @Override
    public void tick() {
        if (status != GameStatus.RUNNING) {
            return;
        }

        tickNumber++;

        // Process all inputs for this tick
        while (!inputQueue.isEmpty()) {
            GameInput input = inputQueue.poll();
            Snake s = snakes.get(input.getPlayerId());
            if (s != null && s.isAlive()) {
                int newDir = input.getAction();
                int currentDir = s.getDirection();
                // Prevent 180 turn
                if (newDir >= 1 && newDir <= 4) {
                if (newDir == 1 && currentDir != 2 ||
                    newDir == 2 && currentDir != 1 ||
                    newDir == 3 && currentDir != 4 ||
                    newDir == 4 && currentDir != 3) {
                    s.setDirection(newDir);
                }
                }
            }
        }

        int aliveCount = 0;

        // Move snakes
        for (Snake s : snakes.values()) {
            if (!s.isAlive()) {
                continue;
            }

            Point head = s.getHead();
            int nx = head.x();
            int ny = head.y();
            
            switch (s.getDirection()) {
                case 1 -> ny -= 1; // UP
                case 2 -> ny += 1; // DOWN
                case 3 -> nx -= 1; // LEFT
                case 4 -> nx += 1; // RIGHT
            }
            
            Point newHead = new Point(nx, ny);
            
            // Wall Collision
            if (nx < 0 || nx >= GRID_WIDTH || ny < 0 || ny >= GRID_HEIGHT) {
                s.setAlive(false);
                continue;
            }
            
            // Self Collision
            if (s.getBody().contains(newHead)) {
                s.setAlive(false);
                continue;
            }
            
            // Other Snake Collision
            boolean hitOther = false;
            for (Snake other : snakes.values()) {
                if (other != s && other.isAlive() && other.getBody().contains(newHead)) {
                    hitOther = true;
                    break;
                }
            }
            if (hitOther) {
                s.setAlive(false);
                continue;
            }
            
            // Move
            s.getBody().addFirst(newHead);
            
            // Eat food
            if (newHead.equals(food)) {
                scores.computeIfPresent(s.getPlayerId(), (k, v) -> v + 1);
                spawnFood();
            } else {
                s.getBody().removeLast();
            }
            
            aliveCount++;
        }

        if (aliveCount == 0 && !snakes.isEmpty()) {
            status = GameStatus.FINISHED;
        }
    }

    @Override
    public GameSnapshot snapshot() {
        Map<String, Object> state = new HashMap<>();
        state.put("width", GRID_WIDTH);
        state.put("height", GRID_HEIGHT);
        state.put("food", food);
        
        List<Map<String, Object>> snakesList = new ArrayList<>();
        for (Snake s : snakes.values()) {
            Map<String, Object> snakeMap = new HashMap<>();
            snakeMap.put("id", s.getPlayerId().toString());
            snakeMap.put("alive", s.isAlive());
            snakeMap.put("color", s.getColor());
            snakeMap.put("body", s.getBody());
            snakesList.add(snakeMap);
        }
        state.put("snakes", snakesList);
        
        Map<String, Integer> stringScores = new HashMap<>();
        for (Map.Entry<UUID, Integer> entry : scores.entrySet()) {
            stringScores.put(entry.getKey().toString(), entry.getValue());
        }
        state.put("scores", stringScores);
        state.put("tick", tickNumber);

        byte[] payload = "{}".getBytes(StandardCharsets.UTF_8);
        try {
            payload = EventSerializer.getMapper().writeValueAsBytes(state);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new GameSnapshot(
            context.getGameId(),
            context.getRoomId(),
            context.getGameType(),
            status,
            tickNumber,
            new HashMap<>(scores),
            payload
        );
    }

    @Override
    public boolean isFinished() {
        return status == GameStatus.FINISHED;
    }
}
