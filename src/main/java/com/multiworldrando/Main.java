package com.multiworldrando;

import com.multiworldrando.games.hm1.Hm1Patcher;
import java.io.File;

public class Main {
    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("     MultiWorld Randomizer Engine (Java)          ");
        System.out.println("==================================================");

        File inputRom;
        File outputRom;

        if (args.length >= 2) {
            inputRom = new File(args[0]);
            outputRom = new File(args[1]);
        } else if (args.length == 1) {
            inputRom = new File(args[0]);
            outputRom = new File("Harvest_Moon_GBC_Rando.gbc");
        } else {
            // Default paths
            File default1 = new File("Harvest Moon GB (U) [C][!].gbc");
            File default2 = new File("/home/user/roms/Harvest Moon GB (U) [C][!].gbc");

            if (default1.exists()) {
                inputRom = default1;
            } else if (default2.exists()) {
                inputRom = default2;
            } else {
                System.out.println("Usage: java -jar multiworld-rando.jar <input_rom.gbc> [output_rom.gbc]");
                return;
            }
            outputRom = new File("Harvest_Moon_GBC_Rando.gbc");
        }

        if (!inputRom.exists()) {
            System.err.println("Error: Input ROM not found: " + inputRom.getAbsolutePath());
            return;
        }

        try {
            System.out.println("Input ROM:  " + inputRom.getAbsolutePath());
            System.out.println("Output ROM: " + outputRom.getAbsolutePath());
            System.out.println("Patching Harvest Moon GBC randomizer features...");

            File result = Hm1Patcher.patchRom(inputRom, outputRom, null);
            System.out.println("Success! Generated: " + result.getName() + " (" + result.length() + " bytes)");
        } catch (Exception e) {
            System.err.println("Failed to patch ROM: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
