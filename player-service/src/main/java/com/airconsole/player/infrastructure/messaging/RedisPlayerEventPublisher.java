package com.airconsole.player.infrastructure.messaging;

import com.airconsole.common.messaging.EventEnvelope;
import com.airconsole.common.util.EventSerializer;
import com.airconsole.player.application.PlayerEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisPlayerEventPublisher implements PlayerEventPublisher {

    private static final Logger LOG = LoggerFactory.getLogger(RedisPlayerEventPublisher.class);

    private final StringRedisTemplate redisTemplate;

    public RedisPlayerEventPublisher(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public <T> void publish(T event) {
        try {
            String streamKey = com.airconsole.common.messaging.ChannelNames.STREAM_PLAYER;
            
            com.fasterxml.jackson.databind.JsonNode rootNode = com.airconsole.common.util.EventSerializer.getMapper().valueToTree(event);
            String roomId = rootNode.has("roomId") ? rootNode.get("roomId").asText() : null;

            EventEnvelope<T> envelope = new EventEnvelope<>(
                java.util.UUID.randomUUID(),
                event.getClass().getSimpleName(),
                "player-service",
                "1.0",
                java.time.Instant.now(),
                event,
                roomId
            );
            String json = EventSerializer.serializeEnvelope(envelope);
            redisTemplate.opsForStream().add(streamKey, java.util.Map.of("data", json));
            LOG.debug("Published player event to {}", streamKey);
        } catch (Exception ex) {
            LOG.error("Failed to publish player event", ex);
            throw new RuntimeException("Player event publish failed", ex);
        }
    }
}
