package com.airconsole.room.application;

/**
 * Application port — publishes domain events to messaging infrastructure.
 * Infrastructure provides Redis adapter.
 */
public interface RoomEventPublisher {

    <T> void publish(T event);
}
