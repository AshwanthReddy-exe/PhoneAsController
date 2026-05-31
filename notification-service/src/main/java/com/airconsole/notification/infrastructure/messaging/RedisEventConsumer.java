package com.airconsole.notification.infrastructure.messaging;

import com.airconsole.common.messaging.EventEnvelope;
import com.airconsole.notification.domain.EventRouter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Component;

/**
 * Consumes events from Redis Streams and routes them to SignalR groups via EventRouter.
 *
 * <p>Uses the spring-data-redis 3.3.0 StreamListener API (not the deprecated
 * StreamMessageListener interface which was removed in this version).
 *
 * <p>Events consumed from:
 * <ul>
 *   <li>{@code airconsole.stream.room}</li>
 *   <li>{@code airconsole.stream.player}</li>
 *   <li>{@code airconsole.stream.game}</li>
 * </ul>
 */
@Component
public class RedisEventConsumer implements StreamListener<String, MapRecord<String, String, String>> {

    private static final Logger LOG = LoggerFactory.getLogger(RedisEventConsumer.class);

    private final EventRouter eventRouter;
    private final ObjectMapper objectMapper;

    public RedisEventConsumer(EventRouter eventRouter, ObjectMapper objectMapper) {
        this.eventRouter = eventRouter;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onMessage(MapRecord<String, String, String> record) {
        // The stream entry value is a Map<String, String> — we stored the EventEnvelope JSON
        // under the key "data" when producing to the stream.
        String rawPayload = record.getValue().get("data");
        if (rawPayload == null || rawPayload.isBlank()) {
            LOG.warn("Stream entry {} has no 'data' field, skipping", record.getId());
            return;
        }

        String streamId = record.getId().getValue();
        LOG.debug("Received message from stream {}, id={}: {}",
                record.getStream(), streamId, truncate(rawPayload, 120));

        try {
            // Deserialize the EventEnvelope — roomId is at the top level of the envelope
            EventEnvelope<JsonNode> envelope = objectMapper.readValue(
                    rawPayload,
                    new com.fasterxml.jackson.core.type.TypeReference<>() { }
            );

            // roomId is now a first-class field on EventEnvelope (added in Phase 4)
            String roomId = envelope.getRoomId();

            // Build a routed envelope with the extracted roomId
            EventEnvelope<JsonNode> routedEnvelope = envelope.withRoomId(roomId);

            // Pass the raw JSON so EventRouter doesn't need to re-serialize
            eventRouter.route(routedEnvelope, rawPayload);

        } catch (Exception e) {
            LOG.error("Failed to process message from stream {} id={}: {}",
                    record.getStream(), streamId, e.getMessage());
            // Do NOT re-throw — we don't want to re-read this message forever in prod.
        }
    }

    private String truncate(String s, int max) {
        if (s == null) {
            return "null";
        }
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}