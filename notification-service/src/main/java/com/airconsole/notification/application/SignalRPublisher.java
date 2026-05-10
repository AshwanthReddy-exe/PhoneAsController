package com.airconsole.notification.application;

/**
 * Port — outbound SignalR communication.
 *
 * <p>Implementations handle the actual transport to Azure SignalR.
 * This interface is kept narrow so the domain never knows about HTTP clients.
 */
public interface SignalRPublisher {

    /**
     * Broadcast a message to all clients connected to a SignalR group.
     *
     * @param groupName SignalR group name (e.g. "room:ABCDE")
     * @param message   JSON payload to send
     */
    void broadcastToGroup(String groupName, String message);

    /**
     * Send a message to a specific connected client.
     *
     * @param connectionId SignalR connection ID
     * @param message      JSON payload to send
     */
    void sendToConnection(String connectionId, String message);

    /**
     * Broadcast a system-wide message to ALL connected clients.
     * Use sparingly (e.g. service announcements).
     *
     * @param message JSON payload
     */
    void broadcastToAll(String message);

    /**
     * Add a connection to a SignalR group (join room).
     *
     * @param connectionId the client connection
     * @param groupName    the group to join
     */
    void addConnectionToGroup(String connectionId, String groupName);

    /**
     * Remove a connection from a SignalR group (leave room).
     *
     * @param connectionId the client connection
     * @param groupName    the group to leave
     */
    void removeConnectionFromGroup(String connectionId, String groupName);
}