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

    /**
     * Grandpa's rainy day money drawer: interacting with object $09 runs a
     * one-time grant of 1000G (flag $B8FF in SRAM, message $09 first time /
     * $0a afterwards). The grant amount is the 16-bit immediate of
     * {@code ld bc, $03e8} right after {@code ld hl, $b8ef}:
     * <pre>
     * ld hl, $b8ef
     * ld bc, $03e8      ; 1000G  &lt;-- patched: lo, mid
     * call Call_000_0cbb  ; add bc to money
     * call Call_000_1056  ; money validation
     * </pre>
     * One entry per mirrored copy: {lo-byte offset, mid-byte offset}.
     * The 12 bytes from lo-4 must equal {@link #DRAWER_GOLD_SIGNATURE}.
     */
    public static final int[][] DRAWER_GOLD_IMMEDIATES = {
        { 0x0099D7, 0x0099D8 }, // bank $02
        { 0x089944, 0x089945 }, // bank $22
    };

    /** ld hl,$b8ef / ld bc,$03e8 / call add / call validate. */
    public static final byte[] DRAWER_GOLD_SIGNATURE = {
        (byte) 0x21, (byte) 0xEF, (byte) 0xB8,
        (byte) 0x01, (byte) 0xE8, (byte) 0x03,
        (byte) 0xCD, (byte) 0xBB, (byte) 0x0C,
        (byte) 0xCD, (byte) 0x56, (byte) 0x10
    };

    /**
     * Seed shop: per-slot item table. Each 9-byte record describes what one
     * buy-menu slot sells:
     * <pre>
     *   +0..+1  SRAM flag address (per-item stock/bought flag, $B8D2-$B8DA)
     *   +2      season availability bitmask (01 spring, 02 summer, 04 fall,
     *           08 winter, 07 all year)
     *   +3      growth parameter (per-item)
     *   +4..+5  pointer to the slot's display entry in the buy-menu script
     *           (POSITION-linked: slot i always uses entry i)
     *   +6..+7  pointer to the item's [desc-list + purchase header] pair in
     *           bank $1B (carries the price charged, item granted and the
     *           "Those are ... Seeds" description)
     *   +8      inventory RAM slot offset (Grass $50, crops $54-$70)
     * </pre>
     * Two byte-identical copies (bank $09 and its bank $29 mirror). One
     * record per slot, in menu order (Turnip, Potato, Tomato, Corn, Eggplant,
     * Peanut, Carrot, Broccoli, Grass).
     */
    public static final int[] SEED_SHOP_TABLE_OFFSETS = {
        0x02495D, // bank $09
        0x0A4940, // bank $29 (mirror)
    };

    /** Slot count / record size of the seed shop item table. */
    public static final int SEED_SHOP_SLOT_COUNT = 9;
    public static final int SEED_SHOP_RECORD_SIZE = 9;

    /**
     * Vanilla record 0 (Turnip Seeds), used as a layout signature. Verified
     * against both table copies before patching.
     */
    public static final byte[] SEED_SHOP_RECORD0_SIGNATURE = {
        (byte) 0xD2, (byte) 0xB8, // flag $B8D2
        (byte) 0x01, (byte) 0x1F, // spring, growth $1F
        (byte) 0x19, (byte) 0x77, // display entry ptr $7719
        (byte) 0x26, (byte) 0x6E, // desc-list ptr $6E26
        (byte) 0x54,              // inventory RAM $54
    };

    /**
     * Seed shop buy-menu slot texts (bank $1B): 9 fixed 32-byte rows,
     * two 16-char lines each ("Turnip Seeds" / "1 bag is 120G."). The menu
     * script at CPU $7715 points at these in slot order; shuffling the row
     * contents re-labels what each slot shows.
     */
    public static final int SEED_SHOP_ROWS_OFFSET = 0x06F773;
    public static final int SEED_SHOP_ROW_SIZE = 32;

    /** Global ROM checksum (not verified by the game, kept correct anyway). */
    public static final int GLOBAL_CHECKSUM_HI = 0x14E;

    private HarvestMoonGbcRom() {
    }
}
