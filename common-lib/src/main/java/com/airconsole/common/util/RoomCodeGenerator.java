package com.airconsole.common.util;

import java.security.SecureRandom;

/**
 * Crockford Base32 code generator for room codes.
 * 5 chars = 32^5 = 33,554,432 combinations.
 * Excludes I, L, O, U to avoid visual confusion.
 * Alphabet: 0123456789ABCDEFGHJKMNPQRSTVWXYZ
 */
public final class RoomCodeGenerator {

    private static final String ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ";
    private static final int CODE_LENGTH = 5;
    private static final SecureRandom RANDOM = new SecureRandom();

    private RoomCodeGenerator() {
        // utility class
    }

    /**
     * Generates a random 5-character Crockford Base32 room code.
     */
    public static String generate() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }

    /**
     * Validates a room code format.
     */
    public static boolean isValid(String code) {
        if (code == null || code.length() != CODE_LENGTH) {
            return false;
        }
        for (char c : code.toUpperCase().toCharArray()) {
            if (ALPHABET.indexOf(c) < 0) {
                return false;
            }
        }
        return true;
    }
}
