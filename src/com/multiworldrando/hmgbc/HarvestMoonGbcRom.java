package com.multiworldrando.hmgbc;

/**
 * ROM map for Harvest Moon GB (USA) [C][!].
 * <p>
 * Offsets are file offsets into the 1 MiB ROM image. Reverse-engineering notes:
 * <ul>
 * <li>Money is a 3-byte little-endian value in battery SRAM at $B8EF-$B8F1
 *     (SRAM offsets 0x18EF-0x18F1), capped at 99,999G.</li>
 * <li>The new-game initializer lives at CPU $52B9 in banks $1D and $3D
 *     (mirrored code). It sets a couple of starting variables and writes the
 *     vanilla starting gold of 500G ($01F4) with immediate operands:
 *     <pre>
 *     ld a, $f4 / ld [$b8ef], a   ; money lo
 *     ld a, $01 / ld [$b8f0], a   ; money mid
 *     xor a    / ld [$b8f1], a   ; money hi
 *     call Call_000_1056          ; money validation
 *     </pre>
 *     Traced live in an emulator: the routine fires once, when the player
 *     confirms a new game.</li>
 * </ul>
 * Patching the two immediate bytes in each mirror changes the starting gold
 * without touching code.
 */
public final class HarvestMoonGbcRom {

    /** ROM size in bytes. */
    public static final int SIZE = 0x100000;

    /**
     * Immediate operand offsets of the starting-gold writes, one pair per
     * mirrored copy: {money-lo immediate, money-mid immediate}.
     * The 24 bytes before each call site must equal {@link #INIT_SIGNATURE}.
     */
    public static final int[][] STARTING_GOLD_IMMEDIATES = {
        { 0x0752C4, 0x0752C9 }, // bank $1D
        { 0x0F527F, 0x0F5284 }, // bank $3D
    };

    /**
     * Expected bytes around the first starting-gold write in each mirror:
     * ld a,$f4 / ld [$b8ef],a / ld a,$01 / ld [$b8f0],a / xor a / ld [$b8f1],a.
     */
    public static final byte[] INIT_SIGNATURE = {
        (byte) 0x3E, (byte) 0xF4,
        (byte) 0xEA, (byte) 0xEF, (byte) 0xB8,
        (byte) 0x3E, (byte) 0x01,
        (byte) 0xEA, (byte) 0xF0, (byte) 0xB8,
        (byte) 0xAF,
        (byte) 0xEA, (byte) 0xF1, (byte) 0xB8
    };

    /** Global ROM checksum (not verified by the game, kept correct anyway). */
    public static final int GLOBAL_CHECKSUM_HI = 0x14E;

    private HarvestMoonGbcRom() {
    }
}
