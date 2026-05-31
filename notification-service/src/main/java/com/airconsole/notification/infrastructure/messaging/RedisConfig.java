package com.airconsole.notification.infrastructure.messaging;

import com.airconsole.common.messaging.ChannelNames;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.StreamMessageListenerContainer.StreamMessageListenerContainerOptions;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Redis configuration for the notification-service.
 *
 * <p>Sets up:
 * <ul>
 *   <li>StringRedisTemplate (for pub/sub and stream reading)</li>
 *   <li>StreamMessageListenerContainer (consumer group subscription)</li>
 *   <li>RedisEventConsumer (the actual message handler)</li>
 * </ul>
 *
 * <p>spring-data-redis 3.3.0 uses MapRecord (which implements Record) — NOT ObjectRecord.
 */
@Configuration
@EnableScheduling
public class RedisConfig {

    private static final Logger LOG = LoggerFactory.getLogger(RedisConfig.class);

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${redis.streams.auto-create-groups:true}")
    private boolean autoCreateGroups;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(redisHost, redisPort);
        return new LettuceConnectionFactory(config);
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }

    /**
     * Creates the StreamMessageListenerContainer that listens to all event streams.
     *
     * <p>Uses consumer group "notification-service-cg" to track which events
     * have been acknowledged. Events are read with BLOCK to minimize polling.
     */
    @Bean
    public StreamMessageListenerContainer<String, MapRecord<String, String, String>> streamListenerContainer(
            RedisConnectionFactory connectionFactory,
            @Value("${redis.streams.block-duration:2000}") long blockDurationMs,
            RedisEventConsumer eventConsumer) {

        StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> options =
                StreamMessageListenerContainerOptions
                        .<String, MapRecord<String, String, String>>builder()
                        .batchSize(10)
                        .pollTimeout(Duration.ofMillis(blockDurationMs))
                        .build();

        StreamMessageListenerContainer<String, MapRecord<String, String, String>> container =
                StreamMessageListenerContainer.create(connectionFactory, options);

        // Subscribe to all three event streams
        for (String stream : new String[]{
                ChannelNames.STREAM_ROOM,
                ChannelNames.STREAM_PLAYER,
                ChannelNames.STREAM_GAME}) {

            ensureConsumerGroup(stream, connectionFactory);

            StreamOffset<String> offset = StreamOffset.create(
                    stream,
                    ReadOffset.lastConsumed()
            );
            Consumer consumer = Consumer.from(
                    ChannelNames.CG_NOTIFICATION_SERVICE,
                    "notification-service-1"
            );

            container.receive(consumer, offset, eventConsumer);
            LOG.info("Subscribed to stream: {} with consumer group: {}",
                    stream, ChannelNames.CG_NOTIFICATION_SERVICE);
        }

        container.start();
        return container;
    }

    private void ensureConsumerGroup(String streamKey, RedisConnectionFactory connectionFactory) {
        try {
            var template = new StringRedisTemplate(connectionFactory);
            template.afterPropertiesSet();
            template.opsForStream().createGroup(streamKey, ReadOffset.from("0"), ChannelNames.CG_NOTIFICATION_SERVICE);
            LOG.info("Created consumer group for stream: {}", streamKey);
        } catch (Exception e) {
            // Group already exists — safe to ignore
            LOG.debug("Consumer group already exists for stream {}: {}", streamKey, e.getMessage());
        }
    }
}