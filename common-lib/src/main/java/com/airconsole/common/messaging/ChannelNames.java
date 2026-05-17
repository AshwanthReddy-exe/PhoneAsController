package com.airconsole.common.messaging;

/**
 * Centralized naming for Redis Streams and Pub/Sub channels.
 * Prevents typos across services. All channels prefixed with "airconsole.".
 */
public final class ChannelNames {

    private ChannelNames() {
        // utility class
    }

    // --- Redis Streams (critical events, durable) ---
    public static final String STREAM_ROOM = "airconsole.stream.room";
    public static final String STREAM_PLAYER = "airconsole.stream.player";
    public static final String STREAM_GAME = "airconsole.stream.game";
    public static final String STREAM_ALL = "airconsole.stream.*";

    // --- Redis Pub/Sub (real-time broadcasts) ---
    public static final String PUBSUB_GAME_STATE = "airconsole.pubsub.game.state";

    // --- Consumer group names (for Redis Streams consumer groups) ---
    public static final String CG_ROOM_SERVICE = "room-service-cg";
    public static final String CG_PLAYER_SERVICE = "player-service-cg";
    public static final String CG_GAME_SERVICE = "game-service-cg";
    public static final String CG_NOTIFICATION_SERVICE = "notification-service-cg";
    public static final String CG_SCORE_SERVICE = "score-service-cg";

    /**
     * Builds a Pub/Sub channel for game state updates for a specific room.
     * Example: airconsole.pubsub.game.state.X7K9P
     */
    public static String gameStateChannel(String roomCode) {
        return PUBSUB_GAME_STATE + "." + roomCode;
    }
}
