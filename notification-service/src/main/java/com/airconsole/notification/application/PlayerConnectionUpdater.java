package com.airconsole.notification.application;

import com.airconsole.notification.domain.ConnectionTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Calls player-service to update connection status on SignalR connect/disconnect.
 *
 * <p>This is the bridge between SignalR's webhook events and our player session state.
 * We use {@code PATCH /api/players/{id}/connection} per the API contract.
 */
@Component
public class PlayerConnectionUpdater {

    private static final Logger LOG = LoggerFactory.getLogger(PlayerConnectionUpdater.class);

    private final RestClient restClient;
    private final ConnectionTracker connectionTracker;

    public PlayerConnectionUpdater(
            @Value("${player-service.url}") String playerServiceUrl,
            ConnectionTracker connectionTracker,
            RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder
                .baseUrl(playerServiceUrl)
                .build();
        this.connectionTracker = connectionTracker;
    }

    /**
     * Called when SignalR fires a connect webhook.
     * Looks up the playerId from our connection tracker and notifies player-service.
     */
    public void onConnected(String connectionId, String roomId) {
        String playerId = connectionTracker.getPlayerId(connectionId);
        if (playerId == null) {
            LOG.debug("Connection {} has no tracked playerId, skipping player-service update", connectionId);
            return;
        }

        LOG.info("Player {} connected (connectionId={})", playerId, connectionId);
        try {
            restClient.patch()
                    .uri("/api/players/{id}/connection", playerId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new ConnectionUpdatePayload(connectionId, roomId, "CONNECTED"))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            LOG.error("Failed to notify player-service of connect for player {}: {}",
                    playerId, e.getMessage());
        }
    }

    /**
     * Called when SignalR fires a disconnect webhook.
     * Notifies player-service that the connection is gone.
     */
    public void onDisconnected(String connectionId) {
        String playerId = connectionTracker.getPlayerId(connectionId);
        if (playerId == null) {
            LOG.debug("Connection {} not tracked, skipping disconnect notification", connectionId);
            return;
        }

        LOG.info("Player {} disconnected (connectionId={})", playerId, connectionId);
        try {
            restClient.patch()
                    .uri("/api/players/{id}/connection", playerId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new ConnectionUpdatePayload(connectionId, null, "DISCONNECTED"))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            LOG.error("Failed to notify player-service of disconnect for player {}: {}",
                    playerId, e.getMessage());
        }
    }

    /** Payload for PATCH /api/players/{id}/connection */
    private record ConnectionUpdatePayload(
            String connectionId,
            String roomId,
            String status
    ) { }
}