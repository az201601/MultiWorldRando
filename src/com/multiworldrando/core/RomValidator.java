package com.multiworldrando.core;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Verifies that the ROM the user picked is a supported game before any
 * patching happens. Currently: Harvest Moon GB (USA) (CGB).
 */
public final class RomValidator {

    /** Harvest Moon GB (USA) [C][!] - the supported target ROM. */
    public static final String HMGBC_MD5 = "498c0a50a5e5cde16127617a97ad6162";

    private static final Set<String> SUPPORTED_MD5S;

    static {
        Set<String> s = new HashSet<String>();
        s.add(HMGBC_MD5);
        SUPPORTED_MD5S = Collections.unmodifiableSet(s);
    }

    private RomValidator() {
    }

    /**
     * Validates a ROM image.
     *
     * @throws RomValidationException if the file is not a supported ROM,
     *         with a message explaining what is wrong.
     */
    public static void validate(byte[] rom) throws RomValidationException {
        if (rom == null || rom.length < 0x8000) {
            throw new RomValidationException(
                    "File is too small to be a Game Boy ROM (" +
                    (rom == null ? 0 : rom.length) + " bytes).");
        }

        // Sanity-check the cartridge header first: cheapest way to give a
        // useful error ("this is not a Game Boy ROM at all").
        if (rom.length < 0x150 || rom[0x104] != (byte) 0xCE) {
            throw new RomValidationException(
                    "File does not have a valid Game Boy cartridge header.");
        }

        String title = readTitle(rom);
        String md5 = md5Hex(rom);

        if (!SUPPORTED_MD5S.contains(md5)) {
            StringBuilder sb = new StringBuilder();
            sb.append("This is not the supported Harvest Moon GB ROM.\n");
            sb.append("  Detected title: ").append(title).append('\n');
            sb.append("  Detected MD5:   ").append(md5).append('\n');
            sb.append("  Expected MD5:   ").append(HMGBC_MD5)
              .append(" (Harvest Moon GB (USA) [C][!])");
            throw new RomValidationException(sb.toString());
        }
    }

    /** Reads the cartridge title from the ROM header. */
    public static String readTitle(byte[] rom) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0x134; i < 0x144; i++) {
            int b = rom[i] & 0xFF;
            if (b == 0) {
                break;
            }
            sb.append((char) b);
        }
        return sb.toString().trim();
    }

    /** Computes the MD5 of the ROM as a lowercase hex string. */
    public static String md5Hex(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(data);
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 unavailable", e);
        }
    }
}
