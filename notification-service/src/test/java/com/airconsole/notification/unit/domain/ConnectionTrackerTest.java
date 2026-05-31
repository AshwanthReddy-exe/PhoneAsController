package com.airconsole.notification.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.airconsole.notification.domain.ConnectionTracker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ConnectionTracker")
class ConnectionTrackerTest {

    private final ConnectionTracker tracker = new ConnectionTracker();

    @Test
    void trackAndGetPlayerId() {
        tracker.track("conn-1", "player-A", "room-X");

        assertThat(tracker.getPlayerId("conn-1")).isEqualTo("player-A");
        assertThat(tracker.getConnectionId("player-A")).isEqualTo("conn-1");
    }

    @Test
    void trackMultipleConnections() {
        tracker.track("conn-1", "player-A", "room-X");
        tracker.track("conn-2", "player-B", "room-X");

        assertThat(tracker.getPlayerId("conn-1")).isEqualTo("player-A");
        assertThat(tracker.getPlayerId("conn-2")).isEqualTo("player-B");
        assertThat(tracker.activeConnectionCount()).isEqualTo(2);
    }

    @Test
    void reconnectUpdatesConnection() {
        tracker.track("conn-1", "player-A", "room-X");
        assertThat(tracker.getConnectionId("player-A")).isEqualTo("conn-1");

        // Simulate reconnect with new connectionId
        tracker.track("conn-2", "player-A", "room-X");

        assertThat(tracker.getPlayerId("conn-1")).isNull(); // old one removed
        assertThat(tracker.getPlayerId("conn-2")).isEqualTo("player-A");
        assertThat(tracker.getConnectionId("player-A")).isEqualTo("conn-2");
        assertThat(tracker.activeConnectionCount()).isEqualTo(1);
    }

    @Test
    void untrackRemovesBothDirections() {
        tracker.track("conn-1", "player-A", "room-X");
        tracker.untrack("conn-1");

        assertThat(tracker.getPlayerId("conn-1")).isNull();
        assertThat(tracker.getConnectionId("player-A")).isNull();
        assertThat(tracker.activeConnectionCount()).isEqualTo(0);
    }

    @Test
    void untrackUnknownConnectionIsSafe() {
        tracker.track("conn-1", "player-A", "room-X");
        tracker.untrack("unknown-connection"); // no-op, doesn't throw

        assertThat(tracker.getPlayerId("conn-1")).isEqualTo("player-A");
        assertThat(tracker.activeConnectionCount()).isEqualTo(1);
    }

    @Test
    void isTrackedAndIsConnected() {
        tracker.track("conn-1", "player-A", "room-X");

        assertThat(tracker.isTracked("conn-1")).isTrue();
        assertThat(tracker.isTracked("unknown")).isFalse();
        assertThat(tracker.isConnected("player-A")).isTrue();
        assertThat(tracker.isConnected("unknown")).isFalse();
    }
}