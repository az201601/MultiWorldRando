package com.multiworldrando;

import java.io.File;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.multiworldrando.core.Randomizer;
import com.multiworldrando.core.SeedGenerator;
import com.multiworldrando.core.Settings;
import com.multiworldrando.ui.RandomizerFrame;

/**
 * Entry point for the randomizer.
 * <p>
 * With no arguments the Swing GUI starts. For automated testing there is a
 * headless mode:
 * <pre>
 *   java -jar MultiWorldRando.jar --headless &lt;rom&gt; [--gold] [--out &lt;dir&gt;]
 * </pre>
 */
public final class MultiWorldRando {

    public static void main(String[] args) {
        if (args.length > 0 && "--headless".equals(args[0])) {
            int status = runHeadless(args);
            System.exit(status);
            return;
        }

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception ignored) {
                    // default look and feel is fine
                }
                new RandomizerFrame().setVisible(true);
            }
        });
    }

    private static int runHeadless(String[] args) {
        String romPath = null;
        boolean gold = false;
        boolean drawer = false;
        File outDir = null;

        for (int i = 1; i < args.length; i++) {
            if ("--gold".equals(args[i])) {
                gold = true;
            } else if ("--drawer".equals(args[i])) {
                drawer = true;
            } else if ("--out".equals(args[i]) && i + 1 < args.length) {
                outDir = new File(args[++i]);
            } else if (romPath == null) {
                romPath = args[i];
            }
        }

        if (romPath == null) {
            System.err.println("usage: --headless <rom> [--gold] [--drawer] [--out <dir>]");
            return 2;
        }

        try {
            Settings settings = new Settings();
            settings.setRandomizeStartingGold(gold);
            settings.setRandomizeDrawerMoney(drawer);

            Randomizer.Result result =
                    Randomizer.randomize(new File(romPath), settings, outDir);

            System.out.println("SEED=" + SeedGenerator.format(result.seed));
            System.out.println("ROM=" + result.romOut.getAbsolutePath());
            System.out.println("SPOILER=" + result.spoilerOut.getAbsolutePath());
            return 0;
        } catch (Exception e) {
            System.err.println("FAILED=" + e.getMessage());
            return 1;
        }
    }
}
