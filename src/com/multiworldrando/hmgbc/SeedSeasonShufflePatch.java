package com.multiworldrando.hmgbc;

import java.util.Random;

import com.multiworldrando.core.Patch;
import com.multiworldrando.core.SpoilerLog;

/**
 * Shuffles which season each crop seed is sold in.
 * <p>
 * The seed shop item table (see {@link HarvestMoonGbcRom#SEED_SHOP_TABLE_OFFSETS})
 * gives every seed a season-availability bitmask at record byte +2: 01 spring,
 * 02 summer, 04 fall, 08 winter. The shop shows the seeds whose mask matches
 * the current season, so vanilla sells {Turnip, Potato} in spring,
 * {Tomato, Corn} in summer, {Eggplant, Peanut} in fall and {Carrot, Broccoli}
 * in winter. Grass Seeds keeps 07 (spring|summer|fall) and is left alone -
 * it still takes part in the slot shuffle.
 * <p>
 * This patch permutes the 8 crop masks among the 8 crop records (two crops per
 * season, as vanilla - the shop shelf is only ever asked to show up to three
 * bags), leaving Grass Seeds' 07 untouched. It works standalone or combined
 * with the slot shuffle. Tomato Seeds' description ("...during Summer,") is
 * rewritten to its new season - the only season word in any seed description.
 */
public final class SeedSeasonShufflePatch implements Patch {

    /** Crop seeds in table order, for the spoiler log. */
    public static final String[] CROP_NAMES = {
        "Turnip Seeds", "Potato Seeds", "Tomato Seeds", "Corn Seeds",
        "Eggplant Seeds", "Peanut Seeds", "Carrot Seeds", "Broccoli Seeds",
    };

    /** Vanilla crops per season, in table order. */
    public static final String[][] VANILLA_SEASON_CROPS = {
        { "Turnip Seeds", "Potato Seeds" },     // spring
        { "Tomato Seeds", "Corn Seeds" },       // summer
        { "Eggplant Seeds", "Peanut Seeds" },   // fall
        { "Carrot Seeds", "Broccoli Seeds" },   // winter
    };

    /** desc-list pointer of Tomato Seeds' purchase data (identifies its record). */
    private static final int TOMATO_DESC_PTR = 0x6ED0;

    /** desc-list pointer of Grass Seeds' purchase data (identifies its record). */
    private static final int GRASS_DESC_PTR = 0x70DA;

    @Override
    public String name() {
        return "Seed shop crop seasons (shuffled)";
    }

