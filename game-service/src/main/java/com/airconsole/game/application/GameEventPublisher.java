package com.airconsole.game.application;

/**
 * Application port — publishes domain events to messaging infrastructure.
 */
public interface GameEventPublisher {

    <T> void publish(T event);
}
