package com.airconsole.room.infrastructure.messaging;

import com.airconsole.common.messaging.EventEnvelope;
import com.airconsole.common.util.EventSerializer;
import com.airconsole.room.application.RoomEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis Streams adapter for RoomEventPublisher port.
 */
@Component
public class RedisRoomEventPublisher implements RoomEventPublisher {

    private static final Logger LOG = LoggerFactory.getLogger(RedisRoomEventPublisher.class);

    private final StringRedisTemplate redisTemplate;

    public RedisRoomEventPublisher(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public <T> void publish(T event) {
        try {
            String streamKey = com.airconsole.common.messaging.ChannelNames.STREAM_ROOM;
            
            com.fasterxml.jackson.databind.JsonNode rootNode = com.airconsole.common.util.EventSerializer.getMapper().valueToTree(event);
            String roomId = rootNode.has("roomId") ? rootNode.get("roomId").asText() : null;

            EventEnvelope<T> envelope = new EventEnvelope<>(
                java.util.UUID.randomUUID(),
                event.getClass().getSimpleName(),
                "room-service",
                "1.0",
                java.time.Instant.now(),
                event,
                roomId
            );
            String json = EventSerializer.serializeEnvelope(envelope);
            redisTemplate.opsForStream().add(streamKey, java.util.Map.of("data", json));
            LOG.debug("Published event to stream {}: {}", streamKey, json);
        } catch (Exception ex) {
            LOG.error("Failed to publish event: {}", event, ex);
            throw new RuntimeException("Event publish failed", ex);
        }
    }
}
