package com.airconsole.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

class RoomCodeGeneratorTest {

    @Test
    void generatesCorrectLength() {
        String code = RoomCodeGenerator.generate();
        assertThat(code).hasSize(5);
    }

    @RepeatedTest(100)
    void generatesOnlyValidChars() {
        String code = RoomCodeGenerator.generate();
        assertThat(code).matches("^[A-Z0-9]{5}$");
        assertThat(code).doesNotContain("I", "L", "O", "U");
    }

    @Test
    void validatesCorrectCode() {
        assertThat(RoomCodeGenerator.isValid("ABCDE")).isTrue();
        assertThat(RoomCodeGenerator.isValid("12345")).isTrue();
    }

    @Test
    void rejectsInvalidCodes() {
        assertThat(RoomCodeGenerator.isValid(null)).isFalse();
        assertThat(RoomCodeGenerator.isValid("ABCD")).isFalse();      // too short
        assertThat(RoomCodeGenerator.isValid("ABCDEF")).isFalse();     // too long
        assertThat(RoomCodeGenerator.isValid("ABCDI")).isFalse();      // I char
        assertThat(RoomCodeGenerator.isValid("ABCDL")).isFalse();      // L char
    }

    @RepeatedTest(50)
    void codesShouldBeMostlyUnique() {
        String code1 = RoomCodeGenerator.generate();
        String code2 = RoomCodeGenerator.generate();
        assertThat(code1).isNotEqualTo(code2);
    }
}
