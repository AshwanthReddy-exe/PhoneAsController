package com.airconsole.score.infrastructure.messaging;

import com.airconsole.common.events.GameFinishedEvent;
import com.airconsole.common.messaging.EventEnvelope;
import com.airconsole.common.util.EventSerializer;
import com.airconsole.score.application.GameFinishedEventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Component;

@Component
public class RedisGameFinishedListener implements StreamListener<String, MapRecord<String, String, String>> {

    private static final Logger log = LoggerFactory.getLogger(RedisGameFinishedListener.class);

    private final GameFinishedEventHandler eventHandler;

    public RedisGameFinishedListener(GameFinishedEventHandler eventHandler) {
        this.eventHandler = eventHandler;
    }

    @Override
    public void onMessage(MapRecord<String, String, String> message) {
        try {
            String payload = message.getValue().get("data");
            if (payload == null) {
                return;
            }

            EventEnvelope<?> genericEnvelope = EventSerializer.getMapper().readValue(payload, EventEnvelope.class);
            if ("GameFinishedEvent".equals(genericEnvelope.getEventType())) {
                EventEnvelope<GameFinishedEvent> envelope = EventSerializer.deserializeEnvelopeSafe(payload, GameFinishedEvent.class);
                eventHandler.handle(envelope.getPayload());
            }
        } catch (Exception e) {
            log.error("Failed to process message from stream: {}", message.getId(), e);
        }
    }
}
