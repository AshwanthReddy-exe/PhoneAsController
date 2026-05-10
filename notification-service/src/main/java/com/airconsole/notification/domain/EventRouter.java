package com.airconsole.notification.domain;

import com.airconsole.common.messaging.EventEnvelope;
import com.airconsole.notification.application.SignalRPublisher;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Routes inbound events to the appropriate SignalR group(s).
 *
 * <p>Each event carries a roomId which we use to derive the SignalR group name,
 * then delegate to {@link SignalRPublisher#broadcastToGroup(String, String)}.
 *
 * <p>This class is intentionally stateless — all routing logic lives here.
 * It never makes direct I/O calls; it instructs the {@link SignalRPublisher} to do so.
 */
@Component
public final class EventRouter {

    private static final Logger LOG = LoggerFactory.getLogger(EventRouter.class);

    private final SignalRPublisher signalRPublisher;
    private final GroupMapper groupMapper;

    public EventRouter(SignalRPublisher signalRPublisher, GroupMapper groupMapper) {
        this.signalRPublisher = signalRPublisher;
        this.groupMapper = groupMapper;
    }

    /**
     * Route an inbound event to the correct SignalR group.
     *
     * @param envelope    the deserialized event envelope (roomId used for routing)
     * @param rawJsonPayload the original JSON string of the payload (avoids re-serialization)
     */
    public void route(EventEnvelope<?> envelope, String rawJsonPayload) {
        String roomId = envelope.getRoomId();
        if (roomId == null || roomId.isBlank()) {
            LOG.warn("Event {} has no roomId, discarding: {}",
                    envelope.getEventType(), envelope.getEventId());
            return;
        }

        String groupName = groupMapper.toGroupName(roomId);

        LOG.debug("Routing event {} (type={}) to group {}",
                envelope.getEventId(), envelope.getEventType(), groupName);

        signalRPublisher.broadcastToGroup(groupName, rawJsonPayload);
    }

    /**
     * Route an event to a list of groups (for events that span multiple rooms).
     *
     * @param envelope        the event
     * @param groupNames      SignalR group names
     * @param rawJsonPayload  original JSON payload string
     */
    public void routeToGroups(EventEnvelope<?> envelope, List<String> groupNames, String rawJsonPayload) {
        for (String group : groupNames) {
            signalRPublisher.broadcastToGroup(group, rawJsonPayload);
        }
    }

    /**
     * Broadcast a raw message to all connected clients (system announcements).
     *
     * @param message raw JSON message
     */
    public void broadcastSystemMessage(String message) {
        signalRPublisher.broadcastToAll(message);
    }
}