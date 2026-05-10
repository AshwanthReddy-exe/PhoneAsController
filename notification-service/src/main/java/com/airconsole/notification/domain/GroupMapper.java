package com.airconsole.notification.domain;

import org.springframework.stereotype.Component;

/**
 * Maps internal room identifiers to Azure SignalR group names.
 *
 * <p>SignalR groups are identified by name — we use roomCode as the canonical
 * group identifier since it is the human-friendly code players share.
 *
 * <p>Azure SignalR limits group names to 256 chars. roomCode is 5 chars so we are safe.
 * The format is always "room:{roomCode}" for clarity in SignalR traffic logs.
 */
@Component
public final class GroupMapper {

    private static final String ROOM_PREFIX = "room:";
    private static final int MAX_GROUP_NAME_LENGTH = 256;

    /**
     * Convert a roomId (UUID from our DB) into a SignalR group name.
     *
     * @param roomId internal UUID
     * @return SignalR group name, e.g. "room:ABCDE"
     */
    public String toGroupName(String roomId) {
        // roomId may be a UUID or a roomCode depending on context.
        // If it looks like a UUID (36 chars with dashes), we use it directly.
        // If it's shorter, treat it as a roomCode.
        if (roomId == null || roomId.isBlank()) {
            throw new IllegalArgumentException("roomId must not be blank");
        }
        String group = roomId.length() <= 10 ? roomId.toUpperCase() : roomId;
        return ROOM_PREFIX + group;
    }

    /**
     * Extract roomCode from a SignalR group name.
     * Inverse of {@link #toGroupName(String)}.
     *
     * @param groupName SignalR group name, e.g. "room:ABCDE"
     * @return roomCode portion, e.g. "ABCDE"
     */
    public String toRoomCode(String groupName) {
        if (groupName == null || !groupName.startsWith(ROOM_PREFIX)) {
            throw new IllegalArgumentException("Invalid group name: " + groupName);
        }
        String roomCode = groupName.substring(ROOM_PREFIX.length());
        if (roomCode.isEmpty()) {
            throw new IllegalArgumentException("Invalid group name: " + groupName);
        }
        return roomCode;
    }

    /**
     * Build the Redis channel name for a room.
     * Used when subscribing to room-scoped events.
     *
     * @param roomId internal room UUID
     * @return Redis channel name, e.g. "room:abc123-def456..."
     */
    public String toRedisChannel(String roomId) {
        return ROOM_PREFIX + roomId;
    }

    /**
     * Validate that a SignalR group name is within Azure limits.
     */
    public boolean isValidGroupName(String groupName) {
        return groupName != null
                && groupName.length() <= MAX_GROUP_NAME_LENGTH
                && groupName.length() > ROOM_PREFIX.length();
    }
}