package com.multiworldrando.games.hm1;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class Hm1Patcher {

    public static void fixChecksums(byte[] rom) {
        // 1. Header Complement Checksum (0x0134 to 0x014C)
        int chk = 0;
        for (int i = 0x0134; i <= 0x014C; i++) {
            chk = (chk - (rom[i] & 0xFF) - 1) & 0xFF;
        }
        rom[0x014D] = (byte) chk;

        // 2. Global Checksum (16-bit sum of all bytes except 0x014E and 0x014F)
        int globalChk = 0;
        for (int i = 0; i < rom.length; i++) {
            if (i != 0x014E && i != 0x014F) {
                globalChk = (globalChk + (rom[i] & 0xFF)) & 0xFFFF;
            }
        }
        rom[0x014E] = (byte) ((globalChk >> 8) & 0xFF);
        rom[0x014F] = (byte) (globalChk & 0xFF);
    }

    public static File patchRom(File inputRom, File outputRom, List<Hm1GrandpaIntro.DialoguePage> customScript) throws IOException {
        byte[] rom = Files.readAllBytes(inputRom.toPath());

        // Apply Grandpa cutscene
        Hm1GrandpaIntro.patchGrandpaIntro(rom, customScript);

        // Fix Game Boy header checksums
        fixChecksums(rom);

        // Ensure parent directory exists
        if (outputRom.getParentFile() != null) {
            outputRom.getParentFile().mkdirs();
        }

        Files.write(outputRom.toPath(), rom);
        return outputRom;
    }
}
