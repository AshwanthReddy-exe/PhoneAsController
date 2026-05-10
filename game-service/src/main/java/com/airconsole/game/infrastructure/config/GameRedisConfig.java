package com.airconsole.game.infrastructure.config;

import com.airconsole.common.events.PlayerDisconnectedEvent;
import com.airconsole.common.events.PlayerRejoinedEvent;
import com.airconsole.common.events.RoomExpiredEvent;
import com.airconsole.common.messaging.ChannelNames;
import com.airconsole.common.messaging.EventEnvelope;
import com.airconsole.common.util.EventSerializer;
import com.airconsole.game.application.GameEventHandler;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.StreamMessageListenerContainer.StreamMessageListenerContainerOptions;

/**
 * Redis Streams configuration for game-service.
 *
 * <p>Subscribes GameEventHandler to player and room streams to receive
 * PlayerDisconnectedEvent, PlayerRejoinedEvent, and RoomExpiredEvent.
 */
@Configuration
public class GameRedisConfig {

    private static final Logger LOG = LoggerFactory.getLogger(GameRedisConfig.class);

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${redis.streams.block-duration:2000}")
    private long blockDurationMs;

    @Bean
    public StreamMessageListenerContainer<String, MapRecord<String, String, String>> gameStreamListenerContainer(
            RedisConnectionFactory connectionFactory,
            GameEventHandler eventHandler) {

        StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> options =
                StreamMessageListenerContainerOptions
                        .<String, MapRecord<String, String, String>>builder()
                        .batchSize(10)
                        .pollTimeout(Duration.ofMillis(blockDurationMs))
                        .build();

        StreamMessageListenerContainer<String, MapRecord<String, String, String>> container =
                StreamMessageListenerContainer.create(connectionFactory, options);

        subscribeStream(container, connectionFactory, ChannelNames.STREAM_PLAYER, ChannelNames.CG_GAME_SERVICE, "game-svc-player", new PlayerStreamListener(eventHandler));
        subscribeStream(container, connectionFactory, ChannelNames.STREAM_ROOM, ChannelNames.CG_GAME_SERVICE, "game-svc-room", new RoomStreamListener(eventHandler));

        container.start();
        return container;
    }

    private void subscribeStream(StreamMessageListenerContainer<String, MapRecord<String, String, String>> container,
                                 RedisConnectionFactory connectionFactory,
                                 String stream, String group, String consumerName,
                                 StreamListener<String, MapRecord<String, String, String>> listener) {
        ensureConsumerGroup(stream, group, connectionFactory);
        StreamOffset<String> offset = StreamOffset.create(stream, ReadOffset.lastConsumed());
        Consumer consumer = Consumer.from(group, consumerName);
        container.receive(consumer, offset, listener);
        LOG.info("Subscribed to stream: {} with consumer group: {}", stream, group);
    }

    private void ensureConsumerGroup(String streamKey, String groupName, RedisConnectionFactory connectionFactory) {
        try {
            StringRedisTemplate template = new StringRedisTemplate(connectionFactory);
            template.afterPropertiesSet();
            template.opsForStream().createGroup(streamKey, ReadOffset.from("0"), groupName);
            LOG.info("Created consumer group {} for stream: {}", groupName, streamKey);
        } catch (Exception e) {
            LOG.debug("Consumer group already exists for stream {}: {}", streamKey, e.getMessage());
        }
    }

    private static class PlayerStreamListener implements StreamListener<String, MapRecord<String, String, String>> {

        private static final Logger LOG = LoggerFactory.getLogger(PlayerStreamListener.class);
        private final GameEventHandler handler;

        PlayerStreamListener(GameEventHandler handler) {
            this.handler = handler;
        }

        @Override
        public void onMessage(MapRecord<String, String, String> record) {
            try {
                String payload = record.getValue().get("data");
                if (payload == null || payload.isBlank()) {
                    LOG.warn("Stream entry {} has no 'data' field, skipping", record.getId());
                    return;
                }

                EventEnvelope<?> genericEnvelope = EventSerializer.getMapper().readValue(payload, EventEnvelope.class);
                String eventType = genericEnvelope.getEventType();

                if ("PlayerDisconnectedEvent".equals(eventType)) {
                    EventEnvelope<PlayerDisconnectedEvent> envelope = EventSerializer.deserializeEnvelopeSafe(payload, PlayerDisconnectedEvent.class);
                    handler.onPlayerDisconnected(envelope.getPayload());
                } else if ("PlayerRejoinedEvent".equals(eventType)) {
                    EventEnvelope<PlayerRejoinedEvent> envelope = EventSerializer.deserializeEnvelopeSafe(payload, PlayerRejoinedEvent.class);
                    handler.onPlayerRejoined(envelope.getPayload());
                } else {
                    LOG.debug("Ignoring event type {} on player stream", eventType);
                }
            } catch (Exception e) {
                LOG.error("Failed to process message from player stream: {}", record.getId(), e);
            }
        }
    }

    private static class RoomStreamListener implements StreamListener<String, MapRecord<String, String, String>> {

        private static final Logger LOG = LoggerFactory.getLogger(RoomStreamListener.class);
        private final GameEventHandler handler;

        RoomStreamListener(GameEventHandler handler) {
            this.handler = handler;
        }

        @Override
        public void onMessage(MapRecord<String, String, String> record) {
            try {
                String payload = record.getValue().get("data");
                if (payload == null || payload.isBlank()) {
                    LOG.warn("Stream entry {} has no 'data' field, skipping", record.getId());
                    return;
                }

                EventEnvelope<?> genericEnvelope = EventSerializer.getMapper().readValue(payload, EventEnvelope.class);
                String eventType = genericEnvelope.getEventType();

                if ("RoomExpiredEvent".equals(eventType)) {
                    EventEnvelope<RoomExpiredEvent> envelope = EventSerializer.deserializeEnvelopeSafe(payload, RoomExpiredEvent.class);
                    handler.onRoomExpired(envelope.getPayload());
                } else {
                    LOG.debug("Ignoring event type {} on room stream", eventType);
                }
            } catch (Exception e) {
                LOG.error("Failed to process message from room stream: {}", record.getId(), e);
            }
        }
    }
}
