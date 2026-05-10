package com.airconsole.score.infrastructure.messaging;

import com.airconsole.common.messaging.ChannelNames;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.StreamMessageListenerContainer.StreamMessageListenerContainerOptions;

@Configuration
public class ScoreRedisConfig {

    private static final Logger LOG = LoggerFactory.getLogger(ScoreRedisConfig.class);

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(redisHost, redisPort);
        return new LettuceConnectionFactory(config);
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }

    @Bean
    public StreamMessageListenerContainer<String, MapRecord<String, String, String>> streamListenerContainer(
            RedisConnectionFactory connectionFactory,
            @Value("${redis.streams.block-duration:2000}") long blockDurationMs,
            RedisGameFinishedListener eventConsumer) {

        StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> options =
                StreamMessageListenerContainerOptions
                        .<String, MapRecord<String, String, String>>builder()
                        .batchSize(10)
                        .pollTimeout(Duration.ofMillis(blockDurationMs))
                        .build();

        StreamMessageListenerContainer<String, MapRecord<String, String, String>> container =
                StreamMessageListenerContainer.create(connectionFactory, options);

        String stream = ChannelNames.STREAM_GAME;
        String group = ChannelNames.CG_SCORE_SERVICE;

        ensureConsumerGroup(stream, group, connectionFactory);

        StreamOffset<String> offset = StreamOffset.create(stream, ReadOffset.lastConsumed());
        Consumer consumer = Consumer.from(group, "score-service-1");

        container.receive(consumer, offset, eventConsumer);
        LOG.info("Subscribed to stream: {} with consumer group: {}", stream, group);

        container.start();
        return container;
    }

    private void ensureConsumerGroup(String streamKey, String groupName, RedisConnectionFactory connectionFactory) {
        try {
            var template = new StringRedisTemplate(connectionFactory);
            template.afterPropertiesSet();
            template.opsForStream().createGroup(streamKey, ReadOffset.from("0"), groupName);
            LOG.info("Created consumer group {} for stream: {}", groupName, streamKey);
        } catch (Exception e) {
            LOG.debug("Consumer group already exists for stream {}: {}", streamKey, e.getMessage());
        }
    }
}
