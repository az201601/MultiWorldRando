package com.multiworldrando.hmgbc;

import java.util.Random;

import com.multiworldrando.core.Patch;
import com.multiworldrando.core.SpoilerLog;

/**
 * Randomizes the money found in grandpa's rainy day money drawer
 * (the one-time object-$09 grant) between 0G and 1000G (inclusive),
 * and bakes a matching flavor message into the drawer dialog
 * (message $09) at generation time.
 * <p>
 * Vanilla Harvest Moon GB grants 1000G the first time you check the drawer,
 * written as the {@code ld bc, $03e8} immediate in the grant handler
 * (mirrored in ROM banks $02 and $22). This patch replaces those immediates
 * with the randomized amount - a pure data patch, no code changes.
 * <p>
 * Message tiers (user-approved):
 * <ul>
 *   <li>0G: "Empty! Grandpa's rainy day fund was all sunshine."</li>
 *   <li>1-499G: "Rainy day fund? More like a light drizzle." followed by
 *       "Received XG!!" showing the actual rolled amount.</li>
 *   <li>500-1000G: the vanilla dialog, with the "Received XG!!" line
 *       updated to show the actual rolled amount.</li>
 * </ul>
 * <p>
 * The game's text engine uses one byte per glyph: {@code 0x00-0x19}=A-Z,
 * {@code 0x1A-0x33}=a-z, {@code 0x34-0x3D}=0-9, {@code 0xEF}=space,
 * {@code 0x4C}='.', {@code 0x40}='!', {@code 0x41}='?', {@code 0x53}="'s",
 * {@code 0xE9}=wait prompt, {@code 0xFE}=frame break, {@code 0xFF}=end.
 * A message is one or more frames; each frame is a 10-byte box header
 * followed by glyph bytes. Message $09 lives at file offset 0x1C684 and is
 * enqueued only by the drawer handler (verified xref).
 */
public final class GrandpaDrawerPatch implements Patch {

    public static final int MIN_GOLD = 0;
    public static final int MAX_GOLD = 1000;

    /** Vanilla drawer amount for the spoiler log. */
    public static final int VANILLA_GOLD = 1000;

    /** File offset of drawer message $09 (text bank $07, CPU $4684). */
    static final int MSG09_OFFSET = 0x1C684;

    /** Message $0a begins here; $09 may never grow past this. */
    static final int MSG09_END = 0x1C730;

    /** 10-byte dialog-box frame header used by the vanilla drawer message. */
    private static final byte[] FRAME_HEADER = {
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x78, 0x70, 0x00, 0x00
    };

    /** "Received " - start of vanilla frame 4, used to locate it. */
    private static final byte[] RECEIVED_SIG = {
        0x11, 0x1E, 0x1C, 0x1E, 0x22, 0x2F, 0x1E, 0x1D, (byte) 0xEF
    };

    /** "What! There" + frame header - sanity check before rewriting $09. */
    private static final byte[] MSG09_SIG = {
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x78, 0x70, 0x00, 0x00,
        0x16, 0x21, 0x1A, 0x2D, 0x40, (byte) 0xEF, 0x13, 0x21, 0x1E, 0x2B, 0x1E
    };

    static final String TIER_EMPTY = "Empty! Grandpa's rainy day fund was all sunshine.";
    static final String TIER_DRIZZLE = "Rainy day fund? More like a light drizzle.";

    @Override
    public String name() {
        return "Grandpa's rainy day money drawer (0-1000G)";
    }

    @Override
    public void apply(byte[] rom, Random rng, SpoilerLog spoiler) throws Exception {
        // Sanity: make sure we are patching the code we reverse-engineered.
        for (int[] site : HarvestMoonGbcRom.DRAWER_GOLD_IMMEDIATES) {
            int base = site[0] - 4; // start of signature: 'ld hl, $b8ef'
            for (int i = 0; i < HarvestMoonGbcRom.DRAWER_GOLD_SIGNATURE.length; i++) {
                if (rom[base + i] != HarvestMoonGbcRom.DRAWER_GOLD_SIGNATURE[i]) {
                    throw new IllegalStateException(String.format(
                            "Unexpected code at ROM 0x%06X - refusing to patch.",
                            base + i));
                }
            }
        }

        int gold = MIN_GOLD + rng.nextInt(MAX_GOLD - MIN_GOLD + 1);
        int lo = gold & 0xFF;
        int mid = (gold >> 8) & 0xFF;

        for (int[] site : HarvestMoonGbcRom.DRAWER_GOLD_IMMEDIATES) {
            rom[site[0]] = (byte) lo;   // immediate lo of 'ld bc, ...'
            rom[site[1]] = (byte) mid;  // immediate mid of 'ld bc, ...'
        }

        String message = rewriteDrawerMessage(rom, gold);

        spoiler.addChange("Grandpa's rainy day money drawer",
                gold + "G (vanilla: " + VANILLA_GOLD + "G) -- message: " + message);
    }

