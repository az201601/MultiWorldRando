package com.multiworldrando.core;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.multiworldrando.hmgbc.GrandpaDrawerPatch;
import com.multiworldrando.hmgbc.HarvestMoonGbcRom;
import com.multiworldrando.hmgbc.StartingGoldPatch;

/**
 * Orchestrates a randomizer run: validate the ROM, auto-generate a seed,
 * apply the enabled patches, then write the randomized ROM and spoiler log.
 */
public final class Randomizer {

    /** Result of a successful run. */
    public static final class Result {
        public final long seed;
        public final File romOut;
        public final File spoilerOut;

        Result(long seed, File romOut, File spoilerOut) {
            this.seed = seed;
            this.romOut = romOut;
            this.spoilerOut = spoilerOut;
        }
    }

    private Randomizer() {
    }

    /**
     * Randomizes the given ROM according to the settings.
     * The seed is generated automatically.
     */
    public static Result randomize(File romFile, Settings settings, File outputDir)
            throws IOException, RomValidationException {
        return randomize(romFile, settings, outputDir, null);
    }

    /**
     * Randomizes the given ROM according to the settings.
     * If {@code requestedSeed} is null a seed is generated automatically;
     * otherwise the given seed is used, so the run is reproducible.
     */
    public static Result randomize(File romFile, Settings settings, File outputDir,
            Long requestedSeed) throws IOException, RomValidationException {

        byte[] rom = readFile(romFile);
        RomValidator.validate(rom);

        // Auto-generate unless the user supplied a specific seed.
        long seed = requestedSeed != null
                ? requestedSeed.longValue()
                : SeedGenerator.nextSeed();
        String seedStr = SeedGenerator.format(seed);

        SpoilerLog spoiler = new SpoilerLog(seed, romFile.getName(),
                RomValidator.md5Hex(rom));
        spoiler.addSetting("Randomize starting gold (0-500G)",
                settings.isRandomizeStartingGold() ? "yes" : "no");
        spoiler.addSetting("Randomize grandpa's rainy day money drawer (0-1000G)",
                settings.isRandomizeDrawerMoney() ? "yes" : "no");

        List<Patch> patches = new ArrayList<Patch>();
        if (settings.isRandomizeStartingGold()) {
            patches.add(new StartingGoldPatch());
        }
        if (settings.isRandomizeDrawerMoney()) {
            patches.add(new GrandpaDrawerPatch());
        }

        int index = 1;
        for (Patch patch : patches) {
            // Each patch gets its own deterministic stream derived from the
            // seed, so patches stay independent of each other.
            Random rng = new Random(seed ^ (0x9E3779B97F4A7C15L * index));
            try {
                patch.apply(rom, rng, spoiler);
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new IOException(
                        "Patch '" + patch.name() + "' failed: " + e.getMessage(), e);
            }
            index++;
        }

        fixGlobalChecksum(rom);

        String base = romFile.getName();
        int dot = base.lastIndexOf('.');
        if (dot > 0) {
            base = base.substring(0, dot);
        }
        File dir = outputDir != null ? outputDir : romFile.getParentFile();
        File romOut = new File(dir, base + " (Seed " + seedStr + ").gbc");
        File spoilerOut = new File(dir, base + " (Seed " + seedStr + ") Spoiler Log.txt");

        writeFile(romOut, rom);
        spoiler.write(spoilerOut);

        return new Result(seed, romOut, spoilerOut);
    }

    /** Recomputes the (informational) global ROM checksum at 0x14E-0x14F. */
    static void fixGlobalChecksum(byte[] rom) {
        int sum = 0;
        for (int i = 0; i < rom.length; i++) {
            if (i == HarvestMoonGbcRom.GLOBAL_CHECKSUM_HI
                    || i == HarvestMoonGbcRom.GLOBAL_CHECKSUM_HI + 1) {
                continue;
            }
            sum = (sum + (rom[i] & 0xFF)) & 0xFFFF;
        }
        rom[HarvestMoonGbcRom.GLOBAL_CHECKSUM_HI] = (byte) ((sum >> 8) & 0xFF);
        rom[HarvestMoonGbcRom.GLOBAL_CHECKSUM_HI + 1] = (byte) (sum & 0xFF);
    }

    private static byte[] readFile(File f) throws IOException {
        InputStream in = new FileInputStream(f);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[65536];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        } finally {
            in.close();
        }
    }

    private static void writeFile(File f, byte[] data) throws IOException {
        FileOutputStream out = new FileOutputStream(f);
        try {
            out.write(data);
        } finally {
            out.close();
        }
    }
}
