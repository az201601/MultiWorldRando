package com.multiworldrando.core;

import java.security.SecureRandom;

/**
 * Generates and parses randomizer seeds. By default seeds are produced
 * automatically when the user clicks Generate; the user may instead type a
 * specific seed to reproduce a generation.
 */
public final class SeedGenerator {

    private static final SecureRandom RNG = new SecureRandom();

    private SeedGenerator() {
    }

    /** Returns a fresh auto-generated 64-bit seed. */
    public static long nextSeed() {
        return RNG.nextLong();
    }

    /**
     * Parses a user-entered seed: 1-16 hexadecimal digits, optionally
     * prefixed with {@code 0x} (case-insensitive). Any 64-bit seed value
     * can be written this way.
     *
     * @throws IllegalArgumentException if the text is not a valid seed
     */
    public static long parseSeed(String text) {
        String s = text.trim();
        if (s.startsWith("0x") || s.startsWith("0X")) {
            s = s.substring(2);
        }
        if (s.isEmpty() || s.length() > 16) {
            throw new IllegalArgumentException(
                    "a seed is 1-16 hexadecimal digits (0-9, A-F)");
        }
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            boolean hex = (c >= '0' && c <= '9')
                    || (c >= 'a' && c <= 'f')
                    || (c >= 'A' && c <= 'F');
            if (!hex) {
                throw new IllegalArgumentException(
                        "a seed is 1-16 hexadecimal digits (0-9, A-F)");
            }
        }
        return Long.parseUnsignedLong(s, 16);
    }

    /** Formats a seed as a fixed-width uppercase hexadecimal string. */
    public static String format(long seed) {
        return String.format("%016X", seed);
    }
}
