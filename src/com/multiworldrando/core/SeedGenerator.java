package com.multiworldrando.core;

import java.security.SecureRandom;

/**
 * Generates randomizer seeds. Seeds are produced automatically when the user
 * clicks Generate - there is deliberately no user-facing "generate seed" step.
 */
public final class SeedGenerator {

    private static final SecureRandom RNG = new SecureRandom();

    private SeedGenerator() {
    }

    /** Returns a fresh auto-generated 64-bit seed. */
    public static long nextSeed() {
        return RNG.nextLong();
    }

    /** Formats a seed as a fixed-width uppercase hexadecimal string. */
    public static String format(long seed) {
        return String.format("%016X", seed);
    }
}
