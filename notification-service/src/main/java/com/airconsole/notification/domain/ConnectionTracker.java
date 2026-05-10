package com.airconsole.notification.domain;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Component;

/**
 * Tracks which SignalR connectionId belongs to which playerId.
 * connectionId is assigned by Azure SignalR on handshake.
 * playerId is our internal identifier.
 *
 * <p>Two maps kept in sync for O(1) lookups both directions.
 * All operations are lock-free (ConcurrentHashMap).
 */
@Component
public final class ConnectionTracker {

    private final ConcurrentMap<String, String> connectionIdToPlayerId = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, String> playerIdToConnectionId = new ConcurrentHashMap<>();

    /**
     * Called when a SignalR client connects (via webhook).
     *
     * @param connectionId Azure SignalR assigned connection ID
     * @param playerId     our internal player ID
     * @param roomId       room the player is in (used for group join)
     */
    public void track(String connectionId, String playerId, String roomId) {
        // Remove any existing connectionId for this playerId first (reconnect case)
        String existingConnectionId = playerIdToConnectionId.get(playerId);
        if (existingConnectionId != null) {
            connectionIdToPlayerId.remove(existingConnectionId);
        }
        connectionIdToPlayerId.put(connectionId, playerId);
        playerIdToConnectionId.put(playerId, connectionId);
    }

    /**
     * Called when a SignalR client disconnects (via webhook).
     *
     * @param connectionId the disconnecting connection ID
     */
    public void untrack(String connectionId) {
        String playerId = connectionIdToPlayerId.remove(connectionId);
        if (playerId != null) {
            playerIdToConnectionId.remove(playerId);
        }
    }

    /**
     * Resolve playerId from connectionId.
     */
    public String getPlayerId(String connectionId) {
        return connectionIdToPlayerId.get(connectionId);
    }

    /**
     * Resolve connectionId from playerId.
     */
    public String getConnectionId(String playerId) {
        return playerIdToConnectionId.get(playerId);
    }

    /**
     * True if we are currently tracking this connection.
     */
    public boolean isTracked(String connectionId) {
        return connectionIdToPlayerId.containsKey(connectionId);
    }

    /**
     * True if this player has an active connection.
     */
    public boolean isConnected(String playerId) {
        return playerIdToConnectionId.containsKey(playerId);
    }

    /**
     * Number of active tracked connections.
     */
    public int activeConnectionCount() {
        return connectionIdToPlayerId.size();
    }
}