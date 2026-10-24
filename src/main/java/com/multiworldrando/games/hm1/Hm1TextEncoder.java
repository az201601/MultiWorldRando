package com.multiworldrando.games.hm1;

import java.util.HashMap;
import java.util.Map;

/**
 * Text and dialogue encoder for Harvest Moon GBC.
 * Encodes text into the game's custom 1-byte tile character map.
 */
public class Hm1TextEncoder {

    public static final byte CHAR_SPACE = (byte) 0xEF;
    public static final byte CHAR_EXCLAMATION = (byte) 0x40;
    public static final byte CHAR_QUESTION = (byte) 0x41;
    public static final byte CHAR_HEART = (byte) 0x42;
    public static final byte CHAR_ELLIPSIS = (byte) 0x43;
    public static final byte CHAR_HYPHEN = (byte) 0x44;
    public static final byte CHAR_TILDE = (byte) 0x45;
    public static final byte CHAR_APOSTROPHE = (byte) 0x4B;
    public static final byte CHAR_PERIOD = (byte) 0x4C;
    public static final byte CHAR_COMMA = (byte) 0x8E;
    public static final byte CHAR_OPEN_PAREN = (byte) 0x50;
    public static final byte CHAR_CLOSE_PAREN = (byte) 0x51;
    public static final byte CHAR_OPEN_BRACKET = (byte) 0x3E;
    public static final byte CHAR_CLOSE_BRACKET = (byte) 0x3F;

    // Control codes
    public static final byte CODE_WAIT = (byte) 0xE9;
    public static final byte CODE_CLEAR = (byte) 0xFE;
    public static final byte CODE_END = (byte) 0xFF;

    // RAM Variable tags
    public static final byte[] VAR_PLAYER = new byte[] {(byte) 0xA0, (byte) 0xA1, (byte) 0xA2, (byte) 0xA3};

    private static final Map<Character, Byte> PUNCTUATION = new HashMap<>();

    static {
        PUNCTUATION.put(' ', CHAR_SPACE);
        PUNCTUATION.put('!', CHAR_EXCLAMATION);
        PUNCTUATION.put('?', CHAR_QUESTION);
        PUNCTUATION.put('♥', CHAR_HEART);
        PUNCTUATION.put('-', CHAR_HYPHEN);
        PUNCTUATION.put('~', CHAR_TILDE);
        PUNCTUATION.put('\'', CHAR_APOSTROPHE);
        PUNCTUATION.put('.', CHAR_PERIOD);
        PUNCTUATION.put(',', CHAR_COMMA);
        PUNCTUATION.put('(', CHAR_OPEN_PAREN);
        PUNCTUATION.put(')', CHAR_CLOSE_PAREN);
        PUNCTUATION.put('[', CHAR_OPEN_BRACKET);
        PUNCTUATION.put(']', CHAR_CLOSE_BRACKET);

        for (int i = 0; i <= 9; i++) {
            PUNCTUATION.put((char) ('0' + i), (byte) (0x34 + i));
        }
    }

    public static byte encodeChar(char ch, boolean isWordStart) {
        if (PUNCTUATION.containsKey(ch)) {
            return PUNCTUATION.get(ch);
        }
        if (ch >= 'a' && ch <= 'z') {
            return (byte) (0x1A + (ch - 'a'));
        }
        if (ch >= 'A' && ch <= 'Z') {
            if (isWordStart) {
                return (byte) (0x00 + (ch - 'A'));
            } else {
                return (byte) (0x1A + (ch - 'A'));
            }
        }
        return CHAR_SPACE;
    }

    public static byte[] encodeString(String text, int targetLength) {
        byte[] buffer = new byte[targetLength * 2 + 10];
        int writePos = 0;
        boolean isStart = true;
        int i = 0;

        while (i < text.length()) {
            if (text.startsWith("<PLAYER>", i)) {
                for (byte b : VAR_PLAYER) {
                    buffer[writePos++] = b;
                }
                i += 8;
                isStart = false;
                continue;
            }
            if (text.startsWith("...", i)) {
                buffer[writePos++] = CHAR_ELLIPSIS;
                i += 3;
                isStart = true;
                continue;
            }
            char ch = text.charAt(i);
            buffer[writePos++] = encodeChar(ch, isStart);
            if (ch == ' ' || ch == '-' || ch == '.') {
                isStart = true;
            } else {
                isStart = false;
            }
            i++;
        }

        byte[] result = new byte[targetLength];
        for (int j = 0; j < targetLength; j++) {
            if (j < writePos) {
                result[j] = buffer[j];
            } else {
                result[j] = CHAR_SPACE;
            }
        }
        return result;
    }

    public static byte[] makePage(String line1, String line2, boolean isLast) {
        byte[] page = new byte[43];
        // Window header: bc 54 00 00
        page[0] = (byte) 0xBC;
        page[1] = (byte) 0x54;
        page[2] = 0x00;
        page[3] = 0x00;

        byte[] l1 = encodeString(line1, 16);
        byte[] l2 = encodeString(line2, 15);
        System.arraycopy(l1, 0, page, 4, 16);
        System.arraycopy(l2, 0, page, 20, 15);

        if (isLast) {
            page[35] = CHAR_SPACE;
            page[36] = CODE_END;
        } else {
            page[35] = CODE_WAIT;
            page[36] = CODE_CLEAR;
        }

        // Bytes 37-42 are 0x00 padding
        for (int j = 37; j < 43; j++) {
            page[j] = 0x00;
        }
        return page;
    }
}