    @Override
    public void apply(byte[] rom, Random rng, SpoilerLog spoiler) throws Exception {
        int n = HarvestMoonGbcRom.SEED_SHOP_SLOT_COUNT;
        int recSize = HarvestMoonGbcRom.SEED_SHOP_RECORD_SIZE;
        int table = HarvestMoonGbcRom.SEED_SHOP_TABLE_OFFSETS[0];
        int seaOff = HarvestMoonGbcRom.SEASON_BYTE_OFFSET;

        // ---- validate: both copies identical, season masks form the vanilla
        // multiset {01,01,02,02,04,04,08,08} plus one grass 07 ----
        for (int t : HarvestMoonGbcRom.SEED_SHOP_TABLE_OFFSETS) {
            for (int i = 0; i < n * recSize; i++) {
                if (rom[t + i] != rom[table + i]) {
                    throw new IllegalStateException(
                            "Seed shop table copies differ - refusing to patch.");
                }
            }
        }
        int grassSlot = -1;
        int[] counts = new int[16];
        for (int i = 0; i < n; i++) {
            int mask = rom[table + i * recSize + seaOff] & 0xFF;
            if (mask == HarvestMoonGbcRom.GRASS_SEASON_MASK) {
                if (grassSlot >= 0) {
                    throw new IllegalStateException(
                            "Multiple all-season seed records - refusing to patch.");
                }
                grassSlot = i;
            } else {
                counts[mask & 0x0F]++; // masks are single bits 01/02/04/08
            }
        }
        if (grassSlot < 0
                || counts[0x1] != 2 || counts[0x2] != 2
                || counts[0x4] != 2 || counts[0x8] != 2
                || counts[0x0] + counts[0x3] + counts[0x5] + counts[0x6]
                   + counts[0x7] + counts[0x9] + counts[0xA] + counts[0xB]
                   + counts[0xC] + counts[0xD] + counts[0xE] + counts[0xF] != 0) {
            throw new IllegalStateException(
                    "Unexpected seed season masks - refusing to patch.");
        }
        // The 8 crop records must carry the vanilla purchase-data pointers.
        int[] wantDescs = { 0x6E26, 0x6E7B, 0x6ED0, 0x6F25, 0x6F7A, 0x6FCF, 0x7024, 0x7079 };
        boolean[] seen = new boolean[8];
        for (int i = 0; i < n; i++) {
            if (i == grassSlot) {
                continue;
            }
            int desc = rom[table + i * recSize + 6] & 0xFF
                    | (rom[table + i * recSize + 7] & 0xFF) << 8;
            boolean found = false;
            for (int w = 0; w < wantDescs.length; w++) {
                if (!seen[w] && wantDescs[w] == desc) {
                    seen[w] = true;
                    found = true;
                    break;
                }
            }
            if (!found) {
                throw new IllegalStateException(String.format(
                        "Unexpected crop record (desc 0x%04X) - refusing to patch.", desc));
            }
        }
        // Grass must really be Grass (its identity travels as one group).
        int grassBase = table + grassSlot * recSize;
        if ((rom[grassBase + 6] & 0xFF | (rom[grassBase + 7] & 0xFF) << 8)
                != GRASS_DESC_PTR) {
            throw new IllegalStateException(
                    "All-season record is not Grass Seeds - refusing to patch.");
        }

        // ---- permute the 8 crop masks (Fisher-Yates) ----
        int[] masks = new int[8];
        int k = 0;
        for (int i = 0; i < n; i++) {
            if (i != grassSlot) {
                masks[k++] = rom[table + i * recSize + seaOff] & 0xFF;
            }
        }
        for (int i = 7; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            int t = masks[i];
            masks[i] = masks[j];
            masks[j] = t;
        }
        k = 0;
        for (int i = 0; i < n; i++) {
            if (i != grassSlot) {
                rom[table + i * recSize + seaOff] = (byte) masks[k];
                // keep the bank $29 mirror in sync
                int m = HarvestMoonGbcRom.SEED_SHOP_TABLE_OFFSETS[1];
                rom[m + i * recSize + seaOff] = (byte) masks[k];
                k++;
            }
        }

        // ---- find each crop's assigned season for the spoiler and Tomato text ----
        String[][] seasonCrops = new String[4][2];
        int[] seasonFill = new int[4];
        int tomatoSeasonMask = 0x02; // vanilla; updated below
        for (int i = 0; i < n; i++) {
            if (i == grassSlot) {
                continue;
            }
            int mask = rom[table + i * recSize + seaOff] & 0xFF;
            int desc = rom[table + i * recSize + 6] & 0xFF
                    | (rom[table + i * recSize + 7] & 0xFF) << 8;
            String name = cropNameForDesc(desc);
            int season = seasonIndexOf(mask);
            if (seasonFill[season] < 2) {
                seasonCrops[season][seasonFill[season]] = name;
                seasonFill[season]++;
            }
            if (desc == TOMATO_DESC_PTR) {
                tomatoSeasonMask = mask;
            }
        }

        // ---- rewrite Tomato's "during Summer," to its new season ----
        String seasonName = HarvestMoonGbcRom.SEASON_NAMES[seasonIndexOf(tomatoSeasonMask)];
        if (!"Summer".equals(seasonName)) {
            byte[] replacement = encodeSeasonWord(seasonName);
            for (int off : HarvestMoonGbcRom.TOMATO_SUMMER_TEXT_OFFSETS) {
                for (int i = 0; i < HarvestMoonGbcRom.TOMATO_SUMMER_TEXT.length; i++) {
                    if (rom[off + i] != HarvestMoonGbcRom.TOMATO_SUMMER_TEXT[i]) {
                        throw new IllegalStateException(String.format(
                                "Unexpected Tomato description at ROM 0x%06X - refusing to patch.",
                                off));
                    }
                }
                System.arraycopy(replacement, 0, rom, off, replacement.length);
            }
            spoiler.addChange("Tomato Seeds description",
                    "\"during " + seasonName + ",\" [was \"during Summer,\"]");
        }

        // ---- spoiler log: what each season sells now ----
        for (int s = 0; s < 4; s++) {
            String grass = s == 3 ? "" : " (+ Grass Seeds)";
            spoiler.addChange(
                    HarvestMoonGbcRom.SEASON_NAMES[s] + " sells",
                    seasonCrops[s][0] + ", " + seasonCrops[s][1] + grass
                    + " [was " + VANILLA_SEASON_CROPS[s][0] + ", "
                    + VANILLA_SEASON_CROPS[s][1] + grass + "]");
        }
    }

    private static int seasonIndexOf(int mask) {
        switch (mask) {
            case 0x01: return 0;
            case 0x02: return 1;
            case 0x04: return 2;
            default:   return 3;
        }
    }

    private static String cropNameForDesc(int desc) {
        int[] descs = {
            0x6E26, 0x6E7B, 0x6ED0, 0x6F25,
            0x6F7A, 0x6FCF, 0x7024, 0x7079,
        };
        for (int i = 0; i < descs.length; i++) {
            if (descs[i] == desc) {
                return CROP_NAMES[i];
            }
        }
        return "Unknown Seeds";
    }

    /** Encodes "&lt;Season&gt;," into the 7-glyph span, padded with spaces. */
    private static byte[] encodeSeasonWord(String season) {
        byte[] out = new byte[HarvestMoonGbcRom.TOMATO_SUMMER_TEXT.length];
        int i = 0;
        for (int c = 0; c < season.length(); c++) {
            char ch = season.charAt(c);
            if (ch >= 'A' && ch <= 'Z') {
                out[i++] = (byte) (ch - 'A');        // A-Z = 0x00-0x19
            } else {
                out[i++] = (byte) (ch - 'a' + 0x1A); // a-z = 0x1A-0x33
            }
        }
        out[i++] = (byte) 0x8E; // ','
        while (i < out.length) {
            out[i++] = (byte) 0xEF; // space
        }
        return out;
    }
}
