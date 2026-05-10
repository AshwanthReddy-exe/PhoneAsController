package com.airconsole.notification.infrastructure.websocket;

import com.airconsole.notification.application.SignalRPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class StompWebSocketPublisher implements SignalRPublisher {

    private static final Logger LOG = LoggerFactory.getLogger(StompWebSocketPublisher.class);

    private final SimpMessagingTemplate messagingTemplate;

    public StompWebSocketPublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void broadcastToGroup(String groupName, String message) {
        // e.g., groupName = "room:ABCDE" -> topic = "/topic/room:ABCDE"
        String topic = "/topic/" + groupName;
        messagingTemplate.convertAndSend(topic, message);
        LOG.debug("Broadcasted to {}: {}", topic, truncate(message));
    }

    @Override
    public void sendToConnection(String connectionId, String message) {
        String topic = "/topic/connection/" + connectionId;
        messagingTemplate.convertAndSend(topic, message);
        LOG.debug("Sent to connection {}: {}", connectionId, truncate(message));
    }

    @Override
    public void broadcastToAll(String message) {
        messagingTemplate.convertAndSend("/topic/all", message);
        LOG.debug("Broadcasted to all: {}", truncate(message));
    }

    @Override
    public void addConnectionToGroup(String connectionId, String groupName) {
        // STOMP clients subscribe to topics directly, so the server doesn't need to manually map them.
        LOG.debug("STOMP: addConnectionToGroup ignored. Clients subscribe directly.");
    }

    @Override
    public void removeConnectionFromGroup(String connectionId, String groupName) {
        // STOMP clients unsubscribe from topics directly.
        LOG.debug("STOMP: removeConnectionFromGroup ignored. Clients unsubscribe directly.");
    }

    private String truncate(String s) {
        if (s == null) {
            return "null";
        }
        return s.length() > 100 ? s.substring(0, 100) + "..." : s;
    }
}
