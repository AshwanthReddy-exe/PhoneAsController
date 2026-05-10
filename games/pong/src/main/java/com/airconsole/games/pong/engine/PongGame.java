package com.airconsole.games.pong.engine;

import com.airconsole.common.engine.ControllerLayout;
import com.airconsole.common.engine.GameEngine;
import com.airconsole.common.enums.GameStatus;
import com.airconsole.common.enums.GameType;
import com.airconsole.common.model.GameContext;
import com.airconsole.common.model.GameInput;
import com.airconsole.common.model.GameSnapshot;
import com.airconsole.common.util.EventSerializer;
import com.airconsole.games.pong.domain.PongControllerLayout;
import com.airconsole.games.pong.state.PongState;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class PongGame implements GameEngine {

    // Tick rate: 60fps (16ms)
    public static final long TICK_MS = 16L;

    // Paddle movement speed (rows per tick)
    private static final double PADDLE_SPEED = 2.0;

    private GameContext context;
    private final ControllerLayout layout = new PongControllerLayout();
    private GameStatus status = GameStatus.WAITING;
    private long tickNumber = 0;

    // Ordered list of player IDs — index 0 = left player, index 1 = right player
    private final ConcurrentLinkedQueue<UUID> playerOrder = new ConcurrentLinkedQueue<>();
    private final Map<UUID, Integer> scores = new ConcurrentHashMap<>();

    private final ConcurrentLinkedQueue<GameInput> inputQueue = new ConcurrentLinkedQueue<>();

    // Shared game state
    private final PongState state = new PongState();

    // Paddle Y targets (smooth movement)
    private final Map<UUID, Double> paddleTargets = new ConcurrentHashMap<>();

    // Pending score events for this tick
    private final Set<UUID> scoredThisTick = new HashSet<>();

    @Override
    public GameType getType() {
        return GameType.PONG;
    }

    @Override
    public ControllerLayout getControllerLayout() {
        return layout;
    }

    @Override
    public void initialize(GameContext context) {
        this.context = context;

        int idx = 0;
        for (UUID playerId : context.getPlayerIds()) {
            scores.put(playerId, 0);
            paddleTargets.put(playerId, PongState.HEIGHT / 2.0);
            playerOrder.add(playerId);
            idx++;
        }

        // Init paddle positions based on index
        state.leftPaddleY  = (PongState.HEIGHT - PongState.PADDLE_HEIGHT) / 2.0;
        state.rightPaddleY = (PongState.HEIGHT - PongState.PADDLE_HEIGHT) / 2.0;

        this.status = GameStatus.RUNNING;
    }

    @Override
    public void processInput(GameInput input) {
        if (status != GameStatus.RUNNING) {
            return;
        }
        inputQueue.add(input);
    }

    @Override
    public void tick() {
        if (status != GameStatus.RUNNING) {
            return;
        }

        tickNumber++;
        scoredThisTick.clear();

        // --- Process inputs ---
        while (!inputQueue.isEmpty()) {
            GameInput input = inputQueue.poll();
            UUID playerId = input.getPlayerId();
            int action = input.getAction();

            // Action 3 = UP, Action 4 = DOWN
            if (action == 3 || action == 4) {
                double current = paddleTargets.getOrDefault(playerId, PongState.HEIGHT / 2.0);
                double delta = (action == 3) ? -PADDLE_SPEED : PADDLE_SPEED;
                double next = Math.max(0, Math.min(PongState.HEIGHT - PongState.PADDLE_HEIGHT, current + delta));
                paddleTargets.put(playerId, next);
            }
        }

        // --- Smooth paddle movement (interpolate to target) ---
        // Players are ordered: index 0 = left, index 1 = right
        UUID[] orderArr = playerOrder.toArray(new UUID[0]);
        if (orderArr.length > 0) {
            Double leftTarget = paddleTargets.get(orderArr[0]);
            if (leftTarget != null) {
                state.leftPaddleY = clampPaddle(leftTarget);
            }
        }
        if (orderArr.length > 1) {
            Double rightTarget = paddleTargets.get(orderArr[1]);
            if (rightTarget != null) {
                state.rightPaddleY = clampPaddle(rightTarget);
            }
        }

        // --- Move ball ---
        state.moveBall();

        // --- Wall bounce (top/bottom) ---
        state.bounceWall();

        // --- Paddle collision ---
        handlePaddleCollision();

        // --- Scoring ---
        handleScoring(orderArr);

        // --- Check win condition ---
        checkWin();
    }

    private void handlePaddleCollision() {
        // Left paddle collision
        double leftPaddleTop = state.leftPaddleY;
        double leftPaddleBottom = leftPaddleTop + PongState.PADDLE_HEIGHT;
        if (state.ballVX < 0
                && state.ballX - PongState.BALL_RADIUS <= PongState.PADDLE_X_OFFSET + 1.0
                && state.ballX + PongState.BALL_RADIUS >= PongState.PADDLE_X_OFFSET
                && state.ballY >= leftPaddleTop - PongState.BALL_RADIUS
                && state.ballY <= leftPaddleBottom + PongState.BALL_RADIUS) {
            // Bounce off left paddle — reflect and add slight angle based on hit position
            state.ballX = PongState.PADDLE_X_OFFSET + 1.0 + PongState.BALL_RADIUS;
            state.ballVX = Math.abs(state.ballVX);
            double hitRatio = (state.ballY - leftPaddleTop) / PongState.PADDLE_HEIGHT;
            state.ballVY = PongState.BALL_SPEED * (hitRatio * 2.0 - 1.0) * 0.75;
        }

        // Right paddle collision
        double rightPaddleX = PongState.WIDTH - PongState.PADDLE_X_OFFSET - 1.0;
        double rightPaddleTop = state.rightPaddleY;
        double rightPaddleBottom = rightPaddleTop + PongState.PADDLE_HEIGHT;
        if (state.ballVX > 0
                && state.ballX + PongState.BALL_RADIUS >= rightPaddleX
                && state.ballX - PongState.BALL_RADIUS <= rightPaddleX + 1.0
                && state.ballY >= rightPaddleTop - PongState.BALL_RADIUS
                && state.ballY <= rightPaddleBottom + PongState.BALL_RADIUS) {
            // Bounce off right paddle
            state.ballX = rightPaddleX - PongState.BALL_RADIUS;
            state.ballVX = -Math.abs(state.ballVX);
            double hitRatio = (state.ballY - rightPaddleTop) / PongState.PADDLE_HEIGHT;
            state.ballVY = PongState.BALL_SPEED * (hitRatio * 2.0 - 1.0) * 0.75;
        }
    }

    private void handleScoring(UUID[] orderArr) {
        if (state.pastLeftWall() && orderArr.length > 1) {
            // Right player (index 1) scores
            UUID scorer = orderArr[1];
            scores.computeIfPresent(scorer, (k, v) -> v + 1);
            scoredThisTick.add(scorer);
            state.resetBall();
        } else if (state.pastRightWall() && orderArr.length > 0) {
            // Left player (index 0) scores
            UUID scorer = orderArr[0];
            scores.computeIfPresent(scorer, (k, v) -> v + 1);
            scoredThisTick.add(scorer);
            state.resetBall();
        }
    }

    private void checkWin() {
        for (Map.Entry<UUID, Integer> entry : scores.entrySet()) {
            if (entry.getValue() >= PongState.WIN_SCORE) {
                status = GameStatus.FINISHED;
                return;
            }
        }
    }

    private double clampPaddle(double y) {
        return Math.max(0, Math.min(PongState.HEIGHT - PongState.PADDLE_HEIGHT, y));
    }

    @Override
    public GameSnapshot snapshot() {
        Map<String, Object> s = new HashMap<>();
        s.put("width", PongState.WIDTH);
        s.put("height", PongState.HEIGHT);
        s.put("ballX", Math.round(state.ballX * 100.0) / 100.0);
        s.put("ballY", Math.round(state.ballY * 100.0) / 100.0);
        s.put("ballVX", Math.round(state.ballVX * 100.0) / 100.0);
        s.put("ballVY", Math.round(state.ballVY * 100.0) / 100.0);
        s.put("leftPaddleY", Math.round(state.leftPaddleY * 100.0) / 100.0);
        s.put("rightPaddleY", Math.round(state.rightPaddleY * 100.0) / 100.0);
        s.put("paddleHeight", PongState.PADDLE_HEIGHT);

        // Scores as string-keyed map
        Map<String, Integer> stringScores = new HashMap<>();
        for (Map.Entry<UUID, Integer> entry : scores.entrySet()) {
            stringScores.put(entry.getKey().toString(), entry.getValue());
        }
        s.put("scores", stringScores);
        s.put("tick", tickNumber);
        s.put("winScore", PongState.WIN_SCORE);

        byte[] payload;
        try {
            payload = EventSerializer.getMapper().writeValueAsBytes(s);
        } catch (Exception e) {
            e.printStackTrace();
            payload = "{}".getBytes(StandardCharsets.UTF_8);
        }

        // Clone scores for snapshot
        Map<UUID, Integer> snapshotScores = new HashMap<>(scores);

        return new GameSnapshot(
            context.getGameId(),
            context.getRoomId(),
            context.getGameType(),
            status,
            tickNumber,
            snapshotScores,
            payload
        );
    }

    @Override
    public boolean isFinished() {
        return status == GameStatus.FINISHED;
    }
}