    /**
     * Rewrites drawer message $09 in place according to the amount tier and
     * returns a human-readable description of the message for the spoiler log.
     */
    static String rewriteDrawerMessage(byte[] rom, int gold) throws Exception {
        for (int i = 0; i < MSG09_SIG.length; i++) {
            if (rom[MSG09_OFFSET + i] != MSG09_SIG[i]) {
                throw new IllegalStateException(String.format(
                        "Unexpected drawer message at ROM 0x%06X - refusing to patch.",
                        MSG09_OFFSET + i));
            }
        }

        byte[] message;
        String description;
        if (gold <= 0) {
            message = buildFrames(new String[] {
                "Empty! Grandpa's rainy day",
                "fund was all sunshine."
            });
            description = "\"" + TIER_EMPTY + "\"";
        } else if (gold < 500) {
            message = buildFrames(new String[] {
                "Rainy day fund? More like",
                "a light drizzle.",
                "Received " + gold + "G!!"
            });
            description = "\"" + TIER_DRIZZLE + "\" then \"Received " + gold + "G!!\"";
        } else {
            // Keep the vanilla dialog; re-issue frame 4 so the "Received XG!!"
            // line shows the actual rolled amount instead of a fixed 1000G.
            int frame4 = indexOf(rom, RECEIVED_SIG, MSG09_OFFSET, MSG09_END) - FRAME_HEADER.length;
            if (frame4 < MSG09_OFFSET) {
                throw new IllegalStateException(
                        "Vanilla 'Received' frame not found in drawer message.");
            }
            byte[] tail = new byte[] {};
            tail = concat(tail, FRAME_HEADER);
            tail = concat(tail, encodeText("Received " + gold + "G!!"));
            tail = concat(tail, new byte[] { (byte) 0xFF });

            message = new byte[frame4 - MSG09_OFFSET + tail.length];
            System.arraycopy(rom, MSG09_OFFSET, message, 0, frame4 - MSG09_OFFSET);
            System.arraycopy(tail, 0, message, frame4 - MSG09_OFFSET, tail.length);
            description = "vanilla (Received " + gold + "G!!)";
        }

        if (message.length > MSG09_END - MSG09_OFFSET) {
            throw new IllegalStateException("Rewritten drawer message is too long.");
        }
        // Wipe the old message body first so no stale text can ever be read.
        for (int i = 0; i < MSG09_END - MSG09_OFFSET; i++) {
            rom[MSG09_OFFSET + i] = 0;
        }
        System.arraycopy(message, 0, rom, MSG09_OFFSET, message.length);
        return description;
    }

    /** Builds the byte stream for a multi-frame message. */
    private static byte[] buildFrames(String[] frames) {
        byte[] out = new byte[] {};
        for (int i = 0; i < frames.length; i++) {
            out = concat(out, FRAME_HEADER);
            out = concat(out, encodeText(frames[i]));
            if (i < frames.length - 1) {
                out = concat(out, new byte[] { (byte) 0xE9, (byte) 0xFE }); // prompt + next frame
            } else {
                out = concat(out, new byte[] { (byte) 0xFF }); // end of message
            }
        }
        return out;
    }

    /** Encodes plain text into the game's glyph codes (see class javadoc). */
    static byte[] encodeText(String text) {
        byte[] out = new byte[text.length()];
        int n = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\'' && i + 1 < text.length()
                    && (text.charAt(i + 1) == 's' || text.charAt(i + 1) == 'S')) {
                out[n++] = 0x53; // "'s" ligature
                i++;
            } else {
                out[n++] = (byte) glyphCode(c);
            }
        }
        byte[] trimmed = new byte[n];
        System.arraycopy(out, 0, trimmed, 0, n);
        return trimmed;
    }

    private static int glyphCode(char c) {
        if (c >= 'A' && c <= 'Z') {
            return c - 'A';
        }
        if (c >= 'a' && c <= 'z') {
            return 0x1A + (c - 'a');
        }
        if (c >= '0' && c <= '9') {
            return 0x34 + (c - '0');
        }
        switch (c) {
            case ' ': return 0xEF;
            case '.': return 0x4C;
            case '!': return 0x40;
            case '?': return 0x41;
            default:
                throw new IllegalArgumentException(
                        "No glyph mapping for character '" + c + "'.");
        }
    }

    private static int indexOf(byte[] haystack, byte[] needle, int from, int to) {
        outer:
        for (int i = from; i <= to - needle.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (haystack[i + j] != needle[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    private static byte[] concat(byte[] a, byte[] b) {
        byte[] out = new byte[a.length + b.length];
        System.arraycopy(a, 0, out, 0, a.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }
}
