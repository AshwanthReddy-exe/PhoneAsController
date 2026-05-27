package com.airconsole.games.pong.state;

import java.util.Map;
import java.util.UUID;

/**
 * Pure data class holding the current game state.
 * All fields are package-private for direct access from PongGame.
 */
public class PongState {

    // World dimensions (logical units)
    public static final int WIDTH  = 60;   // columns
    public static final int HEIGHT = 40;   // rows
    public static final int WIN_SCORE = 5;
    public static final double BALL_RADIUS = 0.5;
    public static final double BALL_SPEED  = 5.0;   // units per tick
    public static final double PADDLE_HEIGHT = 6.0;
    public static final double PADDLE_X_OFFSET = 2.0; // distance from wall

    // Ball
    public double ballX;
    public double ballY;
    public double ballVX;   // velocity X
    public double ballVY;   // velocity Y

    // Paddles (y = top row, height = 6 rows)
    public double leftPaddleY;
    public double rightPaddleY;

    // Scores keyed by player UUID
    public Map<UUID, Integer> scores;

    public PongState() {
        resetBall();
        leftPaddleY  = (HEIGHT - PADDLE_HEIGHT) / 2.0;
        rightPaddleY = (HEIGHT - PADDLE_HEIGHT) / 2.0;
    }

    /** Reset ball to center with random diagonal direction. */
    public void resetBall() {
        ballX = WIDTH / 2.0;
        ballY = HEIGHT / 2.0;
        // Random angle between -45 and +45 degrees, random side
        double angle = (Math.random() * 90.0 - 45.0) * Math.PI / 180.0;
        int dir = Math.random() < 0.5 ? 1 : -1;
        ballVX = dir * BALL_SPEED * Math.cos(angle);
        ballVY = BALL_SPEED * Math.sin(angle);
    }

    /** Move ball one step. Call after collision resolution each tick. */
    public void moveBall() {
        ballX += ballVX;
        ballY += ballVY;
    }

    /** Bounce ball off top or bottom wall. */
    public void bounceWall() {
        if (ballY - BALL_RADIUS <= 0) {
            ballY = BALL_RADIUS;
            ballVY = Math.abs(ballVY);
        } else if (ballY + BALL_RADIUS >= HEIGHT) {
            ballY = HEIGHT - BALL_RADIUS;
            ballVY = -Math.abs(ballVY);
        }
    }

    /** Returns true if ball is past the left wall (right player scores). */
    public boolean pastLeftWall() {
        return ballX - BALL_RADIUS < 0;
    }

    /** Returns true if ball is past the right wall (left player scores). */
    public boolean pastRightWall() {
        return ballX + BALL_RADIUS > WIDTH;
    }
}