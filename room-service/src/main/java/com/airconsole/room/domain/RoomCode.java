package com.airconsole.room.domain;

import com.airconsole.common.util.RoomCodeGenerator;

/**
 * Value object — room code string with validation.
 * Ensures code always valid Crockford Base32.
 */
public final class RoomCode {

    private final String value;

    private RoomCode(String value) {
        if (!RoomCodeGenerator.isValid(value)) {
            throw new IllegalArgumentException("Invalid room code: " + value);
        }
        this.value = value.toUpperCase();
    }

    public static RoomCode generate() {
        return new RoomCode(RoomCodeGenerator.generate());
    }

    public static RoomCode of(String raw) {
        return new RoomCode(raw);
    }

    public String value() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RoomCode roomCode = (RoomCode) o;
        return value.equals(roomCode.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
