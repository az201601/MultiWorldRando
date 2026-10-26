package com.multiworldrando.hmgbc;

import java.util.Random;

import com.multiworldrando.core.Patch;
import com.multiworldrando.core.SpoilerLog;

/**
 * Randomizes the player's starting gold between 0G and 500G (inclusive).
 * <p>
 * Vanilla Harvest Moon GB starts every new game at 500G, written as literal
 * immediates by the new-game initializer (banks $1D/$3D). This patch replaces
 * those immediates with the randomized value - a pure data patch, no code
 * changes.
 */
public final class StartingGoldPatch implements Patch {

    public static final int MIN_GOLD = 0;
    public static final int MAX_GOLD = 500;

    /** Vanilla starting gold for the spoiler log. */
    public static final int VANILLA_GOLD = 500;

    @Override
    public String name() {
        return "Starting gold (0-500G)";
    }

    @Override
    public void apply(byte[] rom, Random rng, SpoilerLog spoiler) throws Exception {
        // Sanity: make sure we are patching the code we reverse-engineered.
        for (int[] site : HarvestMoonGbcRom.STARTING_GOLD_IMMEDIATES) {
            int base = site[0] - 1; // include the 'ld a,' opcode before the immediate
            for (int i = 0; i < HarvestMoonGbcRom.INIT_SIGNATURE.length; i++) {
                if (rom[base + i] != HarvestMoonGbcRom.INIT_SIGNATURE[i]) {
                    throw new IllegalStateException(String.format(
                            "Unexpected code at ROM 0x%06X - refusing to patch.",
                            base + i));
                }
            }
        }

        int gold = MIN_GOLD + rng.nextInt(MAX_GOLD - MIN_GOLD + 1);
        int lo = gold & 0xFF;
        int mid = (gold >> 8) & 0xFF;

        for (int[] site : HarvestMoonGbcRom.STARTING_GOLD_IMMEDIATES) {
            rom[site[0]] = (byte) lo;   // immediate of 'ld a, lo'
            rom[site[1]] = (byte) mid;  // immediate of 'ld a, mid'
            // 'xor a' keeps the high byte at 0 - always correct for 0-500.
        }

        spoiler.addChange("Starting gold", gold + "G (vanilla: " + VANILLA_GOLD + "G)");
    }
}
