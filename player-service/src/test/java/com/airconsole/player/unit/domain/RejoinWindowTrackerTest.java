package com.airconsole.player.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.airconsole.player.domain.RejoinWindowTracker;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class RejoinWindowTrackerTest {

    RejoinWindowTracker tracker = new RejoinWindowTracker();

    @Test
    void withinWindow() {
        Instant recent = Instant.now();
        assertThat(tracker.isWithinWindow(recent)).isTrue();
        assertThat(tracker.isExpired(recent)).isFalse();
    }

    @Test
    void expiredAfter60Seconds() {
        Instant longAgo = Instant.now().minusSeconds(61);
        assertThat(tracker.isWithinWindow(longAgo)).isFalse();
        assertThat(tracker.isExpired(longAgo)).isTrue();
    }

    @Test
    void remainingSecondsDecreases() {
        Instant ago30 = Instant.now().minusSeconds(30);
        long remaining = tracker.remainingSeconds(ago30);
        assertThat(remaining).isLessThanOrEqualTo(30L);
        assertThat(remaining).isGreaterThan(0L);
    }

    @Test
    void expiredReturnsZeroRemaining() {
        Instant longAgo = Instant.now().minusSeconds(90);
        assertThat(tracker.remainingSeconds(longAgo)).isZero();
    }
}
