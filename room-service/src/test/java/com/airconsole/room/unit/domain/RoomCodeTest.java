package com.airconsole.room.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.airconsole.room.domain.RoomCode;
import org.junit.jupiter.api.Test;

class RoomCodeTest {

    @Test
    void generatesValidCode() {
        RoomCode code = RoomCode.generate();
        assertThat(code.value()).hasSize(5);
    }

    @Test
    void ofAcceptsValidCode() {
        RoomCode code = RoomCode.of("ABCDE");
        assertThat(code.value()).isEqualTo("ABCDE");
    }

    @Test
    void ofNormalizesToUpperCase() {
        RoomCode code = RoomCode.of("abcde");
        assertThat(code.value()).isEqualTo("ABCDE");
    }

    @Test
    void ofRejectsInvalidCharacter() {
        assertThatThrownBy(() -> RoomCode.of("ABCDI"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void ofRejectsTooShort() {
        assertThatThrownBy(() -> RoomCode.of("ABCD"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void equalityBasedOnValue() {
        RoomCode a = RoomCode.of("ABCDE");
        RoomCode b = RoomCode.of("ABCDE");
        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }
}
