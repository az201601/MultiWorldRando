package com.multiworldrando.core;

import java.util.Random;

/**
 * A single randomizer feature that modifies the ROM.
 * <p>
 * Each patch receives its own deterministic {@link Random} stream derived from
 * the seed, so a given seed always produces the same ROM.
 */
public interface Patch {

    /** Human-readable name of this feature (for logs). */
    String name();

    /**
     * Applies this patch in place.
     *
     * @param rom    the full ROM image (mutable)
     * @param rng    deterministic random stream for this patch
     * @param spoiler spoiler log to record what was changed
     */
    void apply(byte[] rom, Random rng, SpoilerLog spoiler) throws Exception;
}
