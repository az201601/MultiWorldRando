package com.multiworldrando.hmgbc;

import java.util.Random;

import com.multiworldrando.core.Patch;
import com.multiworldrando.core.SpoilerLog;

/**
 * Shuffles what each seed shop slot sells.
 * <p>
 * The buy menu has 9 slots (Turnip, Potato, Tomato, Corn, Eggplant, Peanut,
 * Carrot, Broccoli, Grass seeds). The seed shop keeps one 9-byte item record
 * per slot in two mirrored tables (banks $09/$29); each record carries the
 * item's identity (stock flag, season, growth, description, price and item
 * payload via its desc-list pointer, inventory RAM slot) plus a pointer to the
 * slot's display text. The display texts are 9 fixed 32-byte rows in bank $1B.
 * <p>
 * The shuffle permutes the 9 item-identity field groups across slots and moves
 * the display rows with the same permutation, so every slot consistently shows
 * and sells its new item (name, price tag, description and price charged all
 * travel together). The per-slot display-entry pointers stay put - they are
 * position-linked.
 */
public final class SeedShopShufflePatch implements Patch {

    /** Vanilla slot order for the spoiler log. */
    public static final String[] VANILLA_SLOT_NAMES = {
        "Turnip Seeds", "Potato Seeds", "Tomato Seeds", "Corn Seeds",
        "Eggplant Seeds", "Peanut Seeds", "Carrot Seeds", "Broccoli Seeds",
        "Grass Seeds",
    };

    /** Vanilla prices in slot order (from the menu rows). */
    public static final int[] VANILLA_SLOT_PRICES = {
        120, 150, 200, 300, 150, 200, 300, 500, 500,
    };

    /** Byte spans of the item-identity fields inside a 9-byte record. */
    private static final int IDENTITY_SPANS[][] = {
        { 0, 4 }, // flag, season, growth
        { 6, 3 }, // desc-list ptr, inventory RAM slot
    };

    /** Byte span of the position-linked display entry pointer. */
    private static final int DISPLAY_PTR_OFFSET = 4;

    @Override
    public String name() {
        return "Seed shop slots (shuffled)";
    }

    @Override
    public void apply(byte[] rom, Random rng, SpoilerLog spoiler) throws Exception {
        int n = HarvestMoonGbcRom.SEED_SHOP_SLOT_COUNT;
        int recSize = HarvestMoonGbcRom.SEED_SHOP_RECORD_SIZE;

        // ---- validate the tables we are about to permute ----
        for (int table : HarvestMoonGbcRom.SEED_SHOP_TABLE_OFFSETS) {
            for (int i = 0; i < HarvestMoonGbcRom.SEED_SHOP_RECORD0_SIGNATURE.length; i++) {
                if (rom[table + i] != HarvestMoonGbcRom.SEED_SHOP_RECORD0_SIGNATURE[i]) {
                    throw new IllegalStateException(String.format(
                            "Unexpected seed shop table at ROM 0x%06X - refusing to patch.",
                            table));
                }
            }
            // Both copies must be identical.
            int other = HarvestMoonGbcRom.SEED_SHOP_TABLE_OFFSETS[0];
            for (int i = 0; i < n * recSize; i++) {
                if (rom[table + i] != rom[other + i]) {
                    throw new IllegalStateException(
                            "Seed shop table copies differ - refusing to patch.");
                }
            }
        }

        int rowsOff = HarvestMoonGbcRom.SEED_SHOP_ROWS_OFFSET;
        int rowSize = HarvestMoonGbcRom.SEED_SHOP_ROW_SIZE;

        // ---- build a permutation of slot indices (Fisher-Yates) ----
        int[] perm = new int[n];
        for (int i = 0; i < n; i++) {
            perm[i] = i;
        }
        for (int i = n - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            int t = perm[i];
            perm[i] = perm[j];
            perm[j] = t;
        }

        // ---- snapshot the original records and rows ----
        int table = HarvestMoonGbcRom.SEED_SHOP_TABLE_OFFSETS[0];
        byte[][] oldRecords = new byte[n][recSize];
        for (int i = 0; i < n; i++) {
            System.arraycopy(rom, table + i * recSize, oldRecords[i], 0, recSize);
        }
        byte[][] oldRows = new byte[n][rowSize];
        for (int i = 0; i < n; i++) {
            System.arraycopy(rom, rowsOff + i * rowSize, oldRows[i], 0, rowSize);
        }

        // ---- permute: slot i receives the item previously in slot perm[i].
        // The display-entry pointer is position-linked and stays with the slot. ----
        byte[][] newRecords = new byte[n][recSize];
        for (int i = 0; i < n; i++) {
            System.arraycopy(oldRecords[i], 0, newRecords[i], 0, recSize);
            for (int[] span : IDENTITY_SPANS) {
                System.arraycopy(oldRecords[perm[i]], span[0], newRecords[i],
                        span[0], span[1]);
            }
            // display pointer untouched (already copied from oldRecords[i])
        }

        byte[][] newRows = new byte[n][rowSize];
        for (int i = 0; i < n; i++) {
            System.arraycopy(oldRows[perm[i]], 0, newRows[i], 0, rowSize);
        }

        // ---- write both table copies and the rows ----
        for (int t : HarvestMoonGbcRom.SEED_SHOP_TABLE_OFFSETS) {
            for (int i = 0; i < n; i++) {
                System.arraycopy(newRecords[i], 0, rom, t + i * recSize, recSize);
            }
        }
        for (int i = 0; i < n; i++) {
            System.arraycopy(newRows[i], 0, rom, rowsOff + i * rowSize, rowSize);
        }

        // ---- spoiler log ----
        for (int i = 0; i < n; i++) {
            String name = rowName(newRows[i]);
            int price = rowPrice(newRows[i]);
            spoiler.addChange("Seed shop slot " + (i + 1), String.format(
                    "%s (%dG) [was %s]", name, Integer.valueOf(price),
                    VANILLA_SLOT_NAMES[i]));
        }
    }

    /** Decodes the item name from a 32-byte menu row (first 16 glyph bytes). */
    static String rowName(byte[] row) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 16; i++) {
            int b = row[i] & 0xFF;
            if (b == 0xEF) {
                sb.append(' ');
            } else if (b <= 0x19) {
                sb.append((char) ('A' + b));
            } else if (b <= 0x33) {
                sb.append((char) ('a' + b - 0x1A));
            } else if (b <= 0x3D) {
                sb.append((char) ('0' + b - 0x34));
            } else if (b == 0x4C) {
                sb.append('.');
            }
        }
        return sb.toString().trim();
    }

    /** Extracts the price in gold from a menu row ("... 1 bag is 120G."). */
    static int rowPrice(byte[] row) {
        int price = -1;
        for (int i = 0; i < row.length; i++) {
            int b = row[i] & 0xFF;
            if (b < 0x34 || b > 0x3D) {
                continue;
            }
            int value = 0;
            int j = i;
            while (j < row.length) {
                int d = row[j] & 0xFF;
                if (d < 0x34 || d > 0x3D) {
                    break;
                }
                value = value * 10 + (d - 0x34);
                j++;
            }
            // Only accept the number that ends in 'G' (the price).
            if (j < row.length && (row[j] & 0xFF) == 0x06) {
                price = value;
            }
            i = j;
        }
        return price;
    }
}
