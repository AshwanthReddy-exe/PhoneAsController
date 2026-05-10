package com.airconsole.common.messaging;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.UUID;

/**
 * Wraps every domain event with metadata for traceability and idempotency.
 * Immutable. Thread-safe (all fields final).
 */
public final class EventEnvelope<T> {

    private final UUID eventId;
    private final String eventType;
    private final String source;
    private final String version;
    private final Instant timestamp;
    private final T payload;
    private final String roomId;

    /**
     * Creates envelope with auto-generated eventId and current timestamp.
     * Convenience for services that don't track roomId yet.
     */
    public EventEnvelope(String eventType, String source, String version, T payload) {
        this(UUID.randomUUID(), eventType, source, version, Instant.now(), payload, null);
    }

    /**
     * Creates envelope with all fields explicit.
     * roomId is optional and may be null.
     */
    public EventEnvelope(UUID eventId, String eventType, String source, String version,
                         Instant timestamp, T payload, String roomId) {
        this.eventId = eventId != null ? eventId : UUID.randomUUID();
        this.eventType = eventType;
        this.source = source;
        this.version = version;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
        this.payload = payload;
        this.roomId = roomId;
    }

    /** For Jackson only — delegates to the public constructor. */
    @JsonCreator
    static <T> EventEnvelope<T> fromJackson(
            @JsonProperty("eventId") UUID eventId,
            @JsonProperty("eventType") String eventType,
            @JsonProperty("source") String source,
            @JsonProperty("version") String version,
            @JsonProperty("timestamp") Instant timestamp,
            @JsonProperty("payload") T payload,
            @JsonProperty("roomId") String roomId) {
        return new EventEnvelope<>(eventId, eventType, source, version, timestamp, payload, roomId);
    }

    public UUID getEventId() { return eventId; }
    public String getEventType() { return eventType; }
    public String getSource() { return source; }
    public String getVersion() { return version; }
    public Instant getTimestamp() { return timestamp; }
    public T getPayload() { return payload; }

    /** Room ID for SignalR group routing. May be null. */
    public String getRoomId() { return roomId; }

    /**
     * Creates a new envelope with the same fields but a different roomId.
     * Useful for routing an envelope to a specific SignalR group.
     */
    public EventEnvelope<T> withRoomId(String roomId) {
        return new EventEnvelope<>(eventId, eventType, source, version, timestamp, payload, roomId);
    }

    @Override
    public String toString() {
        return "EventEnvelope{eventId=" + eventId
                + ", eventType='" + eventType + '\''
                + ", source='" + source + '\''
                + ", version='" + version + '\''
                + ", timestamp=" + timestamp
                + ", roomId=" + roomId
                + '}';
    }
}