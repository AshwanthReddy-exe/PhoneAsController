package com.airconsole.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import com.airconsole.common.enums.GameType;
import com.airconsole.common.events.GameStartedEvent;
import com.airconsole.common.events.RoomCreatedEvent;
import com.airconsole.common.messaging.EventEnvelope;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EventSerializerTest {

    @Test
    void serializeAndDeserializeEvent() throws Exception {
        UUID roomId = UUID.randomUUID();
        EventEnvelope<RoomCreatedEvent> envelope = new EventEnvelope<>(
            "room.created",
            "room-service",
            "1.0",
            new RoomCreatedEvent(
                roomId,
                "ABCDE",
                GameType.SNAKE,
                UUID.randomUUID(),
                4,
                Instant.now()
            )
        );

        String json = EventSerializer.serialize(envelope);
        assertThat(json).contains("room.created").contains(roomId.toString());
    }

    @Test
    void deserializeEnvelope() throws Exception {
        String json = """
            {
                "eventId": "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11",
                "eventType": "game.started",
                "source": "game-service",
                "version": "1.0",
                "timestamp": "2025-01-01T00:00:00Z",
                "payload": {
                    "gameId": "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12",
                    "roomId": "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13",
                    "gameType": "SNAKE",
                    "startedAt": "2025-01-01T00:00:00Z"
                }
            }
            """;

        EventEnvelope<GameStartedEvent> envelope =
            EventSerializer.deserializeEnvelopeSafe(json, GameStartedEvent.class);

        assertThat(envelope.getEventType()).isEqualTo("game.started");
        assertThat(envelope.getPayload().getGameType()).isEqualTo(GameType.SNAKE);
    }
}
