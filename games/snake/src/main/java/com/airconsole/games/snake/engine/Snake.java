package com.airconsole.games.snake.engine;

import java.util.LinkedList;
import java.util.UUID;

public class Snake {
    private final UUID playerId;
    private final LinkedList<Point> body = new LinkedList<>();
    private int direction; // 1=UP, 2=DOWN, 3=LEFT, 4=RIGHT
    private boolean alive = true;
    private String color;

    public Snake(UUID playerId, Point start, int direction, String color) {
        this.playerId = playerId;
        this.body.add(start);
        this.direction = direction;
        this.color = color;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public LinkedList<Point> getBody() {
        return body;
    }

    public Point getHead() {
        return body.getFirst();
    }

    public int getDirection() {
        return direction;
    }

    public void setDirection(int direction) {
        this.direction = direction;
    }

    public boolean isAlive() {
        return alive;
    }

    public void setAlive(boolean alive) {
        this.alive = alive;
    }

    public String getColor() {
        return color;
    }
}
