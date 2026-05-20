package com.airconsole.room.infrastructure.messaging;

import com.airconsole.common.messaging.ChannelNames;
import com.airconsole.common.messaging.EventEnvelope;
import com.airconsole.common.util.EventSerializer;
import com.airconsole.room.application.RoomEventHandler;
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
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.StreamMessageListenerContainer.StreamMessageListenerContainerOptions;
import org.springframework.data.redis.stream.StreamListener;

/**
 * Redis Streams configuration for room-service.
 *
 * <p>Subscribes RoomEventHandler to {@code airconsole.stream.player} to receive
 * PlayerJoinedEvent and PlayerLeftEvent, updating denormalized playerCount on
 * Room entities.
 */
@Configuration
public class RoomRedisConfig {

    private static final Logger LOG = LoggerFactory.getLogger(RoomRedisConfig.class);

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${redis.streams.block-duration:2000}")
    private long blockDurationMs;

    private final RoomEventHandler eventHandler;

    public RoomRedisConfig(RoomEventHandler eventHandler) {
        this.eventHandler = eventHandler;
    }

    @Bean
    public StreamMessageListenerContainer<String, MapRecord<String, String, String>> streamListenerContainer(
            RedisConnectionFactory connectionFactory) {

        StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> options =
                StreamMessageListenerContainerOptions
                        .<String, MapRecord<String, String, String>>builder()
                        .batchSize(10)
                        .pollTimeout(Duration.ofMillis(blockDurationMs))
                        .build();

        StreamMessageListenerContainer<String, MapRecord<String, String, String>> container =
                StreamMessageListenerContainer.create(connectionFactory, options);

        String stream = ChannelNames.STREAM_PLAYER;
        String group = ChannelNames.CG_ROOM_SERVICE;

        ensureConsumerGroup(stream, group, connectionFactory);

        StreamOffset<String> offset = StreamOffset.create(stream, ReadOffset.lastConsumed());
        Consumer consumer = Consumer.from(group, "room-service-1");

        container.receive(consumer, offset, new RoomPlayerStreamListener(eventHandler));
        LOG.info("Subscribed to stream: {} with consumer group: {}", stream, group);

        container.start();
        return container;
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

    /**
     * Adapts raw MapRecord stream messages to RoomEventHandler callbacks.
     */
    private static class RoomPlayerStreamListener
            implements StreamListener<String, MapRecord<String, String, String>> {

        private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(RoomPlayerStreamListener.class);

        private final RoomEventHandler handler;

        RoomPlayerStreamListener(RoomEventHandler handler) {
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

                EventEnvelope<?> genericEnvelope =
                        EventSerializer.getMapper().readValue(payload, EventEnvelope.class);
                String eventType = genericEnvelope.getEventType();

                if ("PlayerJoinedEvent".equals(eventType)) {
                    EventEnvelope<com.airconsole.common.events.PlayerJoinedEvent> envelope =
                            EventSerializer.deserializeEnvelopeSafe(payload,
                                    com.airconsole.common.events.PlayerJoinedEvent.class);
                    handler.onPlayerJoined(envelope.getPayload());
                } else if ("PlayerLeftEvent".equals(eventType)) {
                    EventEnvelope<com.airconsole.common.events.PlayerLeftEvent> envelope =
                            EventSerializer.deserializeEnvelopeSafe(payload,
                                    com.airconsole.common.events.PlayerLeftEvent.class);
                    handler.onPlayerLeft(envelope.getPayload());
                } else {
                    LOG.debug("Ignoring event type {} on player stream", eventType);
                }
            } catch (Exception e) {
                LOG.error("Failed to process message from player stream: {}", record.getId(), e);
            }
        }
    }
}