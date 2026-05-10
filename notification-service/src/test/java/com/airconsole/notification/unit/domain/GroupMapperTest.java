package com.airconsole.notification.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.airconsole.notification.domain.GroupMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("GroupMapper")
class GroupMapperTest {

    private final GroupMapper mapper = new GroupMapper();

    @Test
    void toGroupNameWithRoomCode() {
        assertThat(mapper.toGroupName("ABCDE")).isEqualTo("room:ABCDE");
        assertThat(mapper.toGroupName("abcde")).isEqualTo("room:ABCDE"); // upper-cased
    }

    @Test
    void toGroupNameWithUUID() {
        String uuid = "a1b2c3d4-e5f6-7890-abcd-ef1234567890";
        assertThat(mapper.toGroupName(uuid)).isEqualTo("room:" + uuid);
    }

    @Test
    void toGroupNameThrowsOnNull() {
        assertThatThrownBy(() -> mapper.toGroupName(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> mapper.toGroupName(""))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> mapper.toGroupName("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void toRoomCode() {
        assertThat(mapper.toRoomCode("room:ABCDE")).isEqualTo("ABCDE");
    }

    @Test
    void toRoomCodeThrowsOnInvalid() {
        assertThatThrownBy(() -> mapper.toRoomCode(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> mapper.toRoomCode("invalid"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> mapper.toRoomCode("room:"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"room:ABCDE", "room:a1b2c3d4-e5f6-7890-abcd-ef1234567890"})
    void isValidGroupNameValid(String groupName) {
        assertThat(mapper.isValidGroupName(groupName)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"room:", "short", "x"})
    void isValidGroupNameInvalid(String groupName) {
        assertThat(mapper.isValidGroupName(groupName)).isFalse();
    }

    @Test
    void toRedisChannel() {
        assertThat(mapper.toRedisChannel("abc123"))
                .isEqualTo("room:abc123");
    }
}