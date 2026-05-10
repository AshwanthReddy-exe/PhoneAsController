package com.airconsole.game.infrastructure.messaging;

import com.airconsole.common.messaging.EventEnvelope;
import com.airconsole.common.util.EventSerializer;
import com.airconsole.game.application.GameEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisGameEventPublisher implements GameEventPublisher {

    private static final Logger LOG = LoggerFactory.getLogger(RedisGameEventPublisher.class);

    private final StringRedisTemplate redisTemplate;

    public RedisGameEventPublisher(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public <T> void publish(T event) {
        try {
            String streamKey = com.airconsole.common.messaging.ChannelNames.STREAM_GAME;
            
            com.fasterxml.jackson.databind.JsonNode rootNode = com.airconsole.common.util.EventSerializer.getMapper().valueToTree(event);
            String roomId = rootNode.has("roomId") ? rootNode.get("roomId").asText() : null;

            EventEnvelope<T> envelope = new EventEnvelope<>(
                java.util.UUID.randomUUID(),
                event.getClass().getSimpleName(),
                "game-service",
                "1.0",
                java.time.Instant.now(),
                event,
                roomId
            );
            String json = EventSerializer.serializeEnvelope(envelope);
            redisTemplate.opsForStream().add(streamKey, java.util.Map.of("data", json));
            LOG.debug("Published game event to {}", streamKey);
        } catch (Exception ex) {
            LOG.error("Failed to publish game event", ex);
            throw new RuntimeException("Game event publish failed", ex);
        }
    }
}
