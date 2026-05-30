package com.airconsole.notification.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Message sent over SignalR to frontend clients (screen/controller).
 * The "target" field tells the client which method to invoke on the SignalR hub.
 */
public record WebSocketMessage(

        /** Event type matching the class name of the domain event, e.g. "RoomCreatedEvent" */
        @JsonProperty("target") String target,

        /** The full JSON event payload */
        @JsonProperty("payload") String payload,

        /** Optional room code for routing on the client side */
        @JsonProperty("roomCode") String roomCode,

        /** Timestamp in ISO-8601 */
        @JsonProperty("timestamp") String timestamp
) {
    public static WebSocketMessage of(String target, String payload, String roomCode) {
        return new WebSocketMessage(target, payload, roomCode, java.time.Instant.now().toString());
    }
}