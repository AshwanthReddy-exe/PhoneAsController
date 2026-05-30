package com.airconsole.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Notification Service — SignalR bridge for AirConsole.
 *
 * <p>Stateless service that:
 * <ul>
 *   <li>Consumes all events from Redis Streams</li>
 *   <li>Bridges them to Azure SignalR groups (rooms)</li>
 *   <li>Handles SignalR connect/disconnect webhooks</li>
 *   <li>Updates player-service on connection lifecycle</li>
 * </ul>
 *
 * <p>No database — all state lives in Redis or the SignalR service itself.
 */
@SpringBootApplication
@EnableScheduling
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}