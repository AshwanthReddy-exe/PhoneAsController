package com.airconsole.notification.unit.domain;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.airconsole.common.messaging.EventEnvelope;
import com.airconsole.notification.application.SignalRPublisher;
import com.airconsole.notification.domain.EventRouter;
import com.airconsole.notification.domain.GroupMapper;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("EventRouter")
class EventRouterTest {

    @Mock
    private SignalRPublisher signalRPublisher;

    private GroupMapper groupMapper;
    private EventRouter eventRouter;

    @BeforeEach
    void setUp() {
        groupMapper = new GroupMapper();
        eventRouter = new EventRouter(signalRPublisher, groupMapper);
    }

    @Test
    void routeBroadcastsToCorrectGroup() {
        EventEnvelope<String> envelope = new EventEnvelope<>(
                UUID.randomUUID(),
                "RoomCreatedEvent",
                "room-service",
                "1.0",
                Instant.now(),
                "{}",
                "room-uuid-123"
        );

        eventRouter.route(envelope, "{}");

        verify(signalRPublisher).broadcastToGroup("room:room-uuid-123", "{}");
    }

    @Test
    void routeIgnoresEventsWithoutRoomId() {
        EventEnvelope<String> envelope = new EventEnvelope<>(
                UUID.randomUUID(),
                "SomeEvent",
                "test",
                "1.0",
                Instant.now(),
                "{}",
                null
        );

        eventRouter.route(envelope, "{}");

        verifyNoInteractions(signalRPublisher);
    }

    @Test
    void routeIgnoresBlankRoomId() {
        EventEnvelope<String> envelope = new EventEnvelope<>(
                UUID.randomUUID(),
                "SomeEvent",
                "test",
                "1.0",
                Instant.now(),
                "{}",
                ""
        );

        eventRouter.route(envelope, "{}");

        verifyNoInteractions(signalRPublisher);
    }

    @Test
    void routeToGroupsBroadcastsToAllGroups() {
        EventEnvelope<String> envelope = new EventEnvelope<>(
                UUID.randomUUID(),
                "CrossRoomEvent",
                "test",
                "1.0",
                Instant.now(),
                "{}",
                "room-A"
        );

        eventRouter.routeToGroups(envelope, List.of("room:A", "room:B"), "{}");

        verify(signalRPublisher).broadcastToGroup("room:A", "{}");
        verify(signalRPublisher).broadcastToGroup("room:B", "{}");
    }

    @Test
    void broadcastSystemMessage() {
        eventRouter.broadcastSystemMessage("{\"type\":\"maintenance\"}");

        verify(signalRPublisher).broadcastToAll("{\"type\":\"maintenance\"}");
    }
}