package com.airconsole.player.application;

/**
 * Application port — publishes domain events to messaging infrastructure.
 */
public interface PlayerEventPublisher {

    <T> void publish(T event);
}
